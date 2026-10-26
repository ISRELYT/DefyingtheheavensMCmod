package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.SpatialTearEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * A Heavenly Being's tear in space (SpatialTearEntity): the Spatial Rift's jagged tear (SpatialRiftRenderer), a little taller,
 * opening as a slit that widens and closing the same way, its rim lit by where it leads (violet into the gap; out of it, green
 * for the mortal world, gold for the Upper Realm; a Void Breach starlit and {@link #BREACH_SCALE} times the size, a way down out of
 * the Great Void jade) over a pale halo, and within its black the void's own motes turning slowly.
 * It turns to face the camera, so it reads as a tear from every side (a void current lying along its flow, turned about it).
 */
public class SpatialTearRenderer extends EntityRenderer<SpatialTearEntity> {
	/** The rift's outline (SpatialRiftRenderer), going round the tear: x across, y up from its foot. */
	private static final float[][] OUTLINE = {
			{0.00f, 2.25f}, {0.10f, 1.95f}, {0.05f, 1.80f}, {0.22f, 1.55f}, {0.14f, 1.35f},
			{0.38f, 1.05f}, {0.20f, 0.85f}, {0.30f, 0.55f}, {0.12f, 0.35f}, {0.18f, 0.10f},
			{0.00f, -0.25f}, {-0.16f, 0.05f}, {-0.08f, 0.30f}, {-0.32f, 0.60f}, {-0.18f, 0.80f},
			{-0.42f, 1.10f}, {-0.20f, 1.30f}, {-0.26f, 1.60f}, {-0.08f, 1.80f}, {-0.12f, 2.00f}};
	/** Its size: the rift's outline a quarter taller, and as wide as a doorway one walks through (WIDTH more across). */
	private static final float SCALE = 1.25f, WIDTH = 2.1f, CENTER_Y = 1.0f * SCALE + 0.25f;
	private static final float[] GAP_RIM = {0.80f, 0.62f, 1.0f}, OVERWORLD_RIM = {0.55f, 1.0f, 0.55f}, UPPER_RIM = {1.0f, 0.82f, 0.35f};
	/** A Void Breach's rim, starlight on the void's violet; a way down out of the Great Void's, jade (the Jade Firmament's). */
	private static final float[] BREACH_RIM = {0.88f, 0.80f, 1.0f}, JADE_RIM = {0.45f, 1.0f, 0.75f};
	/** A Void Breach is torn this much larger than a tear: a way between realms that several may ride (VoidBreach#MOUTH_HEIGHT). */
	private static final float BREACH_SCALE = 2.2f;
	/** A void current's rim, pale starlight; it lies on its side, drawn this much longer along its flow and this much narrower across. */
	private static final float[] CURRENT_RIM = {0.62f, 0.95f, 1.0f};
	private static final float CURRENT_LENGTH = 2.4f, CURRENT_WIDTH = 0.8f;
	private static final float[] HALO = {0.55f, 0.85f, 1.0f};
	private static final float CORE_WIDTH = 0.06f, GLOW_WIDTH = 0.34f;
	private static final ResourceLocation NONE = new ResourceLocation("textures/misc/white.png");

	public SpatialTearRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(SpatialTearEntity tear, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
		float open = tear.openness(partialTick);
		if (open <= 0.001f) return;
		float time = tear.tickCount + partialTick;
		float[] rim = switch (tear.leads()) {
			case GAP -> GAP_RIM;
			case OVERWORLD -> OVERWORLD_RIM;
			case UPPER_REALM -> UPPER_RIM;
			case VOID_BREACH -> BREACH_RIM;
			case JADE_FIRMAMENT -> JADE_RIM;
			case VOID_CURRENT -> CURRENT_RIM;
		};
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
		poseStack.pushPose();
		if (tear.leads() == SpatialTearEntity.Leads.VOID_CURRENT) {
			// A current lies on its side about its heart along its flow, long and narrow, turned about its flow to face the camera: a
			// streak of light across the void the way it bears one (VoidCurrents). Laid down first, then drawn out along its flow (drawn
			// out first, then laid down, it stood up tall: tour 8).
			float turn = -tear.getYRot() - 90;
			Vec3 eye = camera.getPosition().subtract(tear.getPosition(partialTick).add(0, CENTER_Y, 0));
			double across = eye.x * Math.sin(Math.toRadians(turn)) + eye.z * Math.cos(Math.toRadians(turn));
			poseStack.translate(0, CENTER_Y, 0);
			poseStack.mulPose(Axis.YP.rotationDegrees(turn));
			poseStack.mulPose(Axis.XP.rotation((float) Math.atan2(-eye.y, across)));
			poseStack.scale(CURRENT_LENGTH, CURRENT_WIDTH, 1);
			poseStack.mulPose(Axis.ZP.rotationDegrees(90));
			poseStack.translate(0, -CENTER_Y, 0);
		} else {
			poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
			if (tear.leads() == SpatialTearEntity.Leads.VOID_BREACH) poseStack.scale(BREACH_SCALE, BREACH_SCALE, BREACH_SCALE);
		}
		Matrix4f pose = poseStack.last().pose();

		// The slit widens as it opens (and narrows as it closes), and stands to its full height a little sooner.
		float wide = SpellRender.easeOut(open), tall = (float) Math.sqrt(open);
		int n = OUTLINE.length;
		float[] xs = new float[n], ys = new float[n];
		for (int i = 0; i < n; i++) {
			float jitter = 0.03f * Mth.sin(time * 0.37f + i * 1.7f);
			xs[i] = OUTLINE[i][0] * SCALE * WIDTH * (1.0f + jitter * 2.0f) * wide;
			ys[i] = CENTER_Y + (OUTLINE[i][1] * SCALE + 0.25f - CENTER_Y) * tall + jitter * 0.4f;
		}

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

		VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
		float pulse = (0.85f + 0.15f * Mth.sin(time * 0.22f)) * Math.min(1, open * 1.5f);
		band(glow, pose, xs, ys, 0.0f, CORE_WIDTH, pulse, pulse, rim);
		band(glow, pose, xs, ys, CORE_WIDTH, GLOW_WIDTH, 0.5f * pulse, 0.0f, HALO);
		band(glow, pose, xs, ys, GLOW_WIDTH, GLOW_WIDTH * 2.4f, 0.12f * pulse, 0.0f, rim);
		// Within, the void's motes, turning slowly round its heart and drawn toward it.
		for (int k = 0; k < 26; k++) {
			float phase = SpellRender.hash(tear.getId() * 31L + k) * Mth.TWO_PI;
			float r = (0.15f + 0.6f * ((time * 0.006f + k / 26f) % 1f)) * wide;
			float a = phase + time * (0.02f + 0.01f * (k % 3));
			float mx = Mth.cos(a) * r * 0.35f * WIDTH * 0.8f, my = CENTER_Y + Mth.sin(a) * r * 1.1f * tall;
			float s = 0.035f + 0.025f * (k % 2);
			float alpha = (1 - r / Math.max(0.01f, wide)) * 0.9f * pulse;
			mote(glow, pose, mx, my, s, alpha, k % 3 == 0 ? rim : HALO);
		}
		poseStack.popPose();
		super.render(tear, entityYaw, partialTick, poseStack, buffers, light);
	}

	private static void mote(VertexConsumer consumer, Matrix4f pose, float x, float y, float s, float alpha, float[] colour) {
		float[] a = {x - s, y}, b = {x, y + s}, c = {x + s, y}, d = {x, y - s};
		glowVertex(consumer, pose, a, alpha, colour);
		glowVertex(consumer, pose, b, alpha, colour);
		glowVertex(consumer, pose, c, alpha, colour);
		glowVertex(consumer, pose, d, alpha, colour);
		glowVertex(consumer, pose, a, alpha, colour);
		glowVertex(consumer, pose, d, alpha, colour);
		glowVertex(consumer, pose, c, alpha, colour);
		glowVertex(consumer, pose, b, alpha, colour);
	}

	private static void band(VertexConsumer consumer, Matrix4f pose, float[] xs, float[] ys, float from, float to, float innerAlpha, float outerAlpha,
			float[] colour) {
		int n = xs.length;
		for (int i = 0; i < n; i++) {
			int j = (i + 1) % n;
			float[] a0 = push(xs[i], ys[i], from), a1 = push(xs[j], ys[j], from);
			float[] b0 = push(xs[i], ys[i], to), b1 = push(xs[j], ys[j], to);
			glowVertex(consumer, pose, a0, innerAlpha, colour);
			glowVertex(consumer, pose, a1, innerAlpha, colour);
			glowVertex(consumer, pose, b1, outerAlpha, colour);
			glowVertex(consumer, pose, b0, outerAlpha, colour);
			glowVertex(consumer, pose, a0, innerAlpha, colour);
			glowVertex(consumer, pose, b0, outerAlpha, colour);
			glowVertex(consumer, pose, b1, outerAlpha, colour);
			glowVertex(consumer, pose, a1, innerAlpha, colour);
		}
	}

	private static float[] push(float x, float y, float distance) {
		float dx = x, dy = y - CENTER_Y;
		float length = Mth.sqrt(dx * dx + dy * dy);
		if (length < 1.0e-4f) return new float[] {x, y};
		return new float[] {x + dx / length * distance, y + dy / length * distance};
	}

	private static void fillVertex(VertexConsumer consumer, Matrix4f pose, float x, float y) {
		consumer.vertex(pose, x, y, 0.0f).color(0, 0, 0, 255).uv2(LightTexture.FULL_BRIGHT).endVertex();
	}

	private static void glowVertex(VertexConsumer consumer, Matrix4f pose, float[] point, float alpha, float[] colour) {
		consumer.vertex(pose, point[0], point[1], 0.0f).color(colour[0], colour[1], colour[2], alpha).endVertex();
	}

	@Override
	public ResourceLocation getTextureLocation(SpatialTearEntity tear) {
		return NONE;
	}
}
