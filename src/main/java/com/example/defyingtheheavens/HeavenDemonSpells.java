package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The spells of both paths (Spell#isDual), the heaven path's and the Demon Path's as one (the user's ask, 2026-10-24: "the dual path
 * spells are now from demon and heaven path instead of heaven and ghost"): one for each realm from Foundation Building, in reach once
 * both paths stand there (TwoPaths#bothReach), cast by the living on qi alone (Spells#cast pays; the Demon Path shares the qi pool).
 * Heaven's gold light and a demon's crimson qi, twined; their harm the two as one (ModDamageTypes#HEAVEN_DEMON: through armour, as the
 * elements' qi), reaching the dead as a living blow does. The five before them were the heaven path's and the ghost path's (Yin-Yang
 * Palm, Taiji Shield, Life and Death Seal, Heaven and Hell Pillar, Primordial Chaos), all built on the ghost path's half: these take
 * their places, their reach and strength kept. Dao Integration's, the Heaven-Demon Avatar, is a living shape of GeckoLib's
 * (SpellConstruct HEAVEN_DEMON_AVATAR, its hands AVATAR_PALM and AVATAR_CLAW); Void Shattering's, the Heaven-Demon Void Shatter, a fist of
 * the two that shatters space where it lands (HEAVEN_DEMON_FIST, SHATTERED_SPACE). Those two wound the land (since 2026-10-24: the
 * Avatar's hands crater and gouge it, the Void Shatter's blow craters and cracks it); the rest leave it as it was.
 */
public final class HeavenDemonSpells {
	/** Heaven's gold and its paler light; a demon's crimson and the dark of its blood. */
	static final int GOLD = 0xFFC83A, GOLD_PALE = 0xFFF0B0, CRIMSON = 0xD0182C, BLOOD = 0x5A0612;

	/** Heaven-Demon Palm: reach and harm, and the share of the harm done that mends the caster. */
	public static final double PALM_RANGE = 16;
	public static final float PALM_DAMAGE = 10.0f, PALM_MENDS = 0.25f;
	/** Heaven-Demon Mantle: how long it wraps the caster, and how near what is shot is turned back. */
	public static final int MANTLE_TICKS = 10 * 20;
	public static final double MANTLE_RADIUS = 3.5;
	/** Devouring Seal: reach, how long it holds, the harm a second, and the share of it that mends the caster. */
	public static final double SEAL_RANGE = 32;
	public static final int SEAL_TICKS = 8 * 20;
	public static final float SEAL_DAMAGE = 4.0f, SEAL_MENDS = 0.5f;
	/** Heaven-Demon Claw: how far off it can rake, how wide it strikes, the harm, and how long the struck are weakened. */
	public static final double CLAW_RANGE = 32, CLAW_RADIUS = 5;
	public static final float CLAW_DAMAGE = 30.0f;
	public static final int CLAW_WEAKENS = 5 * 20;
	/** Heaven and Demon as One: reach, harm, the share of all the harm done that mends the caster, and how long the caster is made strong. */
	public static final double ONE_RADIUS = 16;
	public static final float ONE_DAMAGE = 50.0f, ONE_MENDS = 0.1f;
	public static final int ONE_TICKS = 10 * 20;
	/**
	 * Heaven-Demon Avatar: how far off its hands come down (82, by its realm: HeavenSpells#span), how wide each strikes (14) and how hard, the
	 * share of all its harm that mends the caster, how tall it stands (41: its model, SpellConstruct#sizedFor), how far behind its caster it
	 * rises, how long it stays, how long a hand takes to fall, how long a fallen hand stays pressed into the land, how long the struck are
	 * slowed, and how fast it turns to where its caster looks (degrees a tick).
	 */
	public static final double AVATAR_RANGE = HeavenSpells.span(Realm.DAO_INTEGRATION, 64), AVATAR_RADIUS = HeavenSpells.span(Realm.DAO_INTEGRATION, 11),
			AVATAR_HEIGHT = HeavenSpells.span(Realm.DAO_INTEGRATION, 32), AVATAR_BEHIND = HeavenSpells.span(Realm.DAO_INTEGRATION, 5);
	public static final float AVATAR_DAMAGE = 140, AVATAR_MENDS = 0.2f, AVATAR_TURN = 4;
	public static final int AVATAR_TICKS = 8 * 20, AVATAR_FALL = 14, AVATAR_PRESS = 26, AVATAR_SLOWS = 3 * 20;
	/**
	 * What its hands leave in the land (the user's ask, 2026-10-24: more of it): heaven's palm a crater (18 round, 8 deep: by its realm,
	 * Devastation#sized) with cracks running out from it; the demon's claw four gouges (28 long, 2.5 wide, 8 deep) raked through the land
	 * the way it came down, and a lesser crater where it struck.
	 */
	public static final double AVATAR_PALM_CRATER = Devastation.sized(Realm.DAO_INTEGRATION, 14), AVATAR_PALM_DEPTH = Devastation.sized(Realm.DAO_INTEGRATION, 6),
			AVATAR_GOUGE = Devastation.sized(Realm.DAO_INTEGRATION, 22), AVATAR_GOUGE_WIDTH = 2.5, AVATAR_GOUGE_DEPTH = Devastation.sized(Realm.DAO_INTEGRATION, 6),
			AVATAR_CLAW_CRATER = Devastation.sized(Realm.DAO_INTEGRATION, 7);
	public static final int AVATAR_GOUGES = 4;
	/** When (ticks after it rises) its hands are raised, each to come down {@link #AVATAR_FALL} later: heaven's palm, then the demon's claw. */
	public static final int[] AVATAR_STRIKES = {40, 100};
	/**
	 * Heaven-Demon Void Shatter: how far its fist flies (118 by its realm: HeavenSpells#span), whom its blow strikes (within
	 * {@link #SHATTER_RADIUS}, 18) and how hard, how far across the shattered space is (37), and what it cuts a second; the fist's flight, and
	 * how long the shattered space hangs there (ticks).
	 */
	public static final double SHATTER_RANGE = HeavenSpells.span(Realm.VOID_SHATTERING, 64), SHATTER_RADIUS = HeavenSpells.span(Realm.VOID_SHATTERING, 10),
			SHATTER_SPACE = HeavenSpells.span(Realm.VOID_SHATTERING, 20);
	public static final float SHATTER_DAMAGE = 520, SHATTER_CUTS = 60;
	public static final int SHATTER_FLIGHT = 12, SHATTER_TICKS = 5 * 20;
	/**
	 * What its blow leaves in the land where it lands near the ground (the user's ask, 2026-10-24): a crater (28 round, 15 deep: by its
	 * realm) and {@link #SHATTER_CRACKS} cracks running out from it like a pane's, to {@link #SHATTER_CRACK_REACH}; nothing if it lands
	 * further than {@link #SHATTER_GROUND} over the land (its blow is space's, there).
	 */
	public static final double SHATTER_CRATER = Devastation.sized(Realm.VOID_SHATTERING, 15), SHATTER_CRATER_DEPTH = Devastation.sized(Realm.VOID_SHATTERING, 8),
			SHATTER_CRACK_REACH = Devastation.sized(Realm.VOID_SHATTERING, 30), SHATTER_GROUND = HeavenSpells.span(Realm.VOID_SHATTERING, 10);
	public static final int SHATTER_CRACKS = 12, SHATTER_CRACK_DEPTH = 9;
	/**
	 * Heaven-Demon Bell: where (up to {@link #BELL_RANGE}, 136 by its realm), how high it falls from (64), how far its mouth spans (19: under
	 * it) and how far each toll's wave runs out over the land round it (38); each toll's harm under it and in its wave, and the share of all
	 * of it that mends the caster; the crater its rim presses into the land (19 round, 4 deep) and the cracks each toll sends out (to 41), by
	 * its realm; how long it falls, how long after it lands it first tolls, how far apart its tolls, and how long it takes to rise away (ticks).
	 */
	public static final double BELL_RANGE = HeavenSpells.span(Realm.SACRED_VESSEL, 64), BELL_HIGH = HeavenSpells.span(Realm.SACRED_VESSEL, 30),
			BELL_RADIUS = HeavenSpells.span(Realm.SACRED_VESSEL, 9), BELL_WAVE = HeavenSpells.span(Realm.SACRED_VESSEL, 18),
			BELL_CRATER = Devastation.sized(Realm.SACRED_VESSEL, 9), BELL_CRATER_DEPTH = Devastation.sized(Realm.SACRED_VESSEL, 2),
			BELL_CRACKS = Devastation.sized(Realm.SACRED_VESSEL, 19);
	public static final float BELL_TOLL = 500, BELL_WAVE_DAMAGE = 200, BELL_MENDS = 0.2f;
	public static final int BELL_FALL = 18, BELL_FIRST = 12, BELL_EVERY = 20, BELL_TOLLS = 3, BELL_RISE = 18;
	/**
	 * Heaven-Demon Eclipse: where (154 by its realm), how high over the land the sun and the moon meet (58); how far round its beam of darkness
	 * strikes (22); each pulse of it, the first the greatest (through armour); the share of all of it mending its caster; the crater of black
	 * glass its first pulse burns (19 round, 7 deep, by its realm); how long they take to meet, how far apart its pulses come, and how long it
	 * has after (ticks).
	 */
	public static final double ECLIPSE_RANGE = HeavenSpells.span(Realm.ENTERING_NIRVANA, 64), ECLIPSE_HIGH = HeavenSpells.span(Realm.ENTERING_NIRVANA, 24),
			ECLIPSE_RADIUS = HeavenSpells.span(Realm.ENTERING_NIRVANA, 9), ECLIPSE_CRATER = Devastation.sized(Realm.ENTERING_NIRVANA, 8),
			ECLIPSE_CRATER_DEPTH = Devastation.sized(Realm.ENTERING_NIRVANA, 3);
	public static final float[] ECLIPSE_PULSES = {1200, 600, 600};
	public static final float ECLIPSE_MENDS = 0.2f;
	public static final int ECLIPSE_MEETS = 32, ECLIPSE_EVERY = 15, ECLIPSE_AFTER = 24;
	/**
	 * Primordial Chaos: where (218 by its realm), how high over the spot its sphere hangs (24), how far round it draws everything in (54), how far
	 * from its heart it churns what it has drawn (27), how far round its burst reaches (34); the crater of glass it leaves (24 round, 10 deep, by
	 * its realm); its harm each time it churns, and as it bursts, through armour; the share of all of it mending its caster; how long it takes to
	 * swell, how long it churns, how often, how long it takes to burst, and how long it has after (ticks).
	 */
	public static final double CHAOS_RANGE = HeavenSpells.span(Realm.TRUE_IMMORTAL, 64), CHAOS_HIGH = HeavenSpells.span(Realm.TRUE_IMMORTAL, 7),
			CHAOS_PULL = HeavenSpells.span(Realm.TRUE_IMMORTAL, 16), CHAOS_RADIUS = HeavenSpells.span(Realm.TRUE_IMMORTAL, 8),
			CHAOS_BURST_RADIUS = HeavenSpells.span(Realm.TRUE_IMMORTAL, 10), CHAOS_CRATER = Devastation.sized(Realm.TRUE_IMMORTAL, 7),
			CHAOS_CRATER_DEPTH = Devastation.sized(Realm.TRUE_IMMORTAL, 3);
	public static final float CHAOS_CHURN = 1000, CHAOS_BURST = 15000, CHAOS_MENDS = 0.2f;
	public static final int CHAOS_SWELLS = 30, CHAOS_TICKS = 5 * 20, CHAOS_EVERY = 10, CHAOS_BURSTS = 5, CHAOS_AFTER = 14;

	/** Whether {@code spell} can take hold for {@code p} now: a seal wants a being to seal (refused, nothing is paid). */
	public static boolean canCast(LivingEntity p, Spell spell) {
		return spell != Spell.DEVOURING_SEAL || HeavenSpells.aimed(p, SEAL_RANGE) != null || HeavenSpells.refuse(p);
	}

	/** Casts {@code spell} (Spells#cast has paid for it). */
	public static void cast(LivingEntity p, Spell spell) {
		switch (spell) {
			case HEAVEN_DEMON_PALM -> palm(p);
			case HEAVEN_DEMON_MANTLE -> {
				HeavenSpells.endLingering(p.getUUID(), Mantle.class);
				HeavenSpells.linger(new Mantle(p));
			}
			case DEVOURING_SEAL -> {
				LivingEntity target = HeavenSpells.aimed(p, SEAL_RANGE);
				if (target != null) HeavenSpells.linger(new Seal(p, target));
			}
			case HEAVEN_DEMON_CLAW -> claw(p);
			case HEAVEN_AND_DEMON_AS_ONE -> asOne(p);
			case HEAVEN_DEMON_AVATAR -> {
				HeavenSpells.endLingering(p.getUUID(), Avatar.class);
				HeavenSpells.linger(new Avatar(p, spell));
			}
			case HEAVEN_DEMON_VOID_SHATTER -> HeavenSpells.linger(new Shatter(p, spell));
			case HEAVEN_DEMON_BELL -> {
				LivingEntity aimed = HeavenSpells.aimed(p, BELL_RANGE);
				HeavenSpells.linger(new Bell(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, BELL_RANGE)));
			}
			case HEAVEN_DEMON_ECLIPSE -> {
				LivingEntity aimed = HeavenSpells.aimed(p, ECLIPSE_RANGE);
				HeavenSpells.linger(new Eclipse(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, ECLIPSE_RANGE)));
			}
			case PRIMORDIAL_CHAOS -> {
				HeavenSpells.endLingering(p.getUUID(), Chaos.class);
				LivingEntity aimed = HeavenSpells.aimed(p, CHAOS_RANGE);
				HeavenSpells.linger(new Chaos(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, CHAOS_RANGE)));
			}
			default -> {}
		}
	}

	// --- Harm and light ---

	/**
	 * Strikes {@code target} for {@code amount} of heaven-and-demon harm by {@code p} (not past a barrier it couldn't break:
	 * HeavenSpells#walled) and returns how much it took.
	 */
	static float strike(LivingEntity p, LivingEntity target, float amount) {
		if (HeavenSpells.walled(p, target)) return 0;
		HeavenSpells.landsAcross(p, target);
		float before = target.getHealth() + target.getAbsorptionAmount();
		target.hurt(ModDamageTypes.heavenDemon(HeavenSpells.level(p), p), amount);
		return Math.max(0, before - target.getHealth() - target.getAbsorptionAmount());
	}

	static DustParticleOptions gold(float size) {
		return HeavenSpells.dust(GOLD, size);
	}

	static DustParticleOptions crimson(float size) {
		return HeavenSpells.dust(CRIMSON, size);
	}

	/** Heaven's gold and a demon's crimson wound about each other along a line, from {@code a} to {@code b}. */
	static void twined(ServerLevel level, Vec3 a, Vec3 b, double turns) {
		Vec3 along = b.subtract(a);
		double length = along.length();
		if (length < 1.0e-3) return;
		Vec3 dir = along.scale(1 / length);
		Vec3 side = dir.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(dir).normalize();
		DustParticleOptions gold = gold(1.1f), red = crimson(1.1f);
		for (double d = 0; d < length; d += 0.3) {
			double angle = d / length * Math.PI * 2 * turns;
			Vec3 offset = side.scale(Math.cos(angle) * 0.35).add(up.scale(Math.sin(angle) * 0.35));
			Vec3 at = a.add(dir.scale(d));
			level.sendParticles(gold, at.x + offset.x, at.y + offset.y, at.z + offset.z, 1, 0, 0, 0, 0);
			level.sendParticles(red, at.x - offset.x, at.y - offset.y, at.z - offset.z, 1, 0, 0, 0, 0);
			if (((int) (d / 0.3)) % 4 == 0) level.sendParticles(ParticleTypes.END_ROD, at.x + offset.x, at.y + offset.y, at.z + offset.z, 1, 0, 0, 0, 0);
		}
	}

	/**
	 * A demon's claw marks: {@code claws} rakes of crimson qi edged with gold, {@code size} long, slanting down across {@code at} as seen
	 * from the way {@code way}.
	 */
	static void rake(ServerLevel level, Vec3 at, Vec3 way, double size, int claws) {
		Vec3 flat = new Vec3(way.x, 0, way.z);
		Vec3 side = flat.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : new Vec3(-flat.z, 0, flat.x).normalize();
		Vec3 up = new Vec3(0, 1, 0);
		// Down and across, and the way the claws stand apart (square to it).
		Vec3 stroke = side.scale(0.55).subtract(up).normalize(), apart = side.add(up.scale(0.55)).normalize();
		DustParticleOptions red = crimson(Math.min(3.0f, 0.9f + (float) size * 0.12f)), edge = gold(0.8f);
		for (int k = 0; k < claws; k++) {
			Vec3 middle = at.add(apart.scale((k - (claws - 1) / 2.0) * size * 0.22));
			Vec3 a = middle.subtract(stroke.scale(size / 2)), b = middle.add(stroke.scale(size / 2));
			HeavenSpells.line(level, a, b, red, Math.max(0.12, size * 0.02));
			HeavenSpells.line(level, a.add(apart.scale(0.12)), b.add(apart.scale(0.12)), edge, Math.max(0.25, size * 0.04));
		}
	}

	// --- Foundation Building: Heaven-Demon Palm ---

	/**
	 * Heaven-Demon Palm: gold light and crimson qi twined out to the first being ahead; it is struck, raked and thrown back, and a share of it
	 * mends you. A palm of heaven's gold, a demon's qi burning off it (SpellConstruct HD_PALM), is thrust out along the twined light to it.
	 */
	private static void palm(LivingEntity p) {
		ServerLevel level = HeavenSpells.level(p);
		LivingEntity target = HeavenSpells.aimed(p, PALM_RANGE);
		Vec3 from = HeavenSpells.fromHands(p);
		Vec3 to = target == null ? HeavenSpells.beamEnd(p, PALM_RANGE) : HeavenSpells.middle(target);
		twined(level, from, to, 1.5);
		HeavenSpells.linger(new Thrust(p, from, to));
		HeavenSpells.sound(level, p.position(), SoundEvents.EVOKER_CAST_SPELL, 1.0f, 1.3f);
		HeavenSpells.sound(level, p.position(), SoundEvents.RAVAGER_ATTACK, 0.8f, 1.4f);
		if (target == null) return;
		float taken = strike(p, target, PALM_DAMAGE);
		HeavenSpells.push(p.position(), target, 1.8);
		HeavenSpells.mend(p, taken * PALM_MENDS);
		rake(level, to, to.subtract(from), 1.4, 3);
		SpellFx.send(level, SpellFx.Kind.FLASH, to, Vec3.ZERO, 0.6f, GOLD);
		HeavenSpells.sound(level, to, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 0.7f);
	}

	/** The palm thrust out from its caster's hands to where it strikes ({@link #THRUST} ticks), where it flares, and goes. */
	static final class Thrust extends HeavenSpells.Timed {
		private final Vec3 from, to;
		private final SpellConstruct palm;

		Thrust(LivingEntity caster, Vec3 from, Vec3 to) {
			super(caster, THRUST + 8);
			this.from = from;
			this.to = to;
			Vec3 d = to.subtract(from);
			float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z)), pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
			this.palm = SpellConstruct.create(level, SpellConstruct.Kind.HD_PALM, from, yaw, pitch, THRUST + 18, caster).spawn();
		}

		@Override
		protected boolean run(long age, long now) {
			if (age <= THRUST) palm.place(from.lerp(to, age / (double) THRUST), palm.getYRot(), palm.getXRot());
			if (age == THRUST) palm.act("strike");
			return true;
		}

		@Override
		public void end() {
			palm.dissolve();
		}
	}

	/** Ticks the palm takes from its caster's hands to where it strikes. */
	static final int THRUST = 5;

	// --- Core Formation: Heaven-Demon Mantle ---

	/**
	 * Heaven-Demon Mantle: gold light and crimson demon scales wrap the caster for {@link #MANTLE_TICKS} (Resistance I); what flies at it
	 * from near ({@link #MANTLE_RADIUS}) is sent back the way it came, its shooter's own now.
	 */
	public static final class Mantle extends HeavenSpells.Timed {
		/** Its rings of the two round its caster (SpellConstruct HD_MANTLE), drawn at their size. */
		private final SpellConstruct mantle;

		Mantle(LivingEntity caster) {
			super(caster, MANTLE_TICKS);
			caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, MANTLE_TICKS, 0));
			HeavenSpells.sound(level, caster.position(), SoundEvents.BEACON_ACTIVATE, 1.0f, 1.5f);
			HeavenSpells.sound(level, caster.position(), SoundEvents.ARMOR_EQUIP_NETHERITE, 1.0f, 0.7f);
			this.mantle = SpellConstruct.create(level, SpellConstruct.Kind.HD_MANTLE, caster.position(), 0, 0, MANTLE_TICKS + 10, caster)
					.size(caster.getBbHeight() / 2).spawn();
			wrap(0);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			mantle.place(caster.position(), 0, 0);
			turnBack();
			return true;
		}

		@Override
		public void end() {
			mantle.dissolve();
		}

		/** The mantle turned {@code turn} radians: a ring of gold light about the chest, a ring of crimson scales turning the other way below it. */
		private void wrap(double turn) {
			DustParticleOptions gold = gold(1.0f), scale = crimson(1.3f);
			double y = caster.getY() + caster.getBbHeight() * 0.62, low = caster.getY() + caster.getBbHeight() * 0.3;
			for (int i = 0; i < 16; i++) {
				double a = turn + Math.PI * 2 * i / 16, b = -turn * 1.3 + Math.PI * 2 * (i + 0.5) / 16;
				level.sendParticles(gold, caster.getX() + Math.cos(a) * 1.3, y + Math.sin(a * 2 + turn) * 0.1, caster.getZ() + Math.sin(a) * 1.3, 1, 0, 0, 0, 0);
				level.sendParticles(scale, caster.getX() + Math.cos(b) * 1.1, low, caster.getZ() + Math.sin(b) * 1.1, 1, 0, 0, 0, 0);
			}
			if (turn == 0) level.sendParticles(ParticleTypes.END_ROD, caster.getX(), y, caster.getZ(), 12, 0.6, 0.5, 0.6, 0.02);
		}

		/** What flies at its caster from near is sent back the way it came, its shooter's own now. */
		private void turnBack() {
			Vec3 centre = HeavenSpells.middle(caster);
			for (net.minecraft.world.entity.projectile.Projectile shot : level.getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class,
					caster.getBoundingBox().inflate(MANTLE_RADIUS), shot -> shot.getOwner() != caster && shot.isAlive()
							&& !(shot instanceof net.minecraft.world.entity.projectile.FishingHook))) { // a line cast is no shot (and is its angler's)
				Vec3 motion = shot.getDeltaMovement();
				if (motion.lengthSqr() < 1.0e-4 || motion.dot(centre.subtract(shot.position())) <= 0) continue; // not coming at it
				shot.setDeltaMovement(motion.scale(-1));
				if (shot instanceof net.minecraft.world.entity.projectile.AbstractHurtingProjectile fireball) {
					fireball.xPower = -fireball.xPower;
					fireball.yPower = -fireball.yPower;
					fireball.zPower = -fireball.zPower;
				}
				shot.setOwner(caster);
				shot.hurtMarked = true;
				level.sendParticles(crimson(1.2f), shot.getX(), shot.getY(), shot.getZ(), 6, 0.15, 0.15, 0.15, 0);
				level.sendParticles(ParticleTypes.END_ROD, shot.getX(), shot.getY(), shot.getZ(), 4, 0.1, 0.1, 0.1, 0.05);
				HeavenSpells.sound(level, shot.position(), SoundEvents.SHIELD_BLOCK, 0.8f, 1.4f);
			}
		}
	}

	/** Whether a Heaven-Demon Mantle wraps {@code p} now. */
	public static boolean mantled(LivingEntity p) {
		return !HeavenSpells.lingering(p.getUUID(), Mantle.class).isEmpty();
	}

	// --- Nascent Soul: Devouring Seal ---

	/**
	 * Devouring Seal: the being its caster looks at, sealed under heaven's gold seal for {@link #SEAL_TICKS} (slowed), while a demon's maw
	 * devours it: once a second it takes {@link #SEAL_DAMAGE}, and {@link #SEAL_MENDS} of it comes to the caster (in its level). With its
	 * caster gone, the seal still does its harm, by no one's hand.
	 */
	public static final class Seal extends HeavenSpells.Timed {
		final LivingEntity sealed;
		/** Heaven's seal over it and the maw round it (SpellConstruct DEVOURING_SEAL), drawn at its size; the maw bites as its harm comes. */
		private final SpellConstruct seal;

		Seal(LivingEntity caster, LivingEntity sealed) {
			super(caster, SEAL_TICKS);
			this.sealed = sealed;
			sealed.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SEAL_TICKS, 1));
			twined(level, HeavenSpells.fromHands(caster), HeavenSpells.middle(sealed), 2.5);
			this.seal = SpellConstruct.create(level, SpellConstruct.Kind.DEVOURING_SEAL, sealed.position(), caster.getRandom().nextFloat() * 360, 0, SEAL_TICKS + 10, caster)
					.size(Math.max(sealed.getBbHeight() / 2, sealed.getBbWidth() / 0.75f)).spawn();
			HeavenSpells.sound(level, caster.position(), SoundEvents.EVOKER_PREPARE_ATTACK, 1.0f, 0.7f);
			HeavenSpells.sound(level, sealed.position(), SoundEvents.BELL_RESONATE, 1.0f, 1.4f);
		}

		@Override
		public void end() {
			seal.dissolve();
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(sealed, level)) return false;
			seal.place(sealed.position(), seal.getYRot(), 0);
			if (age == 0 || age % 20 != 0) return true; // once a second, from a second on
			seal.act("bite");
			if (!caster.isAlive() || caster.isRemoved()) {
				sealed.hurt(ModDamageTypes.heavenDemon(level, null), SEAL_DAMAGE);
				return true;
			}
			float taken = strike(caster, sealed, SEAL_DAMAGE);
			if (taken > 0 && caster.level() == level) {
				HeavenSpells.mend(caster, taken * SEAL_MENDS);
				// Its life, drawn off to the caster in crimson.
				HeavenSpells.line(level, HeavenSpells.middle(sealed), HeavenSpells.middle(caster), crimson(0.9f), 0.6);
			}
			HeavenSpells.sound(level, sealed.position(), SoundEvents.GENERIC_EAT, 0.8f, 0.5f);
			return true;
		}
	}

	/** Whether a Devouring Seal holds {@code target} now. */
	public static boolean sealed(LivingEntity target) {
		for (Seal seal : HeavenSpells.lingering(null, Seal.class)) if (seal.sealed == target) return true;
		return false;
	}

	// --- Heavenly Being: Heaven-Demon Claw ---

	/**
	 * Heaven-Demon Claw: a great claw of gold light and crimson qi rakes down out of the sky where its caster looks (on the being looked at,
	 * up to {@link #CLAW_RANGE}): all within {@link #CLAW_RADIUS} of it are struck, thrown up and weakened.
	 */
	private static void claw(LivingEntity p) {
		ServerLevel level = HeavenSpells.level(p);
		Vec3 at = HeavenSpells.beamEnd(p, CLAW_RANGE);
		LivingEntity aimedAt = HeavenSpells.aimed(p, CLAW_RANGE);
		if (aimedAt != null) at = aimedAt.position();
		Vec3 way = at.subtract(p.position());
		// Four great talons rake down out of the sky through the place and on (SpellConstruct HD_CLAW), leaving their gouges burning.
		float yaw = way.horizontalDistanceSqr() < 1.0e-4 ? p.getYRot() : (float) Math.toDegrees(Math.atan2(-way.x, way.z));
		SpellConstruct talons = SpellConstruct.create(level, SpellConstruct.Kind.HD_CLAW, at, yaw, 0, 26, p).spawn();
		HeavenSpells.linger(new FireSpells.Delayed(p, 16, talons::dissolve));
		SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, at, Vec3.ZERO, (float) CLAW_RADIUS, 0);
		SpellFx.send(level, SpellFx.Kind.FLASH, at.add(0, 1, 0), Vec3.ZERO, 1.5f, GOLD);
		HeavenSpells.ring(level, at.add(0, 0.2, 0), CLAW_RADIUS, crimson(1.5f), 40);
		HeavenSpells.sound(level, at, SoundEvents.RAVAGER_ROAR, 2.0f, 0.7f);
		HeavenSpells.sound(level, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5f, 1.3f);
		HeavenSpells.sound(level, at, SoundEvents.PLAYER_ATTACK_SWEEP, 2.0f, 0.5f);
		Vec3 centre = at;
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(CLAW_RADIUS, 6, CLAW_RADIUS),
				e -> !HeavenSpells.spared(p, e) && e.position().multiply(1, 0, 1).distanceToSqr(centre.multiply(1, 0, 1)) <= CLAW_RADIUS * CLAW_RADIUS)) {
			strike(p, target, CLAW_DAMAGE);
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, CLAW_WEAKENS, 0));
			HeavenSpells.move(target, target.getDeltaMovement().add(0, 0.7, 0));
		}
	}

	// --- Four Axis: Heaven and Demon as One ---

	/** Heaven and Demon as One: the two made one: all around dragged in and struck, a share of it all mending you, and you made strong. */
	private static void asOne(LivingEntity p) {
		ServerLevel level = HeavenSpells.level(p);
		HeavenSpells.sound(level, p.position(), SoundEvents.WARDEN_SONIC_BOOM, 2.0f, 0.4f);
		HeavenSpells.sound(level, p.position(), SoundEvents.ENDER_DRAGON_GROWL, 1.5f, 0.6f);
		HeavenSpells.sound(level, p.position(), SoundEvents.BEACON_POWER_SELECT, 2.0f, 0.5f);
		// The two wound into the caster (SpellConstruct HD_AS_ONE): a vortex of gold and crimson on the ground round them, a pillar of the two.
		SpellConstruct one = SpellConstruct.create(level, SpellConstruct.Kind.HD_AS_ONE, p.position(), p.getYRot(), 0, 40, p).spawn();
		HeavenSpells.linger(new FireSpells.Delayed(p, 30, one::dissolve));
		SpellFx.send(level, SpellFx.Kind.FLASH, HeavenSpells.middle(p), Vec3.ZERO, 2.0f, CRIMSON);
		float dealt = 0;
		for (LivingEntity target : HeavenSpells.around(p, ONE_RADIUS, e -> !HeavenSpells.spared(p, e))) {
			level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(), 1, 0, 0, 0, 0);
			Vec3 in = p.position().subtract(target.position());
			double distance = in.length();
			if (distance > 2) HeavenSpells.move(target, in.normalize().scale(Math.min(2.0, distance * 0.15)).add(0, 0.3, 0));
			dealt += strike(p, target, ONE_DAMAGE);
		}
		HeavenSpells.mend(p, dealt * ONE_MENDS);
		p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ONE_TICKS, 1));
	}

	// --- Dao Integration: Heaven-Demon Avatar ---

	/**
	 * Heaven-Demon Avatar: a colossal avatar of gold light and crimson demon qi (SpellConstruct HEAVEN_DEMON_AVATAR, its own GeckoLib model:
	 * heaven's side gold, haloed, an open palm; the demon's crimson, horned, clawed) rises out of a cloud of the two behind its caster and
	 * stands {@link #AVATAR_TICKS}, turning to where its caster looks. Twice ({@link #AVATAR_STRIKES}) it raises a hand and that hand comes
	 * down out of the sky where its caster looks (up to {@link #AVATAR_RANGE}; AVATAR_PALM, then AVATAR_CLAW): all within
	 * {@link #AVATAR_RADIUS} are struck for {@link #AVATAR_DAMAGE}, thrown down and slowed, and {@link #AVATAR_MENDS} of all of it mends the
	 * caster. The palm crushes a crater into the land, the claw rakes gouges through it ({@link #wound}; the user's ask, 2026-10-24: before,
	 * the land was left as it was). Its caster gone, it goes.
	 */
	public static final class Avatar extends HeavenSpells.Timed {
		private final Spell spell;
		private final SpellConstruct avatar;
		/** Every hand it has sent down (dissolved with it), and those still falling. */
		private final List<SpellConstruct> sent = new ArrayList<>();
		private final List<Hand> falling = new ArrayList<>();

		private record Hand(Vec3 ground, boolean palm, long lands) {}

		Avatar(LivingEntity caster, Spell spell) {
			super(caster, AVATAR_TICKS);
			this.spell = spell;
			Vec3 back = Vec3.directionFromRotation(0, caster.getYRot()).scale(-AVATAR_BEHIND);
			Vec3 behind = caster.position().add(back);
			Vec3 below = HeavenSpells.groundBelow(level, behind.add(0, 2, 0), 4, 24);
			Vec3 at = below != null ? below : behind.subtract(0, 6, 0); // aloft: on its own cloud, below its caster
			this.avatar = SpellConstruct.create(level, SpellConstruct.Kind.HEAVEN_DEMON_AVATAR, at, caster.getYRot(), 0, AVATAR_TICKS, caster).sizedFor(spell).spawn();
			HeavenSpells.sound(level, at, SoundEvents.BEACON_ACTIVATE, 3.0f, 0.5f);
			HeavenSpells.sound(level, at, SoundEvents.ENDER_DRAGON_GROWL, 2.5f, 0.6f);
			HeavenSpells.sound(level, at, SoundEvents.WARDEN_EMERGE, 2.0f, 0.8f);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, at, Vec3.ZERO, (float) AVATAR_RADIUS, 0, 192);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			turnTo(HeavenSpells.lookPoint(caster, AVATAR_RANGE));
			for (int i = 0; i < AVATAR_STRIKES.length; i++) if (age == AVATAR_STRIKES[i]) raise(i == 0, age);
			for (java.util.Iterator<Hand> it = falling.iterator(); it.hasNext(); ) {
				Hand hand = it.next();
				if (age < hand.lands()) continue;
				it.remove();
				land(hand);
			}
			return true;
		}

		/** Turns it a little toward {@code point} (its watchers' copies ease after it). */
		private void turnTo(Vec3 point) {
			Vec3 to = point.subtract(avatar.position());
			if (to.x * to.x + to.z * to.z < 1) return;
			float want = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
			float yaw = avatar.getYRot() + Mth.clamp(Mth.wrapDegrees(want - avatar.getYRot()), -AVATAR_TURN, AVATAR_TURN);
			avatar.place(avatar.position(), yaw, 0);
		}

		/** A hand raised (heaven's palm, or the demon's claw): it comes down where its caster looks, on the being looked at if any. */
		private void raise(boolean palm, long age) {
			LivingEntity aimed = HeavenSpells.aimed(caster, AVATAR_RANGE);
			Vec3 point = aimed != null ? aimed.position() : HeavenSpells.lookPoint(caster, AVATAR_RANGE);
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			Vec3 ground = below != null ? below : point;
			avatar.act(palm ? "strike_palm" : "strike_claw");
			float yaw = HeavenSpells.facing(ground.subtract(avatar.position()).multiply(1, 0, 1))[0];
			sent.add(SpellConstruct.create(level, palm ? SpellConstruct.Kind.AVATAR_PALM : SpellConstruct.Kind.AVATAR_CLAW, ground, yaw, 0, AVATAR_FALL + AVATAR_PRESS, caster)
					.sizedFor(spell).spawn());
			falling.add(new Hand(ground, palm, age + AVATAR_FALL));
			HeavenSpells.sound(level, avatar.position().add(0, AVATAR_HEIGHT * 0.7, 0), palm ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.RAVAGER_ROAR, 3.0f, 0.5f);
			HeavenSpells.sound(level, ground, SoundEvents.ELYTRA_FLYING, 2.0f, 0.6f);
		}

		/** A hand come down: all within its reach struck, thrown down and slowed; a share of all it did mends the caster. */
		private void land(Hand hand) {
			float dealt = 0;
			for (LivingEntity e : HeavenSpells.aroundPoint(level, hand.ground(), AVATAR_RADIUS, e -> !HeavenSpells.spared(caster, e))) {
				dealt += strike(caster, e, AVATAR_DAMAGE);
				e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, AVATAR_SLOWS, 2));
				if (!HeavenSpells.greatBeast(caster, e)) HeavenSpells.move(e, new Vec3(e.getDeltaMovement().x * 0.2, -1.6, e.getDeltaMovement().z * 0.2));
			}
			HeavenSpells.mend(caster, dealt * AVATAR_MENDS);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, hand.ground(), Vec3.ZERO, (float) AVATAR_RADIUS, 0, 192);
			SpellFx.send(level, SpellFx.Kind.FLASH, hand.ground().add(0, 2, 0), Vec3.ZERO, 5.0f, hand.palm() ? GOLD : CRIMSON, 192);
			HeavenSpells.ring(level, hand.ground().add(0, 0.3, 0), AVATAR_RADIUS, hand.palm() ? gold(2.5f) : crimson(2.5f), 72);
			if (!hand.palm()) rake(level, hand.ground().add(0, 1, 0), hand.ground().subtract(avatar.position()), AVATAR_RADIUS * 1.2, 5);
			wound(hand);
			HeavenSpells.sound(level, hand.ground(), SoundEvents.GENERIC_EXPLODE, 4.0f, 0.6f);
			HeavenSpells.sound(level, hand.ground(), hand.palm() ? SoundEvents.ANVIL_LAND : SoundEvents.RAVAGER_ATTACK, 3.0f, 0.5f);
		}

		/**
		 * The land under a hand come down (a player's: Devastation): heaven's palm crushes a crater with cracks running out of it; the
		 * demon's claw rakes {@link #AVATAR_GOUGES} gouges across the way it came down, its claws' marks, and a lesser crater where it struck.
		 */
		private void wound(Hand hand) {
			Vec3 at = hand.ground();
			if (hand.palm()) {
				Devastation.crater(caster, level, at, AVATAR_PALM_CRATER, AVATAR_PALM_DEPTH, Devastation.Floor.RUBBLE);
				Devastation.fissures(caster, level, at, AVATAR_PALM_CRATER * 2.2, 8, 6, AVATAR_PALM_CRATER * 0.8);
				return;
			}
			Vec3 way = at.subtract(avatar.position()).multiply(1, 0, 1);
			way = way.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : way.normalize();
			Vec3 side = new Vec3(-way.z, 0, way.x);
			for (int k = 0; k < AVATAR_GOUGES; k++) {
				Vec3 middle = at.add(way.scale((k - (AVATAR_GOUGES - 1) / 2.0) * (AVATAR_GOUGE_WIDTH + 2.5)));
				Devastation.gouge(caster, level, middle.subtract(side.scale(AVATAR_GOUGE / 2)), middle.add(side.scale(AVATAR_GOUGE / 2)), AVATAR_GOUGE_WIDTH, AVATAR_GOUGE_DEPTH);
			}
			Devastation.crater(caster, level, at, AVATAR_CLAW_CRATER, AVATAR_GOUGE_DEPTH * 0.6, Devastation.Floor.RUBBLE);
		}

		@Override
		public void end() {
			avatar.dissolve();
			for (SpellConstruct hand : sent) hand.dissolve();
		}
	}

	// --- Void Shattering: Heaven-Demon Void Shatter ---

	/**
	 * Heaven-Demon Void Shatter: a colossal fist forms over its caster's shoulder (SpellConstruct HEAVEN_DEMON_FIST, its own GeckoLib model:
	 * heaven's gauntlet of gold plates, a demon's crimson claws closed in it) and punches out to where they look (the being looked at, up to
	 * {@link #SHATTER_RANGE}; {@link #SHATTER_FLIGHT} ticks): where it lands space itself shatters like glass (SHATTERED_SPACE): everything
	 * within {@link #SHATTER_RADIUS} takes {@link #SHATTER_DAMAGE} and is thrown out, and the shattered space hangs there {@link #SHATTER_TICKS},
	 * cutting whoever stays in it for {@link #SHATTER_CUTS} a second. The land under it shatters too: a crater, cracks running out of it
	 * (the user's ask, 2026-10-24: before, the land was left as it was).
	 */
	public static final class Shatter extends HeavenSpells.Timed {
		private final Vec3 from, point;
		private final float[] face;
		private final SpellConstruct fist;
		private SpellConstruct shattered;

		Shatter(LivingEntity caster, Spell spell) {
			super(caster, SHATTER_FLIGHT + SHATTER_TICKS + SpellConstruct.FADE);
			LivingEntity aimed = HeavenSpells.aimed(caster, SHATTER_RANGE);
			this.point = aimed != null ? HeavenSpells.middle(aimed) : HeavenSpells.lookPoint(caster, SHATTER_RANGE);
			this.from = caster.getEyePosition().add(Vec3.directionFromRotation(0, caster.getYRot()).scale(-3)).add(0, 2.5, 0);
			this.face = HeavenSpells.facing(point.subtract(from));
			this.fist = SpellConstruct.create(level, SpellConstruct.Kind.HEAVEN_DEMON_FIST, from, face[0], face[1], SHATTER_FLIGHT + 16, caster).sizedFor(spell).spawn();
			HeavenSpells.sound(level, from, SoundEvents.BEACON_POWER_SELECT, 3.0f, 0.5f);
			HeavenSpells.sound(level, from, SoundEvents.RAVAGER_ROAR, 2.5f, 0.6f);
			HeavenSpells.sound(level, from, SoundEvents.ELYTRA_FLYING, 2.0f, 0.8f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age <= SHATTER_FLIGHT) {
				double f = age / (double) SHATTER_FLIGHT;
				Vec3 at = from.lerp(point, f * f);
				fist.place(at, face[0], face[1]);
				if (age % 2 == 0) twined(level, at, from.lerp(point, Math.max(0, f * f - 0.08)), 1);
				if (age == SHATTER_FLIGHT) impact();
				return true;
			}
			long hanging = age - SHATTER_FLIGHT;
			if (hanging < SHATTER_TICKS && hanging % 20 == 10) {
				for (LivingEntity e : HeavenSpells.aroundPoint(level, point, SHATTER_RADIUS, e -> !HeavenSpells.spared(caster, e))) {
					e.invulnerableTime = 0;
					strike(caster, e, SHATTER_CUTS);
					level.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 6, 0.3, 0.4, 0.3, 0.05);
				}
				HeavenSpells.sound(level, point, SoundEvents.AMETHYST_CLUSTER_BREAK, 3.0f, 0.5f);
			}
			return true;
		}

		/** The fist lands: everything near struck and thrown out, and space there shattered. */
		private void impact() {
			fist.act("impact");
			for (LivingEntity e : HeavenSpells.aroundPoint(level, point, SHATTER_RADIUS, e -> !HeavenSpells.spared(caster, e))) {
				strike(caster, e, SHATTER_DAMAGE);
				if (HeavenSpells.greatBeast(caster, e)) continue;
				Vec3 out = HeavenSpells.middle(e).subtract(point);
				out = out.lengthSqr() < 1.0e-4 ? new Vec3(0, 1, 0) : out.normalize();
				HeavenSpells.move(e, out.scale(2.2).add(0, 0.4, 0));
				HeavenSpells.safeFall(e, 80);
			}
			shattered = SpellConstruct.create(level, SpellConstruct.Kind.SHATTERED_SPACE, point, face[0], face[1], SHATTER_TICKS + SpellConstruct.FADE, caster)
					.size((float) SHATTER_SPACE).tint(GOLD).spawn();
			// The land shatters with the space over it (a player's: Devastation): a crater where it struck, cracks running out of it like a
			// pane's; struck into a hillside or a wall, there; landed high over the land, none.
			Vec3 land = level.getBlockState(BlockPos.containing(point)).isSolid() ? point : HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, (int) SHATTER_GROUND);
			if (land != null) {
				Devastation.crater(caster, level, land, SHATTER_CRATER, SHATTER_CRATER_DEPTH, Devastation.Floor.RUBBLE);
				Devastation.fissures(caster, level, land, SHATTER_CRACK_REACH, SHATTER_CRACKS, SHATTER_CRACK_DEPTH, SHATTER_CRATER * 0.8);
			}
			SpellFx.send(level, SpellFx.Kind.FLASH, point, Vec3.ZERO, 10, GOLD_PALE, 192);
			SpellFx.send(level, SpellFx.Kind.FLASH, point, Vec3.ZERO, 6, CRIMSON, 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, point, Vec3.ZERO, (float) SHATTER_RADIUS * 1.5f, 0, 192);
			HeavenSpells.sound(level, point, SoundEvents.GLASS_BREAK, 5.0f, 0.4f);
			HeavenSpells.sound(level, point, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.5f);
			HeavenSpells.sound(level, point, SoundEvents.ANVIL_LAND, 3.0f, 0.4f);
		}

		@Override
		public void end() {
			fist.dissolve();
			if (shattered != null) shattered.dissolve();
		}
	}

	// --- Sacred Vessel ---

	/**
	 * Heaven-Demon Bell: a colossal bell, heaven's gold on one side and the demon's crimson bronze on the other, its bosses and bands half sun
	 * and half fang (SpellConstruct HEAVEN_DEMON_BELL, its own GeckoLib model), falls out of the sky over where its caster looks
	 * ({@link #BELL_FALL}), its rim pressing a crater {@link #BELL_RADIUS} round into the land, and tolls {@link #BELL_TOLLS} times,
	 * {@link #BELL_EVERY} ticks apart: each toll strikes everything under it for {@link #BELL_TOLL}, through armour, and its wave runs out over
	 * the land round it to {@link #BELL_WAVE}, striking everything there for {@link #BELL_WAVE_DAMAGE} and throwing it back, the land cracking
	 * under it. {@link #BELL_MENDS} of all of it mends the caster. Then it rises away.
	 */
	public static final class Bell extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground, sky;
		private final SpellConstruct bell;
		private final long landed, risen;

		Bell(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, BELL_FALL + BELL_FIRST + (BELL_TOLLS - 1) * BELL_EVERY + 10 + BELL_RISE);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.sky = ground.add(0, BELL_HIGH, 0);
			this.landed = BELL_FALL;
			this.risen = BELL_FALL + BELL_FIRST + (BELL_TOLLS - 1) * BELL_EVERY + 10;
			this.bell = SpellConstruct.create(level, SpellConstruct.Kind.HEAVEN_DEMON_BELL, sky, caster.getYRot(), 0, (int) (end - start), caster).sizedFor(spell).spawn();
			HeavenSpells.sound(level, sky, SoundEvents.BELL_RESONATE, 4.0f, 0.3f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age <= landed) {
				double f = age / (double) BELL_FALL;
				bell.place(sky.lerp(ground, f * f), bell.getYRot(), 0);
				if (age == landed) land();
				return true;
			}
			long since = age - landed - BELL_FIRST;
			if (since >= 0 && since % BELL_EVERY == 0 && since / BELL_EVERY < BELL_TOLLS) toll();
			if (age > risen) {
				double f = (age - risen) / (double) BELL_RISE;
				bell.place(ground.add(0, BELL_HIGH * 0.5 * f * f, 0), bell.getYRot(), 0);
			}
			return true;
		}

		/** It lands: its rim pressed into the land. */
		private void land() {
			Devastation.crater(caster, level, ground, BELL_CRATER, BELL_CRATER_DEPTH, Devastation.Floor.RUBBLE);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) BELL_RADIUS * 1.5f, 0, 192);
			HeavenSpells.sound(level, ground, SoundEvents.ANVIL_LAND, 5.0f, 0.3f);
			HeavenSpells.sound(level, ground, SoundEvents.BELL_BLOCK, 5.0f, 0.3f);
		}

		/** It tolls: everything under it struck through armour, its wave striking and throwing back all round it, the land cracking. */
		private void toll() {
			bell.act("toll");
			float dealt = 0;
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, BELL_WAVE + 10, e -> !HeavenSpells.spared(caster, e))) {
				double d = Math.hypot(e.getX() - ground.x, e.getZ() - ground.z);
				if (d > BELL_WAVE || e.getY() > ground.y + 24 || e.getY() < ground.y - 10) continue;
				e.invulnerableTime = 0;
				if (d <= BELL_RADIUS) {
					dealt += strike(caster, e, BELL_TOLL);
					HeavenSpells.still(e);
				} else {
					dealt += strike(caster, e, BELL_WAVE_DAMAGE);
					HeavenSpells.push(ground, e, 1.6);
				}
			}
			HeavenSpells.mend(caster, dealt * BELL_MENDS);
			Devastation.fissures(caster, level, ground, BELL_CRACKS, 6, 4, BELL_RADIUS);
			SpellFx.send(level, SpellFx.Kind.BELL_TOLL, ground.add(0, BELL_RADIUS, 0), Vec3.ZERO, 40, 0, 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) BELL_WAVE, 0, 192);
			SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, BELL_RADIUS * 0.6, 0), Vec3.ZERO, 10, GOLD, 192);
			SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, BELL_RADIUS * 0.4, 0), Vec3.ZERO, 8, CRIMSON, 192);
			HeavenSpells.sound(level, ground, SoundEvents.BELL_BLOCK, 6.0f, 0.4f);
			HeavenSpells.sound(level, ground, SoundEvents.BELL_RESONATE, 5.0f, 0.5f);
		}

		@Override
		public void end() {
			bell.dissolve();
		}
	}

	/**
	 * Heaven-Demon Eclipse: heaven's sun and the demon's moon (SpellConstruct HEAVEN_DEMON_ECLIPSE, its own GeckoLib model) come together
	 * {@link #ECLIPSE_HIGH} over where its caster looks ({@link #ECLIPSE_MEETS}), the moon passing before the sun, its corona blazing round them;
	 * a beam of darkness pours down from them and pulses three times, {@link #ECLIPSE_EVERY} ticks apart: each strikes everything under it,
	 * within {@link #ECLIPSE_RADIUS}, for {@link #ECLIPSE_PULSES}, through armour, holding it where it stands, and the first burns the land
	 * into a crater of black glass. {@link #ECLIPSE_MENDS} of all of it mends its caster.
	 */
	public static final class Eclipse extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground, sky;
		private final SpellConstruct eclipse;

		Eclipse(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, ECLIPSE_MEETS + (ECLIPSE_PULSES.length - 1) * ECLIPSE_EVERY + ECLIPSE_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.sky = ground.add(0, ECLIPSE_HIGH, 0);
			this.eclipse = SpellConstruct.create(level, SpellConstruct.Kind.HEAVEN_DEMON_ECLIPSE, sky, caster.getYRot(), 0, (int) (end - start), caster).sizedFor(spell).spawn();
			HeavenSpells.sound(level, sky, SoundEvents.BEACON_ACTIVATE, 4.0f, 0.4f);
			HeavenSpells.sound(level, sky, SoundEvents.WITHER_AMBIENT, 3.0f, 0.4f);
		}

		@Override
		protected boolean run(long age, long now) {
			long since = age - ECLIPSE_MEETS;
			if (since >= 0 && since % ECLIPSE_EVERY == 0 && since / ECLIPSE_EVERY < ECLIPSE_PULSES.length) pulse((int) (since / ECLIPSE_EVERY));
			return true;
		}

		/** Its {@code k}th pulse: everything under it struck through armour and held; the first burns the land to black glass. */
		private void pulse(int k) {
			if (k > 0) eclipse.act("pulse");
			float dealt = 0;
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, ECLIPSE_RADIUS + 12, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > ECLIPSE_RADIUS || e.getY() > sky.y || e.getY() < ground.y - 10) continue;
				e.invulnerableTime = 0;
				dealt += strike(caster, e, ECLIPSE_PULSES[k]);
				HeavenSpells.still(e);
			}
			HeavenSpells.mend(caster, dealt * ECLIPSE_MENDS);
			if (k == 0) Devastation.crater(caster, level, ground, ECLIPSE_CRATER, ECLIPSE_CRATER_DEPTH, Devastation.Floor.GLASSY);
			SpellFx.send(level, SpellFx.Kind.STREAK, sky, ground.subtract(sky), 0, CRIMSON, 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) ECLIPSE_RADIUS * 1.4f, 0, 192);
			SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, 3, 0), Vec3.ZERO, 10, CRIMSON, 192);
			SpellFx.send(level, SpellFx.Kind.FLASH, sky, Vec3.ZERO, 14, GOLD, 192);
			HeavenSpells.sound(level, ground, SoundEvents.WARDEN_SONIC_BOOM, 4.0f, 0.4f + 0.1f * k);
			HeavenSpells.sound(level, ground, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.5f);
		}

		@Override
		public void end() {
			eclipse.dissolve();
		}
	}

	/**
	 * Primordial Chaos: a sphere of the chaos before heaven and earth were parted, heaven's gold and the demon's crimson swirled together, two
	 * orbits of their light wheeling against each other round it (SpellConstruct CHAOS_SPHERE, its own GeckoLib model), swells out of a point
	 * {@link #CHAOS_HIGH} over where its caster looks ({@link #CHAOS_SWELLS}) and churns for {@link #CHAOS_TICKS}: everything within
	 * {@link #CHAOS_PULL} is drawn in toward it (a great beast slowly), and every {@link #CHAOS_EVERY} ticks everything within {@link #CHAOS_RADIUS}
	 * of its heart takes {@link #CHAOS_CHURN}, through armour. Then it bursts ({@link #CHAOS_BURSTS}): {@link #CHAOS_BURST} to everything within
	 * {@link #CHAOS_BURST_RADIUS}, thrown out, the land under it burnt into a crater of glass. {@link #CHAOS_MENDS} of all of it mends its caster.
	 */
	public static final class Chaos extends HeavenSpells.Timed {
		private final Vec3 ground, heart;
		private final SpellConstruct sphere;
		private float dealt;

		Chaos(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, CHAOS_SWELLS + CHAOS_TICKS + CHAOS_BURSTS + CHAOS_AFTER);
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.heart = ground.add(0, CHAOS_HIGH, 0);
			this.sphere = SpellConstruct.create(level, SpellConstruct.Kind.CHAOS_SPHERE, heart, caster.getYRot(), 0, (int) (end - start), caster).sizedFor(spell).spawn();
			HeavenSpells.sound(level, heart, SoundEvents.BEACON_ACTIVATE, 5.0f, 0.3f);
			HeavenSpells.sound(level, heart, SoundEvents.WITHER_AMBIENT, 4.0f, 0.3f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < CHAOS_SWELLS) return true;
			long churning = age - CHAOS_SWELLS;
			if (churning < CHAOS_TICKS) {
				draw(churning % CHAOS_EVERY == 0);
				if (churning % 20 == 0) HeavenSpells.sound(level, heart, SoundEvents.WARDEN_HEARTBEAT, 5.0f, 0.4f);
				return true;
			}
			if (churning == CHAOS_TICKS) {
				sphere.act("burst");
				HeavenSpells.sound(level, heart, SoundEvents.WARDEN_SONIC_CHARGE, 5.0f, 0.5f);
			}
			if (churning == CHAOS_TICKS + CHAOS_BURSTS) burst();
			return true;
		}

		/** A tick of its churning: everything near drawn in toward it; what it has drawn churned through armour if {@code churn}. */
		private void draw(boolean churn) {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, heart, CHAOS_PULL, e -> !HeavenSpells.spared(caster, e))) {
				Vec3 to = heart.subtract(HeavenSpells.middle(e));
				double d = to.length();
				if (d > 4) {
					double pull = HeavenSpells.greatBeast(caster, e) ? 0.1 : 0.4;
					HeavenSpells.move(e, to.scale(pull / d));
					HeavenSpells.safeFall(e, 60);
				}
				if (churn && d <= CHAOS_RADIUS) {
					e.invulnerableTime = 0;
					dealt += strike(caster, e, CHAOS_CHURN);
					HeavenSpells.particles(level, ParticleTypes.CRIMSON_SPORE, HeavenSpells.middle(e), 10, 0.5, 0.05);
				}
			}
			if (churn) HeavenSpells.line(level, heart, ground, ParticleTypes.END_ROD, 3);
		}

		/** It bursts: everything within struck through armour and thrown out, the land burnt into a crater of glass, a share mending its caster. */
		private void burst() {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, CHAOS_BURST_RADIUS + CHAOS_HIGH, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > CHAOS_BURST_RADIUS || e.getY() > heart.y + CHAOS_RADIUS || e.getY() < ground.y - 10) continue;
				e.invulnerableTime = 0;
				dealt += strike(caster, e, CHAOS_BURST);
				if (!HeavenSpells.greatBeast(caster, e)) HeavenSpells.move(e, HeavenSpells.middle(e).subtract(heart).normalize().scale(2.4).add(0, 0.6, 0));
				HeavenSpells.safeFall(e, 120);
			}
			HeavenSpells.mend(caster, dealt * CHAOS_MENDS);
			Devastation.crater(caster, level, ground, CHAOS_CRATER, CHAOS_CRATER_DEPTH, Devastation.Floor.GLASSY);
			SpellFx.send(level, SpellFx.Kind.FLASH, heart, Vec3.ZERO, 22, GOLD, 256);
			SpellFx.send(level, SpellFx.Kind.FLASH, heart, Vec3.ZERO, 16, CRIMSON, 256);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) CHAOS_BURST_RADIUS * 1.6f, 0, 256);
			SpellFx.send(level, SpellFx.Kind.FIRE_EXPLOSION, heart, Vec3.ZERO, (float) CHAOS_RADIUS, 0, 256);
			HeavenSpells.sound(level, heart, SoundEvents.GENERIC_EXPLODE, 7.0f, 0.4f);
			HeavenSpells.sound(level, heart, SoundEvents.WARDEN_SONIC_BOOM, 6.0f, 0.4f);
		}

		@Override
		public void end() {
			sphere.dissolve();
		}
	}

	private HeavenDemonSpells() {}
}
