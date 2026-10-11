package com.example.defyingtheheavens;

import java.util.UUID;

/**
 * Personal names for cultivators: a surname and a given name of one or two syllables ("Mo Lian", "Gu Hanyu"), worked out
 * from the cultivator's UUID, so a name never needs saving and is the same whether the cultivator is loaded or only a line
 * in its sect's member list.
 */
public final class NpcNames {
	private static final String[] SURNAMES = {"Mo", "Gu", "Lin", "Shen", "Xu", "Bai", "Han", "Su", "Ye", "Lu", "Qin", "Wei", "Jiang", "Yun",
			"Feng", "Chu", "Lan", "Zhao", "Long", "Hua", "Liu", "Tang", "Song", "Fang", "Xiao", "Murong", "Shangguan", "Ouyang"};
	private static final String[] SYLLABLES = {"lian", "han", "yu", "chen", "xuan", "ming", "rou", "feng", "yi", "zhen", "qing", "lan",
			"tian", "hao", "jing", "wen", "ying", "shuang", "lei", "ning", "an", "ruo", "xue", "kai", "yao", "mei", "jun", "heng", "su", "bai"};

	/** The name of the cultivator with this UUID. */
	public static String of(UUID id) {
		long h = id.getMostSignificantBits() * 0x9E3779B97F4A7C15L ^ id.getLeastSignificantBits();
		h ^= h >>> 29;
		String surname = SURNAMES[Math.floorMod((int) h, SURNAMES.length)];
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 31;
		int g = Math.floorMod((int) h, SYLLABLES.length);
		if (SYLLABLES[g].equalsIgnoreCase(surname)) g = (g + 1) % SYLLABLES.length; // no "Feng Feng"
		String given = SYLLABLES[g];
		h *= 0x94D049BB133111EBL;
		h ^= h >>> 29;
		if (Math.floorMod((int) h, 3) == 0) {
			String second = SYLLABLES[Math.floorMod((int) (h >>> 32), SYLLABLES.length)];
			// Two syllables run together, the second never a vowel-led one ("Ruoan" reads badly).
			if (!second.equals(given) && "aeiou".indexOf(second.charAt(0)) < 0) given = given + second;
		}
		return surname + " " + Character.toUpperCase(given.charAt(0)) + given.substring(1);
	}

	private NpcNames() {}
}
