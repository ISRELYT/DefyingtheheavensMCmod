package com.example.defyingtheheavens;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * The Immortal Realm's sects (phase 2 of the realm plan, 2026-10-25: "Immortal sects. Joinable, with True Immortal libraries. NO
 * steles"), the tier above the Holy Lands and laid out as none below it is (ImmortalSectPlan: a court on the ground and five peaks floating
 * over it). Of {@link #MEMBERS}: a Sect Lord of the Earth Immortals (phase 3, 2026-10-26: the first realm past True Immortal), two grand elders of True Immortal Late or Grand Perfection,
 * four elders of True Immortal Early or Mid and thirteen disciples of Entering Nirvana ({@link #foundingRanks}: the titles fall to them by
 * strength, as in any sect). None of them below Entering Nirvana (the user's rule for the realm, 2026-10-25), its new disciples coming at
 * {@link #FLOOR}. Their library holds the True Immortal heaven methods' slips (chests/sect_library, the realm's own pool), their elders
 * teach theirs (TalkSystem#teachHeaven); joined as any sect is (SectMembership), its master named its Sect Lord.
 */
public final class ImmortalSects {
	/** An immortal sect's members. */
	public static final int MEMBERS = 20;
	/** Its newest disciples' rank: Entering Nirvana's first (none below it in the Immortal Realm). */
	public static final int FLOOR = PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.EARLY);
	/** Its disciples run up to this (Entering Nirvana Grand Perfection), leaning low. */
	private static final int TOP_DISCIPLE = PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION);
	private static final double DISCIPLE_LEAN = 1.2;

	/** Whether a sect in {@code dimension} is an immortal sect: every sect of the Immortal Realm is. */
	public static boolean isImmortalSect(ResourceKey<Level> dimension) {
		return ModDimensions.isImmortalRealm(dimension);
	}

	/**
	 * An immortal sect's founding ranks, its Sect Lord first: an Earth Immortal of Early to Late, then two of True Immortal Late or Grand
	 * Perfection, four of True Immortal Early or Mid, and the rest disciples from {@link #FLOOR} to Entering Nirvana Grand Perfection.
	 * HighRealms holds each to the room the Immortal Realm has for its realm (CultivatorNpc#setRank): one turned away stands at the
	 * Grand Perfection of the highest realm below with room (True Immortal's, Entering Nirvana's).
	 */
	public static int[] foundingRanks(RandomSource random, int count) {
		int[] ranks = new int[count];
		for (int i = 0; i < count; i++) {
			if (i == 0) ranks[i] = PlayerCultivation.rank(Realm.EARTH_IMMORTAL, Stage.byIndex(random.nextInt(3)));
			else if (i <= 2) ranks[i] = PlayerCultivation.rank(Realm.TRUE_IMMORTAL, random.nextBoolean() ? Stage.LATE : Stage.GRAND_PERFECTION);
			else if (i <= 6) ranks[i] = PlayerCultivation.rank(Realm.TRUE_IMMORTAL, random.nextBoolean() ? Stage.EARLY : Stage.MID);
			else ranks[i] = FLOOR + (int) ((TOP_DISCIPLE - FLOOR + 1) * Math.pow(random.nextFloat(), DISCIPLE_LEAN));
		}
		return ranks;
	}

	private ImmortalSects() {}
}
