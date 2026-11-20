package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.CircuitBoard;
import com.example.defyingtheheavens.PuppetFrameItem;
import com.example.defyingtheheavens.PuppetWorkbenchBlock;
import com.example.defyingtheheavens.PuppetWorkbenchBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * What lies on a Puppet Workbench (PuppetWorkbenchBlockEntity): the frame laid flat on its top, the demon core set in its middle, turning
 * slowly a little above it; and, for one with Qi Sense on, the circuits painted on the frame glowing over the bench's board.
 */
public class PuppetWorkbenchRenderer implements BlockEntityRenderer<PuppetWorkbenchBlockEntity> {
	/** The bench's top, and the board inlaid in it (models/block/puppet_workbench: 14 px wide). */
	private static final float TOP = 14 / 16f, BOARD = 13 / 16f;

	public PuppetWorkbenchRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(PuppetWorkbenchBlockEntity bench, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		Level level = bench.getLevel();
		if (level == null || bench.frame().isEmpty()) return;
		int above = LevelRenderer.getLightColor(level, bench.getBlockPos().above());
		Direction facing = bench.getBlockState().hasProperty(PuppetWorkbenchBlock.FACING) ? bench.getBlockState().getValue(PuppetWorkbenchBlock.FACING) : Direction.NORTH;
		// The frame, lying flat, its head away from whoever faces the bench.
		pose.pushPose();
		pose.translate(0.5, TOP + 0.01, 0.5);
		pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
		pose.mulPose(Axis.XP.rotationDegrees(90));
		pose.scale(0.8f, 0.8f, 0.8f);
		Minecraft.getInstance().getItemRenderer().renderStatic(bench.frame(), ItemDisplayContext.FIXED, above, OverlayTexture.NO_OVERLAY, pose, buffers,
				level, (int) bench.getBlockPos().asLong());
		pose.popPose();
		ItemStack core = bench.core();
		if (!core.isEmpty()) {
			float clock = level.getGameTime() + partialTick;
			pose.pushPose();
			pose.translate(0.5, TOP + 0.18 + 0.03 * Mth.sin(clock * 0.1f), 0.5);
			pose.mulPose(Axis.YP.rotationDegrees(clock * 2));
			pose.scale(0.4f, 0.4f, 0.4f);
			Minecraft.getInstance().getItemRenderer().renderStatic(core, ItemDisplayContext.FIXED, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
					pose, buffers, level, (int) bench.getBlockPos().asLong() + 1);
			pose.popPose();
		}
		// Its circuits, to one with Qi Sense on.
		if (!QiSenseClientHandler.isActive() || !(bench.frame().getItem() instanceof PuppetFrameItem item)) return;
		byte[] strokes = PuppetFrameItem.strokes(bench.frame());
		if (strokes == null) return;
		int size = CircuitBoard.sizeFor(item.grade().ordinal());
		pose.pushPose();
		pose.translate(0.5, 0, 0.5);
		pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
		float glow = 0.75f + 0.25f * Mth.sin((level.getGameTime() + partialTick) * 0.12f);
		double h = BOARD / 2;
		// Just over the frame lying there (its sprite stands a little off the top), so nothing of it hides them.
		CircuitGlow.draw(QiSenseTreasures.glow(buffers), pose.last().pose(), strokes, size, item.blueprint(), new Vec3(-h, TOP + 0.06, -h),
				new Vec3(BOARD, 0, 0), new Vec3(0, 0, BOARD), new Vec3(0, 1, 0), glow);
		pose.popPose();
	}

	@Override
	public int getViewDistance() {
		return 48;
	}
}
