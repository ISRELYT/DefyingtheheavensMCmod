package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Alignment;
import com.example.defyingtheheavens.CultivatorNpc;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.NpcTitle;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.Rankings;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * The lists drawn (Rankings, ClientRankings): a tab for each world's list (the player's own chosen first), the player's own place, then the
 * list's hundred, a row each (its place and name, gold for a player; its realm and stage on the right), its sect, title and path on hover.
 * Scrolls with the wheel. Shared by the Ranking Stele's screen (RankingScreen) and the Journal tab's second page (CultivationScreen).
 */
public final class RankingView {
	private static final int TAB_H = 12, ROW_H = 11;
	/** The list shown (Rankings.Board ordinal); -1: the player's own. */
	private int board = -1;
	private int scroll, maxScroll;
	private final List<int[]> tabs = new ArrayList<>();

	private int shown() {
		return board >= 0 ? board : Math.max(0, ClientRankings.ownBoard());
	}

	public void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY) {
		int shown = shown();
		tabs.clear();
		int tx = x;
		for (Rankings.Board b : Rankings.Board.values()) {
			Component label = Component.translatable(ModLang.rankingBoardShort(b.getId()));
			int tw = font.width(label) + 8;
			boolean on = b.ordinal() == shown, hover = mouseX >= tx && mouseX < tx + tw && mouseY >= y && mouseY < y + TAB_H;
			g.fill(tx, y, tx + tw, y + TAB_H, on ? 0x90D4A63A : hover ? 0x40FFFFFF : 0x40000000);
			g.drawString(font, label, tx + 4, y + 2, on ? 0xFFF1C8 : 0xB8B0A0, false);
			tabs.add(new int[] {tx, y, tw, TAB_H, b.ordinal()});
			tx += tw + 3;
		}
		int top = y + TAB_H + 4;
		Rankings.Board list = Rankings.Board.byIndex(shown);
		int own = ClientRankings.ownBoard();
		Component you = own < 0 ? null : ClientRankings.ownPlace() > 0
				? Component.translatable(ModLang.RANKING_YOU, ClientRankings.ownPlace(), Rankings.Board.byIndex(own).getDisplayName())
				: Component.translatable(ModLang.RANKING_YOU_OFF, Rankings.Board.byIndex(own).getDisplayName());
		if (you != null) {
			g.drawString(font, you, x, top, 0xFFD54F, false);
			top += ROW_H + 2;
		}
		List<ClientRankings.Row> rows = ClientRankings.list(shown);
		if (rows.isEmpty()) {
			g.drawString(font, Component.translatable(ModLang.RANKING_EMPTY), x, top + 4, 0x9E9E9E, false);
			maxScroll = 0;
			return;
		}
		int bottom = y + h;
		int visible = Math.max(1, (bottom - top) / ROW_H);
		maxScroll = Math.max(0, rows.size() - visible);
		scroll = Mth.clamp(scroll, 0, maxScroll);
		ClientRankings.Row hovered = null;
		g.enableScissor(x, top, x + w, bottom);
		for (int i = 0; i < visible + 1 && i + scroll < rows.size(); i++) {
			int index = i + scroll;
			ClientRankings.Row row = rows.get(index);
			int ry = top + i * ROW_H;
			boolean over = mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + ROW_H && mouseY < bottom;
			if (over) {
				g.fill(x, ry, x + w, ry + ROW_H, 0x30FFFFFF);
				hovered = row;
			}
			Component realm = PlayerCultivation.rankName(CultivatorNpc.realmOf(row.rank()), CultivatorNpc.stageOf(row.rank()));
			int realmWidth = font.width(realm);
			String name = (index + 1) + ". " + row.name();
			int room = w - realmWidth - 10;
			if (font.width(name) > room) name = font.plainSubstrByWidth(name, room - font.width("...")) + "...";
			g.drawString(font, name, x + 2, ry + 2, row.player() ? 0xFFD54F : index < 10 ? 0xF0E6D0 : 0xC8C0B0, false);
			g.drawString(font, realm, x + w - realmWidth - 2, ry + 2, 0x8FB8E8, false);
		}
		g.disableScissor();
		if (maxScroll > 0) {
			int track = bottom - top, knob = Math.max(10, track * visible / rows.size());
			int ky = top + (track - knob) * scroll / maxScroll;
			g.fill(x + w + 2, ky, x + w + 4, ky + knob, 0xFFB08A2E);
		}
		if (hovered != null) {
			List<Component> lines = new ArrayList<>();
			lines.add(Component.literal(hovered.name()));
			if (!hovered.sect().isEmpty()) {
				lines.add(hovered.title() >= 0 ? Component.translatable(ModLang.RANKING_ROW, NpcTitle.byIndex(hovered.title()).getDisplayName(), hovered.sect())
						: Component.literal(hovered.sect()));
			}
			Alignment.Faction[] factions = Alignment.Faction.values();
			if (hovered.faction() >= 0 && hovered.faction() < factions.length) lines.add(factions[hovered.faction()].getDisplayName());
			g.renderComponentTooltip(font, lines, mouseX, mouseY);
		}
	}

	/** A tab clicked: that list shown, from its top. */
	public boolean mouseClicked(double mouseX, double mouseY) {
		for (int[] tab : tabs) {
			if (mouseX >= tab[0] && mouseX < tab[0] + tab[2] && mouseY >= tab[1] && mouseY < tab[1] + tab[3]) {
				board = tab[4];
				scroll = 0;
				return true;
			}
		}
		return false;
	}

	public void scroll(double delta) {
		scroll = Mth.clamp(scroll - (int) Math.signum(delta) * 3, 0, maxScroll);
	}
}
