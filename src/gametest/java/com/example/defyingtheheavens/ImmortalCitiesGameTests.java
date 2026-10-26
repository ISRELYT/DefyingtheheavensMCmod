package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * The Immortal Realm's cities on the land as it lies (phase 2, 2026-10-25; the user's ask: the cities "dont need to be on perfectly flat
 * ground", as Minecraft's and Terralith's structures don't): a building's level from the ground under its lot (near its highest, a gap or
 * a crag passed over); the Jade Capital's plan (its houses at their own levels, the Court's stair rising from the city to the Court, the
 * Gate's plaza and the market square evened under them); the other cities' sites (one a cell at most, clear of the capital's island, their
 * districts never meeting, the same each time asked, their ground rolling gently).
 */
public class ImmortalCitiesGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void aBuildingStandsOnItsOwnGround(GameTestHelper helper) {
		helper.assertTrue(CityPlanner.level(new int[] {60, 60, 60, 61, 61, 61, 62, 62, 64}) == 63, "Near the highest of the ground under it");
		helper.assertTrue(CityPlanner.level(new int[] {-64, 60, 60, 61, 61, 61, 62, 62, 64}) == 63, "A column over a gap passed over");
		helper.assertTrue(CityPlanner.level(new int[] {60, 60, 60, 61, 61, 61, 62, 64, 140}) == 63, "A crag passed over");
		helper.assertTrue(CityPlanner.steep(new int[] {60, 60, 60, 61, 61, 61, 62, 64, 75}) && !CityPlanner.steep(new int[] {60, 60, 60, 61, 61, 61, 62, 64, 70}),
				"A house or a shop is left off ground that falls more than ten under its lot");
		helper.succeed();
	}

	/** The Immortal Realm's land as its generator makes it (the tests' server has no Immortal Realm loaded): its noise, generator and heights. */
	record Land(RandomState state, NoiseBasedChunkGenerator generator, LevelHeightAccessor heights) {}

	static Land land(GameTestHelper helper) {
		RegistryAccess access = helper.getLevel().registryAccess();
		Holder<NoiseGeneratorSettings> settings = access.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(ModNoiseSettings.IMMORTAL_REALM);
		RandomState state = RandomState.create(settings.value(), access.lookupOrThrow(Registries.NOISE), helper.getLevel().getSeed());
		Holder<Biome> peaks = access.registryOrThrow(Registries.BIOME).getHolderOrThrow(ModBiomes.JADE_PEAKS);
		return new Land(state, new NoiseBasedChunkGenerator(new FixedBiomeSource(peaks), settings),
				LevelHeightAccessor.create(ModDimensions.IMMORTAL_REALM_MIN_Y, ModDimensions.IMMORTAL_REALM_HEIGHT));
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theCapitalStandsOnItsLand(GameTestHelper helper) {
		Land land = land(helper);
		ImmortalCapital.Plan plan = ImmortalCapital.plan(land.state(), land.generator(), land.heights());
		TreeSet<Integer> houses = new TreeSet<>();
		int stairs = 0, rise = 0, stairY = 0;
		for (SectPiece piece : plan.pieces()) {
			if (piece.kind() == SectPiece.Kind.DWELLING || piece.kind() == SectPiece.Kind.ELDER_HOUSE) houses.add(piece.getBoundingBox().minY());
			if (piece.kind() == SectPiece.Kind.COURT_STAIR) {
				stairs++;
				rise = piece.extra().getInt("Rise");
				stairY = piece.getBoundingBox().minY();
			}
		}
		helper.assertTrue(houses.size() >= 4, "Its houses each at their own ground's level (" + houses.size() + " levels)");
		helper.assertTrue(stairs == 1 && stairY + rise == plan.courtY(), "The Court's stair rises from the city to the Court (" + stairY + " + " + rise + ", the Court at "
				+ plan.courtY() + ")");
		helper.assertTrue(plan.courtY() > plan.squareY() + 50, "The Court high over the city (" + plan.courtY() + " over " + plan.squareY() + ")");
		boolean square = false, gate = false;
		for (BoundingBox box : plan.beards()) {
			if (box.minY() == plan.squareY() && box.getXSpan() >= 70) square = true;
			if (box.minY() == plan.gateY() && box.isInside(new net.minecraft.core.BlockPos(ImmortalGate.HEAVEN_X, plan.gateY(), ImmortalGate.HEAVEN_Z))) gate = true;
		}
		helper.assertTrue(square && gate, "The land evened under the market square and the Gate's plaza (" + square + ", " + gate + ")");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theOtherCitiesStandApart(GameTestHelper helper) {
		Land land = land(helper);
		RandomState state = land.state();
		List<ModNoiseSettings.CitySite> sites = new ArrayList<>();
		for (int cx = -6; cx <= 6; cx++) {
			for (int cz = -6; cz <= 6; cz++) {
				ModNoiseSettings.CitySite site = ModNoiseSettings.citySite(state, cx, cz);
				if (site == null) continue;
				helper.assertTrue(site.equals(ModNoiseSettings.citySite(state, cx, cz)), "The same site each time asked");
				helper.assertTrue(Math.floorDiv(site.x(), ModNoiseSettings.CITY_CELL) == cx && Math.floorDiv(site.z(), ModNoiseSettings.CITY_CELL) == cz,
						"A site in its own cell");
				helper.assertTrue(Math.hypot(site.x() - ModNoiseSettings.MAIN_X, site.z() - ModNoiseSettings.MAIN_Z)
						> ModNoiseSettings.MAIN_OUTER + ModNoiseSettings.CITY_DISTRICT_OUTER, "Clear of the capital's island");
				sites.add(site);
			}
		}
		for (int i = 0; i < sites.size(); i++) {
			for (int j = i + 1; j < sites.size(); j++) {
				double apart = Math.hypot(sites.get(i).x() - sites.get(j).x(), sites.get(i).z() - sites.get(j).z());
				helper.assertTrue(apart > 2 * ModNoiseSettings.CITY_DISTRICT_OUTER, "Two cities' districts never meet (" + (int) apart + " apart)");
			}
		}
		helper.assertTrue(sites.size() >= 169 / 8, "Cities about the realm (" + sites.size() + " of 169 cells)");
		// A city's ground rolls gently (no pinnacle or mountain in it).
		ModNoiseSettings.CitySite first = sites.get(0);
		int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
		for (int dx = -60; dx <= 60; dx += 20) {
			for (int dz = -60; dz <= 60; dz += 20) {
				int y = land.generator().getFirstOccupiedHeight(first.x() + dx, first.z() + dz, Heightmap.Types.WORLD_SURFACE_WG, land.heights(), state);
				lo = Math.min(lo, y);
				hi = Math.max(hi, y);
			}
		}
		helper.assertTrue(lo > ModDimensions.IMMORTAL_CLOUD_Y && hi - lo <= 30, "A city's ground rolls gently (" + lo + ".." + hi + ")");
		helper.succeed();
	}
}
