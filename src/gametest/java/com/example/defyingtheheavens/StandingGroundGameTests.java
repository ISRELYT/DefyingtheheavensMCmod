package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * The user's asks of 2026-10-27: strike one of those waiting on the Shore of Nirvana and the whole camp answers (NirvanaCamp#rally); one of
 * Entering Nirvana or above runs from nobody for their path or their strength, only from a beating (CultivatorNpc#holdsGround); and in the
 * Immortal Realm nobody goes for another over the righteous and the devilish, a quarrel beneath them there (CultivatorNpc#abovePathQuarrels,
 * TalkSystem#isPathEnemy).
 */
public class StandingGroundGameTests implements FabricGameTest {
	/** One of {@code realm} of path {@code alignment}, kept still, in the test's own space. */
	private static CultivatorNpc npc(GameTestHelper helper, BlockPos pos, Realm realm, int alignment) {
		CultivatorNpc npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, pos);
		npc.setRank(PlayerCultivation.rank(realm, Stage.EARLY), true);
		npc.setAlignment(alignment);
		npc.setHealth(npc.getMaxHealth());
		return npc;
	}

	/** One of {@code realm} of path {@code alignment} in {@code level} at {@code at}, kept still in the air. */
	private static CultivatorNpc npcIn(ServerLevel level, BlockPos at, Realm realm, int alignment, List<CultivatorNpc> made) {
		CultivatorNpc npc = CultivatorNpc.createFor(level);
		npc.becomeRogue(PlayerCultivation.rank(realm, Stage.EARLY), alignment, level.getRandom());
		npc.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		npc.setNoAi(true);
		npc.setNoGravity(true);
		npc.setHealth(npc.getMaxHealth());
		level.addFreshEntity(npc);
		made.add(npc);
		return npc;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void enteringNirvanaAndUpHoldTheirGround(GameTestHelper helper) {
		CultivatorNpc nirvana = npc(helper, new BlockPos(1, 2, 1), Realm.ENTERING_NIRVANA, Alignment.RIGHTEOUS_ABOVE + 40);
		CultivatorNpc immortal = npc(helper, new BlockPos(2, 2, 1), Realm.TRUE_IMMORTAL, Alignment.RIGHTEOUS_ABOVE + 40);
		CultivatorNpc vessel = npc(helper, new BlockPos(3, 2, 1), Realm.SACRED_VESSEL, Alignment.RIGHTEOUS_ABOVE + 40);
		CultivatorNpc devil = npc(helper, new BlockPos(1, 2, 3), Realm.TRUE_IMMORTAL, Alignment.DEVILISH_BELOW - 40);
		helper.assertTrue(nirvana.holdsGround() && immortal.holdsGround() && !vessel.holdsGround(), "Entering Nirvana and up hold their ground; Sacred Vessel doesn't");
		// A True Immortal of the other path bearing down: the one below Entering Nirvana runs, the others stand.
		for (CultivatorNpc npc : List.of(nirvana, immortal, vessel)) npc.startFleeing(devil);
		helper.assertTrue(!nirvana.shouldFlee() && !immortal.shouldFlee(), "Of Entering Nirvana and up, none runs from a stronger one of the other path");
		helper.assertTrue(vessel.shouldFlee(), "...a Sacred Vessel still does");
		// A beating sends it back all the same.
		nirvana.hurt(helper.getLevel().damageSources().mobAttack(devil), 1);
		nirvana.setHealth(nirvana.getMaxHealth() * 0.1f);
		helper.assertTrue(nirvana.shouldFlee(), "Badly hurt, with its foe still on it, one of Entering Nirvana backs away");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
	public void theImmortalRealmIsAbovePathQuarrels(GameTestHelper helper) {
		// Down here, the righteous and the devilish are each other's enemies.
		CultivatorNpc righteous = npc(helper, new BlockPos(1, 2, 1), Realm.TRUE_IMMORTAL, Alignment.RIGHTEOUS_ABOVE + 40);
		CultivatorNpc devilish = npc(helper, new BlockPos(3, 2, 1), Realm.TRUE_IMMORTAL, Alignment.DEVILISH_BELOW - 40);
		helper.assertTrue(righteous.isEnemy(devilish) && devilish.isEnemy(righteous) && !righteous.abovePathQuarrels(), "Below, the two paths are enemies");
		// In the Immortal Realm, in the open sky over the sea of clouds, the same two are not.
		ServerLevel immortal = helper.getLevel().getServer().getLevel(ModDimensions.IMMORTAL_REALM);
		helper.assertTrue(immortal != null, "The Immortal Realm is there");
		BlockPos at = new BlockPos(4000, ModDimensions.IMMORTAL_CLOUD_Y + 60, 4000);
		immortal.setChunkForced(at.getX() >> 4, at.getZ() >> 4, true);
		List<CultivatorNpc> made = new ArrayList<>();
		boolean[] done = {false};
		String[] failure = {null};
		helper.succeedWhen(() -> {
			if (failure[0] != null) throw new GameTestAssertException(failure[0]);
			if (!immortal.isPositionEntityTicking(at)) throw new GameTestAssertException("Waiting for the sky over the clouds to load");
			if (!done[0]) {
				done[0] = true;
				try {
					CultivatorNpc up = npcIn(immortal, at, Realm.TRUE_IMMORTAL, Alignment.RIGHTEOUS_ABOVE + 40, made);
					CultivatorNpc down = npcIn(immortal, at.east(3), Realm.TRUE_IMMORTAL, Alignment.DEVILISH_BELOW - 40, made);
					helper.assertTrue(up.abovePathQuarrels() && !up.isEnemy(down) && !down.isEnemy(up), "In the Immortal Realm neither is the other's enemy for its path");
					helper.assertTrue(!up.isThreat(down), "...nor a threat to run from, though as strong");
					// Struck, it is a threat: it fights back.
					down.hurt(immortal.damageSources().mobAttack(up), 5);
					helper.assertTrue(down.isThreat(up) && down.getLastHurtByMob() == up, "Struck, it takes the striker for its foe");
				} catch (GameTestAssertException e) {
					failure[0] = e.getMessage();
					throw e;
				} finally {
					for (CultivatorNpc npc : made) npc.discard();
					immortal.setChunkForced(at.getX() >> 4, at.getZ() >> 4, false);
				}
			}
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
	public void strikeOneOfTheCampAndTheCampAnswers(GameTestHelper helper) {
		ServerLevel greatVoid = helper.getLevel().getServer().getLevel(ModDimensions.GREAT_VOID);
		helper.assertTrue(greatVoid != null, "The Great Void is there");
		BlockPos middle = NirvanaCamp.middle(greatVoid);
		BlockPos at = middle.above(20);
		greatVoid.setChunkForced(middle.getX() >> 4, middle.getZ() >> 4, true);
		List<CultivatorNpc> made = new ArrayList<>();
		boolean[] done = {false};
		String[] failure = {null};
		helper.succeedWhen(() -> {
			if (failure[0] != null) throw new GameTestAssertException(failure[0]);
			if (!greatVoid.isPositionEntityTicking(middle)) throw new GameTestAssertException("Waiting for the camp to load");
			if (!done[0]) {
				done[0] = true;
				try {
					List<CultivatorNpc> campers = new ArrayList<>();
					for (int i = 0; i < 3; i++) {
						CultivatorNpc camper = npcIn(greatVoid, at.east(2 * i), Realm.ENTERING_NIRVANA, i * 40 - 40, made);
						camper.setSettledAt(middle);
						campers.add(camper);
					}
					helper.assertTrue(campers.stream().allMatch(NirvanaCamp::isCamper), "Three wait in the camp");
					CultivatorNpc stranger = npcIn(greatVoid, at.south(4), Realm.ENTERING_NIRVANA, 0, made);
					// One of them struck by a stranger: the other two take the stranger for their foe as well.
					campers.get(0).hurt(greatVoid.damageSources().mobAttack(stranger), 5);
					for (int i = 1; i < 3; i++) {
						CultivatorNpc camper = campers.get(i);
						helper.assertTrue(camper.getTarget() == stranger && camper.getLastHurtByMob() == stranger,
								"Strike one of the camp and the whole camp answers (" + i + ": " + camper.getTarget() + ")");
					}
					// One of the camp striking another: theirs alone.
					for (CultivatorNpc camper : campers) {
						camper.setTarget(null);
						camper.setLastHurtByMob(null);
					}
					campers.get(1).hurt(greatVoid.damageSources().mobAttack(campers.get(2)), 5);
					helper.assertTrue(campers.get(0).getTarget() == null, "A quarrel within the camp is the two's alone");
				} catch (GameTestAssertException e) {
					failure[0] = e.getMessage();
					throw e;
				} finally {
					for (CultivatorNpc npc : made) npc.discard();
					greatVoid.setChunkForced(middle.getX() >> 4, middle.getZ() >> 4, false);
				}
			}
		});
	}
}
