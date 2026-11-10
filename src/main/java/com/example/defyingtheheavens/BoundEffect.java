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
 * Bound: held where one stands by a Binding Talisman (Talismans#bind), the living's counterpart of the dead's Quelled (QuelledEffect). Its
 * speed taken (both sides, the effect's own attribute), it can't jump (SoulLivingEntityMixin) or fly (Talismans weighs a bound player down,
 * QiFlight#weighDown); a mob is held still, falling only. It can still strike what is in reach. Ochre motes circle it while it lasts.
 */
public class BoundEffect extends MobEffect {
	private static final String SPEED_ID = "7b1e9c34-2a6d-4f58-8e0b-3c9d5a7f2e61";

	public BoundEffect() {
		super(MobEffectCategory.HARMFUL, 0xC8963C);
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
			level.sendParticles(ParticleTypes.WAX_OFF, entity.getX() + Math.cos(angle) * 0.6, entity.getY() + 0.2 + (entity.tickCount % 20) * 0.08,
					entity.getZ() + Math.sin(angle) * 0.6, 1, 0, 0, 0, 0);
		}
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}
}
