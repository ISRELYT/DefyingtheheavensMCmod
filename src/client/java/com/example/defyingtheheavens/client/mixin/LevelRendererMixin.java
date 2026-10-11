package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.ClientSoul;
import com.example.defyingtheheavens.client.ConsciousnessRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

	/**
	 * A projected soul inside solid rock sees out as a spectator does: vanilla's cave culling, which would hide everything from a
	 * camera inside an opaque block, is only waived for spectators.
	 */
	@ModifyArg(method = "renderLevel", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/LevelRenderer;setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V"),
			index = 3)
	private boolean dth$soulSeesThroughRock(boolean spectator) {
		return spectator || ClientSoul.isOut();
	}

	/**
	 * A soul possessing a body looks out through its eyes: in first person the body itself isn't drawn (vanilla always draws
	 * what the player rides, whatever EntityRenderDispatcher#shouldRender says, so it is skipped here).
	 */
	@Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
	private void dth$possessedBodyUnseenFromWithin(Entity entity, double camX, double camY, double camZ, float partialTick, PoseStack poseStack,
												   MultiBufferSource buffers, CallbackInfo ci) {
		if (ClientSoul.isOut() && entity.getId() == ClientSoul.host() && Minecraft.getInstance().options.getCameraType().isFirstPerson()) ci.cancel();
	}
}
