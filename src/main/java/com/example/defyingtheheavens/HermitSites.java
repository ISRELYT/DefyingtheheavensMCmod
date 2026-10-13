package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Heavenly Beings who were in seclusion before anyone came: in each region of {@link #REGION} x {@link #REGION} chunks of the
 * Overworld, perhaps one ({@link #CHANCE}), in a cave or high on a mountain in the chunk the world's seed picks for it. It is
 * placed the first time that chunk loads (worldgen itself can't raise a Concealment Barrier), so it is there as soon as anyone
 * could see the land, and from then on lives like any other secluded cultivator: cultivating, setting off to ascend at Grand
 * Perfection whether anyone is near or not (NpcLedger), and talked of in rumours. Each region is decided once and remembered,
 * a spot found or not.
 * <p>
 * When a region's hermit leaves (sets off to ascend, or dies), another takes its place {@link #REFILL} later, seated the
 * next time the land loads; like a sect's members, one killed by a player is never replaced. Saved with the Overworld.
 */
public class HermitSites extends SavedData {
	private static final String NAME = DefyingTheHeavens.MOD_ID + "_hermits";
	/** Region size in chunks (1,024 blocks), how many chunks from its edges the hermit's chunk keeps, and how often a region has one. */
	static final int REGION = 64;
	private static final int MARGIN = 8;
	static final float CHANCE = 0.8f;
	/** Below this a cave is the deep dark's, and above this (open sky) a mountain is high enough to seclude oneself on. */
	private static final int DEEPEST = 0;
	private static final int MOUNTAIN = 110;
	/** A hermit is only seated with no player this close to its chunk (sideways or not), so none appears before anyone's eyes. */
	private static final double UNSEEN = 80;

	/** A region left empty gets a new hermit this long (ticks: two weeks of game days) after its last one left. */
	static final long REFILL = 14 * HeavenRitual.DAY;

	private final Set<Long> decided = new HashSet<>();
	/** Each seated hermit's region, and the regions waiting for a new one (and from when). */
	private final Map<UUID, Long> hermits = new HashMap<>();
	private final Map<Long, Long> vacantUntil = new HashMap<>();
	/** Regions whose chunk has loaded, waiting for their neighbours to load too (not saved: they ask again on loading). */
	private static final Set<Long> PENDING = new HashSet<>();

	public static HermitSites get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(HermitSites::load, HermitSites::new, NAME);
	}

	/** The chunk the world's seed picks for region {@code rx, rz}'s hermit, or null if the region has none. */
	static ChunkPos siteChunk(long seed, int rx, int rz) {
		RandomSource random = RandomSource.create(seed ^ (rx * 341873128712L + rz * 132897987541L) ^ 0x5EC1D3DL);
		if (random.nextFloat() >= CHANCE) return null;
		return new ChunkPos(rx * REGION + MARGIN + random.nextInt(REGION - 2 * MARGIN), rz * REGION + MARGIN + random.nextInt(REGION - 2 * MARGIN));
	}

	/** A chunk loading: if it is its region's hermit chunk and the region is still undecided, the hermit is placed soon. */
	public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
		if (level.dimension() != Level.OVERWORLD) return;
		ChunkPos pos = chunk.getPos();
		int rx = Math.floorDiv(pos.x, REGION), rz = Math.floorDiv(pos.z, REGION);
		long key = ChunkPos.asLong(rx, rz);
		if (PENDING.contains(key) || !pos.equals(siteChunk(level.getSeed(), rx, rz))) return;
		if (!get(level.getServer()).decided.contains(key)) PENDING.add(key);
	}

	/**
	 * A cultivator leaving seclusion for good (setting off to ascend, or dying): if it was a region's hermit, the region gets
	 * a new one {@link #REFILL} from now, unless a player killed it.
	 */
	public static void onLeft(ServerLevel level, CultivatorNpc npc, boolean killedByPlayer) {
		HermitSites data = get(level.getServer());
		Long key = data.hermits.remove(npc.getUUID());
		if (key == null) return;
		if (!killedByPlayer) data.vacantUntil.put(key, level.getGameTime() + REFILL);
		data.setDirty();
	}

	/**
	 * Every second: regions whose wait for a new hermit is over become undecided again (seated as their chunk next loads, or
	 * now if it is loaded); pending regions whose chunk and its neighbours are all loaded get their hermit (or are decided
	 * without one).
	 */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 5) return;
		ServerLevel level = server.overworld();
		HermitSites data = get(server);
		reopenDue(level, level.getGameTime());
		for (Iterator<Long> it = PENDING.iterator(); it.hasNext(); ) {
			long key = it.next();
			ChunkPos chunk = siteChunk(level.getSeed(), ChunkPos.getX(key), ChunkPos.getZ(key));
			if (chunk == null || data.decided.contains(key)) {
				it.remove();
				continue;
			}
			if (!aroundLoaded(level, chunk)) continue;
			// Nobody close enough to see one appear: it waits (pending) until they have gone.
			if (level.getNearestPlayer(chunk.getMiddleBlockX(), 64, chunk.getMiddleBlockZ(), UNSEEN, false) != null) continue;
			// Nor while a sect may yet be founded near by (its site not looked at yet): it waits until that is settled.
			if (SectSites.unsettled(level, chunk.getMiddleBlockPosition(64), NpcSpawner.SECT_CLEARANCE + NpcSpawner.UNFOUNDED_GROUNDS)) continue;
			it.remove();
			data.decided.add(key);
			data.setDirty();
			// A fresh draw each time (the seed, the region and the hour), so a successor sits somewhere of its own.
			CultivatorNpc hermit = place(level, chunk, RandomSource.create(level.getSeed() ^ key ^ level.getGameTime()));
			if (hermit != null) data.hermits.put(hermit.getUUID(), key);
		}
	}

	/** Regions whose wait for a new hermit is over at {@code now}: undecided again, and pending at once if their chunk is loaded. */
	static void reopenDue(ServerLevel level, long now) {
		HermitSites data = get(level.getServer());
		for (Iterator<Map.Entry<Long, Long>> it = data.vacantUntil.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Long, Long> e = it.next();
			if (e.getValue() > now) continue;
			it.remove();
			data.decided.remove(e.getKey());
			data.setDirty();
			ChunkPos chunk = siteChunk(level.getSeed(), ChunkPos.getX(e.getKey()), ChunkPos.getZ(e.getKey()));
			if (chunk != null && level.hasChunk(chunk.x, chunk.z)) PENDING.add(e.getKey());
		}
	}

	/** Whether region {@code key} has been decided (has its hermit, or is known to have none) (for tests). */
	static boolean isDecided(MinecraftServer server, long key) {
		return get(server).decided.contains(key);
	}

	private static boolean aroundLoaded(ServerLevel level, ChunkPos chunk) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (!level.hasChunk(chunk.x + dx, chunk.z + dz)) return false;
			}
		}
		return true;
	}

	/** A Heavenly Being (Early, Mid or Late) seated in seclusion at the best spot around {@code chunk}, if there is one. */
	static CultivatorNpc place(ServerLevel level, ChunkPos chunk, RandomSource random) {
		// Hermits keep away from people: none where a sect's grounds are (founded or still to be), as rogues don't.
		if (NpcSpawner.nearSect(level, new BlockPos(chunk.getMiddleBlockX(), 64, chunk.getMiddleBlockZ()))) return null;
		BlockPos site = findSite(level, chunk, random);
		if (site == null) return null;
		CultivatorNpc npc = CultivatorNpc.createFor(level);
		if (npc == null) return null;
		npc.moveTo(site.getX() + 0.5, site.getY(), site.getZ() + 0.5, random.nextFloat() * 360, 0);
		float roll = random.nextFloat();
		Stage stage = roll < 0.5f ? Stage.EARLY : roll < 0.8f ? Stage.MID : Stage.LATE;
		npc.becomeRogue(PlayerCultivation.rank(Realm.HEAVENLY_BEING, stage), Alignment.randomRogue(random), random);
		level.addFreshEntity(npc);
		npc.enterSeclusion(level, site);
		DefyingTheHeavens.LOGGER.info("A Heavenly Being ({}) has long been in seclusion at {}", stage.name().toLowerCase(java.util.Locale.ROOT),
				site.toShortString());
		return npc;
	}

	/**
	 * The best place around {@code chunk} (it and its neighbours) to sit in seclusion: a cave floor, roofed and dark, above the
	 * deep dark and clear of lava; or else open ground high on a mountain, the higher the better. Never on a sect's grounds or
	 * inside a barrier. Null if there is neither.
	 */
	static BlockPos findSite(ServerLevel level, ChunkPos chunk, RandomSource random) {
		BlockPos best = null;
		int bestScore = Integer.MIN_VALUE;
		for (int attempt = 0; attempt < 96; attempt++) {
			int x = chunk.getMinBlockX() - 16 + random.nextInt(48);
			int z = chunk.getMinBlockZ() - 16 + random.nextInt(48);
			int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos top = new BlockPos(x, surface, z);
			if (surface > MOUNTAIN && CultivatorNpc.isSeatable(level, top) && clear(level, top)) {
				int score = 10 + (surface - MOUNTAIN) / 5;
				if (score > bestScore) {
					bestScore = score;
					best = top;
				}
			}
			for (int y = surface - 8; y > Math.max(DEEPEST, level.getMinBuildHeight() + 4); y--) {
				BlockPos feet = new BlockPos(x, y, z);
				if (!CultivatorNpc.isSeatable(level, feet) || level.getBrightness(LightLayer.SKY, feet) >= 4 || !clear(level, feet)) continue;
				int score = 20 + random.nextInt(5);
				if (score > bestScore) {
					bestScore = score;
					best = feet;
				}
				break;
			}
		}
		return best;
	}

	/**
	 * No lava within two blocks; off every sect's grounds and outside every barrier; and nobody's work about: unlit (no torch
	 * or lantern light) and no chest, bed, furnace, sign or other block with a block entity, nor rails, within four blocks.
	 * A dark pocket deep in someone's mine or under their house looks just like a cave otherwise.
	 */
	private static boolean clear(ServerLevel level, BlockPos feet) {
		if (level.getBrightness(LightLayer.BLOCK, feet) > 0) return false;
		for (BlockPos near : BlockPos.betweenClosed(feet.offset(-4, -2, -4), feet.offset(4, 3, 4))) {
			BlockState state = level.getBlockState(near);
			if (state.hasBlockEntity() || state.is(BlockTags.RAILS)) return false;
			if (Math.abs(near.getX() - feet.getX()) <= 2 && Math.abs(near.getZ() - feet.getZ()) <= 2 && state.getFluidState().is(FluidTags.LAVA)) return false;
		}
		return SectManager.sectAt(level, feet) == null && !Formations.shelters(level, feet);
	}

	public static void clearPending() {
		PENDING.clear();
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		tag.putLongArray("Decided", decided.stream().mapToLong(Long::longValue).toArray());
		ListTag seated = new ListTag();
		for (Map.Entry<UUID, Long> e : hermits.entrySet()) {
			CompoundTag h = new CompoundTag();
			h.putUUID("Hermit", e.getKey());
			h.putLong("Region", e.getValue());
			seated.add(h);
		}
		tag.put("Hermits", seated);
		ListTag vacant = new ListTag();
		for (Map.Entry<Long, Long> e : vacantUntil.entrySet()) {
			CompoundTag v = new CompoundTag();
			v.putLong("Region", e.getKey());
			v.putLong("Until", e.getValue());
			vacant.add(v);
		}
		tag.put("Vacant", vacant);
		return tag;
	}

	public static HermitSites load(CompoundTag tag) {
		HermitSites data = new HermitSites();
		for (long key : tag.getLongArray("Decided")) data.decided.add(key);
		ListTag seated = tag.getList("Hermits", Tag.TAG_COMPOUND);
		for (int i = 0; i < seated.size(); i++) {
			CompoundTag h = seated.getCompound(i);
			if (h.hasUUID("Hermit")) data.hermits.put(h.getUUID("Hermit"), h.getLong("Region"));
		}
		ListTag vacant = tag.getList("Vacant", Tag.TAG_COMPOUND);
		for (int i = 0; i < vacant.size(); i++) data.vacantUntil.put(vacant.getCompound(i).getLong("Region"), vacant.getCompound(i).getLong("Until"));
		return data;
	}

	/** Whether {@code npc} is a region's hermit (for tests). */
	static boolean isHermit(MinecraftServer server, CultivatorNpc npc) {
		return get(server).hermits.containsKey(npc.getUUID());
	}

	/** When (game time) region {@code key} gets a new hermit, or -1 if it isn't waiting for one (for tests). */
	static long vacantUntil(MinecraftServer server, long key) {
		Long until = get(server).vacantUntil.get(key);
		return until == null ? -1 : until;
	}

	/** Seats a region's hermit as if its chunk had just loaded (for tests): {@code npc} becomes region {@code key}'s hermit. */
	static void seatForTest(MinecraftServer server, CultivatorNpc npc, long key) {
		HermitSites data = get(server);
		data.decided.add(key);
		data.hermits.put(npc.getUUID(), key);
	}
}
