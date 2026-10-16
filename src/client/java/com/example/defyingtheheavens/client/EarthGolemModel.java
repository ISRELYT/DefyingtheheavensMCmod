package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.EarthGolem;
import net.minecraft.client.model.HierarchicalModel;
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
 * The earth golem (EarthGolem): a hulking, legless torso of packed earth and stone floating over the ground, a small head sunk
 * between great shoulders, long heavy arms with fists like boulders, slabs of rock on its back and shoulders, and a tail of broken
 * stones trailing beneath it in place of legs. Its arms swing as it flies and pound when it strikes. Texture: SpellRender.GOLEM,
 * 64 x 64, with molten seams lit on SpellRender.GOLEM_GLOW. Units are pixels; it stands about 2.6 blocks tall.
 */
public class EarthGolemModel extends HierarchicalModel<EarthGolem> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(DefyingTheHeavens.id("earth_golem"), "main");
	private final ModelPart root, body, head, rightArm, leftArm, tail;

	public EarthGolemModel(ModelPart root) {
		this.root = root;
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightArm = body.getChild("right_arm");
		this.leftArm = body.getChild("left_arm");
		this.tail = body.getChild("tail");
	}

	public static LayerDefinition definition() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition parts = mesh.getRoot();
		PartDefinition body = parts.addOrReplaceChild("body", CubeListBuilder.create()
						.texOffs(0, 32).addBox(-9, -24, -6, 18, 16, 12)
						.texOffs(0, 16).addBox(-7, -8, -5, 14, 8, 10, new CubeDeformation(0.2f))
						// Slabs of rock on the back and shoulders.
						.texOffs(32, 0).addBox(-10, -27, 3, 8, 6, 4)
						.texOffs(32, 0).addBox(2, -26, 3, 8, 5, 4),
				PartPose.offset(0, 6, 0));
		body.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4, -7, -5, 8, 8, 8)
				.texOffs(48, 12).addBox(-5, -6, -6, 10, 2, 2), PartPose.offset(0, -23, -3));
		body.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(40, 16).addBox(-6, -3, -4, 7, 10, 8)
				.texOffs(32, 36).addBox(-6.5f, 7, -4.5f, 8, 14, 9)
				.texOffs(0, 48).addBox(-7.5f, 21, -5.5f, 10, 8, 11), PartPose.offset(-10, -21, 0));
		body.addOrReplaceChild("left_arm", CubeListBuilder.create().mirror()
				.texOffs(40, 16).addBox(-1, -3, -4, 7, 10, 8)
				.texOffs(32, 36).addBox(-1.5f, 7, -4.5f, 8, 14, 9)
				.texOffs(0, 48).addBox(-2.5f, 21, -5.5f, 10, 8, 11), PartPose.offset(10, -21, 0));
		// Broken stones trailing under it where legs would be.
		body.addOrReplaceChild("tail", CubeListBuilder.create()
				.texOffs(44, 0).addBox(-4, 0, -3, 8, 5, 6)
				.texOffs(44, 0).addBox(-3, 6, -2, 6, 4, 5)
				.texOffs(44, 0).addBox(-2, 11, -1.5f, 4, 3, 3), PartPose.offset(0, 0, 0));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public ModelPart root() {
		return root;
	}

	@Override
	public void setupAnim(EarthGolem golem, float walk, float walkSpeed, float age, float headYaw, float headPitch) {
		head.yRot = headYaw * Mth.DEG_TO_RAD;
		head.xRot = headPitch * Mth.DEG_TO_RAD;
		// Its arms sway as it flies; a blow brings one fist down hard.
		float sway = Mth.sin(age * 0.12f) * 0.12f + walkSpeed * 0.4f;
		rightArm.xRot = -0.15f + sway;
		leftArm.xRot = -0.15f - sway;
		rightArm.zRot = 0.08f;
		leftArm.zRot = -0.08f;
		int since = golem.sinceSwing();
		if (since < 10) {
			float t = since / 10f;
			float swing = t < 0.3f ? -2.2f * (t / 0.3f) : -2.2f * (1 - (t - 0.3f) / 0.7f);
			rightArm.xRot = swing;
			leftArm.xRot = swing * 0.8f;
		}
		body.xRot = 0.12f + Mth.sin(age * 0.08f) * 0.03f;
		tail.xRot = 0.25f + Mth.sin(age * 0.1f) * 0.1f;
		tail.zRot = Mth.sin(age * 0.07f) * 0.08f;
	}
}
