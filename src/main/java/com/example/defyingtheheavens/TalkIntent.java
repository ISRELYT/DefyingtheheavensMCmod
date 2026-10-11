package com.example.defyingtheheavens;

/**
 * What a player can mean by talking to a cultivator: the talk wheel's choices (see {@link TalkSystem}), in the order the
 * wheel shows them. The six of the design are Greet, Ask, Spar, Trade, Gift and Provoke; Demand and Release appear when
 * you press your realm on someone two realms beneath you, Spare when they beg for their life, Leave when they won't talk
 * (an enemy of their path) or beg. Ask and Spar open the dialogue box; Trade opens the trading screen; the rest are
 * answered in one line.
 */
public enum TalkIntent {
	GREET("greet"),
	ASK("ask"),
	SPAR("spar"),
	TRADE("trade"),
	GIFT("gift"),
	PROVOKE("provoke"),
	DEMAND("demand"),
	RELEASE("release"),
	SPARE("spare"),
	LEAVE("leave");

	private final String id;

	TalkIntent(String id) {
		this.id = id;
	}

	public String getId() { return id; }

	/** Answered in one line (or a screen of its own, Trade), with no dialogue box. */
	public boolean isQuick() { return this != ASK && this != SPAR; }

	public int bit() { return 1 << ordinal(); }

	public static TalkIntent byIndex(int index) {
		return index >= 0 && index < values().length ? values()[index] : null;
	}
}
