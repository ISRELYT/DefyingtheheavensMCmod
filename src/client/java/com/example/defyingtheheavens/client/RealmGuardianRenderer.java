package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.RealmGuardian;
import com.mojang.blaze3d.vertex.PoseStack;
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
 * A secret realm's guardian (RealmGuardian): its GeckoLib model (geo/realm/{@code id}, made with the spells' shapes: tools/spell-art,
 * Realms), made of its realm's matter, the Vine Keeper of old wood and living vines, the Bronze Puppet of green-rusted bronze; their glowing
 * parts whatever the light (its `_glowmask`). Asleep, curled or still (its "dormant" loop); awake, its head turns to what it looks at. The
 * Gatekeeper is a puppet a quarter as large again, the Sleeping Sword's spirit a wraith as much larger. The second phase's: the Sword Wraith,
 * the Bone Soldier; the Battle Spirit and the General's Spirit drawn see-through (their light), the General larger.
 */
public class RealmGuardianRenderer extends GeoEntityRenderer<RealmGuardian> {
	private final float scale;
	private final boolean ghostly;

	public RealmGuardianRenderer(EntityRendererProvider.Context context, String id, float shadow) {
		this(context, id, shadow, 1.0f, false);
	}

	/** Drawn {@code scale} times its model's size; a spirit ({@code ghostly}) seen through where its texture is. */
	public RealmGuardianRenderer(EntityRendererProvider.Context context, String id, float shadow, float scale, boolean ghostly) {
		super(context, new Model(id));
		this.shadowRadius = shadow;
		this.scale = scale;
		this.ghostly = ghostly;
		addRenderLayer(new AutoGlowingGeoLayer<>(this));
	}

	@Override
	public void scaleModelForRender(float widthScale, float heightScale, PoseStack pose, RealmGuardian guardian, BakedGeoModel model, boolean isReRender,
			float partialTick, int packedLight, int packedOverlay) {
		float s = (guardian.isGatekeeper() ? 1.25f : 1.0f) * scale;
		super.scaleModelForRender(widthScale * s, heightScale * s, pose, guardian, model, isReRender, partialTick, packedLight, packedOverlay);
	}

	@Override
	public net.minecraft.client.renderer.RenderType getRenderType(RealmGuardian guardian, ResourceLocation texture,
			@org.jetbrains.annotations.Nullable net.minecraft.client.renderer.MultiBufferSource buffers, float partialTick) {
		return ghostly ? net.minecraft.client.renderer.RenderType.entityTranslucent(texture) : super.getRenderType(guardian, texture, buffers, partialTick);
	}

	/** Its model, texture and animations (geo/realm, textures/entity/realm, animations/realm), its head turned to its look while awake. */
	static final class Model extends GeoModel<RealmGuardian> {
		private final ResourceLocation geo, texture, animations;

		Model(String id) {
			geo = DefyingTheHeavens.id("geo/realm/" + id + ".geo.json");
			texture = DefyingTheHeavens.id("textures/entity/realm/" + id + ".png");
			animations = DefyingTheHeavens.id("animations/realm/" + id + ".animation.json");
		}

		@Override
		public ResourceLocation getModelResource(RealmGuardian guardian) { return geo; }

		@Override
		public ResourceLocation getTextureResource(RealmGuardian guardian) { return texture; }

		@Override
		public ResourceLocation getAnimationResource(RealmGuardian guardian) { return animations; }

		/** The head bone the look last turned, and by how much (radians about x and y), to be taken off before the next frame's. */
		private CoreGeoBone looked;
		private float lookX, lookY;
		private boolean customised;

		/**
		 * Last frame's look taken off the head before GeckoLib sets anything (as BeastModels does): a bone no playing animation keys (an
		 * awake guardian's head in its idle and walk) keeps what was set last frame, so a look added to it piled up frame on frame and the
		 * head spun (found on the puppets, 2026-11-17, which turn their heads the same way). When GeckoLib draws the same frame again and
		 * sets nothing, the look goes back on as it was.
		 */
		@Override
		public void handleAnimations(RealmGuardian guardian, long instanceId, AnimationState<RealmGuardian> state) {
			CoreGeoBone last = looked;
			float lastX = lookX, lastY = lookY;
			if (last != null) {
				last.setRotX(last.getRotX() - lastX);
				last.setRotY(last.getRotY() - lastY);
			}
			looked = null;
			lookX = lookY = 0;
			customised = false;
			super.handleAnimations(guardian, instanceId, state);
			if (customised || last == null) return;
			last.setRotX(last.getRotX() + lastX);
			last.setRotY(last.getRotY() + lastY);
			looked = last;
			lookX = lastX;
			lookY = lastY;
		}

		@Override
		public void setCustomAnimations(RealmGuardian guardian, long instanceId, AnimationState<RealmGuardian> state) {
			customised = true;
			if (guardian.isDormant()) return;
			CoreGeoBone head = getAnimationProcessor().getBone("head");
			EntityModelData look = state.getData(DataTickets.ENTITY_MODEL_DATA);
			if (head == null || look == null) return;
			float x = Mth.clamp(look.headPitch(), -30, 30) * Mth.DEG_TO_RAD, y = Mth.clamp(Mth.wrapDegrees(look.netHeadYaw()), -50, 50) * Mth.DEG_TO_RAD;
			head.setRotX(head.getRotX() + x);
			head.setRotY(head.getRotY() + y);
			looked = head;
			lookX = x;
			lookY = y;
		}
	}
}
