package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.FourAxis;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.Obsession;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * "Name Your Obsession" (Obsession), before the tribulation into Heavenly Being, in the mod's lacquer and gold (TalkUi, the Heaven
 * Altar's screen): the four obsessions, each with what it gives and its axis filling twice as fast. Click one (or press its number)
 * to choose it, then Face the Tribulation (or Enter): the server keeps it and the tribulation begins. It is named once and for good,
 * hence the two steps. Closed without naming, nothing happens: the Breakthrough button brings it up again.
 */
public class ObsessionScreen extends Screen {
	private static final int W = 332, H = 236;
	/** The four choices, two by two (wide enough for the longest English line on one row: 136 pixels inside). */
	private static final int CARD_W = 150, CARD_H = 52, CARD_GAP = 8, CARDS_Y = 64;
	private static final FourAxis.Axis[] CHOICES = FourAxis.Axis.values();

	/** Whose tribulation it stands before: the ghost path's (the Netherworld's), or the heavens'. */
	private final boolean ghost;
	private FourAxis.Axis chosen;
	private Button face;
	private int left, top;

	public ObsessionScreen(boolean ghost) {
		super(Component.translatable(ModLang.OBSESSION_TITLE));
		this.ghost = ghost;
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H) / 2;
		face = addRenderableWidget(Button.builder(Component.translatable(ModLang.OBSESSION_CONFIRM), b -> confirm())
				.bounds(left + (W - 150) / 2, top + H - 30, 150, 20).build());
		face.active = chosen != null;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private int cardX(int i) { return left + (W - 2 * CARD_W - CARD_GAP) / 2 + (i % 2) * (CARD_W + CARD_GAP); }

	private int cardY(int i) { return top + CARDS_Y + (i / 2) * (CARD_H + CARD_GAP); }

	private int cardAt(double mouseX, double mouseY) {
		for (int i = 0; i < CHOICES.length; i++) {
			int x = cardX(i), y = cardY(i);
			if (mouseX >= x && mouseX < x + CARD_W && mouseY >= y && mouseY < y + CARD_H) return i;
		}
		return -1;
	}

	private void choose(int i) {
		if (i < 0 || i >= CHOICES.length) return;
		chosen = CHOICES[i];
		face.active = true;
		if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	/** Named: told to the server, which keeps it and brings the tribulation. */
	private void confirm() {
		if (chosen == null) return;
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(chosen.ordinal());
		buf.writeBoolean(ghost);
		ClientPlayNetworking.send(ModPackets.OBSESSION_NAME, buf);
		onClose();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int i = cardAt(mouseX, mouseY);
		if (button == 0 && i >= 0) {
			choose(i);
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int key, int scan, int modifiers) {
		if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_4) {
			choose(key - GLFW.GLFW_KEY_1);
			return true;
		}
		if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && chosen != null) {
			confirm();
			return true;
		}
		return super.keyPressed(key, scan, modifiers);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		int x = left, y = top;
		TalkUi.panel(g, x, y, x + W, y + H, TalkUi.GOLD);
		// The title, a gold rule under it with a scroll roller's knob at each end (as the Heaven Altar's).
		g.drawString(font, title, x + (W - font.width(title)) / 2, y + 7, TalkUi.GOLD_TEXT, false);
		g.fill(x + 14, y + 19, x + W - 14, y + 20, TalkUi.GOLD);
		g.fill(x + 14, y + 20, x + W - 14, y + 21, TalkUi.GOLD_DIM);
		for (int kx : new int[] {x + 10, x + W - 14}) {
			g.fill(kx, y + 17, kx + 4, y + 23, TalkUi.GOLD);
			g.fill(kx + 1, y + 18, kx + 3, y + 22, TalkUi.GOLD_DIM);
		}
		// What it is, under the title.
		List<FormattedCharSequence> intro = font.split(Component.translatable(ModLang.OBSESSION_INTRO), W - 28);
		int ly = y + 27;
		for (FormattedCharSequence line : intro) {
			g.drawString(font, line, x + (W - font.width(line)) / 2, ly, TalkUi.CREAM, false);
			ly += 10;
		}
		int hovered = cardAt(mouseX, mouseY);
		for (int i = 0; i < CHOICES.length; i++) card(g, i, i == hovered);
		// Under the four: the one chosen, or how to choose.
		Component hint = chosen == null ? Component.translatable(ModLang.OBSESSION_CHOOSE)
				: Component.translatable(ModLang.OBSESSION_CHOSEN, Obsession.displayName(chosen));
		g.drawString(font, hint, x + (W - font.width(hint)) / 2, top + H - 44, chosen == null ? TalkUi.MUTED : TalkUi.GOLD_TEXT, false);
		super.render(g, mouseX, mouseY, partialTick);
	}

	/** One obsession: its number and name, what it gives, its axis at Four Axis; gold-framed when chosen, lit when the mouse is on it. */
	private void card(GuiGraphics g, int i, boolean hovered) {
		FourAxis.Axis axis = CHOICES[i];
		int x = cardX(i), y = cardY(i);
		boolean picked = axis == chosen;
		TalkUi.panel(g, x, y, x + CARD_W, y + CARD_H, picked ? TalkUi.GOLD : TalkUi.GOLD_DIM);
		if (hovered || picked) g.fill(x + 3, y + 3, x + CARD_W - 3, y + CARD_H - 3, TalkUi.PANEL_LIT);
		String number = (i + 1) + ".";
		g.drawString(font, number, x + 7, y + 6, TalkUi.MUTED, false);
		g.drawString(font, Obsession.displayName(axis), x + 7 + font.width(number) + 4, y + 6, picked ? TalkUi.CREAM_BRIGHT : TalkUi.GOLD_TEXT, false);
		int ly = y + 19;
		for (FormattedCharSequence line : font.split(Obsession.boon(axis), CARD_W - 14)) {
			g.drawString(font, line, x + 7, ly, TalkUi.CREAM, false);
			ly += 10;
		}
		for (FormattedCharSequence line : font.split(Component.translatable(ModLang.OBSESSION_AXIS), CARD_W - 14)) {
			if (ly > y + CARD_H - 11) break;
			g.drawString(font, line, x + 7, ly, TalkUi.MUTED, false);
			ly += 10;
		}
	}
}
