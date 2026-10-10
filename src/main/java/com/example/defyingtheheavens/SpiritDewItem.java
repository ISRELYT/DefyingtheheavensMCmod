package com.example.defyingtheheavens;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * A bead of Spirit Dew from Spirit Dew Grass: drunk, it refills part of a cultivator's qi, or tempers a mortal's body; also
 * an alchemy ingredient.
 */
public class SpiritDewItem extends Item {
    /** Share of the qi pool one drop refills. */
    public static final double QI_RESTORED = 0.25;

    public SpiritDewItem(Properties properties) { super(properties); }

    /** Why it would be wasted right now, or null. */
    private static Component refusal(PlayerCultivation c) {
        if (c.isMortal()) return c.isMortalPeak() ? Component.translatable(ModLang.MSG_TEMPER_FULL) : null;
        if (c.getQi() >= c.maxQi()) return Component.translatable(ModLang.MSG_DEW_FULL);
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Component refusal = refusal(CultivationManager.forPlayer(player));
        if (refusal != null) {
            if (!level.isClientSide) player.displayClientMessage(refusal, true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override public int getUseDuration(ItemStack stack) { return 16; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return stack;
        PlayerCultivation c = CultivationManager.get(player);
        Component refusal = refusal(c);
        if (refusal != null) {
            player.displayClientMessage(refusal, true);
            return stack;
        }
        if (c.isMortal()) {
            MortalStage was = c.getMortalStage();
            if (!Tempering.gain(player, Tempering.SPIRIT_DEW)) return stack;
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.5f);
            if (c.getMortalStage() == was) { // a new stage has its own message
                player.displayClientMessage(Component.translatable(ModLang.MSG_HERB_EATEN, Math.round(Tempering.SPIRIT_DEW)), true);
            }
            if (!player.getAbilities().instabuild) stack.shrink(1);
            return stack;
        }
        double before = c.getQi();
        c.setQi(Math.min(c.maxQi(), before + c.maxQi() * QI_RESTORED));
        CultivationManager.markDirty(player.server);
        CultivationManager.sync(player);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.5f);
        player.displayClientMessage(Component.translatable(ModLang.MSG_DEW_DRUNK, Math.round(c.getQi() - before)), true);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        Tooltips.add(lines, Component.translatable(ModLang.DEW_TOOLTIP, Math.round(QI_RESTORED * 100)).withStyle(ChatFormatting.AQUA));
        Tooltips.add(lines, Component.translatable(ModLang.GINSENG_INGREDIENT).withStyle(ChatFormatting.GREEN));
    }
}
