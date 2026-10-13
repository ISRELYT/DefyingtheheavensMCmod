package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * A Hungry Ghost (饿鬼): one of the dead who starved in life and hungers still, gaunt, translucent and cyan (drawn with the
 * ghost shader in a skin of its own, SoulRenderer). About Qi Refining strength, {@link #HEALTH} health; packs of two to four,
 * most often in the Bone Wastes, spawning in any light but never on the Ghost Gate's ground.
 * <p>
 * It goes for the living first, then for ghosts. A ghost it strikes takes {@link #ON_THE_DEAD} (ghostly harm); the living take
 * only {@link #ON_THE_LIVING}, but each blow drives {@link #YIN_PER_BLOW} of the Netherworld's yin into them. Blows reach it as
 * they reach any of the dead (GhostSystem#reaches); a ghost who disperses one takes {@link #QI_REWARD} ghostly qi from it.
 */
public class HungryGhost extends Monster implements Ghostly {
	public static final float HEALTH = 16.0f;
	public static final float ON_THE_DEAD = 3.0f, ON_THE_LIVING = 1.0f;
	/** A blow on the living fills them with this much yin (5% of the way to becoming one of the dead). */
	public static final double YIN_PER_BLOW = 0.05;
	public static final double QI_REWARD = 5;

	public HungryGhost(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		xpReward = 5;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, HEALTH)
				.add(Attributes.ATTACK_DAMAGE, ON_THE_DEAD)
				.add(Attributes.MOVEMENT_SPEED, 0.27)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		// The living first; only with none about, the dead.
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, target -> !GhostSystem.isDead(target)));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, GhostSystem::isDead));
	}

	/** Its own harm: heavy on the dead, light on the living but carrying the Netherworld's yin. */
	@Override
	public boolean doHurtTarget(Entity target) {
		if (!(target instanceof LivingEntity living) || !(level() instanceof ServerLevel level)) return false;
		boolean dead = GhostSystem.isDead(living);
		boolean hit = living.hurt(ModDamageTypes.ghostly(level, this), dead ? ON_THE_DEAD : ON_THE_LIVING);
		if (hit && !dead && living instanceof ServerPlayer player && YinCorruption.affects(player)) {
			NetherworldState s = CultivationManager.get(player).netherworld();
			s.setYinCorruption(s.yinCorruption() + YIN_PER_BLOW);
			NetherworldManager.refresh(player);
		}
		if (hit) level.sendParticles(ParticleTypes.SOUL, living.getX(), living.getY() + 1, living.getZ(), 4, 0.3, 0.4, 0.3, 0.02);
		return hit;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (level().isClientSide) return super.hurt(source, amount);
		if (!GhostSystem.reaches(source)) return false;
		return super.hurt(source, GhostSystem.scaled(source, amount));
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!(level() instanceof ServerLevel level)) return;
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1, getZ(), 30, 0.3, 0.6, 0.3, 0.06);
		// A ghost that disperses it takes in what it hungered on.
		if (source.getEntity() instanceof ServerPlayer killer && GhostSystem.isGhost(killer)) {
			NetherworldState s = CultivationManager.get(killer).netherworld();
			s.setGhostQi(Math.min(GhostQi.poolMax(CultivationManager.get(killer)), s.ghostQi() + QI_REWARD));
			NetherworldManager.refresh(killer);
		}
	}

	@Override
	protected void tickDeath() {
		if (!level().isClientSide && !isRemoved()) remove(RemovalReason.KILLED);
	}

	@Override public boolean fireImmune() { return true; }
	@Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }
	@Override public boolean isSensitiveToWater() { return false; }
	/** The dead tread without a sound: no footsteps, no splashing (a player's ghost likewise, SoulPlayerMixin). */
	@Override protected MovementEmission getMovementEmission() { return MovementEmission.NONE; }

	@Override protected SoundEvent getAmbientSound() { return SoundEvents.VEX_AMBIENT; }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.VEX_HURT; }
	@Override protected SoundEvent getDeathSound() { return SoundEvents.ALLAY_DEATH; }
	@Override public float getVoicePitch() { return 0.5f + random.nextFloat() * 0.1f; }
	@Override public SoundSource getSoundSource() { return SoundSource.HOSTILE; }

	/**
	 * A natural spawning try in a chunk comes to anything this often (decided once a chunk a second, so a pack comes whole or not
	 * at all): most often in the Bone Wastes. And never more than {@link #CROWD_CAP} within {@link #CROWD} blocks.
	 */
	public static final float SPAWN_CHANCE_BONE_WASTES = 0.3f, SPAWN_CHANCE = 0.06f;
	public static final int CROWD_CAP = 8;
	public static final double CROWD = 48;

	/** Spawns in any light, on solid ground, but never on the Ghost Gate's ground, nor in peace. */
	public static boolean checkSpawn(EntityType<HungryGhost> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
		if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
		if (reason == MobSpawnType.NATURAL) {
			if (GhostGate.protects(level.getLevel(), pos)) return false;
			boolean bones = level.getBiome(pos).is(ModBiomes.BONE_WASTES);
			long seed = net.minecraft.world.level.ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4) ^ (level.getLevel().getGameTime() / 20) * 0x9E3779B97F4A7C15L;
			if (RandomSource.create(seed).nextFloat() >= (bones ? SPAWN_CHANCE_BONE_WASTES : SPAWN_CHANCE)) return false;
			if (level.getEntitiesOfClass(HungryGhost.class, new net.minecraft.world.phys.AABB(pos).inflate(CROWD)).size() >= CROWD_CAP) return false;
		}
		return checkMobSpawnRules(type, level, reason, pos, random);
	}
}
