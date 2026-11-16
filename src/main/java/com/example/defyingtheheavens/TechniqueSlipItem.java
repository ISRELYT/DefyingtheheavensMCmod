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

import java.util.List;

/**
 * A Technique Jade Slip (the user's ask, 2026-11-16: "replace technique manuals with technique jade slips"): a slip of jade inked in gold holding
 * one technique (its "Technique" tag: Technique) and who wrote it out ("Author"), if anyone did. Read (used), it teaches the technique to one who
 * stands at its realm (TechniqueManualItem#learn), and isn't used up. Where a technique's manual was kept, sold or found, its slip is now
 * (TechniqueManualItem#of(Technique) makes one); a technique's manual from before turns into its slip as it is carried
 * (TechniqueManualItem#inventoryTick). The spells' manuals stay manuals: Art Manuals (TechniqueManualItem).
 */
public class TechniqueSlipItem extends Item {
	static final String TECHNIQUE = "Technique", AUTHOR = "Author";

	public TechniqueSlipItem(Properties properties) {
		super(properties);
	}

	/** A slip of {@code technique}. */
	public static ItemStack of(Technique technique) {
		ItemStack stack = new ItemStack(ModItems.TECHNIQUE_JADE_SLIP);
		stack.getOrCreateTag().putString(TECHNIQUE, technique.getId());
		return stack;
	}

	public static Technique technique(ItemStack stack) {
		return stack.hasTag() && stack.getTag().contains(TECHNIQUE) ? Technique.byId(stack.getTag().getString(TECHNIQUE)) : null;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		Technique technique = technique(stack);
		if (technique == null) return InteractionResultHolder.pass(stack);
		if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
		if (!TechniqueManualItem.learn(sp, technique)) return InteractionResultHolder.fail(stack);
		level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.0f);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public Component getName(ItemStack stack) {
		Technique technique = technique(stack);
		return technique == null ? super.getName(stack) : Component.translatable(ModLang.ITEM_TECHNIQUE_SLIP_OF, technique.getDisplayName());
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
		Technique technique = technique(stack);
		if (technique != null) {
			lines.add(Component.translatable(ModLang.TOOLTIP_MANUAL_REALM, PlayerCultivation.rankName(technique.realm(), Stage.EARLY)).withStyle(ChatFormatting.GOLD));
			Tooltips.add(lines, technique.getDescription().copy().withStyle(ChatFormatting.GRAY));
		}
		if (stack.hasTag() && stack.getTag().contains(AUTHOR)) {
			lines.add(Component.translatable(ModLang.TOOLTIP_MANUAL_AUTHOR, stack.getTag().getString(AUTHOR)).withStyle(ChatFormatting.DARK_GRAY));
		}
		lines.add(Component.translatable(ModLang.TOOLTIP_TECHNIQUE_SLIP).withStyle(ChatFormatting.GRAY));
	}
}
