package com.example.defyingtheheavens;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The secret realms' land as the realm laid it (SecretRealms: "each opening repairs what visitors broke, through a ledger of block changes like
 * Concealment's"): for every block anything changes in their dimension (a visitor's hands, a blast, a spell, a guardian, the closing quake's
 * cracks), the block that stood there first, noted as it changes (mixin/LevelChunkLedgerMixin) and set back as the realm next stirs
 * ({@link #mending}). Only a block becoming another is noted, not a block's state turning (a door opened, a leaf's distance) nor water
 * running on (a source poured is noted; what runs from it dries when it is taken up). What the realm itself lays and renews is laid
 * {@link #quietly}. The bodies of the dead are never taken up (SecretRealms' rule: they lie sealed in until someone finds them), nor are
 * herbs set back (the realm renews its beds itself). Kept with the world (the Overworld's data).
 */
public final class RealmLedger extends SavedData {
	private static final String NAME = DefyingTheHeavens.MOD_ID + "_secret_realm_ledger";
	/** More than this many blocks changed, and the rest go unnoted (a realm left in ruins, not a save swollen without end). */
	public static final int MOST = 400_000;

	private final Long2ObjectOpenHashMap<BlockState> first = new Long2ObjectOpenHashMap<>();
	/** Set while the realm lays or mends its own land: nothing changed meanwhile is a visitor's. */
	private static int quiet;
	/** The world's ledger while it runs (looked up once, not each block). */
	private static RealmLedger current;

	public static RealmLedger get(MinecraftServer server) {
		RealmLedger ledger = server.overworld().getDataStorage().computeIfAbsent(RealmLedger::load, RealmLedger::new, NAME);
		current = ledger;
		return ledger;
	}

	/** {@code work} done as the realm's own: nothing it changes is noted. */
	public static void quietly(Runnable work) {
		quiet++;
		try {
			work.run();
		} finally {
			quiet--;
		}
	}

	public static boolean isQuiet() { return quiet > 0; }

	/** LevelChunk#setBlockState is about to set {@code pos} to {@code now} (mixin/LevelChunkLedgerMixin): noted if it is in the realms and no work of theirs. */
	public static void setting(LevelChunk chunk, BlockPos pos, BlockState now) {
		if (quiet > 0 || current == null || chunk.getLevel().isClientSide || !ModDimensions.isSecretRealm(chunk.getLevel().dimension())) return;
		BlockState before = chunk.getBlockState(pos);
		if (before.getBlock() == now.getBlock() || running(before) || running(now)) return;
		current.note(pos.asLong(), before, now);
	}

	/** Water or lava running (not a source): it comes and goes of itself. */
	private static boolean running(BlockState state) {
		return state.getBlock() instanceof LiquidBlock && !state.getFluidState().isSource();
	}

	private void note(long key, BlockState before, BlockState now) {
		BlockState was = first.get(key);
		if (was == null) {
			if (first.size() >= MOST) return;
			first.put(key, before);
		} else if (was == now) {
			first.remove(key);
		} else {
			return;
		}
		setDirty();
	}

	/** How many blocks stand changed in the realms now. */
	public int size() { return first.size(); }

	/** What first stood at {@code pos} (null: nothing changed it since the realm last mended it). */
	public BlockState original(BlockPos pos) { return first.get(pos.asLong()); }

	/**
	 * The mending of {@code realm}'s land, to be done a little at a time ({@link Mending#step}): every block changed in its reach set back as
	 * it first stood, chunk by chunk (each loaded once). A body lying where something stood stays; herbs aren't set back.
	 */
	public Mending mending(SecretRealm realm) {
		Map<Long, List<Long>> byChunk = new HashMap<>();
		BlockPos centre = realm.centre();
		for (Iterator<Long2ObjectMap.Entry<BlockState>> it = first.long2ObjectEntrySet().iterator(); it.hasNext(); ) {
			long key = it.next().getLongKey();
			int x = BlockPos.getX(key), z = BlockPos.getZ(key);
			if (SecretRealm.at(x, z) != realm || Math.hypot(x - centre.getX(), z - centre.getZ()) > SecretRealm.BOUND + 16) continue;
			byChunk.computeIfAbsent(ChunkPos.asLong(x >> 4, z >> 4), c -> new ArrayList<>()).add(key);
		}
		return new Mending(new ArrayList<>(byChunk.values()));
	}

	/** One realm's mending under way: a chunk's blocks at a time. */
	public final class Mending {
		private final List<List<Long>> chunks;
		private int next;
		public int mended;

		Mending(List<List<Long>> chunks) {
			this.chunks = chunks;
		}

		public boolean done() { return next >= chunks.size(); }

		/** The next chunk's blocks set back (its chunk loaded for it). */
		public void step(ServerLevel level) {
			if (done()) return;
			List<Long> keys = chunks.get(next++);
			quietly(() -> {
				BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
				for (long key : keys) {
					BlockState was = first.remove(key);
					if (was == null) continue;
					pos.set(key);
					BlockState here = level.getBlockState(pos);
					// The dead keep their place; the beds are the realm's to renew.
					if (here.getBlock() instanceof CorpseBlock || was.getBlock() instanceof CorpseBlock || was.getBlock() instanceof GinsengBlock) continue;
					level.setBlock(pos, was, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
					mended++;
				}
			});
			setDirty();
		}
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		// Its states once each (a palette), then each position with its state's index.
		Map<BlockState, Integer> palette = new HashMap<>();
		ListTag states = new ListTag();
		long[] keys = new long[first.size()];
		int[] indices = new int[first.size()];
		int i = 0;
		for (Long2ObjectMap.Entry<BlockState> entry : first.long2ObjectEntrySet()) {
			Integer index = palette.get(entry.getValue());
			if (index == null) {
				index = palette.size();
				palette.put(entry.getValue(), index);
				states.add(NbtUtils.writeBlockState(entry.getValue()));
			}
			keys[i] = entry.getLongKey();
			indices[i++] = index;
		}
		tag.put("Palette", states);
		tag.put("Positions", new LongArrayTag(keys));
		tag.putIntArray("States", indices);
		return tag;
	}

	public static RealmLedger load(CompoundTag tag) {
		RealmLedger ledger = new RealmLedger();
		ListTag palette = tag.getList("Palette", Tag.TAG_COMPOUND);
		List<BlockState> states = new ArrayList<>();
		for (int i = 0; i < palette.size(); i++) {
			states.add(NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(), palette.getCompound(i)));
		}
		long[] keys = tag.getLongArray("Positions");
		int[] indices = tag.getIntArray("States");
		for (int i = 0; i < Math.min(keys.length, indices.length); i++) {
			if (indices[i] >= 0 && indices[i] < states.size()) ledger.first.put(keys[i], states.get(indices[i]));
		}
		return ledger;
	}

	/** The server stopped: the next one looks its own up. */
	public static void clear() {
		current = null;
		quiet = 0;
	}

	private RealmLedger() {}
}
