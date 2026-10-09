package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A floating tear in reality. Walking into it pulls any player, of any cultivation, into the Spatial Gap to begin
 * ascension. Drawn entirely by the client's block entity renderer (a jagged black shard with a cyan glow).
 */
public class SpatialRiftBlock extends BaseEntityBlock {
	public static final int RIFT_X = 0;
	public static final int RIFT_Z = 0;

	public SpatialRiftBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SpatialRiftBlockEntity(pos, state);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	/** No outline or hitbox; entering is detected per block cell in {@link #entityInside}. */
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return;
		try {
			SpatialTrialHandler.beginAscension(player);
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Spatial rift failed to pull {} into the Spatial Gap", player.getScoreboardName(), e);
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		for (int i = 0; i < 2; i++) {
			double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.7;
			double y = pos.getY() - 0.2 + random.nextDouble() * 2.4;
			double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.7;
			level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z,
					(random.nextDouble() - 0.5) * 0.15, (random.nextDouble() - 0.5) * 0.15, (random.nextDouble() - 0.5) * 0.15);
		}
		if (random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
					(random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.5) * 0.5);
		}
	}

	/**
	 * Makes sure the Overworld has its rift at X=0, Z=0, floating two blocks above the top solid block. Called on
	 * server start, so it also appears in existing worlds and comes back if it was removed with commands.
	 */
	public static void ensureOverworldRift(MinecraftServer server) {
		if (server == null) return;
		try {
			ServerLevel overworld = server.overworld();
			if (overworld == null) return;

			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(RIFT_X, 0, RIFT_Z);
			for (int y = overworld.getMinBuildHeight(); y < overworld.getMaxBuildHeight(); y++) {
				if (overworld.getBlockState(cursor.setY(y)).is(ModBlocks.SPATIAL_RIFT)) return; // already there
			}

			overworld.getChunk(RIFT_X >> 4, RIFT_Z >> 4); // make sure the column is generated
			// Heightmap value is the first free Y above the top solid block, so "top solid + 2" is that + 1.
			int firstFree = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, RIFT_X, RIFT_Z);
			int y = Math.min(firstFree + 1, overworld.getMaxBuildHeight() - 3);
			BlockPos pos = new BlockPos(RIFT_X, y, RIFT_Z);
			overworld.setBlock(pos, ModBlocks.SPATIAL_RIFT.defaultBlockState(), 3);
			DefyingTheHeavens.LOGGER.info("Spatial rift placed at {}", pos.toShortString());
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Could not place the spatial rift in the Overworld", e);
		}
	}
}
