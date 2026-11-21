package com.example.defyingtheheavens.client.cinematic;

import com.example.defyingtheheavens.Cinematics;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * The player's own settings for the breakthrough cinematics (the brief §6.10), kept in config/defying-the-heavens-client.json: a stub
 * for step B1 (the file and what it holds; the options screen comes later). Cinematics: full, short or off; camera shake 0-100%;
 * flashes: full or reduced (the white-out a warm glow); reduce motion (no whips, no roll). Written with its defaults the first time.
 */
public final class CinematicSettings {
	public static Cinematics.Pref cinematics = Cinematics.Pref.FULL;
	public static int shake = 100;
	public static boolean reducedFlashes;
	public static boolean reduceMotion;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("defying-the-heavens-client.json");
	}

	public static void load() {
		Path file = file();
		try {
			if (Files.exists(file)) {
				JsonObject o = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
				// Each value on its own: a misspelt one keeps its default and doesn't stop the rest being read.
				read(file, "cinematics", () -> cinematics = Cinematics.Pref.valueOf(o.get("cinematics").getAsString().toUpperCase(Locale.ROOT)), o);
				read(file, "shake", () -> shake = Mth.clamp(o.get("shake").getAsInt(), 0, 100), o);
				read(file, "flashes", () -> reducedFlashes = o.get("flashes").getAsString().equalsIgnoreCase("reduced"), o);
				read(file, "reduceMotion", () -> reduceMotion = o.get("reduceMotion").getAsBoolean(), o);
			}
		} catch (IOException | RuntimeException e) {
			DefyingTheHeavens.LOGGER.warn("[DTH-CINEMATIC] couldn't read {}: the defaults stand", file, e);
		}
		try {
			save(); // written back: the defaults filled in, a misspelt value replaced by what stands
		} catch (IOException e) {
			DefyingTheHeavens.LOGGER.warn("[DTH-CINEMATIC] couldn't write {}", file, e);
		}
	}

	private static void read(Path file, String key, Runnable apply, JsonObject o) {
		if (!o.has(key)) return;
		try {
			apply.run();
		} catch (RuntimeException e) {
			DefyingTheHeavens.LOGGER.warn("[DTH-CINEMATIC] {}: '{}' is not a value of {}: its default stands", file, o.get(key), key);
		}
	}

	public static void save() throws IOException {
		JsonObject o = new JsonObject();
		o.addProperty("cinematics", cinematics.name().toLowerCase(Locale.ROOT));
		o.addProperty("shake", shake);
		o.addProperty("flashes", reducedFlashes ? "reduced" : "full");
		o.addProperty("reduceMotion", reduceMotion);
		Files.writeString(file(), GSON.toJson(o), StandardCharsets.UTF_8);
	}

	private CinematicSettings() {}
}
