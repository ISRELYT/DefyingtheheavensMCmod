package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds the one Cultivation Fruit on display. Harvested fruit no longer ages, so its age is fixed while it sits here. */
public class SpiritPedestalBlockEntity extends BlockEntity {
    private ItemStack fruit = ItemStack.EMPTY;

    public SpiritPedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPIRIT_PEDESTAL, pos, state);
    }

    public ItemStack getFruit() { return fruit; }

    public boolean hasFruit() { return !fruit.isEmpty(); }

    /** Age of the fruit on display, or 0 when the pedestal is empty. */
    public int fruitAge() { return hasFruit() ? CultivationFruitItem.age(fruit) : 0; }

    /** Puts a fruit on display (or clears it with {@link ItemStack#EMPTY}) and tells nearby clients, who draw it and its aura. */
    public void setFruit(ItemStack stack) {
        fruit = stack;
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (hasFruit()) tag.put("Fruit", fruit.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // An update without "Fruit" means it was taken off, so the client clears it too.
        fruit = tag.contains("Fruit") ? ItemStack.of(tag.getCompound("Fruit")) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = saveWithoutMetadata();
        // The update packet drops an empty tag instead of sending it, so an emptied pedestal would never reach the
        // client (it kept drawing the fruit that had been taken). This marker keeps the tag from ever being empty.
        if (!hasFruit()) tag.putBoolean("Empty", true);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
