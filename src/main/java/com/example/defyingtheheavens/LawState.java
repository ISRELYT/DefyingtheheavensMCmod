package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;

/**
 * An immortal's hold on the Laws (Laws, phase 3, 2026-10-26), and the Five Decays' reckoning with it (FiveDecays): each Law's level (0 to
 * Laws#MAX_LEVEL) and the insight gathered toward its next, the Law meditation in the Immortal Realm comprehends ({@link #focus}), the
 * levels owed for Decays weathered, for the player to choose ({@link #choices}); and when the next Decay comes, which of its signs it has
 * reached and since when, and whether it was warned of. Saved under "Laws" once any of it is.
 */
public final class LawState {
	private final int[] levels = new int[Law.values().length];
	private final double[] insight = new double[Law.values().length];
	private Law focus;
	private int choices;
	/** The game time the next Decay comes at (0: not yet reckoned). */
	private long decayDue;
	/** The Decay under way: 0 none, 1 to 5 its signs, 6 its judgement (FiveDecays). */
	private int decaySign;
	/** The game time its sign (or judgement) came at. */
	private long decayAt;
	/** Told the day before that the next is near. */
	private boolean decayWarned;

	public int level(Law law) { return levels[law.ordinal()]; }

	public void setLevel(Law law, int level) {
		levels[law.ordinal()] = Math.max(0, Math.min(Laws.MAX_LEVEL, level));
		if (levels[law.ordinal()] >= Laws.MAX_LEVEL) insight[law.ordinal()] = 0;
	}

	/** Every Law's level together (Laws#needed asks for so many at each of the ladder's breakthroughs). */
	public int total() {
		int total = 0;
		for (int level : levels) total += level;
		return total;
	}

	public double insight(Law law) { return insight[law.ordinal()]; }
	public void setInsight(Law law, double seconds) { insight[law.ordinal()] = Math.max(0, seconds); }

	public Law focus() { return focus; }
	public void setFocus(Law law) { this.focus = law; }

	public int choices() { return choices; }
	public void setChoices(int choices) { this.choices = Math.max(0, choices); }

	public long decayDue() { return decayDue; }
	public void setDecayDue(long at) { this.decayDue = Math.max(0, at); }

	public int decaySign() { return decaySign; }
	public long decayAt() { return decayAt; }

	/** The Decay at {@code sign} (0: none under way) since {@code at}. */
	public void setDecay(int sign, long at) {
		this.decaySign = Math.max(0, Math.min(FiveDecays.JUDGEMENT, sign));
		this.decayAt = at;
	}

	public boolean decayWarned() { return decayWarned; }
	public void setDecayWarned(boolean warned) { this.decayWarned = warned; }

	/** Born again with {@code old}'s cultivation (an Eternal Reincarnation Body: PlayerCultivation#carryOver): the Laws as they were. */
	void carryOver(LawState old) {
		System.arraycopy(old.levels, 0, levels, 0, levels.length);
		System.arraycopy(old.insight, 0, insight, 0, insight.length);
		focus = old.focus;
		choices = old.choices;
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		boolean any = focus != null || choices > 0 || decayDue > 0 || decaySign > 0;
		for (int i = 0; i < levels.length; i++) any |= levels[i] > 0 || insight[i] > 0;
		if (!any) return tag;
		// By id, so Laws added later (or their order) keep what is saved.
		CompoundTag laws = new CompoundTag();
		for (Law law : Law.values()) {
			if (levels[law.ordinal()] <= 0 && insight[law.ordinal()] <= 0) continue;
			CompoundTag one = new CompoundTag();
			one.putInt("Level", levels[law.ordinal()]);
			if (insight[law.ordinal()] > 0) one.putDouble("Insight", insight[law.ordinal()]);
			laws.put(law.getId(), one);
		}
		tag.put("Levels", laws);
		if (focus != null) tag.putString("Focus", focus.getId());
		if (choices > 0) tag.putInt("Choices", choices);
		if (decayDue > 0) tag.putLong("DecayDue", decayDue);
		if (decaySign > 0) {
			tag.putInt("DecaySign", decaySign);
			tag.putLong("DecayAt", decayAt);
		}
		if (decayWarned) tag.putBoolean("DecayWarned", true);
		return tag;
	}

	public void load(CompoundTag tag) {
		CompoundTag laws = tag.getCompound("Levels");
		for (Law law : Law.values()) {
			CompoundTag one = laws.getCompound(law.getId());
			levels[law.ordinal()] = Math.max(0, Math.min(Laws.MAX_LEVEL, one.getInt("Level")));
			insight[law.ordinal()] = Math.max(0, one.getDouble("Insight"));
		}
		focus = Law.byId(tag.getString("Focus"));
		choices = Math.max(0, tag.getInt("Choices"));
		decayDue = Math.max(0, tag.getLong("DecayDue"));
		decaySign = Math.max(0, Math.min(FiveDecays.JUDGEMENT, tag.getInt("DecaySign")));
		decayAt = tag.getLong("DecayAt");
		decayWarned = tag.getBoolean("DecayWarned");
	}
}
