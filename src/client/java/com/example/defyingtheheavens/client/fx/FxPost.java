package com.example.defyingtheheavens.client.fx;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.DevTools;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostPass;
import org.joml.Matrix4f;

/**
 * The frame put together (the brief §6.5's chain, in one place): the world graded (exposure, open sky darkened further, tint,
 * saturation, contrast), the effects' layer laid over it with its own bloom (three blurred sizes, so a white-hot core glows wide and
 * haze hardly at all), a vignette, then a radial blur out from the push's or the whip's aim. Run from GameRendererMixin once the world,
 * the shader pack's frame and the senses' layer are done, so with a pack or without. The grade, the vignette and the blur are the
 * breakthrough player's own ({@link #grade}, {@link #radial}, set each frame by whoever owns them); everyone else gets the layer and
 * its bloom alone, their own pixels untouched where nothing is added and never over their first-person hand. The split colour and the
 * shockwave's warp join this chain in step B2's second half. Made ready ahead of need ({@link #warm}: FxLayer#warmUp), let go on leaving
 * the world; anything in it that throws closes it and ends the cinematics (Fx#failed), the frame left as the world drew it.
 */
public final class FxPost {
	private static TextureTarget swap;
	private static FxTarget b1, b1t, b2, b2t, b3, b3t;
	private static PostPass down1, blur1h, blur1v, down2, blur2h, blur2v, down3, blur3h, blur3v, composite, radialPass;
	private static boolean failed;
	private static int builtWidth = -1, builtHeight = -1;

	// This frame's grade, set by its owner after frameBegins(); identity unless set.
	private static boolean graded;
	private static float exposure, skyDark, saturation, contrast, tintR, tintG, tintB, vignette;
	private static float radial, radialX, radialY;
	/** How bright the effects' light is laid on, and its bloom. */
	private static float fxBoost = 1, bloomStrength = 1;

	/** From FxLayer as the world's drawing ends: nothing graded until an owner says so. */
	static void frameBegins() {
		graded = false;
		radial = 0;
		fxBoost = 1;
		bloomStrength = 1;
	}

	/** The breakthrough player's grade for this frame. */
	public static void grade(float exposure, float skyDark, float saturation, float contrast, float tintR, float tintG, float tintB, float vignette) {
		graded = true;
		FxPost.exposure = exposure;
		FxPost.skyDark = skyDark;
		FxPost.saturation = saturation;
		FxPost.contrast = contrast;
		FxPost.tintR = tintR;
		FxPost.tintG = tintG;
		FxPost.tintB = tintB;
		FxPost.vignette = vignette;
	}

	/** This frame's radial blur: its strength (0 none) and its centre on the screen (0..1, y up). */
	public static void radial(float strength, float x, float y) {
		radial = strength;
		radialX = x;
		radialY = y;
	}

	/** How strongly the effects' layer and its bloom are laid on this frame (1, 1 as they were drawn). */
	public static void light(float boost, float bloom) {
		fxBoost = boost;
		bloomStrength = bloom;
	}

	/**
	 * GameRendererMixin: the frame put together, if there is anything to do. Guarded whole: anything that throws closes the chain (made
	 * again on next use), ends the cinematics (Fx#failed) and leaves the frame as the world drew it, the state put back.
	 */
	public static void apply(float partial) {
		compose(partial);
		if (DevTools.ENABLED && wantState) { // read where the chain runs, every frame, a film or none
			wantState = false;
			lastState = state(Minecraft.getInstance().getMainRenderTarget());
		}
	}

