package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * The Artifact Forge (the user's ask, 2026-10-28: "artifact refining (artifact forge required)"): a bronze furnace on four feet, its mouth
 * glowing while it works. A fire, a lit campfire, lava or magma beneath heats it; materials go in one by one, an empty hand begins the
 * working, and its forger stays by it feeding it qi until the artifact comes out (ArtifactForgeBlockEntity, ArtifactRecipes).
 */
public class ArtifactForgeBlock extends BaseEntityBlock {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty WORKING = BooleanProperty.create("working");

	/** Its four feet, its belly, its lip and its chimney (models/block/artifact_forge). */
	private static final VoxelShape SHAPE = Shapes.or(box(1, 0, 1, 4, 2, 4), box(12, 0, 1, 15, 2, 4), box(1, 0, 12, 4, 2, 15),
			box(12, 0, 12, 15, 2, 15), box(2, 2, 2, 14, 12, 14), box(1, 12, 1, 15, 14, 15), box(5, 14, 5, 11, 16, 11));

	public ArtifactForgeBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WORKING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WORKING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide) return InteractionResult.SUCCESS;
		if (level.getBlockEntity(pos) instanceof ArtifactForgeBlockEntity forge) return forge.interact(state, player, hand);
		return InteractionResult.PASS;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ArtifactForgeBlockEntity forge) {
			forge.dropContents(); // breaking the forge gives the materials back
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	/** Its mouth breathes flame and sparks while it works; a little smoke over plain heat. Client only. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(WORKING)) return;
		Direction facing = state.getValue(FACING);
		double x = pos.getX() + 0.5 + facing.getStepX() * 0.52, y = pos.getY() + 0.35, z = pos.getZ() + 0.5 + facing.getStepZ() * 0.52;
		level.addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 0.3, y + random.nextDouble() * 0.2, z + (random.nextDouble() - 0.5) * 0.3, 0, 0.01, 0);
		if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0, 0.05, 0);
		if (random.nextInt(8) == 0) {
			level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.8f, 0.9f + random.nextFloat() * 0.2f, false);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> lines, TooltipFlag flag) {
		Tooltips.add(lines, Component.translatable(ModLang.FORGE_TOOLTIP).withStyle(ChatFormatting.GRAY));
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	@SuppressWarnings("deprecation")
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ArtifactForgeBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.ARTIFACT_FORGE, ArtifactForgeBlockEntity::serverTick);
	}
}
