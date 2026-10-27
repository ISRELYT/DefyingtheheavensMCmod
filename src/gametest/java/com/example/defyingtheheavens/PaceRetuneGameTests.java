package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * The user's retune of 2026-10-27 ("cultivation is too fast with all the bonuses"; their picks: slower stages and weaker boosts both,
 * and no longer-lived demons): every stage {@link PlayerCultivation#STAGE_LENGTH} times its realm's base on every path (the world's
 * cultivators at their old pace); a pedestal 1% to 25%; the Inner Realm x1.25, saving years too; and the talent race (the grades'
 * talents and the shares), so on a red mat in the Inner Realm a Mid grade reaches Core Formation, a High one Void Shattering, a
 * Supreme one Sacred Vessel and an Immortal one True Immortal, a Low one with everything the lower realms give stops inside Core
 * Formation's Grand Perfection, and the Immortal's road with everything but a stone and a stele takes some five hours (ten before the
 * halving of the same night). No players: one batch.
 */
public class PaceRetuneGameTests implements FabricGameTest {
	/** Where years run out for {@code k} (talent x what saves years) with {@code extra} years besides: the realm's index, and how far into it (1 past Entering Nirvana). */
	private static double[] reach(double k, double extra) {
		double left = extra;
		for (int r = 0; r <= Realm.ENTERING_NIRVANA.ordinal(); r++) {
			Realm realm = Realm.byIndex(r);
			double years = realm == Realm.QI_REFINING ? Lifespan.BASE_YEARS - Lifespan.START_AGE : Lifespan.realmYears(realm);
			double avail = left + years, cost = Lifespan.realmSeclusion(realm) / k;
			if (cost > avail) return new double[] {r, avail / cost};
			left = avail - cost;
		}
		return new double[] {Realm.TRUE_IMMORTAL.ordinal(), 0};
	}

