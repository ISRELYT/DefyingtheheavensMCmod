package com.example.defyingtheheavens;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * The Great Void Realm's Holy Lands (the user's pick, 2026-10-25): its sects, and the only ones there. Rare (one in some 2,000 x
 * 2,000 blocks: structure set holy_lands), always of the largest grounds and laid out on a continent (SectStructure), built of white
 * jade and gold (SectPalette#holy), and of {@link #MEMBERS}: a Sacred Vessel Patriarch, two Void Shattering Grand Elders, four Dao
 * Integration Elders and thirteen disciples of Heavenly Being to Four Axis ({@link #foundingRanks}; the titles fall to them by
 * strength, as in any sect: Sect#titleCounts gives twenty one master, two grand elders, four elders). Its new disciples come at
 * {@link #FLOOR}. Joined as any sect is (SectMembership); its master is named its Patriarch (NpcTitle#getDisplayName(boolean, boolean)).
 */
public final class HolyLands {
	/** A Holy Land's members (the largest grounds' most, SectStructure). */
	public static final int MEMBERS = 20;
	/** Its newest disciples' rank: a Heavenly Being's first. */
	public static final int FLOOR = PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY);
	/** Its disciples run up to this (Four Axis Grand Perfection), leaning low. */
	private static final int TOP_DISCIPLE = PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION);
	private static final double DISCIPLE_LEAN = 1.3;

	/** Whether a sect in {@code dimension} is a Holy Land: every sect of the Great Void is. */
	public static boolean isHolyLand(ResourceKey<Level> dimension) {
		return ModDimensions.isGreatVoid(dimension);
	}

	/**
	 * A Holy Land's founding ranks, its Patriarch first: of Sacred Vessel, then two of Void Shattering, four of Dao Integration (each at
	 * any stage of its realm), and the rest disciples from {@link #FLOOR} to Four Axis Grand Perfection. HighRealms holds each to the
	 * room the Great Void has for its realm (CultivatorNpc#setRank).
	 */
	public static int[] foundingRanks(RandomSource random, int count) {
		int[] ranks = new int[count];
		int stages = Stage.values().length;
		for (int i = 0; i < count; i++) {
			Realm realm = i == 0 ? Realm.SACRED_VESSEL : i <= 2 ? Realm.VOID_SHATTERING : i <= 6 ? Realm.DAO_INTEGRATION : null;
			ranks[i] = realm != null ? PlayerCultivation.rank(realm, Stage.byIndex(random.nextInt(stages)))
					: FLOOR + (int) ((TOP_DISCIPLE - FLOOR + 1) * Math.pow(random.nextFloat(), DISCIPLE_LEAN));
		}
		return ranks;
	}

	private HolyLands() {}
}
