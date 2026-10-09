package com.example.defyingtheheavens;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * The five elements qi takes in the world, each with the colours it shows in under Qi Sense: a core and the glow around it.
 * All five drift everywhere, but the land decides which is thickest ({@link #pick}): forests are thick with wood qi, seas
 * with water, the Nether and deserts with fire, mountains and caves with earth and metal.
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
	METAL(0xFFFFFF, 0xFFC83C);

	/** This share of every place's qi is of any element at random, so all five always show; the land decides the rest. */
	private static final float MIXED_SHARE = 0.35f;

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

	// Weights, in enum order: wood, water, fire, earth, metal.
	private static final float[] NETHER = {0, 0, 8, 2, 1};
	private static final float[] END = {0, 0, 0, 2, 6};
	private static final float[] WATERS = {1, 8, 0, 1, 0};
	private static final float[] SWAMP = {4, 5, 0, 1, 0};
	private static final float[] FOREST = {8, 1, 0.3f, 1, 0.3f};
	private static final float[] ARID = {0.3f, 0, 5, 4, 0.5f};
	private static final float[] SAVANNA = {2, 0, 2, 2, 0.3f};
	private static final float[] PEAKS = {0.3f, 0.5f, 0, 4, 5};
	private static final float[] SNOW = {0.5f, 4, 0, 1, 3};
	private static final float[] UNDERGROUND = {0.2f, 0.5f, 0.5f, 5, 4};
	private static final float[] LUSH_CAVES = {5, 3, 0, 2, 0.5f};
	private static final float[] THUNDER = {0, 0, 4, 1, 4};
	private static final float[] SWORDS = {0.3f, 0, 0, 2, 8};
	private static final float[] CHAOS = {1, 1, 1, 1, 1};
	private static final float[] PLAINS = {3, 1, 1, 3, 1};

	/**
	 * A random element for qi gathering in {@code biome}; {@code underground} (no sky above, in a realm that has one) leans to
	 * earth and metal whatever the biome. Every element is at least {@link #MIXED_SHARE} / 5 (7%) of the qi anywhere.
	 */
	public static QiElement pick(Holder<Biome> biome, boolean underground, RandomSource random) {
		QiElement[] all = values();
		if (random.nextFloat() < MIXED_SHARE) return all[random.nextInt(all.length)];
		float[] weights = weights(biome, underground);
		float total = 0;
		for (float w : weights) total += w;
		float roll = random.nextFloat() * total;
		for (QiElement element : all) {
			roll -= weights[element.ordinal()];
			if (roll < 0) return element;
		}
		return EARTH;
	}

	private static float[] weights(Holder<Biome> biome, boolean underground) {
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
