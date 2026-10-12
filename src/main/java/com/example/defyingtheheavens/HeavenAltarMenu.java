package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.core.Registry;

/**
 * The Heaven Altar's offering screen: its nine offering slots in a ring, the player's inventory below, and what the heavens
 * would make of a ritual here, judged on the server when it opens (see {@link HeavenAltar}): the altar's points by part,
 * whether it sees the sky, its height bonus and level, and where the player stands with the heavens. The offering itself is
 * weighed on the client from what lies in the slots, so the chance follows every item laid down or taken up.
 */
public class HeavenAltarMenu extends AbstractContainerMenu {
	public static final MenuType<HeavenAltarMenu> TYPE = Registry.register(BuiltInRegistries.MENU, DefyingTheHeavens.id("heaven_altar"),
			new MenuType<>(HeavenAltarMenu::new, FeatureFlags.VANILLA_SET));

	/** Where the offering ring's middle lies in the screen, and how far out its slots sit. */
	public static final int RING_X = 66, RING_Y = 74, RING_RADIUS = 40;
	/** The player's inventory: its left edge and top, the hotbar 58 below. */
	public static final int INVENTORY_X = 69, INVENTORY_Y = 142;

	/** The synced values, by index. */
	public static final int SKY = 0, POINTS = 1, HEIGHT = 2, PLATFORM = 3, TERRACES = 4, NOBLE = 5, PILLARS = 6, LIGHTS = 7, TREASURES = 8,
			STATUS = 9, FAVOUR = 10, ALTAR_Y = 11, COUNT = 12;
	/** Where the player stands with the heavens ({@link #STATUS}). */
	public static final int READY = 0, GRANTED = 1, NOT_YET = 2, UNTIL_DAWN = 3, BUSY = 4;

	private final Container altar;
	private final ContainerData data;

	/** The client's side: the slots and values arrive from the server. */
	public HeavenAltarMenu(int id, Inventory inventory) {
		this(id, inventory, new SimpleContainer(HeavenAltarBlockEntity.SLOTS), new SimpleContainerData(COUNT));
	}

	public HeavenAltarMenu(int id, Inventory inventory, Container altar, ContainerData data) {
		super(TYPE, id);
		checkContainerSize(altar, HeavenAltarBlockEntity.SLOTS);
		checkContainerDataCount(data, COUNT);
		this.altar = altar;
		this.data = data;
		altar.startOpen(inventory.player);
		for (int i = 0; i < HeavenAltarBlockEntity.SLOTS; i++) {
			int[] at = ringSlot(i);
			addSlot(new Slot(altar, i, at[0], at[1]));
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
		}
		for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
		addDataSlots(data);
	}

	/** Offering slot {@code i}'s top-left corner: round the ring clockwise from the top. */
	public static int[] ringSlot(int i) {
		double angle = -Math.PI / 2 + 2 * Math.PI * i / HeavenAltarBlockEntity.SLOTS;
		return new int[] {RING_X + (int) Math.round(Math.cos(angle) * RING_RADIUS) - 8, RING_Y + (int) Math.round(Math.sin(angle) * RING_RADIUS) - 8};
	}

	/** What a ritual at {@code pos} looks like to {@code player}, for the screen. */
	public static ContainerData judge(ServerPlayer player, BlockPos pos) {
		SimpleContainerData data = new SimpleContainerData(COUNT);
		HeavenAltar.Judgement j = HeavenAltar.judge(player.level(), pos);
		data.set(SKY, j.sky() ? 1 : 0);
		data.set(POINTS, j.points());
		data.set(HEIGHT, HeavenAltar.heightBonus(player.level(), pos));
		data.set(PLATFORM, j.platform());
		data.set(TERRACES, j.terraces());
		data.set(NOBLE, j.noble());
		data.set(PILLARS, j.pillars());
		data.set(LIGHTS, j.lights());
		data.set(TREASURES, j.treasures());
		PlayerCultivation c = CultivationManager.get(player);
		String refusal = HeavenRitual.refusal(player, c);
		int status = READY;
		if (c.hasHeavensLeave() && !c.isMortal()) status = GRANTED;
		else if (ModLang.MSG_RITUAL_NOT_NOW.equals(refusal)) status = NOT_YET;
		else if (ModLang.MSG_RITUAL_DAWN.equals(refusal)) status = UNTIL_DAWN;
		else if (refusal != null) status = BUSY;
		data.set(STATUS, status);
		data.set(FAVOUR, c.getFavour());
		data.set(ALTAR_Y, pos.getY());
		return data;
	}

	public int get(int index) { return data.get(index); }

	/** What lies on the altar is worth, to the heavens (Low spirit stones). */
	public double offeringWorth() {
		double total = 0;
		for (int i = 0; i < HeavenAltarBlockEntity.SLOTS; i++) total += HeavenAltar.worth(slots.get(i).getItem());
		return total;
	}

	@Override
	public boolean stillValid(Player player) { return altar.stillValid(player); }

	@Override
	public void removed(Player player) {
		super.removed(player);
		altar.stopOpen(player);
	}

	/** Shift-click: from the altar into the inventory, or from the inventory onto the altar. */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		int altarSlots = HeavenAltarBlockEntity.SLOTS;
		if (index < altarSlots) {
			if (!moveItemStackTo(stack, altarSlots, slots.size(), true)) return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, 0, altarSlots, false)) {
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
