package com.example.defyingtheheavens;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Runtime-only meditation sessions (not persisted: logging out ends meditation). */
public final class MeditationManager {
	private static final class Session {
		double x, y, z;
		/** What the surroundings add (see CultivationBoost), re-read every {@link #BOOST_REFRESH_TICKS}. */
		CultivationBoost.Breakdown boost = CultivationBoost.Breakdown.NONE;
		int ticks;
		/** The boost percentage last told to the player; -1 before the first. */
		int shownPercent = -1;
		/** Qi Harmony (see {@link #harmony}): surges played well in a row, and when the last one was. */
		int circuitStreak;
		int lastCircuitTick = -CIRCULATION_LASTS;
		/**
		 * Seconds of this meditation won from surge games (wisps, a walked sequence, a slain heart demon), added on the drawn path's next
		 * gain at its own rate (before the sweep of 2026-10-23 they were reckoned at the heaven path's, whatever was drawn, and the ghost
		 * path never had them: they waited for the heaven path's next gain).
		 */
		double bonusSeconds;
		/** What the heaven method practised saves of the years its gain costs (Lifespan#saving), as heavenMethods last reckoned it. */
		double methodSaving = 1;

		/** Ticks left in which a soul just arrived in the Inner Realm settles onto the island (moving doesn't count yet). */
		int settling;
		/** Begun in the air, held up by flight ({@link #steady}). */
		boolean airborne;

		// The ghost methods (see GhostMethods): the Method Steles near, re-read every few seconds; seconds spent by the stele of
		// each method not yet known.
		java.util.Set<GhostMethod> stelesNear = java.util.EnumSet.noneOf(GhostMethod.class);
		java.util.Set<GhostMethod> stelesInReach = java.util.EnumSet.noneOf(GhostMethod.class);
		final Map<GhostMethod, Double> steleSeconds = new java.util.EnumMap<>(GhostMethod.class);
		// The heaven methods (see HeavenMethods), the same way: the Dao Steles near, and seconds by each.
		java.util.Set<HeavenMethod> daoStelesNear = java.util.EnumSet.noneOf(HeavenMethod.class);
		java.util.Set<HeavenMethod> daoStelesInReach = java.util.EnumSet.noneOf(HeavenMethod.class);
		final Map<HeavenMethod, Double> daoSteleSeconds = new java.util.EnumMap<>(HeavenMethod.class);

