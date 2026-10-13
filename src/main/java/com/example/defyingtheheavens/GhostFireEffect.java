package com.example.defyingtheheavens;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Ghost fire (Ghost Fire Scripture, see Spells): cold cyan fire that burns {@link Spells#GHOST_FIRE_DAMAGE} a second as the
 * dead's own harm (so it reaches ghosts), three times that on the dead themselves. Not vanilla's burning, whose orange
 * flames it would draw: soul-fire flames instead.
 */
public class GhostFireEffect extends MobEffect {
	public GhostFireEffect() {
		super(MobEffectCategory.HARMFUL, 0x4FE3E8);
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		if (!(entity.level() instanceof ServerLevel level)) return;
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 3,
				entity.getBbWidth() * 0.4, entity.getBbHeight() * 0.35, entity.getBbWidth() * 0.4, 0.01);
		if (entity.tickCount % 20 != 0) return;
		boolean dead = GhostSystem.isDead(entity); // a player's ghost, a ghost cultivator, a Hungry Ghost
		entity.hurt(ModDamageTypes.ghostly(level, null), Spells.GHOST_FIRE_DAMAGE * (dead ? Spells.GHOST_FIRE_DEAD_FACTOR : 1));
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true; // every tick: the flames; the harm once a second (above)
	}
}
