package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The hub of the Wheel of Reincarnation on the far bank of the Wangchuan, by the Ghost Gate (see GhostGate): unbreakable, and
 * used by the dead to return to life (see GhostSystem). The whole upright six-spoked wheel of blackstone, gold and Yin Jade is
 * drawn turning slowly around it by its block entity renderer.
 */
public class WheelOfReincarnationBlock extends BaseEntityBlock {
	/** The block event that turns the wheel round for a ghost returning to life (its parameter: how many ticks it takes). */
	public static final int EVENT_TURN = 1;

	public WheelOfReincarnationBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WheelOfReincarnationBlockEntity(pos, state);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	/** Passes the turn on to the block entity (both sides: the server sends block events to the clients). */
	@Override
	public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		BlockEntity entity = level.getBlockEntity(pos);
		return entity != null && entity.triggerEvent(id, param);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
		if (level.isClientSide) return InteractionResult.SUCCESS;
		if (player instanceof ServerPlayer sp) GhostSystem.useWheel(sp, pos);
		return InteractionResult.CONSUME;
	}
}
