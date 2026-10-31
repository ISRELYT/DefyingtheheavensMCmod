package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A storage formation's own (StorageFormations): the id of the space it holds (StorageSpaces; made the first time it is opened), its
 * owner and the players they trust, the treasures laid up in it by the world (a loot table and what was put in before, rolled in the
 * first time it is opened), and its seal: one forcing it and how far, and whether it is broken.
 */
public class StorageFormationBlockEntity extends BlockEntity {
	/** Most players an owner can trust with one. */
	public static final int MAX_TRUSTED = 16;

	private UUID space;
	private UUID owner;
	private String ownerName = "";
	private final Map<UUID, String> trusted = new LinkedHashMap<>();
	/** Laid up by the world, rolled into the space the first time it is opened. */
	private ResourceLocation lootTable;
	private long lootSeed;
	private final List<ItemStack> laidUp = new ArrayList<>();
	/** The seal: broken (open to all until its owner mends it), or being forced by {@code forcer} this many ticks. */
	private boolean broken;
	private UUID forcer;
	private int forceTicks;

	public StorageFormationBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.STORAGE_FORMATION, pos, state);
	}

	public StorageFormations.Tier tier() {
		StorageFormations.Tier tier = StorageFormations.Tier.of(getBlockState().getBlock());
		return tier == null ? StorageFormations.Tier.LOW : tier;
	}

	public UUID spaceId() { return space; }
	public UUID getOwner() { return owner; }
	public String getOwnerName() { return ownerName; }
	public Map<UUID, String> getTrusted() { return Collections.unmodifiableMap(trusted); }
	public boolean isBroken() { return broken; }
	public UUID forcer() { return forcer; }

	/** Set down by {@code player} (from an item that may carry a space already: StorageFormationItem). */
	public void placedBy(Player player, ItemStack stack) {
		owner = player.getUUID();
		ownerName = player.getGameProfile().getName();
		UUID carried = StorageFormationItem.spaceOf(stack);
		if (carried != null) space = carried;
		setChanged();
	}

	/** Whether it holds nothing: its space empty (or never made), and nothing the world laid up in it still to roll in. */
	public boolean holdsNothing(MinecraftServer server) {
		if (lootTable != null || !laidUp.isEmpty()) return false;
		StorageSpaces.Space held = StorageSpaces.forServer(server).existing(space);
		return held == null || held.isEmpty();
	}

	/** Whether {@code player} opens it as its own: its owner, or one they trust. */
	public boolean allows(Player player) {
		return player.getUUID().equals(owner) || trusted.containsKey(player.getUUID());
	}

	/** The space made, if it had none, and the world's treasures laid up in it rolled in (once). */
	public void ensureSpace(ServerLevel level) {
		boolean changed = false;
		if (space == null) {
			space = UUID.randomUUID();
			changed = true;
		}
		StorageSpaces spaces = StorageSpaces.forServer(level.getServer());
		StorageSpaces.Space held = spaces.space(space);
		if (!laidUp.isEmpty()) {
			for (ItemStack stack : laidUp) held.insert(stack.copy(), 0);
			laidUp.clear();
			changed = true;
		}
		if (lootTable != null) {
			LootTable table = level.getServer().getLootData().getLootTable(lootTable);
			LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
					.create(LootContextParamSets.CHEST);
			for (ItemStack stack : table.getRandomItems(params, lootSeed)) held.insert(stack, 0);
			lootTable = null;
			changed = true;
		}
		if (changed) {
			spaces.setDirty();
			setChanged();
		}
	}

	/** The world lays treasures up in it (a treasury's): rolled from {@code table} the first time it is opened. */
	public void layUp(ResourceLocation table, long seed) {
		lootTable = table;
		lootSeed = seed;
		setChanged();
	}

	/** A treasure laid up in it before its loot is named (a clan's blood phial: DemonBlood#hoarded). */
	public void layUp(ItemStack stack) {
		if (!stack.isEmpty()) laidUp.add(stack.copy());
		setChanged();
	}

	/** Whose a secret realm's vaults are (HerbValleyWorks): the ancients', no one alive; their seal forced as anyone's is. */
	public static final UUID ANCIENTS = UUID.fromString("a17c1e57-5ea1-4d0e-b5e7-000000a7c1e5");

	/**
	 * Sealed anew for the ancients (a secret realm's vault, renewed each time its realm stirs): whatever is left in it taken away, {@code treasures}
	 * laid in it, its seal whole again.
	 */
	public void sealForAncients(ServerLevel level, List<ItemStack> treasures) {
		owner = ANCIENTS;
		ownerName = "";
		trusted.clear();
		broken = false;
		forcer = null;
		forceTicks = 0;
		lootTable = null;
		laidUp.clear();
		ensureSpace(level);
		StorageSpaces spaces = StorageSpaces.forServer(level.getServer());
		StorageSpaces.Space held = spaces.space(space);
		held.entries().clear();
		for (ItemStack stack : treasures) held.insert(stack.copy(), 0);
		spaces.setDirty();
		setChanged();
	}

	// --- Trust ---

	/** From the screen: trust the player called {@code name} (online now, or seen on this server before). */
	public void trustByName(ServerPlayer by, String name) {
		if (!by.getUUID().equals(owner) || name == null || name.isBlank()) return;
		com.mojang.authlib.GameProfile profile = by.server.getProfileCache() == null ? null
				: by.server.getProfileCache().get(name.trim()).orElse(null);
		if (profile == null) {
			by.displayClientMessage(Component.translatable(ModLang.MSG_TRUST_UNKNOWN, name.trim()), true);
			return;
		}
		if (profile.getId().equals(owner)) {
			by.displayClientMessage(Component.translatable(ModLang.MSG_TRUST_SELF), true);
			return;
		}
		if (!trusted.containsKey(profile.getId()) && trusted.size() >= MAX_TRUSTED) {
			by.displayClientMessage(Component.translatable(ModLang.MSG_TRUST_FULL, MAX_TRUSTED), true);
			return;
		}
		trusted.put(profile.getId(), profile.getName());
		setChanged();
		by.displayClientMessage(Component.translatable(ModLang.MSG_TRUST_ADDED, profile.getName()), true);
	}

	/** From the screen: no longer trusts {@code player}. */
	public void distrust(ServerPlayer by, UUID player) {
		if (!by.getUUID().equals(owner)) return;
		String name = trusted.remove(player);
		if (name != null) {
			setChanged();
			by.displayClientMessage(Component.translatable(ModLang.MSG_TRUST_REMOVED, name), true);
		}
	}

	/** Trusts {@code player} (the tests). */
	public void trust(UUID player, String name) {
		trusted.put(player, name);
		setChanged();
	}

	// --- The seal ---

	public void beginForcing(ServerPlayer player) {
		forcer = player.getUUID();
		forceTicks = 0;
		setChanged();
	}

	/** How far the one forcing it has got, in percent. */
	public int forcePercent() {
		return Math.min(100, forceTicks * 100 / Math.max(1, tier().forceSeconds() * 20));
	}

	/** Its owner opens it: the seal whole again. */
	public void mend() {
		broken = false;
		forcer = null;
		forceTicks = 0;
		setChanged();
	}

	/** Breaks the seal now (the tests; a forcing done). */
	public void breakSeal() {
		broken = true;
		forcer = null;
		forceTicks = 0;
		setChanged();
	}

	/**
	 * Each tick while one forces it: on while they stay by it, alive (their progress on their action bar now and then); given up if they
	 * go; the seal broken when the time is up, its owner told.
	 */
	public static void serverTick(Level level, BlockPos pos, BlockState state, StorageFormationBlockEntity formation) {
		if (formation.forcer == null || !(level instanceof ServerLevel server)) return;
		ServerPlayer player = server.getServer().getPlayerList().getPlayer(formation.forcer);
		if (player == null || !player.isAlive() || player.level() != level
				|| player.distanceToSqr(Vec3.atCenterOf(pos)) > StorageFormations.FORCE_REACH * StorageFormations.FORCE_REACH) {
			if (player != null) player.displayClientMessage(Component.translatable(ModLang.MSG_STORAGE_FORCE_ABANDONED), true);
			formation.forcer = null;
			formation.forceTicks = 0;
			formation.setChanged();
			return;
		}
		formation.forceTicks++;
		if (formation.forceTicks % 10 == 0) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_STORAGE_FORCING, formation.forcePercent()), true);
			server.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.4);
		}
		if (formation.forceTicks >= formation.tier().forceSeconds() * 20) {
			formation.breakSeal();
			player.displayClientMessage(Component.translatable(ModLang.MSG_STORAGE_FORCE_DONE).withStyle(ChatFormatting.GOLD), true);
			server.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.2f, 0.6f);
			StorageFormations.warnOwner(server.getServer(), formation, Component.translatable(ModLang.MSG_STORAGE_OWNER_BROKEN, player.getDisplayName(),
					StorageFormations.place(formation)).withStyle(ChatFormatting.RED));
		}
	}

	// --- Saving ---

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (space != null) tag.putUUID("Space", space);
		if (owner != null) {
			tag.putUUID("Owner", owner);
			tag.putString("OwnerName", ownerName);
		}
		if (!trusted.isEmpty()) {
			ListTag list = new ListTag();
			trusted.forEach((id, name) -> {
				CompoundTag one = new CompoundTag();
				one.putUUID("Id", id);
				one.putString("Name", name);
				list.add(one);
			});
			tag.put("Trusted", list);
		}
		if (lootTable != null) {
			tag.putString("LootTable", lootTable.toString());
			tag.putLong("LootSeed", lootSeed);
		}
		if (!laidUp.isEmpty()) {
			ListTag list = new ListTag();
			for (ItemStack stack : laidUp) list.add(stack.save(new CompoundTag()));
			tag.put("LaidUp", list);
		}
		if (broken) tag.putBoolean("Broken", true);
		if (forcer != null) {
			tag.putUUID("Forcer", forcer);
			tag.putInt("ForceTicks", forceTicks);
		}
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		space = tag.hasUUID("Space") ? tag.getUUID("Space") : null;
		owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
		ownerName = tag.getString("OwnerName");
		trusted.clear();
		ListTag list = tag.getList("Trusted", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag one = list.getCompound(i);
			if (one.hasUUID("Id")) trusted.put(one.getUUID("Id"), one.getString("Name"));
		}
		lootTable = tag.contains("LootTable") ? ResourceLocation.tryParse(tag.getString("LootTable")) : null;
		lootSeed = tag.getLong("LootSeed");
		laidUp.clear();
		ListTag laid = tag.getList("LaidUp", Tag.TAG_COMPOUND);
		for (int i = 0; i < laid.size(); i++) {
			ItemStack stack = ItemStack.of(laid.getCompound(i));
			if (!stack.isEmpty()) laidUp.add(stack);
		}
		broken = tag.getBoolean("Broken");
		forcer = tag.hasUUID("Forcer") ? tag.getUUID("Forcer") : null;
		forceTicks = tag.getInt("ForceTicks");
	}
}
