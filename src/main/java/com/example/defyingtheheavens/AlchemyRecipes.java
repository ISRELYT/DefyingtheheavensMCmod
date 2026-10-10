package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the Alchemy Cauldron can brew. Ingredients go in one at a time, in any order; a recipe brews as soon as the cauldron
 * holds exactly its ingredients. The cauldron refuses anything that wouldn't still fit some recipe, so a wrong ingredient is
 * never lost.
 * <p>
 * Aged ingredients (the herbs, Cultivation Fruit) carry their years into the pill: the average age of the aged ingredients in
 * a brew decides the grade's odds and adds to the pill's strength (see {@link PillGrade}). The rarer herb variants (Purple
 * Lingzhi, Ochre Huangjing) stand in for the ordinary herb in any recipe and count as twice their age.
 */
public final class AlchemyRecipes {
	/** Most ingredients the cauldron holds at once. */
	public static final int MAX_INGREDIENTS = 6;

	/** One recipe: what it brews, its ingredients (item to count, in display order) and how long it takes over the fire. */
	public record Recipe(Item output, Map<Item, Integer> ingredients, int brewTicks) {
		public boolean matches(Map<Item, Integer> counts) {
			return counts.equals(ingredients);
		}

		/** Every count in {@code counts} still fits this recipe. */
		public boolean couldBecome(Map<Item, Integer> counts) {
			for (Map.Entry<Item, Integer> entry : counts.entrySet()) {
				if (entry.getValue() > ingredients.getOrDefault(entry.getKey(), 0)) return false;
			}
			return true;
		}
	}

	private static List<Recipe> recipes;

	/** Built on first use, once every item is registered. */
	public static List<Recipe> all() {
		if (recipes == null) {
			recipes = List.of(
					// The first step on the path: drunk by a mortal to open the meridians.
					recipe(ModItems.MARROW_CLEANSING_ELIXIR, 20, ModItems.GINSENG, 1, Items.HONEY_BOTTLE, 1, Items.BONE_MEAL, 2),
					recipe(ModItems.QI_GATHERING_PILL, 20, ModItems.GINSENG, 1, ModItems.SPIRIT_DEW, 1, Items.GLOWSTONE_DUST, 1),
					recipe(ModItems.CULTIVATION_PILL, 30, ModItems.GINSENG, 1, ModItems.HUANGJING, 1, Items.GLISTERING_MELON_SLICE, 1),
					// Breakthrough pills: Qi Refining -> Foundation Building, Foundation Building -> Core Formation.
					recipe(ModItems.FOUNDATION_PILL, 45, ModItems.GINSENG, 1, ModItems.LINGZHI, 1, ModItems.SPIRIT_GINSENG, 1,
							Items.AMETHYST_SHARD, 1),
					recipe(ModItems.CORE_PILL, 60, ModItems.SPIRIT_GINSENG, 1, ModItems.HUANGJING, 1, ModItems.CULTIVATION_FRUIT, 1,
							Items.BLAZE_POWDER, 1));
		}
		return recipes;
	}

	/** {@code seconds} over the fire; then pairs of (item, count). */
	private static Recipe recipe(Item output, int seconds, Object... itemsAndCounts) {
		Map<Item, Integer> ingredients = new LinkedHashMap<>();
		for (int i = 0; i < itemsAndCounts.length; i += 2) {
			ingredients.put((Item) itemsAndCounts[i], (Integer) itemsAndCounts[i + 1]);
		}
		return new Recipe(output, ingredients, seconds * 20);
	}

	/** The ordinary herb a rarer variant stands in for (Purple Lingzhi for Lingzhi...), or null. */
	public static Item substituteFor(Item item) {
		if (item == ModItems.PURPLE_LINGZHI) return ModItems.LINGZHI;
		if (item == ModItems.OCHRE_HUANGJING) return ModItems.HUANGJING;
		return null;
	}

	/** The item a recipe asks for: a rarer variant counts as its ordinary herb. */
	private static Item canonical(Item item) {
		Item base = substituteFor(item);
		return base == null ? item : base;
	}

	private static Map<Item, Integer> counts(List<ItemStack> contents) {
		Map<Item, Integer> counts = new HashMap<>();
		for (ItemStack stack : contents) counts.merge(canonical(stack.getItem()), stack.getCount(), Integer::sum);
		return counts;
	}

	/** One of {@code stack} can go in on top of {@code contents} and still lead to some recipe. */
	public static boolean canAdd(List<ItemStack> contents, ItemStack stack) {
		if (stack.isEmpty() || contents.size() >= MAX_INGREDIENTS) return false;
		Map<Item, Integer> counts = counts(contents);
		counts.merge(canonical(stack.getItem()), 1, Integer::sum);
		for (Recipe recipe : all()) {
			if (recipe.couldBecome(counts)) return true;
		}
		return false;
	}

	/** The recipe {@code contents} makes exactly, or null. */
	public static Recipe match(List<ItemStack> contents) {
		if (contents.isEmpty()) return null;
		Map<Item, Integer> counts = counts(contents);
		for (Recipe recipe : all()) {
			if (recipe.matches(counts)) return recipe;
		}
		return null;
	}

	public static Recipe forOutput(Item output) {
		for (Recipe recipe : all()) {
			if (recipe.output() == output) return recipe;
		}
		return null;
	}

	/** "2 Ginseng, Spirit Ginseng, Amethyst Shard", for tooltips and the cultivation menu. */
	public static Component describe(Item output) {
		Recipe recipe = forOutput(output);
		MutableComponent text = Component.empty();
		if (recipe == null) return text;
		boolean first = true;
		for (Map.Entry<Item, Integer> entry : recipe.ingredients().entrySet()) {
			if (!first) text.append(", ");
			first = false;
			if (entry.getValue() > 1) text.append(entry.getValue() + " ");
			text.append(entry.getKey().getDescription());
		}
		return text;
	}

	/** Years an aged ingredient has grown (the herbs, Cultivation Fruit); 0 for anything that doesn't age. */
	public static int ageOf(ItemStack stack) {
		if (stack.getItem() instanceof GinsengItem) return GinsengItem.age(stack);
		if (stack.getItem() instanceof CultivationFruitItem) return CultivationFruitItem.age(stack);
		return 0;
	}

	/** Average age of the aged ingredients in {@code contents}, each counted once per item; 1 if none of them age. */
	public static int averageAge(List<ItemStack> contents) {
		long total = 0;
		int aged = 0;
		for (ItemStack stack : contents) {
			int age = ageOf(stack);
			if (age <= 0) continue;
			if (substituteFor(stack.getItem()) != null) age = FruitAge.clamp(age * 2); // the rare variant's richer essence
			total += (long) age * stack.getCount();
			aged += stack.getCount();
		}
		return aged == 0 ? 1 : FruitAge.clamp((int) Math.round(total / (double) aged));
	}

	private AlchemyRecipes() {}
}
