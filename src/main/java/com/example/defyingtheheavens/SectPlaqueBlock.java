package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * A Sect Plaque (PlayerSects: the user's "Build as designed", 2026-11-10): a lacquered board with its sect's name in gold, hung on a wall as a
 * sign is. Inside a player's sect's barrier it marks a gate: aspirants come in before it, and its gate guards stand either side (PlayerSects#lay).
 * Its FACING is the way its face looks, away from the wall it hangs on.
 */
public class SectPlaqueBlock extends HorizontalDirectionalBlock {
	private static final VoxelShape NORTH = Block.box(1, 3, 15, 15, 13, 16), SOUTH = Block.box(1, 3, 0, 15, 13, 1),
			EAST = Block.box(0, 3, 1, 1, 13, 15), WEST = Block.box(15, 3, 1, 16, 13, 15);

	public SectPlaqueBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case SOUTH -> SOUTH;
			case EAST -> EAST;
			case WEST -> WEST;
			default -> NORTH;
		};
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty(); // a sign's board: walked past, not into
	}

	/** Hung on the wall it is put against (the face looked at), or another wall beside it; never on a floor or a ceiling. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		for (Direction looking : context.getNearestLookingDirections()) {
			if (!looking.getAxis().isHorizontal()) continue;
			BlockState state = defaultBlockState().setValue(FACING, looking.getOpposite());
			if (state.canSurvive(context.getLevel(), context.getClickedPos())) return state;
		}
		return null;
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockPos wall = pos.relative(facing.getOpposite());
		return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
	}

	/** Its wall gone, it falls. */
	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
		return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
	}

	@Override
	public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(ModLang.TOOLTIP_SECT_PLAQUE).withStyle(ChatFormatting.GRAY));
	}
}
