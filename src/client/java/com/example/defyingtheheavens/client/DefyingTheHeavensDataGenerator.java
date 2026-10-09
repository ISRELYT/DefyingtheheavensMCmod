package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModBiomes;
import com.example.defyingtheheavens.ModConfiguredFeatures;
import com.example.defyingtheheavens.ModDamageTypes;
import com.example.defyingtheheavens.ModDimensions;
import com.example.defyingtheheavens.ModNoiseSettings;
import com.example.defyingtheheavens.ModPlacedFeatures;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

public class DefyingTheHeavensDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

		// This line hooks up your English translation manager!
		pack.addProvider(ModEnglishLangProvider::new);
		pack.addProvider(ModModelProvider::new);
		pack.addProvider(ModWorldgenProvider::new);
		pack.addProvider(ModBlockTagProvider::new);
		pack.addProvider(ModDamageTypeTagProvider::new);
		pack.addProvider(ModBlockLootProvider::new);
	}

	/** The Upper Realm's worldgen, built with vanilla's codecs and written out by ModWorldgenProvider. */
	@Override
	public void buildRegistry(RegistrySetBuilder registryBuilder) {
		registryBuilder.add(Registries.DIMENSION_TYPE, ModDimensions::bootstrapTypes);
		registryBuilder.add(Registries.NOISE, ModNoiseSettings::bootstrapNoises);
		registryBuilder.add(Registries.NOISE_SETTINGS, ModNoiseSettings::bootstrap);
		registryBuilder.add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap);
		registryBuilder.add(Registries.PLACED_FEATURE, ModPlacedFeatures::bootstrap);
		registryBuilder.add(Registries.BIOME, ModBiomes::bootstrap);
		registryBuilder.add(Registries.DAMAGE_TYPE, ModDamageTypes::bootstrap);
	}
}
