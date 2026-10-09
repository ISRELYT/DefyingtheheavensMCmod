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

/** A temporary, non-interactive storm tracked by the server so every nearby client can see it. */
public class TribulationCloud extends Entity {
	public static final float HEIGHT_ABOVE_PLAYER = 64.0f;
	private Vec3 interpolationTarget;
	private int interpolationSteps;
	private static final EntityDataAccessor<Integer> FLASH_TICKS = SynchedEntityData.defineId(TribulationCloud.class, EntityDataSerializers.INT);
	private static final int FLASH_DURATION = 6;

	public TribulationCloud(EntityType<? extends TribulationCloud> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
		setInvulnerable(true);
	}

	@Override
	protected void defineSynchedData() {
		entityData.define(FLASH_TICKS, 0);
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
		Vec3 target = playerPosition.add(Math.sin(time * 0.008) * 4.5,
				HEIGHT_ABOVE_PLAYER + Math.sin(time * 0.006) * 1.5, Math.cos(time * 0.006) * 3.5);
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
		return distance < 192.0 * 192.0;
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
