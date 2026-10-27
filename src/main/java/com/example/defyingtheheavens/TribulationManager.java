package com.example.defyingtheheavens;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Heavenly Tribulation: the trial a player must survive to cross into the next major realm,
 * and, from Nascent Soul on, into every minor stage.
 * The trial itself is runtime-only and never survives a server restart or a disconnect, but leaving is no way out: the
 * player's cultivation is marked (and saved at once) while a trial runs, and a player who returns still marked has failed
 * it (see {@link #onJoin}).
 * <p>
 * <b>The Netherworld's tribulation</b> (the user's ask, 2026-10-19) is the ghost path's own ({@link #startGhost}; GhostMethods' bar
 * at its bottleneck): the same strikes at the same breakthroughs, of yin lightning (ModDamageTypes#YIN_LIGHTNING and #YIN_TRIBULATION)
 * from a dark teal storm, cyan bolts. It comes wherever its cultivator stands, the Netherworld too (it is the dead's realm's, not the
 * heavens'), and it reaches the dead, each strike the same share of their hearts as of a living body's in the same armour (the common
 * dead, with no hands for armour, as if in iron: {@link #GHOST_FORM_ARMOR}). Failed, the ghost path falls a stage: the living die,
 * the dead scatter ({@link #onScattered}).
 */
public final class TribulationManager {
	private static final int FIRST_STRIKE_DELAY = 60;  // 3 s for the local cloud to gather before the first bolt
	public static final int STRIKE_INTERVAL = 60;      // 3 s between strikes, while a tribulation's strikes fit so into its time
	/**
	 * The longest a tribulation lasts, from its clouds gathering to the breakthrough (the user's ask, 2026-10-22: 30 seconds ever, but
	 * the lifespan tribulation): its strikes are fitted into it ({@link #strikeSpan}), several a tick for the greatest.
	 */
	public static final int MAX_TICKS = 30 * 20;
	/** Strikes closer than this (ticks) share the bolt drawn for the first: ten bolts a second at most (each strike's harm lands all the same). */
	public static final int MIN_BOLT_INTERVAL = 2;
	/** A storm's bolts flash the whole sky at most this often (ticks); those between are dim (TribulationLightning#setDim): no strobing. */
	public static final int FLASH_EVERY = 20;
	private static final int FINISH_DELAY = 20;        // 1 s after the last strike before the breakthrough completes
	/** All a tribulation's strikes drunk in by a Heavenly Thunder Body (Physique): this share of the new stage's cultivation. */
	public static final double THUNDER_SHARE = 0.6;
	/**
	 * The armour one of the common dead (no hands for armour: Incorporeal#isRuledGhost) turns the Netherworld's tribulation's bolt with
	 * at least: a full iron set's, as the balance table asks of the living at Core Formation, the last tribulation one of them meets
	 * (a ghost path of Core Formation is a Ghost King's, whose hands are its own).
	 */
	public static final float GHOST_FORM_ARMOR = 15.0f;
	/** The heavens hate a Primordial Chaos Body (Physique): its every tribulation strikes this many times as often (rounded up). */
	public static final double CHAOS_STRIKES = 1.5;
	/** An Ancient Holy Body (Physique) takes a tribulation's strikes at this share. */
	public static final float HOLY_STRIKES = 0.75f;
	/** What a Heaven-Defying Body (Physique) is left with by the strike that would have killed it: one heart. */
	public static final float DEFIED_HEALTH = 2.0f;

	/** One tribulation: a set number of identical strikes, each heavenly lightning (armor applies) plus magic (it doesn't). */
	public record Strikes(int count, float bolt, float magic) {}

	private static final class Trial {
		final Realm targetRealm;
		final Stage targetStage;
		/** The ghost path's (the Netherworld's tribulation), not the heaven path's. */
		final boolean ghost;
		/** The Demon Path's (DemonPath): the heavens' lightning, the harder ({@link DemonPath#TRIBULATION_HARM}). */
		boolean demon;
		final Strikes strikes;
		/** Ticks from its first strike to its last, all of them evenly spread over it ({@link #strikeSpan}). */
		final int span;
		int strikesLeft;
		/** Ticks since its first strike (below 0 while its clouds gather). */
		int sinceFirst = -FIRST_STRIKE_DELAY;
		int finishIn = FINISH_DELAY;
		int age;
		/** Ticks since its last bolt that flashed the sky ({@link #FLASH_EVERY}). */
		int sinceFlash = FLASH_EVERY;
		/** Ticks since its last bolt was drawn ({@link #MIN_BOLT_INTERVAL}). */
		int sinceBolt = MIN_BOLT_INTERVAL;
		/** A Heavenly Thunder Body's share of the strikes drunk in (Physique): {@link #THUNDER_SHARE} of the new stage's cultivation at most. */
		double absorbed;
		TribulationCloud cloud;

		void removeCloud() {
			if (cloud != null) cloud.discard();
			cloud = null;
		}

		/** {@code hated}: a Primordial Chaos Body's, struck {@link #CHAOS_STRIKES} times as often. */
		Trial(Realm targetRealm, Stage targetStage, boolean ghost, boolean hated) {
			this.targetRealm = targetRealm;
			this.targetStage = targetStage;
			this.ghost = ghost;
			this.strikes = strikes(targetRealm, targetStage, hated);
			this.strikesLeft = strikes.count();
			// A Primordial Chaos Body's extra strikes come in the same time, the faster (no tribulation outlasts MAX_TICKS: the user's ask,
			// 2026-10-22; they used to take the longer).
			this.span = strikeSpan(strikes.count());
		}

		Component targetName() {
			return PlayerCultivation.rankName(targetRealm, targetStage);
		}
	}

	private static final Map<UUID, Trial> ACTIVE = new HashMap<>();

	public static boolean isActive(UUID id) {
		return ACTIVE.containsKey(id);
	}

	/** Whether {@code id} faces the Netherworld's tribulation (the ghost path's) right now. */
	public static boolean isGhostTrial(UUID id) {
		Trial t = ACTIVE.get(id);
		return t != null && t.ghost;
	}

	/*
	 * Balance (retuned 2026-10-22 for the user's 30-second cap, and for the realms after Four Axis; the strike counts are the user's,
	 * 2026-10-20: Heavenly Being 50, Four Axis 100, the realms below in step, and those after it each twice the one before). A set
	 * number of strikes lands after 3 s of gathering clouds, 3 s apart while they fit, else all fitted evenly into the 26 s left
	 * before the breakthrough's last second ({@link #strikeSpan}: Nascent Soul's 18 some 1.5 s apart, Heavenly Being's 50 a half
	 * second, Four Axis's 100 a quarter, True Immortal's 3,200 six a tick; strikes in the same tenth of a second share the bolt drawn).
	 * From Foundation Building on, the Qi Sustenance ability (if left switched on) keeps vanilla's fast regeneration running, 2
	 * health a second, so only damage above that wears the player down. (Since 2026-10-22 a body mends at its realm's pace,
	 * CultivationStats#mendFactor, here too at the user's word, "its okay if they become easier": from Nascent Soul up the table
	 * below, weighed at vanilla's 2 a second, asks less than it says.) Lightning is cut by armor (up to 80%, and the more the
	 * smaller the hit); the magic part ignores armor but not Protection enchantments, Resistance or absorption.
	 * Every strike of a tribulation is the same, so the outcome is certain. Tuned (deterministic simulation of vanilla's damage
	 * rules tick by tick, starting at full health, no potions/apples/totems; it reproduced the old table exactly; scratchpad
	 * trib-tune-1022b.ps1 of session 59448793) so a cultivator survives with this full armor set and dies with the step below
	 * (netherite counts as diamond):
	 *   -> Foundation Building   2 strikes    nothing needed at full health (16 lightning against 22.6 health)
	 *   -> Core Formation        6 strikes    iron (leather dies)
	 *   -> Nascent Soul         18 strikes    diamond, or iron with Protection II (plain iron dies) - also NS Mid
	 *   -> NS Late              18 strikes    diamond with Protection I, or iron with Protection II (plain diamond dies)
	 *   -> NS Grand Perfection  18 strikes    diamond with Protection II (Protection I dies)
	 *   -> Heavenly Being       50 strikes    Protection III (II dies) - also HB Mid and Late
	 *   -> HB Grand Perfection  50 strikes    Protection IV (III dies)
	 * Protection IV is the best gear there is, so Four Axis asks for a beacon on top of it:
	 *   -> Four Axis           100 strikes    Protection IV + Resistance I (Protection IV alone dies) - also FA Mid and Late
	 *   -> FA Grand Perfection 100 strikes    Protection IV + Resistance II (Resistance I with Regeneration I dies; in 30 s
	 *                                         Regeneration I heals too little to tell one from the other, as it did in 35)
	 * and every tribulation after it asks the same, a beacon's whole Resistance on Protection IV, against the body's own might:
	 *   -> Dao Integration     200 strikes    and every stage of it, Void Shattering's 400, Sacred Vessel's 800, Entering
	 *      ... True Immortal 3,200            Nirvana's 1,600, True Immortal's 3,200 (Resistance I with Regeneration I dies)
	 *   -> the ladder past it  3,200 strikes  Earth Immortal to Heavenly Lord (phase 3), each strike the heavier, asking the same
	 * Golden apples, Resistance/Regeneration potions and totems let a player get through a step below these,
	 * and each worn Ring of Transcendence halves all strike damage on top of the table.
	 * The Netherworld's tribulation strikes as this table says for the living (its yin lightning turned by armour as the heavens'
	 * bolts are, the yin in it passing armour as magic does); the dead take the same share of their hearts (GhostSystem#livingShare).
	 * The common dead's iron (GHOST_FORM_ARMOR) only meets the tribulations into Foundation Building and Core Formation, iron being
	 * what the living need for Core Formation's: no easier for the dead than for a life in iron.
	 */

	/** The tribulation for breaking through into this realm and stage. Before Nascent Soul only Early is ever a target. */
	public static Strikes strikes(Realm target, Stage stage) {
		return switch (target) {
			case QI_REFINING -> new Strikes(0, 0.0f, 0.0f); // never a breakthrough target
			case FOUNDATION_BUILDING -> new Strikes(2, 8.0f, 0.0f);
			case CORE_FORMATION -> new Strikes(6, 12.5f, 1.5f);
			case NASCENT_SOUL -> switch (stage) {
				case EARLY -> new Strikes(18, 14.25f, 1.5f);
				case MID -> new Strikes(18, 19.0f, 2.0f);
				case LATE -> new Strikes(18, 11.75f, 5.0f); // more of it magic, so Protection tells diamond from diamond and Protection I
				case GRAND_PERFECTION -> new Strikes(18, 14.75f, 6.25f);
			};
			case HEAVENLY_BEING -> switch (stage) {
				case EARLY -> new Strikes(50, 4.25f, 3.5f);
				case MID -> new Strikes(50, 5.25f, 4.5f);
				case LATE -> new Strikes(50, 5.75f, 4.5f);
				case GRAND_PERFECTION -> new Strikes(50, 8.25f, 6.75f);
			};
			case FOUR_AXIS -> switch (stage) { // even at Grand Perfection, armor still reaches its 80% cap
				case EARLY -> new Strikes(100, 2.75f, 5.25f);
				case MID -> new Strikes(100, 3.75f, 7.0f);
				case LATE -> new Strikes(100, 4.0f, 7.5f);
				case GRAND_PERFECTION -> new Strikes(100, 5.75f, 10.5f);
			};
			// Each realm's Early is the might of the realm before's Grand Perfection to bear (Void Shattering's and True Immortal's the
			// smaller beside their own stages after the leap in power).
			case DAO_INTEGRATION -> switch (stage) {
				case EARLY -> new Strikes(200, 3.0f, 5.75f);
				case MID -> new Strikes(200, 4.25f, 8.0f);
				case LATE -> new Strikes(200, 4.5f, 8.5f);
				case GRAND_PERFECTION -> new Strikes(200, 5.0f, 9.25f);
			};
			case VOID_SHATTERING -> switch (stage) {
				case EARLY -> new Strikes(400, 2.75f, 5.0f);
				case MID -> new Strikes(400, 7.5f, 13.75f);
				case LATE -> new Strikes(400, 8.0f, 15.0f);
				case GRAND_PERFECTION -> new Strikes(400, 8.75f, 16.25f);
			};
			case SACRED_VESSEL -> switch (stage) {
				case EARLY -> new Strikes(800, 4.75f, 8.75f);
				case MID -> new Strikes(800, 7.25f, 13.5f);
				case LATE -> new Strikes(800, 8.0f, 14.75f);
				case GRAND_PERFECTION -> new Strikes(800, 8.5f, 16.0f);
			};
			case ENTERING_NIRVANA -> switch (stage) {
				case EARLY -> new Strikes(1600, 4.75f, 8.75f);
				case MID -> new Strikes(1600, 7.0f, 13.25f);
				case LATE -> new Strikes(1600, 7.75f, 14.5f);
				case GRAND_PERFECTION -> new Strikes(1600, 8.5f, 15.75f);
			};
			case TRUE_IMMORTAL -> switch (stage) {
				case EARLY -> new Strikes(3200, 4.5f, 8.5f);
				case MID -> new Strikes(3200, 35.25f, 65.0f);
				case LATE -> new Strikes(3200, 38.75f, 72.0f);
				case GRAND_PERFECTION -> new Strikes(3200, 42.0f, 78.0f);
			};
			// The ladder past True Immortal (phase 3, 2026-10-26): True Immortal's 3,200 strikes still (more would only weigh on the
			// server), each the heavier as the body doubles, asking the same beacon's whole Resistance on Protection IV (trib-tune-1026.ps1,
			// which gives True Immortal's rows above again).
			case EARTH_IMMORTAL -> switch (stage) {
				case EARLY -> new Strikes(3200, 46.0f, 85.0f);
				case MID -> new Strikes(3200, 70.0f, 130.0f);
				case LATE -> new Strikes(3200, 77.0f, 143.0f);
				case GRAND_PERFECTION -> new Strikes(3200, 84.0f, 156.0f);
			};
			case HEAVEN_IMMORTAL -> switch (stage) {
				case EARLY -> new Strikes(3200, 91.0f, 169.0f);
				case MID -> new Strikes(3200, 140.0f, 261.0f);
				case LATE -> new Strikes(3200, 154.0f, 287.0f);
				case GRAND_PERFECTION -> new Strikes(3200, 168.0f, 313.0f);
			};
			case GOLDEN_IMMORTAL -> switch (stage) {
				case EARLY -> new Strikes(3200, 182.0f, 339.0f);
				case MID -> new Strikes(3200, 306.0f, 374.0f);
				case LATE -> new Strikes(3200, 340.0f, 415.0f);
				case GRAND_PERFECTION -> new Strikes(3200, 374.0f, 458.0f);
			};
			case TAIYI_GOLDEN_IMMORTAL -> switch (stage) {
				case EARLY -> new Strikes(3200, 409.0f, 500.0f);
				case MID -> new Strikes(3200, 458.0f, 851.0f);
				case LATE -> new Strikes(3200, 507.0f, 942.0f);
				case GRAND_PERFECTION -> new Strikes(3200, 557.0f, 1035.0f);
			};
			case PROFOUND_SOVEREIGN -> switch (stage) {
				case EARLY -> new Strikes(3200, 608.0f, 1129.0f);
				case MID -> new Strikes(3200, 916.0f, 1701.0f);
				case LATE -> new Strikes(3200, 1015.0f, 1885.0f);
				case GRAND_PERFECTION -> new Strikes(3200, 1115.0f, 2070.0f);
			};
			case HEAVENLY_LORD -> switch (stage) {
				case EARLY -> new Strikes(3200, 1216.0f, 2259.0f);
				case MID -> new Strikes(3200, 1832.0f, 3402.0f);
				case LATE -> new Strikes(3200, 2029.0f, 3769.0f);
				case GRAND_PERFECTION -> new Strikes(3200, 2230.0f, 4141.0f);
			};
		};
	}

	/** {@link #strikes(Realm, Stage)}, for a Primordial Chaos Body ({@code hated}: Physique) {@link #CHAOS_STRIKES} times as many, rounded up. */
	public static Strikes strikes(Realm target, Stage stage, boolean hated) {
		Strikes base = strikes(target, stage);
		return hated ? new Strikes((int) Math.ceil(base.count() * CHAOS_STRIKES), base.bolt(), base.magic()) : base;
	}

	/**
	 * Ticks from the first strike of a tribulation of {@code count} to its last, all of them evenly spread over it (the user's asks:
	 * 2026-10-20, so the greater tribulations don't take an age; 2026-10-22, none longer than {@link #MAX_TICKS}): {@link
	 * #STRIKE_INTERVAL} apart while they fit so, else fitted into what the gathering and the breakthrough's last second leave of it,
	 * several a tick for the greatest ({@link #strikesBy}): Nascent Soul's 18 some 31 ticks apart, Heavenly Being's 50 every 10.6,
	 * Four Axis's 100 every 5.3, True Immortal's 3,200 six a tick.
	 */
	public static int strikeSpan(int count) {
		if (count <= 1) return 0;
		return Math.min((count - 1) * STRIKE_INTERVAL, MAX_TICKS - FIRST_STRIKE_DELAY - FINISH_DELAY);
	}

	/** How many of a tribulation of {@code count} strikes spread over {@code span} ticks ({@link #strikeSpan}) have landed {@code since} ticks after its first. */
	public static int strikesBy(int count, int span, int since) {
		if (since < 0) return 0;
		if (span <= 0) return count;
		return (int) Math.min(count, (long) since * (count - 1) / span + 1);
	}

	/** How long a tribulation of {@code count} strikes lasts, from its clouds gathering to the breakthrough (ticks): {@link #MAX_TICKS} at most. */
	public static int duration(int count) {
		return FIRST_STRIKE_DELAY + strikeSpan(count) + FINISH_DELAY;
	}

	/** Entry point from the Breakthrough button. */
	public static void start(ServerPlayer player) {
		if (!player.isAlive()) return; // e.g. a modified client sending the packet from the death screen
		if (isActive(player.getUUID())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_TRIB_BUSY), true);
			return;
		}
		if (ModDimensions.isInnerRealm(player.level().dimension())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_INNER_TRIBULATION), true);
			return;
		}
		// The heavens send their lightning only where they see: not into the realm of the dead.
		if (ModDimensions.isUnseenByHeaven(player.level().dimension())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_HEAVENS_BLIND), true);
			return;
		}
		// The heavens strike the cultivator, body and soul together: not a soul out of its body, nor one possessing another's.
		if (SoulProjection.isActive(player.getUUID())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_BUSY), true);
			return;
		}
		// Nor a clone (Clones): here too, as naming an obsession (Obsession#name) comes straight here.
		if (Clones.inClone(player)) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_CLONE_CANNOT), true);
			return;
		}
		PlayerCultivation c = CultivationManager.get(player);
		if (c.isAtBottleneck() && c.isBreakthroughLocked()) { // past the lower realms' cap; into True Immortal and past it, the Immortal Realm's alone
			player.displayClientMessage(c.lockLine(), true);
			return;
		}
		// A new realm waits on a heaven method of it too (HeavenMethods#opensWay): said in chat, so a pill also wanting is said as well.
		boolean held = c.isHeldByMethod();
		if (held) player.sendSystemMessage(HeavenMethods.gateLine(c).copy().withStyle(net.minecraft.ChatFormatting.GOLD));
		if (c.isAtBottleneck() && c.isMissingBreakthroughPill()) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_NEED_PILL,
					PillItem.breakthroughPillFor(c.breakthroughRealm()).getDescription(), c.breakthroughRealm().getDisplayName()), true);
			return;
		}
		if (held) return;
		if (c.isHeldByAxis()) { // a Four Axis stage's axis of virtue not yet complete (FourAxis)
			player.sendSystemMessage(FourAxis.gateLine(c, PlayerCultivation.rank(c.getRealm(), c.getStage())).copy().withStyle(net.minecraft.ChatFormatting.GOLD));
			return;
		}
		if (!c.canBreakthrough()) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_NOT_READY), true);
			return;
		}
		// Into Heavenly Being (or on from there, for one with none named yet), the obsession is named first (Obsession): its
		// screen comes up, and naming it brings the tribulation.
		if (Obsession.asks(c, PlayerCultivation.rank(c.getRealm(), c.getStage()))) {
			Obsession.ask(player, false);
			return;
		}

		MeditationManager.stop(player, false); // no-op if not meditating
		Trial trial = new Trial(c.breakthroughRealm(), c.breakthroughStage(), false, c.hasPhysique(Physique.PRIMORDIAL_CHAOS_BODY));
		ACTIVE.put(player.getUUID(), trial);
		// Written to disk now, not at the next autosave, so not even closing the game this instant gets out of the trial.
		c.setInTribulation(true);
		CultivationManager.markDirty(player.server);
		player.server.overworld().getDataStorage().save();

		updateCloud(player, trial);
		ModPackets.sendTribulation(player, true, trial.strikesLeft, trial.targetRealm, trial.targetStage, false);
		player.playNotifySound(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0f, 0.4f);
		player.sendSystemMessage(trial.strikesLeft == 1
				? Component.translatable(ModLang.MSG_TRIB_START_SINGLE, trial.targetName())
				: Component.translatable(ModLang.MSG_TRIB_START, trial.targetName(), trial.strikesLeft));
	}

	/**
	 * Entry point from the Breakthrough button with the ghost path drawn in (TwoPaths#cultivatesGhost): the Netherworld's
	 * tribulation, wherever its cultivator stands (the Netherworld too).
	 */
	public static void startGhost(ServerPlayer player) {
		if (!player.isAlive()) return;
		if (isActive(player.getUUID())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_TRIB_BUSY), true);
			return;
		}
		if (ModDimensions.isInnerRealm(player.level().dimension())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_INNER_TRIBULATION), true);
			return;
		}
		if (Clones.inClone(player)) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_CLONE_CANNOT), true);
			return;
		}
		if (SoulProjection.isActive(player.getUUID())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_BUSY), true);
			return;
		}
		PlayerCultivation c = CultivationManager.get(player);
		if (GhostMethods.heldByPlace(c)) { // into True Immortal and past it, the Immortal Realm's alone, on the ghost path too
			player.displayClientMessage(c.lockLine(GhostMethods.ghostRank(c.netherworld()) + 1), true);
			return;
		}
		if (GhostMethods.heldByMethod(c)) {
			player.sendSystemMessage(GhostMethods.gateLine(c).copy().withStyle(net.minecraft.ChatFormatting.GOLD));
			return;
		}
		if (GhostMethods.heldByAxis(c)) { // a Four Axis stage's axis of virtue not yet complete (FourAxis)
			player.sendSystemMessage(FourAxis.gateLine(c, GhostMethods.ghostRank(c.netherworld())).copy().withStyle(net.minecraft.ChatFormatting.GOLD));
			return;
		}
		if (!GhostMethods.canBreakthrough(c)) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_NOT_READY), true);
			return;
		}
		if (Obsession.asks(c, GhostMethods.ghostRank(c.netherworld()))) { // as the heaven path's (Obsession): one obsession, either path
			Obsession.ask(player, true);
			return;
		}

		MeditationManager.stop(player, false);
		int target = GhostMethods.ghostRank(c.netherworld()) + 1;
		int stages = Stage.values().length;
		Trial trial = new Trial(Realm.byIndex(target / stages), Stage.byIndex(target % stages), true, c.hasPhysique(Physique.PRIMORDIAL_CHAOS_BODY));
		ACTIVE.put(player.getUUID(), trial);
		c.netherworld().setInGhostTribulation(true); // saved at once, as the heaven path's mark is: leaving fails it
		CultivationManager.markDirty(player.server);
		player.server.overworld().getDataStorage().save();

		updateCloud(player, trial);
		ModPackets.sendTribulation(player, true, trial.strikesLeft, trial.targetRealm, trial.targetStage, true);
		player.playNotifySound(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0f, 0.3f);
		player.playNotifySound(SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5f, 0.5f);
		player.sendSystemMessage((trial.strikesLeft == 1
				? Component.translatable(ModLang.MSG_GHOST_TRIB_START_SINGLE, trial.targetName())
				: Component.translatable(ModLang.MSG_GHOST_TRIB_START, trial.targetName(), trial.strikesLeft)).withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
	}

	/**
	 * Entry point from the Breakthrough button with the Demon Path drawn in (TwoPaths#cultivatesDemon): the heavens' tribulation, as the
	 * heaven path's but for the Demon Path's stage and the harder ({@link DemonPath#TRIBULATION_HARM}). No pill, ritual or method holds it:
	 * the lower realms' cap and the axes of virtue do.
	 */
	public static void startDemon(ServerPlayer player) {
		if (!player.isAlive()) return;
		if (isActive(player.getUUID())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_TRIB_BUSY), true);
			return;
		}
		if (ModDimensions.isInnerRealm(player.level().dimension())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_INNER_TRIBULATION), true);
			return;
		}
		if (ModDimensions.isUnseenByHeaven(player.level().dimension())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_HEAVENS_BLIND), true);
			return;
		}
		if (SoulProjection.isActive(player.getUUID())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_BUSY), true);
			return;
		}
		if (Clones.inClone(player)) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_CLONE_CANNOT), true);
			return;
		}
		PlayerCultivation c = CultivationManager.get(player);
		if (DemonPath.atBottleneck(c) && DemonPath.locked(c)) {
			player.displayClientMessage(c.lockLine(DemonPath.rank(c) + 1), true);
			return;
		}
		if (DemonPath.heldByAxis(c)) {
			player.sendSystemMessage(FourAxis.gateLine(c, DemonPath.rank(c)).copy().withStyle(net.minecraft.ChatFormatting.GOLD));
			return;
		}
		if (DemonPath.heldByBlood(c)) { // the realm ahead shut for want of its race's blood (DemonBlood)
			player.sendSystemMessage(DemonPath.bloodGateLine(c).copy().withStyle(net.minecraft.ChatFormatting.GOLD));
			return;
		}
		if (DemonPath.heldByLaws(c)) { // the ladder past True Immortal shut for want of the Laws (Laws, phase 3)
			player.sendSystemMessage(DemonPath.lawGateLine(c).copy().withStyle(net.minecraft.ChatFormatting.GOLD));
			return;
		}
		if (!DemonPath.canBreakthrough(c)) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_NOT_READY), true);
			return;
		}
		// As the heaven path's (Obsession): one obsession, every path (the screen's answer comes back as the heaven path's, Obsession#name
		// telling the Demon Path's by what is drawn in).
		if (Obsession.asks(c, DemonPath.rank(c))) {
			Obsession.ask(player, false);
			return;
		}
		MeditationManager.stop(player, false);
		int target = DemonPath.rank(c) + 1;
		Trial trial = new Trial(DemonPath.realm(target), DemonPath.stage(target), false, c.hasPhysique(Physique.PRIMORDIAL_CHAOS_BODY));
		trial.demon = true;
		ACTIVE.put(player.getUUID(), trial);
		c.demon().setInTribulation(true); // saved at once, as the heaven path's mark is: leaving fails it
		CultivationManager.markDirty(player.server);
		player.server.overworld().getDataStorage().save();

		updateCloud(player, trial);
		ModPackets.sendTribulation(player, true, trial.strikesLeft, trial.targetRealm, trial.targetStage, false);
		player.playNotifySound(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0f, 0.35f);
		player.playNotifySound(SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.6f, 0.7f);
		player.sendSystemMessage((trial.strikesLeft == 1
				? Component.translatable(ModLang.MSG_DEMON_TRIB_START_SINGLE, trial.targetName())
				: Component.translatable(ModLang.MSG_DEMON_TRIB_START, trial.targetName(), trial.strikesLeft)).withStyle(net.minecraft.ChatFormatting.RED));
	}

	public static void tick(MinecraftServer server) {
		if (ACTIVE.isEmpty()) return;
		List<ServerPlayer> survivors = new ArrayList<>();

		// Iterate a copy: a strike can kill the player, and the death handler removes the trial.
		for (Map.Entry<UUID, Trial> entry : new ArrayList<>(ACTIVE.entrySet())) {
			UUID id = entry.getKey();
			Trial t = entry.getValue();
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (p == null) {
				forget(id);
				continue;
			}
			if (!p.isAlive()) continue; // onDeath() handles failure

			t.age++;
			t.sinceFlash++;
			t.sinceBolt++;
			updateCloud(p, t);

			if (t.strikesLeft > 0) {
				// Every strike due by now lands (several a tick for the greatest tribulations: strikeSpan).
				int due = strikesBy(t.strikes.count(), t.span, ++t.sinceFirst);
				boolean struck = false;
				while (t.strikes.count() - t.strikesLeft < due) {
					strike(p, t);
					if (ACTIVE.get(id) != t) break; // died (or scattered) just now
					t.strikesLeft--;
					struck = true;
				}
				if (ACTIVE.get(id) != t) continue;
				if (struck) ModPackets.sendTribulation(p, true, t.strikesLeft, t.targetRealm, t.targetStage, t.ghost);
			} else if (--t.finishIn <= 0) {
				survivors.add(p); // the last strike has faded
				continue;
			}

			if (t.age % 20 == 0) {
				ModPackets.sendTribulation(p, true, t.strikesLeft, t.targetRealm, t.targetStage, t.ghost);
			}
		}
		survivors.forEach(TribulationManager::succeed);
	}

	/**
	 * How much thicker a tribulation's bolt is drawn than vanilla's: as its cloud grows with the realm (TribulationCloud#stormScale,
	 * {@link TribulationCloud#SCALE_PER_TIER} a realm past Foundation Building), and a quarter of that more for each stage within
	 * the realm (the user's ask, 2026-10-19). Foundation Building x1, Core Formation x1.45, Nascent Soul Early x1.9 to Grand
	 * Perfection x2.24, Four Axis Grand Perfection x3.14. Only its thickness: its zig-zag stays vanilla's (the user's word, the
	 * same day, after the whole bolt scaled up sprawled across the sky).
	 */
	public static float boltScale(Realm target, Stage stage) {
		int tier = Math.max(0, target.ordinal() - Realm.FOUNDATION_BUILDING.ordinal());
		return 1.0f + TribulationCloud.SCALE_PER_TIER * (tier + stage.ordinal() / (float) Stage.values().length);
	}

	/**
	 * A strike: the one bolt the cloud sends, at the cultivator (the only lightning a tribulation has: the scattered bolts it once
	 * threw about for show were taken out at the user's word, 2026-10-19).
	 */
	private static void strike(ServerPlayer p, Trial t) {
		ServerLevel level = p.serverLevel();
		// Strikes closer together than MIN_BOLT_INTERVAL share the first one's bolt (and its sparks and sound): each one's harm lands.
		boolean drawn = t.sinceBolt >= MIN_BOLT_INTERVAL;
		if (drawn) {
			t.sinceBolt = 0;
			TribulationLightning bolt = ModEntities.TRIBULATION_LIGHTNING.create(level); // drawn bright blue (cyan: the Netherworld's) by the client
			if (bolt != null) {
				bolt.moveTo(p.getX(), p.getY(), p.getZ());
				bolt.setCloudBaseY(t.cloud == null ? p.getY() + TribulationCloud.BASE_HEIGHT_ABOVE_PLAYER : t.cloud.getY());
				bolt.setScale(boltScale(t.targetRealm, t.targetStage));
				bolt.setGhost(t.ghost);
				bolt.setVisualOnly(true); // 1.20.1 bolts always deal a flat 5, so the tribulation's hit is applied by hand below
				// Strikes in quick succession: only one a second lights the whole sky (it would strobe otherwise).
				bolt.setDim(t.sinceFlash < FLASH_EVERY);
				if (!bolt.isDim()) t.sinceFlash = 0;
				level.addFreshEntity(bolt);
			}
			if (t.cloud != null) t.cloud.flash();
		}

		// A Heavenly Thunder Body drinks the heavens' bolt in: no harm, the qi pool filled, and a share of the new stage's cultivation
		// banked for the breakthrough. (The Netherworld's yin is no thunder of its.)
		PlayerCultivation c = CultivationManager.get(p);
		if (!t.ghost && c.hasPhysique(Physique.HEAVENLY_THUNDER_BODY)) {
			t.absorbed += 1.0 / Math.max(1, t.strikes.count());
			c.setQi(c.maxQi());
			p.heal(t.strikes.bolt() * 0.25f);
			if (drawn) {
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1.0, p.getZ(), 60, 0.5, 0.9, 0.5, 0.3);
				level.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.6f);
				CultivationManager.sync(p);
			}
			return;
		}
		if (t.ghost && drawn) level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.4, 0.9, 0.4, 0.05);

		// Both parts land in the same tick as the bolt. Clearing the hurt cooldown in between stops vanilla from
		// discarding the second hit; magic goes first so a killing strike usually reads "struck by lightning".
		// It is cleared before the first part too: within half a second of another hit, vanilla only deals what exceeds
		// that hit, so taking a big blow on purpose (an explosion against Blast Protection, say) would blunt the strike.
		// The dead take each strike as the same share of their hearts as the living take of theirs, in the same armour (the user's ask,
		// 2026-10-19): the bolt as their armour turns it, then the whole of it by their hearts against the living body's they are
		// reckoned from (GhostSystem#livingShare), so a ghost's hearts, more or fewer, make it no easier or harder than a life.
		boolean dead = GhostSystem.isGhost(p);
		// An Ancient Holy Body takes the strikes the more lightly (Physique).
		float multiplier = RingOfTranscendenceItem.damageMultiplier(p) * (c.hasPhysique(Physique.ANCIENT_HOLY_BODY) ? HOLY_STRIKES : 1.0f)
				* (t.demon ? DemonPath.TRIBULATION_HARM : 1.0f); // the heavens hate demons
		float share = dead ? (float) GhostSystem.livingShare(p) : 1.0f;
		float magic = t.strikes.magic() * multiplier * share;
		striking = p.getUUID(); // a killing strike, here, is the tribulation's (#allowDeath)
		try {
			p.invulnerableTime = 0;
			if (magic > 0) {
				p.hurt(t.ghost ? ModDamageTypes.yinTribulation(level) : level.damageSources().magic(), magic);
				p.invulnerableTime = 0;
			}
			if (p.isAlive() && ACTIVE.get(p.getUUID()) == t) {
				float harm = t.strikes.bolt() * multiplier;
				if (dead) {
					// Turned by armour here, before the share (the turning is no straight share of the blow), and dealt past it: one of the
					// common dead, with no hands for armour, turns it as a ghostly form does, as an iron set would (GHOST_FORM_ARMOR).
					float armour = Incorporeal.isRuledGhost(p) ? Math.max(p.getArmorValue(), GHOST_FORM_ARMOR) : p.getArmorValue();
					harm = CombatRules.getDamageAfterAbsorb(harm, armour, (float) p.getAttributeValue(Attributes.ARMOR_TOUGHNESS)) * share;
					p.hurt(ModDamageTypes.yinTribulation(level), harm);
				} else {
					p.hurt(t.ghost ? ModDamageTypes.yinLightning(level) : level.damageSources().lightningBolt(), harm);
				}
			}
		} finally {
			striking = null;
		}
	}

	/** The cultivator a tribulation's strike is landing on this moment, or null. */
	private static UUID striking;

	/**
	 * Whether a tribulation's strike is landing on {@code entity} this moment: the heavens' (or the Netherworld's) own, which no heaven
	 * spell's ward, Golden Bell or rebirth turns (HeavenSpells#incoming, #allowDeath; the user's pick, 2026-10-24).
	 */
	public static boolean isStriking(net.minecraft.world.entity.Entity entity) {
		return striking != null && entity != null && striking.equals(entity.getUUID());
	}

	/**
	 * ALLOW_DEATH: the heavens can't strike a Heaven-Defying Body down (Physique; the user's ask, 2026-10-20: not spared its
	 * tribulations, only their killing). Its strike's killing blow leaves it {@link #DEFIED_HEALTH} and the tribulation fails, costing
	 * it nothing ({@link #holds}); the dead don't scatter for it either. @return false to hold off the death
	 */
	public static boolean allowDeath(ServerPlayer p) {
		if (!p.getUUID().equals(striking)) return true;
		Trial t = ACTIVE.get(p.getUUID());
		if (t == null || !CultivationManager.get(p).hasPhysique(Physique.HEAVEN_DEFYING_BODY)) return true;
		ACTIVE.remove(p.getUUID());
		p.setHealth(DEFIED_HEALTH);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_TRIB_DEFIED).withStyle(net.minecraft.ChatFormatting.DARK_RED));
		p.level().playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6f, 1.4f);
		fail(p, t, false);
		CultivationManager.refresh(p);
		return false;
	}

	private static void succeed(ServerPlayer p) {
		Trial t = ACTIVE.remove(p.getUUID());
		if (t == null) return;

		t.removeCloud();
		ModPackets.sendTribulation(p, false, 0, t.targetRealm, t.targetStage, t.ghost);

		PlayerCultivation c = CultivationManager.get(p);
		if (t.ghost) {
			succeedGhost(p, c, t);
			return;
		}
		if (t.demon) {
			succeedDemon(p, c, t);
			return;
		}
		c.setInTribulation(false);
		CultivationManager.markDirty(p.server);
		boolean sameTarget = c.breakthroughRealm() == t.targetRealm && c.breakthroughStage() == t.targetStage;
		if (!sameTarget || !c.breakthrough()) { // state changed mid-trial (e.g. admin command)
			p.displayClientMessage(Component.translatable(ModLang.MSG_NOT_READY), true);
			return;
		}
		if (t.absorbed > 0) { // a Heavenly Thunder Body's banked lightning becomes cultivation in the new stage
			double gained = Math.min(1, t.absorbed) * THUNDER_SHARE * c.cultivationRequired();
			c.addCultivation(gained);
			p.displayClientMessage(Component.translatable(ModLang.MSG_THUNDER_ABSORBED, Math.round(gained)).withStyle(net.minecraft.ChatFormatting.AQUA), true);
		}
		CultivationManager.refresh(p);
		p.setHealth(p.getMaxHealth());
		p.clearFire();

		ServerLevel level = p.serverLevel();
		level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
		level.playSound(null, p.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.6f, 1.5f);
		level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 80, 0.6, 1.0, 0.6, 0.15);
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.5, 0.8, 0.5, 0.4);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_BREAKTHROUGH, t.targetName()));
		Milestones.onTribulation(p, t.targetRealm);
	}

	/** The Netherworld's tribulation survived: the ghost path on to the stage it was struck for. */
	private static void succeedGhost(ServerPlayer p, PlayerCultivation c, Trial t) {
		NetherworldState s = c.netherworld();
		s.setInGhostTribulation(false);
		CultivationManager.markDirty(p.server);
		int before = GhostMethods.ghostRank(s);
		boolean sameTarget = before + 1 == PlayerCultivation.rank(t.targetRealm, t.targetStage);
		if (!sameTarget || !GhostMethods.canBreakthrough(c) || !GhostMethods.breakthrough(s)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_NOT_READY), true);
			return;
		}
		GhostMethods.afterRank(p, before);
		p.setHealth(p.getMaxHealth());
		p.clearFire();
		Spells.sync(p);
		NetherworldManager.refresh(p);

		ServerLevel level = p.serverLevel();
		level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 0.8f);
		level.playSound(null, p.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.2f, 0.6f);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1.0, p.getZ(), 80, 0.6, 1.0, 0.6, 0.1);
		level.sendParticles(ParticleTypes.GLOW, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.5, 0.8, 0.5, 0.2);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_GHOST_BREAKTHROUGH, t.targetName()).withStyle(net.minecraft.ChatFormatting.AQUA));
		Milestones.onTribulation(p, t.targetRealm);
	}

	/** The Demon Path's tribulation survived: on to the stage it was struck for. */
	private static void succeedDemon(ServerPlayer p, PlayerCultivation c, Trial t) {
		c.demon().setInTribulation(false);
		CultivationManager.markDirty(p.server);
		int before = DemonPath.rank(c);
		boolean sameTarget = before + 1 == PlayerCultivation.rank(t.targetRealm, t.targetStage);
		if (!sameTarget || !DemonPath.canBreakthrough(c) || !DemonPath.breakthrough(c)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_NOT_READY), true);
			return;
		}
		// A Heavenly Thunder Body's banked lightning becomes cultivation in the new stage, as on the heaven path (it was drunk for
		// nothing here before the sweep of 2026-10-23).
		if (t.absorbed > 0) {
			double gained = Math.min(1, t.absorbed) * THUNDER_SHARE * DemonPath.required(DemonPath.rank(c));
			DemonPath.addCultivation(c, gained);
			p.displayClientMessage(Component.translatable(ModLang.MSG_THUNDER_ABSORBED, Math.round(gained)).withStyle(net.minecraft.ChatFormatting.AQUA), true);
		}
		DemonPath.afterRank(p, before);
		p.setHealth(p.getMaxHealth());
		p.clearFire();

		ServerLevel level = p.serverLevel();
		level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 0.9f);
		level.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.8f, 0.8f);
		level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1.0, p.getZ(), 80, 0.6, 1.0, 0.6, 0.12);
		level.sendParticles(ParticleTypes.CRIMSON_SPORE, p.getX(), p.getY() + 1.0, p.getZ(), 60, 0.6, 1.0, 0.6, 0.2);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_DEMON_BREAKTHROUGH, t.targetName()).withStyle(net.minecraft.ChatFormatting.RED));
		Milestones.onTribulation(p, t.targetRealm);
	}

	/** Called from the AFTER_DEATH event (totems of undying prevent that event, so they save the player). */
	public static void onDeath(ServerPlayer p) {
		Trial t = ACTIVE.remove(p.getUUID());
		if (t == null) return;
		fail(p, t, false);
		// The respawn event re-applies the lowered stats and re-syncs the client.
	}

	/** One of the dead scattered (GhostSystem#disperse): mid-way through the Netherworld's tribulation, it has failed it. */
	public static void onScattered(ServerPlayer p) {
		Trial t = ACTIVE.get(p.getUUID());
		if (t == null || !t.ghost) return;
		ACTIVE.remove(p.getUUID());
		fail(p, t, false);
		CultivationManager.refresh(p);
	}

	/**
	 * Whether a failed tribulation costs {@code c} nothing: a Heaven-Defying Body (Physique; the user's ask, 2026-10-20) falls no stage
	 * for it, however it failed (struck, fled, killed or scattered by something else), and keeps its cultivation and any breakthrough
	 * pill's preparing, to face the heavens again at once.
	 */
	public static boolean holds(PlayerCultivation c) {
		return c.hasPhysique(Physique.HEAVEN_DEFYING_BODY);
	}

	/** {@link #holds}: told so, at the rank ({@code rank}) it still stands at. */
	private static Component held(Component rank) {
		return Component.translatable(ModLang.MSG_TRIB_HOLDS, rank).withStyle(net.minecraft.ChatFormatting.GOLD);
	}

	/** A trial failed (struck down, or fled: {@code fled}): its path falls one stage, its cultivation there lost; but see {@link #holds}. */
	private static void fail(ServerPlayer p, Trial t, boolean fled) {
		PlayerCultivation c = CultivationManager.get(p);
		boolean holds = holds(c);
		t.removeCloud();
		ModPackets.sendTribulation(p, false, 0, t.targetRealm, t.targetStage, t.ghost);
		p.server.getPlayerList().broadcastSystemMessage(Component.translatable(ModLang.MSG_TRIB_FAILED, p.getDisplayName()), false);
		if (t.ghost) {
			NetherworldState s = c.netherworld();
			s.setInGhostTribulation(false);
			if (!holds) GhostMethods.fallOneStage(s);
			CultivationManager.markDirty(p.server);
			Component rank = GhostMethods.rankName(GhostMethods.ghostRank(s));
			p.sendSystemMessage(holds ? held(rank) : Component.translatable(fled ? ModLang.MSG_GHOST_TRIB_FLED : ModLang.MSG_GHOST_TRIB_FALL, rank));
			Spells.sync(p);
			return;
		}
		if (t.demon) {
			c.demon().setInTribulation(false);
			if (!holds) DemonPath.fallOneStage(c);
			CultivationManager.markDirty(p.server);
			Component rank = DemonPath.rankName(DemonPath.rank(c));
			p.sendSystemMessage(holds ? held(rank) : Component.translatable(fled ? ModLang.MSG_DEMON_TRIB_FLED : ModLang.MSG_DEMON_TRIB_FALL, rank));
			Spells.sync(p);
			return;
		}
		c.setInTribulation(false);
		if (!holds) c.fallOneStage();
		CultivationManager.markDirty(p.server);
		Component rank = PlayerCultivation.rankName(c.getRealm(), c.getStage());
		p.sendSystemMessage(holds ? held(rank) : Component.translatable(fled ? ModLang.MSG_TRIB_FLED : ModLang.MSG_TRIB_FALL, rank));
	}

	/**
	 * Disconnect: the running trial and its cloud go. The player's cultivation stays marked as mid-tribulation, so they fail
	 * it when they return ({@link #onJoin}).
	 */
	public static void forget(UUID id) {
		Trial trial = ACTIVE.remove(id);
		if (trial != null) trial.removeCloud();
	}

	/**
	 * A player has joined. One who left mid-tribulation (logged out, quit, or the game closed) has failed it, just as if
	 * struck down: they fall one minor stage and lose their cultivation (the ghost path's, for the Netherworld's); a Heaven-Defying
	 * Body loses nothing ({@link #holds}).
	 *
	 * @return true if they fell
	 */
	public static boolean onJoin(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		if (isActive(p.getUUID())) return false;
		boolean holds = holds(c), failed = false;
		if (c.isInTribulation()) {
			c.setInTribulation(false);
			if (!holds) c.fallOneStage();
			p.server.getPlayerList().broadcastSystemMessage(Component.translatable(ModLang.MSG_TRIB_FAILED, p.getDisplayName()), false);
			Component rank = PlayerCultivation.rankName(c.getRealm(), c.getStage());
			p.sendSystemMessage(holds ? held(rank) : Component.translatable(ModLang.MSG_TRIB_FLED, rank));
			failed = true;
		}
		NetherworldState s = c.netherworld();
		if (s.inGhostTribulation()) {
			s.setInGhostTribulation(false);
			if (!holds) GhostMethods.fallOneStage(s);
			p.server.getPlayerList().broadcastSystemMessage(Component.translatable(ModLang.MSG_TRIB_FAILED, p.getDisplayName()), false);
			Component rank = GhostMethods.rankName(GhostMethods.ghostRank(s));
			p.sendSystemMessage(holds ? held(rank) : Component.translatable(ModLang.MSG_GHOST_TRIB_FLED, rank));
			failed = true;
		}
		if (c.demon().inTribulation()) { // the Demon Path's, left mid-way
			c.demon().setInTribulation(false);
			if (!holds) DemonPath.fallOneStage(c);
			p.server.getPlayerList().broadcastSystemMessage(Component.translatable(ModLang.MSG_TRIB_FAILED, p.getDisplayName()), false);
			Component rank = DemonPath.rankName(DemonPath.rank(c));
			p.sendSystemMessage(holds ? held(rank) : Component.translatable(ModLang.MSG_DEMON_TRIB_FLED, rank));
			failed = true;
		}
		if (failed) CultivationManager.markDirty(p.server);
		return failed && !holds;
	}

	/**
	 * Entering the Spatial Gap mid-trial, through the rift or by falling out of the Upper Realm: cancelled without reward or
	 * penalty (see SpatialTrialHandler#enterGap).
	 */
	public static void abandon(ServerPlayer p) {
		Trial t = ACTIVE.remove(p.getUUID());
		if (t == null) return;
		PlayerCultivation c = CultivationManager.get(p);
		if (t.ghost) c.netherworld().setInGhostTribulation(false);
		else if (t.demon) c.demon().setInTribulation(false);
		else c.setInTribulation(false);
		CultivationManager.markDirty(p.server);
		t.removeCloud();
		ModPackets.sendTribulation(p, false, 0, t.targetRealm, t.targetStage, t.ghost);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_TRIB_ABANDONED));
	}

	/** Follows movement and recreates the tracked entity if the player changes dimensions. */
	private static void updateCloud(ServerPlayer player, Trial trial) {
		if (trial.cloud == null || trial.cloud.isRemoved() || trial.cloud.level() != player.level()) {
			trial.removeCloud();
			trial.cloud = ModEntities.TRIBULATION_CLOUD.create(player.serverLevel());
			if (trial.cloud == null) return;
			trial.cloud.configure(trial.targetRealm, trial.ghost);
			trial.cloud.setPos(player.getX(), trial.cloud.cloudBaseY(player.getY()), player.getZ());
			player.serverLevel().addFreshEntity(trial.cloud);
		}
		trial.cloud.follow(player.position());
	}

	/**
	 * Clear references before a world/server is closed, including singleplayer world switches. Anyone mid-trial stays marked
	 * and fails it on their return ({@link #onJoin}).
	 */
	public static void clear() {
		ACTIVE.values().forEach(Trial::removeCloud);
		ACTIVE.clear();
	}

	private TribulationManager() {}
}
