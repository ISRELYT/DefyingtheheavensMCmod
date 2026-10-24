package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * A demon clan's den, piece by piece (SectPiece's DEN_ kinds, laid out by DenStructure): hollow mounds and caves, each drawn in its
 * piece's own frame (x across its front, z inward from its door, y up from the ground at 0) from its seed alone, so a den split
 * across chunks comes out whole.
 * <p>
 * Every den is a {@link Mound}: a shell of the race's earth or stone rising from the ground in a rough dome (bumped by a soft noise),
 * hollowed into a chamber with a level floor, a tunnel in from the door (or, for a tiger's overhang, its whole front open under the
 * lip). Then each is furnished in its race's way. Nothing a spring can open in (stone, dirt, deepslate, tuff, andesite...) is in a
 * shell: water would pour from it once worldgen sets its springs.
 */
final class DenPieces {
	/** The sparring ring's plot, square. */
	static final int RING = 15;

	private DenPieces() {}

	// =====================================================================================================================
	// Sizes (width across the door, depth inward, height) and posts, for DenStructure's plan
	// =====================================================================================================================

	static int[] headSize(Bloodline race) {
		return switch (race) {
			case LIZARD -> new int[] {21, 19, 13};
			case FOX -> new int[] {21, 25, 24};
			case DRAGON -> new int[] {41, 31, 42};
			default -> new int[] {23, 21, 15};
		};
	}

	static int[] hoardSize(Bloodline race) {
		return race == Bloodline.LIZARD ? new int[] {13, 11, 9} : new int[] {13, 11, 10};
	}

	/** The disciples' den: two rows of mats under one roof, enough for the most a den of {@code members} holds. */
	static int[] burrowSize(Bloodline race, int members) {
		int width = members > 12 ? 21 : 19;
		return new int[] {width, 13, race == Bloodline.LIZARD ? 8 : 10};
	}

	static int[] elderSize(Bloodline race) {
		return new int[] {11, 11, 9};
	}

	/** The Clan Head's mat, {x, y, z}: at the back of its hall; a fox's in its shrine; a dragon's up on the floor at its crag's back. */
	static int[] seat(SectPiece piece) {
		if (piece.kind() == SectPiece.Kind.DEN_CRAG) return new int[] {piece.width() / 2, CRAG_FLOOR + 1, CRAG_HEAD_Z + 2};
		if (piece.race == Bloodline.FOX) return new int[] {piece.width() / 2, FOX_PLATEAU + 1, foxShrineZ(piece) + 4};
		Mound m = mound(piece);
		return new int[] {piece.width() / 2, 1, (int) Math.round(m.cz() + m.innerRz() * 0.55)};
	}

	/** An elder's mat, {x, y, z}: in the middle of its den. */
	static int[] home(SectPiece piece) {
		Mound m = mound(piece);
		return new int[] {piece.width() / 2, 1, (int) Math.round(m.cz()) + 1};
	}

	/** The disciples' mats, {x, y, z}: two rows across the burrow (every other block, an aisle in the middle); a dragon's on its crag's floor. */
	static int[][] mats(SectPiece piece) {
		List<int[]> mats = new ArrayList<>();
		int cx = piece.width() / 2;
		if (piece.kind() == SectPiece.Kind.DEN_CRAG) {
			// Rows between the ledges' steps, an aisle up the middle to the head's floor, clear of the hoard's guards.
			int members = Math.max(10, piece.extra().getInt("Members"));
			for (int z = 7; z <= 16 && mats.size() < members; z += 3) {
				for (int dx = 2; dx <= 6 && mats.size() < members; dx += 2) {
					mats.add(new int[] {cx - dx, 1, z});
					mats.add(new int[] {cx + dx, 1, z});
				}
			}
			return mats.toArray(new int[0][]);
		}
		Mound m = mound(piece);
		int[] rows = piece.race == Bloodline.TIGER ? new int[] {6, 9} : new int[] {(int) Math.round(m.cz()) - 1, (int) Math.round(m.cz()) + 2};
		for (int z : rows) {
			for (int x = 3; x <= piece.width() - 4; x += 2) {
				if (Math.abs(x - cx) <= 1) continue; // the aisle
				if (m.hollow(x, z) >= 2) mats.add(new int[] {x, 1, z});
			}
		}
		return mats.toArray(new int[0][]);
	}

	/** The elders' posts at the hoard, {x, y, z}: either side of its door outside, and inside by the chests; a dragon's at its alcove. */
	static int[][] hoardGuards(SectPiece piece) {
		// (The third inside by the chests, on the alcove's open floor: it stood in the hoard's own wall before the sweep of 2026-10-23.)
		if (piece.kind() == SectPiece.Kind.DEN_CRAG) return new int[][] {{HOARD_X1 + 2, 1, HOARD_Z0 + 1}, {HOARD_X1 + 2, 1, HOARD_Z1 - 1}, {HOARD_X0 + 3, 1, 11}};
		int cx = piece.width() / 2;
		return new int[][] {{cx - 3, 1, -2}, {cx + 3, 1, -2}, {cx, 1, piece.depth() - 4}};
	}

