package com.example.defyingtheheavens.client.fx;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The effects' textures (textures/fx, made by tools/fx-tex): smooth, repeating, and given mipmaps the first time each is used (and
 * again after a reload), so a cloud deck or a ribbon seen from far off doesn't shimmer.
 */
public final class FxTextures {
	public static final ResourceLocation GLOW = tex("glow"), SPARK = tex("spark"), STREAK = tex("streak"), RIBBON = tex("ribbon"),
			FLOW = tex("flow"), FLAME_BAND = tex("flame_band"), CLOUD = tex("cloud"), PUFF = tex("puff"), RING = tex("ring"), RAYS = tex("rays"),
			ARRAY_RINGS = tex("array_rings"), ARRAY_CIRCLES = tex("array_circles"), ARRAY_STAR = tex("array_star"), ARRAY_FLAME = tex("array_flame"),
			ARRAY_CORE = tex("array_core"), SOFT = tex("soft");

	/** Every one of them (read and given its mipmaps ahead of need: FxLayer#warmUp). */
	public static final List<ResourceLocation> ALL = List.of(GLOW, SPARK, STREAK, RIBBON, FLOW, FLAME_BAND, CLOUD, PUFF, RING, RAYS, ARRAY_RINGS,
			ARRAY_CIRCLES, ARRAY_STAR, ARRAY_FLAME, ARRAY_CORE, SOFT);

	/** The texture ids given mipmaps (a reload can hand a texture a new id; then it is done again). */
	private static final Map<ResourceLocation, Integer> MIPPED = new HashMap<>();

	private static ResourceLocation tex(String name) {
		return DefyingTheHeavens.id("textures/fx/" + name + ".png");
	}

	/** {@code texture} read and given its mipmaps now, so the frame that first draws it doesn't wait on it. */
	static void warm(ResourceLocation texture) {
		bind(texture);
		RenderSystem.setShaderTexture(0, 0);
	}

	/** {@code texture} as the shader's texture 0. */
	static void bind(ResourceLocation texture) {
		AbstractTexture t = Minecraft.getInstance().getTextureManager().getTexture(texture);
		int id = t.getId();
		Integer done = MIPPED.get(texture);
		if (done == null || done != id) {
			mipmap(id);
			MIPPED.put(texture, id);
		}
		RenderSystem.setShaderTexture(0, id);
	}

	/** Resources reloaded (F3+T): every texture is given its mipmaps again on its next use. */
	public static void reloaded() {
		MIPPED.clear();
	}

	private static void mipmap(int id) {
		GlStateManager._bindTexture(id);
		int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
		int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
		int levels = Math.max(0, 31 - Integer.numberOfLeadingZeros(Math.max(1, Math.max(w, h))));
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, levels);
		GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
	}

	private FxTextures() {}
}
