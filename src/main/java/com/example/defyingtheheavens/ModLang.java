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

	/** The mod's creative tab (ModCreativeTab). */
	public static final String CREATIVE_TAB = "itemGroup." + DefyingTheHeavens.MOD_ID;
	public static final String KEY_CATEGORY = "key.categories." + DefyingTheHeavens.MOD_ID;
	public static final String KEY_MENU = "key." + DefyingTheHeavens.MOD_ID + ".cultivation_menu";
	public static final String KEY_MEDITATE = "key." + DefyingTheHeavens.MOD_ID + ".meditate";
	public static final String KEY_CIRCULATE = "key." + DefyingTheHeavens.MOD_ID + ".circulate";
	public static final String KEY_ABILITY_WHEEL = "key." + DefyingTheHeavens.MOD_ID + ".ability_wheel";

	public static final String TITLE = k("title");
	public static final String REALM = k("realm");
	public static final String STAGE = k("stage");
	public static final String CULTIVATION = k("cultivation");
	public static final String CULTIVATION_MAX = k("cultivation_max");
	/**
	 * Under the cultivation bar: the heaven method practised, its layer, and what meditating here gathers a second with it; where
	 * that won't fit, the method and the rate, else the layer and the rate.
	 */
	public static final String METHOD_LINE = k("method_line");
	public static final String METHOD_LINE_NAME = k("method_line.name");
	public static final String METHOD_LINE_SHORT = k("method_line.short");
	public static final String METHOD_LINE_NONE = k("method_line.none");
	/** A heaven method's layer settling in (HeavenMethods#settle), and how far. */
	public static final String SETTLING_LINE = k("settling_line");
	/** The Upper Realm's dense qi, among the Cultivation tab's details. */
	public static final String UPPER_REALM_LINE = k("upper_realm_line");
	public static final String HUD_QI = k("hud.qi");
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
	// The ability wheel (held open by its key) and its editor on the Abilities tab.
	public static final String BTN_ARRANGE_WHEEL = k("abilities.arrange_wheel");
	public static final String BTN_ABILITY_LIST = k("abilities.list");
	public static final String WHEEL_TITLE = k("wheel.title");
	public static final String WHEEL_HINT = k("wheel.hint");
	public static final String WHEEL_EMPTY = k("wheel.empty");
	public static final String WHEEL_EMPTY_SLOT = k("wheel.empty_slot");
	public static final String WHEEL_EDITOR_HINT = k("wheel.editor_hint");
	public static final String WHEEL_EDITOR_KEY = k("wheel.editor_key");
	public static final String WHEEL_EDITOR_PICK = k("wheel.editor_pick");
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
	public static final String MSG_SWORD_FLIGHT_GAINED = k("message.sword_flight.gained");
	public static final String MSG_FLIGHT_EXCLUSIVE = k("message.flight_exclusive");
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
	public static final String MSG_TRIB_FLED = k("message.tribulation.fled");
	public static final String TRIB_HUD = k("hud.tribulation");
	public static final String MSG_ARRIVE_UPPER = k("message.spatial_gap.arrive_upper");
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
	public static final String MSG_AWAKENED_DEAD = k("message.awakened_dead");
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
	public static final String SURGE_CIRCUIT = k("surge.circuit");
	public static final String SURGE_ELEMENTS = k("surge.elements");
	/** An element's name: this and its name in lower case (QiElement#getDisplayName). */
	public static final String ELEMENT_PREFIX = k("element.");
	public static final String SURGE_BREATH = k("surge.breath");
	public static final String SURGE_SYNC = k("surge.sync");
	public static final String MSG_HARMONY = k("message.harmony");
	public static final String MSG_SURGE_FAILED = k("message.surge_failed");
	public static final String MSG_WISPS = k("message.wisps");
	public static final String MSG_WISPS_DONE = k("message.wisps_done");
	public static final String MSG_SEQUENCE = k("message.sequence");
	public static final String MSG_SEQUENCE_GO = k("message.sequence_go");
	public static final String MSG_SEQUENCE_FAIL = k("message.sequence_fail");
	public static final String MSG_DEMON = k("message.heart_demon");
	public static final String MSG_DEMON_WIN = k("message.heart_demon_win");
	public static final String MSG_DEMON_LOSE = k("message.heart_demon_lose");
	public static final String MSG_DEMON_FADE = k("message.heart_demon_fade");
	public static final String MSG_PILL_QI = k("message.pill_qi");
	public static final String MSG_FRUIT_MORTAL = k("message.fruit_mortal");
	public static final String MSG_FRUIT_BOTTLENECK = k("message.fruit_bottleneck");
	public static final String MSG_FRUIT_EATEN = k("message.fruit_eaten");
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

	// Alignment (see Alignment)
	public static String factionKey(String id) { return "faction." + DefyingTheHeavens.MOD_ID + "." + id; }
	public static final String ALIGNMENT_VALUE = k("alignment.value");
	public static final String ALIGNMENT_LINE = k("alignment.line");
	public static final String MSG_ALIGNMENT_UP = k("message.alignment.up");
	public static final String MSG_ALIGNMENT_DOWN = k("message.alignment.down");

	// NPC cultivators and sects (see CultivatorNpc, Sect)
	public static String titleKey(String id) { return "npc_title." + DefyingTheHeavens.MOD_ID + "." + id; }
	public static final String NPC_TITLE_BRACKETS = k("npc.title_brackets");
	public static final String NPC_GATE_GUARD = k("npc.gate_guard");
	public static final String NPC_OF_SECT = k("npc.of_sect");
	public static final String NPC_GREET_RIGHTEOUS = k("npc.greet.righteous");
	public static final String NPC_GREET_NEUTRAL = k("npc.greet.neutral");
	public static final String NPC_GREET_DEMONIC = k("npc.greet.demonic");
	public static final String NPC_GREET_HOSTILE = k("npc.greet.hostile");
	public static final String NPC_GREET_SECLUDED = k("npc.greet.secluded");
	/** Something a cultivator says of its own accord (NpcLines: {@code key} is the line's key and variant number). */
	public static String npcSay(String key) { return k("npc.say." + key); }

	// Talking to cultivators (TalkSystem): the wheel, the dialogue box, and what cultivators say.
	/** A line a cultivator speaks in a {@code kind} ("greet", "ask", "gift.small"...), by their path and standing. */
	public static String talkLine(String kind, String faction, String standing) { return k("talk.line." + kind + "." + faction + "." + standing); }
	/** A line by path alone. */
	public static String talkLine(String kind, String faction) { return k("talk.line." + kind + "." + faction); }
	/** A line with no path to it (a mortal's, a sect fact). */
	public static String talkLine(String kind) { return k("talk.line." + kind); }
	public static String talkIntent(String id) { return k("talk.intent." + id); }
	public static String talkIntentHint(String id) { return k("talk.intent." + id + ".hint"); }
	public static String talkTopic(String id) { return k("talk.topic." + id); }
	public static String talkRegardLevel(String id) { return k("talk.regard." + id); }
	public static String talkDirection(String id) { return k("talk.direction." + id); }
	public static String talkPath(String faction) { return k("talk.path." + faction); }
	public static final String TALK_SAYS = k("talk.says");
	public static final String TALK_REGARD_DELTA = k("talk.regard_delta");
	public static final String TALK_REGARD = k("talk.regard");
	public static final String TALK_BUSY = k("talk.busy");
	public static final String TALK_OCCUPIED = k("talk.occupied");
	public static final String TALK_GIFT_EMPTY = k("talk.gift_empty");
	public static final String TALK_MORTAL = k("talk.mortal");
	public static final String TALK_TITLED = k("talk.titled");
	public static final String TALK_AND = k("talk.and");
	public static final String TALK_LIST = k("talk.list");
	public static final String TALK_BACK = k("talk.back");
	public static final String TALK_PICK = k("talk.pick");
	/** A cultivator with another's soul inside it (SoulProjection) doesn't answer. */
	public static final String TALK_POSSESSED = k("talk.possessed");
	/** One of the living who can't see you (you in a Ghost King's form, or dead, to one below Nascent Soul: CultivatorNpc#senses). */
	public static final String TALK_UNSEEN = k("talk.unseen");
	/** The same, to a Ghost King of the dead: told how it would be seen (its human form, TwoPaths#inHumanForm). */
	public static final String TALK_UNSEEN_KING = k("talk.unseen_king");
	/** Struck mid-talk; a Sworn friend's aid and gift; one begging for its life; a topic's regard; a junior's realm. */
	public static final String TALK_INTERRUPTED = k("talk.interrupted");
	public static final String TALK_AID = k("talk.aid");
	public static final String TALK_GIFT_BACK = k("talk.gift_back");
	public static final String TALK_BEGS = k("talk.begs");
	public static final String TALK_NEEDS = k("talk.needs");
	/** A line on a trading screen not yet open to you (CultivatorTrades#locked): the regard it wants, and theirs for you. */
	public static final String TRADE_LOCKED = k("talk.trade_locked");
	public static final String TALK_AT_REALM = k("talk.at_realm");
	/** Rumours (see Rumours) and guidance: noted in the journal; a senior's Insight. */
	public static final String TALK_JOURNAL_NOTED = k("talk.journal_noted");
	public static final String TALK_INSIGHT = k("talk.insight");
	/** A rumour's words: "old" (a herb, fruit or tree), "cave", "core", "killing", and the pieces they're made of. */
	public static String rumour(String id) { return k("talk.rumour." + id); }
	public static String rumourSubject(Rumours.Kind kind) { return k("talk.rumour.subject." + kind.name().toLowerCase(java.util.Locale.ROOT)); }
	// Sparring (SparSystem)
	public static String sparTerms(String id) { return k("talk.spar_terms." + id); }
	public static final String SPAR_BUSY = k("spar.busy");
	public static final String SPAR_HURT = k("spar.hurt");
	public static final String SPAR_NO_STAKE = k("spar.no_stake");
	public static final String SPAR_WAGERED = k("spar.wagered");
	public static final String SPAR_HOLD_STAKE = k("spar.hold_stake");
	public static final String SPAR_BENEATH = k("spar.beneath");
	public static final String SPAR_FRIENDLY_ONLY = k("spar.friendly_only");
	public static final String SPAR_STAKES = k("spar.stakes");
	public static final String SPAR_COUNT = k("spar.count");
	public static final String SPAR_BEGIN = k("spar.begin");
	public static final String SPAR_COUNTDOWN = k("spar.countdown");
	public static final String SPAR_FIGHT = k("spar.fight");
	// Spirit stones (SpiritStoneItem)
	public static final String MSG_STONE_MORTAL = k("message.stone.mortal");
	public static final String MSG_STONE_QI_FULL = k("message.stone.qi_full");
	public static final String MSG_STONE_ABSORBED = k("message.stone.absorbed");
	public static final String STONE_WORTH = k("stone.worth");
	public static final String STONE_ABSORB = k("stone.absorb");
	public static final String STONE_MEDITATE = k("stone.meditate");
	public static final String STONE_QI_LEFT = k("stone.qi_left");
	/** A settlement's share of its fortune (SpiritStoneItem#fortune): whom it was shared with, and what it fills. */
	public static final String STONE_FORTUNE = k("stone.fortune");
	// The menu's Journal tab (see Journal)
	public static final String TAB_JOURNAL = k("tab.journal");
	public static final String JOURNAL_EMPTY = k("journal.empty");
	public static final String JOURNAL_ENTRY = k("journal.entry");
	public static final String MSG_SECLUSION_REPULSE = k("message.seclusion.repulse");

	// Cultivator clothing (see ClothingItem)
	public static String clothingStyleKey(String id) { return "clothing_style." + DefyingTheHeavens.MOD_ID + "." + id; }
	public static final String CLOTHING_QI = k("tooltip.clothing.qi");
	public static final String CLOTHING_MEDITATION = k("tooltip.clothing.meditation");
	public static final String CLOTHING_UPGRADE_HINT = k("tooltip.clothing.upgrade");
	public static final String BOOST_GARMENTS = k("boost.garments");

	// Formations: the core, its barrier, seals and the brush (see FormationCoreBlockEntity, Formations, SealBlock)
	public static final String CORE_TOOLTIP = k("tooltip.formation_core");
	public static final String CORE_RAISED = k("core.raised");
	public static final String CORE_LOWERED = k("core.lowered");
	public static final String CORE_SHATTERED = k("core.shattered");
	public static final String CORE_TITLE = k("core.title");
	public static final String CORE_RADIUS = k("core.radius");
	public static final String CORE_SWITCH_ON = k("core.switch_on");
	public static final String CORE_SWITCH_OFF = k("core.switch_off");
	public static final String CORE_BATTERY = k("core.battery");
	public static final String CORE_SUPPLY = k("core.supply");
	public static final String CORE_UPKEEP = k("core.upkeep");
	public static final String CORE_VEINS = k("core.veins");
	public static final String CORE_STRENGTH = k("core.strength");
	public static final String CORE_STRENGTH_MORTAL = k("core.strength_mortal");
	public static final String CORE_OWNER = k("core.owner");
	public static final String CORE_SHORTFALL = k("core.shortfall");
	public static final String CORE_HINT = k("core.hint");
	public static final String MSG_CORE_SECT = k("message.core.sect");
	public static final String MSG_CORE_NOT_YOURS = k("message.core.not_yours");
	public static final String MSG_BARRIER_TOO_STRONG = k("message.barrier.too_strong");
	public static final String MSG_BARRIER_GROUND = k("message.barrier.ground");
	public static final String MSG_BARRIER_EXPELLED = k("message.barrier.expelled");
	public static final String MSG_SEAL_UNSEEN = k("message.seal.unseen");
	/** A sect's treasury held shut while its elders keep it (SectManager#treasurySeal): the seal's realm; and broken. */
	public static final String MSG_TREASURY_SEALED = k("message.treasury_sealed");
	public static final String MSG_TREASURY_SEAL_BROKEN = k("message.treasury_seal_broken");
	/** Players in sects (SectMembership): joined, left, cast out (and why), a warning at the treasury, a new title; the Stats line. */
	public static final String MSG_SECT_JOINED = k("message.sect.joined");
	public static final String MSG_SECT_LEFT = k("message.sect.left");
	public static final String MSG_SECT_EXPELLED = k("message.sect.expelled");
	public static final String MSG_SECT_EXPELLED_TREASURY = k("message.sect.expelled.treasury");
	public static final String MSG_SECT_EXPELLED_VIOLENCE = k("message.sect.expelled.violence");
	public static final String MSG_SECT_TREASURY_WARNING = k("message.sect.treasury_warning");
	public static final String MSG_SECT_TITLE = k("message.sect.title");
	public static final String STATS_SECT = k("stats.sect");
	/** The menu's Sect tab (SectMembership#sendInfo). */
	public static final String TAB_SECT = k("tab.sect");
	public static final String SECT_TAB_LOADING = k("sect_tab.loading");
	public static final String SECT_TAB_NONE = k("sect_tab.none");
	public static final String SECT_TAB_HOW = k("sect_tab.how");
	public static final String SECT_TAB_NEAREST = k("sect_tab.nearest");
	public static final String SECT_TAB_NEAREST_NONE = k("sect_tab.nearest_none");
	public static final String SECT_TAB_BARRED = k("sect_tab.barred");
	public static final String SECT_TAB_TITLE = k("sect_tab.title");
	public static final String SECT_TAB_STANDING = k("sect_tab.standing");
	public static final String SECT_TAB_GROUNDS_HERE = k("sect_tab.grounds_here");
	public static final String SECT_TAB_GROUNDS_AWAY = k("sect_tab.grounds_away");
	public static final String SECT_TAB_GROUNDS_ELSEWHERE = k("sect_tab.grounds_elsewhere");
	public static final String SECT_TAB_BARRIER_UP = k("sect_tab.barrier_up");
	public static final String SECT_TAB_BARRIER_DOWN = k("sect_tab.barrier_down");
	public static final String SECT_TAB_TREASURY_OPEN = k("sect_tab.treasury_open");
	public static final String SECT_TAB_TREASURY_CLOSED = k("sect_tab.treasury_closed");
	public static final String SECT_TAB_TREASURY_LAST = k("sect_tab.treasury_last");
	public static final String SECT_TAB_MEMBERS = k("sect_tab.members");
	public static final String SECT_TAB_DAYS = k("sect_tab.days");
	public static final String SECT_TAB_LEAVE = k("sect_tab.leave");
	public static final String SECT_TAB_PLAYER = k("sect_tab.player");
	public static final String SECT_TAB_OFFLINE = k("sect_tab.offline");
	/** Stat points (StatPoints): each stat's name ("stat_point.<id>") and effect (".effect"), and the Stats tab's Points page. */
	public static final String STAT_POINT_PREFIX = k("stat_point.");
	public static final String MSG_STAT_POINTS_RETURNED = k("message.stat_points.returned");
	/** Lifespan (Lifespan): its warnings and its end, alive and dead, and the Stats tab's line. */
	public static final String MSG_LIFESPAN_WARN = k("message.lifespan.warn");
	public static final String MSG_LIFESPAN_END = k("message.lifespan.end");
	public static final String MSG_SOUL_TIME_WARN = k("message.soul_time.warn");
	public static final String MSG_SOUL_TIME_END = k("message.soul_time.end");
	public static final String STATS_LIFESPAN = k("stats.lifespan");
	/** ...a True Immortal's (Lifespan#endless): its age, and no end to its years. */
	public static final String STATS_LIFESPAN_ENDLESS = k("stats.lifespan_endless");
	public static final String CMD_LIFESPAN = k("command.lifespan");
	public static final String CMD_LIFESPAN_ENDLESS = k("command.lifespan_endless");
	/**
	 * The lifespan tribulation (LifespanTribulation): its HUD line (the heavens', the Netherworld's, waiting beyond the gap), its
	 * coming (alive, dead), the server told, its waiting and finding one again, its end, and what it forbids.
	 */
	public static final String LIFE_TRIB_HUD = k("life_trib.hud");
	public static final String LIFE_TRIB_HUD_DEAD = k("life_trib.hud.dead");
	public static final String LIFE_TRIB_HUD_WAITING = k("life_trib.hud.waiting");
	public static final String MSG_LIFE_TRIB_START = k("message.life_trib.start");
	public static final String MSG_LIFE_TRIB_START_DEAD = k("message.life_trib.start_dead");
	public static final String MSG_LIFE_TRIB_BROADCAST = k("message.life_trib.broadcast");
	public static final String MSG_LIFE_TRIB_PAUSED = k("message.life_trib.paused");
	public static final String MSG_LIFE_TRIB_RESUMED = k("message.life_trib.resumed");
	public static final String MSG_LIFE_TRIB_RENEWED = k("message.life_trib.renewed");
	public static final String MSG_LIFE_TRIB_RENEWED_BROADCAST = k("message.life_trib.renewed_broadcast");
	public static final String MSG_LIFE_TRIB_NO_PILL = k("message.life_trib.no_pill");
	public static final String MSG_POSSESS_HEAVENS = k("message.life_trib.possess_heavens");
	public static final String MSG_SOUL_BODY_TAKEN = k("message.life_trib.body_taken");
	/**
	 * What years are spent on besides (the user's asks, 2026-10-21; Lifespan): the meditation's pace (its button and tooltips, its price
	 * told on changing it), a fast meditation slowed near the end, space travel's HUD line and its warning, a mortal's old age.
	 */
	public static final String BTN_PACE = k("button.pace");
	public static final String TIP_PACE_1 = k("tooltip.pace.1");
	public static final String TIP_PACE_2 = k("tooltip.pace.2");
	public static final String TIP_PACE_3 = k("tooltip.pace.3");
	public static final String[] TIP_PACE = {TIP_PACE_1, TIP_PACE_2, TIP_PACE_3};
	public static final String MSG_PACE_1 = k("message.pace.1");
	public static final String MSG_PACE_2 = k("message.pace.2");
	public static final String MSG_PACE_3 = k("message.pace.3");
	public static final String[] MSG_PACE = {MSG_PACE_1, MSG_PACE_2, MSG_PACE_3};
	public static final String MSG_MEDITATION_SLOWED = k("message.pace.slowed");
	/** The Cultivation tab's word of the talent race (Lifespan#seclude): the years to the realm's peak, and those left. */
	public static final String SECLUSION_LINE = k("line.seclusion");
	public static final String HUD_SPACE_YEARS = k("hud.space_years");
	public static final String MSG_SPACE_NO_WAY_HOME = k("message.astral.no_way_home");
	public static final String MSG_OLD_AGE = k("message.lifespan.old_age");
	/** Burn Lifespan (BurnLifespan): lit, put out, gone out by itself near the end, refused with too few years left. */
	public static final String MSG_BURN_ON = k("message.burn_lifespan.on");
	/** ...lit by a True Immortal (Lifespan#endless): its years have no end, so it burns for nothing. */
	public static final String MSG_BURN_ON_ENDLESS = k("message.burn_lifespan.on_endless");
	public static final String MSG_BURN_OFF = k("message.burn_lifespan.off");
	public static final String MSG_BURN_OUT = k("message.burn_lifespan.out");
	public static final String MSG_BURN_TOO_FEW = k("message.burn_lifespan.too_few");
	/** Life Force's years (WoodSpells): drawn out of another (told to both), given to a herb, refused with too few years left. */
	public static final String MSG_LIFE_YEARS_DRAWN = k("message.spell.life_years_drawn");
	public static final String MSG_LIFE_YEARS_LOST = k("message.spell.life_years_lost");
	public static final String MSG_LIFE_YEARS_GIVEN = k("message.spell.life_years_given");
	public static final String MSG_LIFE_YEARS_TOO_FEW = k("message.spell.life_years_too_few");
	/**
	 * The four axes of virtue (FourAxis): each axis's name ("axis.&lt;id&gt;"), what it is measured in ("axis_unit.&lt;id&gt;"), how
	 * it is filled ("axis_how.&lt;id&gt;"), its gate, what was won toward it and its completion.
	 */
	public static final String AXIS_PREFIX = k("axis.");
	public static final String AXIS_UNIT_PREFIX = k("axis_unit.");
	public static final String AXIS_HOW_PREFIX = k("axis_how.");
	public static final String AXIS_GATE = k("axis_gate");
	/** The Cultivation tab's axis bar: what is written on it (and where that won't fit, the short); under it, once the breakthrough waits on it, how it is filled. */
	public static final String AXIS_BAR = k("axis_bar");
	public static final String AXIS_BAR_SHORT = k("axis_bar.short");
	public static final String MSG_AXIS_PROGRESS = k("message.axis.progress");
	public static final String MSG_AXIS_COMPLETE = k("message.axis.complete");
	/** A settler's wish (Wishes): why one can't grant it, how it's said ("wish.say.<axis>.<1-3>"), its grant and its outcomes. */
	public static final String WISH_NOT_FOUR_AXIS = k("wish.not_four_axis");
	public static final String WISH_AXES_DONE = k("wish.axes_done");
	public static final String WISH_AXIS_DONE = k("wish.axis_done");
	public static final String WISH_OTHER_AXIS = k("wish.other_axis");
	public static final String WISH_NONE = k("wish.none");
	public static final String WISH_SAY = k("wish.say.");
	public static final String WISH_HEALED = k("wish.healed");
	public static final String WISH_THREAT_OUT = k("wish.threat_out");
	public static final String WISH_GUIDE_SIT = k("wish.guide_sit");
	public static final String WISH_TAKE_NONE = k("wish.take.none");
	public static final String WISH_TAKE_FEW = k("wish.take.few");
	public static final String WISH_TAKE_SHARE = k("wish.take.share");
	public static final String WISH_TAKE_ALL = k("wish.take.all");
	public static final String WISH_GRANT_HEAL = k("wish.grant.heal");
	public static final String WISH_GRANT_GIVE = k("wish.grant.give");
	public static final String WISH_GRANT_THREAT = k("wish.grant.threat");
	public static final String WISH_GRANT_GUIDE = k("wish.grant.guide");
	public static final String WISH_HEAL_QI = k("wish.heal_qi");
	public static final String WISH_TOOK_NONE = k("wish.took_none");
	public static final String WISH_TOOK = k("wish.took");
	public static final String WISH_GIVE_LACK = k("wish.give_lack");
	public static final String WISH_GIVEN = k("wish.given");
	public static final String WISH_THREAT_SLAIN = k("wish.threat_slain");
	public static final String WISH_GUIDED = k("wish.guided");
	public static final String WISH_NOT_FOUR_AXIS_DEAD = k("wish.not_four_axis_dead");
	public static final String WISH_THREAT_OUT_MANY = k("wish.threat_out_many");
	public static final String WISH_THREAT_LOST = k("wish.threat_lost");
	public static final String WISH_GUIDED_WITHIN = k("wish.guided_within");
	/**
	 * "Name Your Obsession" (Obsession): each obsession's name ("obsession.&lt;axis id&gt;") and what it gives ("obsession.boon.&lt;id&gt;"),
	 * the screen's lines, the naming, the Stats tab's line, the command's report.
	 */
	public static final String OBSESSION_PREFIX = k("obsession.");
	public static final String OBSESSION_BOON_PREFIX = k("obsession.boon.");
	public static final String OBSESSION_TITLE = k("obsession.title");
	public static final String OBSESSION_INTRO = k("obsession.intro");
	public static final String OBSESSION_AXIS = k("obsession.axis");
	public static final String OBSESSION_CHOOSE = k("obsession.choose");
	public static final String OBSESSION_CHOSEN = k("obsession.chosen");
	public static final String OBSESSION_CONFIRM = k("obsession.confirm");
	public static final String MSG_OBSESSION_NAMED = k("message.obsession_named");
	public static final String STATS_OBSESSION = k("stats.obsession");
	public static final String STATS_OBSESSION_BOON = k("stats.obsession_boon");
	public static final String CMD_OBSESSION = k("command.obsession");
	public static final String CMD_OBSESSION_NONE = k("command.obsession_none");
	/** A settlement of the Astral Realm's worlds (Settlements): its name on coming near. */
	public static final String MSG_SETTLEMENT_NEAR = k("message.settlement_near");
	public static final String MSG_SETTLEMENT_SMOKE = k("message.settlement_smoke");
	/** A newcomer come to a settlement that has lost people (Settlements#replenish). */
	public static final String MSG_SETTLER_ARRIVES = k("message.settler_arrives");
	public static final String BTN_STAT_POINTS = k("button.stat_points");
	public static final String BTN_STAT_LIST = k("button.stat_list");
	public static final String STAT_POINTS_UNSPENT = k("stat_points.unspent");
	public static final String STAT_POINTS_EARNED = k("stat_points.earned");
	public static final String STAT_POINTS_HINT = k("stat_points.hint");
	public static final String STAT_POINTS_MORTAL = k("stat_points.mortal");
	/** A ghost palace's treasury (PalaceTreasury): its king's seal holding, or broken. */
	public static final String MSG_PALACE_SEALED = k("message.palace_sealed");
	public static final String MSG_PALACE_SEAL_BROKEN = k("message.palace_seal_broken");
	public static final String BRUSH_TOOLTIP = k("tooltip.inscription_brush");
	public static final String BRUSH_TOOLTIP_ERASE = k("tooltip.inscription_brush.erase");
	public static final String MSG_CORE_CLAIMED = k("message.core.claimed");
	// The core's trusted cultivators
	public static final String CORE_TRUSTED_BUTTON = k("core.trusted.button");
	public static final String CORE_TRUSTED_TITLE = k("core.trusted.title");
	public static final String CORE_TRUSTED_HINT = k("core.trusted.hint");
	public static final String CORE_TRUSTED_COUNT = k("core.trusted.count");
	public static final String CORE_TRUSTED_NONE = k("core.trusted.none");
	public static final String CORE_TRUST_ADD = k("core.trusted.add");
	public static final String CORE_TRUST_REMOVE = k("core.trusted.remove");
	public static final String CORE_BACK = k("core.back");
	public static final String MSG_TRUST_ADDED = k("message.trust.added");
	public static final String MSG_TRUST_REMOVED = k("message.trust.removed");
	public static final String MSG_TRUST_UNKNOWN = k("message.trust.unknown");
	public static final String MSG_TRUST_FULL = k("message.trust.full");
	public static final String MSG_TRUST_SELF = k("message.trust.self");
	// Soul Projection (see SoulProjection)
	public static final String MSG_SOUL_GAINED = k("message.soul.gained");
	public static final String MSG_SOUL_GATHERING = k("message.soul.gathering");
	public static final String MSG_SOUL_CANCELLED = k("message.soul.cancelled");
	public static final String MSG_SOUL_MOVED = k("message.soul.moved");
	public static final String MSG_SOUL_NOT_HERE = k("message.soul.not_here");
	public static final String MSG_SOUL_GROUND = k("message.soul.ground");
	public static final String MSG_SOUL_LOW_QI = k("message.soul.low_qi");
	/** A soul born of the ghost path (PlayerCultivation#soulPath) pays in ghost qi: its words for the pool short and spent. */
	public static final String MSG_SOUL_LOW_GHOST_QI = k("message.soul.low_ghost_qi");
	public static final String MSG_SOUL_NO_GHOST_QI = k("message.soul.no_ghost_qi");
	public static final String MSG_POSSESS_GHOST_QI = k("message.possess.ghost_qi");
	public static final String MSG_SOUL_OUT = k("message.soul.out");
	public static final String MSG_SOUL_RECALLING = k("message.soul.recalling");
	public static final String MSG_SOUL_BACK = k("message.soul.back");
	public static final String MSG_SOUL_NO_QI = k("message.soul.no_qi");
	public static final String MSG_SOUL_BUSY = k("message.soul.busy");
	public static final String MSG_SOUL_LOST = k("message.soul.lost");
	public static final String MSG_SOUL_DESTROYED = k("message.soul.destroyed");
	public static final String MSG_SOUL_SEVERED = k("message.soul.severed");
	public static final String MSG_SOUL_BODY_SLAIN = k("message.soul.body_slain");
	public static final String MSG_SOUL_BODY_GONE = k("message.soul.body_gone");
	public static final String MSG_SOUL_FLED = k("message.soul.fled");
	public static final String MSG_SOUL_BODY_HURT = k("message.soul.body_hurt");
	public static final String MSG_SOUL_TETHER_STRUCK = k("message.soul.tether_struck");
	public static final String MSG_SOUL_SENSED = k("message.soul.sensed");
	public static final String MSG_SOUL_BARRIER = k("message.soul.barrier");
	public static final String MSG_POSSESS_START = k("message.possess.start");
	public static final String MSG_POSSESS_CANNOT = k("message.possess.cannot");
	public static final String MSG_POSSESS_PVP = k("message.possess.pvp");
	public static final String MSG_POSSESS_TOO_STRONG = k("message.possess.too_strong");
	public static final String MSG_POSSESS_QI = k("message.possess.qi");
	public static final String MSG_POSSESS_EJECTED = k("message.possess.ejected");
	public static final String MSG_POSSESS_TETHER = k("message.possess.tether");
	public static final String MSG_POSSESS_DEAD = k("message.possess.dead");
	public static final String MSG_POSSESS_INHABIT = k("message.possess.inhabit");
	public static final String MSG_POSSESS_TAKEN = k("message.possess.taken");
	public static final String MSG_POSSESS_FREED = k("message.possess.freed");
	public static final String MSG_POSSESS_LET_GO = k("message.possess.let_go");
	public static final String MSG_POSSESS_BODY_DIED = k("message.possess.body_died");
	public static final String MSG_POSSESS_OWNER_LEFT = k("message.possess.owner_left");
	public static final String MSG_POSSESS_LOST = k("message.possess.lost");
	public static final String MSG_POSSESS_OWNER_AWAY = k("message.possess.owner_away");
	public static final String MSG_POSSESS_OWNER_BACK = k("message.possess.owner_back");
	public static final String MSG_POSSESS_STILL_HELD = k("message.possess.still_held");
	public static final String MSG_POSSESS_DIED_AWAY = k("message.possess.died_away");
	public static final String MSG_POSSESS_GUIDANCE = k("message.possess.guidance");
	public static final String MSG_POSSESS_MERIDIANS = k("message.possess.meridians");
	public static final String MSG_POSSESS_MERIDIANS_TAKEN = k("message.possess.meridians_taken");
	public static final String ABILITY_SOUL_WOUNDED = k("ability.soul_wounded");
	public static final String HUD_SOUL_TETHER_AWAY = k("hud.soul.tether_away");
	public static final String HUD_SOUL_INSIDE = k("hud.soul.inside");
	public static final String HUD_SOUL_OWN_QI_LABEL = k("hud.soul.own_qi_label");
	public static final String HUD_SOUL_OWN_GHOST_QI_LABEL = k("hud.soul.own_ghost_qi_label");
	public static final String HUD_SOUL_GATHERING = k("hud.soul.gathering");
	public static final String HUD_SOUL_RECALLING = k("hud.soul.recalling");
	public static final String HUD_SOUL_TETHER = k("hud.soul.tether");
	public static final String HUD_SOUL_BODY = k("hud.soul.body");
	public static final String HUD_SOUL_POSSESSING = k("hud.soul.possessing");
	public static final String DEATH_SOUL_SEVERED = "death.attack." + DefyingTheHeavens.MOD_ID + ".soul_severed";
	public static final String DEATH_OLD_AGE = "death.attack." + DefyingTheHeavens.MOD_ID + ".old_age";
	public static final String DEATH_SOUL_SEIZED = "death.attack." + DefyingTheHeavens.MOD_ID + ".soul_seized";
	// The Ritual to the Heavens (see HeavenRitual, HeavenAltar)
	public static String ritualOffering(String id) { return k("ritual.offering." + id); }
	public static String ritualGrandeur(String id) { return k("ritual.grandeur." + id); }
	public static final String ALTAR_TOOLTIP = k("tooltip.heaven_altar");
	public static final String ALTAR_TOOLTIP_BEGIN = k("tooltip.heaven_altar.begin");
	public static final String ALTAR_TRAY = k("ritual.tray");
	// The altar's screen (HeavenAltarScreen)
	public static final String ALTAR_CHANCE = k("ritual.menu.chance");
	public static final String ALTAR_OFFERING = k("ritual.menu.offering");
	public static final String ALTAR_WORTH = k("ritual.menu.worth");
	public static final String ALTAR_WORTH_NEXT = k("ritual.menu.worth_next");
	public static final String ALTAR_ALTAR = k("ritual.menu.altar");
	public static final String ALTAR_HEIGHT = k("ritual.menu.height");
	public static final String ALTAR_HEIGHT_Y = k("ritual.menu.height_y");
	public static final String ALTAR_REWARD = k("ritual.menu.reward");
	public static final String ALTAR_REWARD_LEAVE = k("ritual.menu.reward_leave");
	public static final String ALTAR_REWARD_FAVOUR = k("ritual.menu.reward_favour");
	public static final String ALTAR_REWARD_HAVE = k("ritual.menu.reward_have");
	public static String altarPart(String id) { return k("ritual.menu.part." + id); }
	public static String altarState(String id) { return k("ritual.menu.state." + id); }
	public static final String ALTAR_NO_SKY = k("ritual.no_sky");
	public static final String FAVOUR_LINE = k("ritual.favour_line");
	public static final String RITUAL_AHEAD = k("ritual.ahead");
	public static final String RITUAL_GATE = k("ritual.gate");
	public static final String RITUAL_HOW = k("ritual.how");
	public static final String MSG_RITUAL_GATE = k("message.ritual.gate");
	public static final String MSG_RITUAL_BEGIN = k("message.ritual.begin");
	public static final String MSG_RITUAL_NOT_NOW = k("message.ritual.not_now");
	public static final String MSG_RITUAL_DAWN = k("message.ritual.dawn");
	public static final String MSG_RITUAL_ALTAR_BUSY = k("message.ritual.altar_busy");
	public static final String MSG_RITUAL_BUSY = k("message.ritual.busy");
	public static final String MSG_RITUAL_MOVED = k("message.ritual.moved");
	public static final String MSG_RITUAL_BROKEN = k("message.ritual.broken");
	public static final String MSG_RITUAL_SUCCESS = k("message.ritual.success");
	public static final String MSG_RITUAL_FAVOUR = k("message.ritual.favour");
	public static final String MSG_RITUAL_FAIL = k("message.ritual.fail");
	public static final String RITUAL_TITLE_SUCCESS = k("ritual.title.success");
	public static final String RITUAL_TITLE_FAIL = k("ritual.title.fail");
	public static final String HUD_RITUAL = k("hud.ritual");

	// The Netherworld (see HighRealm, GhostGate, SoulRiver)
	public static final String MSG_HEAVENS_BLIND = k("message.netherworld.heavens_blind");
	public static final String MSG_GATE_PROTECTED = k("message.netherworld.gate_protected");
	public static final String MSG_WHEEL_LIVING = k("message.netherworld.wheel_living");
	public static final String MSG_ARRIVE_NETHERWORLD = k("message.netherworld.arrive");
	public static final String MSG_ARRIVE_NETHER = k("message.netherworld.arrive_nether");
	public static final String MSG_RIVER_ASCEND = k("message.soul_river.ascend");
	public static final String MSG_RIVER_DESCEND = k("message.soul_river.descend");
	public static final String MSG_RIVER_ABYSS = k("message.soul_river.abyss");
	public static final String MSG_RIVER_RESTART = k("message.soul_river.restart");
	public static final String MSG_RIVER_SCATTERED_UP = k("message.soul_river.scattered_up");
	public static final String MSG_RIVER_SCATTERED_DOWN = k("message.soul_river.scattered_down");
	public static final String MSG_RIVER_COOLDOWN = k("message.soul_river.cooldown");
	public static final String HUD_RIVER = k("hud.soul_river");
	public static final String HUD_RIVER_STRAY = k("hud.soul_river.stray");
	public static final String DEATH_SOUL_SCATTER = "death.attack." + DefyingTheHeavens.MOD_ID + ".soul_scatter";
	public static final String DEATH_JUDGE_GAZE = "death.attack." + DefyingTheHeavens.MOD_ID + ".judge_gaze";
	public static final String DEATH_GHOSTLY = "death.attack." + DefyingTheHeavens.MOD_ID + ".ghostly";
	public static final String GAMERULE_AFTER_DEATH = "gamerule.netherworldAfterDeath";
	public static final String MSG_GHOST_WAKE = k("message.ghost.wake");
	public static final String MSG_YIN_OVERTAKEN = k("message.ghost.yin_overtaken");
	public static final String MSG_GHOST_DISPERSED = k("message.ghost.dispersed");
	public static final String MSG_GHOST_REFORMED = k("message.ghost.reformed");
	public static final String MSG_GHOST_CANNOT = k("message.ghost.cannot");
	public static final String MSG_WHEEL_TURNS = k("message.ghost.wheel_turns");
	public static final String MSG_WHEEL_HARDCORE = k("message.ghost.wheel_hardcore");
	public static final String MSG_REVIVED = k("message.ghost.revived");
	public static final String MSG_BELONGINGS_LEFT = k("message.ghost.belongings_left");
	public static final String MSG_GHOST_KING = k("message.ghost.king");
	public static final String MSG_GHOST_KING_LOST = k("message.ghost.king_lost");
	public static final String JOURNAL_GHOST_KING = k("journal.ghost_king");
	public static final String JOURNAL_GHOST_KING_TEXT = k("journal.ghost_king.text");
	public static final String MSG_JUDGE_WARNING = k("message.judge.warning");
	public static final String MSG_JUDGE_STRIPPED = k("message.judge.stripped");
	public static final String MSG_JUDGE_MORTAL = k("message.judge.mortal");
	public static final String MSG_YIN_HALF = k("message.yin.half");
	public static final String MSG_YIN_THREE_QUARTERS = k("message.yin.three_quarters");
	public static final String MSG_YIN_NEAR = k("message.yin.near");
	public static final String MSG_YIN_CLEANSED = k("message.yin.cleansed");
	public static final String MSG_YIN_NONE = k("message.yin.none");
	public static final String YIN_LINE = k("netherworld.yin_line");
	public static final String YIN_EBBING = k("netherworld.yin_ebbing");
	public static final String YIN_HELD = k("netherworld.yin_held");
	public static final String GHOST_QI_HERE = k("netherworld.ghost_qi_here");
	public static final String GHOST_QI_DENSE = k("netherworld.ghost_qi.dense");
	public static final String GHOST_QI_PEAKS = k("netherworld.ghost_qi.peaks");
	public static final String GHOST_QI_THIN = k("netherworld.ghost_qi.thin");
	public static final String GHOST_QI_FAINT = k("netherworld.ghost_qi.faint");
	public static final String HUD_GHOST_QI_LABEL = k("hud.ghost_qi_label");
	public static final String HUD_YIN_CORRUPTION_LABEL = k("hud.yin_corruption_label");
	public static final String HUD_PERCENT = k("hud.percent");
	public static final String TOOLTIP_YANG_FIRE_TALISMAN = k("tooltip.yang_fire_talisman");
	// The ghost methods and their spells (see GhostMethods, Spells)
	public static String methodKey(String id) { return "ghost_method." + DefyingTheHeavens.MOD_ID + "." + id; }
	public static String spellKey(String id) { return "spell." + DefyingTheHeavens.MOD_ID + "." + id; }
	/** A heaven method's name; and with ".description" and ".gains" its lore and what its layers bring (HeavenMethod). */
	public static String heavenMethodKey(String id) { return "heaven_method." + DefyingTheHeavens.MOD_ID + "." + id; }
	// The heaven methods (HeavenMethods): learning, cultivating, the Dao Steles, the jade slips, the Methods tab's heaven page
	public static final String MSG_HEAVEN_METHOD_MERIDIANS = k("message.heaven_method.meridians");
	public static final String MSG_HEAVEN_METHOD_DEAD = k("message.heaven_method.dead");
	public static final String MSG_HEAVEN_LEARNED = k("message.heaven_method.learned");
	public static final String MSG_HEAVEN_PRACTISED = k("message.heaven_method.practised");
	/** A layer settled in (HeavenMethods#settle); with the spell it brings. */
	public static final String MSG_HEAVEN_LAYER = k("message.heaven_method.layer");
	public static final String MSG_HEAVEN_LAYER_SPELL = k("message.heaven_method.layer_spell");
	/** Meditating with no heaven method known (MeditationManager), once a login. */
	public static final String MSG_HEAVEN_NONE_KNOWN = k("message.heaven_method.none_known");
	/** A breakthrough into a realm waiting on a heaven method of it (HeavenMethods#gateLine). */
	public static final String HEAVEN_GATE_NONE = k("heaven_method.gate.none");
	public static final String JOURNAL_HEAVEN_METHOD = k("journal.heaven_method");
	public static final String JOURNAL_HEAVEN_METHOD_TEXT = k("journal.heaven_method.text");
	public static final String MSG_SPELL_NO_HEAVEN_QI = k("message.spell.no_heaven_qi");
	public static final String MSG_SPELL_NO_HOLD = k("message.spell.no_hold");
	// The element spells' own words (FireSpells to MetalSpells)
	public static final String MSG_YANG_BOMB_SET = k("message.spell.yang_bomb_set");
	public static final String MSG_YANG_BOMB_FAR = k("message.spell.yang_bomb_far");
	public static final String MSG_YANG_BOMB_GONE = k("message.spell.yang_bomb_gone");
	public static final String MSG_NO_EARTH = k("message.spell.no_earth");
	public static final String MSG_ORES_FOUND = k("message.spell.ores_found");
	public static final String MSG_NO_ORES = k("message.spell.no_ores");
	public static final String MSG_PHOENIX_REBORN = k("message.spell.phoenix_reborn");
	/** Life Force Drain or Gain refused: a sealed treasury's herb; a whole stack held rather than one. */
	public static final String MSG_LIFE_SEALED = k("message.spell.life_sealed");
	public static final String MSG_LIFE_HOLD_ONE = k("message.spell.life_hold_one");
	// The body one is born with (Physique) and the body refining of shattered meridians (BodyRefining)
	/** A physique's name; with ".description" what it does. */
	public static String physiqueKey(String id) { return "physique." + DefyingTheHeavens.MOD_ID + "." + id; }
	public static String physiqueGradeKey(String id) { return "physique_grade." + DefyingTheHeavens.MOD_ID + "." + id; }
	public static String bodyRefiningKey(String id) { return "body_refining." + DefyingTheHeavens.MOD_ID + "." + id; }
	/** The roll on a first join: the physique, its grade; and the roots' elements where it has any. */
	public static final String MSG_PHYSIQUE_BORN = k("message.physique.born");
	public static final String MSG_PHYSIQUE_ROOTS = k("message.physique.roots");
	/** A heaven method of an element one's roots won't take. */
	public static final String MSG_PHYSIQUE_WRONG_ELEMENT = k("message.physique.wrong_element");
	/** Shattered meridians: a breakthrough or a cultivation elixir refused; a body refining stage reached. */
	public static final String MSG_PHYSIQUE_SHATTERED = k("message.physique.shattered");
	public static final String MSG_BODY_REFINED = k("message.physique.body_refined");
	public static final String MSG_THUNDER_ABSORBED = k("message.physique.thunder_absorbed");
	public static final String JOURNAL_PHYSIQUE = k("journal.physique");
	public static final String JOURNAL_PHYSIQUE_TEXT = k("journal.physique.text");
	public static final String STATS_PHYSIQUE = k("stats.physique");
	public static final String STATS_ROOTS = k("stats.roots");
	public static final String STATS_BODY_REFINING = k("stats.body_refining");
	public static final String CMD_PHYSIQUE_GET = k("command.physique.get");
	public static final String CMD_PHYSIQUE_SET = k("command.physique.set");
	public static final String CMD_PHYSIQUE_UNKNOWN = k("command.physique.unknown");
	/** A Myriad Form Body's forms: told when born, in its journal, on the Stats tab. */
	public static final String MSG_PHYSIQUE_FORMS = k("message.physique.forms");
	public static final String JOURNAL_PHYSIQUE_FORM = k("journal.physique.form");
	public static final String STATS_FORMS = k("stats.forms");
	/** A Soul-Devouring Body's kill: the cultivation devoured; the drag on its path. */
	public static final String MSG_DEVOURED = k("message.physique.devoured");
	public static final String MSG_DEVOURED_DRAG = k("message.physique.devoured_drag");
	/** To a player a Soul-Devouring Body slew: who devoured them, and the stage they fell to. */
	public static final String MSG_DEVOURED_FALL = k("message.physique.devoured_fall");
	/** A Heaven-Defying Body's killing strike, defied (TribulationManager#allowDeath); any tribulation of its failed, costing it nothing (#holds). */
	public static final String MSG_TRIB_DEFIED = k("message.tribulation.defied");
	public static final String MSG_TRIB_HOLDS = k("message.tribulation.holds");
	/** An Eternal Reincarnation Body born again, at the rank it begins from (Lifespan#reincarnate). */
	public static final String MSG_REINCARNATED = k("message.physique.reincarnated");
	/** A pill brewed by an Innate Alchemy Body (PillItem#ALCHEMY_BODY_TAG). */
	public static final String PILL_ALCHEMY_BODY = k("tooltip.pill.alchemy_body");
	// The Storage Ring (StorageRingItem, StorageRings) and its screen
	public static String storageGradeKey(String id) { return "storage_grade." + DefyingTheHeavens.MOD_ID + "." + id; }
	/** "Storage Ring (Spirit grade)"; the last grade's own name. */
	public static final String STORAGE_RING_NAME = "item." + DefyingTheHeavens.MOD_ID + ".storage_ring.graded";
	public static final String STORAGE_RING_BOUNDLESS = "item." + DefyingTheHeavens.MOD_ID + ".storage_ring.boundless";
	public static final String STORAGE_TOOLTIP_STACKS = k("tooltip.storage_ring.stacks");
	public static final String STORAGE_TOOLTIP_BOUNDLESS = k("tooltip.storage_ring.boundless");
	public static final String STORAGE_TOOLTIP_USE = k("tooltip.storage_ring.use");
	public static final String STORAGE_USED = k("storage_ring.used");
	public static final String STORAGE_USED_BOUNDLESS = k("storage_ring.used_boundless");
	public static final String STORAGE_SEARCH = k("storage_ring.search");
	public static final String STORAGE_EMPTY = k("storage_ring.empty");
	public static final String STORAGE_REFINE = k("storage_ring.refine");
	public static final String STORAGE_REFINE_TO = k("storage_ring.refine_to");
	public static final String STORAGE_REFINE_REALM = k("storage_ring.refine_realm");
	public static final String STORAGE_REFINE_LAST = k("storage_ring.refine_last");
	public static final String STORAGE_COUNT = k("storage_ring.count");
	public static final String MSG_STORAGE_NO_RING = k("message.storage_ring.none");
	public static final String MSG_STORAGE_LAST_GRADE = k("message.storage_ring.last_grade");
	public static final String MSG_STORAGE_REALM = k("message.storage_ring.realm");
	public static final String MSG_STORAGE_COST = k("message.storage_ring.cost");
	public static final String MSG_STORAGE_REFINED = k("message.storage_ring.refined");
	public static final String KEY_STORAGE_RING = "key." + DefyingTheHeavens.MOD_ID + ".storage_ring";
	// Switching abilities by key, without the wheel (client QuickAbilities)
	public static final String KEY_QUICK_ABILITY = "key." + DefyingTheHeavens.MOD_ID + ".quick_ability";
	/** Followed by the slot's number, 1 to 8: "key.defying-the-heavens.ability_slot_3". */
	public static final String KEY_ABILITY_SLOT_PREFIX = "key." + DefyingTheHeavens.MOD_ID + ".ability_slot_";
	public static final String MSG_QUICK_ABILITY_STATE = k("message.quick_ability.state");
	public static final String MSG_QUICK_ABILITY_EMPTY = k("message.quick_ability.empty");
	public static final String MSG_SELECTED_ABILITY_NONE = k("message.selected_ability.none");
	public static final String MSG_SELECTED_ABILITY_ASLEEP = k("message.selected_ability.asleep");
	public static final String WHEEL_SELECTED = k("wheel.selected");
	public static final String HUD_SELECTED_ABILITY = k("hud.selected_ability");
	// Detonating the golden core (CoreDetonation)
	public static final String MSG_CORE_CONFIRM = k("message.core_detonation.confirm");
	public static final String MSG_CORE_TOO_LATE = k("message.core_detonation.too_late");
	public static final String MSG_CORE_BREAKING = k("message.core_detonation.breaking");
	public static final String DEATH_CORE_DETONATION = "death.attack." + DefyingTheHeavens.MOD_ID + ".core_detonation";
	// The Spatial Tear and travel through the gap (SpatialTrialHandler#tear)
	public static final String MSG_GAP_TRAVEL = k("message.spatial_gap.travel");
	public static final String MSG_GAP_TRAVEL_NO_TEAR = k("message.spatial_gap.travel_no_tear");
	public static final String MSG_GAP_TRAVEL_SHALLOWS = k("message.spatial_gap.travel_shallows");
	public static final String MSG_GAP_TRAVEL_UPPER = k("message.spatial_gap.travel_upper");
	public static final String MSG_GAP_TRAVEL_LOWER = k("message.spatial_gap.travel_lower");
	public static final String MSG_GAP_SETTLING = k("message.spatial_gap.settling");
	public static final String MSG_TEAR_COOLING = k("message.spatial_tear.cooling");
	public static final String MSG_TEAR_OPEN = k("message.spatial_tear.open");
	public static final String MSG_TEAR_NOT_HERE = k("message.spatial_tear.not_here");
	public static final String MSG_TEAR_NO_QI = k("message.spatial_tear.no_qi");
	/** The tear awakened by the ghost path (TwoPaths#awakens) is paid in ghost qi. */
	public static final String MSG_TEAR_NO_GHOST_QI = k("message.spatial_tear.no_ghost_qi");
	public static final String MSG_TEAR_OUT = k("message.spatial_tear.out");
	public static final String MSG_TEAR_OUT_UPPER = k("message.spatial_tear.out_upper");
	public static final String MSG_DAO_STELE_TEACHES = k("message.dao_stele.teaches");
	public static final String MSG_DAO_STELE_KNOWN = k("message.dao_stele.known");
	public static final String MSG_DAO_STELE_READING = k("message.dao_stele.reading");
	public static final String ITEM_JADE_SLIP_OF = "item." + DefyingTheHeavens.MOD_ID + ".jade_slip.of";
	public static final String TOOLTIP_JADE_SLIP = k("tooltip.jade_slip");
	public static final String TOOLTIP_JADE_SLIP_ELEMENT = k("tooltip.jade_slip.element");
	public static final String METHODS_PAGE_GHOST = k("methods.page.ghost");
	public static final String METHODS_PAGE_HEAVEN = k("methods.page.heaven");
	public static final String METHODS_HEAVEN_KNOWN = k("methods.heaven.known");
	public static final String METHODS_HEAVEN_EMPTY = k("methods.heaven.empty");
	public static final String METHODS_HEAVEN_HINT = k("methods.heaven.hint");
	public static final String METHODS_HEAVEN_DEAD = k("methods.heaven.dead");
	public static final String METHODS_HEAVEN_SEALED = k("methods.heaven.sealed");
	public static final String METHODS_HEAVEN_RATE = k("methods.heaven.rate");
	/** The practised method understood as far as the realm: where its next layer opens. */
	public static final String METHODS_HEAVEN_NEXT = k("methods.heaven.next");
	/** A method's next layer, waiting on the realm (in short where that won't fit). */
	public static final String METHODS_HEAVEN_OPENS = k("methods.heaven.opens");
	public static final String METHODS_HEAVEN_OPENS_SHORT = k("methods.heaven.opens.short");
	public static final String METHODS_HEAVEN_SHARE = k("methods.heaven.share");
	/** The deepest realm a breakthrough may lead into, by the methods known (HeavenMethods#opensWay). */
	public static final String METHODS_HEAVEN_REACH = k("methods.heaven.reach");
	public static final String METHODS_HEAVEN_UNKNOWN = k("methods.heaven.unknown");
	public static final String METHODS_HEAVEN_PASSIVE = k("methods.heaven.passive");
	public static final String TALK_TEACH_HEAVEN = k("talk.teach.heaven");
	public static final String TALK_TEACH_HEAVEN_PROGRESS = k("talk.teach.heaven_progress");
	/** An element's passive's name and its line on the Methods tab (HeavenPassives): {@code passive.<mod>.<element>}. */
	public static String passiveKey(QiElement element) { return "passive." + DefyingTheHeavens.MOD_ID + "." + element.name().toLowerCase(java.util.Locale.ROOT); }
	public static final String KEY_SPELL_WHEEL = "key." + DefyingTheHeavens.MOD_ID + ".spell_wheel";
	public static final String KEY_CAST_SPELL = "key." + DefyingTheHeavens.MOD_ID + ".cast_spell";
	public static final String MSG_METHOD_LEARNED = k("message.method.learned");
	public static final String MSG_METHOD_LEARNED_AT = k("message.method.learned_at");
	public static final String MSG_METHOD_PRACTISED = k("message.method.practised");
	public static final String MSG_GHOST_QI_SPENT = k("message.method.ghost_qi_spent");
	public static final String MSG_STELE_TEACHES = k("message.stele.teaches");
	public static final String MSG_STELE_KNOWN = k("message.stele.known");
	public static final String MSG_STELE_READING = k("message.stele.reading");
	public static final String MSG_SLIP_KNOWN = k("message.slip.known");
	public static final String ITEM_GHOST_JADE_SLIP_OF = "item." + DefyingTheHeavens.MOD_ID + ".ghost_jade_slip.of";
	public static final String TOOLTIP_GHOST_JADE_SLIP = k("tooltip.ghost_jade_slip");
	public static final String MSG_SPELL_NONE_READY = k("message.spell.none_ready");
	public static final String MSG_SPELL_NOT_HERE = k("message.spell.not_here");
	public static final String MSG_SPELL_COOLING = k("message.spell.cooling");
	public static final String MSG_SPELL_NO_QI = k("message.spell.no_qi");
	public static final String MSG_SPELL_READIED = k("message.spell.readied");
	public static final String MSG_GHOST_FIRE_ARMED = k("message.spell.ghost_fire_armed");
	// The Methods tab
	public static final String METHODS_KNOWN = k("methods.known");
	public static final String METHODS_LAYER = k("methods.layer");
	public static final String METHODS_LAYER_REALM = k("methods.layer_realm");
	public static final String METHODS_PERFECTED = k("methods.perfected");
	public static final String METHODS_GHOST_PATH = k("methods.ghost_path");
	public static final String METHODS_GHOST_PATH_NONE = k("methods.ghost_path_none");
	public static final String METHODS_UNKNOWN_DEEP = k("methods.unknown_deep");
	public static final String METHODS_ACTIVE = k("methods.active");
	public static final String METHODS_UNKNOWN = k("methods.unknown");
	public static final String METHODS_GAINS = k("methods.gains");
	public static final String BTN_PRACTISE = k("methods.practise");
	public static final String METHODS_NO_QI = k("methods.no_qi");
	public static final String BTN_SPELL_LIST = k("spells.list");
	// The Spells tab and the spell wheel (held open by its key)
	public static final String SPELLS_COST = k("spells.cost");
	public static final String SPELLS_COOLDOWN = k("spells.cooldown");
	public static final String SPELLS_LOCKED = k("spells.locked");
	public static final String SPELLS_LOCKED_REALM = k("spells.locked_realm");
	/** A heaven spell not yet known: the realm and stage one's cultivation must reach for it, as well as its layer (Spell#realmNeeded). */
	public static final String SPELLS_LOCKED_STAGE = k("spells.locked_stage");
	public static final String SPELLS_READY = k("spells.ready");
	public static final String SPELLS_READIED = k("spells.readied");
	public static final String SPELLS_COOLING = k("spells.cooling");
	public static final String SPELLS_ON_WHEEL = k("spells.on_wheel");
	public static final String SPELLS_ARRANGE_HINT = k("spells.arrange_hint");
	public static final String SPELLS_COST_BOTH = k("spells.cost_both");
	public static final String SPELLS_QI_COST = k("spells.qi_cost");
	public static final String SPELLS_LOCKED_BOTH = k("spells.locked_both");
	public static final String SPELL_WHEEL_TITLE = k("spell_wheel.title");
	// The two paths (see TwoPaths): the body, the harmony, the spells of both paths, the forms
	public static final String STATS_GHOST_BODY = k("stats.ghost_body");
	public static final String STATS_HARMONY = k("stats.harmony");
	public static final String TWO_PATHS_LINE = k("two_paths.line");
	/** A seized player's body opened by the soul in it (VesselMenu): its title. */
	public static final String VESSEL_INVENTORY = k("soul.vessel_inventory");
	/** The Cultivation tab on both paths: the heaven path's line. */
	public static final String HEAVEN_PATH_LINE = k("two_paths.heaven_line");
	/**
	 * Effective cultivation (TwoPaths#effective): the line, and its names: past a stage ("%s - Quasi-%s"), past a realm's Grand
	 * Perfection ("Quasi-%s"), past the very last.
	 */
	public static final String EFFECTIVE_LINE = k("two_paths.effective");
	public static final String EFFECTIVE_QUASI_STAGE = k("two_paths.effective.quasi_stage");
	public static final String EFFECTIVE_QUASI_REALM = k("two_paths.effective.quasi_realm");
	public static final String EFFECTIVE_BEYOND = k("two_paths.effective.beyond");
	/** A Quasi- name and how far toward it ("%s (%s%%)"). */
	public static final String EFFECTIVE_TOWARD = k("two_paths.effective.toward");
	/** The heaven method practised at the very peak, on the Cultivation tab's bar: its name and layer (in short where that won't fit). */
	public static final String METHOD_BAR = k("method_bar");
	public static final String METHOD_BAR_SHORT = k("method_bar.short");
	/**
	 * The Cultivation tab on both paths, each path a column of its own (half the width): the bars' shortest words (the cultivation
	 * without its label, the layer without the method's name), and the ghost path's column's line when there is no ghostly qi.
	 */
	public static final String CULTIVATION_SHORT = k("cultivation.short");
	public static final String CULTIVATION_MAX_SHORT = k("cultivation_max.short");
	public static final String METHOD_BAR_LAYERS = k("method_bar.layers");
	public static final String GHOST_COLUMN_NO_QI = k("two_paths.ghost.no_qi");
	/**
	 * The ghost path's own bar (2026-10-19, GhostMethods): its column's pace line, what stands next on it, the Cultivation tab's switch
	 * between the two qi, its layers settling, its gate, and the Netherworld's tribulation (TribulationManager, the ghost path's).
	 */
	public static final String GHOST_COLUMN_PACE = k("two_paths.ghost.pace");
	public static final String GHOST_COLUMN_PACE_SHORT = k("two_paths.ghost.pace_short");
	/** The ghost path cultivated with no foundation known (GhostMethods#NO_METHOD_FACTOR), as the heaven column's METHOD_LINE_NONE. */
	public static final String GHOST_COLUMN_PACE_NONE = k("two_paths.ghost.pace_none");
	/** Either path's column, its qi not drawn in. */
	public static final String COLUMN_RESTING = k("two_paths.resting");
	public static final String GHOST_BOTTLENECK = k("two_paths.ghost.bottleneck");
	/** At a path's bottleneck with the other's qi drawn in: the Breakthrough button is the other's, so switch first. */
	public static final String GHOST_BOTTLENECK_SWITCH = k("two_paths.ghost.bottleneck_switch");
	public static final String BOTTLENECK_SWITCH = k("two_paths.heaven.bottleneck_switch");
	public static final String GHOST_PINNACLE = k("two_paths.ghost.pinnacle");
	public static final String GHOST_GATE_NONE = k("two_paths.ghost.gate_none");
	public static final String BTN_PATH_HEAVEN = k("button.path.heaven");
	public static final String BTN_PATH_GHOST = k("button.path.ghost");
	public static final String MSG_PATH_HEAVEN = k("message.path.heaven");
	public static final String MSG_PATH_GHOST = k("message.path.ghost");
	public static final String MSG_PATH_NO_GHOST = k("message.path.no_ghost");
	public static final String MSG_GHOST_LAYER = k("message.ghost_method.layer");
	public static final String MSG_GHOST_LAYER_SPELL = k("message.ghost_method.layer_spell");
	public static final String MSG_GHOST_PATH_RISES = k("message.ghost_path.rises");
	/** One of the dead begins the ghost path with no method (GhostMethods#begin). */
	public static final String MSG_GHOST_PATH_BEGINS = k("message.ghost_path.begins");
	public static final String MSG_GHOST_BOTTLENECK = k("message.ghost_path.bottleneck");
	public static final String MSG_GHOST_TRIB_START = k("message.ghost_tribulation.start");
	public static final String MSG_GHOST_TRIB_START_SINGLE = k("message.ghost_tribulation.start_single");
	public static final String MSG_GHOST_BREAKTHROUGH = k("message.ghost_tribulation.survived");
	public static final String MSG_GHOST_TRIB_FALL = k("message.ghost_tribulation.fall");
	public static final String MSG_GHOST_TRIB_FLED = k("message.ghost_tribulation.fled");
	public static final String TRIB_HUD_GHOST = k("hud.ghost_tribulation");
	public static final String METHODS_GHOST_RESTING = k("methods.ghost_resting");
	public static final String METHODS_GHOST_NEXT = k("methods.ghost_next");
	public static final String METHODS_GHOST_REACH = k("methods.ghost_reach");
	/** The Netherworld's tribulation's yin lightning (ModDamageTypes): its bolt, and the yin that goes through armour. */
	public static final String DEATH_YIN_LIGHTNING = "death.attack." + DefyingTheHeavens.MOD_ID + ".yin_lightning";
	public static final String DEATH_YIN_TRIBULATION = "death.attack." + DefyingTheHeavens.MOD_ID + ".yin_tribulation";
	public static final String MSG_SPELL_NO_BOTH = k("message.spell.no_both");
	public static final String MSG_SPELL_BOTH_PATHS = k("message.spell.both_paths");
	/** A form's name: this and its id (TwoPaths.Form). */
	public static final String FORM_PREFIX = k("form.");
	public static final String MSG_FORMS_OPEN = k("message.form.open");
	public static final String MSG_FORMS_KEPT = k("message.form.kept");
	/** A mortal's closed meridians, alive (MSG_METHOD_MERIDIANS) or dead: no ghost method taken up or cultivated (GhostMethods#refusal). */
	public static final String MSG_METHOD_MERIDIANS = k("message.method.meridians");
	public static final String MSG_METHOD_MERIDIANS_DEAD = k("message.method.meridians_dead");
	public static final String METHODS_GHOST_PATH_SEALED = k("methods.ghost_path_sealed");
	public static final String MSG_TRUE_FORM = k("message.form.true");
	public static final String MSG_FORM_TAKEN = k("message.form.taken");
	public static final String JOURNAL_FORMS = k("journal.two_forms");
	public static final String JOURNAL_FORMS_TEXT = k("journal.two_forms.text");
	public static final String SPELL_WHEEL_HINT = k("spell_wheel.hint");
	public static final String SPELL_WHEEL_EMPTY = k("spell_wheel.empty");
	public static final String HUD_READIED_SPELL = k("hud.readied_spell");
	// The Netherworld's inhabitants and its herbs, items and pills (phase 5)
	public static String officialName(String id) { return k("official." + id); }
	public static final String TOOLTIP_PEACH_WOOD_SWORD = k("tooltip.peach_wood_sword");
	public static final String MSG_PILL_NO_METHOD = k("message.pill.no_method");
	public static final String MSG_PILL_YANG = k("message.pill.yang");
	public static final String MSG_PILL_YIN = k("message.pill.yin");
	public static final String PILL_EFFECT_YANG = k("pill.effect.yang");
	public static final String PILL_EFFECT_YIN = k("pill.effect.yin");
	/** A Longevity Pill: the years it adds (and how much less it works for those taken before it this life); eaten. */
	public static final String PILL_EFFECT_LONGEVITY = k("pill.effect.longevity");
	public static final String PILL_EFFECT_LONGEVITY_LESS = k("pill.effect.longevity_less");
	public static final String MSG_PILL_LONGEVITY = k("message.pill.longevity");
	/** A Longevity Pill refused by a True Immortal (Lifespan#endless): it has no end to its years to add to. */
	public static final String MSG_PILL_LONGEVITY_ENDLESS = k("message.pill.longevity_endless");
	/** A Rebirth Pill (RebirthPillItem): its tooltip, the warning that it is eaten only sneaking, and its working. */
	public static final String TOOLTIP_REBIRTH_PILL = k("tooltip.rebirth_pill");
	public static final String TOOLTIP_REBIRTH_PILL_WARN = k("tooltip.rebirth_pill.warn");
	public static final String MSG_REBIRTH_CONFIRM = k("message.rebirth_pill.confirm");
	public static final String MSG_REBIRTH = k("message.rebirth_pill");
	/** ...refused by an Eternal Reincarnation Body (RebirthPillItem#refuses). */
	public static final String MSG_REBIRTH_ETERNAL = k("message.rebirth_pill.eternal");
	public static final String MSG_TERRACE = k("message.terrace");
	public static final String TERRACE_PLACE = k("terrace.place");
	public static final String JOURNAL_TERRACE = k("journal.terrace");
	public static final String JOURNAL_TERRACE_TEXT = k("journal.terrace.text");
	public static final String JOURNAL_METHOD = k("journal.method");
	public static final String JOURNAL_METHOD_TEXT = k("journal.method.text");
	public static final String DIMENSION_OVERWORLD = k("dimension.overworld");
	public static final String DIMENSION_NETHER = k("dimension.nether");
	public static final String DIMENSION_END = k("dimension.end");
	public static final String DIMENSION_UPPER_REALM = k("dimension.upper_realm");
	public static final String DIMENSION_NETHERWORLD = k("dimension.netherworld");
	public static final String MSG_SOUP = k("message.soup");
	public static final String MSG_SOUP_AGAIN = k("message.soup.again");
	public static final String MSG_SOUP_LIVING = k("message.soup.living");
	public static final String TALK_SOUP = k("talk.soup");
	public static final String TALK_SOUP_LIVING = k("talk.soup.living");
	public static final String TALK_TEACH_LEARN = k("talk.teach.learn");
	public static final String TALK_TEACH_PROGRESS = k("talk.teach.progress");
	// The Ghost-Quelling Talisman
	public static final String TOOLTIP_GHOST_QUELLING_TALISMAN = k("tooltip.ghost_quelling_talisman");
	public static final String MSG_QUELLED = k("message.quelled");
	public static final String MSG_QUELLED_OTHER = k("message.quelled.other");
	public static final String MSG_QUELLED_HELD = k("message.quelled.held");
	// The Astral Realm
	public static final String DIMENSION_ASTRAL_VOID = k("dimension.astral_void");
	public static final String DIMENSION_ASTRAL_SURFACES = k("dimension.astral_surfaces");
	public static final String MSG_ASTRAL_TOO_WEAK = k("message.astral.too_weak");
	public static final String MSG_ASTRAL_ENTER = k("message.astral.enter");
	public static final String MSG_ASTRAL_LAND = k("message.astral.land");
	public static final String MSG_ASTRAL_HOME = k("message.astral.home");
	public static final String MSG_ASTRAL_LEAVE = k("message.astral.leave");
	public static final String MSG_ASTRAL_HOLD = k("message.astral.hold");
	public static final String MSG_ASTRAL_STAR_HEAT = k("message.astral.star_heat");
	public static final String VESTIGE_NAME = k("vestige.name");
	public static final String ASTRAL_QI_LINE = k("astral_qi_line");
	public static final String STATS_QI_GATHER_PLACE = k("stats.qi_gather_place");
	public static final String MSG_VESTIGE_NEAR = k("message.vestige.near");

	/** A planet's kind, as told (verdant, volcanic, ...). */
	public static String astralKind(AstralPlanets.Kind kind) { return k("astral.kind." + kind.getId()); }

	private ModLang() {}
}
