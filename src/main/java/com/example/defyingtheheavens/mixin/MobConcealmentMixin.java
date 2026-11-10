package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.Talismans;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * One concealed by a Concealment Talisman (Talismans#conceal) is never taken up as a mob's target (a beast's, a cultivator's, any monster's),
 * unless the mob stands two realms or more above the talisman or was struck by them (Talismans#unseenBy). Striking anything ends it
 * (Talismans#struck).
 */
@Mixin(Mob.class)
public abstract class MobConcealmentMixin {
	@Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
	private void dth$concealedUnnoticed(LivingEntity target, CallbackInfo ci) {
		Mob self = (Mob) (Object) this;
		if (target != null && !self.level().isClientSide && Talismans.unseenBy(self, target)) ci.cancel();
	}
}
