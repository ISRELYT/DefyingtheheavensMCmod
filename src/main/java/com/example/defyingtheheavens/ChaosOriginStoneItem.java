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
 * A Chaos Origin Stone (phase 3c): chaos from before heaven and earth were parted, set hard. The Chaos Frontier's: left by its fiends
 * (ChaosFiend) and the Primordial Chaos (PrimordialBeast), mined from Chaos Origin Ore with a diamond pickaxe or better. What a small world's
 * seed is made of (with immortal jade: phase 3d).
 */
public class ChaosOriginStoneItem extends Item {
	public ChaosOriginStoneItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(ModLang.TOOLTIP_CHAOS_ORIGIN_STONE).withStyle(ChatFormatting.DARK_PURPLE));
	}
}
