package com.example.defyingtheheavens;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

/**
 * An earth golem raised by Summon Golem (a Nascent Soul's earth spell), or the Earthen Titan three times its size (Four Axis): a
 * hulk of packed earth and stone with molten amber in its cracks, floating a little off the ground. It flies (at
 * {@link #FLY_SHARE} of qi flight's pace), keeps by its caster, and fights for them: whatever strikes them or they strike, or a
 * monster that comes for them. It never harms its caster or their allies, and crumbles back to earth when its time is up or its
 * caster is gone. Never saved.
 */
public class EarthGolem extends PathfinderMob {
	/** Its share of a cultivator's qi flight (vanilla's flying speed, about 0.55 blocks a tick at a sprint): 60%. */
	public static final double FLY_SHARE = 0.6;
	public static final double FLY_SPEED = 0.55 * FLY_SHARE;
	/** A golem's strength; the titan's is its own (by {@link #TITAN_SCALE}). */
	public static final double HEALTH = 80, DAMAGE = 18, TITAN_HEALTH = 400, TITAN_DAMAGE = 80;
	public static final float TITAN_SCALE = 3.0f;
	/** How far it keeps from its caster when there is nothing to fight. */
	private static final double KEEP_NEAR = 4;
	private static final int ATTACK_INTERVAL = 20;

