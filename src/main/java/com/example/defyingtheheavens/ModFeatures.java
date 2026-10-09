package com.example.defyingtheheavens;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/** Custom world-generation placement types (used by datagen, see ModPlacedFeatures). */
public final class ModFeatures {
	/** Placement modifier that targets a random island surface in the column (see IslandSurfacePlacement). */
	public static final PlacementModifierType<IslandSurfacePlacement> ISLAND_SURFACE = Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE,
			DefyingTheHeavens.id("island_surface"), () -> IslandSurfacePlacement.CODEC);

	/** Touching this class registers the feature types. */
	public static void register() {
	}

	private ModFeatures() {}
}
