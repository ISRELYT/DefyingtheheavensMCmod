package com.example.defyingtheheavens.client.fx;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The ground's heights round a point, read once (as a cinematic begins), so rings, cracks and pools of light can lie over the terrain
 * (the brief §6.3: "they follow the terrain"). Each column's height is the top of its highest solid block (or a water surface) within
 * reach of the start's height; leaves and plants don't count. Sampled smoothly between columns but never under a column's own top,
 * so what lies on it doesn't sink into a step.
 */
public final class HeightField {
	private final int x0, z0, size;
	private final float[] heights;
	private final float fallback;

	private HeightField(int x0, int z0, int size, float[] heights, float fallback) {
		this.x0 = x0;
		this.z0 = z0;
		this.size = size;
		this.heights = heights;
		this.fallback = fallback;
	}

	/** The heights {@code half} blocks each way round (x, z), searched from 8 blocks over {@code y} down to 24 under it. */
	public static HeightField of(Level level, double x, double y, double z, int half) {
		int size = half * 2 + 1, bx = (int) Math.floor(x) - half, bz = (int) Math.floor(z) - half;
		float[] h = new float[size * size];
		int top = (int) Math.floor(y) + 8, bottom = (int) Math.floor(y) - 24;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				float found = (float) y;
				for (int yy = top; yy >= bottom; yy--) {
					pos.set(bx + i, yy, bz + j);
					BlockState state = level.getBlockState(pos);
					if (!state.getFluidState().isEmpty()) {
						found = yy + state.getFluidState().getHeight(level, pos);
						break;
					}
					if (state.isAir() || state.is(BlockTags.LEAVES)) continue;
					VoxelShape shape = state.getCollisionShape(level, pos);
					if (shape.isEmpty()) continue;
					found = (float) (yy + shape.max(Direction.Axis.Y));
					break;
				}
				h[i * size + j] = found;
			}
		}
		return new HeightField(bx, bz, size, h, (float) y);
	}

	/** The ground's height at (x, z): smooth between columns, never under the column's own top; the start's height beyond the field. */
	public float at(double x, double z) {
		double fx = x - x0 - 0.5, fz = z - z0 - 0.5;
		int i = (int) Math.floor(fx), j = (int) Math.floor(fz);
		if (i < 0 || j < 0 || i + 1 >= size || j + 1 >= size) return column((int) Math.floor(x - x0), (int) Math.floor(z - z0));
		float tx = (float) (fx - i), tz = (float) (fz - j);
		float a = heights[i * size + j], b = heights[(i + 1) * size + j], c = heights[i * size + j + 1], d = heights[(i + 1) * size + j + 1];
		float smooth = (a * (1 - tx) + b * tx) * (1 - tz) + (c * (1 - tx) + d * tx) * tz;
		return Math.max(smooth, column((int) Math.floor(x - x0), (int) Math.floor(z - z0)));
	}

	private float column(int i, int j) {
		if (i < 0 || j < 0 || i >= size || j >= size) return fallback;
		return heights[i * size + j];
	}
}
