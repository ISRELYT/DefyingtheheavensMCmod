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
 * The Verdant Herb Valley's land (SecretRealm#VERDANT_HERB_VALLEY), worked out column by column from the world's seed alone (so the
 * generator, SecretRealmGenerator, and whatever builds on it, SecretRealms and HerbValleyWorks, agree without a chunk loaded): a bowl
 * ringed by peaks, every world's its own.
 * <ul>
 *   <li><b>The heart</b> ({@link #HEART_R}): an island in a lake, level on top for the Pill Sage's hall, a bridge to it from the main
 *   path's side.</li>
 *   <li><b>The lake</b> (to {@link #LAKE_R}): still water at {@link #WATER_Y}, a few blocks deep.</li>
 *   <li><b>The middle ring</b> (to {@link #MIDDLE_R}): meadows rising gently from the shore, where the alchemists' sect's ruins stand.</li>
 *   <li><b>The outer ring</b> (to {@link #TERRACE_R}): terraced herb gardens climbing the valley's sides, a level every
 *   {@link #STEP} blocks up behind walls of old stone, their contours winding; herb beds on them ({@link Bed}).</li>
 *   <li><b>The rim</b> (to {@link #EDGE_R}): steep slopes of grey rock up to ridges, stone pillars standing out of them crowned with green;
 *   past it, nothing (the realm's end, hidden in the mist).</li>
 *   <li><b>The paths</b>: {@link #PATHS} ways down the valley's sides from the arrival terraces at the rim's foot to the lake, ramps of
 *   stone steps through the terraces; <b>the streams</b>: {@link #STREAMS} brooks from springs at the rim's foot, falling from terrace to
 *   terrace, into the lake.</li>
 * </ul>
 */
public final class HerbValley {
	/** The valley's land, laid block by block (HerbValleyLand). */
	public static final RealmLand LAND = new HerbValleyLand();
	/** The lake's surface; the island's top; the middle ring's foot and head; the terraces' foot. */
	public static final int WATER_Y = 72, ISLAND_Y = 77, MEADOW_FOOT = 73, TERRACE_FOOT = 84;
	/** The rings' outer edges, from the middle. */
	public static final int HEART_R = 15, LAKE_R = 34, MIDDLE_R = 72, TERRACE_R = 120, EDGE_R = 164;
	/** The island's level top reaches this far out. */
	public static final int ISLAND_FLAT = 11;
	/** A terrace's height over the one below it; the slope they climb (blocks up a block out). */
	public static final int STEP = 4;
	static final double SLOPE = 0.75;
	/** The ways down from the rim's foot, and the brooks. */
	public static final int PATHS = 8, STREAMS = 3;
	/** A path is this wide either side of its line; a brook this wide. */
	static final double PATH_HALF = 1.6, STREAM_HALF = 1.1;
	/** Where the arrival terraces stand (the paths' heads), out from the middle. */
	public static final int ARRIVAL_R = TERRACE_R + 3;
	/** How many herb beds on the terraces, each so long (blocks along its terrace). */
	public static final int BEDS = 24;
	static final double BED_LENGTH = 9;

	/** What a column is. */
	public enum Kind { ISLAND, SHORE, LAKE, MEADOW, PATH, TERRACE, BED, WALL, STREAM, RIM, PILLAR, NONE }

	/** One column: its ground's top (the highest solid block), its water's top (below the ground where there is none), and its kind. */
	public record Column(int ground, int water, Kind kind) {
		public boolean wet() { return water > ground; }
	}

	/** A herb bed on a terrace: its middle, the terrace's level (0 the lowest), the bearing it lies at, and the herbs' spots on it. */
	public record Bed(int index, int level, double bearing, BlockPos middle, List<BlockPos> herbs) {}

	/** A stone pillar of the rim: its foot's middle, its width, and its top. */
	record Pillar(double x, double z, double radius, int top) {}

	/** A world's valley: its noises, the paths' and streams' bearings, its beds and pillars (made once a seed). */
	public static final class Shape {
		final SimplexNoise warp, hills, ridges, grain;
		/** The first path's bearing (radians, a quarter turn's whole): the main way, toward the bridge. */
		public final double pathTurn;
		final double[] streams = new double[STREAMS], streamPhase = new double[STREAMS];
		final List<Pillar> pillars = new ArrayList<>();
		public final List<Bed> beds = new ArrayList<>();
		final long seed;

		Shape(long seed) {
			this.seed = seed;
			RandomSource random = RandomSource.create(seed ^ 0x5EC2E7L);
			warp = new SimplexNoise(random);
			hills = new SimplexNoise(random);
			ridges = new SimplexNoise(random);
			grain = new SimplexNoise(random);
			pathTurn = random.nextInt(4) * Math.PI / 2; // the main way runs along x or z (its bridge, the hall it leads to, built square to it)
			// The brooks, each in an eighth between two paths, in three of the eight (never next to one another).
			int[] eighths = {0, 3, 5};
			int turn = random.nextInt(8);
			for (int i = 0; i < STREAMS; i++) {
				streams[i] = pathTurn + ((eighths[i] + turn) % 8 + 0.5) * Math.PI * 2 / PATHS;
				streamPhase[i] = random.nextDouble() * Math.PI * 2;
			}
			// The rim's pillars: on a jittered ring, two deep.
			for (int ring = 0; ring < 2; ring++) {
				int count = ring == 0 ? 22 : 18;
				for (int i = 0; i < count; i++) {
					double a = (i + 0.5 * ring + random.nextDouble() * 0.6) * Math.PI * 2 / count;
					double r = (ring == 0 ? TERRACE_R + 16 : TERRACE_R + 30) + random.nextDouble() * 8;
					pillars.add(new Pillar(Math.cos(a) * r, Math.sin(a) * r, 2.6 + random.nextDouble() * 3.2, 150 + random.nextInt(ring == 0 ? 36 : 52)));
				}
			}
			layBeds(random);
		}

		/** The beds: three in each eighth of the terraces, on levels apart, clear of its paths and brooks. */
		private void layBeds(RandomSource random) {
			int levels = levels();
			for (int eighth = 0; eighth < PATHS; eighth++) {
				List<Integer> free = new ArrayList<>();
				for (int l = 1; l < levels - 1; l++) free.add(l);
				Collections.shuffle(free, new java.util.Random(random.nextLong()));
				for (int k = 0; k < BEDS / PATHS; k++) {
					int level = free.get(k % free.size());
					// Its bearing within the eighth, kept off the paths at either side (and off a brook in it).
					double from = pathTurn + eighth * Math.PI * 2 / PATHS;
					double span = Math.PI * 2 / PATHS;
					double bearing = from + span * (0.2 + 0.6 * random.nextDouble());
					double mid = middleOf(bearing, level);
					for (int tries = 0; tries < 6 && nearStream(bearing, mid, BED_LENGTH / 2 + 3); tries++) {
						bearing = from + span * (0.2 + 0.6 * random.nextDouble());
						mid = middleOf(bearing, level);
					}
					int x = (int) Math.round(Math.cos(bearing) * mid), z = (int) Math.round(Math.sin(bearing) * mid);
					beds.add(new Bed(beds.size(), level, bearing, new BlockPos(x, 0, z), new ArrayList<>()));
				}
			}
		}

		/**
		 * How far out along {@code bearing} the middle of terrace {@code level} lies: where its contours wind it (windingRadius), not where a
		 * compass would draw it, so a bed's middle is on its own soil.
		 */
		private double middleOf(double bearing, int level) {
			double cos = Math.cos(bearing), sin = Math.sin(bearing);
			double best = MIDDLE_R + (level + 0.5) * STEP / SLOPE, closest = Double.MAX_VALUE;
			for (double r = MIDDLE_R; r < TERRACE_R; r += 0.25) {
				double rr = windingRadius(this, cos * r, sin * r);
				if (rr <= MIDDLE_R || rr >= TERRACE_R || terraceLevel(rr) != level) continue;
				double f = (terraceBase(rr) - TERRACE_FOOT) / STEP - level;
				if (Math.abs(f - 0.45) < closest) {
					closest = Math.abs(f - 0.45);
					best = r;
				}
			}
			return best;
		}

		/** How many terrace levels the outer ring holds. */
		int levels() { return (int) Math.floor((TERRACE_R - MIDDLE_R) * SLOPE / STEP); }

		/** Whether a point {@code r} out on bearing {@code bearing} lies within {@code reach} of a brook. */
		boolean nearStream(double bearing, double r, double reach) {
			for (int i = 0; i < STREAMS; i++) {
				double line = streamLine(i, r);
				if (Math.abs(Mth.wrapDegrees(Math.toDegrees(bearing - line))) * Math.PI / 180 * r < reach) return true;
			}
			return false;
		}

		/** Brook {@code i}'s bearing {@code r} out: winding a little about its own. */
		double streamLine(int i, double r) {
			return streams[i] + 0.07 * Math.sin(r * 0.08 + streamPhase[i]);
		}
	}

	private static final Map<Long, Shape> SHAPES = Collections.synchronizedMap(new HashMap<>());

	/** The valley of the world whose seed is {@code seed}. */
	public static Shape shape(long seed) {
		return SHAPES.computeIfAbsent(seed, Shape::new);
	}

	// --- A column ---

	/** How far out a column lies, its contours winding (so the rings and terraces aren't drawn with a compass). */
	static double windingRadius(Shape s, double lx, double lz) {
		double r = Math.sqrt(lx * lx + lz * lz);
		if (r < 1) return r;
		double cos = lx / r, sin = lz / r;
		double wind = 4.5 * s.warp.getValue(cos * 1.9, sin * 1.9) + 2.0 * s.warp.getValue(lx * 0.035, lz * 0.035);
		// Little winding at the heart (the island stays round), more further out.
		return r + wind * Mth.clamp((r - HEART_R) / 30.0, 0, 1);
	}

	/** Perpendicular distance from (lx, lz) to the ray from the middle on {@code bearing} (infinite where it lies behind). */
	static double offLine(double lx, double lz, double bearing) {
		double cos = Math.cos(bearing), sin = Math.sin(bearing);
		double along = lx * cos + lz * sin;
		if (along < 0) return Double.MAX_VALUE;
		return Math.abs(-lx * sin + lz * cos);
	}

	/** Whether (lx, lz) lies on one of the paths: which (0 the main), or -1. */
	public static int pathAt(Shape s, double lx, double lz) {
		for (int k = 0; k < PATHS; k++) if (offLine(lx, lz, pathBearing(s, k)) < PATH_HALF) return k;
		return -1;
	}

	public static double pathBearing(Shape s, int k) {
		return s.pathTurn + k * Math.PI * 2 / PATHS;
	}

	/** Whether (lx, lz), {@code r} out, lies in a brook. */
	static boolean inStream(Shape s, double lx, double lz, double r) {
		for (int i = 0; i < STREAMS; i++) if (offLine(lx, lz, s.streamLine(i, r)) < STREAM_HALF) return true;
		return false;
	}

	/** The terraces' slope {@code rr} out (before it is cut into levels). */
	static double terraceBase(double rr) {
		return TERRACE_FOOT + (rr - MIDDLE_R) * SLOPE;
	}

	/** The terrace level (0 the lowest) a column {@code rr} out stands on. */
	public static int terraceLevel(double rr) {
		return Math.max(0, (int) Math.floor((terraceBase(rr) - TERRACE_FOOT) / STEP));
	}

	/**
	 * The column at (lx, lz) from the valley's middle (the realm's, SecretRealm#centre). Every ring's ground and water, the paths ramped
	 * through the terraces, the brooks cut into whatever they cross, the beds marked on their terraces, the rim's ridges and pillars.
	 */
	public static Column column(Shape s, int lx, int lz) {
		double r = Math.sqrt((double) lx * lx + (double) lz * lz);
		double rr = windingRadius(s, lx, lz);
		if (rr > EDGE_R + 3 * s.ridges.getValue(lx * 0.02, lz * 0.02)) return new Column(-1, -1, Kind.NONE);
		Column c = ground(s, lx, lz, r, rr);
		// The brooks: cut a block into the ground (the island and the lake aside), their water at the cut's brim.
		if (c.kind != Kind.LAKE && c.kind != Kind.ISLAND && c.kind != Kind.PILLAR && rr > LAKE_R - 1 && rr < TERRACE_R + 9 && inStream(s, lx, lz, r)) {
			int g = c.ground - 1;
			return new Column(g - 1, g, Kind.STREAM);
		}
		return c;
	}

	private static Column ground(Shape s, int lx, int lz, double r, double rr) {
		if (rr < HEART_R) {
			// The island: level within ISLAND_FLAT for the hall, falling to the shore.
			if (rr <= ISLAND_FLAT) return new Column(ISLAND_Y, -1, Kind.ISLAND);
			double t = (rr - ISLAND_FLAT) / (HEART_R - ISLAND_FLAT);
			int g = (int) Math.round(ISLAND_Y - t * (ISLAND_Y - (WATER_Y - 1)));
			return new Column(g, WATER_Y, g <= WATER_Y ? Kind.SHORE : Kind.ISLAND);
		}
		if (rr < LAKE_R) {
			double t = (rr - HEART_R) / (LAKE_R - HEART_R);
			double deep = Math.sin(Math.PI * t) * (4.5 + 1.5 * s.hills.getValue(lx * 0.06, lz * 0.06));
			int g = (int) Math.round(WATER_Y - 1 - deep);
			return new Column(g, WATER_Y, t > 0.9 || t < 0.08 ? Kind.SHORE : Kind.LAKE);
		}
		int path = pathAt(s, lx, lz);
		if (rr < MIDDLE_R) {
			// The meadows: rising from the shore to the terraces' foot, rolling a little.
			double t = (rr - LAKE_R) / (MIDDLE_R - LAKE_R);
			double rise = MEADOW_FOOT + t * (TERRACE_FOOT - 1 - MEADOW_FOOT);
			// (Level at either edge: the shore, and the terraces' foot, where the paths climb on a step at a time.)
			double roll = 1.8 * s.hills.getValue(lx * 0.045, lz * 0.045) * Mth.clamp((rr - LAKE_R) / 6.0, 0, 1) * Mth.clamp((MIDDLE_R - rr) / 6.0, 0, 1);
			int g = (int) Math.round(rise + roll);
			if (rr < LAKE_R + 3) return new Column(Math.min(g, WATER_Y + 1), WATER_Y, Kind.SHORE);
			return new Column(g, -1, path >= 0 ? Kind.PATH : Kind.MEADOW);
		}
		if (rr < TERRACE_R) {
			double base = terraceBase(rr);
			// A path ramps straight up through the terraces, a step at a time; elsewhere the slope is cut into level terraces.
			if (path >= 0) return new Column((int) Math.round(base - 0.5), -1, Kind.PATH);
			int level = terraceLevel(rr);
			int g = TERRACE_FOOT + level * STEP;
			// Where on its terrace (0 at its foot's edge, 1 at the wall up to the next), and whether a bed lies there.
			double f = (base - TERRACE_FOOT) / STEP - level;
			Bed bed = f > 0.18 && f < 0.72 ? bedAt(s, lx, lz, level) : null;
			return new Column(g, -1, bed != null ? Kind.BED : Kind.TERRACE);
		}
		// The rim: the slope steepening up into ridges, then falling away to the realm's end; its pillars standing out of it.
		double over = rr - TERRACE_R;
		double ridge = 1 - Math.abs(s.ridges.getValue(lx * 0.03, lz * 0.03));
		double up = 30 * smooth(over / 30) + 14 * ridge * smooth(over / 20) + 3 * s.grain.getValue(lx * 0.12, lz * 0.12) * smooth(over / 10);
		double down = Math.max(0, rr - (EDGE_R - 12)) * 2.6;
		int g = (int) Math.round(TERRACE_FOOT + (TERRACE_R - MIDDLE_R) * SLOPE + up - down);
		// A path ramps on up to its arrival terrace however far its contour winds out there (its head a straight ARRIVAL_R out).
		if (path >= 0 && (over < 6 || r <= ARRIVAL_R + 3)) return new Column((int) Math.round(terraceBase(rr) - 0.5), -1, Kind.PATH);
		for (Pillar p : s.pillars) {
			double dx = lx - p.x, dz = lz - p.z;
			double wobble = p.radius + 0.7 * s.grain.getValue(lx * 0.3, lz * 0.3);
			if (dx * dx + dz * dz < wobble * wobble) return new Column(Math.max(g, p.top), -1, Kind.PILLAR);
		}
		return new Column(g, -1, Kind.RIM);
	}

	private static double smooth(double t) {
		t = Mth.clamp(t, 0, 1);
		return t * t * (3 - 2 * t);
	}

	/** The bed on terrace {@code level} that (lx, lz) lies in, or null. */
	static Bed bedAt(Shape s, double lx, double lz, int level) {
		double r = Math.sqrt(lx * lx + lz * lz);
		double bearing = Math.atan2(lz, lx);
		for (Bed bed : s.beds) {
			if (bed.level != level) continue;
			double off = Math.abs(Mth.wrapDegrees(Math.toDegrees(bearing - bed.bearing))) * Math.PI / 180 * r;
			if (off < BED_LENGTH / 2) return bed;
		}
		return null;
	}

	// --- The places on it (in the realm's own coordinates: from its middle) ---

	/** The arrival terraces at the paths' heads, where those who come in are set down: their middles, on the ground. */
	public static List<BlockPos> arrivals(Shape s) {
		List<BlockPos> at = new ArrayList<>();
		for (int k = 0; k < PATHS; k++) {
			double b = pathBearing(s, k);
			int x = (int) Math.round(Math.cos(b) * ARRIVAL_R), z = (int) Math.round(Math.sin(b) * ARRIVAL_R);
			at.add(new BlockPos(x, column(s, x, z).ground(), z));
		}
		return at;
	}

	/** Where along a bed its herbs stand: four spots on its soil, spread along it (the herbs on top of them). */
	public static List<BlockPos> herbSpots(Shape s, Bed bed) {
		if (!bed.herbs.isEmpty()) return bed.herbs;
		synchronized (bed.herbs) {
			if (!bed.herbs.isEmpty()) return bed.herbs;
			double mid = Math.sqrt((double) bed.middle.getX() * bed.middle.getX() + (double) bed.middle.getZ() * bed.middle.getZ());
			RandomSource random = RandomSource.create(s.seed ^ (bed.index * 0x9E3779B97F4A7C15L));
			List<BlockPos> spots = new ArrayList<>();
			for (int i = 0; i < 4; i++) {
				double along = (i - 1.5) / 4 * (BED_LENGTH - 2) + (random.nextDouble() - 0.5) * 0.8;
				double a = bed.bearing + along / mid;
				double jitter = (random.nextDouble() - 0.5) * 2.2;
				// Out from there to either side till on the bed's own soil (its terrace winds a little along it).
				for (int step = 0; step <= 16; step++) {
					double r = mid + jitter + (step % 2 == 0 ? 1 : -1) * ((step + 1) / 2) * 0.5;
					int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
					Column c = column(s, x, z);
					if (c.kind() == Kind.BED && terraceLevel(windingRadius(s, x, z)) == bed.level) {
						BlockPos spot = new BlockPos(x, c.ground() + 1, z);
						if (!spots.contains(spot)) {
							spots.add(spot);
							break;
						}
					}
				}
			}
			bed.herbs.addAll(spots);
			return bed.herbs;
		}
	}

	private HerbValley() {}
}
