package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Every element spell cast in circumstances other than an open floor: under water, in the air, under a low roof, in the Nether,
 * with no qi, on cooldown, by a creative caster, by two casters at once. None may throw; each either takes hold (paid, its
 * cooldown begun) or is refused with nothing paid. Each in a batch of its own on a floor high over its spot (HeavenSpellTesting).
 */
public class SpellCircumstancesGameTests implements FabricGameTest {
	/** A master of every element's spells: True Immortal's Grand Perfection, each element's method of that realm understood whole. */
	private static ServerPlayer master(GameTestHelper helper, BlockPos at) {
		ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.FIRE_FOUR_AXIS, at);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION, 0);
		HeavenState s = c.heaven();
		for (QiElement element : QiElement.five()) {
			HeavenMethod method = HeavenMethod.of(element, Realm.TRUE_IMMORTAL);
			s.setMethod(method, method.layers(), 0);
		}
		CultivationManager.refresh(p);
		c.setQi(c.maxQi());
		p.setHealth(p.getMaxHealth());
		return p;
	}

	private static void sweep(ServerLevel level, Vec3 at, double radius) {
		for (Entity e : level.getEntities((Entity) null, new AABB(at, at).inflate(radius),
				e -> e instanceof SpellProjectile || e instanceof SpellField || e instanceof SpellBlock || e instanceof EarthGolem || e instanceof DirtTide
						|| e instanceof SpellConstruct)) {
			e.discard();
		}
	}

	/** Whether {@code spell} took hold for {@code p} just now: its cooldown begun (the cast reached it), qi paid or not. */
	private static boolean tookHold(ServerPlayer p, Spell spell) {
		return spell.cooldownTicks() <= 0 || Spells.cooldownLeft(p, spell) > 0;
	}

	/**
	 * Casts every element spell from {@code home} at {@code husk} (or the floor before it), after {@code before} sets the circumstance
	 * for each; records any that throw, and each refused. Whatever a spell set going is ended after it.
	 */
	private static List<String> castAll(GameTestHelper helper, ServerPlayer p, Vec3 home, Husk husk, Consumer<Spell> before, List<String> refused) {
		return castAll(helper, p, home, husk, before, refused, spell -> {});
	}

	/** {@link #castAll}, with {@code after} told of each spell as soon as it is cast. */
	private static List<String> castAll(GameTestHelper helper, ServerPlayer p, Vec3 home, Husk husk, Consumer<Spell> before, List<String> refused,
										Consumer<Spell> after) {
		ServerLevel level = (ServerLevel) p.level();
		List<String> threw = new ArrayList<>();
		for (Spell spell : Spell.values()) {
			if (!spell.isHeaven()) continue;
			p.stopRiding();
			HeavenSpells.teleport(p, home);
			p.setHealth(p.getMaxHealth());
			CultivationManager.get(p).setQi(CultivationManager.get(p).maxQi());
			Spells.setCooldown(p, spell, 0);
			before.accept(spell);
			try {
				HeavenSpellTesting.lookAt(p, HeavenSpells.middle(husk));
				Spells.cast(p, spell);
				if (!tookHold(p, spell)) {
					HeavenSpellTesting.lookAt(p, home.add(0, -1, 3));
					Spells.cast(p, spell);
				}
				if (!tookHold(p, spell)) refused.add(spell.getId());
				after.accept(spell);
				helper.assertTrue(CultivationManager.get(p).getQi() >= 0, spell.getId() + " left the caster's qi below nothing");
			} catch (RuntimeException e) {
				DefyingTheHeavens.LOGGER.error("[spell circumstances] {} threw", spell.getId(), e);
				threw.add(spell.getId() + " (" + e + ")");
			} finally {
				HeavenSpells.forget(p.getUUID());
				Spells.forget(p.getUUID());
				SpellAuras.forget(p);
				sweep(level, home, 140);
				husk.setHealth(husk.getMaxHealth());
				husk.clearFire();
			}
		}
		return threw;
	}

	/**
	 * Casts every element spell in turn, {@code wait} ticks apart so each plays out (its missiles fly, its fields tick, its blocks
	 * rise) in the circumstance {@code before} sets; records any that throw and each refused, then runs {@code then}. A spell's
	 * ticking going wrong would stop the server, and the run with it.
	 */
	private static void castInTurn(GameTestHelper helper, ServerPlayer p, Vec3 home, Husk husk, Consumer<Spell> before, int wait, List<String> refused,
								   List<String> threw, Runnable then) {
		List<Spell> spells = new ArrayList<>();
		for (Spell spell : Spell.values()) if (spell.isHeaven()) spells.add(spell);
		step(helper, p, home, husk, before, wait, refused, threw, then, spells, 0);
	}

	private static void step(GameTestHelper helper, ServerPlayer p, Vec3 home, Husk husk, Consumer<Spell> before, int wait, List<String> refused,
							 List<String> threw, Runnable then, List<Spell> spells, int i) {
		if (i >= spells.size()) {
			then.run();
			return;
		}
		Spell spell = spells.get(i);
		ServerLevel level = (ServerLevel) p.level();
		p.stopRiding();
		HeavenSpells.teleport(p, home);
		p.setHealth(p.getMaxHealth());
		CultivationManager.get(p).setQi(CultivationManager.get(p).maxQi());
		Spells.setCooldown(p, spell, 0);
		before.accept(spell);
		try {
			HeavenSpellTesting.lookAt(p, HeavenSpells.middle(husk));
			Spells.cast(p, spell);
			if (!tookHold(p, spell)) {
				HeavenSpellTesting.lookAt(p, home.add(0, -1, 3));
				Spells.cast(p, spell);
			}
			if (!tookHold(p, spell)) refused.add(spell.getId());
		} catch (RuntimeException e) {
			DefyingTheHeavens.LOGGER.error("[spell circumstances] {} threw", spell.getId(), e);
			threw.add(spell.getId() + " (" + e + ")");
		}
		helper.runAfterDelay(wait, () -> {
			helper.assertTrue(CultivationManager.get(p).getQi() >= 0, spell.getId() + " left the caster's qi below nothing");
			helper.assertTrue(p.isAlive() || p.getHealth() >= 0, spell.getId() + " left its caster in a broken state");
			// Everything it set going ended and every block it set down taken up (an earth wall left standing would shield the mark
			// from the spells after it): the batch is this test's alone.
			HeavenSpells.clear();
			Spells.forget(p.getUUID());
			SpellAuras.forget(p);
			sweep(level, home, 140);
			if (!p.isAlive()) p.setHealth(p.getMaxHealth());
			husk.setHealth(husk.getMaxHealth());
			husk.clearFire();
			step(helper, p, home, husk, before, wait, refused, threw, then, spells, i + 1);
		});
	}

	private static Husk mark(ServerLevel level, Vec3 at, List<LivingEntity> spawned) {
		Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, at, 4000, spawned);
		husk.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 20 * 600, 0, false, false));
		return husk;
	}

	/** The ticks each spell is given to play out in the ticking tours. */
	private static final int PLAY_OUT = 12;
	/** Room for every element spell's play (80 since Dao Integration's, 2026-10-24; up to 140, as the realms after it come). */
	private static final int TOUR_TICKS = 140 * PLAY_OUT + 200;

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "spells_underwater", timeoutTicks = TOUR_TICKS)
	public void everySpellUnderWater(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 12, 10);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = master(helper, floor.centre());
		Vec3 home = p.position();
		Husk husk = mark(level, home.add(0, 0, 6), spawned);
		List<String> refused = new ArrayList<>(), threw = new ArrayList<>();
		// A pool four deep over the floor, laid again before each (a spell may boil, freeze or dig some away).
		Consumer<Spell> pool = spell -> {
			for (int dx = -8; dx <= 8; dx++) {
				for (int dz = -6; dz <= 10; dz++) {
					for (int dy = 0; dy < 4; dy++) {
						BlockPos pos = floor.centre().offset(dx, dy, dz);
						if (!HeavenSpells.isTemporary(level, pos)) level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
					}
					level.setBlock(floor.centre().offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 2);
				}
			}
		};
		castInTurn(helper, p, home, husk, pool, PLAY_OUT, refused, threw, () -> {
			try {
				DefyingTheHeavens.LOGGER.info("[spell circumstances] under water, refused: {}", refused);
				helper.assertTrue(threw.isEmpty(), "No spell throws cast under water (threw: " + threw + ")");
				helper.succeed();
			} finally {
				HeavenSpellTesting.cleanUp(p, spawned, floor);
			}
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "spells_midair", timeoutTicks = TOUR_TICKS)
	public void everySpellInTheAir(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 12, 30);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = master(helper, floor.centre());
		Vec3 home = p.position().add(0, 18, 0);
		Husk husk = mark(level, p.position().add(0, 0, 6), spawned);
		List<String> refused = new ArrayList<>(), threw = new ArrayList<>();
		Consumer<Spell> airborne = spell -> {
			p.getAbilities().mayfly = true;
			p.getAbilities().flying = true;
			p.setOnGround(false);
			p.fallDistance = 0;
		};
		castInTurn(helper, p, home, husk, airborne, PLAY_OUT, refused, threw, () -> {
			try {
				DefyingTheHeavens.LOGGER.info("[spell circumstances] in the air, refused: {}", refused);
				helper.assertTrue(threw.isEmpty(), "No spell throws cast in the air (threw: " + threw + ")");
				helper.succeed();
			} finally {
				HeavenSpellTesting.cleanUp(p, spawned, floor);
			}
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "spells_roof", timeoutTicks = TOUR_TICKS)
	public void everySpellUnderALowRoof(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 12, 10);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = master(helper, floor.centre());
		Vec3 home = p.position();
		Husk husk = mark(level, home.add(0, 0, 6), spawned);
		List<String> refused = new ArrayList<>(), threw = new ArrayList<>();
		Consumer<Spell> roof = spell -> {
			for (int dx = -10; dx <= 10; dx++) {
				for (int dz = -10; dz <= 12; dz++) {
					BlockPos pos = floor.centre().offset(dx, 3, dz);
					if (!HeavenSpells.isTemporary(level, pos)) level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
					for (int dy = 0; dy < 3; dy++) {
						BlockPos air = floor.centre().offset(dx, dy, dz);
						if (!level.getBlockState(air).isAir() && !HeavenSpells.isTemporary(level, air)) level.setBlock(air, Blocks.AIR.defaultBlockState(), 2);
					}
					level.setBlock(floor.centre().offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 2);
				}
			}
		};
		castInTurn(helper, p, home, husk, roof, PLAY_OUT, refused, threw, () -> {
			try {
				DefyingTheHeavens.LOGGER.info("[spell circumstances] under a roof, refused: {}", refused);
				helper.assertTrue(threw.isEmpty(), "No spell throws cast under a roof three high (threw: " + threw + ")");
				helper.succeed();
			} finally {
				for (int dx = -10; dx <= 10; dx++) for (int dz = -10; dz <= 12; dz++) level.setBlock(floor.centre().offset(dx, 3, dz), Blocks.AIR.defaultBlockState(), 2);
				HeavenSpellTesting.cleanUp(p, spawned, floor);
			}
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "spells_no_qi", timeoutTicks = 200)
	public void noSpellTakesHoldWithoutQi(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 12, 10);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = master(helper, floor.centre());
		Vec3 home = p.position();
		Husk husk = mark(level, home.add(0, 0, 6), spawned);
		try {
			List<String> tookHold = new ArrayList<>();
			// What earlier tests left flying about (a crater's flung pieces) isn't counted against these casts.
			java.util.function.Predicate<Entity> spellMade = e -> e instanceof SpellProjectile || e instanceof SpellField || e instanceof SpellBlock
					|| e instanceof EarthGolem || e instanceof DirtTide || e instanceof SpellConstruct;
			java.util.Set<Entity> before = new java.util.HashSet<>(level.getEntities((Entity) null, new AABB(home, home).inflate(60), spellMade));
			for (Spell spell : Spell.values()) {
				if (!spell.isHeaven() || spell.qiCost() <= 0) continue;
				CultivationManager.get(p).setQi(spell.qiCost() * 0.5);
				Spells.setCooldown(p, spell, 0);
				HeavenSpellTesting.lookAt(p, HeavenSpells.middle(husk));
				Spells.cast(p, spell);
				if (Spells.cooldownLeft(p, spell) > 0 || CultivationManager.get(p).getQi() < spell.qiCost() * 0.5 - 1.0e-6) tookHold.add(spell.getId());
				int left = level.getEntities((Entity) null, new AABB(home, home).inflate(60), e -> spellMade.test(e) && !before.contains(e)).size();
				helper.assertTrue(left == 0, spell.getId() + " set something going with only half its qi");
				helper.assertTrue(!SpellAuras.has(p, SpellAura.GOLDEN_BELL) && !SpellAuras.has(p, SpellAura.HARDENED_BODY), spell.getId() + " warded its caster unpaid");
				HeavenSpells.forget(p.getUUID());
				SpellAuras.forget(p);
			}
			helper.assertTrue(tookHold.isEmpty(), "No spell takes hold (nor takes qi) with half its qi (took hold: " + tookHold + ")");
			helper.succeed();
		} finally {
			HeavenSpellTesting.cleanUp(p, spawned, floor);
		}
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "spells_cooldown", timeoutTicks = 900)
	public void aSpellCoolingDownIsRefusedUnpaid(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 12, 12);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = master(helper, floor.centre());
		Vec3 home = p.position();
		Husk husk = mark(level, home.add(0, 0, 6), spawned);
		// Players' cooldowns are kept only with the gamerule on (off by default since 2026-10-24): on for these casts, all of them this tick.
		boolean rule = level.getGameRules().getBoolean(ModGameRules.SPELL_COOLDOWNS);
		level.getGameRules().getRule(ModGameRules.SPELL_COOLDOWNS).set(true, level.getServer());
		try {
			List<String> twice = new ArrayList<>();
			for (Spell spell : Spell.values()) {
				if (!spell.isHeaven() || spell.cooldownTicks() <= 0) continue;
				HeavenSpells.teleport(p, home);
				CultivationManager.get(p).setQi(CultivationManager.get(p).maxQi());
				Spells.setCooldown(p, spell, 0);
				HeavenSpellTesting.lookAt(p, HeavenSpells.middle(husk));
				Spells.cast(p, spell);
				if (Spells.cooldownLeft(p, spell) <= 0) {
					HeavenSpellTesting.lookAt(p, home.add(0, -1, 3));
					Spells.cast(p, spell);
				}
				if (Spells.cooldownLeft(p, spell) <= 0) continue; // refused here for its own reasons (everySpellTakesHold covers it)
				double qi = CultivationManager.get(p).getQi();
				Spells.cast(p, spell);
				if (CultivationManager.get(p).getQi() < qi - 1.0e-6) twice.add(spell.getId());
				HeavenSpells.forget(p.getUUID());
				SpellAuras.forget(p);
				sweep(level, home, 140);
			}
			level.getGameRules().getRule(ModGameRules.SPELL_COOLDOWNS).set(rule, level.getServer());
			helper.assertTrue(twice.isEmpty(), "A spell cast again while cooling down is refused, nothing paid (paid twice: " + twice + ")");
			HeavenSpellTesting.succeedOnceHealed(helper, p, spawned, floor);
		} catch (RuntimeException e) {
			level.getGameRules().getRule(ModGameRules.SPELL_COOLDOWNS).set(rule, level.getServer());
			HeavenSpellTesting.cleanUp(p, spawned, floor);
			throw e;
		}
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "spells_creative", timeoutTicks = 200)
	public void aCreativeCasterPaysNothing(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 14, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = master(helper, floor.centre());
		p.setGameMode(GameType.CREATIVE);
		Vec3 home = p.position();
		Husk husk = mark(level, home.add(0, 0, 7), spawned);
		List<String> refused = new ArrayList<>();
		try {
			List<String> paid = new ArrayList<>();
			List<String> threw = castAll(helper, p, home, husk, spell -> {
				for (int dx = -5; dx <= 5; dx++) {
					for (int dz = -3; dz <= 10; dz++) level.setBlock(floor.centre().offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 2);
				}
				// An ore in the floor for Mineral Purification to draw out (finding none, it takes no hold).
				if (spell == Spell.MINERAL_PURIFICATION) level.setBlock(floor.centre().offset(5, -1, -3), Blocks.IRON_ORE.defaultBlockState(), 2);
				CultivationManager.get(p).setQi(1);
			}, refused, spell -> {
				if (CultivationManager.get(p).getQi() < 1 - 1.0e-6) paid.add(spell.getId());
			});
			helper.assertTrue(threw.isEmpty(), "No spell throws for a creative caster (threw: " + threw + ")");
			helper.assertTrue(refused.isEmpty(), "A creative caster with no qi casts every spell (refused: " + refused + ")");
			helper.assertTrue(paid.isEmpty(), "...paying nothing (paid: " + paid + ")");
			helper.succeed();
		} finally {
			HeavenSpellTesting.cleanUp(p, spawned, floor);
		}
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "spells_two_casters", timeoutTicks = 200)
	public void twoCastersAtOnce(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 14, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer a = master(helper, floor.centre());
		ServerPlayer b = master(helper, floor.centre().offset(3, 0, 0));
		Vec3 homeA = a.position(), homeB = b.position();
		Husk husk = mark(level, homeA.add(1.5, 0, 7), spawned);
		try {
			List<String> threw = new ArrayList<>(), refused = new ArrayList<>();
			for (Spell spell : Spell.values()) {
				if (!spell.isHeaven()) continue;
				for (ServerPlayer p : new ServerPlayer[] {a, b}) {
					p.stopRiding();
					HeavenSpells.teleport(p, p == a ? homeA : homeB);
					CultivationManager.get(p).setQi(CultivationManager.get(p).maxQi());
					p.setHealth(p.getMaxHealth());
					Spells.setCooldown(p, spell, 0);
				}
				try {
					for (ServerPlayer p : new ServerPlayer[] {a, b}) {
						HeavenSpellTesting.lookAt(p, HeavenSpells.middle(husk));
						Spells.cast(p, spell);
						if (!tookHold(p, spell)) {
							HeavenSpellTesting.lookAt(p, (p == a ? homeA : homeB).add(0, -1, 3));
							Spells.cast(p, spell);
						}
						if (!tookHold(p, spell)) refused.add(spell.getId() + (p == a ? " (first)" : " (second)"));
					}
				} catch (RuntimeException e) {
					DefyingTheHeavens.LOGGER.error("[spell circumstances] {} threw with two casters", spell.getId(), e);
					threw.add(spell.getId() + " (" + e + ")");
				} finally {
					for (ServerPlayer p : new ServerPlayer[] {a, b}) {
						HeavenSpells.forget(p.getUUID());
						Spells.forget(p.getUUID());
						SpellAuras.forget(p);
					}
					sweep(level, homeA, 140);
					husk.setHealth(husk.getMaxHealth());
					husk.clearFire();
					for (int dx = -6; dx <= 9; dx++) {
						for (int dz = -3; dz <= 10; dz++) level.setBlock(floor.centre().offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 2);
					}
				}
			}
			DefyingTheHeavens.LOGGER.info("[spell circumstances] two casters, refused: {}", refused);
			helper.assertTrue(threw.isEmpty(), "No spell throws cast by two side by side at once (threw: " + threw + ")");
			helper.succeed();
		} finally {
			HeavenSpellTesting.cleanUp(a, spawned, floor);
			b.server.getPlayerList().remove(b);
		}
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "spells_nether", timeoutTicks = 400)
	public void everySpellInTheNether(GameTestHelper helper) {
		ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
		if (nether == null) {
			helper.succeed();
			return;
		}
		BlockPos centre = new BlockPos(40008, 100, 40008);
		nether.getChunk(centre.getX() >> 4, centre.getZ() >> 4);
		for (int dx = -12; dx <= 12; dx++) {
			for (int dz = -12; dz <= 12; dz++) {
				nether.setBlock(centre.offset(dx, -1, dz), Blocks.NETHERRACK.defaultBlockState(), 2);
				for (int dy = 0; dy < 14; dy++) nether.setBlock(centre.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
			}
		}
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = master(helper, helper.absolutePos(new BlockPos(1, 2, 1)));
		p.teleportTo(nether, centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5, 0, 0);
		p.setOnGround(true);
		Vec3 home = p.position();
		Husk husk = mark(nether, home.add(0, 0, 6), spawned);
		List<String> refused = new ArrayList<>();
		try {
			List<String> threw = castAll(helper, p, home, husk, spell -> {
				for (int dx = -5; dx <= 5; dx++) {
					for (int dz = -3; dz <= 10; dz++) nether.setBlock(centre.offset(dx, -1, dz), Blocks.NETHERRACK.defaultBlockState(), 2);
				}
			}, refused);
			DefyingTheHeavens.LOGGER.info("[spell circumstances] in the Nether, refused: {}", refused);
			helper.assertTrue(threw.isEmpty(), "No spell throws cast in the Nether (threw: " + threw + ")");
			helper.assertTrue(refused.size() <= 10, "Nearly every spell takes hold in the Nether (refused: " + refused + ")");
			helper.succeed();
		} finally {
			Spells.forget(p.getUUID());
			for (LivingEntity e : spawned) e.discard();
			p.server.getPlayerList().remove(p);
			for (int dx = -12; dx <= 12; dx++) {
				for (int dz = -12; dz <= 12; dz++) {
					for (int dy = -1; dy < 14; dy++) nether.setBlock(centre.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
	}
}
