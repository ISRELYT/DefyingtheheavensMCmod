package com.example.defyingtheheavens;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Overseer Core's screen (OverseerCoreBlockEntity; the client's OverseerScreen): its nine diagrams in a 3 x 3, the player's inventory
 * below; the screen draws the chosen diagram's board with its wear from what is written on it (synced with the slot).
 */
public class OverseerMenu extends AbstractContainerMenu {
	public static final MenuType<OverseerMenu> TYPE = Registry.register(BuiltInRegistries.MENU, DefyingTheHeavens.id("overseer_core"),
			new MenuType<>(OverseerMenu::new, FeatureFlags.VANILLA_SET));

	/** Where its slots stand on its screen: the diagrams' 3 x 3; the inventory, its hotbar 58 below. */
	public static final int DIAGRAMS_X = 16, DIAGRAMS_Y = 30, INVENTORY_X = 69, INVENTORY_Y = 140;

	private final Container core;

	/** The client's side: the slots arrive from the server. */
	public OverseerMenu(int id, Inventory inventory) {
		this(id, inventory, new SimpleContainer(OverseerCoreBlockEntity.SLOTS));
	}

	public OverseerMenu(int id, Inventory inventory, Container core) {
		super(TYPE, id);
		checkContainerSize(core, OverseerCoreBlockEntity.SLOTS);
		this.core = core;
		core.startOpen(inventory.player);
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				addSlot(new Slot(core, row * 3 + col, DIAGRAMS_X + col * 18, DIAGRAMS_Y + row * 18) {
					@Override
					public boolean mayPlace(ItemStack stack) {
						return stack.getItem() instanceof CircuitDiagramItem;
					}

					@Override
					public int getMaxStackSize() {
						return 1;
					}
				});
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
		}
		for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
	}

	@Override
	public boolean stillValid(Player player) { return core.stillValid(player); }

	@Override
	public void removed(Player player) {
		super.removed(player);
		core.stopOpen(player);
	}

	/** Shift-click: a diagram from the core into the inventory, or from the inventory into the core. */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		int own = OverseerCoreBlockEntity.SLOTS;
		if (index < own) {
			if (!moveItemStackTo(stack, own, slots.size(), true)) return ItemStack.EMPTY;
		} else if (!(stack.getItem() instanceof CircuitDiagramItem) || !moveItemStackTo(stack, 0, own, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		return copy;
	}

	/** Touching this class registers the menu type. */
	public static void register() {
	}
}
