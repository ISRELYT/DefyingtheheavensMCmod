package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Arrays;

import static com.example.defyingtheheavens.RealmLand.chance;

/**
 * The Ancient Battlefield's land laid block by block (Battlefield works out its columns): earth churned rust-red (coarse earth, red earth
 * and clay, gravel), soul soil where the dead lie thickest, bones half buried, broken blades and spears driven into the ground; the trenches'
 * walls and floors of trampled earth, the earthworks heaped beside them; the hill of packed earth, its ramp stepped in old stone; low red
 * ridges at the rim.
 */
final class BattlefieldLand implements RealmLand {
	@Override
	public void fill(ChunkAccess chunk, int x0, int z0, BlockPos centre, long seed) {
		Battlefield.Shape s = Battlefield.shape(seed);
		Battlefield.Column[] columns = new Battlefield.Column[18 * 18];
		for (int dz = -1; dz <= 16; dz++) {
			for (int dx = -1; dx <= 16; dx++) columns[(dz + 1) * 18 + dx + 1] = Battlefield.column(s, x0 + dx - centre.getX(), z0 + dz - centre.getZ());
		}
		BlockState[] out = new BlockState[ModDimensions.SECRET_REALM_HEIGHT];
		LevelChunkSection[] sections = chunk.getSections();
		for (LevelChunkSection section : sections) section.acquire();
		try {
			for (int dz = 0; dz < 16; dz++) {
				for (int dx = 0; dx < 16; dx++) {
					Battlefield.Column c = columns[(dz + 1) * 18 + dx + 1];
					if (c.kind() == Battlefield.Kind.NONE) continue;
					int top = stack(s, c, columns, dx, dz, x0 + dx, z0 + dz, out);
					RealmLand.setColumn(chunk, sections, dx, dz, out, top, c.ground(), -1);
				}
			}
		} finally {
			for (LevelChunkSection section : sections) section.release();
		}
	}

	@Override
	public int ground(long seed, int lx, int lz) {
		return Battlefield.column(Battlefield.shape(seed), lx, lz).ground();
	}

	@Override
	public int centreY() { return Battlefield.HILL_TOP_Y; }

	@Override
	public String describe(long seed, int lx, int lz) {
		return Battlefield.column(Battlefield.shape(seed), lx, lz).kind().name().toLowerCase();
	}

	private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
	private static final Direction[] FACING = {Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};

