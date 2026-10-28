package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the Artifact Forge can forge (the user's asks, 2026-10-28): every kind of artifact (ArtifactKind) at every grade (ArtifactGrade).
 * Materials go in one at a time, in any order; the forge refuses anything that wouldn't still fit some working, so nothing is lost. A
 * working is begun by hand (an empty hand on the forge, ArtifactForgeBlockEntity) once its materials are all in: two workings can share
 * a beginning (a flying sword's two metal and a stick are a glaive's but for a second stick), so none starts by itself.
 * <p>
 * Each working takes its grade's metal ({@link ArtifactGrade#metal}), two of its grade's spirit stones, the kind's own make-up, and from
 * Spirit grade up a demon core of at least the grade's realm ({@link ArtifactGrade#coreRealm}: the element of an element banner is its
 * core's). A Mortal banner takes an element's token instead: blaze powder (fire), a prismarine shard (water), a sapling (wood), clay
 * (earth) or an iron nugget (metal).
 * <p>
 * A weapon made of its element (phase 2: ElementalWeaponItem), from Spirit grade up, takes its element's own matter (two blaze rods and
 * magma cream; three prismarine crystals; two vines and a Blue Spirit Log; two obsidian and Jade Stone; two gold ingots and glowstone dust),
 * two of the grade's stones and a demon core of that element, of the grade's realm or above.
 */
public final class ArtifactRecipes {
	/** Most materials the forge holds at once. */
	public static final int MAX_MATERIALS = 9;

	/**
	 * One working: what it forges, its plain materials (item to count, in display order), and whether it wants a demon core besides (of
	 * at least its grade's realm) or an element's token.
	 */
	public record Recipe(ArtifactKind kind, ArtifactGrade grade, Map<Item, Integer> materials, boolean core, boolean token) {
		boolean matches(Tally tally) {
			return tally.counts().equals(materials) && tally.cores().size() == (core ? 1 : 0) && tally.tokens().size() == (token ? 1 : 0)
					&& coresFit(tally);
		}

		boolean couldBecome(Tally tally) {
			for (Map.Entry<Item, Integer> entry : tally.counts().entrySet()) {
				if (entry.getValue() > materials.getOrDefault(entry.getKey(), 0)) return false;
			}
			return tally.cores().size() <= (core ? 1 : 0) && tally.tokens().size() <= (token ? 1 : 0) && coresFit(tally);
		}

		/** Its cores of its grade's realm or above, and, for a weapon made of its element, of that element. */
		private boolean coresFit(Tally tally) {
			for (ItemStack core : tally.cores()) {
				if (grade.coreRealm() == null || DemonCoreItem.realm(core).ordinal() < grade.coreRealm().ordinal()) return false;
				if (kind.isElemental() && DemonCoreItem.element(core) != kind.element()) return false;
			}
			return true;
		}

		/** The item it forges. */
		public Item output() {
			return ModItems.artifact(kind, grade);
		}
	}

	/** What the forge holds, counted: plain materials by item, the demon cores and the element tokens apart. */
	record Tally(Map<Item, Integer> counts, List<ItemStack> cores, List<ItemStack> tokens) {}

	private static List<Recipe> recipes;

	/** Built on first use, once every item is registered. */
	public static List<Recipe> all() {
		if (recipes == null) {
			List<Recipe> list = new ArrayList<>();
			for (ArtifactGrade grade : ArtifactGrade.values()) {
				Item metal = grade.metal(), stone = grade.stone();
				boolean core = grade.coreRealm() != null;
				boolean fitted = grade.ordinal() >= ArtifactGrade.EARTH.ordinal(); // Earth grade up: the vessels' fittings in its metal
				list.add(recipe(ArtifactKind.FLYING_SWORD, grade, core, false, metal, 2, Items.STICK, 1, stone, 2));
				list.add(recipe(ArtifactKind.SABER, grade, core, false, metal, 3, Items.LEATHER, 1, stone, 2));
				list.add(recipe(ArtifactKind.SPEAR, grade, core, false, metal, 1, Items.STICK, 2, stone, 2));
				list.add(recipe(ArtifactKind.GLAIVE, grade, core, false, metal, 2, Items.STICK, 2, stone, 2));
				list.add(recipe(ArtifactKind.BANNER, grade, core, !core, Items.WHITE_WOOL, 2, Items.STICK, 1, stone, 2));
				list.add(fitted ? recipe(ArtifactKind.GOURD, grade, core, false, Items.PUMPKIN, 1, Items.GLASS_BOTTLE, 1, stone, 2, metal, 1)
						: recipe(ArtifactKind.GOURD, grade, core, false, Items.PUMPKIN, 1, Items.GLASS_BOTTLE, 1, stone, 2));
				list.add(fitted ? recipe(ArtifactKind.JADE, grade, core, false, ModItems.JADE, 2, Items.STRING, 1, stone, 2, metal, 1)
						: recipe(ArtifactKind.JADE, grade, core, false, ModItems.JADE, 2, Items.STRING, 1, stone, 2));
				if (!core) continue;
				// The weapons made of their element: the element's own matter, the grade's stones and a core of that element.
				list.add(recipe(ArtifactKind.FLAME_BLADE, grade, true, false, Items.BLAZE_ROD, 2, Items.MAGMA_CREAM, 1, stone, 2));
				list.add(recipe(ArtifactKind.TIDE_SPEAR, grade, true, false, Items.PRISMARINE_CRYSTALS, 3, stone, 2));
				list.add(recipe(ArtifactKind.VINE_WHIP, grade, true, false, Items.VINE, 2, ModBlocks.BLUE_SPIRIT_LOG.asItem(), 1, stone, 2));
				list.add(recipe(ArtifactKind.MOUNTAIN_HAMMER, grade, true, false, Items.OBSIDIAN, 2, ModBlocks.JADE_STONE.asItem(), 1, stone, 2));
				list.add(recipe(ArtifactKind.GOLDEN_SWORD, grade, true, false, Items.GOLD_INGOT, 2, Items.GLOWSTONE_DUST, 1, stone, 2));
			}
			recipes = List.copyOf(list);
		}
		return recipes;
	}

