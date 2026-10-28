package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.SectMissions;
import com.example.defyingtheheavens.Wishes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * What preys on a settlement (a Merit wish's threat: Wishes) or on a sect's people (a mission's quarry: SectMissions) leaves its spoils only
 * to the one who took it on (Wishes#spoilsDrop, SectMissions#spoilsDrop): slain by anything else, its loot, gear and experience stay with it.
 */
@Mixin(LivingEntity.class)
public abstract class ThreatLootMixin {
	@Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
	private void dth$spoilsToTheTaker(DamageSource source, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!Wishes.spoilsDrop(self, source) || !SectMissions.spoilsDrop(self, source)) ci.cancel();
	}
}
