package com.example.defyingtheheavens;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Deliberately an ordinary food Item, never a BlockItem: harvested fruit cannot be replanted. */
public class CultivationFruitItem extends Item {
    public CultivationFruitItem(Properties properties) { super(properties); }

    public static int age(ItemStack stack) {
        return FruitAge.clamp(stack.hasTag() ? stack.getTag().getInt("FruitAge") : 1);
    }

    public static ItemStack create(int years) {
        ItemStack stack = new ItemStack(ModItems.CULTIVATION_FRUIT);
        stack.getOrCreateTag().putInt("FruitAge", FruitAge.clamp(years));
        return stack;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
        int amount = FruitAge.cultivation(age(stack));
        if (consumer instanceof ServerPlayer player) {
            CultivationManager.get(player).addCultivation(amount);
            CultivationManager.refresh(player);
        }
        return super.finishUsingItem(stack, level, consumer);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.defying-the-heavens.cultivation_fruit.age", age(stack)).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("item.defying-the-heavens.cultivation_fruit.gain", FruitAge.cultivation(age(stack))).withStyle(ChatFormatting.GREEN));
        lines.add(Component.translatable(ModLang.FRUIT_PEDESTAL_TOOLTIP,
                Math.round(CultivationBoost.pedestalBonus(age(stack)) * 100)).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("item.defying-the-heavens.cultivation_fruit.picked").withStyle(ChatFormatting.GRAY));
    }
}
