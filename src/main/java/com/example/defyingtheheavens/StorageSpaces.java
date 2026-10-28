package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The folded spaces the storage formations hold (StorageFormations; the Storage Rings', which held them before, 2026-10-28: the user's ask
 * to remove the rings, their spaces kept under the same ids so a ring becomes a formation with all it held), kept by the world, not
 * the formation: a formation carries only its space's id, so packed up as an item it keeps everything without carrying it.
 * <p>
 * A space holds its things as entries of one kind each, any number of them; how much fits is its formation's tier's (so many stacks'
 * worth, each kind taking whole stacks as in a chest, or without end).
 */
public final class StorageSpaces extends SavedData {
	/** The Storage Rings' file still: their spaces are the formations' now. */
	private static final String FILE_ID = "defying_the_heavens_storage_rings";

	/** One kind of thing in a space: what it is (a single item, its tags and all) and how many. */
	public static final class Entry {
		final ItemStack kind;
		long count;

		Entry(ItemStack kind, long count) {
			this.kind = kind.copyWithCount(1);
			this.count = count;
		}

		public ItemStack kind() { return kind; }
		public long count() { return count; }

		/** Whole stacks it fills. */
		long stacks() {
			int max = Math.max(1, kind.getMaxStackSize());
			return (count + max - 1) / max;
		}
	}

	/** One space: its entries, in the order they first came in. */
	public static final class Space {
		final List<Entry> entries = new ArrayList<>();

		public List<Entry> entries() { return entries; }

		public boolean isEmpty() { return entries.isEmpty(); }

		/** Stacks' worth filled. */
		public long stacksUsed() {
			long used = 0;
			for (Entry entry : entries) used += entry.stacks();
			return used;
		}

		Entry find(ItemStack stack) {
			for (Entry entry : entries) if (ItemStack.isSameItemSameTags(entry.kind, stack)) return entry;
			return null;
		}

		/** How many of {@code stack}'s kind still fit in a space of {@code stacks} stacks' worth (0: without end). */
		public long room(ItemStack stack, int stacks) {
			if (stacks <= 0) return Long.MAX_VALUE;
			int max = Math.max(1, stack.getMaxStackSize());
			Entry entry = find(stack);
			long partial = entry == null ? 0 : entry.stacks() * max - entry.count;
			return partial + Math.max(0, stacks - stacksUsed()) * (long) max;
		}

		/** Puts in as much of {@code stack} as fits a space of {@code stacks} (shrinking it by that); returns how many went in. */
		public int insert(ItemStack stack, int stacks) {
			if (stack.isEmpty() || !mayHold(stack)) return 0;
			int moved = (int) Math.min(stack.getCount(), room(stack, stacks));
			if (moved <= 0) return 0;
			Entry entry = find(stack);
			if (entry == null) entries.add(new Entry(stack, moved));
			else entry.count += moved;
			stack.shrink(moved);
			return moved;
		}

		/** Takes up to {@code amount} of the kind {@code like} out (nothing if there is none of it). */
		public ItemStack extract(ItemStack like, int amount) {
			Entry entry = find(like);
			if (entry == null || amount <= 0) return ItemStack.EMPTY;
			int taken = (int) Math.min(amount, entry.count);
			entry.count -= taken;
			if (entry.count <= 0) entries.remove(entry);
			return entry.kind.copyWithCount(taken);
		}

		public long count(ItemStack like) {
			Entry entry = find(like);
			return entry == null ? 0 : entry.count;
		}

		CompoundTag save() {
			ListTag list = new ListTag();
			for (Entry entry : entries) {
				CompoundTag tag = new CompoundTag();
				tag.put("Item", entry.kind.save(new CompoundTag()));
				tag.putLong("Count", entry.count);
				list.add(tag);
			}
			CompoundTag tag = new CompoundTag();
			tag.put("Entries", list);
			return tag;
		}

		static Space load(CompoundTag tag) {
			Space space = new Space();
			ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				CompoundTag entry = list.getCompound(i);
				ItemStack kind = ItemStack.of(entry.getCompound("Item"));
				long count = entry.getLong("Count");
				if (!kind.isEmpty() && count > 0) space.entries.add(new Entry(kind, count));
			}
			return space;
		}
	}

	/**
	 * Whether {@code stack} may go into a space: anything but what folds a space of its own (a packed storage formation, an old Storage
	 * Ring), nor anything carrying one (a shulker box's, a bundle's things: one went in so before the sweep of 2026-10-23).
	 */
	public static boolean mayHold(ItemStack stack) {
		return !(stack.getItem() instanceof StorageRingItem) && StorageFormationItem.spaceOf(stack) == null
				&& (!stack.hasTag() || !carriesSpace(stack.getTag(), 0));
	}

	/** Whether {@code tag} (an item's, or any part of it) holds a space among what it carries, however deep (to a limit). */
	private static boolean carriesSpace(Tag tag, int depth) {
		if (depth > 12) return false;
		if (tag instanceof CompoundTag compound) {
			if (compound.contains("id", Tag.TAG_STRING) && RING_ID.equals(compound.getString("id"))) return true;
			if (compound.hasUUID(StorageFormationItem.SPACE_TAG)) return true;
			for (String key : compound.getAllKeys()) if (carriesSpace(compound.get(key), depth + 1)) return true;
		} else if (tag instanceof ListTag list) {
			for (Tag one : list) if (carriesSpace(one, depth + 1)) return true;
		}
		return false;
	}

	private static final String RING_ID = DefyingTheHeavens.MOD_ID + ":storage_ring";

	private final Map<UUID, Space> spaces = new HashMap<>();

	public static StorageSpaces forServer(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(StorageSpaces::load, StorageSpaces::new, FILE_ID);
	}

	/** The space {@code id} (made empty the first time). */
	public Space space(UUID id) {
		return spaces.computeIfAbsent(id, k -> new Space());
	}

	/** The space {@code id} if it holds anything (an old ring's not yet made: none). */
	public Space existing(UUID id) {
		return id == null ? null : spaces.get(id);
	}

	public static StorageSpaces load(CompoundTag tag) {
		StorageSpaces data = new StorageSpaces();
		CompoundTag all = tag.getCompound("Rings");
		for (String key : all.getAllKeys()) {
			try {
				data.spaces.put(UUID.fromString(key), Space.load(all.getCompound(key)));
			} catch (IllegalArgumentException ignored) {
				// skip a corrupt entry
			}
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		CompoundTag all = new CompoundTag();
		spaces.forEach((id, space) -> {
			if (!space.entries.isEmpty()) all.put(id.toString(), space.save());
		});
		tag.put("Rings", all);
		return tag;
	}

	// --- To the client ---

	/** A space's entries go in packets of at most this many bytes (a space without end can grow past one packet). */
	static final int PACKET_BYTES = 512 * 1024;

	/** Players whose open space changed this tick: sent once at the tick's end ({@link #flush}), however many clicks came. */
	private static final java.util.Set<UUID> PENDING = new java.util.HashSet<>();

	/** {@code player}'s open space is to be sent, at the end of this tick. */
	public static void send(ServerPlayer player) {
		PENDING.add(player.getUUID());
	}

	/** Every server tick: the spaces that changed go to their viewers, once each (a boundless space can be thousands of kinds). */
	public static void flush(MinecraftServer server) {
		if (PENDING.isEmpty()) return;
		for (UUID id : PENDING) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player != null) StorageFormations.sendOpen(player);
		}
		PENDING.clear();
	}
}
