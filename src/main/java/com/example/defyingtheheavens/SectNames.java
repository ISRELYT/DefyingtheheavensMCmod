package com.example.defyingtheheavens;

import net.minecraft.util.RandomSource;

/** Names for newly founded sects, in the manner of their path: "Azure Cloud Sect", "Blood Moon Palace", "Misty Jade Valley". */
public final class SectNames {
	private static final String[] RIGHTEOUS_FIRST = {"Azure", "Heavenly", "Pure", "Radiant", "Celestial", "Jade", "Golden", "Crane", "Morning",
			"Clear", "Lotus", "Thousand", "Sword", "Profound", "Serene", "Dragon"};
	private static final String[] RIGHTEOUS_SECOND = {"Cloud", "Sword", "Lotus", "Peak", "Sun", "Sky", "Spring", "Light", "Crane", "Pine",
			"Heart", "Mountain", "Phoenix", "Sea"};
	private static final String[] DEMONIC_FIRST = {"Blood", "Crimson", "Nine", "Black", "Shadow", "Bone", "Ghost", "Scarlet", "Withered",
			"Abyssal", "Hungry", "Iron", "Ashen", "Venom"};
	private static final String[] DEMONIC_SECOND = {"Moon", "Netherworld", "Lotus", "Serpent", "Soul", "Flame", "Fiend", "Shroud", "Skull",
			"Abyss", "Corpse", "Thorn", "Mist", "Gate"};
	private static final String[] NEUTRAL_FIRST = {"Misty", "Hidden", "Wandering", "Quiet", "Silver", "Bamboo", "Ancient", "Floating",
			"Distant", "Moonlit", "Willow", "Stone", "Autumn", "River"};
	private static final String[] NEUTRAL_SECOND = {"Valley", "Pavilion", "Mist", "Stream", "Bamboo", "Lake", "Cliff", "Wind", "Gate",
			"Bell", "Leaf", "Garden", "Tower", "Brook"};
	private static final String[] RIGHTEOUS_KIND = {"Sect", "Sect", "Sect", "Palace", "School", "Pavilion"};
	private static final String[] DEMONIC_KIND = {"Sect", "Sect", "Palace", "Hall", "Cult", "Temple"};
	private static final String[] NEUTRAL_KIND = {"Sect", "Sect", "Pavilion", "Valley", "Hall", "Manor"};

	public static String generate(RandomSource random, Alignment.Faction faction) {
		String[] first, second, kind;
		switch (faction) {
			case RIGHTEOUS -> {
				first = RIGHTEOUS_FIRST;
				second = RIGHTEOUS_SECOND;
				kind = RIGHTEOUS_KIND;
			}
			case DEMONIC -> {
				first = DEMONIC_FIRST;
				second = DEMONIC_SECOND;
				kind = DEMONIC_KIND;
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

	private SectNames() {}
}
