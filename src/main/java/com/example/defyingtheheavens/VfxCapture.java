package com.example.defyingtheheavens;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.function.IntSupplier;

/**
 * /vfxcapture: the capture tool of the visual-effects work (the VFX plan's step A, 2026-11-21). It plays an effect on the player who runs
 * it (the subject) and has the game's own picture of it recorded by the client that sees it (client VfxRecorder): every frame at the very
 * end of the frame, after the post effects, a shader pack and the HUD, labelled with its tick. tools/vfx-check (VfxCheck) turns a run into
 * sheets, curves and checks.
 * <ul>
 * <li>{@code breakthrough:<realm>[:<stage>[:<path>]] [seed] [full|short]}: the breakthrough into that realm (and stage) as the game
 * plays it: its tribulation to the end, recorded from the player's own eyes, from behind them and by an observer 24 blocks off at
 * eye height (the brief's two runs with the same seed, and a third); then its cinematic where it has one (Cinematics, with the dev flag),
 * every view's the same version with the same seed (else one drawn for the run), to its hand-back. Short only once made (step B4).</li>
 * <li>{@code meditation:<path>:<realm>[:<stage>] [harmony|surge|ready]}: a meditation from the side at 5 blocks (its settling in too),
 * from the player's own eyes and by an observer at 16 blocks, 6 seconds each.</li>
 * <li>{@code matrix:realms:<path>[:<stage>]}, {@code matrix:paths:<realm>[:<stage>]}: one frame of meditation for each realm (17) or each
 * path (4), from the side, for the meditation brief's two overview sheets.</li>
 * <li>{@code wall:<path>:<realm>[:<stage>]}: a meditation behind a wall of stone, seen by an observer through it: nothing may show.</li>
 * <li>{@code perf:} before any of the first two: the same spot without the effect and with it, nothing saved and the frame rate unbound,
 * only the frame times logged.</li>
 * </ul>
 * Options: {@code views=a,b} (breakthrough: player, third, observer; meditation: side, first, observer), {@code observer=<name>} (else the
 * nearest other player), {@code label=<text>}, {@code time=noon|night|keep}, {@code weather=clear|rain|keep}, {@code secs=<n>} (a
 * meditation view's, 6), {@code after=<ticks>} (recorded after a breakthrough, 160), {@code others=keep} (the subject's other paths left
 * where they stand; by default they go back to their first stage, setPath), {@code pitch=<degrees>} (where the subject looks while it
 * plays, up negative: a cinematic must blend from wherever that is), {@code obshud} (the observer recorded with their own HUD, in creative
 * mode, so their frames show it kept). {@code /vfxcapture stop} ends a run and puts things
 * back. Runs go to {@code -Ddth.vfx.dir} (else vfx-captures beside the game), a folder each. It sets its subject's cultivation as
 * /cultivation set does, so it is for test worlds; and it is a developer's alone (DevTools): without -Ddth.dev it doesn't exist.
 */
public final class VfxCapture {
	public static final ResourceLocation CHANNEL = DefyingTheHeavens.id("vfx_capture");
	/** The phase of SERVER_STOPPING a run is ended in, ahead of everyone else's. */
	private static final ResourceLocation BEFORE_THE_MOD = DefyingTheHeavens.id("vfx_capture_first");
	public static final int OP_BEGIN = 0, OP_MARK = 1, OP_CELL = 2, OP_END = 3;
	public static final int CAMERA_KEEP = 0, CAMERA_FIRST = 1, CAMERA_BACK = 2;
	/** The briefs' distances (blocks): the breakthrough's observer; the meditation's side camera and its observer. */
	public static final double OBSERVER = 24, SIDE = 5, FAR = 16;
	/** The block states hashed round the subject before and after (the plan's world-untouched check), this far each way. */
	public static final int HASH_RADIUS = 48;
	/** A bar's worth past any realm's, written as /cultivation addcultivation reads a number (no exponent). */
	private static final String BIG = "1000000000000000000000000000000";
	/** The longest a tribulation is waited on before the run gives up (ticks): its longest, and some. */
	private static final int TRIBULATION_WAIT = TribulationManager.MAX_TICKS + 400;
	/** Recorded after a cinematic's hand-back (ticks): the camera come home, the HUD back. */
	private static final int AFTER_CINEMATIC = 40;
	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	private record Spec(String text, String kind, boolean perf, Realm realm, Stage stage, TwoPaths.Path path, boolean byRealm, long seed,
			boolean full, String modifier, List<String> views, String observer, String label, String time, String weather, int seconds, int after,
			boolean keepOthers, Float pitch, boolean observerHud) {}

	/** One step of a run: it returns the ticks to wait before the next, or -1 to be asked again next tick (at most {@code limit} times). */
	private record Step(String name, IntSupplier action, int limit) {}

	private static final class Run {
		final Spec spec;
		final String id;
		final Path dir;
		final MinecraftServer server;
		final CommandSourceStack source;
		final UUID subject;
		UUID observer;
		final List<Step> steps = new ArrayList<>();
		int index, nextAt, age, repeats;
		final JsonObject json = new JsonObject();
		final JsonArray views = new JsonArray(), events = new JsonArray(), notes = new JsonArray(), cells = new JsonArray(), hashes = new JsonArray();
		final List<Runnable> restores = new ArrayList<>();
		/** The view being recorded, and whose client records it. */
		JsonObject view;
		UUID recorder;
		/** Where the subject stands, in which world, and which way they face, as the run began. */
		Vec3 stand;
		float facing;
		net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> world;
		/** The block states round the subject before the effect ({@link #HASH_RADIUS}). */
		int[] before;
		BlockPos hashCentre;
		int hashMinY;
		/** The path's rank before the breakthrough; the tribulation's start and its end (game time). */
		int rankBefore;
		long tribulationAt, tick0 = Long.MIN_VALUE;
		/** The seed every view's cinematic plays with (the spec's, else one drawn as the run begins); whether this view's breakthrough played one. */
		long seed;
		boolean cinematic;
		/** The wall's blocks (wall), to be put back as they were. */
		final List<BlockPos> wall = new ArrayList<>();
		boolean observerMoved;

		Run(Spec spec, String id, Path dir, MinecraftServer server, CommandSourceStack source, ServerPlayer subject) {
			this.spec = spec;
			this.id = id;
			this.dir = dir;
			this.server = server;
			this.source = source;
			this.subject = subject.getUUID();
		}

		ServerPlayer subject() {
			return server.getPlayerList().getPlayer(subject);
		}

		ServerPlayer observer() {
			return observer == null ? null : server.getPlayerList().getPlayer(observer);
		}

		/** The world the run began in (not the subject's: one who has left it mid-run is checked against the world they left). */
		ServerLevel level() {
			ServerLevel level = world == null ? null : server.getLevel(world);
			return level != null ? level : subject().serverLevel();
		}

		long now() {
			return level().getGameTime();
		}

		void add(String name, IntSupplier action) {
			steps.add(new Step(name, action, 0));
		}

		void poll(String name, int limit, IntSupplier action) {
			steps.add(new Step(name, action, limit));
		}
	}

	private static Run run;
	/** Subjects who left mid-run, and the guard (invulnerability) they had before it: put back as soon as they are on again. */
	private static final Map<UUID, Boolean> GUARD_OWED = new HashMap<>();

