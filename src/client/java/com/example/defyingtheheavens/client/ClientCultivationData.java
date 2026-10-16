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
							  double pillResistance, double medicinalQi, int alignment, double gearQiBonus, int[] wheel, int soulState,
								  boolean heavensLeave, int favour) {
		DATA.setState(Realm.byIndex(realm), Stage.byIndex(stage), cultivation);
		DATA.setHeavensLeave(heavensLeave); // after setState, which grants it to anyone past Qi Refining Mid
		DATA.setFavour(favour);
		DATA.setSoulState(com.example.defyingtheheavens.SoulProjection.State.byIndex(soulState));
		DATA.setAlignment(alignment);
		DATA.setGearQiBonus(gearQiBonus);
		DATA.setMortal(mortal); // after setState, which marks a cultivator
		DATA.setMortalState(MortalStage.byIndex(mortalStage), tempering);
		DATA.prepareBreakthrough(preparedRealm >= 0 && preparedRealm < Realm.values().length ? Realm.byIndex(preparedRealm) : null,
				preparedBonus);
		DATA.setQiBoost(qiBoost);
		DATA.setPillResistance(pillResistance);
		DATA.setMedicinalQi(medicinalQi);
		DATA.setDisabledAbilityMask(disabledAbilities);
		for (int i = 0; i < wheel.length; i++) DATA.setWheelSlot(i, Ability.byIndex(wheel[i]));
		DATA.setLowerRealmBound(lowerRealmBound);
		DATA.setInUpperRealm(inUpperRealm);
		DATA.setPressure(pressureStages, pressurePenalty);
		DATA.setQi(qi); // after the pressure, which sets the maximum it is read against
		ClientCultivationData.meditating = meditating;
	}

	/** The body the local player was born with (Physique): its ordinal (-1 none yet), its roots' elements as a bit mask, body refining. */
	public static void physique(int physique, int roots, int bodyRefining) {
		DATA.setPhysique(com.example.defyingtheheavens.Physique.byIndex(physique));
		java.util.List<com.example.defyingtheheavens.QiElement> elements = new java.util.ArrayList<>();
		for (com.example.defyingtheheavens.QiElement element : com.example.defyingtheheavens.QiElement.five()) {
			if ((roots & 1 << element.ordinal()) != 0) elements.add(element);
		}
		DATA.setRootElements(elements);
		DATA.setBodyRefining(bodyRefining);
	}

	/** Disconnect: don't show the last world's cultivation on the next server. */
	public static void clear() {
		physique(-1, 0, 0);
		DATA.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		DATA.setHeavensLeave(false);
		DATA.setFavour(0);
		DATA.setMortal(true);
		DATA.setMortalState(MortalStage.LOW, 0);
		DATA.prepareBreakthrough(null, 0);
		DATA.setQiBoost(0);
		DATA.setPillResistance(0);
		DATA.setMedicinalQi(0);
		DATA.setQi(0);
		DATA.setAlignment(0);
		DATA.setGearQiBonus(0);
		int offByDefault = 0;
		for (Ability ability : Ability.offByDefault()) offByDefault |= 1 << ability.ordinal();
		DATA.setDisabledAbilityMask(offByDefault);
		for (int i = 0; i < PlayerCultivation.WHEEL_SLOTS; i++) DATA.setWheelSlot(i, Ability.byIndex(i));
		DATA.setLowerRealmBound(false);
		DATA.setInUpperRealm(false);
		DATA.setPressure(0, 0);
		DATA.setSoulState(com.example.defyingtheheavens.SoulProjection.State.NONE);
		meditating = false;
	}

	private ClientCultivationData() {}
}
