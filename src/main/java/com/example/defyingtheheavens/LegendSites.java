package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * The legends' resting places (Legends#lay), laid on the land as it is the first time someone comes near: Ganjiang's sword tomb on its
 * mountain ({@link #tomb}), Moye's yin pool among the Other Shore's flowers ({@link #pool}); phase 3b's, the Xuanyuan Sword's jade shrine
 * on the Jade Firmament's highest peak ({@link #shrine}), the dead sun's crater on a barren world of the Astral Realm ({@link #crater}),
 * and the Sea-Fixing Needle standing as a pillar over the Immortal Realm's sea of clouds ({@link #pillar}). Each gives back its floor, the
 * block the legend's stone stands on (Legends#putInStone; the pillar's, its foot).
 */
final class LegendSites {
	/** The tomb's ring of paving, its pillars standing on its edge. */
	static final int TOMB_RADIUS = 7;
	/** The old blades standing in the tomb's paving, and how near the stone the nearest stands. */
	static final int BLADES = 22, BLADES_FROM = 3;
	/** The pool's water, its rim one further, the dark soil and the flowers about it two further. */
	static final int POOL_RADIUS = 4;
	/** Ground is filled in under a site's paving this far down at most (a mountainside falling away under it). */
	static final int FILL = 24;
	/** A site whose column holds no land looks this far about it for some (#ground). */
	static final int SEEK = 48;

	/**
	 * The ground at or near (x, z): the top block of the land there; where the column holds none (a gap in the Netherworld's land, which its
	 * biome search may land on: the first tour laid Moye's pool at the very bottom of the world, out of anyone's reach), that of the nearest
	 * column within {@link #SEEK} that does, rings outward.
	 */
	static BlockPos ground(ServerLevel level, int x, int z) {
		int bottom = level.getMinBuildHeight() + 4;
		for (int r = 0; r <= SEEK; r += 4) {
			int steps = r == 0 ? 1 : 2 * r;
			for (int i = 0; i < steps; i++) {
				double angle = Math.PI * 2 * i / steps;
				int cx = x + (int) Math.round(Math.cos(angle) * r), cz = z + (int) Math.round(Math.sin(angle) * r);
				int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz) - 1;
				if (top > bottom) return new BlockPos(cx, top, cz);
			}
		}
		return new BlockPos(x, Math.max(bottom, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1), z);
	}

	/**
	 * Ganjiang's sword tomb at (x, z): a level ring of old paving cut into the mountain (stone brick, mossy and cracked), eight broken pillars
	 * on its edge with lanterns on the whole ones, old bronze-hilted blades standing point down all about it, and the stone in its middle.
	 */
	static BlockPos tomb(ServerLevel level, int x, int z) {
		BlockPos floor = ground(level, x, z);
		RandomSource random = RandomSource.create(level.getSeed() ^ floor.asLong());
		for (int dx = -TOMB_RADIUS; dx <= TOMB_RADIUS; dx++) {
			for (int dz = -TOMB_RADIUS; dz <= TOMB_RADIUS; dz++) {
				if (dx * dx + dz * dz > (TOMB_RADIUS + 0.5) * (TOMB_RADIUS + 0.5)) continue;
				BlockPos at = floor.offset(dx, 0, dz);
				level.setBlock(at, dx * dx + dz * dz <= 2 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : paving(random), 2);
				fillUnder(level, at, Blocks.STONE.defaultBlockState());
				for (int up = 1; up <= 12; up++) level.setBlock(at.above(up), Blocks.AIR.defaultBlockState(), 2);
			}
		}
		// Eight pillars on the ring's edge, some broken short, the whole ones with a lantern.
		for (int i = 0; i < 8; i++) {
			double angle = Math.PI * 2 * i / 8;
			BlockPos base = floor.offset((int) Math.round(Math.cos(angle) * TOMB_RADIUS), 1, (int) Math.round(Math.sin(angle) * TOMB_RADIUS));
			boolean whole = i % 2 == 0 || random.nextInt(3) == 0;
			int height = whole ? 4 : 1 + random.nextInt(3);
			for (int j = 0; j < height; j++) {
				BlockState block = j == 0 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
						: random.nextInt(4) == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
				level.setBlock(base.above(j), block, 2);
			}
			if (whole) level.setBlock(base.above(height), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false), 2);
			else if (random.nextBoolean()) level.setBlock(base.above(height), Blocks.STONE_BRICK_SLAB.defaultBlockState(), 2);
		}
		// The tomb's old blades, standing point down in the paving between the stone and the pillars.
		Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		for (int i = 0, tries = 0; i < BLADES && tries < BLADES * 6; tries++) {
			int dx = random.nextInt(TOMB_RADIUS * 2 - 1) - (TOMB_RADIUS - 1), dz = random.nextInt(TOMB_RADIUS * 2 - 1) - (TOMB_RADIUS - 1);
			int d2 = dx * dx + dz * dz;
			if (d2 < BLADES_FROM * BLADES_FROM || d2 > (TOMB_RADIUS - 1) * (TOMB_RADIUS - 1)) continue;
			BlockPos at = floor.offset(dx, 1, dz);
			if (!level.getBlockState(at).isAir()) continue;
			level.setBlock(at, ModBlocks.BURIED_BLADE.defaultBlockState().setValue(BuriedBladeBlock.FACING, facings[random.nextInt(4)])
					.setValue(BuriedBladeBlock.TILTED, random.nextInt(3) == 0), 2);
			i++;
		}
		return floor;
	}

	/** Old paving: stone brick, mossy here and cracked there. */
	private static BlockState paving(RandomSource random) {
		int roll = random.nextInt(10);
		return roll < 2 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : roll < 4 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
				: Blocks.STONE_BRICKS.defaultBlockState();
	}

	/**
	 * Moye's yin pool at (x, z): still dark water two deep in a rim of blackstone, a single stone of blackstone in its middle for her sword's
	 * stone to stand on, a ring of soul soil about it with red spider lilies, and soul lanterns on four posts.
	 */
	static BlockPos pool(ServerLevel level, int x, int z) {
		BlockPos floor = ground(level, x, z);
		RandomSource random = RandomSource.create(level.getSeed() ^ floor.asLong());
		int reach = POOL_RADIUS + 3;
		for (int dx = -reach; dx <= reach; dx++) {
			for (int dz = -reach; dz <= reach; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > reach + 0.5) continue;
				BlockPos at = floor.offset(dx, 0, dz);
				for (int up = 1; up <= 6; up++) level.setBlock(at.above(up), Blocks.AIR.defaultBlockState(), 2);
				if (dx == 0 && dz == 0) {
					level.setBlock(at, Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
					level.setBlock(at.below(), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 2);
					level.setBlock(at.below(2), Blocks.BLACKSTONE.defaultBlockState(), 2);
				} else if (d <= POOL_RADIUS + 0.5) {
					level.setBlock(at, Blocks.WATER.defaultBlockState(), 2);
					level.setBlock(at.below(), Blocks.WATER.defaultBlockState(), 2);
					level.setBlock(at.below(2), Blocks.BLACKSTONE.defaultBlockState(), 2);
					fillUnder(level, at.below(2), Blocks.BLACKSTONE.defaultBlockState());
				} else if (d <= POOL_RADIUS + 1.5) {
					level.setBlock(at, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 2);
					level.setBlock(at.below(), Blocks.BLACKSTONE.defaultBlockState(), 2);
					level.setBlock(at.below(2), Blocks.BLACKSTONE.defaultBlockState(), 2);
					fillUnder(level, at.below(2), Blocks.BLACKSTONE.defaultBlockState());
				} else {
					level.setBlock(at, Blocks.SOUL_SOIL.defaultBlockState(), 2);
					fillUnder(level, at, Blocks.SOUL_SOIL.defaultBlockState());
					if (random.nextInt(5) < 2) level.setBlock(at.above(), ModBlocks.RED_SPIDER_LILY.defaultBlockState(), 2);
				}
			}
		}
		// Soul lanterns on four posts, at the corners between the compass points.
		for (int i = 0; i < 4; i++) {
			double angle = Math.PI / 4 + Math.PI / 2 * i;
			BlockPos base = floor.offset((int) Math.round(Math.cos(angle) * (POOL_RADIUS + 2.4)), 1, (int) Math.round(Math.sin(angle) * (POOL_RADIUS + 2.4)));
			level.setBlock(base, Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState(), 2);
			level.setBlock(base.above(), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState(), 2);
			level.setBlock(base.above(2), Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false), 2);
		}
		return floor;
	}

	// --- Phase 3b's ---

	/** The Xuanyuan Sword's shrine: its terrace, its pillars standing on its edge. */
	static final int SHRINE_RADIUS = 6;
	/** Its peak is sought this far out from the Jade Firmament's middle, on a grid this far apart, then finer about the highest found. */
	static final int PEAK_REACH = 640, PEAK_STEP = 32;
	/** The dead sun's crater: its bowl, how deep its middle lies under the land about it, and its rim's breadth. */
	static final int CRATER_RADIUS = 14, CRATER_DEPTH = 6, CRATER_RIM = 3;
	/** The Sea-Fixing Needle's pillar: how far it stands out of the sea of clouds and down into it, and its breadth. */
	static final int PILLAR_OVER = 40, PILLAR_UNDER = 12;
	static final double PILLAR_RADIUS = 2.2;
	/** Its bands of gold, at its top and just out of the clouds: this many blocks deep. */
	static final int BAND = 3;
	/** The pillar stands this far out from the Southern Heavenly Gate, over open sea, and this far at least from the Kun's own sea. */
	static final int SEA_MIN = 600, SEA_MAX = 1400, KUN_KEEP = 400;

	/** Where the Xuanyuan Sword rests: the highest peak of the Jade Firmament within {@link #PEAK_REACH} of its heart. */
	static BlockPos peak(ServerLevel level) {
		RandomState state = level.getChunkSource().randomState();
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		int[] best = PrimordialBeasts.highest(level, state, generator, 0, 0, PEAK_REACH, PEAK_STEP);
		best = PrimordialBeasts.highest(level, state, generator, best[0], best[2], PEAK_STEP, 4);
		return new BlockPos(best[0], level.getMinBuildHeight(), best[2]);
	}

	/**
	 * The Xuanyuan Sword's shrine at (x, z): a round terrace of white jade (quartz) inlaid with green jade cut level into the peak, eight white
	 * pillars on its edge with lanterns on them, and a dais in its middle, its heart a block of gold, the stone standing on it. Gives back the
	 * gold.
	 */
	static BlockPos shrine(ServerLevel level, int x, int z) {
		BlockPos floor = ground(level, x, z);
		BlockState jade = ModBlocks.JADE_STONE.defaultBlockState();
		for (int dx = -SHRINE_RADIUS; dx <= SHRINE_RADIUS; dx++) {
			for (int dz = -SHRINE_RADIUS; dz <= SHRINE_RADIUS; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > SHRINE_RADIUS + 0.5) continue;
				BlockPos at = floor.offset(dx, 0, dz);
				boolean inlay = d >= 2.5 && d < 3.5 || d >= SHRINE_RADIUS - 0.5;
				level.setBlock(at, inlay ? jade : Blocks.QUARTZ_BRICKS.defaultBlockState(), 2);
				fillUnder(level, at, jade);
				for (int up = 1; up <= 12; up++) level.setBlock(at.above(up), Blocks.AIR.defaultBlockState(), 2);
			}
		}
		// The dais: a step of white jade about the heart of gold the stone stands on.
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				level.setBlock(floor.offset(dx, 1, dz), dx == 0 && dz == 0 ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState(), 2);
			}
		}
		// Eight white pillars on the terrace's edge, a lantern on each.
		for (int i = 0; i < 8; i++) {
			double angle = Math.PI * 2 * i / 8 + Math.PI / 8;
			BlockPos base = floor.offset((int) Math.round(Math.cos(angle) * (SHRINE_RADIUS - 1)), 1, (int) Math.round(Math.sin(angle) * (SHRINE_RADIUS - 1)));
			level.setBlock(base, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(), 2);
			for (int j = 1; j <= 3; j++) level.setBlock(base.above(j), Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
			level.setBlock(base.above(4), Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(), 2);
			level.setBlock(base.above(5), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false), 2);
		}
		return floor.above();
	}

	/**
	 * Where the dead sun fell (the Sun-Shooting Bow's): on the barren or ashen world nearest the Overworld (a moon of it, it may be), on its
	 * east-facing side, which is its land's middle in the surfaces (PlanetTerrain: longitude and latitude nought). None of either kind (it
	 * never happens), the nearest world of any.
	 */
	static BlockPos deadStar(ServerLevel level) {
		AstralPlanets planets = AstralPlanets.forSeed(AstralPlanets.astralSeed(level.getServer().getWorldData().worldGenOptions().seed()));
		AstralPlanets.Planet best = null, any = null;
		for (AstralPlanets.Planet planet : planets.planets()) {
			if (planet.isOverworld()) continue;
			double d = planet.pos().distanceToSqr(AstralPlanets.OVERWORLD_POS);
			if (any == null || d < any.pos().distanceToSqr(AstralPlanets.OVERWORLD_POS)) any = planet;
			boolean dead = planet.kind() == AstralPlanets.Kind.BARREN || planet.kind() == AstralPlanets.Kind.ASHEN;
			if (dead && (best == null || d < best.pos().distanceToSqr(AstralPlanets.OVERWORLD_POS))) best = planet;
		}
		AstralPlanets.Planet fell = best != null ? best : any;
		return fell == null ? new BlockPos(0, level.getMinBuildHeight(), 0) : new BlockPos(fell.landX(), level.getMinBuildHeight(), fell.landZ());
	}

	/**
	 * The dead sun's crater at (x, z): a bowl {@link #CRATER_DEPTH} deep of basalt and blackstone, magma smouldering in it and some fires
	 * still burning, its rim thrown up about it, and in its middle the sun's cinder, gilded blackstone on a ring of magma, the stone standing
	 * on it. Gives back the cinder.
	 */
	static BlockPos crater(ServerLevel level, int x, int z) {
		BlockPos ground = ground(level, x, z);
		RandomSource random = RandomSource.create(level.getSeed() ^ ground.asLong());
		int reach = CRATER_RADIUS + CRATER_RIM, top = ground.getY();
		for (int dx = -reach; dx <= reach; dx++) {
			for (int dz = -reach; dz <= reach; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > reach + 0.5) continue;
				BlockPos column = new BlockPos(x + dx, top, z + dz);
				if (d <= CRATER_RADIUS) {
					int bowl = top - (int) Math.round(CRATER_DEPTH * (1 - d * d / (CRATER_RADIUS * CRATER_RADIUS)));
					for (int y = bowl + 1; y <= top + 12; y++) level.setBlock(column.atY(y), Blocks.AIR.defaultBlockState(), 2);
					BlockPos at = column.atY(bowl);
					level.setBlock(at, cinder(random, d), 2);
					level.setBlock(at.below(), Blocks.BLACKSTONE.defaultBlockState(), 2);
					fillUnder(level, at.below(), Blocks.BLACKSTONE.defaultBlockState());
					// Fires still burning on the magma, away from the middle.
					if (d > 4 && level.getBlockState(at).is(Blocks.MAGMA_BLOCK) && random.nextInt(3) == 0) {
						level.setBlock(at.above(), Blocks.FIRE.defaultBlockState(), 2);
					}
				} else {
					// The rim thrown up, highest just outside the bowl.
					int raised = d <= CRATER_RADIUS + 1.5 ? 2 : 1;
					for (int y = top + 1; y <= top + raised; y++) {
						level.setBlock(column.atY(y), random.nextInt(3) == 0 ? Blocks.BASALT.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState(), 2);
					}
					for (int y = top + raised + 1; y <= top + 12; y++) level.setBlock(column.atY(y), Blocks.AIR.defaultBlockState(), 2);
					fillUnder(level, column.atY(top + 1), Blocks.BLACKSTONE.defaultBlockState());
				}
			}
		}
		// The cinder: gilded blackstone on a ring of magma, a step up from the bowl's floor.
		int floorY = top - CRATER_DEPTH;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				level.setBlock(new BlockPos(x + dx, floorY + 1, z + dz), dx == 0 && dz == 0 ? Blocks.GILDED_BLACKSTONE.defaultBlockState()
						: Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
			}
		}
		return new BlockPos(x, floorY + 1, z);
	}

	/** The crater's floor, {@code d} out from its middle: basalt and blackstone, magma smouldering here and there, more of it nearer the middle. */
	private static BlockState cinder(RandomSource random, double d) {
		int roll = random.nextInt(10);
		if (roll < (d < CRATER_RADIUS / 2.0 ? 3 : 1)) return Blocks.MAGMA_BLOCK.defaultBlockState();
		return roll < 5 ? Blocks.BASALT.defaultBlockState() : roll < 7 ? Blocks.SMOOTH_BASALT.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
	}

	/**
	 * Where the Sea-Fixing Needle stands: by the world's seed, {@link #SEA_MIN} to {@link #SEA_MAX} out from the Southern Heavenly Gate, over
	 * open sea of clouds (no land near: as the Kun's lair is sought, PrimordialBeasts#kunLair), clear of the realm's people and
	 * {@link #KUN_KEEP} at least from the Kun's own sea; failing those, a narrower sea; failing all, the last tried.
	 */
	static BlockPos openSea(ServerLevel level) {
		RandomState state = level.getChunkSource().randomState();
		RandomSource random = RandomSource.create(level.getSeed() ^ 0x5EA5EEDL);
		BlockPos kun = PrimordialBeasts.findLair(level, BeastKind.KUN_PENG);
		BlockPos at = new BlockPos(ImmortalGate.HEAVEN_X + SEA_MIN, level.getMinBuildHeight(), ImmortalGate.HEAVEN_Z);
		for (int pass = 0; pass < 2; pass++) {
			for (int t = 0; t < 96; t++) {
				double angle = random.nextDouble() * Math.PI * 2, out = SEA_MIN + random.nextDouble() * (SEA_MAX - SEA_MIN);
				int x = (int) Math.round(ImmortalGate.HEAVEN_X + Math.cos(angle) * out), z = (int) Math.round(ImmortalGate.HEAVEN_Z + Math.sin(angle) * out);
				if (!PrimordialBeasts.clearOfPeople(state, x, z) || Math.hypot(x - kun.getX(), z - kun.getZ()) < KUN_KEEP) continue;
				at = new BlockPos(x, level.getMinBuildHeight(), z);
				if (PrimordialBeasts.kunLair(state, x, z, pass > 0) != null) return at;
			}
		}
		DefyingTheHeavens.LOGGER.warn("No open sea found for the Sea-Fixing Needle; it stands at {}", at.toShortString());
		return at;
	}

	/**
	 * The Sea-Fixing Needle standing as a pillar at (x, z): black iron, {@link #PILLAR_OVER} out of the sea of clouds and {@link #PILLAR_UNDER}
	 * down into it, banded with gold at its top and just out of the clouds. Gives back its foot.
	 */
	static BlockPos pillar(ServerLevel level, int x, int z) {
		int top = ModDimensions.IMMORTAL_CLOUD_Y + PILLAR_OVER, bottom = ModDimensions.IMMORTAL_CLOUD_Y - PILLAR_UNDER;
		int r = (int) Math.ceil(PILLAR_RADIUS);
		for (int y = bottom; y <= top; y++) {
			boolean band = y > top - BAND || y > ModDimensions.IMMORTAL_CLOUD_Y && y <= ModDimensions.IMMORTAL_CLOUD_Y + BAND;
			BlockState block = ModBlocks.SEA_PILLAR.defaultBlockState().setValue(SeaPillarBlock.BANDED, band);
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (dx * dx + dz * dz <= PILLAR_RADIUS * PILLAR_RADIUS) level.setBlock(new BlockPos(x + dx, y, z + dz), block, 2);
				}
			}
		}
		return new BlockPos(x, bottom, z);
	}

	/** The pillar at {@code foot} gone from over the sea (the Needle pulled out of it: Legends#pull). */
	static void unbuildPillar(ServerLevel level, BlockPos foot) {
		int top = ModDimensions.IMMORTAL_CLOUD_Y + PILLAR_OVER, r = (int) Math.ceil(PILLAR_RADIUS);
		for (int y = foot.getY(); y <= top; y++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					BlockPos at = new BlockPos(foot.getX() + dx, y, foot.getZ() + dz);
					if (level.getBlockState(at).is(ModBlocks.SEA_PILLAR)) level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
	}

	/** Ground filled in under {@code at} with {@code fill} down to whatever stands there already, {@link #FILL} at most. */
	private static void fillUnder(ServerLevel level, BlockPos at, BlockState fill) {
		for (int down = 1; down <= FILL; down++) {
			BlockPos under = at.below(down);
			BlockState there = level.getBlockState(under);
			if (!there.canBeReplaced() && there.getFluidState().isEmpty()) return;
			level.setBlock(under, fill, 2);
		}
	}

	private LegendSites() {}
}
