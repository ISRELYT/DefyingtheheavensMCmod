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

	private static Item register(String id, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(DefyingTheHeavens.MOD_ID, id), item);
	}

	/** Touching this class registers the items; also lists them in the creative menu. */
	public static void register() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries ->
				entries.accept(CultivationFruitItem.create(1)));
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			entries.accept(RING_OF_POWER);
			entries.accept(RING_OF_TRANSCENDENCE);
		});
	}

	private ModItems() {}
}
