package com.example.defyingtheheavens;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * What a demon beast does (DemonBeast): wander as its kind does (on foot, in the water, in the air), go for whoever comes near if
 * its kind is territorial (and fight back if not), and strike with its kind's signature move, each in its own time:
 * <ul>
 *   <li>CHARGE (the boar), SPARK_CHARGE (the antelope: a spark that slows), STUN_CHARGE (the rhino: it stuns): a rush in a straight
 *       line, knocking back what it meets.</li>
 *   <li>PACK (the wolves): a pack fights as one; at night they hunt a quarter the harder (DemonBeast).</li>
 *   <li>VENOM (the viper): its bite poisons. DRAG (the crocodile): its bite holds, and in the water pulls under.</li>
 *   <li>SHELL (the turtle): hurt, it draws into its shell a while, little getting through.</li>
 *   <li>SWOOP (the hawk), DIVE (the roc, with a gust): it circles high, dives on its prey, climbs away.</li>
 *   <li>THROW (the monkey): burning stones. WATER_JET (the flood dragon): a jet of water that knocks back.</li>
 *   <li>POUNCE (the tiger): a leap onto its prey. ICE_SLAM (the ape: frost that slows), POUND (the bear: the ground heaves).</li>
 *   <li>BREATH (the ember fox): fire breathed in a cone. COIL (the python): coiled round its prey, holding it and crushing.</li>
 * </ul>
 * An elder (BeastKind#isElder) strikes with its kind's move as the wild do, its reach grown with its size ({@link #grown}: a pounce, a
 * slam, its breath, a dive's circling and gust), and goes back to the pool it guards (DemonBeast#guard) when it has none to fight.
 */
final class DemonBeastGoals {
	private DemonBeastGoals() {}

	static void register(DemonBeast beast, GoalSelector goals, GoalSelector targets) {
		BeastKind kind = beast.kind();
		if (kind.moves == BeastKind.Moves.GROUND) goals.addGoal(0, new FloatGoal(beast));
		Goal signature = signature(beast, kind.signature);
		if (signature != null) goals.addGoal(1, signature);
		if (kind.moves != BeastKind.Moves.FLYING) goals.addGoal(2, new MeleeAttackGoal(beast, 1.2, false));
		if (kind.isElder()) goals.addGoal(4, new MoveTowardsRestrictionGoal(beast, 1.0));
		switch (kind.moves) {
			case FLYING -> goals.addGoal(5, new WaterAvoidingRandomFlyingGoal(beast, 1.0));
			case SWIMMING -> goals.addGoal(5, new RandomSwimmingGoal(beast, 1.0, 10));
			case AMPHIBIOUS -> goals.addGoal(5, new RandomStrollGoal(beast, 0.8));
			default -> goals.addGoal(5, new WaterAvoidingRandomStrollGoal(beast, 0.8));
		}
		goals.addGoal(6, new LookAtPlayerGoal(beast, Player.class, 8.0f));
		goals.addGoal(7, new RandomLookAroundGoal(beast));
		targets.addGoal(1, new HurtByTargetGoal(beast));
		targets.addGoal(2, new NearestAttackableTargetGoal<>(beast, Player.class, 10, true, false, t -> near(beast, t) && beast.hunts(t)));
		targets.addGoal(3, new NearestAttackableTargetGoal<>(beast, CultivatorNpc.class, 10, true, false, t -> near(beast, t) && beast.hunts(t)));
	}

	private static boolean near(DemonBeast beast, LivingEntity target) {
		double reach = DemonBeast.NOTICE + beast.getBbWidth();
		return beast.distanceToSqr(target) <= reach * reach;
	}

	private static Goal signature(DemonBeast beast, BeastKind.Signature signature) {
		return switch (signature) {
			case CHARGE, SPARK_CHARGE, STUN_CHARGE -> new ChargeGoal(beast, signature);
			case POUNCE -> new PounceGoal(beast);
			case ICE_SLAM, POUND -> new SlamGoal(beast, signature);
			case BREATH -> new BreathGoal(beast);
			case COIL -> new CoilGoal(beast);
			case SWOOP, DIVE -> new SwoopGoal(beast, signature == BeastKind.Signature.DIVE);
			case THROW -> new ShootGoal(beast, BeastShot.Kind.FIRE_STONE, 3, 16, 50);
			case WATER_JET -> new ShootGoal(beast, BeastShot.Kind.WATER, 3, 20, 45);
			case SHELL -> new ShellGoal(beast);
			default -> null; // PACK, VENOM, DRAG: in its blows and its hurts (onBite, onHurt)
		};
	}

	// --- In its blows and its hurts ---

	/** Its bite landed on {@code target}: a viper's venom; a crocodile's hold, pulling under in the water. */
	static void onBite(DemonBeast beast, LivingEntity target) {
		long now = beast.level().getGameTime();
		switch (beast.kind().signature) {
			case VENOM -> DemonBeast.afflict(target, MobEffects.POISON, 100, Math.min(2, beast.getRank() / 2), beast);
			case DRAG -> {
				if (now < beast.signatureReady || beast.getRandom().nextFloat() > 0.4f) return;
				beast.signatureReady = now + 100;
				DemonBeast.afflict(target, MobEffects.MOVEMENT_SLOWDOWN, 50, 3, beast);
				if (target.isInWater() || beast.isInWater()) {
					Vec3 toward = beast.position().subtract(target.position()).normalize().scale(0.3);
					target.setDeltaMovement(toward.x, -0.45, toward.z);
					target.hurtMarked = true;
				}
				sound(beast, SoundEvents.GENERIC_SPLASH, 1.0f, 0.6f);
			}
			default -> {
			}
		}
	}

	/**
	 * It was hurt: a wolf's pack turns on whoever did it, each that may (Mob#canAttack: not a creative player, say, whom the pack set on
	 * and circled for nothing before the sweep of 2026-10-23); a turtle draws into its shell (ShellGoal).
	 */
	static void onHurt(DemonBeast beast, DamageSource source) {
		if (!(source.getEntity() instanceof LivingEntity attacker) || attacker instanceof DemonBeast) return;
		if (beast.kind().signature == BeastKind.Signature.PACK) {
			for (DemonBeast fellow : beast.level().getEntitiesOfClass(DemonBeast.class, beast.getBoundingBox().inflate(16),
					b -> b != beast && b.getType() == beast.getType() && b.getTarget() == null && b.canAttack(attacker))) {
				fellow.setTarget(attacker);
			}
		}
	}

	/**
	 * Whether an area move of {@code beast}'s (a slam, its breath, a gust) reaches {@code other}: not another beast, nor what nothing of the
	 * living world touches (a soul, the dead: Incorporeal#untouchable; a spectator or an invulnerable player), and only what it can see:
	 * before the sweep of 2026-10-23 they reached through walls, and a gust threw souls and the dead about.
	 */
	private static boolean reaches(DemonBeast beast, LivingEntity other) {
		if (other == beast || other instanceof DemonBeast || !other.isAlive()) return false;
		if (other instanceof Player player && (player.isSpectator() || player.getAbilities().invulnerable || Incorporeal.untouchable(player))) return false;
		return beast.hasLineOfSight(other);
	}

	/** What of a push is left on {@code other} past its knockback resistance (a netherite-clad player, a heavy mob: little or none). */
	private static double pushShare(LivingEntity other) {
		return Math.max(0, 1 - other.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
	}

	private static void sound(DemonBeast beast, SoundEvent sound, float volume, float pitch) {
		beast.level().playSound(null, beast.blockPosition(), sound, SoundSource.HOSTILE, volume, pitch);
	}

	private static float attack(DemonBeast beast) {
		var attack = beast.getAttribute(Attributes.ATTACK_DAMAGE);
		return attack == null ? 3 : (float) attack.getValue();
	}

	/** How much further an elder's moves reach than its wild kin's: by its size (DemonBeast#scale, BeastKind#modelScale); a wild beast's, as they were. */
	static double grown(DemonBeast beast) {
		return beast.kind().isElder() ? beast.scale() * beast.kind().modelScale() : 1;
	}

	private static boolean ready(DemonBeast beast, int chance) {
		return beast.level().getGameTime() >= beast.signatureReady && beast.getRandom().nextInt(chance) == 0;
	}

	private static void rest(DemonBeast beast, int ticks) {
		beast.signatureReady = beast.level().getGameTime() + ticks;
	}

	/** Its target, alive and in reach of {@code min} to {@code max} blocks (edge to edge, roughly), seen. */
	private static LivingEntity inReach(DemonBeast beast, double min, double max) {
		LivingEntity target = beast.getTarget();
		if (target == null || !target.isAlive()) return null;
		double d = Math.sqrt(beast.distanceToSqr(target)) - beast.getBbWidth() / 2;
		return d >= min && d <= max && beast.getSensing().hasLineOfSight(target) ? target : null;
	}

	// --- The charge ---

	static final class ChargeGoal extends Goal {
		private final DemonBeast beast;
		private final BeastKind.Signature how;
		private LivingEntity target;
		private Vec3 way;
		private int ticks;

		ChargeGoal(DemonBeast beast, BeastKind.Signature how) {
			this.beast = beast;
			this.how = how;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			if (!beast.onGround() || !ready(beast, 8)) return false;
			target = inReach(beast, 3.5, 14);
			return target != null;
		}

		@Override
		public boolean canContinueToUse() {
			return ticks < 45 && target.isAlive() && way != null;
		}

		@Override
		public void start() {
			ticks = 0;
			Vec3 to = target.position().subtract(beast.position());
			way = new Vec3(to.x, 0, to.z).normalize();
			beast.getNavigation().stop();
			beast.setActing(DemonBeast.ACT_CHARGE, true);
			sound(beast, how == BeastKind.Signature.STUN_CHARGE ? SoundEvents.RAVAGER_ROAR : how == BeastKind.Signature.CHARGE
					? SoundEvents.HOGLIN_ANGRY : SoundEvents.GOAT_SCREAMING_PREPARE_RAM, 1.2f, 0.9f);
		}

		@Override
		public void tick() {
			ticks++;
			if (ticks < 6) { // the run-up
				beast.getLookControl().setLookAt(target, 30, 30);
				return;
			}
			double speed = 0.55 + beast.getRank() * 0.03;
			beast.setDeltaMovement(way.x * speed, beast.getDeltaMovement().y, way.z * speed);
			beast.setYRot((float) (Math.toDegrees(Math.atan2(-way.x, way.z))));
			beast.yBodyRot = beast.getYRot();
			if (beast.level() instanceof ServerLevel level && how == BeastKind.Signature.SPARK_CHARGE && ticks % 2 == 0) {
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, beast.getX(), beast.getY() + beast.getBbHeight() * 0.6, beast.getZ(), 3, 0.3, 0.3, 0.3, 0.05);
			}
			if (beast.getBoundingBox().inflate(0.4).intersects(target.getBoundingBox())) {
				if (target.hurt(beast.damageSources().mobAttack(beast), attack(beast) * 1.6f)) {
					target.knockback(1.6, -way.x, -way.z);
					target.setDeltaMovement(target.getDeltaMovement().add(0, 0.3, 0));
					target.hurtMarked = true;
					if (how == BeastKind.Signature.SPARK_CHARGE) DemonBeast.afflict(target, MobEffects.MOVEMENT_SLOWDOWN, 40, 1, beast);
					if (how == BeastKind.Signature.STUN_CHARGE) {
						DemonBeast.afflict(target, MobEffects.MOVEMENT_SLOWDOWN, 40, 5, beast);
						DemonBeast.afflict(target, MobEffects.WEAKNESS, 40, 2, beast);
						if (beast.level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getEyeY(), target.getZ(), 20, 0.4, 0.4, 0.4, 0.1);
					}
					sound(beast, SoundEvents.GOAT_RAM_IMPACT, 1.2f, 0.7f);
				}
				way = null;
			} else if (beast.horizontalCollision) {
				way = null;
			}
		}

		@Override
		public void stop() {
			beast.setActing(DemonBeast.ACT_CHARGE, false);
			rest(beast, how == BeastKind.Signature.STUN_CHARGE ? 110 : 80);
			way = null;
		}
	}

	// --- The pounce ---

	static final class PounceGoal extends Goal {
		private final DemonBeast beast;
		private LivingEntity target;
		private int ticks;

		PounceGoal(DemonBeast beast) {
			this.beast = beast;
			setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (!beast.onGround() || !ready(beast, 6)) return false;
			// From a few strides off, or springing on prey already at its jaws (a still target was never pounced on, 2026-10-23).
			target = inReach(beast, 1.0, 9 * grown(beast));
			return target != null;
		}

		@Override
		public boolean canContinueToUse() {
			return ticks < 30 && target.isAlive() && !(ticks > 3 && beast.onGround());
		}

		@Override
		public void start() {
			ticks = 0;
			Vec3 to = target.position().subtract(beast.position());
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			double reach = Math.min(1.4 * grown(beast), 0.35 + flat * 0.13);
			// Prey right under it (or over it) springs it the way it faces: across nothing it divided by nothing, and its motion was no
			// number at all, before the sweep of 2026-10-23.
			Vec3 way = flat < 1.0e-3 ? Vec3.directionFromRotation(0, beast.getYRot()) : new Vec3(to.x / flat, 0, to.z / flat);
			beast.setDeltaMovement(way.x * reach, 0.45 + flat * 0.02, way.z * reach);
			beast.getNavigation().stop();
			sound(beast, SoundEvents.RAVAGER_ROAR, 0.9f, 1.4f);
		}

		@Override
		public void tick() {
			ticks++;
			beast.getLookControl().setLookAt(target, 30, 30);
			if (beast.getBoundingBox().inflate(0.6).intersects(target.getBoundingBox())) {
				if (target.hurt(beast.damageSources().mobAttack(beast), attack(beast) * 1.7f)) {
					DemonBeast.afflict(target, MobEffects.MOVEMENT_SLOWDOWN, 30, 1, beast);
				}
				ticks = 30;
			}
		}

		@Override
		public void stop() {
			rest(beast, 90);
		}
	}

	// --- The slam ---

	static final class SlamGoal extends Goal {
		private final DemonBeast beast;
		private final boolean frost;
		private LivingEntity target;
		private int ticks;

		SlamGoal(DemonBeast beast, BeastKind.Signature how) {
			this.beast = beast;
			this.frost = how == BeastKind.Signature.ICE_SLAM;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (!ready(beast, 5)) return false;
			target = inReach(beast, 0, 2.5 * grown(beast));
			return target != null;
		}

		@Override
		public boolean canContinueToUse() {
			return ticks <= 14;
		}

		@Override
		public void start() {
			ticks = 0;
			beast.getNavigation().stop();
			beast.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
		}

		@Override
		public void tick() {
			ticks++;
			if (target != null) beast.getLookControl().setLookAt(target, 30, 30);
			if (ticks != 12 || !(beast.level() instanceof ServerLevel level)) return;
			double radius = (frost ? 3.5 : 4.5) * (beast.kind().isElder() ? grown(beast) : beast.scale());
			for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, beast.getBoundingBox().inflate(radius),
					e -> e.distanceToSqr(beast) <= radius * radius && reaches(beast, e))) {
				if (!other.hurt(beast.damageSources().mobAttack(beast), attack(beast) * 1.3f)) continue;
				if (frost) {
					DemonBeast.afflict(other, MobEffects.MOVEMENT_SLOWDOWN, 80, 2, beast);
					other.setTicksFrozen(Math.max(other.getTicksFrozen(), 160));
				} else {
					Vec3 away = other.position().subtract(beast.position());
					away = new Vec3(away.x, 0, away.z);
					away = away.lengthSqr() < 1.0e-6 ? Vec3.ZERO : away.normalize();
					double share = pushShare(other);
					other.setDeltaMovement(away.x * 0.8 * share, 0.7 * share, away.z * 0.8 * share);
					other.hurtMarked = true;
				}
			}
			if (frost) {
				level.sendParticles(ParticleTypes.SNOWFLAKE, beast.getX(), beast.getY() + 0.3, beast.getZ(), 60, radius * 0.4, 0.2, radius * 0.4, 0.05);
				sound(beast, SoundEvents.GLASS_BREAK, 1.0f, 0.6f);
			} else {
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), beast.getX(), beast.getY() + 0.2, beast.getZ(), 80,
						radius * 0.4, 0.2, radius * 0.4, 0.2);
				sound(beast, SoundEvents.GENERIC_EXPLODE, 0.8f, 0.6f);
			}
		}

		@Override
		public void stop() {
			rest(beast, frost ? 100 : 130);
		}
	}

	// --- The fire breath ---

	static final class BreathGoal extends Goal {
		private final DemonBeast beast;
		private LivingEntity target;
		private int ticks;

		BreathGoal(DemonBeast beast) {
			this.beast = beast;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (!ready(beast, 6)) return false;
			target = inReach(beast, 0, 6 * grown(beast));
			return target != null;
		}

		@Override
		public boolean canContinueToUse() {
			return ticks < 30 && target.isAlive();
		}

		@Override
		public void start() {
			ticks = 0;
			beast.getNavigation().stop();
			beast.setActing(DemonBeast.ACT_BREATH, true);
			sound(beast, SoundEvents.BLAZE_SHOOT, 1.0f, 0.7f);
		}

		@Override
		public void tick() {
			ticks++;
			beast.getLookControl().setLookAt(target, 30, 30);
			if (!(beast.level() instanceof ServerLevel level)) return;
			Vec3 mouth = beast.getEyePosition();
			Vec3 look = target.getEyePosition().subtract(mouth).normalize();
			double grown = grown(beast), length = 7 * grown;
			if (ticks % 2 == 0) {
				for (int i = 1; i <= (int) (6 * grown); i++) {
					Vec3 at = mouth.add(look.scale(i));
					level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 2, i * 0.08, i * 0.08, i * 0.08, 0.01);
				}
			}
			if (ticks % 6 != 0) return;
			for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, beast.getBoundingBox().inflate(length), e -> reaches(beast, e))) {
				Vec3 to = other.getEyePosition().subtract(mouth);
				if (to.length() > length || to.normalize().dot(look) < 0.8) continue;
				if (other.hurt(beast.damageSources().mobAttack(beast), attack(beast) * 0.6f)) other.setSecondsOnFire(3);
			}
		}

		@Override
		public void stop() {
			beast.setActing(DemonBeast.ACT_BREATH, false);
			rest(beast, 120);
		}
	}

	// --- The coil ---

	static final class CoilGoal extends Goal {
		private final DemonBeast beast;
		private LivingEntity target;
		private int ticks;

		CoilGoal(DemonBeast beast) {
			this.beast = beast;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			if (!ready(beast, 8)) return false;
			target = inReach(beast, 0, 2);
			return target != null;
		}

		/**
		 * While its prey is still its prey (its target, one it may attack: not gone creative or another world's), alive and near across
		 * the ground (within 6, and no more than 3 above or below: before the sweep of 2026-10-23 it held what it no longer hunted, a
		 * player gone into spectating, and from any height).
		 */
		@Override
		public boolean canContinueToUse() {
			if (ticks >= 60 || !target.isAlive() || target.level() != beast.level() || beast.getTarget() != target || !beast.canAttack(target)) return false;
			double dx = target.getX() - beast.getX(), dz = target.getZ() - beast.getZ();
			return dx * dx + dz * dz < 36 && Math.abs(target.getY() - beast.getY()) <= 3;
		}

		@Override
		public void start() {
			ticks = 0;
			beast.getNavigation().stop();
			beast.setActing(DemonBeast.ACT_COIL, true);
			sound(beast, SoundEvents.PHANTOM_BITE, 1.0f, 0.5f);
		}

		@Override
		public void tick() {
			ticks++;
			// Held fast before its jaws: slowed to nothing, put back if it slips; crushed now and then.
			Vec3 hold = beast.position().add(Vec3.directionFromRotation(0, beast.getYRot()).scale(beast.getBbWidth() * 0.7 + 0.5));
			DemonBeast.afflict(target, MobEffects.MOVEMENT_SLOWDOWN, 5, 6, beast);
			// Put back only where there is room for it, and not through a wall (it pulled its prey into stone, or out from behind it).
			if (target.position().distanceToSqr(hold) > 1.0 && beast.hasLineOfSight(target)
					&& beast.level().noCollision(target, target.getBoundingBox().move(hold.x - target.getX(), 0, hold.z - target.getZ()))) {
				if (target instanceof ServerPlayer player) player.teleportTo(hold.x, target.getY(), hold.z);
				else target.teleportTo(hold.x, target.getY(), hold.z);
			}
			if (ticks % 12 == 0) target.hurt(beast.damageSources().mobAttack(beast), attack(beast) * 0.7f);
		}

		@Override
		public void stop() {
			beast.setActing(DemonBeast.ACT_COIL, false);
			rest(beast, 200);
		}
	}

	// --- The swoop and the dive ---

	static final class SwoopGoal extends Goal {
		private final DemonBeast beast;
		private final boolean dive;
		private LivingEntity target;
		private int ticks, phase; // 0 circling, 1 diving, 2 climbing away
		private double angle;

		SwoopGoal(DemonBeast beast, boolean dive) {
			this.beast = beast;
			this.dive = dive;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			target = beast.getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public boolean canContinueToUse() {
			return target.isAlive() && beast.getTarget() == target;
		}

		@Override
		public void start() {
			phase = 0;
			ticks = 0;
			angle = beast.getRandom().nextDouble() * Math.PI * 2;
		}

		@Override
		public void tick() {
			ticks++;
			double high = (dive ? 10 : 7) * grown(beast), round = (dive ? 10 : 7) * grown(beast);
			switch (phase) {
				case 0 -> {
					angle += 0.06;
					beast.getMoveControl().setWantedPosition(target.getX() + Math.cos(angle) * round, target.getY() + high, target.getZ() + Math.sin(angle) * round, 1.0);
					if (ticks > 40 && beast.level().getGameTime() >= beast.signatureReady && beast.getSensing().hasLineOfSight(target)) {
						phase = 1;
						ticks = 0;
						sound(beast, dive ? SoundEvents.ENDER_DRAGON_FLAP : SoundEvents.PHANTOM_SWOOP, 1.2f, dive ? 0.6f : 1.2f);
					}
				}
				case 1 -> {
					beast.getMoveControl().setWantedPosition(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 2.4);
					beast.getLookControl().setLookAt(target, 30, 30);
					if (beast.getBoundingBox().inflate(0.4).intersects(target.getBoundingBox())) {
						boolean struck = target.hurt(beast.damageSources().mobAttack(beast), attack(beast) * (dive ? 1.8f : 1.4f));
						if (struck && dive && beast.level() instanceof ServerLevel level) gust(level); // the gust of a strike that landed
						phase = 2;
						ticks = 0;
						rest(beast, dive ? 80 : 50);
					} else if (ticks > 60) {
						phase = 2;
						ticks = 0;
					}
				}
				default -> {
					beast.getMoveControl().setWantedPosition(beast.getX() + Math.cos(angle) * 6, target.getY() + high + 2, beast.getZ() + Math.sin(angle) * 6, 1.4);
					if (ticks > 30) {
						phase = 0;
						ticks = 0;
					}
				}
			}
		}

		/** The roc's wings' gust as it strikes: everything near it reaches ({@link #reaches}) thrown back, as far as it can be ({@link #pushShare}). */
		private void gust(ServerLevel level) {
			for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, beast.getBoundingBox().inflate(4 * grown(beast)), e -> reaches(beast, e))) {
				Vec3 away = other.position().subtract(beast.position());
				away = away.lengthSqr() < 1.0e-6 ? Vec3.ZERO : away.normalize();
				double share = pushShare(other);
				other.setDeltaMovement(away.x * 1.2 * share, 0.5 * share, away.z * 1.2 * share);
				other.hurtMarked = true;
			}
			level.sendParticles(ParticleTypes.CLOUD, beast.getX(), beast.getY(), beast.getZ(), 40, 2, 0.5, 2, 0.1);
		}
	}

	// --- Shooting ---

	static final class ShootGoal extends Goal {
		private final DemonBeast beast;
		private final BeastShot.Kind shot;
		private final double min, max;
		private final int rest;

		ShootGoal(DemonBeast beast, BeastShot.Kind shot, double min, double max, int rest) {
			this.beast = beast;
			this.shot = shot;
			this.min = min;
			this.max = max;
			this.rest = rest;
			setFlags(EnumSet.of(Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (!ready(beast, 10)) return false;
			return inReach(beast, min, max) != null;
		}

		@Override
		public boolean canContinueToUse() {
			return false;
		}

		@Override
		public void start() {
			LivingEntity target = beast.getTarget();
			if (target == null || !(beast.level() instanceof ServerLevel level)) return;
			beast.getLookControl().setLookAt(target, 30, 30);
			beast.swing(net.minecraft.world.InteractionHand.MAIN_HAND); // a monkey's throwing arm, a dragon's jaws (BeastModels)
			BeastShot projectile = new BeastShot(level, beast, shot, attack(beast) * (shot == BeastShot.Kind.WATER ? 1.0f : 0.8f));
			Vec3 from = beast.getEyePosition();
			projectile.setPos(from.x, from.y - 0.1, from.z);
			double dx = target.getX() - from.x, dz = target.getZ() - from.z;
			double dy = target.getY(0.5) - projectile.getY();
			double flat = Math.sqrt(dx * dx + dz * dz);
			projectile.shoot(dx, dy + (shot == BeastShot.Kind.FIRE_STONE ? flat * 0.2 : 0), dz, shot == BeastShot.Kind.WATER ? 1.6f : 1.1f, 4.0f);
			level.addFreshEntity(projectile);
			sound(beast, shot == BeastShot.Kind.WATER ? SoundEvents.DROWNED_SHOOT : SoundEvents.SNOWBALL_THROW, 1.0f, 0.8f);
			rest(beast, rest);
		}
	}

	// --- The shell ---

	static final class ShellGoal extends Goal {
		private final DemonBeast beast;
		private int ticks;

		ShellGoal(DemonBeast beast) {
			this.beast = beast;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return beast.getLastHurtByMob() != null && beast.tickCount - beast.getLastHurtByMobTimestamp() < 20 && beast.getHealth() < beast.getMaxHealth() * 0.6f
					&& beast.level().getGameTime() >= beast.signatureReady;
		}

		@Override
		public boolean canContinueToUse() {
			return ticks < 100;
		}

		@Override
		public void start() {
			ticks = 0;
			beast.getNavigation().stop();
			beast.setActing(DemonBeast.ACT_SHELL, true);
			sound(beast, SoundEvents.ARMOR_EQUIP_TURTLE, 1.0f, 0.6f);
		}

		@Override
		public void tick() {
			ticks++;
			beast.setDeltaMovement(0, beast.getDeltaMovement().y, 0);
		}

		@Override
		public void stop() {
			beast.setActing(DemonBeast.ACT_SHELL, false);
			rest(beast, 300);
		}
	}

	/** Whatever it is within {@code r} of (for the tests). */
	static AABB around(DemonBeast beast, double r) {
		return beast.getBoundingBox().inflate(r);
	}
}
