package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.block.state.BlockState;

public final class CultivationEvents {
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(MeditationManager::tick);
		ServerTickEvents.END_SERVER_TICK.register(TribulationManager::tick);
		ServerTickEvents.END_SERVER_TICK.register(CultivationStats::tickHunger);
		ServerTickEvents.END_SERVER_TICK.register(QiManager::tick);
		ServerTickEvents.END_SERVER_TICK.register(SpatialTrialHandler::tick);
		ServerTickEvents.END_SERVER_TICK.register(SpatialStorms::tick);
		ServerTickEvents.END_SERVER_TICK.register(VoidFallHandler::tick);
		ServerTickEvents.END_SERVER_TICK.register(RealmSuppressionHandler::tick);
		ServerTickEvents.END_SERVER_TICK.register(ConsciousnessDomainHandler::tick);
		ServerTickEvents.END_SERVER_TICK.register(RealmSuppressSystem::tick);
		ServerTickEvents.END_SERVER_TICK.register(Formations::tick);
		ServerTickEvents.END_SERVER_TICK.register(SectManager::tick);
		ServerTickEvents.END_SERVER_TICK.register(NpcSpawner::tick);
		ServerTickEvents.END_SERVER_TICK.register(NpcTravel::tick);
		ServerTickEvents.END_SERVER_TICK.register(Tempering::tick);
		ServerTickEvents.END_SERVER_TICK.register(InnerRealm::tick);
		ServerTickEvents.END_SERVER_TICK.register(SoulProjection::tick);
		ServerTickEvents.END_SERVER_TICK.register(SparSystem::tick);
		ServerTickEvents.END_SERVER_TICK.register(Rumours::tick);
		InnerRealm.registerEvents();
		registerSoulInteractions();
		Tempering.register();
		RealmSuppressionHandler.register();

