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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import java.util.List;
import java.util.UUID;

/**
 * The bug sweep of 2026-10-30. Vanilla ticks the offhand's stack as "selected" whenever the hotbar's first slot is chosen: a legend too heavy
 * for its holder, held in the offhand so, cleared the main hand and left itself where it was, a copy of it slipping into the pack every tick;
 * an old storage ring there never unfolded. A hopper taking up a legend lying loose counted it lost (to reform at home, its own a hollow
 * echo). And the Sun-Shooting Bow's wait, kept by game time, is forgotten with its world.
 */
public class SweepFiveGameTests implements FabricGameTest {
	/** A player who isn't creative (a mock player says it is), of {@code realm}, with a full pool. */
	private static ServerPlayer survivor(GameTestHelper helper, String name, Realm realm, double x, double z) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isSpectator() { return false; }

			@Override
			public boolean isCreative() { return false; }
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(realm, Stage.EARLY, 0);
		CultivationManager.refresh(p);
		c.setQi(c.maxQi());
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		p.teleportTo(helper.getLevel(), origin.getX() + x, origin.getY() + 2, origin.getZ() + z, -90, 0);
		return p;
	}

	private static void leave(ServerPlayer... players) {
		for (ServerPlayer p : players) {
			if (p != null && p.server.getPlayerList().getPlayer(p.getUUID()) == p) p.server.getPlayerList().remove(p);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void heavyLegendSlipsFromTheOffhandAlone(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer weak = survivor(helper, "test-sweep5-weak", Realm.QI_REFINING, 1.5, 1.5);
		try {
			weak.getInventory().selected = 0;
			weak.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
			weak.getInventory().offhand.set(0, Legends.issue(level.getServer(), Legend.MOYE));
			for (int i = 0; i < 3; i++) weak.getInventory().tick(); // as vanilla ticks it: the offhand's stack "selected" with the first slot
			int moye = 0;
			for (List<ItemStack> part : List.of(weak.getInventory().items, weak.getInventory().armor, weak.getInventory().offhand)) {
				for (ItemStack stack : part) if (stack.is(ModItems.legend(Legend.MOYE))) moye++;
			}
			helper.assertTrue(weak.getMainHandItem().is(Items.IRON_SWORD), "The sword in the main hand kept");
			helper.assertTrue(weak.getOffhandItem().isEmpty() && moye == 1 && !LegendaryWeaponItem.carried(weak, Legend.MOYE).isEmpty(),
					"Below Core Formation, Moye slips from the offhand into the pack: one of it (" + moye + ")");
		} finally {
			leave(weak);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void oldRingInTheOffhandUnfolds(GameTestHelper helper) {
		ServerPlayer p = survivor(helper, "test-sweep5-ring", Realm.QI_REFINING, 1.5, 1.5);
		try {
			p.getInventory().selected = 0;
			p.getInventory().setItem(0, new ItemStack(Items.STICK));
			p.getInventory().offhand.set(0, new ItemStack(ModItems.STORAGE_RING));
			p.getInventory().tick();
			helper.assertTrue(p.getOffhandItem().getItem() instanceof StorageFormationItem && p.getMainHandItem().is(Items.STICK),
					"An old ring in the offhand unfolds into its formation, the stick in the hand kept");
		} finally {
			leave(p);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void hopperTakesUpALegendWithoutLosingIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
		Legends.Entry entry = Legends.get(level.getServer()).entry(Legend.MOYE);
		Legends.State was = entry.state;
		long lostAt = entry.lostAt;
		HopperBlockEntity hopper = null;
		try {
			level.setBlock(at, Blocks.HOPPER.defaultBlockState(), 3);
			hopper = (HopperBlockEntity) level.getBlockEntity(at);
			entry.state = Legends.State.ABROAD;
			ItemEntity lying = new ItemEntity(level, at.getX() + 0.5, at.getY() + 1.2, at.getZ() + 0.5, Legends.issue(level.getServer(), Legend.MOYE));
			level.addFreshEntity(lying);
			helper.assertTrue(HopperBlockEntity.addItem(hopper, lying) && lying.isRemoved(), "The hopper takes Moye up");
			helper.assertTrue(entry.state() == Legends.State.ABROAD && Legends.isTrue(level.getServer(), hopper.getItem(0)),
					"Not lost: the hopper's is the true one (" + entry.state() + ")");
		} finally {
			entry.state = was;
			entry.lostAt = lostAt;
			if (hopper != null) hopper.clearContent();
			level.removeBlock(at, false);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void sunBowsWaitForgottenWithItsWorld(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer archer = survivor(helper, "test-sweep5-archer", Realm.HEAVENLY_BEING, 0.5, 1.5);
		try {
			LegendaryWeaponItem bow = (LegendaryWeaponItem) ModItems.legend(Legend.SUN_BOW);
			ItemStack stack = Legends.issue(level.getServer(), Legend.SUN_BOW);
			archer.setItemSlot(EquipmentSlot.MAINHAND, stack);
			LegendaryWeaponItem.bond(stack, archer);
			archer.setShiftKeyDown(true);
			PlayerCultivation c = CultivationManager.get(archer);
			double full = c.getQi();
			bow.releaseUsing(stack, level, archer, bow.getUseDuration(stack) - 25);
			double once = c.getQi();
			helper.assertTrue(once < full, "Drawn fully and let go sneaking: Falling Sun, its qi spent");
			bow.releaseUsing(stack, level, archer, bow.getUseDuration(stack) - 25);
			helper.assertTrue(c.getQi() == once, "Its sun still gathering: an arrow instead, nothing spent");
			LegendaryWeaponItem.clear(); // as a server stopping does (Legends#clear)
			bow.releaseUsing(stack, level, archer, bow.getUseDuration(stack) - 25);
			helper.assertTrue(c.getQi() < once, "Its world closed, the wait is forgotten: Falling Sun again");
		} finally {
			level.getEntitiesOfClass(AbstractArrow.class, archer.getBoundingBox().inflate(64), a -> a.getOwner() == archer).forEach(AbstractArrow::discard);
			leave(archer);
		}
		helper.succeed();
	}
}
