package com.example.defyingtheheavens;

import net.minecraft.world.entity.player.Player;

/**
 * What a projected nascent soul (SoulProjection) and a ghost (GhostSystem) have in common: neither touches the living world.
 * They break and place nothing, use no item or block (but a ghost may use the dead's own blocks, tagged ghost_usable), drop and
 * pick up nothing (but a ghost takes experience: SoulPlayerMixin), set off no plate or tripwire, take no portal, push nothing
 * and are pushed by nothing (no current carries them), are passed through by arrows and blasts, and no ordinary mob takes them
 * for prey. Both sides.
 * <p>
 * The rest stays each one's own: a soul passes through walls and is seen only by those who perceive it; a ghost walks with
 * gravity and walls and is seen by everyone in the Netherworld. A creative ghost keeps the look and none of these rules.
 */
public final class Incorporeal {
	public static boolean is(Player player) {
		return SoulProjection.isSoul(player) || isRuledGhost(player);
	}

	/** A ghost the dead's rules hold (not a creative or spectating one). */
	public static boolean isRuledGhost(Player player) {
		return GhostSystem.isGhost(player) && !player.isCreative() && !player.isSpectator();
	}

	private Incorporeal() {}
}
