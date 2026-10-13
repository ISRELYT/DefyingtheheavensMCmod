package com.example.defyingtheheavens;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Other-Shore Flower (彼岸花): the red spider lily that grows where the dead walk, as a herb that ages like ginseng (see
 * GinsengBlock) and glows faintly. It roots in soul soil and soul sand (and ordinary soil, planted there); wild, it grows
 * in the Fields of the Other Shore, here and there across the Netherworld, and rarely in a Soul Sand Valley.
 */
public class OtherShoreFlowerBlock extends GinsengBlock {
	public OtherShoreFlowerBlock(Supplier<Item> harvest, Supplier<Item> seeds, Properties properties) {
		super(harvest, seeds, properties);
	}

	/** Soul soil or soul sand, or ordinary soil. */
	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		return below.is(Blocks.SOUL_SOIL) || below.is(Blocks.SOUL_SAND) || below.is(BlockTags.DIRT);
	}
}
