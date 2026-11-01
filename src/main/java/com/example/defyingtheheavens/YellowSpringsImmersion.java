package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Yellow Springs Immersion (CultivationMethod#YELLOW_SPRINGS_IMMERSION; the methods overhaul's phase 4): the ghost path's way from Nascent Soul to
 * Heavenly Being, soaked in the Yellow Springs' waters (the Netherworld's golden riverlands, ModBiomes#YELLOW_SPRINGS). Each second in them gives
 * {@link #SHARE} of the path's stage at Nascent Soul, a realm's time step less for each realm past it ({@link #share}); and at each minute
 * soaked, one time in {@link #FORGET_ODDS}, Meng Po's forgetting takes a memory (a foundation's top layer, to settle in again:
 * SoulRiverFeast#forgetMemory).
 */
public final class YellowSpringsImmersion {
	public static final double SHARE = 0.0025;
	public static final int FORGET_ODDS = 10;

	/** How long each has been soaking, seconds (not saved: out of the water, it starts again). */
	private static final Map<UUID, Integer> SOAKED = new HashMap<>();

	/** Whether {@code p} soaks in the springs to cultivate: its ghost path practising Yellow Springs Immersion, holding its volume. */
	public static boolean soaks(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		return GhostMethods.ghostRank(c.netherworld()) >= 0 && CultivationMethods.practised(c, TwoPaths.Path.GHOST) == CultivationMethod.YELLOW_SPRINGS_IMMERSION
				&& CultivationMethods.holdsVolume(c, TwoPaths.Path.GHOST);
	}

	/** Whether {@code p} is in the Yellow Springs' water. */
	public static boolean inTheSprings(ServerPlayer p) {
		return p.isInWater() && p.serverLevel().getBiome(p.blockPosition()).is(ModBiomes.YELLOW_SPRINGS);
	}

	/** The share of its path's stage a second in the springs gives one whose ghost path stands in {@code realm}. */
	public static double share(Realm realm) {
		return SHARE / Math.pow(GhostMethods.REALM_TIME_STEP, Math.max(0, realm.ordinal() - Realm.NASCENT_SOUL.ordinal()));
	}

	/** A second of {@code p}'s (NetherworldManager#second): soaking in the springs, the gain, the soak told now and then, a memory perhaps lost. */
	static void second(ServerPlayer p) {
		if (!soaks(p) || !inTheSprings(p)) {
			SOAKED.remove(p.getUUID());
			return;
		}
		int seconds = SOAKED.merge(p.getUUID(), 1, Integer::sum);
		Realm realm = GhostMethods.ghostRealm(CultivationManager.get(p).netherworld());
		if (realm == null) return;
		CultivationMethods.grant(p, TwoPaths.Path.GHOST, CultivationMethod.YELLOW_SPRINGS_IMMERSION, share(realm), false);
		// Souls rising from the waters round the soaker (round it, not in it, so it sees them too); the soak told while it lasts.
		for (int i = 0; i < 3; i++) {
			double a = p.getRandom().nextDouble() * Math.PI * 2, r = 0.9 + p.getRandom().nextDouble() * 0.5;
			p.serverLevel().sendParticles(ParticleTypes.SOUL, p.getX() + Math.cos(a) * r, p.getY() + 0.4, p.getZ() + Math.sin(a) * r, 1, 0, 0.1, 0, 0.01);
		}
		if (seconds % 2 == 0) p.displayClientMessage(Component.translatable(ModLang.MSG_SPRINGS_SOAKING, seconds).withStyle(ChatFormatting.GOLD), true);
		if (seconds % 60 == 0 && p.getRandom().nextInt(FORGET_ODDS) == 0) SoulRiverFeast.forgetMemory(p, ModLang.MSG_SPRINGS_MEMORY);
	}

	/** A player left. */
	public static void forget(UUID id) {
		SOAKED.remove(id);
	}

	private YellowSpringsImmersion() {}
}
