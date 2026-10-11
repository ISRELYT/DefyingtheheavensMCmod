package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/**
 * What a cultivator thinks of a player, from {@link #MIN} to {@link #MAX} (see {@link TalkSystem}). Three parts add up:
 * <ul>
 *   <li>a <b>baseline</b> from their paths: +10 between two of the same strong path, -10 when the player leans the other way;</li>
 *   <li>the cultivator's own <b>personal</b> regard ({@link Entry}), from greetings, gifts, insults and blows, drifting a point
 *   a day back toward zero;</li>
 *   <li>its sect's <b>standing</b> with the player ({@link Sect#standing}), from what the player has done to the sect as a
 *   whole: killing a member, robbing the treasury, fighting beside one. Sect grudges don't fade.</li>
 * </ul>
 * Shown as five pips, one per {@link #PER_PIP}: Polite, Courteous, Warm, Trusted, Sworn; below zero, Resentful, and at
 * {@link #HOSTILE} or lower they attack the player if they think they can win.
 */
public final class Regard {
	public static final int MIN = -100;
	public static final int MAX = 100;
	public static final int PER_PIP = 20;
	public static final int HOSTILE = -50;

	public static final int GREET = 2;
	public static final int PROVOKE = -15;
	public static final int STRUCK = -30;
	public static final int MEMBER_KILLED = -40;
	public static final int TREASURY_ROBBED = -60;
	public static final int DEFENDED = 20;
	public static final int DEFENDED_SECT = 5;
	public static final int DISTURBED = -2;
	public static final int GIFT_MAX = 25;

	/** One cultivator's own regard for one player, and what it remembers of their dealings. */
	public static final class Entry {
		public int value;
		/** The game day {@link #value} was last brought up to date (its drift toward zero). */
		public long day;
		public long greetDay = -1;
		public String giftItem = "";
		public long giftDay = -1;
		public int giftCount;

		/** Brings {@link #value} up to {@code today}: a point a day toward zero. */
		public int current(long today) {
			if (today > day) {
				int drift = (int) Math.min(Integer.MAX_VALUE, today - day);
				value = value > 0 ? Math.max(0, value - drift) : Math.min(0, value + drift);
				day = today;
			}
			return value;
		}

		public CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putInt("Value", value);
			tag.putLong("Day", day);
			tag.putLong("GreetDay", greetDay);
			tag.putString("GiftItem", giftItem);
			tag.putLong("GiftDay", giftDay);
			tag.putInt("GiftCount", giftCount);
			return tag;
		}

		public static Entry load(CompoundTag tag) {
			Entry entry = new Entry();
			entry.value = tag.getInt("Value");
			entry.day = tag.getLong("Day");
			entry.greetDay = tag.getLong("GreetDay");
			entry.giftItem = tag.getString("GiftItem");
			entry.giftDay = tag.getLong("GiftDay");
			entry.giftCount = tag.getInt("GiftCount");
			return entry;
		}
	}

	/** The game day, for once-a-day things and the drift (game time, so /time set doesn't move it). */
	public static long day(long gameTime) {
		return gameTime / 24_000L;
	}

	/** Where regard starts from their paths: a righteous cultivator warms to the righteous and cools to those leaning demonic. */
	public static int baseline(Alignment.Faction faction, int playerAlignment) {
		Alignment.Faction player = Alignment.factionOf(playerAlignment);
		return switch (faction) {
			case RIGHTEOUS -> player == Alignment.Faction.RIGHTEOUS ? 10 : playerAlignment < 0 ? -10 : 0;
			case DEMONIC -> player == Alignment.Faction.DEMONIC ? 10 : playerAlignment > 0 ? -10 : 0;
			case NEUTRAL -> 0;
		};
	}

	public static int clamp(int regard) {
		return Mth.clamp(regard, MIN, MAX);
	}

	/** Pips shown, 0 to 5 (none below zero). */
	public static int pips(int regard) {
		return Mth.clamp(regard / PER_PIP, 0, 5);
	}

	/** The word for a regard: hostile, resentful, indifferent, polite, courteous, warm, trusted, sworn. */
	public static String levelId(int regard) {
		if (regard <= HOSTILE) return "hostile";
		if (regard < 0) return "resentful";
		return switch (pips(regard)) {
			case 0 -> "indifferent";
			case 1 -> "polite";
			case 2 -> "courteous";
			case 3 -> "warm";
			case 4 -> "trusted";
			default -> "sworn";
		};
	}

	private Regard() {}
}
