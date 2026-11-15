package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.HeartDemonEntity;
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
 * drawn (LivingEntityRendererMixin). Not on one invisible. Drawn as the body is: a heart demon's shadow-dark and half see-through, as
 * its body (HeartDemonRenderer#SHADOW).
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
		int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0f);
		if (entity instanceof HeartDemonEntity) {
			float[] shadow = HeartDemonRenderer.SHADOW;
			beast.render(pose, buffers.getBuffer(RenderType.entityTranslucent(beast.texture())), light, overlay, shadow[0], shadow[1], shadow[2], shadow[3]);
			return;
		}
		// A body of the dead walked in (CorpseWalk), fresh or rotting: its beast's head and tail rot with it, in its hue (ClientBodies#tint).
		float[] tint = entity instanceof net.minecraft.world.entity.player.Player player ? ClientBodies.tint(player) : null;
		float r = tint == null ? 1.0f : tint[0], g = tint == null ? 1.0f : tint[1], b = tint == null ? 1.0f : tint[2];
		beast.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(beast.texture())), light, overlay, r, g, b, 1.0f);
	}
}
