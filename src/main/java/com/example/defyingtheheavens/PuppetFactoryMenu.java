package com.example.defyingtheheavens;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Puppet Factory's screen (PuppetFactoryBlockEntity; the client's PuppetFactoryScreen): its diagram's slot, its matter's nine, its
 * cores' four, the player's inventory below; its mode, its copy's progress, why it waits and how many it built (synced); the mode button
 * pressed as a menu button ({@link #clickMenuButton}).
 */
public class PuppetFactoryMenu extends AbstractContainerMenu {
	public static final MenuType<PuppetFactoryMenu> TYPE = Registry.register(BuiltInRegistries.MENU, DefyingTheHeavens.id("puppet_factory"),
			new MenuType<>(PuppetFactoryMenu::new, FeatureFlags.VANILLA_SET));

	/** Where its slots stand on its screen: the diagram's, the matter's 3 x 3, the cores' row; the inventory, its hotbar 58 below. */
	public static final int DIAGRAM_X = 20, DIAGRAM_Y = 48, MATTER_X = 60, MATTER_Y = 32, CORES_X = 42, CORES_Y = 98, INVENTORY_X = 69, INVENTORY_Y = 134;

	private final Container factory;
	private final ContainerData data;

	/** The client's side: the slots and values arrive from the server. */
	public PuppetFactoryMenu(int id, Inventory inventory) {
		this(id, inventory, new SimpleContainer(PuppetFactoryBlockEntity.SLOTS), new SimpleContainerData(PuppetFactoryBlockEntity.DATA));
	}

	public PuppetFactoryMenu(int id, Inventory inventory, Container factory, ContainerData data) {
		super(TYPE, id);
		checkContainerSize(factory, PuppetFactoryBlockEntity.SLOTS);
		checkContainerDataCount(data, PuppetFactoryBlockEntity.DATA);
		this.factory = factory;
		this.data = data;
		factory.startOpen(inventory.player);
		addSlot(new Kept(factory, PuppetFactoryBlockEntity.DIAGRAM, DIAGRAM_X, DIAGRAM_Y, 1));
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				addSlot(new Kept(factory, PuppetFactoryBlockEntity.MATTER + row * 3 + col, MATTER_X + col * 18, MATTER_Y + row * 18, 64));
			}
		}
		for (int k = 0; k < PuppetFactoryBlockEntity.CORE_SLOTS; k++) {
			addSlot(new Kept(factory, PuppetFactoryBlockEntity.CORES + k, CORES_X + k * 18, CORES_Y, 64));
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
		}
		for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
		addDataSlots(data);
	}

	/** A slot of the factory's: only what its container takes there (Container#canPlaceItem), so many at most. */
	private static final class Kept extends Slot {
		private final int most;

		Kept(Container container, int index, int x, int y, int most) {
			super(container, index, x, y);
			this.most = most;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return container.canPlaceItem(getContainerSlot(), stack);
		}

		@Override
		public int getMaxStackSize() {
			return most;
		}
	}

	public int get(int index) { return data.get(index); }

	/** The mode button (0): building copies, or repair only, its maker's to switch. */
	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id != 0 || !(factory instanceof PuppetFactoryBlockEntity works) || !works.mayUse(player)) return false;
		works.toggleMode();
		return true;
	}

	@Override
	public boolean stillValid(Player player) { return factory.stillValid(player); }

	@Override
	public void removed(Player player) {
		super.removed(player);
		factory.stopOpen(player);
	}

	/** Shift-click: from the factory into the inventory; from the inventory a diagram to its slot, a core to theirs, the rest to the matter. */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		int own = PuppetFactoryBlockEntity.SLOTS;
		if (index < own) {
			if (!moveItemStackTo(stack, own, slots.size(), true)) return ItemStack.EMPTY;
		} else if (stack.getItem() instanceof CircuitDiagramItem) {
			if (!moveItemStackTo(stack, PuppetFactoryBlockEntity.DIAGRAM, PuppetFactoryBlockEntity.DIAGRAM + 1, false)) return ItemStack.EMPTY;
		} else if (stack.is(ModItems.DEMON_CORE)) {
			if (!moveItemStackTo(stack, PuppetFactoryBlockEntity.CORES, PuppetFactoryBlockEntity.CORES + PuppetFactoryBlockEntity.CORE_SLOTS, false)) return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, PuppetFactoryBlockEntity.MATTER, PuppetFactoryBlockEntity.MATTER + PuppetFactoryBlockEntity.MATTER_SLOTS, false)) {
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
