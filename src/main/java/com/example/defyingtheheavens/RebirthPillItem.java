package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Rebirth Pill (the user's ask, 2026-10-20: a way out of a bad roll): eaten, this life's cultivation ends whole as a spent life's
 * does, a new physique with it, but without the death (Lifespan#rebirth). Any number may be taken, one after another, so no cauldron
 * brews it: it is found only in the sect treasuries of the Upper Realm, now and then (loot_tables/chests/sect_treasury.json). Eaten
 * only while sneaking, so it is never taken by a slip of the hand. It has no hold on an Eternal Reincarnation Body ({@link
 * #refuses}): born again losing nothing (Physique), such a body would have had a whole new life of years from it for nothing.
 */
public class RebirthPillItem extends Item {
	public RebirthPillItem(Properties properties) {
		super(properties);
	}

	/**
	 * Whether {@code c} can't take the pill: an Eternal Reincarnation Body, whose rebirth keeps all its cultivation (the user's call,
	 * 2026-10-22: "Make a Rebirth Pill do nothing for an Eternal Reincarnation Body").
	 */
	public static boolean refuses(PlayerCultivation c) {
		return c.hasPhysique(Physique.ETERNAL_REINCARNATION_BODY);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (refuses(CultivationManager.forPlayer(player))) {
			if (!level.isClientSide) player.displayClientMessage(Component.translatable(ModLang.MSG_REBIRTH_ETERNAL).withStyle(ChatFormatting.GOLD), true);
			return InteractionResultHolder.fail(stack);
		}
		if (!player.isShiftKeyDown()) {
			if (!level.isClientSide) player.displayClientMessage(Component.translatable(ModLang.MSG_REBIRTH_CONFIRM).withStyle(ChatFormatting.RED), true);
			return InteractionResultHolder.fail(stack);
		}
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 40;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.EAT;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!(entity instanceof ServerPlayer player)) return stack;
		if (refuses(CultivationManager.get(player))) { // its body may have changed while it was being eaten (an op's command)
			player.displayClientMessage(Component.translatable(ModLang.MSG_REBIRTH_ETERNAL).withStyle(ChatFormatting.GOLD), true);
			return stack;
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		if (!player.getAbilities().instabuild) stack.shrink(1);
		player.sendSystemMessage(Component.translatable(ModLang.MSG_REBIRTH).withStyle(ChatFormatting.LIGHT_PURPLE));
		Lifespan.rebirth(player);
		return stack;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
		Tooltips.add(lines, Component.translatable(ModLang.TOOLTIP_REBIRTH_PILL).withStyle(ChatFormatting.GOLD));
		Tooltips.add(lines, Component.translatable(ModLang.TOOLTIP_REBIRTH_PILL_WARN).withStyle(ChatFormatting.RED));
	}
}
