package com.example.defyingtheheavens.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Drawing helpers shared by the custom sky renderers: a gradient dome around the camera and a star field. */
final class SkyDome {
	private static final float RADIUS = 100.0f;
	private static final int STAR_COUNT = 900;
	private static final Vector3f[] STARS = new Vector3f[STAR_COUNT];
	private static final float[] STAR_SIZES = new float[STAR_COUNT];

	static {
		RandomSource random = RandomSource.create(10842L);
		for (int i = 0; i < STAR_COUNT; i++) {
			Vector3f dir;
			do {
				dir = new Vector3f(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1);
			} while (dir.lengthSquared() < 0.01f || dir.lengthSquared() > 1.0f);
			STARS[i] = dir.normalize();
			STAR_SIZES[i] = 0.15f + random.nextFloat() * 0.25f;
		}
	}

	/**
	 * Fills the sky with a vertical gradient: {@code horizon} at the horizon, blending to {@code zenith} over the first
	 * {@code bandDegrees} above it and to {@code nadir} below it. Colors are {r, g, b}.
	 */
	static void drawGradient(Matrix4f pose, float[] zenith, float[] horizon, float[] nadir, float bandDegrees) {
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableCull();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		for (int lat = -90; lat < 90; lat += 10) {
			float[] c0 = colorAt(lat, zenith, horizon, nadir, bandDegrees);
			float[] c1 = colorAt(lat + 10, zenith, horizon, nadir, bandDegrees);
			for (int lon = 0; lon < 360; lon += 15) {
				vertex(buffer, pose, lat, lon, c0);
				vertex(buffer, pose, lat, lon + 15, c0);
				vertex(buffer, pose, lat + 10, lon + 15, c1);
				vertex(buffer, pose, lat, lon, c0);
				vertex(buffer, pose, lat + 10, lon + 15, c1);
				vertex(buffer, pose, lat + 10, lon, c1);
			}
		}
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.enableCull();
	}

	/** Additive star points; {@code brightness} 0 draws nothing. */
	static void drawStars(Matrix4f pose, float brightness, float r, float g, float b, float twinkleTime) {
		if (brightness <= 0.01f) return;
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE,
				GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Vector3f up = new Vector3f(0, 1, 0);
		for (int i = 0; i < STAR_COUNT; i++) {
			Vector3f dir = STARS[i];
			float size = STAR_SIZES[i];
			Vector3f t1 = new Vector3f(dir).cross(Math.abs(dir.y) > 0.9f ? new Vector3f(1, 0, 0) : up).normalize(size);
			Vector3f t2 = new Vector3f(dir).cross(t1).normalize(size);
			Vector3f c = new Vector3f(dir).mul(RADIUS);
			float alpha = brightness * (0.6f + 0.4f * Mth.sin(twinkleTime * 0.05f + i * 1.3f));
			buffer.vertex(pose, c.x - t1.x - t2.x, c.y - t1.y - t2.y, c.z - t1.z - t2.z).color(r, g, b, alpha).endVertex();
			buffer.vertex(pose, c.x + t1.x - t2.x, c.y + t1.y - t2.y, c.z + t1.z - t2.z).color(r, g, b, alpha).endVertex();
			buffer.vertex(pose, c.x + t1.x + t2.x, c.y + t1.y + t2.y, c.z + t1.z + t2.z).color(r, g, b, alpha).endVertex();
			buffer.vertex(pose, c.x - t1.x + t2.x, c.y - t1.y + t2.y, c.z - t1.z + t2.z).color(r, g, b, alpha).endVertex();
		}
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.defaultBlendFunc();
	}

	/** Soft additive disc around (0, RADIUS, 0) in the current pose; used for the golden aura around the sun. */
	static void drawHalo(Matrix4f pose, float radius, float r, float g, float b, float alpha) {
		if (alpha <= 0.01f) return;
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE,
				GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		RenderSystem.disableCull();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		int segments = 32;
		for (int i = 0; i < segments; i++) {
			float a0 = Mth.TWO_PI * i / segments;
			float a1 = Mth.TWO_PI * (i + 1) / segments;
			buffer.vertex(pose, 0, RADIUS, 0).color(r, g, b, alpha).endVertex();
			buffer.vertex(pose, Mth.cos(a0) * radius, RADIUS, Mth.sin(a0) * radius).color(r, g, b, 0.0f).endVertex();
			buffer.vertex(pose, Mth.cos(a1) * radius, RADIUS, Mth.sin(a1) * radius).color(r, g, b, 0.0f).endVertex();
		}
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();
	}

	private static float[] colorAt(float latitude, float[] zenith, float[] horizon, float[] nadir, float bandDegrees) {
		if (latitude >= 0) {
			return lerp(horizon, zenith, smooth(latitude / bandDegrees));
		}
		return lerp(horizon, nadir, smooth(-latitude / 35.0f));
	}

	private static float smooth(float t) {
		t = Mth.clamp(t, 0.0f, 1.0f);
		return t * t * (3 - 2 * t);
	}

	static float[] lerp(float[] a, float[] b, float t) {
		return new float[] {Mth.lerp(t, a[0], b[0]), Mth.lerp(t, a[1], b[1]), Mth.lerp(t, a[2], b[2])};
	}

	private static void vertex(BufferBuilder buffer, Matrix4f pose, float latDeg, float lonDeg, float[] color) {
		float lat = latDeg * Mth.DEG_TO_RAD;
		float lon = lonDeg * Mth.DEG_TO_RAD;
		float x = Mth.cos(lat) * Mth.cos(lon) * RADIUS;
		float y = Mth.sin(lat) * RADIUS;
		float z = Mth.cos(lat) * Mth.sin(lon) * RADIUS;
		buffer.vertex(pose, x, y, z).color(color[0], color[1], color[2], 1.0f).endVertex();
	}

	private SkyDome() {}
}
