package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Heaven Altar's offering tray: nine slots, laid out in a ring on its own screen (HeavenAltarMenu). Whatever lies in it
 * when a Ritual to the Heavens begins is taken by the heavens (see {@link HeavenRitual}); until then it is the player's.
 */
public class HeavenAltarBlockEntity extends BaseContainerBlockEntity {
	public static final int SLOTS = 9;
	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

	public HeavenAltarBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.HEAVEN_ALTAR, pos, state);
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable(ModLang.ALTAR_TRAY);
	}

	/** The altar's own screen (HeavenAltarMenu): the offerings in a ring, and how the heavens would weigh a ritual here. */
	@Override
	protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new HeavenAltarMenu(id, inventory, this, inventory.player instanceof ServerPlayer player
				? HeavenAltarMenu.judge(player, worldPosition) : new SimpleContainerData(HeavenAltarMenu.COUNT));
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
	}

	@Override
	public boolean stillValid(Player player) { return net.minecraft.world.Container.stillValidBlockEntity(this, player); }

	@Override
	public void clearContent() {
		items.clear();
		setChanged();
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		ContainerHelper.saveAllItems(tag, items);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, items);
	}
}
