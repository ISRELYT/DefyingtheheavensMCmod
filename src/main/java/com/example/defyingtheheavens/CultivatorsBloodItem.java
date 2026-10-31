package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Cultivator's Blood (the user's pick, 2026-10-26, built with the bodies 2026-10-31): drawn with a glass bottle from a fresh cultivator's
 * body (Corpses#use), of the dead's realm, and whose it was. Drawing it pulls one toward the devilish path; its uses are the devilish
 * arts', still to come.
 */
public class CultivatorsBloodItem extends Item {
	private static final String REALM = "BloodRealm", OF = "BloodOf";

	public CultivatorsBloodItem(Properties properties) {
		super(properties);
	}

	/** The blood of one who died at {@code rank} (a realm and stage as one number), named {@code of}. */
	public static ItemStack of(int rank, String of) {
		ItemStack stack = new ItemStack(ModItems.CULTIVATORS_BLOOD);
		CompoundTag tag = stack.getOrCreateTag();
		tag.putInt(REALM, Math.max(0, rank) / Stage.values().length);
		tag.putString(OF, of);
		return stack;
	}

	/** Its realm, or null: none known (a bare stack). */
	public static @Nullable Realm realm(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null || !tag.contains(REALM) ? null : Realm.byIndex(tag.getInt(REALM));
	}

	@Override
	public Component getName(ItemStack stack) {
		Realm realm = realm(stack);
		return realm == null ? Component.translatable(getDescriptionId()) : Component.translatable(ModLang.CULTIVATORS_BLOOD_NAME, realm.getDisplayName());
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		CompoundTag tag = stack.getTag();
		if (tag != null && tag.contains(OF)) tooltip.add(Component.translatable(ModLang.CULTIVATORS_BLOOD_OF, tag.getString(OF)).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable(ModLang.CULTIVATORS_BLOOD_USE).withStyle(ChatFormatting.DARK_RED));
	}
}
