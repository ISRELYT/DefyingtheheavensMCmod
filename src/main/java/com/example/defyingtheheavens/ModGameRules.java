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

	/**
	 * One ablaze in Meteor Flight (MeteorFlight) tears through what stands in their way, and one flying fast with the boost held
	 * bulldozes the way ahead (Bulldozer). Off, nothing is broken: they are stopped by it as by any wall.
	 */
	public static final GameRules.Key<GameRules.BooleanValue> METEOR_FLIGHT_SMASHES =
			GameRuleRegistry.register("meteorFlightSmashes", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));

	/** Wild demon beasts turn up round players (BeastSpawner). Off, none come of themselves (those already out stay). */
	public static final GameRules.Key<GameRules.BooleanValue> DEMON_BEASTS_SPAWN =
			GameRuleRegistry.register("demonBeastsSpawn", GameRules.Category.SPAWNING, GameRuleFactory.createBooleanRule(true));

	/**
	 * A player's spells wait out their cooldowns (Spells#cast). Off (the user's ask, 2026-10-24: "remove spell cooldowns for now
	 * completely"), a player casts again at once: the cooldown still runs on its record, unshown and unheeded. Cultivators keep theirs.
	 */
	public static final GameRules.Key<GameRules.BooleanValue> SPELL_COOLDOWNS =
			GameRuleRegistry.register("spellCooldowns", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(false));

	/** Touching this class registers the rules. */
	public static void register() {}

	private ModGameRules() {}
}
