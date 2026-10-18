package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.ClientBodies;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A player whose soul is in another's body (Possession) is drawn as that body on every client, their own too (the hands in first
 * person): its skin, its model and its cape (ClientBodies). Their player info (game mode and all) stays their own.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
	@Inject(method = "getSkinTextureLocation", at = @At("HEAD"), cancellable = true)
	private void dth$theBodysSkin(CallbackInfoReturnable<ResourceLocation> cir) {
		ResourceLocation skin = ClientBodies.skin((AbstractClientPlayer) (Object) this);
		if (skin != null) cir.setReturnValue(skin);
	}

	@Inject(method = "getModelName", at = @At("HEAD"), cancellable = true)
	private void dth$theBodysModel(CallbackInfoReturnable<String> cir) {
		String model = ClientBodies.model((AbstractClientPlayer) (Object) this);
		if (model != null) cir.setReturnValue(model);
	}

	@Inject(method = "getCloakTextureLocation", at = @At("HEAD"), cancellable = true)
	private void dth$theBodysCape(CallbackInfoReturnable<ResourceLocation> cir) {
		AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
		if (ClientBodies.disguised(self)) cir.setReturnValue(ClientBodies.cape(self));
	}
}
