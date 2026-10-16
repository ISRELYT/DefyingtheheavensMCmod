package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.ClientGhosts;
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
 * else (SoulRenderer#drawGhostArm): drawn once the model is posed for the hand, in place of the skin. It goes by the look
 * (ClientGhosts#drawnAsLight): the living in a Ghost King's form too, the dead in a human form not.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
	@Inject(method = "renderHand", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/model/PlayerModel;setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
			shift = At.Shift.AFTER), cancellable = true)
	private void dth$ghostlyHand(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, ModelPart arm,
								 ModelPart sleeve, CallbackInfo ci) {
		if (!ClientGhosts.drawnAsLight(player)) return; // a form is a look: a Ghost King's form's hand too, a human form's not
		SoulRenderer.drawGhostArm(pose, buffers, player.getSkinTextureLocation(), arm, sleeve, light);
		ci.cancel();
	}

	/** The spells' auras on your own arm in first person (SpellAuraLayer#firstPersonArm): Flame Fist's fire, a ward's skin. */
	@Inject(method = "renderHand", at = @At("TAIL"))
	private void dth$spellAurasOnHand(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, ModelPart arm,
									  ModelPart sleeve, CallbackInfo ci) {
		boolean right = arm == ((PlayerRenderer) (Object) this).getModel().rightArm;
		com.example.defyingtheheavens.client.SpellAuraLayer.firstPersonArm(pose, buffers, player, arm, sleeve, light, right);
	}
}
