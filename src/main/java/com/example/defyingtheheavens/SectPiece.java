package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BambooStalkBlock;
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
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.material.Fluids;

/**
 * One building of a sect (see {@link SectStructure}): the grounds themselves (grass, ways, footpaths, lanterns, trees and
 * bamboo, as SectLayout planned them), the gate, the torii on the side ways, the Formation plaza at the heart of the sect, the
 * meditation hall, the treasury, the main hall with the Sect Master's dais, the pagoda, herb gardens and houses.
 * <p>
 * Every building is drawn in its own frame, through StructurePiece's orientation: x across its front, z from its front
 * inward, y from the platform's paving (0) up. The orientation (the direction the building extends into from its entrance)
 * rotates and mirrors everything, block states included, into the world. All the randomness comes from the piece's own
 * seed, never the chunk's, so a building split across chunks comes out whole.
 */
public class SectPiece extends StructurePiece {
	/**
	 * TERRACE is the grounds; TOWER (the old walled sects' corner towers) is kept only so their saved pieces still load. The DEN_ kinds
	 * are a demon clan's (DenStructure, drawn by DenPieces): its head's den, an elder's, the disciples' burrow, the hoard (its
	 * treasury), a gate, the sparring ring, the dragons' crag (all of a dragon clan's under one), and the rest that stands about the
	 * grounds (boulders, pools, spires, a row of torii).
	 */
	public enum Kind {
		TERRACE, GATE, TOWER, PLAZA, MEDITATION_HALL, TREASURY, MAIN_HALL, PAGODA, GARDEN, DWELLING, ELDER_HOUSE, TORII, ALTAR, TRAINING,
		DEN_HALL, DEN_ELDER, DEN_BURROW, DEN_HOARD, DEN_GATE, DEN_RING, DEN_CRAG, DEN_FEATURE;

		/** One of a demon clan's (DenPieces). */
		public boolean isDen() { return name().startsWith("DEN_"); }
	}

	public static final ResourceLocation TREASURY_LOOT = DefyingTheHeavens.id("chests/sect_treasury");
	public static final ResourceLocation DWELLING_LOOT = DefyingTheHeavens.id("chests/sect_dwelling");
	public static final ResourceLocation ELDER_LOOT = DefyingTheHeavens.id("chests/sect_elder");
	public static final ResourceLocation LIBRARY_LOOT = DefyingTheHeavens.id("chests/sect_library");
	/** The master's own chests in the main hall: an elder's riches, and now and then a technique's manual (TechniqueManualItem). */
	public static final ResourceLocation MASTER_HALL_LOOT = DefyingTheHeavens.id("chests/sect_master_hall");

	/** How far a building's base may be filled down to meet the ground before it is left to stand on its own. */
	private static final int MAX_FOUNDATION = 12;

	private final Kind kind;
	final long seed;
	private final Alignment.Faction faction;
	final boolean upper;
	/** Per-kind data: the plaza's sect blueprint, the size of a hall, a garden's pond. */
	private final CompoundTag extra;
	/** Whose it is: a sect of men's (HUMAN), or a demon clan's den, built in its race's way (SectPalette#ofRace, DenPieces). */
	final Bloodline race;

	public SectPiece(Kind kind, BoundingBox box, Direction orientation, long seed, Alignment.Faction faction, boolean upper, CompoundTag extra) {
		this(kind, box, orientation, seed, faction, upper, extra, Bloodline.HUMAN);
	}

