package com.example.defyingtheheavens.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * A demon's beast's head and tail on its body (ClientBloodlines, BeastHeadModels): on a player, and on the bodies drawn as one (a
 * body its soul left, a clone, a meditator's, a heart demon, a soul vessel's likeness). Its man's head is taken off while the body is
 * drawn (LivingEntityRendererMixin). Not on one invisible.
 */
public class BeastLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
	public BeastLayer(RenderLayerParent<T, M> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount, float partialTick,
					   float ageInTicks, float netHeadYaw, float headPitch) {
		if (entity.isInvisible()) return;
		BeastHeadModels.Head beast = BeastHeadModels.of(entity);
		if (beast == null) return;
		beast.follow(getParentModel());
		beast.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(beast.texture())), light,
				LivingEntityRenderer.getOverlayCoords(entity, 0.0f), 1.0f, 1.0f, 1.0f, 1.0f);
	}
}
