package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * A bronze alchemy cauldron, where pills are brewed (see {@link AlchemyCauldronBlockEntity} and {@link AlchemyRecipes}):
 * fill it with a water bucket, light a fire beneath it (fire, soul fire, a lit campfire, lava or a magma block), and add the
 * ingredients one by one. Once they make a recipe the water turns jade and the brew begins; the pill pops out when it's done,
 * taking the water with it.
 */
public class AlchemyCauldronBlock extends BaseEntityBlock {
	public static final BooleanProperty FILLED = BooleanProperty.create("filled");
	public static final BooleanProperty BREWING = BooleanProperty.create("brewing");

	// The vanilla cauldron's shape: open on top, with feet.
	private static final VoxelShape INSIDE = box(2.0, 4.0, 2.0, 14.0, 16.0, 14.0);
	private static final VoxelShape SHAPE = Shapes.join(Shapes.block(), Shapes.or(box(0.0, 0.0, 4.0, 16.0, 3.0, 12.0),
			box(4.0, 0.0, 0.0, 12.0, 3.0, 16.0), box(2.0, 0.0, 2.0, 14.0, 3.0, 14.0), INSIDE), BooleanOp.ONLY_FIRST);
	/** Height of the water's surface, as a fraction of the block (the full cauldron model's water). */
	public static final double SURFACE = 15.0 / 16.0;

	public AlchemyCauldronBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FILLED, false).setValue(BREWING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FILLED, BREWING);
	}

	/** Something hot enough to brew over sits right below. */
	public static boolean isHeated(Level level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		return below.is(Blocks.FIRE) || below.is(Blocks.SOUL_FIRE) || below.is(Blocks.LAVA) || below.is(Blocks.MAGMA_BLOCK)
				|| CampfireBlock.isLitCampfire(below);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide) return InteractionResult.SUCCESS;
		if (level.getBlockEntity(pos) instanceof AlchemyCauldronBlockEntity cauldron) {
			return cauldron.interact(state, player, hand);
		}
		return InteractionResult.PASS;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AlchemyCauldronBlockEntity cauldron) {
			cauldron.dropContents(); // breaking the cauldron gives the ingredients back
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	/** Bubbles while the water is over a fire; while brewing, sparks of qi rise out of it too. Client only. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(FILLED) || !isHeated(level, pos)) return;
		double y = pos.getY() + SURFACE;
		boolean brewing = state.getValue(BREWING);
		for (int i = 0; i < (brewing ? 3 : 1); i++) {
			double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
			double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
			level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0, 0.02, 0);
		}
		if (brewing) {
			if (random.nextInt(3) == 0) {
				level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5, y,
						pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5, 0, 0.03, 0);
			}
			if (random.nextInt(10) == 0) {
				level.playLocalSound(pos.getX() + 0.5, y, pos.getZ() + 0.5, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.5f, 0.8f + random.nextFloat() * 0.4f, false);
			}
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> lines, TooltipFlag flag) {
		lines.add(Component.translatable(ModLang.CAULDRON_TOOLTIP).withStyle(ChatFormatting.GRAY));
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return INSIDE;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	/** BaseEntityBlock hides its model by default; the cauldron is an ordinary model. */
	@Override
	@SuppressWarnings("deprecation")
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new AlchemyCauldronBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.ALCHEMY_CAULDRON, AlchemyCauldronBlockEntity::serverTick);
	}
}
