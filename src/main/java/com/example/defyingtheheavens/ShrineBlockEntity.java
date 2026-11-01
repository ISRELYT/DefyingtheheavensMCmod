package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * A shrine's own record (Shrines): whose it is, the faith its worshippers have laid up for it, their devotion (how much each prayer is
 * worth: answered requests raise it, ignored ones lower it), the request it carries, and when it last saw worship. Who broke it last is
 * kept only till it is gone (ShrineBlock), to tell its owner's own hand from a desecration.
 */
public class ShrineBlockEntity extends BlockEntity {
	UUID owner;
	String ownerName = "";
	double faith;
	double devotion = 1.0;
	/** What its worshippers ask of its owner (an item id and how many), and since when (game time); none with an empty id. */
	String request = "";
	int requestCount;
	long requestAt;
	long lastWorship;
	/** Ticks it has stood (its rhythms: Shrines#tick); not saved. */
	int ticks;
	/** Who is breaking it, till it is gone (ShrineBlock#playerWillDestroy); not saved. */
	UUID breaker;

	public ShrineBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SHRINE, pos, state);
	}

	public UUID owner() { return owner; }
	public String ownerName() { return ownerName; }
	public double faith() { return faith; }
	public double devotion() { return devotion; }
	public String request() { return request; }
	public int requestCount() { return requestCount; }
	public boolean hasRequest() { return !request.isEmpty(); }

	void setOwner(UUID owner, String name) {
		this.owner = owner;
		this.ownerName = name == null ? "" : name;
		setChanged();
	}

	/** Its faith set (the game tests). */
	public void setFaith(double faith) {
		this.faith = Math.max(0, Math.min(Shrines.MAX_FAITH, faith));
		setChanged();
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (owner != null) tag.putUUID("Owner", owner);
		tag.putString("OwnerName", ownerName);
		tag.putDouble("Faith", faith);
		tag.putDouble("Devotion", devotion);
		tag.putString("Request", request);
		tag.putInt("RequestCount", requestCount);
		tag.putLong("RequestAt", requestAt);
		tag.putLong("LastWorship", lastWorship);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
		ownerName = tag.getString("OwnerName");
		faith = tag.getDouble("Faith");
		devotion = tag.contains("Devotion") ? tag.getDouble("Devotion") : 1.0;
		request = tag.getString("Request");
		requestCount = tag.getInt("RequestCount");
		requestAt = tag.getLong("RequestAt");
		lastWorship = tag.getLong("LastWorship");
	}
}
