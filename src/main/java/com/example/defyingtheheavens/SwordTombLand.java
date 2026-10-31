package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Arrays;

import static com.example.defyingtheheavens.RealmLand.chance;

/**
 * The Sunken Sword Tomb's land laid block by block (SwordTomb works out its columns): grey stone through and through, banded with andesite
 * and tuff; gravel, coarse earth and stone on the gorge's floor and its ledges, strewn with swords driven into the ground, whole and broken,
 * straight and leaning (BuriedSwordBlock); bare cracked rock on the plateau; the branch canyons' ramps and the Sword Path stepped in stone
 * bricks, a low wall along the path's open edge.
 */
final class SwordTombLand implements RealmLand {
	@Override
	public void fill(ChunkAccess chunk, int x0, int z0, BlockPos centre, long seed) {
		SwordTomb.Shape s = SwordTomb.shape(seed);
		SwordTomb.Column[] columns = new SwordTomb.Column[18 * 18];
		for (int dz = -1; dz <= 16; dz++) {
			for (int dx = -1; dx <= 16; dx++) columns[(dz + 1) * 18 + dx + 1] = SwordTomb.column(s, x0 + dx - centre.getX(), z0 + dz - centre.getZ());
		}
		BlockState[] out = new BlockState[ModDimensions.SECRET_REALM_HEIGHT];
		LevelChunkSection[] sections = chunk.getSections();
		for (LevelChunkSection section : sections) section.acquire();
		try {
			for (int dz = 0; dz < 16; dz++) {
				for (int dx = 0; dx < 16; dx++) {
					SwordTomb.Column c = columns[(dz + 1) * 18 + dx + 1];
					if (c.kind() == SwordTomb.Kind.NONE) continue;
					int top = stack(s, c, columns, dx, dz, x0 + dx, z0 + dz, x0 + dx - centre.getX(), z0 + dz - centre.getZ(), out);
					RealmLand.setColumn(chunk, sections, dx, dz, out, top, c.ground(), -1);
				}
			}
		} finally {
			for (LevelChunkSection section : sections) section.release();
		}
	}

	@Override
	public int ground(long seed, int lx, int lz) {
		return SwordTomb.column(SwordTomb.shape(seed), lx, lz).ground();
	}

	@Override
	public int centreY() { return SwordTomb.SUMMIT_Y; }

	@Override
	public String describe(long seed, int lx, int lz) {
		return SwordTomb.column(SwordTomb.shape(seed), lx, lz).kind().name().toLowerCase();
	}

	private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
	private static final Direction[] FACING = {Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};

