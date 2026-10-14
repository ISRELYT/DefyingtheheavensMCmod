package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Method Stele (功法碑): a tall tablet of carved stone, three blocks high, standing across the Netherworld, each teaching one
 * ghost method (see GhostMethods): meditating beside it for 15 seconds teaches the method, and beside the stele of a method
 * one knows, meditation on that method goes faster. Unbreakable; the dead may use it (tagged ghost_usable): a touch tells what
 * it teaches.
 */
public class MethodSteleBlock extends HorizontalDirectionalBlock {
	public enum Part implements StringRepresentable {
		LOWER, MIDDLE, UPPER;

		@Override
		public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
	}

	public static final EnumProperty<GhostMethod> METHOD = EnumProperty.create("method", GhostMethod.class);
	public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

	private static final VoxelShape NORTH_SOUTH = Block.box(2, 0, 5, 14, 16, 11);
	private static final VoxelShape EAST_WEST = Block.box(5, 0, 2, 11, 16, 14);
	private static final VoxelShape TOP_NORTH_SOUTH = Shapes.or(Block.box(2, 0, 5, 14, 12, 11), Block.box(1, 12, 4, 15, 14, 12));
	private static final VoxelShape TOP_EAST_WEST = Shapes.or(Block.box(5, 0, 2, 11, 12, 14), Block.box(4, 12, 1, 12, 14, 15));

	public MethodSteleBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(METHOD, GhostMethod.NETHERWORLD_BREATHING)
				.setValue(PART, Part.LOWER));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, METHOD, PART);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		boolean eastWest = state.getValue(FACING).getAxis() == Direction.Axis.X;
		if (state.getValue(PART) == Part.UPPER) return eastWest ? TOP_EAST_WEST : TOP_NORTH_SOUTH;
		return eastWest ? EAST_WEST : NORTH_SOUTH;
	}

	/** Stands a whole stele of {@code method} with its foot at {@code foot}, its face toward {@code facing}. */
	public static void place(LevelAccessor level, BlockPos foot, GhostMethod method, Direction facing) {
		BlockState base = ModBlocks.METHOD_STELE.defaultBlockState().setValue(METHOD, method).setValue(FACING, facing);
		level.setBlock(foot, base.setValue(PART, Part.LOWER), 2 | 16);
		level.setBlock(foot.above(), base.setValue(PART, Part.MIDDLE), 2 | 16);
		level.setBlock(foot.above(2), base.setValue(PART, Part.UPPER), 2 | 16);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
		if (level.isClientSide) return InteractionResult.SUCCESS;
		if (player instanceof ServerPlayer sp) {
			GhostMethod method = state.getValue(METHOD);
			boolean known = CultivationManager.get(sp).netherworld().knows(method);
			sp.displayClientMessage(Component.translatable(known ? ModLang.MSG_STELE_KNOWN : ModLang.MSG_STELE_TEACHES, method.getDisplayName())
					.withStyle(ChatFormatting.AQUA), true);
		}
		return InteractionResult.CONSUME;
	}
}
