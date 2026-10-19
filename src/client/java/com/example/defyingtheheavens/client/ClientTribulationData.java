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
	/** The Netherworld's tribulation (the ghost path's), not the heavens'. */
	private static boolean ghost;

	public static boolean isActive() { return active; }
	public static int getStrikesLeft() { return strikesLeft; }
	public static Component getTargetName() { return PlayerCultivation.rankName(targetRealm, targetStage); }
	public static boolean isGhost() { return ghost; }

	public static void update(boolean active, int strikesLeft, int realmIndex, int stageIndex, boolean ghost) {
		ClientTribulationData.active = active;
		ClientTribulationData.strikesLeft = strikesLeft;
		ClientTribulationData.targetRealm = Realm.byIndex(realmIndex);
		ClientTribulationData.targetStage = Stage.byIndex(stageIndex);
		ClientTribulationData.ghost = ghost;
	}

	public static void clear() {
		active = false;
		strikesLeft = 0;
		ghost = false;
	}

	private ClientTribulationData() {}
}
