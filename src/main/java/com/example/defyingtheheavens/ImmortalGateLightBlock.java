package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The light that fills the Immortal Gate's opening, and the Southern Heavenly Gate's, while it stands open (ImmortalGate#open; taken
 * up again when it shuts). Walk-through, unbreakable, no item; what steps into it is the Gate's to carry (ImmortalGate, by the
 * opening's bounds: carried off to another realm in the middle of the step that brought one in, the server snaps the walker back,
 * SpatialRiftBlock). Its faces between two of its own are never drawn, so the opening is one sheet of gold.
 */
public class ImmortalGateLightBlock extends Block {
	public ImmortalGateLightBlock(Properties properties) {
		super(properties);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
		return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
	}

	@Override
	public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
		return 1.0f;
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
	}

	/** Now and then a mote of its light drifting up off it. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(12) != 0) return;
		level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + 0.5,
				(random.nextDouble() - 0.5) * 0.02, 0.03 + random.nextDouble() * 0.03, (random.nextDouble() - 0.5) * 0.02);
	}
}
