package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A storage formation's screen: its space above (not slots: a grid of what it holds, any number of each, drawn by the client from what
 * StorageFormations sends and clicked through it, StorageFormations#act), the player's pack below as real slots. It closes when the
 * formation is gone (taken up, refined into the next tier: its screen opened again on that) or left behind.
 */
public class StorageFormationMenu extends AbstractContainerMenu {
	public static final MenuType<StorageFormationMenu> TYPE = Registry.register(BuiltInRegistries.MENU, DefyingTheHeavens.id("storage_formation"),
			new MenuType<>(StorageFormationMenu::new, FeatureFlags.VANILLA_SET));

	/** Where the space's grid lies in the screen, its columns and the rows shown at once, and the pack below it. */
	public static final int GRID_X = 12, GRID_Y = 38, COLUMNS = 9, ROWS = 5;
	public static final int INVENTORY_X = 12, INVENTORY_Y = GRID_Y + ROWS * 18 + 14;
	/** The screen's size: the grid and its scroll bar across, everything down to the hotbar's foot. */
	public static final int WIDTH = GRID_X + COLUMNS * 18 + 22, HEIGHT = INVENTORY_Y + 58 + 18 + 8;
	/** How far from the formation its screen stays open. */
	private static final double REACH = 8;

	/** Where the formation stands (null on the client, which only draws). */
	private final BlockPos pos;

	/** The client's side: the space arrives by its own packets (ClientStorageSpace). */
	public StorageFormationMenu(int id, Inventory inventory) {
		this(id, inventory, null);
	}

	public StorageFormationMenu(int id, Inventory inventory, BlockPos pos) {
		super(TYPE, id);
		this.pos = pos;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
		}
		for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
	}

	public BlockPos pos() { return pos; }

	/** Shift-click on the pack: into the space, as much as fits. */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (player instanceof ServerPlayer sp && slot.hasItem()) {
			StorageFormations.putFromPack(sp, slot.getItem());
			slot.setChanged();
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		if (pos == null) return true;
		if (!(player.level().getBlockEntity(pos) instanceof StorageFormationBlockEntity formation)
				|| player.distanceToSqr(Vec3.atCenterOf(pos)) > REACH * REACH) return false;
		// Its seal mended, or its owner no longer trusting them, while a stranger looks in: it shuts on them.
		return formation.getOwner() == null || formation.allows(player) || formation.isBroken() || player.isCreative();
	}

	/** Touching this class registers the menu type. */
	public static void register() {
	}
}
