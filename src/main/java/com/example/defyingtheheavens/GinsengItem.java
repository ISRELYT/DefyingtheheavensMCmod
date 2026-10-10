package com.example.defyingtheheavens;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A harvested herb (Ginseng, Spirit Ginseng, Lingzhi, Huangjing, Spirit Lotus and their variants), keeping the age it had
 * when it was dug up. An ingredient for the pills
 * and elixirs alchemy will make; for now it can be set on a Spirit Pedestal. Deliberately an ordinary Item, never a
 * BlockItem: harvested ginseng cannot be replanted, and it is not food.
 */
public class GinsengItem extends Item {
    public GinsengItem(Properties properties) { super(properties); }

    public static int age(ItemStack stack) {
        return FruitAge.clamp(stack.hasTag() ? stack.getTag().getInt("HerbAge") : 1);
    }

    public static ItemStack create(Item item, int years) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putInt("HerbAge", FruitAge.clamp(years));
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.defying-the-heavens.cultivation_fruit.age", age(stack)).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable(ModLang.GINSENG_INGREDIENT).withStyle(ChatFormatting.GREEN));
        Item base = AlchemyRecipes.substituteFor(this);
        if (base != null) lines.add(Component.translatable(ModLang.PILL_SUBSTITUTE, base.getDescription()).withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable(ModLang.FRUIT_PEDESTAL_TOOLTIP,
                Math.round(CultivationBoost.pedestalBonus(age(stack)) * 100)).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable(ModLang.GINSENG_HARVESTED).withStyle(ChatFormatting.GRAY));
    }
}
