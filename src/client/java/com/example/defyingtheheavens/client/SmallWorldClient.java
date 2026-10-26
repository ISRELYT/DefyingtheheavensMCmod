package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.ModBlockEntities;
import com.example.defyingtheheavens.ModDimensions;
import com.example.defyingtheheavens.ModEntities;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.phys.Vec3;

/**
 * The small worlds' look (phase 3d, SmallWorlds): each island under a sky that is all stars, black above, deep indigo at the horizon,
 * the void's nebulae and stars (AstralSky#backdrop, a seed of their own) turning very slowly; no clouds, sun or moon; the fog far off and
 * indigo. Their doors (SmallWorldDoorRenderer) and gates (SmallWorldGateRenderer): the void's stars seen through them.
 */
public final class SmallWorldClient {
	private static final long SEED = 0x5A11_3071_D5EEDL;
	/** Fog start and end, as fractions of the render distance: only the far edge of a great island fades. */
	static final float[] FOG = {0.6f, 1.0f};
	private static final Vec3 HAZE = new Vec3(0.07, 0.08, 0.2);

	public static void register() {
		DimensionRenderingRegistry.registerDimensionEffects(ModDimensions.SMALL_WORLD_EFFECTS, new Effects());
		DimensionRenderingRegistry.registerSkyRenderer(ModDimensions.SMALL_WORLD, SmallWorldClient::sky);
		DimensionRenderingRegistry.registerCloudRenderer(ModDimensions.SMALL_WORLD, context -> {});
		EntityRendererRegistry.register(ModEntities.SMALL_WORLD_DOOR, SmallWorldDoorRenderer::new);
		BlockEntityRenderers.register(ModBlockEntities.SMALL_WORLD_GATE, context -> new SmallWorldGateRenderer());
	}

	/** Black above, indigo at the horizon, the stars and nebulae turning (a full turn in some ten hours). */
	private static void sky(WorldRenderContext context) {
		ClientLevel level = context.world();
		if (level == null) return;
		try {
			PlanetTextures.frame();
			PoseStack poseStack = context.matrixStack();
			float time = level.getGameTime() + context.tickDelta();
			RenderSystem.depthMask(false);
			RenderSystem.disableDepthTest();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			SkyDome.drawGradient(poseStack.last().pose(), new float[] {0.008f, 0.008f, 0.03f}, new float[] {0.09f, 0.1f, 0.25f}, new float[] {0.03f, 0.03f, 0.1f}, 50f);
			poseStack.pushPose();
			poseStack.mulPose(Axis.YP.rotationDegrees(time * 0.0005f));
			AstralSky.backdrop(poseStack.last().pose(), SEED, time, 1.0f);
			poseStack.popPose();
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Small world sky rendering failed", e);
		} finally {
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableBlend();
			RenderSystem.enableCull();
			RenderSystem.enableDepthTest();
			RenderSystem.depthMask(true);
		}
	}

	/** No clouds and no sun or moon: the sky is the renderer's. The fog leans to its indigo. */
	private static final class Effects extends DimensionSpecialEffects {
		Effects() {
			super(Float.NaN, false, SkyType.NONE, false, false);
		}

		@Override
		public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
			return fogColor.lerp(HAZE, 0.6);
		}

		@Override
		public boolean isFoggyAt(int x, int y) {
			return false;
		}
	}

	private SmallWorldClient() {}
}