		Session(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	private static final Map<UUID, Session> SESSIONS = new HashMap<>();
	/**
	 * The realm each player was last told, path by path, that meditation gathers nothing in (no method's volume of it, or Four Axis, whose
	 * axes fill it: {@link #tellNoGain}); told once a login, and again in a new realm. Not saved.
	 */
	private static final Map<UUID, java.util.EnumMap<TwoPaths.Path, Realm>> NO_GAIN_TOLD = new HashMap<>();
	/** Each qi surge played well in a row speeds meditation by this much more, up to {@link #MAX_STREAK} of them. */
	public static final double CIRCULATION_BONUS = 0.5;
	public static final int MAX_STREAK = 2;
	/** Qi Harmony lasts this long after the last surge played well (ticks): long enough to reach the next surge. */
	public static final int CIRCULATION_LASTS = 2400;
	/** Qi deviation costs this share of the qi pool, and a little health (which also breaks the meditation). */
	public static final double DEVIATION_QI_LOSS = 0.25;
	public static final float DEVIATION_DAMAGE = 2.0f;
	/** Results of a screen surge game: played well, failed, a qi deviation, or let pass. */
	public static final int CIRCUIT_CLEAN = 0, CIRCUIT_BROKEN = 1, CIRCUIT_DEVIATION = 2, CIRCUIT_IGNORED = 3;
	private static final double MAX_DRIFT_SQR = 0.25; // half a block
	private static final int BOOST_REFRESH_TICKS = 20;
	/** The boost is first announced this long after sitting down, so the "you begin to gather qi" message is read first. */
	private static final int BOOST_ANNOUNCE_DELAY = 40;

	public static boolean isMeditating(UUID id) {
		return SESSIONS.containsKey(id);
	}

	/** From this realm on the living float as they meditate, as the dead always do (the body's realm: TwoPaths#body). */
	public static final Realm FLOATS_FROM = Realm.CORE_FORMATION;

	/** Whether {@code player} floats in meditation: alive (the dead's own hover is the SoulRenderer's), the body at {@link #FLOATS_FROM} or above. */
	public static boolean floats(ServerPlayer player) {
		PlayerCultivation c = CultivationManager.get(player);
		return !c.netherworld().ghost() && TwoPaths.body(c, false).rank() >= PlayerCultivation.rank(FLOATS_FROM, Stage.EARLY);
	}

	public static void toggle(ServerPlayer player) {
		if (isMeditating(player.getUUID())) {
			stop(player, false);
		} else {
			start(player);
		}
	}

	public static void start(ServerPlayer player) {
		// A clone cultivates nothing (Clones): its maker's cultivation is the main body's, sitting where it was. Here, not only at the
		// key (#toggle): every way into a meditation comes through here (before the sweep of 2026-10-23 the others let a clone sit).
		if (Clones.inClone(player)) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_CLONE_CANNOT), true);
			return;
		}
		if (TribulationManager.isActive(player.getUUID())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_TRIB_BUSY), true);
			return;
		}
		// A soul out of its body (or leaving it) can't settle into meditation; one in another's body can, and cultivates that body (Possession).
		if (SoulProjection.isAwayFromBody(player.getUUID())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_BUSY), true);
			return;
		}
		// A mortal's meridians are closed, the ghost path's as the heaven path's: alive or dead, it has nothing to cultivate with. One with
		// the Body Path drawn in sits to temper the body (BodyPath), and Shattered Meridians short of Peak to temper theirs (Tempering).
		PlayerCultivation sitting = CultivationManager.get(player);
		if (sitting.isMortal() && !TwoPaths.cultivatesBody(sitting)
				&& (!sitting.hasShatteredMeridians() || GhostSystem.isGhost(player) || !sitting.canTemper())) {
			player.displayClientMessage(Component.translatable(GhostSystem.isGhost(player) ? ModLang.MSG_METHOD_MERIDIANS_DEAD : ModLang.MSG_MORTAL_MEDITATE), true);
			return;
		}
		// The dead cultivate only the ghost path, and need no method to begin it (ghostMethods), as the living need none for the
		// heaven path's.
		if (HeavenRitual.isActive(player.getUUID())) { // kneeling before the heavens already
			player.displayClientMessage(Component.translatable(ModLang.MSG_RITUAL_BUSY), true);
			return;
		}
		if (FiveDecays.restless(player)) { // the Five Decays' fifth sign: the mind can find no rest
			player.displayClientMessage(Component.translatable(ModLang.MSG_DECAY_RESTLESS), true);
			return;
		}
		if (!canMeditate(player)) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_CANNOT), true);
			return;
		}
		Session session = new Session(player.getX(), player.getY(), player.getZ());
		session.airborne = !player.onGround();
		session.boost = CultivationBoost.of(player);
		SESSIONS.put(player.getUUID(), session);
		ModPackets.broadcastMeditation(player, true);
		broadcastAbsorption(player, session);
		CultivationManager.sync(player);
		player.displayClientMessage(Component.translatable(ModLang.MSG_START), true);
		// Sitting down with the breakthrough already waiting on a heaven method: a reminder over the hotbar (the chat had it when the bar filled).
		PlayerCultivation c = CultivationManager.get(player);
		if (!GhostSystem.isGhost(player) && c.isHeldByMethod()) player.displayClientMessage(HeavenMethods.gateLine(c).copy().withStyle(net.minecraft.ChatFormatting.GOLD), true);
	}

	public static void stop(ServerPlayer player, boolean interrupted) {
		if (SESSIONS.remove(player.getUUID()) == null) return;
		QiSurges.end(player);
		boolean inside = InnerRealm.isInside(player);
		if (inside) InnerRealm.leave(player); // getting up brings the soul back to the body
		ModPackets.broadcastMeditation(player, false);
		CultivationManager.sync(player);
		player.displayClientMessage(Component.translatable(interrupted ? ModLang.MSG_INTERRUPTED : ModLang.MSG_STOP), true);
	}

	/** Silent removal (disconnect). */
	public static void forget(UUID id) {
		SESSIONS.remove(id);
		NO_GAIN_TOLD.remove(id);
		QiSurges.forget(id);
		SoulCrystalBlock.forget(id);
	}

	/** Whether {@link #start} would begin a session right now: no tribulation running, and steady, dry ground (or flight). */
	public static boolean canStart(ServerPlayer player) {
		return !TribulationManager.isActive(player.getUUID()) && !SoulProjection.isAwayFromBody(player.getUUID()) && canMeditate(player);
	}

	private static boolean canMeditate(ServerPlayer p) {
		return p.isAlive() && !p.isSpectator() && steady(p) && !p.isInWaterOrBubble() && !p.isPassenger() && !p.isSleeping();
	}

	/**
	 * Held steady to sit in meditation: on the ground, or flying (the user's ask, 2026-10-22: one may meditate in the air). The flight
	 * goes on costing its qi as ever (QiFlight), so only a gathering that keeps up with it (Heavenly Being's everywhere, anyone's in
	 * the Upper Realm) holds one up there for good; a pool run dry ends the flight, and with it the meditation ({@link #tick}). An
	 * elytra's glide is no flight to sit on.
	 */
	private static boolean steady(ServerPlayer p) {
		return (p.onGround() || p.getAbilities().flying) && !p.isFallFlying();
	}

	/** How much a run of clean qi circuits speeds meditation right now: x1, then x1.5, then x2. */
	private static double circulationMultiplier(Session s, int ticks) {
		if (ticks - s.lastCircuitTick > CIRCULATION_LASTS) s.circuitStreak = 0;
		return 1 + CIRCULATION_BONUS * Math.min(s.circuitStreak, MAX_STREAK);
	}

	/** The circulation multiplier for {@code player}'s meditation (1 if they aren't meditating). */
	public static double circulationMultiplier(UUID player) {
		Session s = SESSIONS.get(player);
		return s == null ? 1 : circulationMultiplier(s, s.ticks);
	}

	/**
	 * The result of a screen surge game, played on the client (see QiSurgeHud): played well brings Qi Harmony, a failed or
	 * ignored one simply passes, and forcing the qi (three wrong presses) is a qi deviation. Results only count while a
	 * screen surge is running.
	 */
	public static void onCirculation(ServerPlayer p, int outcome) {
		Session s = SESSIONS.get(p.getUUID());
		if (s == null || CultivationManager.get(p).isMortal() || !QiSurges.acceptScreenResult(p, s.ticks)) return;
		switch (outcome) {
			case CIRCUIT_CLEAN -> harmony(p);
			case CIRCUIT_DEVIATION -> {
				s.circuitStreak = 0;
				PlayerCultivation c = CultivationManager.get(p);
				c.setQi(c.getQi() - c.maxQi() * DEVIATION_QI_LOSS);
				p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 100, 0));
				p.displayClientMessage(Component.translatable(ModLang.MSG_DEVIATION), true);
				stop(p, true);
				CultivationManager.sync(p);
				p.hurt(p.damageSources().magic(), DEVIATION_DAMAGE);
			}
			default -> { }
		}
	}

	/** A surge played well: Qi Harmony, x1.5, then x2 for two or more in a row, for {@link #CIRCULATION_LASTS}. */
	public static void harmony(ServerPlayer p) {
		Session s = SESSIONS.get(p.getUUID());
		if (s == null) return;
		circulationMultiplier(s, s.ticks); // lapse an expired streak first
		s.circuitStreak = Math.min(s.circuitStreak + 1, MAX_STREAK);
		s.lastCircuitTick = s.ticks;
		p.displayClientMessage(Component.translatable(ModLang.MSG_HARMONY, s.circuitStreak == 1 ? "1.5" : "2"), true);
	}

	/** Cultivation worth {@code seconds} of this meditation's current rate on the path drawn in, added with its next gain. */
	public static void addBonusSeconds(ServerPlayer p, double seconds) {
		Session s = SESSIONS.get(p.getUUID());
		if (s != null) s.bonusSeconds += Math.max(0, seconds);
	}

	/** How long the player has been meditating (ticks), or 0. */
	public static int sessionTicks(ServerPlayer p) {
		Session s = SESSIONS.get(p.getUUID());
		return s == null ? 0 : s.ticks;
	}

	/** Cultivation per second this meditation gains right now, every bonus included. */
	private static double ratePerSecond(ServerPlayer p, Session session) {
		PlayerCultivation c = CultivationManager.get(p);
		return c.meditationCultivationPerSecond(RingOfPowerItem.cultivationBonus(p)) * session.boost.multiplier()
				* circulationMultiplier(session, session.ticks) * (InnerRealm.isInside(p) ? InnerRealm.CULTIVATION_BONUS : 1.0)
				* heldAndTaught(p);
	}

	/**
	 * A soul in the Inner Realm told what its body's surroundings give it (ModPackets#INNER_PLACE): the Cultivation tab reckons its
	 * rate, its years and its method's element from the player's own surroundings, the island's there, and showed the mats, pedestals,
	 * height and calm about the body gone (the user's report of 2026-10-27: they counted all along; the tab now shows them, with the
	 * Inner Realm's own x1.25).
	 */
	private static void sendBodyPlace(ServerPlayer p, Session session) {
		QiElement[] elements = QiElement.values();
		double[] shares = new double[elements.length];
		for (int i = 0; i < elements.length; i++) shares[i] = HeavenMethods.shareAt(p, elements[i]);
		ModPackets.sendInnerPlace(p, session.boost.multiplier(), shares);
	}

	/**
	 * The meditation's boost as it saves years (Lifespan#saving): its surroundings' (CultivationBoost), and the Inner Realm's as well,
	 * turned inward (the user's retune of 2026-10-27: an Immortal talent becomes a True Immortal with a method, four pedestals and the
	 * Inner Realm, and not without it; before, it only sped the gain).
	 */
	private static double yearsBoost(ServerPlayer p, Session session) {
		return session.boost.multiplier() * (InnerRealm.isInside(p) ? InnerRealm.CULTIVATION_BONUS : 1.0);
	}

	/**
	 * The pace a meditation is set to (the Cultivation tab: x1 its own to x3; Lifespan#pacePrice: what it wins the dearer in years), by
	 * which every gain of it comes the faster: both paths' cultivation, the methods' settling on it, pill and fruit qi refined, a
	 * body tempered. Not a reward of so many seconds of it (addBonusSeconds): that is the meditation's own pace's.
	 */
	private static int pace(ServerPlayer p) {
		return CultivationManager.own(p).meditationSpeed();
	}

	/**
	 * A spirit stone in hand (SpiritStoneItem), a teacher's Insight (TalkSystem guidance), the Spring of the Ancients drunk (a secret realm's,
	 * ModEffects#ANCIENT_SPRING), the master's lecture heard in the Cave Abode (ModEffects#MASTERS_LECTURE), one's own sect's grounds
	 * (SectMembership#groundsMultiplier) and, for a soul meditating in another's body, how far it stands above that body (Possession#guidance),
	 * each speeding the gain.
	 */
	private static double heldAndTaught(ServerPlayer p) {
		return SpiritStoneItem.meditationMultiplier(p) * (p.hasEffect(ModEffects.INSIGHT) ? 1.0 + ModEffects.INSIGHT_BONUS : 1.0)
				* (p.hasEffect(ModEffects.ANCIENT_SPRING) ? 1.0 + ModEffects.SPRING_BONUS : 1.0)
				* (p.hasEffect(ModEffects.MASTERS_LECTURE) ? 1.0 + ModEffects.LECTURE_BONUS : 1.0)
				* SectMembership.groundsMultiplier(p) * Possession.guidance(p);
	}

	private static boolean stillValid(ServerPlayer p, Session s) {
		double dx = p.getX() - s.x;
		double dz = p.getZ() - s.z;
		return !p.isShiftKeyDown() && steady(p) && !p.isInWaterOrBubble() && !p.isPassenger()
				&& dx * dx + dz * dz <= MAX_DRIFT_SQR && Math.abs(p.getY() - s.y) <= 0.5;
	}

	/** A meditation in the air whose flight has given out (its qi run dry, QiFlight): it ends, and is told so as it falls. */
	private static boolean fellFromTheAir(ServerPlayer p, Session s) {
		return s.airborne && !p.onGround() && !p.getAbilities().flying;
	}

	public static void tick(MinecraftServer server) {
		if (SESSIONS.isEmpty()) return;
		int tick = server.getTickCount();
		List<ServerPlayer> interrupted = new ArrayList<>();
		List<ServerPlayer> fell = new ArrayList<>();
		List<UUID> gone = new ArrayList<>();

		// Over a copy: a meditation's tick reaches far (surges, steles, years, the Inner Realm), and a session ended on the way would
		// otherwise break the loop.
		for (Map.Entry<UUID, Session> entry : new ArrayList<>(SESSIONS.entrySet())) {
			if (SESSIONS.get(entry.getKey()) != entry.getValue()) continue; // ended (or begun anew) by one before it this tick
			ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
			if (p == null) {
				gone.add(entry.getKey());
			} else if (!p.isAlive() || (entry.getValue().settling <= 0 && !InnerRealm.isInside(p) && !stillValid(p, entry.getValue()))) {
				// A soul in the Inner Realm walks freely: only the meditate key (or harm to the body) ends it.
				interrupted.add(p);
				if (p.isAlive() && fellFromTheAir(p, entry.getValue())) fell.add(p);
			} else {
				tickMeditation(p, entry.getValue(), tick);
			}
		}
		gone.forEach(SESSIONS::remove);
		interrupted.forEach(p -> stop(p, true));
		fell.forEach(p -> p.displayClientMessage(Component.translatable(ModLang.MSG_MEDITATION_FELL), true));
	}

	private static void tickMeditation(ServerPlayer p, Session session, int tick) {
		ServerLevel level = p.serverLevel();
		PlayerCultivation c = CultivationManager.get(p);

		session.ticks++;
		if (session.settling > 0 && --session.settling == 0) { // settled on the island: from here, getting up ends it
			session.x = p.getX();
			session.y = p.getY();
			session.z = p.getZ();
		}
		// After a while the soul turns inward (an ability that can be switched off), the eyes closing first. Only from a body on firm
		// ground: one held up in the air by its flight would be held up by nothing while its soul was away (or for nothing: no qi paid).
		if (session.ticks == InnerRealm.ENTER_AFTER_TICKS - InnerRealm.FADE_TICKS && p.onGround() && InnerRealm.canEnter(p)) {
			ModPackets.sendInnerFade(p);
		}
		if (session.ticks == InnerRealm.ENTER_AFTER_TICKS && p.onGround() && InnerRealm.enter(p)) {
			session.settling = 40;
			session.x = p.getX();
			session.y = p.getY();
			session.z = p.getZ();
			ModPackets.broadcastMeditation(p, true);
		}
		if (session.ticks % BOOST_REFRESH_TICKS == 0) {
			session.boost = CultivationBoost.of(p);
			if (InnerRealm.isInside(p)) sendBodyPlace(p, session);
		}
		if (session.ticks >= BOOST_ANNOUNCE_DELAY) announceBoost(p, session);
		if (c.isMortal()) { // no qi to circulate: blood instead, tempering the Body Path (BodyPath), or Shattered Meridians' mortal body below Peak
			if (tick % 10 == 0) {
				if (TwoPaths.cultivatesBody(c)) bodyPath(p, c, session);
				else Tempering.gain(p, BodyRefining.meditationPerSecond(c) * 0.5 * heldAndTaught(p) * pace(p));
			}
			// A stone in hand feeds a tempering body as it does a cultivator's, and is spent so (it was free here before 2026-10-22).
			if (tick % 20 == 0 && (c.hasShatteredMeridians() || TwoPaths.cultivatesBody(c))) SpiritStoneItem.drainWhileMeditating(p);
			// An immortal body's meditation in the Immortal Realm comprehends the Law in focus as any immortal's does (Laws, BodyPath#immortal).
			if (tick % 10 == 0) Laws.meditate(p, c, 0.5 * pace(p));
			if (tick % 20 == 0) ModPackets.broadcastMeditation(p, true);
			return;
		}
		QiSurges.tick(p, session.ticks, session.settling <= 0);

		if (tick % 5 == 0 && !InnerRealm.isInside(p)) {
			level.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.2, p.getZ(), 6, 0.5, 0.5, 0.5, 0.8);
			if (c.getRealm().ordinal() >= Realm.CORE_FORMATION.ordinal()) {
				level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 0.2, p.getZ(), 2, 0.4, 0.1, 0.4, 0.02);
			}
		}

		// A meditation draws in one qi or the other, never both (TwoPaths#cultivatesGhost: the Cultivation tab's switch; the dead's
		// always the ghost path's): the ghost path's as often, its steles read either way (ghostMethods); the heaven path's below. A
		// living Primordial Chaos Body draws in both at once, each at its full pace (Physique).
		if (tick % 10 == 0) ghostMethods(p, c, session, tick);
		boolean ghostQi = TwoPaths.cultivatesGhost(c);
		// The Demon Path drawn in (TwoPaths#cultivatesDemon: the switch), the heaven path rests.
		boolean demonQi = TwoPaths.cultivatesDemon(c);
		if (tick % 10 == 0 && demonQi) demonPath(p, c, session);
		// The Body Path drawn in (TwoPaths#cultivatesBody): the blood circulated instead of any qi, the heaven path resting.
		boolean bodyBlood = TwoPaths.cultivatesBody(c);
		if (tick % 10 == 0 && bodyBlood) bodyPath(p, c, session);
		boolean heavenQi = !GhostSystem.isGhost(p) && !demonQi && !bodyBlood && (!ghostQi || drawsBoth(p, c));
		// Drawing in ghostly qi only, a Dao Stele still teaches the living what it carves (learning isn't cultivating).
		if (tick % 10 == 0 && ghostQi && !heavenQi && HeavenMethods.canLearn(c)) learnFromDaoSteles(p, c, session, tick);
		// Gain cultivation twice a second so the menu bar moves smoothly. The dead's meditation feeds only their ghost path.
		if (tick % 10 == 0 && heavenQi) {
			boolean wasBottleneck = c.isAtBottleneck();
			boolean wasAtGate = c.isAtRitualGate();
			boolean wasHeld = c.isHeldByMethod();
			// Mats, fruit on pedestals, height and tranquillity speed the gain (see CultivationBoost); and the pace it is set to.
			CultivationMethod method = CultivationMethods.practised(c, TwoPaths.Path.HEAVEN);
			// A Qi-Gathering Formation thickens the qi about it, for Qi Circulation alone (GatheringFormations): faster, its years as they were.
			double formation = method == CultivationMethod.QI_CIRCULATION ? GatheringFormations.factor(p, GatheringFormations.Kind.QI) : 1.0;
			double steady = ratePerSecond(p, session) * formation, perSecond = steady * pace(p);
			// No method, no gain (the user's rule, 2026-10-31): the method practised must hold its volume of the realm and gather by meditation
			// here (CultivationMethods#whyNoGain: Four Axis's axes fill it; Turtle Breath only in deep seclusion; the rites and the lightning
			// their own ways); else nothing is gathered, and the pill and fruit qi waits unrefined.
			boolean gains = HeavenMethods.canLearn(c) && CultivationMethods.whyNoGain(p, c, TwoPaths.Path.HEAVEN) == null;
			// Unrefined qi from pills and fruit is refined on top, at REFINE_RATE times the meditation rate.
			double refined = gains ? c.refine(perSecond * PlayerCultivation.REFINE_RATE * 0.5) : 0;
			double bonus = steady * session.bonusSeconds; // a reward's seconds at the meditation's own pace, not the pace set
			double gathered = perSecond * 0.5 + bonus;
			session.bonusSeconds = 0;
			// What is gathered goes by the scripture Circulation follows (its element's qi here: HeavenMethods#cultivationFactor), and the
			// scriptures' layers behind the realm settle in on it, gathered or not (understanding what one holds isn't cultivating).
			double factor = heavenMethods(p, c, session, tick, gathered, refined);
			// Turtle Breath: half the pace, no element's lands (its years a third: CultivationMethod#yearsFactor).
			if (method == CultivationMethod.TURTLE_BREATH) {
				factor = CultivationMethods.TURTLE_PACE * HeavenMethods.methodFactor(c, 0.2, false);
				session.methodSaving = 1;
			}
			// Dao Field Attunement: its stage's Dao field's law, whatever one's element, twice as fast (MethodGrounds; nothing elsewhere).
			if (method == CultivationMethod.DAO_FIELD_ATTUNEMENT) {
				factor = HeavenMethods.methodFactor(c, 0.2, false) * MethodGrounds.FIELD_PACE;
				session.methodSaving = 1;
			}
			// Immortal Essence Refining: by the essence it burns, twice as fast while it lasts (MethodGrounds#burn).
			if (method == CultivationMethod.IMMORTAL_ESSENCE_REFINING && gains) factor *= MethodGrounds.burn(p, c);
			if (!gains) factor = 0;
			int rankBefore = PlayerCultivation.rank(c.getRealm(), c.getStage());
			double before = c.getCultivation(), won = gathered * factor + refined;
			boolean advanced = c.addCultivation(won);
			// What it won costs years (Lifespan#seclude: the talent race, by the method's own weight), but for a reward's seconds of meditation.
			// Turtle Breath's third is its own: nothing outside it saves more (a mat's, a pedestal's, a stone's), only denser qi.
			double saving = method == CultivationMethod.TURTLE_BREATH ? Lifespan.saving(1, 1, Lifespan.placeSaving(c), 1)
					: Lifespan.saving(yearsBoost(p, session), session.methodSaving, Lifespan.placeSaving(c), SpiritStoneItem.meditationMultiplier(p));
			Lifespan.seclude(p, rankBefore, before, PlayerCultivation.rank(c.getRealm(), c.getStage()), c.getCultivation(),
					rank -> PlayerCultivation.required(Realm.byIndex(rank / Stage.values().length), Stage.byIndex(rank % Stage.values().length)),
					saving, won <= 0 ? 0 : 1 - bonus * factor / won, method);
			// Thunder Tempering calls the lightning down on one meditating under the open sky (its own way: ThunderTempering).
			if (method == CultivationMethod.THUNDER_TEMPERING && HeavenMethods.canGain(c)) ThunderTempering.meditating(p, c);
			CultivationManager.refresh(p); // re-applies stats (cheap), marks dirty, syncs

			if (advanced) {
				level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
				p.displayClientMessage(Component.translatable(ModLang.MSG_STAGE_UP,
						c.getRealm().getDisplayName(), c.getStage().getDisplayName()), true);
				p.setHealth(p.getMaxHealth()); // after refresh, so it fills the new, higher max health
			}
			// Not else-if: a ring can carry a single tick through the last stage-up straight into the bottleneck.
			if (!wasBottleneck && c.canBreakthrough()) {
				p.sendSystemMessage(Component.translatable(ModLang.MSG_BOTTLENECK));
			} else if (!wasBottleneck && c.isAtBottleneck() && c.isMissingBreakthroughPill()) {
				p.sendSystemMessage(Component.translatable(ModLang.MSG_NEED_PILL, PillItem.breakthroughPillFor(c.breakthroughRealm()).getDescription(),
						c.breakthroughRealm().getDisplayName()));
			} else if (!wasBottleneck && c.isAtBottleneck() && c.isBreakthroughLocked()) {
				p.sendSystemMessage(c.lockLine()); // the lower realms' cap; into True Immortal and past it, the Immortal Realm
			} else if (!wasAtGate && c.isAtRitualGate()) {
				p.sendSystemMessage(Component.translatable(ModLang.MSG_RITUAL_GATE));
			}
			// Not else-if: a pill and a method may both be wanted.
			if (!wasHeld && c.isHeldByMethod()) p.sendSystemMessage(HeavenMethods.gateLine(c).copy().withStyle(net.minecraft.ChatFormatting.GOLD));
			// A Four Axis stage full: its axis of virtue, if not yet complete, is wanted too (FourAxis).
			if (!wasBottleneck && c.isHeldByAxis()) {
				p.sendSystemMessage(FourAxis.gateLine(c, PlayerCultivation.rank(c.getRealm(), c.getStage())).copy().withStyle(net.minecraft.ChatFormatting.GOLD));
			}
		}

		// In the Immortal Realm an immortal's meditation, whichever qi it draws in, comprehends the Law in focus too (Laws, phase 3), at its pace.
		if (tick % 10 == 0) Laws.meditate(p, c, 0.5 * pace(p));

		if (tick % 20 == 0) {
			ModPackets.broadcastMeditation(p, true); // heartbeat for late joiners / newly tracking players
			broadcastAbsorption(p, session);
			SpiritStoneItem.drainWhileMeditating(p); // a second of a held stone's qi, if one is held
		}
	}

	/**
	 * Half a second of the ghost path (see GhostMethods): by a stele of a method not yet known, a step toward learning it (whichever
	 * qi is drawn in); and, drawing in ghostly qi (TwoPaths#cultivatesGhost), the ghost path's bar fills wherever ghostly qi flows, as
	 * fast as it is dense (by the practised method's stele, faster), its stages stepping on to a bottleneck (the Netherworld's
	 * tribulation's), while the methods' layers behind it settle in on what it gathers. Timed by the server's {@code tick} (it runs on
	 * every tenth): the session's own count is offset from it, so tested against that, the steles were never re-read and the
	 * Cultivation tab's bar never sent but at a new stage.
	 */
	/**
	 * Half a second of the Demon Path (DemonPath), drawn in by the switch: its bar fills at its realm's own rate by every boost the heaven
	 * path's has (a mat, a pedestal, a stone in hand, the pace...), by the race's pace, its stages stepping on to a bottleneck, the spells
	 * its inheritance teaches told as they come; its years paid as the heaven path's are (Lifespan#seclude).
	 */
	private static void demonPath(ServerPlayer p, PlayerCultivation c, Session session) {
		// Its blood is its method's volume, so every realm it has reached gathers; but at Four Axis the axes are its method, as every
		// path's (FourAxis#fillBars); Kin Blood and Core Devouring gather their own ways; Territorial Dominion within its territory alone
		// (CultivationMethods#whyNoGain).
		Component why = CultivationMethods.whyNoGain(p, c, TwoPaths.Path.DEMON);
		if (why != null) {
			if (DemonPath.rank(c) >= 0) tellNoGain(p, TwoPaths.Path.DEMON, DemonPath.realm(DemonPath.rank(c)), why);
			return;
		}
		CultivationMethod method = CultivationMethods.practised(c, TwoPaths.Path.DEMON);
		double steady = DemonPath.perSecond(c, RingOfPowerItem.cultivationBonus(p)) * session.boost.multiplier()
				* circulationMultiplier(session, session.ticks) * (InnerRealm.isInside(p) ? InnerRealm.CULTIVATION_BONUS : 1.0)
				* heldAndTaught(p)
				* (method == CultivationMethod.TERRITORIAL_DOMINION ? CultivationMethods.TERRITORY_PACE : 1.0) // its territory's qi its own
				* (method == CultivationMethod.ANCESTRAL_COMMUNION ? MethodGrounds.BONES_PACE : 1.0) // the primeval dragon's bones (MethodGrounds)
				* (method == CultivationMethod.PRIMORDIAL_RETURN ? MethodGrounds.PRIMORDIAL_PACE : 1.0) // a primordial beast near
				* (1 - c.demon().taint()); // foreign cores' taint (Core Devouring) slows it, until Purification cleanses it
		double rate = steady * pace(p);
		double bonus = steady * session.bonusSeconds; // a reward's seconds at its own rate (and the meditation's own pace)
		session.bonusSeconds = 0;
		double gained = rate * 0.5 + bonus;
		int before = DemonPath.rank(c);
		double was = c.demon().cultivation();
		boolean wasBottleneck = DemonPath.atBottleneck(c);
		int stages = DemonPath.addCultivation(c, gained);
		Lifespan.seclude(p, before, was, DemonPath.rank(c), c.demon().cultivation(), DemonPath::required,
				Lifespan.saving(yearsBoost(p, session), 1, Lifespan.placeSaving(c, DemonPath.realm(before)), SpiritStoneItem.meditationMultiplier(p)),
				gained <= 0 ? 0 : 1 - bonus / gained, method);
		// Bloodline Purification cleanses the blood of foreign cores' taint as it gathers (DemonPath#cleanse); a territory is contested.
		if (method == CultivationMethod.BLOODLINE_PURIFICATION) DemonPath.cleanse(c, before, gained);
		if (method == CultivationMethod.TERRITORIAL_DOMINION) Territories.meditating(p);
		if (stages > 0) {
			p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.9f);
			DemonPath.afterRank(p, before);
			p.setHealth(p.getMaxHealth());
		} else {
			CultivationManager.markDirty(p.server);
			CultivationManager.sync(p);
		}
		if (!wasBottleneck && DemonPath.atBottleneck(c)) {
			p.sendSystemMessage(DemonPath.heldByAxis(c) ? FourAxis.gateLine(c, DemonPath.rank(c)).copy().withStyle(net.minecraft.ChatFormatting.GOLD)
					: DemonPath.heldByBlood(c) ? DemonPath.bloodGateLine(c).copy().withStyle(net.minecraft.ChatFormatting.GOLD)
					: DemonPath.heldByLaws(c) ? DemonPath.lawGateLine(c).copy().withStyle(net.minecraft.ChatFormatting.GOLD)
					: DemonPath.locked(c) ? c.lockLine(DemonPath.rank(c) + 1)
					: Component.translatable(ModLang.MSG_DEMON_BOTTLENECK).withStyle(net.minecraft.ChatFormatting.RED));
		}
	}

	/**
	 * Half a second of a meditation with the Body Path drawn in (TwoPaths#cultivatesBody): the blood circulated, tempering it at its stage's own
	 * pace (BodyPath#perSecond) by the meditation's boosts and pace, its years paid as any meditation's are (Lifespan#seclude); at Golden Blood,
	 * its Four Axis, the axes fill it instead (FourAxis#fillBars; CultivationMethods#whyNoGain tells so).
	 */
	private static void bodyPath(ServerPlayer p, PlayerCultivation c, Session session) {
		Component why = CultivationMethods.whyNoGain(p, c, TwoPaths.Path.BODY);
		if (why != null) {
			if (BodyPath.rank(c) >= 0) tellNoGain(p, TwoPaths.Path.BODY, BodyPath.realm(BodyPath.rank(c)), why);
			return;
		}
		double steady = BodyPath.perSecond(c, RingOfPowerItem.cultivationBonus(p)) * session.boost.multiplier()
				* circulationMultiplier(session, session.ticks) * (InnerRealm.isInside(p) ? InnerRealm.CULTIVATION_BONUS : 1.0) * heldAndTaught(p);
		double rate = steady * pace(p);
		double bonus = steady * session.bonusSeconds; // a reward's seconds at its own rate (and the meditation's own pace)
		session.bonusSeconds = 0;
		double gained = rate * 0.5 + bonus;
		int before = BodyPath.rank(c);
		double was = c.body().cultivation();
		boolean wasBottleneck = BodyPath.atBottleneck(c);
		int stages = BodyPath.addCultivation(c, gained);
		Lifespan.seclude(p, before, was, BodyPath.rank(c), c.body().cultivation(), BodyPath::required,
				Lifespan.saving(yearsBoost(p, session), 1, Lifespan.placeSaving(c, BodyPath.realm(before)), SpiritStoneItem.meditationMultiplier(p)),
				gained <= 0 ? 0 : 1 - bonus / gained, CultivationMethod.BODY_TEMPERING);
		if (stages > 0) {
			p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.7f);
			BodyPath.afterRank(p, before);
			p.setHealth(p.getMaxHealth());
		} else {
			CultivationManager.markDirty(p.server);
			CultivationManager.sync(p);
		}
		if (!wasBottleneck && BodyPath.atBottleneck(c)) BodyPath.tellBottleneck(p, c);
	}

	/** One of the living born with a Primordial Chaos Body (Physique): its meditation feeds both paths, each at its full pace. */
	static boolean drawsBoth(ServerPlayer p, PlayerCultivation c) {
		return !c.isMortal() && !GhostSystem.isGhost(p) && c.hasPhysique(Physique.PRIMORDIAL_CHAOS_BODY);
	}

	private static void ghostMethods(ServerPlayer p, PlayerCultivation c, Session session, int tick) {
		if (c.isMortal()) return; // a mortal's meridians are closed: nothing learned, nothing cultivated (it can't sit down to it either)
		NetherworldState s = c.netherworld();
		if (tick % 40 == 0 || session.ticks <= 10) {
			session.stelesInReach = stelesWithin(p, GhostMethods.STELE_LEARN_RADIUS);
			session.stelesNear = stelesWithin(p, GhostMethods.STELE_BONUS_RADIUS);
		}
		for (GhostMethod method : session.stelesInReach) {
			if (s.knows(method) || !GhostMethods.canLearn(c, method) || !method.taughtInTheWorld()) continue; // one carved before the rule unread
			double seconds = session.steleSeconds.merge(method, 0.5, Double::sum);
			if (seconds == 0.5) p.displayClientMessage(Component.translatable(ModLang.MSG_STELE_READING, method.getDisplayName()), true);
			if (seconds >= GhostMethods.STELE_LEARN_SECONDS) {
				session.steleSeconds.remove(method);
				GhostMethods.learn(p, method);
			}
		}
		boolean both = drawsBoth(p, c) && GhostMethods.ghostRank(s) >= 0; // a Primordial Chaos Body's ghost path, begun, drawn in too
		if (!TwoPaths.cultivatesGhost(c) && !both || s.density() <= 0) return;
		// One of the dead with no foundation is taught Netherworld Breathing, which begins the path (GhostMethods#teachTheDead: at the
		// Ghost Gate, and here for a world saved before 2026-10-31, when the dead began it with none); the path is kept if born again. The
		// living begin it with a foundation learned (GhostMethods#give).
		if (s.ghost() && GhostMethods.deepestFoundation(s) == null) GhostMethods.teachTheDead(p);
		if (GhostMethods.ghostRank(s) < 0) return;
		GhostMethod active = s.activeMethod();
		CultivationMethod method = CultivationMethods.practised(c, TwoPaths.Path.GHOST);
		// A Yin-Gathering Formation thickens the yin about it, for Netherworld Breathing alone (GatheringFormations): faster, its years as they were.
		double formation = method == CultivationMethod.NETHERWORLD_BREATHING ? GatheringFormations.factor(p, GatheringFormations.Kind.YIN) : 1.0;
		double steady = GhostMethods.ratePerSecond(c, s.density()) * formation * circulationMultiplier(session, session.ticks)
				* (active != null && session.stelesNear.contains(active) ? GhostMethods.STELE_BONUS : 1.0)
				* (1 + (p.hasEffect(ModEffects.YIN_GATHERING) ? s.methodBoost() : 0)) // a Yin Gathering Pill
				* (InnerRealm.isInside(p) ? InnerRealm.CULTIVATION_BONUS : 1.0) // turned inward, as the heaven path is (the body's ghostly qi)
				* Possession.guidance(p); // a soul in another's body, by how far it stands above it
		double rate = steady * pace(p); // the pace it is set to (its years paid as it wins: Lifespan#seclude)
		// A reward's seconds (addBonusSeconds) are the drawn path's: the ghost path's when it is drawn alone (a Primordial Chaos Body
		// drawing both has them on the heaven path).
		boolean drawn = TwoPaths.cultivatesGhost(c) && !both;
		double bonus = drawn ? steady * session.bonusSeconds : 0;
		if (drawn) session.bonusSeconds = 0;
		double gained = rate * 0.5 + bonus;
		// Soul Furnace Refining: the soul burns in its own ghost fire in place of the yin drawn in, the gain and the harm growing with every
		// second borne (SoulFurnace; out of health, it breaks).
		if (method == CultivationMethod.SOUL_FURNACE && SoulFurnace.burns(c)) gained = SoulFurnace.burn(p, c, session.ticks, tick % 20 == 0) + bonus;
		int before = GhostMethods.ghostRank(s);
		double was = s.pathCultivation();
		boolean wasBottleneck = GhostMethods.atBottleneck(c);
		// No method, no gain: without the volume of the path's realm in the method practised (CultivationMethods#whyNoGain; at Four Axis
		// the axes fill it; Corpse Yin Refining its own way) the bar gains nothing, though the foundations' layers behind the path still
		// settle in on what is drawn.
		Component why = CultivationMethods.whyNoGain(p, c, TwoPaths.Path.GHOST);
		boolean gains = why == null;
		if (!gains) tellNoGain(p, TwoPaths.Path.GHOST, GhostMethods.ghostRealm(s), why);
		int stages = GhostMethods.addCultivation(s, gains ? gained : 0);
		// What it won costs years, as the heaven path's does (Lifespan#seclude: the talent race): its boost, its method's stele, the
		// denser yin of the Peaks and the Great Void's battlefields, and a stone in hand saving them.
		Lifespan.seclude(p, before, was, GhostMethods.ghostRank(s), s.pathCultivation(), GhostMethods::required,
				Lifespan.saving(yearsBoost(p, session), active != null && session.stelesNear.contains(active) ? GhostMethods.STELE_BONUS : 1.0,
						Lifespan.ghostPlaceSaving(s.density()), SpiritStoneItem.meditationMultiplier(p)), gained <= 0 ? 0 : 1 - bonus / gained, method);
		boolean settled = GhostMethods.meditate(p, gained, session.stelesNear);
		if (stages > 0) {
			p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.7f);
			GhostMethods.afterRank(p, before); // told, the body refreshed
			p.setHealth(p.getMaxHealth()); // as a stage of the heaven path fills it
			NetherworldManager.refresh(p);
		}
		if (!wasBottleneck && GhostMethods.atBottleneck(c)) {
			p.sendSystemMessage(GhostMethods.heldByPlace(c) ? c.lockLine(GhostMethods.ghostRank(s) + 1)
					: GhostMethods.heldByMethod(c) ? GhostMethods.gateLine(c).copy().withStyle(net.minecraft.ChatFormatting.GOLD)
					: GhostMethods.heldByAxis(c) ? FourAxis.gateLine(c, GhostMethods.ghostRank(s)).copy().withStyle(net.minecraft.ChatFormatting.GOLD)
					: Component.translatable(ModLang.MSG_GHOST_BOTTLENECK).withStyle(net.minecraft.ChatFormatting.AQUA));
		}
		CultivationManager.markDirty(p.server);
		if (tick % 20 == 0 || stages > 0 || settled) Spells.sync(p); // the Cultivation tab's ghost bar, the Methods tab's layers
	}

	/**
	 * Half a second of the heaven methods (see HeavenMethods), the living's: by a Dao Stele of a method not yet known, a step toward
	 * learning it; the layers of every method known that lag behind the realm settle in on the cultivation {@code gathered} this half
	 * second (every boost to meditation in it) by their own elements' share of the qi here, the {@code refined} pill and fruit qi on
	 * top (HeavenMethods#meditate). Returns what {@code gathered} is multiplied by for the cultivation bar: the method practised's
	 * factor here (by its stele, more: HeavenMethods#cultivationFactor), or a cultivator's with none, who is told so once a login.
	 */
	private static double heavenMethods(ServerPlayer p, PlayerCultivation c, Session session, int tick, double gathered, double refined) {
		if (!HeavenMethods.canLearn(c)) {
			session.methodSaving = 1;
			return HeavenMethods.cultivationFactor(c, null, 0, false);
		}
		HeavenState s = c.heaven();
		learnFromDaoSteles(p, c, session, tick);
		java.util.Set<QiElement> byStele = java.util.EnumSet.noneOf(QiElement.class);
		for (HeavenMethod near : session.daoStelesNear) byStele.add(near.element());
		HeavenMethod practised = HeavenMethods.practised(s);
		// Meditation gathering nothing here (CultivationMethods#whyNoGain): no scripture at all, no volume of the realm in the method
		// practised, Four Axis's axes, a method of its own way, Turtle Breath out of seclusion.
		Component why = CultivationMethods.whyNoGain(p, c, TwoPaths.Path.HEAVEN);
		if (why != null && !c.isSuppressed()) tellNoGain(p, TwoPaths.Path.HEAVEN, c.getRealm(), why);
		boolean settling = false;
		for (HeavenMethod method : s.known()) settling |= HeavenMethods.settling(c, method);
		if (settling) {
			boolean settled = HeavenMethods.meditate(p, gathered, refined, byStele);
			CultivationManager.markDirty(p.server);
			if (tick % 20 == 0 && !settled) Spells.sync(p); // the Methods tab's bars (a layer settled has synced already)
		}
		double share = practised == null ? 0 : HeavenMethods.shareAt(p, practised.element());
		boolean stele = practised != null && byStele.contains(practised.element());
		// What the method saves of the years (Lifespan#saving, the user's call, 2026-10-22): its own part of the gain, by its element's
		// share of the qi here and its stele (not a pill's nor the clothing's part).
		session.methodSaving = practised == null ? 1 : HeavenMethods.elementFactor(share) * (stele ? HeavenMethods.STELE_BONUS : 1);
		return HeavenMethods.cultivationFactor(c, practised, share, stele);
	}

	/**
	 * Tells {@code p} once a login, in the chat, that meditating on {@code path} gathers nothing in {@code realm} ({@code line}: why), and
	 * again only in another realm ({@link #NO_GAIN_TOLD}).
	 */
	private static void tellNoGain(ServerPlayer p, TwoPaths.Path path, Realm realm, Component line) {
		if (NO_GAIN_TOLD.computeIfAbsent(p.getUUID(), id -> new java.util.EnumMap<>(TwoPaths.Path.class)).put(path, realm) == realm) return;
		p.sendSystemMessage(line.copy().withStyle(net.minecraft.ChatFormatting.GOLD));
	}

	/** Half a second by the Dao Steles in reach: a step toward learning each heaven method not yet known that one's roots can read. */
	private static void learnFromDaoSteles(ServerPlayer p, PlayerCultivation c, Session session, int tick) {
		HeavenState s = c.heaven();
		if (tick % 40 == 0 || session.ticks <= 10) {
			session.daoStelesInReach = daoStelesWithin(p, HeavenMethods.STELE_LEARN_RADIUS);
			session.daoStelesNear = daoStelesWithin(p, HeavenMethods.STELE_BONUS_RADIUS);
		}
		for (HeavenMethod method : session.daoStelesInReach) {
			// Roots of other elements can't read it (Physique); nor anyone a True Immortal method carved before the worlds stopped teaching them.
			if (s.knows(method) || !c.rootsAllow(method.element()) || !method.realm().taughtInTheWorld()) continue;
			double seconds = session.daoSteleSeconds.merge(method, 0.5, Double::sum);
			if (seconds == 0.5) p.displayClientMessage(Component.translatable(ModLang.MSG_DAO_STELE_READING, method.getDisplayName()), true);
			if (seconds >= HeavenMethods.STELE_LEARN_SECONDS) {
				session.daoSteleSeconds.remove(method);
				HeavenMethods.learn(p, method);
			}
		}
	}

	/** The heaven methods whose Dao Steles stand within {@code radius} of {@code p} (by their middle stone). */
	private static java.util.Set<HeavenMethod> daoStelesWithin(ServerPlayer p, double radius) {
		java.util.Set<HeavenMethod> found = java.util.EnumSet.noneOf(HeavenMethod.class);
		int r = (int) Math.ceil(radius);
		net.minecraft.core.BlockPos at = p.blockPosition();
		for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(at.offset(-r, -2, -r), at.offset(r, 3, r))) {
			var state = p.level().getBlockState(pos);
			if (!state.is(ModBlocks.DAO_STELE) || state.getValue(DaoSteleBlock.PART) != MethodSteleBlock.Part.MIDDLE) continue;
			if (pos.distToCenterSqr(p.getX(), p.getY() + 1, p.getZ()) <= radius * radius) found.add(state.getValue(DaoSteleBlock.METHOD));
		}
		return found;
	}

	/** The methods whose steles stand within {@code radius} of {@code p} (by their middle stone). */
	private static java.util.Set<GhostMethod> stelesWithin(ServerPlayer p, double radius) {
		java.util.Set<GhostMethod> found = java.util.EnumSet.noneOf(GhostMethod.class);
		int r = (int) Math.ceil(radius);
		net.minecraft.core.BlockPos at = p.blockPosition();
		for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(at.offset(-r, -2, -r), at.offset(r, 3, r))) {
			var state = p.level().getBlockState(pos);
			if (!state.is(ModBlocks.METHOD_STELE) || state.getValue(MethodSteleBlock.PART) != MethodSteleBlock.Part.MIDDLE) continue;
			if (pos.distToCenterSqr(p.getX(), p.getY() + 1, p.getZ()) <= radius * radius) found.add(state.getValue(MethodSteleBlock.METHOD));
		}
		return found;
	}

	/**
	 * Tells the player what their surroundings add whenever it changes (sitting down with a boost, a fruit set on a
	 * pedestal mid-meditation...). Nothing is said about having no boost at all, unless one was just lost.
	 */
	private static void announceBoost(ServerPlayer p, Session session) {
		int percent = session.boost.percent();
		if (percent == session.shownPercent) return;
		boolean first = session.shownPercent < 0;
		session.shownPercent = percent;
		if (percent == 0) {
			if (!first) p.displayClientMessage(Component.translatable(ModLang.MSG_BOOST_NONE), true);
			return;
		}
		p.displayClientMessage(Component.translatable(ModLang.MSG_BOOST, percent, sources(session.boost)), true);
	}

	/** "Red Silk Meditation Mat, 3 Spirit Pedestals, High Altitude, Tranquil Surroundings" */
	private static Component sources(CultivationBoost.Breakdown boost) {
		List<Component> parts = new ArrayList<>();
		if (boost.mat() != null) parts.add(boost.mat().getName());
		int pedestals = boost.pedestals().size();
		if (pedestals == 1) parts.add(Component.translatable(ModLang.BOOST_PEDESTAL));
		else if (pedestals > 1) parts.add(Component.translatable(ModLang.BOOST_PEDESTALS, pedestals));
		if (boost.heightBonus() > 0) parts.add(Component.translatable(ModLang.BOOST_HEIGHT));
		if (boost.tranquilBonus() > 0) parts.add(Component.translatable(ModLang.BOOST_TRANQUIL));
		if (boost.gearBonus() > 0) parts.add(Component.translatable(ModLang.BOOST_GARMENTS));
		MutableComponent joined = Component.empty();
		for (int i = 0; i < parts.size(); i++) {
			if (i > 0) joined.append(", ");
			joined.append(parts.get(i));
		}
		return joined;
	}

	/**
	 * Lets Qi Sense users nearby watch the qi flow into the meditator ({@link QiSense}), as fast as they really gather it:
	 * a mat, pedestals and the rest of the boost draw in more.
	 */
	private static void broadcastAbsorption(ServerPlayer p, Session session) {
		PlayerCultivation c = CultivationManager.get(p);
		double perSecond = c.meditationCultivationPerSecond(RingOfPowerItem.cultivationBonus(p)) * session.boost.multiplier() * heldAndTaught(p);
		QiSense.broadcastAbsorption(p, c, perSecond);
	}

	private MeditationManager() {}
}
