package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * A player's journal: the rumours cultivators have told them (see Rumours, TalkSystem), so nothing heard is lost. Newest
 * first, at most {@link #MAX_ENTRIES}; the same rumour heard again replaces the old note. Kept with the player's cultivation
 * (PlayerCultivation), sent to the client for the Journal tab of the cultivation menu.
 */
public final class Journal {
	public static final int MAX_ENTRIES = 100;

	/** One note: the rumour's id, the game day it was heard, who told it, and what they said. */
	public record Entry(String id, long day, String teller, Component text) {}

	/** Notes what {@code teller} told {@code player}, and sends the journal on. */
	public static void add(ServerPlayer player, String id, CultivatorNpc teller, Component text) {
		PlayerCultivation c = CultivationManager.get(player);
		List<Entry> journal = c.journal();
		journal.removeIf(entry -> entry.id().equals(id));
		journal.add(0, new Entry(id, Regard.day(player.level().getGameTime()), teller.getPersonalName(), text));
		while (journal.size() > MAX_ENTRIES) journal.remove(journal.size() - 1);
		CultivationManager.markDirty(player.server);
		ModPackets.sendJournal(player);
	}

	public static ListTag save(List<Entry> journal) {
		ListTag list = new ListTag();
		for (Entry entry : journal) {
			CompoundTag tag = new CompoundTag();
			tag.putString("Id", entry.id());
			tag.putLong("Day", entry.day());
			tag.putString("Teller", entry.teller());
			tag.putString("Text", Component.Serializer.toJson(entry.text()));
			list.add(tag);
		}
		return list;
	}

	public static void load(ListTag list, List<Entry> into) {
		into.clear();
		for (int i = 0; i < list.size() && into.size() < MAX_ENTRIES; i++) {
			CompoundTag tag = list.getCompound(i);
			Component text = Component.Serializer.fromJson(tag.getString("Text"));
			if (text != null) into.add(new Entry(tag.getString("Id"), tag.getLong("Day"), tag.getString("Teller"), text));
		}
	}

	public static void write(FriendlyByteBuf buf, List<Entry> journal) {
		buf.writeVarInt(journal.size());
		for (Entry entry : journal) {
			buf.writeUtf(entry.id());
			buf.writeVarLong(entry.day());
			buf.writeUtf(entry.teller());
			buf.writeComponent(entry.text());
		}
	}

	public static List<Entry> read(FriendlyByteBuf buf) {
		int count = Math.min(buf.readVarInt(), MAX_ENTRIES);
		List<Entry> journal = new ArrayList<>(count);
		for (int i = 0; i < count; i++) journal.add(new Entry(buf.readUtf(), buf.readVarLong(), buf.readUtf(), buf.readComponent()));
		return journal;
	}

	private Journal() {}
}
