package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.HeavenPassives;
import com.example.defyingtheheavens.QiElement;
import com.example.defyingtheheavens.SpellAura;
import com.example.defyingtheheavens.SpellAuras;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The element spells' footing on fluids (SpellAuras): Crimson Tempering bears its wearer on lava, Azure Breathing on water (sneak
 * to sink in); and so does water's mastery (HeavenPassives#masters, Water-Treading: the user's ask, 2026-10-25, "you should just be able
 * to walk on water normally"), and a fall onto the water it walks does it no harm, as a fall into water does none. Asked on both sides:
 * a player's client moves them, so it reads the auras and the mastery it was told (SpellAuras#holds, HeavenPassives#clientView).
 * <p>
 * Water is stood on only at its face (the user's report, 2026-10-25: "if I go underwater I cant jump or do much its just really buggy"):
 * taken for ground whatever the depth, one who had sunk walked the bottom as on land, unable to swim, jump or come up. Now one under it
 * swims as anyone does; and one whose head comes up near its face (sneak not held) is borne up onto it ({@link #dth$riseOntoWater}).
 */
@Mixin(LivingEntity.class)
public abstract class SpellLivingEntityMixin {
	/**
	 * How deep (blocks of water over the feet) one may be and still be stood up on it: the depth one stands at on its face (a source's
	 * face is 8/9 of a block up, its footing half a block: LiquidBlock's), and no deeper. Vanilla's own line for a jump out of water.
	 */
	@Unique
	private static final double STANDING_DEPTH = 0.4;
	/** Within this much (blocks) of the eyes coming up out of the water, one is borne up onto its face; deeper, one swims. */
	@Unique
	private static final double BORNE_UP_WITHIN = 0.6;
	/** How fast (blocks a tick) one rises up onto the water's face, borne up. */
	@Unique
	private static final double RISING = 0.22;

	@Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
	private void dth$walkOnFluid(FluidState fluid, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (fluid.is(FluidTags.LAVA) && SpellAuras.holds(self, SpellAura.CRIMSON_TEMPERING)) cir.setReturnValue(true);
		else if (fluid.is(FluidTags.WATER) && dth$treadsWater(self) && self.getFluidHeight(FluidTags.WATER) <= STANDING_DEPTH) cir.setReturnValue(true);
	}

	/**
	 * One who treads water and has sunk into it, their head near its face, borne up onto it: they rise until they stand on it (where
	 * {@link #dth$walkOnFluid} takes it for ground), rather than swimming there unable to climb out. Deeper down they swim; sneaking they sink.
	 * Run where the body is moved (a player's own client, a mob's server).
	 */
	@Inject(method = "travel", at = @At("HEAD"))
	private void dth$riseOntoWater(Vec3 input, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!self.isControlledByLocalInstance() || !self.isInWater() || !dth$treadsWater(self)) return;
		double depth = self.getFluidHeight(FluidTags.WATER);
		if (depth <= STANDING_DEPTH || depth > self.getEyeHeight() + BORNE_UP_WITHIN) return;
		Vec3 v = self.getDeltaMovement();
		if (v.y < RISING) self.setDeltaMovement(v.x, RISING, v.z);
		self.resetFallDistance();
	}

	/** Whether {@code self} walks on water now: Azure Breathing on it, or water mastered, and not sneaking to go in. */
	@Unique
	private static boolean dth$treadsWater(LivingEntity self) {
		return !self.isShiftKeyDown() && (SpellAuras.holds(self, SpellAura.AZURE_BREATHING) || HeavenPassives.masters(self, QiElement.WATER));
	}

	@Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
	private void dth$fallOntoWater(float distance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.level().getFluidState(self.blockPosition().below()).is(FluidTags.WATER) && HeavenPassives.masters(self, QiElement.WATER)
				|| self.isInWater() && dth$treadsWater(self)) {
			cir.setReturnValue(false);
		}
	}
}
