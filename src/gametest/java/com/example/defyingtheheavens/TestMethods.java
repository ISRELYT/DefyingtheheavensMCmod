package com.example.defyingtheheavens;

/**
 * For game tests not about the heaven methods, whose cultivators must still step from stage to stage: cultivation keeps step with
 * the heaven methods (HeavenMethods#reach), so they are given one that carries them. Fire's, whose passive (attack speed) touches
 * nothing those tests measure.
 */
final class TestMethods {
	/** A Four Axis fire method at its last layer, begun: {@code c}'s cultivation is carried all the way, to Four Axis Grand Perfection. */
	static void carryAll(PlayerCultivation c) {
		HeavenMethod fire = HeavenMethod.of(QiElement.FIRE, Realm.FOUR_AXIS);
		c.heaven().setMethod(fire, fire.layers(), 0);
	}

	private TestMethods() {}
}
