package com.example.defyingtheheavens;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Keeps a Netherworld feature off a ghost palace's ground (placement modifier {@code defying-the-heavens:no_ghost_palace}):
 * nothing is placed from a chunk the palace reaches, or one beside it. A feature spills into the chunks round the one it is
 * placed from, and they may be decorated after the palace's own chunks are built, so without this a neighbour's lake would cut
 * into its platform (GhostPalacePiece), a dead tree grow through its roof, or a spring break out of its foundation.
 */
public final class NoGhostPalaceFilter extends PlacementFilter {
	public static final NoGhostPalaceFilter INSTANCE = new NoGhostPalaceFilter();
	public static final Codec<NoGhostPalaceFilter> CODEC = Codec.unit(() -> INSTANCE);
	public static final PlacementModifierType<NoGhostPalaceFilter> TYPE = Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE,
			DefyingTheHeavens.id("no_ghost_palace"), () -> CODEC);

	private NoGhostPalaceFilter() {}

	/** Touching this class registers the type. */
	public static void register() {
	}

	@Override
	protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
		WorldGenLevel level = context.getLevel();
		Structure palace = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(ModStructures.GHOST_PALACE);
		if (palace == null) return true;
		int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (!level.hasChunk(cx + dx, cz + dz)) continue;
				ChunkAccess chunk = level.getChunk(cx + dx, cz + dz, ChunkStatus.STRUCTURE_REFERENCES, false);
				if (chunk != null && !chunk.getReferencesForStructure(palace).isEmpty()) return false;
			}
		}
		return true;
	}

	@Override
	public PlacementModifierType<?> type() {
		return TYPE;
	}
}
