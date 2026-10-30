package com.example.defyingtheheavens.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Drawing in the artifact's own pixels (the user's ask, 2026-10-30: "i want the UI and HUD to look EXACTLY the same as the artefact at
 * all GUI scales"). The mockups are a 1280 x 720 frame; here that frame is fitted to the window ({@link #unit}: window pixels to one of
 * its pixels, whatever the GUI scale), so a mockup's numbers are drawn as they stand. Words are in Pixelify Sans (HallFonts) at a CSS
 * font size on a CSS line, placed as a browser places them; boxes are CSS border-boxes; diamonds CSS squares turned 45 degrees.
 */
public final class HallArt {
	public static final int W = 1280, H = 720;
	/** The mockups' outline on words: a text-shadow two pixels each way, in black. */
	private static final int[][] OUTLINE = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};

	/** Window pixels to one of the artifact's: its 1280 x 720 fitted to the window. */
	public static double unit(Window window) {
		return Math.max(0.25, Math.min(window.getWidth() / (double) W, window.getHeight() / (double) H));
	}

	public static double unit() {
		return unit(Minecraft.getInstance().getWindow());
	}

	/** GUI pixels to one of the artifact's, for the HUD (whose GUI pixels are the window's at its GUI scale). */
	public static float hudScale() {
		Window window = Minecraft.getInstance().getWindow();
		return (float) (unit(window) / window.getGuiScale());
	}

	/** The window across and down in the artifact's pixels (where the HUD's edges are). */
	public static float width() {
		Window window = Minecraft.getInstance().getWindow();
		return (float) (window.getWidth() / unit(window));
	}

	public static float height() {
		Window window = Minecraft.getInstance().getWindow();
		return (float) (window.getHeight() / unit(window));
	}

	/** The HUD's drawing in the artifact's pixels from here, until {@link #end}. */
	public static void beginHud(GuiGraphics g) {
		float k = hudScale();
		g.pose().pushPose();
		g.pose().scale(k, k, 1);
	}

	/** A framed screen's drawing (HallFrame: one of its pixels is three of the artifact's) in the artifact's pixels, until {@link #end}. */
	public static void beginFramed(GuiGraphics g) {
		g.pose().pushPose();
		g.pose().scale(1 / 3f, 1 / 3f, 1);
	}

	public static void end(GuiGraphics g) {
		g.pose().popPose();
	}

	/** A row of the HUD's top-left stack, in the artifact's pixels, kept clear of by vanilla's boss bars (HudCorner, in GUI pixels). */
	public static void corner(float top, float bottom, float right) {
		float k = hudScale();
		HudCorner.drew((int) Math.floor(top * k), (int) Math.ceil(bottom * k), (int) Math.ceil(right * k));
	}

	// --- Words ---

	/** {@code text} in Pixelify Sans at {@code size} (a CSS font size), at the size its em comes to on the screen. */
	private static Component styled(String text, float size) {
		float px = (float) (size * unit());
		return Component.literal(text).withStyle(s -> s.withFont(HallFonts.id(px)));
	}

	/** How wide {@code text} runs at {@code size}. */
	public static float width(String text, float size) {
		return Minecraft.getInstance().font.getSplitter().stringWidth(styled(text, size)) * size / HallFonts.EM;
	}

	/**
	 * {@code text} at {@code size} on a line {@code lineHeight} tall whose top is {@code top}, from {@code x}: its baseline where CSS puts
	 * it (0.92 of the size under the top of its 1.2-size box, the box in the middle of the line); outlined in black two pixels each way
	 * where {@code outlined}, as the mockups' text-shadow.
	 */
	public static void text(GuiGraphics g, String text, float x, float top, float size, float lineHeight, int color, boolean outlined) {
		if (text.isEmpty()) return;
		Font font = Minecraft.getInstance().font;
		Component styled = styled(text, size);
		float s = size / HallFonts.EM;
		float y = top + (lineHeight - 1.2f * size) / 2 + 0.92f * size - HallFonts.BASELINE * s;
		// Each layer twice: Minecraft lays a glyph's soft edge on linearly, where a browser darkens it, and the strokes read thin.
		for (int pass = 0; pass < 2; pass++) {
			if (outlined) for (int[] o : OUTLINE) draw(g, font, styled, x + o[0], y + o[1], s, 0xFF000000);
		}
		for (int pass = 0; pass < 2; pass++) draw(g, font, styled, x, y, s, 0xFF000000 | color);
	}

	/** {@link #text} with its right end at {@code right}. */
	public static void textRight(GuiGraphics g, String text, float right, float top, float size, float lineHeight, int color, boolean outlined) {
		text(g, text, right - width(text, size), top, size, lineHeight, color, outlined);
	}

	/** {@link #text} with its middle at {@code centre}. */
	public static void textCentred(GuiGraphics g, String text, float centre, float top, float size, float lineHeight, int color, boolean outlined) {
		text(g, text, centre - width(text, size) / 2, top, size, lineHeight, color, outlined);
	}

	/** {@code text} (its own colours kept) wrapped to {@code room} at {@code size}, each line to be drawn by {@link #line}. */
	public static List<FormattedCharSequence> wrap(Component text, float size, float room) {
		float px = (float) (size * unit());
		Component styled = text.copy().withStyle(s -> s.withFont(HallFonts.id(px)));
		return Minecraft.getInstance().font.split(styled, Math.max(1, (int) (room * HallFonts.EM / size)));
	}

	/** A line {@link #wrap} gave, at {@code size} on a line {@code lineHeight} tall whose top is {@code top}, from {@code x}. */
	public static void line(GuiGraphics g, FormattedCharSequence line, float x, float top, float size, float lineHeight, int color) {
		Font font = Minecraft.getInstance().font;
		float s = size / HallFonts.EM;
		float y = top + (lineHeight - 1.2f * size) / 2 + 0.92f * size - HallFonts.BASELINE * s;
		for (int pass = 0; pass < 2; pass++) {
			g.pose().pushPose();
			g.pose().translate(x, y, 0);
			g.pose().scale(s, s, 1);
			g.drawString(font, line, 0, 0, 0xFF000000 | color, false);
			g.pose().popPose();
		}
	}

	/** An item {@code size} across at (x, y) (as the mockups' pictures of items, 16 pixels' art drawn large), its count as an item's is. */
	public static void item(GuiGraphics g, ItemStack stack, float x, float y, float size, boolean decorations) {
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		g.pose().scale(size / 16, size / 16, 1);
		g.renderItem(stack, 0, 0);
		if (decorations) g.renderItemDecorations(Minecraft.getInstance().font, stack, 0, 0);
		g.pose().popPose();
	}

	/** The largest whole size from {@code size} down to {@code least} at which {@code text} fits {@code room} ({@code least} if none). */
	public static float fitSize(String text, float room, float size, float least) {
		float s = size;
		while (s > least && width(text, s) > room) s--;
		return Math.max(s, least);
	}

	/** {@code text} cut short with "..." to fit {@code room} at {@code size}. */
	public static String fit(String text, float room, float size) {
		if (width(text, size) <= room) return text;
		String cut = text;
		while (!cut.isEmpty() && width(cut + "...", size) > room) cut = cut.substring(0, cut.length() - 1);
		return cut.isEmpty() ? "" : cut.stripTrailing() + "...";
	}

	private static void draw(GuiGraphics g, Font font, Component text, float x, float y, float scale, int color) {
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		g.pose().scale(scale, scale, 1);
		g.drawString(font, text, 0, 0, color, false);
		g.pose().popPose();
	}

	// --- The HUD's bars (the mockups' "G") ---

	/** A bar's frame of dark gold, its see-through ink, and the white of the words across it. */
	public static final int FRAME = 0xFF6E5214, TRACK = 0x990B0E12, WHITE = 0xFFFFFF;

	/**
	 * A bar: a box {@code w} by {@code h} at (x, y), its border {@code border} thick in dark gold over a see-through ink, filled from the
	 * left to {@code ratio} in {@code fill}, a band of {@code light} {@code band} deep along the fill's top (none at 0).
	 */
	public static void bar(GuiGraphics g, float x, float y, float w, float h, float border, double ratio, int fill, int light, float band) {
		box(g, x, y, w, h, border, FRAME, TRACK);
		float f = filled(w - 2 * border, ratio);
		if (f <= 0) return;
		rect(g, x + border, y + border, x + border + f, y + h - border, fill);
		if (band > 0) rect(g, x + border, y + border, x + border + f, y + border + band, light);
	}

	/** How much of a bar's inside, {@code inner} across, {@code ratio} fills, in whole pixels. */
	public static float filled(float inner, double ratio) {
		return Math.round(inner * Math.max(0, Math.min(1, ratio)));
	}

	// --- Shapes ---

	/** A rectangle from (x0, y0) to (x1, y1). */
	public static void rect(GuiGraphics g, float x0, float y0, float x1, float y1, int color) {
		if (x1 <= x0 || y1 <= y0) return;
		g.pose().pushPose();
		g.pose().translate(x0, y0, 0);
		g.pose().scale(x1 - x0, y1 - y0, 1);
		g.fill(0, 0, 1, 1, color);
		g.pose().popPose();
	}

	/** A CSS border-box: {@code w} by {@code h} at (x, y), its border {@code border} thick within that, its inside {@code fill}. */
	public static void box(GuiGraphics g, float x, float y, float w, float h, float border, int borderColor, int fill) {
		rect(g, x, y, x + w, y + border, borderColor);
		rect(g, x, y + h - border, x + w, y + h, borderColor);
		rect(g, x, y + border, x + border, y + h - border, borderColor);
		rect(g, x + w - border, y + border, x + w, y + h - border, borderColor);
		if (fill != 0) rect(g, x + border, y + border, x + w - border, y + h - border, fill);
	}

	/**
	 * A range of far hills as the mockups' SVG polygons draw them: from each of {@code points} (x along the mockup's 1280, stretched by
	 * {@code sx}; y down from {@code base}) to the next, filled down to {@code bottom}, in one batch.
	 */
	public static void ridge(GuiGraphics g, float[][] points, float sx, float base, float bottom, int color) {
		Matrix4f m = g.pose().last().pose();
		VertexConsumer buffer = g.bufferSource().getBuffer(RenderType.gui());
		float a = (color >>> 24 & 255) / 255f, r = (color >> 16 & 255) / 255f, gr = (color >> 8 & 255) / 255f, b = (color & 255) / 255f;
		for (int i = 0; i + 1 < points.length; i++) {
			float x0 = points[i][0] * sx, y0 = base + points[i][1], x1 = points[i + 1][0] * sx, y1 = base + points[i + 1][1];
			buffer.vertex(m, x0, y0, 0).color(r, gr, b, a).endVertex();
			buffer.vertex(m, x0, bottom, 0).color(r, gr, b, a).endVertex();
			buffer.vertex(m, x1, bottom, 0).color(r, gr, b, a).endVertex();
			buffer.vertex(m, x1, y1, 0).color(r, gr, b, a).endVertex();
		}
		g.flush();
	}

	/** A CSS square {@code size} across at (left, top), turned 45 degrees about its middle. */
	public static void diamond(GuiGraphics g, float left, float top, float size, int color) {
		diamond(g, left, top, size, 0, 0, color);
	}

	/** The same, its border {@code border} thick within it in {@code borderColor}. */
	public static void diamond(GuiGraphics g, float left, float top, float size, float border, int borderColor, int color) {
		g.pose().pushPose();
		g.pose().translate(left + size / 2, top + size / 2, 0);
		g.pose().mulPose(Axis.ZP.rotationDegrees(45));
		float half = size / 2;
		if (border > 0) {
			rect(g, -half, -half, half, half, borderColor);
			half -= border;
		}
		rect(g, -half, -half, half, half, color);
		g.pose().popPose();
	}

	private HallArt() {}
}
