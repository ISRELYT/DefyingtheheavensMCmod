package com.example.defyingtheheavens;

import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The two special crafting recipes for cultivator clothing (shapeless, any grid):
 * <ul>
 *   <li>{@code clothing_dye}: one piece plus white dye (righteous white), blue or light blue dye (neutral blue), or red dye
 *   and black dye together (demonic red and black).</li>
 *   <li>{@code clothing_upgrade}: one piece plus its grade's materials gives the same piece a grade higher, keeping its
 *   colours, enchantments, name and wear: Mortal to Spirit 2 Spirit Dew and a gold ingot; Spirit to Earth a Qi Vein and a
 *   diamond; Earth to Heaven 2 Qi Veins and netherite scrap; Heaven to Immortal 2 Qi Veins, a netherite ingot and a Spirit
 *   Ginseng.</li>
 * </ul>
 */
public final class ClothingRecipes {
	public static final RecipeSerializer<DyeRecipe> DYE = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
			DefyingTheHeavens.id("clothing_dye"), new SimpleCraftingRecipeSerializer<>(DyeRecipe::new));
	public static final RecipeSerializer<UpgradeRecipe> UPGRADE = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
			DefyingTheHeavens.id("clothing_upgrade"), new SimpleCraftingRecipeSerializer<>(UpgradeRecipe::new));

	/** What raising a piece from {@code tier} to the next grade takes, besides the piece. */
	public static List<Predicate<ItemStack>> upgradeMaterials(ClothingTier tier) {
		Predicate<ItemStack> dew = stack -> stack.is(ModItems.SPIRIT_DEW);
		Predicate<ItemStack> vein = stack -> stack.is(ModTags.QI_VEINS);
		return switch (tier) {
			case MORTAL -> List.of(dew, dew, stack -> stack.is(Items.GOLD_INGOT));
			case SPIRIT -> List.of(vein, stack -> stack.is(Items.DIAMOND));
			case EARTH -> List.of(vein, vein, stack -> stack.is(Items.NETHERITE_SCRAP));
			case HEAVEN -> List.of(vein, vein, stack -> stack.is(Items.NETHERITE_INGOT), stack -> stack.is(ModItems.SPIRIT_GINSENG));
			case IMMORTAL -> List.of();
		};
	}

	/** The one clothing piece in the grid, or EMPTY if there are none or several. */
	private static ItemStack onePiece(CraftingContainer grid, List<ItemStack> others) {
		ItemStack piece = ItemStack.EMPTY;
		for (int i = 0; i < grid.getContainerSize(); i++) {
			ItemStack stack = grid.getItem(i);
			if (stack.isEmpty()) continue;
			if (stack.getItem() instanceof ClothingItem) {
				if (!piece.isEmpty()) return ItemStack.EMPTY;
				piece = stack;
			} else {
				others.add(stack);
			}
		}
		return piece;
	}

	/** Every predicate matched by a different stack, and no stack left over. */
	private static boolean exactly(List<ItemStack> stacks, List<Predicate<ItemStack>> wanted) {
		if (stacks.size() != wanted.size()) return false;
		List<Predicate<ItemStack>> left = new ArrayList<>(wanted);
		for (ItemStack stack : stacks) {
			boolean used = false;
			for (int i = 0; i < left.size(); i++) {
				if (left.get(i).test(stack)) {
					left.remove(i);
					used = true;
					break;
				}
			}
			if (!used) return false;
		}
		return left.isEmpty();
	}

	/** The style the dyes in {@code dyes} make, or null if they make none. */
	static ClothingStyle styleOfDyes(List<ItemStack> dyes) {
		if (dyes.size() == 1 && dyes.get(0).is(Items.WHITE_DYE)) return ClothingStyle.RIGHTEOUS;
		if (dyes.size() == 1 && (dyes.get(0).is(Items.BLUE_DYE) || dyes.get(0).is(Items.LIGHT_BLUE_DYE))) return ClothingStyle.NEUTRAL;
		if (exactly(dyes, List.of(stack -> stack.is(Items.RED_DYE), stack -> stack.is(Items.BLACK_DYE)))) return ClothingStyle.DEMONIC;
		return null;
	}

	public static class DyeRecipe extends CustomRecipe {
		public DyeRecipe(ResourceLocation id, CraftingBookCategory category) {
			super(id, category);
		}

		@Override
		public boolean matches(CraftingContainer grid, Level level) {
			List<ItemStack> dyes = new ArrayList<>();
			ItemStack piece = onePiece(grid, dyes);
			ClothingStyle style = styleOfDyes(dyes);
			return !piece.isEmpty() && style != null && style != ClothingItem.style(piece);
		}

		@Override
		public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
			List<ItemStack> dyes = new ArrayList<>();
			ItemStack piece = onePiece(grid, dyes);
			ClothingStyle style = styleOfDyes(dyes);
			if (piece.isEmpty() || style == null) return ItemStack.EMPTY;
			ItemStack result = piece.copyWithCount(1);
			return ClothingItem.withStyle(result, style);
		}

		@Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
		@Override public RecipeSerializer<?> getSerializer() { return DYE; }
	}

	public static class UpgradeRecipe extends CustomRecipe {
		public UpgradeRecipe(ResourceLocation id, CraftingBookCategory category) {
			super(id, category);
		}

		@Override
		public boolean matches(CraftingContainer grid, Level level) {
			List<ItemStack> others = new ArrayList<>();
			ItemStack piece = onePiece(grid, others);
			if (piece.isEmpty() || !(piece.getItem() instanceof ClothingItem clothing) || clothing.getTier().isLast()) return false;
			return exactly(others, upgradeMaterials(clothing.getTier()));
		}

		@Override
		public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
			List<ItemStack> others = new ArrayList<>();
			ItemStack piece = onePiece(grid, others);
			if (piece.isEmpty() || !(piece.getItem() instanceof ClothingItem clothing) || clothing.getTier().isLast()) return ItemStack.EMPTY;
			Item next = ModItems.clothing(clothing.getTier().next(), clothing.getType());
			ItemStack result = new ItemStack(next);
			if (piece.hasTag()) result.setTag(piece.getTag().copy()); // colours, enchantments, a custom name
			// The same share of wear on the sturdier piece.
			if (piece.isDamaged() && piece.getMaxDamage() > 0) {
				result.setDamageValue((int) Math.round(piece.getDamageValue() / (double) piece.getMaxDamage() * result.getMaxDamage()));
			}
			return result;
		}

		@Override
		public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid) {
			return NonNullList.withSize(grid.getContainerSize(), ItemStack.EMPTY);
		}

		@Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
		@Override public RecipeSerializer<?> getSerializer() { return UPGRADE; }
	}

	/** Touching this class registers the serializers. */
	public static void register() {
	}

	private ClothingRecipes() {}
}
