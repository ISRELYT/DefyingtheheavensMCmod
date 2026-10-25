package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;

/**
 * Spells: acts cast at will, unlike abilities, which are switched on and left on (see Spells for casting). Each comes with a
 * layer of a method, and costs ghostly qi; after it, a while must pass before it can be cast again. Add new spells at the end:
 * their ordinals are synced (saves use their ids).
 * <ul>
 *   <li>Ghost Fire (Ghost Fire Scripture, first layer): the hands take cold fire, and the next few blows set what they strike
 *       burning with it.</li>
 *   <li>Ghost Fire Bolt (fourth layer): a bolt of it, thrown.</li>
 *   <li>Soul Dash (Wandering Soul Steps, second layer): a rush forward on the yin wind.</li>
 *   <li>Ghostly Wail (Netherworld Breathing Art, fifth layer): a cry of the dead that drives back and weakens all around.</li>
 * </ul>
 * And each foundation after the Breathing Art two of its own, at the third and seventh layers of its own realm (the last nine of
 * its layers: GhostMethod#ownLayer):
 * <ul>
 *   <li>Yellow Springs Marrow Art: Yellow Springs Tide (a surge of the springs' water ahead), Marrow Ward (bones like iron).</li>
 *   <li>Ghost Core Canon: Ghost Core Burst (the core flares, burning all around), King's Decree (the hostile cowed).</li>
 *   <li>Nine Nether Soul Sutra: Soul Rend (a beam through every being in a line), Nine Nether Step (a step through the dark).</li>
 *   <li>Yama's Judgement Scripture: Yama's Verdict (one foe judged, the weak twice over), Chains of Hell (foes dragged in and
 *       held).</li>
 *   <li>Six Paths Reincarnation Canon: Wheel of Samsara (made whole), Six Paths Annihilation (all around unmade).</li>
 *   <li>Ten Thousand Ghosts Union (GhostSpells): Procession of Ten Thousand Ghosts (a column of the dead pours out along the look),
 *       Gate of Ten Thousand Ghosts (a gate of the dead opens where you look, drags all near into it, and slams shut).</li>
 *   <li>Void-Rending Nether Canon (GhostSpells): Rift to the Nine Nethers (the ground torn open onto the underworld, all near dragged
 *       down into it), Bridge of No Return (the Naihe Bridge rises along the look, and whoever it passes forgets every blessing).</li>
 * </ul>
 * And the spells of both paths ({@link #isDual}, see TwoPaths; HeavenDemonSpells), of no method: one for each realm from Foundation
 * Building, in reach once the heaven path and the Demon Path both stand there, cast by the living on qi alone (the Demon Path shares
 * the qi pool): heaven's gold light and a demon's crimson qi as one. Before 2026-10-24 they were the heaven path's and the ghost
 * path's (the user's ask: "the dual path spells are now from demon and heaven path"), and those five, built on the ghost path, were
 * made anew:
 * <ul>
 *   <li>Heaven-Demon Palm (Foundation Building): a palm of gold light, a demon's claw in it, strikes the first being ahead, and its
 *       harm mends you.</li>
 *   <li>Heaven-Demon Mantle (Core Formation): gold light and crimson demon scales wrap you, and turn back what is shot at you.</li>
 *   <li>Devouring Seal (Nascent Soul): one foe sealed, its life devoured into yours.</li>
 *   <li>Heaven-Demon Claw (Heavenly Being): a great claw of gold light and crimson qi rakes down where you look.</li>
 *   <li>Heaven and Demon as One (Four Axis): the two made one: all around dragged in and struck, and you made strong.</li>
 *   <li>Heaven-Demon Avatar (Dao Integration): a colossal avatar of the two rises behind you, and twice its hands come down where
 *       you look.</li>
 *   <li>Heaven-Demon Void Shatter (Void Shattering): a colossal fist of the two punches where you look, and space shatters there like
 *       glass.</li>
 * </ul>
 * And the five elements' spells ({@link #isHeaven}; HeavenSpells and the kits of the elements, FireSpells to MetalSpells), cast
 * by the living on qi: each of an element and a realm, at a place among that realm's nine layers, and taught by every heaven
 * method of its element that reaches the realm (a Four Axis fire method teaches every fire spell), once the method is cultivated
 * to the layer and the cultivation stands at the stage it stands for.
 */
