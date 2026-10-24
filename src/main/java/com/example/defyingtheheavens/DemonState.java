package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;

/**
 * The Demon Path of a life's record (DemonPath: the third path, the demon races', beside the heaven and ghost paths): where it stands
 * (a rank, PlayerCultivation#rank; -1 not begun: no demon core), what of that stage it has cultivated, whether a meditation draws it
 * in now (the Cultivation tab's switch), whether its tribulation is under way (saved at once, so leaving fails it), and the blood the
 * record is of (Bloodlines keeps it for the world; here it is known wherever only the record is). Saved under "Demon" when begun.
 * <p>
 * And what of a race's blood it has drunk (DemonBlood, the user's ask, 2026-10-24): the realm its blood is awakened to (the Demon
 * Path's methods, as it were: a realm it hasn't is shut to it), a human's half-blood (the race whose blood it drank, its look its own)
 * and how pure that blood has grown in it (at the whole, it is that race's).
 */
public final class DemonState {
	private int rank = -1;
	private double cultivation;
	private boolean cultivating;
	private boolean inTribulation;
	private Bloodline bloodline = Bloodline.HUMAN;
	/** The realm (ordinal) its blood is awakened to: -1 none. */
	private int bloodRealm = -1;
	/** A human's half-blood: the race whose blood it drank (null: none, or one born to its race). */
	private Bloodline halfBlood;
	/** How pure a half-blood's blood has grown, 0 to 1. */
	private double purity;

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

	public int bloodRealm() { return bloodRealm; }
	public void setBloodRealm(int realm) { this.bloodRealm = Math.max(-1, Math.min(Realm.values().length - 1, realm)); }

	public Bloodline halfBlood() { return halfBlood; }
	public void setHalfBlood(Bloodline race) { this.halfBlood = race == null || !race.isBeast() ? null : race; }

	public double purity() { return purity; }
	public void setPurity(double purity) { this.purity = Math.max(0, Math.min(1, purity)); }

	/** Born again with {@code old}'s cultivation (an Eternal Reincarnation Body: PlayerCultivation#carryOver): the path as it was. */
	void carryOver(DemonState old) {
		rank = old.rank;
		cultivation = old.cultivation;
		cultivating = old.cultivating;
		bloodline = old.bloodline;
		bloodRealm = old.bloodRealm;
		halfBlood = old.halfBlood;
		purity = old.purity;
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		if (rank < 0 && bloodline == Bloodline.HUMAN && halfBlood == null) return tag;
		tag.putInt("Rank", rank);
		if (cultivation > 0) tag.putDouble("Cultivation", cultivation);
		if (cultivating) tag.putBoolean("Cultivating", true);
		if (inTribulation) tag.putBoolean("InTribulation", true);
		tag.putString("Bloodline", bloodline.getId());
		tag.putInt("BloodRealm", bloodRealm);
		if (halfBlood != null) tag.putString("HalfBlood", halfBlood.getId());
		if (purity > 0) tag.putDouble("Purity", purity);
		return tag;
	}

	public void load(CompoundTag tag) {
		rank = tag.contains("Rank") ? tag.getInt("Rank") : -1;
		cultivation = tag.getDouble("Cultivation");
		cultivating = tag.getBoolean("Cultivating");
		inTribulation = tag.getBoolean("InTribulation");
		Bloodline b = Bloodline.byId(tag.getString("Bloodline"));
		bloodline = b == null ? Bloodline.HUMAN : b;
		// A record from before blood (2026-10-24): its blood awakened as far as its Demon Path has come, so nothing it reached is shut.
		bloodRealm = tag.contains("BloodRealm") ? tag.getInt("BloodRealm") : rank < 0 ? -1 : DemonPath.realm(rank).ordinal();
		Bloodline half = Bloodline.byId(tag.getString("HalfBlood"));
		halfBlood = half != null && half.isBeast() ? half : null;
		purity = tag.getDouble("Purity");
	}
}
