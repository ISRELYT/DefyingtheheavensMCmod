package com.example.defyingtheheavens;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * A thrown talisman (TalismanItem: Fire, Five Thunder, Frost, Binding), drawn as the item turning in flight (a snowball's renderer). It keeps how
 * strong it was in its thrower's hands (its rank and its work's worth: Talismans#effectiveRank) and does its pattern's work where it lands
 * (Talismans#land), spent in a puff of paper.
 */
public class TalismanEntity extends ThrowableItemProjectile {
	private int rank;
	private float power = 1;

	public TalismanEntity(EntityType<? extends TalismanEntity> type, Level level) {
		super(type, level);
	}

	public TalismanEntity(Level level, LivingEntity owner) {
		super(ModEntities.TALISMAN, owner, level);
	}

	public void setStrength(int rank, float power) {
		this.rank = rank;
		this.power = power;
	}

	public int rank() { return rank; }

	public float power() { return power; }

	@Override
	protected Item getDefaultItem() {
		return ModItems.FIRE_TALISMAN;
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (level() instanceof ServerLevel level && !isRemoved()) {
			Talismans.land(this, hit);
			level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, getItem()), getX(), getY(), getZ(), 8, 0.1, 0.1, 0.1, 0.08);
			discard();
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("TalismanRank", rank);
		tag.putFloat("TalismanPower", power);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		rank = tag.getInt("TalismanRank");
		power = tag.contains("TalismanPower") ? tag.getFloat("TalismanPower") : 1;
	}
}
