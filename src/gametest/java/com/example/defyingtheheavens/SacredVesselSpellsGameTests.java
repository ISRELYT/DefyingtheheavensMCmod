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
 * The Sacred Vessel spells (2026-10-24: the sacred treasures of the old tales, each a vessel with a world's power held in it; the ghost
 * foundation's Mirror of Retribution and Soul-Locking Pagoda, and the Heaven-Demon Bell of both paths), each cast by a caster of that realm
 * on a floor high over its spot: its treasure comes (SpellConstruct), it strikes whom it should and spares whom it should, and a player's
 * works the land as it says (the ghost spells and the ropes leave it be); a cultivator's leaves the land whole. Each in a batch of its own.
 */
public class SacredVesselSpellsGameTests implements FabricGameTest {
	/** The husks' health: the realm's blows are heavy (3500 at the most), and a husk struck dead is gone before it can be weighed. */
	private static final float HEALTH = 16000; // more than three times its caster's: a great beast to it (HeavenSpells#GREAT_BEYOND)

	/** A Sacred Vessel Grand Perfection caster who knows {@code element}'s method of that realm to its last layer, at {@code at}. */
	private static ServerPlayer caster(GameTestHelper helper, QiElement element, BlockPos at) {
		ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.FIRE_FOUR_AXIS, at);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(Realm.SACRED_VESSEL, Stage.GRAND_PERFECTION, 0);
		HeavenMethod method = HeavenMethod.of(element, Realm.SACRED_VESSEL);
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

	/** A husk that can be moved, to see a spell drag, lift or bind (VoidShatteringSpellsGameTests#mobileHusk): its wits kept, its goals taken, unhurtable. */
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

	/** How many blocks stand over the floor within {@code radius} of {@code at}, up to {@code high} (a mound raised on it). */
	private static int raised(ServerLevel level, BlockPos centre, Vec3 at, double radius, int high) {
		int n = 0;
		int r = (int) Math.ceil(radius);
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx + dz * dz > radius * radius) continue;
				for (int dy = 0; dy < high; dy++) {
					BlockPos pos = new BlockPos((int) Math.floor(at.x) + dx, centre.getY() + dy, (int) Math.floor(at.z) + dz);
					if (!level.getBlockState(pos).isAir()) n++;
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
	public void theSacredVesselSpellsComeInTheirPlaceAndGrow(GameTestHelper helper) {
		Spell[][] pairs = {{Spell.HEAVEN_SWALLOWING_GOURD, Spell.SEVEN_TREASURE_BODHI_BRANCH}, {Spell.WIND_FIRE_WHEELS, Spell.EIGHT_TRIGRAMS_FURNACE},
				{Spell.XIRANG_SELF_GROWING_SOIL, Spell.NINE_CAULDRONS_SUPPRESS_THE_LAND}, {Spell.TWENTY_FOUR_SEA_FIXING_PEARLS, Spell.OCEAN_SEALING_VASE},
				{Spell.IMMORTAL_BINDING_ROPES, Spell.GOLDEN_DRAGON_SCISSORS}, {Spell.MIRROR_OF_RETRIBUTION, Spell.SOUL_LOCKING_PAGODA}};
		for (Spell[] pair : pairs) {
			helper.assertTrue(pair[0].layer() == 75 && pair[1].layer() == 79, pair[0].getId() + " and " + pair[1].getId() + " at layers 75 and 79");
			for (Spell spell : pair) helper.assertTrue(spell.realm() == Realm.SACRED_VESSEL, spell.getId() + " is of Sacred Vessel");
		}
		helper.assertTrue(Spell.HEAVEN_DEMON_BELL.isDual() && Spell.HEAVEN_DEMON_BELL.dualRealm() == Realm.SACRED_VESSEL, "The Heaven-Demon Bell is both paths', of Sacred Vessel");
		helper.assertTrue(WoodSpells.GOURD_RANGE > WoodSpells.THUNDER_RANGE && FireSpells.FURNACE_RANGE > FireSpells.CROW_RANGE && EarthSpells.CAULDRONS_RANGE > EarthSpells.BUZHOU_RANGE
				&& WaterSpells.VASE_RANGE > WaterSpells.WEAK_RANGE && MetalSpells.SCISSORS_RANGE > MetalSpells.PRISON_RANGE && GhostSpells.PAGODA_RANGE > GhostSpells.NETHERS_RANGE
				&& HeavenDemonSpells.BELL_RANGE > HeavenDemonSpells.SHATTER_RANGE, "Sacred Vessel's reach is greater than Void Shattering's");
		SpellConstruct shape = SpellConstruct.create(helper.getLevel(), SpellConstruct.Kind.SWALLOWING_GOURD, Vec3.ZERO, 0, 0, 20, null).sizedFor(Spell.HEAVEN_SWALLOWING_GOURD);
		helper.assertTrue(HeavenSpellTesting.near(shape.size(), HeavenSpells.greatness(Realm.SACRED_VESSEL)) && shape.size() > HeavenSpells.greatness(Realm.VOID_SHATTERING),
				"...and its treasures drawn greater (" + shape.size() + ")");
		shape.discard();
		// Its treasures are models of their own; the golden ropes alone are drawn by hand.
		for (SpellConstruct.Kind kind : new SpellConstruct.Kind[] {SpellConstruct.Kind.SWALLOWING_GOURD, SpellConstruct.Kind.BODHI_BRANCH, SpellConstruct.Kind.WIND_FIRE_WHEEL,
				SpellConstruct.Kind.TRIGRAM_FURNACE, SpellConstruct.Kind.XIRANG, SpellConstruct.Kind.NINE_CAULDRON, SpellConstruct.Kind.SEA_PEARL, SpellConstruct.Kind.OCEAN_VASE,
				SpellConstruct.Kind.BINDING_RING, SpellConstruct.Kind.DRAGON_SCISSORS, SpellConstruct.Kind.KARMA_MIRROR, SpellConstruct.Kind.SOUL_PAGODA, SpellConstruct.Kind.HEAVEN_DEMON_BELL}) {
			helper.assertTrue(kind.modelled(), kind + " is a model of its own");
		}
		helper.assertTrue(!SpellConstruct.Kind.GOLDEN_ROPE.modelled(), "...and its ropes are drawn by hand");
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "vessel_gourd", timeoutTicks = 400)
	public void theGourdSwallowsDrainsAndSpits(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 40, 28);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.WOOD, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 18);
		// A great beast to be drained where it stands (it is looked at: the gourd hangs over it), one light enough to be swallowed, one beyond.
		Husk drained = husk(level, point, spawned), drawn = mobileHusk(level, point.add(6, 0, 0), spawned),
				beyond = husk(level, point.add(-(WoodSpells.GOURD_RADIUS + 6), 0, 0), spawned);
		Runnable thinned = HeavenSpellTesting.thicken(floor, point.x, point.z, 22, 8);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(drained));
		int stone = floorLeft(level, floor.centre(), point, 8);
		Vec3 mouth = point.add(0, WoodSpells.GOURD_HIGH, 0);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.HEAVEN_SWALLOWING_GOURD), "Heaven-Swallowing Gourd is cast, paid for");
		helper.assertTrue(constructs(level, mouth, 4, SpellConstruct.Kind.SWALLOWING_GOURD).size() == 1, "...its gourd comes over the spot");
		helper.runAfterDelay(WoodSpells.GOURD_DESCENDS + 60, () -> {
			String wrong = drawn.getY() < home.y + WoodSpells.GOURD_HIGH - 4 ? "What it can swallow is drawn up into its mouth (" + (drawn.getY() - home.y) + " up)"
					: drained.getHealth() > HEALTH - 2 * WoodSpells.GOURD_DRAIN + 1 ? "...a great beast drained where it stands (" + drained.getHealth() + ")"
					: floorLeft(level, floor.centre(), point, 8) >= stone ? "...and the land under it torn up into it" : null;
			if (wrong != null) {
				thinned.run();
				finish(helper, p, spawned, floor);
				helper.fail(wrong);
				return;
			}
			helper.runAfterDelay(WoodSpells.GOURD_TICKS - 60 + 8, () -> {
				try {
					helper.assertTrue(drained.getHealth() <= HEALTH - 5 * WoodSpells.GOURD_DRAIN + 1, "...drained each second it draws (" + drained.getHealth() + ")");
					double off = Math.hypot(drawn.getX() - mouth.x, drawn.getZ() - mouth.z);
					helper.assertTrue(off > WoodSpells.GOURD_MOUTH * 1.5 || drawn.getY() < mouth.y - WoodSpells.GOURD_MOUTH * 2, "Then it spits out what it swallowed (" + off + " off)");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not reaching what stands beyond it");
				} catch (RuntimeException e) {
					thinned.run();
					finish(helper, p, spawned, floor);
					throw e;
				}
				finishOnceHealed(helper, p, spawned, floor, thinned);
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "vessel_branch", timeoutTicks = 400)
	public void theBodhiTreeSeizesAndItsTreasuresFall(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 44, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.WOOD, floor.centre());
		Vec3 home = p.position();
		// Facing +z: the tree planted 20 ahead; one foe beside where it grows (seized, a treasure on it), one far behind its caster (out of its roots' reach).
		Vec3 tree = home.add(0, 0, 20);
		Husk near = husk(level, tree.add(4, 0, 2), spawned), far = husk(level, home.add(0, 0, -WoodSpells.BODHI_RADIUS), spawned);
		HeavenSpellTesting.lookAt(p, tree);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.SEVEN_TREASURE_BODHI_BRANCH), "Seven-Treasure Bodhi Branch is cast, paid for");
		helper.assertTrue(constructs(level, tree, 8, SpellConstruct.Kind.BODHI_TREE).size() == 1, "...its tree grows where its caster looks");
		helper.runAfterDelay(WoodSpells.BODHI_GROWS + WoodSpells.BODHI_HOLDS + 7 * WoodSpells.BODHI_EVERY + 8, () -> {
			try {
				helper.assertTrue(near.getHealth() <= HEALTH - WoodSpells.BODHI_TREASURE + 1, "Its treasures fall on the foes it holds (" + near.getHealth() + ")");
				helper.assertTrue(far.getHealth() == far.getMaxHealth(), "...not on what stands beyond its roots' reach");
			} catch (RuntimeException e) {
				finish(helper, p, spawned, floor);
				throw e;
			}
			finishOnceHealed(helper, p, spawned, floor, () -> {});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "vessel_wheels", timeoutTicks = 300)
	public void theWindFireWheelsRollOutBurning(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 44, 16);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.FIRE, floor.centre());
		Vec3 home = p.position();
		HeavenSpellTesting.lookAt(p, home.add(0, p.getEyeHeight(), 30));
		// A tenth of the way along their run each wheel is out to its side: one husk in each one's way, one well off both.
		double t = 0.1, side = 2.5 * (1 - t) + FireSpells.WHEELS_WEAVE * Math.sin(2 * Math.PI * t), along = 4 + FireSpells.WHEELS_LENGTH * t;
		Husk right = husk(level, home.add(-side, 0, along), spawned), left = husk(level, home.add(side, 0, along), spawned), aside = husk(level, home.add(side + 16, 0, along), spawned);
		int stone = floorLeft(level, floor.centre(), home.add(side, 0, along), 2);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.WIND_FIRE_WHEELS), "Wind-Fire Wheels is cast, paid for");
		helper.assertTrue(constructs(level, home, 12, SpellConstruct.Kind.WIND_FIRE_WHEEL).size() == 2, "...its two wheels flare up before its caster");
		helper.runAfterDelay(FireSpells.WHEELS_IGNITE + (int) (FireSpells.WHEELS_RUN * t) + 6, () -> {
			try {
				helper.assertTrue(right.getHealth() <= HEALTH - FireSpells.WHEELS_DAMAGE + 1 && left.getHealth() <= HEALTH - FireSpells.WHEELS_DAMAGE + 1,
						"Each wheel runs over what is in its way (" + right.getHealth() + ", " + left.getHealth() + ")");
				helper.assertTrue(right.getRemainingFireTicks() > 0, "...setting it burning");
				helper.assertTrue(aside.getHealth() == aside.getMaxHealth(), "...not what stands well off their way");
				helper.assertTrue(floorLeft(level, floor.centre(), home.add(side, 0, along), 2) < stone, "...and burns a trench into the land as it rolls");
			} catch (RuntimeException e) {
				finish(helper, p, spawned, floor);
				throw e;
			}
			finishOnceHealed(helper, p, spawned, floor, () -> {});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "vessel_furnace", timeoutTicks = 400)
	public void theFurnaceHoldsJetsAndBursts(GameTestHelper helper) {
		// Wide enough for its jets (FireSpells#FURNACE_JETS): past the floor they would burn the test world's own ground.
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 60, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.FIRE, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 8);
		// In its belly (looked at), one to be drawn in, one in the way of its first window's jet (out past its reach), one behind it, beyond both.
		Vec3 jet = Vec3.directionFromRotation(0, 22.5f);
		Husk inside = husk(level, point, spawned), drawn = mobileHusk(level, point.add(16, 0, 0), spawned), jetted = husk(level, point.add(jet.scale(FireSpells.FURNACE_RADIUS + 9)), spawned),
				beyond = husk(level, point.add(0, 0, -(FireSpells.FURNACE_RADIUS + 6)), spawned);
		Runnable thinned = HeavenSpellTesting.thicken(floor, point.x, point.z, 14, 10);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(inside));
		int stone = floorLeft(level, floor.centre(), point, 8);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.EIGHT_TRIGRAMS_FURNACE), "Eight Trigrams Furnace is cast, paid for");
		helper.assertTrue(constructs(level, point.add(0, FireSpells.FURNACE_HIGH, 0), 4, SpellConstruct.Kind.TRIGRAM_FURNACE).size() == 1, "...its furnace high over the spot");
		helper.runAfterDelay(FireSpells.FURNACE_FALL + FireSpells.FURNACE_JET_EVERY + 6, () -> {
			double off = Math.hypot(drawn.getX() - point.x, drawn.getZ() - point.z);
			String wrong = inside.getHealth() > HEALTH - FireSpells.FURNACE_SLAM - FireSpells.FURNACE_BURN + 1 ? "It lands and burns what is in its belly (" + inside.getHealth() + ")"
					: off > 12 ? "...drawing foes round it into its belly (" + off + " off)"
					: jetted.getHealth() > HEALTH - FireSpells.FURNACE_JET + 1 ? "...its windows jetting fire at what stands in their way (" + jetted.getHealth() + ")" : null;
			if (wrong != null) {
				thinned.run();
				finish(helper, p, spawned, floor);
				helper.fail(wrong);
				return;
			}
			int burst = FireSpells.FURNACE_FALL + FireSpells.FURNACE_TICKS + FireSpells.FURNACE_BURSTS;
			helper.runAfterDelay(burst - (FireSpells.FURNACE_FALL + FireSpells.FURNACE_JET_EVERY + 6) + 3, () -> {
				try {
					helper.assertTrue(inside.getHealth() <= HEALTH - FireSpells.FURNACE_SLAM - 6 * FireSpells.FURNACE_BURN - FireSpells.FURNACE_BURST + 1,
							"...burning it each second, then bursting open (" + inside.getHealth() + ")");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not reaching what stands beyond it");
					helper.assertTrue(floorLeft(level, floor.centre(), point, 8) < stone, "...the ground under it melted into a pit");
				} catch (RuntimeException e) {
					thinned.run();
					finish(helper, p, spawned, floor);
					throw e;
				}
				finishOnceHealed(helper, p, spawned, floor, thinned);
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "vessel_xirang", timeoutTicks = 300)
	public void xirangSwellsIntoAMoundAndBuries(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 36, 20);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.EARTH, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 20);
		Husk buried = husk(level, point.add(4, 0, 0), spawned), beyond = husk(level, point.add(EarthSpells.XIRANG_RADIUS + 5, 0, 0), spawned);
		HeavenSpellTesting.lookAt(p, point.add(0, 0.5, 0));
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.XIRANG_SELF_GROWING_SOIL), "Xirang, the Self-Growing Soil is cast, paid for");
		helper.assertTrue(constructs(level, home, 6, SpellConstruct.Kind.CLOD_MASS).size() == 1, "...its clod in its caster's hands");
		helper.runAfterDelay(EarthSpells.XIRANG_FLIES + 25, () -> {
			int mound = raised(level, floor.centre(), point, 8, (int) Math.ceil(EarthSpells.XIRANG_HEIGHT) + 2);
			boolean held = HeavenSpells.lingering(p.getUUID(), WoodSpells.Bound.class).stream().anyMatch(b -> b.target == buried);
			if (mound < 40 || !held) {
				finish(helper, p, spawned, floor);
				helper.fail("It swells: a mound heaves up out of the land there (" + mound + " blocks), burying and holding what stands on it (" + held + ")");
				return;
			}
			helper.runAfterDelay(EarthSpells.XIRANG_SWELLS + EarthSpells.XIRANG_HOLDS - 25 + 3, () -> {
				try {
					helper.assertTrue(buried.getHealth() <= HEALTH - 5 * EarthSpells.XIRANG_DAMAGE - EarthSpells.XIRANG_SETTLE + 1, "...crushed each second, then settled on (" + buried.getHealth() + ")");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond it");
					helper.assertTrue(raised(level, floor.centre(), point, 8, (int) Math.ceil(EarthSpells.XIRANG_HEIGHT) + 2) >= mound * 0.8, "...and the mound stays");
				} catch (RuntimeException e) {
					finish(helper, p, spawned, floor);
					throw e;
				}
				finishOnceHealed(helper, p, spawned, floor, () -> {});
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "vessel_cauldrons", timeoutTicks = 400)
	public void theNineCauldronsFallAndSuppress(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 48, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.EARTH, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 6);
		// At the heart (looked at), under the ring's first cauldron (the way its caster looks), inside the ring clear of every cauldron, and
		// outside it between two of them.
		Vec3 out = Vec3.directionFromRotation(0, 180 + 22.5f).scale(EarthSpells.CAULDRONS_RING + 12);
		Husk heart = husk(level, point, spawned), ringed = husk(level, point.add(0, 0, EarthSpells.CAULDRONS_RING), spawned), pressed = husk(level, point.add(-14, 0, 6), spawned),
				beyond = husk(level, point.add(out), spawned);
		Runnable thinned = HeavenSpellTesting.thicken(floor, point.x, point.z, (int) EarthSpells.CAULDRONS_RING + 4, 10);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(heart));
		int stone = floorLeft(level, floor.centre(), point.add(-14, 0, 6), 4);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.NINE_CAULDRONS_SUPPRESS_THE_LAND), "Nine Cauldrons Suppress the Land is cast, paid for");
		helper.runAfterDelay(EarthSpells.CAULDRON_EVERY + EarthSpells.CAULDRON_FALL + 2, () -> {
			if (heart.getHealth() > HEALTH - EarthSpells.CAULDRON_DAMAGE + 1 || ringed.getHealth() > HEALTH - EarthSpells.CAULDRON_DAMAGE + 1) {
				thinned.run();
				finish(helper, p, spawned, floor);
				helper.fail("Its cauldrons fall one after another, on the heart and round it (" + heart.getHealth() + ", " + ringed.getHealth() + ")");
				return;
			}
			int landed = 8 * EarthSpells.CAULDRON_EVERY + EarthSpells.CAULDRON_FALL;
			helper.runAfterDelay(landed + 45 - (EarthSpells.CAULDRON_EVERY + EarthSpells.CAULDRON_FALL + 2), () -> {
				try {
					helper.assertTrue(constructs(level, point, EarthSpells.CAULDRONS_RING + 6, SpellConstruct.Kind.CAULDRON_MASS).size() == 9, "All nine stand");
					helper.assertTrue(pressed.getHealth() <= HEALTH - 3 * EarthSpells.CAULDRONS_PRESS + 1, "...pressing every foe inside the ring each second (" + pressed.getHealth() + ")");
					helper.assertTrue(pressed.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && pressed.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() >= 4, "...slowed to a crawl");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands outside it");
					helper.assertTrue(floorLeft(level, floor.centre(), point.add(-14, 0, 6), 4) < stone, "...and the land within sinks");
				} catch (RuntimeException e) {
					thinned.run();
					finish(helper, p, spawned, floor);
					throw e;
				}
				finishOnceHealed(helper, p, spawned, floor, thinned);
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "vessel_pearls", timeoutTicks = 300)
	public void thePearlsFlyRoundTheFoes(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 36, 12);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.WATER, floor.centre());
		Vec3 home = p.position();
		Husk near = husk(level, home.add(0, 0, 12), spawned), far = husk(level, home.add(10, 0, 18), spawned);
		// Ground under them deep enough for a dozen little craters each.
		Runnable underNear = HeavenSpellTesting.thicken(floor, near.getX(), near.getZ(), 6, 16), underFar = HeavenSpellTesting.thicken(floor, far.getX(), far.getZ(), 6, 16);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(near));
		int stone = floorLeft(level, floor.centre(), near.position(), 2);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.TWENTY_FOUR_SEA_FIXING_PEARLS), "Twenty-Four Sea-Fixing Pearls is cast, paid for");
		helper.assertTrue(constructs(level, home, WaterSpells.PEARLS_CIRCLE + 3, SpellConstruct.Kind.PEARL).size() == WaterSpells.PEARLS, "...its twenty-four pearls unwind round its caster");
		helper.runAfterDelay(WaterSpells.PEARLS_FORM + WaterSpells.PEARLS * WaterSpells.PEARL_EVERY + WaterSpells.PEARL_FLIGHT + 4, () -> {
			try {
				helper.assertTrue(near.getHealth() <= HEALTH - 10 * WaterSpells.PEARL_DAMAGE + 1 && far.getHealth() <= HEALTH - 10 * WaterSpells.PEARL_DAMAGE + 1,
						"They fly off one after another at the foes, round and round them (" + near.getHealth() + ", " + far.getHealth() + ")");
				helper.assertTrue(constructs(level, home, 60, SpellConstruct.Kind.PEARL).stream().allMatch(c -> c.life() <= c.tickCount + SpellConstruct.FADE), "...each gone as it bursts");
				helper.assertTrue(floorLeft(level, floor.centre(), near.position(), 2) < stone || near.getY() < home.y - 0.5, "...leaving little craters");
			} catch (RuntimeException e) {
				underNear.run();
				underFar.run();
				finish(helper, p, spawned, floor);
				throw e;
			}
			finishOnceHealed(helper, p, spawned, floor, () -> {
				underNear.run();
				underFar.run();
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "vessel_vase", timeoutTicks = 400)
	public void theVaseDrinksSealsAndComesDown(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 48, 36);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.WATER, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 16);
		// A great beast under it (looked at), one light enough to be drawn up into its mouth, one beyond; and a pool for it to drink.
		Husk crushed = husk(level, point, spawned), drawn = mobileHusk(level, point.add(5, 0, 0), spawned), beyond = husk(level, point.add(0, 0, -(WaterSpells.VASE_RADIUS + 5)), spawned);
		Runnable thinned = HeavenSpellTesting.thicken(floor, point.x, point.z, (int) WaterSpells.VASE_CRATER + 3, 10);
		BlockPos pool = BlockPos.containing(point.x + 12, floor.centre().getY() - 1, point.z);
		level.setBlock(pool, Blocks.WATER.defaultBlockState(), 2);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(crushed));
		int stone = floorLeft(level, floor.centre(), point, 8);
		Vec3 mouth = point.add(0, WaterSpells.VASE_HIGH, 0);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.OCEAN_SEALING_VASE), "Ocean-Sealing Vase is cast, paid for");
		helper.assertTrue(constructs(level, mouth, 4, SpellConstruct.Kind.OCEAN_VASE).size() == 1, "...its vase comes over the spot");
		helper.runAfterDelay(WaterSpells.VASE_COMES + 52, () -> {
			String wrong = !level.getBlockState(pool).getFluidState().isEmpty() ? "It drinks the water out of the land round it"
					: drawn.getY() < mouth.y - WaterSpells.VASE_MOUTH * 2 ? "...drawing what it can up into its mouth (" + (drawn.getY() - home.y) + " up)"
					: crushed.getHealth() > HEALTH - 3 * WaterSpells.VASE_CRUSH + 1 ? "...crushing a great beast where it stands (" + crushed.getHealth() + ")" : null;
			if (wrong != null) {
				thinned.run();
				finish(helper, p, spawned, floor);
				helper.fail(wrong);
				return;
			}
			int slam = WaterSpells.VASE_COMES + WaterSpells.VASE_TICKS + WaterSpells.VASE_SEALS + WaterSpells.VASE_FALL;
			helper.runAfterDelay(slam - (WaterSpells.VASE_COMES + 52) + 3, () -> {
				try {
					helper.assertTrue(crushed.getHealth() <= HEALTH - 5 * WaterSpells.VASE_CRUSH - WaterSpells.VASE_SLAM + 1, "...then it seals and comes down on it (" + crushed.getHealth() + ")");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not reaching what stands beyond it");
					helper.assertTrue(floorLeft(level, floor.centre(), point, 8) < stone, "...the ocean it held poured back into a crater");
				} catch (RuntimeException e) {
					thinned.run();
					finish(helper, p, spawned, floor);
					throw e;
				}
				finishOnceHealed(helper, p, spawned, floor, thinned);
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "vessel_ropes", timeoutTicks = 300)
	public void theGoldenRopesBindHaulAndCinch(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 40, 26);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.METAL, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 16);
		Husk great = husk(level, point, spawned), hauled = mobileHusk(level, point.add(4, 0, 0), spawned), beyond = husk(level, point.add(MetalSpells.ROPES_RADIUS + 5, 0, 0), spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(great));
		int stone = floorLeft(level, floor.centre(), point, 10);
		boolean quiet = Devastation.pending() == 0;
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.IMMORTAL_BINDING_ROPES), "Immortal-Binding Golden Ropes is cast, paid for");
		helper.assertTrue(constructs(level, point.add(0, MetalSpells.ROPES_HIGH, 0), 4, SpellConstruct.Kind.BINDING_RING).size() == 1, "...its ring comes over the spot");
		helper.runAfterDelay(MetalSpells.ROPES_COMES + 35, () -> {
			List<SpellConstruct> ropes = constructs(level, point, 40, SpellConstruct.Kind.GOLDEN_ROPE);
			String wrong = ropes.stream().noneMatch(c -> c.target() == great) || ropes.stream().noneMatch(c -> c.target() == hauled) ? "Its ropes lash out to each foe near the spot (" + ropes.size() + ")"
					: ropes.stream().anyMatch(c -> c.target() == beyond) ? "...none beyond its reach"
					: hauled.getY() < home.y + MetalSpells.ROPES_HIGH - MetalSpells.ROPES_HANG - 3 ? "...hauling what it can up to hang under the ring (" + (hauled.getY() - home.y) + " up)"
					: great.position().distanceTo(point) > 1.5 ? "...a great beast bound where it stands (" + great.position().distanceTo(point) + " off)"
					: great.getHealth() > HEALTH - 2 * MetalSpells.ROPES_DAMAGE + 1 ? "...constricting them each second (" + great.getHealth() + ")" : null;
			if (wrong != null) {
				finish(helper, p, spawned, floor);
				helper.fail(wrong);
				return;
			}
			helper.runAfterDelay(MetalSpells.ROPES_TICKS - 35 + 3, () -> {
				try {
					helper.assertTrue(great.getHealth() <= HEALTH - 5 * MetalSpells.ROPES_DAMAGE - MetalSpells.ROPES_CINCH + 1, "...then they cinch (" + great.getHealth() + ")");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond its reach");
					helper.assertTrue(landKept(level, floor, point, 10, stone, quiet), "...and the land is left as it was");
					helper.succeed();
				} finally {
					finish(helper, p, spawned, floor);
				}
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "vessel_scissors", timeoutTicks = 300)
	public void theDragonScissorsSnapShut(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 48, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.METAL, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 20);
		// Facing +z, its blades lie across the sight, their points to the caster's right (-x), its pivot half a blade to their left (+x).
		Vec3 pivot = point.add(MetalSpells.SCISSORS_LENGTH / 2, 0, 0);
		Husk middle = husk(level, point, spawned), between = husk(level, point.add(0, 0, 10), spawned), byPivot = husk(level, pivot.add(-4, 0, 9), spawned),
				pastPoints = husk(level, point.add(-(MetalSpells.SCISSORS_LENGTH / 2 + 8), 0, 0), spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(middle));
		int stone = floorLeft(level, floor.centre(), point, 2);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.GOLDEN_DRAGON_SCISSORS), "Golden Dragon Scissors is cast, paid for");
		helper.assertTrue(constructs(level, pivot.add(0, MetalSpells.SCISSORS_HIGH, 0), 3, SpellConstruct.Kind.DRAGON_SCISSORS).size() == 1,
				"...its scissors open over the spot, their pivot half a blade to its caster's left");
		helper.runAfterDelay(MetalSpells.SCISSORS_OPENS + MetalSpells.SCISSORS_HOLD + MetalSpells.SCISSORS_SNAP + 3, () -> {
			try {
				helper.assertTrue(middle.getHealth() <= HEALTH - MetalSpells.SCISSORS_DAMAGE + 1 && between.getHealth() <= HEALTH - MetalSpells.SCISSORS_DAMAGE + 1,
						"They snap shut on everything between their blades (" + middle.getHealth() + ", " + between.getHealth() + ")");
				helper.assertTrue(byPivot.getHealth() == byPivot.getMaxHealth(), "...not what stands outside them by the pivot");
				helper.assertTrue(pastPoints.getHealth() == pastPoints.getMaxHealth(), "...nor past their points");
				helper.assertTrue(floorLeft(level, floor.centre(), point, 2) < stone, "...and the land is cut where they meet");
			} catch (RuntimeException e) {
				finish(helper, p, spawned, floor);
				throw e;
			}
			finishOnceHealed(helper, p, spawned, floor, () -> {});
		});
	}

	/** A living caster whose ghost path stands at Sacred Vessel Grand Perfection, its foundations understood to the Hell-Forged Vessel Sutra. */
	private static ServerPlayer ghostCaster(GameTestHelper helper, BlockPos at) {
		ServerPlayer p = caster(helper, QiElement.WATER, at);
		NetherworldState s = CultivationManager.get(p).netherworld();
		for (GhostMethod method : GhostMethod.values()) {
			if (method.isFoundation()) s.setMethod(method, method.realm().ordinal() <= Realm.SACRED_VESSEL.ordinal() ? 1 : 0, 0);
		}
		GhostMethods.setPath(s, PlayerCultivation.rank(Realm.SACRED_VESSEL, Stage.GRAND_PERFECTION));
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

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "vessel_mirror", timeoutTicks = 300)
	public void theMirrorShowsEverySoulItsSins(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 40, 12);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = ghostCaster(helper, floor.centre());
		Vec3 home = p.position();
		Husk shown = husk(level, home.add(0, 0, 20), spawned), aside = husk(level, home.add(14, 0, 14), spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(shown));
		int stone = floorLeft(level, floor.centre(), home.add(0, 0, 20), 6);
		boolean quiet = Devastation.pending() == 0;
		helper.assertTrue(Spell.MIRROR_OF_RETRIBUTION.isKnown(CultivationManager.get(p)), "Its ghost path carries the Hell-Forged Vessel Sutra to its spell");
		helper.assertTrue(took(p, Spell.MIRROR_OF_RETRIBUTION), "The Mirror of Retribution is cast");
		helper.assertTrue(constructs(level, home, 16, SpellConstruct.Kind.KARMA_MIRROR).size() == 1, "...its mirror rises over its caster");
		helper.runAfterDelay(GhostSpells.MIRROR_TICKS + 3, () -> {
			try {
				helper.assertTrue(shown.getHealth() <= HEALTH - 3 * GhostSpells.MIRROR_DAMAGE - GhostSpells.MIRROR_FLASH + 1, "Its light strikes what it shines on each second, then flashes (" + shown.getHealth() + ")");
				helper.assertTrue(shown.hasEffect(MobEffects.GLOWING) && shown.hasEffect(MobEffects.WEAKNESS), "...showing it glowing, weakened");
				helper.assertTrue(aside.getHealth() == aside.getMaxHealth(), "...not what stands out of its light");
				helper.assertTrue(landKept(level, floor, home.add(0, 0, 20), 6, stone, quiet), "...and the land is left as it was");
				helper.succeed();
			} finally {
				finish(helper, p, spawned, floor);
			}
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "vessel_pagoda", timeoutTicks = 400)
	public void thePagodaLocksDrainsAndSeals(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 40, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = ghostCaster(helper, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 20);
		Husk locked = husk(level, point, spawned), stray = mobileHusk(level, point.add(-8, 0, 0), spawned), beyond = husk(level, point.add(GhostSpells.PAGODA_RADIUS + 6, 0, 0), spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(locked));
		int stone = floorLeft(level, floor.centre(), point, 10);
		boolean quiet = Devastation.pending() == 0;
		helper.assertTrue(took(p, Spell.SOUL_LOCKING_PAGODA), "The Soul-Locking Pagoda is cast");
		helper.assertTrue(constructs(level, point.add(0, GhostSpells.PAGODA_HIGH, 0), 4, SpellConstruct.Kind.PAGODA_MASS).size() == 1, "...its pagoda high over the spot");
		helper.runAfterDelay(GhostSpells.PAGODA_FALL + 15, () -> {
			// One locked in it tries to leave.
			stray.teleportTo(point.x - 25, stray.getY(), point.z);
			helper.runAfterDelay(2, () -> {
				double off = Math.hypot(stray.getX() - point.x, stray.getZ() - point.z);
				if (off > GhostSpells.PAGODA_RADIUS) {
					finish(helper, p, spawned, floor);
					helper.fail("What it has locked in can't leave it (" + off + " off)");
					return;
				}
				helper.runAfterDelay(GhostSpells.PAGODA_TICKS + GhostSpells.PAGODA_SEALS - 17 + 3, () -> {
					try {
						helper.assertTrue(locked.getHealth() <= HEALTH - GhostSpells.PAGODA_SLAM - 6 * GhostSpells.PAGODA_DRAIN - GhostSpells.PAGODA_SEAL + 1,
								"It lands on what is under it, drains it each second and seals (" + locked.getHealth() + ")");
						helper.assertTrue(locked.hasEffect(MobEffects.WEAKNESS), "...weakening it");
						helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not reaching what stands beyond it");
						helper.assertTrue(landKept(level, floor, point, 10, stone, quiet), "...and the land is left as it was");
						helper.succeed();
					} finally {
						finish(helper, p, spawned, floor);
					}
				});
			});
		});
	}

	/** A tiger demon of Sacred Vessel Grand Perfection on the heaven path and the Demon Path both. */
	private static ServerPlayer demonCaster(GameTestHelper helper, BlockPos at) {
		ServerPlayer p = caster(helper, QiElement.METAL, at);
		PlayerCultivation c = CultivationManager.get(p);
		c.demon().setBloodline(Bloodline.TIGER);
		c.demon().set(PlayerCultivation.rank(Realm.SACRED_VESSEL, Stage.GRAND_PERFECTION), 0);
		CultivationManager.refresh(p);
		c.setQi(c.maxQi());
		p.setHealth(p.getMaxHealth());
		return p;
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "vessel_bell", timeoutTicks = 300)
	public void theBellOfBothPathsTolls(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 48, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = demonCaster(helper, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 20);
		Husk under = husk(level, point, spawned), waved = husk(level, point.add(25, 0, 0), spawned), beyond = husk(level, point.add(0, 0, -(HeavenDemonSpells.BELL_WAVE + 6)), spawned);
		Runnable thinned = HeavenSpellTesting.thicken(floor, point.x, point.z, (int) HeavenDemonSpells.BELL_CRATER + 3, 8);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(under));
		p.setHealth(p.getMaxHealth() / 2);
		float hurt = p.getHealth();
		int stone = floorLeft(level, floor.centre(), point, 8);
		helper.assertTrue(Spell.HEAVEN_DEMON_BELL.isKnown(CultivationManager.get(p)), "Both paths at Sacred Vessel: the Bell is theirs");
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.HEAVEN_DEMON_BELL), "The Heaven-Demon Bell is cast, paid for in qi");
		helper.assertTrue(constructs(level, point.add(0, HeavenDemonSpells.BELL_HIGH, 0), 4, SpellConstruct.Kind.HEAVEN_DEMON_BELL).size() == 1, "...its bell high over the spot");
		int tolled = HeavenDemonSpells.BELL_FALL + HeavenDemonSpells.BELL_FIRST + (HeavenDemonSpells.BELL_TOLLS - 1) * HeavenDemonSpells.BELL_EVERY;
		helper.runAfterDelay(tolled + 3, () -> {
			try {
				helper.assertTrue(under.getHealth() <= HEALTH - HeavenDemonSpells.BELL_TOLLS * HeavenDemonSpells.BELL_TOLL + 1, "It tolls three times on what is under it (" + under.getHealth() + ")");
				helper.assertTrue(waved.getHealth() <= HEALTH - HeavenDemonSpells.BELL_WAVE_DAMAGE + 1, "...its wave striking what stands round it (" + waved.getHealth() + ")");
				helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond its wave");
				helper.assertTrue(p.getHealth() > hurt, "...a share of it mending its caster (" + hurt + " -> " + p.getHealth() + ")");
				helper.assertTrue(floorLeft(level, floor.centre(), point, 8) < stone, "...its rim pressed into the land");
			} catch (RuntimeException e) {
				thinned.run();
				finish(helper, p, spawned, floor);
				throw e;
			}
			finishOnceHealed(helper, p, spawned, floor, thinned);
		});
	}

	/** A cultivator's Sacred Vessel spells strike as a player's do but leave the land whole (HeavenSpells#carvesLand): no pit, no mound. */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "vessel_cultivator", timeoutTicks = 300)
	public void aCultivatorsVesselSpellsLeaveTheLand(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 36, 18);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer watcher = caster(helper, QiElement.METAL, floor.centre().offset(-30, 0, -30));
		BlockPos at = floor.centre();
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, helper.relativePos(at));
		npc.setNoAi(true);
		npc.setRank(PlayerCultivation.rank(Realm.SACRED_VESSEL, Stage.EARLY));
		npc.setHealth(npc.getMaxHealth());
		npc.cultivation().setQi(npc.cultivation().maxQi());
		spawned.add(npc);
		Vec3 home = npc.position();
		Husk foe = husk(level, home.add(0, 0, 16), spawned);
		npc.setTarget(foe);
		CultivatorSpells.face(npc, foe);
		int stone = floorLeft(level, floor.centre(), home, 30), over = raised(level, floor.centre(), foe.position(), 10, 14);
		boolean quiet = Devastation.pending() == 0;
		helper.assertTrue(Spells.castBy(npc, Spell.EIGHT_TRIGRAMS_FURNACE), "A cultivator casts the Eight Trigrams Furnace");
		helper.runAfterDelay(FireSpells.FURNACE_FALL + 4, () -> {
			try {
				helper.assertTrue(foe.getHealth() <= HEALTH - FireSpells.FURNACE_SLAM + 1, "Its furnace falls on its foe (" + foe.getHealth() + ")");
				helper.assertTrue(Spells.castBy(npc, Spell.XIRANG_SELF_GROWING_SOIL) || npc.cultivation().getQi() < Spell.XIRANG_SELF_GROWING_SOIL.qiCost(), "...and Xirang after it");
				helper.runAfterDelay(EarthSpells.XIRANG_FLIES + 10, () -> {
					try {
						helper.assertTrue(landKept(level, floor, home, 30, stone, quiet), "Nothing it cast took any of the land (" + stone + " -> " + floorLeft(level, floor.centre(), home, 30) + ")");
						helper.assertTrue(raised(level, floor.centre(), foe.position(), 10, 14) <= over, "...nor raised any on it");
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