	/** Fills {@code out} (by height) with column (dx, dz)'s blocks. @return the highest block set */
	private static int stack(SwordTomb.Shape s, SwordTomb.Column c, SwordTomb.Column[] columns, int dx, int dz, int x, int z, int lx, int lz, BlockState[] out) {
		Arrays.fill(out, null);
		int g = Math.min(c.ground(), out.length - 3);
		int stepUp = -1, lowest = g;
		for (int i = 0; i < NEIGHBOURS.length; i++) {
			SwordTomb.Column n = columns[(dz + 1 + NEIGHBOURS[i][1]) * 18 + dx + 1 + NEIGHBOURS[i][0]];
			if (n.kind() == SwordTomb.Kind.NONE) continue;
			lowest = Math.min(lowest, n.ground());
			boolean stepped = c.kind() == SwordTomb.Kind.PATH || c.kind() == SwordTomb.Kind.BRANCH;
			if (stepped && n.kind() == c.kind() && n.ground() == g + 1 && stepUp < 0) stepUp = i;
		}
		for (int y = 0; y <= g; y++) out[y] = y == 0 ? Blocks.BEDROCK.defaultBlockState() : rock(s, x, y, z);
		int top = g;
		switch (c.kind()) {
			case FLOOR, LEDGE -> {
				out[g] = ground(s, x, z, false);
				if (g > 1) out[g - 1] = Blocks.GRAVEL.defaultBlockState();
				BlockState sword = sword(x, z, c.kind() == SwordTomb.Kind.FLOOR ? 0.07 : 0.03);
				if (sword != null) out[top = g + 1] = sword;
			}
			case BRANCH, PATH -> {
				double roll = chance(x, z, 0x9A71);
				out[g] = (roll < 0.5 ? Blocks.STONE_BRICKS : roll < 0.8 ? Blocks.CRACKED_STONE_BRICKS : Blocks.POLISHED_ANDESITE).defaultBlockState();
				if (stepUp >= 0) out[top = g + 1] = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, FACING[stepUp]);
				// The Sword Path's open edge: a low wall where it drops away to the gorge's floor.
				else if (c.kind() == SwordTomb.Kind.PATH && Math.hypot(lx, lz) > SwordTomb.SPIRE_R - 0.9 && g - lowest > 4) {
					out[top = g + 1] = Blocks.STONE_BRICK_WALL.defaultBlockState();
				}
			}
			case PLATEAU -> {
				out[g] = ground(s, x, z, true);
				if (chance(x, z, 0x7A2E) < 0.01) out[top = g + 1] = Blocks.DEAD_BUSH.defaultBlockState();
				else {
					BlockState sword = sword(x, z, 0.004);
					if (sword != null) out[top = g + 1] = sword;
				}
			}
			case SUMMIT -> out[g] = (chance(x, z, 0x5A3F) < 0.6 ? Blocks.POLISHED_ANDESITE : Blocks.STONE).defaultBlockState();
			default -> {
			}
		}
		return top;
	}

	/** Whether one of the fallen sect's swords was driven in at world column ({@code x}, {@code z}) (its own roll, with {@code odds}). */
	static boolean swordAt(int x, int z, double odds) {
		return chance(x, z, 0x5C0D) < odds;
	}

	/** One of the fallen sect's swords, left where it was driven in (by its own roll, with {@code odds}), or null. */
	private static BlockState sword(int x, int z, double odds) {
		if (!swordAt(x, z, odds)) return null;
		double roll = chance(x, z, 0x5C1D);
		return ModBlocks.BURIED_SWORD.defaultBlockState()
				.setValue(BuriedSwordBlock.FACING, Direction.from2DDataValue((int) (chance(x, z, 0x5C2D) * 4) % 4))
				.setValue(BuriedSwordBlock.BROKEN, roll < 0.55)
				.setValue(BuriedSwordBlock.TILTED, chance(x, z, 0x5C3D) < 0.4);
	}

	/**
	 * The tomb's rock: grey stone laid down in strata of andesite and tuff two or three blocks thick, running level across every face (each
	 * layer its own roll, rising and falling gently with the land: no stripes up the cliffs), flecked with cobble; darker deep down.
	 */
	static BlockState rock(SwordTomb.Shape s, int x, int y, int z) {
		double wobble = 3 * s.rough.getValue(x * 0.012, z * 0.012);
		double band = chance((int) Math.floor((y + wobble) / 2.5), 0, 0xB7C1);
		if (y < 40) return (band < 0.3 ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState();
		if (chance(x * 31 + y, z * 17 - y, 0xB7C2) < 0.03) return Blocks.COBBLESTONE.defaultBlockState();
		return (band < 0.24 ? Blocks.ANDESITE : band < 0.36 ? Blocks.TUFF : band < 0.42 ? Blocks.SMOOTH_BASALT : Blocks.STONE).defaultBlockState();
	}

	/**
	 * The ground's top: stone and gravel in drifts (by the land's own noise, a little ragged at their edges), tuff here and there, and on the
	 * gorge's floor a rare stain of rust-brown earth where the swords lie thickest; the plateau barer.
	 */
	private static BlockState ground(SwordTomb.Shape s, int x, int z, boolean plateau) {
		double p = s.cracks.getValue(x * 0.05, z * 0.05) + (chance(x, z, 0x51F0) - 0.5) * 0.3;
		if (p < -0.35) return Blocks.GRAVEL.defaultBlockState();
		if (p < 0.15) return (chance(x, z, 0x51F1) < 0.5 ? Blocks.STONE : Blocks.ANDESITE).defaultBlockState();
		if (p < 0.6 || plateau) return (p < 0.6 ? Blocks.TUFF : Blocks.STONE).defaultBlockState();
		return Blocks.COARSE_DIRT.defaultBlockState();
	}

	private static BlockState block(Block block) { return block.defaultBlockState(); }
}
