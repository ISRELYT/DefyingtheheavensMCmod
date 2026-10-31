package com.example.defyingtheheavens;

import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Sleeping Sword, half of the Sunken Sword Tomb's Great Inheritance (SwordTombWorks, 2026-10-31): a master's jian of Perfect work that
 * grows with whoever bears it, its grade following their realm ({@link #gradeFor}: Spirit to Core Formation, Earth for Nascent Soul and
 * Heavenly Being, Heaven for Four Axis and Dao Integration, Immortal from Void Shattering), its blow and might a flying sword's of that
 * grade, and ridden (SwordFlight) it saves its rider that grade's share of the flight's qi (ArtifactWeaponItem#flightShare).
 */
public class SleepingSwordItem extends ArtifactWeaponItem {
	private static final String GRADE = "SleepingGrade";
	/** Its blow, swing and might at each grade (Perfect work). */
	private final List<Multimap<Attribute, AttributeModifier>> byGrade = new ArrayList<>();

	public SleepingSwordItem(Properties properties) {
		super(ArtifactKind.FLYING_SWORD, ArtifactGrade.SPIRIT, properties);
		for (ArtifactGrade grade : ArtifactGrade.values()) byGrade.add(modifiersFor(ArtifactKind.FLYING_SWORD, grade, Artifacts.Quality.PERFECT.power()));
	}

	/** A Sleeping Sword as its stone gives it up: of Perfect work. */
	public static ItemStack create() {
		return Artifacts.withQuality(new ItemStack(ModItems.SLEEPING_SWORD), Artifacts.Quality.PERFECT);
	}

	/** The grade it takes for a bearer of true rank {@code rank} (either path's). */
	public static ArtifactGrade gradeFor(int rank) {
		int realm = CultivatorNpc.realmOf(rank).ordinal();
		if (realm >= Realm.VOID_SHATTERING.ordinal()) return ArtifactGrade.IMMORTAL;
		if (realm >= Realm.FOUR_AXIS.ordinal()) return ArtifactGrade.HEAVEN;
		if (realm >= Realm.NASCENT_SOUL.ordinal()) return ArtifactGrade.EARTH;
		return ArtifactGrade.SPIRIT;
	}

	@Override
	public ArtifactGrade grade(ItemStack stack) {
		int g = stack.getTag() != null && stack.getTag().contains(GRADE) ? stack.getTag().getInt(GRADE) : ArtifactGrade.SPIRIT.ordinal();
		return ArtifactGrade.values()[Math.max(0, Math.min(ArtifactGrade.values().length - 1, g))];
	}

	/** Borne: its grade follows its bearer's realm (looked at once a second). */
	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		super.inventoryTick(stack, level, entity, slot, selected);
		if (level.isClientSide || !(entity instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
		ArtifactGrade grade = gradeFor(TwoPaths.higherRank(CultivationManager.get(p), true));
		if (stack.getTag() == null || !stack.getTag().contains(GRADE) || grade(stack) != grade) stack.getOrCreateTag().putInt(GRADE, grade.ordinal());
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getAttributeModifiers(ItemStack stack, EquipmentSlot slot) {
		return slot == EquipmentSlot.MAINHAND ? byGrade.get(grade(stack).ordinal()) : getDefaultAttributeModifiers(slot);
	}

	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable(getDescriptionId(stack)).withStyle(grade(stack).colour());
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
		ArtifactGrade grade = grade(stack);
		Artifacts.describe(stack, grade, lines);
		Tooltips.add(lines, Component.translatable(ModLang.TOOLTIP_SLEEPING_SWORD_GROWS, grade.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
		Tooltips.add(lines, Component.translatable(ModLang.FLYING_SWORD_PERK, Artifacts.percent(flightSaving(grade) * Artifacts.power(stack))).withStyle(ChatFormatting.BLUE));
		Tooltips.add(lines, Component.translatable(ModLang.TOOLTIP_SLEEPING_SWORD).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
	}
}
