package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.RingContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The creative "Survival Inventory" tab lays out the player's menu slots by index; anything past the offhand would land
 * on top of the hotbar. Moves the ring slots to a column right of the armor (mirroring the offhand on the left).
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
	@Unique
	private static final int RING_X = 127;
	@Unique
	private static final int RING_Y = 6;
	@Unique
	private static final int RING_SPACING = 27;

	@Shadow
	private static CreativeModeTab selectedTab;

	protected CreativeModeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Unique
	private static int dth$ringIndex(int menuIndex) {
		int ring = menuIndex - RingContainer.FIRST_MENU_SLOT;
		return ring >= 0 && ring < RingContainer.SIZE ? ring : -1;
	}

	@ModifyArg(method = "selectTab", index = 2, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"))
	private int dth$ringSlotX(Slot target, int index, int x, int y) {
		return dth$ringIndex(index) >= 0 ? RING_X : x;
	}

	@ModifyArg(method = "selectTab", index = 3, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;<init>(Lnet/minecraft/world/inventory/Slot;III)V"))
	private int dth$ringSlotY(Slot target, int index, int x, int y) {
		int ring = dth$ringIndex(index);
		return ring >= 0 ? RING_Y + ring * RING_SPACING : y;
	}

	@Inject(method = "renderBg", at = @At("TAIL"))
	private void dth$drawRingSlots(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
		if (selectedTab.getType() != CreativeModeTab.Type.INVENTORY) return;
		for (int i = 0; i < RingContainer.SIZE; i++) {
			// Borrow the survival inventory's offhand frame (76, 61); the creative texture has no spare one.
			graphics.blit(INVENTORY_LOCATION, leftPos + RING_X - 1, topPos + RING_Y - 1 + i * RING_SPACING, 76, 61, 18, 18);
		}
	}
}
