package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;

/**
 * The Netherworld's sky: near-black teal with no sun, moon or stars; slow drifts of cyan ghost fire high overhead, and a faint
 * teal aurora wavering round the sky above the horizon.
 */
public class NetherworldSkyRenderer implements DimensionRenderingRegistry.SkyRenderer {
	private static final float[] ZENITH = {0.010f, 0.035f, 0.040f};
	/** The horizon when the fog's colour can't be read; normally it is the fog's. */
	private static final float[] HORIZON = {0.020f, 0.065f, 0.070f};
	private static final float[] NADIR = {0.004f, 0.012f, 0.016f};
	/** Wisps of ghost fire: where each drifts (azimuth, elevation), its size, how fast it drifts and flickers. */
	private static final int FIRES = 42;
	private static final float[][] FIRE = new float[FIRES][5];
	private static final float RADIUS = 100.0f;

	static {
		RandomSource random = RandomSource.create(4449L);
		for (int i = 0; i < FIRES; i++) {
			FIRE[i][0] = random.nextFloat() * Mth.TWO_PI;          // azimuth
			FIRE[i][1] = 0.35f + random.nextFloat() * 1.05f;       // elevation (radians above the horizon)
			FIRE[i][2] = 3.0f + random.nextFloat() * 6.0f;         // size
			FIRE[i][3] = 0.00025f + random.nextFloat() * 0.0006f;  // drift (radians a tick)
			FIRE[i][4] = random.nextFloat() * 100.0f;              // flicker phase
		}
	}

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
			// The horizon takes the fog's own colour (each biome's, as the game blends it), so far land fades into the sky
			// rather than standing out against it; the zenith darkens from there.
			float[] fog = RenderSystem.getShaderFogColor();
			float[] horizon = fog.length >= 3 ? new float[]{fog[0], fog[1], fog[2]} : HORIZON;
			float[] zenith = {Mth.lerp(0.3f, ZENITH[0], horizon[0]), Mth.lerp(0.3f, ZENITH[1], horizon[1]), Mth.lerp(0.3f, ZENITH[2], horizon[2])};
			SkyDome.drawGradient(poseStack.last().pose(), zenith, horizon, NADIR, 55.0f);
			drawAurora(poseStack.last().pose(), time);
			for (int i = 0; i < FIRES; i++) {
				float[] fire = FIRE[i];
				float azimuth = fire[0] + time * fire[3];
				float flicker = 0.55f + 0.45f * Mth.sin(time * 0.05f + fire[4]) * Mth.sin(time * 0.013f + fire[4] * 2.0f);
				poseStack.pushPose();
				poseStack.mulPose(Axis.YP.rotation(azimuth));
				poseStack.mulPose(Axis.XP.rotation(Mth.HALF_PI - fire[1]));
				SkyDome.drawHalo(poseStack.last().pose(), fire[2], 0.35f, 0.95f, 1.0f, 0.28f * flicker);
				SkyDome.drawHalo(poseStack.last().pose(), fire[2] * 0.35f, 0.8f, 1.0f, 1.0f, 0.4f * flicker);
				poseStack.popPose();
			}
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Netherworld sky rendering failed", e);
		} finally {
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableBlend();
			RenderSystem.depthMask(true);
		}
	}

	/** Two faint curtains of teal light round the sky, their lower edge brightest, wavering slowly. */
	private static void drawAurora(Matrix4f pose, float time) {
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		RenderSystem.disableCull();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		int segments = 96;
		for (int curtain = 0; curtain < 2; curtain++) {
			float base = curtain == 0 ? 0.42f : 0.62f;
			float height = curtain == 0 ? 0.28f : 0.2f;
			float phase = curtain * 2.3f;
			for (int i = 0; i < segments; i++) {
				float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
				float w0 = wave(a0, time, phase), w1 = wave(a1, time, phase);
				float b0 = base + 0.06f * w0, b1 = base + 0.06f * w1;
				float alpha0 = 0.09f * glow(a0, time, phase) * rays(a0, time, phase), alpha1 = 0.09f * glow(a1, time, phase) * rays(a1, time, phase);
				// A soft lower edge, then the curtain fading upward.
				vertex(buffer, pose, a0, b0 - 0.05f, 0.20f, 0.85f, 0.75f, 0.0f);
				vertex(buffer, pose, a1, b1 - 0.05f, 0.20f, 0.85f, 0.75f, 0.0f);
				vertex(buffer, pose, a1, b1, 0.20f, 0.85f, 0.75f, alpha1);
				vertex(buffer, pose, a0, b0, 0.20f, 0.85f, 0.75f, alpha0);
				vertex(buffer, pose, a0, b0, 0.20f, 0.85f, 0.75f, alpha0);
				vertex(buffer, pose, a1, b1, 0.20f, 0.85f, 0.75f, alpha1);
				vertex(buffer, pose, a1, b1 + height, 0.15f, 0.55f, 0.75f, 0.0f);
				vertex(buffer, pose, a0, b0 + height, 0.15f, 0.55f, 0.75f, 0.0f);
			}
		}
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();
	}

	private static float wave(float azimuth, float time, float phase) {
		return Mth.sin(azimuth * 5.0f + time * 0.004f + phase) * 0.6f + Mth.sin(azimuth * 11.0f - time * 0.007f + phase) * 0.4f;
	}

	private static float glow(float azimuth, float time, float phase) {
		float g = 0.5f + 0.5f * Mth.sin(azimuth * 3.0f + time * 0.003f + phase * 1.7f);
		return g * g; // whole stretches of the sky go dark between the curtains' brighter folds
	}

	/** Fine vertical rays drifting along the curtain, as an aurora's are. */
	private static float rays(float azimuth, float time, float phase) {
		float r = 0.5f + 0.5f * Mth.sin(azimuth * 41.0f + time * 0.012f + phase * 3.1f);
		return 0.35f + 0.65f * r * r * r;
	}

	private static void vertex(BufferBuilder buffer, Matrix4f pose, float azimuth, float elevation, float r, float g, float b, float a) {
		float x = Mth.cos(elevation) * Mth.cos(azimuth) * RADIUS;
		float y = Mth.sin(elevation) * RADIUS;
		float z = Mth.cos(elevation) * Mth.sin(azimuth) * RADIUS;
		buffer.vertex(pose, x, y, z).color(r, g, b, a).endVertex();
	}
}
