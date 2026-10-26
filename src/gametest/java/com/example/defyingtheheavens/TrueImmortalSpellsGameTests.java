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
 * The True Immortal spells (2026-10-24: the elements as they were before heaven and earth were parted; the ghost foundation's Fengdu's Walls
 * Rise and Ten Thousand Souls Ascend, and Primordial Chaos of both paths), each cast by a caster of that realm on a floor high over its spot:
 * its shapes come (SpellConstruct), it strikes whom it should and spares whom it should, and a player's works the land as it says (the ghost
 * spells leave it be); a cultivator's leaves the land whole. Each in a batch of its own.
 */
public class TrueImmortalSpellsGameTests implements FabricGameTest {
	/** The husks' health: the realm's blows are the heaviest there are (75000 at the most), and a husk struck dead is gone before it can be weighed. */
	private static final float HEALTH = 300000; // more than three times its caster's: a great beast to it (HeavenSpells#GREAT_BEYOND)

	/** A True Immortal Grand Perfection caster who knows {@code element}'s method of that realm to its last layer, at {@code at}. */
	private static ServerPlayer caster(GameTestHelper helper, QiElement element, BlockPos at) {
		ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.FIRE_FOUR_AXIS, at);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION, 0);
		HeavenMethod method = HeavenMethod.of(element, Realm.TRUE_IMMORTAL);
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

	/** A husk that can be moved, to see a spell carry it (VoidShatteringSpellsGameTests#mobileHusk): its wits kept, its goals taken, unhurtable. */
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

	/** How many blocks within {@code radius} of {@code at} (level, from the floor's layer to {@code up} over it) {@code which} takes. */
	private static int count(ServerLevel level, BlockPos centre, Vec3 at, double radius, int up, java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState> which) {
		int n = 0;
		int r = (int) Math.ceil(radius);
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx + dz * dz > radius * radius) continue;
				for (int dy = -14; dy <= up; dy++) {
					if (which.test(level.getBlockState(new BlockPos((int) Math.floor(at.x) + dx, centre.getY() + dy, (int) Math.floor(at.z) + dz)))) n++;
				}
			}
		}
		return n;
	}

	private static List<SpellConstruct> constructs(ServerLevel level, Vec3 at, double radius, SpellConstruct.Kind kind) {
		return level.getEntitiesOfClass(SpellConstruct.class, new AABB(at, at).inflate(radius), c -> c.kind() == kind);
	}

	private static void finish(GameTestHelper helper, ServerPlayer p, List<LivingEntity> spawned, HeavenSpellTesting.Floor floor) {
		HeavenSpells.forget(p.getUUID());
		for (SpellConstruct c : helper.getLevel().getEntitiesOfClass(SpellConstruct.class, new AABB(floor.centre()).inflate(220))) c.discard();
		HeavenSpellTesting.cleanUp(p, spawned, floor);
	}

	/** Ends a test whose spell wounds the land once its wounds are cut and drained (HeavenSpellTesting#succeedOnceHealed), its shapes gone first. */
	private static void finishOnceHealed(GameTestHelper helper, ServerPlayer p, List<LivingEntity> spawned, HeavenSpellTesting.Floor floor, Runnable after) {
		HeavenSpells.forget(p.getUUID());
		for (SpellConstruct c : helper.getLevel().getEntitiesOfClass(SpellConstruct.class, new AABB(floor.centre()).inflate(220))) c.discard();
		HeavenSpellTesting.succeedOnceHealed(helper, p, spawned, floor, after);
	}

	/** Whether the floor round {@code at} is as it was, asked only where no wound was being cut as the test began (DaoIntegrationSpellsGameTests#landKept). */
	private static boolean landKept(ServerLevel level, HeavenSpellTesting.Floor floor, Vec3 at, double radius, int stone, boolean quiet) {
		return !quiet || floorLeft(level, floor.centre(), at, radius) == stone;
	}

	/** Runs {@code check} {@code delay} ticks on, then ends the test once its wounds are healed (or at once, failing, if the check throws). */
	private static void thenCheck(GameTestHelper helper, int delay, ServerPlayer p, List<LivingEntity> spawned, HeavenSpellTesting.Floor floor, Runnable thinned, Runnable check) {
		helper.runAfterDelay(delay, () -> {
			try {
				check.run();
			} catch (RuntimeException e) {
				thinned.run();
				finish(helper, p, spawned, floor);
				throw e;
			}
			finishOnceHealed(helper, p, spawned, floor, thinned);
		});
	}

	/** Its spells come at the third and seventh layers of its realm, and are as great as their realm makes them (the user's rule, 2026-10-24). */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void theTrueImmortalSpellsComeInTheirPlaceAndGrow(GameTestHelper helper) {
		Spell[][] pairs = {{Spell.PRIMAL_WIND_OF_CREATION, Spell.FIRST_SEED_OF_CREATION}, {Spell.SAMADHI_TRUE_FIRE, Spell.FIRST_FLAME_BEFORE_HEAVEN},
				{Spell.HAND_OF_HOUTU, Spell.FALLING_CONTINENT}, {Spell.GLACIER_OF_THE_FIRST_AGE, Spell.THE_PRIMORDIAL_SEA},
				{Spell.HEAVEN_SLAYING_SWORD_RISES, Spell.PRIMAL_AXE_SPLITS_CHAOS}, {Spell.FENGDU_WALLS_RISE, Spell.TEN_THOUSAND_SOULS_ASCEND}};
		for (Spell[] pair : pairs) {
			helper.assertTrue(pair[0].layer() == 93 && pair[1].layer() == 97, pair[0].getId() + " and " + pair[1].getId() + " at layers 93 and 97");
			for (Spell spell : pair) helper.assertTrue(spell.realm() == Realm.TRUE_IMMORTAL, spell.getId() + " is of True Immortal");
		}
		helper.assertTrue(Spell.PRIMORDIAL_CHAOS.isDual() && Spell.PRIMORDIAL_CHAOS.dualRealm() == Realm.TRUE_IMMORTAL, "Primordial Chaos is both paths', of True Immortal");
		helper.assertTrue(WoodSpells.WIND_RANGE > WoodSpells.SEED_RANGE && FireSpells.TRUE_FIRE_RANGE > FireSpells.PYRE_RANGE && EarthSpells.HOUTU_RANGE > EarthSpells.GRAVE_RANGE
				&& WaterSpells.PRIMAL_SEA_RANGE > WaterSpells.FALLS_RANGE && MetalSpells.SLAYER_RANGE > MetalSpells.REBIRTH_RANGE && GhostSpells.FENGDU_RANGE > GhostSpells.IMPERMANENCE_RANGE
				&& HeavenDemonSpells.CHAOS_RANGE > HeavenDemonSpells.ECLIPSE_RANGE, "True Immortal's reach is greater than Entering Nirvana's");
		SpellConstruct shape = SpellConstruct.create(helper.getLevel(), SpellConstruct.Kind.FALLING_CONTINENT, Vec3.ZERO, 0, 0, 20, null).sizedFor(Spell.FALLING_CONTINENT);
		helper.assertTrue(HeavenSpellTesting.near(shape.size(), HeavenSpells.greatness(Realm.TRUE_IMMORTAL)) && shape.size() > HeavenSpells.greatness(Realm.ENTERING_NIRVANA),
				"...and its shapes drawn greater (" + shape.size() + ")");
		shape.discard();
		for (SpellConstruct.Kind kind : new SpellConstruct.Kind[] {SpellConstruct.Kind.PRIMORDIAL_CYCLONE, SpellConstruct.Kind.CREATION_SEED, SpellConstruct.Kind.SAMADHI_PILLARS,
				SpellConstruct.Kind.CHAOS_FLOWER, SpellConstruct.Kind.HOUTU_HAND, SpellConstruct.Kind.FALLING_CONTINENT, SpellConstruct.Kind.PRIMAL_GLACIER,
				SpellConstruct.Kind.PRIMORDIAL_SEA, SpellConstruct.Kind.HEAVEN_SLAYING_SWORD, SpellConstruct.Kind.PANGU_AXE, SpellConstruct.Kind.FENGDU_WALL,
				SpellConstruct.Kind.SOUL_HELIX, SpellConstruct.Kind.CHAOS_SPHERE}) {
			helper.assertTrue(kind.modelled(), kind + " is a model of its own");
		}
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "immortal_wind", timeoutTicks = 500)
	public void thePrimalWindWandersOnWhirlingAndFlings(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 60, 30);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.WOOD, floor.centre().offset(0, 0, -40));
		Vec3 home = p.position(), point = home.add(0, 0, 16);
		double travel = WoodSpells.WIND_SPEED * WoodSpells.WIND_TICKS;
		// One on its way, one where it ends (flung), one well to the side of it.
		Husk onWay = husk(level, point.add(0, 0, 24), spawned), atEnd = husk(level, point.add(0, 0, travel - 2), spawned),
				aside = husk(level, point.add(WoodSpells.WIND_PULL + 6, 0, 24), spawned);
		HeavenSpellTesting.lookAt(p, point);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.PRIMAL_WIND_OF_CREATION), "Primal Wind of Creation is cast, paid for");
		helper.runAfterDelay(WoodSpells.WIND_FORMS + 60, () -> {
			List<SpellConstruct> found = constructs(level, point, 60, SpellConstruct.Kind.PRIMORDIAL_CYCLONE);
			if (found.size() != 1 || found.get(0).position().z - point.z < 15) {
				finish(helper, p, spawned, floor);
				helper.fail("Its cyclone forms where its caster looks and wanders on along the look (" + (found.isEmpty() ? "none" : found.get(0).position().z - point.z + " on") + ")");
				return;
			}
			thenCheck(helper, WoodSpells.WIND_TICKS - 60 + 3, p, spawned, floor, () -> {}, () -> {
				helper.assertTrue(onWay.getHealth() <= HEALTH - 8 * WoodSpells.WIND_DAMAGE + 1, "It whirls what it passes over, striking it (" + onWay.getHealth() + ")");
				helper.assertTrue(atEnd.getHealth() <= HEALTH - 3 * WoodSpells.WIND_DAMAGE - WoodSpells.WIND_FLING + 1, "...and flings out what it holds at its end (" + atEnd.getHealth() + ")");
				helper.assertTrue(aside.getHealth() == aside.getMaxHealth(), "...not what stands well aside of it");
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "immortal_creation", timeoutTicks = 500)
	public void theSeedOfCreationDrinksAndTheTreeBursts(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 58, 30);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.WOOD, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 18);
		Husk heart = husk(level, point, spawned), edge = husk(level, point.add(WoodSpells.CREATION_RADIUS - 4, 0, 0), spawned),
				beyond = husk(level, point.add(-WoodSpells.CREATION_RADIUS - 6, 0, 0), spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(heart));
		p.setHealth(p.getMaxHealth() / 2);
		float hurt = p.getHealth();
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.FIRST_SEED_OF_CREATION), "First Seed of Creation is cast, paid for");
		helper.assertTrue(constructs(level, point, 6, SpellConstruct.Kind.CREATION_SEED).size() == 1, "...its dome of life growing over where its caster looks");
		thenCheck(helper, WoodSpells.CREATION_GROWS + WoodSpells.CREATION_TICKS + 6 + 3, p, spawned, floor, () -> {}, () -> {
			float all = WoodSpells.CREATION_TICKS / WoodSpells.CREATION_EVERY * WoodSpells.CREATION_DRAIN + WoodSpells.CREATION_BLOOM;
			helper.assertTrue(heart.getHealth() <= HEALTH - all + 1 && edge.getHealth() <= HEALTH - all + 1, "It drinks every foe within, then its tree bursts up through them ("
					+ heart.getHealth() + ", " + edge.getHealth() + ")");
			helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond its dome");
			helper.assertTrue(p.getHealth() > hurt, "...a share of it mending its caster (" + hurt + " -> " + p.getHealth() + ")");
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "immortal_true_fire", timeoutTicks = 500)
	public void theThreeTrueFiresBurnAndMeet(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 52, 30);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.FIRE, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 18);
		Husk heart = husk(level, point, spawned), near = husk(level, point.add(FireSpells.TRUE_FIRE_RADIUS - 3, 0, 0), spawned),
				beyond = husk(level, point.add(-FireSpells.TRUE_FIRE_FLARE_RADIUS - 6, 0, 0), spawned);
		Runnable thinned = HeavenSpellTesting.thicken(floor, point.x, point.z, (int) FireSpells.TRUE_FIRE_CRATER + 2, 12);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(heart));
		int stone = floorLeft(level, floor.centre(), point, 6);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.SAMADHI_TRUE_FIRE), "Samadhi True Fire is cast, paid for");
		thenCheck(helper, FireSpells.TRUE_FIRE_ERUPTS + FireSpells.TRUE_FIRE_TICKS + FireSpells.TRUE_FIRE_MEETS + 3, p, spawned, floor, thinned, () -> {
			float all = FireSpells.TRUE_FIRE_TICKS / FireSpells.TRUE_FIRE_EVERY * FireSpells.TRUE_FIRE_BURN + FireSpells.TRUE_FIRE_FLARE;
			helper.assertTrue(heart.getHealth() <= HEALTH - all + 1 && near.getHealth() <= HEALTH - all + 1,
					"Its pillars burn every foe within, fire-proof or not, then meet in one flare (" + heart.getHealth() + ", " + near.getHealth() + ")");
			helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond its flare");
			helper.assertTrue(floorLeft(level, floor.centre(), point, 6) < stone, "...the land burnt into a crater");
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "immortal_first_flame", timeoutTicks = 500)
	public void theFlowerOfChaosFireOpensAndBursts(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 58, 30);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.FIRE, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 16);
		Husk heart = husk(level, point, spawned), petal = husk(level, point.add(FireSpells.FIRST_FLAME_RADIUS - 4, 0, 0), spawned),
				beyond = husk(level, point.add(-FireSpells.FIRST_FLAME_RADIUS - 6, 0, 0), spawned);
		Runnable thinned = HeavenSpellTesting.thicken(floor, point.x, point.z, (int) FireSpells.FIRST_FLAME_CRATER + 2, 14);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(heart));
		int stone = floorLeft(level, floor.centre(), point, 8);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.FIRST_FLAME_BEFORE_HEAVEN), "First Flame Before Heaven is cast, paid for");
		thenCheck(helper, FireSpells.FIRST_FLAME_OPENS + FireSpells.FIRST_FLAME_TICKS + FireSpells.FIRST_FLAME_IMPLODES + 3, p, spawned, floor, thinned, () -> {
			float all = FireSpells.FIRST_FLAME_TICKS / FireSpells.FIRST_FLAME_EVERY * FireSpells.FIRST_FLAME_BURN + FireSpells.FIRST_FLAME_BURST;
			helper.assertTrue(heart.getHealth() <= HEALTH - all + 1 && petal.getHealth() <= HEALTH - all + 1,
					"It burns every foe within, then slams shut and bursts (" + heart.getHealth() + ", " + petal.getHealth() + ")");
			helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond its petals");
			helper.assertTrue(floorLeft(level, floor.centre(), point, 8) < stone, "...the land blown into a crater of glass");
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "immortal_houtu", timeoutTicks = 400)
	public void theHandOfHoutuGraspsAndSlams(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 50, 34);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.EARTH, floor.centre().offset(0, 0, -16));
		Vec3 home = p.position(), point = home.add(0, 0, 16), slam = point.add(0, 0, EarthSpells.HOUTU_FIST);
		// One in its hand (too great to carry), one where its fist comes down, one it can carry, one beyond it all.
		Husk grasped = husk(level, point, spawned), under = husk(level, slam, spawned), beyond = husk(level, point.add(-EarthSpells.HOUTU_GRASP - 6, 0, 0), spawned);
		Husk carried = mobileHusk(level, point.add(3, 0, 0), spawned);
		Runnable thinned = HeavenSpellTesting.thicken(floor, slam.x, slam.z, (int) EarthSpells.HOUTU_CRATER + 2, 12);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(grasped));
		int stone = floorLeft(level, floor.centre(), slam, 6);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.HAND_OF_HOUTU), "Hand of Houtu is cast, paid for");
		helper.runAfterDelay(EarthSpells.HOUTU_RISES + EarthSpells.HOUTU_GRASPS + 6, () -> {
			if (grasped.getHealth() > HEALTH - EarthSpells.HOUTU_CRUSH + 1 || carried.getY() < point.y + EarthSpells.HOUTU_FIST - 5) {
				thinned.run();
				finish(helper, p, spawned, floor);
				helper.fail("Its hand closes on what stands in it, crushing it, catching up what it can carry in its fist (" + grasped.getHealth() + ", carried to "
						+ (carried.getY() - point.y) + ")");
				return;
			}
			thenCheck(helper, EarthSpells.HOUTU_HOLDS - 6 + EarthSpells.HOUTU_SLAMS + 3, p, spawned, floor, thinned, () -> {
				helper.assertTrue(grasped.getHealth() <= HEALTH - EarthSpells.HOUTU_CRUSH - EarthSpells.HOUTU_SLAM + 1, "Then its fist comes down (" + grasped.getHealth() + ")");
				helper.assertTrue(under.getHealth() <= HEALTH - EarthSpells.HOUTU_SLAM + 1, "...on what stands where it lands (" + under.getHealth() + ")");
				helper.assertTrue(carried.position().distanceTo(slam) < EarthSpells.HOUTU_SLAM_RADIUS, "...carrying what it held down with it (" + carried.position().distanceTo(slam) + " off)");
				helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond it");
				helper.assertTrue(floorLeft(level, floor.centre(), slam, 6) < stone, "...the land smashed into a crater");
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "immortal_continent", timeoutTicks = 400)
	public void theContinentFallsOnTheLand(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 58, 30);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.EARTH, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 16);
		Husk heart = husk(level, point, spawned), edge = husk(level, point.add(EarthSpells.CONTINENT_RADIUS - 4, 0, 0), spawned),
				beyond = husk(level, point.add(-EarthSpells.CONTINENT_RADIUS - 6, 0, 0), spawned);
		Runnable thinned = HeavenSpellTesting.thicken(floor, point.x, point.z, (int) EarthSpells.CONTINENT_CRATER + 2, 14);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(heart));
		int stone = floorLeft(level, floor.centre(), point, 10);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.FALLING_CONTINENT), "Falling Continent is cast, paid for");
		helper.assertTrue(constructs(level, point.add(0, EarthSpells.CONTINENT_HIGH, 0), 8, SpellConstruct.Kind.CONTINENT_MASS).size() == 1, "...a continent high over the spot");
		thenCheck(helper, EarthSpells.CONTINENT_FALLS + 3, p, spawned, floor, thinned, () -> {
			helper.assertTrue(heart.getHealth() <= HEALTH - EarthSpells.CONTINENT_DAMAGE + 1 && edge.getHealth() <= HEALTH - EarthSpells.CONTINENT_DAMAGE + 1,
					"It falls on everything under it (" + heart.getHealth() + ", " + edge.getHealth() + ")");
			helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond it");
			helper.assertTrue(floorLeft(level, floor.centre(), point, 10) < stone, "...the land smashed into a vast crater");
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "immortal_glacier", timeoutTicks = 400)
	public void theGlacierGrindsOnAndCalves(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 60, 34);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.WATER, floor.centre().offset(0, 0, -58));
		Vec3 home = p.position(), end = home.add(0, 0, WaterSpells.GLACIER_START + WaterSpells.GLACIER_REACH);
		// One it grinds past on its way (too great to shove), one by where its face ends (its calving falls on it), one aside of its path.
		Husk ground = husk(level, home.add(0, 0, 58), spawned), calved = husk(level, end.subtract(0, 0, 1), spawned),
				aside = husk(level, home.add(WaterSpells.GLACIER_HALF + 6, 0, 58), spawned);
		HeavenSpellTesting.lookAt(p, home.add(0, p.getEyeHeight(), 40));
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.GLACIER_OF_THE_FIRST_AGE), "Glacier of the First Age is cast, paid for");
		helper.assertTrue(!HeavenSpells.lingering(p.getUUID(), WaterSpells.Glacier.class).isEmpty(), "...its wall of ice rising before its caster");
		// Struck every so often while its face passes over it: from 3 blocks before it to its back, its body's depth behind.
		int grinds = (int) ((WaterSpells.GLACIER_BODY + 4) / (WaterSpells.GLACIER_REACH / WaterSpells.GLACIER_TICKS) / WaterSpells.GLACIER_EVERY);
		thenCheck(helper, WaterSpells.GLACIER_RISES + WaterSpells.GLACIER_TICKS + WaterSpells.GLACIER_CALVES / 2 + 3, p, spawned, floor, () -> {}, () -> {
			helper.assertTrue(grinds >= 2 && ground.getHealth() <= HEALTH - grinds * WaterSpells.GLACIER_GRIND + 1, "It grinds over what stands in its way (" + ground.getHealth() + ")");
			helper.assertTrue(calved.getHealth() <= HEALTH - WaterSpells.GLACIER_CALVE + 1, "...and its face calves on what stands before it at its end (" + calved.getHealth() + ")");
			helper.assertTrue(aside.getHealth() == aside.getMaxHealth(), "...not what stands aside of its path");
			int frost = count(level, floor.centre(), home.add(0, 0, 40), 12, 2, s -> s.is(Blocks.SNOW) || s.is(Blocks.ICE) || s.is(Blocks.PACKED_ICE) || s.is(Blocks.BLUE_ICE));
			helper.assertTrue(frost > 20, "...the land in its wake frozen (" + frost + ")");
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "immortal_sea", timeoutTicks = 500)
	public void thePrimordialSeaDrownsAndCrashes(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 56, 34);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.WATER, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 14);
		Husk heart = husk(level, point, spawned), edge = husk(level, point.add(WaterSpells.PRIMAL_SEA_RADIUS - 4, 0, 0), spawned),
				beyond = husk(level, point.add(-WaterSpells.PRIMAL_SEA_RADIUS - 6, 0, 0), spawned);
		Runnable thinned = HeavenSpellTesting.thicken(floor, point.x, point.z, (int) WaterSpells.PRIMAL_SEA_LAKE + 2, 12);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(heart));
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.THE_PRIMORDIAL_SEA), "The Primordial Sea is cast, paid for");
		thenCheck(helper, WaterSpells.PRIMAL_SEA_RISES + WaterSpells.PRIMAL_SEA_TICKS + WaterSpells.PRIMAL_SEA_CLOSES + 3, p, spawned, floor, thinned, () -> {
			float all = WaterSpells.PRIMAL_SEA_TICKS / WaterSpells.PRIMAL_SEA_EVERY * WaterSpells.PRIMAL_SEA_DROWN + WaterSpells.PRIMAL_SEA_CRASH;
			helper.assertTrue(heart.getHealth() <= HEALTH - all + 1 && edge.getHealth() <= HEALTH - all + 1, "It drowns every foe within, then its waves crash in ("
					+ heart.getHealth() + ", " + edge.getHealth() + ")");
			helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond its waves");
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "immortal_slayer", timeoutTicks = 400)
	public void theHeavenSlayingSwordRisesAndCuts(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 56, 30);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.METAL, floor.centre().offset(0, 0, -10));
		Vec3 home = p.position(), point = home.add(0, 0, 10);
		// Its caster looks along +z: its first cut runs 22.5 degrees off that, the next 45 on; one stands on the first, one between it and the last.
		Vec3 onCut = point.add(Vec3.directionFromRotation(0, 22.5f).scale(34)), between = point.add(Vec3.directionFromRotation(0, 0).scale(34));
		Husk heart = husk(level, point, spawned), cut = husk(level, onCut, spawned), spared = husk(level, between, spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(heart));
		int stone = floorLeft(level, floor.centre(), point.lerp(onCut, 0.6), 2);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.HEAVEN_SLAYING_SWORD_RISES), "Heaven-Slaying Sword Rises is cast, paid for");
		thenCheck(helper, MetalSpells.SLAYER_RISES + MetalSpells.SLAYER_STANDS + 3, p, spawned, floor, () -> {}, () -> {
			helper.assertTrue(heart.getHealth() <= HEALTH - MetalSpells.SLAYER_RISE - MetalSpells.SLAYER_CUT_DAMAGE + 1, "It rises through what stands over it, and its cuts run out from it ("
					+ heart.getHealth() + ")");
			helper.assertTrue(cut.getHealth() <= HEALTH - MetalSpells.SLAYER_CUT_DAMAGE + 1 && cut.getHealth() > HEALTH - MetalSpells.SLAYER_CUT_DAMAGE - MetalSpells.SLAYER_RISE,
					"...one of them through what stands on it, far from the blade (" + cut.getHealth() + ")");
			helper.assertTrue(spared.getHealth() == spared.getMaxHealth(), "...none through what stands between them");
			helper.assertTrue(floorLeft(level, floor.centre(), point.lerp(onCut, 0.6), 2) < stone, "...the land cut along them");
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "immortal_axe", timeoutTicks = 400)
	public void thePrimalAxeSplitsTheLand(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 60, 30);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.METAL, floor.centre().offset(0, 0, -40));
		Vec3 home = p.position(), along = home.add(0, 0, 40);
		Husk split = husk(level, along, spawned), aside = husk(level, along.add(MetalSpells.AXE_WIDTH + 6, 0, 0), spawned);
		HeavenSpellTesting.lookAt(p, home.add(0, p.getEyeHeight(), 40));
		int stone = floorLeft(level, floor.centre(), along, 3), spare = floorLeft(level, floor.centre(), home.add(0, 0, 3), 2);
		boolean quiet = Devastation.pending() == 0;
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.PRIMAL_AXE_SPLITS_CHAOS), "Primal Axe Splits Chaos is cast, paid for");
		helper.assertTrue(constructs(level, home, 12, SpellConstruct.Kind.PANGU_AXE).size() == 1, "...Pangu's axe standing behind its caster");
		thenCheck(helper, MetalSpells.AXE_RAISES + MetalSpells.AXE_HOLDS + MetalSpells.AXE_CHOPS + 3, p, spawned, floor, () -> {}, () -> {
			helper.assertTrue(split.getHealth() <= HEALTH - MetalSpells.AXE_DAMAGE + 1, "It chops down through what stands along its line (" + split.getHealth() + ")");
			helper.assertTrue(aside.getHealth() == aside.getMaxHealth(), "...not what stands aside of it");
			helper.assertTrue(floorLeft(level, floor.centre(), along, 3) < stone, "...the land split into a chasm along it");
			helper.assertTrue(landKept(level, floor, home.add(0, 0, 3), 2, spare, quiet), "...but not at its caster's feet");
		});
	}

	/** A living caster whose ghost path stands at True Immortal Grand Perfection, its foundations understood to the Netherworld Immortal Sovereign Canon. */
	private static ServerPlayer ghostCaster(GameTestHelper helper, BlockPos at) {
		ServerPlayer p = caster(helper, QiElement.WATER, at);
		NetherworldState s = CultivationManager.get(p).netherworld();
		for (GhostMethod method : GhostMethod.values()) {
			if (method.isFoundation()) s.setMethod(method, 1, 0);
		}
		GhostMethods.setPath(s, PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION));
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

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "immortal_fengdu", timeoutTicks = 400)
	public void theWallsOfFengduHoldAndFall(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 54, 30);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = ghostCaster(helper, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 12);
		double inner = GhostSpells.FENGDU_RADIUS * Math.cos(Math.PI / GhostSpells.FENGDU_WALLS);
		Husk heart = husk(level, point, spawned), wall = husk(level, point.add(inner - 4, 0, 0), spawned),
				beyond = husk(level, point.add(-GhostSpells.FENGDU_RADIUS - 6, 0, 0), spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(heart));
		helper.assertTrue(Spell.FENGDU_WALLS_RISE.isKnown(CultivationManager.get(p)), "Its ghost path carries the Netherworld Immortal Sovereign Canon to its spell");
		helper.assertTrue(took(p, Spell.FENGDU_WALLS_RISE), "Fengdu's Walls Rise is cast");
		helper.assertTrue(constructs(level, point, GhostSpells.FENGDU_RADIUS + 6, SpellConstruct.Kind.WALL_MASS).size() == GhostSpells.FENGDU_WALLS, "...its walls rising round the spot");
		helper.runAfterDelay(GhostSpells.FENGDU_RISES + GhostSpells.FENGDU_TICKS + GhostSpells.FENGDU_FALLS + 3, () -> {
			try {
				float all = GhostSpells.FENGDU_TICKS / GhostSpells.FENGDU_EVERY * GhostSpells.FENGDU_HARM + GhostSpells.FENGDU_FALL;
				helper.assertTrue(heart.getHealth() <= HEALTH - all + 1 && wall.getHealth() <= HEALTH - all + 1, "The city's dead fall on every foe within, then its walls topple in ("
						+ heart.getHealth() + ", " + wall.getHealth() + ")");
				helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not on what stands beyond them");
				helper.succeed();
			} finally {
				finish(helper, p, spawned, floor);
			}
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "immortal_souls", timeoutTicks = 400)
	public void theTenThousandSoulsCarryAndDrag(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 48, 30);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = ghostCaster(helper, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 14);
		Husk held = husk(level, point, spawned), beyond = husk(level, point.add(-GhostSpells.SOULS_PULL - 6, 0, 0), spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(held));
		helper.assertTrue(took(p, Spell.TEN_THOUSAND_SOULS_ASCEND), "Ten Thousand Souls Ascend is cast");
		helper.assertTrue(constructs(level, point, 6, SpellConstruct.Kind.SOUL_VORTEX).size() == 1, "...its helix of souls rising where its caster looks");
		helper.runAfterDelay(GhostSpells.SOULS_RISES + GhostSpells.SOULS_TICKS + 3, () -> {
			try {
				float all = GhostSpells.SOULS_TICKS / GhostSpells.SOULS_EVERY * GhostSpells.SOULS_HARM + GhostSpells.SOULS_SLAM;
				helper.assertTrue(held.getHealth() <= HEALTH - all + 1, "Its souls tear at what it holds, then drag it down (" + held.getHealth() + ")");
				helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not at what stands beyond its draw");
				helper.succeed();
			} finally {
				finish(helper, p, spawned, floor);
			}
		});
	}

	/** A tiger demon of True Immortal Grand Perfection on the heaven path and the Demon Path both. */
	private static ServerPlayer demonCaster(GameTestHelper helper, BlockPos at) {
		ServerPlayer p = caster(helper, QiElement.METAL, at);
		PlayerCultivation c = CultivationManager.get(p);
		c.demon().setBloodline(Bloodline.TIGER);
		c.demon().set(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION), 0);
		CultivationManager.refresh(p);
		c.setQi(c.maxQi());
		p.setHealth(p.getMaxHealth());
		return p;
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "immortal_chaos", timeoutTicks = 500)
	public void thePrimordialChaosChurnsAndBursts(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 52, 40);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = demonCaster(helper, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 18);
		Husk under = husk(level, point, spawned), beyond = husk(level, point.add(-HeavenDemonSpells.CHAOS_BURST_RADIUS - 6, 0, 0), spawned);
		Runnable thinned = HeavenSpellTesting.thicken(floor, point.x, point.z, (int) HeavenDemonSpells.CHAOS_CRATER + 2, 12);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(under));
		p.setHealth(p.getMaxHealth() / 2);
		float hurt = p.getHealth();
		int stone = floorLeft(level, floor.centre(), point, 8);
		helper.assertTrue(Spell.PRIMORDIAL_CHAOS.isKnown(CultivationManager.get(p)), "Both paths at True Immortal: Primordial Chaos is theirs");
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.PRIMORDIAL_CHAOS), "Primordial Chaos is cast, paid for in qi");
		helper.assertTrue(constructs(level, point.add(0, HeavenDemonSpells.CHAOS_HIGH, 0), 6, SpellConstruct.Kind.CHAOS_SPHERE).size() == 1, "...its sphere of chaos over the spot");
		thenCheck(helper, HeavenDemonSpells.CHAOS_SWELLS + HeavenDemonSpells.CHAOS_TICKS + HeavenDemonSpells.CHAOS_BURSTS + 3, p, spawned, floor, thinned, () -> {
			float all = HeavenDemonSpells.CHAOS_TICKS / HeavenDemonSpells.CHAOS_EVERY * HeavenDemonSpells.CHAOS_CHURN + HeavenDemonSpells.CHAOS_BURST;
			helper.assertTrue(under.getHealth() <= HEALTH - all + 1, "It churns what it draws in, then bursts (" + under.getHealth() + ")");
			helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not on what stands beyond its burst");
			helper.assertTrue(p.getHealth() > hurt, "...a share of it mending its caster (" + hurt + " -> " + p.getHealth() + ")");
			helper.assertTrue(floorLeft(level, floor.centre(), point, 8) < stone, "...the land burnt into a crater of glass");
		});
	}

	/** A cultivator's True Immortal spells strike as a player's do but leave the land whole (HeavenSpells#carvesLand). */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "immortal_cultivator", timeoutTicks = 300)
	public void aCultivatorsImmortalSpellsLeaveTheLand(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 40, 30);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer watcher = caster(helper, QiElement.METAL, floor.centre().offset(-34, 0, -34));
		BlockPos at = floor.centre();
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, helper.relativePos(at));
		npc.setNoAi(true);
		// Past the census (HighRealms: no True Immortal walks the realms below theirs), as a body a True Immortal's soul cultivated would be.
		npc.setRank(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION), true);
		npc.setHealth(npc.getMaxHealth());
		npc.cultivation().setQi(npc.cultivation().maxQi());
		spawned.add(npc);
		Vec3 home = npc.position();
		Husk foe = husk(level, home.add(0, 0, 16), spawned);
		npc.setTarget(foe);
		CultivatorSpells.face(npc, foe);
		int stone = floorLeft(level, floor.centre(), home, 34);
		boolean quiet = Devastation.pending() == 0;
		helper.assertTrue(Spells.castBy(npc, Spell.HEAVEN_SLAYING_SWORD_RISES), "A cultivator casts Heaven-Slaying Sword Rises");
		helper.runAfterDelay(MetalSpells.SLAYER_RISES + MetalSpells.SLAYER_STANDS + 4, () -> {
			try {
				helper.assertTrue(foe.getHealth() <= HEALTH - MetalSpells.SLAYER_RISE + 1, "Its sword rises through its foe (" + foe.getHealth() + ")");
				helper.assertTrue(landKept(level, floor, home, 34, stone, quiet), "...and nothing it cast took any of the land (" + stone + " -> " + floorLeft(level, floor.centre(), home, 34) + ")");
				helper.succeed();
			} finally {
				HeavenSpells.forget(npc.getUUID());
				finish(helper, watcher, spawned, floor);
			}
		});
	}
}
