package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.Formations;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No monster is born inside a raised formation, sect barrier or Concealment Barrier alike: the world's own spawning (in the
 * dark, at night, in caves under the grounds) passes over every spot a formation encloses. The mob has already been placed
 * where it would spawn when this is asked (see Formations#shelters).
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
	@Inject(method = "isValidPositionForMob", at = @At("HEAD"), cancellable = true)
	private static void dth$notInsideAFormation(ServerLevel level, Mob mob, double distanceSqr, CallbackInfoReturnable<Boolean> cir) {
		if (Formations.isMonster(mob) && Formations.shelters(level, mob.blockPosition())) cir.setReturnValue(false);
	}
}
