package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.AstralPlanets;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.ModDimensions;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * The Astral Realm on the client: space's sky (AstralSky) and its near-black, all but fogless void; the planets' lands with their
 * own skies (the planet's star rising and setting by the day as the Overworld's sun does, its sister planets and moons where they
 * truly hang, the stars by night, all day where there is no air), their own clouds only where there is air.
 */
public final class AstralClient {
	/** Space and an airless world: next to no fog (there is no air to hold it). */
	private static final float[] CLEAR = {0.97f, 1.0f};

	public static void register() {
		DimensionRenderingRegistry.registerDimensionEffects(ModDimensions.ASTRAL_VOID_EFFECTS, new VoidEffects());
		DimensionRenderingRegistry.registerSkyRenderer(ModDimensions.ASTRAL_VOID, new AstralSky());
		DimensionRenderingRegistry.registerCloudRenderer(ModDimensions.ASTRAL_VOID, context -> {});
		DimensionRenderingRegistry.registerDimensionEffects(ModDimensions.ASTRAL_SURFACES_EFFECTS, new SurfaceEffects());
		DimensionRenderingRegistry.registerSkyRenderer(ModDimensions.ASTRAL_SURFACES, AstralClient::surfaceSky);
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(com.example.defyingtheheavens.ModEntities.VOID_VESTIGE, VestigeRenderer::new);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(PlanetTextures::clear));
	}

	/** Fog start/end as fractions of the render distance for an Astral Realm dimension, or null for vanilla's. */
	public static float[] fogFor(ResourceKey<Level> dimension) {
		if (ModDimensions.isSpace(dimension)) return CLEAR;
		if (ModDimensions.isPlanetSurface(dimension)) {
			AstralPlanets.Planet here = here();
			return here == null || airless(here) ? CLEAR : null;
		}
		return null;
	}

	/** The planet whose land the eye is over, or null. */
	@Nullable
	static AstralPlanets.Planet here() {
		AstralPlanets planets = AstralPlanets.get();
		Minecraft mc = Minecraft.getInstance();
		if (planets == null || mc.gameRenderer == null || mc.gameRenderer.getMainCamera() == null) return null;
		Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
		return planets.planetAt(eye.x, eye.z);
	}

	static boolean airless(AstralPlanets.Planet planet) {
		return PlanetLooks.of(planet).atmosphereStrength() <= 0;
	}

	/** A planet's sky: its air's gradient (black where it has none), then the Astral Realm turned to the planet's day. */
	private static void surfaceSky(WorldRenderContext context) {
		ClientLevel level = context.world();
		if (level == null || context.camera() == null) return;
		AstralPlanets planets = AstralPlanets.get();
		AstralPlanets.Planet here = here();
		try {
			PlanetTextures.frame();
			if (planets == null || here == null) return;
			float partialTick = context.tickDelta();
			Vec3 eye = context.camera().getPosition();
			PoseStack poseStack = context.matrixStack();
			boolean airless = airless(here);
			float timeOfDay = level.getTimeOfDay(partialTick);
			float day = Mth.clamp(Mth.cos(timeOfDay * Mth.TWO_PI) * 2.0f + 0.5f, 0.0f, 1.0f);
			RenderSystem.depthMask(false);
			RenderSystem.disableDepthTest();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			if (!airless) {
				Vec3 skyColour = level.getSkyColor(eye, partialTick);
				int air = PlanetLooks.of(here).atmosphere();
				float[] sky = {(float) skyColour.x, (float) skyColour.y, (float) skyColour.z};
				float[] horizon = SkyDome.lerp(sky, new float[] {PlanetLooks.r(air) * day, PlanetLooks.g(air) * day, PlanetLooks.b(air) * day}, 0.35f);
				SkyDome.drawGradient(poseStack.last().pose(), sky, horizon, new float[] {sky[0] * 0.8f, sky[1] * 0.8f, sky[2] * 0.85f}, 38.0f);
			}

			// The sky turns with the day as the Overworld's does; the planet's star stands where vanilla's sun would.
			poseStack.pushPose();
			poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f));
			poseStack.mulPose(Axis.XP.rotationDegrees(timeOfDay * 360.0f));
			AstralPlanets.Star star = planets.star(here.system());
			Vec3 sun = star == null ? new Vec3(0, 1, 0) : star.pos().subtract(here.pos()).normalize();
			poseStack.last().pose().mul(toSky(sun));
			Matrix4f celestial = poseStack.last().pose();
			float rain = 1 - level.getRainLevel(partialTick);
			float night = airless ? 1 : level.getStarBrightness(partialTick) * 2 * rain;
			AstralSky.backdrop(celestial, planets.seed(), level.getGameTime() + partialTick, Math.min(1, night));
			// Seen from just above its ground, toward the sky: as from a point on the sphere over the land one stands on.
			Vec3 over = here.pos().add(sun.scale(here.radius() * 1.02));
			AstralSky.universe(celestial, planets, over, AstralSky.pixelsPerRadian(), Math.min(1, 0.35f + night), Math.min(1, night), here.system(), here, 0.22);
			poseStack.popPose();
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Planet sky rendering failed", e);
		} finally {
			AstralSky.restore();
		}
	}

	/** Turns space's axes so {@code sun} lies along +Y (where the day puts the sun), the rest keeping their places about it. */
	private static Matrix4f toSky(Vec3 sun) {
		Vec3 ref = Math.abs(sun.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
		Vec3 x = ref.cross(sun).normalize();
		Vec3 z = x.cross(sun).normalize();
		// The rotation whose rows are the sky's axes (x, sun, z) in space: it takes the sun to +Y. JOML takes it column by column.
		return new Matrix4f(
				(float) x.x, (float) sun.x, (float) z.x, 0,
				(float) x.y, (float) sun.y, (float) z.y, 0,
				(float) x.z, (float) sun.z, (float) z.z, 0,
				0, 0, 0, 1);
	}

	/** Space: no ground, no clouds, near-black fog. */
	private static final class VoidEffects extends DimensionSpecialEffects {
		VoidEffects() {
			super(Float.NaN, false, SkyType.NONE, false, false);
		}

		@Override
		public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
			return new Vec3(0.004, 0.004, 0.012);
		}

		@Override
		public boolean isFoggyAt(int x, int y) {
			return false;
		}
	}

	/** A planet's land: the Overworld's ways, but clouds and sunrises only where there is air. */
	private static final class SurfaceEffects extends DimensionSpecialEffects {
		SurfaceEffects() {
			super(192.0f, true, SkyType.NORMAL, false, false);
		}

		@Override
		public float getCloudHeight() {
			AstralPlanets.Planet here = here();
			return here == null || airless(here) || PlanetLooks.of(here).cloudCover() < 0.12 ? Float.NaN : 192.0f;
		}

		@Override
		public float[] getSunriseColor(float timeOfDay, float partialTicks) {
			AstralPlanets.Planet here = here();
			return here == null || airless(here) ? null : super.getSunriseColor(timeOfDay, partialTicks);
		}

		@Override
		public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
			AstralPlanets.Planet here = here();
			if (here == null || airless(here)) return new Vec3(0.004, 0.004, 0.01);
			return fogColor.multiply(brightness * 0.94 + 0.06, brightness * 0.94 + 0.06, brightness * 0.91 + 0.09);
		}

		@Override
		public boolean isFoggyAt(int x, int y) {
			return false;
		}
	}

	private AstralClient() {}
}
