package com.example.defyingtheheavens.client;


import com.example.defyingtheheavens.ModEntities;
import com.example.defyingtheheavens.ModPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

public class DefyingTheHeavensClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
				com.example.defyingtheheavens.ModBlocks.CULTIVATION_FRUIT, net.minecraft.client.renderer.RenderType.cutout());
		net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
				com.example.defyingtheheavens.ModBlockEntities.CULTIVATION_FRUIT, context -> new CultivationFruitAuraRenderer());
		ModKeybinds.register();
		ClientPacketHandlers.register();
		QiHud.register();
		TribulationHud.register();
		TribulationAtmosphere.register();
		EntityRendererRegistry.register(ModEntities.TRIBULATION_LIGHTNING, TribulationLightningRenderer::new);
		EntityRendererRegistry.register(ModEntities.TRIBULATION_CLOUD, TribulationCloudRenderer::new);
		UpperRealmClient.register();

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientTribulationData.clear();
			ClientCultivationData.clear();
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
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
