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
 * A Jade Slip (玉简): a slip of green jade holding a heaven method (its "HeavenMethod" tag). Read (used), it teaches the method
 * (at the layer HeavenMethods#startLayer gives), and isn't used up. The dead may read one (Incorporeal#mayUse), but take up no
 * heaven method (HeavenMethods#refusal says why).
 * Cultivators carry them: one struck down may drop its own, and some sell theirs (CultivatorNpc#heavenMethod).
 */
public class JadeSlipItem extends Item {
	private static final String TAG = "HeavenMethod";

	public JadeSlipItem(Properties properties) {
		super(properties);
	}

	/** A slip holding {@code method}. */
	public static ItemStack of(HeavenMethod method) {
		ItemStack stack = new ItemStack(ModItems.JADE_SLIP);
		stack.getOrCreateTag().putString(TAG, method.getId());
		return stack;
	}

	public static HeavenMethod method(ItemStack stack) {
		return stack.hasTag() ? HeavenMethod.byId(stack.getTag().getString(TAG)) : null;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		HeavenMethod method = method(stack);
		if (method == null) return InteractionResultHolder.pass(stack);
		if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
		if (CultivationManager.get(sp).heaven().knows(method)) {
			sp.displayClientMessage(Component.translatable(ModLang.MSG_SLIP_KNOWN, method.getDisplayName()), true);
			return InteractionResultHolder.fail(stack);
		}
		// Not taken up (a mortal's meridians are closed): told why, and nothing read.
		if (!HeavenMethods.learn(sp, method)) return InteractionResultHolder.fail(stack);
		level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.2f);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public Component getName(ItemStack stack) {
		HeavenMethod method = method(stack);
		return method == null ? super.getName(stack) : Component.translatable(ModLang.ITEM_JADE_SLIP_OF, method.getDisplayName());
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
		HeavenMethod method = method(stack);
		if (method != null) {
			lines.add(Component.translatable(ModLang.TOOLTIP_JADE_SLIP_ELEMENT, method.element().getDisplayName(), method.realm().getDisplayName())
					.withStyle(ChatFormatting.GOLD));
		}
		lines.add(Component.translatable(ModLang.TOOLTIP_JADE_SLIP).withStyle(ChatFormatting.GRAY));
	}
}
