package com.example.defyingtheheavens;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Quelled: one of the dead bound by a Ghost-Quelling Talisman (see GhostQuellingTalismanItem), held where it stands. Its speed
 * is taken (on both sides, the effect's own attribute), it can't jump (SoulLivingEntityMixin), a ghost can't fly
 * (QiFlight) or dash (Spells); a mob is held still, falling only. It can still strike what is in reach. Gold sparks circle it
 * while it lasts, and the talisman shows on its brow (the client's, SoulRenderer).
 */
public class QuelledEffect extends MobEffect {
	private static final String SPEED_ID = "5d2c7a1e-8f43-4b6a-9e0d-2a7c4f1b3e85";

	public QuelledEffect() {
		super(MobEffectCategory.HARMFUL, 0xE8C547);
		addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_ID, -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		if (!(entity.level() instanceof ServerLevel level)) return;
		// A player's own client moves them (its speed gone, it can't jump or fly); a mob the server holds.
		if (!(entity instanceof Player)) {
			Vec3 motion = entity.getDeltaMovement();
			entity.setDeltaMovement(0, Math.min(0, motion.y), 0);
		}
		if (entity.tickCount % 4 == 0) {
			double angle = entity.tickCount * 0.35;
			level.sendParticles(ParticleTypes.WAX_ON, entity.getX() + Math.cos(angle) * 0.6, entity.getY() + 0.2 + (entity.tickCount % 20) * 0.08,
					entity.getZ() + Math.sin(angle) * 0.6, 1, 0, 0, 0, 0);
		}
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}
}
