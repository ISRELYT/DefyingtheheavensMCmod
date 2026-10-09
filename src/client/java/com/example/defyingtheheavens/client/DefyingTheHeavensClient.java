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
		ModKeybinds.register();
		ClientPacketHandlers.register();
		QiHud.register(); // before the tribulation HUD, whose red tint then covers the Qi bar too
		TribulationHud.register();
		EntityRendererRegistry.register(ModEntities.TRIBULATION_LIGHTNING, TribulationLightningRenderer::new);
		UpperRealmClient.register();

		WorldRenderEvents.AFTER_TRANSLUCENT.register(TribulationClouds::render);

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientTribulationData.clear();
			ClientCultivationData.clear();
			TribulationClouds.clear();
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			TribulationClouds.tick(client);
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
