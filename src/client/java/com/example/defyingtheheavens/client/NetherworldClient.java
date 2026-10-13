package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModBlockEntities;
import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.ModDimensions;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.phys.Vec3;

/**
 * Client setup for the Netherworld and the Soul River (registered beside the Upper Realm's, from DefyingTheHeavensClient): their
 * dimension effects (no clouds, a dark teal fog), the Netherworld's sky ({@link NetherworldSkyRenderer}), the Wheel of
 * Reincarnation's renderer and the spider lily's cutout; the river journey ({@link ClientSoulRiver}, {@link SoulRiverRenderer})
 * and the Netherworld's HUD ({@link NetherworldHud}). The fog distances are UpperRealmClient#fogFor's.
 */
public final class NetherworldClient {
	public static void register() {
		DimensionRenderingRegistry.registerDimensionEffects(ModDimensions.NETHERWORLD_EFFECTS, new NetherworldEffects());
		DimensionRenderingRegistry.registerSkyRenderer(ModDimensions.NETHERWORLD, new NetherworldSkyRenderer());
		DimensionRenderingRegistry.registerCloudRenderer(ModDimensions.NETHERWORLD, context -> {}); // no clouds over the dead
		DimensionRenderingRegistry.registerDimensionEffects(ModDimensions.SOUL_RIVER_EFFECTS, new SoulRiverEffects());
		DimensionRenderingRegistry.registerCloudRenderer(ModDimensions.SOUL_RIVER, context -> {});
		BlockEntityRenderers.register(ModBlockEntities.WHEEL_OF_REINCARNATION, WheelOfReincarnationRenderer::new);
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RED_SPIDER_LILY, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.OTHER_SHORE_FLOWER, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.NETHERWORLD_LOTUS, RenderType.cutout());
		// Its dead: drawn as light by SoulRenderer; their renderers only put their plates over them.
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(com.example.defyingtheheavens.ModEntities.GHOST_CULTIVATOR,
				GhostlyRenderers.GhostCultivatorRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(com.example.defyingtheheavens.ModEntities.HUNGRY_GHOST,
				GhostlyRenderers.HungryGhostRenderer::new);
		// A thrown Ghost-Quelling Talisman: the item, turning as it flies (a snowball's renderer).
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(com.example.defyingtheheavens.ModEntities.GHOST_QUELLING_TALISMAN,
				net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
		// The Soul River: carried along it, its HUD, and the river itself with its starry dark.
		ClientSoulRiver.register();
		SoulRiverRenderer.register();
		ClientJudgeGaze.register();
		// The dead: who is a ghost, their look's vignette and fades.
		ClientGhosts.register();
		// Ghostly qi and yin corruption on the HUD.
		NetherworldHud.register();
		// The Ghost Eye's search for steles nearby.
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(GhostEye::tick);
	}

	/** Leaving a world: forget its Netherworld and any river journey. */
	static void clear() {
		ClientNetherworldData.reset();
		ClientSoulRiver.set(com.example.defyingtheheavens.SoulRiver.Route.NONE, 0);
		ClientJudgeGaze.clear();
		ClientGhosts.clear();
		ClientQuelled.clear();
		ClientMethods.clear();
		GhostEye.clear();
	}

	/** No clouds, no ground glow; the fog keeps the biome's dark teal (lit a little by the realm's dim ambient light). */
	private static final class NetherworldEffects extends DimensionSpecialEffects {
		NetherworldEffects() {
			super(Float.NaN, false, SkyType.NONE, false, false);
		}

		@Override
		public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
			return fogColor.multiply(0.95, 1.0, 1.05);
		}

		@Override
		public boolean isFoggyAt(int x, int y) {
			return false; // the distances are set by FogRendererMixin, not the Nether's thick fog
		}
	}

	/** The Soul River's void: its fog the deep indigo of the void's horizon (see SoulRiverSky). */
	private static final class SoulRiverEffects extends DimensionSpecialEffects {
		SoulRiverEffects() {
			super(Float.NaN, false, SkyType.NONE, false, false);
		}

		@Override
		public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
			return new Vec3(0.05, 0.04, 0.12);
		}

		@Override
		public boolean isFoggyAt(int x, int y) {
			return false;
		}
	}

	private NetherworldClient() {}
}