	public SectPiece(Kind kind, BoundingBox box, Direction orientation, long seed, Alignment.Faction faction, boolean upper, CompoundTag extra,
			Bloodline race) {
		super(ModStructures.SECT_PIECE, 0, box);
		this.kind = kind;
		this.seed = seed;
		this.faction = faction;
		this.upper = upper;
		this.extra = extra == null ? new CompoundTag() : extra;
		this.race = race == null ? Bloodline.HUMAN : race;
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
		this.race = tag.contains("Race") ? Bloodline.byId(tag.getString("Race")) : Bloodline.HUMAN;
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putString("Kind", kind.name());
		tag.putLong("Seed", seed);
		tag.putInt("Faction", faction.ordinal());
		tag.putBoolean("Upper", upper);
		tag.put("Extra", extra);
		if (race.isBeast()) tag.putString("Race", race.getId());
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

	/** The point of this piece's frame {x, z} at world column (x, z): the inverse of getWorldX and getWorldZ. */
	private int[] toLocal(int x, int z) {
		Direction o = getOrientation();
		if (o == null) return new int[] {x - boundingBox.minX(), z - boundingBox.minZ()};
		return switch (o) {
			case NORTH -> new int[] {x - boundingBox.minX(), boundingBox.maxZ() - z};
			case SOUTH -> new int[] {x - boundingBox.minX(), z - boundingBox.minZ()};
			case WEST -> new int[] {z - boundingBox.minZ(), boundingBox.maxX() - x};
			default -> new int[] {z - boundingBox.minZ(), x - boundingBox.minX()};
		};
	}

	/** For the grounds: whether world column {@code pos} lies on them or within {@code margin} of them (SectStructure#onGrounds). */
	public boolean onGrounds(BlockPos pos, int margin) {
		if (kind != Kind.TERRACE || !extra.contains("A")) return false;
		if (pos.getX() < boundingBox.minX() - margin || pos.getX() > boundingBox.maxX() + margin || pos.getZ() < boundingBox.minZ() - margin
				|| pos.getZ() > boundingBox.maxZ() + margin) return false;
		int a = extra.getInt("A"), b = extra.getInt("B");
		int[] local = toLocal(pos.getX(), pos.getZ());
		return SectLayout.outline(a, b, seed, local[0], local[1]) <= 1 + (double) margin / Math.min(a, b);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox chunk,
			ChunkPos chunkPos, BlockPos pivot) {
		SectPalette p = race.isBeast() ? SectPalette.ofRace(race, upper) : SectPalette.of(faction, upper);
		if (kind.isDen()) {
			DenPieces.draw(this, level, chunk, p);
			return;
		}
		switch (kind) {
			case TERRACE -> terrace(level, chunk, p);
			case GATE -> gate(level, chunk, p);
			case TORII -> torii(level, chunk, p);
			case TOWER -> { } // the grounds have no wall now
			case PLAZA -> plaza(level, chunk, p);
			case MEDITATION_HALL -> meditationHall(level, chunk, p);
			case TREASURY -> treasury(level, chunk, p);
			case MAIN_HALL -> mainHall(level, chunk, p);
			case PAGODA -> pagoda(level, chunk, p);
			case GARDEN -> garden(level, chunk, p);
			case DWELLING -> dwelling(level, chunk, p, false);
			case ELDER_HOUSE -> dwelling(level, chunk, p, true);
			case ALTAR -> altarOfHeaven(level, chunk, p);
			case TRAINING -> trainingGround(level, chunk, p);
		}
	}

	// =====================================================================================================================
	// Helpers
	// =====================================================================================================================

	/** A random source for one spot of this piece: the same on every chunk that draws it. */
	RandomSource rand(int x, int z, int salt) {
		return RandomSource.create(seed ^ (x * 341873128712L + z * 132897987541L + salt * 42317861L));
	}

	/**
	 * Places a block in the piece's frame, turned with the piece. Blocks whose shape depends on their neighbours (stairs,
	 * fences, walls, bars, panes) are settled once the whole chunk is generated, so they join up across the building.
	 */
	void put(WorldGenLevel level, BoundingBox chunk, int x, int y, int z, BlockState state) {
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

	void put(WorldGenLevel level, BoundingBox chunk, int x, int y, int z, Block block) {
		put(level, chunk, x, y, z, block.defaultBlockState());
	}

	void fill(WorldGenLevel level, BoundingBox chunk, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
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

	void fill(WorldGenLevel level, BoundingBox chunk, int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
		fill(level, chunk, x0, y0, z0, x1, y1, z1, block.defaultBlockState());
	}

	void air(WorldGenLevel level, BoundingBox chunk, int x0, int y0, int z0, int x1, int y1, int z1) {
		fill(level, chunk, x0, y0, z0, x1, y1, z1, Blocks.AIR.defaultBlockState());
	}

	BlockState at(WorldGenLevel level, BoundingBox chunk, int x, int y, int z) {
		return getBlock(level, x, y, z, chunk);
	}

	/** Fills below {@code y} down to the ground (at most {@link #MAX_FOUNDATION}), so nothing floats over a dip. */
	void foundation(WorldGenLevel level, BoundingBox chunk, int x, int y, int z, BlockState state) {
		BlockPos.MutableBlockPos pos = getWorldPos(x, y - 1, z);
		if (!chunk.isInside(pos)) return;
		for (int i = 0; i < MAX_FOUNDATION && pos.getY() > level.getMinBuildHeight(); i++) {
			BlockState here = level.getBlockState(pos);
			if (!(here.isAir() || here.canBeReplaced() || !here.getFluidState().isEmpty())) break;
			level.setBlock(pos, state, Block.UPDATE_CLIENTS);
			pos.move(Direction.DOWN);
		}
	}

	void pillar(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int y0, int y1, int z) {
		fill(level, chunk, x, y0, z, x, y1, z, p.pillar(Direction.Axis.Y));
	}

	void lanternHanging(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int y, int z) {
		put(level, chunk, x, y, z, p.lantern.defaultBlockState().setValue(LanternBlock.HANGING, true));
	}

	void lanternStanding(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int y, int z) {
		put(level, chunk, x, y, z, p.lantern.defaultBlockState().setValue(LanternBlock.HANGING, false));
	}

	/** A stone lantern: a post with the lantern standing on it (no cap: a slab on top read as clutter). */
	void stoneLantern(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int y, int z) {
		put(level, chunk, x, y, z, p.baseWall);
		lanternStanding(level, chunk, p, x, y + 1, z);
	}

	void candles(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int y, int z, int count) {
		put(level, chunk, x, y, z, p.candle.defaultBlockState().setValue(CandleBlock.CANDLES, Math.max(1, Math.min(4, count))).setValue(CandleBlock.LIT, true));
	}

	void chest(WorldGenLevel level, BoundingBox chunk, int x, int y, int z, Direction facing, ResourceLocation loot) {
		BlockPos pos = getWorldPos(x, y, z);
		if (!chunk.isInside(pos)) return;
		put(level, chunk, x, y, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
		RandomizableContainerBlockEntity.setLootTable(level, rand(x, z, y), pos, loot);
	}

	/** A Spirit Pedestal showing {@code treasure}. */
	void pedestal(WorldGenLevel level, BoundingBox chunk, int x, int y, int z, ItemStack treasure) {
		BlockPos pos = getWorldPos(x, y, z);
		if (!chunk.isInside(pos)) return;
		put(level, chunk, x, y, z, ModBlocks.SPIRIT_PEDESTAL);
		if (level.getBlockEntity(pos) instanceof SpiritPedestalBlockEntity pedestal) pedestal.setFruit(treasure);
	}

	void wallBanner(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int y, int z, Direction facing) {
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

	private void roof(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int z0, int x1, int z1, int y, int courses, boolean ridge) {
		roof(level, chunk, p, x0, z0, x1, z1, y, courses, ridge, false);
	}

	/**
	 * A hipped roof of tiles over the rectangle x0..x1, z0..z1 (eaves included), its lowest course at {@code y}: each course
	 * one block in and one block up, to a ridge of the trim colour whose ends lift. The four lowest corners turn up in a flick
	 * of the trim colour, and with {@code trimEave} (the grander halls) the whole eave is edged in it. {@code courses} limits
	 * how many courses are laid (for a lower eave under an upper storey); a finished roof is closed along its ridge.
	 */
	private void roof(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int z0, int x1, int z1, int y, int courses, boolean ridge,
			boolean trimEave) {
		for (int k = 0; k < courses; k++) {
			int ax0 = x0 + k, ax1 = x1 - k, az0 = z0 + k, az1 = z1 - k, ay = y + k;
			if (ax0 > ax1 || az0 > az1) return;
			if (ridge && (ax1 - ax0 <= 1 || az1 - az0 <= 1)) {
				// The ridge: a line of the trim along its length, its two ends raised.
				fill(level, chunk, ax0, ay, az0, ax1, ay, az1, p.roofTrim);
				for (int[] end : new int[][] {{ax0, az0}, {ax1, az1}, {ax1, az0}, {ax0, az1}}) put(level, chunk, end[0], ay + 1, end[1], p.trimSlab(false));
				return;
			}
			boolean trim = k == 0 && trimEave;
			for (int x = ax0; x <= ax1; x++) {
				put(level, chunk, x, ay, az0, trim ? p.trimStair(Direction.NORTH, false) : p.roofStair(Direction.NORTH, false));
				put(level, chunk, x, ay, az1, trim ? p.trimStair(Direction.SOUTH, false) : p.roofStair(Direction.SOUTH, false));
			}
			for (int z = az0 + 1; z < az1; z++) {
				put(level, chunk, ax0, ay, z, trim ? p.trimStair(Direction.EAST, false) : p.roofStair(Direction.EAST, false));
				put(level, chunk, ax1, ay, z, trim ? p.trimStair(Direction.WEST, false) : p.roofStair(Direction.WEST, false));
			}
			if (k == 0) {
				// Upturned eave corners.
				for (int[] c : new int[][] {{ax0, az0}, {ax1, az0}, {ax0, az1}, {ax1, az1}}) {
					put(level, chunk, c[0], ay, c[1], p.roofTrim);
					put(level, chunk, c[0], ay + 1, c[1], p.trimSlab(false));
				}
			}
		}
		if (ridge) {
			// Ran out of courses: close the top flat.
			int ax0 = x0 + courses, ax1 = x1 - courses, az0 = z0 + courses, az1 = z1 - courses;
			if (ax0 <= ax1 && az0 <= az1) fill(level, chunk, ax0, y + courses - 1, az0, ax1, y + courses - 1, az1, p.roof);
		}
	}

	/**
	 * Closes the gap between a building's walls and the roof over them. The walls round x0..x1, z0..z1 end at {@code wallTop}
	 * (their beam), but a hipped roof over rx0..rz1 climbs a course for each block in from its eave, so over the walls it sits a
	 * course or two higher, and a slit of sky showed between them all the way round (the roof seemed to float). This builds the
	 * walls up to meet it: plaster, pillars at the corners.
	 */
	private void closeEaves(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int z0, int x1, int z1, int wallTop,
			int rx0, int rz0, int rx1, int rz1, int roofY, int courses) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean edgeX = x == x0 || x == x1, edgeZ = z == z0 || z == z1;
				if (!edgeX && !edgeZ) continue;
				int k = Math.min(Math.min(x - rx0, rx1 - x), Math.min(z - rz0, rz1 - z));
				if (k < 0) continue;
				int roofAt = roofY + Math.min(k, courses - 1);
				BlockState fill = edgeX && edgeZ ? p.pillar(Direction.Axis.Y) : p.wall.defaultBlockState();
				for (int y = wallTop + 1; y < roofAt; y++) put(level, chunk, x, y, z, fill);
			}
		}
	}

	/** Courses a hipped roof over this rectangle needs to reach its ridge. */
	private static int ridgeCourses(int x0, int z0, int x1, int z1) {
		return (Math.min(x1 - x0, z1 - z0)) / 2 + 1;
	}

	/**
	 * The walls of a building between x0..x1, z0..z1, from {@code y0} to {@code y1}: pillars at the corners and about every
	 * {@code bay} blocks between, set evenly and mirrored about the middle (the front and back with a bay in the middle, so the
	 * doorway stands centred between two pillars, never beside one on one side only), plain plaster between on a stone foot (no
	 * fence-post lattice; a {@code solid} building, the treasury, has small barred windows), the doorway ({@code door} wide,
	 * {@code doorHeight} high) in the middle of the front, and a timber beam with bronze brackets above the pillars all round.
	 */
	private void walls(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int z0, int x1, int z1, int y0, int y1, int bay, int door,
			int doorHeight, boolean solid) {
		int cx = (x0 + x1) / 2;
		int[] across = pillarSpots(x1 - x0, bay, true), along = pillarSpots(z1 - z0, bay, false);
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean edgeX = x == x0 || x == x1, edgeZ = z == z0 || z == z1;
				if (!edgeX && !edgeZ) continue;
				int[] spots = edgeZ ? across : along;
				int at = edgeZ ? x - x0 : z - z0;
				boolean post = edgeX && edgeZ || java.util.Arrays.binarySearch(spots, at) >= 0;
				if (post) {
					pillar(level, chunk, p, x, y0, y1, z);
					put(level, chunk, x, y1 + 1, z, p.accent);
					continue;
				}
				put(level, chunk, x, y0, z, p.wallBase);
				fill(level, chunk, x, y0 + 1, z, x, y1, z, p.wall);
				// The treasury's barred windows, in the middle of each bay.
				if (solid && y1 - y0 >= 3 && inBayMiddle(spots, at)) put(level, chunk, x, y0 + 2, z, Blocks.IRON_BARS);
				put(level, chunk, x, y1 + 1, z, p.beam(edgeZ ? Direction.Axis.X : Direction.Axis.Z));
			}
		}
		// The doorway, in the front wall.
		if (door > 0) air(level, chunk, cx - door / 2, y0, z0, cx + door / 2, y0 + doorHeight - 1, z0);
	}

