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

	/** Left over after eating a Cultivation Fruit; plants a Spirit Peach sapling. */
	public static final Item PEACH_PIT = register("peach_pit", new PeachPitItem(ModBlocks.SPIRIT_PEACH_SAPLING, new Item.Properties()));

	private static Item register(String id, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(DefyingTheHeavens.MOD_ID, id), item);
	}

	/** Touching this class registers the items; also lists them in the creative menu. */
	public static void register() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries ->
				entries.accept(CultivationFruitItem.create(1)));
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
			entries.accept(PEACH_PIT);
			entries.accept(GINSENG_SEEDS);
			entries.accept(SPIRIT_GINSENG_SEEDS);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
			entries.accept(GinsengItem.create(GINSENG, 1));
			entries.accept(GinsengItem.create(SPIRIT_GINSENG, 1));
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			entries.accept(RING_OF_POWER);
			entries.accept(RING_OF_TRANSCENDENCE);
		});
	}

	private ModItems() {}
}
