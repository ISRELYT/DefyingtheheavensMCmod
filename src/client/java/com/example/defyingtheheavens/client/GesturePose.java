package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Gesture;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Poses for greetings and submission (see TalkSystem): a bow from the waist (the crouch's lean, arms hanging, head down),
 * fist cupped in palm before the chest, and down on one knee, head bowed (one who kneels under pressure, or begs for its
 * life). Applied after the model's own setup, blended in by {@code weight} so a gesture eases in and out.
 */
public final class GesturePose {
	public static void apply(HumanoidModel<?> model, Gesture gesture, float weight) {
		if (weight <= 0) return;
		switch (gesture) {
			case BOW -> bow(model, weight);
			case CUP_FIST -> cupFist(model, weight);
		}
		model.hat.copyFrom(model.head);
	}

	private static void bow(HumanoidModel<?> model, float w) {
		// Vanilla's crouch, for the lean of the back, with the arms hanging and the head bowed.
		model.body.xRot = Mth.lerp(w, model.body.xRot, 0.55f);
		model.body.y = Mth.lerp(w, model.body.y, 3.2f);
		model.head.y = Mth.lerp(w, model.head.y, 4.2f);
		model.head.xRot = Mth.lerp(w, model.head.xRot, 0.7f);
		for (ModelPart arm : new ModelPart[] {model.rightArm, model.leftArm}) {
			arm.y = Mth.lerp(w, arm.y, 5.2f);
			arm.xRot = Mth.lerp(w, arm.xRot, 0.45f);
			arm.yRot = Mth.lerp(w, arm.yRot, 0);
		}
		model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, 0.05f);
		model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, -0.05f);
		for (ModelPart leg : new ModelPart[] {model.rightLeg, model.leftLeg}) {
			leg.z = Mth.lerp(w, leg.z, 4.0f);
			leg.y = Mth.lerp(w, leg.y, 12.2f);
			leg.xRot = Mth.lerp(w, leg.xRot, 0);
		}
	}

	private static void cupFist(HumanoidModel<?> model, float w) {
		// Both forearms up before the chest, the hands meeting: fist in palm.
		model.rightArm.xRot = Mth.lerp(w, model.rightArm.xRot, -1.35f);
		model.rightArm.yRot = Mth.lerp(w, model.rightArm.yRot, -0.6f);
		model.rightArm.zRot = Mth.lerp(w, model.rightArm.zRot, 0);
		model.leftArm.xRot = Mth.lerp(w, model.leftArm.xRot, -1.35f);
		model.leftArm.yRot = Mth.lerp(w, model.leftArm.yRot, 0.6f);
		model.leftArm.zRot = Mth.lerp(w, model.leftArm.zRot, 0);
		model.head.xRot = Mth.lerp(w, model.head.xRot, 0.2f); // a nod
	}

	/** Down on the left knee: everything lowered, the right thigh forward with its knee up, the left leg back along the ground. */
	public static void kneel(HumanoidModel<?> model) {
		model.head.y = 6.0f;
		model.head.xRot = 0.55f;
		model.body.y = 6.0f;
		model.body.xRot = 0.15f;
		model.rightArm.y = 8.0f;
		model.leftArm.y = 8.0f;
		model.rightArm.xRot = -0.55f;
		model.rightArm.yRot = 0;
		model.rightArm.zRot = 0.1f;
		model.leftArm.xRot = -0.15f;
		model.leftArm.yRot = 0;
		model.leftArm.zRot = -0.1f;
		model.rightLeg.y = 18.0f;
		model.rightLeg.z = -1.0f;
		model.rightLeg.xRot = -1.35f;
		model.rightLeg.yRot = 0;
		model.rightLeg.zRot = 0;
		model.leftLeg.y = 18.0f;
		model.leftLeg.z = 1.0f;
		model.leftLeg.xRot = 1.25f;
		model.leftLeg.yRot = 0;
		model.leftLeg.zRot = 0;
		model.hat.copyFrom(model.head);
	}

	private GesturePose() {}
}
