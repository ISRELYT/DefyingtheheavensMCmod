package com.example.defyingtheheavens;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Ginseng or Spirit Ginseng seeds: planted in soil, they grow a seedling of that ginseng, starting at 1 year. */
public class GinsengSeedsItem extends ItemNameBlockItem {
    public GinsengSeedsItem(Block plant, Properties properties) { super(plant, properties); }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        Tooltips.add(lines, Component.translatable(ModLang.GINSENG_SEEDS_TOOLTIP, GinsengBlock.MATURE_YEARS).withStyle(ChatFormatting.GRAY));
    }
}
