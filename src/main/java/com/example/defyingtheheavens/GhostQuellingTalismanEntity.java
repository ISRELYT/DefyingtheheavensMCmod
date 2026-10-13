package com.example.defyingtheheavens;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A thrown Ghost-Quelling Talisman (see GhostQuellingTalismanItem), drawn as the item turning in flight. It passes through the
 * living, who are nothing to it, and through a projected soul; it strikes the dead (SoulProjectileMixin lets it reach a
 * player's ghost, which every other projectile passes through). It is spent on whatever it hits, in a puff of paper.
 */
public class GhostQuellingTalismanEntity extends ThrowableItemProjectile {
	public GhostQuellingTalismanEntity(EntityType<? extends GhostQuellingTalismanEntity> type, Level level) {
		super(type, level);
	}

	public GhostQuellingTalismanEntity(Level level, LivingEntity owner) {
		super(ModEntities.GHOST_QUELLING_TALISMAN, owner, level);
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.GHOST_QUELLING_TALISMAN;
	}

	/** Only the dead: a projected soul is no ghost, and the living let it by. */
	@Override
	protected boolean canHitEntity(Entity target) {
		if (!GhostSystem.isDead(target) || target instanceof Player player && SoulProjection.isSoul(player)) return false;
		return super.canHitEntity(target);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (hit.getEntity() instanceof LivingEntity target) GhostQuellingTalismanItem.strike(target, this, getOwner());
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (level() instanceof ServerLevel level) {
			level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, getItem()), getX(), getY(), getZ(), 8, 0.1, 0.1, 0.1, 0.08);
			discard();
		}
	}
}
