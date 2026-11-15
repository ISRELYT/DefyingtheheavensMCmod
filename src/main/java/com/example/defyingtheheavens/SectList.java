package com.example.defyingtheheavens;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Sect List (the user's "Build as designed", 2026-11-13): each world's {@link #TOP} strongest sects, beside its list of cultivators
 * (Rankings) on the Ranking Stele and the Journal's Lists page (RankingView's Sects tab). A sect's score ({@link #reckon}): the strength of
 * its {@link #STRONGEST} strongest summed (each one's rank and one: PlayerCultivation#rank, its cultivators by their rolls, SectManager's;
 * its players, a player's sect's founder among them, by their own record online or their Lists entry), and for each of its people on its
 * world's list {@link #placeWorth} (twenty for the first place, a fifth for the hundredth). Player sects (PlayerSects) stand on it too, so
 * one's own climbs as its disciples grow.
 * <ul>
 *   <li>A member of a top-ten sect is thought the better of by its world's cultivators ({@link #regardBonus}: CultivatorNpc#regard).</li>
 *   <li>A top-three place changing hands, a sect rising to it, is talked of round the world for a few days (Rumours#noteSectRanking:
 *   {@link #tick} looks each minute).</li>
 * </ul>
 */
public final class SectList {
	/** Each world's list's length, and how many of a sect's people its strength is summed from. */
	public static final int TOP = 10, STRONGEST = 10;
	/** What the first place on a world's list adds to its sect's score (the hundredth a hundredth of it, and so on). */
	public static final double PLACE_WORTH = 20;
	/** What a top-ten sect's member is worth with its world's cultivators (CultivatorNpc#regard). */
	public static final int TOP_REGARD = 5;
	/** The lists are reckoned afresh at most this often (ticks). */
	private static final int FRESH = 20;

	/** A sect as it stands: its id and name, its score, its people, its strongest's rank, its path, and whether a player founded it. */
	public record Standing(UUID id, String name, double score, int people, int best, int faction, boolean players) {}

	private static final Map<Rankings.Board, List<Standing>> LISTS = new EnumMap<>(Rankings.Board.class);
	private static long reckonedAt = -1;
	/** Each list's top three as last looked at ({@link #tick}), for the tales of a place changing hands; none before the first look. */
	private static final Map<Rankings.Board, List<Standing>> LAST_THREE = new EnumMap<>(Rankings.Board.class);

	/** {@code board}'s Sect List: its {@link #TOP}, strongest first. */
	public static List<Standing> top(MinecraftServer server, Rankings.Board board) {
		long now = server.overworld().getGameTime();
		if (reckonedAt < 0 || now < reckonedAt || now - reckonedAt >= FRESH) {
			LISTS.clear();
			LISTS.putAll(reckon(server));
			reckonedAt = now;
		}
		return LISTS.getOrDefault(board, List.of());
	}

	/** The place of the sect {@code sectId} on {@code board}'s Sect List (1 the first), or 0: not among its ten. */
	public static int placeOf(MinecraftServer server, Rankings.Board board, UUID sectId) {
		List<Standing> list = top(server, board);
		for (int i = 0; i < list.size(); i++) if (list.get(i).id().equals(sectId)) return i + 1;
		return 0;
	}

	/** What the {@code place}th place on a world's list adds to its sect's score: {@link #PLACE_WORTH} for the first, a hundredth of it less each place down. */
	public static double placeWorth(int place) {
		if (place < 1 || place > Rankings.TOP) return 0;
		return PLACE_WORTH * (Rankings.TOP + 1 - place) / Rankings.TOP;
	}

	/** Every world's Sect List as it stands now. */
	static Map<Rankings.Board, List<Standing>> reckon(MinecraftServer server) {
		Rankings rankings = Rankings.get(server);
		Map<Rankings.Board, List<Standing>> lists = new EnumMap<>(Rankings.Board.class);
		for (Sect sect : SectData.get(server).all()) {
			if (sect.extinct) continue;
			Rankings.Board board = Rankings.Board.of(sect.dimension);
			if (board == null) continue;
			lists.computeIfAbsent(board, b -> new ArrayList<>()).add(standing(server, rankings, board, sect));
		}
		Comparator<Standing> order = Comparator.comparingDouble(Standing::score).reversed().thenComparing(Standing::name);
		for (Map.Entry<Rankings.Board, List<Standing>> list : lists.entrySet()) {
			list.getValue().sort(order);
			if (list.getValue().size() > TOP) list.setValue(new ArrayList<>(list.getValue().subList(0, TOP)));
		}
		return lists;
	}

	/** {@code sect} as it stands on {@code board}: its strongest summed, its people's places on the world's list added. */
	static Standing standing(MinecraftServer server, Rankings rankings, Rankings.Board board, Sect sect) {
		List<Integer> ranks = new ArrayList<>();
		Set<UUID> people = new HashSet<>();
		for (Sect.Member member : sect.members.values()) {
			people.add(member.id);
			if (member.rank >= 0) ranks.add(member.rank);
		}
		List<UUID> players = new ArrayList<>(sect.disciples.keySet());
		if (sect.founder != null && !players.contains(sect.founder)) players.add(sect.founder);
		for (UUID player : players) {
			if (!people.add(player)) continue;
			int rank = playerRank(server, rankings, player);
			if (rank >= 0) ranks.add(rank);
		}
		ranks.sort(Comparator.reverseOrder());
		double score = 0;
		for (int i = 0; i < Math.min(STRONGEST, ranks.size()); i++) score += ranks.get(i) + 1;
		List<Rankings.Entry> listed = rankings.top(board);
		for (int i = 0; i < listed.size(); i++) if (people.contains(listed.get(i).id)) score += placeWorth(i + 1);
		return new Standing(sect.id, sect.name, score, people.size(), ranks.isEmpty() ? -1 : ranks.get(0), Alignment.factionOf(sect.alignment).ordinal(),
				sect.isPlayers());
	}

	/** A player's strength (the higher of their paths, the body's too, as their Lists entry has it): their own record online, else their entry; -1 a mortal or unknown. */
	static int playerRank(MinecraftServer server, Rankings rankings, UUID id) {
		ServerPlayer online = server.getPlayerList().getPlayer(id);
		if (online != null) {
			PlayerCultivation c = CultivationManager.own(online);
			return c.isMortal() ? -1 : Math.max(TwoPaths.higherRank(c, false), BodyPath.rank(c));
		}
		Rankings.Entry entry = rankings.entry(id);
		return entry == null ? -1 : entry.rank;
	}

	/** The sect {@code player} belongs to that still stands: a disciple of it, or the one who founded it; or null. */
	public static Sect sectOf(MinecraftServer server, UUID player) {
		Sect sect = SectMembership.liveSectOf(server, player);
		if (sect != null) return sect;
		for (Sect s : SectData.get(server).all()) if (!s.extinct && player.equals(s.founder)) return s;
		return null;
	}

	/** What {@code player}'s sect is worth with {@code npc} (CultivatorNpc#regard): {@link #TOP_REGARD} if it stands among the ten of the cultivator's world. */
	public static int regardBonus(ServerPlayer player, CultivatorNpc npc) {
		Rankings.Board board = Rankings.Board.of(npc.level().dimension());
		if (board == null) return 0;
		Sect sect = sectOf(player.server, player.getUUID());
		return sect != null && placeOf(player.server, board, sect.id) > 0 ? TOP_REGARD : 0;
	}

	/**
	 * Once a minute: each list's top three looked at against the last look; a sect risen into one of them (from below it, or from off the
	 * list), taking it from another, is talked of (Rumours#noteSectRanking). The first look after the server starts only notes them.
	 */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 1200 != 611) return;
		look(server);
	}

	/** One look ({@link #tick}): the lists reckoned afresh and their top threes against the last look. Returns how many tales it set going. */
	static int look(MinecraftServer server) {
		reckonedAt = -1; // afresh
		long now = server.overworld().getGameTime();
		int told = 0;
		for (Rankings.Board board : Rankings.Board.values()) {
			List<Standing> list = top(server, board);
			List<Standing> three = new ArrayList<>(list.subList(0, Math.min(3, list.size())));
			List<Standing> before = LAST_THREE.put(board, three);
			if (before == null) continue;
			for (int place = 1; place <= three.size(); place++) {
				Standing holder = three.get(place - 1);
				Standing was = place <= before.size() ? before.get(place - 1) : null;
				if (was == null || was.id().equals(holder.id())) continue;
				int formerly = -1;
				for (int i = 0; i < before.size(); i++) if (before.get(i).id().equals(holder.id())) formerly = i + 1;
				if (formerly != -1 && formerly < place) continue; // it fell to this place: no tale in that
				Rumours.noteSectRanking(board, place, holder.name(), was.name(), now);
				told++;
			}
		}
		return told;
	}

	/** Reckoned afresh at the next asking (a test's sects set up; the server stopping). */
	public static void clear() {
		LISTS.clear();
		LAST_THREE.clear();
		reckonedAt = -1;
	}

	private SectList() {}
}
