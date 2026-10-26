package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.BeastKind;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.DemonBeast;
import com.example.defyingtheheavens.DemonBloodItem;
import com.example.defyingtheheavens.DemonCoreItem;
import com.example.defyingtheheavens.ModEntities;
import com.example.defyingtheheavens.ModItems;
import com.example.defyingtheheavens.PrimordialBeast;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
 * the head turned to what it looks at, a tail swaying (a fox's fanned tails, three or nine), a serpent's body winding, a bird's wings folded or beating, an ape's arms swinging
 * and striking; and its acts (a turtle drawn into its shell, a charge's lowered head, a coil's curled body, fire breathed with open
 * jaws). Its "idle" animation (the file's) breathes and blinks. Two of the primordial beasts are tools/spell-art's instead (the Kun, the
 * spell's own; the True Dragon): seen through, moved by their own "swim", their look and jaws on top ({@link Model#spellMade}). Also the
 * demon cores' look (DemonCoreItem#look, its glow's element).
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

	/** The True Phoenix as an ember: drawn this much its size, its wings folded. */
	static final float EMBER_SCALE = 0.35f;
	/**
	 * How far back (model pixels) tools/spell-art's models are drawn from where the beast is: the Kun's model has its middle there, its head
	 * well ahead; the Dragon's the back of its head, its body trailing. So its head and forequarters are where its bounding box is, where it
	 * bites and is struck.
	 */
	static final float KUN_SET_BACK = 25, DRAGON_SET_BACK = 10;

	/** Draws a beast of {@code kind}: its model grown by its rank, its shadow with it, what of it glows glowing in the dark. */
	static final class Renderer extends GeoEntityRenderer<DemonBeast> {
		private final float shadow;

		Renderer(EntityRendererProvider.Context context, BeastKind kind) {
			super(context, new Model(kind));
			this.shadow = kind.isElder() || kind.isPrimordial() ? kind.width * 0.5f : Math.min(1.6f, kind.width * 0.55f);
			this.shadowRadius = shadow;
			addRenderLayer(new AutoGlowingGeoLayer<>(this));
		}

		@Override
		public void render(DemonBeast beast, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
			shadowRadius = shadow * beast.scale();
			super.render(beast, yaw, partialTick, pose, buffers, light);
		}

		/** How much larger than its model it is drawn: an elder the larger (BeastKind#modelScale), grown by its rank; the Phoenix's ember small. */
		private static float grown(DemonBeast beast) {
			float grown = beast.scale() * beast.kind().modelScale();
			// The True Phoenix burnt to an ember: itself curled small in its fire (PrimordialLooks), till it rises again.
			if (beast instanceof PrimordialBeast primordial && primordial.isEmber()) grown *= EMBER_SCALE;
			return grown;
		}

		@Override
		public void scaleModelForRender(float widthScale, float heightScale, PoseStack pose, DemonBeast beast, BakedGeoModel model, boolean isReRender,
				float partialTick, int packedLight, int packedOverlay) {
			float grown = grown(beast);
			super.scaleModelForRender(widthScale * grown, heightScale * grown, pose, beast, model, isReRender, partialTick, packedLight, packedOverlay);
		}

		/**
		 * tools/spell-art's models (Model#spellMade) lie about the line of their body (y = 0): raised to the middle of its bounding box (before
		 * it turns: up is up whichever way it faces), then set back along itself ({@link #KUN_SET_BACK}, {@link #DRAGON_SET_BACK}). Here, not
		 * where it is scaled: the glow layer draws it again from here (a move made there would be made twice).
		 */
		@Override
		protected void applyRotations(DemonBeast beast, PoseStack pose, float ageInTicks, float rotationYaw, float partialTick) {
			boolean spellMade = Model.spellMade(beast);
			if (spellMade) pose.translate(0, beast.getBbHeight() / 2 / grown(beast), 0);
			super.applyRotations(beast, pose, ageInTicks, rotationYaw, partialTick);
			if (spellMade) pose.translate(0, 0, (beast.kind() == BeastKind.TRUE_DRAGON ? DRAGON_SET_BACK : KUN_SET_BACK) / 16.0f);
		}

		/** tools/spell-art's models are seen through where their texture is (the spells' shapes are: SpellConstructRenderer). */
		@Override
		public RenderType getRenderType(DemonBeast beast, ResourceLocation texture, MultiBufferSource buffers, float partialTick) {
			return Model.spellMade(beast) ? RenderType.entityTranslucent(texture) : super.getRenderType(beast, texture, buffers, partialTick);
		}
	}

	/**
	 * A kind's model: its geometry, texture and "idle" animation, and its moving in code. The Kun-Peng's as the Kun is Void Shattering's Kun
	 * (Kun Rises from the Northern Abyss: the spell's own files, geo/spell/kun; the user, 2026-10-26: "Make the ku's first form look like the
	 * one from the spell that summons it"), as the Peng its own ("kun_peng_peng").
	 */
	static final class Model extends GeoModel<DemonBeast> {
		private static final String[] LEGS = {"leg_fl", "leg_fr", "leg_bl", "leg_br"};

		private final BeastKind kind;
		private final ResourceLocation geometry, texture, animations, pengGeometry, pengTexture, pengAnimations;

		Model(BeastKind kind) {
			this.kind = kind;
			String folder = kind == BeastKind.KUN_PENG ? "spell" : "demon_beast", id = kind == BeastKind.KUN_PENG ? "kun" : kind.getId();
			this.geometry = DefyingTheHeavens.id("geo/" + folder + "/" + id + ".geo.json");
			this.texture = DefyingTheHeavens.id("textures/entity/" + folder + "/" + id + ".png");
			this.animations = DefyingTheHeavens.id("animations/" + folder + "/" + id + ".animation.json");
			this.pengGeometry = DefyingTheHeavens.id("geo/demon_beast/" + kind.getId() + "_peng.geo.json");
			this.pengTexture = DefyingTheHeavens.id("textures/entity/demon_beast/" + kind.getId() + "_peng.png");
			this.pengAnimations = DefyingTheHeavens.id("animations/demon_beast/" + kind.getId() + "_peng.animation.json");
		}

		private static boolean peng(DemonBeast beast) {
			return beast instanceof PrimordialBeast primordial && primordial.isPeng();
		}

		/**
		 * Whether its model is tools/spell-art's (the Kun's, the spell's; the True Dragon's): seen through, lying about its body's line
		 * (Renderer#applyRotations), moved by its own loop (PrimordialBeast#swims).
		 */
		static boolean spellMade(DemonBeast beast) {
			return beast instanceof PrimordialBeast primordial && primordial.swims();
		}

		@Override public ResourceLocation getModelResource(DemonBeast beast) { return peng(beast) ? pengGeometry : geometry; }
		@Override public ResourceLocation getTextureResource(DemonBeast beast) { return peng(beast) ? pengTexture : texture; }
		@Override public ResourceLocation getAnimationResource(DemonBeast beast) { return peng(beast) ? pengAnimations : animations; }

		@Override
		public void setCustomAnimations(DemonBeast beast, long instanceId, AnimationState<DemonBeast> state) {
			float partial = state.getPartialTick();
			float time = beast.tickCount + partial;
			float stride = state.getLimbSwing(), walk = Math.min(1, state.getLimbSwingAmount());
			EntityModelData look = state.getData(DataTickets.ENTITY_MODEL_DATA);
			float yaw = look == null ? 0 : Mth.clamp(look.netHeadYaw(), -50, 50) * Mth.DEG_TO_RAD;
			float pitch = look == null ? 0 : Mth.clamp(look.headPitch(), -40, 40) * Mth.DEG_TO_RAD;
			float strike = Mth.sin(beast.getAttackAnim(partial) * Mth.PI);
			boolean shell = beast.acting(DemonBeast.ACT_SHELL);
			// Its acts eased in and out (DemonBeast#acted): a jaw thrown open in a frame, a body snapped into its coil, read as a jolt.
			float charge = beast.acted(DemonBeast.ACT_CHARGE, partial), coil = beast.acted(DemonBeast.ACT_COIL, partial);
			float breath = beast.acted(DemonBeast.ACT_BREATH, partial);

			float open = Math.max(strike * 0.55f, Math.max(breath * (0.5f + Mth.sin(time * 0.9f) * 0.08f), coil * 0.35f));
			if (spellMade(beast)) {
				// Its own loop moves it (its "swim": PrimordialBeast#registerControllers): only its look, its jaw and the Dragon's coils on top,
				// added to where the loop has them this frame (the loop keys every one of these bones, so nothing builds up frame on frame).
				// The Kun's head is the front of its body: turned but a little.
				boolean kun = kind == BeastKind.KUN_PENG;
				nudge("head", (pitch - charge * 0.45f) * (kun ? 0.1f : 0.3f), yaw * (kun ? 0.15f : 0.4f), 0);
				nudge("jaw", -open, 0, 0);
				for (int i = 0; !kun; i++) {
					CoreGeoBone seg = getAnimationProcessor().getBone("seg_" + i);
					if (seg == null) break;
					nudge(seg, 0, coil * 0.2f, 0); // coiled (its sweep, its prey held), curled round into a ring
				}
				return;
			}

			// The head: turned to what it looks at; lowered in a charge; the jaw dropping as it strikes, breathes fire or coils.
			turn("head", pitch - charge * 0.45f, yaw, 0);
			turn("jaw", -open, 0, 0);

			BeastKind.Body body = beast instanceof PrimordialBeast primordial ? primordial.body() : kind.body;
			switch (body) {
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
					// Aloft its wings beat; on the ground, or burnt to an ember, they fold.
					boolean aloft = !beast.onGround() && !beast.isInWater() && !(beast instanceof PrimordialBeast primordial && primordial.isEmber());
					if (aloft) {
						// The vast birds (the Peng, the Phoenix) beat slow and deep.
						float beat = kind.isPrimordial() ? 0.12f : kind.width > 1.5f ? 0.32f : 0.55f;
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
					// Winding: each piece turned from the one before, the wave running back along it: slowly by time standing, with its
					// stride going (a pace by its speed, as it was, scrambled the wave's phase whenever it sped up or slowed: beast-art rule);
					// coiled round its prey, every piece turned alike, into a ring. A flying dragon's body waves up and down as well.
					boolean flies = kind.moves == BeastKind.Moves.FLYING;
					for (int i = 0; ; i++) {
						CoreGeoBone seg = getAnimationProcessor().getBone("seg_" + i);
						if (seg == null) break;
						float wave = Mth.sin(time * 0.08f - i * 0.8f) * 0.2f * (1 - walk) + Mth.sin(stride * 0.45f - i * 0.8f) * 0.32f * walk;
						float bend = Mth.lerp(coil, wave, 0.55f);
						float lift = flies ? Mth.sin(time * 0.06f - i * 0.7f) * 0.05f : 0;
						turn(seg, lift, bend, 0);
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
				default -> {
					// The Kun (a fish) swims by its own loop (above).
				}
			}

			// A tail: swaying slowly standing, swinging with the stride walking; a fox's fanned behind it (the ember fox's three, the
			// Nine-Tailed Fox's nine), each swaying in its own time, and their tips the more (a tail's "fan_<i>_tip" after its root).
			for (int i = 0; ; i++) {
				CoreGeoBone piece = getAnimationProcessor().getBone("tail_" + i);
				if (piece == null) break;
				float sway = Mth.sin(time * 0.09f - i * 0.6f) * 0.14f * (1 - walk) + Mth.sin(stride * 0.6662f - i * 0.6f) * 0.3f * walk;
				turn(piece, Mth.sin(time * 0.06f - i * 0.5f) * 0.04f, sway, 0);
			}
			for (int i = 0; ; i++) {
				CoreGeoBone fan = getAnimationProcessor().getBone("fan_" + i);
				if (fan == null) break;
				turn(fan, Mth.sin(time * 0.07f + i) * 0.06f, Mth.sin(time * 0.1f + i * 1.7f) * 0.14f, 0);
				CoreGeoBone tip = getAnimationProcessor().getBone("fan_" + i + "_tip");
				if (tip != null) turn(tip, Mth.sin(time * 0.07f + i - 0.6f) * 0.1f, Mth.sin(time * 0.1f + i * 1.7f - 0.7f) * 0.2f, 0);
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

		private void nudge(String name, float x, float y, float z) {
			CoreGeoBone bone = getAnimationProcessor().getBone(name);
			if (bone != null) nudge(bone, x, y, z);
		}

		/**
		 * {@code bone} turned this much (radians) further than its animation has it this frame: only for a bone its loop keys (GeckoLib sets a
		 * keyed bone afresh each frame; one no animation keys keeps what was set last frame, and would turn on and on).
		 */
		private static void nudge(CoreGeoBone bone, float x, float y, float z) {
			bone.setRotX(bone.getRotX() + x);
			bone.setRotY(bone.getRotY() + y);
			bone.setRotZ(bone.getRotZ() + z);
		}
	}
}
