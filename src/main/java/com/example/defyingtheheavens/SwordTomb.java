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
 * The Sunken Sword Tomb's land (SecretRealm#SUNKEN_SWORD_TOMB), worked out column by column from the world's seed alone (SwordTombLand lays
 * it, SwordTombWorks builds on it): a plateau of grey stone with a gorge sunk deep into it, every world's its own.
 * <ul>
 *   <li><b>The plateau</b> (to {@link #EDGE_R}): bare grey rock at {@link #PLATEAU_Y}, cracked and broken by outcrops; past its edge the
 *   realm's end, hidden in the mist.</li>
 *   <li><b>The gorge</b>: a ring of canyon ({@link #RING_IN} to {@link #RING_OUT}) sunk to {@link #FLOOR_Y}, its walls sheer with ledges, its
 *   floor strewn with the swords of a sect that fell here (BuriedSwordBlock); {@link #BRANCHES} branch canyons running out from it, their
 *   floors ramping up to the plateau: the ways down from the arrival ledges at their heads.</li>
 *   <li><b>The spire</b> (within {@link #SPIRE_R}): a pillar of rock standing up out of the ring to {@link #SUMMIT_Y}, the Sleeping Sword's
 *   shrine on its top, the Sword Path cut into its face: a ledge of steps winding up three-quarters round it from the gorge's floor to the
 *   summit ({@link #pathAt}).</li>
 * </ul>
 */
public final class SwordTomb {
	/** The tomb's land, laid block by block (SwordTombLand). */
	public static final RealmLand LAND = new SwordTombLand();
	/** The plateau's top, the gorge's floor, the spire's summit. */
	public static final int PLATEAU_Y = 112, FLOOR_Y = 58, SUMMIT_Y = 128;
	/** The spire's flank; the gorge's ring, inner and outer; where the plateau ends. */
	public static final int SPIRE_R = 31, RING_IN = 34, RING_OUT = 80, EDGE_R = 164;
	/** The Sword Path's ledge: cut this far into the spire (from its flank), turning this far (degrees) as it climbs from the floor to the summit. */
	public static final double PATH_IN = 3.6, PATH_TURN = 300;
	/** The branch canyons running out to the arrival ledges, how wide each (blocks either side of its line), and where their heads are. */
	public static final int BRANCHES = 4, BRANCH_HALF = 7, ARRIVAL_R = 146;

	/** What a column is. */
	public enum Kind { PLATEAU, WALL, LEDGE, FLOOR, BRANCH, SPIRE, PATH, SUMMIT, NONE }

	/** One column: its ground's top and its kind. */
	public record Column(int ground, Kind kind) {}

	/** A world's tomb: its noises, the main way's bearing (the first branch's, the Sword Path's foot), and the branches'. */
	public static final class Shape {
		final SimplexNoise rough, wall, cracks;
		/** The main way: the first branch's bearing (a quarter turn's whole), the Sword Path starting at the spire's foot facing it. */
		public final double mainTurn;
		final long seed;

		Shape(long seed) {
			this.seed = seed;
			RandomSource random = RandomSource.create(seed ^ 0x53D70BL);
			rough = new SimplexNoise(random);
			wall = new SimplexNoise(random);
			cracks = new SimplexNoise(random);
			mainTurn = random.nextInt(4) * Math.PI / 2;
		}

		/** Branch {@code k}'s bearing (k 0 the main). */
		public double branch(int k) { return mainTurn + k * Math.PI * 2 / BRANCHES; }
	}

	private static final Map<Long, Shape> SHAPES = Collections.synchronizedMap(new HashMap<>());

	public static Shape shape(long seed) {
		return SHAPES.computeIfAbsent(seed, Shape::new);
	}

	/** The Sword Path's foot, in degrees from +x round toward +z: the main way's bearing turned back a little. */
	static double pathStart(Shape s) {
		return Math.toDegrees(s.mainTurn) + 30;
	}

	/** How far round the spire (lx, lz) lies from the Sword Path's foot (0 to 360). */
	static double roundFromStart(Shape s, double lx, double lz) {
		double deg = Math.toDegrees(Math.atan2(lz, lx));
		return Mth.positiveModulo(deg - pathStart(s), 360);
	}

	/** The Sword Path's height {@code turn} degrees round from its foot (rising evenly from the floor to the summit). */
	public static int pathHeight(double turn) {
		return FLOOR_Y + (int) Math.round(Mth.clamp(turn / PATH_TURN, 0, 1) * (SUMMIT_Y - FLOOR_Y));
	}

	/**
	 * How far out the spire's rock face stands behind the Sword Path at (lx, lz)'s bearing: a little in or out round it (by the bearing's
	 * cosine and sine: no seam), so the spire is no smooth pillar and the ledge is wider in places.
	 */
	static double spireFace(Shape s, double lx, double lz) {
		double r = Math.max(1.0e-6, Math.hypot(lx, lz));
		return SPIRE_R - PATH_IN - 0.5 - 1.8 * (0.5 + 0.5 * s.wall.getValue(lx / r * 3.3 + 17, lz / r * 3.3 + 17));
	}

	/** Whether (lx, lz) lies on the Sword Path's ledge (from the spire's face out to its flank); the turn there if so, else -1. */
	public static double pathAt(Shape s, double lx, double lz) {
		double r = Math.hypot(lx, lz);
		if (r > SPIRE_R + 0.5 || r < spireFace(s, lx, lz)) return -1;
		double turn = roundFromStart(s, lx, lz);
		return turn <= PATH_TURN ? turn : -1;
	}

	/** How far (lx, lz) lies off the line of branch {@code k}, and how far out along it (negative behind the middle). */
	static double[] branchOff(Shape s, int k, double lx, double lz) {
		double b = s.branch(k), cos = Math.cos(b), sin = Math.sin(b);
		return new double[] {Math.abs(-lx * sin + lz * cos), lx * cos + lz * sin};
	}

	/** Which branch canyon (lx, lz) lies in (its winding sides), or -1. */
	static int branchAt(Shape s, double lx, double lz) {
		for (int k = 0; k < BRANCHES; k++) {
			double[] o = branchOff(s, k, lx, lz);
			double half = BRANCH_HALF + 2.5 * s.wall.getValue(o[1] * 0.05, k * 7.0);
			if (o[1] > RING_OUT - 10 && o[1] < ARRIVAL_R + 4 && o[0] < half) return k;
		}
		return -1;
	}

	/**
	 * A branch's floor {@code along} out: from the gorge's floor where the ring's outer wall stands there ({@code outer}: it winds) to the
	 * plateau just short of its head, a ramp of steps a block at most each.
	 */
	static int branchFloor(double along, double outer) {
		double t = Mth.clamp((along - outer) / (ARRIVAL_R - 2 - outer), 0, 1);
		return FLOOR_Y + (int) Math.round(t * (PLATEAU_Y - FLOOR_Y));
	}

	/**
	 * The column at (lx, lz) from the tomb's middle (SecretRealm#centre): the plateau, the gorge's walls and their ledges, its floor, the branch
	 * canyons' ramps, the spire, its path and summit.
	 */
	public static Column column(Shape s, int lx, int lz) {
		double r = Math.hypot(lx, lz);
		if (r > EDGE_R + 4 * s.rough.getValue(lx * 0.02, lz * 0.02)) return new Column(-1, Kind.NONE);
		// The spire: its summit, its flank, the Sword Path cut into it.
		if (r <= SPIRE_R) {
			double turn = pathAt(s, lx, lz);
			if (turn >= 0) return new Column(pathHeight(turn), Kind.PATH);
			if (r < spireFace(s, lx, lz)) return new Column(SUMMIT_Y + (r < 14 ? 0 : (int) Math.round(1.5 * s.rough.getValue(lx * 0.2, lz * 0.2))), Kind.SUMMIT);
			return new Column(SUMMIT_Y - 1, Kind.SPIRE);
		}
		double rough = s.rough.getValue(lx * 0.04, lz * 0.04);
		int floor = FLOOR_Y + (int) Math.round(1.6 * rough + 0.8 * s.cracks.getValue(lx * 0.15, lz * 0.15));
		// The ring of the gorge, its outer wall winding (by its bearing's cosine and sine: no seam where the angle wraps round).
		double cos = lx / r, sin = lz / r;
		double outer = RING_OUT + 6 * s.wall.getValue(cos * 2.2, sin * 2.2) + 2 * s.wall.getValue(lx * 0.03, lz * 0.03);
		if (r < RING_IN) return new Column(floor, Kind.FLOOR);
		if (r < outer) return new Column(floor, Kind.FLOOR);
		int branch = branchAt(s, lx, lz);
		if (branch >= 0) {
			double along = branchOff(s, branch, lx, lz)[1];
			return new Column(Math.max(floor, branchFloor(along, outer)), Kind.BRANCH);
		}
		// The gorge's outer wall: sheer, broken by ledges every so far up, to the plateau.
		double over = r - outer;
		if (over < 14) {
			int ledges = (int) Math.floor(over / 3.5);
			int g = FLOOR_Y + (int) Math.round((PLATEAU_Y - FLOOR_Y) * Math.min(1, (ledges + 1) / 4.0)) - 2 + (int) Math.round(rough);
			return new Column(Math.min(g, PLATEAU_Y), over - ledges * 3.5 < 1.2 ? Kind.LEDGE : Kind.WALL);
		}
		// The plateau: bare rock, cracked, an outcrop here and there; falling away past its edge.
		double outcrop = Math.max(0, s.cracks.getValue(lx * 0.06, lz * 0.06) - 0.45) * 22;
		int g = PLATEAU_Y + (int) Math.round(1.5 * rough + outcrop) - (int) Math.max(0, (r - (EDGE_R - 10)) * 2.4);
		return new Column(g, Kind.PLATEAU);
	}

	// --- The places on it (in the tomb's own coordinates: from its middle) ---

	/** The arrival ledges at the branch canyons' heads, on the plateau: their middles, on the ground. */
	public static List<BlockPos> arrivals(Shape s) {
		List<BlockPos> at = new ArrayList<>();
		for (int k = 0; k < BRANCHES; k++) {
			double b = s.branch(k);
			int x = (int) Math.round(Math.cos(b) * ARRIVAL_R), z = (int) Math.round(Math.sin(b) * ARRIVAL_R);
			at.add(new BlockPos(x, Math.max(column(s, x, z).ground(), PLATEAU_Y), z));
		}
		return at;
	}

	/** The Sword Path's foot (on the gorge's floor, before its first step) and its head (on the summit, at its last). */
	public static BlockPos pathFoot(Shape s) {
		double a = Math.toRadians(pathStart(s) - 6), r = SPIRE_R + 3;
		int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
		return new BlockPos(x, column(s, x, z).ground() + 1, z);
	}

	/** The middle of the Sword Path's ledge {@code turn} degrees round from its foot, standing on it (local). */
	public static BlockPos pathPoint(Shape s, double turn) {
		double a = Math.toRadians(pathStart(s) + turn), r = SPIRE_R - PATH_IN / 2;
		return new BlockPos((int) Math.round(Math.cos(a) * r), pathHeight(turn) + 1, (int) Math.round(Math.sin(a) * r));
	}

	public static BlockPos pathHead(Shape s) {
		double a = Math.toRadians(pathStart(s) + PATH_TURN), r = SPIRE_R - PATH_IN - 3;
		int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
		return new BlockPos(x, SUMMIT_Y + 1, z);
	}

	private SwordTomb() {}
}
