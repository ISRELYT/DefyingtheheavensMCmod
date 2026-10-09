package com.example.defyingtheheavens.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The blocky, churning storm cloud that {@link ClientSpatialStorms} draws: a dark mass of cloud cells whose core is
 * darkest and thickest and dips down like a funnel, the pattern swirling (faster toward the middle) while the cells stay
 * square to the world like vanilla clouds, lit from within when lightning flashes. Callers draw it twice, depth only and
 * then colour, so it is translucent without showing its own inner faces.
 */
final class StormCloudMesh {
	/** Cloud cells: horizontal size and layer height in blocks, before any scaling by the caller. */
	private static final float CELL = 6;
	private static final float LAYER = 3;
	/** Layers below the cloud's base (the funnel under the core) and above it. */
	private static final int LOW_LAYER = -1;
	private static final int HIGH_LAYER = 3;
	/** Radians per tick the pattern turns at the rim; the core turns 2.5x as fast. */
	private static final float SWIRL = 0.004f;

	/** One frame's cloud shape: per column, the lowest and highest layer, or no cloud. */
	static final class Shape {
		final int n;
		final int[] low;
		final int[] high;
		final float[] core; // 1 at the centre, 0 at the rim
		final float[] mottle; // per-column light/dark variation, -1..1

		Shape(int n) {
			this.n = n;
			int size = (2 * n + 1) * (2 * n + 1);
			low = new int[size];
			high = new int[size];
			core = new float[size];
			mottle = new float[size];
		}

		int index(int i, int k) { return (i + n) * (2 * n + 1) + (k + n); }

		boolean filled(int i, int k, int layer) {
			if (i < -n || i > n || k < -n || k > n) return false;
			int idx = index(i, k);
			return high[idx] >= low[idx] && layer >= low[idx] && layer <= high[idx];
		}
	}

	/** Which cells are cloud this frame: a swirling noise pattern, always filled near the core and ragged at the rim. */
	static Shape shape(int seed, float radius, float time) {
		int n = Mth.ceil(radius / CELL);
		Shape shape = new Shape(n);
		for (int i = -n; i <= n; i++) {
			for (int k = -n; k <= n; k++) {
				int idx = shape.index(i, k);
				shape.high[idx] = Integer.MIN_VALUE;
				float cx = i * CELL;
				float cz = k * CELL;
				float r = Mth.sqrt(cx * cx + cz * cz);
				if (r > radius) continue;
				float core = 1 - r / radius;
				// Sample the noise in a frame that turns with time, faster toward the core: the pattern swirls while the
				// cells stay square to the world like vanilla clouds.
				float angle = time * SWIRL * (1 + 1.5f * core);
				float sin = Mth.sin(angle);
				float cos = Mth.cos(angle);
				float sx = (cx * cos - cz * sin) / CELL;
				float sz = (cx * sin + cz * cos) / CELL;
				float density = fbm(sx * 0.35f, sz * 0.35f, seed);
				if (core < 0.75f && density < 0.62f - 0.7f * core) continue;
				shape.core[idx] = core;
				// The underside hangs in lumps, more and deeper toward the funnel at the core.
				float lump = fbm(sx * 0.5f - 7.9f, sz * 0.5f + 23.1f, seed);
				int low = core > 0.82f ? LOW_LAYER : core > 0.5f ? 0 : 1;
				if (core > 0.15f && lump > 0.58f) low = Math.min(low, 0);
				if (core > 0.4f && lump > 0.72f) low = LOW_LAYER;
				shape.low[idx] = low;
				int high = 1 + (core > 0.25f ? 1 : 0) + (fbm(sx * 0.6f + 31.7f, sz * 0.6f - 12.3f, seed) > 0.62f ? 1 : 0);
				shape.high[idx] = Math.min(HIGH_LAYER, high);
				shape.mottle[idx] = noise(sx * 0.9f + 50.3f, sz * 0.9f - 3.7f, seed + 7) * 2 - 1;
			}
		}
		return shape;
	}

