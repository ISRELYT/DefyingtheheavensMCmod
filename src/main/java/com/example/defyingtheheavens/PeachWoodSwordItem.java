package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A Peach Wood Sword (桃木剑), the exorcist's weapon: against the living it fights like a stone sword; against the dead (a
 * player's ghost, a ghost cultivator, a Hungry Ghost) every blow lands in full whoever swings it, whatever their realm, and
 * twice as hard ({@link #ON_THE_DEAD}; see GhostSystem#peachWood).
 */
public class PeachWoodSwordItem extends SwordItem {
	/** A blow on the dead lands this many times as hard. */
	public static final float ON_THE_DEAD = 2.0f;

	public PeachWoodSwordItem(Properties properties) {
		super(Tiers.STONE, 3, -2.4f, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
		lines.add(Component.translatable(ModLang.TOOLTIP_PEACH_WOOD_SWORD).withStyle(ChatFormatting.GRAY));
	}
}
