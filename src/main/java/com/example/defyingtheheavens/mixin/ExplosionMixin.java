package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.SectManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * A blast doesn't break into a sect's treasury while its elders keep it (SectManager#treasurySealed): the seal that holds its
 * chests shut holds its walls and chests against fire and blast too, so nothing can be blown open and spilled.
 */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {
	@Shadow @Final private Level level;

	@Shadow public abstract List<BlockPos> getToBlow();

	@Inject(method = "explode", at = @At("TAIL"))
	private void dth$spareSealedTreasuries(CallbackInfo ci) {
		if (level instanceof ServerLevel server) SectManager.spareSealedTreasuries(server, getToBlow());
	}
}