		// Barrier blocks left in a chunk that was unloaded when their formation came down are swept as it loads.
		ServerChunkEvents.CHUNK_LOAD.register(Formations::onChunkLoad);
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> CultivatorNpc.onLoad(entity));
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> CultivatorNpc.onUnload(entity));

		ServerLifecycleEvents.SERVER_STARTED.register(SpatialRiftBlock::ensureOverworldRift);
		// First: every soul goes home, no penalty, while its player is still in the world to be saved there.
		ServerLifecycleEvents.SERVER_STOPPING.register(SoulProjection::returnAll);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> TribulationManager.clear());
		// Spars end without a result, stakes back to the players still here, before anyone is saved.
		ServerLifecycleEvents.SERVER_STOPPING.register(SparSystem::clear);
		// Before players and chunks are saved, so nobody is saved with the pressure's lowered health.
		ServerLifecycleEvents.SERVER_STOPPING.register(RealmSuppressSystem::releaseAll);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> SpatialStorms.clear());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			Formations.clear();
			CultivatorNpc.clearLoaded();
			SoulProjection.clear();
			Rumours.clearPending();
		});

		// A formation's shell runs through the ground too: those who couldn't break the barrier can't dig under it either.
		// Breaking anything on a sect's grounds is trespass (theft, in the treasury), and its disciples answer it.
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer sp)) return true;
			if (!(state.getBlock() instanceof SectBarrierBlock) && Formations.protectsGround(sp, pos)) return false;
			SectManager.onTrespass(level, sp, pos);
			return true;
		});
		// Opening the treasury's chests, lifting what its pedestals hold, or harvesting the sect's herbs is theft.
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (world.isClientSide || !(world instanceof ServerLevel level) || !(player instanceof ServerPlayer sp) || hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}
			BlockPos pos = hit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			boolean valuable = level.getBlockEntity(pos) instanceof Container || state.getBlock() instanceof SpiritPedestalBlock
					|| state.getBlock() instanceof GinsengBlock || state.getBlock() instanceof CultivationFruitBlock;
			if (valuable) SectManager.onTrespass(level, sp, pos);
			return InteractionResult.PASS;
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			// A tribulation walked out of (logged out, quit, the game closed) fails now, before the stats are applied.
			boolean fled = TribulationManager.onJoin(player);
			// Applies stats from the effective (possibly suppressed) stage and syncs the client.
			// Back from a crash with the soul still inward: return it to the body first.
			if (ModDimensions.isInnerRealm(player.level().dimension())) InnerRealm.leave(player);
			// Likewise a nascent soul still out of its body (a crash, or a server killed mid-way): home, no penalty.
			SoulProjection.onJoin(player);
			RealmSuppressionHandler.update(player);
			if (fled) player.setHealth(Math.min(player.getHealth(), player.getMaxHealth())); // the lower stage's smaller body
			Formations.syncSoon(); // the barriers near them, at once
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			// Walking out of a spar forfeits it (the cultivator keeps the stakes).
			SparSystem.abandon(handler.getPlayer());
			// A soul out of its body goes home (or, fleeing a fight, is severed) while the player can still be moved and saved.
			SoulProjection.onDisconnect(handler.getPlayer());
			SoulProjection.forget(handler.getPlayer().getUUID());
			SoulBarriers.forget(handler.getPlayer().getUUID());
			// First, while the player is still in the world: lifts the pressure on and from them before they're saved.
			RealmSuppressSystem.onDisconnect(handler.getPlayer());
			InnerRealm.onDisconnect(handler.getPlayer()); // the soul returns to its body when they next join
			ConsciousnessDomainHandler.forget(handler.getPlayer().getUUID());
			QiSense.forget(handler.getPlayer().getUUID());
			MeditationManager.forget(handler.getPlayer().getUUID());
			TribulationManager.forget(handler.getPlayer().getUUID());
			SpatialTrialHandler.forget(handler.getPlayer().getUUID());
			PortalRestrictionHandler.forget(handler.getPlayer().getUUID());
			QiFlight.forget(handler.getPlayer().getUUID());
			Tempering.forget(handler.getPlayer().getUUID());
		});

		// Rings stay on through the End portal and with keepInventory; on a normal death dropEquipment already emptied them.
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
				RingContainer.of(newPlayer).copyFrom(RingContainer.of(oldPlayer)));

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			RealmSuppressSystem.onRespawn(newPlayer); // a new body starts unpressed; the next scan presses it again if in reach
			// The respawn point may be in another realm: re-evaluate suppression, apply stats, sync.
			RealmSuppressionHandler.update(newPlayer);
			// Leaving the End (alive) copies health onto the new entity before our max-health bonus exists, capping it at 20.
			newPlayer.setHealth(alive ? oldPlayer.getHealth() : newPlayer.getMaxHealth());
		});

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer sp)) return true;
			// A projected soul is harmed only by cultivators who see it, by their own hand (see SoulProjection#allowSoulDamage).
			if (SoulProjection.isSoul(sp)) return SoulProjection.allowSoulDamage(sp, source);
			// The drop back into the Overworld sky after descension doesn't kill on landing.
			if (source.is(DamageTypeTags.IS_FALL) && SpatialTrialHandler.consumeFallProtection(sp)) {
				return false;
			}
			// A soul in the Inner Realm fights on (its heart demon): only a defeat there sends it back.
			if (InnerRealm.isInside(sp)) return QiSurges.allowInnerDamage(sp, source, amount);
			// Any damage breaks concentration.
			if (amount > 0 && MeditationManager.isMeditating(sp.getUUID())) {
				MeditationManager.stop(sp, true);
			}
			// And any talk (being struck is no time for words); Sworn friends nearby come to help (TalkSystem).
			if (amount > 0 && !sp.isInvulnerableTo(source)) TalkSystem.onPlayerHurt(sp, source);
			return true;
		});

		// Before a death goes ahead: a soul dies where its body sat and loses the nascent soul; a possessed body's death is its
		// possessor's doing.
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			// Nobody dies in a spar by the other's hand: the blow stops at the line, and the spar ends (SparSystem).
			if (!SparSystem.allowDeath(entity, source)) return false;
			if (entity instanceof ServerPlayer sp) SoulProjection.onSoulDying(sp);
			else SoulProjection.onHostDying(entity, source);
			return true;
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer sp) {
				TribulationManager.onDeath(sp); // dying mid-tribulation fails the breakthrough
				SpatialTrialHandler.onDeath(sp); // dying in the Spatial Gap ends the trial
				SoulProjection.onDeath(sp); // nothing of a soul's journey outlives a death
				SparSystem.abandon(sp); // killed mid-spar by something else: the spar is forfeit
				RealmSuppressSystem.refreshNow(); // a fallen suppressor's pressure lifts at once
			}
		});
	}

	/**
	 * A projected soul touches nothing: no blocks, no items, nothing it can use or hit. Its one act is to enter a being (right
	 * click, see SoulProjection#tryPossess), and while it is in one, its blows are that body's. Registered for both sides: the
	 * client's answer decides whether a packet is sent at all.
	 */
	private static void registerSoulInteractions() {
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
				SoulProjection.isSoul(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseBlockCallback.EVENT.register((player, world, hand, hit) ->
				SoulProjection.isSoul(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseItemCallback.EVENT.register((player, world, hand) -> SoulProjection.isSoul(player)
				? InteractionResultHolder.fail(player.getItemInHand(hand)) : InteractionResultHolder.pass(player.getItemInHand(hand)));
		UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (!SoulProjection.isSoul(player)) return InteractionResult.PASS;
			if (hand != InteractionHand.MAIN_HAND || player.isPassenger()) return InteractionResult.FAIL;
			if (world.isClientSide) return InteractionResult.SUCCESS; // the server decides
			return player instanceof ServerPlayer sp ? SoulProjection.tryPossess(sp, entity) : InteractionResult.FAIL;
		});
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (!SoulProjection.isSoul(player)) return InteractionResult.PASS;
			if (!player.isPassenger()) return InteractionResult.FAIL;
			if (world.isClientSide) return InteractionResult.SUCCESS; // sent on to the server, which strikes with the host
			if (player instanceof ServerPlayer sp) SoulProjection.hostAttack(sp, entity);
			return InteractionResult.FAIL;
		});
	}

	private CultivationEvents() {}
}
