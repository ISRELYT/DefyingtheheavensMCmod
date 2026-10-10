package com.example.defyingtheheavens;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Refills every player's qi pool each tick at their qi gather rate, less what flying on it ({@link QiFlight}) and pressing
 * down on others ({@link RealmSuppressSystem}) cost, and keeps the client's Qi bar in step. Spells and techniques spend qi
 * through {@link #trySpend}.
 */
public final class QiManager {
	/** While a pool is filling or draining, its owner is synced this often (in ticks); filling up or running dry syncs at once. */
	private static final int SYNC_INTERVAL = 4;

	public static void tick(MinecraftServer server) {
		boolean syncTick = server.getTickCount() % SYNC_INTERVAL == 0;
		boolean changed = false;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isAlive()) continue;
			PlayerCultivation c = CultivationManager.get(player);
			// A Qi Gathering Pill lasts as long as its effect: milk, death or the timer running out ends the boost.
			if (c.getQiBoost() > 0 && !player.hasEffect(ModEffects.QI_GATHERING)) {
				c.setQiBoost(0);
				changed = true;
				CultivationManager.sync(player);
			}
			if (c.decayPillResistance(1 / 20.0)) {
				changed = true;
				if (server.getTickCount() % 20 == 0) CultivationManager.sync(player); // keeps the menu's pill resistance current
			}
			double flightCost = QiFlight.tick(player, c);
			QiSense.tick(player, c);
			// Flight is paid first: running dry should end the pressure, not drop the cultivator out of the sky.
			double suppressCost = RealmSuppressSystem.upkeep(player, c, flightCost);
			if (!c.gatherQi(1 / 20.0, flightCost + suppressCost)) continue;
			changed = true;
			if (syncTick || c.getQi() >= c.maxQi() || c.getQi() <= 0) {
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
