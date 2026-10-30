package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;

/**
 * Storage formations (the user's ask, 2026-10-28: "remove the storage rings from the game entirely. And let's add storage formations (this
 * will also replace chests in treasuries)... its tier decides the level of the seal on it so enemy players can't break into it without the
 * required realm. Crafting a better tier uses higher grade resources"; and "the immortal storage formation must be infinite").
 * <p>
 * A formation is a block holding a folded space (StorageSpaces) in one of five tiers ({@link Tier}): Low, crafted; each tier after
 * refined in place from its own screen, with the next grade's spirit stones and more, by a cultivator of its seal's realm (and, for the
 * last two, some mastery of formations: CraftMastery). Its owner and the players they trust open it freely. Anyone else needs the seal's
 * realm, and then must force it ({@link #force}): stay by it while the seal gives way, longer the higher the tier, its owner warned when
 * it begins and when it breaks; a broken seal stays open until its owner opens it again, which mends it. Below the seal's realm it can't
 * be opened, and nothing breaks it: no pickaxe, blast, spell or hopper. It stays where it is set down (no portable store: the user,
 * 2026-10-30, so players make bases); its owner takes it up only empty (sneaking, an empty hand: StorageFormationBlock).
 * <p>
 * The treasuries the world raises (sects', clans' hoards, the ghost palaces') hold their riches in formations of no owner: their sect's or
 * king's own rules keep them (CultivationEvents, PalaceTreasury). An old Storage Ring, carried, unfolds into the formation of its size with
 * all it held ({@link #convertRings}).
 */
public final class StorageFormations {
	public enum Tier {
		LOW("low", 27, Realm.FOUNDATION_BUILDING, 10, ChatFormatting.WHITE),
		MID("mid", 54, Realm.CORE_FORMATION, 20, ChatFormatting.GREEN),
		HIGH("high", 81, Realm.HEAVENLY_BEING, 40, ChatFormatting.AQUA),
		SUPREME("supreme", 108, Realm.VOID_SHATTERING, 80, ChatFormatting.LIGHT_PURPLE),
		IMMORTAL("immortal", 0, Realm.TRUE_IMMORTAL, 160, ChatFormatting.GOLD);

		private final String id;
		private final int stacks;
		private final Realm seal;
		private final int forceSeconds;
		private final ChatFormatting colour;

		Tier(String id, int stacks, Realm seal, int forceSeconds, ChatFormatting colour) {
			this.id = id;
			this.stacks = stacks;
			this.seal = seal;
			this.forceSeconds = forceSeconds;
			this.colour = colour;
		}

		public String getId() { return id; }
		/** The stacks' worth it holds, or 0 for no end. */
		public int stacks() { return stacks; }
		public boolean boundless() { return stacks == 0; }
		/** The realm its seal is of: who may force it, and who may refine one into it. */
		public Realm seal() { return seal; }
		/** How long forcing its seal takes, in seconds. */
		public int forceSeconds() { return forceSeconds; }
		public ChatFormatting colour() { return colour; }
		public Tier next() { return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null; }
		public Component getDisplayName() { return Component.translatable(ModLang.storageTier(id)).withStyle(colour); }

		/** What refining a formation into this tier takes from the refiner's pack. */
		public List<ItemStack> cost() {
			return switch (this) {
				case LOW -> List.of();
				case MID -> List.of(new ItemStack(ModItems.MID_SPIRIT_STONE, 16), new ItemStack(ModItems.JADE, 8));
				case HIGH -> List.of(new ItemStack(ModItems.HIGH_SPIRIT_STONE, 16), new ItemStack(Items.SHULKER_SHELL, 4));
				case SUPREME -> List.of(new ItemStack(ModItems.SUPREME_SPIRIT_STONE, 16), new ItemStack(Items.NETHER_STAR, 1));
				case IMMORTAL -> List.of(new ItemStack(ModItems.IMMORTAL_SPIRIT_STONE, 8), new ItemStack(ModItems.IMMORTAL_JADE, 4));
			};
		}

