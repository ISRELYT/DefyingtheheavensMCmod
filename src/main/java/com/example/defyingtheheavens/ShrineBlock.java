package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A shrine (Shrines): its look by what its placer is worshipped as ({@link #KIND}: a jade statue for the heaven path's Incense and Faith, a
 * lacquered ancestral tablet for the ghost path's, a beast idol for Beast Deity Worship), its placer its owner. Right-clicked: incense
 * burned, a request answered, or what it holds told (Shrines#use). Broken by any hand but its owner's, or blown apart, it is desecrated and
 * wounds its owner (Shrines#removed).
 */
public class ShrineBlock extends BaseEntityBlock {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<Shrines.Kind> KIND = EnumProperty.create("kind", Shrines.Kind.class);
	private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 15, 15);

	public ShrineBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(KIND, Shrines.Kind.HEAVEN));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, KIND);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Player player = context.getPlayer();
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
				.setValue(KIND, player == null ? Shrines.Kind.HEAVEN : Shrines.kindFor(CultivationManager.forPlayer(player)));
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide || !(placer instanceof ServerPlayer player) || !(level.getBlockEntity(pos) instanceof ShrineBlockEntity shrine)) return;
		shrine.setOwner(player.getUUID(), player.getGameProfile().getName());
		Shrines.Kind kind = Shrines.kindFor(CultivationManager.get(player));
		if (state.getValue(KIND) != kind) level.setBlock(pos, state.setValue(KIND, kind), Block.UPDATE_ALL);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ShrineBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.SHRINE, Shrines::tick);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide) return InteractionResult.SUCCESS;
		if (!(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof ShrineBlockEntity shrine)) return InteractionResult.PASS;
		return Shrines.use(state, sp, shrine, hand);
	}

	@Override
	public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (level.getBlockEntity(pos) instanceof ShrineBlockEntity shrine) shrine.breaker = player.getUUID();
		super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof ShrineBlockEntity shrine) {
			Shrines.removed(level, pos, shrine);
		}
		super.onRemove(state, level, pos, newState, moved);
	}
}
