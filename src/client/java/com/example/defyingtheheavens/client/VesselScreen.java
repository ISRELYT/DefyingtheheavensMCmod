package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.SoulVesselEntity;
import com.example.defyingtheheavens.VesselMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * A seized player's body opened by the soul in it (VesselMenu), in the Hall's ink and gold (TalkUi, HallHud): its name, its gear along
 * the top (the armour at the left, the off hand and the hand it fights with at the right), a rule, its pack below. No inventory of the
 * soul's own: a soul carries nothing.
 */
public class VesselScreen extends AbstractContainerScreen<VesselMenu> implements HallFrame.Framed {
	private static final int W = 178, H = VesselMenu.PACK_Y + (SoulVesselEntity.PACK / 9) * 18 + 7;
	private static final int SLOT_BG = 0xFF0B0E12;

	public VesselScreen(VesselMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = W;
		imageHeight = H;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		renderTooltip(g, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		TalkUi.panel(g, x, y, x + W, y + H, TalkUi.GOLD);
		// The slots: ink wells in a dark-gold frame, the gear's in brighter gold.
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot slot = menu.slots.get(i);
			boolean gear = i >= SoulVesselEntity.PACK;
			HallHud.panel(g, x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, gear ? HallHud.FRAME_LIT : HallHud.FRAME, SLOT_BG);
		}
		// A gold rule between the gear and the pack.
		int ry = y + (VesselMenu.GEAR_Y + 16 + VesselMenu.PACK_Y) / 2;
		g.fill(x + VesselMenu.LEFT, ry, x + VesselMenu.LEFT + 9 * 18 - 2, ry + 1, TalkUi.GOLD_DIM);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		HallHud.heading(g, font, title, 10, 7, W - 20);
	}
}