	/**
	 * Where a wall {@code length} blocks long (corner to corner) has its pillars: at both ends and evenly between, about
	 * {@code bay} apart, mirrored about the middle; with {@code middleBay} (the front and back) an odd number of bays, so the
	 * middle of the wall, where the door is, falls in a bay's middle rather than on a pillar.
	 */
	static int[] pillarSpots(int length, int bay, boolean middleBay) {
		int bays = Math.max(1, Math.round((float) length / bay));
		if (middleBay && bays % 2 == 0) bays += (float) length / (bays + 1) >= 3 ? 1 : -1;
		bays = Math.max(1, bays);
		int[] spots = spread(length, bays);
		// A middle bay too narrow to hold the door (a short wall in many bays): fewer, wider bays.
		while (middleBay && bays > 1 && java.util.Arrays.binarySearch(spots, length / 2) >= 0) spots = spread(length, bays -= 2);
		return spots;
	}

	/** Pillars at both ends of a wall {@code length} long and {@code bays - 1} between, rounded alike from either end. */
	private static int[] spread(int length, int bays) {
		int[] spots = new int[bays + 1];
		for (int i = 0; i <= bays; i++) {
			spots[i] = 2 * i <= bays ? (2 * i * length + bays) / (2 * bays) : length - (2 * (bays - i) * length + bays) / (2 * bays);
		}
		return spots;
	}

