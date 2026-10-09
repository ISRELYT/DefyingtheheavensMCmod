package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
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
        // Only set the field here: setChanged() would look up this chunk while it is still loading, and the server
        // waits on itself forever (new worlds hung at 100%). Placing the block already marks the chunk changed.
        if (!level.isClientSide && plantedAt < 0) plantedAt = level.getGameTime();
    }

    /** Worldgen: the fruit has no level yet, so the clock is started from the world's time when it is generated. */
    public void setWildAge(int years, long gameTime) {
        initialYears = FruitAge.clamp(years);
        plantedAt = gameTime;
    }

    /** Sets the age and restarts the clock from now (for fruit already in the world). */
    public void setAge(int years) {
        initialYears = FruitAge.clamp(years);
        plantedAt = level == null ? -1 : level.getGameTime();
        setChanged();
        // Nearby clients draw the aura from the age, so tell them straight away.
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
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

    /** Sent with the chunk, so the client can work out the age (and aura) from the same clock as the server. */
    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
