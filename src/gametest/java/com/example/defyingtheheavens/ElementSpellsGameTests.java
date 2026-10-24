package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.example.defyingtheheavens.HeavenSpellTesting.near;

/**
 * The five elements' spells (Spell#isHeaven; FireSpells to MetalSpells, SpellAuras): every one takes hold and is paid for, the wards
 * and the cycle of the five in numbers, and a spell of each kind doing what it says. Each long-reaching test in a batch of its own on a
 * floor high over its spot (HeavenSpellTesting).
 */
public class ElementSpellsGameTests implements FabricGameTest {
	/** A Four Axis Grand Perfection caster who knows every element's Four Axis method to its last layer. */
	private static ServerPlayer master(GameTestHelper helper, BlockPos at) {
		ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.FIRE_FOUR_AXIS, at);
		HeavenState s = CultivationManager.get(p).heaven();
		for (QiElement element : QiElement.five()) {
			HeavenMethod method = HeavenMethod.of(element, Realm.FOUR_AXIS);
			s.setMethod(method, method.layers(), 0);
		}
		return p;
	}

	/** A master of every element's spells, Dao Integration's and Void Shattering's too: that realm's Grand Perfection, each element's method of it understood whole. */
	private static ServerPlayer grandMaster(GameTestHelper helper, BlockPos at) {
		ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.FIRE_FOUR_AXIS, at);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(Realm.VOID_SHATTERING, Stage.GRAND_PERFECTION, 0);
		for (QiElement element : QiElement.five()) {
			HeavenMethod method = HeavenMethod.of(element, Realm.VOID_SHATTERING);
			c.heaven().setMethod(method, method.layers(), 0);
		}
		CultivationManager.refresh(p);
		c.setQi(c.maxQi());
		p.setHealth(p.getMaxHealth());
		return p;
	}

	private static Husk mark(ServerLevel level, Vec3 at, List<LivingEntity> spawned) {
		Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, at, 4000, spawned);
		husk.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 20 * 600, 0, false, false));
		return husk;
	}

	/** Every spell entity (missiles, fields, moving blocks, golems, tides, the great spells' living shapes) round {@code at} discarded. */
	private static void sweep(ServerLevel level, Vec3 at, double radius) {
		for (Entity e : level.getEntities((Entity) null, new AABB(at, at).inflate(radius),
				e -> e instanceof SpellProjectile || e instanceof SpellField || e instanceof SpellBlock || e instanceof EarthGolem || e instanceof DirtTide
						|| e instanceof SpellConstruct)) {
			e.discard();
		}
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "element_spells_all", timeoutTicks = 900)
	public void everySpellTakesHold(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 14, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = grandMaster(helper, floor.centre());
		Vec3 home = p.position();
		Husk husk = mark(level, home.add(0, 0, 7), spawned);
		List<String> refused = new ArrayList<>();
		try {
			for (Spell spell : Spell.values()) {
				if (!spell.isHeaven()) continue;
				p.stopRiding();
				// The floor laid again before it (a boulder's crater, a cave, a cleave's slot may have taken some of it).
				for (int dx = -5; dx <= 5; dx++) {
					for (int dz = -3; dz <= 10; dz++) {
						BlockPos pos = floor.centre().offset(dx, -1, dz);
						level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
						for (int dy = 0; dy < 3; dy++) if (!level.getBlockState(pos.above(1 + dy)).isAir()) level.setBlock(pos.above(1 + dy), Blocks.AIR.defaultBlockState(), 2);
					}
				}
				HeavenSpells.teleport(p, home);
				p.setOnGround(true);
				p.setHealth(p.getMaxHealth());
				CultivationManager.get(p).setQi(CultivationManager.get(p).maxQi());
				// An ore in the floor for Mineral Purification to draw out (finding none, it takes no hold and pays nothing).
				if (spell == Spell.MINERAL_PURIFICATION) level.setBlock(floor.centre().offset(5, -1, -3), Blocks.IRON_ORE.defaultBlockState(), 2);
				helper.assertTrue(spell.isKnown(CultivationManager.get(p)), spell.getId() + " is known to a Void Shattering master of every element");
				// Look at the mark; for a spell wanting a surface, at the floor before it.
				HeavenSpellTesting.lookAt(p, HeavenSpells.middle(husk));
				boolean paid = HeavenSpellTesting.cast(p, spell);
				if (!paid) {
					HeavenSpellTesting.lookAt(p, home.add(0, 0, 3));
					paid = HeavenSpellTesting.cast(p, spell);
				}
				if (!paid) refused.add(spell.getId());
				// Whatever it set going ends at once (lingering effects, golems, a tide), its pictures swept away.
				HeavenSpells.forget(p.getUUID());
				Spells.forget(p.getUUID());
				sweep(level, home, 140);
				husk.setHealth(husk.getMaxHealth());
				husk.clearFire();
			}
			helper.assertTrue(refused.isEmpty(), "Every element spell takes hold and is paid for (refused: " + refused + ")");
			sweep(level, home, 140);
			HeavenSpellTesting.succeedOnceHealed(helper, p, spawned, floor);
		} catch (RuntimeException e) {
			sweep(level, home, 140);
			HeavenSpellTesting.cleanUp(p, spawned, floor);
			throw e;
		}
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void wardsStandInTheCycleOfTheFive(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
		Husk husk = mark(level, at, spawned);
		try {
			DamageSource fireQr = ModDamageTypes.qi(level, QiElement.FIRE, Realm.QI_REFINING, null, null);
			DamageSource fireHb = ModDamageTypes.qi(level, QiElement.FIRE, Realm.HEAVENLY_BEING, null, null);
			DamageSource metal = ModDamageTypes.qi(level, QiElement.METAL, Realm.FOUNDATION_BUILDING, null, null);
			DamageSource earth = ModDamageTypes.qi(level, QiElement.EARTH, Realm.FOUNDATION_BUILDING, null, null);
			DamageSource water = ModDamageTypes.qi(level, QiElement.WATER, Realm.FOUNDATION_BUILDING, null, null);
			DamageSource wood = ModDamageTypes.qi(level, QiElement.WOOD, Realm.CORE_FORMATION, null, null);
			SpellAuras.put(husk, SpellAura.AZURE_COATING, 200);
			helper.assertTrue(HeavenSpells.incoming(husk, fireQr, 5) == 0 && HeavenSpells.wardedOff(husk), "Azure Coating stops a fire spell outright");
			helper.assertTrue(near(HeavenSpells.incoming(husk, fireHb, 10), 5), "...but one of a deeper realm breaks half through");
			helper.assertTrue(HeavenSpells.incoming(husk, level.damageSources().onFire(), 1) == 0, "...and fire itself can't burn it");
			helper.assertTrue(near(HeavenSpells.incoming(husk, earth, 10), 12.5), "Earth overcomes water: through the coating a quarter harder");
			SpellAuras.remove(husk, SpellAura.AZURE_COATING);
			SpellAuras.put(husk, SpellAura.FLAME_ARMOUR, 200);
			helper.assertTrue(near(HeavenSpells.incoming(husk, metal, 10), 8), "Flame Armour: a fifth off metal's harm");
			helper.assertTrue(near(HeavenSpells.incoming(husk, earth, 10), 9), "...a tenth off anything else's");
			helper.assertTrue(near(HeavenSpells.incoming(husk, water, 10), 10 * 0.9 * 1.25), "...and water pierces it");
			SpellAuras.remove(husk, SpellAura.FLAME_ARMOUR);
			SpellAuras.put(husk, SpellAura.HARDENED_BODY, 200);
			DamageSource waterQr = ModDamageTypes.qi(level, QiElement.WATER, Realm.QI_REFINING, null, null);
			helper.assertTrue(near(HeavenSpells.incoming(husk, earth, 10), 9.3) && near(HeavenSpells.incoming(husk, waterQr, 10), 8.6),
					"Hardened Body: 7% off, 14% off water's");
			helper.assertTrue(near(HeavenSpells.incoming(husk, water, 10), 9.3), "...but a deeper realm's water spell half breaks through: back to 7%");
			SpellAuras.remove(husk, SpellAura.HARDENED_BODY);
			SpellAuras.put(husk, SpellAura.CRIMSON_TEMPERING, 200);
			helper.assertTrue(HeavenSpells.incoming(husk, level.damageSources().lava(), 4) == 0, "Crimson Tempering: lava can't burn it");
			SpellAuras.remove(husk, SpellAura.CRIMSON_TEMPERING);
			SpellAuras.bell(husk, 40, 200);
			helper.assertTrue(HeavenSpells.incoming(husk, earth, 30) == 0 && near(SpellAuras.bellLeft(husk), 10), "The Golden Bell takes 30 of its 40");
			helper.assertTrue(HeavenSpells.incoming(husk, wood, 10) == 0 && near(SpellAuras.bellLeft(husk), 5), "...wood's at half its cost");
			// Fire overcomes metal: the blow comes a quarter harder (12.5), and the bell takes it at twice the cost (its 5 left: 2.5).
			float through = HeavenSpells.incoming(husk, fireQr, 10);
			helper.assertTrue(near(through, 10) && !SpellAuras.has(husk, SpellAura.GOLDEN_BELL), "...fire's pierces it and costs twice: it breaks (" + through + ")");
			helper.succeed();
		} finally {
			for (LivingEntity e : spawned) e.discard();
		}
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void bladePolishAndFlameFist(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
		Husk striker = HeavenSpellTesting.idle(level, EntityType.HUSK, at, 40, spawned);
		Husk struck = HeavenSpellTesting.idle(level, EntityType.HUSK, at.add(1, 0, 0), 40, spawned);
		try {
			SpellAuras.put(striker, SpellAura.BLADE_POLISH, 200);
			helper.assertTrue(near(HeavenSpells.incoming(struck, level.damageSources().mobAttack(striker), 10), 12), "A polished blade: 1.2 times the blow");
			SpellAuras.put(striker, SpellAura.FLAME_FIST, 200);
			SpellAuras.onMelee(striker, struck);
			helper.assertTrue(struck.isOnFire(), "A burning fist sets its mark alight");
			struck.clearFire();
			SpellAuras.put(struck, SpellAura.AZURE_COATING, 200);
			SpellAuras.onMelee(striker, struck);
			helper.assertTrue(!struck.isOnFire(), "...not one coated in water");
			helper.succeed();
		} finally {
			for (LivingEntity e : spawned) e.discard();
		}
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "element_spells_bolt", timeoutTicks = 120)
	public void flameBoltStrikesAndBurns(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 6, 6);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = master(helper, floor.centre());
		Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, p.position().add(0, 0, 5), 40, spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(husk));
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.FLAME_BOLT), "Cast");
		helper.runAfterDelay(25, () -> {
			try {
				helper.assertTrue(husk.getHealth() < 40 && husk.getHealth() >= 34 && husk.isOnFire(), "The bolt struck for 5 and set it burning (" + husk.getHealth() + ")");
				helper.succeed();
			} finally {
				sweep(level, floor.centre().getCenter(), 30);
				HeavenSpellTesting.cleanUp(p, spawned, floor);
			}
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "element_spells_wall", timeoutTicks = 200)
	public void earthWallRisesAndSinks(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 8);
		ServerLevel level = helper.getLevel();
		ServerPlayer p = master(helper, floor.centre());
		p.setYRot(0); // facing south (+z)
		p.setYHeadRot(0);
		p.setOnGround(true);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.EARTH_WALL_TECHNIQUE), "Cast");
		BlockPos front = p.blockPosition().south(EarthSpells.WALL_AHEAD);
		helper.runAfterDelay(EarthSpells.WALL_RISE + 12, () -> {
			int stone = 0;
			for (int dx = -4; dx <= 4; dx++) for (int dy = 0; dy < 6; dy++) if (level.getBlockState(front.offset(dx, dy, 0)).is(Blocks.STONE)) stone++;
			try {
				helper.assertTrue(stone == EarthSpells.WALL_LONG * EarthSpells.WALL_TALL, "A wall 6 by 4 of the ground's stone (" + stone + ")");
				HeavenSpells.endLingering(p.getUUID(), EarthSpells.Wall.class);
				int left = 0;
				for (int dx = -4; dx <= 4; dx++) for (int dy = 0; dy < 6; dy++) if (level.getBlockState(front.offset(dx, dy, 0)).is(Blocks.STONE)) left++;
				helper.assertTrue(left == 0, "...that sinks back without a trace (" + left + ")");
				helper.succeed();
			} finally {
				sweep(level, floor.centre().getCenter(), 30);
				HeavenSpellTesting.cleanUp(p, HeavenSpellTesting.spawned(), floor);
			}
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "element_spells_prison", timeoutTicks = 120)
	public void bristlePrisonBreaksAtThreeBlows(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 6, 6);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = master(helper, floor.centre());
		Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, p.position().add(0, 0, 4), 200, spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(husk));
		try {
			helper.assertTrue(HeavenSpellTesting.cast(p, Spell.MYSTERIOUS_BRISTLE_PRISON), "Cast");
			List<WoodSpells.Prison> prisons = HeavenSpells.lingering(p.getUUID(), WoodSpells.Prison.class);
			List<WoodSpells.Bound> bound = HeavenSpells.lingering(p.getUUID(), WoodSpells.Bound.class);
			helper.assertTrue(prisons.size() == 1 && bound.size() == 1 && bound.get(0).target == husk, "The husk caged and held");
			SpellField cage = level.getEntitiesOfClass(SpellField.class, husk.getBoundingBox().inflate(2), f -> f.kind() == SpellField.Kind.BRISTLE_PRISON).get(0);
			helper.assertTrue(cage.isPickable(), "The cage can be struck");
			for (int i = 0; i < WoodSpells.PRISON_STRIKES; i++) cage.hurt(level.damageSources().mobAttack(husk), 1);
			helper.runAfterDelay(3, () -> {
				helper.assertTrue(HeavenSpells.lingering(p.getUUID(), WoodSpells.Prison.class).isEmpty() && HeavenSpells.lingering(p.getUUID(), WoodSpells.Bound.class).isEmpty(),
						"Three blows from inside break it, and the husk is free");
				helper.assertTrue(husk.getHealth() < 200, "...though the thorns tore it for each");
				helper.succeed();
			});
		} finally {
			helper.runAfterDelay(4, () -> {
				sweep(level, floor.centre().getCenter(), 30);
				HeavenSpellTesting.cleanUp(p, spawned, floor);
			});
		}
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "element_spells_water_prison", timeoutTicks = 120)
	public void waterPrisonDrowns(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 6, 6);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = master(helper, floor.centre());
		// A living mark: the undead (a husk) breathe water as vanilla has it, and the prison can't drown them.
		net.minecraft.world.entity.monster.Vindicator mark = HeavenSpellTesting.idle(level, EntityType.VINDICATOR, p.position().add(0, 0, 4), 200, spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(mark));
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.WATER_PRISON), "Cast");
		helper.runAfterDelay(45, () -> {
			try {
				// Its air is held at nothing (its own tick breathes a little back in before the prison takes it again): two drownings
				// (2 each) and three strikes of water qi by now.
				helper.assertTrue(mark.getAirSupply() <= 10 && mark.getHealth() <= 200 - 8, "No air in it, drowning from the first second ("
						+ mark.getAirSupply() + " air, " + mark.getHealth() + ")");
				helper.succeed();
			} finally {
				Spells.forget(p.getUUID());
				sweep(level, floor.centre().getCenter(), 30);
				HeavenSpellTesting.cleanUp(p, spawned, floor);
			}
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "element_spells_golems", timeoutTicks = 120)
	public void twoGolemsAtMost(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 8);
		ServerLevel level = helper.getLevel();
		ServerPlayer p = master(helper, floor.centre());
		try {
			for (int i = 0; i < 3; i++) helper.assertTrue(HeavenSpellTesting.cast(p, Spell.SUMMON_GOLEM), "Cast " + (i + 1));
			List<EarthGolem> golems = EarthSpells.golems(p);
			helper.assertTrue(golems.size() == EarthSpells.GOLEMS, "Two golems stand; the third sent the oldest back (" + golems.size() + ")");
			helper.assertTrue(golems.get(0).caster() == p && !golems.get(0).isTitan(), "Its caster's, a golem's size");
			helper.succeed();
		} finally {
			Spells.forget(p.getUUID());
			sweep(level, floor.centre().getCenter(), 30);
			HeavenSpellTesting.cleanUp(p, HeavenSpellTesting.spawned(), floor);
		}
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void karmicFireBurnsBySin(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
		CultivatorNpc devil = ModEntities.CULTIVATOR.create(level);
		CultivatorNpc saint = ModEntities.CULTIVATOR.create(level);
		try {
			devil.setAlignment(Alignment.MIN);
			saint.setAlignment(Alignment.MAX);
			Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, at, 20, spawned);
			helper.assertTrue(near(FireSpells.karmicHarm(devil), 11) && near(FireSpells.karmicHarm(saint), 1) && near(FireSpells.karmicHarm(husk), 6),
					"A devil 11 a second, the righteous 1, a monster 6");
			helper.succeed();
		} finally {
			devil.discard();
			saint.discard();
			for (LivingEntity e : spawned) e.discard();
		}
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "element_spells_nirvana", timeoutTicks = 60)
	public void phoenixNirvanaUndoesOneDeath(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 6, 6);
		ServerPlayer p = master(helper, floor.centre());
		try {
			helper.assertTrue(HeavenSpellTesting.cast(p, Spell.PHOENIX_NIRVANA), "Cast");
			// What nothing holds off (/kill, old age, one's own core: HeavenPassives#untouchedByWards) it doesn't either, nor is it spent
			// on it (the bug sweep of 2026-10-23).
			helper.assertTrue(HeavenSpells.allowDeath(p, helper.getLevel().damageSources().genericKill()) && SpellAuras.has(p, SpellAura.PHOENIX_NIRVANA),
					"/kill is not undone, and leaves it be");
			DamageSource death = helper.getLevel().damageSources().generic();
			helper.assertTrue(!HeavenSpells.allowDeath(p, death) && near(p.getHealth(), p.getMaxHealth()) && !SpellAuras.has(p, SpellAura.PHOENIX_NIRVANA),
					"The first death is undone, at full health");
			helper.assertTrue(HeavenSpells.allowDeath(p, death), "...the second is not");
			helper.succeed();
		} finally {
			Spells.forget(p.getUUID());
			HeavenSpellTesting.cleanUp(p, HeavenSpellTesting.spawned(), floor);
		}
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "element_spells_ores", timeoutTicks = 60)
	public void mineralPurificationDrawsOutTheOre(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 6, 6);
		ServerLevel level = helper.getLevel();
		ServerPlayer p = master(helper, floor.centre());
		BlockPos ore = floor.centre().offset(2, -1, 0);
		level.setBlock(ore, Blocks.IRON_ORE.defaultBlockState(), 3);
		level.setBlock(ore.east(), Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState(), 3);
		try {
			helper.assertTrue(HeavenSpellTesting.cast(p, Spell.MINERAL_PURIFICATION), "Cast");
			helper.assertTrue(level.getBlockState(ore).is(Blocks.STONE) && level.getBlockState(ore.east()).is(Blocks.DEEPSLATE), "The rock is left");
			boolean iron = false, gold = false;
			for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
				iron |= p.getInventory().getItem(i).is(Items.RAW_IRON);
				gold |= p.getInventory().getItem(i).is(Items.RAW_GOLD);
			}
			helper.assertTrue(iron && gold, "...and the ores, unsmelted, are the caster's");
			helper.succeed();
		} finally {
			sweep(level, floor.centre().getCenter(), 30);
			HeavenSpellTesting.cleanUp(p, HeavenSpellTesting.spawned(), floor);
		}
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "element_spells_ores_barrier", timeoutTicks = 60)
	public void mineralPurificationLeavesOresInABarrier(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 10, 8);
		ServerLevel level = helper.getLevel();
		ServerPlayer p = master(helper, floor.centre());
		BlockPos outside = floor.centre().offset(3, -1, 0);
		BlockPos centre = floor.centre().offset(-6, 0, 0);
		BlockPos inside = centre.below();
		level.setBlock(outside, Blocks.IRON_ORE.defaultBlockState(), 3);
		level.setBlock(inside, Blocks.GOLD_ORE.defaultBlockState(), 3);
		List<UUID> raised = new ArrayList<>();
		try {
			// A protective barrier, then a concealment, round the gold; the caster (far the stronger) outside both.
			for (Formation.Kind kind : Formation.Kind.values()) {
				UUID id = UUID.randomUUID();
				raised.add(id);
				Formation formation = Formations.create(level, id, kind, centre, Formations.MIN_RADIUS);
				Formations.update(level, formation, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null);
				Formations.raise(level, formation);
				List<BlockPos> ores = MetalSpells.oresWithin(level, p, floor.centre(), 12);
				helper.assertTrue(ores.contains(outside) && !ores.contains(inside), "The ore inside a raised " + kind + " barrier is beyond its reach (" + ores + ")");
				Formations.dissolve(level, id);
			}
			helper.assertTrue(MetalSpells.oresWithin(level, p, floor.centre(), 12).contains(inside), "...and within it once the barrier is gone");
			helper.succeed();
		} finally {
			for (UUID id : raised) Formations.dissolve(level, id);
			HeavenSpellTesting.cleanUp(p, HeavenSpellTesting.spawned(), floor);
		}
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "element_spells_life", timeoutTicks = 80)
	public void lifeForceDrawsOnAFruitOnlyOnceHeld(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 6, 6);
		ServerLevel level = helper.getLevel();
		ServerPlayer p = master(helper, floor.centre());
		BlockPos fruit = floor.centre().offset(0, 1, 2);
		level.setBlock(fruit.above(), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), 3);
		level.setBlock(fruit, ModBlocks.CULTIVATION_FRUIT.defaultBlockState(), 3);
		HeavenSpellTesting.lookAt(p, Vec3.atCenterOf(fruit));
		try {
			WoodSpells.LifeTarget given = WoodSpells.lifeTarget(p, false, false);
			helper.assertTrue(given != null && fruit.equals(given.plant()), "Life Force Gain takes hold of the fruit on its tree (" + given + ")");
			helper.assertTrue(WoodSpells.lifeTarget(p, true, false) == null, "...Drain can't: a fruit is drawn on only once picked");
			ItemStack two = CultivationFruitItem.create(500);
			two.setCount(2);
			p.setItemInHand(InteractionHand.MAIN_HAND, two);
			helper.assertTrue(WoodSpells.lifeTarget(p, true, false) == WoodSpells.LifeTarget.TOLD, "...nor a whole stack held at once");
			p.setItemInHand(InteractionHand.MAIN_HAND, CultivationFruitItem.create(500));
			WoodSpells.LifeTarget held = WoodSpells.lifeTarget(p, true, false);
			helper.assertTrue(held != null && held.hand() == InteractionHand.MAIN_HAND, "...but a single fruit in hand, yes");
			helper.assertTrue(HeavenSpellTesting.cast(p, Spell.LIFE_FORCE_DRAIN), "Cast");
		} catch (RuntimeException e) {
			HeavenSpellTesting.cleanUp(p, HeavenSpellTesting.spawned(), floor);
			throw e;
		}
		helper.runAfterDelay(25, () -> {
			try {
				int age = CultivationFruitItem.age(p.getMainHandItem());
				helper.assertTrue(age == 500 - WoodSpells.DRAIN_YEARS, "Two half-seconds of drawing took 60 of its 500 years (" + age + ")");
				helper.succeed();
			} finally {
				sweep(level, floor.centre().getCenter(), 30);
				HeavenSpellTesting.cleanUp(p, HeavenSpellTesting.spawned(), floor);
			}
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "element_spells_carve", timeoutTicks = 60)
	public void terrainSpellsCarveWhatIsBuilt(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 6, 6);
		ServerLevel level = helper.getLevel();
		ServerPlayer p = master(helper, floor.centre());
		BlockPos at = floor.centre().offset(0, 0, 4);
		level.setBlock(at, Blocks.OAK_PLANKS.defaultBlockState(), 3);
		level.setBlock(at.above(), Blocks.STONE_BRICKS.defaultBlockState(), 3);
		level.setBlock(at.east(), Blocks.CHEST.defaultBlockState(), 3);
		level.setBlock(at.west(), Blocks.OBSIDIAN.defaultBlockState(), 3);
		helper.assertTrue(HeavenSpells.carvable(level, at, p) && HeavenSpells.carvable(level, at.above(), p), "Planks and bricks laid by hand can be torn up");
		helper.assertTrue(!HeavenSpells.carvable(level, at.east(), p) && !HeavenSpells.carvable(level, at.west(), p), "...not a chest (what it holds), nor obsidian");
		Devastation.crater(p, level, Vec3.atCenterOf(at), 2.5, 2.5, Devastation.Floor.NONE);
		// A crater is cut over the next ticks (Devastation's budget).
		helper.runAfterDelay(5, () -> {
			try {
				helper.assertTrue(level.getBlockState(at).isAir() && level.getBlockState(at.above()).isAir(), "A spell's crater there takes the planks and bricks");
				helper.assertTrue(level.getBlockState(at.east()).is(Blocks.CHEST) && level.getBlockState(at.west()).is(Blocks.OBSIDIAN), "...and leaves the chest and the obsidian");
				helper.succeed();
			} finally {
				HeavenSpellTesting.cleanUp(p, HeavenSpellTesting.spawned(), floor);
			}
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void spellsComeWithTheirElementsDeeperMethods(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION, 0);
		HeavenMethod fireCf = HeavenMethod.of(QiElement.FIRE, Realm.CORE_FORMATION);
		c.heaven().setMethod(fireCf, fireCf.layers(), 0);
		helper.assertTrue(Spell.FLAME_BOLT.isKnown(c) && Spell.FIRE_SERPENT.isKnown(c) && Spell.SOLAR_FIELD.isKnown(c), "A Core Formation fire method teaches the fire spells up to its realm");
		helper.assertTrue(!Spell.KARMIC_FIRE.isKnown(c) && !Spell.WOOD_BULLET.isKnown(c), "...not a deeper realm's, nor another element's");
		helper.assertTrue(Spell.FLAME_BOLT.teacher(c.heaven()) == fireCf, "Its teacher shown is the method known");
		helper.succeed();
	}
}
