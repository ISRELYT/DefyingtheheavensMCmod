package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A qi gourd, forged at the Artifact Forge (the user's pick, 2026-10-28): it keeps spare qi for later. Sneak and right-click to pour
 * your qi into it (as much as it has room for); right-click to drink it back (as much as your pool has room for), a gulp's while.
 * How much it holds goes by its grade (a Qi Refining cultivator's whole pool at Mortal grade, a Void Shattering one's at Immortal), its
 * work's worth with it; the bar on it shows how full it is.
 */
public class QiGourdItem extends Item {
	/** The qi a gourd holds, by grade. */
	private static final double[] CAPACITY = {100, 300, 1500, 8000, 60000};
	private static final String HELD = "GourdQi";
	/** Ticks a gulp takes. */
	private static final int GULP = 24;

	private final ArtifactGrade grade;

	public QiGourdItem(ArtifactGrade grade, Properties properties) {
		super(properties);
		this.grade = grade;
	}

	public ArtifactGrade grade() { return grade; }

	public static double capacity(ItemStack stack) {
		return stack.getItem() instanceof QiGourdItem gourd ? CAPACITY[gourd.grade.ordinal()] * Artifacts.power(stack) : 0;
	}

	public static double held(ItemStack stack) {
		return stack.hasTag() ? Math.max(0, stack.getTag().getDouble(HELD)) : 0;
	}

	static void setHeld(ItemStack stack, double qi) {
		if (qi <= 0) {
			if (stack.hasTag()) stack.getTag().remove(HELD);
		} else {
			stack.getOrCreateTag().putDouble(HELD, qi);
		}
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isShiftKeyDown()) {
			if (player instanceof ServerPlayer server) pour(server, stack);
			return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
		}
		PlayerCultivation c = CultivationManager.forPlayer(player);
		if (held(stack) <= 0 || c.getQi() >= c.maxQi()) {
			player.displayClientMessage(Component.translatable(held(stack) <= 0 ? ModLang.GOURD_EMPTY : ModLang.GOURD_POOL_FULL), true);
			return InteractionResultHolder.fail(stack);
		}
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	/** {@code player}'s qi poured in: as much as it has room for, as much as they hold. */
	public static void pour(ServerPlayer player, ItemStack stack) {
		PlayerCultivation c = CultivationManager.forPlayer(player);
		double room = capacity(stack) - held(stack);
		double poured = Math.min(room, c.getQi());
		if (poured < 1) {
			player.displayClientMessage(Component.translatable(room < 1 ? ModLang.GOURD_FULL : ModLang.GOURD_NO_QI), true);
			return;
		}
		if (!c.consumeQi(poured)) return;
		setHeld(stack, held(stack) + poured);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.8f, 0.8f);
		player.displayClientMessage(Component.translatable(ModLang.GOURD_POURED, Math.round(poured), Math.round(held(stack))), true);
		CultivationManager.sync(player);
	}

	/** A gulp from it: as much as {@code player}'s pool has room for. */
	public static void drink(ServerPlayer player, ItemStack stack) {
		PlayerCultivation c = CultivationManager.forPlayer(player);
		double drunk = Math.min(held(stack), c.maxQi() - c.getQi());
		if (drunk <= 0) return;
		c.setQi(c.getQi() + drunk);
		setHeld(stack, held(stack) - drunk);
		player.displayClientMessage(Component.translatable(ModLang.GOURD_DRUNK, Math.round(drunk), Math.round(held(stack))), true);
		CultivationManager.sync(player);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (entity instanceof ServerPlayer player) drink(player, stack);
		return stack;
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return GULP;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.DRINK;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return held(stack) > 0;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		double capacity = capacity(stack);
		return capacity <= 0 ? 0 : (int) Math.round(13 * Math.min(1, held(stack) / capacity));
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0x4FA8E8; // the qi bar's azure
	}

	@Override
	public Component getName(ItemStack stack) {
		return Artifacts.name(super.getName(stack), stack, grade);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return grade == ArtifactGrade.IMMORTAL || Artifacts.quality(stack) == Artifacts.Quality.PERFECT;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
		Artifacts.describe(stack, grade, lines);
		Tooltips.add(lines, Component.translatable(ModLang.GOURD_HOLDS, Math.round(held(stack)), Math.round(capacity(stack))).withStyle(ChatFormatting.AQUA));
		Tooltips.add(lines, Component.translatable(ModLang.GOURD_USE).withStyle(ChatFormatting.GRAY));
	}
}
