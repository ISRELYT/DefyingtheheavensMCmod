package com.example.defyingtheheavens;

import com.example.defyingtheheavens.DefyingTheHeavens;

/** Single source of truth for translation keys; the datagen lang provider fills in the English text. */
public final class ModLang {
	private static String k(String s) {
		return "cultivation." + DefyingTheHeavens.MOD_ID + "." + s;
	}

	public static String realmKey(String id) { return "realm." + DefyingTheHeavens.MOD_ID + "." + id; }
	public static String stageKey(String id) { return "stage." + DefyingTheHeavens.MOD_ID + "." + id; }
	public static String mortalStageKey(String id) { return k("mortal_stage." + id); }

	public static final String KEY_CATEGORY = "key.categories." + DefyingTheHeavens.MOD_ID;
	public static final String KEY_MENU = "key." + DefyingTheHeavens.MOD_ID + ".cultivation_menu";
	public static final String KEY_MEDITATE = "key." + DefyingTheHeavens.MOD_ID + ".meditate";
	public static final String KEY_CIRCULATE = "key." + DefyingTheHeavens.MOD_ID + ".circulate";

	public static final String TITLE = k("title");
	public static final String REALM = k("realm");
	public static final String STAGE = k("stage");
	public static final String CULTIVATION = k("cultivation");
	public static final String CULTIVATION_MAX = k("cultivation_max");
	public static final String CULTIVATION_RATE = k("cultivation_rate");
	public static final String CULTIVATION_RATE_UPPER = k("cultivation_rate_upper");
	public static final String HUD_QI = k("hud.qi");
	public static final String HUD_QI_LABEL = k("hud.qi_label");
	public static final String BOTTLENECK = k("bottleneck");
	public static final String BOTTLENECK_NEXT = k("bottleneck_next");
	public static final String PINNACLE = k("pinnacle");
	// Tabbed menu (tabs left to right).
	public static final String TAB_CULTIVATION = k("tab.cultivation");
	public static final String TAB_STATS = k("tab.stats");
	public static final String TAB_ABILITIES = k("tab.abilities");
	public static final String TAB_METHODS = k("tab.methods");
	public static final String TAB_SPELLS = k("tab.spells");
	public static final String STATS_SUPPRESSED = k("stats.suppressed");
	public static final String STATS_COL_STAT = k("stats.column.stat");
	public static final String STATS_COL_BASE = k("stats.column.base");
	public static final String STATS_COL_BONUS = k("stats.column.bonus");
	public static final String STATS_COL_CURRENT = k("stats.column.current");
	public static final String STATS_COL_TRUE = k("stats.column.true");
	public static final String STATS_COL_NOW = k("stats.column.now");
	public static final String STAT_NAME_HEALTH = k("stats.max_health");
	public static final String STAT_NAME_DAMAGE = k("stats.attack_damage");
	public static final String STAT_NAME_SPEED = k("stats.move_speed");
	public static final String STAT_NAME_ARMOR = k("stats.armor");
	public static final String STAT_NAME_TOUGHNESS = k("stats.toughness");
	public static final String STAT_NAME_KNOCKBACK = k("stats.knockback_resistance");
	public static final String STAT_NAME_MAX_QI = k("stats.max_qi");
	public static final String STAT_NAME_QI_GATHER = k("stats.qi_gather");
	public static final String STATS_QI_GATHER_UPPER = k("stats.qi_gather_upper");
	public static final String ABILITIES_TITLE = k("abilities.title");
	public static final String ABILITIES_EMPTY = k("abilities.empty");
	public static final String ABILITIES_HINT = k("abilities.hint");
	public static final String ABILITY_ON = k("abilities.on");
	public static final String ABILITY_OFF = k("abilities.off");
	public static final String METHODS_TITLE = k("methods.title");
	public static final String METHODS_EMPTY = k("methods.empty");
	public static final String METHODS_HINT = k("methods.hint");
	public static final String SPELLS_TITLE = k("spells.title");
	public static final String SPELLS_EMPTY = k("spells.empty");
	public static final String SPELLS_HINT = k("spells.hint");
	// Stat lines of the previous single-page menu, kept so backups/cultivation-menu-2026-10-09 can be restored as is.
	public static final String STATS_HEADER = k("stats_header");
	public static final String STAT_HEALTH = k("stat.max_health");
	public static final String STAT_DAMAGE = k("stat.attack_damage");
	public static final String STAT_SPEED = k("stat.move_speed");
	public static final String STAT_ARMOR = k("stat.armor");
	public static final String STAT_TOUGHNESS = k("stat.toughness");
	public static final String STAT_KNOCKBACK = k("stat.knockback_resistance");
	public static final String STATUS_MEDITATING = k("status.meditating");
	public static final String STATUS_IDLE = k("status.idle");
	public static final String BTN_MEDITATE = k("button.meditate");
	public static final String BTN_STOP = k("button.stop_meditating");
	public static final String BTN_BREAKTHROUGH = k("button.breakthrough");

