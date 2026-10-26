package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Five Decays of Heaven and Man (phase 3 of the realm plan, 2026-10-26; the user's picks: a weathered Decay's Law chosen by the
 * player, a failed judgement a stage lost and the life kept): what comes for every True Immortal player and those past them, once in
 * {@link #PERIOD} (ten days), told a day before ({@link #WARNING}). Its five signs come a minute apart ({@link #SIGN_TICKS}), each staying
 * till the Decay ends:
 * <ol>
 *   <li>the robes stain: the body's armour {@link #ARMOUR_KEPT} of itself ({@link #applyArmour});</li>
 *   <li>the flowers wither: nothing mends it, its own body nor anything else ({@link #withered}, Laws#healed);</li>
 *   <li>the qi falters: its gathering halved ({@link #gatherFactor}), its immortal essence draining wherever it is ({@link #drains},
 *       a full pool in {@link #ESSENCE_DRAIN_SECONDS});</li>
 *   <li>the body sweats: its blows and spells {@link #HARM_KEPT} of themselves ({@link #outgoing});</li>
 *   <li>the mind can't rest: no meditation ({@link #restless}).</li>
 * </ol>
 * A minute after the fifth, the judgement: a storm over it for thirty seconds, its strikes the tribulation's into the stage after its own
 * (TribulationManager#strikes, what its body is weighed by; a Heavenly Lord of Grand Perfection, with none after it, its own {@link
 * #LAST_HARDER} times), softened as any tribulation is and past every ward, nothing mending it meanwhile. Weathered, a level of the
 * Laws for the player to choose (LawState#choices: the Methods tab's Laws page); struck down, or killed by anything while it runs, it lives
 * on a stage lower at half its health ({@link #allowDeath}). Then ten days more.
 * <p>
 * The Decays wait, their time standing still, where the heavens don't see (the Netherworld, the Spatial Gap, the Inner Realm), for a soul
 * away from its body, in another's or in a clone, for the dead, and for creative and spectating players; and through a logout (a sign
 * under way begins its minute again; a judgement under way comes again a minute after the return). The realm's own immortals never
 * decay. Saved on the record (LawState).
 */
public final class FiveDecays {
	/** Game ticks between one Decay and the next (ten in-game days), and how long before one it is told (a day). */
	public static final long PERIOD = 10L * 24000, WARNING = 24000;
	/** Between one sign and the next, and from the fifth to the judgement (a minute). */
	public static final int SIGN_TICKS = 1200;
	/** Its five signs (LawState#decaySign 1 to 5), and its judgement after them (6). */
	public static final int SIGNS = 5, JUDGEMENT = 6;
	/** The robes stain: so much of its armour is left it. */
	public static final double ARMOUR_KEPT = 0.7;
	/** The body sweats: so much of its blows' and spells' harm. */
	public static final float HARM_KEPT = 0.7f;
	/** The qi falters: so much of its gathering. */
	public static final double GATHER_KEPT = 0.5;
	/** ...and its immortal essence drains away, a full pool in so many seconds. */
	public static final int ESSENCE_DRAIN_SECONDS = 180;
	/** A Heavenly Lord of Grand Perfection's judgement (no stage after its own): its own tribulation's strikes, this much the harder. */
	public static final float LAST_HARDER = 1.1f;
	private static final int GATHER_TICKS = 60, FINISH_TICKS = 20;
	private static final UUID ARMOUR_ID = UUID.fromString("9e2c5b14-6f3a-4d87-b1c9-3a7e5d2f8c41");

	/** A judgement under way (not saved: LawState keeps that one is, and it comes again after a logout). */
	private static final class Judgement {
		final TribulationManager.Strikes strikes;
		final Realm realm;
		final Stage stage;
		final int span;
		int struck;
		int sinceFirst = -GATHER_TICKS;
		int finishIn = FINISH_TICKS;
		int sinceBolt = TribulationManager.MIN_BOLT_INTERVAL;
		int sinceFlash = TribulationManager.FLASH_EVERY;
		boolean paused;
		TribulationCloud cloud;

		Judgement(TribulationManager.Strikes strikes, Realm realm, Stage stage) {
			this.strikes = strikes;
			this.realm = realm;
			this.stage = stage;
			this.span = TribulationManager.strikeSpan(strikes.count());
		}

		void removeCloud() {
			if (cloud != null) cloud.discard();
			cloud = null;
		}
	}

	private static final Map<UUID, Judgement> JUDGED = new HashMap<>();
	/** The one a judgement's strike is landing on this moment, or null. */
	private static UUID striking;

	/** Whether {@code entity} is taking a judgement's strike this moment: past every ward, as a tribulation's (HeavenSpells#incoming). */
	public static boolean isStriking(Entity entity) {
		return striking != null && entity != null && striking.equals(entity.getUUID());
	}

	/** Whether {@code c}'s Decays come at all: an immortal's (Laws#isImmortal), one of the living. */
	public static boolean decays(PlayerCultivation c) {
		return Laws.isImmortal(c) && !c.netherworld().ghost();
	}

	/** The sign {@code c} stands under (0 none, 1 to {@link #SIGNS}, {@link #JUDGEMENT}). */
	public static int sign(PlayerCultivation c) {
		return c.laws().decaySign();
	}

	/** The flowers wither (the second sign on): nothing mends {@code p}. */
	public static boolean withered(ServerPlayer p) {
		return sign(CultivationManager.get(p)) >= 2;
	}

	/** The qi falters (the third sign on): what of {@code c}'s qi gathering is left it (PlayerCultivation#qiGatherAt). */
	public static double gatherFactor(PlayerCultivation c) {
		return sign(c) >= 3 ? GATHER_KEPT : 1;
	}

	/** ...and its immortal essence drains wherever it is (ImmortalEssence#second). */
	public static boolean drains(PlayerCultivation c) {
		return sign(c) >= 3;
	}

	/** The body sweats (the fourth sign on): a blow or spell of {@code attacker}'s (Physiques#outgoing). */
	public static float outgoing(ServerPlayer attacker, float amount) {
		return sign(CultivationManager.get(attacker)) >= 4 ? amount * HARM_KEPT : amount;
	}

	/** The mind can't rest (the fifth sign on, the judgement too): {@code p} can't meditate (MeditationManager). */
	public static boolean restless(ServerPlayer p) {
		return sign(CultivationManager.get(p)) >= SIGNS;
	}

	// --- Every tick ---

	public static void tick(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
			Judgement j = JUDGED.get(p.getUUID());
			if (j != null) judge(p, j);
			else if (p.tickCount % 20 == 0) second(p, now);
		}
	}

	/** Where {@code p}'s Decays wait, their time standing still: unseen by the heavens, its soul away from its own body, creative. */
	private static boolean waiting(ServerPlayer p) {
		return p.isCreative() || p.isSpectator() || !p.isAlive() || ModDimensions.isUnseenByHeaven(p.level().dimension())
				|| ModDimensions.isSpatialGap(p.level().dimension()) || ModDimensions.isInnerRealm(p.level().dimension())
				|| SoulProjection.isAwayFromBody(p.getUUID()) || Possession.inhabits(p.getUUID()) || Clones.inClone(p);
	}

	/** A second of {@code p}'s Decays: reckoned, warned of, begun, each sign on its minute, the judgement after the last. */
	private static void second(ServerPlayer p, long now) {
		PlayerCultivation c = CultivationManager.own(p); // its own record, even where its soul is away from its body (waiting, below)
		LawState s = c.laws();
		if (!decays(c)) {
			// No immortal (fallen below True Immortal, or dead): nothing of a Decay left on it.
			if (s.decaySign() > 0 || s.decayDue() > 0 || s.decayWarned()) {
				s.setDecay(0, 0);
				s.setDecayDue(0);
				s.setDecayWarned(false);
				applyArmour(p, false);
				CultivationManager.markDirty(p.server);
				CultivationManager.sync(p);
			}
			return;
		}
		if (s.decayDue() <= 0) { // newly an immortal: its first in ten days
			s.setDecayDue(now + PERIOD);
			CultivationManager.markDirty(p.server);
			CultivationManager.sync(p);
			return;
		}
		if (waiting(p)) {
			s.setDecayDue(s.decayDue() + 20);
			if (s.decaySign() > 0) s.setDecay(s.decaySign(), s.decayAt() + 20);
			return;
		}
		if (s.decaySign() == 0) {
			if (!s.decayWarned() && now >= s.decayDue() - WARNING) {
				s.setDecayWarned(true);
				p.sendSystemMessage(Component.translatable(ModLang.MSG_DECAY_WARNING).withStyle(ChatFormatting.DARK_PURPLE));
				p.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0f, 0.6f);
				CultivationManager.markDirty(p.server);
			}
			if (now >= s.decayDue()) sign(p, c, 1, now);
			return;
		}
		if (now - s.decayAt() >= SIGN_TICKS) {
			if (s.decaySign() < SIGNS) sign(p, c, s.decaySign() + 1, now);
			else if (s.decaySign() == SIGNS) startJudgement(p, c, now);
		}
		// The decaying's look: ash drifting off it, and grey petals.
		ServerLevel level = p.serverLevel();
		level.sendParticles(ParticleTypes.WHITE_ASH, p.getX(), p.getY() + 1.2, p.getZ(), 6 + 3 * s.decaySign(), 0.4, 0.7, 0.4, 0.01);
		if (s.decaySign() >= 2) level.sendParticles(ParticleTypes.ASH, p.getX(), p.getY() + 1.8, p.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
	}

	/** The Decay's sign {@code sign} comes on {@code p}: told, heard, and what it brings (the armour's at once). */
	private static void sign(ServerPlayer p, PlayerCultivation c, int sign, long now) {
		c.laws().setDecay(sign, now);
		applyArmour(p, true);
		if (sign >= SIGNS) MeditationManager.stop(p, true);
		CultivationManager.markDirty(p.server);
		CultivationManager.refresh(p); // the qi's gathering, the Laws page
		p.sendSystemMessage(Component.translatable(ModLang.MSG_DECAY_SIGN[sign - 1]).withStyle(ChatFormatting.DARK_PURPLE));
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.5f, 0.6f + 0.1f * sign);
	}

	/** The robes stain: {@code p}'s armour {@link #ARMOUR_KEPT} of itself while a Decay is under way ({@code on}), else whole again. */
	static void applyArmour(ServerPlayer p, boolean on) {
		AttributeInstance armour = p.getAttribute(Attributes.ARMOR);
		if (armour == null) return;
		armour.removeModifier(ARMOUR_ID);
		if (on) armour.addTransientModifier(new AttributeModifier(ARMOUR_ID, "The Five Decays", ARMOUR_KEPT - 1, AttributeModifier.Operation.MULTIPLY_TOTAL));
	}

	// --- The judgement ---

	/** The fifth sign's minute is out: the judgement comes on {@code p}. */
	private static void startJudgement(ServerPlayer p, PlayerCultivation c, long now) {
		int rank = TwoPaths.higherRank(c, false);
		int target = Math.min(PlayerCultivation.LAST_RANK, rank + 1);
		int stages = Stage.values().length;
		Realm realm = Realm.byIndex(target / stages);
		Stage stage = Stage.byIndex(target % stages);
		TribulationManager.Strikes strikes = TribulationManager.strikes(realm, stage, c.hasPhysique(Physique.PRIMORDIAL_CHAOS_BODY));
		if (rank >= PlayerCultivation.LAST_RANK) strikes = new TribulationManager.Strikes(strikes.count(), strikes.bolt() * LAST_HARDER, strikes.magic() * LAST_HARDER);
		c.laws().setDecay(JUDGEMENT, now);
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
		MeditationManager.stop(p, false);
		JUDGED.put(p.getUUID(), new Judgement(strikes, realm, stage));
		p.sendSystemMessage(Component.translatable(ModLang.MSG_DECAY_JUDGEMENT, strikes.count()).withStyle(ChatFormatting.DARK_PURPLE));
		p.server.getPlayerList().broadcastSystemMessage(Component.translatable(ModLang.MSG_DECAY_JUDGEMENT_BROADCAST, p.getDisplayName())
				.withStyle(ChatFormatting.DARK_PURPLE), false);
		p.playNotifySound(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0f, 0.3f);
	}

	/** A tick of {@code p}'s judgement: its storm over it, every strike due by now, then (all of them weathered) its end. */
	private static void judge(ServerPlayer p, Judgement j) {
		if (!p.isAlive()) return; // its death is seen to (allowDeath, onDeath)
		if (waiting(p)) {
			if (!j.paused) {
				j.paused = true;
				j.removeCloud();
				p.sendSystemMessage(Component.translatable(ModLang.MSG_DECAY_WAITS).withStyle(ChatFormatting.GRAY));
			}
			return;
		}
		if (j.paused) {
			j.paused = false;
			j.sinceFirst = Math.min(j.sinceFirst, -GATHER_TICKS / 2); // its storm gathers again before it strikes on
		}
		j.sinceBolt++;
		j.sinceFlash++;
		updateCloud(p, j);
		if (j.struck < j.strikes.count()) {
			int due = TribulationManager.strikesBy(j.strikes.count(), j.span, ++j.sinceFirst);
			while (j.struck < due) {
				strike(p, j);
				if (JUDGED.get(p.getUUID()) != j) return; // struck down just now
				j.struck++;
			}
		} else if (--j.finishIn <= 0) {
			weathered(p);
			return;
		}
		if (p.tickCount % 10 == 0) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_DECAY_STRIKES, j.strikes.count() - j.struck).withStyle(ChatFormatting.DARK_PURPLE), true);
		}
	}

	/** A judgement's strike on {@code p}: its bolt (shared by strikes closer than a bolt's while), and its harm, magic then lightning. */
	private static void strike(ServerPlayer p, Judgement j) {
		ServerLevel level = p.serverLevel();
		if (j.sinceBolt >= TribulationManager.MIN_BOLT_INTERVAL) {
			j.sinceBolt = 0;
			TribulationLightning bolt = ModEntities.TRIBULATION_LIGHTNING.create(level);
			if (bolt != null) {
				bolt.moveTo(p.getX(), p.getY(), p.getZ());
				bolt.setCloudBaseY(j.cloud == null ? p.getY() + TribulationCloud.BASE_HEIGHT_ABOVE_PLAYER : j.cloud.getY());
				bolt.setScale(TribulationManager.boltScale(j.realm, j.stage));
				bolt.setVisualOnly(true);
				bolt.setDim(j.sinceFlash < TribulationManager.FLASH_EVERY);
				if (!bolt.isDim()) j.sinceFlash = 0;
				level.addFreshEntity(bolt);
			}
			if (j.cloud != null) j.cloud.flash();
		}
		PlayerCultivation c = CultivationManager.get(p);
		// A Heavenly Thunder Body drinks the heavens' bolt in, as it drinks a tribulation's (no harm; its pool filled).
		if (c.hasPhysique(Physique.HEAVENLY_THUNDER_BODY)) {
			c.setQi(c.maxQi());
			return;
		}
		float multiplier = RingOfTranscendenceItem.damageMultiplier(p) * (c.hasPhysique(Physique.ANCIENT_HOLY_BODY) ? TribulationManager.HOLY_STRIKES : 1.0f);
		striking = p.getUUID();
		try {
			p.invulnerableTime = 0;
			if (j.strikes.magic() > 0) {
				p.hurt(level.damageSources().magic(), j.strikes.magic() * multiplier);
				p.invulnerableTime = 0;
			}
			if (p.isAlive() && JUDGED.get(p.getUUID()) == j) p.hurt(level.damageSources().lightningBolt(), j.strikes.bolt() * multiplier);
		} finally {
			striking = null;
		}
	}

	/** The judgement's storm over {@code p}, following it: a tribulation cloud of the realm its strikes are of. */
	private static void updateCloud(ServerPlayer p, Judgement j) {
		ServerLevel level = p.serverLevel();
		if (j.cloud == null || j.cloud.isRemoved() || j.cloud.level() != level) {
			j.removeCloud();
			j.cloud = ModEntities.TRIBULATION_CLOUD.create(level);
			if (j.cloud == null) return;
			j.cloud.configure(j.realm, false);
			j.cloud.setPos(p.getX(), j.cloud.cloudBaseY(p.getY()), p.getZ());
			level.addFreshEntity(j.cloud);
		}
		j.cloud.follow(p.position());
	}

	/** {@code p} has weathered its judgement: a level of the Laws theirs to choose, its body whole, the next Decay ten days on (package-private for the game tests). */
	static void weathered(ServerPlayer p) {
		Judgement j = JUDGED.remove(p.getUUID());
		if (j != null) j.removeCloud();
		PlayerCultivation c = CultivationManager.get(p);
		LawState s = c.laws();
		long now = p.level().getGameTime();
		end(p, s, now);
		p.displayClientMessage(Component.empty(), true); // the strikes' count off the hotbar
		s.setChoices(s.choices() + 1);
		CultivationManager.refresh(p);
		p.setHealth(p.getMaxHealth());
		p.clearFire();
		ServerLevel level = p.serverLevel();
		level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 0.8f);
		level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 0.6f);
		level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 80, 0.6, 1.0, 0.6, 0.15);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_DECAY_WEATHERED).withStyle(ChatFormatting.GOLD));
		p.server.getPlayerList().broadcastSystemMessage(Component.translatable(ModLang.MSG_DECAY_WEATHERED_BROADCAST, p.getDisplayName())
				.withStyle(ChatFormatting.GOLD), false);
	}

	/** The Decay over on {@code s} ({@code p}'s), weathered or failed: no sign left, the next ten days on. */
	private static void end(ServerPlayer p, LawState s, long now) {
		s.setDecay(0, 0);
		s.setDecayDue(now + PERIOD);
		s.setDecayWarned(false);
		applyArmour(p, false);
		CultivationManager.markDirty(p.server);
	}

	/**
	 * ALLOW_DEATH: struck down while its judgement runs (by its strike or anything else), {@code p} lives on a stage lower, at half its
	 * health, the Decay over: the user's pick ("Drop a stage, live"). @return false to hold the death off
	 */
	public static boolean allowDeath(ServerPlayer p) {
		Judgement j = JUDGED.remove(p.getUUID());
		if (j == null) return true;
		j.removeCloud();
		p.displayClientMessage(Component.empty(), true); // the strikes' count off the hotbar
		Component fallen = fall(p);
		CultivationManager.refresh(p);
		p.setHealth(Math.max(1, p.getMaxHealth() * 0.5f));
		p.clearFire();
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.5f, 1.2f);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_DECAY_FAILED, fallen).withStyle(ChatFormatting.DARK_RED));
		p.server.getPlayerList().broadcastSystemMessage(Component.translatable(ModLang.MSG_DECAY_FAILED_BROADCAST, p.getDisplayName())
				.withStyle(ChatFormatting.DARK_RED), false);
		return false;
	}

	/** AFTER_DEATH: a death the judgement couldn't hold off (nothing does: an op's /kill) fails it all the same. */
	public static void onDeath(ServerPlayer p) {
		Judgement j = JUDGED.remove(p.getUUID());
		if (j == null) return;
		j.removeCloud();
		Component fallen = fall(p);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_DECAY_FAILED, fallen).withStyle(ChatFormatting.DARK_RED));
	}

	/**
	 * The judgement failed: {@code p} falls a stage on the highest of its paths (as one devoured does, Physiques#devoured), the Decay over.
	 * @return the rank it stands at now, named
	 */
	private static Component fall(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		end(p, c.laws(), p.level().getGameTime());
		int demon = TwoPaths.demonRank(c, false);
		if (demon >= 0 && demon > TwoPaths.ghostRank(c, false) && demon > TwoPaths.heavenRank(c, false)) {
			DemonPath.fallOneStage(c);
			return DemonPath.rankName(DemonPath.rank(c));
		}
		if (TwoPaths.ghostRank(c, false) > TwoPaths.heavenRank(c, false)) {
			GhostMethods.fallOneStage(c.netherworld());
			Spells.sync(p);
			return GhostMethods.rankName(TwoPaths.ghostRank(c, false));
		}
		c.fallOneStage();
		return PlayerCultivation.rankName(c.getRealm(), c.getStage());
	}

	// --- Comings and goings ---

	/**
	 * JOIN: the Decays' time stood still while {@code p} was away. A sign under way begins its minute again, a judgement under way comes
	 * again a minute on (as the fifth sign's minute), and one fallen due meanwhile comes a minute after the return; the robes' stain put
	 * back on.
	 */
	public static void onJoin(ServerPlayer p) {
		LawState s = CultivationManager.own(p).laws();
		long now = p.level().getGameTime();
		if (s.decaySign() >= JUDGEMENT) s.setDecay(SIGNS, now);
		else if (s.decaySign() > 0) s.setDecay(s.decaySign(), now);
		else if (s.decayDue() > 0 && s.decayDue() < now + SIGN_TICKS) s.setDecayDue(now + SIGN_TICKS);
		applyArmour(p, s.decaySign() > 0);
	}

	/** AFTER_RESPAWN: the new body wears the robes' stain too while a Decay is under way. */
	public static void onRespawn(ServerPlayer p) {
		applyArmour(p, sign(CultivationManager.get(p)) > 0);
	}

	/** Logged out: its judgement's storm goes (the record keeps that one was under way). */
	public static void forget(UUID id) {
		Judgement j = JUDGED.remove(id);
		if (j != null) j.removeCloud();
	}

	public static void clear() {
		JUDGED.values().forEach(Judgement::removeCloud);
		JUDGED.clear();
	}

	private FiveDecays() {}
}
