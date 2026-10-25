package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.EarthGolem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * An earth golem (EarthGolem): its GeckoLib model (geo/spell/earth_golem, made with the spells' shapes: tools/spell-art, Axis#earthGolem), a hulk
 * of packed earth and fitted stone, its molten seams and amber glowing whatever the light (its `_glowmask`: AutoGlowingGeoLayer), three times as
 * large for the Earthen Titan. It heaves itself up out of the ground as it is raised, floats bobbing, its stones wheeling round it, and pounds
 * with both fists as it strikes (its animations); its head turns to what it looks at. Under it the earth's seal turns and glows.
 */
public class EarthGolemRenderer extends GeoEntityRenderer<EarthGolem> {
	static final ResourceLocation TEXTURE = DefyingTheHeavens.id("textures/entity/spell/earth_golem.png");

	public EarthGolemRenderer(EntityRendererProvider.Context context) {
		super(context, new Model());
		this.shadowRadius = 0.8f;
		addRenderLayer(new AutoGlowingGeoLayer<>(this));
	}

	@Override
	public void render(EarthGolem golem, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
		shadowRadius = 0.8f * golem.size();
		super.render(golem, yaw, partial, pose, buffers, light);
		if (golem.isInvisible()) return;
		MultiBufferSource own = SpellBuffers.begin(buffers);
		try {
			earthLight(golem, pose, own, golem.tickCount + partial);
		} finally {
			SpellBuffers.end();
		}
	}

	@Override
	public void scaleModelForRender(float widthScale, float heightScale, PoseStack pose, EarthGolem golem, BakedGeoModel model, boolean isReRender, float partialTick,
			int packedLight, int packedOverlay) {
		float s = golem.size();
		super.scaleModelForRender(widthScale * s, heightScale * s, pose, golem, model, isReRender, partialTick, packedLight, packedOverlay);
	}

	private static final float[] WHITE = {1, 1, 1}, EARTH = {0.85f, 0.6f, 0.3f};

	/** What the earth's qi shows round a golem: the earth's seal turning on the ground under it and its glow, and dust thrown up as it heaves itself out of the ground. */
	private void earthLight(EarthGolem golem, PoseStack pose, MultiBufferSource buffers, float age) {
		float s = golem.size(), rise = SpellRender.easeOut(Math.min(1, age / 20));
		float[] seal = SpellRender.light(com.example.defyingtheheavens.QiElement.EARTH);
		SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.SEAL_EARTH)), pose.last(), 1.7f * s * rise, 0.06f, age * 0.02f, seal, 0.6f, SpellRender.FULL_BRIGHT);
		SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose.last(), 3.2f * s, 0.07f, 0, EARTH, 0.3f, SpellRender.FULL_BRIGHT);
		if (age < 30) {
			var facing = entityRenderDispatcher.cameraOrientation();
			var dust = buffers.getBuffer(SpellRender.clear(SpellRender.DUST));
			float t = age / 30;
			for (int i = 0; i < 12; i++) {
				double a = Math.PI * 2 * i / 12 + SpellRender.hash(golem.getId() + i);
				float d = s * (0.8f + 1.6f * t);
				SpellRender.billboard(dust, pose, facing, (float) Math.cos(a) * d, 0.4f + t * s, (float) Math.sin(a) * d, s * (1.2f + t * 1.5f), i, WHITE, 0.6f * (1 - t),
						0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			}
		}
	}

	/** The golem's model, texture and animations (geo/spell, textures/entity/spell, animations/spell), its head turned to its look. */
	static final class Model extends GeoModel<EarthGolem> {
		@Override
		public ResourceLocation getModelResource(EarthGolem golem) {
			return DefyingTheHeavens.id("geo/spell/earth_golem.geo.json");
		}

		@Override
		public ResourceLocation getTextureResource(EarthGolem golem) {
			return TEXTURE;
		}

		@Override
		public ResourceLocation getAnimationResource(EarthGolem golem) {
			return DefyingTheHeavens.id("animations/spell/earth_golem.animation.json");
		}

		/** Its head turned to what it looks at, over what its animations make of it (they key its head every frame: its float does). */
		@Override
		public void setCustomAnimations(EarthGolem golem, long instanceId, AnimationState<EarthGolem> state) {
			CoreGeoBone head = getAnimationProcessor().getBone("head");
			EntityModelData look = state.getData(DataTickets.ENTITY_MODEL_DATA);
			if (head == null || look == null) return;
			head.setRotX(head.getRotX() + Mth.clamp(look.headPitch(), -30, 30) * Mth.DEG_TO_RAD);
			head.setRotY(head.getRotY() + Mth.clamp(look.netHeadYaw(), -45, 45) * Mth.DEG_TO_RAD);
		}
	}
}
