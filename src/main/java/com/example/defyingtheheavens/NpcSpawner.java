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
 * the Netherworld's too, where those who turn up are its dead, ghost cultivators (GhostCultivatorNpc), never the living; and
 * the Great Void's over its own, from Heavenly Being Early to Void Shattering Grand Perfection ({@link #rollGreatVoidRank}).
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

	/**
	 * The Great Void's rogues (2026-10-25) run over their own curve, from Heavenly Being Early (a Holy Land's youngest, wandered off)
	 * to Void Shattering Grand Perfection, never a mortal; at its peak this share are of Sacred Vessel instead, while the realm has room
	 * (HighRealms).
	 */
	public static final int GREAT_VOID_FLOOR = PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY),
			GREAT_VOID_TOP = PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.GRAND_PERFECTION);
	public static final double SACRED_VESSEL_SHARE = 0.25;

	/** The rank the roll lands on in the Great Void ({@link #GREAT_VOID_FLOOR} up), or {@link Integer#MIN_VALUE} for nobody (unless {@code mustFind}). */
	public static int rollGreatVoidRank(RandomSource random, boolean mustFind) {
		return rollBetween(random, GREAT_VOID_FLOOR, GREAT_VOID_TOP, mustFind);
	}

	/**
	 * The Immortal Realm's wanderers (phase 2, 2026-10-25): from Entering Nirvana Early (those come up through the Gate) to True Immortal
	 * Grand Perfection on its own curve, never a mortal; none of the realms past it (phase 3: the few of those are its sects' Sect Lords
	 * and the Court's, and whoever climbs there).
	 */
	public static final int IMMORTAL_FLOOR = PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.EARLY),
			IMMORTAL_TOP = PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION);

	/** The rank the roll lands on in the Immortal Realm ({@link #IMMORTAL_FLOOR} up), or {@link Integer#MIN_VALUE} for nobody (unless {@code mustFind}). */
	public static int rollImmortalRank(RandomSource random, boolean mustFind) {
		return rollBetween(random, IMMORTAL_FLOOR, IMMORTAL_TOP, mustFind);
	}

	private static int rollBetween(RandomSource random, int floor, int top, boolean mustFind) {
		int steps = top - floor + 1;
		for (int tries = 0; tries < 64; tries++) {
			double roll = random.nextDouble();
			for (int step = 0; step < steps; step++) {
				roll -= chance(step, steps);
				if (roll < 0) return floor + step;
			}
			if (!mustFind) return Integer.MIN_VALUE;
		}
		return floor;
	}

	/** The rank the roll lands on where {@code level} is: the Immortal Realm's curve, the Great Void's, the Upper Realm's or the lower realms'. */
	public static int rollRank(RandomSource random, ResourceKey<Level> dimension, boolean mustFind) {
		if (ModDimensions.isImmortalRealm(dimension)) return rollImmortalRank(random, mustFind);
		return ModDimensions.isGreatVoid(dimension) ? rollGreatVoidRank(random, mustFind) : rollRank(random, ModDimensions.isUpperRealm(dimension), mustFind);
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
	 * nor (for now) in the Astral Realm, whose worlds are wild, nor in an immortal's own small world (SmallWorlds: its owner's and its
	 * guests'; the wild beasts keep out by this too, BeastSpawner).
	 */
	public static boolean spawnsLiving(ResourceKey<Level> dimension) {
		return !ModDimensions.isPassage(dimension) && !ModDimensions.isInnerRealm(dimension) && !ModDimensions.isNetherworld(dimension)
				&& !ModDimensions.isAstral(dimension) && !ModDimensions.isSmallWorld(dimension);
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
		boolean greatVoid = ModDimensions.isGreatVoid(level.dimension()), immortal = ModDimensions.isImmortalRealm(level.dimension());
		boolean upper = dead || ModDimensions.isUpperRealm(level.dimension());
		int rank = immortal ? rollImmortalRank(random, false) : greatVoid ? rollGreatVoidRank(random, false) : rollRank(random, upper, false);
		if (rank == Integer.MIN_VALUE) return;
		boolean mortal = rank < 0;
		if (RogueIndex.anyWithin(level, player.getX(), player.getZ(), CROWD_RADIUS, mortal)) return;
		BlockPos spot = heading == null ? findSpot(level, player, random, mortal) : findSpotAhead(level, player, heading, random, mortal);
		if (spot == null || dead && GhostGate.protects(level, spot)) return;
		// None of the Immortal Realm's wanderers out where its land breaks up into the Chaos Frontier.
		if (immortal && ChaosFrontier.pastSettled(spot.getX(), spot.getZ())) return;
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
			// And the Great Void's now and then one of Sacred Vessel.
			if (greatVoid && rank == GREAT_VOID_TOP && random.nextDouble() < SACRED_VESSEL_SHARE) {
				rank = PlayerCultivation.rank(Realm.SACRED_VESSEL, Stage.EARLY) + random.nextInt(Stage.values().length);
			}
			npc.becomeRogue(rank, Alignment.randomRogue(random), random);
			// Now and then a demon (the user's design, 2026-10-23): of the race whose land this is; in the upper realms, of any.
			if (!mortal && random.nextInt(DEMON_IN) == 0) {
				Bloodline race = upper || greatVoid || immortal ? DenStructure.upperRace(random) : DenStructure.raceOfLand(level.getBiome(spot));
				if (race != null) npc.setRace(race, random.nextInt(3));
			}
		}
		level.addFreshEntity(npc);
	}

	/** One wandering cultivator in this many is a demon, where its race's land is (DenStructure#raceOfLand). */
	public static final int DEMON_IN = 8;

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
