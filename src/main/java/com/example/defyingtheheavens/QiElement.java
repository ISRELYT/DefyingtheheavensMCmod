package com.example.defyingtheheavens;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * The elements qi takes in the world, each with the colours it shows in under Qi Sense: a core and the glow around it.
 * The five drift everywhere, but the land decides which is thickest ({@link #pick}): forests are thick with wood qi, seas
 * with water, the Nether and deserts with fire, mountains and caves with earth and metal. The sixth, yin, is ghostly qi
 * ({@link GhostQi}): it shows only where ghostly qi flows, and the Netherworld is almost nothing else.
 */
public enum QiElement {
	/** Green. */
	WOOD(0x3CE65A, 0x14B43C),
	/** Blue. */
	WATER(0x3C8CFF, 0x1E50FF),
	/** Red. */
	FIRE(0xFF4020, 0xFF1A00),
	/** Yellow core in a brown glow. */
	EARTH(0xD99A2B, 0x8C5A1E),
	/** White core in a gold glow. */
	METAL(0xFFFFFF, 0xFFC83C),
	/** Pale cyan-white core in a cyan glow: ghostly qi. Appended, so the five keep their ordinals. */
	YIN(0xE6FFFF, 0x4FE3E8);

	/**
	 * This share of every place's qi is of an element at random, so all the land's elements always show; the land decides the
	 * rest. Only where ghostly qi flows can the random one be yin, so the living world never shows a mote of it.
	 */
	private static final float MIXED_SHARE = 0.35f;
	/**
	 * In the Netherworld, this share of the random part is yin outright (the rest drawn from all six): with its weights below,
	 * 0.65 x 12/12.7 + 0.35 x (0.9 + 0.1/6) = about 93% of its qi is yin, "almost pure".
	 */
	private static final float NETHERWORLD_MIXED_YIN = 0.9f;
	private static final QiElement[] FIVE = {WOOD, WATER, FIRE, EARTH, METAL};

	private final int coreColor;
	private final int glowColor;

	QiElement(int coreColor, int glowColor) {
		this.coreColor = coreColor;
		this.glowColor = glowColor;
	}

	/** The mote itself, 0xRRGGBB. */
	public int getCoreColor() { return coreColor; }

	/** The glow around it, 0xRRGGBB. */
	public int getGlowColor() { return glowColor; }

	// Weights, in enum order: wood, water, fire, earth, metal, yin. Every array has all six.
	/** Almost pure yin (see {@link #NETHERWORLD_MIXED_YIN}), whatever lies under it: deep underground too. */
	private static final float[] NETHERWORLD = {0, 0.2f, 0, 0.3f, 0.2f, 12};
	/** Mostly yin, the Nether's fire beneath: 0.65 x 10/13.5 + 0.35 / 6 = about 54% yin. */
	private static final float[] SOUL_SAND_VALLEY = {0, 0, 2, 1, 0.5f, 10};
	private static final float[] NETHER = {0, 0, 8, 2, 1, 0};
	private static final float[] END = {0, 0, 0, 2, 6, 0};
	private static final float[] WATERS = {1, 8, 0, 1, 0, 0};
	private static final float[] SWAMP = {4, 5, 0, 1, 0, 0};
	private static final float[] FOREST = {8, 1, 0.3f, 1, 0.3f, 0};
	private static final float[] ARID = {0.3f, 0, 5, 4, 0.5f, 0};
	private static final float[] SAVANNA = {2, 0, 2, 2, 0.3f, 0};
	private static final float[] PEAKS = {0.3f, 0.5f, 0, 4, 5, 0};
	private static final float[] SNOW = {0.5f, 4, 0, 1, 3, 0};
	private static final float[] UNDERGROUND = {0.2f, 0.5f, 0.5f, 5, 4, 0};
	private static final float[] LUSH_CAVES = {5, 3, 0, 2, 0.5f, 0};
	private static final float[] THUNDER = {0, 0, 4, 1, 4, 0};
	private static final float[] SWORDS = {0.3f, 0, 0, 2, 8, 0};
	private static final float[] CHAOS = {1, 1, 1, 1, 1, 0};
	private static final float[] PLAINS = {3, 1, 1, 3, 1, 0};

	/**
	 * A random element for qi gathering in {@code biome}; {@code underground} (no sky above, in a realm that has one) leans to
	 * earth and metal whatever the biome. Every one of the five is at least {@link #MIXED_SHARE} / 5 (7%) of the qi anywhere
	 * outside the realm of the dead; yin shows only where ghostly qi flows.
	 */
	public static QiElement pick(Holder<Biome> biome, boolean underground, RandomSource random) {
		boolean netherworld = biome.is(ModTags.IS_NETHERWORLD);
		if (random.nextFloat() < MIXED_SHARE) {
			if (netherworld && random.nextFloat() < NETHERWORLD_MIXED_YIN) return YIN;
			QiElement[] pool = netherworld || biome.is(Biomes.SOUL_SAND_VALLEY) ? values() : FIVE;
			return pool[random.nextInt(pool.length)];
		}
		float[] weights = weights(biome, underground);
		float total = 0;
		for (float w : weights) total += w;
		float roll = random.nextFloat() * total;
		for (QiElement element : values()) {
			roll -= weights[element.ordinal()];
			if (roll < 0) return element;
		}
		return EARTH;
	}

	/** The five (not yin), in their order: the heaven methods' elements (HeavenMethod). */
	public static QiElement[] five() {
		return FIVE.clone();
	}

	/** The element's name ("Wood", ...), for the heaven methods' screens and messages. */
	public net.minecraft.network.chat.Component getDisplayName() {
		return net.minecraft.network.chat.Component.translatable(ModLang.ELEMENT_PREFIX + name().toLowerCase(java.util.Locale.ROOT));
	}

	/**
	 * What share of the qi in {@code biome} is {@code element} (0 to 1), as {@link #pick} draws it: the random part shared out
	 * evenly, the rest by the land's weights. A heaven method of an element cultivates the faster the greater its share where one
	 * meditates (HeavenMethods#methodFactor).
	 */
	public static double share(Holder<Biome> biome, boolean underground, QiElement element) {
		boolean netherworld = biome.is(ModTags.IS_NETHERWORLD);
		boolean allSix = netherworld || biome.is(Biomes.SOUL_SAND_VALLEY);
		double random;
		if (element == YIN && !allSix) random = 0;
		else if (netherworld) random = element == YIN ? NETHERWORLD_MIXED_YIN + (1 - NETHERWORLD_MIXED_YIN) / 6.0 : (1 - NETHERWORLD_MIXED_YIN) / 6.0;
		else random = 1.0 / (allSix ? 6 : 5);
		float[] weights = weights(biome, underground);
		float total = 0;
		for (float w : weights) total += w;
		return MIXED_SHARE * random + (1 - MIXED_SHARE) * (total <= 0 ? 0 : weights[element.ordinal()] / total);
	}

	/** One of the five (never yin) drawn by the land's own weights in {@code biome}: the element a stele there is carved for. */
	public static QiElement pickOfFive(Holder<Biome> biome, boolean underground, RandomSource random) {
		float[] weights = weights(biome, underground);
		float total = 0;
		for (QiElement element : FIVE) total += weights[element.ordinal()];
		if (total <= 0) return FIVE[random.nextInt(FIVE.length)];
		float roll = random.nextFloat() * total;
		for (QiElement element : FIVE) {
			roll -= weights[element.ordinal()];
			if (roll < 0) return element;
		}
		return EARTH;
	}

	private static float[] weights(Holder<Biome> biome, boolean underground) {
		if (biome.is(ModTags.IS_NETHERWORLD) || biome.is(ModBiomes.SOUL_RIVER)) return NETHERWORLD;
		if (biome.is(Biomes.SOUL_SAND_VALLEY)) return SOUL_SAND_VALLEY;
		if (biome.is(BiomeTags.IS_NETHER)) return NETHER;
		if (biome.is(BiomeTags.IS_END)) return END;
		if (biome.is(ModBiomes.SPATIAL_GAP)) return CHAOS;
		if (biome.is(Biomes.LUSH_CAVES)) return LUSH_CAVES;
		if (underground || biome.is(Biomes.DRIPSTONE_CAVES) || biome.is(Biomes.DEEP_DARK)) return UNDERGROUND;
		if (biome.is(ModBiomes.THUNDER_PEAKS)) return THUNDER;
		if (biome.is(ModBiomes.ANCIENT_SWORD_GRAVEYARD)) return SWORDS;
		if (biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP)) return SWAMP;
		if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN) || biome.is(BiomeTags.IS_RIVER)
				|| biome.is(BiomeTags.IS_BEACH)) return WATERS;
		if (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA) || biome.is(BiomeTags.IS_JUNGLE)
				|| biome.is(Biomes.CHERRY_GROVE) || biome.is(ModBiomes.SPIRIT_FOREST) || biome.is(ModBiomes.PEACH_BLOSSOM_SANCTUARY)) return FOREST;
		if (biome.is(BiomeTags.IS_BADLANDS) || biome.is(Biomes.DESERT)) return ARID;
		if (biome.is(BiomeTags.IS_SAVANNA)) return SAVANNA;
		if (biome.is(Biomes.SNOWY_PLAINS) || biome.is(Biomes.ICE_SPIKES) || biome.is(Biomes.SNOWY_SLOPES)
				|| biome.is(Biomes.GROVE)) return SNOW;
		if (biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(BiomeTags.IS_HILL) || biome.is(ModBiomes.DENSE_QI_PEAKS)) return PEAKS;
		return PLAINS;
	}
}
