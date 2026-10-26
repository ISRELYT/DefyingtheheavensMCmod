package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A Court Merit Token (CelestialCourt): a command token of gold and jade, the Celestial Court's pay for its officers' service (a minute's
 * watch at the open Southern Heavenly Gate, a devilish immortal of the realm slain); spent with the Court's Quartermaster.
 */
public class CourtMeritTokenItem extends Item {
	public CourtMeritTokenItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(ModLang.TOKEN_EARNED).withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.translatable(ModLang.TOKEN_SPENT).withStyle(ChatFormatting.GRAY));
	}
}
