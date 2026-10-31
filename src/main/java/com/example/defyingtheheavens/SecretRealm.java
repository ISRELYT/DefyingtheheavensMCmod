package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * The secret realms (2026-10-31, the user's picks on the design: "no upper limit for who can enter, just a lower limit so people too weak
 * can't enter"; "flight should be allowed for anyone vastly powerful compared to the secret realm's recommended level"; the rest as
 * designed): sealed worlds the ancients left, each under a Seal in its own world, stirring every few years and open a short while
 * (SecretRealms). Each has its lowest realm (one weaker is turned back at the gate), the realm it is meant for, how often it opens and how
 * long one may stay; its land lies in the secret realms' dimension (ModDimensions#SECRET_REALM) in a slot of its own, laid by its own
 * terrain (SecretRealmGenerator). The first phase brought the Verdant Herb Valley; the second the Sunken Sword Tomb and the Ancient
 * Battlefield; the third the Primeval Beast Graveyard, the Cave Abode of a Heavenly Being and the Yellow Springs Hollow (whose Seal is in
 * the Netherworld, and which admits the dead); the fourth the Jade Lake Mirage (its Seal in the Jade Firmament), the Ruins of a Fallen
 * Holy Land (in the Great Void) and the Tomb of a Fallen Immortal (in the Immortal Realm), the last two floating over the void
 * ({@link #floats}).
 */
public enum SecretRealm {
	/**
	 * Misty terraced herb gardens round a lake, the ruins of an alchemists' sect about it, the Pill Sage's hall on the island at its heart: from
	 * Qi Refining Late, meant for Foundation Building, every three years, a quarter of an hour inside.
	 */
	VERDANT_HERB_VALLEY("verdant_herb_valley", 0, Level.OVERWORLD, Realm.QI_REFINING, Stage.LATE, Realm.FOUNDATION_BUILDING, 3, 15),
	/**
	 * A gorge sunk deep into grey stone, its floor strewn with the swords of a sect that fell there, a spire at its heart with the Sleeping
	 * Sword's shrine on its top and the Sword Path up its face (the second phase, 2026-10-31): from Foundation Building Late, meant for Core
	 * Formation, every four years, twenty minutes inside.
	 */
	SUNKEN_SWORD_TOMB("sunken_sword_tomb", 1, Level.OVERWORLD, Realm.FOUNDATION_BUILDING, Stage.LATE, Realm.CORE_FORMATION, 4, 20),
	/**
	 * The red plain an old war was fought over, trenches, banners and the bones of its dead all over it, the General's hill at its heart (the
	 * second phase, 2026-10-31): from Core Formation Late, meant for Nascent Soul, every five years, twenty minutes inside.
	 */
	ANCIENT_BATTLEFIELD("ancient_battlefield", 2, Level.OVERWORLD, Realm.CORE_FORMATION, Stage.LATE, Realm.NASCENT_SOUL, 5, 20),
	/**
	 * A jungle basin ringed by cliffs, the bones of a primeval dragon lying across its heart, where wild beasts gather and grow past their
	 * kind (the third phase, 2026-10-31): from Core Formation Late, meant for Nascent Soul, every six years, twenty minutes inside.
	 */
	PRIMEVAL_BEAST_GRAVEYARD("primeval_beast_graveyard", 3, Level.OVERWORLD, Realm.CORE_FORMATION, Stage.LATE, Realm.NASCENT_SOUL, 6, 20),
	/**
	 * A lone mountain a Heavenly Being hollowed into his abode, its halls, garden, library and springs behind formations (the third phase):
	 * from Nascent Soul Late, meant for Heavenly Being, every seven years, twenty-five minutes inside.
	 */
	CAVE_ABODE("cave_abode", 4, Level.OVERWORLD, Realm.NASCENT_SOUL, Stage.LATE, Realm.HEAVENLY_BEING, 7, 25),
	/**
	 * A drowned city of the dead under the Netherworld, its Seal there; the dead may enter, and lay hands on things inside (the third
	 * phase): from Nascent Soul Late, meant for Heavenly Being, every five years, twenty minutes inside.
	 */
	YELLOW_SPRINGS_HOLLOW("yellow_springs_hollow", 5, ModDimensions.NETHERWORLD, Realm.NASCENT_SOUL, Stage.LATE, Realm.HEAVENLY_BEING, 5, 20),
	/**
	 * A still lake of pale jade under a pale sky, white isles in it joined by jade bridges (not all of them real), the Mirror Pavilion on the
	 * isle at its heart (the fourth phase, 2026-10-31; its Seal in the Jade Firmament): from Four Axis Late, meant for Dao Integration,
	 * every eight years, twenty-five minutes inside.
	 */
	JADE_LAKE_MIRAGE("jade_lake_mirage", 6, ModDimensions.UPPER_REALM, Realm.FOUR_AXIS, Stage.LATE, Realm.DAO_INTEGRATION, 8, 25),
	/**
	 * The shards of a holy land broken in some war of the ancients, floating in the void with its currents between them, its ancestral hall
	 * on the greatest under a formation (the fourth phase; its Seal in the Great Void): from Sacred Vessel Late, meant for Entering Nirvana,
	 * every ten years, half an hour inside.
	 */
	FALLEN_HOLY_LAND("fallen_holy_land", 7, ModDimensions.GREAT_VOID, Realm.SACRED_VESSEL, Stage.LATE, Realm.ENTERING_NIRVANA, 10, 30),
	/**
	 * A fallen immortal's palace on a floating base over a sea of golden cloud, a hall for each Law round its court, his sanctum sealed
	 * beyond (the fourth phase; its Seal in the Immortal Realm): from Entering Nirvana Late, meant for True Immortal, every twelve years,
	 * half an hour inside.
	 */
	FALLEN_IMMORTAL_TOMB("fallen_immortal_tomb", 8, ModDimensions.IMMORTAL_REALM, Realm.ENTERING_NIRVANA, Stage.LATE, Realm.TRUE_IMMORTAL, 12, 30);

	/** Each realm's land has a spot of its own, this far apart along x (as the small worlds' and the Inner Realm's). */
	public static final int SLOT_SPACING = 4096;
	/** A realm's land reaches this far from its middle; past {@link #BOUND} one is caught and set back on it. */
	public static final int REACH = 168, BOUND = 176;
	/** A realm stirs this long before it opens (half a day: ten minutes), and quakes this long before it closes (two minutes). */
	public static final int STIR_TICKS = 12_000, QUAKE_TICKS = 2 * 60 * 20;
	/** Its gate stands open for this share of its time inside: those who come late may still go in, with less time. */
	public static final int GATE_SHARE = 4;
	/** Two whole realms above the one a realm is meant for, one flies there freely (the user's "vastly powerful"); everyone else keeps low. */
	public static final int FREE_FLIGHT_REALMS = 2;

	private final String id;
	private final int slot;
	private final ResourceKey<Level> home;
	private final int lowest;
	private final Realm lowestRealm;
	private final Stage lowestStage;
	private final Realm meantFor;
	private final int years, minutes;

	SecretRealm(String id, int slot, ResourceKey<Level> home, Realm lowestRealm, Stage lowestStage, Realm meantFor, int years, int minutes) {
		this.id = id;
		this.slot = slot;
		this.home = home;
		this.lowestRealm = lowestRealm;
		this.lowestStage = lowestStage;
		this.lowest = PlayerCultivation.rank(lowestRealm, lowestStage);
		this.meantFor = meantFor;
		this.years = years;
		this.minutes = minutes;
	}

	public String getId() { return id; }

	/** The world its Seal stands in, whose law holds inside it (SecretRealms#lawOf). */
	public ResourceKey<Level> home() { return home; }

	/** The realm it is meant for (its guardians are of it, its heart's at its peak). */
	public Realm meantFor() { return meantFor; }

	/** The weakest rank it lets in (either path's true rank: TwoPaths#higherRank). */
	public int lowest() { return lowest; }

	/** "Qi Refining Late": its lowest realm, named. */
	public Component lowestName() { return PlayerCultivation.rankName(lowestRealm, lowestStage); }

	public Component displayName() { return Component.translatable(ModLang.secretRealm(id)); }

	/** It opens every this many years (in-game days). */
	public int years() { return years; }

	/** Ticks from one opening to the next. */
	public long period() { return years * 24_000L; }

	/** How long it stays open, its gate open for the first {@link #GATE_SHARE}th of it. */
	public int insideTicks() { return minutes * 60 * 20; }

	public int gateTicks() { return insideTicks() / GATE_SHARE; }

	/** Whether one of true rank {@code rank} (their higher path's) may go in: its lowest realm or above; there is no upper limit. */
	public boolean mayEnter(int rank) { return rank >= lowest; }

	/**
	 * Whether one of true rank {@code rank} flies freely in it: {@link #FREE_FLIGHT_REALMS} whole realms above the realm it is meant for, or
	 * more (the Verdant Herb Valley, meant for Foundation Building: Nascent Soul and above). Everyone else is held near the ground.
	 */
	public boolean fliesFreely(int rank) {
		int free = meantFor.ordinal() + FREE_FLIGHT_REALMS;
		return free < Realm.values().length && rank >= PlayerCultivation.rank(Realm.values()[free], Stage.EARLY);
	}

	/** The rank its guardians are drawn round: the realm it is meant for, at {@code stage}. */
	public int guardianRank(Stage stage) { return PlayerCultivation.rank(meantFor, stage); }

	/** Its land (RealmLand: laid by SecretRealmGenerator, built on by its works). */
	public RealmLand land() {
		return switch (this) {
			case VERDANT_HERB_VALLEY -> HerbValley.LAND;
			case SUNKEN_SWORD_TOMB -> SwordTomb.LAND;
			case ANCIENT_BATTLEFIELD -> Battlefield.LAND;
			case PRIMEVAL_BEAST_GRAVEYARD -> BeastGraveyard.LAND;
			case CAVE_ABODE -> CaveAbode.LAND;
			case YELLOW_SPRINGS_HOLLOW -> SpringsHollow.LAND;
			case JADE_LAKE_MIRAGE -> MirageLake.LAND;
			case FALLEN_HOLY_LAND -> FallenHolyLand.LAND;
			case FALLEN_IMMORTAL_TOMB -> ImmortalTomb.LAND;
		};
	}

	/**
	 * Whether its land floats over the void (the Ruins' shards, the Tomb's base): there the sky's hold is measured from its land's level
	 * ({@link #holdFloor}), not from the void's floor, so one held low may still cross between its shards.
	 */
	public boolean floats() {
		return this == FALLEN_HOLY_LAND || this == FALLEN_IMMORTAL_TOMB;
	}

	/** The height the sky's hold is measured from where there is no ground under one: its land's level if it floats, else none (-1). */
	public int holdFloor() {
		return switch (this) {
			case FALLEN_HOLY_LAND -> FallenHolyLand.LEVEL_Y;
			case FALLEN_IMMORTAL_TOMB -> ImmortalTomb.BASE_Y;
			default -> -1;
		};
	}

	/** Its works (RealmWorks): its buildings, guardians, event, trials and inheritance. */
	public RealmWorks works() {
		return switch (this) {
			case VERDANT_HERB_VALLEY -> HerbValleyWorks.WORKS;
			case SUNKEN_SWORD_TOMB -> SwordTombWorks.WORKS;
			case ANCIENT_BATTLEFIELD -> BattlefieldWorks.WORKS;
			case PRIMEVAL_BEAST_GRAVEYARD -> BeastGraveyardWorks.WORKS;
			case CAVE_ABODE -> CaveAbodeWorks.WORKS;
			case YELLOW_SPRINGS_HOLLOW -> SpringsHollowWorks.WORKS;
			case JADE_LAKE_MIRAGE -> MirageLakeWorks.WORKS;
			case FALLEN_HOLY_LAND -> FallenHolyLandWorks.WORKS;
			case FALLEN_IMMORTAL_TOMB -> ImmortalTombWorks.WORKS;
		};
	}

	/** Whether the dead may enter it (only the Yellow Springs Hollow, under the Netherworld; every other admits the living only). */
	public boolean admitsDead() {
		return this == YELLOW_SPRINGS_HOLLOW;
	}

	/** Its biome (ModBiomes: its sky, mist, grass and motes), all over its slot (SecretRealmBiomeSource). */
	public ResourceKey<net.minecraft.world.level.biome.Biome> biome() {
		return switch (this) {
			case VERDANT_HERB_VALLEY -> ModBiomes.HERB_VALLEY;
			case SUNKEN_SWORD_TOMB -> ModBiomes.SWORD_TOMB;
			case ANCIENT_BATTLEFIELD -> ModBiomes.OLD_WAR_FIELD;
			case PRIMEVAL_BEAST_GRAVEYARD -> ModBiomes.BEAST_GRAVEYARD;
			case CAVE_ABODE -> ModBiomes.CAVE_ABODE;
			case YELLOW_SPRINGS_HOLLOW -> ModBiomes.SPRINGS_HOLLOW;
			case JADE_LAKE_MIRAGE -> ModBiomes.MIRAGE_LAKE;
			case FALLEN_HOLY_LAND -> ModBiomes.FALLEN_HOLY_LAND;
			case FALLEN_IMMORTAL_TOMB -> ModBiomes.IMMORTAL_TOMB;
		};
	}

	/** Its land's middle in the secret realms' dimension (y: its heart's ground, RealmLand#centreY). */
	public BlockPos centre() { return new BlockPos(slot * SLOT_SPACING, land().centreY(), 0); }

	/** How far ({@code x}, {@code z}) in the secret realms' dimension lies from this realm's middle, sideways. */
	public double distance(double x, double z) {
		return Math.hypot(x - slot * SLOT_SPACING, z);
	}

	/** The realm whose land lies at ({@code x}, {@code z}) in the secret realms' dimension, or null. */
	public static SecretRealm at(double x, double z) {
		long slot = Math.round(x / SLOT_SPACING);
		if (Math.abs(z) > SLOT_SPACING / 2.0) return null;
		for (SecretRealm realm : values()) if (realm.slot == slot) return realm;
		return null;
	}

	public static SecretRealm byId(String id) {
		for (SecretRealm realm : values()) if (realm.id.equals(id)) return realm;
		return null;
	}
}
