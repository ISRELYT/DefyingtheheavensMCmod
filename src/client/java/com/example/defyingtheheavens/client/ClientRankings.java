package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.Rankings;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/** The lists as the server last sent them (ModPackets#sendRankings): every world's hundred, and the player's own list and place. */
public final class ClientRankings {
	/** One listed: their name, rank (PlayerCultivation#rank), sect and title in it (NpcTitle ordinal, -1 none), path (Alignment.Faction), a player or not. */
	public record Row(String name, int rank, String sect, int title, int faction, boolean player) {}

	private static final List<List<Row>> LISTS = new ArrayList<>();
	private static int ownBoard = -1, ownPlace;

	/** Reads S2C rankings after its first flag (whether to open the stele's screen). */
	static void read(FriendlyByteBuf buf) {
		ownBoard = buf.readVarInt();
		ownPlace = buf.readVarInt();
		LISTS.clear();
		for (int b = 0; b < Rankings.Board.values().length; b++) {
			int count = buf.readVarInt();
			List<Row> rows = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				rows.add(new Row(buf.readUtf(64), buf.readVarInt(), buf.readUtf(96), buf.readVarInt() - 1, buf.readVarInt(), buf.readBoolean()));
			}
			LISTS.add(rows);
		}
	}

	public static List<Row> list(int board) {
		return board >= 0 && board < LISTS.size() ? LISTS.get(board) : List.of();
	}

	/** The player's own list (Rankings.Board ordinal), or -1 (a mortal: on none). */
	public static int ownBoard() { return ownBoard; }

	/** The player's place on their own list (1 the first), or 0: not among its hundred. */
	public static int ownPlace() { return ownPlace; }

	/** Asks the server for them afresh (the Journal's page). */
	public static void request() {
		ClientPlayNetworking.send(ModPackets.RANKINGS_REQUEST, PacketByteBufs.create());
	}

	public static void clear() {
		LISTS.clear();
		ownBoard = -1;
		ownPlace = 0;
	}

	private ClientRankings() {}
}
