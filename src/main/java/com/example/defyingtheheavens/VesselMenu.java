package com.example.defyingtheheavens;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A seized player's body opened by the soul in it (SoulVesselEntity#openCustomInventoryScreen): its gear in a row along the top
 * (head, chest, legs, feet, then the off hand and the hand it fights with), its pack below, five rows of nine. Only the body's own
 * slots: the soul's things aren't shown and nothing passes to or from them (no hotbar or off-hand swap keys, and what the cursor
 * holds when it closes goes back into the pack); anything thrown out, or dropped outside the screen, falls from the body.
 */
public class VesselMenu extends AbstractContainerMenu {
	public static final MenuType<VesselMenu> TYPE = Registry.register(BuiltInRegistries.MENU, DefyingTheHeavens.id("soul_vessel"),
			new MenuType<>(VesselMenu::new, FeatureFlags.VANILLA_SET));

	/** Where the gear row and the pack lie in the screen (slot corners). */
	public static final int LEFT = 8, GEAR_Y = 27, PACK_Y = 51;
	/** The gear row's columns (of nine): the four armour pieces, a gap, the two hands at its right. */
	private static final int[] GEAR_COLUMNS = {0, 1, 2, 3, 7, 8};
	/** The empty-slot pictures of the gear row, as a player's inventory shows them (null: none). */
	private static final ResourceLocation[] GEAR_ICONS = {InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
			InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD,
			new ResourceLocation("item/empty_slot_sword")};

	private final Container body;
	/** The body itself (the server's side; null on the client, which only shows the slots). */
	private final SoulVesselEntity vessel;

	/** The client's side: the slots arrive from the server. */
	public VesselMenu(int id, Inventory inventory) {
		this(id, inventory, new SimpleContainer(SoulVesselEntity.PACK + SoulVesselEntity.GEAR.length), null);
	}

	public VesselMenu(int id, Inventory inventory, SoulVesselEntity vessel) {
		this(id, inventory, vessel.inventory(), vessel);
	}

	private VesselMenu(int id, Inventory inventory, Container body, SoulVesselEntity vessel) {
		super(TYPE, id);
		checkContainerSize(body, SoulVesselEntity.PACK + SoulVesselEntity.GEAR.length);
		this.body = body;
		this.vessel = vessel;
		for (int row = 0; row < SoulVesselEntity.PACK / 9; row++) {
			for (int col = 0; col < 9; col++) addSlot(new Slot(body, row * 9 + col, LEFT + col * 18, PACK_Y + row * 18));
		}
		for (int i = 0; i < SoulVesselEntity.GEAR.length; i++) {
			addSlot(new GearSlot(body, SoulVesselEntity.PACK + i, LEFT + GEAR_COLUMNS[i] * 18, GEAR_Y, SoulVesselEntity.GEAR[i], GEAR_ICONS[i]));
		}
	}

	/** A slot of the body's gear: armour only on its own piece, one at a time; the hands take anything. */
	private static final class GearSlot extends Slot {
		private final EquipmentSlot gear;
		private final ResourceLocation icon;

		GearSlot(Container container, int index, int x, int y, EquipmentSlot gear, ResourceLocation icon) {
			super(container, index, x, y);
			this.gear = gear;
			this.icon = icon;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return gear.getType() != EquipmentSlot.Type.ARMOR || Mob.getEquipmentSlotForItem(stack) == gear;
		}

		@Override
		public int getMaxStackSize() {
			return gear.getType() == EquipmentSlot.Type.ARMOR ? 1 : super.getMaxStackSize();
		}

		@Override
		public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
			return icon == null ? null : Pair.of(InventoryMenu.BLOCK_ATLAS, icon);
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return body.stillValid(player);
	}

	/**
	 * Clicks within the body's own slots, as in any chest; but no swap with the soul's hotbar or off hand (it has nothing to hold
	 * them with), and what is thrown out, or dropped outside the screen, falls from the body, not the soul.
	 */
	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		if (clickType == ClickType.SWAP) return;
		if (clickType == ClickType.THROW && slotId >= 0 && slotId < slots.size()) {
			Slot slot = slots.get(slotId);
			if (getCarried().isEmpty() && slot.hasItem() && slot.mayPickup(player)) {
				ItemStack thrown = slot.remove(button == 0 ? 1 : slot.getItem().getCount());
				slot.setChanged();
				if (vessel != null) vessel.throwItem(thrown);
			}
			return;
		}
		if (slotId == SLOT_CLICKED_OUTSIDE && (clickType == ClickType.PICKUP || clickType == ClickType.THROW) && !getCarried().isEmpty()) {
			ItemStack thrown = button == 0 ? getCarried() : getCarried().split(1);
			if (button == 0) setCarried(ItemStack.EMPTY);
			if (vessel != null) vessel.throwItem(thrown);
			return;
		}
		super.clicked(slotId, button, clickType, player);
	}

	/** Shift-click: from the pack onto the body (armour to its piece, else into the hands), from the gear into the pack. */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		int pack = SoulVesselEntity.PACK;
		if (index < pack) {
			if (!moveItemStackTo(stack, pack, slots.size(), false)) return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, 0, pack, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		return copy;
	}

	/** Closed with something on the cursor: back into the body's pack (or, with no room, falling from it), never to the soul. */
	@Override
	public void removed(Player player) {
		ItemStack held = getCarried();
		setCarried(ItemStack.EMPTY);
		if (held.isEmpty() || player.level().isClientSide) return;
		if (!moveItemStackTo(held, 0, SoulVesselEntity.PACK, false) || !held.isEmpty()) {
			if (vessel != null && !held.isEmpty()) vessel.throwItem(held);
		}
	}

	/** Touching this class registers the menu type. */
	public static void register() {
	}
}
