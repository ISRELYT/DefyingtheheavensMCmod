package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.function.IntToDoubleFunction;

/**
 * What years are spent on besides (the user's asks, 2026-10-21; Lifespan): flying through space by the block, the same for everyone; a
 * meditation's pace, priced steeply and slowed near the end; Burn Lifespan, a share of the span a second; Life Force's years, given a
 * herb or drawn out of another; and a mortal's old age, never a cultivator's.
 */
public class LifespanCostsGameTests implements FabricGameTest {
	/**
	 * A survival player in earnest (a mock player answers that it is creative, and creative players pay no years), of {@code realm}
	 * Early (null: a mortal), at the test's corner. Remove it with {@link #leave}.
	 */
	private static ServerPlayer survivor(GameTestHelper helper, Realm realm) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "test-survivor")) {
			@Override
			public boolean isSpectator() { return false; }

			@Override
			public boolean isCreative() { return false; }
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.teleportTo(helper.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		if (realm != null) {
			CultivationManager.get(p).setState(realm, Stage.EARLY, 0);
			CultivationManager.refresh(p);
		}
		p.setHealth(p.getMaxHealth());
		return p;
	}

	private static void leave(ServerPlayer... players) {
		for (ServerPlayer p : players) {
			BurnLifespan.onDisconnect(p);
			Lifespan.forget(p.getUUID());
			LifespanTribulation.forget(p.getUUID());
			AstralRealm.forget(p.getUUID());
			if (p.server.getPlayerList().getPlayer(p.getUUID()) == p) p.server.getPlayerList().remove(p);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void spaceCostsEveryoneTheSameYears(GameTestHelper helper) {
		ServerPlayer nascent = survivor(helper, Realm.NASCENT_SOUL), axis = survivor(helper, Realm.FOUR_AXIS);
		try {
			for (ServerPlayer p : new ServerPlayer[] {nascent, axis}) {
				PlayerCultivation c = CultivationManager.own(p);
				c.setAge(100);
				double perBlock = AstralRealm.yearsPerBlock(c);
				for (int i = 0; i < 40; i++) AstralRealm.flown(p.getUUID(), 25, perBlock); // 1,000 blocks flown, a tick at a time
				AstralRealm.flown(p.getUUID(), 500, perBlock); // a teleport's jump
				AstralRealm.pay(p);
				helper.assertTrue(Math.abs(c.age() - 110) < 1e-6, c.getRealm().getId() + ": 1,000 blocks flown cost 10 years, a jump none (" + c.age() + ")");
			}
			PlayerCultivation starborn = new PlayerCultivation();
			starborn.setPhysique(Physique.STARBORN_BODY);
			helper.assertTrue(Math.abs(AstralRealm.yearsPerBlock(starborn) - AstralRealm.YEARS_PER_BLOCK / AstralRealm.STARBORN_SPEED) < 1e-12,
					"A Starborn Body pays the less by its speed there: a year every 150 blocks");
			AstralPlanets planets = AstralPlanets.get();
			if (planets != null) {
				AstralPlanets.Planet home = planets.overworld();
				Vec3 out = home.pos().add(0, home.atmosphere() + 1600, 0);
				helper.assertTrue(Math.abs(AstralRealm.homeYears(planets, out, AstralRealm.YEARS_PER_BLOCK) - 16) < 1e-6,
						"1,600 blocks out from the Overworld's air: 16 years home");
			}
		} finally {
			leave(nascent, axis);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void meditationPacesArePricedSteeply(GameTestHelper helper) {
		helper.assertTrue(Lifespan.pacePrice(1) == 1 && Lifespan.pacePrice(2) == 2 && Lifespan.pacePrice(3) == 4 && Lifespan.pacePrice(9) == 4,
				"x1 its own pace; at x2 each bit costs twice the years, at x3 four times");
		ServerPlayer p = survivor(helper, Realm.CORE_FORMATION);
		PlayerCultivation c = CultivationManager.own(p);
		try {
			helper.assertTrue(c.meditationSpeed() == 1, "Its own pace to begin with");
			Lifespan.cycleMeditationSpeed(p);
			helper.assertTrue(c.meditationSpeed() == 2, "The button: x2");
			Lifespan.cycleMeditationSpeed(p);
			Lifespan.cycleMeditationSpeed(p);
			helper.assertTrue(c.meditationSpeed() == 1, "...x3, and round to x1");
			c.setMeditationSpeed(3);
			helper.assertTrue(PlayerCultivation.load(c.save()).meditationSpeed() == 3 && !new PlayerCultivation().save().contains("MeditationSpeed"), "Saved");
			c.setAge(Lifespan.span(c) - 20);
			Lifespan.check(p);
			helper.assertTrue(c.meditationSpeed() == 3, "Twenty years left: still x3");
			c.setAge(Lifespan.span(c) - 9);
			Lifespan.check(p);
			helper.assertTrue(c.meditationSpeed() == 1, "Ten years left: slowed to its own pace by itself");
		} finally {
			leave(p);
		}
		helper.succeed();
	}

	/**
	 * The talent race (the user's vision, 2026-10-21): a realm's cultivation costs its share of the years it gives, over its stages by
	 * their time, divided by talent (the grade) and the meditation's boost, the pace's price on top; only what is won is paid for.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void meditationCostsYearsByTalentAndBoost(GameTestHelper helper) {
		helper.assertTrue(Math.abs(Lifespan.realmSeclusion(Realm.QI_REFINING) - 50.4) < 1e-9 && Math.abs(Lifespan.realmSeclusion(Realm.CORE_FORMATION) - 640) < 1e-9
				&& Math.abs(Lifespan.realmSeclusion(Realm.FOUR_AXIS) - 48000) < 1e-9, "Qi Refining 50.4 years, Core Formation 640, Four Axis 48,000");
		double all = 0;
		for (Stage stage : Stage.values()) all += Lifespan.stageSeclusion(Realm.CORE_FORMATION, stage);
		helper.assertTrue(Math.abs(all - 640) < 1e-6 && Lifespan.stageSeclusion(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION)
				> Lifespan.stageSeclusion(Realm.CORE_FORMATION, Stage.EARLY), "Shared over its stages, the longer the dearer");
		PlayerCultivation low = new PlayerCultivation(), immortal = new PlayerCultivation();
		low.setPhysique(Physique.FIVE_ELEMENT_ROOTS);
		immortal.setPhysique(Physique.FALLEN_TRUE_IMMORTAL);
		double early = Lifespan.stageSeclusion(Realm.CORE_FORMATION, Stage.EARLY);
		helper.assertTrue(Math.abs(Lifespan.seclusionYears(low, Realm.CORE_FORMATION, Stage.EARLY, 1, 1) - early) < 1e-9
				&& Math.abs(Lifespan.seclusionYears(immortal, Realm.CORE_FORMATION, Stage.EARLY, 1, 1) - early / 16) < 1e-9
				&& Math.abs(Lifespan.seclusionYears(low, Realm.CORE_FORMATION, Stage.EARLY, 0.5, 2) - early / 4) < 1e-9,
				"A Low grade pays it whole, an Immortal one a 16th; half the stage with x2 saving, a quarter");
		// What saves years (the user's call, 2026-10-22): boost, method, dense qi, a stone, each only ever helping.
		helper.assertTrue(Math.abs(Lifespan.saving(2, 1.5, 3, 1.5) - 13.5) < 1e-9 && Lifespan.saving(1, 0.75, 0.25, 1) == 1,
				"Boost x2, a method x1.5, qi x3, a stone x1.5: x13.5; no method and thin qi cost nothing more");
		PlayerCultivation upper = new PlayerCultivation();
		upper.setState(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION, 0);
		upper.setInUpperRealm(true);
		PlayerCultivation axis = new PlayerCultivation();
		axis.setState(Realm.FOUR_AXIS, Stage.EARLY, 0);
		axis.setInUpperRealm(true);
		helper.assertTrue(Math.abs(Lifespan.placeSaving(upper) - 10) < 1e-9 && Math.abs(Lifespan.placeSaving(axis) - 1) < 1e-9
				&& Math.abs(Lifespan.ghostPlaceSaving(GhostQi.NETHERWORLD * GhostQi.PEAKS_FACTOR) - 1.2) < 1e-9,
				"The Upper Realm's qi saves a Heavenly Being ten times, not a Four Axis (its own place); the Nine Nether Peaks the ghost path x1.2");
		// The small speed edge by grade (the user's pick): a Mid grade's pace x1.1, an Immortal's x1.5.
		PlayerCultivation mid = new PlayerCultivation();
		mid.setPhysique(Physique.INNATE_ALCHEMY_BODY);
		helper.assertTrue(Math.abs(mid.physiqueCultivationFactor() - 1.1) < 1e-9 && Math.abs(immortal.physiqueCultivationFactor() - 7.5) < 1e-9
				&& Math.abs(mid.physiqueGhostFactor() - 1.1) < 1e-9, "Mid grade x1.1 on both paths; a Fallen True Immortal its x5 and x1.5");
		low.setMeditationSpeed(3);
		helper.assertTrue(Math.abs(Lifespan.seclusionYears(low, Realm.CORE_FORMATION, Stage.EARLY, 1, 1) - early * 4) < 1e-9, "At x3, four times");
		ServerPlayer p = survivor(helper, Realm.CORE_FORMATION);
		PlayerCultivation c = CultivationManager.own(p);
		c.setPhysique(Physique.FIVE_ELEMENT_ROOTS);
		try {
			c.setAge(100);
			IntToDoubleFunction required = rank -> PlayerCultivation.required(Realm.byIndex(rank / 4), Stage.byIndex(rank % 4));
			int cfEarly = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY);
			double need = required.applyAsDouble(cfEarly);
			Lifespan.seclude(p, cfEarly, 0, cfEarly, need / 2, required, 1, 1);
			helper.assertTrue(Math.abs(c.age() - (100 + early / 2)) < 1e-6, "Half of Core Formation Early meditated: half its years paid (" + c.age() + ")");
			Lifespan.seclude(p, cfEarly, need / 2, cfEarly + 1, 0, required, 1, 1);
			helper.assertTrue(Math.abs(c.age() - (100 + early)) < 1e-6, "...and the rest of it, up to the next stage");
			Lifespan.seclude(p, cfEarly + 1, 10, cfEarly + 1, 10, required, 1, 1);
			helper.assertTrue(Math.abs(c.age() - (100 + early)) < 1e-6, "Nothing won (a bottleneck waited at): nothing paid");
			Lifespan.seclude(p, cfEarly + 1, 0, cfEarly + 1, required.applyAsDouble(cfEarly + 1), required, 1, 0.5);
			helper.assertTrue(Math.abs(c.age() - (100 + early + Lifespan.stageSeclusion(Realm.CORE_FORMATION, Stage.MID) / 2)) < 1e-6,
					"A share of a gain that was a reward's seconds: unpaid");
		} finally {
			leave(p);
		}
		helper.succeed();
	}

	/** Whether Burn Lifespan's quarter more speed is on {@code p}. */
	private static boolean burnSpeed(ServerPlayer p) {
		return p.getAttribute(Attributes.MOVEMENT_SPEED).getModifiers().stream()
				.anyMatch(m -> "Burn Lifespan".equals(m.getName()) && Math.abs(m.getAmount() - 0.25) < 1e-9);
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void burnLifespanBurnsAShareForStrength(GameTestHelper helper) {
		ServerPlayer nascent = survivor(helper, Realm.NASCENT_SOUL), axis = survivor(helper, Realm.FOUR_AXIS);
		PlayerCultivation c = CultivationManager.own(nascent), a = CultivationManager.own(axis);
		try {
			helper.assertTrue(Ability.BURN_LIFESPAN.isUnlocked(c) && !BurnLifespan.isBurning(nascent), "Awakened, and off to begin with");
			c.setAge(200);
			CultivationManager.toggleAbility(nascent, Ability.BURN_LIFESPAN.ordinal());
			helper.assertTrue(BurnLifespan.isBurning(nascent), "Lit");
			helper.assertTrue(Math.abs(Physiques.outgoing(nascent.damageSources().playerAttack(nascent), 10) - 15) < 1e-4, "Its blows half again as hard");
			helper.assertTrue(burnSpeed(nascent), "A quarter swifter");
			BurnLifespan.second(nascent);
			helper.assertTrue(Math.abs(c.age() - 206.5) < 1e-6, "A second of it: half a percent of its 1,300 years (" + c.age() + ")");
			a.setAge(1000);
			CultivationManager.toggleAbility(axis, Ability.BURN_LIFESPAN.ordinal());
			BurnLifespan.second(axis);
			helper.assertTrue(Math.abs(a.age() - 1062.5) < 1e-6, "Four Axis: the same share of its 12,500 (" + a.age() + ")");
			// Near the end it goes out by itself, and won't light with too few left.
			c.setAge(Lifespan.span(c) - 15);
			BurnLifespan.second(nascent);
			helper.assertTrue(!BurnLifespan.isBurning(nascent) && Math.abs(Lifespan.left(c) - 15) < 1e-6, "Out by itself before it would leave fewer than ten");
			helper.assertTrue(!burnSpeed(nascent), "...its speed gone with it");
			CultivationManager.toggleAbility(nascent, Ability.BURN_LIFESPAN.ordinal());
			helper.assertTrue(!BurnLifespan.isBurning(nascent), "...and it won't light again");
			helper.assertTrue(!Ability.BURN_LIFESPAN.isUnlocked(new PlayerCultivation()), "A mortal has nothing to burn it with");
		} finally {
			leave(nascent, axis);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void lifeForceDrawsYearsOutOfAnother(GameTestHelper helper) {
		ServerPlayer taker = survivor(helper, Realm.FOUNDATION_BUILDING), victim = survivor(helper, Realm.FOUNDATION_BUILDING);
		PlayerCultivation mine = CultivationManager.own(taker), theirs = CultivationManager.own(victim);
		try {
			theirs.setAge(100);
			double drawn = Lifespan.draw(taker, victim, 5);
			helper.assertTrue(drawn == 5 && Math.abs(theirs.age() - 105) < 1e-6 && Math.abs(mine.extraYears() - 2.5) < 1e-6,
					"Five of their years drawn out of them, half of them won (" + drawn + ", " + theirs.age() + ", " + mine.extraYears() + ")");
			theirs.setAge(Lifespan.span(theirs) - 2);
			helper.assertTrue(Math.abs(Lifespan.draw(taker, victim, 5) - 2) < 1e-6 && theirs.age() == Lifespan.span(theirs)
					&& LifespanTribulation.isRunning(victim.getUUID()), "No more than they have left: then the heavens come for them");
		} finally {
			leave(taker, victim);
		}
		helper.succeed();
	}

	/** Life Force Gain on a fruit held: its growth as before, on the caster's own years. */
	@GameTest(template = EMPTY_STRUCTURE, batch = "lifespan_costs_life", timeoutTicks = 80)
	public void lifeForceGainGrowsAHerbOnTheCastersYears(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 4, 4);
		ServerPlayer p = survivor(helper, null);
		PlayerCultivation c = CultivationManager.own(p);
		try {
			c.setState(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION, 0);
			HeavenMethod wood = HeavenMethod.of(QiElement.WOOD, Realm.FOUR_AXIS);
			c.heaven().setMethod(wood, wood.layers(), 0);
			CultivationManager.refresh(p);
			c.setQi(c.maxQi());
			c.setAge(100);
			HeavenSpellTesting.vulnerable(p);
			p.teleportTo(helper.getLevel(), floor.centre().getX() + 0.5, floor.centre().getY(), floor.centre().getZ() + 0.5, 0, -90); // at the sky
			p.setOnGround(true);
			p.setItemInHand(InteractionHand.MAIN_HAND, CultivationFruitItem.create(500)); // no being nor herb before it: it takes the fruit held
			helper.assertTrue(HeavenSpellTesting.cast(p, Spell.LIFE_FORCE_GAIN), "Cast");
		} catch (RuntimeException e) {
			HeavenSpellTesting.cleanUp(p, HeavenSpellTesting.spawned(), floor);
			throw e;
		}
		helper.runAfterDelay(25, () -> {
			try {
				int age = CultivationFruitItem.age(p.getMainHandItem());
				helper.assertTrue(age == 500 + WoodSpells.GAIN_YEARS, "Two half-seconds grew it 120 years (" + age + ")");
				helper.assertTrue(Math.abs(c.age() - (100 + WoodSpells.GAIN_LIFE_YEARS)) < 0.01, "...on 10 of the caster's own years (" + c.age() + ")");
				helper.succeed();
			} finally {
				Lifespan.forget(p.getUUID());
				HeavenSpellTesting.cleanUp(p, HeavenSpellTesting.spawned(), floor);
			}
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void oldAgeWeakensOnlyMortals(GameTestHelper helper) {
		ServerPlayer mortal = survivor(helper, null);
		PlayerCultivation c = CultivationManager.own(mortal);
		double health = mortal.getAttributeValue(Attributes.MAX_HEALTH), speed = mortal.getAttributeValue(Attributes.MOVEMENT_SPEED);
		try {
			helper.assertTrue(c.isMortal() && Lifespan.oldAge(c) == 0, "Sixteen: no old age");
			c.setAge(60);
			helper.assertTrue(Lifespan.oldAge(c) == 0, "Sixty of a hundred: not yet");
			c.setAge(90);
			helper.assertTrue(Math.abs(Lifespan.oldAge(c) - 0.75) < 1e-9, "Ninety: three quarters of the way");
			Lifespan.weaken(mortal);
			helper.assertTrue(mortal.getAttributeValue(Attributes.MAX_HEALTH) < health * (1 - Lifespan.OLD_HEALTH * 0.74)
					&& mortal.getAttributeValue(Attributes.MOVEMENT_SPEED) < speed * (1 - Lifespan.OLD_SPEED * 0.74), "Weaker: less health, slower");
			c.setAge(Lifespan.span(c));
			helper.assertTrue(Lifespan.oldAge(c) == 1, "At the span's end: all of it");
			PlayerCultivation cultivator = new PlayerCultivation();
			cultivator.setState(Realm.QI_REFINING, Stage.EARLY, 0);
			cultivator.setAge(95);
			helper.assertTrue(Lifespan.oldAge(cultivator) == 0, "A cultivator's body never weakens with age, even at 95 of its 100");
			c.setAge(20);
			Lifespan.weaken(mortal);
			helper.assertTrue(Math.abs(mortal.getAttributeValue(Attributes.MAX_HEALTH) - health) < 1e-6
					&& Math.abs(mortal.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed) < 1e-9, "Young again (years won): whole again");
		} finally {
			leave(mortal);
		}
		helper.succeed();
	}
}
