package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Ghost-Quelling Talisman: yellow paper, cinnabar (redstone) and Yin Jade, painted with the Inscription Brush (two to a
 * painting). Thrown, it flies past the living and strikes only the dead (a player's ghost, a ghost cultivator, a Hungry
 * Ghost): {@link #DAMAGE} of the dead's own harm, which lands in full, and it binds them where they stand for
 * {@link #BIND_TICKS} (QuelledEffect), sealed to the brow. A ghost can't throw one (the dead use no items); it hurts another
 * player's ghost only where PvP is on.
 */
public class GhostQuellingTalismanItem extends Item {
	public static final float DAMAGE = 6.0f;
	public static final int BIND_TICKS = 5 * 20;
	/** How hard it's thrown (a snowball's 1.5) and a moment between throws. */
	private static final float SPEED = 1.5f;
	private static final int COOLDOWN_TICKS = 10;

	public GhostQuellingTalismanItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5f,
				0.6f + level.getRandom().nextFloat() * 0.2f);
		if (!level.isClientSide) {
			GhostQuellingTalismanEntity talisman = new GhostQuellingTalismanEntity(level, player);
			talisman.setItem(stack);
			talisman.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, SPEED, 1.0f);
			level.addFreshEntity(talisman);
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
		if (!player.getAbilities().instabuild) stack.shrink(1);
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	/**
	 * The talisman strikes {@code target}, one of the dead: the harm, and if it is still there, the binding. Returns whether it
	 * struck (PvP off, say, and a player's ghost is spared).
	 */
	public static boolean strike(LivingEntity target, Entity talisman, Entity owner) {
		if (!(target.level() instanceof ServerLevel level) || !GhostSystem.isDead(target)) return false;
		if (!target.hurt(ModDamageTypes.ghostly(level, talisman, owner), DAMAGE)) return false;
		if (target.isAlive()) quell(target);
		if (owner instanceof ServerPlayer thrower && thrower != target) {
			thrower.displayClientMessage(Component.translatable(ModLang.MSG_QUELLED_OTHER, target.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
		}
		return true;
	}

	/** Binds one of the dead where it stands, and shows everyone near the talisman on its brow. */
	public static void quell(LivingEntity target) {
		target.addEffect(new MobEffectInstance(ModEffects.QUELLED, BIND_TICKS, 0, false, false, true));
		ModPackets.broadcastQuelled(target, BIND_TICKS);
		if (target instanceof ServerPlayer ghost) {
			ghost.displayClientMessage(Component.translatable(ModLang.MSG_QUELLED).withStyle(ChatFormatting.GOLD), true);
		}
		if (target.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.WAX_ON, target.getX(), target.getEyeY(), target.getZ(), 16, 0.3, 0.3, 0.3, 0.6);
			level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + 1.0, target.getZ(), 30, 0.4, 0.7, 0.4, 0.4);
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 1.4f);
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0f, 0.6f);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
		lines.add(Component.translatable(ModLang.TOOLTIP_GHOST_QUELLING_TALISMAN, BIND_TICKS / 20).withStyle(ChatFormatting.GRAY));
	}
}
