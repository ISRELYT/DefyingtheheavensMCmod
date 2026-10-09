package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.SenseOverlay;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Qi Sense's monochrome and the senses' overlay, once the world and hand are drawn: just before vanilla lays the entity
 * outlines over the frame, so the Consciousness Domain's outlines keep their colour.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "render(FJZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V"))
	private void dth$senses(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
		SenseOverlay.beforeEntityOutline(partialTicks);
	}
}
