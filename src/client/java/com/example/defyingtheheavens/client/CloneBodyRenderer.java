package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.CloneBodyEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * The body of a cultivator with a clone out that isn't being played (Clones): its owner in their own skin, wearing and holding what that
 * body does; the clone standing, their name over it, or the main body sitting cross-legged in meditation (LotusPose).
 */
public class CloneBodyRenderer extends LivingEntityRenderer<CloneBodyEntity, PlayerModel<CloneBodyEntity>> {
	public CloneBodyRenderer(EntityRendererProvider.Context context) {
		super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
		addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
				new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
		addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
	}

	@Override
	public ResourceLocation getTextureLocation(CloneBodyEntity body) {
		return InnerBodyRenderer.skinOf(body.getOwnerId());
	}

	@Override
	protected boolean shouldShowName(CloneBodyEntity body) {
		return body.shouldShowName();
	}
}
