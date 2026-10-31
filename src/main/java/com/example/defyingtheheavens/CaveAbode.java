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
 * The Cave Abode of a Heavenly Being's land (SecretRealm#CAVE_ABODE), worked out column by column from the world's seed alone (CaveAbodeLand
 * lays it, CaveAbodeWorks hollows and builds on it): every world's its own.
 * <ul>
 *   <li><b>The mountain</b> (within {@link #MOUNT_R}): one lone peak rising steep out of the basin to {@link #PEAK_Y}, ridged and crowned,
 *   solid rock: the abode is hollowed out of it, its gate on the main way at the mountain's foot.</li>
 *   <li><b>The lake</b>: still water before the gate ({@link #LAKE_R} out on the main way), a causeway across it (the works').</li>
 *   <li><b>The basin</b> (to {@link #BASIN_R}): meadow and pine wood on rolling ground at {@link #FLOOR_Y}; its rim of grey crags, the realm's
 *   end past {@link #EDGE_R}.</li>
 * </ul>
 */
public final class CaveAbode {
	/** The abode's land, laid block by block (CaveAbodeLand). */
	public static final RealmLand LAND = new CaveAbodeLand();
	/** The basin's floor and the lake's water; the peak's top. */
	public static final int FLOOR_Y = 72, WATER_Y = 71, PEAK_Y = 168;
	/** The mountain's foot, the lake's middle on the main way, the basin's edge, the realm's end, the arrivals. */
	public static final int MOUNT_R = 58, LAKE_R = 76, BASIN_R = 138, EDGE_R = 164, ARRIVAL_R = 126;

	/** What a column is. */
	public enum Kind { MOUNTAIN, LAKE, FLOOR, CRAG, NONE }

	/** One column: its ground's top and its kind. */
	public record Column(int ground, Kind kind) {}

	/** A world's abode: its noises and its main way's bearing (a quarter turn's whole: the gate faces it). */
	public static final class Shape {
		final SimplexNoise ridge, roll, crag;
		public final double mainTurn;
		final long seed;

		Shape(long seed) {
			this.seed = seed;
			RandomSource random = RandomSource.create(seed ^ 0xCA7EAB0DEL);
			ridge = new SimplexNoise(random);
			roll = new SimplexNoise(random);
			crag = new SimplexNoise(random);
			mainTurn = random.nextInt(4) * Math.PI / 2;
		}
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

	/**
	 * The mountain's height {@code r} out (by bearing, no seam): a steep peak, crowned; spurs standing out of its middle slopes with gullies
	 * between (sharp-crested, never lower than the slope itself, none at its foot over the abode's outer halls), its face rough.
	 */
	static double mountain(Shape s, double lx, double lz, double r) {
		double t = Mth.clamp(1 - r / MOUNT_R, 0, 1);
		double cos = r < 1.0e-6 ? 1 : lx / r, sin = r < 1.0e-6 ? 0 : lz / r;
		double crest = 1 - Math.abs(s.ridge.getValue(cos * 2.2 + 7.3, sin * 2.2 - 3.1));
		double spurs = 18 * crest * crest * crest * Math.sin(Math.PI * Mth.clamp((t - 0.12) / 0.8, 0, 1));
		double ridges = 6 * s.ridge.getValue(cos * 3.4, sin * 3.4) * t + 3 * s.ridge.getValue(lx * 0.06, lz * 0.06) + 2.5 * s.crag.getValue(lx * 0.15, lz * 0.15);
		return FLOOR_Y + (PEAK_Y - FLOOR_Y) * Math.pow(t, 0.8) + spurs + ridges;
	}

	/** The column at (lx, lz) from the abode's middle (SecretRealm#centre: the mountain's). */
	public static Column column(Shape s, int lx, int lz) {
		double r = Math.hypot(lx, lz);
		if (r > EDGE_R + 4 * s.roll.getValue(lx * 0.02, lz * 0.02)) return new Column(-1, Kind.NONE);
		double roll = 1.8 * s.roll.getValue(lx * 0.03, lz * 0.03) + 0.6 * s.roll.getValue(lx * 0.1, lz * 0.1);
		if (r < MOUNT_R) return new Column((int) Math.round(Math.max(FLOOR_Y, mountain(s, lx, lz, r))), Kind.MOUNTAIN);
		// The rim: grey crags rising past the basin's edge, falling away past the realm's end.
		double over = r - BASIN_R - 5 * s.crag.getValue(lx / Math.max(1, r) * 2.2, lz / Math.max(1, r) * 2.2);
		if (over > 0) {
			double up = 26 * Mth.clamp(over / 14, 0, 1) + 6 * Math.abs(s.crag.getValue(lx * 0.07, lz * 0.07));
			double down = Math.max(0, r - (EDGE_R - 8)) * 3;
			return new Column((int) Math.round(FLOOR_Y + roll + up - down), Kind.CRAG);
		}
		// The lake before the gate: an oval on the main way, a little off the mountain's foot.
		double[] off = mainOff(s, lx, lz);
		double lake = Math.pow((off[1] - LAKE_R) / 16.0, 2) + Math.pow(off[0] / 24.0, 2) + 0.15 * s.roll.getValue(lx * 0.08, lz * 0.08);
		if (lake < 1) return new Column(WATER_Y - 1 - (int) Math.round(3 * (1 - lake)), Kind.LAKE);
		return new Column(FLOOR_Y + (int) Math.round(roll * Mth.clamp((r - MOUNT_R) / 8, 0, 1)), Kind.FLOOR);
	}

	// --- The places on it (in the abode's own coordinates) ---

	/** The arrivals on the basin's floor: their middles, on the ground (none on the lake's way). */
	public static List<BlockPos> arrivals(Shape s) {
		List<BlockPos> at = new ArrayList<>();
		for (int k = 0; k < 4; k++) {
			double b = s.mainTurn + Math.PI / 4 + k * Math.PI / 2;
			int x = (int) Math.round(Math.cos(b) * ARRIVAL_R), z = (int) Math.round(Math.sin(b) * ARRIVAL_R);
			at.add(new BlockPos(x, column(s, x, z).ground(), z));
		}
		return at;
	}

	private CaveAbode() {}
}
