package com.example.defyingtheheavens;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Spirit Lotus seeds: placed on still water like a lily pad, they grow a Spirit Lotus, starting at 1 year. */
public class SpiritLotusSeedsItem extends PlaceOnWaterBlockItem {
    public SpiritLotusSeedsItem(Block plant, Properties properties) { super(plant, properties); }

    /** Named as seeds, not after the lotus they grow into. */
    @Override
    public String getDescriptionId() { return getOrCreateDescriptionId(); }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(ModLang.LOTUS_SEEDS_TOOLTIP, GinsengBlock.MATURE_YEARS).withStyle(ChatFormatting.GRAY));
    }
}
