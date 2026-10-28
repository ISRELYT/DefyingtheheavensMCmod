package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * A Storage Ring, from before the rings were taken out of the game (the user's ask, 2026-10-28: storage formations in their place,
 * StorageFormations). Kept only so the rings already in a world aren't lost with all they hold: carried, one unfolds at once into the
 * storage formation of its size with its very space (StorageFormations#convertRings). Nothing makes one any more.
 */
public class StorageRingItem extends Item {
	private static final String ID_TAG = "StorageRing";
	private static final String GRADE_TAG = "StorageGrade";

	public StorageRingItem(Properties properties) {
		super(properties);
	}

	/** The ring's space's id, or null if it was never opened. */
	public static UUID idOf(ItemStack stack) {
		if (!(stack.getItem() instanceof StorageRingItem) || !stack.hasTag() || !stack.getTag().hasUUID(ID_TAG)) return null;
		return stack.getTag().getUUID(ID_TAG);
	}

	/** The ring's grade, as it was kept: Mortal 0 to Boundless 4. */
	public static int gradeIndex(ItemStack stack) {
		return stack.hasTag() ? Math.max(0, Math.min(4, stack.getTag().getInt(GRADE_TAG))) : 0;
	}

	/** In a player's pack: unfolds into its formation at once (the ring slots and the ender chest each second: StorageFormations). */
	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		if (level.isClientSide || !(entity instanceof Player player)) return;
		if (player.getInventory().getItem(slot) == stack) {
			player.getInventory().setItem(slot, StorageFormations.unfold(stack));
			player.sendSystemMessage(Component.translatable(ModLang.MSG_STORAGE_RING_UNFOLDED).withStyle(ChatFormatting.GOLD));
		}
	}

	@Override
	public boolean canBeHurtBy(net.minecraft.world.damagesource.DamageSource source) {
		return false;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
		Tooltips.add(lines, Component.translatable(ModLang.STORAGE_RING_OLD).withStyle(ChatFormatting.GRAY));
	}
}
