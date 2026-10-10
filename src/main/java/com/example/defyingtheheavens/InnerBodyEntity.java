package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A meditator's body, left sitting cross-legged while their soul is in the Inner Realm (see {@link InnerRealm}). It takes
 * no damage itself: a blow, or being moved, sends the soul back to take it. It removes itself once its owner isn't inside.
 */
public class InnerBodyEntity extends LivingEntity {
	private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(InnerBodyEntity.class,
			EntityDataSerializers.OPTIONAL_UUID);
	private Vec3 anchor;

	public InnerBodyEntity(EntityType<? extends InnerBodyEntity> type, Level level) {
		super(type, level);
		setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return LivingEntity.createLivingAttributes();
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(OWNER, Optional.empty());
	}

	public void setOwner(Player player) { entityData.set(OWNER, Optional.of(player.getUUID())); }

	public UUID getOwnerId() { return entityData.get(OWNER).orElse(null); }

	@Override
	public void tick() {
		super.tick();
		setDeltaMovement(Vec3.ZERO);
		if (level().isClientSide) return;
		if (anchor == null) anchor = position();
		ServerPlayer owner = owner();
		CompoundTag back = owner == null ? null : CultivationManager.get(owner).getInnerReturn();
		boolean occupied = owner != null && InnerRealm.isInside(owner) && back != null && back.hasUUID("Body")
				&& back.getUUID("Body").equals(getUUID());
		if (!occupied) {
			discard(); // its soul has gone back (or it was left over from a crash)
			return;
		}
		if (position().distanceToSqr(anchor) > 0.25) InnerRealm.onBodyDisturbed(owner);
	}

	private ServerPlayer owner() {
		UUID id = getOwnerId();
		return id == null || level().getServer() == null ? null : level().getServer().getPlayerList().getPlayer(id);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (level().isClientSide || isRemoved() || amount <= 0) return false;
		ServerPlayer owner = owner();
		if (owner != null && InnerRealm.isInside(owner)) InnerRealm.onBodyHurt(this, owner, source, amount);
		else discard();
		return false;
	}

	@Override public boolean isPushable() { return false; }
	@Override public boolean isPickable() { return true; }
	@Override public boolean canBeCollidedWith() { return false; }
	@Override public boolean shouldShowName() { return false; }
	@Override public Iterable<ItemStack> getArmorSlots() { return List.of(); }
	@Override public ItemStack getItemBySlot(EquipmentSlot slot) { return ItemStack.EMPTY; }
	@Override public void setItemSlot(EquipmentSlot slot, ItemStack stack) { }
	@Override public HumanoidArm getMainArm() { return HumanoidArm.RIGHT; }

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		UUID id = getOwnerId();
		if (id != null) tag.putUUID("Owner", id);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.hasUUID("Owner")) entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
	}
}
