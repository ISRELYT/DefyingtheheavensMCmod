package com.example.defyingtheheavens;

/**
 * For game tests not about the methods whose cultivators must gather as they climb (the methods overhaul, 2026-10-31: without a method's
 * volume of one's realm, no gain, HeavenMethods#canGain; before, a breakthrough waited on a scripture of the realm ahead): they are given
 * a Four Axis scripture, whose Qi Circulation volumes reach every realm up to Heavenly Being (Four Axis itself has none: its axes fill
 * it). Fire's, whose passive (attack speed) touches nothing those tests measure.
 */
final class TestMethods {
	/** A Four Axis fire scripture, every layer understood: Circulation's volumes to Heavenly Being. */
	static void carryAll(PlayerCultivation c) {
		HeavenMethod fire = HeavenMethod.of(QiElement.FIRE, Realm.FOUR_AXIS);
		c.heaven().setMethod(fire, fire.layers(), 0);
	}

	/**
	 * {@code c} set to {@code realm} {@code stage} with none of it cultivated, a Four Axis fire scripture practised and understood as far
	 * as the stage (HeavenMethods#understoodTo): Circulation's volumes to Heavenly Being, nothing left to settle.
	 */
	static void setAt(PlayerCultivation c, Realm realm, Stage stage) {
		c.setState(realm, stage, 0);
		HeavenMethod fire = HeavenMethod.of(QiElement.FIRE, Realm.FOUR_AXIS);
		c.heaven().setMethod(fire, HeavenMethods.understoodTo(c, fire), 0);
		c.heaven().setActiveMethod(fire);
	}

	private TestMethods() {}
}
