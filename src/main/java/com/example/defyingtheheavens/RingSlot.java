package com.example.defyingtheheavens;

import com.mojang.datafixers.util.Pair;
import java.util.function.BooleanSupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * One ring slot in the player's inventory menu. The slots sit in the bauble panel to the left of the survival
 * inventory (client/BaublePanel), which a button in the inventory opens and closes; while it is closed the slots are
 * inactive, so they are neither drawn nor clickable. Shift-clicking a ring still puts it on.
 */
public class RingSlot extends Slot {
	/** Survival inventory: a column inside the bauble panel, left of the inventory window (negative = left of it). */
	public static final int INVENTORY_X = -23;
	public static final int INVENTORY_Y = 11;
	public static final int SPACING = 18;

	private static final ResourceLocation EMPTY_ICON = new ResourceLocation(DefyingTheHeavens.MOD_ID, "item/empty_ring_slot");

	/** Whether ring slots are showing. Always on the server; the client sets this to follow the bauble panel. */
	private static BooleanSupplier visible = () -> true;

	public RingSlot(Container rings, int index, int x, int y) {
		super(rings, index, x, y);
	}

	public static void setVisibility(BooleanSupplier supplier) {
		visible = supplier;
	}

	@Override
	public boolean isActive() {
		return visible.getAsBoolean();
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
