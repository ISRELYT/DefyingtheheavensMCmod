package com.example.defyingtheheavens;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The land as it was before players changed it (the user's ask, 2026-10-28, for concealment: a live record, "make sure the game keeping a
 * record of land isn't taxing on performance"): for every block a player's hands (breaking, placing, using an item on a block: mixin/
 * ServerPlayerGameModeMixin), blasts (ExplosionMixin) or spells (HeavenSpells#carvable) change, the state that stood there first.
 * <p>
 * Written only while a player acts, a map look-up a block; read only when a concealed area is sent (Concealments). Kept on each chunk and
 * saved in its own entry of the chunk's file (mixin/ChunkSerializerMixin), so only loaded chunks hold theirs in memory, and a world's
 * ledgers cost nothing where no one has built. A block set back to what first stood there is struck off it.
 */
public final class LandLedger {
	/** The chunk file's entry. */
	private static final String TAG = "dth_ledger";
	/** How deep in players' acts the server thread is (0: none): what changes meanwhile is their doing. */
	private static int acting;

	/** A player's act begins (each begin has its end; START_SERVER_TICK sets it back to none, so a throw midway can't leave it on). */
	public static void begin() {
		acting++;
	}

	public static void end() {
		if (acting > 0) acting--;
	}

	public static void tickReset() {
		acting = 0;
	}

	public static boolean recording() {
		return acting > 0;
	}

	/**
	 * LevelChunk#setBlockState is about to set {@code pos} to {@code now} (mixin/LevelChunkLedgerMixin): noted if a player's act does it, or
	 * if the world does it to or beside a block players changed ({@link #spreading}). Anywhere no player has changed anything, one
	 * counter's check and a field's.
	 */
	public static void setting(LevelChunk chunk, BlockPos pos, BlockState now) {
		if (chunk.getLevel().isClientSide) return;
		if (acting > 0) note(chunk, pos, chunk.getBlockState(pos), now);
		else spreading(chunk, pos, now);
	}

	/**
	 * The world changing {@code pos} by itself where players have worked: grass dying to dirt under a wall, a sapling's tree growing, water
	 * a bucket poured running on, fire spreading from a player's fire, the leaves of a felled tree falling. A change at or beside a block
	 * players changed is theirs too, so the land shown under a concealment stays whole (the user, 2026-10-28: "the blocks where the
	 * concealment barrier is placed has dirt rather than grass": the grass under a hut's walls had died, and showed its outline).
	 */
	private static void spreading(LevelChunk chunk, BlockPos pos, BlockState now) {
		Long2ObjectOpenHashMap<BlockState> ledger = ((LedgerHolder) chunk).dth$ledger();
		if (ledger == null || ledger.isEmpty()) return;
		BlockState before = chunk.getBlockState(pos);
		if (!ledger.containsKey(pos.asLong())) {
			// The weather's snow and ice settle wherever the sky reaches: theirs only lying on players' work (snow on a hidden roof). Beside
			// it, a strip of bare ground round hidden walls would show where they stand.
			boolean weather = isWeather(before) || isWeather(now);
			if (weather ? !ledger.containsKey(BlockPos.asLong(pos.getX(), pos.getY() - 1, pos.getZ())) : !besideNoted(chunk, pos)) return;
		}
		note(chunk, pos, before, now);
	}

	private static boolean isWeather(BlockState state) {
		return state.is(Blocks.SNOW) || state.is(Blocks.ICE) || state.is(Blocks.POWDER_SNOW);
	}

	/** Whether a block beside {@code pos} (its six faces, across into a loaded neighbouring chunk) is one players changed. */
	private static boolean besideNoted(LevelChunk chunk, BlockPos pos) {
		int x = pos.getX(), y = pos.getY(), z = pos.getZ();
		int[][] faces = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
		for (int[] face : faces) {
			int nx = x + face[0], ny = y + face[1], nz = z + face[2];
			LevelChunk holder = nx >> 4 == chunk.getPos().x && nz >> 4 == chunk.getPos().z ? chunk
					: chunk.getLevel() instanceof ServerLevel level ? level.getChunkSource().getChunkNow(nx >> 4, nz >> 4) : null;
			if (holder == null) continue;
			Long2ObjectOpenHashMap<BlockState> ledger = ((LedgerHolder) holder).dth$ledger();
			if (ledger != null && ledger.containsKey(BlockPos.asLong(nx, ny, nz))) return true;
		}
		return false;
	}

	/** {@code pos} goes from {@code before} to {@code now}: the first state kept, struck off once it stands there again. */
	static void note(LevelChunk chunk, BlockPos pos, BlockState before, BlockState now) {
		if (before == now) return;
		LedgerHolder holder = (LedgerHolder) chunk;
		Long2ObjectOpenHashMap<BlockState> ledger = holder.dth$ledger();
		long key = pos.asLong();
		BlockState first = ledger == null ? null : ledger.get(key);
		if (first == null) {
			if (ledger == null) holder.dth$setLedger(ledger = new Long2ObjectOpenHashMap<>());
			ledger.put(key, before);
		} else if (first == now) {
			ledger.remove(key);
		} else {
			return;
		}
		chunk.setUnsaved(true);
	}

	/** A player's spell is about to take {@code pos} (HeavenSpells#carvable): what stands there now noted, unless something was already. */
	public static void noteSpell(ServerLevel level, BlockPos pos) {
		LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
		if (chunk == null) return;
		Long2ObjectOpenHashMap<BlockState> ledger = ((LedgerHolder) chunk).dth$ledger();
		if (ledger != null && ledger.containsKey(pos.asLong())) return;
		note(chunk, pos, chunk.getBlockState(pos), null);
	}

	/** What stood at {@code pos} before players changed it, or null if they never did (or set it back). */
	public static BlockState original(LevelChunk chunk, BlockPos pos) {
		Long2ObjectOpenHashMap<BlockState> ledger = ((LedgerHolder) chunk).dth$ledger();
		return ledger == null ? null : ledger.get(pos.asLong());
	}

	/** {@link #original(LevelChunk, BlockPos)} in {@code level}'s loaded chunk (null if it isn't loaded). */
	public static BlockState original(ServerLevel level, BlockPos pos) {
		LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
		return chunk == null ? null : original(chunk, pos);
	}

	/** The positions (BlockPos#asLong) players have changed in {@code chunk}. */
	public static LongSet positions(LevelChunk chunk) {
		Long2ObjectOpenHashMap<BlockState> ledger = ((LedgerHolder) chunk).dth$ledger();
		return ledger == null ? LongSets.EMPTY_SET : ledger.keySet();
	}

	public static int size(LevelChunk chunk) {
		Long2ObjectOpenHashMap<BlockState> ledger = ((LedgerHolder) chunk).dth$ledger();
		return ledger == null ? 0 : ledger.size();
	}

	// --- With the chunk's file (mixin/ChunkSerializerMixin) ---

	/**
	 * The ledger into the chunk's file: its states once each (a palette: air, stone, dirt...), then each position (BlockPos#asLong) with
	 * its state's index. Nothing written for a chunk no one has changed.
	 */
	public static void write(ChunkAccess chunk, CompoundTag tag) {
		if (chunk instanceof ImposterProtoChunk imposter) chunk = imposter.getWrapped();
		Long2ObjectOpenHashMap<BlockState> ledger = ((LedgerHolder) chunk).dth$ledger();
		if (ledger == null || ledger.isEmpty()) return;
		ListTag palette = new ListTag();
		Object2IntOpenHashMap<BlockState> index = new Object2IntOpenHashMap<>();
		index.defaultReturnValue(-1);
		long[] positions = new long[ledger.size()];
		int[] states = new int[ledger.size()];
		int i = 0;
		for (Long2ObjectMap.Entry<BlockState> entry : ledger.long2ObjectEntrySet()) {
			int state = index.getInt(entry.getValue());
			if (state < 0) {
				state = palette.size();
				palette.add(NbtUtils.writeBlockState(entry.getValue()));
				index.put(entry.getValue(), state);
			}
			positions[i] = entry.getLongKey();
			states[i++] = state;
		}
		CompoundTag out = new CompoundTag();
		out.put("Palette", palette);
		out.putLongArray("Pos", positions);
		out.putIntArray("State", states);
		tag.put(TAG, out);
	}

	/** The ledger back from the chunk's file, onto the chunk read (a full chunk's, within the stand-in vanilla hands back). */
	public static void read(ChunkAccess chunk, CompoundTag tag) {
		if (!tag.contains(TAG, Tag.TAG_COMPOUND)) return;
		if (chunk instanceof ImposterProtoChunk imposter) chunk = imposter.getWrapped();
		CompoundTag in = tag.getCompound(TAG);
		ListTag paletteTag = in.getList("Palette", Tag.TAG_COMPOUND);
		BlockState[] palette = new BlockState[paletteTag.size()];
		for (int i = 0; i < palette.length; i++) palette[i] = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), paletteTag.getCompound(i));
		long[] positions = in.getLongArray("Pos");
		int[] states = in.getIntArray("State");
		Long2ObjectOpenHashMap<BlockState> ledger = new Long2ObjectOpenHashMap<>(positions.length);
		for (int i = 0; i < positions.length && i < states.length; i++) {
			if (states[i] >= 0 && states[i] < palette.length) ledger.put(positions[i], palette[states[i]]);
		}
		if (!ledger.isEmpty()) ((LedgerHolder) chunk).dth$setLedger(ledger);
	}

	private LandLedger() {}
}
