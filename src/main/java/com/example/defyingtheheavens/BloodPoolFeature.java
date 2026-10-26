package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A blood pool on the Great Void's Ancient Battlefields (the realm plan, 2026-10-25: "Void Shattering to Entering Nirvana blood, guarded
 * by elder beasts"): where a demon race's elder bled in the old war of Heaven and the demons, and its blood never dried (the battlefields'
 * water runs red: ModBiomes). A ragged pool some sixteen across, three deep at its heart, on a bed of mud and soul soil, its shore of mud,
 * blackstone and old bones, skulls and broken spears about it; at its heart an island of blackstone bricks, red candles burning at its
 * corners, and on its altar a phial of the race's blood (DemonBloodItem, of Void Shattering to Entering Nirvana: {@link #REALM_WEIGHTS})
 * in an unseen, glowing frame (struck, it gives the phial up); stepping stones out to it from the shore, and the offerings' chest there
 * ({@link #LOOT}). Only on level, dry ground. The race's elder rises out of it the first time anyone comes near (BloodPools).
 */
public class BloodPoolFeature extends Feature<NoneFeatureConfiguration> {
	public static final ResourceLocation LOOT = DefyingTheHeavens.id("chests/blood_pool");
	/**
	 * The pool's reach (blocks each way, at most: room for its elder, drawn huge), its depth at its heart, and how far the ground under it
	 * may rise or fall: the battlefields' churned land is rarely level for twenty blocks (laid where it rose and fell no more than three,
	 * none was found: tour 7, 2026-10-25), so the blood lies at the lowest of it, cut into the rest, its shore made up where it falls away.
	 */
	static final int RADIUS = 10, DEPTH = 3, MAX_RELIEF = 6;
	/** Its blood's realm, by these weights: Void Shattering, Sacred Vessel, Entering Nirvana. */
	static final Realm[] REALMS = {Realm.VOID_SHATTERING, Realm.SACRED_VESSEL, Realm.ENTERING_NIRVANA};
	static final int[] REALM_WEIGHTS = {18, 12, 8};
	/** The races whose elders bled here. */
	static final Bloodline[] RACES = {Bloodline.TIGER, Bloodline.LIZARD, Bloodline.FOX, Bloodline.DRAGON};

	public BloodPoolFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	/** A realm for a pool's blood, by {@link #REALM_WEIGHTS}. */
	static Realm realmFor(RandomSource random) {
		int total = 0;
		for (int w : REALM_WEIGHTS) total += w;
		int roll = random.nextInt(total);
		for (int i = 0; i < REALMS.length; i++) {
			roll -= REALM_WEIGHTS[i];
			if (roll < 0) return REALMS[i];
		}
		return REALMS[0];
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		BlockPos foot = context.origin();
		RandomSource random = context.random();
		// Dry ground over land (not the void beyond a rim), rising and falling no more than MAX_RELIEF: every few columns of it. The blood
		// lies at the lowest of its middle.
		int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE, inner = Integer.MAX_VALUE;
		for (int dx = -RADIUS; dx <= RADIUS; dx += 2) {
			for (int dz = -RADIUS; dz <= RADIUS; dz += 2) {
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, foot.getX() + dx, foot.getZ() + dz);
				if (y <= level.getMinBuildHeight() + 8) return false;
				low = Math.min(low, y);
				high = Math.max(high, y);
				if (dx * dx + dz * dz <= 36) inner = Math.min(inner, y);
			}
		}
		if (high - low > MAX_RELIEF || !level.getFluidState(foot.below()).isEmpty() || !level.getFluidState(foot).isEmpty()) return false;
		int y = inner;
		foot = new BlockPos(foot.getX(), y, foot.getZ());

		// Its shape: a ragged ellipse turned any way; how deep each column of it is (0: dry), three at its heart.
		double rx = 7 + random.nextDouble() * 2, rz = 7 + random.nextDouble() * 2, turn = random.nextDouble() * Math.PI;
		double cos = Math.cos(turn), sin = Math.sin(turn), wobble = random.nextDouble() * Math.PI * 2;
		int span = RADIUS + 2, size = 2 * span + 1;
		int[][] depth = new int[size][size];
		for (int dx = -span; dx <= span; dx++) {
			for (int dz = -span; dz <= span; dz++) {
				double ax = dx * cos + dz * sin, az = -dx * sin + dz * cos;
				double r = Math.sqrt(ax * ax / (rx * rx) + az * az / (rz * rz));
				r *= 1 + 0.12 * Math.sin(Math.atan2(az, ax) * 3 + wobble); // ragged
				if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) continue; // the island
				depth[dx + span][dz + span] = r < 0.5 ? DEPTH : r < 0.8 ? 2 : r < 1 ? 1 : 0;
			}
		}
		// The way out to the island (stepping stones that way), and the shore where the chest stands at its end: any way.
		Direction toward = Direction.Plane.HORIZONTAL.getRandomDirection(random);

		// The pool: cleared above, its blood (water: the land's red), its bed of mud and soul soil, made up below where hollow.
		for (int dx = -span; dx <= span; dx++) {
			for (int dz = -span; dz <= span; dz++) {
				int d = depth[dx + span][dz + span];
				int x = foot.getX() + dx, z = foot.getZ() + dz;
				if (d > 0) {
					for (int up = 0; up <= MAX_RELIEF + 3; up++) set(level, new BlockPos(x, y + up, z), Blocks.AIR.defaultBlockState());
					for (int k = 1; k <= d; k++) set(level, new BlockPos(x, y - k, z), Blocks.WATER.defaultBlockState());
					set(level, new BlockPos(x, y - d - 1, z), random.nextInt(3) == 0 ? Blocks.SOUL_SOIL.defaultBlockState() : Blocks.MUD.defaultBlockState());
					for (int k = d + 2; k <= d + 4; k++) solidIfHollow(level, new BlockPos(x, y - k, z), Blocks.BLACKSTONE.defaultBlockState());
				} else if (wetNear(depth, dx + span, dz + span)) {
					// The shore: holding the blood in (solid round it, as deep as the pool, and down to the ground where it falls away), its
					// top of mud, blackstone and bones.
					for (int k = 2; k <= DEPTH + 1 + MAX_RELIEF; k++) {
						BlockPos below = new BlockPos(x, y - k, z);
						if (k > DEPTH + 1 && !level.getBlockState(below).canBeReplaced()) break;
						solidIfHollow(level, below, Blocks.BLACKSTONE.defaultBlockState());
					}
					set(level, new BlockPos(x, y - 1, z), shore(random));
					BlockPos over = new BlockPos(x, y, z);
					if (level.getBlockState(over).canBeReplaced()) set(level, over, Blocks.AIR.defaultBlockState());
				}
			}
		}

		// The island at its heart, its altar and the phial on it, a red candle burning at each corner.
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				for (int k = 1; k <= DEPTH + 2; k++) set(level, foot.offset(dx, -k, dz), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
				for (int up = 0; up <= MAX_RELIEF + 3; up++) set(level, foot.offset(dx, up, dz), Blocks.AIR.defaultBlockState());
				if (dx != 0 && dz != 0) {
					set(level, foot.offset(dx, 0, dz), Blocks.RED_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + random.nextInt(4))
							.setValue(CandleBlock.LIT, true));
				}
			}
		}
		set(level, foot, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
		GlowItemFrame frame = new GlowItemFrame(level.getLevel(), foot.above(), Direction.UP);
		frame.setItem(DemonBloodItem.of(RACES[random.nextInt(RACES.length)], realmFor(random)), false);
		frame.setInvisible(true);
		level.addFreshEntity(frame);

		// Stepping stones out from the island, just over the blood, to the shore where the chest stands, facing the altar.
		int out = 2;
		while (out <= span && depth[toward.getStepX() * out + span][toward.getStepZ() * out + span] > 0) {
			if (out % 2 == 0) {
				for (int k = 1; k <= DEPTH + 1; k++) set(level, foot.relative(toward, out).below(k), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
			}
			out++;
		}
		BlockPos chest = foot.relative(toward, Math.min(out + 1, span));
		chest = new BlockPos(chest.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, chest.getX(), chest.getZ()), chest.getZ());
		set(level, chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, toward.getOpposite()));
		RandomizableContainerBlockEntity.setLootTable(level, random, chest, LOOT);

		// What the old war left about it: skulls, broken spears stuck in the ground, bones.
		for (int i = 0; i < 6 + random.nextInt(5); i++) {
			double angle = random.nextDouble() * Math.PI * 2, reach = Math.max(rx, rz) + 1 + random.nextDouble() * 2.5;
			int x = foot.getX() + (int) Math.round(Math.cos(angle) * reach), z = foot.getZ() + (int) Math.round(Math.sin(angle) * reach);
			BlockPos at = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
			if (!level.getBlockState(at).canBeReplaced() || !level.getFluidState(at.below()).isEmpty() || at.equals(chest)) continue;
			switch (random.nextInt(4)) {
				case 0 -> set(level, at, (random.nextInt(4) == 0 ? Blocks.WITHER_SKELETON_SKULL : Blocks.SKELETON_SKULL).defaultBlockState()
						.setValue(SkullBlock.ROTATION, random.nextInt(16)));
				case 1 -> {
					int tall = 1 + random.nextInt(2);
					for (int k = 0; k < tall; k++) set(level, at.above(k), Blocks.IRON_BARS.defaultBlockState());
				}
				case 2 -> set(level, at.below(), Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS,
						random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z));
				default -> set(level, at, Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS,
						random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z));
			}
		}
		return true;
	}

	/** Whether column (i, j) of {@code depth}, dry, has blood beside it (any of its eight neighbours). */
	private static boolean wetNear(int[][] depth, int i, int j) {
		for (int di = -1; di <= 1; di++) {
			for (int dj = -1; dj <= 1; dj++) {
				int a = i + di, b = j + dj;
				if (a >= 0 && b >= 0 && a < depth.length && b < depth.length && depth[a][b] > 0) return true;
			}
		}
		return false;
	}

	/** A block of the pool's shore: mud mostly, blackstone, now and then a bone. */
	private static BlockState shore(RandomSource random) {
		int roll = random.nextInt(10);
		if (roll < 5) return Blocks.MUD.defaultBlockState();
		if (roll < 8) return Blocks.BLACKSTONE.defaultBlockState();
		if (roll < 9) return Blocks.COARSE_DIRT.defaultBlockState();
		return Blocks.BONE_BLOCK.defaultBlockState();
	}

	private static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, 2);
	}

	/** {@code state} at {@code pos} if what is there can't hold the blood in (air, a plant, a fluid). */
	private static void solidIfHollow(WorldGenLevel level, BlockPos pos, BlockState state) {
		BlockState there = level.getBlockState(pos);
		if (there.canBeReplaced() || !there.getFluidState().isEmpty()) set(level, pos, state);
	}
}