public enum Spell {
	/** Cost: lighting the hands; each burning blow then costs {@link Spells#GHOST_FIRE_PER_HIT} more. */
	GHOST_FIRE("ghost_fire", GhostMethod.GHOST_FIRE_SCRIPTURE, 1, 3, 2 * 20),
	GHOST_FIRE_BOLT("ghost_fire_bolt", GhostMethod.GHOST_FIRE_SCRIPTURE, 4, 8, 30),
	SOUL_DASH("soul_dash", GhostMethod.WANDERING_SOUL_STEPS, 2, 6, 3 * 20),
	GHOSTLY_WAIL("ghostly_wail", GhostMethod.NETHERWORLD_BREATHING, 5, 15, 12 * 20),
	YELLOW_SPRINGS_TIDE("yellow_springs_tide", GhostMethod.YELLOW_SPRINGS_MARROW, 3, 12, 8 * 20),
	MARROW_WARD("marrow_ward", GhostMethod.YELLOW_SPRINGS_MARROW, 7, 20, 30 * 20),
	GHOST_CORE_BURST("ghost_core_burst", GhostMethod.GHOST_CORE_CANON, 3, 25, 10 * 20),
	KINGS_DECREE("kings_decree", GhostMethod.GHOST_CORE_CANON, 7, 30, 40 * 20),
	SOUL_REND("soul_rend", GhostMethod.NINE_NETHER_SOUL_SUTRA, 3, 35, 6 * 20),
	NINE_NETHER_STEP("nine_nether_step", GhostMethod.NINE_NETHER_SOUL_SUTRA, 7, 30, 8 * 20),
	YAMA_VERDICT("yama_verdict", GhostMethod.YAMA_JUDGEMENT, 3, 50, 12 * 20),
	HELL_CHAINS("hell_chains", GhostMethod.YAMA_JUDGEMENT, 7, 60, 30 * 20),
	SAMSARA_WHEEL("samsara_wheel", GhostMethod.SIX_PATHS_CANON, 3, 80, 120 * 20),
	SIX_PATHS_ANNIHILATION("six_paths_annihilation", GhostMethod.SIX_PATHS_CANON, 7, 150, 90 * 20),
	// The spells of both paths, the heaven path and the Demon Path (HeavenDemonSpells): realm, qi, cooldown. In the places of the heaven
	// and ghost paths' five (Yin-Yang Palm, Taiji Shield, Life and Death Seal, Heaven and Hell Pillar, Primordial Chaos), their qi and
	// ghostly qi added together.
	HEAVEN_DEMON_PALM("heaven_demon_palm", Realm.FOUNDATION_BUILDING, 35, 6 * 20),
	HEAVEN_DEMON_MANTLE("heaven_demon_mantle", Realm.CORE_FORMATION, 85, 30 * 20),
	DEVOURING_SEAL("devouring_seal", Realm.NASCENT_SOUL, 190, 20 * 20),
	HEAVEN_DEMON_CLAW("heaven_demon_claw", Realm.HEAVENLY_BEING, 410, 25 * 20),
	HEAVEN_AND_DEMON_AS_ONE("heaven_and_demon_as_one", Realm.FOUR_AXIS, 920, 120 * 20),
	// The five elements' spells (HeavenSpells), taught by any heaven method of the element that reaches their realm: element, realm,
	// place among the realm's nine layers (Qi Refining and Foundation Building three spells at 2, 5 and 8, the deeper realms two at 3
	// and 7), qi, cooldown.
	FLAME_BOLT("flame_bolt", QiElement.FIRE, Realm.QI_REFINING, 2, 8, 30),
	FLAME_FIST("flame_fist", QiElement.FIRE, Realm.QI_REFINING, 5, 14, 20 * 20),
	CRIMSON_TEMPERING("crimson_tempering", QiElement.FIRE, Realm.QI_REFINING, 8, 20, 45 * 20),
	WOOD_BULLET("wood_bullet", QiElement.WOOD, Realm.QI_REFINING, 2, 8, 30),
	EMERALD_HEALING_ART("emerald_healing_art", QiElement.WOOD, Realm.QI_REFINING, 5, 15, 30 * 20),
	VERDANT_GROWTH("verdant_growth", QiElement.WOOD, Realm.QI_REFINING, 8, 20, 40 * 20),
	EARTH_SPEAR("earth_spear", QiElement.EARTH, Realm.QI_REFINING, 2, 10, 40),
	HARDENED_BODY("hardened_body", QiElement.EARTH, Realm.QI_REFINING, 5, 12, 30 * 20),
	DIRT_TIDE("dirt_tide", QiElement.EARTH, Realm.QI_REFINING, 8, 18, 25 * 20),
	WATER_ARROW("water_arrow", QiElement.WATER, Realm.QI_REFINING, 2, 8, 30),
	AZURE_COATING_TECHNIQUE("azure_coating_technique", QiElement.WATER, Realm.QI_REFINING, 5, 12, 30 * 20),
	AZURE_BREATHING_TECHNIQUE("azure_breathing_technique", QiElement.WATER, Realm.QI_REFINING, 8, 18, 60 * 20),
	FLYING_SWORD("flying_sword", QiElement.METAL, Realm.QI_REFINING, 2, 10, 40),
	BLADE_POLISH("blade_polish", QiElement.METAL, Realm.QI_REFINING, 5, 12, 30 * 20),
	STEEL_HURRICANE("steel_hurricane", QiElement.METAL, Realm.QI_REFINING, 8, 15, 8 * 20),
	REMOTE_YANG_BOMB("remote_yang_bomb", QiElement.FIRE, Realm.FOUNDATION_BUILDING, 2, 30, 20),
	FLAME_ARMOUR("flame_armour", QiElement.FIRE, Realm.FOUNDATION_BUILDING, 5, 35, 40 * 20),
	FIRE_SERPENT("fire_serpent", QiElement.FIRE, Realm.FOUNDATION_BUILDING, 8, 40, 6 * 20),
	LIFE_FORCE_DRAIN("life_force_drain", QiElement.WOOD, Realm.FOUNDATION_BUILDING, 2, 25, 12 * 20),
	LIFE_FORCE_GAIN("life_force_gain", QiElement.WOOD, Realm.FOUNDATION_BUILDING, 5, 20, 12 * 20),
	MYSTERIOUS_BRISTLE_PRISON("mysterious_bristle_prison", QiElement.WOOD, Realm.FOUNDATION_BUILDING, 8, 40, 15 * 20),
	EARTH_WALL_TECHNIQUE("earth_wall_technique", QiElement.EARTH, Realm.FOUNDATION_BUILDING, 2, 25, 15 * 20),
	BOULDER_THROW("boulder_throw", QiElement.EARTH, Realm.FOUNDATION_BUILDING, 5, 35, 10 * 20),
	CAVE_FORMATION("cave_formation", QiElement.EARTH, Realm.FOUNDATION_BUILDING, 8, 30, 20 * 20),
	RAIN_CALL("rain_call", QiElement.WATER, Realm.FOUNDATION_BUILDING, 2, 25, 30 * 20),
	WATER_PRISON("water_prison", QiElement.WATER, Realm.FOUNDATION_BUILDING, 5, 40, 18 * 20),
	TORRENTIAL_SPRING("torrential_spring", QiElement.WATER, Realm.FOUNDATION_BUILDING, 8, 50, 30 * 20),
	BOULDER_CLEAVE("boulder_cleave", QiElement.METAL, Realm.FOUNDATION_BUILDING, 2, 30, 8 * 20),
	MINERAL_PURIFICATION("mineral_purification", QiElement.METAL, Realm.FOUNDATION_BUILDING, 5, 50, 60 * 20),
	ADVANCED_FLYING_SWORD_TECHNIQUE("advanced_flying_sword_technique", QiElement.METAL, Realm.FOUNDATION_BUILDING, 8, 50, 15 * 20),
	CRIMSON_SWIRL("crimson_swirl", QiElement.FIRE, Realm.CORE_FORMATION, 3, 120, 30 * 20),
	SOLAR_FIELD("solar_field", QiElement.FIRE, Realm.CORE_FORMATION, 7, 150, 60 * 20),
	THOUSAND_LEAF_STORM("thousand_leaf_storm", QiElement.WOOD, Realm.CORE_FORMATION, 3, 80, 10 * 20),
	ANCIENT_BARK_SKIN("ancient_bark_skin", QiElement.WOOD, Realm.CORE_FORMATION, 7, 100, 40 * 20),
	EARTH_DRAGON_SURGE("earth_dragon_surge", QiElement.EARTH, Realm.CORE_FORMATION, 3, 90, 12 * 20),
	MOUNTAIN_STANCE("mountain_stance", QiElement.EARTH, Realm.CORE_FORMATION, 7, 100, 40 * 20),
	TIDAL_LANCE("tidal_lance", QiElement.WATER, Realm.CORE_FORMATION, 3, 90, 12 * 20),
	MIST_VEIL("mist_veil", QiElement.WATER, Realm.CORE_FORMATION, 7, 100, 45 * 20),
	SWORD_RAIN("sword_rain", QiElement.METAL, Realm.CORE_FORMATION, 3, 100, 14 * 20),
	GOLDEN_BELL_SHIELD("golden_bell_shield", QiElement.METAL, Realm.CORE_FORMATION, 7, 110, 40 * 20),
	KARMIC_FIRE("karmic_fire", QiElement.FIRE, Realm.NASCENT_SOUL, 3, 200, 15 * 20),
	BLAZING_METEOR("blazing_meteor", QiElement.FIRE, Realm.NASCENT_SOUL, 7, 300, 30 * 20),
	THORN_SPEAR_FOREST("thorn_spear_forest", QiElement.WOOD, Realm.NASCENT_SOUL, 3, 200, 15 * 20),
	LOTUS_OF_REBIRTH("lotus_of_rebirth", QiElement.WOOD, Realm.NASCENT_SOUL, 7, 300, 60 * 20),
	SUMMON_GOLEM("summon_golem", QiElement.EARTH, Realm.NASCENT_SOUL, 3, 300, 20 * 20),
	EARTHQUAKE("earthquake", QiElement.EARTH, Realm.NASCENT_SOUL, 7, 350, 40 * 20),
	GLACIAL_SPEARS("glacial_spears", QiElement.WATER, Realm.NASCENT_SOUL, 3, 220, 12 * 20),
	GREAT_TIDE("great_tide", QiElement.WATER, Realm.NASCENT_SOUL, 7, 320, 35 * 20),
	PIERCING_SWORD_LIGHT("piercing_sword_light", QiElement.METAL, Realm.NASCENT_SOUL, 3, 220, 12 * 20),
	VAJRA_BODY("vajra_body", QiElement.METAL, Realm.NASCENT_SOUL, 7, 300, 45 * 20),
	PHOENIX_DESCENT("phoenix_descent", QiElement.FIRE, Realm.HEAVENLY_BEING, 3, 450, 20 * 20),
	SEA_OF_FLAMES("sea_of_flames", QiElement.FIRE, Realm.HEAVENLY_BEING, 7, 700, 60 * 20),
	HEAVEN_PIERCING_BAMBOO("heaven_piercing_bamboo", QiElement.WOOD, Realm.HEAVENLY_BEING, 3, 500, 20 * 20),
	SPRING_OF_MYRIAD_LIFE("spring_of_myriad_life", QiElement.WOOD, Realm.HEAVENLY_BEING, 7, 800, 90 * 20),
	FALLING_MOUNTAIN("falling_mountain", QiElement.EARTH, Realm.HEAVENLY_BEING, 3, 600, 40 * 20),
	HEAVY_EARTH_SEAL("heavy_earth_seal", QiElement.EARTH, Realm.HEAVENLY_BEING, 7, 700, 60 * 20),
	HEAVENLY_RIVER_TORRENT("heavenly_river_torrent", QiElement.WATER, Realm.HEAVENLY_BEING, 3, 500, 20 * 20),
	FROZEN_FIELD("frozen_field", QiElement.WATER, Realm.HEAVENLY_BEING, 7, 900, 120 * 20),
	MOUNTAIN_CLEAVE("mountain_cleave", QiElement.METAL, Realm.HEAVENLY_BEING, 3, 700, 45 * 20),
	SWORD_DOMAIN("sword_domain", QiElement.METAL, Realm.HEAVENLY_BEING, 7, 800, 60 * 20),
	NINE_SUNS_DESCEND("nine_suns_descend", QiElement.FIRE, Realm.FOUR_AXIS, 3, 1500, 60 * 20),
	PHOENIX_NIRVANA("phoenix_nirvana", QiElement.FIRE, Realm.FOUR_AXIS, 7, 1200, 300 * 20),
	MYRIAD_VINE_BINDING("myriad_vine_binding", QiElement.WOOD, Realm.FOUR_AXIS, 3, 1200, 40 * 20),
	WORLD_TREE("world_tree", QiElement.WOOD, Realm.FOUR_AXIS, 7, 2000, 180 * 20),
	EARTHEN_TITAN("earthen_titan", QiElement.EARTH, Realm.FOUR_AXIS, 3, 1500, 120 * 20),
	MOUNTAIN_SEAL("mountain_seal", QiElement.EARTH, Realm.FOUR_AXIS, 7, 1800, 90 * 20),
	ABSOLUTE_ZERO("absolute_zero", QiElement.WATER, Realm.FOUR_AXIS, 3, 1200, 30 * 20),
	BOUNDLESS_SEA("boundless_sea", QiElement.WATER, Realm.FOUR_AXIS, 7, 2000, 120 * 20),
	HEAVEN_SEVERING_SWORD("heaven_severing_sword", QiElement.METAL, Realm.FOUR_AXIS, 3, 1500, 60 * 20),
	FOUR_SWORDS_ARRAY("four_swords_array", QiElement.METAL, Realm.FOUR_AXIS, 7, 2000, 180 * 20),
	// Dao Integration (the user's ask, 2026-10-24; their shapes GeckoLib's: SpellConstruct): at the third layer the five elements' guardian
	// beasts, at the seventh the realm's methods' own arts.
	AZURE_DRAGON("azure_dragon", QiElement.WOOD, Realm.DAO_INTEGRATION, 3, 2800, 70 * 20),
	MYRIAD_WOODS_RETURN("myriad_woods_return", QiElement.WOOD, Realm.DAO_INTEGRATION, 7, 3600, 120 * 20),
	VERMILION_BIRD_DESCENT("vermilion_bird_descent", QiElement.FIRE, Realm.DAO_INTEGRATION, 3, 3000, 70 * 20),
	ONE_FLAME_BURNS_THE_HEAVENS("one_flame_burns_the_heavens", QiElement.FIRE, Realm.DAO_INTEGRATION, 7, 3800, 120 * 20),
	QILIN_TRAMPLES_THE_LAND("qilin_tramples_the_land", QiElement.EARTH, Realm.DAO_INTEGRATION, 3, 2800, 70 * 20),
	SEAL_OF_TEN_THOUSAND_MOUNTAINS("seal_of_ten_thousand_mountains", QiElement.EARTH, Realm.DAO_INTEGRATION, 7, 3800, 120 * 20),
	BLACK_TORTOISE_BASTION("black_tortoise_bastion", QiElement.WATER, Realm.DAO_INTEGRATION, 3, 2600, 90 * 20),
	HUNDRED_RIVERS_CONVERGE("hundred_rivers_converge", QiElement.WATER, Realm.DAO_INTEGRATION, 7, 3800, 120 * 20),
	WHITE_TIGER_POUNCE("white_tiger_pounce", QiElement.METAL, Realm.DAO_INTEGRATION, 3, 3000, 70 * 20),
	MYRIAD_SWORDS_RETURN("myriad_swords_return", QiElement.METAL, Realm.DAO_INTEGRATION, 7, 3800, 120 * 20),
	// Dao Integration's ghost foundation's (GhostSpells), at the third and seventh layers of its realm, and its spell of both paths
	// (HeavenDemonSpells): living shapes of GeckoLib's too.
	PROCESSION_OF_TEN_THOUSAND_GHOSTS("procession_of_ten_thousand_ghosts", GhostMethod.TEN_THOUSAND_GHOSTS_UNION, 3, 200, 45 * 20),
	GATE_OF_TEN_THOUSAND_GHOSTS("gate_of_ten_thousand_ghosts", GhostMethod.TEN_THOUSAND_GHOSTS_UNION, 7, 300, 90 * 20),
	HEAVEN_DEMON_AVATAR("heaven_demon_avatar", Realm.DAO_INTEGRATION, 3200, 100 * 20),
	// Void Shattering (the user's ask, 2026-10-24: "void shattering doesn't have to be limited to rifts and special spells but it wouldn't hurt
	// to have some in there"): space torn open, and what comes through it. The five elements' at the third and seventh layers, the ghost
	// foundation's at its own, and the spell of both paths.
	SPRING_THUNDER_OF_THE_AZURE_EMPEROR("spring_thunder_of_the_azure_emperor", QiElement.WOOD, Realm.VOID_SHATTERING, 3, 9000, 70 * 20),
	VOID_WITHERING("void_withering", QiElement.WOOD, Realm.VOID_SHATTERING, 7, 11500, 120 * 20),
	GOLDEN_CROW_DESCENDS("golden_crow_descends", QiElement.FIRE, Realm.VOID_SHATTERING, 3, 9500, 70 * 20),
	SUN_THROUGH_THE_VOID("sun_through_the_void", QiElement.FIRE, Realm.VOID_SHATTERING, 7, 12500, 120 * 20),
	BUZHOU_FALLS("buzhou_falls", QiElement.EARTH, Realm.VOID_SHATTERING, 3, 10000, 80 * 20),
	EARTH_VEINS_LOCK_THE_VOID("earth_veins_lock_the_void", QiElement.EARTH, Realm.VOID_SHATTERING, 7, 11000, 120 * 20),
	WEAK_WATER_FROM_THE_VOID("weak_water_from_the_void", QiElement.WATER, Realm.VOID_SHATTERING, 3, 9500, 70 * 20),
	KUN_RISES_FROM_THE_ABYSS("kun_rises_from_the_abyss", QiElement.WATER, Realm.VOID_SHATTERING, 7, 12500, 120 * 20),
	VOID_SEVERING_SWORD_LIGHT("void_severing_sword_light", QiElement.METAL, Realm.VOID_SHATTERING, 3, 10000, 80 * 20),
	TEN_DIRECTION_SWORD_PRISON("ten_direction_sword_prison", QiElement.METAL, Realm.VOID_SHATTERING, 7, 12000, 120 * 20),
	RIFT_TO_THE_NINE_NETHERS("rift_to_the_nine_nethers", GhostMethod.VOID_RENDING_NETHER_CANON, 3, 280, 60 * 20),
	BRIDGE_OF_NO_RETURN("bridge_of_no_return", GhostMethod.VOID_RENDING_NETHER_CANON, 7, 400, 90 * 20),
	HEAVEN_DEMON_VOID_SHATTER("heaven_demon_void_shatter", Realm.VOID_SHATTERING, 11000, 100 * 20),
	// Sacred Vessel (2026-10-24): the sacred treasures, each called down and wielded. The five elements' at the third and seventh layers, the
	// ghost foundation's at its own, and the spell of both paths.
	HEAVEN_SWALLOWING_GOURD("heaven_swallowing_gourd", QiElement.WOOD, Realm.SACRED_VESSEL, 3, 20000, 70 * 20),
	SEVEN_TREASURE_BODHI_BRANCH("seven_treasure_bodhi_branch", QiElement.WOOD, Realm.SACRED_VESSEL, 7, 26000, 120 * 20),
	WIND_FIRE_WHEELS("wind_fire_wheels", QiElement.FIRE, Realm.SACRED_VESSEL, 3, 21000, 70 * 20),
	EIGHT_TRIGRAMS_FURNACE("eight_trigrams_furnace", QiElement.FIRE, Realm.SACRED_VESSEL, 7, 27000, 120 * 20),
	XIRANG_SELF_GROWING_SOIL("xirang_self_growing_soil", QiElement.EARTH, Realm.SACRED_VESSEL, 3, 20000, 80 * 20),
	NINE_CAULDRONS_SUPPRESS_THE_LAND("nine_cauldrons_suppress_the_land", QiElement.EARTH, Realm.SACRED_VESSEL, 7, 27000, 120 * 20),
	TWENTY_FOUR_SEA_FIXING_PEARLS("twenty_four_sea_fixing_pearls", QiElement.WATER, Realm.SACRED_VESSEL, 3, 21000, 70 * 20),
	OCEAN_SEALING_VASE("ocean_sealing_vase", QiElement.WATER, Realm.SACRED_VESSEL, 7, 26000, 120 * 20),
	IMMORTAL_BINDING_ROPES("immortal_binding_ropes", QiElement.METAL, Realm.SACRED_VESSEL, 3, 20000, 80 * 20),
	GOLDEN_DRAGON_SCISSORS("golden_dragon_scissors", QiElement.METAL, Realm.SACRED_VESSEL, 7, 27000, 120 * 20),
	MIRROR_OF_RETRIBUTION("mirror_of_retribution", GhostMethod.HELL_FORGED_VESSEL_SUTRA, 3, 320, 60 * 20),
	SOUL_LOCKING_PAGODA("soul_locking_pagoda", GhostMethod.HELL_FORGED_VESSEL_SUTRA, 7, 460, 90 * 20),
	HEAVEN_DEMON_BELL("heaven_demon_bell", Realm.SACRED_VESSEL, 24000, 100 * 20),
	// Entering Nirvana (2026-10-24): death and rebirth.
	SEED_OF_WITHERING_REBIRTH("seed_of_withering_rebirth", QiElement.WOOD, Realm.ENTERING_NIRVANA, 3, 44000, 70 * 20),
	WITHERING_AND_FLOURISHING("withering_and_flourishing", QiElement.WOOD, Realm.ENTERING_NIRVANA, 7, 56000, 120 * 20),
	UNDYING_EMBER("undying_ember", QiElement.FIRE, Realm.ENTERING_NIRVANA, 3, 45000, 70 * 20),
	PYRE_OF_THE_HEAVENS("pyre_of_the_heavens", QiElement.FIRE, Realm.ENTERING_NIRVANA, 7, 58000, 120 * 20),
	GRAVESTONE_FALLS("gravestone_falls", QiElement.EARTH, Realm.ENTERING_NIRVANA, 3, 45000, 80 * 20),
	BURIED_WORLD("buried_world", QiElement.EARTH, Realm.ENTERING_NIRVANA, 7, 58000, 120 * 20),
	FROZEN_MOON_IN_THE_WELL("frozen_moon_in_the_well", QiElement.WATER, Realm.ENTERING_NIRVANA, 3, 44000, 70 * 20),
	THE_FROZEN_MOON_FALLS("the_frozen_moon_falls", QiElement.WATER, Realm.ENTERING_NIRVANA, 7, 57000, 120 * 20),
	SWORD_TOMB_AWAKENS("sword_tomb_awakens", QiElement.METAL, Realm.ENTERING_NIRVANA, 3, 45000, 80 * 20),
	NINE_REBIRTHS_OF_THE_SWORD("nine_rebirths_of_the_sword", QiElement.METAL, Realm.ENTERING_NIRVANA, 7, 58000, 120 * 20),
	BLACK_AND_WHITE_IMPERMANENCE("black_and_white_impermanence", GhostMethod.UNDERWORLD_NIRVANA_SCRIPTURE, 3, 360, 60 * 20),
	BOOK_OF_LIFE_AND_DEATH("book_of_life_and_death", GhostMethod.UNDERWORLD_NIRVANA_SCRIPTURE, 7, 520, 90 * 20),
	HEAVEN_DEMON_ECLIPSE("heaven_demon_eclipse", Realm.ENTERING_NIRVANA, 52000, 100 * 20),
	// True Immortal (2026-10-24): the elements as they were at the beginning, before heaven and earth were parted.
	PRIMAL_WIND_OF_CREATION("primal_wind_of_creation", QiElement.WOOD, Realm.TRUE_IMMORTAL, 3, 380000, 70 * 20),
	FIRST_SEED_OF_CREATION("first_seed_of_creation", QiElement.WOOD, Realm.TRUE_IMMORTAL, 7, 480000, 120 * 20),
	SAMADHI_TRUE_FIRE("samadhi_true_fire", QiElement.FIRE, Realm.TRUE_IMMORTAL, 3, 390000, 70 * 20),
	FIRST_FLAME_BEFORE_HEAVEN("first_flame_before_heaven", QiElement.FIRE, Realm.TRUE_IMMORTAL, 7, 500000, 120 * 20),
	HAND_OF_HOUTU("hand_of_houtu", QiElement.EARTH, Realm.TRUE_IMMORTAL, 3, 390000, 80 * 20),
	FALLING_CONTINENT("falling_continent", QiElement.EARTH, Realm.TRUE_IMMORTAL, 7, 500000, 120 * 20),
	GLACIER_OF_THE_FIRST_AGE("glacier_of_the_first_age", QiElement.WATER, Realm.TRUE_IMMORTAL, 3, 380000, 80 * 20),
	THE_PRIMORDIAL_SEA("the_primordial_sea", QiElement.WATER, Realm.TRUE_IMMORTAL, 7, 490000, 120 * 20),
	HEAVEN_SLAYING_SWORD_RISES("heaven_slaying_sword_rises", QiElement.METAL, Realm.TRUE_IMMORTAL, 3, 390000, 80 * 20),
	PRIMAL_AXE_SPLITS_CHAOS("primal_axe_splits_chaos", QiElement.METAL, Realm.TRUE_IMMORTAL, 7, 500000, 120 * 20),
	FENGDU_WALLS_RISE("fengdu_walls_rise", GhostMethod.NETHERWORLD_IMMORTAL_CANON, 3, 400, 60 * 20),
	TEN_THOUSAND_SOULS_ASCEND("ten_thousand_souls_ascend", GhostMethod.NETHERWORLD_IMMORTAL_CANON, 7, 600, 90 * 20),
	PRIMORDIAL_CHAOS("primordial_chaos", Realm.TRUE_IMMORTAL, 450000, 100 * 20),
	// Return to the Bough (WoodSpells#Bough; the user's ask, 2026-10-25: "a mid to late realm wood spell that lets u put cultivation fruits back
	// onto trees"): Four Axis, between its two spells of war.
	RETURN_TO_THE_BOUGH("return_to_the_bough", QiElement.WOOD, Realm.FOUR_AXIS, 5, 400, 10 * 20);

