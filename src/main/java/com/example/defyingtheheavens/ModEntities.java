package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.EnumMap;
import java.util.Map;

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

	/** NPC cultivators: sect members and rogues (see CultivatorNpc). Player-sized. */
	public static final EntityType<CultivatorNpc> CULTIVATOR = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("cultivator"),
			EntityType.Builder.<CultivatorNpc>of(CultivatorNpc::new, MobCategory.MISC)
					.sized(0.6f, 1.8f)
					.clientTrackingRange(10)
					.build("cultivator"));

	/** The Netherworld's ghost cultivators, its officials among them (see GhostCultivatorNpc). Player-sized; fire passes through them. */
	public static final EntityType<GhostCultivatorNpc> GHOST_CULTIVATOR = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("ghost_cultivator"),
			EntityType.Builder.<GhostCultivatorNpc>of(GhostCultivatorNpc::new, MobCategory.MISC)
					.fireImmune()
					.sized(0.6f, 1.8f)
					.clientTrackingRange(10)
					.build("ghost_cultivator"));

	/** The Netherworld's Hungry Ghosts (see HungryGhost): packs of the starving dead. */
	public static final EntityType<HungryGhost> HUNGRY_GHOST = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("hungry_ghost"),
			EntityType.Builder.<HungryGhost>of(HungryGhost::new, MobCategory.MONSTER)
					.fireImmune()
					.sized(0.6f, 1.8f)
					.clientTrackingRange(8)
					.build("hungry_ghost"));

	/** A thrown Ghost-Quelling Talisman (see GhostQuellingTalismanEntity): small and quick, like a snowball. */
	public static final EntityType<GhostQuellingTalismanEntity> GHOST_QUELLING_TALISMAN = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("ghost_quelling_talisman"),
			EntityType.Builder.<GhostQuellingTalismanEntity>of(GhostQuellingTalismanEntity::new, MobCategory.MISC)
					.sized(0.25f, 0.25f)
					.clientTrackingRange(4)
					.updateInterval(10)
					.build("ghost_quelling_talisman"));

	/** A meditator's body, left sitting while their soul is in the Inner Realm (see {@link InnerRealm}). */
	public static final EntityType<InnerBodyEntity> INNER_BODY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("inner_body"),
			EntityType.Builder.<InnerBodyEntity>of(InnerBodyEntity::new, MobCategory.MISC)
					.noSummon().fireImmune()
					.sized(0.6f, 1.2f)
					.clientTrackingRange(10)
					.build("inner_body"));

	/** A cultivator's body, left sitting while their nascent soul is out (see {@link SoulProjection}). */
	public static final EntityType<SoulBodyEntity> SOUL_BODY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("soul_body"),
			EntityType.Builder.<SoulBodyEntity>of(SoulBodyEntity::new, MobCategory.MISC)
					.noSave().noSummon()
					.sized(0.6f, 1.2f)
					.clientTrackingRange(10)
					.build("soul_body"));

	/**
	 * A body of a cultivator with a clone out that isn't being played (see {@link Clones}): the clone standing, player-sized, or the main
	 * body sitting ({@link CloneBodyEntity#getDimensions}). Saved: it outlasts its owner's logout.
	 */
	public static final EntityType<CloneBodyEntity> CLONE_BODY = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("clone_body"),
			EntityType.Builder.<CloneBodyEntity>of(CloneBodyEntity::new, MobCategory.MISC)
					.noSummon()
					.sized(0.6f, 1.8f)
					.clientTrackingRange(10)
					.build("clone_body"));

	/** A player's body seized by another cultivator's nascent soul (see {@link SoulVesselEntity}). Player-sized. */
	public static final EntityType<SoulVesselEntity> SOUL_VESSEL = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("soul_vessel"),
			EntityType.Builder.<SoulVesselEntity>of(SoulVesselEntity::new, MobCategory.MISC)
					.noSummon()
					.sized(0.6f, 1.8f)
					.clientTrackingRange(10)
					.build("soul_vessel"));

	/** A cultivator's heart demon, risen in their Inner Realm (see {@link HeartDemonEntity}). */
	public static final EntityType<HeartDemonEntity> HEART_DEMON = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("heart_demon"),
			EntityType.Builder.<HeartDemonEntity>of(HeartDemonEntity::new, MobCategory.MONSTER)
					.noSave().noSummon().fireImmune()
					.sized(0.6f, 1.8f)
					.clientTrackingRange(8)
					.build("heart_demon"));

	/** What an element spell throws (see {@link SpellProjectile}): small by default, its hitbox set by its spell; seen from far (a slash runs 240 blocks). */
	public static final EntityType<SpellProjectile> SPELL_PROJECTILE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("spell_projectile"),
			EntityType.Builder.<SpellProjectile>of(SpellProjectile::new, MobCategory.MISC)
					.noSave().noSummon().fireImmune()
					.sized(0.5f, 0.5f)
					.clientTrackingRange(16)
					.updateInterval(1)
					.build("spell_projectile"));

	/** What an element spell leaves standing a while (see {@link SpellField}): seen from far, since some are vast. */
	public static final EntityType<SpellField> SPELL_FIELD = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("spell_field"),
			EntityType.Builder.<SpellField>of(SpellField::new, MobCategory.MISC)
					.noSave().noSummon().fireImmune()
					.sized(0.1f, 0.1f)
					.clientTrackingRange(16)
					.updateInterval(1)
					.build("spell_field"));

	/**
	 * A great spell's living shape (see {@link SpellConstruct}: a spirit beast, a tree, a seal, a vortex of swords): GeckoLib's, moved
	 * by its spell every tick and seen from far, since they are vast.
	 */
	public static final EntityType<SpellConstruct> SPELL_CONSTRUCT = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("spell_construct"),
			EntityType.Builder.<SpellConstruct>of(SpellConstruct::new, MobCategory.MISC)
					.noSave().noSummon().fireImmune()
					.sized(0.1f, 0.1f)
					.clientTrackingRange(16)
					.updateInterval(1)
					.build("spell_construct"));

	/** A block a spell moves before your eyes (see {@link SpellBlock}): it never moves itself, so it is never updated. */
	public static final EntityType<SpellBlock> SPELL_BLOCK = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("spell_block"),
			EntityType.Builder.<SpellBlock>of(SpellBlock::new, MobCategory.MISC)
					.noSave().noSummon().fireImmune()
					.sized(0.98f, 0.98f)
					.clientTrackingRange(8)
					.updateInterval(Integer.MAX_VALUE)
					.build("spell_block"));

	/** An earth golem summoned by its caster (see {@link EarthGolem}); the Earthen Titan is one three times as large. */
	public static final EntityType<EarthGolem> EARTH_GOLEM = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("earth_golem"),
			EntityType.Builder.<EarthGolem>of(EarthGolem::new, MobCategory.MISC)
					.noSave().noSummon()
					.sized(1.2f, 2.6f)
					.clientTrackingRange(10)
					.build("earth_golem"));

	/** The wave of dirt a Dirt Tide rides on (see {@link DirtTide}): steered by its rider. */
	public static final EntityType<DirtTide> DIRT_TIDE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("dirt_tide"),
			EntityType.Builder.<DirtTide>of(DirtTide::new, MobCategory.MISC)
					.noSave().noSummon()
					.sized(1.6f, 0.9f)
					.clientTrackingRange(10)
					.build("dirt_tide"));

	/** A tear in space a Heavenly Being opens (see {@link SpatialTearEntity}): walked through, into the gap or out of it. */
	public static final EntityType<SpatialTearEntity> SPATIAL_TEAR = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("spatial_tear"),
			EntityType.Builder.<SpatialTearEntity>of(SpatialTearEntity::new, MobCategory.MISC)
					.noSave().noSummon().fireImmune()
					.sized(1.4f, 2.8f)
					.clientTrackingRange(10)
					.updateInterval(20)
					.build("spatial_tear"));

	/** A Vestige of the Void, hunting the fliers of open space (see {@link VoidVestige}); seen from afar, its position sent every tick. */
	public static final EntityType<VoidVestige> VOID_VESTIGE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("void_vestige"),
			EntityType.Builder.<VoidVestige>of(VoidVestige::new, MobCategory.MONSTER)
					.fireImmune()
					.sized(2.4f, 1.8f)
					.clientTrackingRange(16)
					.updateInterval(1)
					.build("void_vestige"));

	/** A demon beast's burning stone or jet of water (see {@link BeastShot}): small and quick, like a snowball. */
	public static final EntityType<BeastShot> BEAST_SHOT = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("beast_shot"),
			EntityType.Builder.<BeastShot>of(BeastShot::new, MobCategory.MISC)
					.noSave().noSummon()
					.sized(0.3f, 0.3f)
					.clientTrackingRange(6)
					.updateInterval(5)
					.build("beast_shot"));

	/**
	 * The wild demon beasts (see {@link DemonBeast}), each kind its own type ({@code defying-the-heavens:<kind's id>}), sized as its kind at
	 * its least rank (DemonBeast#getDimensions grows it with its rank).
	 */
	private static final Map<BeastKind, EntityType<DemonBeast>> BEASTS = new EnumMap<>(BeastKind.class);

	static {
		for (BeastKind kind : BeastKind.values()) {
			EntityType<DemonBeast> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, DefyingTheHeavens.id(kind.getId()),
					EntityType.Builder.<DemonBeast>of(DemonBeast::new, MobCategory.MONSTER)
							.sized(kind.width, kind.height)
							.clientTrackingRange(10)
							.build(kind.getId()));
			DemonBeast.bind(type, kind);
			BEASTS.put(kind, type);
		}
	}

	public static EntityType<DemonBeast> beast(BeastKind kind) {
		return BEASTS.get(kind);
	}

	/** Touching this class registers the entity types and their attributes. */
	public static void register() {
		for (EntityType<DemonBeast> beast : BEASTS.values()) FabricDefaultAttributeRegistry.register(beast, DemonBeast.createAttributes());
		FabricDefaultAttributeRegistry.register(VOID_VESTIGE, VoidVestige.createAttributes());
		FabricDefaultAttributeRegistry.register(CULTIVATOR, CultivatorNpc.createAttributes());
		FabricDefaultAttributeRegistry.register(GHOST_CULTIVATOR, CultivatorNpc.createAttributes());
		FabricDefaultAttributeRegistry.register(HUNGRY_GHOST, HungryGhost.createAttributes());
		net.minecraft.world.entity.SpawnPlacements.register(HUNGRY_GHOST, net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, HungryGhost::checkSpawn);
		FabricDefaultAttributeRegistry.register(INNER_BODY, InnerBodyEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(HEART_DEMON, HeartDemonEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SOUL_BODY, SoulBodyEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(CLONE_BODY, CloneBodyEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SOUL_VESSEL, SoulVesselEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(EARTH_GOLEM, EarthGolem.createAttributes());
	}

	private ModEntities() {}
}
