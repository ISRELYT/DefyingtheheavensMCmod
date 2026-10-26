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
	/**
	 * From Foundation Building, on either path (TwoPaths#awakens: the realm's abilities come from either one): hunger and
	 * saturation always stay full ({@link CultivationStats#tickHunger}).
	 */
	QI_SUSTENANCE("qi_sustenance", true, c -> TwoPaths.awakens(c, Realm.FOUNDATION_BUILDING, false) != null),
	/** From Core Formation on the heaven path or the Demon Path (PlayerCultivation#flightRealm; the ghost path's own flight is a ghostly form's, TwoPaths#fliesFreely): fly on qi ({@link QiFlight}). */
	QI_FLIGHT("qi_flight", true, PlayerCultivation::canFlyOnQi),
	/** From Core Formation, on either path: see the plane of Qi, free of cost ({@link QiSense}, drawn by the client). */
	QI_SENSE("qi_sense", true, c -> TwoPaths.awakens(c, Realm.CORE_FORMATION, true) != null),
	/** From the start: see everything inside the consciousness domain, free of cost ({@link ConsciousnessDomainHandler}). */
	CONSCIOUSNESS_DOMAIN("consciousness_domain", true, c -> true),
	/** From the start, off until switched on: weigh down everything weaker in the domain, for qi ({@link RealmSuppressSystem}). */
	REALM_SUPPRESS("realm_suppress", false, c -> true),
	/** From the start: after meditating a while, the soul turns inward to the Inner Realm ({@link InnerRealm}). */
	INNER_REALM("inner_realm", true, c -> true),
	/** From Foundation Building: ride a sword held in the main hand through the air, for less qi than Qi Flight ({@link SwordFlight}). */
	SWORD_FLIGHT("sword_flight", true, PlayerCultivation::canRideSword),
	/**
	 * From Nascent Soul on either path, the living and the dead alike (the user's call, 2026-10-19): send the nascent soul out of the
	 * body ({@link SoulProjection}; the soul is the path's that awakened it, PlayerCultivation#soulPath), a ghost's out of its ghostly
	 * form, which sits on in its light. Unlike the others it is an act, not a standing switch: "on" while the soul is out (or going
	 * out), "off" once it is back.
	 */
	SOUL_PROJECTION("soul_projection", false, c -> TwoPaths.awakens(c, Realm.NASCENT_SOUL, true) != null),
	/**
	 * Netherworld Breathing Art, third layer: ghosts, ghost cultivators and Method Steles glow through walls within 32 blocks;
	 * outside the Netherworld, projected souls are seen as a Nascent Soul with Qi Sense open would see them. Ghostly qi upkeep.
	 */
	GHOST_EYE("ghost_eye", false, true, c -> c.netherworld().layer(GhostMethod.NETHERWORLD_BREATHING) >= 3),
	/** Netherworld Breathing Art, sixth layer: the living take the Netherworld's yin at half the rate. Ghostly qi upkeep. */
	NETHERWORLD_VEIL("netherworld_veil", false, true, c -> c.netherworld().layer(GhostMethod.NETHERWORLD_BREATHING) >= 6),
	/** Wandering Soul Steps, first layer: swifter, and falls do no harm. Ghostly qi upkeep. */
	YIN_WIND_STEPS("yin_wind_steps", false, true, c -> c.netherworld().layer(GhostMethod.WANDERING_SOUL_STEPS) >= 1),
	/**
	 * Retired 2026-10-18: it made the dead fly at Qi Flight's speed on ghostly qi (Wandering Soul Steps' fourth layer); now every
	 * ghost, and the living in a Ghost King's form, flies so for nothing (TwoPaths#fliesFreely). Kept, never awakened, so the
	 * abilities after it keep their places in the saved and synced switches.
	 */
	GHOST_FLIGHT("ghost_flight", false, true, c -> false),
	/**
	 * From a ghost path of Core Formation, or once a Ghost King among the dead, for the living; and for a Ghost King among the dead
	 * (TwoPaths#formsOpen): the other form. One's true form is the higher path's, human
	 * or a Ghost King's; switched on, the other is shown instead. In a Ghost King's form one is seen as the dead are, outside the
	 * Netherworld only by those who perceive souls. At no cost.
	 */
	FORM_SHIFT("form_shift", false, true, TwoPaths::formsOpen),
	/**
	 * From Core Formation on either path, the living: detonate the core (CoreDetonation; the heaven path's golden core, else the
	 * ghost path's). An act, asked twice: "on" while it gathers. The cultivator dies, that path's core lost (back to Foundation
	 * Building Grand Perfection); everything near takes the blast, by distance and by how far its realm stands from theirs.
	 */
	CORE_DETONATION("core_detonation", false, c -> !c.netherworld().ghost() && TwoPaths.awakens(c, Realm.CORE_FORMATION, false) != null),
	/**
	 * From Heavenly Being on either path, the living: tear space open and step into the Spatial Gap, or out of it
	 * (SpatialTrialHandler, Route TRAVEL), for that path's pool. A block crossed in the gap is twenty in the world. An act: "on"
	 * while in the gap by one's own tear.
	 */
	SPATIAL_TEAR("spatial_tear", false, c -> !c.netherworld().ghost() && TwoPaths.awakens(c, Realm.HEAVENLY_BEING, false) != null),
	/**
	 * From Qi Refining, for every cultivator, the living and the dead (the user's ask, 2026-10-21): one's own years set alight for
	 * strength (BurnLifespan), a standing switch: while on, BurnLifespan#SPAN_PER_SECOND of one's whole span a second (a share, so
	 * every realm alike), for blows and spells half again as hard and a quarter more speed. It goes out by itself before it would
	 * leave fewer than Lifespan#WARN_YEARS.
	 */
	BURN_LIFESPAN("burn_lifespan", false, c -> true),
	/**
	 * A technique, not the realm's (Technique#CLONE, from its manual; the user's ask, 2026-10-23), wielded from Nascent Soul by the
	 * living: split a clone off one's body, go into it and back, and draw it back in (Clones). An act: "on" while one is out.
	 */
	CLONE("clone", false, c -> !c.netherworld().ghost() && c.knowsTechnique(Technique.CLONE) && Technique.CLONE.wieldable(c)),
	/**
	 * From Core Formation on either path (the user's ask, 2026-10-23): Qi Flight's fast mode, a standing switch, off by default
	 * ({@link MeteorFlight}). Off, every realm flies at creative flight's own speed, for fine work; on, one flies where one looks,
	 * as fast as one's realm allows, and from Nascent Soul one comes ablaze and smashes through what stands in the way. The free
	 * flight of the dead and of a Ghost King's form goes by it too.
	 */
	METEOR_FLIGHT("meteor_flight", false, c -> TwoPaths.awakens(c, Realm.CORE_FORMATION, true) != null),
	/**
	 * A tiger's, from Core Formation on any path (RaceGifts#roar): those near and far enough below it weakened and slowed. An act, for qi,
	 * with a cooldown.
	 */
	TIGERS_ROAR("tigers_roar", false, c -> RaceGifts.hasAct(c, Bloodline.TIGER)),
	/** A fox spirit's, from Core Formation on any path (RaceGifts#illusion): a while unseen, till it strikes. An act, for qi, with a cooldown. */
	ILLUSION("illusion", false, c -> RaceGifts.hasAct(c, Bloodline.FOX)),
	/**
	 * From True Immortal on any path (phase 2, 2026-10-25): one's immortal essence set to work (ImmortalEssence), a standing switch, off
	 * to begin with: while on, blows and spells half again as hard, for a share of the essence pool a second. The pool fills only in the
	 * Immortal Realm and leaks away anywhere else.
	 */
	IMMORTAL_ESSENCE("immortal_essence", false, c -> TwoPaths.awakens(c, Realm.TRUE_IMMORTAL, false) != null);

	private final String id;
	private final boolean onByDefault;
	/** A ghost method's ability (the ghostly qi pool pays any upkeep it has), not the realm's. */
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
	 * The cultivator has awakened this ability (whether or not it is switched on). A mortal has none, alive or dead: its
	 * meridians are closed, the realm's abilities and the ghost methods' alike.
	 */
	public boolean isUnlocked(PlayerCultivation c) {
		if (this == QI_SENSE && c.hasPhysique(Physique.BLESSED_MERIDIANS)) return true; // open from a mortal's first day (Physique)
		// A soul in another's body (Possession), a mortal's too, keeps the way out of it, and its own tear if the body has none.
		if (this == SOUL_PROJECTION && Possession.isBorrowed(c) || this == SPATIAL_TEAR && Possession.soulTears(c)) return true;
		// A clone (Clones) keeps the way back to its main body, whatever its own realm.
		if (this == CLONE && Clones.isCloneRecord(c)) return true;
		return !c.isMortal() && unlocked.test(c);
	}

	public Component getDisplayName() { return Component.translatable(ModLang.abilityKey(id)); }

	/** What the ability does, with this cultivator's numbers filled in (as the ghost path's, if that path awakened it: {@link #awakenedBy}). */
	public Component getDescription(PlayerCultivation c) {
		// In another's body (Possession), Soul Projection is the way out of it.
		if (this == SOUL_PROJECTION && Possession.isBorrowed(c)) return Component.translatable(ModLang.abilityKey(id) + ".description.leave");
		String key = ModLang.abilityKey(id) + ".description" + (awakenedBy(c) == TwoPaths.Path.GHOST && hasGhostWording() ? ".ghost" : "");
		return switch (this) {
			case QI_FLIGHT -> Component.translatable(key, Math.round(c.qiFlightCostPerSecond()));
			case SWORD_FLIGHT -> Component.translatable(key, Math.round(c.swordFlightCostPerSecond()));
			case CONSCIOUSNESS_DOMAIN, REALM_SUPPRESS -> Component.translatable(key, Math.round(ConsciousnessDomainHandler.radius(c)));
			case SOUL_PROJECTION -> {
				// From Void Shattering its tether has no end, and it costs what it does beside the body wherever it goes.
				Component said = c.soulTetherEndless()
						? Component.translatable(key + ".endless", c.soulHearts(), Math.round(c.soulCostPerSecond(0, false)))
						: Component.translatable(key, c.soulHearts(), c.soulTetherRadius(), Math.round(c.soulCostPerSecond(0, false)),
								Math.round(c.soulCostPerSecond(1, false)));
				// Wounds it carries yet (SoulProjection#mendSouls): how many of its hearts it would set out on, to the half heart.
				if (c.getSoulWounds() <= 0) yield said;
				double whole = c.soulHearts();
				double left = Math.max(0.5, Math.floor((whole * 2 - c.getSoulWounds())) / 2.0);
				yield said.copy().append(" ").append(Component.translatable(ModLang.ABILITY_SOUL_WOUNDED,
						left == Math.floor(left) ? String.valueOf((long) left) : String.valueOf(left), c.soulHearts()));
			}
			case GHOST_EYE -> Component.translatable(key, GhostMethods.GHOST_EYE_COST);
			case NETHERWORLD_VEIL -> Component.translatable(key, Math.round(GhostMethods.VEIL_COST));
			case YIN_WIND_STEPS -> Component.translatable(key, Math.round(GhostMethods.YIN_WIND_SPEED * 100), Math.round(GhostMethods.YIN_WIND_COST));
			case GHOST_FLIGHT -> Component.translatable(key, Math.round(GhostMethods.GHOST_FLIGHT_COST));
			case FORM_SHIFT -> Component.translatable(key, TwoPaths.trueForm(c).getDisplayName(), TwoPaths.trueForm(c).other().getDisplayName());
			case CORE_DETONATION -> Component.translatable(key, Math.round(CoreDetonation.radius(c)));
			case SPATIAL_TEAR -> Component.translatable(key, SpatialTrialHandler.GAP_SCALE, Math.round(SpatialTrialHandler.TEAR_IN_QI * 100),
					Math.round(SpatialTrialHandler.TEAR_OUT_QI * 100));
			case BURN_LIFESPAN -> Component.translatable(key, BurnLifespan.percent(), Math.round((BurnLifespan.HARM - 1) * 100),
					Math.round((BurnLifespan.SPEED - 1) * 100), (long) Lifespan.WARN_YEARS);
			// In the clone: the way back (and, near the main body, in); else the clone, its realm and its cost.
			case CLONE -> Clones.isCloneRecord(c) ? Component.translatable(key + ".clone", Math.round(Clones.ABSORB_RANGE))
					: Component.translatable(key, Math.round(Clones.STAT_SHARE * 100), Math.round(Clones.SUMMON_SHARE * 100), Math.round(Clones.ABSORB_RANGE),
							Clones.REFORM_TICKS / 1200);
			// Its top speed, and what it costs flying fast (three times Qi Flight's; from Heavenly Being never more than one gathers).
			case METEOR_FLIGHT -> Component.translatable(key, Math.round(MeteorFlight.topSpeed(c)), Math.round(MeteorFlight.streakCost(c, c.qiFlightCostPerSecond())));
			case TIGERS_ROAR -> Component.translatable(key, RaceGifts.ROAR_RADIUS, RaceGifts.ROAR_STAGES_BELOW, RaceGifts.ROAR_TICKS / 20,
					RaceGifts.ROAR_COOLDOWN / 20, Math.round(RaceGifts.ROAR_QI * 100));
			case ILLUSION -> Component.translatable(key, RaceGifts.ILLUSION_TICKS / 20, RaceGifts.ILLUSION_COOLDOWN / 20, Math.round(RaceGifts.ILLUSION_QI * 100));
			// The harm it adds, how long a full pool lasts, the pool, how long the Immortal Realm takes to fill it, and to leak it elsewhere.
			case IMMORTAL_ESSENCE -> Component.translatable(key, Math.round((ImmortalEssence.HARM - 1) * 100), ImmortalEssence.BURN_SECONDS,
					Math.round(ImmortalEssence.max(c)), ImmortalEssence.FILL_SECONDS / 60, ImmortalEssence.MEDITATING, ImmortalEssence.LEAK_SECONDS / 60);
			default -> Component.translatable(key);
		};
	}

	/**
	 * Which path awakened this realm's ability for {@code c} (TwoPaths#awakens; its costs come from that path's pool), or null for
	 * one not awakened by a realm (a ghost method's, the domain's) or not awakened at all.
	 */
	public TwoPaths.Path awakenedBy(PlayerCultivation c) {
		return switch (this) {
			case QI_SUSTENANCE -> TwoPaths.awakens(c, Realm.FOUNDATION_BUILDING, false);
			case QI_SENSE -> TwoPaths.awakens(c, Realm.CORE_FORMATION, true);
			case SOUL_PROJECTION -> TwoPaths.awakens(c, Realm.NASCENT_SOUL, true);
			case CORE_DETONATION -> TwoPaths.awakens(c, Realm.CORE_FORMATION, false);
			case SPATIAL_TEAR -> TwoPaths.awakens(c, Realm.HEAVENLY_BEING, false);
			case QI_FLIGHT, SWORD_FLIGHT -> isUnlocked(c) ? TwoPaths.Path.HEAVEN : null;
			default -> null;
		};
	}

	/** The ability has its own wording when the ghost path awakened it (its cost in ghost qi, its loss the ghost path's). */
	private boolean hasGhostWording() {
		return this == SOUL_PROJECTION || this == CORE_DETONATION || this == SPATIAL_TEAR;
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
