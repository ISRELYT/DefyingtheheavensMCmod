package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The light that fills the Immortal Gate's opening, and the Southern Heavenly Gate's, while it stands open (ImmortalGate#open; taken
 * up again when it shuts). Walk-through, unbreakable, no item; what steps into it is the Gate's to carry (ImmortalGate, by the
 * opening's bounds: carried off to another realm in the middle of the step that brought one in, the server snaps the walker back,
 * SpatialRiftBlock). It draws nothing of its own: the client draws the whole opening's light over the cells it fills, one sea of
 * gold (GatePortalRenderer; the user's ask, 2026-10-27, for a better look than a flat sheet of its old texture).
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
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
		return 1.0f;
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
	}
}
