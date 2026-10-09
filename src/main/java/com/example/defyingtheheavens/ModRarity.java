package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

/** Name colors beyond vanilla's four rarities (white / yellow / aqua / purple). */
public final class ModRarity {
	/** Orange, for legendary (really rare) items. */
	public static final TextColor LEGENDARY = TextColor.fromRgb(0xFF8000);

	/** Use from an item's getName(): the item's own color wins over the vanilla rarity color around it. */
	public static MutableComponent legendary(Component name) {
		return name.copy().withStyle(style -> style.withColor(LEGENDARY));
	}

	private ModRarity() {}
}
