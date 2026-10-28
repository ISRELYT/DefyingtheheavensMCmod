package com.example.defyingtheheavens;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
	public static final BlockEntityType<CultivationFruitBlockEntity> CULTIVATION_FRUIT = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, DefyingTheHeavens.id("cultivation_fruit"),
			BlockEntityType.Builder.of(CultivationFruitBlockEntity::new, ModBlocks.CULTIVATION_FRUIT).build(null));
	/** Carries no data; it exists so the client can draw the rift (either kind) with a block entity renderer. */
	public static final BlockEntityType<SpatialRiftBlockEntity> SPATIAL_RIFT = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("spatial_rift"),
			BlockEntityType.Builder.of(SpatialRiftBlockEntity::new, ModBlocks.SPATIAL_RIFT, ModBlocks.YELLOW_SPRINGS_RIFT).build(null));
	/** The Wheel of Reincarnation's hub; no data, only drawn turning. */
	public static final BlockEntityType<WheelOfReincarnationBlockEntity> WHEEL_OF_REINCARNATION = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("wheel_of_reincarnation"),
			BlockEntityType.Builder.of(WheelOfReincarnationBlockEntity::new, ModBlocks.WHEEL_OF_REINCARNATION).build(null));

	public static final BlockEntityType<SpiritPedestalBlockEntity> SPIRIT_PEDESTAL = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("spirit_pedestal"),
			BlockEntityType.Builder.of(SpiritPedestalBlockEntity::new, ModBlocks.SPIRIT_PEDESTAL).build(null));

	/** A Heaven Altar's offering tray (see HeavenAltarBlockEntity). */
	public static final BlockEntityType<HeavenAltarBlockEntity> HEAVEN_ALTAR = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("heaven_altar"),
			BlockEntityType.Builder.of(HeavenAltarBlockEntity::new, ModBlocks.HEAVEN_ALTAR).build(null));

	/**
	 * A growing herb's age clock; shared by every aging herb (ginseng, lingzhi, huangjing, Spirit Lotus, and the Netherworld's
	 * Other-Shore Flower and Netherworld Lotus).
	 */
	public static final BlockEntityType<GinsengBlockEntity> GINSENG = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("ginseng"),
			BlockEntityType.Builder.of(GinsengBlockEntity::new, ModBlocks.GINSENG, ModBlocks.SPIRIT_GINSENG,
					ModBlocks.LINGZHI, ModBlocks.PURPLE_LINGZHI, ModBlocks.HUANGJING, ModBlocks.OCHRE_HUANGJING, ModBlocks.SPIRIT_LOTUS,
					ModBlocks.OTHER_SHORE_FLOWER, ModBlocks.NETHERWORLD_LOTUS)
					.build(null));

	/** A Spirit Peach Tree's heart: its age, its fruit, the qi poured into it. */
	public static final BlockEntityType<SpiritPeachHeartBlockEntity> SPIRIT_PEACH_HEART = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("spirit_peach_heart"),
			BlockEntityType.Builder.of(SpiritPeachHeartBlockEntity::new, ModBlocks.SPIRIT_PEACH_HEART).build(null));

	/** The Alchemy Cauldron's ingredients and brew. */
	public static final BlockEntityType<AlchemyCauldronBlockEntity> ALCHEMY_CAULDRON = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("alchemy_cauldron"),
			BlockEntityType.Builder.of(AlchemyCauldronBlockEntity::new, ModBlocks.ALCHEMY_CAULDRON).build(null));

	/** The Artifact Forge's materials and working (see ArtifactForgeBlockEntity). */
	public static final BlockEntityType<ArtifactForgeBlockEntity> ARTIFACT_FORGE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("artifact_forge"),
			BlockEntityType.Builder.of(ArtifactForgeBlockEntity::new, ModBlocks.ARTIFACT_FORGE).build(null));

	/** A Formation Core's battery, radius and owner (see FormationCoreBlockEntity). */
	public static final BlockEntityType<FormationCoreBlockEntity> FORMATION_CORE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("formation_core"),
			BlockEntityType.Builder.of(FormationCoreBlockEntity::new, ModBlocks.FORMATION_CORE).build(null));

	/** A storage formation's space, owner, trust and seal (StorageFormationBlockEntity): every tier's block. */
	public static final BlockEntityType<StorageFormationBlockEntity> STORAGE_FORMATION = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("storage_formation"),
			BlockEntityType.Builder.of(StorageFormationBlockEntity::new, java.util.Arrays.stream(StorageFormations.Tier.values())
					.map(ModBlocks::storageFormation).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

	/** A seal of qi ink; no data, only drawn (under Qi Sense) by its renderer. */
	public static final BlockEntityType<SealBlockEntity> SEAL = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("seal"),
			BlockEntityType.Builder.of(SealBlockEntity::new, ModBlocks.SEAL).build(null));

	/** A small world's gate (SmallWorldGateBlock); no data, only drawn by its renderer. */
	public static final BlockEntityType<SmallWorldGateBlockEntity> SMALL_WORLD_GATE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("small_world_gate"),
			BlockEntityType.Builder.of(SmallWorldGateBlockEntity::new, ModBlocks.SMALL_WORLD_GATE).build(null));

	/** A legend's stone's sword and the tomb's trial (see LegendStoneBlockEntity). */
	public static final BlockEntityType<LegendStoneBlockEntity> LEGEND_STONE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("legend_stone"),
			BlockEntityType.Builder.of(LegendStoneBlockEntity::new, ModBlocks.LEGEND_STONE).build(null));

	/** Touching this class registers the block entity types. */
	public static void register() {
	}

	private ModBlockEntities() {}
}
