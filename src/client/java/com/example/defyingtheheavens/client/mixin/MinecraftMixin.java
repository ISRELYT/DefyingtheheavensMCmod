package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.ConsciousnessRenderer;
import com.example.defyingtheheavens.client.TalkReach;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Consciousness Domain: living things inside the domain show through walls (coloured by {@link LevelRendererMixin}). And a
 * right-click on a cultivator a few steps off starts a talk (TalkReach), before vanilla finds nothing within arm's reach.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Shadow private int rightClickDelay;

	@Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
	private void dth$consciousnessOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (ConsciousnessRenderer.isHighlighted(entity)) cir.setReturnValue(true);
	}

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void dth$talkFromAfar(CallbackInfo ci) {
		if (TalkReach.tryTalk((Minecraft) (Object) this)) {
			rightClickDelay = 4;
			ci.cancel();
		}
	}
}
