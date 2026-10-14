package com.example.defyingtheheavens;

/**
 * For game tests not about the heaven methods whose cultivators must break into new realms: a breakthrough into a realm wants a heaven
 * method of it or deeper (HeavenMethods#opensWay), so they are given one that opens every realm. Fire's, whose passive (attack speed)
 * touches nothing those tests measure.
 */
final class TestMethods {
	/** A Four Axis fire method, every layer understood: every breakthrough's way open, to Four Axis Grand Perfection. */
	static void carryAll(PlayerCultivation c) {
		HeavenMethod fire = HeavenMethod.of(QiElement.FIRE, Realm.FOUR_AXIS);
		c.heaven().setMethod(fire, fire.layers(), 0);
	}

	/**
	 * {@code c} set to {@code realm} {@code stage} with none of it cultivated, a Four Axis fire method practised and understood as far
	 * as the stage (HeavenMethods#understoodTo): every breakthrough's way open, nothing left to settle.
	 */
	static void setAt(PlayerCultivation c, Realm realm, Stage stage) {
		c.setState(realm, stage, 0);
		HeavenMethod fire = HeavenMethod.of(QiElement.FIRE, Realm.FOUR_AXIS);
		c.heaven().setMethod(fire, HeavenMethods.understoodTo(c, fire), 0);
		c.heaven().setActiveMethod(fire);
	}

	private TestMethods() {}
}
