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
		/** Qi circulation (see {@link #onCirculation}): clean circuits in a row, and when the last one was made. */
		int circuitStreak;
		int lastCircuitTick = -CIRCULATION_LASTS;

		/** Ticks left in which a soul just arrived in the Inner Realm settles onto the island (moving doesn't count yet). */
		int settling;

		Session(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	private static final Map<UUID, Session> SESSIONS = new HashMap<>();
	/** Each clean circuit in a row speeds meditation by this much more, up to {@link #MAX_STREAK} of them. */
	public static final double CIRCULATION_BONUS = 0.5;
	public static final int MAX_STREAK = 2;
	/** The circulation bonus lasts this long after the last clean circuit (ticks). */
	public static final int CIRCULATION_LASTS = 600;
	/** Qi deviation costs this share of the qi pool, and a little health (which also breaks the meditation). */
	public static final double DEVIATION_QI_LOSS = 0.25;
	public static final float DEVIATION_DAMAGE = 2.0f;
	public static final int CIRCUIT_CLEAN = 0, CIRCUIT_BROKEN = 1, CIRCUIT_DEVIATION = 2;
	private static final double MAX_DRIFT_SQR = 0.25; // half a block
	private static final int BOOST_REFRESH_TICKS = 20;
	/** The boost is first announced this long after sitting down, so the "you begin to gather qi" message is read first. */
	private static final int BOOST_ANNOUNCE_DELAY = 40;

	public static boolean isMeditating(UUID id) {
		return SESSIONS.containsKey(id);
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
		if (!canMeditate(player)) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_CANNOT), true);
			return;
		}
		if (CultivationManager.get(player).isMortal()) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_MORTAL_MEDITATE), true);
			return;
		}
		Session session = new Session(player.getX(), player.getY(), player.getZ());
		session.boost = CultivationBoost.of(player);
		SESSIONS.put(player.getUUID(), session);
		ModPackets.broadcastMeditation(player, true);
		broadcastAbsorption(player, session);
		CultivationManager.sync(player);
		player.displayClientMessage(Component.translatable(ModLang.MSG_START), true);
	}

	public static void stop(ServerPlayer player, boolean interrupted) {
		if (SESSIONS.remove(player.getUUID()) == null) return;
		boolean inside = InnerRealm.isInside(player);
		if (inside) InnerRealm.leave(player); // getting up brings the soul back to the body
		ModPackets.broadcastMeditation(player, false);
		CultivationManager.sync(player);
		player.displayClientMessage(Component.translatable(interrupted ? ModLang.MSG_INTERRUPTED : ModLang.MSG_STOP), true);
	}

	/** Silent removal (disconnect). */
	public static void forget(UUID id) {
		SESSIONS.remove(id);
	}

	/** Whether {@link #start} would begin a session right now: no tribulation running, and steady, dry ground. */
	public static boolean canStart(ServerPlayer player) {
		return !TribulationManager.isActive(player.getUUID()) && canMeditate(player);
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
	 * The qi circulation game, played on the client while meditating (see QiCirculationHud): a clean circuit builds the
	 * streak, a broken one resets it, and three wrong presses in a row are a qi deviation.
	 */
	public static void onCirculation(ServerPlayer p, int outcome) {
		Session s = SESSIONS.get(p.getUUID());
		if (s == null || CultivationManager.get(p).isMortal()) return;
		switch (outcome) {
			case CIRCUIT_CLEAN -> {
				circulationMultiplier(s, s.ticks); // lapse an expired streak first
				s.circuitStreak++;
				s.lastCircuitTick = s.ticks;
			}
			case CIRCUIT_BROKEN -> s.circuitStreak = 0;
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
			} else if (!p.isAlive() || (entry.getValue().settling <= 0 && !stillValid(p, entry.getValue()))) {
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
		// After a while the soul turns inward (an ability that can be switched off).
		if (session.ticks == InnerRealm.ENTER_AFTER_TICKS && InnerRealm.enter(p)) {
			session.settling = 40;
			session.x = p.getX();
			session.y = p.getY();
			session.z = p.getZ();
			ModPackets.broadcastMeditation(p, true);
		}
		if (session.ticks % BOOST_REFRESH_TICKS == 0) session.boost = CultivationBoost.of(p);
		if (session.ticks >= BOOST_ANNOUNCE_DELAY) announceBoost(p, session);

		if (tick % 5 == 0) {
			level.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.2, p.getZ(), 6, 0.5, 0.5, 0.5, 0.8);
			if (c.getRealm().ordinal() >= Realm.CORE_FORMATION.ordinal()) {
				level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 0.2, p.getZ(), 2, 0.4, 0.1, 0.4, 0.02);
			}
		}

		// Gain cultivation twice a second so the menu bar moves smoothly.
		if (tick % 10 == 0) {
			boolean wasBottleneck = c.isAtBottleneck();
			// Mats, fruit on pedestals, height and tranquillity speed the gain (see CultivationBoost).
			double perSecond = c.meditationCultivationPerSecond(RingOfPowerItem.cultivationBonus(p)) * session.boost.multiplier()
					* circulationMultiplier(session, session.ticks) * (InnerRealm.isInside(p) ? InnerRealm.CULTIVATION_BONUS : 1.0);
			// Unrefined qi from pills and fruit is refined on top, at REFINE_RATE times the meditation rate.
			double refined = c.refine(perSecond * PlayerCultivation.REFINE_RATE * 0.5);
			boolean advanced = c.addCultivation(perSecond * 0.5 + refined);
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
			}
		}

		if (tick % 20 == 0) {
			ModPackets.broadcastMeditation(p, true); // heartbeat for late joiners / newly tracking players
			broadcastAbsorption(p, session);
		}
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
		double perSecond = c.meditationCultivationPerSecond(RingOfPowerItem.cultivationBonus(p)) * session.boost.multiplier();
		QiSense.broadcastAbsorption(p, c, perSecond);
	}

	private MeditationManager() {}
}
