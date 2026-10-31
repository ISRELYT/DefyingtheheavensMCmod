package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Arrays;

import static com.example.defyingtheheavens.RealmLand.chance;

/**
 * The Primeval Beast Graveyard's land laid block by block (BeastGraveyard works out its columns): the jungle floor's grass in broad patches
 * of podzol, moss and root-bound earth, thick with ferns, tall grass and low jungle bushes; still ponds of murky water on mud, lily pads
 * on them; its cliffs of stone laid in level strata, mossy at their lips; the rim's jungle over them. (Its trees, and the dragon's bones,
 * are the works'.)
 */
final class BeastGraveyardLand implements RealmLand {
	@Override
	public void fill(ChunkAccess chunk, int x0, int z0, BlockPos centre, long seed) {
		BeastGraveyard.Shape s = BeastGraveyard.shape(seed);
		BlockState[] out = new BlockState[ModDimensions.SECRET_REALM_HEIGHT];
		LevelChunkSection[] sections = chunk.getSections();
		for (LevelChunkSection section : sections) section.acquire();
		try {
			for (int dz = 0; dz < 16; dz++) {
				for (int dx = 0; dx < 16; dx++) {
					BeastGraveyard.Column c = BeastGraveyard.column(s, x0 + dx - centre.getX(), z0 + dz - centre.getZ());
					if (c.kind() == BeastGraveyard.Kind.NONE) continue;
					int top = stack(s, c, x0 + dx, z0 + dz, out);
					RealmLand.setColumn(chunk, sections, dx, dz, out, top, c.ground(), c.kind() == BeastGraveyard.Kind.POND ? BeastGraveyard.WATER_Y : -1);
				}
			}
		} finally {
			for (LevelChunkSection section : sections) section.release();
		}
	}

	@Override
	public int ground(long seed, int lx, int lz) {
		return BeastGraveyard.column(BeastGraveyard.shape(seed), lx, lz).ground();
	}

	@Override
	public int water(long seed, int lx, int lz) {
		return BeastGraveyard.column(BeastGraveyard.shape(seed), lx, lz).kind() == BeastGraveyard.Kind.POND ? BeastGraveyard.WATER_Y : -1;
	}

	@Override
	public int centreY() { return BeastGraveyard.FLOOR_Y; }

	@Override
	public String describe(long seed, int lx, int lz) {
		return BeastGraveyard.column(BeastGraveyard.shape(seed), lx, lz).kind().name().toLowerCase();
	}

	/** Fills {@code out} (by height) with the column's blocks. @return the highest block set */
	private static int stack(BeastGraveyard.Shape s, BeastGraveyard.Column c, int x, int z, BlockState[] out) {
		Arrays.fill(out, null);
		int g = Math.min(c.ground(), out.length - 4);
		for (int y = 0; y <= g; y++) out[y] = y == 0 ? Blocks.BEDROCK.defaultBlockState() : y < g - 3 ? rock(s, x, y, z) : Blocks.DIRT.defaultBlockState();
		int top = g;
		switch (c.kind()) {
			case FLOOR, RIM -> {
				out[g] = floor(s, x, z);
				BlockState[] plant = plant(x, z);
				if (plant != null) {
					out[top = g + 1] = plant[0];
					if (plant.length > 1) out[top = g + 2] = plant[1];
				}
			}
			case POND -> {
				out[g] = (chance(x, z, 0xB6A1) < 0.6 ? Blocks.MUD : Blocks.CLAY).defaultBlockState();
				for (int y = g + 1; y <= BeastGraveyard.WATER_Y; y++) out[y] = Blocks.WATER.defaultBlockState();
				top = BeastGraveyard.WATER_Y;
				if (chance(x, z, 0xB6A2) < 0.06) out[top = BeastGraveyard.WATER_Y + 1] = Blocks.LILY_PAD.defaultBlockState();
			}
			case CLIFF -> {
				for (int y = Math.max(1, g - 3); y <= g; y++) out[y] = rock(s, x, y, z);
				out[g] = chance(x, z, 0xB6A3) < 0.55 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.MOSSY_COBBLESTONE.defaultBlockState();
			}
			default -> {
			}
		}
		return top;
	}

	/** The jungle floor's top: grass in broad patches of podzol, moss and root-bound earth (by the land's own noise, ragged at their edges). */
	private static BlockState floor(BeastGraveyard.Shape s, int x, int z) {
		double p = s.patches.getValue(x * 0.04, z * 0.04) + (chance(x, z, 0xB6B0) - 0.5) * 0.3;
		if (p < -0.45) return Blocks.PODZOL.defaultBlockState();
		if (p > 0.6) return Blocks.MOSS_BLOCK.defaultBlockState();
		if (p > 0.45) return Blocks.ROOTED_DIRT.defaultBlockState();
		return Blocks.GRASS_BLOCK.defaultBlockState();
	}

	/** The undergrowth: ferns and grass, tall ones two blocks high, a low jungle bush; mostly something. */
	private static BlockState[] plant(int x, int z) {
		double roll = chance(x, z, 0xB6C0);
		if (roll < 0.12) return new BlockState[] {Blocks.GRASS.defaultBlockState()};
		if (roll < 0.22) return new BlockState[] {Blocks.FERN.defaultBlockState()};
		if (roll < 0.26) return twoHigh(Blocks.LARGE_FERN.defaultBlockState());
		if (roll < 0.29) return twoHigh(Blocks.TALL_GRASS.defaultBlockState());
		if (roll < 0.31) return new BlockState[] {Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true)};
		return null;
	}

	private static BlockState[] twoHigh(BlockState plant) {
		return new BlockState[] {plant.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), plant.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER)};
	}

	/**
	 * Deep down and in the cliffs: stone laid in level strata of andesite, tuff, mossy stone and dark deepslate (each layer its own roll,
	 * rising and falling gently), mixed enough that the cliffs read as layered rock and not one grey wall.
	 */
	private static BlockState rock(BeastGraveyard.Shape s, int x, int y, int z) {
		double band = chance((int) Math.floor((y + 4 * s.roll.getValue(x * 0.02, z * 0.02)) / 3.0), 0, 0xB6D0);
		if (y < 40) return (band < 0.3 ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState();
		return (band < 0.24 ? Blocks.ANDESITE : band < 0.44 ? Blocks.TUFF : band < 0.54 ? Blocks.MOSSY_COBBLESTONE : band < 0.62 ? Blocks.DEEPSLATE
				: Blocks.STONE).defaultBlockState();
	}
}
