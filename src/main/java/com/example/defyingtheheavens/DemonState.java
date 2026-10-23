package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;

/**
 * The Demon Path of a life's record (DemonPath: the third path, the demon races', beside the heaven and ghost paths): where it stands
 * (a rank, PlayerCultivation#rank; -1 not begun: no demon core), what of that stage it has cultivated, whether a meditation draws it
 * in now (the Cultivation tab's switch), whether its tribulation is under way (saved at once, so leaving fails it), and the blood the
 * record is of (Bloodlines keeps it for the world; here it is known wherever only the record is). Saved under "Demon" when begun.
 */
public final class DemonState {
	private int rank = -1;
	private double cultivation;
	private boolean cultivating;
	private boolean inTribulation;
	private Bloodline bloodline = Bloodline.HUMAN;

	public int rank() { return rank; }
	public double cultivation() { return cultivation; }

	/** The path at {@code rank} (-1: not begun), {@code cultivation} into that stage. */
	public void set(int rank, double cultivation) {
		this.rank = Math.max(-1, Math.min(PlayerCultivation.LAST_RANK, rank));
		this.cultivation = this.rank < 0 ? 0 : Math.max(0, cultivation);
	}

	public boolean begun() { return rank >= 0; }

	public boolean cultivating() { return cultivating; }
	public void setCultivating(boolean cultivating) { this.cultivating = cultivating; }

	public boolean inTribulation() { return inTribulation; }
	public void setInTribulation(boolean inTribulation) { this.inTribulation = inTribulation; }

	public Bloodline bloodline() { return bloodline; }
	public void setBloodline(Bloodline bloodline) { this.bloodline = bloodline == null ? Bloodline.HUMAN : bloodline; }

	/** Born again with {@code old}'s cultivation (an Eternal Reincarnation Body: PlayerCultivation#carryOver): the path as it was. */
	void carryOver(DemonState old) {
		rank = old.rank;
		cultivation = old.cultivation;
		cultivating = old.cultivating;
		bloodline = old.bloodline;
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		if (rank < 0 && bloodline == Bloodline.HUMAN) return tag;
		tag.putInt("Rank", rank);
		if (cultivation > 0) tag.putDouble("Cultivation", cultivation);
		if (cultivating) tag.putBoolean("Cultivating", true);
		if (inTribulation) tag.putBoolean("InTribulation", true);
		tag.putString("Bloodline", bloodline.getId());
		return tag;
	}

	public void load(CompoundTag tag) {
		rank = tag.contains("Rank") ? tag.getInt("Rank") : -1;
		cultivation = tag.getDouble("Cultivation");
		cultivating = tag.getBoolean("Cultivating");
		inTribulation = tag.getBoolean("InTribulation");
		Bloodline b = Bloodline.byId(tag.getString("Bloodline"));
		bloodline = b == null ? Bloodline.HUMAN : b;
	}
}
