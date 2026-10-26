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
	/** The Netherworld's herbs: the Other-Shore Flower on soul soil and soul sand, the Netherworld Lotus on still water. */
	public static final Feature<NoneFeatureConfiguration> OTHER_SHORE_FLOWER = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("other_shore_flower"), new GinsengFeature(() -> ModBlocks.OTHER_SHORE_FLOWER, GinsengFeature.Ground.SOUL, 2, 5));
	public static final Feature<NoneFeatureConfiguration> NETHERWORLD_LOTUS = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("netherworld_lotus"), new GinsengFeature(() -> ModBlocks.NETHERWORLD_LOTUS, GinsengFeature.Ground.WATER, 1, 3));
	/** A Qi Vein of 1-4 blocks (see QiVeinFeature). */
	public static final Feature<NoneFeatureConfiguration> QI_VEIN = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("qi_vein"), new QiVeinFeature());
	/** A bare dead tree hung with soul lanterns, in the Netherworld (see DeadTreeFeature). */
	public static final Feature<NoneFeatureConfiguration> DEAD_TREE = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("dead_tree"), new DeadTreeFeature());
	/** The Yellow Springs' riverlands flooded with golden water (see YellowSpringsPoolFeature). */
	public static final Feature<NoneFeatureConfiguration> YELLOW_SPRINGS_POOL = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("yellow_springs_pool"), new YellowSpringsPoolFeature());
	/** A Method Stele on its plinth, teaching a ghost method (see MethodSteleFeature). */
	public static final Feature<NoneFeatureConfiguration> METHOD_STELE = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("method_stele"), new MethodSteleFeature());
	/** A Dao Stele on its plinth, teaching a heaven method (see DaoSteleFeature). */
	public static final Feature<NoneFeatureConfiguration> DAO_STELE = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("dao_stele"), new DaoSteleFeature());
	/** A Sacred Vessel's ruined altar in the Great Void, its vessel still on it (see SacredRelicFeature). */
	public static final Feature<NoneFeatureConfiguration> SACRED_RELIC = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("sacred_relic"), new SacredRelicFeature());
	/** What an ancient war left on the Great Void's battlefields: giant swords, bones, broken walls, war chests (see BattlefieldRuinFeature). */
	public static final Feature<NoneFeatureConfiguration> BATTLEFIELD_RUIN = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("battlefield_ruin"), new BattlefieldRuinFeature());
	/** A blood pool on the Great Void's battlefields, a demon race's phial on its altar, its elder rising to keep it (see BloodPoolFeature). */
	public static final Feature<NoneFeatureConfiguration> BLOOD_POOL = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("blood_pool"), new BloodPoolFeature());
	/** The Immortal Realm's Jade Capital and Celestial Court on its main island, laid chunk by chunk (see ImmortalCapital). */
	public static final Feature<NoneFeatureConfiguration> IMMORTAL_CAPITAL = Registry.register(BuiltInRegistries.FEATURE,
			DefyingTheHeavens.id("immortal_capital"), new ImmortalCapital.CapitalFeature());

	/**
	 * Touching this class registers the feature types. Also appends wild Cultivation Fruit to the end of vegetal
	 * decoration (after trees) in every Overworld biome and, more often, in every Upper Realm biome.
	 * <p>
	 * The Upper Realm is told by its biome tag ({@link ModTags#IS_UPPER_REALM}), not by the mod's namespace: the Netherworld's
	 * biomes are the mod's too, and grow none of the Upper Realm's herbs, ores or fruit.
	 */
	public static void register() {
		// The Great Void's lands (2026-10-25) grow all the Upper Realm's own (its herbs, ores, fruit) as richly, and stand its Dao Steles
		// (all but the Shore of Nirvana).
		Predicate<BiomeSelectionContext> steleLands = BiomeSelectors.tag(ModTags.IS_UPPER_REALM).or(BiomeSelectors.tag(ModTags.IS_GREAT_VOID));
		// The Immortal Realm's peaks (phase 2) grow them all too, but stand no steles (the user's rule, 2026-10-25: "do not add steles to
		// the immortal realm"): the steles below keep to steleLands.
		Predicate<BiomeSelectionContext> upperRealm = steleLands.or(BiomeSelectors.tag(ModTags.IS_IMMORTAL_REALM));
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
				GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.CULTIVATION_FRUIT);
		BiomeModifications.addFeature(upperRealm, GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.CULTIVATION_FRUIT_UPPER_REALM);
		// Dao Steles, teaching the heaven methods: across the Overworld and, more often, the Upper Realm (not its Spatial Gap).
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.SURFACE_STRUCTURES, ModPlacedFeatures.DAO_STELE);
		BiomeModifications.addFeature(steleLands.and(context -> !context.getBiomeKey().equals(ModBiomes.SPATIAL_GAP)
				&& !context.getBiomeKey().equals(ModBiomes.SHORE_OF_NIRVANA)), GenerationStep.Decoration.SURFACE_STRUCTURES, ModPlacedFeatures.DAO_STELE_UPPER_REALM);
		// The Great Void's Sacred Vessels on their altars (its steppes and Dao fields), and its battlefields' ruins and blood pools.
		BiomeModifications.addFeature(BiomeSelectors.includeByKey(ModBiomes.VOID_STEPPES, ModBiomes.DAO_FIELD_WOOD, ModBiomes.DAO_FIELD_FIRE,
				ModBiomes.DAO_FIELD_EARTH, ModBiomes.DAO_FIELD_METAL, ModBiomes.DAO_FIELD_WATER), GenerationStep.Decoration.SURFACE_STRUCTURES,
				ModPlacedFeatures.SACRED_RELIC);
		BiomeModifications.addFeature(BiomeSelectors.includeByKey(ModBiomes.ANCIENT_BATTLEFIELD), GenerationStep.Decoration.SURFACE_STRUCTURES,
				ModPlacedFeatures.BATTLEFIELD_RUIN);
		BiomeModifications.addFeature(BiomeSelectors.includeByKey(ModBiomes.ANCIENT_BATTLEFIELD), GenerationStep.Decoration.SURFACE_STRUCTURES,
				ModPlacedFeatures.BLOOD_POOL);
		// The Immortal Realm's capital and its Court: asked in every chunk of the realm, laid in those of the main island.
		BiomeModifications.addFeature(BiomeSelectors.tag(ModTags.IS_IMMORTAL_REALM), GenerationStep.Decoration.SURFACE_STRUCTURES,
				ModPlacedFeatures.IMMORTAL_CAPITAL);

		// Ginseng grows in the shade of Overworld woods (forests, taiga, jungle) and, more often, all over the Upper Realm.
		// Spirit Ginseng is far rarer in both.
		Predicate<BiomeSelectionContext> woods = BiomeSelectors.foundInOverworld().and(BiomeSelectors.tag(BiomeTags.IS_FOREST)
				.or(BiomeSelectors.tag(BiomeTags.IS_TAIGA)).or(BiomeSelectors.tag(BiomeTags.IS_JUNGLE)));
		// Blue Spirit Trees (a spruce of Blue Spirit Log) in every taiga and anywhere in the Upper Realm. Added before the
		// herbs, so they can still take root in its shade.
		Predicate<BiomeSelectionContext> taiga = BiomeSelectors.foundInOverworld().and(BiomeSelectors.tag(BiomeTags.IS_TAIGA));
		// Not on the Immortal Realm's capital's ground: no tree grows in its streets (ImmortalCapital).
		herb(taiga, upperRealm.and(context -> !context.getBiomeKey().equals(ModBiomes.JADE_CAPITAL)), ModPlacedFeatures.BLUE_SPIRIT_TREE,
				ModPlacedFeatures.BLUE_SPIRIT_TREE_UPPER_REALM);

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

		// Qi Veins: as common as diamonds, wherever diamonds can be found (all the Overworld, every Upper Realm island, and the
		// Astral Realm's planets: those in the Overworld's biomes have them already, and its own lands have them too).
		Predicate<BiomeSelectionContext> lower = BiomeSelectors.foundInOverworld().or(BiomeSelectors.includeByKey(ModBiomes.ASTRAL_VOLCANIC,
				ModBiomes.ASTRAL_ASHEN, ModBiomes.ASTRAL_CRYSTAL, ModBiomes.ASTRAL_BARREN));
		BiomeModifications.addFeature(lower, GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.QI_VEIN);
		BiomeModifications.addFeature(upperRealm.and(context -> !context.getBiomeKey().equals(ModBiomes.SPATIAL_GAP)),
				GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.QI_VEIN_UPPER_REALM);
		jade(upperRealm);
		// Spirit stone veins (see ModPlacedFeatures#ORE_SPIRIT_STONE): the common kind everywhere underground, the rich kind deep
		// in the Overworld and all through the Upper Realm (never the Spatial Gap).
		Predicate<BiomeSelectionContext> islands = upperRealm.and(context -> !context.getBiomeKey().equals(ModBiomes.SPATIAL_GAP));
		BiomeModifications.addFeature(lower, GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.ORE_SPIRIT_STONE);
		BiomeModifications.addFeature(lower, GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.ORE_RICH_SPIRIT_STONE_DEEP);
		BiomeModifications.addFeature(islands, GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.ORE_SPIRIT_STONE_UPPER_REALM);
		BiomeModifications.addFeature(islands, GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.ORE_RICH_SPIRIT_STONE_UPPER_REALM);

		// What of the Netherworld reaches down into the Nether's Soul Sand Valleys: the Other-Shore Flower, rarely, and Yin Jade,
		// very rarely. (In the Netherworld itself they are in its biomes' own features, ModBiomes.)
		Predicate<BiomeSelectionContext> soulSandValley = BiomeSelectors.includeByKey(Biomes.SOUL_SAND_VALLEY);
		BiomeModifications.addFeature(soulSandValley, GenerationStep.Decoration.VEGETAL_DECORATION, ModPlacedFeatures.OTHER_SHORE_FLOWER_NETHER);
		BiomeModifications.addFeature(soulSandValley, GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.ORE_YIN_JADE_NETHER);
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