	private static void compose(float partial) {
		FxTarget fx = FxLayer.drawn();
		FxLayer.used();
		if (fx == null && !graded && radial <= 0) return;
		// The world's depth comes with the layer: with none drawn this frame, the last one made (its depth a frame old), or nothing at all.
		FxTarget layerOrLast = fx != null ? fx : FxLayer.lastMade();
		if (layerOrLast == null) return;
		Minecraft mc = Minecraft.getInstance();
		RenderTarget main = mc.getMainRenderTarget();
		// The depth test and blending as vanilla had them here (both off, after the hand): put back exactly, whatever happens.
		boolean depthWas = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
		boolean blendWas = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
		try {
			if (!build(main)) return;
			LAYER_PROXY.follow(layerOrLast);
			RenderSystem.disableBlend();
			RenderSystem.disableDepthTest();
			RenderSystem.depthMask(false);
			RenderSystem.resetTextureMatrix();
			boolean bloom = fx != null;
			if (bloom) {
				down1.getEffect().safeGetUniform("Knee").set(0.6f);
				run(down1, partial);
				blur(blur1h, blur1v, partial);
				run(down2, partial);
				blur(blur2h, blur2v, partial);
				run(down3, partial);
				blur(blur3h, blur3v, partial);
			}
			if (Fx.devThrowNow("post")) throw new IllegalStateException("[DTH-FX] dev: the post chain throws part-way (the failure-safety test)");
			var e = composite.getEffect();
			if (graded) {
				e.safeGetUniform("Exposure").set(exposure);
				e.safeGetUniform("SkyDark").set(skyDark);
				e.safeGetUniform("Saturation").set(saturation);
				e.safeGetUniform("Contrast").set(contrast);
				e.safeGetUniform("Tint").set(tintR, tintG, tintB);
				e.safeGetUniform("Vignette").set(vignette);
			} else {
				e.safeGetUniform("Exposure").set(1f);
				e.safeGetUniform("SkyDark").set(1f);
				e.safeGetUniform("Saturation").set(1f);
				e.safeGetUniform("Contrast").set(1f);
				e.safeGetUniform("Tint").set(1f, 1f, 1f);
				e.safeGetUniform("Vignette").set(0f);
			}
			e.safeGetUniform("FxBoost").set(bloom ? fxBoost : 0f);
			e.safeGetUniform("BloomStrength").set(bloom ? bloomStrength : 0f);
			e.safeGetUniform("FxCover").set(bloom ? 1f : 0f);
			currentLayer = layerOrLast; // with no layer drawn this frame, its light is weighed at 0 (FxBoost)
			// The frame's depth as it is now, to keep the layer off the first-person hand (drawn after the layer took the world's depth;
			// see dth_cine_composite). With none to read, the layer's own depth (nothing masked).
			currentMainDepth = main.getDepthTextureId() > 0 ? main.getDepthTextureId() : layerOrLast.getDepthTextureId();
			run(composite, partial);
			radialPass.getEffect().safeGetUniform("Strength").set(Math.max(0, radial));
			radialPass.getEffect().safeGetUniform("Center").set(radialX, radialY);
			run(radialPass, partial);
		} catch (RuntimeException e) {
			// Whatever pass was part-way: none of the chain's programs left in use, for the post passes' cache or the core shaders'.
			try {
				if (composite != null) composite.getEffect().clear();
				net.minecraft.client.renderer.ShaderInstance vanilla = net.minecraft.client.renderer.GameRenderer.getPositionColorShader();
				if (vanilla != null) vanilla.clear();
			} catch (RuntimeException again) {
				DefyingTheHeavens.LOGGER.error("[DTH-FX] couldn't clear the post chain's program after its failure", again);
			}
			Fx.abandon(); // the shared builder free, should whoever failed have left it building
			close();
			Fx.failed("the cinematics' post chain", e);
		} finally {
			if (depthWas) RenderSystem.enableDepthTest();
			else RenderSystem.disableDepthTest();
			if (blendWas) RenderSystem.enableBlend();
			else RenderSystem.disableBlend();
			RenderSystem.depthMask(true);
			RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL); // a pass let go part-way leaves vanilla's "always"
			main.bindWrite(true);
		}
	}

	/** The layer the composite reads this frame (its colour and the world's depth kept with it), and the frame's depth now. */
	private static FxTarget currentLayer;
	private static int currentMainDepth;

	// --- For the capture harness (development only): the GL state the chain leaves, read right after it ---

	private static boolean wantState;
	private static String lastState = "not read yet";

	/** The next frame's state after the chain is read (development only); {@link #lastState} gives the last read. */
	public static void readState() {
		wantState = true;
	}

	public static String lastState() {
		return lastState;
	}

	private static String state(RenderTarget main) {
		int fbo = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING);
		return "layer=" + FxLayer.describe() + " chain=" + (composite == null ? (failed ? "failed" : "not made") : builtWidth + "x" + builtHeight)
				+ " blend=" + org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND)
				+ " depthTest=" + org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST)
				+ " depthMask=" + org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK)
				+ " cull=" + org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE)
				+ " framebuffer=" + (fbo == main.frameBufferId ? "main" : String.valueOf(fbo))
				+ " shared builder building=" + com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder().building();
	}

	private static void run(PostPass pass, float partial) {
		pass.process(partial);
	}

	private static void blur(PostPass h, PostPass v, float partial) {
		h.getEffect().safeGetUniform("BlurDir").set(1f, 0f);
		h.process(partial);
		v.getEffect().safeGetUniform("BlurDir").set(0f, 1f);
		v.process(partial);
	}

	/** The targets and passes, made on first use and kept to the window's size; false (logged once) if a program can't load. */
	private static boolean build(RenderTarget main) {
		if (failed) return false;
		int w = main.width, h = main.height;
		if (w <= 0 || h <= 0) return false;
		try {
			if (composite == null) {
				Minecraft mc = Minecraft.getInstance();
				var res = mc.getResourceManager();
				swap = new TextureTarget(w, h, false, Minecraft.ON_OSX);
				b1 = new FxTarget(half(w, 2), half(h, 2), false);
				b1t = new FxTarget(half(w, 2), half(h, 2), false);
				b2 = new FxTarget(half(w, 4), half(h, 4), false);
				b2t = new FxTarget(half(w, 4), half(h, 4), false);
				b3 = new FxTarget(half(w, 8), half(h, 8), false);
				b3t = new FxTarget(half(w, 8), half(h, 8), false);
				// The first step down reads the effects' layer, which is made each frame (FxLayer): read through a stand-in that hands over
				// whichever layer is current.
				down1 = new PostPass(res, "dth_fx_down", LAYER_PROXY, b1);
				blur1h = new PostPass(res, "dth_fx_blur", b1, b1t);
				blur1v = new PostPass(res, "dth_fx_blur", b1t, b1);
				down2 = new PostPass(res, "dth_fx_down", b1, b2);
				blur2h = new PostPass(res, "dth_fx_blur", b2, b2t);
				blur2v = new PostPass(res, "dth_fx_blur", b2t, b2);
				down3 = new PostPass(res, "dth_fx_down", b2, b3);
				blur3h = new PostPass(res, "dth_fx_blur", b3, b3t);
				blur3v = new PostPass(res, "dth_fx_blur", b3t, b3);
				composite = new PostPass(res, "dth_cine_composite", main, swap);
				composite.addAuxAsset("FxSampler", () -> currentLayer.getColorTextureId(), w, h);
				composite.addAuxAsset("DepthSampler", () -> currentLayer.getDepthTextureId(), w, h);
				composite.addAuxAsset("MainDepthSampler", () -> currentMainDepth, w, h);
				composite.addAuxAsset("Bloom1Sampler", b1::getColorTextureId, b1.width, b1.height);
				composite.addAuxAsset("Bloom2Sampler", b2::getColorTextureId, b2.width, b2.height);
				composite.addAuxAsset("Bloom3Sampler", b3::getColorTextureId, b3.width, b3.height);
				radialPass = new PostPass(res, "dth_cine_radial", swap, main);
				builtWidth = -1;
			}
			if (w != builtWidth || h != builtHeight) {
				swap.resize(w, h, Minecraft.ON_OSX);
				swap.setFilterMode(org.lwjgl.opengl.GL11.GL_LINEAR);
				b1.resize(half(w, 2), half(h, 2), Minecraft.ON_OSX);
				b1t.resize(half(w, 2), half(h, 2), Minecraft.ON_OSX);
				b2.resize(half(w, 4), half(h, 4), Minecraft.ON_OSX);
				b2t.resize(half(w, 4), half(h, 4), Minecraft.ON_OSX);
				b3.resize(half(w, 8), half(h, 8), Minecraft.ON_OSX);
				b3t.resize(half(w, 8), half(h, 8), Minecraft.ON_OSX);
				for (PostPass p : new PostPass[] {down1, blur1h, blur1v, down2, blur2h, blur2v, down3, blur3h, blur3v, composite, radialPass}) {
					p.setOrthoMatrix(new Matrix4f().setOrtho(0, p.outTarget.width, 0, p.outTarget.height, 0.1f, 1000f));
				}
				builtWidth = w;
				builtHeight = h;
			}
			return true;
		} catch (Exception ex) {
			failed = true;
			DefyingTheHeavens.LOGGER.error("[DTH-FX] the cinematics' post chain couldn't load; the effects' layer won't show", ex);
			close();
			return false;
		}
	}

	private static int half(int size, int by) {
		return Math.max(1, size / by);
	}

	/**
	 * Made ready ahead of need (FxLayer#warmUp, after the layer is made): the programs read and linked, the targets made, and the bloom's
	 * passes run once over the empty layer, so a cinematic's first frame draws through a chain already in use. Writes nothing to the frame.
	 */
	static void warm(RenderTarget main) {
		if (!build(main)) return;
		FxTarget layer = FxLayer.lastMade();
		if (layer == null) return;
		LAYER_PROXY.follow(layer);
		RenderSystem.disableBlend();
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		try {
			run(down1, 0);
			blur(blur1h, blur1v, 0);
			run(down2, 0);
			blur(blur2h, blur2v, 0);
			run(down3, 0);
			blur(blur3h, blur3v, 0);
		} finally {
			RenderSystem.enableDepthTest();
			RenderSystem.depthMask(true);
			main.bindWrite(true);
		}
	}

	/** Resources reloaded (F3+T), the world left or the game closing: everything made again on next use. */
	public static void close() {
		for (PostPass p : new PostPass[] {down1, blur1h, blur1v, down2, blur2h, blur2v, down3, blur3h, blur3v, composite, radialPass}) {
			if (p != null) p.close();
		}
		down1 = blur1h = blur1v = down2 = blur2h = blur2v = down3 = blur3h = blur3v = composite = radialPass = null;
		for (RenderTarget t : new RenderTarget[] {swap, b1, b1t, b2, b2t, b3, b3t}) {
			if (t != null) t.destroyBuffers();
		}
		swap = null;
		b1 = b1t = b2 = b2t = b3 = b3t = null;
		builtWidth = builtHeight = -1;
	}

	/** Resources reloaded: a program that failed may load now. */
	public static void reloaded() {
		close();
		failed = false;
	}

	/**
	 * A stand-in for the effects' layer as the first bloom pass's input (a pass is made with its targets, the layer is made by
	 * FxLayer and may be made again): its colour, size and binding are the current layer's.
	 */
	private static final LayerProxy LAYER_PROXY = new LayerProxy();

	private static final class LayerProxy extends RenderTarget {
		private RenderTarget current;

		LayerProxy() {
			super(false);
		}

		void follow(RenderTarget layer) {
			current = layer;
			width = layer.width;
			height = layer.height;
			viewWidth = layer.viewWidth;
			viewHeight = layer.viewHeight;
		}

		@Override
		public int getColorTextureId() {
			return current.getColorTextureId();
		}

		@Override
		public void bindRead() {
			current.bindRead();
		}

		@Override
		public void unbindRead() {
			current.unbindRead();
		}

		@Override
		public void bindWrite(boolean setViewport) {
			current.bindWrite(setViewport);
		}

		@Override
		public void unbindWrite() {
			current.unbindWrite();
		}

		@Override
		public void resize(int width, int height, boolean clearError) {
		}

		@Override
		public void destroyBuffers() {
		}
	}

	private FxPost() {}
}
