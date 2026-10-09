package com.example.defyingtheheavens;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	/**
	 * Heavenly Tribulation lightning: an ordinary (visual-only) lightning bolt under its own entity type, so the client can
	 * draw it bright blue. Same settings as vanilla's bolt.
	 */
	public static final EntityType<LightningBolt> TRIBULATION_LIGHTNING = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("tribulation_lightning"),
			EntityType.Builder.<LightningBolt>of(LightningBolt::new, MobCategory.MISC)
					.noSave()
					.sized(0.0f, 0.0f)
					.clientTrackingRange(16)
					.updateInterval(Integer.MAX_VALUE)
					.build("tribulation_lightning"));

	/** Touching this class registers the entity types. */
	public static void register() {
	}

	private ModEntities() {}
}
