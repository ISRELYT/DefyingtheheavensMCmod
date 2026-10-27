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

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The wild demon beasts' looks (DemonBeast, BeastKind): each kind's own GeckoLib model, texture and glow (tools/beast-art's Bodies), drawn
 * the larger the higher its rank (DemonBeast#scale), moved in code by its build ({@link Model#setCustomAnimations}): legs in their gait,
 * the head turned to what it looks at, a tail swaying (a fox's fanned tails, three or nine), a serpent's body winding, a bird's wings folded or beating, an ape's arms swinging
 * and striking, a Chaos Fiend's long arms and its smoke and the shards circling it, the Primordial Chaos's four wings and six legs; and its acts (a turtle drawn into its shell, a charge's lowered head, a coil's curled body, fire breathed with open
 * jaws). Its "idle" animation (the file's) breathes and blinks. Two of the primordial beasts are tools/spell-art's instead (the Kun, the
 * spell's own; the True Dragon): seen through, moved by their own "swim", their look and jaws on top ({@link Model#spellMade}; the Dragon's
 * head and neck alive on top of it, Model#gaze). Also the
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

		/** What {@link #nudge} turned each bone by last frame (radians about x, y, z), and whether this frame's were set. */
		private final Map<CoreGeoBone, float[]> nudged = new IdentityHashMap<>();
		private boolean customised;

		/**
		 * Last frame's nudges taken off first. GeckoLib eases a bone into a move (the Dragon's roar) or back to its loop from a snapshot
		 * of the bone as it stands, and a nudge left in it was counted twice: the True Dragon's head jerked some seventy degrees in a frame
		 * each time it roared (tour dragon1). When GeckoLib draws the same frame again and sets nothing, they go back on as they were.
		 */
		@Override
		public void handleAnimations(DemonBeast beast, long instanceId, AnimationState<DemonBeast> state) {
			Map<CoreGeoBone, float[]> last = nudged.isEmpty() ? Map.of() : new IdentityHashMap<>(nudged);
			for (Map.Entry<CoreGeoBone, float[]> e : last.entrySet()) turnBy(e.getKey(), -e.getValue()[0], -e.getValue()[1], -e.getValue()[2]);
			nudged.clear();
			customised = false;
			super.handleAnimations(beast, instanceId, state);
			if (customised) return;
			for (Map.Entry<CoreGeoBone, float[]> e : last.entrySet()) turnBy(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]);
			nudged.putAll(last);
		}

		@Override
		public void setCustomAnimations(DemonBeast beast, long instanceId, AnimationState<DemonBeast> state) {
			customised = true;
			float partial = state.getPartialTick();
			float time = beast.tickCount + partial;
			float stride = state.getLimbSwing(), walk = Math.min(1, state.getLimbSwingAmount());
			EntityModelData look = state.getData(DataTickets.ENTITY_MODEL_DATA);
			// Wrapped: the head's and the body's turns can be a whole turn apart (a beast circling for long), read as the wrong side.
			float yaw = look == null ? 0 : Mth.clamp(Mth.wrapDegrees(look.netHeadYaw()), -50, 50) * Mth.DEG_TO_RAD;
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
				if (kun) nudge("head", (pitch - charge * 0.45f) * 0.1f, yaw * 0.15f, 0);
				else gaze((PrimordialBeast) beast, time, partial, yaw, pitch - charge * 0.45f);
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
						// The vast birds (the Peng, the Phoenix) beat slow and deep (a little quicker since they fly the swifter: PrimordialBeast#SWIFT).
						float beat = kind.isPrimordial() ? 0.15f : kind.width > 1.5f ? 0.32f : 0.55f;
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
				case WRAITH -> {
					// A Chaos Fiend: its long arms swaying as it drifts, flung out as it looses a bolt or claws (its strike); its smoke
					// trailing beneath it in a slow swirl; the shards of stone about it circling.
					float sway = Mth.sin(time * 0.07f) * 0.12f;
					turn("arm_l", -0.35f + sway - strike * 1.6f, 0, -0.12f + sway * 0.5f);
					turn("arm_r", -0.35f - sway - strike * 1.9f, 0, 0.12f + sway * 0.5f);
					for (int i = 0; ; i++) {
						CoreGeoBone smoke = getAnimationProcessor().getBone("smoke_" + i);
						if (smoke == null) break;
						turn(smoke, Mth.sin(time * 0.09f - i * 0.8f) * 0.16f, 0, Mth.cos(time * 0.07f - i * 0.8f) * 0.16f);
					}
					turn("orbit", 0, time * 0.05f, 0);
				}
				case CHAOS -> {
					// The Primordial Chaos: its four wings beating slow, the hind pair after the fore; its six legs paddling the air, each
					// in its own time; drawn in on itself as it gathers its Unmaking (its coil), its maw torn open as it devours (its breath).
					float flap = Mth.sin(time * 0.1f) * 0.45f, hind = Mth.sin(time * 0.1f - 1.2f) * 0.4f;
					turn("wing_l", 0, 0, flap);
					turn("wing_r", 0, 0, -flap);
					turn("wing_l2", 0, 0, hind);
					turn("wing_r2", 0, 0, -hind);
					for (int i = 0; i < 6; i++) turn("leg_" + i, Mth.sin(time * 0.08f + i * 1.1f) * 0.22f, 0, Mth.cos(time * 0.06f + i * 1.7f) * 0.08f);
					CoreGeoBone maw = getAnimationProcessor().getBone("maw");
					if (maw != null) maw.setScaleY(0.25f + 3.5f * breath); // no loop keys it: set whole each frame
					CoreGeoBone sack = getAnimationProcessor().getBone("body");
					if (sack != null && coil > 0) {
						// Its loop keys the body's breath: drawn in on that, this frame's.
						float in = 1 - 0.14f * coil;
						sack.setScaleX(sack.getScaleX() * in);
						sack.setScaleY(sack.getScaleY() * in);
						sack.setScaleZ(sack.getScaleZ() * in);
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

		/**
		 * How much of the True Dragon's head's turn each of its neck's pieces carries, from the head back: the first turned most toward
		 * it, the next three back again, so its neck bends (the head and the first piece share a pivot: the head turned alone kinks there)
		 * and the body behind goes on straight as it was.
		 */
		private static final float[] NECK = {0.55f, -0.25f, -0.2f, -0.1f};
		/** The most the Dragon's head turns from its body, side to side and up and down (radians). */
		private static final float GAZE_YAW = 0.8f, GAZE_PITCH = 0.5f;

		/**
		 * The True Dragon's head (the user, 2026-10-27: "the dragon isnt that stiff which is nice but its head never moves"; its swim turns
		 * it a few degrees): looking about as it flies, slowly, on two swells; leading into its turns; raised as it climbs and lowered as it
		 * dives; turned hard to its foe when it has one ({@code yaw}, {@code pitch}: what it looks at); a slow roll. The neck carries a share
		 * of it ({@link #NECK}).
		 */
		private void gaze(PrimordialBeast dragon, float time, float partial, float yaw, float pitch) {
			float looking = Math.min(1, Math.abs(yaw) / 0.35f + Math.abs(pitch) / 0.35f), about = 1 - 0.6f * looking;
			float side = yaw * 0.75f + dragon.turning(partial) * 0.3f + (Mth.sin(time * 0.045f) * 0.15f + Mth.sin(time * 0.11f + 1.3f) * 0.05f) * about;
			float up = pitch * 0.6f - dragon.climbing(partial) * 0.3f + Mth.sin(time * 0.06f + 0.7f) * 0.07f * about;
			side = Mth.clamp(side, -GAZE_YAW, GAZE_YAW);
			up = Mth.clamp(up, -GAZE_PITCH, GAZE_PITCH);
			nudge("head", up, side, Mth.sin(time * 0.05f + 2.1f) * 0.06f);
			for (int i = 0; i < NECK.length; i++) nudge("seg_" + i, up * NECK[i], side * NECK[i], 0);
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
		 * {@code bone} turned this much (radians) further than its animation has it this frame: kept ({@link #nudged}) to be taken off before
		 * the next (GeckoLib sets a keyed bone afresh each frame, but one no animation keys keeps what was set last frame, and its snapshots
		 * take whatever stands).
		 */
		private void nudge(CoreGeoBone bone, float x, float y, float z) {
			turnBy(bone, x, y, z);
			float[] sum = nudged.computeIfAbsent(bone, b -> new float[3]);
			sum[0] += x;
			sum[1] += y;
			sum[2] += z;
		}

		private static void turnBy(CoreGeoBone bone, float x, float y, float z) {
			bone.setRotX(bone.getRotX() + x);
			bone.setRotY(bone.getRotY() + y);
			bone.setRotZ(bone.getRotZ() + z);
		}
	}
}
