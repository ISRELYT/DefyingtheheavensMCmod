package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A sword spirit (phase 3a of the Artifact Forge): a flying blade with a will of its own, passing through stone as a vex does.
 * <ul>
 *   <li><b>A tomb's</b> ({@link #guardian}): one of the old blades of Ganjiang's sword tomb, risen against whoever comes for the sword
 *   (Legends#tombTrial). It goes for players near its stone, never strays far from it, and fades when the trial is put off.</li>
 *   <li><b>Called</b> ({@link #called}): Twin Dragons, a spirit of Ganjiang or Moye flown out of the sword to fight for its owner a while:
 *   whoever its owner fights or is fought by, else the nearest foe about them, its blows its sword's fire or cold.</li>
 * </ul>
 * Never saved: a tomb's trial begins anew, and a called spirit's time is short.
 */
public class SwordSpirit extends Vex {
	private static final EntityDataAccessor<Byte> KIND = SynchedEntityData.defineId(SwordSpirit.class, EntityDataSerializers.BYTE);
	/** Its kinds: one of a tomb's old blades, Ganjiang's spirit, Moye's (synced: the client draws its sword). */
	public static final byte TOMB = 0, GANJIANG = 1, MOYE = 2;
	/** A tomb's spirit's strength: for one of Core Formation, the swords' own lowest realm, to beat. */
	static final double GUARDIAN_HEALTH = 70, GUARDIAN_BLOW = 9, GUARDIAN_ARMOR = 8;
	/** A tomb's spirit goes for players this near its stone, and never strays much further. */
	static final double LEASH = 20;
	/** A called spirit finds foes of its own this near its owner, and its owner's own foes this near. */
	static final double GUARD = 14, REACH = 32;
	/** A called spirit's health: this share of its owner's. */
	static final float CALLED_HEALTH = 0.5f;
	/** It strikes at most this often (ticks); it flies at its foe at this pace (VexMoveControl's), and drifts about at the other. */
	static final int STRIKE_EVERY = 12;
	static final double CHARGE = 1.2, DRIFT = 0.3;

	/** A tomb's spirit's stone. */
	private @Nullable BlockPos home;
	/** A called spirit's owner, and the ticks it has left. */
	private @Nullable UUID owner;
	private int life;
	/** Ticks before it may strike again. */
	private int strikeIn;

	public SwordSpirit(EntityType<? extends SwordSpirit> type, Level level) {
		super(type, level);
		xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Vex.createAttributes().add(Attributes.MAX_HEALTH, GUARDIAN_HEALTH).add(Attributes.ATTACK_DAMAGE, GUARDIAN_BLOW)
				.add(Attributes.ARMOR, GUARDIAN_ARMOR).add(Attributes.FOLLOW_RANGE, REACH);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(KIND, TOMB);
	}

	public byte kind() { return entityData.get(KIND); }

	private void setKind(byte kind) { entityData.set(KIND, kind); }

	/**
	 * None of a vex's: it chooses its own foes (not every player, as a vex's are, nor a mob master's), flies at them and strikes on its own
	 * (#customServerAiStep). The vex's own charge waited on its random drifting and, in the first tour, never came to a blow.
	 */
	@Override
	protected void registerGoals() {
	}

	// --- Made ---

	/** One of a tomb's old blades risen at {@code at} to keep the stone at {@code home} (Legends#wave). */
	static @Nullable SwordSpirit guardian(ServerLevel level, Vec3 at, BlockPos home) {
		SwordSpirit spirit = ModEntities.SWORD_SPIRIT.create(level);
		if (spirit == null) return null;
		spirit.moveTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360, 0);
		spirit.setKind(TOMB);
		spirit.home = home.immutable();
		spirit.setBoundOrigin(home.above(2));
		level.addFreshEntity(spirit);
		level.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 12, 0.3, 0.3, 0.3, 0.02);
		return spirit;
	}

	/** A spirit of {@code legend} flown out of its sword to fight for {@code owner} for {@code life} ticks, each blow {@code blow} (Twin Dragons). */
	static @Nullable SwordSpirit called(ServerLevel level, ServerPlayer owner, Legend legend, double blow, int life) {
		SwordSpirit spirit = ModEntities.SWORD_SPIRIT.create(level);
		if (spirit == null) return null;
		Vec3 side = owner.getLookAngle().cross(new Vec3(0, legend == Legend.GANJIANG ? 1 : -1, 0)).normalize();
		Vec3 at = owner.getEyePosition().add(owner.getLookAngle().scale(1.2)).add(side.scale(0.8));
		spirit.moveTo(at.x, at.y, at.z, owner.getYRot(), 0);
		spirit.setKind(legend == Legend.GANJIANG ? GANJIANG : MOYE);
		spirit.owner = owner.getUUID();
		spirit.life = life;
		spirit.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.max(1, blow));
		spirit.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(20, owner.getMaxHealth() * CALLED_HEALTH));
		spirit.setHealth(spirit.getMaxHealth());
		spirit.setBoundOrigin(owner.blockPosition().above(2));
		level.addFreshEntity(spirit);
		level.sendParticles(legend == Legend.GANJIANG ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z, 20, 0.3, 0.3, 0.3, 0.05);
		return spirit;
	}

	// --- Its fight ---

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		if (!(level() instanceof ServerLevel level)) return;
		if (kind() != TOMB) {
			ServerPlayer master = master();
			if (master == null || !master.isAlive() || master.level() != level || --life <= 0 || distanceToSqr(master) > 48 * 48) {
				fade(level);
				return;
			}
			if (tickCount % 20 == 0) setBoundOrigin(master.blockPosition().above(2));
		} else if (home != null && distanceToSqr(Vec3.atCenterOf(home)) > (LEASH + 8) * (LEASH + 8)) {
			// Strayed from its stone: back to it.
			moveTo(home.getX() + 0.5, home.getY() + 2.5, home.getZ() + 0.5);
			setTarget(null);
		}
		if (strikeIn > 0) strikeIn--;
		if (tickCount % 10 == 0) {
			LivingEntity target = getTarget();
			if (target != null && !fair(target)) setTarget(null);
			if (getTarget() == null) setTarget(find(level));
		}
		LivingEntity target = getTarget();
		if (target != null) {
			// At its foe point first; a blow on reaching it, and another each STRIKE_EVERY while it stays on it.
			Vec3 aim = target.position().add(0, target.getBbHeight() * 0.6, 0);
			getMoveControl().setWantedPosition(aim.x, aim.y, aim.z, CHARGE);
			getLookControl().setLookAt(target, 30, 30);
			if (strikeIn == 0 && getBoundingBox().inflate(0.5).intersects(target.getBoundingBox())) {
				doHurtTarget(target);
				strikeIn = STRIKE_EVERY;
			}
		} else if (!getMoveControl().hasWanted() || tickCount % 40 == 0) {
			// Nothing to fight: it hangs about its stone, or its owner.
			BlockPos origin = getBoundOrigin() != null ? getBoundOrigin() : blockPosition();
			getMoveControl().setWantedPosition(origin.getX() + 0.5 + random.nextInt(9) - 4, origin.getY() + random.nextInt(3) - 1,
					origin.getZ() + 0.5 + random.nextInt(9) - 4, DRIFT);
		}
	}

	/** Whether it fights {@code target}: a tomb's, a player near its stone; a called one, its owner's foe (never its owner, nor theirs). */
	private boolean fair(LivingEntity target) {
		if (!target.isAlive() || target.isRemoved() || target.level() != level()) return false;
		if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
		if (kind() == TOMB) return target instanceof Player && home != null && target.distanceToSqr(Vec3.atCenterOf(home)) <= (LEASH + 8) * (LEASH + 8);
		ServerPlayer master = master();
		return master != null && target != master && !(target instanceof SwordSpirit spirit && master.getUUID().equals(spirit.owner))
				&& Artifacts.mayStrike(master, target) && target.distanceToSqr(master) <= REACH * REACH;
	}

	/** Whom it goes for now: a tomb's, the nearest player near its stone; a called one, whoever its owner fights or is fought by, else the nearest foe about them. */
	private @Nullable LivingEntity find(ServerLevel level) {
		if (kind() == TOMB) {
			if (home == null) return null;
			Player near = level.getNearestPlayer(home.getX() + 0.5, home.getY() + 1, home.getZ() + 0.5, LEASH, e -> e instanceof LivingEntity living && fair(living));
			return near;
		}
		ServerPlayer master = master();
		if (master == null) return null;
		LivingEntity fought = master.getLastHurtMob();
		if (fought != null && master.tickCount - master.getLastHurtMobTimestamp() < 200 && fair(fought)) return fought;
		LivingEntity fighting = master.getLastHurtByMob();
		if (fighting != null && fair(fighting)) return fighting;
		LivingEntity best = null;
		double bestDistance = GUARD * GUARD;
		for (LivingEntity foe : level.getEntitiesOfClass(LivingEntity.class, master.getBoundingBox().inflate(GUARD), e -> e instanceof Enemy && fair(e))) {
			double d = foe.distanceToSqr(master);
			if (d < bestDistance) {
				best = foe;
				bestDistance = d;
			}
		}
		return best;
	}

	private @Nullable ServerPlayer master() {
		return owner != null && level() instanceof ServerLevel level ? level.getServer().getPlayerList().getPlayer(owner) : null;
	}

	/** A called spirit's blow carries its sword's own: Ganjiang's fire, Moye's cold. */
	@Override
	public boolean doHurtTarget(Entity target) {
		boolean hit = super.doHurtTarget(target);
		if (hit && target instanceof LivingEntity living && level() instanceof ServerLevel level) {
			Vec3 at = living.position().add(0, living.getBbHeight() * 0.6, 0);
			if (kind() == GANJIANG) {
				living.setSecondsOnFire(3);
				SpellFx.send(level, SpellFx.Kind.FLAME_BURST, at, Vec3.ZERO, 0.4f, 0);
			} else if (kind() == MOYE) {
				living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0), this);
				level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 10, 0.3, 0.3, 0.3, 0.02);
			}
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, getSoundSource(), 0.6f, 1.4f);
		}
		return hit;
	}

	/** Its owner's own blows pass through it. */
	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (owner != null && source.getEntity() != null && owner.equals(source.getEntity().getUUID())) return false;
		return super.hurt(source, amount);
	}

	/** Its time up, or its owner gone: back into the sword it came from. */
	private void fade(ServerLevel level) {
		level.sendParticles(kind() == GANJIANG ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.3, getZ(), 16, 0.2, 0.2, 0.2, 0.03);
		discard();
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide && tickCount % 2 == 0) {
			ParticleOptions trail = switch (kind()) {
				case GANJIANG -> ParticleTypes.SMALL_FLAME;
				case MOYE -> ParticleTypes.SOUL_FIRE_FLAME;
				default -> ParticleTypes.ENCHANTED_HIT;
			};
			level().addParticle(trail, getX(), getY() + getBbHeight() * 0.5, getZ(), 0, 0.01, 0);
		}
	}

	/** A called spirit stays in peace too (it fights nothing then); a tomb's goes as monsters do. */
	@Override
	protected boolean shouldDespawnInPeaceful() {
		return kind() == TOMB;
	}

	@Override protected SoundEvent getAmbientSound() { return SoundEvents.AMETHYST_BLOCK_CHIME; }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.CHAIN_HIT; }
	@Override protected SoundEvent getDeathSound() { return SoundEvents.ITEM_BREAK; }
}
