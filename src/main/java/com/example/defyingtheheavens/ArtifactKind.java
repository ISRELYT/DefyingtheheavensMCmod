package com.example.defyingtheheavens;

import org.jetbrains.annotations.Nullable;

/**
 * What an artifact is (ArtifactRecipes; the user's picks, 2026-10-28): four blades of metal (the flying sword, a jian its rider flies on
 * for less qi; the heavy saber; the spear that pierces; the glaive that sweeps), the element banner, the qi gourd and the protective jade,
 * each in every grade (ArtifactGrade); and since phase 2, the five weapons made of their element (ElementalWeaponItem: the user's ask,
 * "some made of their element"), from Spirit grade up, for it takes a demon core of their element to give the element a shape. Its item
 * id is the grade's and the kind's ("spirit_flying_sword", "heaven_flame_blade").
 */
public enum ArtifactKind {
	FLYING_SWORD("flying_sword", true, null),
	SABER("saber", true, null),
	SPEAR("spear", true, null),
	GLAIVE("glaive", true, null),
	BANNER("element_banner", false, null),
	GOURD("qi_gourd", false, null),
	JADE("protective_jade", false, null),
	FLAME_BLADE("flame_blade", true, QiElement.FIRE),
	TIDE_SPEAR("tide_spear", true, QiElement.WATER),
	VINE_WHIP("vine_whip", true, QiElement.WOOD),
	MOUNTAIN_HAMMER("mountain_hammer", true, QiElement.EARTH),
	GOLDEN_SWORD("golden_light_sword", true, QiElement.METAL);

	private final String id;
	private final boolean weapon;
	private final QiElement element;

	ArtifactKind(String id, boolean weapon, @Nullable QiElement element) {
		this.id = id;
		this.weapon = weapon;
		this.element = element;
	}

	public String getId() { return id; }
	public boolean isWeapon() { return weapon; }
	/** The element a weapon is made of (null: none, a phase 1 artifact). */
	public @Nullable QiElement element() { return element; }
	public boolean isElemental() { return element != null; }

	/** Whether it is forged at {@code grade}: every grade, an elemental weapon from Spirit up. */
	public boolean has(ArtifactGrade grade) {
		return element == null || grade != ArtifactGrade.MORTAL;
	}

	/** The weapon made of {@code element}. */
	public static ArtifactKind madeOf(QiElement element) {
		for (ArtifactKind kind : values()) if (kind.element == element) return kind;
		throw new IllegalArgumentException("No weapon is made of " + element);
	}

	/** The item id of this kind at {@code grade}. */
	public String itemId(ArtifactGrade grade) {
		return grade.getId() + "_" + id;
	}
}
