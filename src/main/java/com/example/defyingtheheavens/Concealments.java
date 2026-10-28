package com.example.defyingtheheavens;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Concealment, the second of a Formation Core's functions beside its barrier (the user's asks, 2026-10-28: "concealment formations ...
 * hides any player built structures or changes made to terrain, and players inside it"; "a function of the current formation core ... run
 * off of qi veins like the protective barrier"; seen through only by "anyone stronger than its owner", and walking in reveals nothing).
 * <p>
 * A raised {@link Veil} is a sphere round its core. Anyone who can't see through it ({@link #weigh}: not its owner, nor one they trust, nor
 * an operator in creative or spectator, nor stronger than its owner) is shown everything in it as the land was before players changed it
 * (LandLedger), and its barrier's cells as the air or water they stand in: the chunks there are put right just after they are sent
 * ({@link #chunkSent}), and every block change, a block's own data and light changes there are rewritten or kept back as they go out to
 * each player ({@link #shownPacket}). The players inside are never sent them (mixin/SoulServerPlayerMixin), nor the sounds, particles and
 * cracks of breaking blocks from within ({@link #broadcast}, {@link #cracks}), and they can't lay hands on what is hidden from them.
 * <p>
 * Nothing is looked at where no veil stands: every check starts from the world's raised veils, an empty array almost everywhere.
 */
public final class Concealments {
	private static final String FILE = "defying_the_heavens_concealments";
	private static final Veil[] NONE = new Veil[0];
	/** Whether a veil stands raised in any world: the one check the hottest hooks make (every collision, every block's touch). */
	public static boolean any;
	/** The raised veils of each world, for the checks on every packet that might show what one hides. */
	private static final Map<ResourceKey<Level>, Veil[]> RAISED = new HashMap<>();
	/** Each player's standing among the veils near them at the last look (inside which, seeing through which): re-tracked when it changes. */
	private static final Map<UUID, Long> STANDING = new HashMap<>();

	/** One core's concealment (its id the core's own). */
	public static final class Veil {
		public final UUID id;
		public final BlockPos center;
		private int radius;
		private boolean raised;
		/** The owner's rank (PlayerCultivation#rank) when they last set it: anyone above it sees through. -1: a mortal's. */
		private int rank = -1;
		private UUID owner;
		private final Set<UUID> trusted = new LinkedHashSet<>();
		/** Runtime: who sees through it, as last weighed (#tick weighs those near it again each second). */
		final Map<UUID, Boolean> seers = new HashMap<>();

		Veil(UUID id, BlockPos center, int radius) {
			this.id = id;
			this.center = center.immutable();
			this.radius = radius;
		}

		public int radius() { return radius; }
		public boolean isRaised() { return raised; }
		public int rank() { return rank; }
		public UUID owner() { return owner; }
		public Set<UUID> trusted() { return Collections.unmodifiableSet(trusted); }

		public boolean encloses(double x, double y, double z) {
			double dx = x - (center.getX() + 0.5), dy = y - (center.getY() + 0.5), dz = z - (center.getZ() + 0.5);
			double r = radius + 0.5;
			return dx * dx + dy * dy + dz * dz <= r * r;
		}

		public boolean encloses(BlockPos pos) {
			return encloses(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		}

		boolean touches(int chunkX, int chunkZ) {
			int minX = chunkX << 4, minZ = chunkZ << 4;
			return center.getX() + radius >= minX && center.getX() - radius <= minX + 15 && center.getZ() + radius >= minZ && center.getZ() - radius <= minZ + 15;
		}

		CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putUUID("Id", id);
			tag.putLong("Center", center.asLong());
			tag.putInt("Radius", radius);
			tag.putBoolean("Raised", raised);
			tag.putInt("Rank", rank);
			if (owner != null) tag.putUUID("Owner", owner);
			ListTag list = new ListTag();
			for (UUID player : trusted) list.add(NbtUtils.createUUID(player));
			tag.put("Trusted", list);
			return tag;
		}

		static Veil load(CompoundTag tag) {
			Veil veil = new Veil(tag.getUUID("Id"), BlockPos.of(tag.getLong("Center")), tag.getInt("Radius"));
			veil.raised = tag.getBoolean("Raised");
			veil.rank = tag.getInt("Rank");
			if (tag.hasUUID("Owner")) veil.owner = tag.getUUID("Owner");
			for (Tag entry : tag.getList("Trusted", Tag.TAG_INT_ARRAY)) veil.trusted.add(NbtUtils.loadUUID(entry));
			return veil;
		}
	}

	/** The veils of one world, kept with it (a veil stays as it was while its core's chunk is unloaded, as a barrier does). */
	public static final class Data extends SavedData {
		private final Map<UUID, Veil> veils = new LinkedHashMap<>();

		public static Data get(ServerLevel level) {
			return level.getDataStorage().computeIfAbsent(Data::load, Data::new, FILE);
		}

		static Data load(CompoundTag tag) {
			Data data = new Data();
			for (Tag entry : tag.getList("Veils", Tag.TAG_COMPOUND)) {
				Veil veil = Veil.load((CompoundTag) entry);
				data.veils.put(veil.id, veil);
			}
			return data;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			ListTag list = new ListTag();
			for (Veil veil : veils.values()) list.add(veil.save());
			tag.put("Veils", list);
			return tag;
		}
	}

	public static void register() {
		ServerTickEvents.START_SERVER_TICK.register(server -> LandLedger.tickReset());
		ServerTickEvents.END_SERVER_TICK.register(Concealments::tick);
		ServerWorldEvents.LOAD.register((server, level) -> reindex(level));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			RAISED.clear();
			STANDING.clear();
			any = false;
		});
		// No hand on what is hidden from one: they see the land as it was there (their client set right again).
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (world.isClientSide || !(player instanceof ServerPlayer viewer) || !reachesHidden(viewer, hit.getBlockPos())) return InteractionResult.PASS;
			viewer.connection.send(new ClientboundBlockUpdatePacket(world, hit.getBlockPos()));
			return InteractionResult.FAIL;
		});
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (world.isClientSide || !(player instanceof ServerPlayer viewer) || !reachesHidden(viewer, pos)) return InteractionResult.PASS;
			viewer.connection.send(new ClientboundBlockUpdatePacket(world, pos));
			return InteractionResult.FAIL;
		});
	}

	// --- The veils ---

	/** The raised veils of {@code level} (none, at once, where none stands). */
	public static Veil[] raised(Level level) {
		return RAISED.getOrDefault(level.dimension(), NONE);
	}

	private static void reindex(ServerLevel level) {
		Veil[] raised = Data.get(level).veils.values().stream().filter(veil -> veil.raised).toArray(Veil[]::new);
		if (raised.length == 0) RAISED.remove(level.dimension());
		else RAISED.put(level.dimension(), raised);
		any = !RAISED.isEmpty();
	}

	/** The veil {@code id}, of the core at {@code center} (made, lowered, the first time). */
	public static Veil veil(ServerLevel level, UUID id, BlockPos center, int radius) {
		Data data = Data.get(level);
		Veil veil = data.veils.get(id);
		if (veil == null) {
			veil = new Veil(id, center, radius);
			data.veils.put(id, veil);
			data.setDirty();
		}
		return veil;
	}

	public static Veil get(ServerLevel level, UUID id) {
		return id == null ? null : Data.get(level).veils.get(id);
	}

	/** Raises or lowers {@code veil}, at {@code radius}: everyone about shown what they may see now. */
	public static void set(ServerLevel level, Veil veil, boolean raised, int radius) {
		if (veil.raised == raised && veil.radius == radius) return;
		boolean showing = veil.raised || raised;
		int reach = Math.max(veil.radius, radius);
		veil.raised = raised;
		veil.radius = radius;
		changed(level, veil, showing ? reach : -1);
	}

	/** Whom {@code veil} answers to: its owner's rank (anyone above it sees through), its owner, those they trust. */
	public static void update(ServerLevel level, Veil veil, int rank, UUID owner, Collection<UUID> trusted) {
		if (veil.rank == rank && Objects.equals(veil.owner, owner) && veil.trusted.size() == trusted.size() && veil.trusted.containsAll(trusted)) return;
		veil.rank = rank;
		veil.owner = owner;
		veil.trusted.clear();
		veil.trusted.addAll(trusted);
		changed(level, veil, veil.raised ? veil.radius : -1);
	}

	/** {@code id}'s veil is gone (its core broken): lowered, and forgotten. */
	public static void dissolve(ServerLevel level, UUID id) {
		Data data = Data.get(level);
		Veil veil = data.veils.remove(id);
		if (veil == null) return;
		boolean was = veil.raised;
		veil.raised = false;
		data.setDirty();
		reindex(level);
		if (was) refreshAround(level, veil.center, veil.radius);
	}

	/** {@code veil} changed: saved, the index made again, all weighed anew, and those about shown again within {@code reach} (-1: none). */
	private static void changed(ServerLevel level, Veil veil, int reach) {
		Data.get(level).setDirty();
		reindex(level);
		veil.seers.clear();
		if (reach >= 0) refreshAround(level, veil.center, reach);
	}

	// --- Who sees what ---

	/** Whether {@code viewer} sees through {@code veil} (as last weighed: #tick weighs those near it again each second). */
	public static boolean seesThrough(Veil veil, ServerPlayer viewer) {
		Boolean known = veil.seers.get(viewer.getUUID());
		if (known != null) return known;
		boolean sees = weigh(veil, viewer);
		veil.seers.put(viewer.getUUID(), sees);
		return sees;
	}

	/** Its owner, those they trust, an operator in creative or spectator, and anyone stronger than its owner (the user's pick) see through it. */
	static boolean weigh(Veil veil, ServerPlayer viewer) {
		if (viewer.getUUID().equals(veil.owner) || veil.trusted.contains(viewer.getUUID())) return true;
		if ((viewer.isCreative() || viewer.isSpectator()) && viewer.hasPermissions(2)) return true;
		return Formations.rankOf(viewer) > veil.rank;
	}

	/** The raised veil holding (x, y, z) in {@code level} that {@code viewer} can't see through, or null. */
	public static Veil hiding(Level level, ServerPlayer viewer, double x, double y, double z) {
		for (Veil veil : raised(level)) if (veil.encloses(x, y, z) && !seesThrough(veil, viewer)) return veil;
		return null;
	}

	public static Veil hiding(Level level, ServerPlayer viewer, BlockPos pos) {
		return hiding(level, viewer, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
	}

	/**
	 * What {@code viewer} is shown at {@code pos}, where {@code actual} stands: within a veil that hides from them, what stood there before
	 * players changed it, a barrier's cell as the air or water it stands in; anywhere else, what is there.
	 */
	public static BlockState shown(ServerLevel level, ServerPlayer viewer, BlockPos pos, BlockState actual) {
		if (hiding(level, viewer, pos) == null) return actual;
		BlockState original = LandLedger.original(level, pos);
		if (original != null) return original;
		return actual.getBlock() instanceof SectBarrierBlock ? SectBarrierBlock.residue(actual) : actual;
	}

	/** Whether {@code target} is hidden from {@code viewer}: inside a veil they can't see through. */
	public static boolean hidesPlayer(ServerPlayer target, ServerPlayer viewer) {
		return target != viewer && hiding(target.level(), viewer, target.getX(), target.getY() + 0.5, target.getZ()) != null;
	}

	/**
	 * The collision {@code player} meets at {@code pos}, where {@code actual} stands, when a veil hides it from them: the shape of what they
	 * are shown (the user, 2026-10-28: "concealment barriers should no longer provide an invisible wall that blocks people", it takes both
	 * functions "to be unseen and not able to enter"). They walk through hidden work and over the ground they see, as their own eyes have
	 * it. A barrier's cell keeps its own rule: the barrier, not the concealment, keeps people out. Null: as it truly is
	 * (mixin/BlockStateBaseVeilMixin; the shown state's own shape comes back here and passes as it is).
	 */
	public static net.minecraft.world.phys.shapes.VoxelShape illusoryShape(ServerPlayer player, BlockPos pos, BlockState actual) {
		if (actual.getBlock() instanceof SectBarrierBlock || !(player.level() instanceof ServerLevel level) || !level.getServer().isSameThread()) return null;
		BlockState shown = shown(level, player, pos, actual);
		return shown == actual ? null : shown.getCollisionShape(level, pos, net.minecraft.world.phys.shapes.CollisionContext.of(player));
	}

	/** Whether a hidden block at {@code pos} leaves {@code entity} alone: a player it is hidden from feels no fire, thorn or plate of it. */
	public static boolean untouchedBy(net.minecraft.world.entity.Entity entity, BlockPos pos, BlockState actual) {
		return entity instanceof ServerPlayer player && player.level() instanceof ServerLevel level && level.getServer().isSameThread()
				&& !(actual.getBlock() instanceof SectBarrierBlock) && shown(level, player, pos, actual) != actual;
	}

	/** Whether {@code player}'s eyes are inside something hidden from them that what they are shown there wouldn't smother them in. */
	public static boolean breathesInHidden(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level) || !level.getServer().isSameThread()) return false;
		BlockPos eye = BlockPos.containing(player.getEyePosition());
		BlockState actual = level.getBlockState(eye);
		if (actual.getBlock() instanceof SectBarrierBlock) return false;
		BlockState shown = shown(level, player, eye, actual);
		return shown != actual && !shown.isSuffocating(level, eye);
	}

	/** Whether {@code viewer} reaches for something at {@code pos} a veil hides from them. */
	public static boolean reachesHidden(ServerPlayer viewer, BlockPos pos) {
		if (!(viewer.level() instanceof ServerLevel level) || raised(level).length == 0) return false;
		BlockState actual = level.getBlockState(pos);
		return shown(level, viewer, pos, actual) != actual;
	}

	private static boolean enclosedByAny(Veil[] veils, double x, double y, double z) {
		for (Veil veil : veils) if (veil.encloses(x, y, z)) return true;
		return false;
	}

	private static boolean enclosedByAny(Veil[] veils, BlockPos pos) {
		return enclosedByAny(veils, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
	}

	// --- What goes out ---

	/**
	 * A packet about to go to {@code viewer} (mixin/ServerGamePacketListenerImplMixin): a block's state rewritten where a veil hides it from
	 * them; a hidden block's own data (a sign's words, a banner's colours) and the light changes of a chunk a veil hides from them in kept
	 * back. The same packet if nothing in it is hidden (so one rewritten here, sent, passes the second time as it is); null to send nothing.
	 */
	public static Packet<?> shownPacket(ServerPlayer viewer, Packet<?> packet) {
		if (!(viewer.level() instanceof ServerLevel level)) return packet;
		Veil[] veils = raised(level);
		if (veils.length == 0 || !level.getServer().isSameThread()) return packet;
		if (packet instanceof ClientboundBlockUpdatePacket update) {
			BlockPos pos = update.getPos();
			if (!enclosedByAny(veils, pos)) return packet;
			BlockState shown = shown(level, viewer, pos, update.getBlockState());
			return shown == update.getBlockState() ? packet : new ClientboundBlockUpdatePacket(pos, shown);
		}
		if (packet instanceof ClientboundSectionBlocksUpdatePacket section) return shownSection(level, viewer, veils, section);
		if (packet instanceof ClientboundBlockEntityDataPacket data) {
			BlockPos pos = data.getPos();
			return enclosedByAny(veils, pos) && hiding(level, viewer, pos) != null && LandLedger.original(level, pos) != null ? null : packet;
		}
		if (packet instanceof ClientboundLightUpdatePacket light) {
			for (Veil veil : veils) if (veil.touches(light.getX(), light.getZ()) && !seesThrough(veil, viewer)) return null;
		}
		return packet;
	}

	private static Packet<?> shownSection(ServerLevel level, ServerPlayer viewer, Veil[] veils, ClientboundSectionBlocksUpdatePacket section) {
		List<BlockPos> positions = new ArrayList<>();
		List<BlockState> states = new ArrayList<>();
		section.runUpdates((pos, state) -> {
			positions.add(pos.immutable());
			states.add(state);
		});
		boolean changed = false;
		for (int i = 0; i < positions.size(); i++) {
			BlockPos pos = positions.get(i);
			if (!enclosedByAny(veils, pos)) continue;
			BlockState shown = shown(level, viewer, pos, states.get(i));
			if (shown != states.get(i)) {
				states.set(i, shown);
				changed = true;
			}
		}
		return changed ? sectionPacket(positions, states) : section;
	}

	/** One section's blocks as a packet of these states (vanilla's own constructor reads them off the section: these may be the shown ones). */
	static Packet<?> sectionPacket(List<BlockPos> positions, List<BlockState> states) {
		if (positions.size() == 1) return new ClientboundBlockUpdatePacket(positions.get(0), states.get(0));
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		buf.writeLong(SectionPos.of(positions.get(0)).asLong());
		buf.writeVarInt(positions.size());
		for (int i = 0; i < positions.size(); i++) {
			buf.writeVarLong((long) Block.getId(states.get(i)) << 12 | (long) (SectionPos.sectionRelativePos(positions.get(i)) & 0xFFF));
		}
		return new ClientboundSectionBlocksUpdatePacket(buf);
	}

	/** {@code chunk} was just sent to {@code viewer} (mixin/ChunkMapMixin): what a veil hides in it from them put right at once after it. */
	public static void chunkSent(ServerPlayer viewer, LevelChunk chunk) {
		if (!(chunk.getLevel() instanceof ServerLevel level)) return;
		Veil[] veils = raised(level);
		if (veils.length == 0) return;
		for (Veil veil : veils) {
			if (veil.touches(chunk.getPos().x, chunk.getPos().z) && !seesThrough(veil, viewer)) {
				show(viewer, level, chunk, pos -> enclosedByAny(veils, pos));
				return;
			}
		}
	}

	/**
	 * Sends {@code viewer} every block of {@code chunk} a veil could change ({@code within} it): the ones players changed (LandLedger) and the
	 * barrier's cells, as they truly stand, with their own data; the send hook ({@link #shownPacket}) turns each into what they are shown.
	 */
	static void show(ServerPlayer viewer, ServerLevel level, LevelChunk chunk, Predicate<BlockPos> within) {
		LongOpenHashSet cells = new LongOpenHashSet();
		LongIterator changed = LandLedger.positions(chunk).iterator();
		while (changed.hasNext()) {
			long key = changed.nextLong();
			if (within.test(BlockPos.of(key))) cells.add(key);
		}
		barrierCells(chunk, within, cells);
		if (cells.isEmpty()) return;
		Map<Long, List<BlockPos>> bySection = new HashMap<>();
		LongIterator it = cells.iterator();
		while (it.hasNext()) {
			BlockPos pos = BlockPos.of(it.nextLong());
			bySection.computeIfAbsent(SectionPos.of(pos).asLong(), k -> new ArrayList<>()).add(pos);
		}
		for (List<BlockPos> positions : bySection.values()) {
			List<BlockState> states = new ArrayList<>(positions.size());
			for (BlockPos pos : positions) states.add(chunk.getBlockState(pos));
			viewer.connection.send(sectionPacket(positions, states));
			for (int i = 0; i < positions.size(); i++) {
				if (!states.get(i).hasBlockEntity()) continue;
				BlockEntity entity = chunk.getBlockEntity(positions.get(i));
				Packet<?> data = entity == null ? null : entity.getUpdatePacket();
				if (data != null) viewer.connection.send(data);
			}
		}
	}

	/** The barrier's cells in {@code chunk} {@code within} (only the sections whose palette holds one are looked through). */
	private static void barrierCells(LevelChunk chunk, Predicate<BlockPos> within, LongOpenHashSet into) {
		LevelChunkSection[] sections = chunk.getSections();
		int baseX = chunk.getPos().getMinBlockX(), baseZ = chunk.getPos().getMinBlockZ();
		for (int i = 0; i < sections.length; i++) {
			LevelChunkSection section = sections[i];
			if (section.hasOnlyAir() || !section.maybeHas(state -> state.getBlock() instanceof SectBarrierBlock)) continue;
			int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						if (!(section.getBlockState(x, y, z).getBlock() instanceof SectBarrierBlock)) continue;
						BlockPos pos = new BlockPos(baseX + x, baseY + y, baseZ + z);
						if (within.test(pos)) into.add(pos.asLong());
					}
				}
			}
		}
	}

	/** Everyone watching the chunks round {@code center} shown again what they may see within {@code reach} of it (a veil rose, fell, changed). */
	static void refreshAround(ServerLevel level, BlockPos center, int reach) {
		double r = reach + 0.5;
		Predicate<BlockPos> within = pos -> pos.distToCenterSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5) <= r * r;
		for (int cx = (center.getX() - reach) >> 4; cx <= (center.getX() + reach) >> 4; cx++) {
			for (int cz = (center.getZ() - reach) >> 4; cz <= (center.getZ() + reach) >> 4; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) continue;
				for (ServerPlayer viewer : level.getChunkSource().chunkMap.getPlayers(chunk.getPos(), false)) show(viewer, level, chunk, within);
			}
		}
	}

	/** {@code viewer} shown again what they may see within {@code veil} (they came to see through it, or stopped). */
	static void refreshFor(ServerPlayer viewer, ServerLevel level, Veil veil) {
		Predicate<BlockPos> within = veil::encloses;
		int view = level.getServer().getPlayerList().getViewDistance() + 1;
		int px = viewer.chunkPosition().x, pz = viewer.chunkPosition().z;
		for (int cx = (veil.center.getX() - veil.radius) >> 4; cx <= (veil.center.getX() + veil.radius) >> 4; cx++) {
			for (int cz = (veil.center.getZ() - veil.radius) >> 4; cz <= (veil.center.getZ() + veil.radius) >> 4; cz++) {
				if (Math.abs(cx - px) > view || Math.abs(cz - pz) > view) continue;
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk != null) show(viewer, level, chunk, within);
			}
		}
	}

	/** A sound or a block's event at (x, y, z) (PlayerList#broadcast): within a veil, sent only to those it doesn't hide from. Whether handled. */
	public static boolean broadcast(MinecraftServer server, Player except, double x, double y, double z, double radius, ResourceKey<Level> dimension,
			Packet<?> packet) {
		Veil[] veils = RAISED.getOrDefault(dimension, NONE);
		if (veils.length == 0 || !enclosedByAny(veils, x, y, z)) return false;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player == except || player.level().dimension() != dimension) continue;
			double dx = x - player.getX(), dy = y - player.getY(), dz = z - player.getZ();
			if (dx * dx + dy * dy + dz * dz < radius * radius && hiding(player.level(), player, x, y, z) == null) player.connection.send(packet);
		}
		return true;
	}

	/** A block breaking's cracks at {@code pos} (ServerLevel#destroyBlockProgress): within a veil, shown only to those it doesn't hide from. */
	public static boolean cracks(ServerLevel level, int breaker, BlockPos pos, int progress) {
		Veil[] veils = raised(level);
		if (veils.length == 0 || !enclosedByAny(veils, pos)) return false;
		for (ServerPlayer player : level.players()) {
			if (player.getId() == breaker) continue;
			double dx = pos.getX() - player.getX(), dy = pos.getY() - player.getY(), dz = pos.getZ() - player.getZ();
			if (dx * dx + dy * dy + dz * dz < 1024.0 && hiding(level, player, pos) == null) {
				player.connection.send(new ClientboundBlockDestructionPacket(breaker, pos, progress));
			}
		}
		return true;
	}

	// --- Each second ---

	/**
	 * Once a second: those near each raised veil weighed again (one grown stronger than its owner, trusted, struck off), and anyone whose
	 * seeing changed shown it again; anyone whose standing among the veils changed (in or out of one, seeing through one or not) re-tracked,
	 * so the players a veil hides go from or come back to the eyes of others.
	 */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) return;
		for (ServerLevel level : server.getAllLevels()) {
			Veil[] veils = raised(level);
			for (ServerPlayer player : level.players()) {
				long standing = 0;
				for (Veil veil : veils) {
					if (!near(veil, player, server)) {
						veil.seers.remove(player.getUUID());
						continue;
					}
					boolean sees = weigh(veil, player);
					Boolean was = veil.seers.put(player.getUUID(), sees);
					if (was != null && was != sees) refreshFor(player, level, veil);
					boolean inside = veil.encloses(player.getX(), player.getY() + 0.5, player.getZ());
					standing = standing * 31 + veil.id.hashCode() * 4L + (sees ? 1 : 0) + (inside ? 2 : 0);
				}
				Long before = STANDING.put(player.getUUID(), standing);
				if (before == null ? standing != 0 : before != standing) level.getChunkSource().move(player);
			}
		}
	}

	/** Within sight of {@code veil}: its radius and the view distance. */
	private static boolean near(Veil veil, ServerPlayer player, MinecraftServer server) {
		double reach = veil.radius + (server.getPlayerList().getViewDistance() + 2) * 16.0;
		double dx = player.getX() - veil.center.getX(), dz = player.getZ() - veil.center.getZ();
		return dx * dx + dz * dz <= reach * reach;
	}

	private Concealments() {}
}
