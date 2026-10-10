package com.example.defyingtheheavens;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
	public static final BlockEntityType<CultivationFruitBlockEntity> CULTIVATION_FRUIT = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, DefyingTheHeavens.id("cultivation_fruit"),
			BlockEntityType.Builder.of(CultivationFruitBlockEntity::new, ModBlocks.CULTIVATION_FRUIT).build(null));
	/** Carries no data; it exists so the client can draw the rift with a block entity renderer. */
	public static final BlockEntityType<SpatialRiftBlockEntity> SPATIAL_RIFT = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("spatial_rift"),
			BlockEntityType.Builder.of(SpatialRiftBlockEntity::new, ModBlocks.SPATIAL_RIFT).build(null));

	public static final BlockEntityType<SpiritPedestalBlockEntity> SPIRIT_PEDESTAL = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("spirit_pedestal"),
			BlockEntityType.Builder.of(SpiritPedestalBlockEntity::new, ModBlocks.SPIRIT_PEDESTAL).build(null));

	/** A growing herb's age clock; shared by every aging herb (ginseng, lingzhi, huangjing, Spirit Lotus). */
	public static final BlockEntityType<GinsengBlockEntity> GINSENG = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("ginseng"),
			BlockEntityType.Builder.of(GinsengBlockEntity::new, ModBlocks.GINSENG, ModBlocks.SPIRIT_GINSENG,
					ModBlocks.LINGZHI, ModBlocks.PURPLE_LINGZHI, ModBlocks.HUANGJING, ModBlocks.OCHRE_HUANGJING, ModBlocks.SPIRIT_LOTUS)
					.build(null));

	/** A Spirit Peach Tree's heart: its age, its fruit, the qi poured into it. */
	public static final BlockEntityType<SpiritPeachHeartBlockEntity> SPIRIT_PEACH_HEART = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("spirit_peach_heart"),
			BlockEntityType.Builder.of(SpiritPeachHeartBlockEntity::new, ModBlocks.SPIRIT_PEACH_HEART).build(null));

	/** The Alchemy Cauldron's ingredients and brew. */
	public static final BlockEntityType<AlchemyCauldronBlockEntity> ALCHEMY_CAULDRON = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("alchemy_cauldron"),
			BlockEntityType.Builder.of(AlchemyCauldronBlockEntity::new, ModBlocks.ALCHEMY_CAULDRON).build(null));

	/** A Formation Core's battery, radius and owner (see FormationCoreBlockEntity). */
	public static final BlockEntityType<FormationCoreBlockEntity> FORMATION_CORE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("formation_core"),
			BlockEntityType.Builder.of(FormationCoreBlockEntity::new, ModBlocks.FORMATION_CORE).build(null));

	/** A seal of qi ink; no data, only drawn (under Qi Sense) by its renderer. */
	public static final BlockEntityType<SealBlockEntity> SEAL = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			DefyingTheHeavens.id("seal"),
			BlockEntityType.Builder.of(SealBlockEntity::new, ModBlocks.SEAL).build(null));

	/** Touching this class registers the block entity types. */
	public static void register() {
	}

	private ModBlockEntities() {}
}
