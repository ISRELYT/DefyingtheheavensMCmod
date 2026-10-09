package com.example.defyingtheheavens;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** One ring slot in the player's inventory menu. */
public class RingSlot extends Slot {
	/** Survival inventory: a column directly above the offhand slot (77, 62). */
	public static final int INVENTORY_X = 77;
	public static final int INVENTORY_Y = 26;
	public static final int SPACING = 18;

	private static final ResourceLocation EMPTY_ICON = new ResourceLocation(DefyingTheHeavens.MOD_ID, "item/empty_ring_slot");

	public RingSlot(Container rings, int index, int x, int y) {
		super(rings, index, x, y);
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return RingContainer.isRing(stack);
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
		return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_ICON);
	}
}
