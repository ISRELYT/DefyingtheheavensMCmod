package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.ConsciousnessRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Consciousness Domain: living things inside the domain show through walls (coloured by {@link LevelRendererMixin}). */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
	private void dth$consciousnessOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (ConsciousnessRenderer.isHighlighted(entity)) cir.setReturnValue(true);
	}
}
