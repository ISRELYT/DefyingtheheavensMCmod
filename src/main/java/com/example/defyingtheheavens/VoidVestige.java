package com.example.defyingtheheavens;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A Vestige of the Void (the user's, 2026-10-17): a black thing of the deep void, shaped like the dragonfish of the deep sea
 * (long eel's body, a great fanged jaw jutting below, a huge eye, lights along its belly; drawn by the client, VestigeRenderer),
 * that hunts whoever flies open space. Never on a planet, nor in a planet's air: it turns away from the planets, and one who
 * reaches a planet's sky is safe from it.
 * <ul>
 *   <li><b>Strength.</b> A cultivator's rank, from Nascent Soul Early to Heavenly Being Grand Perfection (the most the Astral
 *   Realm, a lower realm, sustains): health and bite as a cultivator of that stage, half again as tough.</li>
 *   <li><b>Size.</b> Larger with every stage: some 14 blocks long at Nascent Soul Early, near a hundred at Grand Perfection
 *   ({@link #scale}); its hitbox is its head and jaws.</li>
 *   <li><b>Speed.</b> A little faster than a cultivator of its own stage flies in space, sprinting ({@link #maxSpeed}): one who has
 *   only just reached the void can't outrun one of their own strength; a stronger cultivator can.</li>
 *   <li><b>Its hunt.</b> It goes for the nearest flier in open space within {@link #HUNT_RANGE}, leading its prey, biting when in
 *   reach, now and then lunging; it gives up on one who reaches a planet's air. Unseen by anyone for long, it slips away.</li>
 * </ul>
 * Where they spawn, and how strong, is VestigeSpawner's. They hunt the Void Breach's climbers too (VoidBreach, 2026-10-25): there of a
 * Heavenly Being's size but its prey's own might ({@link #setMight}), no swifter than the climb ({@link #BREACH_SPEED}).
 */
public class VoidVestige extends Monster {
	public static final int MIN_RANK = PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY);
	public static final int MAX_RANK = PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION);
	/**
	 * The mightiest a Vestige comes ({@link #setMight}): Sacred Vessel's Grand Perfection (the user's ask of 2026-10-27: a breach's came
	 * at its climber's own might, True Immortal's for one past it, and even an Entering Nirvana could hardly climb to the Great Void).
	 */
	public static final int MAX_MIGHT = PlayerCultivation.rank(Realm.SACRED_VESSEL, Stage.GRAND_PERFECTION);
	/** Its length at Nascent Soul Early, in blocks; each stage more multiplies it by {@link #GROWTH}. */
	public static final double BASE_LENGTH = 14, GROWTH = 1.32;
	/** It hunts prey this near, and keeps this far out of any planet's air. */
	public static final double HUNT_RANGE = 160, AIR_MARGIN = 50;
	/** Between bites (ticks). */
	private static final int BITE_COOLDOWN = 30;
	/**
	 * In a Void Breach's shattered space (VoidBreach, 2026-10-25) it hunts those who climb it: no faster than this (blocks a tick, the
	 * climb rising at half a block), and kept this far inside the breach's floor and ceiling.
	 */
	static final double BREACH_SPEED = 1.1, BREACH_MARGIN = 20;

	private static final EntityDataAccessor<Integer> RANK = SynchedEntityData.defineId(VoidVestige.class, EntityDataSerializers.INT);
	/** The rank its strength is of, when that isn't its size's ({@link #setMight}: a breach's); -1, its size's own. */
	private static final EntityDataAccessor<Integer> MIGHT = SynchedEntityData.defineId(VoidVestige.class, EntityDataSerializers.INT);

	private ServerPlayer prey;
	private Vec3 lastPreyPos;
	private int biteCooldown, lungeTicks, nextLunge = 100, alone;
	private Vec3 wander = new Vec3(1, 0, 0);

	/** The client's: where its head has been, newest first, so its body follows its path (VestigeRenderer). */
	public final Deque<Vec3> trail = new ArrayDeque<>();
	/** The client's: ticks since it last bit (its jaw snaps shut, then gapes again). */
	public int biteAnimation = 100;

	public VoidVestige(EntityType<? extends VoidVestige> type, Level level) {
		super(type, level);
		setNoGravity(true);
		noPhysics = true;
		xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 100)
				.add(Attributes.ATTACK_DAMAGE, 12)
				.add(Attributes.ARMOR, 9)
				.add(Attributes.ARMOR_TOUGHNESS, 4)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.FOLLOW_RANGE, HUNT_RANGE)
				.add(Attributes.MOVEMENT_SPEED, 0.3);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(RANK, MIN_RANK);
		entityData.define(MIGHT, -1);
	}

	public int getRank() { return entityData.get(RANK); }

	public Realm realm() { return CultivatorNpc.realmOf(getRank()); }

	public Stage stage() { return CultivatorNpc.stageOf(getRank()); }

	/** The rank its strength is of: its {@link #setMight might}'s, else its size's own. */
	public int mightRank() {
		int might = entityData.get(MIGHT);
		return might >= 0 ? might : getRank();
	}

	/**
	 * Its strength (health, bite, hide) that of rank {@code rank}, any rank, its size unchanged: one of a Void Breach's, of the might of
	 * whoever it comes for (a Heavenly Being's size there, a Dao Integration's or Void Shattering's bite), never past {@link #MAX_MIGHT}
	 * (one saved mightier comes back so). Call after {@link #setRank}.
	 */
	public void setMight(int rank) {
		rank = Mth.clamp(rank, MIN_RANK, MAX_MIGHT);
		entityData.set(MIGHT, rank);
		strengthOf(CultivatorNpc.realmOf(rank), CultivatorNpc.stageOf(rank));
	}

	/** Health and bite as a cultivator of {@code realm}'s {@code stage}, half again as tough, and its hide. */
	private void strengthOf(Realm realm, Stage stage) {
		getAttribute(Attributes.MAX_HEALTH).setBaseValue((20 + CultivationStats.maxHealth(realm, stage)) * 1.5);
		getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(1 + CultivationStats.attackDamage(realm, stage));
		getAttribute(Attributes.ARMOR).setBaseValue(CultivationStats.armor(realm, stage));
		getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(CultivationStats.toughness(realm, stage));
		setHealth(getMaxHealth());
	}

	/** Sets its strength (and size) to cultivation rank {@code rank} (clamped to Nascent Soul Early..Heavenly Being Grand Perfection). */
	public void setRank(int rank) {
		rank = Mth.clamp(rank, MIN_RANK, MAX_RANK);
		entityData.set(RANK, rank);
		entityData.set(MIGHT, -1);
		strengthOf(CultivatorNpc.realmOf(rank), CultivatorNpc.stageOf(rank));
		refreshDimensions();
	}

	/** How much larger than one of Nascent Soul Early it is. */
	public double scale() { return Math.pow(GROWTH, getRank() - MIN_RANK); }

	/** Its whole length, head to tail. */
	public double length() { return BASE_LENGTH * scale(); }

	/**
	 * Its top speed (blocks a tick): a little more than a cultivator of its own stage flying in space at a sprint (a flier's speed
	 * runs to about 20 times the flying speed when sprinting), and the faster when lunging.
	 */
	public double maxSpeed() {
		double flier = 20.2 * AstralRealm.spaceSpeed(realm(), stage());
		return flier * 1.08 * (lungeTicks > 0 ? 1.5 : 1);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (RANK.equals(key)) refreshDimensions();
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		double s = scale();
		return EntityDimensions.scalable((float) Math.min(12, 2.4 * s), (float) Math.min(8, 1.8 * s));
	}

	@Override
	public Component getName() {
		int might = mightRank();
		return Component.translatable(ModLang.VESTIGE_NAME, PlayerCultivation.rankName(CultivatorNpc.realmOf(might), CultivatorNpc.stageOf(might)));
	}

	// --- Its hunt ---

	@Override
	protected void customServerAiStep() {
		// Space's, or a Void Breach's (VoidBreach): there are no planets in broken space, only its floor and ceiling.
		boolean breach = ModDimensions.isVoidBreach(level().dimension());
		if (!(level() instanceof ServerLevel server) || !ModDimensions.isSpace(level().dimension()) && !breach) {
			discard();
			return;
		}
		AstralPlanets planets = breach ? null : AstralPlanets.get();
		if (planets == null && !breach) return;
		if (biteCooldown > 0) biteCooldown--;
		if (lungeTicks > 0) lungeTicks--;

		// One gone from space (down to a planet, home, or out of the game) is let go at once, not hunted on till its next look.
		if (prey != null && !huntable(prey)) prey = null;
		if (prey == null || tickCount % 10 == 0) choosePrey(server, planets);
		Vec3 pos = position().add(0, getBbHeight() / 2, 0);
		double max = breach ? Math.min(maxSpeed(), BREACH_SPEED * (lungeTicks > 0 ? 1.5 : 1)) : maxSpeed();
		Vec3 desired;
		if (prey != null) {
			Vec3 preyAt = prey.position().add(0, prey.getBbHeight() / 2, 0);
			Vec3 preyMove = lastPreyPos == null ? Vec3.ZERO : prey.position().subtract(lastPreyPos);
			lastPreyPos = prey.position();
			double distance = preyAt.distanceTo(pos);
			// Leading its prey, a little ahead of where it's going.
			Vec3 aimAt = preyAt.add(preyMove.scale(Math.min(20, distance / Math.max(0.5, max))));
			desired = aimAt.subtract(pos).normalize().scale(max);
			if (--nextLunge <= 0 && distance < 25 * Math.sqrt(scale())) {
				lungeTicks = 12;
				nextLunge = 80 + random.nextInt(60);
				playSound(SoundEvents.WARDEN_SONIC_CHARGE, 2.0f, 0.5f);
			}
			if (biteCooldown <= 0 && reaches(prey)) bite(prey);
		} else {
			// No prey: a slow wandering.
			if (tickCount % 40 == 0) wander = wander.add(random.nextGaussian() * 0.5, random.nextGaussian() * 0.2, random.nextGaussian() * 0.5).normalize();
			desired = wander.scale(max * 0.2);
		}

		// Never into a planet's air: it turns away from it, the harder the deeper it has come.
		AstralPlanets.Planet near = planets == null ? null : planets.atmosphereAt(pos, AIR_MARGIN);
		if (near != null) {
			Vec3 out = pos.subtract(near.pos()).normalize();
			double depth = (near.atmosphere() + AIR_MARGIN - pos.distanceTo(near.pos())) / AIR_MARGIN;
			desired = desired.add(out.scale(max * Mth.clamp(depth * 2.5, 0, 3)));
		}
		// Nor out past space's floor or ceiling (or the breach's).
		double floor = breach ? ModDimensions.VOID_BREACH_MIN_Y + BREACH_MARGIN : AstralPlanets.SPACE_FLOOR + 60;
		double ceiling = breach ? ModDimensions.VOID_BREACH_MIN_Y + ModDimensions.VOID_BREACH_HEIGHT - BREACH_MARGIN : AstralPlanets.SPACE_CEILING - 60;
		if (pos.y < floor) desired = desired.add(0, max, 0);
		if (pos.y > ceiling) desired = desired.add(0, -max, 0);

		// Turning as a great body turns: the larger, the slower to change its course.
		Vec3 velocity = getDeltaMovement();
		Vec3 change = desired.subtract(velocity);
		double agility = max * 0.11 / Math.sqrt(scale());
		if (change.length() > agility) change = change.normalize().scale(agility);
		velocity = velocity.add(change);
		if (velocity.length() > max * 1.05) velocity = velocity.normalize().scale(max);
		setDeltaMovement(velocity);
		if (velocity.lengthSqr() > 1e-4) {
			float yaw = (float) (Mth.atan2(velocity.z, velocity.x) * Mth.RAD_TO_DEG) - 90.0f;
			float pitch = (float) (-Mth.atan2(velocity.y, velocity.horizontalDistance()) * Mth.RAD_TO_DEG);
			setYRot(yaw);
			setXRot(pitch);
			yHeadRot = yaw;
			yBodyRot = yaw;
		}

		// Unseen for long, it slips back into the dark.
		if (server.getNearestPlayer(this, 400) == null) {
			if (++alone > 400) discard();
		} else {
			alone = 0;
		}
	}

	/** The nearest flier in open space it can hunt (not creative, not in a planet's air), or none. */
	private void choosePrey(ServerLevel server, AstralPlanets planets) {
		ServerPlayer best = null;
		double bestDistance = HUNT_RANGE * HUNT_RANGE;
		for (ServerPlayer player : server.players()) {
			if (!huntable(player)) continue;
			if (planets != null && planets.atmosphereAt(player.position(), AIR_MARGIN * 0.5) != null) continue;
			double d = player.distanceToSqr(this);
			// It keeps to the prey it has, unless another is much nearer.
			if (player == prey) d *= 0.6;
			if (d < bestDistance) {
				bestDistance = d;
				best = player;
			}
		}
		if (best != prey) lastPreyPos = null;
		prey = best;
		setTarget(best);
		if (best != null && entityData.get(MIGHT) >= 0 && ModDimensions.isVoidBreach(level().dimension())) matchMight(best);
	}

	/**
	 * A breach's Vestige (of a might: {@link #setMight}) of the might of the climber it hunts now (VoidBreach#mightFor), its wounds kept
	 * as a share of its health: one left from a mightier climb (the user's of 2026-10-27, fallen back from True Immortal and hunted by
	 * the Vestiges of their peak), or come upon a weaker climber, matches them.
	 */
	void matchMight(ServerPlayer climber) {
		int wanted = Mth.clamp(VoidBreach.mightFor(climber), MIN_RANK, MAX_MIGHT);
		if (wanted == mightRank()) return;
		float share = getHealth() / Math.max(1.0f, getMaxHealth());
		setMight(wanted);
		setHealth(getMaxHealth() * share);
	}

	/**
	 * Here in space with it, alive, and neither creative nor a spectator, nor a ghost scattered (invulnerable while it reforms: the
	 * dead are its prey too since 2026-10-24, GhostSystem#reaches).
	 */
	private boolean huntable(ServerPlayer player) {
		return player.isAlive() && !player.isRemoved() && player.level() == level() && !player.isCreative() && !player.isSpectator()
				&& !player.isInvulnerable();
	}

	/** Its jaws within reach of {@code target}: its head's reach out from its hitbox. */
	private boolean reaches(ServerPlayer target) {
		double reach = 1.5 + Math.sqrt(scale()) * 1.5;
		AABB jaws = getBoundingBox().inflate(reach);
		return jaws.intersects(target.getBoundingBox());
	}

	private void bite(ServerPlayer target) {
		biteCooldown = BITE_COOLDOWN;
		target.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
		Vec3 push = target.position().subtract(position()).normalize().scale(0.6 + 0.2 * Math.sqrt(scale()));
		target.push(push.x, push.y * 0.5, push.z);
		target.hurtMarked = true;
		level().broadcastEntityEvent(this, (byte) 4);
		level().playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 2.0f, 0.5f);
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == 4) biteAnimation = 0;
		else super.handleEntityEvent(id);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) {
			biteAnimation++;
			trail.addFirst(position().add(0, getBbHeight() / 2, 0));
			// Kept as long as its body (and a little more), or a few hundred ticks still.
			double length = 0, want = length() * 1.2;
			Vec3 last = null;
			int count = 0;
			for (java.util.Iterator<Vec3> it = trail.iterator(); it.hasNext(); ) {
				Vec3 p = it.next();
				if (last != null) length += last.distanceTo(p);
				last = p;
				if (++count > 600 || length > want && count > 4) {
					it.remove();
					while (it.hasNext()) {
						it.next();
						it.remove();
					}
					break;
				}
			}
			if (random.nextFloat() < 0.15f * Math.sqrt(scale())) {
				Vec3 tail = trail.peekLast();
				if (tail != null) level().addParticle(ParticleTypes.REVERSE_PORTAL, tail.x, tail.y, tail.z, 0, 0, 0);
			}
		}
	}

	@Override
	public void travel(Vec3 input) {
		// Its own course (customServerAiStep), unslowed by air: there is none.
		move(MoverType.SELF, getDeltaMovement());
	}

	@Override
	public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 700 * 700;
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		return getBoundingBox().inflate(length() + 4);
	}

	// --- Sounds, death ---

	@Override
	protected SoundEvent getAmbientSound() { return SoundEvents.ELDER_GUARDIAN_AMBIENT; }

	@Override
	protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ELDER_GUARDIAN_HURT; }

	@Override
	protected SoundEvent getDeathSound() { return SoundEvents.ELDER_GUARDIAN_DEATH; }

	@Override
	protected float getSoundVolume() { return (float) (2.0 + scale()); }

	@Override
	public float getVoicePitch() { return (float) (0.6 / Math.sqrt(scale())); }

	@Override
	public int getAmbientSoundInterval() { return 200; }

	/** What's left of it: spirit stones, finer the stronger it was. */
	@Override
	protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hitByPlayer) {
		super.dropCustomDeathLoot(source, looting, hitByPlayer);
		int over = mightRank() - MIN_RANK;
		Item stone = over >= 6 ? ModItems.HIGH_SPIRIT_STONE : over >= 3 ? ModItems.MID_SPIRIT_STONE : ModItems.LOW_SPIRIT_STONE;
		int count = 2 + random.nextInt(3) + over % 3 * 2;
		ItemEntity drop = spawnAtLocation(new ItemStack(stone, count));
		if (drop != null) drop.setNoGravity(true);
	}

	@Override
	public int getExperienceReward() {
		return 20 + (mightRank() - MIN_RANK) * 15;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("Rank", getRank());
		if (entityData.get(MIGHT) >= 0) tag.putInt("Might", entityData.get(MIGHT));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("Rank")) {
			float health = getHealth();
			setRank(tag.getInt("Rank"));
			if (tag.contains("Might")) setMight(tag.getInt("Might"));
			if (tag.contains("Health")) setHealth(health);
		}
	}
}
