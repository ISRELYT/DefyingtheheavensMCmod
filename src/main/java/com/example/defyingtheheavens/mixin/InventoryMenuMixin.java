package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.RingContainer;
import com.example.defyingtheheavens.RingSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Appends the two ring slots after vanilla's last slot (the offhand), so every vanilla slot index stays the same.
 * Vanilla's shift-click already sends anything past the offhand back to the main inventory.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {
	protected InventoryMenuMixin(MenuType<?> type, int containerId) {
		super(type, containerId);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void dth$addRingSlots(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
		RingContainer rings = RingContainer.of(owner);
		for (int i = 0; i < RingContainer.SIZE; i++) {
			addSlot(new RingSlot(rings, i, RingSlot.INVENTORY_X, RingSlot.INVENTORY_Y + i * RingSlot.SPACING));
		}
	}

	/** Shift-clicking a ring in the inventory, hotbar or offhand puts it on if a ring slot is free. */
	@Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
	private void dth$quickMoveRing(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
		if (index < InventoryMenu.INV_SLOT_START || index > InventoryMenu.SHIELD_SLOT) return;
		Slot slot = slots.get(index);
		ItemStack stack = slot.getItem();
		if (!RingContainer.isRing(stack)) return;

		ItemStack original = stack.copy();
		if (!moveItemStackTo(stack, RingContainer.FIRST_MENU_SLOT, RingContainer.FIRST_MENU_SLOT + RingContainer.SIZE, false)) {
			return; // both ring slots taken: fall back to vanilla's inventory <-> hotbar move
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		cir.setReturnValue(original);
	}
}
