package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.UpperRealmClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Custom fog distances for the Upper Realm and Spatial Gap (open air only; water, lava, blindness keep vanilla fog). */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
	@Inject(method = "setupFog", at = @At("TAIL"))
	private static void dth$dimensionFog(Camera camera, FogRenderer.FogMode mode, float farPlaneDistance, boolean thickFog,
			float partialTick, CallbackInfo ci) {
		if (mode != FogRenderer.FogMode.FOG_TERRAIN || camera.getFluidInCamera() != FogType.NONE) return;
		Entity entity = camera.getEntity();
		if (entity == null || entity.level() == null) return;
		if (entity instanceof LivingEntity living && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) return;
		float[] fog = UpperRealmClient.fogFor(entity.level().dimension());
		if (fog == null) return;
		RenderSystem.setShaderFogStart(farPlaneDistance * fog[0]);
		RenderSystem.setShaderFogEnd(farPlaneDistance * fog[1]);
	}
}
