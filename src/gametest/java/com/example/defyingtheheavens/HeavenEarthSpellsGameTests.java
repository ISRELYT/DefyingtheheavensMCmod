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
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The earth methods' twelve spells (EarthSpells), each doing what it says, each test alone in its batch on a floor of its own
 * (HeavenSpellTesting). Husks stand in for foes. Harm that reaches the caster is lessened by its own earth method's passive.
 */
public class HeavenEarthSpellsGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_earth_rollingmillstonerunsover")
	public void rollingMillstoneRunsOver(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 10, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.EARTH_QI_REFINING, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(4)), 20, spawned);
			HeavenSpellTesting.lookAt(p, p.getEyePosition().add(0, 0, 10));
			boolean cast = HeavenSpellTesting.cast(p, Spell.ROLLING_MILLSTONE);
			helper.runAfterDelay(9, () -> {
				try {
					EarthSpells.Millstone stone = HeavenSpells.lingering(p.getUUID(), EarthSpells.Millstone.class).get(0);
					helper.assertTrue(cast && husk.getHealth() == 16 && stone.struck.contains(husk), "Run over: 4 harm (" + cast + ", " + husk.getHealth() + ")");
					helper.assertTrue(stone.at().z > centre.getZ() + 4, "Still rolling on (" + stone.at() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_earth_rammedrampartrisesandcrumbles")
	public void rammedRampartRisesAndCrumbles(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 6);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.EARTH_QI_REFINING, centre);
			HeavenSpellTesting.lookAt(p, p.getEyePosition().add(0, 0, 10));
			boolean cast = HeavenSpellTesting.cast(p, Spell.RAMMED_EARTH_RAMPART);
			helper.runAfterDelay(6, () -> {
				try {
					EarthSpells.Rampart rampart = HeavenSpells.lingering(p.getUUID(), EarthSpells.Rampart.class).get(0);
					List<BlockPos> raised = List.copyOf(rampart.raised());
					helper.assertTrue(cast && raised.size() == 15, "Five wide, three high (" + raised.size() + ")");
					helper.assertTrue(raised.stream().allMatch(pos -> level.getBlockState(pos).is(Blocks.PACKED_MUD) && pos.getZ() == centre.getZ() + 3),
							"Of rammed earth, three ahead");
					HeavenSpells.endLingering(p.getUUID(), EarthSpells.Rampart.class);
					helper.assertTrue(raised.stream().allMatch(pos -> level.getBlockState(pos).isAir()), "Crumbled away to nothing");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_earth_stonescalesbreakblowbyblow")
	public void stoneScalesBreakBlowByBlow(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 6, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.EARTH_FOUNDATION_BUILDING, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(2)), 20, spawned);
			boolean cast = HeavenSpellTesting.cast(p, Spell.STONE_SCALE_ARMOUR);
			try {
				float light = HeavenSpells.incoming(p, p.damageSources().mobAttack(husk), 8);
				float fall = HeavenSpells.incoming(p, p.damageSources().fall(), 8);
				float heavy = HeavenSpells.incoming(p, p.damageSources().mobAttack(husk), 15);
				float third = HeavenSpells.incoming(p, p.damageSources().mobAttack(husk), 6);
				float bare = HeavenSpells.incoming(p, p.damageSources().mobAttack(husk), 6);
				helper.assertTrue(cast && light == 0 && heavy == 5 && third == 0, "Each scale stops a light blow or lessens a heavy one by 10 (" + light + ", "
						+ heavy + ", " + third + ")");
				helper.assertTrue(fall > 0 && bare > 0, "A fall is no blow; the fourth blow lands (" + fall + ", " + bare + ")");
			} finally {
				HeavenSpellTesting.cleanUp(p, spawned, floor);
			}
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_earth_swallowingsandssink")
	public void swallowingSandsSink(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 10, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.EARTH_FOUNDATION_BUILDING, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(7)), 40, spawned);
			HeavenSpellTesting.lookAt(p, Vec3.atCenterOf(centre.south(6).below()).add(0, 0.5, 0));
			boolean cast = HeavenSpellTesting.cast(p, Spell.SWALLOWING_SANDS);
			helper.runAfterDelay(42, () -> {
				try {
					EarthSpells.Sands sands = HeavenSpells.lingering(p.getUUID(), EarthSpells.Sands.class).get(0);
					MobEffectInstance slow = husk.getEffect(MobEffects.MOVEMENT_SLOWDOWN), jump = husk.getEffect(MobEffects.JUMP);
					helper.assertTrue(cast && sands.depth(husk) == 2 && slow != null && slow.getAmplifier() == 1, "Sunk two deep, Slowness II (" + sands.depth(husk)
							+ ", " + slow + ")");
					helper.assertTrue(jump != null && jump.getAmplifier() < 0, "No jumping out (" + jump + ")");
					helper.assertTrue(husk.getHealth() == 38.5f, "Half a point a depth each second (" + husk.getHealth() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_earth_fissureeruptsuntilawall")
	public void fissureEruptsUntilAWall(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 18, 4);
		BlockPos centre = floor.centre();
		for (int y = 0; y < 2; y++) for (int x = -2; x <= 2; x++) level.setBlock(centre.offset(x, y, 12), Blocks.STONE.defaultBlockState(), 2);
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.EARTH_CORE_FORMATION, centre);
			Husk on = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(8)), 40, spawned);
			Husk beyond = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(15)), 40, spawned);
			HeavenSpellTesting.lookAt(p, p.getEyePosition().add(0, 0, 10));
			boolean cast = HeavenSpellTesting.cast(p, Spell.EARTH_SPLITTING_FISSURE);
			float waiting = on.getHealth();
			helper.runAfterDelay(23, () -> {
				try {
					helper.assertTrue(cast && waiting == 40, "A moment's wait (" + cast + ", " + waiting + ")");
					helper.assertTrue(on.getHealth() == 18 && on.getDeltaMovement().y > 0.5, "It erupted under the husk: 22, thrown up (" + on.getHealth() + ", "
							+ on.getDeltaMovement() + ")");
					helper.assertTrue(beyond.getHealth() == 40, "The wall stopped it (" + beyond.getHealth() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_earth_mounttaipressesdown")
	public void mountTaiPressesDown(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 10, 10);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.EARTH_CORE_FORMATION, centre);
			Husk grounded = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(6)), 40, spawned);
			grounded.setOnGround(true);
			Husk aloft = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.east(6).above(5)), 40, spawned);
			aloft.setOnGround(false);
			aloft.setNoGravity(true);
			HeavenSpellTesting.lookAt(p, HeavenSpells.middle(grounded));
			boolean first = HeavenSpellTesting.cast(p, Spell.MOUNT_TAI_PRESSES_DOWN);
			HeavenSpellTesting.lookAt(p, HeavenSpells.middle(aloft));
			boolean second = HeavenSpellTesting.cast(p, Spell.MOUNT_TAI_PRESSES_DOWN);
			helper.runAfterDelay(2, () -> {
				try {
					MobEffectInstance slow = grounded.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
					helper.assertTrue(first && second && grounded.getHealth() == 22 && slow != null && slow.getAmplifier() == 2, "Crushed where it stood: 18, Slowness III ("
							+ grounded.getHealth() + ", " + slow + ")");
					helper.assertTrue(aloft.getDeltaMovement().y < -0.1 && aloft.getHealth() == 40, "The one aloft dragged down, not yet crushed ("
							+ aloft.getDeltaMovement() + ", " + aloft.getHealth() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_earth_earthdragonstrikesfrombelow")
	public void earthDragonStrikesFromBelow(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 10, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.EARTH_NASCENT_SOUL, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(6)), 60, spawned);
			HeavenSpellTesting.lookAt(p, HeavenSpells.middle(husk));
			boolean cast = HeavenSpellTesting.cast(p, Spell.EARTH_DRAGON_TURNS_OVER);
			helper.runAfterDelay(25, () -> {
				try {
					helper.assertTrue(cast && husk.getHealth() == 20, "Struck from below: 40 (" + cast + ", " + husk.getHealth() + ")");
					helper.assertTrue(husk.hasEffect(MobEffects.CONFUSION) && husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "...and left reeling");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_earth_earthveinguardianbites")
	public void earthVeinGuardianBites(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 12, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.EARTH_NASCENT_SOUL, centre);
			BlockPos spot = centre.south(4).below();
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(spot.above().east(6)), 40, spawned);
			HeavenSpellTesting.lookAt(p, Vec3.atCenterOf(spot).add(0, 0.5, 0));
			boolean cast = HeavenSpellTesting.cast(p, Spell.EARTH_VEIN_GUARDIAN);
			helper.runAfterDelay(43, () -> {
				try {
					helper.assertTrue(cast && husk.getHealth() == 16, "Bitten: 24 (" + cast + ", " + husk.getHealth() + ")");
					helper.assertTrue(husk.getDeltaMovement().x < -0.3, "...and dragged toward the head (" + husk.getDeltaMovement() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_earth_bedrockbearstheblows")
	public void bedrockBearsTheBlows(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 6, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.EARTH_HEAVENLY_BEING, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(2)), 20, spawned);
			boolean cast = HeavenSpellTesting.cast(p, Spell.BEDROCK_BEARS_THE_BLOWS);
			float landed = HeavenSpells.incoming(p, p.damageSources().mobAttack(husk), 10);
			EarthSpells.Bedrock bedrock = HeavenSpells.lingering(p.getUUID(), EarthSpells.Bedrock.class).get(0);
			double owed = bedrock.owed();
			// Six tenths of the hurt after the caster's armour (a Four Axis body bears some).
			double expected = 0.6 * net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(10, p.getArmorValue(),
					(float) p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS));
			float before = p.getHealth();
			helper.runAfterDelay(22, () -> {
				try {
					helper.assertTrue(cast && landed == 0 && HeavenSpellTesting.near(owed, expected), "Nothing lands at once; six tenths owed (" + landed + ", " + owed + ", " + expected + ")");
					float lost = before - p.getHealth();
					helper.assertTrue(lost > expected / 4 * 0.5 && lost <= expected / 4 + 0.01 && HeavenSpellTesting.near(bedrock.owed(), expected * 0.75), "A quarter of it paid after a second (" + lost
							+ ", " + bedrock.owed() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_earth_shouldertheburdens")
	public void shoulderTheBurdens(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.EARTH_HEAVENLY_BEING, centre);
			Wolf wolf = HeavenSpellTesting.idle(level, EntityType.WOLF, Vec3.atBottomCenterOf(centre.east(4)), 20, spawned);
			wolf.tame(p);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(4)), 20, spawned);
			boolean cast = HeavenSpellTesting.cast(p, Spell.SHOULDER_THE_MYRIAD_BURDENS);
			float kept = HeavenSpells.incoming(wolf, wolf.damageSources().mobAttack(husk), 10);
			float before = p.getHealth();
			helper.runAfterDelay(2, () -> {
				try {
					float lost = before - p.getHealth();
					helper.assertTrue(cast && kept == 5, "The wolf takes half (" + kept + ")");
					helper.assertTrue(lost > 2.0f && lost <= 2.5f, "The caster bears a quarter (less its own stone) (" + lost + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_earth_ridingdunescarriesandtramples")
	public void ridingDunesCarriesAndTramples(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.EARTH_FOUR_AXIS, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, p.position().add(0, 0, 1.2), 100, spawned);
			HeavenSpellTesting.lookAt(p, p.getEyePosition().add(0, 0, 10));
			boolean cast = HeavenSpellTesting.cast(p, Spell.RIDING_TEN_THOUSAND_DUNES);
			helper.runAfterDelay(2, () -> {
				try {
					Vec3 v = p.getDeltaMovement();
					helper.assertTrue(cast && HeavenSpellTesting.near(v.z, 1.25) && Math.abs(v.x) < 0.05, "Carried along the look at 25 blocks a second (" + v + ")");
					MobEffectInstance slow = husk.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
					helper.assertTrue(husk.getHealth() == 50 && slow != null && slow.getAmplifier() == 3, "Trampled: 50, half-buried (" + husk.getHealth() + ", "
							+ slow + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_earth_fallofbuzhou", timeoutTicks = 150)
	public void fallOfBuzhou(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 26, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.EARTH_FOUR_AXIS, centre);
			BlockPos spot = centre.south(6).below();
			Husk under = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(spot.above().east(4)), 250, spawned);
			Husk quake = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(spot.above().east(18)), 40, spawned);
			HeavenSpellTesting.lookAt(p, Vec3.atCenterOf(spot).add(0, 0.5, 0));
			boolean cast = HeavenSpellTesting.cast(p, Spell.FALL_OF_BUZHOU);
			float casterBefore = p.getHealth();
			helper.runAfterDelay(63, () -> {
				try {
					MobEffectInstance slow = under.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
					helper.assertTrue(cast && under.getHealth() == 200 && slow != null && slow.getAmplifier() == 3, "In the shadow: a fifth of 250 (at least 50), half-buried ("
							+ under.getHealth() + ", " + slow + ")");
					helper.assertTrue(quake.getHealth() == 40 && quake.getDeltaMovement().x > 0.5, "Further out: thrown outward, unharmed (" + quake.getHealth() + ", "
							+ quake.getDeltaMovement() + ")");
					helper.assertTrue(p.getHealth() == casterBefore, "The caster spared");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}
}
