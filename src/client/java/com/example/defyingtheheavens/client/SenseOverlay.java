package com.example.defyingtheheavens.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;

/**
 * The layer the cultivator's senses draw on, laid over the finished frame so it keeps its colour. Qi Sense drains the colour
 * from the whole frame (world and hand) with a post effect, so what must stay vivid is drawn here instead of into the world:
 * the qi motes ({@link QiSenseClientHandler}) and the consciousness domains ({@link ConsciousnessRenderer}).
 * <p>
 * At the end of world rendering this target is cleared and given a copy of the world's depth, so the layer still hides
 * behind terrain and entities. It holds premultiplied colour: domains and the motes' cores are blended in, the motes' halos
 * are added (alpha 0, so they brighten whatever is under them). Just before vanilla lays the entity outlines over the frame
 * (which therefore stay coloured too), the post effect runs and this layer goes on top. Used whether or not Qi Sense is on,
 * so the domains look the same either way.
 */
public final class SenseOverlay {
	private static TextureTarget target;
	/** The layer was drawn this frame and still has to be laid over it. */
	private static boolean pending;

	/** WorldRenderEvents.END: draws this frame's layer. */
	public static void onWorldEnd(WorldRenderContext context) {
		pending = false;
		ConsciousnessRenderer.captureView(context);
		boolean motes = QiSenseClientHandler.hasMotes();
		boolean domains = ConsciousnessRenderer.hasDomains();
		if (!motes && !domains) return;

		Minecraft mc = Minecraft.getInstance();
		RenderTarget main = mc.getMainRenderTarget();
		if (main.width <= 0 || main.height <= 0) return; // minimised
		if (target == null) {
			target = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
			target.setClearColor(0, 0, 0, 0);
		} else if (target.width != main.width || target.height != main.height) {
			target.resize(main.width, main.height, Minecraft.ON_OSX);
		}
		target.clear(Minecraft.ON_OSX);
		target.copyDepthFrom(main);
		target.bindWrite(false);

		primeBlendMode();
		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(false); // tested against the world, never written: layers in the layer don't hide each other
		RenderSystem.disableCull();
		RenderSystem.setShaderColor(1, 1, 1, 1);
		// Blended in: coloured and covering, so the frame beneath shows through by what's left.
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		if (domains) ConsciousnessRenderer.renderDomains(context);
		if (motes) {
			QiSenseClientHandler.renderMotes(context, false);
			// Added on: colour without coverage, so the glow brightens whatever is under it.
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
					GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
			QiSenseClientHandler.renderMotes(context, true);
		}
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		main.bindWrite(false);
		pending = true;
	}

	/**
	 * From GameRendererMixin, after the world and hand are drawn and just before vanilla adds the entity outlines: Qi Sense's
	 * monochrome, then this layer over it.
	 */
	public static void beforeEntityOutline(float partialTick) {
		QiSenseClientHandler.applyMonochrome(partialTick);
		if (!pending || target == null) return;
		pending = false;
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		main.bindWrite(false);
		primeBlendMode();
		RenderSystem.enableBlend();
		// Premultiplied "over": the layer's colour plus whatever its coverage leaves of the frame.
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		target.blitToScreen(main.width, main.height, false);
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	/**
	 * Every vanilla core shader carries the same blend mode and re-applies it whenever the last shader used had another one
	 * (a post effect pass), which would undo the blend functions set here. Applying a core shader once first makes the cached
	 * mode match, so the shaders used below leave the blend functions alone.
	 */
	private static void primeBlendMode() {
		ShaderInstance shader = GameRenderer.getPositionColorShader();
		if (shader == null) return;
		shader.apply();
		shader.clear();
	}

	private SenseOverlay() {}
}
