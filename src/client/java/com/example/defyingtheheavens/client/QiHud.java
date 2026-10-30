package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModDimensions;
import com.example.defyingtheheavens.ModLang;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The thin bars of the top-left stack, under the health and qi (VitalsHud), as the mockups' "G" draws them (in the artifact's pixels,
 * HallArt): each 334 by 22 in a thin dark-gold frame, its label and what it holds written across it ("Ghost Qi 23.9M / 270.4M"), a
 * glint flowing along it while it fills. Here, in a person's body (Possession), the soul's own pool, in violet, and a True Immortal's
 * immortal essence (ImmortalEssence), in pale gold, while it holds any or the land fills it (an empty one out in the lower worlds says
 * nothing); under them the ghostly qi pool, where there is one, and the rest of the Netherworld's (see NetherworldHud), all drawn by the
 * same {@link Pool}.
 */
public final class QiHud {
	/** From one thin bar's top to the next one's, in the artifact's pixels. */
	static final int ROW = 26;

	private static final Pool OWN = new Pool(HallHud.SOUL, HallHud.SOUL_LIGHT);
	private static final Pool ESSENCE = new Pool(HallHud.ESSENCE, HallHud.ESSENCE_LIGHT);

	public static void register() {
		HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || mc.player == null || mc.options.hideGui || mc.options.renderDebug || mc.player.isSpectator()) return;
			HallArt.beginHud(graphics);
			try {
				// In a person's body the qi bar above is the body's (its spells and abilities draw on it); the soul's own pool, which its
				// upkeep and its own tear come out of, under it.
				float y = top();
				if (ownShowing()) {
					OWN.draw(graphics, y, Component.translatable(ownLabel()), ClientSoul.ownQi(), ClientSoul.ownMax(), ClientSoul.ownRising());
					y += ROW;
				} else {
					OWN.reset();
				}
				// A True Immortal's essence, its glint flowing while the Immortal Realm (or a small world) fills it.
				if (essenceShowing()) {
					ESSENCE.draw(graphics, y, Component.translatable(ModLang.HUD_ESSENCE_LABEL), ClientCultivationData.essence(),
							ClientCultivationData.essenceMax(), essenceFilling());
				} else {
					ESSENCE.reset();
				}
			} finally {
				HallArt.end(graphics);
			}
		});
	}

	/** Where the stack of thin bars starts, in the artifact's pixels: under the health and qi (VitalsHud), else the corner. */
	static float top() {
		return VitalsHud.bottom();
	}

	/** True when the soul's own pool shows: in a person's body (Possession). */
	static boolean ownShowing() {
		return ClientSoul.isInside() && ClientSoul.ownMax() > 0;
	}

	/**
	 * True when the immortal essence's bar shows: a True Immortal's (ImmortalEssence), while it holds any or the land fills it; empty out
	 * in the lower worlds it would only be a bar of nothing.
	 */
	static boolean essenceShowing() {
		return ClientCultivationData.essenceMax() > 0 && (ClientCultivationData.essence() >= 1 || essenceFilling());
	}

	private static boolean essenceFilling() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && ModDimensions.hasImmortalQi(mc.level.dimension());
	}

	/** The thin bars it draws at the top of the stack: the soul's own pool while in a person's body, and a True Immortal's essence. */
	static int rows() {
		return (ownShowing() ? 1 : 0) + (essenceShowing() ? 1 : 0);
	}

	/** Under the health and qi, where the whole stack of thin bars ends (this one's and the Netherworld's), in the artifact's pixels. */
	static float stackBottom() {
		return top() + (rows() + NetherworldHud.rows()) * ROW;
	}

	private static String ownLabel() {
		return ClientSoul.ownGhost() ? ModLang.HUD_SOUL_OWN_GHOST_QI_LABEL : ModLang.HUD_SOUL_OWN_QI_LABEL;
	}

	/**
	 * One thin bar (the mockups' pools): 334 by 22, a border of 2, its label and what it holds written across it. Each keeps its own eased
	 * fill, so the server's steps become a smooth slide. Drawn in the artifact's pixels (HallArt#beginHud).
	 */
	static final class Pool {
		static final int H = 22, BORDER = 2;
		/** One glint crosses the filled part this often while the pool is filling, in milliseconds. */
		private static final long GLINT_PERIOD = 2400;
		/**
		 * How quickly the fill chases the synced value, per second. The server syncs a filling pool 5 times a second, so this turns
		 * those steps into a smooth slide.
		 */
		private static final double EASE = 12;

		private final int fill;
		private final int light;
		private double shownRatio = -1;
		private long lastFrame;

		Pool(int fill, int light) {
			this.fill = fill;
			this.light = light;
		}

		/** Draws the bar with its top at {@code y}, "Label value / max" across it; {@code mayGlint} false holds the glint back. */
		void draw(GuiGraphics g, float y, Component label, double value, double max, boolean mayGlint) {
			String text = label.getString() + " " + HallHud.compact(value) + " / " + HallHud.compact(max);
			draw(g, y, max <= 0 ? 0 : Math.min(1.0, value / max), text, value < max && mayGlint);
		}

		/** Draws the bar filled to {@code ratio}, with {@code text} (whatever it says) across it. */
		void draw(GuiGraphics g, float y, double ratio, String text, boolean filling) {
			long now = Util.getMillis();
			if (shownRatio < 0) {
				shownRatio = ratio;
			} else {
				double seconds = Math.min(0.25, (now - lastFrame) / 1000.0);
				shownRatio += (ratio - shownRatio) * Math.min(1.0, seconds * EASE);
			}
			lastFrame = now;

			float x = VitalsHud.X, w = VitalsHud.W, left = x + BORDER, top = y + BORDER, bottom = y + H - BORDER;
			HallArt.bar(g, x, y, w, H, BORDER, shownRatio, fill, light, 0);
			float f = HallArt.filled(w - 2 * BORDER, shownRatio);
			if (f > 0 && filling) {
				float centre = left - 9 + (f + 18) * (now % GLINT_PERIOD) / (float) GLINT_PERIOD;
				glint(g, top, bottom, centre - 7, centre + 8, 0x28FFFFFF, left, left + f);
				glint(g, top, bottom, centre - 3, centre + 4, 0x40FFFFFF, left, left + f);
			}
			HallArt.textCentred(g, text, x + w / 2, top, 15, 18, HallArt.WHITE, true);
			HallArt.corner(y, y + H, x + w);
		}

		/** Forgets the eased fill, so the bar starts from the true value the next time it shows. */
		void reset() {
			shownRatio = -1;
		}

		private static void glint(GuiGraphics g, float top, float bottom, float x1, float x2, int color, float clipLeft, float clipRight) {
			float from = Math.max(x1, clipLeft), to = Math.min(x2, clipRight);
			if (to > from) HallArt.rect(g, from, top, to, bottom, color);
		}
	}

	private QiHud() {}
}
