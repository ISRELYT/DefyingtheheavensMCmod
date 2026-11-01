package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;

/**
 * The Body Path of a life's record (BodyPath: the fourth path, the body's own, beside the heaven, ghost and Demon paths; the user's ask of
 * 2026-11-01): where it stands (a rank, PlayerCultivation#rank, its realms the body's own names; -1 not begun), what of that stage it has
 * tempered, whether it is drawn in now (the Cultivation tab's switch: meditation and deeds temper it), whether its tribulation is under
 * way (saved at once, so leaving fails it), and the vigor its techniques spend (BodyTechniques: the body's own pool, whole to begin with).
 * Saved under "Body" when begun.
 */
public final class BodyState {
	private int rank = -1;
	private double cultivation;
	private boolean cultivating;
	private boolean inTribulation;
	private double vigor = BodyTechniques.VIGOR;

	public int rank() { return rank; }
	public double cultivation() { return cultivation; }

	/** The path at {@code rank} (-1: not begun), {@code cultivation} into that stage; vigor past the pool a lower realm holds, gone. */
	public void set(int rank, double cultivation) {
		this.rank = Math.max(-1, Math.min(BodyPath.LAST_RANK, rank));
		this.cultivation = this.rank < 0 ? 0 : Math.max(0, cultivation);
		this.vigor = Math.min(vigor, BodyTechniques.maxVigor(this.rank));
	}

	public boolean begun() { return rank >= 0; }

	public boolean cultivating() { return cultivating; }
	public void setCultivating(boolean cultivating) { this.cultivating = cultivating; }

	public boolean inTribulation() { return inTribulation; }
	public void setInTribulation(boolean inTribulation) { this.inTribulation = inTribulation; }

	/** The vigor the body's techniques spend (BodyTechniques), 0 to its realm's pool (BodyTechniques#maxVigor). */
	public double vigor() { return vigor; }
	public void setVigor(double vigor) { this.vigor = Math.max(0, Math.min(BodyTechniques.maxVigor(rank), vigor)); }

	/** Born again with {@code old}'s cultivation (an Eternal Reincarnation Body: PlayerCultivation#carryOver): the path as it was. */
	void carryOver(BodyState old) {
		rank = old.rank;
		cultivation = old.cultivation;
		cultivating = old.cultivating;
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		if (rank < 0) return tag;
		tag.putInt("Rank", rank);
		if (cultivation > 0) tag.putDouble("Cultivation", cultivation);
		if (cultivating) tag.putBoolean("Cultivating", true);
		if (inTribulation) tag.putBoolean("InTribulation", true);
		if (vigor < BodyTechniques.maxVigor(rank)) tag.putDouble("Vigor", vigor);
		return tag;
	}

	public void load(CompoundTag tag) {
		set(tag.contains("Rank") ? tag.getInt("Rank") : -1, tag.getDouble("Cultivation"));
		cultivating = tag.getBoolean("Cultivating");
		inTribulation = tag.getBoolean("InTribulation");
		// Whole when none was saved: a full pool (saved only short of it), or a record from before the techniques.
		setVigor(tag.contains("Vigor") ? tag.getDouble("Vigor") : BodyTechniques.maxVigor(rank));
	}
}
