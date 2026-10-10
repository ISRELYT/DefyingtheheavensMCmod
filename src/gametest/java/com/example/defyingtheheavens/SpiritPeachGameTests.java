package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class SpiritPeachGameTests implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE)
    public void eatingFruitSometimesLeavesAPit(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        int eaten = 2000, pits = 0;
        for (int i = 0; i < eaten; i++) {
            ItemStack left = CultivationFruitItem.create(40).finishUsingItem(helper.getLevel(), player);
            helper.assertTrue(left.isEmpty() || left.is(ModItems.PEACH_PIT), "A fruit leaves nothing or its pit");
            if (left.is(ModItems.PEACH_PIT)) pits++;
        }
        // One in 8 is 250 of 2000; this allows about seven standard deviations either way.
        helper.assertTrue(pits >= 150 && pits <= 350, "About one fruit in " + CultivationFruitItem.PIT_CHANCE + " leaves a pit (" + pits + " of " + eaten + ")");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void saplingGrowsIntoATree(GameTestHelper helper) {
        BlockPos at = new BlockPos(3, 2, 3);
        helper.setBlock(at.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(at, ModBlocks.SPIRIT_PEACH_SAPLING);
        BlockPos pos = helper.absolutePos(at);
        SpiritPeachSaplingBlock sapling = (SpiritPeachSaplingBlock) ModBlocks.SPIRIT_PEACH_SAPLING;
        for (int i = 0; i < 2; i++) {
            BlockState state = helper.getLevel().getBlockState(pos);
            if (state.is(ModBlocks.SPIRIT_PEACH_SAPLING)) sapling.advanceTree(helper.getLevel(), pos, state, helper.getLevel().random);
        }
        helper.assertBlockPresent(ModBlocks.SPIRIT_PEACH_HEART, at);
        helper.assertBlockPresent(Blocks.CHERRY_LOG, at.above());
        boolean leaves = false;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, 2, -2), pos.offset(2, 7, 2))) {
            leaves |= helper.getLevel().getBlockState(p).is(ModBlocks.SPIRIT_PEACH_LEAVES);
        }
        helper.assertTrue(leaves, "The tree has a canopy of Spirit Peach leaves");
        SpiritPeachHeartBlockEntity heart = (SpiritPeachHeartBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(heart.age() == 1, "A new tree starts at 1 year");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void brokenSaplingGivesBackItsPit(GameTestHelper helper) {
        BlockPos at = new BlockPos(3, 2, 3);
        helper.setBlock(at.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(at, ModBlocks.SPIRIT_PEACH_SAPLING);
        helper.getLevel().destroyBlock(helper.absolutePos(at), true);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(at)).inflate(1));
        helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(ModItems.PEACH_PIT) && drops.get(0).getItem().getCount() == 1,
                "Breaking a sapling gives back the peach pit it grew from");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void feedingAgesOnlyThatTreesFruit(GameTestHelper helper) {
        // Two hearts four blocks apart, so each one's canopy reaches the other's fruit, and a wild fruit by the first.
        BlockPos heartA = new BlockPos(1, 1, 3), heartB = new BlockPos(5, 1, 3);
        helper.setBlock(heartA, ModBlocks.SPIRIT_PEACH_HEART);
        helper.setBlock(heartB, ModBlocks.SPIRIT_PEACH_HEART);
        BlockPos fruitA = new BlockPos(2, 4, 3), fruitB = new BlockPos(4, 4, 3), wild = new BlockPos(1, 4, 4);
        hang(helper, fruitA, ModBlocks.SPIRIT_PEACH_LEAVES);
        hang(helper, fruitB, ModBlocks.SPIRIT_PEACH_LEAVES);
        hang(helper, wild, Blocks.OAK_LEAVES);
        var ofA = ((SpiritPeachHeartBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(heartA))).fruit();
        var ofB = ((SpiritPeachHeartBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(heartB))).fruit();
        helper.assertTrue(ofA.size() == 1 && ofA.get(0).getBlockPos().equals(helper.absolutePos(fruitA)),
                "A tree feeds only the fruit nearest it, not its neighbour's or wild fruit under other leaves");
        helper.assertTrue(ofB.size() == 1 && ofB.get(0).getBlockPos().equals(helper.absolutePos(fruitB)),
                "And its neighbour only its own");
        helper.succeed();
    }

    /** A fruit hanging under a block of {@code leaves}. */
    private static void hang(GameTestHelper helper, BlockPos fruit, Block leaves) {
        helper.setBlock(fruit.above(), leaves.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        helper.setBlock(fruit, ModBlocks.CULTIVATION_FRUIT);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void qiBuysYearsAtARisingCost(GameTestHelper helper) {
        for (int age : new int[] {1, 100, 750, 5_000, 9_000}) {
            for (int years : new int[] {1, 7, 40}) {
                helper.assertTrue(SpiritPeachHeartBlockEntity.yearsFor(age, SpiritPeachHeartBlockEntity.qiCost(age, years)) == years,
                        "The qi a number of years costs buys exactly that many years (age " + age + ", " + years + " years)");
            }
        }
        helper.assertTrue(SpiritPeachHeartBlockEntity.qiCost(5_000, 1) > SpiritPeachHeartBlockEntity.qiCost(100, 1) * 10,
                "An old tree's year costs far more than a young tree's");
        helper.assertTrue(SpiritPeachHeartBlockEntity.yearsFor(FruitAge.MAX_YEARS - 1, 1.0e12) == 1, "Feeding never passes 10,000 years");
        helper.succeed();
    }
}
