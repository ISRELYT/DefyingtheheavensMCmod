package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.Incorporeal;
import com.example.defyingtheheavens.SoulProjection;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A projected soul (see SoulProjection), and one of the dead outside the Netherworld (a Ghost King in the living world), or one of
 * the living in a Ghost King's form there (TwoPaths#inGhostForm), is sent only to the clients of those who perceive it
 * ({@link SoulProjection#perceives}): the entity tracker asks this for every player
 * near it, so everyone else's client never learns it is there, and no client change can reveal it. And it can't throw anything
 * from its inventory: a soul carries nothing in hand (a Ghost King's hands are its own).
 */
@Mixin(ServerPlayer.class)
public abstract class SoulServerPlayerMixin {
	@Inject(method = "broadcastToPlayer", at = @At("HEAD"), cancellable = true)
	private void dth$onlyToThoseWhoPerceive(ServerPlayer viewer, CallbackInfoReturnable<Boolean> cir) {
		if (!SoulProjection.broadcastsTo((ServerPlayer) (Object) this, viewer)) cir.setReturnValue(false);
	}

	@Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
	private void dth$soulDropsNothing(boolean wholeStack, CallbackInfoReturnable<Boolean> cir) {
		if (Incorporeal.is((ServerPlayer) (Object) this)) cir.setReturnValue(false);
	}
}
