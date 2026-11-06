package com.example.defyingtheheavens;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.LiquidBlock;
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
 * ledgers cost nothing where no one has built. A block set back to what first stood there is struck off it. What a stranger does under a
 * raised veil isn't noted at all: it never reaches the land (Concealments#wreck), but for a new block they set down (Concealments#strangerSets).
 */
public final class LandLedger {
	/** The chunk file's entry. */
	private static final String TAG = "dth_ledger";
	/** How deep in players' acts the server thread is (0: none): what changes meanwhile is their doing. */
	private static int acting;
	/** Whose acts they are, where that was said. */
	private static ServerPlayer actor;
	/** A stranger's own block their spell took this tick ({@link #noteSpell}): when it goes it is no doing of the world's, whatever stands beside it. */
	private static final LongOpenHashSet LAID_BARE = new LongOpenHashSet();
	/** Where a stranger's spell works under a raised veil this tick ({@link #noteSpell}, {@link #markSpell}): what it sets there goes no further than strangers' eyes. */
	private static final LongOpenHashSet FOR_SHOW = new LongOpenHashSet();

	/** A player's act begins (each begin has its end; START_SERVER_TICK sets it back to none, so a throw midway can't leave it on). */
	public static void begin() {
		acting++;
	}

	/** {@code player}'s act begins (the first of them, where one comes within another, the one who answers for it). */
	public static void begin(ServerPlayer player) {
		if (acting++ == 0) actor = player;
	}

	public static void end() {
		if (acting > 0) acting--;
		if (acting == 0) actor = null;
	}

	public static void tickReset() {
		acting = 0;
		actor = null;
		LAID_BARE.clear();
		FOR_SHOW.clear();
	}

	public static boolean recording() {
		return acting > 0;
	}

	/** Whether a stranger's spell is at work anywhere this tick ({@link #FOR_SHOW}): the one check every block change makes for it. */
	public static boolean showing() {
		return !FOR_SHOW.isEmpty();
	}

	/** Whether what is set at {@code pos} now goes no further than strangers' eyes (so nothing it holds is spilled for it: Devastation#obliterate). */
	static boolean forShow(BlockPos pos) {
		return FOR_SHOW.contains(pos.asLong());
	}

	/**
	 * LevelChunk#setBlockState is about to set {@code pos} to {@code now} (mixin/LevelChunkLedgerMixin): noted if a player's act does it, or
	 * if the world does it to or beside a block players changed ({@link #spreading}). Under a raised veil a stranger's act goes otherwise: a new
	 * block they set down is real and kept from the veil's owner alone, and anything else they do to the land goes no further than strangers' eyes
	 * (Concealments#wreck). Anywhere no player has changed anything, one counter's check and a field's.
	 *
	 * @return whether the change is kept from the world: the block stands as it was
	 */
	public static boolean setting(LevelChunk chunk, BlockPos pos, BlockState now) {
		if (!(chunk.getLevel() instanceof ServerLevel level)) return false;
		BlockState before = chunk.getBlockState(pos);
		if (FOR_SHOW.contains(pos.asLong()) && before != now) return Concealments.wreck(level, pos, now);
		if (acting == 0) {
			Concealments.settles(level, pos, now);
			if (!LAID_BARE.contains(pos.asLong())) spreading(chunk, pos, now);
			return false;
		}
		if (actor == null || !Concealments.strangerAt(level, actor, pos)) {
			// Over a stranger's block the land as it was is what stood under that, not the block their hands just lost.
			BlockState under = Concealments.strangerGone(level, pos);
			note(chunk, pos, under != null ? under : before, now);
			return false;
		}
		if (Concealments.strangersBlock(level, pos)) {
			// A block of their own, real: theirs to rework or take.
			if (now.isAir() || now.getBlock() instanceof LiquidBlock) {
				Concealments.strangerGone(level, pos);
				strikeOff(chunk, pos);
			} else {
				Concealments.strangerSets(level, pos, before);
			}
			return false;
		}
		// A door swung, a fence joining a block set beside it: the same block, as seen or as hidden as it was.
		if (before.isAir() && now.isAir() || before.is(now.getBlock())) return false;
		if (!setsDown(before, now)) return Concealments.wreck(level, pos, now);
		Concealments.strangerSets(level, pos, before);
		return false;
	}

