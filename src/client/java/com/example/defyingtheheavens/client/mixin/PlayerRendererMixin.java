package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.GhostSystem;
import com.example.defyingtheheavens.client.SoulRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A ghost's own hand in first person (empty-handed, or holding a map) is the dead's cyan light, as the rest of it is to everyone
 * else (SoulRenderer#drawGhostArm): drawn once the model is posed for the hand, in place of the skin.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
	@Inject(method = "renderHand", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/model/PlayerModel;setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
			shift = At.Shift.AFTER), cancellable = true)
	private void dth$ghostlyHand(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, ModelPart arm,
								 ModelPart sleeve, CallbackInfo ci) {
		if (!GhostSystem.isGhost(player)) return;
		SoulRenderer.drawGhostArm(pose, buffers, player.getSkinTextureLocation(), arm, sleeve, light);
		ci.cancel();
	}
}
