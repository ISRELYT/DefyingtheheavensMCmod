package com.example.defyingtheheavens;

import net.minecraft.util.RandomSource;

/** Names for newly founded sects, in the manner of their path: "Azure Cloud Sect", "Blood Moon Palace", "Misty Jade Valley". */
public final class SectNames {
	private static final String[] RIGHTEOUS_FIRST = {"Azure", "Heavenly", "Pure", "Radiant", "Celestial", "Jade", "Golden", "Crane", "Morning",
			"Clear", "Lotus", "Thousand", "Sword", "Profound", "Serene", "Dragon"};
	private static final String[] RIGHTEOUS_SECOND = {"Cloud", "Sword", "Lotus", "Peak", "Sun", "Sky", "Spring", "Light", "Crane", "Pine",
			"Heart", "Mountain", "Phoenix", "Sea"};
	private static final String[] DEVILISH_FIRST = {"Blood", "Crimson", "Nine", "Black", "Shadow", "Bone", "Ghost", "Scarlet", "Withered",
			"Abyssal", "Hungry", "Iron", "Ashen", "Venom"};
	private static final String[] DEVILISH_SECOND = {"Moon", "Netherworld", "Lotus", "Serpent", "Soul", "Flame", "Fiend", "Shroud", "Skull",
			"Abyss", "Corpse", "Thorn", "Mist", "Gate"};
	private static final String[] NEUTRAL_FIRST = {"Misty", "Hidden", "Wandering", "Quiet", "Silver", "Bamboo", "Ancient", "Floating",
			"Distant", "Moonlit", "Willow", "Stone", "Autumn", "River"};
	private static final String[] NEUTRAL_SECOND = {"Valley", "Pavilion", "Mist", "Stream", "Bamboo", "Lake", "Cliff", "Wind", "Gate",
			"Bell", "Leaf", "Garden", "Tower", "Brook"};
	private static final String[] RIGHTEOUS_KIND = {"Sect", "Sect", "Sect", "Palace", "School", "Pavilion"};
	private static final String[] DEVILISH_KIND = {"Sect", "Sect", "Palace", "Hall", "Cult", "Temple"};
	private static final String[] NEUTRAL_KIND = {"Sect", "Sect", "Pavilion", "Valley", "Hall", "Manor"};

	public static String generate(RandomSource random, Alignment.Faction faction) {
		String[] first, second, kind;
		switch (faction) {
			case RIGHTEOUS -> {
				first = RIGHTEOUS_FIRST;
				second = RIGHTEOUS_SECOND;
				kind = RIGHTEOUS_KIND;
			}
			case DEVILISH -> {
				first = DEVILISH_FIRST;
				second = DEVILISH_SECOND;
				kind = DEVILISH_KIND;
			}
			default -> {
				first = NEUTRAL_FIRST;
				second = NEUTRAL_SECOND;
				kind = NEUTRAL_KIND;
			}
		}
		String a = first[random.nextInt(first.length)];
		String b = second[random.nextInt(second.length)];
		for (int i = 0; i < 4 && b.equals(a); i++) b = second[random.nextInt(second.length)];
		String k = kind[random.nextInt(kind.length)];
		if (k.equals(b)) k = "Sect";
		return a + " " + b + " " + k;
	}

	/** A Holy Land's two words, by its path, before "Holy Land" (HolyLands): "Primordial Sun Holy Land", "Nine Nether Holy Land", "Hidden Moon Holy Land". */
	private static final String[][] HOLY_FIRST = {
			{"Primordial", "Myriad", "Supreme", "Boundless", "Radiant", "Eternal", "Heavenly", "Azure", "Golden", "Starlit", "Pure", "Profound"},
			{"Nine", "Blood", "Abyssal", "Black", "Crimson", "Withered", "Bone", "Scarlet", "Endless", "Ashen"},
			{"Great", "Ancient", "Hidden", "Silent", "Distant", "Moonlit", "Misty", "Wandering", "Floating", "Silver"}};
	private static final String[][] HOLY_SECOND = {
			{"Sun", "Dao", "Star", "Lotus", "Origin", "Sword", "Light", "Heaven", "Jade Pool", "Phoenix", "Crane", "Cloud"},
			{"Nether", "Sea", "Moon", "Soul", "Flame", "Abyss", "Shroud", "Skull", "Night", "Serpent"},
			{"Void", "Mist", "Moon", "Stream", "Mountain", "Bell", "Cloud", "Sky", "Valley", "Lake"}};

	/** A Holy Land's name (HolyLands): two words of its path's, then "Holy Land". */
	public static String holy(RandomSource random, Alignment.Faction faction) {
		int f = faction == Alignment.Faction.RIGHTEOUS ? 0 : faction == Alignment.Faction.DEVILISH ? 1 : 2;
		String a = HOLY_FIRST[f][random.nextInt(HOLY_FIRST[f].length)];
		String b = HOLY_SECOND[f][random.nextInt(HOLY_SECOND[f].length)];
		// Never two words of one stem ("Moonlit Moon").
		for (int i = 0; i < 6 && a.regionMatches(true, 0, b, 0, Math.min(4, Math.min(a.length(), b.length()))); i++) {
			b = HOLY_SECOND[f][random.nextInt(HOLY_SECOND[f].length)];
		}
		return a + " " + b + " Holy Land";
	}

