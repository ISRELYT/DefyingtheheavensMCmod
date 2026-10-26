package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The few of the realms after Four Axis among the cultivators of the world (the user's lore, 2026-10-22): in a realm (a world of its
 * own, the Upper Realm or the Netherworld) those of Dao Integration are rare, those of Void Shattering a handful, of Sacred Vessel
 * one; the many of Entering Nirvana live elsewhere, waiting for the way into the immortal realm to show itself, and no True
 * Immortal walks below it ({@link #QUOTA}). A cultivator (or one of the dead) rises into one of those realms only while its world
 * has room in it (CultivatorNpc#advance, #setRank), waiting at the Grand Perfection below when it hasn't, and holds its place
 * until it dies or is taken out of the world; one of its kind only met in the world, not yet seen there, holds it no longer than
 * {@link #UNCONFIRMED}, and one unseen for {@link #FORGOTTEN} is given up as gone. Players are never held back. Saved with the
 * Overworld.
 */
public class HighRealms extends SavedData {
	private static final String NAME = DefyingTheHeavens.MOD_ID + "_high_realms";
	/** The most of each realm, Dao Integration to True Immortal, that one world holds. */
	static final int[] QUOTA = {24, 5, 1, 0, 0};
	/**
	 * The Great Void Realm's own (2026-10-25), the home of those realms: its Holy Lands' Dao Integration elders, Void Shattering grand
	 * elders and Sacred Vessel patriarchs (HolyLands: four, two and one a Holy Land) with its rogues, and the Entering Nirvana who camp
	 * on the Shore of Nirvana waiting for the Immortal Gate (some twenty, the user's number: NirvanaCamp). Still no True Immortal.
	 */
	static final int[] GREAT_VOID_QUOTA = {96, 32, 12, 24, 0};
	/**
	 * The Immortal Realm's (phase 2, 2026-10-25), the True Immortals' home: those who came up through the Gate, of Entering Nirvana, and
	 * the immortals of its cities, sects and Court. None below Entering Nirvana is born there (the user's rule, 2026-10-25: "people less
	 * than EN shouldnt spawn", NpcSpawner#IMMORTAL_FLOOR; a sect's own disciples perhaps excepted), and its Entering Nirvana have no limit, so
	 * the census never holds one of them back below it: whoever the True Immortals' room turns away waits at Entering Nirvana's Grand
	 * Perfection. The places of the realms below are a sect's disciples' alone.
	 */
	static final int[] IMMORTAL_REALM_QUOTA = {16, 16, 16, Integer.MAX_VALUE, 160};
	/** One unseen this long (ticks: a hundred days) is given up as gone (its land lost, say), its place free again. */
	static final long FORGOTTEN = 100L * Lifespan.YEAR_TICKS;
	/** A place taken by one not yet seen in the world (made, never spawned) is given up after this long (ticks). */
	static final long UNCONFIRMED = 1200;

	/** One holding a place: in which world, of which realm (its ordinal), when last seen, and whether it has been seen at all. */
	private record Entry(String world, int realm, long seen, boolean confirmed) {}

	private final Map<UUID, Entry> entries = new HashMap<>();

	public static HighRealms get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(HighRealms::load, HighRealms::new, NAME);
	}

	/** How many of {@code realm} one world holds at most: no limit before Dao Integration. */
	public static int quota(Realm realm) {
		return quota(realm, QUOTA);
	}

	/** The same in {@code world} (a dimension's id): the Great Void's own, the Immortal Realm's, else the one rule for every world. */
	public static int quota(Realm realm, String world) {
		return quota(realm, world.equals(ModDimensions.GREAT_VOID.location().toString()) ? GREAT_VOID_QUOTA
				: world.equals(ModDimensions.IMMORTAL_REALM.location().toString()) ? IMMORTAL_REALM_QUOTA : QUOTA);
	}

	private static int quota(Realm realm, int[] quotas) {
		int index = realm.ordinal() - Realm.DAO_INTEGRATION.ordinal();
		return index < 0 ? Integer.MAX_VALUE : quotas[Math.min(index, quotas.length - 1)];
	}

	/**
	 * The highest rank up to {@code wanted} that {@code self} may hold in {@code level}'s world: {@code wanted} itself while its realm
	 * has room (its own place counted as room), else the Grand Perfection of the highest realm below it that has.
	 */
	public static int allowed(ServerLevel level, UUID self, int wanted) {
		int first = PlayerCultivation.rank(Realm.DAO_INTEGRATION, Stage.EARLY);
		int top = Math.min(wanted, PlayerCultivation.LAST_RANK);
		if (top < first) return top;
		HighRealms census = get(level.getServer());
		census.forgetStale(level.getGameTime());
		String world = world(level);
		while (top >= first) {
			Realm realm = Realm.byIndex(top / Stage.values().length);
			if (census.count(world, realm, self) < quota(realm, world)) return top;
			top = PlayerCultivation.rank(realm, Stage.EARLY) - 1; // the realm below's Grand Perfection
		}
		return top;
	}

	/** How many of {@code realm} hold a place in {@code world}, {@code self} left out. */
	private int count(String world, Realm realm, UUID self) {
		int count = 0;
		for (Map.Entry<UUID, Entry> e : entries.entrySet()) {
			if (!e.getKey().equals(self) && e.getValue().world.equals(world) && e.getValue().realm == realm.ordinal()) count++;
		}
		return count;
	}

	/**
	 * {@code npc} at its rank now: of a realm after Four Axis, it holds its place there ({@code seen}: it stands in the world now, not
	 * only made); below them, it holds none.
	 */
	public static void hold(CultivatorNpc npc, boolean seen) {
		if (!(npc.level() instanceof ServerLevel level)) return;
		HighRealms census = get(level.getServer());
		int rank = npc.getRank();
		if (rank < PlayerCultivation.rank(Realm.DAO_INTEGRATION, Stage.EARLY)) {
			if (census.entries.remove(npc.getUUID()) != null) census.setDirty();
			return;
		}
		Entry before = census.entries.get(npc.getUUID());
		Entry now = new Entry(world(level), rank / Stage.values().length, level.getGameTime(), seen || before != null && before.confirmed);
		census.entries.put(npc.getUUID(), now);
		if (before == null || before.realm != now.realm || !before.world.equals(now.world) || before.confirmed != now.confirmed) census.setDirty();
	}

	/** {@code id} is gone from the world for good (dead, discarded): its place is free. */
	public static void gone(MinecraftServer server, UUID id) {
		HighRealms census = get(server);
		if (census.entries.remove(id) != null) census.setDirty();
	}

	/** How many hold a place of {@code realm} in {@code level}'s world (the game tests). */
	public static int holding(ServerLevel level, Realm realm) {
		HighRealms census = get(level.getServer());
		census.forgetStale(level.getGameTime());
		return census.count(world(level), realm, null);
	}

	private void forgetStale(long now) {
		for (Iterator<Entry> it = entries.values().iterator(); it.hasNext(); ) {
			Entry e = it.next();
			if (now - e.seen > (e.confirmed ? FORGOTTEN : UNCONFIRMED)) {
				it.remove();
				setDirty();
			}
		}
	}

	private static String world(ServerLevel level) {
		return level.dimension().location().toString();
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		entries.forEach((id, e) -> {
			CompoundTag entry = new CompoundTag();
			entry.putUUID("Id", id);
			entry.putString("World", e.world);
			entry.putInt("Realm", e.realm);
			entry.putLong("Seen", e.seen);
			entry.putBoolean("Confirmed", e.confirmed);
			list.add(entry);
		});
		tag.put("Entries", list);
		return tag;
	}

	private static HighRealms load(CompoundTag tag) {
		HighRealms census = new HighRealms();
		ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			census.entries.put(entry.getUUID("Id"), new Entry(entry.getString("World"), entry.getInt("Realm"), entry.getLong("Seen"),
					entry.getBoolean("Confirmed")));
		}
		return census;
	}
}
