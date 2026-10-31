package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The bodies of the dead (Corpses, the user's rules 2026-10-31): a death's drops lie in its body, broken it spills them, it rots to bones in
 * minutes and the bones stay; a fresh cultivator's body bled once; the yin round a body. Bodies are asked for here ({@link Corpses#inTests})
 * and taken up again after, so the kept test world holds none.
 */
public class CorpseGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void aPlayerLiesWithWhatTheyCarried(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		BlockPos at = null;
		Corpses.inTests = true;
		try {
			ServerPlayer p = survivor(helper, made, "corpse-player");
			p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
			p.getInventory().setItem(5, new ItemStack(Items.GOLD_INGOT, 3));
			p.kill();
			at = bodyNear(helper.getLevel(), p.blockPosition());
			helper.assertTrue(at != null, "A body lies where the player fell");
			CorpseBlockEntity body = (CorpseBlockEntity) helper.getLevel().getBlockEntity(at);
			helper.assertTrue(body.name().equals("corpse-player") && count(body.held(), Items.DIAMOND) == 7 && count(body.held(), Items.GOLD_INGOT) == 3,
					"...holding what they carried (" + body.held() + ")");
			helper.assertTrue(items(helper.getLevel(), at).isEmpty(), "...and none of it scattered");
			helper.getLevel().destroyBlock(at, false);
			at = null;
			List<ItemEntity> spilt = items(helper.getLevel(), p.blockPosition());
			helper.assertTrue(spilt.stream().mapToInt(i -> i.getItem().is(Items.DIAMOND) ? i.getItem().getCount() : 0).sum() == 7,
					"Broken, it spills what it held");
			spilt.forEach(ItemEntity::discard);
			helper.succeed();
		} finally {
			Corpses.inTests = false;
			takeUp(helper.getLevel(), at);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBodyRotsToBonesThatStay(GameTestHelper helper) {
		BlockPos at = null;
		CultivatorNpc npc = null;
		Corpses.inTests = true;
		try {
			npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(3, 2, 2));
			npc.setRank(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY));
			npc.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
			npc.setDropChance(EquipmentSlot.MAINHAND, 2.0f);
			BlockPos fell = npc.blockPosition();
			npc.kill();
			at = bodyNear(helper.getLevel(), fell);
			helper.assertTrue(at != null, "A cultivator leaves its body");
			ServerLevel level = helper.getLevel();
			CorpseBlockEntity body = (CorpseBlockEntity) level.getBlockEntity(at);
			helper.assertTrue(count(body.held(), Items.IRON_SWORD) == 1 && body.rank() == PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY),
					"...its sword in it, and its realm known");
			helper.assertTrue(level.getBlockState(at).getValue(CorpseBlock.STAGE) == CorpseBlock.Rot.FRESH, "Fresh at first");
			RandomSource random = level.getRandom();
			body.backdate(Corpses.FRESH_TICKS);
			level.getBlockState(at).tick(level, at, random);
			helper.assertTrue(level.getBlockState(at).getValue(CorpseBlock.STAGE) == CorpseBlock.Rot.ROTTING, "Rotting after two minutes");
			body.backdate(Corpses.BONES_AT);
			level.getBlockState(at).tick(level, at, random);
			BlockState bones = level.getBlockState(at);
			helper.assertTrue(bones.is(ModBlocks.CORPSE) && bones.getValue(CorpseBlock.STAGE) == CorpseBlock.Rot.BONES, "Bones after five");
			body.backdate(1_000_000);
			level.getBlockState(at).tick(level, at, random);
			helper.assertTrue(level.getBlockState(at).is(ModBlocks.CORPSE), "...and the bones stay");
			level.destroyBlock(at, false);
			at = null;
			List<ItemEntity> spilt = items(level, fell);
			helper.assertTrue(spilt.stream().anyMatch(i -> i.getItem().is(Items.IRON_SWORD))
							&& spilt.stream().mapToInt(i -> i.getItem().is(Items.BONE) ? i.getItem().getCount() : 0).sum() == Corpses.BONES_DROPPED,
					"Broken, the bones spill the sword, and bones");
			spilt.forEach(ItemEntity::discard);
			helper.succeed();
		} finally {
			Corpses.inTests = false;
			takeUp(helper.getLevel(), at);
			if (npc != null) npc.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aFreshBodyIsBledOnce(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		BlockPos at = null;
		CultivatorNpc npc = null;
		Corpses.inTests = true;
		try {
			npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(3, 2, 2));
			npc.setRank(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.MID));
			BlockPos fell = npc.blockPosition();
			npc.kill();
			at = bodyNear(helper.getLevel(), fell);
			helper.assertTrue(at != null, "A body to bleed");
			CorpseBlockEntity body = (CorpseBlockEntity) helper.getLevel().getBlockEntity(at);
			ServerPlayer p = survivor(helper, made, "corpse-bleeder");
			p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
			PlayerCultivation c = CultivationManager.own(p);
			int before = c.getAlignment();
			Corpses.use(p, InteractionHand.MAIN_HAND, body, helper.getLevel().getBlockState(at));
			ItemStack blood = p.getInventory().items.stream().filter(s -> s.is(ModItems.CULTIVATORS_BLOOD)).findFirst().orElse(ItemStack.EMPTY);
			helper.assertTrue(!blood.isEmpty() && CultivatorsBloodItem.realm(blood) == Realm.NASCENT_SOUL && p.getMainHandItem().getCount() == 1,
					"A bottle of Nascent Soul blood, a bottle spent");
			helper.assertTrue(c.getAlignment() == before - Corpses.BLEED_SHIFT, "...and the path darkened (" + before + " to " + c.getAlignment() + ")");
			Corpses.use(p, InteractionHand.MAIN_HAND, body, helper.getLevel().getBlockState(at));
			helper.assertTrue(p.getMainHandItem().getCount() == 1 && body.bled(), "Once only");
			helper.succeed();
		} finally {
			Corpses.inTests = false;
			takeUp(helper.getLevel(), at);
			if (npc != null) npc.discard();
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDeadGiveOffYin(GameTestHelper helper) {
		BlockPos at = null;
		CultivatorNpc npc = null;
		Corpses.inTests = true;
		try {
			npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(3, 2, 2));
			int rank = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.LATE);
			npc.setRank(rank);
			BlockPos fell = npc.blockPosition();
			npc.kill();
			at = bodyNear(helper.getLevel(), fell);
			helper.assertTrue(at != null, "A body lies");
			ServerLevel level = helper.getLevel();
			double near = GhostQi.densityAt(level, at.above()), far = GhostQi.densityAt(level, at.offset(Corpses.yinRadius(rank) + 3, 0, 0));
			helper.assertTrue(Math.abs(near - (GhostQi.OVERWORLD + Corpses.yin(rank, false))) < 1e-9 && Math.abs(far - GhostQi.OVERWORLD) < 1e-9,
					"Its yin beside it, none past its reach (" + near + ", " + far + ")");
			CorpseBlockEntity body = (CorpseBlockEntity) level.getBlockEntity(at);
			body.backdate(Corpses.BONES_AT);
			level.getBlockState(at).tick(level, at, level.getRandom());
			double bones = GhostQi.densityAt(level, at.above());
			helper.assertTrue(Math.abs(bones - (GhostQi.OVERWORLD + Corpses.yin(rank, true))) < 1e-9, "Bones give off half (" + bones + ")");
			helper.succeed();
		} finally {
			Corpses.inTests = false;
			takeUp(helper.getLevel(), at);
			if (npc != null) npc.discard();
		}
	}

	// --- Helpers ---

	/** The body nearest {@code pos} within a few blocks (it may have fallen to the ground under it), or null. */
	private static BlockPos bodyNear(ServerLevel level, BlockPos pos) {
		for (BlockPos p : BlockPos.withinManhattan(pos, 3, 4, 3)) if (level.getBlockState(p).is(ModBlocks.CORPSE)) return p.immutable();
		return null;
	}

	private static int count(List<ItemStack> stacks, net.minecraft.world.item.Item item) {
		return stacks.stream().mapToInt(s -> s.is(item) ? s.getCount() : 0).sum();
	}

	private static List<ItemEntity> items(ServerLevel level, BlockPos pos) {
		return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4));
	}

	/** A body left by a test taken up whole: what it held taken out first, so nothing spills into the kept world. */
	private static void takeUp(ServerLevel level, BlockPos at) {
		if (at == null || !level.getBlockState(at).is(ModBlocks.CORPSE)) return;
		if (level.getBlockEntity(at) instanceof CorpseBlockEntity body) body.takeAll();
		level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
		items(level, at).forEach(ItemEntity::discard);
	}

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

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) p.server.getPlayerList().remove(p);
	}
}
