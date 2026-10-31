package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The user's two asks of 2026-10-30, built 2026-10-31. Ghostly qi refills on its own as qi does (the user's pick, "On its own, like qi":
 * it had filled only from the place, never in the worlds without yin, and slowly in the Overworld). And possession weighs the highest
 * of the cultivator's paths (a ghost path at Heavenly Lord couldn't take an Entering Nirvana Grand Perfection body while the heaven
 * path, the soul's, stood lower). Driven by hand on survival test players in the player list, as PossessionGameTests' are.
 */
public class GhostRefillPossessionGameTests implements FabricGameTest {
	private static boolean near(double a, double b, double within) {
		return Math.abs(a - b) <= within;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void ghostlyQiRefillsOnItsOwnLikeQi(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.setMortal(false);
		c.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		c.netherworld().setPath(PlayerCultivation.rank(Realm.HEAVENLY_LORD, Stage.GRAND_PERFECTION), 0);
		double pool = GhostQi.realmPool(c);
		double nowhere = pool / GhostQi.fillPerSecond(c, 0), overworld = pool / GhostQi.fillPerSecond(c, GhostQi.OVERWORLD);
		double netherworld = pool / GhostQi.fillPerSecond(c, GhostQi.NETHERWORLD);
		double battlefield = pool / GhostQi.fillPerSecond(c, GhostQi.ANCIENT_BATTLEFIELD);
		helper.assertTrue(near(nowhere, 40, 1e-6),
				"Where there is no yin at all an empty Heavenly Lord pool fills in 40 s, as the qi pool does (" + nowhere + " s)");
		helper.assertTrue(near(overworld, 200 / 5.25, 1e-6) && near(netherworld, 200 / 15.0, 1e-6) && near(battlefield, 200 / 105.0, 1e-6),
				"The place adds its own: the Overworld ~38 s, the Netherworld ~13 s, a battlefield ~2 s (" + overworld + ", " + netherworld
						+ ", " + battlefield + ")");
		helper.assertTrue(near(GhostQi.fillPerSecond(GhostQi.NETHERWORLD), 2, 1e-9) && near(SoulProjection.GHOST_SOUL_BASE, 2, 1e-9),
				"The place's own share is as it was, and so is a ghost soul's upkeep reckoned from it");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anEmptyPoolFillsInTheOverworld(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "refill-overworld");
			PlayerCultivation c = CultivationManager.get(p);
			c.setMortal(false);
			c.setState(Realm.QI_REFINING, Stage.EARLY, 0);
			c.netherworld().setPath(PlayerCultivation.rank(Realm.HEAVENLY_LORD, Stage.GRAND_PERFECTION), 0);
			c.netherworld().setGhostQi(0);
			NetherworldManager.second(p, helper.getLevel().getGameTime());
			double filled = c.netherworld().ghostQi(), expected = GhostQi.fillPerSecond(c, GhostQi.OVERWORLD);
			helper.assertTrue(filled > 0 && near(filled, expected, expected * 1e-9),
					"A second in the Overworld fills the pool by its own and the place's (" + filled + " of " + expected + ")");
			helper.assertTrue(GhostQi.realmPool(c) / filled < 60, "...so the realm's pool is full in under a minute ("
					+ GhostQi.realmPool(c) / filled + " s)");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void possessionWeighsTheHighestPath(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		CultivatorNpc npc = null;
		try {
			npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(3, 2, 2));
			npc.setRank(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.GRAND_PERFECTION));
			// Every path at Nascent Soul: a body of its own realm is too strong, as ever.
			ServerPlayer even = projected(helper, made, "paths-even", c -> {});
			helper.assertTrue(SoulProjection.tryPossess(even, npc) == InteractionResult.FAIL && !npc.isVacant(),
					"A Nascent Soul can't take a Nascent Soul Grand Perfection body");
			// The user's case a few realms down: the heaven path, the soul's, at Nascent Soul, the ghost path at Heavenly Being Grand
			// Perfection (theirs: a Heavenly Lord ghost path, an Entering Nirvana Grand Perfection body).
			ServerPlayer ghostly = projected(helper, made, "paths-ghostly",
					c -> c.netherworld().setPath(PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION), 0));
			PlayerCultivation c = CultivationManager.own(ghostly);
			helper.assertTrue(c.soulPath() == TwoPaths.Path.HEAVEN && TwoPaths.ghostRank(c, true) > TwoPaths.heavenRank(c, true),
					"The soul is the heaven path's, the ghost path the higher");
			helper.assertTrue(SoulProjection.tryPossess(ghostly, npc) == InteractionResult.SUCCESS && npc.isVacant(),
					"Its ghost path weighs the body: taken");
			helper.succeed();
		} finally {
			leave(made);
			if (npc != null) npc.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theSoulReachesByTheHighestPath(GameTestHelper helper) {
		// 2026-10-31, the rest of the soul after possession: its tether's reach and what it brings a body go by the highest path.
		PlayerCultivation heavenOnly = new PlayerCultivation();
		heavenOnly.setMortal(false);
		heavenOnly.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
		PlayerCultivation ghostly = new PlayerCultivation();
		ghostly.setMortal(false);
		ghostly.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
		ghostly.netherworld().setPath(PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.EARLY), 0);
		helper.assertTrue(ghostly.soulPath() == TwoPaths.Path.HEAVEN, "The soul is the heaven path's");
		helper.assertTrue(!heavenOnly.soulTetherEndless() && ghostly.soulTetherEndless()
						&& ghostly.soulTetherRadius() == SoulProjection.ENDLESS_TETHER,
				"A Void Shattering ghost path frees the tether, though the soul's own path stands at Nascent Soul");
		PlayerCultivation mortal = new PlayerCultivation();
		helper.assertTrue(Possession.guidance(ghostly, mortal) > Possession.guidance(heavenOnly, mortal),
				"...and quickens a body it is in as its highest path would (" + Possession.guidance(ghostly, mortal) + " against "
						+ Possession.guidance(heavenOnly, mortal) + ")");
		helper.succeed();
	}

	// --- Helpers (as PossessionGameTests') ---

	/** A survival player in the player list (the helper's mock player is creative), remembered in {@code made} to take out after. */
	private static ServerPlayer survivor(GameTestHelper helper, List<ServerPlayer> made, String name) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isCreative() {
				return false;
			}
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		p.setOnGround(true);
		made.add(p);
		return p;
	}

	/** A Nascent Soul Early cultivator ({@code setup} done to its record first) with its soul already out, full of qi. */
	private static ServerPlayer projected(GameTestHelper helper, List<ServerPlayer> made, String name, Consumer<PlayerCultivation> setup) {
		ServerPlayer p = survivor(helper, made, name);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
		setup.accept(c);
		CultivationStats.apply(p, c);
		p.setHealth(p.getMaxHealth());
		c.setQi(c.maxQi());
		long now = helper.getLevel().getGameTime();
		SoulProjection.toggle(p);
		for (int i = 0; i < SoulProjection.CHANNEL_TICKS; i++) SoulProjection.tick(p, now);
		helper.assertTrue(SoulProjection.isSoul(p), "The soul is out");
		return p;
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) if (Possession.inhabits(p.getUUID())) SoulProjection.letGo(p, null);
		for (ServerPlayer p : made) {
			SoulProjection.onDeath(p); // nothing of a journey is left behind (its body discards itself)
			CultivationManager.own(p).setPossession(null);
			p.server.getPlayerList().remove(p);
		}
	}
}
