package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.FruitAura;
import com.example.defyingtheheavens.GinsengBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** The glow of growing ginseng's age aura: the same as a Cultivation Fruit's of the same age, centred on the bush. */
public class GinsengAuraRenderer implements BlockEntityRenderer<GinsengBlockEntity> {
	@Override
	public void render(GinsengBlockEntity ginseng, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		if (ginseng.getLevel() == null) return;
		BlockPos pos = ginseng.getBlockPos();
		poseStack.pushPose();
		poseStack.translate(0.5, FruitAura.HERB_CENTER_Y, 0.5);
		CultivationFruitAuraRenderer.drawAura(ginseng.age(), pos, Vec3.atLowerCornerOf(pos).add(0.5, FruitAura.HERB_CENTER_Y, 0.5),
				FruitAura.clock(ginseng.getLevel().getGameTime(), pos) + partialTick, poseStack, buffers);
		poseStack.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen(GinsengBlockEntity ginseng) {
		return true; // halos and beams reach beyond the block
	}

	@Override
	public int getViewDistance() {
		return 256; // a 10,000-year ginseng's pillar of light is seen from afar
	}
}
