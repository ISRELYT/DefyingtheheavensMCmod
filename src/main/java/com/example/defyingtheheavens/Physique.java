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
 * the ghost form, body tempering, and Physiques' own hooks for the rest). An element limit never keeps anyone off the ghost path.
 * Ask a cultivator whether it has a body with PlayerCultivation#hasPhysique, not by comparing its physique: a Myriad Form Body has
 * two more ({@link #rollForms}). Synced and saved by id: add new ones at the end.
 * <p>
 * Of every hundred births (the user's ask, 2026-10-20, to even out the grades): Low 56, Mid 24, High 12, Supreme 5, Immortal 3.
 */
public enum Physique {
	/**
	 * The common body: no limit, no gift. It took the old Mortal Body's share too (removed 2026-10-19, the user's call: they were the
	 * same body), and a save that still names that body is read as this one ({@link #byId}).
	 */
	FIVE_ELEMENT_ROOTS("five_element_spiritual_roots", Grade.LOW, 40),
	CURSED_MERIDIANS("cursed_meridian_body", Grade.LOW, 10),
	/** Cannot cultivate: the meridians won't hold qi. Body tempering instead, past a mortal's Peak (BodyRefining). */
	SHATTERED_MERIDIANS("shattered_meridians", Grade.LOW, 6),
	/** Three elements' methods only, at 1.5 times the pace. */
	THREE_ELEMENT_ROOTS("three_element_spiritual_roots", Grade.MID, 7),
	/** Yin corruption never takes hold; the ghost path at 1.2 times the pace. */
	YIN_SOUL_BODY("yin_soul_body", Grade.MID, 4),
	/** No Ritual to the Heavens wanted; the heaven path at 1.2 times the pace. */
	HEAVEN_SACRED_BODY("heaven_sacred_body", Grade.MID, 5),
	/** Pills and elixirs brewed by them work twice as strongly. */
	INNATE_ALCHEMY_BODY("innate_alchemy_body", Grade.MID, 4),
	/** One element's methods only, at twice the pace. */
	TRUE_ELEMENT_ROOT("true_element_spiritual_root", Grade.HIGH, 3),
	/** A Consciousness Domain as wide as a Four Axis cultivator's from its first day; it never grows. */
	INNATE_CONSCIOUSNESS_BODY("innate_consciousness_body", Grade.HIGH, 2),
	/** Qi gathered 1.5 times as fast into a pool 1.5 times as deep; Qi Sense open from a mortal's first day. */
	BLESSED_MERIDIANS("blessed_meridian_body", Grade.HIGH, 3),
	/**
	 * No medicinal toxicity; a pill's or fruit's qi refined the moment it is taken; no poison nor wither takes hold of it, and its
	 * blows poison (Physiques#onMeleeHit).
	 */
	JADE_POISON_BODY("jade_poison_body", Grade.SUPREME, 1.25),
	/** Five times the pace; any element; a method's new layers need no settling. */
	FALLEN_TRUE_IMMORTAL("fallen_true_immortal", Grade.IMMORTAL, 0.6),
	/** The ghost path at twice the pace; a ghost form of its own anywhere, a Ghost King's, as strong as its ghost methods. */
	GHOST_DEVIL_BODY("ghost_devil_body", Grade.IMMORTAL, 0.6),
	/** A heavenly tribulation's lightning feeds it: no harm, cultivation and qi instead. */
	HEAVENLY_THUNDER_BODY("heavenly_thunder_body", Grade.IMMORTAL, 0.3),
	// The user's eleven of 2026-10-20, as first proposed (Heaven-Defying as they changed it):
	/** Sees souls and the dead from its first day, as the Ghost Eye does, and clearly, for nothing (SoulProjection#perceives). */
	SPIRIT_EYE_BODY("spirit_eye_body", Grade.MID, 4),
	/** Fire and lava never burn it (Physiques#allowDamage); the heaven path at 1.3 times the pace, the ghost path at half. */
	NINE_YANG_BODY("nine_yang_body", Grade.HIGH, 2),
	/**
	 * The void bears it from Core Formation (AstralRealm#canBearTheVoid); a star's heat never burns it, space's qi is whole to it, and
	 * it flies through space 1.5 times as fast, paying the years of its flight there by as much less (AstralRealm#yearsPerBlock).
	 */
	STARBORN_BODY("starborn_body", Grade.HIGH, 2),
	/** Health and strength 1.3 times (CultivationStats); a heavenly tribulation's strikes a quarter weaker; tempering twice as fast. */
	ANCIENT_HOLY_BODY("ancient_holy_body", Grade.SUPREME, 1.25),
	/** Heals by a share of the harm it deals (Physiques#onHurt). */
	BLOOD_DEMON_BODY("blood_demon_body", Grade.SUPREME, 1.25),
	/** A stat point more at every breakthrough (StatPoints#earned). */
	HUNDRED_REFINEMENTS_BODY("hundred_refinements_body", Grade.SUPREME, 1.25),
	/**
	 * Meditation feeds both paths at once, each at its full pace (MeditationManager); no element limit; its spells strike a quarter
	 * harder. The heavens hate it: every tribulation strikes half as many times again (TribulationManager).
	 */
	PRIMORDIAL_CHAOS_BODY("primordial_chaos_body", Grade.IMMORTAL, 0.4),
	/**
	 * The heavens can't strike it down: a tribulation's killing strike leaves it one heart and the tribulation fails (the user's
	 * change, 2026-10-20: it isn't spared them altogether), and no failed tribulation costs it a stage (the user's, the same day:
	 * TribulationManager#holds); no Ritual to the Heavens wanted. The heaven path at 0.75 times the pace, and the righteous think the
	 * less of it (CultivatorNpc#regard).
	 */
	HEAVEN_DEFYING_BODY("heaven_defying_body", Grade.IMMORTAL, 0.5),
	/**
	 * Every cultivator it slays, players too, gives it a tenth of what the victim's stage takes (Physiques#onKill); each righteous or
	 * neutral one drags its path toward the demonic, and the righteous think the less of it.
	 */
	SOUL_DEVOURING_BODY("soul_devouring_body", Grade.IMMORTAL, 0.4),
	/**
	 * Reborn (a life spent, a soul scattered, a Rebirth Pill), it keeps its methods, half its realm on each path and this body, the
	 * only one a new life doesn't roll anew (Lifespan#wipe). The rarest of all, with the Myriad Form Body.
	 */
	ETERNAL_REINCARNATION_BODY("eternal_reincarnation_body", Grade.IMMORTAL, 0.1),
	/** Born with two more physiques of Mid to Supreme grade, never a pair that clashes, and all three's gifts ({@link #rollForms}). */
	MYRIAD_FORM_BODY("myriad_form_body", Grade.IMMORTAL, 0.1);

	/**
	 * How rare a physique is, its colour, its talent and its speed. Its talent is what the years a meditation costs are divided by
	 * (Lifespan#seclusionYears; the user's vision, 2026-10-21: talent decides how far one gets), Low x1 to Immortal x16 (rebalanced
	 * 2026-10-22 as dense qi, methods and spirit stones came to save years too); its speed, a small edge to the pace of either path's
	 * cultivation (the user's pick, 2026-10-22: the gifted cultivate a little faster too), Low x1 to Immortal x1.5.
	 */
	public enum Grade {
		LOW("low", ChatFormatting.GRAY, 1.0, 1.0), MID("mid", ChatFormatting.GREEN, 1.5, 1.1), HIGH("high", ChatFormatting.AQUA, 2.1, 1.2),
		SUPREME("supreme", ChatFormatting.LIGHT_PURPLE, 4.5, 1.35), IMMORTAL("immortal", ChatFormatting.GOLD, 16.0, 1.5);

		private final String id;
		private final ChatFormatting colour;
		private final double talent;
		private final double speed;

		Grade(String id, ChatFormatting colour, double talent, double speed) {
			this.id = id;
			this.colour = colour;
			this.talent = talent;
			this.speed = speed;
		}

		public ChatFormatting colour() { return colour; }
		public double talent() { return talent; }
		public double speed() { return speed; }

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
			case NINE_YANG_BODY -> 1.3;
			case HEAVEN_DEFYING_BODY -> 0.75;
			default -> 1.0;
		};
	}

	/** What the ghost path's cultivation is multiplied by. */
	public double ghostFactor() {
		return switch (this) {
			case GHOST_DEVIL_BODY -> 2.0;
			case FALLEN_TRUE_IMMORTAL -> 5.0;
			case YIN_SOUL_BODY -> 1.2;
			case NINE_YANG_BODY -> 0.5;
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
		return FIVE_ELEMENT_ROOTS;
	}

	/** A Myriad Form Body's forms number this many. */
	public static final int FORMS = 2;

	/** Whether a Myriad Form Body may be born with it besides: Mid to Supreme grade. */
	public boolean isForm() {
		return grade.ordinal() >= Grade.MID.ordinal() && grade.ordinal() <= Grade.SUPREME.ordinal();
	}

	/** Two bodies that can't be one: roots of three elements and of one, the Nine Yang's yang and the Yin Soul's yin. */
	public static boolean clash(Physique a, Physique b) {
		if (a == b) return true;
		if (a.rootCount() > 0 && b.rootCount() > 0) return true;
		return a == NINE_YANG_BODY && b == YIN_SOUL_BODY || a == YIN_SOUL_BODY && b == NINE_YANG_BODY;
	}

	/**
	 * A Myriad Form Body's forms: {@link #FORMS} physiques of Mid to Supreme grade ({@link #isForm}) by their weights, no two that
	 * {@link #clash}.
	 */
	public static EnumSet<Physique> rollForms(RandomSource random) {
		EnumSet<Physique> forms = EnumSet.noneOf(Physique.class);
		while (forms.size() < FORMS) {
			double total = 0;
			List<Physique> open = new java.util.ArrayList<>();
			for (Physique p : values()) {
				if (!p.isForm() || forms.stream().anyMatch(f -> clash(f, p))) continue;
				open.add(p);
				total += p.weight;
			}
			if (open.isEmpty()) break;
			double pick = random.nextDouble() * total;
			Physique chosen = open.get(open.size() - 1);
			for (Physique p : open) {
				pick -= p.weight;
				if (pick < 0) {
					chosen = p;
					break;
				}
			}
			forms.add(chosen);
		}
		return forms;
	}

	/** The id the removed Mortal Body was saved by: read as Five Element Spiritual Roots, the same body. */
	private static final String MORTAL_BODY_ID = "mortal_body";

	/** The elements of {@code physique}'s roots ({@link #rootCount} of them): {@code known} first (methods already learned), then at random. */
	public static EnumSet<QiElement> rollRoots(Physique physique, java.util.Collection<QiElement> known, RandomSource random) {
		return rollRoots(physique.rootCount(), known, random);
	}

	/** {@code count} elements for roots: {@code known} first (methods already learned), then at random. */
	public static EnumSet<QiElement> rollRoots(int count, java.util.Collection<QiElement> known, RandomSource random) {
		EnumSet<QiElement> roots = EnumSet.noneOf(QiElement.class);
		for (QiElement element : known) if (roots.size() < count) roots.add(element);
		List<QiElement> rest = new java.util.ArrayList<>(List.of(QiElement.five()));
		rest.removeAll(roots);
		while (roots.size() < count && !rest.isEmpty()) roots.add(rest.remove(random.nextInt(rest.size())));
		return roots;
	}

	public static Physique byId(String id) {
		if (MORTAL_BODY_ID.equals(id)) return FIVE_ELEMENT_ROOTS;
		for (Physique p : values()) if (p.id.equals(id)) return p;
		return null;
	}

	public static Physique byIndex(int index) {
		return index >= 0 && index < values().length ? values()[index] : null;
	}
}
