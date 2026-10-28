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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * A legend made vast for a moment by its art (phase 3b of the Artifact Forge): the Xuanyuan Sword's Yellow Emperor's Judgement (a giant
 * golden sword falling point first out of the sky onto where its wielder looked: {@link #FALL}), and the Sea-Fixing Needle's As You Will
 * (the Needle grown to a mountain coming down upright there, {@link #SLAM}, or stretched out along a line from its wielder's hand,
 * {@link #STRETCH}). It stands where its art lands; it carries its item (drawn so by the client, ColossusRenderer), its size, how it
 * moves and where from; its harm lands at {@link #impact}, as its art said. Never saved: an art's moment.
 */
public class Colossus extends Entity {
	private static final EntityDataAccessor<ItemStack> ITEM = SynchedEntityData.defineId(Colossus.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(Colossus.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Byte> MODE = SynchedEntityData.defineId(Colossus.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Vector3f> FROM = SynchedEntityData.defineId(Colossus.class, EntityDataSerializers.VECTOR3);
	/** Falling point first (a sword), falling upright (the Needle a mountain), stretched along a line (the Needle reaching). */
	public static final byte FALL = 0, SLAM = 1, STRETCH = 2;

	/** What lands at its moment (its art's harm): the server's alone, not saved. */
	private @Nullable Runnable harm;

	public Colossus(EntityType<? extends Colossus> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
		setInvulnerable(true);
	}

	/**
	 * {@code item} made vast ({@code size} times a held one's) for its art at {@code at}, coming {@code from} there, moving as {@code mode} says,
	 * its {@code harm} landing at its moment.
	 */
	static void make(ServerLevel level, ItemStack item, byte mode, float size, Vec3 from, Vec3 at, Runnable harm) {
		Colossus colossus = ModEntities.COLOSSUS.create(level);
		if (colossus == null) {
			harm.run();
			return;
		}
		colossus.moveTo(at.x, at.y, at.z);
		colossus.entityData.set(ITEM, item.copyWithCount(1));
		colossus.entityData.set(SIZE, size);
		colossus.entityData.set(MODE, mode);
		colossus.entityData.set(FROM, new Vector3f((float) from.x, (float) from.y, (float) from.z));
		colossus.harm = harm;
		level.addFreshEntity(colossus);
	}

	@Override
	protected void defineSynchedData() {
		entityData.define(ITEM, ItemStack.EMPTY);
		entityData.define(SIZE, 1.0f);
		entityData.define(MODE, FALL);
		entityData.define(FROM, new Vector3f());
	}

	public ItemStack item() { return entityData.get(ITEM); }
	public float size() { return entityData.get(SIZE); }
	public byte mode() { return entityData.get(MODE); }
	public Vec3 from() {
		Vector3f from = entityData.get(FROM);
		return new Vec3(from.x, from.y, from.z);
	}

	/** Ticks from its making to its harm landing: a sword's fall, a mountain's, a reach's. */
	public int impact() {
		return switch (mode()) {
			case SLAM -> 14;
			case STRETCH -> 4;
			default -> 10;
		};
	}

	/** Ticks it lasts all told: its moment, then a while sunk in the land (fading), or drawn back in. */
	public int lifetime() {
		return impact() + (mode() == STRETCH ? 8 : 24);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) return;
		if (tickCount == impact() && harm != null) {
			Runnable landing = harm;
			harm = null;
			landing.run();
		}
		if (tickCount >= lifetime()) discard();
	}

	/** Seen from far: it is vast. */
	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 256 * 256;
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		Vec3 from = from();
		return new AABB(position(), from).inflate(size() * 1.2 + 2);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}
}
