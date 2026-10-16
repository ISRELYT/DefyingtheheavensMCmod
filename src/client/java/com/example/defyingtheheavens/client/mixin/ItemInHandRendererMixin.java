package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.ClientSoul;
import com.example.defyingtheheavens.client.SwordFlightClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First person: while riding, the sword is under your feet (look down to see it), not in your hand. A projected soul has no
 * hands to show at all.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
	private void dth$swordUnderfoot(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swingProgress,
									ItemStack stack, float equipProgress, PoseStack poseStack, MultiBufferSource buffers, int light,
									CallbackInfo ci) {
		if (hand == InteractionHand.MAIN_HAND && SwordFlightClient.isRiding(player)) ci.cancel();
	}

	/** A polished blade in your own hand wisps with white smoke (BladeWisps), as others see it. */
	@Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
	private void dth$polishedBlade(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swingProgress,
								   ItemStack stack, float equipProgress, PoseStack poseStack, MultiBufferSource buffers, int light,
								   CallbackInfo ci) {
		boolean right = (hand == InteractionHand.MAIN_HAND) == (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
		com.example.defyingtheheavens.client.BladeWisps.draw(player, stack, right ? net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
				: net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !right, poseStack, buffers);
	}

	@Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void dth$soulHasNoHands(float partialTick, PoseStack poseStack, MultiBufferSource.BufferSource buffers, LocalPlayer player, int light,
									CallbackInfo ci) {
		if (ClientSoul.isOut()) ci.cancel();
	}
}