	/** Whether {@code at} is in the middle of its bay between the pillars at {@code spots} (a window's place). */
	private static boolean inBayMiddle(int[] spots, int at) {
		for (int i = 0; i + 1 < spots.length; i++) {
			if (at <= spots[i] || at >= spots[i + 1]) continue;
			double middle = (spots[i] + spots[i + 1]) / 2.0;
			return Math.abs(at - middle) <= (spots[i + 1] - spots[i] > 3 ? 1 : 0.5);
		}
		return false;
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

	/** How far a tree's crown may spread from its trunk, so the grounds draw the crowns of trees just across a chunk's edge. */
	private static final int CROWN = 3;

	/**
	 * The grounds, from SectLayout's map: no wall and no paving, but the open ground levelled (cleared above, filled with earth
	 * below), the paved ways with their edging, earthen footpaths to the doors, stone lanterns along the ways, and trees and
	 * bamboo on the grass between the buildings. The open ground keeps the land's own floor, column by column: what was on top
	 * of each before it was levelled (podzol and coarse dirt in an old taiga, grass on the plains, the stone of a rocky peak),
	 * wherever that was, raised or lowered to the grounds' level; so a sect sits in its land rather than on a lawn laid over
	 * it. Only where there was no such ground (water) is it grass.
	 */
	private void terrace(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = width(), d = depth();
		byte[] map = extra.getByteArray("Map");
		if (map.length != w * d) return; // a walled sect from before the grounds were laid out like this: no plan to draw
		BlockState earth = Blocks.DIRT.defaultBlockState(), stone = p.foundation.defaultBlockState();
		// The ground first, column by column.
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				byte code = map[x + z * w];
				if (code == SectLayout.OUT || !chunk.isInside(getWorldPos(x, 0, z))) continue;
				// The land's floor here, read before the levelling takes it away.
				BlockState floor = landFloor(level, chunk, x, z);
				boolean paved = code == SectLayout.PATH || code == SectLayout.PATH_EDGE || code == SectLayout.PLAZA;
				foundation(level, chunk, x, 0, z, paved ? stone : earth);
				air(level, chunk, x, 1, z, x, 22, z);
				switch (code) {
					case SectLayout.PATH -> put(level, chunk, x, 0, z, p.path);
					case SectLayout.PATH_EDGE, SectLayout.PLAZA -> put(level, chunk, x, 0, z, p.foundation);
					case SectLayout.FOOTPATH -> {
						int roll = rand(x, z, 3).nextInt(10);
						put(level, chunk, x, 0, z, roll < 7 ? Blocks.DIRT_PATH : roll < 9 ? Blocks.COARSE_DIRT : p.paving);
					}
					default -> put(level, chunk, x, 0, z, floor != null ? floor : Blocks.GRASS_BLOCK.defaultBlockState());
				}
			}
		}
		// Then what stands on it, out to a crown's spread beyond the chunk.
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				byte code = map[x + z * w];
				if (code != SectLayout.LANTERN && code != SectLayout.TREE && code != SectLayout.BAMBOO) continue;
				BlockPos pos = getWorldPos(x, 0, z);
				if (pos.getX() < chunk.minX() - CROWN || pos.getX() > chunk.maxX() + CROWN || pos.getZ() < chunk.minZ() - CROWN
						|| pos.getZ() > chunk.maxZ() + CROWN) continue;
				switch (code) {
					case SectLayout.LANTERN -> stoneLantern(level, chunk, p, x, 1, z);
					case SectLayout.TREE -> tree(level, chunk, p, x, z, rand(x, z, 7));
					// A den's undergrowth where a sect has bamboo (DenPieces#brush).
					default -> {
						if (race.isBeast()) DenPieces.brush(this, level, chunk, p, x, z, rand(x, z, 9));
						else bamboo(level, chunk, x, z, rand(x, z, 9));
					}
				}
			}
		}
	}

	/**
	 * How far above and below the grounds' level a column is searched for the land's top: below as deep as the grounds are ever
	 * set into a dip, above past the tallest knoll the levelling cuts away (and a tree's crown over it).
	 */
	private static final int FLOOR_ABOVE = 40, FLOOR_BELOW = 14;

	/**
	 * The land's floor in column x, z: the first block down that the land is made of ({@link #asFloor}), under whatever stands
	 * or grows on it (plants, trees, mushrooms, snow cover, moss carpet); null if water comes first, or no such block is found.
	 */
	private BlockState landFloor(WorldGenLevel level, BoundingBox chunk, int x, int z) {
		for (int y = FLOOR_ABOVE; y >= -FLOOR_BELOW; y--) {
			BlockState state = at(level, chunk, x, y, z);
			if (!state.getFluidState().isEmpty()) return null;
			BlockState floor = asFloor(state);
			if (floor != null) return floor;
		}
		return null;
	}

	/**
	 * The floor the grounds lay for a block the land's top is made of: earth of any kind, sand, gravel, snow, or the land's
	 * own stone, as it is (but plain, without what a buried or snowed-on block carries); the snow's ice and powder as packed
	 * snow, firm to walk on. Null for anything else.
	 */
	private static BlockState asFloor(BlockState state) {
		if (state.is(Blocks.SUSPICIOUS_SAND)) return Blocks.SAND.defaultBlockState();
		if (state.is(Blocks.SUSPICIOUS_GRAVEL)) return Blocks.GRAVEL.defaultBlockState();
		if (state.is(Blocks.POWDER_SNOW) || state.is(BlockTags.ICE)) return Blocks.SNOW_BLOCK.defaultBlockState();
		boolean ground = state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.BASE_STONE_OVERWORLD)
				|| state.is(Blocks.GRAVEL) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.CALCITE) || state.is(Blocks.BASALT)
				|| state.is(Blocks.BLACKSTONE) || state.is(Blocks.OBSIDIAN) || state.is(ModBlocks.JADE_STONE);
		return ground ? state.getBlock().defaultBlockState() : null;
	}

	/**
	 * A tree of the palette's wood and leaves (persistent, so they never decay): a blossom tree, its crown spreading wide and
	 * hanging at the rim like a cherry's, or for the spruce of the neutral sects a tall tiered fir.
	 */
	void tree(WorldGenLevel level, BoundingBox chunk, SectPalette p, int cx, int cz, RandomSource random) {
		BlockState log = p.trunk.defaultBlockState();
		if (log.hasProperty(RotatedPillarBlock.AXIS)) log = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
		BlockState leaf = p.leaves.defaultBlockState();
		if (leaf.hasProperty(LeavesBlock.PERSISTENT)) leaf = leaf.setValue(LeavesBlock.PERSISTENT, true);
		if (p.leaves == Blocks.SPRUCE_LEAVES) {
			// A cone of boughs from a little above the ground to a point over the trunk, every other one drawn in.
			int height = 8 + random.nextInt(4);
			fill(level, chunk, cx, 1, cz, cx, height, cz, log);
			put(level, chunk, cx, height + 1, cz, leaf);
			put(level, chunk, cx, height + 2, cz, leaf);
			for (int y = 3; y <= height + 1; y++) {
				int t = height + 1 - y;
				int r = Math.max(1, Math.min(3, 1 + t / 2) - (t % 2 == 1 ? 1 : 0));
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						if (dx == 0 && dz == 0 || dx * dx + dz * dz > r * r + 1) continue;
						put(level, chunk, cx + dx, y, cz + dz, leaf);
					}
				}
			}
			return;
		}
		int height = 4 + random.nextInt(3);
		fill(level, chunk, cx, 1, cz, cx, height, cz, log);
		// A bough or two leaning out from the top of the trunk.
		int lean = random.nextInt(4);
		int bx = cx + (lean == 0 ? 1 : lean == 1 ? -1 : 0), bz = cz + (lean == 2 ? 1 : lean == 3 ? -1 : 0);
		put(level, chunk, bx, height, bz, log);
		for (int dy = -1; dy <= 2; dy++) {
			int r = dy == 2 ? 1 : dy == -1 ? 2 : 3;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					double reach = Math.sqrt(dx * dx + dz * dz);
					if (reach > r + 0.3 || reach > r - 0.7 && random.nextInt(3) == 0) continue;
					if (dx == 0 && dz == 0 && dy <= 0) continue;
					BlockPos pos = getWorldPos(cx + dx, height + dy, cz + dz);
					if (!chunk.isInside(pos) || !level.getBlockState(pos).isAir()) continue;
					put(level, chunk, cx + dx, height + dy, cz + dz, leaf);
				}
			}
		}
		// Blossom hanging from the rim of the crown.
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				double reach = Math.sqrt(dx * dx + dz * dz);
				if (reach < 2.2 || reach > 3.3 || random.nextInt(3) != 0) continue;
				BlockPos pos = getWorldPos(cx + dx, height - 1, cz + dz);
				if (!chunk.isInside(pos) || !level.getBlockState(pos).isAir()) continue;
				put(level, chunk, cx + dx, height - 1, cz + dz, leaf);
			}
		}
	}

	/** A stalk of bamboo, 5 to 11 tall, leafy at the top. */
	void bamboo(WorldGenLevel level, BoundingBox chunk, int x, int z, RandomSource random) {
		int height = 5 + random.nextInt(7);
		BlockState stalk = Blocks.BAMBOO.defaultBlockState().setValue(BambooStalkBlock.AGE, 1);
		for (int y = 1; y <= height; y++) {
			BambooLeaves leaves = y == height || y == height - 1 ? BambooLeaves.LARGE : y == height - 2 ? BambooLeaves.SMALL : BambooLeaves.NONE;
			put(level, chunk, x, y, z, stalk.setValue(BambooStalkBlock.LEAVES, leaves).setValue(BambooStalkBlock.STAGE, y == height ? 1 : 0));
		}
	}

	/** A pillar standing free on the ground: a stone footing under it and stone feet round its foot. */
	void post(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int z, int top) {
		put(level, chunk, x, 0, z, p.foundation);
		pillar(level, chunk, p, x, 1, top, z);
		put(level, chunk, x - 1, 1, z, p.baseStair(Direction.EAST));
		put(level, chunk, x + 1, 1, z, p.baseStair(Direction.WEST));
		put(level, chunk, x, 1, z - 1, p.baseStair(Direction.NORTH));
		put(level, chunk, x, 1, z + 1, p.baseStair(Direction.SOUTH));
	}

	/** A tie beam from x0 to x1 at height {@code y} over row z, through (not over) the pillars standing at {@code pillars}. */
	void tieBeam(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int x1, int y, int z, int... pillars) {
		for (int x = x0; x <= x1; x++) {
			final int at = x;
			if (java.util.Arrays.stream(pillars).noneMatch(px -> px == at)) put(level, chunk, x, y, z, p.beam(Direction.Axis.X));
		}
	}

	/**
	 * A lintel of the trim colour from x0 to x1 at height {@code y} over row z, capped with slabs, its ends ({@code flareLeft},
	 * {@code flareRight}) reaching one further and sweeping up: the crowning beam of a torii or a paifang.
	 */
	void lintel(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int x1, int y, int z, boolean flareLeft, boolean flareRight) {
		fill(level, chunk, x0, y, z, x1, y, z, p.roofTrim);
		fill(level, chunk, x0, y + 1, z, x1, y + 1, z, p.trimSlab(false));
		if (flareLeft) {
			put(level, chunk, x0 - 1, y, z, p.trimSlab(true));
			put(level, chunk, x0 - 1, y + 1, z, p.trimStair(Direction.WEST, false));
		}
		if (flareRight) {
			put(level, chunk, x1 + 1, y, z, p.trimSlab(true));
			put(level, chunk, x1 + 1, y + 1, z, p.trimStair(Direction.EAST, false));
		}
	}

	/**
	 * The gate: a paifang of three bays over the foot of the way in, free-standing (the grounds have no wall). The tall middle
	 * bay bears the sect's plaque and lanterns, its pillars the sect's banners; the lower side bays flank it.
	 */
	private void gate(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = width(), d = depth();
		int cx = w / 2, z = d / 2;
		air(level, chunk, 0, 1, 0, w - 1, 12, d - 1);
		for (int x : new int[] {cx - 3, cx + 3}) post(level, chunk, p, x, z, 9);
		for (int x : new int[] {cx - 7, cx + 7}) post(level, chunk, p, x, z, 6);
		// The middle bay: the tie beam between the pillars, the plaque, the crowning lintel.
		tieBeam(level, chunk, p, cx - 2, cx + 2, 8, z);
		put(level, chunk, cx, 9, z, p.accent);
		lintel(level, chunk, p, cx - 5, cx + 5, 10, z, true, true);
		lanternHanging(level, chunk, p, cx - 2, 7, z);
		lanternHanging(level, chunk, p, cx + 2, 7, z);
		for (int x : new int[] {cx - 3, cx + 3}) {
			wallBanner(level, chunk, p, x, 6, z - 1, Direction.SOUTH);
			wallBanner(level, chunk, p, x, 6, z + 1, Direction.NORTH);
		}
		// The side bays, their lintels butting against the middle pillars and sweeping up at the outer ends.
		tieBeam(level, chunk, p, cx - 6, cx - 4, 5, z);
		tieBeam(level, chunk, p, cx + 4, cx + 6, 5, z);
		put(level, chunk, cx - 8, 5, z, p.beam(Direction.Axis.X));
		put(level, chunk, cx + 8, 5, z, p.beam(Direction.Axis.X));
		lintel(level, chunk, p, cx - 8, cx - 4, 7, z, true, false);
		lintel(level, chunk, p, cx + 4, cx + 8, 7, z, false, true);
	}

	/** A torii where a side way leaves the grounds: two pillars, a tie beam and an upswept lintel with a plaque between. */
	private void torii(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = width(), d = depth();
		int cx = w / 2, z = d / 2;
		air(level, chunk, 0, 1, 0, w - 1, 9, d - 1);
		for (int x : new int[] {cx - 3, cx + 3}) post(level, chunk, p, x, z, 6);
		tieBeam(level, chunk, p, cx - 4, cx + 4, 5, z, cx - 3, cx + 3);
		put(level, chunk, cx, 6, z, p.accent);
		lintel(level, chunk, p, cx - 4, cx + 4, 7, z, true, true);
		lanternHanging(level, chunk, p, cx - 1, 4, z);
		lanternHanging(level, chunk, p, cx + 1, 4, z);
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
		int courses = ridgeCourses(0, 0, width() - 1, depth() - 1);
		roof(level, chunk, p, 0, 0, width() - 1, depth() - 1, top + 1, courses, true);
		closeEaves(level, chunk, p, f.x0, f.z0, f.x1, f.z1, top, 0, 0, width() - 1, depth() - 1, top + 1, courses);
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
		int floor = 2, top = floor + 6; // walls five high, so it stands up under its roof rather than squatting
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
		int courses = ridgeCourses(0, 1, width() - 1, depth() - 1);
		roof(level, chunk, p, 0, 1, width() - 1, depth() - 1, top + 1, courses, true);
		closeEaves(level, chunk, p, f.x0, f.z0, f.x1, f.z1, top, 0, 1, width() - 1, depth() - 1, top + 1, courses);
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
	ItemStack treasure(RandomSource random, int index) {
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
	 * of pillars hung with the sect's banners lead down a bordered carpet to the Sect Master's dais: two steps up to a red silk
	 * mat before a carved screen, lanterns on posts either side, lanterns hung low on chains. Outside, a plaque over the door, lanterns and potted plants on the
	 * porch, flag poles at the terrace's corners, stone guardians at the foot of the steps and two great smoking braziers.
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
		roof(level, chunk, p, ex0, ez0, ex1, ez1, top + 1, 4, false, true);
		closeEaves(level, chunk, p, f.x0, hz0, f.x1, f.z1, top, ex0, ez0, ex1, ez1, top + 1, 4);
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
				}
			}
		}
		int upper = ridgeCourses(cx0 - 2, cz0 - 2, cx1 + 2, cz1 + 2);
		roof(level, chunk, p, cx0 - 2, cz0 - 2, cx1 + 2, cz1 + 2, top + 5, upper, true, true);
		closeEaves(level, chunk, p, cx0, cz0, cx1, cz1, top + 4, cx0 - 2, cz0 - 2, cx1 + 2, cz1 + 2, top + 5, upper);
		// The carpet, bordered, and the dais.
		for (int z = hz0; z < f.z1 - 5; z++) {
			fill(level, chunk, f.cx() - 1, floor + 1, z, f.cx() + 1, floor + 1, z, p.carpet);
			put(level, chunk, f.cx() - 2, floor + 1, z, p.carpetAlt);
			put(level, chunk, f.cx() + 2, floor + 1, z, p.carpetAlt);
		}
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
		// Lanterns on stone posts at the front of the upper dais, potted plants at the back corners of the lower.
		for (int x : new int[] {f.cx() - 3, f.cx() + 3}) {
			put(level, chunk, x, floor + 3, dz + 1, p.baseWall);
			lanternStanding(level, chunk, p, x, floor + 4, dz + 1);
		}
		put(level, chunk, f.cx() - 4, floor + 2, f.z1 - 1, Blocks.POTTED_BAMBOO);
		put(level, chunk, f.cx() + 4, floor + 2, f.z1 - 1, Blocks.POTTED_AZALEA);
		candles(level, chunk, p, f.cx() - 2, floor + 3, f.z1 - 2, 4);
		candles(level, chunk, p, f.cx() + 2, floor + 3, f.z1 - 2, 4);
		// The sect's banners on the pillars, facing the aisle.
		for (int z = hz0 + 3; z <= f.z1 - 4; z += 4) {
			wallBanner(level, chunk, p, f.cx() - 4, floor + 5, z, Direction.EAST);
			wallBanner(level, chunk, p, f.cx() + 4, floor + 5, z, Direction.WEST);
		}
		// Lanterns hung low on chains down the hall and over the aisle, and on posts along the side walls; chests of the
		// master's own behind the screen's wings.
		for (int z = hz0 + 2; z <= f.z1 - 4; z += 4) {
			chainedLantern(level, chunk, p, f.cx() - 5, top - 1, floor + 5, z + 2);
			chainedLantern(level, chunk, p, f.cx() + 5, top - 1, floor + 5, z + 2);
		}
		chainedLantern(level, chunk, p, f.cx(), top - 1, floor + 5, hz0 + 2);
		stoneLantern(level, chunk, p, f.x0 + 1, floor + 1, dz);
		stoneLantern(level, chunk, p, f.x1 - 1, floor + 1, dz);
		chest(level, chunk, f.x0 + 1, floor + 1, f.z1 - 1, Direction.SOUTH, MASTER_HALL_LOOT);
		chest(level, chunk, f.x1 - 1, floor + 1, f.z1 - 1, Direction.SOUTH, MASTER_HALL_LOOT);

		// Outside: a plaque over the door; on the porch, lanterns and potted plants; flag poles bearing the sect's banners at the
		// terrace's front corners; at the foot of the steps a stone guardian either side, and beyond them two great braziers
		// sending up smoke.
		fill(level, chunk, f.cx() - 2, top - 1, hz0, f.cx() + 2, top - 1, hz0, p.roofTrim);
		fill(level, chunk, f.cx() - 1, top - 1, hz0, f.cx() + 1, top - 1, hz0, p.accent);
		for (int s = -1; s <= 1; s += 2) {
			stoneLantern(level, chunk, p, f.cx() + s * 6, floor + 1, hz0 - 1);
			put(level, chunk, f.cx() + s * 3, floor + 1, hz0 - 1, s < 0 ? Blocks.POTTED_BAMBOO : Blocks.POTTED_AZALEA);
			int poleX = s < 0 ? f.x0 - 2 : f.x1 + 2;
			pillar(level, chunk, p, poleX, floor + 1, top, f.z0);
			wallBanner(level, chunk, p, poleX, top - 3, f.z0 - 1, Direction.SOUTH);
			// The guardian: a pedestal, a body of the accent, a head turned to the steps' foot.
			int gx = f.cx() + s * 5;
			put(level, chunk, gx, 1, 4, p.foundation);
			put(level, chunk, gx, 2, 4, p.accent);
			put(level, chunk, gx, 3, 4, p.baseStair(Direction.NORTH));
			bigBrazier(level, chunk, p, f.cx() + s * 8, 1, 3);
		}
	}

	/** A brazier: a lit campfire, of soul fire where the sect burns it. */
	private static BlockState brazier(SectPalette p) {
		return (p.soulFire ? Blocks.SOUL_CAMPFIRE : Blocks.CAMPFIRE).defaultBlockState();
	}

	/** A lantern hung on a chain from {@code from} down to {@code y}. */
	private void chainedLantern(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int from, int y, int z) {
		fill(level, chunk, x, y + 1, z, x, from, z, Blocks.CHAIN.defaultBlockState());
		lanternHanging(level, chunk, p, x, y, z);
	}

	/** A great brazier three blocks across on a stone base, (x, z) its middle: a fire in a bowl of the trim, smoke rising. */
	private void bigBrazier(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int y, int z) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				put(level, chunk, x + dx, y, z + dz, p.foundation);
				foundation(level, chunk, x + dx, y, z + dz, p.foundation.defaultBlockState());
			}
		}
		put(level, chunk, x, y + 1, z, brazier(p));
		put(level, chunk, x - 1, y + 1, z, p.trimStair(Direction.WEST, false));
		put(level, chunk, x + 1, y + 1, z, p.trimStair(Direction.EAST, false));
		put(level, chunk, x, y + 1, z - 1, p.trimStair(Direction.SOUTH, false));
		put(level, chunk, x, y + 1, z + 1, p.trimStair(Direction.NORTH, false));
		for (int[] c : new int[][] {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) put(level, chunk, x + c[0], y + 1, z + c[1], p.trimSlab(false));
	}

	/** The Sect Master's mat on the dais, {x, y, z} in the main hall's frame. */
	public int[] masterSeat() {
		int m = 3;
		Frame f = new Frame(m, m + 3, width() - 1 - m, depth() - 1 - m);
		return new int[] {f.cx(), 3 + 3, f.z1 - 3};
	}

	/** How far in each storey of the pagoda is set from the one below: a block every third storey. */
	private static int pagodaInset(int storey) {
		return storey / 3;
	}

	/** How many of its {@code Tiers} storeys the pagoda raises: none narrower than three blocks inside. */
	private int storeys() {
		int tiers = extra.getInt("Tiers"), w = width(), m = 2, n = 0;
		while (n < tiers && w - 1 - 2 * m - 2 * pagodaInset(n) >= 4) n++;
		return Math.max(1, n);
	}

	/**
	 * The pagoda: a tower of square storeys, a little narrower every third floor and each ringed by its own eave, crowned
	 * with a spire. Every storey but the top is the sect's library (bookshelves down both side walls with candles burning on
	 * them, a lectern where there is room, the library chest on the ground floor); a ladder climbs through them all to a quiet
	 * meditation chamber at the top, lit by its lantern and candles. Everything is laid for the storeys actually raised, so
	 * the top chamber and its mat always stand on the top floor.
	 */
	private void pagoda(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int storeys = storeys();
		int w = width();
		int m = 2;
		fill(level, chunk, m - 1, 0, m - 1, w - m, 0, w - m, p.foundation);
		for (int x = m - 1; x <= w - m; x++) {
			for (int z = m - 1; z <= w - m; z++) foundation(level, chunk, x, 0, z, p.foundation.defaultBlockState());
		}
		int topInset = pagodaInset(storeys - 1);
		int tx0 = m + topInset, tx1 = w - 1 - m - topInset;
		int ladderX = w / 2, ladderZ = tx1 - 1; // against the back wall of even the narrowest storey
		int base = 1;
		for (int tier = 0; tier < storeys; tier++) {
			int inset = pagodaInset(tier);
			int x0 = m + inset, x1 = w - 1 - m - inset;
			fill(level, chunk, x0 + 1, base - 1, x0 + 1, x1 - 1, base - 1, x1 - 1, p.floor);
			air(level, chunk, x0 + 1, base, x0 + 1, x1 - 1, base + 3, x1 - 1);
			walls(level, chunk, p, x0, x0, x1, x1, base, base + 3, x1 - x0, tier == 0 ? 3 : 0, 3, false);
			// The eave round this storey (tiles out over the walls, then in to the next storey's foot; the next storey's walls
			// replace whatever of it they stand on).
			roof(level, chunk, p, x0 - 2, x0 - 2, x1 + 2, x1 + 2, base + 4, 3, false, true);
			closeEaves(level, chunk, p, x0, x0, x1, x1, base + 4, x0 - 2, x0 - 2, x1 + 2, x1 + 2, base + 4, 3);
			// Lanterns, clear of the ladder up the middle of the back: one at the front of the middle, and in a wider storey one
			// over each back corner as well.
			int mid = (x0 + x1) / 2;
			lanternHanging(level, chunk, p, mid, base + 3, mid - 1);
			if (x1 - x0 >= 6) {
				lanternHanging(level, chunk, p, x0 + 2, base + 3, x1 - 2);
				lanternHanging(level, chunk, p, x1 - 2, base + 3, x1 - 2);
			}
			if (tier < storeys - 1) library(level, chunk, p, x0, x1, base, tier == 0, ladderX, ladderZ);
			base += 5;
		}
		// The ladder, laid once all the floors are in: one straight shaft against a timber spine, up to the top floor.
		int topBase = 1 + 5 * (storeys - 1);
		for (int y = 1; y < topBase; y++) {
			put(level, chunk, ladderX, y, ladderZ + 1, p.floor);
			put(level, chunk, ladderX, y, ladderZ, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
		}
		// The top storey: a ceiling to close it off from the hollow of the crowning roof (and to hang its lantern from), a
		// meditation mat under the spire, candles either side of it.
		fill(level, chunk, tx0 + 1, topBase + 4, tx0 + 1, tx1 - 1, topBase + 4, tx1 - 1, p.floor);
		int[] seat = pagodaSeat();
		put(level, chunk, seat[0], seat[1], seat[2], ModBlocks.RED_MEDITATION_MAT);
		candles(level, chunk, p, tx0 + 1, topBase, seat[2], 4);
		candles(level, chunk, p, tx1 - 1, topBase, seat[2], 4);
		// The crowning roof and spire.
		roof(level, chunk, p, tx0 - 1, tx0 - 1, tx1 + 1, tx1 + 1, base, ridgeCourses(tx0 - 1, tx0 - 1, tx1 + 1, tx1 + 1), true, true);
		closeEaves(level, chunk, p, tx0, tx0, tx1, tx1, topBase + 4, tx0 - 1, tx0 - 1, tx1 + 1, tx1 + 1, base,
				ridgeCourses(tx0 - 1, tx0 - 1, tx1 + 1, tx1 + 1));
		int cx = w / 2;
		int spire = base + ridgeCourses(tx0 - 1, tx0 - 1, tx1 + 1, tx1 + 1);
		put(level, chunk, cx, spire, cx, p.accent);
		put(level, chunk, cx, spire + 1, cx, p.window);
		put(level, chunk, cx, spire + 2, cx, Blocks.LIGHTNING_ROD);
	}

	/**
	 * One storey of the pagoda's library, inside walls x0 and x1 (the same in z) from {@code base} up: bookshelves two high
	 * down both side walls with candles on them, a gap halfway along each wall for a lectern (and on the ground floor the
	 * library's chest), all clear of the ladder up the middle.
	 */
	private void library(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int x1, int base, boolean ground, int ladderX, int ladderZ) {
		int mid = (x0 + x1) / 2;
		for (int z = x0 + 1; z <= x1 - 1; z++) {
			if (z == mid) continue;
			for (int x : new int[] {x0 + 1, x1 - 1}) {
				if (x == ladderX && (z == ladderZ || z == ladderZ + 1)) continue;
				fill(level, chunk, x, base, z, x, base + 1, z, Blocks.BOOKSHELF);
				if ((z - x0) % 2 == 1) candles(level, chunk, p, x, base + 2, z, 3 + rand(x, z, base).nextInt(2));
			}
		}
		put(level, chunk, x1 - 1, base, mid, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.WEST));
		if (ground) chest(level, chunk, x0 + 1, base, mid, Direction.EAST, LIBRARY_LOOT);
		else put(level, chunk, x0 + 1, base, mid, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.EAST));
	}

	/** The meditation mat at the top of the pagoda, {x, y, z}: in the middle of the top floor, in front of the ladder. */
	public int[] pagodaSeat() {
		int m = 2, w = width(), top = storeys() - 1;
		int tx0 = m + pagodaInset(top), tx1 = w - 1 - m - pagodaInset(top);
		return new int[] {(tx0 + tx1) / 2, 1 + 5 * top, (tx0 + tx1) / 2 - 1};
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
	 * A disciple's house: a little house of plaster and pillars on a stone base, under a hipped roof with a ridge of the trim
	 * colour, its mat (its home) in the middle, a chest, a lantern. An elder's house is larger and taller, a pavilion of two
	 * eaves with a red silk mat, shelves and a better chest.
	 */
	private void dwelling(WorldGenLevel level, BoundingBox chunk, SectPalette p, boolean elder) {
		Frame f = frame(1, 1);
		int floor = 1, top = floor + (elder ? 5 : 4);
		fill(level, chunk, f.x0, 0, f.z0, f.x1, 0, f.z1, p.foundation);
		for (int x = f.x0; x <= f.x1; x++) {
			for (int z = f.z0; z <= f.z1; z++) foundation(level, chunk, x, 0, z, p.foundation.defaultBlockState());
		}
		fill(level, chunk, f.x0, 1, f.z0, f.x1, 1, f.z1, p.foundation);
		interior(level, chunk, p, f.x0 + 1, f.z0 + 1, f.x1 - 1, f.z1 - 1, floor, top + 3);
		walls(level, chunk, p, f.x0, f.z0, f.x1, f.z1, floor + 1, top - 1, elder ? 3 : 2, 1, 2, false);
		put(level, chunk, f.cx(), floor, f.z0 - 1, p.baseStair(Direction.NORTH));
		fill(level, chunk, f.x0 + 1, top, f.z0 + 1, f.x1 - 1, top, f.z1 - 1, p.floor);
		if (elder) {
			// The lower eave, a short upper storey inside it, and the upper roof.
			roof(level, chunk, p, 0, 0, width() - 1, depth() - 1, top + 1, 2, false, true);
			closeEaves(level, chunk, p, f.x0, f.z0, f.x1, f.z1, top, 0, 0, width() - 1, depth() - 1, top + 1, 2);
			int ux0 = f.x0 + 1, uz0 = f.z0 + 1, ux1 = f.x1 - 1, uz1 = f.z1 - 1;
			for (int x = ux0; x <= ux1; x++) {
				for (int z = uz0; z <= uz1; z++) {
					boolean edgeX = x == ux0 || x == ux1, edgeZ = z == uz0 || z == uz1;
					if (!edgeX && !edgeZ) continue;
					boolean corner = edgeX && edgeZ;
					fill(level, chunk, x, top + 1, z, x, top + 3, z, corner ? p.pillar(Direction.Axis.Y) : p.wall.defaultBlockState());
				}
			}
			int upper = ridgeCourses(1, 1, width() - 2, depth() - 2);
			roof(level, chunk, p, 1, 1, width() - 2, depth() - 2, top + 4, upper, true, true);
			closeEaves(level, chunk, p, ux0, uz0, ux1, uz1, top + 3, 1, 1, width() - 2, depth() - 2, top + 4, upper);
		} else {
			int courses = ridgeCourses(0, 0, width() - 1, depth() - 1);
			roof(level, chunk, p, 0, 0, width() - 1, depth() - 1, top + 1, courses, true);
			closeEaves(level, chunk, p, f.x0, f.z0, f.x1, f.z1, top, 0, 0, width() - 1, depth() - 1, top + 1, courses);
		}
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

	// --- The Altar of Heaven ---

	/** How many round terraces the Altar of Heaven has (1 to 3, by the sect's size). */
	private int altarTiers() { return Math.max(1, Math.min(3, extra.getInt("Tiers"))); }

	/** The radius of terrace {@code tier}, counting down from the top (0); each ring below is three blocks wider. */
	private double altarRadius(int tier) { return (altarTiers() == 3 ? 5.7 : 4.5) + 3 * tier; }

	/** The square lot an Altar of Heaven of {@code tiers} needs: its widest ring and the two steps up to it. */
	public static int altarSize(int tiers) {
		double widest = (tiers >= 3 ? 5.7 : 4.5) + 3 * (Math.max(1, Math.min(3, tiers)) - 1);
		return 2 * (int) Math.ceil(widest) + 5;
	}

	/** How high the terraces stand {@code dist} from the middle: two blocks for each terrace whose ring reaches that far. */
	private int altarHeight(double dist) {
		int tiers = altarTiers();
		for (int t = 0; t < tiers; t++) if (dist <= altarRadius(t)) return 2 * (tiers - t);
		return 0;
	}

	/** {x, y, z}: the Heaven Altar block at the top's middle, and the spot before it, toward the stairs, where one kneels. */
	public int[][] altarPosts() {
		int cx = width() / 2, cz = depth() / 2, top = 2 * altarTiers();
		return new int[][] {{cx, top + 1, cz}, {cx, top + 1, cz - 2}};
	}

	/**
	 * The Altar of Heaven, after the Circular Mound in Beijing: round terraces of worked stone, each two blocks above the one
	 * below and three blocks narrower, a balustrade round each, stairs up the front; on the top the Heaven Altar where the sect's
	 * cultivators perform the Ritual to the Heavens (see NpcLife), under open sky. Lantern posts at the top's corners, and for
	 * the greatest sects four pillars crowned with lanterns and incense candles by the altar. Built to be judged (HeavenAltar)
	 * Modest, Grand and Magnificent with one, two and three terraces.
	 */
	private void altarOfHeaven(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int tiers = altarTiers();
		int cx = width() / 2, cz = depth() / 2, top = 2 * tiers;
		for (int x = 0; x < width(); x++) {
			for (int z = 0; z < depth(); z++) {
				double dist = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
				int h = altarHeight(dist);
				if (h == 0) continue;
				fill(level, chunk, x, 0, z, x, h - 1, z, p.foundation);
				foundation(level, chunk, x, 0, z, p.foundation.defaultBlockState());
				put(level, chunk, x, h, z, p.paving);
				air(level, chunk, x, h + 1, z, x, top + 6, z);
				// The balustrade round the rim of this terrace, open where the stairs come up.
				double rim = altarRadius(tiers - h / 2);
				boolean stairs = Math.abs(x - cx) <= 1 && z < cz;
				if (dist > rim - 1 && !stairs) put(level, chunk, x, h + 1, z, p.baseWall);
			}
		}
		// The stairs up the front: wherever the next column inward stands higher, a step for every block of the rise, the top
		// one against the terrace on a block of stone and each lower one a column further out, so the way up is a walk, not a jump.
		for (int x = cx - 1; x <= cx + 1; x++) {
			for (int z = 0; z < cz; z++) {
				int here = altarHeight(Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz)));
				int next = altarHeight(Math.sqrt((x - cx) * (x - cx) + (z + 1 - cz) * (z + 1 - cz)));
				int rise = next - here;
				for (int k = 0; k < rise && z - k >= 0; k++) {
					int step = next - k;
					if (step - 1 >= here + 1) fill(level, chunk, x, here + 1, z - k, x, step - 1, z - k, p.foundation);
					put(level, chunk, x, step, z - k, p.baseStair(Direction.NORTH));
				}
			}
		}
		if (tiers >= 3) {
			// Four pillars on the top, crowned with lanterns.
			for (int[] c : new int[][] {{5, 2}, {5, -2}, {-5, 2}, {-5, -2}}) {
				pillar(level, chunk, p, cx + c[0], top + 1, top + 3, cz + c[1]);
				lanternStanding(level, chunk, p, cx + c[0], top + 4, cz + c[1]);
			}
		} else {
			for (int[] c : new int[][] {{3, 3}, {3, -3}, {-3, 3}, {-3, -3}}) {
				put(level, chunk, cx + c[0], top + 1, cz + c[1], p.baseWall);
				lanternStanding(level, chunk, p, cx + c[0], top + 2, cz + c[1]);
			}
		}
		put(level, chunk, cx, top + 1, cz, ModBlocks.HEAVEN_ALTAR);
		if (tiers >= 2) {
			candles(level, chunk, p, cx - 1, top + 1, cz + 1, 3);
			candles(level, chunk, p, cx + 1, top + 1, cz + 1, 3);
		}
	}

	// --- The training ground ---

	/** The training ground's lot: across its front, and deep. */
	public static final int TRAINING_W = 19, TRAINING_D = 17;
	/** Where the dummies stand across the back, and how far in the sparring ring's middle lies. */
	private static final int[] DUMMIES = {3, 7, 11, 15};
	private static final int RING_Z = 7;

	/** {x, y, z}: the sparring ring's middle, then a drill spot before each training dummy. */
	public int[][] trainingPosts() {
		int[][] posts = new int[1 + DUMMIES.length][];
		posts[0] = new int[] {width() / 2, 1, RING_Z};
		for (int i = 0; i < DUMMIES.length; i++) posts[i + 1] = new int[] {DUMMIES[i], 1, depth() - 5};
		return posts;
	}

	/**
	 * The training ground, where the disciples practise (NpcLife): a yard of worked stone with an edging, a sparring ring set
	 * into the floor, straw training dummies on posts across the back with a drill spot before each, sharpening stones and
	 * barrels of practice gear down the sides, a stone lantern at each corner and the sect's banners either side of the way in.
	 */
	private void trainingGround(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = width(), d = depth(), cx = w / 2;
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				boolean edge = x == 0 || z == 0 || x == w - 1 || z == d - 1;
				foundation(level, chunk, x, 0, z, p.foundation.defaultBlockState());
				double ring = Math.sqrt((x - cx) * (x - cx) + (z - RING_Z) * (z - RING_Z));
				put(level, chunk, x, 0, z, edge ? p.foundation : ring >= 3.6 && ring < 4.6 ? p.path : p.paving);
				air(level, chunk, x, 1, z, x, 8, z);
			}
		}
		for (int x : DUMMIES) {
			put(level, chunk, x, 1, d - 3, p.pillar(Direction.Axis.Y));
			put(level, chunk, x, 2, d - 3, Blocks.HAY_BLOCK);
		}
		for (int side : new int[] {1, w - 2}) {
			put(level, chunk, side, 1, 5, Blocks.GRINDSTONE);
			put(level, chunk, side, 1, 9, Blocks.BARREL);
			put(level, chunk, side, 1, 10, Blocks.BARREL);
		}
		for (int[] c : new int[][] {{1, 1}, {w - 2, 1}, {1, d - 2}, {w - 2, d - 2}}) stoneLantern(level, chunk, p, c[0], 1, c[1]);
		for (int x : new int[] {cx - 3, cx + 3}) {
			fill(level, chunk, x, 1, 0, x, 3, 0, p.baseWall);
			put(level, chunk, x, 4, 0, p.banner);
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
