package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The methods overhaul, phase 3 (the user's design of 2026-10-31): Five Elements Cycling's offerings (remade 2026-11-01); the shrines' prayer, faith, requests,
 * incense and desecration; a beast subdued, bound and grown with, and its death's price; a ghost palace held and its court's tribute;
 * souls seized on the Soul River; the living's villages haunted, an exorcist sent for.
 */
public class MethodsPhaseThreeGameTests implements FabricGameTest {
	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-6 * Math.max(1, Math.abs(b));
	}

	private static String key(Component message) {
		return message != null && message.getContents() instanceof TranslatableContents t ? t.getKey() : "";
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void fiveElementsAreTakenInAsOfferingsInTheirOrder(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "five-elements");
			PlayerCultivation c = CultivationManager.own(p);
			c.awaken(0);
			TestMethods.setAt(c, Realm.CORE_FORMATION, Stage.EARLY);
			// What is an offering of which element, and the realm it stands at.
			helper.assertTrue(FiveElementsCycling.element(new ItemStack(Items.OAK_SAPLING)) == QiElement.WOOD
					&& FiveElementsCycling.element(new ItemStack(Items.COAL)) == QiElement.FIRE
					&& FiveElementsCycling.element(new ItemStack(Items.CLAY_BALL)) == QiElement.EARTH
					&& FiveElementsCycling.element(new ItemStack(Items.IRON_INGOT)) == QiElement.METAL
					&& FiveElementsCycling.element(new ItemStack(Items.WATER_BUCKET)) == QiElement.WATER
					&& FiveElementsCycling.element(new ItemStack(Items.STONE)) == null, "The five's offerings; stone none");
			int coreRank = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY);
			ItemStack fireCore = DemonCoreItem.of(coreRank, QiElement.FIRE, null);
			helper.assertTrue(FiveElementsCycling.element(fireCore) == QiElement.FIRE && FiveElementsCycling.grade(fireCore) == Realm.CORE_FORMATION
					&& FiveElementsCycling.element(DemonCoreItem.of(coreRank, QiElement.YIN, null)) == null, "A core its element and realm; a yin core none");
			helper.assertTrue(FiveElementsCycling.grade(GinsengItem.create(ModItems.GINSENG, 40)) == Realm.FOUNDATION_BUILDING
					&& FiveElementsCycling.grade(GinsengItem.create(ModItems.LINGZHI, 1200)) == Realm.HEAVENLY_BEING
					&& FiveElementsCycling.element(GinsengItem.create(ModItems.NETHERWORLD_LOTUS, 40)) == null
					&& FiveElementsCycling.grade(new ItemStack(Items.IRON_INGOT)) == Realm.QI_REFINING, "Herbs by their age (not the Netherworld's); mortal things Qi Refining");
			helper.assertTrue(near(FiveElementsCycling.worth(Realm.CORE_FORMATION, Realm.CORE_FORMATION), FiveElementsCycling.LINK)
					&& near(FiveElementsCycling.worth(Realm.QI_REFINING, Realm.CORE_FORMATION), FiveElementsCycling.LINK / 4)
					&& near(FiveElementsCycling.worth(Realm.VOID_SHATTERING, Realm.CORE_FORMATION), FiveElementsCycling.LINK * 4), "x2 a realm above (at most two), /2 below");
			// Not practised: an offering sneak-used is only used.
			p.setShiftKeyDown(true);
			p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COAL, 3));
			helper.assertTrue(!FiveElementsCycling.practises(c)
					&& FiveElementsCycling.use(p, p.level(), InteractionHand.MAIN_HAND).getResult() == InteractionResult.PASS, "Qi Circulation: no offerings");
			CultivationMethods.learn(p, CultivationMethod.FIVE_ELEMENTS, Realm.ENTERING_NIRVANA);
			CultivationMethods.practise(p, CultivationMethod.FIVE_ELEMENTS);
			helper.assertTrue(FiveElementsCycling.practises(c), "Practised");
			// Sneak-used: one taken in, the first link.
			helper.assertTrue(FiveElementsCycling.use(p, p.level(), InteractionHand.MAIN_HAND).getResult().consumesAction()
					&& p.getMainHandItem().getCount() == 2 && FiveElementsCycling.links(p.getUUID()) == 1, "Coal taken in: one fewer, the first link");
			helper.assertTrue(!FiveElementsCycling.offer(p, new ItemStack(Items.COAL), QiElement.FIRE) && FiveElementsCycling.links(p.getUUID()) == 1,
					"Fire twice: only refused");
			// Earth, metal, water (its bucket left), wood: closed. Five mortal things at Core Formation, each a quarter of a link.
			double before = c.getCultivation();
			FiveElementsCycling.offer(p, new ItemStack(Items.CLAY_BALL), QiElement.EARTH);
			FiveElementsCycling.offer(p, new ItemStack(Items.IRON_INGOT), QiElement.METAL);
			p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
			FiveElementsCycling.use(p, p.level(), InteractionHand.MAIN_HAND);
			helper.assertTrue(p.getMainHandItem().is(Items.BUCKET) && FiveElementsCycling.links(p.getUUID()) == 4, "The water bucket taken in, its bucket left");
			FiveElementsCycling.offer(p, new ItemStack(Items.OAK_LOG), QiElement.WOOD);
			helper.assertTrue(FiveElementsCycling.links(p.getUUID()) == 0
					&& near(c.getCultivation() - before, c.cultivationRequired() * 5 * FiveElementsCycling.worth(Realm.QI_REFINING, Realm.CORE_FORMATION)),
					"The cycle closed: five mortal things' worth won");
			// Circulating: nothing taken till the elements settle.
			helper.assertTrue(!FiveElementsCycling.offer(p, new ItemStack(Items.OAK_SAPLING), QiElement.WOOD), "Circulating: refused");
			FiveElementsCycling.forget(p.getUUID()); // settled at once
			// Five Core Formation cores, in order from water: half the stage.
			c.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
			for (QiElement element : new QiElement[] {QiElement.WATER, QiElement.WOOD, QiElement.FIRE, QiElement.EARTH, QiElement.METAL}) {
				helper.assertTrue(FiveElementsCycling.offer(p, DemonCoreItem.of(coreRank, element, null), element), "A core of " + element + " taken in");
			}
			helper.assertTrue(near(c.getCultivation(), c.cultivationRequired() * 5 * FiveElementsCycling.LINK), "Five cores of the realm: half the stage");
			FiveElementsCycling.forget(p.getUUID());
			// Out of order: a deviation, the offering gone, the cycle lost, a twentieth of the stage with it.
			c.setState(Realm.CORE_FORMATION, Stage.EARLY, c.cultivationRequired() * 0.5);
			FiveElementsCycling.offer(p, new ItemStack(Items.OAK_SAPLING), QiElement.WOOD);
			helper.assertTrue(FiveElementsCycling.offer(p, new ItemStack(Items.IRON_INGOT), QiElement.METAL) && FiveElementsCycling.links(p.getUUID()) == 0
					&& near(c.getCultivation(), c.cultivationRequired() * (0.5 - FiveElementsCycling.DEVIATION_LOSS)), "Metal after wood: a deviation");
			// A full stage takes nothing in.
			c.setState(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION, PlayerCultivation.required(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION));
			helper.assertTrue(c.isStalled() && !FiveElementsCycling.offer(p, new ItemStack(Items.OAK_SAPLING), QiElement.WOOD), "The bottleneck: refused");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aShrineIsPrayedAtAndItsFaithFlows(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<Entity> spawned = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		BlockPos at = new BlockPos(3, 2, 3), abs = helper.absolutePos(at);
		try {
			ServerPlayer owner = survivor(helper, made, "shrine-owner");
			PlayerCultivation c = CultivationManager.own(owner);
			c.awaken(0);
			TestMethods.setAt(c, Realm.FOUNDATION_BUILDING, Stage.EARLY);
			CultivationMethods.learn(owner, CultivationMethod.INCENSE_FAITH, Realm.TRUE_IMMORTAL);
			CultivationMethods.practise(owner, CultivationMethod.INCENSE_FAITH);
			helper.assertTrue(Shrines.kindFor(c) == Shrines.Kind.HEAVEN, "Practising Incense and Faith: a statue");
			helper.setBlock(at, ModBlocks.SHRINE);
			ShrineBlockEntity shrine = (ShrineBlockEntity) level.getBlockEntity(abs);
			shrine.setOwner(owner.getUUID(), "shrine-owner");
			// (A neighbouring test's people may stand within the shrine's reach: what ours add is measured against them.)
			double heavenBefore = Shrines.worship(level, abs, Shrines.Kind.HEAVEN), demonBefore = Shrines.worship(level, abs, Shrines.Kind.DEMON);
			for (int i = 0; i < 2; i++) spawned.add(helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(5 + i, 2, 5)));
			CultivatorNpc npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(1, 2, 5));
			npc.becomeRogue(PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY), 0, level.getRandom());
			spawned.add(npc);
			double heaven = Shrines.worship(level, abs, Shrines.Kind.HEAVEN);
			helper.assertTrue(near(heaven - heavenBefore, 4) && near(Shrines.worship(level, abs, Shrines.Kind.DEMON) - demonBefore, 2),
					"Two villagers (1 each) and a Foundation Building cultivator (2): 4; a mountain god hears the mortals alone: 2");
			Shrines.pray(level, abs, Shrines.Kind.HEAVEN, shrine, level.getGameTime());
			helper.assertTrue(near(shrine.faith(), heaven), "Their prayer laid up as faith (" + shrine.faith() + ")");
			shrine.setFaith(100);
			double before = c.getCultivation();
			double share = Shrines.flow(level, Shrines.Kind.HEAVEN, shrine);
			helper.assertTrue(near(share, 100 * Shrines.perFaith(1)) && near(c.getCultivation() - before, share * c.cultivationRequired()) && shrine.faith() < 1e-6,
					"Its faith flows to its owner: 0.002 of the stage a faith at Foundation Building (" + share + ")");
			Shrines.request(level, abs, Shrines.Kind.HEAVEN, shrine, level.getGameTime());
			helper.assertTrue(shrine.hasRequest(), "A day on, its people ask something of it");
			ItemStack asked = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(new net.minecraft.resources.ResourceLocation(shrine.request())),
					shrine.requestCount());
			owner.setItemInHand(InteractionHand.MAIN_HAND, asked);
			Shrines.use(level.getBlockState(abs), owner, shrine, InteractionHand.MAIN_HAND);
			helper.assertTrue(!shrine.hasRequest() && near(shrine.devotion(), 1.25) && near(shrine.faith(), Shrines.ANSWER_FAITH) && owner.getMainHandItem().isEmpty(),
					"Brought: devotion up a quarter, and faith laid up for it");
			ServerPlayer pilgrim = survivor(helper, made, "shrine-pilgrim");
			CultivationManager.own(pilgrim).awaken(0);
			pilgrim.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.INCENSE, 2));
			Shrines.use(level.getBlockState(abs), pilgrim, shrine, InteractionHand.MAIN_HAND);
			helper.assertTrue(near(shrine.faith(), Shrines.ANSWER_FAITH + Shrines.INCENSE_FAITH * 1.25) && pilgrim.getMainHandItem().getCount() == 1,
					"A pilgrim's incense: 4 faith, by their devotion");
			owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.INCENSE, 2));
			double faith = shrine.faith();
			Shrines.use(level.getBlockState(abs), owner, shrine, InteractionHand.MAIN_HAND);
			helper.assertTrue(shrine.faith() == faith && owner.getMainHandItem().getCount() == 2, "Its owner's own incense: nothing");
			owner.setHealth(owner.getMaxHealth());
			float whole = owner.getHealth();
			shrine.breaker = pilgrim.getUUID();
			level.destroyBlock(abs, false);
			helper.assertTrue(Math.abs(owner.getHealth() - whole * (1 - Shrines.WOUND)) < 0.01, "Desecrated by another: its owner wounded (" + owner.getHealth() + ")");
			helper.succeed();
		} finally {
			spawned.forEach(Entity::discard);
			helper.setBlock(at, Blocks.AIR);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSubduedBeastIsBoundAndGrownWith(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<Entity> spawned = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		try {
			ServerPlayer owner = survivor(helper, made, "beast-binder");
			PlayerCultivation c = CultivationManager.own(owner);
			c.awaken(0);
			TestMethods.setAt(c, Realm.CORE_FORMATION, Stage.EARLY);
			CultivationMethods.learn(owner, CultivationMethod.SPIRIT_BEAST_BOND, Realm.TRUE_IMMORTAL);
			CultivationMethods.practise(owner, CultivationMethod.SPIRIT_BEAST_BOND);
			DemonBeast wolf = beast(helper, spawned, BeastKind.SHADOW_WOLF, new BlockPos(3, 2, 3), 0);
			helper.assertTrue(SpiritBonds.interact(wolf, owner, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME && wolf.getOwnerUUID() == null,
					"Not subdued: no bond");
			wolf.setHealth(wolf.getMaxHealth() * 0.2f);
			helper.assertTrue(SpiritBonds.interact(wolf, owner, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS && owner.getUUID().equals(wolf.getOwnerUUID())
					&& wolf.getUUID().equals(c.spiritBeast()) && wolf.getHealth() == wolf.getMaxHealth(), "Subdued: bound, and whole again");
			helper.assertTrue(!wolf.hunts(owner) && !wolf.canAttack(owner), "Bound, it never turns on its owner");
			DemonBeast other = beast(helper, spawned, BeastKind.SHADOW_WOLF, new BlockPos(5, 2, 3), 0);
			other.setHealth(other.getMaxHealth() * 0.2f);
			helper.assertTrue(key(SpiritBonds.refusal(other, owner, c)).equals(ModLang.MSG_BOND_ALREADY), "One bond at a time");
			int rank = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY);
			double before = c.getCultivation();
			ItemStack core = DemonCoreItem.of(rank, QiElement.FIRE, null);
			owner.setItemInHand(InteractionHand.MAIN_HAND, core);
			SpiritBonds.interact(wolf, owner, InteractionHand.MAIN_HAND);
			helper.assertTrue(wolf.getRank() == 1 && near(c.getCultivation() - before, SpiritBonds.share(2, 2) * c.cultivationRequired()) && owner.getMainHandItem().isEmpty(),
					"A core fed: it grows a stage, its owner a share of the stage with it");
			before = c.getCultivation();
			DemonBeast prey = beast(helper, spawned, BeastKind.SHADOW_WOLF, new BlockPos(6, 2, 6), 0);
			SpiritBonds.killed(wolf, prey);
			helper.assertTrue(near(c.getCultivation() - before, SpiritBonds.killShare(0, 2) * c.cultivationRequired()), "Its kill is its owner's share too");
			c.setState(Realm.CORE_FORMATION, Stage.MID, 30);
			SpiritBonds.died(level, wolf);
			helper.assertTrue(c.getStage() == Stage.EARLY && c.getCultivation() == 0 && c.spiritBeast() == null,
					"It died: the realm's progress it carried gone, its owner back at Core Formation Early");
			helper.succeed();
		} finally {
			spawned.forEach(Entity::discard);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aPalaceHeldAndItsCourtsTribute(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		ServerLevel netherworld = helper.getLevel().getServer().getLevel(ModDimensions.NETHERWORLD);
		BlockPos throne = new BlockPos(9_000_123, 80, 9_000_456);
		try {
			ServerPlayer king = survivor(helper, made, "court-king"), stranger = survivor(helper, made, "court-stranger");
			GhostPalaces.note(netherworld, throne, UUID.randomUUID());
			GhostPalaces.vacate(netherworld, throne);
			helper.assertTrue(throne.equals(GhostPalaces.vacantNear(netherworld, throne.offset(2, 0, 0), CourtOfTheDead.CLAIM_REACH)), "Its king slain: the throne stands empty");
			GhostPalaces.claim(netherworld, throne, king.getUUID());
			helper.assertTrue(throne.equals(GhostPalaces.heldBy(netherworld, king.getUUID())) && GhostPalaces.vacantNear(netherworld, throne, 4) == null
					&& king.getUUID().equals(GhostPalaces.kingOf(netherworld, throne.offset(10, 0, 0), 48)), "Taken: the player's palace now");
			GhostCultivatorNpc retainer = ModEntities.GHOST_CULTIVATOR.create(netherworld);
			retainer.becomeCourtier(PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY), 0, throne.offset(10, 0, 0), netherworld.getRandom());
			helper.assertTrue(retainer.isAlly(king) && !retainer.isAlly(stranger) && retainer.servesThrone(throne), "Its retainers are its subjects, no one else's");
			retainer.discard();
			helper.assertTrue(near(CourtOfTheDead.tribute(1, 2), 0.01) && near(CourtOfTheDead.tribute(1, 9), CourtOfTheDead.FLOOR),
					"A Foundation Building retainer's tribute to a Core Formation king: 0.01 of the stage; never less than the floor");
			helper.succeed();
		} finally {
			GhostPalaces.forget(netherworld, throne);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void soulsAreSeizedOnTheRiver(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			helper.assertTrue(near(SoulRiverFeast.share(0, 1), 0.025) && near(SoulRiverFeast.share(2, 1), 0.1) && near(SoulRiverFeast.share(9, 1), SoulRiverFeast.MOST)
					&& SoulRiverFeast.fightsBack(2, 1) && !SoulRiverFeast.fightsBack(1, 1), "A soul's worth by its realm against the seizer's; one above fights back");
			for (int i = 0; i < 200; i++) {
				int realm = SoulRiverFeast.roll(5);
				helper.assertTrue(realm >= 2 && realm <= 6, "The river carries souls three below to one above (" + realm + ")");
			}
			ServerPlayer p = survivor(helper, made, "river-feaster");
			PlayerCultivation c = CultivationManager.own(p);
			c.awaken(0);
			GhostMethods.give(p, GhostMethod.NETHERWORLD_BREATHING);
			GhostMethods.give(p, GhostMethod.YELLOW_SPRINGS_MARROW);
			NetherworldState s = c.netherworld();
			int fb = PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY);
			GhostMethods.setPath(s, fb);
			s.setMethod(GhostMethod.NETHERWORLD_BREATHING, 1, 0);
			s.setMethod(GhostMethod.YELLOW_SPRINGS_MARROW, 3, 0);
			CultivationMethods.learn(p, CultivationMethod.SOUL_RIVER_FEAST, Realm.SACRED_VESSEL);
			CultivationMethods.practise(p, CultivationMethod.SOUL_RIVER_FEAST);
			helper.assertTrue(SoulRiverFeast.feasts(p), "Practising the Feast, its Foundation Building volume held");
			helper.assertTrue(SoulRiverFeast.take(p, 0, p.getEyePosition()) && near(s.pathCultivation(), SoulRiverFeast.share(0, 1) * GhostMethods.required(fb)),
					"A Qi Refining soul fed on: a fortieth of the stage (" + s.pathCultivation() + ")");
			SoulRiverFeast.forgetMemory(p);
			helper.assertTrue(s.layer(GhostMethod.YELLOW_SPRINGS_MARROW) == 2, "The river takes a memory: a foundation's top layer");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDeadHauntTheLivingsVillages(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<Entity> spawned = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		try {
			ServerPlayer ghost = survivor(helper, made, "haunting-ghost"), living = survivor(helper, made, "haunting-living");
			PlayerCultivation c = CultivationManager.own(ghost);
			c.awaken(0);
			CultivationManager.own(living).awaken(0);
			GhostSystem.makeGhost(ghost); // taught Netherworld Breathing, its ghost path begun
			CultivationMethods.learn(ghost, CultivationMethod.HAUNTING, Realm.NASCENT_SOUL);
			CultivationMethods.practise(ghost, CultivationMethod.HAUNTING);
			helper.assertTrue(Haunting.haunts(ghost) && Haunting.crosses(ghost) && !Haunting.crosses(living), "One of the dead haunting crosses as a king does; the living don't");
			for (int i = 0; i < 3; i++) spawned.add(helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(3 + i, 2, 4)));
			Haunting.Villages.get(level.getServer()).clear(level, spawned.get(0).blockPosition()); // the kept test world's record of this square, from a run before
			long now = level.getGameTime();
			NetherworldState s = c.netherworld();
			helper.assertTrue(Haunting.frighten(ghost, level, now) == 3 && near(s.pathCultivation(), Haunting.share(3, 0) * GhostMethods.required(0)),
					"Three villagers frightened: their fear 0.06 of the stage (" + s.pathCultivation() + ")");
			helper.assertTrue(Haunting.frighten(ghost, level, now) == 0, "Frightened once a minute at most");
			Haunting.Villages villages = Haunting.Villages.get(level.getServer());
			BlockPos square = spawned.get(0).blockPosition();
			for (int i = 0; i < Haunting.EXORCIST_AT; i++) villages.fright(level, square, now);
			spawned.add(helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(3, 2, 6)));
			Haunting.frighten(ghost, level, now);
			List<CultivatorNpc> exorcists = level.getEntitiesOfClass(CultivatorNpc.class, new AABB(square).inflate(16), npc -> npc.getTags().contains(Haunting.EXORCIST_TAG));
			spawned.addAll(exorcists);
			helper.assertTrue(exorcists.size() == 1 && exorcists.get(0).getMainHandItem().is(ModItems.PEACH_WOOD_SWORD), "Haunted too often: an exorcist sent for");
			ghost.setHealth(ghost.getMaxHealth());
			((com.example.defyingtheheavens.mixin.ServerPlayerAccessor) ghost).dth$setSpawnInvulnerableTime(0); // just joined: vanilla shields it 3 s
			float whole = ghost.getHealth();
			helper.assertTrue(Haunting.exorcised(ghost, level) && ghost.getHealth() < whole, "Its rite burns the haunting ghost (" + whole + " to " + ghost.getHealth() + ")");
			helper.assertTrue(near(Haunting.share(1, 3), Haunting.SHARE / 8), "Fear is worth less as the ghost grows");
			helper.succeed();
		} finally {
			if (!spawned.isEmpty()) Haunting.Villages.get(level.getServer()).clear(level, spawned.get(0).blockPosition());
			spawned.forEach(Entity::discard);
			leave(made);
		}
	}

	// --- Helpers ---

	private static DemonBeast beast(GameTestHelper helper, List<Entity> spawned, BeastKind kind, BlockPos at, int rank) {
		ServerLevel level = helper.getLevel();
		DemonBeast beast = ModEntities.beast(kind).create(level);
		Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(at));
		beast.moveTo(pos.x, pos.y, pos.z, 0, 0);
		beast.setRank(rank);
		beast.setNoAi(true);
		level.addFreshEntity(beast);
		spawned.add(beast);
		return beast;
	}

	/** A survival player in the player list (the helper's mock player is creative, and pays no years), remembered in {@code made}. */
	private static ServerPlayer survivor(GameTestHelper helper, List<ServerPlayer> made, String name) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isCreative() {
				return false;
			}
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		p.setOnGround(true);
		made.add(p);
		return p;
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) {
			MeditationManager.forget(p.getUUID());
			Spells.forget(p.getUUID());
			HeavenMethods.forget(p.getUUID());
			GhostSystem.forget(p.getUUID());
			TwoPaths.forget(p.getUUID());
			FiveElementsCycling.forget(p.getUUID());
			CourtOfTheDead.forget(p.getUUID());
			SoulRiverFeast.forget(p.getUUID());
			Haunting.forget(p.getUUID());
			p.server.getPlayerList().remove(p);
		}
	}
}
