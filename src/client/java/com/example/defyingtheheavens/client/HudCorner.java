package com.example.defyingtheheavens.client;

import java.util.ArrayList;
import java.util.List;

/**
 * What the top-left stack drew (VitalsHud's portrait and its bars, the thin bars under them: QiHud, NetherworldHud), row by row, so that
 * vanilla's boss bars keep clear of it (the user, 2026-10-26: "the qi bar covers the boss health bar"; mixin/BossHealthOverlayMixin):
 * beside it on their own row, as vanilla has them but moved right, or under it where the screen is too narrow for that. The boss bars
 * are drawn before the stack each frame, so they keep clear of what the frame before drew.
 */
public final class HudCorner {
	/** Kept between the stack and the boss bars. */
	private static final int GAP = 4;
	/** Vanilla's boss bars (BossHealthOverlay): their half width, the first one's top (its name's), its bar's top, from one to the next, a bar's height. */
	private static final int HALF = 91, NAME_TOP = 3, BAR_TOP = 12, STEP = 19, BAR_H = 5;
	private static final int[] NONE = {0, 0};

	private static List<int[]> drawn = new ArrayList<>(), drawing = new ArrayList<>();

	/** A row of the stack drawn this frame: from {@code top} down to {@code bottom}, out to {@code right}. */
	static void drew(int top, int bottom, int right) {
		drawing.add(new int[] {top, bottom, right});
	}

	/** At each frame's boss bars (drawn before the stack): what the frame before drew becomes what to keep clear of. */
	public static void nextFrame() {
		List<int[]> last = drawn;
		drawn = drawing;
		drawing = last;
		drawing.clear();
	}

	/**
	 * How far to move {@code count} boss bars, the widest name {@code widestName} across, on a screen {@code width} by {@code height}: not
	 * at all where they clear the stack; else right, beside it; else (no room beside it) down, under it.
	 */
	public static int[] bossShift(int width, int height, int count, int widestName) {
		if (count <= 0 || drawn.isEmpty()) return NONE;
		// As many as vanilla draws: it stops at the first past a third of the way down.
		int bars = 0;
		for (int y = BAR_TOP; bars < count; y += STEP) {
			bars++;
			if (y + STEP >= height / 3) break;
		}
		int top = NAME_TOP, bottom = BAR_TOP + STEP * (bars - 1) + BAR_H, half = Math.max(HALF, (widestName + 1) / 2);
		int right = 0, foot = 0;
		for (int[] row : drawn) {
			if (row[0] < bottom && row[1] > top) right = Math.max(right, row[2]);
			foot = Math.max(foot, row[1]);
		}
		int left = width / 2 - half;
		if (right == 0 || left >= right + GAP) return NONE;
		int dx = right + GAP - left;
		if (width / 2 + half + dx <= width - GAP) return new int[] {dx, 0};
		return new int[] {0, foot + GAP - top};
	}

	private HudCorner() {}
}
