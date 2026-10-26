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
 * entering a passage or a high realm (the Upper Realm, the Netherworld) restores the true stage at once. The true stage itself
 * is never changed. Breakthroughs past the cap (into and within Four Axis) are only possible in the Upper Realm itself. From Void
 * Shattering on (either path: PlayerCultivation#FREE_OF_LOWER_REALMS, the user's ask of 2026-10-22) the lower realms hold no one:
 * the whole might there, and their thin qi gathered, though the breakthroughs still wait for the Upper Realm. A breakthrough into True
 * Immortal or past it waits for the Immortal Realm itself (PlayerCultivation#isImmortalRealmLocked, the user's rule of 2026-10-26).
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
			applyRealm(c, InnerRealm.bodyDimension(player));
			CultivationManager.refresh(player); // stats from the effective stage + save + sync, also on every dimension change

			// Keep the same fraction of vitality when the cap toggles, so a restored cultivator enters the
			// Spatial Gap with their full strength rather than the suppressed health pool.
			if (wasSuppressed != c.isSuppressed() && player.isAlive() && oldMax > 0) {
				player.setHealth(oldHealth * player.getMaxHealth() / oldMax);
			}

			if (c.isSuppressed() && !wasSuppressed) {
				player.displayClientMessage(Component.translatable(ModLang.MSG_SUPPRESSED,
						PlayerCultivation.rankName(c.getEffectiveRealm(), c.getEffectiveStage())), true);
			} else if (!c.isSuppressed() && wasSuppressed) {
				player.displayClientMessage(Component.translatable(ModLang.MSG_RESTORED,
						PlayerCultivation.rankName(c.getRealm(), c.getStage())), true);
			}
			// A bottleneck that was sealed outside the Upper Realm (or, into True Immortal and past it, the Immortal Realm) can be broken
			// now that the player has arrived.
			if (wasSealed && c.canBreakthrough()) {
				player.sendSystemMessage(Component.translatable(ModLang.MSG_BOTTLENECK));
			}
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Realm suppression update failed for {}", player.getScoreboardName(), e);
		}
	}

	/**
	 * What the realm a cultivator stands in does to their cultivation: the lower realms (the Nether among them) hold them to
	 * the cap; the high realms lift it ({@link HighRealm}); only the Upper Realm is dense in ordinary qi and lets them break
	 * through past the cap. The Netherworld lifts the cap with ordinary qi as thin as anywhere.
	 */
	static void applyRealm(PlayerCultivation c, ResourceKey<Level> where) {
		c.setLowerRealmBound(ModDimensions.isLowerRealm(where));
		c.setInUpperRealm(ModDimensions.isUpperRealm(where));
		c.setInImmortalRealm(ModDimensions.isImmortalRealm(where)); // the only place a breakthrough into True Immortal and past it is made
		// The Astral Realm's density is the place's own, kept by AstralRealm as one moves; everywhere else, the realm's: the Great Void's
		// a hundredfold (PlayerCultivation#GREAT_VOID_QI_MULTIPLIER), the Immortal Realm's a thousandfold, the rest the lower realms' own.
		if (!ModDimensions.isAstral(where)) c.setPlaceQi(realmQi(where));
	}

	/**
	 * How dense the qi of the realm {@code where} is, against the lower realms' (the Astral Realm's places aside): the Immortal Realm's,
	 * and the small worlds refined in it (SmallWorlds), a thousandfold.
	 */
	public static double realmQi(ResourceKey<Level> where) {
		return ModDimensions.hasImmortalQi(where) ? PlayerCultivation.IMMORTAL_QI_MULTIPLIER
				: ModDimensions.isGreatVoid(where) ? PlayerCultivation.GREAT_VOID_QI_MULTIPLIER : 1.0;
	}

	/** Safety net for dimension changes that don't fire the world-change event (e.g. some command teleports). */
	public static void tick(MinecraftServer server) {
		if (server == null || server.getTickCount() % RECHECK_INTERVAL != 0) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			PlayerCultivation c = CultivationManager.get(player);
			ResourceKey<Level> dimension = InnerRealm.bodyDimension(player);
			if (c.isLowerRealmBound() != ModDimensions.isLowerRealm(dimension) || c.isInUpperRealm() != ModDimensions.isUpperRealm(dimension)
					|| c.isInImmortalRealm() != ModDimensions.isImmortalRealm(dimension)
					|| !ModDimensions.isAstral(dimension) && !ModDimensions.isUpperRealm(dimension) && c.placeFactor() != realmQi(dimension)) {
				update(player);
			}
		}
	}

	private RealmSuppressionHandler() {}
}
