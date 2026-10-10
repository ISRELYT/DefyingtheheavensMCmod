package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The shapes cultivator clothing is worn in (see {@link ClothingArmorRenderer}): on top of the usual armor body, a robe
 * skirt that falls from the waist to the knees, wide sleeves that hang from the elbow past the wrist, and a little hair
 * crown with its pin on top of the head. The trousers are the plain inner armor shape.
 * <p>
 * Outer texture (64 x 64): head 0,0; body 16,16; arm 40,16; leg 0,16 (as vanilla armor); skirt 0,32; sleeve 28,32;
 * crown 48,32; pin 48,40.
 */
public final class ClothingModels {
	public static final ModelLayerLocation OUTER = new ModelLayerLocation(DefyingTheHeavens.id("cultivator_clothing"), "outer");
	public static final ModelLayerLocation INNER = new ModelLayerLocation(DefyingTheHeavens.id("cultivator_clothing"), "inner");

	public static void register() {
		EntityModelLayerRegistry.registerModelLayer(OUTER, ClothingModels::outer);
		EntityModelLayerRegistry.registerModelLayer(INNER, ClothingModels::inner);
	}

	private static LayerDefinition outer() {
		MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(1.0f), 0);
		PartDefinition root = mesh.getRoot();
		root.getChild("body").addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(0, 32)
				.addBox(-4.5f, 11.0f, -2.5f, 9, 9, 5, new CubeDeformation(0.8f)), PartPose.ZERO);
		root.getChild("right_arm").addOrReplaceChild("sleeve", CubeListBuilder.create().texOffs(28, 32)
				.addBox(-3.5f, 3.0f, -2.5f, 5, 10, 5, new CubeDeformation(0.6f)), PartPose.ZERO);
		root.getChild("left_arm").addOrReplaceChild("sleeve", CubeListBuilder.create().texOffs(28, 32).mirror()
				.addBox(-1.5f, 3.0f, -2.5f, 5, 10, 5, new CubeDeformation(0.6f)), PartPose.ZERO);
		root.getChild("head").addOrReplaceChild("crown", CubeListBuilder.create().texOffs(48, 32)
				.addBox(-1.5f, -11.5f, -1.5f, 3, 3, 3, CubeDeformation.NONE), PartPose.ZERO);
		root.getChild("head").addOrReplaceChild("pin", CubeListBuilder.create().texOffs(48, 40)
				.addBox(-3.0f, -10.5f, -0.5f, 6, 1, 1, CubeDeformation.NONE), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

	private static LayerDefinition inner() {
		return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.5f), 0), 64, 32);
	}

	private ClothingModels() {}
}