		/** The mastery of formations a refiner needs to make one of this tier (CraftMastery). */
		public CraftMastery.Rank mastery() {
			return switch (this) {
				case SUPREME -> CraftMastery.Rank.ADEPT;
				case IMMORTAL -> CraftMastery.Rank.MASTER;
				default -> CraftMastery.Rank.APPRENTICE;
			};
		}

		/** The practice in formations making one of this tier teaches (crafting a Low one, refining into the rest). */
		public int practice() {
			return switch (this) {
				case LOW -> 4;
				case MID -> 6;
				case HIGH -> 10;
				case SUPREME -> 16;
				case IMMORTAL -> 24;
			};
		}

		public Block block() { return ModBlocks.storageFormation(this); }

		public static Tier byIndex(int index) { return values()[Math.max(0, Math.min(values().length - 1, index))]; }

		public static Tier of(Block block) {
			for (Tier tier : values()) if (tier.block() == block) return tier;
			return null;
		}
	}

	/** Clicks on a space's grid (StorageFormationMenu): what to do with the entry (or, with something carried, the space), or refine. */
	public static final int TAKE_STACK = 0, TAKE_HALF = 1, TAKE_TO_PACK = 2, PUT_ALL = 3, PUT_ONE = 4, REFINE = 5;

	// --- Opening one ---

