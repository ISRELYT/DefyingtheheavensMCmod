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
	public static final int METEOR_FALL = 30, METEOR_CRATER = 6;
	public static final double PHOENIX_SPEED = 1.6, PHOENIX_RANGE = 64, PHOENIX_RADIUS = 5;
	public static final float PHOENIX_DAMAGE = 90;
	public static final int PHOENIX_CRATER = 4;
	public static final double SEA_RANGE = 48, SEA_RADIUS = 20, SEA_FIRES = 0.12;
	public static final int SEA_TICKS = 10 * 20;
	public static final float SEA_DAMAGE = 20;
	public static final double SUNS_RANGE = 64, SUNS_SPREAD = 10, SUNS_RADIUS = 7;
	public static final float SUNS_DAMAGE = 150;
	public static final int SUNS_CRATER = 6;
	public static final int NIRVANA_TICKS = 60 * 20;
	public static final double NIRVANA_RADIUS = 10;
	public static final float NIRVANA_DAMAGE = 200;

	/** Each caster's yang bomb, while one is set. */
	private static final Map<UUID, Bomb> BOMBS = new HashMap<>();

	static boolean canCast(ServerPlayer p, Spell spell) {
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

	static void cast(ServerPlayer p, Spell spell) {
		switch (spell) {
			case FLAME_BOLT -> bolt(p, spell);
			case FLAME_FIST -> {
				SpellAuras.put(p, SpellAura.FLAME_FIST, FLAME_FIST_TICKS);
				HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.FIRECHARGE_USE, 1.0f, 0.8f);
				HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.BLAZE_AMBIENT, 0.6f, 1.6f);
			}
			case CRIMSON_TEMPERING -> {
				SpellAuras.put(p, SpellAura.CRIMSON_TEMPERING, CRIMSON_TEMPERING_TICKS);
				HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.LAVA_AMBIENT, 1.2f, 0.7f);
				HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.FIRECHARGE_USE, 0.8f, 0.6f);
			}
			case REMOTE_YANG_BOMB -> setBomb(p, spell);
			case FLAME_ARMOUR -> {
				SpellAuras.put(p, SpellAura.FLAME_ARMOUR, FLAME_ARMOUR_TICKS);
				HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.BLAZE_SHOOT, 1.0f, 0.5f);
				HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.ARMOR_EQUIP_NETHERITE, 1.0f, 1.2f);
				SpellFx.send(p.serverLevel(), SpellFx.Kind.FLAME_BURST, HeavenSpells.middle(p), Vec3.ZERO, 1.2f, 0);
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
				HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.BEACON_POWER_SELECT, 1.2f, 0.6f);
				HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.BLAZE_SHOOT, 1.2f, 0.4f);
			}
			default -> {}
		}
	}

	// --- Qi Refining ---

	private static void bolt(ServerPlayer p, Spell spell) {
		SpellProjectile.cast(p, spell, SpellProjectile.Shape.FLAME_BOLT, 1.0f, 2, new Vec3(0.35, -0.35, 0.9), BOLT_SPEED, BOLT_RANGE,
				new SpellProjectile.Behaviour() {
					@Override
					public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
						burn(p, target, missile, BOLT_DAMAGE, BOLT_BURN_SECONDS, spell);
						return false;
					}

					@Override
					public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
						SpellFx.send(p.serverLevel(), SpellFx.Kind.FLAME_BURST, hit.getLocation(), Vec3.ZERO, 0.8f, 0);
						HeavenSpells.sound(p.serverLevel(), hit.getLocation(), SoundEvents.FIRECHARGE_USE, 0.6f, 1.3f);
					}
				});
		HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.BLAZE_SHOOT, 0.8f, 1.3f);
	}

	/**
	 * A fiery thing struck {@code target}: {@code damage} of fire qi through {@code via}, and set burning {@code seconds}; one
	 * warded by water takes none of it and doesn't catch (the ward's work, and a hiss of steam).
	 */
	private static void burn(ServerPlayer p, LivingEntity target, net.minecraft.world.entity.Entity via, float damage, int seconds, Spell spell) {
		ServerLevel level = p.serverLevel();
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

	private static void setBomb(ServerPlayer p, Spell spell) {
		BlockHitResult hit = HeavenSpells.lookedBlock(p, BOMB_REACH);
		if (hit == null) return;
		Vec3 at = hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.25));
		Bomb bomb = new Bomb(p, spell, at);
		BOMBS.put(p.getUUID(), bomb);
		HeavenSpells.linger(bomb);
		HeavenSpells.sound(p.serverLevel(), at, SoundEvents.FIRECHARGE_USE, 0.6f, 0.6f);
		p.displayClientMessage(Component.translatable(ModLang.MSG_YANG_BOMB_SET), true);
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

		Bomb(ServerPlayer caster, Spell spell, Vec3 at) {
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
			if (age % 20 == 19 && !caster.getAbilities().instabuild) {
				PlayerCultivation c = CultivationManager.get(caster);
				if (!c.consumeQi(BOMB_UPKEEP)) return gutter();
				CultivationManager.sync(caster);
			}
			return true;
		}

		private boolean gutter() {
			done = true;
			if (caster.isAlive()) caster.displayClientMessage(Component.translatable(ModLang.MSG_YANG_BOMB_GONE), true);
			return false;
		}

		/** Sets it off: if its caster is near enough ({@code asked}: they cast the spell again). */
		void detonate(boolean asked) {
			if (done) return;
			if (asked && caster.position().distanceToSqr(at) > BOMB_DETONATE_RANGE * BOMB_DETONATE_RANGE) {
				caster.displayClientMessage(Component.translatable(ModLang.MSG_YANG_BOMB_FAR), true);
				return;
			}
			done = true;
			BOMBS.remove(caster.getUUID());
			explode(caster, spell, level, at, BOMB_RADIUS, BOMB_DAMAGE, BOMB_BURN_SECONDS, 1.4);
			Spells.setCooldown(caster, spell, BOMB_COOLDOWN);
			Spells.sync(caster);
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
	static void explode(ServerPlayer p, Spell spell, ServerLevel level, Vec3 at, double radius, float damage, int seconds, double push) {
		SpellFx.send(level, SpellFx.Kind.FIRE_EXPLOSION, at, Vec3.ZERO, (float) radius, 0, 192);
		HeavenSpells.sound(level, at, SoundEvents.GENERIC_EXPLODE, (float) Math.min(4, 1 + radius / 4), 0.9f);
		HeavenSpells.sound(level, at, SoundEvents.BLAZE_SHOOT, 1.5f, 0.5f);
		for (LivingEntity target : HeavenSpells.aroundPoint(level, at, radius + 1, e -> !HeavenSpells.spared(p, e))) {
			double share = 1 - 0.4 * Math.min(1, target.position().distanceTo(at) / (radius + 1));
			burn(p, target, null, (float) (damage * share), seconds, spell);
			HeavenSpells.push(at, target, push);
		}
	}

	private static void serpent(ServerPlayer p, Spell spell) {
		SpellProjectile.cast(p, spell, SpellProjectile.Shape.FIRE_SERPENT, 1.0f, 8, new Vec3(0.4, -0.3, 1.0), SERPENT_SPEED, SERPENT_RANGE,
				new SpellProjectile.Behaviour() {
					@Override
					public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
						burn(p, target, missile, SERPENT_DAMAGE, 4, spell);
						SpellFx.send(p.serverLevel(), SpellFx.Kind.FLAME_BURST, missile.position(), Vec3.ZERO, 1.6f, 0);
						return false;
					}

					@Override
					public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
						SpellFx.send(p.serverLevel(), SpellFx.Kind.FLAME_BURST, hit.getLocation(), Vec3.ZERO, 1.4f, 0);
						HeavenSpells.sound(p.serverLevel(), hit.getLocation(), SoundEvents.FIRECHARGE_USE, 0.8f, 0.8f);
					}

					@Override
					public void launched(SpellProjectile missile) {
						HeavenSpells.sound(p.serverLevel(), missile.position(), SoundEvents.BLAZE_SHOOT, 1.0f, 0.6f);
					}
				}).homing(null, SERPENT_TURN).data(p.getRandom().nextInt());
		HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.BLAZE_AMBIENT, 1.0f, 0.7f);
	}

	// --- Core Formation ---

	private static void swirl(ServerPlayer p, Spell spell) {
		Vec3 ahead = Vec3.directionFromRotation(0, p.getYRot());
		Vec3 start = p.position().add(ahead.scale(3));
		Vec3 ground = HeavenSpells.groundBelow(p.serverLevel(), start, 3, 8);
		if (ground != null) start = ground;
		HeavenSpells.linger(new Swirl(p, spell, start, HeavenSpells.lookPoint(p, SWIRL_RANGE)));
		HeavenSpells.sound(p.serverLevel(), start, SoundEvents.BLAZE_SHOOT, 1.5f, 0.4f);
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

		Swirl(ServerPlayer caster, Spell spell, Vec3 start, Vec3 goal) {
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
							&& !Formations.shelters(level, fire) && !Formations.protectsGround(caster, fire) && caster.mayBuild() && level.mayInteract(caster, fire)) {
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

		Sun(ServerPlayer caster, Spell spell) {
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

	private static void karmic(ServerPlayer p, Spell spell) {
		LivingEntity target = HeavenSpells.aimed(p, KARMIC_RANGE);
		if (target == null) return;
		HeavenSpells.endLingering(p.getUUID(), Karma.class);
		HeavenSpells.linger(new Karma(p, spell, target));
		SpellAuras.put(target, SpellAura.KARMIC_FIRE, KARMIC_TICKS);
		SpellFx.send(p.serverLevel(), SpellFx.Kind.STREAK, HeavenSpells.fromHands(p), HeavenSpells.middle(target), 1, 0xE0482A);
		HeavenSpells.sound(p.serverLevel(), target.position(), SoundEvents.BELL_RESONATE, 1.0f, 0.5f);
		HeavenSpells.sound(p.serverLevel(), target.position(), SoundEvents.BLAZE_SHOOT, 1.0f, 0.4f);
	}

	/** Karmic Fire on one being: its sins burn, a second at a time ({@link #karmicHarm}), and a sinner's alignment burns toward none. */
	public static final class Karma extends HeavenSpells.Timed {
		private final Spell spell;
		public final LivingEntity target;

		Karma(ServerPlayer caster, Spell spell, LivingEntity target) {
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

	private static void meteor(ServerPlayer p, Spell spell) {
		LivingEntity aimed = HeavenSpells.aimed(p, METEOR_RANGE);
		Vec3 at = aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, METEOR_RANGE);
		fallOn(p, spell, at, SpellProjectile.Shape.METEOR, 1.0f, METEOR_FALL, METEOR_RADIUS, METEOR_DAMAGE, METEOR_CRATER, 0);
		HeavenSpells.sound(p.serverLevel(), at, SoundEvents.FIRECHARGE_USE, 2.0f, 0.3f);
	}

	/**
	 * Something huge and burning falls from the sky onto {@code at} over {@code ticks} (after {@code delay}), its mark glowing on the
	 * ground below it: a blast of fire there ({@link #explode}), a scorched crater {@code crater} round.
	 */
	static void fallOn(ServerPlayer p, Spell spell, Vec3 at, SpellProjectile.Shape shape, float scale, int ticks, double radius, float damage,
					   int crater, int delay) {
		ServerLevel level = p.serverLevel();
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
	private static void scorch(ServerPlayer p, ServerLevel level, Vec3 at, int crater) {
		if (crater <= 0) return;
		BlockPos centre = BlockPos.containing(at);
		Devastation.crater(p, level, at, crater, crater * 0.7, Devastation.Floor.SCORCHED);
		for (int i = 0; i < crater * 6; i++) {
			BlockPos pos = centre.offset(level.random.nextInt(crater * 4 + 1) - crater * 2, 0, level.random.nextInt(crater * 4 + 1) - crater * 2);
			Vec3 ground = HeavenSpells.groundBelow(level, Vec3.atBottomCenterOf(pos), 3, 6);
			if (ground == null) continue;
			BlockPos fire = BlockPos.containing(ground.x, ground.y + 0.1, ground.z);
			if (level.getBlockState(fire).isAir() && BaseFireBlock.canBePlacedAt(level, fire, net.minecraft.core.Direction.UP)
					&& !Formations.shelters(level, fire) && !Formations.protectsGround(p, fire) && p.mayBuild() && level.mayInteract(p, fire)) {
				level.setBlock(fire, BaseFireBlock.getState(level, fire), 11);
			}
		}
	}

	/** Something a spell does after a while (a meteor let fall when its mark has glowed, one of nine suns after another). */
	static final class Delayed extends HeavenSpells.Timed {
		private final Runnable then;

		Delayed(ServerPlayer caster, int ticks, Runnable then) {
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

	private static void phoenix(ServerPlayer p, Spell spell) {
		SpellProjectile.cast(p, spell, SpellProjectile.Shape.PHOENIX, 1.0f, 10, new Vec3(0, 0.6, 1.6), PHOENIX_SPEED, PHOENIX_RANGE,
				new SpellProjectile.Behaviour() {
					@Override
					public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
						explode(p, spell, p.serverLevel(), missile.position(), PHOENIX_RADIUS, PHOENIX_DAMAGE, 6, 1.8);
						scorch(p, p.serverLevel(), missile.position(), PHOENIX_CRATER);
						return false;
					}

					@Override
					public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
						explode(p, spell, p.serverLevel(), hit.getLocation(), PHOENIX_RADIUS, PHOENIX_DAMAGE, 6, 1.8);
						scorch(p, p.serverLevel(), hit.getLocation(), PHOENIX_CRATER);
					}

					@Override
					public void launched(SpellProjectile missile) {
						HeavenSpells.sound(p.serverLevel(), missile.position(), SoundEvents.PHANTOM_SWOOP, 2.0f, 0.5f);
						HeavenSpells.sound(p.serverLevel(), missile.position(), SoundEvents.BLAZE_SHOOT, 2.0f, 0.5f);
					}
				}).homing(null, 3).hitSize(1.5f);
		HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.PARROT_IMITATE_BLAZE, 2.0f, 0.5f);
	}

	/** Sea of Flames: the ground {@link #SEA_RADIUS} round a point burns; foes on it take {@link #SEA_DAMAGE} a second. */
	public static final class Sea extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 at;
		private final SpellField field;

		Sea(ServerPlayer caster, Spell spell, Vec3 point) {
			super(caster, SEA_TICKS);
			this.spell = spell;
			Vec3 ground = HeavenSpells.groundBelow(level, point.add(0, 1, 0), 4, 24);
			this.at = ground != null ? ground : point;
			this.field = SpellField.create(level, SpellField.Kind.SEA_OF_FLAMES, at, (float) SEA_RADIUS, 3, SEA_TICKS, caster)
					.data(caster.getRandom().nextInt()).spawn();
			// The land itself takes fire: scorched, patches of it molten, flames on what stands there (trees catch, and spread it).
			Devastation.burn(caster, level, at, SEA_RADIUS, SEA_FIRES);
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

	private static void nineSuns(ServerPlayer p, Spell spell) {
		LivingEntity aimed = HeavenSpells.aimed(p, SUNS_RANGE);
		Vec3 centre = aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, SUNS_RANGE);
		for (int i = 0; i < 9; i++) {
			Vec3 at = i == 0 ? centre : centre.add(Math.cos(Math.PI * 2 * i / 8) * SUNS_SPREAD, 0, Math.sin(Math.PI * 2 * i / 8) * SUNS_SPREAD);
			fallOn(p, spell, at, SpellProjectile.Shape.FALLING_SUN, 1.0f, 26, SUNS_RADIUS, SUNS_DAMAGE, SUNS_CRATER, i * 5);
		}
		HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.BEACON_ACTIVATE, 3.0f, 0.4f);
		HeavenSpells.sound(p.serverLevel(), centre, SoundEvents.BLAZE_SHOOT, 3.0f, 0.3f);
	}

	/**
	 * Phoenix Nirvana: for its time, a death of its caster's is undone: they rise again at full health in a burst of flame
	 * ({@link #NIRVANA_DAMAGE} to all but their allies within {@link #NIRVANA_RADIUS}), harmful effects burnt away. Once.
	 */
	public static final class Nirvana extends HeavenSpells.Timed implements HeavenSpells.DeathListener {
		private final Spell spell;
		private boolean spent;

		Nirvana(ServerPlayer caster, Spell spell) {
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
			caster.displayClientMessage(Component.translatable(ModLang.MSG_PHOENIX_REBORN), true);
			return false;
		}

		@Override
		public void end() {
			SpellAuras.remove(caster, SpellAura.PHOENIX_NIRVANA);
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
