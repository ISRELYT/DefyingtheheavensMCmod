package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
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
 * The metal element's spells (Spell, HeavenSpells): metal is weak to fire and strong against wood (SpellAuras). Every number is the
 * design's; every look its own (the client's spell renderers).
 * <ul>
 *   <li>Qi Refining: Flying Sword (a sword of metal formed in the air and shot), Blade Polish (melee x1.2, a white wisp on the
 *       blade), Steel Hurricane (a dash with the held weapon, spinning, as a trident's riptide).</li>
 *   <li>Foundation Building: Boulder Cleave (a crescent of sword qi that cuts through the ground for 15 blocks), Mineral
 *       Purification (every ore within the reach of one's consciousness drawn out of its rock), Advanced Flying Sword Technique (six
 *       swords formed round you one by one, loosed one by one).</li>
 *   <li>Core Formation: Sword Rain, Golden Bell Shield.</li>
 *   <li>Nascent Soul: Piercing Sword Light, Vajra Body.</li>
 *   <li>Heavenly Being: Mountain Cleave (a slash that cuts through mountains), Sword Domain.</li>
 *   <li>Four Axis: Heaven-Severing Sword, Four Swords Array.</li>
 *   <li>Dao Integration: White Tiger's Pounce (a tiger of sword light, and its rake), Myriad Swords Return to One (a colossal sword).</li>
 *   <li>Void Shattering: Void-Severing Sword Light (a stroke that cuts space itself, and leaves it cut open), Ten-Direction Sword Prison
 *       (swords out of ten rifts: a ring of them holds all within, and the last two meet in the middle).</li>
 * </ul>
 */
public final class MetalSpells {
	public static final double SWORD_SPEED = 2.6, SWORD_RANGE = 48;
	public static final float SWORD_DAMAGE = 6;
	public static final int POLISH_TICKS = 30 * 20, HURRICANE_TICKS = 32;
	public static final double HURRICANE_SPEED = 2.6;
	public static final float HURRICANE_DAMAGE = 3;
	public static final double CLEAVE_SPEED = 1.5, CLEAVE_RANGE = 15;
	public static final float CLEAVE_DAMAGE = 14;
	public static final int CLEAVE_WIDTH = 3, CLEAVE_HEIGHT = 2;
	public static final int ORES_MAX = 256;
	public static final int SWORDS = 6, SWORD_FORM_EVERY = 5, SWORD_LOOSE_EVERY = 6;
	public static final float ADVANCED_DAMAGE = 9;
	public static final double RAIN_RANGE = 32, RAIN_RADIUS = 5;
	public static final int RAIN_SWORDS = 12, RAIN_FORM = 15;
	public static final float RAIN_DAMAGE = 6;
	public static final float BELL_HOLD = 40;
	public static final int BELL_TICKS = 10 * 20;
	/** The sword light's tunnel: 2.5 round, by its realm (Devastation#sized). */
	public static final double LIGHT_RANGE = 64, LIGHT_BORE = Devastation.sized(Realm.NASCENT_SOUL, 4.4);
	public static final float LIGHT_DAMAGE = 55;
	public static final int VAJRA_TICKS = 12 * 20;
	public static final double MOUNTAIN_SPEED = 4.0, MOUNTAIN_RANGE = 140;
	public static final float MOUNTAIN_DAMAGE = 150;
	/** The slot Mountain Cleave cuts: 6 wide and 23 tall, by its realm (Devastation#sized); its crescent drawn as large. */
	public static final int MOUNTAIN_WIDTH = (int) Devastation.sized(Realm.HEAVENLY_BEING, 8), MOUNTAIN_HEIGHT = (int) Devastation.sized(Realm.HEAVENLY_BEING, 30);
	public static final float MOUNTAIN_SCALE = 4.0f * MOUNTAIN_HEIGHT / 16f;
	public static final double DOMAIN_RADIUS = 10;
	public static final int DOMAIN_TICKS = 12 * 20;
	public static final float DOMAIN_DAMAGE = 20;
	/**
	 * Heaven-Severing Sword: a level crescent of {@link #SEVER_RADIUS} round {@link #SEVER_SPAN} radians (its tips about 110 blocks
	 * apart, trailing 0.59 of the radius behind its bow), {@link #SEVER_THICK} blocks thick at its middle and a quarter of that at its
	 * tips, its middle {@link #SEVER_LIFT} over the ground it set out from; it flies {@link #SEVER_RANGE} at {@link #SEVER_SPEED} a
	 * tick and cuts only what its blade passes through. The client draws it to the same numbers (SpellProjectileRenderer).
	 */
	public static final double SEVER_RANGE = 120, SEVER_SPEED = 6;
	/**
	 * The stroke that looses it (SpellConstruct SEVERING_SWORD): a sword of light {@link #SEVER_SWORD} blocks long forms in the caster's
	 * hand over {@link #SEVER_FORM} ticks and is swung level across their front over {@link #SEVER_SWING}; the slash leaves it
	 * {@link #SEVER_RELEASE} ticks in, as its blade crosses the front, and the sword is gone {@link #SEVER_STROKE} ticks in.
	 */
	public static final float SEVER_SWORD = 34;
	public static final int SEVER_FORM = 4, SEVER_SWING = 5, SEVER_RELEASE = 6, SEVER_STROKE = 22;
	public static final float SEVER_DAMAGE = 300;
	/** Its slit {@link #SEVER_THICK} tall at its middle: half what it was (the user's ask, 2026-10-23; 10). */
	public static final float SEVER_RADIUS = 60, SEVER_SPAN = 2.3f, SEVER_THICK = 5, SEVER_LIFT = 1.3f;
	/** The slit it cuts in the land, of its blade's height: half (the user's ask, 2026-10-24: the slash's wound half as tall again). */
	public static final double SEVER_CUT_SHARE = 0.5;

	/**
	 * The severing slash's way, asked to load (out to its tips) and tick (round its middle) the moment it is cast, and again just
	 * ahead of it as it flies: its 120 blocks (and its tips 55 out to either side) run past the usual simulation distance, where it would otherwise stop dead and its cut
	 * with it (land never made before is made meanwhile; the slash waits at its edge until it is). Each lapses on its own.
	 */
	private static final TicketType<ChunkPos> SEVER_TICKET = TicketType.create("dth_sever", java.util.Comparator.comparingLong(ChunkPos::toLong), 300);
	private static final int SEVER_TICKET_RADIUS = 4;

	private static void loadSeverWay(ServerLevel level, Vec3 at) {
		ChunkPos chunk = new ChunkPos(BlockPos.containing(at));
		level.getChunkSource().addRegionTicket(SEVER_TICKET, chunk, SEVER_TICKET_RADIUS, chunk);
	}

	/** How thick the severing blade is (top to bottom) at {@code across} blocks out to the side of its middle (0 past its tips). */
	public static float severThickness(double across) {
		double s = across / SEVER_RADIUS;
		if (Math.abs(s) > Math.sin(SEVER_SPAN / 2)) return 0;
		double a = Math.asin(s);
		return (float) (SEVER_THICK * (0.25 + 0.75 * Math.sin(Math.PI * (a / SEVER_SPAN + 0.5))));
	}

	/** How far behind its bow the severing blade is at {@code across} blocks out to the side (its crescent's curve). */
	public static double severLag(double across) {
		double s = Mth.clamp(across / SEVER_RADIUS, -1, 1);
		return SEVER_RADIUS * (1 - Math.cos(Math.asin(s)));
	}
	public static final double ARRAY_RANGE = 48, ARRAY_HALF = 16;
	public static final int ARRAY_TICKS = 8 * 20;
	public static final float ARRAY_DAMAGE = 80;
	/** Where each of the Four Swords is driven in: a crater this round and deep (by its realm, Devastation#sized). */
	public static final double ARRAY_CRATER = Devastation.sized(Realm.FOUR_AXIS, 7), ARRAY_CRATER_DEPTH = Devastation.sized(Realm.FOUR_AXIS, 10);
	/**
	 * White Tiger's Pounce: how far it leaps (up to {@link #TIGER_RANGE}), whom it lands on (within {@link #TIGER_RADIUS}); its rake, four
	 * gouges {@link #TIGER_GOUGE} long, {@link #TIGER_SPREAD} apart and 4 deep (by its realm), striking within {@link #TIGER_RAKE_REACH} of
	 * them; its crouch, leap, the claw after it lands, the rake's running out, and how long it stays (ticks).
	 */
	public static final double TIGER_RANGE = HeavenSpells.span(Realm.DAO_INTEGRATION, 64), TIGER_RADIUS = HeavenSpells.span(Realm.DAO_INTEGRATION, 8), TIGER_GOUGE = HeavenSpells.span(Realm.DAO_INTEGRATION, 80), TIGER_SPREAD = HeavenSpells.span(Realm.DAO_INTEGRATION, 3.5),
			TIGER_RAKE_REACH = HeavenSpells.span(Realm.DAO_INTEGRATION, 2.5),
			TIGER_GOUGE_DEPTH = Devastation.sized(Realm.DAO_INTEGRATION, 3.5);
	public static final float TIGER_DAMAGE = 500, TIGER_RAKE_DAMAGE = 300;
	public static final int TIGER_CROUCH = 5, TIGER_LEAP = 14, TIGER_CLAW = 6, TIGER_RAKE = 10, TIGER_STAY = 24;
	/**
	 * Myriad Swords Return to One: where (up to {@link #SWORDS_RANGE}), how high over it the swords gather, whom the colossal sword strikes
	 * (within {@link #SWORDS_RADIUS}); its cross of slots (60 out each way, 8 deep) and its pit (19 deep, {@link #SWORDS_PIT} round: by its
	 * realm); the gathering, the merging, the fall and how long it stands (ticks).
	 */
	public static final double SWORDS_RANGE = HeavenSpells.span(Realm.DAO_INTEGRATION, 64), SWORDS_HIGH = HeavenSpells.span(Realm.DAO_INTEGRATION, 34), SWORDS_RADIUS = HeavenSpells.span(Realm.DAO_INTEGRATION, 12),
			SWORDS_PIT = Devastation.sized(Realm.DAO_INTEGRATION, 3.5), SWORDS_LOW = HeavenSpells.span(Realm.DAO_INTEGRATION, 6), SWORDS_SLOT = Devastation.sized(Realm.DAO_INTEGRATION, 47),
			SWORDS_SLOT_DEPTH = Devastation.sized(Realm.DAO_INTEGRATION, 6), SWORDS_PIT_DEPTH = Math.round(Devastation.sized(Realm.DAO_INTEGRATION, 15));
	public static final float SWORDS_DAMAGE = 600;
	public static final int SWORDS_GATHER = 30, SWORDS_MERGE = 24, SWORDS_FALL = 10, SWORDS_STAND = 6 * 20;
	/** Sword light's white, cold as moonlit steel: the rifts the swords cut glow so. */
	public static final int SWORD_WHITE = 0xD6E6FF;
	/**
	 * Void-Severing Sword Light: how far its stroke cuts (147 by its realm, through everything), how near its line it strikes (3), the chasm it
	 * cleaves under its line, in the rift's own shape (its half-width at its middle, 6, and its depth there below the line, 17: by its realm;
	 * pointed at both ends, at least {@link #SEVERING_BORE} round its line all along it), its harm and the rift's harm a second to whoever is in
	 * its cut; how long the stroke takes, and how long space stays cut open after it (ticks).
	 */
	public static final double SEVERING_LENGTH = HeavenSpells.span(Realm.VOID_SHATTERING, 80), SEVERING_WIDTH = HeavenSpells.span(Realm.VOID_SHATTERING, 1.6),
			SEVERING_BORE = Devastation.sized(Realm.VOID_SHATTERING, 1.2), SEVERING_CLEFT = Devastation.sized(Realm.VOID_SHATTERING, 3.5),
			SEVERING_CLEFT_DEPTH = Devastation.sized(Realm.VOID_SHATTERING, 9);
	public static final float SEVERING_DAMAGE = 1500, SEVERING_RIFT_DAMAGE = 400;
	public static final int SEVERING_THRUST = 6, SEVERING_RIFT = 3 * 20;
	/**
	 * Ten-Direction Sword Prison: where (up to {@link #PRISON_RANGE}, 118 by its realm), how high its rifts open (40) and how long each is, the
	 * ring its swords fall in ({@link #PRISON_RADIUS}, 13), each second's harm to whoever is held in it and the last two swords' meeting's; the
	 * craters the ring's swords leave and the pit where the last two meet (by its realm); how long the rifts take to open, the swords' fall, how
	 * long it holds, how long the last two take to meet, and how long it stands after (ticks).
	 */
	public static final double PRISON_RANGE = HeavenSpells.span(Realm.VOID_SHATTERING, 64), PRISON_HIGH = HeavenSpells.span(Realm.VOID_SHATTERING, 22),
			PRISON_RIFT = HeavenSpells.span(Realm.VOID_SHATTERING, 5), PRISON_RADIUS = HeavenSpells.span(Realm.VOID_SHATTERING, 7),
			PRISON_CRATER = Devastation.sized(Realm.VOID_SHATTERING, 2.2), PRISON_CRATER_DEPTH = Devastation.sized(Realm.VOID_SHATTERING, 3),
			PRISON_PIT = Devastation.sized(Realm.VOID_SHATTERING, 4), PRISON_PIT_DEPTH = Devastation.sized(Realm.VOID_SHATTERING, 5);
	public static final float PRISON_DAMAGE = 150, PRISON_MEETING = 1000;
	public static final int PRISON_OPENS = 12, PRISON_FALL = 12, PRISON_TICKS = 6 * 20, PRISON_MEET = 14, PRISON_AFTER = 24, PRISON_SWORDS = 8;
	/**
	 * Immortal-Binding Golden Ropes: where (up to {@link #ROPES_RANGE}, 136 by its realm), how high its ring hangs over the land (21), how far
	 * round it its ropes reach (30) and how many they bind at most, how far under the ring the bound hang (11); its constriction a second and
	 * its cinch at the end; how long the ring takes to come, how long a rope takes to haul its catch up, and how long they are held (ticks).
	 */
	public static final double ROPES_RANGE = HeavenSpells.span(Realm.SACRED_VESSEL, 64), ROPES_HIGH = HeavenSpells.span(Realm.SACRED_VESSEL, 10),
			ROPES_RADIUS = HeavenSpells.span(Realm.SACRED_VESSEL, 14), ROPES_HANG = HeavenSpells.span(Realm.SACRED_VESSEL, 5);
	public static final float ROPES_DAMAGE = 450, ROPES_CINCH = 900;
	public static final int ROPES_MOST = 8, ROPES_COMES = 14, ROPES_HAUL = 20, ROPES_TICKS = 5 * 20;
	/**
	 * Golden Dragon Scissors: where (up to {@link #SCISSORS_RANGE}, 136), how high over the land they open (17), how far each way along
	 * them they cut (32 each way of their pivot: 64 in all) and how near their blades they strike (5); their harm; how far apart their blades
	 * open (degrees off their line); the cut where their blades meet (5 wide, 15 deep) and the two shallower along where they lay open (3
	 * wide, 6 deep), by its realm; how long they take to open, how long they hold open, how long they take to snap, and how long after (ticks).
	 */
	public static final double SCISSORS_RANGE = HeavenSpells.span(Realm.SACRED_VESSEL, 64), SCISSORS_HIGH = HeavenSpells.span(Realm.SACRED_VESSEL, 8),
			SCISSORS_LENGTH = HeavenSpells.span(Realm.SACRED_VESSEL, 30), SCISSORS_WIDTH = HeavenSpells.span(Realm.SACRED_VESSEL, 2.5), SCISSORS_OPEN = 35,
			SCISSORS_CUT = Devastation.sized(Realm.SACRED_VESSEL, 2.5), SCISSORS_CUT_DEPTH = Devastation.sized(Realm.SACRED_VESSEL, 7),
			SCISSORS_SCAR = Devastation.sized(Realm.SACRED_VESSEL, 1.5), SCISSORS_SCAR_DEPTH = Devastation.sized(Realm.SACRED_VESSEL, 3);
	public static final float SCISSORS_DAMAGE = 3500;
	public static final int SCISSORS_OPENS = 20, SCISSORS_HOLD = 10, SCISSORS_SNAP = 5, SCISSORS_AFTER = 24;
	/**
	 * Sword Tomb Awakens: where (154 by its realm), how far round its ring of swords stands (12: its model's, SpellConstruct#sizedFor), and how
	 * many swords; the harm as they burst up, to everything within the ring; how far round where each lands it strikes (6) and how hard; the
	 * crater they leave at its heart (7 round, 5 deep, by its realm); how long they take to burst up, how long they stand, how long they take to
	 * rise, how far apart they plunge, and how long it has after (ticks).
	 */
	public static final double TOMB_RANGE = HeavenSpells.span(Realm.ENTERING_NIRVANA, 64), TOMB_RING = HeavenSpells.span(Realm.ENTERING_NIRVANA, 5),
			TOMB_IMPACT = HeavenSpells.span(Realm.ENTERING_NIRVANA, 2.5), TOMB_CRATER = Devastation.sized(Realm.ENTERING_NIRVANA, 3),
			TOMB_CRATER_DEPTH = Devastation.sized(Realm.ENTERING_NIRVANA, 2);
	public static final int TOMB_SWORDS = 24;
	public static final float TOMB_DAMAGE = 1000, TOMB_SWORD = 160;
	public static final int TOMB_ERUPTS = 16, TOMB_STANDS = 14, TOMB_RISES = 20, TOMB_PLUNGE = 6, TOMB_EVERY = 1, TOMB_AFTER = 12;
	/** A buried sword's light: white, cold. */
	public static final int TOMB_LIGHT = 0xE6F2FF;
	/**
	 * Nine Rebirths of the Sword: where (154 by its realm), how high over the spot it hangs as it sweeps (7), how far round its blade reaches (53:
	 * its model's length, as great as its realm makes it), how many degrees each sweep goes round; each of its eight sweeps' harm, and its ninth's,
	 * plunged into the land, to everything within 24; the crater of white-flame glass that leaves (14 round, 7 deep, by its realm) and how far its
	 * cracks run (38); its blade's length (55: its model's 92 pixels) and its pitch as it sweeps level, its point skimming the land; each sweep's
	 * tilt (rising off the land, less than nought, or falling onto it); how many sweeps, how long each takes and how long it is reborn after, how
	 * long it takes to form, to rise for its plunge and to fall, and how long it stands after (ticks).
	 */
	public static final double REBIRTH_RANGE = HeavenSpells.span(Realm.ENTERING_NIRVANA, 64), REBIRTH_HIGH = HeavenSpells.span(Realm.ENTERING_NIRVANA, 3),
			REBIRTH_REACH = HeavenSpells.span(Realm.ENTERING_NIRVANA, 22), REBIRTH_ARC = 210, REBIRTH_PLUNGE_RADIUS = HeavenSpells.span(Realm.ENTERING_NIRVANA, 10),
			REBIRTH_CRATER = Devastation.sized(Realm.ENTERING_NIRVANA, 6), REBIRTH_CRATER_DEPTH = Devastation.sized(Realm.ENTERING_NIRVANA, 3),
			REBIRTH_CRACKS = Devastation.sized(Realm.ENTERING_NIRVANA, 16), REBIRTH_BLADE = HeavenSpells.span(Realm.ENTERING_NIRVANA, 23),
			REBIRTH_SKIM = Math.toDegrees(Math.asin((REBIRTH_HIGH - 1.5) / REBIRTH_BLADE));
	/** The reborn sword's white flame. */
	public static final int REBIRTH_LIGHT = 0xF0F4FF;
	/**
	 * Heaven-Slaying Sword Rises: where (218 by its realm), how far round its blade strikes as it rises (10); how far its eight cuts run out
	 * from its foot (82) and how near one they strike (5), how wide and deep they cut the land (4 and 7, by its realm); its harm as it rises,
	 * and along a cut; how long it takes to rise, how long it stands before it flares, and how long it has after (ticks).
	 */
	public static final double SLAYER_RANGE = HeavenSpells.span(Realm.TRUE_IMMORTAL, 64), SLAYER_RADIUS = HeavenSpells.span(Realm.TRUE_IMMORTAL, 3),
			SLAYER_CUTS = HeavenSpells.span(Realm.TRUE_IMMORTAL, 24), SLAYER_CUT_WIDTH = HeavenSpells.span(Realm.TRUE_IMMORTAL, 1.5),
			SLAYER_CUT = Devastation.sized(Realm.TRUE_IMMORTAL, 1.2), SLAYER_CUT_DEPTH = Devastation.sized(Realm.TRUE_IMMORTAL, 2);
	public static final float SLAYER_RISE = 40000, SLAYER_CUT_DAMAGE = 20000;
	public static final int SLAYER_LINES = 8, SLAYER_RISES = 20, SLAYER_STANDS = 16, SLAYER_AFTER = 30;
	/** Sword-light: the white gold of the heaven-slaying sword. */
	public static final int SLAYER_LIGHT = 0xFFF4D0;
	/**
	 * Primal Axe Splits Chaos: how far behind its caster its haft's end stands (7), how far ahead of that it splits the land (89: its model's
	 * haft and head), how near its line it strikes (10), and how near its caster it leaves the land be (10); the chasm it splits (9 wide, 27 deep,
	 * by its realm) and how far the cracks run out where its head lands (34); its harm; how long it takes to be raised, how long it waits raised,
	 * how long its chop takes to land, and how long it has after (ticks).
	 */
	public static final double AXE_BACK = HeavenSpells.span(Realm.TRUE_IMMORTAL, 2), AXE_REACH = HeavenSpells.span(Realm.TRUE_IMMORTAL, 26),
			AXE_WIDTH = HeavenSpells.span(Realm.TRUE_IMMORTAL, 3), AXE_SPARE = 10, AXE_CHASM = Devastation.sized(Realm.TRUE_IMMORTAL, 2.5),
			AXE_CHASM_DEPTH = Devastation.sized(Realm.TRUE_IMMORTAL, 8), AXE_CRACKS = Devastation.sized(Realm.TRUE_IMMORTAL, 10);
	public static final float AXE_DAMAGE = 75000;
	public static final int AXE_RAISES = 16, AXE_HOLDS = 16, AXE_CHOPS = 8, AXE_AFTER = 30;
	/** The light of the beginning off the axe's edge, as chaos is split. */
	public static final int CHAOS_SPLIT = 0xFFE89A;
	public static final float REBIRTH_DAMAGE = 600, REBIRTH_PLUNGE = 2000;
	public static final float[] REBIRTH_TILTS = {0, -18, 14, -26, 22, -10, 30, 6};
	public static final int REBIRTHS = 8, REBIRTH_SWEEP = 10, REBIRTH_REBORN = 4, REBIRTH_FORMS = 10, REBIRTH_RISES = 10, REBIRTH_FALL = 6, REBIRTH_AFTER = 20;

	static boolean canCast(LivingEntity p, Spell spell) {
		return true;
	}

	static void cast(LivingEntity p, Spell spell) {
		ServerLevel level = HeavenSpells.level(p);
		switch (spell) {
			case FLYING_SWORD -> sword(p, spell, new Vec3(0.45, 0.15, 1.2), 8, SWORD_DAMAGE, 0);
			case BLADE_POLISH -> {
				SpellAuras.put(p, SpellAura.BLADE_POLISH, POLISH_TICKS);
				HeavenSpells.sound(level, p.position(), SoundEvents.GRINDSTONE_USE, 1.0f, 1.4f);
				HeavenSpells.sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.8f);
			}
			case STEEL_HURRICANE -> hurricane(p, spell);
			case BOULDER_CLEAVE -> cleave(p, spell, SpellProjectile.Shape.CLEAVE_SLASH, CLEAVE_SPEED, CLEAVE_RANGE, CLEAVE_DAMAGE, CLEAVE_WIDTH, CLEAVE_HEIGHT, 1.0f);
			case MINERAL_PURIFICATION -> purify(p);
			case ADVANCED_FLYING_SWORD_TECHNIQUE -> advanced(p, spell);
			case SWORD_RAIN -> rain(p, spell);
			case GOLDEN_BELL_SHIELD -> {
				SpellAuras.bell(p, BELL_HOLD, BELL_TICKS);
				HeavenSpells.sound(level, p.position(), SoundEvents.BELL_BLOCK, 2.0f, 0.5f);
				HeavenSpells.sound(level, p.position(), SoundEvents.BELL_RESONATE, 1.5f, 0.8f);
			}
			case PIERCING_SWORD_LIGHT -> light(p, spell);
			case VAJRA_BODY -> {
				SpellAuras.put(p, SpellAura.VAJRA_BODY, VAJRA_TICKS);
				HeavenSpells.sound(level, p.position(), SoundEvents.ARMOR_EQUIP_GOLD, 1.5f, 0.6f);
				HeavenSpells.sound(level, p.position(), SoundEvents.BEACON_POWER_SELECT, 1.0f, 1.2f);
			}
			case MOUNTAIN_CLEAVE -> cleave(p, spell, SpellProjectile.Shape.MOUNTAIN_SLASH, MOUNTAIN_SPEED, MOUNTAIN_RANGE, MOUNTAIN_DAMAGE, MOUNTAIN_WIDTH,
					MOUNTAIN_HEIGHT, MOUNTAIN_SCALE);
			case SWORD_DOMAIN -> {
				HeavenSpells.endLingering(p.getUUID(), Domain.class);
				HeavenSpells.linger(new Domain(p, spell));
			}
			case HEAVEN_SEVERING_SWORD -> sever(p, spell);
			case FOUR_SWORDS_ARRAY -> HeavenSpells.linger(new Array(p, spell));
			case WHITE_TIGER_POUNCE -> {
				LivingEntity aimed = HeavenSpells.aimed(p, TIGER_RANGE);
				HeavenSpells.linger(new Tiger(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, TIGER_RANGE)));
			}
			case MYRIAD_SWORDS_RETURN -> {
				LivingEntity aimed = HeavenSpells.aimed(p, SWORDS_RANGE);
				HeavenSpells.linger(new Swords(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, SWORDS_RANGE)));
			}
			case VOID_SEVERING_SWORD_LIGHT -> HeavenSpells.linger(new Severing(p, spell));
			case TEN_DIRECTION_SWORD_PRISON -> {
				LivingEntity aimed = HeavenSpells.aimed(p, PRISON_RANGE);
				HeavenSpells.linger(new Prison(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, PRISON_RANGE)));
			}
			case IMMORTAL_BINDING_ROPES -> {
				LivingEntity aimed = HeavenSpells.aimed(p, ROPES_RANGE);
				HeavenSpells.linger(new Ropes(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, ROPES_RANGE)));
			}
			case GOLDEN_DRAGON_SCISSORS -> {
				LivingEntity aimed = HeavenSpells.aimed(p, SCISSORS_RANGE);
				HeavenSpells.linger(new Scissors(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, SCISSORS_RANGE)));
			}
			case SWORD_TOMB_AWAKENS -> {
				LivingEntity aimed = HeavenSpells.aimed(p, TOMB_RANGE);
				HeavenSpells.linger(new Tombs(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, TOMB_RANGE)));
			}
			case NINE_REBIRTHS_OF_THE_SWORD -> {
				HeavenSpells.endLingering(p.getUUID(), Rebirths.class);
				LivingEntity aimed = HeavenSpells.aimed(p, REBIRTH_RANGE);
				HeavenSpells.linger(new Rebirths(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, REBIRTH_RANGE)));
			}
			case HEAVEN_SLAYING_SWORD_RISES -> {
				LivingEntity aimed = HeavenSpells.aimed(p, SLAYER_RANGE);
				HeavenSpells.linger(new Slayer(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, SLAYER_RANGE)));
			}
			case PRIMAL_AXE_SPLITS_CHAOS -> {
				HeavenSpells.endLingering(p.getUUID(), Axe.class);
				HeavenSpells.linger(new Axe(p, spell));
			}
			default -> {}
		}
	}

	// --- Qi Refining ---

	/** A sword of metal formed at {@code at} (beside the caster) over {@code form} ticks and shot: {@code damage}, through {@code pierce} more. */
	private static SpellProjectile sword(LivingEntity p, Spell spell, Vec3 at, int form, float damage, int pierce) {
		return SpellProjectile.cast(p, spell, SpellProjectile.Shape.FLYING_SWORD, 1.0f, form, at, SWORD_SPEED, SWORD_RANGE, new SpellProjectile.Behaviour() {
			@Override
			public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
				HeavenSpells.strike(p, target, damage, spell, missile);
				target.invulnerableTime = 0;
				SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.METAL_SPARKS, missile.position(), missile.getDeltaMovement(), 0.7f, 0);
				HeavenSpells.sound(HeavenSpells.level(p), target.position(), SoundEvents.TRIDENT_HIT, 1.0f, 1.4f);
				return true;
			}

			@Override
			public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
				SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.METAL_SPARKS, hit.getLocation(), missile.getDeltaMovement(), 0.8f, 0);
				HeavenSpells.sound(HeavenSpells.level(p), hit.getLocation(), SoundEvents.TRIDENT_HIT_GROUND, 1.0f, 1.3f);
			}

			@Override
			public void launched(SpellProjectile missile) {
				HeavenSpells.sound(HeavenSpells.level(p), missile.position(), SoundEvents.TRIDENT_THROW, 0.9f, 1.5f);
			}
		}).pierce(pierce);
	}

	private static void hurricane(LivingEntity p, Spell spell) {
		Vec3 look = p.getViewVector(1.0f);
		if (p instanceof net.minecraft.world.entity.player.Player player) player.startAutoSpinAttack(HURRICANE_TICKS); // the riptide's spin, a player's
		HeavenSpells.move(p, look.scale(HURRICANE_SPEED).add(0, p.onGround() ? 0.25 : 0, 0));
		Spells.safeFall(p, HURRICANE_TICKS + 40);
		SpellAuras.put(p, SpellAura.STEEL_HURRICANE, HURRICANE_TICKS);
		HeavenSpells.endLingering(p.getUUID(), Hurricane.class);
		HeavenSpells.linger(new Hurricane(p, spell));
		HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.TRIDENT_RIPTIDE_3, 1.2f, 1.2f);
		HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);
	}

	/** Steel Hurricane's edge: each being its spinning blade meets takes {@link #HURRICANE_DAMAGE} of metal on top of the weapon's blow. */
	public static final class Hurricane extends HeavenSpells.Timed implements HeavenSpells.MeleeListener {
		private final Spell spell;
		private final Set<LivingEntity> cut = new HashSet<>();

		Hurricane(LivingEntity caster, Spell spell) {
			super(caster, HURRICANE_TICKS + 4);
			this.spell = spell;
		}

		@Override
		protected boolean run(long age, long now) {
			if (age % 4 == 0) SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(caster), caster.getDeltaMovement(), 0.4f, 0);
			return casterHere();
		}

		@Override
		public void onMelee(ServerPlayer attacker, LivingEntity target) {
			if (attacker != caster || !cut.add(target)) return;
			HeavenSpells.strikeAgain(caster, target, HURRICANE_DAMAGE, spell);
			SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(target), caster.getDeltaMovement(), 0.8f, 0);
		}
	}

	// --- Foundation Building ---

	/**
	 * A crescent of sword qi flung from the caster (Boulder Cleave, Mountain Cleave): it flies {@code speed} a tick for {@code range},
	 * through the ground as through air, cutting a slot {@code width} wide and {@code height} tall wherever it passes (what it cuts is
	 * gone: nothing drops), and cleaving every being in its way for {@code damage}.
	 */
	private static void cleave(LivingEntity p, Spell spell, SpellProjectile.Shape shape, double speed, double range, float damage, int width, int height, float scale) {
		ServerLevel level = HeavenSpells.level(p);
		Vec3 look = p.getViewVector(1.0f);
		Vec3 flat = look.multiply(1, 0, 1).lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0, p.getYRot()) : look.multiply(1, 0, 1).normalize();
		Vec3 side = new Vec3(-flat.z, 0, flat.x);
		Set<BlockPos> cut = new HashSet<>();
		SpellProjectile.cast(p, spell, shape, scale, 0, new Vec3(0, -0.3, 1.0), speed, range, new SpellProjectile.Behaviour() {
			@Override
			public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
				HeavenSpells.strike(p, target, damage, spell, missile);
				SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(target), missile.getDeltaMovement(), 1.2f, 0);
				return true;
			}

			@Override
			public void hitBlock(SpellProjectile missile, BlockHitResult hit) {}

			@Override
			public void flying(SpellProjectile missile) {
				// The slot it cuts: its width across its way, its height from a little under it up. Sampled every half block, along
				// and across: a block's step on a slant (its way not along the grid) stepped over whole columns, left standing in the slot.
				Vec3 at = missile.position();
				Vec3 step = missile.getDeltaMovement();
				int steps = (int) Math.ceil(step.length() * 2);
				for (int s = 0; s <= steps; s++) {
					Vec3 point = at.add(step.scale(s / (double) Math.max(1, steps)));
					for (double w = -(width / 2); w <= width / 2; w += 0.5) {
						for (int h = 0; h < height; h++) {
							BlockPos pos = BlockPos.containing(point.add(side.scale(w)).add(0, h - (height > 4 ? height / 3 : 0), 0));
							if (!cut.add(pos)) continue;
							BlockState state = level.getBlockState(pos);
							if (state.isAir() || !HeavenSpells.carvable(level, pos, p)) continue;
							level.removeBlock(pos, false);
							if (level.random.nextInt(height > 4 ? 12 : 4) == 0) {
								SpellBlock.show(level, state, Vec3.atBottomCenterOf(pos), Vec3.ZERO, 0, 10, SpellBlock.Motion.CRUMBLE, 0);
							}
						}
					}
				}
				if (missile.tickCount % 3 == 0) HeavenSpells.sound(level, at, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.4f);
			}
		}).phasing().pierce(1000).hitSize(scale * 1.6f);
		HeavenSpells.sound(level, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, scale > 2 ? 0.4f : 0.8f);
		HeavenSpells.sound(level, p.position(), SoundEvents.TRIDENT_RIPTIDE_2, 1.0f, scale > 2 ? 0.5f : 1.4f);
	}

	/** Every ore block (any block named for one, of any mod, and ancient debris), found once. */
	private static java.util.Set<Block> oreBlocks;

	/** Whether {@code state} is an ore (any block named for one, or ancient debris). */
	static boolean ore(BlockState state) {
		if (oreBlocks == null) {
			java.util.Set<Block> found = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
			for (Block block : BuiltInRegistries.BLOCK) if (BuiltInRegistries.BLOCK.getKey(block).getPath().endsWith("_ore")) found.add(block);
			found.add(Blocks.ANCIENT_DEBRIS);
			oreBlocks = found;
		}
		return oreBlocks.contains(state.getBlock());
	}

	/** The plain rock an ore lies in: deepslate's, the Nether's, the End's or stone. */
	static BlockState rockOf(BlockState ore) {
		String path = BuiltInRegistries.BLOCK.getKey(ore.getBlock()).getPath();
		if (path.startsWith("deepslate_")) return Blocks.DEEPSLATE.defaultBlockState();
		if (path.startsWith("nether_") || ore.is(Blocks.ANCIENT_DEBRIS)) return Blocks.NETHERRACK.defaultBlockState();
		if (ore.is(Blocks.GILDED_BLACKSTONE)) return Blocks.BLACKSTONE.defaultBlockState();
		return Blocks.STONE.defaultBlockState();
	}

	/** The colour an ore's streak of light takes, by its name. */
	static int oreColour(BlockState ore) {
		String path = BuiltInRegistries.BLOCK.getKey(ore.getBlock()).getPath();
		if (path.contains("diamond")) return 0x6FF2E6;
		if (path.contains("emerald")) return 0x3FE07A;
		if (path.contains("gold")) return 0xFFD23A;
		if (path.contains("redstone")) return 0xFF3020;
		if (path.contains("lapis")) return 0x3A5CFF;
		if (path.contains("copper")) return 0xE07A45;
		if (path.contains("iron")) return 0xE8C8B0;
		if (path.contains("coal")) return 0x707070;
		if (path.contains("quartz")) return 0xF4EEE6;
		if (path.contains("jade")) return 0x5CE0A8;
		if (ore.is(Blocks.ANCIENT_DEBRIS)) return 0x9A6A4E;
		return 0xE0E0F0;
	}

	/**
	 * Mineral Purification: every ore (as many as {@link #ORES_MAX}, nearest first) within the reach of the caster's consciousness
	 * (ConsciousnessDomainHandler#radius) gives up its ore, which streams to the caster as light and is theirs (as mined, unsmelted),
	 * the rock it lay in left behind.
	 */
	private static void purify(LivingEntity caster) {
		if (!(caster instanceof ServerPlayer p)) return; // ore into a player's hands: a player's spell
		ServerLevel level = HeavenSpells.level(p);
		double radius = ConsciousnessDomainHandler.radius(CultivationManager.get(p));
		BlockPos centre = p.blockPosition();
		List<BlockPos> ores = oresWithin(level, p, centre, radius);
		ores.sort(java.util.Comparator.comparingDouble(pos -> pos.distSqr(centre)));
		if (ores.size() > ORES_MAX) ores = ores.subList(0, ORES_MAX);
		if (ores.isEmpty()) {
			// Nothing to take hold of: nothing paid (its qi given back, its cooldown lifted), as every spell that finds nothing pays nothing.
			if (!HeavenSpells.creative(p)) {
				PlayerCultivation c = CultivationManager.get(p);
				c.setQi(Math.min(c.maxQi(), c.getQi() + Spell.MINERAL_PURIFICATION.qiCost()));
				CultivationManager.sync(p);
			}
			Spells.setCooldown(p, Spell.MINERAL_PURIFICATION, 0);
			HeavenSpells.tell(p, Component.translatable(ModLang.MSG_NO_ORES));
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, p.position(), Vec3.ZERO, (float) Math.min(radius, 24), 0);
			return;
		}
		int count = 0;
		Vec3 to = HeavenSpells.middle(p);
		for (BlockPos pos : ores) {
			BlockState state = level.getBlockState(pos);
			List<ItemStack> drops = Block.getDrops(state, level, pos, null, p, ItemStack.EMPTY);
			level.setBlock(pos, rockOf(state), 3);
			SectManager.spoiled(level, p, pos); // a sect's ores taken from its grounds: trespass, as a pick's would be
			for (ItemStack drop : drops) p.getInventory().placeItemBackInInventory(drop); // what doesn't fit at their feet
			if (count++ < 64 || count % 4 == 0) SpellFx.send(level, SpellFx.Kind.STREAK, Vec3.atCenterOf(pos), to, 1, oreColour(state));
		}
		p.containerMenu.broadcastChanges();
		HeavenSpells.tell(p, Component.translatable(ModLang.MSG_ORES_FOUND, ores.size()));
		HeavenSpells.sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 0.8f);
		HeavenSpells.sound(level, p.position(), SoundEvents.BEACON_POWER_SELECT, 1.0f, 1.5f);
		SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, p.position(), Vec3.ZERO, (float) Math.min(radius, 32), 0);
	}

	/**
	 * Every ore within {@code radius} of {@code centre}, in the loaded world, outside shelters and ground {@code p} may not dig. A
	 * consciousness reaches far (160 blocks at Four Axis), so the world is read a chunk section at a time, and a section whose
	 * palette holds no ore (or only air) is passed over whole.
	 */
	static List<BlockPos> oresWithin(ServerLevel level, ServerPlayer p, BlockPos centre, double radius) {
		List<BlockPos> ores = new ArrayList<>();
		int r = (int) Math.ceil(radius);
		double r2 = radius * radius;
		for (int cx = (centre.getX() - r) >> 4; cx <= (centre.getX() + r) >> 4; cx++) {
			for (int cz = (centre.getZ() - r) >> 4; cz <= (centre.getZ() + r) >> 4; cz++) {
				net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) continue;
				net.minecraft.world.level.chunk.LevelChunkSection[] sections = chunk.getSections();
				for (int i = 0; i < sections.length; i++) {
					net.minecraft.world.level.chunk.LevelChunkSection section = sections[i];
					if (section == null || section.hasOnlyAir() || !section.getStates().maybeHas(MetalSpells::ore)) continue;
					int baseY = level.getSectionYFromSectionIndex(i) << 4;
					if (baseY + 15 < centre.getY() - r || baseY > centre.getY() + r) continue;
					for (int x = 0; x < 16; x++) {
						for (int y = 0; y < 16; y++) {
							for (int z = 0; z < 16; z++) {
								BlockState state = section.getBlockState(x, y, z);
								if (!ore(state)) continue;
								BlockPos pos = new BlockPos((cx << 4) + x, baseY + y, (cz << 4) + z);
								if (pos.distSqr(centre) > r2 || Formations.shelters(level, pos) || Formations.protectsGroundQuietly(p, pos)) continue;
								// Never a spell's own block (an ore wall: its ore is no one's), nor ground the caster couldn't dig by
								// hand (Adventure mode, spawn protection), nor what is kept safe (the Ghost Gate, a sealed treasury).
								if (HeavenSpells.isTemporary(level, pos) || !p.mayBuild() || !level.mayInteract(p, pos) || HeavenSpells.guarded(level, pos, p)) continue;
								ores.add(pos);
							}
						}
					}
				}
			}
		}
		return ores;
	}

	/**
	 * Advanced Flying Sword Technique: {@link #SWORDS} swords form in the air round the caster one by one, then fly one by one at
	 * where they look (each its {@link #ADVANCED_DAMAGE}, through one being).
	 */
	private static void advanced(LivingEntity p, Spell spell) {
		for (int i = 0; i < SWORDS; i++) {
			int index = i;
			double a = Math.PI * (0.15 + 0.7 * i / (SWORDS - 1));
			Vec3 at = new Vec3(Math.cos(a) * 1.6, 0.4 + Math.sin(a) * 0.9, -0.2);
			int formed = i * SWORD_FORM_EVERY;
			int loosed = SWORDS * SWORD_FORM_EVERY + 4 + i * SWORD_LOOSE_EVERY;
			Runnable make = () -> {
				SpellProjectile sword = sword(p, spell, at, loosed - formed, ADVANCED_DAMAGE, 1);
				sword.data(index);
				HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.ANVIL_PLACE, 0.3f, 1.8f);
			};
			if (formed <= 0) make.run();
			else HeavenSpells.linger(new FireSpells.Delayed(p, formed, make));
		}
	}

	// --- Core Formation ---

	private static void rain(LivingEntity p, Spell spell) {
		ServerLevel level = HeavenSpells.level(p);
		LivingEntity aimed = HeavenSpells.aimed(p, RAIN_RANGE);
		Vec3 point = aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, RAIN_RANGE);
		Vec3 ground = HeavenSpells.groundBelow(level, point.add(0, 1, 0), 3, 24);
		Vec3 target = ground != null ? ground : point;
		Vec3 sky = target.add(0, 9, 0);
		// The ring of swords (SpellConstruct SWORD_RING, unturned: its swords hang where their blades are loosed from) forming over the mark.
		SpellConstruct ring = SpellConstruct.create(level, SpellConstruct.Kind.SWORD_RING, sky, 0, 0, RAIN_FORM + 12, p).sizedFor(spell).spawn();
		HeavenSpells.sound(level, sky, SoundEvents.ANVIL_PLACE, 1.0f, 1.6f);
		HeavenSpells.linger(new FireSpells.Delayed(p, RAIN_FORM, () -> {
			ring.dissolve();
			for (int i = 0; i < RAIN_SWORDS; i++) {
				double a = Math.PI * 2 * i / RAIN_SWORDS;
				Vec3 from = sky.add(Math.cos(a) * RAIN_RADIUS * 0.8, 0, Math.sin(a) * RAIN_RADIUS * 0.8);
				Vec3 down = target.add(Math.cos(a) * RAIN_RADIUS * 0.5 * level.random.nextDouble(), 0, Math.sin(a) * RAIN_RADIUS * 0.5 * level.random.nextDouble());
				SpellProjectile sword = SpellProjectile.thrown(level, p, spell, SpellProjectile.Shape.PLUNGING_SWORD, 1.0f, from, down.subtract(from).normalize().scale(1.8), 16,
						new SpellProjectile.Behaviour() {
							@Override
							public boolean hitEntity(SpellProjectile missile, LivingEntity hit) {
								HeavenSpells.strike(p, hit, RAIN_DAMAGE, spell, missile);
								hit.invulnerableTime = 0;
								SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, missile.position(), missile.getDeltaMovement(), 0.6f, 0);
								return false;
							}

							@Override
							public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
								SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, hit.getLocation(), missile.getDeltaMovement(), 0.6f, 0);
								HeavenSpells.sound(level, hit.getLocation(), SoundEvents.TRIDENT_HIT_GROUND, 0.6f, 1.5f);
							}
						}).data(i);
				sword.body(SpellConstruct.create(level, SpellConstruct.Kind.RAIN_SWORD, from, -sword.getYRot(), -sword.getXRot(), 20, p).sizedFor(spell).spawn());
			}
			HeavenSpells.sound(level, sky, SoundEvents.TRIDENT_RIPTIDE_3, 1.5f, 1.5f);
		}));
	}

	// --- Nascent Soul ---

	/**
	 * Piercing Sword Light: a line of sword light along the look for {@link #LIGHT_RANGE}, through whatever stands in its way: it
	 * bores a tunnel {@link #LIGHT_BORE} round through hill and wall (Devastation#bore) and cuts every being along it.
	 */
	private static void light(LivingEntity p, Spell spell) {
		ServerLevel level = HeavenSpells.level(p);
		Vec3 from = HeavenSpells.fromHands(p);
		Vec3 look = p.getViewVector(1.0f);
		Vec3 to = p.getEyePosition().add(look.scale(LIGHT_RANGE));
		SpellField.create(level, SpellField.Kind.SWORD_LIGHT, from, 0.6f, (float) from.distanceTo(to), 14, p).facing(p.getYRot(), p.getXRot()).fleeting().spawn();
		Vec3 eye = p.getEyePosition();
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(eye, to).inflate(2), e -> !HeavenSpells.spared(p, e))) {
			Vec3 mid = HeavenSpells.middle(e).subtract(eye);
			double along = mid.dot(look);
			if (along < 0 || along > LIGHT_RANGE || mid.subtract(look.scale(along)).length() > 1.2 + e.getBbWidth() / 2) continue;
			HeavenSpells.strike(p, e, LIGHT_DAMAGE, spell);
			e.invulnerableTime = 0;
			SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(e), look, 1.2f, 0);
		}
		Devastation.bore(p, level, eye.add(look.scale(1.5)), to, LIGHT_BORE);
		SpellFx.send(level, SpellFx.Kind.FLASH, to, Vec3.ZERO, 2.0f, 0xFFF2C0);
		HeavenSpells.sound(level, p.position(), SoundEvents.TRIDENT_THUNDER, 1.0f, 1.8f);
		HeavenSpells.sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 1.0f, 2.0f);
	}

	// --- Heavenly Being ---

	/**
	 * Sword Domain: swords circle the caster {@link #DOMAIN_RADIUS} round for {@link #DOMAIN_TICKS}; each foe inside is cut every half second. Its
	 * swords (SpellConstruct SWORD_DOMAIN) wheel in two rings over its formation, darting in at every other cut.
	 */
	public static final class Domain extends HeavenSpells.Timed {
		private final Spell spell;
		private final SpellConstruct domain;

		Domain(LivingEntity caster, Spell spell) {
			super(caster, DOMAIN_TICKS);
			this.spell = spell;
			this.domain = SpellConstruct.create(level, SpellConstruct.Kind.SWORD_DOMAIN, caster.position(), 0, 0, DOMAIN_TICKS + 10, caster).sizedFor(spell)
					.anchor(caster, SpellAnchor.POSITION, Vec3.ZERO).spawn();
			sound(caster.position(), SoundEvents.BEACON_ACTIVATE, 1.5f, 1.8f);
			sound(caster.position(), SoundEvents.ANVIL_USE, 1.0f, 1.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			domain.place(caster.position(), 0, 0);
			if (age % 10 == 5) {
				for (LivingEntity e : HeavenSpells.around(caster, DOMAIN_RADIUS, e -> HeavenSpells.mark(caster, e))) {
					HeavenSpells.strikeAgain(caster, e, DOMAIN_DAMAGE, spell);
					SpellFx.send(level, SpellFx.Kind.STREAK, HeavenSpells.middle(caster).add(0, 1, 0), HeavenSpells.middle(e), 1, 0xF4F0D8);
					SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(e), Vec3.ZERO, 0.6f, 0);
				}
				if (age % 20 == 5) {
					domain.act("strike");
					sound(caster.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.6f);
				}
			}
			return true;
		}

		@Override
		public void end() {
			domain.dissolve();
		}
	}

	// --- Four Axis ---

	/**
	 * Heaven-Severing Sword: a sword of light as long as a tower forms in the caster's hand and is swung level across their front
	 * ({@link SeverStroke}); as its blade crosses the front a crescent of sword light about 110 blocks across leaves it, flying out
	 * level from the caster's chest the way they face, {@link #SEVER_SPEED} a tick for {@link #SEVER_RANGE} blocks. Nothing stops it, and it cuts only what
	 * its blade passes through: a slit {@link #SEVER_THICK} blocks tall at its middle, thinning toward its tips, through hills,
	 * mountains, trees and whatever is built in its way (the tops stand on above the cut), the land's far end of it cut in its
	 * crescent's curve. Every being its blade passes through takes {@link #SEVER_DAMAGE} and is hurled ahead of it.
	 */
	private static void sever(LivingEntity p, Spell spell) {
		ServerLevel level = HeavenSpells.level(p);
		Vec3 look = p.getViewVector(1.0f);
		Vec3 flat = look.multiply(1, 0, 1).lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0, p.getYRot()) : look.multiply(1, 0, 1).normalize();
		Vec3 side = new Vec3(-flat.z, 0, flat.x);
		// It runs level, its middle at a caster's chest over the ground they stand over (in the air, the ground below them, if near).
		Vec3 ground = HeavenSpells.groundBelow(level, p.position().add(0, 1, 0), 2, 24);
		int base = (int) Math.floor((ground != null ? ground : p.position()).y);
		double bladeY = base + SEVER_LIFT;
		double half = SEVER_RADIUS * Math.sin(SEVER_SPAN / 2);
		Vec3 from = new Vec3(p.getX(), base + 1, p.getZ()).add(flat.scale(2.5));
		for (int d = 0; d <= SEVER_RANGE + 16; d += 32) loadSeverWay(level, from.add(flat.scale(d)));
		// The stroke: the sword forms in the hand and is swung; the slash leaves it as it crosses the front.
		float yaw = (float) Math.toDegrees(Math.atan2(-flat.x, flat.z));
		SpellConstruct sword = SpellConstruct.create(level, SpellConstruct.Kind.SEVERING_SWORD, p.position().add(0, p.getBbHeight() * 0.65, 0), yaw, 0, SEVER_STROKE + 4, p)
				.sizedFor(spell).spawn();
		HeavenSpells.sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 3.0f, 0.4f);
		HeavenSpells.sound(level, p.position(), SoundEvents.TRIDENT_RETURN, 3.0f, 0.5f);
		HeavenSpells.linger(new SeverStroke(p, sword, () -> release(p, spell, level, flat, side, from, base, bladeY, half)));
	}

	/**
	 * The Heaven-Severing Sword's stroke (SpellConstruct SEVERING_SWORD, its stroke timed to {@link #SEVER_FORM} and {@link #SEVER_SWING}:
	 * tools/spell-art, Axis#severingSword): the slash is released {@link #SEVER_RELEASE} ticks in, as the blade crosses the front, and the sword
	 * is gone at the end. Its caster gone before the release (left, or another level), nothing is released.
	 */
	static final class SeverStroke extends HeavenSpells.Timed {
		private final SpellConstruct sword;
		private final Runnable release;
		private boolean released;

		SeverStroke(LivingEntity caster, SpellConstruct sword, Runnable release) {
			super(caster, SEVER_STROKE);
			this.sword = sword;
			this.release = release;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!released && age >= SEVER_RELEASE) {
				released = true;
				if (casterHere()) release.run();
			}
			return true;
		}

		@Override
		public void end() {
			sword.dissolve();
		}
	}

	/** The severing slash let go: it flies out from {@code from} along {@code flat}, cutting its way (see {@link #sever}). */
	private static void release(LivingEntity p, Spell spell, ServerLevel level, Vec3 flat, Vec3 side, Vec3 from, int base, double bladeY, double half) {
		java.util.Set<Long> columns = new java.util.HashSet<>();
		java.util.Set<LivingEntity> struck = new java.util.HashSet<>();
		long[] ticketed = {Long.MIN_VALUE};
		SpellProjectile.thrown(level, p, spell, SpellProjectile.Shape.HEAVEN_SLASH, 1.0f, from, flat.scale(SEVER_SPEED), SEVER_RANGE, new SpellProjectile.Behaviour() {
			@Override
			public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
				return true; // its blade cuts what it passes (flying), and goes on
			}

			@Override
			public void hitBlock(SpellProjectile missile, BlockHitResult hit) {}

			@Override
			public void flying(SpellProjectile missile) {
				Vec3 at = missile.position();
				// The land ahead of it asked for again as it goes (the first asking may have lapsed if it waited at unmade land).
				ChunkPos ahead = new ChunkPos(BlockPos.containing(at.add(flat.scale(32))));
				if (ahead.toLong() != ticketed[0]) {
					ticketed[0] = ahead.toLong();
					loadSeverWay(level, at.add(flat.scale(32)));
				}
				// How far out its bow is now and was a tick ago, along its way from where it set out (its caster stands just behind).
				double bowNow = at.subtract(from).dot(flat), bowBefore = bowNow - SEVER_SPEED;
				// What its blade passed through this tick: at each step out to the side, the blade lags its bow by the crescent's curve,
				// and is as thick there as the crescent is. Sampled every half block, along and across: a block's step, on a slant (its
				// way not along the grid), stepped over whole columns and left them standing in the cut.
				List<int[]> cut = new ArrayList<>();
				for (double w = -Math.floor(half); w <= Math.floor(half); w += 0.5) {
					double lag = severLag(w), thick = severThickness(w);
					double fFrom = Math.max(0, bowBefore - lag), fTo = bowNow - lag;
					if (fTo < 0 || thick <= 0) continue;
					// The land cut half as tall as the blade (SEVER_CUT_SHARE: the user's ask, 2026-10-24), from the blade's lower edge up: the
					// ground under it still scoured, the slit in a hill half as high; what it strikes, its whole height.
					int yLo = (int) Math.floor(bladeY - thick / 2), yHi = (int) Math.floor(bladeY - thick / 2 + thick * SEVER_CUT_SHARE);
					for (double f = Math.ceil(fFrom * 2) / 2; f <= fTo; f += 0.5) {
						BlockPos column = BlockPos.containing(from.add(flat.scale(f)).add(side.scale(w)));
						if (columns.add(BlockPos.asLong(column.getX(), 0, column.getZ()))) cut.add(new int[]{column.getX(), column.getZ(), yLo, yHi});
					}
				}
				Devastation.shear(p, level, cut, at);
				// Every being its blade passes through this tick.
				AABB band = new AABB(at.subtract(flat.scale(SEVER_SPEED + SEVER_RADIUS)), at).inflate(half + 1, 0, half + 1)
						.setMinY(bladeY - SEVER_THICK / 2 - 2).setMaxY(bladeY + SEVER_THICK / 2 + 1);
				for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, band, e -> !HeavenSpells.spared(p, e))) {
					Vec3 off = e.position().subtract(from);
					double across = off.dot(side), thick = severThickness(across);
					double blade = bowNow - severLag(across);
					double feet = e.getY(), head = e.getY() + e.getBbHeight();
					if (thick <= 0 || off.dot(flat) > blade + 1 || off.dot(flat) < Math.max(-1, blade - SEVER_SPEED - 2)) continue;
					if (head < bladeY - thick / 2 || feet > bladeY + thick / 2 || !struck.add(e)) continue;
					HeavenSpells.strike(p, e, SEVER_DAMAGE, spell, missile);
					HeavenSpells.move(e, flat.scale(1.8).add(0, 0.7, 0));
					SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(e), flat, 2.0f, 0);
				}
				if (missile.tickCount % 2 == 0) {
					HeavenSpells.sound(level, at, SoundEvents.PLAYER_ATTACK_SWEEP, 3.0f, 0.4f);
					SpellFx.send(level, SpellFx.Kind.EARTH_BURST, new Vec3(at.x, base + 0.5, at.z), Vec3.ZERO, 3, Block.getId(Blocks.STONE.defaultBlockState()), 160);
				}
				if (missile.tickCount % 6 == 0) HeavenSpells.sound(level, at, SoundEvents.GENERIC_EXPLODE, 3.0f, 0.4f);
			}
		}).phasing().pierce(100000).hitSize(4.0f).data(Math.round((float) Math.toDegrees(Math.atan2(-flat.x, flat.z))));
		Vec3 at = from.subtract(flat.scale(2.5));
		SpellFx.send(level, SpellFx.Kind.FLASH, at.add(flat.scale(3)).add(0, 1, 0), Vec3.ZERO, 8, 0xFFF4D0, 256);
		SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, at, Vec3.ZERO, 14, 0, 256);
		HeavenSpells.sound(level, at, SoundEvents.TRIDENT_THUNDER, 4.0f, 0.5f);
		HeavenSpells.sound(level, at, SoundEvents.PLAYER_ATTACK_SWEEP, 4.0f, 0.3f);
	}

	/**
	 * Four Swords Array: four giant swords plant at the corners of a square {@link #ARRAY_HALF} each way of the point looked at;
	 * for {@link #ARRAY_TICKS} every foe inside is held in (thrown back from its edge) and cut ({@link #ARRAY_DAMAGE} a second).
	 */
	public static final class Array extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 at;
		private final SpellField field;

		Array(LivingEntity caster, Spell spell) {
			super(caster, ARRAY_TICKS);
			this.spell = spell;
			LivingEntity aimed = HeavenSpells.aimed(caster, ARRAY_RANGE);
			Vec3 point = aimed != null ? aimed.position() : HeavenSpells.lookPoint(caster, ARRAY_RANGE);
			Vec3 ground = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 32);
			this.at = ground != null ? ground : point;
			this.field = SpellField.create(level, SpellField.Kind.SWORD_ARRAY, at, (float) ARRAY_HALF, 24, ARRAY_TICKS, caster).spawn();
			sound(at, SoundEvents.ANVIL_LAND, 4.0f, 0.3f);
			sound(at, SoundEvents.BEACON_ACTIVATE, 3.0f, 0.5f);
		}

		private boolean inside(Vec3 pos, double margin) {
			return Math.abs(pos.x - at.x) <= ARRAY_HALF + margin && Math.abs(pos.z - at.z) <= ARRAY_HALF + margin && pos.y > at.y - 8 && pos.y < at.y + 32;
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < 10) return true; // the swords are still falling into place
			if (age == 10) {
				// Each sword driven deep into the ground where it lands, the earth thrown up round it.
				for (int c = 0; c < 4; c++) {
					Vec3 corner = at.add((c == 0 || c == 3 ? -1 : 1) * ARRAY_HALF, 0, (c < 2 ? -1 : 1) * ARRAY_HALF);
					Devastation.crater(caster, level, corner, ARRAY_CRATER, ARRAY_CRATER_DEPTH, Devastation.Floor.RUBBLE);
					SpellFx.send(level, SpellFx.Kind.EARTH_BURST, corner, Vec3.ZERO, 4, Block.getId(Blocks.STONE.defaultBlockState()), 192);
				}
				SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, at, Vec3.ZERO, (float) ARRAY_HALF * 2.5f, 0, 256);
				sound(at, SoundEvents.GENERIC_EXPLODE, 5.0f, 0.4f);
			}
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(ARRAY_HALF + 3, 32, ARRAY_HALF + 3),
					e -> HeavenSpells.mark(caster, e))) {
				if (!inside(e.position(), 2.5)) continue;
				// Held in: at its edge, thrown back toward its middle.
				if (!inside(e.position(), -1.0)) {
					Vec3 in = at.subtract(e.position()).multiply(1, 0, 1).normalize();
					HeavenSpells.move(e, in.scale(0.8).add(0, 0.2, 0));
				}
				if (age % 10 == 0) HeavenSpells.strikeAgain(caster, e, ARRAY_DAMAGE / 2, spell);
			}
			if (age % 20 == 0) sound(at, SoundEvents.BEACON_AMBIENT, 3.0f, 1.6f);
			return true;
		}

		@Override
		public void end() {
			field.discard();
		}
	}

	// --- Dao Integration ---

	/**
	 * The columns of a gouge (or a slot) {@code wide} across, along {@code way} from {@code from} out {@code d0} to {@code d1} blocks,
	 * {@code across} blocks off to the side of it; each a span from {@code depth} under the land's top there to two above it (Devastation#shear).
	 */
	private static void gouge(ServerLevel level, Vec3 from, Vec3 way, double across, double d0, double d1, double wide, double depth, Set<Long> seen, List<int[]> spans) {
		Vec3 side = new Vec3(-way.z, 0, way.x);
		for (double d = d0; d <= d1; d += 0.5) {
			for (double w = -wide / 2; w <= wide / 2 + 1.0e-6; w += 0.7) {
				int x = Mth.floor(from.x + way.x * d + side.x * (across + w)), z = Mth.floor(from.z + way.z * d + side.z * (across + w));
				if (!seen.add(BlockPos.asLong(x, 0, z))) continue;
				int top = level.hasChunk(x >> 4, z >> 4) ? level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1
						: Mth.floor(from.y) - 1;
				spans.add(new int[] {x, z, top - (int) Math.round(depth) + 1, top + 2});
			}
		}
	}

	/**
	 * White Tiger's Pounce: the White Tiger of the west, made of steel and sword light (SpellConstruct WHITE_TIGER, its own GeckoLib
	 * model), crouches by its caster and leaps onto where they look: {@link #TIGER_DAMAGE} to everything within {@link #TIGER_RADIUS} where
	 * it lands. Then its claws rake out ahead of it: four gouges {@link #TIGER_GOUGE} long are cut through the land, running out over
	 * {@link #TIGER_RAKE} ticks, and everything in them takes {@link #TIGER_RAKE_DAMAGE}. It stays a little, then fades.
	 */
	public static final class Tiger extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 from, land, way;
		private final SpellConstruct tiger;
		private final Set<LivingEntity> raked = new HashSet<>();
		private final Set<Long> cut = new HashSet<>();
		private final int landAt = TIGER_CROUCH + TIGER_LEAP, rakeAt = TIGER_CROUCH + TIGER_LEAP + TIGER_CLAW + 4;
		/** How far out the gouges have run. */
		private double raking;

		Tiger(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, TIGER_CROUCH + TIGER_LEAP + TIGER_CLAW + 4 + TIGER_RAKE + TIGER_STAY);
			this.spell = spell;
			this.from = caster.position();
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 32);
			this.land = below != null ? below : point;
			Vec3 flat = land.subtract(from).multiply(1, 0, 1);
			this.way = flat.lengthSqr() > 4 ? flat.normalize() : Vec3.directionFromRotation(0, caster.getYRot());
			this.tiger = SpellConstruct.create(level, SpellConstruct.Kind.WHITE_TIGER, from, HeavenSpells.facing(way)[0], 0, (int) (end - start), caster).sizedFor(spell).spawn();
			tiger.act("pounce");
			sound(from, SoundEvents.RAVAGER_ROAR, 3.0f, 1.2f);
			sound(from, SoundEvents.TRIDENT_RIPTIDE_3, 2.0f, 0.8f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age <= landAt) {
				// The leap: an arc from its caster to where it lands, higher the further it goes.
				double f = Math.max(0, (age - TIGER_CROUCH) / (double) TIGER_LEAP), arc = 6 + from.distanceTo(land) * 0.15;
				Vec3 here = from.lerp(land, f).add(0, Math.sin(Math.PI * f) * arc, 0), next = from.lerp(land, Math.min(1, f + 0.05)).add(0, Math.sin(Math.PI * Math.min(1, f + 0.05)) * arc, 0);
				float[] face = HeavenSpells.facing(age < TIGER_CROUCH ? way : next.subtract(here));
				tiger.place(here, face[0], face[1] * 0.5f);
				if (age == landAt) {
					for (LivingEntity e : HeavenSpells.aroundPoint(level, land, TIGER_RADIUS, e -> !HeavenSpells.spared(caster, e))) {
						HeavenSpells.strike(caster, e, TIGER_DAMAGE, spell);
						HeavenSpells.push(land, e, 1.2);
					}
					SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, land, Vec3.ZERO, (float) TIGER_RADIUS * 1.5f, 0, 192);
					SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, land.add(0, 1, 0), Vec3.ZERO, (float) TIGER_RADIUS, 0, 192);
					sound(land, SoundEvents.ANVIL_LAND, 3.0f, 0.6f);
					sound(land, SoundEvents.RAVAGER_ROAR, 3.0f, 0.9f);
				}
				return true;
			}
			if (age == landAt + TIGER_CLAW) {
				tiger.act("claw");
				sound(land, SoundEvents.PLAYER_ATTACK_SWEEP, 3.0f, 0.5f);
			}
			if (age >= rakeAt && raking < TIGER_GOUGE) {
				// The rake: four gouges running out ahead of it, cut through whatever stands in them, striking what they pass.
				double d0 = raking, d1 = Math.min(TIGER_GOUGE, raking + TIGER_GOUGE / TIGER_RAKE);
				List<int[]> spans = new ArrayList<>();
				Vec3 side = new Vec3(-way.z, 0, way.x);
				for (int k = 0; k < 4; k++) {
					double across = (k - 1.5) * TIGER_SPREAD;
					gouge(level, land, way, across, d0 + 2, d1 + 2, 1.4, TIGER_GOUGE_DEPTH, cut, spans);
					Vec3 a = land.add(way.scale(d0 + 2)).add(side.scale(across)).add(0, 0.6, 0);
					SpellFx.send(level, SpellFx.Kind.STREAK, a, way.scale(d1 - d0), 1, 0xE6F2FF, 160);
				}
				Devastation.shear(caster, level, spans, land);
				Vec3 mid = land.add(way.scale((d0 + d1) / 2 + 2));
				for (LivingEntity e : HeavenSpells.aroundPoint(level, mid, (d1 - d0) / 2 + 3 * TIGER_SPREAD, e -> !raked.contains(e) && !HeavenSpells.spared(caster, e))) {
					Vec3 rel = e.position().subtract(land);
					double along = rel.x * way.x + rel.z * way.z - 2, off = rel.x * side.x + rel.z * side.z;
					if (along < d0 - 1 || along > d1 + 1 || e.getY() > land.y + 8 || e.getY() < land.y - 6) continue;
					double nearest = 1e9;
					for (int k = 0; k < 4; k++) nearest = Math.min(nearest, Math.abs(off - (k - 1.5) * TIGER_SPREAD));
					if (nearest > TIGER_RAKE_REACH) continue;
					raked.add(e);
					HeavenSpells.strike(caster, e, TIGER_RAKE_DAMAGE, spell);
					SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(e), Vec3.ZERO, 1.2f, 0);
				}
				raking = d1;
				if (age % 3 == 0) sound(mid, SoundEvents.GRINDSTONE_USE, 3.0f, 0.5f);
			}
			return true;
		}

		@Override
		public void end() {
			tiger.dissolve();
		}
	}

	/**
	 * Myriad Swords Return to One: ten thousand swords gather in a turning vortex high over where its caster looks (SpellConstruct
	 * MYRIAD_SWORDS, {@link #SWORDS_GATHER} ticks), close in and fuse ({@link #SWORDS_MERGE}) into one colossal sword (COLOSSAL_SWORD) that
	 * falls ({@link #SWORDS_FALL}) and is driven into the land: {@link #SWORDS_DAMAGE} to everything within {@link #SWORDS_RADIUS}, a cross
	 * of slots cut out from it each way and a pit where it went in. It stands there a while, then fades.
	 */
	public static final class Swords extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final float yaw;
		private final SpellConstruct vortex;
		private SpellConstruct sword;
		private final int fuseAt = SWORDS_GATHER + SWORDS_MERGE, strikeAt = SWORDS_GATHER + SWORDS_MERGE + SWORDS_FALL;

		Swords(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, SWORDS_GATHER + SWORDS_MERGE + SWORDS_FALL + SWORDS_STAND);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 32);
			this.ground = below != null ? below : point;
			this.yaw = caster.getYRot();
			// Bright till they fuse, then fading as the sword they become fades in.
			this.vortex = SpellConstruct.create(level, SpellConstruct.Kind.MYRIAD_SWORDS, ground.add(0, SWORDS_HIGH - SWORDS_LOW, 0), yaw, 0, fuseAt + SpellConstruct.FADE, caster).sizedFor(spell).spawn();
			sound(ground.add(0, SWORDS_HIGH, 0), SoundEvents.TRIDENT_RETURN, 3.0f, 0.5f);
			sound(caster.position(), SoundEvents.BEACON_ACTIVATE, 2.0f, 1.6f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age == SWORDS_GATHER) {
				vortex.act("merge");
				sound(ground.add(0, SWORDS_HIGH, 0), SoundEvents.BEACON_POWER_SELECT, 3.0f, 1.4f);
			}
			if (age < fuseAt) {
				if (age % 8 == 0) sound(ground.add(0, SWORDS_HIGH, 0), SoundEvents.TRIDENT_RIPTIDE_1, 2.5f, 1.2f + age * 0.01f);
				return true;
			}
			if (age == fuseAt) {
				vortex.dissolve();
				sword = SpellConstruct.create(level, SpellConstruct.Kind.COLOSSAL_SWORD, ground.add(0, SWORDS_HIGH + 8, 0), yaw, 0, SWORDS_FALL + SWORDS_STAND, caster).sizedFor(spell).spawn();
				SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, SWORDS_HIGH + 20, 0), Vec3.ZERO, 12, 0xE6F2FF, 192);
			}
			if (age <= strikeAt) {
				double f = (age - fuseAt) / (double) SWORDS_FALL;
				sword.place(ground.add(0, SWORDS_HIGH + 8 - (SWORDS_HIGH + 8 + SWORDS_PIT_DEPTH) * f * f, 0), yaw, 0);
			}
			if (age == strikeAt) {
				for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, SWORDS_RADIUS + 12, e -> !HeavenSpells.spared(caster, e))) {
					if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > SWORDS_RADIUS || e.getY() > ground.y + 16 || e.getY() < ground.y - SWORDS_PIT_DEPTH - 2) continue;
					HeavenSpells.strike(caster, e, SWORDS_DAMAGE, spell);
					HeavenSpells.push(ground, e, 1.6);
				}
				// Its pit where it went in, and a cross of slots split out from it each way.
				Devastation.bore(caster, level, ground.add(0, 1, 0), ground.add(0, -SWORDS_PIT_DEPTH, 0), SWORDS_PIT);
				List<int[]> spans = new ArrayList<>();
				Set<Long> seen = new HashSet<>();
				Vec3 look = Vec3.directionFromRotation(0, yaw);
				for (Vec3 way : new Vec3[] {look, look.scale(-1), new Vec3(-look.z, 0, look.x), new Vec3(look.z, 0, -look.x)}) {
					gouge(level, ground, way, 0, SWORDS_PIT, SWORDS_SLOT, 1.4, SWORDS_SLOT_DEPTH, seen, spans);
				}
				Devastation.shear(caster, level, spans, ground);
				SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) SWORDS_SLOT * 0.6f, 0, 192);
				SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, ground.add(0, 2, 0), Vec3.ZERO, (float) SWORDS_RADIUS, 0, 192);
				sound(ground, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.5f);
				sound(ground, SoundEvents.ANVIL_LAND, 4.0f, 0.4f);
				sound(ground, SoundEvents.BELL_BLOCK, 4.0f, 0.5f);
			}
			return true;
		}

		@Override
		public void end() {
			vortex.dissolve();
			if (sword != null) sword.dissolve();
		}
	}

	// --- Void Shattering ---

	/**
	 * Void-Severing Sword Light: a sword of light (SpellConstruct SEVERING_LIGHT, its own GeckoLib model) thrusts out of its caster's hands
	 * along their look, {@link #SEVERING_LENGTH} in {@link #SEVERING_THRUST} ticks, through whatever stands in its way, and cleaves the land
	 * under it into a chasm in the shape of the rift it leaves (the user's ask, 2026-10-24: "way deeper and wider and not a perfect
	 * rectangle"): pointed at both ends, {@link #SEVERING_CLEFT} across each way at its middle and {@link #SEVERING_CLEFT_DEPTH} deep there below
	 * its line, deepest along its middle, its sides and floor ragged, and through whatever stands on its line a slot cut too (a player's:
	 * Devastation#cut). Every being on its line or down in its chasm takes {@link #SEVERING_DAMAGE} (once) and is hurled on along it. It cuts
	 * space itself: behind it the line stays torn open (RIFT_LINE) for {@link #SEVERING_RIFT}, and every second whoever is in its cut, on the line
	 * or down in the chasm, takes {@link #SEVERING_RIFT_DAMAGE}.
	 */
	public static final class Severing extends HeavenSpells.Timed {
		/** How far along its line from its caster's hands its chasm begins (their own footing kept). */
		private static final double CLEFT_FROM = 2.5;
		private final Spell spell;
		private final Vec3 from, way, side;
		private final float[] face;
		private final SpellConstruct light, rift;
		private final Set<LivingEntity> struck = new HashSet<>();
		/** Its chasm's cells already given to be cut. */
		private final Set<Long> cleaved = new HashSet<>();
		/** Its own shape's seed (its chasm's wandering and ragged edges). */
		private final long seed;
		/** How far along its line it has cut. */
		private double cut;

		Severing(LivingEntity caster, Spell spell) {
			super(caster, SEVERING_THRUST + SEVERING_RIFT);
			this.spell = spell;
			this.from = HeavenSpells.fromHands(caster);
			this.way = caster.getViewVector(1.0f).normalize();
			Vec3 flat = new Vec3(way.x, 0, way.z);
			this.side = flat.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : new Vec3(-flat.z, 0, flat.x).normalize();
			this.seed = BlockPos.containing(from).asLong() * 31 + start;
			this.face = HeavenSpells.facing(way);
			this.light = SpellConstruct.create(level, SpellConstruct.Kind.SEVERING_LIGHT, from, face[0], face[1], SEVERING_THRUST + SpellConstruct.FADE, caster).sizedFor(spell).spawn();
			this.rift = SpellConstruct.create(level, SpellConstruct.Kind.RIFT_LINE, from, face[0], face[1], (int) (end - start), caster).size((float) SEVERING_LENGTH)
					.tint(SWORD_WHITE).spawn();
			sound(from, SoundEvents.TRIDENT_THUNDER, 2.0f, 1.4f);
			sound(from, SoundEvents.BEACON_POWER_SELECT, 2.0f, 1.8f);
			sound(from, SoundEvents.END_PORTAL_SPAWN, 2.0f, 1.6f);
		}

		/** Its chasm's half-width {@code s} blocks along its line: nothing at its ends, widest about its middle, wandering as it goes. */
		double cleftWidth(double s) {
			double t = s / SEVERING_LENGTH;
			if (t <= 0 || t >= 1) return 0;
			return SEVERING_CLEFT * Math.pow(1 - (2 * t - 1) * (2 * t - 1), 0.5) * (0.78 + 0.4 * smooth(s / 9.0, 1));
		}

		/** Its chasm's depth below its line {@code s} blocks along it (along its middle; shallower toward its sides). */
		double cleftDepth(double s) {
			double t = s / SEVERING_LENGTH;
			if (t <= 0 || t >= 1) return 0;
			return SEVERING_CLEFT_DEPTH * Math.pow(1 - (2 * t - 1) * (2 * t - 1), 0.7) * (0.72 + 0.5 * smooth(s / 7.0, 2));
		}

		/** Smooth noise along its line (0 to 1), its own for each {@code channel}. */
		private double smooth(double x, int channel) {
			int i = (int) Math.floor(x);
			double f = x - i, u = f * f * (3 - 2 * f);
			return hash(i, channel, 0) * (1 - u) + hash(i + 1, channel, 0) * u;
		}

		/** A number from 0 to 1, the same for the same cast and the same {@code a, b, c}. */
		private double hash(int a, int b, int c) {
			long h = seed ^ a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL ^ c * 0x165667B19E3779F9L;
			h = (h ^ h >>> 33) * 0xFF51AFD7ED558CCDL;
			h = (h ^ h >>> 33) * 0xC4CEB9FE1A85EC53L;
			return ((h ^ h >>> 33) >>> 11) * 0x1.0p-53;
		}

		/**
		 * Its chasm from {@code s0} to {@code s1} blocks along its line, given to be cut (each cell once): across it within its width there (its
		 * edge ragged), from its floor (deepest along its middle, ragged) up past its line by half its width (the slot it cleaves through a hill).
		 */
		private void cleave(double s0, double s1) {
			Vec3 flat = new Vec3(way.x, 0, way.z);
			double flatLength = flat.length(), step = 0.6, strip = step * flatLength / 2 + 0.5;
			Vec3 forward = flatLength < 1.0e-3 ? Vec3.ZERO : flat.scale(1 / flatLength);
			List<BlockPos> cells = new ArrayList<>();
			for (double s = Math.max(CLEFT_FROM, s0); s <= s1; s += step) {
				Vec3 at = from.add(way.scale(s));
				double w = Math.max(SEVERING_BORE, cleftWidth(s)), h = cleftDepth(s);
				int r = (int) Math.ceil(w * 1.15) + 1, cx = net.minecraft.util.Mth.floor(at.x), cz = net.minecraft.util.Mth.floor(at.z);
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						double px = cx + dx + 0.5 - at.x, pz = cz + dz + 0.5 - at.z;
						if (Math.abs(px * forward.x + pz * forward.z) > strip) continue; // the next point along takes the next strip
						int x = cx + dx, z = cz + dz;
						double edge = w * (0.85 + 0.3 * hash(x, z, 3)), off = Math.abs(px * side.x + pz * side.z);
						if (off > edge) continue;
						double across = Math.sqrt(1 - (off / edge) * (off / edge));
						int bottom = net.minecraft.util.Mth.floor(at.y - Math.max(1, h * across * (0.85 + 0.3 * hash(x, z, 4)))),
								top = net.minecraft.util.Mth.floor(at.y + Math.max(SEVERING_BORE, w * 0.55) * across);
						for (int y = bottom; y <= top; y++) if (cleaved.add(BlockPos.asLong(x, y, z))) cells.add(new BlockPos(x, y, z));
					}
				}
			}
			Devastation.cut(caster, level, cells, from.add(way.scale((s0 + s1) / 2)));
		}

		/**
		 * Whether {@code e} is in its cut from {@code s0} to {@code s1} blocks along it: within {@code near} of its line, or down in the chasm it
		 * cleaves under it (across within its width there, between its floor and its line).
		 */
		private boolean inCut(LivingEntity e, double s0, double s1, double near) {
			Vec3 mid = HeavenSpells.middle(e).subtract(from);
			double s = mid.dot(way);
			if (s < s0 - 1 || s > s1 + 1) return false;
			Vec3 off = mid.subtract(way.scale(s));
			double half = e.getBbWidth() / 2;
			if (off.length() <= near + half) return true;
			double w = cleftWidth(s), h = cleftDepth(s);
			if (w < 0.5) return false;
			double across = Math.abs(off.x * side.x + off.z * side.z), below = -off.y;
			return across <= w + half && below >= -near && below <= h + e.getBbHeight() / 2 + 1;
		}

		/** Every being in its cut from {@code s0} to {@code s1} blocks along it ({@link #inCut}) that its caster's spells don't spare. */
		private List<LivingEntity> inCut(double s0, double s1, double near) {
			double out = SEVERING_CLEFT * 1.2 + 2;
			Vec3 a = from.add(way.scale(s0)), b = from.add(way.scale(s1));
			return level.getEntitiesOfClass(LivingEntity.class, new AABB(a, b).inflate(out, SEVERING_CLEFT_DEPTH + 3, out),
					e -> !HeavenSpells.spared(caster, e) && inCut(e, s0, s1, near));
		}

		@Override
		protected boolean run(long age, long now) {
			if (age <= SEVERING_THRUST) {
				double reach = SEVERING_LENGTH * age / SEVERING_THRUST;
				light.place(from.add(way.scale(reach)), face[0], face[1]);
				if (age == 0) return true;
				for (LivingEntity e : inCut(cut, reach, SEVERING_WIDTH)) {
					if (!struck.add(e)) continue;
					HeavenSpells.strike(caster, e, SEVERING_DAMAGE, spell);
					if (!HeavenSpells.greatBeast(caster, e)) HeavenSpells.move(e, way.scale(1.6).add(0, 0.3, 0));
					SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(e), way, 1.6f, 0);
				}
				if (HeavenSpells.carvesLand(caster)) cleave(Math.max(1.5, cut), reach);
				cut = reach;
				if (age == SEVERING_THRUST) {
					SpellFx.send(level, SpellFx.Kind.FLASH, from.add(way.scale(SEVERING_LENGTH)), Vec3.ZERO, 6, SWORD_WHITE, 192);
					sound(from.add(way.scale(SEVERING_LENGTH * 0.5)), SoundEvents.GLASS_BREAK, 4.0f, 0.4f);
				}
				return true;
			}
			// Space stays cut: whoever is in the cut, on its line or down in its chasm, is cut again, once a second.
			long open = age - SEVERING_THRUST;
			if (open % 20 == 10) {
				for (LivingEntity e : inCut(0, SEVERING_LENGTH, SEVERING_WIDTH * 0.75)) {
					HeavenSpells.strikeAgain(caster, e, SEVERING_RIFT_DAMAGE, spell);
					SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(e), Vec3.ZERO, 1.2f, 0);
				}
				sound(from.add(way.scale(SEVERING_LENGTH * 0.3)), SoundEvents.AMETHYST_BLOCK_CHIME, 3.0f, 0.5f);
			}
			return true;
		}

		@Override
		public void end() {
			light.dissolve();
			rift.dissolve();
		}
	}

	/** How high the last sword's rift opens over the prison's heart (50 by its realm). */
	public static final double PRISON_TOP = HeavenSpells.span(Realm.VOID_SHATTERING, 27);

	/**
	 * Ten-Direction Sword Prison (十方: the eight quarters, above and below): space tears open in ten places round where its caster looks:
	 * eight rifts in a ring {@link #PRISON_HIGH} over the land, one higher over its heart, and later one in the ground beneath it (SpellConstruct
	 * VOID_RIFT, ABYSS_RIFT; sword light's white). Out of the eight great swords plunge into the land in a ring {@link #PRISON_RADIUS} round
	 * (SWORD_PRISON, its own GeckoLib model, walls of sword light between them), each leaving a crater; for {@link #PRISON_TICKS} every foe
	 * inside is held in (thrown back from its edge) and takes {@link #PRISON_DAMAGE} a second. Then the last two swords, from above and from
	 * below, meet in its heart ({@link #PRISON_MEET}): {@link #PRISON_MEETING} to every foe inside, and a pit where they met.
	 */
	public static final class Prison extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final SpellConstruct prison, top;
		private final List<SpellConstruct> rifts = new ArrayList<>();
		private SpellConstruct below;
		private final int landsAt = PRISON_OPENS + PRISON_FALL, closesAt = PRISON_OPENS + PRISON_FALL + PRISON_TICKS, meetsAt = closesAt + PRISON_MEET;

		Prison(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, PRISON_OPENS + PRISON_FALL + PRISON_TICKS + PRISON_MEET + PRISON_AFTER);
			this.spell = spell;
			Vec3 found = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = found != null ? found : point;
			float yaw = caster.getYRot();
			this.prison = SpellConstruct.create(level, SpellConstruct.Kind.SWORD_PRISON, ground, yaw, 0, (int) (end - start), caster).sizedFor(spell).spawn();
			for (int k = 0; k < PRISON_SWORDS; k++) {
				rifts.add(SpellConstruct.create(level, SpellConstruct.Kind.VOID_RIFT, ring(k).add(0, PRISON_HIGH, 0), 0, 0, landsAt + 6 + SpellConstruct.FADE, caster)
						.size((float) PRISON_RIFT).tint(SWORD_WHITE).spawn());
			}
			this.top = SpellConstruct.create(level, SpellConstruct.Kind.VOID_RIFT, ground.add(0, PRISON_TOP, 0), 0, 0, closesAt + 8 + SpellConstruct.FADE, caster)
					.size((float) PRISON_RIFT * 1.6f).tint(SWORD_WHITE).spawn();
			sound(ground.add(0, PRISON_HIGH, 0), SoundEvents.END_PORTAL_SPAWN, 3.0f, 1.2f);
			sound(caster.position(), SoundEvents.TRIDENT_RETURN, 2.0f, 0.5f);
		}

		/** Where the ring's {@code k}th sword stands (its model's: the eight quarters round its facing). */
		private Vec3 ring(int k) {
			double angle = Math.toRadians(prison.getYRot()) + Math.PI * 2 * k / PRISON_SWORDS;
			return ground.add(-Math.sin(angle) * PRISON_RADIUS, 0, Math.cos(angle) * PRISON_RADIUS);
		}

		private boolean inside(LivingEntity e, double margin) {
			return Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) <= PRISON_RADIUS + margin && e.getY() > ground.y - 8 && e.getY() < ground.y + 30;
		}

		@Override
		protected boolean run(long age, long now) {
			if (age == landsAt) {
				// The eight driven into the land, the earth thrown up round each.
				for (int k = 0; k < PRISON_SWORDS; k++) {
					Vec3 at = ring(k);
					Devastation.crater(caster, level, at, PRISON_CRATER, PRISON_CRATER_DEPTH, Devastation.Floor.RUBBLE);
					SpellFx.send(level, SpellFx.Kind.EARTH_BURST, at, Vec3.ZERO, 3, Block.getId(Blocks.STONE.defaultBlockState()), 192);
				}
				SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) PRISON_RADIUS * 2.2f, 0, 192);
				sound(ground, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.5f);
				sound(ground, SoundEvents.ANVIL_LAND, 4.0f, 0.5f);
			}
			if (age == closesAt) {
				prison.act("converge");
				below = SpellConstruct.create(level, SpellConstruct.Kind.ABYSS_RIFT, ground.add(0, 0.06, 0), prison.getYRot(), 0, PRISON_MEET + 8 + SpellConstruct.FADE, caster)
						.size((float) PRISON_RIFT * 1.6f).tint(SWORD_WHITE).spawn();
				sound(ground, SoundEvents.TRIDENT_RIPTIDE_3, 4.0f, 0.6f);
			}
			if (age == meetsAt) meet();
			if (age < landsAt || age >= closesAt) return true;
			long holding = age - landsAt;
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, PRISON_RADIUS + 30, e -> HeavenSpells.mark(caster, e))) {
				if (!inside(e, 2.5)) continue;
				// Held in: at its edge, thrown back toward its heart.
				if (!inside(e, -1.0)) {
					Vec3 in = ground.subtract(e.position()).multiply(1, 0, 1).normalize();
					HeavenSpells.move(e, in.scale(0.8).add(0, 0.2, 0));
				}
				if (holding % 20 == 10) {
					HeavenSpells.strikeAgain(caster, e, PRISON_DAMAGE, spell);
					SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(e), Vec3.ZERO, 1.0f, 0);
				}
			}
			if (holding % 20 == 0) sound(ground, SoundEvents.BEACON_AMBIENT, 3.0f, 1.4f);
			return true;
		}

		/** The last two swords meet in its heart: every foe inside struck, a pit where they met. */
		private void meet() {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, PRISON_RADIUS + 30, e -> HeavenSpells.mark(caster, e))) {
				if (!inside(e, 1.5)) continue;
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, PRISON_MEETING, spell);
				HeavenSpells.push(ground, e, 1.4);
			}
			Devastation.crater(caster, level, ground, PRISON_PIT, PRISON_PIT_DEPTH, Devastation.Floor.RUBBLE);
			SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, 6, 0), Vec3.ZERO, 12, SWORD_WHITE, 192);
			SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, ground.add(0, 3, 0), Vec3.ZERO, (float) PRISON_RADIUS, 0, 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) PRISON_RADIUS * 2.5f, 0, 192);
			sound(ground, SoundEvents.GENERIC_EXPLODE, 5.0f, 0.5f);
			sound(ground, SoundEvents.BELL_BLOCK, 4.0f, 0.5f);
			sound(ground, SoundEvents.ANVIL_LAND, 4.0f, 0.4f);
			top.dissolve();
		}

		@Override
		public void end() {
			prison.dissolve();
			top.dissolve();
			for (SpellConstruct rift : rifts) rift.dissolve();
			if (below != null) below.dissolve();
		}
	}

	// --- Sacred Vessel ---

	/**
	 * Immortal-Binding Golden Ropes: a ring of golden rope, its coils hung with charms and bells (SpellConstruct BINDING_RING, its own GeckoLib
	 * model), comes to hang {@link #ROPES_HIGH} over where its caster looks ({@link #ROPES_COMES}), and its ropes (GOLDEN_ROPE, drawn by hand:
	 * TendrilRenderer) lash out to the foes within {@link #ROPES_RADIUS} of the spot, {@link #ROPES_MOST} at most, nearest first: each is
	 * bound (it can't walk, fly or strike back), hauled up to hang under the ring ({@link #ROPES_HAUL}) and held there for {@link #ROPES_TICKS},
	 * constricted for {@link #ROPES_DAMAGE} a second. Then the ropes cinch, {@link #ROPES_CINCH} more, and let go (what they held lands unharmed
	 * by the fall). A great beast is bound where it stands.
	 */
	public static final class Ropes extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground, ring;
		private final SpellConstruct coil;
		private final List<LivingEntity> bound = new ArrayList<>();
		private final List<Vec3> from = new ArrayList<>();
		private final List<SpellConstruct> ropes = new ArrayList<>();

		Ropes(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, ROPES_COMES + ROPES_TICKS + 20);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.ring = ground.add(0, ROPES_HIGH, 0);
			this.coil = SpellConstruct.create(level, SpellConstruct.Kind.BINDING_RING, ring, caster.getYRot(), 0, (int) (end - start), caster).sizedFor(spell).spawn();
			sound(ring, SoundEvents.BELL_RESONATE, 3.0f, 1.2f);
			sound(ring, SoundEvents.ARMOR_EQUIP_GOLD, 3.0f, 0.5f);
		}

		/** Where the {@code i}th of {@code n} bound hangs: in a ring under the ring, a little out from its heart. */
		private Vec3 hang(int i, int n) {
			double angle = Math.PI * 2 * i / Math.max(1, n);
			double out = n <= 1 ? 0 : ROPES_HANG * 0.35;
			return ring.add(Math.cos(angle) * out, -ROPES_HANG, Math.sin(angle) * out);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < ROPES_COMES) return true;
			long holding = age - ROPES_COMES;
			if (holding == 0) lash();
			if (holding >= ROPES_TICKS) {
				if (holding == ROPES_TICKS) cinch();
				return true;
			}
			for (int i = 0; i < bound.size(); i++) {
				LivingEntity e = bound.get(i);
				if (!HeavenSpells.present(e, level)) continue;
				if (HeavenSpells.greatBeast(caster, e)) {
					HeavenSpells.still(e);
				} else if (holding >= WoodSpells.WITHER_LASH) {
					// Hauled up once the rope has reached it, and held there.
					double f = Math.min(1, (holding - WoodSpells.WITHER_LASH) / (double) ROPES_HAUL);
					Vec3 to = hang(i, bound.size());
					Vec3 at = from.get(i).lerp(to.subtract(0, e.getBbHeight() / 2, 0), f * f * (3 - 2 * f));
					HeavenSpells.teleport(e, at);
					HeavenSpells.safeFall(e, 100);
				}
				if (holding % 20 == 10) {
					HeavenSpells.strikeAgain(caster, e, ROPES_DAMAGE, spell);
					e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 3));
					e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30, 3));
				}
			}
			if (holding % 20 == 10) sound(ring, SoundEvents.CHAIN_STEP, 3.0f, 0.6f);
			return true;
		}

		/** Its ropes lash out to the foes near the spot, nearest first. */
		private void lash() {
			List<LivingEntity> foes = HeavenSpells.aroundPoint(level, ground, ROPES_RADIUS + ROPES_HIGH, e -> HeavenSpells.mark(caster, e) && !HeavenSpells.spared(caster, e));
			foes.removeIf(e -> Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > ROPES_RADIUS || e.getY() > ring.y || e.getY() < ground.y - 16);
			foes.sort(java.util.Comparator.comparingDouble(e -> e.position().distanceToSqr(ground)));
			for (LivingEntity e : foes.subList(0, Math.min(ROPES_MOST, foes.size()))) {
				bound.add(e);
				from.add(e.position());
				ropes.add(SpellConstruct.create(level, SpellConstruct.Kind.GOLDEN_ROPE, ring, 0, 0, ROPES_TICKS + SpellConstruct.FADE, caster).size((float) HeavenSpells.greatness(Realm.SACRED_VESSEL) * 0.8f)
						.target(e).spawn());
				// A great beast bound where it stands; the rest held by the haul itself (a binding would hold them down against it).
				if (HeavenSpells.greatBeast(caster, e)) WoodSpells.bind(caster, e, ROPES_TICKS);
			}
			if (!bound.isEmpty()) sound(ring, SoundEvents.LEASH_KNOT_PLACE, 3.0f, 0.6f);
		}

		/** The ropes cinch, and let go. */
		private void cinch() {
			coil.act("cinch");
			for (LivingEntity e : bound) {
				if (!HeavenSpells.present(e, level)) continue;
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, ROPES_CINCH, spell);
				HeavenSpells.safeFall(e, 200);
				SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(e), Vec3.ZERO, 2, 0);
			}
			for (SpellConstruct rope : ropes) rope.dissolve();
			sound(ring, SoundEvents.CHAIN_BREAK, 4.0f, 0.5f);
			sound(ring, SoundEvents.BELL_BLOCK, 3.0f, 0.8f);
		}

		@Override
		public void end() {
			coil.dissolve();
			for (SpellConstruct rope : ropes) rope.dissolve();
			for (LivingEntity e : bound) HeavenSpells.safeFall(e, 200);
		}
	}

	/**
	 * Golden Dragon Scissors: a pair of colossal golden scissors, each blade wrought as a dragon of gold (SpellConstruct DRAGON_SCISSORS, its own
	 * GeckoLib model), opens {@link #SCISSORS_HIGH} over where its caster looks, lying level across their sight (its pivot half a blade to their
	 * left, its blades {@link #SCISSORS_LENGTH} long reaching out to their right, so their middle hangs over the spot), each blade
	 * {@link #SCISSORS_OPEN} degrees off the line ({@link #SCISSORS_OPENS}), holds a moment ({@link #SCISSORS_HOLD}) and snaps shut
	 * ({@link #SCISSORS_SNAP}): everything between its blades as they close takes {@link #SCISSORS_DAMAGE} and is cut down to the ground; the
	 * land is cut deep where the blades meet and scarred along where they lay open.
	 */
	public static final class Scissors extends HeavenSpells.Timed {
		private final Spell spell;
		/** Where they hang over (the spot looked at), the ground under their pivot (half a blade back from it), the pivot, and the way their points go. */
		private final Vec3 ground, base, pivot, along;
		private final float yaw;
		private final SpellConstruct scissors;

		Scissors(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, SCISSORS_OPENS + SCISSORS_HOLD + SCISSORS_SNAP + SCISSORS_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			// Lying across the caster's sight, its points to their right (the model's points are its -z: SpellConstructRenderer turns that to
			// the yaw's way), its pivot half a blade to their left so the blades lie over the spot.
			this.yaw = caster.getYRot() + 90;
			this.along = Vec3.directionFromRotation(0, yaw);
			this.base = ground.subtract(along.scale(SCISSORS_LENGTH / 2));
			this.pivot = base.add(0, SCISSORS_HIGH, 0);
			this.scissors = SpellConstruct.create(level, SpellConstruct.Kind.DRAGON_SCISSORS, pivot, yaw, 0, (int) (end - start), caster).sizedFor(spell).spawn();
			sound(pivot, SoundEvents.ARMOR_EQUIP_GOLD, 4.0f, 0.4f);
			sound(pivot, SoundEvents.BEACON_ACTIVATE, 3.0f, 0.7f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age == SCISSORS_OPENS + SCISSORS_HOLD) {
				scissors.act("snap");
				sound(pivot, SoundEvents.SHEEP_SHEAR, 5.0f, 0.3f);
			}
			if (age == SCISSORS_OPENS + SCISSORS_HOLD + SCISSORS_SNAP) snap();
			return true;
		}

		/** They snap shut: everything between the blades struck, the land cut where they met and scarred where they lay open. */
		private void snap() {
			double open = Math.toRadians(SCISSORS_OPEN);
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, SCISSORS_LENGTH * 0.5 + SCISSORS_LENGTH * Math.tan(open) + 8, e -> !HeavenSpells.spared(caster, e))) {
				Vec3 rel = e.position().subtract(base).multiply(1, 0, 1);
				double t = rel.dot(along), off = rel.subtract(along.scale(t)).length();
				if (t < -SCISSORS_WIDTH || t > SCISSORS_LENGTH || e.getY() > pivot.y + 8 || e.getY() < ground.y - 10) continue;
				// Between the blades: within their angle of the line out from the pivot, or near the line itself.
				if (off > SCISSORS_WIDTH && off > Math.max(0, t) * Math.tan(open)) continue;
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, SCISSORS_DAMAGE, spell);
				HeavenSpells.move(e, new Vec3(0, -0.8, 0));
				SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(e), Vec3.ZERO, 2.5f, 0);
			}
			Vec3 a = base, b = base.add(along.scale(SCISSORS_LENGTH));
			Devastation.gouge(caster, level, a, b, SCISSORS_CUT, SCISSORS_CUT_DEPTH);
			for (int s = -1; s <= 1; s += 2) {
				Vec3 blade = along.yRot((float) (s * open));
				Devastation.trench(caster, level, base, base.add(blade.scale(SCISSORS_LENGTH)), SCISSORS_SCAR, SCISSORS_SCAR_DEPTH);
			}
			SpellFx.send(level, SpellFx.Kind.STREAK, a.add(0, 1.5, 0), b.subtract(a), 0, 0xFFD54A, 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) (SCISSORS_LENGTH * 0.6), 0, 192);
			SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, pivot, Vec3.ZERO, 6, 0, 192);
			sound(pivot, SoundEvents.ANVIL_LAND, 5.0f, 0.5f);
			sound(ground, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.7f);
		}

		@Override
		public void end() {
			scissors.dissolve();
		}
	}

	// --- Entering Nirvana ---

	/**
	 * Sword Tomb Awakens: the grave of fallen swordsmen, a ring of {@link #TOMB_SWORDS} of their swords stuck point-down in the earth
	 * (SpellConstruct SWORD_TOMB, its own GeckoLib model), bursts up out of the land {@link #TOMB_RING} round where its caster looks
	 * ({@link #TOMB_ERUPTS}): everything within the ring takes {@link #TOMB_DAMAGE}, thrown up. The swords stand a moment, trembling
	 * ({@link #TOMB_STANDS}); then they tear free and rise over the ring, their points turned in on its heart ({@link #TOMB_RISES}), and plunge
	 * down into it one after another, {@link #TOMB_EVERY} tick apart: each strikes everything within {@link #TOMB_IMPACT} of where it lands for
	 * {@link #TOMB_SWORD}, and the last leaves a crater at the heart.
	 */
	public static final class Tombs extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final float yaw;
		private final SpellConstruct grave;
		private boolean erupted;

		Tombs(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, TOMB_ERUPTS + TOMB_STANDS + TOMB_RISES + TOMB_PLUNGE + TOMB_SWORDS * TOMB_EVERY + TOMB_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.yaw = caster.getYRot();
			this.grave = SpellConstruct.create(level, SpellConstruct.Kind.SWORD_TOMB, ground, yaw, 0, (int) (end - start), caster).sizedFor(spell).spawn();
			sound(ground, SoundEvents.SOUL_ESCAPE, 3.0f, 0.5f);
			sound(ground, SoundEvents.ANVIL_PLACE, 3.0f, 0.4f);
		}

		@Override
		protected boolean run(long age, long now) {
			// Its burst the first tick it is due (not its tick alone: a cast after this tick's spells have run is first seen a tick in).
			if (!erupted && age >= TOMB_ERUPTS / 2) {
				erupted = true;
				erupt();
			}
			long rising = age - TOMB_ERUPTS - TOMB_STANDS;
			if (rising == 0) {
				grave.act("rise");
				sound(ground, SoundEvents.TRIDENT_RETURN, 4.0f, 0.5f);
			}
			long raining = rising - TOMB_RISES;
			if (raining == 0) {
				grave.act("rain");
				sound(ground, SoundEvents.TRIDENT_THROW, 4.0f, 0.6f);
			}
			long landing = raining - TOMB_PLUNGE;
			if (landing >= 0 && landing % TOMB_EVERY == 0 && landing / TOMB_EVERY < TOMB_SWORDS) land((int) (landing / TOMB_EVERY));
			return true;
		}

		/** It bursts up: everything within the ring struck and thrown up. */
		private void erupt() {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, TOMB_RING + 6, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > TOMB_RING + 2 || e.getY() > ground.y + 12 || e.getY() < ground.y - 6) continue;
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, TOMB_DAMAGE, spell);
				if (!HeavenSpells.greatBeast(caster, e)) HeavenSpells.move(e, new Vec3(0, 0.9, 0));
				HeavenSpells.safeFall(e, 80);
			}
			for (int k = 0; k < 8; k++) {
				Vec3 at = ground.add(Vec3.directionFromRotation(0, yaw + 45 * k).scale(TOMB_RING));
				SpellFx.send(level, SpellFx.Kind.EARTH_BURST, at, Vec3.ZERO, 3, net.minecraft.world.level.block.Block.getId(net.minecraft.world.level.block.Blocks.COARSE_DIRT.defaultBlockState()), 192);
			}
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) TOMB_RING * 1.3f, 0, 192);
			sound(ground, SoundEvents.ROOTED_DIRT_BREAK, 4.0f, 0.5f);
			sound(ground, SoundEvents.ANVIL_LAND, 3.0f, 0.6f);
		}

		/** Sword {@code k} plunges into the heart: everything near where it lands struck; the last leaves a crater. */
		private void land(int k) {
			Vec3 at = ground.add(Vec3.directionFromRotation(0, yaw + 360f * k / TOMB_SWORDS).scale(TOMB_RING * 0.15));
			for (LivingEntity e : HeavenSpells.aroundPoint(level, at, TOMB_IMPACT + 4, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - at.x, e.getZ() - at.z) > TOMB_IMPACT || e.getY() > ground.y + 12 || e.getY() < ground.y - 8) continue;
				HeavenSpells.strikeAgain(caster, e, TOMB_SWORD, spell);
			}
			SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, at.add(0, 1, 0), Vec3.ZERO, 2.5f, 0, 160);
			if (k % 3 == 0) sound(at, SoundEvents.TRIDENT_HIT, 3.0f, 0.6f + 0.02f * k);
			if (k == TOMB_SWORDS - 1) {
				Devastation.crater(caster, level, ground, TOMB_CRATER, TOMB_CRATER_DEPTH, Devastation.Floor.RUBBLE);
				SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, 2, 0), Vec3.ZERO, 10, TOMB_LIGHT, 192);
				SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) TOMB_RING, 0, 192);
				sound(ground, SoundEvents.ANVIL_LAND, 4.0f, 0.4f);
			}
		}

		@Override
		public void end() {
			grave.dissolve();
		}
	}

	/**
	 * Nine Rebirths of the Sword: a colossal sword wreathed in white flame (SpellConstruct REBIRTH_SWORD, its own GeckoLib model) forms over where
	 * its caster looks ({@link #REBIRTH_FORMS}), its hilt {@link #REBIRTH_HIGH} over the heart, and sweeps round it {@link #REBIRTHS} times, each
	 * sweep {@link #REBIRTH_ARC} degrees in {@link #REBIRTH_SWEEP} ticks, begun 135 degrees on from the last and turning the other way (level, its
	 * point skimming the land, or rising off it or falling onto it as {@link #REBIRTH_TILTS} has it), reborn in its white flame between them
	 * ({@link #REBIRTH_REBORN}): everything within {@link #REBIRTH_REACH} it passes over takes {@link #REBIRTH_DAMAGE}, once a sweep. Reborn a last
	 * time high over the heart, point-down, it rises ({@link #REBIRTH_RISES}) and plunges into it ({@link #REBIRTH_FALL}): everything within
	 * {@link #REBIRTH_PLUNGE_RADIUS} takes {@link #REBIRTH_PLUNGE}, the land glassed into a crater, cracks running out of it; it stands there
	 * {@link #REBIRTH_AFTER} ticks and is gone.
	 */
	public static final class Rebirths extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground, hilt;
		private final float yaw;
		private final SpellConstruct sword;
		private final java.util.Set<LivingEntity> struck = new java.util.HashSet<>();
		private boolean plunged;

		Rebirths(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, REBIRTH_FORMS + REBIRTHS * (REBIRTH_SWEEP + REBIRTH_REBORN) + REBIRTH_RISES + REBIRTH_FALL + REBIRTH_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.hilt = ground.add(0, REBIRTH_HIGH, 0);
			this.yaw = caster.getYRot();
			this.sword = SpellConstruct.create(level, SpellConstruct.Kind.REBIRTH_SWORD, hilt, sweepYaw(0, 0), sweepPitch(0, 0), (int) (end - start), caster)
					.sizedFor(spell).spawn();
			sound(ground, SoundEvents.BLAZE_SHOOT, 3.0f, 1.4f);
			sound(ground, SoundEvents.BEACON_ACTIVATE, 3.0f, 1.2f);
		}

		/** Sweep {@code k}'s way {@code f} of the way through it: each begun 135 degrees on from the last, turning the other way. */
		private float sweepYaw(int k, double f) {
			return yaw + 135f * k + (k % 2 == 0 ? 1 : -1) * (float) (REBIRTH_ARC * f);
		}

		/** Sweep {@code k}'s pitch {@code f} of the way through it: level (its point skimming the land), rising off it, or falling onto it. */
		private float sweepPitch(int k, double f) {
			float tilt = REBIRTH_TILTS[k % REBIRTH_TILTS.length];
			return (float) (tilt < 0 ? REBIRTH_SKIM + tilt * f : REBIRTH_SKIM - tilt * (1 - f));
		}

		/** Where its point is, turned so. */
		private Vec3 point(float yaw, float pitch) {
			return hilt.add(Vec3.directionFromRotation(pitch, yaw).scale(REBIRTH_BLADE));
		}

		@Override
		protected boolean run(long age, long now) {
			long since = age - REBIRTH_FORMS, each = REBIRTH_SWEEP + REBIRTH_REBORN;
			if (since < 0) return true;
			int k = (int) (since / each);
			if (k < REBIRTHS) {
				long into = since % each;
				if (into < REBIRTH_SWEEP) sweep(k, into);
				else if (into == REBIRTH_SWEEP) reborn(k + 1);
				return true;
			}
			long after = since - REBIRTHS * each;
			if (after < REBIRTH_RISES) {
				double f = (after + 1) / (double) REBIRTH_RISES;
				sword.place(ground.add(0, REBIRTH_BLADE * (1.15 + 0.2 * f), 0), sweepYaw(REBIRTHS - 1, 1), 90);
			} else if (after < REBIRTH_RISES + REBIRTH_FALL) {
				double f = (after - REBIRTH_RISES + 1) / (double) REBIRTH_FALL;
				sword.place(ground.add(0, REBIRTH_BLADE * (1.35 - 0.5 * f * f), 0), sweepYaw(REBIRTHS - 1, 1), 90);
				if (after == REBIRTH_RISES) sound(ground, SoundEvents.TRIDENT_THROW, 5.0f, 0.4f);
			}
			if (!plunged && after >= REBIRTH_RISES + REBIRTH_FALL - 1) plunge();
			return true;
		}

		/** A tick of sweep {@code k}: it turns on, and everything within its reach in the arc it turned through is struck (once a sweep). */
		private void sweep(int k, long into) {
			if (into == 0) {
				struck.clear();
				sound(hilt, SoundEvents.PLAYER_ATTACK_SWEEP, 5.0f, 0.5f + 0.06f * k);
				sound(hilt, SoundEvents.BLAZE_SHOOT, 3.0f, 1.3f);
			}
			double f0 = into / (double) REBIRTH_SWEEP, f1 = (into + 1) / (double) REBIRTH_SWEEP;
			float from = sweepYaw(k, f0), to = sweepYaw(k, f1), pitch = sweepPitch(k, f1);
			sword.place(hilt, to, pitch);
			Vec3 a = point(from, sweepPitch(k, f0)), b = point(to, pitch);
			SpellFx.send(level, SpellFx.Kind.STREAK, a, b.subtract(a), 0, REBIRTH_LIGHT, 192);
			int way = k % 2 == 0 ? 1 : -1;
			double step = REBIRTH_ARC / REBIRTH_SWEEP;
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, REBIRTH_REACH + 4, e -> !struck.contains(e) && !HeavenSpells.spared(caster, e))) {
				double dx = e.getX() - ground.x, dz = e.getZ() - ground.z, far = Math.hypot(dx, dz);
				if (far > REBIRTH_REACH + e.getBbWidth() / 2 || e.getY() > ground.y + 16 || e.getY() < ground.y - 8) continue;
				// In the arc it turned through this tick (by its bearing from the heart), or under its hilt.
				double past = Mth.wrapDegrees(Math.toDegrees(Math.atan2(-dx, dz)) - from) * way, slack = Math.toDegrees(Math.atan2(e.getBbWidth(), Math.max(far, 1)));
				if (far > 3 && (past < -slack || past > step + slack)) continue;
				struck.add(e);
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, REBIRTH_DAMAGE, spell);
				SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(e), Vec3.ZERO, 2, 0);
			}
		}

		/** It is reborn in its white flame for sweep {@code k} (where that begins), or, the last time, high over the heart, point-down. */
		private void reborn(int k) {
			Vec3 at = k < REBIRTHS ? hilt : ground.add(0, REBIRTH_BLADE * 1.15, 0);
			if (k < REBIRTHS) sword.place(at, sweepYaw(k, 0), sweepPitch(k, 0));
			else sword.place(at, sweepYaw(REBIRTHS - 1, 1), 90);
			sword.act("reborn");
			SpellFx.send(level, SpellFx.Kind.FLASH, at, Vec3.ZERO, k < REBIRTHS ? 8 : 12, REBIRTH_LIGHT, 192);
			sound(at, SoundEvents.FIRECHARGE_USE, 4.0f, 0.6f + 0.05f * k);
			if (k == REBIRTHS) sound(at, SoundEvents.BEACON_POWER_SELECT, 5.0f, 0.8f);
		}

		/** Its last rebirth plunges into the heart: everything near struck, the land glassed into a crater and cracked round it. */
		private void plunge() {
			plunged = true;
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, REBIRTH_PLUNGE_RADIUS + 4, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > REBIRTH_PLUNGE_RADIUS || e.getY() > ground.y + 16 || e.getY() < ground.y - 10) continue;
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, REBIRTH_PLUNGE, spell);
				if (!HeavenSpells.greatBeast(caster, e)) HeavenSpells.push(ground, e, 1.6);
				HeavenSpells.safeFall(e, 80);
			}
			Devastation.crater(caster, level, ground, REBIRTH_CRATER, REBIRTH_CRATER_DEPTH, Devastation.Floor.GLASSY);
			Devastation.fissures(caster, level, ground, REBIRTH_CRACKS, 9, 4, REBIRTH_CRATER * 0.8);
			SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, 3, 0), Vec3.ZERO, 16, REBIRTH_LIGHT, 256);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) REBIRTH_PLUNGE_RADIUS * 1.3f, 0, 256);
			SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, ground.add(0, 2, 0), Vec3.ZERO, 8, 0, 256);
			sound(ground, SoundEvents.GENERIC_EXPLODE, 5.0f, 0.5f);
			sound(ground, SoundEvents.ANVIL_LAND, 5.0f, 0.4f);
		}

		@Override
		public void end() {
			sword.dissolve();
		}
	}

	// --- True Immortal ---

	/** Whether {@code e} stands within {@code width} of the line from {@code a} to {@code b} (level, by its feet; a little past either end). */
	static boolean nearLine(LivingEntity e, Vec3 a, Vec3 b, double width) {
		Vec3 line = b.subtract(a).multiply(1, 0, 1), rel = e.position().subtract(a).multiply(1, 0, 1);
		double length = Math.max(1.0e-3, line.length()), t = rel.dot(line) / length;
		return t >= -width && t <= length + width && rel.subtract(line.scale(t / length)).length() <= width + e.getBbWidth() / 2;
	}

	/**
	 * Heaven-Slaying Sword Rises: a sword of sword-light that could slay heaven itself, its guard of gold swept up like wings, a halo round its
	 * point (SpellConstruct HEAVEN_SLAYING_SWORD, its own GeckoLib model), rises point first out of the land where its caster looks
	 * ({@link #SLAYER_RISES}): everything within {@link #SLAYER_RADIUS} of its blade takes {@link #SLAYER_RISE}, thrown high. It stands
	 * ({@link #SLAYER_STANDS}), then flares: {@link #SLAYER_LINES} cuts run out from its foot across the land, {@link #SLAYER_CUTS} long,
	 * {@link #SLAYER_CUT_DAMAGE} to everything within {@link #SLAYER_CUT_WIDTH} of one (once), the land cut along them.
	 */
	public static final class Slayer extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final float yaw;
		private final SpellConstruct sword;
		private boolean risen;

		Slayer(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, SLAYER_RISES + SLAYER_STANDS + SLAYER_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.yaw = caster.getYRot();
			this.sword = SpellConstruct.create(level, SpellConstruct.Kind.HEAVEN_SLAYING_SWORD, ground, yaw, 0, (int) (end - start), caster).sizedFor(spell).spawn();
			sound(ground, SoundEvents.ROOTED_DIRT_BREAK, 5.0f, 0.3f);
			sound(ground, SoundEvents.TRIDENT_THROW, 5.0f, 0.4f);
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, ground, Vec3.ZERO, 8, Block.getId(Blocks.STONE.defaultBlockState()), 192);
		}

		@Override
		protected boolean run(long age, long now) {
			// Its point breaks the land a few ticks in (not its tick alone: a cast after this tick's spells have run is first seen a tick in).
			if (!risen && age >= 6) {
				risen = true;
				rise();
			}
			if (age == SLAYER_RISES + SLAYER_STANDS) flare();
			return true;
		}

		/** It rises: everything near its blade struck and thrown high. */
		private void rise() {
			for (LivingEntity e : WoodSpells.foesWithin(caster, level, ground, SLAYER_RADIUS, 8, 100)) {
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, SLAYER_RISE, spell);
				if (!HeavenSpells.greatBeast(caster, e)) HeavenSpells.move(e, new Vec3(0, 2.2, 0));
				HeavenSpells.safeFall(e, 160);
				SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(e), Vec3.ZERO, 3, 0);
			}
			SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, 6, 0), Vec3.ZERO, 14, SLAYER_LIGHT, 256);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) SLAYER_RADIUS * 3, 0, 256);
			sound(ground, SoundEvents.ANVIL_LAND, 5.0f, 0.5f);
			sound(ground, SoundEvents.BEACON_ACTIVATE, 5.0f, 1.2f);
		}

		/** It flares: its cuts run out from its foot, everything near one struck, the land cut along them. */
		private void flare() {
			sword.act("flare");
			java.util.Set<LivingEntity> struck = new java.util.HashSet<>();
			List<LivingEntity> near = WoodSpells.foesWithin(caster, level, ground, SLAYER_CUTS + SLAYER_CUT_WIDTH, 10, 30);
			for (int k = 0; k < SLAYER_LINES; k++) {
				Vec3 way = Vec3.directionFromRotation(0, yaw + 22.5f + 360f * k / SLAYER_LINES), a = ground.add(way.scale(2)), b = ground.add(way.scale(SLAYER_CUTS));
				for (LivingEntity e : near) {
					if (struck.contains(e) || !nearLine(e, a, b, SLAYER_CUT_WIDTH)) continue;
					struck.add(e);
					e.invulnerableTime = 0;
					HeavenSpells.strike(caster, e, SLAYER_CUT_DAMAGE, spell);
					SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(e), Vec3.ZERO, 2.5f, 0);
				}
				Devastation.trench(caster, level, a, b, SLAYER_CUT, SLAYER_CUT_DEPTH);
				SpellFx.send(level, SpellFx.Kind.STREAK, a.add(0, 1.5, 0), b.subtract(a), 0, SLAYER_LIGHT, 256);
			}
			SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, 30, 0), Vec3.ZERO, 20, SLAYER_LIGHT, 256);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) SLAYER_CUTS, 0, 256);
			sound(ground, SoundEvents.PLAYER_ATTACK_SWEEP, 6.0f, 0.4f);
			sound(ground, SoundEvents.GENERIC_EXPLODE, 5.0f, 0.6f);
		}

		@Override
		public void end() {
			sword.dissolve();
		}
	}

	/**
	 * Primal Axe Splits Chaos: Pangu's axe, that split heaven from earth, its haft of ancient wood bound in bronze and its crescent head swirled
	 * with chaos (SpellConstruct PANGU_AXE, its own GeckoLib model), stands {@link #AXE_BACK} behind its caster and is raised back over them
	 * ({@link #AXE_RAISES}), waits ({@link #AXE_HOLDS}), and chops down forward along their look ({@link #AXE_CHOPS}): everything within
	 * {@link #AXE_WIDTH} of its line, out to {@link #AXE_REACH}, takes {@link #AXE_DAMAGE}, thrown aside, and the land is split into a chasm
	 * along it (from {@link #AXE_SPARE} ahead of its caster), cracks running out where its head lands.
	 */
	public static final class Axe extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 haft, forward;
		private final float yaw;
		private final SpellConstruct axe;

		Axe(LivingEntity caster, Spell spell) {
			super(caster, AXE_RAISES + AXE_HOLDS + AXE_CHOPS + AXE_AFTER);
			this.spell = spell;
			this.yaw = caster.getYRot();
			this.forward = Vec3.directionFromRotation(0, yaw);
			Vec3 at = caster.position().subtract(forward.scale(AXE_BACK)), below = HeavenSpells.groundBelow(level, at.add(0, 4, 0), 6, 24);
			this.haft = below != null ? below : at;
			this.axe = SpellConstruct.create(level, SpellConstruct.Kind.PANGU_AXE, haft, yaw, 0, (int) (end - start), caster).sizedFor(spell).spawn();
			sound(haft, SoundEvents.ANVIL_PLACE, 5.0f, 0.3f);
			sound(haft, SoundEvents.BEACON_ACTIVATE, 5.0f, 0.4f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age == AXE_RAISES + AXE_HOLDS) {
				axe.act("chop");
				sound(haft, SoundEvents.PLAYER_ATTACK_SWEEP, 6.0f, 0.3f);
			}
			if (age == AXE_RAISES + AXE_HOLDS + AXE_CHOPS) chop();
			return true;
		}

		/** Its head lands: everything along its line struck and thrown aside, the land split into a chasm along it. */
		private void chop() {
			Vec3 a = haft.add(forward.scale(AXE_BACK + 3)), b = haft.add(forward.scale(AXE_REACH)), side = new Vec3(-forward.z, 0, forward.x);
			for (LivingEntity e : WoodSpells.foesWithin(caster, level, a.lerp(b, 0.5), AXE_REACH / 2 + AXE_WIDTH, 10, 30)) {
				if (!nearLine(e, a, b, AXE_WIDTH)) continue;
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, AXE_DAMAGE, spell);
				double s = Math.signum(e.position().subtract(a).dot(side));
				if (!HeavenSpells.greatBeast(caster, e)) HeavenSpells.move(e, side.scale((s == 0 ? 1 : s) * 1.8).add(0, 0.9, 0));
				HeavenSpells.safeFall(e, 120);
			}
			Devastation.gouge(caster, level, haft.add(forward.scale(AXE_BACK + AXE_SPARE)), b.add(forward.scale(6)), AXE_CHASM, AXE_CHASM_DEPTH);
			Devastation.fissures(caster, level, b, AXE_CRACKS, 8, 5, AXE_CHASM);
			SpellFx.send(level, SpellFx.Kind.STREAK, a.add(0, 1.5, 0), b.subtract(a), 0, CHAOS_SPLIT, 256);
			for (int i = 1; i <= 6; i++) {
				SpellFx.send(level, SpellFx.Kind.EARTH_BURST, a.lerp(b, i / 6.0), Vec3.ZERO, 6, Block.getId(Blocks.STONE.defaultBlockState()), 256);
			}
			SpellFx.send(level, SpellFx.Kind.FLASH, b.add(0, 4, 0), Vec3.ZERO, 20, CHAOS_SPLIT, 256);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, b, Vec3.ZERO, (float) AXE_CRACKS, 0, 256);
			sound(b, SoundEvents.GENERIC_EXPLODE, 8.0f, 0.3f);
			sound(b, SoundEvents.WARDEN_SONIC_BOOM, 6.0f, 0.4f);
			sound(b, SoundEvents.ANVIL_LAND, 6.0f, 0.3f);
		}

		@Override
		public void end() {
			axe.dissolve();
		}
	}

	static void tick(MinecraftServer server) {}

	static void forget(UUID id) {}

	static void clear() {}

	private MetalSpells() {}
}
