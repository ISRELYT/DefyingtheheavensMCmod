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
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A harvested herb (Ginseng, Spirit Ginseng, Lingzhi, Huangjing, Spirit Lotus and their variants), keeping the age it had
 * when it was dug up. An ingredient for the pills
 * and elixirs alchemy will make; it can also be set on a Spirit Pedestal, and a mortal can eat it raw to temper their body
 * (see {@link Tempering}). Deliberately an ordinary Item, never a BlockItem: harvested ginseng cannot be replanted.
 */
public class GinsengItem extends Item {
    public GinsengItem(Properties properties) { super(properties); }

    public static int age(ItemStack stack) {
        return FruitAge.clamp(stack.hasTag() ? stack.getTag().getInt("HerbAge") : 1);
    }

    /** Only a mortal below Peak eats herbs raw; for a cultivator they're for alchemy and pedestals. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        PlayerCultivation c = CultivationManager.forPlayer(player);
        if (!c.isMortal()) return InteractionResultHolder.pass(stack);
        if (c.isMortalPeak()) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable(ModLang.MSG_TEMPER_FULL), true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override public int getUseDuration(ItemStack stack) { return 32; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.EAT; }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return stack;
        PlayerCultivation c = CultivationManager.get(player);
        MortalStage was = c.getMortalStage();
        double amount = Tempering.herb(stack);
        if (!Tempering.gain(player, amount)) return stack;
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8f, 1.0f);
        if (c.getMortalStage() == was) player.displayClientMessage(Component.translatable(ModLang.MSG_HERB_EATEN, Math.round(amount)), true);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return stack;
    }

    public static ItemStack create(Item item, int years) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putInt("HerbAge", FruitAge.clamp(years));
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        Tooltips.add(lines, Component.translatable("item.defying-the-heavens.cultivation_fruit.age", age(stack)).withStyle(ChatFormatting.GOLD));
        Tooltips.add(lines, Component.translatable(ModLang.GINSENG_INGREDIENT).withStyle(ChatFormatting.GREEN));
        Item base = AlchemyRecipes.substituteFor(this);
        if (base != null) Tooltips.add(lines, Component.translatable(ModLang.PILL_SUBSTITUTE, base.getDescription()).withStyle(ChatFormatting.LIGHT_PURPLE));
        Tooltips.add(lines, Component.translatable(ModLang.FRUIT_PEDESTAL_TOOLTIP,
                Math.round(CultivationBoost.pedestalBonus(age(stack)) * 100)).withStyle(ChatFormatting.AQUA));
        Tooltips.add(lines, Component.translatable(ModLang.GINSENG_HARVESTED).withStyle(ChatFormatting.GRAY));
    }
}
