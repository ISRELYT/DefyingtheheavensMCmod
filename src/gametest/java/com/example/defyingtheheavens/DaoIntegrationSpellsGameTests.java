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
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Dao Integration spells (the five elements' guardian beasts and the realm's methods' own arts, 2026-10-24; the ghost foundation's
 * Procession and Gate, and the Heaven-Demon Avatar of both paths), each cast by a caster of that realm on a floor high over its spot: its
 * living shape (SpellConstruct) comes, it strikes whom it should and spares whom it should, and a player's cuts the land as it says (the
 * ghost and both paths' spells leave it be); a cultivator's leaves the land whole. Each in a batch of its own: they reach far and cut the
 * floor.
 */
public class DaoIntegrationSpellsGameTests implements FabricGameTest {
	/** A Dao Integration Grand Perfection caster who knows {@code element}'s method of that realm to its last layer, at {@code at}. */
	private static ServerPlayer caster(GameTestHelper helper, QiElement element, BlockPos at) {
		ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.FIRE_FOUR_AXIS, at);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(Realm.DAO_INTEGRATION, Stage.GRAND_PERFECTION, 0);
		HeavenMethod method = HeavenMethod.of(element, Realm.DAO_INTEGRATION);
		c.heaven().setMethod(method, method.layers(), 0);
		CultivationManager.refresh(p);
		c.setQi(c.maxQi());
		p.setHealth(p.getMaxHealth());
		return p;
	}

	/** A hardy husk to be struck (4000 health, fire-proof against burning's own harm), standing at {@code at}. */
	private static Husk husk(ServerLevel level, Vec3 at, List<LivingEntity> spawned) {
		Husk husk = HeavenSpellTesting.idle(level, EntityType.HUSK, at, 4000, spawned);
		husk.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 600, 0, false, false));
		return husk;
	}

	/**
	 * A husk that can be moved, to see a spell carry or drag: a creature with no will of its own has no motion either (vanilla moves only
	 * one with its wits, Mob#isEffectiveAi), so its wits are kept and every goal and aim it had taken from it; of 200 health (one of 300
	 * or more is a great beast, which no spell carries: HeavenSpells#greatBeast), and unhurtable so it lives through the spell.
	 */
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
		for (SpellConstruct c : helper.getLevel().getEntitiesOfClass(SpellConstruct.class, new AABB(floor.centre()).inflate(140))) c.discard();
		HeavenSpellTesting.cleanUp(p, spawned, floor);
	}

	/** The user's rule (2026-10-24): a spell's scale grows with its realm, its reach, areas and living shape alike, not only its wounds. */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void theGreatSpellsGrowWithTheirRealm(GameTestHelper helper) {
		Realm[] deep = {Realm.FOUR_AXIS, Realm.DAO_INTEGRATION, Realm.VOID_SHATTERING, Realm.SACRED_VESSEL, Realm.ENTERING_NIRVANA, Realm.TRUE_IMMORTAL};
		for (int i = 1; i < deep.length; i++) {
			helper.assertTrue(HeavenSpells.greatness(deep[i]) > HeavenSpells.greatness(deep[i - 1]), deep[i] + "'s spells greater than " + deep[i - 1] + "'s");
			helper.assertTrue(HeavenSpellTesting.near(HeavenSpells.greatness(deep[i]), Devastation.scale(deep[i])), "...by the measure their wounds grow by");
		}
		helper.assertTrue(HeavenSpells.greatness(Realm.FOUR_AXIS) == 1, "Four Axis's the yardstick");
		helper.assertTrue(WoodSpells.DRAGON_RANGE > 64 && WoodSpells.DRAGON_RADIUS > 14 && FireSpells.BIRD_SWEEP > 120 && FireSpells.FLAME_RADIUS > 8
				&& EarthSpells.QILIN_RUN > 100 && EarthSpells.MOUNTAINS_RADIUS > 14 && WaterSpells.RIVERS_RADIUS > 24 && WaterSpells.TORTOISE_REACH > 16
				&& MetalSpells.TIGER_GOUGE > 80 && MetalSpells.SWORDS_RADIUS > 12 && WoodSpells.WOODS_RADIUS > 32,
				"Dao Integration's reach and areas are greater than Four Axis's would be");
		SpellConstruct shape = SpellConstruct.create(helper.getLevel(), SpellConstruct.Kind.AZURE_DRAGON, Vec3.ZERO, 0, 0, 20, null).sizedFor(Spell.AZURE_DRAGON);
		helper.assertTrue(HeavenSpellTesting.near(shape.size(), HeavenSpells.greatness(Realm.DAO_INTEGRATION)) && shape.size() > 1, "...and its living shapes drawn greater (" + shape.size() + ")");
		shape.discard();
		// The ghost foundation's and both paths' alike, by their own realm (Spell#realm).
		helper.assertTrue(GhostSpells.PROCESSION_RANGE > 48 && GhostSpells.GATE_RADIUS > 14 && HeavenDemonSpells.AVATAR_RANGE > 64 && HeavenDemonSpells.AVATAR_RADIUS > 11,
				"Dao Integration's ghost and both paths' spells reach as far as their realm makes them");
		for (Spell spell : new Spell[] {Spell.GATE_OF_TEN_THOUSAND_GHOSTS, Spell.HEAVEN_DEMON_AVATAR}) {
			helper.assertTrue(spell.realm() == Realm.DAO_INTEGRATION, spell.getId() + " is of Dao Integration");
			SpellConstruct great = SpellConstruct.create(helper.getLevel(), SpellConstruct.Kind.GHOST_GATE, Vec3.ZERO, 0, 0, 20, null).sizedFor(spell);
			helper.assertTrue(HeavenSpellTesting.near(great.size(), HeavenSpells.greatness(Realm.DAO_INTEGRATION)), "...its living shape drawn as great (" + great.size() + ")");
			great.discard();
		}
		helper.succeed();
	}

	/**
	 * A living shape fades out as its spell lets it go, not blinking out bright (2026-10-24, seen checking them in game: every great spell
	 * took its shape away at its end still glowing): let go, it lasts out its fade and goes; one whose time is already out goes at once.
	 */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 60)
	public void aLivingShapeFadesAsItsSpellLetsItGo(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
		SpellConstruct bright = SpellConstruct.create(level, SpellConstruct.Kind.BLACK_TORTOISE, at, 0, 0, 400, null).spawn();
		SpellConstruct spent = SpellConstruct.create(level, SpellConstruct.Kind.QILIN, at, 0, 0, 5, null).spawn();
		helper.runAfterDelay(6, () -> {
			bright.dissolve();
			spent.dissolve();
			helper.assertTrue(bright.life() == bright.tickCount + SpellConstruct.FADE, "Let go still glowing, it has its fade yet to come (" + bright.life() + " at " + bright.tickCount + ")");
		});
		helper.runAfterDelay(6 + SpellConstruct.FADE / 2, () -> {
			helper.assertTrue(bright.isAlive(), "...and fading, it is still there");
			helper.assertTrue(spent.isRemoved(), "One whose time is already out goes at once");
		});
		helper.runAfterDelay(6 + SpellConstruct.FADE + 6, () -> {
			helper.assertTrue(bright.isRemoved(), "...and once faded, it is gone");
			helper.succeed();
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dao_dragon", timeoutTicks = 200)
	public void theAzureDragonRisesCatchingWhatStandsThere(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 24, 10);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.WOOD, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 14);
		Husk husk = husk(level, point.add(2, 0, 0), spawned), light = mobileHusk(level, point.add(-3, 0, 2), spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(husk));
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.AZURE_DRAGON), "The Azure Dragon is cast, paid for");
		helper.assertTrue(constructs(level, point, 60, SpellConstruct.Kind.AZURE_DRAGON).size() == 1, "...and its dragon rises");
		helper.assertTrue(husk.getHealth() <= 4000 - WoodSpells.DRAGON_DAMAGE + 1, "What stands where it rises is struck (" + husk.getHealth() + ")");
		helper.runAfterDelay(30, () -> {
			try {
				helper.assertTrue(light.getY() > home.y + 8, "...caught in its coils and carried up (" + (light.getY() - home.y) + " up)");
				helper.assertTrue(Math.abs(husk.getY() - home.y) < 2, "...a great beast only struck, not carried (" + (husk.getY() - home.y) + ")");
				helper.assertTrue(floorLeft(level, floor.centre(), point, WoodSpells.DRAGON_REACH * 0.4) < floorLeft(level, floor.centre(), home.add(0, 0, -14), WoodSpells.DRAGON_REACH * 0.4)
						|| floorLeft(level, floor.centre(), point, 20) < 1200, "...the land it burst from torn open");
				helper.succeed();
			} finally {
				finish(helper, p, spawned, floor);
			}
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dao_bird", timeoutTicks = 260)
	public void theVermilionBirdSweepsBurningAlongTheLook(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 44, 12);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.FIRE, floor.centre());
		Vec3 home = p.position();
		Husk near = husk(level, home.add(3, 0, 12), spawned), far = husk(level, home.add(-4, 0, 36), spawned), aside = husk(level, home.add(20, 0, 24), spawned);
		HeavenSpellTesting.lookAt(p, home.add(0, 1.6, 30));
		int stone = floorLeft(level, floor.centre(), home.add(0, 0, 24), 6);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.VERMILION_BIRD_DESCENT), "Vermilion Bird's Descent is cast, paid for");
		helper.assertTrue(constructs(level, home, 80, SpellConstruct.Kind.VERMILION_BIRD).size() == 1, "...and the bird stoops out of the sky");
		helper.runAfterDelay(FireSpells.BIRD_STOOP + FireSpells.BIRD_SWEEP_TICKS / 2 + 10, () -> {
			try {
				helper.assertTrue(near.getHealth() <= 4000 - FireSpells.BIRD_DAMAGE + 1 && far.getHealth() <= 4000 - FireSpells.BIRD_DAMAGE + 1,
						"Everything along its path is struck (" + near.getHealth() + ", " + far.getHealth() + ")");
				helper.assertTrue(near.getRemainingFireTicks() > 0 || far.getRemainingFireTicks() > 0, "...and burns");
				helper.assertTrue(aside.getHealth() == aside.getMaxHealth(), "...not what stands well off it (" + aside.getHealth() + ")");
				helper.assertTrue(floorLeft(level, floor.centre(), home.add(0, 0, 24), 6) < stone, "...and the land under it is burnt into a trench");
				helper.succeed();
			} finally {
				finish(helper, p, spawned, floor);
			}
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dao_flame", timeoutTicks = 200)
	public void oneFlameBlooms(GameTestHelper helper) {
		// Wide enough for the land it sets ablaze (FireSpells#FLAME_SCORCH): past the floor it would burn the test world's own ground.
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 56, 12);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.FIRE, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 18);
		Husk inside = husk(level, point.add(3, 0, 0), spawned), outside = husk(level, point.add(14, 0, 0), spawned);
		// Beside the pillar, in the land it sets ablaze: kept from those fires, so only the pillar's own harm is asked after.
		outside.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0));
		HeavenSpellTesting.lookAt(p, point);
		int stone = floorLeft(level, floor.centre(), point, 6);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.ONE_FLAME_BURNS_THE_HEAVENS), "One Flame Burns the Heavens is cast, paid for");
		helper.assertTrue(constructs(level, home, 10, SpellConstruct.Kind.ONE_FLAME_SEED).size() == 1, "...its seed leaves the caster's hands");
		// Once the seed has faded into it (SpellConstruct#dissolve).
		helper.runAfterDelay(FireSpells.FLAME_FLIGHT + FireSpells.FLAME_BLOOM + SpellConstruct.FADE + 4, () -> {
			try {
				helper.assertTrue(constructs(level, point, 20, SpellConstruct.Kind.ONE_FLAME_PILLAR).size() == 1 && constructs(level, point, 40, SpellConstruct.Kind.ONE_FLAME_SEED).isEmpty(),
						"It blooms into the pillar of fire");
				helper.assertTrue(inside.getHealth() <= 4000 - FireSpells.FLAME_DAMAGE + 1, "What stands in it burns (" + inside.getHealth() + ")");
				helper.assertTrue(outside.getHealth() == outside.getMaxHealth(), "...not what stands beside it (" + outside.getHealth() + ")");
				helper.assertTrue(floorLeft(level, floor.centre(), point, 6) < stone, "...and the ground under it melts away into its pit");
			} catch (RuntimeException e) {
				finish(helper, p, spawned, floor);
				throw e;
			}
			// Its pit, shaft and the land it set ablaze cut and put out before the floor goes (HeavenSpellTesting#succeedOnceHealed).
			HeavenSpells.forget(p.getUUID());
			for (SpellConstruct c : level.getEntitiesOfClass(SpellConstruct.class, new AABB(floor.centre()).inflate(200))) c.discard();
			HeavenSpellTesting.succeedOnceHealed(helper, p, spawned, floor);
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dao_qilin", timeoutTicks = 200)
	public void theQilinTramplesAndThrows(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 44, 12);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.EARTH, floor.centre());
		Vec3 home = p.position();
		Husk ahead = husk(level, home.add(2, 0, 28), spawned), aside = husk(level, home.add(14, 0, 20), spawned);
		HeavenSpellTesting.lookAt(p, home.add(0, 1.6, 30));
		int stone = floorLeft(level, floor.centre(), home.add(0, 0, 21), 5);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.QILIN_TRAMPLES_THE_LAND), "Qilin Tramples the Land is cast, paid for");
		helper.assertTrue(constructs(level, home, 12, SpellConstruct.Kind.QILIN).size() == 1, "...and the qilin rears beside its caster");
		helper.runAfterDelay(EarthSpells.QILIN_REAR + EarthSpells.QILIN_GALLOP / 2, () -> {
			try {
				helper.assertTrue(ahead.getHealth() <= 4000 - EarthSpells.QILIN_DAMAGE + 1, "What stands in its way is trampled (" + ahead.getHealth() + ")");
				helper.assertTrue(aside.getHealth() == aside.getMaxHealth(), "...not what stands off it");
				helper.assertTrue(floorLeft(level, floor.centre(), home.add(0, 0, 21), 5) < stone, "...and its strides crack the land");
				helper.succeed();
			} finally {
				finish(helper, p, spawned, floor);
			}
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dao_seal", timeoutTicks = 200)
	public void theSealCrushesAndHolds(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 34, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.EARTH, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 20);
		Husk under = husk(level, point.add(5, 0, 0), spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(under));
		int stone = floorLeft(level, floor.centre(), point, 18);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.SEAL_OF_TEN_THOUSAND_MOUNTAINS), "The Seal of Ten Thousand Mountains is cast, paid for");
		helper.runAfterDelay(EarthSpells.MOUNTAINS_FALL + 8, () -> {
			try {
				List<SpellConstruct> seal = constructs(level, point, 30, SpellConstruct.Kind.SEAL_MASS);
				helper.assertTrue(seal.size() == 1 && Math.abs(seal.get(0).getY() - home.y) < 1.5, "Its seal has come down on the land");
				helper.assertTrue(under.getHealth() <= 4000 - EarthSpells.MOUNTAINS_DAMAGE + 1, "What it came down on is crushed (" + under.getHealth() + ")");
				helper.assertTrue(under.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && under.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() >= 5, "...and held pressed down");
				helper.assertTrue(floorLeft(level, floor.centre(), point, 18) < stone, "...the land sunk under its weight");
				helper.succeed();
			} finally {
				finish(helper, p, spawned, floor);
			}
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dao_tortoise", timeoutTicks = 200)
	public void theBlackTortoiseWardsItsOwnAndItsSerpentStrikes(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 30, 10);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.WATER, floor.centre());
		Vec3 home = p.position();
		Husk foe = husk(level, home.add(0, 0, 11), spawned), far = husk(level, home.add(0, 0, -(WaterSpells.TORTOISE_REACH + 5)), spawned);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.BLACK_TORTOISE_BASTION), "Black Tortoise Bastion is cast, paid for");
		// Its serpent holds off while the tortoise rises (struck as it rose, it was cut short in its rising: seen in game, 2026-10-24).
		helper.runAfterDelay(WaterSpells.TORTOISE_RISEN - 2, () -> {
			if (foe.getHealth() < foe.getMaxHealth()) {
				finish(helper, p, spawned, floor);
				helper.fail("Its serpent struck before the tortoise had risen (" + foe.getHealth() + ")");
			}
		});
		helper.runAfterDelay(WaterSpells.TORTOISE_RISEN + WaterSpells.TORTOISE_LUNGE + 6, () -> {
			try {
				List<SpellConstruct> tortoise = constructs(level, home, 4, SpellConstruct.Kind.BLACK_TORTOISE);
				helper.assertTrue(tortoise.size() == 1, "The tortoise rises round its caster");
				helper.assertTrue(SpellAuras.has(p, SpellAura.BLACK_TORTOISE), "...and its shell wards them");
				DamageSource fire = ModDamageTypes.qi(level, QiElement.FIRE, Realm.DAO_INTEGRATION, null, null), metal = ModDamageTypes.qi(level, QiElement.METAL, Realm.DAO_INTEGRATION, null, null);
				helper.assertTrue(HeavenSpells.incoming(p, fire, 10) == 0, "Fire of its own realm stops at the shell");
				helper.assertTrue(HeavenSpellTesting.near(HeavenSpells.incoming(p, metal, 10), 6), "...anything else's harm two fifths less (" + HeavenSpells.incoming(p, metal, 10) + ")");
				helper.assertTrue(foe.getHealth() <= 4000 - WaterSpells.TORTOISE_DAMAGE + 1, "Its serpent strikes the foe within its reach (" + foe.getHealth() + ")");
				helper.assertTrue(far.getHealth() == far.getMaxHealth(), "...not one beyond it");
				helper.succeed();
			} finally {
				finish(helper, p, spawned, floor);
			}
		});
	}

	/**
	 * Its basin's lake is set in the cut itself: through the test's one-block floor it would fall on the tests below. So the floor is
	 * thickened under the whirlpool, deeper than the basin, and taken up again (water and all) only once the cut is done.
	 */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dao_rivers", timeoutTicks = 400)
	public void hundredRiversDragsInAndBursts(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 48, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.WATER, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 14);
		int deep = (int) Math.ceil(WaterSpells.RIVERS_BASIN_DEPTH) + 3, wide = (int) Math.ceil(WaterSpells.RIVERS_BASIN) + 3;
		BlockPos heart = BlockPos.containing(point.x, floor.centre().getY() - 1, point.z);
		for (int dx = -wide; dx <= wide; dx++) {
			for (int dz = -wide; dz <= wide; dz++) for (int dy = 1; dy <= deep; dy++) level.setBlock(heart.offset(dx, -dy, dz), Blocks.STONE.defaultBlockState(), 2);
		}
		Husk drawn = mobileHusk(level, point.add(12, 0, 0), spawned), struck = husk(level, point.add(-4, 0, 2), spawned);
		HeavenSpellTesting.lookAt(p, point);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.HUNDRED_RIVERS_CONVERGE), "Hundred Rivers Converge is cast, paid for");
		boolean[] cleared = {false};
		Runnable clear = () -> {
			if (cleared[0]) return;
			cleared[0] = true;
			for (int dx = -wide; dx <= wide; dx++) {
				for (int dz = -wide; dz <= wide; dz++) for (int dy = 0; dy <= deep; dy++) level.setBlock(heart.offset(dx, -dy, dz), Blocks.AIR.defaultBlockState(), 2);
			}
			finish(helper, p, spawned, floor);
		};
		helper.runAfterDelay(40, () -> {
			double off = Math.hypot(drawn.getX() - point.x, drawn.getZ() - point.z);
			if (off >= 7) {
				clear.run();
				helper.fail("A foe within its reach is dragged into the whirlpool (" + off + " off its heart)");
				return;
			}
			helper.runAfterDelay(WaterSpells.RIVERS_TICKS - 40 + 6, () -> {
				boolean hurt = struck.getHealth() <= 4000 - WaterSpells.RIVERS_DAMAGE * 4 + 1, bursting = constructs(level, point, 40, SpellConstruct.Kind.HUNDRED_RIVERS).size() == 1;
				float health = struck.getHealth();
				// Once its basin is cut (and its lake set), the floor and the water are taken up.
				helper.succeedWhen(() -> {
					helper.assertTrue(Devastation.pending() == 0, "...its basin still being cut");
					clear.run();
					helper.assertTrue(hurt, "...and struck while it is held (" + health + ")");
					helper.assertTrue(bursting, "The whirlpool bursts (its shape still there, bursting)");
				});
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dao_tiger", timeoutTicks = 200)
	public void theWhiteTigerPouncesAndRakes(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 44, 12);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.METAL, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 14);
		Husk prey = husk(level, point, spawned), raked = husk(level, point.add(MetalSpells.TIGER_SPREAD * 0.5, 0, 22), spawned), aside = husk(level, point.add(-16, 0, 10), spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(prey));
		int stone = floorLeft(level, floor.centre(), point.add(0, 0, 14), 6);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.WHITE_TIGER_POUNCE), "White Tiger's Pounce is cast, paid for");
		helper.assertTrue(constructs(level, home, 8, SpellConstruct.Kind.WHITE_TIGER).size() == 1, "...the tiger crouches by its caster");
		int done = MetalSpells.TIGER_CROUCH + MetalSpells.TIGER_LEAP + MetalSpells.TIGER_CLAW + 4 + MetalSpells.TIGER_RAKE + 4;
		helper.runAfterDelay(done, () -> {
			try {
				helper.assertTrue(prey.getHealth() <= 4000 - MetalSpells.TIGER_DAMAGE + 1, "It lands on its prey (" + prey.getHealth() + ")");
				helper.assertTrue(raked.getHealth() <= 4000 - MetalSpells.TIGER_RAKE_DAMAGE + 1, "...its rake strikes what stands in its gouges (" + raked.getHealth() + ")");
				helper.assertTrue(aside.getHealth() == aside.getMaxHealth(), "...not what stands off them");
				helper.assertTrue(floorLeft(level, floor.centre(), point.add(0, 0, 14), 6) < stone, "...and its gouges are cut through the land");
				helper.succeed();
			} finally {
				finish(helper, p, spawned, floor);
			}
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dao_swords", timeoutTicks = 200)
	public void myriadSwordsFuseAndFall(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 44, 14);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = caster(helper, QiElement.METAL, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 16);
		Husk under = husk(level, point.add(4, 0, 0), spawned);
		HeavenSpellTesting.lookAt(p, HeavenSpells.middle(under));
		int stone = floorLeft(level, floor.centre(), point.add(30, 0, 0), 3) + floorLeft(level, floor.centre(), point, 3);
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.MYRIAD_SWORDS_RETURN), "Myriad Swords Return to One is cast, paid for");
		helper.assertTrue(constructs(level, point, 50, SpellConstruct.Kind.MYRIAD_SWORDS).size() == 1, "...its swords gather over the spot");
		// Once the swords have faded into it and it has struck (SpellConstruct#dissolve).
		helper.runAfterDelay(MetalSpells.SWORDS_GATHER + MetalSpells.SWORDS_MERGE + Math.max(MetalSpells.SWORDS_FALL, SpellConstruct.FADE) + 4, () -> {
			try {
				helper.assertTrue(constructs(level, point, 50, SpellConstruct.Kind.COLOSSAL_SWORD).size() == 1 && constructs(level, point, 50, SpellConstruct.Kind.MYRIAD_SWORDS).isEmpty(),
						"They fuse into one colossal sword");
				helper.assertTrue(under.getHealth() <= 4000 - MetalSpells.SWORDS_DAMAGE + 1, "It strikes what it falls among (" + under.getHealth() + ")");
				helper.assertTrue(floorLeft(level, floor.centre(), point.add(30, 0, 0), 3) + floorLeft(level, floor.centre(), point, 3) < stone,
						"...its pit and its cross of slots cut in the land");
				helper.succeed();
			} finally {
				finish(helper, p, spawned, floor);
			}
		});
	}

	/** A cultivator's great spells strike as a player's do but leave the land whole (HeavenSpells#carvesLand). */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dao_cultivator", timeoutTicks = 200)
	public void aCultivatorsGreatSpellsLeaveTheLand(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 34, 12);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer watcher = caster(helper, QiElement.METAL, floor.centre().offset(-30, 0, -30));
		BlockPos at = floor.centre();
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, helper.relativePos(at));
		npc.setNoAi(true);
		npc.setRank(PlayerCultivation.rank(Realm.DAO_INTEGRATION, Stage.EARLY));
		npc.setHealth(npc.getMaxHealth());
		npc.cultivation().setQi(npc.cultivation().maxQi());
		spawned.add(npc);
		Vec3 home = npc.position();
		Husk foe = husk(level, home.add(0, 0, 14), spawned);
		npc.setTarget(foe);
		CultivatorSpells.face(npc, foe);
		int stone = floorLeft(level, floor.centre(), home, 30);
		boolean quiet = Devastation.pending() == 0;
		helper.assertTrue(Spells.castBy(npc, Spell.WHITE_TIGER_POUNCE), "A cultivator casts White Tiger's Pounce");
		helper.runAfterDelay(MetalSpells.TIGER_CROUCH + MetalSpells.TIGER_LEAP + MetalSpells.TIGER_CLAW + 4 + MetalSpells.TIGER_RAKE + 4, () -> {
			try {
				helper.assertTrue(foe.getHealth() <= 4000 - MetalSpells.TIGER_DAMAGE + 1, "Its tiger lands on its foe (" + foe.getHealth() + ")");
				helper.assertTrue(Spells.castBy(npc, Spell.SEAL_OF_TEN_THOUSAND_MOUNTAINS) || npc.cultivation().getQi() < Spell.SEAL_OF_TEN_THOUSAND_MOUNTAINS.qiCost(),
						"...and the Seal of Ten Thousand Mountains after it");
				helper.runAfterDelay(EarthSpells.MOUNTAINS_FALL + 8, () -> {
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

	// --- The ghost foundation's (GhostSpells) and both paths' (HeavenDemonSpells) ---

	/** A living caster whose ghost path stands at Dao Integration Grand Perfection, the Ten Thousand Ghosts Union understood to its last layer. */
	private static ServerPlayer ghostCaster(GameTestHelper helper, BlockPos at) {
		ServerPlayer p = caster(helper, QiElement.WATER, at);
		NetherworldState s = CultivationManager.get(p).netherworld();
		for (GhostMethod method : GhostMethod.values()) {
			if (method.isFoundation()) s.setMethod(method, method.realm().ordinal() <= Realm.DAO_INTEGRATION.ordinal() ? 1 : 0, 0);
		}
		GhostMethods.setPath(s, PlayerCultivation.rank(Realm.DAO_INTEGRATION, Stage.GRAND_PERFECTION));
		CultivationManager.refresh(p);
		s.setGhostQi(GhostQi.poolMax(CultivationManager.get(p)));
		return p;
	}

	/** A tiger demon of Dao Integration Grand Perfection on the heaven path and the Demon Path both: every spell of both paths is its. */
	private static ServerPlayer demonCaster(GameTestHelper helper, BlockPos at) {
		ServerPlayer p = caster(helper, QiElement.METAL, at);
		PlayerCultivation c = CultivationManager.get(p);
		c.demon().setBloodline(Bloodline.TIGER);
		c.demon().set(PlayerCultivation.rank(Realm.DAO_INTEGRATION, Stage.GRAND_PERFECTION), 0);
		CultivationManager.refresh(p);
		c.setQi(c.maxQi());
		p.setHealth(p.getMaxHealth());
		return p;
	}

	/** Casts {@code spell} afresh: true if it took hold (its cooldown began). */
	private static boolean took(ServerPlayer p, Spell spell) {
		Spells.setCooldown(p, spell, 0);
		Spells.cast(p, spell);
		return Spells.cooldownLeft(p, spell) > 0;
	}

	/**
	 * Whether the floor round {@code at} is as it was ({@code stone} of its stone within {@code radius}), asked only where no wound was being
	 * cut as the test began ({@code quiet}: Devastation#pending): a test before it whose carving is still under way cuts floors laid after it
	 * (the gate's floor, 2026-10-24, cut by the batch before it).
	 */
	private static boolean landKept(ServerLevel level, HeavenSpellTesting.Floor floor, Vec3 at, double radius, int stone, boolean quiet) {
		return !quiet || floorLeft(level, floor.centre(), at, radius) == stone;
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dao_procession", timeoutTicks = 200)
	public void theProcessionPoursOutAlongTheLook(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 44, 12);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = ghostCaster(helper, floor.centre());
		Vec3 home = p.position();
		Husk near = husk(level, home.add(2, 0, 12), spawned), far = husk(level, home.add(-2, 0, 36), spawned), aside = husk(level, home.add(14, 0, 24), spawned);
		HeavenSpellTesting.lookAt(p, home.add(0, 1.0, 30));
		int stone = floorLeft(level, floor.centre(), home.add(0, 0, 24), 8);
		boolean quiet = Devastation.pending() == 0;
		helper.assertTrue(Spell.PROCESSION_OF_TEN_THOUSAND_GHOSTS.isKnown(CultivationManager.get(p)), "Its ghost path carries the Ten Thousand Ghosts Union to its spell");
		helper.assertTrue(took(p, Spell.PROCESSION_OF_TEN_THOUSAND_GHOSTS), "The Procession of Ten Thousand Ghosts is cast");
		helper.assertTrue(constructs(level, home, 10, SpellConstruct.Kind.GHOST_PROCESSION).size() == 1, "...its column pours out of its caster");
		helper.runAfterDelay(GhostSpells.PROCESSION_TICKS + 4, () -> {
			try {
				helper.assertTrue(near.getHealth() <= 4000 - GhostSpells.PROCESSION_DAMAGE + 1 && far.getHealth() <= 4000 - GhostSpells.PROCESSION_DAMAGE + 1,
						"Everything it passes through is struck (" + near.getHealth() + ", " + far.getHealth() + ")");
				helper.assertTrue(near.getHealth() > 4000 - GhostSpells.PROCESSION_DAMAGE * 2, "...once");
				helper.assertTrue(near.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && near.hasEffect(MobEffects.WEAKNESS), "...slowed and weakened");
				helper.assertTrue(aside.getHealth() == aside.getMaxHealth(), "...not what stands off it (" + aside.getHealth() + ")");
				helper.assertTrue(landKept(level, floor, home.add(0, 0, 24), 8, stone, quiet), "...and the land is left as it was");
				helper.succeed();
			} finally {
				finish(helper, p, spawned, floor);
			}
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dao_gate", timeoutTicks = 360)
	public void theGateDrawsInAndSlamsShut(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 40, 16);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = ghostCaster(helper, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 20);
		Husk held = husk(level, point.add(4, 0, 0), spawned), drawn = mobileHusk(level, point.add(14, 0, 0), spawned),
				beyond = husk(level, point.add(-(GhostSpells.GATE_RADIUS + 6), 0, 0), spawned);
		HeavenSpellTesting.lookAt(p, point);
		int stone = floorLeft(level, floor.centre(), point, 12);
		boolean quiet = Devastation.pending() == 0;
		p.setHealth(p.getMaxHealth() / 2);
		float hurt = p.getHealth();
		helper.assertTrue(took(p, Spell.GATE_OF_TEN_THOUSAND_GHOSTS), "The Gate of Ten Thousand Ghosts is cast");
		helper.assertTrue(constructs(level, point, 8, SpellConstruct.Kind.GHOST_GATE).size() == 1, "...and its gate rises where its caster looks");
		helper.runAfterDelay(GhostSpells.GATE_OPENS + 60, () -> {
			double off = Math.hypot(drawn.getX() - point.x, drawn.getZ() - point.z);
			String wrong = off >= 10 ? "A foe near it is dragged toward the gate (" + off + " off it)"
					: held.getHealth() > 4000 - GhostSpells.GATE_DRAIN * 2 + 1 ? "...and drained each second (" + held.getHealth() + ")" : null;
			if (wrong != null) {
				finish(helper, p, spawned, floor);
				helper.fail(wrong);
				return;
			}
			helper.runAfterDelay(GhostSpells.GATE_TICKS - 60 + GhostSpells.GATE_SHUT + 4, () -> {
				try {
					helper.assertTrue(held.getHealth() <= 4000 - GhostSpells.GATE_DRAIN * 8 - GhostSpells.GATE_SLAM + 1, "Eight seconds drained, then slammed shut on (" + held.getHealth() + ")");
					helper.assertTrue(beyond.getHealth() == beyond.getMaxHealth(), "...not what stands beyond its reach");
					helper.assertTrue(p.getHealth() > hurt, "A quarter of its harm mends its caster (" + hurt + " -> " + p.getHealth() + ")");
					helper.assertTrue(landKept(level, floor, point, 12, stone, quiet), "...and the land is left as it was");
					helper.succeed();
				} finally {
					finish(helper, p, spawned, floor);
				}
			});
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, batch = "dao_avatar", timeoutTicks = 300)
	public void theAvatarsHandsComeDownWhereItsCasterLooks(GameTestHelper helper) {
		// Wide enough for its palm's cracks: past the floor they would cut the test world's own ground.
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 68, 16);
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = HeavenSpellTesting.spawned();
		ServerPlayer p = demonCaster(helper, floor.centre());
		Vec3 home = p.position(), point = home.add(0, 0, 24);
		Husk struck = husk(level, point.add(3, 0, 0), spawned), aside = husk(level, point.add(-(HeavenDemonSpells.AVATAR_RADIUS + 6), 0, 0), spawned);
		// Ground under where its hands come down: its palm's crater would cut the one-block floor and drop the struck out of the claw's reach.
		Runnable thinned = HeavenSpellTesting.thicken(floor, point.x, point.z, 24, 12);
		HeavenSpellTesting.lookAt(p, point);
		int stone = floorLeft(level, floor.centre(), point, 12);
		boolean quiet = Devastation.pending() == 0;
		helper.assertTrue(HeavenSpellTesting.cast(p, Spell.HEAVEN_DEMON_AVATAR), "The Heaven-Demon Avatar is cast, paid for in qi");
		helper.assertTrue(constructs(level, home, 20, SpellConstruct.Kind.AVATAR_PROJECTION).size() == 1, "...and the avatar rises behind its caster");
		int first = HeavenDemonSpells.AVATAR_STRIKES[0] + HeavenDemonSpells.AVATAR_FALL, second = HeavenDemonSpells.AVATAR_STRIKES[1] + HeavenDemonSpells.AVATAR_FALL;
		helper.runAfterDelay(first + 2, () -> {
			if (struck.getHealth() > 4000 - HeavenDemonSpells.AVATAR_DAMAGE + 1 || constructs(level, point, 20, SpellConstruct.Kind.AVATAR_HAND).size() != 1) {
				thinned.run();
				finish(helper, p, spawned, floor);
				helper.fail("Heaven's palm comes down where its caster looks (" + struck.getHealth() + ")");
				return;
			}
			// Its crater took the ground its caster's look met: looking at the struck now (thrown down into it), as it rises for the claw.
			helper.runAfterDelay(HeavenDemonSpells.AVATAR_STRIKES[1] - first - 4, () -> HeavenSpellTesting.lookAt(p, HeavenSpells.middle(struck)));
			helper.runAfterDelay(second - first, () -> {
				try {
					helper.assertTrue(struck.getHealth() <= 4000 - 2 * HeavenDemonSpells.AVATAR_DAMAGE + 1 && constructs(level, point, 20, SpellConstruct.Kind.AVATAR_HAND).size() == 1,
							"...then the demon's claw (" + struck.getHealth() + ")");
					helper.assertTrue(struck.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "...the struck slowed");
					helper.assertTrue(aside.getHealth() == aside.getMaxHealth(), "...not what stands beyond their reach");
					// Its hands wound the land (since 2026-10-24: it was left as it was).
					helper.assertTrue(floorLeft(level, floor.centre(), point, 12) < stone, "...and the land is cratered and gouged where they came down");
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
}
