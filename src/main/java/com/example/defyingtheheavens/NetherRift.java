package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Gate of the Yellow Springs (黄泉门): the Nether's rift at 0, 0, up the Soul River to the Netherworld. Placed on server start
 * inside the Nether, never on its bedrock roof: in a standing pocket of the 0, 0 column between Y {@link #POCKET_MIN_Y} and
 * {@link #POCKET_MAX_Y} clear of lava, or failing that in a chamber carved at Y {@link #CHAMBER_Y}. A small shrine of blackstone
 * bricks, soul soil and soul lanterns marks it. Also finds safe ground in the Nether for anyone the river sets down
 * ({@link #land}).
 */
public final class NetherRift {
	public static final int POCKET_MIN_Y = 32, POCKET_MAX_Y = 110, CHAMBER_Y = 70;
	/** The shrine's platform reaches this far from the rift; lava this close is turned to blackstone. */
	private static final int SHRINE_RADIUS = 2, LAVA_CLEAR_RADIUS = 4;
	/** Where to look for ground around a landing, and how far clear of the rift's column it must be. */
	private static final int[] LAND_RADII = {0, 3, 6, 10, 14, 20, 28, 36, 48};
	private static final int RIFT_CLEARANCE = 3;
	/** How far above or below the rift's height a landing may be, searched in turn: by the shrine first, then further. */
	private static final int[] LAND_BANDS = {8, 24, 256};
	/** The Nether's ceiling of bedrock starts here: nobody is set down at or above it. */
	private static final int ROOF_Y = 122;

	/** Makes sure the Nether has its rift at 0, 0 (a server start; also back after a command removed it). */
	public static void ensure(MinecraftServer server) {
		if (server == null) return;
		try {
			ServerLevel nether = server.getLevel(Level.NETHER);
			if (nether == null) return;
			int x = SpatialRiftBlock.RIFT_X, z = SpatialRiftBlock.RIFT_Z;
			nether.getChunk(x >> 4, z >> 4);
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, 0, z);
			for (int y = nether.getMinBuildHeight(); y < nether.getMaxBuildHeight(); y++) {
				if (nether.getBlockState(cursor.setY(y)).is(ModBlocks.YELLOW_SPRINGS_RIFT)) return; // already there
			}
			BlockPos pocket = findPocket(nether, x, z);
			BlockPos rift = pocket != null ? pocket : carveChamber(nether, x, z);
			shrine(nether, rift);
			nether.setBlock(rift, ModBlocks.YELLOW_SPRINGS_RIFT.defaultBlockState(), 3);
			DefyingTheHeavens.LOGGER.info("Gate of the Yellow Springs placed in the Nether at {} ({})", rift.toShortString(),
					pocket != null ? "a standing pocket" : "a carved chamber");
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Could not place the Gate of the Yellow Springs in the Nether", e);
		}
	}

	/** The rift block in the Nether's 0, 0 column, or null if that column isn't loaded (or holds none). For NPC ascenders. */
	public static BlockPos riftPosition(ServerLevel nether) {
		BlockPos column = new BlockPos(SpatialRiftBlock.RIFT_X, 0, SpatialRiftBlock.RIFT_Z);
		if (!nether.hasChunkAt(column)) return null;
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(SpatialRiftBlock.RIFT_X, 0, SpatialRiftBlock.RIFT_Z);
		for (int y = POCKET_MIN_Y - 8; y <= ROOF_Y; y++) {
			if (nether.getBlockState(cursor.setY(y)).is(ModBlocks.YELLOW_SPRINGS_RIFT)) return cursor.immutable();
		}
		return null;
	}

	/**
	 * The standing pocket nearest Y {@link #CHAMBER_Y}: firm, lava-free ground with three blocks of air above, and no lava
	 * within reach of the shrine. Null if the column has none.
	 */
	private static BlockPos findPocket(ServerLevel level, int x, int z) {
		BlockPos best = null;
		for (int y = POCKET_MIN_Y; y <= POCKET_MAX_Y; y++) {
			BlockPos at = new BlockPos(x, y, z);
			if (!isStandable(level, at, 3) || lavaNear(level, at, SHRINE_RADIUS + 1)) continue;
			if (best == null || Math.abs(y - CHAMBER_Y) < Math.abs(best.getY() - CHAMBER_Y)) best = at;
		}
		return best;
	}

	/** Firm ground below {@code feet}, {@code air} clear blocks from {@code feet} up, no fluid in them. */
	private static boolean isStandable(ServerLevel level, BlockPos feet, int air) {
		BlockState ground = level.getBlockState(feet.below());
		if (!ground.getFluidState().isEmpty() || !ground.isFaceSturdy(level, feet.below(), Direction.UP)
				|| ground.is(Blocks.MAGMA_BLOCK) || ground.is(Blocks.BEDROCK)) return false;
		for (int dy = 0; dy < air; dy++) {
			BlockState state = level.getBlockState(feet.above(dy));
			if (!state.isAir() || !state.getFluidState().isEmpty()) return false;
		}
		return true;
	}

	private static boolean lavaNear(ServerLevel level, BlockPos at, int radius) {
		for (BlockPos pos : BlockPos.betweenClosed(at.offset(-radius, -1, -radius), at.offset(radius, 3, radius))) {
			if (level.getFluidState(pos).is(FluidTags.LAVA)) return true;
		}
		return false;
	}

	/** Hollows a small room at Y {@link #CHAMBER_Y} walled in blackstone bricks, so no lava runs in; returns where the rift stands. */
	private static BlockPos carveChamber(ServerLevel level, int x, int z) {
		BlockPos floor = new BlockPos(x, CHAMBER_Y - 1, z);
		int r = SHRINE_RADIUS + 2;
		for (int dx = -r - 1; dx <= r + 1; dx++) {
			for (int dz = -r - 1; dz <= r + 1; dz++) {
				for (int dy = 0; dy <= 6; dy++) {
					boolean shell = Math.abs(dx) == r + 1 || Math.abs(dz) == r + 1 || dy == 0 || dy == 6;
					BlockPos pos = floor.offset(dx, dy, dz);
					level.setBlock(pos, shell ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
				}
			}
		}
		return floor.above();
	}

	/** A blackstone-brick platform with a ring of soul soil, four posts with soul lanterns, and any lava nearby turned to stone. */
	private static void shrine(ServerLevel level, BlockPos rift) {
		for (BlockPos pos : BlockPos.betweenClosed(rift.offset(-LAVA_CLEAR_RADIUS, -2, -LAVA_CLEAR_RADIUS), rift.offset(LAVA_CLEAR_RADIUS, 4, LAVA_CLEAR_RADIUS))) {
			if (level.getFluidState(pos).is(FluidTags.LAVA)) level.setBlock(pos, Blocks.BLACKSTONE.defaultBlockState(), 2 | 16);
		}
		for (int dx = -SHRINE_RADIUS; dx <= SHRINE_RADIUS; dx++) {
			for (int dz = -SHRINE_RADIUS; dz <= SHRINE_RADIUS; dz++) {
				boolean ring = Math.max(Math.abs(dx), Math.abs(dz)) == SHRINE_RADIUS;
				level.setBlock(rift.offset(dx, -1, dz), ring ? Blocks.SOUL_SOIL.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 2 | 16);
				for (int dy = 0; dy < 3; dy++) {
					BlockPos pos = rift.offset(dx, dy, dz);
					if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
				}
			}
		}
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				BlockPos post = rift.offset(sx * SHRINE_RADIUS, 0, sz * SHRINE_RADIUS);
				level.setBlock(post, Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState(), 2 | 16);
				level.setBlock(post.above(), Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false), 2 | 16);
			}
		}
	}

	/**
	 * Sets {@code player} down in the Nether near {@code x}, {@code z}: on firm, lava-free ground with two blocks of air, a few
	 * blocks clear of the rift's column, never on the roof; a small blackstone platform if there is no such ground nearby.
	 */
	public static void land(MinecraftServer server, ServerPlayer player, int x, int z) {
		ServerLevel nether = server.getLevel(Level.NETHER);
		if (nether == null) {
			DefyingTheHeavens.LOGGER.error("The Nether is missing; leaving {} where they are", player.getScoreboardName());
			return;
		}
		BlockPos at = findGround(nether, x, z);
		player.teleportTo(nether, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, player.getYRot(), 0.0f);
		SoulRiver.settle(player);
	}

	/**
	 * Where in the Nether a traveller can stand near {@code x}, {@code z} (see {@link #land}): of the ground in each column, the
	 * stretch nearest the rift's own height (or Y {@link #CHAMBER_Y} where there is no rift), so they come out by its shrine
	 * rather than in some sealed pocket above or below. Searched in widening bands of height ({@link #LAND_BANDS}), each over
	 * every radius before the next: ground near that height a little further off is better than the first column's only
	 * standing room, a pocket under the bedrock roof.
	 */
	public static BlockPos findGround(ServerLevel nether, int x, int z) {
		int preferredY = riftY(nether);
		for (int band : LAND_BANDS) {
			for (int radius : LAND_RADII) {
				int points = radius == 0 ? 1 : 8 + radius / 2;
				for (int i = 0; i < points; i++) {
					double angle = Math.PI * 2 * i / points;
					int cx = x + (int) Math.round(Math.cos(angle) * radius);
					int cz = z + (int) Math.round(Math.sin(angle) * radius);
					int fromRiftX = cx - SpatialRiftBlock.RIFT_X, fromRiftZ = cz - SpatialRiftBlock.RIFT_Z;
					if (fromRiftX * fromRiftX + fromRiftZ * fromRiftZ < RIFT_CLEARANCE * RIFT_CLEARANCE) continue;
					nether.getChunk(cx >> 4, cz >> 4);
					BlockPos best = null;
					int from = Math.max(nether.getMinBuildHeight() + 5, preferredY - band), to = Math.min(ROOF_Y - 2, preferredY + band);
					for (int y = from; y <= to; y++) {
						BlockPos feet = new BlockPos(cx, y, cz);
						if (best != null && Math.abs(y - preferredY) >= Math.abs(best.getY() - preferredY)) continue;
						if (isStandable(nether, feet, 2) && !lavaNear(nether, feet, 1)) best = feet;
					}
					if (best != null) return best;
				}
			}
		}
		BlockPos platform = new BlockPos(x + RIFT_CLEARANCE + 1, CHAMBER_Y, z);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				nether.setBlock(platform.offset(dx, -1, dz), Blocks.BLACKSTONE.defaultBlockState(), 3);
				for (int dy = 0; dy < 2; dy++) nether.setBlock(platform.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
			}
		}
		DefyingTheHeavens.LOGGER.info("No ground in the Nether near {}, {}; built a landing platform at {}", x, z, platform.toShortString());
		return platform;
	}

	/** The height of the rift at 0, 0, or {@link #CHAMBER_Y} if it isn't there. */
	private static int riftY(ServerLevel nether) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(SpatialRiftBlock.RIFT_X, 0, SpatialRiftBlock.RIFT_Z);
		for (int y = nether.getMinBuildHeight(); y < nether.getMaxBuildHeight(); y++) {
			if (nether.getBlockState(cursor.setY(y)).is(ModBlocks.YELLOW_SPRINGS_RIFT)) return y;
		}
		return CHAMBER_Y;
	}

	private NetherRift() {}
}
