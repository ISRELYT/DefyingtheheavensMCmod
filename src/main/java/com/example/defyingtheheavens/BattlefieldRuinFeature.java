package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
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
 * What an ancient war left on the Great Void's battlefields (2026-10-25: the Ancient Battlefield land; remade 2026-10-27 at the user's
 * word: "I don't like all the swords poking out of the ground, instead, to show it's a battlefield, there should be craters and
 * destruction remnants left"). One of
 * <ul>
 *   <li>a crater ({@link #crater}, a third of them): a bowl five to twenty-five across blasted into the land, its rim thrown up of broken
 *   earth and stone, its floor burnt to blackstone and basalt; some glassed to obsidian at their hearts, some with a heart of magma still
 *   smouldering, some holding a pool of dark water; the largest cracked out round their rims; now and then two or three small ones
 *   together, where a volley fell;</li>
 *   <li>a fissure ({@link #fissure}): a jagged rift torn through the ground, its edges scorched to basalt, magma glowing in some;</li>
 *   <li>a toppled spire ({@link #spire}): the stump of a great pillar of rock blasted off at its base, the rest of it lying broken across the
 *   land;</li>
 *   <li>a field of rubble ({@link #rubble}): boulders and broken stone flung down, gravel, the blackened bricks of what stood there;</li>
 *   <li>a heap of bones and skulls;</li>
 *   <li>the stump of a broken wall of blackstone with a tattered banner on it, or a gate's arch fallen between its pillars ({@link #arch});</li>
 *   <li>a war chest half sunk in the earth by a fallen banner, its old weapons and slips ({@link #LOOT}).</li>
 * </ul>
 * Everything it lays lies within {@link #REACH} of where it is asked (as far as a feature may reach into the chunks beside its own).
 */
public class BattlefieldRuinFeature extends Feature<NoneFeatureConfiguration> {
	public static final ResourceLocation LOOT = DefyingTheHeavens.id("chests/ancient_battlefield");
	/** Nothing is laid further than this from the feature's own spot, either way along x or z. */
	static final int REACH = 16;

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
		if (roll < 0.36f) {
			// Now and then a volley: two or three small craters together rather than one.
			if (random.nextInt(5) == 0) {
				int many = 2 + random.nextInt(2);
				for (int i = 0; i < many; i++) {
					BlockPos at = foot.offset(random.nextInt(13) - 6, 0, random.nextInt(13) - 6);
					crater(level, foot, at, 2 + random.nextInt(3), random);
				}
			} else {
				crater(level, foot, foot, craterRadius(random), random);
			}
		}
		else if (roll < 0.46f) fissure(level, foot, random);
		else if (roll < 0.56f) spire(level, foot, random);
		else if (roll < 0.66f) rubble(level, foot, random);
		else if (roll < 0.78f) bones(level, foot, random);
		else if (roll < 0.88f) {
			if (random.nextBoolean()) wall(level, foot, random);
			else arch(level, foot, random);
		}
		else warChest(level, foot, random);
		return true;
	}

	/** A crater's radius: most small (two to four), many middling (five to eight), a few great (nine to twelve). */
	private static int craterRadius(RandomSource random) {
		float r = random.nextFloat();
		if (r < 0.5f) return 2 + random.nextInt(3);
		if (r < 0.85f) return 5 + random.nextInt(4);
		return 9 + random.nextInt(4);
	}

	// --- A crater ---

	/**
	 * A crater of radius {@code radius} centred on {@code at} (the ground's top there), for the feature asked at {@code foot}: a bowl,
	 * its edge ragged, half its radius deep at its middle; a rim thrown up round it; a burnt floor, its heart glassed, smouldering, or
	 * holding dark water; the great ones cracked out round their rims; stones flung out beyond.
	 */
	private static void crater(WorldGenLevel level, BlockPos foot, BlockPos at, int radius, RandomSource random) {
		int heart = random.nextInt(10);
		// 0-1: glassed to obsidian; 2-3: magma still smouldering; 4: dark water; else only burnt.
		boolean glass = heart < 2, magma = heart >= 2 && heart < 4, pool = heart == 4 && radius >= 4;
		double depth = radius * 0.45 + 1;
		int rimWidth = Math.max(1, (int) Math.round(radius * 0.3)), rimHeight = Math.max(1, (int) Math.round(radius * 0.22));
		double p1 = random.nextDouble() * Math.PI * 2, p2 = random.nextDouble() * Math.PI * 2;
		int span = radius + rimWidth + 1;
		int poolLevel = Integer.MIN_VALUE;
		for (int dx = -span; dx <= span; dx++) {
			for (int dz = -span; dz <= span; dz++) {
				int x = at.getX() + dx, z = at.getZ() + dz;
				if (!inReach(foot, x, z)) continue;
				double d = Math.sqrt(dx * dx + dz * dz);
				double a = Math.atan2(dz, dx);
				double edge = radius * (1 + 0.12 * Math.sin(3 * a + p1) + 0.07 * Math.sin(7 * a + p2));
				if (d > edge + rimWidth) continue;
				int top = ground(level, x, at.getY(), z);
				if (top == Integer.MIN_VALUE) continue;
				if (d < edge) {
					double share = d / edge;
					int floor = top - (int) Math.round(depth * (1 - share * share));
					for (int y = floor + 1; y <= top + 2; y++) set(level, new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
					BlockPos floorAt = new BlockPos(x, floor, z);
					boolean middle = share < 0.35;
					set(level, floorAt, middle && glass ? (random.nextInt(6) == 0 ? Blocks.CRYING_OBSIDIAN : Blocks.OBSIDIAN).defaultBlockState()
							: middle && magma && share < 0.25 ? Blocks.MAGMA_BLOCK.defaultBlockState() : burnt(random));
					set(level, floorAt.below(), random.nextInt(3) == 0 ? Blocks.BASALT.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState());
					if (pool && share < 0.5) poolLevel = Math.max(poolLevel, floor + 1);
				} else {
					// The rim: broken earth and stone thrown up, highest at the crater's lip.
					int h = (int) Math.round(rimHeight * (1 - (d - edge) / rimWidth));
					for (int y = top + 1; y <= top + h; y++) set(level, new BlockPos(x, y, z), y == top + h ? Blocks.COARSE_DIRT.defaultBlockState() : thrown(random));
				}
			}
		}
		// Dark water in the deepest of its floor, where the floor holds it (never spilling out over the rim).
		if (pool && poolLevel != Integer.MIN_VALUE) {
			int lid = poolLevel + Math.max(0, (int) (depth * 0.35) - 1);
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (dx * dx + dz * dz > radius * radius * 0.2 || !inReach(foot, at.getX() + dx, at.getZ() + dz)) continue;
					for (int y = poolLevel; y <= lid; y++) {
						BlockPos pos = new BlockPos(at.getX() + dx, y, at.getZ() + dz);
						if (level.getBlockState(pos).isAir() && held(level, pos)) set(level, pos, Blocks.WATER.defaultBlockState());
					}
				}
			}
		}
		// The great ones cracked out round their rims.
		if (radius >= 8) {
			int cracks = 4 + random.nextInt(4);
			for (int i = 0; i < cracks; i++) {
				double a = random.nextDouble() * Math.PI * 2;
				int length = 4 + random.nextInt(6);
				for (int k = 0; k < length; k++) {
					double out = radius + rimWidth + k;
					int x = at.getX() + (int) Math.round(Math.cos(a) * out), z = at.getZ() + (int) Math.round(Math.sin(a) * out);
					a += (random.nextDouble() - 0.5) * 0.25; // a crack wanders
					if (!inReach(foot, x, z)) break;
					int top = ground(level, x, at.getY(), z);
					if (top == Integer.MIN_VALUE) continue;
					int deep = Math.max(1, 3 - k / 3);
					for (int y = top - deep + 1; y <= top; y++) set(level, new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
					set(level, new BlockPos(x, top - deep, z), Blocks.BASALT.defaultBlockState());
				}
			}
		}
		// Stones flung out beyond its rim.
		int flung = radius / 2 + random.nextInt(3);
		for (int i = 0; i < flung; i++) {
			double a = random.nextDouble() * Math.PI * 2, out = radius + rimWidth + 1 + random.nextInt(Math.max(2, radius));
			int x = at.getX() + (int) Math.round(Math.cos(a) * out), z = at.getZ() + (int) Math.round(Math.sin(a) * out);
			if (!inReach(foot, x, z)) continue;
			int top = ground(level, x, at.getY(), z);
			if (top != Integer.MIN_VALUE) boulder(level, foot, new BlockPos(x, top + 1, z), random.nextInt(3) == 0 ? 1 : 0, random);
		}
	}

	// --- A fissure ---

	/** A jagged rift torn through the ground, deepest at its middle, its edges scorched to basalt, magma glowing in one in three. */
	private static void fissure(WorldGenLevel level, BlockPos foot, RandomSource random) {
		double a = random.nextDouble() * Math.PI;
		double ux = Math.cos(a), uz = Math.sin(a);
		int half = 9 + random.nextInt(7);
		boolean glowing = random.nextInt(3) == 0;
		double wobble = random.nextDouble() * 10;
		for (int t = -half; t <= half; t++) {
			double along = 1 - Math.abs(t) / (double) half;
			double sideways = 1.6 * Math.sin(t * 0.45 + wobble);
			int width = along > 0.5 ? 2 + random.nextInt(2) : along > 0.2 ? 1 + random.nextInt(2) : 1;
			int depth = 2 + (int) Math.round(along * (4 + random.nextInt(4)));
			for (int w = -width / 2; w <= width - 1 - width / 2; w++) {
				int x = foot.getX() + (int) Math.round(ux * t - uz * (w + sideways)), z = foot.getZ() + (int) Math.round(uz * t + ux * (w + sideways));
				if (!inReach(foot, x, z)) continue;
				int top = ground(level, x, foot.getY(), z);
				if (top == Integer.MIN_VALUE) continue;
				for (int y = top - depth + 1; y <= top + 1; y++) set(level, new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
				set(level, new BlockPos(x, top - depth, z), glowing && along > 0.3 ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.BASALT.defaultBlockState());
				// Its lips scorched.
				for (int s : new int[] {-1, 1}) {
					int lx = x + (int) Math.round(-uz * s), lz = z + (int) Math.round(ux * s);
					if (!inReach(foot, lx, lz)) continue;
					BlockPos lip = new BlockPos(lx, top, lz);
					if (!level.getBlockState(lip).isAir() && random.nextInt(3) != 0) set(level, lip, random.nextBoolean() ? Blocks.BASALT.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState());
				}
			}
		}
	}

	// --- A toppled spire ---

	/** The stump of a great pillar of rock blasted off at its base, the rest of it lying broken across the land, rubble about it. */
	private static void spire(WorldGenLevel level, BlockPos foot, RandomSource random) {
		BlockState[] rock = {Blocks.STONE.defaultBlockState(), Blocks.ANDESITE.defaultBlockState(), Blocks.TUFF.defaultBlockState(),
				Blocks.COBBLESTONE.defaultBlockState()};
		int thick = random.nextBoolean() ? 2 : 3;
		int stump = 3 + random.nextInt(4);
		// The stump, its top broken off ragged.
		for (int dx = 0; dx < thick; dx++) {
			for (int dz = 0; dz < thick; dz++) {
				int x = foot.getX() + dx, z = foot.getZ() + dz;
				int top = ground(level, x, foot.getY(), z);
				if (top == Integer.MIN_VALUE) continue;
				int h = stump - random.nextInt(3);
				for (int y = top - 1; y <= top + h; y++) set(level, new BlockPos(x, y, z), rock[random.nextInt(rock.length)]);
			}
		}
		// The rest of it fallen, lying along the land, broken into lengths.
		Direction fell = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		Direction across = fell.getClockWise();
		int length = 7 + random.nextInt(6);
		for (int k = 0; k < length; k++) {
			if (random.nextInt(6) == 0) continue; // a break between two of its lengths
			for (int w = 0; w < thick; w++) {
				BlockPos column = foot.relative(fell, thick + 1 + k).relative(across, w);
				if (!inReach(foot, column.getX(), column.getZ())) continue;
				int top = ground(level, column.getX(), foot.getY(), column.getZ());
				if (top == Integer.MIN_VALUE) continue;
				int from = k > length - 3 ? top : top + 1; // its far end sunk into the ground, the rest lying on it
				for (int y = from; y < from + thick; y++) set(level, new BlockPos(column.getX(), y, column.getZ()), rock[random.nextInt(rock.length)]);
			}
		}
		rubbleAbout(level, foot, foot, 4, 4 + random.nextInt(4), random);
	}

	// --- Rubble ---

	/** A field of rubble: boulders and broken stone flung down, gravel among them, the blackened bricks of what stood there. */
	private static void rubble(WorldGenLevel level, BlockPos foot, RandomSource random) {
		rubbleAbout(level, foot, foot, 7 + random.nextInt(4), 7 + random.nextInt(6), random);
		for (int i = 0; i < 10 + random.nextInt(10); i++) {
			int x = foot.getX() + random.nextInt(17) - 8, z = foot.getZ() + random.nextInt(17) - 8;
			if (!inReach(foot, x, z)) continue;
			int top = ground(level, x, foot.getY(), z);
			if (top == Integer.MIN_VALUE) continue;
			BlockPos at = new BlockPos(x, top, z);
			if (random.nextInt(3) == 0) set(level, at, Blocks.GRAVEL.defaultBlockState());
			else if (random.nextInt(4) == 0) set(level, at.above(), random.nextBoolean() ? Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState()
					: Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
		}
	}

	/** {@code count} boulders flung down within {@code spread} of {@code at}. */
	private static void rubbleAbout(WorldGenLevel level, BlockPos foot, BlockPos at, int spread, int count, RandomSource random) {
		for (int i = 0; i < count; i++) {
			int x = at.getX() + random.nextInt(spread * 2 + 1) - spread, z = at.getZ() + random.nextInt(spread * 2 + 1) - spread;
			if (!inReach(foot, x, z)) continue;
			int top = ground(level, x, at.getY(), z);
			if (top != Integer.MIN_VALUE) boulder(level, foot, new BlockPos(x, top + 1, z), random.nextInt(4) == 0 ? 2 : random.nextInt(2), random);
		}
	}

	/** A boulder resting at {@code at}, half sunk: one block, or a lump {@code size} across. */
	private static void boulder(WorldGenLevel level, BlockPos foot, BlockPos at, int size, RandomSource random) {
		for (int dx = -size; dx <= size; dx++) {
			for (int dz = -size; dz <= size; dz++) {
				for (int dy = -1; dy <= size; dy++) {
					if (dx * dx + dz * dz + dy * dy > size * size + 1 || !inReach(foot, at.getX() + dx, at.getZ() + dz)) continue;
					if (size > 0 && random.nextInt(5) == 0) continue; // ragged
					set(level, at.offset(dx, dy, dz), thrown(random));
				}
			}
		}
	}

	// --- What stood ---

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
			for (int up = -1; up < height; up++) set(level, base.above(up), brick(random));
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

	/** A gate's arch fallen: its two pillars standing broken, five apart, the lintel's pieces lying between them, a banner in the dirt. */
	private static void arch(WorldGenLevel level, BlockPos foot, RandomSource random) {
		Direction across = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		int gap = 4 + random.nextInt(3);
		for (int side = 0; side <= gap + 1; side += gap + 1) {
			BlockPos base = foot.relative(across, side);
			BlockPos ground = top(level, base);
			if (ground == null) continue;
			int height = 3 + random.nextInt(4);
			for (int up = -1; up < height; up++) {
				set(level, ground.above(up), brick(random));
				set(level, ground.above(up).relative(across.getClockWise()), brick(random));
			}
		}
		// The lintel's pieces where they fell.
		for (int k = 1; k <= gap; k++) {
			if (random.nextInt(3) == 0) continue;
			BlockPos at = top(level, foot.relative(across, k).relative(across.getClockWise(), random.nextInt(3) - 1));
			if (at == null) continue;
			set(level, at, random.nextBoolean() ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.GILDED_BLACKSTONE.defaultBlockState());
			if (random.nextInt(3) == 0) set(level, at.above(), Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState());
		}
		BlockPos banner = top(level, foot.relative(across, gap / 2 + 1).relative(across.getCounterClockWise(), 2));
		if (banner != null) set(level, banner, Blocks.RED_BANNER.defaultBlockState().setValue(BannerBlock.ROTATION, random.nextInt(16)));
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

	// --- The ground and its stuff ---

	/** Whether column (x, z) lies within {@link #REACH} of {@code foot} either way. */
	private static boolean inReach(BlockPos foot, int x, int z) {
		return Math.abs(x - foot.getX()) <= REACH && Math.abs(z - foot.getZ()) <= REACH;
	}

	/** The top of the ground in column (x, z) near height {@code around} (the highest solid block a little above to well below it), or MIN_VALUE. */
	private static int ground(WorldGenLevel level, int x, int around, int z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, around + 8, z);
		for (int y = around + 8; y >= around - 14; y--) {
			pos.setY(y);
			BlockState state = level.getBlockState(pos);
			if (!state.isAir() && !state.canBeReplaced() && level.getFluidState(pos).isEmpty()) return y;
		}
		return Integer.MIN_VALUE;
	}

	/** Whether water set at {@code pos} would be held there: solid ground beneath it and on its four sides, or more water. */
	private static boolean held(WorldGenLevel level, BlockPos pos) {
		if (level.getBlockState(pos.below()).isAir()) return false;
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockState next = level.getBlockState(pos.relative(side));
			if (next.isAir() && !level.getBlockState(pos.relative(side).below()).isAir()) continue; // its neighbour fills too, in turn
			if (next.isAir()) return false;
		}
		return true;
	}

	/** A crater's burnt floor. */
	private static BlockState burnt(RandomSource random) {
		int r = random.nextInt(10);
		return (r < 4 ? Blocks.BLACKSTONE : r < 6 ? Blocks.BASALT : r < 8 ? Blocks.COARSE_DIRT : r < 9 ? Blocks.GRAVEL : Blocks.COBBLED_DEEPSLATE).defaultBlockState();
	}

	/** Earth and stone thrown up out of a crater. */
	private static BlockState thrown(RandomSource random) {
		int r = random.nextInt(10);
		Block block = r < 3 ? Blocks.COARSE_DIRT : r < 5 ? Blocks.COBBLESTONE : r < 7 ? Blocks.GRAVEL : r < 8 ? Blocks.ANDESITE : r < 9 ? Blocks.BLACKSTONE : Blocks.STONE;
		return block.defaultBlockState();
	}

	/** A blackened brick of what was built here. */
	private static BlockState brick(RandomSource random) {
		return random.nextInt(3) == 0 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
				: random.nextInt(5) == 0 ? Blocks.COBBLED_DEEPSLATE.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
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

	/** Package-private for the game tests: a crater of {@code radius} laid at {@code at} (its feature asked there too). */
	static void craterForTests(WorldGenLevel level, BlockPos at, int radius, RandomSource random) {
		crater(level, at, at, radius, random);
	}
}
