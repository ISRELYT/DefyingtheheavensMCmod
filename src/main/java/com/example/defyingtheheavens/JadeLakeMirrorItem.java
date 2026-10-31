package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Jade Lake Mirror, the Jade Lake Mirage's Great Inheritance (MirageLakeWorks): raised (used), for {@link #RAISED_TICKS} the next
 * {@link #TURNS} blows and spells that strike its bearer are turned back on whoever struck them, whole, and the bearer takes nothing of
 * them (ModEffects#MIRROR_RAISED, its turns left its amplifier); then it rests {@link #REST_TICKS} before it may be raised again.
 */
public class JadeLakeMirrorItem extends Item {
	/** How long it stays raised, how many blows it turns, how long it rests after (ticks). */
	public static final int RAISED_TICKS = 200, TURNS = 3, REST_TICKS = 1200;
	/** Set while a blow is being turned back, so a mirror never turns back a blow turned back at it. */
	private static boolean turning;

	public JadeLakeMirrorItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level instanceof ServerLevel server) {
			player.addEffect(new MobEffectInstance(ModEffects.MIRROR_RAISED, RAISED_TICKS, TURNS - 1, false, true, true));
			player.getCooldowns().addCooldown(this, REST_TICKS);
			server.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.4f, 1.2f);
			server.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.2, player.getZ(), 24, 0.6, 0.8, 0.6, 0.04);
			player.displayClientMessage(Component.translatable(ModLang.MSG_MIRROR_RAISED).withStyle(ChatFormatting.AQUA), true);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		Tooltips.add(tooltip, Component.translatable(ModLang.TOOLTIP_JADE_LAKE_MIRROR, TURNS, RAISED_TICKS / 20, REST_TICKS / 20).withStyle(ChatFormatting.GRAY));
	}

	@Override
	public boolean isFoil(ItemStack stack) { return true; }

	/** A blow at one whose mirror is raised: turned back on its striker, whole, and refused the bearer (a turn of the mirror spent). */
	static boolean allowDamage(LivingEntity bearer, DamageSource source, float amount) {
		if (turning || !bearer.hasEffect(ModEffects.MIRROR_RAISED) || !(bearer.level() instanceof ServerLevel level)) return true;
		if (!(source.getEntity() instanceof LivingEntity striker) || striker == bearer || !striker.isAlive()) return true;
		MobEffectInstance raised = bearer.getEffect(ModEffects.MIRROR_RAISED);
		bearer.removeEffect(ModEffects.MIRROR_RAISED);
		if (raised != null && raised.getAmplifier() > 0) {
			bearer.addEffect(new MobEffectInstance(ModEffects.MIRROR_RAISED, raised.getDuration(), raised.getAmplifier() - 1, false, true, true));
		}
		turning = true;
		try {
			striker.hurt(level.damageSources().indirectMagic(bearer, bearer), amount);
		} finally {
			turning = false;
		}
		level.sendParticles(ParticleTypes.END_ROD, bearer.getX(), bearer.getY() + 1.2, bearer.getZ(), 12, 0.4, 0.6, 0.4, 0.06);
		level.sendParticles(ParticleTypes.END_ROD, striker.getX(), striker.getY() + 1.0, striker.getZ(), 8, 0.3, 0.5, 0.3, 0.04);
		level.playSound(null, bearer.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.4f, 1.4f);
		if (bearer instanceof ServerPlayer sp && !bearer.hasEffect(ModEffects.MIRROR_RAISED)) {
			sp.displayClientMessage(Component.translatable(ModLang.MSG_MIRROR_RESTS).withStyle(ChatFormatting.GRAY), true);
		}
		return false;
	}

	public static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(JadeLakeMirrorItem::allowDamage);
	}
}
