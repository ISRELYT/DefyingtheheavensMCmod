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
 * The Jade Lake Mirage's land (SecretRealm#JADE_LAKE_MIRAGE), worked out column by column from the world's seed alone (MirageLakeLand
 * lays it, MirageLakeWorks builds on it): a still lake of pale jade, every world's its own.
 * <ul>
 *   <li><b>The heart isle</b> (within {@link #HEART_R}): a low mound of white stone, the Mirror Pavilion on it (the works').</li>
 *   <li><b>The lake</b>: still and clear to its pale bed out to the hills, its isles ({@link Isle}: the landing on the main way, one at each
 *   arrival, the lake's own between) standing out of it, joined by bridges of jade (the works', not all of them real).</li>
 *   <li><b>The hills</b>: a pale shore at the water's edge, then white hills of calcite and diorite rising to {@link #RIM_Y} about it all,
 *   falling away past {@link #EDGE_R}.</li>
 * </ul>
 */
public final class MirageLake {
	/** The lake's land, laid block by block (MirageLakeLand). */
	public static final RealmLand LAND = new MirageLakeLand();
	/** The lake's water; the isles' ground; the lake's bed; the hills' rim. */
	public static final int WATER_Y = 64, ISLE_Y = 66, BED_Y = 57, RIM_Y = 94;
	/** The heart isle, the shore's foot, the realm's end, the arrivals' isles. */
	public static final int HEART_R = 21, WALL_R = 140, EDGE_R = 164, ARRIVAL_R = 118;

	/** What a column is. */
	public enum Kind { HEART, ISLE, LAKE, SHORE, HILL, NONE }

	/** One column: its ground's top and its kind. */
	public record Column(int ground, Kind kind) {}

	/** One of the lake's isles: its middle (from the lake's), its reach, and what it is ("landing", "arrival", "isle"). */
	public record Isle(int x, int z, int r, String role) {}

	/** A world's lake: its noises, the main way's bearing (the landing faces the heart from it), its isles. */
	public static final class Shape {
		final SimplexNoise roll, shore, hill, bed;
		public final double mainTurn;
		public final List<Isle> isles;
		final long seed;

		Shape(long seed) {
			this.seed = seed;
			RandomSource random = RandomSource.create(seed ^ 0x3A1E3A1EL);
			roll = new SimplexNoise(random);
			shore = new SimplexNoise(random);
			hill = new SimplexNoise(random);
			bed = new SimplexNoise(random);
			mainTurn = random.nextInt(4) * Math.PI / 2;
			List<Isle> isles = new ArrayList<>();
			isles.add(at(mainTurn, HEART_R + 26, 9, "landing"));
			for (int k = 0; k < 4; k++) isles.add(at(mainTurn + Math.PI / 4 + k * Math.PI / 2, ARRIVAL_R, 10, "arrival"));
			// The lake's own, between: wherever there is room, none crowding another.
			for (int tries = 0; isles.size() < 14 && tries < 300; tries++) {
				Isle isle = at(random.nextDouble() * Math.PI * 2, HEART_R + 24 + random.nextDouble() * (WALL_R - HEART_R - 44), 7 + random.nextInt(7), "isle");
				boolean room = Math.hypot(isle.x, isle.z) > HEART_R + isle.r + 10;
				for (Isle other : isles) if (Math.hypot(isle.x - other.x, isle.z - other.z) < isle.r + other.r + 10) room = false;
				if (room) isles.add(isle);
			}
			this.isles = List.copyOf(isles);
		}

		private static Isle at(double bearing, double out, int r, String role) {
			return new Isle((int) Math.round(Math.cos(bearing) * out), (int) Math.round(Math.sin(bearing) * out), r, role);
		}

		/** The landing's isle (the first). */
		public Isle landing() { return isles.get(0); }
	}

	private static final Map<Long, Shape> SHAPES = Collections.synchronizedMap(new HashMap<>());

	public static Shape shape(long seed) {
		return SHAPES.computeIfAbsent(seed, Shape::new);
	}

	/** The heart isle's edge at (lx, lz)'s bearing (winding: by the bearing's cosine and sine, no seam). */
	static double heartEdge(Shape s, double lx, double lz) {
		double r = Math.max(1.0e-6, Math.hypot(lx, lz));
		return HEART_R + 2.5 * s.shore.getValue(lx / r * 2.0, lz / r * 2.0);
	}

	/** The column at (lx, lz) from the lake's middle (SecretRealm#centre: the heart isle's). */
	public static Column column(Shape s, int lx, int lz) {
		double r = Math.hypot(lx, lz);
		if (r > EDGE_R + 4 * s.roll.getValue(lx * 0.02, lz * 0.02)) return new Column(-1, Kind.NONE);
		double sr = Math.max(1, r);
		// The hills: a pale shore at the water's edge, then white hills rolling up to the rim, falling away past the realm's end.
		double shoreAt = WALL_R + 7 * s.hill.getValue(lx / sr * 2.4, lz / sr * 2.4);
		if (r > shoreAt) {
			double over = r - shoreAt;
			if (over < 4) return new Column(WATER_Y + (over < 1.5 ? 0 : 1), Kind.SHORE);
			double rise = (RIM_Y - WATER_Y) * Mth.clamp((over - 4) / 16, 0, 1);
			double roll = 6 * s.hill.getValue(lx * 0.035, lz * 0.035) + 2 * s.roll.getValue(lx * 0.09, lz * 0.09);
			double down = Math.max(0, r - (EDGE_R - 8)) * 3;
			return new Column((int) Math.round(Math.max(WATER_Y + 1, WATER_Y + 2 + rise + roll * Mth.clamp((over - 4) / 10, 0, 1) - down)), Kind.HILL);
		}
		// The heart isle: a low mound, its shore sloping into the lake.
		double edge = heartEdge(s, lx, lz);
		if (r < edge) {
			double in = Mth.clamp((edge - r) / 4, 0, 1);
			int g = (int) Math.round(BED_Y + (ISLE_Y - BED_Y) * in + 2 * Mth.clamp(1 - r / edge, 0, 1));
			return new Column(g, g > WATER_Y ? Kind.HEART : Kind.LAKE);
		}
		// The lake's isles, their shores sloping into the water.
		for (Isle isle : s.isles) {
			double d = Math.hypot(lx - isle.x(), lz - isle.z());
			double reach = isle.r() + 2 * s.shore.getValue(lx * 0.08, lz * 0.08);
			if (d >= reach) continue;
			double in = Mth.clamp((reach - d) / 3, 0, 1);
			int g = (int) Math.round(BED_Y + (ISLE_Y - BED_Y) * in + (in >= 1 ? Math.round(0.7 * s.roll.getValue(lx * 0.1, lz * 0.1)) : 0));
			return new Column(g, g > WATER_Y ? Kind.ISLE : Kind.LAKE);
		}
		// The lake's bed: deepest between the heart and the hills, rising to the shore.
		double out = Mth.clamp((r - HEART_R) / (WALL_R - HEART_R), 0, 1);
		double deep = Math.sin(Math.PI * out);
		return new Column((int) Math.round(BED_Y + 3 - 4 * deep + 1.5 * s.bed.getValue(lx * 0.04, lz * 0.04)), Kind.LAKE);
	}

	/** The arrivals: the arrival isles' middles, on the ground. */
	public static List<BlockPos> arrivals(Shape s) {
		List<BlockPos> at = new ArrayList<>();
		for (Isle isle : s.isles) if (isle.role().equals("arrival")) at.add(new BlockPos(isle.x(), column(s, isle.x(), isle.z()).ground(), isle.z()));
		return at;
	}

	private MirageLake() {}
}
