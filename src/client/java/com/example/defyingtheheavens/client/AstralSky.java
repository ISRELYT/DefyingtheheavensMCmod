package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.AstralPlanets;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Space's sky (ModDimensions#ASTRAL_VOID): everything there is to see in the Astral Realm, from where the eye truly is.
 * <ul>
 *   <li>behind it all, the nebulae and the galactic band (PlanetPainter#nebula), and a field of stars too far to reach,
 *   crowding along the band, white, blue, gold and red, faintly twinkling;</li>
 *   <li>every star system's sun (AstralPlanets.Star): a point of its colour, brighter the nearer and larger; near enough, its face
 *   and corona;</li>
 *   <li>every planet: a lit dot far off, its painted face, air and rings once it shows (PlanetDraw), nearest drawn last.</li>
 * </ul>
 * From a planet's land (AstralSurfaceSky) the same is drawn turned to that planet's sky.
 */
final class AstralSky implements DimensionRenderingRegistry.SkyRenderer {
	private static final int FIELD = 6500;
	private static long fieldSeed = Long.MIN_VALUE;
	private static float[] field;

	@Override
	public void render(WorldRenderContext context) {
		if (context.world() == null || context.camera() == null) return;
		AstralPlanets planets = AstralPlanets.get();
		try {
			PlanetTextures.frame();
			Matrix4f pose = context.matrixStack().last().pose();
			Vec3 eye = context.camera().getPosition();
			float time = context.world().getGameTime() + context.tickDelta();
			RenderSystem.depthMask(false);
			RenderSystem.disableDepthTest();
			RenderSystem.enableBlend();
			if (planets != null) {
				backdrop(pose, planets.seed(), time, 1f);
				universe(pose, planets, eye, pixelsPerRadian(), 1f, 1f, planets.nearestStar(eye).index(), null, Double.MAX_VALUE);
			}
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Astral sky rendering failed", e);
		} finally {
			restore();
		}
	}

	/** Back to how the level's own drawing expects things; the depth the sky used is cleared for the world. */
	static void restore() {
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		RenderSystem.enableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(515);
		RenderSystem.depthMask(true);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.clear(256, Minecraft.ON_OSX);
	}

	/** Screen pixels to a radian of view, near the middle. */
	static double pixelsPerRadian() {
		Minecraft mc = Minecraft.getInstance();
		double fov = Math.toRadians(mc.options.fov().get());
		return mc.getWindow().getHeight() / (2 * Math.tan(fov / 2));
	}

	/** The nebulae and the far stars, {@code alpha} of their full brightness. */
	static void backdrop(Matrix4f pose, long seed, float time, float alpha) {
		if (alpha <= 0.01f) return;
		ResourceLocation nebula = PlanetTextures.nebula(seed);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		RenderSystem.disableCull();
		if (nebula != null) {
			RenderSystem.defaultBlendFunc();
			RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
			RenderSystem.setShaderTexture(0, nebula);
			buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			int lon = 64, lat = 32;
			for (int j = 0; j < lat; j++) {
				for (int i = 0; i < lon; i++) {
					skyVertex(buffer, pose, i, j, lon, lat, alpha);
					skyVertex(buffer, pose, i, j + 1, lon, lat, alpha);
					skyVertex(buffer, pose, i + 1, j + 1, lon, lat, alpha);
					skyVertex(buffer, pose, i + 1, j, lon, lat, alpha);
				}
			}
			BufferUploader.drawWithShader(buffer.end());
		}
		float[] stars = field(seed);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, PlanetTextures.glow());
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		for (int i = 0; i < FIELD; i++) {
			int o = i * 8;
			float twinkle = 0.82f + 0.18f * Mth.sin(time * 0.05f + stars[o + 7]);
			float k = twinkle * alpha;
			PlanetDraw.texturedQuad(buffer, pose, new Vec3(stars[o], stars[o + 1], stars[o + 2]).scale(95), stars[o + 3], stars[o + 4] * k,
					stars[o + 5] * k, stars[o + 6] * k, 1);
		}
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableCull();
	}

	private static void skyVertex(BufferBuilder buffer, Matrix4f pose, int i, int j, int lon, int lat, float alpha) {
		double a = (i / (double) lon * 2 - 1) * Math.PI, b = (j / (double) lat - 0.5) * Math.PI;
		float x = (float) (Math.cos(b) * Math.cos(a) * 100), y = (float) (Math.sin(b) * 100), z = (float) (Math.cos(b) * Math.sin(a) * 100);
		buffer.vertex(pose, x, y, z).uv(i / (float) lon, 1 - j / (float) lat).color(1f, 1f, 1f, alpha).endVertex();
	}

	/** The far star field for {@code seed}: per star its direction, size, colour and twinkle phase (8 floats). */
	private static float[] field(long seed) {
		if (field != null && fieldSeed == seed) return field;
		RandomSource r = RandomSource.create(seed ^ 0x57A4F1E1DL);
		Vec3 pole = PlanetPainter.galacticPole(seed);
		Vec3 a = pole.cross(new Vec3(1, 0, 0)).normalize(), b = pole.cross(a).normalize();
		float[] out = new float[FIELD * 8];
		for (int i = 0; i < FIELD; i++) {
			Vec3 dir;
			if (r.nextFloat() < 0.45f) {
				// Crowding the galaxy's band.
				double t = r.nextDouble() * Math.PI * 2;
				dir = a.scale(Math.cos(t)).add(b.scale(Math.sin(t))).add(pole.scale(r.nextGaussian() * 0.12)).normalize();
			} else {
				double z = r.nextDouble() * 2 - 1, t = r.nextDouble() * Math.PI * 2, s = Math.sqrt(1 - z * z);
				dir = new Vec3(s * Math.cos(t), z, s * Math.sin(t));
			}
			double bright = Math.pow(r.nextDouble(), 3.2);
			float size = (float) (0.16 + bright * 0.75 + (r.nextFloat() < 0.012f ? 0.9 : 0));
			float k = (float) (0.35 + 0.65 * Math.min(1, bright * 2 + 0.2));
			float[] colour = temperature(r.nextFloat());
			int o = i * 8;
			out[o] = (float) dir.x;
			out[o + 1] = (float) dir.y;
			out[o + 2] = (float) dir.z;
			out[o + 3] = size;
			out[o + 4] = colour[0] * k;
			out[o + 5] = colour[1] * k;
			out[o + 6] = colour[2] * k;
			out[o + 7] = r.nextFloat() * 100;
		}
		field = out;
		fieldSeed = seed;
		return out;
	}

	/** A star's colour by its heat: most white or gold, some blue-white, orange or red. */
	private static float[] temperature(float t) {
		if (t < 0.12f) return new float[] {0.68f, 0.78f, 1f};
		if (t < 0.5f) return new float[] {1f, 0.98f, 0.95f};
		if (t < 0.76f) return new float[] {1f, 0.92f, 0.74f};
		if (t < 0.92f) return new float[] {1f, 0.78f, 0.55f};
		return new float[] {1f, 0.6f, 0.48f};
	}

	private record Near(double distance, Runnable draw) {}

	/** Beyond this, another system's star shows as a point of light, however large it is: its face and corona are for its own system. */
	private static final double FACE_REACH = 9000;
	/** Another system's planets fade in from this far (fully seen at half of it). */
	private static final double OTHER_PLANETS = 12000;

	/**
	 * Every star and planet as seen from {@code eye} (in space's own coordinates, under the current pose's rotation), but
	 * {@code skip} (the planet one stands on). The star and planets of system {@code own} show at {@code alpha}, everything beyond
	 * at {@code farAlpha} (from a planet's land by day: its own sun and sister planets, but not the far stars); nothing shows
	 * larger than {@code maxAngular} across (radians, half-width: a planet's land keeps a near star from filling its sky).
	 */
	static void universe(Matrix4f pose, AstralPlanets planets, Vec3 eye, double ppr, float alpha, float farAlpha, int own, AstralPlanets.Planet skip,
			double maxAngular) {
		List<Near> near = new ArrayList<>();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, PlanetTextures.glow());
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		for (AstralPlanets.Star star : planets.stars()) {
			boolean mine = star.index() == own;
			float a = mine ? 1f : farAlpha;
			if (a <= 0.01f) continue;
			Vec3 offset = clampSize(star.pos().subtract(eye), star.radius(), maxAngular);
			double distance = offset.length();
			if (mine || distance < FACE_REACH) {
				near.add(new Near(distance, () -> PlanetDraw.star(pose, star, offset, ppr, a, mine)));
			} else {
				// A point of light, brighter the nearer and the larger.
				float bright = (float) Mth.clamp(star.radius() / distance * 40, 0.05, 1) * a;
				double size = 0.3 + 1.0 * bright;
				Vec3 at = offset.scale(PlanetDraw.SHELL / distance);
				PlanetDraw.texturedQuad(buffer, pose, at, size, PlanetLooks.r(star.colour()) * bright, PlanetLooks.g(star.colour()) * bright,
						PlanetLooks.b(star.colour()) * bright, 1);
				PlanetDraw.texturedQuad(buffer, pose, at, size * 3.5, PlanetLooks.r(star.colour()) * bright * 0.25f,
						PlanetLooks.g(star.colour()) * bright * 0.25f, PlanetLooks.b(star.colour()) * bright * 0.25f, 1);
			}
		}
		List<AstralPlanets.Planet> all = new ArrayList<>(planets.planets().size() + 1);
		all.add(planets.overworld());
		all.addAll(planets.planets());
		for (AstralPlanets.Planet planet : all) {
			if (planet == skip) continue;
			Vec3 raw = planet.pos().subtract(eye);
			double rawDistance = raw.length();
			// Another system's planets are lost in its star's light from afar: they show only as one comes near them.
			float a = planet.system() == own ? alpha : farAlpha * (float) Mth.clamp((OTHER_PLANETS - rawDistance) / (OTHER_PLANETS / 2), 0, 1);
			if (a <= 0.01f) continue;
			if (planet.radius() / rawDistance * ppr * 2 < 0.35) continue;
			Vec3 offset = clampSize(raw, planet.radius(), maxAngular);
			double distance = offset.length();
			double pixels = planet.radius() / distance * ppr * 2;
			AstralPlanets.Star star = planets.star(planet.system());
			Vec3 sun = star == null ? new Vec3(0, 1, 0) : star.pos().subtract(planet.pos()).normalize();
			if (pixels < 3) {
				PlanetDraw.dot(buffer, pose, planet, offset, sun, pixels, ppr, a);
			} else {
				near.add(new Near(distance, () -> PlanetDraw.planet(pose, planet, offset, sun, pixels, a)));
			}
		}
		BufferUploader.drawWithShader(buffer.end());
		near.sort(Comparator.comparingDouble(Near::distance).reversed());
		for (Near n : near) n.draw.run();
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
	}

	/** {@code offset} pushed farther along its way, if need be, so a body {@code radius} across shows no larger than {@code maxAngular}. */
	private static Vec3 clampSize(Vec3 offset, double radius, double maxAngular) {
		if (maxAngular >= Math.PI) return offset;
		double distance = offset.length();
		double need = radius / Math.tan(maxAngular);
		return distance >= need ? offset : offset.scale(need / distance);
	}
}
