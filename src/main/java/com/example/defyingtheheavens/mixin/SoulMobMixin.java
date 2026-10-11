package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.SoulProjection;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A possessed body (see SoulProjection#tryPossess) answers to the soul riding it, as a saddled horse to its rider: the soul
 * controls it (its client moves it, see SoulLivingEntityMixin), and its own mind (goals, targets, pathfinding) is silent until the
 * soul leaves.
 */
@Mixin(Mob.class)
public abstract class SoulMobMixin {
	@Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
	private void dth$soulControls(CallbackInfoReturnable<LivingEntity> cir) {
		Player possessor = SoulProjection.possessorOf((Mob) (Object) this);
		if (possessor != null) cir.setReturnValue(possessor);
	}

	@Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
	private void dth$ownMindSilent(CallbackInfo ci) {
		if (SoulProjection.isPossessed((Mob) (Object) this)) ci.cancel();
	}
}
