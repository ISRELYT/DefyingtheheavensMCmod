package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * The swift and the mending (the user's asks, 2026-10-22): from Void Shattering on, flight far swifter (QiFlight#realmSpeed) and
 * space's swifter still (AstralRealm#spaceSpeed), room for it on the server (QiFlight#moveAllowanceSq), space unchecked for blocks
 * (AstralRealm#passesThrough) and an atmosphere met though crossed between two ticks (AstralPlanets#atmosphereAlong); natural
 * regeneration at the realm's pace (CultivationStats#mendFactor).
 */
public class SwiftAndMendingGameTests implements FabricGameTest {
	/** A living cultivator in survival in {@code level}, of {@code realm} {@code stage}, wielding it whole (as in the Upper Realm). */
	private static ServerPlayer cultivator(ServerLevel level, Realm realm, Stage stage) {
		ServerPlayer player = FakePlayer.get(level, new GameProfile(UUID.randomUUID(), "swift"));
		player.setGameMode(GameType.SURVIVAL);
		PlayerCultivation c = CultivationManager.get(player);
		c.setState(realm, stage, 0);
		c.setLowerRealmBound(false);
		CultivationManager.refresh(player);
		return player;
	}

	/** Blocks a second sprinting at {@code speed} (vanilla flying speed): QiFlight#stepAt across, twenty ticks. */
	private static double sprint(float speed) {
		return 22.2 * speed * 20;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void fromVoidShatteringOnTheyFlyFarSwifter(GameTestHelper helper) {
		float lastFlight = 0, lastSpace = 0;
		for (Realm realm : Realm.values()) {
			for (Stage stage : Stage.values()) {
				float flight = QiFlight.realmSpeed(realm, stage), space = AstralRealm.spaceSpeed(realm, stage);
				if (realm.ordinal() < Realm.VOID_SHATTERING.ordinal()) {
					helper.assertTrue(flight == SwordFlight.QI_FLIGHT_SPEED, "Below Void Shattering, Qi Flight's own speed: " + realm.getId());
				} else {
					helper.assertTrue(flight > lastFlight && flight > SwordFlight.QI_FLIGHT_SPEED * 2,
							"Swifter with every stage from Void Shattering: " + realm.getId() + " " + stage.getId() + " (" + flight + ")");
				}
				if (realm.ordinal() >= AstralRealm.MIN_REALM.ordinal()) {
					helper.assertTrue(space > lastSpace, "Space's speed swifter with every stage: " + realm.getId() + " " + stage.getId() + " (" + space + ")");
				}
				lastFlight = flight;
				lastSpace = space;
			}
		}
		float axis = AstralRealm.spaceSpeed(Realm.FOUR_AXIS, Stage.EARLY);
		helper.assertTrue(AstralRealm.spaceSpeed(Realm.VOID_SHATTERING, Stage.EARLY) > 3.5 * axis
				&& AstralRealm.spaceSpeed(Realm.TRUE_IMMORTAL, Stage.EARLY) > 18 * axis, "Void Shattering some four times Four Axis's in space, True Immortal nineteen");
		helper.assertTrue(QiFlight.realmSpeed(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION) > 7 * SwordFlight.QI_FLIGHT_SPEED, "A True Immortal's Qi Flight seven times vanilla's");
		// To the next star, sprinting: Four Axis about two minutes, Void Shattering half a minute, True Immortal a few seconds.
		double star = AstralPlanets.SYSTEM_SPACING;
		helper.assertTrue(star / sprint(axis) > 100 && star / sprint(AstralRealm.spaceSpeed(Realm.VOID_SHATTERING, Stage.EARLY)) < 35
				&& star / sprint(AstralRealm.spaceSpeed(Realm.TRUE_IMMORTAL, Stage.EARLY)) < 8, "The way to the next star");
		helper.assertTrue(Math.abs(QiFlight.freeSpeed(QiFlight.realmSpeed(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION), GhostMethods.LAYERS)
				- QiFlight.realmSpeed(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION) * 1.6f) < 1e-6, "The steps' +60% on the realm's speed");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theirFlightIsTheRealmTheyWield(GameTestHelper helper) {
		ServerPlayer shatterer = cultivator(helper.getLevel(), Realm.VOID_SHATTERING, Stage.EARLY);
		ServerPlayer dead = cultivator(helper.getLevel(), Realm.TRUE_IMMORTAL, Stage.LATE);
		ServerPlayer walker = cultivator(helper.getLevel(), Realm.QI_REFINING, Stage.EARLY);
		try {
			PlayerCultivation c = CultivationManager.get(shatterer);
			c.setAbilityEnabled(Ability.QI_FLIGHT, true);
			c.setQi(c.maxQi());
			QiFlight.tick(shatterer, c);
			float speed = shatterer.getAbilities().getFlyingSpeed();
			helper.assertTrue(shatterer.getAbilities().mayfly && speed == QiFlight.realmSpeed(Realm.VOID_SHATTERING, Stage.EARLY),
					"Void Shattering flies on qi at its realm's speed (" + speed + ")");
			// In a lower realm Realm Suppression holds it to Heavenly Being's might: to Qi Flight's own speed.
			c.setLowerRealmBound(true);
			QiFlight.tick(shatterer, c);
			helper.assertTrue(shatterer.getAbilities().getFlyingSpeed() == SwordFlight.QI_FLIGHT_SPEED, "Held to Heavenly Being in a lower realm, at Qi Flight's own");
			// The dead fly freely at their realm's speed.
			GhostSystem.makeGhost(dead);
			PlayerCultivation d = CultivationManager.get(dead);
			QiFlight.tick(dead, d);
			helper.assertTrue(dead.getAbilities().mayfly && dead.getAbilities().getFlyingSpeed() == QiFlight.realmSpeed(Realm.TRUE_IMMORTAL, Stage.LATE),
					"A True Immortal of the dead flies freely at its realm's speed (" + dead.getAbilities().getFlyingSpeed() + ")");
			// Saved with the player and brought back after a relog, the new speeds are still known as flight's: one without flight now
			// goes back to vanilla's.
			PlayerCultivation w = CultivationManager.get(walker);
			float[] saved = {QiFlight.realmSpeed(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION),
					QiFlight.freeSpeed(QiFlight.realmSpeed(Realm.SACRED_VESSEL, Stage.MID), 6), AstralRealm.spaceSpeed(Realm.ENTERING_NIRVANA, Stage.LATE)};
			for (float s : saved) {
				walker.getAbilities().setFlyingSpeed(s);
				QiFlight.forget(walker.getUUID());
				QiFlight.tick(walker, w);
				helper.assertTrue(walker.getAbilities().getFlyingSpeed() == SwordFlight.QI_FLIGHT_SPEED && !walker.getAbilities().mayfly,
						"A saved " + s + " goes back to Qi Flight's speed (" + walker.getAbilities().getFlyingSpeed() + ")");
			}
		} finally {
			GhostSystem.forget(dead.getUUID());
			QiFlight.forget(shatterer.getUUID());
			QiFlight.forget(dead.getUUID());
			QiFlight.forget(walker.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void roomOnTheServerAndNoBlocksInTheVoid(GameTestHelper helper) {
		ServerPlayer flier = cultivator(helper.getLevel(), Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION);
		ServerPlayer walker = cultivator(helper.getLevel(), Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION);
		float top = AstralRealm.spaceSpeed(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION);
		flier.getAbilities().flying = true;
		flier.getAbilities().setFlyingSpeed(top);
		double step = QiFlight.stepAt(top);
		helper.assertTrue(step > 150 && QiFlight.moveAllowanceSq(flier, 100) >= (float) (step * step * 4),
				"A True Immortal's flight through space (" + Math.round(step) + " blocks a tick) has room, bunched packets too");
		helper.assertTrue(QiFlight.moveAllowanceSq(walker, 100) == 100, "One on foot: vanilla's ten blocks");
		helper.assertTrue(AstralRealm.maxStep(flier) >= 2 * step && AstralRealm.maxStep(walker) == 64, "Its steps flight, not teleports: the years they cost are paid");
		helper.assertTrue(!AstralRealm.passesThrough(flier), "Unchecked for blocks nowhere but space");
		// The test server has only vanilla's dimensions: space's own half is seen in game.
		ServerLevel space = helper.getLevel().getServer().getLevel(ModDimensions.ASTRAL_VOID);
		if (space != null) {
			ServerPlayer voyager = cultivator(space, Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION);
			voyager.getAbilities().flying = true;
			voyager.getAbilities().setFlyingSpeed(top);
			helper.assertTrue(AstralRealm.passesThrough(voyager), "Through space unchecked for blocks");
			voyager.getAbilities().setFlyingSpeed(AstralRealm.starborn(AstralRealm.spaceSpeed(Realm.DAO_INTEGRATION, Stage.GRAND_PERFECTION)));
			helper.assertTrue(!AstralRealm.passesThrough(voyager), "...not at Dao Integration's, a Starborn Body's either");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theSwiftMeetTheAirTheyCrossBetweenTwoTicks(GameTestHelper helper) {
		AstralPlanets planets = AstralPlanets.get();
		helper.assertTrue(planets != null, "The catalog is made");
		AstralPlanets.Planet moon = null;
		for (int i = 0; i < AstralPlanets.PLANETS; i++) {
			AstralPlanets.Planet p = planets.planet(i);
			if (p != null && (moon == null || p.atmosphere() < moon.atmosphere())) moon = p;
		}
		helper.assertTrue(moon != null, "A smallest planet");
		Vec3 centre = moon.pos();
		double air = moon.atmosphere();
		// A step of a True Immortal's (some 170 blocks a tick) can go clean through a small moon's air: neither end in it.
		Vec3 from = centre.add(-air - 20, 0, 0), to = centre.add(air + 20, 0, 0);
		helper.assertTrue(planets.atmosphereAt(from) != moon && planets.atmosphereAt(to) != moon, "Both ends outside its air (" + Math.round(2 * air + 40) + " blocks)");
		AstralPlanets.Crossing crossing = planets.atmosphereAlong(from, to);
		helper.assertTrue(crossing != null && crossing.planet() == moon && Math.abs(crossing.at().distanceTo(centre) - air) < 1e-3 && crossing.at().x < centre.x,
				"Met where the way came into its air");
		AstralPlanets.Crossing wide = planets.atmosphereAlong(from.add(0, air * 1.5, 0), to.add(0, air * 1.5, 0));
		AstralPlanets.Crossing away = planets.atmosphereAlong(to, to.add(200, 0, 0));
		helper.assertTrue((wide == null || wide.planet() != moon) && (away == null || away.planet() != moon), "Not a way passing wide of it, nor one heading away");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theirBodiesMendAtTheirRealmsPace(GameTestHelper helper) {
		ServerPlayer core = cultivator(helper.getLevel(), Realm.CORE_FORMATION, Stage.GRAND_PERFECTION);
		ServerPlayer axis = cultivator(helper.getLevel(), Realm.FOUR_AXIS, Stage.GRAND_PERFECTION);
		ServerPlayer immortal = cultivator(helper.getLevel(), Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION);
		helper.assertTrue(CultivationStats.mendFactor(core) == 1, "A small body mends at vanilla's own pace");
		double axisBody = CultivationStats.bodyHealth(TwoPaths.body(CultivationManager.get(axis), true));
		helper.assertTrue(Math.abs(CultivationStats.mendFactor(axis) - axisBody / 60) < 1e-9 && CultivationStats.mendFactor(axis) > 4,
				"Four Axis Grand Perfection: its body over sixty, some four times vanilla's (" + CultivationStats.mendFactor(axis) + ")");
		helper.assertTrue(CultivationStats.mendFactor(immortal) > 1000, "A True Immortal's over a thousand times (" + CultivationStats.mendFactor(immortal) + ")");
		// Fed as Qi Sustenance keeps them, from half: vanilla's quick heal (every tenth tick) mends that many times its one.
		for (ServerPlayer p : new ServerPlayer[] {core, axis, immortal}) {
			p.setHealth(p.getMaxHealth() / 2);
			FoodData food = p.getFoodData();
			float before = p.getHealth();
			for (int i = 0; i < 10; i++) {
				food.setFoodLevel(20);
				food.setSaturation(20);
				food.tick(p);
			}
			double healed = p.getHealth() - before, want = CultivationStats.mendFactor(p);
			helper.assertTrue(Math.abs(healed - want) < want * 0.01 + 0.01, "A quick heal mends " + want + " (" + healed + ")");
		}
		// The whole of a great body in some thirty seconds of it (two heals a second).
		double whole = immortal.getMaxHealth() / (2 * CultivationStats.mendFactor(immortal));
		helper.assertTrue(whole > 25 && whole < 35, "A True Immortal mended whole in half a minute (" + whole + " s)");
		// An NPC cultivator at rest: two a second, or the whole of a great body in thirty seconds.
		helper.assertTrue(CultivationStats.mendPerSecond(20) == 2 && Math.abs(CultivationStats.mendPerSecond(6000) - 200) < 1e-3, "An NPC's rest mends by its body too");
		helper.succeed();
	}
}
