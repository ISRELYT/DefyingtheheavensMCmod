package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.SoulProjection;
import com.example.defyingtheheavens.client.ClientSoul;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A projected soul is never drawn as a player (no body, no name, no shadow): it is drawn as light on the sense layer instead
 * (SoulRenderer), for those who see it; so too one drawn as the dead (ClientGhosts#drawnAsLight). And the body a soul possesses
 * isn't drawn for that soul in first person: it looks out through those eyes.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class SoulRenderDispatcherMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private <E extends Entity> void dth$soulsAreLight(E entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
		// A soul, or one drawn as the dead's light (one of the dead, or one in a Ghost King's form): drawn as light instead (SoulRenderer).
		if (entity instanceof Player player && (SoulProjection.isSoul(player) || com.example.defyingtheheavens.client.ClientGhosts.drawnAsLight(player))) {
			cir.setReturnValue(false);
			return;
		}
		// A body left sitting in the dead's light (its owner set out from a ghostly form): drawn as that light (SoulRenderer).
		if (com.example.defyingtheheavens.client.SoulRenderer.inLight(entity)) {
			cir.setReturnValue(false);
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (ClientSoul.isOut() && entity.getId() == ClientSoul.host() && mc.options.getCameraType().isFirstPerson()) cir.setReturnValue(false);
	}
}
