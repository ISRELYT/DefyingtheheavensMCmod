package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.CultivationFruitBlockEntity;
import com.example.defyingtheheavens.FruitAura;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws the glowing part of an attached Cultivation Fruit's aura (particles and sounds are FruitAura#clientTick).
 * Each tier moves in its own way (see FruitAura): jade breathes, aqua ripples, frost glints, white-gold radiates
 * beams like the Ender Dragon's death rays, and the 10,000-year fruit beats like a heart.
 * <p>
 * Drawn with {@link FruitAuraRenderType#GLOW} (additive, no depth writes), and faded out as the camera comes right up
 * to the fruit so the glow never swamps the screen.
 */
public class CultivationFruitAuraRenderer implements BlockEntityRenderer<CultivationFruitBlockEntity> {
	private static final int HALO_SEGMENTS = 24;
	private static final int PILLAR_SEGMENTS = 8;
	private static final float HALF_SQRT_3 = (float) (Math.sqrt(3.0) / 2.0);
	private static final float[] WHITE = {1.0f, 1.0f, 1.0f};
	private static final float[] WARM_WHITE = {1.0f, 0.98f, 0.9f};

	@Override
	public void render(CultivationFruitBlockEntity fruit, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		if (fruit.getLevel() == null) return;
		int years = fruit.age();
		FruitAura.Tier tier = FruitAura.of(years);
		if (tier == FruitAura.Tier.NONE) return;
		BlockPos pos = fruit.getBlockPos();
		float clock = FruitAura.clock(fruit.getLevel().getGameTime(), pos) + partialTick;
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
		float near = nearFade(camera, pos);
		VertexConsumer glow = buffers.getBuffer(FruitAuraRenderType.GLOW);

		poseStack.pushPose();
		poseStack.translate(0.5, FruitAura.CENTER_Y, 0.5);
		switch (tier) {
			case HUNDRED_YEAR -> {
				// Jade: one slow breath in and out.
				float breath = FruitAura.swell(clock, FruitAura.BREATH_TICKS);
				halo(poseStack, camera, glow, 0.27f + 0.06f * breath, FruitAura.JADE, (0.2f + 0.18f * breath) * near);
			}
			case FIVE_HUNDRED_YEAR -> {
				// Aqua: still like water, with a faint shimmer and a ripple spreading out now and then.
				float shimmer = 0.5f + 0.25f * Mth.sin(clock * 0.21f) + 0.25f * Mth.sin(clock * 0.13f + 1.3f);
				halo(poseStack, camera, glow, 0.3f, FruitAura.AQUA, (0.3f + 0.06f * shimmer) * near);
				halo(poseStack, camera, glow, 0.11f, FruitAura.FROST, 0.35f * near);
				ripple(poseStack, camera, glow, FruitAura.phase(clock, FruitAura.RIPPLE_TICKS), 0.2f, 0.85f, 0.045f, FruitAura.AQUA, 0.3f * near);
			}
			case THOUSAND_YEAR -> {
				// Frost: a steady, crisp glow that now and then glints like ice catching the light.
				float glint = FruitAura.glint(clock);
				halo(poseStack, camera, glow, 0.48f, FruitAura.FROST, (0.38f + 0.12f * glint) * near);
				halo(poseStack, camera, glow, 0.18f, WHITE, (0.5f + 0.3f * glint) * near);
				if (glint > 0.01f) star(poseStack, camera, glow, 0.35f + 0.35f * glint, clock * 0.5f, WHITE, 0.9f * glint * near);
			}
			case HEAVENLY -> {
				// White-gold: radiant and steady; the beams carry the movement, each breathing on its own.
				float strength = FruitAura.heavenlyStrength(years);
				halo(poseStack, camera, glow, 0.75f, FruitAura.WHITE_GOLD, 0.42f * near);
				halo(poseStack, camera, glow, 0.28f, WARM_WHITE, 0.65f * near);
				rays(poseStack, glow, pos.asLong(), 10 + Math.round(4 * strength), clock, 0.35f, 1.3f + 0.3f * strength, 0.22f, 1.0f,
						WARM_WHITE, FruitAura.GOLD, 0.65f * near);
			}
			case TEN_THOUSAND_YEAR -> {
				// A heartbeat: the halo swells, the beams surge outward, the pillar flares and a ripple leaves on the "lub".
				float beat = FruitAura.heartbeat(clock);
				halo(poseStack, camera, glow, 1.0f + 0.12f * beat, FruitAura.GOLD, (0.18f + 0.12f * beat) * near);
				halo(poseStack, camera, glow, 0.68f + 0.08f * beat, FruitAura.WHITE_GOLD, (0.38f + 0.15f * beat) * near);
				halo(poseStack, camera, glow, 0.3f, new float[] {1.0f, 0.99f, 0.95f}, 0.8f * near);
				float sinceBeat = clock % FruitAura.HEARTBEAT_TICKS;
				if (sinceBeat < 30) ripple(poseStack, camera, glow, sinceBeat / 30.0f, 0.35f, 1.6f, 0.07f, FruitAura.GOLD, 0.5f * near);
				// Long white beams turning one way, shorter gold beams the other.
				float surge = 1.0f + 0.12f * beat;
				rays(poseStack, glow, pos.asLong(), 16, clock, 0.25f, 1.9f, 0.2f, surge, WARM_WHITE, FruitAura.WHITE_GOLD, 0.7f * near);
				rays(poseStack, glow, pos.asLong() ^ 0x5DEECE66DL, 10, clock, -0.4f, 1.2f, 0.3f, surge, FruitAura.WHITE_GOLD, FruitAura.GOLD,
						(0.5f + 0.2f * beat) * near);
				pillar(poseStack, camera, glow, 64.0f, 0.75f + 0.25f * beat);
			}
			default -> {}
		}
		poseStack.popPose();
	}

	/** 0 when the camera is at the fruit, rising smoothly to 1 a block and a half away. */
	private static float nearFade(Camera camera, BlockPos pos) {
		Vec3 centre = new Vec3(pos.getX() + 0.5, pos.getY() + FruitAura.CENTER_Y, pos.getZ() + 0.5);
		float t = Mth.clamp((float) ((camera.getPosition().distanceTo(centre) - 0.3) / 1.2), 0.0f, 1.0f);
		return t * t * (3.0f - 2.0f * t);
	}

	/** A camera-facing disc, brightest at the centre and fading to nothing at its rim. */
	private static void halo(PoseStack poseStack, Camera camera, VertexConsumer consumer, float radius, float[] rgb, float alpha) {
		if (alpha < 0.005f) return;
		poseStack.pushPose();
		poseStack.mulPose(camera.rotation());
		Matrix4f pose = poseStack.last().pose();
		for (int i = 0; i < HALO_SEGMENTS; i++) {
			quad(consumer, pose, 0, 0, alpha, 0, 0, alpha, cos(i + 1) * radius, sin(i + 1) * radius, 0, cos(i) * radius, sin(i) * radius, 0, rgb);
		}
		poseStack.popPose();
	}

	/** A camera-facing ring that eases out from {@code from} to {@code to} as {@code phase} goes 0..1, fading as it goes. */
	private static void ripple(PoseStack poseStack, Camera camera, VertexConsumer consumer, float phase, float from, float to,
			float halfWidth, float[] rgb, float alpha) {
		float eased = 1.0f - (1.0f - phase) * (1.0f - phase);
		float radius = from + (to - from) * eased;
		float a = alpha * (1.0f - phase) * (1.0f - phase);
		if (a < 0.005f) return;
		float inner = Math.max(0.0f, radius - halfWidth), outer = radius + halfWidth;
		poseStack.pushPose();
		poseStack.mulPose(camera.rotation());
		Matrix4f pose = poseStack.last().pose();
		for (int i = 0; i < HALO_SEGMENTS; i++) {
			float c0 = cos(i), s0 = sin(i), c1 = cos(i + 1), s1 = sin(i + 1);
			quad(consumer, pose, c0 * inner, s0 * inner, 0, c0 * radius, s0 * radius, a, c1 * radius, s1 * radius, a, c1 * inner, s1 * inner, 0, rgb);
			quad(consumer, pose, c0 * radius, s0 * radius, a, c0 * outer, s0 * outer, 0, c1 * outer, s1 * outer, 0, c1 * radius, s1 * radius, a, rgb);
		}
		poseStack.popPose();
	}

	/** A four-pointed glint facing the camera, slowly turning: thin spokes, bright at the centre, sharp at the tips. */
	private static void star(PoseStack poseStack, Camera camera, VertexConsumer consumer, float size, float spinDegrees, float[] rgb, float alpha) {
		poseStack.pushPose();
		poseStack.mulPose(camera.rotation());
		poseStack.mulPose(Axis.ZP.rotationDegrees(spinDegrees));
		Matrix4f pose = poseStack.last().pose();
		float width = size * 0.08f;
		for (int spoke = 0; spoke < 4; spoke++) {
			float dx = spoke == 0 ? 1 : spoke == 2 ? -1 : 0, dy = spoke == 1 ? 1 : spoke == 3 ? -1 : 0;
			// Spoke from the centre (two corners either side, bright) to a single sharp tip (clear).
			quad(consumer, pose, -dy * width, dx * width, alpha, dy * width, -dx * width, alpha, dx * size, dy * size, 0, dx * size, dy * size, 0, rgb);
		}
		poseStack.popPose();
	}

	/**
	 * Beams of light bursting from the fruit in fixed random directions, drawn the way the Ender Dragon's death rays
	 * are: each is a thin three-sided spike, bright at the fruit and fading to nothing at its tip. The layout is seeded
	 * by the fruit's position so it never jumps; the whole burst turns slowly and each beam breathes on its own beat.
	 */
	private static void rays(PoseStack poseStack, VertexConsumer consumer, long seed, int count, float clock, float spinDegreesPerTick,
			float maxLength, float maxWidth, float surge, float[] core, float[] tip, float alpha) {
		if (alpha < 0.005f) return;
		RandomSource random = RandomSource.create(seed);
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(clock * spinDegreesPerTick));
		for (int i = 0; i < count; i++) {
			// Rotations accumulate from beam to beam, scattering them in every direction (as the dragon's do).
			poseStack.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0f));
			poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0f));
			poseStack.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0f));
			float length = surge * maxLength * (0.55f + 0.45f * random.nextFloat()) * (0.8f + 0.2f * Mth.sin(clock * 0.06f + i * 1.7f));
			float width = maxWidth * (0.6f + 0.4f * random.nextFloat());
			Matrix4f pose = poseStack.last().pose();
			float ax = -HALF_SQRT_3 * width, az = -0.5f * width, bx = HALF_SQRT_3 * width, bz = -0.5f * width, cz = width;
			spikeFace(consumer, pose, length, ax, az, bx, bz, core, tip, alpha);
			spikeFace(consumer, pose, length, bx, bz, 0, cz, core, tip, alpha);
			spikeFace(consumer, pose, length, 0, cz, ax, az, core, tip, alpha);
		}
		poseStack.popPose();
	}

	/** One side of a beam: a triangle from the fruit (bright, core colour) to two tip corners (clear, tip colour). */
	private static void spikeFace(VertexConsumer consumer, Matrix4f pose, float length, float x0, float z0, float x1, float z1,
			float[] core, float[] tip, float alpha) {
		vertex(consumer, pose, 0, 0, 0, core, alpha);
		vertex(consumer, pose, 0, 0, 0, core, alpha);
		vertex(consumer, pose, x0, length, z0, tip, 0);
		vertex(consumer, pose, x1, length, z1, tip, 0);
	}

	/** A thin vertical beam that turns to face the camera, white-gold at its core, fading out with height. */
	private static void pillar(PoseStack poseStack, Camera camera, VertexConsumer consumer, float height, float brightness) {
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
		Matrix4f pose = poseStack.last().pose();
		beam(consumer, pose, 0.08f, height, WARM_WHITE, 0.7f * brightness);
		beam(consumer, pose, 0.4f, height * 0.8f, FruitAura.GOLD, 0.22f * brightness);
		poseStack.popPose();
	}

	private static void beam(VertexConsumer consumer, Matrix4f pose, float halfWidth, float height, float[] rgb, float alpha) {
		for (int i = 0; i < PILLAR_SEGMENTS; i++) {
			float t0 = i / (float) PILLAR_SEGMENTS, t1 = (i + 1) / (float) PILLAR_SEGMENTS;
			float y0 = t0 * height, y1 = t1 * height, a0 = alpha * fade(t0), a1 = alpha * fade(t1);
			quad(consumer, pose, 0, y0, a0, -halfWidth, y0, 0, -halfWidth, y1, 0, 0, y1, a1, rgb);
			quad(consumer, pose, 0, y0, a0, halfWidth, y0, 0, halfWidth, y1, 0, 0, y1, a1, rgb);
		}
	}

	/** Brightness along the pillar: full at the fruit, gone at the top. */
	private static float fade(float t) {
		float left = 1.0f - t;
		return left * Mth.sqrt(left);
	}

	/** A flat quad in the XY plane with per-corner alpha. The render type does not cull, so one winding shows both sides. */
	private static void quad(VertexConsumer consumer, Matrix4f pose, float x0, float y0, float a0, float x1, float y1, float a1,
			float x2, float y2, float a2, float x3, float y3, float a3, float[] rgb) {
		vertex(consumer, pose, x0, y0, 0, rgb, a0);
		vertex(consumer, pose, x1, y1, 0, rgb, a1);
		vertex(consumer, pose, x2, y2, 0, rgb, a2);
		vertex(consumer, pose, x3, y3, 0, rgb, a3);
	}

	private static float cos(int segment) {
		return Mth.cos((float) (segment * Math.PI * 2 / HALO_SEGMENTS));
	}

	private static float sin(int segment) {
		return Mth.sin((float) (segment * Math.PI * 2 / HALO_SEGMENTS));
	}

	private static void vertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z, float[] rgb, float a) {
		consumer.vertex(pose, x, y, z).color(rgb[0], rgb[1], rgb[2], a).endVertex();
	}

	@Override
	public boolean shouldRenderOffScreen(CultivationFruitBlockEntity fruit) {
		return true; // halos and beams reach beyond the block
	}

	@Override
	public int getViewDistance() {
		return 256; // the 10,000-year pillar should be seen from afar
	}
}
