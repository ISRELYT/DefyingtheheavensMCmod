package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * A legend's stone (LegendStoneBlock): the sword resting in it (Ganjiang in its tomb, Moye in her pool: Legends), which its renderer draws
 * sunk point first in the stone; and the tomb's trial as it stands: which wave of sword spirits is up, which spirits they are, and whether
 * the last is beaten and the sword may be drawn (Legends#tombTrial).
 */
public class LegendStoneBlockEntity extends BlockEntity {
	private ItemStack held = ItemStack.EMPTY;
	private int wave;
	private boolean unlocked;
	private final List<UUID> spirits = new ArrayList<>();

	public LegendStoneBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LEGEND_STONE, pos, state);
	}

	/** The sword in it, or none. */
	public ItemStack held() { return held; }
	public int wave() { return wave; }
	public boolean unlocked() { return unlocked; }

	/** {@code stack} into the stone (Legends#putInStone), told to the clients near, who draw it. */
	public void setHeld(ItemStack stack) {
		held = stack;
		changed();
	}

	/** The sword drawn out of the stone (Legends#draw, Moye's ghost rising with hers). */
	public ItemStack take() {
		ItemStack taken = held;
		held = ItemStack.EMPTY;
		changed();
		return taken;
	}

	void setWave(int wave) {
		this.wave = wave;
		changed();
	}

	void unlock() {
		unlocked = true;
		changed();
	}

	/** One of this trial's guards risen: a tomb's sword spirit, the dead sun's Golden Crow. */
	void addSpirit(Entity guard) {
		spirits.add(guard.getUUID());
		setChanged();
	}

	/** This trial's guards still standing: any not found here is gone (slain, faded, or never saved: the spirits aren't). */
	List<LivingEntity> guards(ServerLevel level) {
		List<LivingEntity> standing = new ArrayList<>();
		for (Iterator<UUID> it = spirits.iterator(); it.hasNext(); ) {
			if (level.getEntity(it.next()) instanceof LivingEntity guard && guard.isAlive()) standing.add(guard);
			else it.remove();
		}
		return standing;
	}

	/** This trial's sword spirits still standing (the tomb's). */
	List<SwordSpirit> spirits(ServerLevel level) {
		List<SwordSpirit> standing = new ArrayList<>();
		for (LivingEntity guard : guards(level)) if (guard instanceof SwordSpirit spirit) standing.add(spirit);
		return standing;
	}

	/** The trial begun anew: its guards gone, no wave up, the sword held fast again. */
	void resetTrial(ServerLevel level) {
		for (LivingEntity guard : guards(level)) guard.discard();
		spirits.clear();
		wave = 0;
		unlocked = false;
		changed();
	}

	private void changed() {
		setChanged();
		if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (!held.isEmpty()) tag.put("Held", held.save(new CompoundTag()));
		tag.putInt("Wave", wave);
		tag.putBoolean("Unlocked", unlocked);
		ListTag list = new ListTag();
		for (UUID id : spirits) list.add(NbtUtils.createUUID(id));
		tag.put("Spirits", list);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		// An update without "Held" means the sword was drawn, so the client clears it too.
		held = tag.contains("Held") ? ItemStack.of(tag.getCompound("Held")) : ItemStack.EMPTY;
		wave = tag.getInt("Wave");
		unlocked = tag.getBoolean("Unlocked");
		spirits.clear();
		for (Tag id : tag.getList("Spirits", Tag.TAG_INT_ARRAY)) spirits.add(NbtUtils.loadUUID(id));
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = new CompoundTag();
		if (!held.isEmpty()) tag.put("Held", held.save(new CompoundTag()));
		// Never an empty tag (the update packet drops one): an emptied stone would go on showing its sword (as SpiritPedestalBlockEntity's).
		tag.putBoolean("Unlocked", unlocked);
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