	/** An immortal sect's words, by its path, before its kind (ImmortalSects): "Nine Heavens Immortal Sect", "Blood Sea Immortal Palace". */
	private static final String[][] IMMORTAL_WORDS = {
			{"Nine Heavens", "Jade Void", "Myriad Swords", "Celestial Crane", "Golden Lotus", "Eternal Spring", "Azure Cloud", "Primordial Light",
					"Heaven Piercing", "Purple Mist", "Supreme Pure", "Radiant Sun"},
			{"Blood Sea", "Nine Nether", "Soul Devouring", "Crimson Moon", "Abyssal Flame", "Black Lotus", "Bone Throne", "Withering Star",
					"Heaven Defying", "Shadow Abyss"},
			{"Floating Peaks", "Misty Cloud", "Hidden Dao", "Silver Moon", "Distant Star", "Wandering Wind", "Quiet Lake", "Ancient Pine",
					"Cloud Sea", "Still Water"}};
	private static final String[] IMMORTAL_KINDS = {"Immortal Sect", "Immortal Sect", "Immortal Sect", "Immortal Palace", "Immortal Mountain"};

	/** An immortal sect's name (ImmortalSects): its path's words, then "Immortal Sect" (now and then "Immortal Palace" or "Immortal Mountain"). */
	public static String immortal(RandomSource random, Alignment.Faction faction) {
		int f = faction == Alignment.Faction.RIGHTEOUS ? 0 : faction == Alignment.Faction.DEVILISH ? 1 : 2;
		return IMMORTAL_WORDS[f][random.nextInt(IMMORTAL_WORDS[f].length)] + " " + IMMORTAL_KINDS[random.nextInt(IMMORTAL_KINDS.length)];
	}

	/** A demon clan's two words, by its race, before "<Race> Clan": "Iron Fang Tiger Clan", "Jade Scale Lizard Clan", "Nine Tails Fox Clan". */
	private static final String[][] CLAN_FIRST = {
			{"Iron", "Golden", "White", "Black", "Thunder", "Mountain", "Bloodstripe", "Savage", "Roaring", "Ember"},
			{"Jade", "Black", "Crimson", "Mire", "Sunbaked", "Sunken", "Stone", "Venom", "Silent", "Old"},
			{"Nine", "Red", "Silver", "Moonlit", "Misty", "Snow", "Laughing", "Shrine", "Foxfire", "Hidden"},
			{"Azure", "Golden", "Crimson", "Cloud", "Storm", "Heavenly", "Pearl", "Ancient", "Thunder", "Celestial"}};
	private static final String[][] CLAN_SECOND = {
			{"Fang", "Claw", "Stripe", "Roar", "Pelt", "Peak", "Forest", "Hunt"},
			{"Scale", "Tongue", "Marsh", "Coil", "Sand", "Fen", "Hide", "Root"},
			{"Tails", "Moon", "Shrine", "Fire", "Whisper", "Grove", "Mask", "Lantern"},
			{"Cloud", "Pearl", "Horn", "Scale", "Mountain", "Mist", "Whisker", "Gate"}};

	/** A demon clan's name: two words of its race's, then the race and "Clan" (the user's ask, 2026-10-23). */
	public static String clan(RandomSource random, Bloodline race) {
		int r = Math.max(0, Math.min(CLAN_FIRST.length - 1, race.ordinal() - 1)); // HUMAN is first; the demon races after it
		String a = CLAN_FIRST[r][random.nextInt(CLAN_FIRST[r].length)];
		String b = CLAN_SECOND[r][random.nextInt(CLAN_SECOND[r].length)];
		// Never two words of one stem ("Stone Stone", a "Moonlit Moon").
		for (int i = 0; i < 6 && a.regionMatches(true, 0, b, 0, Math.min(4, Math.min(a.length(), b.length()))); i++) {
			b = CLAN_SECOND[r][random.nextInt(CLAN_SECOND[r].length)];
		}
		// (The race's word from its id: a server has no mod language to read a display name in. Sects' names are English too.)
		String word = Character.toUpperCase(race.getId().charAt(0)) + race.getId().substring(1);
		return a + " " + b + " " + word + " Clan";
	}

	/** A Demon Palace's word, by its race, before "<Race> Demon Palace": "Roaring Tiger Demon Palace", "Moonlit Fox Demon Palace". */
	private static final String[][] PALACE_FIRST = {
			{"White", "Roaring", "Iron", "Golden", "Thunder", "Ten Thousand Fang", "Blood Moon", "Silver", "Heaven Devouring", "Black"},
			{"Mountain Bearing", "Ancient", "Jade", "Earth Shaking", "Black Scale", "Stone Heart", "Myriad Scale", "Sunken", "Deep", "Old"},
			{"Nine Tailed", "Moonlit", "Crimson", "Foxfire", "Heavenly", "Silver", "Thousand Mirage", "Red Lotus", "Snow", "Dream"},
			{"Azure", "Celestial", "Golden", "Storm", "Heaven Soaring", "Pearl", "Cloud Coiling", "Thunder", "Ancient", "Myriad"}};

	/** A Demon Palace's name (DemonPalaces): a word of its race's, then the race and "Demon Palace" (the user's name for them). */
	public static String palace(RandomSource random, Bloodline race) {
		int r = Math.max(0, Math.min(PALACE_FIRST.length - 1, race.ordinal() - 1)); // HUMAN is first; the demon races after it
		String word = Character.toUpperCase(race.getId().charAt(0)) + race.getId().substring(1);
		return PALACE_FIRST[r][random.nextInt(PALACE_FIRST[r].length)] + " " + word + " Demon Palace";
	}

	private SectNames() {}
}
