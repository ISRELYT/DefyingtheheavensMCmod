package com.example.defyingtheheavens;

import com.example.defyingtheheavens.mixin.ServerPlayerAccessor;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

public class TribulationCloudGameTests implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE)
    public void breakthroughsSpawnScaledCloudsAndCleanUp(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            PlayerCultivation cultivation = CultivationManager.get(player);
            cultivation.setState(Realm.QI_REFINING, Stage.GRAND_PERFECTION, 0);
            cultivation.addCultivation(cultivation.cultivationRequired());
            cultivation.prepareBreakthrough(Realm.FOUNDATION_BUILDING, 0); // Foundation Building takes a Foundation Pill
            TribulationManager.start(player);

            TribulationCloud foundation = cloudFor(helper, player);
            helper.assertTrue(TribulationManager.isActive(player.getUUID()), "Breakthrough starts a trial");
            helper.assertTrue(foundation.stormTier() == 0, "Foundation Building starts the smallest storm");
            helper.assertTrue(foundation.getY() == player.getY() + 32,
                    "Foundation Building clouds start 32 blocks above the player");
            double originalY = player.getY();
            player.setPos(player.getX(), originalY + 80, player.getZ());
            TribulationManager.tick(helper.getLevel().getServer());
            helper.assertTrue(foundation.getY() == player.getY() + 32,
                    "Climbing keeps the same overhead gap without vertical lag");
            player.setPos(player.getX(), originalY - 20, player.getZ());
            TribulationManager.tick(helper.getLevel().getServer());
            helper.assertTrue(foundation.getY() == player.getY() + 32,
                    "Descending keeps the cloud at the same relative height");
            player.setPos(player.getX(), originalY, player.getZ());

            // Advance the trial to its first real strike and verify its synced render length.
            for (int tick = 0; tick < 60; tick++) TribulationManager.tick(helper.getLevel().getServer());
            var bolts = helper.getLevel().getEntitiesOfClass(TribulationLightning.class,
                    new AABB(player.blockPosition()).inflate(2));
            helper.assertTrue(bolts.size() == 1, "Trial creates its targeted lightning bolt");
            TribulationLightning bolt = bolts.get(0);
            helper.assertTrue(bolt.getY() + bolt.renderHeight() == foundation.getY(),
                    "Targeted lightning reaches the storm's current altitude");
            bolt.discard();
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
            helper.assertTrue(fourAxis.getY() == player.getY() + 56,
                    "Four Axis clouds remain a moderate 56 blocks above the player");
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

    @GameTest(template = EMPTY_STRUCTURE)
    public void leavingMidTribulationFailsItOnReturn(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            PlayerCultivation cultivation = CultivationManager.get(player);
            readyForFoundation(cultivation);
            TribulationManager.start(player);
            helper.assertTrue(cultivation.isInTribulation(), "A running tribulation marks the cultivator");
            TribulationManager.forget(player.getUUID()); // logging out, quitting or the game closing
            helper.assertTrue(cultivation.isInTribulation() && PlayerCultivation.load(cultivation.save()).isInTribulation(),
                    "Leaving keeps the mark, and it is saved");
            helper.assertTrue(TribulationManager.onJoin(player), "Returning still marked fails the tribulation");
            helper.assertTrue(cultivation.getRealm() == Realm.QI_REFINING && cultivation.getStage() == Stage.LATE
                    && cultivation.getCultivation() == 0 && cultivation.getPreparedRealm() == null && !cultivation.isInTribulation(),
                    "Fleeing costs a stage, the cultivation and the pill, like being struck down");
            helper.assertTrue(!TribulationManager.onJoin(player), "It is paid only once");

            readyForFoundation(cultivation);
            TribulationManager.start(player);
            TribulationManager.abandon(player); // falling out of the Upper Realm
            helper.assertTrue(!cultivation.isInTribulation() && !TribulationManager.onJoin(player),
                    "A tribulation that ended (here abandoned) leaves no mark behind");
            helper.succeed();
        } finally {
            TribulationManager.forget(player.getUUID());
            helper.getLevel().getServer().getPlayerList().remove(player);
            player.discard();
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void aHitJustBeforeAStrikeDoesNotBluntIt(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            player.setGameMode(GameType.SURVIVAL);
            ((ServerPlayerAccessor) player).dth$setSpawnInvulnerableTime(0);
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
            player.setHealth(100);
            PlayerCultivation cultivation = CultivationManager.get(player);
            cultivation.setState(Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION, 0);
            cultivation.addCultivation(cultivation.cultivationRequired());
            cultivation.prepareBreakthrough(Realm.CORE_FORMATION, 0);
            TribulationManager.start(player);
            for (int tick = 0; tick < 59; tick++) TribulationManager.tick(helper.getLevel().getServer());
            helper.assertTrue(player.getHealth() == 100, "No strike has landed yet");
            player.hurt(helper.getLevel().damageSources().generic(), 10); // a blow taken just before the strike
            TribulationManager.tick(helper.getLevel().getServer()); // the first Core Formation strike: 5 magic + 13 lightning
            helper.assertTrue(Math.abs(player.getHealth() - 72) < 0.01f, "The whole strike lands on top of the blow: " + player.getHealth());
            helper.succeed();
        } finally {
            TribulationManager.forget(player.getUUID());
            helper.getLevel().getServer().getPlayerList().remove(player);
            player.discard();
        }
    }

    /** Qi Refining Grand Perfection, bar full, Foundation Pill taken: ready to break into Foundation Building. */
    private static void readyForFoundation(PlayerCultivation cultivation) {
        cultivation.setState(Realm.QI_REFINING, Stage.GRAND_PERFECTION, 0);
        cultivation.addCultivation(cultivation.cultivationRequired());
        cultivation.prepareBreakthrough(Realm.FOUNDATION_BUILDING, 0);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void distantLightningKeepsItsStormHeightWhenSynced(GameTestHelper helper) {
        TribulationLightning serverBolt = ModEntities.TRIBULATION_LIGHTNING.create(helper.getLevel());
        TribulationLightning observerBolt = ModEntities.TRIBULATION_LIGHTNING.create(helper.getLevel());
        serverBolt.setPos(0, 60, 0);
        serverBolt.setCloudBaseY(144); // Strike lower terrain beneath a higher player's storm.
        observerBolt.getEntityData().assignValues(serverBolt.getEntityData().getNonDefaultValues());
        helper.assertTrue(observerBolt.renderHeight() == 84,
                "Observers receive the actual cloud-to-ground distance, not their own altitude or realm");
        helper.succeed();
    }

    private TribulationCloud cloudFor(GameTestHelper helper, ServerPlayer player) {
        var clouds = helper.getLevel().getEntitiesOfClass(TribulationCloud.class,
                new AABB(player.blockPosition()).inflate(300));
        helper.assertTrue(clouds.size() == 1, "Exactly one server-tracked cloud exists for the trial");
        return clouds.get(0);
    }
}
