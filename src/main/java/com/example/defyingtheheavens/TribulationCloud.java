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
	public static final float HEIGHT_ABOVE_PLAYER = 40.0f;
	private static final double BELOW_NORMAL_CLOUDS = 64.0;

	/** Keep even the tallest storm banks clearly beneath the normal cloud layer. */
	public static double cloudBaseY(Level level, double playerY) {
		if (level.dimension() == Level.OVERWORLD) return 192.0 - BELOW_NORMAL_CLOUDS;
		if (ModDimensions.isUpperRealm(level.dimension())) return 336.0 - BELOW_NORMAL_CLOUDS;
		return playerY + HEIGHT_ABOVE_PLAYER;
	}
	private Vec3 interpolationTarget;
	private int interpolationSteps;
	private static final EntityDataAccessor<Integer> FLASH_TICKS = SynchedEntityData.defineId(TribulationCloud.class, EntityDataSerializers.INT);
	private static final int FLASH_DURATION = 6;
	private static final EntityDataAccessor<Integer> STORM_TIER = SynchedEntityData.defineId(TribulationCloud.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> FORMED_AT = SynchedEntityData.defineId(TribulationCloud.class, EntityDataSerializers.LONG);

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
	}

	public void configure(Realm targetRealm) {
		entityData.set(STORM_TIER, Math.max(0, targetRealm.ordinal() - Realm.FOUNDATION_BUILDING.ordinal()));
		entityData.set(FORMED_AT, level().getGameTime());
	}

	public int stormTier() { return entityData.get(STORM_TIER); }
	public float stormScale() { return 1.0f + stormTier() * 0.45f; }

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

	/** Ease toward the cultivator with a slow crosswind, retaining some natural trailing motion. */
	public void follow(Vec3 playerPosition) {
		double time = level().getGameTime();
		Vec3 target = new Vec3(playerPosition.x + Math.sin(time * 0.008) * 4.5,
				cloudBaseY(level(), playerPosition.y) + Math.sin(time * 0.006) * 1.5,
				playerPosition.z + Math.cos(time * 0.006) * 3.5);
		setPos(position().distanceToSqr(target) > 96 * 96 ? target : position().lerp(target, 0.08));
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
