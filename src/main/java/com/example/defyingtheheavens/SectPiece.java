package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * One part of a sect (see {@link SectStructure}): the grounds themselves (the levelled meadow, its paths, lanterns and trees),
 * the gates, the Formation plaza at the heart of the sect, the Sect Master's hall beside it, the meditation hall, the treasury,
 * the pagoda, herb gardens, a lotus pond and its pavilion, and the members' homes (elders' houses, disciples' houses and the
 * outer disciples' dormitories).
 * <p>
 * The look, in a few rules (less is more): every building stands on a stone base or on stone feet; red pillars carry a beam
 * all round; walls are plaster on a stone sill with lattice windows; the roofs overhang by two, carried on brackets, and are
 * concave - a thin eave whose corners flick up, rising gently and then steeply to a capped ridge with upturned ends.
 * <p>
 * Every building is drawn in its own frame, through StructurePiece's orientation: x across its front, z from its front
 * inward, y from the ground (0) up. The orientation (the direction the building extends into from its entrance) rotates and
 * mirrors everything, block states included, into the world. The grounds alone are drawn straight in world coordinates.
 * All the randomness comes from the piece's own seed, never the chunk's, so a building split across chunks comes out whole.
 */
public class SectPiece extends StructurePiece {
	/**
	 * What a piece is. TERRACE and TOWER belong to the walled compounds sects were before they became villages: kept only so
	 * that a half-generated old sect still loads (they draw nothing now).
	 */
	public enum Kind { TERRACE, GATE, TOWER, PLAZA, MEDITATION_HALL, TREASURY, MAIN_HALL, PAGODA, GARDEN, DWELLING, ELDER_HOUSE,
		GROUNDS, DORMITORY, POND, PAVILION }

	public static final ResourceLocation TREASURY_LOOT = DefyingTheHeavens.id("chests/sect_treasury");
	public static final ResourceLocation DWELLING_LOOT = DefyingTheHeavens.id("chests/sect_dwelling");
	public static final ResourceLocation ELDER_LOOT = DefyingTheHeavens.id("chests/sect_elder");
	public static final ResourceLocation LIBRARY_LOOT = DefyingTheHeavens.id("chests/sect_library");

	/** How far a building's base may be filled down to meet the ground before it is left to stand on its own. */
	private static final int MAX_FOUNDATION = 12;
	/** How high the grounds are cleared above the meadow (trees, the tops of hummocks). */
	private static final int CLEARANCE = 24;

