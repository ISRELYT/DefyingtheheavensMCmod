package com.example.defyingtheheavens.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;

/** The cross-legged meditation pose, shared by meditating players, their bodies, and the Nascent Soul in the Inner Realm. */
public final class LotusPose {
	/** Poses {@code model} (already set up for its entity) cross-legged on the ground. */
	public static void apply(HumanoidModel<?> model) {
		// Model space: y = 24 is the ground. Drop everything 10px so the hips sit on the floor.
		model.head.y = 10.0F;
		model.body.y = 10.0F;
		model.body.xRot = 0.0F;
		model.hat.copyFrom(model.head);

		model.rightArm.y = 12.0F;
		model.leftArm.y = 12.0F;
		model.rightArm.xRot = -0.8F;
		model.leftArm.xRot = -0.8F;
		model.rightArm.yRot = -0.2F;
		model.leftArm.yRot = 0.2F;
		model.rightArm.zRot = 0.1F;
		model.leftArm.zRot = -0.1F;

		// Thighs forward and crossed in front; left leg 1px higher to avoid z-fighting.
		model.rightLeg.y = 22.0F;
		model.leftLeg.y = 21.0F;
		model.rightLeg.z = 0.1F;
		model.leftLeg.z = 0.1F;
		model.rightLeg.xRot = -1.35F;
		model.leftLeg.xRot = -1.35F;
		model.rightLeg.yRot = -0.75F;
		model.leftLeg.yRot = 0.75F;
		model.rightLeg.zRot = 0.0F;
		model.leftLeg.zRot = 0.0F;
	}

	/** The pose with the outer layers (sleeves, trousers, jacket) following, for a model drawn without setupAnim. */
	public static void applyWithLayers(PlayerModel<?> model) {
		model.head.resetPose();
		model.body.resetPose();
		model.rightArm.resetPose();
		model.leftArm.resetPose();
		model.rightLeg.resetPose();
		model.leftLeg.resetPose();
		model.head.xRot = 0.15F; // eyes lowered
		apply(model);
		model.leftPants.copyFrom(model.leftLeg);
		model.rightPants.copyFrom(model.rightLeg);
		model.leftSleeve.copyFrom(model.leftArm);
		model.rightSleeve.copyFrom(model.rightArm);
		model.jacket.copyFrom(model.body);
	}

	private LotusPose() {}
}
