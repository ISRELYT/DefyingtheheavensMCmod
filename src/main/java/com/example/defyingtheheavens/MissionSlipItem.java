package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * A mission taken at a sect's board (SectMissions): the Mission Slip, or for a delivery the Sealed Letter itself. It names its mission and
 * who took it on; held (either hand), it shows the way over the hotbar (SectMissions#pointer); its mission over (seen through, abandoned,
 * lapsed, its taker gone from the sect), it crumbles. A letter is handed to any of its sect's people (talk: Ask, "Deliver the sealed letter").
 */
public class MissionSlipItem extends Item {
	private static final String SECT = "Sect", OWNER = "Owner", OWNER_NAME = "OwnerName", MISSION = "Mission", TITLE = "Title", SECT_NAME = "SectName",
			TO = "To";

	public MissionSlipItem(Properties properties) {
		super(properties);
	}

	/** The slip (or letter: {@code item}) of {@code m}, taken by {@code owner} of {@code sect}. */
	static ItemStack make(Item item, Sect sect, ServerPlayer owner, SectMissions.Mission m) {
		ItemStack stack = new ItemStack(item);
		CompoundTag tag = stack.getOrCreateTag();
		tag.putUUID(SECT, sect.id);
		tag.putString(SECT_NAME, sect.name);
		tag.putUUID(OWNER, owner.getUUID());
		tag.putString(OWNER_NAME, owner.getGameProfile().getName());
		tag.putInt(MISSION, m.id);
		tag.putString(TITLE, Component.Serializer.toJson(SectMissions.title(m)));
		if (m.kind == SectMissions.Kind.DELIVER) tag.putString(TO, m.what);
		return stack;
	}

	public static @Nullable UUID sect(ItemStack stack) {
		return stack.hasTag() && stack.getTag().hasUUID(SECT) ? stack.getTag().getUUID(SECT) : null;
	}

	public static @Nullable UUID owner(ItemStack stack) {
		return stack.hasTag() && stack.getTag().hasUUID(OWNER) ? stack.getTag().getUUID(OWNER) : null;
	}

	public static int mission(ItemStack stack) {
		return stack.hasTag() ? stack.getTag().getInt(MISSION) : 0;
	}

	/** Whether {@code stack} is the slip (or letter) of {@code owner}'s mission {@code id}. */
	static boolean isOf(ItemStack stack, UUID owner, int id) {
		return owner.equals(owner(stack)) && mission(stack) == id;
	}

	/** Once a second: its mission over, it crumbles; held, it shows the way. */
	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		if (!(level instanceof ServerLevel server) || !(entity instanceof ServerPlayer player) || level.getGameTime() % 20 != 0) return;
		SectMissions.Found found = SectMissions.ofSlip(server.getServer(), stack);
		if (found == null) {
			stack.setCount(0);
			player.displayClientMessage(Component.translatable(ModLang.MSG_SLIP_CRUMBLES).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		if (selected || player.getOffhandItem() == stack) player.displayClientMessage(SectMissions.pointer(player, found), true);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
		CompoundTag tag = stack.getTag();
		if (tag == null || !tag.hasUUID(SECT)) return;
		Component title = Component.Serializer.fromJson(tag.getString(TITLE));
		if (title != null) Tooltips.add(lines, title.copy().withStyle(ChatFormatting.GOLD));
		Tooltips.add(lines, Component.translatable(ModLang.MISSION_SLIP_OF, tag.getString(SECT_NAME), tag.getString(OWNER_NAME)).withStyle(ChatFormatting.GRAY));
		Tooltips.add(lines, tag.contains(TO) ? Component.translatable(ModLang.SEALED_LETTER_HINT, tag.getString(TO)).withStyle(ChatFormatting.DARK_GRAY)
				: Component.translatable(ModLang.MISSION_SLIP_HINT).withStyle(ChatFormatting.DARK_GRAY));
	}
}
