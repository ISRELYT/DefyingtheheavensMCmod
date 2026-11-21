package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.cinematic.HeroFx;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While a breakthrough cinematic's storm covers the sky (its cloud deck, HeroFx), vanilla's own white clouds are left out: they showed
 * through the storm's edge and under it, a fair-weather sky inside the tribulation's. Only the drawing is skipped; nothing of the
 * weather or of anyone's settings changes, and they're back the moment the storm is.
 */
@Mixin(LevelRenderer.class)
public abstract class CinematicCloudsMixin {
	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	private void dth$noFairCloudsInTheStorm(PoseStack poseStack, Matrix4f projection, float partialTick, double camX, double camY, double camZ,
											CallbackInfo ci) {
		if (HeroFx.coversTheSky()) ci.cancel();
	}
}
