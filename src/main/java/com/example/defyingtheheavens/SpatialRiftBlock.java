package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
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
 * A floating tear in reality. Where it leads depends on the realm it stands in ({@link #leads}): in the Overworld it pulls any
 * player, of any cultivation, into the Spatial Gap to begin ascension to the Upper Realm, out from under a running tribulation
 * too; in the Nether (the Gate of the Yellow Springs) it leads up the Soul River to the Netherworld; on the Ghost Gate's plaza
 * in the Netherworld it is the way down. Drawn entirely by the client's block entity renderer (a jagged black shard with a cyan
 * glow; the Yellow Springs' kind gold and cyan).
 */
public class SpatialRiftBlock extends BaseEntityBlock {
	public static final int RIFT_X = 0;
	public static final int RIFT_Z = 0;

	/** Where a rift leads one who walks into it, by the realm it stands in. */
	public enum Leads { UPPER_REALM, NETHERWORLD, DOWN_TO_NETHER, NOWHERE }

	/** The Overworld's rift leads to the Upper Realm; the Nether's to the Netherworld; the Netherworld's is the way down. */
	public static Leads leads(ResourceKey<Level> dimension) {
		if (dimension == Level.OVERWORLD) return Leads.UPPER_REALM;
		if (dimension == Level.NETHER) return Leads.NETHERWORLD;
		if (ModDimensions.isNetherworld(dimension)) return Leads.DOWN_TO_NETHER;
		return Leads.NOWHERE;
	}

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

	/**
	 * Players a rift has taken hold of, to be carried on as soon as the step that brought them in is done, and the server tick
	 * it took hold (a hold older than a second is stale: a world closed before its task ran, say).
	 */
	private static final java.util.Map<java.util.UUID, Integer> PENDING = new java.util.HashMap<>();

	/**
	 * A player walked in: carried on to where the rift leads, but not at once. A walker touches the rift inside the server's
	 * handling of their own step (it moves them along it to check it), and carrying them to another dimension right there makes
	 * the server, finishing that check, take the step for a wrong move and snap them back to where it ended: off the Soul River
	 * or the Spatial Gap's course in the new dimension, facing however they walked in. So it waits for the server's next task.
	 */
	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return;
		Leads leads = leads(level.dimension());
		MinecraftServer server = player.getServer();
		if (leads == Leads.NOWHERE || server == null) return;
		Integer held = PENDING.get(player.getUUID());
		if (held != null && Math.abs(server.getTickCount() - held) < 20) return;
		PENDING.put(player.getUUID(), server.getTickCount());
		server.tell(new net.minecraft.server.TickTask(server.getTickCount(), () -> {
			PENDING.remove(player.getUUID());
			if (player.isRemoved() || player.level() != level) return; // gone, or carried off some other way meanwhile
			try {
				switch (leads) {
					case UPPER_REALM -> SpatialTrialHandler.beginAscension(player);
					case NETHERWORLD -> SoulRiver.beginAscent(player);
					case DOWN_TO_NETHER -> SoulRiver.beginDescent(player, true);
					case NOWHERE -> { }
				}
			} catch (Exception e) {
				DefyingTheHeavens.LOGGER.error("Rift failed to carry {} on from {}", player.getScoreboardName(), level.dimension().location(), e);
			}
		}));
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.is(ModBlocks.YELLOW_SPRINGS_RIFT)) {
			// Souls drifting up out of the gold-rimmed tear, and the odd cold flame.
			double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
			double y = pos.getY() - 0.2 + random.nextDouble() * 2.4;
			double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
			level.addParticle(ParticleTypes.SOUL, x, y, z, 0, 0.03, 0);
			if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.02, 0);
			return;
		}
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
	 * Makes sure the Overworld has its rift at X=0, Z=0, floating two blocks above the top block, treetops included.
	 * Called on server start, so it also appears in existing worlds and comes back if it was removed with commands.
	 * A rift left inside a tree's leaves (placed before leaves counted) is moved up above them.
	 */
	public static void ensureOverworldRift(MinecraftServer server) {
		if (server == null) return;
		try {
			ServerLevel overworld = server.overworld();
			if (overworld == null) return;

			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(RIFT_X, 0, RIFT_Z);
			for (int y = overworld.getMinBuildHeight(); y < overworld.getMaxBuildHeight(); y++) {
				if (!overworld.getBlockState(cursor.setY(y)).is(ModBlocks.SPATIAL_RIFT)) continue;
				if (!isUnderLeaves(overworld, cursor)) return; // already there
				overworld.removeBlock(cursor, false);
				DefyingTheHeavens.LOGGER.info("Spatial rift at {} was inside leaves, moving it up", cursor.toShortString());
				break;
			}

			overworld.getChunk(RIFT_X >> 4, RIFT_Z >> 4); // make sure the column is generated
			// Heightmap value is the first free Y above the top block, so "top block + 2" is that + 1.
			int firstFree = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING, RIFT_X, RIFT_Z);
			int y = Math.min(firstFree + 1, overworld.getMaxBuildHeight() - 3);
			BlockPos pos = new BlockPos(RIFT_X, y, RIFT_Z);
			overworld.setBlock(pos, ModBlocks.SPATIAL_RIFT.defaultBlockState(), 3);
			DefyingTheHeavens.LOGGER.info("Spatial rift placed at {}", pos.toShortString());
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Could not place the spatial rift in the Overworld", e);
		}
	}

	/** True if any leaves block sits in the column above the rift. */
	private static boolean isUnderLeaves(ServerLevel level, BlockPos rift) {
		BlockPos.MutableBlockPos above = rift.mutable();
		for (int y = rift.getY() + 1; y < level.getMaxBuildHeight(); y++) {
			if (level.getBlockState(above.setY(y)).is(BlockTags.LEAVES)) return true;
		}
		return false;
	}
}
