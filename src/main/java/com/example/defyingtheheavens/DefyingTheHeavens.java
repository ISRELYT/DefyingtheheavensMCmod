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
		StorageFormationMenu.register();
		PuppetFactoryMenu.register(); // puppet phase 3: the Puppet Factory's screen
		OverseerMenu.register(); // and the Overseer Core's
		ModEntities.register();
		ModParticles.register();
		ModItems.register();
		ModCreativeTab.register();
		ModNoiseSettings.registerTypes();
		AstralRealm.registerWorldgen(); // the planets' generator and biome source
		SecretRealms.register(); // the secret realms' generator
		ModGameRules.register();
		ModFeatures.register();
		ModStructures.register();
		NoGhostPalaceFilter.register();
		ClothingRecipes.register();
		CoreShieldRecipe.register(); // puppet phase 3: a demon core shielded with jade and obsidian
		Milestones.register(); // the advancements' trigger
		ModPackets.registerServerReceivers();
		CultivationEvents.register();
		AstralRealm.register();
		CultivationCommand.register();
		CultivationFruitCommand.register();
		GinsengCommand.register();
		SectCommand.register();
		NetherworldCommand.register();
		SmallWorldCommand.register();
		SleeveCommand.register();
		SecretRealmCommand.register();
		WarArts.register(); // the secret realms' war arts: Sword Intent's second cuts, Blood Battle's share
		Legends.register(); // the legendary weapons' record, their keepers and trials
		LegendCommand.register();
		SectMissions.register(); // the sect mission board: its postings, quarry, the gate watch, the boards set up
		VanillaLoot.register(); // the mod's goods and writings in the vanilla world's chests (2026-11-16)
		Puppetry.register(); // the puppets' messenger bees on the wing, boards and seizings let go of as their players leave
		VfxCapture.register(); // /vfxcapture, the effects work's capture tool: only with the dev flag (DevTools), never in a player's game
		Cinematics.register(); // the breakthrough cinematics' hold, packets and /breakthroughfx (step B1: real breakthroughs play them with the dev flag only)
		LOGGER.info("Defying The Heavens initialised.");
	}

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}
}
