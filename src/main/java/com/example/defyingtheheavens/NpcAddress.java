package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * How a cultivator calls a player (the user's ask, 2026-10-27: "NPCs should henceforth refer to them by their title rather than just
 * senior ... Calling someone just a senior when they are qi refining and you are a 4 axis or sacred vessel doesn't make sense they should
 * revere you more"; and "make it so NPCs don't call you venerable (title is only reserved for actual venerables)"). By how many realms
 * the player stands above it or below it, as each shows itself ({@link #gap}; a mortal one realm below Qi Refining), a rung of address
 * ({@link Rung}): a peer is a Fellow Daoist; one realm above, Senior; two or three, Esteemed Senior; four or five, Revered Senior; six or
 * more, Ancestor (Lord Immortal to one who is a True Immortal or past it). Never a rank one doesn't hold (no Venerable, no Patriarch).
 * Above it, the player is a junior to it, in its path's voice: a young friend, a brat, an ant. A mortal calls any cultivator Immortal.
 * From Void Shattering on, one who has taken a title (PlayerTitles) is called by it in place of all of those; else its own sect's
 * Elder, Grand Elder or Sect Master over it is called by that title, one they truly hold.
 */
public final class NpcAddress {
	/** From this many realms above it, a cultivator kneels before one (TalkSystem#kneelsTo: the user's pick, 2026-10-27). */
	public static final int KNEEL_GAP = 4;

	/** Where the player stands against it, by how many realms. */
	public enum Rung {
		/** It is two realms or more above the player. */
		FAR_ABOVE("far_above"),
		/** It is one realm above the player. */
		ABOVE("above"),
		PEER("peer"),
		/** The player is one realm above it. */
		SENIOR("senior"),
		/** Two or three above it. */
		ESTEEMED("esteemed"),
		/** Four or five above it: it kneels. */
		REVERED("revered"),
		/** Six or more above it: it kneels, and can't so much as sense how far. */
		ANCESTOR("ancestor"),
		/** The player is a mortal (to a cultivator). */
		MORTAL("mortal");

		private final String id;

		Rung(String id) {
			this.id = id;
		}

		public String id() { return id; }

		/** The player stands above it. */
		public boolean above() { return ordinal() >= SENIOR.ordinal() && this != MORTAL; }
	}

	/** How many realms {@code player} stands above {@code npc} (below it, less than 0), as each shows itself; a mortal at one below Qi Refining. */
	public static int gap(ServerPlayer player, CultivatorNpc npc) {
		PlayerCultivation c = CultivationManager.get(player);
		int theirs = npc.isMortal() ? -1 : npc.getDisplayedRealm().ordinal();
		int yours = c.isMortal() ? -1 : TwoPaths.wieldedRealm(c).ordinal();
		return yours - theirs;
	}

	/** The rung {@code player} stands on before {@code npc}. */
	public static Rung rung(ServerPlayer player, CultivatorNpc npc) {
		PlayerCultivation c = CultivationManager.get(player);
		if (c.isMortal() && !npc.isMortal()) return Rung.MORTAL;
		int gap = gap(player, npc);
		if (gap >= 6) return Rung.ANCESTOR;
		if (gap >= KNEEL_GAP) return Rung.REVERED;
		if (gap >= 2) return Rung.ESTEEMED;
		if (gap == 1) return Rung.SENIOR;
		if (gap == 0) return Rung.PEER;
		return gap == -1 ? Rung.ABOVE : Rung.FAR_ABOVE;
	}

	/** The player's name as {@code npc} would say it: the title they have taken, from Void Shattering on, else their own name. */
	public static String name(ServerPlayer player) {
		String title = PlayerTitles.shown(player);
		return title.isEmpty() ? player.getGameProfile().getName() : title;
	}

	/** What {@code npc} calls {@code player}: by their title if they have taken one, else by its rung of address, in its voice. */
	public static Component address(ServerPlayer player, CultivatorNpc npc) {
		Rung rung = rung(player, npc);
		String voice = NpcLines.voice(npc);
		String title = PlayerTitles.shown(player);
		boolean immortal = TwoPaths.wieldedRealm(CultivationManager.get(player)).ordinal() >= Realm.TRUE_IMMORTAL.ordinal()
				&& !CultivationManager.get(player).isMortal();
		// A mortal calls any cultivator Immortal (by their title, or their name), and Lord Immortal one who truly is; another mortal, friend.
		if (npc.isMortal()) {
			if (rung == Rung.PEER) return Component.translatable(ModLang.address("friend"));
			if (immortal) return Component.translatable(ModLang.address("lord_immortal"));
			return Component.translatable(ModLang.address("immortal"), name(player));
		}
		if (!title.isEmpty()) {
			if (!rung.above() && rung != Rung.PEER) return Component.translatable(ModLang.address("titled_junior." + junior(voice)), title);
			return Component.literal(title);
		}
		// Its own sect's Elder (or Grand Elder, or Sect Master) over it is called by the title they hold in it.
		Component held = sectTitle(player, npc);
		if (held != null) return held;
		return switch (rung) {
			case ANCESTOR -> Component.translatable(ModLang.address(immortal ? "lord_immortal" : "ancestor"));
			case REVERED -> Component.translatable(ModLang.address("revered"));
			case ESTEEMED -> Component.translatable(ModLang.address("esteemed"));
			case SENIOR -> Component.translatable(ModLang.address("senior"));
			case PEER -> Component.translatable(ModLang.address("peer"));
			case ABOVE -> Component.translatable(ModLang.address("above." + junior(voice)));
			case FAR_ABOVE -> Component.translatable(ModLang.address("far_above." + junior(voice)));
			case MORTAL -> Component.translatable(ModLang.address("mortal." + junior(voice)));
		};
	}

	/** Whether {@code player} belongs to {@code npc}'s own sect (SectMembership). */
	public static boolean ofItsSect(ServerPlayer player, CultivatorNpc npc) {
		Sect sect = npc.sect();
		if (sect == null || sect.extinct) return false;
		Sect own = SectMembership.sectOf(player.server, player.getUUID());
		return own != null && own.id.equals(sect.id);
	}

	/** The title {@code player} holds in {@code npc}'s own sect, if they are an Elder or above in it and outrank it; else null. */
	static Component sectTitle(ServerPlayer player, CultivatorNpc npc) {
		if (!ofItsSect(player, npc)) return null;
		Sect sect = npc.sect();
		NpcTitle held = SectMembership.titleOf(sect, player);
		if (!held.isElderOrAbove() || held.ordinal() >= npc.getTitle().ordinal()) return null;
		return held.getDisplayName(sect.isClan(), sect.isHoly(), sect.isImmortal());
	}

	/** The voice its words to a junior are in: the paths' own, the dead's; a mortal's as the neutral's. */
	private static String junior(String voice) {
		return switch (voice) {
			case "righteous", "devilish", "ghost" -> voice;
			default -> "neutral";
		};
	}

	private NpcAddress() {}
}
