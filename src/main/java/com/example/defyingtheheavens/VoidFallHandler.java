package com.example.defyingtheheavens;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;

/** Falling out of the bottom of the Upper Realm drops the player into the Spatial Gap to begin descension. */
public final class VoidFallHandler {
	/** The Upper Realm's floor; vanilla void damage only starts 64 blocks lower, so this always triggers first. */
	public static final int VOID_FALL_Y = ModDimensions.UPPER_REALM_MIN_Y;

	public static void tick(MinecraftServer server) {
		if (server == null) return;
		ServerLevel upper = server.getLevel(ModDimensions.UPPER_REALM);
		if (upper == null) return;
		for (ServerPlayer player : new ArrayList<>(upper.players())) {
			if (player.isAlive() && !player.isSpectator() && player.getY() < VOID_FALL_Y) {
				try {
					// A running tribulation would otherwise block the descent and leave the player to die in the void.
					TribulationManager.abandon(player);
					SpatialTrialHandler.beginDescension(player);
				} catch (Exception e) {
					DefyingTheHeavens.LOGGER.error("Void fall routing failed for {}", player.getScoreboardName(), e);
				}
			}
		}
	}

	private VoidFallHandler() {}
}
