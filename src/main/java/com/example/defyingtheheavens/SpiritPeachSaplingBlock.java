package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/**
 * A Spirit Peach sapling, planted from a peach pit. It grows like a vanilla sapling (in light, over a while, or faster
 * with bone meal), but into a Spirit Peach Tree built by SpiritPeachTree rather than from a configured feature.
 */
public class SpiritPeachSaplingBlock extends SaplingBlock {
    /** Vanilla's tree growers name a configured feature; this tree is built in code, so it has none. */
    private static final AbstractTreeGrower NO_FEATURE = new AbstractTreeGrower() {
        @Override
        protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean flowers) {
            return null;
        }
    };

    public SpiritPeachSaplingBlock(Properties properties) {
        super(NO_FEATURE, properties);
    }

    /** First a growth stage (as vanilla), then the tree. If there's no room it stays a sapling and tries again later. */
    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (state.getValue(STAGE) == 0) {
            level.setBlock(pos, state.cycle(STAGE), 4);
        } else {
            SpiritPeachTree.grow(level, pos, random);
        }
    }

    /** Pick-block gives the pit it grows from. */
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.PEACH_PIT);
    }
}
