package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Spring of the Ancients (found only in the secret realms): water of a spring the ancients drew from, bottled. Drunk, for a day meditation
 * gathers half as much cultivation again (ModEffects#ANCIENT_SPRING); the bottle is given back.
 */
public class SpringOfTheAncientsItem extends Item {
	public SpringOfTheAncientsItem(Properties properties) {
		super(properties);
	}

	@Override
	public int getUseDuration(ItemStack stack) { return 32; }

	@Override
	public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		return ItemUtils.startUsingInstantly(level, player, hand);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
		if (!level.isClientSide) {
			living.addEffect(new MobEffectInstance(ModEffects.ANCIENT_SPRING, ModEffects.SPRING_TICKS, 0, false, true, true));
			if (living instanceof ServerPlayer p) p.displayClientMessage(Component.translatable(ModLang.MSG_SPRING_DRUNK).withStyle(ChatFormatting.AQUA), true);
		}
		if (living instanceof Player p && p.getAbilities().instabuild) return stack;
		stack.shrink(1);
		if (stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
		if (living instanceof Player p && !p.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) p.drop(new ItemStack(Items.GLASS_BOTTLE), false);
		return stack;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(ModLang.TOOLTIP_ANCIENT_SPRING, Math.round(ModEffects.SPRING_BONUS * 100)).withStyle(ChatFormatting.GRAY));
	}
}
