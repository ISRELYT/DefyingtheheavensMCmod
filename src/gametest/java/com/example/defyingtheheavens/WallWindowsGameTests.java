package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.Arrays;
import java.util.List;

/**
 * The windows of a wall (2026-11-13, the user's screenshot of the Jade Capital's gate pavilion: "the fences not being in the correct spot,
 * its not symmetrical"): SectPiece#bayWindows sets them in the middle of each bay between the wall's pillars, mirrored about its middle,
 * never on a pillar nor in the doorway's bay. Before, the gate pavilions laid them every four blocks from one end.
 */
public class WallWindowsGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void windowsMirrorAboutTheDoorAndKeepOffThePillars(GameTestHelper helper) {
		// The gates' pavilions (16 and 22 long: the cities' 25-wide gates and the capital's 31-wide) and walls of other lengths.
		for (int length : new int[] {16, 22, 9, 12, 13, 18, 30}) {
			for (boolean door : new boolean[] {true, false}) {
				int[] pillars = SectPiece.pillarSpots(length, 4, true);
				List<Integer> windows = SectPiece.bayWindowSpots(length, 4, door);
				helper.assertTrue(!windows.isEmpty(), "A wall " + length + " long has windows");
				int middle = length / 2;
				for (int at : windows) {
					helper.assertTrue(Arrays.binarySearch(pillars, at) < 0, "No window on a pillar (" + length + ": " + at + " in " + Arrays.toString(pillars) + ")");
					helper.assertTrue(windows.contains(length - at), "Mirrored about the middle (" + length + ": " + at + ", " + windows + ")");
					if (door) {
						// The doorway's bay: between the pillars either side of the middle.
						int below = -1, above = length + 1;
						for (int p : pillars) {
							if (p < middle) below = Math.max(below, p);
							if (p > middle) above = Math.min(above, p);
						}
						helper.assertTrue(at < below || at > above, "None in the doorway's bay (" + length + ": " + at + ")");
					}
				}
			}
		}
		// The capital's gate (its pavilion's wall 22 long, the door's pillars at 9 and 13, the doorway 10 to 12): none in them now, where the
		// user saw one in a door pillar and one in the doorway's top corner.
		List<Integer> capital = SectPiece.bayWindowSpots(22, 4, true);
		helper.assertTrue(Arrays.equals(SectPiece.pillarSpots(22, 4, true), new int[] {0, 3, 6, 9, 13, 16, 19, 22}), "The capital's pavilion's pillars");
		for (int at = 9; at <= 13; at++) helper.assertTrue(!capital.contains(at), "Nothing in the door's pillars or the doorway (" + capital + ")");
		helper.succeed();
	}
}
