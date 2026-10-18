package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.ClientJudgeGaze;
import com.example.defyingtheheavens.client.ClientSoul;
import com.example.defyingtheheavens.client.SenseOverlay;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/**
 * Under a shader pack, the realm's own sky laid over the pack's (SkyLayer); then Qi Sense's monochrome and the senses' overlay,
 * once the world and hand are drawn: just before vanilla lays the entity outlines over the frame, so the Consciousness Domain's
 * outlines keep their colour. And the view warped by the Chief Judge's gaze on the Soul River (ClientJudgeGaze).
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "render(FJZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V"))
	private void dth$senses(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
		com.example.defyingtheheavens.client.SkyLayer.composite();
		SenseOverlay.beforeEntityOutline(partialTicks);
	}

	/**
	 * A soul possessing a body looks out from inside it (its eyes sit at the body's, within the body's own box), and vanilla's pick
	 * takes a box holding the eyes for a hit whenever that box comes later in the level's entity list than the true target: then
	 * every swing landed on the host itself and was thrown away (so a creature possessed and moved about, then taken again by any
	 * soul, struck nothing). The body it is in is never what its crosshair is on.
	 */
	@ModifyArg(method = "pick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"),
			index = 4)
	private Predicate<Entity> dth$notTheBodyItIsIn(Predicate<Entity> pickable) {
		Minecraft mc = Minecraft.getInstance();
		Entity host = mc.player == null || !ClientSoul.isOut() ? null : mc.player.getVehicle();
		return host == null ? pickable : pickable.and(entity -> entity != host);
	}

	/** A projected soul touches no block, so none is outlined under its gaze. */
	@Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
	private void dth$soulTouchesNoBlock(CallbackInfoReturnable<Boolean> cir) {
		if (ClientSoul.isOut()) cir.setReturnValue(false);
	}

	/** Meeting the Chief Judge's gaze (ClientJudgeGaze): the view rolls and lurches. */
	@Inject(method = "bobHurt", at = @At("TAIL"))
	private void dth$judgeGazeRolls(PoseStack pose, float partialTicks, CallbackInfo ci) {
		ClientJudgeGaze.distort(pose, partialTicks);
		com.example.defyingtheheavens.client.ClientSpellFx.shake(pose, partialTicks); // a blow or a quake near shakes the view
	}

	/** ...and breathes in and out. */
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void dth$judgeGazeBreathes(Camera camera, float partialTicks, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
		double scale = ClientJudgeGaze.fovScale();
		if (scale != 1) cir.setReturnValue(cir.getReturnValue() * scale);
	}
}
