package com.example.defyingtheheavens;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Dao Codex (Codex, 2026-10-27): every page opened by advancements the server really loads, and written in English; the first
 * steps open from the first day; a page's keys are keys the Codex knows, and no stray % breaks its words.
 */
public class CodexGameTests implements FabricGameTest {
	private static final String NS = DefyingTheHeavens.MOD_ID;

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyPageOpensByARealAdvancement(GameTestHelper helper) {
		List<String> missing = new ArrayList<>();
		Set<String> ids = new HashSet<>();
		for (Codex.Entry entry : Codex.entries()) {
			helper.assertTrue(ids.add(entry.id()), "One page to an id: " + entry.id());
			for (String unlock : entry.unlocks()) {
				if (helper.getLevel().getServer().getAdvancements().getAdvancement(new ResourceLocation(NS, unlock)) == null) {
					missing.add(entry.id() + " <- " + unlock);
				}
			}
		}
		helper.assertTrue(missing.isEmpty(), "Every page's advancements are loaded: " + missing);
		helper.assertTrue(Codex.chapters().size() >= 10 && Codex.entries().size() >= 100,
				"The Codex is all there (" + Codex.chapters().size() + " chapters, " + Codex.entries().size() + " pages)");
		for (Codex.Chapter chapter : Codex.chapters()) helper.assertTrue(!chapter.entries().isEmpty(), "No empty chapter: " + chapter.id());
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyPageHasItsWords(GameTestHelper helper) {
		JsonObject lang = lang();
		List<String> missing = new ArrayList<>();
		for (Codex.Hint hint : Codex.Hint.values()) if (!lang.has(ModLang.codexHint(hint.id()))) missing.add("hint " + hint.id());
		for (Codex.Chapter chapter : Codex.chapters()) if (!lang.has(chapter.titleKey())) missing.add("chapter " + chapter.id());
		for (Codex.Entry entry : Codex.entries()) {
			if (!lang.has(entry.titleKey())) missing.add(entry.id() + " title");
			if (!lang.has(entry.textKey())) missing.add(entry.id() + " text");
			else {
				String text = lang.get(entry.textKey()).getAsString();
				if (!Codex.unknownKeys(text).isEmpty()) missing.add(entry.id() + " keys " + Codex.unknownKeys(text));
				if (text.contains("%")) missing.add(entry.id() + " has a %");
			}
		}
		for (String key : List.of(ModLang.KEY_CODEX, ModLang.CODEX_TITLE, ModLang.CODEX_BUTTON, ModLang.CODEX_LOCKED, ModLang.CODEX_COUNT,
				ModLang.CODEX_TOAST_NEW, ModLang.CODEX_TOAST_MANY)) {
			if (!lang.has(key)) missing.add(key);
		}
		helper.assertTrue(missing.isEmpty(), "Every chapter, page and hint is written in English: " + missing);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theFirstStepsAreOpenAndTheRestUnfold(GameTestHelper helper) {
		Codex.Chapter first = Codex.chapters().get(0);
		for (Codex.Entry entry : first.entries()) helper.assertTrue(entry.open(), "The first steps are open from the first day: " + entry.id());
		int open = 0, sealed = 0;
		for (Codex.Entry entry : Codex.entries()) {
			if (entry.open()) open++;
			else sealed++;
		}
		helper.assertTrue(sealed > open * 3, "Most of the Codex opens as one goes (" + open + " open, " + sealed + " sealed)");
		// The realms open in order: each realm's page by its own advancement.
		for (Realm realm : new Realm[] {Realm.FOUNDATION_BUILDING, Realm.CORE_FORMATION, Realm.NASCENT_SOUL, Realm.HEAVENLY_BEING}) {
			Codex.Entry page = Codex.entry(realm.name().toLowerCase(java.util.Locale.ROOT));
			helper.assertTrue(page != null && page.unlocks().contains("cultivation/" + realm.name().toLowerCase(java.util.Locale.ROOT)),
					"The " + realm + " page opens at that realm");
		}
		helper.succeed();
	}

	private static JsonObject lang() {
		try (InputStream in = DefyingTheHeavens.class.getResourceAsStream("/assets/" + NS + "/lang/en_us.json")) {
			if (in == null) return new JsonObject();
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (IOException e) {
			return new JsonObject();
		}
	}
}
