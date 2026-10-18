package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Once a second for every player: the ghostly qi density where their body is (walking from the Nether Wastes into a Soul Sand
 * Valley changes it without any change of dimension, so it can't wait for one), the ghostly qi pool filling from it, and yin
 * corruption rising or ebbing (see YinCorruption). The client is told what changed (ModPackets#NETHERWORLD_SYNC), and only
 * then.
 */
public final class NetherworldManager {
	private static final int INTERVAL = 20;

	/** What each player's client was last told, so an unchanged state isn't sent again. */
	private record Sent(long ghostQi, long ghostQiMax, double density, float corruption, int trend, int heldSeconds) {}

	private static final Map<UUID, Sent> SENT = new HashMap<>();
	/** Each player's corruption trend from the last second, for the HUD (1 rising, -1 ebbing, 0 held). */
	private static final Map<UUID, Integer> TREND = new HashMap<>();

	public static void register() {
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> refresh(player));
	}

	public static void tick(MinecraftServer server) {
		if (server == null || server.getTickCount() % INTERVAL != 0) return;
		long now = server.overworld().getGameTime();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			try {
				second(player, now);
			} catch (Exception e) {
				DefyingTheHeavens.LOGGER.error("Netherworld tick failed for {}", player.getScoreboardName(), e);
			}
		}
	}

	/** One player's second (package-private for the game tests). */
	static void second(ServerPlayer player, long now) {
		PlayerCultivation c = CultivationManager.get(player);
		NetherworldState s = c.netherworld();
		refreshDensity(player, s);
		double max = GhostQi.poolMax(c);
		double before = s.ghostQi();
		if (before < max) s.setGhostQi(Math.min(max, before + GhostQi.fillPerSecond(s.density())));
		else if (before > max) s.setGhostQi(max);
		// A soul in another's body (Possession) keeps its own pool apart, and it fills by the place as the body's does: a ghost path
		// soul's upkeep comes out of it (SoulProjection#upkeep). Not a held body's owner: the soul in it plays their record, filled above.
		if (Possession.inhabits(player.getUUID())) {
			PlayerCultivation own = CultivationManager.own(player);
			NetherworldState mine = own.netherworld();
			double ownMax = GhostQi.poolMax(own);
			if (mine.ghostQi() < ownMax) mine.setGhostQi(Math.min(ownMax, mine.ghostQi() + GhostQi.fillPerSecond(s.density())));
			else if (mine.ghostQi() > ownMax) mine.setGhostQi(ownMax);
		}
		// The ghost abilities' upkeep, after the second's filling; a dry pool switches them all off.
		double upkeep = player.isCreative() ? 0 : GhostMethods.upkeep(c);
		if (upkeep > 0) {
			if (s.ghostQi() < upkeep) GhostMethods.exhaust(player, c);
			else s.setGhostQi(s.ghostQi() - upkeep);
		}
		GhostMethods.applyModifiers(player);
		HeavenPassives.apply(player); // the heaven methods' elements, here for the same second's tick
		HeavenMethods.watchSpells(player); // and their spells, which come as the realm reaches them
		double corruption = s.yinCorruption();
		int trend = YinCorruption.second(player, c, now);
		TREND.put(player.getUUID(), trend);
		if (s.ghostQi() != before || s.yinCorruption() != corruption) CultivationManager.markDirty(player.server);
		sync(player);
	}

	/** The density of ghostly qi where the body is: in the Inner Realm, the body's place, not the soul's. */
	private static void refreshDensity(ServerPlayer player, NetherworldState s) {
		ServerLevel body = player.server.getLevel(InnerRealm.bodyDimension(player));
		s.setDensity(body == null ? 0 : GhostQi.densityAt(body, InnerRealm.bodyPos(player)));
	}

	/** At once, not at the next second: a join, a change of dimension, a talisman burnt. */
	public static void refresh(ServerPlayer player) {
		if (player == null || player.getServer() == null) return;
		refreshDensity(player, CultivationManager.get(player).netherworld());
		SENT.remove(player.getUUID());
		sync(player);
	}

	/** Tells the client its Netherworld state if any of it has changed since it was last told. */
	public static void sync(ServerPlayer player) {
		PlayerCultivation c = CultivationManager.get(player);
		NetherworldState s = c.netherworld();
		double max = GhostQi.poolMax(c);
		boolean shows = s.yinCorruption() > 0 || ModDimensions.isNetherworld(player.level().dimension()) && YinCorruption.affects(player);
		float corruption = shows ? (float) s.yinCorruption() : -1.0f;
		int held = (int) Math.max(0, (s.yinPausedUntil() - player.level().getGameTime() + 19) / 20);
		Sent now = new Sent(Math.round(s.ghostQi() * 10), Math.round(max * 10), s.density(), corruption,
				TREND.getOrDefault(player.getUUID(), 0), held);
		if (now.equals(SENT.get(player.getUUID()))) return;
		SENT.put(player.getUUID(), now);
		ModPackets.sendNetherworld(player, s.ghostQi(), max, s.density(), corruption, now.trend(), held);
	}

	public static void forget(UUID id) {
		SENT.remove(id);
		TREND.remove(id);
	}

	public static void clear() {
		SENT.clear();
		TREND.clear();
	}

	private NetherworldManager() {}
}
