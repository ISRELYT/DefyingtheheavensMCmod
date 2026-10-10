package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.material.Fluids;

/**
 * One building of a sect (see {@link SectStructure}): the grounds themselves (paving, walls, lanterns), the gate, corner
 * towers, the Formation plaza at the heart of the sect, the meditation hall, the treasury, the main hall with the Sect
 * Master's dais, the pagoda, herb gardens and dwellings.
 * <p>
 * Every building is drawn in its own frame, through StructurePiece's orientation: x across its front, z from its front
 * inward, y from the platform's paving (0) up. The orientation (the direction the building extends into from its entrance)
 * rotates and mirrors everything, block states included, into the world. All the randomness comes from the piece's own
 * seed, never the chunk's, so a building split across chunks comes out whole.
 */
public class SectPiece extends StructurePiece {
	public enum Kind { TERRACE, GATE, TOWER, PLAZA, MEDITATION_HALL, TREASURY, MAIN_HALL, PAGODA, GARDEN, DWELLING, ELDER_HOUSE }

	public static final ResourceLocation TREASURY_LOOT = DefyingTheHeavens.id("chests/sect_treasury");
	public static final ResourceLocation DWELLING_LOOT = DefyingTheHeavens.id("chests/sect_dwelling");
	public static final ResourceLocation ELDER_LOOT = DefyingTheHeavens.id("chests/sect_elder");
	public static final ResourceLocation LIBRARY_LOOT = DefyingTheHeavens.id("chests/sect_library");

	/** How far a building's base may be filled down to meet the ground before it is left to stand on its own. */
	private static final int MAX_FOUNDATION = 12;

	private final Kind kind;
	private final long seed;
	private final Alignment.Faction faction;
	private final boolean upper;
	/** Per-kind data: the plaza's sect blueprint, the size of a hall, a garden's pond. */
	private final CompoundTag extra;

	public SectPiece(Kind kind, BoundingBox box, Direction orientation, long seed, Alignment.Faction faction, boolean upper, CompoundTag extra) {
		super(ModStructures.SECT_PIECE, 0, box);
		this.kind = kind;
		this.seed = seed;
		this.faction = faction;
		this.upper = upper;
		this.extra = extra == null ? new CompoundTag() : extra;
		setOrientation(orientation);
	}

	public SectPiece(CompoundTag tag) {
		super(ModStructures.SECT_PIECE, tag);
		Kind k;
		try {
			k = Kind.valueOf(tag.getString("Kind"));
		} catch (IllegalArgumentException e) {
			k = Kind.TERRACE;
		}
		this.kind = k;
		this.seed = tag.getLong("Seed");
		this.faction = Alignment.Faction.values()[Math.max(0, Math.min(Alignment.Faction.values().length - 1, tag.getInt("Faction")))];
		this.upper = tag.getBoolean("Upper");
		this.extra = tag.getCompound("Extra");
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putString("Kind", kind.name());
		tag.putLong("Seed", seed);
		tag.putInt("Faction", faction.ordinal());
		tag.putBoolean("Upper", upper);
		tag.put("Extra", extra);
	}

	public Kind kind() { return kind; }
	public CompoundTag extra() { return extra; }

	/** Width across the front, in the piece's own frame. */
	public int width() {
		Direction o = getOrientation();
		return o == null || o.getAxis() == Direction.Axis.Z ? boundingBox.getXSpan() : boundingBox.getZSpan();
	}

	/** Depth from the front inward. */
	public int depth() {
		Direction o = getOrientation();
		return o == null || o.getAxis() == Direction.Axis.Z ? boundingBox.getZSpan() : boundingBox.getXSpan();
	}

	/** Where a point of this piece's frame lies in the world (for the sect's posts). */
	public BlockPos toWorld(int x, int y, int z) {
		return getWorldPos(x, y, z).immutable();
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox chunk,
			ChunkPos chunkPos, BlockPos pivot) {
		SectPalette p = SectPalette.of(faction, upper);
		switch (kind) {
			case TERRACE -> terrace(level, chunk, p);
			case GATE -> gate(level, chunk, p);
			case TOWER -> tower(level, chunk, p);
			case PLAZA -> plaza(level, chunk, p);
			case MEDITATION_HALL -> meditationHall(level, chunk, p);
			case TREASURY -> treasury(level, chunk, p);
			case MAIN_HALL -> mainHall(level, chunk, p);
			case PAGODA -> pagoda(level, chunk, p);
			case GARDEN -> garden(level, chunk, p);
			case DWELLING -> dwelling(level, chunk, p, false);
			case ELDER_HOUSE -> dwelling(level, chunk, p, true);
		}
	}

	// =====================================================================================================================
	// Helpers
	// =====================================================================================================================

	/** A random source for one spot of this piece: the same on every chunk that draws it. */
	private RandomSource rand(int x, int z, int salt) {
		return RandomSource.create(seed ^ (x * 341873128712L + z * 132897987541L + salt * 42317861L));
	}

	/**
	 * Places a block in the piece's frame, turned with the piece. Blocks whose shape depends on their neighbours (stairs,
	 * fences, walls, bars, panes) are settled once the whole chunk is generated, so they join up across the building.
	 */
	private void put(WorldGenLevel level, BoundingBox chunk, int x, int y, int z, BlockState state) {
		BlockPos pos = getWorldPos(x, y, z);
		if (!chunk.isInside(pos)) return;
		if (getMirror() != net.minecraft.world.level.block.Mirror.NONE) state = state.mirror(getMirror());
		if (getRotation() != net.minecraft.world.level.block.Rotation.NONE) state = state.rotate(getRotation());
		level.setBlock(pos, state, Block.UPDATE_CLIENTS);
		Block block = state.getBlock();
		if (block instanceof StairBlock || block instanceof CrossCollisionBlock || block instanceof WallBlock || block instanceof FenceBlock
				|| block instanceof IronBarsBlock) {
			level.getChunk(pos).markPosForPostprocessing(pos);
		}
		if (!state.getFluidState().isEmpty()) level.scheduleTick(pos, state.getFluidState().getType(), 0);
	}

	private void put(WorldGenLevel level, BoundingBox chunk, int x, int y, int z, Block block) {
		put(level, chunk, x, y, z, block.defaultBlockState());
	}