	private final String id;
	private final GhostMethod method;
	/** An element's spell (HeavenSpells): its element and realm, or null for a ghost method's or both paths'. */
	private final QiElement element;
	private final Realm heavenRealm;
	/**
	 * The method's layer it comes with, counted from its first (a foundation's spells come in its own realm: GhostMethod#ownLayer;
	 * an element's spell, the layer of its realm's nine at its place: MethodLayers).
	 */
	private final int layer;
	/** A spell of both paths: the realm both must stand at (null for a method's spell). */
	private final Realm dualRealm;
	private final double cost;
	private final double qiCost;
	private final int cooldownTicks;

	Spell(String id, GhostMethod method, int layer, double cost, int cooldownTicks) {
		this(id, method, layer, null, cost, 0, cooldownTicks);
	}

	/** A spell of both paths: of {@code dualRealm}, costing qi alone. */
	Spell(String id, Realm dualRealm, double qiCost, int cooldownTicks) {
		this(id, null, 0, dualRealm, 0, qiCost, cooldownTicks);
	}

	/** An element's spell: of {@code realm}, at {@code place} among its nine layers (1 to 9), costing qi alone. */
	Spell(String id, QiElement element, Realm realm, int place, double qiCost, int cooldownTicks) {
		this.id = id;
		this.method = null;
		this.element = element;
		this.heavenRealm = realm;
		this.layer = realm.ordinal() * MethodLayers.PER_REALM + place;
		this.dualRealm = null;
		this.cost = 0;
		this.qiCost = qiCost;
		this.cooldownTicks = cooldownTicks;
	}

