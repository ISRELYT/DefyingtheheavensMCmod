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
 * A Ghost Jade Slip (鬼玉简): a slip of dark jade holding a ghost method (its "Method" tag). Read (used), it teaches the method
 * (at the layer GhostMethods#startLayer gives), and isn't used up. An item, so only the living can read one. Dropped now and then by ghost cultivators, sold by
 * ghost elders.
 */
public class GhostJadeSlipItem extends Item {
	private static final String TAG = "Method";

	public GhostJadeSlipItem(Properties properties) {
		super(properties);
	}

	/** A slip holding {@code method}. */
	public static ItemStack of(GhostMethod method) {
		ItemStack stack = new ItemStack(ModItems.GHOST_JADE_SLIP);
		stack.getOrCreateTag().putString(TAG, method.getId());
		return stack;
	}

	public static GhostMethod method(ItemStack stack) {
		return stack.hasTag() ? GhostMethod.byId(stack.getTag().getString(TAG)) : null;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		GhostMethod method = method(stack);
		if (method == null) return InteractionResultHolder.pass(stack);
		if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
		if (CultivationManager.get(sp).netherworld().knows(method)) {
			sp.displayClientMessage(Component.translatable(ModLang.MSG_SLIP_KNOWN, method.getDisplayName()), true);
			return InteractionResultHolder.fail(stack);
		}
		// Not taken up (a mortal's meridians are closed): told why, and nothing read.
		if (!GhostMethods.learn(sp, method)) return InteractionResultHolder.fail(stack);
		level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.6f);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public Component getName(ItemStack stack) {
		GhostMethod method = method(stack);
		return method == null ? super.getName(stack) : Component.translatable(ModLang.ITEM_GHOST_JADE_SLIP_OF, method.getDisplayName());
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
		lines.add(Component.translatable(ModLang.TOOLTIP_GHOST_JADE_SLIP).withStyle(ChatFormatting.GRAY));
	}
}
