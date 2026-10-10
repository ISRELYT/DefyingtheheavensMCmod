package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
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
 *   <li>it belongs to a formation: one on no raised formation's shell removes itself.</li>
 * </ul>
 * Under Qi Sense the whole barrier shows as a faint purple membrane (drawn by the client from the formations the server sends).
 */
public class SectBarrierBlock extends Block {
	public SectBarrierBlock(Properties properties) {
		super(properties);
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
		if (Formations.isOrphan(level, pos)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
	}
}
