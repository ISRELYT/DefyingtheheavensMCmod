package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.TalkIntent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The talk wheel: what you mean by talking to a cultivator, set round the crosshair (see TalkSystem). Point at one and
 * click, or press its number. Greet, Gift and Provoke are answered in a line over the hotbar and the wheel closes; Ask
 * opens the dialogue box. The cultivator's name plate sits at the top, and a strip at the foot says what the choice under
 * the pointer will do. The world keeps running behind it; walking away or a fight closes it.
 */
public class TalkWheelScreen extends Screen {
	private static final int TILE_W = 72;
	private static final int TILE_H = 26;
	private static final int RADIUS = 60;

	final TalkSession session;
	private int hovered = -1;
	/** Ask was chosen: waiting for the dialogue box. */
	private boolean asking;

	TalkWheelScreen(TalkSession session) {
		super(Component.literal(session.name));
		this.session = session;
	}

	/**
	 * The wheel spreads sideways with more choices (up to eight, two realms above someone under your pressure), so the tiles
	 * don't touch; its height stays, clear of the name plate above and the hint below.
	 */
	private int radiusX() {
		return Math.max(RADIUS, session.intents.size() * 15);
	}

	private int tileX(int i) {
		double angle = Math.toRadians(-90 + 360.0 * i / session.intents.size());
		return width / 2 + (int) Math.round(Math.cos(angle) * radiusX()) - TILE_W / 2;
	}

	private int tileY(int i) {
		double angle = Math.toRadians(-90 + 360.0 * i / session.intents.size());
		return height / 2 + (int) Math.round(Math.sin(angle) * (RADIUS * 0.8)) - TILE_H / 2;
	}

	private int tileAt(double mouseX, double mouseY) {
		for (int i = 0; i < session.intents.size(); i++) {
			int x = tileX(i), y = tileY(i);
			if (mouseX >= x && mouseX < x + TILE_W && mouseY >= y && mouseY < y + TILE_H) return i;
		}
		return -1;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, width, height, 0x70080604);
		int cx = width / 2, cy = height / 2;
		// The name plate, centred at the top.
		int plateW = Math.max(font.width(session.name), TalkUi.chipsWidth(font, session));
		TalkUi.namePlate(g, font, session, cx - plateW / 2, 14);
		// The crosshair the wheel turns about.
		g.fill(cx - 4, cy, cx + 5, cy + 1, 0xFFFFFFFF);
		g.fill(cx, cy - 4, cx + 1, cy + 5, 0xFFFFFFFF);
		hovered = tileAt(mouseX, mouseY);
		for (int i = 0; i < session.intents.size(); i++) {
			TalkIntent intent = session.intents.get(i);
			int x = tileX(i), y = tileY(i);
			boolean lit = i == hovered;
			int border = lit ? TalkUi.GOLD : intent == TalkIntent.PROVOKE ? TalkUi.RED_DIM : TalkUi.GOLD_DIM;
			g.fill(x, y, x + TILE_W, y + TILE_H, border);
			g.fill(x + 2, y + 2, x + TILE_W - 2, y + TILE_H - 2, lit ? TalkUi.PANEL_LIT : TalkUi.PANEL);
			Component label = Component.translatable(ModLang.talkIntent(intent.getId()));
			int color = lit ? TalkUi.CREAM_BRIGHT : intent == TalkIntent.PROVOKE ? 0xE8A89A : TalkUi.CREAM;
			g.drawCenteredString(font, label, x + TILE_W / 2, y + 9, color);
			g.drawString(font, Integer.toString(i + 1), x + 5, y + 4, TalkUi.MUTED, false);
		}
		// What the choice under the pointer does.
		if (hovered >= 0) {
			TalkIntent intent = session.intents.get(hovered);
			int w = Math.min(260, width - 24);
			List<FormattedCharSequence> lines = font.split(Component.translatable(ModLang.talkIntentHint(intent.getId())), w - 16);
			int h = 20 + lines.size() * 10;
			int x0 = cx - w / 2, y0 = height - h - 12;
			TalkUi.panel(g, x0, y0, x0 + w, y0 + h, TalkUi.GOLD);
			g.drawString(font, Component.translatable(ModLang.talkIntent(intent.getId())), x0 + 8, y0 + 6, TalkUi.GOLD_TEXT, false);
			int ly = y0 + 17;
			for (FormattedCharSequence line : lines) {
				g.drawString(font, line, x0 + 8, ly, TalkUi.CREAM, false);
				ly += 10;
			}
		}
		g.drawString(font, Component.translatable(ModLang.TALK_PICK, session.intents.size()), 8, height - 14, TalkUi.MUTED, true);
		super.render(g, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int tile = tileAt(mouseX, mouseY);
		if (button == 0 && tile >= 0) {
			choose(session.intents.get(tile));
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		int index = key - GLFW.GLFW_KEY_1;
		if (index >= 0 && index < session.intents.size()) {
			choose(session.intents.get(index));
			return true;
		}
		return super.keyPressed(key, scanCode, modifiers);
	}

	private void choose(TalkIntent intent) {
		if (asking) return;
		session.sendIntent(intent);
		if (intent.isQuick()) {
			// The answer comes as a line over the hotbar; the server has already ended the talk.
			Minecraft.getInstance().setScreen(null);
		} else {
			asking = true;
		}
	}

	@Override
	public void tick() {
		if (session.entity() == null) onClose();
	}

	@Override
	public void onClose() {
		session.sendEnd();
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
