package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

public class TribulationCloudGameTests implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE)
    public void breakthroughsSpawnScaledCloudsAndCleanUp(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            PlayerCultivation cultivation = CultivationManager.get(player);
            cultivation.setState(Realm.QI_REFINING, Stage.GRAND_PERFECTION, 0);
            cultivation.addCultivation(cultivation.cultivationRequired());
            TribulationManager.start(player);

            TribulationCloud foundation = cloudFor(helper, player);
            helper.assertTrue(TribulationManager.isActive(player.getUUID()), "Breakthrough starts a trial");
            helper.assertTrue(foundation.stormTier() == 0, "Foundation Building starts the smallest storm");
            helper.assertTrue(Math.abs(foundation.getY() - 128) <= 1.5,
                    "Overworld storm stays near Y=128, including its gentle vertical drift");
            TribulationManager.forget(player.getUUID());
            helper.assertTrue(foundation.isRemoved(), "Disconnect cleanup removes the tracked cloud");

            cultivation.setState(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION, 0);
            cultivation.setLowerRealmBound(false);
            cultivation.setInUpperRealm(true);
            cultivation.addCultivation(cultivation.cultivationRequired());
            TribulationManager.start(player);

            TribulationCloud fourAxis = cloudFor(helper, player);
            helper.assertTrue(fourAxis.stormTier() == 4 && fourAxis.stormScale() > foundation.stormScale(),
                    "Four Axis breakthrough creates the larger realm-scaled storm");
            TribulationManager.abandon(player);
            helper.assertTrue(fourAxis.isRemoved() && !TribulationManager.isActive(player.getUUID()),
                    "Abandoning the trial clears the cloud and trial state");
            helper.succeed();
        } finally {
            TribulationManager.forget(player.getUUID());
            helper.getLevel().getServer().getPlayerList().remove(player);
            player.discard();
        }
    }

    private TribulationCloud cloudFor(GameTestHelper helper, ServerPlayer player) {
        var clouds = helper.getLevel().getEntitiesOfClass(TribulationCloud.class,
                new AABB(player.blockPosition()).inflate(300));
        helper.assertTrue(clouds.size() == 1, "Exactly one server-tracked cloud exists for the trial");
        return clouds.get(0);
    }
}
