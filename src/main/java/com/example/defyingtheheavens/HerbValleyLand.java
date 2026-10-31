package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Arrays;

import static com.example.defyingtheheavens.RealmLand.chance;

/**
 * The Verdant Herb Valley's land laid block by block (HerbValley works out its columns): stone up to its ground, earth and grass on its
 * meadows and terraces, old dry-stone walls holding up its terraces, steps of stone where its paths climb them, soil on its herb beds, sand
 * and clay under its lake and brooks, grey rock on its rim; grass, ferns and flowers about it, lily pads on the lake's shallows. Its brooks
 * are marked to run as they are first loaded (they fall from terrace to terrace).
 */
final class HerbValleyLand implements RealmLand {
	/** A wall stands where a terrace drops this far (or more) to its neighbour. */
	private static final int WALL_DROP = 2;

	@Override
	public void fill(ChunkAccess chunk, int x0, int z0, BlockPos centre, long seed) {
		HerbValley.Shape valley = HerbValley.shape(seed);
		// The chunk's columns and a ring round them (for its walls and steps).
		HerbValley.Column[] columns = new HerbValley.Column[18 * 18];
		for (int dz = -1; dz <= 16; dz++) {
			for (int dx = -1; dx <= 16; dx++) {
				columns[(dz + 1) * 18 + dx + 1] = HerbValley.column(valley, x0 + dx - centre.getX(), z0 + dz - centre.getZ());
			}
		}
		BlockState[] column = new BlockState[ModDimensions.SECRET_REALM_HEIGHT];
		LevelChunkSection[] sections = chunk.getSections();
		for (LevelChunkSection section : sections) section.acquire();
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		try {
			for (int dz = 0; dz < 16; dz++) {
				for (int dx = 0; dx < 16; dx++) {
					HerbValley.Column c = columns[(dz + 1) * 18 + dx + 1];
					if (c.kind() == HerbValley.Kind.NONE) continue;
					int x = x0 + dx, z = z0 + dz;
					int top = stack(c, columns, dx, dz, x, z, column);
					RealmLand.setColumn(chunk, sections, dx, dz, column, top, c.ground(), c.water());
					// A brook's water runs as it first loads (falling over the terraces' walls).
					if (c.kind() == HerbValley.Kind.STREAM) chunk.markPosForPostprocessing(at.set(x, c.water(), z));
				}
			}
		} finally {
			for (LevelChunkSection section : sections) section.release();
		}
	}

	@Override
	public int ground(long seed, int lx, int lz) {
		HerbValley.Column c = HerbValley.column(HerbValley.shape(seed), lx, lz);
		return c.kind() == HerbValley.Kind.NONE ? -1 : c.ground();
	}

	@Override
	public int water(long seed, int lx, int lz) {
		return HerbValley.column(HerbValley.shape(seed), lx, lz).water();
	}

	@Override
	public int centreY() { return HerbValley.WATER_Y; }

	@Override
	public String describe(long seed, int lx, int lz) {
		return HerbValley.column(HerbValley.shape(seed), lx, lz).kind().name().toLowerCase();
	}

	private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
	private static final Direction[] FACING = {Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};

