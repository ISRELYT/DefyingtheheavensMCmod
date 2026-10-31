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

	/** A small world's door of starlight (see {@link SmallWorldDoor}, SmallWorlds): never saved, seen from afar. */
	public static final EntityType<SmallWorldDoor> SMALL_WORLD_DOOR = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("small_world_door"),
			EntityType.Builder.<SmallWorldDoor>of(SmallWorldDoor::new, MobCategory.MISC)
					.noSave().noSummon().fireImmune()
					.sized(SmallWorldDoor.WIDTH, SmallWorldDoor.HEIGHT)
					.clientTrackingRange(10)
					.updateInterval(20)
					.build("small_world_door"));

	/** A sword spirit (see {@link SwordSpirit}): a tomb's old blade risen, or a legend's called by its art. Never saved. */
	public static final EntityType<SwordSpirit> SWORD_SPIRIT = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("sword_spirit"),
			EntityType.Builder.<SwordSpirit>of(SwordSpirit::new, MobCategory.MISC)
					.noSave().noSummon().fireImmune()
					.sized(0.6f, 0.6f)
					.clientTrackingRange(8)
					.build("sword_spirit"));

	/** A Vine Keeper of the Verdant Herb Valley (see {@link RealmGuardian}): lying by its herb bed till it wakes. */
	public static final EntityType<RealmGuardian> VINE_KEEPER = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("vine_keeper"),
			EntityType.Builder.<RealmGuardian>of(RealmGuardian::new, MobCategory.MONSTER)
					.sized(1.1f, 2.3f)
					.clientTrackingRange(10)
					.build("vine_keeper"));

	/** A Bronze Puppet of a secret realm's ruins, or the Gatekeeper before the Pill Sage's hall (see {@link RealmGuardian}). */
	public static final EntityType<RealmGuardian> BRONZE_PUPPET = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("bronze_puppet"),
			EntityType.Builder.<RealmGuardian>of(RealmGuardian::new, MobCategory.MONSTER)
					.fireImmune()
					.sized(1.0f, 2.4f)
					.clientTrackingRange(10)
					.build("bronze_puppet"));

	/** A Sword Wraith of the Sunken Sword Tomb, or the Sleeping Sword's spirit (see {@link RealmGuardian}): a fallen sword's spirit. */
	public static final EntityType<RealmGuardian> SWORD_WRAITH = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("sword_wraith"),
			EntityType.Builder.<RealmGuardian>of(RealmGuardian::new, MobCategory.MONSTER)
					.fireImmune()
					.sized(0.7f, 2.0f)
					.clientTrackingRange(10)
					.build("sword_wraith"));

	/** A Bone Soldier of the Ancient Battlefield (see {@link RealmGuardian}): an old soldier risen in the armour it died in. */
	public static final EntityType<RealmGuardian> BONE_SOLDIER = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("bone_soldier"),
			EntityType.Builder.<RealmGuardian>of(RealmGuardian::new, MobCategory.MONSTER)
					.sized(0.7f, 2.1f)
					.clientTrackingRange(10)
					.build("bone_soldier"));

	/** A Battle Spirit come up the General's hill (see {@link RealmGuardian}): a soldier of the old war in light. */
	public static final EntityType<RealmGuardian> BATTLE_SPIRIT = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("battle_spirit"),
			EntityType.Builder.<RealmGuardian>of(RealmGuardian::new, MobCategory.MONSTER)
					.fireImmune()
					.sized(0.7f, 2.1f)
					.clientTrackingRange(10)
					.build("battle_spirit"));

	/** The General's Spirit, the last to come up his hill (see {@link RealmGuardian}). */
	public static final EntityType<RealmGuardian> GENERAL_SPIRIT = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("general_spirit"),
			EntityType.Builder.<RealmGuardian>of(RealmGuardian::new, MobCategory.MONSTER)
					.fireImmune()
					.sized(0.95f, 2.8f)
					.clientTrackingRange(10)
					.build("general_spirit"));

	/** The Dragon's Echo of the Primeval Beast Graveyard (see {@link RealmGuardian}): the dead dragon risen as a shade, its trial's guardian. */
	public static final EntityType<RealmGuardian> DRAGON_ECHO = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("dragon_echo"),
			EntityType.Builder.<RealmGuardian>of(RealmGuardian::new, MobCategory.MONSTER)
					.fireImmune()
					.sized(1.6f, 1.8f)
					.clientTrackingRange(10)
					.build("dragon_echo"));

	/** The master's own puppet of the Cave Abode (see {@link RealmGuardian}): white jade and gold, its last trial's guardian. */
	public static final EntityType<RealmGuardian> MASTER_PUPPET = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("master_puppet"),
			EntityType.Builder.<RealmGuardian>of(RealmGuardian::new, MobCategory.MONSTER)
					.fireImmune()
					.sized(1.1f, 2.7f)
					.clientTrackingRange(10)
					.build("master_puppet"));

	/** A Drowned Shade of the Yellow Springs Hollow (see {@link RealmGuardian}): a soldier of the drowned city's garrison, risen in the springs' light. */
	public static final EntityType<RealmGuardian> DROWNED_SHADE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("drowned_shade"),
			EntityType.Builder.<RealmGuardian>of(RealmGuardian::new, MobCategory.MONSTER)
					.fireImmune()
					.sized(0.7f, 2.1f)
					.clientTrackingRange(10)
					.build("drowned_shade"));

	/** The Ferryman of the Yellow Springs Hollow (see {@link RealmGuardian}): at his landing, its trial's judge. */
	public static final EntityType<RealmGuardian> FERRYMAN = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("ferryman"),
			EntityType.Builder.<RealmGuardian>of(RealmGuardian::new, MobCategory.MONSTER)
					.fireImmune()
					.sized(0.9f, 2.2f)
					.clientTrackingRange(10)
					.build("ferryman"));

	/** A Mirror Shade of the Jade Lake Mirage (see {@link RealmGuardian}): a soldier of the lake's old sect, risen in the mirror's glassy light. */
	public static final EntityType<RealmGuardian> MIRROR_SHADE = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("mirror_shade"),
			EntityType.Builder.<RealmGuardian>of(RealmGuardian::new, MobCategory.MONSTER)
					.fireImmune()
					.sized(0.7f, 2.1f)
					.clientTrackingRange(10)
					.build("mirror_shade"));

	/** A reflection out of the Jade Lake Mirage's mirror (see {@link MirrorReflection}): a copy of its owner, never saved. */
	public static final EntityType<MirrorReflection> MIRROR_REFLECTION = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("mirror_reflection"),
			EntityType.Builder.<MirrorReflection>of(MirrorReflection::new, MobCategory.MISC)
					.fireImmune()
					.sized(0.6f, 1.8f)
					.clientTrackingRange(10)
					.build("mirror_reflection"));

	/** A remnant guard of the Ruins of a Fallen Holy Land (see {@link RealmGuardian}): its disciples' last watch, in white and gold. */
	public static final EntityType<RealmGuardian> HOLY_REMNANT = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("holy_remnant"),
			EntityType.Builder.<RealmGuardian>of(RealmGuardian::new, MobCategory.MONSTER)
					.fireImmune()
					.sized(0.7f, 2.1f)
					.clientTrackingRange(10)
					.build("holy_remnant"));

	/** A decayed celestial guard of the Tomb of a Fallen Immortal (see {@link RealmGuardian}): its gold gone to rot with its lord. */
	public static final EntityType<RealmGuardian> DECAYED_GUARD = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("decayed_guard"),
			EntityType.Builder.<RealmGuardian>of(RealmGuardian::new, MobCategory.MONSTER)
					.fireImmune()
					.sized(0.7f, 2.1f)
					.clientTrackingRange(10)
					.build("decayed_guard"));

	/** The Tomb of a Fallen Immortal's lord's decayed spirit (see {@link RealmGuardian}): before his sanctum, its trial's. */
	public static final EntityType<RealmGuardian> DECAYED_IMMORTAL = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("decayed_immortal"),
			EntityType.Builder.<RealmGuardian>of(RealmGuardian::new, MobCategory.MONSTER)
					.fireImmune()
					.sized(0.9f, 2.9f)
					.clientTrackingRange(10)
					.build("decayed_immortal"));

	/** A soldier of the old army the War General's Banner calls up (see {@link ArmySpirit}): its bearer's for a while, never saved. */
	public static final EntityType<ArmySpirit> ARMY_SPIRIT = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("army_spirit"),
			EntityType.Builder.<ArmySpirit>of(ArmySpirit::new, MobCategory.MISC)
					.noSave().noSummon().fireImmune()
					.sized(0.7f, 2.1f)
					.clientTrackingRange(10)
					.build("army_spirit"));

	/** A legend made vast for a moment by its art (see {@link Colossus}): seen from far, never saved. */
	public static final EntityType<Colossus> COLOSSUS = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			DefyingTheHeavens.id("colossus"),
			EntityType.Builder.<Colossus>of(Colossus::new, MobCategory.MISC)
					.noSave().noSummon().fireImmune()
					.sized(1.0f, 1.0f)
					.clientTrackingRange(16)
					.updateInterval(Integer.MAX_VALUE)
					.build("colossus"));

	/**
	 * The wild demon beasts (see {@link DemonBeast}), each kind its own type ({@code defying-the-heavens:<kind's id>}), sized as its kind at
	 * its least rank (DemonBeast#getDimensions grows it with its rank). The primordial beasts are their own (see {@link PrimordialBeast}),
	 * seen from further off (vast, high in the air) and moved the more smoothly; and the Chaos Fiends ({@link ChaosFiend}).
	 */
	private static final Map<BeastKind, EntityType<DemonBeast>> BEASTS = new EnumMap<>(BeastKind.class);

	static {
		for (BeastKind kind : BeastKind.values()) {
			EntityType.EntityFactory<DemonBeast> make = kind.isPrimordial() ? PrimordialBeast::new : kind == BeastKind.CHAOS_FIEND ? ChaosFiend::new : DemonBeast::new;
			EntityType<DemonBeast> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, DefyingTheHeavens.id(kind.getId()),
					EntityType.Builder.<DemonBeast>of(make, MobCategory.MONSTER)
							.sized(kind.width, kind.height)
							.clientTrackingRange(kind.isPrimordial() ? 16 : 10)
							.updateInterval(kind.isPrimordial() ? 2 : 3)
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
		FabricDefaultAttributeRegistry.register(SWORD_SPIRIT, SwordSpirit.createAttributes());
		FabricDefaultAttributeRegistry.register(VINE_KEEPER, RealmGuardian.createAttributes());
		FabricDefaultAttributeRegistry.register(BRONZE_PUPPET, RealmGuardian.createAttributes());
		FabricDefaultAttributeRegistry.register(SWORD_WRAITH, RealmGuardian.createAttributes());
		FabricDefaultAttributeRegistry.register(BONE_SOLDIER, RealmGuardian.createAttributes());
		FabricDefaultAttributeRegistry.register(BATTLE_SPIRIT, RealmGuardian.createAttributes());
		FabricDefaultAttributeRegistry.register(GENERAL_SPIRIT, RealmGuardian.createAttributes());
		FabricDefaultAttributeRegistry.register(ARMY_SPIRIT, ArmySpirit.createAttributes());
		FabricDefaultAttributeRegistry.register(DRAGON_ECHO, RealmGuardian.createAttributes());
		FabricDefaultAttributeRegistry.register(MASTER_PUPPET, RealmGuardian.createAttributes());
		FabricDefaultAttributeRegistry.register(DROWNED_SHADE, RealmGuardian.createAttributes());
		FabricDefaultAttributeRegistry.register(FERRYMAN, RealmGuardian.createAttributes());
		FabricDefaultAttributeRegistry.register(MIRROR_SHADE, RealmGuardian.createAttributes());
		FabricDefaultAttributeRegistry.register(MIRROR_REFLECTION, CultivatorNpc.createAttributes());
		FabricDefaultAttributeRegistry.register(HOLY_REMNANT, RealmGuardian.createAttributes());
		FabricDefaultAttributeRegistry.register(DECAYED_GUARD, RealmGuardian.createAttributes());
		FabricDefaultAttributeRegistry.register(DECAYED_IMMORTAL, RealmGuardian.createAttributes());
	}

	private ModEntities() {}
}
