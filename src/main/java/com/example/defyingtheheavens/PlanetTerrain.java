package com.example.defyingtheheavens;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A planet's land in the Astral Realm's surfaces (ModDimensions#ASTRAL_SURFACES): how high it lies at every point, its seas and
 * rivers, the biome there and the ground it is made of, all from the planet's own seed within its {@link AstralPlanets.Kind}
 * (PlanetSurfaceGenerator lays the blocks, PlanetBiomeSource names the biomes).
 * <p>
 * The land is the planet's map: {@link AstralPlanets#LAND_RADIUS} east and west of its middle is once round its equator, and
 * {@link AstralPlanets#LAND_POLE} north and south reaches its poles ({@link #lon}, {@link #lat}). Continents, seas, mountain belts,
 * the climate (warm at the equator, ice toward the poles) and the great craters and volcanoes come from noise on the sphere itself,
 * so the client's painter (PlanetPainter) draws the planet seen from space from these same numbers, and one who flies down onto a
 * green continent lands on it. Below what space can show, hills, rivers, dunes, terraces, small craters and cones and crystal
 * spires come from the land's own block coordinates.
 * <p>
 * Each kind sets the ranges its planets' numbers are drawn from (how much of it is sea, how high its land, how rough, how warm and
 * wet), so planets of one kind still differ, and a few verdant ones come out much like the Overworld.
 */
public final class PlanetTerrain {
	public static final int SEA_LEVEL = 63;
	/** The surfaces' floor and height (ModDimensions#ASTRAL_SURFACES): -64 to 319, as the Overworld's. */
	public static final int MIN_Y = -64, HEIGHT = 384, TOP_Y = MIN_Y + HEIGHT - 1;
	/** Ground never rises past this: room above it to fly, below the way back out to space (AstralRealm#EXIT_Y). */
	public static final int MAX_GROUND = 290;
	/** Blocks of land to a unit of the planet's sphere (at its equator; the same both ways). */
	public static final double BLOCKS_PER_UNIT = AstralPlanets.LAND_RADIUS / Math.PI;

	/** What fills a planet's low places: water, lava, or nothing. */
	public enum Fluid { WATER, LAVA, NONE }

	/** The blocks a zone's ground is made of: its top, what lies under it, its floor under water, and its bare cliffs. */
	public enum Ground {
		GRASS(Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.SAND, Blocks.STONE),
		PODZOL(Blocks.PODZOL, Blocks.DIRT, Blocks.GRAVEL, Blocks.STONE),
		COARSE(Blocks.COARSE_DIRT, Blocks.DIRT, Blocks.GRAVEL, Blocks.TERRACOTTA),
		SNOW(Blocks.SNOW_BLOCK, Blocks.DIRT, Blocks.GRAVEL, Blocks.STONE),
		ICE(Blocks.SNOW_BLOCK, Blocks.PACKED_ICE, Blocks.GRAVEL, Blocks.PACKED_ICE),
		SAND(Blocks.SAND, Blocks.SANDSTONE, Blocks.SAND, Blocks.SANDSTONE),
		BADLANDS(Blocks.RED_SAND, Blocks.TERRACOTTA, Blocks.RED_SAND, Blocks.TERRACOTTA),
		MYCELIUM(Blocks.MYCELIUM, Blocks.DIRT, Blocks.GRAVEL, Blocks.STONE),
		MUD(Blocks.MUD, Blocks.MUD, Blocks.MUD, Blocks.STONE),
		SWAMP(Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.CLAY, Blocks.STONE),
		STONE(Blocks.STONE, Blocks.STONE, Blocks.GRAVEL, Blocks.STONE),
		SHORE(Blocks.SAND, Blocks.SAND, Blocks.SAND, Blocks.STONE),
		RIVERBED(Blocks.SAND, Blocks.SAND, Blocks.SAND, Blocks.STONE),
		SEABED(Blocks.SAND, Blocks.SAND, Blocks.SAND, Blocks.STONE),
		DEEP_SEABED(Blocks.GRAVEL, Blocks.GRAVEL, Blocks.GRAVEL, Blocks.STONE),
		BASALT(Blocks.BASALT, Blocks.BLACKSTONE, Blocks.BASALT, Blocks.BLACKSTONE),
		SCORIA(Blocks.BLACKSTONE, Blocks.BASALT, Blocks.MAGMA_BLOCK, Blocks.BLACKSTONE),
		ASH(Blocks.TUFF, Blocks.TUFF, Blocks.GRAVEL, Blocks.TUFF),
		ASH_GRAVEL(Blocks.GRAVEL, Blocks.TUFF, Blocks.GRAVEL, Blocks.TUFF),
		CRYSTAL(Blocks.CALCITE, Blocks.CALCITE, Blocks.CALCITE, Blocks.SMOOTH_BASALT),
		REGOLITH(Blocks.ANDESITE, Blocks.STONE, Blocks.GRAVEL, Blocks.STONE),
		DARK_REGOLITH(Blocks.SMOOTH_BASALT, Blocks.DEEPSLATE, Blocks.SMOOTH_BASALT, Blocks.DEEPSLATE);

		public final Block top, filler, floor, cliff;

		Ground(Block top, Block filler, Block floor, Block cliff) {
			this.top = top;
			this.filler = filler;
			this.floor = floor;
			this.cliff = cliff;
		}
	}

	/**
	 * What lies at a point of a planet's land: its biome (the Overworld's own, or the Astral Realm's for the lands the Overworld
	 * has none like), its ground, and its colour seen from space (0xRRGGBB, PlanetPainter).
	 */
	public enum Zone {
		PLAINS(Biomes.PLAINS, Ground.GRASS, 0x7FAE4E),
		SUNFLOWER_PLAINS(Biomes.SUNFLOWER_PLAINS, Ground.GRASS, 0x8DB64F),
		FOREST(Biomes.FOREST, Ground.GRASS, 0x3D6B27),
		FLOWER_FOREST(Biomes.FLOWER_FOREST, Ground.GRASS, 0x5A8836),
		BIRCH_FOREST(Biomes.BIRCH_FOREST, Ground.GRASS, 0x5C8D3B),
		OLD_GROWTH_BIRCH_FOREST(Biomes.OLD_GROWTH_BIRCH_FOREST, Ground.GRASS, 0x528338),
		DARK_FOREST(Biomes.DARK_FOREST, Ground.GRASS, 0x243F18),
		MEADOW(Biomes.MEADOW, Ground.GRASS, 0x86B666),
		CHERRY_GROVE(Biomes.CHERRY_GROVE, Ground.GRASS, 0xE4A2C4),
		WINDSWEPT_HILLS(Biomes.WINDSWEPT_HILLS, Ground.GRASS, 0x75836C),
		WINDSWEPT_FOREST(Biomes.WINDSWEPT_FOREST, Ground.GRASS, 0x486740),
		GROVE(Biomes.GROVE, Ground.SNOW, 0xD2DDE2),
		SNOWY_SLOPES(Biomes.SNOWY_SLOPES, Ground.SNOW, 0xECF1F5),
		FROZEN_PEAKS(Biomes.FROZEN_PEAKS, Ground.ICE, 0xE2ECF6),
		JAGGED_PEAKS(Biomes.JAGGED_PEAKS, Ground.SNOW, 0xE7EBEF),
		STONY_PEAKS(Biomes.STONY_PEAKS, Ground.STONE, 0x898985),
		SNOWY_PLAINS(Biomes.SNOWY_PLAINS, Ground.GRASS, 0xEEF2F4),
		ICE_SPIKES(Biomes.ICE_SPIKES, Ground.SNOW, 0xDAE8F4),
		SNOWY_TAIGA(Biomes.SNOWY_TAIGA, Ground.GRASS, 0xC6D2CC),
		TAIGA(Biomes.TAIGA, Ground.GRASS, 0x3B6348),
		OLD_GROWTH_PINE_TAIGA(Biomes.OLD_GROWTH_PINE_TAIGA, Ground.PODZOL, 0x4A5B36),
		OLD_GROWTH_SPRUCE_TAIGA(Biomes.OLD_GROWTH_SPRUCE_TAIGA, Ground.PODZOL, 0x3A5432),
		DESERT(Biomes.DESERT, Ground.SAND, 0xD9C78A),
		BADLANDS(Biomes.BADLANDS, Ground.BADLANDS, 0xBE6438),
		ERODED_BADLANDS(Biomes.ERODED_BADLANDS, Ground.BADLANDS, 0xB05C34),
		WOODED_BADLANDS(Biomes.WOODED_BADLANDS, Ground.COARSE, 0x8A7945),
		SAVANNA(Biomes.SAVANNA, Ground.GRASS, 0xA4A055),
		SAVANNA_PLATEAU(Biomes.SAVANNA_PLATEAU, Ground.GRASS, 0x9A9850),
		WINDSWEPT_SAVANNA(Biomes.WINDSWEPT_SAVANNA, Ground.COARSE, 0x989360),
		JUNGLE(Biomes.JUNGLE, Ground.GRASS, 0x2A781A),
		SPARSE_JUNGLE(Biomes.SPARSE_JUNGLE, Ground.GRASS, 0x4C8A2A),
		BAMBOO_JUNGLE(Biomes.BAMBOO_JUNGLE, Ground.PODZOL, 0x3C8A22),
		SWAMP(Biomes.SWAMP, Ground.SWAMP, 0x485936),
		MANGROVE_SWAMP(Biomes.MANGROVE_SWAMP, Ground.MUD, 0x4B562F),
		MUSHROOM_FIELDS(Biomes.MUSHROOM_FIELDS, Ground.MYCELIUM, 0x8D7A90),
		BEACH(Biomes.BEACH, Ground.SHORE, 0xE0D4A0),
		SNOWY_BEACH(Biomes.SNOWY_BEACH, Ground.SHORE, 0xE7E4D6),
		STONY_SHORE(Biomes.STONY_SHORE, Ground.STONE, 0x888888),
		RIVER(Biomes.RIVER, Ground.RIVERBED, 0x396CD0),
		FROZEN_RIVER(Biomes.FROZEN_RIVER, Ground.RIVERBED, 0xB0CAE4),
		OCEAN(Biomes.OCEAN, Ground.SEABED, 0x1F3E8A),
		DEEP_OCEAN(Biomes.DEEP_OCEAN, Ground.DEEP_SEABED, 0x142C66),
		WARM_OCEAN(Biomes.WARM_OCEAN, Ground.SEABED, 0x1D6896),
		LUKEWARM_OCEAN(Biomes.LUKEWARM_OCEAN, Ground.SEABED, 0x1D5490),
		DEEP_LUKEWARM_OCEAN(Biomes.DEEP_LUKEWARM_OCEAN, Ground.DEEP_SEABED, 0x16427A),
		COLD_OCEAN(Biomes.COLD_OCEAN, Ground.DEEP_SEABED, 0x203A78),
		DEEP_COLD_OCEAN(Biomes.DEEP_COLD_OCEAN, Ground.DEEP_SEABED, 0x16295C),
		FROZEN_OCEAN(Biomes.FROZEN_OCEAN, Ground.DEEP_SEABED, 0xC2D8EA),
		DEEP_FROZEN_OCEAN(Biomes.DEEP_FROZEN_OCEAN, Ground.DEEP_SEABED, 0xADC8E0),
		VOLCANIC_PLAINS(ModBiomes.ASTRAL_VOLCANIC, Ground.BASALT, 0x39322F),
		VOLCANIC_PEAKS(ModBiomes.ASTRAL_VOLCANIC, Ground.SCORIA, 0x27201D),
		LAVA_SEA(ModBiomes.ASTRAL_VOLCANIC, Ground.BASALT, 0xEE681C),
		ASH_PLAINS(ModBiomes.ASTRAL_ASHEN, Ground.ASH, 0x8A8782),
		ASH_DUNES(ModBiomes.ASTRAL_ASHEN, Ground.ASH_GRAVEL, 0x9A9691),
		CRYSTAL_FIELDS(ModBiomes.ASTRAL_CRYSTAL, Ground.CRYSTAL, 0xC9B6E8),
		REGOLITH(ModBiomes.ASTRAL_BARREN, Ground.REGOLITH, 0x989895),
		DARK_REGOLITH(ModBiomes.ASTRAL_BARREN, Ground.DARK_REGOLITH, 0x545459);

		public final ResourceKey<Biome> biome;
		public final Ground ground;
		public final int colour;

		Zone(ResourceKey<Biome> biome, Ground ground, int colour) {
			this.biome = biome;
			this.ground = ground;
			this.colour = colour;
		}

		public boolean isWater() {
			return ground == Ground.SEABED || ground == Ground.DEEP_SEABED || this == RIVER || this == FROZEN_RIVER;
		}
	}

	/** A spire on a crystal planet: its body and its glowing tip. */
	public enum Spire {
		AMETHYST(Blocks.AMETHYST_BLOCK), QUARTZ(Blocks.QUARTZ_BLOCK), ICE(Blocks.BLUE_ICE), CALCITE(Blocks.CALCITE);

		public final Block body;

		Spire(Block body) {
			this.body = body;
		}
	}

	/**
	 * One column of a planet's land: its top solid block ({@code ground}), the fluid over it up to {@code fluidTop} (no fluid:
	 * {@code fluidTop == ground}), its zone, how warm it is (below about -0.5 snow and ice), how much of its height its mountains
	 * make, and a crystal spire standing on it ({@code spire} blocks of {@code spireKind}; 0 for none).
	 */
	public record Column(int ground, int fluidTop, Fluid fluid, Zone zone, double temperature, double mountain, int spire, Spire spireKind) {
		public boolean underFluid() { return fluidTop > ground; }
	}

	/**
	 * One impact basin (or shield volcano) on the sphere: its centre, its angular radius, how deep (or high) it reaches in
	 * elevation, and the cosine of the angle out to which it reaches (its rim included).
	 */
	private record Mark(double x, double y, double z, double radius, double depth, double reach) {
		Mark(Vec3 at, double radius, double depth, double reachFactor) {
			this(at.x, at.y, at.z, radius, depth, Math.cos(Math.min(Math.PI, radius * reachFactor)));
		}
	}

	private final AstralPlanets.Planet planet;
	private final AstralPlanets.Kind kind;
	private final long seed;
	private final SimplexNoise continents, ranges, ridges, hills, warmth, damp, variety, rivers, warp;
	/** The planet's numbers, within its kind's ranges. */
	private final double contFreq, gain, landScale, hillAmp, mountainAmp, oceanDepth, tempShift, humidShift, rangeCover;
	private final Fluid fluid;
	private final boolean hasRivers, terraces, dunes, spires, smallCraters, cones;
	private final int terraceStep;
	private final double duneAngle, duneLength, duneAmp;
	/** Elevation: the level the sea stands at, and how far the highest land rises and the deepest sea falls from it. */
	private final double seaThreshold, eTop, eBottom;
	private final List<Mark> impacts = new ArrayList<>();
	private final List<Mark> shields = new ArrayList<>();

	private PlanetTerrain(AstralPlanets.Planet planet) {
		this.planet = planet;
		this.kind = planet.kind();
		this.seed = planet.seed();
		continents = noise(1);
		ranges = noise(2);
		ridges = noise(3);
		hills = noise(4);
		warmth = noise(5);
		damp = noise(6);
		variety = noise(7);
		rivers = noise(8);
		warp = noise(9);

		RandomSource r = RandomSource.create(seed ^ 0x51A7E7L);
		boolean moon = planet.parent() >= 0;
		double seaLow, seaHigh, landLow, landHigh, hillLow, hillHigh, mountLow, mountHigh, tempLow, tempHigh, humidLow, humidHigh;
		Fluid fluid = Fluid.WATER;
		switch (kind) {
			case VERDANT -> { seaLow = 0.55; seaHigh = 0.72; landLow = 30; landHigh = 60; hillLow = 8; hillHigh = 20; mountLow = 40; mountHigh = 110; tempLow = 0.0; tempHigh = 0.2; humidLow = -0.05; humidHigh = 0.25; }
			case TEMPERATE -> { seaLow = 0.42; seaHigh = 0.62; landLow = 25; landHigh = 50; hillLow = 10; hillHigh = 26; mountLow = 30; mountHigh = 90; tempLow = -0.1; tempHigh = 0.1; humidLow = 0.0; humidHigh = 0.25; }
			case OCEAN -> { seaLow = 0.82; seaHigh = 0.95; landLow = 14; landHigh = 32; hillLow = 4; hillHigh = 10; mountLow = 10; mountHigh = 50; tempLow = 0.1; tempHigh = 0.45; humidLow = 0.2; humidHigh = 0.5; }
			case DESERT -> { seaLow = 0.0; seaHigh = 0.16; landLow = 20; landHigh = 45; hillLow = 3; hillHigh = 8; mountLow = 20; mountHigh = 70; tempLow = 0.55; tempHigh = 0.9; humidLow = -0.9; humidHigh = -0.6; }
			case MESA -> { seaLow = 0.05; seaHigh = 0.22; landLow = 35; landHigh = 70; hillLow = 6; hillHigh = 14; mountLow = 30; mountHigh = 80; tempLow = 0.45; tempHigh = 0.8; humidLow = -0.6; humidHigh = -0.25; }
			case FROZEN -> { seaLow = 0.35; seaHigh = 0.6; landLow = 25; landHigh = 55; hillLow = 8; hillHigh = 18; mountLow = 40; mountHigh = 110; tempLow = -1.25; tempHigh = -0.85; humidLow = -0.1; humidHigh = 0.35; }
			case JUNGLE -> { seaLow = 0.35; seaHigh = 0.55; landLow = 25; landHigh = 55; hillLow = 15; hillHigh = 30; mountLow = 40; mountHigh = 100; tempLow = 0.65; tempHigh = 0.95; humidLow = 0.6; humidHigh = 0.95; }
			case MUSHROOM -> { seaLow = 0.68; seaHigh = 0.86; landLow = 12; landHigh = 30; hillLow = 4; hillHigh = 10; mountLow = 10; mountHigh = 30; tempLow = 0.15; tempHigh = 0.4; humidLow = 0.6; humidHigh = 0.9; }
			case VOLCANIC -> { seaLow = 0.25; seaHigh = 0.5; landLow = 30; landHigh = 70; hillLow = 10; hillHigh = 25; mountLow = 60; mountHigh = 140; tempLow = 0.8; tempHigh = 1.0; humidLow = -1.0; humidHigh = -0.6; fluid = Fluid.LAVA; }
			case ASHEN -> { seaLow = 0.3; seaHigh = 0.5; landLow = 15; landHigh = 35; hillLow = 4; hillHigh = 10; mountLow = 20; mountHigh = 60; tempLow = 0.2; tempHigh = 0.6; humidLow = -1.0; humidHigh = -0.8; fluid = Fluid.NONE; }
			case CRYSTAL -> { seaLow = 0.2; seaHigh = 0.45; landLow = 25; landHigh = 50; hillLow = 8; hillHigh = 16; mountLow = 40; mountHigh = 90; tempLow = -0.3; tempHigh = 0.3; humidLow = -0.2; humidHigh = 0.2; }
			case SWAMP -> { seaLow = 0.45; seaHigh = 0.6; landLow = 6; landHigh = 14; hillLow = 2; hillHigh = 5; mountLow = 10; mountHigh = 30; tempLow = 0.3; tempHigh = 0.6; humidLow = 0.7; humidHigh = 1.0; }
			case BARREN -> { seaLow = 0.3; seaHigh = 0.55; landLow = 10; landHigh = 30; hillLow = 3; hillHigh = 8; mountLow = 15; mountHigh = 50; tempLow = -0.3; tempHigh = 0.3; humidLow = -1.0; humidHigh = -1.0; fluid = Fluid.NONE; }
			case BLOSSOM -> { seaLow = 0.35; seaHigh = 0.55; landLow = 30; landHigh = 60; hillLow = 12; hillHigh = 24; mountLow = 50; mountHigh = 120; tempLow = 0.0; tempHigh = 0.25; humidLow = 0.3; humidHigh = 0.6; }
			case SAVANNA -> { seaLow = 0.2; seaHigh = 0.4; landLow = 25; landHigh = 45; hillLow = 6; hillHigh = 14; mountLow = 30; mountHigh = 70; tempLow = 0.55; tempHigh = 0.8; humidLow = -0.4; humidHigh = -0.1; }
			default -> { seaLow = 0.35; seaHigh = 0.55; landLow = 30; landHigh = 55; hillLow = 10; hillHigh = 22; mountLow = 40; mountHigh = 110; tempLow = -0.65; tempHigh = -0.35; humidLow = 0.0; humidHigh = 0.4; } // TAIGA
		}
		this.fluid = fluid;
		double seaFraction = Mth.lerp(r.nextDouble(), seaLow, seaHigh);
		contFreq = 1.3 + r.nextDouble() * 1.4;
		gain = 0.46 + r.nextDouble() * 0.12;
		landScale = Mth.lerp(r.nextDouble(), landLow, landHigh) * (moon ? 0.7 : 1);
		hillAmp = Mth.lerp(r.nextDouble(), hillLow, hillHigh);
		mountainAmp = Mth.lerp(r.nextDouble(), mountLow, mountHigh);
		oceanDepth = 22 + r.nextDouble() * 22;
		tempShift = Mth.lerp(r.nextDouble(), tempLow, tempHigh);
		humidShift = Mth.lerp(r.nextDouble(), humidLow, humidHigh);
		rangeCover = 0.45 + r.nextDouble() * 0.25;
		hasRivers = fluid != Fluid.NONE && kind != AstralPlanets.Kind.DESERT && r.nextFloat() < 0.85f;
		terraces = kind == AstralPlanets.Kind.MESA;
		terraceStep = 6 + r.nextInt(6);
		dunes = kind == AstralPlanets.Kind.DESERT || kind == AstralPlanets.Kind.ASHEN || kind == AstralPlanets.Kind.MESA;
		duneAngle = r.nextDouble() * Math.PI;
		duneLength = 26 + r.nextDouble() * 34;
		duneAmp = 3 + r.nextDouble() * 6;
		spires = kind == AstralPlanets.Kind.CRYSTAL;
		smallCraters = kind == AstralPlanets.Kind.BARREN || kind == AstralPlanets.Kind.ASHEN || moon;
		cones = kind == AstralPlanets.Kind.VOLCANIC;

		// Great impact basins: the barren worlds and moons are covered in them, ashen ones scarred, and now and then another kind
		// keeps a few old ones; volcanic worlds raise great shield volcanoes instead.
		int craterCount = kind == AstralPlanets.Kind.BARREN || moon ? 45 + r.nextInt(50) : kind == AstralPlanets.Kind.ASHEN ? 18 + r.nextInt(22)
				: r.nextFloat() < 0.15f ? 2 + r.nextInt(6) : 0;
		for (int i = 0; i < craterCount; i++) {
			Vec3 at = unit(r);
			double radius = 0.02 + Math.pow(r.nextDouble(), 2.2) * 0.22;
			impacts.add(new Mark(at, radius, 0.05 + radius * 0.6, 1.8));
		}
		if (cones) {
			int count = 2 + r.nextInt(4);
			for (int i = 0; i < count; i++) {
				shields.add(new Mark(unit(r), 0.04 + r.nextDouble() * 0.06, 0.25 + r.nextDouble() * 0.25, 1.0));
			}
		}

		// The sea stands where it covers {@code seaFraction} of the sphere; the land's and sea's reach are measured the same way.
		RandomSource sampler = RandomSource.create(seed ^ 0x5EA5EAL);
		double[] samples = new double[900];
		for (int i = 0; i < samples.length; i++) {
			Vec3 at = unit(sampler);
			samples[i] = rawElevation(at.x, at.y, at.z, 7);
		}
		Arrays.sort(samples);
		seaThreshold = samples[Mth.clamp((int) Math.round(seaFraction * (samples.length - 1)), 0, samples.length - 1)];
		eTop = Math.max(0.05, samples[(int) (samples.length * 0.985)] - seaThreshold);
		eBottom = Math.max(0.05, seaThreshold - samples[(int) (samples.length * 0.015)]);
	}

	private SimplexNoise noise(int salt) {
		return new SimplexNoise(RandomSource.create(seed * 31 + salt * 0x9E3779B97F4A7C15L));
	}

	private static Vec3 unit(RandomSource r) {
		double z = r.nextDouble() * 2 - 1, a = r.nextDouble() * Math.PI * 2, s = Math.sqrt(1 - z * z);
		return new Vec3(s * Math.cos(a), z, s * Math.sin(a));
	}

	// --- Where on the sphere ---

	/** The longitude (radians, -pi to pi) of land {@code dx} east of the planet's middle. */
	public static double lon(double dx) { return dx / AstralPlanets.LAND_RADIUS * Math.PI; }

	/** The latitude (radians, pi/2 at the north pole) of land {@code dz} south of the planet's middle. */
	public static double lat(double dz) { return -dz / AstralPlanets.LAND_RADIUS * Math.PI; }

	/**
	 * The land (dx, dz from the planet's middle) under {@code direction} (from the planet's centre in space, any length): where one
	 * flying into the planet that way comes down.
	 */
	public static double[] landUnder(Vec3 direction) {
		Vec3 d = direction.normalize();
		double lon = Math.atan2(d.z, d.x), lat = Math.asin(Mth.clamp(d.y, -1, 1));
		return new double[] {lon / Math.PI * AstralPlanets.LAND_RADIUS, -lat / Math.PI * AstralPlanets.LAND_RADIUS};
	}

	/** The way out of the planet's centre that land (dx, dz) lies (a unit vector): where one leaving it comes out in space. */
	public static Vec3 directionOver(double dx, double dz) {
		double lon = lon(dx), lat = Mth.clamp(lat(dz), -Math.PI / 2, Math.PI / 2);
		return new Vec3(Math.cos(lat) * Math.cos(lon), Math.sin(lat), Math.cos(lat) * Math.sin(lon));
	}

	// --- The land ---

	/** Elevation at a sphere point before the sea is set: continents, and the great basins and volcanoes. */
	private double rawElevation(double x, double y, double z, int octaves) {
		double e = fbm3(continents, x, y, z, contFreq, octaves, gain);
		for (Mark m : impacts) {
			double dot = x * m.x + y * m.y + z * m.z;
			if (dot < m.reach) continue;
			double d = Math.acos(Mth.clamp(dot, -1, 1)) / m.radius;
			if (d < 1) e -= m.depth * (1 - d * d);
			e += m.depth * 0.35 * Math.exp(-Mth.square((d - 1) / 0.22));
			if (m.radius > 0.1) e += m.depth * 0.45 * Math.exp(-Mth.square(d / 0.12));
		}
		for (Mark m : shields) {
			double dot = x * m.x + y * m.y + z * m.z;
			if (dot < m.reach) continue;
			double d = Math.acos(Mth.clamp(dot, -1, 1)) / m.radius;
			e += m.depth * Math.pow(1 - d, 1.6);
		}
		return e;
	}

	/**
	 * The column at land (dx, dz) from the planet's middle. {@code painting}: as seen from space (PlanetPainter), without what is
	 * too small to see there (hills, rivers, dunes, small craters and cones, spires) and with fewer octaves, much cheaper.
	 */
	public Column column(double dx, double dz, boolean painting) {
		double lon = lon(dx), lat = Mth.clamp(lat(dz), -Math.PI / 2, Math.PI / 2);
		double cl = Math.cos(lat);
		double px = cl * Math.cos(lon), py = Math.sin(lat), pz = cl * Math.sin(lon);
		double e = rawElevation(px, py, pz, painting ? 7 : 10) - seaThreshold;

		// Mountain belts, their ridges, and how much of the land they raise.
		double belt = smooth(rangeCover, rangeCover + 0.22, fbm3(ranges, px, py, pz, 6.5, 2, 0.5) * 0.5 + 0.5);
		double mountain = 0;
		if (belt > 0 && e > 0) {
			double ridged = ridged3(ridges, px, py, pz, BLOCKS_PER_UNIT / 2200, painting ? 1 : 3);
			mountain = mountainAmp * ridged * ridged * belt * Mth.clamp(e / (eTop * 0.15), 0, 1);
		}

		double h;
		if (e > 0) {
			h = SEA_LEVEL + 1 + landScale * Math.pow(Mth.clamp(e / eTop, 0, 1), 1.15) + mountain;
		} else {
			h = SEA_LEVEL - 3 - oceanDepth * Math.pow(Mth.clamp(-e / eBottom, 0, 1), 0.75);
		}

		boolean river = false, calderaLava = false;
		int spire = 0;
		Spire spireKind = null;
		double fluidTop = fluid == Fluid.NONE ? Double.NEGATIVE_INFINITY : SEA_LEVEL;
		if (!painting) {
			double coastFade = Mth.clamp(e / 0.02, 0, 1);
			h += hillAmp * fbm2(hills, dx / 380.0, dz / 380.0, 3, 0.5) * (e > 0 ? 0.35 + 0.65 * coastFade : 0.25);
			if (e > 0 && dunes) {
				double along = dx * Math.cos(duneAngle) + dz * Math.sin(duneAngle);
				double wobble = 2.2 * warp.getValue(dx / 340.0, dz / 340.0);
				double crest = 1 - Math.abs(Math.sin(along / duneLength + wobble));
				h += duneAmp * crest * crest * Mth.clamp(1 - mountain / 20, 0, 1) * coastFade;
			}
			if (smallCraters) h += smallCraters(dx, dz);
			if (cones) {
				double[] cone = cone(dx, dz);
				h += cone[0];
				if (cone[1] > 0) {
					calderaLava = true;
					fluidTop = Math.max(fluidTop, h + cone[1]);
				}
			}
			if (terraces && h > SEA_LEVEL + 2) {
				double a = (h - SEA_LEVEL) / terraceStep;
				double f = a - Math.floor(a);
				h = SEA_LEVEL + (Math.floor(a) + smooth(0.72, 1.0, f)) * terraceStep;
			}
			if (hasRivers && e > 0) {
				double rv = Math.abs(rivers.getValue(dx / 3400.0, dz / 3400.0) + 0.45 * rivers.getValue(dx / 1150.0 + 40, dz / 1150.0 - 40)) / 1.45;
				double width = 0.022;
				if (rv < width) {
					double depth = 1 - rv / width;
					double fade = 1 - smooth(SEA_LEVEL + 30, SEA_LEVEL + 75, h);
					double carved = Mth.lerp(smooth(0, 1, depth) * fade, h, SEA_LEVEL - 2.5 - 2.5 * depth);
					if (carved < h) {
						h = carved;
						river = h < SEA_LEVEL;
					}
				}
			}
			if (spires && e > 0.01) {
				int[] s = spire(dx, dz);
				spire = s[0];
				if (spire > 0) spireKind = Spire.values()[s[1]];
			}
		}

		// The highest peaks are eased under the ceiling rather than cut flat at it.
		if (h > 220) h = 220 + (h - 220) / (1 + (h - 220) / 140);
		int ground = Mth.clamp((int) Math.round(h), MIN_Y + 6, MAX_GROUND);
		if (spire > 0) spire = Math.min(spire, MAX_GROUND + 30 - ground);
		Fluid here = fluid;
		if (calderaLava) here = Fluid.LAVA;
		int top = (int) Math.floor(fluidTop);
		if (here == Fluid.NONE || top <= ground) {
			here = Fluid.NONE;
			top = ground;
		}
		if (river && kind == AstralPlanets.Kind.VOLCANIC) here = Fluid.LAVA; // the volcanic worlds' rivers run with lava

		double temperature = Math.cos(lat) * 1.6 - 0.75 + tempShift + fbm3(warmth, px, py, pz, 4.0, 3, 0.5) * 0.45
				- Math.max(0, ground - SEA_LEVEL - 30) * 0.008;
		double humidity = humidShift + fbm3(damp, px, py, pz, 5.0, 3, 0.5) * 0.7;
		double v = painting ? fbm3(variety, px, py, pz, 60, 2, 0.5) * 1.6 : variety.getValue(dx / 1700.0, dz / 1700.0) * 0.75 + 0.25 * variety.getValue(dx / 520.0, dz / 520.0);
		Zone zone = zone(ground, top > ground, river, e, temperature, humidity, v, mountain);
		return new Column(ground, top, here, zone, temperature, mountain, spire, spireKind);
	}

	/** The column's zone: its biome and ground. */
	private Zone zone(int ground, boolean wet, boolean river, double e, double t, double h, double v, double mountain) {
		int alt = ground - SEA_LEVEL;
		if (kind == AstralPlanets.Kind.VOLCANIC) {
			if (wet && ground < SEA_LEVEL) return Zone.LAVA_SEA;
			return alt > 45 ? Zone.VOLCANIC_PEAKS : Zone.VOLCANIC_PLAINS;
		}
		if (kind == AstralPlanets.Kind.ASHEN) return v > 0.25 ? Zone.ASH_DUNES : Zone.ASH_PLAINS;
		if (kind == AstralPlanets.Kind.BARREN) return e < 0 ? Zone.DARK_REGOLITH : Zone.REGOLITH;
		if (river) return t < -0.5 ? Zone.FROZEN_RIVER : Zone.RIVER;
		if (wet && ground < SEA_LEVEL) {
			boolean deep = ground < SEA_LEVEL - 22;
			if (t < -0.55) return deep ? Zone.DEEP_FROZEN_OCEAN : Zone.FROZEN_OCEAN;
			if (t < -0.15) return deep ? Zone.DEEP_COLD_OCEAN : Zone.COLD_OCEAN;
			if (t > 0.75) return deep ? Zone.DEEP_LUKEWARM_OCEAN : Zone.WARM_OCEAN;
			if (t > 0.35) return deep ? Zone.DEEP_LUKEWARM_OCEAN : Zone.LUKEWARM_OCEAN;
			return deep ? Zone.DEEP_OCEAN : Zone.OCEAN;
		}
		// Shores: sand at the water's edge, bare rock where mountains meet the sea.
		if (alt <= 2 && e < eTop * 0.04 && fluid == Fluid.WATER) {
			if (mountain > 10) return Zone.STONY_SHORE;
			if (kind != AstralPlanets.Kind.SWAMP && kind != AstralPlanets.Kind.MUSHROOM && kind != AstralPlanets.Kind.CRYSTAL) {
				return t < -0.5 ? Zone.SNOWY_BEACH : Zone.BEACH;
			}
		}
		switch (kind) {
			case CRYSTAL:
				return alt > 110 && t < 0 ? Zone.FROZEN_PEAKS : Zone.CRYSTAL_FIELDS;
			case MUSHROOM:
				return alt > 55 ? Zone.DARK_FOREST : Zone.MUSHROOM_FIELDS;
			case DESERT:
				if (alt > 95) return Zone.STONY_PEAKS;
				if (alt > 60) return v > 0.2 ? Zone.BADLANDS : Zone.WINDSWEPT_SAVANNA;
				if (h > 0.25 && alt < 12) return Zone.SAVANNA; // an oasis
				return t < -0.4 ? Zone.SNOWY_PLAINS : Zone.DESERT;
			case MESA:
				if (alt > 55) return h > -0.25 ? Zone.WOODED_BADLANDS : Zone.ERODED_BADLANDS;
				if (h > 0.2) return Zone.SAVANNA_PLATEAU;
				if (h < -0.7) return Zone.DESERT;
				return v > 0.45 ? Zone.ERODED_BADLANDS : Zone.BADLANDS;
			case SWAMP:
				if (alt < 9) return t > 0.5 && v > -0.2 ? Zone.MANGROVE_SWAMP : Zone.SWAMP;
				return alt > 40 ? Zone.FOREST : Zone.DARK_FOREST;
			default:
				return earthlike(t, h, alt, v);
		}
	}

	/** The Overworld's own run of lands by warmth, wetness and height, leant by the kind (blossom worlds pink with cherry groves). */
	private Zone earthlike(double t, double h, int alt, double v) {
		boolean blossom = kind == AstralPlanets.Kind.BLOSSOM;
		if (alt > 115) return t < 0.1 ? (v > 0 ? Zone.JAGGED_PEAKS : Zone.FROZEN_PEAKS) : Zone.STONY_PEAKS;
		if (alt > 82) {
			if (t < -0.2) return Zone.SNOWY_SLOPES;
			if (t < 0.15) return Zone.GROVE;
			if (blossom) return Zone.CHERRY_GROVE;
			return h > 0.2 ? Zone.WINDSWEPT_FOREST : Zone.WINDSWEPT_HILLS;
		}
		if (t < -0.55) return v > 0.5 ? Zone.ICE_SPIKES : h > 0.05 ? Zone.SNOWY_TAIGA : Zone.SNOWY_PLAINS;
		if (t < -0.15) {
			if (h > 0.35) return v > 0.3 ? Zone.OLD_GROWTH_SPRUCE_TAIGA : Zone.TAIGA;
			return v > 0.5 ? Zone.OLD_GROWTH_PINE_TAIGA : Zone.TAIGA;
		}
		if (t > 0.75) {
			if (h < -0.35) return Zone.DESERT;
			if (h < 0.05) return alt > 45 ? Zone.SAVANNA_PLATEAU : Zone.SAVANNA;
			if (h > 0.55) return v > 0.4 ? Zone.BAMBOO_JUNGLE : Zone.JUNGLE;
			return Zone.SPARSE_JUNGLE;
		}
		if (blossom) {
			if (v > -0.35) return Zone.CHERRY_GROVE;
			return h > 0.2 ? Zone.FLOWER_FOREST : Zone.MEADOW;
		}
		if (alt > 50 && h > -0.1) return v > 0.2 ? Zone.MEADOW : Zone.FOREST;
		if (h < -0.4) return v > 0.45 ? Zone.SUNFLOWER_PLAINS : Zone.PLAINS;
		if (h < -0.05) return v > 0.5 ? Zone.FLOWER_FOREST : Zone.PLAINS;
		if (h < 0.35) return v > 0.3 ? Zone.BIRCH_FOREST : Zone.FOREST;
		if (h < 0.7) return v > 0.5 ? Zone.OLD_GROWTH_BIRCH_FOREST : Zone.DARK_FOREST;
		return alt < 6 ? Zone.SWAMP : Zone.DARK_FOREST;
	}

	// --- Small things, in the land's own blocks ---

	/** Small craters, on the barren worlds and the moons: bowls with raised rims, many to a cell. */
	private double smallCraters(double dx, double dz) {
		double total = 0;
		double size = 230;
		int cx = Mth.floor(dx / size), cz = Mth.floor(dz / size);
		for (int ox = -1; ox <= 1; ox++) {
			for (int oz = -1; oz <= 1; oz++) {
				long h = hash(cx + ox, cz + oz, 0xC2A7E2L);
				if ((h & 0xFF) > 150) continue;
				double x = (cx + ox + unitHash(h, 8)) * size, z = (cz + oz + unitHash(h, 24)) * size;
				double r = 8 + Math.pow(unitHash(h, 40), 2) * 70;
				double d = Math.sqrt(Mth.square(dx - x) + Mth.square(dz - z)) / r;
				if (d > 1.6) continue;
				double depth = Math.min(28, r * 0.28), rim = Math.min(9, r * 0.09);
				if (d < 1) total -= depth * (1 - d * d);
				total += rim * Math.exp(-Mth.square((d - 1) / 0.2));
			}
		}
		return total;
	}

	/** A volcanic world's cones: [the rise here, the depth of lava standing in its crater here (0 for none)]. */
	private double[] cone(double dx, double dz) {
		double rise = 0, lava = 0;
		double size = 1600;
		int cx = Mth.floor(dx / size), cz = Mth.floor(dz / size);
		for (int ox = -1; ox <= 1; ox++) {
			for (int oz = -1; oz <= 1; oz++) {
				long h = hash(cx + ox, cz + oz, 0xC0E5L);
				if ((h & 0xFF) > 115) continue;
				double x = (cx + ox + 0.2 + unitHash(h, 8) * 0.6) * size, z = (cz + oz + 0.2 + unitHash(h, 24) * 0.6) * size;
				double r = 180 + unitHash(h, 40) * 300, height = 50 + unitHash(h, 52) * 90;
				double d = Math.sqrt(Mth.square(dx - x) + Mth.square(dz - z)) / r;
				if (d >= 1) continue;
				double crater = 0.16;
				if (d < crater) {
					double peak = height * Math.pow(1 - crater, 1.6);
					double floor = peak - 18 * (1 - Mth.square(d / crater));
					rise += floor;
					lava = Math.max(lava, peak - 6 - floor);
				} else {
					rise += height * Math.pow(1 - d, 1.6);
				}
			}
		}
		return new double[] {rise, lava};
	}

	/** A crystal world's spires: [its height here (0: none), its kind]. Thin, tall, tapering, a few to every cell. */
	private int[] spire(double dx, double dz) {
		double size = 64;
		int cx = Mth.floor(dx / size), cz = Mth.floor(dz / size);
		for (int ox = -1; ox <= 1; ox++) {
			for (int oz = -1; oz <= 1; oz++) {
				long h = hash(cx + ox, cz + oz, 0x5A1FEL);
				if ((h & 0xFF) > 60) continue;
				double x = (cx + ox + unitHash(h, 8)) * size, z = (cz + oz + unitHash(h, 24)) * size;
				double r = 2.5 + unitHash(h, 40) * 5;
				double d = Math.sqrt(Mth.square(dx - x) + Mth.square(dz - z)) / r;
				if (d >= 1) continue;
				int height = (int) Math.round((14 + unitHash(h, 50) * 50) * Math.pow(1 - d, 0.7));
				int kindIndex = (int) ((h >>> 58) & 3);
				return new int[] {Math.max(1, height), kindIndex};
			}
		}
		return new int[] {0, 0};
	}

	// --- Noise ---

	private static double fbm3(SimplexNoise noise, double x, double y, double z, double freq, int octaves, double gain) {
		double sum = 0, amp = 1, norm = 0;
		for (int i = 0; i < octaves; i++) {
			sum += amp * noise.getValue(x * freq + i * 31.7, y * freq + i * 17.3, z * freq - i * 23.1);
			norm += amp;
			amp *= gain;
			freq *= 2.03;
		}
		return sum / norm;
	}

	/** Ridges: sharp crests where the noise crosses zero, each octave weighted by the last (rough crests, smooth valleys). */
	private static double ridged3(SimplexNoise noise, double x, double y, double z, double freq, int octaves) {
		double sum = 0, amp = 1, norm = 0, weight = 1;
		for (int i = 0; i < octaves; i++) {
			double r = 1 - Math.abs(noise.getValue(x * freq + i * 11.1, y * freq + i * 7.7, z * freq + i * 5.3));
			r *= r * weight;
			weight = Mth.clamp(r * 2, 0, 1);
			sum += r * amp;
			norm += amp;
			amp *= 0.5;
			freq *= 2.1;
		}
		return sum / norm;
	}

	private static double fbm2(SimplexNoise noise, double x, double z, int octaves, double gain) {
		double sum = 0, amp = 1, norm = 0, freq = 1;
		for (int i = 0; i < octaves; i++) {
			sum += amp * noise.getValue(x * freq + i * 19.1, z * freq - i * 13.7);
			norm += amp;
			amp *= gain;
			freq *= 2.2;
		}
		return sum / norm;
	}

	private static double smooth(double from, double to, double x) {
		double t = Mth.clamp((x - from) / (to - from), 0, 1);
		return t * t * (3 - 2 * t);
	}

	private long hash(int x, int z, long salt) {
		long h = seed ^ salt;
		h ^= x * 0x9E3779B97F4A7C15L;
		h = Long.rotateLeft(h, 27) * 0xBF58476D1CE4E5B9L;
		h ^= z * 0xC2B2AE3D27D4EB4FL;
		h ^= h >>> 31;
		h *= 0x94D049BB133111EBL;
		return h ^ (h >>> 29);
	}

	/** Sixteen bits of {@code h} from {@code bit}, as a number from 0 to 1. */
	private static double unitHash(long h, int bit) {
		return ((h >>> bit) & 0xFFFF) / 65535.0;
	}

	/** A number from 0 to 1 for a block column of this planet (decorations: clusters, bushes, magma). */
	public double columnChance(int x, int z, long salt) {
		return unitHash(hash(x, z, salt), 20);
	}

	// --- What the planet is ---

	public AstralPlanets.Planet planet() { return planet; }

	public Fluid fluid() { return fluid; }

	/** Its sea level, or below the floor for a world with no seas. */
	public int seaLevel() { return fluid == Fluid.NONE ? MIN_Y - 1 : SEA_LEVEL; }

	private static final Map<Long, PlanetTerrain> CACHE = new ConcurrentHashMap<>();

	/** {@code planet}'s land (made once and kept while it is in use; the world-generation threads share it). */
	public static PlanetTerrain of(AstralPlanets.Planet planet) {
		PlanetTerrain terrain = CACHE.get(planet.seed());
		if (terrain != null) return terrain;
		if (CACHE.size() > 48) CACHE.clear();
		return CACHE.computeIfAbsent(planet.seed(), s -> new PlanetTerrain(planet));
	}
}
