package com.example.defyingtheheavens;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Plants one wild ginseng on open soil near the placement origin, with an age rolled by {@link FruitAge#natural}. Like
 * CultivationFruitFeature it scans whole columns, so every Upper Realm island tier qualifies, and runs after trees (it is
 * appended to the end of vegetal decoration), so it grows in the shade of forests that are already there.
 * How often it runs, and where, is set by the placed features (see ModPlacedFeatures) and ModFeatures#register.
 */
public class GinsengFeature extends Feature<NoneFeatureConfiguration> {
    private static final int ATTEMPTS = 16;
    /** Columns are picked within this many blocks of the origin, keeping writes inside the chunks worldgen allows. */
    private static final int SPREAD = 7;
    private final Supplier<Block> plant;

    public GinsengFeature(Supplier<Block> plant) {
        super(NoneFeatureConfiguration.CODEC);
        this.plant = plant;
    }

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
            if (!level.setBlock(pos, plant.get().defaultBlockState(), Block.UPDATE_CLIENTS)) continue;
            if (level.getBlockEntity(pos) instanceof GinsengBlockEntity ginseng) {
                ginseng.setWildAge(FruitAge.natural(random), level.getLevel().getGameTime());
            }
            return true;
        }
        return false;
    }

    /**
     * Every air block in the column resting on grass or podzol, on any island tier. Only those two: they form only under
     * open sky, so ginseng never sprouts on the dirt of a cave floor.
     */
    private static void collectSpots(WorldGenLevel level, int x, int z, List<BlockPos> spots) {
        ChunkAccess chunk = level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int index = chunk.getSectionsCount() - 1; index >= 0; index--) {
            LevelChunkSection section = chunk.getSection(index);
            if (section.hasOnlyAir()) continue; // most of the Upper Realm's sky
            int bottom = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
            // Scan the soil, not the air above it: the soil's own section is never all air.
            for (int y = bottom + 15; y >= bottom; y--) {
                if (y + 1 >= level.getMaxBuildHeight()) continue;
                BlockState soil = chunk.getBlockState(cursor.set(x, y, z));
                if (!soil.is(Blocks.GRASS_BLOCK) && !soil.is(Blocks.PODZOL)) continue;
                if (chunk.getBlockState(cursor.set(x, y + 1, z)).isAir()) spots.add(new BlockPos(x, y + 1, z));
            }
        }
    }
}
