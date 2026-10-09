package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.RingContainer;
import com.example.defyingtheheavens.RingSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The bauble panel, Curios-style: a small ring button at the top left of the player preview in the survival inventory
 * opens a panel on the left of the inventory holding the bauble slots (the two ring slots for now). Drawn and clicked
 * through client/mixin/InventoryScreenMixin. The creative inventory tab shows the ring slots all the time, as before.
 */
public final class BaublePanel {
	/** Button, relative to the inventory window: top left of the player preview box. */
	public static final int BUTTON_X = 27, BUTTON_Y = 9, BUTTON_SIZE = 10;
	/** Panel, relative to the inventory window: a column left of it, with room for every bauble slot. */
	public static final int PANEL_X = RingSlot.INVENTORY_X - 8, PANEL_Y = RingSlot.INVENTORY_Y - 8;
	public static final int PANEL_W = 28, PANEL_H = 14 + RingContainer.SIZE * RingSlot.SPACING;

	public static final Component TITLE = Component.translatable("gui.defying-the-heavens.baubles");
	private static final ResourceLocation BUTTON = DefyingTheHeavens.id("textures/gui/bauble_button.png");
	private static final ResourceLocation INVENTORY = new ResourceLocation("textures/gui/container/inventory.png");

	private static boolean open;

	public static void register() {
		RingSlot.setVisibility(() -> {
			var screen = Minecraft.getInstance().screen;
			return screen instanceof CreativeModeInventoryScreen || (open && screen instanceof InventoryScreen);
		});
	}

	public static boolean isOpen() { return open; }
	public static void toggle() { open = !open; }
	public static void close() { open = false; }

	public static boolean isOverButton(int left, int top, double mouseX, double mouseY) {
		return mouseX >= left + BUTTON_X && mouseX < left + BUTTON_X + BUTTON_SIZE
				&& mouseY >= top + BUTTON_Y && mouseY < top + BUTTON_Y + BUTTON_SIZE;
	}

	public static boolean isOverPanel(int left, int top, double mouseX, double mouseY) {
		return mouseX >= left + PANEL_X && mouseX < left + PANEL_X + PANEL_W
				&& mouseY >= top + PANEL_Y && mouseY < top + PANEL_Y + PANEL_H;
	}

	/** The toggle button: a small gold ring, lit while hovered or while the panel is open. */
	public static void renderButton(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
		boolean lit = open || isOverButton(left, top, mouseX, mouseY);
		graphics.blit(BUTTON, left + BUTTON_X, top + BUTTON_Y, lit ? BUTTON_SIZE : 0, 0, BUTTON_SIZE, BUTTON_SIZE, 32, 16);
	}

	/** The panel: a small window in the vanilla style (dark outline, light bevel) with a frame for each slot. */
	public static void renderPanel(GuiGraphics graphics, int left, int top) {
		int x0 = left + PANEL_X, y0 = top + PANEL_Y, x1 = x0 + PANEL_W, y1 = y0 + PANEL_H;
		graphics.fill(x0 + 1, y0, x1 - 1, y1, 0xFF000000);         // outline, with the corners clipped like vanilla
		graphics.fill(x0, y0 + 1, x1, y1 - 1, 0xFF000000);
		graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xFFC6C6C6); // body
		graphics.fill(x0 + 1, y0 + 1, x1 - 3, y0 + 3, 0xFFFFFFFF); // light top and left edges
		graphics.fill(x0 + 1, y0 + 1, x0 + 3, y1 - 3, 0xFFFFFFFF);
		graphics.fill(x0 + 3, y1 - 3, x1 - 1, y1 - 1, 0xFF555555); // shaded bottom and right edges
		graphics.fill(x1 - 3, y0 + 3, x1 - 1, y1 - 1, 0xFF555555);
		for (int i = 0; i < RingContainer.SIZE; i++) {
			// The offhand slot's frame (76, 61 in the inventory texture) doubles as a bauble slot frame.
			graphics.blit(INVENTORY, left + RingSlot.INVENTORY_X - 1, top + RingSlot.INVENTORY_Y - 1 + i * RingSlot.SPACING, 76, 61, 18, 18);
		}
	}

	private BaublePanel() {}
}
