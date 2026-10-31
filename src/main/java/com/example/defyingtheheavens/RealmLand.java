package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * A secret realm's land (SecretRealm#land): worked out column by column from the world's seed alone, so the generator
 * (SecretRealmGenerator) and whatever builds on it (the realm's RealmWorks) agree without a chunk loaded. Its coordinates are the realm's
 * own, from its middle (SecretRealm#centre). Each realm's is its own class: HerbValley's, SwordTomb's, Battlefield's.
 */
public interface RealmLand {
	/** The chunk whose least corner is world ({@code x0}, {@code z0}), of the realm whose middle is {@code centre}: its blocks laid, its heightmaps kept. */
	void fill(ChunkAccess chunk, int x0, int z0, BlockPos centre, long seed);

	/** The ground's top (the highest solid block) at ({@code lx}, {@code lz}) from the middle; -1 past the realm's edge. */
	int ground(long seed, int lx, int lz);

	/** The water's top there (lower than the ground where there is none). */
	default int water(long seed, int lx, int lz) { return -1; }

	/** The height its middle stands at (SecretRealm#centre: its heart's ground, more or less). */
	int centreY();

	/** A line for the debug screen: what lies at ({@code lx}, {@code lz}). */
	default String describe(long seed, int lx, int lz) { return ""; }

	// --- Shared by the lands ---

	/** A column's own roll in [0, 1), the same every time it is made. */
	static double chance(int x, int z, long salt) {
		long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		return (h >>> 11) / (double) (1L << 53);
	}

	/**
	 * One column's blocks ({@code out}, by height from the world's floor, null for air) set into {@code chunk} at ({@code dx}, {@code dz}),
	 * its heightmaps raised to its ground and to its water's top.
	 */
	static void setColumn(ChunkAccess chunk, LevelChunkSection[] sections, int dx, int dz, BlockState[] out, int top, int ground, int water) {
		for (int y = 0; y <= top && y < out.length; y++) {
			BlockState state = out[y];
			if (state == null || state.isAir()) continue;
			sections[chunk.getSectionIndex(y)].setBlockState(dx, y & 15, dz, state, false);
		}
		chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG).update(dx, ground, dz, Blocks.STONE.defaultBlockState());
		chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG).update(dx, Math.max(ground, water), dz, Blocks.STONE.defaultBlockState());
	}
}
