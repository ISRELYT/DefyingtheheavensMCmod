package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.Stage;
import net.minecraft.network.chat.Component;

/**
 * Client mirror of the local player's tribulation, fed by the TRIBULATION_STATE packet (on every strike and once per second); the
 * lifespan tribulation's and space travel's years besides, for TribulationHud's lines.
 */
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
		lifespanActive = false;
		lifespanPower = 0;
		inSpace = false;
	}

	// --- The lifespan tribulation (LifespanTribulation), fed by the LIFESPAN_TRIBULATION packet ---

	private static boolean lifespanActive, lifespanPaused, lifespanGhost;
	private static float lifespanPower;

	public static boolean isLifespanActive() { return lifespanActive; }
	public static float lifespanPower() { return lifespanPower; }
	public static boolean isLifespanPaused() { return lifespanPaused; }
	public static boolean isLifespanGhost() { return lifespanGhost; }

	public static void updateLifespan(boolean active, float power, boolean paused, boolean ghost) {
		lifespanActive = active;
		lifespanPower = power;
		lifespanPaused = paused;
		lifespanGhost = ghost;
	}

	// --- Space travel's years (AstralRealm#travel), fed by the SPACE_YEARS packet ---

	private static boolean inSpace;
	private static float spaceTrip, spaceLeft, spaceHome;

	public static boolean isInSpace() { return inSpace; }
	public static float spaceTrip() { return spaceTrip; }
	public static float spaceLeft() { return spaceLeft; }
	public static float spaceHome() { return spaceHome; }

	public static void updateSpace(boolean in, float trip, float left, float home) {
		inSpace = in;
		spaceTrip = trip;
		spaceLeft = left;
		spaceHome = home;
	}

	private ClientTribulationData() {}
}
