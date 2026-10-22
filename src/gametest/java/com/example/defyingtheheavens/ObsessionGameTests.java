package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.HashMap;

/**
 * Name Your Obsession (the user's ask, 2026-10-19; Obsession): asked before the tribulation into Heavenly Being, on either path, and
 * named once; each obsession's boon (lifespan, prices, regard, cultivation) and its axis filling twice as fast.
 */
public class ObsessionGameTests implements FabricGameTest {
	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-6;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theObsessionIsNamedBeforeTheTribulation(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		PlayerCultivation c = CultivationManager.get(player);
		try {
			TestMethods.carryAll(c); // a heaven method opening every realm's way
			c.setState(Realm.NASCENT_SOUL, Stage.GRAND_PERFECTION, 0);
			c.addCultivation(c.cultivationRequired());
			c.prepareBreakthrough(Realm.HEAVENLY_BEING, 0);
			helper.assertTrue(c.canBreakthrough(), "At Nascent Soul Grand Perfection's wall");
			TribulationManager.start(player);
			helper.assertTrue(!TribulationManager.isActive(player.getUUID()) && c.obsession() == null,
					"The Breakthrough button asks for the obsession first: no tribulation yet");
			Obsession.name(player, FourAxis.Axis.FORTUNE, false);
			helper.assertTrue(c.obsession() == FourAxis.Axis.FORTUNE && TribulationManager.isActive(player.getUUID()),
					"Named: kept, and the tribulation begins at once");
			TribulationManager.forget(player.getUUID());
			Obsession.name(player, FourAxis.Axis.MERIT, false);
			helper.assertTrue(c.obsession() == FourAxis.Axis.FORTUNE, "Named once, never again");
			TribulationManager.forget(player.getUUID());

			// One already past it with none named (an older save) is asked at its next tribulation; named, it goes on.
			c.setObsession(null);
			c.setState(Realm.HEAVENLY_BEING, Stage.EARLY, 0);
			c.addCultivation(c.cultivationRequired());
			TribulationManager.start(player);
			helper.assertTrue(!TribulationManager.isActive(player.getUUID()), "Heavenly Being Early with none named: asked");
			c.setObsession(FourAxis.Axis.LONGEVITY);
			TribulationManager.start(player);
			helper.assertTrue(TribulationManager.isActive(player.getUUID()), "...and with one named, on to the tribulation");
			TribulationManager.forget(player.getUUID());

			// Below it nothing is asked; and a stale answer, away from that wall, names nothing.
			c.setObsession(null);
			c.setState(Realm.NASCENT_SOUL, Stage.LATE, 0);
			c.addCultivation(c.cultivationRequired());
			TribulationManager.start(player);
			helper.assertTrue(TribulationManager.isActive(player.getUUID()) && c.obsession() == null, "Nascent Soul Late's tribulation: nothing asked");
			TribulationManager.forget(player.getUUID());
			c.setState(Realm.NASCENT_SOUL, Stage.GRAND_PERFECTION, 0);
			Obsession.name(player, FourAxis.Axis.MERIT, false);
			helper.assertTrue(c.obsession() == null && !TribulationManager.isActive(player.getUUID()), "Not at the wall: nothing named");
		} finally {
			TribulationManager.forget(player.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theGhostPathNamesItToo(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		PlayerCultivation c = CultivationManager.get(player);
		try {
			c.setState(Realm.QI_REFINING, Stage.EARLY, 0);
			NetherworldState s = c.netherworld();
			s.setMethod(GhostMethod.SIX_PATHS_CANON, 1, 0);
			int rank = PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.GRAND_PERFECTION);
			GhostMethods.setPath(s, rank);
			s.setPath(rank, GhostMethods.required(rank));
			helper.assertTrue(GhostMethods.canBreakthrough(c), "The ghost path at Nascent Soul Grand Perfection's wall");
			TribulationManager.startGhost(player);
			helper.assertTrue(!TribulationManager.isActive(player.getUUID()) && c.obsession() == null, "The Netherworld's tribulation asks it first too");
			Obsession.name(player, FourAxis.Axis.ENLIGHTENMENT, true);
			helper.assertTrue(c.obsession() == FourAxis.Axis.ENLIGHTENMENT && TribulationManager.isGhostTrial(player.getUUID()),
					"Named, and the Netherworld's tribulation begins");
		} finally {
			TribulationManager.forget(player.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void eachObsessionHasItsBoon(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		PlayerCultivation c = CultivationManager.get(player);
		c.setState(Realm.HEAVENLY_BEING, Stage.EARLY, 0);
		CultivatorNpc npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1));
		npc.becomeRogue(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID), 0, helper.getLevel().getRandom());
		try {
			double span = Lifespan.span(c), rate = StatPoints.cultivationFactor(c);
			int regard = npc.regard(player), price = price(npc, player);
			c.setObsession(FourAxis.Axis.LONGEVITY);
			helper.assertTrue(near(Lifespan.span(c), span * 1.2), "Longevity: lifespan x1.2 (" + span + " -> " + Lifespan.span(c) + ")");
			c.setObsession(FourAxis.Axis.ENLIGHTENMENT);
			helper.assertTrue(near(StatPoints.cultivationFactor(c), rate * 1.2) && near(Lifespan.span(c), span), "Enlightenment: cultivation x1.2");
			c.setObsession(FourAxis.Axis.MERIT);
			helper.assertTrue(npc.regard(player) == Regard.clamp(regard + Obsession.REGARD), "Merit: +10 regard (" + regard + " -> " + npc.regard(player) + ")");
			c.setObsession(FourAxis.Axis.FORTUNE);
			int less = price(npc, player);
			helper.assertTrue(less < price && npc.regard(player) == regard, "Fortune: its prices lower, x0.8 (" + price + " -> " + less + " Low stones)");

			// The axis of one's obsession fills twice as fast (it needs half); any other as ever.
			c.setState(Realm.FOUR_AXIS, Stage.EARLY, 0);
			int full = FourAxis.Axis.LONGEVITY.baseNeed();
			helper.assertTrue(FourAxis.need(c, FourAxis.Axis.LONGEVITY) == full && FourAxis.need(c, FourAxis.Axis.FORTUNE) == FourAxis.Axis.FORTUNE.baseNeed() / 2,
					"Fortune's: Longevity as ever, Fortune half");
			helper.assertTrue(FourAxis.add(player, FourAxis.Axis.LONGEVITY, full / 2) == full / 2 && !FourAxis.complete(c, FourAxis.Axis.LONGEVITY),
					"Half Longevity's years: not yet complete");
			c.setAxisProgress(FourAxis.Axis.LONGEVITY, 0);
			c.setObsession(FourAxis.Axis.LONGEVITY);
			helper.assertTrue(FourAxis.add(player, FourAxis.Axis.LONGEVITY, full) == full / 2 && FourAxis.complete(c, FourAxis.Axis.LONGEVITY),
					"Longevity's: half the years complete it, and no more is taken in");
			helper.assertTrue(PlayerCultivation.load(c.save()).obsession() == FourAxis.Axis.LONGEVITY && PlayerCultivation.load(new PlayerCultivation().save()).obsession() == null,
					"Saved (and none for a record that named none)");
		} finally {
			npc.discard();
		}
		helper.succeed();
	}

	/** What {@code npc} asks for all it sells to {@code player} today, in Low stones. */
	private static int price(CultivatorNpc npc, ServerPlayer player) {
		int total = 0;
		for (MerchantOffer offer : CultivatorTrades.offersFor(npc, player, new HashMap<>(), new HashMap<>())) {
			if (offer.getResult().getItem() instanceof SpiritStoneItem) continue; // what it would buy (paid in stones): not a price of its
			total += SpiritStoneItem.valueOf(offer.getBaseCostA()) + SpiritStoneItem.valueOf(offer.getCostB());
		}
		return total;
	}
}
