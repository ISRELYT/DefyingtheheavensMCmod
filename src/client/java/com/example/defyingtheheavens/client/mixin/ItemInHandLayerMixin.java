package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.SwordFlightClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A sword rider's sword is under their feet (SwordFlightClient draws it there), so not in their hand as well. A polished blade
 * (Blade Polish) wisps with white smoke (BladeWisps).
 */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
	@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
	private void dth$swordUnderfoot(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm, PoseStack poseStack,
									MultiBufferSource buffers, int light, CallbackInfo ci) {
		if (arm == entity.getMainArm() && SwordFlightClient.isRiding(entity)) ci.cancel();
	}

	@Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
	private void dth$polishedBlade(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm, PoseStack poseStack,
								   MultiBufferSource buffers, int light, CallbackInfo ci) {
		com.example.defyingtheheavens.client.BladeWisps.draw(entity, stack, context, arm == HumanoidArm.LEFT, poseStack, buffers);
	}
}
