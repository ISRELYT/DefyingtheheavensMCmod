package com.example.defyingtheheavens;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/**
 * A body of a cultivator with a clone out (Clones) that its player isn't playing now: the clone, left standing where its player went
 * back to their main body; or the main body, left sitting in meditation while they are in the clone. It is that body, as it was left:
 * its owner's likeness (and their name over a standing clone), what it wears and holds (drawn on it), its health and armour; what
 * hurts it hurts that body. What it carries is kept in its owner's record (PlayerCultivation#getClone), not here: it drops nothing
 * of itself, and a clone slain lets its things fall (Clones#onStandInDied). It goes the moment its owner's record no longer has it.
 */
public class CloneBodyEntity extends LivingEntity {
	private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(CloneBodyEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	/** The main body, sitting in meditation (else the clone, standing). */
	private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(CloneBodyEntity.class, EntityDataSerializers.BOOLEAN);
	private final NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);
	private final NonNullList<ItemStack> hands = NonNullList.withSize(2, ItemStack.EMPTY);
	/** The body's armour and toughness as it was left (what its own attributes miss: the cultivation's share). */
	private float armorValue;
	private float toughness;

	public CloneBodyEntity(EntityType<? extends CloneBodyEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return LivingEntity.createLivingAttributes().add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	/**
	 * The body {@code owner} leaves where {@code at} stands (themselves, as they go into the other: their look, armour and health
	 * then), {@code sitting} for the main body; what it wears and holds as {@code body} has it (Possession#takeFrom's tag).
	 */
	static CloneBodyEntity of(ServerPlayer owner, ServerPlayer at, boolean sitting, CompoundTag body) {
		CloneBodyEntity entity = new CloneBodyEntity(ModEntities.CLONE_BODY, at.level());
		entity.entityData.set(OWNER, Optional.of(owner.getUUID()));
		entity.entityData.set(SITTING, sitting);
		entity.moveTo(at.getX(), at.getY(), at.getZ(), at.getYRot(), 0);
		entity.setYBodyRot(at.getYRot());
		entity.setYHeadRot(at.getYRot());
		entity.wear(body);
		entity.healthFrom(body, at.getMaxHealth(), at.getHealth());
		entity.armorValue = at.getArmorValue();
		entity.toughness = (float) at.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
		if (!sitting) entity.setCustomName(owner.getGameProfile().getName() == null ? null : Component.literal(owner.getGameProfile().getName()));
		return entity;
	}

	/** A clone just split off {@code owner} at {@code x, y, z}: nothing on it, its health whole ({@code maxHealth}), its armour its realm's. */
	static CloneBodyEntity split(ServerPlayer owner, double x, double y, double z, float yRot, float maxHealth, float armor) {
		CloneBodyEntity entity = new CloneBodyEntity(ModEntities.CLONE_BODY, owner.level());
		entity.entityData.set(OWNER, Optional.of(owner.getUUID()));
		entity.entityData.set(SITTING, false);
		entity.moveTo(x, y, z, yRot, 0);
		entity.setYBodyRot(yRot);
		entity.setYHeadRot(yRot);
		var max = entity.getAttribute(Attributes.MAX_HEALTH);
		if (max != null) max.setBaseValue(Math.max(1, maxHealth));
		entity.setHealth(Math.max(1, maxHealth));
		entity.armorValue = armor;
		entity.setCustomName(Component.literal(owner.getGameProfile().getName()));
		return entity;
	}

	/** What {@code body} (Possession#takeFrom's tag) wears and holds, put on it to be seen. */
	private void wear(CompoundTag body) {
		int selected = body.getInt("Selected");
		for (Tag entry : body.getList("Inventory", Tag.TAG_COMPOUND)) {
			CompoundTag one = (CompoundTag) entry;
			int slot = one.getByte("Slot") & 255;
			EquipmentSlot worn = slot == selected ? EquipmentSlot.MAINHAND : switch (slot) {
				case 100 -> EquipmentSlot.FEET;
				case 101 -> EquipmentSlot.LEGS;
				case 102 -> EquipmentSlot.CHEST;
				case 103 -> EquipmentSlot.HEAD;
				case 150 -> EquipmentSlot.OFFHAND;
				default -> null;
			};
			if (worn != null) setItemSlot(worn, ItemStack.of(one));
		}
	}

	/** Its health and the whole of it: as {@code body} had them, else {@code health} of {@code max} (the player leaving it). */
	private void healthFrom(CompoundTag body, float max, float health) {
		float whole = body.contains("MaxHealth") && body.getFloat("MaxHealth") > 0 ? body.getFloat("MaxHealth") : max;
		var attribute = getAttribute(Attributes.MAX_HEALTH);
		if (attribute != null) attribute.setBaseValue(Math.max(1, whole));
		setHealth(Math.max(1, body.contains("Health") && body.getFloat("Health") > 0 ? body.getFloat("Health") : health));
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(OWNER, Optional.empty());
		entityData.define(SITTING, false);
	}

	public UUID getOwnerId() { return entityData.get(OWNER).orElse(null); }

	/** The main body, sitting in meditation (else the clone, standing). */
	public boolean isSitting() { return entityData.get(SITTING); }

	/** Sitting, it is as low as a body left in meditation is (SoulBodyEntity's 1.2); standing, a player's height. */
	@Override
	public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
		return isSitting() ? net.minecraft.world.entity.EntityDimensions.scalable(0.6f, 1.2f) : super.getDimensions(pose);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (SITTING.equals(key)) refreshDimensions();
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide || isRemoved() || isDeadOrDying() || tickCount % 20 != 0) return;
		UUID owner = getOwnerId();
		MinecraftServer server = level().getServer();
		// Its body taken back up, drawn in, dissolved, or outlived by a crash: nothing claims it any more.
		if (owner == null || server == null || !Clones.claims(server, owner, getUUID(), isSitting())) discard();
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (level().isClientSide || isRemoved() || isDeadOrDying()) return false;
		UUID owner = getOwnerId();
		MinecraftServer server = level().getServer();
		if (owner == null || server == null || !Clones.claims(server, owner, getUUID(), isSitting())) {
			discard();
			return false;
		}
		// A player strikes it only where players may strike its owner.
		if (source.getEntity() instanceof Player attacker) {
			ServerPlayer self = server.getPlayerList().getPlayer(owner);
			if (!server.isPvpAllowed() || self != null && self != attacker && !self.canHarmPlayer(attacker)) return false;
		}
		boolean hurt = super.hurt(source, amount);
		if (hurt && isAlive()) Clones.onStandInHurt(this, owner);
		return hurt;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		UUID owner = getOwnerId();
		if (!level().isClientSide && owner != null && level().getServer() != null) Clones.onStandInDied(this, owner, source);
	}

	/** The cultivation's armour is the body's too (its own attributes only hold what the worn gear adds). */
	@Override
	public int getArmorValue() {
		return Math.max(super.getArmorValue(), Math.round(armorValue));
	}

	@Override
	public double getAttributeValue(Attribute attribute) {
		return attribute == Attributes.ARMOR_TOUGHNESS ? Math.max(super.getAttributeValue(attribute), toughness) : super.getAttributeValue(attribute);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		UUID owner = getOwnerId();
		if (owner != null) tag.putUUID("Owner", owner);
		tag.putBoolean("Sitting", isSitting());
		tag.putFloat("ArmorValue", armorValue);
		tag.putFloat("Toughness", toughness);
		ListTag worn = new ListTag();
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = getItemBySlot(slot);
			if (stack.isEmpty()) continue;
			CompoundTag one = stack.save(new CompoundTag());
			one.putString("Worn", slot.getName());
			worn.add(one);
		}
		tag.put("Worn", worn);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		entityData.set(OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
		entityData.set(SITTING, tag.getBoolean("Sitting"));
		armorValue = tag.getFloat("ArmorValue");
		toughness = tag.getFloat("Toughness");
		for (Tag entry : tag.getList("Worn", Tag.TAG_COMPOUND)) {
			CompoundTag one = (CompoundTag) entry;
			setItemSlot(EquipmentSlot.byName(one.getString("Worn")), ItemStack.of(one));
		}
	}

	@Override protected void dropAllDeathLoot(DamageSource source) { }
	/** It stays in the world its owner's record has it in (a portal would carry off a body still claiming to be where it was left). */
	@Override public boolean canChangeDimensions() { return false; }
	@Override public boolean isPushable() { return false; }
	@Override public boolean isPickable() { return !isRemoved(); }
	@Override public boolean canBeCollidedWith() { return false; }
	@Override public boolean shouldShowName() { return !isSitting() && hasCustomName(); }
	@Override public HumanoidArm getMainArm() { return HumanoidArm.RIGHT; }
	@Override public Iterable<ItemStack> getArmorSlots() { return armor; }
	@Override public Iterable<ItemStack> getHandSlots() { return hands; }

	@Override
	public ItemStack getItemBySlot(EquipmentSlot slot) {
		return slot.getType() == EquipmentSlot.Type.ARMOR ? armor.get(slot.getIndex()) : hands.get(slot.getIndex());
	}

	@Override
	public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
		if (slot.getType() == EquipmentSlot.Type.ARMOR) armor.set(slot.getIndex(), stack);
		else hands.set(slot.getIndex(), stack);
	}
}
