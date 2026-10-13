package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.Incorporeal;
import com.example.defyingtheheavens.SoulProjection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * What every entity is asked that a projected soul (see SoulProjection) or a ghost (Incorporeal) answers its own way: it sets
 * off no pressure plate or tripwire, no blast throws it about, no portal takes it to another dimension; riding a body it
 * possesses, a soul looks out through that body's eyes. (A ghost's silent tread is SoulPlayerMixin's: the player's own
 * movement emission.)
 */
@Mixin(Entity.class)
public abstract class SoulEntityMixin {
	@Inject(method = "isIgnoringBlockTriggers", at = @At("HEAD"), cancellable = true)
	private void dth$soulTriggersNothing(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Player player && Incorporeal.is(player)) cir.setReturnValue(true);
	}

	@Inject(method = "ignoreExplosion", at = @At("HEAD"), cancellable = true)
	private void dth$soulIgnoresBlasts(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Player player && Incorporeal.is(player)) cir.setReturnValue(true);
	}

	@Inject(method = "canChangeDimensions", at = @At("HEAD"), cancellable = true)
	private void dth$soulKeepsToItsRealm(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Player player && Incorporeal.is(player)) cir.setReturnValue(false);
	}

	/** A soul riding the body it possesses sits at that body's eyes, so its view is the body's (whatever the body's size). */
	@Inject(method = "positionRider(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
	private void dth$soulBehindTheEyes(Entity passenger, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (!(passenger instanceof Player player) || !self.hasPassenger(passenger) || !SoulProjection.isSoul(player)) return;
		passenger.setPos(self.getX(), self.getY() + self.getEyeHeight() - player.getEyeHeight(), self.getZ());
		ci.cancel();
	}
}
