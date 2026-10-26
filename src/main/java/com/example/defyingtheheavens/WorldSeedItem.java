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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A World Seed (phase 3d): Chaos Origin Stones and Immortal Jade made one, the seed of a small world. Held in use by a True Immortal, or
 * one past it, for {@link SmallWorlds#REFINE_TICKS} (chaos drawn into it, turning), it is refined into a world of their own and spent
 * (SmallWorlds#refine); one each. Refused at once, before the holding, to one who can't (SmallWorlds#whyNotRefine).
 */
public class WorldSeedItem extends Item {
	public WorldSeedItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player instanceof ServerPlayer p) {
			Component why = SmallWorlds.whyNotRefine(p);
			if (why != null) {
				p.displayClientMessage(why.copy().withStyle(ChatFormatting.LIGHT_PURPLE), true);
				return InteractionResultHolder.fail(stack);
			}
			p.displayClientMessage(Component.translatable(ModLang.MSG_SEED_REFINING, SmallWorlds.REFINE_TICKS / 20).withStyle(ChatFormatting.LIGHT_PURPLE), true);
		}
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	/** Chaos drawn into it as it is refined, turning about it; a chime each second. */
	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
		if (!(level instanceof ServerLevel server)) return;
		double t = (getUseDuration(stack) - remaining) * 0.25;
		for (int i = 0; i < 2; i++) {
			double a = t + i * Math.PI;
			Vec3 at = entity.position().add(Math.cos(a) * 1.4, 1.1 + Math.sin(t * 0.7) * 0.3, Math.sin(a) * 1.4);
			Vec3 in = entity.position().add(0, 1.2, 0).subtract(at).scale(0.08);
			server.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 0, in.x, in.y, in.z, 1.0);
		}
		if (remaining % 20 == 0) server.playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 0.5f + 0.5f * (1 - remaining / (float) getUseDuration(stack)));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (entity instanceof ServerPlayer p && SmallWorlds.refine(p) && !p.getAbilities().instabuild) stack.shrink(1);
		return stack;
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return SmallWorlds.REFINE_TICKS;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BOW;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(ModLang.TOOLTIP_WORLD_SEED, SmallWorlds.REFINE_TICKS / 20).withStyle(ChatFormatting.GRAY));
	}
}
