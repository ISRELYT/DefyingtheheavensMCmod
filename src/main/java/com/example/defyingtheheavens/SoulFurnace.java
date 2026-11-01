package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Soul Furnace Refining (CultivationMethod#SOUL_FURNACE; the methods overhaul's phase 4): the ghost path's way at Entering Nirvana, meditated in
 * one's own ghost fire in place of the yin drawn in (MeditationManager#ghostMethods). Each second of a meditation burns hotter: its second n
 * gives {@link #GAIN} x (1 + n / {@link #RAMP}) of the path's stage, and costs {@link #HARM} + {@link #HARM_STEP} x n of the soul's health, at
 * most {@link #MOST_HARM} a second (the gain grows no more once the harm is at its most, {@link #HOTTEST} seconds in, so no mending outlasts
 * it into an endless climb). The burning never breaks the meditation (one stands up to end it, and sits down to a cold furnace); out
 * of health, the furnace breaks ({@link #breaks}): the stage's cultivation burns away, the meditation ends, and one of the dead scatters
 * (GhostSystem#disperse: it reforms at the Gate), one of the living dies.
 */
public final class SoulFurnace {
	public static final double GAIN = 0.002, RAMP = 30, HARM = 0.01, HARM_STEP = 0.002, MOST_HARM = 0.2;
	/** The second the furnace is at its hottest: its harm at the most, its gain grown no further. */
	public static final double HOTTEST = (MOST_HARM - HARM) / HARM_STEP;

	/** Whether {@code c}'s ghost path burns in the furnace as it meditates: practising Soul Furnace Refining, holding its volume. */
	public static boolean burns(PlayerCultivation c) {
		return CultivationMethods.practised(c, TwoPaths.Path.GHOST) == CultivationMethod.SOUL_FURNACE && CultivationMethods.holdsVolume(c, TwoPaths.Path.GHOST);
	}

	/** The share of its health a second of the furnace costs at its second {@code n}. */
	public static double harm(double n) {
		return Math.min(MOST_HARM, HARM + HARM_STEP * n);
	}

	/** The share of its path's stage a second of the furnace gives at its second {@code n} (no more past {@link #HOTTEST}). */
	public static double gain(double n) {
		return GAIN * (1 + Math.min(n, HOTTEST) / RAMP);
	}

	/**
	 * Half a second of the furnace, {@code ticks} into the meditation: {@code p} burnt (told how long, and at what cost, on the {@code second}),
	 * and the cultivation it gives returned; nothing once it breaks.
	 */
	static double burn(ServerPlayer p, PlayerCultivation c, int ticks, boolean second) {
		int rank = GhostMethods.ghostRank(c.netherworld());
		if (rank < 0) return 0;
		double n = ticks / 20.0;
		ServerLevel level = p.serverLevel();
		// The ghost fire in a ring about the soul, hotter the longer it burns: round the body, not in it, so the burning see it too.
		int flames = 4 + (int) Math.min(16, n / 4);
		for (int i = 0; i < flames; i++) {
			double a = p.getRandom().nextDouble() * Math.PI * 2, r = 0.8 + p.getRandom().nextDouble() * 0.4;
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX() + Math.cos(a) * r, p.getY() + 0.1 + p.getRandom().nextDouble() * 1.6,
					p.getZ() + Math.sin(a) * r, 1, 0, 0.05, 0, 0.02);
		}
		if (!p.isCreative()) {
			float harm = (float) (0.5 * harm(n) * p.getMaxHealth());
			if (p.getHealth() - harm <= 0) {
				breaks(p);
				return 0;
			}
			p.setHealth(p.getHealth() - harm);
		}
		if (second) {
			level.playSound(null, p.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.5f, 0.5f + (float) Math.min(0.8, n / 100));
			p.displayClientMessage(Component.translatable(ModLang.MSG_FURNACE_BURNING, (int) n, Math.round(harm(n) * 1000) / 10.0)
					.withStyle(ChatFormatting.DARK_AQUA), true);
		}
		return 0.5 * gain(n) * GhostMethods.required(rank);
	}

	/** The furnace breaks: the stage's cultivation burns away, the meditation ends; the dead scatter, the living die. Told. */
	static void breaks(ServerPlayer p) {
		MeditationManager.stop(p, true);
		CultivationMethods.lose(p, TwoPaths.Path.GHOST, 1.0);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_FURNACE_BREAKS).withStyle(ChatFormatting.DARK_RED));
		ServerLevel level = p.serverLevel();
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 0.9, 0.6, 0.08);
		if (GhostSystem.isGhost(p)) {
			GhostSystem.disperse(p);
			return;
		}
		p.invulnerableTime = 0;
		p.hurt(ModDamageTypes.ghostly(level, null), Float.MAX_VALUE);
	}

	private SoulFurnace() {}
}
