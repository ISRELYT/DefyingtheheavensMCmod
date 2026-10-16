package com.example.defyingtheheavens;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A player's body seized by another cultivator's nascent soul (see {@link SoulProjection#tryPossess}): the player dies, their
 * soul driven out, and this stands in their place, in their likeness, with their body's strength (their realm's health, damage,
 * speed and armor; what they wore and held, it wears and holds) and, unless items are kept on death, everything they carried.
 * Only the soul in it moves it; the moment the soul leaves (or runs dry, or is called home) it collapses, and everything it
 * carries falls where it does.
 * <p>
 * The soul in it may open its inventory (the inventory key, as a horse's rider opens the horse's: {@link #openCustomInventoryScreen},
 * VesselMenu): what it wears, holds and carries, to move about, put on and take in hand. Nothing passes between it and the soul's
 * own things: a soul touches nothing.
 */
public class SoulVesselEntity extends PathfinderMob implements HasCustomInventoryScreen {
	private static final EntityDataAccessor<Optional<UUID>> LIKENESS = SynchedEntityData.defineId(SoulVesselEntity.class,
			EntityDataSerializers.OPTIONAL_UUID);
	/** The attributes copied from the seized body: their base and their cultivation's share (not pressure, not worn gear's). */
	private static final Attribute[] BODY = {Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.MOVEMENT_SPEED, Attributes.ARMOR,
			Attributes.ARMOR_TOUGHNESS, Attributes.KNOCKBACK_RESISTANCE};
	/** Slots for what it carries beyond what it wears and holds: a player's pack less the hand, and their rings, with room over. */
	public static final int PACK = 45;
	/** What it wears and holds, after the pack in its inventory ({@link #inventory}): head, chest, legs, feet, off hand, main hand. */
	public static final EquipmentSlot[] GEAR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
			EquipmentSlot.OFFHAND, EquipmentSlot.MAINHAND};
	/** What it carries beyond its gear (dropped when it falls), slot by slot. */
	private final NonNullList<ItemStack> carried = NonNullList.withSize(PACK, ItemStack.EMPTY);

	public SoulVesselEntity(EntityType<? extends SoulVesselEntity> type, Level level) {
		super(type, level);
	}

	/** What it carries beyond its gear, slot by slot (empty slots included). */
	List<ItemStack> carried() {
		return carried;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.ATTACK_DAMAGE, 1.0).add(Attributes.MOVEMENT_SPEED, 0.1)
				.add(Attributes.ARMOR, 0.0).add(Attributes.ARMOR_TOUGHNESS, 0.0);
	}

	/**
	 * The vessel {@code victim}'s body becomes: their likeness and strength, and ({@code takeItems}) everything they carry, taken
	 * from them so their death drops nothing.
	 */
	public static SoulVesselEntity of(ServerPlayer victim, boolean takeItems) {
		SoulVesselEntity vessel = new SoulVesselEntity(ModEntities.SOUL_VESSEL, victim.level());
		vessel.entityData.set(LIKENESS, Optional.of(victim.getUUID()));
		vessel.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), victim.getXRot());
		vessel.setYBodyRot(victim.yBodyRot);
		vessel.setYHeadRot(victim.getYHeadRot());
		vessel.setCustomName(victim.getDisplayName());
		vessel.setCustomNameVisible(true);
		vessel.setPersistenceRequired();
		for (Attribute attribute : BODY) {
			AttributeInstance from = victim.getAttribute(attribute);
			AttributeInstance to = vessel.getAttribute(attribute);
			if (from == null || to == null) continue;
			to.setBaseValue(from.getBaseValue());
			for (AttributeModifier modifier : from.getModifiers()) {
				if (CultivationStats.isCultivationModifier(modifier.getId())) to.addPermanentModifier(modifier);
			}
		}
		if (takeItems) {
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				vessel.setItemSlot(slot, victim.getItemBySlot(slot).copy());
				victim.setItemSlot(slot, ItemStack.EMPTY);
			}
			var inventory = victim.getInventory();
			for (int i = 0; i < inventory.getContainerSize(); i++) {
				ItemStack stack = inventory.removeItemNoUpdate(i);
				if (!stack.isEmpty()) vessel.stow(stack);
			}
			RingContainer rings = RingContainer.of(victim);
			for (int i = 0; i < rings.getContainerSize(); i++) {
				ItemStack stack = rings.removeItemNoUpdate(i);
				if (!stack.isEmpty()) vessel.stow(stack);
			}
		}
		vessel.setHealth(Math.min(victim.getHealth() > 0 ? victim.getHealth() : vessel.getMaxHealth(), vessel.getMaxHealth()));
		return vessel;
	}

	/** Into the first empty slot of its pack; with none left, it falls at its feet (nothing is lost). */
	void stow(ItemStack stack) {
		for (int i = 0; i < PACK; i++) {
			if (carried.get(i).isEmpty()) {
				carried.set(i, stack);
				return;
			}
		}
		spawnAtLocation(stack);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(LIKENESS, Optional.empty());
	}

	/** Whose likeness it wears (for its skin). */
	public UUID getLikeness() { return entityData.get(LIKENESS).orElse(null); }

	/** No soul holds it up: it falls, and everything it carries with it. */
	public void collapse() {
		if (isAlive()) hurt(damageSources().genericKill(), Float.MAX_VALUE);
	}

	@Override
	public void tick() {
		super.tick();
		// Nothing moves it but a soul: one left without one (a soul gone home, a reload) falls.
		if (!level().isClientSide && isAlive() && tickCount > 5 && !SoulProjection.isPossessed(this)) collapse();
	}

	// --- Its inventory, opened by the soul in it ---

	/** The inventory key, pressed by the one riding it: the soul in it opens what it wears, holds and carries (VesselMenu). */
	@Override
	public void openCustomInventoryScreen(Player player) {
		if (level().isClientSide || !(player instanceof ServerPlayer soul) || SoulProjection.possessorOf(this) != soul) return;
		soul.openMenu(new SimpleMenuProvider((id, inventory, p) -> new VesselMenu(id, inventory, this),
				Component.translatable(ModLang.VESSEL_INVENTORY, getDisplayName())));
	}

	/**
	 * Its inventory as one container: the pack ({@link #PACK} slots), then its gear ({@link #GEAR}); open only to the soul in it
	 * while it stands.
	 */
	public Container inventory() {
		return new Container() {
			@Override public int getContainerSize() { return PACK + GEAR.length; }

			@Override
			public boolean isEmpty() {
				for (int i = 0; i < getContainerSize(); i++) if (!getItem(i).isEmpty()) return false;
				return true;
			}

			@Override
			public ItemStack getItem(int slot) {
				return slot < PACK ? carried.get(slot) : getItemBySlot(GEAR[slot - PACK]);
			}

			@Override
			public ItemStack removeItem(int slot, int count) {
				ItemStack stack = getItem(slot);
				if (stack.isEmpty() || count <= 0) return ItemStack.EMPTY;
				ItemStack taken = stack.split(count);
				setItem(slot, stack); // a stack left empty is set back as nothing (and the gear sees its change)
				return taken;
			}

			@Override
			public ItemStack removeItemNoUpdate(int slot) {
				ItemStack stack = getItem(slot);
				setItem(slot, ItemStack.EMPTY);
				return stack;
			}

			@Override
			public void setItem(int slot, ItemStack stack) {
				if (slot < PACK) carried.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
				else setItemSlot(GEAR[slot - PACK], stack.isEmpty() ? ItemStack.EMPTY : stack);
			}

			@Override public void setChanged() { }

			@Override
			public boolean stillValid(Player player) {
				return isAlive() && SoulProjection.possessorOf(SoulVesselEntity.this) == player;
			}

			@Override
			public void clearContent() {
				for (int i = 0; i < getContainerSize(); i++) setItem(i, ItemStack.EMPTY);
			}
		};
	}

	/** Something thrown from its inventory: it falls before it, as a player's throw would. */
	void throwItem(ItemStack stack) {
		if (stack.isEmpty() || level().isClientSide) return;
		var item = new net.minecraft.world.entity.item.ItemEntity(level(), getX(), getEyeY() - 0.3, getZ(), stack);
		item.setPickUpDelay(40);
		item.setDeltaMovement(getLookAngle().scale(0.3).add(0, 0.1, 0));
		level().addFreshEntity(item);
	}

	/** Its gear is dropped with the rest below (whatever the mob-loot rule says: these are a player's things), not as mob loot. */
	@Override
	protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) { }

	/** Everything it wears, holds and carries falls where it does, as a player's things fall where they die. */
	@Override
	protected void dropEquipment() {
		super.dropEquipment();
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = getItemBySlot(slot);
			setItemSlot(slot, ItemStack.EMPTY);
			if (!stack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(stack)) spawnAtLocation(stack);
		}
		for (int i = 0; i < PACK; i++) {
			ItemStack stack = carried.get(i);
			carried.set(i, ItemStack.EMPTY);
			if (!stack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(stack)) spawnAtLocation(stack);
		}
	}

	@Override public boolean removeWhenFarAway(double distance) { return false; }
	@Override public boolean requiresCustomPersistence() { return true; }

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		UUID likeness = getLikeness();
		if (likeness != null) tag.putUUID("Likeness", likeness);
		ListTag list = new ListTag();
		for (int i = 0; i < PACK; i++) {
			ItemStack stack = carried.get(i);
			if (stack.isEmpty()) continue;
			CompoundTag one = stack.save(new CompoundTag());
			one.putByte("Slot", (byte) i);
			list.add(one);
		}
		tag.put("Carried", list);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.hasUUID("Likeness")) entityData.set(LIKENESS, Optional.of(tag.getUUID("Likeness")));
		for (int i = 0; i < PACK; i++) carried.set(i, ItemStack.EMPTY);
		for (Tag entry : tag.getList("Carried", Tag.TAG_COMPOUND)) {
			CompoundTag one = (CompoundTag) entry;
			ItemStack stack = ItemStack.of(one);
			if (stack.isEmpty()) continue;
			int slot = one.contains("Slot") ? one.getByte("Slot") & 0xFF : -1; // saves from before its slots: in order
			if (slot >= 0 && slot < PACK && carried.get(slot).isEmpty()) carried.set(slot, stack);
			else stow(stack);
		}
	}
}
