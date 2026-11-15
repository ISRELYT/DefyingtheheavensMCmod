package com.example.defyingtheheavens.client.compat.jei;

import com.example.defyingtheheavens.ArtifactGrade;
import com.example.defyingtheheavens.ArtifactRecipes;
import com.example.defyingtheheavens.ArtifactWeaponItem;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.DemonCoreItem;
import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.ModItems;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.QiElement;
import com.example.defyingtheheavens.Stage;
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
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JEI's page of the Artifact Forge's workings (ArtifactRecipes): the materials, the demon core a working wants from Spirit grade up (one of
 * the grade's realm or above; for a weapon made of its element, of that element) or the element's token a Mortal banner takes, and the
 * artifact; how long over the fire, the qi it draws from its forger each second, and the rank of Forging from which it never fails. And the
 * natal treasures' refinings (NatalTreasures): one's own natal blade and the next grade's materials of its kind raise the blade itself a
 * grade. How the forge is worked is told over the arrow.
 */
final class ForgeCategory implements IRecipeCategory<ForgeCategory.Working> {
	/** One working: a forging, or, with {@code blade}, that natal treasure raised to the recipe's grade. */
	record Working(ArtifactRecipes.Recipe recipe, @Nullable Item blade) {}

	static final RecipeType<Working> TYPE = RecipeType.create(DefyingTheHeavens.MOD_ID, "artifact_forge", Working.class);
	private static final int WIDTH = 150, HEIGHT = 80, ARROW_X = 62, ARROW_Y = 10, OUT_X = 98, OUT_Y = 10;

	private final IGuiHelper gui;
	private final IDrawable icon;
	private final Map<Integer, IDrawableAnimated> arrows = new HashMap<>();

	ForgeCategory(IGuiHelper gui) {
		this.gui = gui;
		icon = gui.createDrawableItemLike(ModBlocks.ARTIFACT_FORGE);
	}

	/** Every forging, then every refining: a weapon one grade below a forging of its kind, raised to it. */
	static List<Working> workings() {
		List<Working> workings = new ArrayList<>();
		for (ArtifactRecipes.Recipe recipe : ArtifactRecipes.all()) workings.add(new Working(recipe, null));
		for (ArtifactRecipes.Recipe recipe : ArtifactRecipes.all()) {
			if (recipe.grade().ordinal() == 0) continue;
			Item below = ModItems.artifact(recipe.kind(), ArtifactGrade.values()[recipe.grade().ordinal() - 1]);
			if (below instanceof ArtifactWeaponItem) workings.add(new Working(recipe, below));
		}
		return workings;
	}

	@Override
	public RecipeType<Working> getRecipeType() {
		return TYPE;
	}

	@Override
	public Component getTitle() {
		return ModBlocks.ARTIFACT_FORGE.getName();
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
	public void setRecipe(IRecipeLayoutBuilder builder, Working working, IFocusGroup focuses) {
		ArtifactRecipes.Recipe recipe = working.recipe();
		int i = 0;
		if (working.blade() != null) {
			note(slot(builder, i++).addItemStack(new ItemStack(working.blade())), Component.translatable(ModLang.JEI_NATAL_IN));
		}
		for (Map.Entry<Item, Integer> entry : recipe.materials().entrySet()) slot(builder, i++).addItemStack(new ItemStack(entry.getKey(), entry.getValue()));
		if (recipe.core()) {
			Component realm = recipe.grade().coreRealm().getDisplayName();
			note(slot(builder, i++).addItemStacks(cores(recipe)), recipe.kind().isElemental()
					? Component.translatable(ModLang.JEI_ELEMENT_CORE, recipe.kind().element().getDisplayName(), realm)
					: Component.translatable(ModLang.JEI_CORE, realm));
		}
		if (recipe.token()) note(slot(builder, i++).addItemStacks(tokens()), Component.translatable(ModLang.JEI_TOKEN));
		IRecipeSlotBuilder out = builder.addOutputSlot(OUT_X, OUT_Y).setOutputSlotBackground().addItemStack(new ItemStack(recipe.output()));
		if (working.blade() != null) note(out, Component.translatable(ModLang.JEI_NATAL_OUT));
	}

	/** The {@code i}th material's slot: three across, two down. */
	private static IRecipeSlotBuilder slot(IRecipeLayoutBuilder builder, int i) {
		return builder.addInputSlot(1 + (i % 3) * 18, 1 + (i / 3) * 18).setStandardSlotBackground();
	}

	private static void note(IRecipeSlotBuilder slot, Component text) {
		slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(text.copy().withStyle(ChatFormatting.GRAY)));
	}

	/** Cores of the grade's realm (the least a working takes: any above it does too), of each element, or of the weapon's own. */
	private static List<ItemStack> cores(ArtifactRecipes.Recipe recipe) {
		int rank = recipe.grade().coreRealm().ordinal() * Stage.values().length;
		QiElement[] elements = recipe.kind().isElemental() ? new QiElement[] {recipe.kind().element()} : QiElement.five();
		List<ItemStack> cores = new ArrayList<>();
		for (QiElement element : elements) cores.add(DemonCoreItem.of(rank, element, null));
		return cores;
	}

	/** The elements' tokens a Mortal banner takes (ArtifactRecipes#tokenElement). */
	private static List<ItemStack> tokens() {
		List<ItemStack> tokens = new ArrayList<>(List.of(new ItemStack(Items.BLAZE_POWDER), new ItemStack(Items.PRISMARINE_SHARD)));
		BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.SAPLINGS).forEach(sapling -> tokens.add(new ItemStack(sapling)));
		if (tokens.size() == 2) tokens.add(new ItemStack(Items.OAK_SAPLING));
		tokens.add(new ItemStack(Items.CLAY_BALL));
		tokens.add(new ItemStack(Items.IRON_NUGGET));
		return tokens;
	}

	@Override
	public void draw(Working working, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
		ArtifactGrade grade = working.recipe().grade();
		arrows.computeIfAbsent(grade.seconds() * 20, gui::createAnimatedRecipeArrow).draw(g, ARROW_X, ARROW_Y);
		int y = 40;
		if (working.blade() != null) {
			DthJeiPlugin.line(g, Component.translatable(ModLang.JEI_REFINING, grade.getDisplayName()), 1, y);
			y += 10;
		}
		DthJeiPlugin.line(g, Component.translatable(ModLang.JEI_FORGE_TIME, grade.getDisplayName(), grade.seconds()), 1, y);
		DthJeiPlugin.line(g, Component.translatable(ModLang.JEI_FORGE_QI, number(grade.qiPerSecond())), 1, y + 10);
		DthJeiPlugin.line(g, DthJeiPlugin.sureFrom(grade.difficulty()), 1, y + 20);
	}

	private static String number(double v) {
		return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, Working working, IRecipeSlotsView slots, double mouseX, double mouseY) {
		if (DthJeiPlugin.overArrow(mouseX, mouseY, ARROW_X, ARROW_Y)) DthJeiPlugin.how(tooltip, Component.translatable(ModLang.JEI_FORGE_HOW));
	}
}
