package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.SoulProjection;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Projected souls and the bodies they possess (see SoulProjection). A soul never heals (its hearts only run down), a totem in its
 * hand doesn't hold off its death (the totem is the body's), it pushes nothing and nothing pushes it, and a blow on it lands only
 * as much as the attacker can see of it ({@link SoulProjection#harmFactor}). A possessed body moves as its soul steers it: the
 * soul's inputs, the soul's facing, and the body's own speed (what vanilla does for a ridden horse, for anything possessed).
 */
@Mixin(LivingEntity.class)
public abstract class SoulLivingEntityMixin {
	@Shadow protected boolean jumping;

	@Shadow protected abstract void jumpFromGround();

	@Inject(method = "heal", at = @At("HEAD"), cancellable = true)
	private void dth$soulNeverHeals(float amount, CallbackInfo ci) {
		if ((Object) this instanceof Player player && SoulProjection.isSoul(player)) ci.cancel();
	}

	@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
	private float dth$soulTakesWhatIsSeen(float amount, DamageSource source, float original) {
		return SoulProjection.scaleSoulDamage((LivingEntity) (Object) this, source, amount);
	}

	@Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
	private void dth$noTotemForASoul(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Player player && SoulProjection.isSoul(player)) cir.setReturnValue(false);
	}

	@Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
	private void dth$soulIsNotPushed(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Player player && SoulProjection.isSoul(player)) cir.setReturnValue(false);
	}

	@Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
	private void dth$soulPushesNothing(CallbackInfo ci) {
		if ((Object) this instanceof Player player && SoulProjection.isSoul(player)) ci.cancel();
	}

	@Inject(method = "getRiddenInput", at = @At("HEAD"), cancellable = true)
	private void dth$possessedSteps(Player player, Vec3 travel, CallbackInfoReturnable<Vec3> cir) {
		if (SoulProjection.possessorOf((LivingEntity) (Object) this) == player) cir.setReturnValue(new Vec3(player.xxa, 0, player.zza));
	}

	@Inject(method = "tickRidden", at = @At("HEAD"), cancellable = true)
	private void dth$possessedFacing(Player player, Vec3 input, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (SoulProjection.possessorOf(self) != player) return;
		self.setYRot(player.getYRot());
		self.yRotO = self.getYRot();
		self.setXRot(player.getXRot());
		self.yBodyRot = self.getYRot();
		self.yHeadRot = self.getYRot();
		if (((SoulLivingEntityMixin) (Object) player).jumping && self.onGround()) jumpFromGround();
		ci.cancel();
	}

	@Inject(method = "getRiddenSpeed", at = @At("HEAD"), cancellable = true)
	private void dth$possessedPace(Player player, CallbackInfoReturnable<Float> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (SoulProjection.possessorOf(self) == player) cir.setReturnValue((float) self.getAttributeValue(Attributes.MOVEMENT_SPEED));
	}
}
