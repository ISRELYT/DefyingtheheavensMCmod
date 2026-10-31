package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ArmySpirit;
import com.example.defyingtheheavens.DefyingTheHeavens;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * A soldier of the old army the War General's Banner calls up (ArmySpirit): the Ancient Battlefield's Battle Spirit's shape, seen through as
 * that one is, but painted in the warm gold of the banner's side, not the field's pale blue-green (geo/realm/army_spirit: tools/spell-art,
 * Realms#armySpirit; a tint alone was lost under its glow); it comes out of the air over half a second and goes back into it over its last.
 */
public class ArmySpiritRenderer extends GeoEntityRenderer<ArmySpirit> {
	public ArmySpiritRenderer(EntityRendererProvider.Context context) {
		super(context, new Model());
		this.shadowRadius = 0.3f;
		addRenderLayer(new AutoGlowingGeoLayer<>(this));
	}

	@Override
	public RenderType getRenderType(ArmySpirit spirit, ResourceLocation texture, @Nullable MultiBufferSource buffers, float partialTick) {
		return RenderType.entityTranslucent(texture);
	}

	@Override
	public Color getRenderColor(ArmySpirit spirit, float partialTick, int packedLight) {
		float age = spirit.tickCount + partialTick;
		float alpha = Mth.clamp(Math.min(age / 10, (ArmySpirit.LIFE - age) / 20), 0.0f, 1.0f);
		return Color.ofRGBA(1.0f, 1.0f, 1.0f, alpha);
	}

	/** Its model, texture and animations (the Battle Spirit's, in gold). */
	private static final class Model extends GeoModel<ArmySpirit> {
		private static final ResourceLocation GEO = DefyingTheHeavens.id("geo/realm/army_spirit.geo.json"),
				TEXTURE = DefyingTheHeavens.id("textures/entity/realm/army_spirit.png"),
				ANIMATIONS = DefyingTheHeavens.id("animations/realm/army_spirit.animation.json");

		@Override
		public ResourceLocation getModelResource(ArmySpirit spirit) { return GEO; }

		@Override
		public ResourceLocation getTextureResource(ArmySpirit spirit) { return TEXTURE; }

		@Override
		public ResourceLocation getAnimationResource(ArmySpirit spirit) { return ANIMATIONS; }
	}
}
