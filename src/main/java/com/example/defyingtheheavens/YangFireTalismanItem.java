package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Yang Fire Talisman (阳火符): paper, blaze powder and a gold nugget, painted with the Inscription Brush. Burnt (used), its yang
 * fire drives a third of the yin out of a living body (see YinCorruption#TALISMAN_CLEANSE). It does nothing for a body with no
 * yin in it, and isn't spent then.
 */
public class YangFireTalismanItem extends Item {
	/** A moment between talismans, so one click never burns two. */
	private static final int COOLDOWN_TICKS = 10;

	public YangFireTalismanItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack); // the server decides
		PlayerCultivation c = CultivationManager.get(sp);
		double burnt = YinCorruption.cleanse(c, YinCorruption.TALISMAN_CLEANSE);
		if (burnt <= 0) {
			sp.displayClientMessage(Component.translatable(ModLang.MSG_YIN_NONE), true);
			return InteractionResultHolder.fail(stack);
		}
		if (!sp.getAbilities().instabuild) stack.shrink(1);
		sp.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
		CultivationManager.markDirty(sp.server);
		NetherworldManager.refresh(sp);
		level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
		if (level instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.FLAME, sp.getX(), sp.getY() + 1.0, sp.getZ(), 24, 0.35, 0.6, 0.35, 0.02);
			server.sendParticles(ParticleTypes.WAX_OFF, sp.getX(), sp.getY() + 1.2, sp.getZ(), 10, 0.4, 0.5, 0.4, 0.5);
		}
		sp.displayClientMessage(Component.translatable(ModLang.MSG_YIN_CLEANSED, Math.round(burnt * 100),
				Math.round(c.netherworld().yinCorruption() * 100)).withStyle(ChatFormatting.GOLD), true);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
		lines.add(Component.translatable(ModLang.TOOLTIP_YANG_FIRE_TALISMAN).withStyle(ChatFormatting.GRAY));
	}
}
