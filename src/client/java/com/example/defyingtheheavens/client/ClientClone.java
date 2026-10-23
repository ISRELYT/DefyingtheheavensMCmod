package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Clones;

/**
 * The local player's clone as the server tells it (S2C clone_state, Clones): whether they play it now, and how their main body fares
 * where it sits, for the HUD (SoulHud). Whether the record shown is a clone's (its way back on the wheel: Ability#CLONE) comes with
 * the sync (ClientCultivationData#inClone).
 */
public final class ClientClone {
	private static float mainHealth, mainMax;

	static void set(boolean in, float health, float max) {
		ClientCultivationData.setInClone(in);
		mainHealth = health;
		mainMax = max;
	}

	public static float mainHealth() { return mainHealth; }

	public static float mainMax() { return mainMax; }

	public static void register() {
		Clones.clientCloneRecord = c -> c == ClientCultivationData.get() && ClientCultivationData.inClone();
	}

	static void clear() {
		mainHealth = mainMax = 0;
	}

	private ClientClone() {}
}
