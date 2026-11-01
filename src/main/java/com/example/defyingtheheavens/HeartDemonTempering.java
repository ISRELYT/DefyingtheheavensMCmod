package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Heart Demon Tempering (CultivationMethod#HEART_DEMON_TEMPERING; the methods overhaul's phase 4): the heaven path's way at Nascent Soul, its
 * gain the heart demon's defeat. Its meditation gathers nothing; turned inward (InnerRealm), the heart demon comes for it again and again,
 * {@link #FIRST} ticks into the meditation and every {@link #INTERVAL_MIN} to {@link #INTERVAL_MAX} after (QiSurges: no other surge there, and
 * no cooldown), each one slain {@link #WIN} of the stage; a defeat costs {@link #LOSS} of it on top of the usual (thrown back to the body,
 * shaken, the qi drained: QiSurges#allowInnerDamage).
 */
public final class HeartDemonTempering {
	public static final int FIRST = 400, INTERVAL_MIN = 400, INTERVAL_MAX = 600;
	public static final double WIN = 0.1, LOSS = 0.1;

	/** Whether {@code p}'s meditation tempers its heart now: the heaven path's drawn in, practising it, holding its volume. */
	public static boolean tempers(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		return !GhostSystem.isGhost(p) && !TwoPaths.cultivatesGhost(c) && !TwoPaths.cultivatesDemon(c)
				&& CultivationMethods.practised(c, TwoPaths.Path.HEAVEN) == CultivationMethod.HEART_DEMON_TEMPERING
				&& CultivationMethods.holdsVolume(c, TwoPaths.Path.HEAVEN);
	}

	/** {@code p} slew its heart demon: a gain of its stage. */
	static void won(ServerPlayer p) {
		CultivationMethods.grant(p, TwoPaths.Path.HEAVEN, CultivationMethod.HEART_DEMON_TEMPERING, WIN);
	}

	/** {@code p}'s heart demon won: a loss of its stage besides (never the stage itself). Told. */
	static void lost(ServerPlayer p) {
		if (CultivationMethods.lose(p, TwoPaths.Path.HEAVEN, LOSS) > 0) {
			p.sendSystemMessage(Component.translatable(ModLang.MSG_TEMPERING_LOST).withStyle(ChatFormatting.RED));
		}
	}

	private HeartDemonTempering() {}
}
