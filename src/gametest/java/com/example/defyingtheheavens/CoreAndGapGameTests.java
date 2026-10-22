package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Detonating the golden core (CoreDetonation): its harm by distance and realm, asked twice before it breaks, and the cultivator's
 * death with the core lost. The Spatial Tear (SpatialTrialHandler#tear): only from Heavenly Being, only where the gap opens, out
 * only back home; the gap's stormy layers above and below (the gap itself isn't on the test server: its rules are checked by number).
 */
public class CoreAndGapGameTests implements FabricGameTest {
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void theBlastFallsOffByDistanceAndRealm(GameTestHelper helper) {
		Realm cf = Realm.CORE_FORMATION;
		double r = CoreDetonation.radius(cf, Stage.EARLY);
		double heart = CoreDetonation.harm(cf, Stage.EARLY, r, 0, cf.ordinal());
		double body = 20 + CultivationStats.maxHealth(cf, Stage.EARLY);
		helper.assertTrue(Math.abs(heart - CoreDetonation.HEART_HARM * body) < 1e-6, "At its heart: more than a whole body of its own stage");
		helper.assertTrue(Math.abs(CoreDetonation.harm(cf, Stage.EARLY, r, r / 2, cf.ordinal()) - heart / 2) < 1e-6, "Halfway out, half");
		helper.assertTrue(CoreDetonation.harm(cf, Stage.EARLY, r, r, cf.ordinal()) == 0, "Nothing past its reach");
		helper.assertTrue(CoreDetonation.harm(cf, Stage.EARLY, r, 0, Realm.FOUNDATION_BUILDING.ordinal()) == heart * 2, "A realm below: twice");
		helper.assertTrue(Math.abs(CoreDetonation.harm(cf, Stage.EARLY, r, 0, Realm.NASCENT_SOUL.ordinal()) - heart * CoreDetonation.ABOVE_FACTOR) < 1e-6,
				"A realm above: far less");
		helper.assertTrue(CoreDetonation.radius(Realm.HEAVENLY_BEING, Stage.EARLY) > r, "A deeper core's blast reaches further");
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void everyHigherRealmBlastsFurther(GameTestHelper helper) {
		for (Realm realm = Realm.CORE_FORMATION; !realm.isLast(); realm = realm.next()) {
			for (Stage stage : Stage.values()) {
				helper.assertTrue(CoreDetonation.radius(realm.next(), stage) > CoreDetonation.radius(realm, stage),
						realm.next().getId() + " " + stage + ": reaches further than " + realm.getId() + "'s");
			}
		}
		helper.assertTrue(CoreDetonation.radius(Realm.TRUE_IMMORTAL, Stage.EARLY) > 3 * CoreDetonation.radius(Realm.FOUR_AXIS, Stage.EARLY),
				"A True Immortal's reaches past three times a Four Axis's");
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
	public void theCoreBreaksOnlyWhenAskedTwiceAndIsLost(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		p.setGameMode(GameType.SURVIVAL);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(Realm.FOUNDATION_BUILDING, Stage.LATE, 0);
		helper.assertTrue(!Ability.CORE_DETONATION.isUnlocked(c), "Below Core Formation there is no core to detonate");
		c.setState(Realm.CORE_FORMATION, Stage.MID, 0);
		CultivationManager.refresh(p);
		helper.assertTrue(Ability.CORE_DETONATION.isUnlocked(c), "From Core Formation, there is");
		CoreDetonation.toggle(p);
		helper.assertTrue(!CoreDetonation.isCharging(p.getUUID()), "Asked once, it only trembles");
		Vec3 at = p.position();
		Husk near = EntityType.HUSK.create(helper.getLevel());
		near.moveTo(at.x + 3, at.y, at.z);
		near.setNoAi(true);
		helper.getLevel().addFreshEntity(near);
		Husk far = EntityType.HUSK.create(helper.getLevel());
		far.moveTo(at.x + 40, at.y, at.z);
		far.setNoAi(true);
		helper.getLevel().addFreshEntity(far);
		CoreDetonation.toggle(p);
		helper.assertTrue(!CoreDetonation.isCharging(p.getUUID()), "Asked again at once (a double click), it lets it pass");
		helper.runAfterDelay(CoreDetonation.CONFIRM_MIN_TICKS + 2, () -> {
			CoreDetonation.toggle(p);
			helper.assertTrue(CoreDetonation.isCharging(p.getUUID()) && c.isAbilityEnabled(Ability.CORE_DETONATION), "Asked again a moment later, it breaks");
		});
		helper.runAfterDelay(CoreDetonation.CONFIRM_MIN_TICKS + 2 + CoreDetonation.CHARGE_TICKS + 5, () -> {
			helper.assertTrue(!near.isAlive(), "A beast beside it is gone");
			helper.assertTrue(far.isAlive(), "One beyond its reach is untouched");
			helper.assertTrue(!p.isAlive() || p.isDeadOrDying(), "Its cultivator dies");
			helper.assertTrue(c.getRealm() == Realm.FOUNDATION_BUILDING && c.getStage() == Stage.GRAND_PERFECTION, "...the core lost: Foundation Building Grand Perfection");
			far.discard();
			helper.succeed();
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void spellsSparePlayersTheyMayNotHarm(GameTestHelper helper) {
		ServerPlayer caster = helper.makeMockServerPlayerInLevel();
		ServerPlayer other = helper.makeMockServerPlayerInLevel();
		boolean pvp = helper.getLevel().getServer().isPvpAllowed();
		try {
			helper.getLevel().getServer().setPvpAllowed(false);
			helper.assertTrue(HeavenSpells.spared(caster, other), "PvP off: another player is spared, holds and drownings too");
			helper.getLevel().getServer().setPvpAllowed(true);
			helper.assertTrue(!HeavenSpells.spared(caster, other), "PvP on: not spared");
		} finally {
			helper.getLevel().getServer().setPvpAllowed(pvp);
		}
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void spellBlocksAreNoQuarry(GameTestHelper helper) {
		net.minecraft.server.level.ServerLevel level = helper.getLevel();
		net.minecraft.core.BlockPos pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 3, 1));
		level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
		helper.assertTrue(HeavenSpells.placeTemporary(level, pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 400), "A spell's stone set down");
		helper.assertTrue(HeavenSpells.breakTemporary(level, pos) && level.getBlockState(pos).isAir() && !HeavenSpells.isTemporary(level, pos),
				"Broken, it is taken up: air again, nothing left of it");
		java.util.List<net.minecraft.core.BlockPos> blast = new java.util.ArrayList<>();
		HeavenSpells.placeTemporary(level, pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 400);
		blast.add(pos);
		HeavenSpells.spareTemporaryFromBlast(level, blast);
		helper.assertTrue(blast.isEmpty() && level.getBlockState(pos).isAir(), "A blast takes it up rather than blowing it into loot");
		ItemStack ring = StorageRingItem.of(StorageRings.Grade.MORTAL);
		helper.assertTrue(!ring.getItem().canBeHurtBy(level.damageSources().cactus()) && !ring.getItem().canBeHurtBy(level.damageSources().inFire()),
				"A dropped Storage Ring is proof against cactus and fire");
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void spaceTearsOnlyForHeavenlyBeings(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		p.setGameMode(GameType.SURVIVAL);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(Realm.NASCENT_SOUL, Stage.GRAND_PERFECTION, 0);
		helper.assertTrue(!Ability.SPATIAL_TEAR.isUnlocked(c), "Nascent Soul can't tear space");
		c.setState(Realm.HEAVENLY_BEING, Stage.EARLY, 0);
		CultivationManager.refresh(p);
		c.setQi(c.maxQi());
		helper.assertTrue(Ability.SPATIAL_TEAR.isUnlocked(c), "A Heavenly Being can");
		double before = c.getQi();
		SpatialTrialHandler.tear(p);
		if (helper.getLevel().getServer().getLevel(ModDimensions.SPATIAL_GAP) == null) {
			// The test server has only vanilla's dimensions: no gap to open onto, so nothing is spent and nothing happens.
			helper.assertTrue(c.getQi() == before && !SpatialTrialHandler.isInTrial(p.getUUID()), "No gap: space won't tear, no qi spent");
		} else {
			helper.assertTrue(SpatialTrialHandler.isTravelling(p.getUUID()) && p.level().dimension() == ModDimensions.SPATIAL_GAP
					&& Math.abs(c.getQi() - before * (1 - SpatialTrialHandler.TEAR_IN_QI)) < 1, "Torn in, for a quarter of the pool");
			SpatialTrialHandler.forget(p.getUUID());
			c.setPendingTrial(SpatialTrialHandler.Route.NONE);
		}
		helper.assertTrue(SpatialTrialHandler.GAP_SCALE == 20, "A block in the gap is twenty in the world");
		helper.succeed();
	}

	/**
	 * The old trial is gone (the user, 2026-10-18): one below Heavenly Being stepping into the rift at 0, 0 is in the gap as a
	 * Heavenly Being would be, free to fly it, in its shallows at a twentieth of where they stood; and an old save's descent trial,
	 * back in the gap, goes on as travel from the Upper Realm.
	 */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void anyoneInTheGapFliesItNoTrial(GameTestHelper helper) {
		net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
		if (server.getLevel(ModDimensions.SPATIAL_GAP) == null) { // only vanilla's dimensions on this server: no gap to enter
			helper.succeed();
			return;
		}
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		p.setGameMode(GameType.SURVIVAL);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
		CultivationManager.refresh(p);
		try {
			p.moveTo(400, p.getY(), -200, 0, 0);
			SpatialTrialHandler.beginAscension(p);
			helper.assertTrue(SpatialTrialHandler.isTravelling(p.getUUID()) && p.level().dimension() == ModDimensions.SPATIAL_GAP
					&& Math.abs(p.getY() - SpatialTrialHandler.TEAR_IN_Y) < 1 && Math.abs(p.getX() - 20) < 1 && Math.abs(p.getZ() + 10) < 1
					&& c.getPendingTrial() == SpatialTrialHandler.Route.TRAVEL && !c.isGapFromUpper(),
					"Through the rift, a Core Formation cultivator is in the gap's shallows, free to fly it (" + p.position() + ")");
			SpatialTrialHandler.forget(p.getUUID());
			c.setPendingTrial(SpatialTrialHandler.Route.DESCEND); // as a save from the old trials' days left it
			SpatialTrialHandler.tick(server);
			helper.assertTrue(SpatialTrialHandler.isTravelling(p.getUUID()) && c.getPendingTrial() == SpatialTrialHandler.Route.TRAVEL
					&& c.isGapFromUpper(), "An old descent's trial, back in the gap: travelling, come from the Upper Realm");
			helper.succeed();
		} finally {
			SpatialTrialHandler.forget(p.getUUID());
			c.setPendingTrial(SpatialTrialHandler.Route.NONE);
			c.setAbilityEnabled(Ability.SPATIAL_TEAR, false);
			c.setGapFromUpper(false);
			net.minecraft.core.BlockPos home = helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
			p.teleportTo(helper.getLevel(), home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0, 0);
		}
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void aTearLeadsHomeAndTheWayOnIsUpOrDown(GameTestHelper helper) {
		SpatialTrialHandler.Route travel = SpatialTrialHandler.Route.TRAVEL;
		helper.assertTrue(SpatialTrialHandler.homeRealm(travel, false) == net.minecraft.world.level.Level.OVERWORLD
				&& SpatialTrialHandler.homeRealm(travel, true) == ModDimensions.UPPER_REALM, "Tearing out leads back to the realm torn in from");
		helper.assertTrue(SpatialTrialHandler.homeRealm(SpatialTrialHandler.Route.DESCEND, false) == ModDimensions.UPPER_REALM
				&& SpatialTrialHandler.homeRealm(SpatialTrialHandler.Route.ASCEND, true) == net.minecraft.world.level.Level.OVERWORLD,
				"...an old save's trial, to the realm it set out from (a descent's tear can't skip it)");
		helper.assertTrue(SpatialTrialHandler.pressureAt(SpatialTrialHandler.TEAR_IN_Y) == SpatialTrialHandler.SHALLOW_PRESSURE
				&& SpatialTrialHandler.pressureAt(SpatialTrialHandler.UPPER_LAYERS_Y) == 8.2f
				&& SpatialTrialHandler.pressureAt(SpatialTrialHandler.LOWER_LAYERS_Y - 1) == 8.2f
				&& SpatialTrialHandler.pressureAt(SpatialTrialHandler.LOWER_LAYERS_Y) == SpatialTrialHandler.SHALLOW_PRESSURE,
				"The shallows press lightly (2.5 a second); the layers above and below with 8.2");
		helper.assertTrue(SpatialTrialHandler.inLowerLayers(SpatialTrialHandler.FLOOR_Y) && !SpatialTrialHandler.inLowerLayers(SpatialTrialHandler.FLOOR_Y - 1)
				&& !SpatialTrialHandler.inLowerLayers(SpatialTrialHandler.LOWER_LAYERS_Y), "The lower layers run from their top down to the floor");
		helper.assertTrue(SpatialStorms.targetAround(SpatialTrialHandler.LOWER_LAYERS_Y - 10) == SpatialStorms.targetAround(SpatialTrialHandler.UPPER_LAYERS_Y + 10)
				&& SpatialStorms.targetAround(SpatialTrialHandler.LOWER_LAYERS_Y - 10) > SpatialStorms.targetAround(SpatialTrialHandler.TEAR_IN_Y),
				"Storms crowd the lower layers as they do the upper");
		double seconds = (SpatialTrialHandler.LOWER_LAYERS_Y - SpatialTrialHandler.FLOOR_Y) / (SpatialTrialHandler.LOWER_SINK * 20);
		helper.assertTrue(seconds >= 10 && SpatialTrialHandler.LOWER_SINK * 20 < 0.225 * 20,
				"Sinking through them takes " + seconds + " s, no faster than Qi Flight climbs: about 50 pressure before the storms");
		helper.succeed();
	}

	/**
	 * The Upper Realm's islands found by numbers (SpatialArrivals#islandColumn), against the realm's own generator built from its
	 * noise settings (the test server has no Upper Realm): islands are found, each well inside one by its mask with ground under it
	 * by the generator, in milliseconds, and nothing generated to find them.
	 */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
	public void upperRealmIslandsAreFoundByNumbers(GameTestHelper helper) {
		var access = helper.getLevel().registryAccess();
		Holder<NoiseGeneratorSettings> settings = access.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(ModNoiseSettings.UPPER_REALM);
		RandomState noise = RandomState.create(settings.value(), access.lookupOrThrow(Registries.NOISE), 20261016L);
		NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(
				new FixedBiomeSource(access.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS)), settings);
		LevelHeightAccessor heights = LevelHeightAccessor.create(ModDimensions.UPPER_REALM_MIN_Y, ModDimensions.UPPER_REALM_HEIGHT);
		int found = 0, spots = 12;
		long nanos = 0;
		for (int i = 0; i < spots; i++) {
			int x = 1000 * i - 6000, z = 7919 * i % 9000 - 4500;
			long start = System.nanoTime();
			BlockPos column = SpatialArrivals.islandColumn(generator, noise, heights, x, z);
			nanos += System.nanoTime() - start;
			if (column == null) continue;
			found++;
			helper.assertTrue(Math.abs(column.getX() - x) <= SpatialArrivals.ISLAND_REACH && Math.abs(column.getZ() - z) <= SpatialArrivals.ISLAND_REACH,
					"Within reach of the point sought");
			helper.assertTrue(ModNoiseSettings.islandMask(noise, column.getX(), column.getZ()) > SpatialArrivals.INSIDE
					&& generator.getBaseHeight(column.getX(), column.getZ(), Heightmap.Types.WORLD_SURFACE_WG, heights, noise) > heights.getMinBuildHeight() + 2,
					"Well inside an island, with ground under it (" + column.toShortString() + ")");
		}
		double ms = nanos / 1.0e6 / spots;
		helper.assertTrue(found >= spots / 3, "Islands found near most points (" + found + " of " + spots + ")");
		helper.assertTrue(ms < 150, "In milliseconds, not the seconds generating the sky would take (" + Math.round(ms) + " ms a search)");
		helper.succeed();
	}

	/**
	 * Where an NPC's journey sets down in the Upper Realm (NpcTravel#nearestIsland), against the realm's own generator as above:
	 * only columns well inside an island by its mask are looked at, and the generator is asked about a few at most (about 10 ms
	 * each), not about every point out to the search's reach (some 430: seconds, with no island near); land that is loaded is
	 * read as it stands, the generator not asked at all.
	 */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 200)
	public void anNpcLandingAsksTheGeneratorOnlyAFewTimes(GameTestHelper helper) {
		var access = helper.getLevel().registryAccess();
		Holder<NoiseGeneratorSettings> settings = access.registryOrThrow(Registries.NOISE_SETTINGS).getHolderOrThrow(ModNoiseSettings.UPPER_REALM);
		RandomState noise = RandomState.create(settings.value(), access.lookupOrThrow(Registries.NOISE), 20261016L);
		NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(
				new FixedBiomeSource(access.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS)), settings);
		LevelHeightAccessor heights = LevelHeightAccessor.create(ModDimensions.UPPER_REALM_MIN_Y, ModDimensions.UPPER_REALM_HEIGHT);
		int floor = heights.getMinBuildHeight() + 2;
		int[] asked = {0};
		boolean[] loaded = {false}, stands = {true};
		// The land as the generator makes it: none of it loaded (or all of it), and ground wherever it has some (or none to
		// stand on anywhere: all trunks and ponds).
		NpcTravel.IslandLand land = new NpcTravel.IslandLand() {
			@Override
			public boolean loaded(int x, int z) {
				return loaded[0];
			}

			@Override
			public boolean groundUnder(int x, int z) {
				asked[0]++;
				return generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, noise) > floor;
			}

			@Override
			public BlockPos ground(int x, int z) {
				if (!stands[0]) return null;
				int top = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, noise);
				return top > floor ? new BlockPos(x, top, z) : null;
			}
		};
		int spots = 12, found = 0, foundLoaded = 0, most = 0;
		long nanos = 0;
		for (int i = 0; i < spots; i++) {
			int x = 1000 * i - 6000, z = 7919 * i % 9000 - 4500;
			loaded[0] = false;
			stands[0] = true;
			asked[0] = 0;
			BlockPos island = NpcTravel.nearestIsland(noise, land, x, z);
			most = Math.max(most, asked[0]);
			if (island != null) {
				found++;
				helper.assertTrue(Math.abs(island.getX() - x) <= NpcTravel.ISLAND_REACH && Math.abs(island.getZ() - z) <= NpcTravel.ISLAND_REACH,
						"Within reach of where it was bound");
				helper.assertTrue(ModNoiseSettings.islandMask(noise, island.getX(), island.getZ()) > SpatialArrivals.INSIDE,
						"Well inside an island (" + island.toShortString() + ")");
			}
			// Nothing to stand on anywhere round: it gives up after a few questions, not hundreds.
			stands[0] = false;
			asked[0] = 0;
			long start = System.nanoTime();
			helper.assertTrue(NpcTravel.nearestIsland(noise, land, x, z) == null, "No ground to stand on, no island found");
			nanos += System.nanoTime() - start;
			most = Math.max(most, asked[0]);
			// All of it loaded: read as it stands.
			loaded[0] = true;
			stands[0] = true;
			asked[0] = 0;
			if (NpcTravel.nearestIsland(noise, land, x, z) != null) foundLoaded++;
			helper.assertTrue(asked[0] == 0, "Loaded land is read as it stands, the generator not asked (" + asked[0] + " times)");
		}
		double ms = nanos / 1.0e6 / spots;
		helper.assertTrue(most <= NpcTravel.ISLAND_ASKS, "The generator asked a few times at most a landing (" + most + ")");
		helper.assertTrue(found >= spots / 3 && foundLoaded >= found, "Islands found near most points (" + found + " of " + spots + "; "
				+ foundLoaded + " in loaded land)");
		helper.assertTrue(ms < 250, "With nothing to stand on, given up in milliseconds, not seconds (" + Math.round(ms) + " ms a search)");
		helper.succeed();
	}

	/** The land asked for ahead loads without the server waiting on it (SpatialArrivals#prepare), and the arrival reads only loaded land. */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 300)
	public void theLandAheadLoadsWhileTheServerGoesOn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int far = 200_000 + level.getRandom().nextInt(100_000);
		BlockPos column = new BlockPos(far, 0, far + 4000);
		BlockPos untouched = new BlockPos(-far, 0, far);
		helper.assertTrue(!SpatialArrivals.ready(level, column) && !SpatialArrivals.ready(level, untouched), "Land nobody is near isn't loaded");
		helper.assertTrue(SpatialArrivals.groundNear(level, untouched) == null && !SpatialArrivals.ready(level, untouched),
				"Looking for ground there loads none: there is none to be found");
		SpatialArrivals.prepare(level, column);
		helper.assertTrue(!SpatialArrivals.ready(level, column), "Asking for it doesn't load it then and there");
		whenReady(helper, level, column, 280, () -> {
			helper.assertTrue(SpatialArrivals.ready(level, column), "...it is loaded a little later, the server going on meanwhile");
			helper.assertTrue(SpatialArrivals.groundNear(level, column) != null, "...and ground is found on it");
			helper.succeed();
		});
	}

	private static void whenReady(GameTestHelper helper, ServerLevel level, BlockPos column, int ticks, Runnable then) {
		if (ticks <= 0 || SpatialArrivals.ready(level, column)) then.run();
		else helper.runAfterDelay(1, () -> whenReady(helper, level, column, ticks - 1, then));
	}
}
