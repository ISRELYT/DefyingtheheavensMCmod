package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.BeastHead;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

/**
 * The beasts' heads (see BeastHead): each a little model of its own, built in cubes as vanilla's are (a model layer,
 * {@code defying-the-heavens:beast_head/<id>}), with a 64 x 64 texture, {@code textures/entity/beast_head/<id>.png}.
 * <p>
 * Worn in place of a man's head on any humanoid model: hide the model's head and hat layer, {@link Head#follow} the human
 * head's pose (it turns and nods with it, its pivot the neck), and draw it with the same transforms and render type as the
 * body, in its own texture. SoulRenderer does so for the Ghost Gate's wardens, drawn as the dead's cyan light; a living wearer
 * (a demon race, one day) would do the same in a render layer with an ordinary entity render type.
 * <p>
 * Units are the model's pixels, as in vanilla: the human head is the box from (-4, -8, -4) to (4, 0, 4) about the neck, its
 * face toward -z, -x its right. A new head: an entry in BeastHead, its mesh in {@link #definition}, its {@link Brow}, and its
 * texture (the box layouts are noted by each mesh).
 */
public final class BeastHeadModels {
	private static final Map<BeastHead, ModelLayerLocation> LAYERS = new EnumMap<>(BeastHead.class);
	private static final Map<BeastHead, Head> BAKED = new EnumMap<>(BeastHead.class);

	/**
	 * Where a talisman sealed to the brow hangs (a Ghost-Quelling Talisman, see SoulRenderer): from (x, y, z) in the named part
	 * ("head", or one of its children), turned {@code xRot} about x, hanging {@code length} pixels down its face.
	 */
	record Brow(String part, float x, float y, float z, float xRot, float length) {}

	/** A man's own brow: just before the face (and its hat layer), hanging down over it. */
	static final Brow HUMAN_BROW = new Brow("head", 0.0f, -8.3f, -4.6f, 0.0f, 7.0f);

	public static void register() {
		for (BeastHead head : BeastHead.values()) {
			ModelLayerLocation layer = new ModelLayerLocation(DefyingTheHeavens.id("beast_head/" + head.getId()), "main");
			LAYERS.put(head, layer);
			EntityModelLayerRegistry.registerModelLayer(layer, () -> definition(head));
		}
	}

	/** {@code head}'s model, baked the first time it is asked for; null for none. */
	public static Head get(BeastHead head) {
		if (head == null) return null;
		return BAKED.computeIfAbsent(head, h -> new Head(Minecraft.getInstance().getEntityModels().bakeLayer(LAYERS.get(h)).getChild("head"),
				DefyingTheHeavens.id("textures/entity/beast_head/" + h.getId() + ".png"), brow(h)));
	}

	/** A beast's head, baked: its parts, its texture, its brow. */
	public static final class Head {
		private final ModelPart head;
		private final ResourceLocation texture;
		private final Brow brow;

		Head(ModelPart head, ResourceLocation texture, Brow brow) {
			this.head = head;
			this.texture = texture;
			this.brow = brow;
		}

		public ResourceLocation texture() { return texture; }

		/** Turns and places it as the man's head it replaces (call after the body model's setupAnim). */
		public void follow(ModelPart human) {
			head.copyFrom(human);
		}

		public void render(PoseStack pose, VertexConsumer buffer, int light, int overlay, float red, float green, float blue, float alpha) {
			head.render(pose, buffer, light, overlay, red, green, blue, alpha);
		}

		/** Moves {@code pose} (at the model's origin) to its brow, where a talisman hangs from (see {@link Brow}). */
		void toBrow(PoseStack pose) {
			head.translateAndRotate(pose);
			if (!brow.part().equals("head")) head.getChild(brow.part()).translateAndRotate(pose);
			toBrowOf(pose, brow);
		}

