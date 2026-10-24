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
 * The user's asks of 2026-10-19: the dead begin the ghost path with no method, at the heaven path's no-method pace, and keep it
 * if born again (GhostMethods#begin); and a badly hurt cultivator runs from its pursuer for as long as the pursuer is near, not
 * only for the five seconds vanilla remembers who struck it (CultivatorNpc#getPursuer).
 */
public class GhostPathBeginGameTests implements FabricGameTest {
	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-9;
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void theDeadBeginTheGhostPathWithNoMethodAndKeepIt(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.getAbilities().invulnerable = true; // out of the other tests' fights (a blow ends a meditation)
		PlayerCultivation c = CultivationManager.get(player);
		NetherworldState s = c.netherworld();
		c.awaken(0); // meridians open: a mortal's ghost still has no ghost path (TwoPathsGameTests)
		GhostSystem.makeGhost(player);
		helper.assertTrue(GhostSystem.isGhost(player) && !s.knowsAnyMethod() && GhostMethods.ghostRank(s) < 0, "One of the dead, no method, no ghost path yet");
		// The pace with no foundation known: three quarters of one's with one, as the heaven path's without a method.
		double none = GhostMethods.ratePerSecond(c, GhostQi.SOUL_SAND_VALLEY);
		s.setMethod(GhostMethod.NETHERWORLD_BREATHING, 1, 0);
		double known = GhostMethods.ratePerSecond(c, GhostQi.SOUL_SAND_VALLEY);
		s.setMethod(GhostMethod.NETHERWORLD_BREATHING, 0, 0);
		helper.assertTrue(near(none, known * GhostMethods.NO_METHOD_FACTOR) && near(GhostMethods.NO_METHOD_FACTOR, HeavenMethods.NO_METHOD_FACTOR),
				"No method: the heaven path's no-method pace (" + none + " of " + known + ")");
		s.setDensity(GhostQi.NETHERWORLD); // as among the dead
		player.setOnGround(true);
		MeditationManager.start(player);
		helper.assertTrue(MeditationManager.isMeditating(player.getUUID()), "It sits down to meditate all the same");
		helper.runAfterDelay(40, () -> {
			try {
				helper.assertTrue(GhostMethods.ghostRank(s) == 0 && s.pathCultivation() > 0 && !s.knowsAnyMethod(),
						"Its ghost path begun at Qi Refining Early, and filling (" + s.pathCultivation() + ")");
				// Born again (the Wheel copies its cultivation across): the path is still its own, to go on with in life.
				MeditationManager.forget(player.getUUID());
				s.setGhost(false);
				helper.assertTrue(TwoPaths.ghostRank(c, false) == 0 && TwoPaths.walksBoth(c) && GhostQi.poolMax(c) > 0, "Alive again: on both paths, with a pool");
				TwoPaths.switchPath(player);
				helper.assertTrue(TwoPaths.cultivatesGhost(c), "...and may draw in ghostly qi to go on with it, still with no method");
				helper.assertTrue(GhostMethods.heldByMethod(c) || !GhostMethods.opensWay(c, Realm.FOUNDATION_BUILDING),
						"Foundation Building still wants a foundation of it");
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
