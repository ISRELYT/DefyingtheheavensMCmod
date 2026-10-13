package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.CultivationRecipeTab;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The mod's own tab in the recipe book (see CultivationRecipeTab): a constant added to vanilla's tabs as the enum loads (its
 * values grown by one), placed last among the crafting book's tabs, and searched by the crafting book's search tab.
 */
@Mixin(RecipeBookCategories.class)
public abstract class RecipeBookCategoriesMixin {
	@Shadow @Final @Mutable private static RecipeBookCategories[] $VALUES;
	@Shadow @Final @Mutable public static Map<RecipeBookCategories, List<RecipeBookCategories>> AGGREGATE_CATEGORIES;

	/** The crafting book's tabs with the mod's last, made once. */
	@Unique private static List<RecipeBookCategories> dth$craftingTabs;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void dth$addCultivationTab(CallbackInfo ci) {
		RecipeBookCategories tab = RecipeBookCategoriesInvoker.dth$create("DTH_CULTIVATION", $VALUES.length,
				new ItemStack[] {CultivationRecipeTab.icon()});
		RecipeBookCategories[] values = Arrays.copyOf($VALUES, $VALUES.length + 1);
		values[values.length - 1] = tab;
		$VALUES = values;
		CultivationRecipeTab.category = tab;
		Map<RecipeBookCategories, List<RecipeBookCategories>> aggregate = new LinkedHashMap<>(AGGREGATE_CATEGORIES);
		aggregate.put(RecipeBookCategories.CRAFTING_SEARCH, ImmutableList.<RecipeBookCategories>builder()
				.addAll(AGGREGATE_CATEGORIES.get(RecipeBookCategories.CRAFTING_SEARCH)).add(tab).build());
		AGGREGATE_CATEGORIES = ImmutableMap.copyOf(aggregate);
	}

	@Inject(method = "getCategories", at = @At("RETURN"), cancellable = true)
	private static void dth$craftingHasTheTab(RecipeBookType type, CallbackInfoReturnable<List<RecipeBookCategories>> cir) {
		if (type != RecipeBookType.CRAFTING || CultivationRecipeTab.category == null) return;
		if (dth$craftingTabs == null) {
			dth$craftingTabs = ImmutableList.<RecipeBookCategories>builder().addAll(cir.getReturnValue()).add(CultivationRecipeTab.category).build();
		}
		cir.setReturnValue(dth$craftingTabs);
	}
}
