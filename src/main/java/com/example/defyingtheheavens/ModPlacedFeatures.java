package com.example.defyingtheheavens;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.NoiseBasedCountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Upper Realm placed features.
 * <p>
 * Surface decoration can't use vanilla's heightmap placement, which only ever finds the topmost island in a column.
 * Instead {@link IslandSurfacePlacement} picks a random island surface in the column, so islands on every tier get
 * trees, flowers and ponds.
 * <p>
 * Ores: attempts are spread uniformly over all island heights; attempts that land in the abyss place nothing. Counts
 * are calibrated (by in-game block census) to about 1.75x the Overworld's ore blocks per block of stone. Emeralds are
 * limited to the Dense Qi Peaks and quartz to the Thunder Peaks, each about 1.75x its vanilla counterpart.
 * <p>
 * {@link #ORDER} is the single feature order shared by every biome; biomes pick subsets of it via
 * {@link #addFeatures}, which keeps the order consistent and avoids the "feature order cycle" crash.
 */
public final class ModPlacedFeatures {
	public record Entry(GenerationStep.Decoration step, ResourceKey<PlacedFeature> key) {}

	public static final int ISLAND_MIN_Y = 40;
	public static final int ISLAND_MAX_Y = 500;
	private static final int ORE_MAX_Y = 480;

	public static final ResourceKey<PlacedFeature> SPRING_BASIN = key("spring_basin");
	public static final ResourceKey<PlacedFeature> LAVA_LAKE = key("lava_lake");
	public static final ResourceKey<PlacedFeature> FOREST_ROCK = key("forest_rock");
	public static final ResourceKey<PlacedFeature> ORE_DIRT = key("ore_dirt");
	public static final ResourceKey<PlacedFeature> ORE_GRAVEL = key("ore_gravel");
	public static final ResourceKey<PlacedFeature> ORE_GRANITE = key("ore_granite");
	public static final ResourceKey<PlacedFeature> ORE_DIORITE = key("ore_diorite");
	public static final ResourceKey<PlacedFeature> ORE_ANDESITE = key("ore_andesite");
	public static final ResourceKey<PlacedFeature> ORE_CLAY = key("ore_clay");
	public static final ResourceKey<PlacedFeature> ORE_SAND = key("ore_sand");
	public static final ResourceKey<PlacedFeature> ORE_COAL = key("ore_coal");
	public static final ResourceKey<PlacedFeature> ORE_IRON = key("ore_iron");
	public static final ResourceKey<PlacedFeature> ORE_COPPER = key("ore_copper");
	public static final ResourceKey<PlacedFeature> ORE_GOLD = key("ore_gold");
	public static final ResourceKey<PlacedFeature> ORE_REDSTONE = key("ore_redstone");
	public static final ResourceKey<PlacedFeature> ORE_LAPIS = key("ore_lapis");
	public static final ResourceKey<PlacedFeature> ORE_DIAMOND = key("ore_diamond");
	public static final ResourceKey<PlacedFeature> ORE_DIAMOND_BURIED = key("ore_diamond_buried");
	public static final ResourceKey<PlacedFeature> ORE_EMERALD = key("ore_emerald");
	public static final ResourceKey<PlacedFeature> ORE_QUARTZ = key("ore_quartz");
	public static final ResourceKey<PlacedFeature> SPRING_WATER = key("spring_water");
	public static final ResourceKey<PlacedFeature> SPRING_LAVA = key("spring_lava");
	public static final ResourceKey<PlacedFeature> SPIRIT_FOREST_TREES = key("spirit_forest_trees");
	public static final ResourceKey<PlacedFeature> BAMBOO_GROVE = key("bamboo_grove");
	public static final ResourceKey<PlacedFeature> PEACH_BLOSSOM_TREES = key("peach_blossom_trees");
	public static final ResourceKey<PlacedFeature> PEAK_SPRUCES = key("peak_spruces");
	public static final ResourceKey<PlacedFeature> FLOWERS_CHERRY = key("flowers_cherry");
	public static final ResourceKey<PlacedFeature> FLOWERS_FOREST = key("flowers_forest");
	public static final ResourceKey<PlacedFeature> FLOWERS_DEFAULT = key("flowers_default");
	public static final ResourceKey<PlacedFeature> PATCH_GRASS = key("patch_grass");
	public static final ResourceKey<PlacedFeature> PATCH_SUGAR_CANE = key("patch_sugar_cane");
	public static final ResourceKey<PlacedFeature> PATCH_PUMPKIN = key("patch_pumpkin");
	public static final ResourceKey<PlacedFeature> PATCH_BERRY_BUSH = key("patch_berry_bush");

	/**
	 * Wild Cultivation Fruit. Not in {@link #ORDER}: ModFeatures#register appends these to the end of vegetal decoration
	 * with Fabric's biome modifications, so they always run after every biome's trees.
	 */
	public static final ResourceKey<PlacedFeature> CULTIVATION_FRUIT = key("cultivation_fruit");
	public static final ResourceKey<PlacedFeature> CULTIVATION_FRUIT_UPPER_REALM = key("cultivation_fruit_upper_realm");
	/** On average one fruit per this many chunks that have trees. The Upper Realm's dense qi bears fruit far more often. */
	public static final int FRUIT_CHUNKS_OVERWORLD = 64;
	public static final int FRUIT_CHUNKS_UPPER_REALM = 24;
	/**
	 * Wild ginseng, also appended by ModFeatures#register: in Overworld woods (forests, taiga, jungle) and in every Upper
	 * Realm biome. On average one per this many chunks; Spirit Ginseng is far rarer.
	 */
	public static final ResourceKey<PlacedFeature> GINSENG = key("ginseng");
	public static final ResourceKey<PlacedFeature> GINSENG_UPPER_REALM = key("ginseng_upper_realm");
	public static final ResourceKey<PlacedFeature> SPIRIT_GINSENG = key("spirit_ginseng");
	public static final ResourceKey<PlacedFeature> SPIRIT_GINSENG_UPPER_REALM = key("spirit_ginseng_upper_realm");
	public static final int GINSENG_CHUNKS_WOODS = 80;
	public static final int GINSENG_CHUNKS_UPPER_REALM = 32;
	public static final int SPIRIT_GINSENG_CHUNKS_WOODS = 640;
	public static final int SPIRIT_GINSENG_CHUNKS_UPPER_REALM = 160;
	/**
	 * The other wild herbs (see ModFeatures#register for where): on average one per this many chunks in the Overworld biomes
	 * they grow in, and in the Upper Realm. The rarer variants (Purple Lingzhi, Ochre Huangjing) are far rarer.
	 */
	public static final ResourceKey<PlacedFeature> LINGZHI = key("lingzhi");
	public static final ResourceKey<PlacedFeature> LINGZHI_UPPER_REALM = key("lingzhi_upper_realm");
	public static final ResourceKey<PlacedFeature> PURPLE_LINGZHI = key("purple_lingzhi");
	public static final ResourceKey<PlacedFeature> PURPLE_LINGZHI_UPPER_REALM = key("purple_lingzhi_upper_realm");
	public static final ResourceKey<PlacedFeature> HUANGJING = key("huangjing");
	public static final ResourceKey<PlacedFeature> HUANGJING_UPPER_REALM = key("huangjing_upper_realm");
	public static final ResourceKey<PlacedFeature> OCHRE_HUANGJING = key("ochre_huangjing");
	public static final ResourceKey<PlacedFeature> OCHRE_HUANGJING_UPPER_REALM = key("ochre_huangjing_upper_realm");
	public static final ResourceKey<PlacedFeature> SPIRIT_LOTUS = key("spirit_lotus");
	public static final ResourceKey<PlacedFeature> SPIRIT_LOTUS_UPPER_REALM = key("spirit_lotus_upper_realm");
	public static final ResourceKey<PlacedFeature> SPIRIT_DEW_GRASS = key("spirit_dew_grass");
	public static final ResourceKey<PlacedFeature> SPIRIT_DEW_GRASS_UPPER_REALM = key("spirit_dew_grass_upper_realm");
	public static final int LINGZHI_CHUNKS_OVERWORLD = 100;
	public static final int LINGZHI_CHUNKS_UPPER_REALM = 40;
	public static final int PURPLE_LINGZHI_CHUNKS_OVERWORLD = 1000;
	public static final int PURPLE_LINGZHI_CHUNKS_UPPER_REALM = 250;
	public static final int HUANGJING_CHUNKS_OVERWORLD = 100;
	public static final int HUANGJING_CHUNKS_UPPER_REALM = 40;
	public static final int OCHRE_HUANGJING_CHUNKS_OVERWORLD = 1000;
	public static final int OCHRE_HUANGJING_CHUNKS_UPPER_REALM = 250;
	public static final int SPIRIT_LOTUS_CHUNKS_OVERWORLD = 160;
	public static final int SPIRIT_LOTUS_CHUNKS_UPPER_REALM = 64;
	public static final int SPIRIT_DEW_GRASS_CHUNKS_OVERWORLD = 40;
	public static final int SPIRIT_DEW_GRASS_CHUNKS_UPPER_REALM = 16;
	/**
	 * Blue Spirit Trees: a spruce of Blue Spirit Log, common in Overworld taiga and anywhere in the Upper Realm, also appended
	 * by ModFeatures#register (before the herbs, so herbs can grow beneath them). One per this many chunks.
	 */
	/**
	 * Jade Ore, also appended by ModFeatures#register: veins per chunk in Overworld mountains (from deep in the deepslate
	 * up into the peaks) and all through the Upper Realm's islands. Set to give about as much jade as there are diamonds.
	 */
	/** Blobs of jade stone through the Dense Qi Peaks' stone (appended by ModFeatures#register), like granite elsewhere. */
	public static final ResourceKey<PlacedFeature> ORE_JADE_STONE = key("ore_jade_stone");
	public static final int JADE_STONE_BLOBS = 24;
	public static final ResourceKey<PlacedFeature> ORE_JADE = key("ore_jade");
	public static final ResourceKey<PlacedFeature> ORE_JADE_UPPER_REALM = key("ore_jade_upper_realm");
	public static final int JADE_VEINS_MOUNTAINS = 13;
	public static final int JADE_VEINS_UPPER_REALM = 30;
	public static final ResourceKey<PlacedFeature> BLUE_SPIRIT_TREE = key("blue_spirit_tree");
	public static final ResourceKey<PlacedFeature> BLUE_SPIRIT_TREE_UPPER_REALM = key("blue_spirit_tree_upper_realm");
	public static final int BLUE_SPIRIT_TREE_CHUNKS_TAIGA = 2;
	public static final int BLUE_SPIRIT_TREE_CHUNKS_UPPER_REALM = 2;
	/**
	 * Qi Veins, appended to underground ores by ModFeatures#register. In the Overworld placed exactly like vanilla's diamond
	 * veins (7 tries a chunk, peaking at Y -16, from the bottom of the world up to Y 16); in the Upper Realm as often as its
	 * own diamond veins, across every island tier.
	 */
	public static final ResourceKey<PlacedFeature> QI_VEIN = key("qi_vein");
	public static final ResourceKey<PlacedFeature> QI_VEIN_UPPER_REALM = key("qi_vein_upper_realm");
	public static final int QI_VEIN_TRIES_OVERWORLD = 7;
	public static final int QI_VEIN_TRIES_UPPER_REALM = 22;

	public static final List<Entry> ORDER = List.of(
			new Entry(GenerationStep.Decoration.LAKES, SPRING_BASIN),
			new Entry(GenerationStep.Decoration.LAKES, LAVA_LAKE),
			new Entry(GenerationStep.Decoration.LOCAL_MODIFICATIONS, FOREST_ROCK),
			new Entry(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_DIRT),
			new Entry(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_GRAVEL),
			new Entry(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_GRANITE),
			new Entry(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_DIORITE),
			new Entry(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_ANDESITE),
			new Entry(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_CLAY),
			new Entry(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_SAND),
			new Entry(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_COAL),
			new Entry(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_IRON),
			new Entry(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_COPPER),
			new Entry(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_GOLD),
			new Entry(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_REDSTONE),
			new Entry(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_LAPIS),
			new Entry(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_DIAMOND),
			new Entry(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_DIAMOND_BURIED),
			new Entry(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_EMERALD),
			new Entry(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_QUARTZ),
			new Entry(GenerationStep.Decoration.FLUID_SPRINGS, SPRING_WATER),
			new Entry(GenerationStep.Decoration.FLUID_SPRINGS, SPRING_LAVA),
			new Entry(GenerationStep.Decoration.VEGETAL_DECORATION, SPIRIT_FOREST_TREES),
			new Entry(GenerationStep.Decoration.VEGETAL_DECORATION, BAMBOO_GROVE),
			new Entry(GenerationStep.Decoration.VEGETAL_DECORATION, PEACH_BLOSSOM_TREES),
			new Entry(GenerationStep.Decoration.VEGETAL_DECORATION, PEAK_SPRUCES),
			new Entry(GenerationStep.Decoration.VEGETAL_DECORATION, FLOWERS_CHERRY),
			new Entry(GenerationStep.Decoration.VEGETAL_DECORATION, FLOWERS_FOREST),
			new Entry(GenerationStep.Decoration.VEGETAL_DECORATION, FLOWERS_DEFAULT),
			new Entry(GenerationStep.Decoration.VEGETAL_DECORATION, PATCH_GRASS),
			new Entry(GenerationStep.Decoration.VEGETAL_DECORATION, PATCH_SUGAR_CANE),
			new Entry(GenerationStep.Decoration.VEGETAL_DECORATION, PATCH_PUMPKIN),
			new Entry(GenerationStep.Decoration.VEGETAL_DECORATION, PATCH_BERRY_BUSH));

	/** Resources every Upper Realm biome provides, so an ascended cultivator never has to go back down for basics. */
	public static final Set<ResourceKey<PlacedFeature>> COMMON = Set.of(
			ORE_DIRT, ORE_GRAVEL, ORE_GRANITE, ORE_DIORITE, ORE_ANDESITE, ORE_SAND,
			ORE_COAL, ORE_IRON, ORE_COPPER, ORE_GOLD, ORE_REDSTONE, ORE_LAPIS, ORE_DIAMOND, ORE_DIAMOND_BURIED,
			SPRING_WATER, SPRING_LAVA);

	private static ResourceKey<PlacedFeature> key(String name) {
		return ResourceKey.create(Registries.PLACED_FEATURE, DefyingTheHeavens.id(name));
	}

	/** Adds the wanted features to a biome in the canonical {@link #ORDER}. */
	public static void addFeatures(BiomeGenerationSettings.Builder builder, Set<ResourceKey<PlacedFeature>> wanted) {
		for (Entry entry : ORDER) {
			if (wanted.contains(entry.key())) {
				builder.addFeature(entry.step(), entry.key());
			}
		}
	}

	public static void bootstrap(BootstapContext<PlacedFeature> context) {
		HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);

		// Ponds, lava pools and rocks.
		register(context, SPRING_BASIN, configured.getOrThrow(ModConfiguredFeatures.SPRING_BASIN),
				prepend(RarityFilter.onAverageOnceEvery(2), surface(CountPlacement.of(1), null)));
		register(context, LAVA_LAKE, configured.getOrThrow(MiscOverworldFeatures.LAKE_LAVA), surface(RarityFilter.onAverageOnceEvery(8), null));
		register(context, FOREST_ROCK, configured.getOrThrow(MiscOverworldFeatures.FOREST_ROCK), surface(CountPlacement.of(1), null));

		// Fillers.
		register(context, ORE_DIRT, configured.getOrThrow(ModConfiguredFeatures.ORE_DIRT), ore(6, ISLAND_MIN_Y, 480));
		register(context, ORE_GRAVEL, configured.getOrThrow(ModConfiguredFeatures.ORE_GRAVEL), ore(8, ISLAND_MIN_Y, 480));
		register(context, ORE_GRANITE, configured.getOrThrow(ModConfiguredFeatures.ORE_GRANITE), ore(4, ISLAND_MIN_Y, 480));
		register(context, ORE_DIORITE, configured.getOrThrow(ModConfiguredFeatures.ORE_DIORITE), ore(4, ISLAND_MIN_Y, 480));
		register(context, ORE_ANDESITE, configured.getOrThrow(ModConfiguredFeatures.ORE_ANDESITE), ore(6, ISLAND_MIN_Y, 480));
		register(context, ORE_CLAY, configured.getOrThrow(ModConfiguredFeatures.ORE_CLAY), ore(2, ISLAND_MIN_Y, ORE_MAX_Y));
		register(context, ORE_SAND, configured.getOrThrow(ModConfiguredFeatures.ORE_SAND), ore(6, ISLAND_MIN_Y, ORE_MAX_Y));

		// Ores span every island tier, so the yield doesn't depend on how stone is split between tiers. Counts are
		// calibrated with an in-game block census (ore blocks per 100k ore-replaceable stone, Overworld incl. deepslate)
		// to about 1.75x the Overworld's coal 279, iron 276, copper 285, gold 98, redstone 139, lapis 90, diamond 57.
		register(context, ORE_COAL, configured.getOrThrow(ModConfiguredFeatures.ORE_COAL), ore(25, ISLAND_MIN_Y, ORE_MAX_Y));
		register(context, ORE_IRON, configured.getOrThrow(ModConfiguredFeatures.ORE_IRON), ore(82, ISLAND_MIN_Y, ORE_MAX_Y));
		register(context, ORE_COPPER, configured.getOrThrow(ModConfiguredFeatures.ORE_COPPER), ore(68, ISLAND_MIN_Y, ORE_MAX_Y));
		register(context, ORE_GOLD, configured.getOrThrow(ModConfiguredFeatures.ORE_GOLD), ore(29, ISLAND_MIN_Y, ORE_MAX_Y));
		register(context, ORE_REDSTONE, configured.getOrThrow(ModConfiguredFeatures.ORE_REDSTONE), ore(49, ISLAND_MIN_Y, ORE_MAX_Y));
		register(context, ORE_LAPIS, configured.getOrThrow(ModConfiguredFeatures.ORE_LAPIS), ore(37, ISLAND_MIN_Y, ORE_MAX_Y));
		register(context, ORE_DIAMOND, configured.getOrThrow(ModConfiguredFeatures.ORE_DIAMOND), ore(22, ISLAND_MIN_Y, ORE_MAX_Y));
		register(context, ORE_DIAMOND_BURIED, configured.getOrThrow(ModConfiguredFeatures.ORE_DIAMOND_BURIED), ore(13, ISLAND_MIN_Y, ORE_MAX_Y));
		// Peak-only ores, like vanilla emeralds in mountains: Dense Qi Peaks (jade) and Thunder Peaks (basalt). Also about
		// 1.75x vanilla: emeralds per jade vs. Jagged Peaks (29 per 100k stone), quartz per basalt and blackstone vs. the
		// whole Nether (600 per 100k netherrack, basalt and blackstone).
		register(context, ORE_EMERALD, configured.getOrThrow(ModConfiguredFeatures.ORE_EMERALD), ore(57, 300, ISLAND_MAX_Y));
		register(context, ORE_QUARTZ, configured.getOrThrow(ModConfiguredFeatures.ORE_QUARTZ), ore(48, 300, ISLAND_MAX_Y));

		// Springs in island walls: water spills over cliffs and undersides as waterfalls into the abyss.
		register(context, SPRING_WATER, configured.getOrThrow(MiscOverworldFeatures.SPRING_WATER), ore(96, ISLAND_MIN_Y, 480));
		register(context, SPRING_LAVA, configured.getOrThrow(MiscOverworldFeatures.SPRING_LAVA_OVERWORLD), ore(6, ISLAND_MIN_Y, 480));

		// Vegetation.
		register(context, SPIRIT_FOREST_TREES, configured.getOrThrow(ModConfiguredFeatures.SPIRIT_FOREST_TREES), surface(CountPlacement.of(8), null));
		register(context, BAMBOO_GROVE, configured.getOrThrow(VegetationFeatures.BAMBOO_SOME_PODZOL), surface(NoiseBasedCountPlacement.of(60, 80.0, 0.3), null));
		// Cherry canopies are wide; more than a couple per chunk merges them into one solid pink layer.
		register(context, PEACH_BLOSSOM_TREES, configured.getOrThrow(ModConfiguredFeatures.PEACH_BLOSSOM_TREES), surface(CountPlacement.of(3), null));
		register(context, PEAK_SPRUCES, configured.getOrThrow(TreeFeatures.SPRUCE), surface(CountPlacement.of(2), Blocks.SPRUCE_SAPLING));
		register(context, FLOWERS_CHERRY, configured.getOrThrow(VegetationFeatures.FLOWER_CHERRY), surface(CountPlacement.of(4), null));
		register(context, FLOWERS_FOREST, configured.getOrThrow(VegetationFeatures.FLOWER_FLOWER_FOREST), surface(CountPlacement.of(3), null));
		register(context, FLOWERS_DEFAULT, configured.getOrThrow(VegetationFeatures.FLOWER_DEFAULT), surface(CountPlacement.of(2), null));
		register(context, PATCH_GRASS, configured.getOrThrow(VegetationFeatures.PATCH_GRASS), surface(CountPlacement.of(6), null));
		register(context, PATCH_SUGAR_CANE, configured.getOrThrow(VegetationFeatures.PATCH_SUGAR_CANE), surface(CountPlacement.of(4), null));
		register(context, PATCH_PUMPKIN, configured.getOrThrow(VegetationFeatures.PATCH_PUMPKIN), surface(RarityFilter.onAverageOnceEvery(8), null));
		register(context, PATCH_BERRY_BUSH, configured.getOrThrow(VegetationFeatures.PATCH_BERRY_BUSH), surface(CountPlacement.of(1), null));

		// Wild Cultivation Fruit: the feature searches whole columns for canopies itself, so a plain heightmap origin will do.
		register(context, CULTIVATION_FRUIT, configured.getOrThrow(ModConfiguredFeatures.CULTIVATION_FRUIT), fruit(FRUIT_CHUNKS_OVERWORLD));
		register(context, CULTIVATION_FRUIT_UPPER_REALM, configured.getOrThrow(ModConfiguredFeatures.CULTIVATION_FRUIT), fruit(FRUIT_CHUNKS_UPPER_REALM));

		// Wild ginseng: the feature searches whole columns for open soil itself, so the same plain origin will do.
		register(context, GINSENG, configured.getOrThrow(ModConfiguredFeatures.GINSENG), fruit(GINSENG_CHUNKS_WOODS));
		register(context, GINSENG_UPPER_REALM, configured.getOrThrow(ModConfiguredFeatures.GINSENG), fruit(GINSENG_CHUNKS_UPPER_REALM));
		register(context, SPIRIT_GINSENG, configured.getOrThrow(ModConfiguredFeatures.SPIRIT_GINSENG), fruit(SPIRIT_GINSENG_CHUNKS_WOODS));
		register(context, SPIRIT_GINSENG_UPPER_REALM, configured.getOrThrow(ModConfiguredFeatures.SPIRIT_GINSENG),
				fruit(SPIRIT_GINSENG_CHUNKS_UPPER_REALM));
		// The other wild herbs, placed the same way.
		register(context, LINGZHI, configured.getOrThrow(ModConfiguredFeatures.LINGZHI), fruit(LINGZHI_CHUNKS_OVERWORLD));
		register(context, LINGZHI_UPPER_REALM, configured.getOrThrow(ModConfiguredFeatures.LINGZHI), fruit(LINGZHI_CHUNKS_UPPER_REALM));
		register(context, PURPLE_LINGZHI, configured.getOrThrow(ModConfiguredFeatures.PURPLE_LINGZHI), fruit(PURPLE_LINGZHI_CHUNKS_OVERWORLD));
		register(context, PURPLE_LINGZHI_UPPER_REALM, configured.getOrThrow(ModConfiguredFeatures.PURPLE_LINGZHI), fruit(PURPLE_LINGZHI_CHUNKS_UPPER_REALM));
		register(context, HUANGJING, configured.getOrThrow(ModConfiguredFeatures.HUANGJING), fruit(HUANGJING_CHUNKS_OVERWORLD));
		register(context, HUANGJING_UPPER_REALM, configured.getOrThrow(ModConfiguredFeatures.HUANGJING), fruit(HUANGJING_CHUNKS_UPPER_REALM));
		register(context, OCHRE_HUANGJING, configured.getOrThrow(ModConfiguredFeatures.OCHRE_HUANGJING), fruit(OCHRE_HUANGJING_CHUNKS_OVERWORLD));
		register(context, OCHRE_HUANGJING_UPPER_REALM, configured.getOrThrow(ModConfiguredFeatures.OCHRE_HUANGJING), fruit(OCHRE_HUANGJING_CHUNKS_UPPER_REALM));
		register(context, SPIRIT_LOTUS, configured.getOrThrow(ModConfiguredFeatures.SPIRIT_LOTUS), fruit(SPIRIT_LOTUS_CHUNKS_OVERWORLD));
		register(context, SPIRIT_LOTUS_UPPER_REALM, configured.getOrThrow(ModConfiguredFeatures.SPIRIT_LOTUS), fruit(SPIRIT_LOTUS_CHUNKS_UPPER_REALM));
		register(context, SPIRIT_DEW_GRASS, configured.getOrThrow(ModConfiguredFeatures.SPIRIT_DEW_GRASS), fruit(SPIRIT_DEW_GRASS_CHUNKS_OVERWORLD));
		register(context, SPIRIT_DEW_GRASS_UPPER_REALM, configured.getOrThrow(ModConfiguredFeatures.SPIRIT_DEW_GRASS), fruit(SPIRIT_DEW_GRASS_CHUNKS_UPPER_REALM));

		// Jade Ore: Overworld mountains from Y -48 to 160; every island tier in the Upper Realm, like its other ores.
		register(context, ORE_JADE, configured.getOrThrow(ModConfiguredFeatures.ORE_JADE), List.of(CountPlacement.of(JADE_VEINS_MOUNTAINS),
				InSquarePlacement.spread(), HeightRangePlacement.uniform(VerticalAnchor.absolute(-48), VerticalAnchor.absolute(160)), BiomeFilter.biome()));
		register(context, ORE_JADE_UPPER_REALM, configured.getOrThrow(ModConfiguredFeatures.ORE_JADE), ore(JADE_VEINS_UPPER_REALM, ISLAND_MIN_Y, ORE_MAX_Y));
		register(context, ORE_JADE_STONE, configured.getOrThrow(ModConfiguredFeatures.ORE_JADE_STONE), ore(JADE_STONE_BLOBS, ISLAND_MIN_Y, ISLAND_MAX_Y));

		// Blue Spirit Trees: placed like vanilla trees in the Overworld, on any island tier in the Upper Realm. They come after
		// the biome's own trees, so the Overworld spot is the ground under any canopy (a tree may grow up through leaves),
		// not the top of the leaves, where nearly every spot in a dense taiga would fail.
		register(context, BLUE_SPIRIT_TREE, configured.getOrThrow(ModConfiguredFeatures.BLUE_SPIRIT_TREE), List.of(
				RarityFilter.onAverageOnceEvery(BLUE_SPIRIT_TREE_CHUNKS_TAIGA), InSquarePlacement.spread(), SurfaceWaterDepthFilter.forMaxDepth(0),
				HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES), BiomeFilter.biome(), PlacementUtils.filteredByBlockSurvival(Blocks.SPRUCE_SAPLING)));
		register(context, BLUE_SPIRIT_TREE_UPPER_REALM, configured.getOrThrow(ModConfiguredFeatures.BLUE_SPIRIT_TREE),
				surface(RarityFilter.onAverageOnceEvery(BLUE_SPIRIT_TREE_CHUNKS_UPPER_REALM), Blocks.SPRUCE_SAPLING));

		// Qi Veins: vanilla diamond's placement in the Overworld (see ORE_DIAMOND in OrePlacements); island-wide above.
		register(context, QI_VEIN, configured.getOrThrow(ModConfiguredFeatures.QI_VEIN), List.of(CountPlacement.of(QI_VEIN_TRIES_OVERWORLD),
				InSquarePlacement.spread(), HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80)),
				BiomeFilter.biome()));
		register(context, QI_VEIN_UPPER_REALM, configured.getOrThrow(ModConfiguredFeatures.QI_VEIN), ore(QI_VEIN_TRIES_UPPER_REALM, ISLAND_MIN_Y, ORE_MAX_Y));
	}

	private static List<PlacementModifier> fruit(int chunks) {
		return List.of(RarityFilter.onAverageOnceEvery(chunks), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP, BiomeFilter.biome());
	}

	private static void register(BootstapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key,
			net.minecraft.core.Holder<ConfiguredFeature<?, ?>> feature, List<PlacementModifier> modifiers) {
		PlacementUtils.register(context, key, feature, modifiers);
	}

	private static List<PlacementModifier> ore(int count, int minY, int maxY) {
		return List.of(CountPlacement.of(count), InSquarePlacement.spread(),
				HeightRangePlacement.uniform(VerticalAnchor.absolute(minY), VerticalAnchor.absolute(maxY)), BiomeFilter.biome());
	}

	/** A random island top surface in the column, on any tier (see IslandSurfacePlacement). */
	private static List<PlacementModifier> surface(PlacementModifier count, Block survivalCheck) {
		List<PlacementModifier> modifiers = new ArrayList<>(List.of(
				count,
				InSquarePlacement.spread(),
				new IslandSurfacePlacement(ISLAND_MIN_Y, ISLAND_MAX_Y)));
		if (survivalCheck != null) {
			modifiers.add(PlacementUtils.filteredByBlockSurvival(survivalCheck));
		}
		modifiers.add(BiomeFilter.biome());
		return modifiers;
	}

	private static List<PlacementModifier> prepend(PlacementModifier first, List<PlacementModifier> rest) {
		List<PlacementModifier> modifiers = new ArrayList<>();
		modifiers.add(first);
		modifiers.addAll(rest);
		return modifiers;
	}

	private ModPlacedFeatures() {}
}
