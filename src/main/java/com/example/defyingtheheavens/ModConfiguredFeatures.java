package com.example.defyingtheheavens;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.TreePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.CherryFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.CherryTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;

/**
 * Upper Realm configured features. Ores are our own configurations so that valuable ores are half as likely to show on
 * exposed island faces (keeping cliffs and undersides clean) - see ModPlacedFeatures for counts and heights.
 */
public final class ModConfiguredFeatures {
	public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_COAL = key("ore_coal");
	public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_IRON = key("ore_iron");
	public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_COPPER = key("ore_copper");
	public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_GOLD = key("ore_gold");
	public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_REDSTONE = key("ore_redstone");
	public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_LAPIS = key("ore_lapis");
	public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_DIAMOND = key("ore_diamond");
	public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_DIAMOND_BURIED = key("ore_diamond_buried");
	public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_EMERALD = key("ore_emerald");
	public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_JADE = key("ore_jade");
	public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_QUARTZ = key("ore_quartz");
	public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_CLAY = key("ore_clay");
	public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_SAND = key("ore_sand");
	public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_GRAVEL = key("ore_gravel");
	public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_DIRT = key("ore_dirt");
	public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_GRANITE = key("ore_granite");
	public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_DIORITE = key("ore_diorite");
	public static final ResourceKey<ConfiguredFeature<?, ?>> ORE_ANDESITE = key("ore_andesite");

	public static final ResourceKey<ConfiguredFeature<?, ?>> SPIRIT_FOREST_TREES = key("spirit_forest_trees");
	public static final ResourceKey<ConfiguredFeature<?, ?>> WHITE_BLOSSOM_TREE = key("white_blossom_tree");
	public static final ResourceKey<ConfiguredFeature<?, ?>> PEACH_BLOSSOM_TREES = key("peach_blossom_trees");
	public static final ResourceKey<ConfiguredFeature<?, ?>> SPRING_BASIN = key("spring_basin");
	public static final ResourceKey<ConfiguredFeature<?, ?>> CULTIVATION_FRUIT = key("cultivation_fruit");
	public static final ResourceKey<ConfiguredFeature<?, ?>> GINSENG = key("ginseng");
	public static final ResourceKey<ConfiguredFeature<?, ?>> SPIRIT_GINSENG = key("spirit_ginseng");
	public static final ResourceKey<ConfiguredFeature<?, ?>> LINGZHI = key("lingzhi");
	public static final ResourceKey<ConfiguredFeature<?, ?>> PURPLE_LINGZHI = key("purple_lingzhi");
	public static final ResourceKey<ConfiguredFeature<?, ?>> HUANGJING = key("huangjing");
	public static final ResourceKey<ConfiguredFeature<?, ?>> OCHRE_HUANGJING = key("ochre_huangjing");
	public static final ResourceKey<ConfiguredFeature<?, ?>> SPIRIT_LOTUS = key("spirit_lotus");
	public static final ResourceKey<ConfiguredFeature<?, ?>> SPIRIT_DEW_GRASS = key("spirit_dew_grass");
	public static final ResourceKey<ConfiguredFeature<?, ?>> BLUE_SPIRIT_TREE = key("blue_spirit_tree");

	private static ResourceKey<ConfiguredFeature<?, ?>> key(String name) {
		return ResourceKey.create(Registries.CONFIGURED_FEATURE, DefyingTheHeavens.id(name));
	}

