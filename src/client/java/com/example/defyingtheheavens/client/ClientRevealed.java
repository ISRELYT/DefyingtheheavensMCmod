package com.example.defyingtheheavens.client;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.Util;
import net.minecraft.world.entity.Entity;

/**
 * Who the local player's Revealing Talisman finds hidden near them (ModPackets#REVEALED, told each second while it burns: Talismans#tick): those
 * concealed by a Concealment Talisman, or in seclusion behind a barrier of their own, outlined to them alone in the talisman's fire-gold, through
 * walls (ConsciousnessRenderer#isHighlighted, which the outline hooks ask). Forgotten {@link #KEPT_MS} after it was last told, the talisman out.
 */
public final class ClientRevealed {
	/** The outline's colour: the talisman's fire-gold (ModEffects#REVEALING's own). */
	public static final int COLOR = 0xF0A030;
	/** Told once a second: kept a little longer than that, then forgotten. */
	private static final long KEPT_MS = 1500;
	private static final IntOpenHashSet IDS = new IntOpenHashSet();
	private static long until;

	/** Told who is revealed now (their entity ids). */
	public static void set(int[] ids) {
		IDS.clear();
		for (int id : ids) IDS.add(id);
		until = Util.getMillis() + KEPT_MS;
	}

	/** Whether {@code entity} is revealed to the local player now. */
	public static boolean isRevealed(Entity entity) {
		if (IDS.isEmpty()) return false;
		if (Util.getMillis() > until) {
			IDS.clear();
			return false;
		}
		return IDS.contains(entity.getId());
	}

	public static void clear() {
		IDS.clear();
		until = 0;
	}

	private ClientRevealed() {}
}
