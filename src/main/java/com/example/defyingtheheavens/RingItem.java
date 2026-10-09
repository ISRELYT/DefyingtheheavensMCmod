package com.example.defyingtheheavens;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Anything that can be worn in the two ring slots. Rings only take effect while worn. */
public class RingItem extends Item {
	public RingItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	/** Right-click puts the ring on, like armor. */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		RingContainer rings = RingContainer.of(player);
		for (int i = 0; i < rings.getContainerSize(); i++) {
			if (rings.getItem(i).isEmpty()) {
				rings.setItem(i, stack.split(1));
				player.playSound(SoundEvents.ARMOR_EQUIP_GOLD, 1.0f, 1.0f);
				return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
			}
		}
		return InteractionResultHolder.fail(stack);
	}
}
