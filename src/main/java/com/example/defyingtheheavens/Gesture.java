package com.example.defyingtheheavens;

/**
 * A greeting made with the body (TalkSystem's Greet): a bow to a senior, cupped fist to palm to an equal or a junior. Sent to
 * everyone watching (S2C gesture) and drawn for {@link #TICKS} by the client's model (HumanoidModelMixin).
 */
public enum Gesture {
	BOW,
	CUP_FIST;

	public static final int TICKS = 30;

	public static Gesture byIndex(int index) {
		return index >= 0 && index < values().length ? values()[index] : null;
	}
}
