package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Hundred Battles (CultivationMethod#HUNDRED_BATTLES, the user's "Go, all four", 2026-11-01): the body tempered in the trading of blows. One
 * drawn in by this way, sparring with a cultivator (SparSystem), tempers by every blow traded with it that lands, given or taken
 * ({@link #onHurt}): {@link #BLOW_SECONDS} seconds of the Body Path's own pace, twice for each realm the partner fights at above the body (at
 * most {@link #MOST_ABOVE}; held back to one's own realm, it is an equal), half for each below (at least an eighth); a spar won ({@link #won})
 * {@link #WIN_SECONDS} more the same way. Meditation tempers nothing on it.
 */
public final class HundredBattles {
	public static final double BLOW_SECONDS = 4, WIN_SECONDS = 60;
	public static final int MOST_ABOVE = 2;
	/** A partner below the body is worth half for each realm, an eighth at the least. */
	static final int MOST_BELOW = 3;

	/** A blow from {@code source} took {@code lost} of {@code victim}'s health (Physiques#onHurt): between partners in a spar, both temper by it. */
	public static void onHurt(LivingEntity victim, DamageSource source, float lost) {
		if (lost <= 0 || !(source.getEntity() instanceof LivingEntity dealer) || dealer == victim || !SparSystem.areSparring(victim, dealer)) return;
		traded(victim, dealer);
		traded(dealer, victim);
	}

	private static void traded(LivingEntity one, LivingEntity partner) {
		if (!(one instanceof ServerPlayer p)) return;
		double fraction = BodyPath.temperBy(p, CultivationMethod.HUNDRED_BATTLES, BLOW_SECONDS * worth(p, partner));
		if (fraction > 0) p.displayClientMessage(Component.translatable(ModLang.MSG_HUNDRED_BATTLES, BodyPath.percent(fraction)).withStyle(ChatFormatting.GRAY), true);
	}

	/** {@code p} has won a spar against a cultivator who fought at {@code fought} (SparSystem#end: held back or not, before it let go). */
	public static void won(ServerPlayer p, int fought) {
		double fraction = BodyPath.temperBy(p, CultivationMethod.HUNDRED_BATTLES, WIN_SECONDS * worth(p, fought));
		if (fraction > 0) p.sendSystemMessage(Component.translatable(ModLang.MSG_HUNDRED_BATTLES_WON, BodyPath.percent(fraction)).withStyle(ChatFormatting.GOLD));
	}

	/**
	 * What a blow traded with {@code partner} is worth to {@code p}'s body: twice for each realm it fights at above it, half for each below (a
	 * cultivator held back in the spar at the realm it holds back to: CultivatorNpc#fightingRank; it was weighed at its own before 2026-11-01,
	 * so a strong one holding back to a Qi Refining body was worth four times what it fought as).
	 */
	static double worth(ServerPlayer p, LivingEntity partner) {
		int theirs = partner instanceof CultivatorNpc npc ? (npc.isMortal() ? -1 : npc.fightingRank())
				: partner instanceof ServerPlayer other ? TwoPaths.weighedRank(CultivationManager.get(other)) : -1;
		return worth(p, theirs);
	}

	/** What a blow traded with a partner fighting at {@code theirs} (-1: a mortal) is worth to {@code p}'s body. */
	static double worth(ServerPlayer p, int theirs) {
		int stages = Stage.values().length;
		int body = Math.max(0, BodyPath.rank(CultivationManager.get(p)));
		int realms = (theirs < 0 ? -1 : theirs / stages) - body / stages;
		return Math.pow(2, Math.max(-MOST_BELOW, Math.min(MOST_ABOVE, realms)));
	}

	private HundredBattles() {}
}
