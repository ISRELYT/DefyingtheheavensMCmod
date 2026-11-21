package com.example.defyingtheheavens.client.fx;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * A render target whose colour holds light past white (half floats, RGBA16F) and is sampled smoothly: the effects' layer
 * ({@link FxLayer}) and its bloom ({@link FxPost}). Light piled up there keeps its strength until the frame is put together, where it
 * rolls off to cream-white instead of clipping.
 */
public final class FxTarget extends TextureTarget {
	public FxTarget(int width, int height, boolean depth) {
		super(width, height, depth, Minecraft.ON_OSX);
	}

	@Override
	public void createBuffers(int width, int height, boolean clearError) {
		super.createBuffers(width, height, clearError);
		// The colour made again with room for light past 1, and attached anew; then smooth sampling (vanilla's is nearest).
		GlStateManager._bindTexture(colorTextureId);
		GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, this.width, this.height, 0, GL11.GL_RGBA, GL11.GL_FLOAT, null);
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBufferId);
		GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, colorTextureId, 0);
		checkStatus();
		setFilterMode(GL11.GL_LINEAR);
		clear(clearError);
		unbindRead();
		unbindWrite();
	}
}
