package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.Corpses;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** What a player's death drops (ServerPlayer#die, which drops on its own) goes into the body they leave (Corpses#drops). */
@Mixin(ServerPlayer.class)
public abstract class CorpsePlayerMixin {
	@WrapOperation(method = "die", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerPlayer;dropAllDeathLoot(Lnet/minecraft/world/damagesource/DamageSource;)V"))
	private void dth$intoTheBody(ServerPlayer self, DamageSource source, Operation<Void> drop) {
		Corpses.drops(self, () -> drop.call(self, source));
	}
}
