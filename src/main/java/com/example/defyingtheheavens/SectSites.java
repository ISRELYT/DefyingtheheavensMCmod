package com.example.defyingtheheavens;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Sects that come into being without anyone going near them. A sect is founded the first time its Formation Core ticks
 * (SectManager#found), which used to wait for a player to come by. This asks the world generator where sects stand (as
 * /locate does: nothing is generated to find out) around the places that matter, and loads each one's heart for a moment
 * (NpcLedger#wake) so that it founds itself: its grounds generated, its members born, and its life going on from then on,
 * unseen, like any other sect's (NpcLedger).
 * <p>
 * Where: in the Overworld, within {@link #SPAWN_REACH} of the world spawn and {@link #PLAYER_REACH} of every player there; in
 * the Upper Realm, within {@link #RIFT_REACH} of the rift (where cultivators from below arrive and look for a sect to join)
 * and {@link #PLAYER_REACH} of every player there. One site at a time, nearest first, taking each place in turn; the
 * generator is asked off the server thread. Each site is decided once (a sect founded, or none there) and remembered.
 * Nothing happens in a world made without structures. Saved with the Overworld.
 */
public class SectSites extends SavedData {
	private static final String NAME = DefyingTheHeavens.MOD_ID + "_sect_sites";
	public static final int SPAWN_REACH = 2048;
	public static final int RIFT_REACH = 2048;
	public static final int PLAYER_REACH = 1024;
	/** Ticks between looks; how long a woken site may take to found its sect. */
	private static final int STEP = 10;
	private static final long GIVE_UP = 1200;
	/** A site's heart lived in longer than this (ticks of a player nearby: LevelChunk#getInhabitedTime) with no sect: none is coming. */
	private static final long LIVED_IN = 600;
	/**
	 * A sect founded this close (sideways) to a site's heart is that site's: its core stands there (its start chunk's middle,
	 * or in the Upper Realm up to 48 blocks off it each way, SectStructure#ISLAND_SEARCH); no two sects' cores are nearer
	 * than 160.
	 */
	private static final double FOUNDED_NEAR = 64;

	/** Per dimension, the placement regions decided (as ChunkPos.asLong(region x, region z)): the sects' sites, and the demon clans'. */
	private final Map<ResourceKey<Level>, Set<Long>> decided = new HashMap<>(), decidedClans = new HashMap<>();

	/**
	 * The site the generator is being asked about, off the server thread: the answer means sampling the ground across the
	 * grounds, 10-100 ms of work that would otherwise stall a tick. One at a time (not saved: asked again after a restart).
	 */
	private static Asking asking;
	/** The site being founded now, and its heart, where its core stands (not saved: after a restart it is looked at again). */
	private static ResourceKey<Level> pendingDimension;
	private static BlockPos pendingHeart;
	private static long pendingRegion;
	private static boolean pendingClan;
	private static long pendingSince;
	/** Which place to start from at the next look (each in turn). */
	private static int turn;

	/** {@code clan}: a demon clan's dens' sites (DenStructure), else the sects'. */
	private record Place(ServerLevel level, Holder<Structure> structure, RandomSpreadStructurePlacement placement, BlockPos anchor, int reach, boolean clan) {}

	/** The answer: where the sect's heart would be, or empty for no sect. */
	private record Asking(ResourceKey<Level> dimension, long region, ChunkPos chunk, CompletableFuture<Optional<BlockPos>> answer, boolean clan) {}

	public static SectSites get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(SectSites::load, SectSites::new, NAME);
	}

	public static void clear() {
		asking = null;
		pendingDimension = null;
		pendingHeart = null;
		turn = 0;
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % STEP != 7 || !server.getWorldData().worldGenOptions().generateStructures()) return;
		SectSites data = get(server);
		if (pendingHeart != null && !follow(server, data)) return;
		if (asking != null) {
			if (!asking.answer().isDone()) return;
			Asking answered = asking;
			asking = null;
			ServerLevel level = server.getLevel(answered.dimension());
			if (level != null && heard(data, level, answered)) return;
		}
		List<Place> places = places(server);
		for (int i = 0; i < places.size(); i++) {
			Place place = places.get((turn + i) % places.size());
			long[] next;
			while ((next = nearestUndecided(data, place)) != null) {
				if (ask(data, place, next[0], new ChunkPos(next[1]))) {
					turn = (turn + i + 1) % places.size(); // the next place's turn next
					return;
				}
			}
		}
	}

	/**
	 * Looks at region {@code region}'s site at {@code chunk}: decided at once if no sect can stand there (another structure's
	 * ground, as villages keep sects off theirs) or one already does; otherwise the generator is asked. @return whether it was
	 */
	private static boolean ask(SectSites data, Place place, long region, ChunkPos chunk) {
		ServerLevel level = place.level();
		ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
		if (!place.placement().isStructureChunk(state, chunk.x, chunk.z) || foundedNear(level, chunk.getMiddleBlockPosition(0))) {
			data.decide(level.dimension(), region, place.clan());
			return false;
		}
		// What worldgen does to decide whether to start a sect in that chunk (and /locate to find one), short of generating it.
		Structure structure = place.structure().value();
		ServerChunkCache chunks = level.getChunkSource();
		Structure.GenerationContext context = new Structure.GenerationContext(level.registryAccess(), chunks.getGenerator(),
				chunks.getGenerator().getBiomeSource(), chunks.randomState(), level.getStructureManager(), state.getLevelSeed(), chunk, level,
				structure.biomes()::contains);
		asking = new Asking(level.dimension(), region, chunk,
				CompletableFuture.supplyAsync(() -> structure.findValidGenerationPoint(context).map(Structure.GenerationStub::position),
						Util.backgroundExecutor()), place.clan());
		return true;
	}

	/** The generator's answer for a site: none there (decided), or a sect, whose heart is woken. @return whether it was woken */
	private static boolean heard(SectSites data, ServerLevel level, Asking answered) {
		Optional<BlockPos> heart;
		try {
			heart = answered.answer().join();
		} catch (RuntimeException e) {
			DefyingTheHeavens.LOGGER.warn("Couldn't tell whether a sect stands at {} in {}", answered.chunk(), answered.dimension().location(), e);
			heart = Optional.empty();
		}
		if (heart.isEmpty() || foundedNear(level, heart.get())) {
			data.decide(level.dimension(), answered.region(), answered.clan());
			return false;
		}
		// Its land made and lived in a while already (players were by it: its inhabited time) with no sect there: none will come of it
		// (the land came before the sect could, or the generator put none there after all). Decided at once, where before the sweep of
		// 2026-10-23 it was woken and waited on to the end (GIVE_UP), the next site waiting behind it.
		net.minecraft.world.level.chunk.LevelChunk made = level.getChunkSource().getChunkNow(heart.get().getX() >> 4, heart.get().getZ() >> 4);
		if (made != null && made.getInhabitedTime() > LIVED_IN) {
			data.decide(level.dimension(), answered.region(), answered.clan());
			return false;
		}
		pendingDimension = level.dimension();
		pendingHeart = heart.get();
		pendingRegion = answered.region();
		pendingClan = answered.clan();
		pendingSince = level.getServer().overworld().getGameTime();
		NpcLedger.wake(level, pendingHeart);
		return true;
	}

	/** The site being founded: done (its sect stands, or it never will), or woken again to give its core time. @return done */
	private static boolean follow(MinecraftServer server, SectSites data) {
		ServerLevel level = server.getLevel(pendingDimension);
		long now = server.overworld().getGameTime();
		boolean founded = level != null && foundedNear(level, pendingHeart);
		if (level == null || founded || now - pendingSince > GIVE_UP) {
			if (level != null) data.decide(level.dimension(), pendingRegion, pendingClan);
			if (level != null && !founded) DefyingTheHeavens.LOGGER.info("No sect came of the site at {} in {}", pendingHeart.toShortString(), pendingDimension.location());
			pendingHeart = null;
			pendingDimension = null;
			return true;
		}
		NpcLedger.wake(level, pendingHeart);
		return false;
	}

	private static long[] nearestUndecided(SectSites data, Place place) {
		return nearestUndecided((place.clan() ? data.decidedClans : data.decided).getOrDefault(place.level().dimension(), Set.of()), place.placement(),
				place.level().getChunkSource().getGeneratorState().getLevelSeed(), place.anchor(), place.reach());
	}

	/**
	 * The nearest region round {@code anchor} whose site lies within {@code reach} and that isn't in {@code done}: {region key,
	 * site chunk}, or null if none is left.
	 */
	static long[] nearestUndecided(Set<Long> done, RandomSpreadStructurePlacement placement, long seed, BlockPos anchor, int reach) {
		int spacing = placement.spacing();
		int rx0 = Math.floorDiv(anchor.getX() >> 4, spacing), rz0 = Math.floorDiv(anchor.getZ() >> 4, spacing);
		int span = reach / 16 / spacing + 1;
		long[] best = null;
		double bestDistance = (double) reach * reach;
		for (int rx = rx0 - span; rx <= rx0 + span; rx++) {
			for (int rz = rz0 - span; rz <= rz0 + span; rz++) {
				long region = ChunkPos.asLong(rx, rz);
				if (done.contains(region)) continue;
				ChunkPos chunk = placement.getPotentialStructureChunk(seed, rx * spacing, rz * spacing);
				double dx = chunk.getMiddleBlockX() - anchor.getX(), dz = chunk.getMiddleBlockZ() - anchor.getZ();
				double distance = dx * dx + dz * dz;
				if (distance > bestDistance) continue;
				bestDistance = distance;
				best = new long[] {region, chunk.toLong()};
			}
		}
		return best;
	}

	/**
	 * Whether a sect may yet be founded within {@code reach} of {@code pos}, sideways: a site there not decided yet (still to
	 * be looked at, or being founded). Rogues and hermits wait for it to be settled rather than turn up on grounds still to
	 * be founded: knowing that used to mean asking the generator on the server thread (~80 ms a site). A player's surroundings
	 * are settled within seconds of their coming (PLAYER_REACH).
	 */
	public static boolean unsettled(ServerLevel level, BlockPos pos, int reach) {
		if (!level.getServer().getWorldData().worldGenOptions().generateStructures()) return false;
		for (boolean clan : new boolean[] {false, true}) {
			Place place = place(level, pos, reach + SITE_DRIFT, clan);
			if (place != null && nearestUndecided(get(level.getServer()), place) != null) return true;
		}
		return false;
	}

	/** How far a sect's heart may stand from its start chunk's middle (SectStructure#ISLAND_SEARCH, either way). */
	private static final int SITE_DRIFT = 70;

	/** The places looked round, players first: each player's surroundings, then the world spawn, then the Upper Realm's rift. */
	private static List<Place> places(MinecraftServer server) {
		List<Place> places = new ArrayList<>();
		ServerLevel overworld = server.overworld();
		ServerLevel upper = server.getLevel(ModDimensions.UPPER_REALM);
		for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.isSpectator()) continue;
			ServerLevel level = player.serverLevel();
			if (level != overworld && level != upper) continue;
			for (boolean clan : new boolean[] {false, true}) add(places, place(level, player.blockPosition(), PLAYER_REACH, clan));
		}
		for (boolean clan : new boolean[] {false, true}) {
			add(places, place(overworld, overworld.getSharedSpawnPos(), SPAWN_REACH, clan));
			if (upper != null) add(places, place(upper, new BlockPos(SpatialRiftBlock.RIFT_X, 0, SpatialRiftBlock.RIFT_Z), RIFT_REACH, clan));
		}
		return places;
	}

	private static void add(List<Place> places, Place place) {
		if (place != null) places.add(place);
	}

	/** Round {@code anchor} in {@code level}, the sites of its sects (or, {@code clan}, its demon clans): null in a dimension without them. */
	private static Place place(ServerLevel level, BlockPos anchor, int reach, boolean clan) {
		ResourceKey<Structure> key = level.dimension() == Level.OVERWORLD ? (clan ? ModStructures.DEMON_CLAN : ModStructures.SECT)
				: ModDimensions.isUpperRealm(level.dimension()) ? (clan ? ModStructures.DEMON_CLAN_UPPER_REALM : ModStructures.SECT_UPPER_REALM) : null;
		if (key == null) return null;
		Holder<Structure> structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(key).orElse(null);
		if (structure == null) return null;
		for (StructurePlacement placement : level.getChunkSource().getGeneratorState().getPlacementsForStructure(structure)) {
			if (placement instanceof RandomSpreadStructurePlacement spread) return new Place(level, structure, spread, anchor, reach, clan);
		}
		return null;
	}

	/** Whether a sect has been founded with its core within {@link #FOUNDED_NEAR} of {@code pos}, sideways. */
	private static boolean foundedNear(ServerLevel level, BlockPos pos) {
		for (Sect sect : SectData.get(level.getServer()).all()) {
			if (sect.dimension != level.dimension()) continue;
			double dx = sect.core.getX() - pos.getX(), dz = sect.core.getZ() - pos.getZ();
			if (dx * dx + dz * dz <= FOUNDED_NEAR * FOUNDED_NEAR) return true;
		}
		return false;
	}

	void decide(ResourceKey<Level> dimension, long region) {
		decide(dimension, region, false);
	}

	void decide(ResourceKey<Level> dimension, long region, boolean clan) {
		(clan ? decidedClans : decided).computeIfAbsent(dimension, d -> new HashSet<>()).add(region);
		setDirty();
	}

	boolean isDecided(ResourceKey<Level> dimension, long region) {
		return decided.getOrDefault(dimension, Set.of()).contains(region);
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		tag.put("Decided", saveDecided(decided));
		tag.put("DecidedClans", saveDecided(decidedClans));
		return tag;
	}

	private static ListTag saveDecided(Map<ResourceKey<Level>, Set<Long>> decided) {
		ListTag list = new ListTag();
		for (Map.Entry<ResourceKey<Level>, Set<Long>> e : decided.entrySet()) {
			CompoundTag d = new CompoundTag();
			d.putString("Dimension", e.getKey().location().toString());
			d.putLongArray("Regions", e.getValue().stream().mapToLong(Long::longValue).toArray());
			list.add(d);
		}
		return list;
	}

	public static SectSites load(CompoundTag tag) {
		SectSites data = new SectSites();
		loadDecided(tag.getList("Decided", Tag.TAG_COMPOUND), data.decided);
		loadDecided(tag.getList("DecidedClans", Tag.TAG_COMPOUND), data.decidedClans);
		return data;
	}

	private static void loadDecided(ListTag list, Map<ResourceKey<Level>, Set<Long>> decided) {
		for (int i = 0; i < list.size(); i++) {
			CompoundTag d = list.getCompound(i);
			ResourceLocation dimension = ResourceLocation.tryParse(d.getString("Dimension"));
			if (dimension == null) continue;
			Set<Long> regions = new HashSet<>();
			for (long region : d.getLongArray("Regions")) regions.add(region);
			decided.put(ResourceKey.create(Registries.DIMENSION, dimension), regions);
		}
	}
}