	private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(EarthGolem.class, EntityDataSerializers.FLOAT);
	/** Ticks since its fist last swung (the client's arm swing), counted on both sides from the server's word. */
	private static final EntityDataAccessor<Integer> SWING = SynchedEntityData.defineId(EarthGolem.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(EarthGolem.class, EntityDataSerializers.INT);

	private UUID owner;
	private int attackCooldown;

	public EarthGolem(EntityType<? extends EarthGolem> type, Level level) {
		super(type, level);
		setNoGravity(true);
		xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, HEALTH)
				.add(Attributes.ATTACK_DAMAGE, DAMAGE)
				.add(Attributes.ARMOR, 10)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FOLLOW_RANGE, 32)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.9);
	}

	/** Raises a golem (or a titan) for {@code caster} at {@code at}, for {@code life} ticks. */
	public static EarthGolem raise(ServerPlayer caster, Vec3 at, boolean titan, int life) {
		EarthGolem golem = new EarthGolem(ModEntities.EARTH_GOLEM, caster.level());
		golem.owner = caster.getUUID();
		golem.entityData.set(LIFE, life);
		if (titan) {
			golem.entityData.set(SCALE, TITAN_SCALE);
			golem.getAttribute(Attributes.MAX_HEALTH).setBaseValue(TITAN_HEALTH);
			golem.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(TITAN_DAMAGE);
			golem.refreshDimensions();
		}
		golem.setHealth(golem.getMaxHealth());
		golem.moveTo(at.x, at.y, at.z, caster.getYRot(), 0);
		caster.level().addFreshEntity(golem);
		return golem;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(SCALE, 1.0f);
		entityData.define(SWING, 100);
		entityData.define(LIFE, 20 * 90);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (SCALE.equals(key)) refreshDimensions();
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return super.getDimensions(pose).scale(entityData.get(SCALE));
	}

	/** 1 for a golem, {@link #TITAN_SCALE} for a titan. */
	public float size() { return entityData.get(SCALE); }

	public boolean isTitan() { return size() > 1.5f; }

	/** Ticks since its last blow (for the arm's swing). */
	public int sinceSwing() { return entityData.get(SWING); }

	/** How long it lasts in all. */
	public int life() { return entityData.get(LIFE); }

	public UUID ownerId() { return owner; }

	/** Its caster, while in this world. */
	public ServerPlayer caster() {
		if (owner == null || !(level() instanceof ServerLevel level)) return null;
		return level.getServer().getPlayerList().getPlayer(owner);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new Fight());
		goalSelector.addGoal(2, new Keep());
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide) {
			entityData.set(SWING, Math.min(100, entityData.get(SWING) + 1));
			ServerPlayer caster = caster();
			if (caster == null || !caster.isAlive() || caster.level() != level() || tickCount > life()) {
				crumble();
				return;
			}
			if (attackCooldown > 0) attackCooldown--;
			if (tickCount % 10 == 0) chooseFoe(caster);
		}
	}

	/** Its time is up: it falls back to earth. */
	public void crumble() {
		if (level() instanceof ServerLevel level) {
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, position().add(0, getBbHeight() / 2, 0), Vec3.ZERO, 1.5f * size(),
					net.minecraft.world.level.block.Block.getId(net.minecraft.world.level.block.Blocks.COARSE_DIRT.defaultBlockState()));
			HeavenSpells.sound(level, position(), SoundEvents.ROOTED_DIRT_BREAK, 1.5f, 0.6f);
		}
		discard();
	}

	/** Its caster's foe: whoever struck them or they struck lately, else a monster after them or it. */
	private void chooseFoe(ServerPlayer caster) {
		LivingEntity current = getTarget();
		if (current != null && current.isAlive() && !HeavenSpells.ally(caster, current) && current != caster && distanceToSqr(current) < 32 * 32) return;
		LivingEntity foe = null;
		if (caster.getLastHurtByMob() != null && caster.tickCount - caster.getLastHurtByMobTimestamp() < 200) foe = caster.getLastHurtByMob();
		else if (caster.getLastHurtMob() != null && caster.tickCount - caster.getLastHurtMobTimestamp() < 200) foe = caster.getLastHurtMob();
		if (foe == null) {
			double best = 16 * 16;
			for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16),
					e -> e instanceof Enemy && e instanceof Mob mob && (mob.getTarget() == caster || mob.getTarget() == this))) {
				double d = e.distanceToSqr(this);
				if (d < best) {
					best = d;
					foe = e;
				}
			}
		}
		if (foe != null && (foe == this || foe instanceof EarthGolem golem && caster.getUUID().equals(golem.owner) || HeavenSpells.ally(caster, foe))) foe = null;
		setTarget(foe);
	}

	/** Flies toward {@code at} at its pace, rising over what stands in the way. */
	private void flyToward(Vec3 at, double pace) {
		Vec3 to = at.subtract(position());
		double distance = to.length();
		Vec3 want = distance < 1.0e-3 ? Vec3.ZERO : to.scale(Math.min(pace, distance) / distance);
		Vec3 v = getDeltaMovement().scale(0.6).add(want.scale(0.4));
		if (horizontalCollision) v = new Vec3(v.x, Math.max(v.y, pace * 0.8), v.z);
		setDeltaMovement(v);
		if (to.horizontalDistanceSqr() > 0.01) {
			float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
			setYRot(yaw);
			yBodyRot = yaw;
			yHeadRot = yaw;
		}
	}

	@Override
	public void travel(Vec3 input) {
		// It moves as its goals push it, no weight and no walking.
		move(MoverType.SELF, getDeltaMovement());
		setDeltaMovement(getDeltaMovement().scale(0.85));
		calculateEntityAnimation(true);
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		if (!(level() instanceof ServerLevel level) || !(target instanceof LivingEntity living)) return false;
		entityData.set(SWING, 0);
		float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
		ServerPlayer caster = caster();
		boolean hit = living.hurt(ModDamageTypes.qi(level, QiElement.EARTH, isTitan() ? Realm.FOUR_AXIS : Realm.NASCENT_SOUL, this, caster), damage);
		if (hit) {
			HeavenSpells.push(position(), living, isTitan() ? 2.5 : 1.2);
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, HeavenSpells.middle(living), Vec3.ZERO, isTitan() ? 2.0f : 0.8f,
					net.minecraft.world.level.block.Block.getId(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()));
			HeavenSpells.sound(level, living.position(), SoundEvents.IRON_GOLEM_ATTACK, 1.2f, isTitan() ? 0.5f : 0.8f);
		}
		// The titan's fist strikes the ground about its mark too.
		if (isTitan() && caster != null) {
			for (LivingEntity near : HeavenSpells.aroundPoint(level, living.position(), 4, e -> e != living && HeavenSpells.enemy(caster, e))) {
				near.hurt(ModDamageTypes.qi(level, QiElement.EARTH, Realm.FOUR_AXIS, this, caster), damage * 0.5f);
				HeavenSpells.push(living.position(), near, 1.5);
			}
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, living.position(), Vec3.ZERO, 4, 0);
		}
		return hit;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		// Its caster's own blows and spells pass through it.
		if (owner != null && source.getEntity() != null && owner.equals(source.getEntity().getUUID())) return false;
		boolean hurt = super.hurt(source, amount);
		if (hurt && level() instanceof ServerLevel level) {
			SpellFx.send(level, SpellFx.Kind.ROCK_CHIPS, position().add(0, getBbHeight() * 0.6, 0), Vec3.ZERO, size(), 0);
		}
		return hurt;
	}

	@Override
	public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

	@Override
	public boolean removeWhenFarAway(double distance) { return false; }

	@Override
	protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.STONE_HIT; }

	@Override
	protected SoundEvent getDeathSound() { return SoundEvents.ROOTED_DIRT_BREAK; }

	@Override
	protected void tickDeath() {
		// It doesn't lie and fade: it falls apart at once.
		if (!level().isClientSide) crumble();
	}

	/** It flies at its foe and strikes it. */
	private class Fight extends Goal {
		Fight() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public void tick() {
			LivingEntity target = getTarget();
			if (target == null) return;
			double reach = getBbWidth() * 0.9 + target.getBbWidth() + 1.0;
			Vec3 at = target.position().add(0, target.getBbHeight() * 0.3, 0);
			if (distanceTo(target) > reach) flyToward(at, FLY_SPEED * (isTitan() ? 1.2 : 1.0));
			else setDeltaMovement(getDeltaMovement().scale(0.5));
			getLookControl().setLookAt(target, 30, 30);
			if (distanceTo(target) <= reach + 0.5 && attackCooldown <= 0) {
				doHurtTarget(target);
				attackCooldown = ATTACK_INTERVAL;
			}
		}
	}

	/** With nothing to fight, it keeps beside its caster, a little above the ground. */
	private class Keep extends Goal {
		Keep() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return getTarget() == null && caster() != null;
		}

		@Override
		public void tick() {
			ServerPlayer caster = caster();
			if (caster == null) return;
			Vec3 side = Vec3.directionFromRotation(0, caster.getYRot() + (getId() % 2 == 0 ? 120 : -120)).scale(KEEP_NEAR * size());
			Vec3 at = caster.position().add(side).add(0, 0.5 + 0.3 * Math.sin(tickCount * 0.08), 0);
			if (position().distanceToSqr(at) > 64 * 64) {
				teleportTo(at.x, at.y, at.z);
				return;
			}
			if (position().distanceToSqr(at) > 1) flyToward(at, FLY_SPEED * Math.min(1.5, position().distanceTo(at) / 6 + 0.4));
			getLookControl().setLookAt(caster.getX(), caster.getEyeY(), caster.getZ());
		}
	}
}
