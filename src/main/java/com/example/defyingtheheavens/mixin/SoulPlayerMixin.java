package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.SoulProjection;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A projected nascent soul (see SoulProjection) is a player that, like a spectator, passes through blocks and is never on the
 * ground; it keeps a standing pose inside walls, isn't carried by water, picks nothing up, and no mob takes it for prey. Its
 * movement is the client's own free flight (ClientSoul); the server doesn't simulate it.
 */
@Mixin(Player.class)
public abstract class SoulPlayerMixin {
	/** Player#tick asks whether it is a spectator twice: for no-clip, and to leave the ground. A soul is both. */
	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z"))
	private boolean dth$soulPassesThrough(boolean spectator) {
		return spectator || SoulProjection.isSoul((Player) (Object) this);
	}

	@Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
	private void dth$soulStandsUpright(CallbackInfo ci) {
		Player self = (Player) (Object) this;
		if (SoulProjection.isSoul(self)) {
			self.setPose(Pose.STANDING);
			ci.cancel();
		}
	}

	@Inject(method = "isPushedByFluid", at = @At("HEAD"), cancellable = true)
	private void dth$soulIgnoresCurrents(CallbackInfoReturnable<Boolean> cir) {
		if (SoulProjection.isSoul((Player) (Object) this)) cir.setReturnValue(false);
	}

	@Inject(method = "canBeSeenAsEnemy", at = @At("HEAD"), cancellable = true)
	private void dth$noPreyForMobs(CallbackInfoReturnable<Boolean> cir) {
		if (SoulProjection.isSoul((Player) (Object) this)) cir.setReturnValue(false);
	}

	/** Items, experience and arrows lying about: a soul touches none of them. */
	@Inject(method = "touch", at = @At("HEAD"), cancellable = true)
	private void dth$soulTouchesNothing(Entity entity, CallbackInfo ci) {
		if (SoulProjection.isSoul((Player) (Object) this)) ci.cancel();
	}

	/** The server never moves a soul itself (no gravity to apply): its client flies it. */
	@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
	private void dth$soulFliesOnItsClient(Vec3 input, CallbackInfo ci) {
		Player self = (Player) (Object) this;
		if (!self.level().isClientSide && SoulProjection.isSoul(self)) ci.cancel();
	}
}
