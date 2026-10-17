package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.AstralPlanets;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws the Astral Realm's planets and stars into a sky (AstralSky in space, AstralSurfaceSky from a planet's land), each where it
 * truly lies but brought in to {@link #SHELL} blocks from the eye and shrunk alike, so it looks just as it would at its true
 * distance yet stays inside the view (the far plane can be as near as 128 blocks). Drawn far to near; each planet clears the depth
 * buffer first, so its own sphere and rings hide each other rightly while nearer planets lie over farther ones.
 * <ul>
 *   <li>a planet: its painted face (PlanetTextures) on a sphere fine enough for its size on screen, a plain shaded sphere until the
 *   painting is ready; its air's glow round the rim and over the limb, lit on the sun's side, the whole rim lit when the sun is
 *   behind it; its rings, lit on either face and dark in its shadow;</li>
 *   <li>a planet too small to show its face: a dot of its colour, as lit as the eye sees it;</li>
 *   <li>a star: its face, and its corona.</li>
 * </ul>
 */
final class PlanetDraw {
	/** How far from the eye things are drawn, at most. */
	static final double SHELL = 80;
	/** A planet's colour as a dot, by kind. */
	private static final Map<AstralPlanets.Kind, Integer> DOT = new HashMap<>();

	static {
		int[] colours = {0x5C8AA0, 0x6A8C78, 0x3C6CC0, 0xD4B880, 0xC07A4C, 0xD8E6F0, 0x4C8A5A, 0x9C7CA0, 0x6A3828, 0x9A948C,
				0xC4AEE8, 0x5A7058, 0x9A9894, 0xD6A8C8, 0xA8A060, 0x5A7A70};
		AstralPlanets.Kind[] kinds = AstralPlanets.Kind.values();
		for (int i = 0; i < kinds.length; i++) DOT.put(kinds[i], colours[i]);
	}

	/**
	 * A planet seen from the eye: {@code offset} from the eye to its centre (true blocks), {@code sun} the way from it to its star,
	 * {@code pixels} its size on screen (diameter), {@code alpha} how much of it shows (1 in space; less by day from a planet's land).
	 * The current pose is the eye's rotation only.
	 */
	static void planet(Matrix4f pose, AstralPlanets.Planet planet, Vec3 offset, Vec3 sun, double pixels, float alpha) {
		double distance = offset.length();
		double scale = Math.min(1, SHELL / distance);
		Vec3 centre = offset.scale(scale);
		double radius = planet.radius() * scale;
		Vec3 view = offset.normalize();
		PlanetLooks.Look look = PlanetLooks.of(planet);

		RenderSystem.clear(256, Minecraft.ON_OSX); // its own depth
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(515);
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		ResourceLocation face = PlanetTextures.planet(planet, sun, PlanetTextures.widthFor(pixels), pixels);
		int segments = pixels < 40 ? 24 : pixels < 220 ? 48 : pixels < 700 ? 80 : 128;
		sphere(pose, centre, radius, segments, face, planet, sun, alpha);

		// The rings: behind the planet where its sphere hides them, over it where they pass in front.
		if (planet.rings()) {
			RenderSystem.depthMask(false);
			RenderSystem.disableCull();
			rings(pose, centre, radius, planet, look, sun, alpha);
			RenderSystem.enableCull();
		}
		// Its air, over everything of it.
		RenderSystem.depthMask(false);
		RenderSystem.disableDepthTest();
		if (look.atmosphereStrength() > 0 && pixels > 6) {
			double apparent = radius * distance / Math.sqrt(Math.max(1, distance * distance - planet.radius() * planet.radius()));
			halo(pose, centre, apparent, view, look, sun, alpha);
		}
		RenderSystem.defaultBlendFunc();
	}

	/** A planet too small to show its face: a soft dot of its colour, lit as much as the eye sees of its day side. */
	static void dot(BufferBuilder buffer, Matrix4f pose, AstralPlanets.Planet planet, Vec3 offset, Vec3 sun, double pixels, double pixelsPerRadian, float alpha) {
		double distance = offset.length();
		Vec3 dir = offset.scale(1 / distance);
		double phase = (1 - dir.dot(sun)) / 2; // the share of its lit face toward the eye
		int colour = DOT.getOrDefault(planet.kind(), 0x808080);
		float bright = (float) (0.15 + 0.85 * phase) * Math.min(1, (float) pixels) * alpha;
		double size = Math.max(pixels, 1.6) * 1.6 / pixelsPerRadian * SHELL;
		quad(buffer, pose, dir.scale(SHELL), size, PlanetLooks.r(colour) * bright, PlanetLooks.g(colour) * bright, PlanetLooks.b(colour) * bright, 1);
	}

	/**
	 * A star near enough to show its face: the face, and its corona; {@code own}, the star of the system the eye is in, with a wide
	 * glare too (a farther one's corona stays close about it). {@code offset} from the eye to its centre, true blocks.
	 */
	static void star(Matrix4f pose, AstralPlanets.Star star, Vec3 offset, double pixelsPerRadian, float alpha, boolean own) {
		double distance = offset.length();
		Vec3 dir = offset.scale(1 / distance);
		double angular = Math.max(star.radius() / distance, 2.5 / pixelsPerRadian);
		double shell = SHELL * 0.995;
		double face = 2 * Math.tan(Math.min(1.2, angular)) * shell; // its face's width
		float r = PlanetLooks.r(star.colour()), g = PlanetLooks.g(star.colour()), b = PlanetLooks.b(star.colour());
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		// Its corona (and, for one's own star, its glare), wide and soft; then its face, white-hot at the middle.
		RenderSystem.setShaderTexture(0, PlanetTextures.glow());
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		texturedQuad(buffer, pose, dir.scale(shell), Math.min(face * (own ? 3.2 : 2.2), shell * 2.5), r, g, b, (own ? 0.5f : 0.35f) * alpha);
		if (own) texturedQuad(buffer, pose, dir.scale(shell), Math.min(face * 7, shell * 4), r, g, b, 0.16f * alpha);
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.setShaderTexture(0, PlanetTextures.starFace());
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		texturedQuad(buffer, pose, dir.scale(shell), face, Math.min(1, r * 1.1f + 0.15f), Math.min(1, g * 1.1f + 0.15f), Math.min(1, b * 1.1f + 0.15f), alpha);
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.defaultBlendFunc();
	}

	// --- Shapes ---

	private static final Map<Integer, float[][]> TRIG = new HashMap<>();

	/** Sines and cosines round the sphere for {@code segments} longitudes: [cos lon, sin lon, cos lat, sin lat]. */
	private static float[][] trig(int segments) {
		return TRIG.computeIfAbsent(segments, n -> {
			float[][] t = new float[4][n + 1];
			for (int i = 0; i <= n; i++) {
				double lon = (i / (double) n * 2 - 1) * Math.PI;
				t[0][i] = (float) Math.cos(lon);
				t[1][i] = (float) Math.sin(lon);
			}
			int rows = n / 2;
			for (int j = 0; j <= rows; j++) {
				double lat = (j / (double) rows - 0.5) * Math.PI;
				t[2][j] = (float) Math.cos(lat);
				t[3][j] = (float) Math.sin(lat);
			}
			return t;
		});
	}

	/**
	 * The planet's sphere: its painted face (light painted in), or until that's ready a plain one in its colour, shaded by its sun
	 * here. Faces outward (culled from within).
	 */
	private static void sphere(Matrix4f pose, Vec3 centre, double radius, int segments, ResourceLocation face, AstralPlanets.Planet planet, Vec3 sun, float alpha) {
		float[][] t = trig(segments);
		int rows = segments / 2;
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		int colour = DOT.getOrDefault(planet.kind(), 0x808080);
		if (face != null) {
			RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
			RenderSystem.setShaderTexture(0, face);
			buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		} else {
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		}
		for (int j = 0; j < rows; j++) {
			for (int i = 0; i < segments; i++) {
				// Counter-clockwise from outside: up a latitude, along, back down.
				vertex(buffer, pose, centre, radius, t, i, j, segments, rows, face != null, colour, sun, alpha);
				vertex(buffer, pose, centre, radius, t, i, j + 1, segments, rows, face != null, colour, sun, alpha);
				vertex(buffer, pose, centre, radius, t, i + 1, j + 1, segments, rows, face != null, colour, sun, alpha);
				vertex(buffer, pose, centre, radius, t, i + 1, j, segments, rows, face != null, colour, sun, alpha);
			}
		}
		BufferUploader.drawWithShader(buffer.end());
	}

	private static void vertex(BufferBuilder buffer, Matrix4f pose, Vec3 centre, double radius, float[][] t, int i, int j, int segments, int rows,
			boolean textured, int colour, Vec3 sun, float alpha) {
		float cl = t[2][j], sl = t[3][j], co = t[0][i], so = t[1][i];
		float nx = cl * co, ny = sl, nz = cl * so;
		float x = (float) (centre.x + nx * radius), y = (float) (centre.y + ny * radius), z = (float) (centre.z + nz * radius);
		if (textured) {
			buffer.vertex(pose, x, y, z).uv(i / (float) segments, 1 - j / (float) rows).color(1f, 1f, 1f, alpha).endVertex();
		} else {
			float lit = 0.03f + Math.max(0, (float) (nx * sun.x + ny * sun.y + nz * sun.z));
			buffer.vertex(pose, x, y, z).color(PlanetLooks.r(colour) * lit, PlanetLooks.g(colour) * lit, PlanetLooks.b(colour) * lit, alpha).endVertex();
		}
	}

	/** The air's glow: a ring outside the rim fading out, and a haze inside it thickening to the limb; lit on the sun's side. */
	private static void halo(Matrix4f pose, Vec3 centre, double apparent, Vec3 view, PlanetLooks.Look look, Vec3 sun, float alpha) {
		Vec3 right = view.cross(new Vec3(0, 1, 0));
		if (right.lengthSqr() < 1e-6) right = view.cross(new Vec3(1, 0, 0));
		right = right.normalize();
		Vec3 up = right.cross(view).normalize();
		float r = PlanetLooks.r(look.atmosphere()), g = PlanetLooks.g(look.atmosphere()), b = PlanetLooks.b(look.atmosphere());
		double backlit = 0.8 * Math.pow(Math.max(0, view.dot(sun)), 5);
		double outer = apparent * (1 + look.atmosphereThickness() * 1.7), inner = apparent * 0.78;
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		RenderSystem.disableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		int n = 96;
		for (int i = 0; i < n; i++) {
			double a0 = i / (double) n * Math.PI * 2, a1 = (i + 1) / (double) n * Math.PI * 2;
			Vec3 d0 = right.scale(Math.cos(a0)).add(up.scale(Math.sin(a0))), d1 = right.scale(Math.cos(a1)).add(up.scale(Math.sin(a1)));
			float k0 = (float) ((smooth(-0.35, 0.45, d0.dot(sun)) + backlit) * look.atmosphereStrength() * alpha);
			float k1 = (float) ((smooth(-0.35, 0.45, d1.dot(sun)) + backlit) * look.atmosphereStrength() * alpha);
			// Outside the rim.
			colour(buffer, pose, centre.add(d0.scale(apparent * 0.995)), r, g, b, k0 * 0.85f);
			colour(buffer, pose, centre.add(d0.scale(outer)), r, g, b, 0);
			colour(buffer, pose, centre.add(d1.scale(outer)), r, g, b, 0);
			colour(buffer, pose, centre.add(d1.scale(apparent * 0.995)), r, g, b, k1 * 0.85f);
			// Over the limb, thickening toward it.
			colour(buffer, pose, centre.add(d0.scale(inner)), r, g, b, 0);
			colour(buffer, pose, centre.add(d0.scale(apparent * 0.995)), r, g, b, k0 * 0.55f);
			colour(buffer, pose, centre.add(d1.scale(apparent * 0.995)), r, g, b, k1 * 0.55f);
			colour(buffer, pose, centre.add(d1.scale(inner)), r, g, b, 0);
		}
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.enableCull();
	}

	/** The planet's rings, in their tilted plane: lit on either face, dark where the planet's shadow falls across them. */
	private static void rings(Matrix4f pose, Vec3 centre, double radius, AstralPlanets.Planet planet, PlanetLooks.Look look, Vec3 sun, float alpha) {
		Vec3 normal = new Vec3(Math.sin(look.ringTiltZ()), Math.cos(look.ringTiltX()) * Math.cos(look.ringTiltZ()), Math.sin(look.ringTiltX())).normalize();
		Vec3 a = normal.cross(new Vec3(1, 0, 0));
		if (a.lengthSqr() < 1e-6) a = normal.cross(new Vec3(0, 0, 1));
		a = a.normalize();
		Vec3 b = normal.cross(a).normalize();
		float light = (float) (0.3 + 0.7 * Math.abs(normal.dot(sun)));
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, PlanetTextures.rings(planet, look));
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		int n = 128;
		double in = look.ringInner(), out = look.ringOuter();
		for (int i = 0; i < n; i++) {
			double a0 = i / (double) n * Math.PI * 2, a1 = (i + 1) / (double) n * Math.PI * 2;
			Vec3 d0 = a.scale(Math.cos(a0)).add(b.scale(Math.sin(a0))), d1 = a.scale(Math.cos(a1)).add(b.scale(Math.sin(a1)));
			ringVertex(buffer, pose, centre, radius, d0, in, 0, light, sun, alpha);
			ringVertex(buffer, pose, centre, radius, d0, out, 1, light, sun, alpha);
			ringVertex(buffer, pose, centre, radius, d1, out, 1, light, sun, alpha);
			ringVertex(buffer, pose, centre, radius, d1, in, 0, light, sun, alpha);
		}
		BufferUploader.drawWithShader(buffer.end());
	}

	private static void ringVertex(BufferBuilder buffer, Matrix4f pose, Vec3 centre, double radius, Vec3 dir, double reach, float u, float light, Vec3 sun,
			float alpha) {
		Vec3 p = dir.scale(reach); // in the planet's radii
		double along = p.dot(sun);
		boolean shaded = along < 0 && p.subtract(sun.scale(along)).lengthSqr() < 1;
		float k = shaded ? light * 0.12f : light;
		Vec3 at = centre.add(p.scale(radius));
		buffer.vertex(pose, (float) at.x, (float) at.y, (float) at.z).uv(u, 0.5f).color(k, k, k, alpha).endVertex();
	}

	/** A camera-facing square of side {@code size} round {@code at}, plain colour (a soft dot: drawn with the glow texture). */
	static void quad(BufferBuilder buffer, Matrix4f pose, Vec3 at, double size, float r, float g, float b, float a) {
		texturedQuad(buffer, pose, at, size, r, g, b, a);
	}

	/** A camera-facing square of side {@code size} round {@code at}, the whole texture on it. */
	static void texturedQuad(BufferBuilder buffer, Matrix4f pose, Vec3 at, double size, float r, float g, float b, float a) {
		Vec3 view = at.normalize();
		Vec3 right = view.cross(new Vec3(0, 1, 0));
		if (right.lengthSqr() < 1e-6) right = view.cross(new Vec3(1, 0, 0));
		right = right.normalize().scale(size / 2);
		Vec3 up = right.cross(view).normalize().scale(size / 2);
		Vec3 p0 = at.subtract(right).subtract(up), p1 = at.add(right).subtract(up), p2 = at.add(right).add(up), p3 = at.subtract(right).add(up);
		buffer.vertex(pose, (float) p0.x, (float) p0.y, (float) p0.z).uv(0, 1).color(r, g, b, a).endVertex();
		buffer.vertex(pose, (float) p1.x, (float) p1.y, (float) p1.z).uv(1, 1).color(r, g, b, a).endVertex();
		buffer.vertex(pose, (float) p2.x, (float) p2.y, (float) p2.z).uv(1, 0).color(r, g, b, a).endVertex();
		buffer.vertex(pose, (float) p3.x, (float) p3.y, (float) p3.z).uv(0, 0).color(r, g, b, a).endVertex();
	}

	private static void colour(BufferBuilder buffer, Matrix4f pose, Vec3 at, float r, float g, float b, float a) {
		buffer.vertex(pose, (float) at.x, (float) at.y, (float) at.z).color(r, g, b, a).endVertex();
	}

	static double smooth(double from, double to, double x) {
		double t = Mth.clamp((x - from) / (to - from), 0, 1);
		return t * t * (3 - 2 * t);
	}

	private PlanetDraw() {}
}
