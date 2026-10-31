package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * A body (CorpseBlock, Corpses): whose it was (a player's name and profile, for their skin; or a cultivator's name and skin), a demon's race
 * and colouring (its beast's head on the body: ClientBloodlines), the dead's rank (its yin, its blood), which way it lay, when it died,
 * whether it has been bled, and everything its death dropped. The client is told all but what it holds.
 */
public class CorpseBlockEntity extends BlockEntity {
	private String name = "";
	/** A player's profile (their skin), null for a cultivator's body. */
	private GameProfile profile;
	/** A cultivator's skin (CultivatorNpc#getSkin), -1 for a player's. */
	private int skin = -1;
	/** A demon's race (Bloodline#getId; "" for a man's look), its colouring and a fox spirit's tails. */
	private String race = "";
	private int raceVariant;
	private int tails = 1;
	/** The dead's realm and stage as one number (TwoPaths#higherRank, CultivatorNpc#getRank), -1 for a mortal. */
	private int rank = -1;
	private float yaw;
	private long diedAt;
	private boolean bled;
	/** Laid as one who died long ago ({@link #ancient}: a secret realm's own, its works may lay it anew), not a fresh death. */
	private boolean old;
	private final List<ItemStack> held = new ArrayList<>();

	public CorpseBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CORPSE, pos, state);
	}

	/** {@code dead}'s body, dead at {@code now}: a player's (their own record's rank and race), or a cultivator's. */
	void of(LivingEntity dead, long now) {
		yaw = dead.getYRot();
		diedAt = now;
		Bloodline bloodline = Bloodline.HUMAN;
		if (dead instanceof ServerPlayer player) {
			profile = player.getGameProfile();
			name = profile.getName();
			PlayerCultivation c = CultivationManager.own(player);
			rank = c.isMortal() ? -1 : Math.max(-1, TwoPaths.higherRank(c, false));
			Bloodlines.Entry entry = Bloodlines.entry(player.server, player.getUUID());
			if (entry != null) {
				bloodline = entry.bloodline();
				raceVariant = entry.variant();
				tails = Bloodlines.tails(player.server, player.getUUID());
			}
		} else if (dead instanceof CultivatorNpc npc) {
			name = npc.hasCustomName() ? npc.getCustomName().getString() : npc.getPersonalName();
			skin = npc.getSkin();
			rank = npc.isMortal() ? -1 : npc.getRank();
			bloodline = npc.getBloodline();
			raceVariant = npc.getRaceVariant();
			tails = Bloodline.tails(npc.getDisplayedRealm());
		}
		race = bloodline != null && bloodline.isBeast() ? bloodline.getId() : "";
		if (level instanceof ServerLevel server) Corpses.lies(server, worldPosition, rank); // its yin by the rank now known
		setChanged();
	}

	/**
	 * The bones of one who died long ago (a secret realm's earlier visitor, or its master, HerbValleyWorks): named {@code name}, of a
	 * cultivator's look ({@code skin}) and {@code rank}, lying at {@code yaw}, bare bones these many years, {@code treasures} still on them.
	 */
	public void ancient(String name, int skin, int rank, float yaw, long now, List<ItemStack> treasures) {
		this.name = name;
		this.skin = Math.max(0, skin);
		this.rank = rank;
		this.yaw = yaw;
		profile = null;
		race = "";
		diedAt = now - Corpses.BONES_AT - 24_000L;
		old = true;
		held.clear();
		for (ItemStack stack : treasures) hold(stack);
		if (level instanceof ServerLevel server) Corpses.lies(server, worldPosition, rank);
		setChanged();
	}

	/** Whether it was laid as one who died long ago (a secret realm's own bones), not a fresh death. */
	public boolean isAncient() { return old; }

	/** {@code stack} laid in the body. */
	void hold(ItemStack stack) {
		if (stack.isEmpty()) return;
		held.add(stack.copy());
		setChanged();
	}

	/** Everything it holds, taken out (it spills: CorpseBlock#onRemove). */
	List<ItemStack> takeAll() {
		List<ItemStack> all = new ArrayList<>(held);
		held.clear();
		setChanged();
		return all;
	}

	void markBled() {
		bled = true;
		setChanged();
	}

	/** Dead {@code ticks} longer (the game tests: its rot without the minutes' wait). */
	void backdate(long ticks) {
		diedAt -= ticks;
		setChanged();
	}

	public String name() { return name; }
	public GameProfile profile() { return profile; }
	public int skin() { return skin; }
	/** The demon race the dead was of, or null for a man's look. */
	public Bloodline race() { return race.isEmpty() ? null : Bloodline.byId(race); }
	public int raceVariant() { return raceVariant; }
	public int tails() { return tails; }
	public int rank() { return rank; }
	public float yaw() { return yaw; }
	public long diedAt() { return diedAt; }
	public boolean bled() { return bled; }
	/** What it holds (not to be changed from outside). */
	public List<ItemStack> held() { return held; }

	// --- Its yin's place among the bodies (Corpses#yinAt) ---

	@Override
	public void setLevel(Level level) {
		super.setLevel(level);
		if (level instanceof ServerLevel server) Corpses.lies(server, worldPosition, rank);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (level instanceof ServerLevel server) Corpses.gone(server, worldPosition);
	}

	// --- Saved, and told ---

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		saveLook(tag);
		tag.putLong("DiedAt", diedAt);
		tag.putBoolean("Bled", bled);
		if (old) tag.putBoolean("Ancient", true);
		ListTag items = new ListTag();
		for (ItemStack stack : held) items.add(stack.save(new CompoundTag()));
		tag.put("Held", items);
	}

	/** What the client needs to draw it: whose it was, its look, its rank, which way it lies. */
	private void saveLook(CompoundTag tag) {
		tag.putString("Name", name);
		if (profile != null) tag.put("Profile", NbtUtils.writeGameProfile(new CompoundTag(), profile));
		tag.putInt("Skin", skin);
		tag.putString("Race", race);
		tag.putInt("RaceVariant", raceVariant);
		tag.putInt("Tails", tails);
		tag.putInt("Rank", rank);
		tag.putFloat("Yaw", yaw);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		name = tag.getString("Name");
		profile = tag.contains("Profile", Tag.TAG_COMPOUND) ? NbtUtils.readGameProfile(tag.getCompound("Profile")) : null;
		skin = tag.contains("Skin") ? tag.getInt("Skin") : -1;
		race = tag.getString("Race");
		raceVariant = tag.getInt("RaceVariant");
		tails = Math.max(1, tag.getInt("Tails"));
		rank = tag.contains("Rank") ? tag.getInt("Rank") : -1;
		yaw = tag.getFloat("Yaw");
		if (tag.contains("DiedAt")) diedAt = tag.getLong("DiedAt");
		if (tag.contains("Bled")) bled = tag.getBoolean("Bled");
		old = tag.getBoolean("Ancient");
		if (tag.contains("Held", Tag.TAG_LIST)) {
			held.clear();
			ListTag items = tag.getList("Held", Tag.TAG_COMPOUND);
			for (int i = 0; i < items.size(); i++) {
				ItemStack stack = ItemStack.of(items.getCompound(i));
				if (!stack.isEmpty()) held.add(stack);
			}
		}
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = new CompoundTag();
		saveLook(tag);
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
