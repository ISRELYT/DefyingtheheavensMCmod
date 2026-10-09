package com.example.defyingtheheavens.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.example.defyingtheheavens.DefyingTheHeavens;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Upper Realm sky: the biome's sky colour overhead (azure, or dark purple over the Thunder Peaks) melting into a
 * golden aura at the horizon, a golden halo around the sun, the moon, and stars at night.
 */
public class UpperRealmSkyRenderer implements DimensionRenderingRegistry.SkyRenderer {
	private static final ResourceLocation SUN = new ResourceLocation("textures/environment/sun.png");
	private static final ResourceLocation MOON = new ResourceLocation("textures/environment/moon_phases.png");
	private static final float[] GOLD = {1.0f, 0.84f, 0.52f};

	@Override
	public void render(WorldRenderContext context) {
		ClientLevel level = context.world();
		if (level == null || context.camera() == null) return;
		try {
			float partialTick = context.tickDelta();
			PoseStack poseStack = context.matrixStack();
			Vec3 skyColor = level.getSkyColor(context.camera().getPosition(), partialTick);
			float[] sky = {(float) skyColor.x, (float) skyColor.y, (float) skyColor.z};
			float day = Mth.clamp(Mth.cos(level.getTimeOfDay(partialTick) * Mth.TWO_PI) * 2.0f + 0.5f, 0.0f, 1.0f);

			float clearSky = 1.0f - TribulationAtmosphere.strength(partialTick);
			float[] horizon = SkyDome.lerp(sky, GOLD, 0.55f * day * clearSky);
			float[] nadir = SkyDome.lerp(sky, new float[] {sky[0] * 0.75f, sky[1] * 0.85f, sky[2]}, 0.8f * clearSky);

			RenderSystem.depthMask(false);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			SkyDome.drawGradient(poseStack.last().pose(), sky, horizon, nadir, 38.0f);

			poseStack.pushPose();
			poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f));
			poseStack.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(partialTick) * 360.0f));
			Matrix4f celestial = poseStack.last().pose();

			float rainFade = (1.0f - level.getRainLevel(partialTick)) * (0.2f + 0.8f * clearSky);
			SkyDome.drawStars(celestial, level.getStarBrightness(partialTick) * rainFade, 1.0f, 0.95f, 0.85f, level.getGameTime() + partialTick);
			SkyDome.drawHalo(celestial, 48.0f, GOLD[0], GOLD[1], GOLD[2], 0.45f * rainFade);

			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
					GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, rainFade);
			drawSun(celestial);
			drawMoon(celestial, level.getMoonPhase());
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			poseStack.popPose();
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Upper Realm sky rendering failed", e);
		} finally {
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableBlend();
			RenderSystem.depthMask(true);
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		}
	}

	private static void drawSun(Matrix4f pose) {
		float size = 30.0f;
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, SUN);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.vertex(pose, -size, 100.0f, -size).uv(0.0f, 0.0f).endVertex();
		buffer.vertex(pose, size, 100.0f, -size).uv(1.0f, 0.0f).endVertex();
		buffer.vertex(pose, size, 100.0f, size).uv(1.0f, 1.0f).endVertex();
		buffer.vertex(pose, -size, 100.0f, size).uv(0.0f, 1.0f).endVertex();
		BufferUploader.drawWithShader(buffer.end());
	}

	private static void drawMoon(Matrix4f pose, int phase) {
		float size = 20.0f;
		int column = phase % 4;
		int row = phase / 4 % 2;
		float u0 = column / 4.0f, v0 = row / 2.0f, u1 = (column + 1) / 4.0f, v1 = (row + 1) / 2.0f;
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, MOON);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.vertex(pose, -size, -100.0f, size).uv(u1, v1).endVertex();
		buffer.vertex(pose, size, -100.0f, size).uv(u0, v1).endVertex();
		buffer.vertex(pose, size, -100.0f, -size).uv(u0, v0).endVertex();
		buffer.vertex(pose, -size, -100.0f, -size).uv(u1, v0).endVertex();
		BufferUploader.drawWithShader(buffer.end());
	}
}
