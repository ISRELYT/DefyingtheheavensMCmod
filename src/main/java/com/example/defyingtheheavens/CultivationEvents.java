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
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public final class CultivationEvents {
	/** Players whose leaving is done (#leaving): once for each login, whichever way it is told first. */
	private static final java.util.Set<ServerPlayer> LEFT = java.util.Collections.synchronizedSet(java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>()));

	/**
	 * {@code player} is leaving the game, on the server thread while they are still in the world, before vanilla saves them: what they
	 * were in the middle of ends or is put right first, and what the systems noted of them goes. Once for each login.
	 */
	public static void leaving(ServerPlayer player) {
		if (!LEFT.add(player)) return;
		// Walking out of a spar forfeits it (the cultivator keeps the stakes).
		SparSystem.abandon(player);
		// A golden core breaking bursts as its cultivator goes.
		CoreDetonation.onDisconnect(player);
		// One in their clone goes back to their main body first (the clone stands on where it was): saved there (Clones).
		Clones.onDisconnect(player);
		// One whose body is held doesn't take it with them: it plays on under the soul, and they watch again as they come back (Possession).
		Possession.onDisconnect(player);
		// A bearer sets down those in its sleeve where each was taken in; one in a sleeve stays in it (Sleeves).
		Sleeves.onDisconnect(player);
		// A soul out of its body goes home (or, fleeing a fight, is severed) while the player can still be moved and saved; one in
		// a person's body gives it back first.
		SoulProjection.onDisconnect(player);
		SoulProjection.forget(player.getUUID());
		SoulBarriers.forget(player.getUUID());
		// First, while the player is still in the world: lifts the pressure on and from them before they're saved.
		RealmSuppressSystem.onDisconnect(player);
		InnerRealm.onDisconnect(player); // the soul returns to its body when they next join
		ConsciousnessDomainHandler.forget(player.getUUID());
		QiSense.forget(player.getUUID());
		MeditationManager.forget(player.getUUID());
		TribulationManager.forget(player.getUUID());
		SpatialTrialHandler.forget(player.getUUID());
		VoidBreach.forget(player.getUUID());
		ImmortalGate.forget(player.getUUID());
		VoidCurrents.forget(player.getUUID());
		SectMembership.forget(player.getUUID());
		AstralRealm.forget(player.getUUID());
		SoulRiver.forget(player.getUUID());
		NetherworldManager.forget(player.getUUID());
		GhostSystem.forget(player.getUUID());
		Spells.forget(player.getUUID());
		PortalRestrictionHandler.forget(player.getUUID());
		QiFlight.forget(player.getUUID());
		RaceGifts.onDisconnect(player); // an Illusion up ends (the gifts' cooldowns outlast a logout)
		Tempering.forget(player.getUUID());
		HeavenRitual.forget(player.getUUID());
		Lifespan.forget(player.getUUID());
		LifespanTribulation.forget(player.getUUID()); // it waits on the record, as strong as it was
		BurnLifespan.onDisconnect(player); // a lifespan burning goes out (not lit again by logging in)
		ImmortalEssence.onDisconnect(player); // so does immortal essence lit
		LawDomains.onDisconnect(player); // and a Law's domain folds away
		ChaosFrontier.onDisconnect(player);
		SmallWorlds.onDisconnect(player); // a small world's door closes
		PlayerTitles.onDisconnect(player); // their plate gone from everyone's view
		FiveDecays.forget(player.getUUID()); // a judgement under way comes again after the return (the record keeps it)
		Wishes.forget(player.getUUID());
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(MeditationManager::tick);
		ServerTickEvents.END_SERVER_TICK.register(TribulationManager::tick);
		ServerTickEvents.END_SERVER_TICK.register(CultivationStats::tickHunger);
		ServerTickEvents.END_SERVER_TICK.register(QiManager::tick);
		ServerTickEvents.END_SERVER_TICK.register(SpatialTrialHandler::tick);
		ServerTickEvents.END_SERVER_TICK.register(VoidBreach::tick);
		ServerTickEvents.END_SERVER_TICK.register(ImmortalGate::tick); // the Immortal Gate's opening, the way up and down
		ServerTickEvents.END_SERVER_TICK.register(Sleeves::tick); // those carried in a sleeve, wherever their bearer goes
		ServerTickEvents.END_SERVER_TICK.register(CityMarkets::keep); // the Immortal Realm's markets, their merchants at their stalls
		ServerTickEvents.END_SERVER_TICK.register(CelestialCourt::tick); // the Celestial Court's people at their posts, the Gate's watch counted
		ServerTickEvents.END_SERVER_TICK.register(PrimordialBeasts::tick); // the primordial beasts come to their lairs
		ServerTickEvents.END_SERVER_TICK.register(VoidCurrents::tick); // the Great Void's drifting currents
		ServerTickEvents.END_SERVER_TICK.register(SoulRiver::tick);
		ServerTickEvents.END_SERVER_TICK.register(NetherworldManager::tick);
		ServerTickEvents.END_SERVER_TICK.register(GhostSystem::tick);
		ServerTickEvents.END_SERVER_TICK.register(Lifespan::tick);
		ServerTickEvents.END_SERVER_TICK.register(LifespanTribulation::tick); // the heavens collecting a spent life
		ServerTickEvents.END_SERVER_TICK.register(BurnLifespan::tick); // years set alight for strength
		ServerTickEvents.END_SERVER_TICK.register(ImmortalEssence::tick); // a True Immortal's essence, filled in the Immortal Realm, leaking elsewhere
		ServerTickEvents.END_SERVER_TICK.register(LawDomains::tick); // the Laws' domains unfurled, at work on the foes in them
		ServerTickEvents.END_SERVER_TICK.register(ChaosFrontier::tick); // the Chaos Frontier's storm (sheltered in those domains), its fiends
		ServerTickEvents.END_SERVER_TICK.register(FirstFlame::tick); // the First Flame's basin laid when someone first comes near
		ServerTickEvents.END_SERVER_TICK.register(SmallWorlds::tick); // the small worlds' land laid out, their doors, those who fall off them
		ServerTickEvents.END_SERVER_TICK.register(FiveDecays::tick); // the Five Decays of Heaven and Man, every immortal's tenth day
		Wishes.register();
		ServerTickEvents.END_SERVER_TICK.register(Spells::tick);
		ServerTickEvents.END_SERVER_TICK.register(GhostGate::keepOfficials);
		ServerTickEvents.END_SERVER_TICK.register(NirvanaCamp::keep); // those waiting on the Shore of Nirvana
		ServerTickEvents.END_SERVER_TICK.register(SacredRelics::tick); // the Sacred Vessels' altars' keepers
		ServerTickEvents.END_SERVER_TICK.register(BloodPools::tick); // the blood pools' elders
		// Whoever starts seeing a player is told whether it is a ghost and how it is drawn (TwoPaths#look: one of the living may wear
		// a Ghost King's form), before they could draw it otherwise; every player, so a client that last saw it in another form
		// (and lost sight of it while it changed) isn't left drawing that one.
		EntityTrackingEvents.START_TRACKING.register((tracked, viewer) -> {
			if (tracked instanceof ServerPlayer subject) ModPackets.sendGhost(viewer, subject);
			SpellAuras.startSeeing(viewer, tracked); // the spells' auras on it, to draw (SpellAuras)
			LawDomains.startSeeing(viewer, tracked); // a Law's domain unfurled round it
		});
		net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
			SpellAuras.resend(player);
			LawDomains.resend(player);
		});
		ServerTickEvents.END_SERVER_TICK.register(SpatialStorms::tick);
		ServerTickEvents.END_SERVER_TICK.register(VoidFallHandler::tick);
		ServerTickEvents.END_SERVER_TICK.register(RealmSuppressionHandler::tick);
		ServerTickEvents.END_SERVER_TICK.register(ConsciousnessDomainHandler::tick);
		ServerTickEvents.END_SERVER_TICK.register(RealmSuppressSystem::tick);
		ServerTickEvents.END_SERVER_TICK.register(Formations::tick);
		ServerTickEvents.END_SERVER_TICK.register(SectManager::tick);
		ServerTickEvents.END_SERVER_TICK.register(SectMembership::tick); // disciples told of their title as it changes
		ServerTickEvents.END_SERVER_TICK.register(NpcSpawner::tick);
		ServerTickEvents.END_SERVER_TICK.register(BeastSpawner::tick); // the wild demon beasts, round players by their lands
		ServerTickEvents.END_SERVER_TICK.register(RogueIndex::tick);
		ServerTickEvents.END_SERVER_TICK.register(NpcTravel::tick);
		ServerTickEvents.END_SERVER_TICK.register(NpcLedger::tick);
		ServerTickEvents.END_SERVER_TICK.register(HermitSites::tick);
		ServerTickEvents.END_SERVER_TICK.register(SectSites::tick);
		ServerTickEvents.END_SERVER_TICK.register(Tempering::tick);
		ServerTickEvents.END_SERVER_TICK.register(InnerRealm::tick);
		ServerTickEvents.END_SERVER_TICK.register(SoulProjection::tick);
		ServerTickEvents.END_SERVER_TICK.register(SoulProjection::mendSouls); // a wounded soul resting in a body mends
		ServerTickEvents.END_SERVER_TICK.register(Possession::settle); // a body's health held to while its whole settles
		ServerTickEvents.END_SERVER_TICK.register(Clones::tick); // back from a crash in a clone; the main body's health told
		ServerTickEvents.END_SERVER_TICK.register(SparSystem::tick);
		ServerTickEvents.END_SERVER_TICK.register(Rumours::tick);
		ServerTickEvents.END_SERVER_TICK.register(NpcLife::tick);
		ServerTickEvents.END_SERVER_TICK.register(HeavenRitual::tick);
		ServerTickEvents.END_SERVER_TICK.register(Physiques::tick);
		ServerTickEvents.END_SERVER_TICK.register(Bloodlines::tick); // a fox spirit's tails shown as its realm grows them
		ServerTickEvents.END_SERVER_TICK.register(PlayerTitles::tick); // the plates over players' heads, their titles and sect titles, as they change
		ServerTickEvents.END_SERVER_TICK.register(Milestones::tick); // the advancements' standing milestones (a realm, a sect's title...) granted
		ServerTickEvents.END_SERVER_TICK.register(RaceGifts::tick); // a tiger's sight, a lizard's in the water, a dragon's might
		ServerTickEvents.END_SERVER_TICK.register(CoreDetonation::tick);
		ServerTickEvents.END_SERVER_TICK.register(StorageRings::flush);
		Physiques.register();
		InnerRealm.registerEvents();
		GhostGate.registerProtection();
		registerSoulInteractions();
		Tempering.register();
		RealmSuppressionHandler.register();
		NetherworldManager.register();

		// Barrier blocks left in a chunk that was unloaded when their formation came down are swept as it loads.
		ServerChunkEvents.CHUNK_LOAD.register(Formations::onChunkLoad);
		// A region's hermit is seated the first time its chunk loads.
		ServerChunkEvents.CHUNK_LOAD.register(HermitSites::onChunkLoad);
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> CultivatorNpc.onLoad(entity));
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> StorageRingItem.onItemLoad(entity));
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> CultivatorNpc.onUnload(entity));

		ServerLifecycleEvents.SERVER_STARTED.register(SpatialRiftBlock::ensureOverworldRift);
		// The spells' blocks a crash left standing taken up, what stood there given back (HeavenSpells#restoreLeftLand).
		ServerLifecycleEvents.SERVER_STARTED.register(HeavenSpells::restoreLeftLand);
		// The Ghost Gate at the Netherworld's 0, 0 (built the first time; its fixtures put back every time).
		ServerLifecycleEvents.SERVER_STARTED.register(GhostGate::ensure);
		// The camp on the Great Void's Shore of Nirvana (built the first time).
		ServerLifecycleEvents.SERVER_STARTED.register(NirvanaCamp::ensure);
		// The Immortal Gate over the void sea and the Southern Heavenly Gate (built the first time; their light as the Gate stands).
		ServerLifecycleEvents.SERVER_STARTED.register(ImmortalGate::ensure);
		ServerLifecycleEvents.SERVER_STOPPING.register(ImmortalGate::settleAll); // those flying to or from it are the camp's again
		// The Gate of the Yellow Springs at the Nether's 0, 0, up the Soul River (placed the first time, back if removed).
		ServerLifecycleEvents.SERVER_STARTED.register(NetherRift::ensure);
		// First: every soul goes home, no penalty, while its player is still in the world to be saved there.
		ServerLifecycleEvents.SERVER_STOPPING.register(SoulProjection::returnAll);
		// And everyone in their clone back in their main body (the clones stand on), before anyone is saved.
		ServerLifecycleEvents.SERVER_STOPPING.register(Clones::returnAll);
		// And everyone carried in a sleeve set down (Sleeves#releaseAll).
		ServerLifecycleEvents.SERVER_STOPPING.register(Sleeves::releaseAll);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> TribulationManager.clear());
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> LifespanTribulation.clear()); // their power is on the records already
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> FiveDecays.clear()); // a judgement under way comes again (the records keep it)
		// Spars end without a result, stakes back to the players still here, before anyone is saved.
		ServerLifecycleEvents.SERVER_STOPPING.register(SparSystem::clear);
		// Before players and chunks are saved, so nobody is saved with the pressure's lowered health.
		ServerLifecycleEvents.SERVER_STOPPING.register(RealmSuppressSystem::releaseAll);
		// Cultivators still loaded go into the ledger now: their land unloads only after the save.
		ServerLifecycleEvents.SERVER_STOPPING.register(NpcLedger::enterLoaded);
		// So does where each sect member stands, to wake it if it's let go while unloaded (SectManager#rouse).
		ServerLifecycleEvents.SERVER_STOPPING.register(CultivatorNpc::noteWhereMembersStand);
		// Before the save: every block a heaven spell set down is taken up again, so none is saved into the world.
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> HeavenSpells.clear());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> SpatialStorms.clear());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			Formations.clear();
			CultivatorNpc.clearLoaded();
			SoulProjection.clear();
			Possession.clear();
			Clones.clear();
			Rumours.clearPending();
			HermitSites.clearPending();
			SectSites.clear();
			InnerRealm.clear();
			NpcLedger.clear();
			NpcLife.clear();
			HeavenRitual.clear();
			NetherworldManager.clear();
			GhostSystem.clear();
			PalaceTreasury.clear();
			Spells.clear();
			CoreDetonation.clear();
			SectMembership.clear();
			TalkSystem.clearTrials();
			SpatialTrialHandler.clear();
			VoidBreach.clear();
			ImmortalGate.clear();
			VoidCurrents.clear();
			NirvanaCamp.clear();
			Sleeves.clear();
			Wishes.clear();
			RaceGifts.clear();
			LawDomains.clear();
			ChaosFrontier.clear();
			SmallWorlds.clear();
			PlayerTitles.clear();
		});

		// A formation's shell runs through the ground too: those who couldn't break the barrier can't dig under it either.
		// Breaking anything on a sect's grounds is trespass (theft, in the treasury), and its disciples answer it.
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer sp)) return true;
			if (Incorporeal.is(sp)) return false; // the dead and wandering souls break nothing
			if (!(state.getBlock() instanceof SectBarrierBlock) && Formations.protectsGround(sp, pos)) return false;
			// A spell's block (a wall, a tree, a field's ice) is taken up, dropping nothing: spells are no quarry.
			if (HeavenSpells.breakTemporary(level, pos)) return false;
			// One's own sect's grounds (SectMembership): no trespass; its treasury the elders' alone.
			Sect own = ownSect(level, sp, pos);
			if (own != null) {
				if (!own.treasury.isInside(pos)) return true;
				SectMembership.TreasuryTouch touch = SectMembership.treasury(sp, own);
				if (touch == SectMembership.TreasuryTouch.ALLOWED) return true;
				if (touch == SectMembership.TreasuryTouch.REFUSED) return false;
			}
			SectManager.onTrespass(level, sp, pos);
			// The treasury's seal holds while its elders keep it: nothing in it breaks (a chest broken would spill all it holds).
			Sect sealed = sp.isCreative() ? null : SectManager.sealedTreasuryAt(level, pos);
			if (sealed != null) {
				sealedMessage(sp, level, sealed);
				return false;
			}
			// A ghost palace's treasury the same, under its king's seal (PalaceTreasury).
			return !PalaceTreasury.isTreasury(level, pos) || PalaceTreasury.touch(level, sp, pos);
		});
		// Opening the treasury's chests, lifting what its pedestals hold, or harvesting the sect's herbs is theft.
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (world.isClientSide || !(world instanceof ServerLevel level) || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
			BlockPos pos = hit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			boolean valuable = level.getBlockEntity(pos) instanceof Container || state.getBlock() instanceof SpiritPedestalBlock
					|| state.getBlock() instanceof GinsengBlock || state.getBlock() instanceof CultivationFruitBlock;
			if (!valuable) return InteractionResult.PASS;
			// A ghost palace's treasury: theft, and shut while its king's seal holds (PalaceTreasury).
			if (PalaceTreasury.isTreasury(level, pos)) {
				if (hand != InteractionHand.MAIN_HAND) return PalaceTreasury.sealed(level, pos) ? InteractionResult.FAIL : InteractionResult.PASS;
				return PalaceTreasury.touch(level, sp, pos) ? InteractionResult.PASS : InteractionResult.FAIL;
			}
			// One's own sect's (SectMembership): its things are one's own to use, but its treasury is the elders' alone (a warning,
			// then cast out, then theft as anyone's).
			Sect own = ownSect(level, sp, pos);
			if (own != null) {
				if (!own.treasury.isInside(pos)) return InteractionResult.PASS;
				if (hand != InteractionHand.MAIN_HAND) return SectMembership.isElder(own, sp) ? InteractionResult.PASS : InteractionResult.FAIL;
				SectMembership.TreasuryTouch touch = SectMembership.treasury(sp, own);
				if (touch == SectMembership.TreasuryTouch.ALLOWED) return InteractionResult.PASS;
				if (touch == SectMembership.TreasuryTouch.REFUSED) return InteractionResult.FAIL;
			}
			// The attempt still raises the alarm (once a click), but while the treasury's elders keep it, its chests and pedestals
			// stay shut, from either hand; one a realm above them breaks the seal with this touch, and opens it with the next.
			Sect sealed = sp.isCreative() ? null : SectManager.sealedTreasuryAt(level, pos);
			if (hand != InteractionHand.MAIN_HAND) return sealed != null ? InteractionResult.FAIL : InteractionResult.PASS;
			SectManager.onTrespass(level, sp, pos);
			if (sealed != null) {
				if (SectManager.breakTreasurySeal(level, sealed, sp, pos)) sp.displayClientMessage(Component.translatable(ModLang.MSG_TREASURY_SEAL_BROKEN), true);
				else sealedMessage(sp, level, sealed);
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			// A tribulation walked out of (logged out, quit, the game closed) fails now, before the stats are applied.
			boolean fled = TribulationManager.onJoin(player);
			// Applies stats from the effective (possibly suppressed) stage and syncs the client.
			// Back from a crash with the soul still inward: return it to the body first.
			if (ModDimensions.isInnerRealm(player.level().dimension())) InnerRealm.leave(player);
			// Likewise a possession left on record (Possession: each one's things back where they belong), then a nascent soul still
			// out of its body: home, no penalty.
			Possession.onJoin(player);
			SoulProjection.onJoin(player);
			Clones.onJoin(player); // back from a crash in their clone: in their main body again, a tick on
			LifespanTribulation.onJoin(player); // a spent life ended by a death they never came back from: born anew now
			Physiques.onJoin(player); // the body they were born with, the first time
			Bloodlines.onJoin(player); // everyone's bloodline told; theirs chosen, the first time
			DemonPath.born(player); // a demon's demon core, from birth (and its blood on its record)
			CoreDetonation.onJoin(player); // no act's switch left on from a server that stopped mid-way
			FiveDecays.onJoin(player); // the Decays' time stood still while they were away
			SmallWorlds.onJoin(player); // their own small world's door, and out of one not theirs to be in
			Sleeves.onJoin(player); // back in a sleeve, or let out of one while away: set right a tick on
			PlayerTitles.onJoin(player); // every player's plate told (their title, their sect's title over them)
			RealmSuppressionHandler.update(player);
			if (fled) player.setHealth(Math.min(player.getHealth(), player.getMaxHealth())); // the lower stage's smaller body
			Formations.syncSoon(); // the barriers near them, at once
			GhostSystem.onJoin(player); // a ghost logs back in a ghost
			Spells.sync(player); // ghost methods, spells, the spell wheel
			NetherworldManager.refresh(player); // ghostly qi, yin corruption
			ModRecipeBook.unlock(player); // every recipe of the mod in the recipe book (its own tab)
		});

		// Leaving (logging out, kicked, timed out): Fabric tells of it on the network thread when the connection closes before the
		// server thread notices, and the server thread goes on to save the player meanwhile (a clone's player saved in the clone's
		// place, found 2026-10-23). The work is done on the server thread instead, by whichever comes first, this or the head of
		// vanilla's onDisconnect (ServerGamePacketListenerImplMixin), before they are saved (#leaving).
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (server.isSameThread()) leaving(handler.getPlayer());
			else server.execute(() -> leaving(handler.getPlayer()));
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
			// A death's respawn wakes a ghost at the Ghost Gate; the Wheel's wakes the living (GhostSystem).
			GhostSystem.onRespawn(newPlayer, alive);
			NetherworldManager.refresh(newPlayer);
			// Born again after a spent life's death (LifespanTribulation): the record made anew, a mortal of 16.
			if (!alive) LifespanTribulation.onRespawn(newPlayer);
			DemonPath.born(newPlayer); // a demon born again is born with its demon core again
			FiveDecays.onRespawn(newPlayer); // a Decay's stained robes on the new body too
			Sleeves.onRespawn(newPlayer); // one who died in a sleeve (a command reaches them) plays as before
		});

		// A blow a heaven spell's ward has brought to nothing is refused outright: no knockback, no flinch (HeavenSpells#wardedOff).
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !HeavenSpells.wardedOff(entity));
		// A spar just ended: a blow between its two in the moment after (the next swing) is pulled, not taken for an attack; and in a
		// spar or just after, the player's stray blows on the partner's fellows are pulled too (SparSystem#allowDamage).
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> SparSystem.allowDamage(entity, source));
		// Ghost Fire on a cultivator's hands (Spells): a blow of theirs sets what it strikes burning; a Jade Poison Body's poisons it
		// (Physiques).
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (source.getEntity() instanceof ServerPlayer attacker && source.getDirectEntity() == attacker
					&& source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK) && entity != attacker) {
				Spells.onMeleeHit(attacker, entity);
				Physiques.onMeleeHit(attacker, entity);
			}
			return true;
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer sp)) return true;
			// Just after a Soul Dash, or on Yin Wind Steps, a fall does no harm (Spells#fallIsSafe).
			if (source.is(DamageTypeTags.IS_FALL) && Spells.fallIsSafe(sp)) return false;
			// A projected soul is harmed only by cultivators who see it, by their own hand (see SoulProjection#allowSoulDamage).
			if (SoulProjection.isSoul(sp)) return SoulProjection.allowSoulDamage(sp, source);
			// A ghost, only by what reaches the dead (see GhostSystem#allowDamage). One turned inward (made a ghost there: the yin) fights
			// its heart demon as the living do, a defeat sending it back to its body (it scattered there, before the sweep of 2026-10-23,
			// and reformed at the Gate with its body left sitting and the Inner Realm still its).
			if (GhostSystem.isGhost(sp)) {
				if (!GhostSystem.allowDamage(sp, source)) return false;
				return !InnerRealm.isInside(sp) || QiSurges.allowInnerDamage(sp, source, amount);
			}
			// The drop back into the Overworld sky after descension doesn't kill on landing.
			if (source.is(DamageTypeTags.IS_FALL) && SpatialTrialHandler.consumeFallProtection(sp)) {
				return false;
			}
			// A soul in the Inner Realm fights on (its heart demon): only a defeat there sends it back.
			if (InnerRealm.isInside(sp)) return QiSurges.allowInnerDamage(sp, source, amount);
			// Any damage breaks concentration: but the lifespan tribulation's strikes, meditated through (LifespanTribulation).
			if (amount > 0 && MeditationManager.isMeditating(sp.getUUID()) && !LifespanTribulation.isStriking(sp)) {
				MeditationManager.stop(sp, true);
			}
			// And any talk (being struck is no time for words); Sworn friends nearby come to help (TalkSystem).
			if (amount > 0 && !sp.isInvulnerableTo(source)) TalkSystem.onPlayerHurt(sp, source);
			return true;
		});

		// Before a death goes ahead: a soul dies where its body sat and loses the nascent soul; a possessed body's death is its
		// possessor's doing.
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			// Under the lifespan tribulation, first: its own killing strike is held off by nothing, and one of the dead struck down by
			// anything scatters for good (LifespanTribulation#allowDeath).
			if (entity instanceof ServerPlayer spent) {
				Boolean lifespan = LifespanTribulation.allowDeath(spent, source);
				if (lifespan != null) return lifespan;
			}
			// Struck down while the Five Decays' judgement runs: a stage lower, alive (FiveDecays, the user's pick).
			if (entity instanceof ServerPlayer judged && !FiveDecays.allowDeath(judged)) return false;
			// A tribulation's killing strike on a Heaven-Defying Body: one heart left, and the tribulation failed (TribulationManager).
			if (entity instanceof ServerPlayer defier && !TribulationManager.allowDeath(defier)) return false;
			// A ghost doesn't die: it disperses and reforms at the Gate (GhostSystem). No death screen, none of death's bookkeeping.
			// A ghost's soul struck down out of its form is back in it first, its nascent soul lost, as a living soul's is.
			if (entity instanceof ServerPlayer ghost && GhostSystem.isGhost(ghost)) SoulProjection.onSoulDying(ghost);
			if (entity instanceof ServerPlayer ghost && !GhostSystem.allowDeath(ghost)) return false;
			// A heaven spell that holds off a death (HeavenSpells#allowDeath: a phoenix's rebirth, say).
			if (!HeavenSpells.allowDeath(entity, source)) return false;
			// Nobody dies in a spar by the other's hand: the blow stops at the line, and the spar ends (SparSystem).
			if (!SparSystem.allowDeath(entity, source)) return false;
			// A clone slain doesn't kill its player: it dissolves, and they wake in their main body (Clones).
			if (entity instanceof ServerPlayer cloned && !Clones.allowDeath(cloned, source)) return false;
			// A body held by a soul (Possession) dies as its owner's, by this blow, and the soul is thrown out: never the soul's own
			// death. A totem in its hands holds death off as it would for anyone (vanilla's, next), but not from what nothing stops.
			if (entity instanceof ServerPlayer sp && Possession.inhabits(sp.getUUID())) {
				boolean totem = sp.getMainHandItem().is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING)
						|| sp.getOffhandItem().is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING);
				if (totem && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
				SoulProjection.onBodyDying(sp, source);
				return false;
			}
			if (entity instanceof ServerPlayer sp) SoulProjection.onSoulDying(sp);
			else SoulProjection.onHostDying(entity, source);
			return true;
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			Laws.onDeath(entity, source); // a mighty foe slain by an element's spell: a glimpse of its Law, perhaps
			if (entity instanceof ServerPlayer sp) {
				FiveDecays.onDeath(sp); // a death nothing held off fails the Decays' judgement all the same
				// A demon slain by another player leaves its demon core (DemonCoreItem), read from its record before anything changes it.
				DemonCoreItem.onPlayerDeath(sp, source);
				// A death under the lifespan tribulation, of anything, ends the life whole (made anew at the respawn): first, so a
				// breakthrough's tribulation under way goes with it unremarked.
				boolean spent = LifespanTribulation.onDeath(sp);
				TribulationManager.onDeath(sp); // dying mid-tribulation fails the breakthrough
				Sleeves.onDeath(sp); // those in a sleeve cast out where its bearer fell: before the gap's or a climb's own reckoning
				SpatialTrialHandler.onDeath(sp); // dying in the Spatial Gap ends the trial
				VoidBreach.onDeath(sp); // and in a Void Breach, the climb
				ImmortalGate.onDeath(sp); // and in the Immortal Gate's passage, the climb
				SoulRiver.onDeath(sp); // and on the Soul River, the journey
				// Bound for the Netherworld (the gamerule on): the respawn will be the Ghost Gate's. Not a spent life's (LifespanTribulation):
				// it is born again at its own spawn point, a living mortal (the user's ask, 2026-10-20).
				if (!spent && !source.is(ModDamageTypes.OLD_AGE)) GhostSystem.onDeath(sp);
				SoulProjection.onDeath(sp); // nothing of a soul's journey outlives a death
				Clones.onDeath(sp); // nor a clone: it dissolves where it stands
				SparSystem.abandon(sp); // killed mid-spar by something else: the spar is forfeit
				RealmSuppressSystem.refreshNow(); // a fallen suppressor's pressure lifts at once
			}
		});
	}

	/**
	 * A projected soul touches nothing: no blocks, no items, nothing it can use or hit. Its one act is to enter a being (right
	 * click, see SoulProjection#tryPossess), and while it is in one, its blows are that body's. A ghost (see GhostSystem) touches
	 * nothing of the living world either, but uses the dead's own blocks (tagged ghost_usable: the Wheel and the like) and fights
	 * with its own hands. Registered for both sides: the client's answer decides whether a packet is sent at all.
	 */
	private static void registerSoulInteractions() {
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
				Incorporeal.is(player) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (SoulProjection.isSoul(player)) return InteractionResult.FAIL;
			// One whose body is held only watches it (a spectator could otherwise look into a chest it looks at).
			if (player instanceof ServerPlayer held && (Possession.isHeld(held.getUUID()) || Sleeves.isCarried(held.getUUID()))) return InteractionResult.FAIL;
			if (!Incorporeal.isRuledGhost(player)) return InteractionResult.PASS;
			if (hand != InteractionHand.MAIN_HAND) return InteractionResult.FAIL;
			BlockState state = world.getBlockState(hit.getBlockPos());
			if (state.is(ModTags.GHOST_USABLE)) return InteractionResult.PASS;
			// A ghost palace's treasury is the dead's own (PalaceTreasury): the client asks for any chest in the Netherworld, the
			// server decides.
			boolean treasury = state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock && (world instanceof ServerLevel level
					? PalaceTreasury.isTreasury(level, hit.getBlockPos()) : ModDimensions.isNetherworld(world.dimension()));
			return treasury ? InteractionResult.PASS : InteractionResult.FAIL;
		});
		// Nothing of the living world in the hand of a soul or a ghost; but a ghost may drink a Marrow Cleansing Elixir, its meridians
		// opened in death as in life (a mortal's ghost has no ghost path without it: GhostMethods#refusal), and read a slip.
		UseItemCallback.EVENT.register((player, world, hand) -> Incorporeal.is(player) && !Incorporeal.mayUse(player, player.getItemInHand(hand))
				? InteractionResultHolder.fail(player.getItemInHand(hand)) : InteractionResultHolder.pass(player.getItemInHand(hand)));
		UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			// Nothing to ride or use; only cultivators to talk with, the living and the dead (TalkSystem keeps the talk to what the
			// dead may do).
			if (Incorporeal.isRuledGhost(player)) return entity instanceof CultivatorNpc ? InteractionResult.PASS : InteractionResult.FAIL;
			if (!SoulProjection.isSoul(player)) return InteractionResult.PASS;
			if (hand != InteractionHand.MAIN_HAND || player.isPassenger()) return InteractionResult.FAIL;
			if (world.isClientSide) return InteractionResult.SUCCESS; // the server decides
			return player instanceof ServerPlayer sp ? SoulProjection.tryPossess(sp, entity) : InteractionResult.FAIL;
		});
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (Incorporeal.isRuledGhost(player)) {
				// Another ghost, or the dead's own kind, is struck as anyone would be; anything else is the ghost's own business
				// (GhostSystem#attack).
				if (entity instanceof Player target && GhostSystem.isGhost(target) || entity instanceof Ghostly) return InteractionResult.PASS;
				if (world.isClientSide) return InteractionResult.SUCCESS; // sent on to the server
				return player instanceof ServerPlayer sp && GhostSystem.attack(sp, entity) ? InteractionResult.PASS : InteractionResult.FAIL;
			}
			if (!SoulProjection.isSoul(player)) return InteractionResult.PASS;
			if (!player.isPassenger()) return InteractionResult.FAIL;
			if (world.isClientSide) return InteractionResult.SUCCESS; // sent on to the server, which strikes with the host
			if (player instanceof ServerPlayer sp) SoulProjection.hostAttack(sp, entity);
			return InteractionResult.FAIL;
		});
	}

	/**
	 * The sect whose grounds {@code pos} lies in, if {@code player} is one of its disciples (SectMembership), or is in the body of one
	 * of its own (Possession); else null.
	 */
	private static Sect ownSect(ServerLevel level, ServerPlayer player, BlockPos pos) {
		Sect sect = SectManager.sectAt(level, pos);
		if (sect == null) return null;
		if (Possession.inhabits(player.getUUID())) return sect.id.equals(Possession.sectOfBody(player.getUUID())) ? sect : null;
		return sect.disciples.containsKey(player.getUUID()) ? sect : null;
	}

	/** "A seal of <realm> holds it shut": what a sealed treasury tells one who can't break its seal (SectManager#treasurySeal). */
	private static void sealedMessage(ServerPlayer player, ServerLevel level, Sect sect) {
		Realm seal = SectManager.treasurySeal(level, sect);
		if (seal != null) player.displayClientMessage(Component.translatable(ModLang.MSG_TREASURY_SEALED, seal.getDisplayName()), true);
	}

	private CultivationEvents() {}
}
