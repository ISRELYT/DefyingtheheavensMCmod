package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SpiritPeachGameTests implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE)
    public void eatingFruitLeavesAPit(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        ItemStack left = CultivationFruitItem.create(40).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(left.is(ModItems.PEACH_PIT), "Eating a fruit leaves its pit in your hand");
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
