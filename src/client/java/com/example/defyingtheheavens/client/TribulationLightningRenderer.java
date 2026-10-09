package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.TribulationCloud;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import net.minecraft.world.entity.LightningBolt;

/**
 * Heavenly Tribulation lightning: vanilla's bolt shape, recoloured bright blue. Vanilla draws its layered quads in
 * (0.45, 0.45, 0.5) with additive blending; the same layers in this blue build up to a glowing blue core.
 */
public class TribulationLightningRenderer extends LightningBoltRenderer {
	private static final int RED = 38;
	private static final int GREEN = 115;
	private static final int BLUE = 255;

	public TribulationLightningRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(LightningBolt bolt, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
		poseStack.pushPose();
		// Vanilla's 128-block bolt now runs between the player and their local cloud bank.
		float height = (float) (TribulationCloud.cloudBaseY(bolt.level(), bolt.getY()) - bolt.getY());
		poseStack.scale(1.0f, height / 128.0f, 1.0f);
		super.render(bolt, entityYaw, partialTick, poseStack, type -> new Tinted(buffers.getBuffer(type)), packedLight);
		poseStack.popPose();
	}

	/** Passes every vertex through unchanged except its colour (vanilla's alpha is kept). */
	private record Tinted(VertexConsumer parent) implements VertexConsumer {
		@Override
		public VertexConsumer vertex(double x, double y, double z) {
			parent.vertex(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			parent.color(RED, GREEN, BLUE, alpha);
			return this;
		}

		@Override
		public VertexConsumer uv(float u, float v) {
			parent.uv(u, v);
			return this;
		}

		@Override
		public VertexConsumer overlayCoords(int u, int v) {
			parent.overlayCoords(u, v);
			return this;
		}

		@Override
		public VertexConsumer uv2(int u, int v) {
			parent.uv2(u, v);
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			parent.normal(x, y, z);
			return this;
		}

		@Override
		public void endVertex() {
			parent.endVertex();
		}

		@Override
		public void defaultColor(int red, int green, int blue, int alpha) {
			parent.defaultColor(red, green, blue, alpha);
		}

		@Override
		public void unsetDefaultColor() {
			parent.unsetDefaultColor();
		}
	}
}
