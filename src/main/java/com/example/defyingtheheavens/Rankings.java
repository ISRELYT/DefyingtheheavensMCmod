package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Earth and Heaven Lists (the user's "Build as designed", 2026-11-10): one list a world ({@link Board}), its hundred strongest cultivators,
 * the world's people and its players together. A cultivator is on the list of the world it lives in; a player on that of the highest world
 * they have ascended to (a True Immortal visiting below never tops the Earth List), the dead on the Netherworld's.
 * <p>
 * <b>Order:</b> strength first (realm and stage: PlayerCultivation#rank, then the stage's progress), and places won: beat one ranked above you
 * (a spar won, or a kill) and you go just above them ({@link #won}: the gap made up as glory, kept); they pass you again only by a breakthrough
 * or by beating you back. Cultivators' own bouts count too (NpcLife).
 * <p>
 * Kept for every cultivator ever noted ({@link #note}: as it loads, grows or dies; players every few seconds), unloaded ones as last seen; the
 * sects' rolls are read in the first time and every day after (their members' ranks kept by SectManager). Forgotten after
 * {@link #FORGOTTEN} unseen (unless a sect's member or a player). Shown on the Ranking Stele (RankingSteleBlock) and the Journal tab's second
 * page; NPCs think a little better of the listed and greet them by their place (TalkSystem); a top-ten place changing hands is talked of
 * (Rumours). Saved with the Overworld.
 */
public final class Rankings extends SavedData {
	public static final String NAME = DefyingTheHeavens.MOD_ID + "_rankings";
	/** Each list's length, and how many of each world are kept to fill it. */
	public static final int TOP = 100, KEPT = 300;
	/** One unseen this long (ticks: a month of days) is forgotten, unless a sect's member or a player. */
	public static final long FORGOTTEN = 30 * 24_000L;
	/** What a place on its world's list is worth with that world's cultivators (CultivatorNpc#regard): a top-ten's, any listed one's. */
	public static final int TOP_TEN_REGARD = 10, LISTED_REGARD = 5;
	/** A place won puts the winner this far above the one beaten. */
	public static final double EDGE = 0.01;

	/** The lists, one a world: the lower realms', the Jade Firmament's, the Great Void's, the Immortal Realm's, the Netherworld's. */
	public enum Board {
		EARTH("earth"), HEAVEN("heaven"), VOID("void"), IMMORTAL("immortal"), NETHERWORLD("netherworld");

		private final String id;

		Board(String id) {
			this.id = id;
		}

		public String getId() { return id; }

		public Component getDisplayName() { return Component.translatable(ModLang.rankingBoard(id)); }

		public static Board byIndex(int index) {
			return values()[Math.max(0, Math.min(values().length - 1, index))];
		}

		/** The world its tales go round (Rumours). */
		public ResourceKey<Level> home() {
			return switch (this) {
				case EARTH -> Level.OVERWORLD;
				case HEAVEN -> ModDimensions.UPPER_REALM;
				case VOID -> ModDimensions.GREAT_VOID;
				case IMMORTAL -> ModDimensions.IMMORTAL_REALM;
				case NETHERWORLD -> ModDimensions.NETHERWORLD;
			};
		}

		/** The list of the world {@code dimension} is, or null for none of its own (the ways between, the secret realms, the Inner Realm). */
		public static Board of(ResourceKey<Level> dimension) {
			if (ModDimensions.isNetherworld(dimension)) return NETHERWORLD;
			if (ModDimensions.isImmortalRealm(dimension) || ModDimensions.isSmallWorld(dimension)) return IMMORTAL;
			if (ModDimensions.isGreatVoid(dimension)) return VOID;
			if (ModDimensions.isUpperRealm(dimension)) return HEAVEN;
			if (ModDimensions.isLowerRealm(dimension) || ModDimensions.isAstral(dimension)) return EARTH;
			return null;
		}
	}

	/** One cultivator as last seen. */
	public static final class Entry {
		public final UUID id;
		public String name = "", sect = "";
		public boolean player;
		public Board board = Board.EARTH;
		/** A player's living world's list (the highest they have ascended to), kept while they are dead. */
		public Board living = Board.EARTH;
		public int rank, title = -1, faction;
		public double progress, glory;
		public long seen;

		Entry(UUID id) {
			this.id = id;
		}

		/** Its standing: its rank, and the places it has won. */
		public double score() { return rank + glory; }

		CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putUUID("Id", id);
			tag.putString("Name", name);
			tag.putString("Sect", sect);
			tag.putBoolean("Player", player);
			tag.putInt("Board", board.ordinal());
			tag.putInt("Living", living.ordinal());
			tag.putInt("Rank", rank);
			tag.putInt("Title", title);
			tag.putInt("Faction", faction);
			tag.putDouble("Progress", progress);
			tag.putDouble("Glory", glory);
			tag.putLong("Seen", seen);
			return tag;
		}

		static Entry load(CompoundTag tag) {
			Entry e = new Entry(tag.getUUID("Id"));
			e.name = tag.getString("Name");
			e.sect = tag.getString("Sect");
			e.player = tag.getBoolean("Player");
			e.board = Board.byIndex(tag.getInt("Board"));
			e.living = Board.byIndex(tag.getInt("Living"));
			e.rank = tag.getInt("Rank");
			e.title = tag.getInt("Title");
			e.faction = tag.getInt("Faction");
			e.progress = tag.getDouble("Progress");
			e.glory = tag.getDouble("Glory");
			e.seen = tag.getLong("Seen");
			return e;
		}
	}

	/** Its order: standing, then the stage's progress, then the name. */
	public static final Comparator<Entry> ORDER = Comparator.comparingDouble(Entry::score).reversed()
			.thenComparing(Comparator.comparingDouble((Entry e) -> e.progress).reversed()).thenComparing(e -> e.name);

	private final Map<UUID, Entry> entries = new HashMap<>();
	/** The sects' rolls read in (the first time, and each day after: {@link #tick}). */
	private long rollsReadDay = -1;
	/** The lists as last sorted, and whether anything has changed since. */
	private final Map<Board, List<Entry>> sorted = new EnumMap<>(Board.class);
	private boolean stale = true;

	public static Rankings get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Rankings::load, Rankings::new, NAME);
	}

	public static Rankings load(CompoundTag tag) {
		Rankings data = new Rankings();
		ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			Entry e = Entry.load(list.getCompound(i));
			data.entries.put(e.id, e);
		}
		data.rollsReadDay = tag.contains("RollsRead") ? tag.getLong("RollsRead") : -1;
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Entry e : entries.values()) list.add(e.save());
		tag.put("Entries", list);
		tag.putLong("RollsRead", rollsReadDay);
		return tag;
	}

	private void changed() {
		stale = true;
		setDirty();
	}

	/** {@code board}'s list: its {@link #TOP} first, in order. */
	public List<Entry> top(Board board) {
		if (stale) {
			sorted.clear();
			Map<Board, List<Entry>> all = new EnumMap<>(Board.class);
			for (Entry e : entries.values()) all.computeIfAbsent(e.board, b -> new ArrayList<>()).add(e);
			for (Map.Entry<Board, List<Entry>> list : all.entrySet()) {
				list.getValue().sort(ORDER);
				sorted.put(list.getKey(), list.getValue());
			}
			stale = false;
		}
		List<Entry> list = sorted.getOrDefault(board, List.of());
		return list.size() > TOP ? list.subList(0, TOP) : list;
	}

	/** {@code id}'s place on its list (1 the first), or 0 when it isn't on it (not known, or past the hundredth). */
	public int placeOf(UUID id) {
		Entry e = entries.get(id);
		if (e == null) return 0;
		List<Entry> list = top(e.board);
		for (int i = 0; i < list.size(); i++) if (list.get(i).id.equals(id)) return i + 1;
		return 0;
	}

	public Entry entry(UUID id) { return entries.get(id); }

	// --- Noting who is where, and how strong ---

	/** {@code npc} as it is now (loaded, grown, joined): not a mortal, a reflection, or one with no world of its own just now. */
	public static void note(CultivatorNpc npc) {
		if (!(npc.level() instanceof ServerLevel level) || npc.isRemoved() || npc.isMortal() || !npc.livesItsLife()) return;
		Rankings data = get(level.getServer());
		Board board = Board.of(level.dimension());
		Entry e = data.entries.get(npc.getUUID());
		if (e == null) {
			if (board == null) return;
			e = new Entry(npc.getUUID());
			data.entries.put(e.id, e);
		}
		if (board != null) e.board = board;
		e.player = false;
		e.name = npc.getPersonalName();
		e.rank = npc.getRank();
		PlayerCultivation c = npc.cultivation();
		e.progress = c == null || c.cultivationRequired() <= 0 ? 0 : Math.min(1, c.getCultivation() / c.cultivationRequired());
		Sect sect = npc.getSectId() == null ? null : SectManager.get(level.getServer(), npc.getSectId());
		e.sect = sect == null || sect.extinct ? "" : sect.name;
		e.title = npc.getTitle() == null ? -1 : npc.getTitle().ordinal();
		e.faction = npc.getFaction().ordinal();
		e.seen = level.getGameTime();
		data.changed();
	}

	/**
	 * {@code p} as they are now: their strength (the higher of their paths, the body's too), on the list of the highest world they have
	 * ascended to (a realm past the lower realms' cap means the Jade Firmament at least, a True Immortal's the Immortal Realm), the
	 * Netherworld's while they are dead. A mortal is on no list.
	 */
	public static void note(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.own(p);
		Rankings data = get(p.server);
		if (c.isMortal()) {
			if (data.entries.remove(p.getUUID()) != null) data.changed();
			return;
		}
		Entry e = data.entries.computeIfAbsent(p.getUUID(), Entry::new);
		int rank = Math.max(TwoPaths.higherRank(c, false), BodyPath.rank(c));
		Board here = Board.of(p.level().dimension());
		Board living = e.player ? e.living : Board.EARTH;
		if (here != null && here != Board.NETHERWORLD && here.ordinal() > living.ordinal()) living = here;
		if (rank >= PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.EARLY) && living.ordinal() < Board.HEAVEN.ordinal()) living = Board.HEAVEN;
		if (rank >= PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY)) living = Board.IMMORTAL;
		Sect sect = SectMembership.sectOf(p.server, p.getUUID());
		int title = sect == null ? -1 : SectMembership.titleOf(sect, p).ordinal();
		double progress = c.cultivationRequired() <= 0 ? 0 : Math.min(1, c.getCultivation() / c.cultivationRequired());
		Board board = c.netherworld().ghost() ? Board.NETHERWORLD : living;
		String sectName = sect == null ? "" : sect.name;
		boolean same = e.player && e.rank == rank && e.board == board && e.living == living && e.title == title && e.sect.equals(sectName)
				&& Math.abs(e.progress - progress) < 0.01 && e.name.equals(p.getGameProfile().getName());
		e.player = true;
		e.name = p.getGameProfile().getName();
		e.rank = rank;
		e.living = living;
		e.board = board;
		e.title = title;
		e.sect = sectName;
		e.progress = progress;
		e.faction = Alignment.factionOf(c.getAlignment()).ordinal();
		e.seen = p.level().getGameTime();
		if (!same) data.changed();
		else data.setDirty();
	}

	/**
	 * {@code winner} beat {@code loser} (a spar won, a kill, a cultivators' bout): if the loser stood above the winner on the same list, the
	 * winner goes just above them, the gap made up as glory. A top-ten place won is talked of (Rumours). Returns whether a place changed hands.
	 */
	public static boolean won(MinecraftServer server, UUID winner, UUID loser) {
		Rankings data = get(server);
		Entry w = data.entries.get(winner), l = data.entries.get(loser);
		if (w == null || l == null || w == l || w.board != l.board || ORDER.compare(w, l) < 0) return false;
		int place = data.placeOf(loser);
		w.glory = Math.max(w.glory, l.score() - w.rank + EDGE);
		data.changed();
		if (place >= 1 && place <= 10) Rumours.noteRanking(w.board, place, w.name, l.name, server.overworld().getGameTime());
		return true;
	}

	/** {@code dead} was slain (by {@code killer}, if anyone): a kill wins its place; a cultivator slain is gone from the lists. */
	public static void slain(LivingEntity dead, LivingEntity killer) {
		if (!(dead.level() instanceof ServerLevel level)) return;
		if (killer != null && killer != dead) won(level.getServer(), killer.getUUID(), dead.getUUID());
		if (dead instanceof CultivatorNpc) {
			Rankings data = get(level.getServer());
			if (data.entries.remove(dead.getUUID()) != null) data.changed();
		}
	}

	// --- Kept up ---

	/**
	 * Every few seconds the players online noted; each day the sects' rolls read in (their members' ranks, as SectManager keeps them, for those
	 * not loaded), the long unseen forgotten, and each list trimmed to {@link #KEPT}.
	 */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 100 != 37) return;
		for (ServerPlayer p : server.getPlayerList().getPlayers()) note(p);
		Rankings data = get(server);
		long now = server.overworld().getGameTime(), day = now / 24_000L;
		if (data.rollsReadDay == day) return;
		data.rollsReadDay = day;
		java.util.Set<UUID> members = new java.util.HashSet<>();
		for (Sect sect : SectData.get(server).all()) {
			if (sect.extinct) continue;
			Board board = Board.of(sect.dimension);
			if (board == null) continue;
			for (Sect.Member member : sect.members.values()) {
				members.add(member.id);
				Entry e = data.entries.get(member.id);
				if (e == null) {
					e = new Entry(member.id);
					e.name = NpcNames.of(member.id);
					e.board = board;
					e.faction = Alignment.factionOf(sect.alignment).ordinal();
					e.seen = now;
					data.entries.put(e.id, e);
				}
				if (e.player) continue;
				e.rank = member.rank;
				e.sect = sect.name;
				e.title = member.title.ordinal();
			}
		}
		data.entries.values().removeIf(e -> !e.player && !members.contains(e.id) && now - e.seen > FORGOTTEN);
		Map<Board, List<Entry>> all = new EnumMap<>(Board.class);
		for (Entry e : data.entries.values()) all.computeIfAbsent(e.board, b -> new ArrayList<>()).add(e);
		for (List<Entry> list : all.values()) {
			if (list.size() <= KEPT) continue;
			list.sort(ORDER);
			for (Entry e : list.subList(KEPT, list.size())) if (!e.player) data.entries.remove(e.id);
		}
		data.changed();
	}

	/** What {@code player}'s place on {@code npc}'s world's list is worth with it (CultivatorNpc#regard): nothing off its list. */
	public static int regardBonus(ServerPlayer player, CultivatorNpc npc) {
		Board board = Board.of(npc.level().dimension());
		Rankings data = get(player.server);
		Entry e = data.entries.get(player.getUUID());
		if (board == null || e == null || e.board != board) return 0;
		int place = data.placeOf(player.getUUID());
		return place == 0 ? 0 : place <= 10 ? TOP_TEN_REGARD : LISTED_REGARD;
	}

	/** {@code player}'s place on {@code npc}'s world's list (1 the first), or 0: not on it. */
	public static int placeOn(ServerPlayer player, CultivatorNpc npc) {
		Board board = Board.of(npc.level().dimension());
		Rankings data = get(player.server);
		Entry e = data.entries.get(player.getUUID());
		return board == null || e == null || e.board != board ? 0 : data.placeOf(player.getUUID());
	}

	/** Forgotten with the world (tests). */
	void forget(UUID id) {
		if (entries.remove(id) != null) changed();
	}

	/** Everyone forgotten, the sects' rolls not read again today (the game tests, which keep their world: their lists start empty). */
	void clearAll(MinecraftServer server) {
		entries.clear();
		rollsReadDay = server.overworld().getGameTime() / 24_000L;
		changed();
	}
}
