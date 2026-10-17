package com.example.defyingtheheavens;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

/**
 * The planets' lands (ModDimensions#ASTRAL_SURFACES): each planet's square of the grid (AstralPlanets) laid out block by block
 * from its {@link PlanetTerrain}, the void between them left empty. Bedrock at the floor, stone (deepslate below 0) up to the
 * zone's ground, bare rock where the land is steep, then water or lava to the sea's level; a crystal world's spires, a few
 * amethyst clusters, an ashen world's dead bushes, magma in volcanic ground. The biomes (PlanetBiomeSource) bring the trees,
 * flowers, ores and the rest; no structures. Registered as "defying-the-heavens:planet_surface" (DefyingTheHeavens).
 */
public class PlanetSurfaceGenerator extends ChunkGenerator {
	public static final Codec<PlanetSurfaceGenerator> CODEC = RecordCodecBuilder.create(instance -> instance
			.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource))
			.apply(instance, instance.stable(PlanetSurfaceGenerator::new)));

	/** A drop this steep to a neighbouring column (blocks) bares the rock. */
	private static final int CLIFF = 4;

	public PlanetSurfaceGenerator(BiomeSource biomeSource) {
		super(biomeSource);
	}

	@Override
	protected Codec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	/** No structures on the planets (no villages, strongholds or sects: for now they are wild). */
	@Override
	public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> lookup, RandomState randomState, long seed) {
		return ChunkGeneratorStructureState.createForFlat(randomState, seed, biomeSource, Stream.empty());
	}

	private static AstralPlanets.Planet planetOf(int x, int z) {
		AstralPlanets planets = AstralPlanets.get();
		return planets == null ? null : planets.planetAt(x, z);
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState, StructureManager structures, ChunkAccess chunk) {
		ChunkPos pos = chunk.getPos();
		int x0 = pos.getMinBlockX(), z0 = pos.getMinBlockZ();
		AstralPlanets.Planet planet = planetOf(x0, z0);
		if (planet == null) return CompletableFuture.completedFuture(chunk); // the void between the planets
		PlanetTerrain terrain = PlanetTerrain.of(planet);
		int lx = planet.landX(), lz = planet.landZ();

		// The chunk's columns and a ring round them, for how steep each one is.
		PlanetTerrain.Column[] columns = new PlanetTerrain.Column[18 * 18];
		for (int dz = -1; dz <= 16; dz++) {
			for (int dx = -1; dx <= 16; dx++) {
				columns[(dz + 1) * 18 + dx + 1] = terrain.column(x0 + dx - lx, z0 + dz - lz, false);
			}
		}

		Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
		BlockState[] column = new BlockState[PlanetTerrain.HEIGHT];
		LevelChunkSection[] sections = chunk.getSections();
		for (LevelChunkSection section : sections) section.acquire();
		try {
			for (int dz = 0; dz < 16; dz++) {
				for (int dx = 0; dx < 16; dx++) {
					PlanetTerrain.Column c = columns[(dz + 1) * 18 + dx + 1];
					int steep = 0;
					for (int[] n : NEIGHBOURS) steep = Math.max(steep, c.ground() - columns[(dz + 1 + n[1]) * 18 + dx + 1 + n[0]].ground());
					int x = x0 + dx, z = z0 + dz;
					int top = stack(terrain, c, steep, x, z, column);
					for (int y = PlanetTerrain.MIN_Y; y <= top; y++) {
						BlockState state = column[y - PlanetTerrain.MIN_Y];
						if (state == null || state.isAir()) continue;
						LevelChunkSection section = sections[chunk.getSectionIndex(y)];
						section.setBlockState(dx, y & 15, dz, state, false);
					}
					oceanFloor.update(dx, c.underFluid() ? c.ground() : Math.max(c.ground(), Math.min(top, c.ground() + c.spire())), dz, Blocks.STONE.defaultBlockState());
					surface.update(dx, top, dz, Blocks.STONE.defaultBlockState());
				}
			}
		} finally {
			for (LevelChunkSection section : sections) section.release();
		}
		return CompletableFuture.completedFuture(chunk);
	}

	private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	/**
	 * Fills {@code out} (by height from the floor) with the column's blocks. @return the highest block set
	 */
	private static int stack(PlanetTerrain terrain, PlanetTerrain.Column c, int steep, int x, int z, BlockState[] out) {
		Arrays.fill(out, null);
		PlanetTerrain.Ground ground = c.zone().ground;
		boolean wet = c.underFluid();
		boolean cliff = steep >= CLIFF && !wet && ground != PlanetTerrain.Ground.SHORE;
		int g = c.ground();
		int fillerDepth = 3 + (int) (terrain.columnChance(x, z, 0xF1L) * 3);
		BlockState stone = Blocks.STONE.defaultBlockState(), deepslate = Blocks.DEEPSLATE.defaultBlockState();
		for (int y = PlanetTerrain.MIN_Y; y <= g; y++) {
			BlockState state;
			if (y == PlanetTerrain.MIN_Y || y < PlanetTerrain.MIN_Y + 5 && terrain.columnChance(x * 7 + y, z, 0xBEDL) < (PlanetTerrain.MIN_Y + 5 - y) / 5.0) {
				state = Blocks.BEDROCK.defaultBlockState();
			} else if (y < g - fillerDepth) {
				state = y < 0 || y < 8 && terrain.columnChance(x, z * 5 + y, 0xDEEL) < (8 - y) / 8.0 ? deepslate : stone;
			} else if (y < g) {
				state = cliff ? ground.cliff.defaultBlockState() : badlandsBand(terrain, ground, y, ground.filler);
			} else {
				state = topBlock(terrain, c, ground, wet, cliff, x, z, y);
			}
			out[y - PlanetTerrain.MIN_Y] = state;
		}
		int top = g;
		// Water or lava over it, up to the sea (or a crater's rim).
		if (wet) {
			BlockState fluid = c.fluid() == PlanetTerrain.Fluid.LAVA ? Blocks.LAVA.defaultBlockState() : Blocks.WATER.defaultBlockState();
			for (int y = g + 1; y <= c.fluidTop() && y <= PlanetTerrain.TOP_Y; y++) out[y - PlanetTerrain.MIN_Y] = fluid;
			top = Math.min(PlanetTerrain.TOP_Y, c.fluidTop());
		} else if (c.spire() > 0 && c.spireKind() != null) {
			// A crystal spire, its tip aglow.
			int tip = Math.min(PlanetTerrain.TOP_Y, g + c.spire());
			for (int y = g + 1; y <= tip; y++) {
				out[y - PlanetTerrain.MIN_Y] = y == tip && c.spire() > 6 ? Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState() : c.spireKind().body.defaultBlockState();
			}
			top = tip;
		} else if (g + 1 <= PlanetTerrain.TOP_Y) {
			BlockState decoration = decoration(terrain, c, ground, cliff, x, z);
			if (decoration != null) {
				out[g + 1 - PlanetTerrain.MIN_Y] = decoration;
				top = g + 1;
			}
		}
		return top;
	}

	/** The column's top block: its zone's ground, bare rock on a cliff, its floor under water; magma and patches in the wild lands. */
	private static BlockState topBlock(PlanetTerrain terrain, PlanetTerrain.Column c, PlanetTerrain.Ground ground, boolean wet, boolean cliff,
			int x, int z, int y) {
		if (wet) {
			if (c.fluid() == PlanetTerrain.Fluid.LAVA) return (terrain.columnChance(x, z, 0x3A6L) < 0.3 ? Blocks.MAGMA_BLOCK : Blocks.BASALT).defaultBlockState();
			boolean deep = y < PlanetTerrain.SEA_LEVEL - 18;
			if (c.zone().isWater() && deep && ground == PlanetTerrain.Ground.SEABED && c.temperature() < 0.75) return Blocks.GRAVEL.defaultBlockState();
			return ground.floor.defaultBlockState();
		}
		if (cliff) return ground.cliff.defaultBlockState();
		double chance = terrain.columnChance(x, z, 0x70FL);
		return switch (ground) {
			case BADLANDS -> y > PlanetTerrain.SEA_LEVEL + 12 ? badlandsBand(terrain, ground, y, Blocks.TERRACOTTA) : Blocks.RED_SAND.defaultBlockState();
			case BASALT -> (chance < 0.05 ? Blocks.MAGMA_BLOCK : chance < 0.35 ? Blocks.BLACKSTONE : Blocks.BASALT).defaultBlockState();
			case SCORIA -> (chance < 0.12 ? Blocks.MAGMA_BLOCK : Blocks.BLACKSTONE).defaultBlockState();
			case ASH -> (chance < 0.12 ? Blocks.COARSE_DIRT : chance < 0.3 ? Blocks.GRAVEL : Blocks.TUFF).defaultBlockState();
			case REGOLITH -> (chance < 0.2 ? Blocks.GRAVEL : chance < 0.35 ? Blocks.TUFF : Blocks.ANDESITE).defaultBlockState();
			case CRYSTAL -> (chance < 0.08 ? Blocks.AMETHYST_BLOCK : chance < 0.18 ? Blocks.SMOOTH_BASALT : Blocks.CALCITE).defaultBlockState();
			default -> ground.top.defaultBlockState();
		};
	}

	/** Badlands rock in coloured bands by height, as the Overworld's mesas (the bands' order is the planet's own). */
	private static BlockState badlandsBand(PlanetTerrain terrain, PlanetTerrain.Ground ground, int y, net.minecraft.world.level.block.Block otherwise) {
		if (ground != PlanetTerrain.Ground.BADLANDS) return otherwise.defaultBlockState();
		double band = terrain.columnChance(y, 0, 0xBA4DL);
		return (band < 0.18 ? Blocks.ORANGE_TERRACOTTA : band < 0.3 ? Blocks.YELLOW_TERRACOTTA : band < 0.4 ? Blocks.RED_TERRACOTTA
				: band < 0.48 ? Blocks.WHITE_TERRACOTTA : band < 0.55 ? Blocks.BROWN_TERRACOTTA : band < 0.6 ? Blocks.LIGHT_GRAY_TERRACOTTA
				: Blocks.TERRACOTTA).defaultBlockState();
	}

	/** Something small standing on the ground in the lands the biomes leave bare, or null. */
	private static BlockState decoration(PlanetTerrain terrain, PlanetTerrain.Column c, PlanetTerrain.Ground ground, boolean cliff, int x, int z) {
		if (cliff) return null;
		double chance = terrain.columnChance(x, z, 0xDEC0L);
		return switch (ground) {
			case CRYSTAL -> chance < 0.03 ? Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, net.minecraft.core.Direction.UP)
					: chance < 0.05 ? Blocks.LARGE_AMETHYST_BUD.defaultBlockState().setValue(AmethystClusterBlock.FACING, net.minecraft.core.Direction.UP) : null;
			// Dead bushes take root only in the coarse dirt of the ash plains.
			case ASH -> chance < 0.04 && terrain.columnChance(x, z, 0x70FL) < 0.12 ? Blocks.DEAD_BUSH.defaultBlockState() : null;
			default -> null;
		};
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heights, RandomState randomState) {
		AstralPlanets.Planet planet = planetOf(x, z);
		if (planet == null) return heights.getMinBuildHeight();
		PlanetTerrain.Column c = PlanetTerrain.of(planet).column(x - planet.landX(), z - planet.landZ(), false);
		int solid = c.ground() + Math.max(0, c.spire());
		boolean fluidCounts = type.isOpaque().test(Blocks.WATER.defaultBlockState());
		return (fluidCounts ? Math.max(solid, c.fluidTop()) : solid) + 1;
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heights, RandomState randomState) {
		BlockState[] states = new BlockState[PlanetTerrain.HEIGHT];
		AstralPlanets.Planet planet = planetOf(x, z);
		if (planet != null) {
			PlanetTerrain terrain = PlanetTerrain.of(planet);
			stack(terrain, terrain.column(x - planet.landX(), z - planet.landZ(), false), 0, x, z, states);
		}
		for (int i = 0; i < states.length; i++) if (states[i] == null) states[i] = Blocks.AIR.defaultBlockState();
		return new NoiseColumn(PlanetTerrain.MIN_Y, states);
	}

	@Override
	public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
		AstralPlanets.Planet planet = planetOf(pos.getX(), pos.getZ());
		if (planet == null) return;
		PlanetTerrain.Column c = PlanetTerrain.of(planet).column(pos.getX() - planet.landX(), pos.getZ() - planet.landZ(), false);
		info.add("Planet: " + planet.name() + " (" + planet.kind().getId() + ", qi x" + planet.qi() + ") zone " + c.zone().name().toLowerCase()
				+ String.format(" warmth %.2f", c.temperature()));
	}

	/** The beasts of the land's biomes, as the Overworld's chunks are first stocked with them. */
	@Override
	public void spawnOriginalMobs(WorldGenRegion region) {
		ChunkPos pos = region.getCenter();
		if (planetOf(pos.getMinBlockX(), pos.getMinBlockZ()) == null) return;
		Holder<Biome> biome = region.getBiome(pos.getWorldPosition().atY(region.getMaxBuildHeight() - 1));
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
		random.setDecorationSeed(region.getSeed(), pos.getMinBlockX(), pos.getMinBlockZ());
		NaturalSpawner.spawnMobsForChunkGeneration(region, biome, pos, random);
	}

	@Override
	public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState randomState, ChunkAccess chunk) {
	}

	@Override
	public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomes, StructureManager structures,
			ChunkAccess chunk, GenerationStep.Carving step) {
	}

	@Override
	public int getGenDepth() {
		return PlanetTerrain.HEIGHT;
	}

	@Override
	public int getSeaLevel() {
		return PlanetTerrain.SEA_LEVEL;
	}

	@Override
	public int getMinY() {
		return PlanetTerrain.MIN_Y;
	}
}
