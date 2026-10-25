package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * A sect's guest pass (the user's ask, 2026-10-24: "if you're too strong there is no way to get into a sect peacefully, make them sell a
 * temporary pass or something in the trade menu to let you through the barrier (you must keep it on you to pass though)"): a jade token
 * of the sect's ("PassSect", its name "PassSectName"), good until a time ("PassUntil", game time: {@link #LASTS} from when it was sold).
 * Carried anywhere about one (its inventory, a hand), the sect's barrier lets one through as it does its own (Formation#allows, on both
 * sides). Its people sell it (CultivatorTrades: one a day, to one not resentful), made out to whoever buys it ("PassHolder", their name
 * "PassHolderName": {@link #boundTo}), and it lets no one else through (the user's call, 2026-10-25: a pass handed on could bring in one the
 * sect hates); one made out to no one (a command's, a test's) lets anyone through. Turning on the sect (its alarm raised over one) takes
 * every pass of it one carries ({@link #revoke}).
 */
public class SectPassItem extends Item {
	private static final String SECT = "PassSect", NAME = "PassSectName", UNTIL = "PassUntil", HOLDER = "PassHolder", HOLDER_NAME = "PassHolderName";
	/** How long a pass is good for, from its sale: a day. */
	public static final int LASTS = 24000;
	/** What one costs (Low spirit stones, before regard). */
	public static final int PRICE = 30;

	public SectPassItem(Properties properties) {
		super(properties);
	}

	/** {@code sect}'s pass, good until {@code until} (game time). */
	public static ItemStack of(Sect sect, long until) {
		return of(sect.id, sect.name, until);
	}

	public static ItemStack of(UUID sect, String name, long until) {
		ItemStack stack = new ItemStack(ModItems.SECT_PASS);
		CompoundTag tag = stack.getOrCreateTag();
		tag.putUUID(SECT, sect);
		tag.putString(NAME, name);
		tag.putLong(UNTIL, until);
		return stack;
	}

	public static @Nullable UUID sect(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.hasUUID(SECT) ? tag.getUUID(SECT) : null;
	}

	public static long until(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? 0 : tag.getLong(UNTIL);
	}

	/** {@code pass} made out to {@code buyer} (a copy): it lets them through alone. */
	public static ItemStack boundTo(ItemStack pass, Player buyer) {
		ItemStack bound = pass.copy();
		CompoundTag tag = bound.getOrCreateTag();
		tag.putUUID(HOLDER, buyer.getUUID());
		tag.putString(HOLDER_NAME, buyer.getGameProfile().getName());
		return bound;
	}

	/** Whom {@code stack} is made out to, or null (no one: anyone may pass with it). */
	public static @Nullable UUID holder(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.hasUUID(HOLDER) ? tag.getUUID(HOLDER) : null;
	}

	/** Whether {@code stack} is a pass of {@code sect} still good at {@code now}. */
	public static boolean opens(ItemStack stack, UUID sect, long now) {
		return stack.is(ModItems.SECT_PASS) && sect.equals(sect(stack)) && until(stack) > now;
	}

	/** Whether {@code stack} is a pass of {@code sect} still good at {@code now} for {@code player}: made out to them, or to no one. */
	public static boolean opensFor(ItemStack stack, UUID sect, long now, Player player) {
		if (!opens(stack, sect, now)) return false;
		UUID holder = holder(stack);
		return holder == null || holder.equals(player.getUUID());
	}

	/** Whether {@code player} carries a good pass of {@code sect} anywhere about them, made out to them (Formation#allows). */
	public static boolean carries(Player player, UUID sect) {
		long now = player.level().getGameTime();
		for (ItemStack stack : player.getInventory().items) if (opensFor(stack, sect, now, player)) return true;
		for (ItemStack stack : player.getInventory().offhand) if (opensFor(stack, sect, now, player)) return true;
		return false;
	}

	/** {@code player} turned on {@code sect} (SectMembership#revokeGuest): every pass of it they carry crumbles. @return how many */
	public static int revoke(ServerPlayer player, UUID sect) {
		int taken = 0;
		for (List<ItemStack> list : List.of(player.getInventory().items, player.getInventory().offhand)) {
			for (int i = 0; i < list.size(); i++) {
				ItemStack stack = list.get(i);
				if (!stack.is(ModItems.SECT_PASS) || !sect.equals(sect(stack))) continue;
				taken += stack.getCount();
				list.set(i, ItemStack.EMPTY);
			}
		}
		if (taken > 0) player.displayClientMessage(Component.translatable(ModLang.MSG_PASS_REVOKED).withStyle(ChatFormatting.RED), true);
		return taken;
	}

	/** "Guest Pass of the Azure Cloud Sect". */
	@Override
	public Component getName(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null || !tag.contains(NAME) ? Component.translatable(getDescriptionId()) : Component.translatable(ModLang.PASS_NAME, tag.getString(NAME));
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		if (sect(stack) == null) {
			tooltip.add(Component.translatable(ModLang.PASS_BLANK).withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		tooltip.add(Component.translatable(ModLang.PASS_USE).withStyle(ChatFormatting.GRAY));
		CompoundTag tag = stack.getTag();
		if (tag != null && tag.contains(HOLDER_NAME)) tooltip.add(Component.translatable(ModLang.PASS_BOUND, tag.getString(HOLDER_NAME)).withStyle(ChatFormatting.GOLD));
		long left = level == null ? 0 : until(stack) - level.getGameTime();
		tooltip.add(left > 0 ? Component.translatable(ModLang.PASS_LEFT, (left + 1199) / 1200).withStyle(ChatFormatting.DARK_AQUA)
				: Component.translatable(ModLang.PASS_LAPSED).withStyle(ChatFormatting.RED));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return sect(stack) != null;
	}
}
