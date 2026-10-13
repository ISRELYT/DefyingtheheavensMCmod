package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A bare dead tree of the Netherworld (the Bone Wastes, now and then the Fields of the Other Shore): a crooked dark trunk 4-7
 * blocks tall with two to four leafless branches reaching out and up, twigs at their ends, and soul lanterns hung on chains
 * from some of them, burning cold in the dark.
 */
public class DeadTreeFeature extends Feature<NoneFeatureConfiguration> {
	public DeadTreeFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		BlockPos base = context.origin();
		BlockState ground = level.getBlockState(base.below());
		if (!(ground.is(Blocks.SOUL_SAND) || ground.is(Blocks.SOUL_SOIL) || ground.is(Blocks.BONE_BLOCK) || ground.is(Blocks.BASALT)
				|| ground.is(Blocks.MUD) || ground.is(Blocks.BLACKSTONE))) return false;
		int height = 4 + random.nextInt(4);
		for (int y = 0; y < height + 2; y++) {
			if (!level.isEmptyBlock(base.above(y))) return false;
		}
		BlockState log = Blocks.DARK_OAK_LOG.defaultBlockState();
		for (int y = 0; y < height; y++) level.setBlock(base.above(y), log, 2);
		// A broken crown: one bent stub on top.
		Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		BlockPos top = base.above(height).relative(lean);
		if (level.isEmptyBlock(top)) level.setBlock(top, Blocks.DARK_OAK_FENCE.defaultBlockState(), 2);

		int branches = 2 + random.nextInt(3);
		int lanterns = 0;
		for (int i = 0; i < branches; i++) {
			Direction way = Direction.Plane.HORIZONTAL.getRandomDirection(random);
			BlockPos at = base.above(2 + random.nextInt(Math.max(1, height - 2)));
			int length = 2 + random.nextInt(2);
			BlockState branch = log.setValue(RotatedPillarBlock.AXIS, way.getAxis());
			for (int step = 0; step < length; step++) {
				at = at.relative(way);
				if (step > 0 && random.nextBoolean()) at = at.above(); // reaching up as it goes
				if (!level.isEmptyBlock(at)) break;
				level.setBlock(at, branch, 2);
			}
			BlockPos twig = at.above();
			if (level.isEmptyBlock(twig)) level.setBlock(twig, Blocks.DARK_OAK_FENCE.defaultBlockState(), 2);
			// A soul lantern hung on a chain under the branch's end (not every branch has one).
			if (lanterns < 2 && random.nextFloat() < 0.6f) {
				BlockPos chain = at.below();
				BlockPos lantern = chain.below();
				if (level.isEmptyBlock(chain) && level.isEmptyBlock(lantern) && level.isEmptyBlock(lantern.below())) {
					level.setBlock(chain, Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y), 2);
					level.setBlock(lantern, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
					lanterns++;
				}
			}
		}
		return true;
	}
}
