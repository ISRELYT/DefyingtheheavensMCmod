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
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ancient Essence (found only in the secret realms: SecretRealms, HerbValleyWorks): a bead of a vanished master's refined essence. Taken, it is
 * one more stat point to spend (PlayerCultivation#addEssencePoint, StatPoints#earned), kept for life.
 */
public class AncientEssenceItem extends Item {
	public AncientEssenceItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(player instanceof ServerPlayer p)) return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
		PlayerCultivation c = CultivationManager.own(p);
		c.addEssencePoint();
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
		if (!p.getAbilities().instabuild) stack.shrink(1);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_ESSENCE_TAKEN, StatPoints.unspent(c)).withStyle(ChatFormatting.GOLD));
		ServerLevel server = p.serverLevel();
		server.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.6f);
		server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.7, 0.4, 0.2);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(ModLang.TOOLTIP_ANCIENT_ESSENCE).withStyle(ChatFormatting.GRAY));
	}
}
