package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.Formation;
import com.example.defyingtheheavens.FormationCoreBlockEntity;
import com.example.defyingtheheavens.QiElement;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * The Formation Core's crystal, hovering over its altar: it turns slowly and bobs while the core sleeps; while its barrier
 * stands it spins faster, burns at full brightness, and (to Qi Sense) breathes azure qi up into the air.
 */
public class FormationCoreRenderer implements BlockEntityRenderer<FormationCoreBlockEntity> {
	public static final ResourceLocation CRYSTAL = DefyingTheHeavens.id("block/formation_core_crystal");
	private static final RandomSource RANDOM = RandomSource.create();
	private static final java.util.Map<Long, Long> EMITTED = new java.util.HashMap<>();

	public static void register() {
		ModelLoadingPlugin.register(context -> context.addModels(CRYSTAL));
	}

	public FormationCoreRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(FormationCoreBlockEntity core, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		Level level = core.getLevel();
		if (level == null) return;
		BlockPos pos = core.getBlockPos();
		boolean raised = isRaised(pos);
		float time = level.getGameTime() + partialTick + (pos.hashCode() & 255);
		float bob = Mth.sin(time * 0.06f) * 0.035f;
		BakedModel crystal = Minecraft.getInstance().getModelManager().getModel(CRYSTAL);
		poseStack.pushPose();
		poseStack.translate(0.5, 12.5 / 16.0 + bob, 0.5);
		poseStack.mulPose(Axis.YP.rotationDegrees(time * (raised ? 2.4f : 0.6f)));
		poseStack.translate(-0.5, 0, -0.5);
		Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(), buffers.getBuffer(RenderType.cutout()), null,
				crystal, 1.0f, 1.0f, 1.0f, raised ? LightTexture.FULL_BRIGHT : light, overlay);
		poseStack.popPose();
		// Once a tick at most (this runs every frame), a mote of qi rising off the crystal.
		long tick = level.getGameTime();
		Long last = EMITTED.put(pos.asLong(), tick);
		if (raised && (last == null || last != tick) && QiSenseClientHandler.isActive() && RANDOM.nextInt(2) == 0) {
			double y = pos.getY() + 1.2 + bob;
			QiSenseClientHandler.emit(QiElement.WATER, pos.getX() + 0.5 + (RANDOM.nextDouble() - 0.5) * 0.3, y, pos.getZ() + 0.5 + (RANDOM.nextDouble() - 0.5) * 0.3,
					(RANDOM.nextDouble() - 0.5) * 0.01, 0.03 + RANDOM.nextDouble() * 0.02, (RANDOM.nextDouble() - 0.5) * 0.01,
					0.06f + RANDOM.nextFloat() * 0.03f, 50 + RANDOM.nextInt(30), null);
		}
	}

	/** The server has told us this core's barrier stands. */
	private static boolean isRaised(BlockPos pos) {
		Formation formation = ClientFormations.centredAt(pos);
		return formation != null;
	}

	@Override
	public int getViewDistance() {
		return 96;
	}
}
