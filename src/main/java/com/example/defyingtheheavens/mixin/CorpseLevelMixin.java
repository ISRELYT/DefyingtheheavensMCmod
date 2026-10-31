package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.Corpses;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** An item a death drops while its body is being laid goes into the body, not into the world (Corpses#take). */
@Mixin(ServerLevel.class)
public abstract class CorpseLevelMixin {
	@Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
	private void dth$caughtForTheBody(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (Corpses.take((ServerLevel) (Object) this, entity)) cir.setReturnValue(false);
	}
}
