package com.example.defyingtheheavens;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	/** Shared, temporary cloud bank above an active cultivator; ordinary entity tracking includes late observers. */
	public static final EntityType<TribulationCloud> TRIBULATION_CLOUD = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("tribulation_cloud"),
			EntityType.Builder.<TribulationCloud>of(TribulationCloud::new, MobCategory.MISC)
					.noSave().noSummon().fireImmune()
					.sized(72.0f, 14.0f)
					.clientTrackingRange(12)
					.updateInterval(2)
					.build("tribulation_cloud"));

	/**
	 * Heavenly Tribulation lightning: an ordinary (visual-only) lightning bolt under its own entity type, so the client can
	 * draw it bright blue. Same settings as vanilla's bolt.
	 */
	public static final EntityType<TribulationLightning> TRIBULATION_LIGHTNING = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("tribulation_lightning"),
			EntityType.Builder.<TribulationLightning>of(TribulationLightning::new, MobCategory.MISC)
					.noSave()
					.sized(0.0f, 0.0f)
					.clientTrackingRange(16)
					.updateInterval(Integer.MAX_VALUE)
					.build("tribulation_lightning"));

	/** A meditator's body, left sitting while their soul is in the Inner Realm (see {@link InnerRealm}). */
	public static final EntityType<InnerBodyEntity> INNER_BODY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("inner_body"),
			EntityType.Builder.<InnerBodyEntity>of(InnerBodyEntity::new, MobCategory.MISC)
					.noSummon().fireImmune()
					.sized(0.6f, 1.2f)
					.clientTrackingRange(10)
					.build("inner_body"));

	/** A cultivator's heart demon, risen in their Inner Realm (see {@link HeartDemonEntity}). */
	public static final EntityType<HeartDemonEntity> HEART_DEMON = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("heart_demon"),
			EntityType.Builder.<HeartDemonEntity>of(HeartDemonEntity::new, MobCategory.MONSTER)
					.noSave().noSummon().fireImmune()
					.sized(0.6f, 1.8f)
					.clientTrackingRange(8)
					.build("heart_demon"));

	/** Touching this class registers the entity types. */
	public static void register() {
		net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(INNER_BODY, InnerBodyEntity.createAttributes());
		net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(HEART_DEMON, HeartDemonEntity.createAttributes());
	}

	private ModEntities() {}
}
