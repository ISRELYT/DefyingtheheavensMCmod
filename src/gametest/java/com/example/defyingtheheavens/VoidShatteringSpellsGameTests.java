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
 * The Void Shattering spells (2026-10-24: space torn open, and what comes through it; the ghost foundation's Rift to the Nine Nethers and
 * Bridge of No Return, and the Heaven-Demon Void Shatter of both paths), each cast by a caster of that realm on a floor high over its spot:
 * its tears in space and its living shapes come (SpellConstruct), it strikes whom it should and spares whom it should, and a player's cuts
 * the land as it says (the ghost and both paths' spells, and the lock of the earth's veins, leave it be); a cultivator's leaves the land
 * whole. Each in a batch of its own: they reach far and cut the floor.
 */
public class VoidShatteringSpellsGameTests implements FabricGameTest {
	/** A Void Shattering Grand Perfection caster who knows {@code element}'s method of that realm to its last layer, at {@code at}. */
	private static ServerPlayer caster(GameTestHelper helper, QiElement element, BlockPos at) {
		ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.FIRE_FOUR_AXIS, at);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(Realm.VOID_SHATTERING, Stage.GRAND_PERFECTION, 0);
		HeavenMethod method = HeavenMethod.of(element, Realm.VOID_SHATTERING);
		c.heaven().setMethod(method, method.layers(), 0);
		CultivationManager.refresh(p);
		c.setQi(c.maxQi());
		p.setHealth(p.getMaxHealth());
		return p;
	}

	/** A hardy husk to be struck (4000 health, fire-proof against burning's own harm), standing at {@code at}. */
	private static Husk husk(ServerLevel level, Vec3 at, List<LivingEntity> spawned) {
		Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, at, 4000, spawned);
		husk.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 600, 0, false, false));
		return husk;
	}

	/** A husk that can be moved, to see a spell drag or throw (DaoIntegrationSpellsGameTests#mobileHusk): its wits kept, its goals taken, unhurtable. */
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
	private static void finishOnceHealed(GameTestHelper helper, ServerPlayer p, List<LivingEntity> spawned, HeavenSpellTesting.Floor floor) {
		HeavenSpells.forget(p.getUUID());
		for (SpellConstruct c : helper.getLevel().getEntitiesOfClass(SpellConstruct.class, new AABB(floor.centre()).inflate(200))) c.discard();
		HeavenSpellTesting.succeedOnceHealed(helper, p, spawned, floor);
	}

	/** Whether the floor round {@code at} is as it was, asked only where no wound was being cut as the test began (DaoIntegrationSpellsGameTests#landKept). */
	private static boolean landKept(ServerLevel level, HeavenSpellTesting.Floor floor, Vec3 at, double radius, int stone, boolean quiet) {
		return !quiet || floorLeft(level, floor.centre(), at, radius) == stone;
	}

	/** Its spells come at the third and seventh layers of its realm, and are as great as their realm makes them (the user's rule, 2026-10-24). */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void theVoidShatteringSpellsComeInTheirPlaceAndGrow(GameTestHelper helper) {
		Spell[][] pairs = {{Spell.SPRING_THUNDER_OF_THE_AZURE_EMPEROR, Spell.VOID_WITHERING}, {Spell.GOLDEN_CROW_DESCENDS, Spell.SUN_THROUGH_THE_VOID},
				{Spell.BUZHOU_FALLS, Spell.EARTH_VEINS_LOCK_THE_VOID}, {Spell.WEAK_WATER_FROM_THE_VOID, Spell.KUN_RISES_FROM_THE_ABYSS},
				{Spell.VOID_SEVERING_SWORD_LIGHT, Spell.TEN_DIRECTION_SWORD_PRISON}, {Spell.RIFT_TO_THE_NINE_NETHERS, Spell.BRIDGE_OF_NO_RETURN}};
		for (Spell[] pair : pairs) {
			helper.assertTrue(pair[0].layer() == 66 && pair[1].layer() == 70, pair[0].getId() + " and " + pair[1].getId() + " at layers 66 and 70");
			for (Spell spell : pair) helper.assertTrue(spell.realm() == Realm.VOID_SHATTERING, spell.getId() + " is of Void Shattering");
		}
		helper.assertTrue(Spell.HEAVEN_DEMON_VOID_SHATTER.isDual() && Spell.HEAVEN_DEMON_VOID_SHATTER.dualRealm() == Realm.VOID_SHATTERING, "The Void Shatter is both paths', of Void Shattering");
		helper.assertTrue(WoodSpells.THUNDER_RANGE > WoodSpells.DRAGON_RANGE && FireSpells.CROW_RADIUS > FireSpells.FLAME_RADIUS && EarthSpells.BUZHOU_RADIUS > EarthSpells.MOUNTAINS_RADIUS
				&& WaterSpells.WEAK_RADIUS > WaterSpells.TORTOISE_RADIUS && MetalSpells.SEVERING_LENGTH > MetalSpells.TIGER_GOUGE && GhostSpells.NETHERS_RANGE > GhostSpells.GATE_RANGE
				&& HeavenDemonSpells.SHATTER_RANGE > HeavenDemonSpells.AVATAR_RANGE, "Void Shattering's reach and areas are greater than Dao Integration's");
		SpellConstruct shape = SpellConstruct.create(helper.getLevel(), SpellConstruct.Kind.GOLDEN_CROW, Vec3.ZERO, 0, 0, 20, null).sizedFor(Spell.GOLDEN_CROW_DESCENDS);
		helper.assertTrue(HeavenSpellTesting.near(shape.size(), HeavenSpells.greatness(Realm.VOID_SHATTERING)) && shape.size() > HeavenSpells.greatness(Realm.DAO_INTEGRATION),
				"...and its living shapes drawn greater (" + shape.size() + ")");
		shape.discard();
		// Its tears in space and its grasping roots: no model of GeckoLib's, their glow their spell's.
		for (SpellConstruct.Kind kind : new SpellConstruct.Kind[] {SpellConstruct.Kind.VOID_RIFT, SpellConstruct.Kind.ABYSS_RIFT, SpellConstruct.Kind.RIFT_LINE, SpellConstruct.Kind.SHATTERED_SPACE,
				SpellConstruct.Kind.VOID_TENDRIL}) {
			helper.assertTrue(!kind.modelled() && kind.moves.length == 0, kind + " is drawn by hand");
		}
		SpellConstruct tear = SpellConstruct.create(helper.getLevel(), SpellConstruct.Kind.VOID_RIFT, Vec3.ZERO, 0, 0, 20, null).size(24).tint(FireSpells.SUN_GOLD);
		helper.assertTrue(tear.tint() == FireSpells.SUN_GOLD && HeavenSpellTesting.near(tear.size(), 24), "...its glow and its length its spell's");
		tear.discard();
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "void_thunder", timeoutTicks = 300)
	public void springThunderStrikesWhatStandsUnderItsCloud(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 46, 12);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.WOOD, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 14);
		Husk under = husk(level, point.add(6, 0, 0), spawned), beyond = husk(level, point.add(-(WoodSpells.THUNDER_RADIUS + 5), 0, 0), spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(under));
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.SPRING_THUNDER_OF_THE_AZURE_EMPEROR), "Spring Thunder of the Azure Emperor is cast, paid for");
		helper.assertTrue(constructs(level, point.add(0, WoodSpells.THUNDER_HIGH, 0), 12, SpellConstruct.Kind.AZURE_THUNDER_CLOUD).size() == 1, "...its cloud gathers high over the spot");
		// Its first volley's bolts, jade (a bolt lasts but a few ticks).
		helper.runAfterDelay(WoodSpells.THUNDER_GATHER + 1, () -> {
			if (level.getEntitiesOfClass(TribulationLightning.class, new AABB(point, point).inflate(WoodSpells.THUNDER_RADIUS + 4), b -> b.tint() == WoodSpells.THUNDER_JADE).isEmpty()) {
				finish(helper, p, spawned, floor);
				helper.fail("Its cloud looses bolts of jade lightning");
			}
		});
		helper.runAfterDelay(WoodSpells.THUNDER_GATHER + WoodSpells.THUNDER_EVERY + 4, () -> {
			try {
				helper.assertTrue(under.getHealth() <= 4000 - 2 * WoodSpells.THUNDER_DAMAGE + 1, "Two volleys: what stands under it struck twice (" + under.getHealth() + ")");
				helper.assertTrue(under.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && under.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() >= 5, "...and rooted");
				helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond it (" + beyond.getHealth() + ")");
				helper.succeed();
			} finally {
				finish(helper, p, spawned, floor);
			}
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "void_withering", timeoutTicks = 300)
	public void voidWitheringSeizesDrinksAndMends(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 44, 12);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.WOOD, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 18);
		// A great beast to be drunk (held where it stands), one light enough to be hauled up, and one beyond its reach.
		Husk drunk = husk(level, point.add(5, 0, 0), spawned), seized = mobileHusk(level, point.add(-6, 0, 4), spawned),
				beyond = husk(level, point.add(0, 0, WoodSpells.WITHER_RADIUS + 5), spawned);
		BlockPos grass = BlockPos.containing(point.x + 2, floor.centre().getY() - 1, point.z);
		level.setBlock(grass, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(drunk));
		p.setHealth(p.getMaxHealth() / 2);
		float hurt = p.getHealth();
		Vec3 mouth = point.add(5, WoodSpells.WITHER_HIGH, 0);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.VOID_WITHERING), "Void Withering is cast, paid for");
		helper.assertTrue(constructs(level, mouth, 3, SpellConstruct.Kind.VOID_RIFT).size() == 1 && constructs(level, mouth, 10, SpellConstruct.Kind.VOID_ROOTS).isEmpty(),
				"...space tears open over the spot, the void tree's roots still to come out of it");
		helper.runAfterDelay(WoodSpells.WITHER_OPENS + 2 * 20 + 14, () -> {
			try {
				helper.assertTrue(constructs(level, mouth, 10, SpellConstruct.Kind.VOID_ROOTS).size() == 1, "The void tree's roots have come out of the rift");
				List<SpellConstruct> grasps = constructs(level, point, 60, SpellConstruct.Kind.VOID_TENDRIL);
				helper.assertTrue(grasps.stream().anyMatch(c -> c.target() == seized) && grasps.stream().anyMatch(c -> c.target() == drunk),
						"...a root reaching down to seize each foe within its reach (" + grasps.size() + ")");
				helper.assertTrue(grasps.stream().noneMatch(c -> c.target() == beyond), "...none beyond it");
				helper.assertTrue(seized.getY() > home.y + WoodSpells.WITHER_LIFT * 0.6, "...one it can lift hauled up into the air (" + (seized.getY() - home.y) + " up)");
				helper.assertTrue(Math.abs(drunk.getY() - home.y) < 1, "...a great beast only held where it stands");
				helper.assertTrue(drunk.getHealth() <= 4000 - 2 * WoodSpells.WITHER_DRAIN + 1, "What stands under it drained twice (" + drunk.getHealth() + ")");
				helper.assertTrue(drunk.hasEffect(MobEffects.WEAKNESS) && drunk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "...weakened and slowed");
				helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond its reach");
				helper.assertTrue(p.getHealth() > hurt, "What it drinks mends its caster (" + hurt + " -> " + p.getHealth() + ")");
				helper.assertTrue(level.getBlockState(grass).is(Blocks.COARSE_DIRT), "...and the land under it withers (" + level.getBlockState(grass) + ")");
			} catch (RuntimeException e) {
				finish(helper, p, spawned, floor);
				throw e;
			}
			// Its time out, they let go and draw back up.
			helper.runAfterDelay(WoodSpells.WITHER_TICKS - 20 - (2 * 20 + 14) + 2, () -> {
				try {
					List<SpellConstruct> grasps = constructs(level, point, 60, SpellConstruct.Kind.VOID_TENDRIL);
					helper.assertTrue(grasps.stream().allMatch(c -> c.life() <= c.tickCount + SpellConstruct.FADE), "Its time out, its roots let go (" + grasps.size() + " still holding)");
					helper.succeed();
				} finally {
					finish(helper, p, spawned, floor);
				}
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "void_crow", timeoutTicks = 400)
	public void theGoldenCrowDivesOutOfTheSky(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 44, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.FIRE, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 18);
		Husk struck = husk(level, point.add(4, 0, 0), spawned), beyond = husk(level, point.add(-(FireSpells.CROW_RADIUS + 5), 0, 0), spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(struck));
		int stone = floorLeft(level, floor.centre(), point, 8);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.GOLDEN_CROW_DESCENDS), "Golden Crow Descends is cast, paid for");
		helper.assertTrue(!constructs(level, point, FireSpells.CROW_HIGH + 30, SpellConstruct.Kind.VOID_RIFT).isEmpty(), "...the sky tears open over the spot");
		helper.runAfterDelay(FireSpells.CROW_OPENS + 2, () -> {
			if (constructs(level, point, FireSpells.CROW_HIGH + 30, SpellConstruct.Kind.GOLDEN_CROW).size() != 1) {
				finish(helper, p, spawned, floor);
				helper.fail("The Golden Crow bursts out of the rift");
				return;
			}
			helper.runAfterDelay(FireSpells.CROW_DIVE + 2, () -> {
				try {
					helper.assertTrue(struck.getHealth() <= 4000 - FireSpells.CROW_DAMAGE + 1 && struck.getRemainingFireTicks() > 0, "It strikes, burning (" + struck.getHealth() + ")");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond its reach");
					helper.assertTrue(floorLeft(level, floor.centre(), point, 8) < stone, "...and the land is melted into a crater");
				} catch (RuntimeException e) {
					finish(helper, p, spawned, floor);
					throw e;
				}
				finishOnceHealed(helper, p, spawned, floor);
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "void_sun", timeoutTicks = 500)
	public void theSunBurnsAndBursts(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 48, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.FIRE, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 14);
		Husk under = husk(level, point.add(4, 0, 0), spawned), scorched = husk(level, point.add(-(FireSpells.SUN_BLAST + 6), 0, 0), spawned),
				beyond = husk(level, point.add(FireSpells.SUN_RADIUS + 9, 0, 0), spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(under));
		int stone = floorLeft(level, floor.centre(), point, 8);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.SUN_THROUGH_THE_VOID), "Sun Pulled Through the Void is cast, paid for");
		// Over the struck (where it was looked at): the rift above where the sun is to hang, the sun not through it yet.
		Vec3 hang = point.add(4, FireSpells.SUN_HIGH, 0);
		helper.assertTrue(constructs(level, hang.add(0, FireSpells.SUN_DROP, 0), 3, SpellConstruct.Kind.VOID_RIFT).size() == 1
				&& constructs(level, hang, 40, SpellConstruct.Kind.VOID_SUN).isEmpty(), "...space tears open high over the spot, the sun still to come through");
		int burst = FireSpells.SUN_COMES + FireSpells.SUN_TICKS + FireSpells.SUN_COLLAPSE;
		helper.runAfterDelay(FireSpells.SUN_COMES + 45, () -> {
			String wrong = constructs(level, hang, 2, SpellConstruct.Kind.VOID_SUN).size() != 1 ? "A sun is dragged out of the rift and hangs clear below it"
					: !constructs(level, hang, 40, SpellConstruct.Kind.VOID_RIFT).isEmpty() ? "...the rift shrinking away behind it"
					: under.getHealth() > 4000 - 2 * FireSpells.SUN_DAMAGE + 1 || scorched.getHealth() > 4000 - 2 * FireSpells.SUN_DAMAGE + 1
							? "Everything under it burns each second (" + under.getHealth() + ", " + scorched.getHealth() + ")" : null;
			if (wrong != null) {
				finish(helper, p, spawned, floor);
				helper.fail(wrong);
				return;
			}
			helper.runAfterDelay(burst - FireSpells.SUN_COMES - 45 + 3, () -> {
				try {
					helper.assertTrue(under.getHealth() <= 4000 - 5 * FireSpells.SUN_DAMAGE - FireSpells.SUN_BURST + 1, "...then it bursts on what stands under it (" + under.getHealth() + ")");
					helper.assertTrue(scorched.getHealth() > 4000 - 6 * FireSpells.SUN_DAMAGE, "...its burst not reaching as far as its burning (" + scorched.getHealth() + ")");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...nor what stands beyond either");
					helper.assertTrue(floorLeft(level, floor.centre(), point, 8) < stone, "...and the ground under it is melted into a crater");
				} catch (RuntimeException e) {
					finish(helper, p, spawned, floor);
					throw e;
				}
				finishOnceHealed(helper, p, spawned, floor);
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "void_buzhou", timeoutTicks = 400)
	public void buzhouFallsCrushingAndPinning(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 44, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.EARTH, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 18);
		Husk crushed = husk(level, point.add(5, 0, 0), spawned), beyond = husk(level, point.add(-(EarthSpells.BUZHOU_RADIUS + 5), 0, 0), spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(crushed));
		int stone = floorLeft(level, floor.centre(), point, 10);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.BUZHOU_FALLS), "Buzhou Falls is cast, paid for");
		helper.runAfterDelay(EarthSpells.BUZHOU_OPENS + EarthSpells.BUZHOU_FALL + 4, () -> {
			try {
				List<SpellConstruct> pillar = constructs(level, point, 40, SpellConstruct.Kind.BUZHOU_PILLAR);
				helper.assertTrue(pillar.size() == 1 && pillar.get(0).getY() < home.y + 1, "The broken pillar has come down out of the sky into the land");
				helper.assertTrue(crushed.getHealth() <= 4000 - EarthSpells.BUZHOU_DAMAGE + 1, "...crushing what it fell on (" + crushed.getHealth() + ")");
				helper.assertTrue(crushed.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && crushed.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() >= 5, "...and pinning it");
				helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond its reach");
				helper.assertTrue(floorLeft(level, floor.centre(), point, 10) < stone, "...and smashing the land into a crater");
			} catch (RuntimeException e) {
				finish(helper, p, spawned, floor);
				throw e;
			}
			finishOnceHealed(helper, p, spawned, floor);
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "void_lock", timeoutTicks = 300)
	public void theEarthVeinsLockSpace(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 34, 12);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.EARTH, floor.centre());
		Vec3 home = p.position();
		Husk held = mobileHusk(level, home.add(10, 0, 0), spawned), crushed = husk(level, home.add(-8, 0, 4), spawned),
				beyond = husk(level, home.add(0, 0, EarthSpells.LOCK_RADIUS + 5), spawned);
		int stone = floorLeft(level, floor.centre(), home, 20);
		boolean quiet = Devastation.pending() == 0;
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.EARTH_VEINS_LOCK_THE_VOID), "Earth Veins Lock the Void is cast, paid for");
		helper.assertTrue(constructs(level, home, 4, SpellConstruct.Kind.EARTH_VEIN_LOCK).size() == 1, "...its veins rise in a dome round its caster");
		helper.runAfterDelay(EarthSpells.LOCK_RISES + 6, () -> {
			Vec3 locked = held.position();
			held.teleportTo(locked.x + 3, locked.y, locked.z);
			helper.runAfterDelay(2, () -> {
				try {
					helper.assertTrue(held.position().distanceTo(locked) < 1, "A foe in it is held where it stands (" + held.position().distanceTo(locked) + " off)");
					helper.assertTrue(EarthSpells.spaceLocked(held) && !EarthSpells.spaceLocked(p), "...space locked round it, not round its caster");
					helper.assertTrue(!EarthSpells.spaceLocked(beyond), "...nor beyond it");
				} catch (RuntimeException e) {
					finish(helper, p, spawned, floor);
					throw e;
				}
				// Past its lock (risen at LOCK_RISES, locking LOCK_TICKS): the check above was LOCK_RISES + 8 in.
				helper.runAfterDelay(EarthSpells.LOCK_TICKS, () -> {
					try {
						helper.assertTrue(crushed.getHealth() <= 4000 - 4 * EarthSpells.LOCK_DAMAGE + 1, "...crushed each second it holds (" + crushed.getHealth() + ")");
						helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond it");
						helper.assertTrue(!EarthSpells.spaceLocked(held), "Its time out, space is free again");
						helper.assertTrue(landKept(level, floor, home, 20, stone, quiet), "...and the land is left as it was");
						helper.succeed();
					} finally {
						finish(helper, p, spawned, floor);
					}
				});
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "void_weak", timeoutTicks = 400)
	public void theWeakWaterBearsNothingUp(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 44, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.WATER, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 16);
		// One held aloft (no gravity of its own), burning, and light enough to be borne down (a great beast is only slowed: HeavenSpells#greatBeast).
		Husk under = husk(level, point.add(5, 0, 0), spawned), aloft = mobileHusk(level, point.add(-4, 6, 3), spawned), beyond = husk(level, point.add(WaterSpells.WEAK_RADIUS + 12, 0, 0), spawned);
		aloft.setNoGravity(true);
		aloft.setSecondsOnFire(30);
		double high = aloft.getY();
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(under));
		int stone = floorLeft(level, floor.centre(), point, 8);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.WEAK_WATER_FROM_THE_VOID), "Weak Water from the Void is cast, paid for");
		helper.runAfterDelay(WaterSpells.WEAK_OPENS + 2, () -> {
			if (constructs(level, point, 10, SpellConstruct.Kind.WEAK_WATER_FALL).size() != 1 || aloft.getDeltaMovement().y > -0.4) {
				finish(helper, p, spawned, floor);
				helper.fail("The Weak Water pours out of the rift, and what is aloft in it is borne down (" + aloft.getDeltaMovement().y + ")");
				return;
			}
			helper.runAfterDelay(2 * 20, () -> {
				try {
					helper.assertTrue(under.getHealth() <= 4000 - 2 * WaterSpells.WEAK_DAMAGE + 1, "What stands under it takes its harm each second (" + under.getHealth() + ")");
					helper.assertTrue(under.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "...slowed");
					helper.assertTrue(aloft.getY() < high - 4, "...what was aloft in it gone down (" + (aloft.getY() - high) + ")");
					helper.assertTrue(!aloft.isOnFire(), "...its fires put out");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond it");
					helper.assertTrue(floorLeft(level, floor.centre(), point, 8) < stone, "...the land sinking into a basin under it");
				} catch (RuntimeException e) {
					finish(helper, p, spawned, floor);
					throw e;
				}
				finishOnceHealed(helper, p, spawned, floor);
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "void_kun", timeoutTicks = 400)
	public void theKunBreachesAndDivesBack(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 44, 16);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.WATER, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 18);
		Husk struck = husk(level, point.add(4, 0, 0), spawned), thrown = mobileHusk(level, point.add(-5, 0, 3), spawned),
				beyond = husk(level, point.add(0, 0, WaterSpells.KUN_RADIUS + 5), spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(struck));
		int stone = floorLeft(level, floor.centre(), point, 8);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.KUN_RISES_FROM_THE_ABYSS), "Kun Rises from the Northern Abyss is cast, paid for");
		helper.assertTrue(constructs(level, point, 6, SpellConstruct.Kind.ABYSS_RIFT).size() == 1, "...the land opens onto the abyss");
		helper.runAfterDelay(WaterSpells.KUN_OPENS + 10, () -> {
			double up = thrown.getY() - home.y;
			if (constructs(level, point, 80, SpellConstruct.Kind.KUN).size() != 1 || struck.getHealth() > 4000 - WaterSpells.KUN_DAMAGE + 1 || up < 1) {
				finish(helper, p, spawned, floor);
				helper.fail("The Kun breaches out of it, striking what stands there and throwing it high (" + struck.getHealth() + ", thrown " + up + ")");
				return;
			}
			helper.runAfterDelay(WaterSpells.KUN_LEAPS - 6, () -> {
				try {
					helper.assertTrue(struck.getHealth() <= 4000 - WaterSpells.KUN_DAMAGE - WaterSpells.KUN_DIVE_DAMAGE + 1, "...and it dives back in, striking again (" + struck.getHealth() + ")");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond its reach");
					helper.assertTrue(floorLeft(level, floor.centre(), point, 8) < stone, "...crushing the land into a basin");
				} catch (RuntimeException e) {
					finish(helper, p, spawned, floor);
					throw e;
				}
				finishOnceHealed(helper, p, spawned, floor);
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "void_severing", timeoutTicks = 400)
	public void theSeveringLightCutsSpaceOpen(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 46, 12);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.METAL, floor.centre());
		Vec3 home = p.position();
		// On its line, and (a little off it, out of the line's own reach) down where its chasm is cleft; and well off it.
		Husk near = husk(level, home.add(0, 0, 12), spawned), far = husk(level, home.add(0, 0, 36), spawned), down = husk(level, home.add(3.4, 0, 24), spawned),
				aside = husk(level, home.add(9, 0, 24), spawned);
		HeavenSpellTesting.lookAt(p, home.add(0, p.getEyeHeight() - 0.4, 30));
		int stone = floorLeft(level, floor.centre(), home.add(0, 0, 24), 3), wide = floorLeft(level, floor.centre(), home.add(2.5, 0, 24), 1);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.VOID_SEVERING_SWORD_LIGHT), "Void-Severing Sword Light is cast, paid for");
		helper.assertTrue(constructs(level, home, 6, SpellConstruct.Kind.RIFT_LINE).size() == 1 && constructs(level, home, 6, SpellConstruct.Kind.SEVERING_LIGHT).size() == 1,
				"...its sword of light thrusts out, space cut open behind it");
		helper.runAfterDelay(MetalSpells.SEVERING_THRUST + 2, () -> {
			if (near.getHealth() > 4000 - MetalSpells.SEVERING_DAMAGE + 1 || far.getHealth() > 4000 - MetalSpells.SEVERING_DAMAGE + 1
					|| down.getHealth() > 4000 - MetalSpells.SEVERING_DAMAGE + 1) {
				finish(helper, p, spawned, floor);
				helper.fail("It cuts through everything along its line and down in the chasm it cleaves (" + near.getHealth() + ", " + far.getHealth() + ", " + down.getHealth() + ")");
				return;
			}
			helper.runAfterDelay(20, () -> {
				try {
					helper.assertTrue(near.getHealth() <= 4000 - MetalSpells.SEVERING_DAMAGE - MetalSpells.SEVERING_RIFT_DAMAGE + 1, "...and the cut it leaves cuts again what stands in it (" + near.getHealth() + ")");
					helper.assertTrue(down.getHealth() <= 4000 - MetalSpells.SEVERING_DAMAGE - MetalSpells.SEVERING_RIFT_DAMAGE + 1, "...down in its chasm too (" + down.getHealth() + ")");
					helper.assertTrue(aside.getHealth() == aside.getMaxHealth(), "...not what stands well off its line");
					helper.assertTrue(floorLeft(level, floor.centre(), home.add(0, 0, 24), 3) < stone && floorLeft(level, floor.centre(), home.add(2.5, 0, 24), 1) < wide,
							"...the land cleft wide under where it passed");
				} catch (RuntimeException e) {
					finish(helper, p, spawned, floor);
					throw e;
				}
				finishOnceHealed(helper, p, spawned, floor);
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "void_prison", timeoutTicks = 500)
	public void theSwordPrisonHoldsThenItsLastTwoMeet(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 44, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.METAL, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 20);
		Husk inside = husk(level, point.add(4, 0, 0), spawned), beyond = husk(level, point.add(-(MetalSpells.PRISON_RADIUS + 6), 0, 0), spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(inside));
		int stone = floorLeft(level, floor.centre(), point, 4);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.TEN_DIRECTION_SWORD_PRISON), "Ten-Direction Sword Prison is cast, paid for");
		// Round the struck (its heart), at the ring's height: the tenth, over its heart, higher.
		Vec3 ring = inside.position().add(0, MetalSpells.PRISON_HIGH, 0);
		helper.assertTrue(constructs(level, ring, MetalSpells.PRISON_RADIUS + 4, SpellConstruct.Kind.VOID_RIFT).stream().filter(c -> Math.abs(c.getY() - ring.y) < 1).count() == MetalSpells.PRISON_SWORDS
				&& constructs(level, ring, MetalSpells.PRISON_TOP, SpellConstruct.Kind.VOID_RIFT).stream().anyMatch(c -> c.getY() > ring.y + 4),
				"...eight rifts tear open in a ring over the spot, one higher over its heart");
		int landed = MetalSpells.PRISON_OPENS + MetalSpells.PRISON_FALL, meets = landed + MetalSpells.PRISON_TICKS + MetalSpells.PRISON_MEET;
		helper.runAfterDelay(landed + 2 * 20 + 14, () -> {
			if (inside.getHealth() > 4000 - 2 * MetalSpells.PRISON_DAMAGE + 1) {
				finish(helper, p, spawned, floor);
				helper.fail("What is held inside its ring is struck each second (" + inside.getHealth() + ")");
				return;
			}
			helper.runAfterDelay(meets + 3 - (landed + 2 * 20 + 14), () -> {
				try {
					helper.assertTrue(inside.getHealth() <= 4000 - 6 * MetalSpells.PRISON_DAMAGE - MetalSpells.PRISON_MEETING + 1, "...then its last two swords meet in its heart (" + inside.getHealth() + ")");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands outside its ring");
					helper.assertTrue(floorLeft(level, floor.centre(), point, 4) < stone, "...a pit where they met");
				} catch (RuntimeException e) {
					finish(helper, p, spawned, floor);
					throw e;
				}
				finishOnceHealed(helper, p, spawned, floor);
			});
		});
	}

	/** A living caster whose ghost path stands at Void Shattering Grand Perfection, its foundations understood to the Void-Rending Nether Canon. */
	private static ServerPlayer ghostCaster(GameTestHelper helper, BlockPos at) {
		ServerPlayer p = caster(helper, QiElement.WATER, at);
		NetherworldState s = CultivationManager.get(p).netherworld();
		for (GhostMethod method : GhostMethod.values()) {
			if (method.isFoundation()) s.setMethod(method, method.realm().ordinal() <= Realm.VOID_SHATTERING.ordinal() ? 1 : 0, 0);
		}
		GhostMethods.setPath(s, PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.GRAND_PERFECTION));
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

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "void_nethers", timeoutTicks = 400)
	public void theRiftToTheNineNethersDragsDownAndSeals(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 44, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = ghostCaster(helper, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 20);
		Husk held = husk(level, point.add(3, 0, 0), spawned), drawn = mobileHusk(level, point.add(14, 0, 0), spawned),
				beyond = husk(level, point.add(-(GhostSpells.NETHERS_RADIUS + 6), 0, 0), spawned);
		HeavenSpellTesting.lookAt(p, point);
		int stone = floorLeft(level, floor.centre(), point, 12);
		boolean quiet = Devastation.pending() == 0;
		helper.assertTrue(Spell.RIFT_TO_THE_NINE_NETHERS.isKnown(CultivationManager.get(p)), "Its ghost path carries the Void-Rending Nether Canon to its spell");
		helper.assertTrue(took(p, Spell.RIFT_TO_THE_NINE_NETHERS), "The Rift to the Nine Nethers is cast");
		helper.assertTrue(constructs(level, point, 6, SpellConstruct.Kind.ABYSS_RIFT).size() == 1 && constructs(level, point, 6, SpellConstruct.Kind.NINE_NETHERS).size() == 1,
				"...the ground tears open onto the Nine Nethers, rings of ghost fire turning down into it");
		helper.runAfterDelay(GhostSpells.NETHERS_OPENS + 50, () -> {
			double off = Math.hypot(drawn.getX() - point.x, drawn.getZ() - point.z);
			if (off >= 9 || held.getHealth() > 4000 - 2 * GhostSpells.NETHERS_DRAIN + 1) {
				finish(helper, p, spawned, floor);
				helper.fail("A foe near it is dragged toward it (" + off + " off) and drained each second (" + held.getHealth() + ")");
				return;
			}
			helper.runAfterDelay(GhostSpells.NETHERS_TICKS - 50 + GhostSpells.NETHERS_SEALS + 3, () -> {
				try {
					helper.assertTrue(held.getHealth() <= 4000 - 6 * GhostSpells.NETHERS_DRAIN - GhostSpells.NETHERS_SEAL + 1, "...then it seals on what stands at its mouth (" + held.getHealth() + ")");
					helper.assertTrue(held.hasEffect(MobEffects.WEAKNESS), "...weakening it");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond its reach");
					helper.assertTrue(landKept(level, floor, point, 12, stone, quiet), "...and the land is left as it was");
					helper.succeed();
				} finally {
					finish(helper, p, spawned, floor);
				}
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "void_bridge", timeoutTicks = 300)
	public void theBridgeOfNoReturnTakesItsToll(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 46, 12);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = ghostCaster(helper, floor.centre());
		Vec3 home = p.position();
		Husk near = husk(level, home.add(1, 0, 10), spawned), far = husk(level, home.add(-2, 0, 40), spawned), aside = husk(level, home.add(GhostSpells.BRIDGE_WIDTH + 8, 0, 20), spawned);
		near.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 60, 0));
		near.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 60, 1));
		HeavenSpellTesting.lookAt(p, home.add(0, 1.6, 30));
		int stone = floorLeft(level, floor.centre(), home.add(0, 0, 24), 6);
		boolean quiet = Devastation.pending() == 0;
		helper.assertTrue(took(p, Spell.BRIDGE_OF_NO_RETURN), "The Bridge of No Return is cast");
		helper.runAfterDelay(GhostSpells.BRIDGE_SPANS * GhostSpells.BRIDGE_EVERY + 2, () -> {
			// Its first two spans, over the floor (its last rises 108 blocks out, where the test world's land may not be loaded: a span there
			// waits unseen till it is).
			int first = constructs(level, home.add(0, 0, GhostSpells.BRIDGE_SPAN * 0.5), 3, SpellConstruct.Kind.NAIHE_BRIDGE).size(),
					second = constructs(level, home.add(0, 0, GhostSpells.BRIDGE_SPAN * 1.5), 3, SpellConstruct.Kind.NAIHE_BRIDGE).size();
			if (first != 1 || second != 1) {
				finish(helper, p, spawned, floor);
				helper.fail("The bridge rises span after span along the look (" + first + ", " + second + ")");
				return;
			}
			if (near.getHealth() > 4000 - GhostSpells.BRIDGE_DAMAGE + 1 || far.getHealth() > 4000 - GhostSpells.BRIDGE_DAMAGE + 1
					|| near.hasEffect(MobEffects.REGENERATION) || near.hasEffect(MobEffects.DAMAGE_BOOST)) {
				finish(helper, p, spawned, floor);
				helper.fail("All it rises by are struck and forget every blessing on them (" + near.getHealth() + ", " + far.getHealth() + ")");
				return;
			}
			helper.runAfterDelay(20 + 12, () -> {
				try {
					helper.assertTrue(near.getHealth() <= 4000 - GhostSpells.BRIDGE_DAMAGE - GhostSpells.BRIDGE_TOLL + 1, "...and whoever stays by it pays its toll (" + near.getHealth() + ")");
					helper.assertTrue(near.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "...slowed");
					helper.assertTrue(aside.getHealth() == aside.getMaxHealth(), "...not what stands well off it");
					helper.assertTrue(landKept(level, floor, home.add(0, 0, 24), 6, stone, quiet), "...and the land is left as it was");
					helper.succeed();
				} finally {
					finish(helper, p, spawned, floor);
				}
			});
		});
	}

	/** A tiger demon of Void Shattering Grand Perfection on the heaven path and the Demon Path both. */
	private static ServerPlayer demonCaster(GameTestHelper helper, BlockPos at) {
		ServerPlayer p = caster(helper, QiElement.METAL, at);
		PlayerCultivation c = CultivationManager.get(p);
		c.demon().setBloodline(Bloodline.TIGER);
		c.demon().set(PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.GRAND_PERFECTION), 0);
		CultivationManager.refresh(p);
		c.setQi(c.maxQi());
		p.setHealth(p.getMaxHealth());
		return p;
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "void_shatter", timeoutTicks = 300)
	public void theFistOfBothPathsShattersSpace(GameTestHelper helper) {
		// Wide enough for its cracks (HeavenDemonSpells#SHATTER_CRACK_REACH): past the floor they would cut the test world's own ground.
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 80, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = demonCaster(helper, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 22);
		Husk struck = husk(level, point, spawned), cut = husk(level, point.add(5, 0, 2), spawned), beyond = husk(level, point.add(-(HeavenDemonSpells.SHATTER_RADIUS + 6), 0, 0), spawned);
		// Ground under where it lands: its crater would cut the one-block floor and drop those it cuts out of its shattered space.
		Runnable thinned = HeavenSpellTesting.thicken(floor, point.x, point.z, 31, 18);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(struck));
		int stone = floorLeft(level, floor.centre(), point, 10);
		boolean quiet = Devastation.pending() == 0;
		helper.assertTrue(Spell.HEAVEN_DEMON_VOID_SHATTER.isKnown(CultivationManager.get(p)), "Both paths at Void Shattering: the Void Shatter is theirs");
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.HEAVEN_DEMON_VOID_SHATTER), "The Heaven-Demon Void Shatter is cast, paid for in qi");
		helper.assertTrue(constructs(level, home, 10, SpellConstruct.Kind.HEAVEN_DEMON_FIST).size() == 1, "...its fist forms over its caster's shoulder");
		helper.runAfterDelay(HeavenDemonSpells.SHATTER_FLIGHT + 2, () -> {
			if (struck.getHealth() > 4000 - HeavenDemonSpells.SHATTER_DAMAGE + 1 || constructs(level, point, 8, SpellConstruct.Kind.SHATTERED_SPACE).size() != 1) {
				thinned.run();
				finish(helper, p, spawned, floor);
				helper.fail("It lands where its caster looks, and space shatters there (" + struck.getHealth() + ")");
				return;
			}
			helper.runAfterDelay(40, () -> {
				try {
					helper.assertTrue(cut.getHealth() <= 4000 - HeavenDemonSpells.SHATTER_DAMAGE - HeavenDemonSpells.SHATTER_CUTS + 1, "...its shattered space cutting whoever stays in it (" + cut.getHealth() + ")");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond its reach");
					// The land shatters with the space over it (since 2026-10-24: it was left as it was).
					helper.assertTrue(floorLeft(level, floor.centre(), point, 10) < stone, "...and the land under it shattered into a crater");
				} catch (RuntimeException e) {
					thinned.run();
					finish(helper, p, spawned, floor);
					throw e;
				}
				HeavenSpells.forget(p.getUUID());
				for (SpellConstruct c : level.getEntitiesOfClass(SpellConstruct.class, new AABB(floor.centre()).inflate(200))) c.discard();
				HeavenSpellTesting.succeedOnceHealed(helper, p, spawned, floor, thinned);
			});
		});
	}

	/** A cultivator's Void Shattering spells strike as a player's do but leave the land whole (HeavenSpells#carvesLand). */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "void_cultivator", timeoutTicks = 300)
	public void aCultivatorsVoidSpellsLeaveTheLand(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 36, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer watcher = caster(helper, QiElement.METAL, floor.centre().offset(-30, 0, -30));
		BlockPos at = floor.centre();
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, helper.relativePos(at));
		npc.setNoAi(true);
		npc.setRank(PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.EARLY));
		npc.setHealth(npc.getMaxHealth());
		npc.cultivation().setQi(npc.cultivation().maxQi());
		spawned.add(npc);
		Vec3 home = npc.position();
		Husk foe = husk(level, home.add(0, 0, 16), spawned);
		npc.setTarget(foe);
		CultivatorSpells.face(npc, foe);
		int stone = floorLeft(level, floor.centre(), home, 30);
		boolean quiet = Devastation.pending() == 0;
		helper.assertTrue(Spells.castBy(npc, Spell.BUZHOU_FALLS), "A cultivator casts Buzhou Falls");
		helper.runAfterDelay(EarthSpells.BUZHOU_OPENS + EarthSpells.BUZHOU_FALL + 4, () -> {
			try {
				helper.assertTrue(foe.getHealth() <= 4000 - EarthSpells.BUZHOU_DAMAGE + 1, "Its pillar falls on its foe (" + foe.getHealth() + ")");
				helper.assertTrue(Spells.castBy(npc, Spell.GOLDEN_CROW_DESCENDS) || npc.cultivation().getQi() < Spell.GOLDEN_CROW_DESCENDS.qiCost(), "...and the Golden Crow after it");
				helper.runAfterDelay(FireSpells.CROW_OPENS + FireSpells.CROW_DIVE + 4, () -> {
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
