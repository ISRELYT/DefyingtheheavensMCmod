package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Where one comes out of the Spatial Gap, found and made ready without the server waiting on new land. Coming out where nobody
 * has been means generating the land there, and asked for on the server thread (Level#getChunk) every player waits until it is
 * done: a lag spike at each arrival in fresh land (the old arrival search could generate some 49 chunks of the Upper Realm's sky
 * at once). So:
 * <ul>
 *   <li>the Upper Realm's island to land on is found by numbers ({@link #islandColumn}): the island masks
 *       (ModNoiseSettings#islandMask, four noise samples a point) say where islands lie, and the generator's own heights
 *       (ChunkGenerator#getBaseHeight) confirm the nearest; no land is generated for either;</li>
 *   <li>the land there is asked for ahead ({@link #prepare}): a ticket on its chunks, generated on the world-generation threads
 *       while the server goes on: nearing the crossing or the floor, before one gets there, and for a tear out while space
 *       settles (SpatialTrialHandler: the hold);</li>
 *   <li>and the arrival reads only land that is loaded ({@link #groundNear}).</li>
 * </ul>
 */
public final class SpatialArrivals {
	/** Island ground is sought this far round the point one comes out at in the Upper Realm, on the islands' masks. */
	public static final int ISLAND_REACH = 160;
	private static final int MASK_STEP = 8;
	/** A mask above this is well inside an island, past its ragged rim (0 is the rim; the 3D detail noise reaches about 0.15). */
	static final double INSIDE = 0.05;
	/** At most this many of the masks' points are put to the generator (about 10 ms each); each one turned down rules out its surroundings. */
	private static final int CONFIRMS = 3;
	private static final int TURNED_DOWN_REACH = 32;
	/** Ground is looked for this far round the column arrived at, in the land loaded round it. */
	private static final int GROUND_REACH = 12;
	/** The land asked for stays loaded this long (ticks), then goes as any other. */
	private static final int TICKET_TICKS = 30 * 20;
	private static final TicketType<ChunkPos> TICKET = TicketType.create("dth_gap_arrival", Comparator.comparingLong(ChunkPos::toLong), TICKET_TICKS);

	/** The column to come out at in {@code level} from (x, z): in the Upper Realm, the nearest island's ({@link #islandColumn}); else (x, z). */
	public static BlockPos target(ServerLevel level, int x, int z) {
		if (!ModDimensions.isUpperRealm(level.dimension())) return new BlockPos(x, 0, z);
		BlockPos island = islandColumn(level.getChunkSource().getGenerator(), level.getChunkSource().randomState(), level, x, z);
		return island != null ? island : new BlockPos(x, 0, z);
	}

	/**
	 * The column nearest (x, z), within {@link #ISLAND_REACH}, that is well inside an island by its mask and has ground under it by
	 * the generator's heights; or null. Nothing is generated or loaded: a few milliseconds of noise, and a generator column or two.
	 */
	public static BlockPos islandColumn(ChunkGenerator generator, RandomState noise, LevelHeightAccessor heights, int x, int z) {
		List<int[]> inside = new ArrayList<>();
		int reach2 = ISLAND_REACH * ISLAND_REACH;
		for (int dx = -ISLAND_REACH; dx <= ISLAND_REACH; dx += MASK_STEP) {
			for (int dz = -ISLAND_REACH; dz <= ISLAND_REACH; dz += MASK_STEP) {
				int d2 = dx * dx + dz * dz;
				if (d2 > reach2 || ModNoiseSettings.islandMask(noise, x + dx, z + dz) <= INSIDE) continue;
				inside.add(new int[] {x + dx, z + dz, d2});
			}
		}
		inside.sort(Comparator.comparingInt(c -> c[2]));
		int floor = heights.getMinBuildHeight() + 2;
		List<int[]> turnedDown = new ArrayList<>();
		for (int[] c : inside) {
			if (turnedDown.size() >= CONFIRMS) break;
			boolean near = false;
			for (int[] no : turnedDown) near |= Math.abs(no[0] - c[0]) < TURNED_DOWN_REACH && Math.abs(no[1] - c[1]) < TURNED_DOWN_REACH;
			if (near) continue;
			if (generator.getBaseHeight(c[0], c[1], Heightmap.Types.WORLD_SURFACE_WG, heights, noise) > floor) return new BlockPos(c[0], 0, c[1]);
			turnedDown.add(c);
		}
		return null;
	}

	/** Asks for the land round {@code column} (its chunk and those touching it) to be loaded, generated off the server thread, for 30 s. */
	public static void prepare(ServerLevel level, BlockPos column) {
		ChunkPos chunk = new ChunkPos(column);
		level.getChunkSource().addRegionTicket(TICKET, chunk, 1, chunk);
	}

	/** Whether the land at {@code column} is loaded: arriving there waits on nothing. */
	public static boolean ready(ServerLevel level, BlockPos column) {
		return level.getChunkSource().getChunkNow(column.getX() >> 4, column.getZ() >> 4) != null;
	}

	/** Dry, solid ground to stand on at or near {@code column}, in land already loaded (none is loaded for it); or null. */
	public static BlockPos groundNear(ServerLevel level, BlockPos column) {
		for (int radius = 0; radius <= GROUND_REACH; radius += 4) {
			for (int i = 0; i < (radius == 0 ? 1 : 8); i++) {
				double angle = Math.PI * 2 * i / 8;
				int cx = column.getX() + (int) Math.round(Math.cos(angle) * radius);
				int cz = column.getZ() + (int) Math.round(Math.sin(angle) * radius);
				if (level.getChunkSource().getChunkNow(cx >> 4, cz >> 4) == null) continue;
				int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
				if (top <= level.getMinBuildHeight() + 1 || top >= level.getMaxBuildHeight() - 2) continue;
				BlockPos ground = new BlockPos(cx, top - 1, cz);
				BlockState state = level.getBlockState(ground);
				if (state.getFluidState().isEmpty() && state.isFaceSturdy(level, ground, Direction.UP)) return ground.above();
			}
		}
		return null;
	}

	private SpatialArrivals() {}
}
