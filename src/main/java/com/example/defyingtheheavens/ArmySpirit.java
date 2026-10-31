package com.example.defyingtheheavens;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A soldier of the old army called up by the War General's Banner (WarGeneralsBannerItem, 2026-10-31): a Battle Spirit in gold light (the
 * Ancient Battlefield's model in the banner's colours, ArmySpiritRenderer) that fights beside the banner's bearer for {@link #LIFE} ticks, of their strength
 * (CultivationStats#applyNpc, a little less tough), going after whoever they strike or whoever strikes them, keeping near them
 * otherwise; then it fades. Never saved: the old army comes only while its banner calls.
 */
public class ArmySpirit extends PathfinderMob implements GeoEntity {
	/** How long it stays (ticks: forty seconds). */
	public static final int LIFE = 800;
	private UUID owner;
	private int left = LIFE;
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public ArmySpirit(EntityType<? extends ArmySpirit> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FOLLOW_RANGE, 24.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
	}

	/** Called up beside {@code bearer}: theirs, and of their strength. */
	public void serve(ServerPlayer bearer) {
		owner = bearer.getUUID();
		int rank = TwoPaths.higherRank(CultivationManager.get(bearer), true);
		CultivationStats.applyNpc(this, CultivatorNpc.realmOf(rank), CultivatorNpc.stageOf(rank), false, 0.7);
		setHealth(getMaxHealth());
	}

	public UUID owner() { return owner; }

	private ServerPlayer bearer() {
		return owner != null && level() instanceof ServerLevel level && level.getPlayerByUUID(owner) instanceof ServerPlayer p ? p : null;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
		goalSelector.addGoal(4, new Follow());
		targetSelector.addGoal(1, new Guard());
	}

	/** Never against its bearer, nor anyone fighting on their side (another of the army, their sect's fellows). */
	public boolean fightsFor(LivingEntity e) {
		ServerPlayer p = bearer();
		if (p == null) return false;
		if (e == p || e instanceof ArmySpirit spirit && p.getUUID().equals(spirit.owner)) return true;
		return e instanceof CultivatorNpc npc && SectMembership.areFellows(npc, p);
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return !fightsFor(target) && super.canAttack(target);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) return;
		ServerPlayer p = bearer();
		if (--left <= 0 || p == null || p.level() != level || !p.isAlive()) {
			level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1, getZ(), 12, 0.3, 0.6, 0.3, 0.02);
			discard();
			return;
		}
		if (tickCount % 10 == 0) level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.2, getZ(), 1, 0.2, 0.05, 0.2, 0.01);
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		triggerAnim("main", "strike");
		return super.doHurtTarget(target);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (source.getEntity() instanceof LivingEntity by && fightsFor(by)) return false; // its own side's blows pass through it
		return super.hurt(source, amount);
	}

	@Override
	public boolean shouldBeSaved() { return false; }

	@Override
	public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

	@Override
	protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.VEX_HURT; }

	@Override
	protected SoundEvent getDeathSound() { return SoundEvents.SOUL_ESCAPE; }

	/** Keeps near its bearer when it has nobody to fight: walks after them past six blocks, appears beside them past twenty-four. */
	private final class Follow extends Goal {
		Follow() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			ServerPlayer p = bearer();
			return getTarget() == null && p != null && distanceToSqr(p) > 6 * 6;
		}

		@Override
		public boolean requiresUpdateEveryTick() { return true; }

		@Override
		public void tick() {
			ServerPlayer p = bearer();
			if (p == null) return;
			if (distanceToSqr(p) > 24 * 24) teleportTo(p.getX() + random.nextInt(5) - 2, p.getY(), p.getZ() + random.nextInt(5) - 2);
			else if (getNavigation().isDone() || tickCount % 10 == 0) getNavigation().moveTo(p, 1.2);
		}
	}

	/** Its foe: whoever its bearer struck last, or whoever struck them (asked every other tick, as a goal's choosing is). */
	private final class Guard extends TargetGoal {
		Guard() {
			super(ArmySpirit.this, false);
		}

		@Override
		public boolean canUse() {
			ServerPlayer p = bearer();
			if (p == null) return false;
			LivingEntity foe = p.getLastHurtMob();
			if (foe == null || !foe.isAlive() || p.tickCount - p.getLastHurtMobTimestamp() > 200 || fightsFor(foe)) foe = p.getLastHurtByMob();
			if (foe == null || !foe.isAlive() || fightsFor(foe) || foe.distanceToSqr(ArmySpirit.this) > 24 * 24) return false;
			if (foe instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
			targetMob = foe;
			return true;
		}

		@Override
		public void start() {
			mob.setTarget(targetMob);
			super.start();
		}
	}

	// --- Its animations (the Battle Spirit's, made with its gold model: animations/realm/army_spirit) ---

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle"), WALK = RawAnimation.begin().thenLoop("walk");

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		AnimationController<ArmySpirit> main = new AnimationController<>(this, "main", 6, state -> state.setAndContinue(state.isMoving() ? WALK : IDLE));
		main.triggerableAnim("strike", RawAnimation.begin().thenPlay("strike"));
		controllers.add(main);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
