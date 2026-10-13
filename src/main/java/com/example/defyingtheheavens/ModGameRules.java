package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.level.GameRules;

/** The mod's own gamerules. */
public final class ModGameRules {
	/**
	 * The dead go down to the Netherworld (see GhostSystem): a player who dies wakes at the Ghost Gate as a ghost instead of at
	 * their bed. Off, death is vanilla's and there are no ghosts.
	 */
	public static final GameRules.Key<GameRules.BooleanValue> NETHERWORLD_AFTER_DEATH =
			GameRuleRegistry.register("netherworldAfterDeath", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));

	/** Touching this class registers the rules. */
	public static void register() {}

	private ModGameRules() {}
}