	private static int stack(Battlefield.Shape s, Battlefield.Column c, Battlefield.Column[] columns, int dx, int dz, int x, int z, BlockState[] out) {
		Arrays.fill(out, null);
		int g = Math.min(c.ground(), out.length - 3);
		int stepUp = -1;
		for (int i = 0; i < NEIGHBOURS.length; i++) {
			Battlefield.Column n = columns[(dz + 1 + NEIGHBOURS[i][1]) * 18 + dx + 1 + NEIGHBOURS[i][0]];
			if (c.kind() == Battlefield.Kind.RAMP && n.kind() == Battlefield.Kind.RAMP && n.ground() == g + 1 && stepUp < 0) stepUp = i;
		}
		for (int y = 0; y <= g; y++) out[y] = y == 0 ? Blocks.BEDROCK.defaultBlockState() : y < g - 4 ? deep(s, x, y, z) : Blocks.DIRT.defaultBlockState();
		int top = g;
		double roll = chance(x, z, 0xBA10);
		switch (c.kind()) {
			case PLAIN, CRATER, BERM, RIM -> {
				out[g] = surface(s, x, z, c.kind());
				if (c.kind() != Battlefield.Kind.RIM) {
					// Bones half buried in the earth; on it a skull, a broken blade or spear driven in, a dead bush.
					if (chance(x, z, 0xBA33) < 0.008) out[g] = Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS,
							chance(x, z, 0xBA34) < 0.5 ? Direction.Axis.X : Direction.Axis.Z);
					BlockState thing = relic(x, z);
					if (thing != null) out[top = g + 1] = thing;
				}
			}
			case TRENCH -> {
				out[g] = (roll < 0.5 ? Blocks.COARSE_DIRT : roll < 0.8 ? Blocks.MUD : Blocks.PACKED_MUD).defaultBlockState();
				if (chance(x, z, 0xBA11) < 0.05) out[g] = Blocks.BONE_BLOCK.defaultBlockState();
			}
			case HILL, HILLTOP -> {
				out[g] = (roll < 0.55 ? Blocks.COARSE_DIRT : roll < 0.8 ? Blocks.PODZOL : Blocks.ROOTED_DIRT).defaultBlockState();
				if (c.kind() == Battlefield.Kind.HILL && chance(x, z, 0xBA12) < 0.03) out[top = g + 1] = Blocks.DEAD_BUSH.defaultBlockState();
			}
			case RAMP -> {
				out[g] = (roll < 0.6 ? Blocks.COBBLESTONE : Blocks.MOSSY_COBBLESTONE).defaultBlockState();
				if (stepUp >= 0) out[top = g + 1] = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, FACING[stepUp]);
			}
			default -> {
			}
		}
		return top;
	}

	/**
	 * The churned earth of the plain, in broad patches by the land's own noise (a little ragged at their edges, never a scatter of single
	 * blocks): coarse earth most of all, old blood-dark red earth and clay, gravel where the old roads ran; soul soil where the dead lie
	 * thickest; the craters' floors gravel and soul soil; the rim's ridges terracotta.
	 */
	private static BlockState surface(Battlefield.Shape s, int x, int z, Battlefield.Kind kind) {
		double p = s.ridge.getValue(x * 0.035, z * 0.035) + (chance(x, z, 0xBA20) - 0.5) * 0.25;
		double soul = s.wobble.getValue(x * 0.05 + 40, z * 0.05 - 40) + (chance(x, z, 0xBA21) - 0.5) * 0.2;
		if (kind == Battlefield.Kind.CRATER) return (p < 0 ? Blocks.GRAVEL : soul > 0 ? Blocks.SOUL_SOIL : Blocks.COARSE_DIRT).defaultBlockState();
		if (kind == Battlefield.Kind.RIM) return (p < 0.2 ? Blocks.TERRACOTTA : Blocks.BROWN_TERRACOTTA).defaultBlockState();
		if (soul > 0.55) return Blocks.SOUL_SOIL.defaultBlockState();
		if (p < -0.45) return Blocks.GRAVEL.defaultBlockState();
		if (p < 0.1) return Blocks.COARSE_DIRT.defaultBlockState();
		if (p < 0.4) return Blocks.TERRACOTTA.defaultBlockState();
		if (p < 0.6) return Blocks.RED_TERRACOTTA.defaultBlockState();
		return Blocks.RED_SAND.defaultBlockState();
	}

	/**
	 * What lies on the plain from the old war: a broken blade or spear driven in, a dead bush; mostly nothing. (Never a skull: the land is
	 * laid straight into the chunk, and a skull is drawn by a block entity none would be made for.)
	 */
	private static BlockState relic(int x, int z) {
		double roll = chance(x, z, 0xBA30);
		if (roll < 0.018) {
			return ModBlocks.BURIED_SWORD.defaultBlockState().setValue(BuriedSwordBlock.FACING, Direction.from2DDataValue((int) (chance(x, z, 0xBA31) * 4) % 4))
					.setValue(BuriedSwordBlock.BROKEN, chance(x, z, 0xBA32) < 0.6).setValue(BuriedSwordBlock.TILTED, true);
		}
		if (roll < 0.034) return Blocks.DEAD_BUSH.defaultBlockState();
		return null;
	}

	/** Deep down: stone and old packed earth in level strata (each layer its own roll, rising and falling gently: no stripes down a trench). */
	private static BlockState deep(Battlefield.Shape s, int x, int y, int z) {
		double band = chance((int) Math.floor((y + 2 * s.roll.getValue(x * 0.015, z * 0.015)) / 2.0), 0, 0xBA40);
		return (band < 0.2 ? Blocks.TUFF : band < 0.32 ? Blocks.PACKED_MUD : Blocks.STONE).defaultBlockState();
	}
}
