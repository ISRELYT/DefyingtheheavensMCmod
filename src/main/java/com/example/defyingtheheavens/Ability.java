package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;

import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * Abilities a cultivator awakens as they advance, listed on the cultivation menu's Abilities tab where each can be switched
 * on or off. Most start switched on; those that aren't {@code onByDefault} start off until the player turns them on.
 * {@link PlayerCultivation} remembers every switch.
 * Add new abilities at the end: the switched-off set is synced as a bit mask over the ordinals (a long: 64 at most). Spells
 * (Spell) are not abilities: they are cast, not switched, and have their own wheel.
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
	IMMORTAL_ESSENCE("immortal_essence", false, c -> TwoPaths.awakens(c, Realm.TRUE_IMMORTAL, false) != null),
	/**
	 * The Law domains (phase 3b, 2026-10-26), one for each Law from its first level comprehended, for an immortal (LawDomains): the Law's
	 * own ground unfurled round one, a standing switch, off to begin with, one domain lit at a time, burning immortal essence. Foes in it
	 * are worked on by its Law, and one's own spells of the Law strike them the harder.
	 */
	METAL_DOMAIN("metal_domain", false, c -> LawDomains.unlocked(c, Law.METAL)),
	WOOD_DOMAIN("wood_domain", false, c -> LawDomains.unlocked(c, Law.WOOD)),
	WATER_DOMAIN("water_domain", false, c -> LawDomains.unlocked(c, Law.WATER)),
	FIRE_DOMAIN("fire_domain", false, c -> LawDomains.unlocked(c, Law.FIRE)),
	EARTH_DOMAIN("earth_domain", false, c -> LawDomains.unlocked(c, Law.EARTH)),
	SPACE_DOMAIN("space_domain", false, c -> LawDomains.unlocked(c, Law.SPACE)),
	TIME_DOMAIN("time_domain", false, c -> LawDomains.unlocked(c, Law.TIME)),
	LIFE_AND_DEATH_DOMAIN("life_and_death_domain", false, c -> LawDomains.unlocked(c, Law.LIFE_AND_DEATH)),
	/**
	 * Once a World Seed is refined into one's own small world (phase 3d, 2026-10-26; SmallWorlds), for an immortal: its door opened before
	 * one wherever one stands, for its owner and its guests to walk through; inside it, the way out where one came in. An act: "on" while
	 * its door stands open or one is in it.
	 */
	SMALL_WORLD("small_world", false, c -> c.hasSmallWorld() && Laws.isQiImmortal(c)),
	/**
	 * A technique, not the realm's (Technique#SLEEVE, Universe in a Sleeve, from its manual; the user's ask, 2026-10-26), wielded from
	 * Heavenly Being by the living: offer the player one looks at a place in one's sleeve, to carry them wherever one goes, up through the
	 * passages between the realms whatever their realm; sneaking, let them out (Sleeves). An act: "on" while the sleeve holds anyone.
	 */
	SLEEVE("sleeve", false, c -> !c.netherworld().ghost() && c.knowsTechnique(Technique.SLEEVE) && Technique.SLEEVE.wieldable(c)),
	/**
	 * A technique (Technique#SWORD_INTENT, the Sunken Sword Tomb's Great Inheritance, 2026-10-31), wielded from Core Formation: a standing
	 * switch, off to begin with, for qi a second (WarArts): every blow one lands with a blade is followed by a second cut of lingering intent,
	 * and the tomb's own intent doesn't cut one.
	 */
	SWORD_INTENT("sword_intent", false, c -> c.knowsTechnique(Technique.SWORD_INTENT) && Technique.SWORD_INTENT.wieldable(c)),
	/**
	 * A technique (Technique#BLOOD_BATTLE, the Ancient Battlefield's Great Inheritance, 2026-10-31), wielded from Nascent Soul: a standing
	 * switch, off to begin with, for qi a second (WarArts): every blow one lands heals one a share of it, and the more foes about one, the less
	 * their blows harm.
	 */
	BLOOD_BATTLE("blood_battle", false, c -> c.knowsTechnique(Technique.BLOOD_BATTLE) && Technique.BLOOD_BATTLE.wieldable(c)),
	/**
	 * The Body Path's techniques (BodyTechniques, phase 2, 2026-11-01), each awakened as the body reaches its realm on the Body Path, the
	 * living's alone, a mortal's too ({@link #isUnlocked}); each spends vigor, the body's own pool, not qi. Iron Skin (Copper Skin): an act,
	 * "on" while it holds.
	 */
	IRON_SKIN("iron_skin", false, c -> BodyTechniques.has(c, Realm.QI_REFINING)),
	/** Mountain Stomp (Iron Flesh): an act. */
	MOUNTAIN_STOMP("mountain_stomp", false, c -> BodyTechniques.has(c, Realm.FOUNDATION_BUILDING)),
	/** Ground-Splitting Leap (Steel Sinew): an act, "on" while one is in the air on it. */
	GROUND_SPLITTING_LEAP("ground_splitting_leap", false, c -> BodyTechniques.has(c, Realm.CORE_FORMATION)),
	/** Thousand-Pound Fist (Jade Bone): an act, "on" while the fist waits primed. */
	THOUSAND_POUND_FIST("thousand_pound_fist", false, c -> BodyTechniques.has(c, Realm.NASCENT_SOUL)),
	/** Blood Frenzy (Silver Marrow): a standing switch, off to begin with, for vigor a second. */
	BLOOD_FRENZY("blood_frenzy", false, c -> BodyTechniques.has(c, Realm.HEAVENLY_BEING)),
	/** Golden Body (Golden Blood): an act, "on" while it holds. */
	GOLDEN_BODY("golden_body", false, c -> BodyTechniques.has(c, Realm.FOUR_AXIS)),
	/** The Body Path's upper techniques (the user's "Go", 2026-11-01). Diamond Will (Diamond Heart): an act, "on" while it holds. */
	DIAMOND_WILL("diamond_will", false, c -> BodyTechniques.has(c, Realm.DAO_INTEGRATION)),
	/** Comet Charge (the Star-Forged Body): an act, "on" while one charges. */
	COMET_CHARGE("comet_charge", false, c -> BodyTechniques.has(c, Realm.VOID_SHATTERING)),
	/** Rebounding Bones (the Heaven-Bone Body): an act, "on" while it holds. */
	REBOUNDING_BONES("rebounding_bones", false, c -> BodyTechniques.has(c, Realm.SACRED_VESSEL)),
	/** Flesh Reborn (the Undying Body): an act, "on" while the flesh grows back. */
	FLESH_REBORN("flesh_reborn", false, c -> BodyTechniques.has(c, Realm.ENTERING_NIRVANA)),
	/** Heaven-Shattering Fist (the Indestructible Body): an act. */
	HEAVEN_SHATTERING_FIST("heaven_shattering_fist", false, c -> BodyTechniques.has(c, Realm.TRUE_IMMORTAL)),
	/**
	 * Cloud-Treading Steps (Silver Marrow; the body's way up, the user's "Go, all three parts", 2026-11-01): a standing switch, off to begin
	 * with: the body strides on the air while it is on, for vigor a second in the air (QiFlight, BodyTechniques#treads).
	 */
	CLOUD_TREADING("cloud_treading", false, c -> BodyTechniques.has(c, Realm.HEAVENLY_BEING)),
	/**
	 * The Body Path's techniques of the ladder past the Indestructible Body (the user's "Go, all three parts", 2026-11-01), one for each of its
	 * realms. Earthshaker (the Earth-Bearing Body): an act, "on" while its rings run out.
	 */
	EARTHSHAKER("earthshaker", false, c -> BodyTechniques.has(c, Realm.EARTH_IMMORTAL)),
	/** Sky-Pillar Strike (the Sky-Pillar Body): an act, "on" while one is up on it. */
	SKY_PILLAR_STRIKE("sky_pillar_strike", false, c -> BodyTechniques.has(c, Realm.HEAVEN_IMMORTAL)),
	/** Imperishable Gold (the Imperishable Gold Body): an act, "on" while it holds. */
	IMPERISHABLE_GOLD("imperishable_gold", false, c -> BodyTechniques.has(c, Realm.GOLDEN_IMMORTAL)),
	/** Primordial Roar (the Primordial Saint Body): an act. */
	PRIMORDIAL_ROAR("primordial_roar", false, c -> BodyTechniques.has(c, Realm.TAIYI_GOLDEN_IMMORTAL)),
	/** Myriad Fists (the Chaos-Tempered Body): an act, "on" while its flurry runs. */
	MYRIAD_FISTS("myriad_fists", false, c -> BodyTechniques.has(c, Realm.PROFOUND_SOVEREIGN)),
	/** Dharma Body of Heaven and Earth (the Body of Heaven and Earth): an act, "on" while the body towers. */
	DHARMA_BODY("dharma_body", false, c -> BodyTechniques.has(c, Realm.HEAVENLY_LORD));

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
		// The Body Path's techniques are the flesh's, not the meridians': a mortal tempered on it wields them too (BodyTechniques).
		if (BodyTechniques.isTechnique(this)) return unlocked.test(c);
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
			// Its reach, how long a full pool lasts, how much harder the Law's spells strike in it, and the Law's own numbers at its level.
			case METAL_DOMAIN, WOOD_DOMAIN, WATER_DOMAIN, FIRE_DOMAIN, EARTH_DOMAIN, SPACE_DOMAIN, TIME_DOMAIN, LIFE_AND_DEATH_DOMAIN ->
					LawDomains.description(key, law(), c);
			// How long its door stands, how far out its land reaches by one's Laws now, and how much further a level of them takes it.
			case SMALL_WORLD -> Component.translatable(key, SmallWorlds.DOOR_TICKS / 20, SmallWorlds.radius(c.laws().total()), SmallWorlds.PER_LAW_LEVEL);
			// Its reach, how many it holds, and a True Immortal's share of its body at the Immortal Gate (in hundredths of a body).
			case SLEEVE -> Component.translatable(key, Math.round(Sleeves.REACH), Sleeves.MOST, Math.round(Sleeves.GATE_BASE_BODIES * 100));
			// The second cut's share of the blow, and what it costs a second.
			case SWORD_INTENT -> Component.translatable(key, Math.round(WarArts.INTENT_SHARE * 100), Math.round(WarArts.cost(c)));
			// The share of each blow healed, the harm spared for each foe about one past the first and at most, its reach, and its cost.
			case BLOOD_BATTLE -> Component.translatable(key, Math.round(WarArts.BLOOD_HEAL * 100), Math.round(WarArts.BLOOD_GUARD * 100),
					Math.round(WarArts.BLOOD_GUARD_MOST * 100), WarArts.BLOOD_REACH, Math.round(WarArts.cost(c)));
			// The Body Path's techniques (BodyTechniques): their numbers at the body's realm, their vigor, and how long till they may be used again.
			case IRON_SKIN -> Component.translatable(key, BodyTechniques.seconds(BodyTechniques.IRON_SKIN_TICKS), Math.round(BodyTechniques.ironSkinCut(c) * 100),
					BodyTechniques.number(BodyTechniques.IRON_SKIN_COST), BodyTechniques.seconds(BodyTechniques.IRON_SKIN_COOLDOWN));
			case MOUNTAIN_STOMP -> Component.translatable(key, BodyTechniques.number(BodyTechniques.stompRadius(c)), Math.round(BodyTechniques.STOMP_SHARE * 100),
					BodyTechniques.seconds(BodyTechniques.STOMP_SLOW_TICKS), BodyTechniques.number(BodyTechniques.STOMP_COST),
					BodyTechniques.seconds(BodyTechniques.STOMP_COOLDOWN));
			case GROUND_SPLITTING_LEAP -> Component.translatable(key, BodyTechniques.number(BodyTechniques.landingRadius(c)),
					Math.round(BodyTechniques.LANDING_SHARE * 100), BodyTechniques.number(BodyTechniques.LEAP_COST), BodyTechniques.seconds(BodyTechniques.LEAP_COOLDOWN));
			case THOUSAND_POUND_FIST -> Component.translatable(key, BodyTechniques.seconds(BodyTechniques.FIST_TICKS), BodyTechniques.number(BodyTechniques.fistFactor(c)),
					BodyTechniques.number(BodyTechniques.FIST_COST), BodyTechniques.seconds(BodyTechniques.FIST_COOLDOWN));
			case BLOOD_FRENZY -> Component.translatable(key, Math.round(BodyTechniques.frenzyHarm(c) * 100), Math.round(BodyTechniques.FRENZY_SPEED * 100),
					BodyTechniques.number(BodyTechniques.FRENZY_PER_SECOND));
			case GOLDEN_BODY -> Component.translatable(key, BodyTechniques.seconds(BodyTechniques.goldenTicks(c)), BodyTechniques.number(BodyTechniques.GOLDEN_COST),
					BodyTechniques.seconds(BodyTechniques.GOLDEN_COOLDOWN));
			case DIAMOND_WILL -> Component.translatable(key, BodyTechniques.seconds(BodyTechniques.diamondTicks(c)), BodyTechniques.number(BodyTechniques.DIAMOND_COST),
					BodyTechniques.seconds(BodyTechniques.DIAMOND_COOLDOWN));
			case COMET_CHARGE -> Component.translatable(key, BodyTechniques.cometBlocks(c), Math.round(BodyTechniques.COMET_SHARE * 100),
					BodyTechniques.number(BodyTechniques.COMET_COST), BodyTechniques.seconds(BodyTechniques.COMET_COOLDOWN));
			case REBOUNDING_BONES -> Component.translatable(key, BodyTechniques.seconds(BodyTechniques.reboundTicks(c)), Math.round(BodyTechniques.reboundShare(c) * 100),
					BodyTechniques.REBOUND_REACH, BodyTechniques.number(BodyTechniques.REBOUND_COST), BodyTechniques.seconds(BodyTechniques.REBOUND_COOLDOWN));
			case FLESH_REBORN -> Component.translatable(key, Math.round(BodyTechniques.rebornNow(c) * 100), Math.round(BodyTechniques.REBORN_OVER * 100),
					BodyTechniques.seconds(BodyTechniques.REBORN_TICKS), BodyTechniques.number(BodyTechniques.REBORN_COST),
					BodyTechniques.seconds(BodyTechniques.REBORN_COOLDOWN));
			case HEAVEN_SHATTERING_FIST -> Component.translatable(key, BodyTechniques.number(BodyTechniques.SHATTER_REACH),
					Math.round(BodyTechniques.SHATTER_SHARE * 100), BodyTechniques.number(BodyTechniques.SHATTER_COST),
					BodyTechniques.seconds(BodyTechniques.SHATTER_COOLDOWN));
			// Its vigor a second in the air, and its pace against Qi Flight's.
			case CLOUD_TREADING -> Component.translatable(key, BodyTechniques.number(BodyTechniques.TREAD_PER_SECOND),
					Math.round(BodyTechniques.TREAD_SPEED / SwordFlight.QI_FLIGHT_SPEED * 100));
			case EARTHSHAKER -> Component.translatable(key, BodyTechniques.EARTHSHAKER_RINGS, BodyTechniques.number(BodyTechniques.earthshakerReach(c)),
					Math.round(BodyTechniques.EARTHSHAKER_SHARE * 100), BodyTechniques.number(BodyTechniques.EARTHSHAKER_COST),
					BodyTechniques.seconds(BodyTechniques.EARTHSHAKER_COOLDOWN));
			case SKY_PILLAR_STRIKE -> Component.translatable(key, BodyTechniques.number(BodyTechniques.PILLAR_REACH), BodyTechniques.number(BodyTechniques.pillarRadius(c)),
					Math.round(BodyTechniques.PILLAR_SHARE * 100), BodyTechniques.number(BodyTechniques.PILLAR_COST), BodyTechniques.seconds(BodyTechniques.PILLAR_COOLDOWN));
			case IMPERISHABLE_GOLD -> Component.translatable(key, BodyTechniques.seconds(BodyTechniques.imperishableTicks(c)),
					BodyTechniques.number(BodyTechniques.GOLD_BURST_RADIUS), BodyTechniques.number(BodyTechniques.GOLD_BURST_MOST),
					BodyTechniques.number(BodyTechniques.GOLD_COST), BodyTechniques.seconds(BodyTechniques.GOLD_COOLDOWN));
			case PRIMORDIAL_ROAR -> Component.translatable(key, BodyTechniques.number(BodyTechniques.roarReach(c)), BodyTechniques.seconds(BodyTechniques.ROAR_STUN_TICKS),
					BodyTechniques.number(BodyTechniques.ROAR_COST), BodyTechniques.seconds(BodyTechniques.ROAR_COOLDOWN));
			case MYRIAD_FISTS -> Component.translatable(key, BodyTechniques.MYRIAD_BLOWS, BodyTechniques.seconds(BodyTechniques.MYRIAD_BLOWS * BodyTechniques.MYRIAD_EVERY),
					Math.round(BodyTechniques.myriadShare(c) * 100), BodyTechniques.number(BodyTechniques.MYRIAD_COST), BodyTechniques.seconds(BodyTechniques.MYRIAD_COOLDOWN));
			case DHARMA_BODY -> Component.translatable(key, BodyTechniques.seconds(BodyTechniques.DHARMA_TICKS), BodyTechniques.number(BodyTechniques.DHARMA_HEIGHT),
					Math.round(BodyTechniques.DHARMA_FIST_SHARE * 100), Math.round(BodyTechniques.DHARMA_STOMP_SHARE * 100),
					BodyTechniques.number(BodyTechniques.DHARMA_COST), BodyTechniques.seconds(BodyTechniques.DHARMA_COOLDOWN));
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

	/** The Law whose domain this is (LawDomains), or null for any other ability. */
	public Law law() {
		return switch (this) {
			case METAL_DOMAIN -> Law.METAL;
			case WOOD_DOMAIN -> Law.WOOD;
			case WATER_DOMAIN -> Law.WATER;
			case FIRE_DOMAIN -> Law.FIRE;
			case EARTH_DOMAIN -> Law.EARTH;
			case SPACE_DOMAIN -> Law.SPACE;
			case TIME_DOMAIN -> Law.TIME;
			case LIFE_AND_DEATH_DOMAIN -> Law.LIFE_AND_DEATH;
			default -> null;
		};
	}

	/** The domain of {@code law} (LawDomains). */
	public static Ability domainOf(Law law) {
		for (Ability ability : values()) if (ability.law() == law && law != null) return ability;
		return null;
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
