package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;

/**
 * The user's ask of 2026-10-27: no Vestige of the Void mightier than Sacred Vessel's Grand Perfection (a Void Breach's came at its
 * climber's own might, True Immortal's for one past it). Below the cap, a breach's keeps its climber's might; one saved mightier
 * comes back capped.
 */
public class VestigeCapGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "vestige_cap")
	public void noVestigeMightierThanSacredVessel(GameTestHelper helper) {
		VoidVestige vestige = ModEntities.VOID_VESTIGE.create(helper.getLevel());
		try {
			helper.assertTrue(vestige != null, "A Vestige made");
			int cap = PlayerCultivation.rank(Realm.SACRED_VESSEL, Stage.GRAND_PERFECTION);
			helper.assertTrue(VoidVestige.MAX_MIGHT == cap, "Its cap: Sacred Vessel Grand Perfection");
			vestige.setRank(VoidBreach.VESTIGE_SIZE);
			vestige.setMight(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY));
			helper.assertTrue(vestige.mightRank() == cap, "For a True Immortal climber: Sacred Vessel Grand Perfection's might (" + vestige.mightRank() + ")");
			vestige.setMight(PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION));
			helper.assertTrue(vestige.mightRank() == cap, "For an Entering Nirvana climber too");
			int vs = PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.MID);
			vestige.setMight(vs);
			helper.assertTrue(vestige.mightRank() == vs, "Below the cap, its climber's own might");
			// One saved mightier (before the cap) comes back capped.
			CompoundTag tag = new CompoundTag();
			vestige.addAdditionalSaveData(tag);
			tag.putInt("Might", PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION));
			VoidVestige loaded = ModEntities.VOID_VESTIGE.create(helper.getLevel());
			loaded.readAdditionalSaveData(tag);
			helper.assertTrue(loaded.mightRank() == cap, "One saved mightier comes back capped (" + loaded.mightRank() + ")");
			loaded.discard();
			// One left from a mightier climb, upon a climber fallen back to Void Shattering: of their might now, its wounds kept as a share.
			net.minecraft.server.level.ServerPlayer climber = helper.makeMockServerPlayerInLevel();
			try {
				CultivationManager.get(climber).setState(Realm.VOID_SHATTERING, Stage.EARLY, 0);
				vestige.setMight(cap);
				vestige.setHealth(vestige.getMaxHealth() / 2);
				vestige.matchMight(climber);
				helper.assertTrue(vestige.mightRank() == PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.EARLY)
						&& Math.abs(vestige.getHealth() / vestige.getMaxHealth() - 0.5f) < 0.01f,
						"It matches the climber it hunts as they stand now, its wounds kept (" + vestige.mightRank() + ", " + vestige.getHealth() / vestige.getMaxHealth() + ")");
			} finally {
				Lifespan.forget(climber.getUUID());
				if (climber.server.getPlayerList().getPlayer(climber.getUUID()) == climber) climber.server.getPlayerList().remove(climber);
				else climber.discard();
			}
			helper.succeed();
		} finally {
			if (vestige != null) vestige.discard();
		}
	}
}
