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
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The water methods' twelve spells (WaterSpells), each doing what it says, each test alone in its batch on a floor of its own
 * (HeavenSpellTesting). Husks stand in for foes: they don't burn in the sun.
 */
public class HeavenWaterSpellsGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_water_hoarfrostneedlestacksandfreezes", timeoutTicks = 200)
	public void hoarfrostNeedleStacksAndFreezes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WATER_QI_REFINING, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(6)), 40, spawned);
			HeavenSpellTesting.lookAt(p, HeavenSpells.middle(husk));
			boolean first = HeavenSpellTesting.cast(p, Spell.HOARFROST_NEEDLE);
			helper.runAfterDelay(12, () -> {
				try {
					MobEffectInstance slow = husk.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
					helper.assertTrue(first && husk.getHealth() == 34 && WaterSpells.frost(husk) == 1 && slow != null && slow.getAmplifier() == 0,
							"One needle: 6 harm, a stack, Slowness I (" + husk.getHealth() + ", " + WaterSpells.frost(husk) + ", " + slow + ")");
					HeavenSpellTesting.cast(p, Spell.HOARFROST_NEEDLE);
				} catch (RuntimeException e) {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
					throw e;
				}
				helper.runAfterDelay(12, () -> {
					try {
						MobEffectInstance slow = husk.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
						helper.assertTrue(husk.getHealth() == 28 && WaterSpells.frost(husk) == 2 && slow != null && slow.getAmplifier() == 1,
								"Two: Slowness II (" + husk.getHealth() + ", " + WaterSpells.frost(husk) + ", " + slow + ")");
						HeavenSpellTesting.cast(p, Spell.HOARFROST_NEEDLE);
					} catch (RuntimeException e) {
						HeavenSpellTesting.cleanUp(p, spawned, floor);
						throw e;
					}
					helper.runAfterDelay(12, () -> {
						try {
							MobEffectInstance slow = husk.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
							boolean frozen = HeavenSpells.lingering(p.getUUID(), WaterSpells.Frozen.class).stream().anyMatch(f -> f.target == husk);
							helper.assertTrue(husk.getHealth() == 22 && frozen && WaterSpells.frost(husk) == 0 && slow != null && slow.getAmplifier() == 6,
									"Three: frozen solid, the stacks spent (" + husk.getHealth() + ", " + frozen + ", " + slow + ")");
						} finally {
							HeavenSpellTesting.cleanUp(p, spawned, floor);
						}
						helper.succeed();
					});
				});
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_water_morningmisthidesthecaster")
	public void morningMistHidesTheCaster(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WATER_QI_REFINING, centre);
			Husk far = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(6)), 20, spawned);
			Husk near = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.east(2)), 20, spawned);
			far.setTarget(p);
			near.setTarget(p);
			boolean cast = HeavenSpellTesting.cast(p, Spell.MORNING_MIST_SHROUD);
			helper.runAfterDelay(2, () -> {
				try {
					helper.assertTrue(cast && p.hasEffect(MobEffects.INVISIBILITY) && p.hasEffect(MobEffects.MOVEMENT_SPEED), "Unseen and swift in the mist ("
							+ p.getActiveEffects() + ")");
					helper.assertTrue(far.getTarget() == null && near.getTarget() == p, "The far husk lost track; the near one kept it (" + far.getTarget()
							+ ", " + near.getTarget() + ")");
					HeavenSpellTesting.lookAt(p, HeavenSpells.middle(far));
					HeavenSpellTesting.cast(p, Spell.HOARFROST_NEEDLE);
					WaterSpells.Mist mist = HeavenSpells.lingering(p.getUUID(), WaterSpells.Mist.class).get(0);
					helper.assertTrue(!mist.hiding() && !p.hasEffect(MobEffects.INVISIBILITY), "Casting gives it away (" + p.getActiveEffects() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_water_returningcurrentcarriesback")
	public void returningCurrentCarriesBack(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WATER_FOUNDATION_BUILDING, centre);
			Vec3 mark = p.position();
			float full = p.getHealth();
			boolean cast = HeavenSpellTesting.cast(p, Spell.RETURNING_CURRENT);
			helper.runAfterDelay(2, () -> {
				try {
					p.teleportTo(mark.x + 4, mark.y, mark.z);
					p.setHealth(full - 10);
					p.setRemainingFireTicks(100);
					boolean paid = HeavenSpellTesting.cast(p, Spell.RETURNING_CURRENT);
					helper.assertTrue(cast && !paid, "Cast, then cast again for nothing (" + cast + ", " + paid + ")");
					helper.assertTrue(p.position().distanceTo(mark) < 0.1, "Carried back to the spring (" + p.position() + ", " + mark + ")");
					helper.assertTrue(HeavenSpellTesting.near(p.getHealth(), full - 5) && !p.isOnFire(), "Half the lost health given back, the fire out ("
							+ p.getHealth() + ", " + p.isOnFire() + ")");
					helper.assertTrue(Spells.cooldownLeft(p, Spell.RETURNING_CURRENT) > 200, "Its cooldown runs from the return");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_water_renewingrainmendsandhastens")
	public void renewingRainMendsAndHastens(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 6);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WATER_FOUNDATION_BUILDING, centre);
			float hurt = p.getMaxHealth() - 10;
			p.setHealth(hurt);
			boolean cast = HeavenSpellTesting.cast(p, Spell.RENEWING_MOUNTAIN_RAIN);
			Spells.setCooldown(p, Spell.RETURNING_CURRENT, 200);
			p.setRemainingFireTicks(100);
			helper.runAfterDelay(21, () -> {
				try {
					int left = Spells.cooldownLeft(p, Spell.RETURNING_CURRENT);
					helper.assertTrue(cast && left <= 162 && left >= 154, "Cooldowns run down twice as fast in the rain (" + left + ")");
					helper.assertTrue(p.getHealth() >= hurt + 1.9f && !p.isOnFire(), "Mended each second, the fire out (" + (p.getHealth() - hurt) + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_water_coilinglashthrowsbehind")
	public void coilingLashThrowsBehind(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 10, 8);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WATER_CORE_FORMATION, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(6)), 40, spawned);
			HeavenSpellTesting.lookAt(p, HeavenSpells.middle(husk));
			boolean cast = HeavenSpellTesting.cast(p, Spell.COILING_STREAM_LASH);
			helper.runAfterDelay(2, () -> {
				Vec3 v = husk.getDeltaMovement();
				try {
					helper.assertTrue(cast && v.y > 0 && v.z < 0, "Swung up and back over the caster (" + cast + ", " + v + ")");
				} catch (RuntimeException e) {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
					throw e;
				}
				helper.runAfterDelay(16, () -> {
					try {
						MobEffectInstance slow = husk.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
						helper.assertTrue(husk.getHealth() == 18 && slow != null && slow.getAmplifier() == 2, "Dashed down: 22 harm, Slowness III ("
								+ husk.getHealth() + ", " + slow + ")");
						HeavenSpellTesting.lookAt(p, p.getEyePosition().add(0, 30, 0.1));
						helper.assertTrue(!HeavenSpellTesting.cast(p, Spell.COILING_STREAM_LASH), "Nothing looked at: not cast");
					} finally {
						HeavenSpellTesting.cleanUp(p, spawned, floor);
					}
					helper.succeed();
				});
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_water_whirlpoolsweepsandcollapses", timeoutTicks = 200)
	public void whirlpoolSweepsAndCollapses(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 12, 6);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WATER_CORE_FORMATION, centre);
			BlockPos spot = centre.south(8).below();
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(spot.above().east(2)), 40, spawned);
			HeavenSpellTesting.lookAt(p, Vec3.atCenterOf(spot).add(0, 0.5, 0));
			boolean cast = HeavenSpellTesting.cast(p, Spell.NINE_BEND_WHIRLPOOL);
			helper.runAfterDelay(22, () -> {
				Vec3 v = husk.getDeltaMovement();
				try {
					helper.assertTrue(cast && Math.abs(v.z) > 0.2 && husk.getHealth() == 38, "Swept round, 2 harm a second (" + cast + ", " + v + ", "
							+ husk.getHealth() + ")");
				} catch (RuntimeException e) {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
					throw e;
				}
				helper.runAfterDelay(82, () -> {
					try {
						helper.assertTrue(husk.getHealth() == 24 && husk.getDeltaMovement().y > 0.3, "Flung out as it collapsed: 16 in all ("
								+ husk.getHealth() + ", " + husk.getDeltaMovement() + ")");
						HeavenSpellTesting.lookAt(p, p.getEyePosition().add(0, 30, 0.1));
						helper.assertTrue(!HeavenSpellTesting.cast(p, Spell.NINE_BEND_WHIRLPOOL), "No ground looked at: not cast");
					} finally {
						HeavenSpellTesting.cleanUp(p, spawned, floor);
					}
					helper.succeed();
				});
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_water_mooninthewaterswapsplaces")
	public void moonInTheWaterSwapsPlaces(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 10, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WATER_NASCENT_SOUL, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(8)), 20, spawned);
			Vec3 a = p.position(), b = husk.position();
			HeavenSpellTesting.lookAt(p, HeavenSpells.middle(husk));
			boolean cast = HeavenSpellTesting.cast(p, Spell.MOON_IN_THE_WATER);
			helper.runAfterDelay(1, () -> {
				try {
					helper.assertTrue(cast && p.position().distanceTo(b) < 0.1 && husk.position().distanceTo(a) < 0.1, "Changed places (" + p.position()
							+ ", " + husk.position() + ")");
					MobEffectInstance slow = husk.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
					helper.assertTrue(slow != null && slow.getAmplifier() == 1, "...the other left reeling (" + slow + ")");
					HeavenSpellTesting.lookAt(p, p.getEyePosition().add(0, 30, 0.1));
					helper.assertTrue(!HeavenSpellTesting.cast(p, Spell.MOON_IN_THE_WATER), "No reflection answers: not cast");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_water_stilllakemirrorhalvesandreflects")
	public void stillLakeMirrorHalvesAndReflects(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 6, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WATER_NASCENT_SOUL, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(2)), 40, spawned);
			boolean cast = HeavenSpellTesting.cast(p, Spell.STILL_LAKE_MIRROR);
			float kept = HeavenSpells.incoming(p, p.damageSources().mobAttack(husk), 10);
			helper.runAfterDelay(2, () -> {
				try {
					WaterSpells.Mirror mirror = HeavenSpells.lingering(p.getUUID(), WaterSpells.Mirror.class).get(0);
					helper.assertTrue(cast && kept == 5 && mirror.spared(husk) == 5, "Halved, and remembered (" + kept + ", " + mirror.spared(husk) + ")");
					HeavenSpells.endLingering(p.getUUID(), WaterSpells.Mirror.class);
					helper.assertTrue(husk.getHealth() == 30, "Struck by its own reflection for twice the spared harm (" + husk.getHealth() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_water_abyssalprisondrowns", timeoutTicks = 150)
	public void abyssalPrisonDrowns(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 10, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WATER_HEAVENLY_BEING, centre);
			Pig pig = HeavenSpellTesting.idle(level, EntityType.PIG, Vec3.atBottomCenterOf(centre.south(6)), 100, spawned);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.north(6)), 100, spawned);
			HeavenSpellTesting.lookAt(p, HeavenSpells.middle(pig));
			boolean onPig = HeavenSpellTesting.cast(p, Spell.ABYSSAL_WATER_PRISON);
			HeavenSpellTesting.lookAt(p, HeavenSpells.middle(husk));
			boolean onHusk = HeavenSpellTesting.cast(p, Spell.ABYSSAL_WATER_PRISON);
			helper.runAfterDelay(42, () -> {
				try {
					helper.assertTrue(onPig && onHusk, "Cast on both");
					// Its breath taken each tick (it draws a little back on its own tick in between).
					helper.assertTrue(pig.getHealth() == 34 && pig.getAirSupply() <= 4, "The pig: 50, then drowning 8 a second (" + pig.getHealth() + ", "
							+ pig.getAirSupply() + ")");
					MobEffectInstance slow = husk.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
					helper.assertTrue(husk.getHealth() == 50 && slow != null && slow.getAmplifier() == 1, "The husk needs no air: the 50 and the slow alone ("
							+ husk.getHealth() + ", " + slow + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_water_overturnedrivercarriesandcrashes")
	public void overturnedRiverCarriesAndCrashes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 14, 8);
		BlockPos centre = floor.centre();
		for (int y = 0; y < 3; y++) for (int x = -9; x <= 9; x++) level.setBlock(centre.offset(x, y, 12), Blocks.STONE.defaultBlockState(), 2);
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WATER_HEAVENLY_BEING, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(8).east(3)), 60, spawned);
			HeavenSpellTesting.lookAt(p, p.getEyePosition().add(0, 0, 10));
			boolean cast = HeavenSpellTesting.cast(p, Spell.OVERTURNED_HEAVENLY_RIVER);
			helper.runAfterDelay(25, () -> {
				try {
					MobEffectInstance slow = husk.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
					helper.assertTrue(cast && husk.getHealth() == 20, "Caught for 8, then crashed on at the wall for 32 (" + cast + ", " + husk.getHealth() + ")");
					helper.assertTrue(slow != null && slow.getAmplifier() == 1, "...and slowed (" + slow + ")");
					helper.assertTrue(HeavenSpells.lingering(p.getUUID(), WaterSpells.River.class).isEmpty(), "The river is spent");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_water_tenthousandreflectionsturnfoes")
	public void tenThousandReflectionsTurnFoes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 10, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WATER_FOUR_AXIS, centre);
			Husk a = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(4)), 20, spawned);
			Husk b = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(6).east(2)), 20, spawned);
			a.setTarget(p);
			b.setTarget(p);
			boolean cast = HeavenSpellTesting.cast(p, Spell.TEN_THOUSAND_REFLECTIONS);
			helper.runAfterDelay(2, () -> {
				try {
					helper.assertTrue(cast && a.getTarget() == b && b.getTarget() == a, "Each set on the other (" + a.getTarget() + ", " + b.getTarget() + ")");
					float blow = HeavenSpells.incoming(a, p.damageSources().mobAttack(b), 10);
					helper.assertTrue(blow == 15, "Harder on one another (" + blow + ")");
					HeavenSpells.endLingering(p.getUUID(), WaterSpells.Reflections.class);
					helper.assertTrue(a.getTarget() == null && b.getTarget() == null, "Bewildered when it ends");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_water_eternalwinterfreezesandshatters", timeoutTicks = 300)
	public void eternalWinterFreezesAndShatters(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 10, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.WATER_FOUR_AXIS, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(5)), 200, spawned);
			boolean cast = HeavenSpellTesting.cast(p, Spell.ETERNAL_WINTER_DESCENDS);
			helper.runAfterDelay(24, () -> {
				try {
					WaterSpells.Winter winter = HeavenSpells.lingering(p.getUUID(), WaterSpells.Winter.class).get(0);
					helper.assertTrue(cast && winter.held(husk) == 0, "Frozen still (" + cast + ", " + winter.held(husk) + ")");
					float through = HeavenSpells.incoming(husk, p.damageSources().playerAttack(p), 20);
					helper.assertTrue(through == 0 && winter.held(husk) == 20 && husk.getHealth() == 200, "A blow held in the ice (" + through + ", "
							+ winter.held(husk) + ", " + husk.getHealth() + ")");
				} catch (RuntimeException e) {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
					throw e;
				}
				helper.runAfterDelay(124, () -> {
					try {
						helper.assertTrue(husk.getHealth() == 110, "Shattered: 60 and one and a half times the 20 held (" + husk.getHealth() + ")");
					} finally {
						HeavenSpellTesting.cleanUp(p, spawned, floor);
					}
					helper.succeed();
				});
			});
		});
	}
}
