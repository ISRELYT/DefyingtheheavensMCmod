package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.StorageFormations;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The open storage formation's space as the server last sent it (StorageFormations#sendOpen): for its screen (StorageFormationScreen),
 * with its tier, whether the viewer owns it (Refine and Trust are theirs), and whom it trusts.
 */
public final class ClientStorageSpace {
	/** One kind of thing in the space, and how many. */
	public record Entry(ItemStack kind, long count) {}

	/** One the formation trusts, by the name they had when trusted. */
	public record Trusted(UUID id, String name) {}

	private static int containerId = -1;
	private static StorageFormations.Tier tier = StorageFormations.Tier.LOW;
	private static boolean owns;
	private static final List<Trusted> TRUSTED = new ArrayList<>();
	private static final List<Entry> ENTRIES = new ArrayList<>();
	/** Bumped with every packet, so the screen knows to filter again. */
	private static int version;

	static void receive(int id, int tierIndex, boolean owner, List<Trusted> trusted, boolean fresh, List<Entry> entries) {
		if (fresh || id != containerId) ENTRIES.clear();
		containerId = id;
		tier = StorageFormations.Tier.byIndex(tierIndex);
		owns = owner;
		if (fresh) {
			TRUSTED.clear();
			TRUSTED.addAll(trusted);
		}
		ENTRIES.addAll(entries);
		version++;
	}

	/** The space of the screen {@code id}, or nothing if another's was the last sent. */
	public static List<Entry> entries(int id) { return id == containerId ? ENTRIES : List.of(); }
	public static StorageFormations.Tier tier() { return tier; }
	public static boolean owns() { return owns; }
	public static List<Trusted> trusted() { return TRUSTED; }
	public static int version() { return version; }

	/** Disconnect: the last server's space isn't shown on the next. */
	public static void clear() {
		ENTRIES.clear();
		TRUSTED.clear();
		containerId = -1;
		tier = StorageFormations.Tier.LOW;
		owns = false;
		version++;
	}

	/** Stacks' worth filled, as the server counts it (StorageSpaces.Space#stacksUsed). */
	public static long stacksUsed(int id) {
		long used = 0;
		for (Entry entry : entries(id)) {
			int max = Math.max(1, entry.kind().getMaxStackSize());
			used += (entry.count() + max - 1) / max;
		}
		return used;
	}

	private ClientStorageSpace() {}
}
