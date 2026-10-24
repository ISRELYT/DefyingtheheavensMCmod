package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * The op's commands for every path (the user's ask, 2026-10-24: "add commands that let you add cultivation or set realm and stage in
 * other paths besides the heaven one"): /cultivation set [heaven|ghost|demon] <realm> <stage> and /cultivation addcultivation
 * [heaven|ghost|demon] <amount>, run as an op runs them; the heaven path's without a path named, as before; each path set or fed
 * leaving the others where they stood; a human's Demon Path refused; a mortal's ghost path set with its meridians opened.
 */
public class PathCommandsGameTests implements FabricGameTest {
	/** A player of {@code race}, born (DemonPath#born), in survival, out of the player list. */
	private static ServerPlayer born(GameTestHelper helper, Bloodline race) {
		ServerPlayer p = FakePlayer.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "pathcmd"));
		p.setGameMode(GameType.SURVIVAL);
		Bloodlines.set(helper.getLevel().getServer(), p.getUUID(), race, 0);
		DemonPath.born(p);
		return p;
	}

	/** {@code command} run as {@code p} with an op's leave: what it returned (0 if it failed). */
	private static int run(ServerPlayer p, String command) {
		return p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), command);
	}

	private static void forget(MinecraftServer server, ServerPlayer... players) {
		for (ServerPlayer p : players) {
			Bloodlines.forget(server, p.getUUID());
			RaceGifts.readyAgain(p.getUUID());
			RaceGifts.forget(p.getUUID());
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyPathIsSetByCommand(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer tiger = born(helper, Bloodline.TIGER), human = born(helper, Bloodline.HUMAN);
		try {
			PlayerCultivation t = CultivationManager.get(tiger), h = CultivationManager.get(human);
			helper.assertTrue(run(tiger, "cultivation set 3 2") == 1 && t.getRealm() == Realm.CORE_FORMATION && t.getStage() == Stage.MID,
					"/cultivation set <realm> <stage> is still the heaven path's");
			helper.assertTrue(run(tiger, "cultivation set heaven 2 4") == 1 && t.getRealm() == Realm.FOUNDATION_BUILDING
					&& t.getStage() == Stage.GRAND_PERFECTION, "...and so is /cultivation set heaven");
			int fbGp = PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION);

			int nsEarly = PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY);
			helper.assertTrue(run(tiger, "cultivation set ghost 4 1") == 1 && GhostMethods.ghostRank(t.netherworld()) == nsEarly
					&& t.netherworld().pathCultivation() == 0, "/cultivation set ghost: the ghost path at Nascent Soul Early, its bar empty");
			helper.assertTrue(TwoPaths.heavenRank(t, false) == fbGp && DemonPath.rank(t) == 0, "...the heaven path and the Demon Path where they were");

			int hbLate = PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.LATE);
			helper.assertTrue(run(tiger, "cultivation set demon 5 3") == 1 && DemonPath.rank(t) == hbLate && t.demon().cultivation() == 0,
					"/cultivation set demon: the tiger's Demon Path at Heavenly Being Late, its bar empty");
			helper.assertTrue(TwoPaths.heavenRank(t, false) == fbGp && GhostMethods.ghostRank(t.netherworld()) == nsEarly,
					"...the heaven path and the ghost path where they were");
			Spell metal = null;
			for (Spell spell : Spell.values()) {
				if (spell.element() == QiElement.METAL && spell.realmNeeded() <= hbLate && !spell.isDual()) metal = spell;
			}
			helper.assertTrue(metal != null && metal.isKnown(t), "...its blood's Metal spells known as far as it ("
					+ (metal == null ? "none" : metal.getId()) + ")");
			helper.assertTrue(run(tiger, "cultivation set demon 1 1") == 1 && DemonPath.rank(t) == 0, "...and set back down to Qi Refining Early");

			helper.assertTrue(h.isMortal() && run(human, "cultivation set demon 2 1") == 0 && DemonPath.rank(h) == -1,
					"A human has no Demon Path: /cultivation set demon refuses it");
			int fbEarly = PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY);
			helper.assertTrue(run(human, "cultivation set ghost 2 1") == 1 && GhostMethods.ghostRank(h.netherworld()) == fbEarly,
					"A mortal's ghost path is set by the command");
			helper.assertTrue(!h.isMortal() && TwoPaths.heavenRank(h, false) == 0,
					"...its meridians opened, so it doesn't lie dormant (the heaven path at Qi Refining Early)");
			helper.succeed();
		} finally {
			forget(server, tiger, human);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyPathTakesCultivationByCommand(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer fox = born(helper, Bloodline.FOX), human = born(helper, Bloodline.HUMAN);
		try {
			PlayerCultivation c = CultivationManager.get(fox), h = CultivationManager.get(human);
			helper.assertTrue(run(fox, "cultivation addcultivation 10") == 1 && Math.abs(c.getCultivation() - 10) < 1e-6,
					"/cultivation addcultivation <amount> is still the heaven path's (" + c.getCultivation() + ")");
			helper.assertTrue(run(fox, "cultivation addcultivation heaven 5") == 1 && Math.abs(c.getCultivation() - 15) < 1e-6,
					"...and so is /cultivation addcultivation heaven (" + c.getCultivation() + ")");

			helper.assertTrue(run(fox, "cultivation addcultivation demon 40") == 1 && Math.abs(c.demon().cultivation() - 40) < 1e-6
					&& Math.abs(c.getCultivation() - 15) < 1e-6, "/cultivation addcultivation demon: onto the Demon Path's bar alone");
			int gp = PlayerCultivation.rank(Realm.QI_REFINING, Stage.GRAND_PERFECTION);
			helper.assertTrue(run(fox, "cultivation addcultivation demon 1000000000") == 1 && DemonPath.rank(c) == gp && DemonPath.atBottleneck(c),
					"...its stages stepping on to Grand Perfection's bottleneck, and waiting there (" + DemonPath.rank(c) + ")");

			helper.assertTrue(run(fox, "cultivation addcultivation ghost 100") == 0, "No ghost path begun: /cultivation addcultivation ghost refuses");
			helper.assertTrue(run(fox, "cultivation set ghost 1 1") == 1 && run(fox, "cultivation addcultivation ghost 100") == 1
					&& Math.abs(c.netherworld().pathCultivation() - 100) < 1e-6, "Begun, it takes that many seconds on its bar ("
					+ c.netherworld().pathCultivation() + ")");
			helper.assertTrue(Math.abs(c.getCultivation() - 15) < 1e-6 && DemonPath.rank(c) == gp, "...the other paths where they were");

			helper.assertTrue(run(human, "cultivation addcultivation demon 5") == 0 && DemonPath.rank(h) == -1,
					"A human has no Demon Path: /cultivation addcultivation demon refuses it");
			helper.succeed();
		} finally {
			forget(server, fox, human);
		}
	}
}