	public static void register() {
		if (!DevTools.ENABLED) return;
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(Commands.literal("vfxcapture")
				.requires(src -> src.hasPermission(2))
				.then(Commands.literal("stop").executes(ctx -> {
					if (run == null) {
						ctx.getSource().sendFailure(Component.literal("No capture is running."));
						return 0;
					}
					finish(run, "stopped by /vfxcapture stop");
					return 1;
				}))
				.then(Commands.argument("spec", StringArgumentType.greedyString()).executes(ctx -> start(ctx.getSource(),
						StringArgumentType.getString(ctx, "spec"))))));
		ServerTickEvents.END_SERVER_TICK.register(VfxCapture::tick);
		// The server (or a single-player world) closing mid-run: everything put back while its players are still on, before the world is saved,
		// and before the mod's own clean-up as it closes (CultivationEvents): a trial it clears first could no longer be called off here.
		ServerLifecycleEvents.SERVER_STOPPING.addPhaseOrdering(BEFORE_THE_MOD, Event.DEFAULT_PHASE);
		ServerLifecycleEvents.SERVER_STOPPING.register(BEFORE_THE_MOD, server -> {
			if (run != null && run.server == server) finish(run, "the server stopped");
		});
		ServerLifecycleEvents.SERVER_STARTED.register(VfxCapture::settleUnfinished); // a server killed mid-run: its rules back
		DefyingTheHeavens.LOGGER.info("[DTH-VFX] /vfxcapture is on (dev flag {})", DevTools.FLAG);
	}

	/** Whether a capture is running (the harness waits on it). */
	public static boolean busy() {
		return run != null;
	}

	// --- Starting a run ---

	private static int start(CommandSourceStack source, String text) {
		if (run != null) {
			source.sendFailure(Component.literal("A capture is running already: " + run.id + " (/vfxcapture stop ends it)."));
			return 0;
		}
		ServerPlayer subject;
		Spec spec;
		try {
			subject = source.getPlayerOrException();
			spec = parse(text);
		} catch (Exception e) {
			source.sendFailure(Component.literal("vfxcapture: " + e.getMessage()));
			return 0;
		}
		String id = LocalDateTime.now().format(STAMP) + "-" + (spec.perf() ? "perf-" : "") + spec.kind() + "-" + target(spec)
				+ (spec.label().isEmpty() ? "" : "-" + spec.label());
		Path dir = root(source.getServer()).resolve(id);
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			source.sendFailure(Component.literal("vfxcapture: can't make " + dir + ": " + e.getMessage()));
			return 0;
		}
		Run r = new Run(spec, id, dir, source.getServer(), source, subject);
		r.seed = spec.seed() != 0 ? spec.seed() : subject.getRandom().nextLong();
		r.stand = subject.position();
		r.facing = subject.getYRot();
		r.world = subject.level().dimension();
		describe(r, subject);
		String problem = plan(r, subject);
		if (problem != null) {
			source.sendFailure(Component.literal("vfxcapture: " + problem));
			try {
				Files.deleteIfExists(dir); // the run's folder, still empty: nothing left behind by a run that never began
			} catch (IOException ignored) {
				// not empty after all: kept
			}
			return 0;
		}
		run = r;
		source.sendSuccess(() -> Component.literal("Capturing " + spec.text() + " into " + dir), false);
		DefyingTheHeavens.LOGGER.info("[DTH-VFX] run {} begins: {} ({} steps)", r.id, spec.text(), r.steps.size());
		return 1;
	}

	/** The folder runs are written in: -Ddth.vfx.dir, else vfx-captures beside the game. */
	private static Path root(MinecraftServer server) {
		String dir = System.getProperty("dth.vfx.dir");
		return dir != null && !dir.isBlank() ? Path.of(dir) : server.getServerDirectory().toPath().resolve("vfx-captures");
	}

	// --- A server killed mid-run ---

	/** Where a running run's game rules are written down while it lasts (a killed server runs no SERVER_STOPPING to put them back). */
	private static Path owedFile(MinecraftServer server) {
		return root(server).resolve("unfinished-run.json");
	}

	private static String worldPath(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().toString();
	}

	/** The rules a run changes and what they were before it, written down until the run ends (finish deletes them). */
	private static void owe(Run r, int randomTickSpeed, boolean daylightCycle) {
		JsonObject o = new JsonObject();
		o.addProperty("run", r.id);
		o.addProperty("world", worldPath(r.server));
		o.addProperty("randomTickSpeed", randomTickSpeed);
		o.addProperty("doDaylightCycle", daylightCycle);
		try {
			Files.writeString(owedFile(r.server), o.toString(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			DefyingTheHeavens.LOGGER.warn("[DTH-VFX] couldn't write down {}'s game rules", r.id, e);
		}
	}

	/** As a server starts: the game rules of a run its world was killed in the middle of, put back (the subject's own state is not). */
	private static void settleUnfinished(MinecraftServer server) {
		Path file = owedFile(server);
		if (!Files.exists(file)) return;
		try {
			JsonObject o = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
			if (!o.get("world").getAsString().equals(worldPath(server))) return; // another world's run: left for it
			server.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(o.get("randomTickSpeed").getAsInt(), server);
			server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(o.get("doDaylightCycle").getAsBoolean(), server);
			Files.delete(file);
			DefyingTheHeavens.LOGGER.warn("[DTH-VFX] run {} was cut off with the server: its game rules are put back", o.get("run").getAsString());
		} catch (IOException | RuntimeException e) {
			DefyingTheHeavens.LOGGER.warn("[DTH-VFX] couldn't put back an unfinished run's game rules ({})", file, e);
		}
	}

	private static String target(Spec spec) {
		return switch (spec.kind()) {
			case "matrix" -> spec.byRealm() ? "realms-" + word(spec.path()) : "paths-" + code(spec.realm());
			case "breakthrough" -> code(spec.realm()) + (spec.stage() == Stage.EARLY ? "" : "-" + spec.stage().getId()) + (spec.path() == TwoPaths.Path.HEAVEN ? "" : "-" + word(spec.path()));
			default -> word(spec.path()) + "-" + code(spec.realm()) + (spec.stage() == Stage.EARLY ? "" : "-" + spec.stage().getId());
		};
	}

	// --- The spec ---

	private static final String[] CODES = {"qr", "fb", "cf", "ns", "hb", "fa", "di", "vs", "sv", "en", "ti", "ei", "hi", "gi", "tgi", "ps", "hl"};

	/** The realm's short name, as the plan's tables write it (qr ... hl). */
	public static String code(Realm realm) {
		return CODES[realm.ordinal()];
	}

	private static String word(TwoPaths.Path path) {
		return path.name().toLowerCase(Locale.ROOT);
	}

	private static Spec parse(String text) {
		String[] tokens = text.trim().split("\\s+");
		String target = tokens[0].toLowerCase(Locale.ROOT);
		boolean perf = target.startsWith("perf:");
		if (perf) target = target.substring(5);
		String[] parts = target.split(":");
		String kind = parts[0];
		Realm realm = Realm.HEAVENLY_BEING;
		Stage stage = Stage.EARLY;
		TwoPaths.Path path = TwoPaths.Path.HEAVEN;
		boolean byRealm = false;
		switch (kind) {
			case "breakthrough" -> {
				need(parts, 2, "breakthrough:<realm>[:<stage>[:<path>]]");
				realm = realm(parts[1]);
				if (parts.length > 2) stage = stage(parts[2]);
				if (parts.length > 3) path = path(parts[3]);
				if (realm == Realm.QI_REFINING && stage == Stage.EARLY) throw new IllegalArgumentException("nothing breaks through into Qi Refining Early");
			}
			case "meditation", "wall" -> {
				need(parts, 3, kind + ":<path>:<realm>[:<stage>]");
				path = path(parts[1]);
				realm = realm(parts[2]);
				if (parts.length > 3) stage = stage(parts[3]);
			}
			case "matrix" -> {
				need(parts, 3, "matrix:realms:<path>[:<stage>] or matrix:paths:<realm>[:<stage>]");
				if (parts[1].equals("realms")) {
					byRealm = true;
					path = path(parts[2]);
				} else if (parts[1].equals("paths")) {
					realm = realm(parts[2]);
				} else {
					throw new IllegalArgumentException("matrix:realms:<path> or matrix:paths:<realm>");
				}
				if (parts.length > 3) stage = stage(parts[3]);
			}
			default -> throw new IllegalArgumentException("unknown capture '" + kind + "' (breakthrough, meditation, matrix, wall; perf: before the first two)");
		}
		if (perf && !kind.equals("breakthrough") && !kind.equals("meditation")) throw new IllegalArgumentException("perf: is for breakthrough and meditation");
		long seed = 0;
		boolean full = true;
		String modifier = "", observer = "", label = "", time = "keep", weather = "keep";
		List<String> views = new ArrayList<>(switch (kind) {
			case "breakthrough" -> perf ? List.of("player") : List.of("player", "third", "observer");
			case "meditation" -> perf ? List.of("first") : List.of("side", "first", "observer");
			case "matrix" -> List.of("side");
			default -> List.of("observer");
		});
		int seconds = 6, after = 160;
		boolean keepOthers = false, observerHud = false;
		Float pitch = null;
		for (int i = 1; i < tokens.length; i++) {
			String t = tokens[i];
			String lower = t.toLowerCase(Locale.ROOT);
			if (lower.matches("-?\\d+")) seed = Long.parseLong(lower);
			else if (lower.equals("full") || lower.equals("short")) full = lower.equals("full");
			else if (lower.equals("harmony") || lower.equals("surge") || lower.equals("ready")) modifier = lower;
			else if (lower.startsWith("views=")) views = new ArrayList<>(Arrays.asList(lower.substring(6).split(",")));
			else if (lower.startsWith("observer=")) observer = t.substring(9);
			else if (lower.startsWith("label=")) label = t.substring(6).replaceAll("[^A-Za-z0-9_-]", "");
			else if (lower.startsWith("time=")) time = lower.substring(5);
			else if (lower.startsWith("weather=")) weather = lower.substring(8);
			else if (lower.startsWith("secs=")) seconds = Math.max(1, Math.min(60, Integer.parseInt(lower.substring(5))));
			else if (lower.startsWith("after=")) after = Math.max(20, Math.min(1200, Integer.parseInt(lower.substring(6))));
			else if (lower.equals("others=keep") || lower.equals("others=low")) keepOthers = lower.equals("others=keep");
			else if (lower.startsWith("pitch=")) pitch = Math.max(-90f, Math.min(90f, Float.parseFloat(lower.substring(6))));
			else if (lower.equals("obshud")) observerHud = true;
			else throw new IllegalArgumentException("what is '" + t + "'?");
		}
		for (String v : views) {
			if (!List.of("player", "third", "observer", "side", "first").contains(v)) throw new IllegalArgumentException("unknown view '" + v + "'");
		}
		if (kind.equals("breakthrough") && !full && !Cinematics.SHORT_READY) throw new IllegalArgumentException("the short versions come in step B4");
		return new Spec(text.trim(), kind, perf, realm, stage, path, byRealm, seed, full, modifier, views, observer, label, time, weather, seconds, after,
				keepOthers, pitch, observerHud);
	}

	private static void need(String[] parts, int count, String form) {
		if (parts.length < count) throw new IllegalArgumentException("write it " + form);
	}

	static Realm realm(String word) {
		for (int i = 0; i < CODES.length; i++) if (CODES[i].equals(word)) return Realm.values()[i];
		for (Realm r : Realm.values()) if (r.getId().equals(word)) return r;
		if (word.matches("\\d+")) {
			int i = Integer.parseInt(word);
			if (i >= 1 && i <= Realm.values().length) return Realm.values()[i - 1];
		}
		throw new IllegalArgumentException("unknown realm '" + word + "' (qr fb cf ns hb fa di vs sv en ti ei hi gi tgi ps hl)");
	}

	static Stage stage(String word) {
		return switch (word) {
			case "early", "1" -> Stage.EARLY;
			case "mid", "2" -> Stage.MID;
			case "late", "3" -> Stage.LATE;
			case "gp", "grand_perfection", "4" -> Stage.GRAND_PERFECTION;
			default -> throw new IllegalArgumentException("unknown stage '" + word + "' (early mid late gp)");
		};
	}

	static TwoPaths.Path path(String word) {
		for (TwoPaths.Path p : TwoPaths.Path.values()) if (word(p).equals(word)) return p;
		throw new IllegalArgumentException("unknown path '" + word + "' (heaven ghost demon body)");
	}

	// --- The plans ---

	/** Lays out {@code r}'s steps; @return what keeps it from running, or null */
	private static String plan(Run r, ServerPlayer subject) {
		Spec spec = r.spec;
		boolean needsObserver = spec.views().stream().anyMatch(v -> v.equals("observer") || v.equals("side"));
		if (needsObserver) {
			ServerPlayer o = findObserver(r, subject);
			if (o == null) return "the " + spec.kind() + " capture wants a second player to watch from (observer=<name>)";
			r.observer = o.getUUID();
		}
		for (String v : spec.views()) {
			ServerPlayer recorder = v.equals("observer") || v.equals("side") ? r.observer() : subject;
			if (!ServerPlayNetworking.canSend(recorder, CHANNEL)) return recorder.getGameProfile().getName() + "'s game has no recorder (start it with -D" + DevTools.FLAG + "=true)";
		}
		r.add("set the scene", () -> scene(r));
		switch (spec.kind()) {
			case "breakthrough" -> planBreakthrough(r);
			case "meditation" -> planMeditation(r);
			case "matrix" -> planMatrix(r);
			case "wall" -> planWall(r);
			default -> { }
		}
		return null;
	}

	/** The nearest other player in the subject's world, or the one named. */
	private static ServerPlayer findObserver(Run r, ServerPlayer subject) {
		ServerPlayer best = null;
		for (ServerPlayer p : subject.serverLevel().players()) {
			if (p == subject) continue;
			if (!r.spec.observer().isEmpty()) {
				if (p.getGameProfile().getName().equalsIgnoreCase(r.spec.observer())) return p;
				continue;
			}
			if (best == null || p.distanceToSqr(subject) < best.distanceToSqr(subject)) best = p;
		}
		return best;
	}

	/**
	 * The time and weather asked for (put back afterwards); the world's own slow changes stilled while it runs (random ticks: grass creeping
	 * onto earth and vines growing changed the block states round a capture by themselves, which the world-untouched check would lay at the
	 * effect's door); the subject kept from harm and from turning inward, Qi Sense's grey and the Consciousness Domain's lattice off.
	 */
	private static int scene(Run r) {
		ServerPlayer s = r.subject();
		ServerLevel level = s.serverLevel();
		GameRules.BooleanValue daylight = level.getGameRules().getRule(GameRules.RULE_DAYLIGHT);
		boolean cycle = daylight.get();
		if (!r.spec.time().equals("keep")) {
			long dayTime = level.getDayTime();
			daylight.set(false, r.server);
			level.setDayTime(r.spec.time().equals("night") ? 18000 : r.spec.time().equals("dusk") ? 12500 : 6000);
			r.restores.add(() -> {
				level.setDayTime(dayTime);
				daylight.set(cycle, r.server);
			});
		}
		GameRules.IntegerValue randomTicks = level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING);
		int speed = randomTicks.get();
		randomTicks.set(0, r.server);
		r.restores.add(() -> randomTicks.set(speed, r.server));
		owe(r, speed, cycle);
		if (r.spec.weather().equals("clear") || r.spec.weather().equals("rain")) {
			ServerLevelData weather = (ServerLevelData) level.getLevelData();
			int clearTime = weather.getClearWeatherTime(), rainTime = weather.getRainTime(), thunderTime = weather.getThunderTime();
			boolean raining = weather.isRaining(), thundering = weather.isThundering();
			if (r.spec.weather().equals("clear")) level.setWeatherParameters(12000, 0, false, false);
			else level.setWeatherParameters(0, 12000, true, false);
			r.restores.add(() -> {
				weather.setClearWeatherTime(clearTime);
				weather.setRainTime(rainTime);
				weather.setThunderTime(thunderTime);
				weather.setRaining(raining);
				weather.setThundering(thundering);
			});
		}
		boolean invulnerable = s.getAbilities().invulnerable;
		s.getAbilities().invulnerable = true; // the tribulation's strikes land, and harm nobody
		s.onUpdateAbilities();
		r.restores.add(() -> {
			ServerPlayer p = r.subject();
			if (p == null) { // left mid-run: their abilities were saved with the guard on
				GUARD_OWED.put(r.subject, invulnerable);
				note(r, "the subject had left the game: their guard is put back when they are next on");
				return;
			}
			p.getAbilities().invulnerable = invulnerable;
			p.onUpdateAbilities();
		});
		PlayerCultivation c = CultivationManager.own(s);
		// The ordinary body, the same every run: a physique changes a tribulation (a Primordial Chaos Body's strikes come half as often
		// again, a Heavenly Thunder Body drinks them) and how a body is drawn, and a test player's is rolled anew when they first join.
		Physique physique = c.getPhysique();
		java.util.Set<Physique> forms = new java.util.HashSet<>(c.getForms());
		c.setPhysique(Physique.FIVE_ELEMENT_ROOTS);
		c.setForms(List.of());
		r.restores.add(() -> {
			c.setPhysique(physique);
			c.setForms(forms);
		});
		if (physique != null && physique != Physique.FIVE_ELEMENT_ROOTS) note(r, "the subject's physique (" + physique.name().toLowerCase(Locale.ROOT)
				+ ") was set to the ordinary Five-Element Spiritual Roots for the run, and put back after");
		boolean sense = c.isAbilityEnabled(Ability.QI_SENSE), inner = c.isAbilityEnabled(Ability.INNER_REALM),
				domain = c.isAbilityEnabled(Ability.CONSCIOUSNESS_DOMAIN);
		c.setAbilityEnabled(Ability.QI_SENSE, false); // its grey would colour every frame
		c.setAbilityEnabled(Ability.INNER_REALM, false); // a long sitting would carry the soul off to its island
		c.setAbilityEnabled(Ability.CONSCIOUSNESS_DOMAIN, false); // its lattice of lines round the player would cross every frame
		r.restores.add(() -> {
			c.setAbilityEnabled(Ability.QI_SENSE, sense);
			c.setAbilityEnabled(Ability.INNER_REALM, inner);
			c.setAbilityEnabled(Ability.CONSCIOUSNESS_DOMAIN, domain);
			ServerPlayer p = r.subject();
			if (p != null) CultivationManager.refresh(p);
		});
		Bloodlines.Entry blood = Bloodlines.entry(r.server, r.subject);
		r.restores.add(() -> {
			Bloodlines.Entry now = Bloodlines.entry(r.server, r.subject);
			if (blood == null && now != null) Bloodlines.forget(r.server, r.subject);
			else if (blood != null && (now == null || now.bloodline() != blood.bloodline() || now.variant() != blood.variant())) {
				Bloodlines.set(r.server, r.subject, blood.bloodline(), blood.variant());
			}
		});
		if (MeditationManager.isMeditating(s.getUUID())) MeditationManager.stop(s, false);
		s.setHealth(s.getMaxHealth());
		CultivationManager.refresh(s);
		JsonObject scene = new JsonObject();
		scene.addProperty("dimension", level.dimension().location().toString());
		scene.addProperty("x", r.stand.x);
		scene.addProperty("y", r.stand.y);
		scene.addProperty("z", r.stand.z);
		scene.addProperty("facing", r.facing);
		scene.addProperty("dayTime", level.getDayTime() % 24000);
		scene.addProperty("raining", level.isRaining());
		scene.addProperty("thundering", level.isThundering());
		r.json.add("scene", scene);
		// Two seconds for every client to catch up before anything is recorded: a Consciousness Domain switched off here is still drawn on
		// others' screens until their next domain packet, or for up to 1.5 s (ConsciousnessRenderer), and a run that followed another one
		// (whose end had switched it back on) opened on its lattice in Checkpoint A's second round.
		return 40;
	}

	/**
	 * The breakthrough: its tribulation, run to the end, once for each view; then its cinematic, where it has one (Cinematics), every view's
	 * the same version with the same seed, recorded to its hand-back and a little after.
	 */
	private static void planBreakthrough(Run r) {
		Spec spec = r.spec;
		CinematicTimes.Entry entry = CinematicTimes.of(spec.realm());
		int cinematicWait = entry == null ? 1 : entry.of(spec.full()).length() + 200;
		r.restores.add(() -> { // a run ended part-way: no choice of its left for the subject's next breakthrough, nor a cinematic playing on
			Cinematics.forceNext(r.subject, null, 0);
			Cinematics.end(r.server, r.subject, "the capture ended");
		});
		for (String v : spec.perf() ? List.of("perf") : spec.views()) {
			r.add("ready (" + v + ")", () -> {
				String why = makeReady(r);
				if (why != null) throw new IllegalStateException("the subject isn't ready to break through: " + why);
				return 10;
			});
			r.add("camera (" + v + ")", () -> {
				ServerPlayer s = r.subject();
				switch (v) {
					case "observer" -> {
						if (!placeObserver(r, OBSERVER, 30, -10)) throw new IllegalStateException("no clear place 24 blocks off to watch from");
						begin(r, v, r.observer(), CAMERA_FIRST, spec.observerHud(), false, false);
					}
					case "third" -> begin(r, v, s, CAMERA_BACK, true, false, false);
					case "perf" -> begin(r, "player", s, CAMERA_FIRST, true, false, true);
					default -> begin(r, v, s, CAMERA_FIRST, true, false, false);
				}
				hashBefore(r);
				return 15;
			});
			if (spec.perf()) {
				r.add("without the effect", () -> {
					mark(r, "without");
					return 200;
				});
			}
			r.add("the tribulation (" + v + ")", () -> {
				ServerPlayer s = r.subject();
				r.rankBefore = TwoPaths.rank(CultivationManager.get(s), spec.path(), false);
				r.cinematic = false;
				Cinematics.forceNext(s.getUUID(), spec.full() ? Cinematics.Version.FULL : Cinematics.Version.SHORT, r.seed);
				switch (spec.path()) {
					case GHOST -> TribulationManager.startGhost(s);
					case DEMON -> TribulationManager.startDemon(s);
					case BODY -> TribulationManager.startBody(s);
					default -> TribulationManager.start(s);
				}
				if (!TribulationManager.isActive(s.getUUID())) throw new IllegalStateException("the tribulation didn't begin");
				r.tribulationAt = r.now();
				if (r.view != null) r.view.addProperty("plateUntil", r.tribulationAt); // the frames before: the scene without the effect
				mark(r, spec.perf() ? "with" : "tribulation");
				return 1;
			});
			r.poll("until it is won (" + v + ")", TRIBULATION_WAIT, () -> {
				ServerPlayer s = r.subject();
				if (TribulationManager.isActive(s.getUUID())) return -1;
				int rank = TwoPaths.rank(CultivationManager.get(s), spec.path(), false);
				if (rank <= r.rankBefore) throw new IllegalStateException("the tribulation ended without the breakthrough");
				r.tick0 = r.now();
				r.cinematic = Cinematics.isPlaying(s);
				mark(r, "breakthrough");
				if (r.cinematic) cinematicBeats(r, Cinematics.startedAt(s));
				else beats(r);
				return r.cinematic ? 0 : spec.after();
			});
			r.poll("its cinematic (" + v + ")", cinematicWait, () -> {
				if (!r.cinematic) return 0;
				if (Cinematics.isPlaying(r.subject())) return -1;
				mark(r, "handed back");
				return AFTER_CINEMATIC;
			});
			r.add("end (" + v + ")", () -> {
				end(r);
				hashAfter(r);
				return 10;
			});
		}
	}

	/** The breakthrough's acts and beats as today's game has them (ticks from the breakthrough), for the sheets. */
	private static void beats(Run r) {
		JsonObject v = r.view;
		if (v == null) return;
		long start = r.tribulationAt - r.tick0;
		JsonArray acts = new JsonArray();
		acts.add(act("The storm gathers", start, start + 60));
		acts.add(act("The strikes", start + 60, -20));
		acts.add(act("The breakthrough", -20, r.spec.after()));
		v.add("acts", acts);
		JsonArray beats = new JsonArray();
		beats.add(beat("Clouds gather", start + 30));
		beats.add(beat("First strike", start + 61));
		beats.add(beat("Mid storm", (start + 60) / 2));
		beats.add(beat("Last strikes", -24));
		beats.add(beat("Breakthrough", 0));
		beats.add(beat("Burst", 3));
		beats.add(beat("Burst fading", 20));
		beats.add(beat("After", 60));
		beats.add(beat("Settled", r.spec.after() - 2));
		v.add("beats", beats);
		v.addProperty("tick0", r.tick0);
	}

	/**
	 * A cinematic's acts (CinematicTimes) and beats, ticks from the breakthrough, for the sheets; and where the reference's white-out is
	 * lined up ({@code refAlign}: ours). The cinematic begins in the very tick the tribulation is won (TribulationManager ticks before the
	 * run), so {@code start} is t0; it is written down all the same. The recorder adds each shot (its client's record).
	 */
	private static void cinematicBeats(Run r, long start) {
		JsonObject v = r.view;
		if (v == null) return;
		CinematicTimes.Times times = CinematicTimes.of(r.spec.realm()).of(r.spec.full());
		long off = start - r.tick0;
		JsonArray acts = new JsonArray();
		acts.add(act("The last of the tribulation", -60, 0));
		for (CinematicTimes.Act a : times.acts()) acts.add(act(a.name(), a.from() + off, a.to() + off));
		acts.add(act("Handed back", times.length() + off, times.length() + off + AFTER_CINEMATIC));
		v.add("acts", acts);
		JsonArray beats = new JsonArray();
		beats.add(beat("Last strikes", -24)); // the checker puts it on the last strikes it finds: the cinematic begins only after them
		beats.add(beat("Breakthrough", 0));
		for (CinematicTimes.Act a : times.acts()) beats.add(beat(a.name(), (a.from() + a.to()) / 2 + off));
		beats.add(beat("Blast", times.blast() + off));
		beats.add(beat("White-out", times.whiteOut() + 8 + off));
		beats.add(beat("Title", times.length() - 25 + off));
		beats.add(beat("Handed back", times.length() + 15 + off));
		v.add("beats", beats);
		v.addProperty("tick0", r.tick0);
		v.addProperty("cinematicStart", start);
		v.addProperty("refAlign", times.whiteOut() + off);
	}

	/** Meditation from the side at 5 blocks (with its settling in), from the player's eyes, and by an observer at 16 (with its release). */
	private static void planMeditation(Run r) {
		Spec spec = r.spec;
		int loop = spec.seconds() * 20;
		r.add("ready", () -> {
			String why = setPath(r, spec.path(), spec.realm(), spec.stage(), spec.modifier().equals("ready"));
			if (why != null) throw new IllegalStateException(why);
			return 15;
		});
		if (spec.perf()) {
			r.add("camera", () -> {
				begin(r, "first", r.subject(), CAMERA_FIRST, true, false, true);
				hashBefore(r);
				return 15;
			});
			r.add("without", () -> {
				mark(r, "without");
				return 200;
			});
			r.add("sit", () -> {
				sit(r);
				return 40;
			});
			r.add("with", () -> {
				mark(r, "with");
				return 200;
			});
			r.add("stand", () -> {
				MeditationManager.stop(r.subject(), false);
				end(r);
				hashAfter(r);
				return 10;
			});
			return;
		}
		// Each view whole: a second standing (the scene without the effect, for the checker's clean plate), sitting down and the brief's 2 s
		// of settling in, its loop, and getting up (the release).
		for (String v : spec.views()) {
			r.add("camera (" + v + ")", () -> {
				switch (v) {
					case "side" -> {
						if (!placeObserver(r, SIDE, 90, 0)) throw new IllegalStateException("no clear place 5 blocks to the side");
						begin(r, v, r.observer(), CAMERA_FIRST, false, false, false);
					}
					case "observer" -> {
						if (!placeObserver(r, FAR, 30, 0)) throw new IllegalStateException("no clear place 16 blocks off");
						begin(r, v, r.observer(), CAMERA_FIRST, false, false, false);
					}
					default -> begin(r, "first", r.subject(), CAMERA_FIRST, true, false, false);
				}
				hashBefore(r);
				return 25;
			});
			r.add("sit (" + v + ")", () -> {
				if (r.view != null) r.view.addProperty("plateUntil", r.now());
				sit(r);
				return 40;
			});
			r.add("loop (" + v + ")", () -> {
				mark(r, "loop");
				if (spec.modifier().equals("harmony")) MeditationManager.harmony(r.subject());
				if (spec.modifier().equals("surge")) QiSurges.begin(r.subject(), QiSurges.Game.BREATH);
				return loop;
			});
			r.add("release (" + v + ")", () -> {
				MeditationManager.stop(r.subject(), false);
				mark(r, "release");
				return 25;
			});
			r.add("end (" + v + ")", () -> {
				meditationActs(r);
				end(r);
				return 5;
			});
		}
		r.add("hash after", () -> {
			hashAfter(r);
			return 10;
		});
	}

	/** The sheets' acts for a meditation view: standing and settling in, its loop, and getting up; ticks from the loop's start. */
	private static void meditationActs(Run r) {
		JsonObject v = r.view;
		if (v == null) return;
		long start = v.get("startGameTime").getAsLong();
		long loop = lastEvent(r, "loop"), release = lastEvent(r, "release");
		v.addProperty("tick0", loop);
		JsonArray acts = new JsonArray();
		acts.add(act("Standing, then settling in", start - loop, 0));
		acts.add(act("The breath", 0, r.spec.seconds() * 20));
		acts.add(act("The release", release - loop, r.now() - loop));
		v.add("acts", acts);
		JsonArray beats = new JsonArray();
		beats.add(beat("Standing", start - loop + 2));
		beats.add(beat("Sits", lastEvent(r, "sit") - loop));
		beats.add(beat("Settled", -2));
		for (int s = 0; s < r.spec.seconds(); s += 2) beats.add(beat("Breath +" + s + " s", s * 20L + 1));
		beats.add(beat("Gets up", release - loop + 2));
		v.add("beats", beats);
	}

	private static long lastEvent(Run r, String name) {
		for (int i = r.events.size() - 1; i >= 0; i--) {
			JsonObject e = r.events.get(i).getAsJsonObject();
			if (e.get("name").getAsString().equals(name)) return e.get("gameTime").getAsLong();
		}
		return r.now();
	}

	/** One frame of meditation per realm (or per path), from the side at 5 blocks. */
	private static void planMatrix(Run r) {
		Spec spec = r.spec;
		r.add("camera", () -> {
			if (!placeObserver(r, SIDE, 90, 0)) throw new IllegalStateException("no clear place 5 blocks to the side");
			begin(r, "side", r.observer(), CAMERA_FIRST, false, true, false);
			hashBefore(r);
			return 15;
		});
		List<Object[]> cells = new ArrayList<>();
		if (spec.byRealm()) {
			for (Realm realm : Realm.values()) cells.add(new Object[] {spec.path(), realm});
		} else {
			// The Demon Path last: the blood it may give (a tiger's) changes the body every cell after it would show.
			for (TwoPaths.Path path : new TwoPaths.Path[] {TwoPaths.Path.HEAVEN, TwoPaths.Path.GHOST, TwoPaths.Path.BODY, TwoPaths.Path.DEMON}) {
				cells.add(new Object[] {path, spec.realm()});
			}
		}
		for (int i = 0; i < cells.size(); i++) {
			TwoPaths.Path path = (TwoPaths.Path) cells.get(i)[0];
			Realm realm = (Realm) cells.get(i)[1];
			String name = String.format(Locale.ROOT, "%02d-%s", i + 1, spec.byRealm() ? code(realm) : word(path));
			r.add("set " + name, () -> {
				String why = setPath(r, path, realm, spec.stage(), false);
				if (why != null) {
					note(r, name + ": " + why);
					return 1;
				}
				return 10;
			});
			r.add("sit " + name, () -> {
				ServerPlayer s = r.subject();
				MeditationManager.start(s);
				if (MeditationManager.isMeditating(s.getUUID())) mark(r, "sit");
				else note(r, name + ": the subject couldn't sit to meditate");
				return 60;
			});
			r.add("cell " + name, () -> {
				JsonObject cell = new JsonObject();
				cell.addProperty("name", name);
				cell.addProperty("path", word(path));
				cell.addProperty("realm", code(realm));
				cell.addProperty("stage", spec.stage().getId());
				cell.addProperty("gameTime", r.now());
				cell.addProperty("meditating", MeditationManager.isMeditating(r.subject));
				cell.add("subject", subjectState(r.subject()));
				r.cells.add(cell);
				cell(r, name);
				return 6;
			});
			r.add("stand " + name, () -> {
				MeditationManager.stop(r.subject(), false);
				return 20;
			});
		}
		r.add("end", () -> {
			end(r);
			hashAfter(r);
			return 10;
		});
	}

	/** A meditation behind a wall of stone, watched by an observer through it: 2 s standing, then sitting, settling and its loop. */
	private static void planWall(Run r) {
		Spec spec = r.spec;
		r.add("ready", () -> {
			String why = setPath(r, spec.path(), spec.realm(), spec.stage(), false);
			if (why != null) throw new IllegalStateException(why);
			return 10;
		});
		r.add("camera and wall", () -> {
			if (!placeObserver(r, FAR, 0, 0)) throw new IllegalStateException("no clear place 16 blocks in front");
			buildWall(r);
			begin(r, "observer", r.observer(), CAMERA_FIRST, false, false, false);
			hashBefore(r);
			return 15;
		});
		r.add("standing", () -> {
			mark(r, "standing");
			return 40;
		});
		r.add("sit", () -> {
			if (r.view != null) r.view.addProperty("plateUntil", r.now());
			sit(r);
			return 40;
		});
		r.add("loop", () -> {
			mark(r, "loop");
			return spec.seconds() * 20;
		});
		r.add("end", () -> {
			JsonObject v = r.view;
			long loop = lastEvent(r, "loop");
			v.addProperty("tick0", loop);
			JsonArray acts = new JsonArray();
			acts.add(act("Standing behind the wall", lastEvent(r, "standing") - loop, lastEvent(r, "sit") - loop));
			acts.add(act("Meditating behind it", lastEvent(r, "sit") - loop, spec.seconds() * 20));
			v.add("acts", acts);
			MeditationManager.stop(r.subject(), false);
			end(r);
			hashAfter(r);
			return 5;
		});
	}

	// --- The subject ---

	private static void sit(Run r) {
		ServerPlayer s = r.subject();
		MeditationManager.start(s);
		if (!MeditationManager.isMeditating(s.getUUID())) throw new IllegalStateException("the subject couldn't sit to meditate");
		mark(r, "sit");
	}

	/**
	 * The subject one stage short of the breakthrough asked for, at its bottleneck, ready to call its tribulation: an obsession named
	 * where one is asked (a test's), a breakthrough pill taken where one is needed. @return why not, or null
	 */
	private static String makeReady(Run r) {
		ServerPlayer s = r.subject();
		Spec spec = r.spec;
		int target = PlayerCultivation.rank(spec.realm(), spec.stage());
		Realm realm = Realm.values()[(target - 1) / Stage.COUNT];
		Stage stage = Stage.values()[(target - 1) % Stage.COUNT];
		String why = setPath(r, spec.path(), realm, stage, true);
		if (why != null) return why;
		PlayerCultivation c = CultivationManager.get(s);
		if (Obsession.asks(c, target - 1)) {
			c.setObsession(FourAxis.Axis.LONGEVITY);
			note(r, "named Longevity as the subject's obsession, which the breakthrough asks for");
		}
		if (spec.path() == TwoPaths.Path.HEAVEN) {
			if (c.isMissingBreakthroughPill()) c.prepareBreakthrough(c.breakthroughRealm(), 0);
			CultivationManager.refresh(s);
			if (!c.canBreakthrough()) {
				return !c.isAtBottleneck() ? "not at the bottleneck" : c.isBreakthroughLocked() ? "held by the lower realms' cap (go where it doesn't hold)"
						: c.isHeldByMethod() ? "held by the Laws it wants" : c.isHeldByAxis() ? "held by its Four Axis virtue" : "not ready";
			}
		} else {
			boolean ready = switch (spec.path()) {
				case GHOST -> GhostMethods.canBreakthrough(c);
				case DEMON -> DemonPath.canBreakthrough(c);
				default -> BodyPath.canBreakthrough(c);
			};
			if (!ready) return "its " + word(spec.path()) + " path isn't ready";
		}
		s.setHealth(s.getMaxHealth());
		return null;
	}

	/**
	 * {@code path} set at {@code realm} and {@code stage} through /cultivation set (the same as an op setting it by hand), its bar full to
	 * the bottleneck when {@code full}, and drawn in by meditation. A demon's path wants a demon's blood: a tiger's is given where there's
	 * none (put back afterwards). Unless the spec says {@code others=keep}, every other path the subject walks is first set back to its
	 * first stage, so nothing left over from earlier decides how they look: the body is the higher path's (a ghost path above the heaven
	 * path's showed a Ghost King's form in the first baseline), and so are the float and the realm the HUD names. @return why it couldn't, or null
	 */
	private static String setPath(Run r, TwoPaths.Path path, Realm realm, Stage stage, boolean full) {
		ServerPlayer s = r.subject();
		PlayerCultivation c = CultivationManager.get(s);
		if (path == TwoPaths.Path.DEMON && DemonPath.rank(c) < 0) {
			command(r, "cultivation bloodline set tiger");
			note(r, "gave the subject a tiger's blood for the Demon Path");
		}
		if (path != TwoPaths.Path.HEAVEN && c.isMortal()) command(r, "cultivation set heaven 1 1");
		if (!r.spec.keepOthers()) {
			for (TwoPaths.Path other : TwoPaths.Path.values()) {
				if (other != path && TwoPaths.rank(CultivationManager.get(s), other, false) > 0) command(r, "cultivation set " + word(other) + " 1 1");
			}
		}
		String rank = (realm.ordinal() + 1) + " " + (stage.ordinal() + 1);
		command(r, "cultivation set " + word(path) + " " + rank);
		if (full) command(r, "cultivation addcultivation " + word(path) + " " + BIG);
		c = CultivationManager.get(s);
		c.netherworld().setCultivatingGhost(path == TwoPaths.Path.GHOST);
		c.demon().setCultivating(path == TwoPaths.Path.DEMON);
		c.body().setCultivating(path == TwoPaths.Path.BODY);
		CultivationManager.markDirty(r.server);
		Spells.sync(s);
		CultivationManager.refresh(s);
		s.setHealth(s.getMaxHealth()); // full at the new realm's measure (the scene's own top-up came before it), the HUD's bar alike in every view
		if (TwoPaths.drawnIn(c) != path) return "couldn't draw in the " + word(path) + " path";
		int want = PlayerCultivation.rank(realm, stage), got = TwoPaths.rank(c, path, false);
		if (got != want) return "the " + word(path) + " path stands at rank " + got + ", not " + want;
		return null;
	}

	/** A command run as the subject, at an op's leave, its answer not shown. */
	private static void command(Run r, String line) {
		ServerPlayer s = r.subject();
		r.server.getCommands().performPrefixedCommand(s.createCommandSourceStack().withPermission(4).withSuppressedOutput(), line);
	}

	private static JsonObject subjectState(ServerPlayer s) {
		PlayerCultivation c = CultivationManager.get(s);
		JsonObject o = new JsonObject();
		o.addProperty("name", s.getGameProfile().getName());
		o.addProperty("drawn", word(TwoPaths.drawnIn(c)));
		for (TwoPaths.Path p : TwoPaths.Path.values()) o.addProperty(word(p), TwoPaths.rank(c, p, false));
		o.addProperty("higherRank", TwoPaths.higherRank(c, false));
		o.addProperty("floats", MeditationManager.floats(s));
		o.addProperty("y", s.getY());
		return o;
	}

	// --- Cameras ---

	/**
	 * The observer moved {@code distance} blocks from the subject, its eye at theirs, {@code angle} degrees round from straight in front of
	 * them (or the nearest angle that sees them clearly: their chest and eye in sight, the camera in no block; a little higher if none
	 * does), looking at their chest, {@code tilt} degrees more up. In spectator mode, put back as it was at the end.
	 */
	private static boolean placeObserver(Run r, double distance, double angle, float tilt) {
		ServerPlayer o = r.observer(), s = r.subject();
		if (o == null) return false;
		ServerLevel level = s.serverLevel();
		double eyeHeight = s.getEyeHeight(Pose.STANDING);
		Vec3 eye = r.stand.add(0, eyeHeight, 0), chest = r.stand.add(0, 1.0, 0);
		double[] turns = {0, 20, -20, 40, -40, 70, -70, 110, -110, 150, -150, 180};
		Vec3 cam = null;
		for (double lift : new double[] {0, 2, 4, 8}) {
			for (double turn : turns) {
				double yaw = Math.toRadians(r.facing + angle + turn);
				Vec3 at = eye.add(-Math.sin(yaw) * distance, lift, Math.cos(yaw) * distance);
				BlockPos pos = BlockPos.containing(at);
				if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) continue;
				if (sees(level, at, chest, s) && sees(level, at, eye, s)) {
					cam = at;
					if (turn != 0 || lift != 0) note(r, "the camera " + Math.round(distance) + " blocks off moved " + Math.round(turn) + " degrees round and "
							+ Math.round(lift) + " up to see the subject");
					break;
				}
			}
			if (cam != null) break;
		}
		if (cam == null) return false;
		float yaw = yawTo(cam, chest), pitch = pitchTo(cam, chest) + tilt;
		if (!r.observerMoved) {
			r.observerMoved = true;
			GameType mode = o.gameMode.getGameModeForPlayer();
			Vec3 back = o.position();
			float backYaw = o.getYRot(), backPitch = o.getXRot();
			ServerLevel backLevel = o.serverLevel();
			r.restores.add(() -> {
				ServerPlayer p = r.observer();
				if (p == null) {
					note(r, "the observer had left the game: they come back as they left, in spectator mode where the camera stood");
					return;
				}
				p.setGameMode(mode);
				p.teleportTo(backLevel, back.x, back.y, back.z, backYaw, backPitch);
			});
		}
		if (r.spec.observerHud()) {
			// The observer's own HUD recorded too (obshud): a spectator has next to none, so in creative, flying where the camera stands.
			o.setGameMode(GameType.CREATIVE);
			o.getAbilities().flying = true;
			o.onUpdateAbilities();
		} else {
			o.setGameMode(GameType.SPECTATOR);
		}
		o.teleportTo(level, cam.x, cam.y - o.getEyeHeight(Pose.STANDING), cam.z, yaw, pitch);
		o.connection.resetPosition();
		JsonObject camera = new JsonObject();
		camera.addProperty("x", cam.x);
		camera.addProperty("y", cam.y);
		camera.addProperty("z", cam.z);
		camera.addProperty("yaw", yaw);
		camera.addProperty("pitch", pitch);
		camera.addProperty("distance", cam.distanceTo(chest));
		r.json.add("lastCamera", camera);
		return true;
	}

	private static boolean sees(ServerLevel level, Vec3 from, Vec3 to, ServerPlayer s) {
		return level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, s)).getType() == HitResult.Type.MISS;
	}

	static float yawTo(Vec3 from, Vec3 to) {
		return (float) Math.toDegrees(Math.atan2(to.z - from.z, to.x - from.x)) - 90.0f;
	}

	static float pitchTo(Vec3 from, Vec3 to) {
		double dx = to.x - from.x, dz = to.z - from.z;
		return (float) -Math.toDegrees(Math.atan2(to.y - from.y, Math.sqrt(dx * dx + dz * dz)));
	}

	/**
	 * A wall of stone bricks 11 wide and 6 high, 3 blocks from the subject toward the observer, square to the nearer axis (the wall test).
	 * Only air is built on; it is all taken down at the end.
	 */
	private static void buildWall(Run r) {
		ServerLevel level = r.level();
		ServerPlayer o = r.observer();
		Vec3 d = o.position().subtract(r.stand);
		boolean alongX = Math.abs(d.x) > Math.abs(d.z);
		int step = (int) Math.signum(alongX ? d.x : d.z);
		BlockPos feet = BlockPos.containing(r.stand);
		BlockPos centre = alongX ? feet.offset(3 * step, 0, 0) : feet.offset(0, 0, 3 * step);
		BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
		for (int w = -5; w <= 5; w++) {
			for (int h = 0; h < 6; h++) {
				BlockPos pos = alongX ? centre.offset(0, h, w) : centre.offset(w, h, 0);
				if (!level.getBlockState(pos).isAir()) continue;
				level.setBlock(pos, stone, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
				r.wall.add(pos.immutable());
			}
		}
		r.restores.add(() -> {
			for (BlockPos pos : r.wall) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		});
		JsonObject wall = new JsonObject();
		BlockPos a = alongX ? centre.offset(0, 0, -5) : centre.offset(-5, 0, 0), b = alongX ? centre.offset(1, 6, 6) : centre.offset(6, 6, 1);
		wall.add("min", vec(a.getX(), a.getY(), a.getZ()));
		wall.add("max", vec(b.getX(), b.getY(), b.getZ()));
		wall.addProperty("blocks", r.wall.size());
		r.json.add("wall", wall);
	}

	// --- The world untouched ---

	private static void hashBefore(Run r) {
		if (r.before != null) return;
		ServerLevel level = r.level();
		r.hashCentre = BlockPos.containing(r.stand);
		r.hashMinY = Math.max(level.getMinBuildHeight(), r.hashCentre.getY() - HASH_RADIUS);
		r.before = blocks(level, r.hashCentre, r.hashMinY);
	}

	/** The block states round the subject now, set against those before the effect: the same, or the first that differ. */
	private static void hashAfter(Run r) {
		if (r.before == null) return;
		ServerLevel level = r.level();
		int[] after = blocks(level, r.hashCentre, r.hashMinY);
		JsonObject h = new JsonObject();
		h.addProperty("radius", HASH_RADIUS);
		h.addProperty("blocks", after.length);
		h.addProperty("before", Long.toHexString(hash(r.before)));
		h.addProperty("after", Long.toHexString(hash(after)));
		h.addProperty("gameTime", r.now());
		int changed = 0;
		JsonArray first = new JsonArray();
		int size = 2 * HASH_RADIUS + 1, height = after.length / (size * size);
		for (int i = 0; i < after.length; i++) {
			if (after[i] == r.before[i]) continue;
			if (changed++ < 12) {
				int x = i / (size * height) - HASH_RADIUS, z = i / height % size - HASH_RADIUS, y = i % height + r.hashMinY;
				JsonObject o = new JsonObject();
				o.add("at", vec(r.hashCentre.getX() + x, y, r.hashCentre.getZ() + z));
				o.addProperty("was", Block.stateById(r.before[i]).toString());
				o.addProperty("now", Block.stateById(after[i]).toString());
				first.add(o);
			}
		}
		h.addProperty("changed", changed);
		h.add("firstChanges", first);
		r.hashes.add(h);
		r.before = null;
	}

	private static int[] blocks(ServerLevel level, BlockPos c, int minY) {
		int size = 2 * HASH_RADIUS + 1;
		int maxY = Math.min(level.getMaxBuildHeight() - 1, c.getY() + HASH_RADIUS);
		int height = maxY - minY + 1;
		int[] ids = new int[size * size * height];
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int i = 0;
		for (int x = -HASH_RADIUS; x <= HASH_RADIUS; x++) {
			for (int z = -HASH_RADIUS; z <= HASH_RADIUS; z++) {
				for (int y = minY; y <= maxY; y++) ids[i++] = Block.getId(level.getBlockState(pos.set(c.getX() + x, y, c.getZ() + z)));
			}
		}
		return ids;
	}

	private static long hash(int[] ids) {
		long h = 0xcbf29ce484222325L;
		for (int id : ids) {
			h ^= id;
			h *= 0x100000001b3L;
		}
		return h;
	}

	// --- Talking to the recorders ---

	/**
	 * How far down (degrees) the subject's own camera looks in each view: ahead from their eyes as a breakthrough begins, a little down from
	 * behind them so their body sits in the middle, and a little down from their eyes as they meditate, where the brief's ground layer is.
	 */
	private static float aimPitch(String view) {
		return switch (view) {
			case "third" -> 15;
			case "first" -> 12;
			default -> 0;
		};
	}

	/**
	 * View {@code name} begins: {@code recorder}'s client records every tick's frame from 5 ticks on ({@code cells}: only the frames the
	 * run asks for; {@code perf}: none, the frame rate unbound, the frame times alone), its HUD shown or not, its camera as asked. The
	 * observer looks where it was turned (placeObserver); the subject the way they faced as the run began, as low as the view wants.
	 */
	private static void begin(Run r, String name, ServerPlayer recorder, int camera, boolean hud, boolean cells, boolean perf) {
		if (r.view != null) end(r);
		long now = r.now();
		boolean subject = recorder.getUUID().equals(r.subject);
		float yaw = subject ? r.facing : recorder.getYRot(), pitch = subject ? (r.spec.pitch() != null ? r.spec.pitch() : aimPitch(name)) : recorder.getXRot();
		JsonObject v = new JsonObject();
		v.addProperty("name", name);
		v.addProperty("recorder", recorder.getGameProfile().getName());
		v.addProperty("startGameTime", now + 5);
		v.addProperty("camera", camera == CAMERA_BACK ? "third person, behind" : "first person");
		v.addProperty("hud", hud);
		v.addProperty("cellsOnly", cells);
		v.addProperty("perf", perf);
		v.addProperty("aimYaw", yaw);
		v.addProperty("aimPitch", pitch);
		if (recorder.getUUID().equals(r.observer) && r.json.has("lastCamera")) v.add("cameraAt", r.json.get("lastCamera"));
		v.add("subjectAtStart", subjectState(r.subject()));
		r.view = v;
		r.recorder = recorder.getUUID();
		r.views.add(v);
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeByte(OP_BEGIN);
		buf.writeUtf(r.id);
		buf.writeUtf(name);
		buf.writeUtf(r.dir.toAbsolutePath().toString());
		buf.writeLong(now);
		buf.writeLong(now + 5);
		buf.writeBoolean(cells);
		buf.writeBoolean(perf);
		buf.writeBoolean(hud);
		buf.writeByte(camera);
		buf.writeFloat(yaw);
		buf.writeFloat(pitch);
		buf.writeUtf(r.spec.label());
		ServerPlayNetworking.send(recorder, CHANNEL, buf);
		DefyingTheHeavens.LOGGER.info("[DTH-VFX] view {} of {} begins on {}'s client", name, r.id, recorder.getGameProfile().getName());
	}

	private static void mark(Run r, String name) {
		long now = r.now();
		JsonObject e = new JsonObject();
		e.addProperty("name", name);
		e.addProperty("gameTime", now);
		e.addProperty("view", r.view == null ? "" : r.view.get("name").getAsString());
		r.events.add(e);
		ServerPlayer recorder = r.recorder == null ? null : r.server.getPlayerList().getPlayer(r.recorder);
		if (recorder == null) return;
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeByte(OP_MARK);
		buf.writeUtf(name);
		buf.writeLong(now);
		ServerPlayNetworking.send(recorder, CHANNEL, buf);
	}

	private static void cell(Run r, String name) {
		ServerPlayer recorder = r.recorder == null ? null : r.server.getPlayerList().getPlayer(r.recorder);
		if (recorder == null) return;
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeByte(OP_CELL);
		buf.writeUtf(name);
		ServerPlayNetworking.send(recorder, CHANNEL, buf);
	}

	/** The view being recorded ends: its recorder writes out its frames and the run's record so far. */
	private static void end(Run r) {
		if (r.view == null) return;
		r.view.addProperty("endGameTime", r.now());
		ServerPlayer recorder = r.server.getPlayerList().getPlayer(r.recorder);
		if (recorder != null) {
			FriendlyByteBuf buf = PacketByteBufs.create();
			buf.writeByte(OP_END);
			buf.writeUtf(record(r), 4_000_000);
			ServerPlayNetworking.send(recorder, CHANNEL, buf);
		}
		DefyingTheHeavens.LOGGER.info("[DTH-VFX] view {} of {} ends", r.view.get("name").getAsString(), r.id);
		r.view = null;
		r.recorder = null;
	}

	// --- Ending ---

	private static void tick(MinecraftServer server) {
		if (!GUARD_OWED.isEmpty()) {
			GUARD_OWED.entrySet().removeIf(e -> {
				ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
				if (p == null) return false;
				p.getAbilities().invulnerable = e.getValue();
				p.onUpdateAbilities();
				return true;
			});
		}
		Run r = run;
		if (r == null || r.server != server) return;
		r.age++;
		if (r.subject() == null) {
			finish(r, "the subject left the game");
			return;
		}
		if (r.subject().level().dimension() != r.world) {
			finish(r, "the subject left the world it began in");
			return;
		}
		if (r.observer != null && r.observer() == null) {
			finish(r, "the observer left the game");
			return;
		}
		if (r.observerMoved && r.observer() != null && r.observer().level().dimension() != r.world) {
			finish(r, "the observer left the world the run began in");
			return;
		}
		while (r.index < r.steps.size() && r.age >= r.nextAt) {
			Step step = r.steps.get(r.index);
			int wait;
			try {
				wait = step.action().getAsInt();
			} catch (RuntimeException e) {
				DefyingTheHeavens.LOGGER.warn("[DTH-VFX] step '{}' of {} failed", step.name(), r.id, e);
				finish(r, step.name() + ": " + e.getMessage());
				return;
			}
			if (run != r) return;
			if (wait < 0) {
				if (++r.repeats > step.limit()) {
					finish(r, step.name() + ": waited too long");
					return;
				}
				r.nextAt = r.age + 1;
				break;
			}
			r.repeats = 0;
			r.index++;
			r.nextAt = r.age + wait;
		}
		if (r.index >= r.steps.size() && r.age >= r.nextAt) finish(r, null);
	}

	/** The run ends ({@code failure}: why, or null when it went through): everything it changed put back, its record written. */
	private static void finish(Run r, String failure) {
		run = null;
		if (failure != null) note(r, "FAILED: " + failure);
		try {
			ServerPlayer s = r.subject();
			// Stopped mid-tribulation: the trial called off (no reward, no fall) before the subject's guard is taken off below, or its
			// strikes would go on falling on a player no longer kept from harm.
			if (s != null && TribulationManager.isActive(s.getUUID())) {
				TribulationManager.abandon(s);
				note(r, "the tribulation was still running: called off, without reward or fall");
			} else if (r.tribulationAt > 0 && r.tribulationAt > r.tick0) {
				// The run's trial gone without its end: dropped by the mod as the subject left the game (TribulationManager#forget keeps the
				// mark, and #onJoin fails it when they next join: a fall), or cleared by something else. The mark is taken off their own
				// record, online or not, as abandon does, so the subject neither falls nor gains.
				PlayerCultivation c = CultivationManager.own(r.server, r.subject);
				switch (r.spec.path()) {
					case GHOST -> c.netherworld().setInGhostTribulation(false);
					case DEMON -> c.demon().setInTribulation(false);
					case BODY -> c.body().setInTribulation(false);
					default -> c.setInTribulation(false);
				}
				CultivationManager.markDirty(r.server);
				note(r, (s == null ? "the subject left mid-tribulation" : "the tribulation had been cleared before the run ended")
						+ ": its mark taken off, without reward or fall");
			}
			if (s != null && MeditationManager.isMeditating(s.getUUID())) MeditationManager.stop(s, false);
			if (r.before != null) hashAfter(r); // the run's own world, whether or not the subject is still in it (or in the game)
			if (r.view != null) {
				if (failure != null) r.view.addProperty("cutShort", true); // the view being recorded ended early (the checker fails it)
				end(r);
			}
		} catch (RuntimeException e) {
			DefyingTheHeavens.LOGGER.warn("[DTH-VFX] ending {} threw", r.id, e);
		}
		for (int i = r.restores.size() - 1; i >= 0; i--) {
			try {
				r.restores.get(i).run();
			} catch (RuntimeException e) {
				DefyingTheHeavens.LOGGER.warn("[DTH-VFX] putting back after {} threw", r.id, e);
			}
		}
		try {
			Files.deleteIfExists(owedFile(r.server)); // the rules are back: nothing owed if the server is killed later
		} catch (IOException e) {
			DefyingTheHeavens.LOGGER.warn("[DTH-VFX] couldn't remove {}'s note of its game rules", r.id, e);
		}
		r.json.addProperty("result", failure == null ? "done" : "failed");
		try {
			Files.writeString(r.dir.resolve("server.json"), record(r), StandardCharsets.UTF_8);
		} catch (IOException e) {
			DefyingTheHeavens.LOGGER.warn("[DTH-VFX] couldn't write {}'s record", r.id, e);
		}
		String line = failure == null ? "Capture " + r.id + " done." : "Capture " + r.id + " failed: " + failure;
		r.source.sendSystemMessage(Component.literal(line));
		DefyingTheHeavens.LOGGER.info("[DTH-VFX] run {} {}{}", r.id, failure == null ? "done" : "failed: ", failure == null ? "" : failure);
	}

	private static void note(Run r, String line) {
		r.notes.add(line);
		DefyingTheHeavens.LOGGER.info("[DTH-VFX] {}: {}", r.id, line);
	}

	/** The run's record as it stands, as JSON. */
	private static String record(Run r) {
		JsonObject o = r.json.deepCopy();
		o.addProperty("id", r.id);
		o.addProperty("spec", r.spec.text());
		o.addProperty("kind", (r.spec.perf() ? "perf:" : "") + r.spec.kind());
		o.addProperty("realm", code(r.spec.realm()));
		o.addProperty("stage", r.spec.stage().getId());
		o.addProperty("path", word(r.spec.path()));
		o.addProperty("seed", r.seed);
		o.addProperty("length", r.spec.full() ? "full" : "short");
		o.addProperty("modifier", r.spec.modifier());
		o.addProperty("label", r.spec.label());
		o.addProperty("written", LocalDateTime.now().toString());
		o.add("views", r.views.deepCopy());
		o.add("events", r.events.deepCopy());
		o.add("cells", r.cells.deepCopy());
		o.add("world", r.hashes.deepCopy());
		o.add("notes", r.notes.deepCopy());
		return new GsonBuilder().setPrettyPrinting().create().toJson(o);
	}

	private static JsonObject act(String name, long from, long to) {
		JsonObject o = new JsonObject();
		o.addProperty("name", name);
		o.addProperty("from", from);
		o.addProperty("to", to);
		return o;
	}

	private static JsonObject beat(String name, long tick) {
		JsonObject o = new JsonObject();
		o.addProperty("name", name);
		o.addProperty("tick", tick);
		return o;
	}

	private static JsonArray vec(double x, double y, double z) {
		JsonArray a = new JsonArray();
		a.add(x);
		a.add(y);
		a.add(z);
		return a;
	}

	private static void describe(Run r, ServerPlayer subject) {
		r.json.addProperty("server", r.server.getServerVersion());
		r.json.addProperty("subject", subject.getGameProfile().getName());
		r.json.add("subjectBefore", subjectState(subject));
	}

	private VfxCapture() {}
}
