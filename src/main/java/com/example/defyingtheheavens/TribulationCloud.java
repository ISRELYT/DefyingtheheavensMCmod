package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

/** A temporary, non-interactive storm tracked by the server so every nearby client can see it. */
public class TribulationCloud extends Entity {
	public static final float BASE_HEIGHT_ABOVE_PLAYER = 32.0f;
	public static final float HEIGHT_PER_TIER = 6.0f;
	/** How much wider the storm grows each realm past Foundation Building (its bolt as much thicker: TribulationManager#boltScale). */
	public static final float SCALE_PER_TIER = 0.45f;

	/** Keep a moderate, realm-scaled gap above the cultivator in every dimension. */
	public double cloudBaseY(double playerY) {
		return playerY + heightAbovePlayer();
	}
	private Vec3 interpolationTarget;
	private int interpolationSteps;
	private static final EntityDataAccessor<Integer> FLASH_TICKS = SynchedEntityData.defineId(TribulationCloud.class, EntityDataSerializers.INT);
	private static final int FLASH_DURATION = 6;
	private static final EntityDataAccessor<Integer> STORM_TIER = SynchedEntityData.defineId(TribulationCloud.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> FORMED_AT = SynchedEntityData.defineId(TribulationCloud.class, EntityDataSerializers.LONG);
	/** The Netherworld's tribulation's storm (the ghost path's, TribulationManager): drawn dark teal, its flashes cyan. */
	private static final EntityDataAccessor<Boolean> GHOST = SynchedEntityData.defineId(TribulationCloud.class, EntityDataSerializers.BOOLEAN);
	/**
	 * How many times its realm's size the storm has grown (1: as it gathered): a lifespan tribulation's swells as its power does
	 * (LifespanTribulation#growth; the user's ask, 2026-10-20), wider, higher and darker.
	 */
	private static final EntityDataAccessor<Float> GROWTH = SynchedEntityData.defineId(TribulationCloud.class, EntityDataSerializers.FLOAT);
	/** Each time its realm's size it has grown, it rides this much higher above the cultivator. */
	public static final float HEIGHT_PER_GROWTH = 10.0f;

	public TribulationCloud(EntityType<? extends TribulationCloud> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
		setInvulnerable(true);
	}

	@Override
	protected void defineSynchedData() {
		entityData.define(FLASH_TICKS, 0);
		entityData.define(STORM_TIER, 0);
		entityData.define(FORMED_AT, 0L);
		entityData.define(GHOST, false);
		entityData.define(GROWTH, 1.0f);
	}

	public void configure(Realm targetRealm) {
		configure(targetRealm, false);
	}

	/** A storm for a breakthrough into {@code targetRealm}: the heavens', or the Netherworld's ({@code ghost}: the ghost path's). */
	public void configure(Realm targetRealm, boolean ghost) {
		entityData.set(STORM_TIER, Math.max(0, targetRealm.ordinal() - Realm.FOUNDATION_BUILDING.ordinal()));
		entityData.set(FORMED_AT, level().getGameTime());
		entityData.set(GHOST, ghost);
	}

	public boolean isGhost() { return entityData.get(GHOST); }

	/** Grown to {@code growth} times its realm's size (at least 1). */
	public void setGrowth(float growth) {
		float clamped = Math.max(1.0f, growth);
		if (Math.abs(entityData.get(GROWTH) - clamped) > 0.005f) entityData.set(GROWTH, clamped);
	}

	public float growth() { return entityData.get(GROWTH); }

	public int stormTier() { return entityData.get(STORM_TIER); }
	public float stormScale() { return (1.0f + stormTier() * SCALE_PER_TIER) * growth(); }
	public float heightAbovePlayer() { return BASE_HEIGHT_ABOVE_PLAYER + stormTier() * HEIGHT_PER_TIER + (growth() - 1.0f) * HEIGHT_PER_GROWTH; }

	/** Server time prevents the formation animation restarting for late observers. */
	public float formationAge(float partialTick) {
		return Math.max(0, level().getGameTime() - entityData.get(FORMED_AT) + partialTick);
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		return getBoundingBox().inflate(36.0 * stormScale(), 12.0, 36.0 * stormScale());
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide() && interpolationSteps > 0) {
			setPos(position().lerp(interpolationTarget, 1.0 / interpolationSteps));
			interpolationSteps--;
		}
		if (!level().isClientSide() && entityData.get(FLASH_TICKS) > 0) {
			entityData.set(FLASH_TICKS, entityData.get(FLASH_TICKS) - 1);
		}
	}

	/** Base Entity snaps network updates; explicitly blend them for a smoothly moving cloud. */
	@Override
	public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
		interpolationTarget = new Vec3(x, y, z);
		if (position().distanceToSqr(interpolationTarget) > 96 * 96) {
			setPos(interpolationTarget);
			xo = xOld = x;
			yo = yOld = y;
			zo = zOld = z;
			interpolationSteps = 0;
		} else {
			interpolationSteps = Math.max(3, steps);
		}
	}

	/** Drift horizontally, but keep the vertical gap fixed even during flight or a fall. */
	public void follow(Vec3 playerPosition) {
		double time = level().getGameTime();
		Vec3 target = new Vec3(playerPosition.x + Math.sin(time * 0.008) * 4.5,
				cloudBaseY(playerPosition.y),
				playerPosition.z + Math.cos(time * 0.006) * 3.5);
		Vec3 drift = position().distanceToSqr(target) > 96 * 96 ? target : position().lerp(target, 0.08);
		setPos(drift.x, target.y, drift.z);
	}

	public void flash() {
		entityData.set(FLASH_TICKS, FLASH_DURATION);
	}

	public float flashStrength() {
		return entityData.get(FLASH_TICKS) / (float) FLASH_DURATION;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 512.0 * 512.0;
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}

	// The entity type is not saved; the owning trial creates and removes the cloud.
	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {}
}
