package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * A heart demon: the cultivator's own shadow, risen in their Inner Realm during a qi surge from Nascent Soul on (see
 * QiSurges). It fights only its owner, scaled to them. Slay it for a great surge of cultivation; let it win and the soul is
 * thrown back to its body. Drawn in its owner's skin, dark and smoky (client HeartDemonRenderer).
 */
public class HeartDemonEntity extends Monster {
	private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(HeartDemonEntity.class,
			EntityDataSerializers.OPTIONAL_UUID);
	/** Its health and blows, as shares of its owner's max health. */
	private static final double HEALTH_SHARE = 0.8, DAMAGE_SHARE = 0.1;

	public HeartDemonEntity(EntityType<? extends HeartDemonEntity> type, Level level) {
		super(type, level);
		xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.FOLLOW_RANGE, 48.0);
	}

	/** Raises the owner's heart demon at the far edge of their island. Null if it couldn't be placed. */
	public static HeartDemonEntity summon(ServerPlayer owner) {
		ServerLevel level = owner.serverLevel();
		HeartDemonEntity demon = new HeartDemonEntity(ModEntities.HEART_DEMON, level);
		demon.entityData.set(OWNER, Optional.of(owner.getUUID()));
		BlockPos centre = InnerRealm.islandCentre(owner);
		double radius = InnerRealm.islandRadius(CultivationManager.get(owner).getRealm()) - 1.5;
		Vec3 away = new Vec3(centre.getX() + 0.5 - owner.getX(), 0, centre.getZ() + 0.5 - owner.getZ());
		away = away.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : away.normalize();
		double x = centre.getX() + 0.5 + away.x * radius, z = centre.getZ() + 0.5 + away.z * radius;
		demon.moveTo(x, centre.getY() + 1, z, (float) Math.toDegrees(Math.atan2(-away.x, away.z)) + 180, 0);
		double health = Math.max(20, owner.getMaxHealth() * HEALTH_SHARE);
		demon.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
		demon.setHealth((float) health);
		demon.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.max(4, owner.getMaxHealth() * DAMAGE_SHARE));
		demon.setTarget(owner);
		demon.setPersistenceRequired();
		if (!level.addFreshEntity(demon)) return null;
		level.sendParticles(ParticleTypes.LARGE_SMOKE, x, centre.getY() + 1.8, z, 40, 0.4, 0.8, 0.4, 0.02);
		level.playSound(null, demon.blockPosition(), SoundEvents.WARDEN_EMERGE, net.minecraft.sounds.SoundSource.HOSTILE, 0.6f, 1.4f);
		return demon;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(OWNER, Optional.empty());
	}

	public UUID getOwnerId() { return entityData.get(OWNER).orElse(null); }

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, true));
	}

	private ServerPlayer owner() {
		UUID id = getOwnerId();
		return id == null || getServer() == null ? null : getServer().getPlayerList().getPlayer(id);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide) {
			if (random.nextInt(3) == 0) {
				level().addParticle(ParticleTypes.SMOKE, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.02, 0);
			}
			return;
		}
		ServerPlayer owner = owner();
		BlockPos centre = owner == null ? null : InnerRealm.islandCentre(owner);
		if (owner == null || !InnerRealm.isInside(owner) || getY() < centre.getY() - 10) {
			discard(); // its owner has gone back, or it fell from the island
			return;
		}
		if (getTarget() != owner) setTarget(owner);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 0.4, 0.6, 0.4, 0.05);
			ServerPlayer owner = owner();
			if (owner != null) QiSurges.onDemonSlain(owner);
		}
	}

	@Override protected boolean shouldDropLoot() { return false; }
	@Override protected boolean shouldDespawnInPeaceful() { return false; }
	@Override public boolean removeWhenFarAway(double distance) { return false; }
	@Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }
	@Override protected SoundEvent getAmbientSound() { return SoundEvents.SOUL_ESCAPE; }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SOUL_SAND_BREAK; }
	@Override protected SoundEvent getDeathSound() { return SoundEvents.SOUL_ESCAPE; }

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		UUID id = getOwnerId();
		if (id != null) tag.putUUID("Owner", id);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.hasUUID("Owner")) entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
	}
}
