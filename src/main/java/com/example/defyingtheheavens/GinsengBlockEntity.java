package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A growing ginseng's age, kept exactly like a Cultivation Fruit's (see CultivationFruitBlockEntity): a starting age and
 * the game time it started from, so it ages with server time even while its chunk is unloaded. Shared by Ginseng and
 * Spirit Ginseng.
 */
public class GinsengBlockEntity extends BlockEntity {
    private int initialYears = 1;
    private long plantedAt = -1;

    public GinsengBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GINSENG, pos, state);
    }

    @Override
    public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        // Only set the field here, never setChanged(): see CultivationFruitBlockEntity (new worlds hung while loading).
        if (!level.isClientSide && plantedAt < 0) plantedAt = level.getGameTime();
    }

    /** Worldgen: the plant has no level yet, so the clock is started from the world's time when it is generated. */
    public void setWildAge(int years, long gameTime) {
        initialYears = FruitAge.clamp(years);
        plantedAt = gameTime;
    }

    /** Sets the age and restarts the clock from now. */
    public void setAge(int years) {
        initialYears = FruitAge.clamp(years);
        plantedAt = level == null ? -1 : level.getGameTime();
        setChanged();
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

    /** Sent with the chunk, so the client works out the age (and aura) from the same clock as the server. */
    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
