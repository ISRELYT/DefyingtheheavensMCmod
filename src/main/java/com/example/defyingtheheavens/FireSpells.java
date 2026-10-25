package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The fire element's spells (Spell, HeavenSpells): fire is weak to water and strong against metal (QiElement#overcomes, the wards in
 * SpellAuras). Every number is the design's; every look its own (the client's spell renderers).
 * <ul>
 *   <li>Qi Refining: Flame Bolt (a red bolt from the hand), Flame Fist (fists alight; blows set the struck burning), Crimson
 *       Tempering (fire and lava pass harmlessly; lava bears you).</li>
 *   <li>Foundation Building: Remote Yang Bomb (a ball of yang fire left on a surface, set off from afar), Flame Armour (armour of
 *       flames), Fire Serpent (a serpent of fire that bends toward its mark).</li>
 *   <li>Core Formation: Crimson Swirl (a flaming tornado creeping toward the aim, tearing up the ground and leaving fire), Solar
 *       Field (a small sun overhead burning everyone near).</li>
 *   <li>Nascent Soul: Karmic Fire (burns a being by its sins), Blazing Meteor.</li>
 *   <li>Heavenly Being: Phoenix Descent, Sea of Flames.</li>
 *   <li>Four Axis: Nine Suns Descend, Phoenix Nirvana (a death undone in flames).</li>
 *   <li>Dao Integration: Vermilion Bird's Descent (the Vermilion Bird sweeping the land), One Flame Burns the Heavens (a pillar of fire).</li>
 *   <li>Void Shattering: Golden Crow Descends (the three-legged crow of the sun dives out of a rift in the sky), Sun Pulled Through the Void
 *       (a sun dragged through a rift to hang over the land, burning it, until it collapses and bursts).</li>
 * </ul>
 */
public final class FireSpells {
	public static final double BOLT_SPEED = 2.2, BOLT_RANGE = 40;
	public static final float BOLT_DAMAGE = 5;
	public static final int BOLT_BURN_SECONDS = 3;
	public static final int FLAME_FIST_TICKS = 15 * 20, CRIMSON_TEMPERING_TICKS = 30 * 20;
	public static final double BOMB_REACH = 16, BOMB_DETONATE_RANGE = 48, BOMB_RADIUS = 4;
	public static final float BOMB_DAMAGE = 14;
	public static final double BOMB_UPKEEP = 1.0;
	public static final int BOMB_COOLDOWN = 15 * 20, BOMB_BURN_SECONDS = 4;
	public static final int FLAME_ARMOUR_TICKS = 30 * 20;
	public static final double SERPENT_SPEED = 1.2, SERPENT_RANGE = 48, SERPENT_TURN = 6;
	public static final float SERPENT_DAMAGE = 12;
	public static final double SWIRL_RANGE = 40, SWIRL_PACE = 0.15, SWIRL_RADIUS = 3.5, SWIRL_HEIGHT = 12;
	public static final int SWIRL_TICKS = 10 * 20;
	public static final float SWIRL_DAMAGE = 3;
	public static final double SOLAR_RADIUS = 40, SOLAR_HEIGHT = 8;
	public static final int SOLAR_TICKS = 10 * 20, SOLAR_BURN_SECONDS = 2;
	public static final double KARMIC_RANGE = 32;
	public static final int KARMIC_TICKS = 8 * 20, KARMIC_CLEANSE = 2;
	public static final double METEOR_RANGE = 48, METEOR_RADIUS = 6;
	public static final float METEOR_DAMAGE = 45;
	/** The craters, by the spell's realm (Devastation#sized): the meteor's 13, the phoenix's 7, each falling sun's 9. */
	public static final int METEOR_FALL = 30, METEOR_CRATER = (int) Math.round(Devastation.sized(Realm.NASCENT_SOUL, 23));
	public static final double PHOENIX_SPEED = 1.6, PHOENIX_RANGE = 64, PHOENIX_RADIUS = 5;
	public static final float PHOENIX_DAMAGE = 90;
	public static final int PHOENIX_CRATER = (int) Math.round(Devastation.sized(Realm.HEAVENLY_BEING, 9));
	/** Sea of Flames burns foes {@link #SEA_RADIUS} round; the land it scorches reaches {@link #SEA_SCORCH} (31, by its realm: Devastation#sized). */
	public static final double SEA_RANGE = 48, SEA_RADIUS = 20, SEA_FIRES = 0.12, SEA_SCORCH = Devastation.sized(Realm.HEAVENLY_BEING, 40);
	public static final int SEA_TICKS = 10 * 20;
	public static final float SEA_DAMAGE = 20;
	public static final double SUNS_RANGE = 64, SUNS_SPREAD = 10, SUNS_RADIUS = 7;
	public static final float SUNS_DAMAGE = 150;
	public static final int SUNS_CRATER = (int) Math.round(Devastation.sized(Realm.FOUR_AXIS, 9));
	public static final int NIRVANA_TICKS = 60 * 20;
	public static final double NIRVANA_RADIUS = 10;
	public static final float NIRVANA_DAMAGE = 200;
	/**
	 * Vermilion Bird's Descent: its sweep ({@link #BIRD_SWEEP} long, {@link #BIRD_PATH} each side of it, {@link #BIRD_HEIGHT} over the land);
	 * the trench it scorches (5 round and 3 deep, cut every {@link #BIRD_TRENCH_EVERY} blocks, the land burnt 8 round: by its realm,
	 * Devastation#sized); its stoop, sweep and climb away (ticks).
	 */
	public static final double BIRD_SWEEP = HeavenSpells.span(Realm.DAO_INTEGRATION, 120), BIRD_PATH = HeavenSpells.span(Realm.DAO_INTEGRATION, 8), BIRD_HEIGHT = HeavenSpells.span(Realm.DAO_INTEGRATION, 6),
			BIRD_BEHIND = HeavenSpells.span(Realm.DAO_INTEGRATION, 24), BIRD_ON_HIGH = HeavenSpells.span(Realm.DAO_INTEGRATION, 46), BIRD_TRENCH = Devastation.sized(Realm.DAO_INTEGRATION, 3.5),
			BIRD_TRENCH_DEPTH = Devastation.sized(Realm.DAO_INTEGRATION, 2.4), BIRD_SCORCH = Devastation.sized(Realm.DAO_INTEGRATION, 6), BIRD_TRENCH_EVERY = 4;
	public static final float BIRD_DAMAGE = 450;
	public static final int BIRD_STOOP = 20, BIRD_SWEEP_TICKS = 60, BIRD_CLIMB = 24, BIRD_BURN_SECONDS = 8;
	/**
	 * One Flame Burns the Heavens: where the seed flies (up to {@link #FLAME_RANGE}); the pillar ({@link #FLAME_RADIUS} round,
	 * {@link #FLAME_HEIGHT} high, {@link #FLAME_DAMAGE} a second); the glassy pit (20 round, 13 deep: by its realm; twice what it was before
	 * 2026-10-24, the user's ask for far more) and the land set ablaze round where it breaks out ({@link #FLAME_SCORCH}, 33); the seed's
	 * flight and bloom and the pillar's burning (ticks).
	 */
	public static final double FLAME_RANGE = HeavenSpells.span(Realm.DAO_INTEGRATION, 64), FLAME_RADIUS = HeavenSpells.span(Realm.DAO_INTEGRATION, 8), FLAME_HEIGHT = HeavenSpells.span(Realm.DAO_INTEGRATION, 92), FLAME_PIT = Devastation.sized(Realm.DAO_INTEGRATION, 16),
			FLAME_PIT_DEPTH = Devastation.sized(Realm.DAO_INTEGRATION, 10), FLAME_SCORCH = Devastation.sized(Realm.DAO_INTEGRATION, 26);
	/** Its shaft burnt up to the open sky over it (Devastation#shaft): as wide as its pillar, up to this far (through a mountain, out of a deep cave). */
	public static final int FLAME_SHAFT_MOST = 256;
	public static final float FLAME_DAMAGE = 120;
	public static final int FLAME_FLIGHT = 16, FLAME_BLOOM = 18, FLAME_TICKS = 6 * 20, FLAME_BURN_SECONDS = 4;
	/** The sun's gold, beyond the void: the rifts the Golden Crow and the sun come through glow so. */
	public static final int SUN_GOLD = 0xFFB238;
	/**
	 * Golden Crow Descends: where (up to {@link #CROW_RANGE}, 118 by its realm), how high its rift opens over the land (60) and how long it is;
	 * whom the crow strikes (within {@link #CROW_RADIUS}, 22) and how hard, how long they burn; its glassy crater (13 round, 7 deep) and the
	 * land burnt round it (26), by its realm; how long the rift takes to open, the crow's dive, and how long it flares after (ticks).
	 */
	public static final double CROW_RANGE = HeavenSpells.span(Realm.VOID_SHATTERING, 64), CROW_HIGH = HeavenSpells.span(Realm.VOID_SHATTERING, 33),
			CROW_RIFT = HeavenSpells.span(Realm.VOID_SHATTERING, 17), CROW_RADIUS = HeavenSpells.span(Realm.VOID_SHATTERING, 12),
			CROW_CRATER = Devastation.sized(Realm.VOID_SHATTERING, 7), CROW_CRATER_DEPTH = Devastation.sized(Realm.VOID_SHATTERING, 4),
			CROW_SCORCH = Devastation.sized(Realm.VOID_SHATTERING, 14);
	public static final float CROW_DAMAGE = 1600;
	public static final int CROW_OPENS = 14, CROW_DIVE = 18, CROW_AFTER = 26, CROW_BURN_SECONDS = 10;
	/**
	 * Sun Pulled Through the Void: where (up to {@link #SUN_RANGE}, 118), how high it hangs over the land (29), how far above that its rift
	 * opens ({@link #SUN_DROP}, 15: clear of the sun) and the rift's length; whom it burns as it hangs (within {@link #SUN_RADIUS}, 40) and
	 * how hard a second, whom its burst strikes (within {@link #SUN_BLAST}, 26) and how hard; its glassy crater (17 round, 9 deep) and the land
	 * it scorches, by its realm; how long the rift takes to open, how long the sun is dragged out of it, when it begins to burn, how long it
	 * hangs, how long it is dragged back into the rift before it bursts, and how long the burst's light lasts after (ticks).
	 */
	public static final double SUN_RANGE = HeavenSpells.span(Realm.VOID_SHATTERING, 64), SUN_HIGH = HeavenSpells.span(Realm.VOID_SHATTERING, 16),
			SUN_DROP = HeavenSpells.span(Realm.VOID_SHATTERING, 8), SUN_RIFT = HeavenSpells.span(Realm.VOID_SHATTERING, 20),
			SUN_RADIUS = HeavenSpells.span(Realm.VOID_SHATTERING, 22), SUN_BLAST = HeavenSpells.span(Realm.VOID_SHATTERING, 14),
			SUN_CRATER = Devastation.sized(Realm.VOID_SHATTERING, 9), SUN_CRATER_DEPTH = Devastation.sized(Realm.VOID_SHATTERING, 5),
			SUN_SCORCH = Devastation.sized(Realm.VOID_SHATTERING, 18);
	public static final float SUN_DAMAGE = 100, SUN_BURST = 1200;
	public static final int SUN_OPENS = 10, SUN_PULL = 20, SUN_COMES = SUN_OPENS + SUN_PULL, SUN_TICKS = 5 * 20, SUN_COLLAPSE = 14, SUN_AFTER = 16,
			SUN_BURN_SECONDS = 3, SUN_BURST_BURNS = 8;
	/**
	 * Wind-Fire Wheels: how far along the look they run (128 by its realm), how far to either side of it they weave (13), how near each wheel
	 * it strikes (6) and how hard, how long the struck burn; the trench each burns into the land (5 wide, 4 deep, by its realm); their burst
	 * where they meet at the end (within 17, and a scorched crater 11 round); how long they take to flare up, their run, and how long after.
	 */
	public static final double WHEELS_LENGTH = HeavenSpells.span(Realm.SACRED_VESSEL, 60), WHEELS_WEAVE = HeavenSpells.span(Realm.SACRED_VESSEL, 6),
			WHEELS_RADIUS = HeavenSpells.span(Realm.SACRED_VESSEL, 3), WHEELS_TRENCH = Devastation.sized(Realm.SACRED_VESSEL, 2.5),
			WHEELS_TRENCH_DEPTH = Devastation.sized(Realm.SACRED_VESSEL, 2), WHEELS_BLAST = HeavenSpells.span(Realm.SACRED_VESSEL, 8),
			WHEELS_CRATER = Devastation.sized(Realm.SACRED_VESSEL, 5), WHEELS_CRATER_DEPTH = Devastation.sized(Realm.SACRED_VESSEL, 3);
	public static final float WHEELS_DAMAGE = 1800, WHEELS_BURST = 1200;
	public static final int WHEELS_IGNITE = 12, WHEELS_RUN = 40, WHEELS_AFTER = 16, WHEELS_BURN_SECONDS = 10;
	/**
	 * Eight Trigrams Furnace: where (up to {@link #FURNACE_RANGE}, 136), how high it falls from (64), whom it draws in (within 25) and how far
	 * into its belly (within 9 of its heart); its harm as it lands, a second within it, of the jets out of its eight windows (out to 51), and its
	 * burst; the crater it lands in (11 round), the trenches its jets burn (3 wide, 2 deep) and the glassy pit its burst leaves (19 round, 11
	 * deep), by its realm; how long it falls, how long it burns, how often it jets, how long its burst takes, and how long after (ticks).
	 */
	public static final double FURNACE_RANGE = HeavenSpells.span(Realm.SACRED_VESSEL, 64), FURNACE_HIGH = HeavenSpells.span(Realm.SACRED_VESSEL, 30),
			FURNACE_RADIUS = HeavenSpells.span(Realm.SACRED_VESSEL, 12), FURNACE_INSIDE = HeavenSpells.span(Realm.SACRED_VESSEL, 4),
			FURNACE_JETS = HeavenSpells.span(Realm.SACRED_VESSEL, 24), FURNACE_CRATER = Devastation.sized(Realm.SACRED_VESSEL, 5),
			FURNACE_CRATER_DEPTH = Devastation.sized(Realm.SACRED_VESSEL, 3), FURNACE_JET_WIDTH = Devastation.sized(Realm.SACRED_VESSEL, 1.5),
			FURNACE_JET_DEPTH = Devastation.sized(Realm.SACRED_VESSEL, 1), FURNACE_PIT = Devastation.sized(Realm.SACRED_VESSEL, 9),
			FURNACE_PIT_DEPTH = Devastation.sized(Realm.SACRED_VESSEL, 5), FURNACE_SCORCH = Devastation.sized(Realm.SACRED_VESSEL, 16);
	public static final float FURNACE_SLAM = 1000, FURNACE_BURN = 300, FURNACE_JET = 500, FURNACE_BURST = 1500;
	public static final int FURNACE_FALL = 24, FURNACE_TICKS = 6 * 20, FURNACE_JET_EVERY = 30, FURNACE_BURSTS = 14, FURNACE_AFTER = 16;
	/** Samadhi fire's white gold: the furnace's windows burn so. */
	public static final int SAMADHI_GOLD = 0xFFE8A0;
	/**
	 * Undying Ember: where (154 by its realm), how high it hangs over the land (5); its three bursts, each greater than the one before: how far
	 * round (10, 13, 17), their harm, their craters (6, 8, 11 round and 4, 5, 6 deep, by its realm); how long it flies, how long apart its bursts
	 * are, how long after each it sinks low, how long each burns what it strikes (seconds), and how long it has after its last (ticks).
	 */
	public static final double EMBER_RANGE = HeavenSpells.span(Realm.ENTERING_NIRVANA, 64), EMBER_HOVER = HeavenSpells.span(Realm.ENTERING_NIRVANA, 2);
	public static final double[] EMBER_BLAST = {HeavenSpells.span(Realm.ENTERING_NIRVANA, 4), HeavenSpells.span(Realm.ENTERING_NIRVANA, 5.6), HeavenSpells.span(Realm.ENTERING_NIRVANA, 7.2)},
			EMBER_CRATER = {Devastation.sized(Realm.ENTERING_NIRVANA, 2.5), Devastation.sized(Realm.ENTERING_NIRVANA, 3.5), Devastation.sized(Realm.ENTERING_NIRVANA, 4.5)},
			EMBER_CRATER_DEPTH = {Devastation.sized(Realm.ENTERING_NIRVANA, 1.5), Devastation.sized(Realm.ENTERING_NIRVANA, 2), Devastation.sized(Realm.ENTERING_NIRVANA, 2.5)};
	public static final float[] EMBER_DAMAGE = {1100, 1500, 2100};
	public static final int EMBER_FLIGHT = 14, EMBER_EVERY = 25, EMBER_DIMS = 12, EMBER_BURN = 6, EMBER_AFTER = 20;
	/**
	 * Pyre of the Heavens: where (154 by its realm), how far round its ring of burning cribs stands (29); what it burns every foe within a
	 * second, first and then more each second after; its collapse's harm; its crater (29 round and 12 deep, by its realm) and how far round the
	 * land burns (43); how long it rises, how long it burns, how long it takes to fall, and how long it has after (ticks).
	 */
	public static final double PYRE_RANGE = HeavenSpells.span(Realm.ENTERING_NIRVANA, 64), PYRE_RADIUS = HeavenSpells.span(Realm.ENTERING_NIRVANA, 12),
			PYRE_CRATER = Devastation.sized(Realm.ENTERING_NIRVANA, 12), PYRE_CRATER_DEPTH = Devastation.sized(Realm.ENTERING_NIRVANA, 5),
			PYRE_SCORCH = Devastation.sized(Realm.ENTERING_NIRVANA, 18);
	public static final float PYRE_BURN = 150, PYRE_RAMP = 100, PYRE_COLLAPSE = 2500;
	public static final int PYRE_RISES = 20, PYRE_TICKS = 8 * 20, PYRE_FALLS = 20, PYRE_AFTER = 24;
	/**
	 * Samadhi True Fire: where (218 by its realm), how far round its three pillars burn (17) and how far round they draw foes in (34), how far
	 * round their flare reaches (27); the crater it leaves (17 round, 9 deep, by its realm); its harm each time it burns (to the fire-proof too),
	 * and its flare's; how long they take to erupt, how long they burn, how often, how long they take to meet, and how long it has after (ticks).
	 */
	public static final double TRUE_FIRE_RANGE = HeavenSpells.span(Realm.TRUE_IMMORTAL, 64), TRUE_FIRE_RADIUS = HeavenSpells.span(Realm.TRUE_IMMORTAL, 5),
			TRUE_FIRE_PULL = HeavenSpells.span(Realm.TRUE_IMMORTAL, 10), TRUE_FIRE_FLARE_RADIUS = HeavenSpells.span(Realm.TRUE_IMMORTAL, 8),
			TRUE_FIRE_CRATER = Devastation.sized(Realm.TRUE_IMMORTAL, 5), TRUE_FIRE_CRATER_DEPTH = Devastation.sized(Realm.TRUE_IMMORTAL, 2.5);
	public static final float TRUE_FIRE_BURN = 2500, TRUE_FIRE_FLARE = 20000;
	public static final int TRUE_FIRE_ERUPTS = 16, TRUE_FIRE_TICKS = 8 * 20, TRUE_FIRE_EVERY = 10, TRUE_FIRE_MEETS = 10, TRUE_FIRE_AFTER = 14;
	/**
	 * First Flame Before Heaven: where (218 by its realm), how far round its flower of chaos-fire burns (34: its petals'); the crater its heart
	 * leaves bursting (27 round, 12 deep, by its realm); its harm each time it burns, and its heart's as it bursts, to all within; how long it
	 * takes to open, how long it burns, how often, how long it takes to slam shut on its heart, and how long it has after (ticks).
	 */
	public static final double FIRST_FLAME_RANGE = HeavenSpells.span(Realm.TRUE_IMMORTAL, 64), FIRST_FLAME_RADIUS = HeavenSpells.span(Realm.TRUE_IMMORTAL, 10),
			FIRST_FLAME_CRATER = Devastation.sized(Realm.TRUE_IMMORTAL, 8), FIRST_FLAME_CRATER_DEPTH = Devastation.sized(Realm.TRUE_IMMORTAL, 3.5);
	public static final float FIRST_FLAME_BURN = 3000, FIRST_FLAME_BURST = 45000;
	public static final int FIRST_FLAME_OPENS = 28, FIRST_FLAME_TICKS = 5 * 20, FIRST_FLAME_EVERY = 10, FIRST_FLAME_IMPLODES = 6, FIRST_FLAME_AFTER = 14;
	/** Chaos-fire's two lights: the white, and the black (a deep violet, as black flame is drawn). */
	public static final int CHAOS_WHITE = 0xF2F0FF, CHAOS_BLACK = 0x3E1E5A;

