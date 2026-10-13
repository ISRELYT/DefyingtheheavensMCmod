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
 * the biome's) between mud and soul-soil banks. Where the riverlands reach a chasm the water stands at its edge; springs in the
 * walls (ModPlacedFeatures#NW_SPRING_WATER) spill as waterfalls into the dark.
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
		boolean any = false;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockState water = Blocks.WATER.defaultBlockState();
		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				int x = x0 + dx, z = z0 + dz;
				if (!level.getBiome(pos.set(x, POOL_LEVEL, z)).is(ModBiomes.YELLOW_SPRINGS)) continue;
				int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
				if (top > POOL_LEVEL || top <= level.getMinBuildHeight() + 1) continue;
				BlockState bed = level.getBlockState(pos.set(x, top - 1, z));
				if (bed.isAir() || !bed.getFluidState().isEmpty()) continue;
				level.setBlock(pos, Blocks.MUD.defaultBlockState(), Block.UPDATE_CLIENTS);
				for (int y = top; y < POOL_LEVEL; y++) {
					if (level.getBlockState(pos.set(x, y, z)).isAir()) level.setBlock(pos, water, Block.UPDATE_CLIENTS);
				}
				any = true;
			}
		}
		return any;
	}
}
