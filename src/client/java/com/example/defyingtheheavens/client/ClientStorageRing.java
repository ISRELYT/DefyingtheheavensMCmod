package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.StorageRings;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** The open Storage Ring's space as the server last sent it (StorageRings#send): for its screen (StorageRingScreen). */
public final class ClientStorageRing {
	/** One kind of thing in the space, and how many. */
	public record Entry(ItemStack kind, long count) {}

	private static int containerId = -1;
	private static StorageRings.Grade grade = StorageRings.Grade.MORTAL;
	private static final List<Entry> ENTRIES = new ArrayList<>();
	/** Bumped with every packet, so the screen knows to filter again. */
	private static int version;

	static void receive(int id, int gradeIndex, boolean fresh, List<Entry> entries) {
		if (fresh || id != containerId) ENTRIES.clear();
		containerId = id;
		grade = StorageRings.Grade.byIndex(gradeIndex);
		ENTRIES.addAll(entries);
		version++;
	}

	/** The space of the screen {@code id}, or nothing if another's was the last sent. */
	public static List<Entry> entries(int id) { return id == containerId ? ENTRIES : List.of(); }
	public static StorageRings.Grade grade() { return grade; }
	public static int version() { return version; }

	/** Disconnect: the last server's space isn't shown on the next. */
	public static void clear() {
		ENTRIES.clear();
		containerId = -1;
		grade = StorageRings.Grade.MORTAL;
		version++;
	}

	/** Stacks' worth filled, as the server counts it (StorageRings.Space#stacksUsed). */
	public static long stacksUsed(int id) {
		long used = 0;
		for (Entry entry : entries(id)) {
			int max = Math.max(1, entry.kind().getMaxStackSize());
			used += (entry.count() + max - 1) / max;
		}
		return used;
	}

	private ClientStorageRing() {}
}
