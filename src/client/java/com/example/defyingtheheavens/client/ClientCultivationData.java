package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.MortalStage;
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
							  boolean inUpperRealm, int disabledAbilities, int pressureStages, double pressurePenalty,
							  boolean mortal, int mortalStage, double tempering, int preparedRealm, double preparedBonus, double qiBoost,
							  double pillResistance, double medicinalQi) {
		DATA.setState(Realm.byIndex(realm), Stage.byIndex(stage), cultivation);
		DATA.setMortal(mortal); // after setState, which marks a cultivator
		DATA.setMortalState(MortalStage.byIndex(mortalStage), tempering);
		DATA.prepareBreakthrough(preparedRealm >= 0 && preparedRealm < Realm.values().length ? Realm.byIndex(preparedRealm) : null,
				preparedBonus);
		DATA.setQiBoost(qiBoost);
		DATA.setPillResistance(pillResistance);
		DATA.setMedicinalQi(medicinalQi);
		DATA.setDisabledAbilityMask(disabledAbilities);
		DATA.setLowerRealmBound(lowerRealmBound);
		DATA.setInUpperRealm(inUpperRealm);
		DATA.setPressure(pressureStages, pressurePenalty);
		DATA.setQi(qi); // after the pressure, which sets the maximum it is read against
		ClientCultivationData.meditating = meditating;
	}

	/** Disconnect: don't show the last world's cultivation on the next server. */
	public static void clear() {
		DATA.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		DATA.setMortal(true);
		DATA.setMortalState(MortalStage.LOW, 0);
		DATA.prepareBreakthrough(null, 0);
		DATA.setQiBoost(0);
		DATA.setPillResistance(0);
		DATA.setMedicinalQi(0);
		DATA.setQi(0);
		int offByDefault = 0;
		for (Ability ability : Ability.offByDefault()) offByDefault |= 1 << ability.ordinal();
		DATA.setDisabledAbilityMask(offByDefault);
		DATA.setLowerRealmBound(false);
		DATA.setInUpperRealm(false);
		DATA.setPressure(0, 0);
		meditating = false;
	}

	private ClientCultivationData() {}
}
