package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The shapes cultivator clothing is worn in (see {@link ClothingArmorRenderer}): an inner disciple's robe, close to the
 * body (a thinner shell than vanilla armor), with fitted sleeves to the wrist. The robe's skirt is in two halves, one round
 * each leg from the hip to mid-calf, so it strides with the legs (meeting down the front like the robe's opening when they
 * stand together) instead of flapping or letting the legs through; it belongs to the robe, so it is drawn with the chest
 * piece, the leg cubes themselves skipped. On the body: a slim sash with a knot and one ribbon. On the head: a topknot, a
 * little hair crown in the grade's colour, a pin through it. The trousers are the plain inner armor shape.
 * <p>
 * Outer texture (64 x 64): head 0,0; body 16,16; arm 40,16; leg (boots) 0,16 (as vanilla armor); skirt half 0,32 (4x8x4,
 * mirrored on the left); sash 24,46 (9x2x5); knot 48,32 (3x2x1); ribbon 60,32 (2x6x1); crown 48,36 (3x2x3); pin 48,42
 * (7x1x1); topknot 52,46 (2x2x2).
 */
public final class ClothingModels {
	public static final ModelLayerLocation OUTER = new ModelLayerLocation(DefyingTheHeavens.id("cultivator_clothing"), "outer");
	public static final ModelLayerLocation INNER = new ModelLayerLocation(DefyingTheHeavens.id("cultivator_clothing"), "inner");

	/** How far the robe stands off the body (vanilla armor: 1.0) and the sash off the robe. */
	private static final float ROBE = 0.6f, SASH = 0.45f;
	/** The skirt halves stand off the legs this far (the left a hair less, so the two never fight where they meet). */
	private static final float SKIRT_RIGHT = 0.75f, SKIRT_LEFT = 0.7f;
	public static final String SKIRT = "skirt";

	public static void register() {
		EntityModelLayerRegistry.registerModelLayer(OUTER, ClothingModels::outer);
		EntityModelLayerRegistry.registerModelLayer(INNER, ClothingModels::inner);
	}

	private static LayerDefinition outer() {
		MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(ROBE), 0);
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.getChild("body");
		root.getChild("right_leg").addOrReplaceChild(SKIRT, CubeListBuilder.create().texOffs(0, 32)
				.addBox(-2.0f, 0.0f, -2.0f, 4, 8, 4, new CubeDeformation(SKIRT_RIGHT)), PartPose.ZERO);
		root.getChild("left_leg").addOrReplaceChild(SKIRT, CubeListBuilder.create().texOffs(0, 32).mirror()
				.addBox(-2.0f, 0.0f, -2.0f, 4, 8, 4, new CubeDeformation(SKIRT_LEFT)), PartPose.ZERO);
		body.addOrReplaceChild("sash", CubeListBuilder.create().texOffs(24, 46).addBox(-4.5f, 6.5f, -2.5f, 9, 2, 5, new CubeDeformation(SASH)),
				PartPose.ZERO);
		float knot = -3.4f - SASH; // just in front of the sash
		body.addOrReplaceChild("knot", CubeListBuilder.create().texOffs(48, 32).addBox(-1.5f, 6.5f, knot, 3, 2, 1, new CubeDeformation(0.2f)),
				PartPose.ZERO);
		body.addOrReplaceChild("ribbon", CubeListBuilder.create().texOffs(60, 32).addBox(-1.0f, 0.0f, -0.5f, 2, 6, 1, new CubeDeformation(-0.3f)),
				PartPose.offsetAndRotation(-0.8f, 8.4f, knot + 0.1f, 0, 0, 0.12f));
		PartDefinition head = root.getChild("head");
		head.addOrReplaceChild("topknot", CubeListBuilder.create().texOffs(52, 46).addBox(-1.0f, -10.5f, -1.0f, 2, 2, 2, new CubeDeformation(0.3f)),
				PartPose.ZERO);
		head.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(48, 36).addBox(-1.5f, -11.5f, -1.5f, 3, 2, 3, new CubeDeformation(0.15f)),
				PartPose.ZERO);
		head.addOrReplaceChild("pin", CubeListBuilder.create().texOffs(48, 42).addBox(-3.5f, -11.0f, -0.5f, 7, 1, 1, new CubeDeformation(-0.2f)),
				PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	private static LayerDefinition inner() {
		return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.5f), 0), 64, 32);
	}

	/** After the pose is copied from the wearer: the ribbon swings a little with the leg striding forward. */
	public static void animate(HumanoidModel<?> model) {
		float forward = Math.min(0, Math.min(model.rightLeg.xRot, model.leftLeg.xRot));
		if (model.body.hasChild("ribbon")) model.body.getChild("ribbon").xRot = Mth.clamp(forward * 0.4f, -0.4f, 0);
	}

	/** Whether the skirt halves on the legs are drawn (with the robe) or not (with the boots). */
	public static void showSkirt(HumanoidModel<?> model, boolean shown) {
		for (ModelPart leg : new ModelPart[] {model.rightLeg, model.leftLeg}) {
			if (leg.hasChild(SKIRT)) leg.getChild(SKIRT).visible = shown;
		}
	}

	private ClothingModels() {}
}
