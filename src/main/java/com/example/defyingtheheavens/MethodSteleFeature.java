package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A Method Stele standing on a small worn plinth of blackstone bricks somewhere in the Netherworld, its method picked by the
 * methods' rarity (GhostMethod#steleWeight: the fire's steles most often, then the steps', the Yellow Springs Marrow Art's, now
 * and then the Ghost Core Canon's). The Netherworld Breathing Art is on none of them: only on the Ghost Gate's plaza (the
 * user's rule, 2026-10-19). The deeper foundations are carved only on the Nine Nether Peaks, and seldom (GhostMethod#peakWeight:
 * of 771 steles there, 40 hold the Ghost Core Canon, 20 the Nine Nether Soul Sutra, 8 Yama's Judgement Scripture, and 3 the Six
 * Paths Reincarnation Canon).
 */
public class MethodSteleFeature extends Feature<NoneFeatureConfiguration> {
	/** No other stele within this many blocks across (within the chunks a feature may touch) and this many up or down. */
	private static final int SPACING = 12, SPACING_HEIGHT = 8;

	public MethodSteleFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		BlockPos foot = context.origin();
		RandomSource random = context.random();
		if (!level.getBlockState(foot.below()).isFaceSturdy(level, foot.below(), Direction.UP)) return false;
		for (int dy = 0; dy < 4; dy++) {
			if (!level.getBlockState(foot.above(dy)).canBeReplaced()) return false;
		}
		// Two neighbouring chunks' steles may not stand side by side.
		for (BlockPos near : BlockPos.betweenClosed(foot.offset(-SPACING, -SPACING_HEIGHT, -SPACING), foot.offset(SPACING, SPACING_HEIGHT, SPACING))) {
			if (level.getBlockState(near).is(ModBlocks.METHOD_STELE)) return false;
		}
		GhostMethod chosen = pick(random, level.getBiome(foot).is(ModBiomes.NINE_NETHER_PEAKS));
		// The plinth: three by three, a few bricks missing or cracked with age.
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos at = foot.offset(dx, -1, dz);
				if (random.nextInt(6) == 0) continue;
				level.setBlock(at, (random.nextInt(4) == 0 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS : Blocks.POLISHED_BLACKSTONE_BRICKS)
						.defaultBlockState(), 2);
			}
		}
		level.setBlock(foot.below(), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(), 2);
		MethodSteleBlock.place(level, foot, chosen, Direction.Plane.HORIZONTAL.getRandomDirection(random));
		Rumours.noteStele(level.getLevel().dimension(), foot.above(), chosen); // the dead will tell of it
		return true;
	}

	/** A stele's method, by the methods' rarity across the Netherworld, or on the Nine Nether Peaks ({@code peaks}) by theirs there. */
	static GhostMethod pick(RandomSource random, boolean peaks) {
		int total = 0;
		for (GhostMethod method : GhostMethod.values()) total += peaks ? method.peakWeight() : method.steleWeight();
		int roll = random.nextInt(total);
		for (GhostMethod method : GhostMethod.values()) {
			roll -= peaks ? method.peakWeight() : method.steleWeight();
			if (roll < 0) return method;
		}
		return GhostMethod.GHOST_FIRE_SCRIPTURE;
	}
}
