package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Where the Immortal Realm's three primordial beasts keep (PrimordialBeast), and their coming and going (phase 2 of the realm plan,
 * 2026-10-26: "One of each at a time"). Each has its lair, found from the world's seed ({@link #findLair}) some {@link #LAIR_MIN} to
 * {@link #LAIR_MAX} blocks from the Southern Heavenly Gate, the three a third of the way round from one another: the Kun-Peng over the open
 * sea of clouds, no land near; the True Phoenix over a high peak (a massif's, or a jade pinnacle's: most massifs have their cities); the
 * True Dragon high over a massif or the pinnacles; all clear of the realm's cities and its immortal sects by {@link #SETTLEMENT_KEEP}. A beast comes to its lair when someone comes within {@link #APPEAR}
 * of it; slain, the next comes {@link #RETURN_TICKS} later. One not seen for {@link #LOST_TICKS} while someone stands near its lair is
 * taken as gone from the world (a command, or wandered off where it can't be loaded) and comes again; the one left behind goes when it is
 * found (PrimordialBeast#claims). A Heavenly Venerable tells the way to each ({@link #directions}).
 */
public final class PrimordialBeasts {
	/** A beast comes to its lair when someone comes this near it (sideways); those this near hear of it. */
	public static final double APPEAR = 160, HEARD = 200;
	/** Slain, the next comes this long after (ticks: five in-game days, the Gate's turn). */
	public static final long RETURN_TICKS = 5L * 24000L;
	/** Its lair this far from the Southern Heavenly Gate, at the least and the most. */
	public static final int LAIR_MIN = 1000, LAIR_MAX = 2000;
	/** Unseen this long with someone near its lair, it is taken as gone. */
	static final long LOST_TICKS = 6000;
	/** A lair keeps this far from any city's site or immortal sect's (a beast ranges no further than its leash and chase from it: PrimordialBeast). */
	static final double SETTLEMENT_KEEP = 320;
	static final int LAIR_TRIES = 96;
	/** The Phoenix's peak must stand this far over the clouds' top; it circles this high over it; the Dragon this high over its range. */
	static final int PEAK_OVER_CLOUDS = 60, PHOENIX_OVER = 20, DRAGON_OVER = 64;
	/** The Kun's lair this far under the clouds' top (its body's foot: its back just breaking them, PrimordialBeast#KUN_SHALLOW). */
	static final int KUN_UNDER = 9;

	public static final BeastKind[] KINDS = {BeastKind.KUN_PENG, BeastKind.TRUE_PHOENIX, BeastKind.TRUE_DRAGON};

	public static int index(BeastKind kind) {
		for (int i = 0; i < KINDS.length; i++) if (KINDS[i] == kind) return i;
		return -1;
	}

	/** Every two seconds while anyone is in the Immortal Realm: each beast come to its lair when someone nears it, if it is due. */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 40 != 13) return;
		ServerLevel level = server.getLevel(ModDimensions.IMMORTAL_REALM);
		if (level == null || level.players().isEmpty() || level.getDifficulty() == Difficulty.PEACEFUL) return;
		try {
			Data data = Data.get(server);
			long now = level.getGameTime();
			for (int k = 0; k < KINDS.length; k++) {
				BlockPos lair = lair(level, data, k);
				Entity known = data.alive[k] == null ? null : level.getEntity(data.alive[k]);
				if (known instanceof PrimordialBeast) {
					data.seen[k] = now;
					continue;
				}
				if (!someoneNear(level, lair)) continue;
				if (data.alive[k] != null) {
					if (now - data.seen[k] < LOST_TICKS) continue; // its land loading yet, or it is still to be found
					data.alive[k] = null;
					data.setDirty();
				}
				if (now < data.returnsAt[k] || !level.isPositionEntityTicking(lair)) continue;
				come(level, data, k, lair, now);
			}
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Primordial beasts tick failed", e);
		}
	}

	private static boolean someoneNear(ServerLevel level, BlockPos lair) {
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) continue;
			double dx = player.getX() - lair.getX(), dz = player.getZ() - lair.getZ();
			if (dx * dx + dz * dz <= APPEAR * APPEAR) return true;
		}
		return false;
	}

	/** The beast of kind {@code k} come to its lair, the realm's own. */
	static PrimordialBeast come(ServerLevel level, Data data, int k, BlockPos lair, long now) {
		if (!(ModEntities.beast(KINDS[k]).create(level) instanceof PrimordialBeast beast)) return null;
		beast.moveTo(lair.getX() + 0.5, lair.getY(), lair.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
		beast.finalizeSpawn(level, level.getCurrentDifficultyAt(lair), MobSpawnType.EVENT, null, null);
		beast.settle(lair);
		level.addFreshEntity(beast);
		data.alive[k] = beast.getUUID();
		data.seen[k] = now;
		data.setDirty();
		Component line = Component.translatable(ModLang.primordialComes(KINDS[k].getId())).withStyle(ChatFormatting.GOLD);
		for (ServerPlayer player : level.players()) {
			double dx = player.getX() - lair.getX(), dz = player.getZ() - lair.getZ();
			if (dx * dx + dz * dz <= HEARD * HEARD) player.displayClientMessage(line, false);
		}
		DefyingTheHeavens.LOGGER.info("The {} comes to its lair at {}", KINDS[k].getId(), lair.toShortString());
		return beast;
	}

	/** Whether {@code beast} is still the one of its kind the realm has (a copy left behind when its kind came again is not). */
	static boolean claims(PrimordialBeast beast) {
		MinecraftServer server = beast.getServer();
		int k = index(beast.kind());
		if (server == null || k < 0) return true;
		return beast.getUUID().equals(Data.get(server).alive[k]);
	}

	/** {@code beast} slain: if it was the realm's own, the next of its kind comes {@link #RETURN_TICKS} from now. */
	static void slain(PrimordialBeast beast) {
		MinecraftServer server = beast.getServer();
		int k = index(beast.kind());
		if (server == null || k < 0 || !beast.isOfTheRealm()) return;
		Data data = Data.get(server);
		if (!beast.getUUID().equals(data.alive[k])) return;
		data.alive[k] = null;
		data.returnsAt[k] = beast.level().getGameTime() + RETURN_TICKS;
		data.setDirty();
	}

	// --- Their lairs ---

	/** The lair of kind {@code k}, found the first time it is asked for and kept. */
	static BlockPos lair(ServerLevel level, Data data, int k) {
		if (data.lairs[k] == null) {
			long start = System.currentTimeMillis();
			data.lairs[k] = findLair(level, KINDS[k]);
			data.setDirty();
			DefyingTheHeavens.LOGGER.info("The {}'s lair at {} ({} ms)", KINDS[k].getId(), data.lairs[k].toShortString(), System.currentTimeMillis() - start);
		}
		return data.lairs[k];
	}

	/** The lair of {@code kind} in the Immortal Realm {@code level}: its own way round from the Gate, its own kind of place, clear of people. */
	public static BlockPos lairOf(ServerLevel level, BeastKind kind) {
		int k = index(kind);
		return k < 0 ? null : lair(level, Data.get(level.getServer()), k);
	}

	/**
	 * {@code kind}'s lair, from the world's seed: tries some {@link #LAIR_MIN} to {@link #LAIR_MAX} from the Gate, its own third of the way
	 * round (the three's directions turned by the seed), the first clear of people that is its kind's place ({@link #kunLair},
	 * {@link #phoenixLair}, {@link #dragonLair}); failing all, the same tries again for a place somewhat like it (a lower peak, a narrower
	 * sea: {@code relaxed}); failing those (its third all open sea, say), such a place anywhere round the Gate; failing those, the last tried
	 * clear of people, as its kind would keep there.
	 */
	static BlockPos findLair(ServerLevel level, BeastKind kind) {
		return findLair(level.getChunkSource().randomState(), level.getChunkSource().getGenerator(), level, level.getSeed(), kind);
	}

	/** {@link #findLair(ServerLevel, BeastKind)} by the realm's own land ({@code state}, {@code generator}, {@code heights}) and its seed. */
	static BlockPos findLair(RandomState state, ChunkGenerator generator, LevelHeightAccessor level, long seed, BeastKind kind) {
		double base = new java.util.Random(seed ^ 0x5DEECE66DL).nextDouble() * Math.PI * 2;
		int k = index(kind);
		int x = 0, z = 0, tried = 0, clear = 0;
		for (int pass = 0; pass < 3; pass++) {
			boolean relaxed = pass > 0, anywhere = pass == 2;
			java.util.Random random = new java.util.Random(seed * 31 + kind.ordinal() * 0x9E3779B9L + pass);
			for (int t = 0; t < LAIR_TRIES; t++) {
				double angle = anywhere ? random.nextDouble() * Math.PI * 2 : base + k * Math.PI * 2 / 3 + (random.nextDouble() - 0.5) * (0.8 + t * 0.03);
				double d = LAIR_MIN + random.nextDouble() * (LAIR_MAX - LAIR_MIN);
				int tx = (int) Math.round(ImmortalGate.HEAVEN_X + Math.cos(angle) * d), tz = (int) Math.round(ImmortalGate.HEAVEN_Z + Math.sin(angle) * d);
				tried++;
				if (!clearOfPeople(state, tx, tz)) continue;
				x = tx;
				z = tz;
				clear++;
				BlockPos lair = switch (kind) {
					case KUN_PENG -> kunLair(state, x, z, relaxed);
					case TRUE_PHOENIX -> phoenixLair(level, state, generator, x, z, relaxed);
					default -> dragonLair(level, state, generator, x, z, relaxed);
				};
				if (lair != null) return lair;
			}
		}
		DefyingTheHeavens.LOGGER.warn("No lair like its own for the {} ({} tried, {} clear of people): it keeps at {}, {}", kind.getId(), tried, clear, x, z);
		return kind == BeastKind.KUN_PENG ? new BlockPos(x, ModDimensions.IMMORTAL_CLOUD_Y - KUN_UNDER, z)
				: new BlockPos(x, Math.max(height(level, state, generator, x, z), ModDimensions.IMMORTAL_CLOUD_Y) + DRAGON_OVER, z);
	}

	/**
	 * The Kun's: over the open sea of clouds, no land within some two hundred blocks (the massifs' mask under the sea's there and round it);
	 * {@code relaxed}, within some hundred and forty.
	 */
	static BlockPos kunLair(RandomState state, int x, int z, boolean relaxed) {
		if (ModNoiseSettings.massifMask(state, x, z) > (relaxed ? -0.1 : -0.2)) return null;
		for (int ring = 1; ring <= (relaxed ? 2 : 3); ring++) {
			double reach = 70 * ring;
			for (int i = 0; i < 8 * ring; i++) {
				double a = Math.PI * 2 * i / (8 * ring);
				if (ModNoiseSettings.massifMask(state, (int) Math.round(x + Math.cos(a) * reach), (int) Math.round(z + Math.sin(a) * reach)) > (relaxed ? 0 : -0.1)) return null;
			}
		}
		return new BlockPos(x, ModDimensions.IMMORTAL_CLOUD_Y - KUN_UNDER, z);
	}

	/**
	 * The Phoenix's: over the highest column within some eighty blocks of a spot well inside a massif, standing well out of the clouds;
	 * {@code relaxed}, the highest within eighty of any spot (a massif's, or one of the jade pinnacles standing alone out of the sea of clouds:
	 * the massifs are islands, and most have a city), not so far out.
	 */
	static BlockPos phoenixLair(LevelHeightAccessor level, RandomState state, ChunkGenerator generator, int x, int z, boolean relaxed) {
		if (!relaxed && ModNoiseSettings.massifMask(state, x, z) < 0.3) return null;
		int[] peak = highest(level, state, generator, x, z, 80, relaxed ? 10 : 8);
		if (peak[1] < ModDimensions.IMMORTAL_CLOUD_Y + (relaxed ? PEAK_OVER_CLOUDS / 2 : PEAK_OVER_CLOUDS)) return null;
		return new BlockPos(peak[0], peak[1] + PHOENIX_OVER, peak[2]);
	}

	/**
	 * The Dragon's: high over a massif (its mask above 0 over land), over the highest of it about the spot; {@code relaxed}, high over
	 * whatever stands out of the clouds within eighty blocks (a massif's shore, a jade pinnacle).
	 */
	static BlockPos dragonLair(LevelHeightAccessor level, RandomState state, ChunkGenerator generator, int x, int z, boolean relaxed) {
		if (!relaxed && ModNoiseSettings.massifMask(state, x, z) < 0.1) return null;
		int[] peak = highest(level, state, generator, x, z, relaxed ? 80 : 40, 20);
		if (peak[1] < ModDimensions.IMMORTAL_CLOUD_Y + 20) return null;
		return new BlockPos(x, peak[1] + DRAGON_OVER, z);
	}

	/** The highest column within {@code reach} of (x, z), on a grid {@code step} apart: {x, its height, z}. */
	static int[] highest(LevelHeightAccessor level, RandomState state, ChunkGenerator generator, int x, int z, int reach, int step) {
		int bestY = Integer.MIN_VALUE, bx = x, bz = z;
		for (int dx = -reach; dx <= reach; dx += step) {
			for (int dz = -reach; dz <= reach; dz += step) {
				if (dx * dx + dz * dz > reach * reach) continue;
				int y = height(level, state, generator, x + dx, z + dz);
				if (y > bestY) {
					bestY = y;
					bx = x + dx;
					bz = z + dz;
				}
			}
		}
		return new int[] {bx, bestY, bz};
	}

	/** The land's height at column (x, z), as the generator makes it (loaded or not). */
	static int height(LevelHeightAccessor level, RandomState state, ChunkGenerator generator, int x, int z) {
		return generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, state);
	}

	/** Whether column (x, z) lies clear of the main island, every city's site and every immortal sect's by {@link #SETTLEMENT_KEEP}. */
	static boolean clearOfPeople(RandomState state, int x, int z) {
		if (Math.hypot(x - ModNoiseSettings.MAIN_X, z - ModNoiseSettings.MAIN_Z) < ModNoiseSettings.MAIN_OUTER + SETTLEMENT_KEEP) return false;
		int r = (int) Math.ceil(SETTLEMENT_KEEP);
		for (int cx = Math.floorDiv(x - r, ModNoiseSettings.CITY_CELL); cx <= Math.floorDiv(x + r, ModNoiseSettings.CITY_CELL); cx++) {
			for (int cz = Math.floorDiv(z - r, ModNoiseSettings.CITY_CELL); cz <= Math.floorDiv(z + r, ModNoiseSettings.CITY_CELL); cz++) {
				ModNoiseSettings.CitySite city = ModNoiseSettings.citySite(state, cx, cz);
				if (city != null && Math.hypot(city.x() - x, city.z() - z) < SETTLEMENT_KEEP) return false;
			}
		}
		for (int cx = Math.floorDiv(x - r, ModNoiseSettings.SECT_CELL); cx <= Math.floorDiv(x + r, ModNoiseSettings.SECT_CELL); cx++) {
			for (int cz = Math.floorDiv(z - r, ModNoiseSettings.SECT_CELL); cz <= Math.floorDiv(z + r, ModNoiseSettings.SECT_CELL); cz++) {
				ModNoiseSettings.CitySite sect = ModNoiseSettings.sectSite(state, cx, cz);
				if (sect != null && Math.hypot(sect.x() - x, sect.z() - z) < SETTLEMENT_KEEP) return false;
			}
		}
		return true;
	}

	// --- What the Venerables tell ---

	/**
	 * What a Heavenly Venerable tells of the three (TalkSystem's topic of them): for each, which way from the Southern Heavenly Gate it
	 * keeps, how far, and its spot; or that it was slain, and in how many days the next comes.
	 */
	public static List<Component> directions(ServerLevel level) {
		Data data = Data.get(level.getServer());
		BlockPos[] lairs = new BlockPos[KINDS.length];
		for (int k = 0; k < KINDS.length; k++) lairs[k] = lair(level, data, k);
		return directions(data, lairs, level.getGameTime());
	}

	/** {@link #directions(ServerLevel)} from the beasts' {@code lairs} and {@code data}, at game time {@code now}. */
	static List<Component> directions(Data data, BlockPos[] lairs, long now) {
		List<Component> lines = new ArrayList<>();
		BlockPos gate = new BlockPos(ImmortalGate.HEAVEN_X, 0, ImmortalGate.HEAVEN_Z);
		for (int k = 0; k < KINDS.length; k++) {
			BlockPos lair = lairs[k];
			String id = KINDS[k].getId();
			if (data.alive[k] == null && now < data.returnsAt[k]) {
				int days = (int) Math.max(1, Math.ceil((data.returnsAt[k] - now) / 24000.0));
				lines.add(Component.translatable(ModLang.primordialGone(id), days));
				continue;
			}
			int distance = (int) Math.round(Math.hypot(lair.getX() - gate.getX(), lair.getZ() - gate.getZ()) / 50.0) * 50;
			Component way = Component.translatable(ModLang.talkDirection(TalkSystem.direction(gate, lair)));
			lines.add(Component.translatable(ModLang.primordialWhere(id), way, distance, lair.getX(), lair.getZ()));
		}
		return lines;
	}

	// --- Saved ---

	/** Each beast's lair, the realm's one of it while it lives, when it was last seen, and when the next comes after one was slain. */
	public static final class Data extends SavedData {
		private static final String NAME = DefyingTheHeavens.MOD_ID + "_primordial_beasts";
		final BlockPos[] lairs = new BlockPos[KINDS.length];
		final UUID[] alive = new UUID[KINDS.length];
		final long[] seen = new long[KINDS.length], returnsAt = new long[KINDS.length];

		public static Data get(MinecraftServer server) {
			return server.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, NAME);
		}

		static Data load(CompoundTag tag) {
			Data data = new Data();
			for (int k = 0; k < KINDS.length; k++) {
				CompoundTag one = tag.getCompound(KINDS[k].getId());
				if (one.contains("Lair")) data.lairs[k] = NbtUtils.readBlockPos(one.getCompound("Lair"));
				if (one.hasUUID("Alive")) data.alive[k] = one.getUUID("Alive");
				data.seen[k] = one.getLong("Seen");
				data.returnsAt[k] = one.getLong("ReturnsAt");
			}
			return data;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			for (int k = 0; k < KINDS.length; k++) {
				CompoundTag one = new CompoundTag();
				if (lairs[k] != null) one.put("Lair", NbtUtils.writeBlockPos(lairs[k]));
				if (alive[k] != null) one.putUUID("Alive", alive[k]);
				one.putLong("Seen", seen[k]);
				one.putLong("ReturnsAt", returnsAt[k]);
				tag.put(KINDS[k].getId(), one);
			}
			return tag;
		}

		/** The realm's one of {@code kind} while it lives, or null. */
		public UUID alive(BeastKind kind) { return alive[index(kind)]; }

		/** When the next of {@code kind} comes (game time), after one was slain. */
		public long returnsAt(BeastKind kind) { return returnsAt[index(kind)]; }
	}

	private PrimordialBeasts() {}
}
