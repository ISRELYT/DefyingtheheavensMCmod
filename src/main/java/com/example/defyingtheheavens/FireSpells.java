package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The fire methods' twelve spells (see HeavenSpells for what they share): an ember that sleeps in a being and bursts, a flash
 * that dazzles those looking on, a gallop that leaves burning hoofprints, a body sealed like a furnace that refines harm into qi,
 * a spark that spreads like a prairie fire, a lotus of fire that blooms and scatters its petals, a talon whose burn bars all
 * healing, the Vermilion Bird's plunge, the light of first dawn that burns away every ill, the golden crow's noonday sun, three
 * firestorms that hunt, and the phoenix that rises from its own ashes.
 */
public final class FireSpells {
	static final int FIRE = 0xFF4020, DEEP_FIRE = 0xFF1A00, ORANGE = 0xFF8C1A, GOLD = 0xFFD24A, PALE_GOLD = 0xFFE08A, SPARK = 0xFFE070,
			CRIMSON = 0xC81E1E, ASH = 0x5A5A5A, DAWN = 0xFFE6A0, REFINED = 0xFFA030;

	/** Sleeping Ember: reach, the fuse, harm to the marked and round it, how far, the burning, at most how many marks a caster keeps. */
	static final double EMBER_RANGE = 16, EMBER_SPLASH = 2.5;
	static final int EMBER_FUSE = 60, EMBER_MARKS = 2, EMBER_BURN = 3, EMBER_SPLASH_BURN = 2;
	static final float EMBER_HARM = 7, EMBER_SPLASH_HARM = 3;
	/** Flash of Struck Flint: reach, how near a foe's gaze must be (cosine of 40 degrees), the blinding, the creatures' lost sight. */
	static final double FLINT_RANGE = 12, FLINT_GAZE = 0.766;
	static final int FLINT_BLIND_TICKS = 80, FLINT_DAZZLE_TICKS = 60;
	/** Fire Horse Gallop: how long, how long a print burns, at most how many, the burn, the strikes and how often; the step between. */
	static final int GALLOP_TICKS = 120, PRINT_TICKS = 60, PRINTS_MAX = 40, PRINT_BURN = 3, PRINT_STRIKES = 3, PRINT_EVERY = 20;
	static final double PRINT_STEP = 1.0;
	static final float PRINT_HARM = 2;
	/** Sealed Furnace Refinement: how long, the share of harm let through, the qi refined from each point held back, the most. */
	static final int FURNACE_TICKS = 240;
	static final float FURNACE_KEEPS = 0.65f;
	static final double FURNACE_QI_PER_POINT = 3, FURNACE_QI_MAX = 150;
	/** Prairie-Burning Spark: reach, the first burning, the caught's, the harm each second, the leap's reach and count, the most it catches. */
	static final double SPARK_RANGE = 24, SPARK_LEAP = 4;
	static final int SPARK_TICKS = 160, SPARK_CAUGHT_TICKS = 100, SPARK_LEAPS = 2, SPARK_MOST = 12;
	static final float SPARK_HARM = 2.5f;
	/** Crimson Lotus Blooms: reach, the bud's wait, the heart's reach and harm, the petals (count, reach, speed, harm, burning). */
	static final double LOTUS_RANGE = 24, LOTUS_HEART = 2, PETAL_REACH = 12, PETAL_SPEED = 1.2;
	static final int LOTUS_BUD_TICKS = 30, PETALS = 12, PETAL_BURN = 4;
	static final float LOTUS_HEART_HARM = 20, PETAL_HARM = 14;
	/** Ash-Searing Talon: reach, harm, the burning, how long the wound stays shut. */
	static final double TALON_RANGE = 28;
	static final float TALON_HARM = 36;
	static final int TALON_BURN = 6, SEARED_TICKS = 200;
	/** Vermilion Bird Plunge: the soar, the landing's reach, the soar's and glide's longest, the burning, the clear sky needed; harm and most. */
	static final double PLUNGE_SOAR = 1.9, PLUNGE_REACH = 6;
	static final int PLUNGE_SOAR_TICKS = 30, PLUNGE_GLIDE_TICKS = 120, PLUNGE_BURN = 5, PLUNGE_SKY = 3;
	static final float PLUNGE_HARM = 22, PLUNGE_MAX = 30;
	/** Light of First Dawn: reach, the mending for each ill burned away, the ward after. */
	static final double DAWN_RADIUS = 20;
	static final float DAWN_MEND = 6;
	static final int DAWN_WARD_TICKS = 120;
	/** Noonday Golden Crow: how high the sun hangs, its reach, how long, the burning and glow, the harm (the undead's). */
	static final double CROW_HEIGHT = 7, CROW_RADIUS = 24;
	static final int CROW_TICKS = 240, CROW_BURN = 3, CROW_GLOW_TICKS = 40;
	static final float CROW_HARM = 4, CROW_UNDEAD_HARM = 6;
	/** Three Heaven-Scorching Firestorms: how many, how long, the burning; reach, leash, size, speed; harm each half second. */
	static final int STORMS = 3, STORM_TICKS = 160, STORM_BURN = 4;
	static final double STORM_RANGE = 32, STORM_LEASH = 40, STORM_RADIUS = 2, STORM_HEIGHT = 7, STORM_SPEED = 0.45;
	static final float STORM_HARM = 7;
	/** Undying Phoenix: how long the wreath, the ash's wait, the boons, the burning, the cooldown left if unused; the blast's reach and harm. */
	static final int PHOENIX_TICKS = 1200, PHOENIX_ASH_TICKS = 40, PHOENIX_BOON_TICKS = 300, PHOENIX_BURN = 8, PHOENIX_UNUSED_COOLDOWN = 600;
	static final double PHOENIX_RADIUS = 16;
	static final float PHOENIX_HARM = 80;

	/** Whether {@code spell} can take hold for {@code p} now; refused, nothing is paid. */
	static boolean canCast(ServerPlayer p, Spell spell) {
		switch (spell) {
			case SLEEPING_EMBER -> {
				return HeavenSpells.aimed(p, EMBER_RANGE) != null || HeavenSpells.refuse(p);
			}
			case PRAIRIE_BURNING_SPARK -> {
				return HeavenSpells.aimed(p, SPARK_RANGE) != null || HeavenSpells.refuse(p);
			}
			case CRIMSON_LOTUS_BLOOMS -> {
				return HeavenSpells.lookedBlock(p, LOTUS_RANGE) != null || HeavenSpells.refuse(p);
			}
			case ASH_SEARING_TALON -> {
				return HeavenSpells.aimed(p, TALON_RANGE) != null || HeavenSpells.refuse(p);
			}
			case VERMILION_BIRD_PLUNGE -> {
				for (int dy = 0; dy < PLUNGE_SKY; dy++) {
					BlockPos pos = BlockPos.containing(p.getX(), p.getEyeY() + 0.5 + dy, p.getZ());
					if (!p.level().getBlockState(pos).getCollisionShape(p.level(), pos).isEmpty()) {
						p.displayClientMessage(Component.translatable(ModLang.MSG_NO_SKY), true);
						return false;
					}
				}
				return true;
			}
			default -> {
				return true;
			}
		}
	}

	/** Casts one of the fire spells (paid for). */
	static void cast(ServerPlayer p, Spell spell) {
		switch (spell) {
			case SLEEPING_EMBER -> ember(p);
			case STRUCK_FLINT_FLASH -> flint(p);
			case FIRE_HORSE_GALLOP -> gallop(p);
			case SEALED_FURNACE_REFINEMENT -> furnace(p);
			case PRAIRIE_BURNING_SPARK -> spark(p);
			case CRIMSON_LOTUS_BLOOMS -> lotus(p);
			case ASH_SEARING_TALON -> talon(p);
			case VERMILION_BIRD_PLUNGE -> plunge(p);
			case LIGHT_OF_FIRST_DAWN -> dawn(p);
			case NOONDAY_GOLDEN_CROW -> crow(p);
			case HEAVEN_SCORCHING_FIRESTORMS -> firestorms(p);
			case UNDYING_PHOENIX -> phoenix(p, spell);
			default -> { }
		}
	}

	/** Every server tick (what lingers is ticked by HeavenSpells). */
	static void tick(MinecraftServer server) {
	}

	/** A player left. */
	static void forget(UUID id) {
	}

	/** The server stopping. */
	static void clear() {
	}

	// --- Shared ---

