package com.example.defyingtheheavens.client.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The model's own swing of a blow, for the dead, whose drifting pose is set over it (SoulRenderer#swingPose). */
@Mixin(HumanoidModel.class)
public interface HumanoidModelInvoker {
	@Invoker("setupAttackAnimation")
	void dth$setupAttackAnimation(LivingEntity entity, float ageInTicks);
}
