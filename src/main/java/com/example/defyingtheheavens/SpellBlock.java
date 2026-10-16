package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A block a spell moves before your eyes: a wall's blocks rising out of the ground, a cave's crumbling away, the clods flying
 * together into a boulder, a tree's trunk pushing up. Only a picture (SpellBlockRenderer): the spell sets down or takes up the
 * real block itself when the picture is done. It goes from where it is made by {@link #travel} over {@link #duration} ticks
 * (after {@link #delay}), in its {@link Motion}. Never saved.
 */
public class SpellBlock extends Entity {
	/** How it moves. Synced by ordinal. */
	public enum Motion {
		/** Slides along its way, easing in and out (a wall rising, a trunk growing). */
		SLIDE,
		/** Flies along an arc to its end, tumbling (clods gathering into a boulder, ore flying to the caster). */
		FLY,
		/** Crumbles where it is: shaking, cracking, shrinking and falling apart. */
		CRUMBLE,
		/** Grows from nothing where it is. */
		GROW,
		/** Rises along its way, then sinks back (a spike of rock). */
		RISE_AND_SINK;

		public static Motion byIndex(int index) {
			return index >= 0 && index < values().length ? values()[index] : SLIDE;
		}
	}

	private static final EntityDataAccessor<BlockState> BLOCK = SynchedEntityData.defineId(SpellBlock.class, EntityDataSerializers.BLOCK_STATE);
	private static final EntityDataAccessor<org.joml.Vector3f> TRAVEL = SynchedEntityData.defineId(SpellBlock.class, EntityDataSerializers.VECTOR3);
	private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(SpellBlock.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DELAY = SynchedEntityData.defineId(SpellBlock.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> MOTION = SynchedEntityData.defineId(SpellBlock.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(SpellBlock.class, EntityDataSerializers.FLOAT);
	/** Ticks it stays at its end once there (a picture held until the real block is set down beneath it). */
	private static final EntityDataAccessor<Integer> HOLD = SynchedEntityData.defineId(SpellBlock.class, EntityDataSerializers.INT);

	public SpellBlock(EntityType<? extends SpellBlock> type, Level level) {
		super(type, level);
		noPhysics = true;
		noCulling = true;
	}

	/**
	 * Shows {@code state} moving from {@code from} (a block's bottom centre) by {@code travel} over {@code duration} ticks after
	 * {@code delay}, in {@code motion}, held at its end {@code hold} ticks.
	 */
	public static SpellBlock show(ServerLevel level, BlockState state, Vec3 from, Vec3 travel, int delay, int duration, Motion motion, int hold) {
		SpellBlock block = new SpellBlock(ModEntities.SPELL_BLOCK, level);
		block.entityData.set(BLOCK, state);
		block.entityData.set(TRAVEL, new org.joml.Vector3f((float) travel.x, (float) travel.y, (float) travel.z));
		block.entityData.set(DELAY, Math.max(0, delay));
		block.entityData.set(DURATION, Math.max(1, duration));
		block.entityData.set(MOTION, (byte) motion.ordinal());
		block.entityData.set(HOLD, Math.max(0, hold));
		block.moveTo(from.x, from.y, from.z, 0, 0);
		level.addFreshEntity(block);
		return block;
	}

	public SpellBlock scale(float scale) { entityData.set(SCALE, scale); return this; }

	public BlockState state() { return entityData.get(BLOCK); }
	public Vec3 travel() { org.joml.Vector3f t = entityData.get(TRAVEL); return new Vec3(t.x, t.y, t.z); }
	public int duration() { return entityData.get(DURATION); }
	public int delay() { return entityData.get(DELAY); }
	public Motion motion() { return Motion.byIndex(entityData.get(MOTION)); }
	public float scale() { return entityData.get(SCALE); }

	/** How far along its way it is, 0 (not set off) to 1 (there). */
	public float along(float partialTick) {
		return Math.max(0, Math.min(1, (tickCount + partialTick - delay()) / duration()));
	}

	@Override
	protected void defineSynchedData() {
		entityData.define(BLOCK, Blocks.DIRT.defaultBlockState());
		entityData.define(TRAVEL, new org.joml.Vector3f());
		entityData.define(DURATION, 20);
		entityData.define(DELAY, 0);
		entityData.define(MOTION, (byte) 0);
		entityData.define(SCALE, 1.0f);
		entityData.define(HOLD, 0);
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide && tickCount > delay() + duration() + entityData.get(HOLD)) discard();
	}

	@Override
	public boolean isPickable() { return false; }

	@Override
	public boolean isPushable() { return false; }

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) { return distance < 160 * 160; }

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}

	/** Never pushed, never moved by anything: the picture keeps to its way. */
	@Override
	public void move(net.minecraft.world.entity.MoverType type, Vec3 movement) {}

	/** The entity itself stays where it was made; its picture travels. */
	@Override
	public boolean isNoGravity() { return true; }
}
