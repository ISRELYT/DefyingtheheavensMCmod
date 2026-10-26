package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.SmallWorldGateBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import org.joml.Matrix4f;

/**
 * A small world's gate (SmallWorldGateBlock): a sheet of the void's stars (the end gateway's) through the middle of its block, across it
 * (SmallWorlds lays its gates across X), both faces drawn; its blocks side by side one sheet.
 */
public class SmallWorldGateRenderer implements BlockEntityRenderer<SmallWorldGateBlockEntity> {
	@Override
	public void render(SmallWorldGateBlockEntity gate, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		Matrix4f m = poseStack.last().pose();
		VertexConsumer c = buffers.getBuffer(RenderType.endGateway());
		c.vertex(m, 0, 0, 0.5f).endVertex();
		c.vertex(m, 1, 0, 0.5f).endVertex();
		c.vertex(m, 1, 1, 0.5f).endVertex();
		c.vertex(m, 0, 1, 0.5f).endVertex();
		c.vertex(m, 0, 1, 0.5f).endVertex();
		c.vertex(m, 1, 1, 0.5f).endVertex();
		c.vertex(m, 1, 0, 0.5f).endVertex();
		c.vertex(m, 0, 0, 0.5f).endVertex();
	}
}
