package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;

/**
 * The Netherworld as the Nether's high realm (HighRealm), its dimension, and the Upper Realm's things kept out of it. The test
 * server has only vanilla's dimensions, so these read the worldgen registries and call the pure checks, as the dimension's own
 * code does.
 */
public class NetherworldGameTests implements FabricGameTest {
	private static final List<ResourceKey<Biome>> NETHERWORLD_BIOMES = List.of(ModBiomes.YELLOW_SPRINGS, ModBiomes.FIELDS_OF_THE_OTHER_SHORE,
			ModBiomes.BONE_WASTES, ModBiomes.NINE_NETHER_PEAKS);
	private static final List<ResourceKey<Biome>> UPPER_REALM_BIOMES = List.of(ModBiomes.DENSE_QI_PEAKS, ModBiomes.SPIRIT_FOREST,
			ModBiomes.PEACH_BLOSSOM_SANCTUARY, ModBiomes.THUNDER_PEAKS);

	@GameTest(template = EMPTY_STRUCTURE)
	public void theNetherworldIsTheNethersHighRealm(GameTestHelper helper) {
		helper.assertTrue(HighRealm.above(Level.NETHER) == HighRealm.NETHERWORLD, "The Netherworld sits above the Nether");
		helper.assertTrue(HighRealm.above(Level.OVERWORLD) == HighRealm.UPPER_REALM, "...as the Upper Realm above the Overworld");
		helper.assertTrue(HighRealm.above(Level.END) == null, "The End has no high realm");
		helper.assertTrue(HighRealm.of(ModDimensions.NETHERWORLD) == HighRealm.NETHERWORLD && HighRealm.NETHERWORLD.below() == Level.NETHER,
				"Its realm below is the Nether");
		helper.assertTrue(ModDimensions.isHighRealm(ModDimensions.NETHERWORLD) && ModDimensions.isHighRealm(ModDimensions.UPPER_REALM)
				&& !ModDimensions.isHighRealm(Level.NETHER), "Both are high realms; the Nether isn't");
		helper.assertTrue(ModDimensions.isLowerRealm(Level.NETHER) && !ModDimensions.isLowerRealm(ModDimensions.NETHERWORLD),
				"The Nether is still a lower realm, and the Netherworld is not");
		helper.assertTrue(HighRealm.UPPER_REALM.opensFourAxis() && !HighRealm.NETHERWORLD.opensFourAxis(), "Only the Upper Realm opens Four Axis");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theNetherworldLiftsTheCapButKeepsFourAxisAndOrdinaryQiThin(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION, 0);
		RealmSuppressionHandler.applyRealm(c, Level.OVERWORLD);
		double lowerQi = c.qiGatherPerSecond(), lowerRate = c.meditationCultivationPerSecond(0);
		RealmSuppressionHandler.applyRealm(c, ModDimensions.NETHERWORLD);
		helper.assertTrue(!c.isLowerRealmBound() && !c.isInUpperRealm(), "No lower-realm bound there, and it isn't the Upper Realm");
		helper.assertTrue(c.qiGatherPerSecond() == lowerQi && c.meditationCultivationPerSecond(0) == lowerRate,
				"Ordinary qi and cultivation run at the lower realms' rate, not x10");
		c.setState(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION, c.cultivationRequired());
		// Not suppressed, so no realm lock (the user's call, 2026-10-19: Four Axis is in reach anywhere not suppressed); but the heavens
		// can't see the Netherworld, so no heavenly tribulation comes there to take it (TribulationManager#start).
		helper.assertTrue(c.isAtBottleneck() && !c.isBreakthroughLocked() && ModDimensions.isUnseenByHeaven(ModDimensions.NETHERWORLD),
				"Breakthroughs past Heavenly Being Grand Perfection aren't locked by the realm, but the heavens don't see here");
		RealmSuppressionHandler.applyRealm(c, ModDimensions.UPPER_REALM);
		helper.assertTrue(!c.isBreakthroughLocked() && Math.abs(c.qiGatherPerSecond() - lowerQi * PlayerCultivation.UPPER_REALM_QI_MULTIPLIER) < 1.0e-9,
				"...and open in the Upper Realm, with its x10");
		c.setState(Realm.FOUR_AXIS, Stage.MID, 0);
		RealmSuppressionHandler.applyRealm(c, ModDimensions.NETHERWORLD);
		helper.assertTrue(!c.isSuppressed() && c.sustainedRank() == PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.MID),
				"A Four Axis cultivator is held to nothing there");
		RealmSuppressionHandler.applyRealm(c, Level.NETHER);
		helper.assertTrue(c.isSuppressed(), "...but is suppressed in the Nether below");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void portalsAndTheHeavensAreRefusedThere(GameTestHelper helper) {
		helper.assertTrue(PortalRestrictionHandler.isRestricted(ModDimensions.NETHERWORLD) && PortalRestrictionHandler.isRestricted(ModDimensions.SOUL_RIVER)
				&& PortalRestrictionHandler.isRestricted(ModDimensions.UPPER_REALM) && PortalRestrictionHandler.isRestricted(ModDimensions.SPATIAL_GAP),
				"No portals in the high realms or their passages");
		helper.assertTrue(!PortalRestrictionHandler.isRestricted(Level.NETHER) && !PortalRestrictionHandler.isRestricted(Level.OVERWORLD),
				"...as ever below");
		helper.assertTrue(ModDimensions.isUnseenByHeaven(ModDimensions.NETHERWORLD) && !ModDimensions.isUnseenByHeaven(ModDimensions.UPPER_REALM),
				"The heavens can't see into the Netherworld (no tribulations, rituals, Inner Realm or Soul Projection)");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theNetherworldIsEternalNightWithBedsThatExplode(GameTestHelper helper) {
		Registry<DimensionType> types = helper.getLevel().registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
		DimensionType type = types.get(ModDimensions.NETHERWORLD_TYPE);
		helper.assertTrue(type != null, "The Netherworld's dimension type is registered");
		helper.assertTrue(type.fixedTime().isPresent() && type.fixedTime().getAsLong() == 18000L, "Fixed at midnight");
		helper.assertTrue(!type.hasSkyLight() && !type.hasCeiling(), "No skylight, no ceiling");
		helper.assertTrue(!type.natural(), "Not natural: compasses spin");
		helper.assertTrue(!type.bedWorks() && !type.respawnAnchorWorks(), "Beds and respawn anchors don't work");
		helper.assertTrue(type.coordinateScale() == 8.0, "x/z line up with the Nether's");
		helper.assertTrue(type.infiniburn().equals(BlockTags.INFINIBURN_NETHER), "Infiniburn like the Nether's");
		helper.assertTrue(Math.abs(type.ambientLight() - ModDimensions.NETHERWORLD_AMBIENT_LIGHT) < 1.0e-6, "Dim, but not pitch black");
		helper.assertTrue(types.get(ModDimensions.SOUL_RIVER_TYPE) != null, "The Soul River's type too");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theUpperRealmsThingsStayOutOfTheNetherworld(GameTestHelper helper) {
		Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
		for (ResourceKey<Biome> key : NETHERWORLD_BIOMES) {
			Holder<Biome> biome = biomes.getHolderOrThrow(key);
			helper.assertTrue(biome.is(ModTags.IS_NETHERWORLD) && !biome.is(ModTags.IS_UPPER_REALM), key.location() + " is the Netherworld's");
			helper.assertTrue(!SectStructure.isUpperRealmBiome(biome), "A sect there wouldn't be laid out the Upper Realm's way (" + key.location() + ")");
			// Fabric's biome modifications are applied to the registry's biomes: none of the Upper Realm's herbs, ores or fruit.
			for (HolderSet<PlacedFeature> step : biome.value().getGenerationSettings().features()) {
				for (Holder<PlacedFeature> feature : step) {
					String id = feature.unwrapKey().map(k -> k.location().toString()).orElse("?");
					helper.assertTrue(!id.contains("upper_realm") && !ModPlacedFeatures.COMMON.contains(feature.unwrapKey().orElse(null)),
							key.location() + " grows the Upper Realm's " + id);
				}
			}
		}
		for (ResourceKey<Biome> key : UPPER_REALM_BIOMES) {
			Holder<Biome> biome = biomes.getHolderOrThrow(key);
			helper.assertTrue(biome.is(ModTags.IS_UPPER_REALM) && SectStructure.isUpperRealmBiome(biome), key.location() + " is still the Upper Realm's");
			boolean grows = false;
			for (HolderSet<PlacedFeature> step : biome.value().getGenerationSettings().features()) {
				for (Holder<PlacedFeature> feature : step) grows |= feature.is(ModPlacedFeatures.GINSENG_UPPER_REALM);
			}
			helper.assertTrue(grows, key.location() + " still grows the Upper Realm's ginseng");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void noLivingRoguesInTheRealmOfTheDead(GameTestHelper helper) {
		helper.assertTrue(!NpcSpawner.spawnsLiving(ModDimensions.NETHERWORLD) && !NpcSpawner.spawnsLiving(ModDimensions.SOUL_RIVER),
				"No living rogues or mortals in the Netherworld or on the Soul River");
		helper.assertTrue(NpcSpawner.spawnsLiving(Level.NETHER) && NpcSpawner.spawnsLiving(ModDimensions.UPPER_REALM), "...as before elsewhere");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void eachRiftLeadsAccordingToItsRealm(GameTestHelper helper) {
		helper.assertTrue(SpatialRiftBlock.leads(Level.OVERWORLD) == SpatialRiftBlock.Leads.UPPER_REALM, "The Overworld's rift leads up to the Upper Realm");
		helper.assertTrue(SpatialRiftBlock.leads(Level.NETHER) == SpatialRiftBlock.Leads.NETHERWORLD, "The Nether's leads up to the Netherworld");
		helper.assertTrue(SpatialRiftBlock.leads(ModDimensions.NETHERWORLD) == SpatialRiftBlock.Leads.DOWN_TO_NETHER, "The Netherworld's is the way down");
		helper.assertTrue(SpatialRiftBlock.leads(Level.END) == SpatialRiftBlock.Leads.NOWHERE, "...and none leads anywhere from the End");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theGateKeepsItsWholeGround(GameTestHelper helper) {
		BlockPos[] inside = {GhostGate.ARRIVAL, GhostGate.WAY_DOWN, GhostGate.WHEEL, GhostGate.TERRACE_TOP, GhostGate.MENG_PO_POT,
				new BlockPos(-10, 0, -10), new BlockPos(10, 0, -10)};
		for (BlockPos pos : inside) {
			helper.assertTrue(GhostGate.withinRadius(new BlockPos(GhostGate.X + pos.getX(), 100, GhostGate.Z + pos.getZ()), 0),
					"The gate's ground covers " + pos.toShortString());
		}
		helper.assertTrue(!GhostGate.withinRadius(new BlockPos(0, 100, -40), 0) && !GhostGate.withinRadius(new BlockPos(60, 100, 13), 0),
				"...and nothing far beyond");
		helper.succeed();
	}
}
