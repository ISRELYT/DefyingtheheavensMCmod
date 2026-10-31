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
 * The Ancient Battlefield's land (SecretRealm#ANCIENT_BATTLEFIELD), worked out column by column from the world's seed alone
 * (BattlefieldLand lays it, BattlefieldWorks builds on it): the plain an old war was fought over, every world's its own.
 * <ul>
 *   <li><b>The plain</b> (to {@link #EDGE_R}): earth churned rust-red, rolling a little, pocked with old craters; three rings of trenches
 *   ({@link #TRENCHES}) broken in places, an earthwork thrown up on the inner side of each.</li>
 *   <li><b>The General's hill</b> (within {@link #HILL_R}): rising out of the middle to {@link #HILL_TOP_Y}, its top level
 *   ({@link #HILL_FLAT_R}) for the ring of banners where he made his last stand, a ramp up its front from the main way; his tomb within
 *   it (BattlefieldWorks).</li>
 *   <li><b>The rim</b>: the ground rising into low ridges past the last trench, then falling away to the realm's end in the mist.</li>
 * </ul>
 */
public final class Battlefield {
	/** The battlefield's land, laid block by block (BattlefieldLand). */
	public static final RealmLand LAND = new BattlefieldLand();
	/** The plain's height; the hill's top. */
	public static final int PLAIN_Y = 70, HILL_TOP_Y = 92;
	/** The hill's foot and its level top; the plain's end; the arrival camps, out from the middle. */
	public static final int HILL_R = 38, HILL_FLAT_R = 10, EDGE_R = 164, ARRIVAL_R = 142;
	/** The trench rings' radii (blocks out), each three wide and three deep; the ramp up the hill, either side of the main way. */
	public static final int[] TRENCHES = {64, 96, 126};
	public static final double TRENCH_HALF = 1.6, RAMP_HALF = 2.2;

	/** What a column is. */
	public enum Kind { PLAIN, TRENCH, BERM, CRATER, HILL, HILLTOP, RAMP, RIM, NONE }

	/** One column: its ground's top and its kind. */
	public record Column(int ground, Kind kind) {}

	/** A crater of the old war: where, how wide, how deep. */
	record Crater(double x, double z, double radius, double depth) {}

	/** A world's battlefield: its noises, the main way's bearing, its craters. */
	public static final class Shape {
		final SimplexNoise roll, wobble, ridge;
		/** The main way's bearing (a quarter turn's whole): the ramp up the hill faces it; its tomb's door is on the far side. */
		public final double mainTurn;
		final List<Crater> craters = new ArrayList<>();
		final long seed;

		Shape(long seed) {
			this.seed = seed;
			RandomSource random = RandomSource.create(seed ^ 0xBA771EL);
			roll = new SimplexNoise(random);
			wobble = new SimplexNoise(random);
			ridge = new SimplexNoise(random);
			mainTurn = random.nextInt(4) * Math.PI / 2;
			for (int i = 0; i < 70; i++) {
				double a = random.nextDouble() * Math.PI * 2, r = HILL_R + 6 + random.nextDouble() * (EDGE_R - HILL_R - 30);
				craters.add(new Crater(Math.cos(a) * r, Math.sin(a) * r, 2.5 + random.nextDouble() * 4.5, 1.5 + random.nextDouble() * 3));
			}
		}

		/** Way {@code k}'s bearing (k 0 the main), the arrival camps' and the gaps left in the trenches for them. */
		public double way(int k) { return mainTurn + k * Math.PI / 2; }
	}

	private static final Map<Long, Shape> SHAPES = Collections.synchronizedMap(new HashMap<>());

	public static Shape shape(long seed) {
		return SHAPES.computeIfAbsent(seed, Shape::new);
	}

	/** How far (lx, lz) lies off the main way's line, and how far out along it (negative behind the middle). */
	static double[] mainOff(Shape s, double lx, double lz) {
		double cos = Math.cos(s.mainTurn), sin = Math.sin(s.mainTurn);
		return new double[] {Math.abs(-lx * sin + lz * cos), lx * cos + lz * sin};
	}

	/** Whether (lx, lz) lies on one of the four ways in from the camps (where the trenches were bridged). */
	static boolean onWay(Shape s, double lx, double lz) {
		for (int k = 0; k < 4; k++) {
			double b = s.way(k), cos = Math.cos(b), sin = Math.sin(b);
			if (lx * cos + lz * sin > HILL_R && Math.abs(-lx * sin + lz * cos) < 3.5) return true;
		}
		return false;
	}

	/** The hill's height {@code r} out: level on top, a long rounded slope to its foot. */
	static double hillHeight(double r) {
		if (r <= HILL_FLAT_R) return HILL_TOP_Y;
		double t = Mth.clamp((r - HILL_FLAT_R) / (HILL_R - HILL_FLAT_R), 0, 1);
		double fall = t * t * (3 - 2 * t);
		return HILL_TOP_Y - fall * (HILL_TOP_Y - PLAIN_Y);
	}

	/** The column at (lx, lz) from the battlefield's middle (SecretRealm#centre). */
	public static Column column(Shape s, int lx, int lz) {
		double r = Math.hypot(lx, lz);
		if (r > EDGE_R + 4 * s.ridge.getValue(lx * 0.02, lz * 0.02)) return new Column(-1, Kind.NONE);
		if (r < HILL_R) {
			double[] off = mainOff(s, lx, lz);
			if (r > HILL_FLAT_R - 1 && off[1] > 0 && off[0] < RAMP_HALF) {
				// The ramp straight up its front: an even climb from the plain to the top.
				double t = Mth.clamp((HILL_R - r) / (HILL_R - HILL_FLAT_R), 0, 1);
				return new Column((int) Math.round(PLAIN_Y + t * (HILL_TOP_Y - PLAIN_Y)), Kind.RAMP);
			}
			int g = (int) Math.round(hillHeight(r) + (r > HILL_FLAT_R ? 0.8 * s.roll.getValue(lx * 0.08, lz * 0.08) : 0));
			return new Column(g, r <= HILL_FLAT_R ? Kind.HILLTOP : Kind.HILL);
		}
		double roll = 2.2 * s.roll.getValue(lx * 0.03, lz * 0.03) + 0.8 * s.roll.getValue(lx * 0.1, lz * 0.1);
		int g = PLAIN_Y + (int) Math.round(roll * Mth.clamp((r - HILL_R) / 8.0, 0, 1));
		// The rim: rising into low ridges past the last trench, falling away past the plain's end.
		double over = r - (TRENCHES[2] + 8);
		if (over > 0) {
			double up = 18 * smooth(over / 22) + 8 * (1 - Math.abs(s.ridge.getValue(lx * 0.04, lz * 0.04))) * smooth(over / 14);
			double down = Math.max(0, r - (EDGE_R - 10)) * 2.6;
			if (!onWay(s, lx, lz) || r > ARRIVAL_R + 4) return new Column((int) Math.round(g + up - down), Kind.RIM);
			return new Column((int) Math.round(g + Math.min(up, (r - TRENCHES[2] - 8) * 0.6) - down), Kind.PLAIN);
		}
		// The trenches: three rings, broken where the ways in cross them and here and there besides; an earthwork on the inside of each.
		for (int ring : TRENCHES) {
			double wob = 2.5 * s.wobble.getValue(lx * 0.025, lz * 0.025);
			double d = r - (ring + wob);
			boolean broken = onWay(s, lx, lz) || s.wobble.getValue(lx / r * 2.0 + ring, lz / r * 2.0) > 0.55; // (by bearing: no seam)
			if (Math.abs(d) < TRENCH_HALF && !broken) return new Column(g - 3, Kind.TRENCH);
			if (d < -TRENCH_HALF && d > -TRENCH_HALF - 2.5 && !broken) return new Column(g + 1, Kind.BERM);
		}
		for (Crater crater : s.craters) {
			double d = Math.hypot(lx - crater.x, lz - crater.z);
			if (d < crater.radius) {
				double t = 1 - d / crater.radius;
				return new Column(g - (int) Math.round(crater.depth * t * t * 1.4), Kind.CRATER);
			}
		}
		return new Column(g, Kind.PLAIN);
	}

	private static double smooth(double t) {
		t = Mth.clamp(t, 0, 1);
		return t * t * (3 - 2 * t);
	}

	// --- The places on it (in the battlefield's own coordinates) ---

	/** The arrival camps on the four ways in: their middles, on the ground. */
	public static List<BlockPos> arrivals(Shape s) {
		List<BlockPos> at = new ArrayList<>();
		for (int k = 0; k < 4; k++) {
			double b = s.way(k);
			int x = (int) Math.round(Math.cos(b) * ARRIVAL_R), z = (int) Math.round(Math.sin(b) * ARRIVAL_R);
			at.add(new BlockPos(x, column(s, x, z).ground(), z));
		}
		return at;
	}

	private Battlefield() {}
}
