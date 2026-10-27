package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The space inside every Storage Ring (StorageRingItem), kept by the world, not the ring: each ring carries only its id and its
 * grade, so its contents go wherever it goes, to whoever holds it (a fallen cultivator's ring is the victor's), and a ring lost
 * is a space lost. A space holds its things as entries of one kind each, any number of them; how much fits is the grade's
 * ({@link Grade}): so many stacks' worth (each kind taking whole stacks, as in a chest), until the last grade, which holds
 * without end. Refining a ring to the next grade ({@link #refine}) takes spirit stones and treasures, and a cultivator of the
 * realm to fix the larger space.
 */
public final class StorageRings extends SavedData {
	private static final String FILE_ID = "defying_the_heavens_storage_rings";

	/** A ring's grades: the stacks it holds (0: without end), what refining a ring of the grade before into it takes, and the realm that can. */
	public enum Grade {
		MORTAL("mortal", 27, null, ChatFormatting.WHITE),
		SPIRIT("spirit", 54, Realm.FOUNDATION_BUILDING, ChatFormatting.GREEN),
		EARTH("earth", 108, Realm.CORE_FORMATION, ChatFormatting.AQUA),
		HEAVEN("heaven", 216, Realm.NASCENT_SOUL, ChatFormatting.LIGHT_PURPLE),
		BOUNDLESS("boundless", 0, Realm.HEAVENLY_BEING, ChatFormatting.GOLD);

		private final String id;
		private final int stacks;
		private final Realm realm;
		private final ChatFormatting colour;

		Grade(String id, int stacks, Realm realm, ChatFormatting colour) {
			this.id = id;
			this.stacks = stacks;
			this.realm = realm;
			this.colour = colour;
		}

		public String getId() { return id; }
		/** The stacks' worth it holds, or 0 for no end. */
		public int stacks() { return stacks; }
		public boolean boundless() { return stacks == 0; }
		/** The realm a cultivator needs to refine a ring into this grade (null for the first, crafted). */
		public Realm realm() { return realm; }
		public ChatFormatting colour() { return colour; }
		public Grade next() { return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null; }
		public Component getDisplayName() { return Component.translatable(ModLang.storageGradeKey(id)).withStyle(colour); }

		/** What refining a ring into this grade takes from the refiner's inventory. */
		public List<ItemStack> cost() {
			return switch (this) {
				case MORTAL -> List.of();
				case SPIRIT -> List.of(new ItemStack(ModItems.LOW_SPIRIT_STONE, 16), new ItemStack(Items.ENDER_PEARL, 4));
				case EARTH -> List.of(new ItemStack(ModItems.MID_SPIRIT_STONE, 16), new ItemStack(ModItems.JADE, 8));
				case HEAVEN -> List.of(new ItemStack(ModItems.HIGH_SPIRIT_STONE, 16), new ItemStack(Items.SHULKER_SHELL, 2));
				case BOUNDLESS -> List.of(new ItemStack(ModItems.SUPREME_SPIRIT_STONE, 8), new ItemStack(ModItems.IMMORTAL_SPIRIT_STONE, 1),
						new ItemStack(Items.NETHER_STAR, 1));
			};
		}

		public static Grade byIndex(int index) { return values()[Math.max(0, Math.min(values().length - 1, index))]; }
	}

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

	/** One ring's space: its entries, in the order they first came in. */
	public static final class Space {
		final List<Entry> entries = new ArrayList<>();

		public List<Entry> entries() { return entries; }

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

		/** How many of {@code stack}'s kind still fit in a space of {@code grade}. */
		public long room(ItemStack stack, Grade grade) {
			if (grade.boundless()) return Long.MAX_VALUE;
			int max = Math.max(1, stack.getMaxStackSize());
			Entry entry = find(stack);
			long partial = entry == null ? 0 : entry.stacks() * max - entry.count;
			return partial + Math.max(0, grade.stacks() - stacksUsed()) * (long) max;
		}

		/** Puts in as much of {@code stack} as fits (shrinking it by that); returns how many went in. */
		public int insert(ItemStack stack, Grade grade) {
			if (stack.isEmpty() || !mayHold(stack)) return 0;
			int moved = (int) Math.min(stack.getCount(), room(stack, grade));
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
	 * Whether {@code stack} may go into a ring's space: anything but another Storage Ring (no space folded inside another), nor anything
	 * carrying one (a shulker box's, a bundle's things: one went in so before the sweep of 2026-10-23).
	 */
	public static boolean mayHold(ItemStack stack) {
		return !(stack.getItem() instanceof StorageRingItem) && (!stack.hasTag() || !carriesRing(stack.getTag(), 0));
	}

	/** Whether {@code tag} (an item's, or any part of it) holds a Storage Ring among what it carries, however deep (to a limit). */
	private static boolean carriesRing(Tag tag, int depth) {
		if (depth > 12) return false;
		if (tag instanceof CompoundTag compound) {
			if (compound.contains("id", Tag.TAG_STRING) && RING_ID.equals(compound.getString("id"))) return true;
			for (String key : compound.getAllKeys()) if (carriesRing(compound.get(key), depth + 1)) return true;
		} else if (tag instanceof ListTag list) {
			for (Tag one : list) if (carriesRing(one, depth + 1)) return true;
		}
		return false;
	}

	private static final String RING_ID = DefyingTheHeavens.MOD_ID + ":storage_ring";

	private final Map<UUID, Space> spaces = new HashMap<>();

	public static StorageRings forServer(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(StorageRings::load, StorageRings::new, FILE_ID);
	}

	/** The space of the ring {@code id} (made empty the first time). */
	public Space space(UUID id) {
		return spaces.computeIfAbsent(id, k -> new Space());
	}

	public static StorageRings load(CompoundTag tag) {
		StorageRings data = new StorageRings();
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

	// --- Finding a ring on a player ---

	/** The Storage Ring {@code id} somewhere on {@code player} (the pack, the hands, the armour, the ring slots), or empty. */
	public static ItemStack findOn(Player player, UUID id) {
		if (id == null) return ItemStack.EMPTY;
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (id.equals(StorageRingItem.idOf(inventory.getItem(i)))) return inventory.getItem(i);
		}
		RingContainer rings = RingContainer.of(player);
		for (int i = 0; i < rings.getContainerSize(); i++) {
			if (id.equals(StorageRingItem.idOf(rings.getItem(i)))) return rings.getItem(i);
		}
		return ItemStack.EMPTY;
	}

	/** The first Storage Ring on {@code player}: one worn, else in the hands, else anywhere in the pack; or empty. */
	public static ItemStack firstOn(Player player) {
		RingContainer rings = RingContainer.of(player);
		for (int i = 0; i < rings.getContainerSize(); i++) if (rings.getItem(i).getItem() instanceof StorageRingItem) return rings.getItem(i);
		if (player.getMainHandItem().getItem() instanceof StorageRingItem) return player.getMainHandItem();
		if (player.getOffhandItem().getItem() instanceof StorageRingItem) return player.getOffhandItem();
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) if (inventory.getItem(i).getItem() instanceof StorageRingItem) return inventory.getItem(i);
		return ItemStack.EMPTY;
	}

	/** Opens {@code ring}'s space to {@code player} (its id given it the first time). */
	public static void open(ServerPlayer player, ItemStack ring) {
		if (!(ring.getItem() instanceof StorageRingItem) || player.isSpectator() || !player.isAlive()) return;
		UUID id = StorageRingItem.ensureId(ring);
		player.openMenu(new net.minecraft.world.SimpleMenuProvider((windowId, inventory, p) -> new StorageRingMenu(windowId, inventory, id),
				ring.getHoverName()));
		player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6f, 1.6f);
		send(player);
	}

	/** The key: opens the first Storage Ring on {@code player}, if any. */
	public static void openFirst(ServerPlayer player) {
		ItemStack ring = firstOn(player);
		if (ring.isEmpty()) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_STORAGE_NO_RING), true);
			return;
		}
		open(player, ring);
	}

	// --- What the screen asks ---

	/** Clicks on the space's grid (StorageRingMenu): what to do with the entry (or, with something carried, the space). */
	public static final int TAKE_STACK = 0, TAKE_HALF = 1, TAKE_TO_PACK = 2, PUT_ALL = 3, PUT_ONE = 4, REFINE = 5;

	/**
	 * {@code player}'s click on their open ring's space: {@code action} on the entry of {@code like}'s kind (or the carried stack put
	 * in). Everything is checked against the space as it is now, not as the client last saw it. The space is sent again after.
	 */
	public static void act(ServerPlayer player, int action, ItemStack like) {
		if (!(player.containerMenu instanceof StorageRingMenu menu)) return;
		ItemStack ring = findOn(player, menu.ringId());
		if (ring.isEmpty()) return;
		Space space = forServer(player.server).space(menu.ringId());
		Grade grade = StorageRingItem.grade(ring);
		ItemStack carried = menu.getCarried();
		switch (action) {
			case TAKE_STACK, TAKE_HALF -> {
				if (!carried.isEmpty() || like.isEmpty()) break;
				int stack = Math.max(1, like.getMaxStackSize());
				long have = space.count(like);
				int amount = (int) Math.min(stack, have);
				if (action == TAKE_HALF) amount = (amount + 1) / 2;
				menu.setCarried(space.extract(like, amount));
			}
			case TAKE_TO_PACK -> {
				if (like.isEmpty()) break;
				ItemStack taken = space.extract(like, Math.max(1, like.getMaxStackSize()));
				if (taken.isEmpty()) break;
				player.getInventory().add(taken);
				if (!taken.isEmpty()) space.insert(taken, Grade.BOUNDLESS); // what didn't fit goes back, whatever the grade
			}
			case PUT_ALL -> space.insert(carried, grade);
			case PUT_ONE -> {
				if (carried.isEmpty()) break;
				ItemStack one = carried.copyWithCount(1);
				if (space.insert(one, grade) > 0) carried.shrink(1);
			}
			case REFINE -> refine(player, ring);
			default -> { }
		}
		forServer(player.server).setDirty();
		menu.broadcastChanges();
		send(player);
	}

	/** Shift-click on the pack in the ring's screen: the stack into the space, as much as fits. */
	public static void putFromPack(ServerPlayer player, StorageRingMenu menu, ItemStack stack) {
		ItemStack ring = findOn(player, menu.ringId());
		if (ring.isEmpty() || StorageRingItem.isOpen(stack, menu.ringId())) return;
		if (forServer(player.server).space(menu.ringId()).insert(stack, StorageRingItem.grade(ring)) > 0) {
			forServer(player.server).setDirty();
			send(player);
		}
	}

	/** Why {@code player} can't refine {@code ring} now, or null if they can: the last grade, the realm, the cost. */
	public static Component refineRefusal(Player player, ItemStack ring) {
		Grade next = StorageRingItem.grade(ring).next();
		if (next == null) return Component.translatable(ModLang.MSG_STORAGE_LAST_GRADE);
		PlayerCultivation c = CultivationManager.forPlayer(player);
		// Of the realm on any path (TwoPaths#awakens: the heaven path's alone before the sweep of 2026-10-23).
		if (c.isMortal() || TwoPaths.awakens(c, next.realm(), false) == null) {
			return Component.translatable(ModLang.MSG_STORAGE_REALM, next.realm().getDisplayName(), next.getDisplayName());
		}
		if (!player.getAbilities().instabuild) {
			for (ItemStack cost : next.cost()) {
				if (player.getInventory().countItem(cost.getItem()) < cost.getCount()) {
					return Component.translatable(ModLang.MSG_STORAGE_COST, cost.getCount(), cost.getHoverName());
				}
			}
		}
		return null;
	}

	/** Refines {@code ring} into the next grade, if {@code player} can ({@link #refineRefusal}); tells them either way. */
	public static boolean refine(ServerPlayer player, ItemStack ring) {
		Component refusal = refineRefusal(player, ring);
		if (refusal != null) {
			player.displayClientMessage(refusal.copy().withStyle(ChatFormatting.RED), true);
			return false;
		}
		Grade next = StorageRingItem.grade(ring).next();
		if (!player.getAbilities().instabuild) {
			for (ItemStack cost : next.cost()) take(player.getInventory(), cost.getItem(), cost.getCount());
		}
		StorageRingItem.setGrade(ring, next);
		Milestones.award(player, Milestones.STORAGE_RING);
		player.displayClientMessage(Component.translatable(ModLang.MSG_STORAGE_REFINED, next.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
		player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 0.8f);
		player.level().playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.PLAYERS, 0.7f, 1.2f);
		player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(),
				40, 0.4, 0.6, 0.4, 0.05);
		return true;
	}

	private static void take(Inventory inventory, Item item, int count) {
		for (int i = 0; i < inventory.getContainerSize() && count > 0; i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.is(item)) continue;
			int taken = Math.min(count, stack.getCount());
			stack.shrink(taken);
			count -= taken;
		}
	}

	// --- To the client ---

	/** The space's entries go in packets of at most this many bytes (a space without end can grow past one packet). */
	private static final int PACKET_BYTES = 512 * 1024;

	/** Players whose open ring's space changed this tick: sent once at the tick's end ({@link #flush}), however many clicks came. */
	private static final java.util.Set<UUID> PENDING = new java.util.HashSet<>();

	/** {@code player}'s open ring's space is to be sent, at the end of this tick. */
	public static void send(ServerPlayer player) {
		PENDING.add(player.getUUID());
	}

	/** Every server tick: the spaces that changed go to their viewers, once each (a boundless space can be thousands of kinds). */
	public static void flush(MinecraftServer server) {
		if (PENDING.isEmpty()) return;
		for (UUID id : PENDING) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player != null) sendNow(player);
		}
		PENDING.clear();
	}

	/** Sends {@code player}'s open ring's space: its grade, then its entries, in as many packets as it takes. */
	private static void sendNow(ServerPlayer player) {
		if (!(player.containerMenu instanceof StorageRingMenu menu)) return;
		ItemStack ring = findOn(player, menu.ringId());
		if (ring.isEmpty()) return;
		Space space = forServer(player.server).space(menu.ringId());
		ModPackets.sendStorageRing(player, menu.containerId, StorageRingItem.grade(ring), space.entries(), PACKET_BYTES);
	}
}
