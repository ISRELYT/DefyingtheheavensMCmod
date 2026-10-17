package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Ghost Gate (鬼门关), the hub of the Netherworld at 0, 0, where the dead wake and the living arrive. Built in code the
 * first time the server starts with the Netherworld (as the Overworld's rift is placed), and remembered as built
 * ({@link Data}); its fixtures (the way down, the Wheel's hub) are put back on every start if something took them.
 * <p>
 * Laid out north to south, around its floor level Y (where one stands; the paving is at Y-1):
 * <ul>
 *   <li>the <b>arrival plaza</b>, 21 x 21 of polished blackstone bricks, braziers of soul fire at its corners and soul lanterns
 *   round its rim; on its north side the <b>way down</b> (a Yellow Springs rift, see SpatialRiftBlock), its shrine of lanterns;</li>
 *   <li>the <b>Ghost Gate</b> on its south side: two towering pillars of blackstone striped with bone, a lintel 12 blocks up,
 *   chains and soul lanterns hanging from it;</li>
 *   <li>a short way south to the <b>Naihe Bridge</b>, arched over a channel of the <b>Wangchuan</b>, the river of forgetting,
 *   with <b>Meng Po's hall</b> by its near end, on the bank west of the gate (built for her three times a man's height);</li>
 *   <li>on the far bank, the <b>Wheel of Reincarnation</b> on its dais, flanked by braziers;</li>
 *   <li>west of the plaza, the tall <b>Terrace for Gazing Homeward</b>, its ramp rising from the plaza's edge.</li>
 * </ul>
 * The ground is levelled under it all (cleared above, filled below); where it stands over a chasm, its floor rests on pillars
 * of basalt and bone running down into the abyss. Within {@link #GATE_RADIUS} of its middle survival players can't break or
 * place blocks and blasts break nothing.
 */
public final class GhostGate {
	public static final int X = 0, Z = 0;
	/** The protected ground: within this many blocks (sideways) of {@link #CENTER_Z} on the gate's axis. */
	public static final int GATE_RADIUS = 32;
	/** The protected ground's middle, between the plaza and the Wheel, so the whole of the gate is inside it. */
	public static final int CENTER_Z = 13;
	/** The floor level if the land at 0, 0 can't be read (it always can: the terrain keeps it solid, see ModNoiseSettings). */
	private static final int FALLBACK_Y = 110;
	/** How far below the floor the foundation fills down to ground; deeper than this, pillars instead. */
	private static final int FILL_DEPTH = 24;
	/** The site: everything is built within these bounds (relative x, z). */
	private static final int MIN_X = -28, MAX_X = 28, MIN_Z = -14, MAX_Z = 40;
	private static final int CLEAR_HEIGHT = 22;

	// Places, relative to (X, Y, Z) (Y the floor level).
	/** Where the dead reform and ascenders arrive, on the plaza. */
	public static final BlockPos ARRIVAL = new BlockPos(0, 0, 3);
	/** The way down to the Nether: a Yellow Springs rift on the plaza's north side. */
	public static final BlockPos WAY_DOWN = new BlockPos(0, 0, -7);
	/** The Wheel of Reincarnation's hub, high on its dais on the far bank. */
	public static final BlockPos WHEEL = new BlockPos(0, 6, 32);
	/** The top of the Terrace for Gazing Homeward, where its stone stands. */
	public static final BlockPos TERRACE_TOP = new BlockPos(-23, 10, 0);
	/** Meng Po's pot, in her hall by the near end of the bridge (on its raised floor, over a soul campfire), at her side. */
	public static final BlockPos MENG_PO_POT = new BlockPos(-16, 2, 10);
	/**
	 * Where Meng Po stands: in the middle of her hall, facing the way in, her pot at her side (not before her, where its
	 * smoke would rise across her face and plate).
	 */
	public static final BlockPos MENG_PO_POST = new BlockPos(-18, 1, 12);
	/** Where Ox-Head and Horse-Face keep the gate, either side of it. */
	public static final BlockPos OX_HEAD_POST = new BlockPos(-4, 0, 9), HORSE_FACE_POST = new BlockPos(4, 0, 9);
	/** A stele of the Netherworld Breathing Art on the plaza, facing the arrival: the foundation, where every ghost can find it. */
	public static final BlockPos STELE = new BlockPos(6, 0, 3);

	private static final BlockState BRICKS = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
	private static final BlockState POLISHED = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
	private static final BlockState CHISELED = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
	private static final BlockState WALL = Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState();
	private static final BlockState BONE = Blocks.BONE_BLOCK.defaultBlockState();
	private static final BlockState FILL = Blocks.BLACKSTONE.defaultBlockState();
	private static final BlockState SOUL_SOIL = Blocks.SOUL_SOIL.defaultBlockState();
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();

	/** Builds the gate if it isn't yet, and puts back its fixtures. Called when the server starts. */
	public static void ensure(MinecraftServer server) {
		if (server == null) return;
		ServerLevel level = server.getLevel(ModDimensions.NETHERWORLD);
		if (level == null) return;
		try {
			Data data = Data.get(level);
			if (!data.built) {
				int y = findFloor(level);
				long start = System.currentTimeMillis();
				build(level, y);
				data.built = true;
				data.y = y;
				data.pavilion = PAVILION_VERSION;
				data.setDirty();
				DefyingTheHeavens.LOGGER.info("Ghost Gate built at {}, {}, {} in {} ms", X, y, Z, System.currentTimeMillis() - start);
			}
			if (data.pavilion < PAVILION_VERSION) {
				replacePavilion(level, data.y); // Meng Po stands three times a man's height now: her hall, for her
				data.pavilion = PAVILION_VERSION;
				data.setDirty();
			}
			ensureFixtures(level, data.y);
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Could not build the Ghost Gate in the Netherworld", e);
		}
	}

	/** The floor level of a built gate (the level it was built at), or {@link #FALLBACK_Y} before it is built. */
	public static int floorY(ServerLevel netherworld) {
		Data data = Data.get(netherworld);
		return data.built ? data.y : FALLBACK_Y;
	}

	/** A place at the gate, in the world: {@code relative} from (X, floor, Z). */
	public static BlockPos at(ServerLevel netherworld, BlockPos relative) {
		return new BlockPos(X + relative.getX(), floorY(netherworld) + relative.getY(), Z + relative.getZ());
	}

	/** Within the gate's protected ground (any height) in the Netherworld. */
	public static boolean protects(Level level, BlockPos pos) {
		return level != null && ModDimensions.isNetherworld(level.dimension()) && withinRadius(pos, 0);
	}

	/**
	 * The gate's ground is kept as it is: survival and adventure players can't break blocks there, nor place anything (blocks,
	 * fluids, fire, nor strip, till or bone-meal what's there). Creative players can. Blasts are handled by ExplosionMixin.
	 */
	public static void registerProtection() {
		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) -> {
			if (player.isCreative() || !protects(level, pos)) return true;
			player.displayClientMessage(net.minecraft.network.chat.Component.translatable(ModLang.MSG_GATE_PROTECTED), true);
			return false;
		});
		net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			// Meng Po's pot: her soup, for the dead (the living are turned away).
			if (hand == net.minecraft.world.InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp && isMengPoPot(level, hit.getBlockPos())) {
				drinkSoup(sp);
				return net.minecraft.world.InteractionResult.SUCCESS;
			}
			if (player.isCreative() || !protects(level, hit.getBlockPos())) return net.minecraft.world.InteractionResult.PASS;
			net.minecraft.world.item.Item item = player.getItemInHand(hand).getItem();
			boolean alters = item instanceof net.minecraft.world.item.BlockItem || item instanceof net.minecraft.world.item.BucketItem
					|| item instanceof net.minecraft.world.item.FlintAndSteelItem || item instanceof net.minecraft.world.item.FireChargeItem
					|| item instanceof net.minecraft.world.item.BoneMealItem || item instanceof net.minecraft.world.item.DiggerItem;
			if (!alters) return net.minecraft.world.InteractionResult.PASS;
			if (!level.isClientSide) player.displayClientMessage(net.minecraft.network.chat.Component.translatable(ModLang.MSG_GATE_PROTECTED), true);
			return net.minecraft.world.InteractionResult.FAIL;
		});
	}

	/** Takes the gate's protected ground out of a blast (see ExplosionMixin). */
	public static void spareFromBlast(Level level, List<BlockPos> toBlow) {
		if (level != null && ModDimensions.isNetherworld(level.dimension())) toBlow.removeIf(pos -> withinRadius(pos, 0));
	}

	/** Within {@link #GATE_RADIUS} + {@code margin} blocks (sideways) of the gate's middle. */
	public static boolean withinRadius(BlockPos pos, double margin) {
		double dx = pos.getX() + 0.5 - X, dz = pos.getZ() + 0.5 - (Z + CENTER_Z);
		double reach = GATE_RADIUS + margin;
		return dx * dx + dz * dz <= reach * reach;
	}

	/** The ground level at 0, 0: the median of the land under the plaza (trees and lone spires don't count much). */
	private static int findFloor(ServerLevel level) {
		List<Integer> heights = new ArrayList<>();
		for (int dx = -8; dx <= 8; dx += 8) {
			for (int dz = -8; dz <= 8; dz += 8) {
				int x = X + dx, z = Z + dz;
				level.getChunk(x >> 4, z >> 4);
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
				if (y > level.getMinBuildHeight() + 1) heights.add(y);
			}
		}
		if (heights.isEmpty()) return FALLBACK_Y;
		Collections.sort(heights);
		int y = heights.get(heights.size() / 2);
		return Math.max(level.getMinBuildHeight() + 30, Math.min(level.getMaxBuildHeight() - 40, y));
	}

	// --- Building ---

	private static void build(ServerLevel level, int y) {
		for (int cx = (X + MIN_X) >> 4; cx <= (X + MAX_X) >> 4; cx++) {
			for (int cz = (Z + MIN_Z) >> 4; cz <= (Z + MAX_Z) >> 4; cz++) level.getChunk(cx, cz);
		}
		groundwork(level, y);
		plaza(level, y);
		wayDownShrine(level, y);
		gate(level, y);
		channelAndBridge(level, y);
		mengPoHall(level, y);
		wheelDais(level, y);
		terrace(level, y);
		// Plants knocked loose while the ground was laid (the land's own spider lilies and roots) leave no litter behind.
		level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(X + MIN_X - 4, y - FILL_DEPTH - 8,
				Z + MIN_Z - 4, X + MAX_X + 5, y + CLEAR_HEIGHT + 4, Z + MAX_Z + 5)).forEach(net.minecraft.world.entity.Entity::discard);
	}

	/** Clears the site above the floor and lays the floor (soul soil, paved later where paved) on a levelled foundation. */
	private static void groundwork(ServerLevel level, int y) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = MIN_X; x <= MAX_X; x++) {
			for (int z = MIN_Z; z <= MAX_Z; z++) {
				for (int dy = 0; dy <= CLEAR_HEIGHT; dy++) {
					pos.set(X + x, y + dy, Z + z);
					if (!level.getBlockState(pos).isAir()) level.setBlock(pos, AIR, Block.UPDATE_CLIENTS);
				}
				put(level, x, y - 1, z, SOUL_SOIL);
				put(level, x, y - 2, z, FILL);
				// Down to the ground: filled if it's near, otherwise pillars every 5 blocks into the abyss.
				int depth = 0;
				while (depth < FILL_DEPTH && solidAt(level, x, y - 3 - depth, z) == false && y - 3 - depth > level.getMinBuildHeight()) depth++;
				boolean nearGround = depth < FILL_DEPTH;
				boolean pillar = Math.floorMod(x, 5) == 0 && Math.floorMod(z, 5) == 0;
				if (nearGround) {
					for (int d = 0; d < depth; d++) put(level, x, y - 3 - d, z, FILL);
				} else if (pillar) {
					for (int d = 0; y - 3 - d > level.getMinBuildHeight() && !solidAt(level, x, y - 3 - d, z); d++) {
						put(level, x, y - 3 - d, z, (d / 4) % 2 == 0 ? Blocks.BASALT.defaultBlockState() : BONE);
					}
				}
			}
		}
	}

	private static boolean solidAt(ServerLevel level, int x, int y, int z) {
		BlockState state = level.getBlockState(new BlockPos(X + x, y, Z + z));
		return !state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced();
	}

	/** The arrival plaza: bricks with a polished rim and a chiseled heart, braziers at its corners, lanterns round the rim. */
	private static void plaza(ServerLevel level, int y) {
		for (int x = -10; x <= 10; x++) {
			for (int z = -10; z <= 10; z++) {
				boolean rim = Math.abs(x) == 10 || Math.abs(z) == 10;
				boolean heart = Math.abs(x) <= 1 && Math.abs(z - ARRIVAL.getZ()) <= 1;
				boolean ring = Math.abs(x) <= 3 && Math.abs(z - ARRIVAL.getZ()) <= 3 && (Math.abs(x) == 3 || Math.abs(z - ARRIVAL.getZ()) == 3);
				put(level, x, y - 1, z, heart ? CHISELED : rim || ring ? POLISHED : BRICKS);
			}
		}
		for (int sx : new int[] {-8, 8}) {
			for (int sz : new int[] {-8, 8}) brazier(level, sx, y, sz);
		}
		for (int i = -5; i <= 5; i += 5) {
			if (i != 5) lanternPost(level, -10, y, i); // the terrace's ramp leaves the rim there
			lanternPost(level, 10, y, i);
			if (i != 0) lanternPost(level, i, y, -10);
			if (i != 0) lanternPost(level, i, y, 10);
		}
	}

	/** The way down: the Yellow Springs rift, ringed by four lantern posts. */
	private static void wayDownShrine(ServerLevel level, int y) {
		int z = WAY_DOWN.getZ();
		put(level, 0, y - 1, z, CHISELED);
		for (int dx : new int[] {-2, 2}) {
			for (int dz : new int[] {-2, 2}) lanternPost(level, dx, y, z + dz);
		}
		put(level, 0, y, z, ModBlocks.YELLOW_SPRINGS_RIFT.defaultBlockState());
	}

	/** The Ghost Gate: pillars of blackstone striped with bone, a lintel 12 up, chains and soul lanterns hung from it. */
	private static void gate(ServerLevel level, int y) {
		for (int side : new int[] {-6, 6}) pillar(level, y, side);
		for (int x = -8; x <= 8; x++) {
			for (int z = 11; z <= 13; z++) {
				put(level, x, y + 12, z, Math.abs(x) == 8 ? CHISELED : BRICKS);
				put(level, x, y + 13, z, BRICKS);
			}
		}
		for (int x = -9; x <= 9; x++) {
			for (int z = 10; z <= 14; z++) {
				put(level, x, y + 14, z, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState());
			}
		}
		for (int x : new int[] {-9, 9}) {
			put(level, x, y + 14, 12, BONE.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
			put(level, x, y + 15, 12, BONE.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
		}
		// Chains hang from the lintel, soul lanterns at their ends.
		for (int x : new int[] {-3, 0, 3}) {
			for (int dy = 9; dy <= 11; dy++) put(level, x, y + dy, 12, Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y));
			put(level, x, y + 8, 12, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
		}
		// Paved through the gate and on to the bridge.
		for (int x = -4; x <= 4; x++) {
			for (int z = 11; z <= 16; z++) put(level, x, y - 1, z, Math.abs(x) == 4 ? POLISHED : BRICKS);
		}
	}

	/** One of the gate's pillars, three blocks square about x {@code side}, z 12: blackstone striped with bone, 12 high. */
	private static void pillar(ServerLevel level, int y, int side) {
		for (int x = side - 1; x <= side + 1; x++) {
			for (int z = 11; z <= 13; z++) {
				for (int dy = 0; dy < 12; dy++) {
					boolean stripe = (x == side || z == 12) && !(x == side && z == 12) && dy > 0 && dy < 11;
					put(level, x, y + dy, z, stripe ? BONE.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y) : dy == 0 || dy == 11 ? CHISELED : BRICKS);
				}
			}
		}
	}

	/**
	 * A channel of the Wangchuan right across the site (z 17-23, water three deep in a walled bed), and the Naihe Bridge arched
	 * over it: up and down in half-block steps, two blocks high at the middle, railed and lit.
	 */
	private static void channelAndBridge(ServerLevel level, int y) {
		for (int x = MIN_X; x <= MAX_X; x++) {
			for (int z = 16; z <= 24; z++) {
				boolean bank = z == 16 || z == 24 || x == MIN_X || x == MAX_X;
				for (int dy = -5; dy <= -1; dy++) {
					BlockState state = bank ? (dy == -1 ? POLISHED : BRICKS)
							: dy == -5 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
							: dy == -1 ? AIR : Blocks.WATER.defaultBlockState();
					put(level, x, y + dy, z, state);
				}
				if (bank) {
					for (int d = 6; d < FILL_DEPTH && !solidAt(level, x, y - d, z); d++) put(level, x, y - d, z, FILL);
				}
			}
		}
		// The bridge: deck heights in half blocks above the floor, z 16..24.
		int[] halves = {0, 1, 2, 3, 4, 3, 2, 1, 0};
		for (int i = 0; i < halves.length; i++) {
			int z = 16 + i;
			int h = halves[i];
			for (int x = -3; x <= 3; x++) {
				if (z > 16 && z < 24) put(level, x, y - 1, z, BRICKS); // the arch's body over the water
				boolean rail = Math.abs(x) == 3;
				int blockY = y + (h - 1) / 2; // the block whose top is the deck: h even -> a full block, odd -> a bottom slab
				if (h == 0) continue;
				for (int dy = 0; dy < (h + 1) / 2; dy++) put(level, x, y + dy, z, BRICKS);
				if (h % 2 == 1 && !rail) {
					put(level, x, blockY, z, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
				}
			}
		}
		for (int i = 0; i < halves.length; i++) {
			int z = 16 + i;
			int top = y + (halves[i] + 1) / 2; // first free block above the deck at the rails
			for (int x : new int[] {-3, 3}) {
				boolean post = i == 0 || i == 4 || i == halves.length - 1;
				put(level, x, top, z, WALL);
				if (post) put(level, x, top + 1, z, Blocks.SOUL_LANTERN.defaultBlockState());
			}
		}
	}

	/**
	 * What Meng Po has as it is built now (3: her hall); a gate built with one of the old pavilions has it replaced once
	 * (Data#pavilion).
	 */
	private static final int PAVILION_VERSION = 3;

	// Meng Po's hall: its raised floor (relative x, z) and its columns, and how tall they stand on the floor (up to the beams):
	// room under the roof for her, three times a man's height (5.4 blocks), and for her plate and what she says over it.
	private static final int HALL_WEST = -23, HALL_EAST = -11, HALL_NORTH = 8, HALL_SOUTH = 16;
	private static final int[] HALL_COLUMNS_X = {-22, -17, -12};
	private static final int[] HALL_COLUMNS_Z = {9, 15};
	private static final int HALL_COLUMN = 8;

	private static final BlockState TILES = Blocks.DEEPSLATE_TILES.defaultBlockState();
	private static final BlockState TILE_STAIRS = Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState();
	private static final BlockState TILE_WALL = Blocks.DEEPSLATE_TILE_WALL.defaultBlockState();
	private static final BlockState RED_COLUMN = Blocks.STRIPPED_CRIMSON_STEM.defaultBlockState();
	private static final BlockState RED_WOOD = Blocks.STRIPPED_CRIMSON_HYPHAE.defaultBlockState();
	private static final BlockState GILDED = Blocks.GILDED_BLACKSTONE.defaultBlockState();

	/**
	 * Meng Po's hall, on the bank west of the gate by the bridge's near end, built for her three times a man's height: a raised
	 * floor of blackstone (stairs up along its east side from the plaza and the gate's way, and from the north), railed over the
	 * water; six red columns capped in gold under red beams; a double roof of deepslate tiles, its eaves turned up at the
	 * corners, a red band between its tiers, a lantern on its ridge. Open to the east, a screen at its back, her pot over a soul
	 * campfire at her side, soul lanterns hung from the beams; a path to the bridge south of the gate's west pillar.
	 */
	private static void mengPoHall(ServerLevel level, int y) {
		int floor = y + 1; // where one stands in the hall
		// The raised floor: a polished rim, bricks, a chiseled aisle from the stairs up to her.
		for (int x = HALL_WEST; x <= HALL_EAST; x++) {
			for (int z = HALL_NORTH; z <= HALL_SOUTH; z++) {
				boolean rim = x == HALL_WEST || x == HALL_EAST || z == HALL_NORTH || z == HALL_SOUTH;
				boolean aisle = z == MENG_PO_POST.getZ() && x > MENG_PO_POST.getX() + 1;
				put(level, x, y, z, rim ? POLISHED : aisle ? CHISELED : BRICKS);
			}
		}
		for (int z = HALL_COLUMNS_Z[0]; z <= HALL_COLUMNS_Z[1]; z++) {
			put(level, HALL_EAST + 1, y, z, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
		}
		for (int x = -18; x <= -16; x++) {
			put(level, x, y, HALL_NORTH - 1, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
		}
		// Railed along its north side (but for the stairs), its west, and its south over the water.
		for (int x = HALL_WEST; x <= HALL_EAST; x++) {
			if (x < -18 || x > -16) put(level, x, floor, HALL_NORTH, WALL);
			put(level, x, floor, HALL_SOUTH, WALL);
		}
		for (int z = HALL_NORTH; z <= HALL_SOUTH; z++) put(level, HALL_WEST, floor, z, WALL);
		// The path from its stairs to the bridge, south of the west pillar.
		for (int x = HALL_EAST + 2; x <= -5; x++) {
			for (int z = 11; z <= 15; z++) put(level, x, y - 1, z, BRICKS);
		}

		// The columns, red, on stone bases and under gold capitals, and the red beams over them round the hall.
		int beam = floor + HALL_COLUMN;
		for (int cx : HALL_COLUMNS_X) {
			for (int cz : HALL_COLUMNS_Z) {
				put(level, cx, floor, cz, POLISHED);
				for (int dy = 1; dy < HALL_COLUMN - 1; dy++) put(level, cx, floor + dy, cz, RED_COLUMN);
				put(level, cx, floor + HALL_COLUMN - 1, cz, GILDED);
			}
		}
		for (int x = HALL_COLUMNS_X[0]; x <= HALL_COLUMNS_X[2]; x++) {
			for (int cz : HALL_COLUMNS_Z) put(level, x, beam, cz, RED_WOOD);
		}
		for (int cx : new int[] {HALL_COLUMNS_X[0], HALL_COLUMNS_X[2]}) {
			for (int z = HALL_COLUMNS_Z[0]; z <= HALL_COLUMNS_Z[1]; z++) put(level, cx, beam, z, RED_WOOD);
		}
		// Soul lanterns hung from the beams: two over the way in, two down each side.
		for (int z : new int[] {11, 13}) put(level, HALL_COLUMNS_X[2], beam - 1, z, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
		for (int x : new int[] {-20, -14}) {
			for (int cz : HALL_COLUMNS_Z) put(level, x, beam - 1, cz, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
		}

		// The screen at its back, a gold heart in it, her jars before it.
		for (int z = HALL_COLUMNS_Z[0] + 1; z < HALL_COLUMNS_Z[1]; z++) {
			for (int dy = 0; dy < HALL_COLUMN; dy++) {
				boolean heart = z == MENG_PO_POST.getZ() && (dy == 3 || dy == 4);
				put(level, HALL_COLUMNS_X[0], floor + dy, z, dy == 0 ? POLISHED : dy == HALL_COLUMN - 1 ? RED_WOOD : heart ? GILDED : BRICKS);
			}
		}
		put(level, HALL_COLUMNS_X[0] + 1, floor, HALL_COLUMNS_Z[0] + 1, Blocks.DECORATED_POT.defaultBlockState());
		put(level, HALL_COLUMNS_X[0] + 1, floor, HALL_COLUMNS_Z[1] - 1, Blocks.DECORATED_POT.defaultBlockState());
		// Her pot at her side, over a soul campfire.
		put(level, MENG_PO_POT.getX(), y + MENG_PO_POT.getY() - 1, MENG_PO_POT.getZ(), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, false));
		put(level, MENG_PO_POT.getX(), y + MENG_PO_POT.getY(), MENG_PO_POT.getZ(), Blocks.CAULDRON.defaultBlockState());

		// The roof: the lower eaves two out from the columns, a ledge, the red band, the upper eaves over it, up to the ridge.
		int eave = beam + 1;
		roofRing(level, eave, HALL_WEST - 1, HALL_EAST + 1, HALL_NORTH - 1, HALL_SOUTH + 1);
		roofRing(level, eave + 1, HALL_WEST, HALL_EAST, HALL_NORTH, HALL_SOUTH);
		for (int x = HALL_WEST + 2; x <= HALL_EAST - 2; x++) {
			for (int z = HALL_NORTH + 2; z <= HALL_SOUTH - 2; z++) {
				boolean side = x == HALL_WEST + 2 || x == HALL_EAST - 2, end = z == HALL_NORTH + 2 || z == HALL_SOUTH - 2;
				if (side || end) put(level, x, eave + 2, z, side && end ? GILDED : RED_WOOD);
			}
		}
		roofRing(level, eave + 3, HALL_WEST + 1, HALL_EAST - 1, HALL_NORTH + 1, HALL_SOUTH - 1);
		roofRing(level, eave + 4, HALL_WEST + 2, HALL_EAST - 2, HALL_NORTH + 2, HALL_SOUTH - 2);
		roofRing(level, eave + 5, HALL_WEST + 3, HALL_EAST - 3, HALL_NORTH + 3, HALL_SOUTH - 3);
		// The eaves' corners turned up, both tiers.
		for (int[] corner : new int[][] {{HALL_WEST - 1, HALL_NORTH - 1, 1}, {HALL_EAST + 1, HALL_NORTH - 1, 1}, {HALL_WEST - 1, HALL_SOUTH + 1, 1},
				{HALL_EAST + 1, HALL_SOUTH + 1, 1}, {HALL_WEST + 1, HALL_NORTH + 1, 4}, {HALL_EAST - 1, HALL_NORTH + 1, 4},
				{HALL_WEST + 1, HALL_SOUTH - 1, 4}, {HALL_EAST - 1, HALL_SOUTH - 1, 4}}) {
			put(level, corner[0], eave + corner[2], corner[1], TILE_WALL);
		}
		// The ridge, its ends raised, a soul lantern at its middle.
		int ridgeZ = (HALL_NORTH + HALL_SOUTH) / 2;
		for (int x = HALL_WEST + 4; x <= HALL_EAST - 4; x++) put(level, x, eave + 6, ridgeZ, TILE_WALL);
		put(level, HALL_WEST + 4, eave + 7, ridgeZ, TILE_WALL);
		put(level, HALL_EAST - 4, eave + 7, ridgeZ, TILE_WALL);
		put(level, (HALL_WEST + HALL_EAST) / 2, eave + 7, ridgeZ, Blocks.SOUL_LANTERN.defaultBlockState());

		// Every stair and wall shaped by what was laid around it (a corner turned, a rail joined).
		for (int x = HALL_WEST - 1; x <= HALL_EAST + 1; x++) {
			for (int z = HALL_NORTH - 1; z <= HALL_SOUTH + 1; z++) {
				for (int at = y; at <= eave + 7; at++) settle(level, x, at, z);
			}
		}
	}

	/**
	 * One course of a hipped roof: a ring of tile stairs round x0..x1, z0..z1 at height {@code y}, each rising toward the
	 * middle, the tiles laid flat within.
	 */
	private static void roofRing(ServerLevel level, int y, int x0, int x1, int z0, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if (x > x0 && x < x1 && z > z0 && z < z1) {
					put(level, x, y, z, TILES);
					continue;
				}
				Direction facing = z == z0 ? Direction.SOUTH : z == z1 ? Direction.NORTH : x == x0 ? Direction.EAST : Direction.WEST;
				put(level, x, y, z, TILE_STAIRS.setValue(StairBlock.FACING, facing));
			}
		}
	}

	/** A stair or wall at (x, y, z) shaped by its neighbours (a stair's corner turned, a wall joined to what is beside it). */
	private static void settle(ServerLevel level, int x, int y, int z) {
		BlockPos pos = new BlockPos(X + x, y, Z + z);
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof StairBlock) && !(state.getBlock() instanceof net.minecraft.world.level.block.WallBlock)) return;
		BlockState settled = Block.updateFromNeighbourShapes(state, level, pos);
		if (settled != state) level.setBlock(pos, settled, Block.UPDATE_CLIENTS);
	}

	/**
	 * A gate built with one of the old pavilions (the first with its roof 3 up, the second 6 up; both small, against the gate's
	 * west pillar and cutting into it) gets Meng Po's hall: the pavilion taken away and the ground under it put back, the pillar
	 * made whole, the hall built (Meng Po goes to her new post: keepOfficials).
	 */
	private static void replacePavilion(ServerLevel level, int y) {
		for (int x = -12; x <= -6; x++) {
			for (int z = 12; z <= 16; z++) {
				for (int dy = 0; dy <= 7; dy++) put(level, x, y + dy, z, AIR);
				if (x >= -11 && x <= -7 && z >= 13) put(level, x, y - 1, z, z == 16 ? POLISHED : SOUL_SOIL); // its floor; the bank
			}
		}
		pillar(level, y, -6);
		mengPoHall(level, y);
	}

	/** The Wheel's dais on the far bank: a raised floor with a step, the axle post behind the hub, braziers either side. */
	private static void wheelDais(ServerLevel level, int y) {
		for (int x = -6; x <= 6; x++) {
			for (int z = 27; z <= 37; z++) {
				boolean rim = Math.abs(x) == 6 || z == 27 || z == 37;
				put(level, x, y, z, rim ? POLISHED : BRICKS);
			}
		}
		for (int x = -2; x <= 2; x++) put(level, x, y, 26, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState());
		for (int x = -4; x <= 4; x++) {
			for (int z = 26; z <= 26; z++) {
				if (Math.abs(x) > 2) put(level, x, y - 1, z, BRICKS);
			}
		}
		for (int dy = 1; dy <= 5; dy++) put(level, 0, y + dy, WHEEL.getZ() + 1, WALL);
		put(level, 0, y + WHEEL.getY(), WHEEL.getZ() + 1, POLISHED);
		brazier(level, -5, y + 1, WHEEL.getZ());
		brazier(level, 5, y + 1, WHEEL.getZ());
		for (int x : new int[] {-6, 6}) {
			for (int z : new int[] {27, 37}) put(level, x, y + 1, z, Blocks.SOUL_LANTERN.defaultBlockState());
		}
		// The lawn of the far bank is red with spider lilies.
		for (int x = MIN_X + 1; x < MAX_X; x++) {
			for (int z = 25; z <= MAX_Z; z++) {
				boolean onDais = Math.abs(x) <= 6 && z >= 26 && z <= 37;
				if (!onDais && Math.floorMod(x * 7 + z * 13, 5) == 0) put(level, x, y, z, ModBlocks.RED_SPIDER_LILY.defaultBlockState());
			}
		}
	}

	/**
	 * The Terrace for Gazing Homeward west of the plaza: a 7 x 7 tower ten blocks tall with a parapet and lanterns, reached by a
	 * ramp of stairs climbing west from the plaza's edge to a landing along its south side.
	 */
	private static void terrace(ServerLevel level, int y) {
		for (int x = -26; x <= -20; x++) {
			for (int z = -3; z <= 3; z++) {
				for (int dy = 0; dy < 10; dy++) {
					boolean corner = (x == -26 || x == -20) && (z == -3 || z == 3);
					put(level, x, y + dy, z, corner ? BONE.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y) : dy == 9 ? POLISHED : BRICKS);
				}
				boolean edge = x == -26 || x == -20 || z == -3 || z == 3;
				if (edge && !(z == 3 && x >= -25 && x <= -21)) put(level, x, y + 10, z, WALL);
			}
		}
		for (int x : new int[] {-26, -20}) {
			for (int z : new int[] {-3, 3}) put(level, x, y + 11, z, Blocks.SOUL_LANTERN.defaultBlockState());
		}
		// The ramp: one stair up each block west, then the landing at the top along the terrace's south side.
		for (int step = 0; step < 10; step++) {
			int x = -10 - step;
			for (int z = 4; z <= 5; z++) {
				for (int dy = 0; dy < step; dy++) put(level, x, y + dy, z, BRICKS);
				put(level, x, y + step, z, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
			}
		}
		for (int x = -26; x <= -20; x++) {
			for (int z = 4; z <= 5; z++) {
				for (int dy = 0; dy < 10; dy++) put(level, x, y + dy, z, dy == 9 ? POLISHED : BRICKS);
				if (z == 5) put(level, x, y + 10, z, WALL);
			}
		}
		put(level, TERRACE_TOP.getX(), y + TERRACE_TOP.getY() - 1, TERRACE_TOP.getZ(), CHISELED);
	}

	/** A brazier: a post with a block of soul soil on it, and soul fire burning there for ever. */
	private static void brazier(ServerLevel level, int x, int y, int z) {
		put(level, x, y, z, WALL);
		put(level, x, y + 1, z, SOUL_SOIL);
		put(level, x, y + 2, z, Blocks.SOUL_FIRE.defaultBlockState());
	}

	private static void lanternPost(ServerLevel level, int x, int y, int z) {
		put(level, x, y, z, WALL);
		put(level, x, y + 1, z, Blocks.SOUL_LANTERN.defaultBlockState());
	}

	/** Puts back the way down, the Wheel's hub and the plaza's stele if anything took them (commands, an older gate). */
	private static void ensureFixtures(ServerLevel level, int y) {
		BlockPos stele = new BlockPos(X + STELE.getX(), y + STELE.getY(), Z + STELE.getZ());
		level.getChunk(stele);
		if (!level.getBlockState(stele).is(ModBlocks.METHOD_STELE)) {
			MethodSteleBlock.place(level, stele, GhostMethod.NETHERWORLD_BREATHING, net.minecraft.core.Direction.WEST);
		}
		Rumours.noteStele(level.dimension(), stele.above(), GhostMethod.NETHERWORLD_BREATHING); // the dead tell of it (noted again harmlessly)
		BlockPos rift = new BlockPos(X + WAY_DOWN.getX(), y + WAY_DOWN.getY(), Z + WAY_DOWN.getZ());
		level.getChunk(rift);
		if (!level.getBlockState(rift).is(ModBlocks.YELLOW_SPRINGS_RIFT)) level.setBlock(rift, ModBlocks.YELLOW_SPRINGS_RIFT.defaultBlockState(), Block.UPDATE_ALL);
		BlockPos wheel = new BlockPos(X + WHEEL.getX(), y + WHEEL.getY(), Z + WHEEL.getZ());
		level.getChunk(wheel);
		if (!level.getBlockState(wheel).is(ModBlocks.WHEEL_OF_REINCARNATION)) level.setBlock(wheel, ModBlocks.WHEEL_OF_REINCARNATION.defaultBlockState(), Block.UPDATE_ALL);
	}

	private static void put(ServerLevel level, int x, int y, int z, BlockState state) {
		level.setBlock(new BlockPos(X + x, y, Z + z), state, Block.UPDATE_CLIENTS);
	}

	// --- Its keepers ---

	/** A slain warden comes back to its post after this long (three days). */
	public static final long OFFICIAL_RETURNS_AFTER = 3 * 24_000L;
	/** One missing this long (ticks) from a post whose land is loaded and running is taken as lost, and another comes. */
	private static final int OFFICIAL_MISSING_TICKS = 200;

	/**
	 * Every few seconds while the Gate's land runs: Ox-Head and Horse-Face at their posts and Meng Po by her pot, each put there
	 * the first time, and a slain warden again three days ({@link #OFFICIAL_RETURNS_AFTER}) after it fell.
	 */
	public static void keepOfficials(MinecraftServer server) {
		if (server.getTickCount() % 40 != 0) return;
		ServerLevel level = server.getLevel(ModDimensions.NETHERWORLD);
		if (level == null) return;
		Data data = Data.get(level);
		if (!data.built) return;
		long now = level.getGameTime();
		for (GhostCultivatorNpc.Role role : new GhostCultivatorNpc.Role[] {GhostCultivatorNpc.Role.OX_HEAD, GhostCultivatorNpc.Role.HORSE_FACE,
				GhostCultivatorNpc.Role.MENG_PO}) {
			BlockPos post = at(level, postOf(role));
			if (!level.isPositionEntityTicking(post)) continue;
			Official official = data.officials.computeIfAbsent(role.getId(), id -> new Official());
			if (official.id != null && level.getEntity(official.id) instanceof GhostCultivatorNpc npc && npc.isAlive()) {
				official.missing = 0;
				keepAt(npc, role, post);
				continue;
			}
			// Found by its role (its id lost with an older save), or still loading with its land: give it a moment.
			GhostCultivatorNpc found = null;
			for (CultivatorNpc npc : CultivatorNpc.loaded(level)) {
				if (npc instanceof GhostCultivatorNpc ghost && ghost.getRole() == role && ghost.isAlive()) found = ghost;
			}
			if (found != null) {
				official.id = found.getUUID();
				official.missing = 0;
				data.setDirty();
				keepAt(found, role, post);
				continue;
			}
			if (official.slainAt > 0 && now - official.slainAt < OFFICIAL_RETURNS_AFTER) continue;
			if (official.id != null && official.slainAt == 0 && (official.missing += 40) < OFFICIAL_MISSING_TICKS) continue;
			GhostCultivatorNpc npc = ModEntities.GHOST_CULTIVATOR.create(level);
			if (npc == null) continue;
			npc.moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, facingOf(role), 0);
			npc.becomeOfficial(role, post, level.getRandom());
			level.addFreshEntity(npc);
			official.id = npc.getUUID();
			official.slainAt = 0;
			official.missing = 0;
			data.setDirty();
		}
	}

	/** Where each keeper stands (relative to the gate). */
	static BlockPos postOf(GhostCultivatorNpc.Role role) {
		return switch (role) {
			case OX_HEAD -> OX_HEAD_POST;
			case HORSE_FACE -> HORSE_FACE_POST;
			default -> MENG_PO_POST;
		};
	}

	/** Which way each keeper faces at its post: the wardens the plaza the dead come from, Meng Po the way into her hall. */
	private static float facingOf(GhostCultivatorNpc.Role role) {
		return role == GhostCultivatorNpc.Role.MENG_PO ? -90 : 180;
	}

	/**
	 * A keeper whose post has moved since it took it (Meng Po's, as her pavilion became her hall: replacePavilion) is set down at
	 * the new one.
	 */
	private static void keepAt(GhostCultivatorNpc npc, GhostCultivatorNpc.Role role, BlockPos post) {
		if (post.equals(npc.keeperPost())) return;
		npc.moveKeeperPost(post);
		npc.moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, facingOf(role), 0);
	}

	/** A keeper that can find no way back to its post (GhostCultivatorNpc#customServerAiStep) is set down there again, facing as it should. */
	static void returnToPost(GhostCultivatorNpc npc) {
		BlockPos post = npc.keeperPost();
		if (post == null) return;
		npc.getNavigation().stop();
		npc.moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, facingOf(npc.getRole()), 0);
		if (npc.level() instanceof ServerLevel level) {
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, npc.getX(), npc.getY() + 1, npc.getZ(), 20, 0.5, 1.0, 0.5, 0.03);
		}
	}

	/** A warden has fallen: it comes back in three days. */
	static void onOfficialSlain(ServerLevel level, GhostCultivatorNpc.Role role) {
		if (!ModDimensions.isNetherworld(level.dimension())) return;
		Data data = Data.get(level);
		Official official = data.officials.computeIfAbsent(role.getId(), id -> new Official());
		official.slainAt = Math.max(1, level.getGameTime());
		official.id = null;
		data.setDirty();
	}

	/** When the warden {@code role} last fell (game time), or 0 (for the tests). */
	static long slainAt(ServerLevel level, GhostCultivatorNpc.Role role) {
		Official official = Data.get(level).officials.get(role.getId());
		return official == null ? 0 : official.slainAt;
	}

	/** One keeper: its entity's id, when it fell (0: it hasn't), and how long it has been missing (not saved). */
	static final class Official {
		java.util.UUID id;
		long slainAt;
		int missing;
	}

	// --- The Terrace for Gazing Homeward, and Meng Po's pot ---

	/** Within this many blocks (sideways) of the Terrace's top stone, at its height, a ghost looks homeward. */
	public static final double TERRACE_REACH = 3.5;
	/** The place shown is rounded to this many blocks. */
	public static final int TERRACE_ROUNDING = 10;

	/** Whether {@code p} stands atop the Terrace for Gazing Homeward. */
	public static boolean onTerrace(ServerPlayer p) {
		if (!ModDimensions.isNetherworld(p.level().dimension())) return false;
		BlockPos top = at(p.serverLevel(), TERRACE_TOP);
		double dx = p.getX() - (top.getX() + 0.5), dz = p.getZ() - (top.getZ() + 0.5);
		return dx * dx + dz * dz <= TERRACE_REACH * TERRACE_REACH && p.getY() >= top.getY() - 1.5 && p.getY() <= top.getY() + 3;
	}

	/**
	 * A ghost on the Terrace sees, far off, the place it died: the dimension and the spot to the nearest
	 * {@link #TERRACE_ROUNDING} blocks, also written into its Journal. Once for each death.
	 */
	public static void terrace(ServerPlayer p) {
		NetherworldState s = CultivationManager.get(p).netherworld();
		if (!s.ghost() || s.sawHome() || !s.hasDeathPlace() || !onTerrace(p)) return;
		s.setSawHome(true);
		CultivationManager.markDirty(p.server);
		BlockPos place = s.deathPos();
		int r = TERRACE_ROUNDING;
		Component where = Component.translatable(ModLang.TERRACE_PLACE, dimensionName(s.deathDimension()),
				Math.round(place.getX() / (double) r) * r, Math.round(place.getY() / (double) r) * r, Math.round(place.getZ() / (double) r) * r);
		p.displayClientMessage(Component.translatable(ModLang.MSG_TERRACE, where).withStyle(net.minecraft.ChatFormatting.AQUA), true);
		p.playNotifySound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.6f);
		Journal.note(p, "terrace", ModLang.JOURNAL_TERRACE, Component.translatable(ModLang.JOURNAL_TERRACE_TEXT, where));
	}

	/** "the Overworld", "the Nether"...: a dimension's name for the Terrace's view. */
	private static Component dimensionName(String dimension) {
		return switch (dimension) {
			case "minecraft:overworld" -> Component.translatable(ModLang.DIMENSION_OVERWORLD);
			case "minecraft:the_nether" -> Component.translatable(ModLang.DIMENSION_NETHER);
			case "minecraft:the_end" -> Component.translatable(ModLang.DIMENSION_END);
			default -> dimension.endsWith("upper_realm") ? Component.translatable(ModLang.DIMENSION_UPPER_REALM)
					: dimension.endsWith("netherworld") ? Component.translatable(ModLang.DIMENSION_NETHERWORLD)
					: dimension.endsWith("astral_void") ? Component.translatable(ModLang.DIMENSION_ASTRAL_VOID)
					: dimension.endsWith("astral_surfaces") ? Component.translatable(ModLang.DIMENSION_ASTRAL_SURFACES) : Component.literal(dimension);
		};
	}

	/** Meng Po's pot: whether {@code pos} is it. */
	public static boolean isMengPoPot(Level level, BlockPos pos) {
		return level instanceof ServerLevel server && ModDimensions.isNetherworld(level.dimension()) && pos.equals(at(server, MENG_PO_POT));
	}

	/**
	 * A ghost drinks Meng Po's soup (at her pot, or asking her): the past is forgotten, and when the Wheel returns it to life its
	 * medicinal toxicity is gone (GhostSystem). The living are turned away.
	 */
	public static void drinkSoup(ServerPlayer p) {
		NetherworldState s = CultivationManager.get(p).netherworld();
		if (!s.ghost()) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_SOUP_LIVING), true);
			return;
		}
		if (s.memoryCleansed()) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_SOUP_AGAIN), true);
			return;
		}
		s.setMemoryCleansed(true);
		CultivationManager.markDirty(p.server);
		p.serverLevel().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_DRINK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.8f);
		p.displayClientMessage(Component.translatable(ModLang.MSG_SOUP).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
	}

	/** That the gate is built, and its floor level, and its keepers; kept with the Netherworld's own data. */
	public static final class Data extends SavedData {
		private static final String NAME = DefyingTheHeavens.MOD_ID + "_ghost_gate";
		boolean built;
		int y = FALLBACK_Y;
		/** What Meng Po has (1 and 2: the old pavilions, low and tall; 3: her hall; see PAVILION_VERSION). */
		int pavilion = 1;
		final java.util.Map<String, Official> officials = new java.util.HashMap<>();

		static Data get(ServerLevel netherworld) {
			return netherworld.getDataStorage().computeIfAbsent(Data::load, Data::new, NAME);
		}

		static Data load(CompoundTag tag) {
			Data data = new Data();
			data.built = tag.getBoolean("Built");
			data.y = tag.contains("FloorY") ? tag.getInt("FloorY") : FALLBACK_Y;
			data.pavilion = tag.contains("Pavilion") ? tag.getInt("Pavilion") : 1;
			CompoundTag officials = tag.getCompound("Officials");
			for (String role : officials.getAllKeys()) {
				CompoundTag one = officials.getCompound(role);
				Official official = new Official();
				official.id = one.hasUUID("Id") ? one.getUUID("Id") : null;
				official.slainAt = one.getLong("SlainAt");
				data.officials.put(role, official);
			}
			return data;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			tag.putBoolean("Built", built);
			tag.putInt("FloorY", y);
			tag.putInt("Pavilion", pavilion);
			CompoundTag officials = new CompoundTag();
			for (java.util.Map.Entry<String, Official> entry : this.officials.entrySet()) {
				CompoundTag one = new CompoundTag();
				if (entry.getValue().id != null) one.putUUID("Id", entry.getValue().id);
				one.putLong("SlainAt", entry.getValue().slainAt);
				officials.put(entry.getKey(), one);
			}
			tag.put("Officials", officials);
			return tag;
		}
	}

	private GhostGate() {}
}
