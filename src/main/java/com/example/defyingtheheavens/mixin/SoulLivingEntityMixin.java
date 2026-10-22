package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.GhostSystem;
import com.example.defyingtheheavens.HeavenSpells;
import com.example.defyingtheheavens.Incorporeal;
import com.example.defyingtheheavens.Physiques;
import com.example.defyingtheheavens.SoulProjection;
import com.example.defyingtheheavens.SparSystem;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Projected souls and the bodies they possess (see SoulProjection). A soul out of its body never heals by any means (what it loses
 * it carries home: it mends only resting in a body, SoulProjection#mendSouls, which sets a beast-riding soul's health itself), a totem in its
 * hand doesn't hold off its death (the totem is the body's), it pushes nothing and nothing pushes it, and a blow on it lands only
 * as much as the attacker can see of it ({@link SoulProjection#harmFactor}). A possessed body moves as its soul steers it: the
 * soul's inputs and facing (what vanilla does for a ridden horse, for anything possessed), but at the body's own pace
 * ({@link SoulProjection#hostStep}), in the air as on the ground; and a flier flies, where the soul looks
 * ({@link SoulProjection#flightWish}).
 */
@Mixin(LivingEntity.class)
public abstract class SoulLivingEntityMixin {
	@Shadow protected boolean jumping;

	/** A possessed phantom's glide (SoulProjection#glide), kept by its soul's client, which flies it. */
	@Unique private float dth$glide;
	/** A possessed flier's flight, as its last tick left it (after what it ran into), and that tick. */
	@Unique private Vec3 dth$flight;
	@Unique private int dth$flightTick;

	@Shadow protected abstract void jumpFromGround();

	@Inject(method = "heal", at = @At("HEAD"), cancellable = true)
	private void dth$soulNeverHeals(float amount, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof Player player && SoulProjection.isSoul(player)) ci.cancel();
		// A heaven spell's hold on healing: a seared wound, a strangler fig's host (HeavenSpells#allowHeal).
		else if (!self.level().isClientSide && amount > 0 && !HeavenSpells.allowHeal(self, amount)) ci.cancel();
	}

	/**
	 * A heaven spell's ward against effects: the dawn's light, a severed fate (HeavenSpells#allowEffect); and a Jade Poison Body's,
	 * against poison and wither (Physiques#allowEffect).
	 */
	@Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
	private void dth$heavenWardsEffects(net.minecraft.world.effect.MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!self.level().isClientSide && (!HeavenSpells.allowEffect(self, effect) || !Physiques.allowEffect(self, effect))) cir.setReturnValue(false);
	}

	/** The health before a blow, for what it took (a Blood Demon Body heals by a share of it: Physiques#onHurt). */
	@Inject(method = "hurt", at = @At("HEAD"))
	private void dth$healthBefore(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir, @Share("health") LocalFloatRef health) {
		health.set(((LivingEntity) (Object) this).getHealth());
	}

	@Inject(method = "hurt", at = @At("RETURN"))
	private void dth$bloodDrawn(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir, @Share("health") LocalFloatRef health) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!self.level().isClientSide && cir.getReturnValueZ()) Physiques.onHurt(self, source, health.get() - self.getHealth());
	}

	@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
	private float dth$soulTakesWhatIsSeen(float amount, DamageSource source, float original) {
		LivingEntity self = (LivingEntity) (Object) this;
		// The lifespan tribulation's strike lands whole: no ward, aura or passive of the mod's turns it (LifespanTribulation).
		if (!self.level().isClientSide && com.example.defyingtheheavens.LifespanTribulation.isStriking(self)) return amount;
		// First what the blow is as dealt: a Primordial Chaos Body's spells harder (Physiques#outgoing).
		if (!self.level().isClientSide) amount = Physiques.outgoing(source, amount);
		// A ghost takes only what reaches the dead, the living's blows by how clearly they see it (see GhostSystem).
		float scaled = self instanceof ServerPlayer ghost && GhostSystem.isGhost(ghost) ? GhostSystem.scaleDamage(ghost, source, amount)
				: SoulProjection.scaleSoulDamage(self, source, amount);
		// A spar partner's blow is pulled (SparSystem#BLOW_SHARE).
		if (!self.level().isClientSide) scaled = SparSystem.pulled(self, source, scaled);
		// Then what the heaven methods' wards and the earth's passive make of it (HeavenSpells#incoming), on the server.
		return scaled > 0 && !self.level().isClientSide ? HeavenSpells.incoming(self, source, scaled) : scaled;
	}

	@Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
	private void dth$noTotemForASoul(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Player player && SoulProjection.isSoul(player)) cir.setReturnValue(false);
	}

	@Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
	private void dth$soulIsNotPushed(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Player player && Incorporeal.untouchable(player)) cir.setReturnValue(false);
	}

	@Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
	private void dth$soulPushesNothing(CallbackInfo ci) {
		if ((Object) this instanceof Player player && Incorporeal.untouchable(player)) ci.cancel();
	}

	/** One of the dead bound by a Ghost-Quelling Talisman can't jump (see QuelledEffect). Both sides: a player's client jumps it. */
	@Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
	private void dth$quelledCantJump(CallbackInfo ci) {
		if (((LivingEntity) (Object) this).hasEffect(com.example.defyingtheheavens.ModEffects.QUELLED)) ci.cancel();
	}

	/**
	 * A possessed flier flies on its soul's client (which moves whatever its player rides) in place of its own travel and its
	 * weight: toward where the soul's keys send it, at its own pace (SoulProjection#flightWish, #flightPace, #flightVelocity).
	 */
	@Inject(method = "travelRidden", at = @At("HEAD"), cancellable = true)
	private void dth$possessedFlies(Player player, Vec3 input, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!self.isControlledByLocalInstance() || SoulProjection.possessorOf(self) != player || !SoulProjection.flies(self)) return;
		float turned = Mth.wrapDegrees(player.getYRot() - self.getYRot());
		dth$face(self, player);
		Vec3 wish = SoulProjection.flightWish(player.xxa, player.zza, ((SoulLivingEntityMixin) (Object) player).jumping, player.getLookAngle(),
				player.getYRot());
		dth$glide = SoulProjection.glide(Math.max(dth$glide, SoulProjection.GLIDE_MIN), wish.lengthSqr() > 1.0E-4 ? turned : 180);
		// It flies on from its last tick's flight. What its own body did to its motion since is the body's way of falling, not
		// flight (a blaze's and a parrot's slowed fall, a bat's damped flutter), and only ever slows it; aiStep has slowed it by
		// 0.98 besides, as everything the client moves. A blow or a push that left it faster carries over, as on the ground.
		Vec3 now = self.getDeltaMovement().scale(1 / 0.98);
		boolean fresh = dth$flight == null || self.tickCount - dth$flightTick > 1;
		Vec3 carried = fresh || now.lengthSqr() > dth$flight.lengthSqr() + 1.0E-4 ? now : dth$flight;
		Vec3 v = SoulProjection.flightVelocity(carried, wish, SoulProjection.flightPace(self, dth$glide));
		self.setDeltaMovement(v);
		// It meets blocks as the server will (even a vex): the server holds a ridden body to them, and would put it back.
		boolean phasing = self.noPhysics;
		self.noPhysics = false;
		self.move(MoverType.SELF, v);
		self.noPhysics = phasing;
		dth$flight = self.getDeltaMovement(); // as it is after what it ran into
		dth$flightTick = self.tickCount;
		self.resetFallDistance();
		self.calculateEntityAnimation(self instanceof FlyingAnimal);
		ci.cancel();
	}

	@Inject(method = "getRiddenInput", at = @At("HEAD"), cancellable = true)
	private void dth$possessedSteps(Player player, Vec3 travel, CallbackInfoReturnable<Vec3> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (SoulProjection.possessorOf(self) == player) cir.setReturnValue(SoulProjection.hostStep(self, player.xxa, player.zza));
	}

	@Inject(method = "tickRidden", at = @At("HEAD"), cancellable = true)
	private void dth$possessedFacing(Player player, Vec3 input, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (SoulProjection.possessorOf(self) != player) return;
		dth$face(self, player);
		// A flier's jump is a climb (dth$possessedFlies), not a hop.
		if (((SoulLivingEntityMixin) (Object) player).jumping && self.onGround() && !SoulProjection.flies(self)) jumpFromGround();
		ci.cancel();
	}

	/** A possessed body faces as its soul does. */
	@Unique
	private static void dth$face(LivingEntity self, Player player) {
		self.setYRot(player.getYRot());
		self.yRotO = self.getYRot();
		self.setXRot(player.getXRot());
		self.yBodyRot = self.getYRot();
		self.yHeadRot = self.getYRot();
	}

	@Inject(method = "getRiddenSpeed", at = @At("HEAD"), cancellable = true)
	private void dth$possessedPace(Player player, CallbackInfoReturnable<Float> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (SoulProjection.possessorOf(self) == player) cir.setReturnValue((float) self.getAttributeValue(Attributes.MOVEMENT_SPEED));
	}

	/** Off the ground a possessed body steers as it does by itself (vanilla gives a player-ridden one a tenth of its speed instead). */
	@Inject(method = "getFlyingSpeed", at = @At("HEAD"), cancellable = true)
	private void dth$possessedInTheAir(CallbackInfoReturnable<Float> cir) {
		if (SoulProjection.isPossessed((LivingEntity) (Object) this)) cir.setReturnValue(0.02f);
	}
}
