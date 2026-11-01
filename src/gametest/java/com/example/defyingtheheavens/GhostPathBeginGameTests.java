package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * The dead and the ghost path (the user's asks of 2026-10-19, and the methods overhaul of 2026-10-31): the dead are taught Netherworld
 * Breathing as they die, which begins the path, and keep it if born again (GhostMethods#teachTheDead; before, they began it with no
 * method at all); and a badly hurt cultivator runs from its pursuer for as long as the pursuer is near, not only for the five seconds
 * vanilla remembers who struck it (CultivatorNpc#getPursuer).
 */
public class GhostPathBeginGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void theDeadAreTaughtBreathingAndKeepThePath(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.getAbilities().invulnerable = true; // out of the other tests' fights (a blow ends a meditation)
		PlayerCultivation c = CultivationManager.get(player);
		NetherworldState s = c.netherworld();
		c.awaken(0); // meridians open: a mortal's ghost still has no ghost path (TwoPathsGameTests)
		GhostSystem.makeGhost(player);
		helper.assertTrue(GhostSystem.isGhost(player) && s.knows(GhostMethod.NETHERWORLD_BREATHING) && GhostMethods.ghostRank(s) == 0,
				"One of the dead, taught Netherworld Breathing as it died: its ghost path begun at Qi Refining Early");
		helper.assertTrue(GhostMethods.canGain(c) && !GhostMethods.hasVolume(c, Realm.FOUNDATION_BUILDING), "...its first volume Qi Refining's alone");
		s.setDensity(GhostQi.NETHERWORLD); // as among the dead
		player.setOnGround(true);
		MeditationManager.start(player);
		helper.assertTrue(MeditationManager.isMeditating(player.getUUID()), "It sits down to meditate");
		helper.runAfterDelay(40, () -> {
			try {
				helper.assertTrue(GhostMethods.ghostRank(s) == 0 && s.pathCultivation() > 0, "Its ghost path filling (" + s.pathCultivation() + ")");
				// Born again (the Wheel copies its cultivation across): the path is still its own, to go on with in life.
				MeditationManager.forget(player.getUUID());
				s.setGhost(false);
				helper.assertTrue(TwoPaths.ghostRank(c, false) == 0 && TwoPaths.walksBoth(c) && GhostQi.poolMax(c) > 0, "Alive again: on both paths, with a pool");
				TwoPaths.switchPath(player);
				helper.assertTrue(TwoPaths.cultivatesGhost(c), "...and may draw in ghostly qi to go on with it");
				// Foundation Building: free to break into, but it gathers nothing there until a foundation of it is found.
				GhostMethods.setPath(s, PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY));
				helper.assertTrue(!GhostMethods.heldByMethod(c) && GhostMethods.opensWay(c, Realm.FOUNDATION_BUILDING) && !GhostMethods.canGain(c),
						"In Foundation Building with only the Breathing Art: no gain");
				helper.succeed();
			} finally {
				MeditationManager.forget(player.getUUID());
				GhostSystem.forget(player.getUUID());
				Spells.forget(player.getUUID());
				TwoPaths.forget(player.getUUID());
				player.server.getPlayerList().remove(player);
			}
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theLivingStillNeedAFoundationToBeginIt(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		PlayerCultivation c = CultivationManager.get(player);
		NetherworldState s = c.netherworld();
		c.awaken(0);
		try {
			TwoPaths.switchPath(player);
			helper.assertTrue(GhostMethods.ghostRank(s) < 0 && !TwoPaths.cultivatesGhost(c), "Alive and never dead: no ghost path to draw in");
			GhostMethods.teachTheDead(player);
			helper.assertTrue(!s.knowsAnyMethod(), "The living aren't taught the dead's breath");
			GhostMethods.give(player, GhostMethod.NETHERWORLD_BREATHING);
			helper.assertTrue(GhostMethods.ghostRank(s) == 0, "A foundation learned begins it, as ever");
		} finally {
			Spells.forget(player.getUUID());
			TwoPaths.forget(player.getUUID());
			player.server.getPlayerList().remove(player);
		}
		helper.succeed();
	}

	/**
	 * Struck down to a quarter of its health by a husk outside its glass cell (it can't get away, nor the husk at it), a rogue
	 * still runs from it long after vanilla has forgotten the blow and the fight has gone quiet; the husk gone far, it stops.
	 * Its own batch: another test's Hungry Ghost striking it would be its pursuer instead.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 420, batch = "npc_flee_pursuer")
	public void aBadlyHurtCultivatorRunsWhileItsPursuerIsNear(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = 0; x <= 2; x++) for (int y = 1; y <= 4; y++) for (int z = 0; z <= 2; z++) {
			boolean shell = x != 1 || z != 1 || y == 1 || y == 4;
			helper.setBlock(new BlockPos(x, y, z), shell ? Blocks.GLASS : Blocks.AIR);
		}
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1));
		npc.becomeRogue(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), 0, level.getRandom());
		Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(5, 2, 1));
		npc.hurt(level.damageSources().mobAttack(husk), 1.0f);
		float low = npc.getMaxHealth() * CultivatorNpc.FLEE_HEALTH * 0.5f;
		npc.setHealth(low);
		helper.onEachTick(() -> {
			if (npc.isAlive()) npc.setHealth(low); // kept badly hurt (no recovering out of it meanwhile)
		});
		helper.assertTrue(npc.shouldFlee() && npc.getPursuer() == husk, "Badly hurt by the husk: it runs from it");
		helper.runAfterDelay(330, () -> {
			helper.assertTrue(npc.getLastHurtByMob() == null && !npc.isInCombat(), "The blow long forgotten (vanilla's five seconds), the fight gone quiet");
			helper.assertTrue(npc.getPursuer() == husk && npc.shouldFlee(), "...it still runs from the one who struck it, close by (running from "
					+ npc.getPursuer() + "; the husk " + (husk.isRemoved() ? "removed: " + husk.getRemovalReason() : husk.isAlive()
					? "alive " + String.format(java.util.Locale.ROOT, "%.1f", husk.distanceTo(npc)) + " off" : "dead") + ")");
			husk.teleportTo(husk.getX() + CultivatorNpc.PURSUER_LOST + 16, husk.getY(), husk.getZ());
			helper.runAfterDelay(5, () -> {
				helper.assertTrue(npc.getPursuer() == null && !npc.shouldFlee(), "Left far behind: it stops");
				npc.discard();
				husk.discard();
				helper.succeed();
			});
		});
	}
}
