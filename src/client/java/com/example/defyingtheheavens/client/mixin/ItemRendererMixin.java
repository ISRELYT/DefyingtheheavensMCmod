package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.SpiritStoneGlow;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Spirit stones glow wherever an item is drawn (see SpiritStoneGlow): lit from within, with a halo around them. */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
	/** Never darker than the stone's own light. */
	@ModifyVariable(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int dth$stoneLight(int light, ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack poseStack,
									MultiBufferSource buffers, int original, int overlay, BakedModel model) {
		return SpiritStoneGlow.light(stack, light);
	}

	/** The halo, once the display transform is in place (just before the model is drawn). */
	@Inject(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", shift = At.Shift.AFTER, ordinal = 0))
	private void dth$stoneHalo(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack poseStack, MultiBufferSource buffers,
									int light, int overlay, BakedModel model, CallbackInfo ci) {
		SpiritStoneGlow.draw(stack, context, poseStack, buffers);
	}
}
