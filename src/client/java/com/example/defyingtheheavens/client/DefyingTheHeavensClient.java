package com.example.defyingtheheavens.client;


import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.ModEntities;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public class DefyingTheHeavensClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
				com.example.defyingtheheavens.ModBlocks.CULTIVATION_FRUIT, net.minecraft.client.renderer.RenderType.cutout());
		// Like vanilla leaves: the gaps between blossoms are see-through (in Fast graphics, leaves draw solid anyway).
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
				com.example.defyingtheheavens.ModBlocks.WHITE_BLOSSOM_LEAVES, net.minecraft.client.renderer.RenderType.cutoutMipped());
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlocks(net.minecraft.client.renderer.RenderType.cutout(),
				com.example.defyingtheheavens.ModBlocks.GINSENG, com.example.defyingtheheavens.ModBlocks.SPIRIT_GINSENG,
				com.example.defyingtheheavens.ModBlocks.SPIRIT_PEACH_SAPLING, com.example.defyingtheheavens.ModBlocks.LINGZHI,
				com.example.defyingtheheavens.ModBlocks.PURPLE_LINGZHI, com.example.defyingtheheavens.ModBlocks.HUANGJING,
				com.example.defyingtheheavens.ModBlocks.OCHRE_HUANGJING, com.example.defyingtheheavens.ModBlocks.SPIRIT_LOTUS,
				com.example.defyingtheheavens.ModBlocks.SPIRIT_DEW_GRASS);
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
				com.example.defyingtheheavens.ModBlocks.SPIRIT_PEACH_LEAVES, net.minecraft.client.renderer.RenderType.cutoutMipped());
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
				com.example.defyingtheheavens.ModBlockEntities.GINSENG, context -> new GinsengAuraRenderer());
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
				com.example.defyingtheheavens.ModBlockEntities.CULTIVATION_FRUIT, context -> new CultivationFruitAuraRenderer());
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
				com.example.defyingtheheavens.ModBlockEntities.SPIRIT_PEDESTAL, SpiritPedestalRenderer::new);
		// Items used on both sides (pills) read the local player's cultivation from the synced mirror.
		com.example.defyingtheheavens.CultivationManager.clientMirror = ClientCultivationData::get;
		// The Alchemy Cauldron's water: the biome's water colour, turning jade while a brew cooks.
		net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
			if (tintIndex != 0) return -1;
			if (state.getValue(com.example.defyingtheheavens.AlchemyCauldronBlock.BREWING)) return 0x4FD6A0;
			return world != null && pos != null ? net.minecraft.client.renderer.BiomeColors.getAverageWaterColor(world, pos) : 0x3F76E4;
		}, com.example.defyingtheheavens.ModBlocks.ALCHEMY_CAULDRON);
		ModKeybinds.register();
		BaublePanel.register();
		ClientPacketHandlers.register();
		QiHud.register();
		QiSurgeHud.register();
		TribulationHud.register();
		TribulationAtmosphere.register();
		HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
			SuppressionClient.renderHud(graphics); // under the Qi bar
			ConsciousnessRenderer.renderHud(graphics);
		});
		EntityRendererRegistry.register(ModEntities.TRIBULATION_LIGHTNING, TribulationLightningRenderer::new);
		EntityRendererRegistry.register(ModEntities.TRIBULATION_CLOUD, TribulationCloudRenderer::new);
		EntityRendererRegistry.register(ModEntities.INNER_BODY, InnerBodyRenderer::new);
		EntityRendererRegistry.register(ModEntities.HEART_DEMON, HeartDemonRenderer::new);
		// The Inner Realm's floor: see-through, and coloured by the viewer's realm (gold where a sequence lights it).
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
				com.example.defyingtheheavens.ModBlocks.SOUL_CRYSTAL, net.minecraft.client.renderer.RenderType.translucent());
		net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) ->
				SoulCrystalColours.of(state), com.example.defyingtheheavens.ModBlocks.SOUL_CRYSTAL);
		ParticleFactoryRegistry.getInstance().register(ModParticles.SUPPRESSION, SuppressionParticle.Provider::new);
		UpperRealmClient.register();
		InnerRealmFade.register(); // last, so the fade covers the rest of the HUD

		WorldRenderEvents.AFTER_TRANSLUCENT.register(ClientSpatialStorms::render);
		WorldRenderEvents.AFTER_TRANSLUCENT.register(SuppressionClient::renderMarkers);
		// Qi motes and consciousness domains, on the layer Qi Sense's monochrome leaves in colour.
		WorldRenderEvents.END.register(SenseOverlay::onWorldEnd);
		QiSenseTreasures.register();
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public ResourceLocation getFabricId() {
				return DefyingTheHeavens.id("qi_sense_post_effect");
			}

			@Override
			public void onResourceManagerReload(ResourceManager manager) {
				QiSenseClientHandler.closeMonochrome();
			}
		});

		// When the connection drops, Fabric fires this on the network thread, while the render thread may be drawing from these
		// very lists (a ConcurrentModificationException crash): clear them on the client thread.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
			ClientTribulationData.clear();
			ClientCultivationData.clear();
			SpatialGapAmbience.clear();
			ClientSpatialStorms.clear();
			MeditationFormation.clear();
			QiSenseClientHandler.clear();
			QiSenseTreasures.clear();
			ConsciousnessRenderer.clear();
			SuppressionClient.clear();
			QiSurgeHud.clear();
			InnerRealmFade.clear();
		}));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			SpatialGapAmbience.tick(client);
			ClientSpatialStorms.tick(client);
			MeditationFormation.tick(client);
			QiSenseClientHandler.tick(client);
			QiSenseTreasures.tick(client); // after the motes' own tick, which clears them when Qi Sense is off
			ConsciousnessRenderer.tick(client);
			SuppressionClient.tick(client);
			InnerRealmFade.tick(client);
			while (ModKeybinds.OPEN_MENU.consumeClick()) {
				if (client.player != null && client.screen == null) {
					client.setScreen(new CultivationScreen());
				}
			}
			while (ModKeybinds.MEDITATE.consumeClick()) {
				if (client.player != null && client.screen == null) {
					ClientPlayNetworking.send(ModPackets.TOGGLE_MEDITATION, PacketByteBufs.create());
				}
			}
		});
	}
}
