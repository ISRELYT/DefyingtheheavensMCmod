package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Arrays;
import java.util.Optional;

/**
 * A Ghost King's palace (GhostPalacePiece) in the Netherworld: one to a cell of the structure set {@code ghost_palaces}, in the
 * Fields of the Other Shore and the Bone Wastes (its biome tag), on land level enough for its platform and over no chasm, and
 * never near the Ghost Gate ({@link #GATE_CLEARANCE}).
 */
public class GhostPalaceStructure extends Structure {
	/** No palace stands within this many blocks of the Ghost Gate (0, 0): the Gate's own land. */
	public static final int GATE_CLEARANCE = 256;
	/** The most the ground under the platform may rise and fall (it is levelled, filled below and cut away above). */
	private static final int MAX_RELIEF = 10;

	public GhostPalaceStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	public StructureType<?> type() {
		return ModStructures.GHOST_PALACE_TYPE;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ChunkPos chunk = context.chunkPos();
		int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
		if ((long) x * x + (long) z * z < (long) GATE_CLEARANCE * GATE_CLEARANCE) return Optional.empty();
		ChunkGenerator generator = context.chunkGenerator();
		LevelHeightAccessor heights = context.heightAccessor();
		RandomState state = context.randomState();
		int reach = GhostPalacePiece.HALF - 2;
		int[] samples = new int[9];
		int n = 0;
		for (int dx = -reach; dx <= reach; dx += reach) {
			for (int dz = -reach; dz <= reach; dz += reach) {
				int y = generator.getFirstOccupiedHeight(x + dx, z + dz, Heightmap.Types.WORLD_SURFACE_WG, heights, state);
				if (y <= heights.getMinBuildHeight() + 2) return Optional.empty(); // over the abyss
				samples[n++] = y;
			}
		}
		Arrays.sort(samples);
		if (samples[8] - samples[0] > MAX_RELIEF) return Optional.empty();
		int platformY = samples[4];
		Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
		return Optional.of(new GenerationStub(new BlockPos(x, platformY, z), builder -> builder.addPiece(new GhostPalacePiece(x, platformY, z, facing))));
	}
}
