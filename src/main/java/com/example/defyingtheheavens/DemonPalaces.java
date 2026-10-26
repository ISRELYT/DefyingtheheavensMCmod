package com.example.defyingtheheavens;

import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

/**
 * The Great Void's Demon Palaces (the user's yes, 2026-10-25): its demon clans, a tier above the clans' dens as the Holy Lands are
 * above the sects, and the only clans there. One race each, by its land ({@link #raceOf}): the metal Dao field's White Tiger, the
 * fire field's Nine-Tailed Fox, the earth field's lizard king, the wood and water fields' Azure Dragon; on the steppes any race, a
 * dragon's most often (DenStructure#upperRace). As rare as the Holy Lands, whose sites they share (structure set holy_lands: one or
 * the other at a site, each about one in 2,000 x 2,000 blocks), laid out by DemonPalacePlan and built in dark stone in the race's
 * colours (SectPalette#palace) with its great beasts (DemonPalacePieces, PalaceSculpt). Of a Holy Land's {@link #MEMBERS}
 * (HolyLands#foundingRanks: a Sacred Vessel Demon Sovereign, two of Void Shattering, four of Dao Integration, the rest disciples of
 * Heavenly Being to Four Axis), all of its race; joined as any clan is, by demons of its race (SectMembership); its master its Demon
 * Sovereign (NpcTitle#getDisplayName(boolean, boolean)).
 */
public final class DemonPalaces {
	/** A palace's members: a Holy Land's. */
	public static final int MEMBERS = HolyLands.MEMBERS;

	/** The race whose palace stands on {@code biome}: the Dao fields' own (the elders' elements), on the steppes any race. */
	public static Bloodline raceOf(Holder<Biome> biome, RandomSource random) {
		if (biome.is(ModBiomes.DAO_FIELD_METAL)) return Bloodline.TIGER;
		if (biome.is(ModBiomes.DAO_FIELD_FIRE)) return Bloodline.FOX;
		if (biome.is(ModBiomes.DAO_FIELD_EARTH)) return Bloodline.LIZARD;
		if (biome.is(ModBiomes.DAO_FIELD_WOOD) || biome.is(ModBiomes.DAO_FIELD_WATER)) return Bloodline.DRAGON;
		return DenStructure.upperRace(random);
	}

	/** Whether {@code sect} is a Demon Palace: a clan of the Great Void. */
	public static boolean isPalace(Sect sect) {
		return sect.isClan() && sect.isHoly();
	}

	private DemonPalaces() {}
}
