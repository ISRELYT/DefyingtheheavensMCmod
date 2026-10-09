package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CultivationFruitBlockEntity extends BlockEntity {
    private int initialYears = 1;
    private long plantedAt = -1;

    public CultivationFruitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CULTIVATION_FRUIT, pos, state);
    }

    @Override
    public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        if (!level.isClientSide && plantedAt < 0) setAge(initialYears);
    }

    public void setAge(int years) {
        initialYears = FruitAge.clamp(years);
        plantedAt = level == null ? -1 : level.getGameTime();
        setChanged();
    }

    public int age() {
        return FruitAge.at(initialYears, plantedAt, level == null ? plantedAt : level.getGameTime());
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("InitialYears", initialYears);
        tag.putLong("PlantedAt", plantedAt);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        initialYears = FruitAge.clamp(tag.getInt("InitialYears"));
        plantedAt = tag.contains("PlantedAt") ? tag.getLong("PlantedAt") : -1;
    }
}
