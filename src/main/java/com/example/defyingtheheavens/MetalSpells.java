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
	 * The stroke that looses it (SpellField SEVER_SWING): a sword of light {@link #SEVER_SWORD} blocks long forms in the caster's
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
		SpellField.create(level, SpellField.Kind.SWORD_RING, sky, (float) RAIN_RADIUS, 1, RAIN_FORM, p).data(RAIN_SWORDS).spawn();
		HeavenSpells.sound(level, sky, SoundEvents.ANVIL_PLACE, 1.0f, 1.6f);
		HeavenSpells.linger(new FireSpells.Delayed(p, RAIN_FORM, () -> {
			for (int i = 0; i < RAIN_SWORDS; i++) {
				double a = Math.PI * 2 * i / RAIN_SWORDS;
				Vec3 from = sky.add(Math.cos(a) * RAIN_RADIUS * 0.8, 0, Math.sin(a) * RAIN_RADIUS * 0.8);
				Vec3 down = target.add(Math.cos(a) * RAIN_RADIUS * 0.5 * level.random.nextDouble(), 0, Math.sin(a) * RAIN_RADIUS * 0.5 * level.random.nextDouble());
				SpellProjectile.thrown(level, p, spell, SpellProjectile.Shape.PLUNGING_SWORD, 1.0f, from, down.subtract(from).normalize().scale(1.8), 16,
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
		SpellField.create(level, SpellField.Kind.SWORD_LIGHT, from, 0.6f, (float) from.distanceTo(to), 14, p).facing(p.getYRot(), p.getXRot()).spawn();
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

	/** Sword Domain: swords circle the caster {@link #DOMAIN_RADIUS} round for {@link #DOMAIN_TICKS}; each foe inside is cut every half second. */
	public static final class Domain extends HeavenSpells.Timed {
		private final Spell spell;
		private final SpellField field;

		Domain(LivingEntity caster, Spell spell) {
			super(caster, DOMAIN_TICKS);
			this.spell = spell;
			this.field = SpellField.create(level, SpellField.Kind.SWORD_DOMAIN, caster.position(), (float) DOMAIN_RADIUS, 3, DOMAIN_TICKS, caster).data(36).spawn();
			HeavenSpells.sound(level, caster.position(), SoundEvents.BEACON_ACTIVATE, 1.5f, 1.8f);
			HeavenSpells.sound(level, caster.position(), SoundEvents.ANVIL_USE, 1.0f, 1.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			field.setPos(caster.getX(), caster.getY(), caster.getZ());
			if (age % 10 == 5) {
				for (LivingEntity e : HeavenSpells.around(caster, DOMAIN_RADIUS, e -> HeavenSpells.mark(caster, e))) {
					HeavenSpells.strikeAgain(caster, e, DOMAIN_DAMAGE, spell);
					SpellFx.send(level, SpellFx.Kind.STREAK, HeavenSpells.middle(caster).add(0, 1, 0), HeavenSpells.middle(e), 1, 0xF4F0D8);
					SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(e), Vec3.ZERO, 0.6f, 0);
				}
				if (age % 20 == 5) HeavenSpells.sound(level, caster.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.6f);
			}
			return true;
		}

		@Override
		public void end() {
			field.discard();
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
		SpellField sword = SpellField.create(level, SpellField.Kind.SEVER_SWING, p.position().add(0, p.getBbHeight() * 0.65, 0), SEVER_SWORD, 0,
				SEVER_STROKE, p).facing(yaw, 0).spawn();
		HeavenSpells.sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 3.0f, 0.4f);
		HeavenSpells.sound(level, p.position(), SoundEvents.TRIDENT_RETURN, 3.0f, 0.5f);
		HeavenSpells.linger(new SeverStroke(p, sword, () -> release(p, spell, level, flat, side, from, base, bladeY, half)));
	}

	/**
	 * The Heaven-Severing Sword's stroke (SpellField SEVER_SWING, drawn by the client to {@link #SEVER_FORM}, {@link #SEVER_SWING}):
	 * the slash is released {@link #SEVER_RELEASE} ticks in, as the blade crosses the front, and the sword is gone at the end. Its
	 * caster gone before the release (left, or another level), nothing is released.
	 */
	static final class SeverStroke extends HeavenSpells.Timed {
		private final SpellField sword;
		private final Runnable release;
		private boolean released;

		SeverStroke(LivingEntity caster, SpellField sword, Runnable release) {
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
			sword.discard();
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
			HeavenSpells.sound(level, at, SoundEvents.ANVIL_LAND, 4.0f, 0.3f);
			HeavenSpells.sound(level, at, SoundEvents.BEACON_ACTIVATE, 3.0f, 0.5f);
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
				HeavenSpells.sound(level, at, SoundEvents.GENERIC_EXPLODE, 5.0f, 0.4f);
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
			if (age % 20 == 0) HeavenSpells.sound(level, at, SoundEvents.BEACON_AMBIENT, 3.0f, 1.6f);
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
			HeavenSpells.sound(level, from, SoundEvents.RAVAGER_ROAR, 3.0f, 1.2f);
			HeavenSpells.sound(level, from, SoundEvents.TRIDENT_RIPTIDE_3, 2.0f, 0.8f);
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
					HeavenSpells.sound(level, land, SoundEvents.ANVIL_LAND, 3.0f, 0.6f);
					HeavenSpells.sound(level, land, SoundEvents.RAVAGER_ROAR, 3.0f, 0.9f);
				}
				return true;
			}
			if (age == landAt + TIGER_CLAW) {
				tiger.act("claw");
				HeavenSpells.sound(level, land, SoundEvents.PLAYER_ATTACK_SWEEP, 3.0f, 0.5f);
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
				if (age % 3 == 0) HeavenSpells.sound(level, mid, SoundEvents.GRINDSTONE_USE, 3.0f, 0.5f);
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
			HeavenSpells.sound(level, ground.add(0, SWORDS_HIGH, 0), SoundEvents.TRIDENT_RETURN, 3.0f, 0.5f);
			HeavenSpells.sound(level, caster.position(), SoundEvents.BEACON_ACTIVATE, 2.0f, 1.6f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age == SWORDS_GATHER) {
				vortex.act("merge");
				HeavenSpells.sound(level, ground.add(0, SWORDS_HIGH, 0), SoundEvents.BEACON_POWER_SELECT, 3.0f, 1.4f);
			}
			if (age < fuseAt) {
				if (age % 8 == 0) HeavenSpells.sound(level, ground.add(0, SWORDS_HIGH, 0), SoundEvents.TRIDENT_RIPTIDE_1, 2.5f, 1.2f + age * 0.01f);
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
				HeavenSpells.sound(level, ground, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.5f);
				HeavenSpells.sound(level, ground, SoundEvents.ANVIL_LAND, 4.0f, 0.4f);
				HeavenSpells.sound(level, ground, SoundEvents.BELL_BLOCK, 4.0f, 0.5f);
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
			HeavenSpells.sound(level, from, SoundEvents.TRIDENT_THUNDER, 2.0f, 1.4f);
			HeavenSpells.sound(level, from, SoundEvents.BEACON_POWER_SELECT, 2.0f, 1.8f);
			HeavenSpells.sound(level, from, SoundEvents.END_PORTAL_SPAWN, 2.0f, 1.6f);
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
					if (!HeavenSpells.greatBeast(e)) HeavenSpells.move(e, way.scale(1.6).add(0, 0.3, 0));
					SpellFx.send(level, SpellFx.Kind.METAL_SPARKS, HeavenSpells.middle(e), way, 1.6f, 0);
				}
				if (HeavenSpells.carvesLand(caster)) cleave(Math.max(1.5, cut), reach);
				cut = reach;
				if (age == SEVERING_THRUST) {
					SpellFx.send(level, SpellFx.Kind.FLASH, from.add(way.scale(SEVERING_LENGTH)), Vec3.ZERO, 6, SWORD_WHITE, 192);
					HeavenSpells.sound(level, from.add(way.scale(SEVERING_LENGTH * 0.5)), SoundEvents.GLASS_BREAK, 4.0f, 0.4f);
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
				HeavenSpells.sound(level, from.add(way.scale(SEVERING_LENGTH * 0.3)), SoundEvents.AMETHYST_BLOCK_CHIME, 3.0f, 0.5f);
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
			HeavenSpells.sound(level, ground.add(0, PRISON_HIGH, 0), SoundEvents.END_PORTAL_SPAWN, 3.0f, 1.2f);
			HeavenSpells.sound(level, caster.position(), SoundEvents.TRIDENT_RETURN, 2.0f, 0.5f);
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
				HeavenSpells.sound(level, ground, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.5f);
				HeavenSpells.sound(level, ground, SoundEvents.ANVIL_LAND, 4.0f, 0.5f);
			}
			if (age == closesAt) {
				prison.act("converge");
				below = SpellConstruct.create(level, SpellConstruct.Kind.ABYSS_RIFT, ground.add(0, 0.06, 0), prison.getYRot(), 0, PRISON_MEET + 8 + SpellConstruct.FADE, caster)
						.size((float) PRISON_RIFT * 1.6f).tint(SWORD_WHITE).spawn();
				HeavenSpells.sound(level, ground, SoundEvents.TRIDENT_RIPTIDE_3, 4.0f, 0.6f);
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
			if (holding % 20 == 0) HeavenSpells.sound(level, ground, SoundEvents.BEACON_AMBIENT, 3.0f, 1.4f);
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
			HeavenSpells.sound(level, ground, SoundEvents.GENERIC_EXPLODE, 5.0f, 0.5f);
			HeavenSpells.sound(level, ground, SoundEvents.BELL_BLOCK, 4.0f, 0.5f);
			HeavenSpells.sound(level, ground, SoundEvents.ANVIL_LAND, 4.0f, 0.4f);
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

	static void tick(MinecraftServer server) {}

	static void forget(UUID id) {}

	static void clear() {}

	private MetalSpells() {}
}
