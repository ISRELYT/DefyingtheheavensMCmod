package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.RingContainer;
import com.example.defyingtheheavens.RingSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws frames for the ring slots, which the vanilla inventory texture doesn't have. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends EffectRenderingInventoryScreen<InventoryMenu> {
	protected InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "renderBg", at = @At("TAIL"))
	private void dth$drawRingSlots(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
		for (int i = 0; i < RingContainer.SIZE; i++) {
			// The offhand slot's frame (76, 61 in the texture) doubles as the ring slot frame.
			graphics.blit(INVENTORY_LOCATION, leftPos + RingSlot.INVENTORY_X - 1, topPos + RingSlot.INVENTORY_Y - 1 + i * RingSlot.SPACING,
					76, 61, 18, 18);
		}
	}
}
