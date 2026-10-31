package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Arrays;

import static com.example.defyingtheheavens.RealmLand.chance;

/**
 * The Jade Lake Mirage's land laid block by block (MirageLake works out its columns): pale stone below in level strata of calcite, diorite
 * and stone; the isles' and the hills' tops grass in the lake's pale jade, white flowers in it, bare calcite where the hills are steep;
 * the shore pale gravel and calcite; the lake's bed clay and sand under still clear water, lily pads by the isles. (The pavilion, the
 * bridges, the willows and the blossom trees are the works'.)
 */
final class MirageLakeLand implements RealmLand {
	@Override
	public void fill(ChunkAccess chunk, int x0, int z0, BlockPos centre, long seed) {
		MirageLake.Shape s = MirageLake.shape(seed);
		BlockState[] out = new BlockState[ModDimensions.SECRET_REALM_HEIGHT];
		LevelChunkSection[] sections = chunk.getSections();
		for (LevelChunkSection section : sections) section.acquire();
		try {
			for (int dz = 0; dz < 16; dz++) {
				for (int dx = 0; dx < 16; dx++) {
					int lx = x0 + dx - centre.getX(), lz = z0 + dz - centre.getZ();
					MirageLake.Column c = MirageLake.column(s, lx, lz);
					if (c.kind() == MirageLake.Kind.NONE) continue;
					int top = stack(s, c, lx, lz, x0 + dx, z0 + dz, out);
					RealmLand.setColumn(chunk, sections, dx, dz, out, top, c.ground(), c.kind() == MirageLake.Kind.LAKE ? MirageLake.WATER_Y : -1);
				}
			}
		} finally {
			for (LevelChunkSection section : sections) section.release();
		}
	}

	@Override
	public int ground(long seed, int lx, int lz) {
		return MirageLake.column(MirageLake.shape(seed), lx, lz).ground();
	}

	@Override
	public int water(long seed, int lx, int lz) {
		return MirageLake.column(MirageLake.shape(seed), lx, lz).kind() == MirageLake.Kind.LAKE ? MirageLake.WATER_Y : -1;
	}

	@Override
	public int centreY() { return MirageLake.ISLE_Y; }

	@Override
	public String describe(long seed, int lx, int lz) {
		return MirageLake.column(MirageLake.shape(seed), lx, lz).kind().name().toLowerCase();
	}

	/** Fills {@code out} (by height) with the column's blocks. @return the highest block set */
	private static int stack(MirageLake.Shape s, MirageLake.Column c, int lx, int lz, int x, int z, BlockState[] out) {
		Arrays.fill(out, null);
		int g = Math.min(c.ground(), out.length - 4);
		for (int y = 0; y <= g; y++) out[y] = y == 0 ? Blocks.BEDROCK.defaultBlockState() : rock(s, x, y, z);
		int top = g;
		switch (c.kind()) {
			case HEART, ISLE -> {
				for (int y = Math.max(1, g - 2); y < g; y++) out[y] = Blocks.DIRT.defaultBlockState();
				boolean rim = c.ground() <= MirageLake.WATER_Y + 1;
				out[g] = rim ? (chance(x, z, 0x3A01) < 0.5 ? Blocks.CALCITE : Blocks.GRAVEL).defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
				BlockState plant = rim ? null : plant(x, z);
				if (plant != null) out[top = g + 1] = plant;
			}
			case HILL -> {
				// Steep: the hill's white stone bare; else grass over earth.
				boolean steep = Math.abs(MirageLake.column(s, lx + 2, lz).ground() - MirageLake.column(s, lx - 2, lz).ground()) > 3
						|| Math.abs(MirageLake.column(s, lx, lz + 2).ground() - MirageLake.column(s, lx, lz - 2).ground()) > 3;
				if (!steep) {
					for (int y = Math.max(1, g - 3); y < g; y++) out[y] = Blocks.DIRT.defaultBlockState();
					out[g] = Blocks.GRASS_BLOCK.defaultBlockState();
					BlockState plant = plant(x, z);
					if (plant != null) out[top = g + 1] = plant;
				}
			}
			case SHORE -> out[g] = (chance(x, z, 0x3A02) < 0.55 ? Blocks.GRAVEL : chance(x, z, 0x3A03) < 0.5 ? Blocks.CALCITE : Blocks.DIORITE).defaultBlockState();
			case LAKE -> {
				double p = s.bed.getValue(x * 0.05, z * 0.05) + (chance(x, z, 0x3A04) - 0.5) * 0.3;
				out[g] = (p < -0.35 ? Blocks.GRAVEL : p > 0.4 ? Blocks.SAND : Blocks.CLAY).defaultBlockState();
				for (int y = g + 1; y <= MirageLake.WATER_Y; y++) out[y] = Blocks.WATER.defaultBlockState();
				top = MirageLake.WATER_Y;
				// Lily pads where the water is shallow, by the isles and the shore.
				if (MirageLake.WATER_Y - g <= 2 && chance(x, z, 0x3A05) < 0.06) out[top = MirageLake.WATER_Y + 1] = Blocks.LILY_PAD.defaultBlockState();
				else if (MirageLake.WATER_Y - g >= 3 && chance(x, z, 0x3A06) < 0.05) out[g + 1] = Blocks.SEAGRASS.defaultBlockState();
			}
			default -> {
			}
		}
		return top;
	}

	/** What grows on the isles and hills: grass, and white and pale blue flowers here and there. */
	private static BlockState plant(int x, int z) {
		double roll = chance(x, z, 0x3A10);
		if (roll < 0.22) return Blocks.GRASS.defaultBlockState();
		if (roll < 0.24) return Blocks.LILY_OF_THE_VALLEY.defaultBlockState();
		if (roll < 0.255) return Blocks.AZURE_BLUET.defaultBlockState();
		if (roll < 0.265) return Blocks.WHITE_TULIP.defaultBlockState();
		return null;
	}

	/** Pale stone in level strata: calcite, diorite and stone laid in layers (each its own roll, rising and falling gently), andesite now and then. */
	static BlockState rock(MirageLake.Shape s, int x, int y, int z) {
		double band = chance((int) Math.floor((y + 4 * s.roll.getValue(x * 0.015, z * 0.015)) / 3.0), 0, 0x3A30);
		if (y < 30) return Blocks.STONE.defaultBlockState();
		return (band < 0.4 ? Blocks.CALCITE : band < 0.65 ? Blocks.DIORITE : band < 0.75 ? Blocks.ANDESITE : Blocks.STONE).defaultBlockState();
	}
}
