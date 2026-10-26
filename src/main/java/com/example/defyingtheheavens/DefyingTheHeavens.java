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
		BodyCaps.lift(); // max health and attack damage past vanilla's caps, for the realms after Four Axis
		ModBlocks.register();
		ModEffects.register();
		ModBlockEntities.register();
		HeavenAltarMenu.register();
		VesselMenu.register();
		StorageRingMenu.register();
		ModEntities.register();
		ModParticles.register();
		ModItems.register();
		ModCreativeTab.register();
		ModNoiseSettings.registerTypes();
		AstralRealm.registerWorldgen(); // the planets' generator and biome source
		ModGameRules.register();
		ModFeatures.register();
		ModStructures.register();
		NoGhostPalaceFilter.register();
		ClothingRecipes.register();
		ModPackets.registerServerReceivers();
		CultivationEvents.register();
		AstralRealm.register();
		CultivationCommand.register();
		CultivationFruitCommand.register();
		GinsengCommand.register();
		SectCommand.register();
		NetherworldCommand.register();
		SmallWorldCommand.register();
		LOGGER.info("Defying The Heavens initialised.");
	}

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}
}
