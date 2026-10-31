package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.KillingFormationBlock;
import com.example.defyingtheheavens.KillingFormationBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * A killing formation as Qi Sense shows it (KillingFormationBlock): plain in the plane of qi, its ring laid on the land as far as it reaches,
 * a second ring of its script turning within it, spokes to its eye; in its array's colour (the blades' silver, the fire's red, the weight's
 * violet, the fog's grey-blue). To the eye alone it is nothing but a shimmer now and then (its block's own).
 */
public class KillingFormationRenderer implements BlockEntityRenderer<KillingFormationBlockEntity> {
	private static final int SEGMENTS = 64;

	@Override
	public void render(KillingFormationBlockEntity formation, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		if (!QiSenseClientHandler.isActive() || formation.getLevel() == null) return;
		KillingFormationBlock.Array array = formation.getBlockState().getValue(KillingFormationBlock.ARRAY);
		float[] rgb = switch (array) {
			case BLADES -> new float[] {0.85f, 0.9f, 1.0f};
			case FIRE -> new float[] {1.0f, 0.35f, 0.15f};
			case GRAVITY -> new float[] {0.65f, 0.35f, 1.0f};
			case FOG -> new float[] {0.6f, 0.72f, 0.85f};
		};
		float time = formation.getLevel().getGameTime() + partial;
		float pulse = 0.55f + 0.2f * Mth.sin(time * 0.1f);
		VertexConsumer glow = QiSenseTreasures.glow(buffers);
		Matrix4f m = pose.last().pose();
		float r = array.reach;
		ring(glow, m, r, 0.12f, 1.05f, 0, rgb, pulse);
		ring(glow, m, r * 0.62f, 0.08f, 1.06f, time * 0.01f, rgb, pulse * 0.8f);
		// Spokes from the eye to the ring.
		for (int i = 0; i < 8; i++) {
			double a = i * Math.PI / 4 + time * 0.004;
			float cx = (float) Math.cos(a), cz = (float) Math.sin(a), px = -cz * 0.05f, pz = cx * 0.05f;
			quad(glow, m, 0.5f + px, 1.07f, 0.5f + pz, 0.5f - px, 1.07f, 0.5f - pz, 0.5f + cx * r - px, 1.07f, 0.5f + cz * r - pz, 0.5f + cx * r + px, 1.07f, 0.5f + cz * r + pz,
					rgb, pulse * 0.6f);
		}
	}

	/** A ring {@code r} out, {@code width} wide, {@code y} over the eye's block, turned {@code turn} (radians), dashed where its script runs. */
	private static void ring(VertexConsumer glow, Matrix4f m, float r, float width, float y, float turn, float[] rgb, float alpha) {
		for (int i = 0; i < SEGMENTS; i++) {
			if (turn != 0 && i % 4 == 3) continue; // the script ring is broken into its characters
			double a0 = turn + i * Math.PI * 2 / SEGMENTS, a1 = turn + (i + 1) * Math.PI * 2 / SEGMENTS;
			float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
			float in = r - width, out = r + width;
			quad(glow, m, 0.5f + c0 * in, y, 0.5f + s0 * in, 0.5f + c0 * out, y, 0.5f + s0 * out, 0.5f + c1 * out, y, 0.5f + s1 * out, 0.5f + c1 * in, y, 0.5f + s1 * in,
					rgb, alpha);
		}
	}

	private static void quad(VertexConsumer v, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2,
			float x3, float y3, float z3, float[] rgb, float alpha) {
		v.vertex(m, x0, y0, z0).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
		v.vertex(m, x1, y1, z1).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
		v.vertex(m, x2, y2, z2).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
		v.vertex(m, x3, y3, z3).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
	}

	@Override
	public boolean shouldRenderOffScreen(KillingFormationBlockEntity formation) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 64;
	}
}
