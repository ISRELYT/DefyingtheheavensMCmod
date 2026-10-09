package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Boosts the cultivation gained while meditating (not the qi pool's refill). Only counts while worn in a ring slot; wearing
 * two doubles the bonus.
 */
public class RingOfPowerItem extends RingItem {
	public static final double CULTIVATION_BONUS_PER_SECOND = 1000;

	public RingOfPowerItem(Properties properties) {
		super(properties);
	}

	/** Extra cultivation per second the player gets while meditating. Safe on both sides (the ring slots are synced to the owner). */
	public static double cultivationBonus(Player player) {
		return player == null ? 0 : RingContainer.of(player).countItem(ModItems.RING_OF_POWER) * CULTIVATION_BONUS_PER_SECOND;
	}

	@Override
	public Component getName(ItemStack stack) {
		return ModRarity.legendary(super.getName(stack));
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(ModLang.RING_OF_POWER_TOOLTIP, (int) CULTIVATION_BONUS_PER_SECOND).withStyle(ChatFormatting.BLUE));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}
}
