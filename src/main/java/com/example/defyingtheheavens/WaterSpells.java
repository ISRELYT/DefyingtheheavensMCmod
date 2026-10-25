package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
	/**
	 * Twenty-Four Sea-Fixing Pearls: how many, how far round their caster they reach for foes (64 by its realm), how far out they circle
	 * (7), and how great each is drawn, of its realm's greatness (a pearl two blocks round: twenty-four of them a ring, not one white heap);
	 * each pearl's harm and how far round where it bursts (4); its little crater (3 round, 2 deep, by its realm); how long they take to unwind
	 * round their caster, how far apart they fly, and how long each flies (ticks).
	 */
	public static final int PEARLS = 24;
	public static final float PEARL_SIZE = 0.5f;
	public static final double PEARLS_REACH = HeavenSpells.span(Realm.SACRED_VESSEL, 30), PEARLS_CIRCLE = HeavenSpells.span(Realm.SACRED_VESSEL, 3.4),
			PEARL_BLAST = HeavenSpells.span(Realm.SACRED_VESSEL, 2), PEARL_CRATER = Devastation.sized(Realm.SACRED_VESSEL, 1.5),
			PEARL_CRATER_DEPTH = Devastation.sized(Realm.SACRED_VESSEL, 1);
	public static final float PEARL_DAMAGE = 140;
	public static final int PEARLS_FORM = 16, PEARL_EVERY = 2, PEARL_FLIGHT = 10;
	/**
	 * Ocean-Sealing Vase: where (up to {@link #VASE_RANGE}, 136), how high its mouth hangs (30), how far round it drinks (43: foes, and every
	 * drop of water in the land, down to 13 under it) and how near its mouth what it draws is held (6); its crush a second, and its slam as it
	 * comes down sealed (within 21); its crater (17 round, 6 deep, a lake poured back into it, by its realm); how long it takes to come, how long
	 * it drinks, how long it takes to seal, and how long it falls (ticks).
	 */
	public static final double VASE_RANGE = HeavenSpells.span(Realm.SACRED_VESSEL, 64), VASE_HIGH = HeavenSpells.span(Realm.SACRED_VESSEL, 14),
			VASE_RADIUS = HeavenSpells.span(Realm.SACRED_VESSEL, 20), VASE_MOUTH = HeavenSpells.span(Realm.SACRED_VESSEL, 3),
			VASE_SLAM_RADIUS = HeavenSpells.span(Realm.SACRED_VESSEL, 10), VASE_CRATER = Devastation.sized(Realm.SACRED_VESSEL, 8),
			VASE_CRATER_DEPTH = Devastation.sized(Realm.SACRED_VESSEL, 3);
	public static final int VASE_DRAIN_DOWN = (int) Math.round(Devastation.sized(Realm.SACRED_VESSEL, 6));
	public static final float VASE_CRUSH = 400, VASE_SLAM = 1500;
	public static final int VASE_COMES = 20, VASE_TICKS = 5 * 20, VASE_SEALS = 12, VASE_FALL = 10;
	/**
	 * Frozen Moon in the Well: where (154 by its realm), how far round the well of its reflection reaches (18), how far round the land freezes
	 * (24); what it does a second to every foe held frozen in it, and its shattering's harm; how long it rises, how long it holds them, and how
	 * long it has after it shatters (ticks).
	 */
	public static final double WELL_RANGE = HeavenSpells.span(Realm.ENTERING_NIRVANA, 64), WELL_RADIUS = HeavenSpells.span(Realm.ENTERING_NIRVANA, 7.5),
			WELL_FROST = HeavenSpells.span(Realm.ENTERING_NIRVANA, 10);
	public static final float WELL_DAMAGE = 400, WELL_SHATTER = 3200;
	public static final int WELL_RISES = 24, WELL_TICKS = 4 * 20, WELL_AFTER = 22;
	/** The frozen moon's light: its ice glows so. */
	public static final int MOON_LIGHT = 0xBEEFFF;
	/**
	 * The Frozen Moon Falls: where (154 by its realm), how high it falls from (96); whom it strikes as it lands (within 24) and how hard; how
	 * far round its frost reaches (38); its crater of ice (29 round, 14 deep, by its realm); how long it falls, how long its frost chills, and how
	 * long it has after (ticks).
	 */
	public static final double FALLS_RANGE = HeavenSpells.span(Realm.ENTERING_NIRVANA, 64), FALLS_HIGH = HeavenSpells.span(Realm.ENTERING_NIRVANA, 40),
			FALLS_RADIUS = HeavenSpells.span(Realm.ENTERING_NIRVANA, 10), FALLS_FROST = HeavenSpells.span(Realm.ENTERING_NIRVANA, 16),
			FALLS_CRATER = Devastation.sized(Realm.ENTERING_NIRVANA, 12), FALLS_CRATER_DEPTH = Devastation.sized(Realm.ENTERING_NIRVANA, 6);
	public static final float FALLS_DAMAGE = 6500;
	public static final int FALLS_FALL = 30, FALLS_CHILLS = 8 * 20, FALLS_AFTER = 24;
	/** The ice moon's round (blocks). */
	public static final double FALLS_MOON = HeavenSpells.span(Realm.ENTERING_NIRVANA, 2.6);
	/**
	 * Glacier of the First Age: how far ahead of its caster it rises (14), how far it creeps on (102), half its width (24), how high its seracs
	 * stand (about 14 over its level), how far back its body runs behind its face (blocks), how deep down into a hollow its ice reaches; its harm
	 * each time it grinds, and as its face calves, to everything within the reach of its falling slabs ahead (20); how long it takes to rise, how
	 * long it creeps on, how often it strikes, how long it calves, and how long it has after (ticks); how far ahead of its face it carves the
	 * land, how many slices of its face topple as it calves and how many pieces are seen falling, how long its sheet of ice lasts after it
	 * (ticks), and how far down it looks for the ground under one casting it from the air.
	 */
	public static final double GLACIER_START = HeavenSpells.span(Realm.TRUE_IMMORTAL, 4), GLACIER_REACH = HeavenSpells.span(Realm.TRUE_IMMORTAL, 30),
			GLACIER_HALF = HeavenSpells.span(Realm.TRUE_IMMORTAL, 7), GLACIER_HIGH = HeavenSpells.span(Realm.TRUE_IMMORTAL, 4),
			GLACIER_CALVE_REACH = HeavenSpells.span(Realm.TRUE_IMMORTAL, 6);
	public static final int GLACIER_BODY = 9, GLACIER_FILLS = 12;
	public static final float GLACIER_GRIND = 2500, GLACIER_CALVE = 10000;
	public static final int GLACIER_RISES = 20, GLACIER_TICKS = 10 * 20, GLACIER_EVERY = 10, GLACIER_CALVES = 20, GLACIER_AFTER = 16;
	public static final int GLACIER_CARVE_AHEAD = 3, GLACIER_CALVE_SLICES = 3, GLACIER_CALVE_PIECES = 90, GLACIER_THAWS = 30 * 20, GLACIER_FINDS_GROUND = 96;
	/**
	 * The Primordial Sea: where (218 by its realm), how far round its ring of waves stands (39: its model's); the lake it leaves (27 round, 9
	 * deep, by its realm); what it drowns every foe within of each time, and its waves' crash; how long it takes to rise, how long it churns, how
	 * often it drowns, how long its waves take to crash in, and how long it has after (ticks).
	 */
	public static final double PRIMAL_SEA_RANGE = HeavenSpells.span(Realm.TRUE_IMMORTAL, 64), PRIMAL_SEA_RADIUS = HeavenSpells.span(Realm.TRUE_IMMORTAL, 11.5),
			PRIMAL_SEA_LAKE = Devastation.sized(Realm.TRUE_IMMORTAL, 8), PRIMAL_SEA_LAKE_DEPTH = Devastation.sized(Realm.TRUE_IMMORTAL, 2.5);
	public static final float PRIMAL_SEA_DROWN = 3000, PRIMAL_SEA_CRASH = 45000;
	public static final int PRIMAL_SEA_RISES = 28, PRIMAL_SEA_TICKS = 5 * 20, PRIMAL_SEA_EVERY = 10, PRIMAL_SEA_CLOSES = 16, PRIMAL_SEA_AFTER = 20;

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
			case TWENTY_FOUR_SEA_FIXING_PEARLS -> {
				HeavenSpells.endLingering(p.getUUID(), Pearls.class);
				HeavenSpells.linger(new Pearls(p, spell));
			}
			case OCEAN_SEALING_VASE -> {
				LivingEntity aimed = HeavenSpells.aimed(p, VASE_RANGE);
				HeavenSpells.linger(new Vase(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, VASE_RANGE)));
			}
			case FROZEN_MOON_IN_THE_WELL -> {
				LivingEntity aimed = HeavenSpells.aimed(p, WELL_RANGE);
				HeavenSpells.linger(new MoonWell(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, WELL_RANGE)));
			}
			case THE_FROZEN_MOON_FALLS -> {
				LivingEntity aimed = HeavenSpells.aimed(p, FALLS_RANGE);
				HeavenSpells.linger(new MoonFalls(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, FALLS_RANGE)));
			}
			case GLACIER_OF_THE_FIRST_AGE -> HeavenSpells.linger(new Glacier(p, spell));
			case THE_PRIMORDIAL_SEA -> {
				HeavenSpells.endLingering(p.getUUID(), PrimalSea.class);
				LivingEntity aimed = HeavenSpells.aimed(p, PRIMAL_SEA_RANGE);
				HeavenSpells.linger(new PrimalSea(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, PRIMAL_SEA_RANGE)));
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
			this.at = HeavenSpells.footing(caster);
			this.field = SpellField.create(level, SpellField.Kind.RAIN_CLOUD, at.add(0, RAIN_HEIGHT, 0), (float) RAIN_RADIUS, (float) RAIN_HEIGHT, RAIN_TICKS, caster)
					.data(caster.getRandom().nextInt()).spawn();
			sound(at, SoundEvents.WEATHER_RAIN_ABOVE, 2.0f, 1.0f);
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
			if (age % 60 == 0) sound(at, SoundEvents.WEATHER_RAIN, 1.5f, 1.0f);
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
			sound(target.position(), SoundEvents.GENERIC_SPLASH, 1.5f, 0.6f);
			sound(target.position(), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.5f, 1.0f);
			if (breathes(target)) target.setAirSupply(0);
		}

		private void hit(SpellField sphere, DamageSource source, float amount) {
			if (sphere.isRemoved()) return;
			struck += Math.max(1, amount);
			sphere.value(Math.max(0, 1 - struck / force));
			sound(sphere.position(), SoundEvents.GENERIC_SPLASH, 1.0f, 1.2f);
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
			sound(field.position(), SoundEvents.GENERIC_SPLASH, 1.5f, 0.7f);
			field.discard();
			HeavenSpells.stop(bound);
		}
	}

	private static void spring(LivingEntity p, Spell spell) {
		ServerLevel level = HeavenSpells.level(p);
		Vec3 gather = p.getEyePosition().add(Vec3.directionFromRotation(0, p.getYRot()).scale(4)).add(0, 4, 0);
		SpellField.create(level, SpellField.Kind.WATER_GATHER, gather, 2.2f, 2.2f, SPRING_GATHER, p).data(level.random.nextInt()).fleeting().spawn();
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
			this.field = SpellField.create(level, SpellField.Kind.TIDAL_LANCE, HeavenSpells.fromHands(caster), 0.4f, 1, LANCE_TICKS, caster)
					.anchor(caster, SpellAnchor.HANDS, Vec3.ZERO).spawn();
			sound(caster.position(), SoundEvents.TRIDENT_RIPTIDE_3, 1.2f, 0.8f);
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
			if (age % 8 == 0) sound(to, SoundEvents.WATER_AMBIENT, 1.0f, 1.6f);
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
					.data(caster.getRandom().nextInt()).anchor(caster, SpellAnchor.POSITION, Vec3.ZERO).spawn();
			sound(caster.position(), SoundEvents.FIRE_EXTINGUISH, 1.5f, 0.5f);
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
			this.from = HeavenSpells.footing(caster); // along the land, however high its caster is
			this.way = Vec3.directionFromRotation(0, caster.getYRot());
			this.field = SpellField.create(level, SpellField.Kind.GREAT_TIDE, from, (float) TIDE_WIDTH, (float) TIDE_HEIGHT, (int) (end - start), caster)
					.facing(caster.getYRot(), 0).data(caster.getRandom().nextInt()).spawn();
			sound(from, SoundEvents.ELDER_GUARDIAN_CURSE, 1.0f, 0.5f);
			sound(from, SoundEvents.AMBIENT_UNDERWATER_ENTER, 2.0f, 0.6f);
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
			if (age % 6 == 0) sound(front, SoundEvents.GENERIC_SPLASH, 1.5f, 0.6f);
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
			this.from = HeavenSpells.footing(caster).add(Vec3.directionFromRotation(0, caster.getYRot()).scale(2));
			Vec3 end = HeavenSpells.lookPoint(caster, RIVER_LENGTH + 2);
			Vec3 ground = HeavenSpells.groundBelow(level, end.add(0, 1, 0), 2, 24);
			this.to = ground != null ? ground : end;
			Vec3 d = to.subtract(from);
			float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
			// Its pitch is the ground's slope along the line, so the client lands the falls on the ground at its far end too.
			float slope = (float) Math.toDegrees(Math.atan2(d.y, Math.max(1.0e-3, d.horizontalDistance())));
			this.field = SpellField.create(level, SpellField.Kind.RIVER_TORRENT, from, (float) RIVER_WIDTH, (float) d.horizontalDistance(), RIVER_TICKS, caster)
					.facing(yaw, slope).data(caster.getRandom().nextInt()).spawn();
			sound(from, SoundEvents.AMBIENT_UNDERWATER_ENTER, 3.0f, 0.4f);
			sound(to, SoundEvents.GENERIC_SPLASH, 3.0f, 0.4f);
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
				sound(from.add(dir.scale(length * level.random.nextDouble())), SoundEvents.GENERIC_SPLASH, 2.0f, 0.5f);
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
			sound(centre, SoundEvents.GLASS_BREAK, 3.0f, 0.4f);
			sound(centre, SoundEvents.POWDER_SNOW_STEP, 3.0f, 0.5f);
			sound(centre, SoundEvents.ELDER_GUARDIAN_CURSE, 1.5f, 1.4f);
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
				if (age % 10 == 0) sound(centre, SoundEvents.GLASS_PLACE, 2.0f, 0.5f);
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
	 * {@link #ZERO_TICKS}; when anyone strikes it, or the cold lets go, it shatters for {@link #ZERO_SHATTER} more. It stands frozen in a coffin
	 * of clear ice (SpellConstruct ICE_COFFIN) grown up round it, which bursts apart as it shatters.
	 */
	public static final class Zero extends HeavenSpells.Timed implements HeavenSpells.HarmListener {
		private final Spell spell;
		public final LivingEntity target;
		private final WoodSpells.Bound bound;
		private final SpellConstruct coffin;
		private boolean shattered;

		Zero(LivingEntity caster, Spell spell, LivingEntity target) {
			super(caster, ZERO_TICKS);
			this.spell = spell;
			this.target = target;
			SpellField.create(level, SpellField.Kind.FROST_BEAM, HeavenSpells.fromHands(caster), 0.3f, 1, 14, caster).target(target).fleeting().spawn();
			this.coffin = SpellConstruct.create(level, SpellConstruct.Kind.ICE_COFFIN, target.position(), caster.getRandom().nextFloat() * 360, 0, ZERO_TICKS + 12, caster)
					.size(Math.max(target.getBbHeight() / 2, target.getBbWidth() / 0.75f)).spawn();
			soak(caster, target, ZERO_DAMAGE, spell, null);
			// Its coffin is the look of its freezing (not the frozen's ward of ice, SpellAura FROZEN: drawn in it, it was a box in the crystals).
			target.setTicksFrozen(Math.max(target.getTicksFrozen(), 400));
			this.bound = new WoodSpells.Bound(caster, target, target.position(), ZERO_TICKS);
			HeavenSpells.linger(bound);
			sound(target.position(), SoundEvents.GLASS_BREAK, 2.0f, 0.4f);
			sound(caster.position(), SoundEvents.BEACON_DEACTIVATE, 1.5f, 1.6f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (shattered || !HeavenSpells.present(target, level)) return false;
			coffin.place(target.position(), coffin.getYRot(), 0);
			target.clearFire(); // nothing burns in its ice
			return true;
		}

		@Override
		public float onIncoming(LivingEntity hurt, DamageSource source, float amount) {
			// A blow on the frozen shatters it: someone's (not its own burning as it stood in the sun, which shattered it the moment it froze,
			// 2026-10-25), and not the spell's own strike as it froze.
			if (hurt == target && !shattered && level.getGameTime() > start + 2 && source.getEntity() != null
					&& !(source instanceof QiDamageSource qi && qi.element() == QiElement.WATER)) {
				shatter();
			}
			return amount;
		}

		private void shatter() {
			if (shattered) return;
			shattered = true;
			coffin.act("shatter");
			HeavenSpells.stop(bound);
			if (target.isAlive()) HeavenSpells.strikeAgain(caster, target, ZERO_SHATTER, spell);
			SpellFx.send(level, SpellFx.Kind.ICE_BURST, HeavenSpells.middle(target), Vec3.ZERO, 2.5f, 0);
			sound(target.position(), SoundEvents.GLASS_BREAK, 2.0f, 0.6f);
		}

		@Override
		public void end() {
			if (!shattered && target.isAlive()) shatter();
			coffin.dissolve();
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
			sound(at, SoundEvents.AMBIENT_UNDERWATER_ENTER, 4.0f, 0.4f);
			sound(at, SoundEvents.ELDER_GUARDIAN_CURSE, 2.0f, 0.4f);
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
			if (age % 30 == 0) sound(at, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 3.0f, 0.5f);
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
			this.tortoise = SpellConstruct.create(level, SpellConstruct.Kind.BLACK_TORTOISE, caster.position(), yaw, 0, TORTOISE_TICKS, caster).sizedFor(spell)
					.anchor(caster, SpellAnchor.POSITION, Vec3.ZERO).spawn();
			sound(caster.position(), SoundEvents.CONDUIT_ACTIVATE, 2.0f, 0.5f);
			sound(caster.position(), SoundEvents.TURTLE_AMBIENT_LAND, 2.0f, 0.4f);
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
					sound(caster.position(), SoundEvents.HOGLIN_ATTACK, 2.0f, 1.4f);
				}
			}
			if (striking != null && age >= struckAt + TORTOISE_LUNGE) {
				if (HeavenSpells.present(striking, level) && striking.distanceTo(caster) <= TORTOISE_REACH + 4) {
					HeavenSpells.strike(caster, striking, TORTOISE_DAMAGE, spell);
					HeavenSpells.push(caster.position(), striking, 1.6);
					SpellFx.send(level, SpellFx.Kind.SPLASH, HeavenSpells.middle(striking), Vec3.ZERO, 2.0f, 0);
					sound(striking.position(), SoundEvents.GENERIC_SPLASH, 2.0f, 0.6f);
				}
				striking = null;
			}
			// It keeps by its caster, turning to face the foe its serpent strikes, else the way its caster faces.
			float want = striking != null ? HeavenSpells.facing(striking.position().subtract(caster.position()))[0] : caster.getYRot();
			yaw = net.minecraft.util.Mth.approachDegrees(yaw, want, 14);
			tortoise.place(caster.position(), yaw, 0);
			if (age % 40 == 0) sound(caster.position(), SoundEvents.CONDUIT_AMBIENT, 1.5f, 0.6f);
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
			sound(ground, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 4.0f, 0.4f);
			sound(ground, SoundEvents.GENERIC_SPLASH, 4.0f, 0.5f);
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
					if (HeavenSpells.greatBeast(caster, e)) continue;
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
					sound(ground, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 4.0f, 0.5f);
				}
				return true;
			}
			if (age == RIVERS_TICKS) {
				// It bursts: those it held flung out; the flood it poured in stays, a lake in its basin.
				vortex.act("burst");
				for (LivingEntity e : held) {
					if (!HeavenSpells.present(e, level) || HeavenSpells.greatBeast(caster, e)) continue;
					Vec3 out = e.position().subtract(ground).multiply(1, 0, 1);
					out = out.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : out.normalize();
					HeavenSpells.move(e, out.scale(2.4).add(0, 1.2, 0));
					HeavenSpells.safeFall(e, 80);
				}
				SpellFx.send(level, SpellFx.Kind.SPLASH, ground, Vec3.ZERO, (float) RIVERS_RADIUS, 0, 192);
				SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) RIVERS_RADIUS * 1.4f, 0, 192);
				sound(ground, SoundEvents.GENERIC_EXPLODE, 3.0f, 0.6f);
				sound(ground, SoundEvents.GENERIC_SPLASH, 4.0f, 0.4f);
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
			sound(mouth, SoundEvents.END_PORTAL_SPAWN, 3.0f, 0.7f);
			sound(mouth, SoundEvents.CONDUIT_ACTIVATE, 3.0f, 0.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < WEAK_OPENS) return true;
			long pouring = age - WEAK_OPENS;
			if (pouring == 0) {
				fall = SpellConstruct.create(level, SpellConstruct.Kind.WEAK_WATER_FALL, ground, caster.getYRot(), 0, WEAK_TICKS + SpellConstruct.FADE, caster).sizedFor(spell).spawn();
				sound(ground, SoundEvents.GENERIC_SPLASH, 4.0f, 0.3f);
				sound(mouth, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 4.0f, 0.4f);
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
				if (!HeavenSpells.greatBeast(caster, e)) {
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
			if (pouring % 20 == 0) sound(ground, SoundEvents.WATER_AMBIENT, 4.0f, 0.4f);
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
			sound(ground, SoundEvents.END_PORTAL_SPAWN, 3.0f, 0.4f);
			sound(ground, SoundEvents.ELDER_GUARDIAN_AMBIENT, 3.0f, 0.5f);
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
				sound(ground, SoundEvents.ELDER_GUARDIAN_CURSE, 3.0f, 0.4f);
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
				if (HeavenSpells.greatBeast(caster, e)) continue;
				Vec3 out = e.position().subtract(ground).multiply(1, 0, 1);
				out = out.lengthSqr() < 1.0e-4 ? across : out.normalize();
				HeavenSpells.move(e, breach ? out.scale(0.7).add(0, 2.4, 0) : out.scale(1.8).add(0, 0.6, 0));
				HeavenSpells.safeFall(e, 100);
			}
			SpellFx.send(level, SpellFx.Kind.SPLASH, ground, Vec3.ZERO, (float) KUN_RADIUS, 0, 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) KUN_RADIUS * 1.6f, 0, 192);
			sound(ground, SoundEvents.GENERIC_SPLASH, 5.0f, 0.3f);
			sound(ground, breach ? SoundEvents.ELDER_GUARDIAN_CURSE : SoundEvents.GENERIC_EXPLODE, 4.0f, 0.4f);
		}

		@Override
		public void end() {
			abyss.dissolve();
			if (kun != null) kun.dissolve();
		}
	}

	// --- Sacred Vessel ---

	/**
	 * Twenty-Four Sea-Fixing Pearls: the twenty-four pearls that fix the sea (SpellConstruct SEA_PEARL each, its own GeckoLib model: a pearl
	 * with the sea turning in it) unwind round their caster ({@link #PEARLS_FORM}), circling {@link #PEARLS_CIRCLE} out, and fly off one after
	 * another, {@link #PEARL_EVERY} ticks apart, at the foes within {@link #PEARLS_REACH} of them, nearest first, round and round them (at where
	 * the caster looks, with none): each bursts with the weight of the sea, {@link #PEARL_DAMAGE} to everything within {@link #PEARL_BLAST} and
	 * a little crater.
	 */
	public static final class Pearls extends HeavenSpells.Timed {
		private final Spell spell;
		private final SpellConstruct[] pearls = new SpellConstruct[PEARLS];
		/** Each pearl's flight once loosed: where from, at whom (or where), when. */
		private final Vec3[] from = new Vec3[PEARLS], to = new Vec3[PEARLS];
		private final LivingEntity[] at = new LivingEntity[PEARLS];
		private final long[] loosed = new long[PEARLS];
		private int next;

		Pearls(LivingEntity caster, Spell spell) {
			super(caster, PEARLS_FORM + PEARLS * PEARL_EVERY + PEARL_FLIGHT + 6);
			this.spell = spell;
			java.util.Arrays.fill(loosed, -1);
			for (int i = 0; i < PEARLS; i++) {
				// Drawn by hand: lustrous, the sea turning in it, its water streaming after it (SpellConstruct PEARL: SpellDrawn; the user's ask,
				// 2026-10-25, of their model: "looks stupid").
				pearls[i] = SpellConstruct.create(level, SpellConstruct.Kind.PEARL, circle(i, 0), 0, 0, (int) (end - start), caster)
						.size((float) (HeavenSpells.greatness(Realm.SACRED_VESSEL) * PEARL_SIZE * 0.5)).spawn();
			}
			sound(caster.position(), SoundEvents.CONDUIT_ACTIVATE, 2.0f, 1.2f);
			sound(caster.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 2.0f, 0.8f);
		}

		/** Pearl {@code i}'s place in the circle round its caster, {@code age} ticks in (it unwinds outward as it forms, turning). */
		private Vec3 circle(int i, long age) {
			double unwound = Math.min(1, age / (double) PEARLS_FORM);
			double angle = Math.PI * 2 * i / PEARLS + age * 0.09;
			double r = PEARLS_CIRCLE * (0.3 + 0.7 * unwound);
			return caster.position().add(Math.cos(angle) * r, caster.getBbHeight() * 0.6 + 0.6 * Math.sin(angle * 3 + age * 0.2), Math.sin(angle) * r);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			if (age >= PEARLS_FORM && (age - PEARLS_FORM) % PEARL_EVERY == 0 && next < PEARLS) loose(next++, age);
			for (int i = 0; i < PEARLS; i++) {
				if (loosed[i] < 0) {
					pearls[i].place(circle(i, age), 0, 0);
					continue;
				}
				long flying = age - loosed[i];
				if (flying > PEARL_FLIGHT) continue;
				Vec3 target = at[i] != null && HeavenSpells.present(at[i], level) ? HeavenSpells.middle(at[i]) : to[i];
				double f = flying / (double) PEARL_FLIGHT;
				Vec3 here = from[i].lerp(target, f).add(0, Math.sin(Math.PI * f) * Math.min(8, from[i].distanceTo(target) * 0.2), 0);
				pearls[i].place(here, 0, 0);
				if (flying == PEARL_FLIGHT) burst(i, target);
			}
			return true;
		}

		/** Pearl {@code i} flies off: at the next foe in turn, nearest first (at where the caster looks, with none). */
		private void loose(int i, long age) {
			List<LivingEntity> foes = HeavenSpells.around(caster, PEARLS_REACH, e -> HeavenSpells.mark(caster, e) && !HeavenSpells.spared(caster, e));
			foes.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(caster)));
			from[i] = pearls[i].position();
			if (!foes.isEmpty()) {
				at[i] = foes.get(i % foes.size());
				to[i] = HeavenSpells.middle(at[i]);
			} else {
				to[i] = HeavenSpells.lookPoint(caster, PEARLS_REACH);
			}
			loosed[i] = age;
			sound(from[i], SoundEvents.TRIDENT_THROW, 1.5f, 1.4f);
		}

		/** Pearl {@code i} bursts at {@code where}: everything near it struck by the sea's weight, a little crater, and it is gone. */
		private void burst(int i, Vec3 where) {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, where, PEARL_BLAST + 2, e -> !HeavenSpells.spared(caster, e))) {
				if (HeavenSpells.middle(e).distanceTo(where) > PEARL_BLAST + e.getBbWidth()) continue;
				e.invulnerableTime = 0;
				soak(caster, e, PEARL_DAMAGE, spell, pearls[i]);
				HeavenSpells.push(where, e, 0.6);
			}
			Vec3 ground = HeavenSpells.groundBelow(level, where, 1, 3);
			if (ground != null) Devastation.crater(caster, level, ground, PEARL_CRATER, PEARL_CRATER_DEPTH, Devastation.Floor.NONE);
			// The weight of the sea come down on it.
			Vec3 foot = ground != null ? ground : where;
			SpellFx.send(level, SpellFx.Kind.WATER_COLUMN, foot, Vec3.ZERO, (float) PEARL_BLAST, 0, 160);
			SpellFx.send(level, SpellFx.Kind.FLASH, where, Vec3.ZERO, 2.5f, 0xDDEEFF, 160);
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH, foot.x, foot.y + 0.5, foot.z, 40, PEARL_BLAST * 0.5, 0.4, PEARL_BLAST * 0.5, 0.2);
			sound(where, SoundEvents.GENERIC_SPLASH, 2.0f, 0.8f);
			sound(where, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.6f, 0.6f + level.random.nextFloat() * 0.3f);
			sound(where, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.5f, 0.6f);
			pearls[i].dissolve();
		}

		@Override
		public void end() {
			for (SpellConstruct pearl : pearls) if (pearl != null) pearl.dissolve();
		}
	}

	/**
	 * Ocean-Sealing Vase: a vase of white jade painted with waves, that holds an ocean (SpellConstruct OCEAN_VASE, its own GeckoLib model), comes
	 * over where its caster looks, its mouth tipped down {@link #VASE_HIGH} over the land ({@link #VASE_COMES}), and drinks for
	 * {@link #VASE_TICKS}: every drop of water within {@link #VASE_RADIUS} is drawn up out of the land into it (lakes and rivers left dry:
	 * Devastation#drain), and every foe within it is dragged up into the streams; whatever comes within {@link #VASE_MOUTH} of its mouth is held
	 * there and crushed for {@link #VASE_CRUSH} a second. Then it seals itself ({@link #VASE_SEALS}) and comes down mouth first
	 * ({@link #VASE_FALL}): {@link #VASE_SLAM} to everything within {@link #VASE_SLAM_RADIUS}, thrown out, and the ocean it holds poured back
	 * into a crater with a lake in it.
	 */
	public static final class Vase extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground, mouth;
		private final SpellConstruct vase;
		private final java.util.Set<LivingEntity> held = new java.util.LinkedHashSet<>();
		/** The streams drawn up into it while it drinks, and the roar of its whirlpool (a voice of its spell's). */
		private SpellConstruct vortex;
		private int roar = -1;

		Vase(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, VASE_COMES + VASE_TICKS + VASE_SEALS + VASE_FALL + 16);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.mouth = ground.add(0, VASE_HIGH, 0);
			this.vase = SpellConstruct.create(level, SpellConstruct.Kind.OCEAN_VASE, mouth, caster.getYRot(), 0, (int) (end - start), caster).sizedFor(spell).spawn();
			sound(mouth, SoundEvents.CONDUIT_ACTIVATE, 3.0f, 0.5f);
			sound(mouth, SoundEvents.AMETHYST_BLOCK_CHIME, 3.0f, 0.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < VASE_COMES) return true;
			long drinking = age - VASE_COMES;
			if (drinking == 0) {
				Devastation.drain(caster, level, ground, VASE_RADIUS, 8, VASE_DRAIN_DOWN);
				sound(mouth, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 4.0f, 0.4f);
				// The water drawn up in twisting streams over a whirlpool into its mouth (SpellConstruct VASE_VORTEX: SpellDrawn; the user's ask,
				// 2026-10-25: "Ocean Vase needs polish").
				vortex = SpellConstruct.create(level, SpellConstruct.Kind.VASE_VORTEX, ground.add(0, 0.05, 0), 0, 0, VASE_TICKS + SpellConstruct.FADE, caster)
						.size((float) (VASE_RADIUS * 0.85)).value((float) (mouth.y - ground.y)).spawn();
				roar = loop(ground, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 4.0f, 0.5f);
			}
			if (drinking < VASE_TICKS) {
				drink(drinking);
				return true;
			}
			if (drinking == VASE_TICKS) {
				vase.act("seal");
				if (vortex != null) vortex.dissolve();
				if (roar >= 0) hush(roar, 10);
				sound(mouth, SoundEvents.BOTTLE_FILL, 4.0f, 0.4f);
				sound(mouth, SoundEvents.CONDUIT_DEACTIVATE, 3.0f, 0.5f);
			}
			long falling = drinking - VASE_TICKS - VASE_SEALS;
			if (falling >= 0 && falling <= VASE_FALL) {
				double f = falling / (double) VASE_FALL;
				vase.place(mouth.lerp(ground, f * f), vase.getYRot(), 0);
				hold(mouth.lerp(ground, f * f));
				if (falling == VASE_FALL) slam();
			} else if (falling < 0) {
				hold(mouth);
			}
			return true;
		}

		/** A tick of its drinking: foes dragged up into its streams, the held crushed, the water drawn up. */
		private void drink(long drinking) {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, VASE_RADIUS + VASE_HIGH, e -> !held.contains(e) && !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > VASE_RADIUS || e.getY() > mouth.y + 4 || e.getY() < ground.y - 16) continue;
				Vec3 to = mouth.subtract(HeavenSpells.middle(e));
				if (to.length() <= VASE_MOUTH && !HeavenSpells.greatBeast(caster, e)) {
					held.add(e);
					SpellFx.send(level, SpellFx.Kind.SPLASH, HeavenSpells.middle(e), Vec3.ZERO, 2, 0);
					continue;
				}
				// Drawn up in a swirl round the vase's streams: in, up and round.
				Vec3 flat = to.multiply(1, 0, 1);
				Vec3 round = flat.lengthSqr() < 1.0e-4 ? Vec3.ZERO : new Vec3(-flat.z, 0, flat.x).normalize().scale(0.35);
				double pull = HeavenSpells.greatBeast(caster, e) ? 0.1 : 0.3 + 0.7 * (1 - Math.min(1, to.length() / (VASE_RADIUS + VASE_HIGH)));
				HeavenSpells.move(e, to.normalize().scale(pull).add(round));
				HeavenSpells.safeFall(e, 120);
				if (HeavenSpells.greatBeast(caster, e) && drinking % 20 == 10) HeavenSpells.strikeAgain(caster, e, VASE_CRUSH, spell);
			}
			hold(mouth);
			if (drinking % 20 == 10) {
				for (LivingEntity e : held) {
					if (!HeavenSpells.present(e, level)) continue;
					HeavenSpells.strikeAgain(caster, e, VASE_CRUSH, spell);
				}
				sound(mouth, SoundEvents.GENERIC_SPLASH, 3.0f, 0.5f);
			}
			// Spray off the whirlpool's rim, water dripping from the vase, bubbles bursting at its mouth.
			if (drinking % 2 == 0) {
				for (int k = 0; k < 3; k++) {
					double angle = level.random.nextDouble() * Math.PI * 2, out = (0.6 + 0.3 * level.random.nextDouble()) * VASE_RADIUS * 0.85;
					Vec3 foot = ground.add(Math.cos(angle) * out, 0.4, Math.sin(angle) * out);
					level.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH, foot.x, foot.y, foot.z, 12, 1.2, 0.2, 1.2, 0.3);
				}
				level.sendParticles(net.minecraft.core.particles.ParticleTypes.FALLING_WATER, mouth.x, mouth.y - 1.5, mouth.z, 6, 2.0, 0.6, 2.0, 0);
				level.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE_POP, mouth.x, mouth.y, mouth.z, 8, 1.5, 0.8, 1.5, 0.05);
			}
			if (drinking % 25 == 12) sound(ground, SoundEvents.WATER_AMBIENT, 4.0f, 0.5f + level.random.nextFloat() * 0.2f);
		}

		/** The held kept in its mouth (just inside it, a little under), drowning. */
		private void hold(Vec3 at) {
			for (java.util.Iterator<LivingEntity> it = held.iterator(); it.hasNext(); ) {
				LivingEntity e = it.next();
				if (!HeavenSpells.present(e, level)) {
					it.remove();
					continue;
				}
				HeavenSpells.teleport(e, at.subtract(0, e.getBbHeight() * 0.5 + 0.5, 0));
				HeavenSpells.safeFall(e, 100);
				if (breathes(e)) e.setAirSupply(Math.max(-19, e.getAirSupply() - 8));
			}
		}

		/** It comes down sealed: everything near struck and thrown out, the ocean poured back into a crater. */
		private void slam() {
			vase.act("slam");
			held.clear();
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, VASE_SLAM_RADIUS + 10, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > VASE_SLAM_RADIUS || e.getY() > ground.y + 20 || e.getY() < ground.y - 10) continue;
				e.invulnerableTime = 0;
				soak(caster, e, VASE_SLAM, spell, null);
				HeavenSpells.move(e, e.position().subtract(ground).multiply(1, 0, 1).normalize().scale(1.8).add(0, 1.0, 0));
				HeavenSpells.safeFall(e, 120);
			}
			Devastation.crater(caster, level, ground, VASE_CRATER, VASE_CRATER_DEPTH, Devastation.Floor.LAKE);
			SpellFx.send(level, SpellFx.Kind.WATER_COLUMN, ground, Vec3.ZERO, (float) VASE_SLAM_RADIUS * 0.6f, 0, 192);
			SpellFx.send(level, SpellFx.Kind.SPLASH, ground, Vec3.ZERO, (float) VASE_SLAM_RADIUS, 0, 192);
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH, ground.x, ground.y + 0.5, ground.z, 200, VASE_SLAM_RADIUS * 0.5, 0.5, VASE_SLAM_RADIUS * 0.5, 0.4);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) VASE_SLAM_RADIUS * 1.6f, 0, 192);
			sound(ground, SoundEvents.GENERIC_SPLASH, 5.0f, 0.3f);
			sound(ground, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.5f);
		}

		@Override
		public void end() {
			held.clear();
			vase.dissolve();
			if (vortex != null) vortex.dissolve();
		}
	}

	// --- Entering Nirvana ---

	/**
	 * Frozen Moon in the Well: a crescent moon of ice rises over where its caster looks, its reflection shining up out of a well of still black
	 * water under it (SpellConstruct FROZEN_MOON, its own GeckoLib model, {@link #WELL_RISES}); the land round freezes ({@link #WELL_FROST}). For
	 * {@link #WELL_TICKS} every foe in the well ({@link #WELL_RADIUS}) is held frozen in it, as the moon's reflection is held in the water, and
	 * takes {@link #WELL_DAMAGE} a second. Then the moon shatters: {@link #WELL_SHATTER} to every foe in the well, and it lets them go.
	 */
	public static final class MoonWell extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final SpellConstruct moon;

		MoonWell(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, WELL_RISES + WELL_TICKS + WELL_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			// Of the world's own ice, over a well of black water (SpellConstruct CRESCENT_MASS: SpellMasses; the user's ask, 2026-10-25, "polish").
			this.moon = SpellConstruct.create(level, SpellConstruct.Kind.CRESCENT_MASS, ground.add(0, 0.1, 0), caster.getYRot() + 180, 0, (int) (end - start), caster)
					.size((float) (WELL_RADIUS * 0.55)).tint(level.random.nextInt()).spawn();
			sound(ground, SoundEvents.AMETHYST_BLOCK_CHIME, 4.0f, 0.5f);
			sound(ground, SoundEvents.GLASS_PLACE, 3.0f, 0.4f);
		}

		private boolean within(LivingEntity e) {
			return Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) <= WELL_RADIUS && e.getY() < ground.y + 12 && e.getY() > ground.y - 6;
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < WELL_RISES) return true;
			long holding = age - WELL_RISES;
			if (holding == 0) {
				Devastation.freeze(caster, level, ground, WELL_FROST);
				SpellFx.send(level, SpellFx.Kind.ICE_BURST, ground, Vec3.ZERO, (float) WELL_RADIUS, 0, 192);
			}
			if (holding < WELL_TICKS) {
				for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, WELL_RADIUS + 12, e -> !HeavenSpells.spared(caster, e))) {
					if (!within(e)) continue;
					HeavenSpells.still(e);
					e.setTicksFrozen(Math.max(e.getTicksFrozen(), e.getTicksRequiredToFreeze() + 40));
					e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 5));
					if (holding % 20 == 10) {
						HeavenSpells.strikeAgain(caster, e, WELL_DAMAGE, spell);
						SpellFx.send(level, SpellFx.Kind.ICE_BURST, HeavenSpells.middle(e), Vec3.ZERO, 1.5f, 0);
					}
				}
				if (holding % 20 == 10) sound(ground, SoundEvents.POWDER_SNOW_STEP, 3.0f, 0.4f);
				return true;
			}
			if (holding == WELL_TICKS) shatter();
			return true;
		}

		/** The moon shatters: every foe in the well struck, let go. */
		private void shatter() {
			moon.act("shatter");
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, WELL_RADIUS + 12, e -> !HeavenSpells.spared(caster, e))) {
				if (!within(e)) continue;
				e.invulnerableTime = 0;
				soak(caster, e, WELL_SHATTER, spell, moon);
			}
			SpellFx.send(level, SpellFx.Kind.ICE_BURST, ground.add(0, WELL_RADIUS, 0), Vec3.ZERO, (float) WELL_RADIUS, 0, 192);
			SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, WELL_RADIUS * 1.2, 0), Vec3.ZERO, 14, MOON_LIGHT, 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) WELL_RADIUS * 1.4f, 0, 192);
			sound(ground, SoundEvents.GLASS_BREAK, 5.0f, 0.4f);
			sound(ground, SoundEvents.AMETHYST_CLUSTER_BREAK, 5.0f, 0.5f);
		}

		@Override
		public void end() {
			moon.dissolve();
		}
	}

	/**
	 * The Frozen Moon Falls: a moon of ice, cratered and cracked with cold light, its frost streaming up behind it (SpellConstruct ICE_MOON, its
	 * own GeckoLib model), falls out of the sky onto where its caster looks ({@link #FALLS_FALL}): {@link #FALLS_DAMAGE} to everything within
	 * {@link #FALLS_RADIUS}, thrown out; the land smashed into a crater of ice ({@link #FALLS_CRATER}), frozen round it out to {@link #FALLS_FROST},
	 * and every foe within that chilled for {@link #FALLS_CHILLS}.
	 */
	public static final class MoonFalls extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground, sky, rest;
		private final SpellConstruct moon;

		MoonFalls(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, FALLS_FALL + FALLS_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.sky = ground.add(0, FALLS_HIGH, 0);
			// It comes to rest half sunk into what it smashed. Of the world's own ice (SpellConstruct MOON_MASS: SpellMasses; the user's ask, 2026-10-25,
			// "polish all the skills again"), its heart at its place.
			this.rest = ground.add(0, FALLS_MOON * 0.4, 0);
			this.moon = SpellConstruct.create(level, SpellConstruct.Kind.MOON_MASS, sky, caster.getYRot(), 0, (int) (end - start), caster).size((float) FALLS_MOON)
					.tint(level.random.nextInt()).spawn();
			sound(sky, SoundEvents.GLASS_PLACE, 4.0f, 0.3f);
			sound(sky, SoundEvents.ELYTRA_FLYING, 4.0f, 0.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age <= FALLS_FALL) {
				double f = age / (double) FALLS_FALL;
				moon.place(sky.lerp(rest, f * f), moon.getYRot(), 0);
				if (age % 3 == 0) SpellFx.send(level, SpellFx.Kind.ICE_BURST, sky.lerp(rest, f * f).add(0, 10, 0), Vec3.ZERO, 3, 0, 192);
				if (age == FALLS_FALL) impact();
			}
			return true;
		}

		/** It lands: everything near struck and thrown out, the land smashed into a crater of ice and frozen round it, all near it chilled. */
		private void impact() {
			moon.act("shatter");
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, FALLS_FROST + 12, e -> !HeavenSpells.spared(caster, e))) {
				double d = Math.hypot(e.getX() - ground.x, e.getZ() - ground.z);
				if (d > FALLS_FROST || e.getY() > ground.y + 30 || e.getY() < ground.y - 12) continue;
				if (d <= FALLS_RADIUS) {
					e.invulnerableTime = 0;
					soak(caster, e, FALLS_DAMAGE, spell, moon);
					HeavenSpells.move(e, e.position().subtract(ground).multiply(1, 0, 1).normalize().scale(1.8).add(0, 1.0, 0));
					HeavenSpells.safeFall(e, 120);
				}
				e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FALLS_CHILLS, 2));
				e.setTicksFrozen(Math.max(e.getTicksFrozen(), e.getTicksRequiredToFreeze() + FALLS_CHILLS / 2));
			}
			Devastation.crater(caster, level, ground, FALLS_CRATER, FALLS_CRATER_DEPTH, Devastation.Floor.ICE);
			Devastation.freeze(caster, level, ground, FALLS_FROST);
			SpellFx.send(level, SpellFx.Kind.ICE_BURST, rest, Vec3.ZERO, (float) FALLS_RADIUS, 0, 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) FALLS_FROST, 0, 192);
			SpellFx.send(level, SpellFx.Kind.FLASH, rest, Vec3.ZERO, 18, MOON_LIGHT, 192);
			sound(ground, SoundEvents.GENERIC_EXPLODE, 6.0f, 0.4f);
			sound(ground, SoundEvents.GLASS_BREAK, 6.0f, 0.3f);
		}

		@Override
		public void end() {
			moon.dissolve();
		}
	}

	// --- True Immortal ---

	/**
	 * Glacier of the First Age: a wall of the first age's ice {@link #GLACIER_HALF} either side of its middle, built of the world's own ice, block
	 * by block (the user's asks, 2026-10-25: it "tries to track over land and looks very dumb and should just carve through slowly, and there
	 * should be like misty ice vfx at the bottom of it"; and then, of the shape of ice drawn gliding over the land that came of that, "looks
	 * stupid, also make it so if used in air it goes to ground and doesn't glide through air"). It heaves up out of the land {@link #GLACIER_START}
	 * ahead of its caster, on the ground under them however high they are ({@link #GLACIER_RISES}), and creeps forward along their look,
	 * {@link #GLACIER_REACH} in {@link #GLACIER_TICKS}: each block it creeps a new slice of ice stands up at its face and its back slumps away,
	 * so the ice itself is seen to advance. Its top keeps one level, the land's where it rose: down into a hollow its ice reaches to the hollow's
	 * floor (no deeper than {@link #GLACIER_FILLS}); a rise in its way it carves through, a little ahead of its face (a player's, the spells'
	 * griefing on: Devastation#cut; else its ice only fills what is open). Its face is a cliff of blue and packed ice, jagged with seracs, its
	 * back higher, snow on its top; cold mist rolls off its foot and ice crumbles from its face as it grinds. Every foe before its face is shoved
	 * along with it (one it overruns thrown out ahead; a great beast only ground past), struck for {@link #GLACIER_GRIND} every
	 * {@link #GLACIER_EVERY} ticks and chilled; its wake is left a sheet of packed ice and snow that thaws a while after. At its end its face
	 * calves ({@link #GLACIER_CALVES}): its front slabs topple forward and shatter, {@link #GLACIER_CALVE} to everything within
	 * {@link #GLACIER_CALVE_REACH} before it, and what is left of it slumps away.
	 */
	public static final class Glacier extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 forward, side, rose;
		/** Its level: the land's top where it rose (its foot; its top is so far over it). */
		private final int level0;
		private final long seed;
		/** How far on from where it rose its face has come (blocks, a fraction), and the slices stood up and carved so far (whole blocks). */
		private double front;
		private int stood, carved, slumped;
		/** The ice it has set down, by the slice (blocks on from where it rose) each column of it is in. */
		private final java.util.Map<Integer, List<BlockPos>> slices = new java.util.HashMap<>();
		/** Where its face is (its middle, at its foot). */
		private Vec3 face;

		Glacier(LivingEntity caster, Spell spell) {
			super(caster, GLACIER_RISES + GLACIER_TICKS + GLACIER_CALVES + GLACIER_AFTER);
			this.spell = spell;
			float yaw = caster.getYRot();
			this.forward = Vec3.directionFromRotation(0, yaw);
			this.side = new Vec3(-forward.z, 0, forward.x);
			// On the ground ahead of its caster, however high in the air they are.
			Vec3 at = caster.position().add(forward.scale(GLACIER_START));
			Vec3 below = HeavenSpells.groundBelow(level, at.add(0, 2, 0), 2, GLACIER_FINDS_GROUND);
			if (below == null) below = HeavenSpells.groundBelow(level, at.add(0, 24, 0), 24, GLACIER_FINDS_GROUND);
			this.rose = below != null ? below : new Vec3(at.x, caster.getY(), at.z);
			this.level0 = net.minecraft.util.Mth.floor(rose.y + 1.0e-3);
			this.seed = level.random.nextLong();
			this.face = rose;
			sound(rose, SoundEvents.GLASS_BREAK, 5.0f, 0.3f);
			sound(rose, SoundEvents.WARDEN_EMERGE, 5.0f, 0.6f);
			sound(rose, SoundEvents.STONE_BREAK, 5.0f, 0.3f);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, rose, Vec3.ZERO, (float) GLACIER_HALF, 0, 256);
		}

		/**
		 * How high its top stands over its level in slice {@code d}, {@code across} from its middle (blocks), {@code rise} of the way up: seracs,
		 * a few blocks across and along each, standing taller and lower by turns, lower toward its ends.
		 */
		private int height(double across, int d, double rise) {
			double u = across / GLACIER_HALF;
			double serac = 0.62 + 0.38 * noise(Math.floor(across / 2.5) * 31 + Math.floor(d / 3.0), 3) + 0.18 * noise(Math.floor(across / 1.2) + 40 + d * 0.7, 7);
			double h = GLACIER_HIGH * serac * (1 - 0.4 * u * u * u * u);
			return Math.max(2, (int) Math.round(h * rise));
		}

		/** A steady noise, 0 to 1, of {@code x} (its spell's own). */
		private double noise(double x, int salt) {
			long h = seed ^ (long) (x * 1000) * 0x9E3779B97F4A7C15L ^ salt * 0xC2B2AE3D27D4EB4FL;
			h ^= h >>> 31;
			h *= 0xBF58476D1CE4E5B9L;
			h ^= h >>> 29;
			return (h >>> 11) / (double) (1L << 53);
		}

		/** The block columns in slice {@code d} (those whose middles lie between {@code d - 1} and {@code d} on from where it rose), and how far across each is. */
		private java.util.Map<Long, Double> columns(int d) {
			java.util.Map<Long, Double> found = new java.util.LinkedHashMap<>();
			double half = GLACIER_HALF;
			// Sampled over all that the slice's columns could reach (a column whose middle is in it reaches 0.71 past it), each kept by its middle.
			for (double a = d - 1.8; a <= d + 0.8; a += 0.34) {
				for (double w = -half - 1.5; w <= half + 1.5; w += 0.34) {
					Vec3 p = rose.add(forward.scale(a)).add(side.scale(w));
					int x = net.minecraft.util.Mth.floor(p.x), z = net.minecraft.util.Mth.floor(p.z);
					long key = BlockPos.asLong(x, 0, z);
					if (found.containsKey(key)) continue;
					Vec3 middle = new Vec3(x + 0.5 - rose.x, 0, z + 0.5 - rose.z);
					double on = middle.dot(forward), across = middle.dot(side);
					double edge = half * (0.94 + 0.06 * noise(Math.floor(on), 11));
					if (on <= d - 1 || on > d || Math.abs(across) > edge) continue;
					found.put(key, across);
				}
			}
			return found;
		}

		/** Where the land's top is in a column (the highest block one could stand on, leaves not counted). */
		private int groundIn(int x, int z) {
			return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
		}

		/** The land in slice {@code d} carved away from its level up past its top (a rise in its way), ahead of its face. */
		private void carve(int d) {
			List<BlockPos> cells = new ArrayList<>();
			for (java.util.Map.Entry<Long, Double> c : columns(d).entrySet()) {
				int x = BlockPos.getX(c.getKey()), z = BlockPos.getZ(c.getKey());
				int top = Math.min(groundIn(x, z), level0 + (int) GLACIER_HIGH * 2);
				for (int y = level0; y <= top; y++) cells.add(new BlockPos(x, y, z));
			}
			if (!cells.isEmpty()) Devastation.cut(caster, level, cells, face, 2);
		}

		/** Slice {@code d} stood up: its ice set down column by column, from the floor of a hollow (or its level) up to its height; {@code rise} of its height. */
		private void stand(int d, double rise) {
			List<BlockPos> set = new ArrayList<>();
			int lasts = (int) (end - level.getGameTime()) + GLACIER_THAWS;
			for (java.util.Map.Entry<Long, Double> c : columns(d).entrySet()) {
				int x = BlockPos.getX(c.getKey()), z = BlockPos.getZ(c.getKey());
				double across = c.getValue();
				int floor = Math.max(level0 - GLACIER_FILLS, Math.min(level0, groundIn(x, z) + 1));
				int top = level0 + height(across, d, rise);
				for (int y = floor; y < top; y++) {
					BlockPos pos = new BlockPos(x, y, z);
					if (HeavenSpells.placeTemporary(level, pos, ice(y, top, across, true), lasts, 2 | 16)) set.add(pos);
				}
			}
			slices.computeIfAbsent(d, k -> new ArrayList<>()).addAll(set);
		}

		/** Slice {@code d}, left behind by its back: taken away down to a sheet of packed ice and snow on the ground, that thaws a while after. */
		private void slump(int d) {
			List<BlockPos> set = slices.remove(d);
			if (set == null) return;
			java.util.Map<Long, Integer> lowest = new java.util.HashMap<>();
			for (BlockPos pos : set) lowest.merge(BlockPos.asLong(pos.getX(), 0, pos.getZ()), pos.getY(), Math::min);
			for (BlockPos pos : set) {
				int bottom = lowest.get(BlockPos.asLong(pos.getX(), 0, pos.getZ()));
				if (pos.getY() == bottom) {
					HeavenSpells.retime(level, pos, GLACIER_THAWS / 2 + level.random.nextInt(GLACIER_THAWS));
					continue;
				}
				HeavenSpells.removeTemporary(level, pos, 2 | 16);
			}
			// The ice of its back crumbling as it slumps.
			if (!set.isEmpty() && level.random.nextInt(2) == 0) {
				BlockPos pos = set.get(level.random.nextInt(set.size()));
				level.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(Blocks.PACKED_ICE.defaultBlockState()));
			}
		}

		/** The ice at {@code y} of a column whose top is {@code top}, {@code across} from its middle: its face's or (not {@code atFace}) its body's. */
		private net.minecraft.world.level.block.state.BlockState ice(int y, int top, double across, boolean atFace) {
			double roll = noise(across * 7 + y * 13.1 + stood * 3.7, 17);
			if (y == top - 1 && roll < 0.55) return Blocks.SNOW_BLOCK.defaultBlockState();
			if (atFace) {
				if (roll < 0.45) return Blocks.BLUE_ICE.defaultBlockState();
				if (roll < 0.9) return Blocks.PACKED_ICE.defaultBlockState();
				return Blocks.ICE.defaultBlockState();
			}
			return roll < 0.7 ? Blocks.PACKED_ICE.defaultBlockState() : Blocks.BLUE_ICE.defaultBlockState();
		}

		/** How far ahead of its face (back into its body less than nought), to the side of its middle, and over its level {@code e} is. */
		private double[] off(LivingEntity e) {
			Vec3 rel = e.position().subtract(face);
			return new double[] {rel.x * forward.x + rel.z * forward.z, rel.x * side.x + rel.z * side.z, e.getY() - level0};
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < GLACIER_RISES) {
				// It heaves up out of the land: its first slices rising, a layer more of their ice each tick.
				if (age == 0) for (int d = 2 - GLACIER_BODY; d <= GLACIER_CARVE_AHEAD + 1; d++) carve(d);
				for (int d = 2 - GLACIER_BODY; d <= 1; d++) stand(d, (age + 1) / (double) GLACIER_RISES);
				if (age % 3 == 0) SpellFx.send(level, SpellFx.Kind.ICE_BURST, rose.add(side.scale((level.random.nextDouble() * 2 - 1) * GLACIER_HALF)), Vec3.ZERO, 4, 0, 256);
				if (age % 6 == 0) sound(rose, SoundEvents.GLASS_BREAK, 4.0f, 0.35f + 0.1f * level.random.nextFloat());
				if (age % 7 == 2) sound(rose, SoundEvents.STONE_BREAK, 4.0f, 0.4f);
				mist((age + 1) / (double) GLACIER_RISES);
				return true;
			}
			if (age == GLACIER_RISES) {
				stood = 1;
				carved = GLACIER_CARVE_AHEAD + 1;
				slumped = 1 - GLACIER_BODY;
			}
			long grinding = age - GLACIER_RISES;
			if (grinding < GLACIER_TICKS) {
				front = GLACIER_REACH * (grinding + 1) / GLACIER_TICKS;
				face = rose.add(forward.scale(front));
				grind(grinding % GLACIER_EVERY == 0);
				// Each block it creeps on: the land a little ahead of it carved, a new slice of ice stood up at its face, the slice its back
				// leaves behind slumped to a sheet.
				while (stood < (int) Math.floor(front) + 1) {
					stood++;
					while (carved < stood + GLACIER_CARVE_AHEAD) carve(++carved);
					stand(stood, 1);
					while (slumped < stood - GLACIER_BODY) slump(++slumped);
				}
				mist(1);
				if (grinding % 2 == 0) crumble();
				// Its grinding: ice groaning and cracking, the land breaking before it.
				if (grinding % 16 == 0) sound(face, SoundEvents.GLASS_BREAK, 4.0f, 0.3f + 0.04f * level.random.nextFloat());
				if (grinding % 11 == 3) sound(face, SoundEvents.STONE_BREAK, 4.0f, 0.35f + 0.1f * level.random.nextFloat());
				if (grinding % 9 == 5) sound(face.add(side.scale((level.random.nextDouble() * 2 - 1) * GLACIER_HALF)), SoundEvents.POWDER_SNOW_BREAK, 3.0f, 0.5f);
				if (grinding % 23 == 7) sound(face, SoundEvents.AMETHYST_BLOCK_BREAK, 4.0f, 0.35f);
				if (grinding % 37 == 20) sound(face, SoundEvents.WARDEN_HEARTBEAT, 5.0f, 0.5f);
				return true;
			}
			if (grinding == GLACIER_TICKS) {
				sound(face, SoundEvents.GLASS_BREAK, 6.0f, 0.4f);
				sound(face, SoundEvents.WARDEN_SONIC_CHARGE, 4.0f, 0.4f);
			}
			if (grinding == GLACIER_TICKS + GLACIER_CALVES / 2) calve();
			long after = grinding - GLACIER_TICKS - GLACIER_CALVES;
			// What is left of it slumps away, back first.
			if (after >= 0 && after % 2 == 0 && slumped < stood) slump(++slumped);
			if (after >= 0 && after % 2 == 1 && slumped < stood) slump(++slumped);
			if (grinding > GLACIER_TICKS) mist(0.6);
			return true;
		}

		/** Cold mist rolling off its foot along its face, and frost and snow drifting down off it: more as it stands taller ({@code rise}). */
		private void mist(double rise) {
			for (int i = 0; i < 10; i++) {
				Vec3 at = face.add(side.scale((level.random.nextDouble() * 2 - 1) * GLACIER_HALF)).add(forward.scale(0.6 + level.random.nextDouble() * 1.2));
				double y = level0 + 0.3 + level.random.nextDouble() * 1.2;
				level.sendParticles(ParticleTypes.CLOUD, at.x, y, at.z, 0, forward.x * 0.08, 0.004, forward.z * 0.08, 1.0);
			}
			for (int i = 0; i < 4; i++) {
				Vec3 at = face.add(side.scale((level.random.nextDouble() * 2 - 1) * GLACIER_HALF)).add(forward.scale(0.4));
				double y = level0 + level.random.nextDouble() * GLACIER_HIGH * rise;
				level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, y, at.z, 2, 0.6, 0.6, 0.6, 0.02);
				level.sendParticles(ParticleTypes.WHITE_ASH, at.x, y, at.z, 3, 1.5, 1.0, 1.5, 0.0);
			}
		}

		/** Ice crumbling off its face as it grinds: chips of it falling, and now and then a block's worth breaking away. */
		private void crumble() {
			Vec3 at = face.add(side.scale((level.random.nextDouble() * 2 - 1) * GLACIER_HALF)).add(forward.scale(0.7));
			double y = level0 + 2 + level.random.nextDouble() * (GLACIER_HIGH * 0.8 - 2);
			level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), at.x, y, at.z, 14, 0.8, 1.2, 0.8, 0.1);
			if (level.random.nextInt(4) == 0) {
				SpellBlock.show(level, Blocks.PACKED_ICE.defaultBlockState(), new Vec3(at.x, y, at.z), forward.scale(1.5 + level.random.nextDouble() * 2).add(0, -y + level0 + 0.5, 0), 0,
						16 + level.random.nextInt(8), SpellBlock.Motion.FLY, 0).scale(0.6f + level.random.nextFloat() * 0.5f);
			}
		}

		/** A tick of its grinding on: every foe before its face, or overrun by it, shoved along ahead of it; struck if {@code strike}. */
		private void grind(boolean strike) {
			double speed = GLACIER_REACH / GLACIER_TICKS;
			for (LivingEntity e : HeavenSpells.aroundPoint(level, face, GLACIER_HALF + GLACIER_BODY + 6, e -> !HeavenSpells.spared(caster, e))) {
				double[] o = off(e);
				if (Math.abs(o[1]) > GLACIER_HALF + e.getBbWidth() / 2 || o[0] < -GLACIER_BODY - 1 || o[0] > 3 + e.getBbWidth() / 2 || o[2] > GLACIER_HIGH * 1.4 + 4
						|| o[2] < -GLACIER_FILLS - 2) continue;
				if (!HeavenSpells.greatBeast(caster, e)) {
					if (o[0] < 1) HeavenSpells.teleport(e, face.add(forward.scale(2.2 + e.getBbWidth() / 2)).add(side.scale(o[1])).add(0, Math.max(0, e.getY() - face.y), 0));
					HeavenSpells.move(e, forward.scale(speed + 0.3).add(0, 0.05, 0));
				}
				e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
				e.setTicksFrozen(Math.max(e.getTicksFrozen(), e.getTicksRequiredToFreeze() + 20));
				if (strike) {
					e.invulnerableTime = 0;
					soak(caster, e, GLACIER_GRIND, spell, null);
					SpellFx.send(level, SpellFx.Kind.ICE_BURST, HeavenSpells.middle(e), Vec3.ZERO, 1.5f, 0);
				}
			}
		}

		/** Its face calves: its front slabs topple forward and shatter, everything within their reach before it struck, the land frozen where they fall. */
		private void calve() {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, face, GLACIER_HALF + GLACIER_CALVE_REACH + 6, e -> !HeavenSpells.spared(caster, e))) {
				double[] o = off(e);
				if (Math.abs(o[1]) > GLACIER_HALF + e.getBbWidth() / 2 || o[0] < -4 || o[0] > GLACIER_CALVE_REACH || o[2] > GLACIER_HIGH * 1.4 + 4 || o[2] < -GLACIER_FILLS - 2) continue;
				e.invulnerableTime = 0;
				soak(caster, e, GLACIER_CALVE, spell, null);
				if (!HeavenSpells.greatBeast(caster, e)) HeavenSpells.move(e, forward.scale(1.4).add(0, -0.4, 0));
				e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
			}
			// The front slabs topple: its first slices taken away and seen falling forward in pieces.
			int shown = 0;
			for (int d = stood; d > stood - GLACIER_CALVE_SLICES && d > slumped; d--) {
				List<BlockPos> set = slices.remove(d);
				if (set == null) continue;
				for (BlockPos pos : set) {
					net.minecraft.world.level.block.state.BlockState was = level.getBlockState(pos);
					HeavenSpells.removeTemporary(level, pos, 2 | 16);
					if (shown < GLACIER_CALVE_PIECES && level.random.nextInt(3) == 0) {
						shown++;
						Vec3 from = Vec3.atBottomCenterOf(pos);
						double high = pos.getY() - level0;
						Vec3 toward = forward.scale(2 + high * 0.6 + level.random.nextDouble() * 3).add(0, -high + 0.5, 0);
						SpellBlock.show(level, was.isAir() ? Blocks.PACKED_ICE.defaultBlockState() : was, from, toward, 0, 18 + level.random.nextInt(10), SpellBlock.Motion.FLY, 0);
					}
				}
			}
			Vec3 fell = face.add(forward.scale(GLACIER_CALVE_REACH * 0.5));
			Devastation.freeze(caster, level, fell, GLACIER_HALF);
			for (int i = -2; i <= 2; i++) SpellFx.send(level, SpellFx.Kind.ICE_BURST, fell.add(side.scale(i * GLACIER_HALF / 2.5)), Vec3.ZERO, 7, 0, 256);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, fell, Vec3.ZERO, (float) GLACIER_HALF * 1.6f, 0, 256);
			SpellFx.send(level, SpellFx.Kind.DUST, fell, Vec3.ZERO, (float) GLACIER_HALF, 0, 256);
			for (int i = 0; i < 40; i++) {
				Vec3 at = fell.add(side.scale((level.random.nextDouble() * 2 - 1) * GLACIER_HALF)).add(forward.scale((level.random.nextDouble() - 0.5) * GLACIER_CALVE_REACH));
				level.sendParticles(ParticleTypes.CLOUD, at.x, level0 + 0.5, at.z, 0, forward.x * 0.2, 0.02, forward.z * 0.2, 1.0);
			}
			sound(fell, SoundEvents.GENERIC_EXPLODE, 6.0f, 0.5f);
			sound(fell, SoundEvents.GLASS_BREAK, 6.0f, 0.4f);
			sound(fell, SoundEvents.ANVIL_LAND, 5.0f, 0.4f);
		}

		@Override
		public void end() {
			// Whatever of it still stands is taken away (its sheet on the ground thaws in its own time).
			for (List<BlockPos> set : slices.values()) for (BlockPos pos : set) HeavenSpells.removeTemporary(level, pos, 2 | 16);
			slices.clear();
		}
	}

	/**
	 * The Primordial Sea: the waters before heaven and earth well up where its caster looks in a ring of towering waves, {@link #PRIMAL_SEA_RADIUS}
	 * round, facing in (SpellConstruct PRIMORDIAL_SEA, its own GeckoLib model; {@link #PRIMAL_SEA_RISES}), the sea within them wheeling in a
	 * whirlpool. For {@link #PRIMAL_SEA_TICKS} every foe within is dragged round in it (a great beast only slowed), held within its waves, and
	 * every {@link #PRIMAL_SEA_EVERY} ticks drowned for {@link #PRIMAL_SEA_DROWN}. Then its waves crash in over its heart
	 * ({@link #PRIMAL_SEA_CLOSES}): {@link #PRIMAL_SEA_CRASH} to every foe within, the land under it left a lake.
	 */
	public static final class PrimalSea extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final SpellConstruct sea;

		PrimalSea(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, PRIMAL_SEA_RISES + PRIMAL_SEA_TICKS + PRIMAL_SEA_CLOSES + PRIMAL_SEA_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.sea = SpellConstruct.create(level, SpellConstruct.Kind.PRIMORDIAL_SEA, ground.add(0, 0.1, 0), caster.getYRot(), 0, (int) (end - start), caster).sizedFor(spell).spawn();
			sound(ground, SoundEvents.AMBIENT_UNDERWATER_ENTER, 5.0f, 0.4f);
			sound(ground, SoundEvents.GENERIC_SPLASH, 5.0f, 0.3f);
		}

		private List<LivingEntity> within() {
			return WoodSpells.foesWithin(caster, level, ground, PRIMAL_SEA_RADIUS, 10, 36);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < PRIMAL_SEA_RISES) return true;
			long churning = age - PRIMAL_SEA_RISES;
			if (churning < PRIMAL_SEA_TICKS) {
				churn(churning % PRIMAL_SEA_EVERY == 0);
				if (churning % 4 == 0) {
					double a = level.random.nextDouble() * Math.PI * 2, r = PRIMAL_SEA_RADIUS * (0.2 + 0.8 * level.random.nextDouble());
					SpellFx.send(level, SpellFx.Kind.SPLASH, ground.add(Math.cos(a) * r, 1.5, Math.sin(a) * r), Vec3.ZERO, 3, 0, 192);
				}
				if (churning % 20 == 0) sound(ground, SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS, 5.0f, 0.5f);
				return true;
			}
			if (churning == PRIMAL_SEA_TICKS) {
				sea.act("close");
				sound(ground, SoundEvents.GENERIC_SPLASH, 6.0f, 0.4f);
			}
			if (churning == PRIMAL_SEA_TICKS + PRIMAL_SEA_CLOSES) crash();
			return true;
		}

		/** A tick of its churning: every foe within dragged round its whirlpool, turned back from its waves; drowned if {@code drown}. */
		private void churn(boolean drown) {
			for (LivingEntity e : within()) {
				double dx = e.getX() - ground.x, dz = e.getZ() - ground.z, d = Math.max(1.0e-3, Math.hypot(dx, dz));
				Vec3 in = new Vec3(-dx / d, 0, -dz / d), round = new Vec3(-in.z, 0, in.x), v = e.getDeltaMovement();
				if (HeavenSpells.greatBeast(caster, e)) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3));
				else {
					double back = d > PRIMAL_SEA_RADIUS - 3 ? 0.5 : 0.08;
					HeavenSpells.move(e, round.scale(0.45).add(in.scale(back)).add(0, Math.min(v.y, 0.1), 0));
				}
				if (e.isOnFire()) e.clearFire();
				if (drown) {
					e.invulnerableTime = 0;
					soak(caster, e, PRIMAL_SEA_DROWN, spell, null);
					SpellFx.send(level, SpellFx.Kind.SPLASH, HeavenSpells.middle(e), Vec3.ZERO, 1.5f, 0);
				}
			}
		}

		/** Its waves crash in over its heart: every foe within struck, the land under it left a lake. */
		private void crash() {
			for (LivingEntity e : within()) {
				e.invulnerableTime = 0;
				soak(caster, e, PRIMAL_SEA_CRASH, spell, null);
				if (!HeavenSpells.greatBeast(caster, e)) HeavenSpells.move(e, new Vec3(0, -1.2, 0));
				HeavenSpells.safeFall(e, 100);
			}
			Devastation.crater(caster, level, ground, PRIMAL_SEA_LAKE, PRIMAL_SEA_LAKE_DEPTH, Devastation.Floor.LAKE);
			SpellFx.send(level, SpellFx.Kind.SPLASH, ground.add(0, 4, 0), Vec3.ZERO, (float) PRIMAL_SEA_RADIUS, 0, 256);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) PRIMAL_SEA_RADIUS * 1.4f, 0, 256);
			SpellFx.send(level, SpellFx.Kind.RIPPLE, ground.add(0, 1, 0), Vec3.ZERO, (float) PRIMAL_SEA_RADIUS, 0, 256);
			sound(ground, SoundEvents.GENERIC_EXPLODE, 6.0f, 0.5f);
			sound(ground, SoundEvents.GENERIC_SPLASH, 6.0f, 0.3f);
		}

		@Override
		public void end() {
			sea.dissolve();
		}
	}

	static void tick(MinecraftServer server) {}

	static void forget(UUID id) {}

	static void clear() {}

	private WaterSpells() {}
}
