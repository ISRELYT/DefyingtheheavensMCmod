package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.Concealments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A player walking through hidden work (Concealments: it doesn't block them) isn't smothered in a wall they don't see. */
@Mixin(Entity.class)
public abstract class EntityVeilMixin {
	@Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
	private void dth$breathesInHidden(CallbackInfoReturnable<Boolean> cir) {
		if (Concealments.any && (Object) this instanceof ServerPlayer player && Concealments.breathesInHidden(player)) cir.setReturnValue(false);
	}
}
