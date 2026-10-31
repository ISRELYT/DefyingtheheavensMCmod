package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Arrays;

import static com.example.defyingtheheavens.RealmLand.chance;

/**
 * The Ruins of a Fallen Holy Land's land laid block by block (FallenHolyLand works out its columns): each shard's top pale grass gone silver
 * over earth, its old paving of white stone showing through in patches; under it calcite and stone in level strata; its underside, torn
 * from the land below, deepslate and blackstone veined with the void's crying obsidian and amethyst. Nothing between the shards.
 * (The halls, the eyes and the currents are the works'.)
 */
final class FallenHolyLandLand implements RealmLand {
	@Override
	public void fill(ChunkAccess chunk, int x0, int z0, BlockPos centre, long seed) {
		FallenHolyLand.Shape s = FallenHolyLand.shape(seed);
		BlockState[] out = new BlockState[ModDimensions.SECRET_REALM_HEIGHT];
		LevelChunkSection[] sections = chunk.getSections();
		for (LevelChunkSection section : sections) section.acquire();
		try {
			for (int dz = 0; dz < 16; dz++) {
				for (int dx = 0; dx < 16; dx++) {
					FallenHolyLand.Column c = FallenHolyLand.column(s, x0 + dx - centre.getX(), z0 + dz - centre.getZ());
					if (c.kind() == FallenHolyLand.Kind.NONE) continue;
					int top = stack(s, c, x0 + dx, z0 + dz, out);
					RealmLand.setColumn(chunk, sections, dx, dz, out, top, c.ground(), -1);
				}
			}
		} finally {
			for (LevelChunkSection section : sections) section.release();
		}
	}

	@Override
	public int ground(long seed, int lx, int lz) {
		return FallenHolyLand.column(FallenHolyLand.shape(seed), lx, lz).ground();
	}

	@Override
	public int centreY() { return FallenHolyLand.HEART_TOP; }

	@Override
	public String describe(long seed, int lx, int lz) {
		FallenHolyLand.Column c = FallenHolyLand.column(FallenHolyLand.shape(seed), lx, lz);
		return c.kind().name().toLowerCase() + (c.shard() >= 0 ? " " + c.shard() : "");
	}

	/** Fills {@code out} (by height) with the column's blocks, from its underside up. @return the highest block set */
	private static int stack(FallenHolyLand.Shape s, FallenHolyLand.Column c, int x, int z, BlockState[] out) {
		Arrays.fill(out, null);
		int g = Math.min(c.ground(), out.length - 4), bottom = Math.max(1, c.bottom());
		for (int y = bottom; y <= g; y++) out[y] = g - y <= 2 ? Blocks.DIRT.defaultBlockState() : rock(s, x, y, z, y - bottom, g - bottom);
		out[g] = top(s, x, z);
		int top = g;
		if (out[g].is(Blocks.GRASS_BLOCK)) {
			double roll = chance(x, z, 0x401E10);
			if (roll < 0.18) out[top = g + 1] = Blocks.GRASS.defaultBlockState();
			else if (roll < 0.195) out[top = g + 1] = Blocks.OXEYE_DAISY.defaultBlockState();
		}
		return top;
	}

	/** A shard's top: grass, the old white paving showing through it in patches (the land's own noise, ragged at its edges). */
	private static BlockState top(FallenHolyLand.Shape s, int x, int z) {
		double p = s.top.getValue(x * 0.06, z * 0.06) + (chance(x, z, 0x401E11) - 0.5) * 0.35;
		if (p > 0.55) return (chance(x, z, 0x401E12) < 0.3 ? Blocks.CRACKED_STONE_BRICKS : Blocks.POLISHED_DIORITE).defaultBlockState();
		if (p < -0.6) return Blocks.COARSE_DIRT.defaultBlockState();
		return Blocks.GRASS_BLOCK.defaultBlockState();
	}

	/**
	 * Its rock: near the top calcite and stone in level strata; lower, where it was torn away, deepslate and blackstone, the void's crying
	 * obsidian and amethyst in its veins.
	 */
	static BlockState rock(FallenHolyLand.Shape s, int x, int y, int z, int up, int whole) {
		double band = chance((int) Math.floor((y + 3 * s.under.getValue(x * 0.02, z * 0.02)) / 3.0), 0, 0x401E30);
		if (up > whole * 0.55) return (band < 0.45 ? Blocks.CALCITE : band < 0.7 ? Blocks.STONE : Blocks.DIORITE).defaultBlockState();
		double vein = chance(x * 3 + y, z * 3 - y, 0x401E31);
		if (vein < 0.02) return Blocks.CRYING_OBSIDIAN.defaultBlockState();
		if (vein < 0.035) return Blocks.AMETHYST_BLOCK.defaultBlockState();
		return (band < 0.5 ? Blocks.DEEPSLATE : band < 0.8 ? Blocks.BLACKSTONE : Blocks.TUFF).defaultBlockState();
	}
}
