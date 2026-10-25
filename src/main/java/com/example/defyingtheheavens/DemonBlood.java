package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

/**
 * A demon race's blood, drunk (DemonBloodItem; the user's ask, 2026-10-24: "blood lines should be a physical item like slips. you should
 * need to ingest the blood to grow as a demon beast (treated as methods basically, they just unlock your potential). as a human you
 * should be able to ingest them to learn demon path and cultivate it, however if you have too much or you drink some that far outscales
 * your heaven cultivation or ghost cultivation, it will turn you into a full demon race member"):
 * <ul>
 *   <li><b>A demon</b> drinks its own race's blood alone: blood of a realm beyond what its blood is awake to (DemonState#bloodRealm, Qi
 *       Refining's from birth) awakens it so far, and its Demon Path may break through into those realms (DemonPath#heldByBlood), as the
 *       heaven path's into a realm whose method it knows. Blood it already holds is refused, not wasted.</li>
 *   <li><b>A human</b> drinks it to form a demon core: a half-blood (DemonPath#isHalfBlood), its Demon Path begun at Qi Refining Early,
 *       its look its own, the race's gifts at half strength (RaceGifts). Each drink makes the blood in it purer ({@link #gain}); at the
 *       whole, or with blood {@link #TURNS_ABOVE} realms or more above the higher of its heaven and ghost paths, it turns ({@link #turn}):
 *       wholly that race's, its Demon Path kept. A drink that would turn it is refused with a warning, unless drunk sneaking. Once it is of
 *       a race, no other race's blood takes; there is no way back.</li>
 *   <li>Never a mortal's (its meridians sealed), one of the dead's, nor one's in another's body.</li>
 * </ul>
 * It is left by the race's demons (a clan's cultivator now and then: {@link #NPC_CHANCE}; a demon player slain by another, with its core)
 * and by the wild beasts of its kin ({@link #kin}); and now and then a clan keeps a phial of it laid up in its den's chests ({@link #hoarded}).
 */
public final class DemonBlood {
	/** How much purer a half-blood's blood grows a drink: blood below its other paths' realm, at it, a realm above, blood it already held. */
	public static final double PURITY_BELOW = 0.04, PURITY_LEVEL = 0.08, PURITY_ABOVE = 0.30, PURITY_HELD = 0.15;
	/** Blood this many realms or more above the higher of a human's heaven and ghost paths turns it at once. */
	public static final int TURNS_ABOVE = 2;
	/** A clan's cultivator leaves its race's blood this often when it dies (its core always). */
	public static final float NPC_CHANCE = 0.35f;

	public enum Verdict { DRINK, WARN, REFUSE }

	/** What drinking a race's blood would do: drunk (its purity's {@code gain}, whether it {@code turns}), a warning, or refused (why). */
	public record Outcome(Verdict verdict, Component why, double gain, boolean turns) {
		static Outcome refuse(Component why) { return new Outcome(Verdict.REFUSE, why, 0, false); }
	}

	/**
	 * What {@code c} drinking {@code race}'s blood of {@code realm} would do: {@code sneaking} (it means to, though it turn it), {@code dead}
	 * (one of the dead), {@code elsewhere} (its soul in another's body). The same on both sides: the client refuses before the drinking.
	 */
	public static Outcome judge(PlayerCultivation c, Bloodline race, Realm realm, boolean sneaking, boolean dead, boolean elsewhere) {
		if (race == null || !race.isBeast() || realm == null) return Outcome.refuse(Component.translatable(ModLang.MSG_BLOOD_UNKNOWN));
		if (elsewhere) return Outcome.refuse(Component.translatable(ModLang.MSG_BLOOD_ELSEWHERE));
		if (dead) return Outcome.refuse(Component.translatable(ModLang.MSG_BLOOD_DEAD));
		if (c.isMortal()) return Outcome.refuse(Component.translatable(ModLang.MSG_BLOOD_MORTAL));
		Bloodline mine = DemonPath.race(c);
		if (mine.isBeast() && mine != race) return Outcome.refuse(Component.translatable(ModLang.MSG_BLOOD_OTHER, mine.getDisplayName()));
		int awake = c.demon().bloodRealm();
		if (DemonPath.isDemon(c)) {
			if (realm.ordinal() <= awake) return Outcome.refuse(Component.translatable(ModLang.MSG_BLOOD_HELD, Realm.byIndex(awake).getDisplayName()));
			return new Outcome(Verdict.DRINK, null, 0, false);
		}
		double gain = gain(c, realm);
		boolean turns = realm.ordinal() - otherPaths(c) >= TURNS_ABOVE || c.demon().purity() + gain >= 1 - 1.0e-9;
		if (turns && !sneaking) return new Outcome(Verdict.WARN, Component.translatable(ModLang.MSG_BLOOD_WARN, race.getDisplayName()), gain, true);
		return new Outcome(Verdict.DRINK, null, gain, turns);
	}