	/** How far into its realm Grand Perfection begins (by the stages' time weights). */
	private static double grandPerfectionFrom() {
		double all = 0, before = 0;
		for (Stage s : Stage.values()) {
			all += s.timeWeight();
			if (s != Stage.GRAND_PERFECTION) before += s.timeWeight();
		}
		return before / all;
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "pace_retune")
	public void slowerStagesWeakerBoostsAndTheTalentLines(GameTestHelper helper) {
		// The stages: 2.1 times their realms' bases, every path (4.2 at first, halved the same night).
		helper.assertTrue(PlayerCultivation.STAGE_LENGTH == 2.1 && Math.abs(PlayerCultivation.required(Realm.QI_REFINING, Stage.EARLY) - 100 * 1.3 * 2.1) < 1.0e-9
				&& Math.abs(PlayerCultivation.required(Realm.TRUE_IMMORTAL, Stage.EARLY) - Realm.TRUE_IMMORTAL.getCultivationBase() * 1.3 * 1000 * 2.1) < 1,
				"Every stage 2.1 times its realm's base");
		int rank = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID);
		helper.assertTrue(DemonPath.required(rank) == PlayerCultivation.required(Realm.CORE_FORMATION, Stage.MID)
				&& Math.abs(GhostMethods.required(rank) - Math.round(GhostMethods.STAGE_SECONDS * Math.pow(GhostMethods.REALM_TIME_STEP, 2)
						* Stage.MID.timeWeight() * GhostMethods.realmFactor(Realm.CORE_FORMATION) * 2.1)) < 1, "The Demon Path's and the ghost path's too");
		// The boosts: pedestals 1% to 25% (13% at a hundred years), the Inner Realm x1.25.
		helper.assertTrue(Math.abs(CultivationBoost.pedestalBonus(1) - 0.01) < 1.0e-9 && Math.abs(CultivationBoost.pedestalBonus(100) - 0.13) < 1.0e-9
				&& Math.abs(CultivationBoost.pedestalBonus(10000) - 0.25) < 1.0e-9 && Math.abs(CultivationBoost.pedestalBonus(99999) - 0.25) < 1.0e-9,
				"A pedestal 1% to 25%");
		helper.assertTrue(InnerRealm.CULTIVATION_BONUS == 1.25, "The Inner Realm x1.25");
		// The talent ladder (the user's of 2026-10-27): on a red mat in the Inner Realm alone, a Mid grade reaches Core Formation (it was
		// Heavenly Being at x13, till "lower the gap between low and mid"), a High one Void Shattering, a Supreme one Sacred Vessel, an
		// Immortal one True Immortal; a Mid one with everything Sacred Vessel.
		double pedestal = CultivationBoost.pedestalBonus(10000), pedestals = 4 * pedestal, clothes = 4 * ClothingTier.IMMORTAL.getMeditationPerPiece();
		double full = 1 + CultivationBoost.RED_MAT + clothes + pedestals + CultivationBoost.HEIGHT_MAX + CultivationBoost.TRANQUIL;
		double method = HeavenMethods.FACTOR_CAP, inner = InnerRealm.CULTIVATION_BONUS, stone = 1 + SpiritStoneItem.MEDITATION_BOOST, stele = HeavenMethods.STELE_BONUS;
		double low = Physique.Grade.LOW.talent(), matAndInner = (1 + CultivationBoost.RED_MAT) * inner;
		helper.assertTrue(Physique.Grade.MID.talent() == 5 && Physique.Grade.HIGH.talent() == 24 && Physique.Grade.SUPREME.talent() == 40
				&& Physique.Grade.IMMORTAL.talent() == 64 && low == 1, "The grades' talents: x1, x5, x24, x40, x64");
		Realm[] ladder = {Realm.CORE_FORMATION, Realm.VOID_SHATTERING, Realm.SACRED_VESSEL, Realm.TRUE_IMMORTAL};
		Physique.Grade[] grades = {Physique.Grade.MID, Physique.Grade.HIGH, Physique.Grade.SUPREME, Physique.Grade.IMMORTAL};
		for (int i = 0; i < grades.length; i++) {
			double[] at = reach(grades[i].talent() * matAndInner, 0);
			helper.assertTrue(at[0] == ladder[i].ordinal(), "A " + grades[i] + " grade on a red mat in the Inner Realm reaches " + ladder[i] + " ("
					+ Realm.byIndex((int) at[0]) + " " + at[1] + ")");
		}
		// A Low talent with everything the lower realms give (and the best Longevity Pills' 400 years): inside Core Formation's Grand Perfection.
		double[] lowBest = reach(low * method * stele * full * inner * stone, 400);
		helper.assertTrue(lowBest[0] == Realm.CORE_FORMATION.ordinal() && lowBest[1] >= grandPerfectionFrom() && lowBest[1] < 1,
				"A Low talent with everything stops inside Core Formation's Grand Perfection (" + Realm.byIndex((int) lowBest[0]) + " " + lowBest[1] + ")");
		double[] midBest = reach(Physique.Grade.MID.talent() * method * stele * full * inner * stone, 0);
		helper.assertTrue(midBest[0] == Realm.SACRED_VESSEL.ordinal(), "A Mid talent with everything reaches Sacred Vessel (" + Realm.byIndex((int) midBest[0]) + ")");
		// Its road's time: some five hours for an Immortal talent so kitted (clothing's x1.6 to the gathering too); ten before the halving.
		double seconds = 0;
		for (int r = 0; r <= Realm.ENTERING_NIRVANA.ordinal(); r++) {
			Realm realm = Realm.byIndex(r);
			for (Stage stage : Stage.values()) {
				double home = PlayerCultivation.qiNeed(realm); // in the realm's own world, its qi as much as its need
				seconds += PlayerCultivation.required(realm, stage) / (realm.getCultivationPerSecond() * stage.getPowerMultiplier() * home);
			}
		}
		double hours = seconds / (Physique.Grade.IMMORTAL.speed() * method * (1 + 4 * ClothingTier.IMMORTAL.getQiGatherPerPiece()) * full * inner) / 3600;
		helper.assertTrue(hours >= 5 && hours < 5.5, "An Immortal talent so kitted takes five hours to True Immortal (" + hours + ")");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "pace_retune")
	public void demonsLiveNoLongerAndTheWorldKeepsItsPace(GameTestHelper helper) {
		PlayerCultivation human = new PlayerCultivation(), demon = new PlayerCultivation();
		human.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
		demon.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
		demon.demon().setBloodline(Bloodline.TIGER);
		helper.assertTrue(DemonPath.isDemon(demon) && Lifespan.span(demon) == Lifespan.span(human), "A demon lives as long as a man");
		// The world's cultivators: a stage as long as before for them (their rate made up).
		PlayerCultivation scratch = new PlayerCultivation();
		scratch.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
		double npcSeconds = scratch.cultivationRequired() / (scratch.cultivationPerSecond() * CultivatorNpc.NPC_RATE * PlayerCultivation.STAGE_LENGTH);
		double before = Realm.CORE_FORMATION.getCultivationBase() * Stage.EARLY.getCultivationMultiplier() / (scratch.cultivationPerSecond() * CultivatorNpc.NPC_RATE);
		helper.assertTrue(Math.abs(npcSeconds - before) < 1.0e-6, "A sect cultivator's stage as long as it was");
		helper.succeed();
	}
}
