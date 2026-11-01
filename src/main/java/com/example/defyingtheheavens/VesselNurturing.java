package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Vessel Nurturing (CultivationMethod#VESSEL_NURTURING; the methods overhaul's phase 4): the heaven path's way at Sacred Vessel. A Sacred
 * Vessel used sneaking (SacredVesselItem#use) takes {@link #POUR} of one's qi pool and gives it back refined: {@link #SHARE} of the stage. The
 * vessel rests {@link #REST} ticks after; and one time in {@link #CRACK_ODDS} the relic cracks under it and is gone.
 */
public final class VesselNurturing {
	public static final double POUR = 0.5, SHARE = 0.1;
	public static final int REST = 600, CRACK_ODDS = 20;

	/** Whether {@code p} nurtures its qi in a vessel: one of the living, its heaven path practising Vessel Nurturing. */
	public static boolean nurtures(ServerPlayer p) {
		return !GhostSystem.isGhost(p) && CultivationMethods.practised(CultivationManager.get(p), TwoPaths.Path.HEAVEN) == CultivationMethod.VESSEL_NURTURING;
	}

	/**
	 * {@code p} pours its qi into {@code vessel}: refused (told) with too little qi; else poured, drawn back refined, the vessel resting, and
	 * perhaps cracked. @return whether it was Vessel Nurturing's use at all (else the vessel is only held)
	 */
	static boolean pour(ServerPlayer p, ItemStack vessel) {
		if (!nurtures(p) || p.getCooldowns().isOnCooldown(vessel.getItem())) return false;
		PlayerCultivation c = CultivationManager.get(p);
		double want = TwoPaths.poolMax(c, TwoPaths.Path.HEAVEN) * POUR;
		if (want <= 0 || TwoPaths.pool(c, TwoPaths.Path.HEAVEN) < want) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_VESSEL_LOW_QI, Math.round(POUR * 100)).withStyle(ChatFormatting.GRAY), true);
			return true;
		}
		if (CultivationMethods.grant(p, TwoPaths.Path.HEAVEN, CultivationMethod.VESSEL_NURTURING, SHARE) <= 0) return true; // told why
		TwoPaths.spend(c, TwoPaths.Path.HEAVEN, want);
		p.getCooldowns().addCooldown(vessel.getItem(), REST);
		ServerLevel level = p.serverLevel();
		Vec3 at = p.getEyePosition().add(p.getLookAngle().scale(0.6));
		level.sendParticles(ParticleTypes.END_ROD, at.x, at.y - 0.3, at.z, 24, 0.25, 0.25, 0.25, 0.04);
		level.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.1, p.getZ(), 30, 0.4, 0.6, 0.4, 0.6);
		level.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 1.4f);
		if (p.getRandom().nextInt(CRACK_ODDS) == 0 && !p.getAbilities().instabuild) {
			Component name = vessel.getHoverName();
			vessel.shrink(1);
			level.playSound(null, p.blockPosition(), SoundEvents.ANVIL_BREAK, SoundSource.PLAYERS, 1.0f, 0.7f);
			level.sendParticles(ParticleTypes.CRIT, at.x, at.y - 0.3, at.z, 30, 0.3, 0.3, 0.3, 0.2);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_VESSEL_CRACKED, name).withStyle(ChatFormatting.RED));
		}
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
		return true;
	}

	private VesselNurturing() {}
}
