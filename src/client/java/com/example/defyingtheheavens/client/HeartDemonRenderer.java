package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.HeartDemonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** A heart demon: its owner, in their own skin, but shadow-dark and half see-through, trailing smoke. */
public class HeartDemonRenderer extends HumanoidMobRenderer<HeartDemonEntity, PlayerModel<HeartDemonEntity>> {
	public HeartDemonRenderer(EntityRendererProvider.Context context) {
		super(context, new ShadowModel(context.bakeLayer(ModelLayers.PLAYER)), 0.0f);
	}

	@Override
	public ResourceLocation getTextureLocation(HeartDemonEntity demon) {
		return InnerBodyRenderer.skinOf(demon.getOwnerId());
	}

	@Override
	protected RenderType getRenderType(HeartDemonEntity demon, boolean bodyVisible, boolean translucent, boolean glowing) {
		return RenderType.entityTranslucent(getTextureLocation(demon));
	}

	/** The player model, drawn dark and faintly violet. */
	private static final class ShadowModel extends PlayerModel<HeartDemonEntity> {
		ShadowModel(ModelPart root) {
			super(root, false);
		}

		@Override
		public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, float r, float g, float b, float a) {
			super.renderToBuffer(pose, consumer, light, overlay, r * 0.16f, g * 0.1f, b * 0.22f, a * 0.82f);
		}
	}
}