	/** Fills {@code out} (by height) with column (dx, dz)'s blocks. @return the highest block set */
	private static int stack(HerbValley.Column c, HerbValley.Column[] columns, int dx, int dz, int x, int z, BlockState[] out) {
		Arrays.fill(out, null);
		int g = Math.min(c.ground(), out.length - 2);
		// How far it stands over its lowest neighbour (a terrace's wall), and which neighbouring step of a path is one higher.
		int lowest = g;
		int stepUp = -1;
		for (int i = 0; i < NEIGHBOURS.length; i++) {
			HerbValley.Column n = columns[(dz + 1 + NEIGHBOURS[i][1]) * 18 + dx + 1 + NEIGHBOURS[i][0]];
			if (n.kind() == HerbValley.Kind.NONE) continue;
			lowest = Math.min(lowest, n.ground());
			if (c.kind() == HerbValley.Kind.PATH && n.kind() == HerbValley.Kind.PATH && n.ground() == g + 1 && stepUp < 0) stepUp = i;
		}
		boolean wall = (c.kind() == HerbValley.Kind.TERRACE || c.kind() == HerbValley.Kind.BED) && g - lowest >= WALL_DROP;
		boolean steep = g - lowest >= 3;
		for (int y = 0; y <= g; y++) out[y] = y == 0 ? Blocks.BEDROCK.defaultBlockState() : rock(x, y, z);
		switch (c.kind()) {
			case ISLAND, MEADOW -> soil(out, g, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
			case TERRACE -> {
				soil(out, g, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
				if (wall) wall(out, lowest, g, x, z);
			}
			case BED -> {
				soil(out, g, Blocks.PODZOL.defaultBlockState(), 2);
				if (wall) wall(out, lowest, g, x, z);
			}
			case SHORE -> {
				soil(out, g, Blocks.SAND.defaultBlockState(), 3);
				if (chance(x, z, 0x5A0D) < 0.25) out[g] = Blocks.GRAVEL.defaultBlockState();
			}
			case LAKE -> {
				double roll = chance(x, z, 0x1A4E);
				soil(out, g, (roll < 0.45 ? Blocks.CLAY : roll < 0.8 ? Blocks.SAND : Blocks.GRAVEL).defaultBlockState(), 2);
			}
			case STREAM -> soil(out, g, (chance(x, z, 0x5713) < 0.6 ? Blocks.GRAVEL : Blocks.SAND).defaultBlockState(), 2);
			case PATH -> {
				soil(out, g, Blocks.DIRT.defaultBlockState(), 2);
				out[g] = pathStone(x, z);
				if (stepUp >= 0) out[g + 1] = stair(x, z, FACING[stepUp]);
			}
			case RIM -> {
				if (steep) out[g] = rock(x, g + 7, z);
				else soil(out, g, Blocks.GRASS_BLOCK.defaultBlockState(), 1);
			}
			case PILLAR -> {
				out[g] = chance(x, z, 0x9111) < 0.3 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
				if (g > 0) out[g - 1] = Blocks.DIRT.defaultBlockState();
			}
			default -> {
			}
		}
		int top = g;
		// Water over it, up to the lake's (or the brook's) surface.
		if (c.wet()) {
			for (int y = g + 1; y <= c.water() && y < out.length; y++) out[y] = Blocks.WATER.defaultBlockState();
			top = Math.max(top, c.water());
			if (c.water() - g <= 2 && c.kind() != HerbValley.Kind.STREAM && chance(x, z, 0x711D) < 0.05 && c.water() + 1 < out.length) {
				out[c.water() + 1] = Blocks.LILY_PAD.defaultBlockState();
				top = c.water() + 1;
			}
			return top;
		}
		if (out[g + 1] != null) return g + 1;
		BlockState decoration = wall || steep ? null : decoration(c.kind(), x, z);
		if (decoration != null && g + 1 < out.length) {
			out[g + 1] = decoration;
			top = g + 1;
		}
		return top;
	}

	/** The column's top {@code depth} blocks: {@code top} over earth (or sand under sand). */
	static void soil(BlockState[] out, int g, BlockState top, int depth) {
		out[g] = top;
		BlockState under = top.is(Blocks.SAND) ? Blocks.SAND.defaultBlockState() : top.is(Blocks.CLAY) || top.is(Blocks.GRAVEL) ? top : Blocks.DIRT.defaultBlockState();
		for (int y = g - 1; y >= Math.max(1, g - depth); y--) out[y] = under;
	}

	/** A terrace's wall from its foot ({@code lowest} + 1) up under its turf: old stones, mossy, set dry. */
	private static void wall(BlockState[] out, int lowest, int g, int x, int z) {
		for (int y = lowest + 1; y < g; y++) {
			double roll = chance(x * 31 + y, z * 17 - y, 0x3A11);
			out[y] = (roll < 0.34 ? Blocks.MOSSY_COBBLESTONE : roll < 0.62 ? Blocks.COBBLESTONE : roll < 0.84 ? Blocks.STONE_BRICKS
					: Blocks.MOSSY_STONE_BRICKS).defaultBlockState();
		}
	}

	/** The valley's rock: stone, banded with andesite, tuff here and there. */
	private static BlockState rock(int x, int y, int z) {
		double band = chance(y * 7, (x >> 3) + (z >> 3) * 13, 0xA4D3);
		return (band < 0.18 ? Blocks.ANDESITE : band < 0.24 ? Blocks.TUFF : Blocks.STONE).defaultBlockState();
	}

	private static BlockState pathStone(int x, int z) {
		double roll = chance(x, z, 0x9A70);
		return (roll < 0.4 ? Blocks.STONE_BRICKS : roll < 0.7 ? Blocks.MOSSY_STONE_BRICKS : roll < 0.85 ? Blocks.CRACKED_STONE_BRICKS : Blocks.COBBLESTONE).defaultBlockState();
	}

	private static BlockState stair(int x, int z, Direction up) {
		Block block = chance(x, z, 0x57A1) < 0.5 ? Blocks.STONE_BRICK_STAIRS : Blocks.MOSSY_STONE_BRICK_STAIRS;
		return block.defaultBlockState().setValue(StairBlock.FACING, up);
	}

	/** Something small growing on the ground, or null. */
	private static BlockState decoration(HerbValley.Kind kind, int x, int z) {
		double roll = chance(x, z, 0xDEC0);
		return switch (kind) {
			case MEADOW, ISLAND -> roll < 0.16 ? Blocks.GRASS.defaultBlockState() : roll < 0.18 ? Blocks.FERN.defaultBlockState()
					: roll < 0.21 ? flower(x, z) : roll < 0.235 ? petals(x, z) : null;
			case TERRACE -> roll < 0.1 ? Blocks.GRASS.defaultBlockState() : roll < 0.115 ? flower(x, z) : null;
			case RIM -> roll < 0.06 ? Blocks.GRASS.defaultBlockState() : roll < 0.08 ? Blocks.FERN.defaultBlockState() : roll < 0.12 ? Blocks.MOSS_CARPET.defaultBlockState() : null;
			case PILLAR -> roll < 0.3 ? Blocks.MOSS_CARPET.defaultBlockState() : roll < 0.33 ? Blocks.AZALEA.defaultBlockState() : null;
			default -> null;
		};
	}

	private static final Block[] FLOWERS = {Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY, Blocks.ALLIUM, Blocks.WHITE_TULIP};

	private static BlockState flower(int x, int z) {
		return FLOWERS[(int) (chance(x, z, 0xF10) * FLOWERS.length) % FLOWERS.length].defaultBlockState();
	}

	private static BlockState petals(int x, int z) {
		return Blocks.PINK_PETALS.defaultBlockState().setValue(PinkPetalsBlock.AMOUNT, 1 + (int) (chance(x, z, 0x9E7) * 4) % 4)
				.setValue(PinkPetalsBlock.FACING, Direction.from2DDataValue((int) (chance(x, z, 0x3F1) * 4) % 4));
	}
}
