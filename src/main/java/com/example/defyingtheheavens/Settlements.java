package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The settlements of the lower realms' people on the Astral Realm's worlds (the user's ask, 2026-10-19): where Four Axis cultivators
 * find the wishes that fill their axes (Wishes, FourAxis). The worlds' land is laid out in squares of {@link #CELL} blocks, and about
 * every other one ({@link #CHANCE}) holds a site, somewhere in its middle half, on a world that would have people (not a barren,
 * ashen or volcanic one), clear of its land's edge. A site is built when a player first comes within {@link #TRIGGER} blocks of it
 * with its ground loaded: a well, {@link #HUTS} huts round it in the world's own wood, and {@link #MIN_SETTLERS} to
 * {@link #MAX_SETTLERS} settlers (mortals, and cultivators of Qi Refining to Core Formation, of the righteous path or none), each
 * with a wish (Wishes#assign) and kept to its home ground (CultivatorNpc#settledAt). A site is only ever on dry ground (the land's
 * own numbers: PlanetTerrain); ground that won't take one after all leaves it empty. Which sites are built, and what they're called,
 * is saved.
 * <p>
 * Kept alive (the user's ask, 2026-10-20): no monster is born within {@link #SHELTER} blocks of a settlement's well
 * ({@link #shelters}), and one that has lost people takes in a newcomer every {@link #ARRIVAL_TICKS} from when it was first found
 * short, until it has as many as it was built with ({@link #replenish}); those whose time came while nobody was near arrive together.
 */
public final class Settlements {
	public static final int CELL = 640;
	public static final float CHANCE = 0.5f;
	/** A site is built when a player comes this close; its name is shown at {@link #NEAR}. */
	public static final double TRIGGER = 96, NEAR = 40;
	public static final int HUTS = 4, MIN_SETTLERS = 5, MAX_SETTLERS = 8;
	/** How far the huts stand from the well, and how much ground round the well must be loaded to build. */
	private static final int HUT_DISTANCE = 11, REACH = HUT_DISTANCE + 6;
	private static final int CHECK_TICKS = 40;
	/** No monster is born this close to a settlement's well (blocks, across). */
	public static final double SHELTER = 64;
	/** A settlement short of people takes in a newcomer this often (ticks): every other day. */
	public static final long ARRIVAL_TICKS = 2L * 24000;
	/**
	 * Its home ground round its well (its people keep within CultivatorGoals#SETTLER_ROAM of it): they are counted only with all of
	 * that in the world's reach.
	 */
	static final int HOME_REACH = 32;
	/**
	 * Its people are counted out this far, though: a fight or a long meditation can leave one past its home ground a while (seen in
	 * game, 2026-10-20: two at 33 and 50 blocks after a Merit threat), and one out of the count for two days was made up for by a
	 * newcomer, the settlement one too many when it walked home.
	 */
	static final int CENSUS = 128;
	/** A newcomer comes in from about this far out. */
	private static final int ARRIVAL_DISTANCE = 20;

	/** A world's wood: its huts are built of it. */
	public record Palette(Block log, Block planks, Block slab, Block fence) {}

	public static final Palette OAK = new Palette(Blocks.OAK_LOG, Blocks.OAK_PLANKS, Blocks.OAK_SLAB, Blocks.OAK_FENCE);
	private static final Palette SPRUCE = new Palette(Blocks.SPRUCE_LOG, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_SLAB, Blocks.SPRUCE_FENCE);
	private static final Palette ACACIA = new Palette(Blocks.ACACIA_LOG, Blocks.ACACIA_PLANKS, Blocks.ACACIA_SLAB, Blocks.ACACIA_FENCE);
	private static final Palette JUNGLE = new Palette(Blocks.JUNGLE_LOG, Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_SLAB, Blocks.JUNGLE_FENCE);
	private static final Palette CHERRY = new Palette(Blocks.CHERRY_LOG, Blocks.CHERRY_PLANKS, Blocks.CHERRY_SLAB, Blocks.CHERRY_FENCE);
	private static final Palette MANGROVE = new Palette(Blocks.MANGROVE_LOG, Blocks.MANGROVE_PLANKS, Blocks.MANGROVE_SLAB, Blocks.MANGROVE_FENCE);
	private static final Palette DARK_OAK = new Palette(Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_SLAB, Blocks.DARK_OAK_FENCE);
	private static final Palette BIRCH = new Palette(Blocks.BIRCH_LOG, Blocks.BIRCH_PLANKS, Blocks.BIRCH_SLAB, Blocks.BIRCH_FENCE);

	public static Palette palette(AstralPlanets.Kind kind) {
		return switch (kind) {
			case FROZEN, TAIGA -> SPRUCE;
			case DESERT, MESA, SAVANNA -> ACACIA;
			case JUNGLE -> JUNGLE;
			case BLOSSOM -> CHERRY;
			case SWAMP -> MANGROVE;
			case MUSHROOM -> DARK_OAK;
			case CRYSTAL -> BIRCH;
			default -> OAK;
		};
	}

	/** Worlds people would live on. */
	static boolean habitable(AstralPlanets.Kind kind) {
		return kind != AstralPlanets.Kind.BARREN && kind != AstralPlanets.Kind.ASHEN && kind != AstralPlanets.Kind.VOLCANIC;
	}

	private static final String[] FIRST = {"Green Willow", "Still Water", "Red Pine", "Cloud Gate", "Peach Blossom", "White Stone", "Jade Spring",
			"Quiet Valley", "Golden Field", "Plum Rain", "Bamboo Shade", "Morning Mist", "Far Shore", "Lotus Pond", "Crane Rest", "Moon Well",
			"Falling Leaf", "Silver Brook", "Autumn Hill", "Wild Goose"};
	private static final String[] LAST = {"Village", "Hamlet", "Settlement", "Refuge"};

	// --- The sites ---

	/** A site: its square of the lattice, where its well goes, and the world it is on. */
	public record Site(long cell, int x, int z, AstralPlanets.Planet planet) {}

	static long cellKey(int cx, int cz) {
		return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
	}

	private static long mix(long h) {
		h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
		h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
		return h ^ (h >>> 31);
	}

	/** The site in the square ({@code cx}, {@code cz}) of the lattice, or null if it has none. The same for every server of the seed. */
	public static Site site(AstralPlanets planets, int cx, int cz) {
		long h = mix(planets.seed() ^ cellKey(cx, cz) * 0x9E3779B97F4A7C15L);
		if ((h & 0xFFFF) >= CHANCE * 0x10000) return null;
		int x = cx * CELL + CELL / 4 + (int) Math.floorMod(h >>> 16, (long) CELL / 2);
		int z = cz * CELL + CELL / 4 + (int) Math.floorMod(h >>> 40, (long) CELL / 2);
		AstralPlanets.Planet planet = planets.planetAt(x, z);
		if (planet == null || !habitable(planet.kind())) return null;
		// Clear of the land's edge, all round.
		for (int[] d : new int[][] {{REACH * 3, 0}, {-REACH * 3, 0}, {0, REACH * 3}, {0, -REACH * 3}}) {
			if (!planet.equals(planets.planetAt(x + d[0], z + d[1]))) return null;
		}
		// On dry ground (worked out from the land's own numbers, nothing loaded): a site in the sea or on a lava lake would only ever
		// be left empty, and its smoke would have called people to nothing.
		if (PlanetTerrain.of(planet).column(x - planet.landX(), z - planet.landZ(), false).underFluid()) return null;
		return new Site(cellKey(cx, cz), x, z, planet);
	}

	// --- Coming near ---

	/** The settlement each player was last told of (so it's said once, as they come). */
	private static final Map<UUID, Long> TOLD = new HashMap<>();
	/** When each player was last shown the smoke of the nearest settlement ({@link #SMOKE_TICKS}). */
	private static final Map<UUID, Integer> SMOKE = new HashMap<>();
	/** One with an axis to fill (FourAxis#current) is shown, this often, the smoke of the nearest settlement within {@link #SMOKE_CELLS}. */
	private static final int SMOKE_TICKS = 1200, SMOKE_CELLS = 2;

	/**
	 * Every {@link #CHECK_TICKS}: builds the sites players come near, names the built ones they reach, and takes newcomers into those
	 * short of people ({@link #replenish}).
	 */
	static void tick(MinecraftServer server, ServerLevel surfaces, AstralPlanets planets) {
		if (server.getTickCount() % CHECK_TICKS != 0 || surfaces.players().isEmpty()) return;
		SettlementData data = SettlementData.get(server);
		java.util.Set<Long> counted = new java.util.HashSet<>();
		for (ServerPlayer p : surfaces.players()) {
			if (!p.isAlive() || p.isSpectator()) continue;
			showSmoke(p, planets, server.getTickCount());
			int x0 = Mth.floor((p.getX() - TRIGGER) / CELL), x1 = Mth.floor((p.getX() + TRIGGER) / CELL);
			int z0 = Mth.floor((p.getZ() - TRIGGER) / CELL), z1 = Mth.floor((p.getZ() + TRIGGER) / CELL);
			for (int cx = x0; cx <= x1; cx++) {
				for (int cz = z0; cz <= z1; cz++) {
					long key = cellKey(cx, cz);
					Settlement known = data.get(key);
					if (known != null) {
						if (known.built() && known.distanceTo(p.getX(), p.getZ()) <= NEAR && !Long.valueOf(key).equals(TOLD.put(p.getUUID(), key))) {
							p.displayClientMessage(Component.translatable(ModLang.MSG_SETTLEMENT_NEAR, known.name()), true);
						}
						if (known.built() && known.distanceTo(p.getX(), p.getZ()) <= TRIGGER && counted.add(key)) {
							Settlement now = replenish(surfaces, known, surfaces.getGameTime(), surfaces.getRandom());
							if (!now.equals(known)) data.put(key, now);
						}
						continue;
					}
					Site site = site(planets, cx, cz);
					if (site == null || Math.hypot(site.x() + 0.5 - p.getX(), site.z() + 0.5 - p.getZ()) > TRIGGER || !loaded(surfaces, site.x(), site.z())) continue;
					RandomSource random = RandomSource.create(mix(planets.seed() + key));
					Built built = build(surfaces, site.x(), site.z(), palette(site.planet().kind()), random);
					data.put(key, built == null ? Settlement.empty(site.x(), site.z()) : Settlement.of(built));
				}
			}
		}
	}

	// --- Kept alive ---

	/** Whether a monster may not be born at {@code pos}: within {@link #SHELTER} of a settlement's well, on the Astral Realm's worlds. */
	public static boolean shelters(ServerLevel level, BlockPos pos) {
		return level.dimension() == ModDimensions.ASTRAL_SURFACES && sheltered(SettlementData.get(level.getServer()), pos.getX() + 0.5, pos.getZ() + 0.5);
	}

	/**
	 * Whether ({@code x}, {@code z}) is within {@link #SHELTER} of a built settlement's well. A well stands in the middle half of its
	 * square of the lattice, so only the square ({@code x}, {@code z}) is in can hold one that near.
	 */
	static boolean sheltered(SettlementData data, double x, double z) {
		Settlement s = data.get(cellKey(Mth.floor(x / CELL), Mth.floor(z / CELL)));
		return s != null && s.built() && s.distanceTo(x, z) <= SHELTER;
	}

	/**
	 * A built settlement short of the people it was built with (deaths): from when it was first found short, a newcomer every
	 * {@link #ARRIVAL_TICKS} until it is whole again, those whose time came while nobody was near arriving together. Its people are
	 * counted only while all its home ground is in the world's reach (they could be standing anywhere on it), and out to
	 * {@link #CENSUS}: one strayed past its home ground (off after what struck it, or sitting in meditation on its way back) is still
	 * one of them, not a death to make up for.
	 * @return the settlement as it stands now (when it was found short, if it still is)
	 */
	static Settlement replenish(ServerLevel level, Settlement s, long now, RandomSource random) {
		if (!s.built() || !ticking(level, s.well())) return s;
		BlockPos well = s.well();
		int living = level.getEntitiesOfClass(CultivatorNpc.class, new net.minecraft.world.phys.AABB(well).inflate(CENSUS, 64, CENSUS),
				npc -> npc.isAlive() && well.equals(npc.settledAt())).size();
		if (living >= s.people()) return s.shortSince() < 0 ? s : s.withShortSince(-1);
		long since = s.shortSince() < 0 ? now : s.shortSince();
		while (living < s.people() && now - since >= ARRIVAL_TICKS) {
			if (arrive(level, s, random) == null) break;
			living++;
			since += ARRIVAL_TICKS;
		}
		return s.withShortSince(living >= s.people() ? -1 : since);
	}

	/** All of a settlement's home ground round {@code well} ticks its beings (so none of its people is out of the count). */
	private static boolean ticking(ServerLevel level, BlockPos well) {
		for (int dx = -HOME_REACH; dx <= HOME_REACH; dx += HOME_REACH) {
			for (int dz = -HOME_REACH; dz <= HOME_REACH; dz += HOME_REACH) {
				if (!level.isPositionEntityTicking(well.offset(dx, 0, dz))) return false;
			}
		}
		return true;
	}

	/** A newcomer comes to {@code s}, walking in from its edge, and those near are told. @return them, or null if there is no ground */
	static CultivatorNpc arrive(ServerLevel level, Settlement s, RandomSource random) {
		BlockPos well = s.well();
		BlockPos spot = null;
		for (int tries = 0; tries < 8 && spot == null; tries++) {
			double angle = random.nextDouble() * Math.PI * 2;
			BlockPos at = ground(level, Mth.floor(well.getX() + Math.cos(angle) * ARRIVAL_DISTANCE), Mth.floor(well.getZ() + Math.sin(angle) * ARRIVAL_DISTANCE),
					level.getMaxBuildHeight());
			if (at != null && level.isPositionEntityTicking(at)) spot = at;
		}
		if (spot == null) spot = well.offset(2, 0, 0);
		CultivatorNpc npc = settle(level, well, spot, random);
		if (npc == null) return null;
		for (ServerPlayer p : level.players()) {
			if (s.distanceTo(p.getX(), p.getZ()) <= TRIGGER) p.displayClientMessage(Component.translatable(ModLang.MSG_SETTLER_ARRIVES, s.name()), true);
		}
		return npc;
	}

	/** A settler of the well {@code well}, at {@code spot}: of the righteous path or none, with a wish, kept to its home ground. */
	private static CultivatorNpc settle(ServerLevel level, BlockPos well, BlockPos spot, RandomSource random) {
		CultivatorNpc npc = ModEntities.CULTIVATOR.create(level);
		if (npc == null) return null;
		// The righteous path, or none: three in four righteous.
		npc.becomeRogue(settlerRank(random), random.nextInt(Alignment.MAX + 1), random);
		npc.setSettledAt(well);
		npc.setPersistenceRequired();
		Wishes.assign(npc, random);
		npc.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360, 0);
		level.addFreshEntity(npc);
		return npc;
	}

	/**
	 * Smoke on the horizon: one seeking virtue for an axis, out of sight of any settlement, is told now and then which way the nearest
	 * one lies and how far (built or not yet: it is built as they come).
	 */
	private static void showSmoke(ServerPlayer p, AstralPlanets planets, int now) {
		if (now - SMOKE.getOrDefault(p.getUUID(), -SMOKE_TICKS) < SMOKE_TICKS || FourAxis.current(CultivationManager.own(p)) == null) return;
		int pcx = Mth.floor(p.getX() / CELL), pcz = Mth.floor(p.getZ() / CELL);
		Site nearest = null;
		double best = Double.MAX_VALUE;
		for (int cx = pcx - SMOKE_CELLS; cx <= pcx + SMOKE_CELLS; cx++) {
			for (int cz = pcz - SMOKE_CELLS; cz <= pcz + SMOKE_CELLS; cz++) {
				Site site = site(planets, cx, cz);
				if (site == null) continue;
				double d = Math.hypot(site.x() + 0.5 - p.getX(), site.z() + 0.5 - p.getZ());
				if (d < best) {
					best = d;
					nearest = site;
				}
			}
		}
		SMOKE.put(p.getUUID(), now);
		if (nearest == null || best <= NEAR) return;
		String way = TalkSystem.direction(p.blockPosition(), new BlockPos(nearest.x(), p.getBlockY(), nearest.z()));
		p.displayClientMessage(Component.translatable(ModLang.MSG_SETTLEMENT_SMOKE, Math.round(best), Component.translatable(ModLang.talkDirection(way))), true);
	}

	private static boolean loaded(ServerLevel level, int x, int z) {
		for (int cx = (x - REACH) >> 4; cx <= (x + REACH) >> 4; cx++) {
			for (int cz = (z - REACH) >> 4; cz <= (z + REACH) >> 4; cz++) {
				if (!level.getChunkSource().hasChunk(cx, cz)) return false;
			}
		}
		return true;
	}

	// --- The building ---

	/** A settlement just built: its well (at the ground's height), its name, its people. */
	public record Built(BlockPos well, String name, List<CultivatorNpc> settlers) {}

	/**
	 * Builds a settlement round ({@code x}, {@code z}) of {@code level}, its people with it (any level, so the game tests can).
	 * @return it, or null where the ground won't take one (water, lava, the void)
	 */
	public static Built build(ServerLevel level, int x, int z, Palette palette, RandomSource random) {
		return build(level, x, z, level.getMaxBuildHeight(), palette, random);
	}

	/** {@link #build}, finding the ground no higher than {@code ceiling} (the game tests' floor, under the test world's sea). */
	static Built build(ServerLevel level, int x, int z, int ceiling, Palette palette, RandomSource random) {
		BlockPos well = ground(level, x, z, ceiling);
		if (well == null) return null;
		String name = FIRST[random.nextInt(FIRST.length)] + " " + LAST[random.nextInt(LAST.length)];
		buildWell(level, well);
		double turn = random.nextDouble() * Math.PI * 2;
		for (int i = 0; i < HUTS; i++) {
			double angle = turn + i * Math.PI * 2 / HUTS + (random.nextDouble() - 0.5) * 0.4;
			BlockPos at = ground(level, well.getX() + (int) Math.round(Math.cos(angle) * HUT_DISTANCE),
					well.getZ() + (int) Math.round(Math.sin(angle) * HUT_DISTANCE), ceiling);
			if (at == null) continue;
			// Its doorway on the side that faces the well.
			Direction door = Direction.getNearest(well.getX() - at.getX(), 0, well.getZ() - at.getZ());
			buildHut(level, at, door, palette);
		}
		List<CultivatorNpc> settlers = new ArrayList<>();
		int count = MIN_SETTLERS + random.nextInt(MAX_SETTLERS - MIN_SETTLERS + 1);
		for (int i = 0; i < count; i++) {
			double angle = random.nextDouble() * Math.PI * 2, out = 3 + random.nextDouble() * 4;
			BlockPos spot = ground(level, Mth.floor(well.getX() + Math.cos(angle) * out), Mth.floor(well.getZ() + Math.sin(angle) * out), ceiling);
			if (spot == null) spot = well.offset(2, 0, 0);
			CultivatorNpc npc = settle(level, well, spot, random);
			if (npc == null) break;
			settlers.add(npc);
		}
		return new Built(well, name, settlers);
	}

	/** A settler's rank: a mortal nearly half the time, else Qi Refining most, Foundation Building some, Core Formation few. */
	static int settlerRank(RandomSource random) {
		if (random.nextFloat() < 0.45f) return CultivatorNpc.MORTAL;
		float roll = random.nextFloat();
		Realm realm = roll < 0.55f ? Realm.QI_REFINING : roll < 0.85f ? Realm.FOUNDATION_BUILDING : Realm.CORE_FORMATION;
		return PlayerCultivation.rank(realm, Stage.byIndex(random.nextInt(Stage.values().length)));
	}

	/** The first open block over solid ground at ({@code x}, {@code z}), no higher than {@code ceiling}; null over water, lava or nothing. */
	private static BlockPos ground(ServerLevel level, int x, int z, int ceiling) {
		BlockPos.MutableBlockPos top = new BlockPos.MutableBlockPos(x, Math.min(ceiling, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)), z);
		while (top.getY() > level.getMinBuildHeight() + 1 && level.getBlockState(top.below()).isAir()) top.move(Direction.DOWN);
		if (top.getY() <= level.getMinBuildHeight() + 1) return null;
		return level.getBlockState(top.below()).getFluidState().isEmpty() ? top.immutable() : null;
	}

	private static void set(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, Block.UPDATE_CLIENTS);
	}

	/** Open air {@code height} blocks up from {@code c}, {@code radius} round. */
	private static void clear(ServerLevel level, BlockPos c, int radius, int height) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				for (int dy = 0; dy < height; dy++) {
					BlockPos pos = c.offset(dx, dy, dz);
					if (!level.getBlockState(pos).isAir()) set(level, pos, Blocks.AIR.defaultBlockState());
				}
			}
		}
	}

	/** {@code top} laid under {@code c}'s level, {@code radius} round, with earth under it down to the ground where that dips. */
	private static void foundation(ServerLevel level, BlockPos c, int radius, BlockState top) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				set(level, c.offset(dx, -1, dz), top);
				for (int dy = -2; dy >= -8; dy--) {
					BlockPos pos = c.offset(dx, dy, dz);
					BlockState there = level.getBlockState(pos);
					if (!there.isAir() && there.getFluidState().isEmpty() && !there.canBeReplaced()) break;
					set(level, pos, Blocks.DIRT.defaultBlockState());
				}
			}
		}
	}

	/** The well: a stone ring round water, four posts and a little roof, on a trodden square. */
	private static void buildWell(ServerLevel level, BlockPos c) {
		clear(level, c, 3, 6);
		foundation(level, c, 3, Blocks.DIRT_PATH.defaultBlockState());
		BlockState stone = Blocks.STONE_BRICKS.defaultBlockState(), water = Blocks.WATER.defaultBlockState();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				boolean middle = dx == 0 && dz == 0;
				set(level, c.offset(dx, -2, dz), stone);
				set(level, c.offset(dx, -1, dz), middle ? water : stone);
				set(level, c.offset(dx, 0, dz), middle ? water : stone);
				if (Math.abs(dx) == 1 && Math.abs(dz) == 1) {
					set(level, c.offset(dx, 1, dz), Blocks.OAK_FENCE.defaultBlockState());
					set(level, c.offset(dx, 2, dz), Blocks.OAK_FENCE.defaultBlockState());
				}
				set(level, c.offset(dx, 3, dz), Blocks.STONE_BRICK_SLAB.defaultBlockState());
			}
		}
	}

	/** A hut: five by five, log corners, a doorway toward the well ({@code door}), windows in the other walls, a slab-edged roof. */
	private static void buildHut(ServerLevel level, BlockPos c, Direction door, Palette palette) {
		clear(level, c, 3, 7);
		BlockState planks = palette.planks().defaultBlockState();
		foundation(level, c, 2, planks);
		for (int dy = 0; dy <= 2; dy++) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					boolean edgeX = Math.abs(dx) == 2, edgeZ = Math.abs(dz) == 2;
					if (!edgeX && !edgeZ) continue;
					BlockPos pos = c.offset(dx, dy, dz);
					if (edgeX && edgeZ) {
						set(level, pos, palette.log().defaultBlockState());
						continue;
					}
					// The middle of a wall: the doorway on the well's side, a window in the others.
					boolean middle = edgeX ? dz == 0 : dx == 0;
					Direction side = edgeX ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
					if (middle && side == door && dy <= 1) {
						set(level, pos, Blocks.AIR.defaultBlockState());
					} else if (middle && side != door && dy == 1) {
						set(level, pos, edgeX ? Blocks.GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true)
								: Blocks.GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true));
					} else {
						set(level, pos, planks);
					}
				}
			}
		}
		BlockState slab = palette.slab().defaultBlockState();
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				int ring = Math.max(Math.abs(dx), Math.abs(dz));
				set(level, c.offset(dx, 3, dz), ring == 3 ? slab : planks);
				if (ring <= 1) set(level, c.offset(dx, 4, dz), slab);
			}
		}
		// Inside: a barrel and a workbench in the back corners, a lantern hung from the roof.
		Direction back = door.getOpposite(), left = back.getClockWise();
		set(level, c.relative(back).relative(left), Blocks.BARREL.defaultBlockState());
		set(level, c.relative(back).relative(left.getOpposite()), Blocks.CRAFTING_TABLE.defaultBlockState());
		set(level, c.above(2), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
	}

	// --- A Merit threat out: its people keep to their huts ---

	/**
	 * Settlements a Merit threat is out at (Wishes#takeOnThreat), by well: each settler whose threat it is, and when it lapses. Their
	 * people stay home until it is gone (CultivatorGoals.ShelterGoal; the user's ask, 2026-10-20: two were drawn into a fight and
	 * wandered off with it). Not saved: the settler whose threat it is raises it again as it is next about (ShelterGoal, Wishes#threatUntil).
	 */
	private static final Map<BlockPos, Map<UUID, Long>> ALARMS = new HashMap<>();

	/** {@code settler}'s threat is out until {@code until} (game time): its settlement's people go home. */
	public static void raiseAlarm(CultivatorNpc settler, long until) {
		BlockPos well = settler.settledAt();
		if (well != null) ALARMS.computeIfAbsent(well, k -> new HashMap<>()).put(settler.getUUID(), until);
	}

	/** {@code settler}'s threat is gone (slain, lost to other hands, lapsed): its people come out again, unless another's is still out. */
	public static void endAlarm(CultivatorNpc settler) {
		BlockPos well = settler.settledAt();
		Map<UUID, Long> out = well == null ? null : ALARMS.get(well);
		if (out == null) return;
		out.remove(settler.getUUID());
		if (out.isEmpty()) ALARMS.remove(well);
	}

	/** Whether a Merit threat is out at the settlement of {@code well} at game time {@code now} (one lapsed since counts no more). */
	public static boolean alarmed(BlockPos well, long now) {
		Map<UUID, Long> out = ALARMS.get(well);
		if (out == null) return false;
		out.values().removeIf(until -> until < now);
		if (!out.isEmpty()) return true;
		ALARMS.remove(well);
		return false;
	}

	/** Each settlement's huts, as found ({@link #huts}): the floor in the middle of each. Not saved (found again after a restart). */
	private static final Map<BlockPos, List<BlockPos>> HUTS_FOUND = new HashMap<>();

	/**
	 * The huts round {@code well} as they stand (found by the lantern hung in each, two above the middle of its floor; one torn down
	 * is gone from them), looked for once and kept: the middle of each hut's floor.
	 */
	public static List<BlockPos> huts(ServerLevel level, BlockPos well) {
		return HUTS_FOUND.computeIfAbsent(well.immutable(), w -> {
			List<BlockPos> found = new ArrayList<>();
			int reach = HUT_DISTANCE + 3;
			for (BlockPos pos : BlockPos.betweenClosed(w.offset(-reach, -4, -reach), w.offset(reach, 8, reach))) {
				BlockState state = level.getBlockState(pos);
				if (state.is(Blocks.LANTERN) && state.getValue(LanternBlock.HANGING) && !level.getBlockState(pos.below(2)).isSolid()
						&& level.getBlockState(pos.below(3)).isSolid()) {
					found.add(pos.below(2).immutable());
				}
			}
			return found;
		});
	}

	/**
	 * Where {@code settler} takes shelter while a threat is out ({@link #alarmed}): inside one of its settlement's huts (each settler
	 * its own, by who it is), or by the well if none stands; null for one of no settlement.
	 */
	public static BlockPos shelterFor(CultivatorNpc settler) {
		BlockPos well = settler.settledAt();
		if (well == null || !(settler.level() instanceof ServerLevel level)) return null;
		List<BlockPos> huts = huts(level, well);
		if (huts.isEmpty()) return well;
		return huts.get(Math.floorMod(settler.getUUID().hashCode(), huts.size()));
	}

	public static void forget(UUID player) {
		TOLD.remove(player);
		SMOKE.remove(player);
	}

	public static void clear() {
		TOLD.clear();
		SMOKE.clear();
		ALARMS.clear();
		HUTS_FOUND.clear();
	}

	// --- Saved ---

	/**
	 * A decided site: its well and name if it was built, or where it would have been if its ground wouldn't take one; the people it was
	 * built with, and since when it has been short of them (-1: it isn't).
	 */
	public record Settlement(BlockPos well, String name, int people, long shortSince) {
		static Settlement empty(int x, int z) {
			return new Settlement(new BlockPos(x, 0, z), "", 0, -1);
		}

		static Settlement of(Built built) {
			return new Settlement(built.well(), built.name(), built.settlers().size(), -1);
		}

		public boolean built() { return !name.isEmpty(); }

		Settlement withShortSince(long since) {
			return new Settlement(well, name, people, since);
		}

		double distanceTo(double x, double z) {
			return Math.hypot(well.getX() + 0.5 - x, well.getZ() + 0.5 - z);
		}
	}

	/** Which sites are decided (built or left empty), and the built ones' wells and names. Saved with the Overworld. */
	public static final class SettlementData extends SavedData {
		private static final String NAME = DefyingTheHeavens.MOD_ID + "_settlements";
		private final Map<Long, Settlement> sites = new HashMap<>();

		public static SettlementData get(MinecraftServer server) {
			return server.overworld().getDataStorage().computeIfAbsent(SettlementData::load, SettlementData::new, NAME);
		}

		public Settlement get(long cell) { return sites.get(cell); }

		public void put(long cell, Settlement settlement) {
			sites.put(cell, settlement);
			setDirty();
		}

		/** The built settlements, nearest {@code (x, z)} first. */
		public List<Settlement> nearest(double x, double z) {
			List<Settlement> built = new ArrayList<>();
			for (Settlement s : sites.values()) if (s.built()) built.add(s);
			built.sort((a, b) -> Double.compare(a.distanceTo(x, z), b.distanceTo(x, z)));
			return built;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			ListTag list = new ListTag();
			sites.forEach((cell, s) -> {
				CompoundTag entry = new CompoundTag();
				entry.putLong("Cell", cell);
				entry.putInt("X", s.well().getX());
				entry.putInt("Y", s.well().getY());
				entry.putInt("Z", s.well().getZ());
				if (s.built()) {
					entry.putString("Name", s.name());
					entry.putInt("People", s.people());
					if (s.shortSince() >= 0) entry.putLong("ShortSince", s.shortSince());
				}
				list.add(entry);
			});
			tag.put("Sites", list);
			return tag;
		}

		public static SettlementData load(CompoundTag tag) {
			SettlementData data = new SettlementData();
			for (Tag t : tag.getList("Sites", Tag.TAG_COMPOUND)) {
				CompoundTag entry = (CompoundTag) t;
				// From before settlements were kept alive: as many people as the fewest a settlement is built with.
				int people = entry.contains("People") ? entry.getInt("People") : MIN_SETTLERS;
				data.sites.put(entry.getLong("Cell"), new Settlement(new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z")),
						entry.getString("Name"), people, entry.contains("ShortSince") ? entry.getLong("ShortSince") : -1));
			}
			return data;
		}
	}

	private Settlements() {}
}
