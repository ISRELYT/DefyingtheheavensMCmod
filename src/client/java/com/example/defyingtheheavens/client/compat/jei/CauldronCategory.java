package com.example.defyingtheheavens.client.compat.jei;

import com.example.defyingtheheavens.AlchemyRecipes;
import com.example.defyingtheheavens.CultivationFruitItem;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.GinsengItem;
import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.ModLang;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JEI's page of the Alchemy Cauldron's brews (AlchemyRecipes): each kind of ingredient and how many, the pill, how long over the fire and how
 * hard it is (the rank of Alchemy from which it never fails: CraftMastery). A rarer herb stands in for the ordinary one (Purple Lingzhi for
 * Lingzhi...), so its slot shows both; an aged ingredient's slot says its years make the pill's grade. How the cauldron is worked is told over
 * the arrow.
 */
final class CauldronCategory implements IRecipeCategory<AlchemyRecipes.Recipe> {
	static final RecipeType<AlchemyRecipes.Recipe> TYPE = RecipeType.create(DefyingTheHeavens.MOD_ID, "alchemy", AlchemyRecipes.Recipe.class);
	private static final int WIDTH = 150, HEIGHT = 60, ARROW_X = 62, ARROW_Y = 10, OUT_X = 98, OUT_Y = 10;

	private final IGuiHelper gui;
	private final IDrawable icon;
	/** The arrow fills in a brew's own time. */
	private final Map<Integer, IDrawableAnimated> arrows = new HashMap<>();

	CauldronCategory(IGuiHelper gui) {
		this.gui = gui;
		icon = gui.createDrawableItemLike(ModBlocks.ALCHEMY_CAULDRON);
	}

	@Override
	public RecipeType<AlchemyRecipes.Recipe> getRecipeType() {
		return TYPE;
	}

	@Override
	public Component getTitle() {
		return ModBlocks.ALCHEMY_CAULDRON.getName();
	}

	@Override
	public int getWidth() {
		return WIDTH;
	}

	@Override
	public int getHeight() {
		return HEIGHT;
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, AlchemyRecipes.Recipe recipe, IFocusGroup focuses) {
		int i = 0;
		for (Map.Entry<Item, Integer> entry : recipe.ingredients().entrySet()) {
			Item item = entry.getKey();
			IRecipeSlotBuilder slot = builder.addInputSlot(1 + (i % 3) * 18, 1 + (i / 3) * 18).setStandardSlotBackground()
					.addItemStacks(withRarer(item, entry.getValue()));
			if (item instanceof GinsengItem || item instanceof CultivationFruitItem) {
				slot.addRichTooltipCallback((view, tooltip) -> {
					tooltip.add(Component.translatable(ModLang.JEI_AGED).withStyle(ChatFormatting.GRAY));
					if (view.getDisplayedItemStack().filter(shown -> AlchemyRecipes.substituteFor(shown.getItem()) != null).isPresent()) {
						tooltip.add(Component.translatable(ModLang.JEI_RARE_HERB).withStyle(ChatFormatting.GRAY));
					}
				});
			}
			i++;
		}
		builder.addOutputSlot(OUT_X, OUT_Y).setOutputSlotBackground().addItemStack(new ItemStack(recipe.output()));
	}

	/** {@code count} of {@code item}, and of every rarer herb that stands in for it. */
	private static List<ItemStack> withRarer(Item item, int count) {
		List<ItemStack> stacks = new ArrayList<>();
		stacks.add(new ItemStack(item, count));
		for (Item other : BuiltInRegistries.ITEM) {
			if (AlchemyRecipes.substituteFor(other) == item) stacks.add(new ItemStack(other, count));
		}
		return stacks;
	}

	@Override
	public void draw(AlchemyRecipes.Recipe recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
		arrows.computeIfAbsent(recipe.brewTicks(), gui::createAnimatedRecipeArrow).draw(g, ARROW_X, ARROW_Y);
		DthJeiPlugin.line(g, Component.translatable(ModLang.JEI_BREW_TIME, recipe.brewTicks() / 20), 1, 40);
		DthJeiPlugin.line(g, DthJeiPlugin.sureFrom(recipe.difficulty()), 1, 50);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, AlchemyRecipes.Recipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
		if (DthJeiPlugin.overArrow(mouseX, mouseY, ARROW_X, ARROW_Y)) DthJeiPlugin.how(tooltip, Component.translatable(ModLang.JEI_CAULDRON_HOW));
	}
}
