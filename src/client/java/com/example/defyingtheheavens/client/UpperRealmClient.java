package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModBlockEntities;
import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.ModDimensions;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Client setup for the Upper Realm and the Spatial Gap: custom sky renderers, dimension effects (fog colour, clouds),
 * fog distances (applied by client/mixin/FogRendererMixin), the Spatial Rift renderer and the blossom leaves' cutout layer.
 */
public final class UpperRealmClient {
	/** Fog start/end as fractions of the render distance: dense, but the nearer islands stay visible. */
	private static final float[] UPPER_REALM_FOG = {0.25f, 0.95f};
	/** Deep enough that SpatialGapAmbience's far motes glow out of the dark instead of vanishing into it. */
	private static final float[] SPATIAL_GAP_FOG = {0.1f, 1.0f};

	public static void register() {
		DimensionRenderingRegistry.registerDimensionEffects(ModDimensions.UPPER_REALM_EFFECTS, new UpperRealmEffects());
		DimensionRenderingRegistry.registerDimensionEffects(ModDimensions.SPATIAL_GAP_EFFECTS, new SpatialGapEffects());
		DimensionRenderingRegistry.registerSkyRenderer(ModDimensions.UPPER_REALM, new UpperRealmSkyRenderer());
		DimensionRenderingRegistry.registerSkyRenderer(ModDimensions.SPATIAL_GAP, new SpatialGapSkyRenderer());
		DimensionRenderingRegistry.registerCloudRenderer(ModDimensions.SPATIAL_GAP, context -> {}); // nothing but void

		BlockEntityRenderers.register(ModBlockEntities.SPATIAL_RIFT, context -> new SpatialRiftRenderer());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.WHITE_BLOSSOM_LEAVES, RenderType.cutoutMipped());
	}

	/** Fog start/end fractions for a dimension, or null to keep vanilla fog. */
	public static float[] fogFor(ResourceKey<Level> dimension) {
		if (ModDimensions.isUpperRealm(dimension)) return UPPER_REALM_FOG;
		if (ModDimensions.isSpatialGap(dimension)) return SPATIAL_GAP_FOG;
		return null;
	}

	/** Clouds drifting in the gap between the middle (Y 256) and high (Y 390) island tiers; fog tinted toward warm gold. */
	private static final class UpperRealmEffects extends DimensionSpecialEffects {
		UpperRealmEffects() {
			super(336.0f, true, SkyType.NORMAL, false, false);
		}

		@Override
		public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
			double light = brightness * 0.94 + 0.06;
			Vec3 lit = fogColor.multiply(light, light, brightness * 0.91 + 0.09);
			return lit.lerp(new Vec3(1.0, 0.88, 0.6).scale(light), 0.12);
		}

		@Override
		public boolean isFoggyAt(int x, int y) {
			return false; // fog density is set by FogRendererMixin instead of the nether-thick "foggy" mode
		}
	}

	/** No clouds, no ground, near-black fog. */
	private static final class SpatialGapEffects extends DimensionSpecialEffects {
		SpatialGapEffects() {
			super(Float.NaN, false, SkyType.NONE, false, false);
		}

		@Override
		public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
			return new Vec3(0.01, 0.005, 0.03);
		}

		@Override
		public boolean isFoggyAt(int x, int y) {
			return false;
		}
	}

	private UpperRealmClient() {}
}
