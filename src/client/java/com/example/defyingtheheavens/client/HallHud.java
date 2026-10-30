package com.example.defyingtheheavens.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * The Hall's look on the HUD (the user's pick, 2026-10-30: the Cultivation Hall's menu style, "the hall panel but remove the black boxes
 * behind the UI and shrink it slightly"): no panel behind anything, flat bars in a thin dark-gold frame on a see-through track, a lighter
 * row along a fill's top, what a bar holds written across it in white outlined in black, diamonds for markers, and numbers kept short
 * (5.80M) so nothing grows with a realm's might. The Hall's screens share its ground and rows from here too.
 */
public final class HallHud {
	/** The stack's left edge and its bars' width: fixed, whatever the numbers. */
	public static final int X = 4, W = 111;

	public static final int FRAME = 0xFF6E5214;
	public static final int TRACK = 0x990B0E12;
	public static final int TEXT = 0xFFE6D3A3;
	public static final int GOLD = 0xFFFFD54F;
	public static final int CAPTION = 0xFFC9C2B0;
	public static final int OUTLINE = 0xFF000000;

	/** Fills, each with the lighter row along its top: health, qi, the soul's violet, the ghostly qi, immortal essence, yin. */
	public static final int HEALTH = 0xFFC03131, HEALTH_LIGHT = 0xFFE05A5A;
	public static final int QI = 0xFF3593D3, QI_LIGHT = 0xFF74C8F7;
	public static final int SOUL = 0xFF9457E0, SOUL_LIGHT = 0xFFB57BFF;
	public static final int GHOST = 0xFF1F8F86, GHOST_LIGHT = 0xFF2FB5B0;
	public static final int ESSENCE = 0xFFD9A62B, ESSENCE_LIGHT = 0xFFF0C75A;
	public static final int YIN = 0xFF2A6488, YIN_LIGHT = 0xFF3E8AA6;
	public static final int PRESSURE = 0xFFFF6A6A, PRESSURE_TEXT = 0xFFFF8A8A;

	/**
	 * A flat bar at ({@code x}, {@code y}), {@code w} by {@code h} with its frame, filled to {@code ratio} in {@code fill} (its top row
	 * {@code light}), {@code text} across its middle.
	 */
	public static void bar(GuiGraphics g, Font font, int x, int y, int w, int h, double ratio, int fill, int light, String text) {
		frame(g, x, y, w, h);
		int f = filled(w, ratio);
		if (f > 0) {
			g.fill(x + 1, y + 1, x + 1 + f, y + h - 1, fill);
			g.fill(x + 1, y + 1, x + 1 + f, y + 2, light);
		}
		if (text != null && !text.isEmpty()) outlined(g, font, text, x + (w - font.width(text)) / 2, y + (h - 7) / 2, 0xFFFFFFFF);
	}

