package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A Sacred Vessel's altar (2026-10-25): the ruin of an ancient shrine on the Great Void's land, its vessel (SacredVesselItem) still
 * lying on its plinth (in an unseen, glowing frame: struck, it gives the vessel up), of the land's own element in a Dao field, of any
 * on the steppes. A worn round floor of blackstone and gold, four broken quartz pillars (one fallen across the floor), and a chest of
 * the offerings left there at its back ({@link #LOOT}). Only on level, dry ground with room above. Its keeper rises the first time
 * anyone comes near (SacredRelics).
 */
public class SacredRelicFeature extends Feature<NoneFeatureConfiguration> {
	public static final ResourceLocation LOOT = DefyingTheHeavens.id("chests/sacred_relic");
	/** The floor's reach from the plinth, and how far the ground under it may rise or fall. */
	private static final int RADIUS = 4, MAX_RELIEF = 3;

	public SacredRelicFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		BlockPos foot = context.origin();
		RandomSource random = context.random();
		// Level, dry ground: every column of the floor within a few blocks of the plinth's.
		int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
		for (int dx = -RADIUS; dx <= RADIUS; dx += 2) {
			for (int dz = -RADIUS; dz <= RADIUS; dz += 2) {
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, foot.getX() + dx, foot.getZ() + dz);
				if (y <= level.getMinBuildHeight() + 8) return false; // over the void
				low = Math.min(low, y);
				high = Math.max(high, y);
			}
		}
		if (high - low > MAX_RELIEF || !level.getFluidState(foot.below()).isEmpty() || !level.getFluidState(foot).isEmpty()) return false;
		QiElement field = QiElement.daoField(level.getBiome(foot));
		QiElement element = field != null ? field : QiElement.five()[random.nextInt(5)];
		int y = foot.getY();

		// The floor, worn: blackstone bricks, cracked and gilded here and there, gold in a ring round the plinth; the ground made up
		// under it, and cleared over it.
		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dz = -RADIUS; dz <= RADIUS; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > RADIUS + 0.4) continue;
				for (int down = 2; down <= 5; down++) {
					BlockPos under = new BlockPos(foot.getX() + dx, y - down, foot.getZ() + dz);
					if (level.getBlockState(under).canBeReplaced()) level.setBlock(under, Blocks.BLACKSTONE.defaultBlockState(), 2);
				}
				BlockState floor = Math.abs(d - 2) < 0.5 && (dx == 0 || dz == 0) ? Blocks.GOLD_BLOCK.defaultBlockState()
						: random.nextInt(9) == 0 ? Blocks.GILDED_BLACKSTONE.defaultBlockState()
						: random.nextInt(3) == 0 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
						: Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
				if (d > RADIUS - 0.6 && random.nextInt(4) == 0) continue; // its rim broken away here and there
				level.setBlock(new BlockPos(foot.getX() + dx, y - 1, foot.getZ() + dz), floor, 2);
				for (int up = 0; up <= 6; up++) {
					BlockPos over = new BlockPos(foot.getX() + dx, y + up, foot.getZ() + dz);
					if (!level.getBlockState(over).isAir()) level.setBlock(over, Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
		// Four pillars at the corners, broken off at heights of their own; one of them fallen.
		int fallen = random.nextInt(4), corner = 0;
		for (int sx : new int[] {-3, 3}) {
			for (int sz : new int[] {-3, 3}) {
				int height = corner == fallen ? 1 : 1 + random.nextInt(4);
				for (int up = 0; up < height; up++) {
					level.setBlock(foot.offset(sx, up, sz), Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
				}
				if (height >= 4) level.setBlock(foot.offset(sx, height, sz), Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(), 2);
				if (corner == fallen) {
					// Lying across the floor toward the middle, along x or z.
					Direction along = random.nextBoolean() ? (sx < 0 ? Direction.EAST : Direction.WEST) : (sz < 0 ? Direction.SOUTH : Direction.NORTH);
					BlockState lying = Blocks.QUARTZ_PILLAR.defaultBlockState().setValue(RotatedPillarBlock.AXIS, along.getAxis());
					for (int k = 1; k <= 2; k++) level.setBlock(foot.offset(sx, 0, sz).relative(along, k), lying, 2);
				}
				corner++;
			}
		}
		// The plinth, its vessel lying on it.
		level.setBlock(foot, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(), 2);
		GlowItemFrame frame = new GlowItemFrame(level.getLevel(), foot.above(), Direction.UP);
		frame.setItem(new ItemStack(ModItems.sacredVessel(element)), false);
		frame.setInvisible(true);
		level.addFreshEntity(frame);
		// The offerings' chest at its back.
		Direction back = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		BlockPos chest = foot.relative(back, 3);
		level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, back.getOpposite()), 2);
		RandomizableContainerBlockEntity.setLootTable(level, random, chest, LOOT);
		return true;
	}
}
