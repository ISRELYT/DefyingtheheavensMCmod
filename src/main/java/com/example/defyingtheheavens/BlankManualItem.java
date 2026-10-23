package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A Blank Manual (a book bound in jade): what a technique or a spell one knows is written out on, with the Inscription Brush in the
 * other hand (TechniqueManualItem#offerInscription), to become its manual.
 */
public class BlankManualItem extends Item {
	public BlankManualItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
		Tooltips.add(lines, Component.translatable(ModLang.TOOLTIP_BLANK_MANUAL).withStyle(ChatFormatting.GRAY));
	}
}
