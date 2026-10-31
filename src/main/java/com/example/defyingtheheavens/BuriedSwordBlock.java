package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A sword driven into the ground (the Sunken Sword Tomb's floor is strewn with them, SwordTombLand): whole, or broken off short; standing
 * straight or leaning ({@link #TILTED}); one walks through them as through grass. An <b>old sword</b> ({@link #RELIC}) still holds something
 * of its master's: it glints now and then, and one who draws it out (right-click, SwordTombWorks#drawn) takes what it keeps, waking the
 * wraiths about it; drawn, it is a broken sword like the rest till the tomb next stirs.
 */
public class BuriedSwordBlock extends HorizontalDirectionalBlock {
	public static final BooleanProperty BROKEN = BooleanProperty.create("broken");
	public static final BooleanProperty TILTED = BooleanProperty.create("tilted");
	public static final BooleanProperty RELIC = BooleanProperty.create("relic");
	/** Its outline: a whole sword stands a block and a half (so the gorge's swords are seen from afar), a broken one under a block. */
	private static final VoxelShape WHOLE = Block.box(5, 0, 5, 11, 24, 11), SHORT = Block.box(5, 0, 5, 11, 11, 11);

	public BuriedSwordBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BROKEN, false).setValue(TILTED, false).setValue(RELIC, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, BROKEN, TILTED, RELIC);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(BROKEN) ? SHORT : WHOLE;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		return direction == Direction.DOWN && !canSurvive(state, level, pos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
				: super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
	}

	/** An old sword drawn out: what it keeps is the drawer's (SwordTombWorks), and it is broken like the rest. */
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!state.getValue(RELIC) || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
		if (level instanceof ServerLevel server && player instanceof ServerPlayer p) SwordTombWorks.drawn(server, pos, p);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	/** An old sword glints now and then; the tomb's shrine's sword shines. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(RELIC) || random.nextInt(6) != 0) return;
		double y = pos.getY() + (state.getValue(BROKEN) ? 0.35 : 0.65) + random.nextDouble() * 0.2;
		level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.15, y, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.15,
				0, 0.01, 0);
	}
}
