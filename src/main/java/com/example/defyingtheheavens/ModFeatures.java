package com.example.defyingtheheavens;

import java.util.function.Predicate;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BiomeTags;
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

	/** Plants wild ginseng on open soil (see GinsengFeature). */
	public static final Feature<NoneFeatureConfiguration> GINSENG = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("ginseng"), new GinsengFeature(() -> ModBlocks.GINSENG));
	public static final Feature<NoneFeatureConfiguration> SPIRIT_GINSENG = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("spirit_ginseng"), new GinsengFeature(() -> ModBlocks.SPIRIT_GINSENG));

	/**
	 * Touching this class registers the feature types. Also appends wild Cultivation Fruit to the end of vegetal
	 * decoration (after trees) in every Overworld biome and, more often, in every Upper Realm biome.
	 */
	public static void register() {
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
				GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.CULTIVATION_FRUIT);
		BiomeModifications.addFeature(context -> context.getBiomeKey().location().getNamespace().equals(DefyingTheHeavens.MOD_ID),
				GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.CULTIVATION_FRUIT_UPPER_REALM);

		// Ginseng grows in the shade of Overworld woods (forests, taiga, jungle) and, more often, all over the Upper Realm.
		// Spirit Ginseng is far rarer in both.
		Predicate<BiomeSelectionContext> woods = BiomeSelectors.foundInOverworld().and(BiomeSelectors.tag(BiomeTags.IS_FOREST)
				.or(BiomeSelectors.tag(BiomeTags.IS_TAIGA)).or(BiomeSelectors.tag(BiomeTags.IS_JUNGLE)));
		Predicate<BiomeSelectionContext> upperRealm = context -> context.getBiomeKey().location().getNamespace().equals(DefyingTheHeavens.MOD_ID);
		BiomeModifications.addFeature(woods, GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.GINSENG);
		BiomeModifications.addFeature(upperRealm, GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.GINSENG_UPPER_REALM);
		BiomeModifications.addFeature(woods, GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.SPIRIT_GINSENG);
		BiomeModifications.addFeature(upperRealm, GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.SPIRIT_GINSENG_UPPER_REALM);
	}

	private ModFeatures() {}
}