	private void fill(WorldGenLevel level, BoundingBox chunk, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
		boolean clearing = state.isAir();
		for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
			for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
				for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
					if (clearing) {
						// Most of what a building clears is open sky already.
						BlockPos pos = getWorldPos(x, y, z);
						if (!chunk.isInside(pos) || level.getBlockState(pos).isAir()) continue;
					}
					put(level, chunk, x, y, z, state);
				}
			}
		}
	}

	private void fill(WorldGenLevel level, BoundingBox chunk, int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
		fill(level, chunk, x0, y0, z0, x1, y1, z1, block.defaultBlockState());
	}

	private void air(WorldGenLevel level, BoundingBox chunk, int x0, int y0, int z0, int x1, int y1, int z1) {
		fill(level, chunk, x0, y0, z0, x1, y1, z1, Blocks.AIR.defaultBlockState());
	}

	private BlockState at(WorldGenLevel level, BoundingBox chunk, int x, int y, int z) {
		return getBlock(level, x, y, z, chunk);
	}

	/** Fills below {@code y} down to the ground (at most {@link #MAX_FOUNDATION}), so nothing floats over a dip. */
	private void foundation(WorldGenLevel level, BoundingBox chunk, int x, int y, int z, BlockState state) {
		BlockPos.MutableBlockPos pos = getWorldPos(x, y - 1, z);
		if (!chunk.isInside(pos)) return;
		for (int i = 0; i < MAX_FOUNDATION && pos.getY() > level.getMinBuildHeight(); i++) {
			BlockState here = level.getBlockState(pos);
			if (!(here.isAir() || here.canBeReplaced() || !here.getFluidState().isEmpty())) break;
			level.setBlock(pos, state, Block.UPDATE_CLIENTS);
			pos.move(Direction.DOWN);
		}
	}

	private void pillar(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int y0, int y1, int z) {
		fill(level, chunk, x, y0, z, x, y1, z, p.pillar(Direction.Axis.Y));
	}

	private void lanternHanging(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int y, int z) {
		put(level, chunk, x, y, z, p.lantern.defaultBlockState().setValue(LanternBlock.HANGING, true));
	}

	private void lanternStanding(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int y, int z) {
		put(level, chunk, x, y, z, p.lantern.defaultBlockState().setValue(LanternBlock.HANGING, false));
	}

	/** A stone lantern: a post, the lantern, and a little tiled cap. */
	private void stoneLantern(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int y, int z) {
		put(level, chunk, x, y, z, p.baseWall);
		lanternStanding(level, chunk, p, x, y + 1, z);
		put(level, chunk, x, y + 2, z, p.roofSlab(false));
	}

	private void candles(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int y, int z, int count) {
		put(level, chunk, x, y, z, p.candle.defaultBlockState().setValue(CandleBlock.CANDLES, Math.max(1, Math.min(4, count))).setValue(CandleBlock.LIT, true));
	}

	private void chest(WorldGenLevel level, BoundingBox chunk, int x, int y, int z, Direction facing, ResourceLocation loot) {
		BlockPos pos = getWorldPos(x, y, z);
		if (!chunk.isInside(pos)) return;
		put(level, chunk, x, y, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
		RandomizableContainerBlockEntity.setLootTable(level, rand(x, z, y), pos, loot);
	}

	/** A Spirit Pedestal showing {@code treasure}. */
	private void pedestal(WorldGenLevel level, BoundingBox chunk, int x, int y, int z, ItemStack treasure) {
		BlockPos pos = getWorldPos(x, y, z);
		if (!chunk.isInside(pos)) return;
		put(level, chunk, x, y, z, ModBlocks.SPIRIT_PEDESTAL);
		if (level.getBlockEntity(pos) instanceof SpiritPedestalBlockEntity pedestal) pedestal.setFruit(treasure);
	}

	private void wallBanner(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int y, int z, Direction facing) {
		put(level, chunk, x, y, z, p.wallBanner.defaultBlockState().setValue(WallBannerBlock.FACING, facing));
	}

	/** A growing herb of {@code years}, its look set by its age. */
	private void herb(WorldGenLevel level, BoundingBox chunk, Block plant, int x, int y, int z, int years) {
		BlockPos pos = getWorldPos(x, y, z);
		if (!chunk.isInside(pos)) return;
		BlockState state = plant.defaultBlockState();
		if (state.hasProperty(GinsengBlock.STAGE)) state = state.setValue(GinsengBlock.STAGE, GinsengBlock.stageFor(years));
		put(level, chunk, x, y, z, state);
		if (level.getBlockEntity(pos) instanceof GinsengBlockEntity ginseng) ginseng.setWildAge(years, level.getLevel().getGameTime());
	}

	// --- Roofs ---

	/**
	 * A hipped roof of tiles over the rectangle x0..x1, z0..z1 (eaves included), its lowest course at {@code y}: each course
	 * one block in and one block up, to a ridge. The four lowest corners turn up in a trim-coloured flick. {@code courses}
	 * limits how many courses are laid (for a lower eave under an upper storey); a finished roof is closed along its ridge.
	 */
	private void roof(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int z0, int x1, int z1, int y, int courses, boolean ridge) {
		for (int k = 0; k < courses; k++) {
			int ax0 = x0 + k, ax1 = x1 - k, az0 = z0 + k, az1 = z1 - k, ay = y + k;
			if (ax0 > ax1 || az0 > az1) return;
			if (ridge && (ax1 - ax0 <= 1 || az1 - az0 <= 1)) {
				// The ridge: full tiles along its length, trim at both ends.
				fill(level, chunk, ax0, ay, az0, ax1, ay, az1, p.roof);
				boolean alongX = ax1 - ax0 >= az1 - az0;
				int e0x = alongX ? ax0 : ax0, e0z = alongX ? az0 : az0, e1x = alongX ? ax1 : ax1, e1z = alongX ? az1 : az1;
				put(level, chunk, e0x, ay + 1, e0z, p.roofSlab(false));
				put(level, chunk, e1x, ay + 1, e1z, p.roofSlab(false));
				put(level, chunk, e0x, ay, e0z, p.roofTrim);
				put(level, chunk, e1x, ay, e1z, p.roofTrim);
				return;
			}
			for (int x = ax0; x <= ax1; x++) {
				put(level, chunk, x, ay, az0, p.roofStair(Direction.NORTH, false));
				put(level, chunk, x, ay, az1, p.roofStair(Direction.SOUTH, false));
			}
			for (int z = az0 + 1; z < az1; z++) {
				put(level, chunk, ax0, ay, z, p.roofStair(Direction.EAST, false));
				put(level, chunk, ax1, ay, z, p.roofStair(Direction.WEST, false));
			}
			if (k == 0) {
				// Upturned eave corners.
				for (int[] c : new int[][] {{ax0, az0}, {ax1, az0}, {ax0, az1}, {ax1, az1}}) {
					put(level, chunk, c[0], ay, c[1], p.roofTrim);
					put(level, chunk, c[0], ay + 1, c[1], p.roofSlab(false));
				}
			}
		}
		if (ridge) {
			// Ran out of courses: close the top flat.
			int ax0 = x0 + courses, ax1 = x1 - courses, az0 = z0 + courses, az1 = z1 - courses;
			if (ax0 <= ax1 && az0 <= az1) fill(level, chunk, ax0, y + courses - 1, az0, ax1, y + courses - 1, az1, p.roof);
		}
	}

	/** Courses a hipped roof over this rectangle needs to reach its ridge. */
	private static int ridgeCourses(int x0, int z0, int x1, int z1) {
		return (Math.min(x1 - x0, z1 - z0)) / 2 + 1;
	}

	/**
	 * The walls of a building between x0..x1, z0..z1, from {@code y0} to {@code y1}: pillars at the corners and every
	 * {@code bay} blocks, plaster between on a stone foot, lattice windows in each bay unless {@code solid} (small barred
	 * windows instead), the doorway ({@code door} wide, {@code doorHeight} high) in the middle of the front, and a timber
	 * beam with bronze brackets above the pillars all round.
	 */
	private void walls(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int z0, int x1, int z1, int y0, int y1, int bay, int door,
			int doorHeight, boolean solid) {
		int cx = (x0 + x1) / 2;
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean edgeX = x == x0 || x == x1, edgeZ = z == z0 || z == z1;
				if (!edgeX && !edgeZ) continue;
				boolean post = edgeX && edgeZ || edgeZ && (x - x0) % bay == 0 || edgeX && (z - z0) % bay == 0;
				if (post) {
					pillar(level, chunk, p, x, y0, y1, z);
					put(level, chunk, x, y1 + 1, z, p.accent);
					continue;
				}
				put(level, chunk, x, y0, z, p.wallBase);
				fill(level, chunk, x, y0 + 1, z, x, y1, z, p.wall);
				// Windows in the middle of each bay, two blocks high.
				int along = edgeZ ? (x - x0) % bay : (z - z0) % bay;
				boolean middle = along >= 1 && along <= bay - 1 && Math.abs(along - bay / 2.0) <= (bay > 3 ? 1 : 0.5);
				if (middle && y1 - y0 >= 3) {
					if (solid) put(level, chunk, x, y0 + 2, z, Blocks.IRON_BARS);
					else fill(level, chunk, x, y0 + 1, z, x, y0 + 2, z, p.window);
				}
				put(level, chunk, x, y1 + 1, z, p.beam(edgeZ ? Direction.Axis.X : Direction.Axis.Z));
			}
		}
		// The doorway, in the front wall.
		if (door > 0) air(level, chunk, cx - door / 2, y0, z0, cx + door / 2, y0 + doorHeight - 1, z0);
	}

	/** A raised stone base with steps down at the front middle, {@code height} above the paving. */
	private void plinth(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int z0, int x1, int z1, int height, int stepsWidth) {
		if (height <= 0) return;
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				fill(level, chunk, x, 1, z, x, height, z, p.foundation);
				foundation(level, chunk, x, 1, z, p.foundation.defaultBlockState());
			}
		}
		// A balustrade of stone round the top edge, broken by the steps.
		int cx = (x0 + x1) / 2;
		for (int i = 0; i < height; i++) {
			int z = z0 - 1 - i;
			for (int x = cx - stepsWidth / 2; x <= cx + stepsWidth / 2; x++) put(level, chunk, x, height - i, z, p.baseStair(Direction.NORTH));
		}
	}

	/** Clears the inside of a building and lays its floor at {@code floorY}. */
	private void interior(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int z0, int x1, int z1, int floorY, int topY) {
		fill(level, chunk, x0, floorY, z0, x1, floorY, z1, p.floor);
		air(level, chunk, x0, floorY + 1, z0, x1, topY, z1);
	}

	// =====================================================================================================================
	// The grounds
	// =====================================================================================================================

	/**
	 * The whole compound: the ground levelled and paved (stone everywhere, a path down the middle and round the plaza), a
	 * wall round it with a gap for the gate, stone lanterns along the path, trees in planters in the front court.
	 */
	private void terrace(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = width(), d = depth();
		int cu = w / 2;
		int plazaV = extra.getInt("PlazaV");
		int plazaR = extra.getInt("PlazaR");
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				foundation(level, chunk, x, 0, z, p.foundation.defaultBlockState());
				boolean onPath = Math.abs(x - cu) <= 2 || Math.abs(z - plazaV) <= 1 && Math.abs(x - cu) <= w / 2 - 10;
				put(level, chunk, x, 0, z, onPath ? p.path : p.paving);
				air(level, chunk, x, 1, z, x, 22, z);
			}
		}
		// The path's edging.
		for (int z = 7; z < d - 1; z++) {
			put(level, chunk, cu - 3, 0, z, p.foundation);
			put(level, chunk, cu + 3, 0, z, p.foundation);
		}
		// The outer wall: plaster on stone, pillars every six, a tiled coping; the gate fills the gap in the front.
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				boolean edge = x == 0 || x == w - 1 || z == 0 || z == d - 1;
				if (!edge) continue;
				if (z == 0 && Math.abs(x - cu) <= 10) continue; // the gate
				// Side and back gates: openings with a lintel, so patrols can get out to the grounds' edge from anywhere. The
				// side ones open onto the lane between two dwellings, the back ones either side of the pagoda.
				boolean sideGate = (x == 0 || x == w - 1) && z == sideGateV(d) || z == d - 1 && Math.abs(x - cu) == 9;
				if (sideGate) {
					put(level, chunk, x, 4, z, p.beam(x == 0 || x == w - 1 ? Direction.Axis.Z : Direction.Axis.X));
					put(level, chunk, x, 5, z, p.roof);
					put(level, chunk, x, 6, z, p.roofSlab(false));
					continue;
				}
				boolean post = (x % 6 == 0 && (z == 0 || z == d - 1)) || (z % 6 == 0 && (x == 0 || x == w - 1));
				if (post) {
					pillar(level, chunk, p, x, 1, 4, z);
				} else {
					put(level, chunk, x, 1, z, p.wallBase);
					fill(level, chunk, x, 2, z, x, 4, z, p.wall);
				}
				put(level, chunk, x, 5, z, p.roof);
				put(level, chunk, x, 6, z, p.roofSlab(false));
			}
		}
		// Stone lanterns lining the path from the gate to the plaza and on to the main hall.
		for (int z = 10; z < d - 12; z += 6) {
			if (Math.abs(z - plazaV) <= plazaR + 1) continue;
			stoneLantern(level, chunk, p, cu - 4, 1, z);
			stoneLantern(level, chunk, p, cu + 4, 1, z);
		}
		// Trees in planters at the corners of the front court.
		int frontCourt = extra.getInt("FrontCourt");
		for (int[] spot : new int[][] {{cu - 9, 9}, {cu + 9, 9}, {cu - 9, frontCourt}, {cu + 9, frontCourt}}) {
			tree(level, chunk, p, spot[0], spot[1], rand(spot[0], spot[1], 7));
		}
	}

	/** The lane between the two middle dwellings of a side (dwellings start at 9 and repeat every 8; see SectStructure). */
	public static int sideGateV(int depth) {
		int slots = 0;
		for (int v0 = 9; v0 + 6 <= depth - 9; v0 += 8) slots++;
		return 9 + 8 * (slots / 2) - 1;
	}

	/** A small tree in a stone-edged planter: the palette's trunk and blossom (persistent, so it never decays). */
	private void tree(WorldGenLevel level, BoundingBox chunk, SectPalette p, int cx, int cz, RandomSource random) {
		for (int x = cx - 1; x <= cx + 1; x++) {
			for (int z = cz - 1; z <= cz + 1; z++) {
				put(level, chunk, x, 0, z, (x == cx && z == cz) ? Blocks.DIRT : Blocks.GRASS_BLOCK);
				if (x != cx || z != cz) put(level, chunk, x, 1, z, x == cx || z == cz ? Blocks.MOSS_CARPET : p.baseSlab(false).getBlock());
			}
		}
		int height = 4 + random.nextInt(2);
		BlockState log = p.trunk.defaultBlockState();
		if (log.hasProperty(RotatedPillarBlock.AXIS)) log = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
		fill(level, chunk, cx, 1, cz, cx, height, cz, log);
		BlockState leaf = p.leaves.defaultBlockState();
		if (leaf.hasProperty(LeavesBlock.PERSISTENT)) leaf = leaf.setValue(LeavesBlock.PERSISTENT, true);
		for (int dy = -1; dy <= 2; dy++) {
			int r = dy == 2 ? 1 : 2;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.abs(dx) == r && Math.abs(dz) == r && (r == 2 || random.nextBoolean())) continue;
					if (dx == 0 && dz == 0 && dy < 1) continue;
					put(level, chunk, cx + dx, height + dy, cz + dz, leaf);
				}
			}
		}
	}

	/**
	 * The gate (shanmen): a three-bay hall straddling the wall, its middle bay the way in, under a double-eaved roof, with
	 * the sect's banners on its pillars and lanterns over the passage.
	 */
	private void gate(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = width(), d = depth();
		int x0 = 2, x1 = w - 3, z0 = 1, z1 = d - 2;
		int cx = w / 2;
		fill(level, chunk, x0 - 1, 0, z0 - 1, x1 + 1, 0, z1 + 1, p.foundation);
		air(level, chunk, x0, 1, z0, x1, 12, z1);
		for (int x : new int[] {x0, cx - 3, cx + 3, x1}) {
			for (int z : new int[] {z0, z1}) pillar(level, chunk, p, x, 1, 7, z);
		}
		// Solid side bays; the middle bay is open.
		for (int x = x0 + 1; x < cx - 3; x++) {
			for (int z = z0; z <= z1; z++) {
				put(level, chunk, x, 1, z, p.wallBase);
				fill(level, chunk, x, 2, z, x, 7, z, p.wall);
			}
		}
		for (int x = cx + 4; x < x1; x++) {
			for (int z = z0; z <= z1; z++) {
				put(level, chunk, x, 1, z, p.wallBase);
				fill(level, chunk, x, 2, z, x, 7, z, p.wall);
			}
		}
		for (int z = z0 + 1; z < z1; z++) {
			put(level, chunk, x0, 1, z, p.wallBase);
			fill(level, chunk, x0, 2, z, x0, 7, z, p.wall);
			put(level, chunk, x1, 1, z, p.wallBase);
			fill(level, chunk, x1, 2, z, x1, 7, z, p.wall);
		}
		// Beams and the lintel over the passage, with the sect's plaque.
		fill(level, chunk, x0, 8, z0, x1, 8, z0, p.beam(Direction.Axis.X));
		fill(level, chunk, x0, 8, z1, x1, 8, z1, p.beam(Direction.Axis.X));
		fill(level, chunk, x0, 8, z0 + 1, x1, 8, z1 - 1, p.floor);
		fill(level, chunk, cx - 2, 7, z0, cx + 2, 7, z0, p.beam(Direction.Axis.X));
		fill(level, chunk, cx - 1, 6, z0, cx + 1, 6, z0, p.accent);
		lanternHanging(level, chunk, p, cx - 2, 6, z0 + 1);
		lanternHanging(level, chunk, p, cx + 2, 6, z0 + 1);
		lanternHanging(level, chunk, p, cx, 7, z1 - 1);
		for (int x : new int[] {cx - 3, cx + 3}) wallBanner(level, chunk, p, x, 5, z0 - 1, Direction.SOUTH);
		// Lower eave, the upper storey, and its roof.
		roof(level, chunk, p, x0 - 2, z0 - 1, x1 + 2, z1 + 1, 9, 2, false);
		for (int x = x0 + 2; x <= x1 - 2; x++) {
			for (int z = z0 + 1; z <= z1 - 1; z++) {
				boolean edge = x == x0 + 2 || x == x1 - 2 || z == z0 + 1 || z == z1 - 1;
				if (!edge) continue;
				boolean post = (x == x0 + 2 || x == x1 - 2) && (z == z0 + 1 || z == z1 - 1);
				fill(level, chunk, x, 9, z, x, 11, z, post ? p.pillar(Direction.Axis.Y) : p.wall.defaultBlockState());
				if (!post && z == z0 + 1 && (x - cx) % 3 == 0) put(level, chunk, x, 10, z, p.window);
			}
		}
		roof(level, chunk, p, x0, z0 - 1, x1, z1 + 1, 12, ridgeCourses(x0, z0 - 1, x1, z1 + 1), true);
	}

	/** A corner watchtower: a small square pavilion of two storeys over the corner of the wall. */
	private void tower(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = width(), d = depth();
		int x0 = 1, x1 = w - 2, z0 = 1, z1 = d - 2;
		fill(level, chunk, x0, 0, z0, x1, 0, z1, p.foundation);
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) foundation(level, chunk, x, 0, z, p.foundation.defaultBlockState());
		}
		fill(level, chunk, x0, 1, z0, x1, 4, z1, p.wallBase);
		air(level, chunk, x0 + 1, 1, z0 + 1, x1 - 1, 9, z1 - 1);
		fill(level, chunk, x0 + 1, 4, z0 + 1, x1 - 1, 4, z1 - 1, p.floor);
		walls(level, chunk, p, x0, z0, x1, z1, 5, 7, x1 - x0, 0, 0, false);
		put(level, chunk, (x0 + x1) / 2, 1, z0, Blocks.AIR); // a door into the base
		put(level, chunk, (x0 + x1) / 2, 2, z0, Blocks.AIR);
		lanternHanging(level, chunk, p, (x0 + x1) / 2, 7, (z0 + z1) / 2);
		roof(level, chunk, p, x0 - 1, z0 - 1, x1 + 1, z1 + 1, 9, ridgeCourses(x0 - 1, z0 - 1, x1 + 1, z1 + 1), true);
	}

	// =====================================================================================================================
	// The Formation plaza
	// =====================================================================================================================

	/** Radius of the plaza, of its dais, and of the ring of seals and veins. */
	public static final int PLAZA_RADIUS = 12;
	public static final int DAIS_RADIUS = 4;
	public static final int RING_RADIUS = 9;

	/**
	 * The heart of the sect: a round paved plaza with the Formation Core on a stepped dais, and, painted on the stone (only
	 * Qi Sense shows it), its array: eight lines of seals from the core to eight Qi Veins set in the paving, joined by a ring.
	 * Stone lanterns stand between the lines, and red mats where the elders keep watch.
	 */
	private void plaza(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int c = PLAZA_RADIUS;
		for (int x = 0; x <= 2 * c; x++) {
			for (int z = 0; z <= 2 * c; z++) {
				double dist = Math.sqrt((x - c) * (x - c) + (z - c) * (z - c));
				if (dist > c + 0.5) continue;
				Block paving = dist > c - 1.5 ? p.foundation : dist > RING_RADIUS + 1.5 ? p.paving : dist > DAIS_RADIUS + 0.5 ? p.path : p.foundation;
				put(level, chunk, x, 0, z, paving);
				air(level, chunk, x, 1, z, x, 6, z);
				if (dist <= DAIS_RADIUS + 0.5) {
					put(level, chunk, x, 1, z, dist > DAIS_RADIUS - 0.5 ? p.accent : Blocks.CHISELED_STONE_BRICKS);
				}
			}
		}
		// The core on its dais.
		BlockPos corePos = getWorldPos(c, 2, c);
		if (chunk.isInside(corePos)) {
			put(level, chunk, c, 2, c, ModBlocks.FORMATION_CORE);
			if (level.getBlockEntity(corePos) instanceof FormationCoreBlockEntity core) core.setBlueprint(extra.getCompound("Blueprint"));
		}
		// The array: eight spokes and the ring, with a vein at each spoke's end.
		for (int k = 0; k < 8; k++) {
			double angle = Math.PI / 4 * k;
			for (double t = 1; t <= RING_RADIUS + 0.01; t += 0.5) {
				int x = c + (int) Math.round(Math.cos(angle) * t);
				int z = c + (int) Math.round(Math.sin(angle) * t);
				if (x == c && z == c) continue;
				seal(level, chunk, x, z);
			}
			int vx = c + (int) Math.round(Math.cos(angle) * RING_RADIUS);
			int vz = c + (int) Math.round(Math.sin(angle) * RING_RADIUS);
			put(level, chunk, vx, 0, vz, ModBlocks.QI_VEIN);
		}
		for (int x = 0; x <= 2 * c; x++) {
			for (int z = 0; z <= 2 * c; z++) {
				if (Math.round(Math.sqrt((x - c) * (x - c) + (z - c) * (z - c))) == RING_RADIUS) seal(level, chunk, x, z);
			}
		}
		// Lanterns between the spokes, and the elders' mats nearer in.
		for (int k = 0; k < 8; k++) {
			double angle = Math.PI / 4 * k + Math.PI / 8;
			stoneLantern(level, chunk, p, c + (int) Math.round(Math.cos(angle) * (c - 1)), 1, c + (int) Math.round(Math.sin(angle) * (c - 1)));
		}
		for (int[] mat : corePosts()) put(level, chunk, mat[0], 1, mat[1], ModBlocks.RED_MEDITATION_MAT);
	}

	/** The elders' watch posts around the dais, between the spokes. */
	public static int[][] corePosts() {
		int c = PLAZA_RADIUS;
		int[][] posts = new int[4][];
		for (int k = 0; k < 4; k++) {
			double angle = Math.PI / 2 * k + Math.PI / 8 + Math.PI / 4;
			posts[k] = new int[] {c + (int) Math.round(Math.cos(angle) * 6.5), c + (int) Math.round(Math.sin(angle) * 6.5)};
		}
		return posts;
	}

	/** A seal at (x, z): on the dais's top within it, on the paving outside. */
	private void seal(WorldGenLevel level, BoundingBox chunk, int x, int z) {
		int c = PLAZA_RADIUS;
		double dist = Math.sqrt((x - c) * (x - c) + (z - c) * (z - c));
		int y = dist <= DAIS_RADIUS + 0.5 ? 2 : 1;
		BlockState here = at(level, chunk, x, y, z);
		if (!here.isAir()) return; // a lantern or mat already stands there
		put(level, chunk, x, y, z, ModBlocks.SEAL.defaultBlockState().setValue(SealBlock.FACE, Direction.DOWN));
	}

	// =====================================================================================================================
	// Halls
	// =====================================================================================================================

	/** Where the walls of a hall stand within its piece: {@code margin} in from each side for the eaves. */
	private record Frame(int x0, int z0, int x1, int z1) {
		int cx() { return (x0 + x1) / 2; }
	}

	private Frame frame(int margin, int front) {
		return new Frame(margin, front, width() - 1 - margin, depth() - 1 - margin);
	}

	/**
	 * The meditation hall: an open hall of lattice and plaster, rows of meditation mats for the inner disciples, and at its
	 * back an altar with candles and a pedestal of aged ginseng whose qi helps those who sit near it.
	 */
	private void meditationHall(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		Frame f = frame(2, 2);
		int floor = 1, top = floor + 6;
		plinth(level, chunk, p, f.x0 - 1, f.z0, f.x1 + 1, f.z1 + 1, floor, 5);
		interior(level, chunk, p, f.x0 + 1, f.z0 + 1, f.x1 - 1, f.z1 - 1, floor, top + 4);
		walls(level, chunk, p, f.x0, f.z0, f.x1, f.z1, floor + 1, top - 1, 4, 3, 4, false);
		fill(level, chunk, f.x0 + 1, top, f.z0 + 1, f.x1 - 1, top, f.z1 - 1, p.floor); // ceiling
		roof(level, chunk, p, 0, 0, width() - 1, depth() - 1, top + 1, ridgeCourses(0, 0, width() - 1, depth() - 1), true);
		// Mats in rows, a carpet aisle down the middle.
		for (int[] mat : meditationMats()) put(level, chunk, mat[0], floor + 1, mat[1], ModBlocks.MEDITATION_MAT);
		for (int z = f.z0 + 1; z < f.z1 - 2; z++) put(level, chunk, f.cx(), floor + 1, z, p.carpet);
		// The altar.
		int az = f.z1 - 1;
		fill(level, chunk, f.cx() - 2, floor + 1, az, f.cx() + 2, floor + 1, az, Blocks.POLISHED_DEEPSLATE);
		pedestal(level, chunk, f.cx(), floor + 1, az - 1, GinsengItem.create(ModItems.GINSENG, 150 + rand(1, 1, 3).nextInt(600)));
		candles(level, chunk, p, f.cx() - 2, floor + 2, az, 3);
		candles(level, chunk, p, f.cx() + 2, floor + 2, az, 3);
		put(level, chunk, f.cx() - 1, floor + 2, az, Blocks.POTTED_BAMBOO);
		put(level, chunk, f.cx() + 1, floor + 2, az, Blocks.POTTED_AZALEA);
		wallBanner(level, chunk, p, f.cx(), floor + 4, f.z1 - 1, Direction.SOUTH);
		for (int x = f.x0 + 2; x <= f.x1 - 2; x += 4) lanternHanging(level, chunk, p, x, top - 1, (f.z0 + f.z1) / 2);
	}

	/**
	 * The meditation hall's mats, in its frame: every other block across, either side of the aisle, in rows every other block
	 * back to just short of the altar (4 mats in a small sect's hall, 8 in a medium one's, 18 in a large one's).
	 */
	public int[][] meditationMats() {
		Frame f = frame(2, 2);
		java.util.List<int[]> mats = new java.util.ArrayList<>();
		for (int z = f.z0 + 2; z <= f.z1 - 3; z += 2) {
			for (int x = f.x0 + 2; x <= f.x1 - 2; x += 2) {
				if (x == f.cx()) continue; // the aisle
				mats.add(new int[] {x, z});
			}
		}
		return mats.toArray(new int[0][]);
	}

	/**
	 * The treasury: thick stone walls with barred windows, pedestals down both sides bearing the sect's treasures (old fruit,
	 * Spirit Ginseng, fine pills and rings), chests of the sect's wealth at the back, and red mats where the elders stand guard.
	 */
	private void treasury(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		Frame f = frame(2, 3);
		int floor = 2, top = floor + 5;
		plinth(level, chunk, p, f.x0 - 1, f.z0, f.x1 + 1, f.z1 + 1, floor, 3);
		interior(level, chunk, p, f.x0 + 1, f.z0 + 1, f.x1 - 1, f.z1 - 1, floor, top + 4);
		for (int x = f.x0; x <= f.x1; x++) {
			for (int z = f.z0; z <= f.z1; z++) {
				boolean edge = x == f.x0 || x == f.x1 || z == f.z0 || z == f.z1;
				if (edge) fill(level, chunk, x, floor + 1, z, x, top - 1, z, p.foundation);
			}
		}
		walls(level, chunk, p, f.x0, f.z0, f.x1, f.z1, floor + 1, top - 1, 4, 1, 3, true);
		fill(level, chunk, f.x0 + 1, top, f.z0 + 1, f.x1 - 1, top, f.z1 - 1, p.floor);
		roof(level, chunk, p, 0, 1, width() - 1, depth() - 1, top + 1, ridgeCourses(0, 1, width() - 1, depth() - 1), true);
		RandomSource random = rand(3, 3, 11);
		int[][] spots = treasurePedestals();
		for (int i = 0; i < spots.length; i++) pedestal(level, chunk, spots[i][0], floor + 1, spots[i][1], treasure(random, i));
		for (int x = f.cx() - 2; x <= f.cx() + 2; x += 2) chest(level, chunk, x, floor + 1, f.z1 - 1, Direction.SOUTH, TREASURY_LOOT);
		for (int z = f.z0 + 1; z < f.z1 - 1; z++) put(level, chunk, f.cx(), floor + 1, z, p.carpetAlt);
		lanternHanging(level, chunk, p, f.cx(), top - 1, (f.z0 + f.z1) / 2);
		lanternHanging(level, chunk, p, f.cx(), top - 1, f.z1 - 2);
		for (int[] guard : treasuryGuards()) put(level, chunk, guard[0], guard[1], guard[2], ModBlocks.RED_MEDITATION_MAT);
	}

	/** The pedestals down both sides of the treasury. */
	public int[][] treasurePedestals() {
		Frame f = frame(2, 3);
		java.util.List<int[]> spots = new java.util.ArrayList<>();
		for (int z = f.z0 + 2; z <= f.z1 - 3; z += 2) {
			spots.add(new int[] {f.x0 + 2, z});
			spots.add(new int[] {f.x1 - 2, z});
		}
		return spots.toArray(new int[0][]);
	}

	/** The elders' posts by the treasury: either side of its door outside, and inside by the chests. {x, y, z} */
	public int[][] treasuryGuards() {
		Frame f = frame(2, 3);
		return new int[][] {{f.cx() - 3, 1, f.z0 - 2}, {f.cx() + 3, 1, f.z0 - 2}, {f.cx(), 3, f.z1 - 3}};
	}

	/** What the treasury's pedestals hold: centuries-old fruit and ginseng, Spirit Ginseng, and the odd fine pill or ring. */
	private ItemStack treasure(RandomSource random, int index) {
		int roll = random.nextInt(10);
		if (index == 0 || roll < 3) return CultivationFruitItem.create(300 + (int) (Math.pow(random.nextFloat(), 2) * 4700));
		if (roll < 6) return GinsengItem.create(ModItems.SPIRIT_GINSENG, 200 + (int) (Math.pow(random.nextFloat(), 2) * 3800));
		if (roll < 8) return GinsengItem.create(random.nextBoolean() ? ModItems.PURPLE_LINGZHI : ModItems.OCHRE_HUANGJING, 300 + random.nextInt(2000));
		if (roll < 9) return PillItem.create(random.nextBoolean() ? ModItems.CULTIVATION_PILL : ModItems.QI_GATHERING_PILL,
				PillGrade.byIndex(2 + random.nextInt(3)), 500 + random.nextInt(4000));
		return new ItemStack(random.nextInt(4) == 0 ? ModItems.RING_OF_TRANSCENDENCE : ModItems.RING_OF_POWER);
	}

	/**
	 * The main hall: on a high stone terrace with steps up the front, a double-eaved hall with a clerestory. Inside, two rows
	 * of pillars lead down a red carpet to the Sect Master's dais: two steps up to a red silk mat before a carved screen,
	 * lanterns either side, drums by the door.
	 */
	private void mainHall(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int m = 3;
		int front = m + 3;
		Frame f = new Frame(m, front, width() - 1 - m, depth() - 1 - m);
		int floor = 3, top = floor + 8;
		// The terrace and its balustrade.
		plinth(level, chunk, p, f.x0 - 2, f.z0 - 0, f.x1 + 2, f.z1 + 2, floor, 7);
		for (int x = f.x0 - 2; x <= f.x1 + 2; x++) {
			if (Math.abs(x - f.cx()) <= 3) continue;
			put(level, chunk, x, floor + 1, f.z0, p.baseWall);
		}
		for (int z = f.z0; z <= f.z1 + 2; z++) {
			put(level, chunk, f.x0 - 2, floor + 1, z, p.baseWall);
			put(level, chunk, f.x1 + 2, floor + 1, z, p.baseWall);
		}
		int hz0 = f.z0 + 2;
		interior(level, chunk, p, f.x0 + 1, hz0 + 1, f.x1 - 1, f.z1 - 1, floor, top + 10);
		walls(level, chunk, p, f.x0, hz0, f.x1, f.z1, floor + 1, top - 1, 4, 5, 6, false);
		// Interior pillars.
		for (int z = hz0 + 3; z <= f.z1 - 4; z += 4) {
			pillar(level, chunk, p, f.cx() - 5, floor + 1, top - 1, z);
			pillar(level, chunk, p, f.cx() + 5, floor + 1, top - 1, z);
		}
		fill(level, chunk, f.x0 + 1, top, hz0 + 1, f.x1 - 1, top, f.z1 - 1, p.beam(Direction.Axis.X));
		// Double eave: the lower roof, the clerestory, the upper roof.
		int ex0 = f.x0 - 2, ez0 = hz0 - 2, ex1 = f.x1 + 2, ez1 = f.z1 + 2;
		// Four courses: the last meets the clerestory, so no gap opens between the two roofs.
		roof(level, chunk, p, ex0, ez0, ex1, ez1, top + 1, 4, false);
		int cx0 = f.x0 + 2, cz0 = hz0 + 2, cx1 = f.x1 - 2, cz1 = f.z1 - 2;
		for (int x = cx0; x <= cx1; x++) {
			for (int z = cz0; z <= cz1; z++) {
				boolean edgeX = x == cx0 || x == cx1, edgeZ = z == cz0 || z == cz1;
				if (!edgeX && !edgeZ) continue;
				boolean post = edgeX && edgeZ || edgeZ && (x - cx0) % 4 == 0 || edgeX && (z - cz0) % 4 == 0;
				if (post) {
					fill(level, chunk, x, top + 1, z, x, top + 4, z, p.pillar(Direction.Axis.Y));
				} else {
					fill(level, chunk, x, top + 1, z, x, top + 4, z, p.wall.defaultBlockState());
					if (edgeZ && (x - cx0) % 2 == 0) put(level, chunk, x, top + 3, z, p.window);
				}
			}
		}
		roof(level, chunk, p, cx0 - 2, cz0 - 2, cx1 + 2, cz1 + 2, top + 5, ridgeCourses(cx0 - 2, cz0 - 2, cx1 + 2, cz1 + 2), true);
		// The carpet and the dais.
		for (int z = hz0; z < f.z1 - 5; z++) fill(level, chunk, f.cx() - 1, floor + 1, z, f.cx() + 1, floor + 1, z, p.carpet);
		int dz = f.z1 - 5;
		fill(level, chunk, f.cx() - 5, floor + 1, dz, f.cx() + 5, floor + 1, f.z1 - 1, p.foundation);
		for (int x = f.cx() - 5; x <= f.cx() + 5; x++) put(level, chunk, x, floor + 1, dz - 1, p.baseStair(Direction.NORTH));
		fill(level, chunk, f.cx() - 3, floor + 2, dz + 1, f.cx() + 3, floor + 2, f.z1 - 1, p.accent);
		for (int x = f.cx() - 3; x <= f.cx() + 3; x++) put(level, chunk, x, floor + 2, dz, p.baseStair(Direction.NORTH));
		int[] seat = masterSeat();
		put(level, chunk, seat[0], seat[1], seat[2], ModBlocks.RED_MEDITATION_MAT);
		// The screen behind the seat.
		fill(level, chunk, f.cx() - 3, floor + 3, f.z1 - 1, f.cx() + 3, floor + 6, f.z1 - 1, p.beam(Direction.Axis.X));
		fill(level, chunk, f.cx() - 2, floor + 4, f.z1 - 1, f.cx() + 2, floor + 5, f.z1 - 1, p.roofTrim);
		wallBanner(level, chunk, p, f.cx(), floor + 6, f.z1 - 2, Direction.SOUTH);
		for (int x : new int[] {f.cx() - 3, f.cx() + 3}) {
			put(level, chunk, x, floor + 3, dz + 1, p.window);
			lanternStanding(level, chunk, p, x, floor + 4, dz + 1);
		}
		candles(level, chunk, p, f.cx() - 2, floor + 3, f.z1 - 2, 4);
		candles(level, chunk, p, f.cx() + 2, floor + 3, f.z1 - 2, 4);
		// Drums by the door; lanterns down the hall; chests of the master's own behind the screen's wings.
		put(level, chunk, f.x0 + 2, floor + 1, hz0 + 1, Blocks.NOTE_BLOCK);
		put(level, chunk, f.x1 - 2, floor + 1, hz0 + 1, Blocks.NOTE_BLOCK);
		for (int z = hz0 + 2; z <= f.z1 - 4; z += 4) {
			lanternHanging(level, chunk, p, f.cx() - 5, top - 1, z + 2);
			lanternHanging(level, chunk, p, f.cx() + 5, top - 1, z + 2);
		}
		chest(level, chunk, f.x0 + 1, floor + 1, f.z1 - 1, Direction.SOUTH, ELDER_LOOT);
		chest(level, chunk, f.x1 - 1, floor + 1, f.z1 - 1, Direction.SOUTH, ELDER_LOOT);
	}

	/** The Sect Master's mat on the dais, {x, y, z} in the main hall's frame. */
	public int[] masterSeat() {
		int m = 3;
		Frame f = new Frame(m, m + 3, width() - 1 - m, depth() - 1 - m);
		return new int[] {f.cx(), 3 + 3, f.z1 - 3};
	}

	/**
	 * The pagoda: a tower of {@code Tiers} square storeys, each a little narrower every second floor and ringed by its own eave,
	 * crowned with a spire. The ground floor is the sect's library; a ladder climbs to a quiet meditation chamber at the top.
	 */
	private void pagoda(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int tiers = extra.getInt("Tiers");
		int w = width();
		int m = 2;
		fill(level, chunk, m - 1, 0, m - 1, w - m, 0, w - m, p.foundation);
		for (int x = m - 1; x <= w - m; x++) {
			for (int z = m - 1; z <= w - m; z++) foundation(level, chunk, x, 0, z, p.foundation.defaultBlockState());
		}
		int base = 1;
		int topInsetForLadder = (tiers - 1) / 2;
		int ladderX = w / 2, ladderZ = w - 1 - m - topInsetForLadder - 1; // inside even the narrowest storey
		for (int tier = 0; tier < tiers; tier++) {
			int inset = tier / 2;
			int x0 = m + inset, x1 = w - 1 - m - inset;
			if (x1 - x0 < 4) break;
			fill(level, chunk, x0 + 1, base - 1, x0 + 1, x1 - 1, base - 1, x1 - 1, p.floor);
			air(level, chunk, x0 + 1, base, x0 + 1, x1 - 1, base + 3, x1 - 1);
			walls(level, chunk, p, x0, x0, x1, x1, base, base + 3, x1 - x0, tier == 0 ? 3 : 0, 3, false);
			// A window in the middle of each side above the ground floor.
			if (tier > 0) {
				int mid = (x0 + x1) / 2;
				put(level, chunk, mid, base + 1, x0, p.window);
				put(level, chunk, mid, base + 1, x1, p.window);
				put(level, chunk, x0, base + 1, mid, p.window);
				put(level, chunk, x1, base + 1, mid, p.window);
			}
			// The eave round this storey (tiles out over the walls, then in to the next storey's foot; the next storey's walls
			// replace whatever of it they stand on).
			roof(level, chunk, p, x0 - 2, x0 - 2, x1 + 2, x1 + 2, base + 4, 3, false);
			lanternHanging(level, chunk, p, (x0 + x1) / 2, base + 3, (x0 + x1) / 2 + 1);
			base += 5;
		}
		// The ladder, laid once all the floors are in: one straight shaft against a timber spine, through every floor.
		int topFloor = 1 + 5 * (tiers - 1);
		for (int y = 1; y < topFloor; y++) {
			put(level, chunk, ladderX, y, ladderZ + 1, p.floor);
			put(level, chunk, ladderX, y, ladderZ, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
		}
		// The library on the ground floor.
		int x0 = m, x1 = w - 1 - m;
		for (int z = x0 + 1; z <= x1 - 1; z++) {
			if (z == (x0 + x1) / 2) continue;
			fill(level, chunk, x0 + 1, 1, z, x0 + 1, 2, z, Blocks.BOOKSHELF);
			fill(level, chunk, x1 - 1, 1, z, x1 - 1, 2, z, Blocks.BOOKSHELF);
		}
		put(level, chunk, (x0 + x1) / 2 - 1, 1, x1 - 2, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.SOUTH));
		chest(level, chunk, (x0 + x1) / 2 + 1, 1, x1 - 1, Direction.SOUTH, LIBRARY_LOOT);
		// The top storey: a meditation mat under the spire.
		int topInset = (tiers - 1) / 2;
		int tx0 = m + topInset, tx1 = w - 1 - m - topInset;
		int topBase = 1 + 5 * (tiers - 1);
		put(level, chunk, (tx0 + tx1) / 2, topBase, (tx0 + tx1) / 2 - 1, ModBlocks.RED_MEDITATION_MAT);
		// The crowning roof and spire.
		roof(level, chunk, p, tx0 - 1, tx0 - 1, tx1 + 1, tx1 + 1, base, ridgeCourses(tx0 - 1, tx0 - 1, tx1 + 1, tx1 + 1), true);
		int cx = w / 2;
		int spire = base + ridgeCourses(tx0 - 1, tx0 - 1, tx1 + 1, tx1 + 1);
		put(level, chunk, cx, spire, cx, p.accent);
		put(level, chunk, cx, spire + 1, cx, p.window);
		put(level, chunk, cx, spire + 2, cx, Blocks.LIGHTNING_ROD);
	}

	/** The meditation mat at the top of the pagoda, {x, y, z}. */
	public int[] pagodaSeat() {
		int tiers = extra.getInt("Tiers");
		int m = 2, w = width();
		int topInset = (tiers - 1) / 2;
		int tx0 = m + topInset, tx1 = w - 1 - m - topInset;
		return new int[] {(tx0 + tx1) / 2, 1 + 5 * (tiers - 1), (tx0 + tx1) / 2 - 1};
	}

	/**
	 * An herb garden: a fence of bamboo round plots of good soil where the sect grows its ginseng (now and then a Spirit
	 * Ginseng), lingzhi and huangjing, all a good deal older than the wild, a Spirit Peach Tree in fruit, and a little pond
	 * with a Spirit Lotus.
	 */
	private void garden(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = width(), d = depth();
		RandomSource random = rand(5, 5, 23);
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				boolean edge = x == 0 || x == w - 1 || z == 0 || z == d - 1;
				boolean path = x == w / 2 || z % 5 == 0;
				put(level, chunk, x, 0, z, edge || path ? Blocks.GRAVEL : (rand(x, z, 1).nextInt(4) == 0 ? Blocks.PODZOL : Blocks.GRASS_BLOCK));
				air(level, chunk, x, 1, z, x, 9, z);
				if (edge && !(z == 0 && Math.abs(x - w / 2) <= 1)) put(level, chunk, x, 1, z, Blocks.BAMBOO_FENCE);
			}
		}
		// The pond, in the front half.
		int px = w / 4, pz = 2;
		fill(level, chunk, px - 1, 0, pz, px + 1, 0, pz + 2, Blocks.WATER.defaultBlockState());
		fill(level, chunk, px - 1, -1, pz, px + 1, -1, pz + 2, Blocks.CLAY);
		put(level, chunk, px - 1, 1, pz, Blocks.LILY_PAD);
		herb(level, chunk, ModBlocks.SPIRIT_LOTUS, px, 1, pz + 1, 50 + random.nextInt(500));
		// The plots.
		for (int x = 1; x < w - 1; x++) {
			for (int z = 1; z < d - 1; z++) {
				if (x == w / 2 || z % 5 == 0 || x >= px - 1 && x <= px + 1 && z >= pz && z <= pz + 2) continue;
				RandomSource spot = rand(x, z, 2);
				if (spot.nextInt(3) != 0) continue;
				int roll = spot.nextInt(40);
				Block plant;
				int age;
				if (roll == 0) {
					plant = ModBlocks.SPIRIT_GINSENG;
					age = 100 + (int) (Math.pow(spot.nextFloat(), 2) * 1900);
				} else if (roll < 6) {
					plant = spot.nextBoolean() ? ModBlocks.LINGZHI : ModBlocks.HUANGJING;
					age = 30 + spot.nextInt(400);
				} else {
					plant = ModBlocks.GINSENG;
					age = 20 + (int) (Math.pow(spot.nextFloat(), 2) * 800);
				}
				herb(level, chunk, plant, x, 1, z, age);
			}
		}
		// The Spirit Peach Tree, at the back, with its fruit.
		peachTree(level, chunk, w * 3 / 4, d - 4, random);
		put(level, chunk, w / 2, 1, d - 3, Blocks.POTTED_BAMBOO);
	}

	/** A Spirit Peach Tree (its heart keeps its age, see SpiritPeachTree), centuries old, with fruit hanging under its leaves. */
	private void peachTree(WorldGenLevel level, BoundingBox chunk, int cx, int cz, RandomSource random) {
		int treeAge = 200 + (int) (Math.pow(random.nextFloat(), 2) * 2800);
		for (int x = cx - 2; x <= cx + 2; x++) {
			for (int z = cz - 2; z <= cz + 2; z++) {
				put(level, chunk, x, 0, z, Blocks.GRASS_BLOCK);
				air(level, chunk, x, 1, z, x, 8, z);
			}
		}
		BlockPos heartPos = getWorldPos(cx, 1, cz);
		put(level, chunk, cx, 1, cz, ModBlocks.SPIRIT_PEACH_HEART);
		if (chunk.isInside(heartPos) && level.getBlockEntity(heartPos) instanceof SpiritPeachHeartBlockEntity heart) heart.addYears(treeAge - 1);
		int trunk = 4;
		fill(level, chunk, cx, 2, cz, cx, trunk, cz, Blocks.CHERRY_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
		BlockState leaf = ModBlocks.SPIRIT_PEACH_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		int top = trunk;
		for (int dy = -1; dy <= 2; dy++) {
			int r = dy <= 0 ? 2 : 1;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.abs(dx) == r && Math.abs(dz) == r && (r == 2 || dy == 2)) continue;
					if (dx == 0 && dz == 0 && dy <= 0) continue;
					put(level, chunk, cx + dx, top + dy, cz + dz, leaf);
				}
			}
		}
		// Fruit under the lowest leaves, never older than the tree.
		int fruit = 1 + random.nextInt(3);
		int[][] spots = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}, {-1, 1}, {1, -1}};
		for (int i = 0; i < fruit; i++) {
			int[] s = spots[(i * 2 + random.nextInt(2)) % spots.length];
			int fx = cx + s[0], fz = cz + s[1], fy = top - 2;
			BlockPos pos = getWorldPos(fx, fy, fz);
			if (!chunk.isInside(pos) || !at(level, chunk, fx, fy, fz).isAir()) continue;
			put(level, chunk, fx, fy, fz, ModBlocks.CULTIVATION_FRUIT);
			if (level.getBlockEntity(pos) instanceof CultivationFruitBlockEntity f) f.setWildAge(1 + random.nextInt(treeAge), level.getLevel().getGameTime());
		}
	}

	/**
	 * A disciple's dwelling: a little plaster house on a stone step, its mat (its home) in the middle, a chest, a lantern.
	 * An elder's house is larger, with a red silk mat, shelves and a better chest.
	 */
	private void dwelling(WorldGenLevel level, BoundingBox chunk, SectPalette p, boolean elder) {
		Frame f = frame(1, 1);
		int floor = 1, top = floor + 4;
		fill(level, chunk, f.x0, 0, f.z0, f.x1, 0, f.z1, p.foundation);
		for (int x = f.x0; x <= f.x1; x++) {
			for (int z = f.z0; z <= f.z1; z++) foundation(level, chunk, x, 0, z, p.foundation.defaultBlockState());
		}
		fill(level, chunk, f.x0, 1, f.z0, f.x1, 1, f.z1, p.foundation);
		interior(level, chunk, p, f.x0 + 1, f.z0 + 1, f.x1 - 1, f.z1 - 1, floor, top + 3);
		walls(level, chunk, p, f.x0, f.z0, f.x1, f.z1, floor + 1, top - 1, elder ? 3 : 2, 1, 2, false);
		put(level, chunk, f.cx(), floor, f.z0 - 1, p.baseStair(Direction.NORTH));
		fill(level, chunk, f.x0 + 1, top, f.z0 + 1, f.x1 - 1, top, f.z1 - 1, p.floor);
		roof(level, chunk, p, 0, 0, width() - 1, depth() - 1, top + 1, ridgeCourses(0, 0, width() - 1, depth() - 1), true);
		int[] mat = homeMat();
		put(level, chunk, mat[0], mat[1], mat[2], elder ? ModBlocks.RED_MEDITATION_MAT : ModBlocks.MEDITATION_MAT);
		chest(level, chunk, f.x1 - 1, floor + 1, f.z1 - 1, Direction.WEST, elder ? ELDER_LOOT : DWELLING_LOOT);
		lanternHanging(level, chunk, p, f.cx(), top - 1, f.cx() == f.x1 - 1 ? f.z0 + 1 : (f.z0 + f.z1) / 2 - 1);
		put(level, chunk, f.x0 + 1, floor + 1, f.z1 - 1, elder ? Blocks.BOOKSHELF : Blocks.POTTED_FERN);
		if (elder) {
			put(level, chunk, f.x0 + 1, floor + 2, f.z1 - 1, Blocks.POTTED_BAMBOO);
			candles(level, chunk, p, f.x1 - 1, floor + 2, f.z1 - 1, 2);
			wallBanner(level, chunk, p, f.cx(), floor + 3, f.z1 - 1, Direction.SOUTH);
		}
	}

	/** The dwelling's mat, {x, y, z}: its owner's home. */
	public int[] homeMat() {
		Frame f = frame(1, 1);
		return new int[] {f.cx(), 2, (f.z0 + f.z1) / 2};
	}

	/** A point just inside the gate's passage. */
	public int[] gateInside() {
		return new int[] {width() / 2, 1, depth() - 1};
	}
}
