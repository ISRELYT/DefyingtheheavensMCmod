package com.example.defyingtheheavens.client.cinematic;

import com.example.defyingtheheavens.CinematicTimes;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.Realm;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A breakthrough cinematic as data (the brief §6.1): its shots (the camera), its elements (what is drawn in the world), its screen
 * overlays, the FOV and shake over time, and its title, all at whole ticks from its start. One JSON file a realm and version in
 * assets/defying-the-heavens/breakthroughs/ ("heavenly_being.json"), laid over `_skeleton.json` (the takeover, the hand-back and the
 * title's timing every cinematic shares), so tuning needs no recompile: F3+T reloads them, and a mistake is logged with its file and
 * place, that sequence left out (its breakthrough then hands the player straight back).
 *
 * Places are in the frame of the player's start: [side, up, forward], side positive to their right, forward where they faced.
 */
public final class Sequence {
	/**
	 * A camera shot from {@code from} to {@code to}. Either a path (keyframed places and look targets, spread evenly over the shot and
	 * eased by {@code ease}, a Catmull-Rom curve through them) or an orbit about a centre. {@code anchor} names what the places are
	 * relative to: "origin" (the player's start), "soul", "star" or "top" (the pillar's top). Roll in degrees; FOV in degrees or null
	 * for the player's own. {@code scale}: the shot's places are written for a pillar 128 blocks tall (open sky over a field at sea level,
	 * the clouds above it) and are scaled by the pillar's real height (CinematicCamera), so a wide shot of the pillar, the star or the sky
	 * frames them as well under a low ceiling as under high clouds.
	 */
	public record Shot(String name, int from, int to, Ease ease, String anchor, float[][] pos, float[][] look, float[] roll, Float fov,
					   Orbit orbit, boolean whip, boolean scale) {}

	/** An orbit: about centre, its radius, height and angle (degrees, 0 = in front of the player) each from one value to another. */
	public record Orbit(float[] center, float[] radius, float[] height, float[] angle) {}

	/** Something drawn in the world: a kind GreyBox knows, from tick to tick, its colour, and its numbers (by kind). */
	public record Element(String kind, int from, int to, int color, float[] params) {}

	/** Something laid over the breakthrough player's screen: "storm", "blink", "whiteout", "drain". */
	public record Overlay(String kind, int from, int to, int[] times, float alpha, int color) {}

	public record Key(float tick, float value) {}

	public record Shake(int from, int to, float start, float end) {}

	public final String file, realm, version;
	public final int length, blast, whiteOut, blendIn, blendOut, letterbox, titleFrom, titleTo;
	/** Whether its shapes are still the grey box's stand-ins (step B1): the capture tool's checker judges no colour of theirs then. */
	public final boolean placeholder;
	/**
	 * From which tick the film is still the grey box's (step B2 builds it act by act: "placeholderFrom": 330 while Acts 1 to 3 are real);
	 * 0 when it is all stand-ins ("placeholder": true), past its length when none is left.
	 */
	public final int placeholderFrom;
	/**
	 * The breakthrough player's grade over time (FxPost): keys of [tick, exposure, sky darkening, saturation, contrast, tint red, green,
	 * blue, vignette], eased between; none = the world left as it is.
	 */
	public final List<float[]> grade = new ArrayList<>();
	/** The radial blur's strength over time (0 none; a push, a whip), aimed at the player. */
	public final List<Key> radial = new ArrayList<>();
	public final List<Shot> shots = new ArrayList<>();
	public final List<Element> elements = new ArrayList<>();
	public final List<Overlay> overlays = new ArrayList<>();
	public final List<Key> fov = new ArrayList<>();
	public final List<Shake> shakes = new ArrayList<>();

	private Sequence(String file, JsonObject o, List<String> errors) {
		this.file = file;
		realm = str(o, "realm", errors);
		version = o.has("version") ? o.get("version").getAsString() : "full";
		length = num(o, "length", errors);
		blast = num(o, "blast", errors);
		whiteOut = num(o, "whiteOut", errors);
		blendIn = o.has("blendIn") ? o.get("blendIn").getAsInt() : 10;
		blendOut = o.has("blendOut") ? o.get("blendOut").getAsInt() : 10;
		letterbox = o.has("letterbox") ? o.get("letterbox").getAsInt() : 10;
		placeholder = o.has("placeholder") && o.get("placeholder").getAsBoolean();
		placeholderFrom = placeholder ? 0 : o.has("placeholderFrom") ? o.get("placeholderFrom").getAsInt() : Integer.MAX_VALUE;
		JsonObject title = o.has("title") ? o.getAsJsonObject("title") : new JsonObject();
		titleFrom = title.has("from") ? title.get("from").getAsInt() : length - 55;
		titleTo = title.has("to") ? title.get("to").getAsInt() : length;
		for (JsonElement e : arr(o, "shots")) {
			JsonObject s = e.getAsJsonObject();
			String name = s.has("name") ? s.get("name").getAsString() : "shot " + shots.size();
			try {
				Orbit orbit = null;
				if (s.has("orbit")) {
					JsonObject ob = s.getAsJsonObject("orbit");
					orbit = new Orbit(vec(ob.get("center")), pair(ob.get("radius")), pair(ob.get("height")), pair(ob.get("angle")));
				}
				shots.add(new Shot(name, s.get("from").getAsInt(), s.get("to").getAsInt(), Ease.of(s.has("ease") ? s.get("ease").getAsString() : null),
						s.has("anchor") ? s.get("anchor").getAsString() : "origin", s.has("pos") ? vecs(s.get("pos")) : null,
						s.has("look") ? vecs(s.get("look")) : null, s.has("roll") ? pair(s.get("roll")) : new float[] {0, 0},
						s.has("fov") ? s.get("fov").getAsFloat() : null, orbit, s.has("whip") && s.get("whip").getAsBoolean(),
						s.has("scale") && s.get("scale").getAsBoolean()));
				Shot added = shots.get(shots.size() - 1);
				if (added.orbit == null && (added.pos == null || added.look == null)) errors.add("shot '" + name + "' has neither an orbit nor both pos and look");
			} catch (RuntimeException ex) {
				errors.add("shot '" + name + "': " + ex.getMessage());
			}
		}
		for (JsonElement e : arr(o, "elements")) {
			JsonObject s = e.getAsJsonObject();
			String kind = s.has("kind") ? s.get("kind").getAsString() : "?";
			try {
				elements.add(new Element(kind, s.get("from").getAsInt(), s.get("to").getAsInt(), s.has("color") ? colour(s.get("color").getAsString()) : 0xFFFFFFFF,
						s.has("params") ? floats(s.getAsJsonArray("params")) : new float[0]));
				if (!GreyBox.KINDS.contains(kind) && !HeroFx.KINDS.contains(kind)) {
					errors.add("element kind '" + kind + "' is unknown (known: " + GreyBox.KINDS + " and " + HeroFx.KINDS + ")");
				}
			} catch (RuntimeException ex) {
				errors.add("element '" + kind + "': " + ex.getMessage());
			}
		}
		for (JsonElement e : arr(o, "overlays")) {
			JsonObject s = e.getAsJsonObject();
			String kind = s.has("kind") ? s.get("kind").getAsString() : "?";
			try {
				int from = s.get("from").getAsInt();
				int[] times = s.has("times") ? ints(s.getAsJsonArray("times")) : new int[0];
				int to = s.has("to") ? s.get("to").getAsInt() : from + sum(times);
				overlays.add(new Overlay(kind, from, to, times, s.has("alpha") ? s.get("alpha").getAsFloat() : 1, s.has("color") ? colour(s.get("color").getAsString()) : 0xFF000000));
				if (!List.of("storm", "blink", "whiteout", "drain").contains(kind)) errors.add("overlay kind '" + kind + "' is unknown");
			} catch (RuntimeException ex) {
				errors.add("overlay '" + kind + "': " + ex.getMessage());
			}
		}
		for (JsonElement e : arr(o, "fov")) {
			JsonArray k = e.getAsJsonArray();
			fov.add(new Key(k.get(0).getAsFloat(), k.get(1).getAsFloat()));
		}
		for (JsonElement e : arr(o, "shake")) {
			JsonObject s = e.getAsJsonObject();
			float[] amount = pair(s.get("amount"));
			shakes.add(new Shake(s.get("from").getAsInt(), s.get("to").getAsInt(), amount[0], amount[1]));
		}
		for (JsonElement e : arr(o, "grade")) {
			float[] k = floats(e.getAsJsonArray());
			if (k.length != 9) errors.add("a grade key needs 9 numbers [tick, exposure, sky, saturation, contrast, r, g, b, vignette], not " + k.length);
			else grade.add(k);
		}
		for (JsonElement e : arr(o, "radial")) {
			JsonArray k = e.getAsJsonArray();
			radial.add(new Key(k.get(0).getAsFloat(), k.get(1).getAsFloat()));
		}
		check(errors);
	}

	private final float[] gradeNow = new float[8];

	/** The grade at tick {@code t} (exposure, sky, saturation, contrast, r, g, b, vignette; eased between keys), or null if none. Reused. */
	public float[] gradeAt(float t) {
		if (grade.isEmpty()) return null;
		float[] a = grade.get(0), b = a;
		for (float[] k : grade) {
			if (k[0] <= t) a = k;
			if (k[0] >= t) {
				b = k;
				break;
			}
			b = k;
		}
		float f = b[0] <= a[0] ? 0 : Ease.SMOOTH.apply((t - a[0]) / (b[0] - a[0]));
		for (int i = 0; i < 8; i++) gradeNow[i] = a[i + 1] + (b[i + 1] - a[i + 1]) * f;
		return gradeNow;
	}

	/** The radial blur's strength at tick {@code t} (between keys in a straight line; 0 outside them). */
	public float radialAt(float t) {
		if (radial.isEmpty() || t < radial.get(0).tick() || t > radial.get(radial.size() - 1).tick()) return 0;
		for (int i = 0; i + 1 < radial.size(); i++) {
			Key a = radial.get(i), b = radial.get(i + 1);
			if (t >= a.tick() && t <= b.tick()) return b.tick() <= a.tick() ? b.value() : a.value() + (b.value() - a.value()) * (t - a.tick()) / (b.tick() - a.tick());
		}
		return 0;
	}

	/** The sequence held against the server's table (CinematicTimes), and its shots against its length. */
	private void check(List<String> errors) {
		Realm r = null;
		for (Realm each : Realm.values()) if (each.getId().equals(realm)) r = each;
		CinematicTimes.Entry entry = CinematicTimes.of(r);
		if (entry == null) {
			errors.add("realm '" + realm + "' has no entry in CinematicTimes");
			return;
		}
		CinematicTimes.Times t = entry.of(!version.equals("short"));
		if (t.length() != length) errors.add("length " + length + " but CinematicTimes says " + t.length());
		if (t.blast() != blast) errors.add("blast " + blast + " but CinematicTimes says " + t.blast());
		if (t.whiteOut() != whiteOut) errors.add("whiteOut " + whiteOut + " but CinematicTimes says " + t.whiteOut());
		int at = 0;
		List<Shot> sorted = new ArrayList<>(shots);
		sorted.sort((a, b) -> Integer.compare(a.from(), b.from()));
		for (Shot s : sorted) {
			if (s.from() > at) errors.add("no shot from tick " + at + " to " + s.from());
			if (s.to() <= s.from()) errors.add("shot '" + s.name() + "' ends before it begins");
			at = Math.max(at, s.to());
		}
		if (at < length) errors.add("no shot after tick " + at + " (length " + length + ")");
		for (Element e : elements) if (e.from() < 0 || e.to() > length + 20 || e.to() <= e.from()) errors.add("element '" + e.kind() + "' runs " + e.from() + "-" + e.to());
		// The overlays as the HUD reads them (CinematicHud): a blink and a white-out by their three times (down or up, held, up or clearing),
		// the storm and the drain by where they run.
		for (Overlay ov : overlays) {
			boolean timed = ov.kind().equals("blink") || ov.kind().equals("whiteout");
			if (timed && (ov.times().length != 3 || ov.times()[0] <= 0 || ov.times()[1] < 0 || ov.times()[2] <= 0)) {
				errors.add("overlay '" + ov.kind() + "' at " + ov.from() + " needs \"times\": [in, held, out], the first and last above 0");
			}
			if (ov.from() < 0 || ov.to() <= ov.from() || ov.to() > length + 20) errors.add("overlay '" + ov.kind() + "' runs " + ov.from() + "-" + ov.to());
		}
	}

	/** What is wrong with a sequence as given (the mistakes a load would log, none if it would load): for tests and the capture harness. */
	public static List<String> problems(String file, JsonObject o) {
		List<String> errors = new ArrayList<>();
		try {
			new Sequence(file, o, errors);
		} catch (RuntimeException e) {
			errors.add(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		return errors;
	}

	/** The shot on screen at tick {@code t} (the last one begun). */
	public Shot shotAt(float t) {
		Shot best = shots.isEmpty() ? null : shots.get(0);
		for (Shot s : shots) if (s.from() <= t && (best == null || s.from() >= best.from())) best = s;
		return best;
	}

	// --- Loading ---

	private static final Map<String, Sequence> LOADED = new HashMap<>();

	/** The sequence for {@code realm} ("heavenly_being") in {@code version} ("full"), or null. */
	public static Sequence get(String realm, String version) {
		return LOADED.get(realm + ":" + version);
	}

	/** Every sequence read anew (on start-up and F3+T); a cinematic already playing keeps the one it began with. */
	public static void reload(ResourceManager resources) {
		Map<String, Sequence> fresh = new HashMap<>();
		JsonObject skeleton = new JsonObject();
		Map<ResourceLocation, Resource> found = resources.listResources("breakthroughs", id -> id.getNamespace().equals(DefyingTheHeavens.MOD_ID)
				&& id.getPath().endsWith(".json"));
		for (Map.Entry<ResourceLocation, Resource> e : found.entrySet()) {
			if (!e.getKey().getPath().endsWith("/_skeleton.json")) continue;
			try (Reader reader = e.getValue().openAsReader()) {
				skeleton = JsonParser.parseReader(reader).getAsJsonObject();
			} catch (Exception ex) {
				DefyingTheHeavens.LOGGER.error("[DTH-CINEMATIC] {}: {}", e.getKey(), ex.getMessage());
			}
		}
		for (Map.Entry<ResourceLocation, Resource> e : found.entrySet()) {
			String path = e.getKey().getPath();
			if (path.endsWith("/_skeleton.json")) continue;
			List<String> errors = new ArrayList<>();
			try (Reader reader = e.getValue().openAsReader()) {
				JsonObject o = skeleton.deepCopy();
				for (Map.Entry<String, JsonElement> kv : JsonParser.parseReader(reader).getAsJsonObject().entrySet()) o.add(kv.getKey(), kv.getValue());
				Sequence s = new Sequence(e.getKey().toString(), o, errors);
				if (errors.isEmpty()) fresh.put(s.realm + ":" + s.version, s);
			} catch (Exception ex) {
				errors.add(ex.getClass().getSimpleName() + ": " + ex.getMessage());
			}
			for (String err : errors) DefyingTheHeavens.LOGGER.error("[DTH-CINEMATIC] {}: {}", e.getKey(), err);
			if (!errors.isEmpty()) DefyingTheHeavens.LOGGER.error("[DTH-CINEMATIC] {} left out: {} mistake(s) above", e.getKey(), errors.size());
		}
		LOADED.clear();
		LOADED.putAll(fresh);
		DefyingTheHeavens.LOGGER.info("[DTH-CINEMATIC] {} sequence(s) loaded: {}", LOADED.size(), LOADED.keySet());
	}

	// --- JSON helpers ---

	private static String str(JsonObject o, String key, List<String> errors) {
		if (!o.has(key)) {
			errors.add("'" + key + "' is missing");
			return "";
		}
		return o.get(key).getAsString().toLowerCase(Locale.ROOT);
	}

	private static int num(JsonObject o, String key, List<String> errors) {
		if (!o.has(key)) {
			errors.add("'" + key + "' is missing");
			return 0;
		}
		return o.get(key).getAsInt();
	}

	private static JsonArray arr(JsonObject o, String key) {
		return o.has(key) ? o.getAsJsonArray(key) : new JsonArray();
	}

	private static float[] vec(JsonElement e) {
		float[] v = floats(e.getAsJsonArray());
		if (v.length != 3) throw new IllegalArgumentException("a place needs 3 numbers [side, up, forward], not " + v.length);
		return v;
	}

	private static float[][] vecs(JsonElement e) {
		JsonArray a = e.getAsJsonArray();
		if (a.size() == 0) throw new IllegalArgumentException("an empty list of places");
		float[][] out = new float[a.size()][];
		for (int i = 0; i < a.size(); i++) out[i] = vec(a.get(i));
		return out;
	}

	/** One number, or [from, to]. */
	private static float[] pair(JsonElement e) {
		if (e == null) return new float[] {0, 0};
		if (e.isJsonArray()) {
			float[] f = floats(e.getAsJsonArray());
			return f.length == 1 ? new float[] {f[0], f[0]} : new float[] {f[0], f[1]};
		}
		return new float[] {e.getAsFloat(), e.getAsFloat()};
	}

	private static float[] floats(JsonArray a) {
		float[] f = new float[a.size()];
		for (int i = 0; i < f.length; i++) f[i] = a.get(i).getAsFloat();
		return f;
	}

	private static int[] ints(JsonArray a) {
		int[] f = new int[a.size()];
		for (int i = 0; i < f.length; i++) f[i] = a.get(i).getAsInt();
		return f;
	}

	private static int sum(int[] v) {
		int s = 0;
		for (int i : v) s += i;
		return s;
	}

	/** "#RRGGBB" or "#AARRGGBB" (opaque without an alpha). */
	private static int colour(String hex) {
		String h = hex.startsWith("#") ? hex.substring(1) : hex;
		long v = Long.parseLong(h, 16);
		return h.length() <= 6 ? (int) (0xFF000000L | v) : (int) v;
	}
}
