package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.client.CultivationRecipeTab;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The mod's crafting recipes go in its own tab of the recipe book (CultivationRecipeTab), whatever their crafting category. */
@Mixin(ClientRecipeBook.class)
public abstract class ClientRecipeBookMixin {
	@Inject(method = "getCategory", at = @At("HEAD"), cancellable = true)
	private static void dth$cultivationTab(Recipe<?> recipe, CallbackInfoReturnable<RecipeBookCategories> cir) {
		if (!(recipe instanceof CraftingRecipe) || !recipe.getId().getNamespace().equals(DefyingTheHeavens.MOD_ID)) return;
		RecipeBookCategories tab = CultivationRecipeTab.category();
		if (tab != null) cir.setReturnValue(tab);
	}
}
