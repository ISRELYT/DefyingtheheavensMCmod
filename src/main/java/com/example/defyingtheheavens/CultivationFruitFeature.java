package com.example.defyingtheheavens;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Hangs one wild Cultivation Fruit under the canopy of a naturally generated tree near the placement origin, with an
 * age rolled by {@link FruitAge#natural}. Runs after trees (it is appended to the end of vegetal decoration), scans
 * whole columns so trees on every Upper Realm island tier qualify, and does nothing if no tree is nearby.
 * How often it runs is set by the placed features (see ModPlacedFeatures).
 */
public class CultivationFruitFeature extends Feature<NoneFeatureConfiguration> {
    private static final int ATTEMPTS = 16;
    /** Columns are picked within this many blocks of the origin, keeping writes inside the chunks worldgen allows. */
    private static final int SPREAD = 7;

    public CultivationFruitFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        List<BlockPos> spots = new ArrayList<>();
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int x = origin.getX() + random.nextInt(SPREAD * 2 + 1) - SPREAD;
            int z = origin.getZ() + random.nextInt(SPREAD * 2 + 1) - SPREAD;
            spots.clear();
            collectSpots(level, x, z, spots);
            if (spots.isEmpty()) continue;
            BlockPos pos = spots.get(random.nextInt(spots.size()));
            if (!level.setBlock(pos, ModBlocks.CULTIVATION_FRUIT.defaultBlockState(), Block.UPDATE_CLIENTS)) continue;
            if (level.getBlockEntity(pos) instanceof CultivationFruitBlockEntity fruit) {
                fruit.setWildAge(FruitAge.natural(random), level.getLevel().getGameTime());
            }
            return true;
        }
        return false;
    }

    /** Every air block in the column directly beneath natural (non-player-placed) leaves. */
    private static void collectSpots(WorldGenLevel level, int x, int z, List<BlockPos> spots) {
        ChunkAccess chunk = level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int index = chunk.getSectionsCount() - 1; index >= 0; index--) {
            LevelChunkSection section = chunk.getSection(index);
            if (section.hasOnlyAir()) continue; // most of the Upper Realm's sky
            int bottom = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
            for (int y = bottom + 15; y >= bottom; y--) {
                if (y - 1 < level.getMinBuildHeight() || !isNaturalLeaves(chunk.getBlockState(cursor.set(x, y, z)))) continue;
                BlockPos below = new BlockPos(x, y - 1, z);
                if (chunk.getBlockState(below).isAir()) spots.add(below);
            }
        }
    }

    private static boolean isNaturalLeaves(BlockState state) {
        return state.is(BlockTags.LEAVES) && !(state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT));
    }
}
