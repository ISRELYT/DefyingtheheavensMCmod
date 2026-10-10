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
import net.minecraft.world.level.material.FluidState;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Plants a small patch of a wild herb near the placement origin: on open soil (ginseng, lingzhi, huangjing, Spirit Dew
 * Grass) or on still water (Spirit Lotus). The patch has between minPatch and maxPatch plants (fewer if there isn't room),
 * all within {@link #PATCH_RADIUS} blocks of the first. Herbs that age ({@link GinsengBlock}) each get their own age, rolled
 * by {@link FruitAge#natural}. Like
 * CultivationFruitFeature it scans whole columns, so every Upper Realm island tier qualifies, and runs after trees (it is
 * appended to the end of vegetal decoration), so it grows in the shade of forests that are already there.
 * How often it runs, and where, is set by the placed features (see ModPlacedFeatures) and ModFeatures#register.
 */
public class GinsengFeature extends Feature<NoneFeatureConfiguration> {
    private static final int ATTEMPTS = 16;
    /**
     * Columns are picked within this many blocks of the origin, and the rest of a patch within {@link #PATCH_RADIUS} of
     * its first plant, keeping writes inside the chunks worldgen allows.
     */
    private static final int SPREAD = 7;
    public static final int PATCH_RADIUS = 3;
    /** A patch's other plants grow at most this many blocks above or below its first (so a patch stays on one slope). */
    private static final int PATCH_HEIGHT = 3;
    /** Where a herb takes root: open soil, or the surface of still water. */
    public enum Ground { SOIL, WATER }

    private final Supplier<Block> plant;
    private final Ground ground;
    private final int minPatch;
    private final int maxPatch;

    public GinsengFeature(Supplier<Block> plant, int minPatch, int maxPatch) {
        this(plant, Ground.SOIL, minPatch, maxPatch);
    }

    public GinsengFeature(Supplier<Block> plant, Ground ground, int minPatch, int maxPatch) {
        super(NoneFeatureConfiguration.CODEC);
        this.plant = plant;
        this.ground = ground;
        this.minPatch = minPatch;
        this.maxPatch = maxPatch;
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
            collectSpots(level, x, z, ground, spots);
            if (spots.isEmpty()) continue;
            BlockPos pos = spots.get(random.nextInt(spots.size()));
            if (!plantOne(level, pos, random)) continue;
            growPatch(level, pos, random, minPatch + random.nextInt(maxPatch - minPatch + 1));
            return true;
        }
        return false;
    }

    /**
     * Fills out a patch around its first plant at {@code first} until it has {@code size} plants, or gives up after a few
     * tries when there isn't room. Returns how many plants the patch has.
     */
    public int growPatch(WorldGenLevel level, BlockPos first, RandomSource random, int size) {
        int planted = 1;
        List<BlockPos> spots = new ArrayList<>();
        for (int attempt = 0; planted < size && attempt < size * 8; attempt++) {
            int x = first.getX() + random.nextInt(PATCH_RADIUS * 2 + 1) - PATCH_RADIUS;
            int z = first.getZ() + random.nextInt(PATCH_RADIUS * 2 + 1) - PATCH_RADIUS;
            spots.clear();
            collectSpots(level, x, z, ground, spots);
            BlockPos near = null;
            for (BlockPos spot : spots) {
                int dy = Math.abs(spot.getY() - first.getY());
                if (dy <= PATCH_HEIGHT && (near == null || dy < Math.abs(near.getY() - first.getY()))) near = spot;
            }
            if (near != null && plantOne(level, near, random)) planted++;
        }
        return planted;
    }

    /** One plant at {@code pos}, with a freshly rolled wild age if it is a herb that ages. */
    private boolean plantOne(WorldGenLevel level, BlockPos pos, RandomSource random) {
        int age = FruitAge.natural(random);
        BlockState state = plant.get().defaultBlockState();
        if (state.hasProperty(GinsengBlock.STAGE)) state = state.setValue(GinsengBlock.STAGE, GinsengBlock.stageFor(age));
        if (!level.setBlock(pos, state, Block.UPDATE_CLIENTS)) return false;
        if (level.getBlockEntity(pos) instanceof GinsengBlockEntity ginseng) {
            ginseng.setWildAge(age, level.getLevel().getGameTime());
        }
        return true;
    }

    /**
     * Every air block in the column resting on grass or podzol (or, for {@link Ground#WATER}, on a still water source), on
     * any island tier. Only those soils: they form only under open sky, so herbs never sprout on the dirt of a cave floor.
     */
    private static void collectSpots(WorldGenLevel level, int x, int z, Ground ground, List<BlockPos> spots) {
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
                if (ground == Ground.WATER) {
                    FluidState water = soil.getFluidState();
                    if (!water.is(FluidTags.WATER) || !water.isSource() || !soil.is(Blocks.WATER)) continue;
                } else if (!soil.is(Blocks.GRASS_BLOCK) && !soil.is(Blocks.PODZOL)) {
                    continue;
                }
                if (chunk.getBlockState(cursor.set(x, y + 1, z)).isAir()) spots.add(new BlockPos(x, y + 1, z));
            }
        }
    }
}
