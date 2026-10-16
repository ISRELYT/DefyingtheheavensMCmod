package com.example.defyingtheheavens;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * A Storage Ring's screen: its space above (not slots: a grid of what it holds, any number of each, drawn by the client from
 * what StorageRings sends and clicked through it, StorageRings#act), the player's pack below as real slots. The ring itself,
 * wherever on the player it is, can't be moved while its space is open; and the screen closes if it leaves the player.
 */
public class StorageRingMenu extends AbstractContainerMenu {
	public static final MenuType<StorageRingMenu> TYPE = Registry.register(BuiltInRegistries.MENU, DefyingTheHeavens.id("storage_ring"),
			new MenuType<>(StorageRingMenu::new, FeatureFlags.VANILLA_SET));

	/** Where the space's grid lies in the screen, its columns and the rows shown at once, and the pack below it. */
	public static final int GRID_X = 12, GRID_Y = 38, COLUMNS = 9, ROWS = 5;
	public static final int INVENTORY_X = 12, INVENTORY_Y = GRID_Y + ROWS * 18 + 14;
	/** The screen's size: the grid and its scroll bar across, everything down to the hotbar's foot. */
	public static final int WIDTH = GRID_X + COLUMNS * 18 + 22, HEIGHT = INVENTORY_Y + 58 + 18 + 8;

	/** The ring's id (null on the client, which only draws). */
	private final UUID ringId;

	/** The client's side: the space arrives by its own packets (ClientStorageRing). */
	public StorageRingMenu(int id, Inventory inventory) {
		this(id, inventory, null);
	}

	public StorageRingMenu(int id, Inventory inventory, UUID ringId) {
		super(TYPE, id);
		this.ringId = ringId;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) addSlot(new PackSlot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
		}
		for (int col = 0; col < 9; col++) addSlot(new PackSlot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
	}

	public UUID ringId() { return ringId; }

	/** A slot of the pack: the open ring stays where it is. */
	private class PackSlot extends Slot {
		PackSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPickup(Player player) {
			return !StorageRingItem.isOpen(getItem(), ringId) && super.mayPickup(player);
		}
	}

	@Override
	public void clicked(int slotId, int button, ClickType type, Player player) {
		// The number keys and the off-hand key would carry the open ring off from the hotbar or the off hand.
		if (type == ClickType.SWAP && (button >= 0 && button < 9 && StorageRingItem.isOpen(player.getInventory().getItem(button), ringId)
				|| button == 40 && StorageRingItem.isOpen(player.getOffhandItem(), ringId))) {
			return;
		}
		super.clicked(slotId, button, type, player);
	}

	/** Shift-click on the pack: into the space, as much as fits. */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (player instanceof ServerPlayer sp && slot.hasItem()) {
			StorageRings.putFromPack(sp, this, slot.getItem());
			slot.setChanged();
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return ringId == null || !StorageRings.findOn(player, ringId).isEmpty();
	}

	/** Touching this class registers the menu type. */
	public static void register() {
	}
}
