package com.example.defyingtheheavens;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
	private record Session(double x, double y, double z) {}

	private static final Map<UUID, Session> SESSIONS = new HashMap<>();
	private static final double MAX_DRIFT_SQR = 0.25; // half a block

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
		SESSIONS.put(player.getUUID(), new Session(player.getX(), player.getY(), player.getZ()));
		ModPackets.broadcastMeditation(player, true);
		CultivationManager.sync(player);
		player.displayClientMessage(Component.translatable(ModLang.MSG_START), true);
	}

	public static void stop(ServerPlayer player, boolean interrupted) {
		if (SESSIONS.remove(player.getUUID()) == null) return;
		ModPackets.broadcastMeditation(player, false);
		CultivationManager.sync(player);
		player.displayClientMessage(Component.translatable(interrupted ? ModLang.MSG_INTERRUPTED : ModLang.MSG_STOP), true);
	}

	/** Silent removal (disconnect). */
	public static void forget(UUID id) {
		SESSIONS.remove(id);
	}

	private static boolean canMeditate(ServerPlayer p) {
		return p.isAlive() && !p.isSpectator() && p.onGround() && !p.isInWaterOrBubble()
				&& !p.isPassenger() && !p.isSleeping() && !p.isFallFlying();
	}

	private static boolean stillValid(ServerPlayer p, Session s) {
		double dx = p.getX() - s.x();
		double dz = p.getZ() - s.z();
		return !p.isShiftKeyDown() && p.onGround() && !p.isInWaterOrBubble() && !p.isPassenger()
				&& dx * dx + dz * dz <= MAX_DRIFT_SQR && Math.abs(p.getY() - s.y()) <= 0.5;
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
			} else if (!p.isAlive() || !stillValid(p, entry.getValue())) {
				interrupted.add(p);
			} else {
				tickMeditation(p, tick);
			}
		}
		gone.forEach(SESSIONS::remove);
		interrupted.forEach(p -> stop(p, true));
	}

	private static void tickMeditation(ServerPlayer p, int tick) {
		ServerLevel level = p.serverLevel();
		PlayerCultivation c = CultivationManager.get(p);

		if (tick % 5 == 0) {
			level.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.2, p.getZ(), 6, 0.5, 0.5, 0.5, 0.8);
			if (c.getRealm().ordinal() >= Realm.CORE_FORMATION.ordinal()) {
				level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 0.2, p.getZ(), 2, 0.4, 0.1, 0.4, 0.02);
			}
		}

		// Gain cultivation twice a second so the menu bar moves smoothly.
		if (tick % 10 == 0) {
			boolean wasBottleneck = c.isAtBottleneck();
			boolean advanced = c.addCultivation(c.meditationCultivationPerSecond(RingOfPowerItem.cultivationBonus(p)) * 0.5);
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
			} else if (!wasBottleneck && c.isAtBottleneck() && c.isBreakthroughLocked()) {
				p.sendSystemMessage(Component.translatable(ModLang.MSG_REALM_LOCKED,
						PlayerCultivation.rankName(PlayerCultivation.LOWER_REALM_CAP_REALM, PlayerCultivation.LOWER_REALM_CAP_STAGE)));
			}
		}

		if (tick % 20 == 0) {
			ModPackets.broadcastMeditation(p, true); // heartbeat for late joiners / newly tracking players
		}
	}

	private MeditationManager() {}
}
