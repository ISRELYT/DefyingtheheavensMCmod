package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Arrays;

import static com.example.defyingtheheavens.RealmLand.chance;

/**
 * The Tomb of a Fallen Immortal's land laid block by block (ImmortalTomb works out its columns): the base's top pale grass gone yellow with
 * the decay that took its lord, bare earth and dead bushes in it; its rim white stone; under it calcite and diorite, then, deeper in its
 * hanging dome, deepslate and tuff with gilded blackstone in its veins; the islets the same. (The palace is the works'.)
 */
final class ImmortalTombLand implements RealmLand {
	@Override
	public void fill(ChunkAccess chunk, int x0, int z0, BlockPos centre, long seed) {
		ImmortalTomb.Shape s = ImmortalTomb.shape(seed);
		BlockState[] out = new BlockState[ModDimensions.SECRET_REALM_HEIGHT];
		LevelChunkSection[] sections = chunk.getSections();
		for (LevelChunkSection section : sections) section.acquire();
		try {
			for (int dz = 0; dz < 16; dz++) {
				for (int dx = 0; dx < 16; dx++) {
					ImmortalTomb.Column c = ImmortalTomb.column(s, x0 + dx - centre.getX(), z0 + dz - centre.getZ());
					if (c.kind() == ImmortalTomb.Kind.NONE) continue;
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
		return ImmortalTomb.column(ImmortalTomb.shape(seed), lx, lz).ground();
	}

	@Override
	public int centreY() { return ImmortalTomb.BASE_Y; }

	@Override
	public String describe(long seed, int lx, int lz) {
		return ImmortalTomb.column(ImmortalTomb.shape(seed), lx, lz).kind().name().toLowerCase();
	}

	/** Fills {@code out} (by height) with the column's blocks, from its underside up. @return the highest block set */
	private static int stack(ImmortalTomb.Shape s, ImmortalTomb.Column c, int x, int z, BlockState[] out) {
		Arrays.fill(out, null);
		int g = Math.min(c.ground(), out.length - 4), bottom = Math.max(1, c.bottom());
		for (int y = bottom; y <= g; y++) out[y] = g - y <= 2 ? Blocks.DIRT.defaultBlockState() : rock(s, x, y, z, g - y);
		int top = g;
		if (c.kind() == ImmortalTomb.Kind.EDGE) {
			out[g] = (chance(x, z, 0x7031A0) < 0.6 ? Blocks.CALCITE : Blocks.DIORITE).defaultBlockState();
			return top;
		}
		double p = s.top.getValue(x * 0.05, z * 0.05) + (chance(x, z, 0x7031A1) - 0.5) * 0.3;
		out[g] = (p < -0.5 ? Blocks.COARSE_DIRT : p > 0.6 ? Blocks.PODZOL : Blocks.GRASS_BLOCK).defaultBlockState();
		double roll = chance(x, z, 0x7031A2);
		if (out[g].is(Blocks.GRASS_BLOCK) && roll < 0.12) out[top = g + 1] = Blocks.GRASS.defaultBlockState();
		else if (!out[g].is(Blocks.GRASS_BLOCK) && roll < 0.08) out[top = g + 1] = Blocks.DEAD_BUSH.defaultBlockState();
		return top;
	}

	/** White stone near the top in level strata; deeper, deepslate and tuff, gilded blackstone in its veins. */
	static BlockState rock(ImmortalTomb.Shape s, int x, int y, int z, int down) {
		double band = chance((int) Math.floor((y + 3 * s.under.getValue(x * 0.02, z * 0.02)) / 3.0), 0, 0x7031B0);
		if (down < 10) return (band < 0.5 ? Blocks.CALCITE : band < 0.8 ? Blocks.DIORITE : Blocks.STONE).defaultBlockState();
		if (chance(x * 3 + y, z * 3 - y, 0x7031B1) < 0.025) return Blocks.GILDED_BLACKSTONE.defaultBlockState();
		return (band < 0.45 ? Blocks.DEEPSLATE : band < 0.75 ? Blocks.TUFF : Blocks.STONE).defaultBlockState();
	}
}
