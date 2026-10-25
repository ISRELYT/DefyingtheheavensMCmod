package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * What an element spell leaves standing in the world for a while (a rain cloud, a prison of water or thorns, a tornado of fire, a
 * small sun, a field of frost...), drawn by its {@link Kind} on the client (SpellFieldRenderer). The spell's own lingering effect
 * (HeavenSpells.Lingering) does the work and moves or ends it; the field is its body: where it is, how big, how long it lasts and
 * how far along it is. One that can be struck ({@link #strikeable}: a cage to break out of) hears of the blows. Never saved.
 */
public class SpellField extends Entity {
	/** What it is. Synced by ordinal: add new ones at the end. */
	public enum Kind {
		RAIN_CLOUD, WATER_PRISON, BRISTLE_PRISON, YANG_BOMB, LIFE_STREAM, CRIMSON_SWIRL, SOLAR_SUN, MIST_VEIL, TIDAL_LANCE, THORN_SPEARS,
		LOTUS, EARTHQUAKE, GREAT_TIDE, SWORD_LIGHT, SEA_OF_FLAMES, BAMBOO, SPRING_RAIN, HEAVY_SEAL, RIVER_TORRENT, FROZEN_FIELD,
		SWORD_DOMAIN, VINE_BINDING, WORLD_TREE, MOUNTAIN_SEAL, BOUNDLESS_SEA, SWORD_ARRAY, EARTH_SPIKES, FROST_BEAM, BLAST, VERDANT_TREE,
		WATER_GATHER, SWORD_RING, DIRT_WAVE, METEOR_MARK, PHOENIX_FLAME, TREE_VINE, BOULDER_FORMING, SEVER_SWING,
		/** Soul Rend's beam (Spells, 2026-10-25): its height its length, out along its facing. */
		SOUL_REND;

		public static Kind byIndex(int index) {
			return index >= 0 && index < values().length ? values()[index] : RAIN_CLOUD;
		}
	}

	/** Blows struck on a field that can be struck (server side). */
	public interface Strikes {
		/** {@code amount} of harm from {@code source} struck it. */
		void struck(SpellField field, DamageSource source, float amount);
	}

	private static final EntityDataAccessor<Byte> KIND = SynchedEntityData.defineId(SpellField.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(SpellField.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(SpellField.class, EntityDataSerializers.FLOAT);
	/** How long it lasts in all, in ticks (its drawing grows in and fades out by it). */
	private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(SpellField.class, EntityDataSerializers.INT);
	/** Its caster's entity id (0: none). */
	private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(SpellField.class, EntityDataSerializers.INT);
	/** The being it holds or reaches for (0: none). */
	private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(SpellField.class, EntityDataSerializers.INT);
	/** Its kind's own word: a seed, a block's state id, a count. */
	private static final EntityDataAccessor<Integer> DATA = SynchedEntityData.defineId(SpellField.class, EntityDataSerializers.INT);
	/** A way it faces or runs (yaw and pitch, degrees). */
	private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(SpellField.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(SpellField.class, EntityDataSerializers.FLOAT);
	/** How much of it is left or done, 0 to 1 (a cage's strength, a beam's length). */
	private static final EntityDataAccessor<Float> VALUE = SynchedEntityData.defineId(SpellField.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> STRIKEABLE = SynchedEntityData.defineId(SpellField.class, EntityDataSerializers.BOOLEAN);
	/** The being it is held on (SpellAnchor: 0, none), how, and where on it (x to its right, y up, z ahead). */
	private static final EntityDataAccessor<Integer> ANCHOR = SynchedEntityData.defineId(SpellField.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> ANCHOR_HOW = SynchedEntityData.defineId(SpellField.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<org.joml.Vector3f> ANCHOR_OFFSET = SynchedEntityData.defineId(SpellField.class, EntityDataSerializers.VECTOR3);
	/** The frame's share of its tick while a watcher draws it (its facing then as its anchor's this frame: {@link #yaw}); -1 between draws. */
	private float drawing = -1;

	private Strikes strikes;

	public SpellField(EntityType<? extends SpellField> type, Level level) {
		super(type, level);
		noPhysics = true;
		noCulling = true;
	}

	/** A field of {@code kind} at {@code at}, {@code radius} round and {@code height} tall, lasting {@code life} ticks. */
	public static SpellField create(Level level, Kind kind, Vec3 at, float radius, float height, int life, Entity owner) {
		SpellField field = new SpellField(ModEntities.SPELL_FIELD, level);
		field.entityData.set(KIND, (byte) kind.ordinal());
		field.entityData.set(RADIUS, radius);
		field.entityData.set(HEIGHT, height);
		field.entityData.set(LIFE, life);
		field.entityData.set(OWNER, owner == null ? 0 : owner.getId());
		field.moveTo(at.x, at.y, at.z, 0, 0);
		field.refreshDimensions();
		return field;
	}

	/** Adds it to its level. */
	public SpellField spawn() {
		level().addFreshEntity(this);
		return this;
	}

	public SpellField target(Entity target) { entityData.set(TARGET, target == null ? 0 : target.getId()); return this; }
	public SpellField data(int data) { entityData.set(DATA, data); return this; }
	public SpellField facing(float yaw, float pitch) { entityData.set(YAW, yaw); entityData.set(PITCH, pitch); return this; }
	public SpellField value(float value) { entityData.set(VALUE, value); return this; }
	public SpellField radius(float radius) { entityData.set(RADIUS, radius); return this; }
	public SpellField height(float height) { entityData.set(HEIGHT, height); return this; }
	public SpellField life(int life) { entityData.set(LIFE, life); return this; }

	/**
	 * Gone as its life runs out, of itself: a field no spell keeps or ends (a flash of sword light, a boulder forming). Left to the
	 * stray-field net alone, one whose drawing holds at its end once grown (a boulder) stood on in the air five seconds after it was thrown
	 * (the user's report, 2026-10-25: "fix boulder throw remaining after throw").
	 */
	public SpellField fleeting() { fleeting = true; return this; }

	/** Whether it goes of itself at its life's end ({@link #fleeting}). The server's word only: its going is what its watchers see. */
	private boolean fleeting;

	/** Lets blows strike it (a cage), and tells {@code strikes} of them. */
	public SpellField strikeable(Strikes strikes) {
		this.strikes = strikes;
		entityData.set(STRIKEABLE, true);
		return this;
	}

	public Kind kind() { return Kind.byIndex(entityData.get(KIND)); }
	public float radius() { return entityData.get(RADIUS); }
	public float height() { return entityData.get(HEIGHT); }
	public int life() { return entityData.get(LIFE); }
	public int data() { return entityData.get(DATA); }

	/** The way it faces: its spell's word, or (drawn while held on a being that turns it: {@link #anchor}) as that being faces this frame. */
	public float yaw() {
		Entity on = drawing < 0 ? null : anchored();
		return on == null ? entityData.get(YAW) : anchoring().yaw(on, drawing, entityData.get(YAW));
	}

	public float pitch() {
		Entity on = drawing < 0 ? null : anchored();
		return on == null ? entityData.get(PITCH) : anchoring().pitch(on, drawing, entityData.get(PITCH));
	}

	/**
	 * Held on {@code on} ({@link SpellAnchor}: the user's ask, 2026-10-25, what tracks its caster lagged behind them): drawn where {@code on}
	 * is drawn each frame, {@code offset} on it, turned {@code how}; its spell still moves it on the server.
	 */
	public SpellField anchor(Entity on, SpellAnchor how, Vec3 offset) {
		entityData.set(ANCHOR, on == null ? 0 : on.getId());
		entityData.set(ANCHOR_HOW, (byte) how.ordinal());
		entityData.set(ANCHOR_OFFSET, offset.toVector3f());
		if (on != null) moveTo(how.at(on, offset, 1, getYRot()));
		return this;
	}

	/** The being it is held on, or null. */
	public Entity anchored() {
		int id = entityData.get(ANCHOR);
		return id <= 0 || anchoring() == SpellAnchor.NONE ? null : level().getEntity(id);
	}

	public SpellAnchor anchoring() { return SpellAnchor.byIndex(entityData.get(ANCHOR_HOW)); }

	/**
	 * A watcher begins drawing it, {@code partial} of the way through the tick (-1: done drawing), and how far from its own eased place it is
	 * drawn: nothing, unless it is held on a being, then the way to where that being is drawn this very frame.
	 */
	public Vec3 drawing(float partial) {
		drawing = partial;
		Entity on = partial < 0 ? null : anchored();
		if (on == null) return Vec3.ZERO;
		Vec3 own = new Vec3(net.minecraft.util.Mth.lerp(partial, xo, getX()), net.minecraft.util.Mth.lerp(partial, yo, getY()), net.minecraft.util.Mth.lerp(partial, zo, getZ()));
		return anchoring().at(on, new Vec3(entityData.get(ANCHOR_OFFSET)), partial, entityData.get(YAW)).subtract(own);
	}
	public float value() { return entityData.get(VALUE); }

	public Entity owner() {
		int id = entityData.get(OWNER);
		return id <= 0 ? null : level().getEntity(id);
	}

	public Entity target() {
		int id = entityData.get(TARGET);
		return id <= 0 ? null : level().getEntity(id);
	}

	/** How far through its life it is, 0 to 1. */
	public float progress(float partialTick) {
		return Math.min(1, (tickCount + partialTick) / Math.max(1, life()));
	}

	@Override
	protected void defineSynchedData() {
		entityData.define(KIND, (byte) 0);
		entityData.define(RADIUS, 1.0f);
		entityData.define(HEIGHT, 1.0f);
		entityData.define(LIFE, 20);
		entityData.define(OWNER, 0);
		entityData.define(TARGET, 0);
		entityData.define(DATA, 0);
		entityData.define(YAW, 0.0f);
		entityData.define(PITCH, 0.0f);
		entityData.define(VALUE, 1.0f);
		entityData.define(STRIKEABLE, false);
		entityData.define(ANCHOR, 0);
		entityData.define(ANCHOR_HOW, (byte) 0);
		entityData.define(ANCHOR_OFFSET, new org.joml.Vector3f());
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (RADIUS.equals(key) || HEIGHT.equals(key) || STRIKEABLE.equals(key)) refreshDimensions();
	}

	/** A strikeable field's body is its cage; any other's a speck (nothing bumps into it). */
	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return entityData.get(STRIKEABLE) ? EntityDimensions.scalable(radius() * 2, height()) : EntityDimensions.scalable(0.1f, 0.1f);
	}

	@Override
	public void tick() {
		super.tick();
		// Held on a being: kept where it is on a watcher's screen (it is drawn where that being is drawn each frame: #drawing).
		if (level().isClientSide) {
			Entity on = anchored();
			if (on != null) setPos(anchoring().at(on, new Vec3(entityData.get(ANCHOR_OFFSET)), 1, entityData.get(YAW)));
		}
		// Its spell ends it; this is only lest one be left behind (its spell gone with the server or its caster).
		if (!level().isClientSide && (tickCount > life() + 100 || fleeting && tickCount >= life())) discard();
	}

	@Override
	public boolean isPickable() { return entityData.get(STRIKEABLE) && !isRemoved(); }

	@Override
	public boolean isAttackable() { return entityData.get(STRIKEABLE); }

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (level().isClientSide || !entityData.get(STRIKEABLE) || isInvulnerableTo(source)) return false;
		if (strikes != null) strikes.struck(this, source, amount);
		return true;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) { return distance < 320 * 320; }

	@Override
	public boolean isPushable() { return false; }

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
