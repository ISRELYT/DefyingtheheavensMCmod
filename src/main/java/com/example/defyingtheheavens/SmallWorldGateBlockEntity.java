package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A small world's gate (SmallWorldGateBlock): no data, only drawn by its renderer. */
public class SmallWorldGateBlockEntity extends BlockEntity {
	public SmallWorldGateBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SMALL_WORLD_GATE, pos, state);
	}
}
