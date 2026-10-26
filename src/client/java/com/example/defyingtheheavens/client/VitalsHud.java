package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.TwoPaths;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/**
 * The top-left corner (the user's asks, 2026-10-22, after the realms after Four Axis gave a body thousands of hearts, to a picture of
 * theirs in the mod's own style): a portrait of the player in a lacquer frame trimmed in antique gold, their name small above it, and
 * beside it two thick bars, the **health** and the **qi**, each in the Qi bar's gold frame with its scroll roller and tapered tip,
 * what it holds against its maximum written across it ("1,407/1,407"). The health crimson, lit from above, a pale trail draining
 * after a blow, absorption a band of gold along its top (its amount after the health); the qi the mod's blue, a glint flowing
 * along it while it fills. Under them the stack of thinner bars goes on (QiHud: a soul's own pool; NetherworldHud). Nothing of
 * hunger, air or armour up here (the user's word: those stay at the bottom, vanilla's, its hearts held to a row of ten:
 * mixin/GuiHeartsMixin). The health shows when vanilla's hearts do (not in creative or spectator); the qi once the meridians open.
 */
public final class VitalsHud {
	static final int X = 4;
	/** The name's line (half-size text), and the portrait and bars under it. */
	private static final int NAME_Y = 2, TOP = 8;
	/** A bar's height, frame included; the gap between the two; the room under the block before the stack goes on. */
	static final int BAR_H = 12, BAR_GAP = 3, GAP = 3;
	/** The portrait, frame included: as tall as the two bars. */
	private static final int PORTRAIT = 2 * BAR_H + BAR_GAP;
	/** The bars start this far right of the portrait, and are never narrower than this (frame included). */
	private static final int BARS_X = X + PORTRAIT + 4, MIN_W = 100;

	private static final int GOLD = 0xFFB8860B;
	private static final int GOLD_DARK = 0xFF6E5214;
	private static final int NOTCH = 0x608C6A1E;
	private static final int LACQUER = 0xD0101420;
	private static final int TEXT = 0xFFFFFF;
	private static final int NAME = 0xE6D3A3;
	private static final int TEXT_ABSORPTION = 0xFFE08A;
	/** Widths of a tip's rows, top to bottom, from the frame's top row to its bottom row. */
	private static final int[] TIP = {1, 2, 3, 4, 5, 6, 6, 5, 4, 3, 2, 1};
	/** The health's crimson and the qi's blue, lit from above, row by row (a bar's ten inner rows). */
	private static final int[] HEALTH_ROWS = {0xFFFF8A80, 0xFFEA5E5E, 0xFFD94848, 0xFFCC3A3A, 0xFFC03131, 0xFFB42A2A, 0xFFA72323, 0xFF991E1E,
			0xFF891919, 0xFF701313};
	private static final int[] QI_ROWS = {0xFF9ADCFF, 0xFF74C8F7, 0xFF58B7EE, 0xFF4AAEEA, 0xFF3FA0DE, 0xFF3593D3, 0xFF2D88CC, 0xFF2779B8,
			0xFF2169A3, 0xFF1C5C93};
	/** The trail a blow leaves, draining after the health; the band of absorption; the leading edge of gathering qi. */
	private static final int TRAIL = 0xFFF2B5A8, ABSORPTION = 0xFFFFD54F, QI_EDGE = 0xFFCFF0FF;
	/** How quickly a fill chases its value, and the trail the health, per second; one glint along the qi this often (ms). */
	private static final double EASE = 14, TRAIL_EASE = 2.5;
	private static final long GLINT_PERIOD = 2400;

	private static double health = -1, trail = -1, qi = -1;
	private static long lastFrame;

	public static void register() {
		HudRenderCallback.EVENT.register((graphics, tickDelta) -> render(graphics));
	}

	/** Whether the health bar is drawn now (as vanilla's hearts are). */
	static boolean healthShowing() {
		Minecraft mc = Minecraft.getInstance();
		return visible() && mc.gameMode != null && mc.gameMode.canHurtPlayer();
	}

	/** Whether the qi bar is drawn now: once the meridians open (the pool there is). */
	static boolean qiShowing() {
		return visible() && !ClientCultivationData.get().isMortal();
	}

