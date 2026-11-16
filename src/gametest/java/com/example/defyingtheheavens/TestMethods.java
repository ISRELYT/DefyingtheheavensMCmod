package com.example.defyingtheheavens;

/**
 * For game tests not about the methods whose cultivators must gather as they climb (the methods overhaul, 2026-10-31: without a method's
 * volume of one's realm, no gain, HeavenMethods#canGain; before, a breakthrough waited on a scripture of the realm ahead): they are given
 * Qi Circulation's volumes to Heavenly Being (its own Method Manuals' since 2026-11-16; before, a Four Axis scripture carried them; Four
 * Axis itself has none: its axes fill it), and a Four Axis fire scripture to practise. Fire's, whose passive (attack speed) touches
 * nothing those tests measure.
 */
final class TestMethods {
	/** Qi Circulation's volumes to Heavenly Being, and a Four Axis fire scripture known (understood, as every scripture, as far as the realm). */
	static void carryAll(PlayerCultivation c) {
		c.raiseReach(CultivationMethod.QI_CIRCULATION, Realm.HEAVENLY_BEING);
		HeavenMethod fire = HeavenMethod.of(QiElement.FIRE, Realm.FOUR_AXIS);
		c.heaven().setMethod(fire, 1, 0);
	}

	/**
	 * {@code c} set to {@code realm} {@code stage} with none of it cultivated, Qi Circulation's volumes to Heavenly Being, and a Four Axis
	 * fire scripture practised (understood as far as the stage, as every scripture is since 2026-11-16: HeavenState#layer).
	 */
	static void setAt(PlayerCultivation c, Realm realm, Stage stage) {
		c.setState(realm, stage, 0);
		carryAll(c);
		c.heaven().setActiveMethod(HeavenMethod.of(QiElement.FIRE, Realm.FOUR_AXIS));
	}

	private TestMethods() {}
}
