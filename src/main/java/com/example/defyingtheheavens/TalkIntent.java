package com.example.defyingtheheavens;

/**
 * What a player can mean by talking to a cultivator: the talk wheel's choices (see {@link TalkSystem}). Ask opens the
 * dialogue box; the others are answered in one line. Trade and Spar come later.
 */
public enum TalkIntent {
	GREET("greet"),
	ASK("ask"),
	GIFT("gift"),
	PROVOKE("provoke");

	private final String id;

	TalkIntent(String id) {
		this.id = id;
	}

	public String getId() { return id; }

	/** Answered in one line, with no dialogue box. */
	public boolean isQuick() { return this != ASK; }

	public int bit() { return 1 << ordinal(); }

	public static TalkIntent byIndex(int index) {
		return index >= 0 && index < values().length ? values()[index] : null;
	}
}
