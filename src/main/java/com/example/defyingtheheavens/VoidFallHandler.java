package com.example.defyingtheheavens;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;

/**
 * Falling out of the bottom of a high realm: out of the Upper Realm into the Spatial Gap to begin descension; into the
 * Netherworld's abyss, onto the Soul River and down to the Nether; off the Great Void's continents, into the Jade Firmament's sky. Off
 * the Immortal Realm's peaks through its clouds, no way down at all: the clouds bear one back up (the Gate is its only way down).
 */
public final class VoidFallHandler {
	/** The Upper Realm's floor; vanilla void damage only starts 64 blocks lower, so this always triggers first. */
	public static final int VOID_FALL_Y = ModDimensions.UPPER_REALM_MIN_Y;
	/** The Netherworld's abyss takes a faller this far above its floor, before the void can. */
	public static final int NETHERWORLD_FALL_Y = ModDimensions.NETHERWORLD_MIN_Y + 8;

	public static void tick(MinecraftServer server) {
		if (server == null) return;
		ServerLevel netherworld = server.getLevel(ModDimensions.NETHERWORLD);
		if (netherworld != null) {
			for (ServerPlayer player : new ArrayList<>(netherworld.players())) {
				if (player.isAlive() && !player.isSpectator() && player.getY() < NETHERWORLD_FALL_Y) {
					try {
						// The dead have no river to fall to: a ghost that falls into the abyss disperses and reforms at the Gate (a
						// Ghost King, whose way is its own, rides the river down as the living do).
						if (Incorporeal.isRuledGhost(player)) GhostSystem.disperse(player);
						else SoulRiver.beginDescent(player, false);
					} catch (Exception e) {
						DefyingTheHeavens.LOGGER.error("Abyss fall routing failed for {}", player.getScoreboardName(), e);
					}
				}
			}
		}
		// Off a peak of the Immortal Realm, down through its clouds: borne back up onto its nearest land (ImmortalRealm#bearUp; the Gate is
		// the only way down), a projected soul excepted.
		ServerLevel immortal = server.getLevel(ModDimensions.IMMORTAL_REALM);
		if (immortal != null) {
			for (ServerPlayer player : new ArrayList<>(immortal.players())) {
				if (player.isAlive() && !player.isSpectator() && player.getY() < ImmortalRealm.FALL_Y && !SoulProjection.isAwayFromBody(player.getUUID())) {
					try {
						ImmortalRealm.bearUp(player);
					} catch (Exception e) {
						DefyingTheHeavens.LOGGER.error("Immortal Realm fall routing failed for {}", player.getScoreboardName(), e);
					}
				}
			}
		}
		// Off a continent of the Great Void: down into the Jade Firmament's sky (VoidBreach#fallOut), a projected soul excepted.
		ServerLevel greatVoid = server.getLevel(ModDimensions.GREAT_VOID);
		if (greatVoid != null) {
			for (ServerPlayer player : new ArrayList<>(greatVoid.players())) {
				if (player.isAlive() && !player.isSpectator() && player.getY() < VoidBreach.FALL_Y && !SoulProjection.isAwayFromBody(player.getUUID())) {
					try {
						VoidBreach.fallOut(player);
					} catch (Exception e) {
						DefyingTheHeavens.LOGGER.error("Great Void fall routing failed for {}", player.getScoreboardName(), e);
					}
				}
			}
		}
		ServerLevel upper = server.getLevel(ModDimensions.UPPER_REALM);
		if (upper == null) return;
		for (ServerPlayer player : new ArrayList<>(upper.players())) {
			// A projected soul is kept above the floor by its own rules (see SoulProjection) and never falls out of the realm; one in
			// a person's body falls with it, as anyone would (Possession).
			if (player.isAlive() && !player.isSpectator() && player.getY() < VOID_FALL_Y && !SoulProjection.isAwayFromBody(player.getUUID())) {
				try {
					SpatialTrialHandler.beginDescension(player); // a running tribulation is abandoned on the way in
				} catch (Exception e) {
					DefyingTheHeavens.LOGGER.error("Void fall routing failed for {}", player.getScoreboardName(), e);
				}
			}
		}
	}

	private VoidFallHandler() {}
}
