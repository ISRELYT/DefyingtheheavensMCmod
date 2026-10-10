package com.example.defyingtheheavens;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Spirit Lotus (Amazing Cultivation Simulator's Five-Coloured Golden Lotus, the water spirit root): an aging herb like
 * ginseng that floats on still water instead of growing in soil. A lily pad as a seedling, a bud from its 4th year, and a
 * golden lotus with five-coloured petal tips once full grown. Everything else (age, harvest, seeds, qi feeding) is
 * {@link GinsengBlock}'s.
 */
public class SpiritLotusBlock extends GinsengBlock {
    private static final VoxelShape[] SHAPES = {box(1, 0, 1, 15, 1.5, 15), box(1, 0, 1, 15, 7, 15), box(1, 0, 1, 15, 13, 15)};

    public SpiritLotusBlock(Supplier<Item> harvest, Supplier<Item> seeds, Properties properties) {
        super(harvest, seeds, properties);
    }

    /** On a still water source, with nothing but air where it floats. */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        FluidState below = level.getFluidState(pos.below());
        return below.getType() == Fluids.WATER && level.getFluidState(pos).isEmpty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(STAGE)];
    }
}
