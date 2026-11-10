package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Earth and Heaven Lists (the user's "Build as designed", 2026-11-10; Rankings): ordered by strength and places won; a player on the list of
 * the highest world they ascended to; a slain cultivator gone, its slayer in its place; the listed thought better of. Their own batch, the lists
 * emptied first (the kept test world's cultivators would be on them).
 */
public class RankingsGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "rankings")
	public void listsGoByStrengthAndPlacesWon(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		MinecraftServer server = helper.getLevel().getServer();
		try {
			Rankings.get(server).clearAll(server);
			ServerPlayer strong = survivor(helper, made, "ranked-strong"), weak = survivor(helper, made, "ranked-weak");
			TestMethods.setAt(CultivationManager.own(strong), Realm.CORE_FORMATION, Stage.EARLY);
			TestMethods.setAt(CultivationManager.own(weak), Realm.FOUNDATION_BUILDING, Stage.EARLY);
			Rankings.note(strong);
			Rankings.note(weak);
			Rankings data = Rankings.get(server);
			helper.assertTrue(data.placeOf(strong.getUUID()) == 1 && data.placeOf(weak.getUUID()) == 2, "Strength first: Core Formation above Foundation Building");
			helper.assertTrue(Rankings.won(server, weak.getUUID(), strong.getUUID()) && data.placeOf(weak.getUUID()) == 1 && data.placeOf(strong.getUUID()) == 2,
					"The weaker beats the stronger in a spar: it takes the first place");
			helper.assertTrue(!Rankings.won(server, weak.getUUID(), strong.getUUID()) || data.placeOf(weak.getUUID()) == 1, "Beating one already below changes nothing");
			Rankings.tick(server); // (the players noted again: nothing lost)
			helper.assertTrue(data.placeOf(weak.getUUID()) == 1, "The place won is kept");
			TestMethods.setAt(CultivationManager.own(strong), Realm.CORE_FORMATION, Stage.MID);
			Rankings.note(strong);
			helper.assertTrue(data.placeOf(strong.getUUID()) == 1, "A breakthrough passes them back");
			Rumours.tick(server);
			helper.assertTrue(Rumours.all(server, Rumours.Kind.RANKING).stream().anyMatch(r -> r.subject.equals("ranked-weak")),
					"A top-ten place won is talked of");
			helper.succeed();
		} finally {
			leave(made);
			Rankings.get(server).clearAll(server);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "rankings")
	public void aPlayerIsOnTheListOfTheHighestWorldTheyReached(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		MinecraftServer server = helper.getLevel().getServer();
		try {
			Rankings.get(server).clearAll(server);
			ServerPlayer p = survivor(helper, made, "ranked-climber");
			TestMethods.setAt(CultivationManager.own(p), Realm.FOUNDATION_BUILDING, Stage.EARLY);
			Rankings.note(p);
			helper.assertTrue(Rankings.get(server).entry(p.getUUID()).board == Rankings.Board.EARTH, "Foundation Building in the lower realms: the Earth List");
			TestMethods.setAt(CultivationManager.own(p), Realm.FOUR_AXIS, Stage.EARLY);
			Rankings.note(p);
			helper.assertTrue(Rankings.get(server).entry(p.getUUID()).board == Rankings.Board.HEAVEN,
					"Four Axis (only reached in the Jade Firmament): the Heaven List, even visiting below");
			TestMethods.setAt(CultivationManager.own(p), Realm.QI_REFINING, Stage.EARLY);
			Rankings.note(p);
			helper.assertTrue(Rankings.get(server).entry(p.getUUID()).board == Rankings.Board.HEAVEN, "Fallen back, still of the world they ascended to");
			helper.assertTrue(Rankings.Board.of(ModDimensions.IMMORTAL_REALM) == Rankings.Board.IMMORTAL && Rankings.Board.of(ModDimensions.GREAT_VOID) == Rankings.Board.VOID
					&& Rankings.Board.of(ModDimensions.NETHERWORLD) == Rankings.Board.NETHERWORLD && Rankings.Board.of(ModDimensions.SECRET_REALM) == null,
					"Each world its list; the secret realms none of their own");
			helper.succeed();
		} finally {
			leave(made);
			Rankings.get(server).clearAll(server);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "rankings")
	public void aSlainCultivatorsPlaceGoesToItsSlayerAndTheListedAreThoughtBetterOf(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		CultivatorNpc npc = null;
		try {
			Rankings.get(server).clearAll(server);
			ServerPlayer p = survivor(helper, made, "ranked-slayer");
			TestMethods.setAt(CultivationManager.own(p), Realm.FOUNDATION_BUILDING, Stage.EARLY);
			npc = CultivatorNpc.createFor(level);
			Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
			npc.moveTo(at.x, at.y, at.z, 0, 0);
			npc.setRank(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.LATE));
			npc.setNoAi(true);
			level.addFreshEntity(npc);
			Rankings.note(npc);
			int before = npc.regard(p);
			Rankings.note(p);
			Rankings data = Rankings.get(server);
			helper.assertTrue(data.placeOf(npc.getUUID()) == 1 && data.placeOf(p.getUUID()) == 2, "The cultivator, stronger, stands first");
			helper.assertTrue(npc.regard(p) == Regard.clamp(before + Rankings.TOP_TEN_REGARD) || npc.regard(p) > before,
					"On its world's list, the player is thought better of (" + before + " -> " + npc.regard(p) + ")");
			Rankings.slain(npc, p);
			helper.assertTrue(data.entry(npc.getUUID()) == null && data.placeOf(p.getUUID()) == 1, "Slain: gone from the list, its slayer in its place");
			helper.succeed();
		} finally {
			if (npc != null) npc.discard();
			leave(made);
			Rankings.get(server).clearAll(server);
		}
	}

	/** A survival player in the player list (the helper's mock player is creative), remembered in {@code made}. */
	private static ServerPlayer survivor(GameTestHelper helper, List<ServerPlayer> made, String name) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isCreative() {
				return false;
			}
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		p.setOnGround(true);
		CultivationManager.own(p).awaken(0);
		made.add(p);
		return p;
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) p.server.getPlayerList().remove(p);
	}
}
