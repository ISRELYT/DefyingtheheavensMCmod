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
 * A Departure Jade (SecretRealms: the one way out of a secret realm before it closes): held up and crushed inside one, its light gathers for
 * SecretRealms#DEPARTURE_TICKS and carries its holder out beside the realm's Seal, used up; a blow taken meanwhile breaks the light (held
 * up again, it begins again). Outside a secret realm nothing stirs in it. Found in the realms' vaults, and in a sect's exchange.
 */
public class DepartureJadeItem extends Item {
	public DepartureJadeItem(Properties properties) {
		super(properties);
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return SecretRealms.DEPARTURE_TICKS;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BOW;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!ModDimensions.isSecretRealm(level.dimension())) {
			if (!level.isClientSide) player.displayClientMessage(Component.translatable(ModLang.MSG_JADE_OUTSIDE).withStyle(ChatFormatting.GRAY), true);
			return InteractionResultHolder.fail(stack);
		}
		player.startUsingItem(hand);
		if (!level.isClientSide) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_JADE_GATHERS, SecretRealms.DEPARTURE_TICKS / 20).withStyle(ChatFormatting.AQUA), true);
			level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.4f);
		}
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remaining) {
		if (!(level instanceof ServerLevel server)) return;
		if (living.hurtTime > 0) {
			living.stopUsingItem();
			if (living instanceof ServerPlayer p) p.displayClientMessage(Component.translatable(ModLang.MSG_JADE_BROKEN).withStyle(ChatFormatting.RED), true);
			server.playSound(null, living.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0f, 1.2f);
			return;
		}
		if (remaining % 4 == 0) {
			double t = 1 - remaining / (double) getUseDuration(stack);
			server.sendParticles(ParticleTypes.END_ROD, living.getX(), living.getY() + 1.0, living.getZ(), 2 + (int) (t * 8), 0.4, 0.8, 0.4, 0.02 + t * 0.05);
		}
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
		if (!level.isClientSide && living instanceof ServerPlayer p && SecretRealms.depart(p) && !p.getAbilities().instabuild) stack.shrink(1);
		return stack;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(ModLang.TOOLTIP_DEPARTURE_JADE, SecretRealms.DEPARTURE_TICKS / 20).withStyle(ChatFormatting.GRAY));
	}
}
