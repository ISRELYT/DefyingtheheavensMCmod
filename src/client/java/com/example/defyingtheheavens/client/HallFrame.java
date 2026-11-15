package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

/**
 * The mockups' frame for the mod's screens (the user's ask, 2026-10-30: "i want the UI and HUD to look EXACTLY the same as the artefact
 * at all GUI scales"; their pick: the same on screen whatever the GUI scale). A screen marked {@link Framed} is laid out at the size the
 * mockups were made at (1280 x 720 at GUI scale 3: 427 x 240 on a 16:9 window, taller or wider on another), drawn scaled to the window,
 * and given the mouse in the frame's pixels (mixin/HallFrameMinecraftMixin, HallFrameGameRendererMixin, HallFrameMouseMixin,
 * HallFrameGraphicsMixin), its words in Pixelify Sans (HallFonts; mixin/HallFrameScreenMixin). One of the frame's pixels is three of the
 * artifact's (HallArt).
 * <p>
 * What other mods draw over a framed screen through Fabric's screen events (JEI's item list beside the Heaven Altar, its bookmarks, its
 * tooltips) is drawn in the frame too ({@link #frameEvents}): laid out by the screen's own size and slots, which are the frame's, it would
 * otherwise land where the GUI's pixels put it, over the screen. The mouse they are told of is the frame's (HallFrameGameRendererMixin), as
 * the screen's own is; JEI's own reading of it too (compat/jei/mixin/JeiMouseUtilMixin).
 */
public final class HallFrame {
	/** A screen drawn in the frame. */
	public interface Framed {}

	/** While a framed screen draws, or another mod draws over it: GuiGraphics answers the frame's size, and its scissors scale with it. */
	public static boolean rendering;
	/** While a framed screen draws its own words: they lie flat, as the mockups' do (HallFrameGraphicsMixin); another mod's keep theirs. */
	public static boolean flat;

	/** The phases a framed screen's render events are wrapped in: into the frame before every other listener, out of it after them all. */
	private static final ResourceLocation INTO_FRAME = DefyingTheHeavens.id("into_frame"), OUT_OF_FRAME = DefyingTheHeavens.id("out_of_frame");

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
		int mouseX = (int) (mc.mouseHandler.xpos() * width(window) / window.getScreenWidth());
		int mouseY = (int) (mc.mouseHandler.ypos() * height(window) / window.getScreenHeight());
		HallFonts.update(window);
		enter(g);
		flat = true;
		try {
			screen.renderWithTooltip(g, mouseX, mouseY, partialTick);
		} finally {
			flat = false;
			leave(g);
		}
	}

	/**
	 * Called as every screen is laid out (ScreenEvents.AFTER_INIT): a framed one's before- and after-render events, where other mods draw
	 * over a screen, are wrapped in the frame, into it before their listeners and out of it after (event phases, so whenever theirs were
	 * registered). A screen's events are new each time it is laid out, so this is done each time.
	 */
	public static void frameEvents(Screen screen) {
		if (!(screen instanceof Framed)) return;
		Event<ScreenEvents.BeforeRender> before = ScreenEvents.beforeRender(screen);
		before.addPhaseOrdering(INTO_FRAME, Event.DEFAULT_PHASE);
		before.addPhaseOrdering(Event.DEFAULT_PHASE, OUT_OF_FRAME);
		before.register(INTO_FRAME, (s, g, mouseX, mouseY, partialTick) -> enter(g));
		before.register(OUT_OF_FRAME, (s, g, mouseX, mouseY, partialTick) -> leave(g));
		Event<ScreenEvents.AfterRender> after = ScreenEvents.afterRender(screen);
		after.addPhaseOrdering(INTO_FRAME, Event.DEFAULT_PHASE);
		after.addPhaseOrdering(Event.DEFAULT_PHASE, OUT_OF_FRAME);
		after.register(INTO_FRAME, (s, g, mouseX, mouseY, partialTick) -> enter(g));
		after.register(OUT_OF_FRAME, (s, g, mouseX, mouseY, partialTick) -> leave(g));
	}

	/** How deep into the frame the drawing is: entered once however often it is asked (never scaled twice). */
	private static int depth;

	/** Into the frame: the GUI's pixels scaled to the frame's, GuiGraphics answering its size. */
	private static void enter(GuiGraphics g) {
		if (depth++ > 0) return;
		Window window = Minecraft.getInstance().getWindow();
		float f = (float) (scale(window) / window.getGuiScale());
		g.pose().pushPose();
		g.pose().scale(f, f, 1);
		rendering = true;
	}

	private static void leave(GuiGraphics g) {
		if (depth == 0 || --depth > 0) return;
		rendering = false;
		g.pose().popPose();
	}

	/** The frame's mouse, in its pixels, while {@code screen} is open and framed; NaN otherwise (JeiMouseUtilMixin). */
	public static double mouseX() {
		Minecraft mc = Minecraft.getInstance();
		return mc.screen instanceof Framed ? mc.mouseHandler.xpos() * width(mc.getWindow()) / mc.getWindow().getScreenWidth() : Double.NaN;
	}

	public static double mouseY() {
		Minecraft mc = Minecraft.getInstance();
		return mc.screen instanceof Framed ? mc.mouseHandler.ypos() * height(mc.getWindow()) / mc.getWindow().getScreenHeight() : Double.NaN;
	}

	private HallFrame() {}
}
