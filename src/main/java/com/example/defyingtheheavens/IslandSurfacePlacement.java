package com.example.defyingtheheavens;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.stream.Stream;

/**
 * Picks one island top surface in the column at random (the air block just above a solid one), between min_y and
 * max_y. Unlike vanilla's heightmap placement this reaches every tier of stacked floating islands, and unlike a random
 * height plus environment scan it nearly always finds a surface when the column has any island at all.
 */
public class IslandSurfacePlacement extends PlacementModifier {
	public static final Codec<IslandSurfacePlacement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("min_y").forGetter(placement -> placement.minY),
			Codec.INT.fieldOf("max_y").forGetter(placement -> placement.maxY)
	).apply(instance, IslandSurfacePlacement::new));

	private final int minY;
	private final int maxY;

	public IslandSurfacePlacement(int minY, int maxY) {
		this.minY = minY;
		this.maxY = maxY;
	}

	@Override
	public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
		WorldGenLevel level = context.getLevel();
		int top = Math.min(maxY, level.getMaxBuildHeight() - 2);
		int bottom = Math.max(minY, level.getMinBuildHeight() + 1);
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos.getX(), top + 1, pos.getZ());
		boolean airAbove = level.getBlockState(cursor).isAir();
		int[] surfaces = new int[16];
		int found = 0;
		for (int y = top; y >= bottom; y--) {
			BlockState state = level.getBlockState(cursor.setY(y));
			boolean ground = !state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced();
			if (ground && airAbove) {
				if (found == surfaces.length) surfaces = java.util.Arrays.copyOf(surfaces, found * 2);
				surfaces[found++] = y + 1;
			}
			airAbove = state.isAir();
		}
		if (found == 0) return Stream.empty();
		return Stream.of(new BlockPos(pos.getX(), surfaces[random.nextInt(found)], pos.getZ()));
	}

	@Override
	public PlacementModifierType<?> type() {
		return ModFeatures.ISLAND_SURFACE;
	}
}
