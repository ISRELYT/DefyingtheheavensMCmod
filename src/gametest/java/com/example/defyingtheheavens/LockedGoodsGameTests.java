package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.HashMap;
import java.util.IdentityHashMap;

/**
 * The user's ask of 2026-10-19: everyone trades, however far above you (a Ghost King too), and what their regard hasn't opened
 * yet is shown crossed out with the regard it wants (CultivatorTrades#locked), so one can see early what they have.
 */
public class LockedGoodsGameTests implements FabricGameTest {
	/** The offer selling a Ghost Jade Slip of {@code method}, or null. */
	private static MerchantOffer slipOffer(Iterable<MerchantOffer> offers, GhostMethod method) {
		for (MerchantOffer offer : offers) {
			if (offer.getResult().getItem() instanceof GhostJadeSlipItem && GhostJadeSlipItem.method(offer.getResult()) == method) return offer;
		}
		return null;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aGhostKingFarAboveTradesAndShowsWhatItWontYetSell(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		PlayerCultivation c = CultivationManager.get(player);
		c.awaken(0); // Qi Refining Early: a Core Formation king is two realms above
		GhostCultivatorNpc king = ModEntities.GHOST_CULTIVATOR.create(helper.getLevel());
		try {
			king.becomeKing(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), 0, helper.absolutePos(new BlockPos(1, 2, 1)), helper.getLevel().getRandom());
			TalkSystem.Standing standing = TalkSystem.standing(player, king);
			helper.assertTrue(standing == TalkSystem.Standing.FAR_ABOVE && (TalkSystem.intents(player, king, standing) & TalkIntent.TRADE.bit()) != 0,
					"Two realms above you, a Ghost King still trades");
			MerchantOffer slip = slipOffer(CultivatorTrades.offersFor(king, player, new HashMap<>(), new IdentityHashMap<>()), GhostMethod.GHOST_CORE_CANON);
			helper.assertTrue(slip != null && slip.isOutOfStock() && CultivatorTrades.isLocked(slip),
					"Its canon's slip is on show, crossed out, for one it isn't yet Warm to");
			king.changeRegard(player, Regard.WARM);
			slip = slipOffer(CultivatorTrades.offersFor(king, player, new HashMap<>(), new IdentityHashMap<>()), GhostMethod.GHOST_CORE_CANON);
			helper.assertTrue(slip != null && !slip.isOutOfStock() && !CultivatorTrades.isLocked(slip), "Warm: on sale");
		} finally {
			king.discard();
			player.server.getPlayerList().remove(player);
		}
		helper.succeed();
	}
}
