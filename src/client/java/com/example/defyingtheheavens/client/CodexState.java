package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Codex;
import com.example.defyingtheheavens.CodexMap;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.client.mixin.ClientAdvancementsAccessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * What of the Dao Codex (Codex) is open to the player, and what they have read: the user's picks, 2026-10-27, a toast for a page that
 * opens (CodexToast) and a gold dot on each page not yet read (CodexScreen). A page opens with any one of its advancements as the
 * client knows them (the server sends a player's progress: ClientAdvancementsAccessor), so a world from before the Codex opens what its
 * player has already done the first time they come in with it, told by one toast.
 * <p>
 * The worlds of its map (CodexMap) the player has been to, for the map to name them (the user's ask, 2026-10-28): by their advancements
 * (so a save from before the map knows them), or seen here, the world the player stands in noted at every look. And the creatures its
 * Bestiary's pages are of that the player has come upon ({@link #sight}), each page opening (and toasted) once its creature is.
 * <p>
 * Kept for each world on this computer, in config/defying-the-heavens-codex.json, keyed by the singleplayer world's folder or the
 * server's address: the pages already told of (each is toasted once), the pages read, and the map's worlds seen.
 */
public final class CodexState {
	/**
	 * Ticks after coming into a world before the first look: the server sends a player's advancements in their first ticks, and its sweep
	 * (Milestones) grants what they already stand at within two seconds, so the pages a world already has come in one toast.
	 */
	private static final int FIRST_LOOK = 60;
	private static final int LOOK_EVERY = 10;
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve(DefyingTheHeavens.MOD_ID + "-codex.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** The world being played (null: none yet, or between worlds), and how long it has been. */
	private static String world;
	private static int ticks;
	private static boolean looked;
	/** The pages open now; those told of already; those read; and the last pages told of together, in the Codex's order. */
	private static final Set<String> open = new HashSet<>();
	private static final Set<String> told = new HashSet<>();
	private static final Set<String> read = new HashSet<>();
	private static final List<String> latest = new ArrayList<>();
	/** The map's worlds visited (by their advancements or seen), and those seen stood in on this computer. */
	private static final Set<String> visited = new HashSet<>();
	private static final Set<String> seen = new HashSet<>();
	/**
	 * The Bestiary's creatures come upon (entity type ids, kept on disk): one counts once it is in the player's sight within
	 * {@link #SIGHT_RANGE} blocks (the vast ones farther, by their size).
	 */
	private static final Set<String> sighted = new HashSet<>();
	private static final Set<String> SIGHTS = Codex.sights();
	private static final double SIGHT_RANGE = 32;

	/** Each client tick: a look now and then at what the advancements open. */
	public static void tick(Minecraft mc) {
		if (mc.player == null || mc.getConnection() == null) return;
		if (world == null) {
			world = worldKey(mc);
			ticks = 0;
			looked = false;
			load();
		}
		ticks++;
		if (ticks >= FIRST_LOOK && ticks % LOOK_EVERY == 0) look(mc, true);
	}

	/** The next world's pages are its own (the read marks stay on disk). */
	public static void clear() {
		world = null;
		looked = false;
		open.clear();
		told.clear();
		read.clear();
		latest.clear();
		visited.clear();
		seen.clear();
		sighted.clear();
	}

	/** A look for the Codex opened before the first one (no toast: the pages show their dots there). */
	static void lookNow(Minecraft mc) {
		if (!looked) look(mc, false);
	}

	private static void look(Minecraft mc, boolean toast) {
		if (mc.getConnection() == null || world == null) return;
		Set<String> done = new HashSet<>();
		Map<ResourceLocation, AdvancementProgress> byId = new HashMap<>();
		Map<Advancement, AdvancementProgress> progress = ((ClientAdvancementsAccessor) mc.getConnection().getAdvancements()).dth$progress();
		for (Map.Entry<Advancement, AdvancementProgress> e : progress.entrySet()) {
			byId.put(e.getKey().getId(), e.getValue());
			if (e.getValue().isDone() && e.getKey().getId().getNamespace().equals(DefyingTheHeavens.MOD_ID)) done.add(e.getKey().getId().getPath());
		}
		looked = true;
		boolean newlySighted = sight(mc);
		open.clear();
		List<Codex.Entry> news = new ArrayList<>();
		for (Codex.Entry entry : Codex.entries()) {
			boolean opens = entry.sight() != null ? sighted.contains(entry.sight()) : entry.open() || !Collections.disjoint(entry.unlocks(), done);
			if (!opens) continue;
			open.add(entry.id());
			if (told.add(entry.id())) news.add(entry);
		}
		// The map's worlds: the one stood in now noted, then each visited by its advancements or seen.
		CodexMap.World here = mc.level == null || mc.player == null ? null : CodexMap.World.at(mc.level, mc.player.getX(), mc.player.getZ());
		boolean newlySeen = here != null && seen.add(here.id());
		visited.clear();
		for (CodexMap.World world : CodexMap.World.values()) {
			if (world.always() || seen.contains(world.id()) || anyDone(world.visits(), byId)) visited.add(world.id());
		}
		if (news.isEmpty()) {
			if (newlySeen || newlySighted) save();
			return;
		}
		latest.clear();
		for (Codex.Entry entry : news) latest.add(entry.id());
		save();
		if (toast) CodexToast.show(mc.getToasts(), news);
	}

	/**
	 * Notes every Bestiary creature in the player's sight, near enough to make out: within {@link #SIGHT_RANGE} blocks (the vast ones
	 * farther, by their size), not hidden from them, with nothing in between. Only those not yet noted are looked at. Whether any was new.
	 */
	private static boolean sight(Minecraft mc) {
		if (mc.level == null || mc.player == null) return false;
		boolean added = false;
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!(entity instanceof LivingEntity) || entity == mc.player) continue;
			String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
			if (!SIGHTS.contains(id) || sighted.contains(id)) continue;
			double reach = SIGHT_RANGE + entity.getBbWidth() * 2;
			if (entity.distanceToSqr(mc.player) > reach * reach || entity.isInvisibleTo(mc.player) || !mc.player.hasLineOfSight(entity)) continue;
			added |= sighted.add(id);
		}
		return added;
	}

	/** Any one of a world's advancements done ("path#criterion": that criterion of it). */
	private static boolean anyDone(List<String> visits, Map<ResourceLocation, AdvancementProgress> byId) {
		for (String visit : visits) {
			AdvancementProgress progress = byId.get(CodexMap.advancementOf(visit));
			if (progress == null) continue;
			String criterion = CodexMap.criterionOf(visit);
			CriterionProgress part = criterion == null ? null : progress.getCriterion(criterion);
			if (criterion == null ? progress.isDone() : part != null && part.isDone()) return true;
		}
		return false;
	}

	/** Whether the player has been to a world of the Codex's map (the Overworld always). */
	public static boolean visited(CodexMap.World world) {
		return world.always() || visited.contains(world.id());
	}

	public static boolean isOpen(Codex.Entry entry) {
		return open.contains(entry.id());
	}

	public static boolean isRead(Codex.Entry entry) {
		return read.contains(entry.id());
	}

	/** Open and not read yet: the gold dot. */
	public static boolean isUnread(Codex.Entry entry) {
		return isOpen(entry) && !isRead(entry);
	}

	public static void markRead(Codex.Entry entry) {
		if (!isOpen(entry) || !read.add(entry.id())) return;
		save();
	}

	public static boolean anyOpen(Codex.Chapter chapter) {
		for (Codex.Entry entry : chapter.entries()) if (isOpen(entry)) return true;
		return false;
	}

	public static boolean anyUnread(Codex.Chapter chapter) {
		for (Codex.Entry entry : chapter.entries()) if (isUnread(entry)) return true;
		return false;
	}

	/** Anything open and unread (the book beside the cultivation menu wears the dot). */
	public static boolean anyUnread() {
		for (Codex.Entry entry : Codex.entries()) if (isUnread(entry)) return true;
		return false;
	}

	public static int openCount() {
		return open.size();
	}

	/**
	 * The first page not yet read of the last ones told of together (the Codex opens on it: the welcome for a new player, a realm's page
	 * on reaching it), or null.
	 */
	static Codex.Entry newest() {
		for (String id : latest) {
			Codex.Entry entry = Codex.entry(id);
			if (entry != null && isUnread(entry)) return entry;
		}
		return null;
	}

	// --- Kept on disk ---

	private static String worldKey(Minecraft mc) {
		IntegratedServer server = mc.getSingleplayerServer();
		if (server != null) {
			Path root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
			return "world:" + (root.getFileName() == null ? root : root.getFileName());
		}
		ServerData data = mc.getCurrentServer();
		return "server:" + (data == null ? "unknown" : data.ip);
	}

	private static JsonObject worlds() {
		try {
			if (!Files.exists(FILE)) return new JsonObject();
			JsonElement root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8));
			if (root.isJsonObject() && root.getAsJsonObject().get("worlds") instanceof JsonObject worlds) return worlds;
		} catch (IOException | RuntimeException e) {
			DefyingTheHeavens.LOGGER.warn("The Dao Codex's marks couldn't be read from {}: {}", FILE, e.toString());
		}
		return new JsonObject();
	}

	private static void load() {
		told.clear();
		read.clear();
		latest.clear();
		seen.clear();
		sighted.clear();
		if (!(worlds().get(world) instanceof JsonObject mine)) return;
		strings(mine.get("told"), told);
		strings(mine.get("read"), read);
		strings(mine.get("seen"), seen);
		strings(mine.get("sighted"), sighted);
	}

	private static void save() {
		if (world == null) return;
		JsonObject worlds = worlds();
		JsonObject mine = new JsonObject();
		mine.add("told", array(told));
		mine.add("read", array(read));
		mine.add("seen", array(seen));
		mine.add("sighted", array(sighted));
		worlds.add(world, mine);
		JsonObject root = new JsonObject();
		root.add("worlds", worlds);
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (IOException e) {
			DefyingTheHeavens.LOGGER.warn("The Dao Codex's marks couldn't be kept in {}: {}", FILE, e.toString());
		}
	}

	private static void strings(JsonElement element, Set<String> into) {
		if (element instanceof JsonArray array) for (JsonElement e : array) if (e.isJsonPrimitive()) into.add(e.getAsString());
	}

	private static JsonArray array(Set<String> ids) {
		JsonArray array = new JsonArray();
		for (String id : new TreeSet<>(ids)) array.add(id);
		return array;
	}

	private CodexState() {}
}
