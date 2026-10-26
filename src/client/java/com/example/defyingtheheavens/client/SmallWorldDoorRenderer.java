package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.SmallWorldDoor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * A small world's door (SmallWorldDoor): a sheet of the void's stars (the end gateway's, as its world's gate) in a frame of gold, opening
 * out of its middle over {@link #OPENING} ticks and closing into it as its time runs out, both faces drawn: the frame solid and lit of
 * itself (it reads by day as by night), a soft gold glow about it (the dark's). Sparks along it are the entity's own.
 */
public class SmallWorldDoorRenderer extends EntityRenderer<SmallWorldDoor> {
	private static final float OPENING = 12f;
	/** The frame's width, and how far its glow reaches past it. */
	private static final float FRAME = 0.09f, GLOW = 0.32f;
	private static final float[] GOLD = {0.95f, 0.78f, 0.36f}, GLOW_GOLD = {1.0f, 0.86f, 0.5f};

	public SmallWorldDoorRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(SmallWorldDoor door, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
		float shown = Math.min(ease(door.age(partialTick) / OPENING), ease(door.left(partialTick) / OPENING));
		if (shown > 0.01f) {
			float w = SmallWorldDoor.WIDTH / 2 * shown, middle = SmallWorldDoor.HEIGHT / 2, h = middle * shown;
			float x0 = -w, x1 = w, y0 = middle - h, y1 = middle + h;
			poseStack.pushPose();
			poseStack.mulPose(Axis.YP.rotationDegrees(-door.getYRot()));
			Matrix4f m = poseStack.last().pose();
			// The stars within.
			VertexConsumer stars = buffers.getBuffer(RenderType.endGateway());
			quad(stars, m, x0, y0, x1, y1);
			// The frame: four bars of gold round it.
			VertexConsumer frame = buffers.getBuffer(RenderType.textBackground());
			int r = Math.round(GOLD[0] * 255), g = Math.round(GOLD[1] * 255), b = Math.round(GOLD[2] * 255);
			solid(frame, m, x0 - FRAME, y0 - FRAME, x0, y1 + FRAME, r, g, b);
			solid(frame, m, x1, y0 - FRAME, x1 + FRAME, y1 + FRAME, r, g, b);
			solid(frame, m, x0, y1, x1, y1 + FRAME, r, g, b);
			solid(frame, m, x0, y0 - FRAME, x1, y0, r, g, b);
			// Its glow, fading out from the frame.
			VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
			float a = 0.35f * shown;
			glowBand(glow, m, x0 - FRAME, y0 - FRAME, x1 + FRAME, y1 + FRAME, GLOW, a);
			poseStack.popPose();
		}
		super.render(door, entityYaw, partialTick, poseStack, buffers, light);
	}

	/** A quad of {@code c} from (x0, y0) to (x1, y1), both its faces. */
	private static void quad(VertexConsumer c, Matrix4f m, float x0, float y0, float x1, float y1) {
		c.vertex(m, x0, y0, 0).endVertex();
		c.vertex(m, x1, y0, 0).endVertex();
		c.vertex(m, x1, y1, 0).endVertex();
		c.vertex(m, x0, y1, 0).endVertex();
		c.vertex(m, x0, y1, 0).endVertex();
		c.vertex(m, x1, y1, 0).endVertex();
		c.vertex(m, x1, y0, 0).endVertex();
		c.vertex(m, x0, y0, 0).endVertex();
	}

	/** A solid bar of colour, lit of itself, both its faces. */
	private static void solid(VertexConsumer c, Matrix4f m, float x0, float y0, float x1, float y1, int r, int g, int b) {
		float[][] corners = {{x0, y0}, {x1, y0}, {x1, y1}, {x0, y1}};
		for (int i = 0; i < 4; i++) c.vertex(m, corners[i][0], corners[i][1], 0).color(r, g, b, 255).uv2(LightTexture.FULL_BRIGHT).endVertex();
		for (int i = 3; i >= 0; i--) c.vertex(m, corners[i][0], corners[i][1], 0).color(r, g, b, 255).uv2(LightTexture.FULL_BRIGHT).endVertex();
	}

	/** A band {@code width} wide round the rectangle (x0, y0)-(x1, y1), {@code alpha} at its inner edge fading to nothing, both its faces. */
	private static void glowBand(VertexConsumer c, Matrix4f m, float x0, float y0, float x1, float y1, float width, float alpha) {
		float[][] inner = {{x0, y0}, {x1, y0}, {x1, y1}, {x0, y1}};
		float[][] outer = {{x0 - width, y0 - width}, {x1 + width, y0 - width}, {x1 + width, y1 + width}, {x0 - width, y1 + width}};
		for (int i = 0; i < 4; i++) {
			int j = (i + 1) % 4;
			float[][] side = {inner[i], inner[j], outer[j], outer[i]};
			float[] alphas = {alpha, alpha, 0, 0};
			for (int k = 0; k < 4; k++) c.vertex(m, side[k][0], side[k][1], 0).color(GLOW_GOLD[0], GLOW_GOLD[1], GLOW_GOLD[2], alphas[k]).endVertex();
			for (int k = 3; k >= 0; k--) c.vertex(m, side[k][0], side[k][1], 0).color(GLOW_GOLD[0], GLOW_GOLD[1], GLOW_GOLD[2], alphas[k]).endVertex();
		}
	}

	/** Smooth in and out, 0 to 1. */
	private static float ease(float t) {
		t = Mth.clamp(t, 0f, 1f);
		return t * t * (3 - 2 * t);
	}

	@Override
	public ResourceLocation getTextureLocation(SmallWorldDoor door) {
		return TheEndPortalRenderer.END_PORTAL_LOCATION;
	}
}
