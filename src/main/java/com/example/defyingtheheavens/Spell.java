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
 * </ul>
 * And the spells of both paths ({@link #isDual}, see TwoPaths), of no method: one for each realm from Foundation Building, in
 * reach once the heaven path and the ghost path both stand there, costing qi as well as ghostly qi:
 * <ul>
 *   <li>Yin-Yang Palm (Foundation Building): a palm of light and shadow strikes the first being ahead, and its harm mends you.</li>
 *   <li>Taiji Shield (Core Formation): the two turning round you turn back what is shot at you.</li>
 *   <li>Life and Death Seal (Nascent Soul): one foe sealed, its life drawn off into yours.</li>
 *   <li>Heaven and Hell Pillar (Heavenly Being): heaven's light falls and hell's fire rises where you look.</li>
 *   <li>Primordial Chaos (Four Axis): the two made one again: all around dragged in and unmade, and you made strong.</li>
 * </ul>
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
	// The spells of both paths: realm, ghostly qi, qi, cooldown.
	YIN_YANG_PALM("yin_yang_palm", Realm.FOUNDATION_BUILDING, 15, 20, 6 * 20),
	TAIJI_SHIELD("taiji_shield", Realm.CORE_FORMATION, 25, 60, 30 * 20),
	LIFE_DEATH_SEAL("life_death_seal", Realm.NASCENT_SOUL, 40, 150, 20 * 20),
	HEAVEN_HELL_PILLAR("heaven_hell_pillar", Realm.HEAVENLY_BEING, 60, 350, 25 * 20),
	PRIMORDIAL_CHAOS("primordial_chaos", Realm.FOUR_AXIS, 120, 800, 120 * 20),
	// The heaven methods' spells (HeavenSpells): method, layer of its own realm, qi, cooldown.
	GRASPING_ROOTS("grasping_roots", HeavenMethod.WOOD_QI_REFINING, 3, 10, 8 * 20),
	BRAMBLE_MANTLE("bramble_mantle", HeavenMethod.WOOD_QI_REFINING, 7, 18, 30 * 20),
	WISTERIA_ASCENT("wisteria_ascent", HeavenMethod.WOOD_FOUNDATION_BUILDING, 3, 24, 6 * 20),
	TWIN_SERPENT_VINES("twin_serpent_vines", HeavenMethod.WOOD_FOUNDATION_BUILDING, 7, 50, 25 * 20),
	BURSTING_THORN_PODS("bursting_thorn_pods", HeavenMethod.WOOD_CORE_FORMATION, 3, 60, 10 * 20),
	HEARTWOOD_OF_NINE_RINGS("heartwood_of_nine_rings", HeavenMethod.WOOD_CORE_FORMATION, 7, 120, 40 * 20),
	POLLEN_OF_LONG_DREAMS("pollen_of_long_dreams", HeavenMethod.WOOD_NASCENT_SOUL, 3, 150, 12 * 20),
	MYRIAD_THORN_THICKET("myriad_thorn_thicket", HeavenMethod.WOOD_NASCENT_SOUL, 7, 320, 40 * 20),
	STRANGLER_FIG_SEED("strangler_fig_seed", HeavenMethod.WOOD_HEAVENLY_BEING, 3, 340, 10 * 20),
	RITE_OF_RETURNING_SPRING("rite_of_returning_spring", HeavenMethod.WOOD_HEAVENLY_BEING, 7, 700, 60 * 20),
	HEAVEN_SHADING_CANOPY("heaven_shading_canopy", HeavenMethod.WOOD_FOUR_AXIS, 3, 800, 12 * 20),
	WORLD_TREE_RISES("world_tree_rises", HeavenMethod.WOOD_FOUR_AXIS, 7, 1800, 180 * 20),
	HOARFROST_NEEDLE("hoarfrost_needle", HeavenMethod.WATER_QI_REFINING, 3, 10, 4 * 20),
	MORNING_MIST_SHROUD("morning_mist_shroud", HeavenMethod.WATER_QI_REFINING, 7, 18, 30 * 20),
	RETURNING_CURRENT("returning_current", HeavenMethod.WATER_FOUNDATION_BUILDING, 3, 24, 12 * 20),
	RENEWING_MOUNTAIN_RAIN("renewing_mountain_rain", HeavenMethod.WATER_FOUNDATION_BUILDING, 7, 50, 30 * 20),
	COILING_STREAM_LASH("coiling_stream_lash", HeavenMethod.WATER_CORE_FORMATION, 3, 60, 8 * 20),
	NINE_BEND_WHIRLPOOL("nine_bend_whirlpool", HeavenMethod.WATER_CORE_FORMATION, 7, 120, 30 * 20),
	MOON_IN_THE_WATER("moon_in_the_water", HeavenMethod.WATER_NASCENT_SOUL, 3, 150, 10 * 20),
	STILL_LAKE_MIRROR("still_lake_mirror", HeavenMethod.WATER_NASCENT_SOUL, 7, 300, 40 * 20),
	ABYSSAL_WATER_PRISON("abyssal_water_prison", HeavenMethod.WATER_HEAVENLY_BEING, 3, 340, 10 * 20),
	OVERTURNED_HEAVENLY_RIVER("overturned_heavenly_river", HeavenMethod.WATER_HEAVENLY_BEING, 7, 720, 60 * 20),
	TEN_THOUSAND_REFLECTIONS("ten_thousand_reflections", HeavenMethod.WATER_FOUR_AXIS, 3, 800, 12 * 20),
	ETERNAL_WINTER_DESCENDS("eternal_winter_descends", HeavenMethod.WATER_FOUR_AXIS, 7, 1600, 150 * 20),
	SLEEPING_EMBER("sleeping_ember", HeavenMethod.FIRE_QI_REFINING, 3, 10, 6 * 20),
	STRUCK_FLINT_FLASH("struck_flint_flash", HeavenMethod.FIRE_QI_REFINING, 7, 20, 25 * 20),
	FIRE_HORSE_GALLOP("fire_horse_gallop", HeavenMethod.FIRE_FOUNDATION_BUILDING, 3, 25, 12 * 20),
	SEALED_FURNACE_REFINEMENT("sealed_furnace_refinement", HeavenMethod.FIRE_FOUNDATION_BUILDING, 7, 50, 40 * 20),
	PRAIRIE_BURNING_SPARK("prairie_burning_spark", HeavenMethod.FIRE_CORE_FORMATION, 3, 60, 10 * 20),
	CRIMSON_LOTUS_BLOOMS("crimson_lotus_blooms", HeavenMethod.FIRE_CORE_FORMATION, 7, 120, 35 * 20),
	ASH_SEARING_TALON("ash_searing_talon", HeavenMethod.FIRE_NASCENT_SOUL, 3, 150, 12 * 20),
	VERMILION_BIRD_PLUNGE("vermilion_bird_plunge", HeavenMethod.FIRE_NASCENT_SOUL, 7, 300, 30 * 20),
	LIGHT_OF_FIRST_DAWN("light_of_first_dawn", HeavenMethod.FIRE_HEAVENLY_BEING, 3, 320, 12 * 20),
	NOONDAY_GOLDEN_CROW("noonday_golden_crow", HeavenMethod.FIRE_HEAVENLY_BEING, 7, 700, 60 * 20),
	HEAVEN_SCORCHING_FIRESTORMS("heaven_scorching_firestorms", HeavenMethod.FIRE_FOUR_AXIS, 3, 800, 12 * 20),
	UNDYING_PHOENIX("undying_phoenix", HeavenMethod.FIRE_FOUR_AXIS, 7, 1600, 180 * 20),
	ROLLING_MILLSTONE("rolling_millstone", HeavenMethod.EARTH_QI_REFINING, 3, 10, 7 * 20),
	RAMMED_EARTH_RAMPART("rammed_earth_rampart", HeavenMethod.EARTH_QI_REFINING, 7, 20, 30 * 20),
	STONE_SCALE_ARMOUR("stone_scale_armour", HeavenMethod.EARTH_FOUNDATION_BUILDING, 3, 25, 12 * 20),
	SWALLOWING_SANDS("swallowing_sands", HeavenMethod.EARTH_FOUNDATION_BUILDING, 7, 50, 35 * 20),
	EARTH_SPLITTING_FISSURE("earth_splitting_fissure", HeavenMethod.EARTH_CORE_FORMATION, 3, 60, 10 * 20),
	MOUNT_TAI_PRESSES_DOWN("mount_tai_presses_down", HeavenMethod.EARTH_CORE_FORMATION, 7, 120, 30 * 20),
	EARTH_DRAGON_TURNS_OVER("earth_dragon_turns_over", HeavenMethod.EARTH_NASCENT_SOUL, 3, 150, 12 * 20),
	EARTH_VEIN_GUARDIAN("earth_vein_guardian", HeavenMethod.EARTH_NASCENT_SOUL, 7, 300, 45 * 20),
	BEDROCK_BEARS_THE_BLOWS("bedrock_bears_the_blows", HeavenMethod.EARTH_HEAVENLY_BEING, 3, 320, 12 * 20),
	SHOULDER_THE_MYRIAD_BURDENS("shoulder_the_myriad_burdens", HeavenMethod.EARTH_HEAVENLY_BEING, 7, 700, 60 * 20),
	RIDING_TEN_THOUSAND_DUNES("riding_ten_thousand_dunes", HeavenMethod.EARTH_FOUR_AXIS, 3, 800, 12 * 20),
	FALL_OF_BUZHOU("fall_of_buzhou", HeavenMethod.EARTH_FOUR_AXIS, 7, 1800, 150 * 20),
	SWALLOW_RETURN_THREAD("swallow_return_thread", HeavenMethod.METAL_QI_REFINING, 3, 10, 6 * 20),
	IRON_BELL_COVER("iron_bell_cover", HeavenMethod.METAL_QI_REFINING, 7, 20, 30 * 20),
	LODESTONE_BRAND("lodestone_brand", HeavenMethod.METAL_FOUNDATION_BUILDING, 3, 25, 10 * 20),
	HUNDRED_WEAPONS_KNEEL("hundred_weapons_kneel", HeavenMethod.METAL_FOUNDATION_BUILDING, 7, 50, 35 * 20),
	ARMOUR_SPLITTING_FINGER("armour_splitting_finger", HeavenMethod.METAL_CORE_FORMATION, 3, 60, 10 * 20),
	LEAPING_GOLDEN_LIGHTNING("leaping_golden_lightning", HeavenMethod.METAL_CORE_FORMATION, 7, 120, 25 * 20),
	NINE_CIRCLING_BLADES("nine_circling_blades", HeavenMethod.METAL_NASCENT_SOUL, 3, 150, 12 * 20),
	THOUSAND_ECHO_TOLL("thousand_echo_toll", HeavenMethod.METAL_NASCENT_SOUL, 7, 300, 40 * 20),
	FATE_SEVERING_EDGE("fate_severing_edge", HeavenMethod.METAL_HEAVENLY_BEING, 3, 350, 12 * 20),
	TEN_THOUSAND_SWORDS_DESCEND("ten_thousand_swords_descend", HeavenMethod.METAL_HEAVENLY_BEING, 7, 750, 90 * 20),
	INDESTRUCTIBLE_GOLDEN_BODY("indestructible_golden_body", HeavenMethod.METAL_FOUR_AXIS, 3, 800, 12 * 20),
	FOUR_GATES_IMMORTAL_SLAYING_ARRAY("four_gates_immortal_slaying_array", HeavenMethod.METAL_FOUR_AXIS, 7, 1800, 180 * 20);

	private final String id;
	private final GhostMethod method;
	/** The heaven method that teaches it (HeavenMethods), or null for a ghost method's or both paths'. */
	private final HeavenMethod heavenMethod;
	/** The method's layer it comes with, counted from its first (a foundation's spells come in its own realm: GhostMethod#ownLayer). */
	private final int layer;
	/** A spell of both paths: the realm both must stand at (null for a method's spell). */
	private final Realm dualRealm;
	private final double cost;
	private final double qiCost;
	private final int cooldownTicks;

	Spell(String id, GhostMethod method, int layer, double cost, int cooldownTicks) {
		this(id, method, layer, null, cost, 0, cooldownTicks);
	}

	Spell(String id, Realm dualRealm, double cost, double qiCost, int cooldownTicks) {
		this(id, null, 0, dualRealm, cost, qiCost, cooldownTicks);
	}

	/** A heaven method's spell: {@code layer} of the method's own realm (3 or 7), costing qi alone. */
	Spell(String id, HeavenMethod heavenMethod, int layer, double qiCost, int cooldownTicks) {
		this.id = id;
		this.method = null;
		this.heavenMethod = heavenMethod;
		this.layer = heavenMethod.ownLayer(layer);
		this.dualRealm = null;
		this.cost = 0;
		this.qiCost = qiCost;
		this.cooldownTicks = cooldownTicks;
	}

	/** {@code layer}: of the method's own realm (1 to 9; the fire's and the steps' are simply theirs). */
	Spell(String id, GhostMethod method, int layer, Realm dualRealm, double cost, double qiCost, int cooldownTicks) {
		this.id = id;
		this.method = method;
		this.heavenMethod = null;
		this.layer = method == null ? 0 : method.ownLayer(layer);
		this.dualRealm = dualRealm;
		this.cost = cost;
		this.qiCost = qiCost;
		this.cooldownTicks = cooldownTicks;
	}

	public String getId() { return id; }
	/** The ghost method that teaches it; null for a heaven method's spell or one of both paths. */
	public GhostMethod method() { return method; }
	/** The heaven method that teaches it; null for the others. */
	public HeavenMethod heavenMethod() { return heavenMethod; }
	/** A heaven method's spell: cast by the living alone, on qi alone. */
	public boolean isHeaven() { return heavenMethod != null; }
	public int layer() { return layer; }
	/** A spell of both paths (TwoPaths): of no method, in reach once both paths stand at {@link #dualRealm}. */
	public boolean isDual() { return dualRealm != null; }
	public Realm dualRealm() { return dualRealm; }
	/** Ghostly qi it costs. */
	public double cost() { return cost; }
	/** Qi it costs (the heaven methods' spells, and those of both paths as well as ghostly qi). */
	public double qiCost() { return qiCost; }
	public int cooldownTicks() { return cooldownTicks; }

	/**
	 * Whether this cultivator can cast it: the method cultivated to the spell's layer, a ghost method's by a ghost or a living
	 * cultivator, a heaven method's by the living alone (never a mortal, alive or dead: its meridians are closed, its methods
	 * dormant) and once their cultivation has reached the stage the layer stands for (the method may run ahead of the realm, its
	 * spells not: {@link #realmNeeded}); a spell of both paths, both paths at its realm.
	 */
	public boolean isKnown(PlayerCultivation c) {
		if (isDual()) return TwoPaths.bothReach(c, dualRealm);
		if (isHeaven()) return !c.isMortal() && !c.netherworld().ghost() && c.heaven().layer(heavenMethod) >= layer
				&& PlayerCultivation.rank(c.getRealm(), c.getStage()) >= realmNeeded();
		return !c.isMortal() && c.netherworld().layer(method) >= layer;
	}

	/** A heaven method's spell: the realm and stage (a rank, PlayerCultivation#rank) one's cultivation must reach for it, its layer's. */
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
