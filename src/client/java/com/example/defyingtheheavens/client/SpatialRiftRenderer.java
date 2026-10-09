package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.SpatialRiftBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Draws the Spatial Rift: a ~2.5-block jagged tear, pitch black (#000000) inside, with a bright electric-cyan (#00E5FF)
 * outline and a soft outer glow. It turns to face the camera so it reads as a tear from every side, and its edges
 * crackle slightly over time.
 */
public class SpatialRiftRenderer implements BlockEntityRenderer<SpatialRiftBlockEntity> {
	/** Outline points (x, y) in blocks, going around the tear; y spans -0.25 .. 2.25 from the block's base. */
	private static final float[][] OUTLINE = {
			{0.00f, 2.25f}, {0.10f, 1.95f}, {0.05f, 1.80f}, {0.22f, 1.55f}, {0.14f, 1.35f},
			{0.38f, 1.05f}, {0.20f, 0.85f}, {0.30f, 0.55f}, {0.12f, 0.35f}, {0.18f, 0.10f},
			{0.00f, -0.25f}, {-0.16f, 0.05f}, {-0.08f, 0.30f}, {-0.32f, 0.60f}, {-0.18f, 0.80f},
			{-0.42f, 1.10f}, {-0.20f, 1.30f}, {-0.26f, 1.60f}, {-0.08f, 1.80f}, {-0.12f, 2.00f}};
	private static final float CENTER_Y = 1.0f;
	private static final float CYAN_R = 0.0f, CYAN_G = 0xE5 / 255.0f, CYAN_B = 1.0f;
	private static final float CORE_WIDTH = 0.05f;
	private static final float GLOW_WIDTH = 0.24f;

	@Override
	public void render(SpatialRiftBlockEntity rift, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		if (rift.getLevel() == null) return;
		float time = rift.getLevel().getGameTime() + partialTick;
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

		poseStack.pushPose();
		poseStack.translate(0.5, 0.0, 0.5);
		poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
		Matrix4f pose = poseStack.last().pose();

		int n = OUTLINE.length;
		float[] xs = new float[n];
		float[] ys = new float[n];
		for (int i = 0; i < n; i++) {
			float jitter = 0.025f * Mth.sin(time * 0.35f + i * 1.7f);
			xs[i] = OUTLINE[i][0] * (1.0f + jitter * 2.0f);
			ys[i] = OUTLINE[i][1] + jitter * 0.5f;
		}

		// Black interior: a fan of triangles from the centre (each as a quad with a repeated corner), both faces.
		VertexConsumer fill = buffers.getBuffer(RenderType.textBackground());
		for (int i = 0; i < n; i++) {
			int j = (i + 1) % n;
			fillVertex(fill, pose, 0.0f, CENTER_Y);
			fillVertex(fill, pose, xs[i], ys[i]);
			fillVertex(fill, pose, xs[j], ys[j]);
			fillVertex(fill, pose, xs[j], ys[j]);
			fillVertex(fill, pose, 0.0f, CENTER_Y);
			fillVertex(fill, pose, xs[j], ys[j]);
			fillVertex(fill, pose, xs[i], ys[i]);
			fillVertex(fill, pose, xs[i], ys[i]);
		}

		// Glowing outline: a bright core band and a wider fading halo, pushed out from the centre, additive.
		float pulse = 0.85f + 0.15f * Mth.sin(time * 0.2f);
		VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
		band(glow, pose, xs, ys, 0.0f, CORE_WIDTH, pulse, pulse);
		band(glow, pose, xs, ys, CORE_WIDTH, GLOW_WIDTH, 0.45f * pulse, 0.0f);

		poseStack.popPose();
	}

	private static void band(VertexConsumer consumer, Matrix4f pose, float[] xs, float[] ys, float from, float to, float innerAlpha, float outerAlpha) {
		int n = xs.length;
		for (int i = 0; i < n; i++) {
			int j = (i + 1) % n;
			float[] a0 = push(xs[i], ys[i], from), a1 = push(xs[j], ys[j], from);
			float[] b0 = push(xs[i], ys[i], to), b1 = push(xs[j], ys[j], to);
			glowVertex(consumer, pose, a0, innerAlpha);
			glowVertex(consumer, pose, a1, innerAlpha);
			glowVertex(consumer, pose, b1, outerAlpha);
			glowVertex(consumer, pose, b0, outerAlpha);
			glowVertex(consumer, pose, a0, innerAlpha);
			glowVertex(consumer, pose, b0, outerAlpha);
			glowVertex(consumer, pose, b1, outerAlpha);
			glowVertex(consumer, pose, a1, innerAlpha);
		}
	}

	/** Moves an outline point away from the tear's centre by {@code distance}. */
	private static float[] push(float x, float y, float distance) {
		float dx = x, dy = y - CENTER_Y;
		float length = Mth.sqrt(dx * dx + dy * dy);
		if (length < 1.0e-4f) return new float[] {x, y};
		return new float[] {x + dx / length * distance, y + dy / length * distance};
	}

	private static void fillVertex(VertexConsumer consumer, Matrix4f pose, float x, float y) {
		consumer.vertex(pose, x, y, 0.0f).color(0, 0, 0, 255).uv2(LightTexture.FULL_BRIGHT).endVertex();
	}

	private static void glowVertex(VertexConsumer consumer, Matrix4f pose, float[] point, float alpha) {
		consumer.vertex(pose, point[0], point[1], 0.0f).color(CYAN_R, CYAN_G, CYAN_B, alpha).endVertex();
	}

	@Override
	public boolean shouldRenderOffScreen(SpatialRiftBlockEntity rift) {
		return true; // the tear reaches above and below its block
	}

	@Override
	public int getViewDistance() {
		return 128;
	}
}
