package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;

/**
 * Whose blood a player is of (the user's ask, 2026-10-23: the demon races): a human, or one of the demon beasts, chosen the first
 * time they join (Bloodlines); a bloodline awakened later may change it (to come). Tiger, Lizard, Fox, Dragon. A demon wears its race's whole look, skin, head
 * and tail ({@link #head}); a human keeps their own skin. Each race comes in a few colourings, {@link #variants}. Saved by id;
 * add new ones at the end.
 * <p>
 * Not the devilish path of the alignments (Alignment), which any of them may walk.
 */
public enum Bloodline {
	HUMAN("human", null, 0xE8D8B0),
	/** The White Tiger's kin: striped, round-eared, a long striped tail. */
	TIGER("tiger", BeastHead.TIGER, 0xF0A030, "golden", "white", "black"),
	/** The great lizards': scaled, slit-eyed, a forked tongue, a long scaled tail (first named a snake; the user's word, 2026-10-23). */
	LIZARD("lizard", BeastHead.LIZARD, 0x5FB06A, "jade", "black", "crimson"),
	/** The fox spirits': tall ears, a white muzzle, a tail for each realm, nine at Sacred Vessel ({@link #tails}). */
	FOX("fox", BeastHead.FOX, 0xE0703A, "red", "snow", "silver"),
	/** The true dragons': antlers, whiskers, a flowing mane and a long scaled tail tufted at its end. */
	DRAGON("dragon", BeastHead.DRAGON, 0x4FA8D8, "azure", "gold", "crimson");

	/** A fox spirit's most tails, and the realm it has them all by. */
	public static final int MOST_TAILS = 9;

	private final String id;
	private final BeastHead head;
	private final int colour;
	private final String[] variants;

	Bloodline(String id, BeastHead head, int colour, String... variants) {
		this.id = id;
		this.head = head;
		this.colour = colour;
		this.variants = variants;
	}

	public String getId() { return id; }

	/** A demon beast's (not a human's). */
	public boolean isBeast() { return head != null; }

	/** The beast's head (and tail) it wears, or null for a human. */
	public BeastHead head() { return head; }

	/** Its colour in the menus. */
	public int getColour() { return colour; }

	/** How many colourings it comes in (none for a human: their own skin). */
	public int variants() { return variants.length; }

	/** The id of colouring {@code variant} ("golden"), clamped into range; empty for a human. */
	public String variantId(int variant) {
		return variants.length == 0 ? "" : variants[Math.floorMod(variant, variants.length)];
	}

	public Component getDisplayName() { return Component.translatable(ModLang.bloodlineKey(id)); }

	public Component variantName(int variant) { return Component.translatable(ModLang.bloodlineVariantKey(id, variantId(variant))); }

	/** How many tails a fox spirit of {@code realm} has: one at Qi Refining (and as a mortal), one more each realm after. */
	public static int tails(Realm realm) {
		return Math.min(MOST_TAILS, 1 + realm.ordinal());
	}

	public static Bloodline byId(String id) {
		if ("snake".equals(id)) return LIZARD; // its first name
		for (Bloodline b : values()) {
			if (b.id.equals(id)) return b;
		}
		return null;
	}

	public static Bloodline byIndex(int index) {
		return index >= 0 && index < values().length ? values()[index] : null;
	}
}