		float browLength() { return brow.length(); }
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
			// The ox: on the broad forehead, over the brow ridge, down to the muzzle.
			case OX -> new Brow("head", 0.0f, -9.3f, -4.6f, 0.0f, 4.0f);
			// The horse: from the brow down the top of the long face toward the nose, lifting off it a little so that it shows
			// from the front (laid flat, it is all but edge-on to anyone looking the horse in the face).
			case HORSE -> new Brow("face", 0.0f, -0.1f, -1.0f, -Mth.HALF_PI + 0.45f, 5.5f);
		};
	}

	// --- The meshes ---

	static LayerDefinition definition(BeastHead beast) {
		MeshDefinition mesh = new MeshDefinition();
		// The head itself: no box of its own, its pivot the neck, where the human head's is.
		PartDefinition head = mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
		switch (beast) {
			case OX -> ox(head);
			case HORSE -> horse(head);
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	/**
	 * Ox-Head: a bull's head, broader than a man's. A heavy brow over glaring eyes, a curly tuft on the poll, a broad muzzle with a
	 * gold ring through it, ears drooping out to the sides, and great horns sweeping out, up and forward.
	 * <p>
	 * Texture boxes (u, v): skull 0,0 (9 x 9 x 8); muzzle 0,17 (7 x 5 x 5); ring 24,17; brow 0,27 (9 x 1 x 1); horn base 34,0,
	 * middle 34,6, tip 34,10 (each side the same, mirrored); ears 48,0; tuft 48,3.
	 */
	private static void ox(PartDefinition head) {
		head.addOrReplaceChild("skull", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5f, -9.0f, -4.0f, 9, 9, 8), PartPose.ZERO);
		head.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(0, 27).addBox(-4.5f, -8.0f, -4.5f, 9, 1, 1), PartPose.ZERO);
		head.addOrReplaceChild("tuft", CubeListBuilder.create().texOffs(48, 3).addBox(-2.0f, -10.5f, -3.0f, 4, 2, 3), PartPose.ZERO);
		// The muzzle, from the face's lower half, angled a little down; the ring through its nose, two posts and the bar below.
		PartDefinition muzzle = head.addOrReplaceChild("muzzle", CubeListBuilder.create().texOffs(0, 17).addBox(-3.5f, 0.0f, -5.0f, 7, 5, 5),
				PartPose.offsetAndRotation(0.0f, -5.0f, -4.0f, 0.12f, 0.0f, 0.0f));
		muzzle.addOrReplaceChild("ring", CubeListBuilder.create().texOffs(24, 17)
						.addBox(-2.0f, 0.0f, -0.5f, 1, 2, 1).addBox(1.0f, 0.0f, -0.5f, 1, 2, 1).addBox(-2.0f, 2.0f, -0.5f, 4, 1, 1),
				PartPose.offset(0.0f, 3.0f, -5.3f));
		horn(head, "right_horn", -1);
		horn(head, "left_horn", 1);
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(48, 0).addBox(-3.0f, -1.0f, -0.5f, 3, 2, 1),
				PartPose.offsetAndRotation(-4.5f, -5.5f, 0.5f, 0.0f, 0.3f, -0.45f));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(48, 0).mirror().addBox(0.0f, -1.0f, -0.5f, 3, 2, 1),
				PartPose.offsetAndRotation(4.5f, -5.5f, 0.5f, 0.0f, -0.3f, 0.45f));
	}

	/**
	 * One of the ox's horns, {@code side} -1 its right (toward -x), 1 its left: out from the top of the skull's side, rising a
	 * little more with each of its three pieces, the tip curling forward; thinner to the point.
	 */
	private static void horn(PartDefinition head, String name, int side) {
		boolean left = side > 0;
		float from = left ? 0.0f : -3.0f;
		PartDefinition base = head.addOrReplaceChild(name, CubeListBuilder.create().texOffs(34, 0).mirror(left).addBox(from, -1.5f, -1.5f, 3, 3, 3),
				PartPose.offsetAndRotation(4.5f * side, -7.5f, -0.5f, 0.0f, 0.0f, -0.35f * side));
		PartDefinition middle = base.addOrReplaceChild("middle", CubeListBuilder.create().texOffs(34, 6).mirror(left).addBox(from, -1.0f, -1.0f, 3, 2, 2),
				PartPose.offsetAndRotation(3.0f * side, 0.0f, 0.0f, 0.0f, 0.0f, -0.55f * side));
		middle.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(34, 10).mirror(left).addBox(from, -0.5f, -0.5f, 3, 1, 1),
				PartPose.offsetAndRotation(3.0f * side, 0.0f, 0.0f, 0.0f, 0.5f * side, -0.6f * side));
	}

	/**
	 * Horse-Face: a horse's head, its long face sloping down from the brow to a broader nose with the jaw beneath; ears pricked
	 * up and forward, a forelock on the brow, a black mane from the poll down the back of the neck.
	 * <p>
	 * Texture boxes (u, v): skull 0,0 (7 x 8 x 8); face 0,16 (5 x 6 x 8); nose 26,16 (6 x 4 x 3); jaw 44,16 (4 x 1 x 6); ears
	 * 30,0 with their tips 36,0; mane 40,0 (the crest, 2 x 3 x 8) and 0,30 (down the neck); forelock 8,30.
	 */
	private static void horse(PartDefinition head) {
		head.addOrReplaceChild("skull", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5f, -9.0f, -3.5f, 7, 8, 8), PartPose.ZERO);
		// The face slopes from the brow just enough that the nose stays above the chest.
		PartDefinition face = head.addOrReplaceChild("face", CubeListBuilder.create().texOffs(0, 16).addBox(-2.5f, 0.0f, -8.0f, 5, 6, 8),
				PartPose.offsetAndRotation(0.0f, -9.0f, -3.5f, 0.3f, 0.0f, 0.0f));
		face.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(26, 16).addBox(-3.0f, 2.0f, -8.5f, 6, 4, 3), PartPose.ZERO);
		face.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(44, 16).addBox(-2.0f, 6.0f, -7.5f, 4, 1, 6), PartPose.ZERO);
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(30, 0).addBox(-1.0f, -4.0f, -0.5f, 2, 4, 1)
				.texOffs(36, 0).addBox(-0.5f, -5.0f, -0.5f, 1, 1, 1), PartPose.offsetAndRotation(-2.2f, -9.0f, 0.5f, 0.25f, 0.0f, -0.22f));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(30, 0).mirror().addBox(-1.0f, -4.0f, -0.5f, 2, 4, 1)
				.texOffs(36, 0).addBox(-0.5f, -5.0f, -0.5f, 1, 1, 1), PartPose.offsetAndRotation(2.2f, -9.0f, 0.5f, 0.25f, 0.0f, 0.22f));
		head.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(40, 0).addBox(-1.0f, -11.0f, -3.0f, 2, 3, 8)
				.texOffs(0, 30).addBox(-1.0f, -8.0f, 4.0f, 2, 10, 2), PartPose.ZERO);
		head.addOrReplaceChild("forelock", CubeListBuilder.create().texOffs(8, 30).addBox(-1.5f, -9.5f, -4.5f, 3, 3, 1), PartPose.ZERO);
	}

	private BeastHeadModels() {}
}
