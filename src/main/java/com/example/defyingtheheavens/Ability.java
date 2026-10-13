package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;

import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * Abilities a cultivator awakens as they advance, listed on the cultivation menu's Abilities tab where each can be switched
 * on or off. Most start switched on; those that aren't {@code onByDefault} start off until the player turns them on.
 * {@link PlayerCultivation} remembers every switch.
 * Add new abilities at the end: the switched-off set is synced as a bit mask over the ordinals. Spells (Spell) are not
 * abilities: they are cast, not switched, and have their own wheel.
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
	REALM_SUPPRESS("realm_suppress", false, c -> true),
	/** From the start: after meditating a while, the soul turns inward to the Inner Realm ({@link InnerRealm}). */
	INNER_REALM("inner_realm", true, c -> true),
	/** From Foundation Building: ride a sword held in the main hand through the air, for less qi than Qi Flight ({@link SwordFlight}). */
	SWORD_FLIGHT("sword_flight", true, c -> c.getEffectiveRealm().canRideSword()),
	/**
	 * From Nascent Soul: send the nascent soul out of the body ({@link SoulProjection}). Unlike the others it is an act, not a
	 * standing switch: "on" while the soul is out (or going out), "off" once it is back.
	 */
	SOUL_PROJECTION("soul_projection", false, c -> c.getEffectiveRealm().canProjectSoul()),
	/**
	 * Netherworld Breathing Art, third layer: ghosts, ghost cultivators and Method Steles glow through walls within 32 blocks;
	 * outside the Netherworld, projected souls are seen as a Nascent Soul with Qi Sense open would see them. Ghostly qi upkeep.
	 */
	GHOST_EYE("ghost_eye", false, true, c -> c.netherworld().layer(GhostMethod.NETHERWORLD_BREATHING) >= 3),
	/** Netherworld Breathing Art, sixth layer: the living take the Netherworld's yin at half the rate. Ghostly qi upkeep. */
	NETHERWORLD_VEIL("netherworld_veil", false, true, c -> c.netherworld().layer(GhostMethod.NETHERWORLD_BREATHING) >= 6),
	/** Wandering Soul Steps, first layer: swifter, and falls do no harm. Ghostly qi upkeep. */
	YIN_WIND_STEPS("yin_wind_steps", false, true, c -> c.netherworld().layer(GhostMethod.WANDERING_SOUL_STEPS) >= 1),
	/** Wandering Soul Steps, fourth layer: a ghost flies freely, on ghostly qi (never running out in the Netherworld). */
	GHOST_FLIGHT("ghost_flight", false, true, c -> c.netherworld().ghost() && c.netherworld().layer(GhostMethod.WANDERING_SOUL_STEPS) >= 4);

	private final String id;
	private final boolean onByDefault;
	/** A ghost method's ability: a mortal's ghost may use it too (a living mortal still can't). */
	private final boolean ghostly;
	private final Predicate<PlayerCultivation> unlocked;

	Ability(String id, boolean onByDefault, Predicate<PlayerCultivation> unlocked) {
		this(id, onByDefault, false, unlocked);
	}

	Ability(String id, boolean onByDefault, boolean ghostly, Predicate<PlayerCultivation> unlocked) {
		this.id = id;
		this.onByDefault = onByDefault;
		this.ghostly = ghostly;
		this.unlocked = unlocked;
	}

	public String getId() { return id; }

	/** Whether it comes from a ghost method (paid for from the ghostly qi pool) rather than the realm. */
	public boolean isGhostly() { return ghostly; }

	/**
	 * The cultivator has awakened this ability (whether or not it is switched on). A living mortal has none; a mortal's ghost has
	 * its ghost methods' ones.
	 */
	public boolean isUnlocked(PlayerCultivation c) {
		boolean canHold = !c.isMortal() || ghostly && c.netherworld().ghost();
		return canHold && unlocked.test(c);
	}

	public Component getDisplayName() { return Component.translatable(ModLang.abilityKey(id)); }

	/** What the ability does, with this cultivator's numbers filled in. */
	public Component getDescription(PlayerCultivation c) {
		String key = ModLang.abilityKey(id) + ".description";
		return switch (this) {
			case QI_FLIGHT -> Component.translatable(key, Math.round(c.qiFlightCostPerSecond()));
			case SWORD_FLIGHT -> Component.translatable(key, Math.round(c.swordFlightCostPerSecond()));
			case CONSCIOUSNESS_DOMAIN, REALM_SUPPRESS -> Component.translatable(key, Math.round(ConsciousnessDomainHandler.radius(c)));
			case SOUL_PROJECTION -> Component.translatable(key, c.soulHearts(), c.soulTetherRadius(), Math.round(c.soulCostPerSecond(0, false)),
					Math.round(c.soulCostPerSecond(1, false)));
			case GHOST_EYE -> Component.translatable(key, GhostMethods.GHOST_EYE_COST);
			case NETHERWORLD_VEIL -> Component.translatable(key, Math.round(GhostMethods.VEIL_COST));
			case YIN_WIND_STEPS -> Component.translatable(key, Math.round(GhostMethods.YIN_WIND_SPEED * 100), Math.round(GhostMethods.YIN_WIND_COST));
			case GHOST_FLIGHT -> Component.translatable(key, Math.round(GhostMethods.GHOST_FLIGHT_COST));
			default -> Component.translatable(key);
		};
	}

	/**
	 * The ability this one can't be switched on alongside, or null: only one of Qi Flight and Sword Flight is on at a time,
	 * so switching one on switches the other off ({@link PlayerCultivation#setAbilityEnabled}).
	 */
	public Ability exclusiveWith() {
		return switch (this) {
			case QI_FLIGHT -> SWORD_FLIGHT;
			case SWORD_FLIGHT -> QI_FLIGHT;
			default -> null;
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
