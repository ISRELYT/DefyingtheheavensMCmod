package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.SpringConfiguration;

/**
 * The Netherworld's springs (ModConfiguredFeatures#NW_SPRING_WATER): vanilla's spring, set in the rock with one way out, but only where
 * that way out hangs over the abyss, nothing beneath it down to the bottom of the world, so its water falls into the dark.
 * <p>
 * One that opened onto the land ran when its chunk first came into reach and carried off every plant in its way as an item: spider
 * lilies, crimson roots and Other-Shore Flowers, 15 to 18 blocks down a slope (316 in 245 chunks of the Fields of the Other Shore,
 * 2026-11-11). The lakes and the Yellow Springs' pools are held in and wash nothing.
 */
public class NetherworldSpringFeature extends Feature<SpringConfiguration> {
	public NetherworldSpringFeature() {
		super(SpringConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<SpringConfiguration> context) {
		WorldGenLevel level = context.level();
		BlockPos origin = context.origin();
		BlockPos way = null;
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (!level.isEmptyBlock(origin.relative(side))) continue;
			// More than one way out: vanilla's spring wants exactly one.
			if (way != null) return false;
			way = origin.relative(side);
		}
		return way != null && overTheAbyss(level, way) && Feature.SPRING.place(context);
	}

	/** Nothing but air beneath {@code pos}, down to the bottom of the world. */
	static boolean overTheAbyss(BlockGetter level, BlockPos pos) {
		BlockPos.MutableBlockPos below = pos.mutable();
		for (int y = pos.getY() - 1; y >= level.getMinBuildHeight(); y--) {
			if (!level.getBlockState(below.setY(y)).isAir()) return false;
		}
		return true;
	}
}
