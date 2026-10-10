package com.example.defyingtheheavens;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Deliberately an ordinary food Item, never a BlockItem: harvested fruit cannot be replanted. */
public class CultivationFruitItem extends Item {
    /** One fruit in this many leaves a pit that can be planted. */
    public static final int PIT_CHANCE = 8;

    public CultivationFruitItem(Properties properties) { super(properties); }

    public static int age(ItemStack stack) {
        return FruitAge.clamp(stack.hasTag() ? stack.getTag().getInt("FruitAge") : 1);
    }

    public static ItemStack create(int years) {
        ItemStack stack = new ItemStack(ModItems.CULTIVATION_FRUIT);
        stack.getOrCreateTag().putInt("FruitAge", FruitAge.clamp(years));
        return stack;
    }

    /**
     * Why eating it now would waste it, or null: a mortal can't hold its qi, and a stage at its bottleneck (or a cultivator
     * suppressed, or at the very peak) can't take in any more. The same rules as a Cultivation Pill.
     */
    private static Component refusal(PlayerCultivation c) {
        if (c.isMortal()) return Component.translatable(ModLang.MSG_FRUIT_MORTAL);
        if (c.isAtBottleneck() || c.isSuppressed() || c.isMaxed()) return Component.translatable(ModLang.MSG_FRUIT_BOTTLENECK);
        return null;
    }

    /** A fruit that would be wasted isn't eaten: the player is told why instead. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        Component refusal = refusal(CultivationManager.forPlayer(player));
        if (refusal != null) {
            if (!level.isClientSide) player.displayClientMessage(refusal, true);
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
        int amount = FruitAge.cultivation(age(stack));
        if (consumer instanceof ServerPlayer player) {
            // Like a Cultivation Pill: unrefined qi for meditation to refine, weakened by (and adding to) medicinal toxicity.
            PlayerCultivation c = CultivationManager.get(player);
            Component refusal = refusal(c); // things may have changed while it was being eaten
            if (refusal != null) {
                player.displayClientMessage(refusal, true);
                return stack;
            }
            double effectiveness = 1 - c.getPillResistance();
            double taken = c.takeMedicine(amount);
            CultivationManager.refresh(player);
            player.displayClientMessage(Component.translatable(ModLang.MSG_PILL_CULTIVATION, Math.round(taken),
                    Math.round(effectiveness * 100)), true);
        }
        ItemStack rest = super.finishUsingItem(stack, level, consumer);
        // Now and then (one fruit in PIT_CHANCE) its stone is left in your hand (or pocket), ready to plant a Spirit Peach Tree.
        if (!level.isClientSide && consumer instanceof Player player && level.random.nextInt(PIT_CHANCE) == 0) {
            ItemStack pit = new ItemStack(ModItems.PEACH_PIT);
            if (rest.isEmpty()) return pit;
            if (!player.getInventory().add(pit)) player.drop(pit, false);
        }
        return rest;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        Tooltips.add(lines, Component.translatable("item.defying-the-heavens.cultivation_fruit.age", age(stack)).withStyle(ChatFormatting.GOLD));
        Tooltips.add(lines, Component.translatable("item.defying-the-heavens.cultivation_fruit.gain", FruitAge.cultivation(age(stack))).withStyle(ChatFormatting.GREEN));
        double toxicity = CultivationManager.clientMirror.get().getPillResistance();
        if (toxicity > 0.005) {
            Tooltips.add(lines, Component.translatable(ModLang.PILL_EFFECT_RESISTANCE, Math.round((1 - toxicity) * 100)).withStyle(ChatFormatting.RED));
        }
        Tooltips.add(lines, Component.translatable(ModLang.FRUIT_PEDESTAL_TOOLTIP,
                Math.round(CultivationBoost.pedestalBonus(age(stack)) * 100)).withStyle(ChatFormatting.AQUA));
        Tooltips.add(lines, Component.translatable("item.defying-the-heavens.cultivation_fruit.picked").withStyle(ChatFormatting.GRAY));
    }
}
