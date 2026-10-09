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
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.NoiseBasedCountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

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
