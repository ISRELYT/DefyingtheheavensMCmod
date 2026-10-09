package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.ConsciousnessRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Colours the outline of an entity the Consciousness Domain lit ({@link MinecraftMixin}): white for mortals, red for
 * cultivators. Entities that truly glow (the effect, or a spectator's outline key) keep their team colour.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
	private int dth$consciousnessOutlineColor(Entity entity) {
		if (!entity.isCurrentlyGlowing() && ConsciousnessRenderer.isHighlighted(entity)) return ConsciousnessRenderer.outlineColor(entity);
		return entity.getTeamColor();
	}
}
