package com.example.defyingtheheavens.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static com.example.defyingtheheavens.client.SpellRender.v;

/**
 * The element spells' solid things, built from quads in their own space (the point toward +z): a straight sword (jian) of steel
 * with a gold guard, a podao of dried earth, a stake of bark, a lumpy boulder of a block's own stone. Sized in blocks.
 */
public final class SpellModels {
	private static final float[] WHITE = {1, 1, 1};
	/** The qi about a spell's weapon: a sword's white-gold, a stake's green, a podao's amber. */
	private static final float[] QI_LIGHT = {1.0f, 0.93f, 0.75f}, WOOD_QI = {0.45f, 1.0f, 0.5f}, EARTH_QI = {1.0f, 0.7f, 0.3f};

	/**
	 * A jian {@code length} long from its guard (at the origin) to its point (+z): a diamond-section blade of steel, a gold guard and
	 * pommel, a grip bound in dark red. {@code grow} (0 to 1) draws only that much of it (a sword forming out of its shards).
	 */
	public static void jian(PoseStack pose, MultiBufferSource buffers, float length, int light, float grow, float[] tint) {
		if (grow <= 0) return;
		PoseStack.Pose p = pose.last();
		float w = 0.11f * length / 1.3f, t = 0.035f * length / 1.3f;
		float l = length * grow;
		VertexConsumer steel = buffers.getBuffer(SpellRender.solid(SpellRender.STEEL));
		float[] c = tint == null ? WHITE : tint;
		// The blade: diamond rings from the guard to a waist near the point, then the point.
		float[][] rings = {{0, w}, {l * 0.82f, w * 0.88f}, {l, 0}};
		for (int i = 0; i < rings.length - 1; i++) {
			float z0 = rings[i][0], w0 = rings[i][1], z1 = rings[i + 1][0], w1 = rings[i + 1][1];
			Vector3f[] a = diamond(z0, w0, t * (w0 / w)), b = diamond(z1, w1, t * (w1 / Math.max(1.0e-4f, w)));
			for (int k = 0; k < 4; k++) {
				int n = (k + 1) % 4;
				SpellRender.quad(steel, p, a[k], b[k], b[n], a[n], c, 1, k % 2 == 0 ? 0 : 0.5f, z0 / l, k % 2 == 0 ? 0.5f : 1, z1 / l, light);
			}
		}
		if (grow < 0.35f) return;
		// The sword's qi about its blade: a soft glow along it, brightest at the edge, and a glint running up it.
		VertexConsumer aura = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
		List<Vec3> spine = List.of(new Vec3(0, 0, 0), new Vec3(0, 0, l * 0.5), new Vec3(0, 0, l * 0.92), new Vec3(0, 0, l));
		SpellRender.tube(aura, p, spine, new float[]{w * 1.3f, w * 1.2f, w * 0.8f, w * 0.15f}, 6, QI_LIGHT, 0.32f, 0.5f / Math.max(0.3f, l), 0,
				SpellRender.FULL_BRIGHT, null);
		VertexConsumer edge = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
		float glint = (float) ((System.currentTimeMillis() % 1400) / 1400.0);
		SpellRender.quad(edge, p, v(-w * 0.6f, 0, l * glint - w), v(w * 0.6f, 0, l * glint - w), v(w * 0.6f, 0, l * glint + w), v(-w * 0.6f, 0, l * glint + w),
				WHITE, 0.6f * Mth.sin((float) Math.PI * glint), 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		VertexConsumer gold = buffers.getBuffer(SpellRender.solid(SpellRender.GOLD));
		float g = Math.min(1, (grow - 0.35f) / 0.4f);
		SpellRender.box(gold, p, -w * 1.5f * g, -t * 1.3f, -0.03f, w * 1.5f * g, t * 1.3f, 0.03f, WHITE, 1, 0, 0, 1, 0.3f, light);
		VertexConsumer grip = buffers.getBuffer(SpellRender.solid(SpellRender.BARK));
		float gl = length * 0.24f * g;
		SpellRender.box(grip, p, -t * 1.1f, -t * 1.1f, -gl, t * 1.1f, t * 1.1f, -0.03f, new float[]{0.55f, 0.16f, 0.12f}, 1, 0, 0, 0.3f, 1, light);
		SpellRender.box(gold, p, -t * 1.6f, -t * 1.6f, -gl - t * 2.6f, t * 1.6f, t * 1.6f, -gl, WHITE, 1, 0.2f, 0.4f, 0.5f, 0.7f, light);
	}

	private static Vector3f[] diamond(float z, float width, float thickness) {
		return new Vector3f[]{v(width / 2, 0, z), v(0, thickness / 2, z), v(-width / 2, 0, z), v(0, -thickness / 2, z)};
	}

	/**
	 * A podao of dried, cracked earth (its point toward +z): a long haft behind a ring guard, and a broad single-edged blade that
	 * swells toward its end and sweeps up to the point, its edge below. {@code grow} draws that much of it (clods fusing).
	 */
	public static void podao(PoseStack pose, MultiBufferSource buffers, int light, float grow, float[] tint) {
		if (grow <= 0) return;
		PoseStack.Pose p = pose.last();
		VertexConsumer mud = buffers.getBuffer(SpellRender.solid(SpellRender.MUD));
		float[] c = tint == null ? WHITE : tint;
		float s = grow;
		// The haft, the guard, the pommel.
		SpellRender.box(mud, p, -0.045f * s, -0.045f * s, -1.25f * s, 0.045f * s, 0.045f * s, 0, c, 1, 0, 0, 0.15f, 1, light);
		SpellRender.box(mud, p, -0.1f * s, -0.12f * s, -0.04f, 0.1f * s, 0.12f * s, 0.04f, c, 1, 0.3f, 0.3f, 0.7f, 0.7f, light);
		SpellRender.box(mud, p, -0.07f * s, -0.07f * s, -1.36f * s, 0.07f * s, 0.07f * s, -1.25f * s, c, 1, 0.5f, 0.5f, 0.8f, 0.8f, light);
		// The blade: spine (top) and edge (bottom) along z, extruded a little each side.
		int steps = 12;
		float len = 1.25f * s, half = 0.035f * s;
		float[] zs = new float[steps + 1], tops = new float[steps + 1], bottoms = new float[steps + 1];
		for (int i = 0; i <= steps; i++) {
			float t = i / (float) steps;
			zs[i] = 0.04f + t * len;
			tops[i] = (0.07f + 0.1f * t * t * t) * s;
			float swell = 0.17f + 0.2f * Mth.sin((float) Math.PI * Math.min(1, t * 1.2f)) * (t < 0.83f ? 1 : 0);
			bottoms[i] = t < 0.85f ? -swell * s : Mth.lerp((t - 0.85f) / 0.15f, -0.2f * s, tops[i]);
		}
		for (int i = 0; i < steps; i++) {
			float u0 = i / (float) steps, u1 = (i + 1) / (float) steps;
			// Both flats.
			SpellRender.quad(mud, p, v(half, tops[i], zs[i]), v(half, bottoms[i], zs[i]), v(half, bottoms[i + 1], zs[i + 1]), v(half, tops[i + 1], zs[i + 1]),
					c, 1, u0, 0, u1, 1, light);
			SpellRender.quad(mud, p, v(-half, tops[i + 1], zs[i + 1]), v(-half, bottoms[i + 1], zs[i + 1]), v(-half, bottoms[i], zs[i]), v(-half, tops[i], zs[i]),
					c, 1, u1, 0, u0, 1, light);
			// The spine and the edge (narrower: a bevel).
			float[] dark = {c[0] * 0.75f, c[1] * 0.75f, c[2] * 0.75f};
			SpellRender.quad(mud, p, v(-half, tops[i], zs[i]), v(half, tops[i], zs[i]), v(half, tops[i + 1], zs[i + 1]), v(-half, tops[i + 1], zs[i + 1]),
					dark, 1, u0, 0, u1, 0.1f, light);
			SpellRender.quad(mud, p, v(half * 0.3f, bottoms[i], zs[i]), v(-half * 0.3f, bottoms[i], zs[i]), v(-half * 0.3f, bottoms[i + 1], zs[i + 1]),
					v(half * 0.3f, bottoms[i + 1], zs[i + 1]), dark, 1, u0, 0.9f, u1, 1, light);
		}
		// The blade's root against the guard.
		SpellRender.quad(mud, p, v(-half, tops[0], zs[0]), v(-half, bottoms[0], zs[0]), v(half, bottoms[0], zs[0]), v(half, tops[0], zs[0]), c, 1, 0, 0, 0.1f, 1, light);
		// The earth's qi in it: an amber glow about the blade, and its edge lit like a seam of the earth's fire.
		VertexConsumer aura = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
		List<Vec3> spine = List.of(new Vec3(0, -0.05 * s, 0.1 * s), new Vec3(0, -0.08 * s, 0.7 * s), new Vec3(0, -0.02 * s, 1.25 * s));
		SpellRender.tube(aura, p, spine, new float[]{0.3f * s, 0.34f * s, 0.12f * s}, 6, EARTH_QI, 0.28f, 0.5f, 0, SpellRender.FULL_BRIGHT, null);
		VertexConsumer seam = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
		for (int i = 0; i < steps; i++) {
			SpellRender.quad(seam, p, v(half * 1.2f, bottoms[i] + 0.02f * s, zs[i]), v(half * 1.2f, bottoms[i] - 0.02f * s, zs[i]),
					v(half * 1.2f, bottoms[i + 1] - 0.02f * s, zs[i + 1]), v(half * 1.2f, bottoms[i + 1] + 0.02f * s, zs[i + 1]), EARTH_QI, 0.8f, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	/** A stake of bark (its point toward +z), {@code length} long and {@code radius} thick at its foot. */
	public static void stake(PoseStack pose, MultiBufferSource buffers, float length, float radius, int light, float grow) {
		if (grow <= 0) return;
		VertexConsumer bark = buffers.getBuffer(SpellRender.solid(SpellRender.BARK));
		List<Vec3> points = new ArrayList<>();
		float[] radii = new float[7];
		float l = length * grow;
		float[] profile = {0.92f, 1.0f, 0.97f, 0.88f, 0.7f, 0.42f, 0.0f};
		for (int i = 0; i < 7; i++) {
			points.add(new Vec3(0, 0, -l / 2 + l * i / 6.0));
			radii[i] = radius * profile[i] * grow;
		}
		SpellRender.tube(bark, pose.last(), points, radii, 8, WHITE, 1, 1.2f, 0, light, null);
		// The foot's cut end: pale heartwood.
		PoseStack.Pose p = pose.last();
		float r = radii[0];
		float[] wood = {1.15f, 1.05f, 0.85f};
		for (int s = 0; s < 8; s++) {
			double a0 = Math.PI * 2 * s / 8, a1 = Math.PI * 2 * (s + 1) / 8;
			SpellRender.quad(bark, p, v(0, 0, -l / 2), v((float) Math.cos(a1) * r, (float) Math.sin(a1) * r, -l / 2), v((float) Math.cos(a0) * r, (float) Math.sin(a0) * r, -l / 2),
					v(0, 0, -l / 2), wood, 1, 0.4f, 0.4f, 0.6f, 0.6f, light);
		}
		// The wood's qi in it: a green glow about the stake, brightest toward its point.
		float[] glow = new float[radii.length];
		for (int i = 0; i < radii.length; i++) glow[i] = radius * 2.2f * grow * (0.6f + 0.4f * i / (radii.length - 1f));
		SpellRender.tube(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose.last(), points, glow, 6, WOOD_QI, 0.3f * grow, 0.5f, 0, SpellRender.FULL_BRIGHT, null);
	}

	/** A lumpy boulder of {@code radius} faced with {@code sprite} (a block's own texture), tumbling by {@code spin}. */
	public static void boulder(PoseStack pose, MultiBufferSource buffers, TextureAtlasSprite sprite, float radius, float spin, int light, long seed, float[] tint) {
		VertexConsumer vc = buffers.getBuffer(SpellRender.blocks());
		pose.pushPose();
		pose.mulPose(com.mojang.math.Axis.XP.rotation(spin));
		pose.mulPose(com.mojang.math.Axis.ZP.rotation(spin * 0.6f));
		PoseStack.Pose p = pose.last();
		int rings = 7, segments = 10;
		float[] c = tint == null ? WHITE : tint;
		for (int i = 0; i < rings; i++) {
			for (int j = 0; j < segments; j++) {
				Vector3f a = lump(radius, i, j, rings, segments, seed), b = lump(radius, i + 1, j, rings, segments, seed);
				Vector3f cc = lump(radius, i + 1, j + 1, rings, segments, seed), d = lump(radius, i, j + 1, rings, segments, seed);
				float shade = 0.7f + 0.3f * (1 - i / (float) rings);
				SpellRender.quad(vc, p, a, b, cc, d, new float[]{c[0] * shade, c[1] * shade, c[2] * shade}, 1,
						sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), light);
			}
		}
		pose.popPose();
	}

	private static Vector3f lump(float radius, int i, int j, int rings, int segments, long seed) {
		double polar = Math.PI * i / rings, azimuth = Math.PI * 2 * (j % segments) / segments;
		int key = i == 0 || i == rings ? i * 1000 : i * 1000 + j % segments;
		float r = radius * (0.82f + 0.32f * SpellRender.hash(seed * 131 + key));
		return v((float) (Math.sin(polar) * Math.cos(azimuth) * r), (float) (Math.cos(polar) * r), (float) (Math.sin(polar) * Math.sin(azimuth) * r));
	}

	private SpellModels() {}
}
