package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.HeavenPassives;
import com.example.defyingtheheavens.QiElement;
import com.example.defyingtheheavens.SpellAura;
import com.example.defyingtheheavens.SpellAuras;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The element spells' footing on fluids (SpellAuras): Crimson Tempering bears its wearer on lava, Azure Breathing on water (sneak
 * to sink in); and so does water's mastery (HeavenPassives#masters, Water-Treading: the user's ask, 2026-10-25, "you should just be able
 * to walk on water normally"), and a fall onto the water it walks does it no harm, as a fall into water does none. Asked on both sides:
 * a player's client moves them, so it reads the auras and the mastery it was told (SpellAuras#holds, HeavenPassives#clientView).
 */
@Mixin(LivingEntity.class)
public abstract class SpellLivingEntityMixin {
	@Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
	private void dth$walkOnFluid(FluidState fluid, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (fluid.is(FluidTags.LAVA) && SpellAuras.holds(self, SpellAura.CRIMSON_TEMPERING)) cir.setReturnValue(true);
		else if (fluid.is(FluidTags.WATER) && !self.isShiftKeyDown() && (SpellAuras.holds(self, SpellAura.AZURE_BREATHING) || HeavenPassives.masters(self, QiElement.WATER))) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
	private void dth$fallOntoWater(float distance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.level().getFluidState(self.blockPosition().below()).is(FluidTags.WATER) && HeavenPassives.masters(self, QiElement.WATER)) {
			cir.setReturnValue(false);
		}
	}
}
