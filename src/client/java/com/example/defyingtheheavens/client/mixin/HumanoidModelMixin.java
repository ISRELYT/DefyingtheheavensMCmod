package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.ClientMeditationTracker;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Poses meditating players cross-legged on the ground. Injected at the TAIL of HumanoidModel#setupAnim,
 * which PlayerModel calls before copying limbs onto the sleeve/pants layers, so the outer layers follow automatically.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
	@Shadow @Final public ModelPart head;
	@Shadow @Final public ModelPart hat;
	@Shadow @Final public ModelPart body;
	@Shadow @Final public ModelPart rightArm;
	@Shadow @Final public ModelPart leftArm;
	@Shadow @Final public ModelPart rightLeg;
	@Shadow @Final public ModelPart leftLeg;

	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void dth$lotusPose(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
							   float netHeadYaw, float headPitch, CallbackInfo ci) {
		if (!(entity instanceof Player) || !ClientMeditationTracker.isMeditating(entity.getUUID())) return;

		// Model space: y = 24 is the ground. Drop everything 10px so the hips sit on the floor.
		head.y = 10.0F;
		body.y = 10.0F;
		body.xRot = 0.0F;
		hat.copyFrom(head);

		rightArm.y = 12.0F;
		leftArm.y = 12.0F;
		rightArm.xRot = -0.8F;
		leftArm.xRot = -0.8F;
		rightArm.yRot = -0.2F;
		leftArm.yRot = 0.2F;
		rightArm.zRot = 0.1F;
		leftArm.zRot = -0.1F;

		// Thighs forward and crossed in front; left leg 1px higher to avoid z-fighting.
		rightLeg.y = 22.0F;
		leftLeg.y = 21.0F;
		rightLeg.z = 0.1F;
		leftLeg.z = 0.1F;
		rightLeg.xRot = -1.35F;
		leftLeg.xRot = -1.35F;
		rightLeg.yRot = -0.75F;
		leftLeg.yRot = 0.75F;
		rightLeg.zRot = 0.0F;
		leftLeg.zRot = 0.0F;
	}
}
