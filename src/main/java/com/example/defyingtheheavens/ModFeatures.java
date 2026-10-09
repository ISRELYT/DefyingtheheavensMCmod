package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/** Custom world-generation placement and feature types (used by datagen, see ModPlacedFeatures). */
public final class ModFeatures {
	/** Placement modifier that targets a random island surface in the column (see IslandSurfacePlacement). */
	public static final PlacementModifierType<IslandSurfacePlacement> ISLAND_SURFACE = Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE,
			DefyingTheHeavens.id("island_surface"), () -> IslandSurfacePlacement.CODEC);

	/** Hangs a wild Cultivation Fruit under a nearby tree's canopy (see CultivationFruitFeature). */
	public static final Feature<NoneFeatureConfiguration> CULTIVATION_FRUIT = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("cultivation_fruit"), new CultivationFruitFeature(NoneFeatureConfiguration.CODEC));

	/**
	 * Touching this class registers the feature types. Also appends wild Cultivation Fruit to the end of vegetal
	 * decoration (after trees) in every Overworld biome and, more often, in every Upper Realm biome.
	 */
	public static void register() {
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
				GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.CULTIVATION_FRUIT);
		BiomeModifications.addFeature(context -> context.getBiomeKey().location().getNamespace().equals(DefyingTheHeavens.MOD_ID),
				GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.CULTIVATION_FRUIT_UPPER_REALM);
	}

	private ModFeatures() {}
}
