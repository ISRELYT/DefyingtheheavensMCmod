package com.example.defyingtheheavens;

import net.minecraft.world.entity.player.Player;

/**
 * What a projected nascent soul (SoulProjection) and a ghost (GhostSystem) have in common: neither touches the living world.
 * They break and place nothing, use no item or block (but a ghost may use the dead's own blocks, tagged ghost_usable, and drink a
 * Marrow Cleansing Elixir: {@link #mayDrink}), drop and pick up nothing (but a ghost takes experience, and that elixir:
 * SoulPlayerMixin), set off no plate or tripwire, take no portal, push nothing
 * and are pushed by nothing (no current carries them), are passed through by arrows and blasts, and no ordinary mob takes them
 * for prey. Both sides.
 * <p>
 * A Ghost King (GhostSystem#isKing) has its hands back: it touches the world as the living do ({@link #is} leaves it out), but
 * nothing of the living world touches it any more than any of the dead ({@link #untouchable}), and it is still one of the dead
 * in what the dead can't do at all, break through, turn inward and the rest ({@link #isGhost}).
 * <p>
 * The rest stays each one's own: a soul passes through walls and is seen only by those who perceive it; a ghost walks with
 * gravity and walls and is seen by everyone in the Netherworld. A creative ghost keeps the look and none of these rules.
 */
public final class Incorporeal {
	/** Touches nothing of the living world: a soul, or a ghost the dead's rules hold (not a Ghost King). */
	public static boolean is(Player player) {
		return SoulProjection.isSoul(player) || isRuledGhost(player);
	}

	/** A ghost the dead's rules of touch hold: not a creative or spectating one, nor a Ghost King, whose hands are its own. */
	public static boolean isRuledGhost(Player player) {
		return isGhost(player) && !GhostSystem.isKing(player);
	}

	/** One of the dead in earnest, a Ghost King or not: not a creative or spectating ghost, who keeps the look and no rule. */
	public static boolean isGhost(Player player) {
		return GhostSystem.isGhost(player) && !player.isCreative() && !player.isSpectator();
	}

	/**
	 * Nothing of the living world touches it: arrows and blasts pass through, currents and shoves don't move it, no mob hunts it.
	 * A soul, and any of the dead, a Ghost King too.
	 */
	public static boolean untouchable(Player player) {
		return SoulProjection.isSoul(player) || isGhost(player);
	}

	/**
	 * The one thing of the living world a ghost (not a soul) may take up and drink: a Marrow Cleansing Elixir, which opens a
	 * mortal's meridians in death as in life. Handed to it (dropped where it is), or kept by it.
	 */
	public static boolean mayDrink(Player player, net.minecraft.world.item.ItemStack stack) {
		return GhostSystem.isGhost(player) && !SoulProjection.isSoul(player) && stack.is(ModItems.MARROW_CLEANSING_ELIXIR);
	}

	private Incorporeal() {}
}
