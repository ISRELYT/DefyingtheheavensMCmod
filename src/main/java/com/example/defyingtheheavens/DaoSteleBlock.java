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
 * A Dao Stele (道碑): a tall tablet of pale stone, three blocks high, carved with one heaven method and standing where its
 * element's qi runs thick (DaoSteleFeature): meditating beside it for 15 seconds teaches the method (HeavenMethods), and beside a
 * stele of the element one practises, meditation on it goes faster. Unbreakable; a touch tells what it teaches. Shaped as a
 * Method Stele is (MethodSteleBlock, whose parts it shares).
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
		if (player instanceof ServerPlayer sp) {
			HeavenMethod method = state.getValue(METHOD);
			// One carved before True Immortal's were kept for the Immortal Realm (Realm#taughtInTheWorld) can't be read.
			if (!method.realm().taughtInTheWorld()) {
				sp.displayClientMessage(Component.translatable(ModLang.MSG_STELE_SEALED, method.getDisplayName()).withStyle(ChatFormatting.GRAY), true);
				return InteractionResult.CONSUME;
			}
			boolean known = CultivationManager.get(sp).heaven().knows(method);
			sp.displayClientMessage(Component.translatable(known ? ModLang.MSG_DAO_STELE_KNOWN : ModLang.MSG_DAO_STELE_TEACHES, method.getDisplayName(),
					method.element().getDisplayName(), method.realm().getDisplayName()).withStyle(ChatFormatting.GOLD), true);
		}
		return InteractionResult.CONSUME;
	}
}
