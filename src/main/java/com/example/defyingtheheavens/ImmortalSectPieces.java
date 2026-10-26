package com.example.defyingtheheavens;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * An immortal sect's own pieces (ImmortalSectPlan):
 * <ul>
 *   <li><b>A floating island</b> ({@link #island}): an island hanging over the sea of clouds, its top the piece's 0 (the buildings on it
 *   stand there too), its rock hanging below and tapering to points; grass, flowers and trees on its top, its ways and courts paved, stone
 *   lanterns.</li>
 *   <li><b>A bridge</b> ({@link #bridge}) of white jade from island to island, stepping where one stands higher than the other, arched
 *   beneath, its balustrades lit by stone lanterns.</li>
 * </ul>
 * Drawn in the piece's own frame as every sect piece is: x across, z inward ({@code +z} the frame's north, {@code +x} its east, for the
 * blocks that face), turned with the piece into the world.
 */
final class ImmortalSectPieces {
	static void draw(SectPiece piece, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		switch (piece.kind()) {
			case PEAK_ISLAND -> island(piece, level, chunk, p);
			case PEAK_BRIDGE -> bridge(piece, level, chunk, p);
			default -> { }
		}
	}

	/** A number from 0 to 1 for a column, the same each time (the piece's seed, the column). */
	private static double hash(long seed, int x, int z, int salt) {
		long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
		h = (h ^ (h >>> 29)) * 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		return (h & 0xFFFFFF) / (double) 0x1000000;
	}

	// --- A floating island ---

	/** How a column is paved ({@code Paved} and {@code Disc}'s last number): the palette's paving, its path, its foundation's stone. */
	static final int PAVING = 0, PATH = 1, EDGE = 2;

	/** Its shape at a column: how far out it lies (0 its middle, 1 its rim; over 1 beyond it) and its rim's own reach that way. */
	private record Reach(double e, double rim) {}

	private static Reach reach(SectPiece g, double x, double z) {
		CompoundTag extra = g.extra();
		double cx = (g.width() - 1) / 2.0, cz = (g.depth() - 1) / 2.0;
		double dx = (x - cx) / extra.getInt("RU"), dz = (z - cz) / extra.getInt("RV");
		RandomSource shape = g.rand(0, 0, 91);
		double p1 = shape.nextDouble() * Math.PI * 2, p2 = shape.nextDouble() * Math.PI * 2;
		double theta = Math.atan2(dz, dx);
		// The rim wavers outward only (8 in 100 at most), never into the buildings' ground within 0.95 of it.
		return new Reach(Math.sqrt(dx * dx + dz * dz), 1 + 0.05 + 0.03 * Math.sin(3 * theta + p1) + 0.02 * Math.sin(7 * theta + p2));
	}

	/**
	 * A floating island: an ellipse of half-widths {@code RU} across and {@code RV} deep (its rim wavering a little, always outward), its top
	 * flat at the piece's 0; its rock hangs {@code Depth} below its middle and thins to its rim, the rounder for a wobble in it, with points
	 * of stone and dripstone and hanging roots under it. Grass over two of dirt on its top, stone banded with jade stone and veined with
	 * calcite beneath (as the realm's peaks are, ModNoiseSettings#immortalSurface). Its ways and courts paved ({@code Paved}: x0, z0, x1, z1
	 * and how, in fives; {@code Disc}: a round court's middle, radius and how, its rim of the foundation's stone), stone lanterns at
	 * {@code Lanterns} (x, z in pairs); flowers and grass on the rest and a few trees, clear of the buildings' lots and the bridges' landings
	 * ({@code Keep}: x0, z0, x1, z1 in fours) and of the paving. No waterfall off its rim: falling the whole way down through the clouds, one
	 * stood under its island like a pillar of jade (tour 12).
	 */
	static void island(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		CompoundTag extra = g.extra();
		int depth = extra.getInt("Depth");
		int[] keep = extra.getIntArray("Keep"), paved = extra.getIntArray("Paved"), disc = extra.getIntArray("Disc");
		int[] bounds = g.localBounds(chunk);
		BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState(), dirt = Blocks.DIRT.defaultBlockState(), stone = Blocks.STONE.defaultBlockState(),
				jade = ModBlocks.JADE_STONE.defaultBlockState(), calcite = Blocks.CALCITE.defaultBlockState(), moss = Blocks.MOSS_BLOCK.defaultBlockState();
		for (int x = bounds[0]; x <= bounds[2]; x++) {
			for (int z = bounds[1]; z <= bounds[3]; z++) {
				Reach r = reach(g, x, z);
				if (r.e() > r.rim()) continue;
				double t = r.e() / r.rim();
				double wobble = 0.82 + 0.18 * Math.sin(x * 0.31 + g.seed % 7) * Math.cos(z * 0.27 - g.seed % 5) + 0.12 * hash(g.seed, x, z, 1);
				int below = 1 + (int) Math.round(depth * Math.pow(Math.max(0, 1 - t * t), 0.85) * wobble);
				// A point hanging from under its middle now and then (short: tour 12's, up to eight more, hung under the islands like stilts).
				if (t < 0.6 && hash(g.seed, x, z, 2) < 0.02) below += 2 + (int) (hash(g.seed, x, z, 3) * 3);
				int how = paving(paved, disc, x, z);
				boolean rim = t > 0.9;
				g.put(level, chunk, x, 0, z, how >= 0 ? surface(p, how) : rim && hash(g.seed, x, z, 4) < 0.3 ? moss : grass);
				for (int y = -1; y >= -below; y--) {
					BlockState s;
					if (y >= -2) s = dirt;
					else if (Math.floorMod(y + (int) (hash(g.seed, x, 0, 5) * 3) + (int) (Math.sin(z * 0.2) * 2), 7) < 2) s = jade;
					else s = hash(g.seed, x * 31 + y, z, 6) < 0.12 ? calcite : stone;
					g.put(level, chunk, x, y, z, s);
				}
				// Under it: hanging roots, now and then a point of dripstone.
				double under = hash(g.seed, x, z, 7);
				if (under < 0.06) {
					g.put(level, chunk, x, -below - 1, z, Blocks.HANGING_ROOTS.defaultBlockState());
				} else if (under < 0.085 && t < 0.8) {
					int length = 2 + (int) (hash(g.seed, x, z, 8) * 3);
					for (int i = 0; i < length; i++) {
						DripstoneThickness thick = i == length - 1 ? DripstoneThickness.TIP : i == length - 2 ? DripstoneThickness.FRUSTUM
								: i == 0 ? DripstoneThickness.BASE : DripstoneThickness.MIDDLE;
						g.put(level, chunk, x, -below - 1 - i, z, Blocks.POINTED_DRIPSTONE.defaultBlockState()
								.setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN).setValue(PointedDripstoneBlock.THICKNESS, thick));
					}
				}
				// On it, clear of the buildings, the bridges and the paving: grass and flowers.
				if (how >= 0 || kept(keep, x, z)) continue;
				double top = hash(g.seed, x, z, 9);
				if (top < 0.18) g.put(level, chunk, x, 1, z, Blocks.GRASS);
				else if (top < 0.24) g.put(level, chunk, x, 1, z, FLOWERS[(int) (hash(g.seed, x, z, 10) * FLOWERS.length)]);
			}
		}
		for (int[] tree : trees(g, keep, paved, disc)) g.tree(level, chunk, p, tree[0], tree[1], g.rand(tree[0], tree[1], 94));
		int[] lanterns = extra.getIntArray("Lanterns");
		for (int i = 0; i + 1 < lanterns.length; i += 2) g.stoneLantern(level, chunk, p, lanterns[i], 1, lanterns[i + 1]);
	}

	private static final Block[] FLOWERS = {Blocks.LILY_OF_THE_VALLEY, Blocks.AZURE_BLUET, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY, Blocks.ALLIUM,
			Blocks.WHITE_TULIP};

	/**
	 * How column (x, z) of an island's frame is paved ({@link #PAVING}, {@link #PATH}, {@link #EDGE}), or -1 if it isn't: by the first
	 * of {@code paved}'s rectangles it lies in, else by a round court of {@code disc} (its rim the foundation's stone).
	 */
	private static int paving(int[] paved, int[] disc, int x, int z) {
		for (int i = 0; i + 4 < paved.length; i += 5) {
			if (x >= paved[i] && x <= paved[i + 2] && z >= paved[i + 1] && z <= paved[i + 3]) return paved[i + 4];
		}
		for (int i = 0; i + 3 < disc.length; i += 4) {
			double d = Math.hypot(x - disc[i], z - disc[i + 1]);
			if (d <= disc[i + 2] + 0.5) return d > disc[i + 2] - 0.5 ? EDGE : disc[i + 3];
		}
		return -1;
	}

	private static BlockState surface(SectPalette p, int how) {
		return (how == PATH ? p.path : how == EDGE ? p.foundation : p.paving).defaultBlockState();
	}

	/** Whether world column (x, z) lies on island {@code g}, no farther out than {@code share} of its rim (1 its very rim). */
	static boolean onIsland(SectPiece g, int x, int z, double share) {
		int[] local = g.toLocal(x, z);
		Reach r = reach(g, local[0], local[1]);
		return r.e() <= r.rim() * share;
	}

	/** Whether column (x, z) of an island's frame lies in one of {@code keep}'s rectangles (x0, z0, x1, z1 in fours) or within a block of one. */
	private static boolean kept(int[] keep, int x, int z) {
		for (int i = 0; i + 3 < keep.length; i += 4) {
			if (x >= keep[i] - 1 && x <= keep[i + 2] + 1 && z >= keep[i + 1] - 1 && z <= keep[i + 3] + 1) return true;
		}
		return false;
	}

	/** The column of an island's frame {@code share} of the way out to its rim at {@code angle} (radians from its +x). */
	private static int[] rimPoint(SectPiece g, double angle, double share) {
		CompoundTag extra = g.extra();
		double cx = (g.width() - 1) / 2.0, cz = (g.depth() - 1) / 2.0;
		double x = cx, z = cz;
		// Out along the angle to the last column that is still the island's.
		for (double s = 0; s < 1.3; s += 0.01) {
			double tx = cx + Math.cos(angle) * extra.getInt("RU") * s, tz = cz + Math.sin(angle) * extra.getInt("RV") * s;
			Reach r = reach(g, Math.round(tx), Math.round(tz));
			if (r.e() > r.rim() * share) break;
			x = tx;
			z = tz;
		}
		return new int[] {(int) Math.round(x), (int) Math.round(z)};
	}

	/** Where an island's trees stand: up to three, in the band toward its rim, clear of what it keeps clear and of its paving (the same every chunk). */
	private static List<int[]> trees(SectPiece g, int[] keep, int[] paved, int[] disc) {
		List<int[]> trees = new ArrayList<>();
		RandomSource random = g.rand(0, 0, 95);
		int want = 1 + random.nextInt(3);
		for (int tries = 0; tries < 60 && trees.size() < want; tries++) {
			double angle = random.nextDouble() * Math.PI * 2, share = 0.55 + random.nextDouble() * 0.25;
			int[] at = rimPoint(g, angle, share);
			boolean clear = true;
			for (int dx = -4; dx <= 4 && clear; dx += 2) {
				for (int dz = -4; dz <= 4 && clear; dz += 2) clear = !kept(keep, at[0] + dx, at[1] + dz) && paving(paved, disc, at[0] + dx, at[1] + dz) < 0;
			}
			for (int[] other : trees) clear &= Math.abs(other[0] - at[0]) + Math.abs(other[1] - at[1]) > 8;
			if (clear) trees.add(at);
		}
		return trees;
	}

	// --- A bridge ---

	/**
	 * A bridge's deck's level at {@code z} along it: {@code From} over its first island (its first {@code in0} blocks), {@code To} over its
	 * last (its last {@code in1}), a block at a time between.
	 */
	static int deck(int from, int to, int z, int length, int in0, int in1) {
		int start = in0, end = length - 1 - in1;
		if (z <= start || end <= start) return z <= start ? from : to;
		if (z >= end) return to;
		return from + (int) Math.round((to - from) * (z - start) / (double) (end - start));
	}

	/** The same for {@code bridge}'s own (its From, To, In0 and In1). */
	static int deck(SectPiece bridge, int z) {
		CompoundTag data = bridge.extra();
		return deck(data.getInt("From"), data.getInt("To"), z, bridge.depth(), data.getInt("In0"), data.getInt("In1"));
	}

	/**
	 * A bridge of white jade along its frame's z from one island to the next, five wide: its deck from {@code From} (its local y at z 0) to
	 * {@code To} (at its far end), stepping up or down a block at a time across the gap where one island stands higher; a balustrade either
	 * side and a stone lantern every eight blocks over the open sky; arched beneath, its underside hanging lowest at the span's middle. Its
	 * ends lie {@code In0} and {@code In1} over the islands themselves, level with their tops, where it is only a paved way.
	 */
	static void bridge(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		CompoundTag extra = g.extra();
		int w = g.width(), d = g.depth(), from = extra.getInt("From"), to = extra.getInt("To"), in0 = extra.getInt("In0"), in1 = extra.getInt("In1");
		BlockState deck = p.paving.defaultBlockState(), edge = p.foundation.defaultBlockState(), rail = p.baseWall.defaultBlockState(),
				path = p.path.defaultBlockState();
		int spanStart = in0, spanEnd = d - 1 - in1, span = Math.max(1, spanEnd - spanStart);
		for (int z = 0; z < d; z++) {
			int y = deck(from, to, z, d, in0, in1), before = z == 0 ? y : deck(from, to, z - 1, d, in0, in1),
					after = z == d - 1 ? y : deck(from, to, z + 1, d, in0, in1);
			boolean open = z >= spanStart && z <= spanEnd;
			for (int x = 0; x < w; x++) {
				boolean side = x == 0 || x == w - 1;
				BlockState surface = side ? edge : x == w / 2 ? path : deck;
				// A step where the deck has just risen along it (walked up toward +z), or where it falls next (walked up toward -z).
				if (!side && y > before) surface = p.baseStair(Direction.NORTH);
				else if (!side && after < y) surface = p.baseStair(Direction.SOUTH);
				g.put(level, chunk, x, y, z, surface);
				g.air(level, chunk, x, y + 1, z, x, y + 4, z);
				if (!open) continue;
				// Beneath it, its arch: deepest at the span's middle.
				int hang = 1 + (int) Math.round(2.5 * Math.sin(Math.PI * (z - spanStart) / span));
				for (int dy = 1; dy <= hang; dy++) g.put(level, chunk, x, y - dy, z, edge);
				if (side) g.put(level, chunk, x, y + 1, z, rail);
			}
			if (open && (z - spanStart) % 8 == 4) {
				g.stoneLantern(level, chunk, p, 0, y + 1, z);
				g.stoneLantern(level, chunk, p, w - 1, y + 1, z);
			}
		}
	}

	private ImmortalSectPieces() {}
}
