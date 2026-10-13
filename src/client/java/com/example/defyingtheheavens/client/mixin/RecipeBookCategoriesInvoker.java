package com.example.defyingtheheavens.client.mixin;

import net.minecraft.client.RecipeBookCategories;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The recipe book's tabs' own constructor, for adding the mod's tab (RecipeBookCategoriesMixin, CultivationRecipeTab). */
@Mixin(RecipeBookCategories.class)
public interface RecipeBookCategoriesInvoker {
	@Invoker("<init>")
	static RecipeBookCategories dth$create(String name, int ordinal, ItemStack[] icons) {
		throw new AssertionError();
	}
}
