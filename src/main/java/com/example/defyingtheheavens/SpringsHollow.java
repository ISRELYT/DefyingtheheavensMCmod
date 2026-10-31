package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Yellow Springs Hollow's land (SecretRealm#YELLOW_SPRINGS_HOLLOW), worked out column by column from the world's seed alone
 * (SpringsHollowLand lays it, SpringsHollowWorks builds on it): a drowned city of the dead, every world's its own.
 * <ul>
 *   <li><b>The tomb's island</b> (within {@link #TOMB_R}): a mound at the heart, the Ghost King's tomb on it (the works').</li>
 *   <li><b>The Yellow Springs</b>: a deep ring of water about it (to {@link #RIVER_R}), crossed only by the Ferryman's boat (or by those who
 *   dare its pull).</li>
 *   <li><b>The drowned city</b>: shallows over its sunken streets out to {@link #WALL_R}, its islands ({@link Isle}: the landing on the main
 *   way, one at each arrival, the city's own between) standing out of them, joined by bridges (the works').</li>
 *   <li><b>The hollow's wall</b>: cliffs of dark stone rising to {@link #RIM_Y} about it all, falling away past {@link #EDGE_R}.</li>
 * </ul>
 */
public final class SpringsHollow {
	/** The hollow's land, laid block by block (SpringsHollowLand). */
	public static final RealmLand LAND = new SpringsHollowLand();
	/** The springs' water; the islands' ground; the sunken streets' floor and the river's deeper bed; the wall's rim. */
	public static final int WATER_Y = 64, ISLAND_Y = 67, BED_Y = 54, RIVER_BED_Y = 44, RIM_Y = 112;
	/** The tomb's island, the river's outer bank, the hollow's wall, the realm's end, the arrivals' islands. */
	public static final int TOMB_R = 22, RIVER_R = 50, WALL_R = 140, EDGE_R = 164, ARRIVAL_R = 122;

	/** What a column is. */
	public enum Kind { TOMB, RIVER, ISLE, SHALLOWS, WALL, NONE }

	/** One column: its ground's top and its kind. */
	public record Column(int ground, Kind kind) {}

	/** One of the drowned city's islands: its middle (from the hollow's), its reach, and what it is ("landing", "arrival", "city"). */
	public record Isle(int x, int z, int r, String role) {}

	/** A world's hollow: its noises, the main way's bearing (the landing faces the tomb from it), its islands. */
	public static final class Shape {
		final SimplexNoise roll, shore, wall, bed;
		public final double mainTurn;
		public final List<Isle> isles;
		final long seed;

		Shape(long seed) {
			this.seed = seed;
			RandomSource random = RandomSource.create(seed ^ 0x7E11055L);
			roll = new SimplexNoise(random);
			shore = new SimplexNoise(random);
			wall = new SimplexNoise(random);
			bed = new SimplexNoise(random);
			mainTurn = random.nextInt(4) * Math.PI / 2;
			List<Isle> isles = new ArrayList<>();
			isles.add(at(mainTurn, RIVER_R + 14, 12, "landing"));
			for (int k = 0; k < 4; k++) isles.add(at(mainTurn + Math.PI / 4 + k * Math.PI / 2, ARRIVAL_R, 10, "arrival"));
			// The city's own, between: wherever there is room, none crowding another.
			for (int tries = 0; isles.size() < 13 && tries < 300; tries++) {
				Isle isle = at(random.nextDouble() * Math.PI * 2, RIVER_R + 20 + random.nextDouble() * (WALL_R - RIVER_R - 38), 8 + random.nextInt(8), "city");
				boolean room = true;
				for (Isle other : isles) if (Math.hypot(isle.x - other.x, isle.z - other.z) < isle.r + other.r + 8) room = false;
				if (room) isles.add(isle);
			}
			this.isles = List.copyOf(isles);
		}

		private static Isle at(double bearing, double out, int r, String role) {
			return new Isle((int) Math.round(Math.cos(bearing) * out), (int) Math.round(Math.sin(bearing) * out), r, role);
		}

		/** The landing's island (the first). */
		public Isle landing() { return isles.get(0); }
	}

	private static final Map<Long, Shape> SHAPES = Collections.synchronizedMap(new HashMap<>());

	public static Shape shape(long seed) {
		return SHAPES.computeIfAbsent(seed, Shape::new);
	}

	/** How far (lx, lz) lies off the main way's line, and how far out along it. */
	static double[] mainOff(Shape s, double lx, double lz) {
		double cos = Math.cos(s.mainTurn), sin = Math.sin(s.mainTurn);
		return new double[] {Math.abs(-lx * sin + lz * cos), lx * cos + lz * sin};
	}

	/** The tomb island's edge at (lx, lz)'s bearing (winding: by the bearing's cosine and sine, no seam). */
	static double tombEdge(Shape s, double lx, double lz) {
		double r = Math.max(1.0e-6, Math.hypot(lx, lz));
		return TOMB_R + 3 * s.shore.getValue(lx / r * 2.0, lz / r * 2.0);
	}

	/** The column at (lx, lz) from the hollow's middle (SecretRealm#centre: the tomb's). */
	public static Column column(Shape s, int lx, int lz) {
		double r = Math.hypot(lx, lz);
		if (r > EDGE_R + 4 * s.roll.getValue(lx * 0.02, lz * 0.02)) return new Column(-1, Kind.NONE);
		double sr = Math.max(1, r);
		// The wall: dark cliffs rising from the water's edge to the rim, falling away past the realm's end.
		double wallAt = WALL_R + 6 * s.wall.getValue(lx / sr * 2.2, lz / sr * 2.2);
		if (r > wallAt) {
			double over = r - wallAt;
			double up = (RIM_Y - WATER_Y) * Mth.clamp(over / 12, 0, 1) + 5 * Math.abs(s.wall.getValue(lx * 0.06, lz * 0.06));
			double down = Math.max(0, r - (EDGE_R - 8)) * 3;
			return new Column((int) Math.round(Math.max(WATER_Y + 1, WATER_Y + 1 + up - down)), Kind.WALL);
		}
		// The tomb's island: a mound, its shore sloping into the springs.
		double edge = tombEdge(s, lx, lz);
		if (r < edge) {
			double in = Mth.clamp((edge - r) / 4, 0, 1);
			int g = (int) Math.round(WATER_Y - 2 + (ISLAND_Y - WATER_Y + 2) * in + 3 * Mth.clamp(1 - r / edge, 0, 1));
			return new Column(g, g > WATER_Y ? Kind.TOMB : Kind.RIVER);
		}
		// The springs' ring: deep.
		if (r < RIVER_R + 2 * s.shore.getValue(lx * 0.05, lz * 0.05)) {
			return new Column(RIVER_BED_Y + (int) Math.round(3 * s.bed.getValue(lx * 0.05, lz * 0.05)), Kind.RIVER);
		}
		// The city's islands, their shores sloping into the shallows.
		for (Isle isle : s.isles) {
			double d = Math.hypot(lx - isle.x(), lz - isle.z());
			double reach = isle.r() + 2.5 * s.shore.getValue(lx * 0.08, lz * 0.08);
			if (d >= reach) continue;
			double in = Mth.clamp((reach - d) / 3, 0, 1);
			int g = (int) Math.round(BED_Y + (ISLAND_Y - BED_Y) * in + (in >= 1 ? Math.round(0.8 * s.roll.getValue(lx * 0.1, lz * 0.1)) : 0));
			return new Column(g, g > WATER_Y ? Kind.ISLE : Kind.SHALLOWS);
		}
		return new Column(BED_Y + (int) Math.round(2 * s.bed.getValue(lx * 0.04, lz * 0.04)), Kind.SHALLOWS);
	}

	/** The arrivals: the arrival islands' middles, on the ground. */
	public static List<BlockPos> arrivals(Shape s) {
		List<BlockPos> at = new ArrayList<>();
		for (Isle isle : s.isles) if (isle.role().equals("arrival")) at.add(new BlockPos(isle.x(), column(s, isle.x(), isle.z()).ground(), isle.z()));
		return at;
	}

	private SpringsHollow() {}
}