	/** Each caster's yang bomb, while one is set. */
	private static final Map<UUID, Bomb> BOMBS = new HashMap<>();

	/** Whether {@code p} has a yang bomb set (its next cast sets it off, for nothing). */
	static boolean hasBomb(LivingEntity p) {
		return BOMBS.containsKey(p.getUUID());
	}

	static boolean canCast(LivingEntity p, Spell spell) {
		return switch (spell) {
			// With a bomb set, the cast sets it off (free): nothing is paid for a new one.
			case REMOTE_YANG_BOMB -> {
				Bomb bomb = BOMBS.get(p.getUUID());
				if (bomb != null) {
					bomb.detonate(true);
					yield false;
				}
				yield HeavenSpells.lookedBlock(p, BOMB_REACH) != null || HeavenSpells.refuse(p);
			}
			case KARMIC_FIRE -> HeavenSpells.aimed(p, KARMIC_RANGE) != null || HeavenSpells.refuse(p);
			default -> true;
		};
	}

	static void cast(LivingEntity p, Spell spell) {
		switch (spell) {
			case FLAME_BOLT -> bolt(p, spell);
			case FLAME_FIST -> {
				SpellAuras.put(p, SpellAura.FLAME_FIST, FLAME_FIST_TICKS);
				HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.FIRECHARGE_USE, 1.0f, 0.8f);
				HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.BLAZE_AMBIENT, 0.6f, 1.6f);
			}
			case CRIMSON_TEMPERING -> {
				SpellAuras.put(p, SpellAura.CRIMSON_TEMPERING, CRIMSON_TEMPERING_TICKS);
				HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.LAVA_AMBIENT, 1.2f, 0.7f);
				HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.FIRECHARGE_USE, 0.8f, 0.6f);
			}
			case REMOTE_YANG_BOMB -> setBomb(p, spell);
			case FLAME_ARMOUR -> {
				SpellAuras.put(p, SpellAura.FLAME_ARMOUR, FLAME_ARMOUR_TICKS);
				HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.BLAZE_SHOOT, 1.0f, 0.5f);
				HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.ARMOR_EQUIP_NETHERITE, 1.0f, 1.2f);
				SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.FLAME_BURST, HeavenSpells.middle(p), Vec3.ZERO, 1.2f, 0);
			}
			case FIRE_SERPENT -> serpent(p, spell);
			case CRIMSON_SWIRL -> swirl(p, spell);
			case SOLAR_FIELD -> HeavenSpells.linger(new Sun(p, spell));
			case KARMIC_FIRE -> karmic(p, spell);
			case BLAZING_METEOR -> meteor(p, spell);
			case PHOENIX_DESCENT -> phoenix(p, spell);
			case SEA_OF_FLAMES -> HeavenSpells.linger(new Sea(p, spell, HeavenSpells.lookPoint(p, SEA_RANGE)));
			case NINE_SUNS_DESCEND -> nineSuns(p, spell);
			case PHOENIX_NIRVANA -> {
				HeavenSpells.endLingering(p.getUUID(), Nirvana.class);
				HeavenSpells.linger(new Nirvana(p, spell));
				SpellAuras.put(p, SpellAura.PHOENIX_NIRVANA, NIRVANA_TICKS);
				HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.BEACON_POWER_SELECT, 1.2f, 0.6f);
				HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.BLAZE_SHOOT, 1.2f, 0.4f);
			}
			case VERMILION_BIRD_DESCENT -> HeavenSpells.linger(new Bird(p, spell));
			case ONE_FLAME_BURNS_THE_HEAVENS -> {
				LivingEntity aimed = HeavenSpells.aimed(p, FLAME_RANGE);
				HeavenSpells.linger(new OneFlame(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, FLAME_RANGE)));
			}
			case GOLDEN_CROW_DESCENDS -> {
				LivingEntity aimed = HeavenSpells.aimed(p, CROW_RANGE);
				HeavenSpells.linger(new Crow(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, CROW_RANGE)));
			}
			case SUN_THROUGH_THE_VOID -> {
				LivingEntity aimed = HeavenSpells.aimed(p, SUN_RANGE);
				HeavenSpells.linger(new VoidSun(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, SUN_RANGE)));
			}
			case WIND_FIRE_WHEELS -> HeavenSpells.linger(new Wheels(p, spell));
			case EIGHT_TRIGRAMS_FURNACE -> {
				LivingEntity aimed = HeavenSpells.aimed(p, FURNACE_RANGE);
				HeavenSpells.linger(new Furnace(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, FURNACE_RANGE)));
			}
			case UNDYING_EMBER -> {
				LivingEntity aimed = HeavenSpells.aimed(p, EMBER_RANGE);
				HeavenSpells.linger(new Ember(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, EMBER_RANGE)));
			}
			case PYRE_OF_THE_HEAVENS -> {
				HeavenSpells.endLingering(p.getUUID(), Pyre.class);
				LivingEntity aimed = HeavenSpells.aimed(p, PYRE_RANGE);
				HeavenSpells.linger(new Pyre(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, PYRE_RANGE)));
			}
			case SAMADHI_TRUE_FIRE -> {
				LivingEntity aimed = HeavenSpells.aimed(p, TRUE_FIRE_RANGE);
				HeavenSpells.linger(new TrueFire(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, TRUE_FIRE_RANGE)));
			}
			case FIRST_FLAME_BEFORE_HEAVEN -> {
				HeavenSpells.endLingering(p.getUUID(), FirstFlame.class);
				LivingEntity aimed = HeavenSpells.aimed(p, FIRST_FLAME_RANGE);
				HeavenSpells.linger(new FirstFlame(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, FIRST_FLAME_RANGE)));
			}
			default -> {}
		}
	}

	// --- Qi Refining ---

	private static void bolt(LivingEntity p, Spell spell) {
		SpellProjectile.cast(p, spell, SpellProjectile.Shape.FLAME_BOLT, 1.0f, 2, new Vec3(0.35, -0.35, 0.9), BOLT_SPEED, BOLT_RANGE,
				new SpellProjectile.Behaviour() {
					@Override
					public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
						burn(p, target, missile, BOLT_DAMAGE, BOLT_BURN_SECONDS, spell);
						return false;
					}

					@Override
					public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
						SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.FLAME_BURST, hit.getLocation(), Vec3.ZERO, 0.8f, 0);
						HeavenSpells.sound(HeavenSpells.level(p), hit.getLocation(), SoundEvents.FIRECHARGE_USE, 0.6f, 1.3f);
					}
				});
		HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.BLAZE_SHOOT, 0.8f, 1.3f);
	}

	/**
	 * A fiery thing struck {@code target}: {@code damage} of fire qi through {@code via}, and set burning {@code seconds}; one
	 * warded by water takes none of it and doesn't catch (the ward's work, and a hiss of steam).
	 */
	private static void burn(LivingEntity p, LivingEntity target, net.minecraft.world.entity.Entity via, float damage, int seconds, Spell spell) {
		ServerLevel level = HeavenSpells.level(p);
		boolean quenched = SpellAuras.wardedBy(target, QiElement.WATER);
		HeavenSpells.strike(p, target, damage, spell, via);
		if (quenched) {
			SpellFx.send(level, SpellFx.Kind.STEAM, HeavenSpells.middle(target), Vec3.ZERO, 1.0f, 0);
			HeavenSpells.sound(level, target.position(), SoundEvents.FIRE_EXTINGUISH, 1.0f, 1.0f);
			return;
		}
		if (seconds > 0) target.setSecondsOnFire(seconds);
		SpellFx.send(level, SpellFx.Kind.FLAME_BURST, HeavenSpells.middle(target), Vec3.ZERO, 1.0f, 0);
		HeavenSpells.sound(level, target.position(), SoundEvents.FIRECHARGE_USE, 0.8f, 1.1f);
	}

	// --- Foundation Building ---

	private static void setBomb(LivingEntity p, Spell spell) {
		BlockHitResult hit = HeavenSpells.lookedBlock(p, BOMB_REACH);
		if (hit == null) return;
		Vec3 at = hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.25));
		Bomb bomb = new Bomb(p, spell, at);
		BOMBS.put(p.getUUID(), bomb);
		HeavenSpells.linger(bomb);
		HeavenSpells.sound(HeavenSpells.level(p), at, SoundEvents.FIRECHARGE_USE, 0.6f, 0.6f);
		HeavenSpells.tell(p, Component.translatable(ModLang.MSG_YANG_BOMB_SET));
	}

	/**
	 * A ball of yang fire left on a surface: seen by its caster and by those with Qi Sense (SpellField YANG_BOMB), it draws
	 * {@link #BOMB_UPKEEP} qi a second while it waits, and the caster's next cast of the spell sets it off from as far as
	 * {@link #BOMB_DETONATE_RANGE}. It gutters out if their qi runs dry, or they go far off or die.
	 */
	public static final class Bomb extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 at;
		private final SpellField field;
		private boolean done;

		Bomb(LivingEntity caster, Spell spell, Vec3 at) {
			super(caster, Integer.MAX_VALUE / 4);
			this.spell = spell;
			this.at = at;
			this.field = SpellField.create(level, SpellField.Kind.YANG_BOMB, at, 0.35f, 0.7f, 20 * 60 * 60, caster).spawn();
		}

		public Vec3 at() { return at; }

		@Override
		protected boolean run(long age, long now) {
			if (done) return false;
			if (!casterHere() || caster.position().distanceToSqr(at) > (BOMB_DETONATE_RANGE * 2) * (BOMB_DETONATE_RANGE * 2)) return gutter();
			if (age % 20 == 19 && !HeavenSpells.creative(caster)) {
				PlayerCultivation c = HeavenSpells.cultivation(caster);
				if (c == null || !c.consumeQi(BOMB_UPKEEP)) return gutter();
				if (caster instanceof ServerPlayer player) CultivationManager.sync(player);
			}
			return true;
		}

		private boolean gutter() {
			done = true;
			if (caster.isAlive()) HeavenSpells.tell(caster, Component.translatable(ModLang.MSG_YANG_BOMB_GONE));
			return false;
		}

		/** Sets it off: if its caster is near enough ({@code asked}: they cast the spell again). */
		void detonate(boolean asked) {
			if (done) return;
			if (asked && caster.position().distanceToSqr(at) > BOMB_DETONATE_RANGE * BOMB_DETONATE_RANGE) {
				HeavenSpells.tell(caster, Component.translatable(ModLang.MSG_YANG_BOMB_FAR));
				return;
			}
			done = true;
			BOMBS.remove(caster.getUUID());
			explode(caster, spell, level, at, BOMB_RADIUS, BOMB_DAMAGE, BOMB_BURN_SECONDS, 1.4);
			Spells.setCooldown(caster, spell, BOMB_COOLDOWN);
			if (caster instanceof ServerPlayer player) Spells.sync(player);
		}

		@Override
		public void end() {
			done = true;
			BOMBS.remove(caster.getUUID(), this);
			field.discard();
		}
	}

	/**
	 * A blast of fire at {@code at}: everything not spared within {@code radius} takes {@code damage} of fire qi (falling off a
	 * little toward the edge), set burning {@code seconds}, thrown back by {@code push}. No block is broken.
	 */
	static void explode(LivingEntity p, Spell spell, ServerLevel level, Vec3 at, double radius, float damage, int seconds, double push) {
		SpellFx.send(level, SpellFx.Kind.FIRE_EXPLOSION, at, Vec3.ZERO, (float) radius, 0, 192);
		HeavenSpells.sound(level, at, SoundEvents.GENERIC_EXPLODE, (float) Math.min(4, 1 + radius / 4), 0.9f);
		HeavenSpells.sound(level, at, SoundEvents.BLAZE_SHOOT, 1.5f, 0.5f);
		for (LivingEntity target : HeavenSpells.aroundPoint(level, at, radius + 1, e -> !HeavenSpells.spared(p, e))) {
			double share = 1 - 0.4 * Math.min(1, target.position().distanceTo(at) / (radius + 1));
			burn(p, target, null, (float) (damage * share), seconds, spell);
			HeavenSpells.push(at, target, push);
		}
	}

	private static void serpent(LivingEntity p, Spell spell) {
		SpellProjectile.cast(p, spell, SpellProjectile.Shape.FIRE_SERPENT, 1.0f, 8, new Vec3(0.4, -0.3, 1.0), SERPENT_SPEED, SERPENT_RANGE,
				new SpellProjectile.Behaviour() {
					@Override
					public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
						burn(p, target, missile, SERPENT_DAMAGE, 4, spell);
						SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.FLAME_BURST, missile.position(), Vec3.ZERO, 1.6f, 0);
						return false;
					}

					@Override
					public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
						SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.FLAME_BURST, hit.getLocation(), Vec3.ZERO, 1.4f, 0);
						HeavenSpells.sound(HeavenSpells.level(p), hit.getLocation(), SoundEvents.FIRECHARGE_USE, 0.8f, 0.8f);
					}

					@Override
					public void launched(SpellProjectile missile) {
						HeavenSpells.sound(HeavenSpells.level(p), missile.position(), SoundEvents.BLAZE_SHOOT, 1.0f, 0.6f);
					}
				}).homing(null, SERPENT_TURN).data(p.getRandom().nextInt());
		HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.BLAZE_AMBIENT, 1.0f, 0.7f);
	}

	// --- Core Formation ---

	private static void swirl(LivingEntity p, Spell spell) {
		Vec3 ahead = Vec3.directionFromRotation(0, p.getYRot());
		Vec3 start = p.position().add(ahead.scale(3));
		Vec3 ground = HeavenSpells.groundBelow(HeavenSpells.level(p), start, 3, 8);
		if (ground != null) start = ground;
		HeavenSpells.linger(new Swirl(p, spell, start, HeavenSpells.lookPoint(p, SWIRL_RANGE)));
		HeavenSpells.sound(HeavenSpells.level(p), start, SoundEvents.BLAZE_SHOOT, 1.5f, 0.4f);
	}

	/**
	 * Crimson Swirl: a tornado of flame, {@link #SWIRL_RADIUS} round and {@link #SWIRL_HEIGHT} tall, creeping at
	 * {@link #SWIRL_PACE} toward the point aimed at. It drags in and lifts what comes near, burning it, tears up the soft ground
	 * under it (flung spinning up its funnel) and leaves fire behind it.
	 */
	public static final class Swirl extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 goal;
		private final SpellField field;
		private Vec3 at;

		Swirl(LivingEntity caster, Spell spell, Vec3 start, Vec3 goal) {
			super(caster, SWIRL_TICKS);
			this.spell = spell;
			this.goal = goal;
			this.at = start;
			this.field = SpellField.create(level, SpellField.Kind.CRIMSON_SWIRL, start, (float) SWIRL_RADIUS, (float) SWIRL_HEIGHT, SWIRL_TICKS, caster)
					.data(caster.getRandom().nextInt()).spawn();
		}

		public Vec3 at() { return at; }

		@Override
		protected boolean run(long age, long now) {
			Vec3 way = goal.subtract(at).multiply(1, 0, 1);
			if (way.lengthSqr() > 0.04) at = at.add(way.normalize().scale(SWIRL_PACE));
			Vec3 ground = HeavenSpells.groundBelow(level, at.add(0, 1, 0), 3, 6);
			if (ground != null) at = new Vec3(at.x, ground.y, at.z);
			field.setPos(at.x, at.y, at.z);
			if (age % 10 == 0) {
				for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(SWIRL_RADIUS + 2, SWIRL_HEIGHT, SWIRL_RADIUS + 2),
						e -> !HeavenSpells.spared(caster, e) && e.getY() < at.y + SWIRL_HEIGHT && e.getY() > at.y - 2)) {
					Vec3 in = at.subtract(e.position()).multiply(1, 0, 1);
					double d = in.length();
					if (d > SWIRL_RADIUS + 2) continue;
					Vec3 round = new Vec3(-in.z, 0, in.x).normalize().scale(0.35);
					HeavenSpells.move(e, e.getDeltaMovement().scale(0.4).add(d > 0.5 ? in.normalize().scale(0.25) : Vec3.ZERO).add(round).add(0, 0.28, 0));
					if (d <= SWIRL_RADIUS) burn(caster, e, null, SWIRL_DAMAGE, 4, spell);
				}
			}
			if (age % 5 == 0) tearGround();
			return true;
		}

		/** The ground under it, and anything built on it, torn up a little (flung up the funnel) and set burning here and there. */
		private void tearGround() {
			for (int i = 0; i < 3; i++) {
				double a = level.random.nextDouble() * Math.PI * 2, r = level.random.nextDouble() * (SWIRL_RADIUS - 1);
				BlockPos top = BlockPos.containing(at.x + Math.cos(a) * r, at.y - 0.5, at.z + Math.sin(a) * r);
				for (int dy = 1; dy >= -1; dy--) {
					BlockPos pos = top.above(dy);
					BlockState state = level.getBlockState(pos);
					if (state.isAir() || !HeavenSpells.carvable(level, pos, caster)) continue;
					level.removeBlock(pos, false);
					Vec3 from = Vec3.atBottomCenterOf(pos);
					Vec3 up = new Vec3((level.random.nextDouble() - 0.5) * 4, 6 + level.random.nextDouble() * 5, (level.random.nextDouble() - 0.5) * 4);
					SpellBlock.show(level, HeavenSpells.asEarth(state), from, up, 0, 18, SpellBlock.Motion.FLY, 0).scale(0.6f);
					break;
				}
				if (level.random.nextInt(3) == 0) {
					Devastation.ignite(caster, level, BlockPos.containing(at.x + Math.cos(a + 2) * r, at.y + 0.2, at.z + Math.sin(a + 2) * r));
				}
			}
		}

		@Override
		public void end() {
			field.discard();
		}
	}

	/** Solar Field: a small sun {@link #SOLAR_HEIGHT} over its caster; everyone else within {@link #SOLAR_RADIUS} burns. */
	public static final class Sun extends HeavenSpells.Timed {
		private final SpellField field;

		Sun(LivingEntity caster, Spell spell) {
			super(caster, SOLAR_TICKS);
			this.field = SpellField.create(level, SpellField.Kind.SOLAR_SUN, caster.position().add(0, SOLAR_HEIGHT, 0), 2.5f, (float) SOLAR_RADIUS,
					SOLAR_TICKS, caster).anchor(caster, SpellAnchor.POSITION, new Vec3(0, SOLAR_HEIGHT, 0)).spawn();
			sound(caster.position(), SoundEvents.BEACON_ACTIVATE, 2.0f, 0.5f);
			sound(caster.position(), SoundEvents.BLAZE_SHOOT, 2.0f, 0.3f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			Vec3 sun = caster.position().add(0, SOLAR_HEIGHT, 0);
			field.setPos(sun.x, sun.y, sun.z);
			if (age % 20 == 10) {
				for (LivingEntity e : HeavenSpells.around(caster, SOLAR_RADIUS, e -> !HeavenSpells.spared(caster, e))) {
					if (e.fireImmune() || SpellAuras.wardedBy(e, QiElement.WATER)) continue;
					e.setSecondsOnFire(SOLAR_BURN_SECONDS);
				}
				if (age % 60 == 10) sound(sun, SoundEvents.FIRE_AMBIENT, 2.0f, 0.5f);
			}
			return true;
		}

		@Override
		public void end() {
			field.discard();
		}
	}

	// --- Nascent Soul ---

	/** How sinful {@code e} is, 0 (none) to 1 (a devil): a cultivator by its alignment, a monster wholly, a beast or villager hardly. */
	public static double sin(LivingEntity e) {
		int alignment;
		if (e instanceof ServerPlayer player) alignment = CultivationManager.get(player).getAlignment();
		else if (e instanceof CultivatorNpc npc) alignment = npc.getAlignment();
		else return e instanceof Enemy ? 0.6 : 0.05;
		return Math.max(0, -alignment / (double) -Alignment.MIN);
	}

	/** Karmic Fire's harm a second on {@code e}: 1 on the guiltless, up to 11 on a devil, 6 on a monster, a little on a beast. */
	public static float karmicHarm(LivingEntity e) {
		if (e instanceof ServerPlayer || e instanceof CultivatorNpc) return (float) (1 + 10 * sin(e));
		return e instanceof Enemy ? 6 : 0.5f;
	}

	private static void karmic(LivingEntity p, Spell spell) {
		LivingEntity target = HeavenSpells.aimed(p, KARMIC_RANGE);
		if (target == null) return;
		HeavenSpells.endLingering(p.getUUID(), Karma.class);
		HeavenSpells.linger(new Karma(p, spell, target));
		SpellAuras.put(target, SpellAura.KARMIC_FIRE, KARMIC_TICKS);
		SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.STREAK, HeavenSpells.fromHands(p), HeavenSpells.middle(target), 1, 0xE0482A);
		HeavenSpells.sound(HeavenSpells.level(p), target.position(), SoundEvents.BELL_RESONATE, 1.0f, 0.5f);
		HeavenSpells.sound(HeavenSpells.level(p), target.position(), SoundEvents.BLAZE_SHOOT, 1.0f, 0.4f);
	}

	/** Karmic Fire on one being: its sins burn, a second at a time ({@link #karmicHarm}), and a sinner's alignment burns toward none. */
	public static final class Karma extends HeavenSpells.Timed {
		private final Spell spell;
		public final LivingEntity target;

		Karma(LivingEntity caster, Spell spell, LivingEntity target) {
			super(caster, KARMIC_TICKS);
			this.spell = spell;
			this.target = target;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(target, level)) return false;
			if (age % 20 == 19) {
				HeavenSpells.strikeAgain(caster, target, karmicHarm(target), spell);
				if (target instanceof ServerPlayer player) {
					PlayerCultivation c = CultivationManager.get(player);
					if (c.getAlignment() < 0) {
						c.setAlignment(Math.min(0, c.getAlignment() + KARMIC_CLEANSE));
						CultivationManager.sync(player);
					}
				} else if (target instanceof CultivatorNpc npc && npc.getAlignment() < 0) {
					npc.setAlignment(Math.min(0, npc.getAlignment() + KARMIC_CLEANSE));
				}
				sound(target.position(), SoundEvents.FIRE_AMBIENT, 1.0f, 0.6f);
			}
			return true;
		}

		@Override
		public void end() {
			SpellAuras.remove(target, SpellAura.KARMIC_FIRE);
		}
	}

	private static void meteor(LivingEntity p, Spell spell) {
		LivingEntity aimed = HeavenSpells.aimed(p, METEOR_RANGE);
		Vec3 at = aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, METEOR_RANGE);
		fallOn(p, spell, at, SpellProjectile.Shape.METEOR, SpellConstruct.Kind.METEOR_MASS, 1.0f, METEOR_FALL, METEOR_RADIUS, METEOR_DAMAGE, METEOR_CRATER, 0);
		HeavenSpells.sound(HeavenSpells.level(p), at, SoundEvents.FIRECHARGE_USE, 2.0f, 0.3f);
	}

	/**
	 * Something huge and burning falls from the sky onto {@code at} over {@code ticks} (after {@code delay}), its mark glowing on the
	 * ground below it: a blast of fire there ({@link #explode}), a scorched crater {@code crater} round. It flies as {@code body} (a great spell's
	 * shape, SpellProjectile#body: a sun), striking the land with its "impact", if it has one.
	 */
	static void fallOn(LivingEntity p, Spell spell, Vec3 at, SpellProjectile.Shape shape, SpellConstruct.Kind body, float scale, int ticks, double radius,
					   float damage, int crater, int delay) {
		ServerLevel level = HeavenSpells.level(p);
		Vec3 ground = HeavenSpells.groundBelow(level, at.add(0, 2, 0), 4, 32);
		Vec3 target = ground != null ? ground : at;
		SpellField mark = SpellField.create(level, SpellField.Kind.METEOR_MARK, target, (float) radius, 1, ticks + delay, p).spawn();
		Vec3 from = target.add(Vec3.directionFromRotation(0, p.getYRot()).scale(-18)).add(0, 40, 0);
		Vec3 velocity = target.subtract(from).scale(1.0 / ticks);
		Runnable drop = () -> {
			SpellProjectile falling = SpellProjectile.thrown(level, p, spell, shape, scale, from, velocity, from.distanceTo(target) + 4,
					new SpellProjectile.Behaviour() {
						private void land(SpellProjectile missile, Vec3 where) {
							SpellConstruct form = missile.body();
							if (form != null) {
								form.place(where.add(0, 1, 0), form.getYRot(), form.getXRot());
								form.act("impact");
							}
							mark.discard();
							explode(p, spell, level, where, radius, damage, 6, 2.0);
							SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, where, Vec3.ZERO, (float) radius * 1.5f, 0, 192);
							scorch(p, level, where, crater);
						}

						@Override
						public boolean hitEntity(SpellProjectile missile, LivingEntity hit) {
							land(missile, missile.position());
							return false;
						}

						@Override
						public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
							land(missile, hit.getLocation());
						}

						@Override
						public void expire(SpellProjectile missile) {
							land(missile, missile.position());
						}
					}).hitSize(scale * 1.5f).data(level.random.nextInt());
			if (body != null) {
				SpellConstruct form = SpellConstruct.create(level, body, from, -falling.getYRot(), -falling.getXRot(), ticks + 20, p);
				// Of the world's own blocks, or drawn by hand: sized by what it strikes (a meteor's rock, a sun), each its own.
				if (body.modelled()) form.sizedFor(spell);
				else form.size((float) (radius * 0.4)).tint(level.random.nextInt());
				falling.body(form.spawn());
			}
		};
		if (delay <= 0) drop.run();
		else HeavenSpells.linger(new Delayed(p, delay, drop));
	}

	/**
	 * The ground at a blast: a crater {@code crater} round and seven tenths as deep blown out of it, natural or built
	 * (Devastation#crater), its floor burnt to magma and blackstone; fires left on what stands round it.
	 */
	private static void scorch(LivingEntity p, ServerLevel level, Vec3 at, int crater) {
		if (crater <= 0) return;
		BlockPos centre = BlockPos.containing(at);
		Devastation.crater(p, level, at, crater, crater * 0.7, Devastation.Floor.SCORCHED);
		for (int i = 0; i < crater * 6; i++) {
			BlockPos pos = centre.offset(level.random.nextInt(crater * 4 + 1) - crater * 2, 0, level.random.nextInt(crater * 4 + 1) - crater * 2);
			Vec3 ground = HeavenSpells.groundBelow(level, Vec3.atBottomCenterOf(pos), 3, 6);
			if (ground == null) continue;
			Devastation.ignite(p, level, BlockPos.containing(ground.x, ground.y + 0.1, ground.z));
		}
	}

	/** Something a spell does after a while (a meteor let fall when its mark has glowed, one of nine suns after another). */
	static final class Delayed extends HeavenSpells.Timed {
		private final Runnable then;

		Delayed(LivingEntity caster, int ticks, Runnable then) {
			super(caster, ticks);
			this.then = then;
		}

		@Override
		protected boolean run(long age, long now) {
			if (age >= end - start) {
				then.run();
				return false;
			}
			return true;
		}
	}

	// --- Heavenly Being ---

	/**
	 * Phoenix Descent: a phoenix of vermilion fire (SpellConstruct DESCENT_PHOENIX, the missile's body) gathers beside its caster and stoops on
	 * their mark, bending toward it; it bursts where it strikes: a blast of fire, a scorched crater.
	 */
	private static void phoenix(LivingEntity p, Spell spell) {
		SpellProjectile bird = SpellProjectile.cast(p, spell, SpellProjectile.Shape.PHOENIX, 1.0f, 10, new Vec3(0, 0.6, 1.6), PHOENIX_SPEED, PHOENIX_RANGE,
				new SpellProjectile.Behaviour() {
					private void strike(SpellProjectile missile) {
						if (missile.body() != null) missile.body().act("strike");
					}

					@Override
					public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
						strike(missile);
						explode(p, spell, HeavenSpells.level(p), missile.position(), PHOENIX_RADIUS, PHOENIX_DAMAGE, 6, 1.8);
						scorch(p, HeavenSpells.level(p), missile.position(), PHOENIX_CRATER);
						return false;
					}

					@Override
					public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
						strike(missile);
						explode(p, spell, HeavenSpells.level(p), hit.getLocation(), PHOENIX_RADIUS, PHOENIX_DAMAGE, 6, 1.8);
						scorch(p, HeavenSpells.level(p), hit.getLocation(), PHOENIX_CRATER);
					}

					@Override
					public void launched(SpellProjectile missile) {
						HeavenSpells.sound(HeavenSpells.level(p), missile.position(), SoundEvents.PHANTOM_SWOOP, 2.0f, 0.5f);
						HeavenSpells.sound(HeavenSpells.level(p), missile.position(), SoundEvents.BLAZE_SHOOT, 2.0f, 0.5f);
					}
				}).homing(null, 3).hitSize(1.5f);
		// It lives while it forms and flies its reach, at most.
		int life = 10 + (int) Math.ceil(PHOENIX_RANGE / PHOENIX_SPEED) + 12;
		bird.body(SpellConstruct.create(HeavenSpells.level(p), SpellConstruct.Kind.DESCENT_PHOENIX, bird.position(), p.getYRot(), p.getXRot(), life, p).sizedFor(spell).spawn());
		HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.PARROT_IMITATE_BLAZE, 2.0f, 0.5f);
	}

	/** Sea of Flames: the ground {@link #SEA_RADIUS} round a point burns; foes on it take {@link #SEA_DAMAGE} a second. */
	public static final class Sea extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 at;
		private final SpellField field;

		Sea(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, SEA_TICKS);
			this.spell = spell;
			Vec3 ground = HeavenSpells.groundBelow(level, point.add(0, 1, 0), 4, 24);
			this.at = ground != null ? ground : point;
			this.field = SpellField.create(level, SpellField.Kind.SEA_OF_FLAMES, at, (float) SEA_RADIUS, 3, SEA_TICKS, caster)
					.data(caster.getRandom().nextInt()).spawn();
			// The land itself takes fire: scorched, patches of it molten, flames on what stands there (trees catch, and spread it).
			Devastation.burn(caster, level, at, SEA_SCORCH, SEA_FIRES);
			sound(at, SoundEvents.BLAZE_SHOOT, 3.0f, 0.3f);
			sound(at, SoundEvents.FIRECHARGE_USE, 3.0f, 0.4f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age % 20 == 10) {
				for (LivingEntity e : HeavenSpells.aroundPoint(level, at, SEA_RADIUS + 2, e -> HeavenSpells.mark(caster, e))) {
					if (e.getY() > at.y + 4 || e.position().multiply(1, 0, 1).distanceTo(at.multiply(1, 0, 1)) > SEA_RADIUS) continue;
					burn(caster, e, null, SEA_DAMAGE, 3, spell);
				}
				if (age % 40 == 10) sound(at, SoundEvents.FIRE_AMBIENT, 3.0f, 0.5f);
			}
			return true;
		}

		@Override
		public void end() {
			field.discard();
		}
	}

	// --- Four Axis ---

	private static void nineSuns(LivingEntity p, Spell spell) {
		LivingEntity aimed = HeavenSpells.aimed(p, SUNS_RANGE);
		Vec3 centre = aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, SUNS_RANGE);
		for (int i = 0; i < 9; i++) {
			Vec3 at = i == 0 ? centre : centre.add(Math.cos(Math.PI * 2 * i / 8) * SUNS_SPREAD, 0, Math.sin(Math.PI * 2 * i / 8) * SUNS_SPREAD);
			fallOn(p, spell, at, SpellProjectile.Shape.FALLING_SUN, SpellConstruct.Kind.SUN, 1.0f, 26, SUNS_RADIUS, SUNS_DAMAGE, SUNS_CRATER, i * 5);
		}
		HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.BEACON_ACTIVATE, 3.0f, 0.4f);
		HeavenSpells.sound(HeavenSpells.level(p), centre, SoundEvents.BLAZE_SHOOT, 3.0f, 0.3f);
	}

	/** Ticks into Phoenix Nirvana its phoenix enfolds its caster, and is gone into them (its "enfold" takes 1.1 s). */
	static final int PHOENIX_ENFOLD = 52, PHOENIX_GONE = PHOENIX_ENFOLD + 22;
	/** Ticks the phoenix that bursts out of a caster its nirvana saved lasts. */
	static final int PHOENIX_REBORN = 46;

	/**
	 * Phoenix Nirvana: for its time, a death of its caster's is undone: they rise again at full health in a burst of flame
	 * ({@link #NIRVANA_DAMAGE} to all but their allies within {@link #NIRVANA_RADIUS}), harmful effects burnt away. Once. As it is cast a phoenix
	 * of flame (SpellConstruct NIRVANA_PHOENIX) rises out of them before their eyes, spreads its wings and crying enfolds them
	 * ({@link #PHOENIX_ENFOLD}); as it saves them, another bursts up out of them (NIRVANA_REBIRTH).
	 */
	public static final class Nirvana extends HeavenSpells.Timed implements HeavenSpells.DeathListener {
		private final Spell spell;
		private boolean spent;
		/** The phoenix rising out of its caster as it is cast, until it has enfolded them. */
		private SpellConstruct phoenix;

		Nirvana(LivingEntity caster, Spell spell) {
			super(caster, NIRVANA_TICKS);
			this.spell = spell;
			this.phoenix = SpellConstruct.create(level, SpellConstruct.Kind.NIRVANA_PHOENIX, before(caster), caster.getYRot() + 180, 0, PHOENIX_GONE + 16, caster)
					.sizedFor(spell).spawn();
		}

		/** Where the phoenix rises: a little before its caster, facing them (they watch it come, and it enfolds them). */
		private static Vec3 before(LivingEntity caster) {
			return caster.position().add(Vec3.directionFromRotation(0, caster.getYRot()).scale(2.5));
		}

		@Override
		protected boolean run(long age, long now) {
			if (phoenix != null) {
				if (age >= PHOENIX_GONE || !casterHere()) {
					phoenix.dissolve();
					phoenix = null;
				} else {
					phoenix.place(before(caster), caster.getYRot() + 180, 0);
					if (age == 16) sound(caster.position(), SoundEvents.PARROT_IMITATE_BLAZE, 1.6f, 0.6f);
					if (age == PHOENIX_ENFOLD) {
						phoenix.act("enfold");
						sound(caster.position(), SoundEvents.FIRECHARGE_USE, 1.4f, 0.6f);
					}
				}
			}
			return !spent && casterHere();
		}

		@Override
		public boolean allowDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
			if (spent || entity != caster) return true;
			// Not a death that nothing holds off (one's own golden core burst, a soul severed or seized, the Judge's gaze, /kill, old age:
			// HeavenPassives#untouchedByWards), nor a projected soul's, which never mends out of its body (both held off before the sweep
			// of 2026-10-23).
			if (HeavenPassives.untouchedByWards(source) || caster instanceof net.minecraft.world.entity.player.Player player && SoulProjection.isSoul(player)) return true;
			spent = true;
			caster.setHealth(caster.getMaxHealth());
			caster.clearFire();
			for (MobEffectInstance effect : List.copyOf(caster.getActiveEffects())) {
				if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) caster.removeEffect(effect.getEffect());
			}
			SpellAuras.remove(caster, SpellAura.PHOENIX_NIRVANA);
			for (LivingEntity e : HeavenSpells.around(caster, NIRVANA_RADIUS, e -> !HeavenSpells.spared(caster, e) && !HeavenSpells.ally(caster, e))) {
				burn(caster, e, null, NIRVANA_DAMAGE, 6, spell);
				HeavenSpells.push(caster.position(), e, 2.5);
			}
			SpellFx.send(level, SpellFx.Kind.FIRE_EXPLOSION, HeavenSpells.middle(caster), Vec3.ZERO, (float) NIRVANA_RADIUS, 1, 192);
			// The phoenix bursts up out of them, crying.
			SpellConstruct reborn = SpellConstruct.create(level, SpellConstruct.Kind.NIRVANA_REBIRTH, caster.position(), caster.getYRot(), 0, PHOENIX_REBORN, caster)
					.sizedFor(spell).spawn();
			HeavenSpells.linger(new Delayed(caster, PHOENIX_REBORN - SpellConstruct.FADE, reborn::dissolve));
			sound(caster.position(), SoundEvents.TOTEM_USE, 1.5f, 0.6f);
			sound(caster.position(), SoundEvents.BLAZE_SHOOT, 2.0f, 0.4f);
			sound(caster.position(), SoundEvents.PARROT_IMITATE_BLAZE, 2.0f, 0.5f);
			HeavenSpells.tell(caster, Component.translatable(ModLang.MSG_PHOENIX_REBORN));
			return false;
		}

		@Override
		public void end() {
			SpellAuras.remove(caster, SpellAura.PHOENIX_NIRVANA);
			if (phoenix != null) phoenix.dissolve();
		}
	}

	// --- Dao Integration ---

	/**
	 * Vermilion Bird's Descent: the Vermilion Bird of the south (SpellConstruct VERMILION_BIRD, its own GeckoLib model) stoops out of the
	 * sky behind its caster ({@link #BIRD_STOOP} ticks) and sweeps low along their look, {@link #BIRD_HEIGHT} over the land and clearing its
	 * hills, {@link #BIRD_SWEEP} blocks in {@link #BIRD_SWEEP_TICKS}: everything within {@link #BIRD_PATH} of its path takes
	 * {@link #BIRD_DAMAGE} (once) and burns {@link #BIRD_BURN_SECONDS}; under it the land is scorched into a burning trench. Then it climbs
	 * away into the sky and is gone.
	 */
	public static final class Bird extends HeavenSpells.Timed {
		private final Spell spell;
		/** Its stoop's height behind its caster, and where its sweep begins; its way (level, a unit). */
		private final Vec3 high, low, way;
		/** The land along its sweep, and the height it keeps over it (the land's highest near each point). */
		private final double[] floor, flight;
		private final SpellConstruct bird;
		private final java.util.Set<LivingEntity> struck = new java.util.HashSet<>();
		/** How far along its sweep the trench is cut (it begins a step on, clear of its caster's feet). */
		private double cut = BIRD_TRENCH_EVERY;

		Bird(LivingEntity caster, Spell spell) {
			super(caster, BIRD_STOOP + BIRD_SWEEP_TICKS + BIRD_CLIMB);
			this.spell = spell;
			this.way = Vec3.directionFromRotation(0, caster.getYRot());
			Vec3 front = caster.position().add(way.scale(4));
			this.floor = HeavenSpells.floorAlong(level, front, way, (int) BIRD_SWEEP);
			this.flight = HeavenSpells.highestNear(floor, 8);
			this.low = new Vec3(front.x, flight[0] + BIRD_HEIGHT, front.z);
			this.high = caster.position().add(way.scale(-BIRD_BEHIND)).add(0, BIRD_ON_HIGH, 0);
			this.bird = SpellConstruct.create(level, SpellConstruct.Kind.VERMILION_BIRD, high, caster.getYRot(), 50, (int) (end - start), caster).sizedFor(spell).spawn();
			bird.act("dive");
			sound(high, SoundEvents.PARROT_IMITATE_BLAZE, 3.0f, 0.5f);
			sound(caster.position(), SoundEvents.PHANTOM_SWOOP, 3.0f, 0.4f);
		}

		/** Where it is {@code t} ticks in: stooping down from on high behind, then along its sweep over the land, then up and away. */
		private Vec3 at(double t) {
			if (t <= BIRD_STOOP) {
				double f = t / BIRD_STOOP, drop = 1 - (1 - f) * (1 - f);
				return new Vec3(high.x + (low.x - high.x) * f, high.y + (low.y - high.y) * drop, high.z + (low.z - high.z) * f);
			}
			double s = t - BIRD_STOOP;
			if (s <= BIRD_SWEEP_TICKS) {
				double d = s / BIRD_SWEEP_TICKS * BIRD_SWEEP;
				return new Vec3(low.x + way.x * d, HeavenSpells.along(flight, d) + BIRD_HEIGHT, low.z + way.z * d);
			}
			double f = (s - BIRD_SWEEP_TICKS) / BIRD_CLIMB;
			return at(BIRD_STOOP + BIRD_SWEEP_TICKS).add(way.scale(f * BIRD_BEHIND * 1.4)).add(0, f * f * BIRD_ON_HIGH, 0);
		}

		@Override
		protected boolean run(long age, long now) {
			Vec3 here = at(age);
			float[] face = HeavenSpells.facing(at(age + 1).subtract(here));
			bird.place(here, face[0], face[1]);
			if (age == BIRD_STOOP) {
				SpellFx.send(level, SpellFx.Kind.FIRE_EXPLOSION, here, Vec3.ZERO, 10, 0, 192);
				sound(here, SoundEvents.GENERIC_EXPLODE, 3.0f, 0.7f);
				sound(here, SoundEvents.PARROT_IMITATE_BLAZE, 4.0f, 0.4f);
			}
			if (age < BIRD_STOOP - 4 || age > BIRD_STOOP + BIRD_SWEEP_TICKS) return true;
			// Whatever is low under it, and to either side, burns.
			for (LivingEntity e : HeavenSpells.aroundPoint(level, here, BIRD_PATH + BIRD_HEIGHT + 4, e -> !struck.contains(e) && !HeavenSpells.spared(caster, e))) {
				Vec3 m = HeavenSpells.middle(e);
				if (Math.hypot(m.x - here.x, m.z - here.z) > BIRD_PATH || m.y > here.y + 6 || m.y < here.y - BIRD_HEIGHT - 6) continue;
				struck.add(e);
				burn(caster, e, null, BIRD_DAMAGE, BIRD_BURN_SECONDS, spell);
				HeavenSpells.push(here.subtract(way.scale(4)), e, 1.2);
			}
			// The trench burnt into the land behind it.
			double d = (age - BIRD_STOOP) / (double) BIRD_SWEEP_TICKS * BIRD_SWEEP;
			while (cut <= d && cut <= BIRD_SWEEP) {
				Vec3 ground = new Vec3(low.x + way.x * cut, HeavenSpells.along(floor, cut), low.z + way.z * cut);
				Devastation.crater(caster, level, ground, BIRD_TRENCH, BIRD_TRENCH_DEPTH, Devastation.Floor.SCORCHED);
				if ((int) cut % 12 == 0) Devastation.burn(caster, level, ground, BIRD_SCORCH, 0.16);
				cut += BIRD_TRENCH_EVERY;
			}
			if (age % 5 == 0) SpellFx.send(level, SpellFx.Kind.FLAME_BURST, here.add(0, -2, 0), Vec3.ZERO, 3, 0);
			if (age % 10 == 0) sound(here, SoundEvents.BLAZE_SHOOT, 3.0f, 0.5f);
			return true;
		}

		@Override
		public void end() {
			bird.dissolve();
		}
	}

	/**
	 * One Flame Burns the Heavens: a seed of white flame (SpellConstruct ONE_FLAME_SEED) flies from its caster's hands to where they look
	 * ({@link #FLAME_FLIGHT} ticks), blooms there ({@link #FLAME_BLOOM}) and erupts into a pillar of fire (ONE_FLAME_PILLAR)
	 * {@link #FLAME_RADIUS} round that rises to the clouds for {@link #FLAME_TICKS}: everything in it takes {@link #FLAME_DAMAGE} a second
	 * and burns; the ground under it melts into a glassy pit {@link #FLAME_PIT} round, it burns a shaft up through whatever is over it to the
	 * open sky, its walls magma, netherrack and blackstone and on fire (Devastation#shaft: cast underground, a tunnel of fire to the surface),
	 * and the land round where it breaks out is set ablaze ({@link #FLAME_SCORCH}).
	 */
	public static final class OneFlame extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 from, ground;
		private final SpellConstruct seed;
		private SpellConstruct pillar;

		OneFlame(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, FLAME_FLIGHT + FLAME_BLOOM + FLAME_TICKS);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 32);
			this.ground = below != null ? below : point;
			this.from = HeavenSpells.fromHands(caster);
			// Bright till it erupts, then fading as the pillar it becomes fades in.
			this.seed = SpellConstruct.create(level, SpellConstruct.Kind.ONE_FLAME_SEED, from, caster.getYRot(), 0, FLAME_FLIGHT + FLAME_BLOOM + SpellConstruct.FADE, caster).sizedFor(spell).spawn();
			sound(from, SoundEvents.BLAZE_SHOOT, 2.0f, 1.6f);
			sound(from, SoundEvents.BEACON_ACTIVATE, 1.5f, 1.8f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age <= FLAME_FLIGHT) {
				// It arcs to the spot, turning as it goes, and hangs over it.
				double f = age / (double) FLAME_FLIGHT;
				seed.place(from.lerp(ground.add(0, 2.5, 0), f).add(0, Math.sin(Math.PI * f) * 6, 0), seed.getYRot() + 18, 0);
				if (age == FLAME_FLIGHT) {
					seed.act("bloom");
					sound(ground, SoundEvents.BEACON_POWER_SELECT, 2.0f, 1.5f);
				}
				return true;
			}
			long burning = age - FLAME_FLIGHT - FLAME_BLOOM;
			if (burning == 0) {
				seed.dissolve();
				pillar = SpellConstruct.create(level, SpellConstruct.Kind.ONE_FLAME_PILLAR, ground, 0, 0, FLAME_TICKS, caster).sizedFor(spell).spawn();
				Devastation.crater(caster, level, ground, FLAME_PIT, FLAME_PIT_DEPTH, Devastation.Floor.GLASSY);
				// It burns its way up to the sky through whatever is over it (underground, a fiery shaft to the surface), and the land
				// round where it breaks out is set ablaze.
				Devastation.shaft(caster, level, ground, FLAME_RADIUS, FLAME_SHAFT_MOST);
				BlockPos column = BlockPos.containing(ground);
				double sky = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
				Devastation.burn(caster, level, sky > ground.y + 4 ? new Vec3(ground.x, sky, ground.z) : ground, FLAME_SCORCH, 0.22);
				SpellFx.send(level, SpellFx.Kind.FIRE_EXPLOSION, ground, Vec3.ZERO, (float) FLAME_RADIUS * 1.4f, 0, 192);
				SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) FLAME_RADIUS * 2.5f, 0, 192);
				sound(ground, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.5f);
				sound(ground, SoundEvents.BLAZE_SHOOT, 4.0f, 0.3f);
			}
			if (burning >= 0 && burning % 20 == 0) {
				Vec3 middle = ground.add(0, FLAME_HEIGHT / 2, 0);
				for (LivingEntity e : HeavenSpells.aroundPoint(level, middle, FLAME_HEIGHT / 2 + FLAME_RADIUS, e -> !HeavenSpells.spared(caster, e))) {
					Vec3 m = HeavenSpells.middle(e);
					if (Math.hypot(m.x - ground.x, m.z - ground.z) > FLAME_RADIUS || m.y < ground.y - FLAME_PIT_DEPTH - 2 || m.y > ground.y + FLAME_HEIGHT) continue;
					e.invulnerableTime = 0;
					burn(caster, e, null, FLAME_DAMAGE, FLAME_BURN_SECONDS, spell);
				}
				sound(ground, SoundEvents.FIRE_AMBIENT, 4.0f, 0.4f);
			}
			return true;
		}

		@Override
		public void end() {
			seed.dissolve();
			if (pillar != null) pillar.dissolve();
		}
	}

	// --- Void Shattering ---

	/**
	 * Golden Crow Descends: the sky tears open {@link #CROW_HIGH} over where its caster looks (SpellConstruct VOID_RIFT, glowing the sun's
	 * gold), a little toward them, and the Golden Crow, the three-legged crow that carries the sun, made of the sun's own fire (GOLDEN_CROW, its
	 * own GeckoLib model: a sun blazing at its back), bursts out of it ({@link #CROW_OPENS} ticks in) and dives ({@link #CROW_DIVE}):
	 * everything within {@link #CROW_RADIUS} of where it strikes takes {@link #CROW_DAMAGE} and burns {@link #CROW_BURN_SECONDS}; the land
	 * there melts into a glassy crater, and burns round it. It flares a moment where it struck, then is gone.
	 */
	public static final class Crow extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground, mouth;
		private final SpellConstruct rift;
		private SpellConstruct crow;

		Crow(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, CROW_OPENS + CROW_DIVE + CROW_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			// Over it, a little toward the caster: it stoops at a slant, seen whole, not straight down its own back.
			Vec3 toward = caster.position().subtract(ground).multiply(1, 0, 1);
			toward = toward.lengthSqr() < 1.0e-4 ? Vec3.ZERO : toward.normalize();
			this.mouth = ground.add(toward.scale(CROW_HIGH * 0.35)).add(0, CROW_HIGH, 0);
			this.rift = SpellConstruct.create(level, SpellConstruct.Kind.VOID_RIFT, mouth, 0, 0, CROW_OPENS + 10 + SpellConstruct.FADE, caster).size((float) CROW_RIFT)
					.tint(SUN_GOLD).spawn();
			sound(mouth, SoundEvents.END_PORTAL_SPAWN, 3.0f, 0.8f);
			sound(caster.position(), SoundEvents.BLAZE_SHOOT, 2.0f, 0.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < CROW_OPENS) return true;
			float[] face = HeavenSpells.facing(ground.subtract(mouth));
			if (age == CROW_OPENS) {
				crow = SpellConstruct.create(level, SpellConstruct.Kind.GOLDEN_CROW, mouth, face[0], face[1], CROW_DIVE + CROW_AFTER, caster).sizedFor(spell).spawn();
				SpellFx.send(level, SpellFx.Kind.FLASH, mouth, Vec3.ZERO, 8, SUN_GOLD, 192);
				sound(mouth, SoundEvents.PARROT_IMITATE_BLAZE, 4.0f, 0.4f);
				sound(mouth, SoundEvents.FIRECHARGE_USE, 3.0f, 0.5f);
			}
			if (age == CROW_OPENS + 10) rift.dissolve(); // the crow through, the sky closes behind it
			long diving = age - CROW_OPENS;
			if (diving <= CROW_DIVE) {
				double f = diving / (double) CROW_DIVE;
				Vec3 at = mouth.lerp(ground, f * f);
				crow.place(at, face[0], face[1]);
				if (diving % 3 == 0) SpellFx.send(level, SpellFx.Kind.FLAME_BURST, at, Vec3.ZERO, 3, 0);
			}
			if (diving == CROW_DIVE) strike();
			return true;
		}

		/** It strikes: everything near burnt, the land melted into a glassy crater and burning round it. */
		private void strike() {
			crow.act("strike");
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, CROW_RADIUS + 12, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > CROW_RADIUS || e.getY() > ground.y + 16 || e.getY() < ground.y - 8) continue;
				burn(caster, e, null, CROW_DAMAGE, CROW_BURN_SECONDS, spell);
				HeavenSpells.push(ground, e, 1.8);
			}
			Devastation.crater(caster, level, ground, CROW_CRATER, CROW_CRATER_DEPTH, Devastation.Floor.GLASSY);
			Devastation.burn(caster, level, ground, CROW_SCORCH, 0.12);
			SpellFx.send(level, SpellFx.Kind.FIRE_EXPLOSION, ground, Vec3.ZERO, (float) CROW_RADIUS, 0, 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) CROW_RADIUS * 1.8f, 0, 192);
			SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, 4, 0), Vec3.ZERO, 10, SUN_GOLD, 192);
			sound(ground, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.5f);
			sound(ground, SoundEvents.PARROT_IMITATE_BLAZE, 4.0f, 0.3f);
		}

		@Override
		public void end() {
			rift.dissolve();
			if (crow != null) crow.dissolve();
		}
	}

	/**
	 * Sun Pulled Through the Void: space tears open in a slash across the sky {@link #SUN_DROP} over where the sun will hang, {@link #SUN_HIGH}
	 * over where its caster looks (SpellConstruct VOID_RIFT lying across, the sun's gold); once it is open ({@link #SUN_OPENS}) a sun is dragged
	 * out of it (VOID_SUN, its own GeckoLib model), swelling as it comes, and down clear of it ({@link #SUN_PULL}), and the rift shrinks away
	 * behind it (the user's ask, 2026-10-24: the two no longer overlap). It hangs there {@link #SUN_TICKS}: every second everything within
	 * {@link #SUN_RADIUS} of the land beneath it takes {@link #SUN_DAMAGE} and burns, and the land scorches. Then it collapses
	 * ({@link #SUN_COLLAPSE}) and bursts: {@link #SUN_BURST} to everything within {@link #SUN_BLAST}, thrown out, and the land under it melted
	 * into a glassy crater.
	 */
	public static final class VoidSun extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground, at, mouth;
		private final float yaw;
		private final SpellConstruct rift;
		private SpellConstruct sun;

		VoidSun(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, SUN_COMES + SUN_TICKS + SUN_COLLAPSE + SUN_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.at = ground.add(0, SUN_HIGH, 0);
			this.mouth = at.add(0, SUN_DROP, 0);
			this.yaw = caster.getYRot();
			// A slash lying across the sky (RiftRenderer: a tear's yaw turns it on its watchers' screens), the sun to come out of its underside;
			// it shrinks away once the sun is out.
			this.rift = SpellConstruct.create(level, SpellConstruct.Kind.VOID_RIFT, mouth, 90, 0, SUN_COMES + SpellConstruct.FADE, caster).size((float) SUN_RIFT)
					.tint(SUN_GOLD).spawn();
			sound(mouth, SoundEvents.END_PORTAL_SPAWN, 3.0f, 0.6f);
			sound(mouth, SoundEvents.BEACON_ACTIVATE, 3.0f, 0.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < SUN_OPENS) return true;
			if (sun == null) {
				// Dragged out of it once it is open, small, swelling as it comes (its model's emerge).
				sun = SpellConstruct.create(level, SpellConstruct.Kind.VOID_SUN, mouth, yaw, 0, SUN_PULL + SUN_TICKS + SUN_COLLAPSE + SpellConstruct.FADE, caster)
						.sizedFor(spell).spawn();
				SpellFx.send(level, SpellFx.Kind.FLASH, mouth, Vec3.ZERO, 8, SUN_GOLD, 192);
				sound(mouth, SoundEvents.FIRECHARGE_USE, 4.0f, 0.4f);
			}
			long burning = age - SUN_COMES;
			if (burning < 0) {
				// Down out of the rift, swift at first and settling where it is to hang.
				double t = (age - SUN_OPENS) / (double) SUN_PULL, f = 1 - (1 - t) * (1 - t);
				Vec3 here = mouth.lerp(at, f);
				sun.place(here, yaw, 0);
				if (age % 4 == 0) SpellFx.send(level, SpellFx.Kind.FLAME_BURST, here.add(0, SUN_DROP * 0.25, 0), Vec3.ZERO, 4, 0);
				return true;
			}
			if (burning == 0) {
				sun.place(at, yaw, 0);
				rift.dissolve(); // the sun out, the sky closes behind it
				sound(mouth, SoundEvents.END_PORTAL_SPAWN, 2.0f, 1.4f);
			}
			if (burning < SUN_TICKS && burning % 20 == 0) scorch(burning);
			if (burning == SUN_TICKS) {
				sun.act("collapse");
				sound(at, SoundEvents.BEACON_DEACTIVATE, 4.0f, 0.4f);
				sound(at, SoundEvents.WARDEN_SONIC_CHARGE, 3.0f, 0.6f);
			}
			if (burning == SUN_TICKS + SUN_COLLAPSE) burst();
			return true;
		}

		/** A second of it hanging there: everything near the land beneath it burnt; the land itself scorched the first time. */
		private void scorch(long burning) {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, SUN_RADIUS + SUN_HIGH, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > SUN_RADIUS || e.getY() > at.y + 8 || e.getY() < ground.y - 12) continue;
				e.invulnerableTime = 0;
				burn(caster, e, null, SUN_DAMAGE, SUN_BURN_SECONDS, spell);
			}
			if (burning == 20) Devastation.burn(caster, level, ground, SUN_SCORCH, 0.06);
			sound(at, SoundEvents.FIRE_AMBIENT, 4.0f, 0.4f);
			sound(at, SoundEvents.BLAZE_BURN, 3.0f, 0.4f);
		}

		/** It bursts: everything under it struck and thrown out, the land melted into a glassy crater. */
		private void burst() {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, SUN_BLAST + SUN_HIGH, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > SUN_BLAST || e.getY() > at.y + 10 || e.getY() < ground.y - 12) continue;
				e.invulnerableTime = 0;
				burn(caster, e, null, SUN_BURST, SUN_BURST_BURNS, spell);
				HeavenSpells.push(ground, e, 2.2);
			}
			Devastation.crater(caster, level, ground, SUN_CRATER, SUN_CRATER_DEPTH, Devastation.Floor.GLASSY);
			SpellFx.send(level, SpellFx.Kind.FIRE_EXPLOSION, at, Vec3.ZERO, (float) SUN_BLAST, 0, 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) SUN_RADIUS, 0, 192);
			SpellFx.send(level, SpellFx.Kind.FLASH, at, Vec3.ZERO, 16, SUN_GOLD, 192);
			sound(at, SoundEvents.GENERIC_EXPLODE, 5.0f, 0.4f);
			sound(at, SoundEvents.DRAGON_FIREBALL_EXPLODE, 4.0f, 0.5f);
			sun.dissolve();
		}

		@Override
		public void end() {
			rift.dissolve();
			if (sun != null) sun.dissolve();
		}
	}

	// --- Sacred Vessel ---

	/**
	 * Wind-Fire Wheels: two great wheels of fire and whirling wind (SpellConstruct WIND_FIRE_WHEEL, its own GeckoLib model) flare up before
	 * their caster ({@link #WHEELS_IGNITE}) and roll out along the look for {@link #WHEELS_LENGTH} over the land ({@link #WHEELS_RUN}), weaving
	 * {@link #WHEELS_WEAVE} out to either side and crossing midway: everything a wheel runs over (within {@link #WHEELS_RADIUS}) takes
	 * {@link #WHEELS_DAMAGE} and burns, and each burns a trench into the land as it rolls. Where they meet again at the end they burst:
	 * {@link #WHEELS_BURST} to everything within {@link #WHEELS_BLAST}, thrown out, and a scorched crater.
	 */
	public static final class Wheels extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 origin, along, across;
		private final SpellConstruct[] wheels = new SpellConstruct[2];
		private final Vec3[] last = new Vec3[2];
		@SuppressWarnings("unchecked")
		private final java.util.Set<LivingEntity>[] struck = new java.util.Set[] {new java.util.HashSet<>(), new java.util.HashSet<>()};
		private final float wheelHigh;

		Wheels(LivingEntity caster, Spell spell) {
			super(caster, WHEELS_IGNITE + WHEELS_RUN + WHEELS_AFTER);
			this.spell = spell;
			this.along = Vec3.directionFromRotation(0, caster.getYRot());
			this.across = new Vec3(-along.z, 0, along.x);
			this.origin = caster.position().add(along.scale(4));
			// Its middle a wheel's half-height off the land (its model's 2 blocks round, as great as its realm makes it: SpellConstruct#sizedFor).
			this.wheelHigh = (float) (HeavenSpells.greatness(Realm.SACRED_VESSEL) * 4.0);
			for (int i = 0; i < 2; i++) {
				Vec3 at = place(i, 0);
				wheels[i] = SpellConstruct.create(level, SpellConstruct.Kind.WIND_FIRE_WHEEL, at, caster.getYRot(), 0, (int) (end - start), caster).sizedFor(spell).spawn();
				last[i] = ground(at);
			}
			sound(origin, SoundEvents.BLAZE_SHOOT, 3.0f, 0.4f);
			sound(origin, SoundEvents.ELYTRA_FLYING, 2.0f, 1.4f);
		}

		/** Wheel {@code i}'s place {@code t} of the way along its run: out to its side and back, crossing the other midway, meeting it at the end. */
		private Vec3 place(int i, double t) {
			double side = (i == 0 ? 1 : -1) * (2.5 * (1 - t) + WHEELS_WEAVE * Math.sin(2 * Math.PI * t));
			Vec3 flat = origin.add(along.scale(WHEELS_LENGTH * t)).add(across.scale(side));
			Vec3 ground = ground(flat);
			return ground.add(0, wheelHigh, 0);
		}

		/** The land under {@code at} (or {@code at} where there is none near). */
		private Vec3 ground(Vec3 at) {
			Vec3 below = HeavenSpells.groundBelow(level, new Vec3(at.x, origin.y + 24, at.z), 0, 72);
			return below != null ? below : new Vec3(at.x, origin.y, at.z);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < WHEELS_IGNITE) return true;
			long rolling = age - WHEELS_IGNITE;
			if (rolling > WHEELS_RUN) return true;
			double t = rolling / (double) WHEELS_RUN;
			for (int i = 0; i < 2; i++) {
				Vec3 at = place(i, t), next = place(i, Math.min(1, t + 1.0 / WHEELS_RUN));
				float[] face = HeavenSpells.facing(next.subtract(at).multiply(1, 0, 1));
				wheels[i].place(at, rolling >= WHEELS_RUN ? wheels[i].getYRot() : face[0], 0);
				Vec3 on = at.subtract(0, wheelHigh, 0);
				Devastation.trench(caster, level, last[i], on, WHEELS_TRENCH, WHEELS_TRENCH_DEPTH);
				if (rolling % 4 == 0) Devastation.burn(caster, level, on, WHEELS_TRENCH, 0.35);
				last[i] = on;
				for (LivingEntity e : HeavenSpells.aroundPoint(level, at, WHEELS_RADIUS + wheelHigh, e -> !struck[0].contains(e) || !struck[1].contains(e))) {
					if (struck[i].contains(e) || HeavenSpells.spared(caster, e) || e.position().distanceTo(on) > WHEELS_RADIUS + 2 && HeavenSpells.middle(e).distanceTo(at) > WHEELS_RADIUS + 2) continue;
					struck[i].add(e);
					e.invulnerableTime = 0;
					burn(caster, e, null, WHEELS_DAMAGE, WHEELS_BURN_SECONDS, spell);
					HeavenSpells.push(at, e, 1.6);
				}
				if (rolling % 3 == 0) SpellFx.send(level, SpellFx.Kind.FLAME_BURST, on.add(0, 1, 0), Vec3.ZERO, 3, 0);
			}
			if (rolling % 10 == 0) sound(place(0, t), SoundEvents.FIRECHARGE_USE, 3.0f, 0.5f);
			if (rolling == WHEELS_RUN) burst(place(0, 1).subtract(0, wheelHigh, 0));
			return true;
		}

		/** They meet and burst: everything near struck and thrown out, a scorched crater. */
		private void burst(Vec3 at) {
			for (SpellConstruct wheel : wheels) wheel.act("burst");
			for (LivingEntity e : HeavenSpells.aroundPoint(level, at, WHEELS_BLAST + 8, e -> !HeavenSpells.spared(caster, e))) {
				if (e.position().distanceTo(at) > WHEELS_BLAST) continue;
				e.invulnerableTime = 0;
				burn(caster, e, null, WHEELS_BURST, WHEELS_BURN_SECONDS, spell);
				HeavenSpells.push(at, e, 2.0);
			}
			Devastation.crater(caster, level, at, WHEELS_CRATER, WHEELS_CRATER_DEPTH, Devastation.Floor.SCORCHED);
			SpellFx.send(level, SpellFx.Kind.FIRE_EXPLOSION, at, Vec3.ZERO, (float) WHEELS_BLAST, 0, 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, at, Vec3.ZERO, (float) WHEELS_BLAST * 1.6f, 0, 192);
			sound(at, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.6f);
		}

		@Override
		public void end() {
			for (SpellConstruct wheel : wheels) if (wheel != null) wheel.dissolve();
		}
	}

	/**
	 * Eight Trigrams Furnace: Laozi's furnace of eight trigrams, bronze on three legs, the eight trigrams round its belly and Samadhi fire in
	 * it (SpellConstruct TRIGRAM_FURNACE, its own GeckoLib model), falls out of the sky onto where its caster looks ({@link #FURNACE_FALL}):
	 * {@link #FURNACE_SLAM} to everything under it, and a scorched crater. For {@link #FURNACE_TICKS} it burns: every foe within
	 * {@link #FURNACE_RADIUS} is drawn into its belly (within {@link #FURNACE_INSIDE} of its heart) and held there, burning for
	 * {@link #FURNACE_BURN} a second; every {@link #FURNACE_JET_EVERY} ticks jets of Samadhi fire burst out of its eight windows along the land
	 * to {@link #FURNACE_JETS}, striking whatever is in their way for {@link #FURNACE_JET} and burning trenches. Then it bursts open:
	 * {@link #FURNACE_BURST} to everything within {@link #FURNACE_RADIUS}, hurled out, and the ground under it melted into a glassy pit.
	 */
	public static final class Furnace extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground, sky;
		private final SpellConstruct furnace;
		private final float yaw;

		Furnace(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, FURNACE_FALL + FURNACE_TICKS + FURNACE_BURSTS + FURNACE_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.sky = ground.add(0, FURNACE_HIGH, 0);
			this.yaw = caster.getYRot();
			this.furnace = SpellConstruct.create(level, SpellConstruct.Kind.TRIGRAM_FURNACE, sky, yaw, 0, (int) (end - start), caster).sizedFor(spell).spawn();
			sound(sky, SoundEvents.BLAZE_AMBIENT, 4.0f, 0.4f);
			sound(sky, SoundEvents.BEACON_ACTIVATE, 3.0f, 0.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age <= FURNACE_FALL) {
				double f = age / (double) FURNACE_FALL;
				furnace.place(sky.lerp(ground, f * f), yaw, 0);
				if (age == FURNACE_FALL) land();
				return true;
			}
			long burning = age - FURNACE_FALL;
			if (burning < FURNACE_TICKS) {
				hold(burning);
				if (burning % FURNACE_JET_EVERY == FURNACE_JET_EVERY - 1) jets();
				return true;
			}
			if (burning == FURNACE_TICKS) {
				furnace.act("burst");
				sound(ground, SoundEvents.BLAZE_SHOOT, 4.0f, 0.3f);
			}
			if (burning == FURNACE_TICKS + FURNACE_BURSTS) burst();
			return true;
		}

		/** It lands: everything under it struck, a scorched crater round its feet. */
		private void land() {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, FURNACE_INSIDE * 1.6 + 8, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > FURNACE_INSIDE * 1.6 || e.getY() > ground.y + 16 || e.getY() < ground.y - 8) continue;
				e.invulnerableTime = 0;
				burn(caster, e, null, FURNACE_SLAM, 6, spell);
			}
			Devastation.crater(caster, level, ground, FURNACE_CRATER, FURNACE_CRATER_DEPTH, Devastation.Floor.SCORCHED);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) FURNACE_RADIUS, 0, 192);
			SpellFx.send(level, SpellFx.Kind.FLAME_BURST, ground.add(0, 2, 0), Vec3.ZERO, 8, 0, 192);
			sound(ground, SoundEvents.ANVIL_LAND, 4.0f, 0.3f);
			sound(ground, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.6f);
		}

		/** A tick of its burning: foes drawn into its belly and held there, burnt every second. */
		private void hold(long burning) {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, FURNACE_RADIUS + 12, e -> !HeavenSpells.spared(caster, e))) {
				double d = Math.hypot(e.getX() - ground.x, e.getZ() - ground.z);
				if (d > FURNACE_RADIUS || e.getY() > ground.y + 24 || e.getY() < ground.y - 12) continue;
				if (d > FURNACE_INSIDE * 0.7 && !HeavenSpells.greatBeast(caster, e)) {
					Vec3 in = ground.add(0, 1, 0).subtract(e.position()).multiply(1, 0.3, 1).normalize();
					HeavenSpells.move(e, in.scale(d > FURNACE_INSIDE ? 0.9 : 0.4).add(0, 0.08, 0));
					HeavenSpells.safeFall(e, 60);
				}
				if (burning % 20 == 10 && d <= FURNACE_INSIDE) burn(caster, e, null, FURNACE_BURN, 4, spell);
			}
			if (burning % 20 == 10) {
				SpellFx.send(level, SpellFx.Kind.FLAME_BURST, ground.add(0, FURNACE_INSIDE * 0.8, 0), Vec3.ZERO, 6, 0, 192);
				sound(ground, SoundEvents.FIRE_AMBIENT, 4.0f, 0.5f);
			}
		}

		/** Its eight windows jet Samadhi fire along the land: whatever is in their way struck, a burning trench under each. */
		private void jets() {
			furnace.act("jet");
			Vec3 from = ground.add(0, 1, 0);
			for (int k = 0; k < 8; k++) {
				Vec3 way = Vec3.directionFromRotation(0, yaw + 22.5f + 45 * k);
				Vec3 to = from.add(way.scale(FURNACE_JETS));
				for (LivingEntity e : HeavenSpells.aroundPoint(level, from.add(way.scale(FURNACE_JETS / 2)), FURNACE_JETS / 2 + 4, e -> !HeavenSpells.spared(caster, e))) {
					Vec3 rel = e.position().subtract(from);
					double t = rel.dot(way);
					if (t < FURNACE_INSIDE || t > FURNACE_JETS || rel.subtract(way.scale(t)).multiply(1, 0, 1).length() > FURNACE_JET_WIDTH + 1.5
							|| Math.abs(rel.y) > 10) continue;
					e.invulnerableTime = 0;
					burn(caster, e, null, FURNACE_JET, 6, spell);
					HeavenSpells.push(from, e, 1.2);
				}
				Devastation.trench(caster, level, from.add(way.scale(FURNACE_INSIDE)), to, FURNACE_JET_WIDTH, FURNACE_JET_DEPTH);
				Devastation.burn(caster, level, from.add(way.scale(FURNACE_JETS * 0.7)), FURNACE_JET_WIDTH * 2, 0.4);
				SpellFx.send(level, SpellFx.Kind.STREAK, from.add(way.scale(FURNACE_INSIDE)).add(0, 1.5, 0), way.scale(FURNACE_JETS - FURNACE_INSIDE), 0, SAMADHI_GOLD, 192);
				// Its fire seen all along the jet, not only at the window: bursts of flame out along it.
				for (int i = 1; i <= 4; i++) {
					double along = FURNACE_INSIDE + (FURNACE_JETS - FURNACE_INSIDE) * i / 4.5;
					SpellFx.send(level, SpellFx.Kind.FLAME_BURST, from.add(way.scale(along)).add(0, 1, 0), Vec3.ZERO, 3.5f, 0, 192);
				}
			}
			sound(ground, SoundEvents.BLAZE_SHOOT, 4.0f, 0.5f);
		}

		/** It bursts open: everything near struck and hurled out, the ground under it melted into a glassy pit, the land round it burning. */
		private void burst() {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, FURNACE_RADIUS + 12, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > FURNACE_RADIUS || e.getY() > ground.y + 24 || e.getY() < ground.y - 12) continue;
				e.invulnerableTime = 0;
				burn(caster, e, null, FURNACE_BURST, 10, spell);
				HeavenSpells.move(e, e.position().subtract(ground).multiply(1, 0, 1).normalize().scale(2.4).add(0, 1.2, 0));
				HeavenSpells.safeFall(e, 120);
			}
			Devastation.crater(caster, level, ground, FURNACE_PIT, FURNACE_PIT_DEPTH, Devastation.Floor.GLASSY);
			Devastation.burn(caster, level, ground, FURNACE_SCORCH, 0.14);
			SpellFx.send(level, SpellFx.Kind.FIRE_EXPLOSION, ground.add(0, 3, 0), Vec3.ZERO, (float) FURNACE_RADIUS, 0, 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) FURNACE_RADIUS * 1.8f, 0, 192);
			SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, 6, 0), Vec3.ZERO, 14, SAMADHI_GOLD, 192);
			sound(ground, SoundEvents.GENERIC_EXPLODE, 5.0f, 0.4f);
			sound(ground, SoundEvents.DRAGON_FIREBALL_EXPLODE, 4.0f, 0.5f);
			furnace.dissolve();
		}

		@Override
		public void end() {
			furnace.dissolve();
		}
	}

	// --- Entering Nirvana ---

	/**
	 * Undying Ember: the ember the phoenix leaves when it burns, wrapped in feathers of flame (SpellConstruct UNDYING_EMBER, its own GeckoLib
	 * model), flies from its caster's hands to where they look ({@link #EMBER_FLIGHT}) and hangs {@link #EMBER_HOVER} over the land there; it
	 * bursts three times, {@link #EMBER_EVERY} ticks apart, sinking low after each and blazing up again, each burst greater: everything within
	 * {@link #EMBER_BLAST} takes {@link #EMBER_DAMAGE}, burning, thrown out, and the land is blown into a scorched crater ({@link #EMBER_CRATER}).
	 */
	public static final class Ember extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 from, ground, rest;
		private final SpellConstruct ember;

		Ember(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, EMBER_FLIGHT + 2 * EMBER_EVERY + EMBER_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.rest = ground.add(0, EMBER_HOVER, 0);
			this.from = HeavenSpells.fromHands(caster);
			this.ember = SpellConstruct.create(level, SpellConstruct.Kind.UNDYING_EMBER, from, caster.getYRot(), 0, (int) (end - start), caster).sizedFor(spell).spawn();
			sound(from, SoundEvents.BLAZE_SHOOT, 2.0f, 0.7f);
			sound(from, SoundEvents.FIRECHARGE_USE, 2.0f, 0.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age <= EMBER_FLIGHT) {
				double f = age / (double) EMBER_FLIGHT;
				Vec3 here = from.lerp(rest, f).add(0, Math.sin(Math.PI * f) * Math.min(10, from.distanceTo(rest) * 0.2), 0);
				float[] face = HeavenSpells.facing(rest.subtract(from).multiply(1, 0, 1));
				ember.place(here, face[0], 0);
				if (age == EMBER_FLIGHT) burst(0);
				return true;
			}
			long since = age - EMBER_FLIGHT;
			int k = (int) (since / EMBER_EVERY);
			if (since % EMBER_EVERY == 0 && k >= 1 && k <= 2) burst(k);
			if (since % EMBER_EVERY == EMBER_DIMS && k < 2) {
				ember.act("dim");
				sound(rest, SoundEvents.FIRE_EXTINGUISH, 2.0f, 0.5f);
			}
			if (since % 4 == 0 && k < 2) SpellFx.send(level, SpellFx.Kind.FLAME_BURST, rest, Vec3.ZERO, 1.5f, 0);
			return true;
		}

		/** Its {@code k}th burst (0, 1, 2), each greater than the last: everything near struck, burning, thrown out; the land cratered. */
		private void burst(int k) {
			ember.act("flare");
			double blast = EMBER_BLAST[k];
			for (LivingEntity e : HeavenSpells.aroundPoint(level, rest, blast + 4, e -> !HeavenSpells.spared(caster, e))) {
				if (HeavenSpells.middle(e).distanceTo(rest) > blast + e.getBbWidth()) continue;
				e.invulnerableTime = 0;
				burn(caster, e, null, EMBER_DAMAGE[k], EMBER_BURN, spell);
				HeavenSpells.push(rest, e, 1.2 + 0.4 * k);
				HeavenSpells.safeFall(e, 80);
			}
			Devastation.crater(caster, level, ground, EMBER_CRATER[k], EMBER_CRATER_DEPTH[k], Devastation.Floor.SCORCHED);
			Devastation.burn(caster, level, ground, blast, 0.15);
			SpellFx.send(level, SpellFx.Kind.FIRE_EXPLOSION, rest, Vec3.ZERO, (float) blast, 0, 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) blast * 1.4f, 0, 192);
			SpellFx.send(level, SpellFx.Kind.FLASH, rest, Vec3.ZERO, (float) (6 + 3 * k), SAMADHI_GOLD, 192);
			sound(rest, SoundEvents.GENERIC_EXPLODE, 3.0f + k, 0.9f - 0.15f * k);
			sound(rest, SoundEvents.BLAZE_SHOOT, 3.0f, 0.4f);
		}

		@Override
		public void end() {
			ember.dissolve();
		}
	}

	/**
	 * Pyre of the Heavens: a ring of cribs of great logs stacked crosswise and burning, walls of fire between them (SpellConstruct HEAVEN_PYRE,
	 * its own GeckoLib model), rises round where its caster looks, {@link #PYRE_RADIUS} round ({@link #PYRE_RISES}), and burns for
	 * {@link #PYRE_TICKS}: no foe inside gets out through its walls of fire (it is thrown back in, burning), and every second every foe within takes
	 * {@link #PYRE_BURN}, and {@link #PYRE_RAMP} more each second after, as the pyre roars higher. Then it collapses in on its heart
	 * ({@link #PYRE_FALLS}): {@link #PYRE_COLLAPSE} to every foe within, and the land blown into a vast scorched crater, burning round it.
	 */
	public static final class Pyre extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final SpellConstruct pyre;

		Pyre(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, PYRE_RISES + PYRE_TICKS + PYRE_FALLS + PYRE_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.pyre = SpellConstruct.create(level, SpellConstruct.Kind.HEAVEN_PYRE, ground, caster.getYRot(), 0, (int) (end - start), caster).sizedFor(spell).spawn();
			sound(ground, SoundEvents.BLAZE_AMBIENT, 4.0f, 0.4f);
			sound(ground, SoundEvents.FIRECHARGE_USE, 4.0f, 0.3f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < PYRE_RISES) return true;
			long burning = age - PYRE_RISES;
			if (burning == 0) Devastation.burn(caster, level, ground, PYRE_RADIUS, 0.04);
			if (burning < PYRE_TICKS) {
				hold();
				if (burning % 20 == 10) roar((int) (burning / 20));
				return true;
			}
			if (burning == PYRE_TICKS) {
				pyre.act("collapse");
				sound(ground, SoundEvents.WOOD_BREAK, 4.0f, 0.4f);
			}
			if (burning == PYRE_TICKS + PYRE_FALLS) collapse();
			return true;
		}

		/** A tick of its burning: whoever would pass out through its walls of fire thrown back in, burning. */
		private void hold() {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, PYRE_RADIUS * 1.25 + 8, e -> !HeavenSpells.spared(caster, e))) {
				Vec3 off = e.position().subtract(ground).multiply(1, 0, 1);
				double d = off.length();
				if (d < PYRE_RADIUS * 0.9 || d > PYRE_RADIUS * 1.12 || e.getY() > ground.y + 20 || e.getY() < ground.y - 10 || HeavenSpells.greatBeast(caster, e)) continue;
				HeavenSpells.move(e, off.normalize().scale(-0.8).add(0, 0.25, 0));
				e.setSecondsOnFire(4);
				HeavenSpells.safeFall(e, 60);
			}
		}

		/** Its {@code k}th second: every foe within burnt, more than the second before; its flames leaping. */
		private void roar(int k) {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, PYRE_RADIUS + 8, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > PYRE_RADIUS || e.getY() > ground.y + 20 || e.getY() < ground.y - 10) continue;
				e.invulnerableTime = 0;
				burn(caster, e, null, PYRE_BURN + PYRE_RAMP * k, 4, spell);
			}
			for (int i = 0; i < 4 + k; i++) {
				double a = level.random.nextDouble() * Math.PI * 2, r = PYRE_RADIUS * (0.2 + 0.75 * level.random.nextDouble());
				SpellFx.send(level, SpellFx.Kind.FLAME_BURST, ground.add(Math.cos(a) * r, 1, Math.sin(a) * r), Vec3.ZERO, 3 + k * 0.4f, 0, 192);
			}
			sound(ground, SoundEvents.FIRE_AMBIENT, 4.0f, 0.4f + 0.05f * k);
		}

		/** It collapses on its heart: every foe within struck and thrown out, the land blown into a vast scorched crater. */
		private void collapse() {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, PYRE_RADIUS + 10, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > PYRE_RADIUS + 2 || e.getY() > ground.y + 24 || e.getY() < ground.y - 12) continue;
				e.invulnerableTime = 0;
				burn(caster, e, null, PYRE_COLLAPSE, 10, spell);
				HeavenSpells.move(e, e.position().subtract(ground).multiply(1, 0, 1).normalize().scale(2.0).add(0, 1.2, 0));
				HeavenSpells.safeFall(e, 120);
			}
			Devastation.crater(caster, level, ground, PYRE_CRATER, PYRE_CRATER_DEPTH, Devastation.Floor.SCORCHED);
			Devastation.burn(caster, level, ground, PYRE_SCORCH, 0.12);
			SpellFx.send(level, SpellFx.Kind.FIRE_EXPLOSION, ground.add(0, 3, 0), Vec3.ZERO, (float) PYRE_RADIUS, 0, 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) PYRE_RADIUS * 1.6f, 0, 192);
			SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, 6, 0), Vec3.ZERO, 16, SAMADHI_GOLD, 192);
			sound(ground, SoundEvents.GENERIC_EXPLODE, 6.0f, 0.4f);
			sound(ground, SoundEvents.DRAGON_FIREBALL_EXPLODE, 5.0f, 0.4f);
		}

		@Override
		public void end() {
			pyre.dissolve();
		}
	}

	// --- True Immortal ---

	/**
	 * Samadhi True Fire: the three fires of Samadhi, the heart's crimson, the spirit's gold and the essence's white, erupt where its caster looks
	 * in three pillars braided about one another (SpellConstruct SAMADHI_PILLARS, its own GeckoLib model; {@link #TRUE_FIRE_ERUPTS}), scorching
	 * the land, and burn for {@link #TRUE_FIRE_TICKS}: every foe within {@link #TRUE_FIRE_PULL} is drawn in to them (a great beast slowly), and
	 * every {@link #TRUE_FIRE_EVERY} ticks every foe within {@link #TRUE_FIRE_RADIUS} takes {@link #TRUE_FIRE_BURN} (true fire: qi, not flame, so
	 * it burns the fire-proof too). Then the three meet in one white flare ({@link #TRUE_FIRE_MEETS}):
	 * {@link #TRUE_FIRE_FLARE} to every foe within {@link #TRUE_FIRE_FLARE_RADIUS}, the land burnt into a scorched crater.
	 */
	public static final class TrueFire extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final SpellConstruct pillars;

		TrueFire(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, TRUE_FIRE_ERUPTS + TRUE_FIRE_TICKS + TRUE_FIRE_MEETS + TRUE_FIRE_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.pillars = SpellConstruct.create(level, SpellConstruct.Kind.SAMADHI_PILLARS, ground, caster.getYRot(), 0, (int) (end - start), caster).sizedFor(spell).spawn();
			sound(ground, SoundEvents.BLAZE_SHOOT, 5.0f, 0.4f);
			sound(ground, SoundEvents.FIRECHARGE_USE, 5.0f, 0.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < TRUE_FIRE_ERUPTS) return true;
			long burning = age - TRUE_FIRE_ERUPTS;
			if (burning == 0) {
				Devastation.burn(caster, level, ground, TRUE_FIRE_RADIUS, 0.2);
				SpellFx.send(level, SpellFx.Kind.FIRE_EXPLOSION, ground.add(0, 2, 0), Vec3.ZERO, (float) TRUE_FIRE_RADIUS, 0, 192);
			}
			if (burning < TRUE_FIRE_TICKS) {
				draw();
				if (burning % TRUE_FIRE_EVERY == 0) burn();
				return true;
			}
			if (burning == TRUE_FIRE_TICKS) {
				pillars.act("converge");
				sound(ground, SoundEvents.BEACON_POWER_SELECT, 5.0f, 1.4f);
			}
			if (burning == TRUE_FIRE_TICKS + TRUE_FIRE_MEETS) flare();
			return true;
		}

		/** A tick of its burning: every foe near drawn in to the pillars. */
		private void draw() {
			for (LivingEntity e : WoodSpells.foesWithin(caster, level, ground, TRUE_FIRE_PULL, 10, 30)) {
				double dx = e.getX() - ground.x, dz = e.getZ() - ground.z, d = Math.max(1.0e-3, Math.hypot(dx, dz));
				if (d < 2) continue;
				double pull = HeavenSpells.greatBeast(caster, e) ? 0.1 : 0.32;
				Vec3 v = e.getDeltaMovement();
				HeavenSpells.move(e, new Vec3(-dx / d * pull, v.y, -dz / d * pull));
			}
		}

		/** Every foe within burnt by the true fire. */
		private void burn() {
			for (LivingEntity e : WoodSpells.foesWithin(caster, level, ground, TRUE_FIRE_RADIUS, 10, 40)) {
				HeavenSpells.strikeAgain(caster, e, TRUE_FIRE_BURN, spell);
				e.setSecondsOnFire(4);
				SpellFx.send(level, SpellFx.Kind.FLAME_BURST, HeavenSpells.middle(e), Vec3.ZERO, 1.6f, 0);
			}
			for (int i = 0; i < 5; i++) {
				double a = level.random.nextDouble() * Math.PI * 2, r = TRUE_FIRE_RADIUS * level.random.nextDouble();
				SpellFx.send(level, SpellFx.Kind.FLAME_BURST, ground.add(Math.cos(a) * r, 1 + level.random.nextDouble() * 20, Math.sin(a) * r), Vec3.ZERO, 3, 0, 192);
			}
			sound(ground, SoundEvents.FIRE_AMBIENT, 5.0f, 0.5f);
			sound(ground, SoundEvents.BLAZE_BURN, 3.0f, 0.4f);
		}

		/** The three meet in one white flare: every foe within struck and thrown out, the land burnt into a scorched crater. */
		private void flare() {
			for (LivingEntity e : WoodSpells.foesWithin(caster, level, ground, TRUE_FIRE_FLARE_RADIUS, 12, 50)) {
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, TRUE_FIRE_FLARE, spell);
				e.setSecondsOnFire(10);
				if (!HeavenSpells.greatBeast(caster, e)) HeavenSpells.push(ground, e, 2.5);
				HeavenSpells.safeFall(e, 100);
			}
			Devastation.crater(caster, level, ground, TRUE_FIRE_CRATER, TRUE_FIRE_CRATER_DEPTH, Devastation.Floor.SCORCHED);
			Devastation.burn(caster, level, ground, TRUE_FIRE_FLARE_RADIUS * 1.4, 0.12);
			SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, 8, 0), Vec3.ZERO, 18, 0xFFFBEA, 256);
			SpellFx.send(level, SpellFx.Kind.FIRE_EXPLOSION, ground.add(0, 4, 0), Vec3.ZERO, (float) TRUE_FIRE_FLARE_RADIUS, 0, 256);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) TRUE_FIRE_FLARE_RADIUS * 1.4f, 0, 256);
			sound(ground, SoundEvents.GENERIC_EXPLODE, 6.0f, 0.5f);
			sound(ground, SoundEvents.DRAGON_FIREBALL_EXPLODE, 5.0f, 0.6f);
		}

		@Override
		public void end() {
			pillars.dissolve();
		}
	}

	/**
	 * First Flame Before Heaven: the flower of chaos-fire that burned before heaven, its petals of white flame and black by turns (SpellConstruct
	 * CHAOS_FLOWER, its own GeckoLib model), opens where its caster looks, {@link #FIRST_FLAME_RADIUS} round ({@link #FIRST_FLAME_OPENS}), and
	 * burns for {@link #FIRST_FLAME_TICKS}: every {@link #FIRST_FLAME_EVERY} ticks every foe within takes {@link #FIRST_FLAME_BURN}. Then its
	 * petals slam shut on its heart, drawing every foe within in, and its heart bursts ({@link #FIRST_FLAME_IMPLODES}): {@link #FIRST_FLAME_BURST}
	 * to every foe within, the land blown into a crater of glass.
	 */
	public static final class FirstFlame extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final SpellConstruct flower;

		FirstFlame(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, FIRST_FLAME_OPENS + FIRST_FLAME_TICKS + FIRST_FLAME_IMPLODES + FIRST_FLAME_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.flower = SpellConstruct.create(level, SpellConstruct.Kind.CHAOS_FLOWER, ground.add(0, 0.2, 0), caster.getYRot(), 0, (int) (end - start), caster).sizedFor(spell).spawn();
			sound(ground, SoundEvents.BLAZE_SHOOT, 5.0f, 0.3f);
			sound(ground, SoundEvents.BEACON_ACTIVATE, 5.0f, 0.4f);
		}

		private List<LivingEntity> within() {
			return WoodSpells.foesWithin(caster, level, ground, FIRST_FLAME_RADIUS, 10, 30);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < FIRST_FLAME_OPENS) return true;
			long burning = age - FIRST_FLAME_OPENS;
			if (burning == 0) Devastation.burn(caster, level, ground, FIRST_FLAME_RADIUS, 0.08);
			if (burning < FIRST_FLAME_TICKS) {
				if (burning % FIRST_FLAME_EVERY == 0) burn((int) (burning / FIRST_FLAME_EVERY));
				return true;
			}
			if (burning == FIRST_FLAME_TICKS) {
				flower.act("implode");
				sound(ground, SoundEvents.WARDEN_SONIC_CHARGE, 5.0f, 0.6f);
			}
			long imploding = burning - FIRST_FLAME_TICKS;
			if (imploding >= 0 && imploding < FIRST_FLAME_IMPLODES) draw();
			if (imploding == FIRST_FLAME_IMPLODES) burst();
			return true;
		}

		/** Its {@code k}th burning: every foe within struck by the chaos-fire, white and black flames leaping over it. */
		private void burn(int k) {
			for (LivingEntity e : within()) {
				HeavenSpells.strikeAgain(caster, e, FIRST_FLAME_BURN, spell);
				e.setSecondsOnFire(4);
				SpellFx.send(level, SpellFx.Kind.FLAME_BURST, HeavenSpells.middle(e), Vec3.ZERO, 1.6f, k % 2 == 0 ? CHAOS_WHITE : CHAOS_BLACK);
			}
			for (int i = 0; i < 6; i++) {
				double a = level.random.nextDouble() * Math.PI * 2, r = FIRST_FLAME_RADIUS * (0.2 + 0.8 * level.random.nextDouble());
				SpellFx.send(level, SpellFx.Kind.FLAME_BURST, ground.add(Math.cos(a) * r, 1 + level.random.nextDouble() * 4, Math.sin(a) * r), Vec3.ZERO, 3, i % 2 == 0 ? CHAOS_WHITE : CHAOS_BLACK, 192);
			}
			sound(ground, SoundEvents.FIRE_AMBIENT, 5.0f, 0.3f + 0.05f * k);
		}

		/** A tick of its slamming shut: every foe within drawn in to its heart (a great beast slowly). */
		private void draw() {
			for (LivingEntity e : within()) {
				Vec3 to = ground.add(0, 2, 0).subtract(e.position());
				double pull = HeavenSpells.greatBeast(caster, e) ? 0.15 : Math.min(2.5, to.length() / 5);
				HeavenSpells.move(e, to.normalize().scale(pull));
				HeavenSpells.safeFall(e, 80);
			}
		}

		/** Its heart bursts: every foe within struck and thrown out, the land blown into a crater of glass. */
		private void burst() {
			for (LivingEntity e : within()) {
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, FIRST_FLAME_BURST, spell);
				e.setSecondsOnFire(10);
				if (!HeavenSpells.greatBeast(caster, e)) HeavenSpells.move(e, e.position().subtract(ground).multiply(1, 0, 1).normalize().scale(2.4).add(0, 1.4, 0));
				HeavenSpells.safeFall(e, 120);
			}
			Devastation.crater(caster, level, ground, FIRST_FLAME_CRATER, FIRST_FLAME_CRATER_DEPTH, Devastation.Floor.GLASSY);
			Devastation.burn(caster, level, ground, FIRST_FLAME_RADIUS * 1.3, 0.1);
			SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, 6, 0), Vec3.ZERO, 22, CHAOS_WHITE, 256);
			SpellFx.send(level, SpellFx.Kind.FIRE_EXPLOSION, ground.add(0, 4, 0), Vec3.ZERO, (float) FIRST_FLAME_RADIUS, 0, 256);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) FIRST_FLAME_RADIUS * 1.5f, 0, 256);
			sound(ground, SoundEvents.GENERIC_EXPLODE, 6.0f, 0.4f);
			sound(ground, SoundEvents.WARDEN_SONIC_BOOM, 5.0f, 0.5f);
		}

		@Override
		public void end() {
			flower.dissolve();
		}
	}

	static void tick(MinecraftServer server) {}

	static void forget(UUID id) {
		BOMBS.remove(id);
	}

	static void clear() {
		BOMBS.clear();
	}

	private FireSpells() {}
}
