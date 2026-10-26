package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.ModDimensions;
import com.example.defyingtheheavens.client.compat.ShaderPacks;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

/**
 * A realm's own sky under a shader pack. A pack draws the sky with programs of its own, which keep nothing the realm's sky
 * renderer drew: space lost its nebulae, stars, suns and planets, the Netherworld its ghost fires.
 * So in the realms whose sky is wholly the mod's ({@link #ownsTheSky}), the sky renderer is run again at the end of the
 * world's drawing (WorldRenderEvents.END), into a target of its own, and the world's depth is kept beside it (vanilla clears
 * it before the hand); once the pack has finished its frame, before Qi Sense greys it and the senses' layer goes on
 * (GameRendererMixin), that sky is laid over the frame wherever the kept depth is the far plane: open sky, nothing of the
 * world before it (shader {@code sky_layer}). The land stays the pack's, lit its way; without a pack none of this runs.
 */
public final class SkyLayer {
	private static ShaderInstance shader;
	private static TextureTarget sky;
	private static TextureTarget depth;
	/** This frame's sky was drawn and still has to be laid over it. */
	private static boolean pending;

	public static void register() {
		CoreShaderRegistrationCallback.EVENT.register(context ->
				context.register(DefyingTheHeavens.id("sky_layer"), DefaultVertexFormat.POSITION_TEX, s -> shader = s));
	}

	/**
	 * The realms whose sky is drawn the mod's way under a pack: space, the Netherworld, the Soul River, the Inner Realm. Not the
	 * Spatial Gap: its own sky is a near-black void, and laid over a pack it looked bare beside the pack's own (its haze and
	 * stars, the gap's storms blooming in it), which the user liked better.
	 */
	static boolean ownsTheSky(ResourceKey<Level> dimension) {
		return dimension == ModDimensions.ASTRAL_VOID || ModDimensions.isNetherworld(dimension) || ModDimensions.isSoulRiver(dimension)
				|| ModDimensions.isInnerRealm(dimension) || ModDimensions.isGreatVoid(dimension) || ModDimensions.isVoidBreach(dimension)
				|| ModDimensions.isImmortalRealm(dimension) || ModDimensions.isImmortalPassage(dimension) || ModDimensions.isSmallWorld(dimension);
	}

	/** WorldRenderEvents.END: under a shader pack, in such a realm, the frame's sky drawn whole and the world's depth kept. */
	public static void onWorldEnd(WorldRenderContext context) {
		pending = false;
		ClientLevel level = context.world();
		if (shader == null || level == null || !ownsTheSky(level.dimension()) || !ShaderPacks.inUse()) return;
		DimensionRenderingRegistry.SkyRenderer renderer = DimensionRenderingRegistry.getSkyRenderer(level.dimension());
		if (renderer == null) return;
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		if (main.width <= 0 || main.height <= 0) return; // minimised
		sky = sized(sky, main);
		depth = sized(depth, main);
		depth.copyDepthFrom(main);
		sky.clear(Minecraft.ON_OSX);
		sky.bindWrite(true);
		// As the sky is drawn in its own place: the world's projection, the camera's turn in the pose and nothing in the view.
		Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
		VertexSorting sorting = RenderSystem.getVertexSorting();
		RenderSystem.setProjectionMatrix(context.projectionMatrix(), VertexSorting.DISTANCE_TO_ORIGIN);
		PoseStack view = RenderSystem.getModelViewStack();
		view.pushPose();
		view.setIdentity();
		RenderSystem.applyModelViewMatrix();
		try {
			renderer.render(context);
			pending = true;
		} catch (RuntimeException e) {
			DefyingTheHeavens.LOGGER.error("The sky layer failed", e);
		} finally {
			view.popPose();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.setProjectionMatrix(projection, sorting);
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableBlend();
			RenderSystem.enableCull();
			RenderSystem.enableDepthTest();
			RenderSystem.depthMask(true);
			RenderSystem.setShaderColor(1, 1, 1, 1);
			main.bindWrite(false);
		}
	}

	/** GameRendererMixin, the pack's frame finished: this frame's sky over its open sky. */
	public static void composite() {
		if (!pending || shader == null || sky == null || depth == null) return;
		pending = false;
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		if (sky.width != main.width || sky.height != main.height) return;
		main.bindWrite(true);
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(() -> shader);
		RenderSystem.setShaderTexture(0, sky.getColorTextureId());
		RenderSystem.setShaderTexture(1, depth.getDepthTextureId());
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.vertex(-1, -1, 0).uv(0, 0).endVertex();
		buffer.vertex(1, -1, 0).uv(1, 0).endVertex();
		buffer.vertex(1, 1, 0).uv(1, 1).endVertex();
		buffer.vertex(-1, 1, 0).uv(0, 1).endVertex();
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.disableBlend();
		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
	}

	/** {@code target}, made or resized to the frame's size. */
	private static TextureTarget sized(TextureTarget target, RenderTarget main) {
		if (target == null) {
			target = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
			target.setClearColor(0, 0, 0, 1);
		} else if (target.width != main.width || target.height != main.height) {
			target.resize(main.width, main.height, Minecraft.ON_OSX);
		}
		return target;
	}

	private SkyLayer() {}
}
