package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.EnumSet;
import java.util.List;

/**
 * The body a player is born with (PlayerCultivation#getPhysique): rolled once, the first time they join, by {@link #weight}
 * (the rarer the grade, the rarer the roll), and never changed except by an op's command. What each does is where it does it:
 * cultivation speed ({@link #cultivationFactor}, {@link #ghostFactor}), the qi pool ({@link #qiFactor}), which elements' heaven
 * methods may be learned ({@link #allows}, the roots' own elements: PlayerCultivation#getRootElements), and the rest in the systems
 * they touch (Tribulations, PillItem and the cauldron, YinCorruption, the Ritual to the Heavens, the Consciousness Domain, Qi Sense,
 * the ghost form, body tempering). An element limit never keeps anyone off the ghost path. Synced and saved by id: add new ones at
 * the end.
 */
public enum Physique {
	MORTAL_BODY("mortal_body", Grade.LOW, 20),
	FIVE_ELEMENT_ROOTS("five_element_spiritual_roots", Grade.LOW, 24),
	CURSED_MERIDIANS("cursed_meridian_body", Grade.LOW, 10),
	/** Cannot cultivate: the meridians won't hold qi. Body tempering instead, past a mortal's Peak (BodyRefining). */
	SHATTERED_MERIDIANS("shattered_meridians", Grade.LOW, 6),
	/** Three elements' methods only, at 1.5 times the pace. */
	THREE_ELEMENT_ROOTS("three_element_spiritual_roots", Grade.MID, 9),
	/** Yin corruption never takes hold. */
	YIN_SOUL_BODY("yin_soul_body", Grade.MID, 5),
	/** No Ritual to the Heavens wanted; the heaven path at 1.2 times the pace. */
	HEAVEN_SACRED_BODY("heaven_sacred_body", Grade.MID, 6),
	/** Pills and elixirs brewed by them work twice as strongly. */
	INNATE_ALCHEMY_BODY("innate_alchemy_body", Grade.MID, 5),
	/** One element's methods only, at twice the pace. */
	TRUE_ELEMENT_ROOT("true_element_spiritual_root", Grade.HIGH, 4),
	/** A Consciousness Domain as wide as a Four Axis cultivator's from its first day; it never grows. */
	INNATE_CONSCIOUSNESS_BODY("innate_consciousness_body", Grade.HIGH, 3),
	/** Qi gathered 1.5 times as fast into a pool 1.5 times as deep; Qi Sense open from a mortal's first day. */
	BLESSED_MERIDIANS("blessed_meridian_body", Grade.HIGH, 3),
	/** No medicinal toxicity; a pill's or fruit's qi refined the moment it is taken. */
	JADE_POISON_BODY("jade_poison_body", Grade.SUPREME, 3),
	/** Five times the pace; any element; a method's new layers need no settling. */
	FALLEN_TRUE_IMMORTAL("fallen_true_immortal", Grade.IMMORTAL, 0.8),
	/** The ghost path at twice the pace; a ghost form of its own anywhere, a Ghost King's, as strong as its ghost methods. */
	GHOST_DEVIL_BODY("ghost_devil_body", Grade.IMMORTAL, 0.8),
	/** A heavenly tribulation's lightning feeds it: no harm, cultivation and qi instead. The rarest of all. */
	HEAVENLY_THUNDER_BODY("heavenly_thunder_body", Grade.IMMORTAL, 0.4);

	/** How rare a physique is, and its colour. */
	public enum Grade {
		LOW("low", ChatFormatting.GRAY), MID("mid", ChatFormatting.GREEN), HIGH("high", ChatFormatting.AQUA), SUPREME("supreme", ChatFormatting.LIGHT_PURPLE),
		IMMORTAL("immortal", ChatFormatting.GOLD);

		private final String id;
		private final ChatFormatting colour;

		Grade(String id, ChatFormatting colour) {
			this.id = id;
			this.colour = colour;
		}

		public ChatFormatting colour() { return colour; }

		public Component getDisplayName() { return Component.translatable(ModLang.physiqueGradeKey(id)).withStyle(colour); }
	}

	private final String id;
	private final Grade grade;
	/** Its share of every hundred births. */
	private final double weight;

	Physique(String id, Grade grade, double weight) {
		this.id = id;
		this.grade = grade;
		this.weight = weight;
	}

	public String getId() { return id; }
	public Grade grade() { return grade; }
	public double weight() { return weight; }

	public Component getDisplayName() { return Component.translatable(ModLang.physiqueKey(id)).withStyle(grade.colour()); }
	public Component getDescription() { return Component.translatable(ModLang.physiqueKey(id) + ".description"); }

	/** How many elements its roots are of (their methods only), or 0 for no limit. */
	public int rootCount() {
		return this == THREE_ELEMENT_ROOTS ? 3 : this == TRUE_ELEMENT_ROOT ? 1 : 0;
	}

	/** Whether one born with it may take up a heaven method of {@code element} ({@code roots}: the roots' own elements). */
	public boolean allows(QiElement element, java.util.Set<QiElement> roots) {
		return rootCount() == 0 || roots.contains(element);
	}

	/** What the heaven path's cultivation (and the methods' settling with it) is multiplied by. */
	public double cultivationFactor() {
		return switch (this) {
			case THREE_ELEMENT_ROOTS -> 1.5;
			case TRUE_ELEMENT_ROOT -> 2.0;
			case FALLEN_TRUE_IMMORTAL -> 5.0;
			case HEAVEN_SACRED_BODY -> 1.2;
			default -> 1.0;
		};
	}

	/** What the ghost path's cultivation is multiplied by. */
	public double ghostFactor() {
		return switch (this) {
			case GHOST_DEVIL_BODY -> 2.0;
			case FALLEN_TRUE_IMMORTAL -> 5.0;
			default -> 1.0;
		};
	}

	/** What the qi pool's depth and its gathering are multiplied by. */
	public double qiFactor() {
		return switch (this) {
			case BLESSED_MERIDIANS -> 1.5;
			case CURSED_MERIDIANS -> 0.8;
			default -> 1.0;
		};
	}

	/**
	 * A physique by its weights ({@link #weight}). Elements for its roots are drawn too, by the caller ({@link #rollRoots}), the
	 * elements of methods already known first.
	 */
	public static Physique roll(RandomSource random) {
		double total = 0;
		for (Physique p : values()) total += p.weight;
		double pick = random.nextDouble() * total;
		for (Physique p : values()) {
			pick -= p.weight;
			if (pick < 0) return p;
		}
		return MORTAL_BODY;
	}

	/** The elements of {@code physique}'s roots ({@link #rootCount} of them): {@code known} first (methods already learned), then at random. */
	public static EnumSet<QiElement> rollRoots(Physique physique, java.util.Collection<QiElement> known, RandomSource random) {
		EnumSet<QiElement> roots = EnumSet.noneOf(QiElement.class);
		int count = physique.rootCount();
		for (QiElement element : known) if (roots.size() < count) roots.add(element);
		List<QiElement> rest = new java.util.ArrayList<>(List.of(QiElement.five()));
		rest.removeAll(roots);
		while (roots.size() < count && !rest.isEmpty()) roots.add(rest.remove(random.nextInt(rest.size())));
		return roots;
	}

	public static Physique byId(String id) {
		for (Physique p : values()) if (p.id.equals(id)) return p;
		return null;
	}

	public static Physique byIndex(int index) {
		return index >= 0 && index < values().length ? values()[index] : null;
	}
}
