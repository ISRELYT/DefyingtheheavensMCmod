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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Sect List (the user's "Build as designed", 2026-11-13; SectList): each world's ten strongest sects by their strongest people and their
 * places on the world's list; its members thought the better of; a rise into the top three talked of.
 */
public class SectListGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE, batch = "sect_list")
	public void aSectsScoreIsItsTenStrongestAndTheirPlaces(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		Sect sect = sect(helper, "Twelve Peaks Sect", 12, 20);
		try {
			SectList.Standing standing = SectList.standing(server, Rankings.get(server), Rankings.Board.EARTH, sect);
			double strongest = 0;
			for (int i = 0; i < SectList.STRONGEST; i++) strongest += 20 - i + 1; // its twelve at 20, 19, ... : the ten best, each rank + 1
			helper.assertTrue(Math.abs(standing.score() - strongest) < 1.0e-9 && standing.people() == 12 && standing.best() == 20,
					"Its ten strongest summed, each rank and one: " + standing.score() + " (" + strongest + ")");
			helper.assertTrue(SectList.placeWorth(1) == SectList.PLACE_WORTH && Math.abs(SectList.placeWorth(100) - SectList.PLACE_WORTH / 100) < 1.0e-9
					&& SectList.placeWorth(101) == 0, "A place on the list is worth twenty at the first, a fifth at the hundredth, nothing past it");
			helper.succeed();
		} finally {
			sect.extinct = true;
			SectList.clear();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "sect_list")
	public void theStrongestSectsStandFirstAndTheirPeopleAreThoughtBetterOf(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		List<ServerPlayer> made = new ArrayList<>();
		int top = PlayerCultivation.LAST_RANK;
		Sect first = sect(helper, "First Heaven Sect", 10, top), second = sect(helper, "Second Heaven Sect", 10, top - 1);
		CultivatorNpc npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(3, 2, 3));
		try {
			SectList.clear();
			helper.assertTrue(SectList.placeOf(server, Rankings.Board.EARTH, first.id) == 1 && SectList.placeOf(server, Rankings.Board.EARTH, second.id) == 2,
					"The strongest sect first, the next second: " + SectList.top(server, Rankings.Board.EARTH));
			ServerPlayer member = survivor(helper, made, "sect-list-member"), stranger = survivor(helper, made, "sect-list-stranger");
			first.disciples.put(member.getUUID(), new Sect.Disciple(member.getUUID(), "sect-list-member", helper.getLevel().getGameTime()));
			helper.assertTrue(SectList.regardBonus(member, npc) == SectList.TOP_REGARD && SectList.regardBonus(stranger, npc) == 0,
					"A top-ten sect's disciple is thought the better of; one of none isn't");
			helper.succeed();
		} finally {
			first.extinct = true;
			second.extinct = true;
			npc.discard();
			leave(made);
			SectList.clear();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "sect_list")
	public void aSectRisingIntoTheTopThreeIsTalkedOf(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		int top = PlayerCultivation.LAST_RANK;
		Sect leader = sect(helper, "Old Leader Sect", 10, top - 1), riser = sect(helper, "Rising Sun Sect", 10, top - 2);
		try {
			SectList.clear();
			helper.assertTrue(SectList.look(server) == 0, "The first look only notes the top three");
			for (Sect.Member m : riser.members.values()) m.rank = top;
			helper.assertTrue(SectList.look(server) == 1 && SectList.placeOf(server, Rankings.Board.EARTH, riser.id) == 1,
					"Risen past the leader into the first place: talked of, once");
			helper.assertTrue(SectList.look(server) == 0, "Nothing changed since: nothing more said");
			helper.succeed();
		} finally {
			leader.extinct = true;
			riser.extinct = true;
			Rumours.clearPending();
			SectList.clear();
		}
	}

	// --- Helpers ---

	/** A sect in the test's world with {@code count} cultivators on its roll, the first at {@code best} and each next a rank lower. */
	private static Sect sect(GameTestHelper helper, String name, int count, int best) {
		ServerLevel level = helper.getLevel();
		Sect sect = new Sect(UUID.randomUUID());
		sect.name = name;
		sect.dimension = level.dimension();
		sect.core = helper.absolutePos(new BlockPos(1, 1, 1));
		for (int i = 0; i < count; i++) {
			UUID id = UUID.randomUUID();
			sect.members.put(id, new Sect.Member(id, Math.max(0, best - i), i));
		}
		SectData.get(level.getServer()).put(sect);
		return sect;
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
		for (ServerPlayer p : made) {
			QiFlight.forget(p.getUUID());
			p.server.getPlayerList().remove(p);
		}
	}
}
