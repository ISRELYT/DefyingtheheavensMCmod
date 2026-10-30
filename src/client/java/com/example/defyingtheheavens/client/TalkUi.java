package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.Regard;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The look shared by the talk wheel and the dialogue box, and by most of the mod's other screens (the bloodline, the obsession, the
 * mission board, the containers): since 2026-10-30 the cultivation menu's Hall (HallHud; the user's ask: "go through the other menus and
 * make them consistent"), ink panels in a thin dark-gold frame, cream words, gold for what matters; a name plate of the cultivator's name
 * in their title's colour over chips for title, sect and realm, and five pips of regard.
 */
final class TalkUi {
	static final int PANEL = HallHud.PANEL;
	static final int PANEL_LIT = 0xF019202A;
	static final int GOLD = 0xFFD4A63A;
	static final int GOLD_DIM = HallHud.FRAME;
	static final int INNER = 0xFF0B0E12;
	static final int RED_DIM = 0xFF6A3A34;
	static final int CREAM = 0xE6D3A3;
	static final int CREAM_BRIGHT = 0xFFF4C9;
	static final int MUTED = 0xB9B2A0;
	static final int DISABLED = 0x7A7466;
	static final int WARNING = 0xC46A5A;
	static final int GOLD_TEXT = 0xFFD54F;
	static final int SECT_TEXT = 0xB9D0E8;
	static final int SECT_BORDER = 0xFF7F9FBF;
	static final int PIP_ON = 0xFFF2C94C;
	static final int PIP_OFF = 0xFF2A3038;
	static final int PIP_BAD = 0xFFC4473A;

	/** A framed panel: ink within a thin frame, the usual gold one in the Hall's dark gold (another colour, a frame of that colour). */
	static void panel(GuiGraphics g, int x0, int y0, int x1, int y1, int border) {
		HallHud.panel(g, x0, y0, x1, y1, border == GOLD ? HallHud.FRAME : border, PANEL);
	}

	/** A small framed chip of text; returns its width. */
	static int chip(GuiGraphics g, Font font, Component text, int x, int y, int border, int color) {
		int w = font.width(text) + 8;
		HallHud.panel(g, x, y, x + w, y + 12, border, INNER);
		g.drawString(font, text, x + 4, y + 2, color, false);
		return w;
	}

	/** How wide the chips of {@code session} run, for centring. */
	static int chipsWidth(Font font, TalkSession session) {
		int w = font.width(session.title.getDisplayName()) + 8;
		if (!session.sectName.isEmpty()) w += 4 + font.width(session.sectName) + 8;
		return w + 4 + font.width(session.realmName()) + 8;
	}

	/** The name over its chips; {@code x} is the left edge. */
	static void namePlate(GuiGraphics g, Font font, TalkSession session, int x, int y) {
		g.drawString(font, session.name, x, y, session.title.getColor(), true);
		int cx = x;
		int cy = y + 12;
		cx += chip(g, font, session.title.getDisplayName(), cx, cy, 0xFF000000 | session.title.getColor(), session.title.getColor()) + 4;
		if (!session.sectName.isEmpty()) cx += chip(g, font, Component.literal(session.sectName), cx, cy, SECT_BORDER, SECT_TEXT) + 4;
		chip(g, font, session.realmName(), cx, cy, GOLD, GOLD_TEXT);
	}

	/** "Regard ■■□□□ Courteous"; returns the width drawn. */
	static int regard(GuiGraphics g, Font font, int regard, int x, int y) {
		Component label = Component.translatable(ModLang.TALK_REGARD);
		g.drawString(font, label, x, y, MUTED, false);
		int px = x + font.width(label) + 5;
		int pips = Regard.pips(regard);
		for (int i = 0; i < 5; i++) {
			int color = regard < 0 ? (i == 0 ? PIP_BAD : PIP_OFF) : i < pips ? PIP_ON : PIP_OFF;
			g.fill(px, y + 1, px + 6, y + 7, color);
			px += 8;
		}
		Component level = Component.translatable(ModLang.talkRegardLevel(Regard.levelId(regard)));
		g.drawString(font, level, px + 3, y, regard < 0 ? WARNING : MUTED, false);
		return px + 3 + font.width(level) - x;
	}

	private TalkUi() {}
}
