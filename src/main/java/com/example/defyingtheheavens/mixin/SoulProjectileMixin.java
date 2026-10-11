package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.SoulProjection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Arrows, tridents and fireballs pass through a projected soul (see SoulProjection): only a cultivator's own hand can reach it. */
@Mixin(Projectile.class)
public abstract class SoulProjectileMixin {
	@Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
	private void dth$passThroughSouls(Entity target, CallbackInfoReturnable<Boolean> cir) {
		if (target instanceof Player player && SoulProjection.isSoul(player)) cir.setReturnValue(false);
	}
}
