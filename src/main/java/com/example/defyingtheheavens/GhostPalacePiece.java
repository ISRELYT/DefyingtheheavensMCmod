package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A Ghost King's palace (GhostPalaceStructure), built in code chunk by chunk: a platform {@link #HALF} * 2 + 1 blocks square of
 * blackstone, levelled (cut away above, filled below, or resting on pillars of basalt and bone over a deep drop); a wall round
 * it with bone towers at its corners and a gate of bone pillars hung with soul lanterns at its front; a courtyard with braziers
 * of soul fire and beds of red spider lilies either side of the way in; and at its back the hall: on a raised floor, red
 * columns under gold capitals, walls of black stone barred with iron, a double roof of deepslate tiles with upturned eaves and a
 * red band between its tiers, a red carpet up the middle to a dais and the king's throne set in the back wall.
 * <p>
 * Its king (Core Formation, or Nascent Soul, now and then Heavenly Being, seldom Four Axis: {@link #kingRealm}) stands before
 * the throne, and six Ghost
 * Retainers keep the hall and the gate (GhostCultivatorNpc#becomeKing, #becomeCourtier), each put there as the chunk it stands
 * in is built. The king keeps its realm's foundation (GhostCultivatorNpc#method): the way past that realm for whoever it
 * teaches, or takes it from it.
 * <p>
 * Its own frame: u across (−{@link #HALF} to {@link #HALF}), v from the front (−{@link #HALF}, the gate) to the back; the palace
 * faces {@link #facing} (its gate's side), and "north" in it is toward the front ({@link #dir}).
 */
public class GhostPalacePiece extends StructurePiece {
	/** Half the platform's width: it is 41 blocks square. */
	public static final int HALF = 20;
	/** How deep under the platform its foundation is filled before pillars take over; and how high above it is cleared. */
	private static final int FILL_DEPTH = 24, CLEAR_HEIGHT = 22;
	/** The hall: across u {@link #HALL_U}, v from {@link #HALL_FRONT} to {@link #HALL_BACK}; its walls this high over its floor. */
	private static final int HALL_U = 9, HALL_FRONT = 5, HALL_BACK = 17, HALL_WALL = 7;
	private static final int[] HALL_COLUMNS_U = {-9, -5, 5, 9}, HALL_COLUMNS_V = {5, 9, 13, 17};

	private static final BlockState BRICKS = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
	private static final BlockState POLISHED = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
	private static final BlockState CHISELED = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
	private static final BlockState WALL = Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState();
	private static final BlockState FILL = Blocks.BLACKSTONE.defaultBlockState();
	private static final BlockState BONE = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
	private static final BlockState SOUL_SOIL = Blocks.SOUL_SOIL.defaultBlockState();
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();
	private static final BlockState TILES = Blocks.DEEPSLATE_TILES.defaultBlockState();
	private static final BlockState TILE_STAIRS = Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState();
	private static final BlockState TILE_WALL = Blocks.DEEPSLATE_TILE_WALL.defaultBlockState();
	private static final BlockState RED_COLUMN = Blocks.STRIPPED_CRIMSON_STEM.defaultBlockState();
	private static final BlockState RED_WOOD = Blocks.STRIPPED_CRIMSON_HYPHAE.defaultBlockState();
	private static final BlockState GILDED = Blocks.GILDED_BLACKSTONE.defaultBlockState();
	private static final BlockState HANGING_LANTERN = Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);

	private final int cx, y, cz;
	private final Direction facing;

	public GhostPalacePiece(int cx, int y, int cz, Direction facing) {
		super(ModStructures.GHOST_PALACE_PIECE, 0, new BoundingBox(cx - HALF, y - FILL_DEPTH - 2, cz - HALF, cx + HALF, y + CLEAR_HEIGHT, cz + HALF));
		this.cx = cx;
		this.y = y;
		this.cz = cz;
		this.facing = facing;
	}

	public GhostPalacePiece(CompoundTag tag) {
		super(ModStructures.GHOST_PALACE_PIECE, tag);
		cx = tag.getInt("CX");
		y = tag.getInt("Y");
		cz = tag.getInt("CZ");
		facing = Direction.from2DDataValue(tag.getInt("Facing"));
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("CX", cx);
		tag.putInt("Y", y);
		tag.putInt("CZ", cz);
		tag.putInt("Facing", facing.get2DDataValue());
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox chunk,
							ChunkPos chunkPos, BlockPos pivot) {
		groundwork(level, chunk);
		courtyard(level, chunk);
		outerWall(level, chunk);
		gate(level, chunk);
		hall(level, chunk);
		settle(level, chunk);
		court(level, chunk, random);
	}

	// --- The frame ---

	/** The world position of (u, height, v) in the palace's own frame. */
	BlockPos at(int u, int height, int v) {
		return switch (facing) {
			case NORTH -> new BlockPos(cx + u, height, cz + v);
			case SOUTH -> new BlockPos(cx - u, height, cz - v);
			case WEST -> new BlockPos(cx + v, height, cz - u);
			default -> new BlockPos(cx - v, height, cz + u);
		};
	}

	/** A direction in the palace's frame (north: toward its front) in the world's. */
	Direction dir(Direction local) {
		return switch (local) {
			case NORTH -> facing;
			case SOUTH -> facing.getOpposite();
			case EAST -> facing.getClockWise();
			case WEST -> facing.getCounterClockWise();
			default -> local;
		};
	}

	private void put(WorldGenLevel level, BoundingBox chunk, int u, int height, int v, BlockState state) {
		BlockPos pos = at(u, height, v);
		if (chunk.isInside(pos)) level.setBlock(pos, state, Block.UPDATE_CLIENTS);
	}

	/** A soul torch on a wall, pointing {@code local} (away from the wall it hangs on). */
	private BlockState soulTorch(Direction local) {
		return Blocks.SOUL_WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, dir(local));
	}

	private BlockState stair(Direction local) {
		return Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, dir(local));
	}

	// --- The platform ---

	/** Clears the platform's footprint above, lays its floor, and fills it down to the ground (pillars over a deep drop). */
	private void groundwork(WorldGenLevel level, BoundingBox chunk) {
		for (int u = -HALF; u <= HALF; u++) {
			for (int v = -HALF; v <= HALF; v++) {
				BlockPos column = at(u, y, v);
				if (!chunk.isInside(column)) continue;
				for (int dy = 0; dy <= CLEAR_HEIGHT; dy++) {
					BlockPos pos = column.atY(y + dy);
					if (!level.getBlockState(pos).isAir()) level.setBlock(pos, AIR, Block.UPDATE_CLIENTS);
				}
				level.setBlock(column.atY(y - 1), BRICKS, Block.UPDATE_CLIENTS);
				level.setBlock(column.atY(y - 2), FILL, Block.UPDATE_CLIENTS);
				int depth = 0;
				while (depth < FILL_DEPTH && !solid(level, column.atY(y - 3 - depth)) && y - 3 - depth > level.getMinBuildHeight()) depth++;
				if (depth < FILL_DEPTH) {
					for (int d = 0; d < depth; d++) level.setBlock(column.atY(y - 3 - d), FILL, Block.UPDATE_CLIENTS);
				} else if (Math.floorMod(u, 5) == 0 && Math.floorMod(v, 5) == 0) {
					for (int d = 0; y - 3 - d > level.getMinBuildHeight() + 1 && !solid(level, column.atY(y - 3 - d)); d++) {
						level.setBlock(column.atY(y - 3 - d), (d / 4) % 2 == 0 ? Blocks.BASALT.defaultBlockState() : BONE, Block.UPDATE_CLIENTS);
					}
				}
			}
		}
	}

	private static boolean solid(WorldGenLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return !state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced();
	}

	// --- The courtyard, its wall and its gate ---

	/** The way in up the middle, beds of red spider lilies either side, braziers of soul fire. */
	private void courtyard(WorldGenLevel level, BoundingBox chunk) {
		for (int u = -HALF; u <= HALF; u++) {
			for (int v = -HALF; v <= HALF; v++) {
				boolean edge = Math.abs(u) == HALF || Math.abs(v) == HALF;
				boolean way = Math.abs(u) <= 1 && v < HALL_FRONT - 1;
				if (edge || way) put(level, chunk, u, y - 1, v, way && Math.floorMod(v, 4) == 0 ? CHISELED : POLISHED);
			}
		}
		for (int side : new int[] {-1, 1}) {
			for (int u = 7; u <= 13; u++) {
				for (int v = -15; v <= -5; v++) {
					boolean rim = u == 7 || u == 13 || v == -15 || v == -5;
					put(level, chunk, side * u, y - 1, v, rim ? POLISHED : SOUL_SOIL);
					if (!rim && (u + v) % 2 == 0) put(level, chunk, side * u, y, v, ModBlocks.RED_SPIDER_LILY.defaultBlockState());
				}
			}
			for (int v : new int[] {-12, -2}) brazier(level, chunk, side * 5, v);
		}
	}

	private void brazier(WorldGenLevel level, BoundingBox chunk, int u, int v) {
		put(level, chunk, u, y, v, WALL);
		put(level, chunk, u, y + 1, v, SOUL_SOIL);
		put(level, chunk, u, y + 2, v, Blocks.SOUL_FIRE.defaultBlockState());
	}

	/** The wall round the courtyard, crenellated, bone towers at its corners, soul lanterns along its top; the gate's gap. */
	private void outerWall(WorldGenLevel level, BoundingBox chunk) {
		int w = HALF - 1;
		for (int u = -w; u <= w; u++) {
			for (int v = -w; v <= w; v++) {
				if (Math.abs(u) != w && Math.abs(v) != w) continue;
				if (v == -w && Math.abs(u) <= 2) continue; // the gate's gap
				boolean corner = Math.abs(u) == w && Math.abs(v) == w;
				for (int dy = 0; dy < (corner ? 6 : 4); dy++) put(level, chunk, u, y + dy, v, corner ? BONE : BRICKS);
				if (corner) {
					put(level, chunk, u, y + 6, v, CHISELED);
					put(level, chunk, u, y + 7, v, Blocks.SOUL_LANTERN.defaultBlockState());
				} else if (Math.floorMod(u + v, 6) == 0) {
					put(level, chunk, u, y + 4, v, WALL);
					put(level, chunk, u, y + 5, v, Blocks.SOUL_LANTERN.defaultBlockState());
				} else if (Math.floorMod(u + v, 2) == 0) {
					put(level, chunk, u, y + 4, v, WALL);
				}
			}
		}
	}

	/** The gate: two pillars of bone, a lintel of carved black stone over them, soul lanterns hung on chains beneath. */
	private void gate(WorldGenLevel level, BoundingBox chunk) {
		int v0 = -HALF, v1 = -HALF + 1;
		for (int side : new int[] {-3, 3}) {
			for (int v = v0; v <= v1; v++) {
				for (int dy = 0; dy < 6; dy++) put(level, chunk, side, y + dy, v, BONE);
			}
		}
		for (int u = -4; u <= 4; u++) {
			for (int v = v0; v <= v1; v++) {
				put(level, chunk, u, y + 6, v, Math.abs(u) == 4 || u == 0 ? CHISELED : BRICKS);
				put(level, chunk, u, y + 7, v, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState());
			}
		}
		put(level, chunk, 0, y + 7, v0, GILDED);
		for (int u : new int[] {-1, 1}) {
			put(level, chunk, u, y + 5, v1, Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y));
			put(level, chunk, u, y + 4, v1, HANGING_LANTERN);
		}
	}

	// --- The hall ---

	/** The hall: raised floor, columns, walls, beams, its double roof, the carpet, the dais and the throne, lanterns. */
	private void hall(WorldGenLevel level, BoundingBox chunk) {
		int floor = y + 1; // where one stands in the hall
		for (int u = -HALL_U - 1; u <= HALL_U + 1; u++) {
			for (int v = HALL_FRONT - 1; v <= HALL_BACK + 1; v++) {
				boolean rim = Math.abs(u) == HALL_U + 1 || v == HALL_FRONT - 1 || v == HALL_BACK + 1;
				put(level, chunk, u, y, v, rim ? POLISHED : BRICKS);
			}
		}
		for (int u = -2; u <= 2; u++) put(level, chunk, u, y, HALL_FRONT - 2, stair(Direction.SOUTH)); // up to the door
		// The walls: columns, black stone between with iron bars down the sides, the door at the front.
		int beam = floor + HALL_WALL;
		for (int u = -HALL_U; u <= HALL_U; u++) {
			for (int v = HALL_FRONT; v <= HALL_BACK; v++) {
				boolean sideU = Math.abs(u) == HALL_U, endV = v == HALL_FRONT || v == HALL_BACK;
				if (!sideU && !endV) continue;
				boolean column = sideU && contains(HALL_COLUMNS_V, v) || endV && contains(HALL_COLUMNS_U, u);
				for (int dy = 0; dy < HALL_WALL; dy++) {
					BlockState state;
					if (column) state = dy == 0 ? POLISHED : dy == HALL_WALL - 1 ? GILDED : RED_COLUMN;
					else if (dy == 0) state = POLISHED;
					else if (sideU && (v - HALL_FRONT) % 4 == 2 && (dy == 2 || dy == 3)) state = Blocks.IRON_BARS.defaultBlockState();
					else state = BRICKS;
					put(level, chunk, u, floor + dy, v, state);
				}
				put(level, chunk, u, beam, v, RED_WOOD);
			}
		}
		for (int u = -1; u <= 1; u++) {
			for (int dy = 0; dy < 4; dy++) put(level, chunk, u, floor + dy, HALL_FRONT, AIR);
		}
		put(level, chunk, 0, floor + 4, HALL_FRONT, CHISELED);
		// Inside: a red carpet up the middle, black either side, to the dais; the dais in two steps; the throne in the back wall.
		for (int v = HALL_FRONT + 1; v <= 12; v++) {
			put(level, chunk, 0, floor, v, Blocks.RED_CARPET.defaultBlockState());
			put(level, chunk, -1, floor, v, Blocks.BLACK_CARPET.defaultBlockState());
			put(level, chunk, 1, floor, v, Blocks.BLACK_CARPET.defaultBlockState());
		}
		for (int u = -2; u <= 2; u++) put(level, chunk, u, floor, 13, stair(Direction.SOUTH));
		for (int u = -3; u <= 3; u++) {
			for (int v = 14; v <= 16; v++) put(level, chunk, u, floor, v, BRICKS);
		}
		for (int u = -1; u <= 1; u++) put(level, chunk, u, floor + 1, 14, stair(Direction.SOUTH));
		for (int u = -2; u <= 2; u++) {
			for (int v = 15; v <= 16; v++) put(level, chunk, u, floor + 1, v, POLISHED);
		}
		put(level, chunk, 0, floor + 2, HALL_BACK, stair(Direction.SOUTH)); // the seat, its back to the wall
		put(level, chunk, -1, floor + 2, HALL_BACK, WALL);
		put(level, chunk, 1, floor + 2, HALL_BACK, WALL);
		for (int dy = 3; dy <= 5; dy++) put(level, chunk, 0, floor + dy, HALL_BACK, dy == 4 ? GILDED : CHISELED);
		for (int u : new int[] {-2, 2}) put(level, chunk, u, floor + 4, HALL_BACK - 1, Blocks.SOUL_LANTERN.defaultBlockState());
		for (int u : new int[] {-2, 2}) put(level, chunk, u, floor + 3, HALL_BACK - 1, WALL);
		for (int u : new int[] {-5, 5}) {
			for (int v : new int[] {9, 13}) put(level, chunk, u, beam, v, HANGING_LANTERN);
		}
		// Soul torches on the columns' inner faces, down the sides and either side of the door and the throne.
		for (int v : new int[] {9, 13}) {
			put(level, chunk, -HALL_U + 1, floor + 3, v, soulTorch(Direction.EAST));
			put(level, chunk, HALL_U - 1, floor + 3, v, soulTorch(Direction.WEST));
		}
		for (int u : new int[] {-5, 5}) {
			put(level, chunk, u, floor + 3, HALL_FRONT + 1, soulTorch(Direction.SOUTH));
			put(level, chunk, u, floor + 3, HALL_BACK - 1, soulTorch(Direction.NORTH));
		}
		// The roof: the lower eaves two out from the walls, a ledge, the red band, the upper tier up to its ridge.
		int eave = beam + 1;
		roofRing(level, chunk, eave, -HALL_U - 2, HALL_U + 2, HALL_FRONT - 2, HALL_BACK + 2);
		roofRing(level, chunk, eave + 1, -HALL_U - 1, HALL_U + 1, HALL_FRONT - 1, HALL_BACK + 1);
		for (int u = -HALL_U + 1; u <= HALL_U - 1; u++) {
			for (int v = HALL_FRONT + 1; v <= HALL_BACK - 1; v++) {
				boolean side = Math.abs(u) == HALL_U - 1, end = v == HALL_FRONT + 1 || v == HALL_BACK - 1;
				if (side || end) put(level, chunk, u, eave + 2, v, side && end ? GILDED : RED_WOOD);
			}
		}
		int top = eave + 3;
		for (int k = 0; ; k++) {
			int u0 = -HALL_U + k, u1 = HALL_U - k, v0 = HALL_FRONT + k, v1 = HALL_BACK - k;
			if (v1 - v0 < 2) {
				for (int u = u0 + 1; u <= u1 - 1; u++) put(level, chunk, u, top + k, (v0 + v1) / 2, TILE_WALL); // the ridge
				put(level, chunk, 0, top + k + 1, (v0 + v1) / 2, Blocks.SOUL_LANTERN.defaultBlockState());
				break;
			}
			roofRing(level, chunk, top + k, u0, u1, v0, v1);
		}
		// The eaves' corners turned up, both tiers.
		for (int su : new int[] {-1, 1}) {
			for (int sv : new int[] {0, 1}) {
				put(level, chunk, su * (HALL_U + 2), eave + 1, sv == 0 ? HALL_FRONT - 2 : HALL_BACK + 2, TILE_WALL);
				put(level, chunk, su * HALL_U, top + 1, sv == 0 ? HALL_FRONT : HALL_BACK, TILE_WALL);
			}
		}
	}

	/** One course of a hipped roof: tile stairs round u0..u1, v0..v1 at {@code height}, each rising inward, tiles laid within. */
	private void roofRing(WorldGenLevel level, BoundingBox chunk, int height, int u0, int u1, int v0, int v1) {
		for (int u = u0; u <= u1; u++) {
			for (int v = v0; v <= v1; v++) {
				if (u > u0 && u < u1 && v > v0 && v < v1) {
					put(level, chunk, u, height, v, TILES);
					continue;
				}
				Direction inward = v == v0 ? Direction.SOUTH : v == v1 ? Direction.NORTH : u == u0 ? Direction.EAST : Direction.WEST;
				put(level, chunk, u, height, v, TILE_STAIRS.setValue(StairBlock.FACING, dir(inward)));
			}
		}
	}

	private static boolean contains(int[] values, int value) {
		for (int x : values) if (x == value) return true;
		return false;
	}

	/** Every stair and wall in this chunk of the palace shaped by what is beside it (corners turned, walls joined). */
	private void settle(WorldGenLevel level, BoundingBox chunk) {
		for (int u = -HALF; u <= HALF; u++) {
			for (int v = -HALF; v <= HALF; v++) {
				BlockPos column = at(u, y, v);
				if (!chunk.isInside(column)) continue;
				for (int dy = 0; dy <= CLEAR_HEIGHT; dy++) {
					BlockPos pos = column.atY(y + dy);
					BlockState state = level.getBlockState(pos);
					if (!(state.getBlock() instanceof StairBlock) && !(state.getBlock() instanceof WallBlock)) continue;
					BlockState settled = Block.updateFromNeighbourShapes(state, level, pos);
					if (settled != state) level.setBlock(pos, settled, Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	// --- Its king and court ---

	/**
	 * How often a palace's king is of each realm past Core Formation (the rest Core Formation), and so which foundation it has to
	 * teach, sell and leave: Four Axis one palace in twenty (the Six Paths Reincarnation Canon), Heavenly Being in about eight
	 * (Yama's Judgement Scripture), Nascent Soul in about four (the Nine Nether Soul Sutra).
	 */
	public static final float FOUR_AXIS_KING = 0.05f, HEAVENLY_BEING_KING = 0.13f, NASCENT_SOUL_KING = 0.27f;

	/** The realm of the king of the palace whose roll (decided by where it stands) is {@code roll}. */
	static Realm kingRealm(float roll) {
		if (roll < FOUR_AXIS_KING) return Realm.FOUR_AXIS;
		if (roll < FOUR_AXIS_KING + HEAVENLY_BEING_KING) return Realm.HEAVENLY_BEING;
		if (roll < FOUR_AXIS_KING + HEAVENLY_BEING_KING + NASCENT_SOUL_KING) return Realm.NASCENT_SOUL;
		return Realm.CORE_FORMATION;
	}

	/** Where the king stands, before the throne on the dais (palace frame: u, height above the platform, v). */
	static final int[] KING = {0, 3, 15};
	/** Where the retainers stand: two pairs down the hall, a pair inside the gate. */
	static final int[][] RETAINERS = {{-4, 1, 8}, {4, 1, 8}, {-4, 1, 12}, {4, 1, 12}, {-3, 0, -16}, {3, 0, -16}};

	/**
	 * The king and its court, each set in its place as the chunk it stands in is built; all on the one path, the palace's own
	 * (drawn from where it stands, so the chunks built apart agree on it).
	 */
	private void court(WorldGenLevel level, BoundingBox chunk, RandomSource random) {
		ServerLevel server = level.getLevel();
		RandomSource palace = RandomSource.create(net.minecraft.util.Mth.getSeed(cx, y, cz));
		int alignment = Alignment.randomRogue(palace);
		Realm realm = kingRealm(palace.nextFloat());
		BlockPos throne = at(KING[0], y + KING[1], KING[2]);
		if (chunk.isInside(throne)) {
			GhostCultivatorNpc king = ModEntities.GHOST_CULTIVATOR.create(server);
			if (king != null) {
				int rank = PlayerCultivation.rank(realm, Stage.EARLY) + random.nextInt(Stage.values().length);
				king.becomeKing(rank, alignment, throne, random);
				place(level, king, throne);
			}
		}
		for (int[] spot : RETAINERS) {
			BlockPos post = at(spot[0], y + spot[1], spot[2]);
			if (!chunk.isInside(post)) continue;
			GhostCultivatorNpc retainer = ModEntities.GHOST_CULTIVATOR.create(server);
			if (retainer == null) continue;
			retainer.becomeCourtier(PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY) + random.nextInt(4), alignment, post, random);
			place(level, retainer, post);
		}
	}

	private void place(WorldGenLevel level, GhostCultivatorNpc npc, BlockPos at) {
		npc.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, facing.toYRot(), 0);
		npc.setYHeadRot(facing.toYRot());
		level.addFreshEntity(npc);	}
}
