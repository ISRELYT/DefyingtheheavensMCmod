package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A Ranking Stele read (RankingSteleBlock): every world's list in the Hall's ink and gold (TalkUi, HallHud), drawn by {@link RankingView}; a
 * line of how the lists are ordered at its foot.
 */
public class RankingScreen extends Screen implements HallFrame.Framed {
	private static final int W = 300, H = 222;
	private final RankingView view = new RankingView();
	private int left, top;

	public RankingScreen() {
		super(Component.translatable(ModLang.RANKING_TITLE));
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

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		TalkUi.panel(g, left, top, left + W, top + H, TalkUi.GOLD);
		HallHud.heading(g, font, title, left + 10, top + 7, W - 20);
		super.render(g, mouseX, mouseY, partialTick);
		view.render(g, font, left + 10, top + 24, W - 26, H - 24 - 18, mouseX, mouseY);
		Component hint = Component.translatable(ModLang.RANKING_HINT);
		String text = font.plainSubstrByWidth(hint.getString(), W - 16);
		g.drawString(font, text, left + (W - font.width(text)) / 2, top + H - 13, TalkUi.MUTED, false);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && view.mouseClicked(mouseX, mouseY)) return true;
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		view.scroll(delta);
		return true;
	}
}
