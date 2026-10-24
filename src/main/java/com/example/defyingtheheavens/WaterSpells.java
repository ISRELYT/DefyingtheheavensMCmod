package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The water element's spells (Spell, HeavenSpells): water is weak to earth and strong against fire (SpellAuras); it puts out what
 * burns. Every number is the design's; every look its own (the client's spell renderers).
 * <ul>
 *   <li>Qi Refining: Water Arrow, Azure Coating Technique (a water ward: fire's harm and fire spells stopped), Azure Breathing
 *       Technique (walk on water, breathe under it).</li>
 *   <li>Foundation Building: Rain Call (a rain cloud of your own, slowing all under it but you), Water Prison (a sphere of water
 *       round a being, the air drawn out of it), Torrential Spring (a great ball of water gathered and dropped, a small lake made).</li>
 *   <li>Core Formation: Tidal Lance, Mist Veil.</li>
 *   <li>Nascent Soul: Glacial Spears, Great Tide.</li>
 *   <li>Heavenly Being: Heavenly River Torrent, Frozen Field (all within 100 frozen; its thaw hurts).</li>
 *   <li>Four Axis: Absolute Zero, Boundless Sea.</li>
 *   <li>Dao Integration: Black Tortoise Bastion (a water ward, and its serpent), Hundred Rivers Converge (a whirlpool).</li>
 *   <li>Void Shattering: Weak Water from the Void (the water that bears nothing up pours out of a rift: all under it sink, fliers too), Kun
 *       Rises from the Northern Abyss (the land opens onto the abyss and the Kun breaches out of it).</li>
 * </ul>
 */
public final class WaterSpells {
	public static final double ARROW_SPEED = 2.4, ARROW_RANGE = 40;
	public static final float ARROW_DAMAGE = 5;
	public static final int COATING_TICKS = 30 * 20, BREATHING_TICKS = 60 * 20;
	public static final double RAIN_RADIUS = 8, RAIN_HEIGHT = 9, RAIN_SLOW = 0.1;
	public static final int RAIN_TICKS = 20 * 20;
	public static final double PRISON_RANGE = 16;
	public static final int PRISON_TICKS = 4 * 20;
	public static final float PRISON_FORCE = 30, PRISON_DAMAGE = 2;
	public static final double SPRING_RANGE = 32;
	public static final int SPRING_GATHER = 30, LAKE_RADIUS = 3;
	public static final float SPRING_DAMAGE = 8;
	public static final double SPRING_RADIUS = 4;
	public static final double LANCE_RANGE = 24;
	public static final int LANCE_TICKS = 2 * 20;
	public static final float LANCE_DAMAGE = 6;
	public static final double MIST_RADIUS = 10;
	public static final int MIST_TICKS = 15 * 20;
	public static final double SPEAR_SPEED = 2.2, SPEAR_RANGE = 40;
	public static final float SPEAR_DAMAGE = 11;
	public static final double TIDE_LENGTH = 30, TIDE_PACE = 0.75, TIDE_WIDTH = 14, TIDE_HEIGHT = 10;
	public static final float TIDE_DAMAGE = 30;
	/** The channel the river scours where it lands: 3 round, by its realm (Devastation#sized). */
	public static final double RIVER_LENGTH = 32, RIVER_WIDTH = 2.5, RIVER_SCOUR = Devastation.sized(Realm.HEAVENLY_BEING, 3.8);
	public static final int RIVER_TICKS = 2 * 20;
	public static final float RIVER_DAMAGE = 60;
	public static final double FROZEN_RADIUS = 64, FROST_RADIUS = 32;
	public static final int FREEZE_SPREAD = 50, FROZEN_TICKS = 3 * 20, THAW_MIN = 20 * 20, THAW_MAX = 40 * 20;
	public static final float THAW_DAMAGE = 3;
	public static final double ZERO_RANGE = 48;
	public static final int ZERO_TICKS = 5 * 20;
	public static final float ZERO_DAMAGE = 200, ZERO_SHATTER = 60;
	/** The basin the sea leaves behind: 18 round, by its realm (Devastation#sized). */
	public static final double SEA_RANGE = 48, SEA_RADIUS = 16, SEA_BASIN = Devastation.sized(Realm.FOUR_AXIS, 18);
	public static final int SEA_TICKS = 8 * 20;
	public static final float SEA_DAMAGE = 22;
	/**
	 * Black Tortoise Bastion: whom its shell wards (within {@link #TORTOISE_RADIUS} of its caster: SpellAura BLACK_TORTOISE), its serpent's
	 * reach and harm; how long it keeps by its caster, how often its serpent strikes, how long after it rears back it lands, and when it
	 * may first strike (once the tortoise has risen: its rise takes 24 ticks) (ticks).
	 */
	public static final double TORTOISE_RADIUS = HeavenSpells.span(Realm.DAO_INTEGRATION, 8), TORTOISE_REACH = HeavenSpells.span(Realm.DAO_INTEGRATION, 16);
	public static final float TORTOISE_DAMAGE = 200;
	public static final int TORTOISE_TICKS = 12 * 20, TORTOISE_STRIKE_EVERY = 20, TORTOISE_LUNGE = 6, TORTOISE_RISEN = 30;
	/**
	 * Hundred Rivers Converge: where (up to {@link #RIVERS_RANGE}), whom it drags in (within {@link #RIVERS_RADIUS}), its harm a second, how
	 * long it turns and its burst (ticks); the flood its rivers pour in over the whole of its reach as it turns, a basin of it as wide (8
	 * deep at its heart: by its realm, Devastation#sized), poured in from its rim over {@link #RIVERS_POUR} ticks.
	 */
	public static final double RIVERS_RANGE = HeavenSpells.span(Realm.DAO_INTEGRATION, 64), RIVERS_RADIUS = HeavenSpells.span(Realm.DAO_INTEGRATION, 24),
			RIVERS_BASIN = RIVERS_RADIUS, RIVERS_BASIN_DEPTH = Devastation.sized(Realm.DAO_INTEGRATION, 6);
	public static final float RIVERS_DAMAGE = 90;
	public static final int RIVERS_TICKS = 5 * 20, RIVERS_BURST = 16, RIVERS_POUR = 60;
	/**
	 * Weak Water from the Void: where (up to {@link #WEAK_RANGE}, 118 by its realm), how high its rift opens (40: its fall's model is that long,
	 * SpellConstruct#sizedFor) and how long the rift is, how far round where it falls it reaches ({@link #WEAK_RADIUS}, 26), its harm a second;
	 * the flood it spreads as it pours over the whole of that reach, the land sinking under it into a basin as wide (7 deep at its heart: by
	 * its realm), out from where it falls over {@link #WEAK_SPREAD} ticks; how long the rift takes to open, and how long it pours (ticks).
	 */
	public static final double WEAK_RANGE = HeavenSpells.span(Realm.VOID_SHATTERING, 64), WEAK_HIGH = HeavenSpells.span(Realm.VOID_SHATTERING, 22),
			WEAK_RIFT = HeavenSpells.span(Realm.VOID_SHATTERING, 15), WEAK_RADIUS = HeavenSpells.span(Realm.VOID_SHATTERING, 14),
			WEAK_BASIN = WEAK_RADIUS, WEAK_BASIN_DEPTH = Devastation.sized(Realm.VOID_SHATTERING, 4);
	public static final float WEAK_DAMAGE = 160;
	public static final int WEAK_OPENS = 14, WEAK_TICKS = 6 * 20, WEAK_SPREAD = 50;
	/** The Weak Water's pale blue: its rift glows so. */
	public static final int WEAK_PALE = 0xA6D6FF;
	/**
	 * Kun Rises from the Northern Abyss: where (up to {@link #KUN_RANGE}, 118), the abyss's breadth (40), whom it strikes as it breaches and
	 * as it dives back (within {@link #KUN_RADIUS}, 22) and how hard; how high its body's middle leaps (59) and how far its arc reaches to either
	 * side (13); the basin its dive leaves (18 round, 7 deep, a lake in it: by its realm); how long the abyss takes to open, its leap, and how
	 * long the abyss stays open after it has gone (ticks).
	 */
	public static final double KUN_RANGE = HeavenSpells.span(Realm.VOID_SHATTERING, 64), KUN_ABYSS = HeavenSpells.span(Realm.VOID_SHATTERING, 22),
			KUN_RADIUS = HeavenSpells.span(Realm.VOID_SHATTERING, 12), KUN_LEAP = HeavenSpells.span(Realm.VOID_SHATTERING, 32), KUN_SWING = HeavenSpells.span(Realm.VOID_SHATTERING, 7),
			KUN_BASIN = Devastation.sized(Realm.VOID_SHATTERING, 10), KUN_BASIN_DEPTH = Devastation.sized(Realm.VOID_SHATTERING, 4);
	public static final float KUN_DAMAGE = 1500, KUN_DIVE_DAMAGE = 500;
	public static final int KUN_OPENS = 16, KUN_LEAPS = 60, KUN_AFTER = 14;
	/** The northern abyss's deep blue: its rift glows so. */
	public static final int ABYSS_BLUE = 0x3F7CFF;

	private static final UUID RAIN_ID = UUID.fromString("4c1e9a72-0b5f-4e3d-8f26-d91a7b3e5c08");

	static boolean canCast(LivingEntity p, Spell spell) {
		return switch (spell) {
			case WATER_PRISON -> HeavenSpells.aimed(p, PRISON_RANGE) != null || HeavenSpells.refuse(p);
			case ABSOLUTE_ZERO -> HeavenSpells.aimed(p, ZERO_RANGE) != null || HeavenSpells.refuse(p);
			default -> true;
		};
	}

	static void cast(LivingEntity p, Spell spell) {
		ServerLevel level = HeavenSpells.level(p);
		switch (spell) {
			case WATER_ARROW -> arrow(p, spell);
			case AZURE_COATING_TECHNIQUE -> {
				SpellAuras.put(p, SpellAura.AZURE_COATING, COATING_TICKS);
				HeavenSpells.sound(level, p.position(), SoundEvents.BUCKET_EMPTY, 1.0f, 1.2f);
				HeavenSpells.sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.6f);
				SpellFx.send(level, SpellFx.Kind.SPLASH, HeavenSpells.middle(p), Vec3.ZERO, 0.8f, 0);
			}
			case AZURE_BREATHING_TECHNIQUE -> {
				SpellAuras.put(p, SpellAura.AZURE_BREATHING, BREATHING_TICKS);
				HeavenSpells.sound(level, p.position(), SoundEvents.CONDUIT_ACTIVATE, 1.0f, 1.3f);
				SpellFx.send(level, SpellFx.Kind.RIPPLE, p.position(), Vec3.ZERO, 1.5f, 0);
			}
			case RAIN_CALL -> HeavenSpells.linger(new Rain(p));
			case WATER_PRISON -> {
				LivingEntity target = HeavenSpells.aimed(p, PRISON_RANGE);
				if (target != null) HeavenSpells.linger(new Prison(p, spell, target, PRISON_TICKS, PRISON_DAMAGE, PRISON_FORCE, SpellField.Kind.WATER_PRISON));
			}
			case TORRENTIAL_SPRING -> spring(p, spell);
			case TIDAL_LANCE -> {
				HeavenSpells.endLingering(p.getUUID(), Lance.class);
				HeavenSpells.linger(new Lance(p, spell));
			}
			case MIST_VEIL -> HeavenSpells.linger(new Mist(p));
			case GLACIAL_SPEARS -> spears(p, spell);
			case GREAT_TIDE -> HeavenSpells.linger(new Tide(p, spell));
			case HEAVENLY_RIVER_TORRENT -> HeavenSpells.linger(new River(p, spell));
			case FROZEN_FIELD -> {
				HeavenSpells.endLingering(p.getUUID(), Frozen.class);
				HeavenSpells.linger(new Frozen(p, spell));
			}
			case ABSOLUTE_ZERO -> {
				LivingEntity target = HeavenSpells.aimed(p, ZERO_RANGE);
				if (target != null) HeavenSpells.linger(new Zero(p, spell, target));
			}
			case BOUNDLESS_SEA -> HeavenSpells.linger(new Sea(p, spell));
			case BLACK_TORTOISE_BASTION -> {
				HeavenSpells.endLingering(p.getUUID(), Tortoise.class);
				HeavenSpells.linger(new Tortoise(p, spell));
			}
			case HUNDRED_RIVERS_CONVERGE -> {
				LivingEntity aimed = HeavenSpells.aimed(p, RIVERS_RANGE);
				HeavenSpells.linger(new Rivers(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, RIVERS_RANGE)));
			}
			case WEAK_WATER_FROM_THE_VOID -> {
				LivingEntity aimed = HeavenSpells.aimed(p, WEAK_RANGE);
				HeavenSpells.linger(new WeakWater(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, WEAK_RANGE)));
			}
			case KUN_RISES_FROM_THE_ABYSS -> {
				LivingEntity aimed = HeavenSpells.aimed(p, KUN_RANGE);
				HeavenSpells.linger(new Kun(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, KUN_RANGE)));
			}
			default -> {}
		}
	}

	/** Whether {@code e} needs air (a fish, a drowned, a guardian, one under Azure Breathing doesn't). */
	static boolean breathes(LivingEntity e) {
		return !e.canBreatheUnderwater() && !SpellAuras.has(e, SpellAura.AZURE_BREATHING) && !(e instanceof net.minecraft.world.entity.player.Player player && player.getAbilities().invulnerable);
	}

	/** Water strikes {@code target}: harm, and whatever burnt on it is put out. */
	static void soak(LivingEntity p, LivingEntity target, float damage, Spell spell, net.minecraft.world.entity.Entity via) {
		HeavenSpells.strike(p, target, damage, spell, via);
		if (target.isOnFire()) {
			target.clearFire();
			SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.STEAM, HeavenSpells.middle(target), Vec3.ZERO, 0.6f, 0);
		}
	}

	// --- Qi Refining ---

	private static void arrow(LivingEntity p, Spell spell) {
		SpellProjectile.cast(p, spell, SpellProjectile.Shape.WATER_ARROW, 1.0f, 5, new Vec3(0.3, -0.2, 1.2), ARROW_SPEED, ARROW_RANGE,
				new SpellProjectile.Behaviour() {
					@Override
					public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
						soak(p, target, ARROW_DAMAGE, spell, missile);
						SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.SPLASH, missile.position(), missile.getDeltaMovement(), 0.7f, 0);
						HeavenSpells.sound(HeavenSpells.level(p), target.position(), SoundEvents.GENERIC_SPLASH, 0.8f, 1.4f);
						return false;
					}

					@Override
					public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
						SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.SPLASH, hit.getLocation(), missile.getDeltaMovement(), 0.7f, 0);
						HeavenSpells.sound(HeavenSpells.level(p), hit.getLocation(), SoundEvents.GENERIC_SPLASH, 0.6f, 1.5f);
						douse(HeavenSpells.level(p), hit.getBlockPos().relative(hit.getDirection()), 1);
					}

					@Override
					public void launched(SpellProjectile missile) {
						HeavenSpells.sound(HeavenSpells.level(p), missile.position(), SoundEvents.TRIDENT_RIPTIDE_1, 0.6f, 1.6f);
					}
				});
		HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.BUCKET_FILL, 0.8f, 1.5f);
	}

	/** Fires within {@code radius} of {@code centre} put out. */
	static void douse(ServerLevel level, BlockPos centre, int radius) {
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, radius, radius))) {
			if (level.getBlockState(pos).getBlock() instanceof BaseFireBlock) level.removeBlock(pos, false);
		}
	}

	// --- Foundation Building ---

	/**
	 * Rain Call: a rain cloud of the caster's own hangs where they cast it for {@link #RAIN_TICKS}; under it ({@link #RAIN_RADIUS}
	 * round) everyone but the caster is slowed by a tenth, and fires (and what burns) are put out.
	 */
	public static final class Rain extends HeavenSpells.Timed {
		private final Vec3 at;
		private final SpellField field;
		private final Set<LivingEntity> slowed = new HashSet<>();

		Rain(LivingEntity caster) {
			super(caster, RAIN_TICKS);
			Vec3 ground = HeavenSpells.groundBelow(level, caster.position(), 0, 16);
			this.at = ground != null ? ground : caster.position();
			this.field = SpellField.create(level, SpellField.Kind.RAIN_CLOUD, at.add(0, RAIN_HEIGHT, 0), (float) RAIN_RADIUS, (float) RAIN_HEIGHT, RAIN_TICKS, caster)
					.data(caster.getRandom().nextInt()).spawn();
			HeavenSpells.sound(level, at, SoundEvents.WEATHER_RAIN_ABOVE, 2.0f, 1.0f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age % 10 == 0) {
				Set<LivingEntity> under = new HashSet<>();
				for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(RAIN_RADIUS, RAIN_HEIGHT, RAIN_RADIUS),
						e -> e != caster && e.isAlive() && e.position().multiply(1, 0, 1).distanceToSqr(at.multiply(1, 0, 1)) <= RAIN_RADIUS * RAIN_RADIUS
								&& e.getY() >= at.y - 3 && e.getY() <= at.y + RAIN_HEIGHT && !HeavenSpells.walled(caster, e))) {
					under.add(e);
					if (slowed.add(e)) slow(e, true);
					if (e.isOnFire()) e.clearFire();
				}
				for (LivingEntity e : new ArrayList<>(slowed)) {
					if (!under.contains(e)) {
						slow(e, false);
						slowed.remove(e);
					}
				}
				if (age % 40 == 0) douse(level, BlockPos.containing(at), (int) RAIN_RADIUS);
			}
			if (age % 60 == 0) HeavenSpells.sound(level, at, SoundEvents.WEATHER_RAIN, 1.5f, 1.0f);
			return true;
		}

		private static void slow(LivingEntity e, boolean on) {
			AttributeInstance speed = e.getAttribute(Attributes.MOVEMENT_SPEED);
			if (speed == null) return;
			speed.removeModifier(RAIN_ID);
			if (on) speed.addTransientModifier(new AttributeModifier(RAIN_ID, "Rain Call", -RAIN_SLOW, AttributeModifier.Operation.MULTIPLY_TOTAL));
		}

		@Override
		public void end() {
			for (LivingEntity e : slowed) slow(e, false);
			field.discard();
		}
	}

	/**
	 * A being held in a sphere of water (Water Prison; Boundless Sea's foes): the air drawn out of it at once (no breath held: it
	 * drowns from the first second, 2 a second as drowning does, and {@code damage} of water qi besides), held where it is. Blows
	 * strike the sphere: {@code force} of harm in all bursts it, the prisoner's own from inside too.
	 */
	public static final class Prison extends HeavenSpells.Timed {
		private final Spell spell;
		public final LivingEntity target;
		private final float damage, force;
		private final SpellField field;
		private final WoodSpells.Bound bound;
		private float struck;

		Prison(LivingEntity caster, Spell spell, LivingEntity target, int ticks, float damage, float force, SpellField.Kind kind) {
			super(caster, ticks);
			this.spell = spell;
			this.target = target;
			this.damage = damage;
			this.force = force;
			float radius = Math.max(target.getBbWidth(), target.getBbHeight()) * 0.62f + 0.3f;
			this.field = SpellField.create(level, kind, HeavenSpells.middle(target).subtract(0, radius, 0), radius, radius * 2, ticks, caster).target(target)
					.data(caster.getRandom().nextInt()).strikeable(this::hit).spawn();
			this.bound = new WoodSpells.Bound(caster, target, target.position(), ticks);
			HeavenSpells.linger(bound);
			HeavenSpells.sound(level, target.position(), SoundEvents.GENERIC_SPLASH, 1.5f, 0.6f);
			HeavenSpells.sound(level, target.position(), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.5f, 1.0f);
			if (breathes(target)) target.setAirSupply(0);
		}

		private void hit(SpellField sphere, DamageSource source, float amount) {
			if (sphere.isRemoved()) return;
			struck += Math.max(1, amount);
			sphere.value(Math.max(0, 1 - struck / force));
			HeavenSpells.sound(level, sphere.position(), SoundEvents.GENERIC_SPLASH, 1.0f, 1.2f);
			if (struck >= force) end = level.getGameTime();
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(target, level) || field.isRemoved()) return false;
			field.setPos(target.getX(), HeavenSpells.middle(target).y - field.radius(), target.getZ());
			if (target.isOnFire()) target.clearFire();
			if (breathes(target)) {
				target.setAirSupply(0);
				if (age % 20 == 10) {
					target.invulnerableTime = 0;
					target.hurt(level.damageSources().drown(), 2.0f);
				}
			}
			if (age % 20 == 0 && damage > 0) HeavenSpells.strikeAgain(caster, target, damage, spell);
			return true;
		}

		@Override
		public void end() {
			SpellFx.send(level, SpellFx.Kind.SPLASH, field.position().add(0, field.radius(), 0), Vec3.ZERO, field.radius() * 1.5f, 0);
			HeavenSpells.sound(level, field.position(), SoundEvents.GENERIC_SPLASH, 1.5f, 0.7f);
			field.discard();
			HeavenSpells.stop(bound);
		}
	}

	private static void spring(LivingEntity p, Spell spell) {
		ServerLevel level = HeavenSpells.level(p);
		Vec3 gather = p.getEyePosition().add(Vec3.directionFromRotation(0, p.getYRot()).scale(4)).add(0, 4, 0);
		SpellField.create(level, SpellField.Kind.WATER_GATHER, gather, 2.2f, 2.2f, SPRING_GATHER, p).data(level.random.nextInt()).spawn();
		HeavenSpells.sound(level, gather, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 2.0f, 0.6f);
		HeavenSpells.sound(level, gather, SoundEvents.WATER_AMBIENT, 2.0f, 0.8f);
		HeavenSpells.linger(new FireSpells.Delayed(p, SPRING_GATHER, () -> {
			Vec3 to = HeavenSpells.lookPoint(p, SPRING_RANGE);
			Vec3 ground = HeavenSpells.groundBelow(level, to.add(0, 1, 0), 3, 24);
			Vec3 target = ground != null ? ground : to;
			Vec3 v = target.subtract(gather).scale(1.0 / 20);
			SpellProjectile.thrown(level, p, spell, SpellProjectile.Shape.WATER_BALL, 1.0f, gather, v, gather.distanceTo(target) + 3, new SpellProjectile.Behaviour() {
				private void land(Vec3 where) {
					SpellFx.send(level, SpellFx.Kind.SPLASH, where, Vec3.ZERO, 4.0f, 0, 160);
					HeavenSpells.sound(level, where, SoundEvents.GENERIC_SPLASH, 3.0f, 0.5f);
					HeavenSpells.sound(level, where, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 2.0f, 0.6f);
					for (LivingEntity e : HeavenSpells.aroundPoint(level, where, SPRING_RADIUS + 1, e -> !HeavenSpells.spared(p, e))) {
						soak(p, e, SPRING_DAMAGE, spell, null);
						HeavenSpells.push(where, e, 1.5);
					}
					lake(p, level, where);
				}

				@Override
				public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
					land(missile.position());
					return false;
				}

				@Override
				public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
					land(hit.getLocation());
				}

				@Override
				public void expire(SpellProjectile missile) {
					land(missile.position());
				}
			}).hitSize(2.5f).data(level.random.nextInt());
		}));
	}

	/** A small lake where the spring fell: a bowl {@link #LAKE_RADIUS} round pressed into the ground (or what is built on it), filled with water. */
	static void lake(LivingEntity p, ServerLevel level, Vec3 at) {
		BlockPos centre = BlockPos.containing(at.x, at.y - 0.5, at.z);
		int surface = centre.getY();
		List<BlockPos> fill = new ArrayList<>();
		for (int dx = -LAKE_RADIUS; dx <= LAKE_RADIUS; dx++) {
			for (int dz = -LAKE_RADIUS; dz <= LAKE_RADIUS; dz++) {
				double r = Math.sqrt(dx * dx + dz * dz);
				if (r > LAKE_RADIUS + 0.3) continue;
				int depth = r < LAKE_RADIUS - 1.2 ? 2 : 1;
				for (int dy = 0; dy > -depth; dy--) {
					BlockPos pos = new BlockPos(centre.getX() + dx, surface + dy, centre.getZ() + dz);
					BlockState state = level.getBlockState(pos);
					if (state.isAir() || state.canBeReplaced()) {
						// Water only where the caster may change the land (spawn protection, Adventure mode, a barrier's ground, the
						// Ghost Gate: Devastation#mayChange; the lake once filled past them all).
						if (Devastation.mayChange(p, level, pos)) fill.add(pos);
						continue;
					}
					if (HeavenSpells.carvable(level, pos, p)) {
						level.removeBlock(pos, false);
						fill.add(pos);
					}
				}
			}
		}
		// Water only where the bowl holds it: a cell with ground (or more of the lake) under it and on each side.
		for (BlockPos pos : fill) {
			if (Formations.shelters(level, pos) || !level.getBlockState(pos).canBeReplaced()) continue;
			boolean held = true;
			for (net.minecraft.core.Direction side : new net.minecraft.core.Direction[]{net.minecraft.core.Direction.DOWN, net.minecraft.core.Direction.NORTH,
					net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.WEST}) {
				BlockPos next = pos.relative(side);
				if (!fill.contains(next) && level.getBlockState(next).canBeReplaced() && level.getFluidState(next).isEmpty()) {
					held = false;
					break;
				}
			}
			if (held) level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
		}
	}

	// --- Core Formation ---

	/**
	 * Tidal Lance: a jet of water from the caster's hands for {@link #LANCE_TICKS}, as far as {@link #LANCE_RANGE} or the first wall
	 * or being: the first being in it takes {@link #LANCE_DAMAGE} every half second and is driven back along it.
	 */
	public static final class Lance extends HeavenSpells.Timed {
		private final Spell spell;
		private final SpellField field;

		Lance(LivingEntity caster, Spell spell) {
			super(caster, LANCE_TICKS);
			this.spell = spell;
			this.field = SpellField.create(level, SpellField.Kind.TIDAL_LANCE, HeavenSpells.fromHands(caster), 0.4f, 1, LANCE_TICKS, caster).spawn();
			HeavenSpells.sound(level, caster.position(), SoundEvents.TRIDENT_RIPTIDE_3, 1.2f, 0.8f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			Vec3 from = HeavenSpells.fromHands(caster);
			LivingEntity first = HeavenSpells.aimed(caster, LANCE_RANGE);
			Vec3 to = first != null ? HeavenSpells.middle(first) : HeavenSpells.beamEnd(caster, LANCE_RANGE);
			field.setPos(from.x, from.y, from.z);
			field.facing(caster.getYRot(), caster.getXRot()).value((float) from.distanceTo(to));
			if (first != null && age % 10 == 0) {
				soak(caster, first, LANCE_DAMAGE, spell, null);
				Vec3 way = to.subtract(from).normalize();
				HeavenSpells.move(first, first.getDeltaMovement().add(way.scale(0.8)).add(0, 0.1, 0));
				SpellFx.send(level, SpellFx.Kind.SPLASH, to, way.scale(-1), 0.8f, 0);
			}
			if (age % 8 == 0) HeavenSpells.sound(level, to, SoundEvents.WATER_AMBIENT, 1.0f, 1.6f);
			return true;
		}

		@Override
		public void end() {
			field.discard();
		}
	}

	/**
	 * Mist Veil: a thick mist {@link #MIST_RADIUS} round its caster for {@link #MIST_TICKS}: foes inside lose sight of the caster and
	 * their allies unless within three blocks, and missiles flying into it slow to half.
	 */
	public static final class Mist extends HeavenSpells.Timed {
		private final SpellField field;
		private final Set<Integer> slowedShots = new HashSet<>();

		Mist(LivingEntity caster) {
			super(caster, MIST_TICKS);
			this.field = SpellField.create(level, SpellField.Kind.MIST_VEIL, caster.position(), (float) MIST_RADIUS, 6, MIST_TICKS, caster)
					.data(caster.getRandom().nextInt()).spawn();
			HeavenSpells.sound(level, caster.position(), SoundEvents.FIRE_EXTINGUISH, 1.5f, 0.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			field.setPos(caster.getX(), caster.getY(), caster.getZ());
			if (age % 20 == 0) {
				for (Mob mob : level.getEntitiesOfClass(Mob.class, caster.getBoundingBox().inflate(MIST_RADIUS + 8), m -> m.getTarget() != null)) {
					LivingEntity target = mob.getTarget();
					if ((target == caster || HeavenSpells.ally(caster, target)) && target.distanceTo(caster) <= MIST_RADIUS && mob.distanceTo(target) > 3) {
						mob.setTarget(null);
						mob.getNavigation().stop();
					}
				}
			}
			for (Projectile shot : level.getEntitiesOfClass(Projectile.class, caster.getBoundingBox().inflate(MIST_RADIUS),
					s -> s.getOwner() != caster && !(s instanceof SpellProjectile missile && missile.getOwner() == caster))) {
				if (slowedShots.add(shot.getId())) {
					shot.setDeltaMovement(shot.getDeltaMovement().scale(0.5));
					shot.hurtMarked = true;
				}
			}
			return true;
		}

		@Override
		public void end() {
			field.discard();
		}
	}

	// --- Nascent Soul ---

	private static void spears(LivingEntity p, Spell spell) {
		for (int i = 0; i < 5; i++) {
			double side = (i - 2) * 0.9;
			double lift = 1.2 - Math.abs(i - 2) * 0.25;
			int spread = (i - 2) * 6;
			SpellProjectile spear = SpellProjectile.cast(p, spell, SpellProjectile.Shape.ICE_SPEAR, 1.0f, 10 + i * 2, new Vec3(side, lift, 0.6), SPEAR_SPEED,
					SPEAR_RANGE, new SpellProjectile.Behaviour() {
						@Override
						public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
							soak(p, target, SPEAR_DAMAGE, spell, missile);
							target.invulnerableTime = 0;
							target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
							target.setTicksFrozen(Math.max(target.getTicksFrozen(), 140));
							SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.ICE_BURST, missile.position(), Vec3.ZERO, 0.8f, 0);
							HeavenSpells.sound(HeavenSpells.level(p), target.position(), SoundEvents.GLASS_BREAK, 1.0f, 1.2f);
							return false;
						}

						@Override
						public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
							SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.ICE_BURST, hit.getLocation(), Vec3.ZERO, 0.8f, 0);
							HeavenSpells.sound(HeavenSpells.level(p), hit.getLocation(), SoundEvents.GLASS_BREAK, 0.8f, 1.3f);
						}

						@Override
						public void launched(SpellProjectile missile) {
							// A fan: each its own few degrees off the aim.
							missile.setDeltaMovement(missile.getDeltaMovement().yRot((float) Math.toRadians(spread)));
							HeavenSpells.sound(HeavenSpells.level(p), missile.position(), SoundEvents.TRIDENT_THROW, 0.8f, 1.4f);
						}
					});
			spear.data(i);
		}
		HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 1.6f);
	}

	/**
	 * Great Tide: a wave {@link #TIDE_HEIGHT} tall and {@link #TIDE_WIDTH} wide rolls out {@link #TIDE_LENGTH} the way its caster
	 * faces; whatever it overtakes takes {@link #TIDE_DAMAGE} once, is swept along hard and put out.
	 */
	public static final class Tide extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 from, way;
		private final SpellField field;
		private final Set<LivingEntity> struck = new HashSet<>();

		Tide(LivingEntity caster, Spell spell) {
			super(caster, (int) (TIDE_LENGTH / TIDE_PACE) + 10);
			this.spell = spell;
			this.from = caster.position();
			this.way = Vec3.directionFromRotation(0, caster.getYRot());
			this.field = SpellField.create(level, SpellField.Kind.GREAT_TIDE, from, (float) TIDE_WIDTH, (float) TIDE_HEIGHT, (int) (end - start), caster)
					.facing(caster.getYRot(), 0).data(caster.getRandom().nextInt()).spawn();
			HeavenSpells.sound(level, from, SoundEvents.ELDER_GUARDIAN_CURSE, 1.0f, 0.5f);
			HeavenSpells.sound(level, from, SoundEvents.AMBIENT_UNDERWATER_ENTER, 2.0f, 0.6f);
		}

		@Override
		protected boolean run(long age, long now) {
			double reach = Math.min(TIDE_LENGTH, age * TIDE_PACE);
			Vec3 front = from.add(way.scale(reach));
			field.setPos(front.x, from.y, front.z);
			Vec3 side = new Vec3(-way.z, 0, way.x);
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(front, front).inflate(TIDE_WIDTH / 2 + 2, TIDE_HEIGHT, TIDE_WIDTH / 2 + 2),
					e -> !HeavenSpells.spared(caster, e))) {
				Vec3 rel = e.position().subtract(front);
				if (Math.abs(rel.dot(side)) > TIDE_WIDTH / 2 || rel.dot(way) > 1.5 || rel.dot(way) < -3 || rel.y > TIDE_HEIGHT || rel.y < -3) continue;
				if (struck.add(e)) soak(caster, e, TIDE_DAMAGE, spell, null);
				HeavenSpells.move(e, way.scale(TIDE_PACE * 1.6).add(0, 0.25, 0));
			}
			if (age % 6 == 0) HeavenSpells.sound(level, front, SoundEvents.GENERIC_SPLASH, 1.5f, 0.6f);
			if (age % 4 == 0) WaterSpells.douse(level, BlockPos.containing(front), 3);
			return true;
		}

		@Override
		public void end() {
			field.discard();
		}
	}

	// --- Heavenly Being ---

	/**
	 * Heavenly River Torrent: a river pours out of the sky along a line from its caster to where they look ({@link #RIVER_LENGTH}),
	 * for {@link #RIVER_TICKS}: all in its fall take {@link #RIVER_DAMAGE} once and are swept along it.
	 */
	public static final class River extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 from, to;
		private final SpellField field;
		private final Set<LivingEntity> struck = new HashSet<>();

		River(LivingEntity caster, Spell spell) {
			super(caster, RIVER_TICKS);
			this.spell = spell;
			this.from = caster.position().add(Vec3.directionFromRotation(0, caster.getYRot()).scale(2));
			Vec3 end = HeavenSpells.lookPoint(caster, RIVER_LENGTH + 2);
			Vec3 ground = HeavenSpells.groundBelow(level, end.add(0, 1, 0), 2, 24);
			this.to = ground != null ? ground : end;
			Vec3 d = to.subtract(from);
			float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
			// Its pitch is the ground's slope along the line, so the client lands the falls on the ground at its far end too.
			float slope = (float) Math.toDegrees(Math.atan2(d.y, Math.max(1.0e-3, d.horizontalDistance())));
			this.field = SpellField.create(level, SpellField.Kind.RIVER_TORRENT, from, (float) RIVER_WIDTH, (float) d.horizontalDistance(), RIVER_TICKS, caster)
					.facing(yaw, slope).data(caster.getRandom().nextInt()).spawn();
			HeavenSpells.sound(level, from, SoundEvents.AMBIENT_UNDERWATER_ENTER, 3.0f, 0.4f);
			HeavenSpells.sound(level, to, SoundEvents.GENERIC_SPLASH, 3.0f, 0.4f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < 6) return true; // the river is still falling
			if (age == 6) Devastation.bore(caster, level, from.add(0, -1.2, 0), to.add(0, -1.2, 0), RIVER_SCOUR); // where it lands, it scours a channel
			Vec3 way = to.subtract(from).multiply(1, 0, 1);
			double length = way.length();
			Vec3 dir = way.scale(1 / Math.max(1.0e-3, length));
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(RIVER_WIDTH + 1, 6, RIVER_WIDTH + 1),
					e -> !HeavenSpells.spared(caster, e))) {
				Vec3 rel = e.position().subtract(from).multiply(1, 0, 1);
				double along = rel.dot(dir);
				if (along < -1 || along > length + 1 || rel.subtract(dir.scale(along)).length() > RIVER_WIDTH) continue;
				if (struck.add(e)) soak(caster, e, RIVER_DAMAGE, spell, null);
				HeavenSpells.move(e, dir.scale(0.9).add(0, -0.1, 0));
			}
			if (age % 5 == 0) {
				HeavenSpells.sound(level, from.add(dir.scale(length * level.random.nextDouble())), SoundEvents.GENERIC_SPLASH, 2.0f, 0.5f);
				WaterSpells.douse(level, BlockPos.containing(from.add(dir.scale(length / 2))), 2);
			}
			return true;
		}

		@Override
		public void end() {
			field.discard();
		}
	}

	/**
	 * Frozen Field: the cold runs out from its caster to {@link #FROZEN_RADIUS} over {@link #FREEZE_SPREAD} ticks: open water freezes
	 * (and frost lies on the ground within {@link #FROST_RADIUS}), and every being it reaches but the caster's own allies and pets is
	 * frozen solid for {@link #FROZEN_TICKS}. The ice thaws a block at a time over {@link #THAW_MIN} to {@link #THAW_MAX}, and until
	 * it has, a being in the field takes {@link #THAW_DAMAGE} of cold a second.
	 */
	public static final class Frozen extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 centre;
		private final SpellField field;
		private final Set<LivingEntity> frozen = new HashSet<>();
		private int reached;

		Frozen(LivingEntity caster, Spell spell) {
			super(caster, FREEZE_SPREAD + THAW_MAX);
			this.spell = spell;
			this.centre = caster.position();
			this.field = SpellField.create(level, SpellField.Kind.FROZEN_FIELD, centre, (float) FROZEN_RADIUS, 4, FREEZE_SPREAD + THAW_MAX, caster).spawn();
			HeavenSpells.sound(level, centre, SoundEvents.GLASS_BREAK, 3.0f, 0.4f);
			HeavenSpells.sound(level, centre, SoundEvents.POWDER_SNOW_STEP, 3.0f, 0.5f);
			HeavenSpells.sound(level, centre, SoundEvents.ELDER_GUARDIAN_CURSE, 1.5f, 1.4f);
		}

		private boolean spared(LivingEntity e) {
			return HeavenSpells.spared(caster, e) || HeavenSpells.ally(caster, e);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age <= FREEZE_SPREAD) {
				int radius = (int) Math.ceil(FROZEN_RADIUS * age / FREEZE_SPREAD);
				freezeRing(reached, radius);
				for (LivingEntity e : HeavenSpells.aroundPoint(level, centre, radius, e -> !spared(e) && !frozen.contains(e))) {
					frozen.add(e);
					SpellAuras.put(e, SpellAura.FROZEN, FROZEN_TICKS);
					e.setTicksFrozen(Math.max(e.getTicksFrozen(), 300));
					SpellFx.send(level, SpellFx.Kind.ICE_BURST, HeavenSpells.middle(e), Vec3.ZERO, 0.8f, 0);
				}
				reached = radius;
				if (age % 10 == 0) HeavenSpells.sound(level, centre, SoundEvents.GLASS_PLACE, 2.0f, 0.5f);
			}
			// Until the ice is gone, the cold bites whoever stays in the field.
			if (age % 20 == 10) {
				for (LivingEntity e : HeavenSpells.aroundPoint(level, centre, Math.min(FROZEN_RADIUS, reached), e -> !spared(e))) {
					HeavenSpells.strikeAgain(caster, e, THAW_DAMAGE, spell);
					e.setTicksFrozen(Math.max(e.getTicksFrozen(), 160));
				}
			}
			return true;
		}

		/** Freezes the open water between {@code inner} and {@code outer} round the centre; frost on the ground near. */
		private void freezeRing(int inner, int outer) {
			BlockPos c = BlockPos.containing(centre);
			for (int dx = -outer; dx <= outer; dx++) {
				for (int dz = -outer; dz <= outer; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d > outer || d <= inner) continue;
					int x = c.getX() + dx, z = c.getZ() + dz;
					if (!level.hasChunk(x >> 4, z >> 4)) continue;
					int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
					BlockPos pos = new BlockPos(x, top, z);
					BlockState state = level.getBlockState(pos);
					// A formation's barrier (a sect's dome) is invisible: frost laid on its shell hung in the air. Under it is the
					// formation's own ground, which the cold doesn't reach (HeavenSpells#placeTemporary: sheltered).
					if (state.is(ModBlocks.SECT_BARRIER)) continue;
					int thaw = THAW_MIN + level.random.nextInt(THAW_MAX - THAW_MIN);
					if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
						HeavenSpells.placeTemporary(level, pos, Blocks.ICE.defaultBlockState(), thaw);
					} else if (d <= FROST_RADIUS && state.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP)) {
						BlockPos above = pos.above();
						BlockState snow = Blocks.SNOW.defaultBlockState();
						// Only where snow could lie (as vanilla's own snowfall asks: not on a barrier, ice or the like).
						if (level.getBlockState(above).isAir() && snow.canSurvive(level, above)) HeavenSpells.placeTemporary(level, above, snow, thaw);
					}
				}
			}
		}

		@Override
		public void end() {
			for (LivingEntity e : frozen) SpellAuras.remove(e, SpellAura.FROZEN);
			field.discard();
		}
	}

	// --- Four Axis ---

	/**
	 * Absolute Zero: a beam of utter cold freezes the being looked at solid: {@link #ZERO_DAMAGE} of water, held still for
	 * {@link #ZERO_TICKS}; when anyone strikes it, or the cold lets go, it shatters for {@link #ZERO_SHATTER} more.
	 */
	public static final class Zero extends HeavenSpells.Timed implements HeavenSpells.HarmListener {
		private final Spell spell;
		public final LivingEntity target;
		private final WoodSpells.Bound bound;
		private boolean shattered;

		Zero(LivingEntity caster, Spell spell, LivingEntity target) {
			super(caster, ZERO_TICKS);
			this.spell = spell;
			this.target = target;
			SpellField.create(level, SpellField.Kind.FROST_BEAM, HeavenSpells.fromHands(caster), 0.3f, 1, 14, caster).target(target).spawn();
			soak(caster, target, ZERO_DAMAGE, spell, null);
			SpellAuras.put(target, SpellAura.FROZEN, ZERO_TICKS);
			target.setTicksFrozen(Math.max(target.getTicksFrozen(), 400));
			this.bound = new WoodSpells.Bound(caster, target, target.position(), ZERO_TICKS);
			HeavenSpells.linger(bound);
			HeavenSpells.sound(level, target.position(), SoundEvents.GLASS_BREAK, 2.0f, 0.4f);
			HeavenSpells.sound(level, caster.position(), SoundEvents.BEACON_DEACTIVATE, 1.5f, 1.6f);
		}

		@Override
		protected boolean run(long age, long now) {
			return !shattered && HeavenSpells.present(target, level);
		}

		@Override
		public float onIncoming(LivingEntity hurt, DamageSource source, float amount) {
			// A blow on the frozen shatters it (not the spell's own strike as it froze).
			if (hurt == target && !shattered && level.getGameTime() > start + 2 && !(source instanceof QiDamageSource qi && qi.element() == QiElement.WATER)) {
				shatter();
			}
			return amount;
		}

		private void shatter() {
			if (shattered) return;
			shattered = true;
			SpellAuras.remove(target, SpellAura.FROZEN);
			HeavenSpells.stop(bound);
			if (target.isAlive()) HeavenSpells.strikeAgain(caster, target, ZERO_SHATTER, spell);
			SpellFx.send(level, SpellFx.Kind.ICE_BURST, HeavenSpells.middle(target), Vec3.ZERO, 2.5f, 0);
			HeavenSpells.sound(level, target.position(), SoundEvents.GLASS_BREAK, 2.0f, 0.6f);
		}

		@Override
		public void end() {
			if (!shattered && target.isAlive()) shatter();
		}
	}

	/**
	 * Boundless Sea: a sphere of the sea {@link #SEA_RADIUS} round the point looked at for {@link #SEA_TICKS}: every foe inside is held
	 * in it, drowning (as a Water Prison) and taking {@link #SEA_DAMAGE} of water a second.
	 */
	public static final class Sea extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 at;
		private final SpellField field;
		private final Set<LivingEntity> held = new HashSet<>();

		Sea(LivingEntity caster, Spell spell) {
			super(caster, SEA_TICKS);
			this.spell = spell;
			LivingEntity aimed = HeavenSpells.aimed(caster, SEA_RANGE);
			this.at = aimed != null ? HeavenSpells.middle(aimed) : HeavenSpells.lookPoint(caster, SEA_RANGE);
			this.field = SpellField.create(level, SpellField.Kind.BOUNDLESS_SEA, at, (float) SEA_RADIUS, (float) SEA_RADIUS * 2, SEA_TICKS, caster)
					.data(caster.getRandom().nextInt()).spawn();
			HeavenSpells.sound(level, at, SoundEvents.AMBIENT_UNDERWATER_ENTER, 4.0f, 0.4f);
			HeavenSpells.sound(level, at, SoundEvents.ELDER_GUARDIAN_CURSE, 2.0f, 0.4f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age % 5 == 0) {
				for (LivingEntity e : HeavenSpells.aroundPoint(level, at.subtract(0, 1, 0), SEA_RADIUS, e -> HeavenSpells.mark(caster, e))) {
					if (held.add(e)) HeavenSpells.linger(new WoodSpells.Bound(caster, e, e.position(), (int) (end - level.getGameTime())));
				}
			}
			for (LivingEntity e : held) {
				if (!HeavenSpells.present(e, level)) continue;
				if (e.isOnFire()) e.clearFire();
				if (breathes(e)) {
					e.setAirSupply(0);
					if (age % 20 == 10) {
						e.invulnerableTime = 0;
						e.hurt(level.damageSources().drown(), 2.0f);
					}
				}
				if (age % 10 == 5) HeavenSpells.strikeAgain(caster, e, SEA_DAMAGE / 2, spell);
			}
			if (age % 30 == 0) HeavenSpells.sound(level, at, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 3.0f, 0.5f);
			return true;
		}

		@Override
		public void end() {
			SpellFx.send(level, SpellFx.Kind.SPLASH, at, Vec3.ZERO, (float) SEA_RADIUS, 0, 192);
			field.discard();
			// The sea's weight leaves the ground under it crushed into a basin, and a lake in it.
			Vec3 ground = HeavenSpells.groundBelow(level, at, 2, (int) SEA_RADIUS + 4);
			if (ground != null) Devastation.crater(caster, level, ground, SEA_BASIN, SEA_BASIN * 0.45, Devastation.Floor.LAKE);
		}
	}

	// --- Dao Integration ---

	/**
	 * Black Tortoise Bastion: the Black Tortoise of the north (SpellConstruct BLACK_TORTOISE, its own GeckoLib model: its shell a dome of
	 * warding light) rises round its caster and keeps by them {@link #TORTOISE_TICKS}: they and their own within {@link #TORTOISE_RADIUS}
	 * are under its shell (SpellAura BLACK_TORTOISE, renewed while they stay in it), and every {@link #TORTOISE_STRIKE_EVERY} ticks its
	 * serpent strikes the nearest foe within {@link #TORTOISE_REACH} for {@link #TORTOISE_DAMAGE}, throwing it back, the tortoise turning
	 * to face it.
	 */
	public static final class Tortoise extends HeavenSpells.Timed {
		private final Spell spell;
		private final SpellConstruct tortoise;
		private float yaw;
		/** The foe its serpent is striking (it lands {@link #TORTOISE_LUNGE} after it rears back), and when it began. */
		private LivingEntity striking;
		private long struckAt;

		Tortoise(LivingEntity caster, Spell spell) {
			super(caster, TORTOISE_TICKS);
			this.spell = spell;
			this.yaw = caster.getYRot();
			this.tortoise = SpellConstruct.create(level, SpellConstruct.Kind.BLACK_TORTOISE, caster.position(), yaw, 0, TORTOISE_TICKS, caster).sizedFor(spell).spawn();
			HeavenSpells.sound(level, caster.position(), SoundEvents.CONDUIT_ACTIVATE, 2.0f, 0.5f);
			HeavenSpells.sound(level, caster.position(), SoundEvents.TURTLE_AMBIENT_LAND, 2.0f, 0.4f);
			SpellFx.send(level, SpellFx.Kind.SPLASH, caster.position(), Vec3.ZERO, (float) TORTOISE_RADIUS, 0);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			// Its shell wards its caster and their own while they stay within it.
			if (age % 10 == 0) {
				for (LivingEntity e : HeavenSpells.around(caster, TORTOISE_RADIUS, e -> HeavenSpells.blessed(caster, e) || HeavenSpells.ally(caster, e))) {
					SpellAuras.put(e, SpellAura.BLACK_TORTOISE, 15);
				}
			}
			// Its serpent strikes the nearest foe within reach, once the tortoise has risen (struck as it rose, it was cut short in its rising).
			if (striking == null && age >= TORTOISE_RISEN && age % TORTOISE_STRIKE_EVERY == TORTOISE_RISEN % TORTOISE_STRIKE_EVERY) {
				LivingEntity foe = null;
				for (LivingEntity e : HeavenSpells.around(caster, TORTOISE_REACH, e -> e != caster && HeavenSpells.mark(caster, e))) {
					if (foe == null || e.distanceToSqr(caster) < foe.distanceToSqr(caster)) foe = e;
				}
				if (foe != null) {
					striking = foe;
					struckAt = age;
					tortoise.act("strike");
					HeavenSpells.sound(level, caster.position(), SoundEvents.HOGLIN_ATTACK, 2.0f, 1.4f);
				}
			}
			if (striking != null && age >= struckAt + TORTOISE_LUNGE) {
				if (HeavenSpells.present(striking, level) && striking.distanceTo(caster) <= TORTOISE_REACH + 4) {
					HeavenSpells.strike(caster, striking, TORTOISE_DAMAGE, spell);
					HeavenSpells.push(caster.position(), striking, 1.6);
					SpellFx.send(level, SpellFx.Kind.SPLASH, HeavenSpells.middle(striking), Vec3.ZERO, 2.0f, 0);
					HeavenSpells.sound(level, striking.position(), SoundEvents.GENERIC_SPLASH, 2.0f, 0.6f);
				}
				striking = null;
			}
			// It keeps by its caster, turning to face the foe its serpent strikes, else the way its caster faces.
			float want = striking != null ? HeavenSpells.facing(striking.position().subtract(caster.position()))[0] : caster.getYRot();
			yaw = net.minecraft.util.Mth.approachDegrees(yaw, want, 14);
			tortoise.place(caster.position(), yaw, 0);
			if (age % 40 == 0) HeavenSpells.sound(level, caster.position(), SoundEvents.CONDUIT_AMBIENT, 1.5f, 0.6f);
			return true;
		}

		@Override
		public void end() {
			tortoise.dissolve();
		}
	}

	/**
	 * Hundred Rivers Converge: a hundred streams pour together into a great whirlpool (SpellConstruct HUNDRED_RIVERS) where its caster
	 * looks, for {@link #RIVERS_TICKS}: every foe within {@link #RIVERS_RADIUS} is dragged round and into it and held, taking
	 * {@link #RIVERS_DAMAGE} a second. As it turns its rivers pour in from its rim, flooding the whole of its reach ({@link #RIVERS_POUR}
	 * ticks: Devastation#flood, the land scoured into a basin full to the brim; the user's ask, 2026-10-24, the water out during the spell
	 * over all it covers, not a lake at its heart after). When it bursts it flings them out, and the lake stays.
	 */
	public static final class Rivers extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final SpellConstruct vortex;
		private final Set<LivingEntity> held = new HashSet<>();
		/** How far in from its rim its rivers have poured (the flood's front, closing on its heart). */
		private double poured = RIVERS_BASIN * 1.1;

		Rivers(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, RIVERS_TICKS + RIVERS_BURST);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 32);
			this.ground = below != null ? below : point;
			this.vortex = SpellConstruct.create(level, SpellConstruct.Kind.HUNDRED_RIVERS, ground, caster.getYRot(), 0, RIVERS_TICKS + RIVERS_BURST, caster).sizedFor(spell).spawn();
			HeavenSpells.sound(level, ground, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 4.0f, 0.4f);
			HeavenSpells.sound(level, ground, SoundEvents.GENERIC_SPLASH, 4.0f, 0.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			// Its rivers pour in from its rim as it turns, flooding the whole of its reach, the land under it scoured into a basin.
			if (age % 5 == 0 && poured > 0) {
				double in = RIVERS_BASIN * Math.max(0, 1 - (age + 5) / (double) RIVERS_POUR);
				Devastation.flood(caster, level, ground, RIVERS_BASIN, RIVERS_BASIN_DEPTH, in, poured);
				poured = in;
				for (int k = 0; k < 10; k++) { // the streams running in
					double a = Math.PI * 2 * k / 10 + age * 0.05;
					SpellFx.send(level, SpellFx.Kind.SPLASH, ground.add(Math.cos(a) * (in + 1.5), 0.5, Math.sin(a) * (in + 1.5)), Vec3.ZERO, 2.0f, 0, 160);
				}
			}
			if (age < RIVERS_TICKS) {
				// Dragged round and in, and held off the ground at its heart.
				for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, RIVERS_RADIUS + 8, e -> HeavenSpells.mark(caster, e))) {
					Vec3 rel = e.position().subtract(ground).multiply(1, 0, 1);
					double dist = rel.length();
					if (dist > RIVERS_RADIUS || e.getY() > ground.y + 16 || e.getY() < ground.y - 6) continue;
					held.add(e);
					if (HeavenSpells.greatBeast(e)) continue;
					Vec3 in = dist < 1.0e-3 ? Vec3.ZERO : rel.scale(-1 / dist), round = new Vec3(-in.z, 0, in.x);
					double lift = (ground.y + 1.5 - e.getY()) * 0.15;
					HeavenSpells.move(e, in.scale(Math.min(dist, 0.55)).add(round.scale(0.45 * Math.min(1, dist / 4))).add(0, lift, 0));
					e.resetFallDistance();
				}
				if (age % 20 == 10) {
					for (LivingEntity e : held) {
						if (!HeavenSpells.present(e, level) || e.position().distanceTo(ground) > RIVERS_RADIUS + 6) continue;
						HeavenSpells.strikeAgain(caster, e, RIVERS_DAMAGE, spell);
						SpellFx.send(level, SpellFx.Kind.SPLASH, HeavenSpells.middle(e), Vec3.ZERO, 1.2f, 0);
					}
					HeavenSpells.sound(level, ground, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 4.0f, 0.5f);
				}
				return true;
			}
			if (age == RIVERS_TICKS) {
				// It bursts: those it held flung out; the flood it poured in stays, a lake in its basin.
				vortex.act("burst");
				for (LivingEntity e : held) {
					if (!HeavenSpells.present(e, level) || HeavenSpells.greatBeast(e)) continue;
					Vec3 out = e.position().subtract(ground).multiply(1, 0, 1);
					out = out.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : out.normalize();
					HeavenSpells.move(e, out.scale(2.4).add(0, 1.2, 0));
					HeavenSpells.safeFall(e, 80);
				}
				SpellFx.send(level, SpellFx.Kind.SPLASH, ground, Vec3.ZERO, (float) RIVERS_RADIUS, 0, 192);
				SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) RIVERS_RADIUS * 1.4f, 0, 192);
				HeavenSpells.sound(level, ground, SoundEvents.GENERIC_EXPLODE, 3.0f, 0.6f);
				HeavenSpells.sound(level, ground, SoundEvents.GENERIC_SPLASH, 4.0f, 0.4f);
			}
			return true;
		}

		@Override
		public void end() {
			vortex.dissolve();
		}
	}

	// --- Void Shattering ---

	/**
	 * Weak Water from the Void (弱水, the water that bears nothing up, not even a feather): space tears open {@link #WEAK_HIGH} over where its
	 * caster looks (SpellConstruct VOID_RIFT, the Weak Water's pale blue, lying flat across their sight, open below: the user's ask, 2026-10-24,
	 * so the water falls out of it, not past an upright slit) and the Weak Water pours out of it onto the land (WEAK_WATER_FALL, its
	 * own GeckoLib model) for {@link #WEAK_TICKS}: everything within {@link #WEAK_RADIUS} of where it falls sinks (nothing flies in it, the
	 * dead neither: QiFlight#weighDown), is slowed, its fires put out, and takes {@link #WEAK_DAMAGE} a second. As it pours its flood runs out
	 * from where it falls over the whole of that reach ({@link #WEAK_SPREAD} ticks), the land sinking under it into a basin full to the brim
	 * (Devastation#flood: the user's ask, 2026-10-24, the water out during the spell over all it covers, not a lake at its heart after),
	 * and the lake stays.
	 */
	public static final class WeakWater extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground, mouth;
		private final SpellConstruct rift;
		private SpellConstruct fall;
		/** How far out from where it falls the flood has spread. */
		private double flooded;

		WeakWater(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, WEAK_OPENS + WEAK_TICKS + SpellConstruct.FADE);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.mouth = ground.add(0, WEAK_HIGH, 0);
			this.rift = SpellConstruct.create(level, SpellConstruct.Kind.VOID_RIFT, mouth, caster.getYRot() + 90, 90, (int) (end - start), caster).size((float) WEAK_RIFT)
					.tint(WEAK_PALE).spawn();
			HeavenSpells.sound(level, mouth, SoundEvents.END_PORTAL_SPAWN, 3.0f, 0.7f);
			HeavenSpells.sound(level, mouth, SoundEvents.CONDUIT_ACTIVATE, 3.0f, 0.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < WEAK_OPENS) return true;
			long pouring = age - WEAK_OPENS;
			if (pouring == 0) {
				fall = SpellConstruct.create(level, SpellConstruct.Kind.WEAK_WATER_FALL, ground, caster.getYRot(), 0, WEAK_TICKS + SpellConstruct.FADE, caster).sizedFor(spell).spawn();
				HeavenSpells.sound(level, ground, SoundEvents.GENERIC_SPLASH, 4.0f, 0.3f);
				HeavenSpells.sound(level, mouth, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 4.0f, 0.4f);
			}
			// The flood spreads out from where it falls as it pours, the land sinking under it, till it covers the whole of its reach.
			if (pouring % 5 == 0 && flooded < WEAK_BASIN) {
				double out = WEAK_BASIN * Math.min(1, (pouring + 5) / (double) WEAK_SPREAD);
				Devastation.flood(caster, level, ground, WEAK_BASIN, WEAK_BASIN_DEPTH, flooded, out);
				flooded = out;
				for (int k = 0; k < 8; k++) { // its front breaking as it runs out
					double a = Math.PI * 2 * k / 8 + pouring * 0.3;
					SpellFx.send(level, SpellFx.Kind.SPLASH, ground.add(Math.cos(a) * out, 0.5, Math.sin(a) * out), Vec3.ZERO, 2.5f, 0, 160);
				}
			}
			if (pouring == WEAK_TICKS - 16) fall.act("dry");
			if (pouring >= WEAK_TICKS) return true;
			boolean harm = pouring % 20 == 10;
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, WEAK_RADIUS + WEAK_HIGH, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > WEAK_RADIUS || e.getY() > mouth.y || e.getY() < ground.y - 16) continue;
				// Nothing is borne up by it: down they go, and nothing flies in it.
				if (e instanceof net.minecraft.server.level.ServerPlayer player) QiFlight.weighDown(player, 10);
				if (e instanceof CultivatorNpc npc && npc.isQiFlying()) npc.setQiFlying(false);
				if (!HeavenSpells.greatBeast(e)) {
					Vec3 v = e.getDeltaMovement();
					HeavenSpells.move(e, new Vec3(v.x * 0.6, Math.min(v.y, -0.45), v.z * 0.6));
				}
				e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
				if (e.isOnFire()) e.clearFire();
				if (harm) {
					e.invulnerableTime = 0;
					soak(caster, e, WEAK_DAMAGE, spell, null);
					SpellFx.send(level, SpellFx.Kind.SPLASH, HeavenSpells.middle(e), Vec3.ZERO, 1.2f, 0);
				}
			}
			if (pouring % 10 == 0) SpellFx.send(level, SpellFx.Kind.SPLASH, ground, Vec3.ZERO, (float) WEAK_RADIUS * 0.6f, 0, 160);
			if (pouring % 20 == 0) HeavenSpells.sound(level, ground, SoundEvents.WATER_AMBIENT, 4.0f, 0.4f);
			return true;
		}

		@Override
		public void end() {
			rift.dissolve();
			if (fall != null) fall.dissolve();
		}
	}

	/**
	 * Kun Rises from the Northern Abyss: the land where its caster looks opens onto the northern abyss (SpellConstruct ABYSS_RIFT, its deep
	 * blue), and the Kun, the fish of the northern abyss that becomes the Peng, made of the abyss's black water (KUN, its own GeckoLib model),
	 * breaches out of it, leaping {@link #KUN_LEAP} high across the caster's sight and diving back in ({@link #KUN_LEAPS} ticks): as it breaches,
	 * everything within {@link #KUN_RADIUS} takes {@link #KUN_DAMAGE} and is thrown high; as it dives back, {@link #KUN_DIVE_DAMAGE} more, and
	 * the land there is crushed into a basin with a lake in it. Then the abyss closes.
	 */
	public static final class Kun extends HeavenSpells.Timed {
		private static final double FROM = -0.95, TO = Math.PI + 0.95, BREACH = -0.45, DIVE = Math.PI + 0.45;
		private final Spell spell;
		private final Vec3 ground, across;
		private final SpellConstruct abyss;
		private SpellConstruct kun;
		private boolean breached, dived;

		Kun(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, KUN_OPENS + KUN_LEAPS + KUN_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			// It leaps across the caster's sight, from their left to their right: seen from the side, whole.
			Vec3 look = Vec3.directionFromRotation(0, caster.getYRot());
			this.across = new Vec3(-look.z, 0, look.x);
			this.abyss = SpellConstruct.create(level, SpellConstruct.Kind.ABYSS_RIFT, ground.add(0, 0.06, 0), HeavenSpells.facing(across)[0], 0, (int) (end - start), caster)
					.size((float) KUN_ABYSS).tint(ABYSS_BLUE).spawn();
			HeavenSpells.sound(level, ground, SoundEvents.END_PORTAL_SPAWN, 3.0f, 0.4f);
			HeavenSpells.sound(level, ground, SoundEvents.ELDER_GUARDIAN_AMBIENT, 3.0f, 0.5f);
		}

		/** How far round its leap it has come {@code f} of the way through it: swifter out of the abyss and back into it, hanging at the top. */
		private static double angle(double f) {
			double eased = f + 0.35 * Math.sin(2 * Math.PI * f) / (2 * Math.PI);
			return FROM + (TO - FROM) * eased;
		}

		/** Where its body's middle is at {@code angle} round its leap. */
		private Vec3 at(double angle) {
			return ground.add(across.scale(-KUN_SWING * Math.cos(angle))).add(0, KUN_LEAP * Math.sin(angle), 0);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < KUN_OPENS) return true;
			long leaping = age - KUN_OPENS;
			if (leaping > KUN_LEAPS) return true;
			double a = angle(leaping / (double) KUN_LEAPS);
			Vec3 here = at(a);
			float[] face = HeavenSpells.facing(at(angle((leaping + 1) / (double) KUN_LEAPS)).subtract(here));
			if (leaping == 0) {
				kun = SpellConstruct.create(level, SpellConstruct.Kind.KUN, here, face[0], face[1], KUN_LEAPS + SpellConstruct.FADE, caster).sizedFor(spell).spawn();
				HeavenSpells.sound(level, ground, SoundEvents.ELDER_GUARDIAN_CURSE, 3.0f, 0.4f);
			}
			kun.place(here, face[0], face[1]);
			if (!breached && a >= BREACH) {
				breached = true;
				strike(KUN_DAMAGE, true);
			}
			if (!dived && a >= DIVE) {
				dived = true;
				strike(KUN_DIVE_DAMAGE, false);
				Devastation.crater(caster, level, ground, KUN_BASIN, KUN_BASIN_DEPTH, Devastation.Floor.LAKE);
			}
			if (leaping % 4 == 0 && here.y > ground.y - 2) SpellFx.send(level, SpellFx.Kind.SPLASH, here, Vec3.ZERO, 4, 0, 192);
			return true;
		}

		/** It breaks out of the abyss (thrown high) or crashes back into it: all near it struck. */
		private void strike(float damage, boolean breach) {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, KUN_RADIUS + 16, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > KUN_RADIUS || e.getY() > ground.y + (breach ? 20 : 40) || e.getY() < ground.y - 12) continue;
				e.invulnerableTime = 0;
				soak(caster, e, damage, spell, null);
				if (HeavenSpells.greatBeast(e)) continue;
				Vec3 out = e.position().subtract(ground).multiply(1, 0, 1);
				out = out.lengthSqr() < 1.0e-4 ? across : out.normalize();
				HeavenSpells.move(e, breach ? out.scale(0.7).add(0, 2.4, 0) : out.scale(1.8).add(0, 0.6, 0));
				HeavenSpells.safeFall(e, 100);
			}
			SpellFx.send(level, SpellFx.Kind.SPLASH, ground, Vec3.ZERO, (float) KUN_RADIUS, 0, 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) KUN_RADIUS * 1.6f, 0, 192);
			HeavenSpells.sound(level, ground, SoundEvents.GENERIC_SPLASH, 5.0f, 0.3f);
			HeavenSpells.sound(level, ground, breach ? SoundEvents.ELDER_GUARDIAN_CURSE : SoundEvents.GENERIC_EXPLODE, 4.0f, 0.4f);
		}

		@Override
		public void end() {
			abyss.dissolve();
			if (kun != null) kun.dissolve();
		}
	}

	static void tick(MinecraftServer server) {}

	static void forget(UUID id) {}

	static void clear() {}

	private WaterSpells() {}
}
