package com.example.defyingtheheavens;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * What a demon beast throws or spits (DemonBeastGoals#ShootGoal): a fire monkey's burning stone, lobbed, setting what it strikes alight;
 * a flood dragon's jet of water, flying nearly straight, knocking back what it strikes. The primordial beasts' (PrimordialBeast): the
 * Peng's feather blades, flying straight on the wind, and the True Phoenix's burning feathers, falling as rain and setting what they
 * strike alight. None harms the land, nor another beast.
 */
public class BeastShot extends ThrowableItemProjectile {
	public enum Kind { FIRE_STONE, WATER, FEATHER, FIRE_FEATHER }

	private static final EntityDataAccessor<Byte> DATA_KIND = SynchedEntityData.defineId(BeastShot.class, EntityDataSerializers.BYTE);
	private static final int LIFETIME = 100;

	private float damage = 3;

	public BeastShot(EntityType<? extends BeastShot> type, Level level) {
		super(type, level);
	}

	public BeastShot(Level level, LivingEntity owner, Kind kind, float damage) {
		super(ModEntities.BEAST_SHOT, owner, level);
		entityData.set(DATA_KIND, (byte) kind.ordinal());
		this.damage = damage;
		if (kind == Kind.FIRE_STONE) setItem(new net.minecraft.world.item.ItemStack(Items.FIRE_CHARGE));
		if (kind == Kind.FEATHER || kind == Kind.FIRE_FEATHER) setItem(new net.minecraft.world.item.ItemStack(Items.FEATHER));
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DATA_KIND, (byte) 0);
	}

	public Kind kind() {
		Kind[] kinds = Kind.values();
		return kinds[Math.floorMod(entityData.get(DATA_KIND), kinds.length)];
	}

	/** The jet is all water, drawn as its spray (an empty stack draws nothing); the stone a fire charge. */
	@Override
	protected Item getDefaultItem() {
		return Items.AIR;
	}

	@Override
	protected float getGravity() {
		return switch (kind()) {
			case WATER -> 0.005f;
			case FEATHER -> 0.0f; // borne on the Peng's wind
			case FIRE_FEATHER -> 0.02f; // falling as rain
			default -> 0.03f;
		};
	}

	/** What it deals to what it strikes. */
	public float damage() { return damage; }

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) {
			Vec3 at = position();
			if (kind() == Kind.WATER) {
				for (int i = 0; i < 4; i++) {
					level().addParticle(ParticleTypes.SPLASH, at.x + (random.nextDouble() - 0.5) * 0.4, at.y + (random.nextDouble() - 0.5) * 0.4,
							at.z + (random.nextDouble() - 0.5) * 0.4, 0, 0, 0);
				}
				level().addParticle(ParticleTypes.BUBBLE_POP, at.x, at.y, at.z, 0, 0, 0);
			} else if (kind() == Kind.FEATHER) {
				level().addParticle(ParticleTypes.CLOUD, at.x, at.y, at.z, 0, 0, 0); // the wind it rides
			} else {
				level().addParticle(ParticleTypes.FLAME, at.x, at.y, at.z, 0, 0.02, 0);
				level().addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, 0, 0.02, 0);
			}
		} else if (tickCount > LIFETIME) {
			discard();
		}
	}

	@Override
	protected boolean canHitEntity(Entity target) {
		return !(target instanceof DemonBeast) && super.canHitEntity(target);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (!(hit.getEntity() instanceof LivingEntity target)) return;
		Entity owner = getOwner();
		if (!target.hurt(damageSources().thrown(this, owner), damage)) return;
		if (kind() == Kind.FIRE_STONE || kind() == Kind.FIRE_FEATHER) {
			target.setSecondsOnFire(3);
		} else if (kind() == Kind.FEATHER) {
			Vec3 push = getDeltaMovement().normalize();
			target.knockback(0.6, -push.x, -push.z);
			target.hurtMarked = true;
		} else {
			Vec3 push = getDeltaMovement().normalize();
			target.knockback(1.2, -push.x, -push.z);
			target.clearFire();
			target.hurtMarked = true;
		}
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (!(level() instanceof ServerLevel level)) return;
		if (kind() == Kind.WATER) {
			level.sendParticles(ParticleTypes.SPLASH, getX(), getY(), getZ(), 30, 0.5, 0.3, 0.5, 0.1);
			level.playSound(null, blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 0.8f, 1.2f);
		} else if (kind() == Kind.FEATHER) {
			level.sendParticles(ParticleTypes.CLOUD, getX(), getY(), getZ(), 6, 0.2, 0.2, 0.2, 0.02);
			level.playSound(null, blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.HOSTILE, 0.5f, 1.4f);
		} else {
			level.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(), 6, 0.2, 0.2, 0.2, 0.05);
			level.playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.6f, 1.2f);
		}
		discard();
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putByte("ShotKind", entityData.get(DATA_KIND));
		tag.putFloat("Damage", damage);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		entityData.set(DATA_KIND, tag.getByte("ShotKind"));
		damage = tag.getFloat("Damage");
	}
}
