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
	 * {@link #FLAME_HEIGHT} high, {@link #FLAME_DAMAGE} a second); the glassy pit (10 round, 6 deep: by its realm); the seed's flight and
	 * bloom and the pillar's burning (ticks).
	 */
	public static final double FLAME_RANGE = HeavenSpells.span(Realm.DAO_INTEGRATION, 64), FLAME_RADIUS = HeavenSpells.span(Realm.DAO_INTEGRATION, 8), FLAME_HEIGHT = HeavenSpells.span(Realm.DAO_INTEGRATION, 92), FLAME_PIT = Devastation.sized(Realm.DAO_INTEGRATION, 8),
			FLAME_PIT_DEPTH = Devastation.sized(Realm.DAO_INTEGRATION, 5);
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
					BlockPos fire = BlockPos.containing(at.x + Math.cos(a + 2) * r, at.y + 0.2, at.z + Math.sin(a + 2) * r);
					if (level.getBlockState(fire).isAir() && BaseFireBlock.canBePlacedAt(level, fire, net.minecraft.core.Direction.UP)
							&& Devastation.mayChange(caster, level, fire)) {
						level.setBlock(fire, BaseFireBlock.getState(level, fire), 11);
					}
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
					SOLAR_TICKS, caster).spawn();
			HeavenSpells.sound(level, caster.position(), SoundEvents.BEACON_ACTIVATE, 2.0f, 0.5f);
			HeavenSpells.sound(level, caster.position(), SoundEvents.BLAZE_SHOOT, 2.0f, 0.3f);
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
				if (age % 60 == 10) HeavenSpells.sound(level, sun, SoundEvents.FIRE_AMBIENT, 2.0f, 0.5f);
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
				HeavenSpells.sound(level, target.position(), SoundEvents.FIRE_AMBIENT, 1.0f, 0.6f);
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
		fallOn(p, spell, at, SpellProjectile.Shape.METEOR, 1.0f, METEOR_FALL, METEOR_RADIUS, METEOR_DAMAGE, METEOR_CRATER, 0);
		HeavenSpells.sound(HeavenSpells.level(p), at, SoundEvents.FIRECHARGE_USE, 2.0f, 0.3f);
	}

	/**
	 * Something huge and burning falls from the sky onto {@code at} over {@code ticks} (after {@code delay}), its mark glowing on the
	 * ground below it: a blast of fire there ({@link #explode}), a scorched crater {@code crater} round.
	 */
	static void fallOn(LivingEntity p, Spell spell, Vec3 at, SpellProjectile.Shape shape, float scale, int ticks, double radius, float damage,
					   int crater, int delay) {
		ServerLevel level = HeavenSpells.level(p);
		Vec3 ground = HeavenSpells.groundBelow(level, at.add(0, 2, 0), 4, 32);
		Vec3 target = ground != null ? ground : at;
		SpellField mark = SpellField.create(level, SpellField.Kind.METEOR_MARK, target, (float) radius, 1, ticks + delay, p).spawn();
		Vec3 from = target.add(Vec3.directionFromRotation(0, p.getYRot()).scale(-18)).add(0, 40, 0);
		Vec3 velocity = target.subtract(from).scale(1.0 / ticks);
		Runnable drop = () -> SpellProjectile.thrown(level, p, spell, shape, scale, from, velocity, from.distanceTo(target) + 4,
				new SpellProjectile.Behaviour() {
					private void land(Vec3 where) {
						mark.discard();
						explode(p, spell, level, where, radius, damage, 6, 2.0);
						SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, where, Vec3.ZERO, (float) radius * 1.5f, 0, 192);
						scorch(p, level, where, crater);
					}

					@Override
					public boolean hitEntity(SpellProjectile missile, LivingEntity hit) {
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
				}).hitSize(scale * 1.5f).data(level.random.nextInt());
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
			BlockPos fire = BlockPos.containing(ground.x, ground.y + 0.1, ground.z);
			if (level.getBlockState(fire).isAir() && BaseFireBlock.canBePlacedAt(level, fire, net.minecraft.core.Direction.UP)
					&& Devastation.mayChange(p, level, fire)) {
				level.setBlock(fire, BaseFireBlock.getState(level, fire), 11);
			}
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

	private static void phoenix(LivingEntity p, Spell spell) {
		SpellProjectile.cast(p, spell, SpellProjectile.Shape.PHOENIX, 1.0f, 10, new Vec3(0, 0.6, 1.6), PHOENIX_SPEED, PHOENIX_RANGE,
				new SpellProjectile.Behaviour() {
					@Override
					public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
						explode(p, spell, HeavenSpells.level(p), missile.position(), PHOENIX_RADIUS, PHOENIX_DAMAGE, 6, 1.8);
						scorch(p, HeavenSpells.level(p), missile.position(), PHOENIX_CRATER);
						return false;
					}

					@Override
					public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
						explode(p, spell, HeavenSpells.level(p), hit.getLocation(), PHOENIX_RADIUS, PHOENIX_DAMAGE, 6, 1.8);
						scorch(p, HeavenSpells.level(p), hit.getLocation(), PHOENIX_CRATER);
					}

					@Override
					public void launched(SpellProjectile missile) {
						HeavenSpells.sound(HeavenSpells.level(p), missile.position(), SoundEvents.PHANTOM_SWOOP, 2.0f, 0.5f);
						HeavenSpells.sound(HeavenSpells.level(p), missile.position(), SoundEvents.BLAZE_SHOOT, 2.0f, 0.5f);
					}
				}).homing(null, 3).hitSize(1.5f);
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
			HeavenSpells.sound(level, at, SoundEvents.BLAZE_SHOOT, 3.0f, 0.3f);
			HeavenSpells.sound(level, at, SoundEvents.FIRECHARGE_USE, 3.0f, 0.4f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age % 20 == 10) {
				for (LivingEntity e : HeavenSpells.aroundPoint(level, at, SEA_RADIUS + 2, e -> !HeavenSpells.friendly(caster, e) && !HeavenSpells.spared(caster, e))) {
					if (e.getY() > at.y + 4 || e.position().multiply(1, 0, 1).distanceTo(at.multiply(1, 0, 1)) > SEA_RADIUS) continue;
					burn(caster, e, null, SEA_DAMAGE, 3, spell);
				}
				if (age % 40 == 10) HeavenSpells.sound(level, at, SoundEvents.FIRE_AMBIENT, 3.0f, 0.5f);
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
			fallOn(p, spell, at, SpellProjectile.Shape.FALLING_SUN, 1.0f, 26, SUNS_RADIUS, SUNS_DAMAGE, SUNS_CRATER, i * 5);
		}
		HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.BEACON_ACTIVATE, 3.0f, 0.4f);
		HeavenSpells.sound(HeavenSpells.level(p), centre, SoundEvents.BLAZE_SHOOT, 3.0f, 0.3f);
	}

	/**
	 * Phoenix Nirvana: for its time, a death of its caster's is undone: they rise again at full health in a burst of flame
	 * ({@link #NIRVANA_DAMAGE} to all but their allies within {@link #NIRVANA_RADIUS}), harmful effects burnt away. Once.
	 */
	public static final class Nirvana extends HeavenSpells.Timed implements HeavenSpells.DeathListener {
		private final Spell spell;
		private boolean spent;

		Nirvana(LivingEntity caster, Spell spell) {
			super(caster, NIRVANA_TICKS);
			this.spell = spell;
		}

		@Override
		protected boolean run(long age, long now) {
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
			HeavenSpells.sound(level, caster.position(), SoundEvents.TOTEM_USE, 1.5f, 0.6f);
			HeavenSpells.sound(level, caster.position(), SoundEvents.BLAZE_SHOOT, 2.0f, 0.4f);
			HeavenSpells.tell(caster, Component.translatable(ModLang.MSG_PHOENIX_REBORN));
			return false;
		}

		@Override
		public void end() {
			SpellAuras.remove(caster, SpellAura.PHOENIX_NIRVANA);
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
			HeavenSpells.sound(level, high, SoundEvents.PARROT_IMITATE_BLAZE, 3.0f, 0.5f);
			HeavenSpells.sound(level, caster.position(), SoundEvents.PHANTOM_SWOOP, 3.0f, 0.4f);
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
				HeavenSpells.sound(level, here, SoundEvents.GENERIC_EXPLODE, 3.0f, 0.7f);
				HeavenSpells.sound(level, here, SoundEvents.PARROT_IMITATE_BLAZE, 4.0f, 0.4f);
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
			if (age % 10 == 0) HeavenSpells.sound(level, here, SoundEvents.BLAZE_SHOOT, 3.0f, 0.5f);
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
	 * and burns; the ground under it melts into a glassy pit {@link #FLAME_PIT} round.
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
			HeavenSpells.sound(level, from, SoundEvents.BLAZE_SHOOT, 2.0f, 1.6f);
			HeavenSpells.sound(level, from, SoundEvents.BEACON_ACTIVATE, 1.5f, 1.8f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age <= FLAME_FLIGHT) {
				// It arcs to the spot, turning as it goes, and hangs over it.
				double f = age / (double) FLAME_FLIGHT;
				seed.place(from.lerp(ground.add(0, 2.5, 0), f).add(0, Math.sin(Math.PI * f) * 6, 0), seed.getYRot() + 18, 0);
				if (age == FLAME_FLIGHT) {
					seed.act("bloom");
					HeavenSpells.sound(level, ground, SoundEvents.BEACON_POWER_SELECT, 2.0f, 1.5f);
				}
				return true;
			}
			long burning = age - FLAME_FLIGHT - FLAME_BLOOM;
			if (burning == 0) {
				seed.dissolve();
				pillar = SpellConstruct.create(level, SpellConstruct.Kind.ONE_FLAME_PILLAR, ground, 0, 0, FLAME_TICKS, caster).sizedFor(spell).spawn();
				Devastation.crater(caster, level, ground, FLAME_PIT, FLAME_PIT_DEPTH, Devastation.Floor.GLASSY);
				SpellFx.send(level, SpellFx.Kind.FIRE_EXPLOSION, ground, Vec3.ZERO, (float) FLAME_RADIUS * 1.4f, 0, 192);
				SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) FLAME_RADIUS * 2.5f, 0, 192);
				HeavenSpells.sound(level, ground, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.5f);
				HeavenSpells.sound(level, ground, SoundEvents.BLAZE_SHOOT, 4.0f, 0.3f);
			}
			if (burning >= 0 && burning % 20 == 0) {
				Vec3 middle = ground.add(0, FLAME_HEIGHT / 2, 0);
				for (LivingEntity e : HeavenSpells.aroundPoint(level, middle, FLAME_HEIGHT / 2 + FLAME_RADIUS, e -> !HeavenSpells.spared(caster, e))) {
					Vec3 m = HeavenSpells.middle(e);
					if (Math.hypot(m.x - ground.x, m.z - ground.z) > FLAME_RADIUS || m.y < ground.y - FLAME_PIT_DEPTH - 2 || m.y > ground.y + FLAME_HEIGHT) continue;
					e.invulnerableTime = 0;
					burn(caster, e, null, FLAME_DAMAGE, FLAME_BURN_SECONDS, spell);
				}
				HeavenSpells.sound(level, ground, SoundEvents.FIRE_AMBIENT, 4.0f, 0.4f);
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
			HeavenSpells.sound(level, mouth, SoundEvents.END_PORTAL_SPAWN, 3.0f, 0.8f);
			HeavenSpells.sound(level, caster.position(), SoundEvents.BLAZE_SHOOT, 2.0f, 0.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < CROW_OPENS) return true;
			float[] face = HeavenSpells.facing(ground.subtract(mouth));
			if (age == CROW_OPENS) {
				crow = SpellConstruct.create(level, SpellConstruct.Kind.GOLDEN_CROW, mouth, face[0], face[1], CROW_DIVE + CROW_AFTER, caster).sizedFor(spell).spawn();
				SpellFx.send(level, SpellFx.Kind.FLASH, mouth, Vec3.ZERO, 8, SUN_GOLD, 192);
				HeavenSpells.sound(level, mouth, SoundEvents.PARROT_IMITATE_BLAZE, 4.0f, 0.4f);
				HeavenSpells.sound(level, mouth, SoundEvents.FIRECHARGE_USE, 3.0f, 0.5f);
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
			HeavenSpells.sound(level, ground, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.5f);
			HeavenSpells.sound(level, ground, SoundEvents.PARROT_IMITATE_BLAZE, 4.0f, 0.3f);
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
			HeavenSpells.sound(level, mouth, SoundEvents.END_PORTAL_SPAWN, 3.0f, 0.6f);
			HeavenSpells.sound(level, mouth, SoundEvents.BEACON_ACTIVATE, 3.0f, 0.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < SUN_OPENS) return true;
			if (sun == null) {
				// Dragged out of it once it is open, small, swelling as it comes (its model's emerge).
				sun = SpellConstruct.create(level, SpellConstruct.Kind.VOID_SUN, mouth, yaw, 0, SUN_PULL + SUN_TICKS + SUN_COLLAPSE + SpellConstruct.FADE, caster)
						.sizedFor(spell).spawn();
				SpellFx.send(level, SpellFx.Kind.FLASH, mouth, Vec3.ZERO, 8, SUN_GOLD, 192);
				HeavenSpells.sound(level, mouth, SoundEvents.FIRECHARGE_USE, 4.0f, 0.4f);
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
				HeavenSpells.sound(level, mouth, SoundEvents.END_PORTAL_SPAWN, 2.0f, 1.4f);
			}
			if (burning < SUN_TICKS && burning % 20 == 0) scorch(burning);
			if (burning == SUN_TICKS) {
				sun.act("collapse");
				HeavenSpells.sound(level, at, SoundEvents.BEACON_DEACTIVATE, 4.0f, 0.4f);
				HeavenSpells.sound(level, at, SoundEvents.WARDEN_SONIC_CHARGE, 3.0f, 0.6f);
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
			HeavenSpells.sound(level, at, SoundEvents.FIRE_AMBIENT, 4.0f, 0.4f);
			HeavenSpells.sound(level, at, SoundEvents.BLAZE_BURN, 3.0f, 0.4f);
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
			HeavenSpells.sound(level, at, SoundEvents.GENERIC_EXPLODE, 5.0f, 0.4f);
			HeavenSpells.sound(level, at, SoundEvents.DRAGON_FIREBALL_EXPLODE, 4.0f, 0.5f);
			sun.dissolve();
		}

		@Override
		public void end() {
			rift.dissolve();
			if (sun != null) sun.dissolve();
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
