package com.example.defyingtheheavens.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;

/**
 * Standing sideways on a flying sword ({@link SwordFlightClient}). The renderer turns the whole body a quarter turn
 * ({@link #TURN_DEGREES}, in LivingEntityRendererMixin), which leaves the right side leading; the pose then turns the head
 * back to face the way the rider flies, sets the feet apart along the blade, raises the leading arm toward the point and
 * holds the other out behind for balance.
 */
public final class SwordStance {
	/** The quarter turn of the body, about the vertical. */
	public static final float TURN_DEGREES = 90.0F;

	/**
	 * Poses {@code model} (already set up for its entity) on the blade. {@code netHeadYaw} is the head's turn from the body
	 * as vanilla set it; {@code bodyOffset} how far the renderer's body yaw was from the rider's ({@link SwordFlightClient#bodyOffset}).
	 */
	public static void apply(HumanoidModel<?> model, float netHeadYaw, float bodyOffset) {
		// The body's extra turn undone for the head (the model's yaw runs opposite to the renderer's, mirrored as it is), so
		// it looks where it was looking.
		model.head.yRot = (netHeadYaw + bodyOffset + TURN_DEGREES) * Mth.DEG_TO_RAD;
		model.hat.copyFrom(model.head);

		// In model space the right arm is the leading one; a positive zRot swings it out to that side.
		model.rightArm.xRot = -0.1F;
		model.rightArm.yRot = 0.0F;
		model.rightArm.zRot = 1.2F;
		model.leftArm.xRot = 0.1F;
		model.leftArm.yRot = 0.0F;
		model.leftArm.zRot = -0.55F;

		model.rightLeg.xRot = 0.0F;
		model.rightLeg.yRot = 0.0F;
		model.rightLeg.zRot = 0.2F;
		model.leftLeg.xRot = 0.0F;
		model.leftLeg.yRot = 0.0F;
		model.leftLeg.zRot = -0.2F;
	}

	private SwordStance() {}
}
