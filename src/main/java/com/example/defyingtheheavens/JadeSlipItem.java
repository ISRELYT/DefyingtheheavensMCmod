package com.example.defyingtheheavens;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A Jade Slip (玉简) of a heaven method, from before 2026-11-16 (the user's ask: "remove all method jade slips from the game entirely"): no longer
 * made anywhere. One still lying in a world (its "HeavenMethod" tag) turns into an Art Manual of the same art (TechniqueManualItem) the moment it
 * is carried or read; one of nothing, into an empty Art Manual. Kept registered only so old worlds' slips aren't lost.
 */
public class JadeSlipItem extends Item {
	private static final String TAG = "HeavenMethod";

	public JadeSlipItem(Properties properties) {
		super(properties);
	}

	/** What a slip of {@code method} is now: its Art Manual. */
	public static ItemStack of(HeavenMethod method) {
		return TechniqueManualItem.of(method);
	}

	public static HeavenMethod method(ItemStack stack) {
		return stack.hasTag() ? HeavenMethod.byId(stack.getTag().getString(TAG)) : null;
	}

	/** {@code stack} (a slip from before) as what it is now: an Art Manual of its art. */
	static ItemStack converted(ItemStack stack) {
		HeavenMethod method = method(stack);
		return method == null ? new ItemStack(ModItems.TECHNIQUE_MANUAL) : TechniqueManualItem.of(method);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		if (!level.isClientSide && entity instanceof Player player) TechniqueManualItem.swap(player, stack, converted(stack));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack manual = converted(player.getItemInHand(hand));
		player.setItemInHand(hand, manual);
		return manual.use(level, player, hand);
	}
}
