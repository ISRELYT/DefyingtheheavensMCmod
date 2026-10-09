package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SpatialRiftBlockEntity extends BlockEntity {
	public SpatialRiftBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SPATIAL_RIFT, pos, state);
	}
}
