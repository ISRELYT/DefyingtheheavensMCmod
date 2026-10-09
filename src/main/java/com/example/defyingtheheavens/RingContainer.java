package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

/**
 * The player's two ring slots. Lives on the Player entity (see mixin/PlayerMixin), is saved with the player's data,
 * and is exposed as two extra slots at the end of the player's InventoryMenu, which also keeps the client in sync.
 */
public class RingContainer extends SimpleContainer {
	public static final int SIZE = 2;
	/** Menu index of the first ring slot: vanilla's InventoryMenu ends with the offhand slot. */
	public static final int FIRST_MENU_SLOT = InventoryMenu.SHIELD_SLOT + 1;
	public static final String NBT_KEY = "DefyingTheHeavensRings";

	public RingContainer() {
		super(SIZE);
	}

	public static RingContainer of(Player player) {
		return ((RingHolder) player).dth$getRings();
	}

	public static boolean isRing(ItemStack stack) {
		return stack.getItem() instanceof RingItem;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return isRing(stack);
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	/** Keeps each ring in the slot it was worn in (SimpleContainer's own NBT helpers don't). */
	public ListTag save() {
		ListTag list = new ListTag();
		for (int i = 0; i < getContainerSize(); i++) {
			ItemStack stack = getItem(i);
			if (stack.isEmpty()) continue;
			CompoundTag tag = stack.save(new CompoundTag());
			tag.putByte("Slot", (byte) i);
			list.add(tag);
		}
		return list;
	}

	public void load(ListTag list) {
		clearContent();
		for (int i = 0; i < list.size(); i++) {
			CompoundTag tag = list.getCompound(i);
			int slot = tag.getByte("Slot") & 255;
			if (slot < getContainerSize()) {
				setItem(slot, ItemStack.of(tag));
			}
		}
	}

	public void copyFrom(RingContainer other) {
		for (int i = 0; i < getContainerSize(); i++) {
			setItem(i, other.getItem(i).copy());
		}
	}
}
