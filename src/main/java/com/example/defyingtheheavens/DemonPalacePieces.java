package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.BitSet;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Demon Palace's own buildings (DemonPalacePlan), drawn in a SectPiece's frame as every sect building is (x across its front, z from
 * its front inward, y from the grounds' paving; in the frame NORTH is inward, SOUTH outward):
 * <ul>
 *   <li><b>The great torii</b> ({@link #gate}): two pillars two blocks thick on stone footings, a tie beam through them, the
 *   palace's plaque of gold over it, and the top beam sweeping up at its ends; lanterns hung from the tie beam, banners on the
 *   pillars. Two lesser torii stand behind it up the way. (A gate of the beast's head, the way through its jaws, was tried and not
 *   liked: the user, 2026-10-25.)</li>
 *   <li><b>The Sovereign's hall</b> ({@link #hall}): on a dais of three steps of the race's dark stone, the hall of two eaves, pillars
 *   before its walls and two rows within, a carpet from its doors to the Demon Sovereign's throne on its platform under a screen of
 *   gold, braziers and the Sovereign's chests either side.</li>
 *   <li><b>The colossus</b> ({@link #colossus}): the race's beast in stone (PalaceSculpt), right behind the hall, its head over the
 *   hall's roof: a shell two blocks thick, its eyes and the race's fire burning.</li>
 *   <li><b>The blood ring</b> ({@link #pool}): the pool of the race's blood round the Formation plaza at the palace's heart, between
 *   rims of dark stone with a low wall along them, lit red from under its floor, four bridges over it, and eight great fangs of bone
 *   rising from its outer rim and leaning in over it.</li>
 *   <li><b>A statue of the Way of Beasts</b> ({@link #statue}): the beast smaller, on a plinth, looking across the way.</li>
 * </ul>
 */
final class DemonPalacePieces {
	/** The Sovereign's hall's lot: across its front, and deep (its dais's three steps, the hall on the top one). */
	static final int HALL_WIDTH = 41, HALL_DEPTH = 31, HALL_HEIGHT = 30;
	/** The great torii's lot, and a lesser one's. */
	static final int GATE_WIDTH = 29, GATE_DEPTH = 7, GATE_HEIGHT = 20, LESSER_WIDTH = 19, LESSER_DEPTH = 5, LESSER_HEIGHT = 14;
	/** A statue's plinth: how far it reaches past the beast each way, and how high. */
	static final int PLINTH_REACH = 1, PLINTH_HIGH = 2;
	/** The blood ring's: the plaza's edge (its inner rim begins there), the water's inner and outer edge, the outer rim's edge. */
	static final double RING_IN = 12.5, WATER_IN = 15, WATER_OUT = 22.5, RING_OUT = 24.5;

	static void draw(SectPiece piece, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		switch (piece.kind()) {
			case PALACE_GATE -> gate(piece, level, chunk, p);
			case PALACE_HALL -> hall(piece, level, chunk, p);
			case PALACE_COLOSSUS -> colossus(piece, level, chunk, p);
			case PALACE_POOL -> pool(piece, level, chunk, p);
			case PALACE_STATUE -> statue(piece, level, chunk, p);
			default -> { }
		}
	}

	// --- The great beasts ---

	/** A statue's voxels, and which of them are laid (its shell: those within two of its surface; the rest is never seen). */
	record Sculpture(PalaceSculpt.Grid grid, BitSet laid) {
		boolean laid(int x, int y, int z) {
			return grid.inside(x, y, z) && laid.get(x + grid.w * (z + grid.d * y));
		}
	}

	private static final Map<Bloodline, Sculpture> COLOSSI = new ConcurrentHashMap<>(), STATUES = new ConcurrentHashMap<>();

	/** The colossus of {@code race} (DemonPalacePlan#COLOSSUS_SCALE), sculpted once. */
	static Sculpture colossus(Bloodline race) {
		return COLOSSI.computeIfAbsent(race, r -> shell(PalaceSculpt.colossus(r, DemonPalacePlan.COLOSSUS_SCALE)));
	}

	/** A statue of the Way of Beasts of {@code race} (DemonPalacePlan#STATUE_SCALE), sculpted once. */
	static Sculpture statue(Bloodline race) {
		return STATUES.computeIfAbsent(race, r -> shell(PalaceSculpt.statue(r, DemonPalacePlan.STATUE_SCALE)));
	}

	/** {@code grid} with its shell marked: every voxel within two of the air (below its floor is the ground, not air). */
	private static Sculpture shell(PalaceSculpt.Grid grid) {
		BitSet outer = new BitSet(), laid = new BitSet();
		int[][] near = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
		for (int y = 0; y < grid.h; y++) {
			for (int z = 0; z < grid.d; z++) {
				for (int x = 0; x < grid.w; x++) {
					if (grid.get(x, y, z) == null) continue;
					for (int[] n : near) {
						int nx = x + n[0], ny = y + n[1], nz = z + n[2];
						if (ny >= 0 && grid.get(nx, ny, nz) == null) {
							outer.set(x + grid.w * (z + grid.d * y));
							break;
						}
					}
				}
			}
		}
		laid.or(outer);
		for (int y = 0; y < grid.h; y++) {
			for (int z = 0; z < grid.d; z++) {
				for (int x = 0; x < grid.w; x++) {
					if (grid.get(x, y, z) == null || outer.get(x + grid.w * (z + grid.d * y))) continue;
					for (int[] n : near) {
						int nx = x + n[0], ny = y + n[1], nz = z + n[2];
						if (grid.inside(nx, ny, nz) && outer.get(nx + grid.w * (nz + grid.d * ny))) {
							laid.set(x + grid.w * (z + grid.d * y));
							break;
						}
					}
				}
			}
		}
		return new Sculpture(grid, laid);
	}

	/** What each part of {@code race}'s beasts is made of: stone in its colours, its eyes and its fire alight. */
	private static final Map<Bloodline, Map<PalaceSculpt.Paint, BlockState>> STONES = new ConcurrentHashMap<>();

	private static BlockState stone(Bloodline race, PalaceSculpt.Paint paint) {
		return STONES.computeIfAbsent(race, DemonPalacePieces::stones).get(paint);
	}

	private static Map<PalaceSculpt.Paint, BlockState> stones(Bloodline race) {
		Map<PalaceSculpt.Paint, Block> b = new EnumMap<>(PalaceSculpt.Paint.class);
		switch (race) {
			case FOX -> {
				b.put(PalaceSculpt.Paint.BODY, Blocks.SMOOTH_QUARTZ);
				b.put(PalaceSculpt.Paint.MARK, Blocks.RED_NETHER_BRICKS);
				b.put(PalaceSculpt.Paint.BELLY, Blocks.QUARTZ_BRICKS);
				b.put(PalaceSculpt.Paint.TIP, Blocks.ORANGE_CONCRETE);
				b.put(PalaceSculpt.Paint.EYE, Blocks.OCHRE_FROGLIGHT);
				b.put(PalaceSculpt.Paint.GLOW, Blocks.SHROOMLIGHT);
				b.put(PalaceSculpt.Paint.STONE, Blocks.NETHER_BRICKS);
			}
			case LIZARD -> {
				b.put(PalaceSculpt.Paint.BODY, Blocks.GREEN_TERRACOTTA);
				b.put(PalaceSculpt.Paint.MARK, Blocks.BLACK_TERRACOTTA);
				b.put(PalaceSculpt.Paint.BELLY, Blocks.PACKED_MUD);
				b.put(PalaceSculpt.Paint.TIP, Blocks.BLACK_TERRACOTTA);
				b.put(PalaceSculpt.Paint.EYE, Blocks.OCHRE_FROGLIGHT);
				b.put(PalaceSculpt.Paint.GLOW, Blocks.SHROOMLIGHT);
				b.put(PalaceSculpt.Paint.CROWN, ModBlocks.JADE_STONE);
				b.put(PalaceSculpt.Paint.CLAW, Blocks.BONE_BLOCK);
				b.put(PalaceSculpt.Paint.MANE, Blocks.MOSS_BLOCK);
				b.put(PalaceSculpt.Paint.STONE, Blocks.POLISHED_ANDESITE);
			}
			case DRAGON -> {
				b.put(PalaceSculpt.Paint.BODY, Blocks.LIGHT_BLUE_CONCRETE);
				b.put(PalaceSculpt.Paint.MARK, Blocks.BLUE_CONCRETE);
				b.put(PalaceSculpt.Paint.BELLY, Blocks.HONEYCOMB_BLOCK);
				b.put(PalaceSculpt.Paint.TIP, Blocks.BLUE_CONCRETE);
				b.put(PalaceSculpt.Paint.EYE, Blocks.OCHRE_FROGLIGHT);
				b.put(PalaceSculpt.Paint.GLOW, Blocks.SEA_LANTERN);
				b.put(PalaceSculpt.Paint.MANE, Blocks.WAXED_OXIDIZED_COPPER);
				b.put(PalaceSculpt.Paint.STONE, Blocks.POLISHED_DEEPSLATE);
			}
			default -> { // TIGER
				b.put(PalaceSculpt.Paint.BODY, Blocks.SMOOTH_QUARTZ);
				b.put(PalaceSculpt.Paint.MARK, Blocks.POLISHED_BLACKSTONE);
				b.put(PalaceSculpt.Paint.BELLY, Blocks.QUARTZ_BRICKS);
				b.put(PalaceSculpt.Paint.TIP, Blocks.POLISHED_BLACKSTONE);
				b.put(PalaceSculpt.Paint.EYE, Blocks.SEA_LANTERN); // pale fire, the White Tiger's
				b.put(PalaceSculpt.Paint.GLOW, Blocks.SEA_LANTERN);
				b.put(PalaceSculpt.Paint.STONE, Blocks.POLISHED_BLACKSTONE_BRICKS);
			}
		}
		// The rest alike for every race: fangs of bone, claws and crowns of gold's colour (not gold: a colossus is hundreds of blocks of
		// what it is made of, and nothing in it is worth quarrying), a mane of white.
		b.putIfAbsent(PalaceSculpt.Paint.FANG, Blocks.BONE_BLOCK);
		b.putIfAbsent(PalaceSculpt.Paint.CLAW, Blocks.HONEYCOMB_BLOCK);
		b.putIfAbsent(PalaceSculpt.Paint.CROWN, Blocks.HONEYCOMB_BLOCK);
		b.putIfAbsent(PalaceSculpt.Paint.MANE, Blocks.QUARTZ_BLOCK);
		Map<PalaceSculpt.Paint, BlockState> states = new EnumMap<>(PalaceSculpt.Paint.class);
		b.forEach((paint, block) -> states.put(paint, block.defaultBlockState()));
		return states;
	}

	/**
	 * Lays {@code sculpture}'s shell in {@code g}'s frame, its grid's (0, 0, 0) at (ox, oy, oz), only the columns on {@code chunk} (a
	 * colossus crosses some twenty chunks: each draws its own). Under anything on its floor, foundation down to the ground.
	 */
	private static void sculpt(SectPiece g, WorldGenLevel level, BoundingBox chunk, Bloodline race, Sculpture sculpture, int ox, int oy, int oz,
			BlockState footing, boolean small) {
		PalaceSculpt.Grid grid = sculpture.grid();
		int[] in = g.localBounds(chunk);
		for (int x = Math.max(0, in[0] - ox); x <= Math.min(grid.w - 1, in[2] - ox); x++) {
			for (int z = Math.max(0, in[1] - oz); z <= Math.min(grid.d - 1, in[3] - oz); z++) {
				for (int y = 0; y < grid.h; y++) {
					if (!sculpture.laid(x, y, z)) continue;
					PalaceSculpt.Paint paint = grid.get(x, y, z);
					// A small statue's claws in its own stone (in gold's colour they stood out as slabs at its feet: tour 12).
					if (small && paint == PalaceSculpt.Paint.CLAW) paint = PalaceSculpt.Paint.BODY;
					g.put(level, chunk, x + ox, y + oy, z + oz, stone(race, paint));
					if (y == 0) g.foundation(level, chunk, x + ox, oy, z + oz, footing);
				}
			}
		}
	}

	/** The colossus, right behind the Sovereign's hall: standing on the grounds. */
	static void colossus(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		sculpt(g, level, chunk, g.race, colossus(g.race), 0, 1, 0, p.foundation.defaultBlockState(), false);
	}

	/** A statue of the Way of Beasts on its plinth, looking across the way (its front). */
	static void statue(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = g.width(), d = g.depth();
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				g.put(level, chunk, x, 0, z, p.foundation);
				g.foundation(level, chunk, x, 0, z, p.foundation.defaultBlockState());
				g.put(level, chunk, x, 1, z, p.foundation);
				boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
				g.put(level, chunk, x, PLINTH_HIGH, z, edge ? p.trimSlab(false) : p.wallBase.defaultBlockState());
			}
		}
		sculpt(g, level, chunk, g.race, statue(g.race), PLINTH_REACH, PLINTH_HIGH + 1, PLINTH_REACH, p.foundation.defaultBlockState(), true);
	}

	// --- The great torii ---

	/**
	 * The great torii across the way in (or with "Lesser" a lesser one behind it): two pillars on stone footings, the tie beam through
	 * them reaching past them, the plaque of gold between it and the top beam, which sweeps up at its ends; lanterns hung from the tie
	 * beam, banners on the pillars.
	 */
	static void gate(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		boolean lesser = g.extra().getBoolean("Lesser");
		int w = g.width(), d = g.depth(), cx = w / 2, z = d / 2;
		int half = lesser ? 6 : 8, thick = lesser ? 1 : 2, top = lesser ? 10 : 15, tie = lesser ? 7 : 11, reach = lesser ? 8 : 12;
		g.air(level, chunk, 0, 1, 0, w - 1, top + 3, d - 1);
		for (int s : new int[] {-1, 1}) {
			// A pillar, thick, on its footing of the race's stone, a skirt of stairs round its foot.
			int x0 = s < 0 ? cx - half - thick + 1 : cx + half, x1 = x0 + thick - 1, z0 = z - thick / 2, z1 = z0 + thick - 1;
			for (int x = x0 - 1; x <= x1 + 1; x++) {
				for (int zz = z0 - 1; zz <= z1 + 1; zz++) {
					g.put(level, chunk, x, 0, zz, p.foundation);
					boolean core = x >= x0 && x <= x1 && zz >= z0 && zz <= z1;
					if (!core) {
						Direction toward = x < x0 ? Direction.EAST : x > x1 ? Direction.WEST : zz < z0 ? Direction.NORTH : Direction.SOUTH;
						g.put(level, chunk, x, 1, zz, p.baseStair(toward));
					}
				}
			}
			for (int x = x0; x <= x1; x++) {
				for (int zz = z0; zz <= z1; zz++) g.pillar(level, chunk, p, x, 1, top - 1, zz);
			}
			// Banners on the pillars, out to the front and the back.
			g.wallBanner(level, chunk, p, s < 0 ? x0 : x1, tie - 3, z0 - 1, Direction.SOUTH);
			g.wallBanner(level, chunk, p, s < 0 ? x0 : x1, tie - 3, z1 + 1, Direction.NORTH);
		}
		// The tie beam through the pillars, reaching past them; lanterns hung under it.
		int z0 = z - thick / 2, z1 = z0 + thick - 1;
		for (int zz = z0; zz <= z1; zz++) {
			for (int x = cx - reach + 1; x <= cx + reach - 1; x++) g.put(level, chunk, x, tie, zz, p.beam(Direction.Axis.X));
			for (int s : new int[] {-1, 1}) g.lanternHanging(level, chunk, p, cx + s * (half / 2 + 1), tie - 1, zz);
		}
		// The plaque of gold in a frame of the trim between the tie beam and the top, on a strut (the lesser torii have the strut alone).
		for (int zz = z0; zz <= z1; zz++) {
			if (lesser) {
				g.fill(level, chunk, cx, tie + 1, zz, cx, top - 1, zz, p.roofTrim);
			} else {
				g.fill(level, chunk, cx - 2, tie + 1, zz, cx + 2, top - 1, zz, p.roofTrim);
				g.fill(level, chunk, cx - 1, tie + 1, zz, cx + 1, top - 2, zz, p.accent);
			}
		}
		// The top beam over all, its ends sweeping up.
		for (int zz = z0 - (lesser ? 0 : 1); zz <= z1 + (lesser ? 0 : 1); zz++) g.lintel(level, chunk, p, cx - reach, cx + reach, top, zz, true, true);
	}

	// --- The Sovereign's hall ---

	/** The Demon Sovereign's seat on its throne (the master's post, in the hall's frame): before the screen at the hall's back. */
	static int[] throne(SectPiece g) {
		return new int[] {g.width() / 2, 5, g.depth() - 6};
	}

	/**
	 * The Sovereign's hall: a dais of three steps of the race's dark stone (each step's front a flight of stairs all along it), on the
	 * top one the hall, 25 across and 16 deep, alike either side of its middle: a row of pillars before its walls every four from its
	 * doors out to its corners and two rows within, its doors wide, windows down its sides, two eaves; within, a carpet from the
	 * doors to the throne on its platform, a screen of gold behind it, braziers and the Sovereign's chests either side, banners,
	 * lanterns hung from the ceiling.
	 */
	static void hall(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = g.width(), d = g.depth(), cx = w / 2;
		g.air(level, chunk, 0, 1, 0, w - 1, HALL_HEIGHT - 1, d - 1);
		// The dais: three steps, each a flight of stairs along its front.
		int[][] steps = {{1, 3}, {3, 6}, {5, 9}}; // {inset from each side, front row}
		for (int k = 0; k < 3; k++) {
			int inset = steps[k][0], front = steps[k][1], y = k + 1;
			for (int x = inset; x <= w - 1 - inset; x++) {
				for (int z = front; z < d; z++) g.put(level, chunk, x, y, z, p.foundation);
				g.put(level, chunk, x, y, front - 1, p.baseStair(Direction.NORTH));
			}
		}
		for (int x = 1; x <= w - 2; x++) {
			for (int z = 3; z < d; z++) g.foundation(level, chunk, x, 1, z, p.foundation.defaultBlockState());
		}
		// (Its back wall stands three in from the lot's back, so the eaves end inside it: the colossus stands right behind. Its walls
		// twelve either side of its middle, so its pillars stand every four from the doors out to the corners, alike either side: the
		// user, 2026-10-25, "make sure the buildings have symmetrical pillars".)
		int floor = 3, x0 = cx - 12, x1 = cx + 12, z0 = 12, z1 = d - 4, wallTop = floor + 10;
		// The floor within, and the walls: a course of the base stone, then the walls, a band of beam under the eaves.
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean wall = x == x0 || x == x1 || z == z0 || z == z1;
				g.put(level, chunk, x, floor, z, wall ? p.foundation : p.floor);
				if (!wall) continue;
				g.put(level, chunk, x, floor + 1, z, p.wallBase);
				g.fill(level, chunk, x, floor + 2, z, x, wallTop - 1, z, p.wall);
				g.put(level, chunk, x, wallTop, z, p.beam(x == x0 || x == x1 ? Direction.Axis.Z : Direction.Axis.X));
			}
		}
		// The doors, wide, and windows down the sides.
		g.air(level, chunk, cx - 3, floor + 1, z0, cx + 3, floor + 7, z0);
		for (int z = z0 + 3; z < z1 - 1; z += 4) {
			for (int x : new int[] {x0, x1}) g.fill(level, chunk, x, floor + 4, z, x, floor + 6, z, p.window);
		}
		// Pillars at the corners; a row before the front, every four from the doors out to the corners either side; two rows within.
		for (int x : new int[] {x0, x1}) {
			for (int z : new int[] {z0, z1}) g.pillar(level, chunk, p, x, floor + 1, wallTop - 1, z);
		}
		for (int off : new int[] {4, 8, 12}) {
			for (int s : new int[] {-1, 1}) g.pillar(level, chunk, p, cx + s * off, floor + 1, wallTop, z0 - 2);
		}
		for (int z = z0 + 3; z <= z1 - 5; z += 4) {
			for (int s : new int[] {-1, 1}) g.pillar(level, chunk, p, cx + s * 6, floor + 1, wallTop - 1, z);
		}
		// The porch's roof over the pillars before the front, and the two eaves.
		g.roof(level, chunk, p, x0 - 3, z0 - 4, x1 + 3, z1 + 3, wallTop + 1, 3, false, true);
		int ux0 = x0 + 2, ux1 = x1 - 2, uz0 = z0 + 2, uz1 = z1 - 2, upper = wallTop + 4;
		for (int x = ux0; x <= ux1; x++) {
			for (int z = uz0; z <= uz1; z++) {
				if (x == ux0 || x == ux1 || z == uz0 || z == uz1) g.fill(level, chunk, x, wallTop + 1, z, x, upper - 1, z, p.wall);
			}
		}
		g.roof(level, chunk, p, ux0 - 2, uz0 - 2, ux1 + 2, uz1 + 2, upper, SectPiece.ridgeCourses(ux0 - 2, uz0 - 2, ux1 + 2, uz1 + 2), true, true);
		// The carpet from the doors to the throne; the throne's platform, its steps, the seat, its screen of gold.
		int[] seat = throne(g);
		for (int z = z0 + 1; z < seat[2] - 2; z++) {
			for (int x = cx - 1; x <= cx + 1; x++) g.put(level, chunk, x, floor + 1, z, x == cx ? p.carpet : p.carpetAlt);
		}
		for (int x = cx - 5; x <= cx + 5; x++) {
			for (int z = seat[2] - 1; z <= z1 - 1; z++) g.put(level, chunk, x, floor + 1, z, p.foundation);
			g.put(level, chunk, x, floor + 1, seat[2] - 2, p.baseStair(Direction.NORTH));
		}
		g.put(level, chunk, seat[0], seat[1], seat[2], p.baseStair(Direction.NORTH)); // the seat, its back to the screen
		for (int s : new int[] {-1, 1}) g.put(level, chunk, seat[0] + s, seat[1], seat[2], p.trimSlab(false)); // its arms
		g.fill(level, chunk, cx - 3, floor + 2, z1 - 1, cx + 3, floor + 8, z1 - 1, p.roofTrim);
		g.fill(level, chunk, cx - 2, floor + 3, z1 - 1, cx + 2, floor + 7, z1 - 1, p.accent);
		for (int s : new int[] {-1, 1}) {
			g.chest(level, chunk, cx + s * 4, floor + 2, seat[2] + 1, Direction.SOUTH, SectPiece.MASTER_HALL_LOOT);
			g.put(level, chunk, cx + s * 6, floor + 1, seat[2] - 1, (p.soulFire ? Blocks.SOUL_CAMPFIRE : Blocks.CAMPFIRE).defaultBlockState()
					.setValue(CampfireBlock.LIT, true));
			g.candles(level, chunk, p, cx + s * 2, floor + 2, seat[2] + 1, 4);
		}
		// Banners down the walls within; lanterns hung from the ceiling.
		for (int z = z0 + 2; z < z1 - 2; z += 4) {
			g.wallBanner(level, chunk, p, x0 + 1, floor + 6, z, Direction.EAST);
			g.wallBanner(level, chunk, p, x1 - 1, floor + 6, z, Direction.WEST);
			for (int x : new int[] {cx - 4, cx + 4}) g.lanternHanging(level, chunk, p, x, wallTop - 1, z);
		}
		// Before it on the top step, a brazier between each pair of the outer pillars either side (a great one there stood into them).
		for (int s : new int[] {-1, 1}) {
			g.put(level, chunk, cx + s * 10, floor + 1, z0 - 3, (p.soulFire ? Blocks.SOUL_CAMPFIRE : Blocks.CAMPFIRE).defaultBlockState()
					.setValue(CampfireBlock.LIT, true));
		}
	}

	// --- The blood ring ---

	/**
	 * The blood ring round the plaza (its piece the ring's square, the plaza at its middle): an inner rim from the plaza's edge, the pool
	 * (its water two deep over red glass lit from under it, so the blood glows red through), an outer rim, a low wall along both rims,
	 * the four bridges on the ways (walled along their sides), and eight great fangs of bone from the outer rim leaning in over it.
	 */
	static void pool(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int size = g.width(), c = size / 2;
		for (int x = 0; x < size; x++) {
			for (int z = 0; z < size; z++) {
				double r = Math.sqrt((x - c) * (x - c) + (z - c) * (z - c));
				if (r <= RING_IN || r > RING_OUT + 0.5) continue;
				int along = Math.min(Math.abs(x - c), Math.abs(z - c)); // how far off the nearer way's middle line
				boolean bridge = along <= 3; // its deck five wide, a wall either side of it
				boolean water = r > WATER_IN && r <= WATER_OUT;
				g.air(level, chunk, x, 1, z, x, 4, z);
				if (water && !bridge) {
					g.put(level, chunk, x, -4, z, p.foundation);
					g.put(level, chunk, x, -3, z, Blocks.SHROOMLIGHT);
					g.put(level, chunk, x, -2, z, Blocks.RED_STAINED_GLASS);
					g.put(level, chunk, x, -1, z, Blocks.WATER);
					g.put(level, chunk, x, 0, z, Blocks.WATER);
					continue;
				}
				// A rim, or a bridge over the water: stone down to the pool's floor, the way's paving on the deck.
				for (int y = -4; y <= -1; y++) g.put(level, chunk, x, y, z, p.foundation);
				g.put(level, chunk, x, 0, z, along <= 2 ? p.path : p.foundation);
				// A low wall along the water's edge on either rim (open where the bridges cross), and along a bridge's sides over it.
				boolean byWater = r > WATER_IN - 1 && r <= WATER_IN || r > WATER_OUT && r <= WATER_OUT + 1;
				if (water ? along == 3 : byWater && !bridge) g.put(level, chunk, x, 1, z, p.baseWall);
			}
		}
		// Lanterns at the bridges' ends on the outer rim.
		for (int k = 0; k < 4; k++) {
			int dx = k == 0 ? 1 : k == 1 ? -1 : 0, dz = k == 2 ? 1 : k == 3 ? -1 : 0;
			for (int s : new int[] {-1, 1}) {
				int x = c + dx * 24 + (dz != 0 ? s * 3 : 0), z = c + dz * 24 + (dx != 0 ? s * 3 : 0);
				g.stoneLantern(level, chunk, p, x, 1, z);
			}
		}
		// The eight fangs, between the bridges.
		for (int k = 0; k < 8; k++) {
			double angle = Math.PI / 4 * k + Math.PI / 8;
			double ox = Math.cos(angle), oz = Math.sin(angle);
			fang(g, level, chunk, c + ox * 23.5, c + oz * 23.5, -ox, -oz);
		}
		bloodLand(g, level, chunk);
	}

	/**
	 * The land round the blood ring made the blood ring's own (ModBiomes#BLOOD_RING), whose water runs red: water takes its colour from its
	 * land, and in the Great Void's steppes and Dao fields the ring's blood showed violet (tour 10). Only this chunk's (worldgen draws a
	 * piece chunk by chunk; this one is being made), the columns within its outer rim and a little more, the ring's height.
	 */
	private static void bloodLand(SectPiece g, WorldGenLevel level, BoundingBox chunk) {
		Holder<Biome> blood = level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(ModBiomes.BLOOD_RING).orElse(null);
		if (blood == null) return;
		BlockPos heart = g.toWorld(g.width() / 2, 0, g.depth() / 2);
		double reach = RING_OUT + 2;
		ChunkAccess here = level.getChunk(SectionPos.blockToSectionCoord(chunk.minX()), SectionPos.blockToSectionCoord(chunk.minZ()));
		int qx = QuartPos.fromBlock(here.getPos().getMinBlockX()), qz = QuartPos.fromBlock(here.getPos().getMinBlockZ());
		java.util.Set<Integer> done = new java.util.HashSet<>();
		for (int y : new int[] {heart.getY() - 6, heart.getY(), heart.getY() + 6}) {
			int index = here.getSectionIndex(y);
			if (index < 0 || index >= here.getSections().length || !done.add(index)) continue;
			LevelChunkSection section = here.getSection(index);
			int qy = QuartPos.fromSection(SectionPos.blockToSectionCoord(y));
			section.fillBiomesFromNoise((x, yy, z, sampler) -> {
				double dx = QuartPos.toBlock(x) + 2 - heart.getX(), dz = QuartPos.toBlock(z) + 2 - heart.getZ();
				return dx * dx + dz * dz <= reach * reach ? blood : section.getNoiseBiome(x & 3, yy & 3, z & 3);
			}, null, qx, qy, qz);
		}
	}

	/** A great fang of bone rooted at (x, z), rising ten high and leaning in toward (ix, iz), tapering to its point. */
	private static void fang(SectPiece g, WorldGenLevel level, BoundingBox chunk, double x, double z, double ix, double iz) {
		BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
		for (double t = 0; t <= 1.0001; t += 0.05) {
			double cx = x + ix * 4.5 * t * t, cz = z + iz * 4.5 * t * t, cy = 1 + 10 * t, radius = 1.6 * (1 - t) + 0.35;
			for (int bx = (int) Math.floor(cx - radius); bx <= (int) Math.ceil(cx + radius); bx++) {
				for (int bz = (int) Math.floor(cz - radius); bz <= (int) Math.ceil(cz + radius); bz++) {
					double ddx = bx + 0.5 - cx, ddz = bz + 0.5 - cz;
					if (ddx * ddx + ddz * ddz <= radius * radius) g.put(level, chunk, bx, (int) Math.floor(cy), bz, bone);
				}
			}
		}
	}

	private DemonPalacePieces() {}
}
