package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.SpellConstruct;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Space torn open (a SpellConstruct of a Form other than a model's; the user's ask, 2026-10-24: "make sure ... the rifts and tears/cracks
 * in space looks cool"), drawn by hand, as SpatialTearRenderer draws a Heavenly Being's tear, but greater:
 * <ul>
 *   <li>its rim ragged and crackling, white-hot at its very edge, glowing the colour of what comes through it (SpellConstruct#tint), and
 *       haloed; the light spilling in over its inner edge;</li>
 *   <li>within it the void: black, lit faintly from its rim, a nebula of its colour turning slowly round its heart, stars twinkling, motes
 *       of light drawn in;</li>
 *   <li>round it space cracked like glass: cracks running out from its rim, forking, glowing; shards of space breaking off its edge and
 *       drifting away tumbling, some of them pale glass, some of them pieces of the void itself;</li>
 * </ul>
 * opening as it forms (a slit that widens, its cracks running out after it: {@link #OPENS}) and closing as it goes (SpellConstruct#FADE).
 * A {@code TEAR} stands in the air facing whoever looks at it (upright, or lying across their sight: its yaw turns it on their screen); an
 * {@code ABYSS} lies open in the ground, its shards rising out of it; a
 * {@code LINE} is cut from where it stands along its facing, running out as fast as the stroke that cut it ({@link #CUT_RUNS}), and turns
 * about its length to face whoever looks; {@code SHATTER} is space shattered round a point like a pane struck: cracks out from a hole of
 * the void and rings of cracks across them, pieces fallen out showing the void, shards falling away. Its size is its length in blocks.
 */
final class RiftRenderer {
	/** Ticks a tear takes to open: a slit that widens, its cracks running out after it. */
	static final float OPENS = 12;
	/** Ticks a cut takes to run out along its length (as fast as the stroke that cuts it: MetalSpells#SEVERING_THRUST). */
	static final float CUT_RUNS = 6;
	/** How ragged a tear's edges are (of its half-width). */
	private static final float JAG = 0.34f;
	private static final float[] WHITE = {1, 1, 1};

	private RiftRenderer() {}

	/** Draws {@code rift} (the pose at its place). */
	static void render(SpellConstruct rift, float partialTick, PoseStack pose, MultiBufferSource parent) {
		float time = rift.tickCount + partialTick;
		// Opening as it forms, closing as it goes (its life cut short by its spell's end: SpellConstruct#dissolve).
		float open = Math.min(SpellRender.ease(time / OPENS), Mth.clamp((rift.life() - time) / SpellConstruct.FADE, 0, 1));
		if (open <= 0.002f) return;
		Palette colours = Palette.of(rift.tint() != 0 ? rift.tint() : SpellConstruct.VOID);
		float length = rift.size() * rift.kind().scale;
		long seed = rift.getId() * 0x9E3779B97F4A7C15L + rift.kind().ordinal();
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
		MultiBufferSource buffers = SpellBuffers.begin(parent);
		pose.pushPose();
		try {
			switch (rift.kind().form) {
				case TEAR -> {
					// Faces whoever looks at it, upright on their screen, or turned on it by its yaw (90: a slash lying across the sky).
					pose.mulPose(camera.rotation());
					if (rift.getYRot() != 0) pose.mulPose(Axis.ZP.rotationDegrees(rift.getYRot()));
					tear(buffers, pose, seed, length / 2, length * 0.2f, open, time, colours, 0);
				}
				case ABYSS -> {
					// Lying open in the ground, its length along its facing; its shards rise up out of it (its own -z is up).
					pose.mulPose(Axis.YP.rotationDegrees(-rift.getYRot()));
					pose.mulPose(Axis.XP.rotationDegrees(90));
					tear(buffers, pose, seed, length / 2, length * 0.36f, open, time, colours, -1);
				}
				case LINE -> line(buffers, pose, rift, partialTick, camera, seed, length, open, time, colours);
				case SHATTER -> {
					pose.mulPose(camera.rotation());
					shatter(buffers, pose, seed, length / 2, open, time, colours);
				}
				default -> {}
			}
		} finally {
			pose.popPose();
			SpellBuffers.end();
		}
	}

	// --- Colours ---

	/** A tear's colours from its tint: its white-hot edge, its glow, its halo, the void's faint light within, and pale glass. */
	private record Palette(float[] hot, float[] glow, float[] halo, float[] deep, float[] glass) {
		static Palette of(int tint) {
			float[] c = SpellRender.rgb(tint);
			return new Palette(mix(c, WHITE, 0.82f), c, mix(c, WHITE, 0.3f), scale(c, 0.2f), mix(c, WHITE, 0.62f));
		}
	}

	private static float[] mix(float[] a, float[] b, float t) {
		return new float[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
	}

	private static float[] scale(float[] a, float k) {
		return new float[] {a[0] * k, a[1] * k, a[2] * k};
	}

	/** A number from 0 to 1, the same every frame for the same seed and indices. */
	private static float hash(long seed, int a, int b) {
		return SpellRender.hash(seed + a * 0x632BE59BD9B4E019L + b * 0x9E3779B97F4A7C15L);
	}

	// --- Drawing ---

	/** Light, added over what is behind (no depth written). */
	private static VertexConsumer glow(MultiBufferSource buffers) {
		return buffers.getBuffer(SpellRender.flatGlow());
	}

	/** The void itself: dark, hiding what is behind it. */
	private static VertexConsumer dark(MultiBufferSource buffers) {
		return buffers.getBuffer(Types.VOID);
	}

	private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, float[] rgb, float alpha) {
		vc.vertex(m, x, y, z).color(rgb[0], rgb[1], rgb[2], Mth.clamp(alpha, 0, 1)).endVertex();
	}

	/** A triangle (as a quad of four, its last point twice). */
	private static void triangle(VertexConsumer vc, Matrix4f m, float ax, float ay, float bx, float by, float cx, float cy, float z, float[] rgbA, float aA,
			float[] rgbB, float aB, float[] rgbC, float aC) {
		vertex(vc, m, ax, ay, z, rgbA, aA);
		vertex(vc, m, bx, by, z, rgbB, aB);
		vertex(vc, m, cx, cy, z, rgbC, aC);
		vertex(vc, m, cx, cy, z, rgbC, aC);
	}

	/** A soft round glow {@code size} across at {@code x, y}: brightest at its heart, fading to nothing at its edge. */
	private static void blot(VertexConsumer vc, Matrix4f m, float x, float y, float z, float size, float[] rgb, float alpha) {
		int sides = 6;
		for (int i = 0; i < sides; i++) {
			float a0 = Mth.TWO_PI * i / sides, a1 = Mth.TWO_PI * (i + 1) / sides;
			triangle(vc, m, x, y, x + Mth.cos(a0) * size, y + Mth.sin(a0) * size, x + Mth.cos(a1) * size, y + Mth.sin(a1) * size, z, rgb, alpha, rgb, 0, rgb, 0);
		}
	}

	/** A star: a four-pointed glint {@code size} across, its points thin. */
	private static void star(VertexConsumer vc, Matrix4f m, float x, float y, float z, float size, float[] rgb, float alpha) {
		float w = size * 0.22f;
		vertex(vc, m, x, y + size, z, rgb, alpha);
		vertex(vc, m, x + w, y, z, rgb, alpha);
		vertex(vc, m, x, y - size, z, rgb, alpha);
		vertex(vc, m, x - w, y, z, rgb, alpha);
		vertex(vc, m, x + size, y, z, rgb, alpha);
		vertex(vc, m, x, y - w, z, rgb, alpha);
		vertex(vc, m, x - size, y, z, rgb, alpha);
		vertex(vc, m, x, y + w, z, rgb, alpha);
	}

	/**
	 * A band along a polyline ({@code n} points of {@code xs, ys}), from {@code from} to {@code to} off it toward its outer side (its right, going
	 * along it; outward round a closed outline drawn clockwise), {@code alphaIn} at {@code from} to {@code alphaOut} at {@code to}, each piece's
	 * light flickering by {@code flicker} (null: steady).
	 */
	private static void band(VertexConsumer vc, Matrix4f m, float[] xs, float[] ys, int n, boolean closed, float from, float to, float z, float[] rgb,
			float alphaIn, float alphaOut, float[] flicker) {
		if (n < 2) return;
		float[] nx = new float[n], ny = new float[n];
		for (int i = 0; i < n; i++) {
			int prev = closed ? (i - 1 + n) % n : Math.max(0, i - 1), next = closed ? (i + 1) % n : Math.min(n - 1, i + 1);
			float dx = xs[next] - xs[prev], dy = ys[next] - ys[prev], l = Mth.sqrt(dx * dx + dy * dy);
			if (l < 1.0e-5f) {
				nx[i] = 0;
				ny[i] = 0;
				continue;
			}
			nx[i] = -dy / l;
			ny[i] = dx / l;
		}
		int pieces = closed ? n : n - 1;
		for (int i = 0; i < pieces; i++) {
			int j = (i + 1) % n;
			float f = flicker == null ? 1 : flicker[i];
			vertex(vc, m, xs[i] + nx[i] * from, ys[i] + ny[i] * from, z, rgb, alphaIn * f);
			vertex(vc, m, xs[i] + nx[i] * to, ys[i] + ny[i] * to, z, rgb, alphaOut * f);
			vertex(vc, m, xs[j] + nx[j] * to, ys[j] + ny[j] * to, z, rgb, alphaOut * f);
			vertex(vc, m, xs[j] + nx[j] * from, ys[j] + ny[j] * from, z, rgb, alphaIn * f);
		}
	}

	/** A glowing line along a polyline: a white-hot thread {@code core} wide in a glow {@code halo} wide, both sides, fading toward its end by {@code tail}. */
	private static void crack(VertexConsumer vc, Matrix4f m, float[] xs, float[] ys, int n, float core, float halo, float z, Palette colours, float alpha) {
		band(vc, m, xs, ys, n, false, 0, core, z, colours.hot, alpha, alpha * 0.55f, null);
		band(vc, m, xs, ys, n, false, 0, -core, z, colours.hot, alpha, alpha * 0.55f, null);
		band(vc, m, xs, ys, n, false, 0, halo, z, colours.glow, alpha * 0.4f, 0, null);
		band(vc, m, xs, ys, n, false, 0, -halo, z, colours.glow, alpha * 0.4f, 0, null);
	}

	// --- A tear (standing, or an abyss lying open) ---

	/**
	 * A tear {@code half} long each way from its heart and {@code wide} across at its widest, opened {@code open} of the way, at {@code time}:
	 * {@code lift} which way its shards rise out of its plane (0: they drift in it; -1: along its own -z, up out of an abyss lying flat).
	 */
	private static void tear(MultiBufferSource buffers, PoseStack pose, long seed, float half, float wide, float open, float time, Palette colours, int lift) {
		Matrix4f m = pose.last().pose();
		int sides = Mth.clamp((int) (half / 1.6f) + 8, 10, 30);
		float halfWidth = wide / 2, across = SpellRender.easeOut(open), along = (float) Math.sqrt(open);
		float bendPhase = hash(seed, 0, 1) * Mth.TWO_PI;
		// Down its right side, top tip to bottom tip, then up its left (clockwise): the tips shared.
		int n = sides * 2 - 2;
		float[] xs = new float[n], ys = new float[n];
		float[] rightX = new float[sides], leftX = new float[sides], y = new float[sides], middleX = new float[sides];
		for (int k = 0; k < sides; k++) {
			float t = k / (sides - 1f);
			float v = 1 - 2 * t;
			if (k > 0 && k < sides - 1) v += (hash(seed, k, 2) - 0.5f) * 0.7f / (sides - 1);
			float profile = (float) Math.pow(Math.max(0, 1 - v * v), 0.7);
			float bend = 0.22f * Mth.sin(v * 2.1f + bendPhase);
			float crackle = 0.05f * Mth.sin(time * 0.8f + k * 1.9f) * profile;
			float jagRight = (k % 2 == 0 ? 1 : -0.55f) * (0.25f + 0.75f * hash(seed, k, 3)) * JAG;
			float jagLeft = (k % 2 == 1 ? 1 : -0.55f) * (0.25f + 0.75f * hash(seed, k, 4)) * JAG;
			rightX[k] = (bend + profile * (1 + jagRight + crackle)) * halfWidth * across;
			leftX[k] = (bend - profile * (1 + jagLeft - crackle)) * halfWidth * across;
			middleX[k] = bend * halfWidth * across;
			y[k] = v * half * along;
		}
		for (int k = 0; k < sides; k++) {
			xs[k] = rightX[k];
			ys[k] = y[k];
		}
		for (int k = sides - 2; k >= 1; k--) {
			int i = sides + (sides - 2 - k);
			xs[i] = leftX[k];
			ys[i] = y[k];
		}

		// The void within: dark, a little light spilling in from its rim.
		VertexConsumer vc = dark(buffers);
		for (int k = 0; k < sides - 1; k++) {
			float[] edge = colours.deep, heart = scale(colours.deep, 0.15f);
			vertex(vc, m, leftX[k], y[k], 0, edge, 1);
			vertex(vc, m, middleX[k], y[k], 0, heart, 1);
			vertex(vc, m, middleX[k + 1], y[k + 1], 0, heart, 1);
			vertex(vc, m, leftX[k + 1], y[k + 1], 0, edge, 1);
			vertex(vc, m, middleX[k], y[k], 0, heart, 1);
			vertex(vc, m, rightX[k], y[k], 0, edge, 1);
			vertex(vc, m, rightX[k + 1], y[k + 1], 0, edge, 1);
			vertex(vc, m, middleX[k + 1], y[k + 1], 0, heart, 1);
		}

		vc = glow(buffers);
		float pulse = (0.86f + 0.14f * Mth.sin(time * 0.23f)) * Math.min(1, open * 1.4f);
		// Its light a little before the void (toward the eye; up out of an abyss), clear of it at any distance.
		float z = lift != 0 ? 0.1f * lift : 0.1f;
		// Within: a nebula of its colour turning slowly round its heart, its heart's faint light, its stars, motes drawn in.
		inside(vc, m, seed, sides, rightX, leftX, middleX, y, half * along, halfWidth * across, time, colours, pulse, z);
		// Its rim: the light spilling in over its inner edge, its white-hot edge, its glow, its halo; each piece flickering.
		float[] flicker = new float[n];
		for (int i = 0; i < n; i++) flicker[i] = 0.78f + 0.22f * Mth.sin(time * 1.3f + i * 2.7f + hash(seed, i, 5) * 6);
		float scaleBy = Math.min(half, 40);
		float hot = 0.07f + 0.006f * scaleBy, glowWidth = hot + 0.35f + 0.035f * scaleBy, haloWidth = glowWidth + 0.8f + 0.09f * scaleBy;
		band(vc, m, xs, ys, n, true, 0, -(0.3f + 0.05f * scaleBy), z, colours.glow, 0.55f * pulse, 0, flicker);
		band(vc, m, xs, ys, n, true, -hot * 0.5f, hot, z, colours.hot, pulse, pulse * 0.85f, flicker);
		band(vc, m, xs, ys, n, true, hot, glowWidth, z, colours.glow, 0.7f * pulse, 0, flicker);
		band(vc, m, xs, ys, n, true, glowWidth, haloWidth, z, colours.halo, 0.16f * pulse, 0, null);
		// Space cracked round it, and shards of it breaking away.
		cracks(vc, m, seed, xs, ys, n, halfWidth, half, open, time, colours, z);
		shards(buffers, seed, pose, xs, ys, n, halfWidth, half, open, time, colours, lift);
	}

	/** The void within a tear: its nebula turning, its heart's faint light, its stars, and motes of light drawn in toward its heart. */
	private static void inside(VertexConsumer vc, Matrix4f m, long seed, int sides, float[] rightX, float[] leftX, float[] middleX, float[] y, float half, float halfWidth,
			float time, Palette colours, float pulse, float z) {
		if (half < 0.05f) return;
		// A point within it: v along it (-1 at its foot to 1 at its top), u across (-1 its left edge to 1 its right, there).
		java.util.function.BiFunction<Float, Float, float[]> within = (u, v) -> {
			float t = Mth.clamp((1 - v) / 2 * (sides - 1), 0, sides - 1.001f);
			int k = (int) t;
			float f = t - k;
			float mid = Mth.lerp(f, middleX[k], middleX[k + 1]), right = Mth.lerp(f, rightX[k], rightX[k + 1]), left = Mth.lerp(f, leftX[k], leftX[k + 1]);
			float x = u >= 0 ? mid + (right - mid) * u : mid + (mid - left) * u;
			return new float[] {x, Mth.lerp(f, y[k], y[k + 1])};
		};
		// The nebula: three arms of faint light of its colour, turning slowly round its heart.
		for (int arm = 0; arm < 3; arm++) {
			for (int i = 0; i < 10; i++) {
				float rho = 0.1f + 0.085f * i, phi = arm * Mth.TWO_PI / 3 + i * 0.5f + time * 0.008f;
				float[] p = within.apply(rho * Mth.cos(phi) * 0.8f, rho * Mth.sin(phi) * 0.9f);
				blot(vc, m, p[0], p[1], z, halfWidth * (0.35f + 0.25f * (1 - rho)), arm == 1 ? colours.halo : colours.glow, 0.11f * (1 - rho * 0.7f) * pulse);
			}
		}
		blot(vc, m, middleX[sides / 2], y[sides / 2], z, halfWidth * 0.9f, colours.halo, 0.12f * pulse);
		// Its stars, twinkling, turning slowly with it.
		int stars = Mth.clamp((int) (half * 1.6f) + 24, 24, 90);
		float starSize = Math.min(0.55f, 0.07f + half * 0.012f);
		for (int j = 0; j < stars; j++) {
			float rho = (float) Math.sqrt(hash(seed, j, 11)) * 0.78f, phi = hash(seed, j, 12) * Mth.TWO_PI + time * (0.002f + 0.004f * hash(seed, j, 13));
			float[] p = within.apply(rho * Mth.cos(phi), rho * Mth.sin(phi) * 1.15f);
			float twinkle = 0.45f + 0.55f * Mth.sin(time * (0.08f + 0.2f * hash(seed, j, 14)) + hash(seed, j, 15) * 6);
			float[] colour = hash(seed, j, 16) < 0.6f ? WHITE : colours.glass;
			star(vc, m, p[0], p[1], z, starSize * (0.4f + 0.8f * hash(seed, j, 17)), colour, twinkle * pulse);
		}
		// Motes of light drawn in from its rim toward its heart, and swallowed.
		for (int j = 0; j < 18; j++) {
			float phase = (time * 0.014f + j / 18f) % 1f;
			float u = (hash(seed, j, 21) < 0.5f ? -1 : 1) * (1 - phase * phase), v = (hash(seed, j, 22) * 2 - 1) * 0.85f * (1 - phase * phase);
			float[] p = within.apply(u * 0.9f, v);
			star(vc, m, p[0], p[1], z, starSize * 0.8f, colours.hot, (1 - phase) * 0.85f * pulse);
		}
	}

	/**
	 * Space cracked round a tear like glass: cracks running out from its rim's sharpest points, wandering and forking, glowing, out further the
	 * wider it has opened.
	 */
	private static void cracks(VertexConsumer vc, Matrix4f m, long seed, float[] xs, float[] ys, int n, float halfWidth, float half, float open, float time,
			Palette colours, float z) {
		float reach = Mth.clamp((open - 0.15f) / 0.85f, 0, 1);
		if (reach <= 0) return;
		float core = 0.04f + 0.004f * Math.min(half, 40), halo = core * 5;
		float ref = Math.max(halfWidth, half * 0.35f);
		for (int i = 0; i < n; i++) {
			if (hash(seed, i, 31) > 0.62f) continue; // not from every point of the rim
			float dx = xs[(i + 1) % n] - xs[(i - 1 + n) % n], dy = ys[(i + 1) % n] - ys[(i - 1 + n) % n], l = Mth.sqrt(dx * dx + dy * dy);
			if (l < 1.0e-5f) continue;
			float angle = (float) Math.atan2(dx / l, -dy / l) + (hash(seed, i, 32) - 0.5f) * 0.8f; // out from the rim
			float total = ref * (0.6f + 1.1f * hash(seed, i, 33)) * reach;
			int pieces = 4;
			float[] cx = new float[pieces + 1], cy = new float[pieces + 1];
			cx[0] = xs[i];
			cy[0] = ys[i];
			for (int p = 1; p <= pieces; p++) {
				angle += (hash(seed, i * 7 + p, 34) - 0.5f) * 0.9f;
				float step = total / pieces * (1.25f - 0.15f * p);
				cx[p] = cx[p - 1] + Mth.cos(angle) * step;
				cy[p] = cy[p - 1] + Mth.sin(angle) * step;
			}
			float flicker = 0.75f + 0.25f * Mth.sin(time * 0.9f + i * 3.1f);
			crack(vc, m, cx, cy, pieces + 1, core, halo, z, colours, flicker * Math.min(1, open * 1.5f));
			// A fork off its second piece.
			if (hash(seed, i, 35) < 0.65f) {
				float side = hash(seed, i, 36) < 0.5f ? -1 : 1;
				float a = (float) Math.atan2(cy[2] - cy[1], cx[2] - cx[1]) + side * (0.55f + 0.4f * hash(seed, i, 37));
				float step = total * 0.22f;
				float[] fx = {cx[1], cx[1] + Mth.cos(a) * step, cx[1] + Mth.cos(a + side * 0.3f) * step * 1.8f};
				float[] fy = {cy[1], cy[1] + Mth.sin(a) * step, cy[1] + Mth.sin(a + side * 0.3f) * step * 1.8f};
				crack(vc, m, fx, fy, 3, core * 0.7f, halo * 0.7f, z, colours, flicker * 0.8f * Math.min(1, open * 1.5f));
			}
		}
	}

	/**
	 * Shards of space breaking off a tear's rim (or {@code xs, ys}, any outline) and drifting off, tumbling and fading, one after another:
	 * pale glass, or pieces of the void itself; out of its plane along its own -z if {@code lift} (an abyss's, rising up out of it).
	 */
	private static void shards(MultiBufferSource buffers, long seed, PoseStack pose, float[] xs, float[] ys, int n, float halfWidth, float half, float open, float time,
			Palette colours, int lift) {
		if (open < 0.45f || n == 0) return;
		Matrix4f m = pose.last().pose();
		int count = Mth.clamp((int) (half * 0.6f) + 8, 8, 26);
		float fade = Mth.clamp((open - 0.45f) / 0.4f, 0, 1);
		float drift = Math.max(halfWidth, half * 0.3f);
		for (int j = 0; j < count; j++) {
			float period = 40 + 50 * hash(seed, j, 41);
			float phase = ((time + hash(seed, j, 42) * period) % period) / period;
			int i = (int) (hash(seed, j, 43) * n) % n;
			float dx = xs[(i + 1) % n] - xs[(i - 1 + n) % n], dy = ys[(i + 1) % n] - ys[(i - 1 + n) % n], l = Mth.sqrt(dx * dx + dy * dy);
			if (l < 1.0e-5f) continue;
			float nx = -dy / l, ny = dx / l; // out from the rim
			float out = (0.15f + phase * (0.9f + 0.8f * hash(seed, j, 44))) * drift;
			float cx = xs[i] + nx * out + (hash(seed, j, 45) - 0.5f) * drift * 0.3f * phase, cy = ys[i] + ny * out + (hash(seed, j, 46) - 0.5f) * drift * 0.3f * phase;
			float cz = lift != 0 ? lift * phase * drift * 1.2f : 0.03f;
			float size = (0.12f + 0.16f * hash(seed, j, 47)) * Math.min(halfWidth * 1.2f, 6) * (1 - 0.4f * phase);
			float spin = hash(seed, j, 48) * Mth.TWO_PI + phase * (1.5f + 2 * hash(seed, j, 49)), tumble = Mth.cos(phase * Mth.PI * (1 + hash(seed, j, 50)));
			float alpha = Mth.sin(phase * Mth.PI) * fade;
			float[] px = new float[3], py = new float[3];
			for (int c = 0; c < 3; c++) {
				float a = spin + c * 2.1f + (hash(seed, j * 3 + c, 51) - 0.5f) * 0.9f, r = size * (0.6f + 0.4f * hash(seed, j * 3 + c, 52));
				px[c] = cx + Mth.cos(a) * r * tumble;
				py[c] = cy + Mth.sin(a) * r;
			}
			boolean ofTheVoid = hash(seed, j, 53) < 0.4f;
			if (ofTheVoid) {
				VertexConsumer vc = dark(buffers);
				triangle(vc, m, px[0], py[0], px[1], py[1], px[2], py[2], cz, colours.deep, alpha, colours.deep, alpha, scale(colours.deep, 0.3f), alpha);
			} else {
				VertexConsumer vc = glow(buffers);
				triangle(vc, m, px[0], py[0], px[1], py[1], px[2], py[2], cz, colours.glass, 0.22f * alpha, colours.glass, 0.08f * alpha, colours.glass, 0.14f * alpha);
			}
			VertexConsumer vc = glow(buffers);
			float[] ex = {px[0], px[1], px[2], px[0]}, ey = {py[0], py[1], py[2], py[0]};
			float edge = 0.03f + size * 0.05f;
			float glint = cz + (lift != 0 ? 0.05f * lift : 0.05f);
			band(vc, m, ex, ey, 4, false, -edge, edge, glint, colours.hot, 0.85f * alpha, 0.85f * alpha, null);
			band(vc, m, ex, ey, 4, false, 0, edge * 4, glint, colours.glow, 0.3f * alpha, 0, null);
			band(vc, m, ex, ey, 4, false, 0, -edge * 4, glint, colours.glow, 0.3f * alpha, 0, null);
		}
	}

	// --- A cut along a line ---

	/**
	 * A cut in space from where {@code rift} stands along its facing, {@code length} long: running out along it as the stroke that cut it
	 * goes ({@link #CUT_RUNS}), opening behind it, and turned about its length to face whoever looks.
	 */
	private static void line(MultiBufferSource buffers, PoseStack pose, SpellConstruct rift, float partialTick, Camera camera, long seed, float length, float open,
			float time, Palette colours) {
		float run = Mth.clamp(time / CUT_RUNS, 0, 1);
		float reach = length * run;
		if (reach < 0.5f) return;
		Vec3 way = Vec3.directionFromRotation(rift.getXRot(), rift.getYRot());
		Vec3 at = rift.getPosition(partialTick), eye = camera.getPosition().subtract(at);
		// Its plane: along it, and turned about it to face the eye (the eye from its middle).
		Vec3 toEye = eye.subtract(way.scale(Mth.clamp(eye.dot(way), 0, reach)));
		Vec3 facing = toEye.subtract(way.scale(toEye.dot(way)));
		if (facing.lengthSqr() < 1.0e-6) facing = Math.abs(way.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
		facing = facing.normalize();
		Vec3 side = way.cross(facing).normalize();
		Matrix4f basis = new Matrix4f().set((float) side.x, (float) side.y, (float) side.z, 0, (float) way.x, (float) way.y, (float) way.z, 0,
				(float) facing.x, (float) facing.y, (float) facing.z, 0, 0, 0, 0, 1);
		pose.mulPoseMatrix(basis);
		Matrix4f m = pose.last().pose();
		// Its outline: up its left edge from its start to its end, down its right: wide along most of its length, drawn to points at its ends.
		float halfWidth = 0.9f + length * 0.008f;
		int sides = Mth.clamp((int) (reach / 2.2f) + 6, 8, 80);
		float[] left = new float[sides], right = new float[sides], y = new float[sides];
		for (int k = 0; k < sides; k++) {
			float t = k / (sides - 1f), s = t * reach;
			// Open behind the stroke: a little later the further along.
			float here = Mth.clamp((time - t * run * CUT_RUNS) / 4f, 0, 1) * open;
			float profile = (float) Math.pow(Math.max(0, 1 - (2 * t - 1) * (2 * t - 1)), 0.3f) * SpellRender.easeOut(here);
			float crackle = 0.06f * Mth.sin(time * 0.9f + k * 2.3f);
			float jagL = (k % 2 == 0 ? 1 : -0.5f) * (0.2f + 0.8f * hash(seed, k, 61)) * JAG, jagR = (k % 2 == 1 ? 1 : -0.5f) * (0.2f + 0.8f * hash(seed, k, 62)) * JAG;
			left[k] = -profile * (1 + jagL + crackle) * halfWidth;
			right[k] = profile * (1 + jagR - crackle) * halfWidth;
			y[k] = s + (k > 0 && k < sides - 1 ? (hash(seed, k, 63) - 0.5f) * reach / sides * 0.6f : 0);
		}
		int n = sides * 2 - 2;
		float[] xs = new float[n], ys = new float[n];
		// Clockwise in its plane (x across, y along it): out along its left edge, back along its right.
		for (int k = 0; k < sides; k++) {
			xs[k] = left[k];
			ys[k] = y[k];
		}
		for (int k = sides - 2; k >= 1; k--) {
			int i = sides + (sides - 2 - k);
			xs[i] = right[k];
			ys[i] = y[k];
		}
		VertexConsumer vc = dark(buffers);
		for (int k = 0; k < sides - 1; k++) {
			float[] edge = colours.deep, heart = scale(colours.deep, 0.15f);
			vertex(vc, m, left[k], y[k], 0, edge, 1);
			vertex(vc, m, 0, y[k], 0, heart, 1);
			vertex(vc, m, 0, y[k + 1], 0, heart, 1);
			vertex(vc, m, left[k + 1], y[k + 1], 0, edge, 1);
			vertex(vc, m, 0, y[k], 0, heart, 1);
			vertex(vc, m, right[k], y[k], 0, edge, 1);
			vertex(vc, m, right[k + 1], y[k + 1], 0, edge, 1);
			vertex(vc, m, 0, y[k + 1], 0, heart, 1);
		}
		vc = glow(buffers);
		float pulse = (0.86f + 0.14f * Mth.sin(time * 0.23f)) * Math.min(1, open * 1.4f);
		// The void within: stars along it, twinkling, drifting slowly back along it; motes drawn in.
		int stars = Mth.clamp((int) (reach * 0.8f), 6, 140);
		for (int j = 0; j < stars; j++) {
			float t = (hash(seed, j, 71) + time * 0.0015f * (0.5f + hash(seed, j, 72))) % 1f;
			int k = Mth.clamp((int) (t * (sides - 1)), 0, sides - 2);
			float f = t * (sides - 1) - k, u = (hash(seed, j, 73) * 2 - 1) * 0.7f;
			float w = u >= 0 ? Mth.lerp(f, right[k], right[k + 1]) * u : -Mth.lerp(f, left[k], left[k + 1]) * u;
			float twinkle = 0.45f + 0.55f * Mth.sin(time * (0.1f + 0.2f * hash(seed, j, 74)) + hash(seed, j, 75) * 6);
			star(vc, m, w, Mth.lerp(f, y[k], y[k + 1]), 0.1f, 0.08f + 0.18f * hash(seed, j, 76), hash(seed, j, 77) < 0.6f ? WHITE : colours.glass, twinkle * pulse);
		}
		float[] flicker = new float[n];
		for (int i = 0; i < n; i++) flicker[i] = 0.78f + 0.22f * Mth.sin(time * 1.3f + i * 2.7f + hash(seed, i, 78) * 6);
		float hot = 0.09f, glowWidth = 0.6f, haloWidth = 1.9f;
		band(vc, m, xs, ys, n, true, 0, -0.45f, 0.1f, colours.glow, 0.55f * pulse, 0, flicker);
		band(vc, m, xs, ys, n, true, -hot * 0.5f, hot, 0.1f, colours.hot, pulse, pulse * 0.85f, flicker);
		band(vc, m, xs, ys, n, true, hot, glowWidth, 0.1f, colours.glow, 0.7f * pulse, 0, flicker);
		band(vc, m, xs, ys, n, true, glowWidth, haloWidth, 0.1f, colours.halo, 0.16f * pulse, 0, null);
		cracks(vc, m, seed, xs, ys, n, halfWidth * 1.4f, halfWidth * 3, open, time, colours, 0.1f);
		shards(buffers, seed, pose, xs, ys, n, halfWidth, halfWidth * 4, open, time, colours, 0);
	}

	// --- Space shattered like glass ---

	/**
	 * Space shattered round a point like a pane struck, {@code radius} round: a ragged hole of the void at its heart; cracks running out from it
	 * to its edge and rings of cracks across them, racing out as it shatters; here and there a piece fallen out, showing the void; shards
	 * falling away and tumbling.
	 */
	private static void shatter(MultiBufferSource buffers, PoseStack pose, long seed, float radius, float open, float time, Palette colours) {
		Matrix4f m = pose.last().pose();
		float out = radius * SpellRender.easeOut(Mth.clamp(time / 7f, 0, 1));
		float pulse = (0.85f + 0.15f * Mth.sin(time * 0.3f)) * open;
		int spokes = 13 + (int) (hash(seed, 0, 81) * 5);
		float[] angle = new float[spokes], length = new float[spokes];
		for (int k = 0; k < spokes; k++) {
			angle[k] = Mth.TWO_PI * (k + (hash(seed, k, 82) - 0.5f) * 0.6f) / spokes;
			length[k] = radius * (0.72f + 0.28f * hash(seed, k, 83));
		}
		float[] rings = {0.12f, 0.36f, 0.64f, 0.9f};
		// The point at fraction r of the way out along spoke k (wandering a little).
		java.util.function.BiFunction<Integer, Float, float[]> point = (k, r) -> {
			float a = angle[k] + 0.08f * Mth.sin(r * 9 + k) * r;
			float d = Math.min(r * radius, length[k]);
			return new float[] {Mth.cos(a) * d, Mth.sin(a) * d};
		};
		// The hole of the void at its heart, ragged.
		int holeSides = 14;
		float hole = radius * rings[0] * Math.min(1, open * 1.5f);
		float[] hx = new float[holeSides], hy = new float[holeSides];
		for (int i = 0; i < holeSides; i++) {
			float a = -Mth.TWO_PI * i / holeSides, r = hole * (1 + (i % 2 == 0 ? 0.35f : -0.2f) * hash(seed, i, 84));
			hx[i] = Mth.cos(a) * r;
			hy[i] = Mth.sin(a) * r;
		}
		VertexConsumer vc = dark(buffers);
		for (int i = 0; i < holeSides; i++) {
			int j = (i + 1) % holeSides;
			triangle(vc, m, 0, 0, hx[i], hy[i], hx[j], hy[j], 0, scale(colours.deep, 0.1f), 1, colours.deep, 1, colours.deep, 1);
		}
		// Pieces fallen out between the cracks, the void showing through, the nearer its heart the more.
		for (int ring = 0; ring < rings.length - 1; ring++) {
			float r0 = rings[ring], r1 = rings[ring + 1];
			if (out < r1 * radius * 0.95f) continue;
			for (int k = 0; k < spokes; k++) {
				if (hash(seed, k * 5 + ring, 85) > 0.24f - ring * 0.06f) continue;
				int k1 = (k + 1) % spokes;
				float[] a = point.apply(k, r0), b = point.apply(k1, r0), c = point.apply(k1, r1), d = point.apply(k, r1);
				vertex(vc, m, a[0], a[1], 0, colours.deep, open);
				vertex(vc, m, b[0], b[1], 0, colours.deep, open);
				vertex(vc, m, c[0], c[1], 0, scale(colours.deep, 0.3f), open);
				vertex(vc, m, d[0], d[1], 0, scale(colours.deep, 0.3f), open);
			}
		}
		vc = glow(buffers);
		// The panes between the cracks catching the light, each a little differently.
		for (int ring = 0; ring < rings.length - 1; ring++) {
			float r0 = rings[ring], r1 = rings[ring + 1];
			if (out < r1 * radius * 0.6f) continue;
			for (int k = 0; k < spokes; k++) {
				float sheen = hash(seed, k * 5 + ring, 86);
				if (sheen < 0.4f) continue;
				int k1 = (k + 1) % spokes;
				float[] a = point.apply(k, r0), b = point.apply(k1, r0), c = point.apply(k1, r1), d = point.apply(k, r1);
				float glint = 0.05f + 0.08f * sheen * (0.6f + 0.4f * Mth.sin(time * 0.15f + k + ring));
				vertex(vc, m, a[0], a[1], 0.08f, colours.glass, glint * pulse);
				vertex(vc, m, b[0], b[1], 0.08f, colours.glass, glint * pulse * 0.5f);
				vertex(vc, m, c[0], c[1], 0.08f, colours.glass, glint * pulse * 0.2f);
				vertex(vc, m, d[0], d[1], 0.08f, colours.glass, glint * pulse * 0.5f);
			}
		}
		float core = 0.05f + radius * 0.004f, halo = core * 6;
		// The hole's rim burning.
		band(vc, m, hx, hy, holeSides, true, -core, core * 2, 0.1f, colours.hot, pulse, pulse * 0.7f, null);
		band(vc, m, hx, hy, holeSides, true, core * 2, halo * 2, 0.1f, colours.glow, 0.7f * pulse, 0, null);
		blot(vc, m, 0, 0, 0.1f, radius * 0.5f, colours.halo, 0.25f * pulse);
		// The cracks out from it, racing out as it shatters...
		for (int k = 0; k < spokes; k++) {
			int pieces = 6;
			float reach = Math.min(length[k], out);
			if (reach <= hole) continue;
			float[] cx = new float[pieces + 1], cy = new float[pieces + 1];
			for (int p = 0; p <= pieces; p++) {
				float[] at = point.apply(k, (hole + (reach - hole) * p / pieces) / radius);
				cx[p] = at[0];
				cy[p] = at[1];
			}
			crack(vc, m, cx, cy, pieces + 1, core, halo, 0.1f, colours, pulse * (0.8f + 0.2f * Mth.sin(time + k * 2.3f)));
		}
		// ...and the rings across them, once they have reached them (bowed a little, here and there broken).
		for (int ring = 1; ring < rings.length; ring++) {
			float r = rings[ring];
			if (out < r * radius) continue;
			for (int k = 0; k < spokes; k++) {
				if (hash(seed, k * 7 + ring, 87) < 0.2f) continue;
				int k1 = (k + 1) % spokes;
				float[] a = point.apply(k, r), b = point.apply(k1, r);
				float bow = 1 + (hash(seed, k * 7 + ring, 88) - 0.5f) * 0.12f;
				float mx = (a[0] + b[0]) / 2 * bow, my = (a[1] + b[1]) / 2 * bow;
				crack(vc, m, new float[] {a[0], mx, b[0]}, new float[] {a[1], my, b[1]}, 3, core * 0.8f, halo * 0.7f, 0.1f, colours, pulse * 0.85f);
			}
		}
		// Shards falling away from it, tumbling (its outer ring's points their starts).
		float[] rx = new float[spokes], ry = new float[spokes];
		for (int k = 0; k < spokes; k++) {
			float[] at = point.apply(spokes - 1 - k, rings[2]);
			rx[k] = at[0];
			ry[k] = at[1];
		}
		if (out >= rings[2] * radius) shards(buffers, seed, pose, rx, ry, spokes, radius * 0.25f, radius, open, time, colours, 0);
	}

	/** The render types of its own, made the way vanilla's are (extends RenderType only to reach its parts). */
	private static final class Types extends RenderType {
		/** The void: dark, its colour per point, both faces, hiding what is behind it (depth written), drawn over what is before it no more than glass. */
		static final RenderType VOID = create("defying_the_heavens_rift_void", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, true,
				CompositeState.builder()
						.setShaderState(RENDERTYPE_LIGHTNING_SHADER)
						.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
						.setCullState(NO_CULL)
						.setWriteMaskState(COLOR_DEPTH_WRITE)
						.createCompositeState(false));

		private Types(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
			super(name, format, mode, size, crumbling, sort, setup, clear);
		}
	}
}
