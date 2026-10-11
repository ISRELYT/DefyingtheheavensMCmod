package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.SoulBodyEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * A body left behind by its nascent soul: its owner in their own skin and what they wear, sitting cross-legged in meditation
 * (see HumanoidModelMixin), as the body of a soul in the Inner Realm does.
 */
public class SoulBodyRenderer extends LivingEntityRenderer<SoulBodyEntity, PlayerModel<SoulBodyEntity>> {
	public SoulBodyRenderer(EntityRendererProvider.Context context) {
		super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
		addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
				new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
	}

	@Override
	public ResourceLocation getTextureLocation(SoulBodyEntity body) {
		return InnerBodyRenderer.skinOf(body.getOwnerId());
	}

	@Override
	protected boolean shouldShowName(SoulBodyEntity body) {
		return false;
	}
}
