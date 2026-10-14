package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The wood methods' twelve spells (WoodSpells), each doing what it says, each test alone in its batch on a floor of its own
 * (HeavenSpellTesting). Husks stand in for foes: they don't burn in the sun.
 */
public class HeavenWoodSpellsGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_wood_graspingrootsharmsandbinds")
	public void graspingRootsHarmsAndBinds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 6);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WOOD_QI_REFINING, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(5)), 20, spawned);
			husk.setOnGround(true); // standing (the test's creatures aren't always ticked to find their feet)
			HeavenSpellTesting.lookAt(p, HeavenSpells.middle(husk));
			boolean cast = HeavenSpellTesting.cast(p, Spell.GRASPING_ROOTS);
			float after = husk.getHealth();
			helper.runAfterDelay(4, () -> {
				try {
					helper.assertTrue(cast && HeavenSpellTesting.near(after, 15), "Cast at the husk: 5 harm (" + cast + ", " + after + ")");
					MobEffectInstance slow = husk.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
					helper.assertTrue(slow != null && slow.getAmplifier() == 5, "Bound where it stands (" + husk.getActiveEffects() + ", binds "
							+ HeavenSpells.lingering(p.getUUID(), HeavenSpells.Lingering.class).size() + ", on ground " + husk.onGround() + ", at " + husk.position() + ")");
					HeavenSpellTesting.lookAt(p, Vec3.atCenterOf(centre.east(6).above(4)));
					helper.assertTrue(!HeavenSpellTesting.cast(p, Spell.GRASPING_ROOTS), "No being looked at: not cast, nothing paid");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_wood_bramblemantlesoftensblowsandtearsthestriker")
	public void brambleMantleSoftensBlowsAndTearsTheStriker(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 6, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WOOD_QI_REFINING, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(2)), 20, spawned);
			boolean cast = HeavenSpellTesting.cast(p, Spell.BRAMBLE_MANTLE);
			DamageSource blow = p.damageSources().mobAttack(husk);
			float kept = HeavenSpells.incoming(p, blow, 10);
			float arrow = HeavenSpells.incoming(p, p.damageSources().arrow(null, husk), 10);
			helper.runAfterDelay(2, () -> {
				try {
					helper.assertTrue(cast && HeavenSpellTesting.near(kept, 8.5) && arrow == 10, "A melee blow at 85%, an arrow untouched (" + kept + ", " + arrow + ")");
					helper.assertTrue(HeavenSpellTesting.near(20 - husk.getHealth(), 6.25), "The striker torn for 2 + half the blow (" + (20 - husk.getHealth()) + ")");
					helper.assertTrue(husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "...and snagged");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_wood_wisteriaascenthaulsthecastertotheblock")
	public void wisteriaAscentHaulsTheCasterToTheBlock(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 8);
		BlockPos centre = floor.centre();
		for (int y = 0; y < 6; y++) for (int x = -2; x <= 2; x++) level.setBlock(centre.offset(x, y, 8), Blocks.STONE.defaultBlockState(), 2);
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WOOD_FOUNDATION_BUILDING, centre);
			HeavenSpellTesting.lookAt(p, Vec3.atCenterOf(centre.offset(0, 4, 8)));
			boolean cast = HeavenSpellTesting.cast(p, Spell.WISTERIA_ASCENT);
			helper.runAfterDelay(1, () -> {
				try {
					Vec3 v = p.getDeltaMovement();
					helper.assertTrue(cast && v.z > 0.3 && v.y > 0, "Hauled toward the wall and up (" + cast + ", " + v + ")");
					HeavenSpellTesting.lookAt(p, p.getEyePosition().add(0, 30, 0.1));
					helper.assertTrue(!HeavenSpellTesting.cast(p, Spell.WISTERIA_ASCENT), "Nothing looked at in the sky: not cast");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_wood_twinserpentvinesbindtwotogether")
	public void twinSerpentVinesBindTwoTogether(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 10, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WOOD_FOUNDATION_BUILDING, centre);
			Husk first = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(5)), 40, spawned);
			Husk second = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(5).east(3)), 40, spawned);
			HeavenSpellTesting.lookAt(p, HeavenSpells.middle(first));
			boolean cast = HeavenSpellTesting.cast(p, Spell.TWIN_SERPENT_VINES);
			float a = first.getHealth(), b = second.getHealth();
			second.setPos(second.getX() + 6, second.getY(), second.getZ()); // strays 9 apart
			helper.runAfterDelay(2, () -> {
				try {
					helper.assertTrue(cast && a == 32 && b == 32, "8 harm each (" + cast + ", " + a + ", " + b + ")");
					helper.assertTrue(first.getDeltaMovement().x > 0.05 && second.getDeltaMovement().x < -0.05, "Hauled back together ("
							+ first.getDeltaMovement() + ", " + second.getDeltaMovement() + ")");
					helper.assertTrue(first.getHealth() == 28 && second.getHealth() == 28, "...tearing each for 4 (" + first.getHealth() + ", " + second.getHealth() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_wood_burstingthornpodslieinwait")
	public void burstingThornPodsLieInWait(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 10, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WOOD_CORE_FORMATION, centre);
			HeavenSpellTesting.lookAt(p, Vec3.atCenterOf(centre.south(6).below()));
			boolean cast = HeavenSpellTesting.cast(p, Spell.BURSTING_THORN_PODS);
			List<WoodSpells.Pods> pods = HeavenSpells.lingering(p.getUUID(), WoodSpells.Pods.class);
			int lying = pods.isEmpty() ? 0 : pods.get(0).lying.size();
			Husk stepped = lying == 0 ? null : HeavenSpellTesting.idle(level, EntityType.HUSK,
					new Vec3(pods.get(0).lying.get(0)[0], pods.get(0).lying.get(0)[1], pods.get(0).lying.get(0)[2]), 40, spawned);
			helper.runAfterDelay(2, () -> {
				try {
					helper.assertTrue(cast && lying == 5, "Five pods lie (" + cast + ", " + lying + ")");
					helper.assertTrue(stepped.getHealth() == 22 && stepped.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 2,
							"A foe steps on one: 18 harm, Slowness III (" + stepped.getHealth() + ")");
					helper.assertTrue(pods.get(0).lying.size() == 4, "...and the pod is spent");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120, batch = "heaven_wood_heartwoodrootswardsandlendsitsblow")
	public void heartwoodRootsWardsAndLendsItsBlow(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 6, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WOOD_CORE_FORMATION, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(2)), 60, spawned);
			boolean cast = HeavenSpellTesting.cast(p, Spell.HEARTWOOD_OF_NINE_RINGS);
			helper.runAfterDelay(45, () -> {
				try {
					List<WoodSpells.Heartwood> rooted = HeavenSpells.lingering(p.getUUID(), WoodSpells.Heartwood.class);
					helper.assertTrue(cast && rooted.size() == 1 && rooted.get(0).rings == 2, "Two rings in two seconds (" + cast + ", "
							+ (rooted.isEmpty() ? -1 : rooted.get(0).rings) + ")");
					helper.assertTrue(p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 1, "Not to be knocked back");
					helper.assertTrue(HeavenSpellTesting.near(HeavenSpells.incoming(p, p.damageSources().mobAttack(husk), 10), 9), "Each ring takes 5% off");
					helper.assertTrue(!HeavenSpellTesting.cast(p, Spell.HEARTWOOD_OF_NINE_RINGS) && HeavenSpells.lingering(p.getUUID(), WoodSpells.Heartwood.class).isEmpty(),
							"Cast again: uprooted, nothing paid");
					helper.assertTrue(Spells.cooldownLeft(p, Spell.HEARTWOOD_OF_NINE_RINGS) > 700, "Its cooldown runs from the uprooting");
					HeavenSpells.onMelee(p, husk);
					helper.assertTrue(husk.getHealth() == 54, "The next blow strikes 3 more a ring (" + husk.getHealth() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_wood_pollenputsfoestosleepandthewakingblowstrikesharder")
	public void pollenPutsFoesToSleepAndTheWakingBlowStrikesHarder(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 10, 6);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WOOD_NASCENT_SOUL, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(8)), 40, spawned);
			HeavenSpellTesting.lookAt(p, Vec3.atCenterOf(centre.south(8).below()));
			boolean cast = HeavenSpellTesting.cast(p, Spell.POLLEN_OF_LONG_DREAMS);
			helper.runAfterDelay(3, () -> {
				try {
					List<WoodSpells.Pollen> clouds = HeavenSpells.lingering(p.getUUID(), WoodSpells.Pollen.class);
					helper.assertTrue(cast && clouds.size() == 1 && clouds.get(0).asleep(husk), "The husk sleeps (" + cast + ", " + clouds.size() + ")");
					husk.hurt(level.damageSources().generic(), 4);
					helper.assertTrue(husk.getHealth() == 34 && !clouds.get(0).asleep(husk), "The waking blow strikes half again as hard (" + husk.getHealth() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_wood_thickettearswhatmovesandspareswhatstandsstill")
	public void thicketTearsWhatMovesAndSparesWhatStandsStill(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 12, 6);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WOOD_NASCENT_SOUL, centre.north(11));
			Husk mover = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre), 40, spawned);
			Husk still = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.east(3)), 40, spawned);
			HeavenSpellTesting.lookAt(p, Vec3.atCenterOf(centre.below()));
			boolean cast = HeavenSpellTesting.cast(p, Spell.MYRIAD_THORN_THICKET);
			helper.runAfterDelay(3, () -> mover.setPos(mover.getX() + 2, mover.getY(), mover.getZ()));
			helper.runAfterDelay(13, () -> {
				try {
					helper.assertTrue(cast && mover.getHealth() == 34 && still.getHealth() == 40, "The one that moved 2 blocks torn for 6, the still one spared ("
							+ cast + ", " + mover.getHealth() + ", " + still.getHealth() + ")");
					helper.assertTrue(still.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Both slowed");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_wood_stranglerfigturnsitshostshealingtothecaster")
	public void stranglerFigTurnsItsHostsHealingToTheCaster(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 6);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WOOD_HEAVENLY_BEING, centre);
			IronGolem golem = HeavenSpellTesting.idle(level, EntityType.IRON_GOLEM, Vec3.atBottomCenterOf(centre.south(6)), 100, spawned);
			HeavenSpellTesting.lookAt(p, HeavenSpells.middle(golem));
			try {
				helper.assertTrue(HeavenSpellTesting.cast(p, Spell.STRANGLER_FIG_SEED), "Cast");
				helper.assertTrue(golem.getHealth() == 35 && golem.hasEffect(MobEffects.WEAKNESS), "65 harm, and weakened (" + golem.getHealth() + ")");
				p.setHealth(p.getMaxHealth() - 30);
				float before = p.getHealth();
				golem.heal(10);
				helper.assertTrue(golem.getHealth() == 35 && HeavenSpellTesting.near(p.getHealth() - before, 10), "Its healing heals the caster instead");
			} finally {
				HeavenSpellTesting.cleanUp(p, spawned, floor);
			}
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_wood_ritemendsanditsblessedhealfromeveryhurt")
	public void riteMendsAndItsBlessedHealFromEveryHurt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WOOD_HEAVENLY_BEING, centre);
			Villager villager = HeavenSpellTesting.idle(level, EntityType.VILLAGER, Vec3.atBottomCenterOf(centre.south(4)), 40, spawned);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.north(4)), 40, spawned);
			villager.setHealth(5);
			husk.setHealth(5);
			try {
				helper.assertTrue(HeavenSpellTesting.cast(p, Spell.RITE_OF_RETURNING_SPRING), "Cast");
				helper.assertTrue(villager.getHealth() == 35 && husk.getHealth() == 5, "The villager mended 30, the husk not (" + villager.getHealth() + ", " + husk.getHealth() + ")");
				List<WoodSpells.Rite> rites = HeavenSpells.lingering(p.getUUID(), WoodSpells.Rite.class);
				villager.hurt(level.damageSources().generic(), 10);
				helper.assertTrue(rites.size() == 1 && rites.get(0).streams.size() == 1 && HeavenSpellTesting.near((Float) rites.get(0).streams.get(0)[1], 6),
						"Six tenths of its hurt grows back");
			} finally {
				HeavenSpellTesting.cleanUp(p, spawned, floor);
			}
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_wood_canopycatchesmissiles")
	public void canopyCatchesMissiles(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 6);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WOOD_FOUR_AXIS, centre);
			Husk archer = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(10)), 20, spawned);
			boolean cast = HeavenSpellTesting.cast(p, Spell.HEAVEN_SHADING_CANOPY);
			PlayerCultivation c = CultivationManager.get(p);
			c.setQi(100);
			Arrow arrow = new Arrow(level, archer);
			arrow.setPos(centre.getX() + 0.5, centre.getY() + 1.5, centre.getZ() + 9.5);
			arrow.setDeltaMovement(0, 0, -2);
			level.addFreshEntity(arrow);
			Snowball snowball = new Snowball(level, archer);
			snowball.setPos(centre.getX() + 2.5, centre.getY() + 1.5, centre.getZ() + 9.5);
			snowball.setDeltaMovement(0, 0, -1.5);
			level.addFreshEntity(snowball);
			helper.runAfterDelay(2, () -> {
				try {
					helper.assertTrue(cast && arrow.getDeltaMovement().lengthSqr() < 0.04 && snowball.isRemoved(), "The arrow dropped, the snowball snuffed out ("
							+ cast + ", " + arrow.getDeltaMovement() + ", " + snowball.isRemoved() + ")");
					helper.assertTrue(c.getQi() >= 140, "20 qi back for each (" + c.getQi() + ")");
				} finally {
					arrow.discard();
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 180, batch = "heaven_wood_worldtreemendsalliesanditsrootsstrikefoes")
	public void worldTreeMendsAlliesAndItsRootsStrikeFoes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 12, 6);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WOOD_FOUR_AXIS, centre.north(6));
			Villager villager = HeavenSpellTesting.idle(level, EntityType.VILLAGER, Vec3.atBottomCenterOf(centre.east(3)), 40, spawned);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.west(3)), 200, spawned);
			villager.setHealth(5);
			HeavenSpellTesting.lookAt(p, Vec3.atCenterOf(centre.below()));
			boolean cast = HeavenSpellTesting.cast(p, Spell.WORLD_TREE_RISES);
			float[] mended = new float[1];
			float[] healed = new float[1];
			helper.runAfterDelay(45, () -> {
				mended[0] = villager.getHealth();
				husk.setHealth(150);
				husk.heal(20);
				healed[0] = husk.getHealth();
			});
			helper.runAfterDelay(104, () -> {
				try {
					helper.assertTrue(cast && mended[0] >= 17, "The villager mended 12 a second (" + cast + ", " + mended[0] + ")");
					helper.assertTrue(healed[0] == 150, "A foe beneath it can't heal (" + healed[0] + ")");
					helper.assertTrue(husk.getHealth() == 90, "The roots struck it for 60 (" + husk.getHealth() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}
}
