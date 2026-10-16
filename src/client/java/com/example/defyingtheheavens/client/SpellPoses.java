package com.example.defyingtheheavens.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;

/** How the element spells pose a body (HumanoidModelMixin, after its own pose is set up). */
public final class SpellPoses {
	/**
	 * Steel Hurricane: as the body flies spinning along its dash (vanilla's riptide turn), both arms thrust ahead of it over the
	 * head, the weapon in hand leading the way, the legs drawn straight behind. Whatever the weapon, it is held out point first.
	 */
	public static void hurricane(HumanoidModel<?> model) {
		model.rightArm.xRot = (float) -Math.PI + 0.15f;
		model.leftArm.xRot = (float) -Math.PI + 0.15f;
		model.rightArm.yRot = -0.12f;
		model.leftArm.yRot = 0.12f;
		model.rightArm.zRot = 0.05f;
		model.leftArm.zRot = -0.05f;
		model.rightLeg.xRot = 0.08f;
		model.leftLeg.xRot = -0.08f;
		model.rightLeg.yRot = 0;
		model.leftLeg.yRot = 0;
		model.head.xRot = -0.6f;
	}

	/**
	 * Riding a Dirt Tide: standing side-on on the crest, knees bent and legs braced apart, arms out for balance and swaying with the
	 * wave. (Vanilla would sit a rider down.)
	 */
	public static void surf(HumanoidModel<?> model, float age) {
		float sway = Mth.sin(age * 0.25f) * 0.08f;
		model.body.yRot = 0.45f;
		model.rightLeg.xRot = -0.25f;
		model.rightLeg.yRot = 0.45f;
		model.rightLeg.zRot = 0.12f;
		model.leftLeg.xRot = 0.3f;
		model.leftLeg.yRot = 0.45f;
		model.leftLeg.zRot = -0.12f;
		model.rightArm.xRot = -0.3f + sway;
		model.leftArm.xRot = -0.2f - sway;
		model.rightArm.zRot = 1.0f + sway;
		model.leftArm.zRot = -1.0f + sway;
		model.rightArm.yRot = 0.45f;
		model.leftArm.yRot = 0.45f;
	}

	private SpellPoses() {}
}
