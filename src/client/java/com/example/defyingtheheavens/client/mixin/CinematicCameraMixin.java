package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.cinematic.CinematicCamera;
import com.example.defyingtheheavens.client.cinematic.CinematicClient;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A breakthrough cinematic's camera (CinematicCamera): set once vanilla has set up its own, so everything that reads the camera (the
 * world, the sky, the particles, the culling) sees the shot. The roll and the FOV go through GameRendererMixin with the mod's others.
 */
@Mixin(Camera.class)
public abstract class CinematicCameraMixin {
	@Shadow
	protected abstract void setPosition(Vec3 pos);

	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	@Inject(method = "setup", at = @At("TAIL"))
	private void dth$cinematic(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
		CinematicCamera.View view = CinematicClient.view(partialTick);
		if (view == null) return;
		setRotation(view.yaw(), view.pitch());
		setPosition(view.pos());
	}
}
