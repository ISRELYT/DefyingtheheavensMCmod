package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The wave of dirt a Dirt Tide (Qi Refining's earth spell) raises under its caster, who rides its crest standing: it runs the way
 * they face, faster than a sprint ({@link #PACE}; forward pushes it on, back reins it in), climbs a block at a time and runs down
 * slopes, over water too. Its rider steers it from their own client (as a boat), and sneaking steps off. It sinks back into the
 * ground when its time is up ({@link #life}). Drawn as a rolling crest of the ground's own earth (DirtTideRenderer). Never saved.
 */
public class DirtTide extends Entity {
	/** Blocks a tick: a sprint is about 0.28. */
	public static final double PACE = 0.55, PACE_PRESSED = 0.68, PACE_HELD = 0.25;
	private static final double GRAVITY = 0.08;

	/** The block it is made of (its state id), for its look. */
	private static final EntityDataAccessor<Integer> EARTH = SynchedEntityData.defineId(DirtTide.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(DirtTide.class, EntityDataSerializers.INT);

	private int lerpSteps;
	private double lerpX, lerpY, lerpZ, lerpYaw;

	public DirtTide(EntityType<? extends DirtTide> type, Level level) {
		super(type, level);
		setMaxUpStep(1.0f);
		blocksBuilding = true;
	}

	/** Raises a tide of {@code earth} under {@code rider}, lasting {@code life} ticks, and sets them on it. */
	public static DirtTide raise(ServerPlayer rider, BlockState earth, int life) {
		DirtTide tide = new DirtTide(ModEntities.DIRT_TIDE, rider.level());
		tide.entityData.set(EARTH, Block.getId(earth));
		tide.entityData.set(LIFE, life);
		tide.moveTo(rider.getX(), rider.getY(), rider.getZ(), rider.getYRot(), 0);
		rider.level().addFreshEntity(tide);
		rider.startRiding(tide, true);
		return tide;
	}

	public BlockState earth() { return Block.stateById(entityData.get(EARTH)); }
	public int life() { return entityData.get(LIFE); }

	@Override
	protected void defineSynchedData() {
		entityData.define(EARTH, Block.getId(Blocks.DIRT.defaultBlockState()));
		entityData.define(LIFE, 200);
	}

	@Override
	public LivingEntity getControllingPassenger() {
		return getFirstPassenger() instanceof Player player ? player : null;
	}

	/** Its rider stands on the crest: a player's own riding offset (-0.35) is a seated one's, so this is that much higher. */
	@Override
	public double getPassengersRidingOffset() { return 1.05; }

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide) {
			if (tickCount > life() || getPassengers().isEmpty() && tickCount > 5) {
				sink();
				return;
			}
		}
		if (isControlledByLocalInstance()) {
			lerpSteps = 0;
			syncPacketPositionCodec(getX(), getY(), getZ());
			drive();
		} else {
			lerpStep();
		}
		if (level() instanceof ServerLevel level && tickCount % 4 == 0) {
			SpellFx.send(level, SpellFx.Kind.DUST, position(), getDeltaMovement(), 0.7f, entityData.get(EARTH));
			if (tickCount % 8 == 0) HeavenSpells.sound(level, position(), SoundEvents.ROOTED_DIRT_STEP, 0.8f, 0.6f + random.nextFloat() * 0.3f);
		}
	}

	/** Its rider drives it: the way they face, at the pace their keys ask. */
	private void drive() {
		LivingEntity rider = getControllingPassenger();
		Vec3 v = getDeltaMovement();
		if (rider != null) {
			setYRot(rider.getYRot());
			double pace = rider.zza > 0 ? PACE_PRESSED : rider.zza < 0 ? PACE_HELD : PACE;
			Vec3 ahead = Vec3.directionFromRotation(0, rider.getYRot());
			Vec3 side = new Vec3(ahead.z, 0, -ahead.x).scale(rider.xxa * 0.25);
			Vec3 want = ahead.scale(pace).add(side);
			v = new Vec3(v.x * 0.5 + want.x * 0.5, v.y, v.z * 0.5 + want.z * 0.5);
		} else {
			v = v.multiply(0.8, 1, 0.8);
		}
		// Its own weight (the server's floating-vehicle check never sees one with no gravity), and over water it rides the top.
		v = v.add(0, -GRAVITY, 0);
		if (isInWater() || level().getFluidState(blockPosition()).isSource()) v = new Vec3(v.x, Math.max(v.y, 0.12), v.z);
		setDeltaMovement(v);
		move(MoverType.SELF, getDeltaMovement());
		if (onGround()) setDeltaMovement(getDeltaMovement().multiply(1, 0, 1));
	}

	@Override
	public boolean isNoGravity() { return true; }

	/** Its time is up (or its rider stepped off): it sinks back into the ground. */
	public void sink() {
		if (level() instanceof ServerLevel level) {
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, position().add(0, 0.4, 0), Vec3.ZERO, 1.2f, entityData.get(EARTH));
			HeavenSpells.sound(level, position(), SoundEvents.ROOTED_DIRT_BREAK, 1.0f, 0.7f);
			for (Entity passenger : getPassengers()) {
				if (passenger instanceof ServerPlayer rider) Spells.safeFall(rider, 40);
			}
		}
		ejectPassengers();
		discard();
	}

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		if (passenger instanceof ServerPlayer rider) Spells.safeFall(rider, 40);
	}

	@Override
	public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
		lerpX = x;
		lerpY = y;
		lerpZ = z;
		lerpYaw = yaw;
		lerpSteps = 10;
	}

	private void lerpStep() {
		if (lerpSteps <= 0) return;
		double x = getX() + (lerpX - getX()) / lerpSteps;
		double y = getY() + (lerpY - getY()) / lerpSteps;
		double z = getZ() + (lerpZ - getZ()) / lerpSteps;
		setYRot((float) (getYRot() + net.minecraft.util.Mth.wrapDegrees(lerpYaw - getYRot()) / lerpSteps));
		lerpSteps--;
		setPos(x, y, z);
	}

	@Override
	public boolean isPickable() { return false; }

	@Override
	public boolean hurt(DamageSource source, float amount) { return false; }

	@Override
	public boolean canBeCollidedWith() { return false; }

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}
}
