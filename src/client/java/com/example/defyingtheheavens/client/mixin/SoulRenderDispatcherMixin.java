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
 * (SoulRenderer), for those who see it. And the body a soul possesses isn't drawn for that soul in first person: it looks out
 * through those eyes.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class SoulRenderDispatcherMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private <E extends Entity> void dth$soulsAreLight(E entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof Player player && SoulProjection.isSoul(player)) {
			cir.setReturnValue(false);
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (ClientSoul.isOut() && entity.getId() == ClientSoul.host() && mc.options.getCameraType().isFirstPerson()) cir.setReturnValue(false);
	}
}
