package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
	public static final Block CULTIVATION_FRUIT = register("cultivation_fruit", new CultivationFruitBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).noCollission().noOcclusion()
					.instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY)));
	/**
	 * The light in the Immortal Gate's opening and the Southern Heavenly Gate's while the Gate stands open (ImmortalGate, phase 2 of
	 * the realm plan, 2026-10-25). Unbreakable, walk-through, no item, as bright as light gets.
	 */
	public static final Block IMMORTAL_GATE_LIGHT = register("immortal_gate_light", new ImmortalGateLightBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.GOLD)
			.noCollission()
			.noOcclusion()
			.strength(-1.0f, 3600000.0f)
			.noLootTable()
			.lightLevel(state -> 15)
			.pushReaction(PushReaction.BLOCK)
			.isValidSpawn((state, level, pos, type) -> false)
			.isRedstoneConductor((state, level, pos) -> false)
			.isSuffocating((state, level, pos) -> false)
			.isViewBlocking((state, level, pos) -> false)
			.sound(SoundType.AMETHYST)));
	/** The tear at Overworld X=0, Z=0. Unbreakable, walk-through, no item. */
	public static final Block SPATIAL_RIFT = register("spatial_rift", new SpatialRiftBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_BLACK)
			.noCollission()
			.strength(-1.0f, 3600000.0f)
			.noLootTable()
			.lightLevel(state -> 12)
			.pushReaction(PushReaction.BLOCK)
			.sound(SoundType.AMETHYST)));

	/**
	 * The Gate of the Yellow Springs: the rift at Nether 0, 0 that leads up the Soul River to the Netherworld, and the way down
	 * on the Ghost Gate's plaza (see SpatialRiftBlock#leads; drawn gold and cyan). Unbreakable, walk-through, no item.
	 */
	public static final Block YELLOW_SPRINGS_RIFT = register("yellow_springs_rift", new SpatialRiftBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.GOLD)
			.noCollission()
			.strength(-1.0f, 3600000.0f)
			.noLootTable()
			.lightLevel(state -> 12)
			.pushReaction(PushReaction.BLOCK)
			.sound(SoundType.AMETHYST)));
	/** The hub of the Wheel of Reincarnation by the Ghost Gate (see WheelOfReincarnationBlock). Unbreakable, no item. */
	public static final Block WHEEL_OF_REINCARNATION = register("wheel_of_reincarnation", new WheelOfReincarnationBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_BLACK)
			.strength(-1.0f, 3600000.0f)
			.noLootTable()
			.noOcclusion()
			.lightLevel(state -> 10)
			.pushReaction(PushReaction.BLOCK)
			.sound(SoundType.POLISHED_DEEPSLATE)));
	/** A Method Stele, three blocks tall, teaching a ghost method (see MethodSteleBlock). Unbreakable, no item; its emblem glows. */
	public static final Block METHOD_STELE = register("method_stele", new MethodSteleBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_BLACK)
			.strength(-1.0f, 3600000.0f)
			.noLootTable()
			.noOcclusion()
			.lightLevel(state -> state.getValue(MethodSteleBlock.PART) == MethodSteleBlock.Part.MIDDLE ? 6 : 0)
			.pushReaction(PushReaction.BLOCK)
			.sound(SoundType.POLISHED_DEEPSLATE)));
	/** A legend's stone, a legendary sword resting in it (see LegendStoneBlock, Legends). Unbreakable, no item. */
	public static final Block LEGEND_STONE = register("legend_stone", new LegendStoneBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(-1.0f, 3600000.0f)
			.noLootTable()
			.noOcclusion()
			.pushReaction(PushReaction.BLOCK)
			.sound(SoundType.STONE)));
	/** An old blade standing in the ground of Ganjiang's sword tomb (see BuriedBladeBlock, LegendSites). No item, nothing dropped. */
	public static final Block BURIED_BLADE = register("buried_blade", new BuriedBladeBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(1.5f, 6.0f)
			.noLootTable()
			.noOcclusion()
			.noCollission()
			.pushReaction(PushReaction.DESTROY)
			.sound(SoundType.CHAIN)));
	/**
	 * The Sea-Fixing Needle standing as a pillar over the Immortal Realm's sea of clouds (see SeaPillarBlock, Legends): black iron, its bands
	 * of gold glowing a little. Unbreakable, no item.
	 */
	public static final Block SEA_PILLAR = register("sea_pillar", new SeaPillarBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_BLACK)
			.strength(-1.0f, 3600000.0f)
			.noLootTable()
			.lightLevel(state -> state.getValue(SeaPillarBlock.BANDED) ? 7 : 0)
			.pushReaction(PushReaction.BLOCK)
			.sound(SoundType.NETHERITE_BLOCK)));
	/** A Dao Stele, three blocks tall, teaching a heaven method (see DaoSteleBlock). Unbreakable, no item; its element's emblem glows. */
	public static final Block DAO_STELE = register("dao_stele", new DaoSteleBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.QUARTZ)
			.strength(-1.0f, 3600000.0f)
			.noLootTable()
			.noOcclusion()
			.lightLevel(state -> state.getValue(MethodSteleBlock.PART) == MethodSteleBlock.Part.MIDDLE ? 7 : 0)
			.pushReaction(PushReaction.BLOCK)
			.sound(SoundType.STONE)));

	/** The Inner Realm's see-through floor, lit by a soul's steps (see SoulCrystalBlock). Unbreakable, no item. */
	public static final Block SOUL_CRYSTAL = register("soul_crystal", new SoulCrystalBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_LIGHT_BLUE)
			.strength(-1.0f, 3600000.0f)
			.noLootTable()
			.noOcclusion()
			.sound(SoundType.AMETHYST)
			.lightLevel(SoulCrystalBlock::light)
			.isValidSpawn((state, level, pos, type) -> false)
			.isRedstoneConductor((state, level, pos) -> false)
			.isSuffocating((state, level, pos) -> false)
			.isViewBlocking((state, level, pos) -> false)
			.pushReaction(PushReaction.BLOCK)));

	/** Jade-tinged stone of the Dense Qi Peaks. Counts as ore-replaceable stone, so Upper Realm ores spawn in it. */
	public static final Block JADE_STONE = registerWithItem("jade_stone",
			new Block(BlockBehaviour.Properties.copy(Blocks.STONE).mapColor(MapColor.COLOR_LIGHT_GREEN)));

	/**
	 * Jade Ore: drops Jade (more with Fortune) and a little experience, like emerald ore, and needs an iron pickaxe. Found
	 * in Overworld mountains and all through the Upper Realm's islands (see ModPlacedFeatures), the deepslate kind deep down.
	 */
	public static final Block JADE_ORE = registerWithItem("jade_ore",
			new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.EMERALD_ORE), UniformInt.of(3, 7)));
	public static final Block DEEPSLATE_JADE_ORE = registerWithItem("deepslate_jade_ore",
			new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_EMERALD_ORE), UniformInt.of(3, 7)));

	/**
	 * Spirit stone veins: Low spirit stones set in stone like pearls in an oyster, a few Mid ones deep in the deepslate, and in
	 * the Upper Realm the rich veins that give Mid, High and, rarely, Supreme and Immortal stones (see SpiritStoneItem; the
	 * drops are in their loot tables). An iron pickaxe mines them.
	 */
	public static final Block SPIRIT_STONE_ORE = registerWithItem("spirit_stone_ore",
			new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.IRON_ORE).mapColor(MapColor.QUARTZ), UniformInt.of(1, 3)));
	public static final Block DEEPSLATE_SPIRIT_STONE_ORE = registerWithItem("deepslate_spirit_stone_ore",
			new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_IRON_ORE).mapColor(MapColor.QUARTZ), UniformInt.of(2, 4)));
	public static final Block RICH_SPIRIT_STONE_ORE = registerWithItem("rich_spirit_stone_ore",
			new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DIAMOND_ORE).mapColor(MapColor.QUARTZ)
					.lightLevel(state -> 3), UniformInt.of(4, 8)));

	/** The white canopy of the Peach Blossom Sanctuary's pale trees. */
	public static final Block WHITE_BLOSSOM_LEAVES = registerWithItem("white_blossom_leaves",
			new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.CHERRY_LEAVES).mapColor(MapColor.SNOW)));

	/** A woven straw cushion to sit and meditate on (see MeditationMatBlock). */
	public static final Block MEDITATION_MAT = registerWithItem("meditation_mat", new MeditationMatBlock(1.5,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.5f).sound(SoundType.GRASS)
					.noOcclusion().ignitedByLava().pushReaction(PushReaction.DESTROY)));
	/** The same cushion with a square of red silk laid across it. */
	public static final Block RED_MEDITATION_MAT = registerWithItem("red_meditation_mat", new MeditationMatBlock(2.0,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.5f).sound(SoundType.WOOL)
					.noOcclusion().ignitedByLava().pushReaction(PushReaction.DESTROY)));
	/** Displays a Cultivation Fruit; fruit on pedestals near a meditator speeds their cultivation (see SpiritPedestalBlock). */
	public static final Block SPIRIT_PEDESTAL = registerWithItem("spirit_pedestal", new SpiritPedestalBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(1.5f, 6.0f).sound(SoundType.POLISHED_DEEPSLATE)
					.noOcclusion()));

	/** Where the Ritual to the Heavens is performed: an offering table with an incense burner (see HeavenAltarBlock). */
	public static final Block HEAVEN_ALTAR = registerWithItem("heaven_altar", new HeavenAltarBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).requiresCorrectToolForDrops().strength(2.0f, 6.0f)
					.sound(SoundType.POLISHED_DEEPSLATE).lightLevel(state -> 4).noOcclusion()));

	/** Ginseng, aging in the ground (see GinsengBlock). Planted from its seeds; dug up as the ginseng root, keeping its age. */
	public static final Block GINSENG = register("ginseng", new GinsengBlock(() -> ModItems.GINSENG, () -> ModItems.GINSENG_SEEDS,
			ginsengProperties(MapColor.PLANT)));
	/** The rarer Spirit Ginseng: pale jade leaves, golden berries. */
	public static final Block SPIRIT_GINSENG = register("spirit_ginseng", new GinsengBlock(() -> ModItems.SPIRIT_GINSENG,
			() -> ModItems.SPIRIT_GINSENG_SEEDS, ginsengProperties(MapColor.COLOR_LIGHT_GREEN)));

	/**
	 * More aging herbs for alchemy, each with a rarer variant (see GinsengBlock): Lingzhi (a lacquered red shelf fungus) and
	 * Purple Lingzhi; Huangjing (Solomon's seal) and Ochre Huangjing. Like ginseng, dug up as items that keep their age.
	 */
	public static final Block LINGZHI = register("lingzhi", new GinsengBlock(() -> ModItems.LINGZHI, () -> ModItems.LINGZHI_SPORES,
			ginsengProperties(MapColor.COLOR_RED)));
	public static final Block PURPLE_LINGZHI = register("purple_lingzhi", new GinsengBlock(() -> ModItems.PURPLE_LINGZHI,
			() -> ModItems.PURPLE_LINGZHI_SPORES, ginsengProperties(MapColor.COLOR_PURPLE)));
	public static final Block HUANGJING = register("huangjing", new GinsengBlock(() -> ModItems.HUANGJING, () -> ModItems.HUANGJING_SEEDS,
			ginsengProperties(MapColor.PLANT)));
	public static final Block OCHRE_HUANGJING = register("ochre_huangjing", new GinsengBlock(() -> ModItems.OCHRE_HUANGJING,
			() -> ModItems.OCHRE_HUANGJING_SEEDS, ginsengProperties(MapColor.COLOR_ORANGE)));
	/** The Spirit Lotus: an aging herb that floats on still water (see SpiritLotusBlock). */
	public static final Block SPIRIT_LOTUS = register("spirit_lotus", new SpiritLotusBlock(() -> ModItems.SPIRIT_LOTUS,
			() -> ModItems.SPIRIT_LOTUS_SEEDS, ginsengProperties(MapColor.COLOR_YELLOW)));
	/**
	 * The Netherworld's aging herbs: the Other-Shore Flower (彼岸花, the red spider lily as a herb, on soul soil and soul sand;
	 * see OtherShoreFlowerBlock), and the Netherworld Lotus (冥莲), pale cyan-white on the Yellow Springs' still water.
	 */
	public static final Block OTHER_SHORE_FLOWER = register("other_shore_flower", new OtherShoreFlowerBlock(() -> ModItems.OTHER_SHORE_FLOWER,
			() -> ModItems.OTHER_SHORE_FLOWER_SEEDS, ginsengProperties(MapColor.COLOR_RED).lightLevel(state -> 3)));
	public static final Block NETHERWORLD_LOTUS = register("netherworld_lotus", new SpiritLotusBlock(() -> ModItems.NETHERWORLD_LOTUS,
			() -> ModItems.NETHERWORLD_LOTUS_SEEDS, ginsengProperties(MapColor.COLOR_LIGHT_BLUE).lightLevel(state -> 5)));
	/**
	 * Yin Jade Ore: dark teal jade in the Netherworld's deepslate and blackstone (common in the Nine Nether Peaks) and, very
	 * rarely, a Soul Sand Valley's netherrack; drops Yin Jade like Jade Ore, and needs an iron pickaxe. Yin Jade Block: nine of it.
	 */
	public static final Block YIN_JADE_ORE = registerWithItem("yin_jade_ore",
			new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_EMERALD_ORE).mapColor(MapColor.WARPED_STEM).lightLevel(state -> 2),
					UniformInt.of(3, 7)));
	public static final Block YIN_JADE_BLOCK = registerWithItem("yin_jade_block",
			new Block(BlockBehaviour.Properties.copy(Blocks.EMERALD_BLOCK).mapColor(MapColor.WARPED_STEM)));
	/**
	 * The Chaos Frontier's (phase 3c): Chaos Origin Ore, chaos from before heaven and earth set hard in its shards' tuff and deepslate, a dim
	 * violet light in it; a diamond pickaxe or better mines it, for Chaos Origin Stones (more with Fortune). The First Flame of its spring
	 * (see FirstFlameBlock, FirstFlame): taken by no one, burning white-gold.
	 */
	public static final Block CHAOS_ORIGIN_ORE = registerWithItem("chaos_origin_ore",
			new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_DIAMOND_ORE).mapColor(MapColor.COLOR_PURPLE).strength(6.0f, 9.0f)
					.lightLevel(state -> 4), UniformInt.of(6, 12)));
	public static final Block FIRST_FLAME = registerWithItem("first_flame", new FirstFlameBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD)
			.noCollission().noLootTable().strength(-1.0f, 3600000.0f).lightLevel(state -> 15).sound(SoundType.WOOL)
			.isValidSpawn((state, level, pos, type) -> false).pushReaction(PushReaction.BLOCK)));
	/** A small world's gate (phase 3d, see SmallWorldGateBlock, SmallWorlds): its way out, a sheet of starlight; taken by no one, no item. */
	public static final Block SMALL_WORLD_GATE = register("small_world_gate", new SmallWorldGateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
			.noCollission().noLootTable().strength(-1.0f, 3600000.0f).lightLevel(state -> 11).sound(SoundType.AMETHYST)
			.isValidSpawn((state, level, pos, type) -> false).pushReaction(PushReaction.BLOCK).noOcclusion()));
	/** The red spider lily of the Fields of the Other Shore (see SpiderLilyBlock): a plain flower, on soul soil and earth. */
	public static final Block RED_SPIDER_LILY = registerWithItem("red_spider_lily", new SpiderLilyBlock(
			BlockBehaviour.Properties.copy(Blocks.POPPY).mapColor(MapColor.COLOR_RED)));
	/** Grass that gathers Spirit Dew (see SpiritDewGrassBlock). */
	public static final Block SPIRIT_DEW_GRASS = registerWithItem("spirit_dew_grass", new SpiritDewGrassBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).noCollission().instabreak().sound(SoundType.GRASS)
					.pushReaction(PushReaction.DESTROY).randomTicks()));

	/** Spirit Peach Tree (see SpiritPeachTree): the sapling a peach pit grows into, its leaves, and the heart at its base. */
	public static final Block SPIRIT_PEACH_SAPLING = register("spirit_peach_sapling",
			new SpiritPeachSaplingBlock(BlockBehaviour.Properties.copy(Blocks.CHERRY_SAPLING)));
	public static final Block SPIRIT_PEACH_LEAVES = registerWithItem("spirit_peach_leaves",
			new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.FLOWERING_AZALEA_LEAVES)));
	public static final Block SPIRIT_PEACH_HEART = register("spirit_peach_heart",
			// Its own properties, not a copy of the cherry log's: a log's map colour reads its axis, which the heart doesn't have.
			new SpiritPeachHeartBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GRAY).instrument(NoteBlockInstrument.BASS)
					.strength(2.0f).sound(SoundType.CHERRY_WOOD).ignitedByLava()));

	/** Blue Spirit Wood: spruce-dark timber with blue qi pulsing up its veins (the glow is an animated texture). */
	public static final Block BLUE_SPIRIT_LOG = registerWithItem("blue_spirit_log",
			new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.SPRUCE_LOG).mapColor(MapColor.PODZOL)));
	public static final Block BLUE_SPIRIT_WOOD = registerWithItem("blue_spirit_wood",
			new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.SPRUCE_WOOD).mapColor(MapColor.PODZOL)));
	public static final Block BLUE_SPIRIT_PLANKS = registerWithItem("blue_spirit_planks",
			new Block(BlockBehaviour.Properties.copy(Blocks.SPRUCE_PLANKS).mapColor(MapColor.PODZOL)));

	/** Where pills are brewed: water, a fire beneath, then the ingredients (see AlchemyCauldronBlock). */
	public static final Block ALCHEMY_CAULDRON = registerWithItem("alchemy_cauldron",
			new AlchemyCauldronBlock(BlockBehaviour.Properties.copy(Blocks.CAULDRON).mapColor(MapColor.TERRACOTTA_ORANGE)));
	/** Where artifacts are forged: a fire beneath, the materials, the forger's qi (see ArtifactForgeBlock). */
	public static final Block ARTIFACT_FORGE = registerWithItem("artifact_forge", new ArtifactForgeBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.TERRACOTTA_ORANGE).requiresCorrectToolForDrops().strength(3.5f).sound(SoundType.METAL).noOcclusion()
			.lightLevel(state -> state.getValue(ArtifactForgeBlock.WORKING) ? 11 : 0)));

	/**
	 * A sect's mission board (MissionBoardBlock, SectMissions): set up by the sect on its plaza, never crafted (its item is the creative menu's);
	 * nothing breaks it, and it drops nothing.
	 */
	public static final Block MISSION_BOARD = registerWithItem("mission_board", new MissionBoardBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD).strength(-1.0f, 3_600_000.0f).noLootTable().noOcclusion().sound(SoundType.WOOD).pushReaction(PushReaction.BLOCK)
			.isValidSpawn((state, level, pos, type) -> false)));

	/** Stone threaded with condensed qi, as rare as diamond: a formation's power (see QiVeinBlock). Needs a diamond pickaxe. */
	public static final Block QI_VEIN = registerWithItem("qi_vein", new QiVeinBlock(BlockBehaviour.Properties.copy(Blocks.DIAMOND_ORE)
			.mapColor(MapColor.COLOR_LIGHT_BLUE)));
	public static final Block DEEPSLATE_QI_VEIN = registerWithItem("deepslate_qi_vein", new QiVeinBlock(BlockBehaviour.Properties
			.copy(Blocks.DEEPSLATE_DIAMOND_ORE).mapColor(MapColor.COLOR_LIGHT_BLUE)));
	/** The heart of a protective formation (see FormationCoreBlock). */
	public static final Block FORMATION_CORE = registerWithItem("formation_core", new FormationCoreBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_CYAN).requiresCorrectToolForDrops().strength(30.0f, 1200.0f).sound(SoundType.AMETHYST)
			.lightLevel(state -> 7).noOcclusion()));
	/** One cell of a formation's barrier (see SectBarrierBlock): invisible, unpushable, broken only by the strong. No item. */
	public static final Block SECT_BARRIER = register("sect_barrier", new SectBarrierBlock(BlockBehaviour.Properties.of()
			.strength(-1.0f, 3600000.0f).noLootTable().noOcclusion().isValidSpawn((state, level, pos, type) -> false)
			.isRedstoneConductor((state, level, pos) -> false).isSuffocating((state, level, pos) -> false)
			.isViewBlocking((state, level, pos) -> false).pushReaction(PushReaction.BLOCK).sound(SoundType.AMETHYST)
			.lightLevel(state -> state.getValue(SectBarrierBlock.FLUID) == SectBarrierBlock.Held.LAVA ? 15 : 0)));
	/** Qi ink painted with the Inscription Brush (see SealBlock): only Qi Sense sees it. No item; the brush places it. */
	public static final Block SEAL = register("seal", new SealBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE)
			.noCollission().instabreak().noLootTable().replaceable().pushReaction(PushReaction.DESTROY).sound(SoundType.WOOL)));
	/**
	 * Where one of the dead lies (CorpseBlock, Corpses): broken by hand as soon as dirt, it spills what it holds and drops nothing of itself.
	 * No item; a death lays it.
	 */
	public static final Block CORPSE = register("corpse", new CorpseBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE)
			.strength(0.6f).noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK).isValidSpawn((state, level, pos, type) -> false)));

	/** A secret realm's gate (SecretRealmGateBlock, SecretRealms): its Seal's opening, carved stone, cracked with light, or a sheet of light. */
	public static final Block SECRET_REALM_GATE = register("secret_realm_gate", new SecretRealmGateBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.DEEPSLATE).strength(-1.0f, 3_600_000.0f).noLootTable().sound(SoundType.DEEPSLATE).noOcclusion()
			.lightLevel(SecretRealmGateBlock::light).pushReaction(PushReaction.BLOCK).isValidSpawn((state, level, pos, type) -> false)));
	/** A Seal's keystone (SecretRealmSealBlock): its realm's seal carved in it, a pillar of light rising from it while the realm stirs or stands open. */
	public static final Block SECRET_REALM_SEAL = register("secret_realm_seal", new SecretRealmSealBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.DEEPSLATE).strength(-1.0f, 3_600_000.0f).noLootTable().sound(SoundType.DEEPSLATE).lightLevel(state -> 6)
			.pushReaction(PushReaction.BLOCK)));
	/** A killing formation's eye in a secret realm (KillingFormationBlock): broken by a pickaxe or a blow, it drops nothing; no item. */
	public static final Block KILLING_FORMATION = register("killing_formation", new KillingFormationBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE).strength(2.0f, 6.0f).noLootTable().sound(SoundType.STONE).lightLevel(state -> 2)
			.pushReaction(PushReaction.BLOCK)));
	/** One of the Cave Abode's five element pillars (ElementPillarBlock, its first trial): lit in the elements' order; nothing breaks it. */
	public static final Block ELEMENT_PILLAR = register("element_pillar", new ElementPillarBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.QUARTZ).strength(-1.0f, 3_600_000.0f).noLootTable().sound(SoundType.STONE)
			.lightLevel(state -> state.getValue(ElementPillarBlock.LIT) ? 13 : 3).pushReaction(PushReaction.BLOCK)));
	/** A trial's barrier of golden light (TrialBarrierBlock): solid to those who haven't passed its trial; nothing breaks it. */
	public static final Block TRIAL_BARRIER = register("trial_barrier", new TrialBarrierBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.GOLD).strength(-1.0f, 3_600_000.0f).noLootTable().noOcclusion().sound(SoundType.AMETHYST)
			.lightLevel(state -> 10).pushReaction(PushReaction.BLOCK).isValidSpawn((state, level, pos, type) -> false)
			.isViewBlocking((state, level, pos) -> false).isSuffocating((state, level, pos) -> false)));
	/**
	 * The Pill Sage's Cauldron, half of the Verdant Herb Valley's Great Inheritance (HerbValleyWorks): an Alchemy Cauldron with its own fire,
	 * its pills a grade better than the herbs roll and its brews half as likely to fail, and quicker (AlchemyCauldronBlockEntity).
	 */
	public static final Block PILL_SAGE_CAULDRON = registerWithItem("pill_sage_cauldron",
			new AlchemyCauldronBlock(BlockBehaviour.Properties.copy(Blocks.CAULDRON).mapColor(MapColor.GOLD).lightLevel(state -> 4)));
	/** A sword driven into the ground (BuriedSwordBlock): the Sunken Sword Tomb's floor is strewn with them; walked through, drops itself. */
	public static final Block BURIED_SWORD = registerWithItem("buried_sword", new BuriedSwordBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL).strength(1.5f, 6.0f).noOcclusion().noCollission().sound(SoundType.CHAIN).pushReaction(PushReaction.DESTROY)
			.isValidSpawn((state, level, pos, type) -> false)));
	/** The Jade Lake Mirage's mirages (MirageBlock): a span of its jade bridges, a spirit lotus, a vault, each the look of it and nothing of it; no items. */
	public static final Block MIRAGE_JADE = register("mirage_jade", new MirageBlock(MirageBlock.Kind.BRIDGE, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_LIGHT_GREEN).strength(-1.0f, 3_600_000.0f).noLootTable().noCollission().sound(SoundType.AMETHYST)
			.pushReaction(PushReaction.BLOCK).isValidSpawn((state, level, pos, type) -> false)));
	public static final Block MIRAGE_LOTUS = register("mirage_lotus", new MirageBlock(MirageBlock.Kind.LOTUS, BlockBehaviour.Properties.of()
			.mapColor(MapColor.PLANT).instabreak().noLootTable().noCollission().noOcclusion().sound(SoundType.LILY_PAD)
			.pushReaction(PushReaction.DESTROY).isValidSpawn((state, level, pos, type) -> false)));
	public static final Block MIRAGE_CASKET = register("mirage_casket", new MirageBlock(MirageBlock.Kind.CASKET, BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE).instabreak().noLootTable().noCollission().sound(SoundType.AMETHYST)
			.pushReaction(PushReaction.BLOCK).isValidSpawn((state, level, pos, type) -> false)));
	/** The Jade Lake Mirage's great mirror (JadeMirrorBlock, its trial): touched, a reflection steps out; a door to those who bested theirs. */
	public static final Block JADE_MIRROR = register("jade_mirror", new JadeMirrorBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(-1.0f, 3_600_000.0f).noLootTable().noOcclusion().sound(SoundType.GLASS)
			.lightLevel(state -> 7).pushReaction(PushReaction.BLOCK).isValidSpawn((state, level, pos, type) -> false)
			.isViewBlocking((state, level, pos) -> false).isSuffocating((state, level, pos) -> false)));
	/** One of the Ruins of a Fallen Holy Land's formation eyes (FormationEyeBlock, its trial): broken by hand once its guard is down; no item. */
	public static final Block FORMATION_EYE = register("formation_eye", new FormationEyeBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.QUARTZ).strength(6.0f, 3_600_000.0f).noLootTable().sound(SoundType.AMETHYST).lightLevel(state -> 12)
			.pushReaction(PushReaction.BLOCK)));
	/** A Law's seal on a door of the Tomb of a Fallen Immortal's halls (LawSealBlock): solid but to those who hold its Law; nothing breaks it. */
	public static final Block LAW_SEAL = register("law_seal", new LawSealBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.GOLD).strength(-1.0f, 3_600_000.0f).noLootTable().noOcclusion().sound(SoundType.AMETHYST)
			.lightLevel(state -> 9).pushReaction(PushReaction.BLOCK).isValidSpawn((state, level, pos, type) -> false)
			.isViewBlocking((state, level, pos) -> false).isSuffocating((state, level, pos) -> false)));

	/**
	 * The storage formations, one block to a tier (StorageFormations): nothing breaks them (no pickaxe, blast or spell; their owners pack
	 * them up), and they drop nothing of themselves. Their items are StorageFormationItem (ModItems).
	 */
	private static final java.util.Map<StorageFormations.Tier, Block> STORAGE_FORMATIONS = new java.util.EnumMap<>(StorageFormations.Tier.class);

	static {
		for (StorageFormations.Tier tier : StorageFormations.Tier.values()) {
			STORAGE_FORMATIONS.put(tier, register(tier.getId() + "_storage_formation", new StorageFormationBlock(tier, BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_CYAN).strength(-1.0f, 3_600_000.0f).noLootTable().sound(SoundType.AMETHYST).lightLevel(state -> 5)
					.pushReaction(PushReaction.BLOCK))));
		}
	}

	public static Block storageFormation(StorageFormations.Tier tier) {
		return STORAGE_FORMATIONS.get(tier);
	}

	private static BlockBehaviour.Properties ginsengProperties(MapColor colour) {
		return BlockBehaviour.Properties.of().mapColor(colour).noCollission().noOcclusion().instabreak()
				.sound(SoundType.SWEET_BERRY_BUSH).pushReaction(PushReaction.DESTROY).ignitedByLava();
	}

	private static Block register(String id, Block block) {
		return Registry.register(BuiltInRegistries.BLOCK, DefyingTheHeavens.id(id), block);
	}

	private static Block registerWithItem(String id, Block block) {
		register(id, block);
		Registry.register(BuiltInRegistries.ITEM, DefyingTheHeavens.id(id), new BlockItem(block, new Item.Properties()));
		return block;
	}

	/** Touching this class registers the blocks; also wires flammability and creative tabs. */
	public static void register() {
		FlammableBlockRegistry.getDefaultInstance().add(WHITE_BLOSSOM_LEAVES, 30, 60);
		FlammableBlockRegistry.getDefaultInstance().add(SPIRIT_PEACH_LEAVES, 30, 60);
		FlammableBlockRegistry.getDefaultInstance().add(SPIRIT_PEACH_HEART, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(MEDITATION_MAT, 60, 20);
		FlammableBlockRegistry.getDefaultInstance().add(RED_MEDITATION_MAT, 60, 20);
		FlammableBlockRegistry.getDefaultInstance().add(BLUE_SPIRIT_LOG, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(BLUE_SPIRIT_WOOD, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(BLUE_SPIRIT_PLANKS, 5, 20);
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
			entries.accept(MEDITATION_MAT);
			entries.accept(RED_MEDITATION_MAT);
			entries.accept(SPIRIT_PEDESTAL);
			entries.accept(HEAVEN_ALTAR);
			entries.accept(ALCHEMY_CAULDRON);
			entries.accept(FORMATION_CORE);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
			entries.accept(QI_VEIN);
			entries.accept(DEEPSLATE_QI_VEIN);
			entries.accept(JADE_STONE);
			entries.accept(JADE_ORE);
			entries.accept(DEEPSLATE_JADE_ORE);
			entries.accept(SPIRIT_STONE_ORE);
			entries.accept(DEEPSLATE_SPIRIT_STONE_ORE);
			entries.accept(RICH_SPIRIT_STONE_ORE);
			entries.accept(WHITE_BLOSSOM_LEAVES);
			entries.accept(SPIRIT_PEACH_LEAVES);
			entries.accept(BLUE_SPIRIT_LOG);
			entries.accept(SPIRIT_DEW_GRASS);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
			entries.accept(BLUE_SPIRIT_LOG);
			entries.accept(BLUE_SPIRIT_WOOD);
			entries.accept(BLUE_SPIRIT_PLANKS);
		});
	}

	private ModBlocks() {}
}
