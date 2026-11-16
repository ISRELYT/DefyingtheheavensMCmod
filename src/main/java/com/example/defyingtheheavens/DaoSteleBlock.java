package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Dao Stele (道碑): a tall tablet of pale stone, three blocks high, carved after one heaven art (its look, its element's heart-stone, and the
 * realm of what it teaches) and standing where its element's qi runs thick (DaoSteleFeature). Since 2026-11-16 it teaches a method, any a manual
 * holds, drawn by its place (SteleTeachings): a touch tells which, its path and its volumes; meditating beside it for 7 seconds teaches it, and
 * beside it a path practising that method gathers faster. Unbreakable. Shaped as a Method Stele is (MethodSteleBlock, whose parts it shares).
 */
public class DaoSteleBlock extends HorizontalDirectionalBlock {
	public static final EnumProperty<HeavenMethod> METHOD = EnumProperty.create("method", HeavenMethod.class);
	public static final EnumProperty<MethodSteleBlock.Part> PART = MethodSteleBlock.PART;

	public DaoSteleBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(METHOD, HeavenMethod.values()[0])
				.setValue(PART, MethodSteleBlock.Part.LOWER));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, METHOD, PART);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return ModBlocks.METHOD_STELE.defaultBlockState().setValue(FACING, state.getValue(FACING)).setValue(MethodSteleBlock.PART, state.getValue(PART))
				.getShape(level, pos, context);
	}

	/** No way through (not a whole block, vanilla took it for open ground: a walker would push against it, as at a Formation Core). */
	@Override
	@SuppressWarnings("deprecation")
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) { return false; }

	/** Stands a whole stele of {@code method} with its foot at {@code foot}, its face toward {@code facing}. */
	public static void place(LevelAccessor level, BlockPos foot, HeavenMethod method, Direction facing) {
		BlockState base = ModBlocks.DAO_STELE.defaultBlockState().setValue(METHOD, method).setValue(FACING, facing);
		level.setBlock(foot, base.setValue(PART, MethodSteleBlock.Part.LOWER), 2 | 16);
		level.setBlock(foot.above(), base.setValue(PART, MethodSteleBlock.Part.MIDDLE), 2 | 16);
		level.setBlock(foot.above(2), base.setValue(PART, MethodSteleBlock.Part.UPPER), 2 | 16);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
		if (level.isClientSide) return InteractionResult.SUCCESS;
		// What it teaches, its path and volumes, and where the toucher stands with it (one carved too deep for this world can't be read).
		if (player instanceof ServerPlayer sp) SteleTeachings.tell(sp, state, pos);
		return InteractionResult.CONSUME;
	}
}
