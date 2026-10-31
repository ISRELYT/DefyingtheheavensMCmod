package com.example.defyingtheheavens;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

/**
 * The secret realms' lands (ModDimensions#SECRET_REALM): each realm's slot (SecretRealm#centre) laid out block by block by its own land
 * (SecretRealm#land: RealmLand), the world's seed making every world's its own, the space between them left empty. No structures, no
 * creatures (their buildings and guardians are the realms' works: RealmWorks). Registered as "defying-the-heavens:secret_realm"
 * (SecretRealms#register).
 */
public class SecretRealmGenerator extends ChunkGenerator {
	public static final Codec<SecretRealmGenerator> CODEC = RecordCodecBuilder.create(instance -> instance
			.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource))
			.apply(instance, instance.stable(SecretRealmGenerator::new)));

	/** The world's seed, as the chunk map hands it over ({@link #createState}): each world's lands their own. */
	private volatile long seed;

	public SecretRealmGenerator(BiomeSource biomeSource) {
		super(biomeSource);
	}

	@Override
	protected Codec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	/** No structures; the seed kept for the lands. */
	@Override
	public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> lookup, RandomState randomState, long seed) {
		this.seed = seed;
		return ChunkGeneratorStructureState.createForFlat(randomState, seed, biomeSource, Stream.empty());
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState, StructureManager structures, ChunkAccess chunk) {
		ChunkPos pos = chunk.getPos();
		SecretRealm realm = SecretRealm.at(pos.getMiddleBlockX(), pos.getMiddleBlockZ());
		if (realm != null) realm.land().fill(chunk, pos.getMinBlockX(), pos.getMinBlockZ(), realm.centre(), seed);
		return CompletableFuture.completedFuture(chunk);
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heights, RandomState randomState) {
		SecretRealm realm = SecretRealm.at(x, z);
		if (realm == null) return heights.getMinBuildHeight();
		BlockPos centre = realm.centre();
		int ground = realm.land().ground(seed, x - centre.getX(), z - centre.getZ());
		if (ground < 0) return heights.getMinBuildHeight();
		boolean fluidCounts = type.isOpaque().test(Blocks.WATER.defaultBlockState());
		return (fluidCounts ? Math.max(ground, realm.land().water(seed, x - centre.getX(), z - centre.getZ())) : ground) + 1;
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heights, RandomState randomState) {
		BlockState[] states = new BlockState[ModDimensions.SECRET_REALM_HEIGHT];
		SecretRealm realm = SecretRealm.at(x, z);
		if (realm != null) {
			BlockPos centre = realm.centre();
			int ground = realm.land().ground(seed, x - centre.getX(), z - centre.getZ());
			if (ground >= 0) {
				int water = realm.land().water(seed, x - centre.getX(), z - centre.getZ());
				for (int y = 0; y <= ground && y < states.length; y++) states[y] = Blocks.STONE.defaultBlockState();
				for (int y = ground + 1; y <= water && y < states.length; y++) states[y] = Blocks.WATER.defaultBlockState();
			}
		}
		for (int i = 0; i < states.length; i++) if (states[i] == null) states[i] = Blocks.AIR.defaultBlockState();
		return new NoiseColumn(ModDimensions.SECRET_REALM_MIN_Y, states);
	}

	@Override
	public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
		SecretRealm realm = SecretRealm.at(pos.getX(), pos.getZ());
		if (realm == null) return;
		BlockPos centre = realm.centre();
		int lx = pos.getX() - centre.getX(), lz = pos.getZ() - centre.getZ();
		info.add("Secret realm: " + realm.getId() + " " + realm.land().describe(seed, lx, lz) + String.format(" r %.0f", Math.hypot(lx, lz)));
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion region) {
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
		return ModDimensions.SECRET_REALM_HEIGHT;
	}

	@Override
	public int getSeaLevel() {
		return HerbValley.WATER_Y;
	}

	@Override
	public int getMinY() {
		return ModDimensions.SECRET_REALM_MIN_Y;
	}
}
