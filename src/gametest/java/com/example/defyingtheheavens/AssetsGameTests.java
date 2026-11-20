package com.example.defyingtheheavens;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything the mod adds is named and drawn (bug sweep 10, 2026-11-18, the user: "check all screens and stuff"): each of its items, blocks,
 * beings, effects and biomes has its English name (else its raw key shows), each item a model, each block a blockstate, every model its
 * parent and textures (else the purple and black squares), and each sound its files.
 */
public class AssetsGameTests implements FabricGameTest {
	private static final String NS = DefyingTheHeavens.MOD_ID;

	private static @Nullable JsonObject json(String path) {
		try (InputStream in = AssetsGameTests.class.getResourceAsStream(path)) {
			return in == null ? null : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception e) {
			return null;
		}
	}

	private static boolean exists(String path) {
		return AssetsGameTests.class.getResource(path) != null;
	}

	private static boolean mine(ResourceLocation id) {
		return id != null && id.getNamespace().equals(NS);
	}

	/** "defying-the-heavens:item/x" (or "item/x", vanilla's) as a path under assets; null for another mod's or vanilla's. */
	private static @Nullable String assetPath(String ref, String folder, String ext) {
		ResourceLocation id = ResourceLocation.tryParse(ref);
		if (id == null || !mine(id)) return null;
		return "/assets/" + NS + "/" + folder + "/" + id.getPath() + ext;
	}

	/** The model {@code ref} and its parents: each exists, and so does each texture it names. */
	private static void model(String ref, List<String> missing, Set<String> seen, String of) {
		String path = assetPath(ref, "models", ".json");
		if (path == null || !seen.add(path)) return;
		JsonObject model = json(path);
		if (model == null) {
			missing.add("model " + ref + " (" + of + ")");
			return;
		}
		if (model.has("parent")) model(model.get("parent").getAsString(), missing, seen, of);
		if (model.has("textures")) {
			for (Map.Entry<String, JsonElement> t : model.getAsJsonObject("textures").entrySet()) {
				String value = t.getValue().getAsString();
				if (value.startsWith("#")) continue;
				String png = assetPath(value, "textures", ".png");
				if (png != null && !exists(png)) missing.add("texture " + value + " (" + of + ")");
			}
		}
	}

	/** Every model a blockstate names (its variants' and its multipart's). */
	private static void blockstateModels(JsonElement e, List<String> missing, Set<String> seen, String of) {
		if (e.isJsonArray()) {
			for (JsonElement each : e.getAsJsonArray()) blockstateModels(each, missing, seen, of);
		} else if (e.isJsonObject()) {
			JsonObject o = e.getAsJsonObject();
			if (o.has("model") && o.get("model").isJsonPrimitive()) model(o.get("model").getAsString(), missing, seen, of);
			for (Map.Entry<String, JsonElement> each : o.entrySet()) {
				if (!each.getKey().equals("model") && (each.getValue().isJsonObject() || each.getValue().isJsonArray())) {
					blockstateModels(each.getValue(), missing, seen, of);
				}
			}
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void everythingTheModAddsIsNamedAndDrawn(GameTestHelper helper) {
		JsonObject lang = json("/assets/" + NS + "/lang/en_us.json");
		helper.assertTrue(lang != null, "The English lang file is on the classpath");
		List<String> missing = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (Item item : BuiltInRegistries.ITEM) {
			ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
			if (!mine(id)) continue;
			if (!lang.has(item.getDescriptionId())) missing.add("name of " + item.getDescriptionId());
			String path = "/assets/" + NS + "/models/item/" + id.getPath() + ".json";
			if (!exists(path)) missing.add("item model " + id);
			else model(NS + ":item/" + id.getPath(), missing, seen, "item " + id.getPath());
		}
		for (Block block : BuiltInRegistries.BLOCK) {
			ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
			if (!mine(id)) continue;
			if (!lang.has(block.getDescriptionId())) missing.add("name of " + block.getDescriptionId());
			JsonObject state = json("/assets/" + NS + "/blockstates/" + id.getPath() + ".json");
			if (state == null) missing.add("blockstate " + id);
			else blockstateModels(state, missing, seen, "block " + id.getPath());
		}
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			if (mine(BuiltInRegistries.ENTITY_TYPE.getKey(type)) && !lang.has(type.getDescriptionId())) missing.add("name of " + type.getDescriptionId());
		}
		for (MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
			if (mine(BuiltInRegistries.MOB_EFFECT.getKey(effect)) && !lang.has(effect.getDescriptionId())) missing.add("name of " + effect.getDescriptionId());
		}
		JsonObject sounds = json("/assets/" + NS + "/sounds.json");
		for (SoundEvent sound : BuiltInRegistries.SOUND_EVENT) {
			ResourceLocation id = BuiltInRegistries.SOUND_EVENT.getKey(sound);
			if (!mine(id)) continue;
			if (sounds == null || !sounds.has(id.getPath())) {
				missing.add("sounds.json entry " + id);
				continue;
			}
			JsonObject entry = sounds.getAsJsonObject(id.getPath());
			if (!entry.has("sounds")) continue;
			for (JsonElement s : entry.getAsJsonArray("sounds")) {
				String name = s.isJsonObject() ? s.getAsJsonObject().get("name").getAsString() : s.getAsString();
				boolean event = s.isJsonObject() && s.getAsJsonObject().has("type") && s.getAsJsonObject().get("type").getAsString().equals("event");
				String ogg = event ? null : assetPath(name, "sounds", ".ogg");
				if (ogg != null && !exists(ogg)) missing.add("sound file " + name + " (" + id.getPath() + ")");
			}
		}
		Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
		for (ResourceLocation id : biomes.keySet()) {
			if (mine(id) && !lang.has("biome." + NS + "." + id.getPath())) missing.add("name of biome " + id);
		}
		for (String each : missing) DefyingTheHeavens.LOGGER.warn("[assets] missing {}", each);
		helper.assertTrue(missing.isEmpty(), missing.size() + " missing: " + String.join("; ", missing.subList(0, Math.min(30, missing.size()))));
		helper.succeed();
	}
}
