package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class GinsengGameTests implements FabricGameTest {
    private static final BlockPos PLANT = new BlockPos(2, 2, 2);

    private GinsengBlockEntity plant(GameTestHelper helper, Block block, int years) {
        helper.setBlock(PLANT.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(PLANT, block);
        GinsengBlockEntity ginseng = (GinsengBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(PLANT));
        ginseng.setAge(years);
        return ginseng;
    }

    private void use(GameTestHelper helper, Player player, BlockPos at) {
        BlockPos pos = helper.absolutePos(at);
        helper.getLevel().getBlockState(pos).use(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void digUpKeepsAgeAndCannotReplant(GameTestHelper helper) {
        plant(helper, ModBlocks.GINSENG, 400);
        Player player = helper.makeMockPlayer();
        use(helper, player, PLANT);
        helper.assertBlockPresent(Blocks.AIR, PLANT);
        ItemStack root = player.getInventory().getItem(0);
        helper.assertTrue(root.is(ModItems.GINSENG) && root.getCount() == 1, "Digging up gives exactly one ginseng");
        helper.assertTrue(GinsengItem.age(root) == 400, "Digging up keeps the age");
        helper.assertTrue(!(root.getItem() instanceof BlockItem) && !root.isEdible(), "Harvested ginseng can't be replanted or eaten");
        helper.assertTrue(GinsengItem.age(ItemStack.of(root.save(new CompoundTag()))) == 400, "Age survives save/load");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void spiritGinsengDigsUpAsSpiritGinseng(GameTestHelper helper) {
        plant(helper, ModBlocks.SPIRIT_GINSENG, 5_000);
        Player player = helper.makeMockPlayer();
        use(helper, player, PLANT);
        ItemStack root = player.getInventory().getItem(0);
        helper.assertTrue(root.is(ModItems.SPIRIT_GINSENG) && GinsengItem.age(root) == 5_000, "Spirit Ginseng keeps its kind and age");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void removingSoilDropsAgedGinseng(GameTestHelper helper) {
        plant(helper, ModBlocks.GINSENG, 1_000);
        helper.setBlock(PLANT.below(), Blocks.AIR);
        helper.assertBlockPresent(Blocks.AIR, PLANT);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(PLANT)).inflate(1));
        helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(ModItems.GINSENG) && GinsengItem.age(drops.get(0).getItem()) == 1_000,
                "Losing its soil drops exactly one ginseng, age intact");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void ginsengNeedsSoil(GameTestHelper helper) {
        helper.setBlock(PLANT.below(), Blocks.STONE);
        helper.assertTrue(!ModBlocks.GINSENG.defaultBlockState().canSurvive(helper.getLevel(), helper.absolutePos(PLANT)),
                "Ginseng doesn't grow on stone");
        helper.setBlock(PLANT.below(), Blocks.PODZOL);
        helper.assertTrue(ModBlocks.GINSENG.defaultBlockState().canSurvive(helper.getLevel(), helper.absolutePos(PLANT)),
                "Ginseng grows on podzol");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void pedestalHoldsGinseng(GameTestHelper helper) {
        BlockPos at = new BlockPos(5, 2, 5);
        helper.setBlock(at, ModBlocks.SPIRIT_PEDESTAL);
        SpiritPedestalBlockEntity pedestal = (SpiritPedestalBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(at));
        Player player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, GinsengItem.create(ModItems.SPIRIT_GINSENG, 1_000));
        use(helper, player, at);
        helper.assertTrue(pedestal.hasFruit() && pedestal.isHerb() && pedestal.fruitAge() == 1_000, "A pedestal holds ginseng with its age");
        helper.assertTrue(Math.abs(CultivationBoost.pedestalBonus(pedestal.fruitAge()) - 0.38) < 1.0e-9,
                "Ginseng on a pedestal counts like a fruit of the same age");
        use(helper, player, at);
        helper.assertTrue(!pedestal.hasFruit() && player.getMainHandItem().is(ModItems.SPIRIT_GINSENG), "And gives it back");
        helper.succeed();
    }
}
