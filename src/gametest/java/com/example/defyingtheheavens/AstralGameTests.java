package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;

/**
 * The Astral Realm (AstralPlanets, PlanetTerrain, AstralRealm, VoidVestige, VestigeSpawner): the catalog, the planets' lands,
 * the way from the sky to the land and back, the fliers' and the Vestiges' speeds and strengths. (The game-test server loads none
 * of the mod's dimensions, so going there is checked in game.)
 */
public class AstralGameTests implements FabricGameTest {
	private static AstralPlanets catalog() {
		return AstralPlanets.forSeed(AstralPlanets.astralSeed(20261017L));
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theCatalogHoldsThirtySixHundredPlanetsApart(GameTestHelper helper) {
		AstralPlanets planets = catalog();
		helper.assertTrue(planets.planets().size() == AstralPlanets.PLANETS, "3600 planets (" + planets.planets().size() + ")");
		helper.assertTrue(AstralPlanets.forSeed(planets.seed()) == planets, "One catalog to a seed");
		AstralPlanets again = new AstralPlanetsProbe(planets.seed()).make();
		helper.assertTrue(again.planets().get(1234).pos().equals(planets.planets().get(1234).pos()) && again.planets().get(1234).kind() == planets.planets().get(1234).kind(),
				"The same seed, the same planets");
		Map<AstralPlanets.Kind, Integer> kinds = new EnumMap<>(AstralPlanets.Kind.class);
		for (AstralPlanets.Planet planet : planets.planets()) {
			kinds.merge(planet.kind(), 1, Integer::sum);
			helper.assertTrue(planet.pos().y - planet.atmosphere() > AstralPlanets.SPACE_FLOOR && planet.pos().y + planet.atmosphere() < AstralPlanets.SPACE_CEILING,
					planet.name() + " lies between space's floor and ceiling");
			helper.assertTrue(planet.qi() > 0 && planet.qi() <= 3.0 && AstralPlanets.MAX_QI <= 3.0, planet.name() + "'s qi never passes three times the lower realms' (" + planet.qi() + ")");
			helper.assertTrue(planets.planetAt(planet.landX(), planet.landZ()) == planet, planet.name() + "'s land is its own");
		}
		helper.assertTrue(kinds.size() == AstralPlanets.Kind.values().length, "Every kind of planet is among them (" + kinds + ")");
		// Clear of every star's heat, not its own star's alone: an outer planet of one system used to stray into the next's.
		for (AstralPlanets.Planet planet : planets.planets()) {
			for (AstralPlanets.Star star : planets.stars()) {
				helper.assertTrue(planet.pos().distanceTo(star.pos()) - planet.atmosphere() > star.radius() * AstralRealm.STAR_HEAT,
						planet.name() + "'s air is clear of " + star.name() + "'s heat" + (star.index() == planet.system() ? " (its own star)" : ""));
			}
		}
		// No planet's air touches another's, in its own system or any other, nor the Overworld's.
		java.util.List<AstralPlanets.Planet> all = new java.util.ArrayList<>(planets.planets());
		all.add(planets.overworld());
		for (int i = 0; i < all.size(); i++) {
			AstralPlanets.Planet a = all.get(i);
			for (int j = i + 1; j < all.size(); j++) {
				AstralPlanets.Planet b = all.get(j);
				double reach = a.atmosphere() + b.atmosphere();
				if (Math.abs(a.pos().x - b.pos().x) > reach || Math.abs(a.pos().z - b.pos().z) > reach) continue;
				helper.assertTrue(a.pos().distanceTo(b.pos()) > reach, a.name() + " and " + b.name() + " stand apart");
			}
		}
		helper.succeed();
	}

	/** A second catalog of the same seed, made afresh (forSeed keeps the one in use). */
	private record AstralPlanetsProbe(long seed) {
		AstralPlanets make() {
			AstralPlanets.forSeed(seed ^ 1);
			return AstralPlanets.forSeed(seed);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void eachKindsLandIsItsOwn(GameTestHelper helper) {
		AstralPlanets planets = catalog();
		Map<AstralPlanets.Kind, AstralPlanets.Planet> one = new EnumMap<>(AstralPlanets.Kind.class);
		for (AstralPlanets.Planet planet : planets.planets()) if (planet.parent() < 0) one.putIfAbsent(planet.kind(), planet);
		for (AstralPlanets.Planet planet : one.values()) {
			PlanetTerrain terrain = PlanetTerrain.of(planet);
			RandomSource r = RandomSource.create(planet.seed());
			int wet = 0, n = 400;
			for (int i = 0; i < n; i++) {
				// Spread over the sphere (not the map, which crowds its poles).
				double z = r.nextDouble() * 2 - 1, a = r.nextDouble() * Math.PI * 2, s = Math.sqrt(1 - z * z);
				double[] land = PlanetTerrain.landUnder(new Vec3(s * Math.cos(a), z, s * Math.sin(a)));
				PlanetTerrain.Column c = terrain.column(land[0], land[1], false);
				helper.assertTrue(c.ground() >= PlanetTerrain.MIN_Y && c.ground() <= PlanetTerrain.MAX_GROUND, planet.name() + "'s ground in bounds (" + c.ground() + ")");
				helper.assertTrue(c.ground() + Math.max(0, c.spire()) <= PlanetTerrain.TOP_Y + 30, "...and its spires");
				if (c.underFluid()) wet++;
			}
			double share = wet / (double) n;
			switch (planet.kind()) {
				case OCEAN -> helper.assertTrue(share > 0.6, "An ocean world is mostly sea (" + share + ")");
				case DESERT -> helper.assertTrue(share < 0.3, "A desert world is mostly dry (" + share + ")");
				case BARREN, ASHEN -> helper.assertTrue(share == 0, planet.kind() + " has no seas (" + share + ")");
				default -> { }
			}
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theWayDownIsTheLandBelowAndTheWayUpIsOverIt(GameTestHelper helper) {
		RandomSource r = RandomSource.create(7);
		for (int i = 0; i < 200; i++) {
			double z = r.nextDouble() * 1.8 - 0.9, a = r.nextDouble() * Math.PI * 2, s = Math.sqrt(1 - z * z);
			Vec3 way = new Vec3(s * Math.cos(a), z, s * Math.sin(a));
			double[] land = PlanetTerrain.landUnder(way.scale(500));
			helper.assertTrue(Math.abs(land[0]) <= AstralPlanets.LAND_RADIUS && Math.abs(land[1]) <= AstralPlanets.LAND_POLE, "The land under any way in is the planet's own");
			Vec3 back = PlanetTerrain.directionOver(land[0], land[1]);
			helper.assertTrue(back.distanceTo(way) < 1e-6, "...and the way out over that land is the way in (" + back + " vs " + way + ")");
		}
		helper.assertTrue(PlanetTerrain.directionOver(0, -AstralPlanets.LAND_POLE).y > 0.999, "North on the land is the planet's north pole");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void vestigesGrowWithTheirStrengthAndOutpaceTheirOwnStage(GameTestHelper helper) {
		VoidVestige weakest = ModEntities.VOID_VESTIGE.create(helper.getLevel());
		VoidVestige strongest = ModEntities.VOID_VESTIGE.create(helper.getLevel());
		weakest.setRank(VoidVestige.MIN_RANK);
		strongest.setRank(VoidVestige.MAX_RANK);
		helper.assertTrue(weakest.realm() == Realm.NASCENT_SOUL && weakest.stage() == Stage.EARLY, "The weakest is of Nascent Soul Early");
		helper.assertTrue(strongest.length() > 6 * weakest.length() && strongest.length() > 80, "The strongest is huge (" + strongest.length() + " blocks)");
		helper.assertTrue(strongest.getMaxHealth() > weakest.getMaxHealth() && strongest.getBbWidth() > weakest.getBbWidth(), "...stronger and larger to hit");
		for (int rank = VoidVestige.MIN_RANK; rank <= VoidVestige.MAX_RANK; rank++) {
			VoidVestige v = ModEntities.VOID_VESTIGE.create(helper.getLevel());
			v.setRank(rank);
			double flier = 20.2 * AstralRealm.spaceSpeed(v.realm(), v.stage()); // a cultivator of its stage sprinting through space
			helper.assertTrue(v.maxSpeed() > flier && v.maxSpeed() < flier * 1.2, "A Vestige of " + v.realm() + " " + v.stage() + " is a little faster than its stage flies");
			v.discard();
		}
		helper.assertTrue(20.2 * AstralRealm.spaceSpeed(Realm.HEAVENLY_BEING, Stage.EARLY) > weakest.maxSpeed(), "A Heavenly Being outflies the weakest");
		weakest.discard();
		strongest.discard();
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void vestigesAreStrongerFartherFromHome(GameTestHelper helper) {
		RandomSource r = RandomSource.create(3);
		for (int i = 0; i < 100; i++) {
			helper.assertTrue(VestigeSpawner.rankFor(AstralPlanets.OVERWORLD_POS.add(5000, 0, 0), r) <= VoidVestige.MIN_RANK + 1, "Near home: Nascent Soul Early (now and then Mid)");
			int far = VestigeSpawner.rankFor(AstralPlanets.OVERWORLD_POS.add(400000, 0, 0), r);
			helper.assertTrue(far >= VoidVestige.MAX_RANK - 3 && far <= VoidVestige.MAX_RANK, "Far out: Heavenly Being (" + far + ")");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void spaceFliesFasterTheStrongerOneIs(GameTestHelper helper) {
		float last = 0;
		for (Realm realm : new Realm[] {Realm.NASCENT_SOUL, Realm.HEAVENLY_BEING, Realm.FOUR_AXIS}) {
			for (Stage stage : Stage.values()) {
				float speed = AstralRealm.spaceSpeed(realm, stage);
				helper.assertTrue(speed > last, "Faster with every stage (" + realm + " " + stage + ": " + speed + ")");
				last = speed;
			}
		}
		helper.assertTrue(AstralRealm.spaceSpeed(Realm.NASCENT_SOUL, Stage.EARLY) > SwordFlight.QI_FLIGHT_SPEED * 2, "Space's speed is well past Qi Flight's");
		helper.assertTrue(ModDimensions.isLowerRealm(ModDimensions.ASTRAL_VOID) && ModDimensions.isLowerRealm(ModDimensions.ASTRAL_SURFACES),
				"The Astral Realm is a lower realm (the cap holds there)");
		helper.succeed();
	}
}
