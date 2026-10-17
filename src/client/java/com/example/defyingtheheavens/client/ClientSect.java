package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.NpcTitle;

/** The local player's sect and their title in it (S2C sect_status, SectMembership#sendStatus), for the Stats tab. */
public final class ClientSect {
	private static String name = "";
	private static NpcTitle title;

	static void set(String sectName, int titleIndex) {
		name = sectName == null ? "" : sectName;
		title = titleIndex < 0 ? null : NpcTitle.byIndex(titleIndex);
	}

	/** The sect's name, or "" for none. */
	public static String name() { return name; }

	/** Their title in it, or null for none. */
	public static NpcTitle title() { return title; }

	public static boolean member() { return !name.isEmpty() && title != null; }

	static void clear() {
		name = "";
		title = null;
	}

	private ClientSect() {}
}
