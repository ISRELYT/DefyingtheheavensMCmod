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
	/** A meditation in the air ended by its flight giving out, its qi run dry (MeditationManager). */
	public static final String MSG_MEDITATION_FELL = k("message.meditation_fell");
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
	/** A breakthrough into True Immortal or past it, outside the Immortal Realm (PlayerCultivation#lockLine). */
	public static final String MSG_IMMORTAL_REALM_LOCKED = k("message.immortal_realm.locked");
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
	public static final String NPC_GREET_DEVILISH = k("npc.greet.devilish");
	public static final String NPC_GREET_HOSTILE = k("npc.greet.hostile");
	public static final String NPC_GREET_SECLUDED = k("npc.greet.secluded");
	/** Something a cultivator says of its own accord (NpcLines: {@code key} is the line's key and variant number). */
	public static String npcSay(String key) { return k("npc.say." + key); }
	/** A form of address a cultivator calls a player by (NpcAddress): "senior", "far_above.devilish"... */
	public static String address(String id) { return k("address." + id); }

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
	/** A Dao Duel (SparSystem.Terms#DUEL): not on offer, too weak for its weapon, no stake held (a refusal, and its note), begun. */
	public static final String SPAR_NO_DUEL = k("spar.no_duel");
	public static final String SPAR_DUEL_TOO_WEAK = k("spar.duel_too_weak");
	public static final String SPAR_DUEL_NO_STAKE = k("spar.duel_no_stake");
	public static final String SPAR_DUEL_STAKE = k("spar.duel_stake");
	public static final String SPAR_DUEL_BEGINS = k("spar.duel_begins");
	/** A sect's people lead the way to their sparring ground first (SparSystem#LEAD_TICKS): setting off, on the way, waiting there. */
	public static final String SPAR_FOLLOW = k("spar.follow");
	public static final String SPAR_LEADING = k("spar.leading");
	public static final String SPAR_WAITING = k("spar.waiting");
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
	/** Immortal Jade's line (SpiritStoneItem.Grade#IMMORTAL_JADE). */
	public static final String STONE_JADE = k("stone.jade");
	// Demon cores (DemonCoreItem): "Foundation Building Demon Core", its rank, element, whence, and what it is for
	public static final String CORE_NAME = k("demon_core.name");
	public static final String CORE_RANK = k("demon_core.rank");
	public static final String CORE_ELEMENT = k("demon_core.element");
	public static final String CORE_FROM = k("demon_core.from");
	public static final String CORE_USE = k("demon_core.use");
	// A demon race's blood (DemonBloodItem, DemonBlood): "Core Formation Tiger Blood", what it does, why a drink won't take, what it did
	public static final String BLOOD_NAME = k("demon_blood.name");
	public static final String BLOOD_UNKNOWN = k("demon_blood.unknown");
	public static final String BLOOD_USE = k("demon_blood.use");
	public static final String BLOOD_TURN_TIP = k("demon_blood.turn_tip");
	public static final String MSG_BLOOD_UNKNOWN = k("message.blood.unknown");
	public static final String MSG_BLOOD_ELSEWHERE = k("message.blood.elsewhere");
	public static final String MSG_BLOOD_DEAD = k("message.blood.dead");
	public static final String MSG_BLOOD_MORTAL = k("message.blood.mortal");
	public static final String MSG_BLOOD_OTHER = k("message.blood.other");
	public static final String MSG_BLOOD_HELD = k("message.blood.held");
	public static final String MSG_BLOOD_WARN = k("message.blood.warn");
	public static final String MSG_BLOOD_CORE = k("message.blood.core");
	public static final String MSG_BLOOD_AWAKENS = k("message.blood.awakens");
	public static final String MSG_BLOOD_PURITY = k("message.blood.purity");
	public static final String MSG_BLOOD_TURNED = k("message.blood.turned");
	public static final String JOURNAL_HALF_BLOOD = k("journal.half_blood");
	public static final String JOURNAL_HALF_BLOOD_TEXT = k("journal.half_blood.text");
	public static final String JOURNAL_TURNED = k("journal.turned");
	public static final String JOURNAL_TURNED_TEXT = k("journal.turned.text");
	/** The Cultivation tab: a half-blood's purity; the Demon Path held at a realm's gate for want of blood. */
	public static final String HALF_BLOOD_LINE = k("half_blood.line");
	public static final String DEMON_HELD_BY_BLOOD = k("demon_held_by_blood");
	public static final String CMD_BLOODLINE_HALF = k("command.bloodline_half");
	// A sect's guest pass (SectPassItem): "Guest Pass of the Azure Cloud Sect", what it does, how long it is good for, whose it is, taken back
	public static final String PASS_NAME = k("sect_pass.name");
	public static final String PASS_BLANK = k("sect_pass.blank");
	public static final String PASS_USE = k("sect_pass.use");
	public static final String PASS_LEFT = k("sect_pass.left");
	public static final String PASS_LAPSED = k("sect_pass.lapsed");
	public static final String PASS_BOUND = k("sect_pass.bound");
	public static final String MSG_PASS_REVOKED = k("message.sect_pass.revoked");
	// A Sacred Vessel (SacredVesselItem): what it does in hand, and what it is
	public static final String SACRED_VESSEL_USE = k("sacred_vessel.use");
	public static final String SACRED_VESSEL_LORE = k("sacred_vessel.lore");
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
	// Its concealment (Concealments), the core's second function
	public static final String CORE_TAB_BARRIER = k("core.tab.barrier");
	public static final String CORE_TAB_VEIL = k("core.tab.veil");
	public static final String CORE_VEIL_RADIUS = k("core.veil.radius");
	public static final String CORE_VEIL_ON = k("core.veil.on");
	public static final String CORE_VEIL_OFF = k("core.veil.off");
	public static final String CORE_VEIL_UPKEEP = k("core.veil.upkeep");
	public static final String CORE_UPKEEP_BOTH = k("core.upkeep_both");
	public static final String CORE_VEIL_STRENGTH = k("core.veil.strength");
	public static final String CORE_VEIL_STRENGTH_MORTAL = k("core.veil.strength_mortal");
	public static final String CORE_VEIL_RAISED = k("core.veil.raised");
	public static final String CORE_VEIL_LOWERED = k("core.veil.lowered");
	public static final String CORE_MASTERY = k("core.mastery");
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
	/** As the body one's soul is in (SectMembership#leaveAsBody, #banish): it leaves; one cast out by the master's or an elder's word. */
	public static final String MSG_SECT_EXPELLED_BANISHED = k("message.sect.expelled.banished");
	public static final String MSG_SECT_BODY_LEFT = k("message.sect.body_left");
	public static final String MSG_SECT_BANISHED = k("message.sect.banished");
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
	/** A disciple's Contribution in their sect (its mission board: SectMissions), all they have earned, the missions they hold of how many. */
	public static final String SECT_TAB_CONTRIBUTION = k("sect_tab.contribution");
	public static final String SECT_TAB_LEAVE = k("sect_tab.leave");
	public static final String SECT_TAB_PLAYER = k("sect_tab.player");
	public static final String SECT_TAB_OFFLINE = k("sect_tab.offline");
	/** The Sect tab's buttons for a soul in a sect member's body: leave as it; a senior's, cast one below out; and the second click's. */
	public static final String SECT_TAB_BODY = k("sect_tab.body");
	public static final String SECT_TAB_LEAVE_BUTTON = k("sect_tab.leave_button");
	public static final String SECT_TAB_BANISH_BUTTON = k("sect_tab.banish_button");
	public static final String SECT_TAB_SURE = k("sect_tab.sure");
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
	/** In space, the way home (client TribulationHud#drawWayHome): the Overworld's distance, ahead or about, above, below. */
	public static final String HUD_SPACE_HOME = k("hud.space_home");
	public static final String HUD_SPACE_HOME_ABOVE = k("hud.space_home.above");
	public static final String HUD_SPACE_HOME_BELOW = k("hud.space_home.below");
	/** On a planet just come down onto: how to leave it (fly up past this height, out into space). */
	public static final String HUD_PLANET_WAY_OUT = k("hud.planet_way_out");
	public static final String MSG_SPACE_NO_WAY_HOME = k("message.astral.no_way_home");
	public static final String MSG_OLD_AGE = k("message.lifespan.old_age");
	/** Burn Lifespan (BurnLifespan): lit, put out, gone out by itself near the end, refused with too few years left. */
	public static final String MSG_BURN_ON = k("message.burn_lifespan.on");
	/** ...lit by a True Immortal (Lifespan#endless): its years have no end, so it burns for nothing. */
	public static final String MSG_BURN_ON_ENDLESS = k("message.burn_lifespan.on_endless");
	public static final String MSG_BURN_OFF = k("message.burn_lifespan.off");
	public static final String MSG_BURN_OUT = k("message.burn_lifespan.out");
	public static final String MSG_BURN_TOO_FEW = k("message.burn_lifespan.too_few");
	/** Immortal essence (ImmortalEssence, phase 2): lit, settled, spent, too little left to light. */
	public static final String MSG_ESSENCE_ON = k("message.immortal_essence.on");
	public static final String MSG_ESSENCE_OFF = k("message.immortal_essence.off");
	public static final String MSG_ESSENCE_SPENT = k("message.immortal_essence.spent");
	public static final String MSG_ESSENCE_TOO_LITTLE = k("message.immortal_essence.too_little");
	/**
	 * The Celestial Court (CelestialCourt, phase 2): merit for a minute's watch at the open Gate and for a devilish immortal slain, a rank
	 * risen; its ranks ("captain"...); its token's lines; the Sect tab's line of it. (Its King and Venerables are named by their own
	 * realms since phase 3: Realm#PROFOUND_SOVEREIGN, Realm#TAIYI_GOLDEN_IMMORTAL.)
	 */
	public static final String MSG_COURT_MERIT_WATCH = k("message.court.merit_watch");
	public static final String MSG_COURT_MERIT_SLAIN = k("message.court.merit_slain");
	public static final String MSG_COURT_RANK_UP = k("message.court.rank_up");
	public static String courtRank(String id) { return k("court.rank." + id); }
	public static final String TOKEN_EARNED = k("court.token.earned");
	public static final String TOKEN_SPENT = k("court.token.spent");
	public static final String SECT_TAB_COURT = k("sect_tab.court");
	public static final String SECT_TAB_COURT_NEXT = k("sect_tab.court_next");
	/** A Court line on the trading screen not yet open to you (CelestialCourt#offers): the rank it wants, and yours (or none). */
	public static final String TRADE_LOCKED_RANK = k("talk.trade_locked_rank");
	public static final String TRADE_NOT_OF_COURT = k("talk.trade_not_of_court");
	/** A talk topic gated by Court rank (a Venerable's method: TalkSystem): the rank it wants. */
	public static final String TALK_NEEDS_COURT = k("talk.needs_court");
	/**
	 * The primordial beasts (PrimordialBeast, PrimordialBeasts, phase 2): each come to its lair ("kun_peng"...), the Kun broken from the
	 * clouds as the Peng, the Phoenix burnt to an ember and reborn, one slain; the Court's merit for one; where a Venerable says each keeps,
	 * or that it was slain and when the next comes.
	 */
	public static String primordialComes(String id) { return k("message.primordial.comes." + id); }
	public static final String MSG_PRIMORDIAL_BREACH = k("message.primordial.breach");
	public static final String MSG_PRIMORDIAL_EMBER = k("message.primordial.ember");
	public static final String MSG_PRIMORDIAL_REBORN = k("message.primordial.reborn");
	public static final String MSG_PRIMORDIAL_SLAIN = k("message.primordial.slain");
	public static final String MSG_COURT_MERIT_PRIMORDIAL = k("message.court.merit_primordial");
	public static String primordialWhere(String id) { return k("talk.line.primordial." + id); }
	public static String primordialGone(String id) { return k("talk.line.primordial." + id + ".gone"); }
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
	public static final String WISH_GIVE_OWED = k("wish.give_owed");
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

	/** Bloodlines (Bloodline): each race's name, its colourings', and its lore (on the picker's card and in the journal). */
	public static String bloodlineKey(String id) { return "bloodline." + DefyingTheHeavens.MOD_ID + "." + id; }
	public static String bloodlineVariantKey(String id, String variant) { return "bloodline." + DefyingTheHeavens.MOD_ID + "." + id + "." + variant; }
	public static String bloodlineLoreKey(String id) { return "bloodline." + DefyingTheHeavens.MOD_ID + "." + id + ".lore"; }
	/** The race picker, the first time one joins. */
	public static final String BLOODLINE_TITLE = k("bloodline.title");
	public static final String BLOODLINE_INTRO = k("bloodline.intro");
	public static final String BLOODLINE_COLOURING = k("bloodline.colouring");
	public static final String BLOODLINE_OWN_SKIN = k("bloodline.own_skin");
	public static final String BLOODLINE_CORE = k("bloodline.core");
	public static final String BLOODLINE_CONFIRM = k("bloodline.confirm");
	public static final String BLOODLINE_FINAL = k("bloodline.final");
	public static final String MSG_BLOODLINE_BEAST = k("message.bloodline_beast");
	public static final String MSG_BLOODLINE_HUMAN = k("message.bloodline_human");
	public static final String JOURNAL_BLOODLINE = k("journal.bloodline");
	public static final String CMD_BLOODLINE = k("command.bloodline");
	public static final String CMD_BLOODLINE_HUMAN = k("command.bloodline_human");
	public static final String CMD_BLOODLINE_NONE = k("command.bloodline_none");
	public static final String CMD_BLOODLINE_ASKED = k("command.bloodline_asked");
	/** The Demon Path (DemonPath): born with it, its stages and inheritance, its bottleneck and tribulation, the switch to it, its column. */
	public static final String MSG_DEMON_CORE = k("message.demon_core");
	public static final String JOURNAL_DEMON_PATH = k("journal.demon_path");
	public static final String JOURNAL_DEMON_PATH_TEXT = k("journal.demon_path.text");
	public static final String MSG_DEMON_STAGE_UP = k("message.demon_stage_up");
	public static final String MSG_DEMON_INHERITS = k("message.demon_inherits");
	public static final String MSG_DEMON_BOTTLENECK = k("message.demon_bottleneck");
	public static final String MSG_DEMON_TRIB_START = k("message.demon_trib_start");
	public static final String MSG_DEMON_TRIB_START_SINGLE = k("message.demon_trib_start_single");
	public static final String MSG_DEMON_BREAKTHROUGH = k("message.demon_breakthrough");
	public static final String MSG_DEMON_TRIB_FALL = k("message.demon_trib_fall");
	public static final String MSG_DEMON_TRIB_FLED = k("message.demon_trib_fled");
	public static final String MSG_PATH_DEMON = k("message.path_demon");
	public static final String BTN_PATH_DEMON = k("button.path_demon");
	public static final String METHODS_PAGE_DEMON = k("methods.page.demon");
	public static final String DEMON_COLUMN_PACE = k("demon_column.pace");
	public static final String DEMON_COLUMN_PACE_SHORT = k("demon_column.pace_short");
	public static final String DEMON_BOTTLENECK = k("demon_bottleneck");
	public static final String DEMON_BOTTLENECK_SWITCH = k("demon_bottleneck_switch");
	public static final String DEMON_PINNACLE = k("demon_pinnacle");
	public static final String STATS_DEMON_BODY = k("stats.demon_body");
	/** The races' gifts (RaceGifts). */
	public static final String MSG_SHED_SKIN = k("message.shed_skin");
	public static final String MSG_GIFT_COOLDOWN = k("message.gift_cooldown");
	public static final String MSG_GIFT_NO_QI = k("message.gift_no_qi");
	public static final String MSG_ROARED = k("message.roared");
	public static final String MSG_ILLUSION = k("message.illusion");
	public static final String MSG_ILLUSION_BROKEN = k("message.illusion_broken");
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
	public static final String HUD_SOUL_TETHER_ENDLESS = k("hud.soul.tether_endless");
	public static final String HUD_SOUL_INSIDE = k("hud.soul.inside");
	public static final String HUD_SOUL_OWN_QI_LABEL = k("hud.soul.own_qi_label");
	/** A True Immortal's immortal essence bar (ImmortalEssence). */
	public static final String HUD_ESSENCE_LABEL = k("hud.essence_label");
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
	public static final String GHOST_QI_TEEMING = k("netherworld.ghost_qi.teeming");
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
	// The storage formations (StorageFormations, StorageFormationItem) and their screen; an old Storage Ring's word
	public static String storageTier(String id) { return "storage_tier." + DefyingTheHeavens.MOD_ID + "." + id; }
	public static final String STORAGE_RING_OLD = k("tooltip.storage_ring.old");
	public static final String STORAGE_TOOLTIP_STACKS = k("tooltip.storage.stacks");
	public static final String STORAGE_TOOLTIP_BOUNDLESS = k("tooltip.storage.boundless");
	public static final String STORAGE_TOOLTIP_SEAL = k("tooltip.storage.seal");
	public static final String STORAGE_TOOLTIP_HOLDS = k("tooltip.storage.holds");
	public static final String STORAGE_TOOLTIP_USE = k("tooltip.storage.use");
	public static final String STORAGE_USED = k("storage.used");
	public static final String STORAGE_USED_BOUNDLESS = k("storage.used_boundless");
	public static final String STORAGE_TITLE = k("storage.title");
	public static final String STORAGE_SEARCH = k("storage.search");
	public static final String STORAGE_EMPTY = k("storage.empty");
	public static final String STORAGE_NO_MATCH = k("storage.no_match");
	public static final String STORAGE_REFINE = k("storage.refine");
	public static final String STORAGE_REFINE_TO = k("storage.refine_to");
	public static final String STORAGE_REFINE_STACKS = k("storage.refine_stacks");
	public static final String STORAGE_REFINE_ENDLESS = k("storage.refine_endless");
	public static final String STORAGE_REFINE_REALM = k("storage.refine_realm");
	public static final String STORAGE_REFINE_MASTERY = k("storage.refine_mastery");
	public static final String STORAGE_REFINE_LAST = k("storage.refine_last");
	public static final String STORAGE_COUNT = k("storage.count");
	public static final String STORAGE_TRUST = k("storage.trust");
	public static final String STORAGE_TRUST_TIP = k("storage.trust_tip");
	public static final String STORAGE_TRUST_NAME = k("storage.trust_name");
	public static final String STORAGE_TRUST_ADD = k("storage.trust_add");
	public static final String STORAGE_TRUST_NONE = k("storage.trust_none");
	public static final String MSG_STORAGE_LAST_TIER = k("message.storage.last_tier");
	public static final String MSG_STORAGE_REALM = k("message.storage.realm");
	public static final String MSG_STORAGE_MASTERY = k("message.storage.mastery");
	public static final String MSG_STORAGE_COST = k("message.storage.cost");
	public static final String MSG_STORAGE_REFINED = k("message.storage.refined");
	public static final String MSG_STORAGE_NOT_YOURS = k("message.storage.not_yours");
	public static final String MSG_STORAGE_NOT_EMPTY = k("message.storage.not_empty");
	public static final String MSG_STORAGE_SEALED = k("message.storage.sealed");
	public static final String MSG_STORAGE_MENDED = k("message.storage.mended");
	public static final String MSG_STORAGE_FORCE_BEGIN = k("message.storage.force_begin");
	public static final String MSG_STORAGE_FORCING = k("message.storage.forcing");
	public static final String MSG_STORAGE_FORCED_BY_ANOTHER = k("message.storage.forced_by_another");
	public static final String MSG_STORAGE_FORCE_ABANDONED = k("message.storage.force_abandoned");
	public static final String MSG_STORAGE_FORCE_DONE = k("message.storage.force_done");
	public static final String MSG_STORAGE_OWNER_FORCED = k("message.storage.owner_forced");
	public static final String MSG_STORAGE_OWNER_BROKEN = k("message.storage.owner_broken");
	public static final String MSG_STORAGE_PLACE = k("message.storage.place");
	public static final String MSG_STORAGE_RING_UNFOLDED = k("message.storage.ring_unfolded");
	// The Artifact Forge and its artifacts (ArtifactForgeBlockEntity, ArtifactRecipes, Artifacts and the artifact items)
	public static String artifactGrade(String id) { return "artifact_grade." + DefyingTheHeavens.MOD_ID + "." + id; }
	public static String artifactQuality(String id) { return "artifact_quality." + DefyingTheHeavens.MOD_ID + "." + id; }
	public static final String ARTIFACT_QUALITY_NAME = k("artifact.quality_name");
	public static final String ARTIFACT_GRADE_LINE = k("tooltip.artifact.grade");
	public static final String ARTIFACT_QUALITY_LINE = k("tooltip.artifact.quality");
	public static final String FLYING_SWORD_PERK = k("tooltip.artifact.flying_sword");
	public static final String SABER_PERK = k("tooltip.artifact.saber");
	public static final String SPEAR_PERK = k("tooltip.artifact.spear");
	public static final String GLAIVE_PERK = k("tooltip.artifact.glaive");
	public static final String BANNER_NAMED = k("artifact.banner_named");
	public static final String BANNER_PERK = k("tooltip.artifact.banner");
	public static final String BANNER_NO_ELEMENT = k("tooltip.artifact.banner_no_element");
	public static final String GOURD_HOLDS = k("tooltip.artifact.gourd_holds");
	public static final String GOURD_USE = k("tooltip.artifact.gourd_use");
	public static final String GOURD_EMPTY = k("message.gourd.empty");
	public static final String GOURD_POOL_FULL = k("message.gourd.pool_full");
	public static final String GOURD_FULL = k("message.gourd.full");
	public static final String GOURD_NO_QI = k("message.gourd.no_qi");
	public static final String GOURD_POURED = k("message.gourd.poured");
	public static final String GOURD_DRUNK = k("message.gourd.drunk");
	public static final String JADE_PERK = k("tooltip.artifact.jade");
	public static final String FORGE_TOOLTIP = k("tooltip.artifact_forge");
	public static final String FORGE_ADDED = k("message.forge.added");
	public static final String FORGE_REJECTED = k("message.forge.rejected");
	public static final String FORGE_FULL = k("message.forge.full");
	public static final String FORGE_READY = k("message.forge.ready");
	public static final String FORGE_EMPTY = k("message.forge.empty");
	public static final String FORGE_CONTENTS = k("message.forge.contents");
	public static final String FORGE_NEEDS_HEAT = k("message.forge.needs_heat");
	public static final String FORGE_BEGUN = k("message.forge.begun");
	public static final String FORGE_WORKING = k("message.forge.working");
	public static final String FORGE_STALLED = k("message.forge.stalled");
	public static final String FORGE_FAILED = k("message.forge.failed");
	public static final String FORGE_DONE = k("message.forge.done");
	public static final String FORGE_EMPTIED = k("message.forge.emptied");
	public static final String FORGE_NEEDS_CORE = k("forge.needs_core");
	public static final String FORGE_NEEDS_TOKEN = k("forge.needs_token");
	public static final String FORGE_NEEDS_ELEMENT_CORE = k("forge.needs_element_core");
	// The weapons made of their element (ElementalWeaponItem)
	public static final String ELEMENTAL_SHARE = k("tooltip.artifact.elemental_share");
	public static final String FLAME_BLADE_PERK = k("tooltip.artifact.flame_blade");
	public static final String TIDE_SPEAR_PERK = k("tooltip.artifact.tide_spear");
	public static final String VINE_WHIP_PERK = k("tooltip.artifact.vine_whip");
	public static final String MOUNTAIN_HAMMER_PERK = k("tooltip.artifact.mountain_hammer");
	public static final String GOLDEN_SWORD_PERK = k("tooltip.artifact.golden_light_sword");
	public static final String ART_FLAME_ARC = k("tooltip.artifact.art.flame_arc");
	public static final String ART_SURGING_TIDE = k("tooltip.artifact.art.surging_tide");
	public static final String ART_BINDING_LASH = k("tooltip.artifact.art.binding_lash");
	public static final String ART_MOUNTAIN_QUAKE = k("tooltip.artifact.art.mountain_quake");
	public static final String ART_GOLDEN_CRESCENT = k("tooltip.artifact.art.golden_crescent");
	public static final String ART_COST = k("tooltip.artifact.art.cost");
	public static final String ART_NO_QI = k("message.artifact.art_no_qi");
	// The legendary weapons (Legend, LegendaryWeaponItem, Legends, LeiZhen): a legend's name is its item's own
	public static String legendName(String id) { return "item." + DefyingTheHeavens.MOD_ID + "." + id; }
	public static String legendLore(String id) { return k("tooltip.legend.lore." + id); }
	public static String leiZhenRave(int line) { return k("message.lei_zhen.rave." + line); }
	public static final String LEGEND_HOLLOW_LINE = k("tooltip.legend.hollow");
	public static final String LEGEND_WIELDED_FROM = k("tooltip.legend.wielded_from");
	public static final String LEGEND_UNBONDED = k("tooltip.legend.unbonded");
	public static final String LEGEND_BONDED_TO = k("tooltip.legend.bonded_to");
	public static final String LEGEND_STAGE = k("tooltip.legend.stage");
	public static final String LEGEND_GANJIANG_POWER = k("tooltip.legend.ganjiang");
	public static final String LEGEND_MOYE_POWER = k("tooltip.legend.moye");
	public static final String LEGEND_PAIR_POWER = k("tooltip.legend.pair");
	public static final String LEGEND_DRAGONS = k("tooltip.legend.twin_dragons");
	public static final String LEGEND_SPEAR_POWER = k("tooltip.legend.spear");
	public static final String LEGEND_SPEAR_DRINKS = k("tooltip.legend.spear_drinks");
	public static final String LEGEND_SPEAR_DRUNK = k("tooltip.legend.spear_drunk");
	public static final String LEGEND_BRAND = k("tooltip.legend.heavens_brand");
	public static final String LEGEND_HOW_TO_BOND = k("tooltip.legend.how_to_bond");
	public static final String LEGEND_HOW_TO_TAME = k("tooltip.legend.how_to_tame");
	public static final String LEGEND_TOO_HEAVY = k("message.legend.too_heavy");
	public static final String LEGEND_AWAKENS = k("message.legend.awakens");
	public static final String LEGEND_BONDING = k("message.legend.bonding");
	public static final String LEGEND_BONDED = k("message.legend.bonded");
	public static final String LEGEND_HOLLOW = k("message.legend.hollow");
	public static final String LEGEND_NOT_BONDED = k("message.legend.not_bonded");
	public static final String LEGEND_TAMING = k("message.legend.taming");
	public static final String LEGEND_BRAND_NONE = k("message.legend.brand_none");
	public static final String LEGEND_BRANDED = k("message.legend.branded");
	public static final String LEGEND_BRANDED_YOU = k("message.legend.branded_you");
	public static final String LEGEND_DRINKS_BOLT = k("message.legend.drinks_bolt");
	public static final String LEGEND_STONE_EMPTY = k("message.legend.stone_empty");
	public static final String LEGEND_TOMB_GUARDED = k("message.legend.tomb_guarded");
	public static final String LEGEND_TOMB_WAKES = k("message.legend.tomb_wakes");
	public static final String LEGEND_TOMB_SECOND = k("message.legend.tomb_second");
	public static final String LEGEND_TOMB_QUIET = k("message.legend.tomb_quiet");
	public static final String LEGEND_MOYE_RISES = k("message.legend.moye_rises");
	public static final String LEGEND_MOYE_GUARDED = k("message.legend.moye_guarded");
	public static final String LEGEND_DRAWN = k("message.legend.drawn");
	public static final String MSG_TRIB_BRANDED = k("message.tribulation.branded");
	// Phase 3b's legends: the Xuanyuan Sword and Chiyou's shade, the Sun-Shooting Bow and the Golden Crow, the Sea-Fixing Needle and Ao Guang
	public static final String LEGEND_XUANYUAN_POWER = k("tooltip.legend.xuanyuan");
	public static final String LEGEND_XUANYUAN_RIGHTEOUS = k("tooltip.legend.xuanyuan_righteous");
	public static final String LEGEND_JUDGEMENT = k("tooltip.legend.judgement");
	public static final String LEGEND_BOW_POWER = k("tooltip.legend.sun_bow");
	public static final String LEGEND_FALLING_SUN = k("tooltip.legend.falling_sun");
	public static final String LEGEND_NEEDLE_POWER = k("tooltip.legend.sea_needle");
	public static final String LEGEND_NEEDLE_KEPT = k("tooltip.legend.sea_needle_kept");
	public static final String LEGEND_AS_YOU_WILL = k("tooltip.legend.as_you_will");
	public static final String LEGEND_XUANYUAN_REFUSES = k("message.legend.xuanyuan_refuses");
	public static final String LEGEND_XUANYUAN_UNWORTHY = k("message.legend.xuanyuan_unworthy");
	public static final String LEGEND_XUANYUAN_LEAVES = k("message.legend.xuanyuan_leaves");
	public static final String LEGEND_CHIYOU_RISES = k("message.legend.chiyou_rises");
	public static final String CHIYOU_NAME = k("npc.chiyou");
	public static final String CHIYOU_GONE = k("message.legend.chiyou_gone");
	public static final String LEGEND_CROW_RISES = k("message.legend.crow_rises");
	public static final String LEGEND_CROW_GUARDED = k("message.legend.crow_guarded");
	public static final String LEGEND_CROW_FALLS = k("message.legend.crow_falls");
	public static final String LEGEND_PILLAR_STILL = k("message.legend.pillar_still");
	public static final String LEGEND_PILLAR_UNMOVED = k("message.legend.pillar_unmoved");
	public static final String LEGEND_NEEDLE_PULLED = k("message.legend.needle_pulled");
	public static final String LEGEND_AO_GUANG_RISES = k("message.legend.ao_guang_rises");
	public static final String AO_GUANG_GONE = k("message.legend.ao_guang_gone");
	public static final String LEGEND_NEEDLE_KEPT_MSG = k("message.legend.needle_kept");
	public static final String LEGEND_SUN_GATHERING = k("message.legend.sun_gathering");
	// Phase 3c's: Pangu's Axe, the Venerables' five weapons, the Dao Duel
	public static final String LEGEND_AXE_POWER = k("tooltip.legend.pangu_axe");
	public static final String LEGEND_AXE_CLEAVES = k("tooltip.legend.pangu_axe_cleaves");
	public static final String LEGEND_SPLIT = k("tooltip.legend.split_heaven_and_earth");
	public static String legendSymbolPower(String id) { return k("tooltip.legend.symbol." + id); }
	public static final String LEGEND_SYMBOL_ART = k("tooltip.legend.symbol_art");
	public static final String LEGEND_AXE_FALLS = k("message.legend.axe_falls");
	public static final String LEGEND_DUEL_HANDED = k("message.legend.duel_handed");
	// The sect mission board (SectMissions, SectExchange, MissionSlipItem; client MissionBoardScreen)
	public static final String MISSION_BOARD = k("mission.board");
	public static final String MISSION_BOARD_OF = k("mission.board_of");
	public static final String MISSION_NO_SECT = k("mission.no_sect");
	public static final String MISSION_IN_BODY = k("mission.in_body");
	public static final String MISSION_NOT_DISCIPLE = k("mission.not_disciple");
	public static final String MISSION_TAB_MISSIONS = k("mission.tab.missions");
	public static final String MISSION_TAB_EXCHANGE = k("mission.tab.exchange");
	public static final String MISSION_CONTRIBUTION = k("mission.contribution");
	public static final String MISSION_EARNED = k("mission.earned");
	public static final String MISSION_HELD = k("mission.held");
	public static final String MISSION_POSTED = k("mission.posted");
	public static final String MISSION_NOTHING = k("mission.nothing");
	public static final String MISSION_TAKEN_TAG = k("mission.taken_tag");
	public static final String MISSION_TAKE = k("mission.take");
	public static final String MISSION_ABANDON = k("mission.abandon");
	public static final String MISSION_HAND_IN = k("mission.hand_in");
	public static final String MISSION_NEW_SLIP = k("mission.new_slip");
	public static final String MISSION_GRADE_FOR = k("mission.grade_for");
	public static final String MISSION_PAYS = k("mission.pays");
	public static final String MISSION_LASTS = k("mission.lasts");
	public static final String MISSION_LEFT = k("mission.left");
	public static final String MISSION_SLAIN = k("mission.slain");
	public static final String MISSION_WATCHED = k("mission.watched");
	public static final String MISSION_HAVE = k("mission.have");
	public static final String MISSION_YEARS = k("mission.years");
	public static final String MISSION_CORE_REALM = k("mission.core_realm");
	public static final String MISSION_TIME_DAYS = k("mission.time.days");
	public static final String MISSION_TIME_HOURS = k("mission.time.hours");
	public static final String MISSION_TIME_SOON = k("mission.time.soon");
	/** How the sect speaks of a disciple's mission (SectMissions#chatSubject): a deed done ("Tester slew..."), a mission taken on. */
	public static final String MISSION_SAID_DONE = k("mission.said.done");
	public static final String MISSION_SAID_TAKEN = k("mission.said.taken");
	/** A mission's kind ("Hunt"), grade ("Hard"), heading ("Hunt: %s"), lines, the deed it makes and its undertaking, by id. */
	public static String missionKind(String id) { return k("mission.kind." + id); }
	public static String missionGrade(String id) { return k("mission.grade." + id); }
	public static String missionTitle(String id) { return k("mission.title." + id); }
	public static String missionText(String id) { return k("mission.text." + id); }
	public static String missionDeed(String id) { return k("mission.deed." + id); }
	public static String missionUndertaking(String id) { return k("mission.undertaking." + id); }
	/** A pill's grade alone ("Mid"), as a brew asks for it. */
	public static String missionPillGrade(String id) { return k("mission.pill_grade." + id); }
	public static final String MSG_MISSION_TAKEN = k("message.mission.taken");
	public static final String MSG_MISSION_HELD_FULL = k("message.mission.held_full");
	public static final String MSG_MISSION_ABANDONED = k("message.mission.abandoned");
	public static final String MSG_MISSION_DONE = k("message.mission.done");
	public static final String MSG_MISSION_LAPSED = k("message.mission.lapsed");
	public static final String MSG_MISSION_BRING = k("message.mission.bring");
	public static final String MSG_MISSION_QUARRY = k("message.mission.quarry");
	public static final String MSG_MISSION_NEW_SLIP = k("message.mission.new_slip");
	public static final String MSG_MISSION_HAS_SLIP = k("message.mission.has_slip");
	public static final String MSG_WATCH_BEGINS = k("message.mission.watch_begins");
	public static final String MSG_WATCH_WAVE = k("message.mission.watch_wave");
	public static final String MSG_WATCH_LEFT = k("message.mission.watch_left");
	public static final String MSG_WATCH_AWAY = k("message.mission.watch_away");
	public static final String MSG_WATCH_FAILED = k("message.mission.watch_failed");
	public static final String MSG_SLIP_CRUMBLES = k("message.mission.slip_crumbles");
	public static final String MSG_SLIP_POINT = k("message.mission.slip_point");
	public static final String MSG_SLIP_HERE = k("message.mission.slip_here");
	public static final String MSG_SLIP_ELSEWHERE = k("message.mission.slip_elsewhere");
	public static final String MSG_SLIP_HAND_IN = k("message.mission.slip_hand_in");
	public static final String MISSION_SLIP_OF = k("tooltip.mission_slip.of");
	public static final String MISSION_SLIP_HINT = k("tooltip.mission_slip.hint");
	public static final String SEALED_LETTER_HINT = k("tooltip.sealed_letter.hint");
	public static final String EXCHANGE_PRICE = k("mission.exchange.price");
	public static final String EXCHANGE_LEFT = k("mission.exchange.left");
	public static final String EXCHANGE_LOCKED = k("mission.exchange.locked");
	public static final String EXCHANGE_BUY = k("mission.exchange.buy");
	public static final String MSG_EXCHANGE_BOUGHT = k("message.exchange.bought");
	public static final String MSG_EXCHANGE_POOR = k("message.exchange.poor");
	public static final String MSG_EXCHANGE_SOLD_OUT = k("message.exchange.sold_out");
	public static final String MSG_EXCHANGE_LOCKED = k("message.exchange.locked");
	// The Dao Codex, the mod's guide (Codex; client CodexScreen, CodexState, CodexToast)
	public static final String KEY_CODEX = "key." + DefyingTheHeavens.MOD_ID + ".codex";
	public static final String CODEX_TITLE = k("codex.title");
	public static final String CODEX_BUTTON = k("codex.button");
	public static final String CODEX_LOCKED = k("codex.locked");
	public static final String CODEX_COUNT = k("codex.count");
	public static final String CODEX_TOAST_NEW = k("codex.toast.new");
	public static final String CODEX_TOAST_MANY = k("codex.toast.many");

	/** The hint a locked entry of the Codex gives (Codex.Hint). */
	public static String codexHint(String id) { return k("codex.hint." + id); }
	/** A Codex chapter's title, and a page's title and text (Codex). */
	public static String codexChapter(String id) { return k("codex.chapter." + id); }
	public static String codexEntryTitle(String id) { return k("codex.entry." + id + ".title"); }
	public static String codexEntryText(String id) { return k("codex.entry." + id + ".text"); }
	// The crafts' mastery (CraftMastery): their names and ranks, the word of a rank reached, a brew failed, the Stats tab's Crafts page
	public static String craftName(String id) { return k("craft." + id); }
	public static String craftRank(String id) { return k("craft.rank." + id); }
	public static final String MSG_CRAFT_RANK = k("message.craft_rank");
	public static final String CAULDRON_FAILED = k("cauldron.failed");
	public static final String BTN_STAT_CRAFTS = k("button.stat_crafts");
	public static final String CRAFTS_TITLE = k("crafts.title");
	public static final String CRAFTS_PRACTICE = k("crafts.practice");
	public static final String CRAFTS_PRACTICE_TOP = k("crafts.practice_top");
	public static final String CRAFTS_ALCHEMY_GRADE = k("crafts.alchemy.grade");
	public static final String CRAFTS_ALCHEMY_FAIL = k("crafts.alchemy.fail");
	public static final String CRAFTS_ALCHEMY_NEVER_FAILS = k("crafts.alchemy.never_fails");
	public static final String CRAFTS_ALCHEMY_SECOND = k("crafts.alchemy.second");
	public static final String CRAFTS_FORGING = k("crafts.forging");
	public static final String CRAFTS_FORMATIONS = k("crafts.formations");
	public static final String CRAFTS_HINT = k("crafts.hint");
	/** A world on the Codex's map, and its way in (CodexMap; client CodexDiagrams). */
	public static String codexWorld(String id) { return k("codex.world." + id); }
	public static String codexWorldWay(String id) { return k("codex.world." + id + ".way"); }
	// The words of the Codex's diagrams (client CodexDiagrams): a world's qi and the realm to reach before going, where one is
	public static final String CODEX_QI = k("codex.qi");
	public static final String CODEX_QI_BODY = k("codex.qi_body");
	public static final String CODEX_GHOST_QI = k("codex.ghost_qi");
	public static final String CODEX_GHOST_QI_IN_PLACES = k("codex.ghost_qi_in_places");
	public static final String CODEX_TIMES = k("codex.times");
	public static final String CODEX_RANGE = k("codex.range");
	public static final String CODEX_RECOMMENDED = k("codex.recommended");
	public static final String CODEX_ANY_REALM = k("codex.any_realm");
	public static final String CODEX_REALM_STAGE = k("codex.realm_stage");
	public static final String CODEX_HERE = k("codex.here");
	public static final String CODEX_ANYWHERE = k("codex.anywhere");
	public static final String CODEX_LOWER_REALMS = k("codex.lower_realms");
	public static final String CODEX_YOUR_REALM = k("codex.your_realm");
	public static final String CODEX_CULTIVATED_IN = k("codex.cultivated_in");
	public static final String CODEX_OVERCOMES = k("codex.overcomes");
	public static final String CODEX_OVERCOME_BY = k("codex.overcome_by");
	// A Bestiary page's picture (client CodexDiagrams): what the creature is, and what it drops
	public static final String CODEX_BEAST_REALM = k("codex.beast.realm");
	public static final String CODEX_BEAST_STAGE = k("codex.beast.stage");
	public static final String CODEX_BEAST_WITHIN = k("codex.beast.within");
	public static final String CODEX_BEAST_YOUR_REALM = k("codex.beast.your_realm");
	public static final String CODEX_BEAST_ELEMENT = k("codex.beast.element");
	public static final String CODEX_BEAST_HUNTS = k("codex.beast.hunts");
	public static final String CODEX_BEAST_FIGHTS_BACK = k("codex.beast.fights_back");
	public static final String CODEX_BEAST_DROPS = k("codex.beast.drops");
	public static final String CODEX_BEAST_DROPS_NOTHING = k("codex.beast.drops_nothing");
	/** How a creature goes about: "ground", "amphibious", "flying", "swimming" (BeastKind.Moves). */
	public static String codexBeastMoves(String moves) { return k("codex.beast.moves." + moves); }
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
	public static final String MSG_CORE_FIZZLED = k("message.core_detonation.fizzled");
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
	/** The Void Breach and the Great Void (VoidBreach, 2026-10-25). */
	public static final String MSG_BREACH_OPEN = k("message.void_breach.open");
	public static final String MSG_BREACH_TURNED_BACK = k("message.void_breach.turned_back");
	public static final String MSG_BREACH_ENTER = k("message.void_breach.enter");
	public static final String MSG_BREACH_CLIMB = k("message.void_breach.climb");
	public static final String MSG_ARRIVE_GREAT_VOID = k("message.void_breach.arrive");
	public static final String MSG_TEAR_DOWN = k("message.great_void.tear_down");
	public static final String MSG_FALL_TO_FIRMAMENT = k("message.great_void.fall");
	/** The Immortal Realm (phase 2, 2026-10-25): its space won't tear, its clouds bear the fallen back up (ImmortalRealm). */
	public static final String MSG_TEAR_IMMORTAL = k("message.immortal_realm.no_tear");
	public static final String MSG_CLOUDS_BEAR_UP = k("message.immortal_realm.borne_up");
	/** The Immortal Gate (ImmortalGate): its coming, its opening and shutting, the way up and down, those it turns back. */
	public static final String MSG_GATE_STIRS = k("message.immortal_gate.stirs");
	public static final String MSG_GATE_OPENS = k("message.immortal_gate.opens");
	public static final String MSG_GATE_CLOSES = k("message.immortal_gate.closes");
	public static final String MSG_GATE_TURNED_BACK = k("message.immortal_gate.turned_back");
	public static final String MSG_GATE_ENTER = k("message.immortal_gate.enter");
	public static final String MSG_GATE_CLIMB = k("message.immortal_gate.climb");
	public static final String MSG_ARRIVE_IMMORTAL = k("message.immortal_gate.arrive");
	public static final String MSG_GATE_DOWN = k("message.immortal_gate.down");
	public static final String MSG_GATE_SHUT_DAYS = k("message.immortal_gate.shut_days");
	public static final String MSG_GATE_SHUT_HOURS = k("message.immortal_gate.shut_hours");
	/** A void current takes one, and sets them down (VoidCurrents). */
	public static final String MSG_CURRENT_SWEPT = k("message.void_current.swept");
	public static final String MSG_CURRENT_LANDED = k("message.void_current.landed");
	/** A blood pool's elder rises (BloodPools). */
	public static final String MSG_ELDER_RISES = k("message.blood_pool.elder_rises");
	public static final String MSG_DAO_STELE_TEACHES = k("message.dao_stele.teaches");
	public static final String MSG_DAO_STELE_KNOWN = k("message.dao_stele.known");
	public static final String MSG_DAO_STELE_READING = k("message.dao_stele.reading");
	/** A stele carved, before the user's rule of 2026-10-25, with a method the worlds don't teach (Realm#taughtInTheWorld): unreadable. */
	public static final String MSG_STELE_SEALED = k("message.stele.sealed");
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
	/** The Spells tab's switch for whether spells change the land for good (ModGameRules#SPELL_GRIEFING), its tooltips, and the word to all. */
	public static final String BTN_GRIEFING_ON = k("spells.griefing_on");
	public static final String BTN_GRIEFING_OFF = k("spells.griefing_off");
	public static final String TIP_GRIEFING = k("spells.griefing_tip");
	public static final String TIP_GRIEFING_LOCKED = k("spells.griefing_locked");
	public static final String MSG_GRIEFING_ON = k("spells.griefing_now_on");
	public static final String MSG_GRIEFING_OFF = k("spells.griefing_now_off");
	/** Life Force Drain and Gain's modes (WoodSpells.LifeMode): their switch's names in the Spells tab, the word as one is chosen, and refusals. */
	public static final String LIFE_MODE_HEALTH = k("spells.life_mode_health");
	public static final String LIFE_MODE_QI = k("spells.life_mode_qi");
	public static final String LIFE_MODE_YEARS = k("spells.life_mode_years");
	public static final String MSG_LIFE_MODE = k("spells.life_mode_now");
	public static final String MSG_LIFE_NO_YEARS = k("spells.life_no_years");
	public static final String MSG_LIFE_NO_QI = k("spells.life_no_qi");
	public static final String MSG_LIFE_YEARS_GIFTED = k("spells.life_years_gifted");
	/** Return to the Bough's word (WoodSpells#Bough): no fruit held, no bough to hang it on, and how many were hung, at what age. */
	public static final String MSG_BOUGH_NO_FRUIT = k("spells.bough_no_fruit");
	public static final String MSG_BOUGH_NO_TREE = k("spells.bough_no_tree");
	public static final String MSG_BOUGH_HUNG = k("spells.bough_hung");
	public static final String MSG_LIFE_YEARS_RECEIVED = k("spells.life_years_received");
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
	/** The spells of both paths' heaven-and-demon qi (ModDamageTypes#HEAVEN_DEMON). */
	public static final String DEATH_HEAVEN_DEMON = "death.attack." + DefyingTheHeavens.MOD_ID + ".heaven_demon";
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
	public static final String DIMENSION_GREAT_VOID = k("dimension.great_void");
	public static final String DIMENSION_VOID_BREACH = k("dimension.void_breach");
	public static final String DIMENSION_IMMORTAL_REALM = k("dimension.immortal_realm");
	public static final String DIMENSION_IMMORTAL_PASSAGE = k("dimension.immortal_passage");
	/** The Cultivation tab's and the Stats tab's lines for the Great Void's qi (as UPPER_REALM_LINE, STATS_QI_GATHER_UPPER). */
	public static final String GREAT_VOID_LINE = k("great_void_line");
	public static final String STATS_QI_GATHER_GREAT_VOID = k("stats.qi_gather_great_void");
	/** The same for the Immortal Realm's immortal qi. */
	public static final String IMMORTAL_REALM_LINE = k("immortal_realm_line");
	public static final String STATS_QI_GATHER_IMMORTAL = k("stats.qi_gather_immortal");
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
	/** Space locked round one (Earth Veins Lock the Void: EarthSpells#spaceLocked): no step through it, no tear in it. */
	public static final String MSG_SPACE_LOCKED = k("message.space_locked");
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

	// Techniques (Technique) and their manuals (TechniqueManualItem; written out with the Inscription Brush)
	public static String techniqueKey(String id) { return "technique." + DefyingTheHeavens.MOD_ID + "." + id; }
	public static final String ITEM_TECHNIQUE_MANUAL_OF = "item." + DefyingTheHeavens.MOD_ID + ".technique_manual.of";
	public static final String ITEM_SPELL_MANUAL_OF = "item." + DefyingTheHeavens.MOD_ID + ".technique_manual.spell_of";
	public static final String TOOLTIP_MANUAL = k("tooltip.technique_manual");
	public static final String TOOLTIP_MANUAL_REALM = k("tooltip.technique_manual.realm");
	public static final String TOOLTIP_MANUAL_AUTHOR = k("tooltip.technique_manual.author");
	public static final String TOOLTIP_MANUAL_GHOST = k("tooltip.technique_manual.ghost");
	public static final String TOOLTIP_BLANK_MANUAL = k("tooltip.blank_manual");
	public static final String MSG_MANUAL_LEARNED = k("message.manual.learned");
	public static final String MSG_MANUAL_KNOWN = k("message.manual.known");
	public static final String MSG_MANUAL_TOO_LOW = k("message.manual.too_low");
	public static final String MSG_MANUAL_ROOTS = k("message.manual.roots");
	public static final String MSG_MANUAL_DEAD = k("message.manual.dead");
	public static final String INSCRIBE_TITLE = k("inscribe.title");
	public static final String INSCRIBE_HINT = k("inscribe.hint");
	public static final String INSCRIBE_NONE = k("inscribe.none");
	public static final String INSCRIBE_TECHNIQUE = k("inscribe.technique");
	public static final String MSG_INSCRIBED = k("message.inscribed");
	// Clone (Clones)
	public static final String MSG_CLONE_BUSY = k("message.clone.busy");
	public static final String MSG_CLONE_SPLIT = k("message.clone.split");
	public static final String MSG_CLONE_LOST = k("message.clone.lost");
	public static final String MSG_CLONE_INTO = k("message.clone.into");
	public static final String MSG_CLONE_BACK = k("message.clone.back");
	public static final String MSG_CLONE_ABSORBED = k("message.clone.absorbed");
	public static final String MSG_CLONE_HURT = k("message.clone.hurt");
	public static final String MSG_CLONE_MAIN_HURT = k("message.clone.main_hurt");
	public static final String MSG_CLONE_SLAIN = k("message.clone.slain");
	public static final String MSG_CLONE_MAIN_SLAIN = k("message.clone.main_slain");
	public static final String MSG_CLONE_FELL = k("message.clone.fell");
	public static final String MSG_CLONE_CANNOT = k("message.clone.cannot");
	public static final String MSG_CLONE_REFORMING = k("message.clone.reforming");
	public static final String MSG_CLONE_DOMAIN_MET = k("message.clone.domain_met");
	// Universe in a Sleeve (Sleeves, SleeveCommand)
	public static final String MSG_SLEEVE_HOW = k("message.sleeve.how");
	public static final String MSG_SLEEVE_EMPTY = k("message.sleeve.empty");
	public static final String MSG_SLEEVE_OFFER = k("message.sleeve.offer");
	public static final String MSG_SLEEVE_OFFERED = k("message.sleeve.offered");
	public static final String SLEEVE_ACCEPT = k("message.sleeve.accept");
	public static final String SLEEVE_REFUSE = k("message.sleeve.refuse");
	public static final String SLEEVE_LEAVE = k("message.sleeve.leave");
	public static final String MSG_SLEEVE_NO_OFFER = k("message.sleeve.no_offer");
	public static final String MSG_SLEEVE_REFUSED = k("message.sleeve.refused");
	public static final String MSG_SLEEVE_YOU_REFUSED = k("message.sleeve.you_refused");
	public static final String MSG_SLEEVE_NOT_ON_THE_WAY = k("message.sleeve.not_on_the_way");
	public static final String MSG_SLEEVE_FULL = k("message.sleeve.full");
	public static final String MSG_SLEEVE_CANT_TAKE = k("message.sleeve.cant_take");
	public static final String MSG_SLEEVE_TAKEN = k("message.sleeve.taken");
	public static final String MSG_SLEEVE_INSIDE = k("message.sleeve.inside");
	public static final String MSG_SLEEVE_COST = k("message.sleeve.cost");
	public static final String MSG_SLEEVE_MID_CLIMB = k("message.sleeve.mid_climb");
	public static final String MSG_SLEEVE_LET_OUT = k("message.sleeve.let_out");
	public static final String MSG_SLEEVE_OUT = k("message.sleeve.out");
	public static final String MSG_SLEEVE_ARRIVED = k("message.sleeve.arrived");
	public static final String MSG_SLEEVE_CAST_OUT = k("message.sleeve.cast_out");
	public static final String MSG_SLEEVE_BEARER_GONE = k("message.sleeve.bearer_gone");
	public static final String MSG_SLEEVE_OUT_AWAY = k("message.sleeve.out_away");
	public static final String MSG_SLEEVE_NOT_IN = k("message.sleeve.not_in");
	public static final String MSG_SLEEVE_CANT_LEAVE = k("message.sleeve.cant_leave");
	public static final String MSG_SLEEVE_STEPPED_OUT = k("message.sleeve.stepped_out");
	public static final String MSG_SLEEVE_YOU_STEPPED_OUT = k("message.sleeve.you_stepped_out");
	public static final String MSG_BODY_DOMAIN_MET = k("message.clone.body_domain_met");
	public static final String HUD_CLONE_MAIN = k("hud.clone.main");
	public static final String HUD_CLONE_IN = k("hud.clone.in");
	// The Laws (Laws, phase 3): each one's name (and ".gains", ".now"), the ladder's gate, comprehending, the Methods tab's Laws page
	public static String lawKey(String id) { return k("law." + id); }
	public static final String LAW_GATE = k("law.gate");
	public static final String MSG_LAW_LEVEL = k("message.law.level");
	public static final String MSG_LAW_WAY_OPEN = k("message.law.way_open");
	public static final String MSG_LAW_GLIMPSE = k("message.law.glimpse");
	public static final String MSG_LAW_FOCUSED = k("message.law.focused");
	public static final String MSG_LAW_UNFOCUSED = k("message.law.unfocused");
	public static final String MSG_LAW_GREATER_SHUT = k("message.law.greater_shut");
	public static final String MSG_LAW_COMPLETE = k("message.law.complete");
	public static final String MSG_LAW_NOT_IMMORTAL = k("message.law.not_immortal");
	public static final String METHODS_PAGE_LAWS = k("methods.page.laws");
	public static final String LAWS_TITLE = k("laws.title");
	public static final String LAWS_SEALED = k("laws.sealed");
	public static final String LAWS_TOTAL = k("laws.total");
	public static final String LAWS_NEXT = k("laws.next");
	public static final String LAWS_NEXT_OPEN = k("laws.next_open");
	public static final String LAWS_FOCUS = k("laws.focus");
	public static final String LAWS_FOCUS_AWAY = k("laws.focus_away");
	public static final String LAWS_NO_FOCUS = k("laws.no_focus");
	public static final String LAWS_CHOICES = k("laws.choices");
	public static final String LAWS_CHOICE_ONE = k("laws.choice_one");
	public static final String LAWS_LEVEL = k("laws.level");
	public static final String LAWS_INSIGHT = k("laws.insight");
	public static final String LAWS_COMPLETE = k("laws.complete");
	public static final String LAWS_GREATER_SHUT = k("laws.greater_shut");
	public static final String LAWS_FOCUSED = k("laws.focused");
	public static final String BTN_LAW_FOCUS = k("button.law.focus");
	public static final String BTN_LAW_CHOOSE = k("button.law.choose");
	// The Five Decays (FiveDecays, phase 3): the warning, the five signs, the judgement, its end; the Laws page's line of it
	public static final String MSG_DECAY_WARNING = k("message.decay.warning");
	public static final String[] MSG_DECAY_SIGN = {k("message.decay.sign1"), k("message.decay.sign2"), k("message.decay.sign3"),
			k("message.decay.sign4"), k("message.decay.sign5")};
	public static final String MSG_DECAY_JUDGEMENT = k("message.decay.judgement");
	public static final String MSG_DECAY_JUDGEMENT_BROADCAST = k("message.decay.judgement_broadcast");
	public static final String MSG_DECAY_STRIKES = k("message.decay.strikes");
	public static final String MSG_DECAY_WAITS = k("message.decay.waits");
	public static final String MSG_DECAY_WEATHERED = k("message.decay.weathered");
	public static final String MSG_DECAY_WEATHERED_BROADCAST = k("message.decay.weathered_broadcast");
	public static final String MSG_DECAY_FAILED = k("message.decay.failed");
	public static final String MSG_DECAY_FAILED_BROADCAST = k("message.decay.failed_broadcast");
	public static final String MSG_DECAY_RESTLESS = k("message.decay.restless");
	public static final String LAWS_DECAY_IN = k("laws.decay_in");
	public static final String LAWS_DECAY_SOON = k("laws.decay_soon");
	public static final String LAWS_DECAY_SIGN = k("laws.decay_sign");
	public static final String LAWS_DECAY_JUDGEMENT = k("laws.decay_judgement");
	// The Law domains (LawDomains, phase 3b): unfurled, folded away, the essence spent or too little, where it can't hold, space locked by one
	public static final String MSG_DOMAIN_ON = k("message.domain.on");
	public static final String MSG_DOMAIN_OFF = k("message.domain.off");
	public static final String MSG_DOMAIN_SPENT = k("message.domain.spent");
	public static final String MSG_DOMAIN_TOO_LITTLE = k("message.domain.too_little");
	public static final String MSG_DOMAIN_LOST = k("message.domain.lost");
	public static final String MSG_DOMAIN_SPACE_LOCKED = k("message.domain.space_locked");
	/** The Primordial Chaos's Unmaking folded a domain away (PrimordialBeast, LawDomains#unmake). */
	public static final String MSG_DOMAIN_UNMADE = k("message.domain.unmade");
	// The Chaos Frontier (ChaosFrontier, phase 3c): crossing into it and out, the storm's harm begun and sheltered from; the First Flame's
	// spring; the Primordial Chaos's Unmaking gathering; its Dao Fragments and Chaos Origin Stones
	public static final String MSG_CHAOS_ENTER = k("message.chaos.enter");
	public static final String MSG_CHAOS_LEAVE = k("message.chaos.leave");
	public static final String MSG_CHAOS_EXPOSED = k("message.chaos.exposed");
	public static final String MSG_CHAOS_SHELTERED = k("message.chaos.sheltered");
	public static final String MSG_FIRST_FLAME = k("message.chaos.first_flame");
	public static final String MSG_CHAOS_UNMAKING = k("message.chaos.unmaking");
	public static final String MSG_DAO_FRAGMENT = k("message.chaos.dao_fragment");
	public static final String ITEM_DAO_FRAGMENT_OF = k("item.dao_fragment.of");
	public static final String TOOLTIP_DAO_FRAGMENT = k("tooltip.dao_fragment");
	public static final String TOOLTIP_CHAOS_ORIGIN_STONE = k("tooltip.chaos_origin_stone");
	public static final String DEATH_CHAOS_STORM = "death.attack." + DefyingTheHeavens.MOD_ID + ".chaos_storm";
	// The small worlds (SmallWorlds, phase 3d): a seed refined (or why not), the door opened and closed (or why not), a world come into
	// and gone out of, its land grown, those turned away at its door, the guest list (SmallWorldCommand)
	public static final String MSG_SEED_NOT_IMMORTAL = k("message.small_world.seed_not_immortal");
	public static final String MSG_SEED_HAVE_ONE = k("message.small_world.seed_have_one");
	public static final String MSG_SEED_REFINING = k("message.small_world.seed_refining");
	public static final String MSG_SEED_REFINED = k("message.small_world.seed_refined");
	public static final String MSG_DOOR_OPEN = k("message.small_world.door_open");
	public static final String MSG_DOOR_CLOSED = k("message.small_world.door_closed");
	public static final String MSG_DOOR_NOT_HERE = k("message.small_world.door_not_here");
	public static final String MSG_DOOR_NOT_IMMORTAL = k("message.small_world.door_not_immortal");
	public static final String MSG_DOOR_TURNED_AWAY = k("message.small_world.door_turned_away");
	public static final String MSG_WORLD_OWN = k("message.small_world.own");
	public static final String MSG_WORLD_GUEST = k("message.small_world.guest");
	public static final String MSG_WORLD_LEFT = k("message.small_world.left");
	public static final String MSG_WORLD_GROWN = k("message.small_world.grown");
	public static final String MSG_WORLD_CAUGHT = k("message.small_world.caught");
	public static final String MSG_WORLD_SENT_OUT = k("message.small_world.sent_out");
	// A world carried up the ways with its owner, those in it with it (SmallWorlds#startedUp, #cameOut)
	public static final String MSG_WORLD_BORNE = k("message.small_world.borne");
	public static final String MSG_WORLD_CARRIED_UP = k("message.small_world.carried_up");
	public static final String MSG_WORLD_BROUGHT = k("message.small_world.brought");
	public static final String CMD_WORLD_NONE = k("command.small_world.none");
	public static final String CMD_WORLD_INFO = k("command.small_world.info");
	public static final String CMD_WORLD_GUESTS = k("command.small_world.guests");
	public static final String CMD_WORLD_NO_GUESTS = k("command.small_world.no_guests");
	public static final String CMD_GUEST_ADDED = k("command.small_world.guest_added");
	public static final String CMD_GUEST_ALREADY = k("command.small_world.guest_already");
	public static final String CMD_GUEST_SELF = k("command.small_world.guest_self");
	public static final String CMD_GUEST_REMOVED = k("command.small_world.guest_removed");
	public static final String CMD_GUEST_UNKNOWN = k("command.small_world.guest_unknown");
	public static final String MSG_GUEST_INVITED = k("message.small_world.invited");
	public static final String TOOLTIP_WORLD_SEED = k("tooltip.world_seed");

	// The titles players take (PlayerTitles, 2026-10-27): the Cultivation tab's field, and what one is told.
	public static final String TITLE_LABEL = k("title_field.label");
	public static final String TITLE_HINT = k("title_field.hint");
	public static final String TITLE_TIP = k("title_field.tip");
	public static final String MSG_TITLE_SET = k("message.title.set");
	public static final String MSG_TITLE_ANNOUNCED = k("message.title.announced");
	public static final String MSG_TITLE_CLEARED = k("message.title.cleared");
	public static final String MSG_TITLE_LOCKED = k("message.title.locked");

	private ModLang() {}
}
