package com.example.defyingtheheavens;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;

/**
 * The mod's recipes in the recipe book: every one of them (by its id's namespace) known to every player from the moment they
 * join, so the book shows them all, in a tab of their own (the client's CultivationRecipeTab), without first having to come by
 * an ingredient. A recipe added later is learned at the next join. The special recipes (dyeing and upgrading clothing) work on
 * any piece, so, like vanilla's own (fireworks, banners), the book can't show them; nor are the Alchemy Cauldron's brews
 * recipes of the crafting table.
 */
public final class ModRecipeBook {
	/** Teaches {@code player} every recipe of the mod they don't know yet (the toast says so the first time). */
	public static void unlock(ServerPlayer player) {
		List<Recipe<?>> ours = player.server.getRecipeManager().getRecipes().stream()
				.filter(recipe -> recipe.getId().getNamespace().equals(DefyingTheHeavens.MOD_ID))
				.toList();
		player.awardRecipes(ours);
	}

	private ModRecipeBook() {}
}