	/**
	 * {@code player} uses the formation: opens it to its owner, those they trust, and anyone once its seal is broken (its owner opening it
	 * mends it); a stranger of the seal's realm begins forcing it; anyone weaker is told what seal holds it. One without an owner (a
	 * treasury's) opens as a chest did: its keepers' rules decide first (CultivationEvents).
	 */
	public static void use(ServerPlayer player, StorageFormationBlockEntity formation) {
		if (player.isSpectator() || !player.isAlive()) return;
		if (player.isCreative() || formation.getOwner() == null || formation.allows(player)) {
			if (formation.isBroken() && player.getUUID().equals(formation.getOwner())) {
				formation.mend();
				player.displayClientMessage(Component.translatable(ModLang.MSG_STORAGE_MENDED), true);
			}
			open(player, formation);
			return;
		}
		if (formation.isBroken()) {
			open(player, formation);
			return;
		}
		Tier tier = formation.tier();
		PlayerCultivation c = CultivationManager.forPlayer(player);
		if (c.isMortal() || TwoPaths.awakens(c, tier.seal(), false) == null) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_STORAGE_SEALED, tier.seal().getDisplayName()).withStyle(ChatFormatting.RED), true);
			player.level().playSound(null, formation.getBlockPos(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 0.8f, 0.6f);
			return;
		}
		force(player, formation);
	}

	/** Opens {@code formation}'s space to {@code player} (its space made the first time, its laid-up treasures rolled in). */
	public static void open(ServerPlayer player, StorageFormationBlockEntity formation) {
		formation.ensureSpace(player.serverLevel());
		BlockPos pos = formation.getBlockPos();
		// "Immortal Storage Formation", its grade in its colour (the block's own name, "Immortal-Grade Storage Formation", ran past the panel).
		player.openMenu(new net.minecraft.world.SimpleMenuProvider((windowId, inventory, p) -> new StorageFormationMenu(windowId, inventory, pos),
				Component.translatable(ModLang.STORAGE_TITLE, formation.tier().getDisplayName())));
		player.level().playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.6f, 1.6f);
		StorageSpaces.send(player);
	}

	/** The formation the screen {@code player} has open is of, or null (it was taken up, or the screen is another's). */
	static StorageFormationBlockEntity openFormation(ServerPlayer player) {
		if (!(player.containerMenu instanceof StorageFormationMenu menu) || menu.pos() == null) return null;
		return player.level().getBlockEntity(menu.pos()) instanceof StorageFormationBlockEntity formation ? formation : null;
	}

	/** Sends {@code player}'s open space: its tier, whether they own it (Refine and Trust are theirs), whom it trusts, then its entries. */
	static void sendOpen(ServerPlayer player) {
		StorageFormationBlockEntity formation = openFormation(player);
		if (formation == null || formation.spaceId() == null) return;
		StorageSpaces.Space space = StorageSpaces.forServer(player.server).space(formation.spaceId());
		boolean owns = player.getUUID().equals(formation.getOwner()) || player.isCreative() && formation.getOwner() == null;
		ModPackets.sendStorageSpace(player, player.containerMenu.containerId, formation.tier(), owns, formation.getTrusted(), space.entries(),
				StorageSpaces.PACKET_BYTES);
	}

	/**
	 * {@code player}'s click on their open formation's space: {@code action} on the entry of {@code like}'s kind (or the carried stack put
	 * in). Everything is checked against the space as it is now, not as the client last saw it. The space is sent again after.
	 */
	public static void act(ServerPlayer player, int action, ItemStack like) {
		StorageFormationBlockEntity formation = openFormation(player);
		if (formation == null || formation.spaceId() == null) return;
		StorageFormationMenu menu = (StorageFormationMenu) player.containerMenu;
		StorageSpaces.Space space = StorageSpaces.forServer(player.server).space(formation.spaceId());
		int stacks = formation.tier().stacks();
		ItemStack carried = menu.getCarried();
		switch (action) {
			case TAKE_STACK, TAKE_HALF -> {
				if (!carried.isEmpty() || like.isEmpty()) break;
				int stack = Math.max(1, like.getMaxStackSize());
				int amount = (int) Math.min(stack, space.count(like));
				if (action == TAKE_HALF) amount = (amount + 1) / 2;
				menu.setCarried(space.extract(like, amount));
			}
			case TAKE_TO_PACK -> {
				if (like.isEmpty()) break;
				ItemStack taken = space.extract(like, Math.max(1, like.getMaxStackSize()));
				if (taken.isEmpty()) break;
				player.getInventory().add(taken);
				if (!taken.isEmpty()) space.insert(taken, 0); // what didn't fit goes back, whatever the tier
			}
			case PUT_ALL -> space.insert(carried, stacks);
			case PUT_ONE -> {
				if (carried.isEmpty()) break;
				ItemStack one = carried.copyWithCount(1);
				if (space.insert(one, stacks) > 0) carried.shrink(1);
			}
			case REFINE -> refine(player, formation);
			default -> { }
		}
		StorageSpaces.forServer(player.server).setDirty();
		menu.broadcastChanges();
		StorageSpaces.send(player);
		sendViewers(player.server, formation.spaceId());
	}

	/** Shift-click on the pack in the formation's screen: the stack into the space, as much as fits. */
	public static void putFromPack(ServerPlayer player, ItemStack stack) {
		StorageFormationBlockEntity formation = openFormation(player);
		if (formation == null || formation.spaceId() == null) return;
		if (StorageSpaces.forServer(player.server).space(formation.spaceId()).insert(stack, formation.tier().stacks()) > 0) {
			StorageSpaces.forServer(player.server).setDirty();
			sendViewers(player.server, formation.spaceId());
		}
	}

	/** Everyone looking into {@code space} is sent it again at the tick's end (its owner and one they trust may look in at once). */
	static void sendViewers(MinecraftServer server, UUID space) {
		for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
			StorageFormationBlockEntity formation = openFormation(viewer);
			if (formation != null && space.equals(formation.spaceId())) StorageSpaces.send(viewer);
		}
	}

	// --- Refining ---

	/** Why {@code player} can't refine {@code formation} now, or null if they can: not theirs, the last tier, the realm, mastery, the cost. */
	public static Component refineRefusal(Player player, StorageFormationBlockEntity formation) {
		if (!player.getUUID().equals(formation.getOwner()) && !(player.isCreative() && formation.getOwner() == null)) {
			return Component.translatable(ModLang.MSG_STORAGE_NOT_YOURS);
		}
		Tier next = formation.tier().next();
		if (next == null) return Component.translatable(ModLang.MSG_STORAGE_LAST_TIER);
		PlayerCultivation c = CultivationManager.forPlayer(player);
		if (c.isMortal() || TwoPaths.awakens(c, next.seal(), false) == null) {
			return Component.translatable(ModLang.MSG_STORAGE_REALM, next.seal().getDisplayName(), next.getDisplayName());
		}
		PlayerCultivation own = player instanceof ServerPlayer sp ? CultivationManager.own(sp) : c;
		if (CraftMastery.rank(own, CraftMastery.Craft.FORMATIONS).ordinal() < next.mastery().ordinal()) {
			return Component.translatable(ModLang.MSG_STORAGE_MASTERY, next.mastery().getDisplayName(), next.getDisplayName());
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

	/** Refines {@code formation} in place into the next tier, if {@code player} can ({@link #refineRefusal}); tells them either way. */
	public static boolean refine(ServerPlayer player, StorageFormationBlockEntity formation) {
		Component refusal = refineRefusal(player, formation);
		if (refusal != null) {
			player.displayClientMessage(refusal.copy().withStyle(ChatFormatting.RED), true);
			return false;
		}
		Tier next = formation.tier().next();
		if (!player.getAbilities().instabuild) {
			for (ItemStack cost : next.cost()) take(player.getInventory(), cost.getItem(), cost.getCount());
		}
		ServerLevel level = player.serverLevel();
		BlockPos pos = formation.getBlockPos();
		CompoundTag kept = formation.saveWithoutMetadata();
		BlockState state = next.block().defaultBlockState();
		level.setBlock(pos, state, 3);
		if (level.getBlockEntity(pos) instanceof StorageFormationBlockEntity refined) {
			refined.load(kept);
			refined.setChanged();
		}
		CraftMastery.practise(player, CraftMastery.Craft.FORMATIONS, next.practice());
		Milestones.award(player, Milestones.STORAGE_RING);
		player.displayClientMessage(Component.translatable(ModLang.MSG_STORAGE_REFINED, next.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
		level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 0.8f);
		level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 0.7f, 1.2f);
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 40, 0.4, 0.6, 0.4, 0.05);
		// The screen was the old block's: opened again on the new one.
		if (level.getBlockEntity(pos) instanceof StorageFormationBlockEntity refined) open(player, refined);
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

	// --- Forcing a seal ---

	/** How near the formation one forcing it must keep (blocks). */
	public static final double FORCE_REACH = 6;

	/**
	 * {@code player}, of the seal's realm, sets to forcing {@code formation}: its owner warned (if about), the seal giving way while they
	 * stay by it (StorageFormationBlockEntity#tickForcing). Only one forces a seal at a time.
	 */
	public static void force(ServerPlayer player, StorageFormationBlockEntity formation) {
		if (player.getUUID().equals(formation.forcer())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_STORAGE_FORCING, formation.forcePercent()), true);
			return;
		}
		if (formation.forcer() != null) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_STORAGE_FORCED_BY_ANOTHER), true);
			return;
		}
		formation.beginForcing(player);
		player.displayClientMessage(Component.translatable(ModLang.MSG_STORAGE_FORCE_BEGIN, formation.tier().forceSeconds()).withStyle(ChatFormatting.GOLD), true);
		player.level().playSound(null, formation.getBlockPos(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0f, 0.6f);
		warnOwner(player.server, formation, Component.translatable(ModLang.MSG_STORAGE_OWNER_FORCED, player.getDisplayName(), place(formation))
				.withStyle(ChatFormatting.RED));
	}

	/** Tells the formation's owner, if they are about, of what befalls it. */
	static void warnOwner(MinecraftServer server, StorageFormationBlockEntity formation, Component message) {
		if (formation.getOwner() == null) return;
		ServerPlayer owner = server.getPlayerList().getPlayer(formation.getOwner());
		if (owner != null) {
			owner.sendSystemMessage(message);
			owner.playNotifySound(SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0f, 0.7f);
		}
	}

	/** "at 120, 64, -38 in the Overworld" style: where it stands. */
	static Component place(StorageFormationBlockEntity formation) {
		BlockPos pos = formation.getBlockPos();
		return Component.translatable(ModLang.MSG_STORAGE_PLACE, pos.getX(), pos.getY(), pos.getZ());
	}

	// --- The world's treasuries ---

	/** A treasury's formation's tier by the world it stands in: the richer the realm, the stronger the seal. */
	public static Tier treasuryTier(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
		if (ModDimensions.hasImmortalQi(dimension)) return Tier.IMMORTAL;
		if (ModDimensions.isGreatVoid(dimension)) return Tier.SUPREME;
		if (ModDimensions.isUpperRealm(dimension) || ModDimensions.isNetherworld(dimension)) return Tier.HIGH;
		return Tier.MID;
	}

	/**
	 * A treasury's store raised by the world at {@code pos} (a sect's, a clan's hoard, a ghost palace's): a formation of the place's tier,
	 * of no one's (its keepers' rules over it: CultivationEvents, PalaceTreasury), its riches rolled from {@code loot} the first time it
	 * is opened, {@code laidUp} (a clan's blood phial) with them.
	 */
	public static void treasury(net.minecraft.world.level.WorldGenLevel level, BlockPos pos, net.minecraft.resources.ResourceLocation loot, long seed,
			ItemStack laidUp) {
		level.setBlock(pos, treasuryTier(level.getLevel().dimension()).block().defaultBlockState(), 2);
		if (level.getBlockEntity(pos) instanceof StorageFormationBlockEntity formation) {
			formation.layUp(loot, seed);
			formation.layUp(laidUp);
		}
	}

	// --- The old Storage Rings ---

	/** A ring's grade (by the stacks it held) as the formation that holds as much: Mortal 27, Spirit 54, Earth 108, Heaven and Boundless without end. */
	public static Tier tierForRing(int grade) {
		return switch (grade) {
			case 0 -> Tier.LOW;
			case 1 -> Tier.MID;
			case 2 -> Tier.SUPREME;
			default -> Tier.IMMORTAL;
		};
	}

	/**
	 * Every second, for every player: each old Storage Ring they carry (pack, hands, armour, ring slots, ender chest) unfolds into the
	 * storage formation of its size, holding its very space (the same id: nothing moves); they are told.
	 */
	public static void convertRings(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) convertRings(player);
	}

	/** {@code player}'s old rings unfolded (one worn in a ring slot goes to the pack, or at their feet: a formation isn't worn); how many. */
	static int convertRings(ServerPlayer player) {
		int converted = convert(player.getInventory()) + convert(player.getEnderChestInventory());
		RingContainer rings = RingContainer.of(player);
		for (int i = 0; i < rings.getContainerSize(); i++) {
			ItemStack stack = rings.getItem(i);
			if (!(stack.getItem() instanceof StorageRingItem)) continue;
			rings.setItem(i, ItemStack.EMPTY);
			player.getInventory().placeItemBackInInventory(unfold(stack));
			converted++;
		}
		if (converted > 0) {
			player.sendSystemMessage(Component.translatable(ModLang.MSG_STORAGE_RING_UNFOLDED).withStyle(ChatFormatting.GOLD));
			player.inventoryMenu.broadcastChanges();
		}
		return converted;
	}

	/** The rings in {@code container} turned to formations; how many. */
	static int convert(Container container) {
		int converted = 0;
		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack stack = container.getItem(i);
			if (!(stack.getItem() instanceof StorageRingItem)) continue;
			container.setItem(i, unfold(stack));
			converted++;
		}
		return converted;
	}

	/** An old ring as the formation of its size, its space the ring's (if it ever had one). */
	public static ItemStack unfold(ItemStack ring) {
		ItemStack formation = StorageFormationItem.of(tierForRing(StorageRingItem.gradeIndex(ring)));
		UUID space = StorageRingItem.idOf(ring);
		if (space != null) StorageFormationItem.setSpace(formation, space);
		return formation;
	}

	private StorageFormations() {}
}
