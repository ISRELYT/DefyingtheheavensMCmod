package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The water methods' twelve spells (see HeavenSpells for what they share): a needle of hoarfrost whose stacks freeze, a mist that
 * hides its caster, a spring to come back to, a rain that mends and hastens, a whip that hurls a being over the caster's head, a
 * whirlpool, a swap with the being looked at, a still lake that halves blows and mirrors them back, a globe of abyssal water that
 * drowns, the Heavenly River overturned, a mirror lake that turns foes on one another, and the winter of the northern abyss.
 */
public final class WaterSpells {
	static final int WATER = 0x3C8CFF, DEEP = 0x1E50FF, PALE = 0x9FD8FF, FROST = 0xCFEFFF, ICE = 0xBFE8FF, MIST = 0xE6EEF5,
			ABYSS = 0x14286E, FOAM = 0xF0FAFF, SILVER = 0xE8F4FF, RAIN_CLOUD = 0xA0B4C8;

	/** Hoarfrost Needle: reach, speed (blocks a tick), harm, the stacks' life and how many freeze, the freeze, the grace after a thaw. */
	static final double NEEDLE_RANGE = 24, NEEDLE_SPEED = 2;
	static final float NEEDLE_HARM = 6;
	static final int FROST_TICKS = 200, FROST_STACKS = 3, NEEDLE_FREEZE_TICKS = 40, THAW_GRACE_TICKS = 120;
	/** Morning Mist Shroud: size, how near a creature must be to keep track of one inside, how far creatures are searched, how long. */
	static final double MIST_RADIUS = 5, MIST_KEEPS = 3, MIST_REACH = 24;
	static final int MIST_TICKS = 120, MIST_VEIL_TICKS = 25;
	/** Returning Current: how long the spring waits, the share of the lost health given back, the safe fall after. */
	static final int SPRING_TICKS = 100, SPRING_SAFE_TICKS = 40;
	static final float SPRING_SHARE = 0.5f;
	/** Renewing Mountain Rain: size, how long, the mending each second. */
	static final double RAIN_RADIUS = 6;
	static final int RAIN_TICKS = 200;
	static final float RAIN_MEND = 1;
	/** Coiling Stream Lash: reach, the landing behind the caster, the heaviest it throws, the throw's ticks, harm, the slow. */
	static final double LASH_RANGE = 16, LASH_BEHIND = 6, LASH_MAX_HEALTH = 200;
	static final int LASH_TICKS = 14, LASH_SLOW_TICKS = 40;
	static final float LASH_HARM = 22;
	/** Nine-Bend Whirlpool: reach, size, how long, harm each second and at the collapse. */
	static final double POOL_RANGE = 24, POOL_RADIUS = 5;
	static final int POOL_TICKS = 100;
	static final float POOL_HARM = 2, POOL_COLLAPSE = 6;
	/** Moon in the Water: reach, the reeling, the dizziness (a player's), the safe fall. */
	static final double MOON_RANGE = 32;
	static final int MOON_SLOW_TICKS = 40, MOON_DIZZY_TICKS = 60, MOON_SAFE_TICKS = 40;
	/** Still Lake Mirror: how long, the share of each blow it lets through, the reflection's reach and most. */
	static final int MIRROR_TICKS = 160;
	static final float MIRROR_KEEPS = 0.5f, MIRROR_MAX = 50;
	static final double MIRROR_REACH = 32;
	/** Abyssal Water Prison: reach, harm, how long it drowns, the drowning each second. */
	static final double PRISON_RANGE = 32;
	static final float PRISON_HARM = 50, PRISON_DROWN = 8;
	static final int PRISON_TICKS = 100;
	/** Overturned Heavenly River: width (each side), height, length, speed; harm caught and at the crash, the crash's reach, the slow. */
	static final double RIVER_HALF_WIDTH = 7.5, RIVER_HEIGHT = 5, RIVER_LENGTH = 32, RIVER_SPEED = 0.8, RIVER_CRASH_REACH = 6;
	static final float RIVER_HARM = 8, RIVER_CRASH = 32;
	static final int RIVER_SLOW_TICKS = 80;
	/** Ten Thousand Reflections: reach, how long, how far a turned creature looks for another, the bonus on one another, the dizziness. */
	static final double REFLECT_RADIUS = 20, REFLECT_SEEK = 16;
	static final int REFLECT_TICKS = 120, REFLECT_DIZZY_TICKS = 80;
	static final float REFLECT_BONUS = 1.5f;
	/** Eternal Winter Descends: reach, the fall, the freeze (players' and great beasts'), the missiles held, the shatter, the most held. */
	static final double WINTER_RADIUS = 24;
	static final int WINTER_FALL_TICKS = 20, WINTER_FREEZE_TICKS = 120, WINTER_FREEZE_SHORT = 60, WINTER_MISSILE_TICKS = 140;
	static final float WINTER_SHATTER = 60, WINTER_HELD_SHARE = 1.5f, WINTER_HELD_MAX = 140, WINTER_PLAYER_CAP = 0.5f;

	/** Whether {@code spell} can take hold for {@code p} now; refused, nothing is paid. */
	static boolean canCast(ServerPlayer p, Spell spell) {
		switch (spell) {
			case RETURNING_CURRENT -> {
				// Cast again while the spring waits: carried back now, and nothing is paid.
				if (!HeavenSpells.lingering(p.getUUID(), Spring.class).isEmpty()) {
					HeavenSpells.endLingering(p.getUUID(), Spring.class);
					return false;
				}
				return true;
			}
			case COILING_STREAM_LASH -> {
				return HeavenSpells.aimed(p, LASH_RANGE) != null || HeavenSpells.refuse(p);
			}
			case NINE_BEND_WHIRLPOOL -> {
				return poolCentre(p) != null || HeavenSpells.refuse(p);
			}
			case MOON_IN_THE_WATER -> {
				LivingEntity other = moonTarget(p);
				if (other != null && !HeavenSpells.greatBeast(other) && swapRoom(p, other) != null) return true;
				p.displayClientMessage(Component.translatable(ModLang.MSG_NO_REFLECTION), true);
				return false;
			}
			case ABYSSAL_WATER_PRISON -> {
				return HeavenSpells.aimed(p, PRISON_RANGE) != null || HeavenSpells.refuse(p);
			}
			default -> {
				return true;
			}
		}
	}

	/** Casts one of the water spells (paid for). */
	static void cast(ServerPlayer p, Spell spell) {
		switch (spell) {
			case HOARFROST_NEEDLE -> needle(p);
			case MORNING_MIST_SHROUD -> mist(p);
			case RETURNING_CURRENT -> spring(p, spell);
			case RENEWING_MOUNTAIN_RAIN -> rain(p);
			case COILING_STREAM_LASH -> lash(p);
			case NINE_BEND_WHIRLPOOL -> whirlpool(p);
			case MOON_IN_THE_WATER -> moon(p);
			case STILL_LAKE_MIRROR -> mirror(p);
			case ABYSSAL_WATER_PRISON -> prison(p);
			case OVERTURNED_HEAVENLY_RIVER -> river(p);
			case TEN_THOUSAND_REFLECTIONS -> reflections(p);
			case ETERNAL_WINTER_DESCENDS -> winter(p);
			default -> { }
		}
	}

	/** Every server tick: the frost marks that have melted are let go now and then. */
	static void tick(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		if (now % 200 == 0 && !FROSTBITTEN.isEmpty()) FROSTBITTEN.values().removeIf(f -> now > f.until && now > f.graceUntil);
	}

	/** A player left. */
	static void forget(UUID id) {
	}

	/** The server stopping. */
	static void clear() {
		FROSTBITTEN.clear();
	}

	// --- Shared ---

	/** A creature made to lose its quarry: no target, no remembered foe, its anger gone. */
	private static void calm(Mob mob) {
		mob.setTarget(null);
		mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
		if (mob instanceof NeutralMob neutral) neutral.stopBeingAngry();
	}

