package com.example.defyingtheheavens;

import net.minecraft.core.NonNullList;
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
 * A cultivator's body, left sitting in meditation while their nascent soul is out (see {@link SoulProjection}). It is the real
 * body: the player's health, armor and gear (drawn on it, and its Protection counts), and what hurts it hurts them. Its owner hears
 * of every blow; if it is slain, their soul dies with it. It takes no knockback, holds nothing loose, drops nothing (whatever the
 * player carries falls where they die, which is here), and is gone the moment no soul claims it.
 */
public class SoulBodyEntity extends LivingEntity {
	private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(SoulBodyEntity.class,
			EntityDataSerializers.OPTIONAL_UUID);
	/**
	 * How its owner was drawn as they set out (TwoPaths#look, by ordinal), and so how it sits on: as a man, or in the dead's light, a
	 * Ghost King's crowned (one of the dead's form, or one of the living in a Ghost King's form). A form is kept while the soul is out.
	 */
	private static final EntityDataAccessor<Byte> LOOK = SynchedEntityData.defineId(SoulBodyEntity.class, EntityDataSerializers.BYTE);
	private final NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);
	private final NonNullList<ItemStack> hands = NonNullList.withSize(2, ItemStack.EMPTY);
	/** The owner's armor and toughness as they set out (what the body's own attributes would miss: the cultivation's share). */
	private float armorValue;
	private float toughness;

	public SoulBodyEntity(EntityType<? extends SoulBodyEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return LivingEntity.createLivingAttributes().add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	/** The body {@code player} leaves where they sit: their likeness, health and armor. */
	public static SoulBodyEntity of(ServerPlayer player) {
		SoulBodyEntity body = new SoulBodyEntity(ModEntities.SOUL_BODY, player.level());
		body.entityData.set(OWNER, Optional.of(player.getUUID()));
		body.entityData.set(LOOK, (byte) TwoPaths.look(CultivationManager.own(player)).ordinal());
		body.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
		body.setYBodyRot(player.getYRot());
		body.setYHeadRot(player.getYRot());
		var maxHealth = body.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth != null) maxHealth.setBaseValue(player.getMaxHealth());
		body.setHealth(player.getHealth());
		body.armorValue = player.getArmorValue();
		body.toughness = (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
		for (EquipmentSlot slot : EquipmentSlot.values()) body.setItemSlot(slot, player.getItemBySlot(slot).copy());
		return body;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(OWNER, Optional.empty());
		entityData.define(LOOK, (byte) 0);
	}

	public UUID getOwnerId() { return entityData.get(OWNER).orElse(null); }

	/** How it sits on: its owner's look as they set out ({@link #LOOK}). */
	public TwoPaths.Look look() {
		TwoPaths.Look[] looks = TwoPaths.Look.values();
		return looks[Math.max(0, Math.min(looks.length - 1, entityData.get(LOOK)))];
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide || isRemoved() || isDeadOrDying()) return;
		UUID owner = getOwnerId();
		if (owner == null || !SoulProjection.ownsBody(owner, getUUID())) discard(); // its soul is back (or it outlived a crash)
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (level().isClientSide || isRemoved() || isDeadOrDying()) return false;
		UUID owner = getOwnerId();
		MinecraftServer server = level().getServer();
		if (owner == null || server == null || !SoulProjection.ownsBody(owner, getUUID())) {
			discard();
			return false;
		}
		// A player strikes the body only where players may strike its owner.
		if (source.getEntity() instanceof Player attacker && attacker != null) {
			ServerPlayer self = server.getPlayerList().getPlayer(owner);
			if (!server.isPvpAllowed() || self != null && !self.canHarmPlayer(attacker)) return false;
		}
		boolean hurt = super.hurt(source, amount);
		if (hurt && isAlive()) SoulProjection.onBodyHurt(this, owner);
		return hurt;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		UUID owner = getOwnerId();
		if (!level().isClientSide && owner != null && level().getServer() != null) SoulProjection.onBodySlain(owner, level().getServer());
	}

	/** The cultivation's armor is the body's too (its own attributes only hold what the worn gear adds). */
	@Override
	public int getArmorValue() {
		return Math.round(armorValue);
	}

	@Override
	public double getAttributeValue(Attribute attribute) {
		return attribute == Attributes.ARMOR_TOUGHNESS ? toughness : super.getAttributeValue(attribute);
	}

	@Override protected void dropAllDeathLoot(DamageSource source) { }
	@Override public boolean isPushable() { return false; }
	@Override public boolean isPickable() { return !isRemoved(); }
	@Override public boolean canBeCollidedWith() { return false; }
	@Override public boolean shouldShowName() { return false; }
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
