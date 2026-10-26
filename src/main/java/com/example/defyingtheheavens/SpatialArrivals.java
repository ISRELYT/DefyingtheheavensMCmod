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

	/**
	 * The Great Void's continents are sought this far round (and at this step: they are far larger than the Jade Firmament's islands,
	 * and the void between them as wide), on their own mask (ModNoiseSettings#continentMask).
	 */
	public static final int CONTINENT_REACH = 480, CONTINENT_STEP = 16;

	/**
	 * The column to come out at in {@code level} from (x, z): in the Upper Realm, the nearest island's ({@link #islandColumn}); in the
	 * Great Void, the nearest continent's ({@link #continentColumn}), outside any sect's barrier ({@link #clearOfSects}); else (x, z).
	 */
	public static BlockPos target(ServerLevel level, int x, int z) {
		boolean greatVoid = ModDimensions.isGreatVoid(level.dimension()), immortal = ModDimensions.isImmortalRealm(level.dimension());
		if (!ModDimensions.isUpperRealm(level.dimension()) && !greatVoid && !immortal) return new BlockPos(x, 0, z);
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		RandomState noise = level.getChunkSource().randomState();
		BlockPos land = immortal ? massifColumn(generator, noise, level, x, z)
				: greatVoid ? continentColumn(generator, noise, level, x, z) : islandColumn(generator, noise, level, x, z);
		if (land != null && (greatVoid || immortal)) land = clearOfSects(level, land);
		return land != null ? land : new BlockPos(x, 0, z);
	}

	/** How far past a sect's barrier one arriving near it comes down ({@link #clearOfSects}). */
	private static final int CLEAR_OF_SECTS = 12;

	/**
	 * {@code column} moved out past the barrier of any sect it lies within, straight out from the sect's core to {@link #CLEAR_OF_SECTS}
	 * beyond its edge; else as it was. One arriving in the Great Void (through a breach, borne by a current) comes down outside a Holy Land's
	 * formation, not on its dome nor within it (a void current set a player down on a Holy Land's barrier: tour 7, 2026-10-25). Only there:
	 * the Great Void's continents have room round their sects, the Jade Firmament's islands may not.
	 */
	public static BlockPos clearOfSects(ServerLevel level, BlockPos column) {
		for (int pass = 0; pass < 2; pass++) {
			boolean moved = false;
			for (Sect sect : SectData.get(level.getServer()).all()) {
				if (sect.extinct || sect.dimension != level.dimension()) continue;
				double dx = column.getX() - sect.core.getX(), dz = column.getZ() - sect.core.getZ(), d = Math.sqrt(dx * dx + dz * dz);
				double reach = sect.barrierRadius + CLEAR_OF_SECTS;
				if (d >= reach) continue;
				if (d < 1) {
					dx = 1;
					dz = 0;
					d = 1;
				}
				column = new BlockPos(sect.core.getX() + (int) Math.round(dx / d * reach), column.getY(), sect.core.getZ() + (int) Math.round(dz / d * reach));
				moved = true;
			}
			if (!moved) break;
		}
		return column;
	}

	/**
	 * The column nearest (x, z), within {@link #ISLAND_REACH}, that is well inside an island by its mask and has ground under it by
	 * the generator's heights; or null. Nothing is generated or loaded: a few milliseconds of noise, and a generator column or two.
	 */
	public static BlockPos islandColumn(ChunkGenerator generator, RandomState noise, LevelHeightAccessor heights, int x, int z) {
		return landColumn(generator, noise, heights, x, z, ISLAND_REACH, MASK_STEP, (state, px, pz) -> ModNoiseSettings.islandMask(state, px, pz));
	}

	/** The same in the Great Void: the column nearest (x, z) well inside a continent, within {@link #CONTINENT_REACH}; or null. */
	public static BlockPos continentColumn(ChunkGenerator generator, RandomState noise, LevelHeightAccessor heights, int x, int z) {
		return landColumn(generator, noise, heights, x, z, CONTINENT_REACH, CONTINENT_STEP, ModNoiseSettings::continentMask);
	}

	/** The Immortal Realm's massifs are sought this far round (and at this step), on their own mask (ModNoiseSettings#massifMask). */
	public static final int MASSIF_REACH = 320, MASSIF_STEP = 12;

	/** The same in the Immortal Realm: the column nearest (x, z) well inside a massif, within {@link #MASSIF_REACH}; or null. */
	public static BlockPos massifColumn(ChunkGenerator generator, RandomState noise, LevelHeightAccessor heights, int x, int z) {
		return landColumn(generator, noise, heights, x, z, MASSIF_REACH, MASSIF_STEP, ModNoiseSettings::massifMask);
	}

	/** A realm's land mask at a column: positive over land, 0 at its rim. */
	private interface Mask {
		double at(RandomState state, int x, int z);
	}

	private static BlockPos landColumn(ChunkGenerator generator, RandomState noise, LevelHeightAccessor heights, int x, int z, int reach, int step, Mask mask) {
		List<int[]> inside = new ArrayList<>();
		int reach2 = reach * reach;
		for (int dx = -reach; dx <= reach; dx += step) {
			for (int dz = -reach; dz <= reach; dz += step) {
				int d2 = dx * dx + dz * dz;
				if (d2 > reach2 || mask.at(noise, x + dx, z + dz) <= INSIDE) continue;
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
				// Not a sect's barrier: its dome tops the column, the ground lies under it.
				if (state.is(ModBlocks.SECT_BARRIER)) continue;
				if (state.getFluidState().isEmpty() && state.isFaceSturdy(level, ground, Direction.UP)) return ground.above();
			}
		}
		return null;
	}

	private SpatialArrivals() {}
}