	/** How much purer a human's blood grows drinking blood of {@code realm}: by the realm against its other paths', or blood it already held. */
	public static double gain(PlayerCultivation c, Realm realm) {
		if (DemonPath.isHalfBlood(c) && realm.ordinal() <= c.demon().bloodRealm()) return PURITY_HELD;
		int gap = realm.ordinal() - otherPaths(c);
		return gap < 0 ? PURITY_BELOW : gap == 0 ? PURITY_LEVEL : gap < TURNS_ABOVE ? PURITY_ABOVE : 1.0;
	}

	/** The realm (ordinal) of the higher of {@code c}'s heaven and ghost paths (Qi Refining's at the least: one drinking isn't a mortal). */
	public static int otherPaths(PlayerCultivation c) {
		int stages = Stage.values().length;
		int heaven = Math.max(0, TwoPaths.heavenRank(c, false)) / stages;
		int ghost = GhostMethods.ghostRank(c.netherworld());
		return Math.max(heaven, ghost < 0 ? 0 : ghost / stages);
	}

	/**
	 * {@code player} drinks {@code race}'s blood of {@code realm} (DemonBloodItem#finishUsingItem), judged again ({@link #judge}): told why
	 * if it won't take. @return whether it was drunk
	 */
	public static boolean drink(ServerPlayer player, Bloodline race, Realm realm) {
		PlayerCultivation c = CultivationManager.own(player);
		Outcome outcome = judge(c, race, realm, player.isShiftKeyDown(), c.netherworld().ghost(), CultivationManager.get(player) != c);
		if (outcome.verdict() != Verdict.DRINK) {
			player.displayClientMessage(outcome.why().copy().withStyle(outcome.verdict() == Verdict.WARN ? ChatFormatting.RED : ChatFormatting.GRAY), true);
			return false;
		}
		int before = DemonPath.rank(c);
		if (!DemonPath.isDemon(c)) {
			if (c.demon().halfBlood() == null) {
				// A demon core forms: the Demon Path begun at Qi Refining Early (if it never was), a half-blood of the race.
				c.demon().setHalfBlood(race);
				if (!c.demon().begun()) c.demon().set(0, 0);
				QiElement element = DemonPath.element(race);
				player.sendSystemMessage(Component.translatable(ModLang.MSG_BLOOD_CORE, race.getDisplayName(), element.getDisplayName()).withStyle(ChatFormatting.DARK_RED));
				Journal.note(player, "half_blood", ModLang.JOURNAL_HALF_BLOOD, Component.translatable(ModLang.JOURNAL_HALF_BLOOD_TEXT, race.getDisplayName(),
						element.getDisplayName()));
			}
			c.demon().setPurity(c.demon().purity() + outcome.gain());
			if (!outcome.turns()) player.displayClientMessage(Component.translatable(ModLang.MSG_BLOOD_PURITY, race.getDisplayName(),
					Math.round(c.demon().purity() * 100)).withStyle(ChatFormatting.RED), true);
		}
		if (realm.ordinal() > c.demon().bloodRealm()) {
			c.demon().setBloodRealm(realm.ordinal());
			player.sendSystemMessage(Component.translatable(ModLang.MSG_BLOOD_AWAKENS, realm.getDisplayName()).withStyle(ChatFormatting.RED));
		}
		player.serverLevel().sendParticles(new DustParticleOptions(new Vector3f(0.7f, 0.05f, 0.08f), 1.4f), player.getX(), player.getY() + 1.0, player.getZ(),
				30, 0.4, 0.7, 0.4, 0.02);
		player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.2f, 0.8f);
		if (outcome.turns()) turn(player, c, race);
		CultivationManager.markDirty(player.server);
		if (DemonPath.rank(c) > before) DemonPath.afterRank(player, before); // the inheritance's first spells, for a core just formed
		else {
			CultivationManager.refresh(player);
			Spells.sync(player);
		}
		return true;
	}

	/** {@code player} turned by its blood: wholly {@code race}'s (Bloodlines: a colouring of it at random), its Demon Path as it was. */
	static void turn(ServerPlayer player, PlayerCultivation c, Bloodline race) {
		c.demon().setPurity(1);
		Bloodlines.set(player.server, player.getUUID(), race, player.getRandom().nextInt(Math.max(1, race.variants())));
		DemonPath.born(player); // its record of its race's (Bloodlines#set does it only for one in the player list)
		player.sendSystemMessage(Component.translatable(ModLang.MSG_BLOOD_TURNED, race.getDisplayName()).withStyle(ChatFormatting.DARK_RED));
		player.playNotifySound(SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.6f, 1.2f);
		Journal.note(player, "turned_by_blood", ModLang.JOURNAL_TURNED, Component.translatable(ModLang.JOURNAL_TURNED_TEXT, race.getDisplayName()));
	}

	/**
	 * How often a demon clan's den keeps a phial of its race's blood in one of its chests (a hoard's, its head's hall's, an elder's), and of
	 * which realm when it does, weights from Qi Refining up (the user's ask, 2026-10-25: "demons should be able to loot the blood (although
	 * rarely, as rare as getting a heaven method or ghost method at that level from a library) from chests and stuff in the demon clans"; it
	 * also gives the Demon Path its way past Sacred Vessel, whose blood nothing else leaves). Its realms are those of a sect library's slips
	 * in the same world, as often (chests/sect_library keeps each world's own; the user, the same day: "you can get EN phials from clans but
	 * you can't get EN methods from sects"): the lower realms' Qi Refining to Core Formation, the Upper Realm's Core Formation to Entering
	 * Nirvana (never a True Immortal's: Realm#taughtInTheWorld).
	 */
	public static final float HOARD_CHANCE = 0.12f, HALL_CHANCE = 0.12f, ELDER_CHANCE = 0.08f;
	static final int[] LOWER_REALMS_BLOOD = {60, 30, 10}, UPPER_REALM_BLOOD = {0, 0, 20, 20, 18, 15, 11, 7, 5, 4};

	/**
	 * A phial of {@code race}'s blood laid up in one of its den's chests (SectPiece#chest; its loot, {@code chest}, fills in round it when
	 * it is opened), or nothing ({@link #HOARD_CHANCE}): of a realm by the {@code upper} realm's weights or the lower realms'.
	 */
	public static ItemStack hoarded(Bloodline race, boolean upper, ResourceLocation chest, RandomSource random) {
		if (race == null || !race.isBeast()) return ItemStack.EMPTY;
		float chance = chest.equals(SectPiece.TREASURY_LOOT) ? HOARD_CHANCE : chest.equals(SectPiece.MASTER_HALL_LOOT) ? HALL_CHANCE
				: chest.equals(SectPiece.ELDER_LOOT) ? ELDER_CHANCE : 0;
		if (random.nextFloat() >= chance) return ItemStack.EMPTY;
		int[] weights = upper ? UPPER_REALM_BLOOD : LOWER_REALMS_BLOOD;
		int total = 0;
		for (int w : weights) total += w;
		int roll = random.nextInt(total);
		for (int i = 0; i < weights.length; i++) {
			roll -= weights[i];
			if (roll < 0) return DemonBloodItem.of(race, Realm.byIndex(i));
		}
		return ItemStack.EMPTY;
	}

	/** The race whose blood a wild beast of {@code kind} leaves (its kin), or null: the tiger's, the fox's, the reptiles' the lizard's, the flood dragon's. */
	public static Bloodline kin(BeastKind kind) {
		return switch (kind) {
			case BLOODFANG_TIGER -> Bloodline.TIGER;
			case EMBER_FOX -> Bloodline.FOX;
			case JADE_VIPER, JADE_SCALED_PYTHON, IRON_JAW_CROCODILE -> Bloodline.LIZARD;
			case FLOOD_DRAGON -> Bloodline.DRAGON;
			default -> null;
		};
	}

	private DemonBlood() {}
}
