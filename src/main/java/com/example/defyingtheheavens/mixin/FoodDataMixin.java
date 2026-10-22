package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.CultivationStats;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Natural regeneration at the realm's pace (the user's ask, 2026-10-22: the great bodies mended at vanilla's two a second, hours to
 * heal): every heal vanilla gives a fed body, quick or slow, {@link CultivationStats#mendFactor} times over. What it costs in food
 * stays vanilla's.
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"))
	private void dth$mendAtTheRealmsPace(Player player, float amount, Operation<Void> original) {
		original.call(player, player instanceof ServerPlayer p ? (float) (amount * CultivationStats.mendFactor(p)) : amount);
	}
}
