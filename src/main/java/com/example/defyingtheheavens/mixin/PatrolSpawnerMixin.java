package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.Formations;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No pillager patrol forms up inside a raised formation (they spawn near players, and so often right inside a sect). */
@Mixin(PatrolSpawner.class)
public abstract class PatrolSpawnerMixin {
	@Inject(method = "spawnPatrolMember", at = @At("HEAD"), cancellable = true)
	private void dth$notInsideAFormation(ServerLevel level, BlockPos pos, RandomSource random, boolean leader, CallbackInfoReturnable<Boolean> cir) {
		if (Formations.shelters(level, pos)) cir.setReturnValue(false);
	}
}
