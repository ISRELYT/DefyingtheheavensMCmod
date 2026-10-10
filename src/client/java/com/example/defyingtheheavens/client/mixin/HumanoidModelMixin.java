package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.InnerBodyEntity;
import com.example.defyingtheheavens.client.ClientMeditationTracker;
import com.example.defyingtheheavens.client.LotusPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Poses meditating players (and bodies left behind by souls in the Inner Realm) cross-legged on the ground. Injected at the TAIL of HumanoidModel#setupAnim,
 * which PlayerModel calls before copying limbs onto the sleeve/pants layers, so the outer layers follow automatically.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void dth$lotusPose(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
							   float netHeadYaw, float headPitch, CallbackInfo ci) {
		boolean meditating = entity instanceof Player && ClientMeditationTracker.isMeditating(entity.getUUID());
		// A body left behind while its soul is in the Inner Realm sits the same way.
		if (!meditating && !(entity instanceof InnerBodyEntity)) return;
		LotusPose.apply((HumanoidModel<?>) (Object) this);
	}
}
