package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
 * A Dao Fragment (phase 3c): a shard of one Law, broken off it in the chaos before heaven and earth were parted; the Chaos Frontier's (its
 * fiends now and then, ChaosFiend; the Primordial Chaos six, PrimordialBeast). Coloured by its Law (layer 1, the client's). Used, it is
 * taken in: {@link Laws#FRAGMENT_SHARE} of its Law's next level of insight (Laws#absorb), for one who may comprehend that Law (an immortal;
 * a greater Law only from Golden Immortal on).
 */
public class DaoFragmentItem extends Item {
	private static final String LAW = "Law";

	public DaoFragmentItem(Properties properties) {
		super(properties);
	}

	/** A Dao Fragment of {@code law}. */
	public static ItemStack of(Law law) {
		ItemStack stack = new ItemStack(ModItems.DAO_FRAGMENT);
		stack.getOrCreateTag().putString(LAW, law.getId());
		return stack;
	}

	/** Its Law, or null (one made without, by a command). */
	public static Law law(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? null : Law.byId(tag.getString(LAW));
	}

	/** Its colour (layer 1): its Law's; a grey-violet chaos's with none. */
	public static int tint(ItemStack stack) {
		Law law = law(stack);
		return law == null ? 0xA898C0 : law.color();
	}

	@Override
	public Component getName(ItemStack stack) {
		Law law = law(stack);
		return law == null ? super.getName(stack) : Component.translatable(ModLang.ITEM_DAO_FRAGMENT_OF, law.getDisplayName());
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(ModLang.TOOLTIP_DAO_FRAGMENT, Math.round(Laws.FRAGMENT_SHARE * 100)).withStyle(ChatFormatting.GRAY));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(player instanceof ServerPlayer p)) return InteractionResultHolder.success(stack);
		Law law = law(stack);
		if (law == null || !Laws.absorb(p, law)) return InteractionResultHolder.fail(stack);
		if (!p.getAbilities().instabuild) stack.shrink(1);
		return InteractionResultHolder.consume(stack);
	}
}
