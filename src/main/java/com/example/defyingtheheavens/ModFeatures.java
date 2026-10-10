package com.example.defyingtheheavens;

import java.util.function.Predicate;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
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

	/** Plants wild ginseng on open soil, in small patches (see GinsengFeature): 2-4 plants, rare kinds 1-3. */
	public static final Feature<NoneFeatureConfiguration> GINSENG = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("ginseng"), new GinsengFeature(() -> ModBlocks.GINSENG, 2, 4));
	public static final Feature<NoneFeatureConfiguration> SPIRIT_GINSENG = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("spirit_ginseng"), new GinsengFeature(() -> ModBlocks.SPIRIT_GINSENG, 1, 3));
	/** The other wild herbs (see GinsengFeature): Spirit Lotus on still water, the rest on open soil. */
	public static final Feature<NoneFeatureConfiguration> LINGZHI = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("lingzhi"), new GinsengFeature(() -> ModBlocks.LINGZHI, 2, 4));
	public static final Feature<NoneFeatureConfiguration> PURPLE_LINGZHI = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("purple_lingzhi"), new GinsengFeature(() -> ModBlocks.PURPLE_LINGZHI, 1, 3));
	public static final Feature<NoneFeatureConfiguration> HUANGJING = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("huangjing"), new GinsengFeature(() -> ModBlocks.HUANGJING, 2, 4));
	public static final Feature<NoneFeatureConfiguration> OCHRE_HUANGJING = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("ochre_huangjing"), new GinsengFeature(() -> ModBlocks.OCHRE_HUANGJING, 1, 3));
	public static final Feature<NoneFeatureConfiguration> SPIRIT_LOTUS = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("spirit_lotus"), new GinsengFeature(() -> ModBlocks.SPIRIT_LOTUS, GinsengFeature.Ground.WATER, 2, 4));
	public static final Feature<NoneFeatureConfiguration> SPIRIT_DEW_GRASS = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("spirit_dew_grass"), new GinsengFeature(() -> ModBlocks.SPIRIT_DEW_GRASS, 3, 6));
	/** A Qi Vein of 1-4 blocks (see QiVeinFeature). */
	public static final Feature<NoneFeatureConfiguration> QI_VEIN = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("qi_vein"), new QiVeinFeature());

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
		// Blue Spirit Trees (a spruce of Blue Spirit Log) in every taiga and anywhere in the Upper Realm. Added before the
		// herbs, so they can still take root in its shade.
		Predicate<BiomeSelectionContext> taiga = BiomeSelectors.foundInOverworld().and(BiomeSelectors.tag(BiomeTags.IS_TAIGA));
		herb(taiga, upperRealm, ModPlacedFeatures.BLUE_SPIRIT_TREE, ModPlacedFeatures.BLUE_SPIRIT_TREE_UPPER_REALM);

		BiomeModifications.addFeature(woods, GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.GINSENG);
		BiomeModifications.addFeature(upperRealm, GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.GINSENG_UPPER_REALM);
		BiomeModifications.addFeature(woods, GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.SPIRIT_GINSENG);
		BiomeModifications.addFeature(upperRealm, GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.SPIRIT_GINSENG_UPPER_REALM);

		// Lingzhi grows in deep shade (dark and old-growth forests), Huangjing in the same woods as ginseng, the Spirit Lotus
		// on swamps and rivers, Spirit Dew Grass in flowery meadows. All of them, and more often, everywhere in the Upper Realm.
		Predicate<BiomeSelectionContext> shade = BiomeSelectors.foundInOverworld().and(BiomeSelectors.includeByKey(Biomes.DARK_FOREST,
				Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA));
		Predicate<BiomeSelectionContext> wetlands = BiomeSelectors.foundInOverworld().and(BiomeSelectors.includeByKey(Biomes.SWAMP,
				Biomes.MANGROVE_SWAMP).or(BiomeSelectors.tag(BiomeTags.IS_RIVER)));
		Predicate<BiomeSelectionContext> meadows = BiomeSelectors.foundInOverworld().and(BiomeSelectors.includeByKey(Biomes.MEADOW,
				Biomes.FLOWER_FOREST, Biomes.CHERRY_GROVE, Biomes.SUNFLOWER_PLAINS));
		herb(shade, upperRealm, ModPlacedFeatures.LINGZHI, ModPlacedFeatures.LINGZHI_UPPER_REALM);
		herb(shade, upperRealm, ModPlacedFeatures.PURPLE_LINGZHI, ModPlacedFeatures.PURPLE_LINGZHI_UPPER_REALM);
		herb(woods, upperRealm, ModPlacedFeatures.HUANGJING, ModPlacedFeatures.HUANGJING_UPPER_REALM);
		herb(woods, upperRealm, ModPlacedFeatures.OCHRE_HUANGJING, ModPlacedFeatures.OCHRE_HUANGJING_UPPER_REALM);
		herb(wetlands, upperRealm, ModPlacedFeatures.SPIRIT_LOTUS, ModPlacedFeatures.SPIRIT_LOTUS_UPPER_REALM);
		herb(meadows, upperRealm, ModPlacedFeatures.SPIRIT_DEW_GRASS, ModPlacedFeatures.SPIRIT_DEW_GRASS_UPPER_REALM);

		// Qi Veins: as common as diamonds, wherever diamonds can be found (all the Overworld, every Upper Realm island).
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.QI_VEIN);
		BiomeModifications.addFeature(upperRealm.and(context -> !context.getBiomeKey().equals(ModBiomes.SPATIAL_GAP)),
				GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.QI_VEIN_UPPER_REALM);
		jade(upperRealm);
	}

	/** Jade Ore in Overworld mountains and all through the Upper Realm. */
	private static void jade(Predicate<BiomeSelectionContext> upperRealm) {
		// Jade stone blobs first, so Jade Ore can form inside them too.
		BiomeModifications.addFeature(BiomeSelectors.includeByKey(ModBiomes.DENSE_QI_PEAKS), GenerationStep.Decoration.UNDERGROUND_ORES,
				ModPlacedFeatures.ORE_JADE_STONE);
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld().and(BiomeSelectors.tag(BiomeTags.IS_MOUNTAIN)),
				GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.ORE_JADE);
		BiomeModifications.addFeature(upperRealm, GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.ORE_JADE_UPPER_REALM);
	}

	private static void herb(Predicate<BiomeSelectionContext> overworld, Predicate<BiomeSelectionContext> upperRealm,
			ResourceKey<PlacedFeature> lower, ResourceKey<PlacedFeature> upper) {
		BiomeModifications.addFeature(overworld, GenerationStep.Decoration.VEGETAL_DECORATION, lower);
		BiomeModifications.addFeature(upperRealm, GenerationStep.Decoration.VEGETAL_DECORATION, upper);
	}

	private ModFeatures() {}
}