	/** The thin frame and the see-through track inside it (four edges, so the track shows no frame through it). */
	public static void frame(GuiGraphics g, int x, int y, int w, int h) {
		g.fill(x, y, x + w, y + 1, FRAME);
		g.fill(x, y + h - 1, x + w, y + h, FRAME);
		g.fill(x, y + 1, x + 1, y + h - 1, FRAME);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, FRAME);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, TRACK);
	}

	/** How many of a bar's inner columns {@code ratio} fills. */
	public static int filled(int w, double ratio) {
		return (int) Math.round((w - 2) * Math.max(0, Math.min(1, ratio)));
	}

	/** {@code text} in {@code color}, outlined in black a pixel all round. */
	public static void outlined(GuiGraphics g, Font font, String text, int x, int y, int color) {
		g.drawString(font, text, x - 1, y, OUTLINE, false);
		g.drawString(font, text, x + 1, y, OUTLINE, false);
		g.drawString(font, text, x, y - 1, OUTLINE, false);
		g.drawString(font, text, x, y + 1, OUTLINE, false);
		g.drawString(font, text, x, y, color, false);
	}

	/** {@code text} at half size, outlined (a caption: the name over the bars, a slot's word). */
	public static void caption(GuiGraphics g, Font font, String text, int x, int y, int color) {
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		g.pose().scale(0.5f, 0.5f, 1);
		outlined(g, font, text, 0, 0, color);
		g.pose().popPose();
	}

	/**
	 * A diamond {@code size} across, its top at ({@code x} + size / 2, {@code y}): a square turned 45 degrees (HallArt#diamond), so its
	 * edges stay straight where a framed screen draws it large, as the mockups' are.
	 */
	public static void diamond(GuiGraphics g, int x, int y, int size, int color) {
		float side = size / (float) Math.sqrt(2);
		HallArt.diamond(g, x + (size - side) / 2, y + (size - side) / 2, side, color);
	}

	/** The first of {@code options} that fits in {@code width} (the last, if none does). */
	public static String fit(Font font, int width, String... options) {
		for (String option : options) if (font.width(option) <= width) return option;
		return options[options.length - 1];
	}

	/** "Label cur / max", shortened to fit a bar {@code w} wide: "Label cur", then "cur / max", then "cur". */
	public static String amount(Font font, int w, Component label, double value, double max) {
		String cur = compact(value), whole = compact(max), name = label.getString();
		return fit(font, w - 4, name + " " + cur + " / " + whole, name + " " + cur, cur + " / " + whole, cur);
	}

	// --- The Hall's screens: the cultivation menu's (CultivationScreen) and the Codex's (CodexScreen) ---

	/**
	 * The screen's ink (whole, so nothing of the HUD shows through a Hall screen), the column down its left, a panel's ground, the row
	 * open (jade) and a row pointed at.
	 */
	public static final int INK = 0xFF0C0F13, NAV_INK = 0xFF11161C, PANEL = 0xF011161C, ROW_OPEN = 0xFF1E3A32, ROW_LIT = 0xFF19202A;
	/** A card's ground (a path's, a button's), and the rule between the rows of a list. */
	public static final int CARD = 0xFF0D1116, RULE = 0xFF3A3326;
	/** A frame pointed at, one switched on (jade), and one that can't be pressed. */
	public static final int FRAME_LIT = 0xFFB8860B, FRAME_ON = 0xFF3E8C72, FRAME_OFF = 0xFF3A3326;
	/** Words muted, and words of what can't be had or pressed. */
	public static final int MUTED = 0xFFB9B2A0, FADED = 0xFF6C665A;

	/** A panel from (x0, y0) to (x1, y1): its ground in a thin frame, drawn edge by edge so the frame never shows through a see-through ground. */
	public static void panel(GuiGraphics g, int x0, int y0, int x1, int y1, int frame, int ground) {
		g.fill(x0, y0, x1, y0 + 1, frame);
		g.fill(x0, y1 - 1, x1, y1, frame);
		g.fill(x0, y0 + 1, x0 + 1, y1 - 1, frame);
		g.fill(x1 - 1, y0 + 1, x1, y1 - 1, frame);
		g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, ground);
	}

	/** A heading in the Hall: a gold diamond, the words in gold after it, and a dark-gold rule under them {@code w} wide. */
	public static void heading(GuiGraphics g, Font font, Component text, int x, int y, int w) {
		diamond(g, x, y + 1, 5, GOLD);
		g.drawString(font, text, x + 9, y, GOLD, false);
		g.fill(x, y + 12, x + w, y + 13, FRAME);
	}

	/**
	 * A button in the Hall (HallButton, and the screens' own): a card in a thin dark-gold frame, gold while pointed at, faint while it
	 * can't be pressed; its words in the middle, cut short with "..." where they won't fit.
	 */
	public static void button(GuiGraphics g, Font font, int x, int y, int w, int h, Component text, boolean active, boolean lit) {
		panel(g, x, y, x + w, y + h, !active ? FRAME_OFF : lit ? FRAME_LIT : FRAME, active && lit ? ROW_LIT : CARD);
		int color = !active ? FADED : lit ? GOLD : TEXT;
		String words = text.getString();
		if (font.width(words) > w - 6) words = font.plainSubstrByWidth(words, Math.max(0, w - 6 - font.width("..."))) + "...";
		g.drawString(font, words, x + (w - font.width(words) + 1) / 2, y + (h - 7) / 2, color, false);
	}

	/** The Hall's ground: the screen in ink, and two ranges of far mountains along its foot, the nearer the darker, in columns two pixels wide. */
	public static void backdrop(GuiGraphics g, int width, int height) {
		g.fill(0, 0, width, height, INK);
		for (int x = 0; x < width; x += 2) {
			double far = Math.sin(x * 0.021) * 9 + Math.sin(x * 0.057 + 1.3) * 5 + Math.sin(x * 0.013 + 2.1) * 7;
			double near = Math.sin(x * 0.034 + 0.7) * 6 + Math.sin(x * 0.081 + 2.9) * 3;
			g.fill(x, height - 52 + (int) far, x + 2, height, 0xFF11161C);
			g.fill(x, height - 24 + (int) near, x + 2, height, 0xFF0E1318);
		}
	}

	/** A row of a column to choose from: jade under the one open, a gold diamond at its left; lit while pointed at. */
	public static void row(GuiGraphics g, int x, int y, int w, int h, boolean open, boolean lit) {
		if (open) g.fill(x, y, x + w, y + h, ROW_OPEN);
		else if (lit) g.fill(x, y, x + w, y + h, ROW_LIT);
		if (open) diamond(g, x + 3, y + (h - 5) / 2, 5, GOLD);
	}

	/**
	 * A number kept short: as it is under ten thousand (with thousands marked), else three figures and a letter (12.3k, 160M, 5.80B),
	 * rounded down so a bar never reads fuller than it is.
	 */
	public static String compact(double v) {
		v = Math.max(0, v);
		if (v < 10_000) return String.format(Locale.ROOT, "%,d", (long) Math.floor(v + 1e-6));
		String[] units = {"k", "M", "B", "T", "Qa", "Qi"};
		double scaled = v;
		int unit = -1;
		while (scaled >= 1000 && unit < units.length - 1) {
			scaled /= 1000;
			unit++;
		}
		String figures;
		if (scaled >= 100) figures = String.valueOf((long) Math.floor(scaled + 1e-9));
		else if (scaled >= 10) figures = String.format(Locale.ROOT, "%.1f", Math.floor(scaled * 10 + 1e-9) / 10);
		else figures = String.format(Locale.ROOT, "%.2f", Math.floor(scaled * 100 + 1e-9) / 100);
		return figures + units[unit];
	}

	private HallHud() {}
}
