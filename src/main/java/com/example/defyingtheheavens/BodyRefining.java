package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;

/**
 * Body refining: the path of one born with Shattered Meridians (Physique), whose meridians can never hold qi. Past a mortal's Peak
 * their tempering goes on, through eight stages, each a body as strong as a cultivator's of one stage higher, from Qi Refining Mid
 * (Copper Skin) to Core Formation Early (Indestructible Body): the strength, never the qi, abilities or spells. Tempering comes as a
 * mortal's does (fighting, mining, sprinting, eating, herbs: Tempering), and from meditation, circulating blood where others
 * circulate qi ({@link #meditationPerSecond}).
 */
public final class BodyRefining {
	/** The last stage. */
	public static final int LAST = 8;
	/** Tempering to reach each next stage, from Mortal Peak (stage 0) on. */
	private static final double[] TO_NEXT = {600, 1000, 1600, 2600, 4000, 6000, 9000, 13000};
	/** Tempering a second of meditation gives at each stage: about ten to twenty minutes of it a stage. */
	private static final double[] MEDITATION = {1.0, 1.4, 1.8, 2.6, 3.6, 5.0, 7.0, 10.0, 0};
	private static final String[] IDS = {"peak", "copper_skin", "iron_flesh", "steel_sinew", "jade_bone", "silver_marrow", "golden_blood", "diamond_heart",
			"indestructible_body"};

	/** Tempering stage {@code stage} needs to reach the next (0 at the last). */
	public static double toNext(int stage) {
		return stage >= LAST ? 0 : TO_NEXT[Math.max(0, stage)];
	}

	/** Tempering a second of meditation gives {@code c} (nothing for anyone but one with shattered meridians, past nothing at the last). */
	public static double meditationPerSecond(PlayerCultivation c) {
		if (!c.hasShatteredMeridians() || !c.isMortal()) return 0;
		if (!c.getMortalStage().isLast()) return MEDITATION[0] * 0.6;
		return MEDITATION[Math.min(LAST, c.getBodyRefining())];
	}

	/**
	 * The cultivator's rank (PlayerCultivation#rank) whose body stage {@code stage} is as strong as: Qi Refining Early's at Peak, up to
	 * Core Formation Early's at the last.
	 */
	public static int rank(int stage) {
		return Math.max(0, Math.min(LAST, stage));
	}

	public static Component name(int stage) {
		return Component.translatable(ModLang.bodyRefiningKey(IDS[Math.max(0, Math.min(LAST, stage))]));
	}

	private BodyRefining() {}
}
