package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.ConsciousnessRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla stops drawing mob-sized entities ~64 blocks away, but a domain reaches up to 160: entities inside the domain are
 * drawn (and so outlined) as far as the server sends them, as long as they are in view.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void dth$drawWholeDomain(T entity, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
		if (ConsciousnessRenderer.isHighlighted(entity)) {
			cir.setReturnValue(entity.noCulling || frustum.isVisible(entity.getBoundingBoxForCulling().inflate(0.5)));
		}
	}
}
