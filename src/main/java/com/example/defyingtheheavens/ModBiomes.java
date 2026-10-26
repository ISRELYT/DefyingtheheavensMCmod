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
	/**
	 * The Great Void Realm (2026-10-25; dimension/great_void.json picks them by the terrain's own noises, see
	 * ModNoiseSettings#greatVoid): the Void Steppes, silvered grass under the stars; the five Dao fields, each where one element's law
	 * runs strong (its qi thick, so its methods cultivate the faster: QiElement; its spells the stronger and the element it overcomes
	 * the weaker there: DaoFields); the old battlefields of the war between Heaven and the demons; and the Shore of Nirvana about the
	 * world's 0, 0, a pale coast onto a sea of open void, where the Entering Nirvana wait for the Immortal Gate.
	 */
	public static final ResourceKey<Biome> VOID_STEPPES = key("void_steppes");
	public static final ResourceKey<Biome> DAO_FIELD_WOOD = key("dao_field_wood");
	public static final ResourceKey<Biome> DAO_FIELD_FIRE = key("dao_field_fire");
	public static final ResourceKey<Biome> DAO_FIELD_EARTH = key("dao_field_earth");
	public static final ResourceKey<Biome> DAO_FIELD_METAL = key("dao_field_metal");
	public static final ResourceKey<Biome> DAO_FIELD_WATER = key("dao_field_water");
	public static final ResourceKey<Biome> ANCIENT_BATTLEFIELD = key("ancient_battlefield");
	public static final ResourceKey<Biome> SHORE_OF_NIRVANA = key("shore_of_nirvana");
	/** The Void Breach's shattered space (VoidBreach): nothing in it but the shards the client draws. */
	public static final ResourceKey<Biome> VOID_BREACH = key("void_breach");
	/**
	 * A Demon Palace's blood ring (DemonPalacePieces#pool sets the land round it to this): its water red, as the battlefields' runs, and
	 * nothing else of its own. Never in the Great Void's biome source, so worldgen lays nothing of a land's there: made the battlefields'
	 * own, the ring's land took their ruins and blood pools, an elder rising in the palace (tour 12).
	 */
	public static final ResourceKey<Biome> BLOOD_RING = key("blood_ring");
	/**
	 * The Immortal Realm (phase 2, 2026-10-25; dimension/immortal_realm.json picks them by the terrain's own noises, see
	 * ModNoiseSettings#immortalRealm): the Jade Peaks, pines and bamboo on their terraces, their cliffs banded with jade and running
	 * with waterfalls; the Immortal Gardens, lawns and peach blossom on the broad levels (the Southern Heavenly Gate's ground among them);
	 * the Celestial Heights, the white jade of the summits above about Y 320. The Gate's passage: nothing in it but the light.
	 */
	public static final ResourceKey<Biome> JADE_PEAKS = key("jade_peaks");
	public static final ResourceKey<Biome> IMMORTAL_GARDENS = key("immortal_gardens");
	public static final ResourceKey<Biome> CELESTIAL_HEIGHTS = key("celestial_heights");
	/** The main island's level ground, where the capital stands (ImmortalCapital): lawns, nothing grown that would stand in its streets. */
	public static final ResourceKey<Biome> JADE_CAPITAL = key("jade_capital");
	public static final ResourceKey<Biome> IMMORTAL_PASSAGE = key("immortal_passage");
	/**
	 * The Chaos Frontier (phase 3c, 2026-10-26), the Immortal Realm's edge past some 4,150 blocks out (ModNoiseSettings#FRONTIER_FROM; the
	 * dimension's biome source picks it by the terrain's "erosion", the Frontier's share): shards of tuff and deepslate veined with amethyst
	 * and crying obsidian, Chaos Origin Ore in them; nothing grows or lives there of its own (the Chaos Fiends are ChaosFrontier's). Never in
	 * the realm's tag (ModTags#IS_IMMORTAL_REALM), so none of the Jade Firmament's herbs, ores or settlements come to it.
	 */
	public static final ResourceKey<Biome> CHAOS_FRONTIER = key("chaos_frontier");
	/**
	 * The small worlds (phase 3d, 2026-10-26; SmallWorlds): an immortal's own island under the stars, its grass and leaves the Immortal
	 * Realm's lush green, its water jade-clear; no rain, nothing born of it, nothing laid by worldgen (SmallWorlds lays the island).
	 */
	public static final ResourceKey<Biome> SMALL_WORLD = key("small_world");

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

		// The Great Void. No rain or snow under the stars; farm beasts on the steppes and in the wood. Its herbs, spirit stone, jade, qi
		// veins and Cultivation Fruit are the Jade Firmament's, and its Dao Steles (everywhere but the shore: DaoSteleFeature carves them
		// for the Great Void's realms), all added by its tag (ModTags#IS_GREAT_VOID, ModFeatures#register); here each land's own.
		context.register(VOID_STEPPES, biome(false, 0.6f, 0.3f,
				greatVoid(0x9DBC9E, 0x80A88C, 0x5A70D0, 0x1A2050).ambientParticle(new AmbientParticleSettings(ParticleTypes.END_ROD, 0.0006f)),
				spawns(true, false, mobs -> {}),
				greatVoidFeatures(placed, carvers, ModPlacedFeatures.SPRING_BASIN, ModPlacedFeatures.FOREST_ROCK, ModPlacedFeatures.PEAK_SPRUCES,
						ModPlacedFeatures.FLOWERS_DEFAULT, ModPlacedFeatures.PATCH_GRASS)));
		context.register(DAO_FIELD_WOOD, biome(false, 0.8f, 0.8f,
				greatVoid(0x4FCB6A, 0x3BAA55, 0x3FC6A4, 0x0A3D30).ambientParticle(new AmbientParticleSettings(ParticleTypes.SPORE_BLOSSOM_AIR, 0.0025f)),
				spawns(true, false, mobs -> mobs.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3))),
				greatVoidFeatures(placed, carvers, ModPlacedFeatures.SPIRIT_FOREST_TREES, ModPlacedFeatures.BAMBOO_GROVE,
						ModPlacedFeatures.FLOWERS_FOREST, ModPlacedFeatures.PATCH_GRASS, ModPlacedFeatures.PATCH_BERRY_BUSH)));
		context.register(DAO_FIELD_FIRE, biome(false, 2.0f, 0.0f,
				greatVoid(0xB0603A, 0xA04A2A, 0x6A5A50, 0x2A1A14).ambientParticle(new AmbientParticleSettings(ParticleTypes.ASH, 0.012f)),
				spawns(false, false, mobs -> {}),
				greatVoidFeatures(placed, carvers, ModPlacedFeatures.LAVA_LAKE, ModPlacedFeatures.ORE_QUARTZ)));
		context.register(DAO_FIELD_EARTH, biome(false, 1.2f, 0.2f,
				greatVoid(0xC8A050, 0xB08A40, 0x6A7A5A, 0x2A2A18).ambientParticle(new AmbientParticleSettings(ParticleTypes.MYCELIUM, 0.006f)),
				spawns(false, false, mobs -> {}),
				greatVoidFeatures(placed, carvers, ModPlacedFeatures.FOREST_ROCK)));
		context.register(DAO_FIELD_METAL, biome(false, 0.4f, 0.1f,
				greatVoid(0xB8C0C8, 0xA8B0B8, 0x8AA0C0, 0x283040).ambientParticle(new AmbientParticleSettings(ParticleTypes.ELECTRIC_SPARK, 0.0025f)),
				spawns(false, false, mobs -> {}),
				greatVoidFeatures(placed, carvers, ModPlacedFeatures.ORE_EMERALD)));
		context.register(DAO_FIELD_WATER, biome(false, 0.5f, 0.9f,
				greatVoid(0x5AB8B0, 0x48A0A0, 0x3F9FE4, 0x0A2E52).ambientParticle(new AmbientParticleSettings(ParticleTypes.GLOW, 0.0015f)),
				spawns(false, false, mobs -> {}),
				greatVoidFeatures(placed, carvers, ModPlacedFeatures.SPRING_BASIN, ModPlacedFeatures.PATCH_SUGAR_CANE,
						ModPlacedFeatures.FLOWERS_DEFAULT, ModPlacedFeatures.PATCH_GRASS)));
		// Its water runs red: the blood of the old war never dried there (its pools: BloodPoolFeature).
		context.register(ANCIENT_BATTLEFIELD, biome(false, 0.6f, 0.0f,
				greatVoid(0x6E6458, 0x5E5448, 0x8A1414, 0x2A0606).ambientParticle(new AmbientParticleSettings(ParticleTypes.WHITE_ASH, 0.02f)),
				spawns(false, false, mobs -> {}),
				greatVoidFeatures(placed, carvers, ModPlacedFeatures.FOREST_ROCK)));
		context.register(SHORE_OF_NIRVANA, biome(false, 0.7f, 0.4f,
				greatVoid(0xB8D8C8, 0xA0C8B8, 0x7FD0E8, 0x103040).ambientParticle(new AmbientParticleSettings(ParticleTypes.END_ROD, 0.002f)),
				spawns(false, false, mobs -> {}),
				greatVoidFeatures(placed, carvers, ModPlacedFeatures.PATCH_GRASS)));
		// A Demon Palace's blood ring: the steppes' colours but for its water, the battlefields' red; nothing grows or lives there of its own.
		context.register(BLOOD_RING, biome(false, 0.6f, 0.0f,
				greatVoid(0x9DBC9E, 0x80A88C, 0x8A1414, 0x2A0606),
				new MobSpawnSettings.Builder().build(),
				new BiomeGenerationSettings.Builder(placed, carvers).build()));
		// The Void Breach: broken space, nothing in it (the shards are the client's).
		context.register(VOID_BREACH, biome(false, 0.5f, 0.0f,
				new BiomeSpecialEffects.Builder().skyColor(0x000000).fogColor(0x0A0418).waterColor(WATER).waterFogColor(WATER_FOG)
						.ambientParticle(new AmbientParticleSettings(ParticleTypes.REVERSE_PORTAL, 0.01f)),
				new MobSpawnSettings.Builder().build(),
				new BiomeGenerationSettings.Builder(placed, carvers).build()));

		// The Immortal Realm. No rain or snow above the clouds, no monsters; goats on the peaks, farm beasts in the gardens. Its herbs,
		// spirit stone, jade, qi veins and Cultivation Fruit are the Jade Firmament's, added by its tag (ModTags#IS_IMMORTAL_REALM,
		// ModFeatures#register), but no stele stands there (the user's rule, 2026-10-25); here each land's own, and the springs' waterfalls.
		context.register(JADE_PEAKS, biome(false, 0.7f, 0.6f,
				immortal(0x6FCB78, 0x4FB86A).ambientParticle(new AmbientParticleSettings(ParticleTypes.END_ROD, 0.0008f)),
				spawns(false, false, mobs -> mobs.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.GOAT, 5, 1, 3))),
				immortalFeatures(placed, carvers, ModPlacedFeatures.PEAK_SPRUCES, ModPlacedFeatures.BAMBOO_GROVE, ModPlacedFeatures.FLOWERS_DEFAULT,
						ModPlacedFeatures.PATCH_GRASS)));
		// Its petals drift sparely, and no ponds pock its lawns (tour 2: a pond every other chunk, petals thick in the air).
		context.register(IMMORTAL_GARDENS, biome(false, 0.7f, 0.8f,
				immortal(0x86D86A, 0x86D86A).ambientParticle(new AmbientParticleSettings(ParticleTypes.CHERRY_LEAVES, 0.0012f)),
				spawns(true, false, mobs -> {}),
				immortalFeatures(placed, carvers, ModPlacedFeatures.ORE_CLAY, ModPlacedFeatures.IMMORTAL_BLOSSOMS,
						ModPlacedFeatures.FLOWERS_CHERRY, ModPlacedFeatures.FLOWERS_FOREST, ModPlacedFeatures.PATCH_GRASS, ModPlacedFeatures.PATCH_SUGAR_CANE)));
		// The capital's ground: lawns and flowers, no tree, pond or spring to stand in its streets (the capital is laid on it, ImmortalCapital).
		context.register(JADE_CAPITAL, biome(false, 0.7f, 0.6f,
				immortal(0x86D86A, 0x7CC860).ambientParticle(new AmbientParticleSettings(ParticleTypes.END_ROD, 0.0006f)),
				spawns(false, false, mobs -> {}),
				capitalFeatures(placed, carvers)));
		context.register(CELESTIAL_HEIGHTS, biome(false, 0.5f, 0.3f,
				immortal(0x9AD6A0, 0x86C890).ambientParticle(new AmbientParticleSettings(ParticleTypes.END_ROD, 0.0016f)),
				spawns(false, false, mobs -> {}),
				immortalFeatures(placed, carvers, ModPlacedFeatures.ORE_EMERALD, ModPlacedFeatures.PATCH_GRASS)));
		// The Gate's passage: light, nothing in it (the client draws the way).
		context.register(IMMORTAL_PASSAGE, biome(false, 0.5f, 0.0f,
				new BiomeSpecialEffects.Builder().skyColor(0xFFF2C8).fogColor(0xFFE8B0).waterColor(WATER).waterFogColor(WATER_FOG)
						.ambientParticle(new AmbientParticleSettings(ParticleTypes.END_ROD, 0.01f)),
				new MobSpawnSettings.Builder().build(),
				new BiomeGenerationSettings.Builder(placed, carvers).build()));
		// The Chaos Frontier: its sky and fog bruised violet (the client's, ImmortalRealmClient), motes of chaos drifting; its ore and nothing else.
		Set<ResourceKey<PlacedFeature>> chaos = Set.of(ModPlacedFeatures.ORE_CHAOS_ORIGIN);
		BiomeGenerationSettings.Builder chaosFeatures = new BiomeGenerationSettings.Builder(placed, carvers);
		ModPlacedFeatures.addFeatures(chaosFeatures, chaos);
		context.register(CHAOS_FRONTIER, biome(false, 0.5f, 0.0f,
				new BiomeSpecialEffects.Builder().skyColor(0x2E2238).fogColor(0x4A3A5A).waterColor(0x6A5A8A).waterFogColor(0x1A1428)
						.ambientParticle(new AmbientParticleSettings(ParticleTypes.REVERSE_PORTAL, 0.006f))
						.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS),
				new MobSpawnSettings.Builder().build(),
				chaosFeatures.build()));
		// The small worlds: the Immortal Realm's greens under a starry sky (the client's), motes of starlight drifting; nothing else.
		context.register(SMALL_WORLD, biome(false, 0.7f, 0.6f,
				new BiomeSpecialEffects.Builder().skyColor(0x0A0C24).fogColor(0x1A1E44).waterColor(0x4FD8D0).waterFogColor(0x14524E)
						.grassColorOverride(0x6FCB78).foliageColorOverride(0x4FB86A)
						.ambientParticle(new AmbientParticleSettings(ParticleTypes.END_ROD, 0.0006f)),
				new MobSpawnSettings.Builder().build(),
				new BiomeGenerationSettings.Builder(placed, carvers).build()));
	}

	/**
	 * An Immortal Realm land's colours: its grass and foliage, its water jade-clear; the sky is the client's (golden), and the fog the
	 * sky's gold at the horizon, so the far peaks and the sea of clouds fade into it.
	 */
	private static BiomeSpecialEffects.Builder immortal(int grass, int foliage) {
		return new BiomeSpecialEffects.Builder()
				.skyColor(0xF0D89A)
				.fogColor(0xF4DCA4)
				.waterColor(0x4FD8D0)
				.waterFogColor(0x14524E)
				.grassColorOverride(grass)
				.foliageColorOverride(foliage)
				.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS);
	}

	/** The capital's ground's features: the common ores deep under it, and its grass; no springs. */
	private static BiomeGenerationSettings capitalFeatures(HolderGetter<PlacedFeature> placed, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
		Set<ResourceKey<PlacedFeature>> wanted = new HashSet<>(ModPlacedFeatures.COMMON);
		wanted.remove(ModPlacedFeatures.SPRING_LAVA);
		wanted.remove(ModPlacedFeatures.SPRING_WATER);
		wanted.add(ModPlacedFeatures.PATCH_GRASS);
		BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder(placed, carvers);
		ModPlacedFeatures.addFeatures(builder, wanted);
		return builder.build();
	}

	/**
	 * An Immortal Realm land's features: the Jade Firmament's common ores and its water springs (whose waterfalls spill off the cliffs into
	 * the clouds), never its lava; and the land's own (the rest come by its tag: ModFeatures).
	 */
	@SafeVarargs
	private static BiomeGenerationSettings immortalFeatures(HolderGetter<PlacedFeature> placed, HolderGetter<ConfiguredWorldCarver<?>> carvers,
			ResourceKey<PlacedFeature>... specific) {
		Set<ResourceKey<PlacedFeature>> wanted = new HashSet<>(ModPlacedFeatures.COMMON);
		wanted.remove(ModPlacedFeatures.SPRING_LAVA);
		wanted.addAll(Set.of(specific));
		BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder(placed, carvers);
		ModPlacedFeatures.addFeatures(builder, wanted);
		return builder.build();
	}

	/** A Great Void land's colours: its grass, its foliage and its water's; the sky is all stars (drawn by the client) and the fog violet. */
	private static BiomeSpecialEffects.Builder greatVoid(int grass, int foliage, int water, int waterFog) {
		return new BiomeSpecialEffects.Builder()
				.skyColor(0x000000)
				.fogColor(0x1A1030)
				.waterColor(water)
				.waterFogColor(waterFog)
				.grassColorOverride(grass)
				.foliageColorOverride(foliage)
				.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS);
	}

	/** A Great Void land's features: the Jade Firmament's common ores and the land's own (the rest come by its tag: ModFeatures). */
	@SafeVarargs
	private static BiomeGenerationSettings greatVoidFeatures(HolderGetter<PlacedFeature> placed, HolderGetter<ConfiguredWorldCarver<?>> carvers,
			ResourceKey<PlacedFeature>... specific) {
		return features(placed, carvers, specific);
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
