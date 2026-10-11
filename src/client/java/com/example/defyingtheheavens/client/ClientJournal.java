package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Journal;
import com.example.defyingtheheavens.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import java.util.List;

/** The player's journal as the server last sent it (S2C journal, see Journal), for the cultivation menu's Journal tab. */
public final class ClientJournal {
	private static List<Journal.Entry> entries = List.of();

	public static List<Journal.Entry> entries() {
		return entries;
	}

	public static void set(List<Journal.Entry> journal) {
		entries = List.copyOf(journal);
	}

	/** Asks the server for it (the tab opening). */
	public static void request() {
		ClientPlayNetworking.send(ModPackets.JOURNAL_REQUEST, PacketByteBufs.empty());
	}

	public static void clear() {
		entries = List.of();
	}

	private ClientJournal() {}
}
