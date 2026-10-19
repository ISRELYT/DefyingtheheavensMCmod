package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * The user's rule of 2026-10-19: the stat points' buffs are the cultivator's, shared by every path (StatPoints#poolFactor,
 * #cultivationFactor, #gatherFactor); and the Netherworld Breathing Art only on the Ghost Gate's stele, though the dead still
 * know and teach it (GhostMethod#knownWeight).
 */
public class SharedStatsGameTests implements FabricGameTest {
	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-9;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void spiritAndInsightServeTheGhostPathToo(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.awaken(0);
		NetherworldState s = c.netherworld();
		s.setMethod(GhostMethod.NETHERWORLD_BREATHING, 1, 0);
		s.setPath(0, 0);
		double rate = GhostMethods.ratePerSecond(c, GhostQi.NETHERWORLD), pool = GhostQi.poolMax(c), fill = GhostQi.fillPerSecond(c, GhostQi.NETHERWORLD);
		double qi = c.maxQi(), cultivation = c.meditationCultivationPerSecond(0);
		c.setStatPoints(StatPoints.Stat.INSIGHT, 10);
		c.setStatPoints(StatPoints.Stat.SPIRIT, 10);
		helper.assertTrue(near(GhostMethods.ratePerSecond(c, GhostQi.NETHERWORLD), rate * 1.10) && near(c.meditationCultivationPerSecond(0), cultivation * 1.10),
				"Insight: +10% cultivation on the ghost path as on the heaven path");
		helper.assertTrue(near(GhostQi.fillPerSecond(c, GhostQi.NETHERWORLD), fill * 1.05), "...and half that to ghostly qi's filling, as to qi's");
		helper.assertTrue(near(GhostQi.poolMax(c), pool * 1.10) && near(c.maxQi(), qi * 1.10), "Spirit: +10% to both pools");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theBreathingArtIsCarvedOnlyAtTheGateYetTheDeadStillKnowIt(GameTestHelper helper) {
		helper.assertTrue(GhostMethod.NETHERWORLD_BREATHING.steleWeight() == 0 && GhostMethod.NETHERWORLD_BREATHING.peakWeight() == 0,
				"No stele out in the realm carries the Breathing Art");
		helper.assertTrue(GhostMethod.NETHERWORLD_BREATHING.knownWeight() > GhostMethod.GHOST_FIRE_SCRIPTURE.knownWeight()
				&& GhostMethod.GHOST_FIRE_SCRIPTURE.knownWeight() > GhostMethod.WANDERING_SOUL_STEPS.knownWeight(),
				"...though of the first three the dead know it most often");
		helper.succeed();
	}
}
