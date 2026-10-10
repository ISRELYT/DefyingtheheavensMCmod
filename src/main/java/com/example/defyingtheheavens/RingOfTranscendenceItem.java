package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Halves Heavenly Tribulation damage while worn in a ring slot; wearing two halves it again (x0.25). */
public class RingOfTranscendenceItem extends RingItem {
	public static final double TRIBULATION_DAMAGE_MULTIPLIER = 0.5;

	public RingOfTranscendenceItem(Properties properties) {
		super(properties);
	}

	/** Multiplier for all tribulation strike damage the player takes. */
	public static float damageMultiplier(Player player) {
		int worn = RingContainer.of(player).countItem(ModItems.RING_OF_TRANSCENDENCE);
		return (float) Math.pow(TRIBULATION_DAMAGE_MULTIPLIER, worn);
	}

	@Override
	public Component getName(ItemStack stack) {
		return ModRarity.legendary(super.getName(stack));
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		Tooltips.add(tooltip, Component.translatable(ModLang.RING_OF_TRANSCENDENCE_TOOLTIP, String.valueOf(TRIBULATION_DAMAGE_MULTIPLIER))
				.withStyle(ChatFormatting.BLUE));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}
}
