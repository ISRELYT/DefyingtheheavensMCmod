package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * The Demon Path (the user's ask, 2026-10-23: "you should be able to cultivate both the heaven and demon path as a demon or human, but
 * as demons you have a demon core already however as humans you need to form one after receiving a bloodline of a beast"): a third path
 * beside the heaven and ghost paths (TwoPaths), its realms and stages the heaven path's own (their names kept: the user's word), its
 * bar as long, its bottlenecks where the heaven path's are, its tribulations the heavens' and the harder for it ({@link #TRIBULATION_HARM}).
 * <ul>
 *   <li><b>A demon</b> (Bloodline#isBeast) is born with its demon core: its Demon Path begins at Qi Refining Early, and it is never a
 *       mortal, its core having opened its meridians ({@link #born}: on joining, on choosing its blood, born again).</li>
 *   <li><b>A human</b> forms a demon core only once a beast's bloodline is awakened in it (to come): until then the path is closed to it.</li>
 *   <li><b>No method, pill or ritual</b> carries it: its bloodline does. The bloodline's inheritance teaches the race's own element's
 *       spells as the path climbs ({@link #inherits}): the Tiger's Metal, the Lizard's Earth, the Fox's Fire, the Dragon's Wood.</li>
 *   <li>It is cultivated as the heaven path is, at its own realm's pace, by its race's ({@link #racePace}: the Dragon slowest).</li>
 *   <li>Its body is the mightier ({@link #BODY}); it shares the qi pool, its own share of it smaller ({@link #QI_SHARE}).</li>
 * </ul>
 */
public final class DemonPath {
	/** A body the Demon Path gives is this much mightier than the heaven path's of the same rank (TwoPaths#body). */
	public static final double BODY = 1.2;
	/** The qi pool and its gathering the Demon Path gives, of the heaven path's at the same rank (PlayerCultivation#maxQi). */
	public static final double QI_SHARE = 0.85;
	/** A demon's lifespan, of a human's (Lifespan#span): the beasts are long-lived. */
	public static final double LIFESPAN = 1.5;
	/** The Demon Path's tribulations strike this much the harder: the heavens hate demons. */
	public static final float TRIBULATION_HARM = 1.25f;
	/** A true dragon cultivates the slowest. */
	public static final double DRAGON_PACE = 0.8;

	// --- The race ---

	/** The element a race's inheritance teaches (null for a human): the White Tiger's Metal, the Azure Dragon's Wood. */
	public static QiElement element(Bloodline bloodline) {
		return switch (bloodline) {
			case TIGER -> QiElement.METAL;
			case LIZARD -> QiElement.EARTH;
			case FOX -> QiElement.FIRE;
			case DRAGON -> QiElement.WOOD;
			case HUMAN -> null;
		};
	}

	/** {@code c}'s blood (DemonState#bloodline, kept as the world's Bloodlines says). */
	public static Bloodline bloodline(PlayerCultivation c) {
		return c.demon().bloodline();
	}

	/** One of the demon races. */
	public static boolean isDemon(PlayerCultivation c) {
		return bloodline(c).isBeast();
	}

	/** How fast {@code bloodline} cultivates the Demon Path: the Dragon the slowest, the rest alike. */
	public static double racePace(Bloodline bloodline) {
		return bloodline == Bloodline.DRAGON ? DRAGON_PACE : 1.0;
	}

	// --- Where it stands ---

	/** The Demon Path's rank (PlayerCultivation#rank), or -1: no demon core (a human's, or not begun). */
	public static int rank(PlayerCultivation c) {
		return isDemon(c) ? c.demon().rank() : -1;
	}

	public static Realm realm(int rank) {
		return Realm.byIndex(Math.min(Realm.values().length - 1, Math.max(0, rank) / Stage.values().length));
	}

	public static Stage stage(int rank) {
		return Stage.byIndex(Math.max(0, rank) % Stage.values().length);
	}

	/** The Demon Path's bar through the stage {@code rank} stands for: the heaven path's own (PlayerCultivation#required). 0 for none. */
	public static double required(int rank) {
		return rank < 0 ? 0 : PlayerCultivation.required(realm(rank), stage(rank));
	}

	/** How far through its stage, 0 to 1 (the Cultivation tab's bar). */
	public static double stageProgress(PlayerCultivation c) {
		double required = required(rank(c));
		return required <= 0 ? 0 : Math.min(1, c.demon().cultivation() / required);
	}

	/** Leaving the stage {@code rank} stands for takes a tribulation: Grand Perfection always, every stage from Nascent Soul. */
	public static boolean needsTribulation(int rank) {
		return stage(rank).isLast() || realm(rank).hasStageTribulations();
	}

	public static boolean isMaxed(PlayerCultivation c) {
		return rank(c) >= PlayerCultivation.LAST_RANK;
	}

	/** The bar full at a stage only a tribulation leaves. */
	public static boolean atBottleneck(PlayerCultivation c) {
		int rank = rank(c);
		return rank >= 0 && needsTribulation(rank) && c.demon().cultivation() >= required(rank);
	}

	/** Held at the lower realms' cap, as the heaven path is (PlayerCultivation#isBreakthroughLocked): the Upper Realm wanted. */
	public static boolean locked(PlayerCultivation c) {
		return c.isLowerRealmBound() && !isMaxed(c)
				&& rank(c) + 1 > PlayerCultivation.rank(PlayerCultivation.LOWER_REALM_CAP_REALM, PlayerCultivation.LOWER_REALM_CAP_STAGE);
	}

	/** At a Four Axis stage's bottleneck with its axis of virtue not complete (FourAxis: the cultivator's, every path's). */
	public static boolean heldByAxis(PlayerCultivation c) {
		return atBottleneck(c) && FourAxis.holds(c, rank(c));
	}

	/** Ready for its tribulation. */
	public static boolean canBreakthrough(PlayerCultivation c) {
		return atBottleneck(c) && !isMaxed(c) && !locked(c) && !heldByAxis(c);
	}

	public static Component rankName(int rank) {
		return PlayerCultivation.rankName(realm(rank), stage(rank));
	}

	// --- Cultivating it ---

	/**
	 * Adds {@code amount} to the bar; its stages step on by themselves to a bottleneck, where the bar stops full. Nothing without a demon
	 * core. @return how many stages were gained
	 */
	public static int addCultivation(PlayerCultivation c, double amount) {
		int rank = rank(c);
		if (rank < 0 || amount <= 0) return 0;
		double cultivation = c.demon().cultivation() + amount;
		int gained = 0;
		while (cultivation >= required(rank)) {
			if (needsTribulation(rank)) {
				cultivation = required(rank);
				break;
			}
			cultivation -= required(rank);
			rank++;
			gained++;
		}
		c.demon().set(rank, cultivation);
		return gained;
	}

	/**
	 * What a second's meditation on it gathers before the meditation's own boosts (MeditationManager multiplies those in): its realm's and
	 * stage's own rate, as the heaven path's (PlayerCultivation#meditationCultivationPerSecond), by the race's pace. Nothing while the land
	 * holds the cultivator below its realm (as the heaven path: PlayerCultivation#isSuppressed).
	 */
	public static double perSecond(PlayerCultivation c, double bonus) {
		int rank = rank(c);
		if (rank < 0 || c.sustained(rank) < rank) return 0;
		double rate = realm(rank).getCultivationPerSecond() * stage(rank).getPowerMultiplier() + bonus;
		return rate * c.placeFactor() * c.favourFactor() * c.physiqueCultivationFactor() * StatPoints.cultivationFactor(c) * racePace(bloodline(c));
	}

	/** Its tribulation survived: on to the next stage, its bar empty. */
	public static boolean breakthrough(PlayerCultivation c) {
		int rank = rank(c);
		if (rank < 0 || rank >= PlayerCultivation.LAST_RANK) return false;
		c.demon().set(rank + 1, 0);
		return true;
	}

	/** Its tribulation failed: a stage down, its cultivation there lost. */
	public static void fallOneStage(PlayerCultivation c) {
		int rank = rank(c);
		if (rank < 0) return;
		c.demon().set(Math.max(0, rank - 1), 0);
	}

	/** Falls to {@code rank} (a nascent soul lost, a core detonated), its cultivation there lost; one at or below it is untouched. */
	public static void fallTo(PlayerCultivation c, int rank) {
		if (rank(c) <= rank) return;
		c.demon().set(rank, 0);
	}

	// --- Its pool's share, its inheritance ---

	/** The qi pool the Demon Path gives (before the pool's own factors: PlayerCultivation#maxQi), 0 with none. */
	public static double poolBase(PlayerCultivation c) {
		int rank = c.wielded(rank(c));
		return rank < 0 ? 0 : CultivationStats.maxQi(realm(rank), stage(rank)) * QI_SHARE;
	}

	/** The qi a second the Demon Path gathers (PlayerCultivation#qiGatherAt), 0 with none. */
	public static double gatherBase(PlayerCultivation c) {
		int rank = c.wielded(rank(c));
		return rank < 0 ? 0 : CultivationStats.qiGather(realm(rank), stage(rank)) * QI_SHARE;
	}

	/**
	 * Whether {@code c}'s bloodline inheritance teaches {@code spell}: one of its race's element, once the Demon Path has reached the stage
	 * the spell's layer stands for (Spell#realmNeeded), as a heaven method of that element would; the living's (the dead's paths are the
	 * dead's).
	 */
	public static boolean inherits(PlayerCultivation c, Spell spell) {
		QiElement element = element(bloodline(c));
		return element != null && spell.element() == element && !c.netherworld().ghost() && rank(c) >= spell.realmNeeded();
	}

	// --- Born with it ---

	/**
	 * {@code player}'s record takes its blood from the world's Bloodlines; a demon's demon core is there from birth: its Demon Path
	 * begun at Qi Refining Early if it wasn't, and (its meridians opened by its core) never a mortal, its heaven path at Qi Refining
	 * Early too. Shattered Meridians are no demon's ({@link #mendShattered}). Told the first time. On joining, on choosing or being given
	 * its blood, and born again.
	 */
	public static void born(ServerPlayer player) {
		if (Possession.isHeld(player.getUUID())) return;
		PlayerCultivation c = CultivationManager.own(player);
		Bloodline bloodline = Bloodlines.of(player.server, player.getUUID());
		boolean changed = c.demon().bloodline() != bloodline;
		c.demon().setBloodline(bloodline);
		if (bloodline.isBeast()) changed |= mendShattered(player, c);
		if (bloodline.isBeast() && !c.demon().begun()) {
			c.demon().set(0, 0);
			if (c.isMortal() && !c.hasShatteredMeridians()) {
				c.awaken(0);
				c.setMortal(false);
			}
			player.sendSystemMessage(Component.translatable(ModLang.MSG_DEMON_CORE, bloodline.getDisplayName(),
					element(bloodline).getDisplayName()).withStyle(ChatFormatting.DARK_RED));
			player.playNotifySound(SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.4f, 1.6f);
			Journal.note(player, "demon_path", ModLang.JOURNAL_DEMON_PATH, Component.translatable(ModLang.JOURNAL_DEMON_PATH_TEXT,
					bloodline.getDisplayName(), element(bloodline).getDisplayName()));
			changed = true;
		}
		if (changed) {
			CultivationManager.markDirty(player.server);
			CultivationManager.refresh(player);
			Spells.sync(player);
		}
	}

	/**
	 * A demon's core opens the meridians it is born with, so no demon keeps Shattered Meridians: one rolled them as it joined, before its
	 * blood was chosen, is given another physique (rolled again until it isn't those: Physiques#give tells it), and the core it was born
	 * with opens them (as {@link #born}), its Demon Path begun or not. Before the sweep of 2026-10-23 such a demon stayed a mortal with a
	 * demon core it could never cultivate. Not while its soul is in another's body (Physiques#give would give the body the physique): it
	 * is mended the next time it is born. Returns whether anything changed.
	 */
	private static boolean mendShattered(ServerPlayer player, PlayerCultivation c) {
		if (!c.hasShatteredMeridians() || CultivationManager.get(player) != c) return false;
		Physique physique = Physique.roll(player.getRandom());
		while (physique == Physique.SHATTERED_MERIDIANS) physique = Physique.roll(player.getRandom());
		Physiques.give(player, physique);
		if (c.isMortal() && c.demon().begun()) {
			c.awaken(0);
			c.setMortal(false);
		}
		return true;
	}

	/** A stage or more gained ({@code before} the rank it stood at): told, the spells its inheritance now teaches put on the wheel. */
	public static void afterRank(ServerPlayer player, int before) {
		PlayerCultivation c = CultivationManager.get(player);
		int rank = rank(c);
		player.displayClientMessage(Component.translatable(ModLang.MSG_DEMON_STAGE_UP, rankName(rank)).withStyle(ChatFormatting.RED), true);
		boolean any = false;
		for (Spell spell : Spell.values()) {
			if (spell.element() == null || spell.element() != element(bloodline(c))) continue;
			if (spell.realmNeeded() <= before || spell.realmNeeded() > rank) continue;
			c.netherworld().addToWheel(spell);
			player.sendSystemMessage(Component.translatable(ModLang.MSG_DEMON_INHERITS, spell.getDisplayName()).withStyle(ChatFormatting.RED));
			any = true;
		}
		if (any) player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.7f);
		CultivationManager.markDirty(player.server);
		CultivationManager.refresh(player);
		Spells.sync(player);
	}

	private DemonPath() {}
}
