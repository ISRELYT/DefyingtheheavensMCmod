package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.PlayerCultivation;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * The Qi bar in the top-left corner: a "Qi" label, then a thin lacquer bar trimmed in antique gold, with a scroll roller on
 * the left and a tapered tip on the right, notched at 25/50/75%, and the qi pool against its maximum beside it. While the
 * pool fills, a glint flows along the qi.
 */
public final class QiHud {
	private static final int X = 4;
	private static final int Y = 4;
	/** Height of the roller; the bar sits centred on it. */
	private static final int ROLLER_H = 10;
	/** Clear pixels between the label (shadow included) and the roller's knob, which reaches two pixels left of the bar. */
	private static final int LABEL_GAP = 2;
	/** Frame included; the qi itself is {@link #INNER} wide (divisible by 4, so the quarter notches land on whole pixels). */
	private static final int BAR_W = 74;
	private static final int INNER = BAR_W - 2;
	/** The qi runs in rows TRACK_Y .. TRACK_Y + 3, framed by a gold row above and below. */
	private static final int TRACK_Y = Y + 3;
	/** Widths of the tip's rows, top to bottom, from the frame's top row to its bottom row. */
	private static final int[] TIP = {1, 2, 3, 3, 2, 1};
	/** From the bar's left edge to the amount: the bar, its tip and a gap. */
	private static final int TEXT_OFFSET = BAR_W + 3 + 4;

	private static final int GOLD = 0xFFB8860B;
	private static final int GOLD_DARK = 0xFF6E5214;
	private static final int NOTCH = 0xFF8C6A1E;
	private static final int LACQUER = 0xD0101420;
	/** The qi, lit from above: highlight, body, body, shade. */
	private static final int[] QI_ROWS = {0xFF9ADCFF, 0xFF4AAEEA, 0xFF2D88CC, 0xFF1C5C93};
	private static final int QI_EDGE = 0xFFCFF0FF;
	private static final int TEXT = 0xE6D3A3;
	private static final int TEXT_QI = 0xCDEBFF;
	/** One glint crosses the filled part this often while the pool is filling, in milliseconds. */
	private static final long GLINT_PERIOD = 2400;
	/**
	 * How quickly the fill chases the synced value, per second. The server syncs a filling pool 5 times a second, so this
	 * turns those steps into a smooth slide.
	 */
	private static final double EASE = 12;

	private static double shownRatio = -1;
	private static long lastFrame;

	public static void register() {
		HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || mc.player == null || mc.options.hideGui || mc.options.renderDebug || mc.player.isSpectator()) return;

			PlayerCultivation c = ClientCultivationData.get();
			if (c.isMortal()) return; // no qi to show until the meridians open
			double max = c.maxQi();
			double qi = c.getQi();
			double ratio = max <= 0 ? 0 : Math.min(1.0, qi / max);
			long now = Util.getMillis();
			if (shownRatio < 0) {
				shownRatio = ratio;
			} else {
				double seconds = Math.min(0.25, (now - lastFrame) / 1000.0);
				shownRatio += (ratio - shownRatio) * Math.min(1.0, seconds * EASE);
			}
			lastFrame = now;

			// The label sets where the bar starts, so a longer translation pushes it right instead of overlapping.
			Component label = Component.translatable(ModLang.HUD_QI_LABEL);
			graphics.drawString(mc.font, label, X, Y + 1, TEXT);
			int barX = X + mc.font.width(label) + LABEL_GAP + 2;
			// The gathering glint shows while the pool is rising: not in a Qi Flight that costs more than gathering brings in.
			boolean draining = mc.player.getAbilities().flying && !mc.player.isCreative() && c.getEffectiveRealm().canFlyOnQi()
					&& !c.isQiFlightSustained();
			bar(graphics, barX, (int) Math.round(INNER * shownRatio), qi < max && !draining, now);
			Component amount = Component.translatable(ModLang.HUD_QI,
					Component.literal(whole(qi)).withStyle(style -> style.withColor(TEXT_QI)), whole(max));
			graphics.drawString(mc.font, amount, barX + TEXT_OFFSET, Y + 1, TEXT);
		});
	}

	private static void bar(GuiGraphics g, int barX, int fill, boolean filling, long now) {
		int left = barX + 1;
		int top = TRACK_Y - 1;
		int bottom = TRACK_Y + QI_ROWS.length; // the frame's bottom row

		// Gold frame: lit top and sides, shaded bottom.
		g.fill(barX, top, barX + BAR_W, top + 1, GOLD);
		g.fill(barX, bottom, barX + BAR_W, bottom + 1, GOLD_DARK);
		g.fill(barX, TRACK_Y, left, bottom, GOLD);
		g.fill(barX + BAR_W - 1, TRACK_Y, barX + BAR_W, bottom, GOLD);
		g.fill(left, TRACK_Y, left + INNER, bottom, LACQUER);

		roller(g, barX - 1);

		// Tapered tip past the right end, lit above the middle and shaded below like the frame.
		for (int row = 0; row < TIP.length; row++) {
			int y = top + row;
			g.fill(barX + BAR_W, y, barX + BAR_W + TIP[row], y + 1, row < TIP.length / 2 ? GOLD : GOLD_DARK);
		}

		if (fill > 0) {
			for (int row = 0; row < QI_ROWS.length; row++) {
				g.fill(left, TRACK_Y + row, left + fill, TRACK_Y + row + 1, QI_ROWS[row]);
			}
			if (filling) {
				// The leading edge of the gathering qi, and a glint flowing toward it.
				if (fill < INNER) g.fill(left + fill - 1, TRACK_Y, left + fill, bottom, QI_EDGE);
				int center = left - 3 + (int) ((fill + 6) * (now % GLINT_PERIOD) / (double) GLINT_PERIOD);
				glint(g, center - 2, center + 3, 0x28FFFFFF, left, left + fill);
				glint(g, center - 1, center + 2, 0x50FFFFFF, left, left + fill);
			}
		}

		// Notches at the quarters, the half in brighter gold.
		for (int quarter = 1; quarter <= 3; quarter++) {
			int x = left + INNER * quarter / 4;
			g.fill(x, TRACK_Y, x + 1, bottom, quarter == 2 ? GOLD : NOTCH);
		}
	}

	/** A scroll roller two pixels wide at {@code x}, with a knob a pixel wider on each side at both ends. */
	private static void roller(GuiGraphics g, int x) {
		g.fill(x, Y + 1, x + 1, Y + ROLLER_H - 1, GOLD);
		g.fill(x + 1, Y + 1, x + 2, Y + ROLLER_H - 1, GOLD_DARK);
		g.fill(x - 1, Y, x + 3, Y + 1, GOLD_DARK);
		g.fill(x - 1, Y + ROLLER_H - 1, x + 3, Y + ROLLER_H, GOLD_DARK);
	}

	/** Lightens the qi rows between x1 and x2, clipped to the filled part [clipLeft, clipRight). */
	private static void glint(GuiGraphics g, int x1, int x2, int color, int clipLeft, int clipRight) {
		int from = Math.max(x1, clipLeft);
		int to = Math.min(x2, clipRight);
		if (to > from) g.fill(from, TRACK_Y, to, TRACK_Y + QI_ROWS.length, color);
	}

	/** Rounded down, so the bar never reads full before it is (the epsilon absorbs stage-multiplier float noise). */
	private static String whole(double v) {
		return String.format(Locale.ROOT, "%,d", (long) Math.floor(v + 1e-6));
	}

	private QiHud() {}
}