	/** Whether anything of it is drawn: a player in the world, the HUD up, no debug screen, not a spectator. */
	private static boolean visible() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && mc.player != null && !mc.options.hideGui && !mc.options.renderDebug && !mc.player.isSpectator()
				&& mc.getCameraEntity() instanceof Player;
	}

	/** Where the stack under it goes on (QiHud#top): under the block while it shows, else the corner. */
	static int bottom() {
		return healthShowing() || qiShowing() ? TOP + PORTRAIT + GAP : 4;
	}

	/** What one heart of vanilla's ten stands for in {@code player}'s health (GuiHeartsMixin): a tenth of a body past twenty, else 2. */
	public static float heartScale(Player player) {
		float max = player.getMaxHealth();
		return max > 20 ? 20 / max : 1;
	}

	private static void render(GuiGraphics g) {
		boolean showHealth = healthShowing(), showQi = qiShowing();
		if (!showHealth) health = trail = -1;
		if (!showQi) qi = -1;
		if (!showHealth && !showQi) return;
		Minecraft mc = Minecraft.getInstance();
		Player player = (Player) mc.getCameraEntity();
		PlayerCultivation c = ClientCultivationData.get();
		Font font = mc.font;

		float max = Math.max(1, player.getMaxHealth());
		float now = Mth.clamp(player.getHealth(), 0, max);
		float absorption = player.getAbsorptionAmount();
		String healthText = whole(Math.ceil(now)) + "/" + whole(Math.ceil(max)); // both up, so a whole body never reads over its max
		String extra = absorption > 0 ? " +" + whole(Math.ceil(absorption)) : "";
		String qiText = whole(c.getQi()) + "/" + whole(c.maxQi());
		int w = MIN_W;
		if (showHealth) w = Math.max(w, font.width(healthText) + font.width(extra) + 16);
		if (showQi) w = Math.max(w, font.width(qiText) + 16);

		long time = Util.getMillis();
		double seconds = lastFrame == 0 ? 0 : Math.min(0.25, (time - lastFrame) / 1000.0);
		lastFrame = time;

		portrait(g, font, player);
		HudCorner.drew(NAME_Y, TOP + PORTRAIT, BARS_X + w + TIP[TIP.length / 2]); // its name, the portrait and the bars to their tips
		int y = TOP;
		if (showHealth) {
			double ratio = now / max;
			if (health < 0) {
				health = trail = ratio;
			} else {
				health += (ratio - health) * Math.min(1.0, seconds * EASE);
				trail = trail < health ? health : trail + (health - trail) * Math.min(1.0, seconds * TRAIL_EASE);
			}
			frame(g, y, w);
			int inner = w - 2;
			int fill = (int) Math.round(inner * Math.min(1, health));
			int trailTo = (int) Math.round(inner * Math.min(1, trail));
			if (trailTo > fill) g.fill(BARS_X + 1 + fill, y + 1, BARS_X + 1 + trailTo, y + BAR_H - 1, TRAIL);
			fillRows(g, y, fill, HEALTH_ROWS);
			if (absorption > 0) {
				int band = (int) Math.round(inner * Math.min(1, absorption / max));
				g.fill(BARS_X + 1, y + 1, BARS_X + 1 + Math.max(1, band), y + 3, ABSORPTION);
			}
			notches(g, y, w);
			int textW = font.width(healthText) + font.width(extra);
			int textX = BARS_X + (w - textW) / 2;
			g.drawString(font, healthText, textX, y + 2, TEXT, true);
			if (!extra.isEmpty()) g.drawString(font, extra, textX + font.width(healthText), y + 2, TEXT_ABSORPTION, true);
			y += BAR_H + BAR_GAP;
		}
		if (showQi) {
			double ratio = c.maxQi() <= 0 ? 0 : Math.min(1.0, c.getQi() / c.maxQi());
			qi = qi < 0 ? ratio : qi + (ratio - qi) * Math.min(1.0, seconds * EASE);
			// The gathering glint shows while the pool is rising: not in a Qi Flight that costs more than gathering brings in (a
			// ghostly form's flight costs nothing: TwoPaths#fliesFreely).
			boolean draining = player.getAbilities().flying && !player.isCreative() && c.getEffectiveRealm().canFlyOnQi()
					&& !c.isQiFlightSustained() && !TwoPaths.fliesFreely(c);
			frame(g, y, w);
			int inner = w - 2;
			int fill = (int) Math.round(inner * Math.min(1, qi));
			fillRows(g, y, fill, QI_ROWS);
			if (fill > 0 && c.getQi() < c.maxQi() && !draining) {
				// The leading edge of the gathering qi, and a glint flowing toward it.
				int left = BARS_X + 1;
				if (fill < inner) g.fill(left + fill - 1, y + 1, left + fill, y + BAR_H - 1, QI_EDGE);
				int center = left - 3 + (int) ((fill + 6) * (time % GLINT_PERIOD) / (double) GLINT_PERIOD);
				glint(g, y, center - 3, center + 4, 0x28FFFFFF, left, left + fill);
				glint(g, y, center - 1, center + 2, 0x50FFFFFF, left, left + fill);
			}
			notches(g, y, w);
			g.drawString(font, qiText, BARS_X + (w - font.width(qiText)) / 2, y + 2, TEXT, true);
		}
	}

	/** The player in a lacquer frame trimmed in gold, turned a little toward the bars, the name small above it. */
	private static void portrait(GuiGraphics g, Font font, Player player) {
		int x0 = X, y0 = TOP, x1 = X + PORTRAIT, y1 = TOP + PORTRAIT;
		g.fill(x0, y0, x1, y1, GOLD);
		g.fill(x0 + 1, y1 - 1, x1, y1, GOLD_DARK);
		g.fill(x1 - 1, y0 + 1, x1, y1, GOLD_DARK);
		g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, LACQUER);
		// The frame's corners notched in dark gold, as the mod's panels are.
		g.fill(x0, y0, x0 + 2, y0 + 1, GOLD_DARK);
		g.fill(x0, y0, x0 + 1, y0 + 2, GOLD_DARK);
		g.enableScissor(x0 + 1, y0 + 1, x1 - 1, y1 - 1);
		// The whole body, feet a little over the frame's floor, looking a little toward the bars.
		InventoryScreen.renderEntityInInventoryFollowsMouse(g, (x0 + x1) / 2, y1 - 2, 12, -18, -6, player);
		g.disableScissor();
		g.pose().pushPose();
		g.pose().translate(X, NAME_Y, 0);
		g.pose().scale(0.5f, 0.5f, 1);
		g.drawString(font, player.getName(), 0, 0, NAME, true);
		g.pose().popPose();
	}

	/** A bar's gold frame (lit top and sides, shaded bottom), its lacquer, its roller and its tip, with its top at {@code y}, {@code w} wide. */
	private static void frame(GuiGraphics g, int y, int w) {
		int bottom = y + BAR_H - 1;
		g.fill(BARS_X, y, BARS_X + w, y + 1, GOLD);
		g.fill(BARS_X, bottom, BARS_X + w, bottom + 1, GOLD_DARK);
		g.fill(BARS_X, y + 1, BARS_X + 1, bottom, GOLD);
		g.fill(BARS_X + w - 1, y + 1, BARS_X + w, bottom, GOLD);
		g.fill(BARS_X + 1, y + 1, BARS_X + w - 1, bottom, LACQUER);
		// The Qi bar's scroll roller (QiHud.Pool), as tall as this bar.
		int rx = BARS_X - 1;
		g.fill(rx, y + 1, rx + 1, y + BAR_H - 1, GOLD);
		g.fill(rx + 1, y + 1, rx + 2, y + BAR_H - 1, GOLD_DARK);
		g.fill(rx - 1, y, rx + 3, y + 1, GOLD_DARK);
		g.fill(rx - 1, y + BAR_H - 1, rx + 3, y + BAR_H, GOLD_DARK);
		for (int row = 0; row < TIP.length; row++) {
			g.fill(BARS_X + w, y + row, BARS_X + w + TIP[row], y + row + 1, row < TIP.length / 2 ? GOLD : GOLD_DARK);
		}
	}

	private static void fillRows(GuiGraphics g, int y, int fill, int[] rows) {
		if (fill <= 0) return;
		for (int row = 0; row < rows.length; row++) g.fill(BARS_X + 1, y + 1 + row, BARS_X + 1 + fill, y + 2 + row, rows[row]);
	}

	/** Notches at the quarters, faint, under what the bar says. */
	private static void notches(GuiGraphics g, int y, int w) {
		int inner = w - 2;
		for (int quarter = 1; quarter <= 3; quarter++) {
			int x = BARS_X + 1 + inner * quarter / 4;
			g.fill(x, y + 1, x + 1, y + BAR_H - 1, NOTCH);
		}
	}

	/** Lightens the fill's rows between x1 and x2, clipped to the filled part [clipLeft, clipRight). */
	private static void glint(GuiGraphics g, int y, int x1, int x2, int color, int clipLeft, int clipRight) {
		int from = Math.max(x1, clipLeft), to = Math.min(x2, clipRight);
		if (to > from) g.fill(from, y + 1, to, y + BAR_H - 1, color);
	}

	/** Rounded down, so a bar never reads full before it is (the epsilon absorbs stage-multiplier float noise). */
	private static String whole(double v) {
		return String.format(Locale.ROOT, "%,d", (long) Math.floor(Math.max(0, v) + 1e-6));
	}

	private VitalsHud() {}
}
