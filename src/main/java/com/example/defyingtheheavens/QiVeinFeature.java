package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * One Qi Vein: 1 to {@link #MAX_SIZE} blocks grown from the origin through touching stone (deepslate gives the deepslate
 * ore). Placed as often as vanilla's diamond veins (see ModPlacedFeatures); unlike vanilla's ore blob, never more than
 * {@link #MAX_SIZE} blocks. Like diamonds, half the blocks that would lie open to the air are left out.
 */
public class QiVeinFeature extends Feature<NoneFeatureConfiguration> {
	public static final int MAX_SIZE = 4;
	private static final float DISCARD_ON_AIR = 0.5f;

	public QiVeinFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		int size = 1 + random.nextInt(MAX_SIZE);
		List<BlockPos> vein = new ArrayList<>();
		BlockPos cursor = context.origin();
		for (int attempt = 0; vein.size() < size && attempt < size * 4; attempt++) {
			if (!vein.contains(cursor) && tryPlace(level, cursor, random)) vein.add(cursor);
			// Grow from a random block already in the vein, so it stays one touching cluster.
			BlockPos from = vein.isEmpty() ? context.origin() : vein.get(random.nextInt(vein.size()));
			cursor = from.relative(Direction.getRandom(random));
		}
		return !vein.isEmpty();
	}

	private static boolean tryPlace(WorldGenLevel level, BlockPos pos, RandomSource random) {
		BlockState here = level.getBlockState(pos);
		Block ore;
		if (here.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) ore = ModBlocks.DEEPSLATE_QI_VEIN;
		else if (here.is(BlockTags.STONE_ORE_REPLACEABLES)) ore = ModBlocks.QI_VEIN;
		else return false;
		if (exposed(level, pos) && random.nextFloat() < DISCARD_ON_AIR) return false;
		return level.setBlock(pos, ore.defaultBlockState(), Block.UPDATE_CLIENTS);
	}

	private static boolean exposed(WorldGenLevel level, BlockPos pos) {
		for (Direction direction : Direction.values()) {
			if (level.getBlockState(pos.relative(direction)).isAir()) return true;
		}
		return false;
	}
}
