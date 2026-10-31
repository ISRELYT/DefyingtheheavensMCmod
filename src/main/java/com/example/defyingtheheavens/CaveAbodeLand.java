package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Arrays;

import static com.example.defyingtheheavens.RealmLand.chance;

/**
 * The Cave Abode's land laid block by block (CaveAbode works out its columns): the mountain grey stone in level strata of andesite and
 * calcite veins, moss and grass clinging on its gentler slopes, snow on its crown; the basin's meadow, grass and flowers, a gravel shore
 * round the lake on clay; the crags at the rim bare stone. (Its pines, and all the abode inside the mountain, are the works'.)
 */
final class CaveAbodeLand implements RealmLand {
	@Override
	public void fill(ChunkAccess chunk, int x0, int z0, BlockPos centre, long seed) {
		CaveAbode.Shape s = CaveAbode.shape(seed);
		BlockState[] out = new BlockState[ModDimensions.SECRET_REALM_HEIGHT];
		LevelChunkSection[] sections = chunk.getSections();
		for (LevelChunkSection section : sections) section.acquire();
		try {
			for (int dz = 0; dz < 16; dz++) {
				for (int dx = 0; dx < 16; dx++) {
					CaveAbode.Column c = CaveAbode.column(s, x0 + dx - centre.getX(), z0 + dz - centre.getZ());
					if (c.kind() == CaveAbode.Kind.NONE) continue;
					int top = stack(s, c, x0 + dx, z0 + dz, out);
					RealmLand.setColumn(chunk, sections, dx, dz, out, top, c.ground(), c.kind() == CaveAbode.Kind.LAKE ? CaveAbode.WATER_Y : -1);
				}
			}
		} finally {
			for (LevelChunkSection section : sections) section.release();
		}
	}

	@Override
	public int ground(long seed, int lx, int lz) {
		return CaveAbode.column(CaveAbode.shape(seed), lx, lz).ground();
	}

	@Override
	public int water(long seed, int lx, int lz) {
		return CaveAbode.column(CaveAbode.shape(seed), lx, lz).kind() == CaveAbode.Kind.LAKE ? CaveAbode.WATER_Y : -1;
	}

	@Override
	public int centreY() { return CaveAbode.FLOOR_Y; }

	@Override
	public String describe(long seed, int lx, int lz) {
		return CaveAbode.column(CaveAbode.shape(seed), lx, lz).kind().name().toLowerCase();
	}

	private static int stack(CaveAbode.Shape s, CaveAbode.Column c, int x, int z, BlockState[] out) {
		Arrays.fill(out, null);
		int g = Math.min(c.ground(), out.length - 4);
		for (int y = 0; y <= g; y++) out[y] = y == 0 ? Blocks.BEDROCK.defaultBlockState() : rock(s, x, y, z);
		int top = g;
		switch (c.kind()) {
			case MOUNTAIN -> {
				// Moss and grass where it is gentle low down, snow on its crown; bare rock on its steeps.
				if (g > CaveAbode.PEAK_Y - 14) out[g] = Blocks.SNOW_BLOCK.defaultBlockState();
				else if (g < CaveAbode.FLOOR_Y + 24 && chance(x, z, 0xCA01) < 0.45) out[g] = Blocks.MOSS_BLOCK.defaultBlockState();
			}
			case LAKE -> {
				out[g] = (chance(x, z, 0xCA02) < 0.5 ? Blocks.CLAY : Blocks.GRAVEL).defaultBlockState();
				for (int y = g + 1; y <= CaveAbode.WATER_Y; y++) out[y] = Blocks.WATER.defaultBlockState();
				top = CaveAbode.WATER_Y;
				if (chance(x, z, 0xCA03) < 0.04) out[top = CaveAbode.WATER_Y + 1] = Blocks.LILY_PAD.defaultBlockState();
			}
			case FLOOR -> {
				for (int y = Math.max(1, g - 3); y < g; y++) out[y] = Blocks.DIRT.defaultBlockState();
				out[g] = chance(x, z, 0xCA04) < 0.08 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
				BlockState[] plant = plant(x, z);
				if (plant != null) {
					out[top = g + 1] = plant[0];
					if (plant.length > 1) out[top = g + 2] = plant[1];
				}
			}
			case CRAG -> {
				if (chance(x, z, 0xCA05) < 0.3) out[g] = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
			}
			default -> {
			}
		}
		return top;
	}

	/** The meadow's grass and flowers: short grass most, a few tall, cornflowers and azure bluets and lilies of the valley. */
	private static BlockState[] plant(int x, int z) {
		double roll = chance(x, z, 0xCA10);
		if (roll < 0.14) return new BlockState[] {Blocks.GRASS.defaultBlockState()};
		if (roll < 0.17) {
			BlockState tall = Blocks.TALL_GRASS.defaultBlockState();
			return new BlockState[] {tall.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), tall.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER)};
		}
		if (roll < 0.185) return new BlockState[] {Blocks.CORNFLOWER.defaultBlockState()};
		if (roll < 0.2) return new BlockState[] {Blocks.AZURE_BLUET.defaultBlockState()};
		if (roll < 0.21) return new BlockState[] {Blocks.LILY_OF_THE_VALLEY.defaultBlockState()};
		return null;
	}

	/**
	 * The mountain's rock: grey stone in strata of andesite and tuff, rising and falling across its slopes (never level rings about the
	 * peak), a thin pale band of diorite or calcite now and then; darker deep down.
	 */
	static BlockState rock(CaveAbode.Shape s, int x, int y, int z) {
		double band = chance((int) Math.floor((y + 8 * s.roll.getValue(x * 0.02, z * 0.02)) / 3.0), 0, 0xCA20);
		if (y < 40) return (band < 0.3 ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState();
		return (band < 0.24 ? Blocks.ANDESITE : band < 0.32 ? Blocks.TUFF : band < 0.35 ? Blocks.DIORITE : band < 0.37 ? Blocks.CALCITE : Blocks.STONE).defaultBlockState();
	}
}
