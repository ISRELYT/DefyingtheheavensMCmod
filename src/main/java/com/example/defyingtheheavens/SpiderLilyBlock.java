package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The red spider lily that carpets the Fields of the Other Shore: an ordinary flower, as the soil of the dead grows it (soul
 * soil and soul sand, and any earth). The aged, spirit-touched kind is a herb of its own (the Other-Shore Flower).
 */
public class SpiderLilyBlock extends FlowerBlock {
	public SpiderLilyBlock(Properties properties) {
		super(MobEffects.BLINDNESS, 8, properties);
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.is(Blocks.SOUL_SOIL) || state.is(Blocks.SOUL_SAND) || super.mayPlaceOn(state, level, pos);
	}
}
