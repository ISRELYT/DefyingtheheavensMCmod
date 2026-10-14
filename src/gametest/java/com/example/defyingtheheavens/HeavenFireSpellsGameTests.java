package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;

/**
 * The fire methods' twelve spells (FireSpells), each doing what it says, each test alone in its batch on a floor of its own
 * (HeavenSpellTesting). Husks stand in for foes, warded against ordinary burning so that only the spells' own harm counts.
 */
public class HeavenFireSpellsGameTests implements FabricGameTest {
	/** A husk standing idle at {@code at}, of {@code health}, that ordinary burning can't harm. */
	private static Husk husk(ServerLevel level, Vec3 at, double health, List<LivingEntity> spawned) {
		Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, at, health, spawned);
		husk.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2000, 0));
		return husk;
	}

	/** Turns {@code mob} to look at {@code point}. */
	private static void face(Mob mob, Vec3 point) {
		Vec3 d = point.subtract(mob.getEyePosition());
		float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90);
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		mob.setYRot(yaw);
		mob.setYHeadRot(yaw);
		mob.yHeadRotO = yaw;
		mob.setXRot(pitch);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_fire_sleepingemberbursts")
	public void sleepingEmberBursts(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.FIRE_QI_REFINING, centre);
			Husk marked = husk(level, Vec3.atBottomCenterOf(centre.south(6)), 40, spawned);
			Husk beside = husk(level, Vec3.atBottomCenterOf(centre.south(6)).add(1.5, 0, 0), 40, spawned);
			HeavenSpellTesting.lookAt(p, HeavenSpells.middle(marked));
			boolean cast = HeavenSpellTesting.cast(p, Spell.SLEEPING_EMBER);
			float before = marked.getHealth();
			helper.runAfterDelay(66, () -> {
				try {
					helper.assertTrue(cast && before == 40, "Lodged, asleep (" + cast + ", " + before + ")");
					helper.assertTrue(marked.getHealth() == 33 && beside.getHealth() == 37, "It burst: 7 to the marked, 3 beside it (" + marked.getHealth()
							+ ", " + beside.getHealth() + ")");
					helper.assertTrue(marked.isOnFire(), "...and the marked burns");
					HeavenSpellTesting.lookAt(p, p.getEyePosition().add(0, 30, 0.1));
					helper.assertTrue(!HeavenSpellTesting.cast(p, Spell.SLEEPING_EMBER), "No being looked at: not cast");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_fire_struckflintblindsthelookers")
	public void struckFlintBlindsTheLookers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.FIRE_QI_REFINING, centre);
			Husk looking = husk(level, Vec3.atBottomCenterOf(centre.south(5)), 20, spawned);
			Husk away = husk(level, Vec3.atBottomCenterOf(centre.east(5)), 20, spawned);
			face(looking, p.getEyePosition());
			face(away, away.getEyePosition().add(10, 0, 0));
			looking.setTarget(p);
			boolean cast = HeavenSpellTesting.cast(p, Spell.STRUCK_FLINT_FLASH);
			helper.runAfterDelay(2, () -> {
				try {
					helper.assertTrue(cast && looking.hasEffect(MobEffects.BLINDNESS) && looking.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
							"The one looking on is blinded and slowed (" + looking.getActiveEffects() + ")");
					helper.assertTrue(looking.getTarget() == null, "...and loses sight of the caster");
					helper.assertTrue(!away.hasEffect(MobEffects.BLINDNESS), "The one looking away sees nothing");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_fire_firehorsegallopburnsprints")
	public void fireHorseGallopBurnsPrints(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 6, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.FIRE_FOUNDATION_BUILDING, centre);
			boolean cast = HeavenSpellTesting.cast(p, Spell.FIRE_HORSE_GALLOP);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, p.position().add(0.4, 0, 0), 20, spawned);
			helper.runAfterDelay(3, () -> {
				try {
					MobEffectInstance speed = p.getEffect(MobEffects.MOVEMENT_SPEED);
					helper.assertTrue(cast && speed != null && speed.getAmplifier() == 2 && p.hasEffect(MobEffects.DIG_SPEED), "Speed III and Haste II ("
							+ p.getActiveEffects() + ")");
					FireSpells.Gallop gallop = HeavenSpells.lingering(p.getUUID(), FireSpells.Gallop.class).get(0);
					helper.assertTrue(gallop.prints() == 1, "A hoofprint left (" + gallop.prints() + ")");
					helper.assertTrue(husk.getHealth() <= 18 && husk.getHealth() > 16 && husk.isOnFire(), "Treading on it burns (" + husk.getHealth() + ", "
							+ husk.isOnFire() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_fire_sealedfurnacerefinesharm")
	public void sealedFurnaceRefinesHarm(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 6, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.FIRE_FOUNDATION_BUILDING, centre);
			Husk husk = husk(level, Vec3.atBottomCenterOf(centre.south(2)), 20, spawned);
			boolean cast = HeavenSpellTesting.cast(p, Spell.SEALED_FURNACE_REFINEMENT);
			PlayerCultivation c = CultivationManager.get(p);
			c.setQi(c.maxQi() - 500);
			double qi = c.getQi();
			float blow = HeavenSpells.incoming(p, p.damageSources().mobAttack(husk), 10);
			double afterBlow = c.getQi();
			float fire = HeavenSpells.incoming(p, p.damageSources().lava(), 4);
			double afterFire = c.getQi();
			helper.runAfterDelay(1, () -> {
				try {
					helper.assertTrue(cast && HeavenSpellTesting.near(blow, 6.5) && HeavenSpellTesting.near(afterBlow - qi, 10.5),
							"A blow cut by 35%, its 3.5 held back refined to 10.5 qi (" + blow + ", " + (afterBlow - qi) + ")");
					helper.assertTrue(fire == 0 && HeavenSpellTesting.near(afterFire - afterBlow, 12), "Fire held back whole (" + fire + ", "
							+ (afterFire - afterBlow) + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_fire_prairiesparkspreads")
	public void prairieSparkSpreads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 14, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.FIRE_CORE_FORMATION, centre);
			Husk first = husk(level, Vec3.atBottomCenterOf(centre.south(6)), 40, spawned);
			Husk near = husk(level, Vec3.atBottomCenterOf(centre.south(6).east(2)), 40, spawned);
			Husk far = husk(level, Vec3.atBottomCenterOf(centre.south(6).west(10)), 40, spawned);
			HeavenSpellTesting.lookAt(p, HeavenSpells.middle(first));
			boolean cast = HeavenSpellTesting.cast(p, Spell.PRAIRIE_BURNING_SPARK);
			helper.runAfterDelay(23, () -> {
				try {
					FireSpells.Spark spark = HeavenSpells.lingering(p.getUUID(), FireSpells.Spark.class).get(0);
					helper.assertTrue(cast && spark.burns(first) && first.getHealth() == 37.5f, "Crimson fire: 2.5 a second (" + first.getHealth() + ")");
					helper.assertTrue(spark.burns(near) && !spark.burns(far), "It leapt to the near one, not the far");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_fire_crimsonlotusblooms")
	public void crimsonLotusBlooms(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 16, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.FIRE_CORE_FORMATION, centre);
			BlockPos spot = centre.south(6).below();
			Vec3 heart = Vec3.atBottomCenterOf(spot.above());
			Husk atHeart = husk(level, heart.add(0.5, 0, 0), 40, spawned);
			Husk petal = husk(level, heart.add(6, 0, 0), 40, spawned);
			HeavenSpellTesting.lookAt(p, Vec3.atCenterOf(spot).add(0, 0.5, 0));
			boolean cast = HeavenSpellTesting.cast(p, Spell.CRIMSON_LOTUS_BLOOMS);
			helper.runAfterDelay(42, () -> {
				try {
					helper.assertTrue(cast && atHeart.getHealth() == 20, "The heart: 20 (" + cast + ", " + atHeart.getHealth() + ")");
					helper.assertTrue(petal.getHealth() == 26 && petal.isOnFire(), "A petal: 14, and burning (" + petal.getHealth() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_fire_ashsearingtalonbarshealing")
	public void ashSearingTalonBarsHealing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.FIRE_NASCENT_SOUL, centre);
			Husk husk = husk(level, Vec3.atBottomCenterOf(centre.south(6)), 100, spawned);
			HeavenSpellTesting.lookAt(p, HeavenSpells.middle(husk));
			boolean cast = HeavenSpellTesting.cast(p, Spell.ASH_SEARING_TALON);
			helper.runAfterDelay(2, () -> {
				try {
					helper.assertTrue(cast && husk.getHealth() == 64 && husk.isOnFire(), "36 harm, burning (" + husk.getHealth() + ")");
					husk.heal(10);
					helper.assertTrue(husk.getHealth() == 64, "The wound won't heal (" + husk.getHealth() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_fire_vermilionplungestrikeswhereitlands")
	public void vermilionPlungeStrikesWhereItLands(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 6);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.FIRE_NASCENT_SOUL, centre);
			BlockPos roof = BlockPos.containing(p.getX(), p.getEyeY() + 1, p.getZ());
			level.setBlock(roof, Blocks.STONE.defaultBlockState(), 2);
			boolean roofed = HeavenSpellTesting.cast(p, Spell.VERMILION_BIRD_PLUNGE);
			level.setBlock(roof, Blocks.AIR.defaultBlockState(), 2);
			Husk husk = husk(level, Vec3.atBottomCenterOf(centre.south(5)), 40, spawned);
			boolean cast = HeavenSpellTesting.cast(p, Spell.VERMILION_BIRD_PLUNGE);
			double soar = p.getDeltaMovement().y;
			helper.runAfterDelay(34, () -> {
				try {
					helper.assertTrue(!roofed, "No open sky: not cast");
					helper.assertTrue(cast && soar > 1.8, "Soared (" + soar + ")");
					helper.assertTrue(husk.getHealth() == 18, "Struck where it landed: 22 (" + husk.getHealth() + ")");
					helper.assertTrue(HeavenSpells.lingering(p.getUUID(), FireSpells.Plunge.class).isEmpty(), "The plunge is over");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_fire_lightoffirstdawncleanses")
	public void lightOfFirstDawnCleanses(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 6, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.FIRE_HEAVENLY_BEING, centre);
			p.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
			p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
			float hurt = p.getMaxHealth() - 20;
			p.setHealth(hurt);
			p.setRemainingFireTicks(100);
			boolean cast = HeavenSpellTesting.cast(p, Spell.LIGHT_OF_FIRST_DAWN);
			helper.runAfterDelay(1, () -> {
				try {
					helper.assertTrue(cast && !p.hasEffect(MobEffects.POISON) && !p.hasEffect(MobEffects.WEAKNESS) && !p.isOnFire(), "Every ill burned away ("
							+ p.getActiveEffects() + ")");
					helper.assertTrue(HeavenSpellTesting.near(p.getHealth(), hurt + 12), "6 mended for each (" + (p.getHealth() - hurt) + ")");
					p.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
					helper.assertTrue(!p.hasEffect(MobEffects.POISON), "No new ill takes hold");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_fire_noondaycrowsearsthoseitsees")
	public void noondayCrowSearsThoseItSees(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 12, 8);
		BlockPos centre = floor.centre();
		for (int x = 4; x <= 11; x++) for (int z = -2; z <= 2; z++) level.setBlock(centre.offset(x, 3, z), Blocks.STONE.defaultBlockState(), 2);
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.FIRE_HEAVENLY_BEING, centre);
			Husk open = husk(level, Vec3.atBottomCenterOf(centre.south(5)), 40, spawned);
			Husk covered = husk(level, Vec3.atBottomCenterOf(centre.east(8)), 40, spawned);
			boolean cast = HeavenSpellTesting.cast(p, Spell.NOONDAY_GOLDEN_CROW);
			helper.runAfterDelay(23, () -> {
				try {
					helper.assertTrue(cast && open.getHealth() == 34 && open.hasEffect(MobEffects.GLOWING), "In the sun's sight: 6 to the undead, and glowing ("
							+ open.getHealth() + ")");
					helper.assertTrue(covered.getHealth() == 40, "Under cover: untouched (" + covered.getHealth() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_fire_firestormshunteachfoe", timeoutTicks = 200)
	public void firestormsHuntEachFoe(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 14, 9);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.FIRE_FOUR_AXIS, centre);
			Husk a = husk(level, Vec3.atBottomCenterOf(centre.south(8)), 100, spawned);
			Husk b = husk(level, Vec3.atBottomCenterOf(centre.east(8)), 100, spawned);
			Husk c = husk(level, Vec3.atBottomCenterOf(centre.west(8)), 100, spawned);
			boolean cast = HeavenSpellTesting.cast(p, Spell.HEAVEN_SCORCHING_FIRESTORMS);
			FireSpells.Firestorms storms = HeavenSpells.lingering(p.getUUID(), FireSpells.Firestorms.class).get(0);
			List<LivingEntity> hunted = storms.quarries();
			helper.runAfterDelay(50, () -> {
				try {
					helper.assertTrue(cast && new HashSet<>(hunted).equals(java.util.Set.of(a, b, c)), "Each storm hunts a foe of its own (" + hunted + ")");
					helper.assertTrue(a.getHealth() < 100 && b.getHealth() < 100 && c.getHealth() < 100, "...and has caught it (" + a.getHealth() + ", "
							+ b.getHealth() + ", " + c.getHealth() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_fire_undyingphoenixrises")
	public void undyingPhoenixRises(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.FIRE_FOUR_AXIS, centre);
			Husk husk = husk(level, Vec3.atBottomCenterOf(centre.south(5)), 100, spawned);
			boolean cast = HeavenSpellTesting.cast(p, Spell.UNDYING_PHOENIX);
			p.hurt(p.damageSources().mobAttack(husk), p.getMaxHealth() * 4);
			FireSpells.Phoenix phoenix = HeavenSpells.lingering(p.getUUID(), FireSpells.Phoenix.class).get(0);
			boolean ashen = phoenix.ashen() && p.isAlive();
			float inAsh = HeavenSpells.incoming(p, p.damageSources().mobAttack(husk), 10);
			helper.runAfterDelay(44, () -> {
				try {
					helper.assertTrue(cast && ashen && inAsh == 0, "Burned to ash, not dead, and unharmed there (" + ashen + ", " + inAsh + ")");
					helper.assertTrue(HeavenSpellTesting.near(p.getHealth(), p.getMaxHealth() * 0.5f) && p.hasEffect(MobEffects.DAMAGE_BOOST),
							"Risen with half its health and the phoenix's strength (" + p.getHealth() + ", " + p.getActiveEffects() + ")");
					helper.assertTrue(husk.getHealth() == 20, "The rising's fire: 80 (" + husk.getHealth() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}
}
