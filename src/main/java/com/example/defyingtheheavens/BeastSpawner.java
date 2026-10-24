package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the wild demon beasts (DemonBeast) come from: they turn up round players as the rogues do (NpcSpawner), by day and by night, out
 * of sight, 24 to 64 blocks off, in the lands their kind keeps (BeastKind#lands, biome tags {@code beast/<id>}): on the ground, in the
 * water (a flood dragon only in water deep enough to swim, a turtle or a crocodile on the shore or in it), a bird above the ground. A
 * land's kinds are picked by their tier's weight, the rare far rarer; in the Upper Realm, richer in qi, the stronger kinds come the more
 * often (BeastKind.Tier#upperWeight) and each beast stands the higher (DemonBeast#rollRank). Wolves come in packs, monkeys in troops.
 * <p>
 * Held to a few round each player ({@link #NEAR_CAP}) and in each world ({@link #LEVEL_CAP}); never within a sect's barrier or close to
 * its grounds (a clan's den is a sect's), nor in peace (a beast is a monster: it would not stay). They wander off as monsters do when no
 * one is near (they are not kept). The gamerule {@code demonBeastsSpawn} turns them off.
 */
public final class BeastSpawner {
	/** How often each player is tried (ticks), and how likely a try is to bring beasts. */
	private static final int INTERVAL = 200;
	private static final float CHANCE = 0.45f;
	private static final int MIN_DISTANCE = 24, MAX_DISTANCE = 64;
	/** At most this many beasts within {@link #NEAR} of a player, and in a world. */
	public static final int NEAR_CAP = 6, LEVEL_CAP = 80;
	private static final double NEAR = 96;
	/** Kept this far from a sect's barrier. */
	private static final int SECT_CLEARANCE = 48;

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % INTERVAL != 0) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.isSpectator() || player.getRandom().nextFloat() >= CHANCE) continue;
			ServerLevel level = player.serverLevel();
			// Not with the world's own spawning off either (doMobSpawning: they came all the same before the sweep of 2026-10-23).
			if (!level.getGameRules().getBoolean(ModGameRules.DEMON_BEASTS_SPAWN) || !level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)
					|| level.getDifficulty() == Difficulty.PEACEFUL) continue;
			if (!NpcSpawner.spawnsLiving(level.dimension()) || level.dimensionType().hasCeiling()) continue;
			trySpawnNear(level, player);
		}
	}

	/** One try round {@code player}: a spot, a kind of its land, its pack. @return how many came */
	static int trySpawnNear(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		int near = count(level, player, NEAR), all = countAll(level);
		if (near >= NEAR_CAP || all >= LEVEL_CAP) return 0;
		double angle = random.nextDouble() * Math.PI * 2;
		int distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE);
		int x = player.getBlockX() + (int) (Math.cos(angle) * distance), z = player.getBlockZ() + (int) (Math.sin(angle) * distance);
		BlockPos column = new BlockPos(x, player.getBlockY(), z);
		if (!level.isLoaded(column) || !level.isPositionEntityTicking(column)) return 0;
		BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
		BeastKind kind = pick(level.getBiome(ground), ModDimensions.isUpperRealm(level.dimension()), random);
		if (kind == null) return 0;
		BlockPos spot = spotFor(level, kind, x, z);
		if (spot == null || Formations.shelters(level, spot) || nearSect(level, spot)) return 0;
		if (level.getNearestPlayer(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, MIN_DISTANCE - 4, false) != null) return 0;
		int pack = kind.packSize(random), came = 0;
		// The caps hold for each of a pack (a pack of five came whole to a player with five round them already, before the sweep of
		// 2026-10-23).
		for (int i = 0; i < pack && near + came < NEAR_CAP && all + came < LEVEL_CAP; i++) {
			// The rest of a pack beside the first, in its kind's lands too (one at a land's edge stays on its own side of it).
			BlockPos at = i == 0 ? spot : spotFor(level, kind, spot.getX() + random.nextInt(7) - 3, spot.getZ() + random.nextInt(7) - 3);
			if (at == null || i > 0 && !landAt(level, at).is(kind.lands)) continue;
			if (spawn(level, kind, at) != null) came++;
		}
		return came;
	}

	/** The land a column is (its biome at the ground, as a kind is picked by: Terralith's biomes differ with height). */
	static Holder<Biome> landAt(ServerLevel level, BlockPos pos) {
		return level.getBiome(new BlockPos(pos.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()), pos.getZ()));
	}

	/** A beast of {@code kind} at {@code at}, its rank rolled for where it is (DemonBeast#finalizeSpawn); or null if it couldn't be. */
	static DemonBeast spawn(ServerLevel level, BeastKind kind, BlockPos at) {
		EntityType<DemonBeast> type = ModEntities.beast(kind);
		DemonBeast beast = type.create(level);
		if (beast == null) return null;
		beast.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
		beast.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.NATURAL, null, null);
		if (!level.noCollision(beast)) return null;
		level.addFreshEntity(beast);
		return beast;
	}

	/** One of the kinds whose land {@code biome} is, by its tier's weight; null if none keep it. */
	static BeastKind pick(Holder<Biome> biome, boolean upper, RandomSource random) {
		List<BeastKind> kinds = new ArrayList<>();
		int total = 0;
		for (BeastKind kind : BeastKind.values()) {
			if (!biome.is(kind.lands)) continue;
			kinds.add(kind);
			total += upper ? kind.tier.upperWeight : kind.tier.weight;
		}
		if (total <= 0) return null;
		int roll = random.nextInt(total);
		for (BeastKind kind : kinds) {
			roll -= upper ? kind.tier.upperWeight : kind.tier.weight;
			if (roll < 0) return kind;
		}
		return null;
	}

	/**
	 * Where one of {@code kind} may be in column {@code x, z}: a swimmer two blocks under the surface of water at least four deep; a bird
	 * six to ten blocks over the ground; the rest on dry ground (or, amphibious, on the water as well), with room to stand.
	 */
	static BlockPos spotFor(ServerLevel level, BeastKind kind, int x, int z) {
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		if (top <= level.getMinBuildHeight() + 1) return null;
		BlockPos surface = new BlockPos(x, top, z), below = surface.below();
		boolean water = level.getFluidState(below).is(FluidTags.WATER);
		switch (kind.moves) {
			case SWIMMING -> {
				if (!water) return null;
				int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
				return top - floor >= 4 ? new BlockPos(x, top - 2, z) : null;
			}
			case FLYING -> {
				if (water) return null;
				return new BlockPos(x, top + 6 + level.getRandom().nextInt(5), z);
			}
			case AMPHIBIOUS -> {
				if (water) return new BlockPos(x, top - 1, z);
				return standable(level, surface) ? surface : null;
			}
			default -> {
				return !water && standable(level, surface) ? surface : null;
			}
		}
	}

	private static boolean standable(ServerLevel level, BlockPos feet) {
		BlockPos below = feet.below();
		return level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()
				&& level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP) && level.getFluidState(below).isEmpty()
				&& !level.getBlockState(below).is(ModBlocks.SECT_BARRIER);
	}

	/** Within {@link #SECT_CLEARANCE} of a sect's barrier (a clan's den too), or where a sect may yet be founded. */
	static boolean nearSect(ServerLevel level, BlockPos pos) {
		for (Sect sect : SectData.get(level.getServer()).all()) {
			if (sect.extinct || sect.dimension != level.dimension()) continue;
			double dx = sect.core.getX() - pos.getX(), dz = sect.core.getZ() - pos.getZ();
			double reach = sect.barrierRadius + SECT_CLEARANCE;
			if (dx * dx + dz * dz <= reach * reach) return true;
		}
		return SectSites.unsettled(level, pos, SECT_CLEARANCE + NpcSpawner.UNFOUNDED_GROUNDS);
	}

	static int count(ServerLevel level, ServerPlayer player, double radius) {
		return level.getEntitiesOfClass(DemonBeast.class, new AABB(player.blockPosition()).inflate(radius), DemonBeast::isAlive).size();
	}

	static int countAll(ServerLevel level) {
		int n = 0;
		for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) if (entity instanceof DemonBeast) n++;
		return n;
	}

	private BeastSpawner() {}
}
