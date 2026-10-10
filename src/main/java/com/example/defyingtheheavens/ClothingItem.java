package com.example.defyingtheheavens;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cultivator clothing: a hair crown (guan), a robe, trousers and cloud boots, in five grades of craftsmanship
 * ({@link ClothingTier}) and dyed in the colours of a path ({@link ClothingStyle}, saved on the stack as {@link #STYLE_TAG};
 * blue when unset). Worn on the armor slots. Besides armor matching vanilla's tier for tier, every piece adds to qi
 * gathering ({@link #qiGatherBonus}) and to cultivation while meditating ({@link #meditationBonus}); boots also add speed.
 * <p>
 * Sect members wear their sect's colours, the higher their title the finer the grade. Players craft a Mortal-grade set from
 * wool, dye it with the {@code clothing_dye} recipe and raise its grade with the {@code clothing_upgrade} recipe (see
 * {@link ClothingRecipes}).
 */
public class ClothingItem extends ArmorItem {
	public static final String STYLE_TAG = "ClothingStyle";
	/** Vanilla's per-slot armor modifier ids, so clothing replaces armor in a slot like any armor would. */
	private static final Map<ArmorItem.Type, UUID> SLOT_IDS = new EnumMap<>(Map.of(
			ArmorItem.Type.BOOTS, UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"),
			ArmorItem.Type.LEGGINGS, UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"),
			ArmorItem.Type.CHESTPLATE, UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"),
			ArmorItem.Type.HELMET, UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150")));
	private static final UUID BOOTS_SPEED_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4c01");

	private final ClothingTier tier;
	private final Multimap<Attribute, AttributeModifier> modifiers;

	public ClothingItem(ClothingTier tier, ArmorItem.Type type, Item.Properties properties) {
		super(tier, type, properties);
		this.tier = tier;
		UUID id = SLOT_IDS.get(type);
		ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
		builder.put(Attributes.ARMOR, new AttributeModifier(id, "Armor modifier", getDefense(), AttributeModifier.Operation.ADDITION));
		builder.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, "Armor toughness", getToughness(), AttributeModifier.Operation.ADDITION));
		if (tier.getKnockbackResistance() > 0) {
			builder.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(id, "Armor knockback resistance", tier.getKnockbackResistance(),
					AttributeModifier.Operation.ADDITION));
		}
		if (type == ArmorItem.Type.BOOTS) {
			builder.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(BOOTS_SPEED_ID, "Cloud boots speed", tier.getBootsSpeed(),
					AttributeModifier.Operation.MULTIPLY_BASE));
		}
		this.modifiers = builder.build();
	}

	public ClothingTier getTier() { return tier; }

	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
		return slot == type.getSlot() ? modifiers : ImmutableMultimap.of();
	}

	public static ClothingStyle style(ItemStack stack) {
		return stack.hasTag() && stack.getTag().contains(STYLE_TAG) ? ClothingStyle.byIndex(stack.getTag().getInt(STYLE_TAG)) : ClothingStyle.NEUTRAL;
	}

	public static ItemStack withStyle(ItemStack stack, ClothingStyle style) {
		stack.getOrCreateTag().putInt(STYLE_TAG, style.ordinal());
		return stack;
	}

	public static ItemStack create(Item item, ClothingStyle style) {
		return withStyle(new ItemStack(item), style);
	}

	/** Share added to qi gathering by everything {@code entity} wears (0.6 at most: a full Immortal set). */
	public static double qiGatherBonus(LivingEntity entity) {
		double bonus = 0;
		for (ItemStack stack : entity.getArmorSlots()) {
			if (stack.getItem() instanceof ClothingItem clothing) bonus += clothing.tier.getQiGatherPerPiece();
		}
		return bonus;
	}

	/** Share added to cultivation while meditating by everything {@code entity} wears. */
	public static double meditationBonus(LivingEntity entity) {
		double bonus = 0;
		for (ItemStack stack : entity.getArmorSlots()) {
			if (stack.getItem() instanceof ClothingItem clothing) bonus += clothing.tier.getMeditationPerPiece();
		}
		return bonus;
	}

	@Override
	public Component getName(ItemStack stack) {
		return super.getName(stack).copy().withStyle(tier.getColor());
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
		ClothingStyle style = style(stack);
		lines.add(style.getDisplayName().copy().withStyle(s -> s.withColor(style == ClothingStyle.DEMONIC ? style.getAccent() : style.getCloth())));
		lines.add(Component.translatable(ModLang.CLOTHING_QI, Math.round(tier.getQiGatherPerPiece() * 100)).withStyle(ChatFormatting.AQUA));
		lines.add(Component.translatable(ModLang.CLOTHING_MEDITATION, Math.round(tier.getMeditationPerPiece() * 100)).withStyle(ChatFormatting.GREEN));
		if (!tier.isLast()) lines.add(Component.translatable(ModLang.CLOTHING_UPGRADE_HINT).withStyle(ChatFormatting.DARK_GRAY));
	}
}
