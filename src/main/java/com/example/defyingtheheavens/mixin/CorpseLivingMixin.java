package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.Corpses;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** What a death drops (LivingEntity#die: a cultivator's) goes into the body it leaves, if it leaves one (Corpses#drops). */
@Mixin(LivingEntity.class)
public abstract class CorpseLivingMixin {
	@WrapOperation(method = "die", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;dropAllDeathLoot(Lnet/minecraft/world/damagesource/DamageSource;)V"))
	private void dth$intoTheBody(LivingEntity self, DamageSource source, Operation<Void> drop) {
		Corpses.drops(self, () -> drop.call(self, source));
	}
}
