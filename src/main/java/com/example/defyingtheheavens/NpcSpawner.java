package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Rogue cultivators turning up in the world, outside the sects: now and then near a player, out of their sight, in the
 * Overworld, the Nether, the End and the Upper Realm (never the Spatial Gap or the Inner Realm, and never inside a barrier).
 * <p>
 * Each attempt rolls a rank from {@link #chance}: the weaker, the likelier, from {@link #MORTAL_CHANCE} for a mortal
 * wanderer down to {@link #PEAK_CHANCE} for a Heavenly Being at Grand Perfection, falling evenly (by the same factor) with
 * every stage; the rest of the time the attempt finds no one. The lower realms never see anyone above Heavenly Being Grand
 * Perfection. The Upper Realm's curve runs over its own range instead, from Qi Refining Early to Four Axis Grand Perfection.
 */
public final class NpcSpawner {
	public static final double MORTAL_CHANCE = 0.10;
	public static final double PEAK_CHANCE = 0.001;
	/** Ticks between attempts around each player (30 seconds), and the chance each one is made at all. */
	private static final int INTERVAL = 600;
	private static final float ATTEMPT_CHANCE = 0.35f;
	private static final int MIN_DISTANCE = 40;
	private static final int MAX_DISTANCE = 80;
	/** At most this many cultivators near any one player (within {@link #CROWD_RADIUS}), and in any one level. */
	private static final int CROWD = 3;
	private static final double CROWD_RADIUS = 128;
	private static final int LEVEL_CAP = 40;

	/**
	 * Chance an attempt finds a cultivator of {@code step} (0 = mortal ... {@code steps - 1} = the strongest): the
	 * geometric fall from {@link #MORTAL_CHANCE} to {@link #PEAK_CHANCE}.
	 */
	public static double chance(int step, int steps) {
		if (steps <= 1) return MORTAL_CHANCE;
		double ratio = Math.pow(PEAK_CHANCE / MORTAL_CHANCE, 1.0 / (steps - 1));
		return MORTAL_CHANCE * Math.pow(ratio, step);
	}

	/** Steps on each realm's curve: mortal + Qi Refining Early .. Heavenly Being GP below; QR Early .. Four Axis GP above. */
	static int steps(boolean upperRealm) {
		int top = upperRealm ? PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION) : PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION);
		return upperRealm ? top + 1 : top + 2;
	}

	/** The rank the roll lands on, or {@link Integer#MIN_VALUE} for nobody (unless {@code mustFind}, which rolls again). */
	public static int rollRank(RandomSource random, boolean upperRealm, boolean mustFind) {
		int steps = steps(upperRealm);
		for (int tries = 0; tries < 64; tries++) {
			double roll = random.nextDouble();
			for (int step = 0; step < steps; step++) {
				roll -= chance(step, steps);
				if (roll < 0) return upperRealm ? step : step - 1; // below, step 0 is the mortal
			}
			if (!mustFind) return Integer.MIN_VALUE;
		}
		return upperRealm ? 0 : CultivatorNpc.MORTAL;
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % INTERVAL != 0) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.isSpectator() || player.getRandom().nextFloat() >= ATTEMPT_CHANCE) continue;
			trySpawnNear(player.serverLevel(), player);
		}
	}

	private static void trySpawnNear(ServerLevel level, ServerPlayer player) {
		if (ModDimensions.isSpatialGap(level.dimension()) || ModDimensions.isInnerRealm(level.dimension())) return;
		RandomSource random = level.getRandom();
		if (CultivatorNpc.countIn(level) >= LEVEL_CAP || CultivatorNpc.countNear(level, player.position(), CROWD_RADIUS) >= CROWD) return;
		boolean upper = ModDimensions.isUpperRealm(level.dimension());
		int rank = rollRank(random, upper, false);
		if (rank == Integer.MIN_VALUE) return;
		BlockPos spot = findSpot(level, player, random);
		if (spot == null) return;
		CultivatorNpc npc = ModEntities.CULTIVATOR.create(level);
		if (npc == null) return;
		npc.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360, 0);
		npc.becomeRogue(rank, Alignment.randomRogue(random), random);
		level.addFreshEntity(npc);
	}

	/** Solid, dry ground 40-80 blocks from the player, out of every player's close sight, off sect grounds and outside every barrier. */
	private static BlockPos findSpot(ServerLevel level, ServerPlayer player, RandomSource random) {
		for (int attempt = 0; attempt < 8; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			int distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE);
			int x = player.getBlockX() + (int) (Math.cos(angle) * distance);
			int z = player.getBlockZ() + (int) (Math.sin(angle) * distance);
			BlockPos column = new BlockPos(x, player.getBlockY(), z);
			if (!level.isLoaded(column) || !level.isPositionEntityTicking(column)) continue;
			BlockPos feet = ground(level, x, z, player.getBlockY());
			if (feet == null || SectManager.sectAt(level, feet) != null || Formations.shelters(level, feet)) continue;
			Vec3 centre = Vec3.atBottomCenterOf(feet);
			if (level.getNearestPlayer(centre.x, centre.y, centre.z, 24, false) != null) continue;
			return feet;
		}
		return null;
	}

	/** Where to stand in a column: the surface, or, under a ceiling (the Nether), the first open floor near {@code nearY}. */
	private static BlockPos ground(ServerLevel level, int x, int z, int nearY) {
		if (!level.dimensionType().hasCeiling()) {
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos feet = new BlockPos(x, y, z);
			return y > level.getMinBuildHeight() + 1 && standable(level, feet) ? feet : null;
		}
		for (int dy = 0; dy < 32; dy++) {
			for (int sign : new int[] {1, -1}) {
				BlockPos feet = new BlockPos(x, nearY + dy * sign, z);
				if (standable(level, feet)) return feet;
			}
		}
		return null;
	}

	private static boolean standable(Level level, BlockPos feet) {
		BlockPos below = feet.below();
		return level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()
				&& level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && level.getFluidState(below).isEmpty()
				&& !level.getBlockState(below).is(ModBlocks.SECT_BARRIER);
	}

	private NpcSpawner() {}
}