	/** Whether {@code effect} is one of vanilla's harmful effects (the mod's own states, a suppression or a binding, are never touched). */
	static boolean harmful(MobEffectInstance effect) {
		if (effect.getEffect().getCategory() != MobEffectCategory.HARMFUL) return false;
		ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect());
		return id != null && "minecraft".equals(id.getNamespace());
	}

	/** Burns away every harmful effect on {@code e} (vanilla's), and returns how many. */
	static int burnAwayIlls(LivingEntity e) {
		int n = 0;
		for (MobEffectInstance effect : new ArrayList<>(e.getActiveEffects())) {
			if (!harmful(effect)) continue;
			e.removeEffect(effect.getEffect());
			n++;
		}
		return n;
	}

	/** Clears every fire spell's mark on {@code e}: a sleeping ember fizzles unburst, a crimson fire goes out, a seared wound opens. */
	static void cleanse(LivingEntity e) {
		for (Ember ember : HeavenSpells.lingering(null, Ember.class)) {
			if (ember.target == e) {
				ember.done = true;
				ember.end = 0;
			}
		}
		for (Spark spark : HeavenSpells.lingering(null, Spark.class)) spark.burning.remove(e);
		for (Seared seared : HeavenSpells.lingering(null, Seared.class)) if (seared.target == e) seared.end = 0;
	}

	/** An arc of {@code rgb} dust from {@code a} to {@code b}, rising a block at its middle. */
	private static void leap(ServerLevel level, Vec3 a, Vec3 b, int rgb) {
		for (int i = 0; i <= 12; i++) {
			double t = i / 12.0;
			Vec3 at = a.add(b.subtract(a).scale(t)).add(0, Math.sin(t * Math.PI), 0);
			level.sendParticles(HeavenSpells.dust(rgb, 1.0f), at.x, at.y, at.z, 1, 0, 0, 0, 0);
		}
	}

	// --- Sleeping Ember ---

	/** Sleeping Ember: an ember flicked into the first being looked at, that sleeps a while and then bursts. */
	private static void ember(ServerPlayer p) {
		LivingEntity target = HeavenSpells.aimed(p, EMBER_RANGE);
		if (target == null) return;
		ServerLevel level = p.serverLevel();
		HeavenSpells.line(level, HeavenSpells.fromHands(p), HeavenSpells.middle(target), ParticleTypes.SMALL_FLAME, 0.4);
		HeavenSpells.sound(level, p.position(), SoundEvents.FLINTANDSTEEL_USE, 1.0f, 1.2f);
		List<Ember> mine = HeavenSpells.lingering(p.getUUID(), Ember.class);
		for (Ember old : mine) {
			if (old.target == target) {
				old.end = level.getGameTime() + EMBER_FUSE; // already lodged: its fuse begins again
				return;
			}
		}
		if (mine.size() >= EMBER_MARKS) mine.get(0).burstNow(); // a third: the oldest wakes at once
		HeavenSpells.linger(new Ember(p, target));
	}

	/** An ember lodged in a being, smouldering until its fuse runs out (or the caster strikes it), then bursting. */
	static final class Ember extends HeavenSpells.Timed implements HeavenSpells.MeleeListener {
		final LivingEntity target;
		Vec3 last;
		boolean done;

		Ember(ServerPlayer caster, LivingEntity target) {
			super(caster, EMBER_FUSE);
			this.target = target;
			this.last = HeavenSpells.middle(target);
		}

		@Override
		protected boolean run(long age, long now) {
			if (done) return false;
			if (!HeavenSpells.present(target, level)) return false; // bursts where it last was, without the marked one's harm
			last = HeavenSpells.middle(target);
			if (age % 4 == 0) {
				level.sendParticles(ParticleTypes.SMOKE, last.x, last.y + 0.2, last.z, 1, 0.1, 0.1, 0.1, 0);
				level.sendParticles(HeavenSpells.dust(FIRE, 0.8f), last.x, last.y + 0.2, last.z, 1, 0.1, 0.1, 0.1, 0);
			}
			if (age % 20 == 0) {
				level.sendParticles(ParticleTypes.LAVA, last.x, last.y, last.z, 1, 0, 0, 0, 0);
				HeavenSpells.sound(level, last, SoundEvents.CAMPFIRE_CRACKLE, 0.8f, 1.0f);
			}
			return true;
		}

		void burstNow() {
			if (done) return;
			end = 0;
			burst();
		}

		@Override
		public void onMelee(ServerPlayer attacker, LivingEntity struck) {
			if (attacker == caster && struck == target) burstNow();
		}

		@Override
		public void end() {
			burst();
		}

		private void burst() {
			if (done) return;
			done = true;
			boolean there = HeavenSpells.present(target, level);
			Vec3 at = there ? HeavenSpells.middle(target) : last;
			if (there && target.isUnderWater()) {
				HeavenSpells.particles(level, ParticleTypes.SMOKE, at, 8, 0.2, 0.02);
				HeavenSpells.particles(level, ParticleTypes.CLOUD, at, 4, 0.2, 0.02);
				HeavenSpells.sound(level, at, SoundEvents.FIRE_EXTINGUISH, 1.0f, 1.0f);
				return;
			}
			if (there) {
				HeavenSpells.strikeAgain(caster, target, EMBER_HARM, QiElement.FIRE);
				target.setSecondsOnFire(EMBER_BURN);
			}
			for (LivingEntity e : HeavenSpells.aroundPoint(level, at, EMBER_SPLASH + 1, e -> e != target && !HeavenSpells.spared(caster, e)
					&& HeavenSpells.middle(e).distanceTo(at) <= EMBER_SPLASH)) {
				HeavenSpells.strikeAgain(caster, e, EMBER_SPLASH_HARM, QiElement.FIRE);
				e.setSecondsOnFire(EMBER_SPLASH_BURN);
			}
			HeavenSpells.particles(level, ParticleTypes.EXPLOSION, at, 1, 0, 0);
			HeavenSpells.particles(level, ParticleTypes.FLAME, at, 16, 0.2, 0.15);
			HeavenSpells.particles(level, ParticleTypes.LAVA, at, 6, 0.3, 0);
			HeavenSpells.particles(level, HeavenSpells.dust(DEEP_FIRE, 1.5f), at, 10, EMBER_SPLASH / 2, 0);
			HeavenSpells.sound(level, at, SoundEvents.FIREWORK_ROCKET_BLAST, 1.0f, 0.8f);
			HeavenSpells.sound(level, at, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.6f);
		}
	}

	// --- Flash of Struck Flint ---

	/** Flash of Struck Flint: a flash from the palms that blinds every foe looking at the caster, and makes creatures lose sight of it. */
	private static void flint(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		Vec3 eye = p.getEyePosition();
		Dazzle dazzle = new Dazzle(p);
		for (LivingEntity foe : HeavenSpells.aroundPoint(level, eye, FLINT_RANGE, e -> HeavenSpells.foe(p, e))) {
			Vec3 toCaster = eye.subtract(foe.getEyePosition()).normalize();
			if (foe.getViewVector(1.0f).dot(toCaster) < FLINT_GAZE || !foe.hasLineOfSight(p)) continue;
			foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FLINT_BLIND_TICKS, 0));
			if (foe instanceof EnderDragon || foe instanceof WitherBoss || foe instanceof Warden) continue; // the slow alone
			foe.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, FLINT_BLIND_TICKS, 0));
			if (foe instanceof Mob mob) {
				if (mob.getTarget() == p) {
					mob.setTarget(null);
					mob.getNavigation().stop();
				}
				dazzle.dazzled.add(mob);
			}
			HeavenSpells.particles(level, HeavenSpells.dust(0xFFF4C0, 1.0f), foe.getEyePosition(), 6, 0.2, 0);
			HeavenSpells.particles(level, ParticleTypes.SMOKE, foe.getEyePosition(), 3, 0.2, 0.01);
			HeavenSpells.sound(level, foe.position(), SoundEvents.FIRE_EXTINGUISH, 0.3f, 1.8f);
		}
		if (!dazzle.dazzled.isEmpty()) HeavenSpells.linger(dazzle);
		Vec3 flash = eye.add(p.getViewVector(1.0f).scale(0.5));
		HeavenSpells.particles(level, ParticleTypes.FLASH, flash, 1, 0, 0);
		HeavenSpells.particles(level, ParticleTypes.END_ROD, flash, 24, 0.1, 0.3);
		HeavenSpells.ring(level, p.position().add(0, 1.2, 0), 1.5, HeavenSpells.dust(PALE_GOLD, 1.2f), 32);
		HeavenSpells.particles(level, ParticleTypes.SMALL_FLAME, HeavenSpells.fromHands(p), 8, 0.2, 0.05);
		HeavenSpells.sound(level, p.position(), SoundEvents.FLINTANDSTEEL_USE, 1.0f, 0.7f);
		HeavenSpells.sound(level, p.position(), SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.0f, 1.4f);
	}

	/** The dazzled creatures: for a while each loses the caster again whenever it finds it. */
	static final class Dazzle extends HeavenSpells.Timed {
		final Set<Mob> dazzled = new HashSet<>();

		Dazzle(ServerPlayer caster) {
			super(caster, FLINT_DAZZLE_TICKS);
		}

		@Override
		protected boolean run(long age, long now) {
			for (Mob mob : dazzled) {
				if (mob.isAlive() && mob.getTarget() == caster) {
					mob.setTarget(null);
					mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
					mob.getNavigation().stop();
				}
			}
			return true;
		}
	}

	// --- Fire Horse Gallop ---

	/** Fire Horse Gallop: the caster runs like a fire horse, its hoofprints burning whoever treads on them. */
	private static void gallop(ServerPlayer p) {
		HeavenSpells.endLingering(p.getUUID(), Gallop.class);
		p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, GALLOP_TICKS, 2));
		p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, GALLOP_TICKS, 1));
		HeavenSpells.linger(new Gallop(p));
		ServerLevel level = p.serverLevel();
		HeavenSpells.particles(level, ParticleTypes.FLAME, p.position().add(0, 0.4, 0), 20, 0.3, 0.05);
		HeavenSpells.particles(level, ParticleTypes.LAVA, p.position().add(0, 0.4, 0), 6, 0.3, 0);
		HeavenSpells.sound(level, p.position(), SoundEvents.HORSE_ANGRY, 1.0f, 1.3f);
		HeavenSpells.sound(level, p.position(), SoundEvents.FIRECHARGE_USE, 1.0f, 1.0f);
	}

	/** The gallop: the prints it leaves (x, y, z, until when), and who each has burned (how often, when last). */
	static final class Gallop extends HeavenSpells.Timed {
		final List<double[]> prints = new ArrayList<>();
		final Map<LivingEntity, long[]> burned = new HashMap<>();
		Vec3 lastPrint;
		boolean quenched;

		Gallop(ServerPlayer caster) {
			super(caster, GALLOP_TICKS + PRINT_TICKS);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!caster.isAlive() || caster.level() != level) return false;
			boolean galloping = age < GALLOP_TICKS && !quenched;
			if (galloping && caster.isInWater()) {
				quenched = true;
				galloping = false;
				caster.removeEffect(MobEffects.MOVEMENT_SPEED);
				caster.removeEffect(MobEffects.DIG_SPEED);
				HeavenSpells.sound(level, caster.position(), SoundEvents.FIRE_EXTINGUISH, 0.6f, 1.0f);
			}
			if (galloping) {
				Vec3 ground = HeavenSpells.groundBelow(level, caster.position().add(0, 0.01, 0), 0, 2);
				if (ground != null && caster.getY() - ground.y <= 1.2 && (lastPrint == null
						|| Math.hypot(caster.getX() - lastPrint.x, caster.getZ() - lastPrint.z) >= PRINT_STEP)) {
					lastPrint = caster.position();
					prints.add(new double[]{ground.x, ground.y + 0.05, ground.z, now + PRINT_TICKS});
					while (prints.size() > PRINTS_MAX) prints.remove(0);
				}
				if (age % 10 == 0) level.sendParticles(ParticleTypes.LAVA, caster.getX(), caster.getY() + 0.2, caster.getZ(), 1, 0, 0, 0, 0);
				if (age % 8 == 0 && caster.getDeltaMovement().horizontalDistanceSqr() > 0.001) HeavenSpells.sound(level, caster.position(), SoundEvents.HORSE_GALLOP, 0.5f, 1.0f);
			}
			prints.removeIf(print -> now > print[3]);
			for (double[] print : prints) {
				AABB box = new AABB(print[0] - 0.6, print[1], print[2] - 0.6, print[0] + 0.6, print[1] + 1.0, print[2] + 0.6);
				for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> !HeavenSpells.spared(caster, e))) {
					long[] seen = burned.computeIfAbsent(e, x -> new long[]{0, -PRINT_EVERY});
					if (seen[0] >= PRINT_STRIKES || now - seen[1] < PRINT_EVERY) continue;
					seen[0]++;
					seen[1] = now;
					HeavenSpells.strikeAgain(caster, e, PRINT_HARM, QiElement.FIRE);
					e.setSecondsOnFire(PRINT_BURN);
					level.sendParticles(ParticleTypes.FLAME, e.getX(), e.getY() + 0.1, e.getZ(), 4, 0.2, 0.05, 0.2, 0.01);
					HeavenSpells.sound(level, e.position(), SoundEvents.GENERIC_BURN, 0.8f, 1.0f);
				}
				if (age % 4 == 0) {
					if (print[3] - now <= 20) {
						level.sendParticles(ParticleTypes.SMOKE, print[0], print[1], print[2], 1, 0.1, 0, 0.1, 0);
					} else {
						level.sendParticles(ParticleTypes.FLAME, print[0], print[1], print[2], 2, 0.15, 0, 0.15, 0.005);
						level.sendParticles(ParticleTypes.SMALL_FLAME, print[0], print[1], print[2], 1, 0.15, 0, 0.15, 0.005);
						level.sendParticles(HeavenSpells.dust(FIRE, 1.0f), print[0], print[1], print[2], 1, 0.1, 0, 0.1, 0);
					}
				}
			}
			return galloping || !prints.isEmpty();
		}

		/** How many prints burn now (tests). */
		int prints() {
			return prints.size();
		}

		@Override
		public void end() {
			if (caster.isAlive()) HeavenSpells.sound(level, caster.position(), SoundEvents.FIRE_EXTINGUISH, 0.4f, 1.0f);
		}
	}

	// --- Sealed Furnace Refinement ---

	/** Sealed Furnace Refinement: the caster's body sealed like a furnace, harm held back and refined into qi. */
	private static void furnace(ServerPlayer p) {
		HeavenSpells.endLingering(p.getUUID(), Furnace.class);
		HeavenSpells.linger(new Furnace(p));
		ServerLevel level = p.serverLevel();
		HeavenSpells.sound(level, p.position(), SoundEvents.BLASTFURNACE_FIRE_CRACKLE, 1.0f, 1.0f);
		HeavenSpells.sound(level, p.position(), SoundEvents.FIRECHARGE_USE, 1.0f, 0.6f);
	}

	/** The sealed furnace: each blow on the caster cut (fire's whole), what it held back refined into qi, up to its most. */
	static final class Furnace extends HeavenSpells.Timed implements HeavenSpells.HarmListener {
		double refined;

		Furnace(ServerPlayer caster) {
			super(caster, FURNACE_TICKS);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			if (age % 10 == 0) {
				for (int i = 0; i < 10; i++) {
					double a = Math.PI * 2 * i / 10 + age * 0.05;
					level.sendParticles(ParticleTypes.FLAME, caster.getX() + Math.cos(a) * 0.75, caster.getY() + 1.0, caster.getZ() + Math.sin(a) * 0.75, 1, 0, 0, 0, 0);
				}
			}
			if (age % 20 == 0) level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, caster.getX(), caster.getY() + 2.1, caster.getZ(), 0, 0, 1, 0, 0.03);
			if (age % 40 == 0) HeavenSpells.sound(level, caster.position(), SoundEvents.FURNACE_FIRE_CRACKLE, 1.0f, 1.0f);
			return true;
		}

		@Override
		public float onIncoming(LivingEntity target, DamageSource source, float amount) {
			if (target != caster || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
			float kept = source.is(DamageTypeTags.IS_FIRE) ? 0 : amount * FURNACE_KEEPS;
			float held = amount - kept;
			if (held > 0 && refined < FURNACE_QI_MAX && !HeavenSpells.inHurtTime(caster)) {
				double gain = Math.min(FURNACE_QI_MAX - refined, held * FURNACE_QI_PER_POINT);
				refined += gain;
				PlayerCultivation c = CultivationManager.get(caster);
				c.setQi(Math.min(c.maxQi(), c.getQi() + gain));
				CultivationManager.sync(caster);
				HeavenSpells.particles(level, HeavenSpells.dust(REFINED, 1.0f), HeavenSpells.middle(caster), 4, 0.4, 0);
				HeavenSpells.particles(level, ParticleTypes.LAVA, HeavenSpells.middle(caster), 1, 0.1, 0);
				HeavenSpells.sound(level, caster.position(), SoundEvents.LAVA_POP, 0.8f, 1.4f);
			}
			return kept;
		}

		@Override
		public void end() {
			if (caster.isAlive()) {
				HeavenSpells.particles(level, ParticleTypes.SMOKE, caster.position().add(0, 1, 0), 12, 0.3, 0.02);
				HeavenSpells.sound(level, caster.position(), SoundEvents.FIRE_EXTINGUISH, 0.6f, 1.0f);
			}
		}
	}

	// --- Prairie-Burning Spark ---

	/** Prairie-Burning Spark: a spark that sets the first being looked at burning with crimson fire, leaping on to those near it. */
	private static void spark(ServerPlayer p) {
		LivingEntity target = HeavenSpells.aimed(p, SPARK_RANGE);
		if (target == null) return;
		ServerLevel level = p.serverLevel();
		Vec3 from = HeavenSpells.fromHands(p), to = HeavenSpells.middle(target);
		HeavenSpells.line(level, from, to, ParticleTypes.SMALL_FLAME, 0.5);
		HeavenSpells.line(level, from, to, HeavenSpells.dust(SPARK, 0.6f), 0.5);
		HeavenSpells.sound(level, p.position(), SoundEvents.BLAZE_SHOOT, 1.0f, 1.5f);
		long now = level.getGameTime();
		for (Spark old : HeavenSpells.lingering(p.getUUID(), Spark.class)) {
			long[] burning = old.burning.get(target);
			if (burning != null) {
				burning[0] = now + SPARK_TICKS; // already alight with this caster's fire: it burns on afresh
				return;
			}
		}
		Spark spark = new Spark(p);
		spark.light(target, now, SPARK_TICKS);
		HeavenSpells.linger(spark);
	}

	/** One spark's fire: every being it has ever caught, and those burning now (until when, since when). */
	static final class Spark extends HeavenSpells.Timed {
		final Set<LivingEntity> caught = new HashSet<>();
		final Map<LivingEntity, long[]> burning = new HashMap<>();

		Spark(ServerPlayer caster) {
			super(caster, SPARK_TICKS + SPARK_CAUGHT_TICKS * SPARK_MOST);
		}

		void light(LivingEntity e, long now, int ticks) {
			caught.add(e);
			burning.put(e, new long[]{now + ticks, now});
			e.setSecondsOnFire(Math.max(1, ticks / 20));
		}

		@Override
		protected boolean run(long age, long now) {
			if (!caster.isAlive() || caster.level() != level) return false;
			for (Map.Entry<LivingEntity, long[]> entry : new ArrayList<>(burning.entrySet())) {
				LivingEntity e = entry.getKey();
				long[] t = entry.getValue();
				if (!HeavenSpells.present(e, level) || now >= t[0] || e.isInWaterRainOrBubble() || e.isInPowderSnow) {
					burning.remove(e);
					if (e.isAlive()) {
						HeavenSpells.particles(level, ParticleTypes.SMOKE, HeavenSpells.middle(e), 6, 0.3, 0.02);
						HeavenSpells.particles(level, ParticleTypes.WHITE_ASH, HeavenSpells.middle(e), 4, 0.3, 0);
						if (now < t[0]) HeavenSpells.sound(level, e.position(), SoundEvents.FIRE_EXTINGUISH, 0.6f, 1.0f);
					}
					continue;
				}
				if (age % 5 == 0) {
					HeavenSpells.particles(level, ParticleTypes.FLAME, HeavenSpells.middle(e), 2, 0.3, 0.01);
					HeavenSpells.particles(level, HeavenSpells.dust(CRIMSON, 1.0f), HeavenSpells.middle(e), 2, 0.3, 0);
				}
				if (now == t[1] || (now - t[1]) % 20 != 0) continue;
				HeavenSpells.strikeAgain(caster, e, SPARK_HARM, QiElement.FIRE);
				e.setSecondsOnFire((int) Math.max(1, (t[0] - now) / 20));
				HeavenSpells.sound(level, e.position(), SoundEvents.FIRE_AMBIENT, 0.4f, 1.0f);
				if (!e.isAlive()) continue;
				List<LivingEntity> near = HeavenSpells.aroundPoint(level, e.position(), SPARK_LEAP,
						o -> o != e && !caught.contains(o) && !HeavenSpells.spared(caster, o) && e.hasLineOfSight(o));
				near.sort(java.util.Comparator.comparingDouble(o -> o.distanceToSqr(e)));
				for (LivingEntity o : near.subList(0, Math.min(SPARK_LEAPS, near.size()))) {
					if (caught.size() >= SPARK_MOST) break;
					light(o, now, SPARK_CAUGHT_TICKS);
					leap(level, HeavenSpells.middle(e), HeavenSpells.middle(o), FIRE);
					HeavenSpells.particles(level, ParticleTypes.LAVA, HeavenSpells.middle(o), 2, 0.2, 0);
					HeavenSpells.sound(level, o.position(), SoundEvents.FIRECHARGE_USE, 0.6f, 1.3f);
				}
			}
			return !burning.isEmpty();
		}

		/** Whether {@code e} burns with this spark's fire now (tests). */
		boolean burns(LivingEntity e) {
			return burning.containsKey(e);
		}
	}

	// --- Crimson Lotus Blooms ---

	/** Crimson Lotus Blooms: a bud of fire planted where the caster looks, opening to burn its heart and scatter its petals. */
	private static void lotus(ServerPlayer p) {
		BlockHitResult hit = HeavenSpells.lookedBlock(p, LOTUS_RANGE);
		if (hit == null) return;
		Vec3 heart = Vec3.atBottomCenterOf(hit.getBlockPos().above());
		HeavenSpells.linger(new Lotus(p, heart));
		HeavenSpells.sound(p.serverLevel(), heart, SoundEvents.BLAZE_SHOOT, 1.0f, 0.6f);
	}

	/** The lotus: a bud, then its heart's blast and its twelve petals flying out along the ground (each being struck once). */
	static final class Lotus extends HeavenSpells.Timed {
		final Vec3 heart;
		final List<Vec3[]> petals = new ArrayList<>();
		final List<Double> flown = new ArrayList<>();
		final Set<LivingEntity> struck = new HashSet<>();

		Lotus(ServerPlayer caster, Vec3 heart) {
			super(caster, LOTUS_BUD_TICKS + (int) Math.ceil(PETAL_REACH / PETAL_SPEED) + 2);
			this.heart = heart;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!caster.isAlive() || caster.level() != level) return false;
			if (age < LOTUS_BUD_TICKS) {
				double open = age / (double) LOTUS_BUD_TICKS;
				for (int i = 0; i < 8; i++) {
					double a = Math.PI * 2 * i / 8 + age * 0.1, r = 0.3 + open * 0.5;
					level.sendParticles(HeavenSpells.dust(i % 2 == 0 ? CRIMSON : FIRE, 1.2f), heart.x + Math.cos(a) * r, heart.y + 0.3 + open * 0.4,
							heart.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
				}
				if (age % 6 == 0) HeavenSpells.sound(level, heart, SoundEvents.FIRE_AMBIENT, 0.6f, 1.4f);
				return true;
			}
			if (age == LOTUS_BUD_TICKS) {
				for (LivingEntity e : HeavenSpells.aroundPoint(level, heart, LOTUS_HEART + 1, e -> !HeavenSpells.spared(caster, e)
						&& HeavenSpells.middle(e).distanceTo(heart.add(0, 0.5, 0)) <= LOTUS_HEART + e.getBbWidth() / 2)) {
					HeavenSpells.strikeAgain(caster, e, LOTUS_HEART_HARM, QiElement.FIRE);
					e.setSecondsOnFire(PETAL_BURN);
					struck.add(e);
				}
				for (int i = 0; i < PETALS; i++) {
					double a = Math.PI * 2 * i / PETALS;
					petals.add(new Vec3[]{heart.add(0, 0.5, 0), new Vec3(Math.cos(a), 0, Math.sin(a))});
					flown.add(0.0);
				}
				HeavenSpells.particles(level, ParticleTypes.FLAME, heart.add(0, 0.6, 0), 40, 0.6, 0.2);
				HeavenSpells.particles(level, ParticleTypes.LAVA, heart.add(0, 0.6, 0), 10, 0.5, 0);
				HeavenSpells.particles(level, ParticleTypes.EXPLOSION, heart.add(0, 0.6, 0), 1, 0, 0);
				HeavenSpells.sound(level, heart, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.2f);
				HeavenSpells.sound(level, heart, SoundEvents.FIRECHARGE_USE, 1.0f, 0.8f);
			}
			for (int i = petals.size() - 1; i >= 0; i--) {
				Vec3[] petal = petals.get(i);
				Vec3 from = petal[0], to = from.add(petal[1].scale(PETAL_SPEED));
				BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
				Vec3 stop = wall.getType() == HitResult.Type.MISS ? to : wall.getLocation();
				LivingEntity met = null;
				for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, stop).inflate(0.6), e -> !HeavenSpells.spared(caster, e) && !struck.contains(e))) {
					AABB body = e.getBoundingBox().inflate(0.3);
					if (body.clip(from, stop).isPresent() || body.contains(stop)) {
						met = e;
						break;
					}
				}
				HeavenSpells.line(level, from, stop, HeavenSpells.dust(i % 2 == 0 ? FIRE : CRIMSON, 1.3f), 0.4);
				level.sendParticles(ParticleTypes.FLAME, stop.x, stop.y, stop.z, 1, 0.05, 0.05, 0.05, 0);
				double gone = flown.get(i) + PETAL_SPEED;
				if (met != null) {
					struck.add(met);
					HeavenSpells.strikeAgain(caster, met, PETAL_HARM, QiElement.FIRE);
					met.setSecondsOnFire(PETAL_BURN);
					HeavenSpells.particles(level, ParticleTypes.FLAME, HeavenSpells.middle(met), 6, 0.2, 0.05);
					HeavenSpells.sound(level, met.position(), SoundEvents.GENERIC_BURN, 0.8f, 1.0f);
				}
				if (met != null || wall.getType() != HitResult.Type.MISS || gone >= PETAL_REACH) {
					petals.remove(i);
					flown.remove(i);
					continue;
				}
				petal[0] = stop;
				flown.set(i, gone);
			}
			return age <= LOTUS_BUD_TICKS || !petals.isEmpty();
		}
	}

	// --- Ash-Searing Talon ---

	/** Ash-Searing Talon: a burning talon rakes the first being looked at, and its ash seals the wound against all healing. */
	private static void talon(ServerPlayer p) {
		LivingEntity target = HeavenSpells.aimed(p, TALON_RANGE);
		if (target == null) return;
		ServerLevel level = p.serverLevel();
		Vec3 at = HeavenSpells.middle(target);
		Vec3 look = p.getViewVector(1.0f);
		Vec3 across = new Vec3(-look.z, 0, look.x);
		across = across.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : across.normalize();
		for (int claw = -1; claw <= 1; claw++) {
			Vec3 side = across.scale(claw * 0.35);
			HeavenSpells.line(level, at.add(side).add(across.scale(0.3)).add(0, 1.5, 0), at.add(side).subtract(across.scale(0.3)).add(0, -0.8, 0),
					HeavenSpells.dust(DEEP_FIRE, 1.2f), 0.23);
		}
		HeavenSpells.strike(p, target, TALON_HARM, QiElement.FIRE);
		target.setSecondsOnFire(TALON_BURN);
		HeavenSpells.particles(level, ParticleTypes.FLAME, at, 10, 0.3, 0.05);
		HeavenSpells.particles(level, ParticleTypes.ASH, at, 20, 0.4, 0);
		HeavenSpells.sound(level, p.position(), SoundEvents.PHANTOM_SWOOP, 1.0f, 1.4f);
		HeavenSpells.sound(level, p.position(), SoundEvents.BLAZE_SHOOT, 1.0f, 1.0f);
		HeavenSpells.sound(level, target.position(), SoundEvents.GENERIC_BURN, 1.0f, 1.0f);
		HeavenSpells.sound(level, target.position(), SoundEvents.FIRECHARGE_USE, 1.0f, 0.8f);
		if (!target.isAlive()) return;
		for (Seared old : HeavenSpells.lingering(null, Seared.class)) {
			if (old.target == target) {
				old.end = level.getGameTime() + SEARED_TICKS;
				return;
			}
		}
		HeavenSpells.linger(new Seared(p, target));
	}

	/** A wound seared shut with ash: no healing reaches its bearer while it lasts. */
	static final class Seared extends HeavenSpells.Timed implements HeavenSpells.HealListener {
		final LivingEntity target;

		Seared(ServerPlayer caster, LivingEntity target) {
			super(caster, SEARED_TICKS);
			this.target = target;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(target, level)) return false;
			if (age % 10 == 0) {
				Vec3 at = HeavenSpells.middle(target);
				level.sendParticles(ParticleTypes.ASH, at.x, at.y, at.z, 2, 0.2, 0.2, 0.2, 0);
				level.sendParticles(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, 1, 0.2, 0.2, 0.2, 0);
				level.sendParticles(HeavenSpells.dust(ASH, 1.0f), at.x, at.y, at.z, 1, 0.2, 0.2, 0.2, 0);
			}
			return true;
		}

		@Override
		public boolean allowHeal(LivingEntity entity, float amount) {
			if (entity != target || level.getGameTime() > end) return true;
			if (amount >= 0.5f) {
				HeavenSpells.particles(level, ParticleTypes.WHITE_ASH, HeavenSpells.middle(target), 2, 0.2, 0);
				HeavenSpells.sound(level, target.position(), SoundEvents.FIRE_EXTINGUISH, 0.3f, 1.6f);
			}
			return false;
		}
	}

	// --- Vermilion Bird Plunge ---

	/** Vermilion Bird Plunge: the caster soars high on vermilion wings, glides down steering by its look, and strikes where it lands. */
	private static void plunge(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		HeavenSpells.endLingering(p.getUUID(), Plunge.class);
		p.stopRiding();
		Vec3 look = p.getViewVector(1.0f).multiply(1, 0, 1);
		HeavenSpells.move(p, new Vec3(look.x * 0.5, PLUNGE_SOAR, look.z * 0.5));
		HeavenSpells.safeFall(p, PLUNGE_SOAR_TICKS + PLUNGE_GLIDE_TICKS + 40);
		for (LivingEntity e : HeavenSpells.around(p, 4, e -> !HeavenSpells.spared(p, e))) HeavenSpells.push(p.position(), e, 0.8);
		HeavenSpells.particles(level, ParticleTypes.FLAME, p.position().add(0, 0.5, 0), 30, 0.5, 0.15);
		HeavenSpells.particles(level, ParticleTypes.EXPLOSION, p.position(), 1, 0, 0);
		HeavenSpells.ring(level, p.position().add(0, 0.1, 0), 1.5, ParticleTypes.LARGE_SMOKE, 20);
		HeavenSpells.sound(level, p.position(), SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 1.0f);
		HeavenSpells.sound(level, p.position(), SoundEvents.FIRECHARGE_USE, 1.0f, 1.0f);
		HeavenSpells.linger(new Plunge(p));
	}

	/** The plunge: the soar, then the glide (a stoop when the caster sneaks), until it lands and strikes. */
	static final class Plunge extends HeavenSpells.Timed {
		double peak;
		long glideAt = -1;

		Plunge(ServerPlayer caster) {
			super(caster, PLUNGE_SOAR_TICKS + PLUNGE_GLIDE_TICKS + 2);
			this.peak = caster.getY();
		}

		/** Whether it has begun to glide (tests). */
		boolean gliding() {
			return glideAt >= 0;
		}

		@Override
		protected boolean run(long age, long now) {
			// Never flight: taking to Qi Flight or a sword ends it, as does falling or leaving.
			if (!casterHere() || caster.getAbilities().flying || SwordFlight.isRiding(caster)) return false;
			HeavenSpells.safeFall(caster, 40);
			peak = Math.max(peak, caster.getY());
			if (age % 2 == 0) wings();
			if (glideAt < 0) {
				if (age >= PLUNGE_SOAR_TICKS || age > 2 && caster.getDeltaMovement().y <= 0) glideAt = age;
				else return true;
			}
			long glided = age - glideAt;
			if (glided > 0 && (caster.onGround() || caster.isInWater() || caster.isInLava())) {
				land();
				return false;
			}
			if (glided >= PLUNGE_GLIDE_TICKS) {
				caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0));
				return false;
			}
			boolean stoop = caster.isShiftKeyDown();
			Vec3 v = caster.getDeltaMovement(), look = caster.getViewVector(1.0f).multiply(1, 0, 1);
			Vec3 flat = stoop ? new Vec3(v.x * 0.3, 0, v.z * 0.3) : new Vec3(v.x + (look.x * 0.7 - v.x) * 0.15, 0, v.z + (look.z * 0.7 - v.z) * 0.15);
			HeavenSpells.move(caster, new Vec3(flat.x, stoop ? -1.6 : -0.25, flat.z));
			if (stoop) {
				level.sendParticles(ParticleTypes.FLAME, caster.getX(), caster.getY() + 0.5, caster.getZ(), 6, 0.2, 0.4, 0.2, 0.01);
				if (glided % 10 == 0) HeavenSpells.sound(level, caster.position(), SoundEvents.PHANTOM_SWOOP, 1.0f, 1.0f);
			} else if (glided % 20 == 0) {
				HeavenSpells.sound(level, caster.position(), SoundEvents.ENDER_DRAGON_FLAP, 0.5f, 1.4f);
			}
			return true;
		}

		private void wings() {
			Vec3 look = caster.getViewVector(1.0f).multiply(1, 0, 1);
			Vec3 side = look.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : new Vec3(-look.z, 0, look.x).normalize();
			Vec3 shoulders = caster.position().add(0, 1.4, 0);
			for (int s = -1; s <= 1; s += 2) {
				for (int i = 1; i <= 8; i++) {
					double t = i / 8.0;
					Vec3 at = shoulders.add(side.scale(s * 1.8 * t)).add(0, Math.sin(t * Math.PI) * 0.4, 0);
					level.sendParticles(HeavenSpells.dust(t < 0.5 ? FIRE : ORANGE, 1.4f), at.x, at.y, at.z, 1, 0, 0, 0, 0);
				}
			}
			level.sendParticles(ParticleTypes.FLAME, caster.getX(), caster.getY(), caster.getZ(), 2, 0.1, 0, 0.1, 0);
		}

		private void land() {
			Vec3 at = caster.position();
			boolean wet = caster.isInWater();
			float harm = (float) Math.min(PLUNGE_MAX, PLUNGE_HARM + 0.5 * Math.max(0, peak - at.y - 5));
			if (wet) harm /= 2;
			caster.resetFallDistance();
			for (LivingEntity e : HeavenSpells.around(caster, PLUNGE_REACH, e -> !HeavenSpells.spared(caster, e))) {
				HeavenSpells.strikeAgain(caster, e, harm, QiElement.FIRE);
				if (!wet) e.setSecondsOnFire(PLUNGE_BURN);
				double d = e.position().distanceTo(at);
				Vec3 away = e.position().subtract(at).multiply(1, 0, 1);
				if (!HeavenSpells.greatBeast(e) && away.lengthSqr() > 1.0e-4) {
					double force = 1.2 * Math.max(0, 1 - d / 7);
					HeavenSpells.move(e, away.normalize().scale(force).add(0, 0.4, 0));
				}
			}
			if (wet) {
				HeavenSpells.particles(level, ParticleTypes.CLOUD, at.add(0, 0.5, 0), 30, 1.0, 0.05);
				HeavenSpells.particles(level, ParticleTypes.POOF, at.add(0, 0.5, 0), 20, 1.0, 0.05);
				HeavenSpells.sound(level, at, SoundEvents.FIRE_EXTINGUISH, 1.0f, 1.0f);
				HeavenSpells.sound(level, at, SoundEvents.GENERIC_SPLASH, 1.0f, 1.0f);
				return;
			}
			HeavenSpells.particles(level, peak - at.y >= 15 ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, at, 1, 0, 0);
			for (int i = 0; i < 40; i++) {
				double a = Math.PI * 2 * i / 40;
				level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.2, at.z, 0, Math.cos(a), 0.05, Math.sin(a), 0.4);
			}
			HeavenSpells.particles(level, ParticleTypes.LAVA, at.add(0, 0.3, 0), 20, 1.0, 0);
			HeavenSpells.particles(level, ParticleTypes.LARGE_SMOKE, at.add(0, 0.5, 0), 30, 1.5, 0.02);
			HeavenSpells.sound(level, at, SoundEvents.GENERIC_EXPLODE, 1.0f, 1.0f);
			HeavenSpells.sound(level, at, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 1.0f);
		}
	}

	// --- Light of First Dawn ---

	/** Light of First Dawn: the dawn's light round the caster burns away every ill of its friends, and wards them a while after. */
	private static void dawn(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		HeavenSpells.endLingering(p.getUUID(), Dawn.class);
		Dawn dawn = new Dawn(p);
		for (LivingEntity e : HeavenSpells.around(p, DAWN_RADIUS, e -> HeavenSpells.friendly(p, e))) {
			int n = burnAwayIlls(e);
			e.clearFire();
			cleanse(e);
			if (n > 0) {
				HeavenSpells.mend(e, DAWN_MEND * n);
				HeavenSpells.particles(level, ParticleTypes.WHITE_ASH, HeavenSpells.middle(e), 4 * n, 0.3, 0);
				HeavenSpells.particles(level, ParticleTypes.SMALL_FLAME, HeavenSpells.middle(e), 2 * n, 0.3, 0.01);
			}
			dawn.warded.add(e);
			for (int i = 0; i < 12; i++) {
				double a = i * 0.6, h = i * 0.15;
				level.sendParticles(ParticleTypes.END_ROD, e.getX() + Math.cos(a) * 0.6, e.getY() + h, e.getZ() + Math.sin(a) * 0.6, 1, 0, 0, 0, 0);
			}
			HeavenSpells.particles(level, HeavenSpells.dust(0xFFB060, 1.0f), HeavenSpells.middle(e), 6, 0.3, 0);
			HeavenSpells.sound(level, e.position(), SoundEvents.FIRE_EXTINGUISH, 0.3f, 1.8f);
		}
		HeavenSpells.linger(dawn);
		HeavenSpells.sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 1.0f, 1.4f);
		HeavenSpells.sound(level, p.position(), SoundEvents.BEACON_POWER_SELECT, 1.0f, 1.0f);
	}

	/** The dawn's ward: those it lit refuse every harmful effect for a while (it spreads its ring as it begins). */
	static final class Dawn extends HeavenSpells.Timed implements HeavenSpells.EffectListener {
		final Set<LivingEntity> warded = new HashSet<>();
		final Vec3 centre;

		Dawn(ServerPlayer caster) {
			super(caster, DAWN_WARD_TICKS);
			this.centre = caster.position();
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < 10) HeavenSpells.ring(level, centre.add(0, 0.2, 0), DAWN_RADIUS * (age + 1) / 10.0, HeavenSpells.dust(DAWN, 2.0f), 64);
			warded.removeIf(e -> !e.isAlive());
			if (age % 10 == 0) {
				for (LivingEntity e : warded) HeavenSpells.ring(level, e.position().add(0, e.getBbHeight() + 0.3, 0), 0.35, HeavenSpells.dust(DAWN, 0.8f), 6);
			}
			return !warded.isEmpty();
		}

		@Override
		public boolean allowEffect(LivingEntity entity, MobEffectInstance effect) {
			if (!warded.contains(entity) || !harmful(effect) || level.getGameTime() > end) return true;
			HeavenSpells.particles(level, ParticleTypes.WAX_ON, HeavenSpells.middle(entity), 4, 0.3, 0);
			HeavenSpells.sound(level, entity.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.6f);
			return false;
		}
	}

	// --- Noonday Golden Crow ---

	/** Noonday Golden Crow: a golden-crow sun hung over the caster, searing every other being it can see. */
	private static void crow(ServerPlayer p) {
		HeavenSpells.endLingering(p.getUUID(), Crow.class);
		HeavenSpells.linger(new Crow(p));
		ServerLevel level = p.serverLevel();
		HeavenSpells.sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 1.0f, 0.8f);
		HeavenSpells.sound(level, p.position(), SoundEvents.BLAZE_AMBIENT, 1.0f, 1.0f);
	}

	/** The sun over the caster: each second, a burning ray to every being in its sight. */
	static final class Crow extends HeavenSpells.Timed {
		Crow(ServerPlayer caster) {
			super(caster, CROW_TICKS);
		}

		/** Where the sun hangs: over the caster's head, beneath whatever roof is lower (but a block above the head at least). */
		Vec3 sun() {
			Vec3 head = caster.getEyePosition();
			Vec3 high = caster.position().add(0, CROW_HEIGHT, 0);
			BlockHitResult roof = level.clip(new ClipContext(head, high, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
			if (roof.getType() == HitResult.Type.MISS) return high;
			return new Vec3(high.x, Math.max(head.y + 1, roof.getLocation().y - 0.5), high.z);
		}

		private boolean seen(Vec3 sun, LivingEntity e) {
			for (Vec3 to : new Vec3[]{e.getEyePosition(), HeavenSpells.middle(e)}) {
				if (level.clip(new ClipContext(sun, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getType() == HitResult.Type.MISS) return true;
			}
			return false;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere() || CultivationManager.get(caster).netherworld().ghost()) return false;
			Vec3 sun = sun();
			for (int i = 0; i < 30; i++) {
				double y = 1 - 2 * (i + 0.5) / 30, r = Math.sqrt(1 - y * y), a = i * 2.4 + age * 0.05;
				level.sendParticles(HeavenSpells.dust(GOLD, 2.5f), sun.x + Math.cos(a) * r * 1.2, sun.y + y * 1.2, sun.z + Math.sin(a) * r * 1.2, 1, 0, 0, 0, 0);
			}
			HeavenSpells.ring(level, sun, 1.4, HeavenSpells.dust(FIRE, 2.0f), 10);
			level.sendParticles(ParticleTypes.FLAME, sun.x, sun.y, sun.z, 4, 0.6, 0.6, 0.6, 0.01);
			level.sendParticles(ParticleTypes.END_ROD, sun.x, sun.y, sun.z, 1, 0.3, 0.3, 0.3, 0.01);
			level.sendParticles(HeavenSpells.dust(0x1A1A1A, 0.8f), sun.x, sun.y, sun.z, 3, 0.1, 0.1, 0.1, 0);
			if (age == 0 || age % 20 != 0) return true;
			HeavenSpells.sound(level, sun, SoundEvents.FIRE_AMBIENT, 0.6f, 1.0f);
			for (LivingEntity e : HeavenSpells.aroundPoint(level, sun, CROW_RADIUS, e -> !HeavenSpells.spared(caster, e))) {
				if (!seen(sun, e)) continue; // under cover: untouched this time
				boolean undead = e.getMobType() == MobType.UNDEAD;
				HeavenSpells.strikeAgain(caster, e, undead ? CROW_UNDEAD_HARM : CROW_HARM, QiElement.FIRE);
				e.setSecondsOnFire(CROW_BURN);
				e.addEffect(new MobEffectInstance(MobEffects.GLOWING, CROW_GLOW_TICKS, 0, false, false));
				HeavenSpells.line(level, sun, HeavenSpells.middle(e), HeavenSpells.dust(SPARK, 0.8f), 0.75);
				HeavenSpells.particles(level, ParticleTypes.FLAME, HeavenSpells.middle(e), 4, 0.3, 0.01);
				if (undead) HeavenSpells.particles(level, ParticleTypes.SMOKE, HeavenSpells.middle(e), 4, 0.3, 0.01);
				HeavenSpells.sound(level, e.position(), SoundEvents.GENERIC_BURN, 0.6f, 1.0f);
			}
			return true;
		}

		@Override
		public void end() {
			if (caster.isAlive()) HeavenSpells.sound(level, caster.position(), SoundEvents.BEACON_DEACTIVATE, 1.0f, 1.0f);
		}
	}

	// --- Three Heaven-Scorching Firestorms ---

	/** Three Heaven-Scorching Firestorms: three storms of fire loosed round the caster, each hunting a foe of its own. */
	private static void firestorms(ServerPlayer p) {
		HeavenSpells.endLingering(p.getUUID(), Firestorms.class);
		HeavenSpells.linger(new Firestorms(p));
		ServerLevel level = p.serverLevel();
		HeavenSpells.sound(level, p.position(), SoundEvents.BLAZE_SHOOT, 1.0f, 0.6f);
		HeavenSpells.sound(level, p.position(), SoundEvents.ENDER_DRAGON_GROWL, 0.5f, 1.5f);
	}

	/** One storm: where it stands, the foe it hunts, and its turn about the caster when there is none. */
	private static final class Storm {
		double x, y, z, turn;
		LivingEntity quarry;
	}

	/** The three storms, roaming and burning until their time is out. */
	static final class Firestorms extends HeavenSpells.Timed {
		final List<Storm> storms = new ArrayList<>();

		Firestorms(ServerPlayer caster) {
			super(caster, STORM_TICKS);
			double yaw = Math.toRadians(caster.getYRot());
			for (int i = 0; i < STORMS; i++) {
				Storm s = new Storm();
				s.turn = yaw + Math.PI * 2 * i / STORMS;
				s.x = caster.getX() - Math.sin(s.turn) * 3;
				s.z = caster.getZ() + Math.cos(s.turn) * 3;
				s.y = caster.getY();
				storms.add(s);
			}
			hunt();
		}

		/** Each storm without a quarry takes the nearest foe no other hunts (or, if there are too few, one already hunted). */
		private void hunt() {
			List<LivingEntity> foes = HeavenSpells.around(caster, STORM_RANGE, e -> HeavenSpells.foe(caster, e));
			foes.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(caster)));
			for (Storm s : storms) {
				if (s.quarry != null && HeavenSpells.present(s.quarry, level) && s.quarry.distanceTo(caster) <= STORM_LEASH) continue;
				s.quarry = null;
				for (LivingEntity foe : foes) {
					if (storms.stream().noneMatch(o -> o.quarry == foe)) {
						s.quarry = foe;
						break;
					}
				}
				if (s.quarry == null && !foes.isEmpty()) s.quarry = foes.get(0);
			}
		}

		/** Each storm's quarry now (tests). */
		List<LivingEntity> quarries() {
			List<LivingEntity> all = new ArrayList<>();
			for (Storm s : storms) all.add(s.quarry);
			return all;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!caster.isAlive() || caster.level() != level) return false;
			hunt();
			for (Storm s : storms) {
				double tx, tz;
				if (s.quarry != null) {
					tx = s.quarry.getX();
					tz = s.quarry.getZ();
				} else {
					s.turn += 0.3 / 5;
					tx = caster.getX() - Math.sin(s.turn) * 5;
					tz = caster.getZ() + Math.cos(s.turn) * 5;
				}
				double dx = tx - s.x, dz = tz - s.z, d = Math.sqrt(dx * dx + dz * dz);
				if (d > 1.0e-3) {
					double step = Math.min(STORM_SPEED, d);
					s.x += dx / d * step;
					s.z += dz / d * step;
				}
				Vec3 ground = HeavenSpells.groundBelow(level, new Vec3(s.x, s.y, s.z), 8, 8);
				if (ground != null) s.y += Math.max(-1, Math.min(1, ground.y - s.y));
				for (int k = 0; k < 3; k++) {
					double h = (age * 0.35 + k * 2.3) % STORM_HEIGHT, r = 0.6 + (STORM_RADIUS - 0.6) * h / STORM_HEIGHT, a = age * 0.6 + k * 2.1;
					level.sendParticles(ParticleTypes.FLAME, s.x + Math.cos(a) * r, s.y + h, s.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
					int rgb = h < 2.3 ? FIRE : h < 4.6 ? ORANGE : GOLD;
					if (k < 2) level.sendParticles(HeavenSpells.dust(rgb, 1.6f), s.x - Math.cos(a) * r, s.y + h, s.z - Math.sin(a) * r, 1, 0, 0, 0, 0);
				}
				level.sendParticles(ParticleTypes.LARGE_SMOKE, s.x, s.y + STORM_HEIGHT, s.z, 2, 0.4, 0.2, 0.4, 0.01);
				if (age % 5 == 0) {
					level.sendParticles(ParticleTypes.ASH, s.x, s.y + 3, s.z, 1, 0.5, 1, 0.5, 0);
					level.sendParticles(ParticleTypes.LAVA, s.x, s.y + 0.2, s.z, 1, 0.3, 0, 0.3, 0);
				}
				if (age % 10 == 0) HeavenSpells.sound(level, new Vec3(s.x, s.y, s.z), SoundEvents.FIRE_AMBIENT, 1.0f, 1.0f);
				if (age % 20 == 0) HeavenSpells.sound(level, new Vec3(s.x, s.y, s.z), SoundEvents.BLAZE_BURN, 0.8f, 1.0f);
			}
			if (age > 0 && age % 10 == 0) {
				Set<LivingEntity> struck = new HashSet<>();
				for (Storm s : storms) {
					Vec3 foot = new Vec3(s.x, s.y, s.z);
					for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(foot, foot).inflate(STORM_RADIUS + 2, STORM_HEIGHT + 1, STORM_RADIUS + 2),
							e -> !HeavenSpells.spared(caster, e))) {
						double dx = e.getX() - s.x, dz = e.getZ() - s.z, d = Math.sqrt(dx * dx + dz * dz);
						if (d > STORM_RADIUS + e.getBbWidth() / 2 || e.getY() < s.y - 0.5 || e.getY() > s.y + STORM_HEIGHT || !struck.add(e)) continue;
						HeavenSpells.strikeAgain(caster, e, STORM_HARM, QiElement.FIRE);
						e.setSecondsOnFire(STORM_BURN);
						if (!HeavenSpells.greatBeast(e)) {
							Vec3 round = d < 1.0e-3 ? Vec3.ZERO : new Vec3(-dz / d, 0, dx / d).scale(0.35);
							Vec3 v = e.getDeltaMovement();
							HeavenSpells.move(e, new Vec3(v.x + round.x, Math.min(0.8, v.y + 0.5), v.z + round.z));
						}
						HeavenSpells.particles(level, ParticleTypes.FLAME, HeavenSpells.middle(e), 4, 0.3, 0.02);
						HeavenSpells.sound(level, e.position(), SoundEvents.GENERIC_BURN, 0.6f, 1.0f);
					}
				}
			}
			return true;
		}

		@Override
		public void end() {
			for (Storm s : storms) {
				Vec3 at = new Vec3(s.x, s.y + 2, s.z);
				HeavenSpells.particles(level, ParticleTypes.LARGE_SMOKE, at, 30, 1.0, 0.03);
				HeavenSpells.particles(level, ParticleTypes.ASH, at, 20, 1.0, 0);
				HeavenSpells.particles(level, ParticleTypes.POOF, at, 10, 1.0, 0.02);
				HeavenSpells.sound(level, at, SoundEvents.FIRE_EXTINGUISH, 0.8f, 1.0f);
			}
		}
	}

	// --- Undying Phoenix ---

	/** Undying Phoenix: the caster wreathed in phoenix fire, which turns its death to ash and a rebirth. */
	private static void phoenix(ServerPlayer p, Spell spell) {
		HeavenSpells.endLingering(p.getUUID(), Phoenix.class);
		HeavenSpells.linger(new Phoenix(p, spell));
		ServerLevel level = p.serverLevel();
		HeavenSpells.sound(level, p.position(), SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.0f, 1.0f);
		HeavenSpells.sound(level, p.position(), SoundEvents.BLAZE_AMBIENT, 1.0f, 1.0f);
	}

	/** The wreath, waiting; then, at the caster's death, the ash it hangs in, and the rising. */
	static final class Phoenix extends HeavenSpells.Timed implements HeavenSpells.DeathListener, HeavenSpells.HarmListener {
		final Spell spell;
		long ashAt = -1;
		boolean risen;

		Phoenix(ServerPlayer caster, Spell spell) {
			super(caster, PHOENIX_TICKS);
			this.spell = spell;
		}

		/** Whether the caster is in its ashes now (tests). */
		boolean ashen() {
			return ashAt >= 0 && !risen;
		}

		@Override
		protected boolean run(long age, long now) {
			if (caster.isRemoved() || caster.level() != level) return false;
			if (ashAt < 0) {
				if (!caster.isAlive()) return false;
				if (age % 10 == 0) wreath();
				return true;
			}
			// In the ash: hanging still, unharmed, until it rises.
			HeavenSpells.move(caster, Vec3.ZERO);
			caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 5, 0, false, false));
			Vec3 at = caster.position();
			for (int i = 0; i < 8; i++) {
				double a = (now - ashAt) * 0.4 + i * 0.8, h = i * 0.25;
				level.sendParticles(ParticleTypes.ASH, at.x + Math.cos(a) * 0.6, at.y + h, at.z + Math.sin(a) * 0.6, 1, 0, 0, 0, 0);
			}
			level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1, at.z, 4, 0.3, 0.5, 0.3, 0.01);
			level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 1, at.z, 3, 0.3, 0.5, 0.3, 0.01);
			level.sendParticles(HeavenSpells.dust(ASH, 1.0f), at.x, at.y + 1, at.z, 2, 0.3, 0.5, 0.3, 0);
			if (now - ashAt >= PHOENIX_ASH_TICKS) {
				rise();
				return false;
			}
			return true;
		}

		private void wreath() {
			Vec3 look = caster.getViewVector(1.0f).multiply(1, 0, 1);
			Vec3 back = look.lengthSqr() < 1.0e-4 ? Vec3.ZERO : look.normalize().scale(-0.4);
			Vec3 side = look.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : new Vec3(-look.z, 0, look.x).normalize();
			Vec3 at = caster.position().add(back).add(0, 1.4, 0);
			for (int s = -1; s <= 1; s += 2) {
				for (int i = 1; i <= 6; i++) {
					double t = i / 6.0;
					Vec3 point = at.add(side.scale(s * 1.4 * t)).add(0, Math.sin(t * Math.PI) * 0.5 + t * 0.3, 0);
					level.sendParticles(HeavenSpells.dust(t < 0.6 ? FIRE : GOLD, 1.2f), point.x, point.y, point.z, 1, 0, 0, 0, 0);
				}
			}
			level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 3, 0.3, 0.3, 0.3, 0.01);
			level.sendParticles(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, 2, 0.3, 0.3, 0.3, 0.02);
		}

		@Override
		public boolean allowDeath(LivingEntity entity, DamageSource source) {
			if (entity != caster || ashAt >= 0 || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
			// In a spar, the spar's own rule (the blow stops at the line) wins, and the wreath is kept.
			if (source.getEntity() != null && SparSystem.areSparring(entity, source.getEntity())) return true;
			caster.setHealth(1);
			ashAt = level.getGameTime();
			end = ashAt + PHOENIX_ASH_TICKS + 2;
			burnAwayIlls(caster);
			caster.clearFire();
			cleanse(caster);
			HeavenSpells.sound(level, caster.position(), SoundEvents.FIRE_EXTINGUISH, 1.0f, 1.0f);
			HeavenSpells.sound(level, caster.position(), SoundEvents.TOTEM_USE, 1.0f, 0.6f);
			return false;
		}

		@Override
		public float onIncoming(LivingEntity target, DamageSource source, float amount) {
			return target == caster && ashen() ? 0 : amount;
		}

		private void rise() {
			risen = true;
			caster.setHealth(Math.max(1, caster.getMaxHealth() * 0.5f));
			caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, PHOENIX_BOON_TICKS, 1));
			caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, PHOENIX_BOON_TICKS, 2));
			caster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, PHOENIX_BOON_TICKS, 0));
			Vec3 at = caster.position();
			for (LivingEntity e : HeavenSpells.around(caster, PHOENIX_RADIUS, e -> !HeavenSpells.spared(caster, e))) {
				HeavenSpells.strikeAgain(caster, e, PHOENIX_HARM, QiElement.FIRE);
				e.setSecondsOnFire(PHOENIX_BURN);
				Vec3 away = e.position().subtract(at).multiply(1, 0, 1);
				if (!HeavenSpells.greatBeast(e) && away.lengthSqr() > 1.0e-4) {
					double force = 2.0 * Math.max(0, 1 - e.position().distanceTo(at) / 17);
					HeavenSpells.move(e, away.normalize().scale(force).add(0, 0.6, 0));
				}
			}
			HeavenSpells.particles(level, ParticleTypes.FLASH, at.add(0, 1, 0), 1, 0, 0);
			HeavenSpells.particles(level, ParticleTypes.EXPLOSION_EMITTER, at.add(0, 1, 0), 1, 0, 0);
			HeavenSpells.particles(level, ParticleTypes.FLAME, at.add(0, 1, 0), 120, 0.5, 0.6);
			HeavenSpells.particles(level, HeavenSpells.dust(GOLD, 1.5f), at.add(0, 1, 0), 60, 2.0, 0);
			HeavenSpells.particles(level, ParticleTypes.LAVA, at.add(0, 1, 0), 40, 1.5, 0);
			for (int i = 0; i <= 12; i++) {
				double t = i / 12.0 - 0.5;
				Vec3 wing = at.add(t * 6, 6 + t * t * 8, 0);
				level.sendParticles(HeavenSpells.dust(Math.abs(t) > 0.35 ? GOLD : FIRE, 1.5f), wing.x, wing.y, wing.z, 3, 0.1, 0.1, 0.1, 0);
			}
			HeavenSpells.sound(level, at, SoundEvents.GENERIC_EXPLODE, 1.0f, 1.0f);
			HeavenSpells.sound(level, at, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, 1.0f, 1.0f);
			HeavenSpells.sound(level, at, SoundEvents.ENDER_DRAGON_GROWL, 1.0f, 1.6f);
		}

		@Override
		public void end() {
			if (ashAt >= 0 && !risen && caster.isAlive() && !caster.isRemoved()) rise(); // cut short in the ash: it rises now
			if (ashAt >= 0 || !caster.isAlive() || caster.isRemoved()) return;
			// Gone unused: ready again ninety seconds after the cast.
			Spells.setCooldown(caster, spell, PHOENIX_UNUSED_COOLDOWN);
			Spells.sync(caster);
			HeavenSpells.sound(level, caster.position(), SoundEvents.FIRE_EXTINGUISH, 0.5f, 1.0f);
		}
	}

	private FireSpells() {}
}
