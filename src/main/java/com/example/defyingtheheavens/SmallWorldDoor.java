package com.example.defyingtheheavens;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * A small world's door (SmallWorlds, phase 3d): a doorway of starlight {@link #WIDTH} wide and {@link #HEIGHT} high standing upright where
 * its owner opened it, facing them, until it closes, gold sparks along its frame (drawn by the client's SmallWorldDoorRenderer, opening
 * out of its middle and closing into it). Whoever walks into it is carried into its world if it is theirs to come into
 * (SmallWorlds#atDoor), else turned away. Never saved: a door doesn't outlast its land unloading or the server stopping.
 */
public class SmallWorldDoor extends Entity {
	public static final float WIDTH = 1.6f, HEIGHT = 2.6f;
	/** The game times it opened and closes at (synced once, so the client draws it opening and closing). */
	private static final EntityDataAccessor<Long> OPENED_AT = SynchedEntityData.defineId(SmallWorldDoor.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Long> CLOSES_AT = SynchedEntityData.defineId(SmallWorldDoor.class, EntityDataSerializers.LONG);
	private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0f, 0.84f, 0.42f), 0.8f);
	private UUID owner;

	public SmallWorldDoor(EntityType<? extends SmallWorldDoor> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** Its owner, and how long it stands from now. */
	void setUp(UUID owner, int ticks) {
		this.owner = owner;
		long now = level().getGameTime();
		entityData.set(OPENED_AT, now);
		entityData.set(CLOSES_AT, now + ticks);
	}

	public UUID owner() { return owner; }

	/** Ticks since it opened, and ticks it stands yet (at {@code partialTick} into this one). */
	public float age(float partialTick) { return level().getGameTime() - entityData.get(OPENED_AT) + partialTick; }

	public float left(float partialTick) { return entityData.get(CLOSES_AT) - level().getGameTime() - partialTick; }

	@Override
	protected void defineSynchedData() {
		entityData.define(OPENED_AT, 0L);
		entityData.define(CLOSES_AT, 0L);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) {
			sparks();
			return;
		}
		if (left(0) <= 0 || owner == null) {
			SmallWorlds.doorGone(this);
			discard();
			return;
		}
		// Those walking into its plane (a thin box across it).
		Vec3 across = Vec3.directionFromRotation(0, getYRot() + 90);
		Vec3 a = position().add(across.scale(WIDTH / 2)), b = position().subtract(across.scale(WIDTH / 2));
		AABB plane = new AABB(a.x, getY(), a.z, b.x, getY() + HEIGHT, b.z).inflate(0.3, 0, 0.3);
		for (ServerPlayer p : level().getEntitiesOfClass(ServerPlayer.class, plane, ServerPlayer::isAlive)) SmallWorlds.atDoor(this, p);
	}

	@Override
	public void remove(RemovalReason reason) {
		super.remove(reason);
		if (!level().isClientSide) SmallWorlds.doorGone(this);
	}

	/** Gold sparks along its frame, starlight motes drifting out of it (the client's), while it stands open. */
	private void sparks() {
		if (age(0) < 8 || left(0) < 8) return;
		Vec3 across = Vec3.directionFromRotation(0, getYRot() + 90);
		for (int i = 0; i < 2; i++) {
			// A point on the frame: up one side, across the top, down the other.
			double t = random.nextDouble() * (HEIGHT * 2 + WIDTH);
			double side, up;
			if (t < HEIGHT) {
				side = -WIDTH / 2;
				up = t;
			} else if (t < HEIGHT + WIDTH) {
				side = t - HEIGHT - WIDTH / 2;
				up = HEIGHT;
			} else {
				side = WIDTH / 2;
				up = t - HEIGHT - WIDTH;
			}
			Vec3 at = position().add(across.scale(side)).add(0, up, 0);
			level().addParticle(GOLD, at.x, at.y, at.z, 0, 0.01, 0);
		}
		if (random.nextInt(3) == 0) {
			Vec3 at = position().add(across.scale((random.nextDouble() - 0.5) * WIDTH)).add(0, random.nextDouble() * HEIGHT, 0);
			Vec3 out = Vec3.directionFromRotation(0, getYRot()).scale(random.nextBoolean() ? 0.03 : -0.03);
			level().addParticle(ParticleTypes.END_ROD, at.x, at.y, at.z, out.x, 0.005, out.z);
		}
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 128 * 128;
	}

	/** Not saved (its type's noSave): what is read back, if anything, closes it at once. */
	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		entityData.set(CLOSES_AT, 0L);
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}
}
