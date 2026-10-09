package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;

import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * Abilities a cultivator awakens as they advance, listed on the cultivation menu's Abilities tab where each can be switched
 * on or off. Most start switched on; those that aren't {@code onByDefault} start off until the player turns them on.
 * {@link PlayerCultivation} remembers every switch.
 * Add new abilities at the end: the switched-off set is synced as a bit mask over the ordinals.
 */
public enum Ability {
	/** From Foundation Building: hunger and saturation always stay full ({@link CultivationStats#tickHunger}). */
	QI_SUSTENANCE("qi_sustenance", true, c -> c.getRealm().isSustainedByQi()),
	/** From Core Formation: fly on qi ({@link QiFlight}). */
	QI_FLIGHT("qi_flight", true, c -> c.getEffectiveRealm().canFlyOnQi()),
	/** From Core Formation: see the plane of Qi, free of cost ({@link QiSense}, drawn by the client). */
	QI_SENSE("qi_sense", true, c -> c.getEffectiveRealm().canSenseQi()),
	/** From the start: see everything inside the consciousness domain, free of cost ({@link ConsciousnessDomainHandler}). */
	CONSCIOUSNESS_DOMAIN("consciousness_domain", true, c -> true),
	/** From the start, off until switched on: weigh down everything weaker in the domain, for qi ({@link RealmSuppressSystem}). */
	REALM_SUPPRESS("realm_suppress", false, c -> true);

	private final String id;
	private final boolean onByDefault;
	private final Predicate<PlayerCultivation> unlocked;

	Ability(String id, boolean onByDefault, Predicate<PlayerCultivation> unlocked) {
		this.id = id;
		this.onByDefault = onByDefault;
		this.unlocked = unlocked;
	}

	public String getId() { return id; }

	/** The cultivator's realm has awakened this ability (whether or not it is switched on). */
	public boolean isUnlocked(PlayerCultivation c) { return unlocked.test(c); }

	public Component getDisplayName() { return Component.translatable(ModLang.abilityKey(id)); }

	/** What the ability does, with this cultivator's numbers filled in. */
	public Component getDescription(PlayerCultivation c) {
		String key = ModLang.abilityKey(id) + ".description";
		return switch (this) {
			case QI_FLIGHT -> Component.translatable(key, Math.round(c.qiFlightCostPerSecond()));
			case CONSCIOUSNESS_DOMAIN, REALM_SUPPRESS -> Component.translatable(key, Math.round(ConsciousnessDomainHandler.radius(c)));
			default -> Component.translatable(key);
		};
	}

	/** The abilities a new cultivator starts with switched off. */
	public static EnumSet<Ability> offByDefault() {
		EnumSet<Ability> off = EnumSet.noneOf(Ability.class);
		for (Ability ability : values()) {
			if (!ability.onByDefault) off.add(ability);
		}
		return off;
	}

	public static Ability byId(String id) {
		for (Ability ability : values()) {
			if (ability.id.equals(id)) return ability;
		}
		return null;
	}

	/** @return null if out of range (a packet from a client with a different ability list) */
	public static Ability byIndex(int index) {
		return index >= 0 && index < values().length ? values()[index] : null;
	}
}
