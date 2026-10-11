package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Soul Projection's rules, driven by hand on survival test players (Fabric's fake players can't be hurt or ride, and the test
 * helper's mock player is always creative): each test runs within one tick and takes its players out of the world again.
 */
public class SoulProjectionGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void theSoulsNumbersGrowWithTheRealm(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION, 0);
		helper.assertTrue(!Ability.SOUL_PROJECTION.isUnlocked(c), "A golden core can't send out a soul");
		c.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
		helper.assertTrue(Ability.SOUL_PROJECTION.isUnlocked(c), "A nascent soul can");
		helper.assertTrue(c.soulHearts() == 1 && c.soulTetherRadius() == 32, "Nascent Soul Early: 1 heart, 32 blocks");
		helper.assertTrue(c.soulCostPerSecond(0, false) == 12.5 && c.soulCostPerSecond(1, false) == 50 && c.soulCostPerSecond(1, true) == 75,
				"Half its gathering at the body, twice at the tether's end, a gathering more while possessing");
		c.setState(Realm.NASCENT_SOUL, Stage.GRAND_PERFECTION, 0);
		helper.assertTrue(c.soulHearts() == 10 && c.soulTetherRadius() == 96, "Nascent Soul Grand Perfection: 10 hearts, 96 blocks");
		c.setState(Realm.HEAVENLY_BEING, Stage.EARLY, 0);
		helper.assertTrue(c.soulHearts() == 13 && c.soulTetherRadius() == 128, "Heavenly Being: it keeps growing");
		c.setState(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION, 0);
		helper.assertTrue(c.soulHearts() == 34 && c.soulTetherRadius() == 256, "Four Axis Grand Perfection: 34 hearts, 256 blocks");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theSoulGoesOutAndComesHome(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = cultivator(helper, made, "soul-out", Realm.NASCENT_SOUL, Stage.EARLY);
			PlayerCultivation c = CultivationManager.get(p);
			p.setHealth(50);
			long now = helper.getLevel().getGameTime();
			SoulProjection.toggle(p);
			helper.assertTrue(SoulProjection.state(p) == SoulProjection.State.PROJECTING && c.getSoulState() == SoulProjection.State.PROJECTING
					&& c.isAbilityEnabled(Ability.SOUL_PROJECTION), "Switched on, the soul gathers itself");
			for (int i = 0; i < SoulProjection.CHANNEL_TICKS - 1; i++) SoulProjection.tick(p, now);
			helper.assertTrue(SoulProjection.state(p) == SoulProjection.State.PROJECTING, "It takes 5 seconds");
			SoulProjection.tick(p, now);
			helper.assertTrue(SoulProjection.isSoul(p) && c.isSoulOut(), "Then it is out");
			SoulBodyEntity body = bodyNear(helper, p);
			helper.assertTrue(body != null && body.getHealth() == 50 && body.getMaxHealth() == 65, "Its body sits on with the player's health");
			helper.assertTrue(p.getMaxHealth() == 2 && p.getHealth() == 2, "The soul has 1 heart");
			helper.assertTrue(c.isQiSenseOn(), "It sees by Qi Sense");
			c.setAbilityEnabled(Ability.QI_SENSE, false);
			helper.assertTrue(c.isQiSenseOn(), "...forced open, switched off or not");
			c.setAbilityEnabled(Ability.QI_SENSE, true);

			MeditationManager.start(p);
			helper.assertTrue(!MeditationManager.isMeditating(p.getUUID()), "A soul can't meditate");
			c.addCultivation(c.cultivationRequired());
			TribulationManager.start(p);
			helper.assertTrue(!TribulationManager.isActive(p.getUUID()), "Nor face a tribulation (it can't be dodged in or out of a body)");

			helper.assertTrue(SoulProjection.upkeep(p, c) == 12.5, "At the body it costs half its gathering");
			p.setPos(p.getX() + 16, p.getY(), p.getZ());
			helper.assertTrue(Math.abs(SoulProjection.upkeep(p, c) - 31.25) < 1.0e-6, "Halfway along the tether, more");
			p.setPos(p.getX() - 16, p.getY(), p.getZ());

			SoulProjection.toggle(p);
			helper.assertTrue(SoulProjection.state(p) == SoulProjection.State.RECALLING, "Called home");
			for (int i = 0; i < SoulProjection.CHANNEL_TICKS; i++) SoulProjection.tick(p, now);
			helper.assertTrue(!SoulProjection.isActive(p.getUUID()) && !SoulProjection.isSoul(p) && !c.isAbilityEnabled(Ability.SOUL_PROJECTION),
					"After 5 seconds it is back");
			helper.assertTrue(p.getMaxHealth() == 65 && p.getHealth() == 50, "With the body's health, no more, no less (" + p.getHealth() + ")");
			helper.assertTrue(body.isRemoved(), "The body is the player again");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void goingOutTakesStillnessQiAndNoTribulation(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = cultivator(helper, made, "soul-still", Realm.NASCENT_SOUL, Stage.EARLY);
			PlayerCultivation c = CultivationManager.get(p);
			long now = helper.getLevel().getGameTime();
			SoulProjection.toggle(p);
			p.setPos(p.getX() + 1, p.getY(), p.getZ());
			SoulProjection.tick(p, now);
			helper.assertTrue(!SoulProjection.isActive(p.getUUID()), "Moving breaks it off");

			c.setQi(c.maxQi() * 0.05);
			SoulProjection.toggle(p);
			helper.assertTrue(!SoulProjection.isActive(p.getUUID()), "Too little qi to leave");
			c.setQi(c.maxQi());

			c.addCultivation(c.cultivationRequired());
			TribulationManager.start(p);
			helper.assertTrue(TribulationManager.isActive(p.getUUID()), "A tribulation begins");
			SoulProjection.toggle(p);
			helper.assertTrue(!SoulProjection.isActive(p.getUUID()), "The soul can't slip out from under it");
			TribulationManager.forget(p.getUUID());
			c.setInTribulation(false);
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void qiRunningDryBringsTheSoulHomeAtOnce(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = projected(helper, made, "soul-dry", Realm.NASCENT_SOUL, Stage.EARLY);
			PlayerCultivation c = CultivationManager.get(p);
			p.setPos(p.getX() + 30, p.getY(), p.getZ());
			c.setQi(0.5);
			SoulProjection.upkeep(p, c);
			helper.assertTrue(!SoulProjection.isActive(p.getUUID()) && p.isAlive(), "Out of qi it snaps back, unharmed");
			helper.assertTrue(c.getRealm() == Realm.NASCENT_SOUL, "No penalty");
			helper.assertTrue(p.position().distanceTo(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)))) < 1, "Back where the body sat");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSeveredSoulCostsTheNascentSoul(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = projected(helper, made, "soul-cut", Realm.NASCENT_SOUL, Stage.MID);
			PlayerCultivation c = CultivationManager.get(p);
			SoulBodyEntity body = bodyNear(helper, p);
			SoulProjection.sever(p, ModLang.MSG_SOUL_SEVERED);
			helper.assertTrue(!p.isAlive(), "The cultivator dies");
			helper.assertTrue(c.getRealm() == Realm.CORE_FORMATION && c.getStage() == Stage.GRAND_PERFECTION && c.getCultivation() == 0,
					"And falls to Core Formation Grand Perfection: the nascent soul must be formed again");
			helper.assertTrue(!SoulProjection.isActive(p.getUUID()) && c.getSoulReturn() == null && body != null && body.isRemoved(),
					"Nothing of the journey is left");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void loggingOutMidFightSeversTheSoul(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer calm = projected(helper, made, "soul-calm", Realm.NASCENT_SOUL, Stage.EARLY);
			SoulProjection.onDisconnect(calm);
			helper.assertTrue(calm.isAlive() && !SoulProjection.isActive(calm.getUUID()) && CultivationManager.get(calm).getRealm() == Realm.NASCENT_SOUL,
					"Logging out at peace brings the soul home, no penalty");

			ServerPlayer fleeing = projected(helper, made, "soul-flee", Realm.NASCENT_SOUL, Stage.EARLY);
			SoulBodyEntity body = bodyNear(helper, fleeing);
			SoulProjection.onBodyHurt(body, fleeing.getUUID());
			SoulProjection.onDisconnect(fleeing);
			helper.assertTrue(!fleeing.isAlive() && CultivationManager.get(fleeing).getRealm() == Realm.CORE_FORMATION,
					"Logging out within 10 s of harm to the body or soul is fleeing: the tether snaps");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void onlyThoseWhoSeeTheSoulCanHarmIt(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			MinecraftServer server = helper.getLevel().getServer();
			boolean pvp = server.isPvpAllowed();
			server.setPvpAllowed(true);
			try {
				ServerPlayer soul = projected(helper, made, "soul-hunted", Realm.NASCENT_SOUL, Stage.EARLY);
				ServerPlayer mortal = survivor(helper, made, "mortal");
				ServerPlayer blind = cultivator(helper, made, "ns-blind", Realm.NASCENT_SOUL, Stage.EARLY);
				CultivationManager.get(blind).setAbilityEnabled(Ability.QI_SENSE, false);
				ServerPlayer seeing = cultivator(helper, made, "ns-seeing", Realm.NASCENT_SOUL, Stage.EARLY);
				ServerPlayer clear = cultivator(helper, made, "ns-gp", Realm.NASCENT_SOUL, Stage.GRAND_PERFECTION);
				ServerPlayer heavenly = cultivator(helper, made, "hb", Realm.HEAVENLY_BEING, Stage.EARLY);
				CultivationManager.get(heavenly).setAbilityEnabled(Ability.QI_SENSE, false);

				helper.assertTrue(!SoulProjection.perceives(mortal) && !SoulProjection.perceives(blind), "Mortals, and Nascent Souls without Qi Sense, aren't even sent it");
				helper.assertTrue(SoulProjection.perceives(seeing) && SoulProjection.perceives(heavenly), "Nascent Souls with Qi Sense are; Heavenly Beings always are");
				helper.assertTrue(SoulProjection.harmFactor(mortal) == 0 && SoulProjection.harmFactor(blind) == 0, "Who can't see it can't harm it");
				helper.assertTrue(SoulProjection.harmFactor(seeing) == 0.25f && SoulProjection.harmFactor(clear) == 0.7f,
						"To a Nascent Soul it is a haze: a quarter of a blow at Early, more each stage");
				helper.assertTrue(SoulProjection.harmFactor(heavenly) == 1, "A Heavenly Being strikes it whole");

				CultivatorNpc nascent = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(4, 2, 4));
				nascent.setRank(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.MID));
				CultivatorNpc core = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(5, 2, 4));
				core.setRank(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION));
				helper.assertTrue(SoulProjection.harmFactor(nascent) == 0.4f && SoulProjection.harmFactor(core) == 0, "NPC cultivators the same");
				helper.assertTrue(nascent.senses(soul) && !core.senses(soul), "...and only those of Nascent Soul and above sense it at all");

				var sources = helper.getLevel().damageSources();
				helper.assertTrue(!SoulProjection.allowSoulDamage(soul, sources.fall()), "No fall harms it");
				helper.assertTrue(!SoulProjection.allowSoulDamage(soul, sources.playerAttack(mortal)), "Nor a mortal's fist");
				helper.assertTrue(SoulProjection.allowSoulDamage(soul, sources.playerAttack(heavenly)), "A Heavenly Being's does");
				helper.assertTrue(!SoulProjection.allowSoulDamage(soul, sources.arrow(new Arrow(helper.getLevel(), heavenly), heavenly)), "Not an arrow");
				helper.assertTrue(SoulProjection.scaleSoulDamage(soul, sources.playerAttack(seeing), 8) == 2, "A haze takes a quarter of the blow");
			} finally {
				server.setPvpAllowed(pvp);
			}
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theSoulPossessesOnlyTheFarWeaker(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer soul = projected(helper, made, "soul-possess", Realm.NASCENT_SOUL, Stage.EARLY);
			PlayerCultivation c = CultivationManager.get(soul);
			Zombie named = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 2));
			named.setCustomName(Component.literal("Gerald"));
			helper.assertTrue(SoulProjection.tryPossess(soul, named) == InteractionResult.FAIL && !named.isVehicle(), "Not a named creature");
			CultivatorNpc foundation = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(2, 2, 3));
			foundation.setRank(PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY));
			helper.assertTrue(SoulProjection.tryPossess(soul, foundation) == InteractionResult.FAIL, "Not one only two realms below");
			CultivatorNpc refiner = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(1, 2, 2));
			refiner.setRank(PlayerCultivation.rank(Realm.QI_REFINING, Stage.GRAND_PERFECTION));
			helper.assertTrue(SoulProjection.realmOf(refiner) == 0, "A Qi Refiner is three realms below a Nascent Soul");

			Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 1));
			double qi = c.getQi();
			helper.assertTrue(SoulProjection.tryPossess(soul, zombie) == InteractionResult.SUCCESS, "A beast counts as a mortal");
			helper.assertTrue(zombie.getFirstPassenger() == soul && SoulProjection.possessorOf(zombie) == soul && zombie.getControllingPassenger() == soul,
					"The soul rides inside it, and steers it");
			helper.assertTrue(c.getQi() < qi, "Taking hold costs qi");
			ServerPlayer heavenly = cultivator(helper, made, "hb-striker", Realm.HEAVENLY_BEING, Stage.EARLY);
			helper.assertTrue(!SoulProjection.allowSoulDamage(soul, helper.getLevel().damageSources().playerAttack(heavenly)),
					"Inside its host the soul can't be struck (strike the host)");
			Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 2));
			SoulProjection.hostAttack(soul, pig);
			helper.assertTrue(pig.getHealth() < pig.getMaxHealth() && pig.getKillCredit() == soul,
					"The host strikes for the soul, and the blow is the soul's");

			float health = soul.getHealth();
			zombie.kill();
			SoulProjection.tick(soul, helper.getLevel().getGameTime());
			helper.assertTrue(!soul.isPassenger() && SoulProjection.isSoul(soul) && soul.getHealth() == health,
					"The host dead, the soul is thrown out unharmed");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aPossessedCultivatorTalksToNoOne(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer soul = projected(helper, made, "soul-talk", Realm.NASCENT_SOUL, Stage.EARLY);
			CultivatorNpc npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(3, 2, 2));
			npc.setRank(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY));
			npc.setAlignment(0);
			ServerPlayer talker = cultivator(helper, made, "talker", Realm.QI_REFINING, Stage.EARLY);
			talker.moveTo(npc.getX() + 1.5, npc.getY(), npc.getZ(), 0, 0);
			TalkSystem.open(talker, npc);
			helper.assertTrue(npc.talkPartner() == talker, "It talks with a player");
			helper.assertTrue(SoulProjection.tryPossess(soul, npc) == InteractionResult.SUCCESS, "A soul takes it over mid-talk");
			helper.assertTrue(npc.talkPartner() == null, "...and the talk is broken off");
			TalkSystem.open(talker, npc);
			helper.assertTrue(npc.talkPartner() == null && ModLang.TALK_POSSESSED.equals(TalkSystem.refusal(talker, npc)),
					"Possessed, it won't talk to anyone");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSeizedPlayersBodyCarriesTheirThings(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			MinecraftServer server = helper.getLevel().getServer();
			boolean pvp = server.isPvpAllowed();
			GameRules.BooleanValue keep = helper.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);
			boolean kept = keep.get();
			server.setPvpAllowed(true);
			keep.set(false, server);
			try {
				ServerPlayer soul = projected(helper, made, "soul-seize", Realm.NASCENT_SOUL, Stage.EARLY);
				ServerPlayer victim = survivor(helper, made, "victim");
				victim.moveTo(soul.getX() + 1, soul.getY(), soul.getZ(), 0, 0);
				victim.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 5)); // in the pack, not the hand (that would be held)
				victim.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
				helper.assertTrue(SoulProjection.tryPossess(soul, victim) == InteractionResult.SUCCESS, "A mortal player's body is seized");
				helper.assertTrue(!victim.isAlive() && victim.getInventory().isEmpty(), "They die, and drop nothing");
				helper.assertTrue(soul.getVehicle() instanceof SoulVesselEntity vessel && vessel.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET)
						&& vessel.carried().stream().anyMatch(s -> s.is(Items.DIAMOND)), "Their body, with all they carried, is the soul's");
				SoulVesselEntity vessel = (SoulVesselEntity) soul.getVehicle();
				SoulProjection.toggle(soul); // called home: the vessel collapses
				helper.assertTrue(!vessel.isAlive(), "Left without a soul, the body falls");
				List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(vessel.blockPosition()).inflate(3));
				helper.assertTrue(dropped.stream().anyMatch(e -> e.getItem().is(Items.DIAMOND)) && dropped.stream().anyMatch(e -> e.getItem().is(Items.IRON_HELMET)),
						"And everything it carried falls where it does");
			} finally {
				server.setPvpAllowed(pvp);
				keep.set(kept, server);
			}
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aCrashMidJourneyEndsBackInTheBody(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = cultivator(helper, made, "soul-crash", Realm.NASCENT_SOUL, Stage.EARLY);
			PlayerCultivation c = CultivationManager.get(p);
			BlockPos at = helper.absolutePos(new BlockPos(5, 2, 5));
			CompoundTag back = new CompoundTag();
			back.putString("Dimension", helper.getLevel().dimension().location().toString());
			back.putDouble("X", at.getX() + 0.5);
			back.putDouble("Y", at.getY());
			back.putDouble("Z", at.getZ() + 0.5);
			back.putFloat("YRot", 0);
			back.putFloat("XRot", 0);
			back.putFloat("Health", 7);
			c.setSoulReturn(back);
			c.setAbilityEnabled(Ability.SOUL_PROJECTION, true);
			p.setPos(p.getX() + 20, p.getY() + 5, p.getZ()); // where the soul was when it all went down
			SoulProjection.onJoin(p);
			helper.assertTrue(p.blockPosition().equals(at) && p.getHealth() == 7, "Back where the body sat, with its health");
			helper.assertTrue(c.getSoulReturn() == null && !c.isAbilityEnabled(Ability.SOUL_PROJECTION) && c.getRealm() == Realm.NASCENT_SOUL,
					"And no penalty");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	/** A survival test player at the test's floor at {@code realm}/{@code stage}, healthy and full of qi. */
	private static ServerPlayer cultivator(GameTestHelper helper, List<ServerPlayer> made, String name, Realm realm, Stage stage) {
		ServerPlayer p = survivor(helper, made, name);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		p.setOnGround(true);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(realm, stage, 0);
		CultivationStats.apply(p, c);
		p.setHealth(p.getMaxHealth());
		c.setQi(c.maxQi());
		return p;
	}

	/**
	 * A survival player in the player list, as the test helper's mock player but not creative (that one can't die without a
	 * bypass, nor be seized), and remembered in {@code made} to be taken out of the world after.
	 */
	private static ServerPlayer survivor(GameTestHelper helper, List<ServerPlayer> made, String name) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isSpectator() {
				return false;
			}

			@Override
			public boolean isCreative() {
				return false;
			}
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		made.add(p);
		return p;
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) {
			SoulProjection.onDeath(p); // nothing of a journey is left behind (its body discards itself)
			p.server.getPlayerList().remove(p);
		}
	}

	/** {@link #cultivator}, with its soul already out. */
	private static ServerPlayer projected(GameTestHelper helper, List<ServerPlayer> made, String name, Realm realm, Stage stage) {
		ServerPlayer p = cultivator(helper, made, name, realm, stage);
		long now = helper.getLevel().getGameTime();
		SoulProjection.toggle(p);
		for (int i = 0; i < SoulProjection.CHANNEL_TICKS; i++) SoulProjection.tick(p, now);
		helper.assertTrue(SoulProjection.isSoul(p), "The soul is out");
		return p;
	}

	private static SoulBodyEntity bodyNear(GameTestHelper helper, ServerPlayer owner) {
		for (SoulBodyEntity body : helper.getLevel().getEntitiesOfClass(SoulBodyEntity.class, owner.getBoundingBox().inflate(40))) {
			if (owner.getUUID().equals(body.getOwnerId())) return body;
		}
		return null;
	}
}
