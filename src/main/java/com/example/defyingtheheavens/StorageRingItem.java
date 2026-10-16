package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * A Storage Ring: a space folded into a ring (its contents: StorageRings). Right-click opens it; sneaking, it goes on a finger
 * like any ring (RingItem), and the Storage Ring key opens the one worn. Each ring carries its own id (given the first time it
 * is opened) and its grade ({@link StorageRings.Grade}): crafted at Mortal grade, refined in its own screen up to Boundless,
 * which holds without end.
 */
public class StorageRingItem extends RingItem {
	private static final String ID_TAG = "StorageRing";
	private static final String GRADE_TAG = "StorageGrade";

	public StorageRingItem(Properties properties) {
		super(properties);
	}

	/** The ring's space's id, or null if it has never been opened. */
	public static UUID idOf(ItemStack stack) {
		if (!(stack.getItem() instanceof StorageRingItem) || !stack.hasTag() || !stack.getTag().hasUUID(ID_TAG)) return null;
		return stack.getTag().getUUID(ID_TAG);
	}

	/** The ring's space's id, given it now if it has none. */
	public static UUID ensureId(ItemStack stack) {
		UUID id = idOf(stack);
		if (id != null) return id;
		id = UUID.randomUUID();
		stack.getOrCreateTag().putUUID(ID_TAG, id);
		return id;
	}

	/** Whether {@code stack} is the ring {@code id} (the one whose screen is open: it can't be moved while it is). */
	public static boolean isOpen(ItemStack stack, UUID id) {
		return id != null && id.equals(idOf(stack));
	}

	public static StorageRings.Grade grade(ItemStack stack) {
		return StorageRings.Grade.byIndex(stack.hasTag() ? stack.getTag().getInt(GRADE_TAG) : 0);
	}

	public static void setGrade(ItemStack stack, StorageRings.Grade grade) {
		stack.getOrCreateTag().putInt(GRADE_TAG, grade.ordinal());
	}

	/** A ring of {@code grade}, its space not yet made. */
	public static ItemStack of(StorageRings.Grade grade) {
		ItemStack stack = new ItemStack(ModItems.STORAGE_RING);
		if (grade.ordinal() > 0) setGrade(stack, grade);
		return stack;
	}

	/** Right-click: opens the ring's space. Sneaking: puts it on, as any ring. */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isShiftKeyDown()) return super.use(level, player, hand);
		if (player instanceof ServerPlayer sp) StorageRings.open(sp, stack);
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public Component getName(ItemStack stack) {
		StorageRings.Grade grade = grade(stack);
		return Component.translatable(grade.boundless() ? ModLang.STORAGE_RING_BOUNDLESS : ModLang.STORAGE_RING_NAME, grade.getDisplayName())
				.withStyle(grade.colour());
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return grade(stack).boundless();
	}

	/** Lying on the ground, a ring and the space folded in it are proof against fire, cactus and blasts (only the void takes them). */
	@Override
	public boolean canBeHurtBy(net.minecraft.world.damagesource.DamageSource source) {
		return false;
	}

	/** A ring lying on the ground never despawns (ENTITY_LOAD): a whole space must not vanish with a five-minute timer. */
	public static void onItemLoad(net.minecraft.world.entity.Entity entity) {
		if (entity instanceof net.minecraft.world.entity.item.ItemEntity item && item.getItem().getItem() instanceof StorageRingItem) {
			item.setUnlimitedLifetime();
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
		StorageRings.Grade grade = grade(stack);
		Tooltips.add(lines, (grade.boundless() ? Component.translatable(ModLang.STORAGE_TOOLTIP_BOUNDLESS)
				: Component.translatable(ModLang.STORAGE_TOOLTIP_STACKS, grade.stacks())).withStyle(ChatFormatting.AQUA));
		Tooltips.add(lines, Component.translatable(ModLang.STORAGE_TOOLTIP_USE).withStyle(ChatFormatting.GRAY));
	}
}
