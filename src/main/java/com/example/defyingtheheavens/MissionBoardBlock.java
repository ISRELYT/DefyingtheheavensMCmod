package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A sect's mission board (SectMissions): set up by the sect itself on its plaza (never crafted: its item is the creative menu's alone), two
 * red-lacquered posts with the board between them under a little tiled roof, its missions pinned up on it, its face to the gate. Nothing
 * breaks it, as nothing breaks the treasuries' formations: it is the sect's. Right-clicked, its page (SectMissions#openBoard).
 */
public class MissionBoardBlock extends HorizontalDirectionalBlock {
	/** The board as it stands, posts and all, up under its roof into the block above: across x when it faces north or south, else across z. */
	private static final VoxelShape ACROSS_X = Block.box(0, 0, 6, 16, 28, 10), ACROSS_Z = Block.box(6, 0, 0, 10, 28, 16);

	public MissionBoardBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? ACROSS_X : ACROSS_Z;
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide) return InteractionResult.SUCCESS;
		if (player instanceof ServerPlayer serverPlayer) SectMissions.openBoard(serverPlayer, pos);
		return InteractionResult.CONSUME;
	}
}
