package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.Formations;
import com.example.defyingtheheavens.SectBarrierBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An arrow, a trident, a snowball or a fireball that strikes a formation's barrier makes it ripple and ring (Formations#struck). */
@Mixin(Projectile.class)
public abstract class ProjectileBarrierMixin {
	@Inject(method = "onHitBlock", at = @At("HEAD"))
	private void dth$rippleTheBarrier(BlockHitResult hit, CallbackInfo ci) {
		Projectile self = (Projectile) (Object) this;
		if (self.level() instanceof ServerLevel level && level.getBlockState(hit.getBlockPos()).getBlock() instanceof SectBarrierBlock) {
			Formations.struck(level, hit.getBlockPos(), 0.7f);
		}
	}
}
