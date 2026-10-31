package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The realm a Seal's keystone holds shut (SecretRealmSealBlock), sent with its chunk (its light's colour is the realm's). */
public class SecretRealmSealBlockEntity extends BlockEntity {
	private String realm = SecretRealm.VERDANT_HERB_VALLEY.getId();

	public SecretRealmSealBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SECRET_REALM_SEAL, pos, state);
	}

	public SecretRealm realm() {
		SecretRealm r = SecretRealm.byId(realm);
		return r == null ? SecretRealm.VERDANT_HERB_VALLEY : r;
	}

	public void setRealm(SecretRealm r) {
		realm = r.getId();
		setChanged();
		if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putString("Realm", realm);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		if (tag.contains("Realm")) realm = tag.getString("Realm");
	}

	@Override
	public CompoundTag getUpdateTag() {
		return saveWithoutMetadata();
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
