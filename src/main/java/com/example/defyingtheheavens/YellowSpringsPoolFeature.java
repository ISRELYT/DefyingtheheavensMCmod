package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The Yellow Springs' slow, murky water: once a chunk, every column of the Yellow Springs whose ground lies below
 * {@link #POOL_LEVEL} is flooded up to it over a bed of mud, so the biome's low riverlands lie under golden water (the colour is
 * the biome's) between mud and soul-soil banks. The water is held in: a column beside lower ground that won't hold water too
 * (the land beyond the Springs, a chasm's edge) is raised into a bank up to the water line instead of flooded, so no pool spills
 * over its edge in sheets of running water or stands as a wall of water in the air. Springs in the walls
 * (ModPlacedFeatures#NW_SPRING_WATER) still spill as waterfalls into the dark.
 * <p>
 * Placed in every chunk the Springs reach (no biome filter: it checks each column itself), so a pool never ends at a chunk's
 * edge where its neighbour's columns were left dry.
 */
public class YellowSpringsPoolFeature extends Feature<NoneFeatureConfiguration> {
	/** The springs' water surface: the lowlands sink to about Y 65-80 (ModNoiseSettings#netherworld). */
	public static final int POOL_LEVEL = 80;

	public YellowSpringsPoolFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		BlockPos origin = context.origin();
		int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
		// The chunk's columns and a ring round them (in the neighbouring chunks): their ground, and whether they hold water.
		int[][] top = new int[18][18];
		boolean[][] holds = new boolean[18][18];
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		boolean any = false;
		for (int i = 0; i < 18; i++) {
			for (int j = 0; j < 18; j++) {
				int x = x0 - 1 + i, z = z0 - 1 + j;
				top[i][j] = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
				holds[i][j] = holdsWater(level, pos, x, z, top[i][j]);
				any |= holds[i][j] && i >= 1 && i <= 16 && j >= 1 && j <= 16;
			}
		}
		if (!any) return false;
		BlockState water = Blocks.WATER.defaultBlockState();
		for (int i = 1; i <= 16; i++) {
			for (int j = 1; j <= 16; j++) {
				int ground = top[i][j];
				if (!holds[i][j] || ground >= POOL_LEVEL) continue;
				int x = x0 - 1 + i, z = z0 - 1 + j;
				boolean bank = spills(holds, top, i + 1, j) || spills(holds, top, i - 1, j) || spills(holds, top, i, j + 1) || spills(holds, top, i, j - 1);
				level.setBlock(pos.set(x, ground - 1, z), Blocks.MUD.defaultBlockState(), Block.UPDATE_CLIENTS);
				for (int y = ground; y < POOL_LEVEL; y++) {
					if (!level.getBlockState(pos.set(x, y, z)).isAir()) continue;
					BlockState fill = !bank ? water : y == POOL_LEVEL - 1 ? Blocks.SOUL_SOIL.defaultBlockState() : Blocks.MUD.defaultBlockState();
					level.setBlock(pos, fill, Block.UPDATE_CLIENTS);
				}
			}
		}
		return true;
	}

	/** Lower ground beside a pool's column that won't hold water too: the pool would spill over into it. */
	private static boolean spills(boolean[][] holds, int[][] top, int i, int j) {
		return !holds[i][j] && top[i][j] < POOL_LEVEL;
	}

	/**
	 * Whether the column at x, z holds the springs' water: the Yellow Springs, its ground firm and below the water line, above the
	 * abyss. One a neighbouring chunk's run already flooded (its water up to the line) or banked (its bank up to it) still does,
	 * so every chunk reads its neighbours alike whichever was decorated first.
	 */
	private static boolean holdsWater(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int top) {
		if (!level.getBiome(pos.set(x, POOL_LEVEL, z)).is(ModBiomes.YELLOW_SPRINGS)) return false;
		if (top > POOL_LEVEL || top <= level.getMinBuildHeight() + 1) return false;
		BlockState bed = level.getBlockState(pos.set(x, top - 1, z));
		if (bed.isAir()) return false;
		return bed.getFluidState().isEmpty() || top == POOL_LEVEL && bed.is(Blocks.WATER);
	}
}