	public static final String MSG_START = k("message.start");
	public static final String MSG_STOP = k("message.stop");
	public static final String MSG_INTERRUPTED = k("message.interrupted");
	public static final String MSG_CANNOT = k("message.cannot_meditate");
	public static final String MSG_STAGE_UP = k("message.stage_up");
	public static final String MSG_BREAKTHROUGH = k("message.breakthrough");
	public static final String MSG_NOT_READY = k("message.not_ready");
	public static final String MSG_BOTTLENECK = k("message.bottleneck");
	public static final String MSG_QI_FLIGHT_GAINED = k("message.qi_flight.gained");
	public static final String MSG_QI_FLIGHT_EXHAUSTED = k("message.qi_flight.exhausted");
	public static final String MSG_QI_SENSE_GAINED = k("message.qi_sense.gained");
	public static final String MSG_SUPPRESS_EXHAUSTED = k("message.realm_suppress.exhausted");
	// Consciousness Domain and Realm Suppress (another cultivator's pressure on you)
	public static final String CONSCIOUSNESS_ALERT_TITLE = k("consciousness.alert.title");
	public static final String CONSCIOUSNESS_ALERT_DETAIL = k("consciousness.alert.detail");
	public static final String CONSCIOUSNESS_UNKNOWN = k("consciousness.unknown");
	public static final String PRESSURE = k("pressure");
	public static final String PRESSURE_DETAIL = k("pressure.detail");
	public static final String STATS_PRESSED = k("stats.pressed");
	public static final String HUD_PRESSURE = k("hud.pressure");
	public static final String MSG_TRIB_START = k("message.tribulation.start");
	public static final String MSG_TRIB_START_SINGLE = k("message.tribulation.start_single");
	public static final String MSG_TRIB_BUSY = k("message.tribulation.busy");
	public static final String MSG_TRIB_FAILED = k("message.tribulation.failed");
	public static final String MSG_TRIB_FALL = k("message.tribulation.fall");
	public static final String MSG_TRIB_ABANDONED = k("message.tribulation.abandoned");
	public static final String TRIB_HUD = k("hud.tribulation");
	public static final String MSG_GAP_ASCEND = k("message.spatial_gap.ascend");
	public static final String MSG_GAP_DESCEND = k("message.spatial_gap.descend");
	public static final String MSG_GAP_RESTART = k("message.spatial_gap.restart");
	public static final String MSG_GAP_TIMER = k("message.spatial_gap.timer");
	public static final String MSG_ARRIVE_UPPER = k("message.spatial_gap.arrive_upper");
	public static final String MSG_ARRIVE_LOWER = k("message.spatial_gap.arrive_lower");
	public static final String MSG_SUPPRESSED = k("message.suppression.suppressed");
	public static final String MSG_RESTORED = k("message.suppression.restored");
	public static final String MSG_REALM_LOCKED = k("message.suppression.locked");
	public static final String MSG_PORTAL_BLOCKED = k("message.portal_blocked");
	public static final String SUPPRESSED = k("suppressed");
	public static final String SUPPRESSED_TO = k("suppressed_to");
	public static final String BREAKTHROUGH_SEALED = k("breakthrough_sealed");
	public static final String DEATH_SPATIAL_PRESSURE = "death.attack." + DefyingTheHeavens.MOD_ID + ".spatial_pressure";

	public static String biomeKey(String id) { return "biome." + DefyingTheHeavens.MOD_ID + "." + id; }
	/** An ability's name; its description is this plus ".description". */
	public static String abilityKey(String id) { return "ability." + DefyingTheHeavens.MOD_ID + "." + id; }

	public static final String RING_OF_POWER_TOOLTIP = k("tooltip.ring_of_power");
	public static final String RING_OF_TRANSCENDENCE_TOOLTIP = k("tooltip.ring_of_transcendence");

	// Spirit Pedestals and what speeds meditation (see CultivationBoost).
	public static final String PEDESTAL_TOOLTIP = k("tooltip.spirit_pedestal");
	public static final String FRUIT_PEDESTAL_TOOLTIP = k("tooltip.fruit_on_pedestal");
	public static final String MSG_BOOST = k("message.boost");
	public static final String MSG_BOOST_NONE = k("message.boost_none");
	public static final String BOOST_PEDESTAL = k("boost.pedestal");
	public static final String BOOST_PEDESTALS = k("boost.pedestals");
	public static final String BOOST_HEIGHT = k("boost.height");
	public static final String BOOST_TRANQUIL = k("boost.tranquil");

