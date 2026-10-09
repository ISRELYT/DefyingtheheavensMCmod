package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.Stage;

/** Client-side mirror of the local player's cultivation, filled by the SYNC packet. */
public final class ClientCultivationData {
	private static final PlayerCultivation DATA = new PlayerCultivation();
	private static boolean meditating;

	public static PlayerCultivation get() { return DATA; }
	public static boolean isMeditating() { return meditating; }

	public static void update(int realm, int stage, double cultivation, double qi, boolean meditating, boolean lowerRealmBound,
							  boolean inUpperRealm) {
		DATA.setState(Realm.byIndex(realm), Stage.byIndex(stage), cultivation);
		DATA.setQi(qi);
		DATA.setLowerRealmBound(lowerRealmBound);
		DATA.setInUpperRealm(inUpperRealm);
		ClientCultivationData.meditating = meditating;
	}

	/** Disconnect: don't show the last world's cultivation on the next server. */
	public static void clear() {
		DATA.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		DATA.setQi(0);
		DATA.setLowerRealmBound(false);
		DATA.setInUpperRealm(false);
		meditating = false;
	}

	private ClientCultivationData() {}
}
