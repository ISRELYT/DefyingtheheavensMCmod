package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * What an Overseer Core keeps (OverseerCoreBlock; puppet phase 3): {@link #SLOTS} Circuit Diagrams of its keeper's. Every puppet of theirs
 * is watched wherever it fights (PuppetEntity: each cut line, each overheating, kept by its design in Puppetry.Ledger), and on each diagram
 * here the Core writes its design's wear (CircuitDiagramItem#setWear: how often each cell was cut, which ran hot) every
 * {@link #WRITE_TICKS} and as it is opened: its screen draws it, and the diagram carries it to the bench, where its ghost shows it.
 */
public class OverseerCoreBlockEntity extends BaseContainerBlockEntity {
	public static final int SLOTS = 9;
	private static final int WRITE_TICKS = 40;

	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private @Nullable UUID owner;
	private String ownerName = "";
	private int ticks;

	public OverseerCoreBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.OVERSEER_CORE, pos, state);
	}

	public @Nullable UUID owner() { return owner; }
	public String ownerName() { return ownerName; }

	public void setOwner(ServerPlayer player) {
		owner = player.getUUID();
		ownerName = player.getGameProfile().getName();
		setChanged();
	}

	/** Its keeper, or one in creative; anyone, if it has no keeper yet. */
	public boolean mayUse(Player player) {
		return owner == null || owner.equals(player.getUUID()) || player.getAbilities().instabuild;
	}

	void tick() {
		if (++ticks % WRITE_TICKS == 0) writeWear();
	}

	/** Each diagram here given its design's wear in its keeper's puppets' fights, as it stands now. */
	public void writeWear() {
		if (owner == null || !(level instanceof ServerLevel server)) return;
		Puppetry.Ledger ledger = Puppetry.Ledger.get(server.getServer());
		boolean changed = false;
		for (ItemStack stack : items) {
			if (!(stack.getItem() instanceof CircuitDiagramItem)) continue;
			changed |= CircuitDiagramItem.setWear(stack, ledger.wearOf(owner, CircuitDiagramItem.design(stack)));
		}
		if (changed) setChanged();
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable(ModLang.OVERSEER_TITLE);
	}

	@Override
	protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new OverseerMenu(id, inventory, this);
	}

	@Override
	public int getContainerSize() { return SLOTS; }

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : items) if (!stack.isEmpty()) return false;
		return true;
	}

	@Override
	public ItemStack getItem(int slot) { return items.get(slot); }

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack taken = ContainerHelper.removeItem(items, slot, amount);
		if (!taken.isEmpty()) setChanged();
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
		setChanged();
		writeWear(); // a diagram laid in shows its wear at once
	}

	@Override
	public int getMaxStackSize() { return 1; }

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return stack.getItem() instanceof CircuitDiagramItem;
	}

	@Override
	public boolean stillValid(Player player) { return net.minecraft.world.Container.stillValidBlockEntity(this, player) && mayUse(player); }

	@Override
	public void clearContent() {
		items.clear();
		setChanged();
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		ContainerHelper.saveAllItems(tag, items);
		if (owner != null) tag.putUUID("Owner", owner);
		tag.putString("OwnerName", ownerName);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, items);
		owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
		ownerName = tag.getString("OwnerName");
	}
}
