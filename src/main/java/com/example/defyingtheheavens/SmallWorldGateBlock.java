package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A small world's gate (SmallWorlds, phase 3d): a sheet of starlight in its frame of jade by the world's way in, the way out for anyone in
 * it: walked into, they go out where they came in (SmallWorlds#atGate, at the server's next task). Drawn by the client's block entity
 * renderer (SmallWorldGateRenderer: the stars of the void, as an end gateway's); no hitbox, no item, taken by no one.
 */
public class SmallWorldGateBlock extends BaseEntityBlock {
	public SmallWorldGateBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SmallWorldGateBlockEntity(pos, state);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (!level.isClientSide() && entity instanceof ServerPlayer player) SmallWorlds.atGate(player);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(3) != 0) return;
		double x = pos.getX() + random.nextDouble(), y = pos.getY() + random.nextDouble(), z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
		level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.01, (random.nextDouble() - 0.5) * 0.04);
	}
}
