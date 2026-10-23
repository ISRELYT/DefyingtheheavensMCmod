package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.ClientBloodlines;
import com.example.defyingtheheavens.client.ClientMeditationTracker;
import com.example.defyingtheheavens.client.MeteorFlightClient;
import com.example.defyingtheheavens.client.SwordFlightClient;
import com.example.defyingtheheavens.client.SwordStance;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A sword rider stands sideways on the blade: the body is turned square to the way the rider points
 * ({@link SwordFlightClient#riderYaw}), then a quarter turn more ({@link SwordStance}, which turns the head back).
 * PlayerRenderer's own setupRotations calls this one, so players and NPC cultivators both get it. One flying fast in Meteor Flight
 * is laid out along the way they look ({@link MeteorFlightClient#layOut}). A demon's body is drawn without the man's head, which its
 * beast's replaces (ClientBloodlines, BeastLayer).
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Shadow
	protected EntityModel<?> model;

	/** The man's head (and its hat layer) taken off while a demon's body is drawn: its beast's head is worn instead (BeastLayer). */
	@Unique
	private boolean dth$headOff;

	@Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
	private void dth$beastHeadOn(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light,
								 CallbackInfo ci) {
		dth$headOff = model instanceof HumanoidModel<?> humanoid && ClientBloodlines.look(entity) != null && humanoid.head.visible;
		if (dth$headOff) {
			HumanoidModel<?> humanoid = (HumanoidModel<?>) model;
			humanoid.head.visible = false;
			humanoid.hat.visible = false;
		}
	}

	@Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
					shift = At.Shift.AFTER))
	private void dth$beastHeadOff(LivingEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light,
								  CallbackInfo ci) {
		if (!dth$headOff) return;
		dth$headOff = false;
		HumanoidModel<?> humanoid = (HumanoidModel<?>) model;
		humanoid.head.visible = true;
		humanoid.hat.visible = true;
	}

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
