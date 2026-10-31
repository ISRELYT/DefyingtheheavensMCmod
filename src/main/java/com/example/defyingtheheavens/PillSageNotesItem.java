package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Pill Sage's Notes (the Verdant Herb Valley's: HerbValleyWorks), read and used up. <b>The Notes</b>, half its Great Inheritance with his
 * cauldron: a whole rank of alchemy at once (CraftMastery: to the next rank's beginning). <b>A Page</b> of them, a lesser legacy: a fifth of the
 * way to the next rank (at least {@link #PAGE_LEAST}). At Saint there is nothing left in them to learn.
 */
public class PillSageNotesItem extends Item {
	/** A page teaches at least this much practice. */
	public static final int PAGE_LEAST = 12;
	private final boolean whole;

	public PillSageNotesItem(boolean whole, Properties properties) {
		super(properties);
		this.whole = whole;
	}

	/** The practice {@code practice} more that reading it gives: to the next rank's beginning (the Notes) or a fifth of the way there (a Page). */
	public int teaches(int practice) {
		CraftMastery.Rank next = CraftMastery.Rank.of(practice).next();
		if (next == null) return 0;
		int toNext = next.from - practice;
		return whole ? toNext : Math.min(toNext, Math.max(PAGE_LEAST, (next.from - CraftMastery.Rank.of(practice).from) / 5));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(player instanceof ServerPlayer p)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
		int teaches = teaches(CultivationManager.own(p).getMastery(CraftMastery.Craft.ALCHEMY));
		if (teaches <= 0) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_SAGE_NOTES_NOTHING).withStyle(ChatFormatting.GRAY), true);
			return InteractionResultHolder.fail(stack);
		}
		p.sendSystemMessage(Component.translatable(whole ? ModLang.MSG_SAGE_NOTES_READ : ModLang.MSG_SAGE_PAGE_READ).withStyle(ChatFormatting.GOLD));
		CraftMastery.practise(p, CraftMastery.Craft.ALCHEMY, teaches);
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.9f);
		if (!p.getAbilities().instabuild) stack.shrink(1);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(whole ? ModLang.TOOLTIP_SAGE_NOTES : ModLang.TOOLTIP_SAGE_PAGE).withStyle(ChatFormatting.GRAY));
	}
}
