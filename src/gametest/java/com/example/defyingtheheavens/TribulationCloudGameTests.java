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
        ServerPlayer player = isolated(helper);
        try {
            PlayerCultivation cultivation = CultivationManager.get(player);
            TestMethods.carryAll(cultivation); // a heaven method opening the way into new realms (HeavenMethods#opensWay)
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
    public void theRiftIsAWayOutOfATribulationToo(GameTestHelper helper) {
        ServerPlayer player = isolated(helper);
        PlayerCultivation cultivation = CultivationManager.get(player);
        try {
            readyForFoundation(cultivation);
            TribulationManager.start(player);
            TribulationCloud cloud = cloudFor(helper, player);
            SpatialTrialHandler.beginAscension(player); // walking into the rift at 0, 0
            helper.assertTrue(!TribulationManager.isActive(player.getUUID()) && !cultivation.isInTribulation() && cloud.isRemoved(),
                    "Ascending abandons the tribulation, cloud and all");
            helper.assertTrue(cultivation.getRealm() == Realm.QI_REFINING && cultivation.getStage() == Stage.GRAND_PERFECTION
                    && !TribulationManager.onJoin(player), "... without the fall a failed one costs");
            if (helper.getLevel().getServer().getLevel(ModDimensions.SPATIAL_GAP) != null) { // the test server has only vanilla's
                helper.assertTrue(SpatialTrialHandler.isInTrial(player.getUUID()) && player.level().dimension() == ModDimensions.SPATIAL_GAP,
                        "... and the ascension begins");
            }
            helper.succeed();
        } finally {
            SpatialTrialHandler.forget(player.getUUID());
            cultivation.setPendingTrial(SpatialTrialHandler.Route.NONE);
            TribulationManager.forget(player.getUUID());
            helper.getLevel().getServer().getPlayerList().remove(player);
            player.discard();
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void aHitJustBeforeAStrikeDoesNotBluntIt(GameTestHelper helper) {
        ServerPlayer player = isolated(helper);
        try {
            player.setGameMode(GameType.SURVIVAL);
            ((ServerPlayerAccessor) player).dth$setSpawnInvulnerableTime(0);
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
            player.setHealth(100);
            PlayerCultivation cultivation = CultivationManager.get(player);
            TestMethods.carryAll(cultivation);
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

    /**
     * The ghost path at Foundation Building Grand Perfection, its bar full, the Ghost Core Canon known (its way into Core Formation
     * open), the ghost path's qi drawn in: ready for the Netherworld's tribulation into Core Formation.
     */
    private static void readyForGhostCore(PlayerCultivation cultivation) {
        cultivation.setState(Realm.QI_REFINING, Stage.EARLY, 0);
        NetherworldState s = cultivation.netherworld();
        for (GhostMethod method : new GhostMethod[]{GhostMethod.NETHERWORLD_BREATHING, GhostMethod.YELLOW_SPRINGS_MARROW, GhostMethod.GHOST_CORE_CANON}) {
            s.setMethod(method, 1, 0);
        }
        int rank = PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION);
        GhostMethods.setPath(s, rank);
        s.setPath(rank, GhostMethods.required(rank));
        s.setCultivatingGhost(true);
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
    public void theGhostPathsTribulationIsTheNetherworlds(GameTestHelper helper) {
        ServerPlayer player = isolated(helper);
        player.setGameMode(GameType.SURVIVAL);
        ((ServerPlayerAccessor) player).dth$setSpawnInvulnerableTime(0);
        PlayerCultivation c = CultivationManager.get(player);
        NetherworldState s = c.netherworld();
        try {
            readyForGhostCore(c);
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
            player.setHealth(player.getMaxHealth());
            float full = player.getHealth();
            helper.assertTrue(TwoPaths.cultivatesGhost(c) && GhostMethods.canBreakthrough(c) && !c.canBreakthrough(), "The ghost path at its bottleneck, its qi drawn in");
            CultivationManager.tryBreakthrough(player);
            helper.assertTrue(TribulationManager.isGhostTrial(player.getUUID()) && s.inGhostTribulation() && cloudFor(helper, player).isGhost(),
                    "The Breakthrough button brings the Netherworld's tribulation: its own storm, the ghost path marked");
            for (int tick = 0; tick < 60; tick++) TribulationManager.tick(helper.getLevel().getServer());
            var bolts = helper.getLevel().getEntitiesOfClass(TribulationLightning.class, new AABB(player.blockPosition()).inflate(2));
            helper.assertTrue(bolts.size() == 1 && bolts.get(0).isGhost()
                    && Math.abs(bolts.get(0).renderScale() - TribulationManager.boltScale(Realm.CORE_FORMATION, Stage.EARLY)) < 1.0e-5,
                    "Its one bolt, yin lightning, as big as the realm's");
            helper.assertTrue(Math.abs(full - player.getHealth() - 18) < 0.01f, "The Core Formation strike in yin: 5 through armour and 13 more (" + (full - player.getHealth()) + ")");
            bolts.forEach(net.minecraft.world.entity.Entity::discard);
            for (int tick = 0; tick < 60 * 2 + 25 && TribulationManager.isActive(player.getUUID()); tick++) TribulationManager.tick(helper.getLevel().getServer());
            helper.assertTrue(!TribulationManager.isActive(player.getUUID()) && !s.inGhostTribulation()
                    && GhostMethods.ghostRank(s) == PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY) && c.getRealm() == Realm.QI_REFINING,
                    "Survived: the ghost path into Core Formation, the heaven path where it was");
            helper.succeed();
        } finally {
            TribulationManager.forget(player.getUUID());
            helper.getLevel().getServer().getPlayerList().remove(player);
            player.discard();
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void theDeadFaceTheNetherworldsTribulationAsTheLivingDoAndScatterIfItFails(GameTestHelper helper) {
        ServerPlayer player = isolated(helper);
        player.setGameMode(GameType.SURVIVAL);
        ((ServerPlayerAccessor) player).dth$setSpawnInvulnerableTime(0);
        PlayerCultivation c = CultivationManager.get(player);
        NetherworldState s = c.netherworld();
        try {
            readyForGhostCore(c);
            s.setCultivatingGhost(false); // the dead draw in ghostly qi whatever the switch says
            GhostSystem.makeGhost(player);
            helper.assertTrue(TwoPaths.cultivatesGhost(c), "The dead cultivate the ghost path alone");
            player.setHealth(player.getMaxHealth());
            float full = player.getHealth();
            CultivationManager.tryBreakthrough(player);
            helper.assertTrue(TribulationManager.isGhostTrial(player.getUUID()), "The dead break through on the ghost path");
            for (int tick = 0; tick < 60; tick++) TribulationManager.tick(helper.getLevel().getServer());
            // As much of its hearts as of a living body's (GhostSystem#livingShare), the bolt turned first as a common ghost's form
            // turns it, as an iron set would (TribulationManager#GHOST_FORM_ARMOR), the yin passing.
            float share = (float) GhostSystem.livingShare(player);
            // (The helper's mock player counts as creative, outside the dead's rules of touch: its own armour, none.)
            float bolt = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(13,
                    Incorporeal.isRuledGhost(player) ? Math.max(player.getArmorValue(), TribulationManager.GHOST_FORM_ARMOR) : player.getArmorValue(),
                    (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS));
            // (Its body is its ghost path's whole now, dead, as a life's heaven path is: TwoPaths#pathShare. The same share, x1.)
            helper.assertTrue(Math.abs(share - 1) < 1.0e-6 && Math.abs(full - player.getHealth() - (5 + bolt) * share) < 0.01f,
                    "The yin reaches the dead, as great a share of its hearts as of a life's (" + (full - player.getHealth()) + " of "
                            + player.getMaxHealth() + ", x" + share + ")");
            player.setHealth(2);
            for (int tick = 0; tick < 60 && TribulationManager.isActive(player.getUUID()); tick++) TribulationManager.tick(helper.getLevel().getServer());
            helper.assertTrue(!TribulationManager.isActive(player.getUUID()) && !s.inGhostTribulation()
                    && GhostMethods.ghostRank(s) == PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.LATE) && s.pathCultivation() == 0,
                    "Struck down, it scatters: failed, the ghost path a stage down, its cultivation lost (" + GhostMethods.rankName(GhostMethods.ghostRank(s)).getString() + ")");
            helper.succeed();
        } finally {
            TribulationManager.forget(player.getUUID());
            GhostSystem.forget(player.getUUID());
            helper.getLevel().getServer().getPlayerList().remove(player);
            player.discard();
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void leavingTheNetherworldsTribulationFailsItOnReturn(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            PlayerCultivation c = CultivationManager.get(player);
            NetherworldState s = c.netherworld();
            readyForGhostCore(c);
            TribulationManager.startGhost(player);
            helper.assertTrue(s.inGhostTribulation() && !c.isInTribulation(), "The ghost path's own mark, not the heaven path's");
            TribulationManager.forget(player.getUUID());
            helper.assertTrue(PlayerCultivation.load(c.save()).netherworld().inGhostTribulation(), "...saved");
            helper.assertTrue(TribulationManager.onJoin(player) && !s.inGhostTribulation()
                    && GhostMethods.ghostRank(s) == PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.LATE)
                    && c.getRealm() == Realm.QI_REFINING && c.getStage() == Stage.EARLY,
                    "Fled: on return the ghost path falls a stage, the heaven path untouched");
            helper.assertTrue(!TribulationManager.onJoin(player), "It is paid only once");
            helper.succeed();
        } finally {
            TribulationManager.forget(player.getUUID());
            helper.getLevel().getServer().getPlayerList().remove(player);
            player.discard();
        }
    }

    /** Qi Refining Grand Perfection, bar full, Foundation Pill taken, a heaven method opening the way: ready to break into Foundation Building. */
    private static void readyForFoundation(PlayerCultivation cultivation) {
        TestMethods.carryAll(cultivation);
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
        serverBolt.setScale(TribulationManager.boltScale(Realm.NASCENT_SOUL, Stage.MID));
        observerBolt.getEntityData().assignValues(serverBolt.getEntityData().getNonDefaultValues());
        helper.assertTrue(Math.abs(observerBolt.renderScale() - 2.0125f) < 1.0e-4, "...and its size (" + observerBolt.renderScale() + ")");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void theBoltGrowsWithTheTribulationAsItsCloudDoes(GameTestHelper helper) {
        helper.assertTrue(TribulationManager.boltScale(Realm.FOUNDATION_BUILDING, Stage.EARLY) == 1.0f
                && Math.abs(TribulationManager.boltScale(Realm.CORE_FORMATION, Stage.EARLY) - 1.45f) < 1.0e-5,
                "Foundation Building's bolt as vanilla's, Core Formation's as much thicker as its cloud is wider");
        float last = 0;
        boolean growing = true;
        for (Realm realm : new Realm[]{Realm.NASCENT_SOUL, Realm.HEAVENLY_BEING, Realm.FOUR_AXIS}) {
            for (Stage stage : Stage.values()) {
                float scale = TribulationManager.boltScale(realm, stage);
                growing &= scale > last;
                last = scale;
            }
        }
        helper.assertTrue(growing && Math.abs(last - 3.1375f) < 1.0e-4, "Every stage's bolt thicker than the last, Four Axis Grand Perfection's x3.14 (" + last + ")");
        helper.succeed();
    }

    /**
     * A mock player moved into this test's own area. Mock players all join at the world spawn, so tests running side by side
     * would otherwise share one spot, and each other's strikes and clouds.
     */
    private static ServerPlayer isolated(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        net.minecraft.world.phys.Vec3 at = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1)));
        player.teleportTo(helper.getLevel(), at.x, at.y, at.z, 0, 0);
        return player;
    }

    /** The trial's cloud: the one over the player (it follows them, drifting a few blocks about), not another test's. */
    private TribulationCloud cloudFor(GameTestHelper helper, ServerPlayer player) {
        var clouds = helper.getLevel().getEntitiesOfClass(TribulationCloud.class,
                new AABB(player.blockPosition()).inflate(8, 120, 8));
        helper.assertTrue(clouds.size() == 1, "Exactly one server-tracked cloud exists for the trial");
        return clouds.get(0);
    }
}
