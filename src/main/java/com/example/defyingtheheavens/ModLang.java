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
	public static final String SURGE_CIRCUIT = k("surge.circuit");
	public static final String SURGE_ELEMENTS = k("surge.elements");
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
	/** Struck mid-talk; a Sworn friend's aid and gift; one begging for its life; a topic's regard; a junior's realm. */
	public static final String TALK_INTERRUPTED = k("talk.interrupted");
	public static final String TALK_AID = k("talk.aid");
	public static final String TALK_GIFT_BACK = k("talk.gift_back");
	public static final String TALK_BEGS = k("talk.begs");
	public static final String TALK_NEEDS = k("talk.needs");
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
	public static final String HUD_SOUL_GATHERING = k("hud.soul.gathering");
	public static final String HUD_SOUL_RECALLING = k("hud.soul.recalling");
	public static final String HUD_SOUL_TETHER = k("hud.soul.tether");
	public static final String HUD_SOUL_BODY = k("hud.soul.body");
	public static final String HUD_SOUL_POSSESSING = k("hud.soul.possessing");
	public static final String DEATH_SOUL_SEVERED = "death.attack." + DefyingTheHeavens.MOD_ID + ".soul_severed";
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

	private ModLang() {}
}
