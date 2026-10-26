package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A Sacred Vessel (2026-10-25): a relic of the ancients, found still on its altar in the Great Void (SacredRelicFeature), one for each
 * of the five elements: the Jianmu Bough Vase (wood), the Heaven Furnace Censer (fire), the Nine Provinces Ding (earth), the
 * Immortal-Forging Bell (metal) and the Sealed Ocean Gourd (water). Held in either hand it is a vessel of its element's law: its
 * holder's spells of that element strike {@link #STRENGTH} times as hard (HeavenSpells#strike), on top of what a Dao field gives.
 */
public class SacredVesselItem extends Item {
	/** What a vessel in hand makes its element's spells worth. */
	public static final float STRENGTH = 1.25f;

	private final QiElement element;

	public SacredVesselItem(QiElement element, Properties properties) {
		super(properties);
		this.element = element;
	}

	public QiElement element() { return element; }

	/** What {@code holder}'s spells of {@code element} are worth for the vessels in its hands: {@link #STRENGTH} with one of it in either. */
	public static float strength(LivingEntity holder, QiElement element) {
		if (element == null) return 1f;
		for (InteractionHand hand : InteractionHand.values()) {
			if (holder.getItemInHand(hand).getItem() instanceof SacredVesselItem vessel && vessel.element == element) return STRENGTH;
		}
		return 1f;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true; // a relic's sheen
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(ModLang.SACRED_VESSEL_USE, element.getDisplayName(), Math.round((STRENGTH - 1) * 100)).withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.translatable(ModLang.SACRED_VESSEL_LORE).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
	}
}
