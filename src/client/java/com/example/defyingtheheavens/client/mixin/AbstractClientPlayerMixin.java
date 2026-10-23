package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.ClientBloodlines;
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
 * <p>
 * A demon (ClientBloodlines) is drawn in its race's own skin, broad-armed, in place of theirs: whoever's body it is, the body's
 * blood (a player in a demon's body wears the demon's skin; in a human's, the human's own).
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
	@Inject(method = "getSkinTextureLocation", at = @At("HEAD"), cancellable = true)
	private void dth$theBodysSkin(CallbackInfoReturnable<ResourceLocation> cir) {
		AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
		ResourceLocation race = ClientBloodlines.skin(ClientBloodlines.owner(self));
		if (race != null) {
			cir.setReturnValue(race);
			return;
		}
		ResourceLocation skin = ClientBodies.skin(self);
		if (skin != null) cir.setReturnValue(skin);
	}

	@Inject(method = "getModelName", at = @At("HEAD"), cancellable = true)
	private void dth$theBodysModel(CallbackInfoReturnable<String> cir) {
		AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
		if (ClientBloodlines.look(self) != null) {
			cir.setReturnValue("default");
			return;
		}
		String model = ClientBodies.model(self);
		if (model != null) cir.setReturnValue(model);
	}

	@Inject(method = "getCloakTextureLocation", at = @At("HEAD"), cancellable = true)
	private void dth$theBodysCape(CallbackInfoReturnable<ResourceLocation> cir) {
		AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
		if (ClientBodies.disguised(self)) cir.setReturnValue(ClientBodies.cape(self));
	}
}
