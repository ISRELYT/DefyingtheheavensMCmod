package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

/**
 * Nirvana Rebirth (CultivationMethod#NIRVANA_REBIRTH; the methods overhaul's phase 4): the heaven path's way at Entering Nirvana, its gain a
 * death on the Shore of Nirvana (the Great Void's pale coast, ModBiomes#SHORE_OF_NIRVANA). {@link #RISE} times in {@link #OF} it rises again
 * from its ashes, whole (its health full, its fire out, its ills gone), with {@link #SHARE} of the stage; else the death is real. Not a death
 * the void takes (fallen out of the world), a command's, old age's, a tribulation's, a spar's (no one dies in one) or a clone's
 * (CultivationEvents' ALLOW_DEATH asks after those).
 */
public final class NirvanaRebirth {
	public static final double SHARE = 0.5;
	public static final int RISE = 3, OF = 4;

	/** Whether {@code p} practises it: one of the living, its heaven path practising Nirvana Rebirth, holding its volume. */
	public static boolean practises(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		return !GhostSystem.isGhost(p) && CultivationMethods.practised(c, TwoPaths.Path.HEAVEN) == CultivationMethod.NIRVANA_REBIRTH
				&& CultivationMethods.holdsVolume(c, TwoPaths.Path.HEAVEN);
	}

	/** Whether {@code pos} in {@code level} is on the Shore of Nirvana. */
	public static boolean shore(ServerLevel level, BlockPos pos) {
		return level.getBiome(pos).is(ModBiomes.SHORE_OF_NIRVANA);
	}

	/**
	 * ALLOW_DEATH for {@code p}: on the shore, it rises again (false: no death) or dies truly (told); anywhere else, nothing of it. Never
	 * under a tribulation: the heavens' judgement is no death it rises from.
	 */
	static boolean allowDeath(ServerPlayer p, DamageSource source) {
		if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL) || source.is(ModDamageTypes.OLD_AGE)) return true;
		if (TribulationManager.isActive(p.getUUID())) return true;
		if (!practises(p) || !shore(p.serverLevel(), p.blockPosition())) return true;
		return !rises(p);
	}

	/** Struck down on the shore: {@code p} rises again from its ashes, whole, with its share of the stage (true), or doesn't (told). */
	static boolean rises(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		if (p.getRandom().nextInt(OF) >= RISE) {
			p.sendSystemMessage(Component.translatable(ModLang.MSG_NIRVANA_FAILED).withStyle(ChatFormatting.DARK_RED));
			level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
			return false;
		}
		p.setHealth(p.getMaxHealth());
		p.removeAllEffects();
		p.clearFire();
		p.invulnerableTime = 40;
		level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 80, 0.5, 1.0, 0.5, 0.08);
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 1.0, 0.5, 0.4);
		level.playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 0.8f);
		level.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 0.6f);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_NIRVANA_RISEN).withStyle(ChatFormatting.GOLD));
		CultivationMethods.grant(p, TwoPaths.Path.HEAVEN, CultivationMethod.NIRVANA_REBIRTH, SHARE);
		return true;
	}

	private NirvanaRebirth() {}
}
