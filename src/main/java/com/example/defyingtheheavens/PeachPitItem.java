package com.example.defyingtheheavens;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** The stone left after eating a Cultivation Fruit. Planted in soil, it grows into a Spirit Peach sapling. */
public class PeachPitItem extends ItemNameBlockItem {
    public PeachPitItem(Block sapling, Properties properties) { super(sapling, properties); }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        Tooltips.add(lines, Component.translatable(ModLang.PEACH_PIT_TOOLTIP).withStyle(ChatFormatting.GRAY));
    }
}
