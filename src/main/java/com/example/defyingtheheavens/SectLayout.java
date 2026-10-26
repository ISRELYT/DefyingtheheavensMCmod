package com.example.defyingtheheavens;

import net.minecraft.util.RandomSource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * The ground plan of a sect (see SectStructure): open grass in a rounded square round the Formation plaza, with no wall, and on
 * each column of it what the grounds piece lays there (SectPiece#terrace): grass, a paved way, a footpath, a stone lantern, a
 * tree, bamboo. It also keeps track of where the buildings stand, so they keep their distance and every door gets a footpath
 * to the ways.
 * <p>
 * Coordinates are SectStructure's frame: u across the grounds from left to right, v from the front inward, the plaza (and the
 * core) at the middle, ({@link #a}, {@link #b}).
 */
public final class SectLayout {
	/** What the grounds piece lays on a column. */
	public static final byte OUT = 0, GRASS = 1, PATH = 2, PATH_EDGE = 3, FOOTPATH = 4, LANTERN = 5, TREE = 6, BAMBOO = 7, PLAZA = 8;

	/** How far out a building may reach, in {@link #outline}'s measure (1 is the edge of the grounds). */
	private static final double BUILD_LIMIT = 0.96;
	/** The longest footpath a door may need to reach the ways; a house that would need more is built elsewhere. */
	private static final int MAX_FOOTPATH = 48;

	/** What a column is used for while planning (RESERVED: where a gate stands, kept free of houses, trees and bamboo). */
	private static final byte OPEN = 0, BUILDING = 1, CLEAR = 2, WAY = 3, RESERVED = 4;

	/** The way a building extends from its door, in the frame (BACK: away from the gate, its door facing the gate). */
	public enum Side {
		BACK, FRONT, LEFT, RIGHT;

		boolean alongV() { return this == BACK || this == FRONT; }
	}

	/** A building's plot: its corners in the frame and the way it extends from its door. */
	public record Lot(int u0, int v0, int u1, int v1, Side side) {
		/** The column just outside the middle of its front. */
		public int[] doorstep() {
			return switch (side) {
				case BACK -> new int[] {(u0 + u1) / 2, v0 - 1};
				case FRONT -> new int[] {(u0 + u1) / 2, v1 + 1};
				case RIGHT -> new int[] {u0 - 1, (v0 + v1) / 2};
				case LEFT -> new int[] {u1 + 1, (v0 + v1) / 2};
			};
		}

		/** The column {@code in} rows inside the front, {@code across} from its middle. */
		public int[] inside(int in, int across) {
			return switch (side) {
				case BACK -> new int[] {(u0 + u1) / 2 + across, v0 + in};
				case FRONT -> new int[] {(u0 + u1) / 2 + across, v1 - in};
				case RIGHT -> new int[] {u0 + in, (v0 + v1) / 2 + across};
				case LEFT -> new int[] {u1 - in, (v0 + v1) / 2 + across};
			};
		}

		/** A plot {@code width} across its front and {@code depth} deep, centred on (u, v). */
		public static Lot centred(int u, int v, int width, int depth, Side side) {
			int du = side.alongV() ? width : depth, dv = side.alongV() ? depth : width;
			return new Lot(u - du / 2, v - dv / 2, u - du / 2 + du - 1, v - dv / 2 + dv - 1, side);
		}
	}

	public final int a, b, w, d;
	/**
	 * How far from the middle any building may reach: the barrier closes round the buildings, so they stay inside it while
	 * the grounds, the ways and the gates reach on beyond it.
	 */
	public final int buildRadius;
	private final long seed;
	/** The grounds piece's map: one code per column, u + v * w. */
	public final byte[] map;
	private final byte[] use;

	public SectLayout(int a, int b, int buildRadius, long seed) {
		this.a = a;
		this.b = b;
		this.w = 2 * a + 1;
		this.d = 2 * b + 1;
		this.buildRadius = buildRadius;
		this.seed = seed;
		map = new byte[w * d];
		use = new byte[w * d];
		for (int u = 0; u < w; u++) {
			for (int v = 0; v < d; v++) {
				if (outline(a, b, seed, u, v) <= 1) map[u + v * w] = GRASS;
			}
		}
	}

	/**
	 * How far out (u, v) lies on grounds of half-width {@code a} and half-depth {@code b}: 0 at the middle, 1 at the edge. The
	 * grounds are a rounded square whose edge wavers a little with the sect's seed, so the clearing doesn't look surveyed.
	 */
	public static double outline(int a, int b, long seed, double u, double v) {
		double x = (u - a) / (a + 0.5), z = (v - b) / (b + 0.5);
		double reach = Math.sqrt(Math.sqrt(x * x * x * x + z * z * z * z));
		double angle = Math.atan2(z, x);
		double phase1 = (seed >>> 8 & 0xFFFF) / 65536.0 * Math.PI * 2, phase2 = (seed >>> 24 & 0xFFFF) / 65536.0 * Math.PI * 2;
		return reach / (1 + 0.04 * Math.sin(3 * angle + phase1) + 0.03 * Math.sin(5 * angle + phase2));
	}

	private boolean inBounds(int u, int v) {
		return u >= 0 && u < w && v >= 0 && v < d;
	}

	private boolean inside(int u, int v) {
		return inBounds(u, v) && map[u + v * w] != OUT;
	}

	/** Whether (u, v) is open ground: on the grounds, and nothing planned on it yet (no way, building, lantern, tree or gate). */
	public boolean isOpen(int u, int v) {
		return inside(u, v) && use[u + v * w] == OPEN;
	}

	// --- Ways ---

	/** Lays a way (or its edging) on (u, v), even a little past the edge of the grounds (the gate's approach). */
	public void way(int u, int v, byte code) {
		if (!inBounds(u, v)) return;
		int i = u + v * w;
		if (use[i] == BUILDING) return;
		use[i] = WAY;
		if (map[i] != PATH) map[i] = code;
	}

	/** A straight way three wide with an edging either side, from (u0, v0) to (u1, v1) (one of them the same). */
	public void road(int u0, int v0, int u1, int v1, int halfWidth) {
		boolean alongV = u0 == u1;
		for (int u = Math.min(u0, u1); u <= Math.max(u0, u1); u++) {
			for (int v = Math.min(v0, v1); v <= Math.max(v0, v1); v++) {
				for (int k = -halfWidth - 1; k <= halfWidth + 1; k++) {
					byte code = Math.abs(k) > halfWidth ? PATH_EDGE : PATH;
					if (alongV) way(u + k, v, code);
					else way(u, v + k, code);
				}
			}
		}
	}

	/** The round plaza at the middle (the plaza piece paves it). */
	public void plaza(int radius) {
		for (int u = a - radius; u <= a + radius; u++) {
			for (int v = b - radius; v <= b + radius; v++) {
				if (!inBounds(u, v) || (u - a) * (u - a) + (v - b) * (v - b) > (radius + 0.5) * (radius + 0.5)) continue;
				use[u + v * w] = WAY;
				map[u + v * w] = PLAZA;
			}
		}
	}

	/** A stone lantern on (u, v), if it's open ground. */
	public void lantern(int u, int v) {
		if (!inside(u, v) || use[u + v * w] != OPEN && use[u + v * w] != CLEAR) return;
		use[u + v * w] = CLEAR;
		map[u + v * w] = LANTERN;
	}

	/** Keeps a rectangle free of buildings, lanterns, trees and bamboo (where a gate stands), without changing what's laid there. */
	public void keepClear(int u0, int v0, int u1, int v1) {
		for (int u = u0; u <= u1; u++) {
			for (int v = v0; v <= v1; v++) {
				if (inBounds(u, v) && (use[u + v * w] == OPEN || use[u + v * w] == CLEAR)) use[u + v * w] = RESERVED;
			}
		}
	}

	/** Paves the columns of a lot before its door (its front steps, inside its plot) as footpath. */
	public void apron(Lot lot, int rows, int halfWidth) {
		for (int in = 0; in < rows; in++) {
			for (int k = -halfWidth; k <= halfWidth; k++) {
				int[] c = lot.inside(in, k);
				if (inBounds(c[0], c[1]) && map[c[0] + c[1] * w] != PATH && map[c[0] + c[1] * w] != PATH_EDGE) map[c[0] + c[1] * w] = FOOTPATH;
			}
		}
	}

	/**
	 * A footpath from (u, v) to the nearest way, around the buildings, two wide, joining the ways. @return false (and nothing
	 * laid) if no way can be reached in {@link #MAX_FOOTPATH} steps
	 */
	public boolean footpath(int u, int v) {
		if (!inside(u, v) || use[u + v * w] == BUILDING) return false;
		if (use[u + v * w] == WAY) return true;
		int[] from = new int[w * d];
		java.util.Arrays.fill(from, -1);
		int[] steps = new int[w * d];
		ArrayDeque<Integer> queue = new ArrayDeque<>();
		int start = u + v * w;
		from[start] = start;
		queue.add(start);
		int found = -1;
		while (!queue.isEmpty() && found < 0) {
			int at = queue.poll();
			if (steps[at] >= MAX_FOOTPATH) continue;
			int au = at % w, av = at / w;
			for (int[] step : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
				int nu = au + step[0], nv = av + step[1];
				if (!inside(nu, nv)) continue;
				int next = nu + nv * w;
				if (from[next] >= 0 || use[next] == BUILDING) continue;
				from[next] = at;
				steps[next] = steps[at] + 1;
				if (use[next] == WAY) {
					found = next;
					break;
				}
				queue.add(next);
			}
		}
		if (found < 0) return false;
		for (int at = from[found]; ; at = from[at]) {
			int au = at % w, av = at / w;
			for (int[] brush : new int[][] {{0, 0}, {1, 0}, {0, 1}, {1, 1}}) {
				int bu = au + brush[0], bv = av + brush[1];
				if (!inside(bu, bv)) continue;
				int i = bu + bv * w;
				if (use[i] == BUILDING || map[i] == LANTERN || use[i] == WAY && (brush[0] != 0 || brush[1] != 0)) continue;
				use[i] = WAY;
				if (map[i] == GRASS) map[i] = FOOTPATH;
			}
			if (at == start) break;
		}
		return true;
	}

	// --- Buildings ---

	/** Whether all of a lot lies within {@link #buildRadius} of the middle. */
	private boolean inBuildRadius(Lot lot) {
		for (int[] corner : new int[][] {{lot.u0(), lot.v0()}, {lot.u1(), lot.v0()}, {lot.u0(), lot.v1()}, {lot.u1(), lot.v1()}}) {
			if ((corner[0] - a) * (corner[0] - a) + (corner[1] - b) * (corner[1] - b) > buildRadius * buildRadius) return false;
		}
		return true;
	}

	/** Whether a lot lies wholly on open ground well inside the grounds, its door reachable and its ring free of buildings. */
	private boolean fits(Lot lot, int ring) {
		if (!inBuildRadius(lot)) return false;
		for (int[] corner : new int[][] {{lot.u0(), lot.v0()}, {lot.u1(), lot.v0()}, {lot.u0(), lot.v1()}, {lot.u1(), lot.v1()}}) {
			if (outline(a, b, seed, corner[0], corner[1]) > BUILD_LIMIT) return false;
		}
		for (int u = lot.u0() - ring; u <= lot.u1() + ring; u++) {
			for (int v = lot.v0() - ring; v <= lot.v1() + ring; v++) {
				boolean in = u >= lot.u0() && u <= lot.u1() && v >= lot.v0() && v <= lot.v1();
				if (in && (!inside(u, v) || use[u + v * w] != OPEN)) return false;
				if (!in && inBounds(u, v) && use[u + v * w] == BUILDING) return false;
			}
		}
		int[] step = lot.doorstep();
		return inside(step[0], step[1]) && use[step[0] + step[1] * w] != BUILDING;
	}

	/** Claims a lot for a building (whatever was planned there before) and keeps a ring of {@code ring} round it clear. */
	public void claim(Lot lot, int ring) {
		for (int u = lot.u0() - ring; u <= lot.u1() + ring; u++) {
			for (int v = lot.v0() - ring; v <= lot.v1() + ring; v++) {
				if (!inBounds(u, v)) continue;
				boolean in = u >= lot.u0() && u <= lot.u1() && v >= lot.v0() && v <= lot.v1();
				if (in) use[u + v * w] = BUILDING;
				else if (use[u + v * w] == OPEN) use[u + v * w] = CLEAR;
			}
		}
	}
	/**
	 * Whether a lot lies within {@link #buildRadius} and on the grounds no farther out than {@code limit} in {@link #outline}'s
	 * measure (for a garden).
	 */
	public boolean within(Lot lot, double limit) {
		if (!inBuildRadius(lot)) return false;
		for (int[] corner : new int[][] {{lot.u0(), lot.v0()}, {lot.u1(), lot.v0()}, {lot.u0(), lot.v1()}, {lot.u1(), lot.v1()}}) {
			if (outline(a, b, seed, corner[0], corner[1]) > limit) return false;
		}
		return true;
	}

	/**
	 * Sets {@code count} houses round the middle, the first {@code large} of them (the elders') larger: each facing the
	 * plaza, well apart from its neighbours (a lawn of seven to nine blocks between), with a footpath from its door to the
	 * ways. Spots are tried roughly in order of distance but with plenty of chance mixed in, so the houses scatter over the
	 * grounds rather than crowding the plaza or standing in rows. @return the lots, in the order laid (fewer only if the
	 * grounds run out)
	 */
	public List<Lot> houses(RandomSource random, int count, int large) {
		List<long[]> spots = new ArrayList<>();
		for (int u = 0; u < w; u++) {
			for (int v = 0; v < d; v++) {
				if (!inside(u, v) || use[u + v * w] != OPEN) continue;
				double key = Math.sqrt((u - a) * (u - a) + (v - b) * (v - b)) + random.nextFloat() * 30;
				spots.add(new long[] {(long) (key * 1000), u, v});
			}
		}
		spots.sort((x, y) -> Long.compare(x[0], y[0]));
		List<Lot> lots = new ArrayList<>();
		for (long[] spot : spots) {
			if (lots.size() >= count) break;
			int u = (int) spot[1], v = (int) spot[2];
			if (use[u + v * w] != OPEN) continue;
			boolean elder = lots.size() < large;
			int width = elder || random.nextInt(3) == 0 ? 9 : 7, depth = elder ? 9 : 7;
			int du = u - a, dv = v - b;
			boolean sideways = Math.abs(du) >= Math.abs(dv);
			Side across = du < 0 ? Side.LEFT : Side.RIGHT, along = dv < 0 ? Side.FRONT : Side.BACK;
			for (Side side : sideways ? new Side[] {across, along} : new Side[] {along, across}) {
				Lot lot = Lot.centred(u, v, width, depth, side);
				int ring = 7 + random.nextInt(3);
				if (!fits(lot, ring)) continue;
				claim(lot, 0);
				int[] step = lot.doorstep();
				if (!footpath(step[0], step[1])) {
					for (int x = lot.u0(); x <= lot.u1(); x++) {
						for (int z = lot.v0(); z <= lot.v1(); z++) use[x + z * w] = OPEN;
					}
					continue;
				}
				claim(lot, ring);
				lots.add(lot);
				break;
			}
		}
		return lots;
	}

	/**
	 * A lot {@code width} across its front and {@code depth} deep for an open-air building (the Altar of Heaven, the training
	 * ground), on open ground with a lawn of {@code ring} round it, its front facing the middle and a footpath to the ways: as
	 * near {@code from} blocks out from the middle as fits, with a little chance mixed in. @return the lot, or null if the
	 * grounds have no room for it
	 */
	public Lot place(RandomSource random, int width, int depth, int from, int ring) {
		List<long[]> spots = new ArrayList<>();
		for (int u = 0; u < w; u++) {
			for (int v = 0; v < d; v++) {
				if (!inside(u, v) || use[u + v * w] != OPEN) continue;
				double key = Math.abs(Math.sqrt((u - a) * (u - a) + (v - b) * (v - b)) - from) + random.nextFloat() * 8;
				spots.add(new long[] {(long) (key * 1000), u, v});
			}
		}
		spots.sort((x, y) -> Long.compare(x[0], y[0]));
		int tried = 0;
		for (long[] spot : spots) {
			int u = (int) spot[1], v = (int) spot[2];
			if (use[u + v * w] != OPEN) continue;
			if (++tried > 12000) break; // plenty of tries: the grounds just have no room
			int du = u - a, dv = v - b;
			Side side = Math.abs(du) >= Math.abs(dv) ? (du < 0 ? Side.LEFT : Side.RIGHT) : (dv < 0 ? Side.FRONT : Side.BACK);
			Lot lot = Lot.centred(u, v, width, depth, side);
			if (!fits(lot, ring)) continue;
			claim(lot, 0);
			int[] step = lot.doorstep();
			if (!footpath(step[0], step[1])) {
				for (int x = lot.u0(); x <= lot.u1(); x++) {
					for (int z = lot.v0(); z <= lot.v1(); z++) use[x + z * w] = OPEN;
				}
				continue;
			}
			claim(lot, ring);
			return lot;
		}
		return null;
	}

	// --- Trees and bamboo ---

	/** Whether no column used as {@code usage} lies within {@code r} of (u, v). */
	private boolean noneNear(int u, int v, int r, byte usage) {
		for (int x = u - r; x <= u + r; x++) {
			for (int z = v - r; z <= v + r; z++) {
				if (inBounds(x, z) && use[x + z * w] == usage) return false;
			}
		}
		return true;
	}

	private boolean noTreeNear(int u, int v, int r) {
		for (int x = u - r; x <= u + r; x++) {
			for (int z = v - r; z <= v + r; z++) {
				if (inBounds(x, z) && (map[x + z * w] == TREE || map[x + z * w] == BAMBOO)) return false;
			}
		}
		return true;
	}

	/** Whether no column of the plaza or of a gate lies within {@code r} of (u, v): they clear the air over them. */
	private boolean noClearingNear(int u, int v, int r) {
		for (int x = u - r; x <= u + r; x++) {
			for (int z = v - r; z <= v + r; z++) {
				if (inBounds(x, z) && (map[x + z * w] == PLAZA || use[x + z * w] == RESERVED)) return false;
			}
		}
		return true;
	}

	/**
	 * Plants up to {@code trees} trees and {@code clumps} clumps of bamboo on the open grass between the buildings, clear of the
	 * ways and the doors, a little apart from each other.
	 */
	public void plant(RandomSource random, int trees, int clumps) {
		List<int[]> spots = new ArrayList<>();
		for (int u = 0; u < w; u++) {
			for (int v = 0; v < d; v++) {
				int i = u + v * w;
				boolean free = use[i] == OPEN || use[i] == CLEAR && map[i] == GRASS; // between the houses too, not only beyond them
				if (inside(u, v) && free && outline(a, b, seed, u, v) < 0.98) spots.add(new int[] {u, v});
			}
		}
		for (int i = spots.size() - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			int[] t = spots.get(i);
			spots.set(i, spots.get(j));
			spots.set(j, t);
		}
		for (int[] spot : spots) {
			int u = spot[0], v = spot[1];
			if (trees > 0 && noneNear(u, v, 3, BUILDING) && noneNear(u, v, 1, WAY) && noClearingNear(u, v, 3) && noTreeNear(u, v, 5)) {
				map[u + v * w] = TREE;
				use[u + v * w] = CLEAR;
				trees--;
			} else if (clumps > 0 && noneNear(u, v, 1, BUILDING) && noneNear(u, v, 1, WAY) && noTreeNear(u, v, 4)) {
				// A clump: the spot and a few of its neighbours.
				for (int[] n : new int[][] {{0, 0}, {1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1, 1}}) {
					int x = u + n[0], z = v + n[1];
					if (!inside(x, z) || use[x + z * w] != OPEN && use[x + z * w] != CLEAR || map[x + z * w] != GRASS
							|| (n[0] != 0 || n[1] != 0) && random.nextInt(3) == 0) continue;
					map[x + z * w] = BAMBOO;
					use[x + z * w] = CLEAR;
				}
				clumps--;
			}
			if (trees <= 0 && clumps <= 0) break;
		}
	}
}
