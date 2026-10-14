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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The metal methods' twelve spells (MetalSpells), each doing what it says, each test alone in its batch on a floor of its own
 * (HeavenSpellTesting). Husks stand in for foes; the spells' qi harm passes armour, so the numbers are whole.
 */
public class HeavenMetalSpellsGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_metal_swallowthreadpiercesbothways")
	public void swallowThreadPiercesBothWays(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 16, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.METAL_QI_REFINING, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(6)), 40, spawned);
			HeavenSpellTesting.lookAt(p, HeavenSpells.middle(husk));
			boolean cast = HeavenSpellTesting.cast(p, Spell.SWALLOW_RETURN_THREAD);
			helper.runAfterDelay(9, () -> {
				float out = husk.getHealth();
				helper.runAfterDelay(25, () -> {
					try {
						helper.assertTrue(cast && out == 36, "Pierced on the way out: 4 (" + cast + ", " + out + ")");
						helper.assertTrue(husk.getHealth() == 32, "...and again on the way home (" + husk.getHealth() + ")");
						helper.assertTrue(HeavenSpells.lingering(p.getUUID(), MetalSpells.ReturnThread.class).isEmpty(), "Caught again");
					} finally {
						HeavenSpellTesting.cleanUp(p, spawned, floor);
					}
					helper.succeed();
				});
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_metal_ironbellcoverblunts")
	public void ironBellCoverBlunts(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 6, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.METAL_QI_REFINING, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(2)), 20, spawned);
			boolean cast = HeavenSpellTesting.cast(p, Spell.IRON_BELL_COVER);
			try {
				float blow = HeavenSpells.incoming(p, p.damageSources().mobAttack(husk), 5);
				float small = HeavenSpells.incoming(p, p.damageSources().mobAttack(husk), 1.5f);
				float fall = HeavenSpells.incoming(p, p.damageSources().fall(), 5);
				helper.assertTrue(cast && blow == 3 && small == 0, "Each blow two the less, a small one stopped (" + blow + ", " + small + ")");
				helper.assertTrue(fall > 3, "A fall is no being's blow (" + fall + ")");
			} finally {
				HeavenSpellTesting.cleanUp(p, spawned, floor);
			}
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_metal_lodestonebrandbendsmissiles")
	public void lodestoneBrandBendsMissiles(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 10, 6);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.METAL_FOUNDATION_BUILDING, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(6)), 40, spawned);
			HeavenSpellTesting.lookAt(p, HeavenSpells.middle(husk));
			boolean cast = HeavenSpellTesting.cast(p, Spell.LODESTONE_BRAND);
			Arrow arrow = new Arrow(level, husk.getX() + 3, husk.getY() + 1.5, husk.getZ());
			arrow.setDeltaMovement(1, 0, 0);
			level.addFreshEntity(arrow);
			helper.runAfterDelay(2, () -> {
				try {
					helper.assertTrue(cast && husk.getHealth() == 30, "Branded: 10 (" + cast + ", " + husk.getHealth() + ")");
					helper.assertTrue(arrow.isRemoved() || arrow.getDeltaMovement().x < 0, "A passing arrow bends toward the brand (" + arrow.getDeltaMovement() + ")");
				} finally {
					arrow.discard();
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_metal_hundredweaponskneel")
	public void hundredWeaponsKneel(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 10, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.METAL_FOUNDATION_BUILDING, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(5)), 40, spawned);
			husk.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
			boolean cast = HeavenSpellTesting.cast(p, Spell.HUNDRED_WEAPONS_KNEEL);
			helper.runAfterDelay(2, () -> {
				List<ItemEntity> dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(husk.blockPosition()).inflate(4), i -> i.getItem().is(Items.IRON_SWORD));
				try {
					helper.assertTrue(cast && husk.getHealth() == 32, "Wrenched: 8 (" + cast + ", " + husk.getHealth() + ")");
					helper.assertTrue(husk.getMainHandItem().isEmpty() && dropped.size() == 1, "The sword it held (it had picked it up: it keeps it for good) falls ("
							+ husk.getMainHandItem() + ", " + dropped.size() + ")");
					helper.assertTrue(!husk.canPickUpLoot(), "It can't pick anything up a while");
					float blow = HeavenSpells.incoming(p, p.damageSources().mobAttack(husk), 10);
					helper.assertTrue(blow == 5, "Its blows numbed by half (" + blow + ")");
				} finally {
					dropped.forEach(ItemEntity::discard);
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_metal_armoursplittingfingercracks")
	public void armourSplittingFingerCracks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.METAL_CORE_FORMATION, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(6)), 100, spawned);
			double armour = husk.getAttributeValue(Attributes.ARMOR);
			HeavenSpellTesting.lookAt(p, HeavenSpells.middle(husk));
			boolean cast = HeavenSpellTesting.cast(p, Spell.ARMOUR_SPLITTING_FINGER);
			helper.runAfterDelay(12, () -> {
				try {
					MetalSpells.Crack crack = HeavenSpells.lingering(null, MetalSpells.Crack.class).get(0);
					helper.assertTrue(cast && husk.getHealth() == 80 && crack.stage() == 1, "20 harm, a crack opened (" + husk.getHealth() + ", " + crack.stage() + ")");
					helper.assertTrue(HeavenSpellTesting.near(husk.getAttributeValue(Attributes.ARMOR), armour * 0.8), "A fifth of its armour gone ("
							+ husk.getAttributeValue(Attributes.ARMOR) + " of " + armour + ")");
					float blow = HeavenSpells.incoming(husk, p.damageSources().playerAttack(p), 10);
					helper.assertTrue(HeavenSpellTesting.near(blow, 10.5) && crack.stage() == 2, "A blow lands 5% harder and widens it (" + blow + ", " + crack.stage() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_metal_goldenlightningleaps")
	public void goldenLightningLeaps(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 16, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.METAL_CORE_FORMATION, centre);
			Husk a = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(6)), 100, spawned);
			Husk b = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(6).east(4)), 100, spawned);
			Husk c = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(6).east(9)), 100, spawned);
			HeavenSpellTesting.lookAt(p, HeavenSpells.middle(a));
			boolean cast = HeavenSpellTesting.cast(p, Spell.LEAPING_GOLDEN_LIGHTNING);
			helper.runAfterDelay(8, () -> {
				try {
					helper.assertTrue(cast && a.getHealth() == 74 && b.getHealth() == 82 && c.getHealth() == 84, "26, then 18, then 16 as it leapt (" + a.getHealth()
							+ ", " + b.getHealth() + ", " + c.getHealth() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_metal_ninecirclingbladesdart")
	public void nineCirclingBladesDart(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.METAL_NASCENT_SOUL, centre);
			boolean cast = HeavenSpellTesting.cast(p, Spell.NINE_CIRCLING_BLADES);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(4)), 60, spawned);
			helper.runAfterDelay(8, () -> {
				try {
					MetalSpells.Blades blades = HeavenSpells.lingering(p.getUUID(), MetalSpells.Blades.class).get(0);
					helper.assertTrue(cast && husk.getHealth() == 34 && blades.left() == 8, "A blade darted out: 26, spent (" + husk.getHealth() + ", " + blades.left() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_metal_thousandechotollshareswounds")
	public void thousandEchoTollSharesWounds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 10, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.METAL_NASCENT_SOUL, centre);
			Husk a = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(6)), 100, spawned);
			Husk b = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.east(6)), 100, spawned);
			boolean cast = HeavenSpellTesting.cast(p, Spell.THOUSAND_ECHO_TOLL);
			helper.runAfterDelay(2, () -> {
				try {
					helper.assertTrue(cast && a.getHealth() == 78 && b.getHealth() == 78, "Tolled: 22 each (" + a.getHealth() + ", " + b.getHealth() + ")");
					a.setHealth(a.getHealth() - 10);
				} catch (RuntimeException e) {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
					throw e;
				}
				helper.runAfterDelay(2, () -> {
					try {
						helper.assertTrue(b.getHealth() == 74 && a.getHealth() == 68, "The other echoes 40% of it, and no echo comes back (" + a.getHealth() + ", "
								+ b.getHealth() + ")");
					} finally {
						HeavenSpellTesting.cleanUp(p, spawned, floor);
					}
					helper.succeed();
				});
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_metal_fateseveringedgecutsblessings")
	public void fateSeveringEdgeCutsBlessings(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.METAL_HEAVENLY_BEING, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(6)), 100, spawned);
			husk.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 0));
			husk.setAbsorptionAmount(10);
			HeavenSpellTesting.lookAt(p, HeavenSpells.middle(husk));
			boolean cast = HeavenSpellTesting.cast(p, Spell.FATE_SEVERING_EDGE);
			helper.runAfterDelay(2, () -> {
				try {
					helper.assertTrue(cast && !husk.hasEffect(MobEffects.DAMAGE_RESISTANCE) && husk.getAbsorptionAmount() == 0, "Its blessings severed ("
							+ husk.getActiveEffects() + ", " + husk.getAbsorptionAmount() + ")");
					helper.assertTrue(husk.getHealth() == 40, "Then cut for 60 (" + husk.getHealth() + ")");
					husk.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
					helper.assertTrue(!husk.hasEffect(MobEffects.MOVEMENT_SPEED), "No new blessing takes hold");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_metal_tenthousandswordsrain", timeoutTicks = 150)
	public void tenThousandSwordsRain(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 14, 24);
		BlockPos centre = floor.centre();
		BlockPos spot = centre.south(6);
		// A roof wide enough that no sword striking the open ground beside it comes within its cut of the husk beneath.
		for (int x = 2; x <= 8; x++) for (int z = -3; z <= 3; z++) level.setBlock(spot.offset(x, 4, z), Blocks.STONE.defaultBlockState(), 2);
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.METAL_HEAVENLY_BEING, centre);
			Husk open = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(spot.west(2)), 100, spawned);
			Husk roofed = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(spot.east(5)), 100, spawned);
			HeavenSpellTesting.lookAt(p, Vec3.atCenterOf(spot.below()).add(0, 0.5, 0));
			boolean cast = HeavenSpellTesting.cast(p, Spell.TEN_THOUSAND_SWORDS_DESCEND);
			helper.runAfterDelay(70, () -> {
				try {
					float lost = 100 - open.getHealth();
					helper.assertTrue(cast && lost >= 8 && lost <= 24, "Struck by a sword a second at most (" + cast + ", " + lost + ")");
					helper.assertTrue(roofed.getHealth() == 100, "The roof sheltered the other (" + roofed.getHealth() + ")");
				} finally {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
				}
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_metal_indestructiblegoldenbody")
	public void indestructibleGoldenBody(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 6, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.METAL_FOUR_AXIS, centre);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(centre.south(2)), 20, spawned);
			boolean cast = HeavenSpellTesting.cast(p, Spell.INDESTRUCTIBLE_GOLDEN_BODY);
			try {
				float blow = HeavenSpells.incoming(p, p.damageSources().mobAttack(husk), p.getMaxHealth());
				float small = HeavenSpells.incoming(p, p.damageSources().mobAttack(husk), 1);
				helper.assertTrue(cast && HeavenSpellTesting.near(blow, p.getMaxHealth() * 0.1) && small == 1, "No blow takes more than a tenth (" + blow + ", " + small + ")");
				helper.assertTrue(p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 1, "Nothing moves it");
				HeavenSpells.endLingering(p.getUUID(), MetalSpells.Golden.class);
				helper.assertTrue(p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) < 1, "...until it ends");
			} finally {
				HeavenSpellTesting.cleanUp(p, spawned, floor);
			}
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_metal_fourgatesarrayseals", timeoutTicks = 150)
	public void fourGatesArraySeals(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 24, 4);
		BlockPos centre = floor.centre();
		helper.runAfterDelay(1, () -> {
			List<LivingEntity> spawned = HeavenSpellTesting.spawned();
			ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.METAL_FOUR_AXIS, centre);
			BlockPos spot = centre.south(6).below();
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, Vec3.atBottomCenterOf(spot.above().east(3)), 200, spawned);
			HeavenSpellTesting.lookAt(p, Vec3.atCenterOf(spot).add(0, 0.5, 0));
			boolean cast = HeavenSpellTesting.cast(p, Spell.FOUR_GATES_IMMORTAL_SLAYING_ARRAY);
			MetalSpells.Gates gates = HeavenSpells.lingering(p.getUUID(), MetalSpells.Gates.class).get(0);
			helper.runAfterDelay(13, () -> {
				try {
					helper.assertTrue(cast && gates.seals(husk), "Sealed in (" + cast + ")");
					HeavenSpells.teleport(husk, Vec3.atBottomCenterOf(spot.above().east(20)));
				} catch (RuntimeException e) {
					HeavenSpellTesting.cleanUp(p, spawned, floor);
					throw e;
				}
				helper.runAfterDelay(2, () -> {
					try {
						helper.assertTrue(Math.abs(husk.getX() - (spot.getX() + 0.5)) <= 14 && husk.getHealth() == 140, "Cast back inside, cut for 60 ("
								+ husk.position() + ", " + husk.getHealth() + ")");
					} catch (RuntimeException e) {
						HeavenSpellTesting.cleanUp(p, spawned, floor);
						throw e;
					}
					helper.runAfterDelay(58, () -> {
						try {
							helper.assertTrue(husk.getHealth() == 124, "A volley from the gates: 16 (" + husk.getHealth() + ")");
						} finally {
							HeavenSpellTesting.cleanUp(p, spawned, floor);
						}
						helper.succeed();
					});
				});
			});
		});
	}
}
