package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModItems;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.world.item.ItemStack;

/**
 * The recipe book's own tab for the mod's crafting recipes, after vanilla's in the crafting table's book (and the inventory's):
 * shown with a Cultivation Fruit, as the creative tab is. Vanilla's tabs are a fixed enum, so the tab is added to it when it
 * loads (mixin/RecipeBookCategoriesMixin), and the mod's recipes are sorted into it (mixin/ClientRecipeBookMixin); the search
 * tab finds them too. The server teaches every player the recipes (ModRecipeBook), so all of them show.
 */
public final class CultivationRecipeTab {
	/** The tab, once RecipeBookCategories has loaded (set by its mixin). */
	public static RecipeBookCategories category;

	/** The tab (loading vanilla's tabs first, which adds it). */
	public static RecipeBookCategories category() {
		RecipeBookCategories.UNKNOWN.ordinal(); // makes sure the enum, and with it the tab, is there
		return category;
	}

	/** Its icon. */
	public static ItemStack icon() {
		return new ItemStack(ModItems.CULTIVATION_FRUIT);
	}

	private CultivationRecipeTab() {}
}
