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
import net.minecraft.world.item.context.UseOnContext;
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
        helper.assertTrue(root.is(ModItems.GINSENG) && root.getCount() >= 1 && root.getCount() <= 3, "Digging up gives one to three roots");
        helper.assertTrue(GinsengItem.age(root) == 400, "Digging up keeps the age");
        ItemStack seeds = player.getInventory().getItem(1);
        helper.assertTrue(seeds.is(ModItems.GINSENG_SEEDS) && seeds.getCount() >= GinsengBlock.MIN_SEEDS && seeds.getCount() <= GinsengBlock.MAX_SEEDS,
                "A full-grown plant gives seeds");
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
        helper.assertTrue(player.getInventory().getItem(1).is(ModItems.SPIRIT_GINSENG_SEEDS), "And gives Spirit Ginseng seeds");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void removingSoilDropsAgedGinseng(GameTestHelper helper) {
        plant(helper, ModBlocks.GINSENG, 1_000);
        helper.setBlock(PLANT.below(), Blocks.AIR);
        helper.assertBlockPresent(Blocks.AIR, PLANT);
        int roots = 0, seeds = 0;
        for (ItemEntity drop : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(PLANT)).inflate(1))) {
            ItemStack stack = drop.getItem();
            if (stack.is(ModItems.GINSENG)) {
                helper.assertTrue(GinsengItem.age(stack) == 1_000, "Dropped roots keep their age");
                roots += stack.getCount();
            } else if (stack.is(ModItems.GINSENG_SEEDS)) {
                seeds += stack.getCount();
            } else {
                helper.fail("Unexpected drop " + stack);
            }
        }
        helper.assertTrue(roots >= 1 && roots <= 3 && seeds >= 1 && seeds <= 3, "Losing its soil drops its roots and seeds");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void youngGinsengGivesOneRootAndNoSeeds(GameTestHelper helper) {
        GinsengBlock block = (GinsengBlock) ModBlocks.GINSENG;
        for (int i = 0; i < 50; i++) {
            var harvest = block.harvested(GinsengBlock.MATURE_YEARS - 1, helper.getLevel().random);
            helper.assertTrue(harvest.size() == 1 && harvest.get(0).getCount() == 1, "Before its 10th year: one root, no seeds");
        }
        int extra = 0;
        for (int i = 0; i < 400; i++) {
            var harvest = block.harvested(GinsengBlock.MATURE_YEARS, helper.getLevel().random);
            helper.assertTrue(harvest.size() == 2 && harvest.get(0).getCount() <= 3, "Full grown: up to three roots and some seeds");
            if (harvest.get(0).getCount() > 1) extra++;
        }
        helper.assertTrue(extra > 80 && extra < 200, "About a third of full-grown plants give an extra root (" + extra + "/400)");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void seedsPlantASeedlingThatGrowsUp(GameTestHelper helper) {
        helper.setBlock(PLANT.below(), Blocks.GRASS_BLOCK);
        Player player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GINSENG_SEEDS));
        BlockPos soil = helper.absolutePos(PLANT.below());
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(soil).add(0, 0.5, 0), Direction.UP, soil, false)));
        helper.assertBlockPresent(ModBlocks.GINSENG, PLANT);
        BlockPos pos = helper.absolutePos(PLANT);
        GinsengBlockEntity ginseng = (GinsengBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(ginseng.age() == 1 && helper.getLevel().getBlockState(pos).getValue(GinsengBlock.STAGE) == 0,
                "Seeds plant a 1-year seedling");
        ginseng.setAge(GinsengBlock.YOUNG_YEARS);
        helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(GinsengBlock.STAGE) == 1, "It becomes a young plant");
        ginseng.setAge(GinsengBlock.MATURE_YEARS);
        helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(GinsengBlock.STAGE) == 2
                && helper.getLevel().getBlockEntity(pos) == ginseng, "And full grown at 10 years, the same plant");
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