	/** {@code layer}: of the method's own realm (1 to 9; the fire's and the steps' are simply theirs). */
	Spell(String id, GhostMethod method, int layer, Realm dualRealm, double cost, double qiCost, int cooldownTicks) {
		this.id = id;
		this.method = method;
		this.element = null;
		this.heavenRealm = null;
		this.layer = method == null ? 0 : method.ownLayer(layer);
		this.dualRealm = dualRealm;
		this.cost = cost;
		this.qiCost = qiCost;
		this.cooldownTicks = cooldownTicks;
	}

	public String getId() { return id; }
	/** The ghost method that teaches it; null for a heaven method's spell or one of both paths. */
	public GhostMethod method() { return method; }
	/** An element's spell: the heaven method of its element and realm (any of the element reaching as deep teaches it too); null for the others. */
	public HeavenMethod heavenMethod() { return element == null ? null : HeavenMethod.of(element, heavenRealm); }
	/** An element's spell: its element; null for the others. */
	public QiElement element() { return element; }
	/** An element's spell: its realm; null for the others. */
	public Realm heavenRealm() { return heavenRealm; }
	/** A heaven method's spell (an element's): cast by the living alone, on qi alone. */
	public boolean isHeaven() { return element != null; }
	public int layer() { return layer; }

	/** An element's spell: whether {@code method} teaches it (of its element, and reaching its realm). */
	public boolean taughtBy(HeavenMethod method) {
		return element != null && method.element() == element && method.realm().ordinal() >= heavenRealm.ordinal();
	}

