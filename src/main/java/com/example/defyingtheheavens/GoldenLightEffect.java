package com.example.defyingtheheavens;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

/**
 * Golden Light: a Golden Light Talisman's shell (Talismans#goldenLight), harm soaked up as absorption (set as it is burnt: as much as a share of
 * its drawer's realm's whole health). When it ends, what is left of the shell goes with it (unless vanilla's own Absorption holds some too).
 * Golden glints drift about one while it lasts.
 */
public class GoldenLightEffect extends MobEffect {
	public GoldenLightEffect() {
		super(MobEffectCategory.BENEFICIAL, 0xF5D76E);
	}

	@Override
	public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
		super.removeAttributeModifiers(entity, attributes, amplifier);
		if (!entity.hasEffect(MobEffects.ABSORPTION)) entity.setAbsorptionAmount(0);
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		if (!(entity.level() instanceof ServerLevel level) || entity.getAbsorptionAmount() <= 0) return;
		level.sendParticles(ParticleTypes.WAX_ON, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 2,
				entity.getBbWidth() * 0.6, entity.getBbHeight() * 0.4, entity.getBbWidth() * 0.6, 0.02);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return duration % 10 == 0;
	}
}
