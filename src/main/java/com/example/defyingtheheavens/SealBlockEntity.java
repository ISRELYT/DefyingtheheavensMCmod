package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Carries no data: it lets the client draw a seal (on the Qi Sense layer) with a block entity renderer. */
public class SealBlockEntity extends BlockEntity {
	public SealBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SEAL, pos, state);
	}
}
