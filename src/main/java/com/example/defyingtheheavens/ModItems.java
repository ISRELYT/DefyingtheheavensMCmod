package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ModItems {
	public static final Item CULTIVATION_FRUIT = register("cultivation_fruit", new CultivationFruitItem(
			new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder()
					.nutrition(4).saturationMod(0.3f).alwaysEat().build())));
	public static final Item RING_OF_POWER = register("ring_of_power",
			new RingOfPowerItem(new Item.Properties().rarity(Rarity.EPIC)));
	public static final Item RING_OF_TRANSCENDENCE = register("ring_of_transcendence",
			new RingOfTranscendenceItem(new Item.Properties().rarity(Rarity.EPIC)));

	/** Harvested ginseng roots: ingredients for alchemy, keeping their age (see GinsengItem). */
	public static final Item GINSENG = register("ginseng", new GinsengItem(new Item.Properties()));
	public static final Item SPIRIT_GINSENG = register("spirit_ginseng", new GinsengItem(new Item.Properties().rarity(Rarity.RARE)));
	/** Planted in soil, these grow ginseng (see GinsengSeedsItem); a full-grown plant gives 1-3 when dug up. */
	public static final Item GINSENG_SEEDS = register("ginseng_seeds", new GinsengSeedsItem(ModBlocks.GINSENG, new Item.Properties()));
	public static final Item SPIRIT_GINSENG_SEEDS = register("spirit_ginseng_seeds",
			new GinsengSeedsItem(ModBlocks.SPIRIT_GINSENG, new Item.Properties().rarity(Rarity.UNCOMMON)));

	/** The other aging herbs (see ModBlocks): harvested items that keep their age, and the seeds or spores they're grown from. */
	public static final Item LINGZHI = register("lingzhi", new GinsengItem(new Item.Properties()));
	public static final Item PURPLE_LINGZHI = register("purple_lingzhi", new GinsengItem(new Item.Properties().rarity(Rarity.RARE)));
	public static final Item HUANGJING = register("huangjing", new GinsengItem(new Item.Properties()));
	public static final Item OCHRE_HUANGJING = register("ochre_huangjing", new GinsengItem(new Item.Properties().rarity(Rarity.RARE)));
	public static final Item SPIRIT_LOTUS = register("spirit_lotus", new GinsengItem(new Item.Properties().rarity(Rarity.RARE)));
	public static final Item LINGZHI_SPORES = register("lingzhi_spores", new GinsengSeedsItem(ModBlocks.LINGZHI, new Item.Properties()));
	public static final Item PURPLE_LINGZHI_SPORES = register("purple_lingzhi_spores",
			new GinsengSeedsItem(ModBlocks.PURPLE_LINGZHI, new Item.Properties().rarity(Rarity.UNCOMMON)));
	public static final Item HUANGJING_SEEDS = register("huangjing_seeds", new GinsengSeedsItem(ModBlocks.HUANGJING, new Item.Properties()));
	public static final Item OCHRE_HUANGJING_SEEDS = register("ochre_huangjing_seeds",
			new GinsengSeedsItem(ModBlocks.OCHRE_HUANGJING, new Item.Properties().rarity(Rarity.UNCOMMON)));
	public static final Item SPIRIT_LOTUS_SEEDS = register("spirit_lotus_seeds",
			new SpiritLotusSeedsItem(ModBlocks.SPIRIT_LOTUS, new Item.Properties().rarity(Rarity.UNCOMMON)));
	/** Collected from Spirit Dew Grass: drunk for qi, or brewed. */
	public static final Item SPIRIT_DEW = register("spirit_dew", new SpiritDewItem(new Item.Properties().stacksTo(16)));

	/** Left over after eating a Cultivation Fruit; plants a Spirit Peach sapling. */
	public static final Item PEACH_PIT = register("peach_pit", new PeachPitItem(ModBlocks.SPIRIT_PEACH_SAPLING, new Item.Properties()));

	/** Brewed in the Alchemy Cauldron, each with a grade and the age of its ingredients (see PillItem, AlchemyRecipes). */
	public static final Item MARROW_CLEANSING_ELIXIR = register("marrow_cleansing_elixir",
			new PillItem(PillItem.Kind.MARROW_CLEANSING, new Item.Properties().stacksTo(16)));
	public static final Item FOUNDATION_PILL = register("foundation_pill", new PillItem(PillItem.Kind.FOUNDATION, new Item.Properties()));
	public static final Item CORE_PILL = register("core_pill", new PillItem(PillItem.Kind.CORE, new Item.Properties()));
	public static final Item QI_GATHERING_PILL = register("qi_gathering_pill", new PillItem(PillItem.Kind.QI_GATHERING, new Item.Properties()));
	public static final Item CULTIVATION_PILL = register("cultivation_pill", new PillItem(PillItem.Kind.CULTIVATION, new Item.Properties()));

	/** Mined from Jade Ore. */
	public static final Item JADE = register("jade", new Item(new Item.Properties()));

	private static Item register(String id, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(DefyingTheHeavens.MOD_ID, id), item);
	}

	/** Touching this class registers the items; also lists them in the creative menu. */
	public static void register() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
			entries.accept(CultivationFruitItem.create(1));
			for (Item pill : new Item[] {MARROW_CLEANSING_ELIXIR, QI_GATHERING_PILL, CULTIVATION_PILL, FOUNDATION_PILL, CORE_PILL}) {
				entries.accept(PillItem.create(pill, PillGrade.LOW, 1));
			}
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
			entries.accept(PEACH_PIT);
			entries.accept(GINSENG_SEEDS);
			entries.accept(SPIRIT_GINSENG_SEEDS);
			for (Item seeds : new Item[] {LINGZHI_SPORES, PURPLE_LINGZHI_SPORES, HUANGJING_SEEDS, OCHRE_HUANGJING_SEEDS, SPIRIT_LOTUS_SEEDS}) {
				entries.accept(seeds);
			}
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
			entries.accept(GinsengItem.create(GINSENG, 1));
			entries.accept(GinsengItem.create(SPIRIT_GINSENG, 1));
			for (Item herb : new Item[] {LINGZHI, PURPLE_LINGZHI, HUANGJING, OCHRE_HUANGJING, SPIRIT_LOTUS}) {
				entries.accept(GinsengItem.create(herb, 1));
			}
			entries.accept(SPIRIT_DEW);
			entries.accept(JADE);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			entries.accept(RING_OF_POWER);
			entries.accept(RING_OF_TRANSCENDENCE);
		});
	}

	private ModItems() {}
}