	/** Whether {@code pos} going from {@code before} to {@code now} is a new block set down, into air or what gives way to any block; not water, lava or fire, which would run and burn on from it. */
	private static boolean setsDown(BlockState before, BlockState now) {
		return before.canBeReplaced() && !now.isAir() && !(now.getBlock() instanceof LiquidBlock) && !(now.getBlock() instanceof BaseFireBlock);
	}

	/** {@code pos} off {@code chunk}'s ledger: it shows as it stands. */
	private static void strikeOff(LevelChunk chunk, BlockPos pos) {
		Long2ObjectOpenHashMap<BlockState> ledger = ((LedgerHolder) chunk).dth$ledger();
		if (ledger != null && ledger.remove(pos.asLong()) != null) chunk.setUnsaved(true);
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

	/**
	 * {@code caster}'s spell is about to take {@code pos} (HeavenSpells#carvable): what stands there now noted, unless something was already; a
	 * stranger's under a raised veil goes no further than strangers' eyes (Concealments#wreck), but for a block of their own, which simply goes.
	 */
	public static void noteSpell(ServerLevel level, BlockPos pos, ServerPlayer caster) {
		LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
		if (chunk == null) return;
		boolean stranger = Concealments.strangerAt(level, caster, pos);
		if (stranger && !Concealments.strangersBlock(level, pos)) {
			FOR_SHOW.add(pos.asLong());
			return;
		}
		BlockState under = Concealments.strangerGone(level, pos);
		if (stranger) {
			strikeOff(chunk, pos);
			LAID_BARE.add(pos.asLong());
			return;
		}
		Long2ObjectOpenHashMap<BlockState> ledger = ((LedgerHolder) chunk).dth$ledger();
		if (ledger != null && ledger.containsKey(pos.asLong())) return;
		note(chunk, pos, under != null ? under : chunk.getBlockState(pos), null);
	}

	/** {@code caster}'s spell is about to set something at {@code pos} (Devastation#mayChange): a stranger's goes no further than strangers' eyes, as at {@link #noteSpell}. */
	static void markSpell(ServerLevel level, BlockPos pos, ServerPlayer caster) {
		if (Concealments.strangerAt(level, caster, pos) && !Concealments.strangersBlock(level, pos)) FOR_SHOW.add(pos.asLong());
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
		tag.put(TAG, pack(ledger));
	}

	/** {@code places} as a tag: their states once each (a palette), then each place with its state's index. */
	static CompoundTag pack(Long2ObjectMap<BlockState> places) {
		ListTag palette = new ListTag();
		Object2IntOpenHashMap<BlockState> index = new Object2IntOpenHashMap<>();
		index.defaultReturnValue(-1);
		long[] positions = new long[places.size()];
		int[] states = new int[places.size()];
		int i = 0;
		for (Long2ObjectMap.Entry<BlockState> entry : places.long2ObjectEntrySet()) {
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
		return out;
	}

	/** The ledger back from the chunk's file, onto the chunk read (a full chunk's, within the stand-in vanilla hands back). */
	public static void read(ChunkAccess chunk, CompoundTag tag) {
		if (!tag.contains(TAG, Tag.TAG_COMPOUND)) return;
		if (chunk instanceof ImposterProtoChunk imposter) chunk = imposter.getWrapped();
		Long2ObjectOpenHashMap<BlockState> ledger = unpack(tag.getCompound(TAG));
		if (!ledger.isEmpty()) ((LedgerHolder) chunk).dth$setLedger(ledger);
	}

	/** {@link #pack}'s tag read back. */
	static Long2ObjectOpenHashMap<BlockState> unpack(CompoundTag in) {
		ListTag paletteTag = in.getList("Palette", Tag.TAG_COMPOUND);
		BlockState[] palette = new BlockState[paletteTag.size()];
		for (int i = 0; i < palette.length; i++) palette[i] = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), paletteTag.getCompound(i));
		long[] positions = in.getLongArray("Pos");
		int[] states = in.getIntArray("State");
		Long2ObjectOpenHashMap<BlockState> places = new Long2ObjectOpenHashMap<>(positions.length);
		for (int i = 0; i < positions.length && i < states.length; i++) {
			if (states[i] >= 0 && states[i] < palette.length) places.put(positions[i], palette[states[i]]);
		}
		return places;
	}

	private LandLedger() {}
}
