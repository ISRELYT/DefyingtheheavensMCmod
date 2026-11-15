package com.example.defyingtheheavens.client.compat.jei;

import com.example.defyingtheheavens.AlchemyRecipes;
import com.example.defyingtheheavens.ClothingItem;
import com.example.defyingtheheavens.ClothingRecipes;
import com.example.defyingtheheavens.ClothingStyle;
import com.example.defyingtheheavens.ClothingTier;
import com.example.defyingtheheavens.CraftMastery;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.ModItems;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.TalismanPattern;
import com.example.defyingtheheavens.client.BaublePanel;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI integration (the user's ask, 2026-11-13, playing with JEI: JEI to show beside the Heaven Altar, and the mod's crafting to show in it
 * and work). Only loaded when JEI is installed, through the "jei_mod_plugin" entrypoint in fabric.mod.json. JEI finds the mod's
 * crafting-table recipes by itself; this adds what it can't:
 * <ul>
 *   <li>the Alchemy Cauldron's brews (CauldronCategory), the Artifact Forge's workings and natal treasures' refinings (ForgeCategory) and the
 *   talismans drawn with the Inscription Brush (TalismanCategory), each beside what makes it;</li>
 *   <li>examples of the two special crafting recipes, dyeing cultivator clothing and raising it a grade (ClothingRecipes: they take any
 *   piece, so JEI, like the recipe book, can't list them), in JEI's crafting pages, where its button lays them in the grid;</li>
 *   <li>the bauble panel beside the inventory, kept clear of JEI's bookmarks.</li>
 * </ul>
 * Over the mod's framed screens (the Heaven Altar's, a storage formation's, a seized body's) JEI draws in the frame and reads its mouse
 * there (HallFrame#frameEvents, mixin/JeiMouseUtilMixin).
 */
public final class DthJeiPlugin implements IModPlugin {
	private static final ResourceLocation UID = DefyingTheHeavens.id("jei");
	/** The pages' words: JEI's own grey. */
	private static final int TEXT = 0xFF808080;
	/** A recipe arrow's size (IGuiHelper#getRecipeArrow). */
	static final int ARROW_W = 24, ARROW_H = 17;

	@Override
	public ResourceLocation getPluginUid() {
		return UID;
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
		registration.addRecipeCategories(new CauldronCategory(gui), new ForgeCategory(gui), new TalismanCategory(gui));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(CauldronCategory.TYPE, AlchemyRecipes.all());
		registration.addRecipes(ForgeCategory.TYPE, ForgeCategory.workings());
		registration.addRecipes(TalismanCategory.TYPE, List.of(TalismanPattern.values()));
		registration.addRecipes(RecipeTypes.CRAFTING, clothingExamples());
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(ModBlocks.ALCHEMY_CAULDRON, CauldronCategory.TYPE);
		registration.addRecipeCatalyst(ModBlocks.ARTIFACT_FORGE, ForgeCategory.TYPE);
		registration.addRecipeCatalyst(ModItems.INSCRIPTION_BRUSH, TalismanCategory.TYPE);
		registration.addRecipeCatalyst(ModItems.TALISMAN_PAPER, TalismanCategory.TYPE);
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		registration.addGuiContainerHandler(InventoryScreen.class, new IGuiContainerHandler<InventoryScreen>() {
			@Override
			public List<Rect2i> getGuiExtraAreas(InventoryScreen screen) {
				Rect2i panel = BaublePanel.shownArea();
				return panel == null ? List.of() : List.of(panel);
			}
		});
	}

	/**
	 * The special clothing recipes shown by example (ClothingRecipes): every piece with the dyes of each style, and every piece below
	 * Immortal with its grade's materials, raised a grade. Shown only: the grid's real recipes are the special ones, which these match.
	 */
	static List<CraftingRecipe> clothingExamples() {
		List<CraftingRecipe> examples = new ArrayList<>();
		for (ClothingTier tier : ClothingTier.values()) {
			for (ArmorItem.Type type : ArmorItem.Type.values()) {
				Item piece = ModItems.clothing(tier, type);
				if (piece == null) continue;
				String path = BuiltInRegistries.ITEM.getKey(piece).getPath();
				for (ClothingStyle style : ClothingStyle.values()) {
					NonNullList<Ingredient> ingredients = NonNullList.create();
					ingredients.add(Ingredient.of(piece));
					ingredients.addAll(ClothingRecipes.dyeIngredients(style));
					examples.add(new ShapelessRecipe(DefyingTheHeavens.id("jei/clothing_dye/" + path + "/" + style.getId()), "",
							CraftingBookCategory.EQUIPMENT, ClothingItem.withStyle(new ItemStack(piece), style), ingredients));
				}
				Item next = tier.isLast() ? null : ModItems.clothing(tier.next(), type);
				if (next == null) continue;
				NonNullList<Ingredient> ingredients = NonNullList.create();
				ingredients.add(Ingredient.of(piece));
				ingredients.addAll(ClothingRecipes.upgradeIngredients(tier));
				examples.add(new ShapelessRecipe(DefyingTheHeavens.id("jei/clothing_upgrade/" + path), "", CraftingBookCategory.EQUIPMENT,
						new ItemStack(next), ingredients));
			}
		}
		return examples;
	}

	// --- For the pages ---

	/** A line of a page's words, in JEI's grey. */
	static void line(GuiGraphics g, Component text, int x, int y) {
		g.drawString(Minecraft.getInstance().font, text, x, y, TEXT, false);
	}

	/** How hard a working is: never failing, or the rank of its craft from which it never fails (CraftMastery#FAIL_PER_RANK_SHORT). */
	static Component sureFrom(int difficulty) {
		if (difficulty <= 0) return Component.translatable(ModLang.JEI_NEVER_FAILS);
		CraftMastery.Rank[] ranks = CraftMastery.Rank.values();
		return Component.translatable(ModLang.JEI_SURE_FROM, ranks[Math.min(difficulty, ranks.length - 1)].getDisplayName());
	}

	/** The mouse over a recipe arrow drawn at ({@code x}, {@code y}). */
	static boolean overArrow(double mouseX, double mouseY, int x, int y) {
		return mouseX >= x && mouseX < x + ARROW_W && mouseY >= y && mouseY < y + ARROW_H;
	}

	/** How a station is worked, wrapped to a tooltip's width. */
	static void how(ITooltipBuilder tooltip, Component text) {
		for (FormattedText line : Minecraft.getInstance().font.getSplitter().splitLines(text, 200, Style.EMPTY)) tooltip.add(line);
	}
}
