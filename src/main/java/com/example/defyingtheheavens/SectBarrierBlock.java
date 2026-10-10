package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One cell of a formation's barrier (see {@link Formation}). Like vanilla's barrier it is invisible, lets light and sky
 * through, can't be pushed, blown up or spawned on, and drops nothing. Unlike it:
 * <ul>
 *   <li>those the formation answers to walk through it (its owner, its sect's members, see {@link Formation#allows});</li>
 *   <li>it can be broken, but only by a cultivator at least as strong as the formation, faster the stronger they are
 *   ({@link Formations#breakTicks}); striking or breaking it alerts whoever keeps the formation;</li>
 *   <li>it belongs to a formation: one on no raised formation's shell removes itself;</li>
 *   <li>it can stand in still water or lava ({@link #FLUID}), so a river or a lake crossing the shell is closed too and still
 *   looks and flows as before; when the barrier comes down the water or lava is left behind ({@link #residue}).</li>
 * </ul>
 * Under Qi Sense the whole barrier shows as a faint purple membrane (drawn by the client from the formations the server sends).
 */
public class SectBarrierBlock extends Block {
	/** What the cell held before the barrier closed it: nothing, a still water source, or a still lava source. */
	public enum Held implements StringRepresentable {
		NONE("none"), WATER("water"), LAVA("lava");

		private final String id;

		Held(String id) { this.id = id; }

		@Override public String getSerializedName() { return id; }
	}

	public static final EnumProperty<Held> FLUID = EnumProperty.create("fluid", Held.class);

	public SectBarrierBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FLUID, Held.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FLUID);
	}

	/** A barrier cell holding {@code held}. */
	public static BlockState holding(Held held) {
		return ModBlocks.SECT_BARRIER.defaultBlockState().setValue(FLUID, held);
	}

	/** What is left of a barrier cell when its barrier comes down: the water or lava it stood in, or air. */
	public static BlockState residue(BlockState barrier) {
		if (!barrier.hasProperty(FLUID)) return Blocks.AIR.defaultBlockState();
		return switch (barrier.getValue(FLUID)) {
			case WATER -> Blocks.WATER.defaultBlockState();
			case LAVA -> Blocks.LAVA.defaultBlockState();
			case NONE -> Blocks.AIR.defaultBlockState();
		};
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return switch (state.getValue(FLUID)) {
			case WATER -> Fluids.WATER.getSource(false);
			case LAVA -> Fluids.LAVA.getSource(false);
			case NONE -> super.getFluidState(state);
		};
	}

	/** The water or lava it holds flows on into its neighbours, as from any source. */
	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		FluidState fluid = state.getFluidState();
		if (!fluid.isEmpty()) level.scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(level));
		return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
	}

	@Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
	@Override public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }
	@Override public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) { return 1.0f; }
	@Override public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }
	@Override public boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) { return adjacent.is(this); }

	/**
	 * Planned through like open air: the sect's own disciples walk the paths through it, and anyone else simply meets the
	 * wall when they get there.
	 */
	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return true;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() != null) {
			// The entity's own level, not {@code level}: pathfinding asks through a cached region, which isn't a Level, and
			// a sect's disciples must be able to plan their patrol paths through their own barrier.
			Formation formation = Formations.at(entityContext.getEntity().level(), pos);
			if (formation != null && formation.allows(entityContext.getEntity())) return Shapes.empty();
		}
		return Shapes.block();
	}

	@Override
	public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		Formation formation = Formations.at(level, pos);
		if (formation == null) return 0.05f; // an orphan about to vanish anyway
		int ticks = Formations.breakTicks(player, formation);
		return ticks <= 0 ? 0 : 1.0f / ticks;
	}

	@Override
	public void attack(BlockState state, Level level, BlockPos pos, Player player) {
		if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
			Formations.onAttacked(server, pos, sp);
			// The struck cell ripples, so the unseen wall shows for a moment.
			server.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.4);
			server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 0.8f, 0.6f);
		}
	}

	@Override
	public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
			Formations.onBroken(server, pos, sp);
			server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
			server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0f, 0.5f);
		}
		super.playerWillDestroy(level, pos, state, player);
	}

	@Override public boolean isRandomlyTicking(BlockState state) { return true; }

	/** The last safety net: a barrier block that no raised formation claims goes away. */
	@Override
	public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (Formations.isOrphan(level, pos)) level.setBlock(pos, residue(state), residue(state).isAir() ? Block.UPDATE_CLIENTS : Block.UPDATE_ALL);
	}
}
