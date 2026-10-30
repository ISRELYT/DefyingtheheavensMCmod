package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.client.mixin.FontManagerAccessor;
import com.example.defyingtheheavens.client.mixin.MinecraftFontManagerAccessor;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Pixelify Sans, the mockups' font (the user's pick, 2026-10-30; assets/defying-the-heavens/font/pixelify_sans.ttf, SIL Open Font
 * License beside it), as Minecraft draws it. Minecraft samples a TrueType font's glyphs pixel for pixel, so it is defined once for each
 * size it can stand on the screen (font/hall/n.json: rasterised at n window pixels to its em, 6 to 112) and drawn at the one its size
 * on the screen asks for: its letters then come out as smooth as a browser's. Each is 8 of its own units to the em (its definitions'
 * size, 9.6, is its ascent and descent, 1.2 em).
 */
public final class HallFonts {
	static final int MIN = 6, MAX = 112;
	/** The em in the font's units. */
	static final float EM = 8f;
	/**
	 * How far below where it is drawn the baseline stands, in the font's units: Minecraft draws a glyph 3 above its place (BakedGlyph),
	 * the definitions shift it 1 down (counted twice: once into the glyph's box, as stb_truetype's subpixel shift, and once into its
	 * bearing), and the ascent is 0.92 em.
	 */
	static final float BASELINE = -3f + 2 * 1f + 0.92f * EM;

	private static final ResourceLocation[] IDS = new ResourceLocation[MAX + 1];
	/** The framed screens' em on the screen, in window pixels (HallFrame), and their font. */
	private static int screenEm = 24;
	private static Font screenFont;

	/** The size whose em is {@code px} window pixels (the nearest there is). */
	static ResourceLocation id(float px) {
		int n = Mth.clamp(Math.round(px), MIN, MAX);
		ResourceLocation id = IDS[n];
		if (id == null) IDS[n] = id = new ResourceLocation(DefyingTheHeavens.MOD_ID, "hall/" + n);
		return id;
	}

	/**
	 * The framed screens' font (HallFrame): Pixelify Sans in the place of Minecraft's own, its em 8 of the frame's pixels (near Minecraft's
	 * own letters, which the screens were laid out for), at the size the frame stands at on the screen now ({@link #update}).
	 */
	public static Font screenFont() {
		if (screenFont == null) {
			FontManagerAccessor fonts = (FontManagerAccessor) ((MinecraftFontManagerAccessor) Minecraft.getInstance()).dth$fontManager();
			screenFont = new Font(id -> {
				FontSet set = fonts.dth$fontSets().get(id.equals(Style.DEFAULT_FONT) ? id(screenEm) : id);
				return set != null ? set : fonts.dth$missingFontSet();
			}, false);
		}
		return screenFont;
	}

	/** The font a framed screen's words are in now, else Minecraft's own (HallButton, which draws in either). */
	static Font current() {
		Minecraft mc = Minecraft.getInstance();
		return mc.screen instanceof HallFrame.Framed ? screenFont() : mc.font;
	}

	/** The framed screens' em on the screen, as the window stands now. */
	static void update(Window window) {
		screenEm = Math.round(EM * (float) HallFrame.scale(window));
	}

	private HallFonts() {}
}
