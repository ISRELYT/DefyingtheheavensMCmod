package com.example.defyingtheheavens;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;

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
 * steps open from the first day; a page's keys are keys the Codex knows, and no stray % breaks its words. Since 2026-10-28 its
 * pictures name real items and recipes, and its map of the worlds (CodexMap) tells the truth.
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

	/**
	 * The pictures (2026-10-28): every one a picture the Codex draws, of items and recipes that are real (the crafting ones as the server
	 * loads them), and every diagram used.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void everyPictureIsReal(GameTestHelper helper) {
		List<String> problems = new ArrayList<>();
		Set<String> diagrams = new HashSet<>();
		int pictures = 0;
		for (Codex.Entry entry : Codex.entries()) {
			for (String problem : Codex.pictureProblems(entry.text())) problems.add(entry.id() + ": " + problem);
			for (String line : entry.text().split("\n")) {
				String kind = Codex.pictureKind(line);
				if (kind == null) continue;
				pictures++;
				List<String> parts = Codex.pictureParts(line);
				if (kind.equals("diagram")) diagrams.addAll(parts);
				if (kind.equals("craft") && parts.size() == 1) {
					ResourceLocation id = Codex.pictureId(parts.get(0));
					boolean crafted = id != null && helper.getLevel().getRecipeManager().byKey(id)
							.filter(recipe -> recipe.getType() == RecipeType.CRAFTING).isPresent();
					if (!crafted) problems.add(entry.id() + ": no crafting recipe " + parts.get(0));
				}
			}
		}
		helper.assertTrue(problems.isEmpty(), "Every picture shows real things: " + problems);
		helper.assertTrue(diagrams.equals(Codex.DIAGRAMS), "Every diagram is drawn somewhere: " + diagrams);
		helper.assertTrue(pictures >= 20, "The Codex has its pictures (" + pictures + ")");
		helper.assertTrue(!Codex.pictureProblems("@items no_such_thing\n@brew minecraft:stone\n@diagram nowhere\n@sketch jade").isEmpty()
				&& Codex.pictureProblems("@items jade*4 + minecraft:bone_meal > foundation_pill").isEmpty(), "Wrong pictures are told apart from right ones");
		helper.succeed();
	}

	/**
	 * The map of the worlds (CodexMap, 2026-10-28): every way to it from the Overworld, every visit an advancement the server loads (and
	 * its criterion one of that advancement's), each world's qi the game's own, and its words written.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theMapIsTrue(GameTestHelper helper) {
		List<String> problems = new ArrayList<>();
		for (CodexMap.World world : CodexMap.World.values()) {
			for (String visit : world.visits()) {
				Advancement advancement = helper.getLevel().getServer().getAdvancements().getAdvancement(CodexMap.advancementOf(visit));
				String criterion = CodexMap.criterionOf(visit);
				if (advancement == null) problems.add(world.id() + " <- " + visit);
				else if (criterion != null && !advancement.getCriteria().containsKey(criterion)) problems.add(world.id() + " <- no criterion " + visit);
			}
			if (world.always() != (world == CodexMap.World.OVERWORLD)) problems.add(world.id() + " known from the first day");
			if (world.qiTop() < world.qi() || world.ghostQiTop() < world.ghostQi()) problems.add(world.id() + " qi upside down");
		}
		// Every world but those reached from anywhere is reached from the Overworld by the ways.
		Set<CodexMap.World> reached = new HashSet<>(Set.of(CodexMap.World.OVERWORLD));
		for (boolean grew = true; grew; ) {
			grew = false;
			for (CodexMap.Way way : CodexMap.WAYS) {
				if (way.from() == way.to()) problems.add("a way from " + way.from() + " to itself");
				if (reached.contains(way.from()) && reached.add(way.to()) || reached.contains(way.to()) && reached.add(way.from())) grew = true;
			}
		}
		for (CodexMap.World world : CodexMap.World.values()) {
			if (!world.anywhere() && !reached.contains(world)) problems.add(world.id() + " has no way to it");
			if (world.anywhere() && reached.contains(world)) problems.add(world.id() + " is reached from anywhere, with no way drawn");
		}
		// The qi it tells is the qi the game gives.
		if (CodexMap.World.GREAT_VOID.qi() != RealmSuppressionHandler.realmQi(ModDimensions.GREAT_VOID)) problems.add("the Great Void's qi");
		if (CodexMap.World.IMMORTAL_REALM.qi() != PlayerCultivation.IMMORTAL_QI_MULTIPLIER) problems.add("the Immortal Realm's qi");
		if (CodexMap.World.OVERWORLD.qi() != 1 || CodexMap.World.NETHERWORLD.ghostQi() != GhostQi.density(ModDimensions.NETHERWORLD, null)) {
			problems.add("the Overworld's qi or the Netherworld's ghostly qi");
		}
		JsonObject lang = lang();
		for (CodexMap.World world : CodexMap.World.values()) {
			if (!lang.has(world.nameKey()) || !lang.has(world.wayKey())) problems.add(world.id() + " unwritten");
		}
		for (String key : List.of(ModLang.CODEX_QI, ModLang.CODEX_QI_BODY, ModLang.CODEX_GHOST_QI, ModLang.CODEX_GHOST_QI_IN_PLACES, ModLang.CODEX_TIMES,
				ModLang.CODEX_RANGE, ModLang.CODEX_RECOMMENDED, ModLang.CODEX_ANY_REALM, ModLang.CODEX_REALM_STAGE, ModLang.CODEX_HERE,
				ModLang.CODEX_ANYWHERE, ModLang.CODEX_LOWER_REALMS, ModLang.CODEX_YOUR_REALM, ModLang.CODEX_CULTIVATED_IN, ModLang.CODEX_OVERCOMES,
				ModLang.CODEX_OVERCOME_BY)) {
			if (!lang.has(key)) problems.add(key);
		}
		helper.assertTrue(problems.isEmpty(), "The map tells the truth: " + problems);
		helper.succeed();
	}

	/**
	 * The Bestiary (2026-10-28): a page for every beast, elder, primordial and fiend, the Hungry Ghost, the Heart Demon and the Vestige;
	 * each page's creature a real one, opened by sight alone (no advancement), its picture first.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theBestiaryHasEveryCreature(GameTestHelper helper) {
		List<String> problems = new ArrayList<>();
		Set<String> sights = Codex.sights();
		for (BeastKind kind : BeastKind.values()) {
			if (!sights.contains(NS + ":" + kind.getId())) problems.add("no page for " + kind.getId());
		}
		for (String other : List.of("hungry_ghost", "heart_demon", "void_vestige")) {
			if (!sights.contains(NS + ":" + other)) problems.add("no page for " + other);
		}
		for (Codex.Entry entry : Codex.entries()) {
			if (entry.sight() == null) continue;
			if (!BuiltInRegistries.ENTITY_TYPE.containsKey(new ResourceLocation(entry.sight()))) problems.add(entry.id() + ": no creature " + entry.sight());
			if (entry.open() || !entry.unlocks().isEmpty()) problems.add(entry.id() + " opens otherwise than by sight");
			if (!entry.text().startsWith("@beast ")) problems.add(entry.id() + " doesn't begin with its picture");
			if (!entry.chapter().equals("bestiary")) problems.add(entry.id() + " outside the Bestiary");
		}
		helper.assertTrue(problems.isEmpty(), "The Bestiary is whole: " + problems);
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
