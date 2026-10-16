package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * The Storage Ring (StorageRingItem, StorageRings): a grade's room counted in whole stacks, the boundless grade's none, a space
 * saved and kept by the ring's id, the screen's clicks, and refining a ring a grade (the realm, the cost).
 */
public class StorageRingGameTests implements FabricGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		return player;
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void gradesHoldStacksAndTheLastHoldsWithoutEnd(GameTestHelper helper) {
		StorageRings.Space space = new StorageRings.Space();
		StorageRings.Grade mortal = StorageRings.Grade.MORTAL;
		int moved = 0;
		for (int i = 0; i < 30; i++) moved += space.insert(new ItemStack(Items.COBBLESTONE, 64), mortal);
		helper.assertTrue(moved == 27 * 64 && space.stacksUsed() == 27, "A Mortal ring holds 27 stacks: " + moved);
		helper.assertTrue(space.insert(new ItemStack(Items.DIRT), mortal) == 0, "...and nothing more of another kind");

		StorageRings.Space kinds = new StorageRings.Space();
		kinds.insert(new ItemStack(Items.DIRT, 1), mortal);
		kinds.insert(new ItemStack(Items.ENDER_PEARL, 17), mortal);
		helper.assertTrue(kinds.stacksUsed() == 3, "Each kind takes whole stacks of its own size (16 pearls a stack)");
		helper.assertTrue(kinds.room(new ItemStack(Items.ENDER_PEARL), mortal) == 15 + 24 * 16L, "Room: the partial stack, then the free stacks");
		ItemStack pearls = kinds.extract(new ItemStack(Items.ENDER_PEARL), 16);
		helper.assertTrue(pearls.is(Items.ENDER_PEARL) && pearls.getCount() == 16 && kinds.count(new ItemStack(Items.ENDER_PEARL)) == 1, "Taken out");

		StorageRings.Space boundless = new StorageRings.Space();
		for (int i = 0; i < 2000; i++) boundless.insert(new ItemStack(Items.COBBLESTONE, 64), StorageRings.Grade.BOUNDLESS);
		int kindsIn = 0;
		for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			if (kindsIn >= 400 || item == Items.AIR || item == Items.COBBLESTONE) continue;
			if (boundless.insert(new ItemStack(item), StorageRings.Grade.BOUNDLESS) > 0) kindsIn++;
		}
		helper.assertTrue(boundless.count(new ItemStack(Items.COBBLESTONE)) == 2000L * 64 && boundless.entries().size() == 401,
				"A Boundless ring holds any number of any number of kinds");
		helper.assertTrue(boundless.insert(new ItemStack(ModItems.STORAGE_RING), StorageRings.Grade.BOUNDLESS) == 0, "No ring folded inside a ring");

		StorageRings data = new StorageRings();
		java.util.UUID id = java.util.UUID.randomUUID();
		data.space(id).insert(new ItemStack(Items.DIAMOND, 40), StorageRings.Grade.SPIRIT);
		StorageRings loaded = StorageRings.load(data.save(new net.minecraft.nbt.CompoundTag()));
		helper.assertTrue(loaded.space(id).count(new ItemStack(Items.DIAMOND)) == 40, "A space is saved with the world, by its ring's id");
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void theScreenTakesAndPuts(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ItemStack ring = StorageRingItem.of(StorageRings.Grade.MORTAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, ring);
		ring.use(player.level(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.containerMenu instanceof StorageRingMenu menu && StorageRingItem.idOf(ring) != null
				&& StorageRingItem.idOf(ring).equals(menu.ringId()), "Right-click opens the ring's space, giving it an id");
		StorageRingMenu menu = (StorageRingMenu) player.containerMenu;
		StorageRings.Space space = StorageRings.forServer(player.server).space(menu.ringId());

		menu.setCarried(new ItemStack(Items.IRON_INGOT, 50));
		StorageRings.act(player, StorageRings.PUT_ONE, ItemStack.EMPTY);
		helper.assertTrue(space.count(new ItemStack(Items.IRON_INGOT)) == 1 && menu.getCarried().getCount() == 49, "Right-click puts one in");
		StorageRings.act(player, StorageRings.PUT_ALL, ItemStack.EMPTY);
		helper.assertTrue(space.count(new ItemStack(Items.IRON_INGOT)) == 50 && menu.getCarried().isEmpty(), "Left-click puts all in");
		StorageRings.act(player, StorageRings.TAKE_HALF, new ItemStack(Items.IRON_INGOT));
		helper.assertTrue(menu.getCarried().getCount() == 25 && space.count(new ItemStack(Items.IRON_INGOT)) == 25, "Right-click takes half a stack's worth");
		menu.setCarried(ItemStack.EMPTY);
		StorageRings.act(player, StorageRings.TAKE_TO_PACK, new ItemStack(Items.IRON_INGOT));
		helper.assertTrue(player.getInventory().countItem(Items.IRON_INGOT) == 25 && space.count(new ItemStack(Items.IRON_INGOT)) == 0,
				"Shift-click sends it to the pack");

		int slot = 9;
		player.getInventory().setItem(slot, new ItemStack(Items.GOLD_INGOT, 30));
		menu.quickMoveStack(player, 0); // the menu's first slot is the pack's slot 9
		helper.assertTrue(space.count(new ItemStack(Items.GOLD_INGOT)) == 30 && player.getInventory().getItem(slot).isEmpty(),
				"Shift-clicking the pack puts the stack in");
		helper.assertTrue(!menu.slots.get(27 + player.getInventory().selected).mayPickup(player), "The open ring stays where it is");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.assertTrue(!menu.stillValid(player), "Gone from the player, its screen closes");
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void refiningWantsTheRealmAndTheCost(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ItemStack ring = StorageRingItem.of(StorageRings.Grade.MORTAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, ring);
		PlayerCultivation c = CultivationManager.get(player);
		helper.assertTrue(StorageRings.refineRefusal(player, ring) != null && !StorageRings.refine(player, ring), "A mortal can't refine a ring");
		c.setState(Realm.FOUNDATION_BUILDING, Stage.EARLY, 0);
		helper.assertTrue(StorageRings.refineRefusal(player, ring) != null, "...nor anyone without the cost");
		player.getInventory().add(new ItemStack(ModItems.LOW_SPIRIT_STONE, 20));
		player.getInventory().add(new ItemStack(Items.ENDER_PEARL, 4));
		helper.assertTrue(StorageRings.refine(player, ring) && StorageRingItem.grade(ring) == StorageRings.Grade.SPIRIT,
				"Foundation Building, the stones and pearls: a Spirit grade ring");
		helper.assertTrue(player.getInventory().countItem(ModItems.LOW_SPIRIT_STONE) == 4 && player.getInventory().countItem(Items.ENDER_PEARL) == 0,
				"The cost is taken");
		player.getInventory().add(new ItemStack(ModItems.MID_SPIRIT_STONE, 16));
		player.getInventory().add(new ItemStack(ModItems.JADE, 8));
		helper.assertTrue(!StorageRings.refine(player, ring), "Earth grade wants Core Formation");
		helper.succeed();
	}
}
