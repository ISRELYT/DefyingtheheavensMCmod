package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * The methods overhaul, phase 1 (the user's rules, 2026-10-31): a method is how one cultivates, in volumes of a realm each, and with no
 * volume of one's realm nothing is gathered (HeavenMethods#canGain, GhostMethods#canGain, DemonPath#canGain); the heaven and ghost paths
 * break through without one, the Demon Path never without its blood; at Four Axis the axes are everyone's method (FourAxis#fillBars);
 * and a first method is guaranteed (TalkSystem#teachBasics, GhostMethods#teachTheDead).
 */
public class MethodVolumesGameTests implements FabricGameTest {
	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-6 * Math.max(1, Math.abs(b));
	}

	/** A survival player, out of every fight, standing still on a block of its own: ready to meditate. */
	private static ServerPlayer sitter(GameTestHelper helper) {
		helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.getAbilities().invulnerable = true; // a blow ends a meditation
		net.minecraft.world.phys.Vec3 at = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
		player.moveTo(at.x, at.y, at.z);
		player.setOnGround(true);
		CultivationManager.get(player).awaken(0);
		return player;
	}

	private static void leave(ServerPlayer player) {
		MeditationManager.forget(player.getUUID());
		Spells.forget(player.getUUID());
		HeavenMethods.forget(player.getUUID());
		GhostSystem.forget(player.getUUID());
		TwoPaths.forget(player.getUUID());
		player.server.getPlayerList().remove(player);
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void noVolumeOfTheRealmNoGain(GameTestHelper helper) {
		ServerPlayer player = sitter(helper);
		PlayerCultivation c = CultivationManager.get(player);
		c.setState(Realm.FOUNDATION_BUILDING, Stage.EARLY, 0);
		HeavenState s = c.heaven();
		HeavenMethod fire = HeavenMethod.of(QiElement.FIRE, Realm.QI_REFINING);
		s.setMethod(fire, fire.layers(), 0);
		s.setActiveMethod(fire);
		c.setMedicinalQi(50);
		helper.assertTrue(!HeavenMethods.canGain(c) && HeavenMethods.canGain(withRealm(c, Realm.QI_REFINING)),
				"Broken into Foundation Building with a Qi Refining scripture alone: no volume of it");
		MeditationManager.start(player);
		helper.assertTrue(MeditationManager.isMeditating(player.getUUID()), "It sits down to meditate all the same");
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(c.getCultivation() == 0 && near(c.getMedicinalQi(), 50),
					"Nothing gathered, and the pill's qi waits unrefined (" + c.getCultivation() + ", " + c.getMedicinalQi() + ")");
			HeavenMethods.give(player, HeavenMethod.of(QiElement.WOOD, Realm.FOUNDATION_BUILDING));
			helper.assertTrue(HeavenMethods.canGain(c), "A Foundation Building scripture of any element carries its volume");
		});
		helper.runAfterDelay(80, () -> {
			try {
				helper.assertTrue(c.getCultivation() > 0 && c.getMedicinalQi() < 50,
						"Now it gathers, and refines the pill's qi (" + c.getCultivation() + ", " + c.getMedicinalQi() + ")");
				helper.succeed();
			} finally {
				leave(player);
			}
		});
	}

	/** A copy of {@code c}'s record standing in {@code realm}'s Early (its scriptures the same). */
	private static PlayerCultivation withRealm(PlayerCultivation c, Realm realm) {
		PlayerCultivation copy = PlayerCultivation.load(c.save());
		copy.setState(realm, Stage.EARLY, 0);
		return copy;
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void theGhostPathToo(GameTestHelper helper) {
		ServerPlayer player = sitter(helper);
		PlayerCultivation c = CultivationManager.get(player);
		NetherworldState s = c.netherworld();
		GhostSystem.makeGhost(player); // taught Netherworld Breathing
		GhostMethods.setPath(s, PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY));
		s.setDensity(GhostQi.NETHERWORLD);
		helper.assertTrue(s.knows(GhostMethod.NETHERWORLD_BREATHING) && !GhostMethods.canGain(c) && GhostMethods.opensWay(c, Realm.CORE_FORMATION),
				"A ghost path in Foundation Building with only the Breathing Art: no volume of it (its way on open all the same)");
		MeditationManager.start(player);
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(s.pathCultivation() == 0, "The ghost path gathers nothing (" + s.pathCultivation() + ")");
			GhostMethods.give(player, GhostMethod.YELLOW_SPRINGS_MARROW);
			helper.assertTrue(GhostMethods.canGain(c), "The Marrow Art carries Foundation Building's volume");
		});
		helper.runAfterDelay(80, () -> {
			try {
				helper.assertTrue(s.pathCultivation() > 0, "Now it gathers (" + s.pathCultivation() + ")");
				helper.succeed();
			} finally {
				leave(player);
			}
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theAxesFillEveryPathAtFourAxis(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.awaken(0);
		int early = PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.EARLY);
		c.setState(Realm.FOUR_AXIS, Stage.EARLY, 0);
		TestMethods.carryAll(c);
		NetherworldState s = c.netherworld();
		s.setMethod(GhostMethod.SIX_PATHS_CANON, 1, 0);
		GhostMethods.setPath(s, early);
		c.demon().setBloodline(Bloodline.TIGER);
		c.demon().set(early, 0);
		c.demon().setBloodRealm(Realm.FOUR_AXIS.ordinal());
		helper.assertTrue(!HeavenMethods.canGain(c) && !GhostMethods.canGain(c) && !DemonPath.canGain(c),
				"At Four Axis no path's method has a volume, its own scriptures and foundations though it knows");
		int need = FourAxis.need(c, FourAxis.Axis.LONGEVITY);
		c.setAxisProgress(FourAxis.Axis.LONGEVITY, need / 2);
		double half = (need / 2) / (double) need;
		helper.assertTrue(FourAxis.fillBars(c), "The axis moved: the bars follow it");
		helper.assertTrue(near(c.getCultivation(), c.cultivationRequired() * half) && near(s.pathCultivation(), GhostMethods.required(early) * half)
				&& near(c.demon().cultivation(), DemonPath.required(early) * half), "Every path's Four Axis Early bar as far along as the Axis of Longevity");
		helper.assertTrue(!FourAxis.fillBars(c), "Nothing moved: nothing set again");
		c.setAxisProgress(FourAxis.Axis.LONGEVITY, need);
		FourAxis.fillBars(c);
		helper.assertTrue(c.isAtBottleneck() && !c.isHeldByAxis() && GhostMethods.atBottleneck(c) && !GhostMethods.heldByAxis(c)
				&& DemonPath.atBottleneck(c) && !DemonPath.heldByAxis(c), "The axis complete: every bar full, its tribulation waiting");
		// One set of axes: the heaven path through to Mid works the Axis of Fortune; the ghost path still at Early rides the Longevity done.
		c.setState(Realm.FOUR_AXIS, Stage.MID, c.cultivationRequired());
		FourAxis.fillBars(c);
		helper.assertTrue(c.getCultivation() == 0 && GhostMethods.atBottleneck(c), "Mid's bar empty (Fortune not begun); the ghost path's Early still full");
		// Out of Four Axis the bars are meditation's again.
		c.setState(Realm.DAO_INTEGRATION, Stage.EARLY, 5);
		GhostMethods.setPath(s, PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.LATE));
		s.setPath(GhostMethods.ghostRank(s), 7);
		FourAxis.fillBars(c);
		helper.assertTrue(c.getCultivation() == 5 && s.pathCultivation() == 7, "Dao Integration's and Heavenly Being's bars untouched");
		helper.assertTrue(FourAxis.volumeReach(Realm.FOUR_AXIS) == Realm.HEAVENLY_BEING && FourAxis.volumeReach(Realm.DAO_INTEGRATION) == Realm.DAO_INTEGRATION,
				"A Four Axis scripture's volumes reach Heavenly Being");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aDemonsBloodIsItsVolumeAndItsGate(GameTestHelper helper) {
		PlayerCultivation d = new PlayerCultivation();
		d.demon().setBloodline(Bloodline.FOX);
		int peak = PlayerCultivation.rank(Realm.QI_REFINING, Stage.GRAND_PERFECTION);
		d.demon().set(peak, DemonPath.required(peak));
		d.demon().setBloodRealm(Realm.QI_REFINING.ordinal());
		helper.assertTrue(DemonPath.canGain(d) && DemonPath.hasVolume(d, Realm.QI_REFINING) && !DemonPath.hasVolume(d, Realm.FOUNDATION_BUILDING),
				"Its blood of Qi Refining: the volume of Qi Refining, none of Foundation Building");
		helper.assertTrue(DemonPath.heldByBlood(d), "...and without Foundation Building's blood no breakthrough at all, unlike the heaven path");
		d.demon().setBloodRealm(Realm.FOUNDATION_BUILDING.ordinal());
		helper.assertTrue(DemonPath.hasVolume(d, Realm.FOUNDATION_BUILDING) && !DemonPath.heldByBlood(d), "Its blood drunk: the volume and the way both");
		int axis = PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.EARLY);
		d.demon().set(axis, 0);
		d.demon().setBloodRealm(Realm.FOUR_AXIS.ordinal());
		helper.assertTrue(!DemonPath.canGain(d), "At Four Axis its axes, not its blood");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anyCultivatorTeachesTheBasics(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ServerPlayer rooted = helper.makeMockServerPlayerInLevel();
		PlayerCultivation c = CultivationManager.get(player), r = CultivationManager.get(rooted);
		c.awaken(0);
		r.awaken(0);
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1));
		npc.becomeRogue(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), 0, helper.getLevel().getRandom());
		try {
			HeavenMethod own = npc.heavenMethod();
			helper.assertTrue(own != null && !c.heaven().knowsAny() && !HeavenMethods.canGain(c), "A beginner with no scripture gathers nothing");
			TalkSystem.teachBasics(player, npc);
			HeavenMethod basics = HeavenMethod.of(own.element(), Realm.QI_REFINING);
			helper.assertTrue(c.heaven().knows(basics) && c.heaven().known().size() == 1 && HeavenMethods.canGain(c),
					"Taught the Qi Refining scripture of the teacher's own element: Qi Circulation's first volume (" + basics.getId() + ")");
			// Roots that can't read the teacher's element are taught the first element they can.
			QiElement other = own.element() == QiElement.WATER ? QiElement.EARTH : QiElement.WATER;
			r.setPhysique(Physique.TRUE_ELEMENT_ROOT); // a single root (PhysiqueGameTests#rootsHoldOtherElementsBack)
			r.setRootElements(List.of(other));
			TalkSystem.teachBasics(rooted, npc);
			helper.assertTrue(r.heaven().knows(HeavenMethod.of(other, Realm.QI_REFINING)) && r.heaven().known().size() == 1,
					"Roots of " + other.name() + " alone: its Qi Refining scripture instead");
			helper.succeed();
		} finally {
			npc.discard();
			leave(player);
			leave(rooted);
		}
	}
}
