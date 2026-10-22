package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Name Your Obsession (the user's ask, 2026-10-19). Before the tribulation out of Nascent Soul Grand Perfection into Heavenly Being
 * Early, on either path (the heavens' or, on the ghost path, the Netherworld's), the cultivator names the one thing its heart won't
 * let go of: one of the four axes' (FourAxis.Axis). The Breakthrough button brings up the screen first ({@link #ask}); a choice
 * named ({@link #name}), the tribulation begins at once. Its road bends that way for good:
 * <ul>
 *   <li><b>Longevity</b>: lifespan x{@link #LIFESPAN} (Lifespan#span).</li>
 *   <li><b>Fortune</b>: what one pays in trade x{@link #PRICE} (CultivatorTrades#offersFor).</li>
 *   <li><b>Merit</b>: {@link #REGARD} more regard with everyone (CultivatorNpc#regard).</li>
 *   <li><b>Enlightenment</b>: cultivation x{@link #CULTIVATION}, every path's (StatPoints#cultivationFactor).</li>
 * </ul>
 * Whichever it is, its axis at Four Axis fills {@link #AXIS_FACTOR} times as fast: it needs that much less ({@link #need}). Named
 * once, never again (saved on the record, PlayerCultivation#obsession); a spent life loses it with everything else (Lifespan#wipe).
 * One already past it when obsessions came in (an older save) is asked at its next tribulation.
 */
public final class Obsession {
	public static final double LIFESPAN = 1.2, PRICE = 0.8, CULTIVATION = 1.2;
	public static final int REGARD = 10, AXIS_FACTOR = 2;
	/** The rank whose breakthrough asks it (into Heavenly Being Early); any after it ask one who has none yet. */
	public static final int ASKED_AT = PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.GRAND_PERFECTION);

	/** Whether a breakthrough from {@code fromRank} (either path's) waits on an obsession named first. */
	public static boolean asks(PlayerCultivation c, int fromRank) {
		return c.obsession() == null && fromRank >= ASKED_AT;
	}

	/** Puts "Name Your Obsession" before {@code player} (for the heaven path's tribulation, or the ghost path's: {@code ghost}). */
	public static void ask(ServerPlayer player, boolean ghost) {
		ModPackets.sendObsessionAsk(player, ghost);
	}

	/**
	 * {@code player} names {@code obsession} on the screen: kept for good, and the tribulation it stood before begins. Only while
	 * they still stand at that breakthrough with none named (a stale or forged answer names nothing); one named already goes
	 * straight to the tribulation.
	 */
	public static void name(ServerPlayer player, FourAxis.Axis obsession, boolean ghost) {
		if (obsession == null || !player.isAlive()) return;
		PlayerCultivation c = CultivationManager.get(player);
		if (c.obsession() == null) {
			int from = ghost ? GhostMethods.ghostRank(c.netherworld()) : PlayerCultivation.rank(c.getRealm(), c.getStage());
			boolean ready = ghost ? GhostMethods.canBreakthrough(c) : !c.isMortal() && c.canBreakthrough();
			if (!asks(c, from) || !ready) return;
			c.setObsession(obsession);
			CultivationManager.markDirty(player.server);
			CultivationManager.sync(player);
			player.sendSystemMessage(Component.translatable(ModLang.MSG_OBSESSION_NAMED, displayName(obsession), boon(obsession))
					.withStyle(ChatFormatting.GOLD));
			player.playNotifySound(SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0f, 0.8f);
		}
		if (ghost) TribulationManager.startGhost(player);
		else TribulationManager.start(player);
	}

	// --- Its boons ---

	/** Lifespan multiplier: Longevity's. */
	public static double lifespanFactor(PlayerCultivation c) {
		return c.obsession() == FourAxis.Axis.LONGEVITY ? LIFESPAN : 1;
	}

	/** What one pays in trade, multiplied: Fortune's. */
	public static double priceFactor(PlayerCultivation c) {
		return c.obsession() == FourAxis.Axis.FORTUNE ? PRICE : 1;
	}

	/** Regard with everyone, added: Merit's. */
	public static int regardBonus(PlayerCultivation c) {
		return c.obsession() == FourAxis.Axis.MERIT ? REGARD : 0;
	}

	/** Cultivation multiplier, every path's: Enlightenment's. */
	public static double cultivationFactor(PlayerCultivation c) {
		return c.obsession() == FourAxis.Axis.ENLIGHTENMENT ? CULTIVATION : 1;
	}

	/** What completes {@code axis} ({@code need} in its measure), halved (rounded up) if it is the axis of {@code c}'s obsession. */
	public static int need(PlayerCultivation c, FourAxis.Axis axis, int need) {
		return c.obsession() == axis ? (need + AXIS_FACTOR - 1) / AXIS_FACTOR : need;
	}

	/** The obsession's name: "Longevity". */
	public static Component displayName(FourAxis.Axis obsession) {
		return Component.translatable(ModLang.OBSESSION_PREFIX + obsession.getId());
	}

	/** What it gives, said short: "Lifespan x1.2". */
	public static Component boon(FourAxis.Axis obsession) {
		return Component.translatable(ModLang.OBSESSION_BOON_PREFIX + obsession.getId());
	}

	private Obsession() {}
}
