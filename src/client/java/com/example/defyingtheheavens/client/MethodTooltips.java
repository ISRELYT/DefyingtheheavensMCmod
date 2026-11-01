package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.FiveElementsCycling;
import com.example.defyingtheheavens.GinsengItem;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.QiElement;
import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.SpiritHerbRefining;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * What a method practised takes in, told on the tooltips of what it takes: Five Elements Cycling's offerings (FiveElementsCycling#element,
 * #grade: the element an item is taken in as, and the realm it stands at), and Spirit Herb Refining's herbs (GinsengItem#grade), so a
 * sapling and a thousand-year herb are told apart.
 */
final class MethodTooltips {
	static void register() {
		ItemTooltipCallback.EVENT.register((stack, flag, lines) -> {
			PlayerCultivation c = ClientCultivationData.get();
			if (FiveElementsCycling.practises(c)) {
				QiElement element = FiveElementsCycling.element(stack);
				Realm grade = FiveElementsCycling.grade(stack);
				if (element != null && grade != null) {
					lines.add(Component.translatable(ModLang.TOOLTIP_CYCLE_OFFERING, element.getDisplayName(), grade.getDisplayName())
							.withStyle(FiveElementsCycling.colour(element)));
				}
			} else if (SpiritHerbRefining.practises(c) && stack.getItem() instanceof GinsengItem) {
				lines.add(Component.translatable(ModLang.TOOLTIP_HERB_ESSENCE, GinsengItem.grade(stack).getDisplayName()).withStyle(ChatFormatting.GREEN));
			}
		});
	}

	private MethodTooltips() {}
}
