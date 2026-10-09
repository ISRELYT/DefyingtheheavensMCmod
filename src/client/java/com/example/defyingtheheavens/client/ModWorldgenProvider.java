package com.example.defyingtheheavens.client;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;

import java.util.concurrent.CompletableFuture;

/**
 * Writes the mod's worldgen and dynamic-registry entries (built in DefyingTheHeavensDataGenerator#buildRegistry) as
 * datapack JSON: dimension types, noises, noise settings, configured/placed features, biomes and the damage type.
 */
public class ModWorldgenProvider extends FabricDynamicRegistryProvider {
	public ModWorldgenProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
		super(output, registriesFuture);
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		entries.addAll(registries.lookupOrThrow(Registries.DIMENSION_TYPE));
		entries.addAll(registries.lookupOrThrow(Registries.NOISE));
		entries.addAll(registries.lookupOrThrow(Registries.NOISE_SETTINGS));
		entries.addAll(registries.lookupOrThrow(Registries.CONFIGURED_FEATURE));
		entries.addAll(registries.lookupOrThrow(Registries.PLACED_FEATURE));
		entries.addAll(registries.lookupOrThrow(Registries.BIOME));
		entries.addAll(registries.lookupOrThrow(Registries.DAMAGE_TYPE));
	}

	@Override
	public String getName() {
		return "Defying The Heavens worldgen";
	}
}
