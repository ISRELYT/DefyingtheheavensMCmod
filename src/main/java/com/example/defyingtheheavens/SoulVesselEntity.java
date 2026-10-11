package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A player's body seized by another cultivator's nascent soul (see {@link SoulProjection#tryPossess}): the player dies, their
 * soul driven out, and this stands in their place, in their likeness, with their body's strength (their realm's health, damage,
 * speed and armor; what they wore and held, it wears and holds) and, unless items are kept on death, everything they carried.
 * Only the soul in it moves it; the moment the soul leaves (or runs dry, or is called home) it collapses, and everything it
 * carries falls where it does.
 */
public class SoulVesselEntity extends PathfinderMob {
	private static final EntityDataAccessor<Optional<UUID>> LIKENESS = SynchedEntityData.defineId(SoulVesselEntity.class,
			EntityDataSerializers.OPTIONAL_UUID);
	/** The attributes copied from the seized body: their base and their cultivation's share (not pressure, not worn gear's). */
	private static final Attribute[] BODY = {Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.MOVEMENT_SPEED, Attributes.ARMOR,
			Attributes.ARMOR_TOUGHNESS, Attributes.KNOCKBACK_RESISTANCE};
	/** What it carries beyond what it wears and holds (dropped when it falls). */
	private final List<ItemStack> carried = new ArrayList<>();

	public SoulVesselEntity(EntityType<? extends SoulVesselEntity> type, Level level) {
		super(type, level);
	}

	/** What it carries beyond its gear (for the game tests). */
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
				if (!stack.isEmpty()) vessel.carried.add(stack);
			}
			RingContainer rings = RingContainer.of(victim);
			for (int i = 0; i < rings.getContainerSize(); i++) {
				ItemStack stack = rings.removeItemNoUpdate(i);
				if (!stack.isEmpty()) vessel.carried.add(stack);
			}
		}
		vessel.setHealth(Math.min(victim.getHealth() > 0 ? victim.getHealth() : vessel.getMaxHealth(), vessel.getMaxHealth()));
		return vessel;
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
		for (ItemStack stack : carried) {
			if (!EnchantmentHelper.hasVanishingCurse(stack)) spawnAtLocation(stack);
		}
		carried.clear();
	}

	@Override public boolean removeWhenFarAway(double distance) { return false; }
	@Override public boolean requiresCustomPersistence() { return true; }

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		UUID likeness = getLikeness();
		if (likeness != null) tag.putUUID("Likeness", likeness);
		ListTag list = new ListTag();
		for (ItemStack stack : carried) list.add(stack.save(new CompoundTag()));
		tag.put("Carried", list);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.hasUUID("Likeness")) entityData.set(LIKENESS, Optional.of(tag.getUUID("Likeness")));
		carried.clear();
		for (Tag entry : tag.getList("Carried", Tag.TAG_COMPOUND)) {
			ItemStack stack = ItemStack.of((CompoundTag) entry);
			if (!stack.isEmpty()) carried.add(stack);
		}
	}
}
