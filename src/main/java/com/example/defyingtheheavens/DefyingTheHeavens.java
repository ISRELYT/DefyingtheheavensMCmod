package com.example.defyingtheheavens;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefyingTheHeavens implements ModInitializer {
	public static final String MOD_ID = "defying-the-heavens";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.register();
		ModBlockEntities.register();
		ModEntities.register();
		ModItems.register();
		ModFeatures.register();
		ModPackets.registerServerReceivers();
		CultivationEvents.register();
		CultivationCommand.register();
		CultivationFruitCommand.register();
		GinsengCommand.register();
		LOGGER.info("Defying The Heavens initialised.");
	}

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}
}
