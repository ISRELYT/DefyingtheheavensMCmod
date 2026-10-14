package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.GhostSystem;
import com.example.defyingtheheavens.Incorporeal;
import com.example.defyingtheheavens.SoulProjection;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
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
		if (Incorporeal.untouchable((Player) (Object) this)) cir.setReturnValue(false);
	}

	/**
	 * No mob takes a soul or one of the dead for prey, nor one of the living in a Ghost King's form (GhostSystem#seenAsDead): the
	 * living world doesn't see them. The dead's own kind see the dead (GhostSystem#deadMayAttack, their canAttack).
	 */
	@Inject(method = "canBeSeenAsEnemy", at = @At("HEAD"), cancellable = true)
	private void dth$noPreyForMobs(CallbackInfoReturnable<Boolean> cir) {
		Player self = (Player) (Object) this;
		if (Incorporeal.untouchable(self) || GhostSystem.seenAsDead(self)) cir.setReturnValue(false);
	}

	/**
	 * Items, experience and arrows lying about: a soul touches none of them. A ghost takes the experience that comes to it (drawn
	 * to the nearest player, it would only circle the ghost for ever), and the dead's money and writings (Incorporeal#mayCarry), but
	 * nothing else.
	 */
	@Inject(method = "touch", at = @At("HEAD"), cancellable = true)
	private void dth$soulTouchesNothing(Entity entity, CallbackInfo ci) {
		Player self = (Player) (Object) this;
		if (entity instanceof ExperienceOrb && GhostSystem.isGhost(self) && !SoulProjection.isSoul(self)) return;
		// What a ghost takes up (Incorporeal#mayCarry): the dead's money and writings, and a Marrow Cleansing Elixir.
		if (entity instanceof net.minecraft.world.entity.item.ItemEntity item && Incorporeal.mayCarry(self, item.getItem())) return;
		if (Incorporeal.is(self)) ci.cancel();
	}

	/** The server never moves a soul itself (no gravity to apply): its client flies it. */
	@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
	private void dth$soulFliesOnItsClient(Vec3 input, CallbackInfo ci) {
		Player self = (Player) (Object) this;
		if (!self.level().isClientSide && SoulProjection.isSoul(self)) ci.cancel();
	}

	/**
	 * A ghost's tread makes no sound: no footsteps (on any block, through carpet and snow, wading), no splashing. Both sides, so
	 * the ghost's own client is as quiet as everyone else's. EVENTS rather than NONE: only a walker that emits something counts
	 * the distance it walks, which the view's bobbing goes by.
	 */
	@ModifyReturnValue(method = "getMovementEmission", at = @At("RETURN"))
	private Entity.MovementEmission dth$ghostsTreadSilently(Entity.MovementEmission emission) {
		return emission == Entity.MovementEmission.ALL && GhostSystem.isGhost((Player) (Object) this) ? Entity.MovementEmission.EVENTS : emission;
	}

	/** Nor does a ghost land with a thud: a fall is nothing to it (and no fall harms it, see GhostSystem#allowDamage). */
	@Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
	private void dth$ghostsLandSilently(float distance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		if (GhostSystem.isGhost((Player) (Object) this)) cir.setReturnValue(false);
	}
}
