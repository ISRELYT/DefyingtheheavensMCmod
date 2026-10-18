package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * True possession (Possession, the user's rules 2026-10-18): a nascent soul takes a player's or an NPC cultivator's body and plays
 * it as its own, the player watching and the NPC set aside, and gives it back. Driven by hand on survival test players in the
 * player list (as SoulProjectionGameTests' are), within one tick each.
 */
public class PossessionGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void aPossessedPlayerLivesAndLendsTheirBody(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		MinecraftServer server = helper.getLevel().getServer();
		boolean pvp = server.isPvpAllowed();
		server.setPvpAllowed(true);
		try {
			ServerPlayer soul = projected(helper, made, "lend-soul");
			soul.getInventory().setItem(3, new ItemStack(Items.GOLD_INGOT, 2)); // the soul's own things (it carries them, touching nothing)
			ServerPlayer victim = victim(helper, made, "lend-victim", soul);
			victim.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 5));
			victim.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
			victim.setHealth(13);
			victim.setExperienceLevels(5);
			victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600));
			double x = victim.getX(), z = victim.getZ();
			PlayerCultivation theirs = CultivationManager.own(victim);

			helper.assertTrue(SoulProjection.tryPossess(soul, victim) == InteractionResult.SUCCESS, "The soul takes the player's body");
			helper.assertTrue(victim.isAlive() && victim.gameMode.getGameModeForPlayer() == GameType.SPECTATOR && victim.getCamera() == soul
					&& victim.getInventory().isEmpty(), "They live, watching through their body's eyes, holding nothing");
			helper.assertTrue(soul.getInventory().getItem(9).is(Items.DIAMOND) && soul.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET)
					&& soul.getHealth() == 13 && soul.experienceLevel == 5 && soul.hasEffect(MobEffects.MOVEMENT_SPEED),
					"The soul has what the body carries, its health, experience and effects (health " + soul.getHealth() + ")");
			helper.assertTrue(!soul.getInventory().hasAnyMatching(s -> s.is(Items.GOLD_INGOT)), "...and its own things are put by");
			helper.assertTrue(Math.abs(soul.getX() - x) < 0.01 && Math.abs(soul.getZ() - z) < 0.01, "It stands where the body stood");
			helper.assertTrue(SoulProjection.state(soul) == SoulProjection.State.INHABITING && !SoulProjection.isSoul(soul) && !Incorporeal.is(soul),
					"It is no soul to the world now: it touches and takes and breaks as anyone");
			helper.assertTrue(CultivationManager.get(soul) == theirs && "lend-victim".equals(Possession.displayName(soul).getString()),
					"Its cultivation and its name are the body's");

			// It uses the body as its own: puts the diamonds away, takes up an emerald, takes a blow.
			soul.getInventory().setItem(9, ItemStack.EMPTY);
			soul.getInventory().setItem(10, new ItemStack(Items.EMERALD));
			soul.setHealth(9);
			SoulProjection.toggle(soul); // Soul Projection: leave the body
			helper.assertTrue(victim.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && victim.getCamera() == victim,
					"Let go of, the body is its owner's again");
			helper.assertTrue(victim.getInventory().getItem(10).is(Items.EMERALD) && !victim.getInventory().hasAnyMatching(s -> s.is(Items.DIAMOND))
					&& victim.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET) && victim.getHealth() == 9 && victim.experienceLevel == 5,
					"...with whatever was done with it (health " + victim.getHealth() + ")");
			helper.assertTrue(SoulProjection.isSoul(soul) && soul.getInventory().getItem(3).is(Items.GOLD_INGOT) && soul.getInventory().countItem(Items.EMERALD) == 0
					&& CultivationManager.get(soul) == CultivationManager.own(soul), "The soul is out again, with its own things");
			helper.assertTrue(theirs.getPossession() == null && CultivationManager.own(soul).getPossession() == null, "Nothing left on record");
			helper.succeed();
		} finally {
			server.setPvpAllowed(pvp);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void inABodyTheSoulUsesItsCultivation(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		MinecraftServer server = helper.getLevel().getServer();
		boolean pvp = server.isPvpAllowed();
		server.setPvpAllowed(true);
		try {
			ServerPlayer soul = projected(helper, made, "cultivate-soul");
			ServerPlayer victim = victim(helper, made, "cultivate-victim", soul);
			PlayerCultivation theirs = CultivationManager.own(victim);
			theirs.setState(Realm.QI_REFINING, Stage.MID, 0);
			theirs.setQi(theirs.maxQi() * 0.5);
			for (int i = 0; i < PlayerCultivation.WHEEL_SLOTS; i++) theirs.setWheelSlot(i, null); // a wheel of their own making: empty
			CultivationStats.apply(victim, theirs);
			float bodyMax = victim.getMaxHealth();
			helper.assertTrue(!Ability.SOUL_PROJECTION.isUnlocked(theirs), "A Qi Refiner has no soul to send out");

			helper.assertTrue(SoulProjection.tryPossess(soul, victim) == InteractionResult.SUCCESS, "Possessed");
			helper.assertTrue(CultivationManager.get(soul) == theirs && soul.getMaxHealth() == bodyMax,
					"Its record answers for the soul, and its strength is the soul's (" + soul.getMaxHealth() + " of " + bodyMax + ")");
			boolean onWheel = false;
			for (int i = 0; i < PlayerCultivation.WHEEL_SLOTS; i++) onWheel |= theirs.getWheelSlot(i) == Ability.SOUL_PROJECTION;
			helper.assertTrue(Ability.SOUL_PROJECTION.isUnlocked(theirs) && onWheel, "Soul Projection is open on it, on its wheel, to leave by");
			helper.assertTrue(!SoulProjection.isAwayFromBody(soul.getUUID()) && SoulProjection.isActive(soul.getUUID()),
					"It may meditate as the body (cultivating it), but nothing that wants it home (a tribulation)");

			// Its upkeep is its own pool's: the body's qi is the body's.
			PlayerCultivation own = CultivationManager.own(soul);
			double bodyQi = theirs.getQi(), ownQi = own.getQi();
			double cost = own.soulCostPerSecond(soul.position().distanceTo(SoulProjection.bodyPos(soul)) / own.soulTetherRadius(), true);
			double expected = Math.min(own.maxQi(), ownQi + (own.qiGatherPerSecond() - cost) / 20.0);
			helper.assertTrue(SoulProjection.upkeep(soul, CultivationManager.get(soul)) == 0 && theirs.getQi() == bodyQi
					&& Math.abs(own.getQi() - expected) < 1.0e-6, "The soul pays out of its own pool (" + own.getQi() + ", " + expected + ")");

			SoulProjection.toggle(soul);
			boolean still = false;
			for (int i = 0; i < PlayerCultivation.WHEEL_SLOTS; i++) still |= theirs.getWheelSlot(i) == Ability.SOUL_PROJECTION;
			helper.assertTrue(!still && !Ability.SOUL_PROJECTION.isUnlocked(theirs) && CultivationManager.get(victim) == theirs,
					"Given back, it is theirs alone again");
			helper.succeed();
		} finally {
			server.setPvpAllowed(pvp);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBodyThatDiesDiesAsItsOwner(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		MinecraftServer server = helper.getLevel().getServer();
		boolean pvp = server.isPvpAllowed();
		GameRules.BooleanValue keep = helper.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);
		boolean kept = keep.get();
		server.setPvpAllowed(true);
		keep.set(false, server);
		try {
			ServerPlayer soul = projected(helper, made, "die-soul");
			ServerPlayer victim = victim(helper, made, "die-victim", soul);
			victim.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 7));
			helper.assertTrue(SoulProjection.tryPossess(soul, victim) == InteractionResult.SUCCESS, "Possessed");
			soul.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
			helper.assertTrue(soul.isAlive() && SoulProjection.isSoul(soul) && soul.getHealth() > 0, "The soul is thrown out, unharmed");
			helper.assertTrue(victim.isDeadOrDying() && victim.gameMode.getGameModeForPlayer() == GameType.SURVIVAL,
					"The body died as its owner's own death");
			List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(victim.blockPosition()).inflate(3),
					e -> e.getItem().is(Items.DIAMOND));
			helper.assertTrue(!dropped.isEmpty(), "...and what it carried fell where it died");
			for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(victim.blockPosition()).inflate(4))) item.discard();
			for (net.minecraft.world.entity.ExperienceOrb orb : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,
					new AABB(victim.blockPosition()).inflate(4))) orb.discard();
			helper.succeed();
		} finally {
			server.setPvpAllowed(pvp);
			keep.set(kept, server);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void loggingOutDoesNotFreeTheBody(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		MinecraftServer server = helper.getLevel().getServer();
		boolean pvp = server.isPvpAllowed();
		server.setPvpAllowed(true);
		try {
			ServerPlayer soul = projected(helper, made, "logout-soul");
			ServerPlayer victim = victim(helper, made, "logout-victim", soul);
			victim.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 2));
			helper.assertTrue(SoulProjection.tryPossess(soul, victim) == InteractionResult.SUCCESS, "Possessed");
			Possession.onDisconnect(victim); // the owner logs out (the user's rule, 2026-10-18: no escaping so)
			helper.assertTrue(Possession.inhabits(soul.getUUID()) && Possession.isHeld(victim.getUUID()) && soul.getInventory().getItem(9).is(Items.DIAMOND)
					&& "logout-victim".equals(soul.getDisplayName().getString()), "Leaving the world doesn't free their body: the soul plays on in it");
			Possession.onJoin(victim); // back while it is still held
			SoulProjection.tick(soul, helper.getLevel().getGameTime()); // their view put back on the soul's tick, once they're in the world
			helper.assertTrue(victim.gameMode.getGameModeForPlayer() == GameType.SPECTATOR && victim.getCamera() == soul && Possession.playerBody(soul) == victim,
					"Back, they watch through it again");
			// Away again, and the soul lets go of the body meanwhile, a little further on.
			Possession.onDisconnect(victim);
			soul.moveTo(soul.getX() + 2, soul.getY(), soul.getZ(), 0, 0);
			double x = soul.getX();
			SoulProjection.toggle(soul);
			CompoundTag waiting = CultivationManager.own(victim).getPossession();
			helper.assertTrue(!Possession.isHeld(victim.getUUID()) && waiting != null && waiting.contains("Returned") && waiting.contains("At"),
					"Given back while they're away, it waits in their record, with where it was left");
			Possession.onJoin(victim);
			helper.assertTrue(CultivationManager.own(victim).getPossession() != null, "Not yet, as they join: they aren't in the world yet");
			Possession.settle(server); // the next tick
			helper.assertTrue(victim.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && victim.getInventory().getItem(9).is(Items.DIAMOND)
					&& Math.abs(victim.getX() - x) < 0.01 && CultivationManager.own(victim).getPossession() == null,
					"Coming back, they have it whole, where it was left (" + victim.getX() + ", " + x + ")");
			// The soul's own player logging out does give it back (it goes home, out of the body first).
			helper.assertTrue(SoulProjection.tryPossess(soul, victim) == InteractionResult.SUCCESS, "Possessed again");
			SoulProjection.onDisconnect(soul);
			helper.assertTrue(victim.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && victim.getInventory().getItem(9).is(Items.DIAMOND)
					&& !SoulProjection.isActive(soul.getUUID()) && !Possession.inhabits(soul.getUUID()), "The soul going home gives the body back first");
			helper.succeed();
		} finally {
			server.setPvpAllowed(pvp);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBodyThatDiesWhileItsOwnerIsAwayKillsThemOnReturn(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		MinecraftServer server = helper.getLevel().getServer();
		boolean pvp = server.isPvpAllowed();
		GameRules.BooleanValue keep = helper.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);
		boolean kept = keep.get();
		server.setPvpAllowed(true);
		keep.set(false, server);
		try {
			ServerPlayer soul = projected(helper, made, "away-soul");
			ServerPlayer victim = victim(helper, made, "away-victim", soul);
			victim.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 3));
			helper.assertTrue(SoulProjection.tryPossess(soul, victim) == InteractionResult.SUCCESS, "Possessed");
			Possession.onDisconnect(victim);
			soul.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
			CompoundTag waiting = CultivationManager.own(victim).getPossession();
			helper.assertTrue(soul.isAlive() && SoulProjection.isSoul(soul) && waiting != null && waiting.getBoolean("Died"),
					"The body dies with its owner away: the soul thrown out, their death waiting for them");
			List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, soul.getBoundingBox().inflate(4), e -> e.getItem().is(Items.DIAMOND));
			helper.assertTrue(!dropped.isEmpty(), "...what it carried fallen where it died");
			for (ItemEntity item : dropped) item.discard();
			Possession.onJoin(victim);
			Possession.settle(server); // the next tick, once they're in the world
			helper.assertTrue(victim.isDeadOrDying(), "Coming back, they die");
			helper.succeed();
		} finally {
			server.setPvpAllowed(pvp);
			keep.set(kept, server);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBodyCarriesItsSoulIntoAnotherRealm(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		MinecraftServer server = helper.getLevel().getServer();
		boolean pvp = server.isPvpAllowed();
		server.setPvpAllowed(true);
		try {
			ServerPlayer soul = projected(helper, made, "realm-soul");
			ServerPlayer victim = victim(helper, made, "realm-victim", soul);
			helper.assertTrue(SoulProjection.tryPossess(soul, victim) == InteractionResult.SUCCESS, "Possessed");
			net.minecraft.server.level.ServerLevel nether = server.getLevel(net.minecraft.world.level.Level.NETHER);
			soul.teleportTo(nether, 0.5, 100, 0.5, 0, 0); // through a portal, say
			PlayerCultivation own = CultivationManager.own(soul);
			double before = own.getQi();
			double expected = Math.min(own.maxQi(), before + (own.qiGatherPerSecond() - own.soulCostPerSecond(SoulProjection.CROSS_REALM_REACH, true)) / 20.0);
			SoulProjection.tick(soul, helper.getLevel().getGameTime());
			helper.assertTrue(SoulProjection.state(soul) == SoulProjection.State.INHABITING && victim.level() == nether && victim.getCamera() == soul,
					"The soul goes with its body into the Nether, its owner's view with it (" + SoulProjection.state(soul) + ", soul in "
							+ soul.level().dimension().location() + ", victim in " + victim.level().dimension().location() + ", camera " + victim.getCamera() + ")");
			helper.assertTrue(SoulProjection.upkeep(soul, CultivationManager.get(soul)) == 0 && Math.abs(own.getQi() - expected) < 1.0e-6,
					"...its tether reaching across the realms, at what it costs beside its body (" + own.getQi() + ", " + expected + ")");
			SoulProjection.toggle(soul); // out of the body there
			helper.assertTrue(victim.level() == nether && victim.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "The body is given back where it is");
			SoulProjection.tick(soul, helper.getLevel().getGameTime());
			helper.assertTrue(!SoulProjection.isActive(soul.getUUID()) && soul.level() == helper.getLevel(),
					"A soul can't stay in another realm by itself: it goes home");
			helper.succeed();
		} finally {
			server.setPvpAllowed(pvp);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anNpcsBodyCarriesItsSoulIntoUnloadedLand(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		CultivatorNpc npc = null;
		try {
			ServerPlayer soul = projected(helper, made, "carry-soul");
			npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(3, 2, 2));
			npc.setRank(PlayerCultivation.rank(Realm.QI_REFINING, Stage.GRAND_PERFECTION));
			helper.assertTrue(SoulProjection.tryPossess(soul, npc) == InteractionResult.SUCCESS, "Possessed");
			net.minecraft.server.level.ServerLevel nether = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
			soul.teleportTo(nether, 40000.5, 100, 40000.5, 0, 0); // through a tear, say, to land nobody has loaded
			SoulProjection.tick(soul, helper.getLevel().getGameTime());
			net.minecraft.world.entity.LivingEntity body = Possession.bodyEntity(soul);
			helper.assertTrue(SoulProjection.state(soul) == SoulProjection.State.INHABITING && body instanceof CultivatorNpc && body.level() == nether,
					"The NPC's body goes along, its land not loaded yet (" + SoulProjection.state(soul) + ", " + body + ")");
			npc = (CultivatorNpc) body;
			SoulProjection.tick(soul, helper.getLevel().getGameTime());
			helper.assertTrue(SoulProjection.state(soul) == SoulProjection.State.INHABITING, "...and stays with it");
			SoulProjection.toggle(soul);
			helper.assertTrue(!npc.isVacant() && npc.level() == nether, "Given back there");
			helper.succeed();
		} finally {
			if (npc != null) npc.discard();
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void inABodyThatCantTearSpaceTheSoulTearsWithItsOwn(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		MinecraftServer server = helper.getLevel().getServer();
		boolean pvp = server.isPvpAllowed();
		server.setPvpAllowed(true);
		try {
			ServerPlayer soul = projected(helper, made, "tear-soul", Realm.HEAVENLY_BEING);
			ServerPlayer victim = victim(helper, made, "tear-victim", soul);
			PlayerCultivation theirs = CultivationManager.own(victim);
			helper.assertTrue(!Ability.SPATIAL_TEAR.isUnlocked(theirs), "A mortal can't tear space");
			helper.assertTrue(SoulProjection.tryPossess(soul, victim) == InteractionResult.SUCCESS, "A Heavenly Being's soul takes the body");
			helper.assertTrue(Ability.SOUL_PROJECTION.isUnlocked(theirs) && !Ability.CORE_DETONATION.isUnlocked(theirs),
					"A mortal's body has the way out of it on its wheel, though no core to detonate");
			PlayerCultivation own = CultivationManager.own(soul);
			helper.assertTrue(Ability.SPATIAL_TEAR.isUnlocked(theirs) && Possession.soulTears(theirs), "...and may tear space in it, with its own tear ("
					+ Ability.SPATIAL_TEAR.isUnlocked(theirs) + ", " + Possession.soulTears(theirs) + "; its own " + own.getRealm() + " mortal " + own.isMortal()
					+ " tears " + Ability.SPATIAL_TEAR.isUnlocked(own) + ")");
			SoulProjection.toggle(soul);
			helper.assertTrue(!Ability.SPATIAL_TEAR.isUnlocked(theirs), "Given back, the body can't");
			helper.succeed();
		} finally {
			server.setPvpAllowed(pvp);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anNpcIsSetAsideAndGivenBack(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		CultivatorNpc npc = null;
		try {
			ServerPlayer soul = projected(helper, made, "npc-soul");
			npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(3, 2, 2));
			npc.setRank(PlayerCultivation.rank(Realm.QI_REFINING, Stage.GRAND_PERFECTION));
			npc.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
			npc.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
			npc.setHealth(npc.getMaxHealth() * 0.5f);
			boolean noAi = npc.isNoAi();
			helper.assertTrue(SoulProjection.tryPossess(soul, npc) == InteractionResult.SUCCESS, "The soul takes the cultivator's body");
			helper.assertTrue(npc.isNoAi() && npc.isSilent() && npc.isNoGravity(), "Its own mind is silent while it is set aside");
			helper.assertTrue(npc.isVacant() && npc.isInvisible() && npc.isInvulnerable() && !npc.isPickable() && SoulProjection.isPossessed(npc)
					&& npc.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty(), "It is set aside: unseen, untouched, its things the soul's");
			helper.assertTrue(soul.getMainHandItem().is(Items.IRON_SWORD) && soul.getItemBySlot(EquipmentSlot.CHEST).is(Items.LEATHER_CHESTPLATE)
					&& Math.abs(soul.getHealth() / soul.getMaxHealth() - 0.5f) < 0.01f, "The soul holds its sword and wears its robe, at its health");
			helper.assertTrue(CultivationManager.get(soul) == npc.cultivation() && npc.getPersonalName().equals(Possession.displayName(soul).getString()),
					"Its cultivation and its name are the cultivator's");

			soul.getInventory().setItem(12, new ItemStack(Items.EMERALD, 3));
			soul.moveTo(soul.getX() + 2, soul.getY(), soul.getZ(), 0, 0);
			// Just the possession's own tick (a whole soul's tick would set monsters about on its body: the other tests' too).
			helper.assertTrue(Possession.tick(soul) && npc.distanceTo(soul) < 0.01f, "Set aside, it goes where its body goes");
			SoulProjection.toggle(soul);
			helper.assertTrue(!npc.isVacant() && !npc.isInvisible() && !npc.isInvulnerable() && npc.isNoAi() == noAi && !npc.isSilent(),
					"Given back, it is itself again (as the test made it)");
			helper.assertTrue(npc.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.IRON_SWORD) && npc.getItemBySlot(EquipmentSlot.CHEST).is(Items.LEATHER_CHESTPLATE)
					&& Math.abs(npc.getHealth() / npc.getMaxHealth() - 0.5f) < 0.01f, "...its sword in hand, its robe on, at its health");
			helper.assertTrue(npc.takeWinnings().stream().anyMatch(s -> s.is(Items.EMERALD)), "...and what its body took up, it carries");
			helper.assertTrue(SoulProjection.isSoul(soul) && CultivationManager.get(soul) == CultivationManager.own(soul), "The soul is out again");
			helper.succeed();
		} finally {
			leave(made);
			if (npc != null) npc.discard(); // nothing left about the world for other tests' cultivators and ghosts
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aCrashedPossessionIsPutRight(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer soul = survivor(helper, made, "crash-soul");
			ServerPlayer owner = survivor(helper, made, "crash-owner");
			// As a crash leaves it: the soul's player holding the body's diamonds, its own gold put by in its record; the owner a
			// spectator of it, their record saying so.
			soul.getInventory().setItem(0, new ItemStack(Items.GOLD_INGOT, 3));
			CompoundTag stash = Possession.takeFrom(soul);
			soul.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 4));
			CompoundTag tenant = new CompoundTag();
			tenant.putString("Role", "Tenant");
			tenant.putUUID("Body", owner.getUUID());
			tenant.putBoolean("Npc", false);
			tenant.put("Stash", stash);
			CultivationManager.own(soul).setPossession(tenant);
			CompoundTag held = new CompoundTag();
			held.putString("Role", "Held");
			held.putUUID("By", soul.getUUID());
			held.putInt("Mode", GameType.SURVIVAL.getId());
			CultivationManager.own(owner).setPossession(held);
			owner.setGameMode(GameType.SPECTATOR);

			Possession.onJoin(owner);
			Possession.settle(helper.getLevel().getServer()); // set right on the next tick, once they're in the world
			helper.assertTrue(owner.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && CultivationManager.own(owner).getPossession() == null,
					"The owner back first: their game mode again");
			Possession.onJoin(soul);
			helper.assertTrue(soul.getInventory().countItem(Items.GOLD_INGOT) == 3 && soul.getInventory().countItem(Items.DIAMOND) == 0
					&& CultivationManager.own(soul).getPossession() == null, "The soul's player has their own things back");
			helper.assertTrue(owner.getInventory().countItem(Items.DIAMOND) == 4, "...and the body's go to its owner");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void whatAVictimHeldOnTheirCursorGoesWithTheirBody(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		MinecraftServer server = helper.getLevel().getServer();
		boolean pvp = server.isPvpAllowed();
		server.setPvpAllowed(true);
		try {
			ServerPlayer soul = projected(helper, made, "cursor-soul");
			ServerPlayer victim = victim(helper, made, "cursor-victim", soul);
			victim.containerMenu.setCarried(new ItemStack(Items.EMERALD, 4)); // their inventory open, the emeralds picked up
			helper.assertTrue(SoulProjection.tryPossess(soul, victim) == InteractionResult.SUCCESS, "Possessed");
			helper.assertTrue(victim.containerMenu.getCarried().isEmpty() && soul.getInventory().countItem(Items.EMERALD) == 4,
					"What was on their cursor went with their body");
			SoulProjection.toggle(soul);
			helper.assertTrue(victim.getInventory().countItem(Items.EMERALD) == 4, "...and comes back with it");
			helper.succeed();
		} finally {
			server.setPvpAllowed(pvp);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aHeldPlayerMadeAnewIsHeldStill(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		MinecraftServer server = helper.getLevel().getServer();
		boolean pvp = server.isPvpAllowed();
		server.setPvpAllowed(true);
		try {
			ServerPlayer soul = projected(helper, made, "anew-soul");
			ServerPlayer victim = victim(helper, made, "anew-victim", soul);
			helper.assertTrue(SoulProjection.tryPossess(soul, victim) == InteractionResult.SUCCESS, "Possessed");
			// Their player made anew (as a respawn makes it: a /kill reaches even a spectator).
			ServerPlayer again = server.getPlayerList().respawn(victim, true);
			made.add(again);
			SoulProjection.tick(soul, helper.getLevel().getGameTime());
			helper.assertTrue(Possession.playerBody(soul) == again && again.gameMode.getGameModeForPlayer() == GameType.SPECTATOR
					&& again.getCamera() == soul, "Back as a new body of theirs, they are held to it all the same");
			SoulProjection.toggle(soul);
			helper.assertTrue(again.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && again.getCamera() == again,
					"...and given it back when the soul lets go");
			helper.succeed();
		} finally {
			server.setPvpAllowed(pvp);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSoulInABodyKeepsFillingItsOwnGhostQi(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		MinecraftServer server = helper.getLevel().getServer();
		boolean pvp = server.isPvpAllowed();
		server.setPvpAllowed(true);
		try {
			ServerPlayer soul = projected(helper, made, "ghostqi-soul");
			ServerPlayer victim = victim(helper, made, "ghostqi-victim", soul);
			PlayerCultivation own = CultivationManager.own(soul), theirs = CultivationManager.own(victim);
			own.netherworld().setMethod(GhostMethod.NETHERWORLD_BREATHING, 3, 0);
			own.netherworld().setGhostQi(0);
			theirs.netherworld().setMethod(GhostMethod.NETHERWORLD_BREATHING, 1, 0);
			theirs.netherworld().setGhostQi(0);
			helper.assertTrue(SoulProjection.tryPossess(soul, victim) == InteractionResult.SUCCESS, "Possessed");
			// Into the Nether, where there is ghostly qi for the pools to fill by.
			soul.teleportTo(server.getLevel(net.minecraft.world.level.Level.NETHER), 0.5, 100, 0.5, 0, 0);
			long now = helper.getLevel().getGameTime();
			NetherworldManager.second(soul, now);
			double body = theirs.netherworld().ghostQi();
			helper.assertTrue(own.netherworld().ghostQi() > 0 && body > 0, "The soul's own ghost qi fills by the place, as the body's does ("
					+ own.netherworld().ghostQi() + ", " + body + ")");
			NetherworldManager.second(victim, now); // the held owner's own second
			helper.assertTrue(theirs.netherworld().ghostQi() == body, "...the body's pool filled once, not again through its owner");
			helper.succeed();
		} finally {
			server.setPvpAllowed(pvp);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSparIsCalledOffWhenASoulTakesTheCultivatorsBody(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		CultivatorNpc npc = null;
		try {
			MinecraftServer server = helper.getLevel().getServer();
			ServerPlayer partner = survivor(helper, made, "spar-partner");
			ServerPlayer soul = projected(helper, made, "spar-soul");
			npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(3, 2, 2));
			npc.setRank(PlayerCultivation.rank(Realm.QI_REFINING, Stage.GRAND_PERFECTION));
			SparSystem.start(partner, npc, SparSystem.Terms.FRIENDLY);
			helper.assertTrue(SparSystem.areSparring(partner, npc), "A friendly bout begins");
			int regard = npc.regard(partner);
			helper.assertTrue(SoulProjection.tryPossess(soul, npc) == InteractionResult.SUCCESS, "Another's soul takes the cultivator's body");
			SparSystem.tick(server);
			helper.assertTrue(!SparSystem.isSparring(partner) && npc.regard(partner) == regard,
					"The bout is called off, nobody's loss (" + SparSystem.isSparring(partner) + ", " + regard + " -> " + npc.regard(partner) + ")");
			helper.succeed();
		} finally {
			if (npc != null) npc.discard();
			leave(made);
		}
	}

	// --- Helpers (as SoulProjectionGameTests') ---

	/** A survival player in the player list (the helper's mock player is creative), remembered in {@code made} to take out after. */
	private static ServerPlayer survivor(GameTestHelper helper, List<ServerPlayer> made, String name) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isCreative() {
				return false;
			}
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		p.setOnGround(true);
		made.add(p);
		return p;
	}

	/** A Nascent Soul cultivator with its soul already out, full of qi. */
	private static ServerPlayer projected(GameTestHelper helper, List<ServerPlayer> made, String name) {
		return projected(helper, made, name, Realm.NASCENT_SOUL);
	}

	/** A cultivator of {@code realm} (Nascent Soul or above) with its soul already out, full of qi. */
	private static ServerPlayer projected(GameTestHelper helper, List<ServerPlayer> made, String name, Realm realm) {
		ServerPlayer p = survivor(helper, made, name);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(realm, Stage.EARLY, 0);
		CultivationStats.apply(p, c);
		p.setHealth(p.getMaxHealth());
		c.setQi(c.maxQi());
		long now = helper.getLevel().getGameTime();
		SoulProjection.toggle(p);
		for (int i = 0; i < SoulProjection.CHANNEL_TICKS; i++) SoulProjection.tick(p, now);
		helper.assertTrue(SoulProjection.isSoul(p), "The soul is out");
		return p;
	}

	/** A mortal player beside {@code soul}, their body's strength reckoned. */
	private static ServerPlayer victim(GameTestHelper helper, List<ServerPlayer> made, String name, ServerPlayer soul) {
		ServerPlayer p = survivor(helper, made, name);
		p.moveTo(soul.getX() + 1, soul.getY(), soul.getZ(), 90, 10);
		PlayerCultivation c = CultivationManager.own(p);
		c.setMortal(true);
		CultivationStats.apply(p, c);
		p.setHealth(p.getMaxHealth());
		return p;
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) if (Possession.inhabits(p.getUUID())) SoulProjection.letGo(p, null);
		for (ServerPlayer p : made) {
			SoulProjection.onDeath(p); // nothing of a journey is left behind (its body discards itself)
			CultivationManager.own(p).setPossession(null);
			p.server.getPlayerList().remove(p);
		}
	}
}
