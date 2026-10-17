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
		/** Cultivation won from surge games (wisps, a walked sequence, a slain heart demon), added on the next gain. */
		double bonusCultivation;

		/** Ticks left in which a soul just arrived in the Inner Realm settles onto the island (moving doesn't count yet). */
		int settling;

		// The ghost methods (see GhostMethods): the Method Steles near, re-read every few seconds; seconds spent by the stele of
		// each method not yet known; and whether the outside cap has been told.
		java.util.Set<GhostMethod> stelesNear = java.util.EnumSet.noneOf(GhostMethod.class);
		java.util.Set<GhostMethod> stelesInReach = java.util.EnumSet.noneOf(GhostMethod.class);
		final Map<GhostMethod, Double> steleSeconds = new java.util.EnumMap<>(GhostMethod.class);
		boolean capTold;
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
	/** Players told this login that they meditate without a heaven method (not saved). */
	private static final java.util.Set<UUID> NO_METHOD_TOLD = new java.util.HashSet<>();
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
		if (TribulationManager.isActive(player.getUUID())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_TRIB_BUSY), true);
			return;
		}
		if (SoulProjection.isActive(player.getUUID())) { // a soul out of its body (or leaving it) can't settle into meditation
			player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_BUSY), true);
			return;
		}
		// A mortal's meridians are closed, the ghost path's as the heaven path's: alive or dead, it has nothing to cultivate with. Shattered
		// meridians, alive, sit to temper the body instead (BodyRefining).
		PlayerCultivation sitting = CultivationManager.get(player);
		if (sitting.isMortal() && (!sitting.hasShatteredMeridians() || GhostSystem.isGhost(player) || !sitting.canTemper())) {
			player.displayClientMessage(Component.translatable(GhostSystem.isGhost(player) ? ModLang.MSG_METHOD_MERIDIANS_DEAD : ModLang.MSG_MORTAL_MEDITATE), true);
			return;
		}
		// The dead cultivate only a ghost method: one chosen, or one to learn from a stele close by.
		if (GhostSystem.isGhost(player) && CultivationManager.get(player).netherworld().activeMethod() == null && unknownStelesNear(player).isEmpty()) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_GHOST_NO_METHOD), true);
			return;
		}
		if (HeavenRitual.isActive(player.getUUID())) { // kneeling before the heavens already
			player.displayClientMessage(Component.translatable(ModLang.MSG_RITUAL_BUSY), true);
			return;
		}
		if (!canMeditate(player)) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_CANNOT), true);
			return;
		}
		Session session = new Session(player.getX(), player.getY(), player.getZ());
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
		NO_METHOD_TOLD.remove(id);
		QiSurges.forget(id);
		SoulCrystalBlock.forget(id);
	}

	/** Whether {@link #start} would begin a session right now: no tribulation running, and steady, dry ground. */
	public static boolean canStart(ServerPlayer player) {
		return !TribulationManager.isActive(player.getUUID()) && !SoulProjection.isActive(player.getUUID()) && canMeditate(player);
	}

	private static boolean canMeditate(ServerPlayer p) {
		return p.isAlive() && !p.isSpectator() && p.onGround() && !p.isInWaterOrBubble()
				&& !p.isPassenger() && !p.isSleeping() && !p.isFallFlying();
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

	/** Cultivation worth {@code seconds} of this meditation's current rate, added with the next gain. */
	public static void addBonusSeconds(ServerPlayer p, double seconds) {
		Session s = SESSIONS.get(p.getUUID());
		if (s != null) s.bonusCultivation += ratePerSecond(p, s) * seconds;
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
	 * A spirit stone in hand (SpiritStoneItem), a teacher's Insight (TalkSystem guidance) and one's own sect's grounds
	 * (SectMembership#groundsMultiplier), each speeding the gain.
	 */
	private static double heldAndTaught(ServerPlayer p) {
		return SpiritStoneItem.meditationMultiplier(p) * (p.hasEffect(ModEffects.INSIGHT) ? 1.0 + ModEffects.INSIGHT_BONUS : 1.0)
				* SectMembership.groundsMultiplier(p);
	}

	private static boolean stillValid(ServerPlayer p, Session s) {
		double dx = p.getX() - s.x;
		double dz = p.getZ() - s.z;
		return !p.isShiftKeyDown() && p.onGround() && !p.isInWaterOrBubble() && !p.isPassenger()
				&& dx * dx + dz * dz <= MAX_DRIFT_SQR && Math.abs(p.getY() - s.y) <= 0.5;
	}

	public static void tick(MinecraftServer server) {
		if (SESSIONS.isEmpty()) return;
		int tick = server.getTickCount();
		List<ServerPlayer> interrupted = new ArrayList<>();
		List<UUID> gone = new ArrayList<>();

		for (Map.Entry<UUID, Session> entry : SESSIONS.entrySet()) {
			ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
			if (p == null) {
				gone.add(entry.getKey());
			} else if (!p.isAlive() || (entry.getValue().settling <= 0 && !InnerRealm.isInside(p) && !stillValid(p, entry.getValue()))) {
				// A soul in the Inner Realm walks freely: only the meditate key (or harm to the body) ends it.
				interrupted.add(p);
			} else {
				tickMeditation(p, entry.getValue(), tick);
			}
		}
		gone.forEach(SESSIONS::remove);
		interrupted.forEach(p -> stop(p, true));
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
		// After a while the soul turns inward (an ability that can be switched off), the eyes closing first.
		if (session.ticks == InnerRealm.ENTER_AFTER_TICKS - InnerRealm.FADE_TICKS && InnerRealm.canEnter(p)) {
			ModPackets.sendInnerFade(p);
		}
		if (session.ticks == InnerRealm.ENTER_AFTER_TICKS && InnerRealm.enter(p)) {
			session.settling = 40;
			session.x = p.getX();
			session.y = p.getY();
			session.z = p.getZ();
			ModPackets.broadcastMeditation(p, true);
		}
		if (session.ticks % BOOST_REFRESH_TICKS == 0) session.boost = CultivationBoost.of(p);
		if (session.ticks >= BOOST_ANNOUNCE_DELAY) announceBoost(p, session);
		if (c.isMortal()) { // shattered meridians: no qi to circulate, blood instead, tempering the body (BodyRefining)
			if (tick % 10 == 0) Tempering.gain(p, BodyRefining.meditationPerSecond(c) * 0.5 * heldAndTaught(p));
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

		// The ghost methods, as often: learning from a stele, cultivating the one practised.
		if (tick % 10 == 0) ghostMethods(p, c, session, tick);
		// Gain cultivation twice a second so the menu bar moves smoothly. The dead's meditation feeds only their ghost method.
		if (tick % 10 == 0 && !GhostSystem.isGhost(p)) {
			boolean wasBottleneck = c.isAtBottleneck();
			boolean wasAtGate = c.isAtRitualGate();
			boolean wasHeld = c.isHeldByMethod();
			// Mats, fruit on pedestals, height and tranquillity speed the gain (see CultivationBoost).
			double perSecond = ratePerSecond(p, session);
			// Unrefined qi from pills and fruit is refined on top, at REFINE_RATE times the meditation rate.
			double refined = c.refine(perSecond * PlayerCultivation.REFINE_RATE * 0.5);
			double gathered = perSecond * 0.5 + session.bonusCultivation;
			session.bonusCultivation = 0;
			// What is gathered goes by the heaven method practised (its element's qi here: HeavenMethods#cultivationFactor), and the
			// methods' layers behind the realm settle in on it.
			double factor = heavenMethods(p, c, session, tick, gathered, refined);
			boolean advanced = c.addCultivation(gathered * factor + refined);
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
				p.sendSystemMessage(Component.translatable(ModLang.MSG_REALM_LOCKED,
						PlayerCultivation.rankName(PlayerCultivation.LOWER_REALM_CAP_REALM, PlayerCultivation.LOWER_REALM_CAP_STAGE)));
			} else if (!wasAtGate && c.isAtRitualGate()) {
				p.sendSystemMessage(Component.translatable(ModLang.MSG_RITUAL_GATE));
			}
			// Not else-if: a pill and a method may both be wanted.
			if (!wasHeld && c.isHeldByMethod()) p.sendSystemMessage(HeavenMethods.gateLine(c).copy().withStyle(net.minecraft.ChatFormatting.GOLD));
		}

		if (tick % 20 == 0) {
			ModPackets.broadcastMeditation(p, true); // heartbeat for late joiners / newly tracking players
			broadcastAbsorption(p, session);
			SpiritStoneItem.drainWhileMeditating(p); // a second of a held stone's qi, if one is held
		}
	}

	/**
	 * Half a second of the ghost methods (see GhostMethods): by a stele of a method not yet known, a step toward learning it;
	 * the method practised gains progress wherever ghostly qi flows, as fast as it is dense (by its stele, faster), layer by layer,
	 * held where the ghost path can carry it no further. Timed by the server's {@code tick} (it runs on every tenth): the session's own count is offset from it,
	 * so tested against that, the steles were never re-read and the Methods tab's bar never sent but at a new layer.
	 */
	private static void ghostMethods(ServerPlayer p, PlayerCultivation c, Session session, int tick) {
		if (c.isMortal()) return; // a mortal's meridians are closed: nothing learned, nothing cultivated (it can't sit down to it either)
		NetherworldState s = c.netherworld();
		if (tick % 40 == 0 || session.ticks <= 10) {
			session.stelesInReach = stelesWithin(p, GhostMethods.STELE_LEARN_RADIUS);
			session.stelesNear = stelesWithin(p, GhostMethods.STELE_BONUS_RADIUS);
		}
		for (GhostMethod method : session.stelesInReach) {
			if (s.knows(method) || !GhostMethods.canLearn(c, method)) continue;
			double seconds = session.steleSeconds.merge(method, 0.5, Double::sum);
			if (seconds == 0.5) p.displayClientMessage(Component.translatable(ModLang.MSG_STELE_READING, method.getDisplayName()), true);
			if (seconds >= GhostMethods.STELE_LEARN_SECONDS) {
				session.steleSeconds.remove(method);
				GhostMethods.learn(p, method);
			}
		}
		GhostMethod active = s.activeMethod();
		if (active == null || s.density() <= 0) return;
		double rate = GhostMethods.ratePerSecond(c, s.density()) * circulationMultiplier(session, session.ticks)
				* (session.stelesNear.contains(active) ? GhostMethods.STELE_BONUS : 1.0)
				* (1 + (p.hasEffect(ModEffects.YIN_GATHERING) ? s.methodBoost() : 0)); // a Yin Gathering Pill
		int cap = GhostMethods.layerCap(c, active);
		int ghostPathBefore = GhostMethods.ghostRank(s);
		int result = GhostMethods.cultivate(s, active, rate * 0.5, cap);
		if (result > 0) {
			p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.6f);
			p.displayClientMessage(Component.translatable(ModLang.MSG_METHOD_LAYER, active.getDisplayName(), s.layer(active)), true);
			GhostMethods.afterLayer(p, s, ghostPathBefore);
			NetherworldManager.refresh(p);
		} else if (result < 0 && !session.capTold) {
			session.capTold = true;
			p.displayClientMessage(Component.translatable(ModLang.MSG_METHOD_REALM_CAPPED, active.getDisplayName(), cap,
					GhostMethods.realmToPass(active, cap).getDisplayName()), true);
		}
		CultivationManager.markDirty(p.server);
		if (tick % 20 == 0 || result != 0) Spells.sync(p); // the Methods tab's bar
	}

	/**
	 * Half a second of the heaven methods (see HeavenMethods), the living's: by a Dao Stele of a method not yet known, a step toward
	 * learning it; the layers of every method known that lag behind the realm settle in on the cultivation {@code gathered} this half
	 * second (every boost to meditation in it) by their own elements' share of the qi here, the {@code refined} pill and fruit qi on
	 * top (HeavenMethods#meditate). Returns what {@code gathered} is multiplied by for the cultivation bar: the method practised's
	 * factor here (by its stele, more: HeavenMethods#cultivationFactor), or a cultivator's with none, who is told so once a login.
	 */
	private static double heavenMethods(ServerPlayer p, PlayerCultivation c, Session session, int tick, double gathered, double refined) {
		if (!HeavenMethods.canLearn(c)) return HeavenMethods.cultivationFactor(c, null, 0, false);
		HeavenState s = c.heaven();
		if (tick % 40 == 0 || session.ticks <= 10) {
			session.daoStelesInReach = daoStelesWithin(p, HeavenMethods.STELE_LEARN_RADIUS);
			session.daoStelesNear = daoStelesWithin(p, HeavenMethods.STELE_BONUS_RADIUS);
		}
		for (HeavenMethod method : session.daoStelesInReach) {
			if (s.knows(method) || !c.rootsAllow(method.element())) continue; // roots of other elements can't read it (Physique)
			double seconds = session.daoSteleSeconds.merge(method, 0.5, Double::sum);
			if (seconds == 0.5) p.displayClientMessage(Component.translatable(ModLang.MSG_DAO_STELE_READING, method.getDisplayName()), true);
			if (seconds >= HeavenMethods.STELE_LEARN_SECONDS) {
				session.daoSteleSeconds.remove(method);
				HeavenMethods.learn(p, method);
			}
		}
		java.util.Set<QiElement> byStele = java.util.EnumSet.noneOf(QiElement.class);
		for (HeavenMethod near : session.daoStelesNear) byStele.add(near.element());
		HeavenMethod practised = HeavenMethods.practised(s);
		if (practised == null && !c.isSuppressed() && NO_METHOD_TOLD.add(p.getUUID())) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_HEAVEN_NONE_KNOWN).withStyle(net.minecraft.ChatFormatting.GOLD), true);
		}
		boolean settling = false;
		for (HeavenMethod method : s.known()) settling |= HeavenMethods.settling(c, method);
		if (settling) {
			boolean settled = HeavenMethods.meditate(p, gathered, refined, byStele);
			CultivationManager.markDirty(p.server);
			if (tick % 20 == 0 && !settled) Spells.sync(p); // the Methods tab's bars (a layer settled has synced already)
		}
		return HeavenMethods.cultivationFactor(c, practised, practised == null ? 0 : HeavenMethods.shareAt(p, practised.element()),
				practised != null && byStele.contains(practised.element()));
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

	/** The methods by whose steles {@code p} sits that they don't know yet. */
	private static java.util.Set<GhostMethod> unknownStelesNear(ServerPlayer p) {
		java.util.Set<GhostMethod> near = stelesWithin(p, GhostMethods.STELE_LEARN_RADIUS);
		near.removeIf(CultivationManager.get(p).netherworld()::knows);
		return near;
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
