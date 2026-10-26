package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModDimensions;
import com.example.defyingtheheavens.ModLang;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * The thin bars of the top-left stack, under the portrait and its health and qi bars (VitalsHud: the qi pool's own bar is there
 * since 2026-10-22, the user's ask): each a label, then a thin lacquer bar trimmed in antique gold, with a scroll roller on the left
 * and a tapered tip on the right, notched at 25/50/75%, and the pool against its maximum beside it, a glint flowing along it while
 * it fills. Here, in a person's body (Possession), the soul's own pool, in violet, and a True Immortal's immortal essence
 * (ImmortalEssence), in pale gold; under them the ghostly qi pool, where there is one,
 * and the rest of the Netherworld's (see NetherworldHud), all drawn by the same {@link Pool}.
 */
public final class QiHud {
	static final int X = 4;
	/** From one bar's top to the next one's. */
	static final int ROW = 12;

	/** A soul's own pool while it is in a person's body (Possession): the soul's violet, lit from above. */
	private static final int[] OWN_ROWS = {0xFFE4CCFF, 0xFFB57BFF, 0xFF9457E0, 0xFF64339E};
	private static final Pool OWN = new Pool(OWN_ROWS, 0xFFF2E6FF, 0xE4D0FF);
	/** A True Immortal's immortal essence (ImmortalEssence): pale immortal gold, lit from above, brighter than the frame's. */
	private static final int[] ESSENCE_ROWS = {0xFFFFF6CC, 0xFFFFDE73, 0xFFF0B43A, 0xFFB57A14};
	private static final Pool ESSENCE = new Pool(ESSENCE_ROWS, 0xFFFFFBEA, 0xFFE9A8);

	public static void register() {
		HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || mc.player == null || mc.options.hideGui || mc.options.renderDebug || mc.player.isSpectator()) return;
			// In a person's body the qi bar above is the body's (its spells and abilities draw on it); the soul's own pool, which its
			// upkeep and its own tear come out of, under it.
			int y = top();
			if (ownShowing()) {
				OWN.draw(graphics, mc.font, y, barX(mc.font), Component.translatable(ownLabel()), ClientSoul.ownQi(), ClientSoul.ownMax(),
						ClientSoul.ownRising());
				y += ROW;
			} else {
				OWN.reset();
			}
			// A True Immortal's essence, its glint flowing while the Immortal Realm (or a small world) fills it.
			if (essenceShowing()) {
				boolean filling = ModDimensions.hasImmortalQi(mc.level.dimension());
				ESSENCE.draw(graphics, mc.font, y, barX(mc.font), Component.translatable(ModLang.HUD_ESSENCE_LABEL), ClientCultivationData.essence(),
						ClientCultivationData.essenceMax(), filling);
			} else {
				ESSENCE.reset();
			}
		});
	}

	/** Where the stack of thin bars starts: under the portrait and its bars (VitalsHud), else the corner. */
	static int top() {
		return VitalsHud.bottom();
	}

	/** True when the soul's own pool shows: in a person's body (Possession). */
	static boolean ownShowing() {
		return ClientSoul.isInside() && ClientSoul.ownMax() > 0;
	}

	/** True when the immortal essence's bar shows: a True Immortal's (ImmortalEssence). */
	static boolean essenceShowing() {
		return ClientCultivationData.essenceMax() > 0;
	}

	/** The thin bars it draws at the top of the stack: the soul's own pool while in a person's body, and a True Immortal's essence. */
	static int rows() {
		return (ownShowing() ? 1 : 0) + (essenceShowing() ? 1 : 0);
	}

	/** Under the portrait's block, the rows of the whole stack of thin bars (this one's and the Netherworld's), for what goes under them. */
	static int stackBottom() {
		return top() + (rows() + NetherworldHud.rows()) * ROW;
	}

	private static String ownLabel() {
		return ClientSoul.ownGhost() ? ModLang.HUD_SOUL_OWN_GHOST_QI_LABEL : ModLang.HUD_SOUL_OWN_QI_LABEL;
	}

	/**
	 * Where the thin bars start: past the widest label of the bars showing (this one's and the Netherworld's under it, see
	 * NetherworldHud), so they stand in one neat column whatever their labels say.
	 */
	static int barX(Font font) {
		int widest = ownShowing() ? font.width(Component.translatable(ownLabel())) : 0;
		if (essenceShowing()) widest = Math.max(widest, font.width(Component.translatable(ModLang.HUD_ESSENCE_LABEL)));
		widest = Math.max(widest, NetherworldHud.widestLabel(font));
		return X + widest + Pool.LABEL_GAP + 2;
	}

	/**
	 * One pool's bar: its label, then the lacquer bar in its gold frame, and the amount against the maximum beside it. Each pool
	 * keeps its own eased fill, so the server's steps become a smooth slide.
	 */
	static final class Pool {
		/** Height of the roller; the bar sits centred on it. */
		private static final int ROLLER_H = 10;
		/** Clear pixels between the label (shadow included) and the roller's knob, which reaches two pixels left of the bar. */
		static final int LABEL_GAP = 2;
		/** Frame included; the fill itself is {@link #INNER} wide (divisible by 4, so the quarter notches land on whole pixels). */
		private static final int BAR_W = 74;
		private static final int INNER = BAR_W - 2;
		/** Widths of the tip's rows, top to bottom, from the frame's top row to its bottom row. */
		private static final int[] TIP = {1, 2, 3, 3, 2, 1};
		/** From the bar's left edge to the amount: the bar, its tip and a gap. */
		private static final int TEXT_OFFSET = BAR_W + 3 + 4;

		private static final int GOLD = 0xFFB8860B;
		private static final int GOLD_DARK = 0xFF6E5214;
		private static final int NOTCH = 0xFF8C6A1E;
		private static final int LACQUER = 0xD0101420;
		private static final int TEXT = 0xE6D3A3;
		/** One glint crosses the filled part this often while the pool is filling, in milliseconds. */
		private static final long GLINT_PERIOD = 2400;
		/**
		 * How quickly the fill chases the synced value, per second. The server syncs a filling pool 5 times a second, so this
		 * turns those steps into a smooth slide.
		 */
		private static final double EASE = 12;

		private final int[] rows;
		private final int edge;
		private final int amountColor;
		private double shownRatio = -1;
		private long lastFrame;

		Pool(int[] rows, int edge, int amountColor) {
			this.rows = rows;
			this.edge = edge;
			this.amountColor = amountColor;
		}

		/**
		 * Draws the bar with its top at {@code y}, starting at {@code barX} (see {@link #barX}), and the amount against the
		 * maximum beside it; {@code mayGlint} false holds the glint back even while the pool is below full.
		 */
		void draw(GuiGraphics g, Font font, int y, int barX, Component label, double value, double max, boolean mayGlint) {
			Component amount = Component.translatable(ModLang.HUD_QI,
					Component.literal(whole(value)).withStyle(style -> style.withColor(amountColor)), whole(max));
			draw(g, font, y, barX, label, max <= 0 ? 0 : Math.min(1.0, value / max), amount, value < max && mayGlint);
		}

		/** Draws the bar filled to {@code ratio}, with {@code amount} (whatever it says) beside it. Returns where the amount ends. */
		int draw(GuiGraphics g, Font font, int y, int barX, Component label, double ratio, Component amount, boolean filling) {
			long now = Util.getMillis();
			if (shownRatio < 0) {
				shownRatio = ratio;
			} else {
				double seconds = Math.min(0.25, (now - lastFrame) / 1000.0);
				shownRatio += (ratio - shownRatio) * Math.min(1.0, seconds * EASE);
			}
			lastFrame = now;

			g.drawString(font, label, X, y + 1, TEXT);
			bar(g, barX, y, (int) Math.round(INNER * shownRatio), filling, now);
			g.drawString(font, amount, barX + TEXT_OFFSET, y + 1, TEXT);
			int end = barX + TEXT_OFFSET + font.width(amount);
			HudCorner.drew(y, y + ROLLER_H, end);
			return end;
		}

		/** Forgets the eased fill, so the bar starts from the true value the next time it shows. */
		void reset() {
			shownRatio = -1;
		}

		private void bar(GuiGraphics g, int barX, int y, int fill, boolean filling, long now) {
			int trackY = y + 3; // the fill runs in rows trackY .. trackY + 3, framed by a gold row above and below
			int left = barX + 1;
			int top = trackY - 1;
			int bottom = trackY + rows.length; // the frame's bottom row

			// Gold frame: lit top and sides, shaded bottom.
			g.fill(barX, top, barX + BAR_W, top + 1, GOLD);
			g.fill(barX, bottom, barX + BAR_W, bottom + 1, GOLD_DARK);
			g.fill(barX, trackY, left, bottom, GOLD);
			g.fill(barX + BAR_W - 1, trackY, barX + BAR_W, bottom, GOLD);
			g.fill(left, trackY, left + INNER, bottom, LACQUER);

			roller(g, barX - 1, y);

			// Tapered tip past the right end, lit above the middle and shaded below like the frame.
			for (int row = 0; row < TIP.length; row++) {
				int ty = top + row;
				g.fill(barX + BAR_W, ty, barX + BAR_W + TIP[row], ty + 1, row < TIP.length / 2 ? GOLD : GOLD_DARK);
			}

			if (fill > 0) {
				for (int row = 0; row < rows.length; row++) {
					g.fill(left, trackY + row, left + fill, trackY + row + 1, rows[row]);
				}
				if (filling) {
					// The leading edge of the gathering qi, and a glint flowing toward it.
					if (fill < INNER) g.fill(left + fill - 1, trackY, left + fill, bottom, edge);
					int center = left - 3 + (int) ((fill + 6) * (now % GLINT_PERIOD) / (double) GLINT_PERIOD);
					glint(g, trackY, center - 2, center + 3, 0x28FFFFFF, left, left + fill);
					glint(g, trackY, center - 1, center + 2, 0x50FFFFFF, left, left + fill);
				}
			}

			// Notches at the quarters, the half in brighter gold.
			for (int quarter = 1; quarter <= 3; quarter++) {
				int x = left + INNER * quarter / 4;
				g.fill(x, trackY, x + 1, bottom, quarter == 2 ? GOLD : NOTCH);
			}
		}

		/** A scroll roller two pixels wide at {@code x}, with a knob a pixel wider on each side at both ends. */
		private static void roller(GuiGraphics g, int x, int y) {
			g.fill(x, y + 1, x + 1, y + ROLLER_H - 1, GOLD);
			g.fill(x + 1, y + 1, x + 2, y + ROLLER_H - 1, GOLD_DARK);
			g.fill(x - 1, y, x + 3, y + 1, GOLD_DARK);
			g.fill(x - 1, y + ROLLER_H - 1, x + 3, y + ROLLER_H, GOLD_DARK);
		}

		/** Lightens the fill's rows between x1 and x2, clipped to the filled part [clipLeft, clipRight). */
		private void glint(GuiGraphics g, int trackY, int x1, int x2, int color, int clipLeft, int clipRight) {
			int from = Math.max(x1, clipLeft);
			int to = Math.min(x2, clipRight);
			if (to > from) g.fill(from, trackY, to, trackY + rows.length, color);
		}
	}

	/** Rounded down, so the bar never reads full before it is (the epsilon absorbs stage-multiplier float noise). */
	private static String whole(double v) {
		return String.format(Locale.ROOT, "%,d", (long) Math.floor(v + 1e-6));
	}

	private QiHud() {}
}
