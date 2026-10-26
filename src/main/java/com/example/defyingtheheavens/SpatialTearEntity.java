package com.example.defyingtheheavens;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A tear in space opened by the Spatial Tear (SpatialTrialHandler#tear): it stands a little before the one who opened it, facing
 * them, for {@link #LIFE} ticks, and whoever walks into it goes through (SpatialTrialHandler#walkInto): into the gap's shallows
 * from the Overworld or the Upper Realm, or out of the gap to where its opener's way out leads; or, torn by one of Void Shattering
 * high in the Jade Firmament's sky, a Void Breach up into the Great Void, or out of the Great Void down onto the Jade Firmament
 * (VoidBreach, which holds a breach open while its opener stays by it). This is only its body (where it
 * is, how long it stands, where it leads, for the drawing); the gap's handler keeps the rest. Drawn by the client's
 * SpatialTearRenderer: it opens, crackles, and closes again. Never saved: no tear outlasts the server.
 */
public class SpatialTearEntity extends Entity {
	/** How long a tear stands open (ticks); it opens over {@link #OPEN_TICKS} and closes over the last {@link #CLOSE_TICKS}. */
	public static final int LIFE = 20 * 20, OPEN_TICKS = 12, CLOSE_TICKS = 24;
	/**
	 * Where it leads, for its colours: into the gap, out to the Overworld, out to the Upper Realm; a Void Breach up into the Great Void
	 * (VoidBreach: drawn the larger), and down out of the Great Void onto the Jade Firmament; a void current drifting over the Great Void
	 * (VoidCurrents: drawn lying along its flow, its face the way it flows). Synced by ordinal (append only).
	 */
	public enum Leads { GAP, OVERWORLD, UPPER_REALM, VOID_BREACH, JADE_FIRMAMENT, VOID_CURRENT }

	private static final EntityDataAccessor<Byte> LEADS = SynchedEntityData.defineId(SpatialTearEntity.class, EntityDataSerializers.BYTE);
	/** Its whole life, ticks: {@link #LIFE}, longer while a breach's opener holds it open ({@link #holdOpen}), a current's its own. */
	private static final EntityDataAccessor<Integer> LIFE_TICKS = SynchedEntityData.defineId(SpatialTearEntity.class, EntityDataSerializers.INT);

	public SpatialTearEntity(EntityType<? extends SpatialTearEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		noCulling = true;
	}

	/** A tear at {@code at}, its face turned to {@code yaw} (degrees), leading {@code leads}. */
	public static SpatialTearEntity create(Level level, Vec3 at, float yaw, Leads leads) {
		SpatialTearEntity tear = new SpatialTearEntity(ModEntities.SPATIAL_TEAR, level);
		tear.entityData.set(LEADS, (byte) leads.ordinal());
		tear.moveTo(at.x, at.y, at.z, yaw, 0);
		return tear;
	}

	public Leads leads() {
		int i = entityData.get(LEADS);
		return i >= 0 && i < Leads.values().length ? Leads.values()[i] : Leads.GAP;
	}

	/** Its whole life, ticks (it shuts and is gone at its end). */
	public int life() {
		return entityData.get(LIFE_TICKS);
	}

	/** Held open: it closes no sooner than at {@code until} ticks of age (VoidBreach, while the breach's opener holds it). */
	public void holdOpen(int until) {
		if (until > life()) entityData.set(LIFE_TICKS, until);
	}

	/** How open it is, 0 (shut) to 1: opening, standing, closing. */
	public float openness(float partialTick) {
		float age = tickCount + partialTick;
		return Math.min(Mth.clamp(age / OPEN_TICKS, 0, 1), Mth.clamp((life() - age) / CLOSE_TICKS, 0, 1));
	}

	@Override
	protected void defineSynchedData() {
		entityData.define(LEADS, (byte) 0);
		entityData.define(LIFE_TICKS, LIFE);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) {
			float open = openness(0);
			// A current streams on along its flow, starlight carried in it.
			if (leads() == Leads.VOID_CURRENT) {
				if (open > 0.3f && random.nextFloat() < open * 0.8f) {
					Vec3 flow = Vec3.directionFromRotation(0, getYRot());
					double along = (random.nextDouble() - 0.5) * 6, across = (random.nextDouble() - 0.5) * 1.2;
					level().addParticle(ParticleTypes.END_ROD, getX() + flow.x * along - flow.z * across, getY() + VoidCurrents.HEART + (random.nextDouble() - 0.5),
							getZ() + flow.z * along + flow.x * across, flow.x * 0.35, 0, flow.z * 0.35);
				}
				return;
			}
			// Motes of the world drawn into it, and the void's breath out of it.
			if (open > 0.3f && random.nextFloat() < open) {
				double a = random.nextDouble() * Math.PI * 2, r = 1.2 + random.nextDouble() * 1.4;
				double y = getY() + 0.2 + random.nextDouble() * 2.4;
				level().addParticle(ParticleTypes.PORTAL, getX() + Math.cos(a) * r, y, getZ() + Math.sin(a) * r,
						-Math.cos(a) * r * 0.6, (getY() + 1.3 - y) * 0.5, -Math.sin(a) * r * 0.6);
			}
			return;
		}
		if (tickCount % 40 == 5) level().playSound(null, blockPosition(), SoundEvents.PORTAL_AMBIENT, SoundSource.NEUTRAL, 0.35f, 1.7f);
		if (tickCount == life() - CLOSE_TICKS) level().playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 0.6f, 1.6f);
		if (tickCount >= life()) discard();
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) { return distance < 160 * 160; }

	@Override
	public boolean isPushable() { return false; }

	@Override
	public boolean canBeCollidedWith() { return false; }

	@Override
	public boolean isPickable() { return false; }

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}
}
