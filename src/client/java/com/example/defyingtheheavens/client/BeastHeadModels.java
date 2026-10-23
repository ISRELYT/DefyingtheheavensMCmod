package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.BeastHead;
import com.example.defyingtheheavens.Bloodline;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The beasts' heads (see BeastHead), and a demon's tail with its head: GeckoLib models, each made as a Blockbench project would be
 * ({@code geo/beast/<id>_head.geo.json}, {@code <id>_tail.geo.json}, animations {@code animations/beast/<id>.animation.json}),
 * written with their textures by {@code tools/beast-art}. The ears twitch, the eyes blink, a lizard's tongue flicks, a dragon's
 * whiskers drift ("idle"); the jaw opens as it strikes ("strike"); a tail sways as it stands and swings as it walks, a fox's
 * fanned out one for each of its tails (in code: {@link Model#setCustomAnimations}).
 * <p>
 * Worn in place of a man's head on any humanoid model: hide the model's head and hat layer, {@link Head#follow} the model (the beast's
 * head turns and nods with the human's, its pivot the neck; its tail rides the body), say who wears it ({@link Head#on}: whose
 * animations, which colouring), and {@link Head#render} it with the same transforms as the body, in {@link Head#texture}. BeastLayer
 * does so for the living (players, the bodies drawn as them), SoulRenderer for souls and the dead, drawn as their light.
 * <p>
 * Units are the human model's pixels: the head is the box from (-4, -8, -4) to (4, 0, 4) about the neck, its face toward -z. A model's
 * x and z are the human model's, its y the other way up (a model is drawn turned the right way up, {@code scale(-1, -1, 1)}).
 */
public final class BeastHeadModels {
	private static final Map<BeastHead, Head> HEADS = new EnumMap<>(BeastHead.class);

	/**
	 * Where a talisman sealed to the brow hangs (a Ghost-Quelling Talisman, see SoulRenderer): from (x, y, z) about the neck, in the
	 * human head's pixels (y down), turned {@code xRot} about x, hanging {@code length} pixels down its face.
	 */
	record Brow(float x, float y, float z, float xRot, float length) {}

	/** A man's own brow: just before the face (and its hat layer), hanging down over it. */
	static final Brow HUMAN_BROW = new Brow(0.0f, -8.3f, -4.6f, 0.0f, 7.0f);

	/** The heads load with GeckoLib's models (its own resource reload): nothing to register here. */
	public static void register() {}

	/** {@code head}'s model, made the first time it is asked for; null for none. */
	public static Head get(BeastHead head) {
		if (head == null) return null;
		return HEADS.computeIfAbsent(head, Head::new);
	}

	/** The beast's head (and tail) {@code entity} is drawn with by its bloodline (ClientBloodlines#look), worn by it; null for a human's look. */
	public static Head of(Entity entity) {
		ClientBloodlines.Look look = ClientBloodlines.look(entity);
		return look == null ? null : get(look.bloodline().head()).on(entity, look);
	}

	/** A beast's head and tail, ready to wear. */
	public static final class Head {
		private final BeastHead kind;
		private final ResourceLocation ownTexture;
		private final Brow brow;
		private final Beast headBeast, tailBeast;
		private final Part headPart, tailPart;
		/** The human head's pose and the body's, as followed (empty parts that only hold a pose). */
		private final ModelPart headAnchor = new ModelPart(List.of(), Map.of()), bodyAnchor = new ModelPart(List.of(), Map.of());
		private boolean bodyFollowed;
		/** Who wears it now (whose animations), its colouring and tails, and the texture they're drawn in. */
		private Entity wearer;
		private int tails = 1;
		private ResourceLocation texture;

		Head(BeastHead kind) {
			this.kind = kind;
			this.ownTexture = DefyingTheHeavens.id("textures/entity/beast_head/" + kind.getId() + ".png");
			this.texture = ownTexture;
			this.brow = brow(kind);
			this.headBeast = new Beast(this, false);
			this.headPart = new Part(new Model(this, DefyingTheHeavens.id("geo/beast/" + kind.getId() + "_head.geo.json")));
			boolean tailed = kind != BeastHead.OX && kind != BeastHead.HORSE;
			this.tailBeast = tailed ? new Beast(this, true) : null;
			this.tailPart = tailed ? new Part(new Model(this, DefyingTheHeavens.id("geo/beast/" + kind.getId() + "_tail.geo.json"))) : null;
		}

		/** Worn by {@code wearer} (its animations; null for none), in {@code look}'s colouring (null: the head's own texture, Ox-Head's...). */
		public Head on(Entity wearer, ClientBloodlines.Look look) {
			this.wearer = wearer;
			this.tails = look == null ? 1 : look.tails();
			this.texture = look == null ? ownTexture : ClientBloodlines.beastTexture(look);
			return this;
		}

		/** The texture it's drawn in, as worn now. */
		public ResourceLocation texture() { return texture; }

		/** Turns and places the head as the man's head it replaces (call after the body model's setupAnim); no tail. */
		public void follow(ModelPart human) {
			headAnchor.copyFrom(human);
			bodyFollowed = false;
		}

		/** As {@link #follow(ModelPart)}, and its tail rides the model's body. */
		public void follow(HumanoidModel<?> model) {
			headAnchor.copyFrom(model.head);
			bodyAnchor.copyFrom(model.body);
			bodyFollowed = true;
		}

		/** Draws it, the pose at the model's origin, into {@code buffer} (in {@link #texture}), lit {@code light}, tinted. */
		public void render(PoseStack pose, VertexConsumer buffer, int light, int overlay, float red, float green, float blue, float alpha) {
			RenderType type = RenderType.entityCutoutNoCull(texture);
			MultiBufferSource source = Minecraft.getInstance().renderBuffers().bufferSource();
			long id = wearer == null ? 0 : wearer.getId() * 2L;
			pose.pushPose();
			headAnchor.translateAndRotate(pose);
			pose.scale(-1.0f, -1.0f, 1.0f);
			headPart.draw(pose, headBeast, source, type, buffer, light, overlay, red, green, blue, alpha, id);
			pose.popPose();
			if (tailPart != null && bodyFollowed) {
				pose.pushPose();
				bodyAnchor.translateAndRotate(pose);
				pose.scale(-1.0f, -1.0f, 1.0f);
				tailPart.draw(pose, tailBeast, source, type, buffer, light, overlay, red, green, blue, alpha, id + 1);
				pose.popPose();
			}
		}

		/** Moves {@code pose} (at the model's origin) to its brow, where a talisman hangs from (see {@link Brow}). */
		void toBrow(PoseStack pose) {
			headAnchor.translateAndRotate(pose);
			toBrowOf(pose, brow);
		}

		float browLength() { return brow.length(); }

		/** How fast its wearer walks now, 0 to 1. */
		float walking(float partial) {
			return wearer instanceof LivingEntity living ? Math.min(1, living.walkAnimation.speed(partial)) : 0;
		}

		/** Whether its wearer is striking now. */
		boolean striking(float partial) {
			return wearer instanceof LivingEntity living && living.getAttackAnim(partial) > 0;
		}
	}

	/** Moves {@code pose} (at the model's origin) to a man's brow, the human {@code head} posed. */
	static void toHumanBrow(PoseStack pose, ModelPart head) {
		head.translateAndRotate(pose);
		toBrowOf(pose, HUMAN_BROW);
	}

	private static void toBrowOf(PoseStack pose, Brow brow) {
		pose.translate(brow.x() / 16.0f, brow.y() / 16.0f, brow.z() / 16.0f);
		if (brow.xRot() != 0) pose.mulPose(Axis.XP.rotation(brow.xRot()));
	}

	private static Brow brow(BeastHead head) {
		return switch (head) {
			// The ox: on the broad forehead, over the brow ridge, down toward the muzzle.
			case OX -> new Brow(0.0f, -8.6f, -5.1f, 0.0f, 4.0f);
			// The horse: high on the long face, tipped back to lie along it a little, so it shows from the front.
			case HORSE -> new Brow(0.0f, -7.6f, -5.4f, -0.35f, 5.0f);
			case TIGER -> new Brow(0.0f, -7.4f, -5.1f, 0.0f, 4.5f);
			case LIZARD -> new Brow(0.0f, -6.4f, -5.6f, -0.2f, 4.0f);
			case FOX -> new Brow(0.0f, -6.9f, -4.6f, 0.0f, 4.0f);
			case DRAGON -> new Brow(0.0f, -7.4f, -4.6f, 0.0f, 4.5f);
		};
	}

	// --- GeckoLib ---

	/** A head's or a tail's animations, kept for each wearer apart (the wearer's id its instance). */
	static final class Beast implements GeoAnimatable {
		private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
		private static final RawAnimation STRIKE = RawAnimation.begin().thenPlay("strike");

		private final Head head;
		private final boolean tail;
		private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this, true);

		Beast(Head head, boolean tail) {
			this.head = head;
			this.tail = tail;
		}

		@Override
		public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
			if (tail) return; // a tail moves in code (Model#setCustomAnimations)
			controllers.add(new AnimationController<>(this, "idle", 0, state -> state.setAndContinue(IDLE)));
			controllers.add(new AnimationController<>(this, "strike", 2, state -> {
				if (head.striking(state.getPartialTick())) return state.setAndContinue(STRIKE);
				state.getController().forceAnimationReset();
				return PlayState.STOP;
			}));
		}

		@Override
		public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

		@Override
		public double getTick(Object object) {
			return head.wearer == null ? 0 : head.wearer.tickCount;
		}
	}

	/** A head's or a tail's model: its geometry, the head's texture as worn, the head's kind's animations. */
	static final class Model extends GeoModel<Beast> {
		private final Head head;
		private final ResourceLocation geometry, animations;

		Model(Head head, ResourceLocation geometry) {
			this.head = head;
			this.geometry = geometry;
			this.animations = DefyingTheHeavens.id("animations/beast/" + head.kind.getId() + ".animation.json");
		}

		@Override public ResourceLocation getModelResource(Beast beast) { return geometry; }
		@Override public ResourceLocation getTextureResource(Beast beast) { return head.texture; }
		@Override public ResourceLocation getAnimationResource(Beast beast) { return animations; }

		/**
		 * A tail's sway: each piece turned a little more than the one before it, slow standing and swinging wide as the wearer walks; a
		 * fox's tails fanned out evenly, as many shown as it has (Bloodline#tails), the rest hidden.
		 */
		@Override
		public void setCustomAnimations(Beast beast, long instanceId, AnimationState<Beast> state) {
			if (!beast.tail || head.wearer == null) return;
			float partial = state.getPartialTick();
			float time = head.wearer.tickCount + partial;
			float walk = head.walking(partial);
			float sway = 0.12f + 0.25f * walk, pace = 0.08f + 0.22f * walk;
			if (head.kind == BeastHead.FOX) {
				// Fanned up behind the back, as a peacock's tail is: each raised (the more tails, the higher), then rolled about the
				// way they point, so they stand in an arc over the back rather than round the waist.
				int tails = Mth.clamp(head.tails, 1, Bloodline.MOST_TAILS);
				float spread = Math.min(1.3f, 0.19f * (tails - 1));
				for (int i = 1; i <= Bloodline.MOST_TAILS; i++) {
					CoreGeoBone root = getAnimationProcessor().getBone("tail_" + i);
					if (root == null) continue;
					root.setHidden(i > tails);
					if (i > tails) continue;
					float at = tails == 1 ? 0 : -spread + 2 * spread * (i - 1) / (tails - 1);
					float up = root.getInitialSnapshot().getRotX();
					float raise = Math.copySign(0.08f * (tails - 1), up == 0 ? 1 : up);
					turn(root, raise, Mth.sin(time * pace + i) * sway * 0.35f, at);
					for (int s = 1; s <= 2; s++) {
						CoreGeoBone piece = getAnimationProcessor().getBone("tail_" + i + "_" + s);
						if (piece != null) turn(piece, 0, Mth.sin(time * pace + i - s * 0.7f) * sway * 0.5f, 0);
					}
				}
				return;
			}
			for (int s = 0; ; s++) {
				CoreGeoBone piece = getAnimationProcessor().getBone("tail_" + s);
				if (piece == null) break;
				turn(piece, Mth.sin(time * pace * 0.7f - s * 0.5f) * 0.04f, Mth.sin(time * pace - s * 0.6f) * sway, 0);
			}
		}

		/** {@code bone} turned this much (radians) from where the model has it. */
		private static void turn(CoreGeoBone bone, float x, float y, float z) {
			bone.setRotX(bone.getInitialSnapshot().getRotX() + x);
			bone.setRotY(bone.getInitialSnapshot().getRotY() + y);
			bone.setRotZ(bone.getInitialSnapshot().getRotZ() + z);
		}
	}

	/** Draws a head's or a tail's model where the pose stands (no block's offset), as worn: whose animations, its tint and overlay. */
	static final class Part extends GeoObjectRenderer<Beast> {
		private long instance;
		private float red = 1, green = 1, blue = 1, alpha = 1;
		private int overlay;

		Part(Model model) {
			super(model);
		}

		void draw(PoseStack pose, Beast beast, MultiBufferSource source, RenderType type, VertexConsumer buffer, int light, int overlay,
				  float red, float green, float blue, float alpha, long instance) {
			this.instance = instance;
			this.overlay = overlay;
			this.red = red;
			this.green = green;
			this.blue = blue;
			this.alpha = alpha;
			render(pose, beast, source, type, buffer, light);
		}

		@Override
		public void preRender(PoseStack poseStack, Beast beast, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer,
							  boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
			this.objectRenderTranslations = new Matrix4f(poseStack.last().pose());
		}

		@Override public long getInstanceId(Beast beast) { return instance; }
		@Override public Color getRenderColor(Beast beast, float partialTick, int packedLight) { return Color.ofRGBA(red, green, blue, alpha); }
		@Override public int getPackedOverlay(Beast beast, float u, float partialTick) { return overlay; }
	}

	private BeastHeadModels() {}
}
