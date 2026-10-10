package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Realm Suppression: the Overworld, Nether and End cannot sustain cultivation beyond
 * {@link PlayerCultivation#LOWER_REALM_CAP_REALM} - {@link PlayerCultivation#LOWER_REALM_CAP_STAGE}.
 * In those dimensions a stronger cultivator's effective stage and stats are capped and nobody can progress past the cap;
 * entering the Spatial Gap or the Upper Realm restores the true stage at once. The true stage itself is never changed.
 * Breakthroughs past the cap (into and within Four Axis) are only possible in the Upper Realm itself.
 */
public final class RealmSuppressionHandler {
	private static final int RECHECK_INTERVAL = 20;

	public static void register() {
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> update(player));
	}

	/** Re-evaluates suppression for the player's current dimension, re-applies stats and syncs the client. */
	public static void update(ServerPlayer player) {
		if (player == null || player.level() == null || player.getServer() == null) return;
		try {
			PlayerCultivation c = CultivationManager.get(player);
			boolean wasSuppressed = c.isSuppressed();
			boolean wasSealed = c.isAtBottleneck() && c.isBreakthroughLocked();
			float oldMax = player.getMaxHealth();
			float oldHealth = player.getHealth();
			// A soul in the Inner Realm is held to whatever holds its body.
			ResourceKey<Level> where = InnerRealm.bodyDimension(player);
			c.setLowerRealmBound(ModDimensions.isLowerRealm(where));
			c.setInUpperRealm(ModDimensions.isUpperRealm(where));
			CultivationManager.refresh(player); // stats from the effective stage + save + sync, also on every dimension change

			// Keep the same fraction of vitality when the cap toggles, so a restored cultivator enters the
			// Spatial Gap with their full strength rather than the suppressed health pool.
			if (wasSuppressed != c.isSuppressed() && player.isAlive() && oldMax > 0) {
				player.setHealth(oldHealth * player.getMaxHealth() / oldMax);
			}

			if (c.isSuppressed() && !wasSuppressed) {
				player.sendSystemMessage(Component.translatable(ModLang.MSG_SUPPRESSED,
						PlayerCultivation.rankName(c.getEffectiveRealm(), c.getEffectiveStage())));
			} else if (!c.isSuppressed() && wasSuppressed) {
				player.sendSystemMessage(Component.translatable(ModLang.MSG_RESTORED,
						PlayerCultivation.rankName(c.getRealm(), c.getStage())));
			}
			// A bottleneck that was sealed outside the Upper Realm can be broken now that the player has arrived.
			if (wasSealed && c.canBreakthrough()) {
				player.sendSystemMessage(Component.translatable(ModLang.MSG_BOTTLENECK));
			}
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Realm suppression update failed for {}", player.getScoreboardName(), e);
		}
	}

	/** Safety net for dimension changes that don't fire the world-change event (e.g. some command teleports). */
	public static void tick(MinecraftServer server) {
		if (server == null || server.getTickCount() % RECHECK_INTERVAL != 0) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			PlayerCultivation c = CultivationManager.get(player);
			ResourceKey<Level> dimension = InnerRealm.bodyDimension(player);
			if (c.isLowerRealmBound() != ModDimensions.isLowerRealm(dimension) || c.isInUpperRealm() != ModDimensions.isUpperRealm(dimension)) {
				update(player);
			}
		}
	}

	private RealmSuppressionHandler() {}
}
