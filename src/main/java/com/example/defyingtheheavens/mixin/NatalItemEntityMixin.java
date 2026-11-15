package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.NatalTreasures;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A natal treasure lying on the ground (NatalTreasures) waits for its owner's call: it never despawns, and neither fire, lava, a blast nor
 * anything else harms it there (it would otherwise be gone with no way to call it back, its owner's core bound to nothing).
 */
@Mixin(ItemEntity.class)
public abstract class NatalItemEntityMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void dth$natalWaits(CallbackInfo ci) {
		ItemEntity self = (ItemEntity) (Object) this;
		if (!self.level().isClientSide && self.tickCount % 20 == 0 && NatalTreasures.isNatal(self.getItem())) self.setUnlimitedLifetime();
	}

	@Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
	private void dth$natalUnharmed(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (NatalTreasures.isNatal(((ItemEntity) (Object) this).getItem())) cir.setReturnValue(false);
	}

	@Inject(method = "fireImmune", at = @At("HEAD"), cancellable = true)
	private void dth$natalUnburnt(CallbackInfoReturnable<Boolean> cir) {
		if (NatalTreasures.isNatal(((ItemEntity) (Object) this).getItem())) cir.setReturnValue(true);
	}
}
