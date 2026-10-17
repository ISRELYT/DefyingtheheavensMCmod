package com.example.defyingtheheavens;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * A thing an element spell throws (a bolt of flame, a wooden stake, a sword, a boulder, a serpent of fire...), drawn by its
 * {@link Shape} on the client (SpellProjectileRenderer). It may first form in the air beside its caster ({@link #formTicks}: it
 * hangs where {@link #formAt} puts it, following the caster's turning, while the client draws it gathering), then it flies where
 * the caster looks at that moment, straight, falling, or bending toward a mark ({@link #homingTarget}). What it does when it
 * strikes is its spell's ({@link Behaviour}, set on the server); it is never saved.
 */
public class SpellProjectile extends Projectile {
	/** What a spell's missile looks like (and a few things about it). Synced by ordinal: add new ones at the end. */
	public enum Shape {
		FLAME_BOLT, WOOD_STAKE(true), EARTH_PODAO, WATER_ARROW, FLYING_SWORD, FIRE_SERPENT, BOULDER, CLEAVE_SLASH, RAZOR_LEAF(true),
		ICE_SPEAR, METEOR, PHOENIX, PLUNGING_SWORD, FALLING_SUN, COLOSSAL_SWORD, MOUNTAIN, MOUNTAIN_SLASH, WATER_BALL, YANG_BOLT,
		/** Heaven-Severing Sword's slash (synced by place: new shapes go at the end). */
		HEAVEN_SLASH;

		private final boolean wooden;

		Shape() {
			this(false);
		}

		Shape(boolean wooden) {
			this.wooden = wooden;
		}

		/** Of wood: it burns up against a ward of fire (SpellAuras#incoming). */
		public boolean wooden() { return wooden; }

		public static Shape byIndex(int index) {
			return index >= 0 && index < values().length ? values()[index] : FLAME_BOLT;
		}
	}

	/** What a spell's missile does when it strikes (server side). */
	public interface Behaviour {
		/** It struck {@code target}; true to fly on through it. */
		boolean hitEntity(SpellProjectile missile, LivingEntity target);

		/** It struck a block (and is spent). */
		void hitBlock(SpellProjectile missile, BlockHitResult hit);

		/** It flew its full reach without striking anything (and is spent). */
		default void expire(SpellProjectile missile) {}

		/** Once a tick while it flies (a trail it leaves, say). */
		default void flying(SpellProjectile missile) {}

		/** It has just set off from where it formed. */
		default void launched(SpellProjectile missile) {}
	}

	private static final EntityDataAccessor<Byte> SHAPE = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> FORM_TICKS = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.INT);
	/** Where it forms, beside its caster: right, up and ahead of the eyes, in blocks (a ring of swords each has its own). */
	private static final EntityDataAccessor<org.joml.Vector3f> FORM_AT = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.VECTOR3);
	private static final EntityDataAccessor<Integer> HOMING = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.INT);
	/** Its shape's own word: a boulder's block (its state id), a serpent's or a sun's seed. */
	private static final EntityDataAccessor<Integer> DATA = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> HIT_SIZE = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.FLOAT);

	// Server side only.
	private Behaviour behaviour;
	private double speed = 2.0;
	private double gravity;
	private double range = 40;
	/** Degrees a tick it may turn toward its mark. */
	private double homingRate;
	private int pierce;
	private final Set<Integer> struck = new HashSet<>();
	private double flown;
	private boolean launched;
	/** It flies through the ground as through air (a cleaving slash: its spell cuts what it passes). */
	private boolean phasing;
	/** Its own spell's realm and element, for what it strikes (and the ward it meets). */
	private Spell spell;

	public SpellProjectile(EntityType<? extends SpellProjectile> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	/**
	 * A missile of {@code shape} for {@code spell}, cast by {@code caster}: it forms for {@code formTicks} at {@code formAt} (right,
	 * up, ahead of the caster's eyes), then flies {@code speed} blocks a tick for up to {@code range} blocks.
	 */
	public static SpellProjectile cast(ServerPlayer caster, Spell spell, Shape shape, float scale, int formTicks, Vec3 formAt, double speed,
									   double range, Behaviour behaviour) {
		SpellProjectile missile = new SpellProjectile(ModEntities.SPELL_PROJECTILE, caster.level());
		missile.setOwner(caster);
		missile.spell = spell;
		missile.behaviour = behaviour;
		missile.speed = speed;
		missile.range = range;
		missile.entityData.set(SHAPE, (byte) shape.ordinal());
		missile.entityData.set(SCALE, scale);
		missile.entityData.set(FORM_TICKS, Math.max(0, formTicks));
		missile.entityData.set(FORM_AT, new org.joml.Vector3f((float) formAt.x, (float) formAt.y, (float) formAt.z));
		Vec3 at = formPosition(caster, formAt, 1.0f);
		missile.moveTo(at.x, at.y, at.z, -caster.getYRot(), -caster.getXRot());
		caster.level().addFreshEntity(missile);
		if (formTicks <= 0) missile.launch();
		return missile;
	}

	/** A missile that sets off from {@code from} at once along {@code velocity} (one of a ring of swords plunging, a falling sun). */
	public static SpellProjectile thrown(ServerLevel level, ServerPlayer caster, Spell spell, Shape shape, float scale, Vec3 from, Vec3 velocity,
										 double range, Behaviour behaviour) {
		SpellProjectile missile = new SpellProjectile(ModEntities.SPELL_PROJECTILE, level);
		missile.setOwner(caster);
		missile.spell = spell;
		missile.behaviour = behaviour;
		missile.speed = velocity.length();
		missile.range = range;
		missile.entityData.set(SHAPE, (byte) shape.ordinal());
		missile.entityData.set(SCALE, scale);
		missile.moveTo(from.x, from.y, from.z, 0, 0);
		missile.setDeltaMovement(velocity);
		missile.launched = true;
		missile.turnToMotion();
		level.addFreshEntity(missile);
		return missile;
	}

	// --- Setup (server) ---

	public SpellProjectile gravity(double gravity) { this.gravity = gravity; return this; }
	/** Flies through the ground, striking only beings (every one its path sweeps through). */
	public SpellProjectile phasing() { this.phasing = true; return this; }
	public SpellProjectile pierce(int pierce) { this.pierce = pierce; return this; }
	/** Turns toward {@code target} at up to {@code degreesPerTick} (null: toward whoever it finds near its path). */
	public SpellProjectile homing(LivingEntity target, double degreesPerTick) {
		this.homingRate = degreesPerTick;
		if (target != null) entityData.set(HOMING, target.getId());
		return this;
	}
	public SpellProjectile data(int data) { entityData.set(DATA, data); return this; }
	public SpellProjectile hitSize(float size) { entityData.set(HIT_SIZE, size); refreshDimensions(); return this; }

	// --- What it is ---

	public Shape shape() { return Shape.byIndex(entityData.get(SHAPE)); }
	public float scale() { return entityData.get(SCALE); }
	public int formTicks() { return entityData.get(FORM_TICKS); }
	public int data() { return entityData.get(DATA); }
	public Spell spell() { return spell; }
	public boolean isWooden() { return shape().wooden(); }
	/** Whether it is still forming beside its caster. */
	public boolean forming() { return tickCount < formTicks(); }

	/** Its mark, if it has one in sight (client and server alike). */
	public Entity homingTarget() {
		int id = entityData.get(HOMING);
		return id <= 0 ? null : level().getEntity(id);
	}

	@Override
	protected void defineSynchedData() {
		entityData.define(SHAPE, (byte) 0);
		entityData.define(SCALE, 1.0f);
		entityData.define(FORM_TICKS, 0);
		entityData.define(FORM_AT, new org.joml.Vector3f(0, -0.3f, 1.4f));
		entityData.define(HOMING, 0);
		entityData.define(DATA, 0);
		entityData.define(HIT_SIZE, 0.5f);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (HIT_SIZE.equals(key)) refreshDimensions();
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		float size = entityData.get(HIT_SIZE);
		return EntityDimensions.scalable(size, size);
	}

	/** Where something forming at {@code offset} (right, up, ahead of the eyes) beside {@code owner} hangs. */
	public static Vec3 formPosition(Entity owner, Vec3 offset, float partialTick) {
		Vec3 eye = owner.getEyePosition(partialTick);
		float yaw = owner instanceof LivingEntity living ? net.minecraft.util.Mth.rotLerp(partialTick, living.yHeadRotO, living.yHeadRot) : owner.getViewYRot(partialTick);
		Vec3 ahead = Vec3.directionFromRotation(0, yaw);
		Vec3 right = new Vec3(-ahead.z, 0, ahead.x);
		return eye.add(right.scale(offset.x)).add(0, offset.y, 0).add(ahead.scale(offset.z));
	}

	/** Where it forms now (its caster's offset). */
	public Vec3 formPosition(float partialTick) {
		Entity owner = getOwner();
		if (owner == null) return getPosition(partialTick);
		org.joml.Vector3f at = entityData.get(FORM_AT);
		return formPosition(owner, new Vec3(at.x, at.y, at.z), partialTick);
	}

	// --- Flight ---

	/** Sets off toward where its caster looks (or its mark), from where it formed. */
	public void launch() {
		if (launched) return;
		launched = true;
		Entity owner = getOwner();
		Vec3 from = position();
		Vec3 to;
		if (owner instanceof ServerPlayer caster) {
			LivingEntity aimed = HeavenSpells.aimed(caster, range);
			to = aimed != null ? HeavenSpells.middle(aimed) : HeavenSpells.lookPoint(caster, range);
			if (homingRate > 0 && aimed != null && entityData.get(HOMING) <= 0) entityData.set(HOMING, aimed.getId());
		} else if (owner != null) {
			to = owner.getEyePosition().add(owner.getLookAngle().scale(range));
		} else {
			to = from.add(getLookAngle().scale(range));
		}
		Vec3 dir = to.subtract(from);
		if (dir.lengthSqr() < 1.0e-4) dir = owner != null ? owner.getLookAngle() : new Vec3(0, 0, 1);
		setDeltaMovement(dir.normalize().scale(speed));
		turnToMotion();
		if (behaviour != null) behaviour.launched(this);
	}

	@Override
	public void tick() {
		super.tick();
		if (forming()) {
			Vec3 at = formPosition(1.0f);
			setPos(at.x, at.y, at.z);
			setDeltaMovement(Vec3.ZERO);
			Entity owner = getOwner();
			if (owner != null) {
				// Its own way of facing (turnToMotion): yaw from +z toward +x, pitch upward; a looker's yaw and pitch run the other way.
				setYRot(-owner.getYRot());
				setXRot(-owner.getXRot());
			}
			if (!level().isClientSide && (owner == null || !owner.isAlive())) discard();
			return;
		}
		if (!level().isClientSide && !launched) launch();
		Vec3 v = getDeltaMovement();
		if (homingRate > 0 && !level().isClientSide) v = steer(v);
		if (!level().isClientSide) {
			if (phasing) {
				// Every being its path sweeps through this tick, the ground no matter.
				for (Entity e : level().getEntities(this, getBoundingBox().expandTowards(v).inflate(0.5), this::canHitEntity)) {
					onHitEntity(new EntityHitResult(e));
					if (isRemoved()) return;
				}
			} else {
				HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
				if (hit.getType() != HitResult.Type.MISS) {
					onHit(hit);
					if (isRemoved()) return;
				}
			}
			if (behaviour != null) behaviour.flying(this);
			flown += v.length();
			if (flown > range) {
				if (behaviour != null) behaviour.expire(this);
				discard();
				return;
			}
		}
		setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
		if (gravity > 0) v = v.add(0, -gravity, 0);
		setDeltaMovement(v);
		turnToMotion();
	}

	/** Bends the flight toward its mark (or, with none, the nearest foe of its caster's near its path), no faster than its turn. */
	private Vec3 steer(Vec3 v) {
		Entity target = homingTarget();
		if (target == null || !target.isAlive()) {
			target = null;
			if (getOwner() instanceof ServerPlayer caster) {
				double best = 0;
				for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(10), e -> HeavenSpells.enemy(caster, e))) {
					Vec3 to = HeavenSpells.middle(e).subtract(position());
					double facing = to.normalize().dot(v.normalize());
					if (facing > 0.6 && facing > best) {
						best = facing;
						target = e;
					}
				}
				if (target != null) entityData.set(HOMING, target.getId());
			}
		}
		if (target == null) return v;
		Vec3 want = (target instanceof LivingEntity living ? HeavenSpells.middle(living) : target.position()).subtract(position()).normalize();
		Vec3 have = v.normalize();
		double angle = Math.acos(Math.max(-1, Math.min(1, have.dot(want))));
		double max = Math.toRadians(homingRate);
		if (angle <= max || angle < 1.0e-4) return want.scale(v.length());
		double t = max / angle;
		return have.scale(1 - t).add(want.scale(t)).normalize().scale(v.length());
	}

	private void turnToMotion() {
		Vec3 v = getDeltaMovement();
		if (v.lengthSqr() < 1.0e-6) return;
		double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);
		setYRot((float) (Math.toDegrees(Math.atan2(v.x, v.z))));
		setXRot((float) (Math.toDegrees(Math.atan2(v.y, horizontal))));
		yRotO = getYRot();
		xRotO = getXRot();
	}

	@Override
	protected boolean canHitEntity(Entity target) {
		if (!(target instanceof LivingEntity living) || struck.contains(target.getId()) || target == getOwner()) return false;
		if (target.isSpectator() || !target.isAlive()) return false;
		if (getOwner() instanceof ServerPlayer caster && HeavenSpells.spared(caster, living)) return false;
		return !(target instanceof net.minecraft.world.entity.player.Player player && Incorporeal.untouchable(player));
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		if (!(hit.getEntity() instanceof LivingEntity target)) return;
		struck.add(target.getId());
		boolean on = behaviour != null && behaviour.hitEntity(this, target);
		if (!on || pierce-- <= 0) discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		if (behaviour != null) behaviour.hitBlock(this, hit);
		discard();
	}

	/** {@code element}'s qi harm of its spell's realm, by its caster, through it. */
	public DamageSource harm(QiElement element) {
		Realm realm = spell == null ? Realm.QI_REFINING : spell.heavenRealm();
		return ModDamageTypes.qi(level(), element, realm, this, getOwner());
	}

	@Override
	public boolean isPickable() { return false; }

	@Override
	public boolean hurt(DamageSource source, float amount) { return false; }

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) { return distance < 256 * 256; }
}