	private final Kind kind;
	private final long seed;
	private final Alignment.Faction faction;
	private final boolean upper;
	/** Per-kind data: the plaza's sect blueprint, the grounds' paths and trees, a pagoda's tiers, a house's mats. */
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
			case GROUNDS -> grounds(level, chunk, p);
			case GATE -> gate(level, chunk, p);
			case PLAZA -> plaza(level, chunk, p);
			case MEDITATION_HALL -> meditationHall(level, chunk, p);
			case TREASURY -> treasury(level, chunk, p);
			case MAIN_HALL -> mainHall(level, chunk, p);
			case PAGODA -> pagoda(level, chunk, p);
			case GARDEN -> garden(level, chunk, p);
			case POND -> pond(level, chunk, p);
			case PAVILION -> pavilion(level, chunk, p);
			case ELDER_HOUSE -> elderHouse(level, chunk, p);
			case DWELLING, DORMITORY -> dwelling(level, chunk, p);
			case TERRACE, TOWER -> {}
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
		set(level, pos, state);
	}

	private void put(WorldGenLevel level, BoundingBox chunk, int x, int y, int z, Block block) {
		put(level, chunk, x, y, z, block.defaultBlockState());
	}

	/** Places a block at a world position, as it is (the grounds are drawn in world coordinates). */
	private void putWorld(WorldGenLevel level, BoundingBox chunk, int x, int y, int z, BlockState state) {
		BlockPos pos = new BlockPos(x, y, z);
		if (!chunk.isInside(pos)) return;
		set(level, pos, state);
	}

	private static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, Block.UPDATE_CLIENTS);
		Block block = state.getBlock();
		if (block instanceof StairBlock || block instanceof CrossCollisionBlock || block instanceof WallBlock || block instanceof FenceBlock
				|| block instanceof IronBarsBlock) {
			level.getChunk(pos).markPosForPostprocessing(pos);
		}
		if (!state.getFluidState().isEmpty()) level.scheduleTick(pos, state.getFluidState().getType(), 0);
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

	private boolean isAirAt(WorldGenLevel level, BoundingBox chunk, int x, int y, int z) {
		BlockPos pos = getWorldPos(x, y, z);
		return chunk.isInside(pos) && level.getBlockState(pos).isAir();
	}

	/** Fills below {@code y} down to the ground (at most {@link #MAX_FOUNDATION}), so nothing floats over a dip. */
	private void foundation(WorldGenLevel level, BoundingBox chunk, int x, int y, int z, BlockState state) {
		BlockPos.MutableBlockPos pos = getWorldPos(x, y - 1, z);
		if (!chunk.isInside(pos)) return;
		foundationAt(level, pos, state);
	}

	private static void foundationAt(WorldGenLevel level, BlockPos.MutableBlockPos pos, BlockState state) {
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

	/** A stone lantern (toro): a stone post with the lantern standing on it. */
	private void stoneLantern(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int y, int z) {
		put(level, chunk, x, y, z, p.baseWall);
		lanternStanding(level, chunk, p, x, y + 1, z);
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

	private static BlockState lattice(SectPalette p, Direction facing) {
		return p.lattice.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP).setValue(TrapDoorBlock.FACING, facing);
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

	/** Height of hip-roof ring {@code k} above the eave: shallow at the eave, steepening toward the ridge (a concave roof). */
	private static int ringLevel(int k) {
		return k <= 1 ? 0 : k <= 4 ? k - 1 : 2 * k - 5;
	}

	/** Rings from here on rise two blocks a step, a full tile under each stair. */
	private static final int STEEP_RING = 5;

	/**
	 * A concave hipped roof over the rectangle x0..x1, z0..z1 (eaves included), its eave at {@code y}. Ring 0 is a thin
	 * eave of slabs whose four corners flick up; the rings above rise a block a step, then two, to a ridge capped in trim
	 * with upturned ends (over a square, a point). {@code rings} limits how many are laid (a skirt roof under an upper storey,
	 * left open in the middle when {@code ridge} is false). Returns the level just above the ridge.
	 */
	private int roofHip(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int z0, int x1, int z1, int y, int rings, boolean ridge) {
		int prev = Integer.MIN_VALUE;
		for (int k = 0; k < rings; k++) {
			int ax0 = x0 + k, ax1 = x1 - k, az0 = z0 + k, az1 = z1 - k;
			if (ax0 > ax1 || az0 > az1) break;
			int ly = y + ringLevel(k);
			if (ridge && (ax1 - ax0 <= 1 || az1 - az0 <= 1)) {
				return ridgeCap(level, chunk, p, ax0, az0, ax1, az1, prev == Integer.MIN_VALUE ? ly : prev);
			}
			boolean steep = k >= STEEP_RING;
			for (int x = ax0; x <= ax1; x++) {
				ringCell(level, chunk, p, x, ly, az0, Direction.NORTH, k, steep);
				ringCell(level, chunk, p, x, ly, az1, Direction.SOUTH, k, steep);
			}
			for (int z = az0 + 1; z < az1; z++) {
				ringCell(level, chunk, p, ax0, ly, z, Direction.EAST, k, steep);
				ringCell(level, chunk, p, ax1, ly, z, Direction.WEST, k, steep);
			}
			if (k == 0) {
				// The corners flick up: a half tile higher than the eave.
				for (int[] c : new int[][] {{ax0, az0}, {ax1, az0}, {ax0, az1}, {ax1, az1}}) put(level, chunk, c[0], ly, c[1], p.trimSlab(true));
			}
			prev = ly;
		}
		return (prev == Integer.MIN_VALUE ? y : prev) + 1;
	}

	private void ringCell(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x, int y, int z, Direction inward, int k, boolean steep) {
		if (k == 0) {
			put(level, chunk, x, y, z, p.roofSlab(false));
			return;
		}
		put(level, chunk, x, y, z, p.roofStair(inward, false));
		if (steep) put(level, chunk, x, y - 1, z, p.roof);
	}

	/** The ridge at {@code y}: tiles along it, capped in trim, its ends swept up; over a square, a capped point. Returns y + 1. */
	private int ridgeCap(WorldGenLevel level, BoundingBox chunk, SectPalette p, int ax0, int az0, int ax1, int az1, int y) {
		fill(level, chunk, ax0, y, az0, ax1, y, az1, p.roof);
		if (ax1 - ax0 <= 1 && az1 - az0 <= 1) {
			fill(level, chunk, ax0, y + 1, az0, ax1, y + 1, az1, p.trimSlab(false));
			return y + 1;
		}
		if (ax1 - ax0 >= az1 - az0) {
			for (int x = ax0 + 1; x < ax1; x++) fill(level, chunk, x, y + 1, az0, x, y + 1, az1, p.trimSlab(false));
			for (int z = az0; z <= az1; z++) {
				put(level, chunk, ax0, y + 1, z, p.trimStair(Direction.WEST, false));
				put(level, chunk, ax1, y + 1, z, p.trimStair(Direction.EAST, false));
			}
		} else {
			for (int z = az0 + 1; z < az1; z++) fill(level, chunk, ax0, y + 1, z, ax1, y + 1, z, p.trimSlab(false));
			for (int x = ax0; x <= ax1; x++) {
				put(level, chunk, x, y + 1, az0, p.trimStair(Direction.NORTH, false));
				put(level, chunk, x, y + 1, az1, p.trimStair(Direction.SOUTH, false));
			}
		}
		return y + 1;
	}

	/**
	 * A gable roof over x0..x1, z0..z1, its ridge along x: the same concave slopes front and back, the verges in trim (their
	 * eave ends flicked up), a capped ridge with upturned ends. Returns the level just above the ridge.
	 */
	private int roofGable(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int z0, int x1, int z1, int y) {
		int prev = Integer.MIN_VALUE;
		for (int k = 0; ; k++) {
			int az0 = z0 + k, az1 = z1 - k;
			if (az0 > az1) break;
			int ly = y + ringLevel(k);
			if (az1 - az0 <= 1) {
				int ry = prev == Integer.MIN_VALUE ? ly : prev;
				fill(level, chunk, x0, ry, az0, x1, ry, az1, p.roof);
				for (int x = x0 + 1; x < x1; x++) fill(level, chunk, x, ry + 1, az0, x, ry + 1, az1, p.trimSlab(false));
				for (int z = az0; z <= az1; z++) {
					put(level, chunk, x0, ry + 1, z, p.trimStair(Direction.WEST, false));
					put(level, chunk, x1, ry + 1, z, p.trimStair(Direction.EAST, false));
				}
				return ry + 1;
			}
			for (int x = x0; x <= x1; x++) {
				ringCell(level, chunk, p, x, ly, az0, Direction.NORTH, k, k >= STEEP_RING);
				ringCell(level, chunk, p, x, ly, az1, Direction.SOUTH, k, k >= STEEP_RING);
			}
			for (int x : new int[] {x0, x1}) {
				if (k == 0) {
					put(level, chunk, x, ly, az0, p.trimSlab(true));
					put(level, chunk, x, ly, az1, p.trimSlab(true));
				} else {
					put(level, chunk, x, ly, az0, p.trimStair(Direction.NORTH, false));
					put(level, chunk, x, ly, az1, p.trimStair(Direction.SOUTH, false));
				}
			}
			prev = ly;
		}
		return (prev == Integer.MIN_VALUE ? y : prev) + 1;
	}

	/** Plaster up into the gable of each end wall (x0 and x1, z0..z1) under the roof, with a small vent window. */
	private void gableEnds(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int x1, int z0, int z1, int yBase, int ridge) {
		for (int x : new int[] {x0, x1}) {
			for (int z = z0; z <= z1; z++) {
				for (int y = yBase; y <= ridge; y++) {
					if (!isAirAt(level, chunk, x, y, z)) continue;
					boolean roofAbove = false;
					for (int yy = y + 1; yy <= ridge + 1 && !roofAbove; yy++) roofAbove = !at(level, chunk, x, yy, z).isAir();
					if (roofAbove) put(level, chunk, x, y, z, p.wall);
				}
			}
			if (ridge - yBase >= 3) put(level, chunk, x, yBase + 1, (z0 + z1) / 2, p.window);
		}
	}

	// --- Walls ---

	/**
	 * Where the posts stand along a wall from {@code a} to {@code b}, mirrored about its middle so both halves of a face match:
	 * framing a doorway {@code gap} wide in the middle (0 for none), the rest split into bays as near {@code bay} wide as fit.
	 */
	static java.util.SortedSet<Integer> framePosts(int a, int b, int gap, int bay) {
		java.util.SortedSet<Integer> posts = new java.util.TreeSet<>();
		posts.add(a);
		if (gap > 0) {
			int inner = (a + b) / 2 - gap / 2 - 1;
			int span = inner - a;
			if (span >= 2) {
				int n = Math.max(1, Math.round(span / (float) bay));
				for (int i = 1; i <= n; i++) posts.add(a + (int) Math.floor(i * span / (double) n + 0.5));
			}
		} else {
			int length = b - a;
			int n = Math.max(1, Math.round(length / (float) bay));
			if (length % 2 == 1 && n % 2 == 0) n = length / (double) (n + 1) >= 2 ? n + 1 : n - 1;
			for (int i = 1; i <= n / 2; i++) posts.add(a + (int) Math.floor(i * length / (double) n + 0.5));
		}
		for (int x : new java.util.ArrayList<>(posts)) posts.add(a + b - x);
		return posts;
	}

	/**
	 * Timber-framed walls between x0..x1, z0..z1, from {@code y0} to {@code y1}: pillars at the corners and evenly between
	 * (front and back framing the doorway, see {@link #framePosts}), plaster (or {@code infill}) between on a stone sill with
	 * windows in the middle of each bay, lattice (barred if {@code bars}), a beam over everything, and the doorway
	 * ({@code door} wide, {@code doorHeight} high, under a lintel) in the middle of the front.
	 */
	private void walls(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int z0, int x1, int z1, int y0, int y1, int bay, int door,
			int doorHeight, boolean bars) {
		walls(level, chunk, p, x0, z0, x1, z1, y0, y1, bay, door, doorHeight, bars, p.wall);
	}

	private void walls(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int z0, int x1, int z1, int y0, int y1, int bay, int door,
			int doorHeight, boolean bars, Block infill) {
		int cx = (x0 + x1) / 2;
		java.util.SortedSet<Integer> across = framePosts(x0, x1, door, bay), along = framePosts(z0, z1, 0, bay);
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean edgeX = x == x0 || x == x1, edgeZ = z == z0 || z == z1;
				if (!edgeX && !edgeZ) continue;
				java.util.SortedSet<Integer> posts = edgeZ ? across : along;
				int t = edgeZ ? x : z;
				if (posts.contains(t)) {
					pillar(level, chunk, p, x, y0, y1, z);
				} else {
					put(level, chunk, x, y0, z, p.wallBase);
					fill(level, chunk, x, y0 + 1, z, x, y1, z, infill);
					int lo = posts.headSet(t).last(), hi = posts.tailSet(t).first();
					boolean window = hi - lo - 1 <= 3 || Math.min(t - lo, hi - t) >= 2;
					if (window && y1 - y0 >= 2) fill(level, chunk, x, y0 + 1, z, x, Math.min(y1, y0 + 2), z, bars ? Blocks.IRON_BARS : p.window);
				}
				put(level, chunk, x, y1 + 1, z, p.beam(edgeZ ? Direction.Axis.X : Direction.Axis.Z));
			}
		}
		if (door > 0) {
			air(level, chunk, cx - door / 2, y0, z0, cx + door / 2, y0 + doorHeight - 1, z0);
			fill(level, chunk, cx - door / 2, y0 + doorHeight, z0, cx + door / 2, y0 + doorHeight, z0, p.beam(Direction.Axis.X));
		}
	}

	/** Brackets (dougong) under the eaves: upside-down trim stairs one out from the wall line all round, at {@code y}. */
	private void brackets(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int z0, int x1, int z1, int y) {
		for (int x = x0; x <= x1; x++) {
			put(level, chunk, x, y, z0 - 1, p.trimStair(Direction.SOUTH, true));
			put(level, chunk, x, y, z1 + 1, p.trimStair(Direction.NORTH, true));
		}
		for (int z = z0; z <= z1; z++) {
			put(level, chunk, x0 - 1, y, z, p.trimStair(Direction.EAST, true));
			put(level, chunk, x1 + 1, y, z, p.trimStair(Direction.WEST, true));
		}
	}

	/** A raised stone base, {@code height} above the ground, with {@code steps} wide steps down the front middle between cheeks. */
	private void plinth(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int z0, int x1, int z1, int height, int steps) {
		if (height <= 0) return;
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				fill(level, chunk, x, 1, z, x, height, z, p.foundation);
				foundation(level, chunk, x, 1, z, p.foundation.defaultBlockState());
			}
		}
		int cx = (x0 + x1) / 2;
		for (int i = 0; i < height; i++) {
			int z = z0 - 1 - i;
			for (int x = cx - steps / 2; x <= cx + steps / 2; x++) {
				put(level, chunk, x, height - i, z, p.baseStair(Direction.NORTH));
				foundation(level, chunk, x, height - i, z, p.foundation.defaultBlockState());
			}
			for (int x : new int[] {cx - steps / 2 - 1, cx + steps / 2 + 1}) {
				fill(level, chunk, x, 1, z, x, height - i, z, p.foundation);
				foundation(level, chunk, x, 1, z, p.foundation.defaultBlockState());
			}
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

	/** Path kinds, as the grounds' blueprint names them. */
	public static final int PATH_ROAD = 0, PATH_LANE = 1, PATH_RING = 2;
	/** Trees and such on the grounds. */
	public static final int TREE_BLOSSOM = 0, TREE_PINE = 1, TREE_BAMBOO = 2, TREE_ROCK = 3;

	/**
	 * The grounds, all in world coordinates: within {@code Radius} of the core the ground is levelled to the plaza's height
	 * (filled with earth below, cleared above) and grassed over; roads of dressed stone run to the gates, earthen lanes to
	 * each door, a paved ring round the plaza; stone lanterns line the roads; the sect's trees, bamboo and the odd mossy
	 * rock stand on the open ground, with tufts of grass and fallen petals among them. The buildings, drawn after, stand on it.
	 */
	private void grounds(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int y = boundingBox.minY();
		int cx = extra.getInt("CentreX"), cz = extra.getInt("CentreZ");
		double radius = extra.getInt("Radius");
		int x0 = Math.max(chunk.minX(), boundingBox.minX()), x1 = Math.min(chunk.maxX(), boundingBox.maxX());
		int z0 = Math.max(chunk.minZ(), boundingBox.minZ()), z1 = Math.min(chunk.maxZ(), boundingBox.maxZ());
		BlockState dirt = Blocks.DIRT.defaultBlockState();
		BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if (Math.hypot(x - cx, z - cz) > radius + 0.5) continue;
				foundationAt(level, pos.set(x, y - 1, z), dirt);
				putWorld(level, chunk, x, y, z, grass);
				for (int dy = 1; dy <= CLEARANCE; dy++) {
					pos.set(x, y + dy, z);
					if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
		}
		// Paths: {x0, z0, x1, z1, kind} in the world.
		for (Tag t : extra.getList("Paths", Tag.TAG_INT_ARRAY)) {
			int[] path = ((net.minecraft.nbt.IntArrayTag) t).getAsIntArray();
			if (path.length < 5) continue;
			for (int x = Math.max(x0, path[0]); x <= Math.min(x1, path[2]); x++) {
				for (int z = Math.max(z0, path[1]); z <= Math.min(z1, path[3]); z++) {
					double dist = Math.hypot(x - cx, z - cz);
					if (dist > radius + 0.5) continue;
					if (path[4] == PATH_RING && !(dist > SectPiece.PLAZA_RADIUS + 0.5 && dist <= SectPiece.PLAZA_RADIUS + 2.5)) continue;
					putWorld(level, chunk, x, y, z, pathBlock(p, x, z, path[4]));
				}
			}
		}
		for (Tag t : extra.getList("Lanterns", Tag.TAG_INT_ARRAY)) {
			int[] at = ((net.minecraft.nbt.IntArrayTag) t).getAsIntArray();
			if (at.length < 2) continue;
			putWorld(level, chunk, at[0], y + 1, at[1], p.baseWall.defaultBlockState());
			putWorld(level, chunk, at[0], y + 2, at[1], p.lantern.defaultBlockState().setValue(LanternBlock.HANGING, false));
		}
		for (Tag t : extra.getList("Trees", Tag.TAG_INT_ARRAY)) {
			int[] at = ((net.minecraft.nbt.IntArrayTag) t).getAsIntArray();
			if (at.length < 3) continue;
			// Each tree reaches at most 5 blocks from its foot: skip those that can't touch this chunk.
			if (at[0] + 5 < chunk.minX() || at[0] - 5 > chunk.maxX() || at[1] + 5 < chunk.minZ() || at[1] - 5 > chunk.maxZ()) continue;
			RandomSource random = rand(at[0], at[1], 7 + at[2]);
			switch (at[2]) {
				case TREE_BLOSSOM -> blossomTree(level, chunk, p, at[0], y, at[1], random);
				case TREE_PINE -> pineTree(level, chunk, at[0], y, at[1], random);
				case TREE_BAMBOO -> bambooClump(level, chunk, at[0], y, at[1], random);
				default -> rock(level, chunk, at[0], y, at[1], random);
			}
		}
		// Tufts of grass and fallen petals on the open meadow.
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if (Math.hypot(x - cx, z - cz) > radius - 0.5) continue;
				if (!level.getBlockState(pos.set(x, y, z)).is(Blocks.GRASS_BLOCK) || !level.getBlockState(pos.set(x, y + 1, z)).isAir()) continue;
				double r = rand(x, z, 3).nextDouble();
				if (r < 0.06) putWorld(level, chunk, x, y + 1, z, Blocks.GRASS.defaultBlockState());
				else if (r < 0.075) putWorld(level, chunk, x, y + 1, z, (p.leaves == Blocks.CHERRY_LEAVES ? Blocks.PINK_PETALS : Blocks.FERN).defaultBlockState());
			}
		}
	}

	private BlockState pathBlock(SectPalette p, int x, int z, int kind) {
		double r = rand(x, z, 2).nextDouble();
		if (kind == PATH_LANE) return (r > 0.18 ? Blocks.DIRT_PATH : r > 0.06 ? Blocks.COARSE_DIRT : Blocks.GRAVEL).defaultBlockState();
		return (r > 0.12 ? p.path : p.pathAlt).defaultBlockState();
	}

	/** A flowering tree: a short trunk forking into two or three boughs, each under a wide, flat cloud of blossom. */
	private void blossomTree(WorldGenLevel level, BoundingBox chunk, SectPalette p, int cx, int y, int cz, RandomSource random) {
		BlockState log = logOf(p.trunk, Direction.Axis.Y);
		int h = 3 + random.nextInt(2);
		for (int dy = 1; dy <= h; dy++) putWorld(level, chunk, cx, y + dy, cz, log);
		int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
		int first = random.nextInt(dirs.length);
		int boughs = 2 + random.nextInt(2);
		for (int b = 0; b < boughs; b++) {
			int[] d = dirs[(first + b * 3) % dirs.length];
			int bx = cx, by = y + h, bz = cz;
			int steps = 2 + random.nextInt(2);
			for (int s = 0; s < steps; s++) {
				bx += d[0];
				bz += d[1];
				if (s % 2 == 0) by++;
				putWorld(level, chunk, bx, by, bz, log);
			}
			leafCloud(level, chunk, p.leaves, bx, by + 1, bz, 2 + random.nextInt(2), random);
		}
		leafCloud(level, chunk, p.leaves, cx, y + h + 2, cz, 2, random);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int i = 0; i < 8; i++) {
			int x = cx + random.nextInt(9) - 4, z = cz + random.nextInt(9) - 4;
			if (level.getBlockState(pos.set(x, y, z)).is(Blocks.GRASS_BLOCK) && level.getBlockState(pos.set(x, y + 1, z)).isAir()) {
				putWorld(level, chunk, x, y + 1, z, (p.leaves == Blocks.CHERRY_LEAVES ? Blocks.PINK_PETALS : Blocks.GRASS).defaultBlockState());
			}
		}
	}

	/** A flattened dome of leaves: wide at its base, a layer narrower above, a few hanging below the rim. */
	private void leafCloud(WorldGenLevel level, BoundingBox chunk, Block leaves, int cx, int cy, int cz, int rad, RandomSource random) {
		BlockState leaf = leaves.defaultBlockState();
		if (leaf.hasProperty(LeavesBlock.PERSISTENT)) leaf = leaf.setValue(LeavesBlock.PERSISTENT, true);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dy = 0; dy <= 1; dy++) {
			int rr = dy == 0 ? rad : rad - 1;
			for (int dx = -rr; dx <= rr; dx++) {
				for (int dz = -rr; dz <= rr; dz++) {
					if (dx * dx + dz * dz > rr * rr + rr * 0.6) continue;
					if (chunk.isInside(pos.set(cx + dx, cy + dy, cz + dz)) && level.getBlockState(pos).isAir()) putWorld(level, chunk, cx + dx, cy + dy, cz + dz, leaf);
				}
			}
		}
		for (int i = 0; i < rad * 2; i++) {
			double a = random.nextDouble() * Math.PI * 2;
			int dx = (int) Math.round(Math.cos(a) * rad), dz = (int) Math.round(Math.sin(a) * rad);
			if (chunk.isInside(pos.set(cx + dx, cy - 1, cz + dz)) && level.getBlockState(pos).isAir()) putWorld(level, chunk, cx + dx, cy - 1, cz + dz, leaf);
		}
	}

	/** A garden pine (niwaki): a crooked trunk carrying flat pads of needles at staggered heights. */
	private void pineTree(WorldGenLevel level, BoundingBox chunk, int cx, int y, int cz, RandomSource random) {
		int h = 6 + random.nextInt(3);
		int[][] leans = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		int[] lean = leans[random.nextInt(4)];
		int x = cx, z = cz;
		for (int dy = 1; dy <= h; dy++) {
			if ((dy == 3 || dy == 5) && random.nextFloat() < 0.7f) {
				x += lean[0];
				z += lean[1];
			}
			putWorld(level, chunk, x, y + dy, z, logOf(Blocks.SPRUCE_LOG, Direction.Axis.Y));
		}
		pad(level, chunk, x, y + h + 1, z, 2);
		for (int py : new int[] {h - 3, h - 1}) {
			int[] d = leans[random.nextInt(4)];
			putWorld(level, chunk, x + d[0], y + py, z + d[1], logOf(Blocks.SPRUCE_LOG, d[0] != 0 ? Direction.Axis.X : Direction.Axis.Z));
			pad(level, chunk, x + 2 * d[0], y + py + 1, z + 2 * d[1], 1 + random.nextInt(2));
		}
	}

	private void pad(WorldGenLevel level, BoundingBox chunk, int px, int py, int pz, int rad) {
		BlockState leaf = Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -rad - 1; dx <= rad + 1; dx++) {
			for (int dz = -rad - 1; dz <= rad + 1; dz++) {
				int d2 = dx * dx + dz * dz;
				if (d2 <= (rad + 0.5) * (rad + 0.5) + 1 && chunk.isInside(pos.set(px + dx, py, pz + dz)) && level.getBlockState(pos).isAir()) {
					putWorld(level, chunk, px + dx, py, pz + dz, leaf);
				}
				if (d2 <= (rad - 0.5) * (rad - 0.5) && chunk.isInside(pos.set(px + dx, py + 1, pz + dz)) && level.getBlockState(pos).isAir()) {
					putWorld(level, chunk, px + dx, py + 1, pz + dz, leaf);
				}
			}
		}
	}

	/** A clump of bamboo of mixed heights. */
	private void bambooClump(WorldGenLevel level, BoundingBox chunk, int cx, int y, int cz, RandomSource random) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int i = 0; i < 9; i++) {
			int x = cx + random.nextInt(5) - 2, z = cz + random.nextInt(5) - 2;
			if (!chunk.isInside(pos.set(x, y, z)) || !level.getBlockState(pos).is(Blocks.GRASS_BLOCK) || !level.getBlockState(pos.set(x, y + 1, z)).isAir()) continue;
			int h = 4 + random.nextInt(6);
			for (int dy = 1; dy <= h; dy++) {
				BambooLeaves leaves = dy >= h - 1 ? BambooLeaves.LARGE : dy == h - 2 ? BambooLeaves.SMALL : BambooLeaves.NONE;
				putWorld(level, chunk, x, y + dy, z, Blocks.BAMBOO.defaultBlockState().setValue(BambooStalkBlock.LEAVES, leaves).setValue(BambooStalkBlock.AGE, 1));
			}
		}
	}

	/** A mossy boulder half sunk in the grass. */
	private void rock(WorldGenLevel level, BoundingBox chunk, int cx, int y, int cz, RandomSource random) {
		putWorld(level, chunk, cx, y + 1, cz, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
		for (int[] d : new int[][] {{1, 0}, {0, 1}, {-1, 0}, {0, -1}}) {
			if (random.nextBoolean()) {
				Block slab = random.nextBoolean() ? Blocks.MOSSY_COBBLESTONE_SLAB : Blocks.COBBLESTONE_SLAB;
				putWorld(level, chunk, cx + d[0], y + 1, cz + d[1], slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
			}
		}
	}

	private static BlockState logOf(Block block, Direction.Axis axis) {
		BlockState state = block.defaultBlockState();
		return state.hasProperty(RotatedPillarBlock.AXIS) ? state.setValue(RotatedPillarBlock.AXIS, axis) : state;
	}

	// =====================================================================================================================
	// Gates
	// =====================================================================================================================

	/**
	 * A paifang: the way in, marked but not walled. Two tall red posts on stone feet carry a beam, the sect's plaque and a
	 * second beam under a little tiled roof; the main gate has a lower bay either side under roofs of their own. Lanterns hang
	 * in the passage. Its frame: x across, the posts on z = 1.
	 */
	private void gate(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = width();
		int cx = w / 2;
		boolean big = extra.getBoolean("Big");
		int half = big ? 3 : 2;
		int top = big ? 11 : 9;
		int z = 1;
		java.util.List<Integer> posts = new java.util.ArrayList<>(java.util.List.of(cx - half, cx + half));
		if (big) {
			posts.add(cx - half - 3);
			posts.add(cx + half + 3);
		}
		for (int x : posts) {
			int height = Math.abs(x - cx) == half ? top : 7;
			put(level, chunk, x, 1, z, p.foundation);
			foundation(level, chunk, x, 1, z, p.foundation.defaultBlockState());
			put(level, chunk, x, 1, z - 1, p.baseStair(Direction.NORTH));
			put(level, chunk, x, 1, z + 1, p.baseStair(Direction.SOUTH));
			pillar(level, chunk, p, x, 2, height, z);
		}
		fill(level, chunk, cx - half + 1, top - 2, z, cx + half - 1, top - 2, z, p.beam(Direction.Axis.X));
		fill(level, chunk, cx - 1, top - 1, z, cx + 1, top - 1, z, p.accent);
		fill(level, chunk, cx - half - 1, top, z, cx + half + 1, top, z, p.beam(Direction.Axis.X));
		gateRoof(level, chunk, p, cx - half - 2, cx + half + 2, z, top + 1);
		if (big) {
			for (int side : new int[] {-1, 1}) {
				int a = cx + side * half, b = cx + side * (half + 3);
				// From the outer post in to the tall one, not through it.
				int lo = side < 0 ? b : a + 1, hi = side < 0 ? a - 1 : b;
				fill(level, chunk, lo, 6, z, hi, 6, z, p.beam(Direction.Axis.X));
				fill(level, chunk, lo - (side < 0 ? 1 : 0), 7, z, hi + (side > 0 ? 1 : 0), 7, z, p.beam(Direction.Axis.X));
				gateRoof(level, chunk, p, lo - (side < 0 ? 2 : 0), hi + (side > 0 ? 2 : 0), z, 8);
			}
		}
		lanternHanging(level, chunk, p, cx - 1, top - 3, z);
		lanternHanging(level, chunk, p, cx + 1, top - 3, z);
	}

	/** A tiled cap one block deep over a gate's beam: tiles sloping off front and back, a trim ridge, upturned ends. */
	private void gateRoof(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int x1, int z, int y) {
		for (int x = x0; x <= x1; x++) {
			put(level, chunk, x, y, z - 1, p.roofStair(Direction.SOUTH, false));
			put(level, chunk, x, y, z + 1, p.roofStair(Direction.NORTH, false));
			put(level, chunk, x, y, z, p.roof);
			put(level, chunk, x, y + 1, z, p.trimSlab(false));
		}
		for (int zz : new int[] {z - 1, z + 1}) {
			put(level, chunk, x0, y, zz, p.trimSlab(true));
			put(level, chunk, x1, y, zz, p.trimSlab(true));
		}
		put(level, chunk, x0, y + 1, z, p.trimStair(Direction.WEST, false));
		put(level, chunk, x1, y + 1, z, p.trimStair(Direction.EAST, false));
	}

	/** A point just inside a gate's passage (in its frame). */
	public int[] gateInside() {
		return new int[] {width() / 2, 1, 3};
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
				foundation(level, chunk, x, 0, z, p.foundation.defaultBlockState());
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

	/** Where the walls of a hall stand within its piece. */
	private record Frame(int x0, int z0, int x1, int z1) {
		int cx() { return (x0 + x1) / 2; }
	}

	private Frame frame(int margin, int front) {
		return new Frame(margin, front, width() - 1 - margin, depth() - 1 - margin);
	}

	/**
	 * The Sect Master's hall, at the plaza's edge: a three-high stone terrace with a balustrade and broad steps, a hall of
	 * bays fronted by an open colonnade, and a double-eaved hipped roof (a skirt roof on brackets, a clerestory, the main
	 * roof on brackets of its own). Inside, two rows of pillars lead down a red carpet to the master's dais before a screen.
	 */
	private void mainHall(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int m = 3;
		int x0 = m, x1 = width() - 1 - m;
		int zt0 = 3;
		int floor = 3, top = floor + 8;
		int z0 = zt0 + 3, z1 = depth() - 1 - m - 2; // room behind for the back steps
		int cx = (x0 + x1) / 2;
		// The terrace, its balustrade all round, broad steps up the front and narrow ones down the back to the lane.
		int zb = z1 + 2;
		plinth(level, chunk, p, x0 - 1, zt0, x1 + 1, zb, floor, 7);
		for (int x = x0 - 1; x <= x1 + 1; x++) {
			if (Math.abs(x - cx) > 4) put(level, chunk, x, floor + 1, zt0, p.baseWall);
			if (Math.abs(x - cx) > 1) put(level, chunk, x, floor + 1, zb, p.baseWall);
		}
		for (int z = zt0; z <= zb; z++) {
			put(level, chunk, x0 - 1, floor + 1, z, p.baseWall);
			put(level, chunk, x1 + 1, floor + 1, z, p.baseWall);
		}
		for (int i = 0; i < floor; i++) {
			int z = zb + 1 + i;
			for (int x = cx - 1; x <= cx + 1; x++) {
				put(level, chunk, x, floor - i, z, p.baseStair(Direction.SOUTH));
				foundation(level, chunk, x, floor - i, z, p.foundation.defaultBlockState());
			}
			for (int x : new int[] {cx - 2, cx + 2}) {
				fill(level, chunk, x, 1, z, x, floor - i, z, p.foundation);
				foundation(level, chunk, x, 1, z, p.foundation.defaultBlockState());
			}
		}
		int hx0 = x0 + 1, hx1 = x1 - 1;
		interior(level, chunk, p, hx0 + 1, z0 + 1, hx1 - 1, z1 - 1, floor, top + 12);
		walls(level, chunk, p, hx0, z0, hx1, z1, floor + 1, top - 1, 4, 5, 6, false);
		// The colonnade before it, a lattice transom under its beam.
		for (int x : framePosts(hx0, hx1, 5, 4)) pillar(level, chunk, p, x, floor + 1, top - 1, z0 - 2);
		fill(level, chunk, hx0, top, z0 - 2, hx1, top, z0 - 2, p.beam(Direction.Axis.X));
		for (int x = hx0; x <= hx1; x++) put(level, chunk, x, top - 1, z0 - 1, lattice(p, Direction.NORTH));
		fill(level, chunk, hx0, top, z0 - 1, hx1, top, z0 - 1, p.floor);
		// Pillars down the hall, its ceiling.
		for (int z = z0 + 3; z < z1 - 3; z += 4) {
			pillar(level, chunk, p, cx - 5, floor + 1, top - 1, z);
			pillar(level, chunk, p, cx + 5, floor + 1, top - 1, z);
		}
		fill(level, chunk, hx0 + 1, top, z0 + 1, hx1 - 1, top, z1 - 1, p.beam(Direction.Axis.X));
		// The skirt roof, the clerestory, the upper roof.
		brackets(level, chunk, p, hx0, z0 - 2, hx1, z1, top);
		roofHip(level, chunk, p, hx0 - 2, z0 - 4, hx1 + 2, z1 + 2, top + 1, 4, false);
		int cx0 = hx0 + 3, cz0 = z0 + 1, cx1 = hx1 - 3, cz1 = z1 - 3;
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
				put(level, chunk, x, top + 5, z, p.beam(edgeZ ? Direction.Axis.X : Direction.Axis.Z));
			}
		}
		brackets(level, chunk, p, cx0, cz0, cx1, cz1, top + 5);
		roofHip(level, chunk, p, cx0 - 2, cz0 - 2, cx1 + 2, cz1 + 2, top + 6, 99, true);
		// The carpet and the dais.
		for (int z = z0 - 2; z < z1 - 5; z++) fill(level, chunk, cx - 1, floor + 1, z, cx + 1, floor + 1, z, p.carpet);
		int dz = z1 - 5;
		fill(level, chunk, cx - 4, floor + 1, dz, cx + 4, floor + 1, z1 - 1, p.foundation); // clear of the pillars at cx +- 5
		for (int x = cx - 4; x <= cx + 4; x++) put(level, chunk, x, floor + 1, dz - 1, p.baseStair(Direction.NORTH));
		fill(level, chunk, cx - 3, floor + 2, dz + 1, cx + 3, floor + 2, z1 - 1, p.accent);
		for (int x = cx - 3; x <= cx + 3; x++) put(level, chunk, x, floor + 2, dz, p.baseStair(Direction.NORTH));
		int[] seat = masterSeat();
		put(level, chunk, seat[0], seat[1], seat[2], ModBlocks.RED_MEDITATION_MAT);
		// The screen behind the seat.
		fill(level, chunk, cx - 3, floor + 3, z1 - 1, cx + 3, floor + 6, z1 - 1, p.beam(Direction.Axis.X));
		fill(level, chunk, cx - 2, floor + 4, z1 - 1, cx + 2, floor + 5, z1 - 1, p.roofTrim);
		wallBanner(level, chunk, p, cx, floor + 6, z1 - 2, Direction.SOUTH);
		for (int x : new int[] {cx - 3, cx + 3}) {
			put(level, chunk, x, floor + 3, dz + 1, p.window);
			lanternStanding(level, chunk, p, x, floor + 4, dz + 1);
		}
		candles(level, chunk, p, cx - 2, floor + 3, z1 - 2, 4);
		candles(level, chunk, p, cx + 2, floor + 3, z1 - 2, 4);
		// Drums by the door; lanterns down the hall and under the colonnade; the master's own chests by the screen.
		put(level, chunk, hx0 + 2, floor + 1, z0 + 1, Blocks.NOTE_BLOCK);
		put(level, chunk, hx1 - 2, floor + 1, z0 + 1, Blocks.NOTE_BLOCK);
		for (int z = z0 + 2; z < z1 - 3; z += 4) {
			lanternHanging(level, chunk, p, cx - 5, top - 1, z + 2);
			lanternHanging(level, chunk, p, cx + 5, top - 1, z + 2);
		}
		lanternHanging(level, chunk, p, cx - 2, top - 1, z0 - 2); // from the colonnade's beam, either side of the door
		lanternHanging(level, chunk, p, cx + 2, top - 1, z0 - 2);
		chest(level, chunk, hx0 + 1, floor + 1, z1 - 1, Direction.SOUTH, ELDER_LOOT);
		chest(level, chunk, hx1 - 1, floor + 1, z1 - 1, Direction.SOUTH, ELDER_LOOT);
		// Censers smoking either side of the steps.
		for (int x : new int[] {cx - 6, cx + 6}) {
			put(level, chunk, x, 1, 1, p.foundation);
			put(level, chunk, x, 2, 1, Blocks.CAMPFIRE);
		}
	}

	/** The Sect Master's mat on the dais, {x, y, z} in the main hall's frame. */
	public int[] masterSeat() {
		int m = 3;
		Frame f = new Frame(m, m + 3, width() - 1 - m, depth() - 1 - m - 2);
		return new int[] {f.cx(), 3 + 3, f.z1 - 3};
	}

	/**
	 * The meditation hall: an open hall of lattice and plaster on a low plinth under one broad hipped roof, rows of meditation
	 * mats for the inner disciples, and at its back an altar with candles and a pedestal of aged ginseng whose qi helps those
	 * who sit near it.
	 */
	private void meditationHall(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		Frame f = frame(2, 2);
		int floor = 1, top = floor + 5;
		plinth(level, chunk, p, f.x0 - 1, f.z0, f.x1 + 1, f.z1 + 1, floor, 5);
		interior(level, chunk, p, f.x0 + 1, f.z0 + 1, f.x1 - 1, f.z1 - 1, floor, top + 6);
		walls(level, chunk, p, f.x0, f.z0, f.x1, f.z1, floor + 1, top - 1, 4, 3, 4, false);
		fill(level, chunk, f.x0 + 1, top, f.z0 + 1, f.x1 - 1, top, f.z1 - 1, p.floor); // ceiling
		brackets(level, chunk, p, f.x0, f.z0, f.x1, f.z1, top);
		roofHip(level, chunk, p, f.x0 - 2, f.z0 - 2, f.x1 + 2, f.z1 + 2, top + 1, 99, true);
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
		for (int k = 0; k < (f.x1 - f.x0) / 2; k += 4) {
			for (int x : new int[] {f.cx() - k, f.cx() + k}) {
				if (x >= f.x0 + 1 && x <= f.x1 - 1) lanternHanging(level, chunk, p, x, top - 1, (f.z0 + f.z1) / 2);
			}
		}
		lanternHanging(level, chunk, p, f.x0 - 1, top, f.z0 - 1);
		lanternHanging(level, chunk, p, f.x1 + 1, top, f.z0 - 1);
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
	 * The treasury: a stone strongroom on a high plinth, its walls stone between the posts with barred windows, under a hipped roof;
	 * pedestals down both sides bearing the sect's treasures (old fruit, Spirit Ginseng, fine pills and rings), chests of the
	 * sect's wealth at the back, red mats where the elders stand guard, stone lanterns before the steps.
	 */
	private void treasury(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		Frame f = frame(2, 3);
		int floor = 2, top = floor + 5;
		plinth(level, chunk, p, f.x0 - 1, f.z0, f.x1 + 1, f.z1 + 1, floor, 3);
		interior(level, chunk, p, f.x0 + 1, f.z0 + 1, f.x1 - 1, f.z1 - 1, floor, top + 6);
		walls(level, chunk, p, f.x0, f.z0, f.x1, f.z1, floor + 1, top - 1, 4, 3, 3, true, p.foundation);
		fill(level, chunk, f.x0 + 1, top, f.z0 + 1, f.x1 - 1, top, f.z1 - 1, p.floor);
		brackets(level, chunk, p, f.x0, f.z0, f.x1, f.z1, top);
		roofHip(level, chunk, p, f.x0 - 2, f.z0 - 2, f.x1 + 2, f.z1 + 2, top + 1, 99, true);
		RandomSource random = rand(3, 3, 11);
		int[][] spots = treasurePedestals();
		for (int i = 0; i < spots.length; i++) pedestal(level, chunk, spots[i][0], floor + 1, spots[i][1], treasure(random, i));
		for (int x = f.cx() - 2; x <= f.cx() + 2; x += 2) chest(level, chunk, x, floor + 1, f.z1 - 1, Direction.SOUTH, TREASURY_LOOT);
		for (int z = f.z0 + 1; z < f.z1 - 1; z++) put(level, chunk, f.cx(), floor + 1, z, p.carpetAlt);
		lanternHanging(level, chunk, p, f.cx(), top - 1, (f.z0 + f.z1) / 2);
		lanternHanging(level, chunk, p, f.cx(), top - 1, f.z1 - 2);
		for (int[] guard : treasuryGuards()) put(level, chunk, guard[0], guard[1], guard[2], ModBlocks.RED_MEDITATION_MAT);
		for (int x : new int[] {f.cx() - 2, f.cx() + 2}) stoneLantern(level, chunk, p, x, 1, 0);
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

	/** The elders' posts by the treasury: either side of its steps outside, and inside by the chests. {x, y, z} */
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
	 * The pagoda: {@code Tiers} square storeys on a stone base, narrower every second floor, each with lattice windows and its
	 * own flared eave on brackets with lanterns at the corners and lanterns lighting every floor inside, crowned by a pointed roof and a finial. The ground floor is the
	 * sect's library; a ladder climbs to a quiet meditation chamber at the top.
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
			walls(level, chunk, p, x0, x0, x1, x1, base, base + 2, Math.max(2, (x1 - x0) / 2), tier == 0 ? 3 : 0, 3, false);
			brackets(level, chunk, p, x0, x0, x1, x1, base + 3);
			roofHip(level, chunk, p, x0 - 2, x0 - 2, x1 + 2, x1 + 2, base + 4, 3, false);
			// A ceiling (the next storey's floor) with lanterns hung from it in the corners and the middle; more under the eaves.
			fill(level, chunk, x0, base + 4, x0, x1, base + 4, x1, p.floor);
			lanternHanging(level, chunk, p, (x0 + x1) / 2, base + 3, (x0 + x1) / 2);
			for (int[] c : new int[][] {{x0 + 1, x0 + 1}, {x1 - 1, x0 + 1}, {x0 + 1, x1 - 1}, {x1 - 1, x1 - 1}}) {
				lanternHanging(level, chunk, p, c[0], base + 3, c[1]);
			}
			for (int[] c : new int[][] {{x0 - 1, x0 - 1}, {x1 + 1, x0 - 1}, {x0 - 1, x1 + 1}, {x1 + 1, x1 + 1}}) {
				lanternHanging(level, chunk, p, c[0], base + 3, c[1]);
			}
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
		// The top storey: a meditation mat under the crowning roof.
		int topInset = (tiers - 1) / 2;
		int tx0 = m + topInset, tx1 = w - 1 - m - topInset;
		put(level, chunk, (tx0 + tx1) / 2, topFloor, (tx0 + tx1) / 2 - 1, ModBlocks.RED_MEDITATION_MAT);
		int ridge = roofHip(level, chunk, p, tx0 - 2, tx0 - 2, tx1 + 2, tx1 + 2, base - 1, 99, true);
		int cx = w / 2;
		put(level, chunk, cx, ridge, cx, p.accent);
		put(level, chunk, cx, ridge + 1, cx, p.window);
		put(level, chunk, cx, ridge + 2, cx, Blocks.LIGHTNING_ROD);
	}

	/** The meditation mat at the top of the pagoda, {x, y, z}. */
	public int[] pagodaSeat() {
		int tiers = extra.getInt("Tiers");
		int m = 2, w = width();
		int topInset = (tiers - 1) / 2;
		int tx0 = m + topInset, tx1 = w - 1 - m - topInset;
		return new int[] {(tx0 + tx1) / 2, 1 + 5 * (tiers - 1), (tx0 + tx1) / 2 - 1};
	}

	// =====================================================================================================================
	// Gardens, the pond and its pavilion
	// =====================================================================================================================

	/**
	 * An herb garden: plots of good soil between gravel walks inside a bamboo fence, where the sect grows its ginseng (now
	 * and then a Spirit Ginseng), lingzhi and huangjing, all a good deal older than the wild, and a Spirit Peach Tree in fruit.
	 */
	private void garden(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = width(), d = depth();
		RandomSource random = rand(5, 5, 23);
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				boolean edge = x == 0 || x == w - 1 || z == 0 || z == d - 1;
				boolean walk = x == w / 2 || z % 4 == 0;
				put(level, chunk, x, 0, z, edge || walk ? Blocks.GRAVEL : (rand(x, z, 1).nextInt(4) == 0 ? Blocks.PODZOL : Blocks.GRASS_BLOCK));
				air(level, chunk, x, 1, z, x, 9, z);
				if (edge && !(z == 0 && Math.abs(x - w / 2) <= 1)) put(level, chunk, x, 1, z, Blocks.BAMBOO_FENCE);
			}
		}
		int tx = w * 3 / 4, tz = d - 4;
		for (int x = 1; x < w - 1; x++) {
			for (int z = 1; z < d - 1; z++) {
				if (x == w / 2 || z % 4 == 0 || Math.abs(x - tx) <= 1 && Math.abs(z - tz) <= 1) continue;
				RandomSource spot = rand(x, z, 2);
				if (spot.nextFloat() >= 0.55f) continue;
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
		peachTree(level, chunk, tx, tz, random);
	}

	/** A Spirit Peach Tree (its heart keeps its age, see SpiritPeachTree), centuries old, with fruit hanging under its leaves. */
	private void peachTree(WorldGenLevel level, BoundingBox chunk, int cx, int cz, RandomSource random) {
		int treeAge = 200 + (int) (Math.pow(random.nextFloat(), 2) * 2800);
		for (int x = cx - 1; x <= cx + 1; x++) {
			for (int z = cz - 1; z <= cz + 1; z++) {
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

	/** A lotus pond with an uneven shore of grass, moss and gravel, deeper in the middle, lily pads and a Spirit Lotus or two. */
	private void pond(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = width(), d = depth();
		double cx = w / 2.0 - 0.5, cz = d / 2.0 - 0.5, rx = w / 2.0 - 1.5, rz = d / 2.0 - 1.5;
		int phase = (int) Math.floorMod(seed, 7L);
		BlockState water = Blocks.WATER.defaultBlockState();
		for (int x = 0; x < w; x++) {
			for (int z = 0; z < d; z++) {
				double e = Math.pow((x - cx) / rx, 2) + Math.pow((z - cz) / rz, 2) + 0.12 * Math.sin(x * 0.9 + phase) + 0.1 * Math.cos(z * 1.1);
				if (e < 1.0) {
					boolean deep = e < 0.55;
					put(level, chunk, x, deep ? -2 : -1, z, Blocks.CLAY);
					if (deep) put(level, chunk, x, -1, z, water);
					put(level, chunk, x, 0, z, water);
					air(level, chunk, x, 1, z, x, 6, z);
					double s = rand(x, z, 4).nextDouble();
					if (s < 0.06) put(level, chunk, x, 1, z, Blocks.LILY_PAD);
					else if (s < 0.09 && deep) herb(level, chunk, ModBlocks.SPIRIT_LOTUS, x, 1, z, 50 + rand(x, z, 6).nextInt(500));
				} else if (e < 1.35) {
					double s = rand(x, z, 5).nextDouble();
					put(level, chunk, x, 0, z, s > 0.3 ? Blocks.GRASS_BLOCK : s > 0.15 ? Blocks.MOSSY_COBBLESTONE : Blocks.GRAVEL);
					air(level, chunk, x, 1, z, x, 6, z);
					if (s < 0.07) put(level, chunk, x, 1, z, Blocks.MOSSY_COBBLESTONE);
				}
			}
		}
	}

	/** A ting: four posts on a stone floor, benches between three of them, lattice under the beams, a pointed roof with a finial. */
	private void pavilion(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int w = width();
		int a = 2, b = w - 3;
		fill(level, chunk, a, 1, a, b, 1, b, p.foundation);
		for (int x = a; x <= b; x++) {
			for (int z = a; z <= b; z++) foundation(level, chunk, x, 1, z, p.foundation.defaultBlockState());
		}
		fill(level, chunk, a + 1, 1, a + 1, b - 1, 1, b - 1, p.floor);
		air(level, chunk, a, 2, a, b, 8, b);
		put(level, chunk, w / 2, 1, a - 1, p.baseStair(Direction.NORTH));
		for (int[] c : new int[][] {{a, a}, {b, a}, {a, b}, {b, b}}) pillar(level, chunk, p, c[0], 2, 5, c[1]);
		for (int x = a; x <= b; x++) {
			put(level, chunk, x, 6, a, p.beam(Direction.Axis.X));
			put(level, chunk, x, 6, b, p.beam(Direction.Axis.X));
		}
		for (int z = a + 1; z < b; z++) {
			put(level, chunk, a, 6, z, p.beam(Direction.Axis.Z));
			put(level, chunk, b, 6, z, p.beam(Direction.Axis.Z));
		}
		for (int x = a + 1; x < b; x++) {
			put(level, chunk, x, 2, b, p.deckSlab(false));
			put(level, chunk, x, 5, a, lattice(p, Direction.NORTH));
			put(level, chunk, x, 5, b, lattice(p, Direction.SOUTH));
		}
		for (int z = a + 1; z < b; z++) {
			put(level, chunk, a, 2, z, p.deckSlab(false));
			put(level, chunk, b, 2, z, p.deckSlab(false));
			put(level, chunk, a, 5, z, lattice(p, Direction.EAST));
			put(level, chunk, b, 5, z, lattice(p, Direction.WEST));
		}
		brackets(level, chunk, p, a, a, b, b, 6);
		int ridge = roofHip(level, chunk, p, a - 2, a - 2, b + 2, b + 2, 7, 99, true);
		fill(level, chunk, a, 6, w / 2, b, 6, w / 2, p.beam(Direction.Axis.X));
		lanternHanging(level, chunk, p, w / 2, 5, w / 2);
		put(level, chunk, w / 2, ridge, w / 2, p.accent);
		put(level, chunk, w / 2, ridge + 1, w / 2, p.window);
	}

	/** The middle of the pavilion's floor, {x, y, z}. */
	public int[] pavilionSeat() {
		return new int[] {width() / 2, 2, width() / 2};
	}

	// =====================================================================================================================
	// Homes
	// =====================================================================================================================

	/**
	 * A house raised on stones: a stone sill under the walls, flush with the floor (so a doorway has a threshold, not a slot);
	 * the floor carried out one block all round as a veranda of planks on stone feet; and {@code step}-wide steps up to it at
	 * the front middle, the top one let into the veranda.
	 */
	private void homeShell(WorldGenLevel level, BoundingBox chunk, SectPalette p, int x0, int z0, int x1, int z1, int floor, int wallTop,
			int step) {
		int ex0 = x0 - 1, ez0 = z0 - 1, ex1 = x1 + 1, ez1 = z1 + 1;
		BlockState stone = p.foundation.defaultBlockState();
		java.util.SortedSet<Integer> feetX = framePosts(ex0, ex1, 0, 3), feetZ = framePosts(ez0, ez1, 0, 3);
		for (int x = ex0; x <= ex1; x++) {
			for (int z = ez0; z <= ez1; z++) {
				boolean onWall = (x == x0 || x == x1) && z >= z0 && z <= z1 || (z == z0 || z == z1) && x >= x0 && x <= x1;
				boolean rim = x == ex0 || x == ex1 || z == ez0 || z == ez1;
				if (onWall) {
					fill(level, chunk, x, 1, z, x, floor, z, stone);
					foundation(level, chunk, x, 1, z, stone);
				} else if (rim) {
					boolean foot = (z == ez0 || z == ez1) && feetX.contains(x) || (x == ex0 || x == ex1) && feetZ.contains(z);
					if (foot) {
						fill(level, chunk, x, 1, z, x, floor - 1, z, stone);
						foundation(level, chunk, x, 1, z, stone);
					}
					put(level, chunk, x, floor, z, p.deck);
				}
			}
		}
		interior(level, chunk, p, x0 + 1, z0 + 1, x1 - 1, z1 - 1, floor, wallTop + 4);
		int cx = (x0 + x1) / 2;
		for (int x = cx - step / 2; x <= cx + step / 2; x++) {
			fill(level, chunk, x, 1, ez0, x, floor - 1, ez0, stone);
			put(level, chunk, x, floor, ez0, p.baseStair(Direction.NORTH));
			put(level, chunk, x, floor - 1, ez0 - 1, p.baseStair(Direction.NORTH));
			foundation(level, chunk, x, floor - 1, ez0 - 1, stone);
		}
	}

	/** An elder's house: raised on stones with a veranda all round, a hipped roof with swept eaves, a stone lantern by the step. */
	private void elderHouse(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int x0 = 2, z0 = 3, x1 = width() - 3, z1 = depth() - 3;
		int floor = 2, top = 7;
		int cx = (x0 + x1) / 2;
		homeShell(level, chunk, p, x0, z0, x1, z1, floor, top, 3);
		walls(level, chunk, p, x0, z0, x1, z1, floor + 1, top - 1, 3, 3, 3, false);
		fill(level, chunk, x0 + 1, top, z0 + 1, x1 - 1, top, z1 - 1, p.floor);
		brackets(level, chunk, p, x0, z0, x1, z1, top);
		roofHip(level, chunk, p, x0 - 2, z0 - 2, x1 + 2, z1 + 2, top + 1, 99, true);
		for (int[] mat : homeMats()) put(level, chunk, mat[0], mat[1], mat[2], ModBlocks.RED_MEDITATION_MAT);
		chest(level, chunk, x1 - 1, floor + 1, z1 - 1, Direction.WEST, ELDER_LOOT);
		put(level, chunk, x0 + 1, floor + 1, z1 - 1, Blocks.BOOKSHELF);
		put(level, chunk, x0 + 1, floor + 2, z1 - 1, Blocks.POTTED_BAMBOO);
		candles(level, chunk, p, x1 - 1, floor + 2, z1 - 1, 2);
		lanternHanging(level, chunk, p, cx, top - 1, (z0 + z1) / 2 + 1);
		lanternHanging(level, chunk, p, x0 - 1, top, z0 - 1);
		lanternHanging(level, chunk, p, x1 + 1, top, z0 - 1);
		stoneLantern(level, chunk, p, x0 - 1, 1, 0);
		stoneLantern(level, chunk, p, x1 + 1, 1, 0);
		put(level, chunk, x1 - 1, floor + 1, z0 + 1, Blocks.POTTED_AZALEA);
	}

	/**
	 * Disciples' houses and the outer disciples' dormitory: raised on stones with a veranda round them, a gable roof with its
	 * ends in plaster; a mat for each who lives there ({@code Mats}: two in a house, four in a dormitory).
	 */
	private void dwelling(WorldGenLevel level, BoundingBox chunk, SectPalette p) {
		int x0 = 1, z0 = 2, x1 = width() - 2, z1 = depth() - 3;
		int floor = 2, top = 6;
		int cx = (x0 + x1) / 2;
		homeShell(level, chunk, p, x0, z0, x1, z1, floor, top, 1);
		walls(level, chunk, p, x0, z0, x1, z1, floor + 1, top - 1, 3, 1, 3, false);
		fill(level, chunk, x0 + 1, top, z0 + 1, x1 - 1, top, z1 - 1, p.floor);
		int ridge = roofGable(level, chunk, p, x0 - 1, z0 - 2, x1 + 1, z1 + 2, top + 1);
		gableEnds(level, chunk, p, x0, x1, z0, z1, top + 1, ridge);
		for (int[] mat : homeMats()) put(level, chunk, mat[0], mat[1], mat[2], ModBlocks.MEDITATION_MAT);
		chest(level, chunk, x1 - 1, floor + 1, z1 - 1, Direction.WEST, DWELLING_LOOT);
		put(level, chunk, x0 + 1, floor + 1, z1 - 1, Blocks.POTTED_FERN);
		put(level, chunk, x0 + 1, floor + 1, z0 + 1, Blocks.BARREL);
		lanternHanging(level, chunk, p, cx, top - 1, (z0 + z1) / 2 + 1);
		lanternHanging(level, chunk, p, cx - 2, top, z0 - 1);
		lanternHanging(level, chunk, p, cx + 2, top, z0 - 1);
	}

	/** The mats of a home, {x, y, z} each, in its frame: one in an elder's house, else {@code Mats} in a row. */
	public int[][] homeMats() {
		if (kind == Kind.ELDER_HOUSE) {
			int x0 = 2, z0 = 3, x1 = width() - 3, z1 = depth() - 3;
			return new int[][] {{(x0 + x1) / 2, 3, (z0 + z1) / 2 + 1}};
		}
		int x0 = 1, z0 = 2, x1 = width() - 2, z1 = depth() - 3;
		int n = Math.max(1, extra.getInt("Mats"));
		int zc = (z0 + z1) / 2 + 1;
		int[][] mats = new int[n][];
		if (n <= 2) {
			mats[0] = new int[] {x0 + 2, 3, zc};
			if (n == 2) mats[1] = new int[] {x1 - 2, 3, zc};
		} else {
			for (int i = 0; i < n; i++) mats[i] = new int[] {x0 + 2 + 3 * i, 3, zc};
		}
		return mats;
	}
}