	/** An element's spell: whether {@code s} knows a method that teaches it (cultivated that far or not). */
	public boolean taughtBy(HeavenState s) {
		if (element == null) return false;
		for (HeavenMethod known : s.known()) if (taughtBy(known)) return true;
		return false;
	}

	/**
	 * An element's spell: the method of {@code s}'s that teaches it, cultivated deepest (null with none), else (none known) its own
	 * realm's method; null for the others.
	 */
	public HeavenMethod teacher(HeavenState s) {
		if (element == null) return null;
		HeavenMethod best = null;
		for (HeavenMethod known : s.known()) {
			if (taughtBy(known) && (best == null || s.layer(known) > s.layer(best))) best = known;
		}
		return best != null ? best : heavenMethod();
	}
	/** A spell of both paths (TwoPaths): of no method, in reach once the heaven path and the Demon Path stand at {@link #dualRealm}. */
	public boolean isDual() { return dualRealm != null; }
	public Realm dualRealm() { return dualRealm; }

	/**
	 * The realm whose greatness it has (HeavenSpells#span, SpellConstruct#sizedFor): an element's spell's, a ghost foundation's, both
	 * paths'; null for the dead's arts (the fire, the steps), which have none.
	 */
	public Realm realm() {
		if (heavenRealm != null) return heavenRealm;
		if (dualRealm != null) return dualRealm;
		return method == null ? null : method.realm();
	}