	/** The treasury the clan's seal keeps (world): the hoard and a block round it; a dragon's alcove in its crag. */
	static BoundingBox hoardBox(SectPiece piece) {
		if (piece.kind() != SectPiece.Kind.DEN_CRAG) return piece.getBoundingBox().inflatedBy(1);
		BlockPos a = piece.toWorld(HOARD_X0 - 1, 0, HOARD_Z0 - 1), b = piece.toWorld(HOARD_X1 + 1, 7, HOARD_Z1 + 1);
		return new BoundingBox(Math.min(a.getX(), b.getX()), a.getY(), Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()), b.getY(),
				Math.max(a.getZ(), b.getZ()));
	}

	/** A dragon clan's elders' mats, {x, y, z}: one on each ledge up the crag's walls. */
	static int[][] ledges(SectPiece piece) {
		List<int[]> mats = new ArrayList<>();
		for (int[] ledge : ledgeSpots(piece)) mats.add(new int[] {ledge[0], CRAG_FLOOR + 1, ledge[1]});
		return mats.toArray(new int[0][]);
	}

	/** {x, y, z}: the ring's middle, then a drill spot before each of the posts across its back. */
	static int[][] ringPosts(SectPiece piece) {
		int[][] posts = new int[1 + DUMMIES.length][];
		posts[0] = new int[] {RING / 2, 1, RING / 2};
		for (int i = 0; i < DUMMIES.length; i++) posts[i + 1] = new int[] {DUMMIES[i], 1, RING - 4};
		return posts;
	}

	/** A point just inside the gate. */
	static int[] gateInside(SectPiece piece) {
		return new int[] {piece.width() / 2, 1, piece.depth() - 1};
	}

	// =====================================================================================================================
	// Drawing
	// =====================================================================================================================

	static void draw(SectPiece piece, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		switch (piece.kind()) {
			case DEN_HALL -> hall(piece, level, chunk, p);
			case DEN_ELDER -> elder(piece, level, chunk, p);
			case DEN_BURROW -> burrow(piece, level, chunk, p);
			case DEN_HOARD -> hoard(piece, level, chunk, p);
			case DEN_GATE -> gate(piece, level, chunk, p);
			case DEN_RING -> ring(piece, level, chunk, p);
			case DEN_CRAG -> crag(piece, level, chunk, p);
			case DEN_FEATURE -> feature(piece, level, chunk, p);
			default -> {
			}
		}
	}

	// --- The race's stuff ---

	/** What a race's shells are made of, deep in (by a column's hash), at their top (the skin), and its floors. */
	private static BlockState shell(Bloodline race, double roll, int y, int top) {
		return switch (race) {
			case LIZARD -> (roll < 0.5 ? Blocks.MUD_BRICKS : roll < 0.88 ? Blocks.PACKED_MUD : Blocks.MUD).defaultBlockState();
			case FOX -> (roll < 0.45 ? Blocks.ROOTED_DIRT : roll < 0.9 ? Blocks.COARSE_DIRT : Blocks.MOSSY_COBBLESTONE).defaultBlockState();
			case DRAGON -> (roll < 0.04 && y > 3 ? ModBlocks.JADE_STONE : roll < 0.5 ? Blocks.COBBLED_DEEPSLATE : roll < 0.7 ? Blocks.SMOOTH_BASALT
					: roll < 0.86 ? Blocks.BLACKSTONE : Blocks.POLISHED_DEEPSLATE).defaultBlockState();
			default -> (roll < 0.45 ? Blocks.MOSSY_COBBLESTONE : roll < 0.82 ? Blocks.COBBLESTONE : roll < 0.94 ? Blocks.POLISHED_ANDESITE
					: Blocks.COARSE_DIRT).defaultBlockState();
		};
	}

	private static BlockState skin(Bloodline race, double roll, int y) {
		return switch (race) {
			case LIZARD -> (roll < 0.6 ? Blocks.MOSS_BLOCK : roll < 0.85 ? Blocks.MUD_BRICKS : Blocks.MUDDY_MANGROVE_ROOTS).defaultBlockState();
			case FOX -> Blocks.GRASS_BLOCK.defaultBlockState();
			case DRAGON -> (y >= 30 ? Blocks.SNOW_BLOCK : roll < 0.3 ? Blocks.MOSS_BLOCK : roll < 0.75 ? Blocks.COBBLED_DEEPSLATE
					: Blocks.SMOOTH_BASALT).defaultBlockState();
			default -> (roll < 0.5 ? Blocks.MOSS_BLOCK : roll < 0.8 ? Blocks.GRASS_BLOCK : Blocks.MOSSY_COBBLESTONE).defaultBlockState();
		};
	}

	private static BlockState floor(Bloodline race, double roll) {
		return switch (race) {
			case LIZARD -> (roll < 0.7 ? Blocks.PACKED_MUD : Blocks.MUD_BRICKS).defaultBlockState();
			case FOX -> Blocks.CHERRY_PLANKS.defaultBlockState();
			case DRAGON -> (roll < 0.6 ? Blocks.POLISHED_DEEPSLATE : Blocks.DEEPSLATE_TILES).defaultBlockState();
			default -> (roll < 0.6 ? Blocks.COARSE_DIRT : Blocks.PODZOL).defaultBlockState();
		};
	}

	// --- Noise ---

	/** A hash of (x, z) under {@code seed}, 0 to 1. */
	static double hash(long seed, int x, int z) {
		long h = seed ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
		h = (h ^ h >>> 33) * 0xFF51AFD7ED558CCDL;
		h = (h ^ h >>> 33) * 0xC4CEB9FE1A85EC53L;
		h ^= h >>> 33;
		return (h >>> 11) * 0x1.0p-53;
	}

	/** Soft value noise at (x, z), 0 to 1: the hash on a grid {@code cell} apart, eased between. */
	static double noise(long seed, double x, double z, double cell) {
		double gx = x / cell, gz = z / cell;
		int x0 = (int) Math.floor(gx), z0 = (int) Math.floor(gz);
		double fx = gx - x0, fz = gz - z0;
		fx = fx * fx * (3 - 2 * fx);
		fz = fz * fz * (3 - 2 * fz);
		double a = hash(seed, x0, z0), b = hash(seed, x0 + 1, z0), c = hash(seed, x0, z0 + 1), d = hash(seed, x0 + 1, z0 + 1);
		return (a + (b - a) * fx) * (1 - fz) + (c + (d - c) * fx) * fz;
	}

	// =====================================================================================================================
	// The mound
	// =====================================================================================================================

	/**
	 * A mound filling its piece: centred in it ({@link #cx}, {@link #cz}), {@code height} at the top, as round as {@code power} (1:
	 * a dome; more, fuller; less, a peak), bumped by {@code rough} of its height; its shell {@code wall} thick, its chamber
	 * {@code innerHeight} high at the middle; a door {@code door} wide either side of the middle (-1: none) and {@code doorHeight}
	 * high; a cap ({@code plateau}, 0 for none) where its top is cut flat.
	 */
	record Mound(long seed, int w, int d, double height, double power, double rough, int wall, double innerHeight, int door, int doorHeight, int plateau) {
		double cx() { return (w - 1) / 2.0; }
		double cz() { return (d - 1) / 2.0; }
		double innerRz() { return d / 2.0 - wall; }

		/** How high the shell stands at (x, z): 0 off it. */
		double outer(int x, int z) {
			double nx = (x - cx()) / (w / 2.0), nz = (z - cz()) / (d / 2.0);
			double r2 = nx * nx + nz * nz;
			if (r2 >= 1) return 0;
			double top = height * Math.pow(1 - r2, power) + (noise(seed, x, z, 4.0) - 0.5) * rough * height * Math.sqrt(1 - r2);
			return plateau > 0 ? Math.min(top, plateau) : top;
		}

		/** How high the chamber's air reaches at (x, z) (0: none): always two blocks short of the shell's top. */
		int hollow(int x, int z) {
			double nx = (x - cx()) / (w / 2.0 - wall), nz = (z - cz()) / (d / 2.0 - wall);
			double r2 = nx * nx + nz * nz;
			if (r2 >= 1) return 0;
			double air = Math.min(innerHeight * Math.sqrt(1 - r2), outer(x, z) - 2);
			return Math.max(0, (int) Math.floor(air));
		}
	}

	/** The mound a piece is, by its kind and race. */
	static Mound mound(SectPiece piece) {
		int w = piece.width(), d = piece.depth();
		long seed = piece.seed;
		Bloodline race = piece.race;
		return switch (piece.kind()) {
			case DEN_HALL -> switch (race) {
				case LIZARD -> new Mound(seed, w, d, 11, 0.55, 0.12, 2, 7.5, 1, 3, 0);
				case FOX -> new Mound(seed, w, d, 14, 0.6, 0.0, 3, 0, -1, 3, FOX_PLATEAU); // solid: steps up its front instead
				default -> new Mound(seed, w, d, 13, 0.6, 0.35, 3, 8, 1, 4, 0);
			};
			case DEN_ELDER -> race == Bloodline.LIZARD ? new Mound(seed, w, d, 7.5, 0.55, 0.1, 2, 4.5, 1, 3, 0)
					: race == Bloodline.FOX ? new Mound(seed, w, d, 7.5, 0.6, 0.05, 2, 4.5, 1, 3, 0)
					: new Mound(seed, w, d, 8, 0.6, 0.35, 2, 4.5, 1, 3, 0);
			case DEN_BURROW -> race == Bloodline.LIZARD ? new Mound(seed, w, d, 7.5, 0.55, 0.1, 2, 5, 1, 3, 0)
					: race == Bloodline.TIGER ? new Mound(seed, w, d, 10, 0.7, 0.3, 2, 6, 1, 4, 0)
					: new Mound(seed, w, d, 8.5, 0.6, 0.05, 2, 5.5, 1, 3, 0);
			case DEN_HOARD -> race == Bloodline.LIZARD ? new Mound(seed, w, d, 8, 0.55, 0.1, 2, 5, 1, 3, 0)
					: new Mound(seed, w, d, 9, 0.6, race == Bloodline.FOX ? 0.05 : 0.35, 2, 5.5, 1, 3, 0);
			case DEN_CRAG -> new Mound(seed, w, d, 38, 1.2, 0.25, 5, 15, 4, 9, 0);
			default -> new Mound(seed, w, d, 6, 0.6, 0.3, 2, 3, 1, 3, 0);
		};
	}

	/** Whether column (x, z) of the piece is in this chunk. */
	private static boolean here(SectPiece piece, BoundingBox chunk, int x, int z) {
		return chunk.isInside(piece.toWorld(x, chunk.minY() + 1 - piece.getBoundingBox().minY(), z));
	}

	/**
	 * Raises the mound and hollows it: its shell column by column (its race's stuff, its skin on top), the chamber's air over a
	 * level floor, and the tunnel in from the door (an arch over a tall one). {@code openFront}: no door, the front open under the
	 * lip as deep as the chamber reaches forward (a tiger's overhang).
	 */
	private static void raise(SectPiece piece, WorldGenLevel level, BoundingBox chunk, Mound m, boolean openFront) {
		Bloodline race = piece.race;
		int cx = (int) Math.round(m.cx());
		for (int x = 0; x < m.w(); x++) {
			for (int z = 0; z < m.d(); z++) {
				if (!here(piece, chunk, x, z)) continue;
				int top = (int) Math.floor(m.outer(x, z));
				if (top < 1) continue;
				int hollow = m.hollow(x, z);
				// The door: a tunnel from the front edge in to the chamber, as high as the door all the way (a notch where the rim is
				// lower), arched over a wide one.
				boolean tunnel = !openFront && m.door() >= 0 && Math.abs(x - cx) <= m.door() && z <= m.cz();
				int tunnelTop = tunnel ? m.doorHeight() - (m.door() >= 3 ? (int) Math.round(Math.pow((double) Math.abs(x - cx) / m.door(), 2) * 3) : 0) : 0;
				// The open front: everything below the lip forward of the chamber's middle.
				int openTop = openFront && z <= m.cz() ? Math.max(hollow, m.doorHeight()) : 0;
				for (int y = 1; y <= top; y++) {
					boolean air = y <= hollow || y <= tunnelTop || y <= openTop;
					if (air) {
						piece.put(level, chunk, x, y, z, Blocks.AIR.defaultBlockState());
						continue;
					}
					double roll = hash(m.seed() + y * 31L, x, z);
					piece.put(level, chunk, x, y, z, y == top ? skin(race, roll, y) : shell(race, roll, y, top));
				}
				if (hollow > 0 || tunnelTop > 0 || openTop > 0) piece.put(level, chunk, x, 0, z, floor(race, hash(m.seed() + 7, x, z)));
			}
		}
	}

	/** The floor's lights: a lantern hung from the chamber's roof at (x, z) where it is high enough, else stood on the floor. */
	private static void light(SectPiece piece, WorldGenLevel level, BoundingBox chunk, SectPalette p, Mound m, int x, int z) {
		int roof = m.hollow(x, z);
		if (roof >= 4) piece.lanternHanging(level, chunk, p, x, roof, z);
		else if (roof >= 1) piece.lanternStanding(level, chunk, p, x, 1, z);
	}

	// =====================================================================================================================
	// The head's den
	// =====================================================================================================================

	private static void hall(SectPiece piece, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		if (piece.race == Bloodline.FOX) {
			foxHall(piece, level, chunk, p);
			return;
		}
		Mound m = mound(piece);
		raise(piece, level, chunk, m, false);
		if (piece.race == Bloodline.LIZARD) rootArcs(piece, level, chunk, m);
		int cx = piece.width() / 2, cz = (int) Math.round(m.cz());
		int[] seat = seat(piece);
		RandomSource random = piece.rand(3, 3, 41);
		furnish(piece, level, chunk, p, m, random);
		piece.put(level, chunk, seat[0], seat[1], seat[2], ModBlocks.RED_MEDITATION_MAT);
		// The head's riches either side of its mat.
		piece.chest(level, chunk, cx - 3, 1, seat[2] + 1, Direction.NORTH, SectPiece.MASTER_HALL_LOOT);
		piece.chest(level, chunk, cx + 3, 1, seat[2] + 1, Direction.NORTH, SectPiece.MASTER_HALL_LOOT);
		if (piece.race == Bloodline.LIZARD) {
			// Its basking pool, off to one side.
			int px = cx - 5;
			for (int x = px - 1; x <= px + 1; x++) {
				for (int z = cz - 1; z <= cz + 1; z++) {
					piece.put(level, chunk, x, 0, z, Blocks.WATER.defaultBlockState());
					piece.put(level, chunk, x, -1, z, Blocks.MUD.defaultBlockState());
				}
			}
			piece.put(level, chunk, px, 1, cz, Blocks.LILY_PAD);
		} else {
			// A tiger's fires and the bones of its kills.
			for (int side : new int[] {-1, 1}) {
				int fx = cx + side * (int) Math.round(m.w() / 2.0 - m.wall() - 2);
				piece.put(level, chunk, fx, 1, cz, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
				piece.put(level, chunk, fx, 1, cz + 3, Blocks.BONE_BLOCK);
				piece.put(level, chunk, fx, 2, cz + 3, Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
			}
		}
		light(piece, level, chunk, p, m, cx, cz);
		light(piece, level, chunk, p, m, cx, seat[2] - 2);
	}

	/**
	 * A floor laid with its race's comforts, here and there: a tiger's furs (brown and orange), a lizard's moss, a fox's red and white
	 * mats; never in the tunnel's line, where its people walk in.
	 */
	private static void furnish(SectPiece piece, WorldGenLevel level, BoundingBox chunk, SectPalette p, Mound m, RandomSource random) {
		int cx = piece.width() / 2;
		for (int x = 0; x < m.w(); x++) {
			for (int z = 0; z < m.d(); z++) {
				if (Math.abs(x - cx) <= 1 || m.hollow(x, z) < 2 || hash(piece.seed + 99, x, z) > 0.28) continue;
				piece.put(level, chunk, x, 1, z, hash(piece.seed + 98, x, z) < 0.5 ? p.carpet : p.carpetAlt);
			}
		}
	}

	/** A lizard's dome under arching mangrove roots: two arcs over it, crossing at the top. */
	private static void rootArcs(SectPiece piece, WorldGenLevel level, BoundingBox chunk, Mound m) {
		int cx = (int) Math.round(m.cx()), cz = (int) Math.round(m.cz());
		for (int x = 0; x < m.w(); x++) {
			int top = (int) Math.floor(m.outer(x, cz));
			if (top >= 1) piece.put(level, chunk, x, top + 1, cz, Blocks.MANGROVE_ROOTS);
		}
		for (int z = 0; z < m.d(); z++) {
			int top = (int) Math.floor(m.outer(cx + 2, z));
			if (top >= 1) piece.put(level, chunk, cx + 2, top + 1, z, Blocks.MANGROVE_ROOTS);
		}
	}

	/** How high a fox's hillock is cut flat, its shrine standing on top. */
	static final int FOX_PLATEAU = 8;

	/** Where on a fox's hillock its shrine's front stands (z). */
	private static int foxShrineZ(SectPiece piece) {
		return piece.depth() / 2 - 1;
	}

	/**
	 * A fox clan's head's den: the greatest of its grassy hillocks, cut flat on top, with steps up through a cutting in its front
	 * under a torii, and on the top the head's little shrine (white walls, vermilion pillars, a tiled roof), the clan's head's riches
	 * in it, stone lanterns before it.
	 */
	private static void foxHall(SectPiece piece, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		Mound m = mound(piece);
		raise(piece, level, chunk, m, false);
		int w = piece.width(), cx = w / 2, sz = foxShrineZ(piece), floorY = FOX_PLATEAU;
		// The steps up the front, through a cutting.
		for (int z = 0; z < sz; z++) {
			int y = Math.min(floorY, z + 1);
			for (int x = cx - 1; x <= cx + 1; x++) {
				if (!here(piece, chunk, x, z)) continue;
				piece.put(level, chunk, x, y, z, y < floorY ? p.baseStairs.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH)
						: p.foundation.defaultBlockState());
				for (int k = y + 1; k <= floorY + 4; k++) piece.put(level, chunk, x, k, z, Blocks.AIR.defaultBlockState());
				for (int k = 1; k < y; k++) piece.put(level, chunk, x, k, z, p.foundation.defaultBlockState());
			}
			// Stone banks either side of the steps.
			for (int x : new int[] {cx - 2, cx + 2}) {
				if (!here(piece, chunk, x, z)) continue;
				int top = Math.max((int) Math.floor(m.outer(x, z)), Math.min(floorY, z + 2));
				for (int k = 1; k <= top; k++) piece.put(level, chunk, x, k, z, p.foundation.defaultBlockState());
			}
		}
		// A torii at the foot of the steps.
		for (int x : new int[] {cx - 2, cx + 2}) piece.fill(level, chunk, x, 1, 0, x, 5, 0, p.pillar(Direction.Axis.Y));
		piece.fill(level, chunk, cx - 2, 4, 0, cx + 2, 4, 0, p.beam(Direction.Axis.X));
		piece.lintel(level, chunk, p, cx - 2, cx + 2, 6, 0, true, true);
		// The shrine on top: 7 across, 7 deep, white walls between vermilion pillars, a door to the steps.
		int x0 = cx - 3, x1 = cx + 3, z0 = sz, z1 = sz + 6, y0 = floorY + 1, y1 = floorY + 4;
		piece.fill(level, chunk, x0, floorY, z0, x1, floorY, z1, p.floor);
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean edgeX = x == x0 || x == x1, edgeZ = z == z0 || z == z1;
				if (!edgeX && !edgeZ) {
					piece.fill(level, chunk, x, y0, z, x, y1 + 1, z, Blocks.AIR.defaultBlockState());
					continue;
				}
				if (edgeX && edgeZ) piece.fill(level, chunk, x, y0, z, x, y1, z, p.pillar(Direction.Axis.Y));
				else piece.fill(level, chunk, x, y0, z, x, y1, z, p.wall);
			}
		}
		piece.fill(level, chunk, cx - 1, y0, z0, cx + 1, y0 + 2, z0, Blocks.AIR.defaultBlockState());
		// Its roof: tiles stepping in to a red ridge.
		for (int k = 0; k <= 4; k++) {
			int rx0 = x0 - 1 + k, rx1 = x1 + 1 - k, rz0 = z0 - 1 + k, rz1 = z1 + 1 - k, ry = y1 + 1 + k;
			if (rx0 > rx1 || rz0 > rz1) break;
			for (int x = rx0; x <= rx1; x++) {
				for (int z = rz0; z <= rz1; z++) {
					boolean rim = x == rx0 || x == rx1 || z == rz0 || z == rz1;
					if (rim || rx1 - rx0 <= 1 || rz1 - rz0 <= 1) piece.put(level, chunk, x, ry, z, rx1 - rx0 <= 1 || rz1 - rz0 <= 1 ? p.roofTrim : p.roof);
				}
			}
		}
		int[] seat = seat(piece);
		piece.put(level, chunk, seat[0], seat[1], seat[2], ModBlocks.RED_MEDITATION_MAT);
		piece.chest(level, chunk, x0 + 1, y0, z1 - 1, Direction.NORTH, SectPiece.MASTER_HALL_LOOT);
		piece.chest(level, chunk, x1 - 1, y0, z1 - 1, Direction.NORTH, SectPiece.MASTER_HALL_LOOT);
		piece.candles(level, chunk, p, x0 + 1, y0, z0 + 1, 3);
		piece.candles(level, chunk, p, x1 - 1, y0, z0 + 1, 3);
		piece.lanternStanding(level, chunk, p, cx + 2, y0, z1 - 1);
		// Stone lanterns at the shrine's front corners.
		piece.stoneLantern(level, chunk, p, x0 - 1, y0, z0 - 2);
		piece.stoneLantern(level, chunk, p, x1 + 1, y0, z0 - 2);
	}

	// =====================================================================================================================
	// An elder's den, the disciples' burrow, the hoard
	// =====================================================================================================================

	private static void elder(SectPiece piece, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		Mound m = mound(piece);
		raise(piece, level, chunk, m, false);
		if (piece.race == Bloodline.FOX) roundDoor(piece, level, chunk, m);
		if (piece.race == Bloodline.LIZARD) rootArcs(piece, level, chunk, m);
		int[] mat = home(piece);
		int cx = piece.width() / 2;
		piece.put(level, chunk, mat[0], mat[1], mat[2], ModBlocks.MEDITATION_MAT);
		piece.chest(level, chunk, cx, 1, mat[2] + 2, Direction.NORTH, SectPiece.ELDER_LOOT);
		piece.put(level, chunk, cx - 2, 1, mat[2], p.carpet);
		piece.put(level, chunk, cx + 2, 1, mat[2], p.carpetAlt);
		light(piece, level, chunk, p, m, cx - 2, mat[2] + 1);
	}

	/**
	 * Where the disciples live, two rows of mats: a tiger's a long rock overhang open to the front; a lizard's a low dome half sunk
	 * in the ground; a fox's a long hillock.
	 */
	private static void burrow(SectPiece piece, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		Mound m = mound(piece);
		boolean overhang = piece.race == Bloodline.TIGER;
		raise(piece, level, chunk, m, overhang);
		if (piece.race == Bloodline.FOX) roundDoor(piece, level, chunk, m);
		if (piece.race == Bloodline.LIZARD) rootArcs(piece, level, chunk, m);
		for (int[] mat : mats(piece)) piece.put(level, chunk, mat[0], mat[1], mat[2], ModBlocks.MEDITATION_MAT);
		int cx = piece.width() / 2, cz = (int) Math.round(m.cz());
		light(piece, level, chunk, p, m, cx, cz);
		light(piece, level, chunk, p, m, cx - 6, cz);
		light(piece, level, chunk, p, m, cx + 6, cz);
		if (overhang) {
			// A fire before the overhang, and bones.
			piece.put(level, chunk, cx, 1, 1, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
			piece.put(level, chunk, 2, 1, m.d() - 4, Blocks.BONE_BLOCK);
		}
	}

	private static void hoard(SectPiece piece, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		Mound m = mound(piece);
		raise(piece, level, chunk, m, false);
		if (piece.race == Bloodline.FOX) roundDoor(piece, level, chunk, m);
		if (piece.race == Bloodline.LIZARD) rootArcs(piece, level, chunk, m);
		int cx = piece.width() / 2, back = piece.depth() - 3;
		RandomSource random = piece.rand(5, 5, 23);
		// Chests of the clan's wealth along the back, its treasures on pedestals down the sides.
		for (int dx = -2; dx <= 2; dx += 2) piece.chest(level, chunk, cx + dx, 1, back, Direction.NORTH, SectPiece.TREASURY_LOOT);
		int i = 0;
		for (int z = back - 2; z >= back - 4; z -= 2) {
			for (int side : new int[] {-1, 1}) {
				int x = cx + side * 3;
				if (m.hollow(x, z) >= 2) piece.pedestal(level, chunk, x, 1, z, piece.treasure(random, i++));
			}
		}
		piece.put(level, chunk, cx, 1, back - 1, ModBlocks.RED_MEDITATION_MAT); // where an elder keeps it
		light(piece, level, chunk, p, m, cx, back - 3);
	}

	/** A fox's round door: a frame of birch round the tunnel's mouth. */
	private static void roundDoor(SectPiece piece, WorldGenLevel level, BoundingBox chunk, Mound m) {
		int cx = (int) Math.round(m.cx());
		BlockState up = Blocks.STRIPPED_BIRCH_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
		BlockState across = Blocks.STRIPPED_BIRCH_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
		int front = 0;
		while (front < m.d() && m.outer(cx, front) < m.doorHeight() + 1) front++;
		for (int y = 1; y <= m.doorHeight(); y++) {
			piece.put(level, chunk, cx - m.door() - 1, y, front, up);
			piece.put(level, chunk, cx + m.door() + 1, y, front, up);
		}
		for (int x = cx - m.door() - 1; x <= cx + m.door() + 1; x++) piece.put(level, chunk, x, m.doorHeight() + 1, front, across);
		piece.put(level, chunk, cx - m.door() - 1, 1, front - 1, Blocks.LANTERN);
	}

	// =====================================================================================================================
	// The gate, the ring
	// =====================================================================================================================

	/** The gate across the way in (and the side ways'): its race's own. */
	private static void gate(SectPiece piece, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = piece.width(), cx = w / 2, z = piece.depth() / 2;
		int half = w > 12 ? 4 : 3;
		piece.air(level, chunk, 0, 1, 0, w - 1, 11, piece.depth() - 1);
		switch (piece.race) {
			case LIZARD -> {
				// An arch of mangrove: two trunks, and roots arching over the way between them.
				for (int x : new int[] {cx - half, cx + half}) piece.fill(level, chunk, x, 1, z, x, 6, z, Blocks.MANGROVE_LOG);
				for (int x = cx - half; x <= cx + half; x++) {
					double t = (double) (x - cx) / half;
					int y = 6 + (int) Math.round(2 * (1 - t * t));
					piece.put(level, chunk, x, y, z, Blocks.MANGROVE_ROOTS);
					if (Math.abs(x - cx) < half) piece.put(level, chunk, x, y - 1, z, Blocks.MANGROVE_ROOTS);
				}
				piece.put(level, chunk, cx, 9, z, Blocks.MOSS_BLOCK);
				piece.lanternHanging(level, chunk, p, cx - 1, 6, z);
				piece.lanternHanging(level, chunk, p, cx + 1, 6, z);
			}
			case FOX -> {
				for (int x : new int[] {cx - half, cx + half}) piece.post(level, chunk, p, x, z, 6);
				piece.tieBeam(level, chunk, p, cx - half - 1, cx + half + 1, 5, z, cx - half, cx + half);
				piece.put(level, chunk, cx, 6, z, p.accent);
				piece.lintel(level, chunk, p, cx - half - 1, cx + half + 1, 7, z, true, true);
				piece.lanternHanging(level, chunk, p, cx - 1, 4, z);
				piece.lanternHanging(level, chunk, p, cx + 1, 4, z);
			}
			case DRAGON -> {
				// Two crags of dark rock either side, banded with jade, capped with gold.
				for (int x : new int[] {cx - half - 1, cx + half}) {
					for (int dx = 0; dx <= 1; dx++) {
						for (int y = 1; y <= 9; y++) {
							BlockState rock = y % 4 == 0 ? ModBlocks.JADE_STONE.defaultBlockState() : shell(Bloodline.DRAGON, hash(piece.seed, x + dx, y), y, 9);
							piece.put(level, chunk, x + dx, y, z, rock);
						}
						piece.put(level, chunk, x + dx, 10, z, Blocks.GOLD_BLOCK);
					}
				}
			}
			default -> {
				// Two standing stones, mossy, leaning a little apart at the top.
				for (int side : new int[] {-1, 1}) {
					int x = cx + side * half;
					for (int y = 1; y <= 8; y++) {
						int lean = y > 6 ? side : 0;
						BlockState stone = shell(Bloodline.TIGER, hash(piece.seed, x, y), y, 8);
						piece.put(level, chunk, x + lean, y, z, stone);
						if (y <= 5) piece.put(level, chunk, x + side, y, z, shell(Bloodline.TIGER, hash(piece.seed + 1, x, y), y, 8));
					}
					piece.put(level, chunk, x + side, 9, z, Blocks.MOSS_BLOCK);
				}
			}
		}
	}

	/** The posts across the ring's back the disciples drill before. */
	private static final int[] DUMMIES = {3, 6, 8, 11};

	/** The sparring ring: a round floor of trodden earth, its race's stones round it, posts to drill at across the back. */
	private static void ring(SectPiece piece, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int c = RING / 2;
		for (int x = 0; x < RING; x++) {
			for (int z = 0; z < RING; z++) {
				double dist = Math.sqrt((x - c) * (x - c) + (z - c) * (z - c));
				if (dist > c + 0.5) continue;
				piece.put(level, chunk, x, 0, z, dist > c - 1.5 ? p.foundation : p.path);
				piece.air(level, chunk, x, 1, z, x, 4, z);
			}
		}
		// Stones round it (not across its front, where one walks in).
		for (int k = 0; k < 10; k++) {
			double angle = Math.PI * 2 * k / 10 + Math.PI / 10;
			int x = c + (int) Math.round(Math.cos(angle) * (c - 0.5)), z = c + (int) Math.round(Math.sin(angle) * (c - 0.5));
			if (z < 3) continue;
			int tall = 2 + (int) (hash(piece.seed, x, z) * 2);
			for (int y = 1; y <= tall; y++) {
				piece.put(level, chunk, x, y, z, piece.race == Bloodline.FOX && y == tall ? p.lantern.defaultBlockState() : piece.race == Bloodline.FOX
						? p.baseWall.defaultBlockState() : shell(piece.race, hash(piece.seed + y, x, z), y, tall));
			}
		}
		// The posts across the back: logs, a tiger's scored with its claws (stripped).
		BlockState post = switch (piece.race) {
			case LIZARD -> Blocks.MANGROVE_LOG.defaultBlockState();
			case FOX -> Blocks.CHERRY_LOG.defaultBlockState();
			case DRAGON -> Blocks.POLISHED_BASALT.defaultBlockState();
			default -> Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
		};
		for (int x : DUMMIES) piece.fill(level, chunk, x, 1, RING - 3, x, 2, RING - 3, post);
	}

	// =====================================================================================================================
	// The dragons' crag
	// =====================================================================================================================

	/** The crag's floors: its head's floor raised this high at its back, its elders' ledges as high. */
	static final int CRAG_FLOOR = 3;
	/** Where the head's raised floor starts (z), and the hoard's alcove (x, z), in the crag's frame. */
	static final int CRAG_HEAD_Z = 21, HOARD_X0 = 6, HOARD_X1 = 10, HOARD_Z0 = 9, HOARD_Z1 = 17;

	/**
	 * The ledges' places {x, z}: three up the right wall, one at the left wall's back (clear of the hoard, the head's floor and the
	 * mats, each where the chamber is high enough over it); more elders than ledges live on the floor's mats.
	 */
	private static List<int[]> ledgeSpots(SectPiece piece) {
		int w = piece.width(), elders = Math.max(1, Math.min(4, piece.extra().getInt("Elders")));
		int[][] spots = {{w - 11, 9}, {w - 11, 14}, {w - 11, 19}, {10, 20}};
		List<int[]> list = new ArrayList<>();
		for (int i = 0; i < elders; i++) list.add(spots[i]);
		return list;
	}

	/**
	 * A dragon clan's crag: one great hollow peak of dark rock banded with jade (snow on its top), all the clan under it. A wide
	 * arched mouth at the front; inside, the disciples' mats across the floor, the head's floor raised at the back with steps up to
	 * it, the hoard in an alcove on the left (gold, raw gold, amethyst, the chests and pedestals of the clan's treasures), and the
	 * elders' ledges up the walls, steps up to each.
	 */
	private static void crag(SectPiece piece, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		Mound m = mound(piece);
		raise(piece, level, chunk, m, false);
		int w = piece.width(), cx = w / 2;
		RandomSource random = piece.rand(7, 7, 53);
		// The head's floor, steps up to it in the middle.
		for (int x = cx - 6; x <= cx + 6; x++) {
			for (int z = CRAG_HEAD_Z; z <= CRAG_HEAD_Z + 5; z++) {
				if (m.hollow(x, z) < CRAG_FLOOR + 3) continue;
				piece.fill(level, chunk, x, 1, z, x, CRAG_FLOOR, z, Blocks.POLISHED_DEEPSLATE);
				piece.put(level, chunk, x, CRAG_FLOOR, z, Blocks.DEEPSLATE_TILES);
			}
		}
		for (int k = 0; k < CRAG_FLOOR; k++) {
			for (int x = cx - 1; x <= cx + 1; x++) {
				int z = CRAG_HEAD_Z - CRAG_FLOOR + k;
				if (k > 0) piece.fill(level, chunk, x, 1, z, x, k, z, Blocks.POLISHED_DEEPSLATE);
				piece.put(level, chunk, x, k + 1, z, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
			}
		}
		int[] seat = seat(piece);
		piece.put(level, chunk, seat[0], seat[1], seat[2], ModBlocks.RED_MEDITATION_MAT);
		piece.chest(level, chunk, cx - 3, CRAG_FLOOR + 1, seat[2] + 1, Direction.NORTH, SectPiece.MASTER_HALL_LOOT);
		piece.chest(level, chunk, cx + 3, CRAG_FLOOR + 1, seat[2] + 1, Direction.NORTH, SectPiece.MASTER_HALL_LOOT);
		// The hoard: gold heaped along the alcove's wall, amethyst growing from it, the chests and the pedestals.
		int i = 0;
		for (int z = HOARD_Z0; z <= HOARD_Z1; z++) {
			for (int x = HOARD_X0; x <= HOARD_X1; x++) {
				if (m.hollow(x, z) < 3) continue;
				double roll = hash(piece.seed + 3, x, z);
				if (x == HOARD_X0) piece.put(level, chunk, x, 1, z, roll < 0.5 ? Blocks.GOLD_BLOCK : Blocks.RAW_GOLD_BLOCK);
				if (x == HOARD_X0 && roll < 0.4) piece.put(level, chunk, x, 2, z, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP));
				if (x == HOARD_X0 + 1 && z % 3 == 0) piece.chest(level, chunk, x, 1, z, Direction.EAST, SectPiece.TREASURY_LOOT);
				if (x == HOARD_X0 + 2 && z % 4 == 1) piece.pedestal(level, chunk, x, 1, z, piece.treasure(random, i++));
			}
		}
		// The elders' ledges, steps up to each from the floor.
		for (int[] ledge : ledgeSpots(piece)) {
			int lx = ledge[0], lz = ledge[1], toward = lx > cx ? -1 : 1;
			for (int x = lx - 1; x <= lx + 1; x++) {
				for (int z = lz - 1; z <= lz + 1; z++) piece.fill(level, chunk, x, 1, z, x, CRAG_FLOOR, z, Blocks.POLISHED_DEEPSLATE);
			}
			for (int k = 0; k < CRAG_FLOOR; k++) {
				int sx = lx + toward * (2 + CRAG_FLOOR - 1 - k);
				if (k > 0) piece.fill(level, chunk, sx, 1, lz, sx, k, lz, Blocks.POLISHED_DEEPSLATE);
				piece.put(level, chunk, sx, k + 1, lz, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, toward < 0 ? Direction.EAST : Direction.WEST));
			}
			piece.put(level, chunk, lx, CRAG_FLOOR + 1, lz, ModBlocks.MEDITATION_MAT);
		}
		for (int[] mat : mats(piece)) piece.put(level, chunk, mat[0], mat[1], mat[2], ModBlocks.MEDITATION_MAT);
		// Lanterns hung from the roof.
		for (int[] at : new int[][] {{cx, 10}, {cx - 8, 15}, {cx + 8, 15}, {cx, 19}, {HOARD_X0 + 2, 13}}) light(piece, level, chunk, p, m, at[0], at[1]);
	}

	// =====================================================================================================================
	// What stands about the grounds
	// =====================================================================================================================

	private static void feature(SectPiece piece, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		String what = piece.extra().getString("Feature");
		int w = piece.width(), d = piece.depth();
		switch (what) {
			case "boulder" -> raise(piece, level, chunk, new Mound(piece.seed, w, d, 3.5 + hash(piece.seed, 1, 1) * 2, 0.5, 0.5, 9, 0, 0, 0, 0), false);
			case "spire" -> {
				Mound spire = new Mound(piece.seed, w, d, 12 + hash(piece.seed, 2, 2) * 8, 2.2, 0.15, 9, 0, 0, 0, 0);
				raise(piece, level, chunk, spire, false);
			}
			case "pool" -> pool(piece, level, chunk, w, d);
			case "torii_row" -> {
				for (int z = 1; z < d - 1; z += 3) {
					for (int x : new int[] {0, w - 1}) {
						piece.put(level, chunk, x, 0, z, p.foundation);
						piece.fill(level, chunk, x, 1, z, x, 5, z, p.pillar(Direction.Axis.Y));
					}
					piece.fill(level, chunk, 1, 4, z, w - 2, 4, z, p.beam(Direction.Axis.X));
					piece.fill(level, chunk, 0, 6, z, w - 1, 6, z, p.roofTrim);
					if ((z / 3) % 3 == 1) piece.lanternHanging(level, chunk, p, w / 2, 3, z);
				}
			}
			default -> {
			}
		}
	}

	/** A pool: still water two deep in a rim of mud, reeds about its edge, lily pads on it. */
	private static void pool(SectPiece piece, WorldGenLevel level, BoundingBox chunk, int w, int d) {
		double cx = (w - 1) / 2.0, cz = (d - 1) / 2.0;
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				if (!here(piece, chunk, x, z)) continue;
				double nx = (x - cx) / (w / 2.0), nz = (z - cz) / (d / 2.0), r2 = nx * nx + nz * nz;
				if (r2 > 1) continue;
				piece.air(level, chunk, x, 1, z, x, 3, z);
				if (r2 > 0.55) {
					piece.put(level, chunk, x, 0, z, Blocks.MUD);
					// Reeds on the rim where the water touches it (sugar cane grows only by water).
					boolean byWater = false;
					for (int[] step : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
						double sx = (x + step[0] - cx) / (w / 2.0), sz = (z + step[1] - cz) / (d / 2.0);
						if (sx * sx + sz * sz <= 0.55) byWater = true;
					}
					if (byWater && hash(piece.seed, x, z) < 0.5) {
						int tall = 1 + (int) (hash(piece.seed + 1, x, z) * 3);
						for (int y = 1; y <= tall; y++) piece.put(level, chunk, x, y, z, Blocks.SUGAR_CANE);
					}
					continue;
				}
				piece.put(level, chunk, x, -2, z, Blocks.MUD);
				piece.put(level, chunk, x, -1, z, Blocks.WATER.defaultBlockState());
				piece.put(level, chunk, x, 0, z, Blocks.WATER.defaultBlockState());
				if (hash(piece.seed + 2, x, z) < 0.12) piece.put(level, chunk, x, 1, z, Blocks.LILY_PAD);
			}
		}
	}

	/**
	 * The undergrowth a den's grounds have where a sect's have bamboo (SectPiece#terrace): a tiger's ferns, a lizard's tall grass and
	 * orchids, a fox's azaleas and white flowers, a dragon's mossy rocks; only on earth (a rock on anything else).
	 */
	static void brush(SectPiece piece, WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int z, RandomSource random) {
		BlockState ground = piece.at(level, chunk, x, 0, z);
		boolean earth = ground.is(net.minecraft.tags.BlockTags.DIRT);
		if (!earth || piece.race == Bloodline.DRAGON) {
			piece.put(level, chunk, x, 1, z, random.nextBoolean() ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLED_DEEPSLATE);
			return;
		}
		switch (piece.race) {
			case LIZARD -> {
				if (random.nextInt(3) == 0) piece.put(level, chunk, x, 1, z, Blocks.BLUE_ORCHID);
				else tall(piece, level, chunk, x, z, Blocks.TALL_GRASS);
			}
			case FOX -> {
				int roll = random.nextInt(4);
				if (roll == 0) piece.put(level, chunk, x, 1, z, Blocks.FLOWERING_AZALEA);
				else if (roll == 1) piece.put(level, chunk, x, 1, z, Blocks.AZALEA);
				else piece.put(level, chunk, x, 1, z, roll == 2 ? Blocks.LILY_OF_THE_VALLEY : Blocks.WHITE_TULIP);
			}
			default -> {
				if (random.nextInt(3) == 0) piece.put(level, chunk, x, 1, z, Blocks.FERN);
				else tall(piece, level, chunk, x, z, Blocks.LARGE_FERN);
			}
		}
	}

	/** A two-high plant at (x, z). */
	private static void tall(SectPiece piece, WorldGenLevel level, BoundingBox chunk, int x, int z, Block plant) {
		piece.put(level, chunk, x, 1, z, plant.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
		piece.put(level, chunk, x, 2, z, plant.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
	}
}
