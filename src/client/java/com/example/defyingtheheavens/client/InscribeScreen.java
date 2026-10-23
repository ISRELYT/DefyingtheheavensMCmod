package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.Spell;
import com.example.defyingtheheavens.Technique;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Writing out a manual (TechniqueManualItem#inscribe: the Inscription Brush in one hand, a Blank Manual in the other), in the mod's
 * lacquer and gold (TalkUi): what one may write, techniques first then spells, one row each; a click writes it (C2S inscribe) and
 * closes. The list scrolls with the mouse wheel.
 */
public class InscribeScreen extends Screen {
	private static final int W = 260, H = 210, ROW_H = 14, LIST_Y = 40;

	/** Kind (0 a technique, 1 a spell) and ordinal, as the server listed them (S2C inscribe_options). */
	private final List<int[]> options;
	private int left, top, scroll;

	public InscribeScreen(List<int[]> options) {
		super(Component.translatable(ModLang.INSCRIBE_TITLE));
		this.options = options;
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H) / 2;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private int visibleRows() { return (H - LIST_Y - 10) / ROW_H; }

	private int maxScroll() { return Math.max(0, options.size() - visibleRows()); }

	private int rowAt(double mouseX, double mouseY) {
		if (mouseX < left + 10 || mouseX >= left + W - 10) return -1;
		int row = (int) Math.floor((mouseY - (top + LIST_Y)) / ROW_H);
		if (mouseY < top + LIST_Y || row < 0 || row >= visibleRows()) return -1;
		int index = row + scroll;
		return index < options.size() ? index : -1;
	}

	private Component label(int[] option) {
		if (option[0] == 0) {
			Technique technique = Technique.byIndex(option[1]);
			return technique == null ? Component.literal("?") : Component.translatable(ModLang.INSCRIBE_TECHNIQUE, technique.getDisplayName());
		}
		Spell spell = Spell.byIndex(option[1]);
		return spell == null ? Component.literal("?") : spell.getDisplayName();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int index = rowAt(mouseX, mouseY);
		if (button == 0 && index >= 0) {
			FriendlyByteBuf buf = PacketByteBufs.create();
			buf.writeVarInt(options.get(index)[0]);
			buf.writeVarInt(options.get(index)[1]);
			ClientPlayNetworking.send(ModPackets.INSCRIBE, buf);
			if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			onClose();
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		scroll = Mth.clamp(scroll - (int) Math.signum(delta), 0, maxScroll());
		return true;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		int x = left, y = top;
		TalkUi.panel(g, x, y, x + W, y + H, TalkUi.GOLD);
		g.drawString(font, title, x + (W - font.width(title)) / 2, y + 7, TalkUi.GOLD_TEXT, false);
		g.fill(x + 14, y + 19, x + W - 14, y + 20, TalkUi.GOLD);
		g.fill(x + 14, y + 20, x + W - 14, y + 21, TalkUi.GOLD_DIM);
		Component hint = Component.translatable(ModLang.INSCRIBE_HINT);
		g.drawString(font, hint, x + (W - font.width(hint)) / 2, y + 26, TalkUi.MUTED, false);
		int hovered = rowAt(mouseX, mouseY);
		for (int row = 0; row < visibleRows() && row + scroll < options.size(); row++) {
			int index = row + scroll;
			int ry = y + LIST_Y + row * ROW_H;
			if (index == hovered) g.fill(x + 10, ry, x + W - 10, ry + ROW_H - 1, TalkUi.PANEL_LIT);
			Component label = label(options.get(index));
			String text = label.getString();
			int room = W - 30;
			if (font.width(text) > room) text = font.plainSubstrByWidth(text, room - font.width("...")) + "...";
			g.drawString(font, text, x + 14, ry + 3, index == hovered ? TalkUi.CREAM_BRIGHT : TalkUi.CREAM, false);
		}
		if (maxScroll() > 0) {
			int track = visibleRows() * ROW_H, knob = Math.max(10, track * visibleRows() / options.size());
			int ky = y + LIST_Y + (track - knob) * scroll / maxScroll();
			g.fill(x + W - 8, ky, x + W - 6, ky + knob, TalkUi.GOLD_DIM);
		}
		super.render(g, mouseX, mouseY, partialTick);
	}
}