	/** Ghostly qi it costs (a ghost method's spell). */
	public double cost() { return cost; }
	/** Qi it costs (the heaven methods' spells and those of both paths). */
	public double qiCost() { return qiCost; }
	public int cooldownTicks() { return cooldownTicks; }

	/**
	 * Whether this cultivator can cast it: the method cultivated to the spell's layer, a ghost method's by a ghost or a living
	 * cultivator, a heaven method's by the living alone (never a mortal, alive or dead: its meridians are closed, its methods
	 * dormant) and once their cultivation has reached the stage the layer stands for (the method may run ahead of the realm, its
	 * spells not: {@link #realmNeeded}); a spell of both paths, by the living, the heaven path and the Demon Path both at its realm (the
	 * dead's paths are the dead's, as a bloodline's inheritance is the living's: DemonPath#inherits).
	 */
	public boolean isKnown(PlayerCultivation c) {
		if (isDual()) return !c.netherworld().ghost() && TwoPaths.bothReach(c, dualRealm);
		if (isHeaven()) {
			// A demon's bloodline teaches its race's element's spells as its Demon Path climbs, no method wanted (DemonPath#inherits).
			if (DemonPath.inherits(c, this)) return true;
			if (c.isMortal() || c.netherworld().ghost() || PlayerCultivation.rank(c.getRealm(), c.getStage()) < realmNeeded()) return false;
			for (HeavenMethod known : c.heaven().known()) if (taughtBy(known) && c.heaven().layer(known) >= layer) return true;
			return byManual(c);
		}
		return !c.isMortal() && (c.netherworld().layer(method) >= layer || byManual(c));
	}

