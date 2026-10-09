package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;

public final class CultivationEvents {
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(MeditationManager::tick);
		ServerTickEvents.END_SERVER_TICK.register(TribulationManager::tick);
		ServerTickEvents.END_SERVER_TICK.register(CultivationStats::tickHunger);
		ServerTickEvents.END_SERVER_TICK.register(QiManager::tick);
		ServerTickEvents.END_SERVER_TICK.register(SpatialTrialHandler::tick);
		ServerTickEvents.END_SERVER_TICK.register(VoidFallHandler::tick);
		ServerTickEvents.END_SERVER_TICK.register(RealmSuppressionHandler::tick);
		RealmSuppressionHandler.register();

		ServerLifecycleEvents.SERVER_STARTED.register(SpatialRiftBlock::ensureOverworldRift);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> TribulationManager.clear());

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			// Applies stats from the effective (possibly suppressed) stage and syncs the client.
			RealmSuppressionHandler.update(handler.getPlayer());
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			MeditationManager.forget(handler.getPlayer().getUUID());
			TribulationManager.forget(handler.getPlayer().getUUID());
			SpatialTrialHandler.forget(handler.getPlayer().getUUID());
			PortalRestrictionHandler.forget(handler.getPlayer().getUUID());
			QiFlight.forget(handler.getPlayer().getUUID());
		});

		// Rings stay on through the End portal and with keepInventory; on a normal death dropEquipment already emptied them.
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
				RingContainer.of(newPlayer).copyFrom(RingContainer.of(oldPlayer)));

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			// The respawn point may be in another realm: re-evaluate suppression, apply stats, sync.
			RealmSuppressionHandler.update(newPlayer);
			// Leaving the End (alive) copies health onto the new entity before our max-health bonus exists, capping it at 20.
			newPlayer.setHealth(alive ? oldPlayer.getHealth() : newPlayer.getMaxHealth());
		});

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer sp)) return true;
			// The drop back into the Overworld sky after descension doesn't kill on landing.
			if (source.is(DamageTypeTags.IS_FALL) && SpatialTrialHandler.consumeFallProtection(sp)) {
				return false;
			}
			// Any damage breaks concentration.
			if (amount > 0 && MeditationManager.isMeditating(sp.getUUID())) {
				MeditationManager.stop(sp, true);
			}
			return true;
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer sp) {
				TribulationManager.onDeath(sp); // dying mid-tribulation fails the breakthrough
				SpatialTrialHandler.onDeath(sp); // dying in the Spatial Gap ends the trial
			}
		});
	}

	private CultivationEvents() {}
}
