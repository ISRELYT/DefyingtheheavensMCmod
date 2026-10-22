package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rogue cultivators (and mortal wanderers) turning up in the world, outside the sects: now and then near a player, out of
 * their sight, in the Overworld, the Nether, the End and the Upper Realm (never the Spatial Gap or the Inner Realm, never
 * inside a barrier, and never within {@link #SECT_CLEARANCE} of a sect's grounds). Around a player who stays about they turn
 * up 40-80 blocks off; ahead of one travelling (flying, riding), 64-128 blocks along the way, so a journey meets people too.
 * <p>
 * They are rare by placement, not by leaving: cultivators never despawn (see CultivatorNpc#requiresCustomPersistence), so
 * a new one never turns up within {@link #SPACING} of another of its kind (wandering rogue,
 * {@link CultivatorNpc#isWanderingRogue}, or mortal wanderer), loaded or not ({@link RogueIndex}), and so no player is ever
 * within {@link #CROWD_RADIUS} of two rogues, or of two wanderers. Those met before stay where they wander, and keep that
 * stretch of land taken.
 * <p>
 * Each attempt rolls a rank from {@link #chance}: the weaker, the likelier, from {@link #MORTAL_CHANCE} for a mortal
 * wanderer down to {@link #PEAK_CHANCE} for a Heavenly Being at Grand Perfection, falling evenly (by the same factor) with
 * every stage; the rest of the time the attempt finds no one. The lower realms never see anyone above Heavenly Being Grand
 * Perfection. The Upper Realm's curve runs over its own range instead, from Qi Refining Early to Four Axis Grand Perfection;
 * the Netherworld's too, where those who turn up are its dead, ghost cultivators (GhostCultivatorNpc), never the living.
 */
public final class NpcSpawner {
	public static final double MORTAL_CHANCE = 0.10;
	public static final double PEAK_CHANCE = 0.001;
	/**
	 * Of the Upper Realm's rogues found at Four Axis Grand Perfection (its curve's peak), this share are of Dao Integration instead,
	 * while the realm has room for one (HighRealms: the realms after it rare, the user's lore, 2026-10-22); the realms past it come only
	 * by cultivating.
	 */
	public static final double DAO_INTEGRATION_SHARE = 0.25;
	/** Ticks between looks around each player (5 seconds). */
	private static final int CHECK = 100;
	/** Around a player who stays about: an attempt every 30 seconds, made 35% of the time, 40-80 blocks off. */
	private static final int INTERVAL = 600;
	private static final float ATTEMPT_CHANCE = 0.35f;
	private static final int MIN_DISTANCE = 40;
	private static final int MAX_DISTANCE = 80;
	/**
	 * Around a player travelling at least this fast (blocks a second, sideways: flying, riding, a fast runner), an attempt at
	 * every look, ahead along the way ({@link #SPACING} keeps them from bunching). Faster than {@link #TELEPORT_SPEED}
	 * between two looks was a teleport, not a journey.
	 */
	private static final double TRAVEL_SPEED = 7;
	private static final double TELEPORT_SPEED = 120;
	/** Ahead of a traveller: this far along the way, and up to this angle (radians) either side of it. */
	private static final int AHEAD_MIN = 64;
	private static final int AHEAD_MAX = 128;
	private static final double AHEAD_SPREAD = 0.5;
	/**
	 * At most one wandering rogue within this many blocks (sideways) of any player: new ones keep twice that from every other,
	 * and a little more for the strolling they do afterwards ({@link #SPACING}), so no spot in the world has two within it.
	 * And no new one while this many rogues and wanderers are loaded in a level (sect members don't count: one player in a
	 * large sect mustn't stop them turning up for everyone else).
	 */
	public static final double CROWD_RADIUS = 160;
	public static final double SPACING = CROWD_RADIUS * 2 + 32;
	private static final int LEVEL_CAP = 40;
	/** Rogues and wanderers keep this far from a sect's grounds; a sect not founded yet has grounds taken as this across. */
	public static final int SECT_CLEARANCE = 160;
	static final int UNFOUNDED_GROUNDS = 80;

	/** Where each player was at the last look, to tell how fast and which way they're going. */
	private static final Map<UUID, Seen> LAST = new HashMap<>();

	private record Seen(ResourceKey<Level> dimension, Vec3 pos) {}

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
		int now = server.getTickCount();
		if (now % CHECK != 0) return;
		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		LAST.keySet().retainAll(players.stream().map(ServerPlayer::getUUID).toList());
		for (ServerPlayer player : players) {
			Vec3 heading = heading(player);
			if (player.isSpectator()) continue;
			boolean attempt = heading != null || now % INTERVAL == 0 && player.getRandom().nextFloat() < ATTEMPT_CHANCE;
			if (attempt) trySpawnNear(player.serverLevel(), player, heading);
		}
	}

	/** Which way a player is travelling (a unit vector, sideways) since the last look, or null if they aren't (or teleported). */
	private static Vec3 heading(ServerPlayer player) {
		Seen before = LAST.put(player.getUUID(), new Seen(player.level().dimension(), player.position()));
		if (before == null || before.dimension() != player.level().dimension()) return null;
		double dx = player.getX() - before.pos().x, dz = player.getZ() - before.pos().z;
		double speed = Math.sqrt(dx * dx + dz * dz) * 20.0 / CHECK;
		return speed < TRAVEL_SPEED || speed > TELEPORT_SPEED ? null : new Vec3(dx, 0, dz).normalize();
	}

	/**
	 * Whether living rogues and mortal wanderers turn up in this dimension: never in a passage, the Inner Realm or the Netherworld,
	 * nor (for now) in the Astral Realm, whose worlds are wild.
	 */
	public static boolean spawnsLiving(ResourceKey<Level> dimension) {
		return !ModDimensions.isPassage(dimension) && !ModDimensions.isInnerRealm(dimension) && !ModDimensions.isNetherworld(dimension)
				&& !ModDimensions.isAstral(dimension);
	}

	/**
	 * One attempt around {@code player}: 40-80 blocks off, or ahead along {@code heading} if they're travelling. In the
	 * Netherworld it is one of the dead who turns up (GhostCultivatorNpc), on the high realms' curve and never a mortal, and
	 * never on the Ghost Gate's ground.
	 */
	static void trySpawnNear(ServerLevel level, ServerPlayer player, Vec3 heading) {
		boolean dead = ModDimensions.isNetherworld(level.dimension());
		if (!spawnsLiving(level.dimension()) && !dead) return;
		RandomSource random = level.getRandom();
		if (CultivatorNpc.countWanderers(level) >= LEVEL_CAP) return;
		boolean upper = dead || ModDimensions.isUpperRealm(level.dimension());
		int rank = rollRank(random, upper, false);
		if (rank == Integer.MIN_VALUE) return;
		boolean mortal = rank < 0;
		if (RogueIndex.anyWithin(level, player.getX(), player.getZ(), CROWD_RADIUS, mortal)) return;
		BlockPos spot = heading == null ? findSpot(level, player, random, mortal) : findSpotAhead(level, player, heading, random, mortal);
		if (spot == null || dead && GhostGate.protects(level, spot)) return;
		CultivatorNpc npc = CultivatorNpc.createFor(level);
		if (npc == null) return;
		npc.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360, 0);
		// The land's wandering ghosts stop short of Ghost King: the kings keep their palaces (GhostPalacePiece).
		if (npc instanceof GhostCultivatorNpc ghost) {
			ghost.becomeGhost(Math.min(GhostCultivatorNpc.WANDERER_CAP, rank), Alignment.randomRogue(random), random);
		} else {
			// Now and then the Upper Realm's peak is one of Dao Integration (setRank holds it to the room the realm has).
			if (upper && rank == PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION) && random.nextDouble() < DAO_INTEGRATION_SHARE) {
				rank = PlayerCultivation.rank(Realm.DAO_INTEGRATION, Stage.EARLY) + random.nextInt(Stage.values().length);
			}
			npc.becomeRogue(rank, Alignment.randomRogue(random), random);
		}
		level.addFreshEntity(npc);
	}

	/** A spot 40-80 blocks from the player, in any direction. */
	private static BlockPos findSpot(ServerLevel level, ServerPlayer player, RandomSource random, boolean mortal) {
		for (int attempt = 0; attempt < 8; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			int distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE);
			BlockPos feet = spotAt(level, player.getBlockX() + (int) (Math.cos(angle) * distance),
					player.getBlockZ() + (int) (Math.sin(angle) * distance), player.getBlockY(), mortal);
			if (feet != null) return feet;
		}
		return null;
	}

	/**
	 * A spot ahead of a traveller, 64-128 blocks along {@code heading} and a little either side. The far end is tried first;
	 * land that far ahead may not be loaded yet, so later tries come nearer.
	 */
	private static BlockPos findSpotAhead(ServerLevel level, ServerPlayer player, Vec3 heading, RandomSource random, boolean mortal) {
		double way = Math.atan2(heading.z, heading.x);
		for (int attempt = 0; attempt < 8; attempt++) {
			double angle = way + (random.nextDouble() * 2 - 1) * AHEAD_SPREAD;
			int distance = AHEAD_MAX - attempt * (AHEAD_MAX - AHEAD_MIN) / 7;
			BlockPos feet = spotAt(level, player.getBlockX() + (int) (Math.cos(angle) * distance),
					player.getBlockZ() + (int) (Math.sin(angle) * distance), player.getBlockY(), mortal);
			if (feet != null) return feet;
		}
		return null;
	}

	/**
	 * Solid, dry ground in column {@code x, z}, in loaded and ticking land, outside every barrier, out of every player's close
	 * sight, {@link #SPACING} from every other of its kind and {@link #SECT_CLEARANCE} from every sect's grounds; or null.
	 */
	private static BlockPos spotAt(ServerLevel level, int x, int z, int nearY, boolean mortal) {
		BlockPos column = new BlockPos(x, nearY, z);
		if (!level.isLoaded(column) || !level.isPositionEntityTicking(column)) return null;
		BlockPos feet = ground(level, x, z, nearY);
		if (feet == null || Formations.shelters(level, feet)) return null;
		Vec3 centre = Vec3.atBottomCenterOf(feet);
		if (level.getNearestPlayer(centre.x, centre.y, centre.z, 24, false) != null) return null;
		return wouldCrowd(level, centre, mortal) || nearSect(level, feet) ? null : feet;
	}

	/** Whether one at {@code pos} would stand within {@link #SPACING} of another of its kind (rogue, or mortal wanderer), loaded or not. */
	static boolean wouldCrowd(ServerLevel level, Vec3 pos, boolean mortal) {
		return RogueIndex.anyWithin(level, pos.x, pos.z, SPACING, mortal);
	}

	/**
	 * Within {@link #SECT_CLEARANCE} of a sect's grounds: a founded sect's (its barrier's reach from the core), or where one
	 * may yet be founded, its site not settled yet (SectSites#unsettled; the grounds taken as {@link #UNFOUNDED_GROUNDS}
	 * across). Sites round players are settled within seconds of their coming, so this only waits a little.
	 */
	static boolean nearSect(ServerLevel level, BlockPos pos) {
		for (Sect sect : SectData.get(level.getServer()).all()) {
			if (sect.extinct || sect.dimension != level.dimension()) continue;
			double dx = sect.core.getX() - pos.getX(), dz = sect.core.getZ() - pos.getZ();
			double reach = sect.barrierRadius + SECT_CLEARANCE;
			if (dx * dx + dz * dz <= reach * reach) return true;
		}
		return SectSites.unsettled(level, pos, SECT_CLEARANCE + UNFOUNDED_GROUNDS);
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
