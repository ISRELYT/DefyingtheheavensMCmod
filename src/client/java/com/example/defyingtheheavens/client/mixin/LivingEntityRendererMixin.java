package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.ClientMeditationTracker;
import com.example.defyingtheheavens.client.MeteorFlightClient;
import com.example.defyingtheheavens.client.SwordFlightClient;
import com.example.defyingtheheavens.client.SwordStance;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A sword rider stands sideways on the blade: the body is turned square to the way the rider points
 * ({@link SwordFlightClient#riderYaw}), then a quarter turn more ({@link SwordStance}, which turns the head back).
 * PlayerRenderer's own setupRotations calls this one, so players and NPC cultivators both get it. One flying fast in Meteor Flight
 * is laid out along the way they look ({@link MeteorFlightClient#layOut}).
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Inject(method = "setupRotations", at = @At("TAIL"))
	private void dth$sidewaysOnTheSword(LivingEntity entity, PoseStack poseStack, float bob, float bodyYaw, float partialTick,
										CallbackInfo ci) {
		// From Core Formation the living float as they meditate, as the dead do (ClientMeditationTracker#hover).
		float hover = ClientMeditationTracker.hover(entity, partialTick);
		if (hover != 0) poseStack.translate(0, hover, 0);
		if (MeteorFlightClient.isStreaking(entity)) {
			MeteorFlightClient.layOut(entity, poseStack, partialTick);
			return;
		}
		if (!SwordFlightClient.isRiding(entity)) return;
		float turn = SwordStance.TURN_DEGREES + SwordFlightClient.bodyOffset(entity, partialTick);
		poseStack.mulPose(Axis.YP.rotationDegrees(turn));
	}
}
