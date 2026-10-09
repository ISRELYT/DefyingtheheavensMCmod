package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.multiplayer.ClientLevel;

/** The Spatial Gap: an almost black void with a faint indigo glow overhead and slowly drifting cold stars. */
public class SpatialGapSkyRenderer implements DimensionRenderingRegistry.SkyRenderer {
	private static final float[] ZENITH = {0.035f, 0.02f, 0.10f};
	private static final float[] HORIZON = {0.0f, 0.01f, 0.03f};
	private static final float[] NADIR = {0.01f, 0.0f, 0.035f};

	@Override
	public void render(WorldRenderContext context) {
		ClientLevel level = context.world();
		if (level == null) return;
		try {
			float time = level.getGameTime() + context.tickDelta();
			PoseStack poseStack = context.matrixStack();
			RenderSystem.depthMask(false);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			SkyDome.drawGradient(poseStack.last().pose(), ZENITH, HORIZON, NADIR, 70.0f);

			poseStack.pushPose();
			poseStack.mulPose(Axis.YP.rotationDegrees(time * 0.02f));
			poseStack.mulPose(Axis.XP.rotationDegrees(time * 0.007f));
			SkyDome.drawStars(poseStack.last().pose(), 0.9f, 0.55f, 0.95f, 1.0f, time);
			poseStack.popPose();
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Spatial Gap sky rendering failed", e);
		} finally {
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableBlend();
			RenderSystem.depthMask(true);
		}
	}
}
