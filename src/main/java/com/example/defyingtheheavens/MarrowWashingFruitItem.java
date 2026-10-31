package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
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
 * The Marrow-Washing Fruit, half of the Cave Abode's Great Inheritance (CaveAbodeWorks, 2026-10-31): the one fruit of its master's garden.
 * Eaten, it washes the eater's marrow: its physique a grade higher (PlayerCultivation#physiqueGrade: its talent and its pace that grade's),
 * once in a life (an Eternal Reincarnation Body keeps it with its body). Not for one whose marrow is washed already, a physique of the
 * Immortal grade (none higher), a mortal, one of the dead, or a soul in another's body.
 */
public class MarrowWashingFruitItem extends Item {
	public MarrowWashingFruitItem(Properties properties) {
		super(properties);
	}

	/** Why {@code c} may not eat it now (null: it may). The same on both sides. */
	static @Nullable Component why(PlayerCultivation c, boolean elsewhere) {
		if (elsewhere) return Component.translatable(ModLang.MSG_BLOOD_ELSEWHERE);
		if (c.netherworld().ghost()) return Component.translatable(ModLang.MSG_BLOOD_DEAD);
		if (c.isMortal() || c.getPhysique() == null) return Component.translatable(ModLang.MSG_BLOOD_MORTAL);
		if (c.isMarrowWashed()) return Component.translatable(ModLang.MSG_MARROW_ALREADY);
		if (c.physiqueGrade() == Physique.Grade.IMMORTAL) return Component.translatable(ModLang.MSG_MARROW_HIGHEST);
		return null;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		Component why = why(CultivationManager.forPlayer(player), false);
		if (why != null) {
			if (!level.isClientSide) player.displayClientMessage(why.copy().withStyle(ChatFormatting.GRAY), true);
			return InteractionResultHolder.fail(stack);
		}
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 40;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.EAT;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!(entity instanceof ServerPlayer player)) return stack;
		PlayerCultivation c = CultivationManager.own(player);
		Component why = why(c, CultivationManager.get(player) != c);
		if (why != null) {
			player.displayClientMessage(why.copy().withStyle(ChatFormatting.GRAY), true);
			return stack;
		}
		c.setMarrowWashed(true);
		CultivationManager.markDirty(player.server);
		CultivationManager.sync(player);
		player.sendSystemMessage(Component.translatable(ModLang.MSG_MARROW_WASHED, c.physique().getDisplayName(), c.physiqueGrade().getDisplayName())
				.withStyle(ChatFormatting.GOLD));
		player.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 50, 0.5, 0.9, 0.5, 0.3);
		player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 0.8f);
		Journal.note(player, "marrow_washed", ModLang.JOURNAL_MARROW_WASHED, Component.translatable(ModLang.JOURNAL_MARROW_WASHED_TEXT, c.physique().getDisplayName(),
				c.physiqueGrade().getDisplayName()));
		player.awardStat(Stats.ITEM_USED.get(this));
		if (!player.getAbilities().instabuild) stack.shrink(1);
		return stack;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
		Tooltips.add(lines, Component.translatable(ModLang.TOOLTIP_MARROW_FRUIT).withStyle(ChatFormatting.GRAY));
	}
}
