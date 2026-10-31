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
 * The Primeval Beast Graveyard's land (SecretRealm#PRIMEVAL_BEAST_GRAVEYARD), worked out column by column from the world's seed alone
 * (BeastGraveyardLand lays it, BeastGraveyardWorks builds on it): a jungle basin, every world's its own.
 * <ul>
 *   <li><b>The basin</b> (to {@link #BASIN_R}): rolling jungle floor at {@link #FLOOR_Y}, still green ponds in its hollows.</li>
 *   <li><b>The cliffs</b>: the basin's wall, stepping up to the jungle rim at {@link #RIM_Y}; past {@link #EDGE_R} the realm's end, hidden in
 *   the haze.</li>
 *   <li><b>The dragon</b>: its bones lie in a great curve round the heart ({@link #SPINE_R} out, {@link #SPINE_TURN} degrees round, its skull
 *   at the curve's start: {@link #spinePoint}), laid by the works.</li>
 * </ul>
 */
public final class BeastGraveyard {
	/** The graveyard's land, laid block by block (BeastGraveyardLand). */
	public static final RealmLand LAND = new BeastGraveyardLand();
	/** The basin's floor, its ponds' water, the rim over the cliffs. */
	public static final int FLOOR_Y = 70, WATER_Y = 68, RIM_Y = 104;
	/** The cliffs' foot, the realm's end, the arrival clearings (on the basin's floor). */
	public static final int BASIN_R = 128, EDGE_R = 164, ARRIVAL_R = 116;
	/** The dragon's spine: how far out round the heart, and how far round (degrees) from its skull to its tail's tip. */
	public static final int SPINE_R = 48;
	public static final double SPINE_TURN = 250;

	/** What a column is. */
	public enum Kind { FLOOR, POND, CLIFF, RIM, NONE }

	/** One column: its ground's top and its kind. */
	public record Column(int ground, Kind kind) {}

	/** A world's graveyard: its noises and the bearing its dragon's skull lies at (its spine running round from there). */
	public static final class Shape {
		final SimplexNoise roll, ponds, cliff, patches;
		/** The skull's bearing (radians from +x toward +z). */
		public final double skullTurn;
		final long seed;

		Shape(long seed) {
			this.seed = seed;
			RandomSource random = RandomSource.create(seed ^ 0xB0E5DA7L);
			roll = new SimplexNoise(random);
			ponds = new SimplexNoise(random);
			cliff = new SimplexNoise(random);
			patches = new SimplexNoise(random);
			skullTurn = random.nextInt(4) * Math.PI / 2 + Math.PI / 4;
		}
	}

	private static final Map<Long, Shape> SHAPES = Collections.synchronizedMap(new HashMap<>());

	public static Shape shape(long seed) {
		return SHAPES.computeIfAbsent(seed, Shape::new);
	}

	/** Where the cliffs' foot stands at (lx, lz)'s bearing: winding a little (by the bearing's cosine and sine: no seam). */
	static double basinEdge(Shape s, double lx, double lz) {
		double r = Math.max(1.0e-6, Math.hypot(lx, lz));
		return BASIN_R + 7 * s.cliff.getValue(lx / r * 2.4, lz / r * 2.4);
	}

	/** The column at (lx, lz) from the graveyard's middle (SecretRealm#centre). */
	public static Column column(Shape s, int lx, int lz) {
		double r = Math.hypot(lx, lz);
		if (r > EDGE_R + 4 * s.roll.getValue(lx * 0.02, lz * 0.02)) return new Column(-1, Kind.NONE);
		double roll = 2.4 * s.roll.getValue(lx * 0.025, lz * 0.025) + 0.8 * s.roll.getValue(lx * 0.09, lz * 0.09);
		double edge = basinEdge(s, lx, lz);
		if (r >= edge) {
			// The cliffs: a steep climb in broken steps to the rim, then the rim's jungle, falling away past the realm's end. Each step's
			// riser climbs over a block or so (never one sheer curved wall: its block-steps read as stripes), its ledge wandering in and out.
			double over = r - edge;
			if (over < 16) {
				double o = Math.max(0, over + 3 * s.cliff.getValue(lx * 0.07, lz * 0.07));
				int steps = (int) Math.floor(o / 4);
				double rise = Mth.clamp((o / 4 - steps) / 0.4, 0, 1);
				int g = FLOOR_Y + (int) Math.round((RIM_Y - FLOOR_Y) * Math.min(1, (steps + rise) / 4.0) + 1.5 * s.cliff.getValue(lx * 0.08, lz * 0.08));
				return new Column(Math.max(FLOOR_Y, Math.min(g, RIM_Y + 2)), Kind.CLIFF);
			}
			int g = RIM_Y + (int) Math.round(roll) - (int) Math.max(0, (r - (EDGE_R - 10)) * 2.6);
			return new Column(g, Kind.RIM);
		}
		int g = FLOOR_Y + (int) Math.round(roll * Mth.clamp((edge - r) / 10, 0, 1));
		// Its ponds: still water in the floor's hollows, kept off the arrivals, the dragon's skull and the ground its echo rises on before it.
		double pond = s.ponds.getValue(lx * 0.018, lz * 0.018);
		if (pond > 0.52 && r < edge - 18 && Math.hypot(lx - skull(s).getX(), lz - skull(s).getZ()) > 34) {
			int depth = 1 + (int) Math.round(Math.min(3, (pond - 0.52) * 14));
			return new Column(WATER_Y - depth, Kind.POND);
		}
		return new Column(g, Kind.FLOOR);
	}

	// --- The dragon, in the graveyard's own coordinates ---

	/** The spine's middle {@code turn} degrees round from the skull (the curve's start) toward the tail. */
	public static BlockPos spinePoint(Shape s, double turn) {
		double a = s.skullTurn + Math.toRadians(turn);
		return new BlockPos((int) Math.round(Math.cos(a) * SPINE_R), 0, (int) Math.round(Math.sin(a) * SPINE_R));
	}

	/** The skull's middle: just before the spine's start, along the curve's way back. */
	public static BlockPos skull(Shape s) {
		return spinePoint(s, -12);
	}

	/** How far round from the skull (lx, lz) lies along the dragon (0 at the skull), and how far off its spine (signed: out from the heart +). */
	static double[] alongDragon(Shape s, double lx, double lz) {
		double deg = Math.toDegrees(Math.atan2(lz, lx) - s.skullTurn);
		return new double[] {Mth.positiveModulo(deg, 360), Math.hypot(lx, lz) - SPINE_R};
	}

	/** The arrival clearings on the basin's floor, between the dragon's ends: their middles, on the ground. */
	public static List<BlockPos> arrivals(Shape s) {
		List<BlockPos> at = new ArrayList<>();
		for (int k = 0; k < 4; k++) {
			double b = s.skullTurn + Math.toRadians(45 + k * 90);
			int x = (int) Math.round(Math.cos(b) * ARRIVAL_R), z = (int) Math.round(Math.sin(b) * ARRIVAL_R);
			at.add(new BlockPos(x, column(s, x, z).ground(), z));
		}
		return at;
	}

	private BeastGraveyard() {}
}