	/** Pairs of (item, count) after what it forges and whether it wants a core or a token. */
	private static Recipe recipe(ArtifactKind kind, ArtifactGrade grade, boolean core, boolean token, Object... itemsAndCounts) {
		Map<Item, Integer> materials = new LinkedHashMap<>();
		for (int i = 0; i < itemsAndCounts.length; i += 2) materials.merge((Item) itemsAndCounts[i], (Integer) itemsAndCounts[i + 1], Integer::sum);
		return new Recipe(kind, grade, materials, core, token);
	}

	/** The element {@code stack} stands for as a Mortal banner's token, or null if it is none. */
	public static @Nullable QiElement tokenElement(ItemStack stack) {
		if (stack.is(Items.BLAZE_POWDER)) return QiElement.FIRE;
		if (stack.is(Items.PRISMARINE_SHARD)) return QiElement.WATER;
		if (stack.is(ItemTags.SAPLINGS)) return QiElement.WOOD;
		if (stack.is(Items.CLAY_BALL)) return QiElement.EARTH;
		if (stack.is(Items.IRON_NUGGET)) return QiElement.METAL;
		return null;
	}

	static Tally tally(List<ItemStack> contents) {
		Map<Item, Integer> counts = new HashMap<>();
		List<ItemStack> cores = new ArrayList<>(), tokens = new ArrayList<>();
		for (ItemStack stack : contents) {
			if (stack.is(ModItems.DEMON_CORE)) cores.add(stack);
			else if (tokenElement(stack) != null) tokens.add(stack);
			else counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
		}
		return new Tally(counts, cores, tokens);
	}

	/** One of {@code stack} can go in on top of {@code contents} and still lead to some working. */
	public static boolean canAdd(List<ItemStack> contents, ItemStack stack) {
		if (stack.isEmpty() || contents.size() >= MAX_MATERIALS) return false;
		List<ItemStack> next = new ArrayList<>(contents);
		next.add(stack.copyWithCount(1));
		Tally tally = tally(next);
		for (Recipe recipe : all()) {
			if (recipe.couldBecome(tally)) return true;
		}
		return false;
	}

	/** The working {@code contents} makes exactly, or null. */
	public static @Nullable Recipe match(List<ItemStack> contents) {
		if (contents.isEmpty()) return null;
		Tally tally = tally(contents);
		for (Recipe recipe : all()) {
			if (recipe.matches(tally)) return recipe;
		}
		return null;
	}

	public static @Nullable Recipe forOutput(Item output) {
		for (Recipe recipe : all()) {
			if (recipe.output() == output) return recipe;
		}
		return null;
	}

	/** The element a working of {@code contents} gives its banner: its core's, or its token's (null: none). */
	public static @Nullable QiElement element(List<ItemStack> contents) {
		for (ItemStack stack : contents) {
			if (stack.is(ModItems.DEMON_CORE)) return DemonCoreItem.element(stack);
			QiElement token = tokenElement(stack);
			if (token != null) return token;
		}
		return null;
	}

	/** "2 Iron Ingot, Stick, 2 Low-grade Spirit Stone, a demon core of Qi Refining or above", for the Codex and the forge's words. */
	public static Component describe(Recipe recipe) {
		MutableComponent text = Component.empty();
		boolean first = true;
		for (Map.Entry<Item, Integer> entry : recipe.materials().entrySet()) {
			if (!first) text.append(", ");
			first = false;
			if (entry.getValue() > 1) text.append(entry.getValue() + " ");
			text.append(entry.getKey().getDescription());
		}
		if (recipe.core() && recipe.kind().isElemental()) {
			text.append(", ").append(Component.translatable(ModLang.FORGE_NEEDS_ELEMENT_CORE, recipe.kind().element().getDisplayName(),
					recipe.grade().coreRealm().getDisplayName()));
		} else if (recipe.core()) {
			text.append(", ").append(Component.translatable(ModLang.FORGE_NEEDS_CORE, recipe.grade().coreRealm().getDisplayName()));
		}
		if (recipe.token()) text.append(", ").append(Component.translatable(ModLang.FORGE_NEEDS_TOKEN));
		return text;
	}

	private ArtifactRecipes() {}
}
