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
	 * A player's spells wait out their cooldowns (Spells#cast), the shorter the higher they stand above a spell (Laws#cooldown). Off, a player
	 * casts again at once: the cooldown still runs on its record, unshown and unheeded. Cultivators keep theirs. Off by default from the user's
	 * ask of 2026-10-24 ("remove spell cooldowns for now completely"), on again from 2026-11-10 ("add spell cooldowns back"), a world saved
	 * meanwhile turned on once ({@link #cooldownsBack}).
	 */
	public static final GameRules.Key<GameRules.BooleanValue> SPELL_COOLDOWNS =
			GameRuleRegistry.register("spellCooldowns", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));

	/**
	 * A player's spells change the land for good (the user's ask, 2026-10-25: "a grief toggle"): their craters, cuts, fissures and bores,
	 * the lakes and floods of the water spells, the ice and the flowers they leave, and fire that catches and spreads. Off, they leave the
	 * land as it was (HeavenSpells#carvesLand): nothing carved or filled for good, and their fire only a moment's, catching nothing and
	 * putting itself out (Devastation#ignite, FireBlockMixin); the water they pour drains away again. What a spell sets down for a while
	 * (walls, ice, thorns, cages) comes and goes either way. Cultivators' spells never change the land, whatever this says.
	 * <p>
	 * Turned from the cultivation menu's Spells tab too, by an operator or the world's own host ({@link #setSpellGriefing}: the user's
	 * word, 2026-10-25, "I need that grief toggle idk where it is I couldn't find it"); every player is told how it stands as it changes.
	 */
	public static final GameRules.Key<GameRules.BooleanValue> SPELL_GRIEFING =
			GameRuleRegistry.register("spellGriefing", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true, (server, rule) -> {
				for (net.minecraft.server.level.ServerPlayer p : server.getPlayerList().getPlayers()) Spells.sync(p);
			}));

	/**
	 * The dead leave their bodies (Corpses): a player, or a cultivator, lies where they fell with what they carried, rotting to bones. Off,
	 * no body is left and what they carried drops as vanilla's does.
	 */
	public static final GameRules.Key<GameRules.BooleanValue> LEAVE_CORPSES =
			GameRuleRegistry.register("leaveCorpses", GameRules.Category.DROPS, GameRuleFactory.createBooleanRule(true));

	/**
	 * The secret realms stir and open (SecretRealms): each in its turn, by the world's clock. Off, none stirs (one open now closes, those in it
	 * cast out beside its Seal) and its Seal stays shut.
	 */
	public static final GameRules.Key<GameRules.BooleanValue> SECRET_REALMS =
			GameRuleRegistry.register("secretRealms", GameRules.Category.UPDATES, GameRuleFactory.createBooleanRule(true));

	/** Whether {@code p} may turn the mod's gamerules from its menus: an operator, or the host of the world they play in. */
	public static boolean mayChange(net.minecraft.server.level.ServerPlayer p) {
		return p.hasPermissions(2) || p.server.isSingleplayerOwner(p.getGameProfile());
	}

	/** The Spells tab's switch (C2S spell_griefing): {@link #SPELL_GRIEFING} set {@code on} by {@code p}, if they may, and all told. */
	public static void setSpellGriefing(net.minecraft.server.level.ServerPlayer p, boolean on) {
		if (!mayChange(p)) {
			Spells.sync(p); // its switch put back as it stands
			return;
		}
		GameRules.BooleanValue rule = p.server.getGameRules().getRule(SPELL_GRIEFING);
		if (rule.get() == on) return;
		rule.set(on, p.server); // its callback tells every player
		net.minecraft.network.chat.Component said = net.minecraft.network.chat.Component.translatable(on ? ModLang.MSG_GRIEFING_ON : ModLang.MSG_GRIEFING_OFF, p.getDisplayName());
		p.server.getPlayerList().broadcastSystemMessage(said.copy().withStyle(net.minecraft.ChatFormatting.GOLD), false);
	}

	/**
	 * A world started: if it has never had {@link #SPELL_COOLDOWNS} turned back on, it is now, once (a world saves its rules, so one saved
	 * while the rule was off by default kept it off). Turned off again after, it stays off.
	 */
	static void cooldownsBack(net.minecraft.server.MinecraftServer server) {
		CultivationSavedData data = CultivationSavedData.forServer(server);
		if (data.cooldownsBack()) return;
		server.getGameRules().getRule(SPELL_COOLDOWNS).set(true, server);
		data.setCooldownsBack();
	}

	/** Touching this class registers the rules (and has each world's arts' cooldowns turned back on once: {@link #cooldownsBack}). */
	public static void register() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(ModGameRules::cooldownsBack);
	}

	private ModGameRules() {}
}
