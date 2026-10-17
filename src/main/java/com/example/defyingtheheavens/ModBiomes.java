package com.example.defyingtheheavens;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
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
	public static final ResourceKey<Biome> INNER_REALM = key("inner_realm");
	/**
	 * The Netherworld (dimension/netherworld.json picks them from the terrain's own noises, see ModNoiseSettings#netherworld):
	 * the low golden riverlands of the Yellow Springs, the soul-soil meadows of the Fields of the Other Shore red with spider
	 * lilies, the Bone Wastes, and the crags of the Nine Nether Peaks, where ghostly qi is densest.
	 */
	public static final ResourceKey<Biome> YELLOW_SPRINGS = key("yellow_springs");
	public static final ResourceKey<Biome> FIELDS_OF_THE_OTHER_SHORE = key("fields_of_the_other_shore");
	public static final ResourceKey<Biome> BONE_WASTES = key("bone_wastes");
	public static final ResourceKey<Biome> NINE_NETHER_PEAKS = key("nine_nether_peaks");
	/** The Soul River's empty dark (the river itself is drawn by the client, see SoulRiver). */
	public static final ResourceKey<Biome> SOUL_RIVER = key("soul_river");
	/**
	 * The Astral Realm: space itself (the void between the stars), and the planets' lands the Overworld has none like (PlanetTerrain:
	 * the other planets wear the Overworld's own biomes): volcanic, ashen, crystal, and barren (no air: a black sky by day).
	 */
	public static final ResourceKey<Biome> ASTRAL_VOID = key("astral_void");
	public static final ResourceKey<Biome> ASTRAL_VOLCANIC = key("astral_volcanic");
	public static final ResourceKey<Biome> ASTRAL_ASHEN = key("astral_ashen");
	public static final ResourceKey<Biome> ASTRAL_CRYSTAL = key("astral_crystal");
	public static final ResourceKey<Biome> ASTRAL_BARREN = key("astral_barren");

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

		// The Inner Realm: nothing at all but the cultivator's own island and sky.
		context.register(INNER_REALM, biome(false, 0.5f, 0.0f,
				new BiomeSpecialEffects.Builder().skyColor(0x000000).fogColor(0x020108).waterColor(WATER).waterFogColor(WATER_FOG),
				new MobSpawnSettings.Builder().build(),
				new BiomeGenerationSettings.Builder(placed, carvers).build()));

		// The Netherworld: no rain or snow (custom dimensions share the Overworld's weather), the Soul Sand Valley's sounds and
		// music everywhere, and each biome its own fog, sky, water and foliage.
		// Hungry Ghosts in packs of 2-4 everywhere but the Soul River, most often in the Bone Wastes (HungryGhost#checkSpawn sets
		// how often a try comes to anything, by the biome's HungryGhost#spawnChance).
		context.register(YELLOW_SPRINGS, biome(false, 0.8f, 0.0f,
				// GLOW, not SOUL: it shines in the dark (a soul particle takes the dim light and comes out a dull brown face).
				netherworld(0x061A16, 0x1C2A16, 0xA6872C, 0x3A2C0C, 0x6B6A3A).ambientParticle(new AmbientParticleSettings(ParticleTypes.GLOW, 0.0012f)),
				hungryGhosts(),
				netherworldFeatures(placed, carvers, ModPlacedFeatures.NW_SPRINGS_POOL, ModPlacedFeatures.NW_LAKE_SPRINGS, ModPlacedFeatures.NW_SPRING_WATER,
						ModPlacedFeatures.NW_SPIDER_LILIES_SPARSE, ModPlacedFeatures.NW_NETHERWORLD_LOTUS, ModPlacedFeatures.NW_OTHER_SHORE_FLOWER,
						ModPlacedFeatures.NW_ORE_YIN_JADE)));
		context.register(FIELDS_OF_THE_OTHER_SHORE, biome(false, 0.8f, 0.0f,
				netherworld(0x081820, 0x0D2B2E, 0x2F6E6A, 0x0B2224, 0x4E5E4A).ambientParticle(new AmbientParticleSettings(ParticleTypes.GLOW, 0.0025f)),
				hungryGhosts(),
				netherworldFeatures(placed, carvers, ModPlacedFeatures.NW_LAKE_RARE, ModPlacedFeatures.NW_SPRING_WATER, ModPlacedFeatures.NW_DEAD_TREE_RARE,
						ModPlacedFeatures.NW_SPIDER_LILIES, ModPlacedFeatures.NW_CRIMSON_ROOTS, ModPlacedFeatures.NW_OTHER_SHORE_FLOWER_FIELDS,
						ModPlacedFeatures.NW_ORE_YIN_JADE)));
		context.register(BONE_WASTES, biome(false, 0.8f, 0.0f,
				netherworld(0x10140F, 0x2A2A24, 0x3E5E58, 0x101C1A, 0x6E6A5A).ambientParticle(new AmbientParticleSettings(ParticleTypes.WHITE_ASH, 0.04f)),
				hungryGhosts(),
				netherworldFeatures(placed, carvers, ModPlacedFeatures.NW_SPRING_WATER, ModPlacedFeatures.NW_BONE_PILE, ModPlacedFeatures.NW_SOUL_FIRE,
						ModPlacedFeatures.NW_DEAD_TREE, ModPlacedFeatures.NW_OTHER_SHORE_FLOWER, ModPlacedFeatures.NW_ORE_YIN_JADE)));
		context.register(NINE_NETHER_PEAKS, biome(false, 0.8f, 0.0f,
				netherworld(0x050F18, 0x0A1A24, 0x2A5C66, 0x081A20, 0x3E4A4A).ambientParticle(new AmbientParticleSettings(ParticleTypes.ASH, 0.01f)),
				hungryGhosts(),
				netherworldFeatures(placed, carvers, ModPlacedFeatures.NW_BLACKSTONE_BLOBS, ModPlacedFeatures.NW_BASALT_BLOBS,
						ModPlacedFeatures.NW_SPRING_WATER, ModPlacedFeatures.NW_ORE_YIN_JADE_PEAKS)));

		// The Soul River: a void with nothing in it but the river, drawn by the client; the Soul Sand Valley's whispering
		// ambience and its music.
		context.register(SOUL_RIVER, biome(false, 0.5f, 0.0f,
				netherworld(0x000000, 0x0C0A1F, WATER, WATER_FOG, 0x4E5E4A),
				new MobSpawnSettings.Builder().build(),
				new BiomeGenerationSettings.Builder(placed, carvers).build()));

		// The Astral Realm. Space: nothing in it at all.
		context.register(ASTRAL_VOID, biome(false, 0.5f, 0.0f,
				new BiomeSpecialEffects.Builder().skyColor(0x000000).fogColor(0x010105).waterColor(WATER).waterFogColor(WATER_FOG),
				new MobSpawnSettings.Builder().build(),
				new BiomeGenerationSettings.Builder(placed, carvers).build()));
		// The planets' own lands: the Overworld's ores and rock beneath them (in its own order, so they sit beside its biomes on one
		// planet), no beasts (the planets' wildlife is a question for later), each its own sky.
		context.register(ASTRAL_VOLCANIC, biome(false, 2.0f, 0.0f,
				new BiomeSpecialEffects.Builder().skyColor(0x6A3420).fogColor(0x5C3424).waterColor(0x3F5F66).waterFogColor(0x1A2628)
						.foliageColorOverride(0x5A5030).grassColorOverride(0x5A5030)
						.ambientParticle(new AmbientParticleSettings(ParticleTypes.ASH, 0.04f))
						.ambientLoopSound(SoundEvents.AMBIENT_BASALT_DELTAS_LOOP)
						.ambientMoodSound(new AmbientMoodSettings(SoundEvents.AMBIENT_BASALT_DELTAS_MOOD, 6000, 8, 2.0))
						.ambientAdditionsSound(new AmbientAdditionsSettings(SoundEvents.AMBIENT_BASALT_DELTAS_ADDITIONS, 0.0111)),
				new MobSpawnSettings.Builder().build(), astralFeatures(placed, carvers)));
		context.register(ASTRAL_ASHEN, biome(false, 1.2f, 0.0f,
				new BiomeSpecialEffects.Builder().skyColor(0xA29C92).fogColor(0xB4AEA4).waterColor(0x6E7A7C).waterFogColor(0x2A3032)
						.foliageColorOverride(0x7A7660).grassColorOverride(0x7A7660)
						.ambientParticle(new AmbientParticleSettings(ParticleTypes.WHITE_ASH, 0.05f))
						.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS),
				new MobSpawnSettings.Builder().build(), astralFeatures(placed, carvers)));
		context.register(ASTRAL_CRYSTAL, biome(false, 0.6f, 0.2f,
				new BiomeSpecialEffects.Builder().skyColor(0x8A78E2).fogColor(0xCDBEF4).waterColor(0x9C7CE8).waterFogColor(0x2C2050)
						.foliageColorOverride(0x9A88D0).grassColorOverride(0x9A88D0)
						.ambientParticle(new AmbientParticleSettings(ParticleTypes.END_ROD, 0.0015f))
						.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS),
				new MobSpawnSettings.Builder().build(), astralFeatures(placed, carvers)));
		context.register(ASTRAL_BARREN, biome(false, 0.5f, 0.0f,
				new BiomeSpecialEffects.Builder().skyColor(0x000000).fogColor(0x0C0C10).waterColor(WATER).waterFogColor(WATER_FOG)
						.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS),
				new MobSpawnSettings.Builder().build(), astralFeatures(placed, carvers)));
	}

	/**
	 * A planet land's features: the Overworld's lakes, geodes, dungeons, rock varieties, springs, freezing, ores and disks, added
	 * in the order its own biomes add them (one planet mixes these lands with the Overworld's biomes, and a step's features must
	 * keep one order across them all).
	 */
	private static BiomeGenerationSettings astralFeatures(HolderGetter<PlacedFeature> placed, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
		BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder(placed, carvers);
		BiomeDefaultFeatures.addDefaultCarversAndLakes(builder);
		BiomeDefaultFeatures.addDefaultCrystalFormations(builder);
		BiomeDefaultFeatures.addDefaultMonsterRoom(builder);
		BiomeDefaultFeatures.addDefaultUndergroundVariety(builder);
		BiomeDefaultFeatures.addDefaultSprings(builder);
		BiomeDefaultFeatures.addSurfaceFreezing(builder);
		BiomeDefaultFeatures.addDefaultOres(builder);
		BiomeDefaultFeatures.addDefaultSoftDisks(builder);
		return builder.build();
	}

	/** The Netherworld's monsters: Hungry Ghosts, in packs of two to four. */
	private static MobSpawnSettings hungryGhosts() {
		return new MobSpawnSettings.Builder()
				.addSpawn(net.minecraft.world.entity.MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntities.HUNGRY_GHOST, 10, 2, 4))
				.build();
	}

	/** A Netherworld biome's colours, with the Soul Sand Valley's ambience and music. */
	private static BiomeSpecialEffects.Builder netherworld(int sky, int fog, int water, int waterFog, int foliage) {
		return new BiomeSpecialEffects.Builder()
				.skyColor(sky)
				.fogColor(fog)
				.waterColor(water)
				.waterFogColor(waterFog)
				.foliageColorOverride(foliage)
				.grassColorOverride(foliage)
				.ambientLoopSound(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_LOOP)
				.ambientMoodSound(new AmbientMoodSettings(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD, 6000, 8, 2.0))
				.ambientAdditionsSound(new AmbientAdditionsSettings(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, 0.0111))
				.backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_SOUL_SAND_VALLEY));
	}

	/**
	 * A Netherworld biome's features (none of the Upper Realm's common ores: the dead's realm keeps its own riches), and in every
	 * one the Method Steles.
	 */
	@SafeVarargs
	private static BiomeGenerationSettings netherworldFeatures(HolderGetter<PlacedFeature> placed, HolderGetter<ConfiguredWorldCarver<?>> carvers,
			ResourceKey<PlacedFeature>... specific) {
		BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder(placed, carvers);
		Set<ResourceKey<PlacedFeature>> features = new java.util.HashSet<>(Set.of(specific));
		features.add(ModPlacedFeatures.NW_METHOD_STELE);
		ModPlacedFeatures.addFeatures(builder, features);
		return builder.build();
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
