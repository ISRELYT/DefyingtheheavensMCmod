package com.example.defyingtheheavens;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * A shielded core (puppet phase 3, the linked army; the design: "Where qi is suppressed, puppets drop to half strength. A shielded core
 * keeps them at full"): a demon core crafted with {@link #JADE} Jade and {@link #OBSIDIAN} Obsidian, shapeless, any grid that holds them,
 * gives the same core shielded (DemonCoreItem#shielded). Set in a puppet's frame, its puppet keeps its whole strength where it would be held
 * down (PuppetEntity#suppressed, RealmSuppressSystem).
 */
public class CoreShieldRecipe extends CustomRecipe {
	public static final RecipeSerializer<CoreShieldRecipe> SERIALIZER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
			DefyingTheHeavens.id("shield_core"), new SimpleCraftingRecipeSerializer<>(CoreShieldRecipe::new));
	public static final int JADE = 4, OBSIDIAN = 4;

	public CoreShieldRecipe(ResourceLocation id, CraftingBookCategory category) {
		super(id, category);
	}

	/** The one unshielded demon core in {@code grid}, if the rest is exactly the jade and obsidian; else EMPTY. */
	private static ItemStack core(CraftingContainer grid) {
		ItemStack core = ItemStack.EMPTY;
		int jade = 0, obsidian = 0;
		for (int i = 0; i < grid.getContainerSize(); i++) {
			ItemStack stack = grid.getItem(i);
			if (stack.isEmpty()) continue;
			if (stack.is(ModItems.DEMON_CORE)) {
				if (!core.isEmpty() || DemonCoreItem.shielded(stack)) return ItemStack.EMPTY;
				core = stack;
			} else if (stack.is(ModItems.JADE)) {
				jade++;
			} else if (stack.is(Items.OBSIDIAN)) {
				obsidian++;
			} else {
				return ItemStack.EMPTY;
			}
		}
		return jade == JADE && obsidian == OBSIDIAN ? core : ItemStack.EMPTY;
	}

	@Override
	public boolean matches(CraftingContainer grid, Level level) {
		return !core(grid).isEmpty();
	}

	@Override
	public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
		ItemStack core = core(grid);
		return core.isEmpty() ? ItemStack.EMPTY : DemonCoreItem.shield(core);
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 1 + JADE + OBSIDIAN;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	/** Touching this class registers the serializer. */
	public static void register() {
	}
}
