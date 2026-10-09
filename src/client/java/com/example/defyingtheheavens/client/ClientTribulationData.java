package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.Stage;
import net.minecraft.network.chat.Component;

/** Client mirror of the local player's tribulation, fed by the TRIBULATION_STATE packet (on every strike and once per second). */
public final class ClientTribulationData {
	private static boolean active;
	private static int strikesLeft;
	private static Realm targetRealm = Realm.FOUNDATION_BUILDING;
	private static Stage targetStage = Stage.EARLY;

	public static boolean isActive() { return active; }
	public static int getStrikesLeft() { return strikesLeft; }
	public static Component getTargetName() { return PlayerCultivation.rankName(targetRealm, targetStage); }

	public static void update(boolean active, int strikesLeft, int realmIndex, int stageIndex) {
		ClientTribulationData.active = active;
		ClientTribulationData.strikesLeft = strikesLeft;
		ClientTribulationData.targetRealm = Realm.byIndex(realmIndex);
		ClientTribulationData.targetStage = Stage.byIndex(stageIndex);
	}

	public static void clear() {
		active = false;
		strikesLeft = 0;
	}

	private ClientTribulationData() {}
}
