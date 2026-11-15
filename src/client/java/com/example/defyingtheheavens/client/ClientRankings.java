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

	/** One sect on a Sect List (SectList): its name, score, people, its strongest's rank (-1 none), path, a player's sect or not, the player's own or not. */
	public record SectRow(String name, int score, int people, int best, int faction, boolean players, boolean own) {}

	private static final List<List<Row>> LISTS = new ArrayList<>();
	private static final List<List<SectRow>> SECTS = new ArrayList<>();
	private static int ownBoard = -1, ownPlace, ownSectBoard = -1, ownSectPlace;

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
		ownSectBoard = buf.readVarInt();
		ownSectPlace = buf.readVarInt();
		SECTS.clear();
		for (int b = 0; b < Rankings.Board.values().length; b++) {
			int count = buf.readVarInt();
			List<SectRow> rows = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				rows.add(new SectRow(buf.readUtf(96), buf.readVarInt(), buf.readVarInt(), buf.readVarInt() - 1, buf.readVarInt(), buf.readBoolean(), buf.readBoolean()));
			}
			SECTS.add(rows);
		}
	}

	public static List<Row> list(int board) {
		return board >= 0 && board < LISTS.size() ? LISTS.get(board) : List.of();
	}

	/** {@code board}'s Sect List (SectList): its ten. */
	public static List<SectRow> sects(int board) {
		return board >= 0 && board < SECTS.size() ? SECTS.get(board) : List.of();
	}

	/** The world of the player's own sect's list (Rankings.Board ordinal), or -1: no sect. */
	public static int ownSectBoard() { return ownSectBoard; }

	/** Their sect's place on its Sect List (1 the first), or 0: not among the ten. */
	public static int ownSectPlace() { return ownSectPlace; }

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
		SECTS.clear();
		ownBoard = -1;
		ownPlace = 0;
		ownSectBoard = -1;
		ownSectPlace = 0;
	}

	private ClientRankings() {}
}
