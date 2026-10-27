package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * The user's reports of 2026-10-27. A spirit stone held through a real meditation is drawn on (a second of it each second, its qi's bar
 * shown under it) and spent; and a soul in the Inner Realm is still fed by its body's surroundings (a red mat under the body counts
 * there as it did; the Cultivation tab is told them: MeditationManager#sendBodyPlace). Each test with a player in a batch of its own.
 */
public class InnerAndStonesGameTests implements FabricGameTest {
	/** A survival player in earnest, of {@code realm} Early, standing at {@code at} (relative). */
	private static ServerPlayer player(GameTestHelper helper, Realm realm, String name, BlockPos at) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer p = new ServerPlayer(server, helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isCreative() { return false; }
		};
		server.getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos abs = helper.absolutePos(at);
		p.teleportTo(helper.getLevel(), abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
		p.setOnGround(true);
		CultivationManager.get(p).setState(realm, Stage.EARLY, 0);
		CultivationManager.refresh(p);
		return p;
	}

	private static void leave(ServerPlayer p) {
		MeditationManager.stop(p, false);
		InnerRealm.leave(p);
		MeditationManager.forget(p.getUUID());
		Lifespan.forget(p.getUUID());
		LifespanTribulation.forget(p.getUUID());
		QiFlight.forget(p.getUUID());
		if (p.server.getPlayerList().getPlayer(p.getUUID()) == p) p.server.getPlayerList().remove(p);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "inner_stones_stone", timeoutTicks = 200)
	public void aStoneHeldInMeditationIsDrawnOn(GameTestHelper helper) {
		ServerPlayer p = player(helper, Realm.QI_REFINING, "test-stone-a", new BlockPos(2, 2, 2));
		ItemStack stones = new ItemStack(ModItems.LOW_SPIRIT_STONE, 2);
		p.setItemInHand(InteractionHand.MAIN_HAND, stones);
		MeditationManager.start(p);
		helper.assertTrue(MeditationManager.isMeditating(p.getUUID()), "Meditating with a stone in hand");
		helper.runAfterDelay(70, () -> {
			try {
				ItemStack held = p.getMainHandItem();
				double left = SpiritStoneItem.qiLeft(held);
				helper.assertTrue(MeditationManager.isMeditating(p.getUUID()) && held.is(ModItems.LOW_SPIRIT_STONE) && left < 0.95 && left > 0.8,
						"Three seconds in, a tenth of the top stone's qi drawn (" + left + " left)");
				helper.assertTrue(held.getItem().isBarVisible(held) && held.getItem().getBarWidth(held) < 13, "...its qi's bar shown under it");
				// The rest of its thirty seconds drawn (as the meditation's own second does), and it is spent: one of the two left.
				for (int s = 0; s < SpiritStoneItem.secondsPerStone(SpiritStoneItem.Grade.LOW); s++) {
					if (SpiritStoneItem.qiLeft(p.getMainHandItem()) >= 1.0 && p.getMainHandItem().getCount() == 1) break;
					SpiritStoneItem.drainWhileMeditating(p);
				}
				helper.assertTrue(p.getMainHandItem().getCount() == 1 && SpiritStoneItem.qiLeft(p.getMainHandItem()) == 1.0,
						"Its qi spent, the stone is used up and the next begins whole (" + p.getMainHandItem() + ")");
				helper.succeed();
			} finally {
				leave(p);
			}
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "inner_stones_inner", timeoutTicks = 200)
	public void theBodysSurroundingsFeedTheSoulInTheInnerRealm(GameTestHelper helper) {
		BlockPos seat = new BlockPos(2, 2, 2);
		helper.setBlock(seat, ModBlocks.RED_MEDITATION_MAT);
		ServerPlayer p = player(helper, Realm.QI_REFINING, "test-inner-a", seat);
		try {
			CultivationManager.get(p).setAbilityEnabled(Ability.INNER_REALM, true);
			CultivationBoost.Breakdown outside = CultivationBoost.of(p);
			helper.assertTrue(outside.mat() == ModBlocks.RED_MEDITATION_MAT, "Sitting on the red mat counts (" + outside.mat() + ")");
			helper.assertTrue(InnerRealm.enter(p) && InnerRealm.isInside(p), "The soul turned inward");
			CultivationBoost.Breakdown inside = CultivationBoost.of(p);
			helper.assertTrue(inside.mat() == ModBlocks.RED_MEDITATION_MAT && Math.abs(inside.multiplier() - outside.multiplier()) < 1.0e-9,
					"In the Inner Realm, the red mat under the body still counts (" + inside.multiplier() + " against " + outside.multiplier() + ")");
			helper.assertTrue(InnerRealm.CULTIVATION_BONUS == 1.25, "...and the Inner Realm's own x1.25 on top");
			helper.succeed();
		} finally {
			leave(p);
		}
	}
}