	// The mortal path and alchemy (see PillItem, AlchemyCauldronBlock)
	public static final String MORTAL = k("mortal");
	public static final String MORTAL_DETAIL = k("mortal.detail");
	public static final String MORTAL_RECIPE = k("mortal.recipe");
	public static final String MORTAL_STATS = k("mortal.stats");
	public static final String MORTAL_HOW = k("mortal.how");
	public static final String TEMPERING = k("tempering");
	public static final String MSG_TEMPERED = k("message.tempered");
	public static final String MSG_TEMPERED_PEAK = k("message.tempered_peak");
	public static final String MSG_TEMPER_FULL = k("message.temper_full");
	public static final String MSG_ELIXIR_TOO_WEAK = k("message.elixir_too_weak");
	public static final String MSG_HERB_EATEN = k("message.herb_eaten");
	public static final String NEED_PILL = k("need_pill");
	public static final String PILL_PREPARED = k("pill_prepared");
	public static final String QI_BOOST_LINE = k("qi_boost_line");
	public static final String PILL_RESISTANCE_LINE = k("pill_resistance_line");
	public static final String MSG_MORTAL_MEDITATE = k("message.mortal_meditate");
	public static final String MSG_NEED_PILL = k("message.need_pill");
	public static final String MSG_AWAKENED = k("message.awakened");
	public static final String MSG_ALREADY_AWAKENED = k("message.already_awakened");
	public static final String MSG_PILL_PREPARED = k("message.pill_prepared");
	public static final String MSG_PILL_WRONG_TIME = k("message.pill_wrong_time");
	public static final String MSG_PILL_ALREADY = k("message.pill_already");
	public static final String MSG_PILL_MORTAL = k("message.pill_mortal");
	public static final String MSG_PILL_BOTTLENECK = k("message.pill_bottleneck");
	public static final String MSG_PILL_CULTIVATION = k("message.pill_cultivation");
	public static final String UNREFINED_LINE = k("unrefined_line");
	public static final String MSG_INNER_ENTER = k("message.inner_enter");
	public static final String MSG_INNER_PULLED = k("message.inner_pulled");
	public static final String MSG_INNER_TRIBULATION = k("message.inner_tribulation");
	public static final String MSG_DEVIATION = k("message.qi_deviation");
	public static final String CIRCULATION_HINT = k("circulation.hint");
	public static final String CIRCULATION_STREAK = k("circulation.streak");
	public static final String MSG_PILL_QI = k("message.pill_qi");
	public static String pillGradeKey(String id) { return "pill_grade." + DefyingTheHeavens.MOD_ID + "." + id; }
	public static final String PILL_AGE = k("tooltip.pill_age");
	public static final String PILL_AGE_SHORT = k("tooltip.pill_age_short");
	public static final String PILL_RECIPE = k("tooltip.pill_recipe");
	public static final String PILL_EFFECT_AWAKEN = k("tooltip.pill_effect.awaken");
	public static final String PILL_EFFECT_BREAKTHROUGH = k("tooltip.pill_effect.breakthrough");
	public static final String PILL_EFFECT_START = k("tooltip.pill_effect.start");
	public static final String PILL_EFFECT_QI = k("tooltip.pill_effect.qi");
	public static final String PILL_EFFECT_CULTIVATION = k("tooltip.pill_effect.cultivation");
	public static final String PILL_EFFECT_RESISTANCE = k("tooltip.pill_effect.resistance");
	public static final String PILL_WHEN = k("tooltip.pill_when");
	public static final String CAULDRON_TOOLTIP = k("tooltip.alchemy_cauldron");
	public static final String CAULDRON_NEEDS_WATER = k("cauldron.needs_water");
	public static final String CAULDRON_EMPTY = k("cauldron.empty");
	public static final String CAULDRON_CONTENTS = k("cauldron.contents");
	public static final String CAULDRON_ADDED = k("cauldron.added");
	public static final String CAULDRON_REJECTED = k("cauldron.rejected");
	public static final String CAULDRON_FULL = k("cauldron.full");
	public static final String CAULDRON_EMPTIED = k("cauldron.emptied");
	public static final String CAULDRON_BREWING = k("cauldron.brewing");
	public static final String CAULDRON_NEEDS_HEAT = k("cauldron.needs_heat");
	public static final String CAULDRON_DONE = k("cauldron.done");

	// Ginseng
	public static final String GINSENG_INGREDIENT = k("tooltip.ginseng_ingredient");
	public static final String GINSENG_HARVESTED = k("tooltip.ginseng_harvested");
	public static final String GINSENG_FULL = k("message.ginseng_full");
	public static final String GINSENG_SEEDS_TOOLTIP = k("tooltip.ginseng_seeds");
	public static final String LOTUS_SEEDS_TOOLTIP = k("tooltip.lotus_seeds");
	public static final String DEW_TOOLTIP = k("tooltip.spirit_dew");
	public static final String MSG_DEW_MORTAL = k("message.dew_mortal");
	public static final String MSG_DEW_FULL = k("message.dew_full");
	public static final String MSG_DEW_DRUNK = k("message.dew_drunk");
	public static final String PILL_SUBSTITUTE = k("tooltip.pill_substitute");
	public static final String GINSENG_NEED_CORE = k("message.ginseng.need_core");
	public static final String GINSENG_NOT_ENOUGH = k("message.ginseng.not_enough");
	public static final String GINSENG_MAX = k("message.ginseng.max");
	public static final String GINSENG_FED = k("message.ginseng.fed");

	// Spirit Peach Tree
	public static final String PEACH_PIT_TOOLTIP = k("tooltip.peach_pit");
	public static final String PEACH_NEED_CORE = k("message.peach.need_core");
	public static final String PEACH_NOT_ENOUGH = k("message.peach.not_enough");
	public static final String PEACH_MAX = k("message.peach.max");
	public static final String PEACH_FED = k("message.peach.fed");

	private ModLang() {}
}
