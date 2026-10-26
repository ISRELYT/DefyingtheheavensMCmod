package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * What an ancient war left on the Great Void's battlefields (2026-10-25: the Ancient Battlefield land): one of
 * <ul>
 *   <li>a giant's sword driven into the ground, its blade of iron ten to sixteen high, a guard of blackstone and gold, its grip of dark
 *   wood (two in five);</li>
 *   <li>a heap of bones and skulls (one in four);</li>
 *   <li>the stump of a broken wall of blackstone, a tattered banner on it (one in five);</li>
 *   <li>a war chest half sunk in the earth by a fallen banner, its old weapons and slips ({@link #LOOT}; the rest).</li>
 * </ul>
 */
public class BattlefieldRuinFeature extends Feature<NoneFeatureConfiguration> {
	public static final ResourceLocation LOOT = DefyingTheHeavens.id("chests/ancient_battlefield");

	public BattlefieldRuinFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		BlockPos foot = context.origin();
		RandomSource random = context.random();
		if (foot.getY() <= level.getMinBuildHeight() + 8 || !level.getBlockState(foot.below()).isFaceSturdy(level, foot.below(), Direction.UP)
				|| !level.getFluidState(foot).isEmpty()) return false;
		float roll = random.nextFloat();
		if (roll < 0.4f) sword(level, foot, random);
		else if (roll < 0.65f) bones(level, foot, random);
		else if (roll < 0.85f) wall(level, foot, random);
		else warChest(level, foot, random);
		return true;
	}

	/** A giant's sword, point down in the earth. */
	private static void sword(WorldGenLevel level, BlockPos foot, RandomSource random) {
		Direction.Axis across = random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
		Direction side = across == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		int blade = 10 + random.nextInt(7), sunk = 3;
		BlockState iron = Blocks.IRON_BLOCK.defaultBlockState();
		// The blade, two wide, its tip sunk in the ground, one edge notched here and there.
		for (int up = -sunk; up < blade; up++) {
			set(level, foot.above(up), iron);
			if (up < 1 || random.nextInt(7) != 0) set(level, foot.above(up).relative(side), iron);
		}
		// The guard, six across.
		BlockPos guard = foot.above(blade);
		for (int k = -2; k <= 3; k++) {
			set(level, guard.relative(side, k), Math.abs(k - 0.5) > 2 ? Blocks.GOLD_BLOCK.defaultBlockState()
					: Blocks.POLISHED_BLACKSTONE.defaultBlockState());
		}
		// The grip, two wide as the blade, of dark wood; the pommel of gold.
		BlockState grip = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
		for (int up = 1; up <= 3; up++) {
			set(level, guard.above(up), grip);
			set(level, guard.above(up).relative(side), grip);
		}
		set(level, guard.above(4), Blocks.GOLD_BLOCK.defaultBlockState());
		set(level, guard.above(4).relative(side), Blocks.GOLD_BLOCK.defaultBlockState());
	}

	/** A heap of bones, a few skulls among them. */
	private static void bones(WorldGenLevel level, BlockPos foot, RandomSource random) {
		for (int i = 0; i < 6 + random.nextInt(6); i++) {
			BlockPos at = foot.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
			at = top(level, at);
			if (at == null) continue;
			Direction.Axis axis = Direction.Axis.values()[random.nextInt(3)];
			set(level, at, Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
		}
		for (int i = 0; i < 2 + random.nextInt(3); i++) {
			BlockPos at = top(level, foot.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3));
			if (at == null) continue;
			BlockState skull = (random.nextInt(5) == 0 ? Blocks.WITHER_SKELETON_SKULL : Blocks.SKELETON_SKULL).defaultBlockState()
					.setValue(SkullBlock.ROTATION, random.nextInt(16));
			set(level, at, skull);
		}
	}

	/** The stump of a broken wall, a tattered banner left on it. */
	private static void wall(WorldGenLevel level, BlockPos foot, RandomSource random) {
		Direction along = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		int length = 6 + random.nextInt(6);
		int highest = 0;
		BlockPos highestAt = foot;
		for (int k = 0; k < length; k++) {
			int height = Math.max(1, (int) Math.round(4 * Math.sin(Math.PI * (k + 0.5) / length) + random.nextInt(2) - 1));
			BlockPos base = foot.relative(along, k);
			for (int up = -1; up < height; up++) {
				set(level, base.above(up), random.nextInt(3) == 0 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
						: random.nextInt(5) == 0 ? Blocks.COBBLED_DEEPSLATE.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
			}
			if (height > highest) {
				highest = height;
				highestAt = base.above(height);
			}
			// Rubble at its foot.
			if (random.nextInt(3) == 0) {
				BlockPos rubble = top(level, base.relative(along.getClockWise(), random.nextBoolean() ? 1 : -1));
				if (rubble != null) set(level, rubble, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState());
			}
		}
		set(level, highestAt, (random.nextBoolean() ? Blocks.BLACK_BANNER : Blocks.RED_BANNER).defaultBlockState().setValue(BannerBlock.ROTATION, random.nextInt(16)));
	}

	/** A war chest half sunk in the earth, a fallen banner by it. */
	private static void warChest(WorldGenLevel level, BlockPos foot, RandomSource random) {
		Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		set(level, foot.below(), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
		RandomizableContainerBlockEntity.setLootTable(level, random, foot.below(), LOOT);
		set(level, foot, Blocks.COARSE_DIRT.defaultBlockState());
		BlockPos banner = top(level, foot.relative(facing, 2));
		if (banner != null) set(level, banner, Blocks.RED_BANNER.defaultBlockState().setValue(BannerBlock.ROTATION, random.nextInt(16)));
		BlockPos bones = top(level, foot.relative(facing.getClockWise(), 2));
		if (bones != null) set(level, bones, Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, facing.getClockWise().getAxis()));
	}

	/** The first open space over solid ground in {@code at}'s column, within a few blocks of it; or null. */
	private static BlockPos top(WorldGenLevel level, BlockPos at) {
		for (int dy = 3; dy >= -3; dy--) {
			BlockPos here = at.above(dy);
			if (level.getBlockState(here).canBeReplaced() && level.getFluidState(here).isEmpty()
					&& level.getBlockState(here.below()).isFaceSturdy(level, here.below(), Direction.UP)) return here;
		}
		return null;
	}

	private static void set(WorldGenLevel level, BlockPos at, BlockState state) {
		level.setBlock(at, state, 2);
	}
}
