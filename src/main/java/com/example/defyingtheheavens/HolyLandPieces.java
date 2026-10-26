package com.example.defyingtheheavens;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A Holy Land's own buildings (HolyLandPlan; the user's ask, 2026-10-25: "larger and more grand than the previous sects, they should
 * have larger buildings and cover more ground", not the sects' over again), drawn in a SectPiece's frame as every sect building is
 * (x across its front, z from its front inward, y from the grounds' paving; in the frame NORTH is inward, SOUTH outward):
 * <ul>
 *   <li><b>The Mountain Gate</b> ({@link #gate}): a paifang of three bays on piers, each bay under a roof of its own, the middle
 *   one high with the Holy Land's plaque of gold; guardian lions before it.</li>
 *   <li><b>The Patriarch's Palace</b> ({@link #palace}): a hall of triple eaves on three terraces of white jade (the Forbidden City's
 *   three-tiered terrace), each with its balustrade and its flights of stairs either side of a carved ramp of gold; a colonnade
 *   along its front, inside two rows of pillars each side of a carpet to the Patriarch's throne, three steps up before a gold
 *   screen, braziers burning either side.</li>
 *   <li><b>The lotus moat</b> ({@link #moat}): a channel of still water across the grounds, lily pads on it, and an arched bridge of
 *   white jade over it where the Spirit Way crosses.</li>
 *   <li><b>A bell or drum tower</b> ({@link #tower}): a pavilion of two eaves on a high podium, its bell hung from the ceiling, or
 *   its great drum on a stand.</li>
 *   <li><b>The alchemy hall</b> ({@link #alchemy}): three pill furnaces of bronze along its back, brewing tables and herbs down
 *   its sides, a great censer smoking before it.</li>
 *   <li><b>An elder's court</b> ({@link #court}): the walled yard round an Elder's house (an ELDER_HOUSE piece of its own), a roofed
 *   gateway, a blossom tree and a pond.</li>
 *   <li><b>A guardian lion</b> ({@link #statue}): a lion of gold sitting up on a high plinth, watching the way.</li>
 * </ul>
 */
final class HolyLandPieces {
	static void draw(SectPiece piece, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		switch (piece.kind()) {
			case HOLY_GATE -> gate(piece, level, chunk, p);
			case HOLY_PALACE -> palace(piece, level, chunk, p);
			case HOLY_MOAT -> moat(piece, level, chunk, p);
			case HOLY_TOWER -> tower(piece, level, chunk, p);
			case HOLY_ALCHEMY -> alchemy(piece, level, chunk, p);
			case HOLY_COURT -> court(piece, level, chunk, p);
			case HOLY_STATUE -> statue(piece, level, chunk, p);
			default -> { }
		}
	}

	// --- Shared ---

	/**
	 * Stairs up to a terrace's front edge at {@code z0} (its top at {@code top}) from the level below ({@code from}, its top), across
	 * x0..x1, built out in front of it; with {@code ramp} the middle three a carved ramp of the accent between two flights.
	 */
	private static void stairs(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int x1, int z0, int from, int top, boolean ramp) {
		int cx = (x0 + x1) / 2;
		for (int i = 0; i < top - from; i++) {
			int y = top - i, z = z0 - 1 - i;
			for (int x = x0; x <= x1; x++) {
				if (y - 1 > from) g.fill(level, chunk, x, from + 1, z, x, y - 1, z, p.foundation);
				g.put(level, chunk, x, y, z, ramp && Math.abs(x - cx) <= 1 ? p.accent.defaultBlockState() : p.baseStair(Direction.NORTH));
			}
		}
	}

	/** A pier: a row of pillars {@code x}, from z0 to z1, up to {@code top}, on a stone footing with stone feet round it. */
	private static void pier(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int z0, int z1, int top) {
		for (int z = z0; z <= z1; z++) {
			g.put(level, chunk, x, 0, z, p.foundation);
			g.pillar(level, chunk, p, x, 1, top, z);
			g.put(level, chunk, x - 1, 1, z, p.baseStair(Direction.EAST));
			g.put(level, chunk, x + 1, 1, z, p.baseStair(Direction.WEST));
		}
		g.put(level, chunk, x, 1, z0 - 1, p.baseStair(Direction.NORTH));
		g.put(level, chunk, x, 1, z1 + 1, p.baseStair(Direction.SOUTH));
	}

	/** A guardian lion on its plinth at (x, z), looking out to the front (as the gate does). */
	private static void lion(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int z) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				g.put(level, chunk, x + dx, 1, z + dz, p.foundation);
				g.foundation(level, chunk, x + dx, 1, z + dz, p.foundation.defaultBlockState());
			}
		}
		g.put(level, chunk, x, 2, z, p.accent);
		g.put(level, chunk, x, 2, z + 1, p.accent);
		g.put(level, chunk, x, 3, z, p.accent);
		g.put(level, chunk, x, 4, z, p.trimStair(Direction.NORTH, false)); // the head, its face to the front
		g.put(level, chunk, x - 1, 2, z - 1, p.baseStair(Direction.NORTH)); // its paws
		g.put(level, chunk, x + 1, 2, z - 1, p.baseStair(Direction.NORTH));
		g.put(level, chunk, x, 3, z + 1, p.trimSlab(false)); // its mane falling behind
	}

	// --- The Mountain Gate ---

	/**
	 * The Mountain Gate, 35 across and 9 deep: four piers of three pillars each, the inner pair 15 high, the outer 10; the middle bay
	 * eleven wide under its own high roof with the plaque of gold in a frame of the trim over its tie beams, front and back; each side
	 * bay under a lower roof the inner piers rise through; lanterns hung from the beams, banners on the inner piers, and a guardian
	 * lion either side before it.
	 */
	static void gate(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = g.width(), d = g.depth(), cx = w / 2, z = d / 2;
		g.air(level, chunk, 0, 1, 0, w - 1, 24, d - 1);
		int inner = 6, outer = 14;
		// The side bays' roofs first, so the inner piers rise through them.
		for (int s : new int[] {-1, 1}) {
			int x0 = s < 0 ? cx - outer - 3 : cx + inner - 1, x1 = s < 0 ? cx - inner + 1 : cx + outer + 3;
			g.roof(level, chunk, p, x0, z - 2, x1, z + 2, 11, SectPiece.ridgeCourses(x0, z - 2, x1, z + 2), true, true);
		}
		for (int s : new int[] {-1, 1}) {
			pier(g, level, chunk, p, cx + s * inner, z - 1, z + 1, 15);
			pier(g, level, chunk, p, cx + s * outer, z - 1, z + 1, 10);
			// The side bay's tie beams, front and back, and a lantern under each.
			for (int zz : new int[] {z - 1, z + 1}) {
				for (int x = cx + s * (inner + 1); x != cx + s * outer; x += s) g.put(level, chunk, x, 8, zz, p.beam(Direction.Axis.X));
				g.lanternHanging(level, chunk, p, cx + s * 10, 7, zz);
			}
			// Banners on the inner piers, out to the front and the back.
			g.wallBanner(level, chunk, p, cx + s * inner, 9, z - 2, Direction.SOUTH);
			g.wallBanner(level, chunk, p, cx + s * inner, 9, z + 2, Direction.NORTH);
		}
		// The middle bay: its tie beams, the plaque over them (front and back), lanterns, and its high roof.
		for (int zz : new int[] {z - 1, z + 1}) {
			for (int x = cx - inner + 1; x <= cx + inner - 1; x++) g.put(level, chunk, x, 12, zz, p.beam(Direction.Axis.X));
			g.fill(level, chunk, cx - 3, 13, zz, cx + 3, 15, zz, p.roofTrim);
			g.fill(level, chunk, cx - 2, 14, zz, cx + 2, 14, zz, p.accent);
			for (int x : new int[] {cx - 3, cx + 3}) g.lanternHanging(level, chunk, p, x, 11, zz);
		}
		g.roof(level, chunk, p, cx - inner - 3, z - 3, cx + inner + 3, z + 3, 16, SectPiece.ridgeCourses(cx - inner - 3, z - 3, cx + inner + 3, z + 3),
				true, true);
		// The guardian lions before it.
		for (int s : new int[] {-1, 1}) lion(g, level, chunk, p, cx + s * 9, 1);
	}

	// --- The Patriarch's Palace ---

	/** The palace's three terraces, {x0, z0, x1, z1} in its frame (the first inset by its stairs' run), and each one's top. */
	private static int[][] terraces(int w, int d) {
		return new int[][] {{0, 6, w - 1, d - 1}, {5, 11, w - 6, d - 6}, {10, 16, w - 11, d - 11}};
	}

	private static final int[] TERRACE_TOPS = {3, 6, 9};
	/** The palace hall's walls in its frame (the top terrace's floor its floor), their height. */
	private static final int HALL_X = 14, HALL_FRONT = 22, HALL_BACK = 14, HALL_WALL = 11;

	/** Where the Patriarch sits, {x, y, z} in the palace's frame: on the throne's dais, before the screen. */
	static int[] throne(SectPiece g) {
		int z1 = g.depth() - HALL_BACK;
		return new int[] {g.width() / 2, TERRACE_TOPS[2] + 4, z1 - 3};
	}

	static void palace(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = g.width(), d = g.depth(), cx = w / 2;
		g.air(level, chunk, 0, 1, 0, w - 1, 62, d - 1);
		// The three terraces: faced in stone, their tops paved white, a balustrade round each rim, broken where the stairs come up.
		int[][] tiers = terraces(w, d);
		for (int t = 0; t < tiers.length; t++) {
			int x0 = tiers[t][0], z0 = tiers[t][1], x1 = tiers[t][2], z1 = tiers[t][3], top = TERRACE_TOPS[t];
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					g.fill(level, chunk, x, 1, z, x, top - 1, z, p.foundation);
					if (t == 0) g.foundation(level, chunk, x, 1, z, p.foundation.defaultBlockState());
					g.put(level, chunk, x, top, z, p.wall);
				}
			}
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					boolean rim = x == x0 || x == x1 || z == z0 || z == z1;
					if (!rim || z == z0 && Math.abs(x - cx) <= 5) continue;
					g.put(level, chunk, x, top + 1, z, p.baseWall);
				}
			}
			stairs(g, level, chunk, p, cx - 5, cx + 5, z0, t == 0 ? 0 : TERRACE_TOPS[t - 1], top, true);
		}
		// The great lantern pillars at the first terrace's corners, stone lanterns along the second's front.
		for (int[] c : new int[][] {{2, 8}, {w - 3, 8}, {2, d - 3}, {w - 3, d - 3}}) {
			g.pillar(level, chunk, p, c[0], TERRACE_TOPS[0] + 1, TERRACE_TOPS[0] + 7, c[1]);
			g.put(level, chunk, c[0], TERRACE_TOPS[0] + 8, c[1], p.accent);
			g.lanternStanding(level, chunk, p, c[0], TERRACE_TOPS[0] + 9, c[1]);
		}
		for (int x = tiers[1][0] + 3; x <= tiers[1][2] - 3; x += 6) {
			if (Math.abs(x - cx) <= 7) continue;
			g.stoneLantern(level, chunk, p, x, TERRACE_TOPS[1] + 1, tiers[1][1] + 1);
		}

		// The hall on the top terrace.
		int floor = TERRACE_TOPS[2], x0 = HALL_X, x1 = w - 1 - HALL_X, z0 = HALL_FRONT, z1 = d - HALL_BACK, top = floor + HALL_WALL;
		g.interior(level, chunk, p, x0 + 1, z0 + 1, x1 - 1, z1 - 1, floor, top + 16);
		g.walls(level, chunk, p, x0, z0, x1, z1, floor + 1, top - 1, 4, 7, 8, false);
		g.fill(level, chunk, x0 + 1, top, z0 + 1, x1 - 1, top, z1 - 1, p.beam(Direction.Axis.X)); // the ceiling
		// The colonnade along its front, under the lowest eave.
		for (int x : SectPiece.pillarSpots(x1 - x0, 4, true)) g.pillar(level, chunk, p, x0 + x, floor + 1, top, z0 - 2);
		// Triple eaves: the lowest roof, then two clerestories each under a roof of its own.
		g.roof(level, chunk, p, x0 - 2, z0 - 2, x1 + 2, z1 + 2, top + 1, 4, false, true);
		g.closeEaves(level, chunk, p, x0, z0, x1, z1, top, x0 - 2, z0 - 2, x1 + 2, z1 + 2, top + 1, 4);
		clerestory(g, level, chunk, p, x0 + 2, z0 + 2, x1 - 2, z1 - 2, top + 1, top + 4);
		g.roof(level, chunk, p, x0, z0, x1, z1, top + 5, 4, false, true);
		g.closeEaves(level, chunk, p, x0 + 2, z0 + 2, x1 - 2, z1 - 2, top + 4, x0, z0, x1, z1, top + 5, 4);
		clerestory(g, level, chunk, p, x0 + 4, z0 + 4, x1 - 4, z1 - 4, top + 5, top + 8);
		int upper = SectPiece.ridgeCourses(x0 + 2, z0 + 2, x1 - 2, z1 - 2);
		g.roof(level, chunk, p, x0 + 2, z0 + 2, x1 - 2, z1 - 2, top + 9, upper, true, true);
		g.closeEaves(level, chunk, p, x0 + 4, z0 + 4, x1 - 4, z1 - 4, top + 8, x0 + 2, z0 + 2, x1 - 2, z1 - 2, top + 9, upper);
		// The plaque over the door.
		g.fill(level, chunk, cx - 4, top - 1, z0, cx + 4, top - 1, z0, p.roofTrim);
		g.fill(level, chunk, cx - 3, top - 1, z0, cx + 3, top - 1, z0, p.accent);

		// Inside: two rows of pillars each side of the way to the throne, the sect's banners on the inner rows.
		for (int z = z0 + 3; z <= z1 - 9; z += 4) {
			for (int s : new int[] {-1, 1}) {
				g.pillar(level, chunk, p, cx + s * 7, floor + 1, top - 1, z);
				g.pillar(level, chunk, p, cx + s * 13, floor + 1, top - 1, z);
				g.wallBanner(level, chunk, p, cx + s * 6, floor + 6, z, s < 0 ? Direction.EAST : Direction.WEST);
			}
		}
		// The carpet, bordered, from the door to the dais.
		int dz = z1 - 7;
		for (int z = z0; z < dz - 1; z++) {
			g.fill(level, chunk, cx - 1, floor + 1, z, cx + 1, floor + 1, z, p.carpet);
			g.put(level, chunk, cx - 2, floor + 1, z, p.carpetAlt);
			g.put(level, chunk, cx + 2, floor + 1, z, p.carpetAlt);
		}
		// The throne's dais: three steps up, the last two of gold.
		for (int step = 0; step < 3; step++) {
			int half = 8 - 2 * step, y = floor + 1 + step;
			g.fill(level, chunk, cx - half, y, dz + step, cx + half, y, z1 - 1, step == 0 ? p.foundation : p.accent);
			for (int x = cx - half; x <= cx + half; x++) g.put(level, chunk, x, y, dz + step - 1, p.baseStair(Direction.NORTH));
		}
		int[] seat = throne(g);
		g.put(level, chunk, seat[0], seat[1], seat[2], ModBlocks.RED_MEDITATION_MAT);
		// The screen behind it, of gold in a frame of timber; candles, braziers either side of the dais.
		g.fill(level, chunk, cx - 5, floor + 4, z1 - 1, cx + 5, floor + 9, z1 - 1, p.beam(Direction.Axis.X));
		g.fill(level, chunk, cx - 4, floor + 5, z1 - 1, cx + 4, floor + 8, z1 - 1, p.roofTrim);
		g.fill(level, chunk, cx - 2, floor + 6, z1 - 1, cx + 2, floor + 7, z1 - 1, p.accent);
		g.candles(level, chunk, p, cx - 2, floor + 4, z1 - 2, 4);
		g.candles(level, chunk, p, cx + 2, floor + 4, z1 - 2, 4);
		for (int s : new int[] {-1, 1}) g.bigBrazier(level, chunk, p, cx + s * 11, floor + 1, dz + 2);
		// Lanterns hung on chains down the hall; the Patriarch's own chests at the back corners.
		for (int z = z0 + 5; z <= z1 - 9; z += 4) {
			for (int s : new int[] {-1, 1}) g.chainedLantern(level, chunk, p, cx + s * 10, top - 1, floor + 6, z);
			g.chainedLantern(level, chunk, p, cx, top - 1, floor + 7, z);
		}
		g.chest(level, chunk, x0 + 1, floor + 1, z1 - 1, Direction.SOUTH, SectPiece.MASTER_HALL_LOOT);
		g.chest(level, chunk, x1 - 1, floor + 1, z1 - 1, Direction.SOUTH, SectPiece.MASTER_HALL_LOOT);
		// Before the hall, on the top terrace: two great braziers smoking, and a censer of bronze either side of them.
		for (int s : new int[] {-1, 1}) {
			g.bigBrazier(level, chunk, p, cx + s * 9, floor + 1, tiers[2][1] + 2);
			censer(g, level, chunk, p, cx + s * 17, floor + 1, tiers[2][1] + 2);
		}
	}

	/** A clerestory's walls round x0..x1, z0..z1 from y0 to y1: a pillar at the corners and every fourth block, plaster between. */
	private static void clerestory(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int z0, int x1, int z1, int y0, int y1) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean edgeX = x == x0 || x == x1, edgeZ = z == z0 || z == z1;
				if (!edgeX && !edgeZ) continue;
				boolean post = edgeX && edgeZ || edgeZ && (x - x0) % 4 == 0 || edgeX && (z - z0) % 4 == 0;
				g.fill(level, chunk, x, y0, z, x, y1, z, post ? p.pillar(Direction.Axis.Y) : p.wall.defaultBlockState());
			}
		}
	}

	/** A bronze censer on three legs at (x, y, z) (y its foot), incense smoking in its bowl. */
	private static void censer(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int y, int z) {
		BlockState bronze = Blocks.WAXED_CUT_COPPER.defaultBlockState();
		for (int[] leg : new int[][] {{-1, -1}, {1, -1}, {0, 1}}) g.put(level, chunk, x + leg[0], y, z + leg[1], Blocks.WAXED_CUT_COPPER_SLAB);
		g.put(level, chunk, x, y, z, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
		g.put(level, chunk, x - 1, y + 1, z, Blocks.WAXED_CUT_COPPER_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
		g.put(level, chunk, x + 1, y + 1, z, Blocks.WAXED_CUT_COPPER_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
		g.put(level, chunk, x, y + 1, z - 1, Blocks.WAXED_CUT_COPPER_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
		g.put(level, chunk, x, y + 1, z + 1, Blocks.WAXED_CUT_COPPER_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
		for (int[] c : new int[][] {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) g.put(level, chunk, x + c[0], y + 1, z + c[1], bronze);
	}

	// --- The lotus moat ---

	/**
	 * The moat across the grounds, 11 deep, as wide as the grounds are there ({@code A}, {@code B}, {@code Seed}: the grounds' own
	 * outline, SectLayout#outline; {@code V0}: its first row in the grounds' frame): still water two deep in a stone channel, its
	 * banks edged and balustraded, lily pads on it; where the Spirit Way crosses, an arched bridge of white jade nine wide, stairs
	 * up and down its rise, a balustrade along each side.
	 */
	static void moat(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		CompoundTag e = g.extra();
		int a = e.getInt("A"), b = e.getInt("B"), v0 = e.getInt("V0");
		long seed = e.getLong("Seed");
		int w = g.width(), d = g.depth(), cx = w / 2;
		BlockState water = Blocks.WATER.defaultBlockState();
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				double out = SectLayout.outline(a, b, seed, x, v0 + z);
				if (out > 0.97) continue; // past the grounds' edge
				boolean bridge = Math.abs(x - cx) <= 4;
				boolean bank = z == 0 || z == d - 1 || out > 0.93;
				if (bank) {
					g.fill(level, chunk, x, -3, z, x, 0, z, p.foundation);
					if (!bridge) g.put(level, chunk, x, 1, z, p.baseWall);
					continue;
				}
				g.put(level, chunk, x, -3, z, p.foundation);
				g.put(level, chunk, x, -2, z, water);
				g.put(level, chunk, x, -1, z, water);
				g.air(level, chunk, x, 0, z, x, 5, z);
				if (!bridge && g.rand(x, z, 31).nextInt(7) == 0) g.put(level, chunk, x, 0, z, Blocks.LILY_PAD);
			}
		}
		// The bridge: its deck rising in an arch to two blocks over the water, stairs where it climbs and falls.
		int[] rise = new int[d];
		for (int z = 0; z < d; z++) rise[z] = (int) Math.round(2.4 * Math.sin(Math.PI * z / (d - 1)));
		for (int z = 0; z < d; z++) {
			int h = rise[z];
			for (int x = cx - 4; x <= cx + 4; x++) {
				BlockState deck = p.wall.defaultBlockState();
				if (z > 0 && h > rise[z - 1]) deck = p.baseStair(Direction.NORTH);
				else if (z < d - 1 && h > rise[z + 1]) deck = p.baseStair(Direction.SOUTH);
				g.put(level, chunk, x, h, z, deck);
				if (h > 0 && (z == 1 || z == d - 2)) g.fill(level, chunk, x, 0, z, x, h - 1, z, p.foundation); // its abutments
				g.air(level, chunk, x, h + 1, z, x, h + 4, z);
				if (Math.abs(x - cx) == 4) g.put(level, chunk, x, h + 1, z, p.baseWall);
			}
		}
	}

	// --- A bell or drum tower ---

	/**
	 * A bell tower (or, with {@code Drum}, a drum tower), 13 across and 15 deep: a podium four high faced in stone, steps up its
	 * front, a balustrade round it; on it an open pavilion of pillars under a roof, a short upper storey under the crowning roof; in
	 * the pavilion the bronze bell hanging from the ceiling, or the great drum on its stand.
	 */
	static void tower(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = g.width(), d = g.depth(), cx = w / 2;
		boolean drum = g.extra().getBoolean("Drum");
		g.air(level, chunk, 0, 1, 0, w - 1, 26, d - 1);
		int pz0 = 4, top = 4;
		for (int x = 1; x <= w - 2; x++) {
			for (int z = pz0; z <= d - 1; z++) {
				g.fill(level, chunk, x, 1, z, x, top - 1, z, p.foundation);
				g.foundation(level, chunk, x, 1, z, p.foundation.defaultBlockState());
				g.put(level, chunk, x, top, z, p.wall);
				boolean rim = x == 1 || x == w - 2 || z == pz0 || z == d - 1;
				if (rim && !(z == pz0 && Math.abs(x - cx) <= 1)) g.put(level, chunk, x, top + 1, z, p.baseWall);
			}
		}
		stairs(g, level, chunk, p, cx - 1, cx + 1, pz0, 0, top, false);
		// The pavilion: pillars at its corners and the middles of its sides, open between over a low rail; beams and a ceiling.
		int x0 = 3, x1 = w - 4, z0 = pz0 + 2, z1 = d - 3, roofY = top + 7;
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean edgeX = x == x0 || x == x1, edgeZ = z == z0 || z == z1;
				if (!edgeX && !edgeZ) continue;
				boolean post = (x - x0) % 3 == 0 && (z - z0) % 3 == 0;
				if (post) g.pillar(level, chunk, p, x, top + 1, roofY - 1, z);
				else {
					if (z != z0) g.put(level, chunk, x, top + 1, z, p.baseWall); // open at the front, where the stairs come up
					g.put(level, chunk, x, roofY - 1, z, p.beam(edgeZ ? Direction.Axis.X : Direction.Axis.Z));
				}
			}
		}
		g.fill(level, chunk, x0 + 1, roofY - 1, z0 + 1, x1 - 1, roofY - 1, z1 - 1, p.floor);
		g.roof(level, chunk, p, x0 - 2, z0 - 2, x1 + 2, z1 + 2, roofY, 3, false, true);
		g.closeEaves(level, chunk, p, x0, z0, x1, z1, roofY - 1, x0 - 2, z0 - 2, x1 + 2, z1 + 2, roofY, 3);
		// The upper storey and the crowning roof, a finial of gold.
		int ux0 = x0 + 1, ux1 = x1 - 1, uz0 = z0 + 1, uz1 = z1 - 1;
		for (int x = ux0; x <= ux1; x++) {
			for (int z = uz0; z <= uz1; z++) {
				boolean edgeX = x == ux0 || x == ux1, edgeZ = z == uz0 || z == uz1;
				if (!edgeX && !edgeZ) continue;
				boolean corner = edgeX && edgeZ, middle = x == cx || z == (uz0 + uz1) / 2;
				g.fill(level, chunk, x, roofY, z, x, roofY + 2, z, corner ? p.pillar(Direction.Axis.Y) : p.wall.defaultBlockState());
				if (middle && !corner) g.put(level, chunk, x, roofY + 1, z, p.window);
			}
		}
		int crown = SectPiece.ridgeCourses(ux0 - 2, uz0 - 2, ux1 + 2, uz1 + 2);
		g.roof(level, chunk, p, ux0 - 2, uz0 - 2, ux1 + 2, uz1 + 2, roofY + 3, crown, true, true);
		g.closeEaves(level, chunk, p, ux0, uz0, ux1, uz1, roofY + 2, ux0 - 2, uz0 - 2, ux1 + 2, uz1 + 2, roofY + 3, crown);
		g.put(level, chunk, cx, roofY + 3 + crown, (uz0 + uz1) / 2, p.accent);
		// The bell, or the drum.
		int mz = (z0 + z1) / 2;
		if (drum) {
			for (int x = cx - 1; x <= cx + 1; x++) {
				for (int y = top + 2; y <= top + 4; y++) {
					g.put(level, chunk, x, y, mz, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.SOUTH));
				}
			}
			for (int x : new int[] {cx - 1, cx + 1}) g.put(level, chunk, x, top + 1, mz, p.window);
		} else {
			g.put(level, chunk, cx, roofY - 2, mz, Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING)
					.setValue(BellBlock.FACING, Direction.NORTH));
		}
	}

	// --- The alchemy hall ---

	/**
	 * The alchemy hall, 27 across and 21 deep: a hall of two eaves on a low plinth; inside, three pill furnaces of bronze along the
	 * back, each a fire under a cauldron in its ring of bronze; brewing tables, herbs drying and potted down the sides, an elder's
	 * chests of pills; before it a great censer smoking.
	 */
	static void alchemy(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = g.width(), d = g.depth(), cx = w / 2;
		g.air(level, chunk, 0, 1, 0, w - 1, 22, d - 1);
		int x0 = 2, z0 = 7, x1 = w - 3, z1 = d - 3, floor = 2, top = floor + 7;
		g.plinth(level, chunk, p, x0 - 1, z0, x1 + 1, z1 + 1, floor, 5);
		g.interior(level, chunk, p, x0 + 1, z0 + 1, x1 - 1, z1 - 1, floor, top + 6);
		g.walls(level, chunk, p, x0, z0, x1, z1, floor + 1, top - 1, 4, 3, 4, false);
		g.fill(level, chunk, x0 + 1, top, z0 + 1, x1 - 1, top, z1 - 1, p.floor);
		g.roof(level, chunk, p, x0 - 2, z0 - 2, x1 + 2, z1 + 2, top + 1, 3, false, true);
		g.closeEaves(level, chunk, p, x0, z0, x1, z1, top, x0 - 2, z0 - 2, x1 + 2, z1 + 2, top + 1, 3);
		clerestory(g, level, chunk, p, x0 + 1, z0 + 1, x1 - 1, z1 - 1, top + 1, top + 2);
		int upper = SectPiece.ridgeCourses(x0 - 1, z0 - 1, x1 + 1, z1 + 1);
		g.roof(level, chunk, p, x0 - 1, z0 - 1, x1 + 1, z1 + 1, top + 3, upper, true, true);
		g.closeEaves(level, chunk, p, x0 + 1, z0 + 1, x1 - 1, z1 - 1, top + 2, x0 - 1, z0 - 1, x1 + 1, z1 + 1, top + 3, upper);
		// The furnaces.
		for (int fx : new int[] {cx - 7, cx, cx + 7}) {
			int fz = z1 - 3, y = floor + 1;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					boolean middle = dx == 0 && dz == 0;
					g.put(level, chunk, fx + dx, y, fz + dz, middle ? Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)
							: p.foundation.defaultBlockState());
					g.put(level, chunk, fx + dx, y + 1, fz + dz, middle ? Blocks.AIR.defaultBlockState() : Blocks.WAXED_CUT_COPPER.defaultBlockState());
				}
			}
			g.put(level, chunk, fx, y + 2, fz, Blocks.CAULDRON);
			for (int[] c : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) g.put(level, chunk, fx + c[0], y + 2, fz + c[1], Blocks.WAXED_CUT_COPPER_SLAB);
		}
		// Down the sides: brewing tables on stone counters, herbs, the chests.
		for (int z = z0 + 2; z <= z1 - 6; z += 2) {
			for (int x : new int[] {x0 + 1, x1 - 1}) {
				g.put(level, chunk, x, floor + 1, z, p.foundation);
				g.put(level, chunk, x, floor + 2, z, (z / 2) % 2 == 0 ? Blocks.BREWING_STAND : Blocks.POTTED_AZURE_BLUET);
			}
		}
		g.chest(level, chunk, x0 + 1, floor + 1, z1 - 1, Direction.EAST, SectPiece.ELDER_LOOT);
		g.chest(level, chunk, x1 - 1, floor + 1, z1 - 1, Direction.WEST, SectPiece.ELDER_LOOT);
		for (int x = x0 + 3; x <= x1 - 3; x += 5) g.lanternHanging(level, chunk, p, x, top - 1, z0 + 3);
		g.put(level, chunk, cx, floor + 1, z0 + 2, Blocks.HAY_BLOCK); // herbs drying by the door
		// The great censer before it.
		g.bigBrazier(level, chunk, p, cx, 1, 3);
	}

	// --- An elder's court ---

	/**
	 * An elder's court, 23 by 23: its wall all round (a stone foot, plaster, a coping of tiles, pillars at the corners), its front wall
	 * a row in from the front so the roofed gateway in its middle sits square over it, its ridge over the wall and its eaves out over
	 * either side, with a plaque of gold; stone lanterns inside the gate, a blossom tree in each back corner and a pond in a front one.
	 * The elder's house stands in its middle, a piece of its own.
	 */
	static void court(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = g.width(), d = g.depth(), cx = w / 2, front = 1;
		for (int x = 0; x < w; x++) {
			for (int z = front; z < d; z++) {
				boolean edgeX = x == 0 || x == w - 1, edgeZ = z == front || z == d - 1;
				if (!edgeX && !edgeZ) continue;
				g.put(level, chunk, x, 0, z, p.foundation);
				g.foundation(level, chunk, x, 0, z, p.foundation.defaultBlockState());
				if (z == front && Math.abs(x - cx) <= 1) {
					g.air(level, chunk, x, 1, z, x, 4, z);
					continue;
				}
				if (edgeX && edgeZ) {
					g.pillar(level, chunk, p, x, 1, 4, z);
					g.put(level, chunk, x, 5, z, p.trimSlab(false));
					continue;
				}
				g.put(level, chunk, x, 1, z, p.wallBase);
				g.fill(level, chunk, x, 2, z, x, 3, z, p.wall);
				g.put(level, chunk, x, 4, z, p.roofSlab(false));
			}
		}
		// The gateway: its posts, its roof square over the wall, the plaque over the way through.
		for (int x : new int[] {cx - 2, cx + 2}) g.pillar(level, chunk, p, x, 1, 4, front);
		g.roof(level, chunk, p, cx - 4, front - 1, cx + 4, front + 1, 5, SectPiece.ridgeCourses(cx - 4, front - 1, cx + 4, front + 1), true, true);
		g.put(level, chunk, cx, 4, front, p.accent);
		for (int x : new int[] {cx - 3, cx + 3}) g.stoneLantern(level, chunk, p, x, 1, front + 2);
		g.tree(level, chunk, p, 3, d - 4, g.rand(3, d - 4, 5));
		g.tree(level, chunk, p, w - 4, d - 4, g.rand(w - 4, d - 4, 5));
		// The pond.
		for (int x = 2; x <= 4; x++) {
			for (int z = front + 2; z <= front + 3; z++) {
				g.put(level, chunk, x, -1, z, Blocks.CLAY);
				g.put(level, chunk, x, 0, z, Blocks.WATER.defaultBlockState());
				g.air(level, chunk, x, 1, z, x, 2, z);
			}
		}
		g.put(level, chunk, 3, 1, front + 2, Blocks.LILY_PAD);
	}

	// --- A guardian beast ---

	/**
	 * A guardian lion of gold, 7 by 7 and ten high, on a plinth of white jade (a broad step, then a block with its corners carved,
	 * then its top) looking out over the way before it (its front): sitting up on haunches wider than its chest, its rear paws drawn
	 * forward, its forelegs planted, the right on an embroidered ball; its great head (as a guardian lion's is, as wide as its haunches)
	 * in a curled mane of raw gold down its neck and over its crown, dark eyes deep-set either side of its nose, its mouth open in a
	 * roar under it; its tail a curl of the mane's gold behind. All gold, as the gilded lions before a palace's gates are: the qilin of
	 * the trim's blocks it was before, a size too small, read as a heap (tour 6, 2026-10-25).
	 */
	static void statue(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		g.air(level, chunk, 0, 1, 0, 6, 10, 6);
		for (int x = 0; x <= 6; x++) {
			for (int z = 0; z <= 6; z++) {
				g.put(level, chunk, x, 1, z, p.foundation);
				g.foundation(level, chunk, x, 1, z, p.foundation.defaultBlockState());
			}
		}
		for (int x = 1; x <= 5; x++) {
			for (int z = 1; z <= 5; z++) {
				boolean corner = (x == 1 || x == 5) && (z == 1 || z == 5);
				g.put(level, chunk, x, 2, z, corner ? Blocks.CHISELED_QUARTZ_BLOCK : p.wall);
				g.put(level, chunk, x, 3, z, p.foundation);
			}
		}
		BlockState gold = p.accent.defaultBlockState(), mane = Blocks.RAW_GOLD_BLOCK.defaultBlockState(), dark = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
		// Its body: the chest and back, the haunches wider behind, the rear paws drawn forward.
		g.fill(level, chunk, 2, 4, 2, 4, 5, 4, gold);
		g.fill(level, chunk, 2, 6, 2, 4, 6, 5, gold);
		g.fill(level, chunk, 1, 4, 4, 5, 5, 5, gold);
		for (int x : new int[] {1, 5}) g.put(level, chunk, x, 4, 3, gold);
		// Its forelegs, the right (to the west, as it looks out) on the ball.
		g.put(level, chunk, 2, 4, 1, mane);
		g.fill(level, chunk, 2, 5, 1, 2, 6, 1, gold);
		g.fill(level, chunk, 4, 4, 1, 4, 6, 1, gold);
		// Its head: gold within, its face at the front.
		g.fill(level, chunk, 2, 7, 1, 4, 9, 3, gold);
		g.put(level, chunk, 2, 8, 1, dark); // its eyes
		g.put(level, chunk, 4, 8, 1, dark);
		g.put(level, chunk, 3, 8, 0, gold); // its nose
		g.put(level, chunk, 2, 7, 0, gold); // its jaws, open
		g.put(level, chunk, 3, 7, 0, dark);
		g.put(level, chunk, 4, 7, 0, gold);
		// Its mane: down the sides of its head and neck, behind its head, over its crown; and its tail.
		for (int x : new int[] {1, 5}) g.fill(level, chunk, x, 6, 1, x, 9, 3, mane);
		g.fill(level, chunk, 2, 7, 4, 4, 9, 4, mane);
		g.fill(level, chunk, 2, 10, 1, 4, 10, 3, mane);
		g.fill(level, chunk, 3, 7, 5, 3, 8, 5, mane);
	}

	private HolyLandPieces() {}
}
