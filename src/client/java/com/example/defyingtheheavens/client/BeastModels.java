package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.BeastKind;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.DemonBeast;
import com.example.defyingtheheavens.DemonBloodItem;
import com.example.defyingtheheavens.DemonCoreItem;
import com.example.defyingtheheavens.ModEntities;
import com.example.defyingtheheavens.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
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
 * The wild demon beasts' looks (DemonBeast, BeastKind): each kind's own GeckoLib model, texture and glow (tools/beast-art's Bodies), drawn
 * the larger the higher its rank (DemonBeast#scale), moved in code by its build ({@link Model#setCustomAnimations}): legs in their gait,
 * the head turned to what it looks at, a tail swaying, a serpent's body winding, a bird's wings folded or beating, an ape's arms swinging
 * and striking; and its acts (a turtle drawn into its shell, a charge's lowered head, a coil's curled body, fire breathed with open
 * jaws). Its "idle" animation (the file's) breathes and blinks. Also the demon cores' look (DemonCoreItem#look, its glow's element).
 */
public final class BeastModels {
	private BeastModels() {}

	public static void register() {
		for (BeastKind kind : BeastKind.values()) EntityRendererRegistry.register(ModEntities.beast(kind), context -> new Renderer(context, kind));
		EntityRendererRegistry.register(ModEntities.BEAST_SHOT, ThrownItemRenderer::new);
		// A demon core looks its realm ("defying-the-heavens:realm" picks models/item/demon_core_*), its light (layer 1) its element.
		ItemProperties.register(ModItems.DEMON_CORE, DefyingTheHeavens.id("realm"),
				(stack, level, entity, seed) -> DemonCoreItem.look(stack) / (float) (DemonCoreItem.LOOKS - 1));
		ColorProviderRegistry.ITEM.register((stack, tint) -> tint == 1 ? DemonCoreItem.glow(stack) : -1, ModItems.DEMON_CORE);
		// A race's blood likewise: its phial by its realm (models/item/demon_blood_*), its blood (layer 1) its race's hue.
		ItemProperties.register(ModItems.DEMON_BLOOD, DefyingTheHeavens.id("realm"),
				(stack, level, entity, seed) -> DemonBloodItem.look(stack) / (float) (DemonBloodItem.LOOKS - 1));
		ColorProviderRegistry.ITEM.register((stack, tint) -> tint == 1 ? DemonBloodItem.tint(stack) : -1, ModItems.DEMON_BLOOD);
	}

	/** Draws a beast of {@code kind}: its model grown by its rank, its shadow with it, what of it glows glowing in the dark. */
	static final class Renderer extends GeoEntityRenderer<DemonBeast> {
		private final float shadow;

		Renderer(EntityRendererProvider.Context context, BeastKind kind) {
			super(context, new Model(kind));
			this.shadow = Math.min(1.6f, kind.width * 0.55f);
			this.shadowRadius = shadow;
			addRenderLayer(new AutoGlowingGeoLayer<>(this));
		}

		@Override
		public void render(DemonBeast beast, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
			shadowRadius = shadow * beast.scale();
			super.render(beast, yaw, partialTick, pose, buffers, light);
		}

		@Override
		public void scaleModelForRender(float widthScale, float heightScale, PoseStack pose, DemonBeast beast, BakedGeoModel model, boolean isReRender,
				float partialTick, int packedLight, int packedOverlay) {
			float grown = beast.scale();
			super.scaleModelForRender(widthScale * grown, heightScale * grown, pose, beast, model, isReRender, partialTick, packedLight, packedOverlay);
		}
	}

	/** A kind's model: its geometry, texture and "idle" animation, and its moving in code. */
	static final class Model extends GeoModel<DemonBeast> {
		private static final String[] LEGS = {"leg_fl", "leg_fr", "leg_bl", "leg_br"};

		private final BeastKind kind;
		private final ResourceLocation geometry, texture, animations;

		Model(BeastKind kind) {
			this.kind = kind;
			this.geometry = DefyingTheHeavens.id("geo/demon_beast/" + kind.getId() + ".geo.json");
			this.texture = DefyingTheHeavens.id("textures/entity/demon_beast/" + kind.getId() + ".png");
			this.animations = DefyingTheHeavens.id("animations/demon_beast/" + kind.getId() + ".animation.json");
		}

		@Override public ResourceLocation getModelResource(DemonBeast beast) { return geometry; }
		@Override public ResourceLocation getTextureResource(DemonBeast beast) { return texture; }
		@Override public ResourceLocation getAnimationResource(DemonBeast beast) { return animations; }

		@Override
		public void setCustomAnimations(DemonBeast beast, long instanceId, AnimationState<DemonBeast> state) {
			float partial = state.getPartialTick();
			float time = beast.tickCount + partial;
			float stride = state.getLimbSwing(), walk = Math.min(1, state.getLimbSwingAmount());
			EntityModelData look = state.getData(DataTickets.ENTITY_MODEL_DATA);
			float yaw = look == null ? 0 : Mth.clamp(look.netHeadYaw(), -50, 50) * Mth.DEG_TO_RAD;
			float pitch = look == null ? 0 : Mth.clamp(look.headPitch(), -40, 40) * Mth.DEG_TO_RAD;
			float strike = Mth.sin(beast.getAttackAnim(partial) * Mth.PI);
			boolean shell = beast.acting(DemonBeast.ACT_SHELL), charge = beast.acting(DemonBeast.ACT_CHARGE);
			boolean coil = beast.acting(DemonBeast.ACT_COIL), breath = beast.acting(DemonBeast.ACT_BREATH);

			// The head: turned to what it looks at; lowered in a charge; the jaw dropping as it strikes, breathes fire or coils.
			turn("head", pitch - (charge ? 0.45f : 0), yaw, 0);
			float open = Math.max(strike * 0.55f, breath ? 0.5f + Mth.sin(time * 0.9f) * 0.08f : coil ? 0.35f : 0);
			turn("jaw", -open, 0, 0);

			switch (kind.body) {
				case QUADRUPED, TURTLE, CROCODILE -> {
					float reach = kind.body == BeastKind.Body.QUADRUPED ? 1.1f : 0.7f;
					float gait = Mth.cos(stride * 0.6662f) * reach * walk;
					turn(LEGS[0], gait, 0, 0);
					turn(LEGS[3], gait, 0, 0);
					turn(LEGS[1], -gait, 0, 0);
					turn(LEGS[2], -gait, 0, 0);
					if (kind.signature == BeastKind.Signature.POUND) turn("head", pitch - strike * 0.5f, yaw, 0); // the bear's head driven down with its blow
				}
				case APE -> {
					float gait = Mth.cos(stride * 0.6662f) * 0.9f * walk;
					turn("leg_l", gait, 0, 0);
					turn("leg_r", -gait, 0, 0);
					// Its arms swing as it goes; it throws with its right, and slams with both.
					boolean both = kind.signature == BeastKind.Signature.ICE_SLAM;
					turn("arm_l", -gait + (both ? strike * 2.2f : 0), 0, 0);
					turn("arm_r", gait + strike * 2.2f, 0, 0);
				}
				case BIRD -> {
					boolean aloft = !beast.onGround() && !beast.isInWater();
					if (aloft) {
						float beat = kind.width > 1.5f ? 0.32f : 0.55f;
						float flap = Mth.sin(time * beat) * 0.65f;
						turn("wing_l", 0, 0, flap);
						turn("wing_r", 0, 0, -flap);
						turn("wing_l_tip", 0, 0, flap * 0.5f);
						turn("wing_r_tip", 0, 0, -flap * 0.5f);
						turn("leg_l", 0.9f, 0, 0);
						turn("leg_r", 0.9f, 0, 0);
					} else {
						// Folded along its back: swept back, dropped a little, the tips folded in over them.
						turn("wing_l", 0, -1.35f, -0.25f);
						turn("wing_r", 0, 1.35f, 0.25f);
						turn("wing_l_tip", 0, -2.6f, 0);
						turn("wing_r_tip", 0, 2.6f, 0);
						float gait = Mth.cos(stride * 0.6662f) * 0.8f * walk;
						turn("leg_l", gait, 0, 0);
						turn("leg_r", -gait, 0, 0);
					}
				}
				case SERPENT, DRAGON -> {
					// Winding: each piece turned from the one before, the wave running back along it, the quicker the faster it goes; coiled
					// round its prey, every piece turned alike, into a ring.
					float pace = 0.08f + walk * 0.35f, sway = 0.2f + walk * 0.12f;
					for (int i = 0; ; i++) {
						CoreGeoBone seg = getAnimationProcessor().getBone("seg_" + i);
						if (seg == null) break;
						float bend = coil ? 0.55f : Mth.sin(time * pace - i * 0.8f) * sway;
						turn(seg, 0, bend, 0);
					}
					if (kind.body == BeastKind.Body.DRAGON) {
						float paddle = Mth.sin(time * 0.25f) * 0.6f;
						turn(LEGS[0], paddle, 0, 0);
						turn(LEGS[3], paddle, 0, 0);
						turn(LEGS[1], -paddle, 0, 0);
						turn(LEGS[2], -paddle, 0, 0);
						turn("whisker_l", Mth.sin(time * 0.07f) * 0.1f, Mth.sin(time * 0.09f) * 0.15f, 0);
						turn("whisker_r", -Mth.sin(time * 0.07f) * 0.1f, -Mth.sin(time * 0.09f) * 0.15f, 0);
					}
				}
			}

			// A tail: swaying slowly standing, swinging with the stride walking; a fox's three fanned behind it.
			for (int i = 0; ; i++) {
				CoreGeoBone piece = getAnimationProcessor().getBone("tail_" + i);
				if (piece == null) break;
				float sway = Mth.sin(time * 0.09f - i * 0.6f) * 0.14f * (1 - walk) + Mth.sin(stride * 0.6662f - i * 0.6f) * 0.3f * walk;
				turn(piece, Mth.sin(time * 0.06f - i * 0.5f) * 0.04f, sway, 0);
			}
			for (int i = 0; i < 3; i++) {
				CoreGeoBone fan = getAnimationProcessor().getBone("fan_" + i);
				if (fan != null) turn(fan, Mth.sin(time * 0.07f + i) * 0.06f, Mth.sin(time * 0.1f + i * 1.7f) * 0.14f, 0);
			}

			// Drawn into its shell: nothing of it shows but the shell.
			for (String part : new String[] {"head", "tail_0", LEGS[0], LEGS[1], LEGS[2], LEGS[3]}) {
				if (kind.body != BeastKind.Body.TURTLE) break;
				CoreGeoBone bone = getAnimationProcessor().getBone(part);
				if (bone != null) bone.setHidden(shell);
			}
		}

		private void turn(String name, float x, float y, float z) {
			CoreGeoBone bone = getAnimationProcessor().getBone(name);
			if (bone != null) turn(bone, x, y, z);
		}

		/** {@code bone} turned this much (radians) from where the model has it. */
		private static void turn(CoreGeoBone bone, float x, float y, float z) {
			bone.setRotX(bone.getInitialSnapshot().getRotX() + x);
			bone.setRotY(bone.getInitialSnapshot().getRotY() + y);
			bone.setRotZ(bone.getInitialSnapshot().getRotZ() + z);
		}
	}
}
