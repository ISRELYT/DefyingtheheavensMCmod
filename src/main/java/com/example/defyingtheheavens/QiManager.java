package com.example.defyingtheheavens;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Refills every player's qi pool each tick at their qi gather rate and keeps the client's Qi bar in step. Spells and
 * techniques spend qi through {@link #trySpend}.
 */
public final class QiManager {
	/** While a pool is filling, its owner is synced this often (in ticks); filling up syncs at once. */
	private static final int SYNC_INTERVAL = 4;

	public static void tick(MinecraftServer server) {
		boolean syncTick = server.getTickCount() % SYNC_INTERVAL == 0;
		boolean changed = false;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isAlive()) continue;
			PlayerCultivation c = CultivationManager.get(player);
			if (!c.gatherQi(1 / 20.0)) continue;
			changed = true;
			if (syncTick || c.getQi() >= c.maxQi()) {
				CultivationManager.sync(player);
			}
		}
		if (changed) CultivationManager.markDirty(server);
	}

	/** Spends qi and tells the client. @return false, spending nothing, if the player holds less than {@code amount} */
	public static boolean trySpend(ServerPlayer player, double amount) {
		PlayerCultivation c = CultivationManager.get(player);
		if (!c.consumeQi(amount)) return false;
		CultivationManager.markDirty(player.server);
		CultivationManager.sync(player);
		return true;
	}

	private QiManager() {}
}
