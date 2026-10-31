package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Arrays;

import static com.example.defyingtheheavens.RealmLand.chance;

/**
 * The Yellow Springs Hollow's land laid block by block (SpringsHollow works out its columns): dark stone below in level strata of deepslate,
 * blackstone and basalt; the islands' tops soul soil in broad patches of coarse earth and gravel, dead bushes and red spider lilies on them;
 * the springs' bed soul sand and clay, the sunken streets' mud and gravel, under the water's murky gold; the wall's cliffs bare dark stone.
 * (The city's ruins, its bridges and the tomb are the works'.)
 */
final class SpringsHollowLand implements RealmLand {
	@Override
	public void fill(ChunkAccess chunk, int x0, int z0, BlockPos centre, long seed) {
		SpringsHollow.Shape s = SpringsHollow.shape(seed);
		BlockState[] out = new BlockState[ModDimensions.SECRET_REALM_HEIGHT];
		LevelChunkSection[] sections = chunk.getSections();
		for (LevelChunkSection section : sections) section.acquire();
		try {
			for (int dz = 0; dz < 16; dz++) {
				for (int dx = 0; dx < 16; dx++) {
					SpringsHollow.Column c = SpringsHollow.column(s, x0 + dx - centre.getX(), z0 + dz - centre.getZ());
					if (c.kind() == SpringsHollow.Kind.NONE) continue;
					int top = stack(s, c, x0 + dx, z0 + dz, out);
					RealmLand.setColumn(chunk, sections, dx, dz, out, top, c.ground(), wet(c) ? SpringsHollow.WATER_Y : -1);
				}
			}
		} finally {
			for (LevelChunkSection section : sections) section.release();
		}
	}

	private static boolean wet(SpringsHollow.Column c) {
		return c.kind() == SpringsHollow.Kind.RIVER || c.kind() == SpringsHollow.Kind.SHALLOWS;
	}

	@Override
	public int ground(long seed, int lx, int lz) {
		return SpringsHollow.column(SpringsHollow.shape(seed), lx, lz).ground();
	}

	@Override
	public int water(long seed, int lx, int lz) {
		return wet(SpringsHollow.column(SpringsHollow.shape(seed), lx, lz)) ? SpringsHollow.WATER_Y : -1;
	}

	@Override
	public int centreY() { return SpringsHollow.ISLAND_Y; }

	@Override
	public String describe(long seed, int lx, int lz) {
		return SpringsHollow.column(SpringsHollow.shape(seed), lx, lz).kind().name().toLowerCase();
	}

	/** Fills {@code out} (by height) with the column's blocks. @return the highest block set */
	private static int stack(SpringsHollow.Shape s, SpringsHollow.Column c, int x, int z, BlockState[] out) {
		Arrays.fill(out, null);
		int g = Math.min(c.ground(), out.length - 4);
		for (int y = 0; y <= g; y++) out[y] = y == 0 ? Blocks.BEDROCK.defaultBlockState() : rock(s, x, y, z);
		int top = g;
		switch (c.kind()) {
			case TOMB, ISLE -> {
				for (int y = Math.max(1, g - 2); y < g; y++) out[y] = Blocks.SOUL_SOIL.defaultBlockState();
				out[g] = top(s, x, z);
				BlockState plant = plant(x, z, out[g]);
				if (plant != null) out[top = g + 1] = plant;
			}
			case RIVER -> {
				out[g] = (chance(x, z, 0x7E01) < 0.65 ? Blocks.SOUL_SAND : Blocks.CLAY).defaultBlockState();
				for (int y = g + 1; y <= SpringsHollow.WATER_Y; y++) out[y] = Blocks.WATER.defaultBlockState();
				top = SpringsHollow.WATER_Y;
			}
			case SHALLOWS -> {
				double p = s.bed.getValue(x * 0.06, z * 0.06) + (chance(x, z, 0x7E02) - 0.5) * 0.3;
				out[g] = (p < -0.3 ? Blocks.GRAVEL : p > 0.45 ? Blocks.SOUL_SAND : Blocks.MUD).defaultBlockState();
				for (int y = g + 1; y <= SpringsHollow.WATER_Y; y++) out[y] = Blocks.WATER.defaultBlockState();
				top = SpringsHollow.WATER_Y;
			}
			case WALL -> {
				if (chance(x, z, 0x7E03) < 0.3) out[g] = Blocks.SOUL_SOIL.defaultBlockState();
			}
			default -> {
			}
		}
		return top;
	}

	/** An island's top: soul soil in broad patches of coarse earth and gravel (the land's own noise, ragged at the edges). */
	private static BlockState top(SpringsHollow.Shape s, int x, int z) {
		double p = s.roll.getValue(x * 0.05, z * 0.05) + (chance(x, z, 0x7E10) - 0.5) * 0.3;
		if (p < -0.45) return Blocks.GRAVEL.defaultBlockState();
		if (p > 0.5) return Blocks.COARSE_DIRT.defaultBlockState();
		return Blocks.SOUL_SOIL.defaultBlockState();
	}

	/** What grows on the islands: dead bushes on the coarse earth, now and then a red spider lily on the soul soil. */
	private static BlockState plant(int x, int z, BlockState under) {
		double roll = chance(x, z, 0x7E20);
		if (under.is(Blocks.COARSE_DIRT)) return roll < 0.12 ? Blocks.DEAD_BUSH.defaultBlockState() : null;
		if (under.is(Blocks.SOUL_SOIL)) return roll < 0.03 ? ModBlocks.RED_SPIDER_LILY.defaultBlockState() : null;
		return null;
	}

	/** Dark stone in level strata: deepslate, blackstone and basalt laid in layers (each its own roll, rising and falling gently), tuff now and then. */
	static BlockState rock(SpringsHollow.Shape s, int x, int y, int z) {
		double band = chance((int) Math.floor((y + 3 * s.roll.getValue(x * 0.012, z * 0.012)) / 3.0), 0, 0x7E30);
		if (y < 30) return Blocks.DEEPSLATE.defaultBlockState();
		return (band < 0.35 ? Blocks.BLACKSTONE : band < 0.55 ? Blocks.SMOOTH_BASALT : band < 0.62 ? Blocks.TUFF : Blocks.DEEPSLATE).defaultBlockState();
	}
}