	public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> context) {
		HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);

		// Ores (sizes match vanilla; valuables discard 50% of air-exposed blocks).
		ore(context, ORE_COAL, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, 17, 0.5f);
		ore(context, ORE_IRON, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, 9, 0.5f);
		ore(context, ORE_COPPER, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, 10, 0.5f);
		ore(context, ORE_GOLD, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, 9, 0.5f);
		ore(context, ORE_REDSTONE, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, 8, 0.5f);
		ore(context, ORE_LAPIS, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE, 7, 0.5f);
		ore(context, ORE_DIAMOND, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, 4, 0.5f);
		ore(context, ORE_DIAMOND_BURIED, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, 8, 1.0f);
		ore(context, ORE_EMERALD, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE, 3, 0.5f);
		// Jade: small veins, in stone (and the Upper Realm's jade stone) or deepslate.
		ore(context, ORE_JADE, ModBlocks.JADE_ORE, ModBlocks.DEEPSLATE_JADE_ORE, 4, 0.0f);
		FeatureUtils.register(context, ORE_QUARTZ, Feature.ORE, new OreConfiguration(List.of(
				OreConfiguration.target(new BlockMatchTest(Blocks.BASALT), Blocks.NETHER_QUARTZ_ORE.defaultBlockState()),
				OreConfiguration.target(new BlockMatchTest(Blocks.BLACKSTONE), Blocks.NETHER_QUARTZ_ORE.defaultBlockState()),
				OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), Blocks.NETHER_QUARTZ_ORE.defaultBlockState())),
				12, 0.5f));

		// Fillers and soft materials (clay, sand for glass, gravel for flint, stone variants).
		filler(context, ORE_CLAY, Blocks.CLAY, 33);
		filler(context, ORE_SAND, Blocks.SAND, 24);
		filler(context, ORE_GRAVEL, Blocks.GRAVEL, 33);
		filler(context, ORE_DIRT, Blocks.DIRT, 33);
		filler(context, ORE_GRANITE, Blocks.GRANITE, 64);
		filler(context, ORE_DIORITE, Blocks.DIORITE, 64);
		filler(context, ORE_ANDESITE, Blocks.ANDESITE, 64);

		// Spirit Forest: mixed oak / birch / fancy oak.
		FeatureUtils.register(context, SPIRIT_FOREST_TREES, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(
				new WeightedPlacedFeature(placed.getOrThrow(TreePlacements.BIRCH_CHECKED), 0.3f),
				new WeightedPlacedFeature(placed.getOrThrow(TreePlacements.FANCY_OAK_CHECKED), 0.15f)),
				placed.getOrThrow(TreePlacements.OAK_CHECKED)));

		// Peach Blossom Sanctuary: vanilla pink cherry trees and a white-blossomed variant on the same cherry shape.
		Holder<ConfiguredFeature<?, ?>> whiteTree = context.register(WHITE_BLOSSOM_TREE, new ConfiguredFeature<>(Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
				BlockStateProvider.simple(Blocks.CHERRY_LOG),
				new CherryTrunkPlacer(7, 1, 0,
						new WeightedListInt(SimpleWeightedRandomList.<IntProvider>builder()
								.add(ConstantInt.of(1), 1).add(ConstantInt.of(2), 1).add(ConstantInt.of(3), 1).build()),
						UniformInt.of(2, 4), UniformInt.of(-4, -3), UniformInt.of(-1, 0)),
				BlockStateProvider.simple(ModBlocks.WHITE_BLOSSOM_LEAVES),
				new CherryFoliagePlacer(ConstantInt.of(4), ConstantInt.of(0), ConstantInt.of(5), 0.25f, 0.5f, 0.16666667f, 0.33333334f),
				new TwoLayersFeatureSize(1, 0, 2))
				.ignoreVines()
				.build()));
		FeatureUtils.register(context, PEACH_BLOSSOM_TREES, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(
				new WeightedPlacedFeature(PlacementUtils.inlinePlaced(whiteTree, PlacementUtils.filteredByBlockSurvival(Blocks.CHERRY_SAPLING)), 0.35f)),
				placed.getOrThrow(TreePlacements.CHERRY_BEES_005)));

		// Spring basins: small natural ponds dug into the plateaus (the lake feature refuses spots that would leak).
		@SuppressWarnings("deprecation")
		Feature<LakeFeature.Configuration> lake = Feature.LAKE;
		FeatureUtils.register(context, SPRING_BASIN, lake, new LakeFeature.Configuration(
				BlockStateProvider.simple(Blocks.WATER), BlockStateProvider.simple(Blocks.STONE)));

		// Wild Cultivation Fruit under tree canopies (Overworld and Upper Realm, added by ModFeatures#register).
		FeatureUtils.register(context, CULTIVATION_FRUIT, ModFeatures.CULTIVATION_FRUIT);
		FeatureUtils.register(context, GINSENG, ModFeatures.GINSENG);
		FeatureUtils.register(context, SPIRIT_GINSENG, ModFeatures.SPIRIT_GINSENG);
		FeatureUtils.register(context, LINGZHI, ModFeatures.LINGZHI);
		FeatureUtils.register(context, PURPLE_LINGZHI, ModFeatures.PURPLE_LINGZHI);
		FeatureUtils.register(context, HUANGJING, ModFeatures.HUANGJING);
		FeatureUtils.register(context, OCHRE_HUANGJING, ModFeatures.OCHRE_HUANGJING);
		FeatureUtils.register(context, SPIRIT_LOTUS, ModFeatures.SPIRIT_LOTUS);
		FeatureUtils.register(context, SPIRIT_DEW_GRASS, ModFeatures.SPIRIT_DEW_GRASS);

		// Blue Spirit Tree: a vanilla spruce, trunk and all, grown from Blue Spirit Log (spruce leaves stay on it because the
		// log is in the logs tag).
		FeatureUtils.register(context, BLUE_SPIRIT_TREE, Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
				BlockStateProvider.simple(ModBlocks.BLUE_SPIRIT_LOG),
				new StraightTrunkPlacer(5, 2, 1),
				BlockStateProvider.simple(Blocks.SPRUCE_LEAVES),
				new SpruceFoliagePlacer(UniformInt.of(2, 3), UniformInt.of(0, 2), UniformInt.of(1, 2)),
				new TwoLayersFeatureSize(2, 0, 2))
				.ignoreVines()
				.build());
	}

	private static void ore(BootstapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key,
			Block stoneOre, Block deepslateOre, int size, float discardOnAirExposure) {
		FeatureUtils.register(context, key, Feature.ORE, new OreConfiguration(List.of(
				OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), stoneOre.defaultBlockState()),
				OreConfiguration.target(new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES), deepslateOre.defaultBlockState())),
				size, discardOnAirExposure));
	}

	private static void filler(BootstapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key, Block block, int size) {
		FeatureUtils.register(context, key, Feature.ORE,
				new OreConfiguration(new TagMatchTest(BlockTags.BASE_STONE_OVERWORLD), block.defaultBlockState(), size));
	}

	private ModConfiguredFeatures() {}
}
