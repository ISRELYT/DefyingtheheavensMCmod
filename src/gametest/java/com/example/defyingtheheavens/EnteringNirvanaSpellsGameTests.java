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
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Entering Nirvana spells (2026-10-24: death and rebirth; the ghost foundation's Black and White Impermanence and Book of Life and Death,
 * and the Heaven-Demon Eclipse of both paths), each cast by a caster of that realm on a floor high over its spot: its shapes come
 * (SpellConstruct), it strikes whom it should and spares whom it should, and a player's works the land as it says (the ghost spells leave it
 * be); a cultivator's leaves the land whole. Each in a batch of its own.
 */
public class EnteringNirvanaSpellsGameTests implements FabricGameTest {
	/** The husks' health: the realm's blows are heavy (6500 at the most), and a husk struck dead is gone before it can be weighed. */
	private static final float HEALTH = 30000; // more than three times its caster's: a great beast to it (HeavenSpells#GREAT_BEYOND)

	/** An Entering Nirvana Grand Perfection caster who knows {@code element}'s method of that realm to its last layer, at {@code at}. */
	private static ServerPlayer caster(GameTestHelper helper, QiElement element, BlockPos at) {
		ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.FIRE_FOUR_AXIS, at);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION, 0);
		HeavenMethod method = HeavenMethod.of(element, Realm.ENTERING_NIRVANA);
		c.heaven().setMethod(method, method.layers(), 0);
		CultivationManager.refresh(p);
		c.setQi(c.maxQi());
		p.setHealth(p.getMaxHealth());
		return p;
	}

	/** A hardy husk to be struck ({@link #HEALTH}, fire-proof against burning's own harm), standing at {@code at}. */
	private static Husk husk(ServerLevel level, Vec3 at, List<LivingEntity> spawned) {
		Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, at, HEALTH, spawned);
		husk.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 600, 0, false, false));
		return husk;
	}

	/** A husk that can be moved, to see a spell drag or hold (VoidShatteringSpellsGameTests#mobileHusk): its wits kept, its goals taken, unhurtable. */
	private static Husk mobileHusk(ServerLevel level, Vec3 at, List<LivingEntity> spawned) {
		Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, at, 200, spawned);
		husk.setInvulnerable(true);
		husk.setNoAi(false);
		try {
			for (String name : new String[] {"goalSelector", "targetSelector"}) {
				java.lang.reflect.Field field = net.minecraft.world.entity.Mob.class.getDeclaredField(name);
				field.setAccessible(true);
				((net.minecraft.world.entity.ai.goal.GoalSelector) field.get(husk)).removeAllGoals(g -> true);
			}
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
		return husk;
	}

	/** How much of the floor's stone (its one layer under {@code centre}) is left within {@code radius} of {@code at} (level, by column). */
	private static int floorLeft(ServerLevel level, BlockPos centre, Vec3 at, double radius) {
		int n = 0;
		int r = (int) Math.ceil(radius);
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				BlockPos pos = new BlockPos((int) Math.floor(at.x) + dx, centre.getY() - 1, (int) Math.floor(at.z) + dz);
				if (dx * dx + dz * dz <= radius * radius && level.getBlockState(pos).is(Blocks.STONE)) n++;
			}
		}
		return n;
	}

	private static List<SpellConstruct> constructs(ServerLevel level, Vec3 at, double radius, SpellConstruct.Kind kind) {
		return level.getEntitiesOfClass(SpellConstruct.class, new AABB(at, at).inflate(radius), c -> c.kind() == kind);
	}

	private static void finish(GameTestHelper helper, ServerPlayer p, List<LivingEntity> spawned, HeavenSpellTesting.Floor floor) {
		HeavenSpells.forget(p.getUUID());
		for (SpellConstruct c : helper.getLevel().getEntitiesOfClass(SpellConstruct.class, new AABB(floor.centre()).inflate(200))) c.discard();
		HeavenSpellTesting.cleanUp(p, spawned, floor);
	}

	/** Ends a test whose spell wounds the land once its wounds are cut and drained (HeavenSpellTesting#succeedOnceHealed), its shapes gone first. */
	private static void finishOnceHealed(GameTestHelper helper, ServerPlayer p, List<LivingEntity> spawned, HeavenSpellTesting.Floor floor, Runnable after) {
		HeavenSpells.forget(p.getUUID());
		for (SpellConstruct c : helper.getLevel().getEntitiesOfClass(SpellConstruct.class, new AABB(floor.centre()).inflate(200))) c.discard();
		HeavenSpellTesting.succeedOnceHealed(helper, p, spawned, floor, after);
	}

	/** Whether the floor round {@code at} is as it was, asked only where no wound was being cut as the test began (DaoIntegrationSpellsGameTests#landKept). */
	private static boolean landKept(ServerLevel level, HeavenSpellTesting.Floor floor, Vec3 at, double radius, int stone, boolean quiet) {
		return !quiet || floorLeft(level, floor.centre(), at, radius) == stone;
	}

	/** Its spells come at the third and seventh layers of its realm, and are as great as their realm makes them (the user's rule, 2026-10-24). */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void theEnteringNirvanaSpellsComeInTheirPlaceAndGrow(GameTestHelper helper) {
		Spell[][] pairs = {{Spell.SEED_OF_WITHERING_REBIRTH, Spell.WITHERING_AND_FLOURISHING}, {Spell.UNDYING_EMBER, Spell.PYRE_OF_THE_HEAVENS},
				{Spell.GRAVESTONE_FALLS, Spell.BURIED_WORLD}, {Spell.FROZEN_MOON_IN_THE_WELL, Spell.THE_FROZEN_MOON_FALLS},
				{Spell.SWORD_TOMB_AWAKENS, Spell.NINE_REBIRTHS_OF_THE_SWORD}, {Spell.BLACK_AND_WHITE_IMPERMANENCE, Spell.BOOK_OF_LIFE_AND_DEATH}};
		for (Spell[] pair : pairs) {
			helper.assertTrue(pair[0].layer() == 84 && pair[1].layer() == 88, pair[0].getId() + " and " + pair[1].getId() + " at layers 84 and 88");
			for (Spell spell : pair) helper.assertTrue(spell.realm() == Realm.ENTERING_NIRVANA, spell.getId() + " is of Entering Nirvana");
		}
		helper.assertTrue(Spell.HEAVEN_DEMON_ECLIPSE.isDual() && Spell.HEAVEN_DEMON_ECLIPSE.dualRealm() == Realm.ENTERING_NIRVANA, "The Heaven-Demon Eclipse is both paths', of Entering Nirvana");
		helper.assertTrue(WoodSpells.SEED_RANGE > WoodSpells.GOURD_RANGE && FireSpells.PYRE_RANGE > FireSpells.FURNACE_RANGE && EarthSpells.GRAVE_RANGE > EarthSpells.CAULDRONS_RANGE
				&& WaterSpells.FALLS_RANGE > WaterSpells.VASE_RANGE && MetalSpells.REBIRTH_RANGE > MetalSpells.SCISSORS_RANGE && GhostSpells.IMPERMANENCE_RANGE > GhostSpells.MIRROR_RANGE
				&& HeavenDemonSpells.ECLIPSE_RANGE > HeavenDemonSpells.BELL_RANGE, "Entering Nirvana's reach is greater than Sacred Vessel's");
		SpellConstruct shape = SpellConstruct.create(helper.getLevel(), SpellConstruct.Kind.ICE_MOON, Vec3.ZERO, 0, 0, 20, null).sizedFor(Spell.THE_FROZEN_MOON_FALLS);
		helper.assertTrue(HeavenSpellTesting.near(shape.size(), HeavenSpells.greatness(Realm.ENTERING_NIRVANA)) && shape.size() > HeavenSpells.greatness(Realm.SACRED_VESSEL),
				"...and its shapes drawn greater (" + shape.size() + ")");
		shape.discard();
		for (SpellConstruct.Kind kind : new SpellConstruct.Kind[] {SpellConstruct.Kind.REBIRTH_SEED, SpellConstruct.Kind.SEASON_WHEEL, SpellConstruct.Kind.UNDYING_EMBER,
				SpellConstruct.Kind.HEAVEN_PYRE, SpellConstruct.Kind.GRAVESTONE, SpellConstruct.Kind.STANDING_STONE, SpellConstruct.Kind.FROZEN_MOON, SpellConstruct.Kind.ICE_MOON,
				SpellConstruct.Kind.SWORD_TOMB, SpellConstruct.Kind.REBIRTH_SWORD, SpellConstruct.Kind.WHITE_IMPERMANENCE, SpellConstruct.Kind.BLACK_IMPERMANENCE,
				SpellConstruct.Kind.LIFE_DEATH_BOOK, SpellConstruct.Kind.HEAVEN_DEMON_ECLIPSE}) {
			helper.assertTrue(kind.modelled(), kind + " is a model of its own");
		}
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "nirvana_seed", timeoutTicks = 400)
	public void theSeedRootsDrinksAndBlooms(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 36, 16);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.WOOD, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 16);
		Husk rooted = husk(level, point, spawned), near = husk(level, point.add(6, 0, 0), spawned), beyond = husk(level, point.add(-(WoodSpells.SEED_BURST + 6), 0, 0), spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(rooted));
		p.setHealth(p.getMaxHealth() / 2);
		float hurt = p.getHealth();
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.SEED_OF_WITHERING_REBIRTH), "Seed of Withering Rebirth is cast, paid for");
		helper.assertTrue(constructs(level, home, 6, SpellConstruct.Kind.REBIRTH_SEED).size() == 1, "...its seed in its caster's hands");
		helper.runAfterDelay(WoodSpells.SEED_FLIGHT + 52, () -> {
			boolean bound = HeavenSpells.lingering(p.getUUID(), WoodSpells.Bound.class).stream().anyMatch(b -> b.target == rooted);
			String wrong = !bound ? "It roots in what it struck, binding it"
					: rooted.getHealth() > HEALTH - 2 * WoodSpells.SEED_DRAIN + 1 ? "...drinking it each second (" + rooted.getHealth() + ")"
					: p.getHealth() <= hurt ? "...half of it mending its caster (" + hurt + " -> " + p.getHealth() + ")"
					: near.getHealth() < near.getMaxHealth() ? "...nothing else drunk" : null;
			if (wrong != null) {
				finish(helper, p, spawned, floor);
				helper.fail(wrong);
				return;
			}
			helper.runAfterDelay(WoodSpells.SEED_ROOTS - 52 + 3, () -> {
				try {
					helper.assertTrue(rooted.getHealth() <= HEALTH - 5 * WoodSpells.SEED_DRAIN - WoodSpells.SEED_BLOOM + 1, "Then it blooms out of it (" + rooted.getHealth() + ")");
					helper.assertTrue(near.getHealth() <= HEALTH - WoodSpells.SEED_BLOOM_AROUND + 1, "...its burst striking what stands near (" + near.getHealth() + ")");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond it");
				} catch (RuntimeException e) {
					finish(helper, p, spawned, floor);
					throw e;
				}
				finishOnceHealed(helper, p, spawned, floor, () -> {});
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "nirvana_seasons", timeoutTicks = 400)
	public void theWheelOfTheSeasonsDrainsAndTurns(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 52, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.WOOD, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 14);
		Husk drained = husk(level, point.add(8, 0, 4), spawned), beyond = husk(level, point.add(0, 0, WoodSpells.SEASONS_RADIUS + 5), spawned);
		HeavenSpellTesting.lookAt(p, point);
		p.setHealth(p.getMaxHealth() / 2);
		float hurt = p.getHealth();
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.WITHERING_AND_FLOURISHING), "Withering and Flourishing is cast, paid for");
		helper.assertTrue(constructs(level, point, 6, SpellConstruct.Kind.SEASON_WHEEL).size() == 1, "...its wheel grows out of the land where its caster looks");
		helper.runAfterDelay(WoodSpells.SEASONS_GROWS + 52, () -> {
			if (drained.getHealth() > HEALTH - 2 * WoodSpells.SEASONS_DRAIN + 1 || p.getHealth() <= hurt) {
				finish(helper, p, spawned, floor);
				helper.fail("Every foe on it is drained each second, a share of it mending its caster (" + drained.getHealth() + ", " + hurt + " -> " + p.getHealth() + ")");
				return;
			}
			helper.runAfterDelay(WoodSpells.SEASONS_TICKS - 52 + 3, () -> {
				try {
					helper.assertTrue(drained.getHealth() <= HEALTH - 8 * WoodSpells.SEASONS_DRAIN - WoodSpells.SEASONS_TURN + 1, "...then its last turn strikes them (" + drained.getHealth() + ")");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands off the wheel");
				} catch (RuntimeException e) {
					finish(helper, p, spawned, floor);
					throw e;
				}
				finishOnceHealed(helper, p, spawned, floor, () -> {});
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "nirvana_ember", timeoutTicks = 400)
	public void theUndyingEmberBurstsThreeTimes(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 40, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.FIRE, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 18);
		Husk under = husk(level, point, spawned), beyond = husk(level, point.add(FireSpells.EMBER_BLAST[2] + 6, 0, 0), spawned);
		Runnable thinned = HeavenSpellTesting.thicken(floor, point.x, point.z, 14, 10);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(under));
		int stone = floorLeft(level, floor.centre(), point, 5);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.UNDYING_EMBER), "Undying Ember is cast, paid for");
		helper.runAfterDelay(FireSpells.EMBER_FLIGHT + 2 * FireSpells.EMBER_EVERY + 3, () -> {
			try {
				float all = FireSpells.EMBER_DAMAGE[0] + FireSpells.EMBER_DAMAGE[1] + FireSpells.EMBER_DAMAGE[2];
				helper.assertTrue(under.getHealth() <= HEALTH - all + 1, "It bursts three times on what stands under it, each greater (" + under.getHealth() + ")");
				helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not reaching what stands beyond its greatest");
				helper.assertTrue(floorLeft(level, floor.centre(), point, 5) < stone, "...the land blown into a crater");
			} catch (RuntimeException e) {
				thinned.run();
				finish(helper, p, spawned, floor);
				throw e;
			}
			finishOnceHealed(helper, p, spawned, floor, thinned);
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "nirvana_pyre", timeoutTicks = 500)
	public void thePyreTrapsBurnsAndCollapses(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 56, 16);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.FIRE, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 8);
		Husk inside = husk(level, point.add(6, 0, 0), spawned), leaving = mobileHusk(level, point.add(FireSpells.PYRE_RADIUS * 1.02, 0, 0), spawned),
				beyond = husk(level, point.add(0, 0, -(FireSpells.PYRE_RADIUS + 10)), spawned);
		Runnable thinned = HeavenSpellTesting.thicken(floor, point.x, point.z, (int) FireSpells.PYRE_CRATER + 2, 14);
		HeavenSpellTesting.lookAt(p, point);
		int stone = floorLeft(level, floor.centre(), point, 8);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.PYRE_OF_THE_HEAVENS), "Pyre of the Heavens is cast, paid for");
		helper.runAfterDelay(FireSpells.PYRE_RISES + 52, () -> {
			double off = Math.hypot(leaving.getX() - point.x, leaving.getZ() - point.z);
			float burnt = 3 * FireSpells.PYRE_BURN + 3 * FireSpells.PYRE_RAMP;
			if (inside.getHealth() > HEALTH - burnt + 1 || off > FireSpells.PYRE_RADIUS * 1.02) {
				thinned.run();
				finish(helper, p, spawned, floor);
				helper.fail("It burns every foe within, more each second (" + inside.getHealth() + "), and throws back in who would pass its walls (" + off + " off)");
				return;
			}
			helper.runAfterDelay(FireSpells.PYRE_TICKS - 52 + FireSpells.PYRE_FALLS + 3, () -> {
				try {
					helper.assertTrue(inside.getHealth() <= HEALTH - burnt - FireSpells.PYRE_COLLAPSE + 1, "...then it collapses in on them (" + inside.getHealth() + ")");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands outside it");
					helper.assertTrue(floorLeft(level, floor.centre(), point, 8) < stone, "...the land blown into a vast crater");
				} catch (RuntimeException e) {
					thinned.run();
					finish(helper, p, spawned, floor);
					throw e;
				}
				finishOnceHealed(helper, p, spawned, floor, thinned);
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "nirvana_grave", timeoutTicks = 400)
	public void theGravestoneFallsAndSinks(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 44, 16);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.EARTH, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 18);
		Husk crushed = husk(level, point, spawned), dragged = mobileHusk(level, point.add(EarthSpells.GRAVE_PULL * 0.7, 0, 0), spawned),
				beyond = husk(level, point.add(0, 0, EarthSpells.GRAVE_PULL + 6), spawned);
		Runnable thinned = HeavenSpellTesting.thicken(floor, point.x, point.z, (int) EarthSpells.GRAVE_CRATER + 4, 18);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(crushed));
		int stone = floorLeft(level, floor.centre(), point, 6);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.GRAVESTONE_FALLS), "Gravestone Falls is cast, paid for");
		helper.runAfterDelay(EarthSpells.GRAVE_FALL + 4, () -> {
			boolean pinned = HeavenSpells.lingering(p.getUUID(), WoodSpells.Bound.class).stream().anyMatch(b -> b.target == crushed);
			if (crushed.getHealth() > HEALTH - EarthSpells.GRAVE_DAMAGE + 1 || !pinned || floorLeft(level, floor.centre(), point, 6) >= stone) {
				thinned.run();
				finish(helper, p, spawned, floor);
				helper.fail("It falls on what stands there, pinning it, smashing the land (" + crushed.getHealth() + ", pinned " + pinned + ")");
				return;
			}
			double was = Math.hypot(dragged.getX() - point.x, dragged.getZ() - point.z);
			helper.runAfterDelay(EarthSpells.GRAVE_STANDS + EarthSpells.GRAVE_SINKS - 4 + 3, () -> {
				try {
					double now = Math.hypot(dragged.getX() - point.x, dragged.getZ() - point.z);
					helper.assertTrue(now < was - 3, "Sinking, it drags those near to it (" + was + " -> " + now + ")");
					helper.assertTrue(crushed.getHealth() <= HEALTH - EarthSpells.GRAVE_DAMAGE - EarthSpells.GRAVE_BURIES + 1, "...and buries what is by it (" + crushed.getHealth() + ")");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond");
				} catch (RuntimeException e) {
					thinned.run();
					finish(helper, p, spawned, floor);
					throw e;
				}
				finishOnceHealed(helper, p, spawned, floor, thinned);
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "nirvana_buried", timeoutTicks = 500)
	public void theBuriedWorldSinksAndItsStonesFall(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 52, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.EARTH, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 8);
		Husk sunk = husk(level, point.add(-8, 0, 6), spawned), beyond = husk(level, point.add(0, 0, -(EarthSpells.BURIED_RADIUS + 8)), spawned);
		// Ground under it deep enough for all its sinking.
		Runnable thinned = HeavenSpellTesting.thicken(floor, point.x, point.z, (int) EarthSpells.BURIED_RADIUS + 1, 28);
		HeavenSpellTesting.lookAt(p, point);
		int stone = floorLeft(level, floor.centre(), point.add(-8, 0, 6), 4);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.BURIED_WORLD), "Buried World is cast, paid for");
		helper.assertTrue(constructs(level, point, EarthSpells.BURIED_RADIUS + 6, SpellConstruct.Kind.MENHIR_MASS).size() == EarthSpells.BURIED_STONES, "...its ring of standing stones rises");
		helper.runAfterDelay(EarthSpells.BURIED_RISES + 52, () -> {
			if (sunk.getHealth() > HEALTH - 2 * EarthSpells.BURIED_CRUSH + 1 || floorLeft(level, floor.centre(), point.add(-8, 0, 6), 4) >= stone) {
				thinned.run();
				finish(helper, p, spawned, floor);
				helper.fail("The land within sinks, crushing every foe within each second (" + sunk.getHealth() + ")");
				return;
			}
			helper.runAfterDelay(EarthSpells.BURIED_TICKS - 52 + EarthSpells.BURIED_FALLS + 3, () -> {
				try {
					helper.assertTrue(sunk.getHealth() <= HEALTH - 6 * EarthSpells.BURIED_CRUSH - EarthSpells.BURIED_TOPPLE + 1, "...then its stones topple in on them (" + sunk.getHealth() + ")");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands outside the ring");
				} catch (RuntimeException e) {
					thinned.run();
					finish(helper, p, spawned, floor);
					throw e;
				}
				finishOnceHealed(helper, p, spawned, floor, thinned);
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "nirvana_well", timeoutTicks = 400)
	public void theFrozenMoonHoldsThemInTheWell(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 40, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.WATER, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 16);
		Husk held = husk(level, point.add(4, 0, 0), spawned), frozen = mobileHusk(level, point.add(-6, 0, 3), spawned), beyond = husk(level, point.add(WaterSpells.WELL_RADIUS + 5, 0, 0), spawned);
		HeavenSpellTesting.lookAt(p, point);
		boolean quiet = Devastation.pending() == 0;
		int stone = floorLeft(level, floor.centre(), point, 10);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.FROZEN_MOON_IN_THE_WELL), "Frozen Moon in the Well is cast, paid for");
		helper.runAfterDelay(WaterSpells.WELL_RISES + 30, () -> {
			Vec3 at = frozen.position();
			frozen.setDeltaMovement(1.5, 0, 0);
			helper.runAfterDelay(4, () -> {
				// A tick's push may land before the well next holds it (whichever ticks first): no more than that.
				double moved = frozen.position().distanceTo(at);
				if (moved > 2 || held.getHealth() > HEALTH - WaterSpells.WELL_DAMAGE + 1 || !held.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
					finish(helper, p, spawned, floor);
					helper.fail("What stands in the well is held frozen in it (" + moved + " moved) and struck each second (" + held.getHealth() + ")");
					return;
				}
				helper.runAfterDelay(WaterSpells.WELL_TICKS - 34 + 3, () -> {
					try {
						helper.assertTrue(held.getHealth() <= HEALTH - 4 * WaterSpells.WELL_DAMAGE - WaterSpells.WELL_SHATTER + 1, "...then the moon shatters on them (" + held.getHealth() + ")");
						helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond the well");
						helper.assertTrue(landKept(level, floor, point, 10, stone, quiet), "...and nothing of the land is taken");
						helper.succeed();
					} finally {
						finish(helper, p, spawned, floor);
					}
				});
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "nirvana_falls", timeoutTicks = 300)
	public void theFrozenMoonFalls(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 48, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.WATER, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 8);
		Husk struck = husk(level, point.add(6, 0, 0), spawned), chilled = husk(level, point.add(0, 0, WaterSpells.FALLS_RADIUS + 6), spawned),
				beyond = husk(level, point.add(-(WaterSpells.FALLS_FROST + 5), 0, 0), spawned);
		Runnable thinned = HeavenSpellTesting.thicken(floor, point.x, point.z, (int) WaterSpells.FALLS_CRATER + 2, 18);
		HeavenSpellTesting.lookAt(p, point);
		int stone = floorLeft(level, floor.centre(), point, 10);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.THE_FROZEN_MOON_FALLS), "The Frozen Moon Falls is cast, paid for");
		helper.assertTrue(constructs(level, point.add(0, WaterSpells.FALLS_HIGH, 0), 6, SpellConstruct.Kind.MOON_MASS).size() == 1, "...its moon high over the spot");
		helper.runAfterDelay(WaterSpells.FALLS_FALL + 3, () -> {
			try {
				helper.assertTrue(struck.getHealth() <= HEALTH - WaterSpells.FALLS_DAMAGE + 1, "It falls on what stands under it (" + struck.getHealth() + ")");
				helper.assertTrue(chilled.getHealth() == chilled.getMaxHealth() && chilled.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "...its frost chilling those further out");
				helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth() && !beyond.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "...not those beyond it");
				helper.assertTrue(floorLeft(level, floor.centre(), point, 10) < stone, "...the land smashed into a crater of ice");
			} catch (RuntimeException e) {
				thinned.run();
				finish(helper, p, spawned, floor);
				throw e;
			}
			finishOnceHealed(helper, p, spawned, floor, thinned);
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "nirvana_tombs", timeoutTicks = 300)
	public void theSwordTombsEruptAndRainOnItsHeart(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 40, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.METAL, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 18);
		Husk heart = husk(level, point, spawned), edge = husk(level, point.add(MetalSpells.TOMB_RING - 3, 0, 0), spawned),
				beyond = husk(level, point.add(-MetalSpells.TOMB_RING - 6, 0, 0), spawned);
		Runnable thinned = HeavenSpellTesting.thicken(floor, point.x, point.z, (int) MetalSpells.TOMB_CRATER + 2, 10);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(heart));
		int stone = floorLeft(level, floor.centre(), point, 3);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.SWORD_TOMB_AWAKENS), "Sword Tomb Awakens is cast, paid for");
		helper.runAfterDelay(MetalSpells.TOMB_ERUPTS / 2 + 4, () -> {
			int graves = constructs(level, point, 20, SpellConstruct.Kind.SWORD_TOMB).size();
			if (graves != 1 || heart.getHealth() > HEALTH - MetalSpells.TOMB_DAMAGE + 1 || edge.getHealth() > HEALTH - MetalSpells.TOMB_DAMAGE + 1) {
				thinned.run();
				finish(helper, p, spawned, floor);
				helper.fail("Its ring of swords bursts up round where its caster looks (" + graves + "), striking all within it (" + heart.getHealth() + ", " + edge.getHealth() + ")");
				return;
			}
			helper.runAfterDelay(MetalSpells.TOMB_ERUPTS / 2 + MetalSpells.TOMB_STANDS + MetalSpells.TOMB_RISES + MetalSpells.TOMB_PLUNGE + MetalSpells.TOMB_SWORDS * MetalSpells.TOMB_EVERY, () -> {
				try {
					helper.assertTrue(heart.getHealth() <= HEALTH - MetalSpells.TOMB_DAMAGE - MetalSpells.TOMB_SWORDS * MetalSpells.TOMB_SWORD * 0.75f,
							"Then its swords rain down on its heart (" + heart.getHealth() + ")");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not on what stands beyond its ring");
					helper.assertTrue(floorLeft(level, floor.centre(), point, 3) < stone, "...the last leaving a crater");
				} catch (RuntimeException e) {
					thinned.run();
					finish(helper, p, spawned, floor);
					throw e;
				}
				finishOnceHealed(helper, p, spawned, floor, thinned);
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "nirvana_rebirths", timeoutTicks = 400)
	public void theSwordIsRebornNineTimes(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 56, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.METAL, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 10);
		// One at its heart, under its hilt; one out by its point, past where its plunge reaches; one beyond its blade.
		Husk heart = husk(level, point, spawned), swept = husk(level, point.add(MetalSpells.REBIRTH_REACH * 0.6, 0, 0), spawned),
				beyond = husk(level, point.add(0, 0, -MetalSpells.REBIRTH_REACH - 6), spawned);
		Runnable thinned = HeavenSpellTesting.thicken(floor, point.x, point.z, (int) MetalSpells.REBIRTH_CRATER + 2, 10);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(heart));
		int stone = floorLeft(level, floor.centre(), point, 3);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.NINE_REBIRTHS_OF_THE_SWORD), "Nine Rebirths of the Sword is cast, paid for");
		int sweeps = MetalSpells.REBIRTH_FORMS + MetalSpells.REBIRTHS * (MetalSpells.REBIRTH_SWEEP + MetalSpells.REBIRTH_REBORN);
		helper.runAfterDelay(sweeps + MetalSpells.REBIRTH_RISES + MetalSpells.REBIRTH_FALL + 4, () -> {
			try {
				helper.assertTrue(heart.getHealth() <= HEALTH - MetalSpells.REBIRTHS * MetalSpells.REBIRTH_DAMAGE - MetalSpells.REBIRTH_PLUNGE + 1,
						"Each of its eight sweeps passes over its heart, and its ninth rebirth plunges into it (" + heart.getHealth() + ")");
				helper.assertTrue(swept.getHealth() <= HEALTH - MetalSpells.REBIRTH_DAMAGE + 1 && swept.getHealth() > HEALTH - MetalSpells.REBIRTHS * MetalSpells.REBIRTH_DAMAGE - 1,
						"...its sweeps reaching out to what stands by its point, not every one of them (" + swept.getHealth() + ")");
				helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond its blade");
				helper.assertTrue(floorLeft(level, floor.centre(), point, 3) < stone, "...the land glassed into a crater where it plunged");
			} catch (RuntimeException e) {
				thinned.run();
				finish(helper, p, spawned, floor);
				throw e;
			}
			finishOnceHealed(helper, p, spawned, floor, thinned);
		});
	}

	/** A living caster whose ghost path stands at Entering Nirvana Grand Perfection, its foundations understood to the Underworld Nirvana Scripture. */
	private static ServerPlayer ghostCaster(GameTestHelper helper, BlockPos at) {
		ServerPlayer p = caster(helper, QiElement.WATER, at);
		NetherworldState s = CultivationManager.get(p).netherworld();
		for (GhostMethod method : GhostMethod.values()) {
			if (method.isFoundation()) s.setMethod(method, method.realm().ordinal() <= Realm.ENTERING_NIRVANA.ordinal() ? 1 : 0, 0);
		}
		GhostMethods.setPath(s, PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION));
		CultivationManager.refresh(p);
		s.setGhostQi(GhostQi.poolMax(CultivationManager.get(p)));
		return p;
	}

	/** Casts {@code spell} afresh: true if it took hold (its cooldown began). */
	private static boolean took(ServerPlayer p, Spell spell) {
		Spells.setCooldown(p, spell, 0);
		Spells.cast(p, spell);
		return Spells.cooldownLeft(p, spell) > 0;
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "nirvana_impermanence", timeoutTicks = 300)
	public void theImpermanencesComeForTheSoul(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 36, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = ghostCaster(helper, floor.centre());
		Vec3 home = p.position();
		Husk soul = husk(level, home.add(0, 0, 18), spawned), other = husk(level, home.add(10, 0, 22), spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(soul));
		helper.assertTrue(Spell.BLACK_AND_WHITE_IMPERMANENCE.isKnown(CultivationManager.get(p)), "Its ghost path carries the Underworld Nirvana Scripture to its spell");
		helper.assertTrue(took(p, Spell.BLACK_AND_WHITE_IMPERMANENCE), "Black and White Impermanence is cast");
		helper.assertTrue(constructs(level, soul.position(), GhostSpells.IMPERMANENCE_BESIDE + 3, SpellConstruct.Kind.WHITE_IMPERMANENCE_FIGURE).size() == 1
				&& constructs(level, soul.position(), GhostSpells.IMPERMANENCE_BESIDE + 3, SpellConstruct.Kind.BLACK_IMPERMANENCE_FIGURE).size() == 1, "...the White and the Black rise to either side of it");
		helper.runAfterDelay(GhostSpells.IMPERMANENCE_COMES + GhostSpells.IMPERMANENCE_HOOK + GhostSpells.IMPERMANENCE_HOLDS + 3, () -> {
			try {
				float share = Math.min(GhostSpells.IMPERMANENCE_MOST, HEALTH * GhostSpells.IMPERMANENCE_SHARE);
				helper.assertTrue(soul.getHealth() <= HEALTH - GhostSpells.IMPERMANENCE_STRIKE - 3 * GhostSpells.IMPERMANENCE_CHAIN - share + 1,
						"The White strikes it, the Black's chain holds it, and they take their share of it (" + soul.getHealth() + ")");
				helper.assertTrue(other.getHealth() == other.getMaxHealth(), "...none of the others");
				helper.succeed();
			} finally {
				finish(helper, p, spawned, floor);
			}
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "nirvana_book", timeoutTicks = 300)
	public void theBookStrikesTheirNamesOut(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 40, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = ghostCaster(helper, floor.centre());
		Vec3 home = p.position();
		Husk marked = husk(level, home.add(0, 0, 12), spawned), failing = husk(level, home.add(-10, 0, 6), spawned), beyond = husk(level, home.add(0, 0, -(GhostSpells.BOOK_REACH + 6)), spawned);
		failing.setHealth(2400);
		helper.assertTrue(took(p, Spell.BOOK_OF_LIFE_AND_DEATH), "The Book of Life and Death is cast");
		helper.assertTrue(constructs(level, home, 12, SpellConstruct.Kind.LIFE_DEATH_BOOK).size() == 1, "...its book opens over its caster's head");
		helper.runAfterDelay(GhostSpells.BOOK_OPENS + 4, () -> {
			if (!marked.hasEffect(MobEffects.GLOWING) || !failing.hasEffect(MobEffects.GLOWING)) {
				finish(helper, p, spawned, floor);
				helper.fail("It finds the names of the foes round its caster, marking them");
				return;
			}
			helper.runAfterDelay(GhostSpells.BOOK_WRITES + GhostSpells.BOOK_STROKE - 4 + 3, () -> {
				try {
					helper.assertTrue(marked.getHealth() <= HEALTH - 3 * GhostSpells.BOOK_MARK - GhostSpells.BOOK_STRIKE + 1, "...wears at them, then strikes their names out (" + marked.getHealth() + ")");
					helper.assertTrue(!failing.isAlive() || failing.getHealth() <= 0, "...one left near the end of its span struck out of life (" + failing.getHealth() + ")");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not one beyond its reach");
					helper.succeed();
				} finally {
					finish(helper, p, spawned, floor);
				}
			});
		});
	}

	/** A tiger demon of Entering Nirvana Grand Perfection on the heaven path and the Demon Path both. */
	private static ServerPlayer demonCaster(GameTestHelper helper, BlockPos at) {
		ServerPlayer p = caster(helper, QiElement.METAL, at);
		PlayerCultivation c = CultivationManager.get(p);
		c.demon().setBloodline(Bloodline.TIGER);
		c.demon().set(PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION), 0);
		CultivationManager.refresh(p);
		c.setQi(c.maxQi());
		p.setHealth(p.getMaxHealth());
		return p;
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "nirvana_eclipse", timeoutTicks = 300)
	public void theEclipseOfBothPathsPoursDarknessDown(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 40, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = demonCaster(helper, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 18);
		Husk under = husk(level, point, spawned), beyond = husk(level, point.add(HeavenDemonSpells.ECLIPSE_RADIUS + 6, 0, 0), spawned);
		Runnable thinned = HeavenSpellTesting.thicken(floor, point.x, point.z, (int) HeavenDemonSpells.ECLIPSE_CRATER + 2, 10);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(under));
		p.setHealth(p.getMaxHealth() / 2);
		float hurt = p.getHealth();
		int stone = floorLeft(level, floor.centre(), point, 8);
		helper.assertTrue(Spell.HEAVEN_DEMON_ECLIPSE.isKnown(CultivationManager.get(p)), "Both paths at Entering Nirvana: the Eclipse is theirs");
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.HEAVEN_DEMON_ECLIPSE), "The Heaven-Demon Eclipse is cast, paid for in qi");
		helper.assertTrue(constructs(level, point.add(0, HeavenDemonSpells.ECLIPSE_HIGH, 0), 6, SpellConstruct.Kind.ECLIPSE).size() == 1, "...sun and moon high over the spot");
		helper.runAfterDelay(HeavenDemonSpells.ECLIPSE_MEETS + 2 * HeavenDemonSpells.ECLIPSE_EVERY + 3, () -> {
			try {
				float all = HeavenDemonSpells.ECLIPSE_PULSES[0] + HeavenDemonSpells.ECLIPSE_PULSES[1] + HeavenDemonSpells.ECLIPSE_PULSES[2];
				helper.assertTrue(under.getHealth() <= HEALTH - all + 1, "Its beam pulses three times on what stands under it (" + under.getHealth() + ")");
				helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond it");
				helper.assertTrue(p.getHealth() > hurt, "...a share of it mending its caster (" + hurt + " -> " + p.getHealth() + ")");
				helper.assertTrue(floorLeft(level, floor.centre(), point, 8) < stone, "...the land burnt into a crater");
			} catch (RuntimeException e) {
				thinned.run();
				finish(helper, p, spawned, floor);
				throw e;
			}
			finishOnceHealed(helper, p, spawned, floor, thinned);
		});
	}

	/** A cultivator's Entering Nirvana spells strike as a player's do but leave the land whole (HeavenSpells#carvesLand). */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "nirvana_cultivator", timeoutTicks = 300)
	public void aCultivatorsNirvanaSpellsLeaveTheLand(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 36, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer watcher = caster(helper, QiElement.METAL, floor.centre().offset(-30, 0, -30));
		BlockPos at = floor.centre();
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, helper.relativePos(at));
		npc.setNoAi(true);
		npc.setRank(PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.EARLY));
		npc.setHealth(npc.getMaxHealth());
		npc.cultivation().setQi(npc.cultivation().maxQi());
		spawned.add(npc);
		Vec3 home = npc.position();
		Husk foe = husk(level, home.add(0, 0, 16), spawned);
		npc.setTarget(foe);
		CultivatorSpells.face(npc, foe);
		int stone = floorLeft(level, floor.centre(), home, 30);
		boolean quiet = Devastation.pending() == 0;
		helper.assertTrue(Spells.castBy(npc, Spell.GRAVESTONE_FALLS), "A cultivator casts Gravestone Falls");
		helper.runAfterDelay(EarthSpells.GRAVE_FALL + 4, () -> {
			try {
				helper.assertTrue(foe.getHealth() <= HEALTH - EarthSpells.GRAVE_DAMAGE + 1, "Its gravestone falls on its foe (" + foe.getHealth() + ")");
				helper.assertTrue(Spells.castBy(npc, Spell.UNDYING_EMBER) || npc.cultivation().getQi() < Spell.UNDYING_EMBER.qiCost(), "...and the Undying Ember after it");
				helper.runAfterDelay(FireSpells.EMBER_FLIGHT + 4, () -> {
					try {
						helper.assertTrue(landKept(level, floor, home, 30, stone, quiet), "Nothing it cast took any of the land (" + stone + " -> " + floorLeft(level, floor.centre(), home, 30) + ")");
						helper.succeed();
					} finally {
						HeavenSpells.forget(npc.getUUID());
						finish(helper, watcher, spawned, floor);
					}
				});
			} catch (RuntimeException e) {
				HeavenSpells.forget(npc.getUUID());
				finish(helper, watcher, spawned, floor);
				throw e;
			}
		});
	}
}
