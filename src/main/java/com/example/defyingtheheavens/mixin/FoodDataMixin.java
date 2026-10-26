package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.ChaosFrontier;
import com.example.defyingtheheavens.CultivationStats;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Natural regeneration at the realm's pace (the user's ask, 2026-10-22: the great bodies mended at vanilla's two a second, hours to
 * heal): every heal vanilla gives a fed body, quick or slow, {@link CultivationStats#mendFactor} times over. What it costs in food
 * stays vanilla's. Out in the Chaos Frontier's storm unsheltered (ChaosFrontier#exposed) the body does not mend of itself, nor is
 * the food for it spent.
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"))
	private void dth$mendAtTheRealmsPace(Player player, float amount, Operation<Void> original) {
		if (player instanceof ServerPlayer exposed && ChaosFrontier.exposed(exposed)) return;
		original.call(player, player instanceof ServerPlayer p ? (float) (amount * CultivationStats.mendFactor(p)) : amount);
	}

	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V"))
	private void dth$noFoodForNoMending(FoodData food, float exhaustion, Operation<Void> original, @Local(argsOnly = true) Player player) {
		if (player instanceof ServerPlayer exposed && ChaosFrontier.exposed(exposed)) return;
		original.call(food, exhaustion);
	}
}
