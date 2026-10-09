package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;

import java.util.function.Predicate;

/**
 * Abilities a cultivator awakens as they advance, listed on the cultivation menu's Abilities tab where each can be switched
 * on or off. Every ability starts switched on; {@link PlayerCultivation} remembers the ones the player turned off.
 * Add new abilities at the end: the switched-off set is synced as a bit mask over the ordinals.
 */
public enum Ability {
	/** From Foundation Building: hunger and saturation always stay full ({@link CultivationStats#tickHunger}). */
	QI_SUSTENANCE("qi_sustenance", c -> c.getRealm().isSustainedByQi()),
	/** From Core Formation: fly on qi ({@link QiFlight}). */
	QI_FLIGHT("qi_flight", c -> c.getEffectiveRealm().canFlyOnQi());

	private final String id;
	private final Predicate<PlayerCultivation> unlocked;

	Ability(String id, Predicate<PlayerCultivation> unlocked) {
		this.id = id;
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
			default -> Component.translatable(key);
		};
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
