package com.example.defyingtheheavens;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Immortal Realm's markets (phase 2 of the realm plan, 2026-10-25: the user's "Immortal jade + cities"; the plan's "markets sell pills,
 * treasures and True Immortal slips for jade"): merchants keep the stalls round the market squares, {@link #CAPITAL_MERCHANTS} of the Jade
 * Capital's twelve (ImmortalCapital) and {@link #CITY_MERCHANTS} of every other city's eight (ImmortalCityStructure). They are True
 * Immortals of the neutral path (NpcTitle#IMMORTAL_MERCHANT) who sell for Immortal Jade (CultivatorTrades#immortalGoods) and buy what a
 * cultivator has use for. A market is peopled the first time a player comes within {@link #NEAR} with its square loaded, all its
 * merchants at once; one struck down is made up {@link #REPLACE_EVERY} later. Each keeps to its stall (CultivatorNpc#keepsPost): drawn
 * off it, it walks back, and from far it is set back on it. The Immortal Realm's census holds them as any of its True Immortals (HighRealms).
 */
public final class CityMarkets {
	/** How many keep the capital's stalls (every other of its twelve), and another city's (every other of its eight). */
	public static final int CAPITAL_MERCHANTS = 6, CITY_MERCHANTS = 4;
	/** A player this near a market's square (sideways) has it peopled. */
	public static final int NEAR = 96;
	/** One struck down is made up this long after (ticks: an in-game hour). */
	static final long REPLACE_EVERY = 1000;
	/** Drawn this far off its stall a merchant walks back; this far, it is set back on it. */
	static final double STRAY = 2.0, FAR = 16;
	/** A city's stalls: in a ring this far round its square's middle, every 45 degrees from 22.5 (ImmortalCityStructure#plan). */
	private static final int CITY_RING = 11;

	/** A market: its square's middle, its stalls' floors where its merchants stand (those loaded), and how many it keeps. */
	public record Market(BlockPos square, List<BlockPos> stalls, int keeps) {}

	/** Whether {@code npc} keeps a stall in one of the Immortal Realm's markets. */
	public static boolean isMerchant(CultivatorNpc npc) {
		return npc.getTitle() == NpcTitle.IMMORTAL_MERCHANT && npc.settledAt() != null && ModDimensions.isImmortalRealm(npc.level().dimension());
	}

	/** Every two seconds: each market near a player kept peopled, and its merchants kept to their stalls. */
	public static void keep(MinecraftServer server) {
		if (server.getTickCount() % 40 != 21) return;
		ServerLevel level = server.getLevel(ModDimensions.IMMORTAL_REALM);
		if (level == null || level.players().isEmpty()) return;
		Data data = Data.get(level);
		Set<BlockPos> kept = new HashSet<>();
		for (ServerPlayer player : level.players()) {
			for (Market market : near(level, player.getBlockX(), player.getBlockZ())) {
				if (kept.add(market.square())) keep(level, data, market);
			}
		}
	}

	/** The markets whose squares lie within {@link #NEAR} of column (x, z) (sideways), and whose stalls are there to keep. */
	public static List<Market> near(ServerLevel level, int x, int z) {
		List<Market> markets = new ArrayList<>();
		if (within(new BlockPos(ModNoiseSettings.MAIN_X, 0, ModNoiseSettings.MAIN_Z), x, z)) {
			Market capital = capital(level);
			if (capital != null) markets.add(capital);
		}
		RandomState state = level.getChunkSource().randomState();
		int cx = Math.floorDiv(x, ModNoiseSettings.CITY_CELL), cz = Math.floorDiv(z, ModNoiseSettings.CITY_CELL);
		for (int i = cx - 1; i <= cx + 1; i++) {
			for (int j = cz - 1; j <= cz + 1; j++) {
				ModNoiseSettings.CitySite site = ModNoiseSettings.citySite(state, i, j);
				if (site == null || !within(new BlockPos(site.x(), 0, site.z()), x, z)) continue;
				Market city = city(level, site);
				if (city != null) markets.add(city);
			}
		}
		return markets;
	}

	private static boolean within(BlockPos square, int x, int z) {
		double dx = square.getX() - x, dz = square.getZ() - z;
		return dx * dx + dz * dz <= (double) NEAR * NEAR;
	}

	/** The Jade Capital's market: its plan's square, and every other of its stalls (null while its square isn't loaded). */
	static Market capital(ServerLevel level) {
		if (!level.isLoaded(new BlockPos(ModNoiseSettings.MAIN_X, 0, ModNoiseSettings.MAIN_Z))) return null;
		ImmortalCapital.Plan plan = ImmortalCapital.plan(level.getChunkSource().randomState(), level.getChunkSource().getGenerator(), level);
		BlockPos square = new BlockPos(ModNoiseSettings.MAIN_X, plan.squareY(), ModNoiseSettings.MAIN_Z);
		List<BlockPos> stalls = new ArrayList<>();
		int k = 0;
		for (SectPiece piece : plan.pieces()) {
			if (piece.kind() != SectPiece.Kind.CITY_STALL || k++ % 2 != 0) continue;
			BlockPos middle = piece.getBoundingBox().getCenter();
			if (!level.isLoaded(middle)) continue;
			BlockPos floor = floor(level, middle.getX(), middle.getZ(), piece.getBoundingBox().minY());
			if (floor != null) stalls.add(floor);
		}
		return stalls.isEmpty() ? null : new Market(square, stalls, CAPITAL_MERCHANTS);
	}

	/**
	 * Another city's market at {@code site}: its square's level as its plan reckoned it (the median of the land's height about its middle,
	 * ImmortalCityStructure#squareY), its stalls in their ring; null if its square isn't loaded or no city was built there (its land not
	 * one a city stands on).
	 */
	static Market city(ServerLevel level, ModNoiseSettings.CitySite site) {
		BlockPos middle = new BlockPos(site.x(), 0, site.z());
		if (!level.isLoaded(middle)) return null;
		int squareY = ImmortalCityStructure.squareY(CityPlanner.ground(level.getChunkSource().getGenerator(), level, level.getChunkSource().randomState()),
				site.x(), site.z());
		List<BlockPos> stalls = new ArrayList<>();
		for (int k = 0; k < 8; k += 2) {
			double angle = Math.toRadians(22.5 + 45 * k);
			int x = site.x() + (int) Math.round(Math.cos(angle) * CITY_RING), z = site.z() + (int) Math.round(Math.sin(angle) * CITY_RING);
			if (!level.isLoaded(new BlockPos(x, 0, z))) continue;
			BlockPos floor = floor(level, x, z, squareY);
			if (floor != null && level.structureManager().getStructureWithPieceAt(floor, ModStructures.IMMORTAL_CITY).isValid()) stalls.add(floor);
		}
		return stalls.isEmpty() ? null : new Market(new BlockPos(site.x(), squareY, site.z()), stalls, CITY_MERCHANTS);
	}

	/** Where one stands in a stall at column (x, z) built at {@code y}: room for feet and head over something solid, a block or so off it. */
	private static BlockPos floor(ServerLevel level, int x, int z, int y) {
		for (int at = y + 2; at >= y - 3; at--) {
			BlockPos feet = new BlockPos(x, at, z);
			if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir() && level.getBlockState(feet.below()).isSolid()) return feet;
		}
		return null;
	}

	/** {@code market}'s merchants kept: all at once the first time, then one an hour while a stall stands empty; each to its stall. */
	private static void keep(ServerLevel level, Data data, Market market) {
		if (!level.isPositionEntityTicking(market.square())) return;
		List<CultivatorNpc> merchants = merchants(level, market);
		for (CultivatorNpc merchant : merchants) toStall(merchant);
		if (merchants.size() >= market.stalls().size()) return;
		long key = ChunkPos.asLong(market.square().getX(), market.square().getZ());
		long now = level.getGameTime();
		Data.Entry entry = data.markets.computeIfAbsent(key, k -> new Data.Entry());
		if (!entry.peopled && market.stalls().size() < market.keeps()) return; // peopled all at once: its stalls all loaded first
		if (entry.peopled && now - entry.lastArrival < REPLACE_EVERY) return;
		Set<BlockPos> kept = new HashSet<>();
		for (CultivatorNpc merchant : merchants) kept.add(merchant.settledAt());
		RandomSource random = level.getRandom();
		for (BlockPos stall : market.stalls()) {
			if (kept.contains(stall)) continue;
			arrive(level, stall, market.square(), random);
			if (entry.peopled) break; // one an hour once the market has been peopled
		}
		entry.peopled = true;
		entry.lastArrival = now;
		data.setDirty();
	}

	/** The merchants keeping {@code market}'s stalls now (loaded). */
	public static List<CultivatorNpc> merchants(ServerLevel level, Market market) {
		Set<BlockPos> stalls = new HashSet<>(market.stalls());
		return level.getEntitiesOfClass(CultivatorNpc.class, new AABB(market.square()).inflate(NEAR, 48, NEAR),
				npc -> npc.isAlive() && isMerchant(npc) && stalls.contains(npc.settledAt()));
	}

	/** Back to its stall, if drawn off it and not fighting: walking, or set back on it from far. */
	private static void toStall(CultivatorNpc npc) {
		BlockPos stall = npc.settledAt();
		if (npc.getTarget() != null || npc.isBegging()) return;
		Vec3 at = Vec3.atBottomCenterOf(stall);
		double off = npc.position().distanceToSqr(at);
		if (off > FAR * FAR) {
			npc.getNavigation().stop();
			npc.moveTo(at.x, at.y, at.z, npc.getYRot(), 0);
		} else if (off > STRAY * STRAY && npc.getNavigation().isDone()) {
			npc.getNavigation().moveTo(at.x, at.y, at.z, 0.7);
		}
	}

	/** A merchant for {@code stall}: a True Immortal (Early or Mid) of the neutral path, facing the square's middle. */
	private static CultivatorNpc arrive(ServerLevel level, BlockPos stall, BlockPos square, RandomSource random) {
		CultivatorNpc npc = CultivatorNpc.createFor(level);
		if (npc == null) return null;
		float yaw = (float) (Mth.atan2(square.getZ() - stall.getZ(), square.getX() - stall.getX()) * Mth.RAD_TO_DEG) - 90;
		npc.moveTo(stall.getX() + 0.5, stall.getY(), stall.getZ() + 0.5, yaw, 0);
		npc.becomeRogue(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.byIndex(random.nextInt(2))), random.nextInt(61) - 30, random);
		npc.setTitle(NpcTitle.IMMORTAL_MERCHANT);
		npc.setSettledAt(stall);
		npc.setYHeadRot(yaw);
		level.addFreshEntity(npc);
		return npc;
	}

	/** Whether {@code market} has been peopled (the game tests). */
	static boolean peopled(ServerLevel level, Market market) {
		Data.Entry entry = Data.get(level).markets.get(ChunkPos.asLong(market.square().getX(), market.square().getZ()));
		return entry != null && entry.peopled;
	}

	/** Each market's keeping: peopled yet, and when its last merchant came; kept with the Immortal Realm's data. */
	static final class Data extends SavedData {
		private static final String NAME = DefyingTheHeavens.MOD_ID + "_city_markets";
		final Long2ObjectMap<Entry> markets = new Long2ObjectOpenHashMap<>();

		static final class Entry {
			boolean peopled;
			long lastArrival;
		}

		static Data get(ServerLevel level) {
			return level.getDataStorage().computeIfAbsent(Data::load, Data::new, NAME);
		}

		static Data load(CompoundTag tag) {
			Data data = new Data();
			ListTag list = tag.getList("Markets", Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				CompoundTag market = list.getCompound(i);
				Entry entry = new Entry();
				entry.peopled = market.getBoolean("Peopled");
				entry.lastArrival = market.getLong("LastArrival");
				data.markets.put(market.getLong("Square"), entry);
			}
			return data;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			ListTag list = new ListTag();
			for (Long2ObjectMap.Entry<Entry> e : markets.long2ObjectEntrySet()) {
				CompoundTag market = new CompoundTag();
				market.putLong("Square", e.getLongKey());
				market.putBoolean("Peopled", e.getValue().peopled);
				market.putLong("LastArrival", e.getValue().lastArrival);
				list.add(market);
			}
			tag.put("Markets", list);
			return tag;
		}
	}

	private CityMarkets() {}
}