	/**
	 * Every visible face of the cloud's cells: dark grey (times {@code tint}), darker toward the core, bottoms darkest, lit
	 * from within by {@code litColor} {r, g, b} in a flash.
	 */
	static void draw(Matrix4f matrix, Shape shape, float strength, float flash, Vec3 tint, float[] litColor) {
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		int n = shape.n;
		float half = CELL / 2;
		for (int i = -n; i <= n; i++) {
			for (int k = -n; k <= n; k++) {
				int idx = shape.index(i, k);
				if (shape.high[idx] < shape.low[idx]) continue;
				float core = shape.core[idx];
				// Nearly opaque, so the sky behind doesn't tint it: dark grey at the rim to near-black at the core.
				float grey = Mth.lerp(core, 0.22f, 0.055f) * (1 + 0.18f * shape.mottle[idx]);
				float alpha = (0.88f + 0.1f * core) * strength;
				float lit = flash * (0.45f + 0.55f * core);
				float x0 = i * CELL - half, x1 = x0 + CELL;
				float z0 = k * CELL - half, z1 = z0 + CELL;
				for (int layer = shape.low[idx]; layer <= shape.high[idx]; layer++) {
					float y0 = layer * LAYER, y1 = y0 + LAYER;
					if (!shape.filled(i, k, layer + 1)) {
						quad(buffer, matrix, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, grey * 1.15f, lit * 0.3f, alpha, tint, litColor);
					}
					if (!shape.filled(i, k, layer - 1)) {
						quad(buffer, matrix, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0, grey * 0.7f, lit, alpha, tint, litColor);
					}
					if (!shape.filled(i - 1, k, layer)) {
						quad(buffer, matrix, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, grey * 0.9f, lit * 0.7f, alpha, tint, litColor);
					}
					if (!shape.filled(i + 1, k, layer)) {
						quad(buffer, matrix, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, grey * 0.9f, lit * 0.7f, alpha, tint, litColor);
					}
					if (!shape.filled(i, k - 1, layer)) {
						quad(buffer, matrix, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, grey * 0.82f, lit * 0.7f, alpha, tint, litColor);
					}
					if (!shape.filled(i, k + 1, layer)) {
						quad(buffer, matrix, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1, grey * 0.82f, lit * 0.7f, alpha, tint, litColor);
					}
				}
			}
		}
		BufferUploader.drawWithShader(buffer.end());
	}

	private static void quad(BufferBuilder buffer, Matrix4f matrix, float ax, float ay, float az, float bx, float by, float bz,
							 float cx, float cy, float cz, float dx, float dy, float dz, float grey, float lit, float alpha, Vec3 tint,
							 float[] litColor) {
		// The tint dims the grey; the lightning lights the cloud from within.
		float r = grey * (float) tint.x + lit * litColor[0];
		float g = grey * (float) tint.y + lit * litColor[1];
		float b = grey * (float) tint.z + lit * litColor[2];
		buffer.vertex(matrix, ax, ay, az).color(r, g, b, alpha).endVertex();
		buffer.vertex(matrix, bx, by, bz).color(r, g, b, alpha).endVertex();
		buffer.vertex(matrix, cx, cy, cz).color(r, g, b, alpha).endVertex();
		buffer.vertex(matrix, dx, dy, dz).color(r, g, b, alpha).endVertex();
	}

	/** Two octaves of value noise, 0..1. */
	private static float fbm(float x, float z, int seed) {
		return 0.65f * noise(x, z, seed) + 0.35f * noise(x * 2.1f + 5.2f, z * 2.1f + 1.3f, seed + 101);
	}

	private static float noise(float x, float z, int seed) {
		int x0 = Mth.floor(x);
		int z0 = Mth.floor(z);
		float fx = x - x0;
		float fz = z - z0;
		fx = fx * fx * (3 - 2 * fx);
		fz = fz * fz * (3 - 2 * fz);
		float top = Mth.lerp(fx, hash(x0, z0, seed), hash(x0 + 1, z0, seed));
		float bottom = Mth.lerp(fx, hash(x0, z0 + 1, seed), hash(x0 + 1, z0 + 1, seed));
		return Mth.lerp(fz, top, bottom);
	}

	private static float hash(int x, int z, int seed) {
		int h = x * 374761393 + z * 668265263 + seed * 1442695041;
		h = (h ^ (h >>> 13)) * 1274126177;
		return ((h ^ (h >>> 16)) & 0xFFFFFF) / (float) 0xFFFFFF;
	}

	private StormCloudMesh() {}
}
