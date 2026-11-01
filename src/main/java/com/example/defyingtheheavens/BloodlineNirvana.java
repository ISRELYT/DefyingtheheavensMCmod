package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.joml.Vector3f;

/**
 * Bloodline Nirvana (CultivationMethod#BLOODLINE_NIRVANA; the methods overhaul's phase 4): the Demon Path's way at Entering Nirvana. Its own
 * race's blood of Entering Nirvana or above, drunk (DemonBlood#drink: even blood it holds already), burns its blood down to be made anew:
 * {@link #RISE} times in {@link #OF} it is reborn, foreign cores' taint burnt out of it and {@link #SHARE} of the stage won; else it burns and
 * doesn't rise, the stage's cultivation lost and {@link #HARM} of its health with it.
 */
public final class BloodlineNirvana {
	public static final double SHARE = 0.5, HARM = 0.4;
	public static final int RISE = 7, OF = 10;

	/** Whether {@code c}'s Demon Path remakes its blood: practising Bloodline Nirvana, holding its volume (the client's copy too). */
	public static boolean remakes(PlayerCultivation c) {
		return CultivationMethods.practised(c, TwoPaths.Path.DEMON) == CultivationMethod.BLOODLINE_NIRVANA && CultivationMethods.holdsVolume(c, TwoPaths.Path.DEMON);
	}

	/** Whether its own race's blood of {@code realm} is what Bloodline Nirvana takes in, for {@code c}: of Entering Nirvana or above. */
	public static boolean takes(PlayerCultivation c, Realm realm) {
		return realm != null && realm.ordinal() >= Realm.ENTERING_NIRVANA.ordinal() && remakes(c);
	}

	/** {@code p} drank such blood: reborn, or burnt; told. */
	static void drunk(ServerPlayer p, PlayerCultivation c) {
		ServerLevel level = p.serverLevel();
		if (p.getRandom().nextInt(OF) < RISE) {
			c.demon().setTaint(0);
			level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 1.0, 0.5, 0.06);
			level.sendParticles(new DustParticleOptions(new Vector3f(0.9f, 0.1f, 0.1f), 1.6f), p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 1.0, 0.5, 0.02);
			level.playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 0.6f);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_BLOOD_NIRVANA_RISEN).withStyle(ChatFormatting.GOLD));
			CultivationMethods.grant(p, TwoPaths.Path.DEMON, CultivationMethod.BLOODLINE_NIRVANA, SHARE);
			return;
		}
		CultivationMethods.lose(p, TwoPaths.Path.DEMON, 1.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.02);
		level.playSound(null, p.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.6f);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_BLOOD_NIRVANA_BURNT).withStyle(ChatFormatting.DARK_RED));
		p.invulnerableTime = 0;
		p.hurt(p.damageSources().magic(), p.getMaxHealth() * (float) HARM);
	}

	private BloodlineNirvana() {}
}
