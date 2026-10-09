package com.example.defyingtheheavens;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Grows a Spirit Peach Tree where a sapling stands: the heart at the base (it keeps the tree's age and bears fruit),
 * a cherry-wood trunk above it, and a rounded canopy of Spirit Peach leaves, about 6 blocks tall and 5 across.
 */
public final class SpiritPeachTree {
    /** Builds the tree. @return false, changing nothing, if there isn't room for the trunk */
    public static boolean grow(ServerLevel level, BlockPos base, RandomSource random) {
        int trunk = 4 + random.nextInt(2); // heart + logs
        for (int y = 1; y <= trunk + 1; y++) {
            if (!level.getBlockState(base.above(y)).canBeReplaced()) return false;
        }

        List<BlockPos> logs = new ArrayList<>();
        for (int y = 1; y < trunk; y++) logs.add(base.above(y));

        Set<BlockPos> leaves = new HashSet<>();
        int top = trunk - 1;
        for (int dy = -1; dy <= 2; dy++) {
            int radius = dy <= 0 ? 2 : 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius;
                    if (corner && (radius == 2 || dy == 2 || random.nextBoolean())) continue; // rounded
                    BlockPos p = base.offset(dx, top + dy, dz);
                    if (!logs.contains(p) && level.getBlockState(p).canBeReplaced()) leaves.add(p);
                }
            }
        }

        level.setBlock(base, ModBlocks.SPIRIT_PEACH_HEART.defaultBlockState(), Block.UPDATE_ALL);
        BlockState log = Blocks.CHERRY_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        for (BlockPos p : logs) level.setBlock(p, log, Block.UPDATE_ALL);
        Map<BlockPos, Integer> distance = distances(leaves, logs, base);
        for (BlockPos p : leaves) {
            level.setBlock(p, ModBlocks.SPIRIT_PEACH_LEAVES.defaultBlockState()
                    .setValue(LeavesBlock.DISTANCE, Math.min(7, distance.getOrDefault(p, 7)))
                    .setValue(LeavesBlock.PERSISTENT, false), Block.UPDATE_ALL);
        }
        return true;
    }

    /** Each leaf's step distance to the nearest log (what vanilla leaves use to decide whether to decay). */
    private static Map<BlockPos, Integer> distances(Set<BlockPos> leaves, List<BlockPos> logs, BlockPos heart) {
        Map<BlockPos, Integer> distance = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> wood = new ArrayList<>(logs);
        wood.add(heart);
        for (BlockPos log : wood) {
            for (Direction d : Direction.values()) {
                BlockPos n = log.relative(d);
                if (leaves.contains(n) && !distance.containsKey(n)) {
                    distance.put(n, 1);
                    queue.add(n);
                }
            }
        }
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (leaves.contains(n) && !distance.containsKey(n)) {
                    distance.put(n, distance.get(p) + 1);
                    queue.add(n);
                }
            }
        }
        return distance;
    }

    private SpiritPeachTree() {}
}
