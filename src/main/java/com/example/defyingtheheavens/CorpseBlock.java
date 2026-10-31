package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A body where one of the dead lies (Corpses): low on the ground, drawn on its back in the dead's likeness by its renderer (its data:
 * CorpseBlockEntity). Fresh, then rotting (flies about it), then bones, by the time since the death; broken, it spills what it holds,
 * and bones some bones besides. Right-clicked, it says whose it was; with a glass bottle, a fresh one is bled (Corpses#use). Nothing
 * pushes it, and it drops nothing of itself.
 */
public class CorpseBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
	/** How far gone it is. */
	public enum Rot implements StringRepresentable {
		FRESH("fresh"), ROTTING("rotting"), BONES("bones");

		private final String id;

		Rot(String id) {
			this.id = id;
		}

		@Override
		public String getSerializedName() {
			return id;
		}
	}

	public static final EnumProperty<Rot> STAGE = EnumProperty.create("stage", Rot.class);
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);

	public CorpseBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(STAGE, Rot.FRESH).setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(STAGE, WATERLOGGED);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CorpseBlockEntity(pos, state);
	}

	@Override
	public SoundType getSoundType(BlockState state) {
		return state.getValue(STAGE) == Rot.BONES ? SoundType.BONE_BLOCK : SoundType.MUD;
	}

	/** Its rot, by the time since the death: fresh, rotting, then bones for good; the next change waited for. */
	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!(level.getBlockEntity(pos) instanceof CorpseBlockEntity body)) return;
		long dead = level.getGameTime() - body.diedAt();
		Rot rot = dead < Corpses.FRESH_TICKS ? Rot.FRESH : dead < Corpses.BONES_AT ? Rot.ROTTING : Rot.BONES;
		if (rot != state.getValue(STAGE)) level.setBlock(pos, state.setValue(STAGE, rot), Block.UPDATE_CLIENTS);
		if (rot != Rot.BONES) level.scheduleTick(pos, this, (int) Math.max(1, (rot == Rot.FRESH ? Corpses.FRESH_TICKS : Corpses.BONES_AT) - dead));
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide) return InteractionResult.SUCCESS;
		if (!(player instanceof ServerPlayer server) || !(level.getBlockEntity(pos) instanceof CorpseBlockEntity body)) return InteractionResult.PASS;
		return Corpses.use(server, hand, body, state);
	}

	/** One's own body, broken open in a secret realm: their milestone (Milestones#SECRET_OWN_BODY). */
	@Override
	public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (player instanceof ServerPlayer server && ModDimensions.isSecretRealm(level.dimension()) && level.getBlockEntity(pos) instanceof CorpseBlockEntity body
				&& body.profile() != null && server.getUUID().equals(body.profile().getId())) {
			Milestones.award(server, Milestones.SECRET_OWN_BODY);
		}
		super.playerWillDestroy(level, pos, state, player);
	}

	/** Broken (by hand, a blast or anything that takes it away): what it held spills out where it lay, and bones leave some bones. */
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.is(newState.getBlock())) {
			if (level.getBlockEntity(pos) instanceof CorpseBlockEntity body) {
				for (ItemStack stack : body.takeAll()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, stack);
			}
			if (state.getValue(STAGE) == Rot.BONES) {
				Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, new ItemStack(Items.BONE, Corpses.BONES_DROPPED));
			}
		}
		super.onRemove(state, level, pos, newState, moved);
	}

	/** Flies about a rotting body. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(STAGE) != Rot.ROTTING || random.nextInt(3) != 0) return;
		level.addParticle(ParticleTypes.MYCELIUM, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 1.6, pos.getY() + 0.35 + random.nextDouble() * 0.5,
				pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 1.6, 0, 0, 0);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
	}
}