	/**
	 * Whether {@code c} wields this spell as learned from a manual (TechniqueManualItem), apart from any method: only at its realm (the
	 * user's rule, 2026-10-23: "only those of the required realm can learn it and use it"): an element's by the heaven path's rank
	 * (and roots that take its element), a ghost method's by the ghost path's. A spell of both paths is no manual's.
	 */
	public boolean byManual(PlayerCultivation c) {
		return c.knowsByManual(this) && manualReach(c);
	}

	/** Whether {@code c} stands where this spell's manual asks ({@link #byManual}), known or not. */
	public boolean manualReach(PlayerCultivation c) {
		if (isDual() || c.isMortal()) return false;
		if (isHeaven()) return !c.netherworld().ghost() && c.rootsAllow(element) && PlayerCultivation.rank(c.getRealm(), c.getStage()) >= realmNeeded();
		return GhostMethods.ghostRank(c.netherworld()) >= realmNeeded();
	}

	/**
	 * The realm and stage (a rank, PlayerCultivation#rank) one's cultivation must reach for it, its layer's: the heaven path's for an
	 * element's spell, the ghost path's for a ghost method's (their layers are counted alike: MethodLayers).
	 */
	public int realmNeeded() {
		return MethodLayers.floor(layer);
	}

	public Component getDisplayName() { return Component.translatable(ModLang.spellKey(id)); }

	public Component getDescription() { return Component.translatable(ModLang.spellKey(id) + ".description"); }

	public static Spell byId(String id) {
		for (Spell spell : values()) {
			if (spell.id.equals(id)) return spell;
		}
		return null;
	}

	public static Spell byIndex(int index) {
		return index >= 0 && index < values().length ? values()[index] : null;
	}
}
