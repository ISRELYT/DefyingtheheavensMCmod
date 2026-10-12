package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * What cultivators do while nobody has their land loaded. Cultivation catches up whenever a cultivator is next loaded, however
 * long it was away (CultivatorNpc#catchUp), but the steps of a life need it in the world: a sect member who reaches Heavenly
 * Being leaves its sect, a rogue who does goes into seclusion, a secluded one at Grand Perfection sets off to ascend. So when
 * a cultivator's land unloads, the ledger reckons, the same way the catch-up will, when its next step falls due; when it does,
 * its land is loaded for a few seconds ({@link #WAKE}): it catches up, takes the step, and any journey it sets out on goes on
 * unseen (NpcTravel). One held at Qi Refining Mid's gate without the heavens' leave is woken every few days to ask again (its
 * catch-up asks once for each day away).
 * <p>
 * Kept for every lower-realm cultivator with a step ahead whose land is unloaded (mortals and the Upper Realm's have none);
 * dropped when it is loaded again, dies, or never turns up when woken. Saved with the Overworld; the ones still loaded when
 * the server stops are entered then ({@link #enterLoaded}), since their land unloads only after the save.
 */
public class NpcLedger extends SavedData {
	private static final String NAME = DefyingTheHeavens.MOD_ID + "_npc_ledger";
	/** A chunk held loaded this long (ticks) to wake a cultivator in it, or to let a traveller arrive there. */
	public static final TicketType<ChunkPos> WAKE = TicketType.create("defying_the_heavens_wake", Comparator.comparingLong(ChunkPos::toLong), 300);
	/** Held at the gate without leave: woken this often (3 days) to ask the heavens again. */
	static final long GATE_RETRY = 3 * HeavenRitual.DAY;
	/** At most this many cultivators woken every 5 seconds. */
	private static final int WAKES_PER_CHECK = 4;
	/** One woken that hasn't turned up after this long (ticks) is given up as lost. */
	private static final long NO_SHOW = 600;

	private record Entry(ResourceKey<Level> dimension, BlockPos pos, long dueAt) {}

	private final Map<UUID, Entry> entries = new HashMap<>();
	/** When each entry was woken (not saved: after a restart it is simply woken again). */
	private final Map<UUID, Long> woken = new HashMap<>();

	public static NpcLedger get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(NpcLedger::load, NpcLedger::new, NAME);
	}

	/** Its land unloading: entered with its next step's time. Gone for good (death, a journey, another dimension): dropped. */
	public static void onUnload(CultivatorNpc npc) {
		if (!(npc.level() instanceof ServerLevel level)) return;
		Entity.RemovalReason reason = npc.getRemovalReason();
		// Land unloading shows no reason yet (the entity stops being tracked before it is marked removed).
		enter(level, npc, reason == null || reason.shouldSave());
	}

	/** Loaded again: it lives in the world now. */
	public static void onLoad(CultivatorNpc npc) {
		if (!(npc.level() instanceof ServerLevel level)) return;
		NpcLedger ledger = get(level.getServer());
		ledger.woken.remove(npc.getUUID());
		if (ledger.entries.remove(npc.getUUID()) != null) ledger.setDirty();
	}

	/** The server stopping: everyone still loaded is entered now, before the save (their land unloads after it). */
	public static void enterLoaded(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			for (CultivatorNpc npc : CultivatorNpc.loaded(level)) enter(level, npc, true);
		}
	}

	private static void enter(ServerLevel level, CultivatorNpc npc, boolean staying) {
		NpcLedger ledger = get(level.getServer());
		ledger.woken.remove(npc.getUUID());
		Long due = staying ? dueAt(npc, level) : null;
		if (due == null) {
			if (ledger.entries.remove(npc.getUUID()) != null) ledger.setDirty();
			return;
		}
		ledger.entries.put(npc.getUUID(), new Entry(level.dimension(), npc.blockPosition(), due));
		ledger.setDirty();
	}

	/**
	 * When {@code npc}'s next step falls due (game time), or null if it has none ahead: one mid-journey (frozen when its land
	 * unloaded) at once; a sect member or rogue when it reaches Heavenly Being; a secluded one at Grand Perfection; one held at
	 * the gate in {@link #GATE_RETRY}, or when it reaches the gate if that's later.
	 */
	static Long dueAt(CultivatorNpc npc, ServerLevel level) {
		if (npc.isMortal()) return null;
		// Mid-journey (frozen when its land unloaded), looking for seclusion, or just come up with its course to choose, in any
		// realm (in the Upper Realm, one going to join a sect): at once.
		CultivatorNpc.Lifecycle life = npc.getLifecycle();
		if (life == CultivatorNpc.Lifecycle.DEPARTING || life == CultivatorNpc.Lifecycle.ASCENDING || life == CultivatorNpc.Lifecycle.SEEKING_SECLUSION
				|| npc.isNewlyAscended()) {
			return level.getGameTime();
		}
		if (!ModDimensions.isLowerRealm(level.dimension())) return null;
		long seen = npc.lastSeen() < 0 ? level.getGameTime() : npc.lastSeen();
		int target;
		switch (life) {
			case SECT, ROGUE -> target = PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY);
			case SECLUDED -> target = PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION);
			default -> {
				return null;
			}
		}
		PlayerCultivation c = npc.cultivation();
		boolean[] gate = new boolean[1];
		double seconds = secondsAway(npc.getRank(), c.getCultivation(), c.hasHeavensLeave(), target, gate);
		long away = Math.max(CultivatorNpc.MIN_CATCH_UP, (long) Math.ceil(seconds * 20) + 20);
		if (gate[0]) away = Math.max(away, GATE_RETRY);
		return seen + away;
	}

	/**
	 * Seconds away it takes a lower-realm cultivator at {@code rank}, {@code progress} into it, to reach {@code target}, as
	 * the catch-up counts them (a share of each second meditating, each stage at its own rate). If the way passes Qi Refining
	 * Mid's gate without the heavens' leave, {@code heldAtGate[0]} is set and the time is only as far as the gate.
	 */
	static double secondsAway(int rank, double progress, boolean leave, int target, boolean[] heldAtGate) {
		PlayerCultivation scratch = new PlayerCultivation();
		double seconds = 0;
		for (int r = Math.max(0, rank); r < target; r++) {
			scratch.setState(CultivatorNpc.realmOf(r), CultivatorNpc.stageOf(r), 0);
			double rate = scratch.cultivationPerSecond() * CultivatorNpc.NPC_RATE * CultivatorNpc.CATCH_UP_SHARE;
			double need = scratch.cultivationRequired() - (r == rank ? progress : 0);
			seconds += Math.max(0, need) / rate;
			if (r == PlayerCultivation.RITUAL_RANK && !leave) {
				heldAtGate[0] = true;
				return seconds;
			}
		}
		return seconds;
	}

	/**
	 * Loads the land around {@code pos} for a few seconds, entities ticking in its middle chunk. A level with no player in it
	 * stops ticking its entities 15 seconds after the last one leaves (loaded land or not), so its level is kept awake for as
	 * long ({@link #AWAKE}): the Upper Realm with everyone below, the Overworld with everyone above.
	 */
	public static void wake(ServerLevel level, BlockPos pos) {
		ChunkPos chunk = new ChunkPos(pos);
		level.getChunkSource().addRegionTicket(WAKE, chunk, 2, chunk);
		AWAKE.put(level.dimension(), level.getGameTime() + WAKE_TICKS);
		level.resetEmptyTime();
	}

	/** Levels with land woken just now, and until when (not saved). */
	private static final Map<ResourceKey<Level>, Long> AWAKE = new HashMap<>();
	private static final long WAKE_TICKS = 320;

	public static void clear() {
		AWAKE.clear();
	}

	/**
	 * Every second, levels with woken land are kept ticking; every 5 seconds, those whose step is due are woken (a few at a
	 * time), and any woken that never turned up are dropped.
	 */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 == 0 && !AWAKE.isEmpty()) {
			long now = server.overworld().getGameTime();
			AWAKE.values().removeIf(until -> until < now);
			for (ResourceKey<Level> dimension : AWAKE.keySet()) {
				ServerLevel level = server.getLevel(dimension);
				if (level != null) level.resetEmptyTime();
			}
		}
		if (server.getTickCount() % 100 != 75) return;
		NpcLedger ledger = get(server);
		if (ledger.entries.isEmpty()) return;
		long now = server.overworld().getGameTime();
		int wakes = 0;
		for (Iterator<Map.Entry<UUID, Entry>> it = ledger.entries.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Entry> e = it.next();
			if (e.getValue().dueAt() > now) continue;
			Long since = ledger.woken.get(e.getKey());
			if (since != null) {
				if (now - since > NO_SHOW) {
					it.remove();
					ledger.woken.remove(e.getKey());
					ledger.setDirty();
				}
				continue;
			}
			ServerLevel level = server.getLevel(e.getValue().dimension());
			if (level == null) {
				it.remove();
				ledger.setDirty();
				continue;
			}
			if (wakes >= WAKES_PER_CHECK) continue;
			wake(level, e.getValue().pos());
			ledger.woken.put(e.getKey(), now);
			wakes++;
		}
	}

	/** How many cultivators the ledger holds (for tests). */
	public static int size(MinecraftServer server) {
		return get(server).entries.size();
	}

	/** When the ledger expects {@code id}'s next step (game time), or -1 if it isn't in it (for tests). */
	public static long dueAt(MinecraftServer server, UUID id) {
		Entry entry = get(server).entries.get(id);
		return entry == null ? -1 : entry.dueAt();
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Map.Entry<UUID, Entry> e : entries.entrySet()) {
			CompoundTag n = new CompoundTag();
			n.putUUID("Id", e.getKey());
			n.putString("Dimension", e.getValue().dimension().location().toString());
			n.putLong("Pos", e.getValue().pos().asLong());
			n.putLong("DueAt", e.getValue().dueAt());
			list.add(n);
		}
		tag.put("Cultivators", list);
		return tag;
	}

	public static NpcLedger load(CompoundTag tag) {
		NpcLedger ledger = new NpcLedger();
		ListTag list = tag.getList("Cultivators", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag n = list.getCompound(i);
			ResourceLocation dimension = ResourceLocation.tryParse(n.getString("Dimension"));
			if (dimension == null || !n.hasUUID("Id")) continue;
			ledger.entries.put(n.getUUID("Id"), new Entry(ResourceKey.create(Registries.DIMENSION, dimension), BlockPos.of(n.getLong("Pos")),
					n.getLong("DueAt")));
		}
		return ledger;
	}
}
