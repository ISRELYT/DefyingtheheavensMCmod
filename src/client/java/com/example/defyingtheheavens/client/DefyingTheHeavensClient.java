package com.example.defyingtheheavens.client;


import com.example.defyingtheheavens.ModEntities;
import com.example.defyingtheheavens.ModPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

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
				com.example.defyingtheheavens.ModBlocks.SPIRIT_PEACH_SAPLING);
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
				com.example.defyingtheheavens.ModBlocks.SPIRIT_PEACH_LEAVES, net.minecraft.client.renderer.RenderType.cutoutMipped());
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
				com.example.defyingtheheavens.ModBlockEntities.GINSENG, context -> new GinsengAuraRenderer());
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
				com.example.defyingtheheavens.ModBlockEntities.CULTIVATION_FRUIT, context -> new CultivationFruitAuraRenderer());
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
				com.example.defyingtheheavens.ModBlockEntities.SPIRIT_PEDESTAL, SpiritPedestalRenderer::new);
		ModKeybinds.register();
		BaublePanel.register();
		ClientPacketHandlers.register();
		QiHud.register();
		TribulationHud.register();
		TribulationAtmosphere.register();
		EntityRendererRegistry.register(ModEntities.TRIBULATION_LIGHTNING, TribulationLightningRenderer::new);
		EntityRendererRegistry.register(ModEntities.TRIBULATION_CLOUD, TribulationCloudRenderer::new);
		UpperRealmClient.register();

		WorldRenderEvents.AFTER_TRANSLUCENT.register(ClientSpatialStorms::render);

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientTribulationData.clear();
			ClientCultivationData.clear();
			SpatialGapAmbience.clear();
			ClientSpatialStorms.clear();
			MeditationFormation.clear();
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			SpatialGapAmbience.tick(client);
			ClientSpatialStorms.tick(client);
			MeditationFormation.tick(client);
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
