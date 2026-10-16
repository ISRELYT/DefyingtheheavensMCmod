package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.SpellAura;
import com.example.defyingtheheavens.client.ClientSpellAuras;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.SpinAttackEffectLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Steel Hurricane's spin is drawn as its own steel vortex (SpellAuraWorld), not a trident's riptide. */
@Mixin(SpinAttackEffectLayer.class)
public abstract class SpinAttackEffectLayerMixin {
	@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
			at = @At("HEAD"), cancellable = true)
	private void dth$steelHurricane(PoseStack pose, MultiBufferSource buffers, int light, LivingEntity entity, float walk, float walkSpeed, float partial, float age,
									float headYaw, float headPitch, CallbackInfo ci) {
		if (ClientSpellAuras.has(entity, SpellAura.STEEL_HURRICANE)) ci.cancel();
	}
}
