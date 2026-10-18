package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.Possession;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A soul in another's body (Possession) earns what it earns quietly: the world isn't told, under the body's name, of advancements
 * that are the soul's own (taking up the body's diamonds made it "Diamonds!", say).
 */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
	@Shadow private ServerPlayer player;

	@ModifyExpressionValue(method = "award", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
	private boolean dth$quietInAnothersBody(boolean announce) {
		return announce && !Possession.inhabits(player.getUUID());
	}
}
