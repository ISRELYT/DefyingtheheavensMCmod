package com.example.defyingtheheavens;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
	/** Carries no data; it exists so the client can draw the rift with a block entity renderer. */
	public static final BlockEntityType<SpatialRiftBlockEntity> SPATIAL_RIFT = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("spatial_rift"),
			BlockEntityType.Builder.of(SpatialRiftBlockEntity::new, ModBlocks.SPATIAL_RIFT).build(null));

	/** Touching this class registers the block entity types. */
	public static void register() {
	}

	private ModBlockEntities() {}
}
