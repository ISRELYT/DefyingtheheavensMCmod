package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.LegendStoneBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The sword resting in a legend's stone (LegendStoneBlockEntity): standing point first in its top, a quarter of a block deep, its hilt high
 * (Ganjiang in its tomb, Moye in her pool). Lit by the air over the stone.
 */
public class LegendStoneRenderer implements BlockEntityRenderer<LegendStoneBlockEntity> {
	/** The stone's top (13 pixels), how deep the point is sunk in it, and the sword's size beside a held one's. */
	static final float TOP = 13 / 16f, SUNK = 0.25f, SCALE = 1.15f;

	private final ItemRenderer items;

	public LegendStoneRenderer(BlockEntityRendererProvider.Context context) {
		items = context.getItemRenderer();
	}

	@Override
	public void render(LegendStoneBlockEntity stone, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		ItemStack sword = stone.held();
		Level level = stone.getLevel();
		if (sword.isEmpty() || level == null) return;
		// A held sword's model lies on the diagonal of its square, its length some 1.41 of it: half that, scaled, from its middle to its point.
		float half = 0.707f * SCALE;
		poseStack.pushPose();
		poseStack.translate(0.5, TOP - SUNK + half, 0.5);
		poseStack.mulPose(Axis.YP.rotationDegrees(stone.getBlockPos().hashCode() % 4 * 45)); // each stone's sword turned its own way
		// Its point, at the upper right of its square, straight down: its model's "fixed" view turns it half round first (its point then at the
		// upper left), so a quarter and a half more this way. At -135 (until 3b's tour, 2026-10-28) it lay flat across the stone's top.
		poseStack.mulPose(Axis.ZP.rotationDegrees(135));
		poseStack.scale(SCALE, SCALE, SCALE);
		items.renderStatic(sword, ItemDisplayContext.FIXED, LevelRenderer.getLightColor(level, stone.getBlockPos().above()), OverlayTexture.NO_OVERLAY,
				poseStack, buffers, level, (int) stone.getBlockPos().asLong());
		poseStack.popPose();
	}
}
