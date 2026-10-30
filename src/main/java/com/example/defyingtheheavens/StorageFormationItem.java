package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.UUID;

/**
 * A storage formation as an item (StorageFormations): crafted (Low), or taken up again by its owner, empty (StorageFormationBlock). One
 * packed up before formations stayed put (2026-10-30), or an old Storage Ring unfolded, carries the id of the space it holds
 * ({@link #SPACE_TAG}), so set down it holds all it held. Lying on the ground it is proof against fire and blasts and never despawns, as
 * the rings were: a space must not vanish with a five-minute timer.
 */
public class StorageFormationItem extends BlockItem {
	/** The space a packed formation holds (StorageSpaces). */
	public static final String SPACE_TAG = "StorageSpace";

	private final StorageFormations.Tier tier;

	public StorageFormationItem(Block block, StorageFormations.Tier tier, Properties properties) {
		super(block, properties);
		this.tier = tier;
	}

	public StorageFormations.Tier tier() { return tier; }

	/** A formation of {@code tier}, holding no space yet. */
	public static ItemStack of(StorageFormations.Tier tier) {
		return new ItemStack(tier.block().asItem());
	}

	/** The space {@code stack} holds (one packed up before 2026-10-30, or an old ring unfolded), or null. */
	public static UUID spaceOf(ItemStack stack) {
		if (!(stack.getItem() instanceof StorageFormationItem) || !stack.hasTag() || !stack.getTag().hasUUID(SPACE_TAG)) return null;
		return stack.getTag().getUUID(SPACE_TAG);
	}

	public static void setSpace(ItemStack stack, UUID space) {
		stack.getOrCreateTag().putUUID(SPACE_TAG, space);
	}

	/** Crafted: practice in formations (CraftMastery). */
	@Override
	public void onCraftedBy(ItemStack stack, Level level, Player player) {
		super.onCraftedBy(stack, level, player);
		if (player instanceof ServerPlayer sp) CraftMastery.practise(sp, CraftMastery.Craft.FORMATIONS, tier.practice() * Math.max(1, stack.getCount()));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return tier.boundless() || spaceOf(stack) != null;
	}

	@Override
	public boolean canBeHurtBy(net.minecraft.world.damagesource.DamageSource source) {
		return false;
	}

	/** One lying on the ground never despawns (ENTITY_LOAD). */
	public static void onItemLoad(net.minecraft.world.entity.Entity entity) {
		if (entity instanceof net.minecraft.world.entity.item.ItemEntity item && (item.getItem().getItem() instanceof StorageFormationItem
				|| item.getItem().getItem() instanceof StorageRingItem)) {
			item.setUnlimitedLifetime();
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
		describe(tier, stack, lines);
	}

	/** Its size, its seal, whether it holds a space, and how to use it. */
	static void describe(StorageFormations.Tier tier, ItemStack stack, List<Component> lines) {
		Tooltips.add(lines, (tier.boundless() ? Component.translatable(ModLang.STORAGE_TOOLTIP_BOUNDLESS)
				: Component.translatable(ModLang.STORAGE_TOOLTIP_STACKS, tier.stacks())).withStyle(ChatFormatting.AQUA));
		Tooltips.add(lines, Component.translatable(ModLang.STORAGE_TOOLTIP_SEAL, tier.seal().getDisplayName()).withStyle(ChatFormatting.GOLD));
		if (spaceOf(stack) != null) Tooltips.add(lines, Component.translatable(ModLang.STORAGE_TOOLTIP_HOLDS).withStyle(ChatFormatting.LIGHT_PURPLE));
		Tooltips.add(lines, Component.translatable(ModLang.STORAGE_TOOLTIP_USE).withStyle(ChatFormatting.GRAY));
	}
}
