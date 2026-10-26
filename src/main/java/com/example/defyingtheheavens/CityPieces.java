package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * An immortal city's own buildings (ImmortalCapital, phase 2 of the realm plan, 2026-10-25), drawn in a SectPiece's frame as every sect
 * building is (x across its front, z from its front inward, y from the city's paving, 0 its own level):
 * <ul>
 *   <li><b>A stretch of the city wall</b> ({@link #wall}): white jade, a band of gold along its face, a walk along its top between
 *   crenellated parapets; it stands only on the city's level ground (where the Celestial Court's pinnacle rises, the pinnacle is the
 *   wall).</li>
 *   <li><b>A gate tower</b> ({@link #gate}): a broad block of the wall pierced by an arched gateway, a plaque of gold over it front and
 *   back, and on it a two-eaved pavilion of the city's colours.</li>
 *   <li><b>A corner bastion</b> ({@link #tower}): a square tower over the walls' meeting, a pavilion of one eave on it.</li>
 *   <li><b>A shop</b> ({@link #shop}): two storeys on the street, its front open over a counter under an awning, its trade by its seed
 *   (herbs, smithing, silks, tea), its upper storey windowed, a hipped roof.</li>
 *   <li><b>A market stall</b> ({@link #stall}): four posts under a cloth canopy, a counter with its wares.</li>
 *   <li><b>The Celestial Court's stair</b> ({@link #courtStair}): twin flights climbing the Court's pinnacle's south face from either
 *   side to a landing over the city, a bridge on from it to the pinnacle's top, all on arcades of white jade down to the ground.</li>
 * </ul>
 */
final class CityPieces {
	/** A wall's height over the paving, and its walk's (the parapet's foot). */
	static final int WALL_HEIGHT = 14, WALK = WALL_HEIGHT - 2;
	/** A gate tower's gateway: this wide either side of its middle, this high. */
	static final int ARCH_HALF = 3, ARCH_HEIGHT = 9;

	static void draw(SectPiece piece, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		switch (piece.kind()) {
			case CITY_WALL -> wall(piece, level, chunk, p);
			case CITY_GATE -> gate(piece, level, chunk, p);
			case CITY_TOWER -> tower(piece, level, chunk, p);
			case CITY_SHOP -> shop(piece, level, chunk, p);
			case CITY_STALL -> stall(piece, level, chunk, p);
			case COURT_STAIR -> courtStair(piece, level, chunk, p);
			case CITY_GROUNDS -> grounds(piece, level, chunk, p);
			default -> { }
		}
	}

	// --- The grounds ---

	/**
	 * A city's grounds (an immortal city's, ImmortalCityStructure): its ways, footpaths, lanterns and trees, from its ground plan ("Map", in
	 * the piece's frame), laid on the ground where it lies as a village's roads are; the grass between left as the land has it. The piece is
	 * the structure's first, so each column's top is still the ground's (its box lies at the world's floor: the land is shaped round the
	 * buildings, never levelled under the whole of it).
	 */
	static void grounds(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = g.width(), d = g.depth();
		byte[] map = g.extra().getByteArray("Map");
		if (map.length != w * d) return;
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				byte code = map[x + z * w];
				if (code == SectLayout.OUT || code == SectLayout.GRASS) continue;
				BlockPos at = g.toWorld(x, 0, z);
				if (!draws(chunk, code, at.getX(), at.getZ())) continue;
				int top = groundTop(level, at.getX(), at.getZ());
				if (top <= level.getMinBuildHeight() + 2) continue; // nothing under it to lay on
				lay(level, chunk, p, Alignment.Faction.NEUTRAL, code, at.getX(), top, at.getZ());
			}
		}
	}

	/** How far a tree's crown spreads from its trunk (SectPiece#tree): trunks this far beyond a chunk draw their crowns' share in it. */
	static final int CROWN = 3;

	/** Whether a column lies in {@code chunk}, or (a tree's, {@code code}) within its crown's spread of it. */
	static boolean draws(BoundingBox chunk, byte code, int x, int z) {
		int reach = code == SectLayout.TREE ? CROWN : 0;
		return x >= chunk.minX() - reach && x <= chunk.maxX() + reach && z >= chunk.minZ() - reach && z <= chunk.maxZ() + reach;
	}

	/**
	 * The land's top block in column (x, z): the highest that is not a tree's (its leaves or trunk) nor a plant (a column just beyond the
	 * chunk may hold its own tree already, drawn when its own chunk was).
	 */
	static int groundTop(WorldGenLevel level, int x, int z) {
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
		while (at.getY() > level.getMinBuildHeight()) {
			BlockState here = level.getBlockState(at);
			if (!(here.isAir() || here.is(net.minecraft.tags.BlockTags.LEAVES) || here.is(net.minecraft.tags.BlockTags.LOGS)
					|| (here.canBeReplaced() && here.getFluidState().isEmpty()))) break;
			at.move(Direction.DOWN);
		}
		return at.getY();
	}

	/** Lays a ground plan's {@code code} (SectLayout's) on the ground's top block at (x, top, z). */
	static void lay(WorldGenLevel level, BoundingBox chunk, SectPalette p, Alignment.Faction faction, byte code, int x, int top, int z) {
		BlockPos at = new BlockPos(x, top, z);
		switch (code) {
			case SectLayout.PATH -> pave(level, at, p.path.defaultBlockState());
			case SectLayout.PATH_EDGE, SectLayout.PLAZA -> pave(level, at, p.foundation.defaultBlockState());
			case SectLayout.FOOTPATH -> {
				int roll = (int) Math.floorMod(net.minecraft.util.Mth.getSeed(x, 0, z) >> 20, 10L);
				pave(level, at, roll < 7 ? Blocks.DIRT_PATH.defaultBlockState() : roll < 9 ? Blocks.COARSE_DIRT.defaultBlockState() : p.paving.defaultBlockState());
			}
			default -> {
				// What stands on the ground, drawn as a sect's grounds draw it (SectPiece#terrace), by a piece of one column set on it.
				SectPiece spot = new SectPiece(SectPiece.Kind.TERRACE, new BoundingBox(x, top, z, x, top + 1, z), Direction.NORTH,
						net.minecraft.util.Mth.getSeed(x, top, z), faction, true, null);
				switch (code) {
					case SectLayout.LANTERN -> spot.stoneLantern(level, chunk, p, 0, 1, 0);
					case SectLayout.TREE -> spot.tree(level, chunk, p, 0, 0, spot.rand(0, 0, 7));
					case SectLayout.BAMBOO -> spot.bamboo(level, chunk, 0, 0, spot.rand(0, 0, 9));
					default -> { }
				}
			}
		}
	}

	/** Lays a way's block over the ground's top at {@code at}, and takes away what grows over it. */
	private static void pave(WorldGenLevel level, BlockPos at, BlockState state) {
		level.setBlock(at, state, Block.UPDATE_CLIENTS);
		for (int dy = 1; dy <= 3; dy++) {
			BlockPos up = at.above(dy);
			BlockState here = level.getBlockState(up);
			if (!here.isAir() && here.canBeReplaced()) level.setBlock(up, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** Whether the land at a column of {@code g}'s frame stands high over the city's level (the Court's pinnacle): nothing of a wall there. */
	private static boolean underHeights(SectPiece g, WorldGenLevel level, BoundingBox chunk, int x, int z) {
		BlockPos at = g.toWorld(x, 0, z);
		if (!chunk.isInside(new BlockPos(at.getX(), chunk.minY(), at.getZ()))) return false;
		return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, at.getX(), at.getZ()) > g.getBoundingBox().minY() + WALL_HEIGHT + 4;
	}

	/** A column of solid {@code state} from {@code y} down to the ground (however far: an arcade under a stair). */
	private static void downTo(SectPiece g, WorldGenLevel level, BoundingBox chunk, int x, int y, int z, BlockState state) {
		BlockPos.MutableBlockPos pos = g.toWorld(x, y, z).mutable();
		if (!chunk.isInside(pos)) return;
		for (int i = 0; i < 200 && pos.getY() > level.getMinBuildHeight(); i++) {
			BlockState here = level.getBlockState(pos);
			if (!(here.isAir() || here.canBeReplaced() || !here.getFluidState().isEmpty())) break;
			level.setBlock(pos, state, Block.UPDATE_CLIENTS);
			pos.move(Direction.DOWN);
		}
	}

	// --- The walls ---

	/**
	 * A stretch of wall along x, its outer face at z 0 (the frame's front faces out of the city): footings, the wall in white jade on a
	 * course of bricks, a band of the accent along both faces, the walk paved along its top, crenellated parapets either side.
	 */
	static void wall(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = g.width(), d = g.depth();
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				if (underHeights(g, level, chunk, x, z)) continue;
				boolean face = z == 0 || z == d - 1;
				g.put(level, chunk, x, 0, z, p.foundation);
				g.foundation(level, chunk, x, 0, z, p.foundation.defaultBlockState());
				for (int y = 1; y <= WALK; y++) {
					BlockState block = y <= 2 ? p.wallBase.defaultBlockState() : face && y == WALK - 2 ? p.accent.defaultBlockState() : p.wall.defaultBlockState();
					g.put(level, chunk, x, y, z, block);
				}
				if (face) {
					// The parapet, crenellated: merlons on the even blocks, a slab between.
					g.put(level, chunk, x, WALK + 1, z, p.wallBase);
					if (x % 2 == 0) g.put(level, chunk, x, WALK + 2, z, p.wallBase);
				} else {
					g.put(level, chunk, x, WALK, z, p.paving);
				}
				g.air(level, chunk, x, WALK + 3, z, x, WALK + 6, z);
			}
			if (x % 24 == 12 && !underHeights(g, level, chunk, x, 1)) g.stoneLantern(level, chunk, p, x, WALK + 1, d / 2);
		}
	}

	/**
	 * A gate tower, its outer face at z 0: the wall's block made broader and higher, an arched gateway through it ({@link #ARCH_HALF} either
	 * side of its middle, {@link #ARCH_HEIGHT} high) with a plaque of the accent over it on both faces and lanterns hung inside, the walk
	 * across its top, and on it a two-eaved pavilion: pillars, windows, a lower roof round it, an upper roof over it.
	 */
	static void gate(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = g.width(), d = g.depth(), cx = w / 2;
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				g.put(level, chunk, x, 0, z, p.foundation);
				g.foundation(level, chunk, x, 0, z, p.foundation.defaultBlockState());
				boolean face = z == 0 || z == d - 1 || x == 0 || x == w - 1;
				for (int y = 1; y <= WALL_HEIGHT; y++) {
					BlockState block = y <= 2 ? p.wallBase.defaultBlockState() : face && y == WALK - 2 ? p.accent.defaultBlockState() : p.wall.defaultBlockState();
					g.put(level, chunk, x, y, z, block);
				}
				if (face) {
					g.put(level, chunk, x, WALL_HEIGHT + 1, z, p.wallBase);
					if ((x + z) % 2 == 0) g.put(level, chunk, x, WALL_HEIGHT + 2, z, p.wallBase);
				}
			}
		}
		// The gateway, its arch rounded by stairs turned under at its top corners.
		g.air(level, chunk, cx - ARCH_HALF, 1, 0, cx + ARCH_HALF, ARCH_HEIGHT, d - 1);
		for (int z = 0; z < d; z++) {
			g.put(level, chunk, cx - ARCH_HALF, ARCH_HEIGHT, z, p.baseStair(Direction.WEST).setValue(StairBlock.HALF, Half.TOP));
			g.put(level, chunk, cx + ARCH_HALF, ARCH_HEIGHT, z, p.baseStair(Direction.EAST).setValue(StairBlock.HALF, Half.TOP));
			g.put(level, chunk, cx, 0, z, p.path);
		}
		for (int z : new int[] {0, d - 1}) {
			g.fill(level, chunk, cx - 3, ARCH_HEIGHT + 2, z, cx + 3, ARCH_HEIGHT + 3, z, p.accent);
			g.lanternHanging(level, chunk, p, cx - 2, ARCH_HEIGHT - 1, z == 0 ? 1 : d - 2);
			g.lanternHanging(level, chunk, p, cx + 2, ARCH_HEIGHT - 1, z == 0 ? 1 : d - 2);
		}
		// The pavilion on top.
		int base = WALL_HEIGHT + 1, x0 = 4, x1 = w - 5, z0 = 2, z1 = d - 3;
		g.fill(level, chunk, x0, base - 1, z0, x1, base - 1, z1, p.floor);
		g.walls(level, chunk, p, x0, z0, x1, z1, base, base + 5, 4, 3, 4, false);
		for (int x = x0 + 2; x <= x1 - 2; x += 4) {
			g.put(level, chunk, x, base + 3, z0, p.window);
			g.put(level, chunk, x, base + 3, z1, p.window);
		}
		g.air(level, chunk, x0 + 1, base, z0 + 1, x1 - 1, base + 6, z1 - 1);
		g.roof(level, chunk, p, x0 - 2, z0 - 2, x1 + 2, z1 + 2, base + 7, 2, false, true);
		g.fill(level, chunk, x0, base + 8, z0, x1, base + 10, z1, p.wall);
		g.roof(level, chunk, p, x0 - 1, z0 - 1, x1 + 1, z1 + 1, base + 11, SectPiece.ridgeCourses(x0 - 1, z0 - 1, x1 + 1, z1 + 1), true, true);
		g.wallBanner(level, chunk, p, cx - 6, base + 2, z0 - 1, Direction.SOUTH);
		g.wallBanner(level, chunk, p, cx + 6, base + 2, z0 - 1, Direction.SOUTH);
	}

	/** A corner bastion: the walls' meeting raised and broadened, crenellated, a small pavilion of one eave on it. */
	static void tower(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = g.width(), d = g.depth(), top = WALL_HEIGHT + 4;
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				if (underHeights(g, level, chunk, x, z)) continue;
				boolean face = z == 0 || z == d - 1 || x == 0 || x == w - 1;
				g.put(level, chunk, x, 0, z, p.foundation);
				g.foundation(level, chunk, x, 0, z, p.foundation.defaultBlockState());
				for (int y = 1; y <= top; y++) {
					BlockState block = y <= 2 ? p.wallBase.defaultBlockState() : face && (y == WALK - 2 || y == top - 2) ? p.accent.defaultBlockState()
							: p.wall.defaultBlockState();
					g.put(level, chunk, x, y, z, block);
				}
				if (face) {
					g.put(level, chunk, x, top + 1, z, p.wallBase);
					if ((x + z) % 2 == 0) g.put(level, chunk, x, top + 2, z, p.wallBase);
				}
			}
		}
		int x0 = 3, x1 = w - 4, z0 = 3, z1 = d - 4;
		for (int[] c : new int[][] {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) g.pillar(level, chunk, p, c[0], top + 1, top + 4, c[1]);
		g.roof(level, chunk, p, x0 - 2, z0 - 2, x1 + 2, z1 + 2, top + 5, SectPiece.ridgeCourses(x0 - 2, z0 - 2, x1 + 2, z1 + 2), true, true);
		g.lanternHanging(level, chunk, p, (x0 + x1) / 2, top + 4, (z0 + z1) / 2);
	}

	// --- Trade ---

	/** The four trades a shop may keep, by its seed: herbs, smithing, silks, tea. */
	private enum Trade { HERBS, SMITH, SILKS, TEA }

	/**
	 * A shop of two storeys, its front (z 0) on the street: its ground floor open behind a counter under an awning, its trade's fittings
	 * within; its upper storey walled and windowed on a beam; a hipped roof over all.
	 */
	static void shop(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		// Its walls a block in from its plot's sides (the roof's eaves and the awning reach out over that block), its front at z 1.
		int w = g.width(), d = g.depth(), x0 = 1, x1 = w - 2, z0 = 1, z1 = d - 1, mid = (x0 + x1) / 2;
		RandomSource random = g.rand(0, 0, 17);
		Trade trade = Trade.values()[random.nextInt(Trade.values().length)];
		g.air(level, chunk, 0, 1, 0, w - 1, 14, z1);
		for (int x = 0; x < w; x++) {
			for (int z = 0; z <= z1; z++) {
				g.put(level, chunk, x, 0, z, z == 0 || x < x0 || x > x1 ? p.path : p.floor);
				g.foundation(level, chunk, x, 0, z, p.foundation.defaultBlockState());
			}
		}
		// The ground floor: walls at its sides and back, its front open between pillars, a counter across it but for its middle.
		g.walls(level, chunk, p, x0, z0, x1, z1, 1, 4, 4, 0, 0, false);
		g.air(level, chunk, x0 + 1, 1, z0, x1 - 1, 4, z1 - 1);
		for (int x : new int[] {x0, mid, x1}) g.pillar(level, chunk, p, x, 1, 4, z0);
		for (int x = x0 + 1; x < x1; x++) if (x != mid) g.put(level, chunk, x, 1, z0 + 1, p.baseSlab(true));
		// The awning over the counter, the trade's banner beside it.
		for (int x = 0; x < w; x++) g.put(level, chunk, x, 5, 0, p.trimStair(Direction.NORTH, false));
		g.wallBanner(level, chunk, p, x0, 3, z0 - 1, Direction.SOUTH);
		// The trade's fittings at the back.
		fittings(g, level, chunk, p, trade, x0, x1, z1);
		g.lanternHanging(level, chunk, p, mid, 4, (z0 + z1) / 2);
		// The upper storey: its floor on a beam, walled, windows front and back.
		g.fill(level, chunk, x0, 5, z0, x1, 5, z1, p.floor);
		g.walls(level, chunk, p, x0, z0, x1, z1, 6, 8, 4, 0, 0, false);
		for (int x = x0 + 2; x < x1; x += 3) {
			g.put(level, chunk, x, 7, z0, p.window);
			g.put(level, chunk, x, 7, z1, p.window);
		}
		g.air(level, chunk, x0 + 1, 6, z0 + 1, x1 - 1, 8, z1 - 1);
		g.roof(level, chunk, p, 0, 0, w - 1, z1, 9, SectPiece.ridgeCourses(0, 0, w - 1, z1), true, false);
	}

	/** What a shop of {@code trade} keeps along its back wall, inside its walls x0..x1. */
	private static void fittings(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p, Trade trade, int x0, int x1, int z1) {
		int back = z1 - 1, in0 = x0 + 1, in1 = x1 - 1;
		switch (trade) {
			case HERBS -> {
				for (int x = in0; x <= in1; x++) {
					g.put(level, chunk, x, 1, back, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
					g.put(level, chunk, x, 2, back, flowerPot(x));
				}
			}
			case SMITH -> {
				g.put(level, chunk, in0, 1, back, Blocks.BLAST_FURNACE);
				g.put(level, chunk, in0 + 1, 1, back, Blocks.SMITHING_TABLE);
				g.put(level, chunk, in1 - 1, 1, back, Blocks.ANVIL);
				g.put(level, chunk, in1, 1, back, Blocks.GRINDSTONE);
			}
			case SILKS -> {
				for (int x = in0; x <= in1; x++) g.put(level, chunk, x, 1, back, x % 2 == 0 ? Blocks.LOOM : Blocks.WHITE_WOOL);
				g.put(level, chunk, in0, 2, back, Blocks.LIGHT_BLUE_WOOL);
				g.put(level, chunk, in1, 2, back, Blocks.YELLOW_WOOL);
			}
			case TEA -> {
				for (int x = in0 + 1; x < in1; x += 3) {
					g.put(level, chunk, x, 1, back - 1, p.trimSlab(true));
					g.put(level, chunk, x, 2, back - 1, Blocks.FLOWER_POT);
					g.put(level, chunk, x - 1, 1, back - 1, p.baseStair(Direction.EAST));
				}
				g.put(level, chunk, in1, 1, back, Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false));
			}
		}
	}

	private static BlockState flowerPot(int x) {
		Block[] pots = {Blocks.POTTED_FERN, Blocks.POTTED_RED_MUSHROOM, Blocks.POTTED_BROWN_MUSHROOM, Blocks.POTTED_AZURE_BLUET, Blocks.POTTED_LILY_OF_THE_VALLEY};
		return pots[Math.floorMod(x, pots.length)].defaultBlockState();
	}

	/** A market stall: four posts, a canopy of cloth in one of the city's colours, a counter across its front with its wares. */
	static void stall(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = g.width(), d = g.depth(), x1 = w - 1, z1 = d - 1;
		RandomSource random = g.rand(0, 0, 23);
		Block[] cloths = {Blocks.WHITE_WOOL, Blocks.YELLOW_WOOL, Blocks.LIGHT_BLUE_WOOL, Blocks.CYAN_WOOL, Blocks.RED_WOOL};
		Block cloth = cloths[random.nextInt(cloths.length)];
		g.air(level, chunk, 0, 1, 0, x1, 5, z1);
		for (int[] c : new int[][] {{0, 0}, {x1, 0}, {0, z1}, {x1, z1}}) g.fill(level, chunk, c[0], 1, c[1], c[0], 3, c[1], p.window);
		g.fill(level, chunk, 0, 4, 0, x1, 4, z1, cloth);
		for (int x = 1; x < x1; x++) {
			g.put(level, chunk, x, 1, 1, p.baseSlab(true));
			if (random.nextInt(3) > 0) g.put(level, chunk, x, 2, 1, x % 2 == 0 ? Blocks.FLOWER_POT.defaultBlockState() : flowerPot(x + random.nextInt(5)));
		}
		g.put(level, chunk, x1 / 2, 1, z1 - 1, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
	}

	// --- The Court's stair ---

	/** Along each of the Court stair's flights, a pier this often (two wide), arched between. */
	private static final int PIER_SPACING = 10;

	/**
	 * The stair up to the Celestial Court ({@link ImmortalCapital}): twin flights climbing the pinnacle's south face across the frame (x),
	 * from either end (at {@code flightZ}..+8) to a landing in its middle {@code rise} up, and from the landing a bridge on into the frame
	 * ({@code rise} high) to its back, over the pinnacle's edge onto its top (the rock cut away over the deck). The flights ride on arcades
	 * of white jade (tour 4: filled solid to the ground, they stood before the pinnacle as one great white pyramid and hid it): each deck on
	 * piers every {@link #PIER_SPACING}, banded in gold under it, arched between, a lantern hung in each arch; the landing on its tower, the
	 * bridge on piers; balustrades either side, lanterns at the landing and along the bridge.
	 */
	static void courtStair(SectPiece g, WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = g.width(), d = g.depth(), cx = w / 2, rise = g.extra().getInt("Rise"), flightZ = 6, flightW = 9;
		BlockState fill = p.foundation.defaultBlockState(), balustrade = p.baseWall.defaultBlockState(), pier = p.wall.defaultBlockState(),
				band = p.accent.defaultBlockState();
		// The deck's width, a balustrade on either edge.
		int z0 = flightZ - 1, z1 = flightZ + flightW;
		// The flights: from x = cx - rise - 4 (west, at y 1) up eastward to the landing, and its mirror from the east.
		for (int i = 0; i < rise; i++) {
			int y = 1 + i;
			for (int s : new int[] {-1, 1}) {
				int x = cx + s * (rise + 4 - i);
				for (int z = z0; z <= z1; z++) {
					boolean edge = z == z0 || z == z1;
					g.put(level, chunk, x, y, z, edge ? fill : p.baseStair(s < 0 ? Direction.EAST : Direction.WEST));
					g.put(level, chunk, x, y - 1, z, fill);
					if (edge) g.put(level, chunk, x, y + 1, z, balustrade);
					else g.air(level, chunk, x, y + 1, z, x, y + 4, z);
				}
				// Under the deck: a pier down to the ground, or an arch, its spandrels stepping down toward the piers either side.
				int along = i % PIER_SPACING;
				if (along < 2) {
					for (int z = z0; z <= z1; z++) {
						downTo(g, level, chunk, x, y - 2, z, pier);
						if (y - 2 >= 2) g.put(level, chunk, x, y - 2, z, band);
					}
				} else {
					int hang = Math.max(0, 3 - Math.min(along - 2, PIER_SPACING - 1 - along));
					for (int z = z0; z <= z1; z++) {
						for (int dy = 2; dy < 2 + hang && y - dy >= 1; dy++) g.put(level, chunk, x, y - dy, z, fill);
					}
					if (along == PIER_SPACING / 2 && y - 2 >= 1) g.lanternHanging(level, chunk, p, x, y - 2, (z0 + z1) / 2);
				}
			}
		}
		// The landing between them, on its tower (gold-banded every twelve blocks down), the bridge going on from its north side.
		int top = rise;
		for (int x = cx - 4; x <= cx + 4; x++) {
			for (int z = z0; z < z1; z++) {
				g.put(level, chunk, x, top, z, z == z0 ? fill : p.paving.defaultBlockState());
				downTo(g, level, chunk, x, top - 1, z, pier);
				if (z != z0) g.air(level, chunk, x, top + 1, z, x, top + 6, z);
			}
			g.put(level, chunk, x, top + 1, z0, balustrade);
		}
		for (int yy = top - 3; yy >= 1; yy -= 12) {
			for (int x = cx - 4; x <= cx + 4; x++) {
				for (int z = z0; z < z1; z++) {
					boolean face = x == cx - 4 || x == cx + 4 || z == z0 || z == z1 - 1;
					if (face && g.at(level, chunk, x, yy, z).is(pier.getBlock())) g.put(level, chunk, x, yy, z, band);
				}
			}
		}
		g.stoneLantern(level, chunk, p, cx - 4, top + 1, flightZ);
		g.stoneLantern(level, chunk, p, cx + 4, top + 1, flightZ);
		// The bridge on to the pinnacle's top, the rock cut away over its deck.
		for (int z = flightZ + flightW; z < d; z++) {
			for (int x = cx - 4; x <= cx + 4; x++) {
				g.put(level, chunk, x, top, z, Math.abs(x - cx) <= 1 ? p.path.defaultBlockState() : p.paving.defaultBlockState());
				g.air(level, chunk, x, top + 1, z, x, top + 8, z);
			}
			for (int x : new int[] {cx - 5, cx + 5}) {
				g.put(level, chunk, x, top, z, fill);
				g.put(level, chunk, x, top + 1, z, balustrade);
			}
			// Its piers, every eight blocks, down to whatever is below.
			if ((z - flightZ) % 8 == 0) {
				for (int x = cx - 5; x <= cx + 5; x++) downTo(g, level, chunk, x, top - 1, z, fill);
				g.stoneLantern(level, chunk, p, cx - 5, top + 2, z);
				g.stoneLantern(level, chunk, p, cx + 5, top + 2, z);
			}
		}
	}

	private CityPieces() {}
}
