package com.example.defyingtheheavens;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.HashSet;
import java.util.Set;

/**
 * The Upper Realm biomes and the Spatial Gap's void biome. Biome placement (see dimension/upper_realm.json) uses
 * altitude: Dense Qi Peaks and Thunder Peaks crown everything above ~Y 325; Spirit Forest and Peach Blossom Sanctuary
 * share the lower tiers. The Ancient Sword Graveyard no longer generates; it stays registered only so chunks saved in it
 * by older versions still load cleanly.
 */
public final class ModBiomes {
	public static final ResourceKey<Biome> DENSE_QI_PEAKS = key("dense_qi_peaks");
	public static final ResourceKey<Biome> SPIRIT_FOREST = key("spirit_forest");
	public static final ResourceKey<Biome> PEACH_BLOSSOM_SANCTUARY = key("peach_blossom_sanctuary");
	public static final ResourceKey<Biome> THUNDER_PEAKS = key("thunder_peaks");
	public static final ResourceKey<Biome> ANCIENT_SWORD_GRAVEYARD = key("ancient_sword_graveyard");
	public static final ResourceKey<Biome> SPATIAL_GAP = key("spatial_gap");

	private static final int WATER = 0x3FC6E4;
	private static final int WATER_FOG = 0x0A3D52;

	private static ResourceKey<Biome> key(String name) {
		return ResourceKey.create(Registries.BIOME, DefyingTheHeavens.id(name));
	}

	public static void bootstrap(BootstapContext<Biome> context) {
		HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);
		HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);

		context.register(DENSE_QI_PEAKS, biome(false, 0.9f, 0.4f,
				effects(0x7FE0E0, 0xC9F2EA).grassColorOverride(0x6FD6A0).foliageColorOverride(0x4FBF86)
						.ambientParticle(new AmbientParticleSettings(ParticleTypes.END_ROD, 0.0015f)),
				spawns(true, false, mobs -> mobs.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.GOAT, 5, 1, 3))),
				features(placed, carvers, ModPlacedFeatures.ORE_EMERALD, ModPlacedFeatures.PEAK_SPRUCES, ModPlacedFeatures.PATCH_GRASS)));

		context.register(SPIRIT_FOREST, biome(true, 0.8f, 0.8f,
				effects(0x78C8FF, 0xD6EEFF),
				spawns(true, true, mobs -> mobs.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3))),
				features(placed, carvers, ModPlacedFeatures.ORE_CLAY, ModPlacedFeatures.SPIRIT_FOREST_TREES, ModPlacedFeatures.BAMBOO_GROVE,
						ModPlacedFeatures.FLOWERS_FOREST, ModPlacedFeatures.FLOWERS_DEFAULT, ModPlacedFeatures.PATCH_GRASS,
						ModPlacedFeatures.PATCH_SUGAR_CANE, ModPlacedFeatures.PATCH_PUMPKIN, ModPlacedFeatures.PATCH_BERRY_BUSH)));

		context.register(PEACH_BLOSSOM_SANCTUARY, biome(true, 0.7f, 0.8f,
				effects(0x96D2FF, 0xFFE4EE).grassColorOverride(0x8BD86A).foliageColorOverride(0x8BD86A)
						.ambientParticle(new AmbientParticleSettings(ParticleTypes.CHERRY_LEAVES, 0.004f)),
				spawns(true, true, mobs -> {}),
				features(placed, carvers, ModPlacedFeatures.SPRING_BASIN, ModPlacedFeatures.ORE_CLAY, ModPlacedFeatures.PEACH_BLOSSOM_TREES,
						ModPlacedFeatures.FLOWERS_CHERRY, ModPlacedFeatures.FLOWERS_FOREST, ModPlacedFeatures.PATCH_GRASS,
						ModPlacedFeatures.PATCH_SUGAR_CANE)));

		context.register(THUNDER_PEAKS, biome(true, 1.0f, 0.6f,
				effects(0x3A1D5C, 0x4B2C6E).ambientParticle(new AmbientParticleSettings(ParticleTypes.ELECTRIC_SPARK, 0.004f)),
				spawns(false, true, mobs -> {}),
				features(placed, carvers, ModPlacedFeatures.LAVA_LAKE, ModPlacedFeatures.ORE_QUARTZ)));

		context.register(ANCIENT_SWORD_GRAVEYARD, biome(false, 0.6f, 0.3f,
				effects(0x8FA6AB, 0xA5B9BC).grassColorOverride(0x8A9A86).foliageColorOverride(0x7A8A78)
						.ambientParticle(new AmbientParticleSettings(ParticleTypes.WHITE_ASH, 0.02f)),
				spawns(true, true, mobs -> {}),
				features(placed, carvers, ModPlacedFeatures.FOREST_ROCK, ModPlacedFeatures.ORE_CLAY, ModPlacedFeatures.FLOWERS_DEFAULT,
						ModPlacedFeatures.PATCH_GRASS, ModPlacedFeatures.PATCH_PUMPKIN)));

		context.register(SPATIAL_GAP, biome(false, 0.5f, 0.0f,
				new BiomeSpecialEffects.Builder().skyColor(0x000000).fogColor(0x02010A).waterColor(WATER).waterFogColor(WATER_FOG)
						.ambientParticle(new AmbientParticleSettings(ParticleTypes.GLOW, 0.01f)),
				new MobSpawnSettings.Builder().build(),
				new BiomeGenerationSettings.Builder(placed, carvers).build()));
	}

	private static Biome biome(boolean precipitation, float temperature, float downfall, BiomeSpecialEffects.Builder effects,
			MobSpawnSettings spawns, BiomeGenerationSettings generation) {
		return new Biome.BiomeBuilder()
				.hasPrecipitation(precipitation)
				.temperature(temperature)
				.downfall(downfall)
				.specialEffects(effects.build())
				.mobSpawnSettings(spawns)
				.generationSettings(generation)
				.build();
	}

	private static BiomeSpecialEffects.Builder effects(int sky, int fog) {
		return new BiomeSpecialEffects.Builder()
				.skyColor(sky)
				.fogColor(fog)
				.waterColor(WATER)
				.waterFogColor(WATER_FOG)
				.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS);
	}

	/** Farm animals for food, leather and wool; the usual monsters for string, bones, gunpowder and ender pearls. */
	private static MobSpawnSettings spawns(boolean animals, boolean monsters, java.util.function.Consumer<MobSpawnSettings.Builder> extra) {
		MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder();
		if (animals) BiomeDefaultFeatures.farmAnimals(builder);
		if (monsters) BiomeDefaultFeatures.commonSpawns(builder);
		extra.accept(builder);
		return builder.build();
	}

	@SafeVarargs
	private static BiomeGenerationSettings features(HolderGetter<PlacedFeature> placed, HolderGetter<ConfiguredWorldCarver<?>> carvers,
			ResourceKey<PlacedFeature>... specific) {
		Set<ResourceKey<PlacedFeature>> wanted = new HashSet<>(ModPlacedFeatures.COMMON);
		wanted.addAll(Set.of(specific));
		BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder(placed, carvers);
		ModPlacedFeatures.addFeatures(builder, wanted);
		return builder.build();
	}

	private ModBiomes() {}
}