	/** Frozen still where it was: its motion gone (a flier hangs), drawn back if it drifts, held from striking, frosted over. */
	private static void holdFrozen(LivingEntity e, Vec3 anchor, boolean fully) {
		e.setDeltaMovement(fully ? Vec3.ZERO : new Vec3(0, Math.min(e.getDeltaMovement().y, 0), 0));
		e.hurtMarked = true;
		if (anchor != null) {
			double drift = e.position().distanceTo(anchor);
			if (e instanceof Player ? drift > 1 : drift > 0.2) HeavenSpells.teleport(e, anchor);
		}
		if (e instanceof Mob mob) {
			mob.getNavigation().stop();
			if (fully) calm(mob);
		}
		if (e instanceof Creeper creeper) creeper.setSwellDir(-1);
		e.setTicksFrozen(Math.max(e.getTicksFrozen(), e.getTicksRequiredToFreeze() - 1)); // the frost's look, short of its harm
		e.resetFallDistance();
	}

	/** Whether {@code e} can't be frozen solid: the cold's own creatures and the great beasts. */
	private static boolean frostProof(LivingEntity e) {
		return HeavenSpells.greatBeast(e) || e.getType().is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES);
	}

	/** {@code particle} over a ball's surface round {@code centre}, {@code count} of them. */
	private static void globe(ServerLevel level, Vec3 centre, double radius, ParticleOptions particle, int count, double turn) {
		double golden = Math.PI * (3 - Math.sqrt(5));
		for (int i = 0; i < count; i++) {
			double y = 1 - 2 * (i + 0.5) / count, r = Math.sqrt(1 - y * y), a = golden * i + turn;
			level.sendParticles(particle, centre.x + Math.cos(a) * r * radius, centre.y + y * radius, centre.z + Math.sin(a) * r * radius, 1, 0, 0, 0, 0);
		}
	}

	private static Vec3 randomFlat(double radius) {
		java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
		double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * radius;
		return new Vec3(Math.cos(a) * d, 0, Math.sin(a) * d);
	}

	/** Whether {@code e} stands within a flat round zone of {@code radius} at {@code centre}, from {@code below} under it to {@code above} over it. */
	private static boolean within(LivingEntity e, Vec3 centre, double radius, double below, double above) {
		double dx = e.getX() - centre.x, dz = e.getZ() - centre.z;
		return dx * dx + dz * dz <= radius * radius && e.getY() >= centre.y - below && e.getY() <= centre.y + above;
	}

	// --- Hoarfrost Needle ---

	/** Hoarfrost Needle: a needle of hoarfrost flicked along the look; the first being it strikes takes a stack of frost. */
	private static void needle(ServerPlayer p) {
		HeavenSpells.linger(new Needle(p, HeavenSpells.fromHands(p), p.getViewVector(1.0f)));
		HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.SNOW_GOLEM_SHOOT, 1.0f, 1.6f);
	}

	/** The needle in flight, two blocks a tick, until it strikes a being or a wall or has flown its reach. */
	static final class Needle extends HeavenSpells.Timed {
		final Vec3 step;
		Vec3 at;
		double flown;

		Needle(ServerPlayer caster, Vec3 from, Vec3 look) {
			super(caster, (int) Math.ceil(NEEDLE_RANGE / NEEDLE_SPEED) + 1);
			this.at = from;
			this.step = look.normalize().scale(NEEDLE_SPEED);
		}

		@Override
		protected boolean run(long age, long now) {
			if (flown >= NEEDLE_RANGE || caster.level() != level) return false;
			Vec3 next = at.add(step);
			BlockHitResult wall = level.clip(new ClipContext(at, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
			Vec3 stop = wall.getType() == HitResult.Type.MISS ? next : wall.getLocation();
			EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, caster, at, stop, new AABB(at, stop).inflate(1.0),
					e -> e instanceof LivingEntity living && !HeavenSpells.spared(caster, living) && !(e instanceof ArmorStand));
			Vec3 end = hit != null ? hit.getLocation() : stop;
			for (double d = 0; d < at.distanceTo(end); d += 0.25) {
				Vec3 point = at.add(end.subtract(at).normalize().scale(d));
				level.sendParticles(ParticleTypes.SNOWFLAKE, point.x, point.y, point.z, 1, 0, 0, 0, 0);
				level.sendParticles(HeavenSpells.dust(FROST, 0.6f), point.x, point.y, point.z, 1, 0, 0, 0, 0);
			}
			if (hit != null && hit.getEntity() instanceof LivingEntity target) {
				HeavenSpells.strike(caster, target, NEEDLE_HARM, QiElement.WATER);
				target.clearFire();
				HeavenSpells.particles(level, ParticleTypes.ITEM_SNOWBALL, HeavenSpells.middle(target), 8, 0.2, 0.05);
				HeavenSpells.sound(level, target.position(), SoundEvents.POWDER_SNOW_BREAK, 1.0f, 1.0f);
				if (target.isAlive()) frostbite(caster, target);
				return false;
			}
			if (wall.getType() != HitResult.Type.MISS) {
				HeavenSpells.particles(level, ParticleTypes.SNOWFLAKE, stop, 6, 0.1, 0.02);
				return false;
			}
			at = next;
			flown += NEEDLE_SPEED;
			return true;
		}
	}

	/** Each being's frost: how many stacks, until when they last, and until when it can't be frozen again (after a thaw). */
	private static final class Frost {
		int count;
		long until;
		long graceUntil;
	}

	private static final Map<LivingEntity, Frost> FROSTBITTEN = new java.util.WeakHashMap<>();

	/** How many stacks of frost {@code e} bears now (tests). */
	static int frost(LivingEntity e) {
		Frost f = FROSTBITTEN.get(e);
		return f == null || e.level().getGameTime() > f.until ? 0 : f.count;
	}

	/** A stack of frost on {@code target}: slower with each, and at three (if it can be) frozen solid. */
	private static void frostbite(ServerPlayer p, LivingEntity target) {
		ServerLevel level = p.serverLevel();
		long now = level.getGameTime();
		Frost f = FROSTBITTEN.computeIfAbsent(target, e -> new Frost());
		if (now > f.until) f.count = 0;
		boolean canFreeze = !frostProof(target) && now >= f.graceUntil;
		f.count = Math.min(canFreeze ? FROST_STACKS : FROST_STACKS - 1, f.count + 1);
		f.until = now + FROST_TICKS;
		HeavenSpells.ring(level, target.position().add(0, 0.1, 0), 0.6, HeavenSpells.dust(PALE, 0.8f), 8);
		if (f.count < FROST_STACKS) {
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FROST_TICKS, f.count - 1));
			return;
		}
		f.count = 0;
		f.graceUntil = now + NEEDLE_FREEZE_TICKS + THAW_GRACE_TICKS;
		target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
		HeavenSpells.linger(new Frozen(p, target));
	}

	/** Frozen solid by the needle's third stack: it can fall, but not walk or jump, for two seconds. */
	static final class Frozen extends HeavenSpells.Timed {
		final LivingEntity target;

		Frozen(ServerPlayer caster, LivingEntity target) {
			super(caster, NEEDLE_FREEZE_TICKS);
			this.target = target;
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, NEEDLE_FREEZE_TICKS, 6));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, NEEDLE_FREEZE_TICKS, 0));
			HeavenSpells.particles(level, HeavenSpells.block(Blocks.ICE), HeavenSpells.middle(target), 30, 0.3, 0.05);
			HeavenSpells.particles(level, ParticleTypes.SNOWFLAKE, HeavenSpells.middle(target), 20, 0.4, 0.02);
			HeavenSpells.sound(level, target.position(), SoundEvents.PLAYER_HURT_FREEZE, 1.0f, 1.0f);
			HeavenSpells.sound(level, target.position(), SoundEvents.GLASS_PLACE, 1.0f, 1.4f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(target, level)) return false;
			holdFrozen(target, null, false);
			if (age % 5 == 0) HeavenSpells.particles(level, HeavenSpells.dust(ICE, 1.0f), HeavenSpells.middle(target), 3, 0.3, 0);
			return true;
		}

		@Override
		public void end() {
			if (!target.isAlive()) return;
			HeavenSpells.particles(level, HeavenSpells.block(Blocks.ICE), HeavenSpells.middle(target), 10, 0.3, 0);
			HeavenSpells.sound(level, target.position(), SoundEvents.GLASS_BREAK, 0.4f, 1.8f);
		}
	}

	// --- Morning Mist Shroud ---

	/** Morning Mist Shroud: a bank of mist round the caster, in which creatures lose track of those inside and the caster goes unseen. */
	private static void mist(ServerPlayer p) {
		HeavenSpells.endLingering(p.getUUID(), Mist.class);
		HeavenSpells.linger(new Mist(p));
		ServerLevel level = p.serverLevel();
		HeavenSpells.particles(level, ParticleTypes.CLOUD, p.position().add(0, 1, 0), 40, 1.5, 0.05);
		HeavenSpells.sound(level, p.position(), SoundEvents.FIRE_EXTINGUISH, 0.6f, 0.5f);
		HeavenSpells.sound(level, p.position(), SoundEvents.WEATHER_RAIN, 0.3f, 1.6f);
	}

	/** The mist: where it lies, and whether the caster has given itself away (a blow, a cast, a loosed arrow). */
	static final class Mist extends HeavenSpells.Timed implements HeavenSpells.MeleeListener, HeavenSpells.CastListener {
		final Vec3 centre;
		boolean revealed;

		Mist(ServerPlayer caster) {
			super(caster, MIST_TICKS);
			this.centre = caster.position();
		}

		boolean inside(LivingEntity e) {
			return within(e, centre, MIST_RADIUS, 1, 4);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(centre, centre).inflate(MIST_REACH), m -> !(m instanceof Warden))) {
				LivingEntity quarry = mob.getTarget();
				if (quarry != null && inside(quarry) && mob.distanceTo(quarry) > MIST_KEEPS) calm(mob);
			}
			for (LivingEntity e : HeavenSpells.aroundPoint(level, centre, MIST_RADIUS + 1, e -> e instanceof Player && inside(e) && HeavenSpells.enemy(caster, e))) {
				e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false));
			}
			// A missile of its own just loosed gives it away.
			if (!revealed && !level.getEntitiesOfClass(Projectile.class, caster.getBoundingBox().inflate(4), m -> m.getOwner() == caster && m.tickCount <= 1).isEmpty()) reveal();
			if (inside(caster) && !revealed) {
				caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, MIST_VEIL_TICKS, 0, false, false));
				caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MIST_VEIL_TICKS, 0, false, false));
			}
			if (age % 2 == 0) {
				for (int i = 0; i < 12; i++) {
					Vec3 at = centre.add(randomFlat(MIST_RADIUS)).add(0, java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 4, 0);
					level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 0, 0, 1, 0, 0.01);
					level.sendParticles(HeavenSpells.dust(MIST, 2.5f), at.x, at.y, at.z, 1, 0, 0, 0, 0);
				}
				HeavenSpells.ring(level, centre.add(0, 0.3, 0), MIST_RADIUS, ParticleTypes.POOF, 6);
			}
			if (age % 30 == 0) HeavenSpells.sound(level, centre, SoundEvents.WEATHER_RAIN, 0.2f, 1.2f);
			return true;
		}

		/** Whether the mist hides its caster now (tests). */
		boolean hiding() {
			return !revealed && inside(caster);
		}

		private void reveal() {
			if (revealed) return;
			revealed = true;
			MobEffectInstance veil = caster.getEffect(MobEffects.INVISIBILITY);
			if (veil != null && veil.getDuration() <= MIST_VEIL_TICKS) caster.removeEffect(MobEffects.INVISIBILITY); // only the mist's own
			HeavenSpells.particles(level, HeavenSpells.dust(MIST, 1.5f), HeavenSpells.middle(caster), 15, 0.5, 0.05);
			HeavenSpells.sound(level, caster.position(), SoundEvents.PLAYER_SPLASH, 0.3f, 1.8f);
		}

		@Override
		public void onMelee(ServerPlayer attacker, LivingEntity target) {
			if (attacker == caster) reveal();
		}

		@Override
		public void onCast(ServerPlayer who, Spell spell) {
			if (who == caster) reveal();
		}

		@Override
		public void end() {
			MobEffectInstance veil = caster.getEffect(MobEffects.INVISIBILITY);
			if (!revealed && veil != null && veil.getDuration() <= MIST_VEIL_TICKS) caster.removeEffect(MobEffects.INVISIBILITY);
		}
	}

	// --- Returning Current ---

	/** Returning Current: a spring left where the caster stands; it carries the caster back to it (cast again, or in five seconds). */
	private static void spring(ServerPlayer p, Spell spell) {
		HeavenSpells.linger(new Spring(p, spell));
		// Its cooldown runs from the return, and a second cast returns (canCast): none until then.
		Spells.setCooldown(p, spell, 0);
		HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.BUCKET_FILL, 1.0f, 1.2f);
	}

	/** The spring: where, the caster's health then, and (when it ends) the current carrying the caster back to it. */
	static final class Spring extends HeavenSpells.Timed {
		final Spell spell;
		final Vec3 mark;
		final float markHealth;

		Spring(ServerPlayer caster, Spell spell) {
			super(caster, SPRING_TICKS);
			this.spell = spell;
			this.mark = caster.position();
			this.markHealth = caster.getHealth();
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			if (age % 2 == 0) {
				double turn = age * 0.5;
				for (int i = 0; i < 6; i++) {
					double h = i * 0.25, a = turn + i * 1.1;
					level.sendParticles(HeavenSpells.dust(i % 2 == 0 ? WATER : PALE, 1.0f), mark.x + Math.cos(a) * 0.4, mark.y + h, mark.z + Math.sin(a) * 0.4, 1, 0, 0, 0, 0);
				}
				level.sendParticles(ParticleTypes.DRIPPING_WATER, mark.x, mark.y + 1.5, mark.z, 2, 0.2, 0, 0.2, 0);
				HeavenSpells.ring(level, mark.add(0, 0.05, 0), 0.6, ParticleTypes.SPLASH, 6);
			}
			if (age % 4 == 0) level.sendParticles(ParticleTypes.FALLING_WATER, caster.getX(), caster.getY() + 1.4, caster.getZ(), 1, 0.25, 0, 0.25, 0);
			return true;
		}

		@Override
		public void end() {
			if (!caster.isAlive() || caster.isRemoved()) return; // dead or gone: the spring is lost with it
			Spells.setCooldown(caster, spell, spell.cooldownTicks());
			Spells.sync(caster);
			Vec3 from = caster.position();
			if (caster.level() != level || !level.noCollision(caster, caster.getBoundingBox().move(mark.subtract(from)))) {
				caster.displayClientMessage(Component.translatable(ModLang.MSG_SPRING_DRY), true);
				HeavenSpells.sound(caster.serverLevel(), from, SoundEvents.FIRE_EXTINGUISH, 0.6f, 1.5f);
				return;
			}
			HeavenSpells.particles(level, ParticleTypes.SPLASH, from.add(0, 1, 0), 30, 0.4, 0.1);
			HeavenSpells.particles(level, ParticleTypes.BUBBLE_POP, from.add(0, 1, 0), 15, 0.4, 0.05);
			HeavenSpells.line(level, from.add(0, 1, 0), mark.add(0, 1, 0), HeavenSpells.dust(WATER, 1.2f), 0.5);
			HeavenSpells.teleport(caster, mark);
			HeavenSpells.safeFall(caster, SPRING_SAFE_TICKS);
			caster.clearFire();
			HeavenSpells.mend(caster, Math.max(0, markHealth - caster.getHealth()) * SPRING_SHARE);
			HeavenSpells.particles(level, ParticleTypes.SPLASH, mark.add(0, 1, 0), 30, 0.4, 0.1);
			HeavenSpells.particles(level, ParticleTypes.BUBBLE_POP, mark.add(0, 1, 0), 15, 0.4, 0.05);
			HeavenSpells.sound(level, mark, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.0f, 1.0f);
			HeavenSpells.sound(level, mark, SoundEvents.BUCKET_EMPTY, 1.0f, 1.0f);
		}
	}

	// --- Renewing Mountain Rain ---

	/** Renewing Mountain Rain: a soft rain round the caster that mends and cools its friends and hastens their spells. */
	private static void rain(ServerPlayer p) {
		HeavenSpells.endLingering(p.getUUID(), Rain.class);
		HeavenSpells.linger(new Rain(p));
		ServerLevel level = p.serverLevel();
		HeavenSpells.sound(level, p.position(), SoundEvents.BUCKET_EMPTY, 1.0f, 0.8f);
		HeavenSpells.sound(level, p.position(), SoundEvents.WEATHER_RAIN_ABOVE, 1.0f, 1.0f);
	}

	/** The rain, where it fell: each tick the spells of those standing in it hasten; each second it mends and cools. */
	static final class Rain extends HeavenSpells.Timed {
		final Vec3 centre;
		final Set<ServerPlayer> hastened = new HashSet<>();

		Rain(ServerPlayer caster) {
			super(caster, RAIN_TICKS);
			this.centre = caster.position();
		}

		boolean inside(LivingEntity e) {
			return within(e, centre, RAIN_RADIUS, 2, 5);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!caster.isAlive() || caster.level() != level) return false;
			List<LivingEntity> friends = HeavenSpells.aroundPoint(level, centre, RAIN_RADIUS + 5, e -> inside(e) && HeavenSpells.friendly(caster, e));
			for (LivingEntity e : friends) {
				// Their cooldowns run down twice as fast: one tick more taken off each tick.
				if (e instanceof ServerPlayer sp && (sp == caster || HeavenSpells.ally(caster, sp))) {
					Spells.hasten(sp, 1);
					hastened.add(sp);
				}
				if (e.isOnFire()) e.clearFire();
			}
			if (age % 20 == 0) {
				for (LivingEntity e : friends) {
					HeavenSpells.mend(e, RAIN_MEND);
					e.setTicksFrozen(0);
					if (age > 0) level.sendParticles(HeavenSpells.dust(FOAM, 0.8f), e.getX(), e.getY() + 1, e.getZ(), 4, 0.3, 0.5, 0.3, 0);
				}
				for (ServerPlayer sp : hastened) Spells.sync(sp);
				hastened.clear();
				HeavenSpells.sound(level, centre, SoundEvents.WEATHER_RAIN, 0.8f, 1.0f);
			}
			if (age % 2 == 0) {
				for (int i = 0; i < 20; i++) {
					Vec3 at = centre.add(randomFlat(RAIN_RADIUS)).add(0, 5, 0);
					level.sendParticles(ParticleTypes.FALLING_WATER, at.x, at.y, at.z, 1, 0, 0, 0, 0);
				}
				for (int i = 0; i < 6; i++) {
					Vec3 at = centre.add(randomFlat(RAIN_RADIUS)).add(0, 0.1, 0);
					level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
				}
				for (int i = 0; i < 8; i++) {
					Vec3 at = centre.add(randomFlat(RAIN_RADIUS)).add(0, 5.5, 0);
					level.sendParticles(HeavenSpells.dust(RAIN_CLOUD, 2.0f), at.x, at.y, at.z, 1, 0, 0, 0, 0);
				}
			}
			return true;
		}
	}

	// --- Coiling Stream Lash ---

	/** Coiling Stream Lash: a whip of water coils round the first being looked at and dashes it down behind the caster. */
	private static void lash(ServerPlayer p) {
		LivingEntity target = HeavenSpells.aimed(p, LASH_RANGE);
		if (target == null) return;
		ServerLevel level = p.serverLevel();
		Vec3 hand = HeavenSpells.fromHands(p), at = HeavenSpells.middle(target), along = at.subtract(hand);
		for (double d = 0; d < along.length(); d += 0.4) {
			Vec3 point = hand.add(along.normalize().scale(d)).add(0, Math.sin(d * 2.5) * 0.3, 0);
			level.sendParticles(HeavenSpells.dust(WATER, 1.0f), point.x, point.y, point.z, 1, 0, 0, 0, 0);
			level.sendParticles(ParticleTypes.SPLASH, point.x, point.y, point.z, 1, 0, 0, 0, 0);
		}
		HeavenSpells.ring(level, at, 0.6, HeavenSpells.dust(FOAM, 1.0f), 10);
		HeavenSpells.sound(level, p.position(), SoundEvents.FISHING_BOBBER_THROW, 1.0f, 0.6f);
		HeavenSpells.sound(level, p.position(), SoundEvents.PLAYER_SPLASH, 1.0f, 1.0f);
		HeavenSpells.sound(level, target.position(), SoundEvents.FISHING_BOBBER_RETRIEVE, 1.0f, 0.8f);
		boolean throwable = !HeavenSpells.greatBeast(target) && target.getMaxHealth() <= LASH_MAX_HEALTH && !target.isVehicle()
				&& target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) < 1;
		if (!throwable) {
			dash(p, target, level);
			return;
		}
		target.stopRiding();
		Vec3 look = p.getViewVector(1.0f).multiply(1, 0, 1);
		look = look.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : look.normalize();
		Vec3 behind = p.position().subtract(look.scale(LASH_BEHIND));
		Vec3 ground = HeavenSpells.groundBelow(level, new Vec3(behind.x, p.getY(), behind.z), 3, 6);
		Vec3 landing = ground != null ? ground : new Vec3(behind.x, p.getY(), behind.z);
		HeavenSpells.linger(new Lash(p, target, target.position(), landing));
	}

	/** The lash's end: the being dashed down for its harm and slowed. */
	private static void dash(ServerPlayer p, LivingEntity target, ServerLevel level) {
		HeavenSpells.strike(p, target, LASH_HARM, QiElement.WATER);
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, LASH_SLOW_TICKS, 2));
		target.resetFallDistance();
		HeavenSpells.particles(level, ParticleTypes.SPLASH, target.position().add(0, 0.5, 0), 40, 0.6, 0.2);
		HeavenSpells.particles(level, ParticleTypes.BUBBLE_POP, target.position().add(0, 0.5, 0), 12, 0.5, 0.05);
		HeavenSpells.particles(level, ParticleTypes.CLOUD, target.position(), 6, 0.4, 0.02);
		HeavenSpells.sound(level, target.position(), SoundEvents.GENERIC_SPLASH, 1.0f, 1.0f);
		HeavenSpells.sound(level, target.position(), SoundEvents.GENERIC_BIG_FALL, 1.0f, 1.0f);
	}

	/** The being swung in an arc over the caster's head to the landing behind it, then dashed down there (or against a wall on the way). */
	static final class Lash extends HeavenSpells.Timed {
		final LivingEntity target;
		final Vec3 from, landing;
		final double rise;
		boolean dashed;

		Lash(ServerPlayer caster, LivingEntity target, Vec3 from, Vec3 landing) {
			super(caster, LASH_TICKS + 1);
			this.target = target;
			this.from = from;
			this.landing = landing;
			this.rise = Math.max(3, caster.getEyeY() + 1.5 - Math.min(from.y, landing.y));
		}

		private Vec3 point(double s) {
			return from.add(landing.subtract(from).scale(s)).add(0, 4 * rise * s * (1 - s), 0);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(target, level)) return false;
			if (age >= LASH_TICKS) return false;
			if (age > 2 && (target.horizontalCollision || target.verticalCollision && !target.verticalCollisionBelow)) return false; // a wall on the way
			HeavenSpells.move(target, point((age + 1) / (double) LASH_TICKS).subtract(target.position()));
			target.resetFallDistance();
			if (target instanceof Mob mob) mob.getNavigation().stop();
			level.sendParticles(ParticleTypes.FALLING_WATER, target.getX(), target.getY() + 0.5, target.getZ(), 3, 0.2, 0.2, 0.2, 0);
			level.sendParticles(HeavenSpells.dust(PALE, 1.0f), target.getX(), target.getY() + 0.5, target.getZ(), 2, 0.1, 0.1, 0.1, 0);
			return true;
		}

		@Override
		public void end() {
			if (dashed || !HeavenSpells.present(target, level)) return;
			dashed = true;
			dash(caster, target, level);
		}
	}

	// --- Nine-Bend Whirlpool ---

	/** Where the whirlpool opens: on the block looked at, else on the ground below the point twelve blocks along the look; or null. */
	private static Vec3 poolCentre(ServerPlayer p) {
		BlockHitResult hit = HeavenSpells.lookedBlock(p, POOL_RANGE);
		if (hit != null) return Vec3.atBottomCenterOf(hit.getBlockPos().above());
		Vec3 ahead = p.getEyePosition().add(p.getViewVector(1.0f).scale(12));
		return HeavenSpells.groundBelow(p.serverLevel(), ahead, 0, 8);
	}

	/** Nine-Bend Whirlpool: a whirlpool at the spot looked at that sweeps beings round, holds them, and flings them out as it collapses. */
	private static void whirlpool(ServerPlayer p) {
		Vec3 centre = poolCentre(p);
		if (centre == null) return;
		HeavenSpells.linger(new Whirlpool(p, centre));
		HeavenSpells.sound(p.serverLevel(), centre, SoundEvents.AMBIENT_UNDERWATER_ENTER, 1.0f, 1.0f);
		HeavenSpells.sound(p.serverLevel(), centre, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.5f, 1.0f);
	}

	/** The whirlpool: each being in it swept round its centre, harmed each second, flung out when it collapses. */
	static final class Whirlpool extends HeavenSpells.Timed {
		final Vec3 centre;

		Whirlpool(ServerPlayer caster, Vec3 centre) {
			super(caster, POOL_TICKS);
			this.centre = centre;
		}

		boolean inside(LivingEntity e) {
			return within(e, centre, POOL_RADIUS, 1, 4);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!caster.isAlive() || caster.level() != level) return false;
			List<LivingEntity> caught = HeavenSpells.aroundPoint(level, centre, POOL_RADIUS + 4, e -> !HeavenSpells.spared(caster, e) && inside(e));
			boolean collapse = age >= POOL_TICKS;
			for (LivingEntity e : caught) {
				Vec3 r = e.position().subtract(centre).multiply(1, 0, 1);
				double d = r.length();
				Vec3 out = d < 0.3 ? new Vec3(1, 0, 0) : r.scale(1 / d);
				double free = 1 - Math.max(0, Math.min(1, e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));
				boolean held = !HeavenSpells.greatBeast(e);
				if (collapse) {
					HeavenSpells.strikeAgain(caster, e, POOL_HARM + POOL_COLLAPSE, QiElement.WATER);
					if (held) HeavenSpells.move(e, out.scale(1.2 * free).add(0, 0.6 * free, 0));
					continue;
				}
				if (held) {
					Vec3 round = new Vec3(-out.z, 0, out.x);
					Vec3 v = round.scale(0.45).subtract(out.scale(0.12 + (d > POOL_RADIUS - 0.5 ? 0.3 : 0))).scale(free);
					HeavenSpells.move(e, new Vec3(v.x, Math.min(e.getDeltaMovement().y, 0.05), v.z));
					if (e instanceof Mob mob) mob.getNavigation().stop();
					e.resetFallDistance();
				}
				if (age > 0 && age % 20 == 0) HeavenSpells.strikeAgain(caster, e, POOL_HARM, QiElement.WATER);
				if (age % 4 == 0) level.sendParticles(ParticleTypes.SPLASH, e.getX(), e.getY() + 0.3, e.getZ(), 2, 0.2, 0.1, 0.2, 0);
			}
			if (collapse) {
				HeavenSpells.particles(level, ParticleTypes.SPLASH, centre.add(0, 0.5, 0), 80, POOL_RADIUS / 2, 0.3);
				HeavenSpells.particles(level, ParticleTypes.CLOUD, centre.add(0, 0.5, 0), 20, 2, 0.05);
				HeavenSpells.particles(level, ParticleTypes.BUBBLE_POP, centre.add(0, 0.5, 0), 20, 2, 0.05);
				HeavenSpells.sound(level, centre, SoundEvents.GENERIC_SPLASH, 1.5f, 1.0f);
				HeavenSpells.sound(level, centre, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.0f, 1.0f);
				return false;
			}
			// Nine arms turning inward.
			for (int arm = 0; arm < 9; arm++) {
				for (int k = 0; k < 6; k++) {
					double r = 0.5 + k * (POOL_RADIUS - 0.5) / 5, a = Math.toRadians(age * 12 + arm * 40) + r * 0.6;
					level.sendParticles(HeavenSpells.dust(k % 2 == 0 ? DEEP : WATER, k % 2 == 0 ? 1.5f : 1.0f), centre.x + Math.cos(a) * r, centre.y + 0.15,
							centre.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
				}
			}
			if (age % 2 == 0) {
				HeavenSpells.ring(level, centre.add(0, 0.2, 0), POOL_RADIUS, ParticleTypes.SPLASH, 10);
				for (double h = 0; h < 3; h += 0.5) level.sendParticles(HeavenSpells.dust(ABYSS, 1.2f), centre.x, centre.y + h, centre.z, 1, 0.1 * h, 0, 0.1 * h, 0);
				level.sendParticles(ParticleTypes.NAUTILUS, centre.x, centre.y + 1, centre.z, 3, 1.5, 0.5, 1.5, 0);
			}
			if (age % 20 == 0) HeavenSpells.sound(level, centre, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 1.2f, 1.0f);
			return true;
		}
	}

	// --- Moon in the Water ---

	/** The first being along the look within reach that can be swapped with: any but the caster, spectators and armour stands. */
	private static LivingEntity moonTarget(ServerPlayer p) {
		Vec3 from = p.getEyePosition();
		Vec3 to = HeavenSpells.beamEnd(p, MOON_RANGE);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.level(), p, from, to, new AABB(from, to).inflate(1.0),
				e -> e instanceof LivingEntity living && living != p && living.isAlive() && !(e instanceof ArmorStand)
						&& !(e instanceof Player other && other.isSpectator()));
		return hit != null && hit.getEntity() instanceof LivingEntity target ? target : null;
	}

	/** Where each goes in the swap (the caster first), a block higher where its place is blocked; or null if there is no room. */
	private static Vec3[] swapRoom(ServerPlayer p, LivingEntity other) {
		ServerLevel level = p.serverLevel();
		Vec3 a = p.position(), b = other.position();
		Vec3 toB = b;
		if (!level.noCollision(p, p.getBoundingBox().move(toB.subtract(a)))) {
			toB = b.add(0, 1, 0);
			if (!level.noCollision(p, p.getBoundingBox().move(toB.subtract(a)))) return null;
		}
		Vec3 toA = a;
		if (!level.noCollision(other, other.getBoundingBox().move(toA.subtract(b)))) {
			toA = a.add(0, 1, 0);
			if (!level.noCollision(other, other.getBoundingBox().move(toA.subtract(b)))) return null;
		}
		return new Vec3[]{toB, toA};
	}

	/** Moon in the Water: the caster and the being looked at change places in an instant, the other left reeling. */
	private static void moon(ServerPlayer p) {
		LivingEntity other = moonTarget(p);
		if (other == null) return;
		p.stopRiding();
		other.stopRiding();
		Vec3[] room = swapRoom(p, other);
		if (room == null) return;
		ServerLevel level = p.serverLevel();
		Vec3 a = p.position(), b = other.position();
		HeavenSpells.teleport(other, room[1]);
		if (other instanceof Mob mob) mob.getNavigation().stop();
		HeavenSpells.teleport(p, room[0]);
		HeavenSpells.safeFall(p, MOON_SAFE_TICKS);
		other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MOON_SLOW_TICKS, 1));
		if (other instanceof Player) other.addEffect(new MobEffectInstance(MobEffects.CONFUSION, MOON_DIZZY_TICKS, 0));
		for (Vec3 at : new Vec3[]{a, b}) {
			HeavenSpells.ring(level, at.add(0, 0.1, 0), 1.5, HeavenSpells.dust(0xCFE6FF, 1.5f), 20);
			HeavenSpells.ring(level, at.add(0, 1.5, 0), 0.6, ParticleTypes.END_ROD, 20);
			HeavenSpells.particles(level, ParticleTypes.SPLASH, at.add(0, 0.5, 0), 20, 0.4, 0.1);
			HeavenSpells.sound(level, at, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);
		}
		HeavenSpells.line(level, a.add(0, 1, 0), b.add(0, 1, 0), HeavenSpells.dust(SILVER, 0.8f), 0.6);
		HeavenSpells.sound(level, b, SoundEvents.AMBIENT_UNDERWATER_EXIT, 1.0f, 1.4f);
	}

	// --- Still Lake Mirror ---

	/** Still Lake Mirror: the caster a still lake that halves every blow, and mirrors back the harm it spared when it ends. */
	private static void mirror(ServerPlayer p) {
		HeavenSpells.endLingering(p.getUUID(), Mirror.class);
		HeavenSpells.linger(new Mirror(p));
		ServerLevel level = p.serverLevel();
		HeavenSpells.sound(level, p.position(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1.0f, 1.0f);
		HeavenSpells.sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 1.0f);
	}

	/** The mirror: the harm spared from each who struck the caster, reflected on them when it ends. */
	static final class Mirror extends HeavenSpells.Timed implements HeavenSpells.HarmListener {
		final Map<LivingEntity, Float> ledger = new HashMap<>();

		Mirror(ServerPlayer caster) {
			super(caster, MIRROR_TICKS);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) {
				ledger.clear(); // fallen or gone: the reflections with it
				return false;
			}
			if (age % 4 == 0) {
				for (int i = 0; i < 16; i++) {
					double a = Math.PI * 2 * i / 16 + age * 0.05;
					level.sendParticles(ParticleTypes.END_ROD, caster.getX() + Math.cos(a) * 1.3, caster.getY() + 1.2, caster.getZ() + Math.sin(a) * 1.3, 1, 0, 0, 0, 0);
				}
				HeavenSpells.ring(level, caster.position().add(0, 0.1, 0), 0.9, HeavenSpells.dust(0xDCEBFF, 0.8f), 12);
			}
			return true;
		}

		@Override
		public float onIncoming(LivingEntity target, DamageSource source, float amount) {
			if (target != caster || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypes.STARVE)) return amount;
			float kept = amount * MIRROR_KEEPS;
			if (source.getEntity() instanceof LivingEntity striker && striker != caster && !HeavenSpells.inHurtTime(caster)
					&& !(striker instanceof OwnableEntity pet && caster.getUUID().equals(pet.getOwnerUUID()))) {
				ledger.merge(striker, amount - kept, Float::sum);
			}
			HeavenSpells.ring(level, HeavenSpells.middle(caster), 1.0, HeavenSpells.dust(0xFFFFFF, 1.0f), 12);
			HeavenSpells.particles(level, ParticleTypes.SPLASH, HeavenSpells.middle(caster), 6, 0.3, 0.05);
			HeavenSpells.sound(level, caster.position(), SoundEvents.AMETHYST_BLOCK_HIT, 1.0f, 1.5f);
			return kept;
		}

		/** The harm spared from {@code striker} so far (tests). */
		float spared(LivingEntity striker) {
			return ledger.getOrDefault(striker, 0f);
		}

		@Override
		public void end() {
			if (!casterHere()) return;
			HeavenSpells.particles(level, HeavenSpells.block(Blocks.GLASS), HeavenSpells.middle(caster), 40, 0.5, 0.1);
			HeavenSpells.sound(level, caster.position(), SoundEvents.GLASS_BREAK, 1.2f, 1.0f);
			HeavenSpells.sound(level, caster.position(), SoundEvents.ILLUSIONER_CAST_SPELL, 1.0f, 1.0f);
			for (Map.Entry<LivingEntity, Float> entry : new ArrayList<>(ledger.entrySet())) {
				LivingEntity striker = entry.getKey();
				if (!HeavenSpells.present(striker, level) || striker.distanceTo(caster) > MIRROR_REACH) continue;
				HeavenSpells.line(level, HeavenSpells.middle(caster), HeavenSpells.middle(striker), HeavenSpells.dust(0xE0F0FF, 1.2f), 0.5);
				HeavenSpells.strikeAgain(caster, striker, Math.min(MIRROR_MAX, 2 * entry.getValue()), QiElement.WATER);
				HeavenSpells.particles(level, HeavenSpells.block(Blocks.GLASS), HeavenSpells.middle(striker), 15, 0.3, 0.05);
				HeavenSpells.particles(level, ParticleTypes.SPLASH, HeavenSpells.middle(striker), 10, 0.3, 0.05);
				HeavenSpells.sound(level, striker.position(), SoundEvents.GLASS_BREAK, 0.6f, 1.6f);
			}
			ledger.clear();
		}
	}

	// --- Abyssal Water Prison ---

	/** Whether {@code e} takes no harm from a globe of water: it breathes water, or needs no air. */
	private static boolean breathesWater(LivingEntity e) {
		return e.canBreatheUnderwater() || MobEffectUtil.hasWaterBreathing(e) || e instanceof IronGolem;
	}

	/** Abyssal Water Prison: a globe of abyssal water closes over the first being looked at, harming, slowing and drowning it. */
	private static void prison(ServerPlayer p) {
		LivingEntity target = HeavenSpells.aimed(p, PRISON_RANGE);
		if (target == null) return;
		ServerLevel level = p.serverLevel();
		HeavenSpells.line(level, HeavenSpells.fromHands(p), HeavenSpells.middle(target), HeavenSpells.dust(DEEP, 1.2f), 0.5);
		HeavenSpells.strike(p, target, PRISON_HARM, QiElement.WATER);
		HeavenSpells.sound(level, target.position(), SoundEvents.CONDUIT_ATTACK_TARGET, 1.0f, 0.6f);
		HeavenSpells.sound(level, target.position(), SoundEvents.AMBIENT_UNDERWATER_ENTER, 1.0f, 0.6f);
		if (!target.isAlive()) return;
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PRISON_TICKS, 1));
		for (Prison old : HeavenSpells.lingering(null, Prison.class)) if (old.target == target) old.end = 0;
		HeavenSpells.linger(new Prison(p, target, !breathesWater(target)));
	}

	/** The globe over its prisoner: its breath gone (if it needs breath), drowning each second. */
	static final class Prison extends HeavenSpells.Timed {
		final LivingEntity target;
		final boolean drowns;

		Prison(ServerPlayer caster, LivingEntity target, boolean drowns) {
			super(caster, PRISON_TICKS);
			this.target = target;
			this.drowns = drowns;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(target, level)) return false;
			if (drowns) {
				target.setAirSupply(0);
				if (age > 0 && age % 20 == 0) {
					HeavenSpells.strikeAgain(caster, target, PRISON_DROWN, QiElement.WATER);
					HeavenSpells.sound(level, target.position(), SoundEvents.PLAYER_HURT_DROWN, 1.0f, 1.0f);
				}
			}
			if (age % 2 == 0) {
				double r = Math.max(1.0, target.getBbHeight() * 0.7);
				globe(level, HeavenSpells.middle(target), r, HeavenSpells.dust(age % 4 == 0 ? DEEP : ABYSS, 1.3f), 40, age * 0.1);
				level.sendParticles(ParticleTypes.BUBBLE_POP, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 3, 0.3, 0.4, 0.3, 0.02);
			}
			return true;
		}

		@Override
		public void end() {
			if (!target.isAlive()) return;
			if (drowns) target.setAirSupply(target.getMaxAirSupply());
			HeavenSpells.particles(level, ParticleTypes.SPLASH, HeavenSpells.middle(target), 40, 0.6, 0.2);
			HeavenSpells.sound(level, target.position(), SoundEvents.GENERIC_SPLASH, 1.0f, 0.8f);
		}
	}

	// --- Overturned Heavenly River ---

	/** Overturned Heavenly River: a wall of water rolls out ahead of the caster, carrying all it meets, and crashes down at its end. */
	private static void river(ServerPlayer p) {
		HeavenSpells.linger(new River(p));
		ServerLevel level = p.serverLevel();
		HeavenSpells.sound(level, p.position(), SoundEvents.AMBIENT_UNDERWATER_ENTER, 1.5f, 0.8f);
		HeavenSpells.sound(level, p.position(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.5f, 0.6f);
	}

	/** The river's front, advancing along the caster's facing over the ground, the beings it caught carried inside it. */
	static final class River extends HeavenSpells.Timed {
		final Vec3 origin, along, across;
		final Set<LivingEntity> caught = new LinkedHashSet<>();
		double front = 1, floorY;

		River(ServerPlayer caster) {
			super(caster, (int) Math.ceil(RIVER_LENGTH / RIVER_SPEED) + 2);
			Vec3 look = caster.getViewVector(1.0f).multiply(1, 0, 1);
			this.along = look.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : look.normalize();
			this.across = new Vec3(-along.z, 0, along.x);
			this.origin = caster.position();
			this.floorY = caster.getY();
		}

		private Vec3 frontCentre() {
			return new Vec3(origin.x + along.x * front, floorY, origin.z + along.z * front);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!caster.isAlive() || caster.level() != level) return false;
			// A wall three blocks high or more breaks it; anything lower it flows over, as over a dip.
			Vec3 at = frontCentre();
			boolean wall = true;
			for (int k = 0; k < 3 && wall; k++) {
				BlockPos pos = BlockPos.containing(at.x, floorY + k, at.z);
				wall = !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
			}
			if (!wall) {
				Vec3 ground = HeavenSpells.groundBelow(level, frontCentre(), 2, 4);
				if (ground != null) floorY = ground.y;
			}
			return roll(age, frontCentre(), wall);
		}

		private boolean roll(long age, Vec3 at, boolean wall) {
			if (wall || front >= RIVER_LENGTH || !level.getWorldBorder().isWithinBounds(at.x, at.z)) {
				crash(at);
				return false;
			}
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(RIVER_HALF_WIDTH + 1, RIVER_HEIGHT + 1, RIVER_HALF_WIDTH + 1),
					e -> !HeavenSpells.spared(caster, e) && inFront(e, at))) {
				if (caught.add(e)) {
					HeavenSpells.strike(caster, e, RIVER_HARM, QiElement.WATER);
					e.clearFire();
				}
				double free = 1 - Math.max(0, Math.min(1, e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));
				if (HeavenSpells.greatBeast(e) || free <= 0) continue;
				Vec3 carry = along.scale(RIVER_SPEED * free);
				HeavenSpells.move(e, new Vec3(carry.x, Math.max(e.getDeltaMovement().y, -0.1) + (e.onGround() ? 0.1 : 0), carry.z));
				if (e instanceof Mob mob) mob.getNavigation().stop();
				e.resetFallDistance();
				if (age % 2 == 0) level.sendParticles(ParticleTypes.SPLASH, e.getX(), e.getY() + 0.5, e.getZ(), 3, 0.3, 0.3, 0.3, 0);
			}
			for (double w = -RIVER_HALF_WIDTH; w <= RIVER_HALF_WIDTH; w += 0.75) {
				Vec3 col = at.add(across.scale(w));
				for (double h = 0; h <= 4; h += 1) {
					level.sendParticles(HeavenSpells.dust(h < 2 ? DEEP : WATER, h < 2 ? 2.0f : 1.5f), col.x, col.y + h, col.z, 1, 0.1, 0.2, 0.1, 0);
				}
				level.sendParticles(ParticleTypes.FALLING_WATER, col.x, col.y + 4, col.z, 1, 0.1, 0.5, 0.1, 0);
				level.sendParticles(HeavenSpells.dust(FOAM, 1.5f), col.x, col.y + RIVER_HEIGHT, col.z, 1, 0.1, 0, 0.1, 0);
				if (((int) (w * 4)) % 3 == 0) level.sendParticles(ParticleTypes.SPLASH, col.x, col.y + RIVER_HEIGHT, col.z, 1, 0.1, 0, 0.1, 0);
			}
			level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.2, at.z, 4, RIVER_HALF_WIDTH / 2, 0.1, 0.5, 0.02);
			if (age % 5 == 0) HeavenSpells.sound(level, at, SoundEvents.PLAYER_SPLASH, 1.0f, 0.6f);
			front += RIVER_SPEED;
			return true;
		}

		private boolean inFront(LivingEntity e, Vec3 at) {
			Vec3 d = e.position().subtract(at);
			double ahead = d.x * along.x + d.z * along.z, side = d.x * across.x + d.z * across.z;
			return Math.abs(ahead) <= 1 && Math.abs(side) <= RIVER_HALF_WIDTH && e.getY() >= floorY - 0.5 && e.getY() <= floorY + RIVER_HEIGHT;
		}

		private void crash(Vec3 at) {
			for (LivingEntity e : caught) {
				if (!HeavenSpells.present(e, level) || fromFront(e, at) > RIVER_CRASH_REACH) continue;
				HeavenSpells.strikeAgain(caster, e, RIVER_CRASH, QiElement.WATER);
				e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, RIVER_SLOW_TICKS, 1));
				double free = 1 - Math.max(0, Math.min(1, e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));
				if (!HeavenSpells.greatBeast(e) && free > 0) HeavenSpells.move(e, new Vec3(0, 0.7, 0).add(along.scale(0.4)).scale(free));
			}
			for (double w = -RIVER_HALF_WIDTH; w <= RIVER_HALF_WIDTH; w += 1.5) {
				Vec3 col = at.add(across.scale(w));
				HeavenSpells.particles(level, ParticleTypes.SPLASH, col.add(0, 1, 0), 15, 0.5, 0.3);
			}
			HeavenSpells.particles(level, ParticleTypes.POOF, at.add(0, 1, 0), 30, RIVER_HALF_WIDTH / 2, 0.05);
			HeavenSpells.particles(level, ParticleTypes.CLOUD, at.add(0, 1, 0), 30, RIVER_HALF_WIDTH / 2, 0.05);
			HeavenSpells.particles(level, ParticleTypes.EXPLOSION, at.add(0, 1, 0), 3, 2, 0);
			HeavenSpells.sound(level, at, SoundEvents.GENERIC_SPLASH, 2.0f, 0.8f);
			HeavenSpells.sound(level, at, SoundEvents.GENERIC_EXPLODE, 0.8f, 0.6f);
		}

		/** How far {@code e} is from the front (a line across the river's width at {@code at}), along the ground. */
		private double fromFront(LivingEntity e, Vec3 at) {
			Vec3 d = e.position().subtract(at);
			double ahead = d.x * along.x + d.z * along.z, side = Math.max(0, Math.abs(d.x * across.x + d.z * across.z) - RIVER_HALF_WIDTH);
			return Math.sqrt(ahead * ahead + side * side);
		}
	}

	// --- Ten Thousand Reflections ---

	/** Whether {@code mob} can fight another (it strikes, explodes or shoots), and is no great beast. */
	private static boolean canTurn(Mob mob) {
		return !HeavenSpells.greatBeast(mob) && (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null || mob instanceof Creeper || mob instanceof RangedAttackMob);
	}

	/** Ten Thousand Reflections: a mirror lake round the caster in which every foe creature sees its enemies in the others. */
	private static void reflections(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		Reflections lake = new Reflections(p);
		for (LivingEntity e : HeavenSpells.around(p, REFLECT_RADIUS, e -> HeavenSpells.foe(p, e))) {
			if (e instanceof Mob mob && canTurn(mob)) {
				lake.turned.add(mob);
				HeavenSpells.sound(level, mob.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.5f, 1.2f);
			}
		}
		for (LivingEntity e : HeavenSpells.around(p, REFLECT_RADIUS, e -> e instanceof Player && HeavenSpells.enemy(p, e))) {
			e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, REFLECT_DIZZY_TICKS, 0));
		}
		HeavenSpells.linger(lake);
		HeavenSpells.sound(level, p.position(), SoundEvents.ILLUSIONER_CAST_SPELL, 1.0f, 1.0f);
		HeavenSpells.sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 0.7f);
		HeavenSpells.sound(level, p.position(), SoundEvents.AMBIENT_UNDERWATER_EXIT, 1.0f, 1.0f);
	}

	/** The lake: the creatures turned on one another, each set on the nearest other, harder on each other while it lasts. */
	static final class Reflections extends HeavenSpells.Timed implements HeavenSpells.HarmListener {
		final Set<Mob> turned = new LinkedHashSet<>();
		final Vec3 centre;

		Reflections(ServerPlayer caster) {
			super(caster, REFLECT_TICKS);
			this.centre = caster.position();
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			if (age < 15) {
				for (double r = 4; r <= REFLECT_RADIUS; r += 4) {
					double ripple = r * (age + 1) / 15.0;
					HeavenSpells.ring(level, centre.add(0, 0.1, 0), ripple, HeavenSpells.dust(0xBFDFFF, 2.0f), (int) (ripple * 3) + 6);
				}
			}
			turned.removeIf(m -> !HeavenSpells.present(m, level));
			for (Mob mob : turned) {
				LivingEntity quarry = mob.getTarget();
				if (quarry instanceof Mob other && turned.contains(other) && other.isAlive()) continue;
				Mob next = null;
				double best = REFLECT_SEEK * REFLECT_SEEK;
				for (Mob other : turned) {
					if (other == mob) continue;
					double d = other.distanceToSqr(mob);
					if (d < best) {
						best = d;
						next = other;
					}
				}
				if (next == null) {
					calm(mob);
					mob.getNavigation().stop();
				} else {
					mob.setTarget(next);
					mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, next);
					if (mob instanceof NeutralMob neutral) {
						neutral.setPersistentAngerTarget(next.getUUID());
						neutral.setRemainingPersistentAngerTime(REFLECT_TICKS);
					}
				}
			}
			if (age % 5 == 0) {
				for (Mob mob : turned) {
					for (int i = 0; i < 8; i++) {
						double a = Math.PI * 2 * i / 8 + age * 0.1;
						level.sendParticles(ParticleTypes.END_ROD, mob.getX() + Math.cos(a) * 0.7, mob.getY() + mob.getBbHeight() * 0.5, mob.getZ() + Math.sin(a) * 0.7, 1, 0, 0, 0, 0);
					}
					level.sendParticles(HeavenSpells.dust(WATER, 0.6f), mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 3, 0.15, 0.05, 0.15, 0);
				}
			}
			return !turned.isEmpty();
		}

		@Override
		public float onIncoming(LivingEntity target, DamageSource source, float amount) {
			return source.getEntity() instanceof Mob striker && turned.contains(striker) && target instanceof Mob struck && turned.contains(struck)
					? amount * REFLECT_BONUS : amount;
		}

		@Override
		public void end() {
			for (Mob mob : turned) {
				if (!mob.isAlive()) continue;
				calm(mob);
				HeavenSpells.particles(level, HeavenSpells.block(Blocks.GLASS), HeavenSpells.middle(mob), 6, 0.3, 0.05);
				HeavenSpells.sound(level, mob.position(), SoundEvents.GLASS_BREAK, 0.3f, 1.8f);
			}
			turned.clear();
		}
	}

	// --- Eternal Winter Descends ---

	/** Eternal Winter Descends: the winter of the northern abyss falls round the caster and freezes everything in it still. */
	private static void winter(ServerPlayer p) {
		HeavenSpells.endLingering(p.getUUID(), Winter.class);
		HeavenSpells.linger(new Winter(p));
		ServerLevel level = p.serverLevel();
		HeavenSpells.sound(level, p.position(), SoundEvents.ELDER_GUARDIAN_CURSE, 0.8f, 0.6f);
		HeavenSpells.sound(level, p.position(), SoundEvents.POWDER_SNOW_PLACE, 1.0f, 1.0f);
		HeavenSpells.sound(level, p.position(), SoundEvents.PLAYER_HURT_FREEZE, 1.0f, 0.5f);
	}

	/** One being in the winter's ice: where it froze, until when, the harm held in the ice, and whether it is shattering now. */
	private static final class Icebound {
		final Vec3 anchor;
		final long until;
		float held;
		boolean shattering;

		Icebound(Vec3 anchor, long until) {
			this.anchor = anchor;
			this.until = until;
		}
	}

	/** The winter: falling for a second, then the frozen held still, their harm held in the ice, until each shatters. */
	static final class Winter extends HeavenSpells.Timed implements HeavenSpells.HarmListener {
		final Vec3 centre;
		final Map<LivingEntity, Icebound> frozen = new HashMap<>();
		/** Everything the winter froze (thawed or not): only the late, never frozen, are merely slowed. */
		final Set<LivingEntity> froze = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

		Winter(ServerPlayer caster) {
			super(caster, WINTER_FALL_TICKS + WINTER_FREEZE_TICKS + 2);
			this.centre = caster.position();
		}

		/** The harm held in {@code e}'s ice now, or -1 if it isn't frozen (tests). */
		float held(LivingEntity e) {
			Icebound ice = frozen.get(e);
			return ice == null ? -1 : ice.held;
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < WINTER_FALL_TICKS) {
				double r = Math.min(WINTER_RADIUS, (age + 1) * 1.2);
				HeavenSpells.ring(level, centre.add(0, 0.2, 0), r, ParticleTypes.SNOWFLAKE, (int) (r * 6));
				HeavenSpells.ring(level, centre.add(0, 0.3, 0), r, HeavenSpells.dust(0xE8F6FF, 2.0f), (int) (r * 3) + 4);
				return true;
			}
			if (age == WINTER_FALL_TICKS) {
				for (LivingEntity e : HeavenSpells.aroundPoint(level, centre, WINTER_RADIUS, e -> !HeavenSpells.spared(caster, e) && !(e instanceof ArmorStand))) {
					int ticks = e instanceof Player || HeavenSpells.greatBeast(e) ? WINTER_FREEZE_SHORT : WINTER_FREEZE_TICKS;
					frozen.put(e, new Icebound(e.position(), now + ticks));
					froze.add(e);
					HeavenSpells.particles(level, HeavenSpells.block(Blocks.PACKED_ICE), HeavenSpells.middle(e), 40, 0.4, 0.05);
					HeavenSpells.particles(level, HeavenSpells.dust(PALE, 1.5f), HeavenSpells.middle(e), 10, 0.4, 0);
					HeavenSpells.sound(level, e.position(), SoundEvents.GLASS_PLACE, 1.0f, 0.6f);
				}
				HeavenSpells.sound(level, caster.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 0.5f);
			}
			for (java.util.Iterator<Map.Entry<LivingEntity, Icebound>> it = frozen.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<LivingEntity, Icebound> entry = it.next();
				LivingEntity e = entry.getKey();
				Icebound ice = entry.getValue();
				if (!HeavenSpells.present(e, level)) {
					it.remove();
					continue;
				}
				if (now >= ice.until) {
					it.remove();
					shatter(e, ice);
					continue;
				}
				holdFrozen(e, ice.anchor, true);
				e.clearFire();
				e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 6, false, false));
				e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 10, 2, false, false));
				e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10, 2, false, false));
				if (age % 4 == 0) HeavenSpells.particles(level, HeavenSpells.dust(ICE, 1.0f), HeavenSpells.middle(e), 6, 0.3, 0);
			}
			if (age <= WINTER_MISSILE_TICKS) {
				for (Projectile missile : level.getEntitiesOfClass(Projectile.class, new AABB(centre, centre).inflate(WINTER_RADIUS),
						m -> m.distanceToSqr(centre) <= WINTER_RADIUS * WINTER_RADIUS && m.getOwner() != caster)) {
					if (missile instanceof AbstractHurtingProjectile) {
						HeavenSpells.particles(level, ParticleTypes.SNOWFLAKE, missile.position(), 8, 0.2, 0.02);
						missile.discard(); // a fireball or a skull, snuffed out by the cold
						continue;
					}
					missile.setDeltaMovement(Vec3.ZERO);
					missile.hurtMarked = true;
					if (age % 4 == 0) level.sendParticles(ParticleTypes.SNOWFLAKE, missile.getX(), missile.getY(), missile.getZ(), 1, 0, 0, 0, 0);
				}
				if (age % 10 == 0) {
					for (LivingEntity late : HeavenSpells.aroundPoint(level, centre, WINTER_RADIUS, e -> !HeavenSpells.spared(caster, e) && !froze.contains(e))) {
						late.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 3));
					}
				}
				for (int i = 0; i < 40; i++) {
					Vec3 at = centre.add(randomFlat(WINTER_RADIUS)).add(0, 8, 0);
					level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 0, 0, -1, 0, 0.1);
				}
			}
			return age <= WINTER_MISSILE_TICKS || !frozen.isEmpty();
		}

		private void shatter(LivingEntity e, Icebound ice) {
			ice.shattering = true;
			e.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
			e.removeEffect(MobEffects.DIG_SLOWDOWN);
			e.removeEffect(MobEffects.WEAKNESS);
			float harm = WINTER_SHATTER + Math.min(WINTER_HELD_MAX, WINTER_HELD_SHARE * ice.held);
			if (e instanceof Player) harm = Math.min(harm, e.getMaxHealth() * WINTER_PLAYER_CAP); // never a player killed outright
			HeavenSpells.strikeAgain(caster, e, harm, QiElement.WATER);
			if (e.isAlive()) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
			Vec3 at = HeavenSpells.middle(e);
			HeavenSpells.particles(level, HeavenSpells.block(Blocks.BLUE_ICE), at, 60, 0.4, 0.1);
			HeavenSpells.particles(level, ParticleTypes.ITEM_SNOWBALL, at, 20, 0.4, 0.1);
			HeavenSpells.particles(level, ParticleTypes.END_ROD, at, 10, 0.4, 0.05);
			HeavenSpells.particles(level, ParticleTypes.POOF, at, 10, 0.4, 0.02);
			HeavenSpells.sound(level, e.position(), SoundEvents.GLASS_BREAK, 1.0f, 0.8f + java.util.concurrent.ThreadLocalRandom.current().nextFloat() * 0.4f);
		}

		@Override
		public float onIncoming(LivingEntity target, DamageSource source, float amount) {
			Icebound ice = frozen.get(target);
			if (ice == null || ice.shattering || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
			ice.held += amount; // held in the ice, for the shatter
			return 0;
		}

		@Override
		public void end() {
			// Cut short (its caster gone, the server stopping): the ice simply thaws.
			for (LivingEntity e : frozen.keySet()) {
				if (!e.isAlive()) continue;
				e.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
				e.removeEffect(MobEffects.DIG_SLOWDOWN);
				e.removeEffect(MobEffects.WEAKNESS);
			}
			frozen.clear();
		}
	}

	private WaterSpells() {}
}
