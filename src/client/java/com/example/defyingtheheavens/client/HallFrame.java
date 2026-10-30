package com.example.defyingtheheavens.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/**
 * The mockups' frame for the mod's screens (the user's ask, 2026-10-30: "i want the UI and HUD to look EXACTLY the same as the artefact
 * at all GUI scales"; their pick: the same on screen whatever the GUI scale). A screen marked {@link Framed} is laid out at the size the
 * mockups were made at (1280 x 720 at GUI scale 3: 427 x 240 on a 16:9 window, taller or wider on another), drawn scaled to the window,
 * and given the mouse in the frame's pixels (mixin/HallFrameMinecraftMixin, HallFrameGameRendererMixin, HallFrameMouseMixin,
 * HallFrameGraphicsMixin), its words in Pixelify Sans (HallFonts; mixin/HallFrameScreenMixin). One of the frame's pixels is three of the
 * artifact's (HallArt).
 */
public final class HallFrame {
	/** A screen drawn in the frame. */
	public interface Framed {}

	/** While a framed screen draws: GuiGraphics answers the frame's size, and its scissors scale with it. */
	public static boolean rendering;

	/** Window pixels to one of the frame's: three of the artifact's, its 1280 x 720 fitted to the window (HallArt#unit). */
	public static double scale(Window window) {
		return 3.0 * HallArt.unit(window);
	}

	/** The frame across and down, in its pixels. */
	public static int width(Window window) {
		return (int) Math.ceil(window.getWidth() / scale(window));
	}

	public static int height(Window window) {
		return (int) Math.ceil(window.getHeight() / scale(window));
	}

	/** Draws {@code screen} in the frame: scaled from the GUI's pixels to the frame's, the mouse given in the frame's. */
	public static void render(Screen screen, GuiGraphics g, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		Window window = mc.getWindow();
		float f = (float) (scale(window) / window.getGuiScale());
		int mouseX = (int) (mc.mouseHandler.xpos() * width(window) / window.getScreenWidth());
		int mouseY = (int) (mc.mouseHandler.ypos() * height(window) / window.getScreenHeight());
		HallFonts.update(window);
		g.pose().pushPose();
		g.pose().scale(f, f, 1);
		rendering = true;
		try {
			screen.renderWithTooltip(g, mouseX, mouseY, partialTick);
		} finally {
			rendering = false;
			g.pose().popPose();
		}
	}

	private HallFrame() {}
}
