package com.example.defyingtheheavens.client.compat.jei;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.ModItems;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.TalismanPattern;
import com.example.defyingtheheavens.Talismans;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * JEI's page of the talismans the Inscription Brush draws on Talisman Paper (TalismanPattern, Talismans): the brush in the other hand, the
 * paper, its redstone (the cinnabar) and the pattern's own extras, and the talismans one drawing makes; the share of one's qi pool it takes
 * and the rank of Talismans from which it never fails. How a talisman is drawn is told over the arrow.
 */
final class TalismanCategory implements IRecipeCategory<TalismanPattern> {
	static final RecipeType<TalismanPattern> TYPE = RecipeType.create(DefyingTheHeavens.MOD_ID, "talismans", TalismanPattern.class);
	private static final int WIDTH = 150, HEIGHT = 44, ARROW_X = 98, ARROW_Y = 1, OUT_X = 128, OUT_Y = 1;

	private final IDrawable icon;
	/** A drawing takes Talismans#DRAW_TICKS, the use held. */
	private final IDrawableAnimated arrow;

	TalismanCategory(IGuiHelper gui) {
		icon = gui.createDrawableItemLike(ModItems.INSCRIPTION_BRUSH);
		arrow = gui.createAnimatedRecipeArrow(Talismans.DRAW_TICKS);
	}

	@Override
	public RecipeType<TalismanPattern> getRecipeType() {
		return TYPE;
	}

	@Override
	public Component getTitle() {
		return Component.translatable(ModLang.craftName("talismans"));
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
	public void setRecipe(IRecipeLayoutBuilder builder, TalismanPattern pattern, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.CATALYST, 1, 1).setStandardSlotBackground().addItemStack(new ItemStack(ModItems.INSCRIPTION_BRUSH))
				.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(ModLang.JEI_BRUSH).withStyle(ChatFormatting.GRAY)));
		int x = 23;
		builder.addInputSlot(x, 1).setStandardSlotBackground().addItemStack(new ItemStack(ModItems.TALISMAN_PAPER));
		builder.addInputSlot(x += 18, 1).setStandardSlotBackground().addItemStack(new ItemStack(Items.REDSTONE));
		for (Item extra : pattern.extras()) builder.addInputSlot(x += 18, 1).setStandardSlotBackground().addItemStack(new ItemStack(extra));
		builder.addOutputSlot(OUT_X, OUT_Y).setOutputSlotBackground().addItemStack(new ItemStack(pattern.item(), pattern.count));
	}

	@Override
	public void draw(TalismanPattern pattern, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
		arrow.draw(g, ARROW_X, ARROW_Y);
		DthJeiPlugin.line(g, Component.translatable(ModLang.JEI_TALISMAN_QI, Math.round(pattern.qiShare * 100)), 1, 24);
		DthJeiPlugin.line(g, DthJeiPlugin.sureFrom(pattern.difficulty), 1, 34);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, TalismanPattern pattern, IRecipeSlotsView slots, double mouseX, double mouseY) {
		if (DthJeiPlugin.overArrow(mouseX, mouseY, ARROW_X, ARROW_Y)) DthJeiPlugin.how(tooltip, Component.translatable(ModLang.JEI_TALISMAN_HOW));
	}
}
