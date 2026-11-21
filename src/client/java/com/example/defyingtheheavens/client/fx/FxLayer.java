package com.example.defyingtheheavens.client.fx;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The effects' layer (the VFX plan's toolkit): the cinematics' light, smoke and cloud drawn into a target of their own that holds light
 * past white ({@link FxTarget}), with a copy of the world's depth so they still hide behind hills and bodies, at the end of the world's
 * drawing (WorldRenderEvents.END). It is laid over the finished frame later ({@link FxPost}): after a shader pack has finished its
 * frame, so the effects look the same with a pack or without, and graded apart from the world, so the gold stays vivid while the world
 * goes dark. Whoever draws into it registers a painter ({@link #painter}); a painter that throws is reported to its owner by the
 * painter itself (the cinematics end everything and put it back), and anything else that throws here is told through
 * {@link Fx#onFailure}. Everything it needs is made ready on joining a world and after a reload ({@link #warmUp}), and let go on leaving.
 */
public final class FxLayer {
	private static final List<Painter> PAINTERS = new ArrayList<>();
	private static FxTarget layer;
	/** This frame's layer was drawn and still has to be laid over it. */
	private static boolean pending;

	/** Something that draws into the layer: {@code wants} says whether it has anything this frame, {@code paint} draws it. */
	public record Painter(java.util.function.BooleanSupplier wants, Consumer<WorldRenderContext> paint) {}

	public static void painter(java.util.function.BooleanSupplier wants, Consumer<WorldRenderContext> paint) {
		PAINTERS.add(new Painter(wants, paint));
	}

	/** WorldRenderEvents.END: this frame's layer, if any painter has work. */
	public static void onWorldEnd(WorldRenderContext context) {
		pending = false;
		FxPost.frameBegins();
		Fx.newFrame();
		if (!Fx.ready()) return;
		boolean any = false;
		for (Painter p : PAINTERS) any |= p.wants().getAsBoolean();
		if (!any) return;
		Minecraft mc = Minecraft.getInstance();
		RenderTarget main = mc.getMainRenderTarget();
		if (main.width <= 0 || main.height <= 0) return; // minimised
		// Everything from here guarded: a failure (making the target, copying the depth, a painter's own) ends the cinematics (Fx#failed),
		// frees the shared builder whoever left it building, and puts the state back, so the next frame draws as ever.
		try {
			make(main);
			layer.clear(Minecraft.ON_OSX);
			layer.copyDepthFrom(main);
			layer.bindWrite(false);
			Fx.primeBlendMode();
			RenderSystem.enableBlend();
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(GL11.GL_LEQUAL);
			RenderSystem.depthMask(false);
			RenderSystem.disableCull();
			RenderSystem.setShaderColor(1, 1, 1, 1);
			Fx.frame(context);
			for (Painter p : PAINTERS) {
				if (p.wants().getAsBoolean()) p.paint().accept(context);
			}
			Fx.end();
			pending = true;
		} catch (RuntimeException e) {
			pending = false;
			close();
			Fx.failed("the effects' layer", e);
		} finally {
			Fx.abandon();
			RenderSystem.depthMask(true);
			RenderSystem.enableCull();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableBlend();
			main.bindWrite(false);
		}
	}

	/** The layer's target, made (or fitted to the window) if it isn't. */
	private static void make(RenderTarget main) {
		if (layer == null) {
			layer = new FxTarget(main.width, main.height, true);
			layer.setClearColor(0, 0, 0, 0);
		} else if (layer.width != main.width || layer.height != main.height) {
			layer.resize(main.width, main.height, Minecraft.ON_OSX);
		}
	}

	// --- Made ready before any cinematic needs it (on joining a world, after F3+T): its first frames never wait on a texture or a program ---

	/** The steps done of the warm-up (-1: nothing to do). */
	private static int warming = -1;

	/** Joining a world or resources reloaded: the textures, the post chain and the layer made ready over the next ticks, one a tick. */
	public static void warmUp() {
		warming = 0;
	}

	/** A client tick: one step of the warm-up, while in a world and with no reload under way. */
	public static void warmTick() {
		if (warming < 0) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.getOverlay() != null || !Fx.ready()) return;
		RenderTarget main = mc.getMainRenderTarget();
		if (main.width <= 0 || main.height <= 0) return;
		try {
			if (warming < FxTextures.ALL.size()) {
				FxTextures.warm(FxTextures.ALL.get(warming));
			} else if (warming == FxTextures.ALL.size()) {
				make(main);
				layer.clear(Minecraft.ON_OSX);
				main.bindWrite(false);
			} else {
				FxPost.warm(main);
				DefyingTheHeavens.LOGGER.info("[DTH-FX] made ready: {} textures, the layer and the post chain at {}x{}", FxTextures.ALL.size(), main.width, main.height);
				warming = -1;
				return;
			}
			warming++;
		} catch (RuntimeException e) {
			warming = -1;
			DefyingTheHeavens.LOGGER.error("[DTH-FX] couldn't make the effects ready ahead: they are made on first use", e);
		}
	}

	/** For the capture harness's account: the layer's size, or "none". */
	public static String describe() {
		return layer == null ? "none" : layer.width + "x" + layer.height;
	}

	/** The layer drawn this frame (for {@link FxPost}), or null. */
	static FxTarget drawn() {
		return pending ? layer : null;
	}

	/** Laid over the frame: the next frame starts again. */
	static void used() {
		pending = false;
	}

	/** The layer's target as last made (drawn this frame or not), or null if none has been. */
	static FxTarget lastMade() {
		return layer;
	}

	/** Resources reloaded or the world left: the target is made again on next use. */
	public static void close() {
		if (layer != null) layer.destroyBuffers();
		layer = null;
		pending = false;
		DefyingTheHeavens.LOGGER.debug("[DTH-FX] layer closed");
	}

	private FxLayer() {}
}
