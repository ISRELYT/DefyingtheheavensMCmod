package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.Alignment;
import com.example.defyingtheheavens.AstralPlanets;
import com.example.defyingtheheavens.ClothingStyle;
import com.example.defyingtheheavens.ClothingTier;
import com.example.defyingtheheavens.Codex;
import com.example.defyingtheheavens.GhostMethod;
import com.example.defyingtheheavens.Spell;
import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.NpcTitle;
import net.minecraft.world.item.ArmorItem;
import com.example.defyingtheheavens.ModEntities;
import com.example.defyingtheheavens.ModEffects;
import com.example.defyingtheheavens.ModItems;
import com.example.defyingtheheavens.Technique;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.MortalStage;
import com.example.defyingtheheavens.NpcLines;
import com.example.defyingtheheavens.PillGrade;
import com.example.defyingtheheavens.QiElement;
import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.Stage;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.StatPoints;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

public class ModEnglishLangProvider extends FabricLanguageProvider {
	/** What every Law domain's description ends with (LawDomains): its cost, one at a time, when it folds away. */
	private static final String DOMAIN_TAIL = " Burns immortal essence: a full pool lasts %2$s s. One domain at a time; it folds away when the pool runs dry, "
			+ "and when you die or log out.";

	public ModEnglishLangProvider(FabricDataOutput output) {
		super(output, "en_us");
	}

	@Override
	public void generateTranslations(TranslationBuilder t) {
		// The heaven methods' names, lore and spells (generated from their spec sheet)
		HeavenLangText.add(t);
		// Items
		t.add(ModItems.CULTIVATION_FRUIT, "Cultivation Fruit");
		t.add(ModBlocks.CULTIVATION_FRUIT, "Cultivation Fruit");
		t.add("item.defying-the-heavens.cultivation_fruit.age", "Age: %s years");
		t.add("item.defying-the-heavens.cultivation_fruit.gain", "Eat for up to %s unrefined qi");
		t.add("item.defying-the-heavens.cultivation_fruit.picked", "Harvested: no longer ages");
		t.add("item.defying-the-heavens.cultivation_fruit.full", "Inventory full.");
		t.add(ModBlocks.MEDITATION_MAT, "Meditation Mat");
		t.add("gui.defying-the-heavens.baubles", "Baubles");
		t.add("config.jade.plugin_defying-the-heavens.fruit_age", "Fruit and Herb Age"); // Jade's toggle for the fruit-age tooltip
		t.add(ModBlocks.RED_MEDITATION_MAT, "Red Silk Meditation Mat");
		t.add(ModBlocks.SPIRIT_PEDESTAL, "Spirit Pedestal");
		t.add(ModLang.PEDESTAL_TOOLTIP, "Holds a fruit or herb to aid meditation");
		t.add(ModItems.GINSENG, "Ginseng");
		t.add(ModBlocks.GINSENG, "Ginseng");
		t.add(ModItems.SPIRIT_GINSENG, "Spirit Ginseng");
		t.add(ModBlocks.SPIRIT_GINSENG, "Spirit Ginseng");
		t.add(ModItems.PEACH_PIT, "Peach Pit");
		t.add(ModBlocks.SPIRIT_PEACH_SAPLING, "Spirit Peach Sapling");
		t.add(ModBlocks.SPIRIT_PEACH_LEAVES, "Spirit Peach Leaves");
		t.add(ModBlocks.SPIRIT_PEACH_HEART, "Spirit Peach Heart");
		t.add(ModLang.PEACH_PIT_TOOLTIP, "Plant it in soil to grow a Spirit Peach Tree");
		t.add(ModLang.PEACH_NEED_CORE, "Only a Golden Core can feed the tree qi.");
		t.add(ModLang.PEACH_NOT_ENOUGH, "Not enough qi (next year costs %s).");
		t.add(ModLang.PEACH_MAX, "Max age: 10,000 years.");
		t.add(ModLang.PEACH_FED, "-%s qi: the tree grows %s years (now %s).");
		t.add(ModLang.GINSENG_INGREDIENT, "Alchemy ingredient");
		t.add(ModLang.GINSENG_HARVESTED, "Harvested: no longer ages");
		t.add(ModLang.GINSENG_FULL, "Inventory full.");
		t.add(ModItems.GINSENG_SEEDS, "Ginseng Seeds");
		t.add(ModItems.SPIRIT_GINSENG_SEEDS, "Spirit Ginseng Seeds");
		t.add(ModLang.GINSENG_SEEDS_TOOLTIP, "Plant in soil. Grown at %s years; drops seeds when dug up");
		t.add(ModLang.GINSENG_NEED_CORE, "Only a Golden Core can feed herbs qi.");
		t.add(ModLang.GINSENG_NOT_ENOUGH, "Not enough qi (next year costs %s).");
		t.add(ModLang.GINSENG_MAX, "Max age: 10,000 years.");
		t.add(ModLang.GINSENG_FED, "-%s qi: the herb grows %s years (now %s).");
		t.add(ModLang.FRUIT_PEDESTAL_TOOLTIP, "On a pedestal: +%s%% meditation speed nearby");
		t.add(ModItems.RING_OF_POWER, "Ring of Power");
		t.add(ModLang.RING_OF_POWER_TOOLTIP, "Worn: +%s cultivation/s while meditating");
		t.add(ModItems.RING_OF_TRANSCENDENCE, "Ring of Transcendence");
		t.add(ModLang.RING_OF_TRANSCENDENCE_TOOLTIP, "When worn: Heavenly Tribulation damage x%s");

		// Entities
		t.add(ModEntities.TRIBULATION_LIGHTNING, "Tribulation Lightning");
		t.add(ModEntities.INNER_BODY, "Meditating Body");
		t.add(ModEntities.HEART_DEMON, "Heart Demon");
		t.add(ModEntities.SOUL_BODY, "Body");
		t.add(ModEntities.SOUL_VESSEL, "Seized Body");
		t.add(ModBlocks.SOUL_CRYSTAL, "Soul Crystal");
		// The rest, named (the bug sweep of 2026-10-27: a death message falls back on the harm's own name when its caster is gone, and
		// these showed as raw keys there and wherever else an entity is named).
		t.add(ModEntities.TRIBULATION_CLOUD, "Tribulation Cloud");
		t.add(ModEntities.SPELL_PROJECTILE, "Spell");
		t.add(ModEntities.SPELL_FIELD, "Spell Field");
		t.add(ModEntities.SPELL_CONSTRUCT, "Spell Construct");
		t.add(ModEntities.SPELL_BLOCK, "Spell Block");
		t.add(ModEntities.EARTH_GOLEM, "Earth Golem");
		t.add(ModEntities.DIRT_TIDE, "Dirt Tide");
		t.add(ModEntities.SPATIAL_TEAR, "Spatial Tear");

		// Blocks
		t.add(ModBlocks.SPATIAL_RIFT, "Spatial Rift");
		t.add(ModBlocks.YELLOW_SPRINGS_RIFT, "Yellow Springs Rift");
		t.add(ModBlocks.WHEEL_OF_REINCARNATION, "Wheel of Reincarnation");
		t.add(ModBlocks.IMMORTAL_GATE_LIGHT, "Light of the Immortal Gate");
		t.add(ModBlocks.JADE_STONE, "Jade Stone");
		t.add(ModBlocks.JADE_ORE, "Jade Ore");
		t.add(ModBlocks.DEEPSLATE_JADE_ORE, "Deepslate Jade Ore");
		t.add(ModItems.JADE, "Jade");
		t.add(ModBlocks.WHITE_BLOSSOM_LEAVES, "White Blossom Leaves");
		t.add(ModBlocks.BLUE_SPIRIT_LOG, "Blue Spirit Log");
		t.add(ModBlocks.BLUE_SPIRIT_WOOD, "Blue Spirit Wood");
		t.add(ModBlocks.BLUE_SPIRIT_PLANKS, "Blue Spirit Planks");

		// The Jade Firmament's (the Upper Realm's) biomes and the Spatial Gap
		t.add(ModLang.biomeKey("dense_qi_peaks"), "Dense Qi Peaks");
		t.add(ModLang.biomeKey("spirit_forest"), "Spirit Forest");
		t.add(ModLang.biomeKey("peach_blossom_sanctuary"), "Peach Blossom Sanctuary");
		t.add(ModLang.biomeKey("thunder_peaks"), "Thunder Peaks");
		t.add(ModLang.biomeKey("ancient_sword_graveyard"), "Ancient Sword Graveyard");
		t.add(ModLang.biomeKey("spatial_gap"), "Spatial Gap");
		// The Great Void Realm's lands, and the Void Breach
		t.add(ModLang.biomeKey("void_steppes"), "Void Steppes");
		t.add(ModLang.biomeKey("dao_field_wood"), "Dao Field of Wood");
		t.add(ModLang.biomeKey("dao_field_fire"), "Dao Field of Fire");
		t.add(ModLang.biomeKey("dao_field_earth"), "Dao Field of Earth");
		t.add(ModLang.biomeKey("dao_field_metal"), "Dao Field of Metal");
		t.add(ModLang.biomeKey("dao_field_water"), "Dao Field of Water");
		t.add(ModLang.biomeKey("ancient_battlefield"), "Ancient Battlefield");
		t.add(ModLang.biomeKey("shore_of_nirvana"), "Shore of Nirvana");
		t.add(ModLang.biomeKey("void_breach"), "Void Breach");
		t.add(ModLang.biomeKey("blood_ring"), "Blood Ring");
		// The Immortal Realm's (phase 2, 2026-10-25), and the Gate's passage
		t.add(ModLang.biomeKey("jade_peaks"), "Jade Peaks");
		t.add(ModLang.biomeKey("immortal_gardens"), "Immortal Gardens");
		t.add(ModLang.biomeKey("chaos_frontier"), "Chaos Frontier");
		t.add(ModLang.biomeKey("celestial_heights"), "Celestial Heights");
		t.add(ModLang.biomeKey("jade_capital"), "Jade Capital");
		t.add(ModLang.biomeKey("immortal_passage"), "Immortal Gate");
		t.add(ModLang.biomeKey("inner_realm"), "Inner Realm");
		// The Netherworld's biomes, and the Soul River
		t.add(ModLang.biomeKey("yellow_springs"), "Yellow Springs");
		t.add(ModLang.biomeKey("fields_of_the_other_shore"), "Fields of the Other Shore");
		t.add(ModLang.biomeKey("bone_wastes"), "Bone Wastes");
		t.add(ModLang.biomeKey("nine_nether_peaks"), "Nine Nether Peaks");
		t.add(ModLang.biomeKey("soul_river"), "Soul River");
		t.add(ModBlocks.RED_SPIDER_LILY, "Red Spider Lily");
		t.add(ModLang.MSG_HEAVENS_BLIND, "The heavens cannot see you here.");
		t.add(ModLang.MSG_GATE_PROTECTED, "The Ghost Gate's ground is not yours to change.");
		t.add(ModLang.MSG_WHEEL_LIVING, "The Wheel turns only for the dead.");
		t.add(ModLang.MSG_ARRIVE_NETHERWORLD, "The Netherworld. Its yin seeps into the living: leave before it fills you.");
		t.add(ModLang.MSG_ARRIVE_NETHER, "The river sets you down in the Nether.");
		t.add(ModLang.MSG_RIVER_ASCEND, "Up the Soul River: keep to its flow for %s seconds.");
		t.add(ModLang.MSG_RIVER_DESCEND, "Down the Soul River: keep to its flow for %s seconds.");
		t.add(ModLang.MSG_RIVER_ABYSS, "The abyss gives you to the Soul River: keep to its flow for %s seconds.");
		t.add(ModLang.MSG_RIVER_RESTART, "Back on the Soul River: keep to its flow for %s seconds.");
		t.add(ModLang.MSG_RIVER_SCATTERED_UP, "Your soul scattered, and gathered again at the Nether's gate.");
		t.add(ModLang.MSG_RIVER_SCATTERED_DOWN, "Your soul scattered: the river spat you out into the Nether.");
		t.add(ModLang.MSG_RIVER_COOLDOWN, "Your soul is still unsettled from the river. Wait %s seconds.");
		t.add(ModLang.HUD_RIVER, "The Soul River  %s s");
		t.add(ModLang.HUD_RIVER_STRAY, "Back into the flow!");
		t.add(ModLang.DEATH_SOUL_SCATTER, "%1$s's soul scattered on the Soul River");
		t.add(ModLang.DEATH_SOUL_SCATTER + ".player", "%1$s's soul scattered on the Soul River while fighting %2$s");
		t.add(ModLang.DEATH_JUDGE_GAZE, "%1$s met the gaze of the Chief Judge of the Netherworld");
		t.add(ModLang.DEATH_JUDGE_GAZE + ".player", "%1$s met the gaze of the Chief Judge of the Netherworld while fighting %2$s");
		t.add(ModLang.MSG_JUDGE_WARNING, "The Chief Judge's eye has turned on you. Look away!");
		t.add(ModLang.MSG_JUDGE_STRIPPED, "The Chief Judge's gaze unmade you: you are back at Qi Refining Early.");
		t.add(ModLang.MSG_JUDGE_MORTAL, "The Chief Judge's gaze unmade you.");
		t.add(ModLang.DEATH_GHOSTLY, "%1$s was torn at by the dead");
		t.add(ModLang.DEATH_GHOSTLY + ".player", "%1$s was torn at by the dead %2$s");
		// The heaven methods' qi (ModDamageTypes#qi)
		String death = "death.attack." + com.example.defyingtheheavens.DefyingTheHeavens.MOD_ID + ".qi_";
		t.add(death + "wood", "%1$s was strangled by living wood");
		t.add(death + "wood.player", "%1$s was strangled by %2$s's wood qi");
		t.add(death + "water", "%1$s was drowned in qi-born water");
		t.add(death + "water.player", "%1$s was drowned in %2$s's water qi");
		t.add(death + "fire", "%1$s was burnt away by fire qi");
		t.add(death + "fire.player", "%1$s was burnt away by %2$s's fire qi");
		t.add(death + "earth", "%1$s was crushed beneath earth qi");
		t.add(death + "earth.player", "%1$s was crushed beneath %2$s's earth qi");
		t.add(death + "metal", "%1$s was cut down by metal qi");
		t.add(death + "metal.player", "%1$s was cut down by %2$s's metal qi");
		// The spells of both paths' heaven-and-demon qi (ModDamageTypes#heavenDemon)
		t.add(ModLang.DEATH_HEAVEN_DEMON, "%1$s was torn apart by heaven's light and a demon's qi");
		t.add(ModLang.DEATH_HEAVEN_DEMON + ".player", "%1$s was torn apart by %2$s's heaven and demon qi");
		t.add(ModLang.GAMERULE_AFTER_DEATH, "The dead go to the Netherworld");
		t.add(ModLang.GAMERULE_AFTER_DEATH + ".description", "Players who die wake at the Ghost Gate as ghosts, and return to life at the Wheel of Reincarnation.");
		t.add(ModLang.MSG_GHOST_WAKE, "You are dead. The Wheel of Reincarnation, across the bridge, will return you to life.");
		t.add(ModLang.MSG_YIN_OVERTAKEN, "The yin overtakes you: you walk on among the dead.");
		t.add(ModLang.MSG_BELONGINGS_LEFT, "What you carried stays behind at the Wheel.");
		t.add(ModLang.MSG_GHOST_KING, "You are a Ghost King: you can touch the world again, and cross to the living.");
		t.add(ModLang.MSG_GHOST_KING_LOST, "You are no longer a Ghost King.");
		t.add(ModLang.JOURNAL_GHOST_KING, "A Ghost King");
		t.add(ModLang.JOURNAL_GHOST_KING_TEXT, "Dead, with my ghost path at Core Formation, I am a Ghost King. My hands are my own again, and the "
				+ "Soul River carries me down to the world of the living, unseen but by those who see souls.");
		t.add(ModLang.MSG_GHOST_DISPERSED, "You scatter...");
		t.add(ModLang.MSG_GHOST_REFORMED, "You gather yourself again at the Ghost Gate.");
		t.add(ModLang.MSG_GHOST_CANNOT, "The dead can't do that.");
		t.add(ModLang.MSG_WHEEL_TURNS, "The Wheel of Reincarnation turns for you...");
		t.add(ModLang.MSG_WHEEL_HARDCORE, "Your thread of life has been cut.");
		t.add(ModLang.MSG_REVIVED, "You wake, alive.");
		t.add(ModLang.MSG_YIN_HALF, "The yin has half filled you.");
		t.add(ModLang.MSG_YIN_THREE_QUARTERS, "The yin creeps toward your heart.");
		t.add(ModLang.MSG_YIN_NEAR, "The yin has nearly filled you: leave, or burn a Yang Fire Talisman!");
		t.add(ModLang.MSG_YIN_CLEANSED, "The yang fire burns away %s%% of the yin (%s%% left).");
		t.add(ModLang.MSG_YIN_NONE, "There is no yin in you to burn away.");
		t.add(ModLang.YIN_LINE, "Yin corruption: %s%%");
		t.add(ModLang.YIN_EBBING, "Yin corruption: %s%% (ebbing)");
		t.add(ModLang.YIN_HELD, "Yin corruption: %s%% (held still, %s s)");
		t.add(ModLang.GHOST_QI_HERE, "Ghostly qi here: %s");
		t.add(ModLang.GHOST_QI_TEEMING, "teeming x%s (an ancient battlefield's dead)");
		t.add(ModLang.GHOST_QI_DENSE, "dense x%s");
		t.add(ModLang.GHOST_QI_PEAKS, "dense x%s (Nine Nether Peaks)");
		t.add(ModLang.GHOST_QI_THIN, "thin x%s");
		t.add(ModLang.GHOST_QI_FAINT, "faint x%s");
		t.add(ModLang.HUD_GHOST_QI_LABEL, "Ghost Qi");
		t.add(ModLang.HUD_YIN_CORRUPTION_LABEL, "Yin Corruption");
		t.add(ModLang.HUD_PERCENT, "%s%%");
		t.add(ModItems.YANG_FIRE_TALISMAN, "Yang Fire Talisman");
		t.add(ModLang.TOOLTIP_YANG_FIRE_TALISMAN, "Burn it to drive a third of the Netherworld's yin out of your body");
		t.add(ModLang.DEATH_SPATIAL_PRESSURE, "%1$s was crushed by spatial pressure");
		t.add(ModLang.DEATH_SPATIAL_PRESSURE + ".player", "%1$s was crushed by spatial pressure while fighting %2$s");
		t.add(ModLang.DEATH_CHAOS_STORM, "%1$s was unmade by the chaos storm");
		t.add(ModLang.DEATH_CHAOS_STORM + ".player", "%1$s was unmade by the chaos storm while fighting %2$s");
		t.add(ModLang.DEATH_SOUL_SEVERED, "%1$s's nascent soul was severed from their body");
		t.add(ModLang.DEATH_SOUL_SEVERED + ".player", "%1$s's nascent soul was severed from their body by %2$s");
		t.add(ModLang.DEATH_OLD_AGE, "%1$s's years ran out");
		t.add(ModLang.DEATH_OLD_AGE + ".player", "%1$s's years ran out");
		t.add(ModLang.DEATH_SOUL_SEIZED, "%1$s's body was seized by %2$s's nascent soul");
		t.add(ModLang.DEATH_SOUL_SEIZED + ".item", "%1$s's body was seized by %2$s's nascent soul");
		t.add(ModLang.DEATH_SOUL_SEIZED + ".player", "%1$s's body was seized by %2$s's nascent soul");

		// Spatial Gap, Realm Suppression and portals
		t.add(ModLang.MSG_ARRIVE_UPPER, "The Jade Firmament. Its dense qi makes cultivation %sx faster.");
		t.add(ModLang.MSG_SUPPRESSED, "The lower realm suppresses you to %s.");
		t.add(ModLang.MSG_RESTORED, "Suppression lifted: %s.");
		t.add(ModLang.MSG_REALM_LOCKED, "Past %s, only the Jade Firmament and the realms above it can hold you.");
		t.add(ModLang.MSG_IMMORTAL_REALM_LOCKED, "Only the Immortal Realm can hold a breakthrough into %s: go up through the Immortal Gate.");
		t.add(ModLang.MSG_PORTAL_BLOCKED, "The laws of this realm reject the portal.");
		t.add(ModLang.SUPPRESSED, "Realm Suppression! Stats capped at:");
		t.add(ModLang.SUPPRESSED_TO, "%s");
		t.add(ModLang.BREAKTHROUGH_SEALED, "Only the Jade Firmament and the realms above it can hold this breakthrough.");

		// The creative tab
		t.add(ModLang.CREATIVE_TAB, "Defying The Heavens");

		// Keybinds
		t.add(ModLang.KEY_CATEGORY, "Defying The Heavens");
		t.add(ModLang.KEY_MENU, "Open Cultivation Menu");
		t.add(ModLang.KEY_MEDITATE, "Meditate (Lotus Position)");
		t.add(ModLang.KEY_CIRCULATE, "Circulate Qi (qi surges)");
		t.add(ModLang.KEY_ABILITY_WHEEL, "Ability Wheel (hold)");

		// Realms and stages
		for (Realm realm : Realm.values()) t.add(realm.getTranslationKey(), realmName(realm));
		for (Stage stage : Stage.values()) {
			t.add(stage.getTranslationKey(), switch (stage) {
				case EARLY -> "Early";
				case MID -> "Mid";
				case LATE -> "Late";
				case GRAND_PERFECTION -> "Grand Perfection";
			});
		}

		// Menu
		t.add(ModLang.TITLE, "Cultivation");
		t.add(ModLang.REALM, "Realm: %s");
		t.add(ModLang.STAGE, "Stage: %s");
		t.add(ModLang.CULTIVATION, "Cultivation: %s / %s");
		t.add(ModLang.CULTIVATION_MAX, "Cultivation: %s (Maximum)");
		t.add(ModLang.METHOD_LINE, "%s, layer %s of %s: +%s/s");
		t.add(ModLang.METHOD_LINE_NAME, "%s: +%s/s");
		t.add(ModLang.METHOD_LINE_SHORT, "Layer %s of %s: +%s/s");
		t.add(ModLang.METHOD_LINE_NONE, "No heaven method: +%s/s");
		t.add(ModLang.SETTLING_LINE, "Layer %s settling: %s%%");
		t.add(ModLang.UPPER_REALM_LINE, "Jade Firmament: qi x%s");
		t.add(ModLang.GREAT_VOID_LINE, "Great Void: qi x%s");
		t.add(ModLang.IMMORTAL_REALM_LINE, "Immortal Realm: immortal qi x%s");
		t.add(ModLang.HUD_QI, "%s / %s");
		t.add(ModLang.BOTTLENECK, "Bottleneck reached! Break through to advance.");
		t.add(ModLang.BOTTLENECK_NEXT, "Next: %s");
		t.add(ModLang.PINNACLE, "You stand at the pinnacle of cultivation.");
		t.add(ModLang.TAB_CULTIVATION, "Cultivation");
		t.add(ModLang.TAB_STATS, "Stats");
		t.add(ModLang.TAB_ABILITIES, "Abilities");
		t.add(ModLang.TAB_METHODS, "Methods");
		t.add(ModLang.TAB_SPELLS, "Spells");
		t.add(ModLang.STATS_SUPPRESSED, "is suppressed to");
		t.add(ModLang.STATS_COL_STAT, "Stat");
		t.add(ModLang.STATS_COL_BASE, "Base");
		t.add(ModLang.STATS_COL_BONUS, "Realm");
		t.add(ModLang.STATS_COL_CURRENT, "Current");
		t.add(ModLang.STATS_COL_TRUE, "True");
		t.add(ModLang.STATS_COL_NOW, "Now");
		t.add(ModLang.STAT_NAME_HEALTH, "Max Health");
		t.add(ModLang.STAT_NAME_DAMAGE, "Attack Damage");
		t.add(ModLang.STAT_NAME_SPEED, "Speed");
		t.add(ModLang.STAT_NAME_ARMOR, "Armor");
		t.add(ModLang.STAT_NAME_TOUGHNESS, "Toughness");
		t.add(ModLang.STAT_NAME_KNOCKBACK, "Knockback Res.");
		t.add(ModLang.STAT_NAME_MAX_QI, "Max Qi");
		t.add(ModLang.STAT_NAME_QI_GATHER, "Qi Gather");
		t.add(ModLang.STATS_QI_GATHER_UPPER, "Qi gather x%s in the Jade Firmament");
		t.add(ModLang.STATS_QI_GATHER_GREAT_VOID, "Qi gather x%s in the Great Void");
		t.add(ModLang.STATS_QI_GATHER_IMMORTAL, "Qi gather x%s in the Immortal Realm");
		t.add(ModLang.ABILITIES_TITLE, "Abilities");
		t.add(ModLang.ABILITIES_EMPTY, "No abilities awakened yet.");
		t.add(ModLang.ABILITIES_HINT, "Abilities you awaken will appear here.");
		t.add(ModLang.ABILITY_ON, "On");
		t.add(ModLang.ABILITY_OFF, "Off");
		t.add(ModLang.BTN_ARRANGE_WHEEL, "Arrange Wheel");
		t.add(ModLang.BTN_ABILITY_LIST, "Ability List");
		t.add(ModLang.WHEEL_TITLE, "Ability Wheel");
		t.add(ModLang.WHEEL_HINT, "Click: select (use with %s). Click again, right-click or its number: switch now.");
		t.add(ModLang.WHEEL_EMPTY, "Nothing on the wheel yet. Arrange it on the Abilities tab.");
		t.add(ModLang.WHEEL_EMPTY_SLOT, "Empty slot");
		t.add(ModLang.WHEEL_EDITOR_HINT, "Drag an ability onto a slot. Drag a slot onto another to swap them, or off the wheel to empty it.");
		t.add(ModLang.WHEEL_EDITOR_KEY, "Hold %s to open the wheel.");
		t.add(ModLang.WHEEL_EDITOR_PICK, "Drag onto a slot");
		for (Ability ability : Ability.values()) {
			String key = ModLang.abilityKey(ability.getId());
			t.add(key, switch (ability) {
				case QI_SUSTENANCE -> "Qi Sustenance";
				case QI_FLIGHT -> "Qi Flight";
				case QI_SENSE -> "Qi Sense";
				case CONSCIOUSNESS_DOMAIN -> "Consciousness Domain";
				case REALM_SUPPRESS -> "Realm Suppress";
				case INNER_REALM -> "Inner Realm";
				case SWORD_FLIGHT -> "Sword Flight";
				case SOUL_PROJECTION -> "Soul Projection";
				case GHOST_EYE -> "Ghost Eye";
				case NETHERWORLD_VEIL -> "Netherworld Veil";
				case YIN_WIND_STEPS -> "Yin Wind Steps";
				case GHOST_FLIGHT -> "Ghost Flight";
				case FORM_SHIFT -> "Form Shift";
				case CORE_DETONATION -> "Golden Core Detonation";
				case SPATIAL_TEAR -> "Spatial Tear";
				case BURN_LIFESPAN -> "Burn Lifespan";
				case CLONE -> "Clone";
				case METEOR_FLIGHT -> "Meteor Flight";
				case TIGERS_ROAR -> "Tiger's Roar";
				case ILLUSION -> "Illusion";
				case IMMORTAL_ESSENCE -> "Immortal Essence";
				case METAL_DOMAIN -> "Metal Domain";
				case WOOD_DOMAIN -> "Wood Domain";
				case WATER_DOMAIN -> "Water Domain";
				case FIRE_DOMAIN -> "Fire Domain";
				case EARTH_DOMAIN -> "Earth Domain";
				case SPACE_DOMAIN -> "Space Domain";
				case TIME_DOMAIN -> "Time Domain";
				case LIFE_AND_DEATH_DOMAIN -> "Life and Death Domain";
				case SMALL_WORLD -> "Small World";
				case SLEEVE -> "Universe in a Sleeve";
			});
			t.add(key + ".description", switch (ability) {
				case QI_SUSTENANCE -> "Qi keeps you fed. Hunger stays full.";
				case QI_FLIGHT -> "Double-tap jump to fly, at a cost of %s qi/s. Switching it on switches Sword Flight off.";
				case QI_SENSE -> "See the plane of Qi: the five elements drifting through a grey world. No qi cost.";
				case CONSCIOUSNESS_DOMAIN -> "Sense all life within %s blocks, even through walls. Cultivators show their realm. No qi cost.";
				case REALM_SUPPRESS -> "Your realm crushes weaker beings within %s blocks, harder the weaker they are. Costs qi per target.";
				case INNER_REALM -> "After 10 s of meditation your soul turns inward (+25% cultivation, and its years cost a fifth less). Walk freely; stop meditating to return. Harm to your body pulls you back.";
				case SWORD_FLIGHT -> "Hold a sword and double-tap jump to ride it, at a cost of %s qi/s. Cheaper than Qi Flight but slower (75%% of its speed, at every realm); the sword takes no wear. Switching it on switches Qi Flight off.";
				case SOUL_PROJECTION -> "Send your nascent soul out of your body (5 s to leave, 5 s to return) while your body sits on. It flies through walls on %s hearts (they mend only while it rests in a body, never while it's out), within %s blocks of your body, for %s-%s qi/s (more the farther it goes). Right-click a being a realm or more below you to possess it. If its tether is cut or your body slain, you die and fall to Core Formation.";
				case GHOST_EYE -> "See the dead and the Method Steles through walls within 32 blocks. Costs %s ghost qi/s.";
				case NETHERWORLD_VEIL -> "Wrap your body in the Netherworld's own yin: it seeps into you at half the rate. Costs %s ghost qi/s.";
				case YIN_WIND_STEPS -> "Walk on the yin wind: %s%% swifter, and falls do you no harm. Costs %s ghost qi/s.";
				case GHOST_FLIGHT -> "The dead fly at a drift (double-tap jump); with this, as swiftly as on qi, at a cost of %s ghost qi/s.";
				case FORM_SHIFT -> "Your true form is the form of your higher path: now your %s. Switched on, you take your %s instead. "
						+ "In a Ghost King's form you are seen as the dead are: outside the Netherworld, only by those who perceive souls. "
						+ "Dead, a Ghost King's true form is the King's, and its human form is seen by all as the living are. "
						+ "One who has reigned as a Ghost King keeps the King's form when born again. No cost.";
				case CORE_DETONATION -> "The last resort. Use it twice within 5 s and your golden core breaks: 3 s later it bursts, everything within %s blocks "
						+ "harmed by its distance and how far its realm stands below yours (a cultivator of your own realm in good armour may live). "
						+ "You die, and the core is lost: you return at Foundation Building Grand Perfection.";
				case SPATIAL_TEAR -> "Tear space open before you: a rift that stands 20 s, that you and anyone with you can walk through, into the Spatial Gap "
						+ "or out of it again into the realm you came from. There you fly freely, "
						+ "each block crossing %s in the world, under spatial pressure (worse in its stormy layers: rise through the upper ones to cross "
						+ "into the Jade Firmament, or sink slowly through the lower ones to fall into the mortal world). "
						+ "Costs %s%% of your qi to tear in, %s%% to tear out. From Void Shattering on, torn high in the Jade Firmament's sky "
						+ "(Y 448 and up) it opens a Void Breach into the Great Void Realm instead, for half your qi, held open while you stay "
						+ "by it; torn in the Great Void, a way down onto the Jade Firmament.";
				case BURN_LIFESPAN -> "Set your own years alight for strength: while it burns, your blows and spells strike %2$s%% harder and you move "
						+ "%3$s%% faster, for %1$s%% of your whole lifespan a second (the same share at every realm). It goes out by itself before "
						+ "it would leave you fewer than %4$s years, and when you die or log out.";
				case CLONE -> "A technique (from its manual). Use it to split a clone off your body, at your own realm with your abilities and "
						+ "spells but %1$s%% of your strength (health, blows, armour, qi pool); splitting it off takes %2$s%% of your qi. Use it "
						+ "again to go into the clone (your body sits down in meditation where it was) and again to come back (the clone stays "
						+ "standing). Sneak and use it within %3$s blocks of the other body to draw the clone back in, its qi and its things "
						+ "with it. It needs no tether and nothing to stay out; what it spends comes from your own pool. Slain, it dissolves and "
						+ "you wake in your body, and no other can be split off for %4$s minutes; if your body is slain, you die.";
				case METEOR_FLIGHT -> "Qi Flight's fast mode (switched off, you fly at normal speed at every realm, for fine control). Hold forward "
						+ "to fly where you look, laid out flat; sprint to speed up to %1$s blocks a second. Past the speed of sound you break "
						+ "the barrier with a boom. From Nascent Soul, holding top speed sets you ablaze, and you smash through what's in your way "
						+ "(not obsidian or anything harder, nor barriers or ground you couldn't dig). Flying fast costs %2$s qi/s (from Heavenly "
						+ "Being, never more than you gather). Let go of forward to hover and steer as usual. In the Spatial Gap, where it can't be "
						+ "flown, it gives your normal flight your realm's old swiftness instead (from Void Shattering); space keeps its own speeds.";
				case TIGERS_ROAR -> "Your tiger blood's roar: everything within %1$s blocks standing %2$s stages or more below you is weakened and "
						+ "slowed for %3$s s. Every %4$s s, for %5$s%% of your qi.";
				case ILLUSION -> "Your fox blood's illusion: unseen for %1$s s, until you strike. Every %2$s s, for %3$s%% of your qi.";
				case IMMORTAL_ESSENCE -> "Set your immortal essence to work: while it's on, your blows and spells strike %1$s%% harder, and a full "
						+ "pool lasts %2$s s. Your pool holds %3$s, and fills only in the Immortal Realm (or your small world): empty to full in %4$s minutes, %5$s times "
						+ "as fast meditating. Anywhere else it leaks away, a full pool in %6$s minutes: below the Gate an immortal keeps the might "
						+ "of its realm, but not its essence. It goes out when the pool runs dry, and when you die or log out.";
				case METAL_DOMAIN -> "Unfurl the domain of your Law of Metal, %1$s blocks round you. Each second every foe in it (monsters, whatever "
						+ "attacks you, those you're fighting) is cut for %4$s%% of your blow as metal qi, and its armour is cut by %5$s%% while it stays. "
						+ "Your metal spells strike foes in it %3$s%% harder." + DOMAIN_TAIL;
				case WOOD_DOMAIN -> "Unfurl the domain of your Law of Wood, %1$s blocks round you. Each second its roots drain every foe in it "
						+ "(monsters, whatever attacks you, those you're fighting) for %4$s%% of your blow as wood qi, %6$s%% of it back to you as "
						+ "life, and hold them slowed (Slowness %5$s). Your wood spells strike foes in it %3$s%% harder." + DOMAIN_TAIL;
				case WATER_DOMAIN -> "Unfurl the domain of your Law of Water, %1$s blocks round you. Each second its waters crush every foe in it "
						+ "(monsters, whatever attacks you, those you're fighting) for %4$s%% of your blow as water qi and slow them (Slowness %5$s); "
						+ "what burns on them goes out, and the fire they deal is %6$s%% weaker. Your water spells strike foes in it %3$s%% harder."
						+ DOMAIN_TAIL;
				case FIRE_DOMAIN -> "Unfurl the domain of your Law of Fire, %1$s blocks round you. Each second its true fire burns every foe in it "
						+ "(monsters, whatever attacks you, those you're fighting) for %4$s%% of your blow as fire qi, past any resistance or "
						+ "immunity to fire, and sets them alight. Your fire spells strike foes in it %3$s%% harder." + DOMAIN_TAIL;
				case EARTH_DOMAIN -> "Unfurl the domain of your Law of Earth, %1$s blocks round you. Each second its weight crushes every foe in it "
						+ "(monsters, whatever attacks you, those you're fighting) for %4$s%% of your blow as earth qi and slows them (Slowness %5$s); "
						+ "fliers are dragged down out of the air and can't fly in it, though spared the fall. Your earth spells strike foes in it "
						+ "%3$s%% harder." + DOMAIN_TAIL;
				case SPACE_DOMAIN -> "Unfurl the domain of your Law of Space, %1$s blocks round you. Space is locked on every foe in it (monsters, "
						+ "whatever attacks you, those you're fighting): none can step or tear through it, nothing it catches can leave it (it is held "
						+ "to its edge as you go), and all are slowed (Slowness %4$s). Your spells of space strike foes in it %3$s%% harder."
						+ DOMAIN_TAIL;
				case TIME_DOMAIN -> "Unfurl the domain of your Law of Time, %1$s blocks round you. Time runs slow for every foe in it (monsters, "
						+ "whatever attacks you, those you're fighting): they move (Slowness %3$s) and strike more slowly, and their spells cool "
						+ "%4$s%% more slowly." + DOMAIN_TAIL;
				case LIFE_AND_DEATH_DOMAIN -> "Unfurl the domain of your Law of Life and Death, %1$s blocks round you. No foe in it (monsters, "
						+ "whatever attacks you, those you're fighting) can heal at all, and %4$s%% of the harm you deal them comes back to you as "
						+ "life. Your ghost path spells strike foes in it %3$s%% harder." + DOMAIN_TAIL;
				case SMALL_WORLD -> "Open the door to your small world before you, for %1$s s: walk through it, and so may those on its guest list "
						+ "(/smallworld guest add). Its qi is the Immortal Realm's: your essence fills there, and meditating there comprehends your "
						+ "Law in focus (its breakthroughs are still the Immortal Realm's alone). Inside, use this again, or walk through its gate, to "
						+ "go out where you came in. Its land reaches %2$s blocks out, %3$s more for every level of the Laws you hold. Your world "
						+ "goes with you: anyone inside it when you take the Spatial Gap, the Void Breach or the Immortal Gate comes through with you, "
						+ "whatever their realm, and its gate then leads out beside you. As with Universe in a Sleeve, the spatial pressure on you "
						+ "doubles for each of them.";
				case SLEEVE -> "A technique (from its manual). Look at another player within %1$s blocks and use this to offer them a place in "
						+ "your sleeve (up to %2$s at once). If they accept, they vanish into it: safe, untouchable, seeing what you see. They go "
						+ "wherever you go, up through the Spatial Gap, the Void Breach and the Immortal Gate, whatever their realm, and come out "
						+ "beside you when you arrive in a world. You pay for it: the spatial pressure on you doubles for each person you carry "
						+ "(a True Immortal, which feels none at the Immortal Gate, takes %3$s%% of its body over the climb there, doubled the "
						+ "same way). Sneak and use it to let them out beside you (not partway through a climb). If you die, they're cast out "
						+ "where you fell.";
			});
		}
		// Clone while playing the clone (Clones): the way back.
		t.add(ModLang.abilityKey(Ability.CLONE.getId()) + ".description.clone", "You are in your clone. Use this to go back to your body "
				+ "(the clone stays standing where it is). Sneak and use it within %s blocks of your body to draw the clone back in. A clone "
				+ "can't meditate, project its soul, break through or detonate a core.");
		// Soul Projection while the soul is in another's body (Possession): the way out of it.
		t.add(ModLang.abilityKey(Ability.SOUL_PROJECTION.getId()) + ".description.leave", "You are in another's body, using it as your own: "
				+ "its things, its strength, its cultivation. Use this to leave it; your soul goes out where the body stands, and the body is "
				+ "its owner's again.");
		// From Void Shattering the soul's tether has no end (PlayerCultivation#soulTetherEndless): it costs what it does beside the body anywhere.
		t.add(ModLang.abilityKey(Ability.SOUL_PROJECTION.getId()) + ".description.endless", "Send your nascent soul out of your body (5 s to leave, "
				+ "5 s to return) while your body sits on. It flies through walls on %s hearts (they mend only while it rests in a body, never while "
				+ "it's out), as far from your body as you like: from Void Shattering its tether has no end. It costs %s qi/s wherever it goes "
				+ "(more while it possesses). Right-click a being a realm or more below you to possess it. If its tether is cut or your body slain, "
				+ "you die and fall to Core Formation.");
		t.add(ModLang.abilityKey(Ability.SOUL_PROJECTION.getId()) + ".description.ghost.endless", "Send your nascent soul, born of your ghost "
				+ "path, out of your body (5 s to leave, 5 s to return) while your body sits on. It flies through walls on %s hearts (they mend only "
				+ "while it rests in a body, never while it's out), as far from your body as you like: from Void Shattering its tether has no end. "
				+ "It costs %s ghost qi/s wherever it goes (more while it possesses). Right-click a being a realm or more below you to possess it. "
				+ "If its tether is cut or your body slain, you die and your ghost path falls to Core Formation.");
		// The realm's abilities awakened by the ghost path rather than the heaven path (TwoPaths#awakens): paid in its ghost qi, and
		// what is lost is the ghost path's.
		t.add(ModLang.abilityKey(Ability.SOUL_PROJECTION.getId()) + ".description.ghost", "Send your nascent soul, born of your ghost path, out of "
				+ "your body (5 s to leave, 5 s to return) while your body sits on. It flies through walls on %s hearts (they mend only while it rests "
				+ "in a body, never while it's out), within %s blocks of your body, "
				+ "for %s-%s ghost qi/s (more the farther it goes). Right-click a being a realm or more below you to possess it. If its tether is cut "
				+ "or your body slain, you die and your ghost path falls to Core Formation.");
		t.add(ModLang.abilityKey(Ability.CORE_DETONATION.getId()) + ".description.ghost", "The last resort. Use it twice within 5 s and your ghost "
				+ "path's core breaks: 3 s later it bursts, everything within %s blocks harmed by its distance and how far its realm stands below "
				+ "yours (a cultivator of your own realm in good armour may live). You die, and the core is lost: your ghost path returns to "
				+ "Foundation Building Grand Perfection.");
		t.add(ModLang.abilityKey(Ability.SPATIAL_TEAR.getId()) + ".description.ghost", "Tear space open before you: a rift that stands 20 s, that "
				+ "you and anyone with you can walk through, into the Spatial Gap or out of it again into the realm you came from. There you fly "
				+ "freely, each block crossing %s in the world, under spatial pressure (worse in its stormy layers: rise through the upper ones to "
				+ "cross into the Jade Firmament, or sink slowly through the lower ones to fall into the mortal world). Costs %s%% of your ghost qi "
				+ "to tear in, %s%% to tear out. From Void Shattering on, torn high in the Jade Firmament's sky (Y 448 and up) it opens a Void "
				+ "Breach into the Great Void Realm instead, for half your ghost qi, held open while you stay by it; torn in the Great Void, a way "
				+ "down onto the Jade Firmament.");
		t.add(ModLang.METHODS_TITLE, "Cultivation Methods");
		t.add(ModLang.METHODS_EMPTY, "No cultivation methods learned yet.");
		t.add(ModLang.METHODS_HINT, "Methods you learn will appear here.");
		t.add(ModLang.SPELLS_TITLE, "Spells");
		t.add(ModLang.SPELLS_EMPTY, "No spells learned yet.");
		t.add(ModLang.SPELLS_HINT, "Spells you learn will appear here.");
		// The ghost methods and their spells
		for (GhostMethod method : GhostMethod.values()) {
			t.add(ModLang.methodKey(method.getId()), switch (method) {
				case NETHERWORLD_BREATHING -> "Netherworld Breathing Art";
				case GHOST_FIRE_SCRIPTURE -> "Ghost Fire Scripture";
				case WANDERING_SOUL_STEPS -> "Wandering Soul Steps";
				case YELLOW_SPRINGS_MARROW -> "Yellow Springs Marrow Art";
				case GHOST_CORE_CANON -> "Ghost Core Canon";
				case NINE_NETHER_SOUL_SUTRA -> "Nine Nether Soul Sutra";
				case YAMA_JUDGEMENT -> "Yama's Judgement Scripture";
				case SIX_PATHS_CANON -> "Six Paths Reincarnation Canon";
				case TEN_THOUSAND_GHOSTS_UNION -> "Ten Thousand Ghosts Union Scripture";
				case VOID_RENDING_NETHER_CANON -> "Void-Rending Nether Canon";
				case HELL_FORGED_VESSEL_SUTRA -> "Hell-Forged Vessel Sutra";
				case UNDERWORLD_NIRVANA_SCRIPTURE -> "Underworld Nirvana Scripture";
				case NETHERWORLD_IMMORTAL_CANON -> "Netherworld Immortal Sovereign Canon";
			});
			t.add(ModLang.methodKey(method.getId()) + ".description", switch (method) {
				case NETHERWORLD_BREATHING -> "The first foundation of the ghost cultivators: breathe the Netherworld's yin and keep it. It carries "
						+ "your ghost path through Qi Refining, with a deeper ghost qi pool, a ghost's hearts, and longer among the dead for the living.";
				case GHOST_FIRE_SCRIPTURE -> "Cold fire drawn out of yin, that burns the dead worst of all. A ghost's blows strike harder.";
				case WANDERING_SOUL_STEPS -> "How the dead drift where the wind goes. A ghost moves swifter.";
				case YELLOW_SPRINGS_MARROW -> "The second foundation: steep the bones in the Yellow Springs' waters until the marrow runs with yin. "
						+ "It carries your ghost path from Qi Refining through Foundation Building.";
				case GHOST_CORE_CANON -> "The third foundation, the Ghost Kings' own: gather the yin into a core of cold fire. It carries your "
						+ "ghost path from Qi Refining through Core Formation, where the dead are kings.";
				case NINE_NETHER_SOUL_SUTRA -> "The fourth foundation, out of the depths of the Nine Nethers: a soul born again of yin. It carries "
						+ "your ghost path from Qi Refining through Nascent Soul.";
				case YAMA_JUDGEMENT -> "The fifth foundation, the Judges' own: weigh souls as the courts of the underworld weigh them. It carries "
						+ "your ghost path from Qi Refining through Heavenly Being.";
				case SIX_PATHS_CANON -> "The sixth foundation, the law of the Wheel itself: the six paths of rebirth turn within you. It carries "
						+ "your ghost path from Qi Refining all the way through Four Axis.";
				case TEN_THOUSAND_GHOSTS_UNION -> "The seventh foundation: the yin of every soul that ever passed the Ghost Gate, gathered into one Dao "
						+ "and made your own. It carries your ghost path from Qi Refining through Dao Integration.";
				case VOID_RENDING_NETHER_CANON -> "The eighth foundation: a ghost's hand closes on the void between the realms and tears it like "
						+ "cloth. Only a handful have ever read it. It carries your ghost path from Qi Refining through Void Shattering.";
				case HELL_FORGED_VESSEL_SUTRA -> "The ninth foundation: the soul forged in the fires of the lowest hell into a vessel no heaven "
						+ "can break. It carries your ghost path from Qi Refining through Sacred Vessel.";
				case UNDERWORLD_NIRVANA_SCRIPTURE -> "The tenth foundation: to die a last time in the Nine Nethers and come out of it beyond "
						+ "death. It carries your ghost path from Qi Refining through Entering Nirvana.";
				case NETHERWORLD_IMMORTAL_CANON -> "The last foundation, written by the Chief Judge's own hand: the dead's way to immortality. It "
						+ "carries your ghost path from Qi Refining all the way through True Immortal.";
			});
			t.add(ModLang.methodKey(method.getId()) + ".gains", switch (method) {
				case NETHERWORLD_BREATHING -> "Qi Refining: layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection. Layer 3: Ghost Eye. Layer 5: Ghostly "
						+ "Wail. Layer 6: Netherworld Veil. Every layer: +20 ghost qi.";
				case GHOST_FIRE_SCRIPTURE -> "Your ghost path opens its layers: 1-3 at Qi Refining, 4-6 at Foundation Building, 7-9 at Core Formation. "
						+ "Layer 1: Ghost Fire. Layer 4: Ghost Fire Bolt. Layer 7: the fire leaps to a second foe. Every layer: a ghost's blows +10%.";
				case WANDERING_SOUL_STEPS -> "Your ghost path opens its layers: 1-3 at Qi Refining, 4-6 at Foundation Building, 7-9 at Core Formation. "
						+ "Layer 1: Yin Wind Steps. Layer 2: Soul Dash. Layers 4-9: the free flight of the dead and of a Ghost King's form "
						+ "+10% swifter each (+60% at 9). Every layer: a ghost +3% speed.";
				case YELLOW_SPRINGS_MARROW -> "Qi Refining to Foundation Building, nine layers each: of each realm's nine, layer 1 Early, 3 Mid, 6 "
						+ "Late, 9 Grand Perfection. Layer 12: Yellow Springs Tide. Layer 16: Marrow Ward. Every layer: +20 ghost qi.";
				case GHOST_CORE_CANON -> "Qi Refining to Core Formation, nine layers each: of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 "
						+ "Grand Perfection. Layer 21: Ghost Core Burst. Layer 25: King's Decree. Every layer: +20 ghost qi.";
				case NINE_NETHER_SOUL_SUTRA -> "Qi Refining to Nascent Soul, nine layers each: of each realm's nine, layer 1 Early, 3 Mid, 6 Late, "
						+ "9 Grand Perfection. Layer 30: Soul Rend. Layer 34: Nine Nether Step. Every layer: +20 ghost qi.";
				case YAMA_JUDGEMENT -> "Qi Refining to Heavenly Being, nine layers each: of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 "
						+ "Grand Perfection. Layer 39: Yama's Verdict. Layer 43: Chains of Hell. Every layer: +20 ghost qi.";
				case SIX_PATHS_CANON -> "Qi Refining to Four Axis, nine layers each: of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand "
						+ "Perfection. Layer 48: Wheel of Samsara. Layer 52: Six Paths Annihilation. Every layer: +20 ghost qi.";
				case TEN_THOUSAND_GHOSTS_UNION -> "Qi Refining to Dao Integration, nine layers each: of each realm's nine, layer 1 Early, 3 Mid, "
						+ "6 Late, 9 Grand Perfection. Layer 57: Procession of Ten Thousand Ghosts. Layer 61: Gate of Ten Thousand Ghosts. Every "
						+ "layer: +20 ghost qi.";
				case VOID_RENDING_NETHER_CANON -> "Qi Refining to Void Shattering, nine layers each: of each realm's nine, layer 1 Early, 3 Mid, "
						+ "6 Late, 9 Grand Perfection. Layer 66: Rift to the Nine Nethers. Layer 70: Bridge of No Return. Every layer: +20 ghost qi.";
				case HELL_FORGED_VESSEL_SUTRA -> "Qi Refining to Sacred Vessel, nine layers each: of each realm's nine, layer 1 Early, 3 Mid, "
						+ "6 Late, 9 Grand Perfection. Layer 75: Mirror of Retribution. Layer 79: Soul-Locking Pagoda. Every layer: +20 ghost qi.";
				case UNDERWORLD_NIRVANA_SCRIPTURE -> "Qi Refining to Entering Nirvana, nine layers each: of each realm's nine, layer 1 Early, 3 "
						+ "Mid, 6 Late, 9 Grand Perfection. Layer 84: Black and White Impermanence. Layer 88: Book of Life and Death. Every layer: +20 "
						+ "ghost qi.";
				case NETHERWORLD_IMMORTAL_CANON -> "Qi Refining to True Immortal, nine layers each: of each realm's nine, layer 1 Early, 3 Mid, "
						+ "6 Late, 9 Grand Perfection. Layer 93: Fengdu's Walls Rise. Layer 97: Ten Thousand Souls Ascend. Every layer: +20 ghost qi.";
			});
		}
		for (Spell spell : Spell.values()) {
			if (spell.isHeaven()) continue; // the heaven methods' are with HeavenLangText
			t.add(ModLang.spellKey(spell.getId()), switch (spell) {
				case GHOST_FIRE -> "Ghost Fire";
				case GHOST_FIRE_BOLT -> "Ghost Fire Bolt";
				case SOUL_DASH -> "Soul Dash";
				case GHOSTLY_WAIL -> "Ghostly Wail";
				case YELLOW_SPRINGS_TIDE -> "Yellow Springs Tide";
				case MARROW_WARD -> "Marrow Ward";
				case GHOST_CORE_BURST -> "Ghost Core Burst";
				case KINGS_DECREE -> "King's Decree";
				case SOUL_REND -> "Soul Rend";
				case NINE_NETHER_STEP -> "Nine Nether Step";
				case YAMA_VERDICT -> "Yama's Verdict";
				case HELL_CHAINS -> "Chains of Hell";
				case SAMSARA_WHEEL -> "Wheel of Samsara";
				case SIX_PATHS_ANNIHILATION -> "Six Paths Annihilation";
				case PROCESSION_OF_TEN_THOUSAND_GHOSTS -> "Procession of Ten Thousand Ghosts";
				case GATE_OF_TEN_THOUSAND_GHOSTS -> "Gate of Ten Thousand Ghosts";
				case HEAVEN_DEMON_PALM -> "Heaven-Demon Palm";
				case HEAVEN_DEMON_MANTLE -> "Heaven-Demon Mantle";
				case DEVOURING_SEAL -> "Devouring Seal";
				case HEAVEN_DEMON_CLAW -> "Heaven-Demon Claw";
				case HEAVEN_AND_DEMON_AS_ONE -> "Heaven and Demon as One";
				case HEAVEN_DEMON_AVATAR -> "Heaven-Demon Avatar";
				case RIFT_TO_THE_NINE_NETHERS -> "Rift to the Nine Nethers";
				case BRIDGE_OF_NO_RETURN -> "Bridge of No Return";
				case HEAVEN_DEMON_VOID_SHATTER -> "Heaven-Demon Void Shatter";
				case MIRROR_OF_RETRIBUTION -> "Mirror of Retribution";
				case SOUL_LOCKING_PAGODA -> "Soul-Locking Pagoda";
				case HEAVEN_DEMON_BELL -> "Heaven-Demon Bell";
				case BLACK_AND_WHITE_IMPERMANENCE -> "Black and White Impermanence";
				case BOOK_OF_LIFE_AND_DEATH -> "Book of Life and Death";
				case HEAVEN_DEMON_ECLIPSE -> "Heaven-Demon Eclipse";
				case FENGDU_WALLS_RISE -> "Fengdu's Walls Rise";
				case TEN_THOUSAND_SOULS_ASCEND -> "Ten Thousand Souls Ascend";
				case PRIMORDIAL_CHAOS -> "Primordial Chaos";
				default -> throw new IllegalStateException(spell.getId()); // a heaven spell: skipped above
			});
			t.add(ModLang.spellKey(spell.getId()) + ".description", switch (spell) {
				case GHOST_FIRE -> "Wreathe your hands in cold fire: your next 3 blows within 20 seconds set what they strike burning (3 ghost qi each). "
						+ "It burns the dead three times as hard.";
				case GHOST_FIRE_BOLT -> "Throw a bolt of ghost fire up to 24 blocks: it strikes the first being in its path and sets it burning.";
				case SOUL_DASH -> "Rush forward on the yin wind, about eight blocks, your own ghost left along the way. Falls do you no harm for a moment after.";
				case GHOSTLY_WAIL -> "Your own ghost rises giant at your back and wails: everything within 8 blocks is driven back, slowed and weakened, and the dead are torn at.";
				case YELLOW_SPRINGS_TIDE -> "A surge of the Yellow Springs' water rushes 7 blocks ahead: all it reaches is driven back and slowed, "
						+ "and the dead take 3 hearts.";
				case MARROW_WARD -> "Your bones hold like iron for 10 seconds: Resistance II and 4 hearts of absorption.";
				case GHOST_CORE_BURST -> "Your core flares: everything within 5 blocks takes 4 hearts of ghostly harm, is thrown back and set "
						+ "burning with ghost fire.";
				case KINGS_DECREE -> "A Ghost King's word: every foe within 16 blocks is weakened, slowed and shown glowing for 8 seconds.";
				case SOUL_REND -> "A beam of the Nine Nethers' dark, 32 blocks: every being in its line takes 8 hearts of ghostly harm.";
				case NINE_NETHER_STEP -> "Step through the dark to where you look, up to 24 blocks, short of any wall. Falls do you no harm for a "
						+ "moment after.";
				case YAMA_VERDICT -> "Judge the being you look at, up to 32 blocks: 10 hearts of ghostly harm, twice that if it is below half "
						+ "its health.";
				case HELL_CHAINS -> "A way to hell tears open at your feet and chains of black iron lash out of it to every foe within 12 blocks (with none, the nearest living thing your spells may take), haul them to you and hold them fast for 3 seconds.";
				case SAMSARA_WHEEL -> "The six paths turn: your health is made whole, your harmful effects are gone, and half your yin "
						+ "corruption with them.";
				case SIX_PATHS_ANNIHILATION -> "Six pillars of cold fire rise round you: everything within 12 blocks takes 20 hearts of ghostly "
						+ "harm and is hurled away.";
				// Dao Integration's: their numbers as the realm's heaven spells give theirs (health, not hearts; sizes by its realm).
				case PROCESSION_OF_TEN_THOUSAND_GHOSTS -> "A column of the dead, ghost fire and mist, pours out along your look for 61 blocks, "
						+ "8 wide: every being it passes through takes 90 ghostly harm and is slowed and weakened for 5 s. The dead take half as much "
						+ "again.";
				case GATE_OF_TEN_THOUSAND_GHOSTS -> "A gate of the dead, bone-white mist and ghost fire, opens where you look, up to 82 blocks, "
						+ "for 8 s: every foe within 18 blocks of it is dragged toward it and takes 30 ghostly harm a second. Then it slams shut: 120 "
						+ "ghostly harm to everything within 18 blocks. A quarter of all the harm it does mends you.";
				case HEAVEN_DEMON_PALM -> "A spell of both paths, heaven and demon. A palm of gold light, a demon's crimson claw within it, strikes "
						+ "the first being within 16 blocks: 5 hearts of harm, and it is thrown back. A quarter of the harm mends you.";
				case HEAVEN_DEMON_MANTLE -> "A spell of both paths, heaven and demon. Gold light and crimson demon scales wrap you for 10 seconds: "
						+ "arrows and fireballs shot at you are turned back on whoever shot them, and you have Resistance I.";
				case DEVOURING_SEAL -> "A spell of both paths, heaven and demon. Seal the being you look at, up to 32 blocks: heaven's seal holds "
						+ "it while a demon's maw devours it, for 8 seconds. It loses 2 hearts a second and is slowed, and half of it comes to you.";
				case HEAVEN_DEMON_CLAW -> "A spell of both paths, heaven and demon. A great claw of gold light and crimson demon qi rakes down where "
						+ "you look, up to 32 blocks: everything within 5 blocks takes 15 hearts of harm, is thrown up and weakened for 5 seconds.";
				case HEAVEN_AND_DEMON_AS_ONE -> "A spell of both paths, heaven and demon made one. Everything within 16 blocks is dragged in and "
						+ "takes 25 hearts of harm; a tenth of all of it mends you, and you have Strength II for 10 seconds.";
				case HEAVEN_DEMON_AVATAR -> "A spell of both paths, heaven and demon. A colossal projection of your own body, 41 blocks tall, rises out of a cloud of gold "
						+ "and crimson behind you for 8 s: its right side heaven's gold, haloed, its hand held up in heaven's sign; its left the demon's "
						+ "crimson, horned in fire, clawed; a seam of fire burning down it where the two meet. Twice it raises a hand and that hand comes "
						+ "down out of the sky where you look, up to 82 blocks: heaven's palm, then the demon's claw. Each strikes everything within 14 "
						+ "blocks for 140 harm and throws it down, slowed; a fifth of all the harm mends you. Heaven's palm crushes a crater 18 blocks "
						+ "round and 8 deep into the land, cracks running out of it to 39 blocks; the demon's claw rakes four gouges 28 blocks long across "
						+ "where it struck.";
				// Void Shattering's.
				case RIFT_TO_THE_NINE_NETHERS -> "The ground where you look, up to 118 blocks, tears open onto the Nine Nethers, nine rings of ghost "
						+ "fire turning down into it, for 6 s: every foe within 22 blocks is dragged toward it and down, and takes 100 ghostly harm a "
						+ "second. Then it seals: 300 ghostly harm to everything within 8 blocks of it, and Weakness II for 8 s. The dead take half as "
						+ "much again. The land is left as it was.";
				case BRIDGE_OF_NO_RETURN -> "The Naihe Bridge, that the dead cross over the River of Forgetting, rises out of the mist along your "
						+ "look, span after span, 118 blocks: everything within 6 blocks of it as it rises takes 300 ghostly harm and forgets every "
						+ "blessing on it (its good effects are gone). Then for 5 s whoever is on or under it is slowed and pays its toll, 50 ghostly "
						+ "harm a second. The dead take half as much again.";
				case HEAVEN_DEMON_VOID_SHATTER -> "A spell of both paths, heaven and demon. A colossal fist, heaven's gauntlet of gold plates round a "
						+ "demon's crimson claws, forms over your shoulder and punches out to where you look, up to 118 blocks: where it lands, space "
						+ "itself shatters like glass. Everything within 18 blocks takes 520 harm, through armour, and is thrown out; the shattered "
						+ "space hangs there for 5 s, cutting whoever stays in it for 60 harm a second. The land under it is smashed into a crater 28 "
						+ "blocks round and 15 deep, twelve cracks running out of it to 55 blocks.";
				// Sacred Vessel's.
				case MIRROR_OF_RETRIBUTION -> "The karma mirror of Yama's court, that shows every soul its sins, rises over you, behind your head, "
						+ "and turns with your look for 3 s: its light pours out over you along your look, 64 blocks, and every being in it is shown its "
						+ "sins: it takes 140 ghostly "
						+ "harm a second, is weakened and slowed, and is shown glowing wherever it hides for 8 s. Then the mirror flashes: 500 ghostly "
						+ "harm to everything in its light. The dead take half as much again.";
				case SOUL_LOCKING_PAGODA -> "A pagoda of seven tiers, black iron and bone forged in the lowest hell, its eaves hung with soul "
						+ "lanterns, falls out of the sky onto where you look, up to 136 blocks: 300 ghostly harm to everything under it. Every foe "
						+ "within 15 blocks of it is locked in it for 6 s: it can't leave (it is brought back to its walls) and takes 120 ghostly harm "
						+ "a second, ghost fire pouring from the eaves. Then it seals: 500 ghostly harm to all locked in it, and Weakness II for 10 s. "
						+ "The dead take half as much again. The land is left as it was.";
				case HEAVEN_DEMON_BELL -> "A spell of both paths, heaven and demon. A colossal bell, heaven's gold on one side and the demon's "
						+ "crimson bronze on the other, falls out of the sky over where you look, up to 136 blocks, its rim pressing a crater 19 blocks "
						+ "round into the land, and tolls three times, a second apart: each toll strikes everything under it, within 19 blocks, for 500 "
						+ "harm, through armour, and its wave runs out over the land to 38 blocks, striking everything there for 200 and throwing it "
						+ "back, the land cracking under it out to 40 blocks. A fifth of all the harm mends you. Then it rises away.";
				// Entering Nirvana's.
				case BLACK_AND_WHITE_IMPERMANENCE -> "The two soul-catchers of the underworld rise out of the mist to either side of the soul you look at, up to 72 blocks, and come "
						+ "for it: the White, who smiles, in white mourning robes and a tall hat, its long red tongue hanging from its mouth, brings its "
						+ "mourning staff down on it, 500 ghostly harm, slowing it; the Black, who scowls, in black, red-eyed, thrusts out its hand and its "
						+ "chain lashes out and binds it, holding it for 3 s at 300 ghostly harm a second; and then both reach in and take their share of "
						+ "its soul, a tenth of its whole health (no more than 1200). The dead take half as much again.";
				case BOOK_OF_LIFE_AND_DEATH -> "The Book of Life and Death, in which every soul's span is written, opens over your head, its judge's "
						+ "brush red with cinnabar, and finds the names of the foes round you, eight at most within 72 blocks, nearest first: each is "
						+ "marked, shown glowing wherever it is, and for 3 s takes 250 ghostly harm a second. Then the brush strikes their names out: "
						+ "900 ghostly harm to each, and any left with less than a quarter of its whole health is struck out of life (a great one of "
						+ "the world, the Ender Dragon, the Wither, the Warden, takes 2000 more instead). The dead take half as much again.";
				case HEAVEN_DEMON_ECLIPSE -> "A spell of both paths, heaven and demon. Heaven's sun and the demon's moon come together 58 blocks over where you look, up to "
						+ "154 blocks, the black moon sliding across the blazing sun until it covers it, its corona flaring out round them; then a beam of "
						+ "darkness edged with crimson and gold pours straight down from the moon's face to the ground, as wide as its harm reaches, and "
						+ "pulses three times, three quarters of a second apart: each strikes everything under it, within 22 blocks, for 1200, then 600, "
						+ "then 600 harm, through armour, holding it where it stands, and the first burns the land into a crater of black glass 19 blocks "
						+ "round and 7 deep, ash and embers welling up about it. A fifth of all the harm mends you.";
				// True Immortal's.
				case FENGDU_WALLS_RISE -> "Eight lengths of the walls of Fengdu, the city of the dead, black stone, crenellated, a gate-tower at "
						+ "the middle of each and soul lanterns at their ends, rise out of the land in a ring 37 blocks round where you look, up to "
						+ "136 blocks away, facing in: for 5 s no foe within gets out past them, and every half second the city's dead fall on every "
						+ "foe within, 800 ghostly harm, slowing them. Then the walls topple in on its heart: 4000 ghostly harm to every foe within. "
						+ "The dead take half as much again.";
				case TEN_THOUSAND_SOULS_ASCEND -> "A pit of the dark opens where you look, up to 136 blocks away, and a whirlwind of ghost fire winds up out of it, the souls of "
						+ "the dead pouring up it by the hundred, wailing faces as they rise out of the dark that burn away into wisps of cold blue fire as "
						+ "they climb. For 5 s every foe within 34 blocks is drawn in to it, and those within 14 carried up round it as high as 20 (a great "
						+ "beast only drawn, slowly), the souls tearing at them, 1000 ghostly harm every half second. Then it is dragged back down into the "
						+ "dark, dragging them down with it: 5000 ghostly harm to each. The dead take half as much again.";
				case PRIMORDIAL_CHAOS -> "A spell of both paths, heaven and demon. A sphere of the chaos before heaven and earth were parted, heaven's "
						+ "gold and the demon's crimson swirled together, swells out of a point 24 blocks over where you look, up to 218 blocks "
						+ "away, and churns for 5 s: everything within 54 blocks is drawn in toward it, and every half second everything within 27 "
						+ "blocks of its heart takes 1000 harm, through armour. Then it bursts: 15000 harm to everything within 34 blocks, through "
						+ "armour, thrown out, the land under it burnt into a crater of glass 24 blocks round and 10 deep. A fifth of all the harm "
						+ "mends you.";
				default -> throw new IllegalStateException(spell.getId()); // a heaven spell: skipped above
			});
		}
		t.add(ModLang.SPELLS_QI_COST, "%s qi");
		t.add(ModLang.SPELLS_LOCKED_BOTH, "Heaven and Demon Paths at %s");
		t.add(ModLang.MSG_SPELL_BOTH_PATHS, "New spell: %2$s (the heaven path and the Demon Path at %1$s).");
		t.add(ModLang.STATS_GHOST_BODY, "Ghost path: %s");
		t.add(ModLang.STATS_HARMONY, ", harmony +%s%%");
		t.add(ModLang.TWO_PATHS_LINE, "Paths in harmony: +%s%%");
		t.add(ModLang.HEAVEN_PATH_LINE, "Heaven path: %s");
		t.add(ModLang.VESSEL_INVENTORY, "%s's body");
		t.add(ModLang.EFFECTIVE_LINE, "Effective cultivation: %s");
		t.add(ModLang.EFFECTIVE_QUASI_STAGE, "%s - Quasi-%s");
		t.add(ModLang.EFFECTIVE_QUASI_REALM, "Quasi-%s");
		t.add(ModLang.EFFECTIVE_BEYOND, "%s - beyond %s");
		t.add(ModLang.EFFECTIVE_TOWARD, "%s (%s%%)");
		t.add(ModLang.METHOD_BAR, "%s, layer %s of %s");
		t.add(ModLang.METHOD_BAR_SHORT, "%s, %s of %s");
		t.add(ModLang.CULTIVATION_SHORT, "%s / %s");
		t.add(ModLang.CULTIVATION_MAX_SHORT, "%s (Max)");
		t.add(ModLang.METHOD_BAR_LAYERS, "Layer %s of %s");
		t.add(ModLang.GHOST_COLUMN_NO_QI, "No ghost qi here");
		t.add(ModLang.GHOST_COLUMN_PACE, "%s: +%s/s");
		t.add(ModLang.GHOST_COLUMN_PACE_SHORT, "+%s/s");
		t.add(ModLang.GHOST_COLUMN_PACE_NONE, "No ghost method: +%s/s");
		t.add(ModLang.COLUMN_RESTING, "Resting (not drawn in)");
		t.add(ModLang.GHOST_BOTTLENECK, "Your ghost path's bottleneck: break through to %s (the Netherworld's tribulation).");
		t.add(ModLang.GHOST_BOTTLENECK_SWITCH, "Your ghost path's bottleneck: draw in ghostly qi (Path: Ghost) to break through to %s.");
		t.add(ModLang.BOTTLENECK_SWITCH, "Heaven path's bottleneck! Draw in the heavens' qi (Path: Heaven) to break through.");
		t.add(ModLang.GHOST_PINNACLE, "Your ghost path stands at its pinnacle.");
		t.add(ModLang.GHOST_GATE_NONE, "Your ghost path needs a foundation of %s or deeper: seek a Method Stele, a Ghost Jade Slip or a ghost who teaches.");
		t.add(ModLang.BTN_PATH_HEAVEN, "Path: Heaven");
		t.add(ModLang.BTN_PATH_GHOST, "Path: Ghost");
		t.add(ModLang.MSG_PATH_HEAVEN, "You draw in the heavens' qi: meditating cultivates your heaven path.");
		t.add(ModLang.MSG_PATH_GHOST, "You draw in ghostly qi: meditating cultivates your ghost path.");
		t.add(ModLang.MSG_PATH_NO_GHOST, "You have no ghost path to cultivate: learn a ghost foundation first, or begin it among the dead.");
		t.add(ModLang.MSG_GHOST_LAYER, "%s: layer %s settles in.");
		t.add(ModLang.MSG_GHOST_LAYER_SPELL, "New spell: %3$s (%1$s, layer %2$s).");
		t.add(ModLang.MSG_GHOST_PATH_RISES, "Your ghost path rises: %s!");
		t.add(ModLang.MSG_GHOST_PATH_BEGINS, "Drawing in ghostly qi, your ghost path begins: %s. It stays yours, even if you return to the living.");
		t.add(ModLang.MSG_GHOST_BOTTLENECK, "Your ghost path is at its bottleneck: break through in the cultivation menu to face the Netherworld's tribulation.");
		t.add(ModLang.MSG_GHOST_TRIB_START, "The Netherworld answers your ghost path's rise to %s! Survive %s strikes of yin lightning!");
		t.add(ModLang.MSG_GHOST_TRIB_START_SINGLE, "The Netherworld answers your ghost path's rise to %s! Survive its strike of yin lightning!");
		t.add(ModLang.MSG_GHOST_BREAKTHROUGH, "The Netherworld's tribulation survived: your ghost path stands at %s!");
		t.add(ModLang.MSG_GHOST_TRIB_FALL, "Backlash! Your ghost path falls to %s.");
		t.add(ModLang.MSG_GHOST_TRIB_FLED, "You fled the Netherworld's tribulation. Backlash! Your ghost path falls to %s.");
		t.add(ModLang.TRIB_HUD_GHOST, "The Netherworld's Tribulation (%s) - strikes left: %s");
		t.add(ModLang.METHODS_GHOST_RESTING, "You draw in the heavens' qi: your ghost path and its methods rest. Switch to ghostly qi on the Cultivation tab to cultivate them.");
		t.add(ModLang.METHODS_GHOST_NEXT, "The %s is understood as far as your ghost path: its next layer opens at %s.");
		t.add(ModLang.METHODS_GHOST_REACH, "Your foundations open the ghost path's way as far as %s.");
		t.add(ModLang.DEATH_YIN_LIGHTNING, "%1$s was struck down by the Netherworld's tribulation");
		t.add(ModLang.DEATH_YIN_LIGHTNING + ".player", "%1$s was struck down by the Netherworld's tribulation while fighting %2$s");
		t.add(ModLang.DEATH_YIN_TRIBULATION, "%1$s was unmade by the Netherworld's yin");
		t.add(ModLang.DEATH_YIN_TRIBULATION + ".player", "%1$s was unmade by the Netherworld's yin while fighting %2$s");
		t.add(ModLang.FORM_PREFIX + "human", "human form");
		t.add(ModLang.FORM_PREFIX + "ghost_king", "Ghost King form");
		t.add(ModLang.MSG_FORMS_OPEN, "Your ghost path reached Core Formation: Form Shift (Abilities) takes a Ghost King's form.");
		t.add(ModLang.MSG_FORMS_KEPT, "You reigned as a Ghost King among the dead: its form is still yours. Form Shift (Abilities) takes it.");
		t.add(ModLang.MSG_METHOD_MERIDIANS, "Your meridians are sealed: the ghost path, like the heaven path, opens only once a Marrow "
				+ "Cleansing Elixir has opened them.");
		t.add(ModLang.MSG_METHOD_MERIDIANS_DEAD, "Your meridians were never opened in life: the ghost path, like the heaven path, waits on a "
				+ "Marrow Cleansing Elixir. The dead can drink one too, at Mortal Peak (fighting and running temper a ghost as they do the "
				+ "living).");
		t.add(ModLang.MSG_TRUE_FORM, "Your true form is now your %s.");
		t.add(ModLang.MSG_FORM_TAKEN, "You take your %s.");
		t.add(ModLang.JOURNAL_FORMS, "Two forms");
		t.add(ModLang.JOURNAL_FORMS_TEXT, "My ghost path reached Core Formation, and I found a second form in me: a Ghost King's, crowned in "
				+ "soul fire. While I live, the form of whichever path stands higher is my true one, and the other I can take at will "
				+ "(Form Shift); in the Ghost King's, outside the Netherworld only those who perceive souls see me. Dead, as a Ghost King, "
				+ "it is my human form I take.");
		t.add(ModLang.KEY_SPELL_WHEEL, "Spell Wheel (hold)");
		t.add(ModLang.KEY_CAST_SPELL, "Cast Readied Spell");
		t.add(ModLang.MSG_METHOD_LEARNED, "Learned the %s.");
		t.add(ModLang.MSG_METHOD_LEARNED_AT, "Learned the %s, understood to layer %s.");
		t.add(ModLang.MSG_METHOD_PRACTISED, "Your meditation now leans on the %s.");
		t.add(ModLang.MSG_GHOST_QI_SPENT, "Your ghost qi is spent. Your ghostly arts fade.");
		t.add(ModLang.MSG_STELE_TEACHES, "This stele holds the %s. Meditate beside it to learn it.");
		t.add(ModLang.MSG_STELE_KNOWN, "This stele holds the %s, which you know. Meditating beside it while you practise it is faster.");
		t.add(ModLang.MSG_STELE_READING, "You read the %s carved in the stele...");
		t.add(ModLang.MSG_SLIP_KNOWN, "You already know the %s.");
		t.add(ModLang.ITEM_GHOST_JADE_SLIP_OF, "Ghost Jade Slip: %s");
		t.add(ModLang.TOOLTIP_GHOST_JADE_SLIP, "Read it to learn the method");
		t.add(ModItems.GHOST_JADE_SLIP, "Ghost Jade Slip");
		t.add(ModBlocks.METHOD_STELE, "Method Stele");
		t.add(ModEffects.GHOST_FIRE, "Ghost Fire");
		t.add(ModLang.MSG_SPELL_NONE_READY, "No spell readied. Hold the spell wheel key and click one.");
		t.add(ModLang.MSG_SPELL_NOT_HERE, "The river's flow won't let you shape a spell.");
		t.add(ModLang.MSG_SPELL_COOLING, "%s: ready again in %s s.");
		t.add(ModLang.MSG_SPELL_NO_QI, "Not enough ghost qi for %s.");
		t.add(ModLang.MSG_SPELL_READIED, "%s readied.");
		t.add(ModLang.MSG_GHOST_FIRE_ARMED, "Ghost fire wreathes your hands: your next %s blows set what they strike burning.");
		t.add(ModLang.METHODS_KNOWN, "%s of %s methods known");
		t.add(ModLang.METHODS_LAYER, "Layer %s of %s");
		t.add(ModLang.METHODS_LAYER_REALM, "Layer %s of %s: %s, %s of 9");
		t.add(ModLang.METHODS_PERFECTED, "Perfected");
		t.add(ModLang.METHODS_NO_QI, "There is no ghostly qi here to cultivate with. It flows faintest in the Overworld, thicker in the Nether (thickest in its Soul Sand Valleys), and ten times as dense in the Netherworld.");
		t.add(ModLang.BTN_SPELL_LIST, "Spell List");
		t.add(ModLang.LIFE_MODE_HEALTH, "Moves: Health");
		t.add(ModLang.LIFE_MODE_QI, "Moves: Qi");
		t.add(ModLang.LIFE_MODE_YEARS, "Moves: Years");
		t.add(ModLang.MSG_LIFE_MODE, "Life Force Drain and Gain now move %s.");
		t.add(ModLang.MSG_LIFE_NO_YEARS, "It has no years of life to draw on, nor to be given: only a player keeps a lifespan.");
		t.add(ModLang.MSG_LIFE_NO_QI, "You have no qi left to give.");
		t.add(ModLang.MSG_LIFE_YEARS_GIFTED, "%s years of your life given to %s.");
		t.add(ModLang.MSG_LIFE_YEARS_RECEIVED, "%s gave you %s years of their life.");
		t.add(ModLang.MSG_BOUGH_NO_FRUIT, "Hold the Cultivation Fruit you would return to the bough.");
		t.add(ModLang.MSG_BOUGH_NO_TREE, "Look at the leaves of a tree with room beneath them to hang it.");
		t.add(ModLang.MSG_BOUGH_HUNG, "%s Cultivation Fruit returned to the bough: they ripen on from %s years.");
		t.add(ModLang.BTN_GRIEFING_ON, "Grief: On");
		t.add(ModLang.BTN_GRIEFING_OFF, "Grief: Off");
		t.add(ModLang.TIP_GRIEFING, "Whether spells change the land for good. On: craters, cuts, lakes and floods, ice, and fire that spreads. Off: the land is left as it was; spell fire catches nothing and puts itself out, and the water they pour drains away. (The gamerule spellGriefing.)");
		t.add(ModLang.TIP_GRIEFING_LOCKED, "Whether spells change the land for good (the gamerule spellGriefing). Only an operator or the world's host may change it.");
		t.add(ModLang.MSG_GRIEFING_ON, "%s turned spell grief on: spells change the land for good.");
		t.add(ModLang.MSG_GRIEFING_OFF, "%s turned spell grief off: spells leave the land as it was.");
		t.add(ModLang.METHODS_GHOST_PATH, "Ghost path: %s");
		t.add(ModLang.METHODS_GHOST_PATH_NONE, "Ghost path: not yet begun");
		t.add(ModLang.METHODS_GHOST_PATH_SEALED, "Ghost path: sealed, your meridians not yet opened (a Marrow Cleansing Elixir)");
		t.add(ModLang.METHODS_UNKNOWN_DEEP, "Unknown method. It is seldom carved in stone, and seldom taught.");
		t.add(ModLang.METHODS_ACTIVE, "Practising");
		t.add(ModLang.METHODS_UNKNOWN, "Unknown method. Its stele stands somewhere in the Netherworld.");
		t.add(ModLang.METHODS_GAINS, "What its layers bring");
		t.add(ModLang.BTN_PRACTISE, "Practise");
		t.add(ModLang.SPELLS_COST, "%s ghost qi");
		t.add(ModLang.SPELLS_COOLDOWN, "%s s cooldown");
		t.add(ModLang.SPELLS_LOCKED, "%s, layer %s");
		t.add(ModLang.SPELLS_LOCKED_REALM, "%s, layer %s (%s, %s of 9)");
		t.add(ModLang.SPELLS_LOCKED_STAGE, "...and your cultivation at %s");
		t.add(ModLang.SPELLS_READY, "Ready");
		t.add(ModLang.SPELLS_READIED, "Readied");
		t.add(ModLang.SPELLS_COOLING, "Ready in %s s");
		t.add(ModLang.SPELLS_ON_WHEEL, "Spell wheel");
		t.add(ModLang.SPELLS_ARRANGE_HINT, "Drag a spell onto a slot of the wheel. Drag a slot off the wheel, or right-click it, to empty it. Hold %s for the wheel, press %s to cast.");
		t.add(ModLang.SPELL_WHEEL_TITLE, "Spell Wheel");
		t.add(ModLang.SPELL_WHEEL_HINT, "Click: ready (cast with %s). Right-click: cast now.");
		t.add(ModLang.SPELL_WHEEL_EMPTY, "No spells on the wheel yet. Learn them from the heaven or ghost methods.");
		t.add(ModLang.HUD_READIED_SPELL, "Spell: %s");
		// The heaven methods (the names, lore, gains and spells are with HeavenMethodText)
		for (QiElement element : QiElement.five()) {
			t.add(ModLang.ELEMENT_PREFIX + element.name().toLowerCase(java.util.Locale.ROOT), switch (element) {
				case WOOD -> "Wood";
				case WATER -> "Water";
				case FIRE -> "Fire";
				case EARTH -> "Earth";
				default -> "Metal";
			});
		}
		// The five passives (HeavenPassives): each one's name, and its amount on the Methods tab
		for (QiElement element : QiElement.five()) {
			t.add(ModLang.passiveKey(element), switch (element) {
				case WOOD -> "Evergreen Vitality";
				case WATER -> "Ceaseless Flow";
				case FIRE -> "Fury of the Fanned Flame";
				case EARTH -> "Unshaken Mountain Body";
				default -> "Whetted Edge";
			});
			t.add(ModLang.passiveKey(element) + ".amount", switch (element) {
				case WOOD -> "%s health regained each second";
				case WATER -> "+%s qi regained each second";
				case FIRE -> "+%s%% attack speed";
				case EARTH -> "%s%% less harm taken";
				default -> "+%s melee harm";
			});
		}
		t.add(ModLang.MSG_HEAVEN_METHOD_MERIDIANS, "Your meridians are sealed: no heaven method takes hold until a Marrow Cleansing Elixir has opened them.");
		t.add(ModLang.MSG_HEAVEN_METHOD_DEAD, "The heaven methods are the living's. The dead cultivate the ghost methods alone.");
		t.add(ModLang.MSG_HEAVEN_LEARNED, "Learned the %s: layer %s of %s.");
		t.add(ModLang.MSG_HEAVEN_PRACTISED, "Your meditation now cultivates the %s.");
		t.add(ModLang.MSG_HEAVEN_LAYER, "%s: layer %s settles in.");
		t.add(ModLang.MSG_HEAVEN_LAYER_SPELL, "New spell: %3$s (%1$s, layer %2$s).");
		t.add(ModLang.MSG_HEAVEN_NONE_KNOWN, "No heaven method: you cultivate at 3/4 pace. Seek a Dao Stele, a Jade Slip or a sect elder.");
		t.add(ModLang.HEAVEN_GATE_NONE, "You need a heaven method of %s or deeper: seek a Dao Stele, a Jade Slip or a sect elder.");
		t.add(ModLang.JOURNAL_HEAVEN_METHOD, "A heaven method");
		t.add(ModLang.JOURNAL_HEAVEN_METHOD_TEXT, "I learned the %s, a method of %s qi. %s");
		t.add(ModLang.MSG_SPELL_NO_HEAVEN_QI, "Not enough qi for %s.");
		t.add(ModLang.MSG_SPELL_NO_HOLD, "Nothing there for the spell to take hold of.");
		t.add(ModLang.MSG_YANG_BOMB_SET, "A ball of yang fire waits where you left it. Cast the spell again to set it off.");
		t.add(ModLang.MSG_YANG_BOMB_FAR, "Your yang fire is too far away to answer you.");
		t.add(ModLang.MSG_YANG_BOMB_GONE, "Your yang fire gutters out.");
		t.add(ModLang.MSG_NO_EARTH, "There is no earth here for the spell to draw on.");
		t.add(ModLang.MSG_ORES_FOUND, "Your consciousness draws %s ores out of the rock.");
		t.add(ModLang.MSG_NO_ORES, "Your consciousness finds no ore within its reach.");
		t.add(ModLang.MSG_PHOENIX_REBORN, "You rise again from your own flames!");
		t.add(ModLang.MSG_LIFE_SEALED, "The treasury's seal holds what grows in it: break the seal first.");
		t.add(ModLang.MSG_LIFE_HOLD_ONE, "Hold a single fruit or herb: a whole stack can't be drawn on at once.");
		// The body one is born with (Physique), body refining (BodyRefining)
		t.add(ModLang.physiqueGradeKey("low"), "Low");
		t.add(ModLang.physiqueGradeKey("mid"), "Mid");
		t.add(ModLang.physiqueGradeKey("high"), "High");
		t.add(ModLang.physiqueGradeKey("supreme"), "Supreme");
		t.add(ModLang.physiqueGradeKey("immortal"), "Immortal");
		physique(t, "five_element_spiritual_roots", "Five Elements Spiritual Roots",
				"Roots of all five elements: any element's methods may be learned, at the ordinary pace.");
		physique(t, "cursed_meridian_body", "Cursed Meridian Body", "Meridians that leak: qi gathers 0.8 times as fast into a pool 0.8 times as deep.");
		physique(t, "shattered_meridians", "Shattered Meridians",
				"Meridians that will never hold qi: no cultivation, no heaven methods, no spells. The body can be tempered on past a mortal's Peak instead, "
						+ "eight stages of body refining, the last as strong as a Core Formation cultivator's. Meditation tempers it too. "
						+ "It is never the lot of one already cultivating.");
		physique(t, "three_element_spiritual_roots", "Three Elements Spiritual Roots",
				"Roots of three elements only: methods of those three alone, cultivated 1.5 times as fast.");
		physique(t, "yin_soul_body", "Yin Soul Body",
				"A soul at home in yin: yin corruption never takes hold of it, and the ghost path is cultivated 1.2 times as fast.");
		physique(t, "heaven_sacred_body", "Heaven Sacred Body",
				"The heavens favour it: no Ritual to the Heavens is wanted at Qi Refining Mid, and heaven methods are cultivated 1.2 times as fast.");
		physique(t, "innate_alchemy_body", "Innate Alchemy Body", "Pills and elixirs brewed by its hands work twice as strongly.");
		physique(t, "true_element_spiritual_root", "True Element Spiritual Root",
				"A single pure root: one element's methods alone, cultivated twice as fast.");
		physique(t, "innate_consciousness_body", "Innate Consciousness Body",
				"A Consciousness Domain as wide as a Four Axis cultivator's the day it opens. It never grows past that.");
		physique(t, "blessed_meridian_body", "Blessed Meridian Body",
				"Wide, clean meridians: qi gathers 1.5 times as fast into a pool 1.5 times as deep, and Qi Sense opens from a mortal's first day.");
		physique(t, "jade_poison_body", "Jade Poison Body",
				"No medicinal toxicity ever builds up, and the qi of pills and spirit fruit is refined the moment it is taken. No poison nor wither "
						+ "takes hold of it, and its blows poison what they strike, the more strongly the higher its realm.");
		physique(t, "fallen_true_immortal", "Fallen True Immortal",
				"An immortal reborn in a mortal body: any element, five times the pace on both paths, and a method's new layers need no settling.");
		physique(t, "ghost_devil_body", "Ghost Devil Body",
				"Half the dead's already: the ghost path at twice the pace, and a Ghost King's form of its own anywhere, even among the living, "
						+ "as strong as its ghost methods allow. In that form it has the whole of its ghost path's strength, as the dead do; in its "
						+ "human form, half, as any living cultivator's ghost path gives. Find a ghost method of a deeper realm and the ghost path leaps to it.");
		physique(t, "heavenly_thunder_body", "Heavenly Thunder Body",
				"A tribulation's lightning feeds it instead of harming it: every bolt taken becomes cultivation and qi.");
		physique(t, "spirit_eye_body", "Spirit-Eye Body",
				"Eyes open to what others can't see: from its first day, as if the Ghost Eye were always open, it sees souls and the dead clearly, "
						+ "and the dead and the Method Steles even through walls within 32 blocks, for nothing.");
		physique(t, "nine_yang_body", "Nine Yang Body",
				"A body of the blazing sun: fire and lava never burn it. Heaven methods are cultivated 1.3 times as fast; the ghost path, yin to its "
						+ "yang, at half the pace.");
		physique(t, "starborn_body", "Starborn Body",
				"Born under the stars: the void bears it from Core Formation, a realm before anyone else. A star's heat never burns it, space's thin qi "
						+ "is whole to it, and it flies through space 1.5 times as fast, paying the years of its flight there by as much less.");
		physique(t, "ancient_holy_body", "Ancient Holy Body",
				"A body of the ancient sages: health and strength 1.3 times another's, a tribulation's strikes a quarter weaker, and its tempering "
						+ "twice as fast.");
		physique(t, "blood_demon_body", "Blood Demon Body", "It feeds on the blood it spills: every blow it deals heals it by 15%% of the harm done.");
		physique(t, "hundred_refinements_body", "Hundred Refinements Body", "Refined in a hundred fires: one stat point more at every breakthrough.");
		physique(t, "primordial_chaos_body", "Primordial Chaos Body",
				"The chaos from before heaven and earth parted: meditation feeds both paths at once, each at its full pace, no element is barred to it, "
						+ "and its spells strike a quarter harder. The heavens hate it: every tribulation strikes half as many times again.");
		physique(t, "heaven_defying_body", "Heaven-Defying Body",
				"The heavens can't strike it down: a tribulation's killing strike leaves it one heart, and the tribulation fails instead. No failed "
						+ "tribulation costs it a stage or its cultivation: it may face the heavens again at once. It asks no leave of the heavens (no "
						+ "Ritual to the Heavens), but heaven methods come to it at 0.75 times the pace, and the righteous sense something wrong in it "
						+ "(-15 regard).");
		physique(t, "soul_devouring_body", "Soul-Devouring Body",
				"It feeds on souls: every cultivator it slays, players too, gives it a tenth of what the victim's stage takes, on the path it "
						+ "cultivates. Each righteous or neutral one drags its own path toward the devilish, and the righteous think the less of it "
						+ "(-20 regard).");
		physique(t, "eternal_reincarnation_body", "Eternal Reincarnation Body",
				"The rarest of all, and no gift to cultivation: a Low grade's talent and pace. A soul that never forgets: when its life is spent "
						+ "or its soul scatters, it is born again with this same body and all its cultivation, every realm, stage and method on each "
						+ "path, its years begun anew (a Rebirth Pill has no hold on it). Whoever keeps at it reaches True Immortal in the end.");
		physique(t, "myriad_form_body", "Myriad Form Body",
				"As rare as the Eternal Reincarnation Body: born with two more physiques at once, of Mid to Supreme grade and never two that clash, "
						+ "with all their gifts.");
		t.add(ModLang.bodyRefiningKey("peak"), "Mortal Peak");
		t.add(ModLang.bodyRefiningKey("copper_skin"), "Copper Skin");
		t.add(ModLang.bodyRefiningKey("iron_flesh"), "Iron Flesh");
		t.add(ModLang.bodyRefiningKey("steel_sinew"), "Steel Sinew");
		t.add(ModLang.bodyRefiningKey("jade_bone"), "Jade Bone");
		t.add(ModLang.bodyRefiningKey("silver_marrow"), "Silver Marrow");
		t.add(ModLang.bodyRefiningKey("golden_blood"), "Golden Blood");
		t.add(ModLang.bodyRefiningKey("diamond_heart"), "Diamond Heart");
		t.add(ModLang.bodyRefiningKey("indestructible_body"), "Indestructible Body");
		t.add(ModLang.MSG_PHYSIQUE_BORN, "You were born with a %s (%s grade).");
		t.add(ModLang.MSG_PHYSIQUE_ROOTS, "Your roots are of %s.");
		t.add(ModLang.MSG_PHYSIQUE_WRONG_ELEMENT, "Your roots won't take %s qi: only %s methods can be learned with your body.");
		t.add(ModLang.MSG_PHYSIQUE_SHATTERED, "Your meridians are shattered: they will never hold qi. Temper your body instead.");
		t.add(ModLang.MSG_BODY_REFINED, "Your body is refined: %s!");
		t.add(ModLang.MSG_THUNDER_ABSORBED, "Your body drinks in the lightning (+%s cultivation)");
		t.add(ModLang.JOURNAL_PHYSIQUE, "My body");
		t.add(ModLang.JOURNAL_PHYSIQUE_TEXT, "I was born with a %s, a %s grade physique. %s");
		t.add(ModLang.STATS_PHYSIQUE, "Physique: %s (%s, talent ×%s)");
		t.add(ModLang.STATS_ROOTS, "Roots: %s");
		t.add(ModLang.STATS_BODY_REFINING, "Body refining: %s");
		t.add(ModLang.CMD_PHYSIQUE_GET, "%s was born with a %s");
		t.add(ModLang.CMD_PHYSIQUE_SET, "%s now has a %s");
		t.add(ModLang.CMD_PHYSIQUE_UNKNOWN, "No physique called %s");
		t.add(ModLang.MSG_PHYSIQUE_FORMS, "Within it lie two more: %s.");
		t.add(ModLang.JOURNAL_PHYSIQUE_FORM, "Within it, a %s: %s");
		t.add(ModLang.STATS_FORMS, "Forms: %s");
		t.add(ModLang.MSG_DEVOURED, "You devour %s's cultivation: +%s.");
		t.add(ModLang.MSG_DEVOURED_DRAG, "Their blood drags your path toward the devilish (-%s): %s");
		t.add(ModLang.MSG_DEVOURED_FALL, "%s devoured your cultivation: you fall to %s.");
		t.add(ModLang.MSG_TRIB_DEFIED, "The heavens cannot strike you down! Their tribulation breaks against you, and fails.");
		t.add(ModLang.MSG_TRIB_HOLDS, "Your cultivation holds: you stand at %s still, and may face the tribulation again.");
		t.add(ModLang.MSG_REINCARNATED, "Your Eternal Reincarnation Body carries you on: nothing of your cultivation lost, a new life begun at %s.");
		t.add(ModLang.PILL_ALCHEMY_BODY, "Brewed by an Innate Alchemy Body: twice as strong");
		// The Storage Ring (StorageRingItem, StorageRings) and its screen
		t.add(ModItems.STORAGE_RING, "Storage Ring");
		t.add(ModLang.STORAGE_RING_NAME, "Storage Ring (%s grade)");
		t.add(ModLang.STORAGE_RING_BOUNDLESS, "Boundless Qiankun Ring");
		t.add(ModLang.storageGradeKey("mortal"), "Mortal");
		t.add(ModLang.storageGradeKey("spirit"), "Spirit");
		t.add(ModLang.storageGradeKey("earth"), "Earth");
		t.add(ModLang.storageGradeKey("heaven"), "Heaven");
		t.add(ModLang.storageGradeKey("boundless"), "Boundless");
		t.add(ModLang.STORAGE_TOOLTIP_STACKS, "A space folded inside: holds %s stacks");
		t.add(ModLang.STORAGE_TOOLTIP_BOUNDLESS, "A space without end: holds anything, any number of it");
		t.add(ModLang.STORAGE_TOOLTIP_USE, "Right-click to open; sneak + right-click to wear it (then the Storage Ring key opens it)");
		t.add(ModLang.STORAGE_USED, "%s / %s stacks");
		t.add(ModLang.STORAGE_USED_BOUNDLESS, "%s kinds, no end");
		t.add(ModLang.STORAGE_SEARCH, "Search...");
		t.add(ModLang.STORAGE_EMPTY, "The space is empty.");
		t.add(ModLang.STORAGE_REFINE, "Refine");
		t.add(ModLang.STORAGE_REFINE_TO, "Refine into %s grade: %s");
		t.add(ModLang.STORAGE_REFINE_REALM, "Needs a cultivator of %s or above");
		t.add(ModLang.STORAGE_REFINE_LAST, "Boundless: there is no grade beyond");
		t.add(ModLang.STORAGE_COUNT, "In the ring: %s");
		t.add(ModLang.MSG_STORAGE_NO_RING, "You carry no Storage Ring.");
		t.add(ModLang.MSG_STORAGE_LAST_GRADE, "This ring is already boundless.");
		t.add(ModLang.MSG_STORAGE_REALM, "Only a cultivator of %s can fix a space of %s grade.");
		t.add(ModLang.MSG_STORAGE_COST, "Refining it takes %s %s.");
		t.add(ModLang.MSG_STORAGE_REFINED, "The space within unfolds: %s grade!");
		t.add(ModLang.KEY_STORAGE_RING, "Open Storage Ring");
		// The Dao Codex (Codex, CodexScreen, CodexState, CodexToast)
		t.add(ModLang.KEY_CODEX, "Open the Dao Codex");
		t.add(ModLang.CODEX_TITLE, "The Dao Codex");
		t.add(ModLang.CODEX_BUTTON, "The Dao Codex (%s)");
		t.add(ModLang.CODEX_LOCKED, "???");
		t.add(ModLang.CODEX_COUNT, "%s of %s pages");
		t.add(ModLang.CODEX_TOAST_NEW, "New in the Codex");
		t.add(ModLang.CODEX_TOAST_MANY, "%s new pages");
		for (Codex.Hint hint : Codex.Hint.values()) t.add(ModLang.codexHint(hint.id()), hint.english());
		Codex.english().forEach((key, text) -> t.add(key, text));
		t.add(ModLang.KEY_QUICK_ABILITY, "Use Selected Ability");
		for (int slot = 1; slot <= PlayerCultivation.WHEEL_SLOTS; slot++) t.add(ModLang.KEY_ABILITY_SLOT_PREFIX + slot, "Ability Wheel Slot " + slot);
		t.add(ModLang.MSG_QUICK_ABILITY_STATE, "%s: %s");
		t.add(ModLang.MSG_QUICK_ABILITY_EMPTY, "Slot %s of your ability wheel is empty (arrange it on the Abilities tab).");
		t.add(ModLang.MSG_SELECTED_ABILITY_NONE, "No ability selected: hold %s and click one to select it.");
		t.add(ModLang.MSG_SELECTED_ABILITY_ASLEEP, "%s can't be used right now.");
		t.add(ModLang.WHEEL_SELECTED, "Selected");
		t.add(ModLang.HUD_SELECTED_ABILITY, "Ability: %s");
		// Detonating the golden core (CoreDetonation)
		t.add(ModLang.MSG_CORE_CONFIRM, "Detonate again within %s seconds to break your golden core. You will die.");
		t.add(ModLang.MSG_CORE_TOO_LATE, "Your core is already breaking. Nothing can stop it now.");
		t.add(ModLang.MSG_CORE_BREAKING, "%s's golden core is breaking apart! Get away!");
		t.add(ModLang.MSG_CORE_FIZZLED, "The core you broke is no longer in the body you stand in. Its light goes out.");
		t.add(ModLang.DEATH_CORE_DETONATION, "%1$s detonated their golden core");
		t.add(ModLang.DEATH_CORE_DETONATION + ".player", "%1$s detonated their golden core against %2$s");
		// The Spatial Tear and travel through the gap
		t.add(ModLang.MSG_GAP_TRAVEL, "The Spatial Gap: each block here is %s in the world. Tear space again to step out.");
		t.add(ModLang.MSG_GAP_TRAVEL_NO_TEAR, "The Spatial Gap: each block here is %s in the world. Fly up through it into the Jade Firmament, or sink through its floor back to the Overworld.");
		t.add(ModLang.MSG_GAP_TRAVEL_SHALLOWS, "The shallows - the world beneath: %s, %s - %s blocks to the crossing");
		t.add(ModLang.MSG_GAP_TRAVEL_UPPER, "The upper layers - the world beneath: %s, %s - %s blocks to the crossing");
		t.add(ModLang.MSG_GAP_TRAVEL_LOWER, "The lower layers - the world beneath: %s, %s - %s blocks to the floor");
		t.add(ModLang.MSG_GAP_SETTLING, "Space settles about your way out...");
		t.add(ModLang.MSG_TEAR_COOLING, "Space hasn't settled from your last tear (%s s).");
		t.add(ModLang.MSG_TEAR_OPEN, "Space tears open before you. Step through it, and any with you, before it closes.");
		t.add(ModLang.MSG_TEAR_NOT_HERE, "Space here won't tear: only the mortal world, the Jade Firmament and the Great Void open onto another realm.");
		t.add(ModLang.MSG_TEAR_NO_QI, "Tearing space open takes %s qi.");
		t.add(ModLang.MSG_TEAR_NO_GHOST_QI, "Tearing space open takes %s ghost qi.");
		t.add(ModLang.MSG_TEAR_OUT, "You tear your way out of the gap at %s, %s.");
		t.add(ModLang.MSG_TEAR_OUT_UPPER, "You tear your way out into the Jade Firmament at %s, %s.");
		t.add(ModLang.MSG_BREACH_OPEN, "You tear the void open: a breach into the Great Void. Stay by it to hold it open for those who follow.");
		t.add(ModLang.MSG_BREACH_TURNED_BACK, "The void's pressure turns you back: only Dao Integration and above may ride a breach.");
		t.add(ModLang.MSG_BREACH_ENTER, "Shattered space. The breach's current carries you up for %s seconds; the void presses on all sides, and its shards strike where the light gathers.");
		t.add(ModLang.MSG_BREACH_CLIMB, "Void Breach: %s blocks to the Great Void");
		t.add(ModLang.MSG_ARRIVE_GREAT_VOID, "The Great Void Realm. Its qi runs %s times the mortal world's: here Void Shattering and above cultivate at their own pace.");
		t.add(ModLang.MSG_TEAR_DOWN, "You tear space open onto the Jade Firmament below.");
		t.add(ModLang.MSG_FALL_TO_FIRMAMENT, "You fall out of the Great Void into the Jade Firmament's sky.");
		t.add(ModLang.MSG_TEAR_IMMORTAL, "Immortal space holds: it will not tear. The Immortal Gate is the only way down.");
		t.add(ModLang.MSG_CLOUDS_BEAR_UP, "The sea of clouds bears you back up. The only way down is the Immortal Gate.");		t.add(ModLang.MSG_GATE_STIRS, "The sky over the Great Void's void sea stirs: the Immortal Gate opens in a minute.");
		t.add(ModLang.MSG_GATE_OPENS, "The Immortal Gate opens over the Shore of Nirvana! For %s minutes the way to the Immortal Realm stands open, and the way down from it.");
		t.add(ModLang.MSG_GATE_CLOSES, "The Immortal Gate closes. It opens again in %s days.");
		t.add(ModLang.MSG_GATE_TURNED_BACK, "The Gate's pressure turns you back: only Entering Nirvana and above may pass.");
		t.add(ModLang.MSG_GATE_ENTER, "You pass into the Immortal Gate. Its light bears you up for %s seconds; its pressure is on you all the way.");
		t.add(ModLang.MSG_GATE_CLIMB, "The Immortal Gate: %s blocks to the Southern Heavenly Gate");
		t.add(ModLang.MSG_ARRIVE_IMMORTAL, "You come through the Southern Heavenly Gate into the Immortal Realm. Immortal qi here: x%s.");
		t.add(ModLang.MSG_GATE_DOWN, "You pass back down through the Immortal Gate onto the Shore of Nirvana.");
		t.add(ModLang.MSG_GATE_SHUT_DAYS, "The Gate is shut. It opens again in %s days.");
		t.add(ModLang.MSG_GATE_SHUT_HOURS, "The Gate is shut. It opens again in %s hours.");
		t.add(ModLang.MSG_CURRENT_SWEPT, "A current of the void takes you, and its light fills your eyes...");
		t.add(ModLang.MSG_CURRENT_LANDED, "The current sets you down on land far from where it found you.");
		t.add(ModLang.MSG_ELDER_RISES, "The blood in the pool stirs: the %s rises out of it to keep what lies there!");
		t.add(ModLang.MSG_DAO_STELE_TEACHES, "This stele holds the %s (%s, to %s). Meditate beside it to learn it.");
		t.add(ModLang.MSG_DAO_STELE_KNOWN, "This stele holds the %s (%s, to %s), which you know. Meditating beside a stele of its element while you practise it is faster.");
		t.add(ModLang.MSG_DAO_STELE_READING, "You read the %s carved in the stele...");
		t.add(ModLang.MSG_STELE_SEALED, "The %s carved here is too deep for this world: its words won't open to you. It is the Immortal Realm's to teach.");
		// Techniques and their manuals (Technique, TechniqueManualItem), and writing them out
		t.add(ModLang.techniqueKey(Technique.CLONE.getId()), "Clone");
		t.add(ModLang.techniqueKey(Technique.CLONE.getId()) + ".description", "Split a clone off your body and walk in it as yourself: your "
				+ "realm, abilities and spells, half your strength.");
		t.add(ModLang.techniqueKey(Technique.SLEEVE.getId()), "Universe in a Sleeve");
		t.add(ModLang.techniqueKey(Technique.SLEEVE.getId()) + ".description", "Carry others in your sleeve, whatever their realm, up through "
				+ "the Spatial Gap, the Void Breach and the Immortal Gate. The spatial pressure on you doubles for each one.");
		t.add(ModItems.TECHNIQUE_MANUAL, "Technique Manual");
		t.add(ModLang.ITEM_TECHNIQUE_MANUAL_OF, "Technique Manual: %s");
		t.add(ModLang.ITEM_SPELL_MANUAL_OF, "Spell Manual: %s");
		t.add(ModLang.TOOLTIP_MANUAL, "Read it to learn what it holds (it isn't used up)");
		t.add(ModLang.TOOLTIP_MANUAL_REALM, "Wants %s");
		t.add(ModLang.TOOLTIP_MANUAL_GHOST, "%s (ghost path)");
		t.add(ModLang.TOOLTIP_MANUAL_AUTHOR, "Written out by %s");
		t.add(ModItems.BLANK_MANUAL, "Blank Manual");
		t.add(ModLang.TOOLTIP_BLANK_MANUAL, "Hold it in one hand and an Inscription Brush in the other, and use the brush to write out a technique or a spell you know");
		t.add(ModLang.MSG_MANUAL_LEARNED, "You have learned %s.");
		t.add(ModLang.MSG_MANUAL_KNOWN, "You already know %s.");
		t.add(ModLang.MSG_MANUAL_TOO_LOW, "You can't grasp %s yet: it wants %s.");
		t.add(ModLang.MSG_MANUAL_ROOTS, "Your roots won't take %s's element.");
		t.add(ModLang.MSG_MANUAL_DEAD, "The dead can't take up %s: it is the heaven path's.");
		t.add(ModLang.INSCRIBE_TITLE, "Write Out a Manual");
		t.add(ModLang.INSCRIBE_HINT, "Choose what to write on the Blank Manual");
		t.add(ModLang.INSCRIBE_NONE, "You know nothing that can be written out yet.");
		t.add(ModLang.INSCRIBE_TECHNIQUE, "Technique: %s");
		t.add(ModLang.MSG_INSCRIBED, "You write out %s.");
		// Clone (Clones)
		t.add(ModEntities.CLONE_BODY, "Body");
		t.add(ModLang.MSG_CLONE_BUSY, "Not while your soul is away, in another's body, or in a tribulation.");
		t.add(ModLang.MSG_CLONE_SPLIT, "A clone of you splits off and stands beside you.");
		t.add(ModLang.MSG_CLONE_LOST, "Your clone is beyond reach.");
		t.add(ModLang.MSG_CLONE_INTO, "You are in your clone. Your body sits on in meditation.");
		t.add(ModLang.MSG_CLONE_BACK, "You are back in your body. Your clone stands where you left it.");
		t.add(ModLang.MSG_CLONE_ABSORBED, "You draw your clone back into yourself.");
		t.add(ModLang.MSG_CLONE_HURT, "Your clone is under attack! (%s / %s)");
		t.add(ModLang.MSG_CLONE_MAIN_HURT, "Your body is under attack! (%s / %s)");
		t.add(ModLang.MSG_CLONE_SLAIN, "Your clone was slain, and dissolves. Another can be split off in %s minutes.");
		t.add(ModLang.MSG_CLONE_MAIN_SLAIN, "Your body was slain where it sat. Your clone dissolves with you.");
		t.add(ModLang.MSG_CLONE_FELL, "Your clone dissolves: you wake in your body. Another can be split off in %s minutes.");
		t.add(ModLang.MSG_CLONE_CANNOT, "A clone can't do that: your body can.");
		t.add(ModLang.MSG_CLONE_REFORMING, "Your last clone was lost: another can be split off in %s.");
		t.add(ModLang.MSG_CLONE_DOMAIN_MET, "Your clone's consciousness touches another's: %s (%s)");
		// Universe in a Sleeve (Sleeves, SleeveCommand)
		t.add(ModLang.MSG_SLEEVE_HOW, "Look at someone within %1$s blocks and use this to offer them a place in your sleeve. You carry %2$s of %3$s. "
				+ "Sneak and use it to let them out.");
		t.add(ModLang.MSG_SLEEVE_EMPTY, "Your sleeve is empty.");
		t.add(ModLang.MSG_SLEEVE_OFFER, "You offer %s a place in your sleeve.");
		t.add(ModLang.MSG_SLEEVE_OFFERED, "%s offers to carry you in their sleeve.");
		t.add(ModLang.SLEEVE_ACCEPT, "[Accept]");
		t.add(ModLang.SLEEVE_REFUSE, "[Refuse]");
		t.add(ModLang.SLEEVE_LEAVE, "[Step out]");
		t.add(ModLang.MSG_SLEEVE_NO_OFFER, "Nobody is offering you a place in their sleeve now.");
		t.add(ModLang.MSG_SLEEVE_REFUSED, "%s won't go into your sleeve.");
		t.add(ModLang.MSG_SLEEVE_YOU_REFUSED, "You turn down %s's offer.");
		t.add(ModLang.MSG_SLEEVE_NOT_ON_THE_WAY, "Not here: you can only take someone in out in a world, not on the way between realms.");
		t.add(ModLang.MSG_SLEEVE_FULL, "Your sleeve holds no more than %2$s.");
		t.add(ModLang.MSG_SLEEVE_CANT_TAKE, "%1$s can't be taken into your sleeve now.");
		t.add(ModLang.MSG_SLEEVE_TAKEN, "%s is in your sleeve (%s of %s).");
		t.add(ModLang.MSG_SLEEVE_INSIDE, "You are in %s's sleeve. You'll come out beside them when they arrive.");
		t.add(ModLang.MSG_SLEEVE_COST, "You carry %s in your sleeve. With everyone you bring, the spatial pressure on you is %s times what it would be.");
		t.add(ModLang.MSG_SLEEVE_MID_CLIMB, "Not partway through the climb: wait until you're through.");
		t.add(ModLang.MSG_SLEEVE_LET_OUT, "You let them out of your sleeve.");
		t.add(ModLang.MSG_SLEEVE_OUT, "%s lets you out of their sleeve.");
		t.add(ModLang.MSG_SLEEVE_ARRIVED, "You come out of %s's sleeve beside them.");
		t.add(ModLang.MSG_SLEEVE_CAST_OUT, "%s has fallen: you are cast out of their sleeve!");
		t.add(ModLang.MSG_SLEEVE_BEARER_GONE, "%s is gone: you are set down where they took you in.");
		t.add(ModLang.MSG_SLEEVE_OUT_AWAY, "While you were away, %s let you out of their sleeve.");
		t.add(ModLang.MSG_SLEEVE_NOT_IN, "You aren't in anyone's sleeve.");
		t.add(ModLang.MSG_SLEEVE_CANT_LEAVE, "You can't step out on the way between realms: wait until they arrive.");
		t.add(ModLang.MSG_SLEEVE_STEPPED_OUT, "%s steps out of your sleeve.");
		t.add(ModLang.MSG_SLEEVE_YOU_STEPPED_OUT, "You step out of %s's sleeve.");
		t.add(ModLang.MSG_BODY_DOMAIN_MET, "Your main body's consciousness touches another's: %s (%s)");
		t.add(ModLang.HUD_CLONE_IN, "In your clone");
		t.add(ModLang.HUD_CLONE_MAIN, "Body %s / %s");
		t.add(ModLang.ITEM_JADE_SLIP_OF, "Jade Slip: %s");
		t.add(ModLang.TOOLTIP_JADE_SLIP, "Read it to learn the method");
		t.add(ModLang.TOOLTIP_JADE_SLIP_ELEMENT, "A %s method, to %s");
		t.add(ModItems.JADE_SLIP, "Jade Slip");
		t.add(ModBlocks.DAO_STELE, "Dao Stele");
		t.add(ModLang.METHODS_PAGE_GHOST, "Ghost Path");
		t.add(ModLang.METHODS_PAGE_HEAVEN, "Heaven Path");
		t.add(ModLang.METHODS_HEAVEN_KNOWN, "%s of %s heaven methods known");
		t.add(ModLang.METHODS_HEAVEN_EMPTY, "No heaven methods learned yet.");
		t.add(ModLang.METHODS_HEAVEN_HINT, "Without one you cultivate at three quarters' pace and break through no further than Qi Refining. Dao Steles stand where an element's qi runs thick; cultivators carry jade slips; a sect's elders teach its element.");
		t.add(ModLang.METHODS_HEAVEN_DEAD, "The heaven methods are the living's: the dead cultivate the ghost methods alone.");
		t.add(ModLang.METHODS_HEAVEN_SEALED, "Heaven methods: sealed, your meridians not yet opened (a Marrow Cleansing Elixir)");
		t.add(ModLang.METHODS_HEAVEN_RATE, "Meditating here, layer %s of the %s settles in about %s min.");
		t.add(ModLang.METHODS_HEAVEN_NEXT, "The %s is understood as far as your cultivation: its next layer opens at %s.");
		t.add(ModLang.METHODS_HEAVEN_OPENS, "Layer %s opens at %s");
		t.add(ModLang.METHODS_HEAVEN_OPENS_SHORT, "Layer %s: %s");
		t.add(ModLang.METHODS_HEAVEN_SHARE, "%s qi here: %s%% of all (x%s)");
		t.add(ModLang.METHODS_HEAVEN_REACH, "Your heaven methods open the way as far as %s.");
		t.add(ModLang.METHODS_HEAVEN_UNKNOWN, "%s of the %s %s methods unknown. Their Dao Steles stand where their qi runs thick; the deepest are "
				+ "seldom carved below the Jade Firmament, the deepest of all in the Great Void.");
		t.add(ModLang.METHODS_HEAVEN_PASSIVE, "%s: %s");
		t.add(ModLang.TALK_TEACH_HEAVEN, "Then listen. This is the %s, as my teachers gave it to me... Sit with it where %s qi runs thick.");
		t.add(ModLang.TALK_TEACH_HEAVEN_PROGRESS, "Your %s, then. Here, where you falter... (%s minutes' progress)");
		t.add(ModLang.STATS_HEADER, "Stats (cultivation bonus in brackets)");
		t.add(ModLang.STAT_HEALTH, "Max Health: %s (%s)");
		t.add(ModLang.STAT_DAMAGE, "Attack Damage: %s (%s)");
		t.add(ModLang.STAT_SPEED, "Movement Speed: %s (%s)");
		t.add(ModLang.STAT_ARMOR, "Armor: %s (%s)");
		t.add(ModLang.STAT_TOUGHNESS, "Armor Toughness: %s (%s)");
		t.add(ModLang.STAT_KNOCKBACK, "Knockback Resistance: %s (%s)");
		t.add(ModLang.STATUS_MEDITATING, "Meditating...");
		t.add(ModLang.STATUS_IDLE, "Not meditating");
		t.add(ModLang.BTN_MEDITATE, "Meditate");
		t.add(ModLang.BTN_STOP, "Stop Meditating");
		t.add(ModLang.BTN_BREAKTHROUGH, "Breakthrough");

		// Messages
		t.add(ModLang.MSG_START, "You sit and begin gathering qi.");
		t.add(ModLang.MSG_STOP, "You open your eyes, ending your meditation.");
		t.add(ModLang.MSG_INTERRUPTED, "Your meditation was interrupted!");
		t.add(ModLang.MSG_MEDITATION_FELL, "Your qi can no longer hold you up: you fall from your meditation!");
		t.add(ModLang.MSG_BOOST, "Cultivation speed +%s%% (%s)");
		t.add(ModLang.MSG_BOOST_NONE, "Your surroundings no longer aid you.");
		t.add(ModLang.BOOST_PEDESTAL, "1 Spirit Pedestal");
		t.add(ModLang.BOOST_PEDESTALS, "%s Spirit Pedestals");
		t.add(ModLang.BOOST_HEIGHT, "High Altitude");
		t.add(ModLang.BOOST_TRANQUIL, "Tranquil Surroundings");
		t.add(ModLang.MSG_CANNOT, "You need steady, dry ground to meditate.");
		t.add(ModLang.MSG_STAGE_UP, "Your cultivation advances: %s - %s!");
		t.add(ModLang.MSG_BREAKTHROUGH, "Tribulation survived: %s!");
		t.add(ModLang.MSG_NOT_READY, "You are not ready to break through.");
		t.add(ModLang.MSG_BOTTLENECK, "Bottleneck! Break through in the cultivation menu.");
		t.add(ModLang.MSG_QI_FLIGHT_GAINED, "Your golden core can carry you. Double-tap jump to fly.");
		t.add(ModLang.MSG_QI_FLIGHT_EXHAUSTED, "Out of qi! Flight returns at %s%%.");
		t.add(ModLang.MSG_SWORD_FLIGHT_GAINED, "Sword Flight: hold a sword and double-tap jump to ride it.");
		t.add("gamerule.meteorFlightSmashes", "Meteor Flight smashes through blocks");
		t.add(ModLang.MSG_FLIGHT_EXCLUSIVE, "One flight at a time: Qi Flight on, Sword Flight off.");
		t.add(ModLang.MSG_QI_SENSE_GAINED, "You can now peer into the plane of Qi.");
		t.add(ModLang.MSG_SUPPRESS_EXHAUSTED, "Out of qi: your pressure lifts. It returns at %s%%.");
		t.add(ModLang.CONSCIOUSNESS_ALERT_TITLE, "A foreign consciousness touches yours");
		t.add(ModLang.CONSCIOUSNESS_ALERT_DETAIL, "%s - %s");
		t.add(ModLang.CONSCIOUSNESS_UNKNOWN, "Unknown realm");
		t.add(ModLang.PRESSURE, "A higher realm weighs on you!");
		t.add(ModLang.PRESSURE_DETAIL, "Strength and qi -%s%%");
		t.add(ModLang.STATS_PRESSED, "is pressed down to");
		t.add(ModLang.HUD_PRESSURE, "Suppressed -%s%%");
		t.add(ModLang.MSG_TRIB_START, "The heavens notice your ascent to %s! Survive %s strikes!");
		t.add(ModLang.MSG_TRIB_START_SINGLE, "The heavens notice your ascent to %s! Survive the strike!");
		t.add(ModLang.MSG_TRIB_BUSY, "Not during a tribulation.");
		t.add(ModLang.MSG_TRIB_FAILED, "%s failed their breakthrough!");
		t.add(ModLang.MSG_TRIB_FALL, "Backlash! You fall to %s.");
		t.add(ModLang.MSG_TRIB_ABANDONED, "You fled the tribulation. Breakthrough abandoned.");
		t.add(ModLang.MSG_TRIB_FLED, "You fled the tribulation. Backlash! You fall to %s.");
		t.add(ModLang.TRIB_HUD, "Heavenly Tribulation (%s) - strikes left: %s");

		// The mortal path and alchemy
		t.add(ModItems.MARROW_CLEANSING_ELIXIR, "Marrow Cleansing Elixir");
		t.add(ModItems.FOUNDATION_PILL, "Foundation Pill");
		t.add(ModItems.CORE_PILL, "Core Pill");
		t.add(ModItems.QI_GATHERING_PILL, "Qi Gathering Pill");
		t.add(ModItems.CULTIVATION_PILL, "Cultivation Pill");
		t.add(ModBlocks.ALCHEMY_CAULDRON, "Alchemy Cauldron");
		t.add(ModEffects.QI_GATHERING, "Qi Gathering");
		for (PillGrade grade : PillGrade.values()) {
			t.add(ModLang.pillGradeKey(grade.getId()), switch (grade) {
				case LOW -> "Low-Grade %s";
				case MID -> "Mid-Grade %s";
				case HIGH -> "High-Grade %s";
				case SUPREME -> "Supreme-Grade %s";
				case IMMORTAL -> "Immortal-Grade %s";
			});
		}
		t.add(ModLang.MORTAL, "Mortal");
		t.add(ModLang.MORTAL_DETAIL, "Your body is ready. Open your meridians to cultivate.");
		t.add(ModLang.MORTAL_RECIPE, "At Mortal Peak, drink a %s. Brew it in an Alchemy Cauldron: %s.");
		t.add(ModLang.MORTAL_STATS, "Each stage of tempering makes your body a little stronger.");
		t.add(ModLang.MORTAL_HOW, "Fight, mine, sprint, and eat food and raw herbs to temper your body.");
		t.add(ModLang.TEMPERING, "Tempering: %s / %s");
		for (MortalStage stage : MortalStage.values()) {
			t.add(stage.getTranslationKey(), switch (stage) {
				case LOW -> "Mortal Low";
				case MID -> "Mortal Mid";
				case HIGH -> "Mortal High";
				case PEAK -> "Mortal Peak";
			});
		}
		t.add(ModLang.MSG_TEMPERED, "Body tempered: %s!");
		t.add(ModLang.MSG_TEMPERED_PEAK, "Mortal Peak! You're ready for the Marrow Cleansing Elixir.");
		t.add(ModLang.MSG_TEMPER_FULL, "Your body is fully tempered.");
		t.add(ModLang.MSG_ELIXIR_TOO_WEAK, "Your body is too weak. Reach Mortal Peak first.");
		t.add(ModLang.MSG_HERB_EATEN, "+%s tempering");
		t.add(ModLang.NEED_PILL, "%2$s needs a %1$s.");
		t.add(ModLang.PILL_PREPARED, "A pill's power waits to carry you into %s.");
		t.add(ModLang.QI_BOOST_LINE, "Qi Gathering Pill: +%s%%");
		t.add(ModLang.PILL_RESISTANCE_LINE, "Medicinal toxicity: %s%%");
		t.add(ModLang.UNREFINED_LINE, "Unrefined qi: %s");
		t.add(ModLang.MSG_INNER_ENTER, "You close your eyes and turn inward.");
		t.add(ModLang.MSG_INNER_PULLED, "Your body is disturbed! Your soul snaps back.");
		t.add(ModLang.MSG_INNER_TRIBULATION, "Return to your body first.");
		t.add(ModLang.MSG_DEVIATION, "Qi deviation! Your circulation went astray.");
		t.add(ModLang.CIRCULATION_HINT, "[%s] Circulate qi");
		t.add(ModLang.CIRCULATION_STREAK, "Circulation x%s");
		t.add(ModLang.SURGE_CIRCUIT, "Qi surge: tap [%s] as the bead meets each lit point");
		t.add(ModLang.SURGE_ELEMENTS, "Qi surge: tap [%s] on the element that comes next");
		t.add(ModLang.SURGE_BREATH, "Qi surge: hold [%s] as it swells, let go as it shrinks");
		t.add(ModLang.SURGE_SYNC, "In rhythm: %s%%");
		t.add(ModLang.MSG_HARMONY, "Qi harmony: meditation x%s");
		t.add(ModLang.MSG_SURGE_FAILED, "The surge passes.");
		t.add(ModLang.MSG_WISPS, "Stray qi drifts onto your island. Gather it!");
		t.add(ModLang.MSG_WISPS_DONE, "Gathered %s of %s wisps.");
		t.add(ModLang.MSG_SEQUENCE, "The leylines stir. Watch the tiles.");
		t.add(ModLang.MSG_SEQUENCE_GO, "Now walk them in order.");
		t.add(ModLang.MSG_SEQUENCE_FAIL, "The pattern fades.");
		t.add(ModLang.MSG_DEMON, "Your heart demon takes shape!");
		t.add(ModLang.MSG_DEMON_WIN, "Heart demon slain. Your dao heart steadies.");
		t.add(ModLang.MSG_DEMON_LOSE, "Your heart demon overwhelms you!");
		t.add(ModLang.MSG_DEMON_FADE, "Your heart demon fades... for now.");
		t.add(ModLang.MSG_MORTAL_MEDITATE, "Your meridians are sealed. You can't gather qi yet.");
		t.add(ModLang.MSG_NEED_PILL, "Need a %s to enter %s. Eat it here, then break through.");
		t.add(ModLang.MSG_AWAKENED, "Your meridians open. You begin cultivating: %s!");
		t.add(ModLang.MSG_AWAKENED_DEAD, "Your meridians open, even in death: the ghost path is yours.");
		t.add(ModLang.MSG_ALREADY_AWAKENED, "Your meridians are already open.");
		t.add(ModLang.MSG_PILL_PREPARED, "%s absorbed. The way into %s is open (+%s%% head start).");
		t.add(ModLang.MSG_PILL_WRONG_TIME, "This pill is only of use at %s.");
		t.add(ModLang.MSG_PILL_ALREADY, "You have already taken a pill for %s.");
		t.add(ModLang.MSG_PILL_MORTAL, "A mortal body can't absorb this.");
		t.add(ModLang.MSG_PILL_BOTTLENECK, "Bottleneck: the pill would be wasted.");
		t.add(ModLang.MSG_PILL_CULTIVATION, "+%s unrefined qi (%s%% effective). Meditate to refine it.");
		t.add(ModLang.MSG_PILL_QI, "+%s%% qi gathering for %s minutes");
		t.add(ModLang.MSG_FRUIT_MORTAL, "A mortal body can't hold the fruit's qi. Drink a Marrow Cleansing Elixir first.");
		t.add(ModLang.MSG_FRUIT_BOTTLENECK, "Your cultivation can't hold any more right now. The fruit would be wasted.");
		t.add(ModLang.MSG_FRUIT_EATEN, "+%s cultivation");
		t.add(ModLang.PILL_AGE, "Ingredients: %s years old");
		t.add(ModLang.PILL_AGE_SHORT, " (%s years)");
		t.add(ModLang.PILL_RECIPE, "Brewed from: %s");
		t.add(ModLang.PILL_EFFECT_AWAKEN, "Opens a Mortal Peak's meridians");
		t.add(ModLang.PILL_EFFECT_BREAKTHROUGH, "Opens the way into %s");
		t.add(ModLang.PILL_EFFECT_START, "Start %s%% of the way into the new stage");
		t.add(ModLang.PILL_EFFECT_QI, "+%s%% qi gathering for %s minutes");
		t.add(ModLang.PILL_EFFECT_CULTIVATION, "+%s%% of your stage's cultivation");
		t.add(ModLang.PILL_EFFECT_RESISTANCE, "Toxicity: works at %s%% right now");
		t.add(ModLang.PILL_WHEN, "Take it at %s");
		t.add(ModLang.CAULDRON_TOOLTIP, "Add water, light a fire below, add ingredients");
		t.add(ModLang.CAULDRON_NEEDS_WATER, "Fill the cauldron with a water bucket first.");
		t.add(ModLang.CAULDRON_EMPTY, "Water ready. Add ingredients.");
		t.add(ModLang.CAULDRON_CONTENTS, "Cauldron: %s (sneak + empty hand to tip out)");
		t.add(ModLang.CAULDRON_ADDED, "%s added. In the cauldron: %s");
		t.add(ModLang.CAULDRON_REJECTED, "%s fits no recipe here.");
		t.add(ModLang.CAULDRON_FULL, "The cauldron can't hold any more ingredients.");
		t.add(ModLang.CAULDRON_EMPTIED, "You tip the ingredients back out.");
		t.add(ModLang.CAULDRON_BREWING, "Brewing %s... %s%%");
		t.add(ModLang.CAULDRON_NEEDS_HEAT, "%s is ready. Light a fire below.");
		t.add(ModLang.CAULDRON_DONE, "%s is ready!");

		// More herbs (see GinsengBlock, SpiritLotusBlock, SpiritDewGrassBlock)
		t.add(ModItems.LINGZHI, "Lingzhi");
		t.add(ModBlocks.LINGZHI, "Lingzhi");
		t.add(ModItems.PURPLE_LINGZHI, "Purple Lingzhi");
		t.add(ModBlocks.PURPLE_LINGZHI, "Purple Lingzhi");
		t.add(ModItems.HUANGJING, "Huangjing");
		t.add(ModBlocks.HUANGJING, "Huangjing");
		t.add(ModItems.OCHRE_HUANGJING, "Ochre Huangjing");
		t.add(ModBlocks.OCHRE_HUANGJING, "Ochre Huangjing");
		t.add(ModItems.SPIRIT_LOTUS, "Spirit Lotus");
		t.add(ModBlocks.SPIRIT_LOTUS, "Spirit Lotus");
		t.add(ModItems.LINGZHI_SPORES, "Lingzhi Spores");
		t.add(ModItems.PURPLE_LINGZHI_SPORES, "Purple Lingzhi Spores");
		t.add(ModItems.HUANGJING_SEEDS, "Huangjing Seeds");
		t.add(ModItems.OCHRE_HUANGJING_SEEDS, "Ochre Huangjing Seeds");
		t.add(ModItems.SPIRIT_LOTUS_SEEDS, "Spirit Lotus Seeds");
		t.add(ModItems.SPIRIT_DEW, "Spirit Dew");
		t.add(ModBlocks.SPIRIT_DEW_GRASS, "Spirit Dew Grass");
		t.add(ModLang.LOTUS_SEEDS_TOOLTIP, "Place on still water. Grown at %s years; drops seeds when picked");
		t.add(ModLang.DEW_TOOLTIP, "Drink to restore %s%% of your qi");
		t.add(ModLang.MSG_DEW_MORTAL, "A mortal body can't hold the dew's qi.");
		t.add(ModLang.MSG_DEW_FULL, "Your qi is already full.");
		t.add(ModLang.MSG_DEW_DRUNK, "+%s qi");
		t.add(ModLang.PILL_SUBSTITUTE, "Replaces %s in recipes, at twice its age");

		// Alignment
		for (Alignment.Faction faction : Alignment.Faction.values()) {
			t.add(ModLang.factionKey(faction.getId()), switch (faction) {
				case RIGHTEOUS -> "Righteous";
				case NEUTRAL -> "Neutral";
				case DEVILISH -> "Devilish";
			});
		}
		t.add(ModLang.ALIGNMENT_VALUE, "%s (%s)");
		t.add(ModLang.ALIGNMENT_LINE, "Path: %s");
		t.add(ModLang.MSG_ALIGNMENT_UP, "Slaying the wicked strengthens your heart: alignment +%s (%s)");
		t.add(ModLang.MSG_ALIGNMENT_DOWN, "Innocent blood stains your hands: alignment -%s (%s)");

		// NPC cultivators and sects
		t.add(ModEntities.CULTIVATOR, "Cultivator");
		t.add(ModItems.CULTIVATOR_SPAWN_EGG, "Cultivator Spawn Egg");
		// The Netherworld's dead: ghost cultivators, the Gate's keepers, Hungry Ghosts
		t.add(ModEntities.GHOST_CULTIVATOR, "Ghost Cultivator");
		t.add(ModItems.GHOST_CULTIVATOR_SPAWN_EGG, "Ghost Cultivator Spawn Egg");
		t.add(ModEntities.HUNGRY_GHOST, "Hungry Ghost");
		t.add(ModItems.HUNGRY_GHOST_SPAWN_EGG, "Hungry Ghost Spawn Egg");
		// The wild demon beasts (BeastKind) and their spawn eggs; a projectile of theirs; the demon cores they leave (DemonCoreItem)
		for (com.example.defyingtheheavens.BeastKind kind : com.example.defyingtheheavens.BeastKind.values()) {
			String name = switch (kind) {
				case IRON_HIDE_BOAR -> "Iron-Hide Boar";
				case SHADOW_WOLF -> "Shadow Wolf";
				case JADE_VIPER -> "Jade Viper";
				case ROCK_SHELL_TURTLE -> "Rock-Shell Turtle";
				case WIND_HAWK -> "Wind Hawk";
				case FIRE_MONKEY -> "Fire Monkey";
				case BLOODFANG_TIGER -> "Bloodfang Tiger";
				case FROST_APE -> "Frost Ape";
				case EARTHQUAKE_BEAR -> "Earthquake Bear";
				case IRON_JAW_CROCODILE -> "Iron-Jaw Crocodile";
				case LIGHTNING_ANTELOPE -> "Lightning Antelope";
				case EMBER_FOX -> "Ember Fox";
				case GOLDEN_WINGED_ROC -> "Golden-Winged Roc";
				case JADE_SCALED_PYTHON -> "Jade-Scaled Python";
				case HORNED_THUNDER_RHINO -> "Horned Thunder Rhino";
				case FLOOD_DRAGON -> "Flood Dragon";
				case ELDER_WHITE_TIGER -> "Elder White Tiger";
				case NINE_TAILED_FOX -> "Nine-Tailed Fox";
				case MOUNTAIN_LIZARD_KING -> "Mountain-Bearing Lizard King";
				case AZURE_DRAGON -> "Azure Dragon";
				case KUN_PENG -> "Kun-Peng";
				case TRUE_PHOENIX -> "True Phoenix";
				case TRUE_DRAGON -> "True Dragon";
				case CHAOS_FIEND -> "Chaos Fiend";
				case PRIMORDIAL_CHAOS -> "Primordial Chaos";
			};
			t.add(ModEntities.beast(kind), name);
			t.add(ModItems.beastEgg(kind), name + " Spawn Egg");
		}
		t.add(ModEntities.BEAST_SHOT, "Beast's Shot");
		t.add("gamerule.demonBeastsSpawn", "Wild demon beasts spawn");
		t.add(ModItems.DEMON_CORE, "Demon Core");
		t.add(ModLang.CORE_NAME, "%s Demon Core");
		t.add(ModLang.CORE_RANK, "Formed at %s");
		t.add(ModLang.CORE_ELEMENT, "Element: %s");
		t.add(ModLang.CORE_FROM, "Taken from: %s");
		t.add(ModLang.CORE_USE, "A demon's whole cultivation, condensed: prized for crafting and artifact forging");
		t.add(ModItems.DEMON_BLOOD, "Demon Blood");
		t.add("gamerule.spellCooldowns", "Spells wait out their cooldowns");
		t.add("gamerule.spellGriefing", "Spells change the land for good");
		t.add(ModLang.BLOOD_NAME, "%s %s Blood");
		t.add(ModLang.BLOOD_UNKNOWN, "Blood of no race known");
		t.add(ModLang.BLOOD_USE, "Drink it: a %1$s awakens its blood to %2$s, and its Demon Path may break through into that realm; a human forms a demon core of it, a half-blood of the %1$s");
		t.add(ModLang.BLOOD_TURN_TIP, "Too much of it, or blood two realms or more above your heaven and ghost paths, and you become a %s");
		t.add(ModLang.MSG_BLOOD_UNKNOWN, "This blood is of no race: it won't take.");
		t.add(ModLang.MSG_BLOOD_ELSEWHERE, "Your own blood is in your own body: not while you're in another's.");
		t.add(ModLang.MSG_BLOOD_DEAD, "The dead have no blood for it to take in.");
		t.add(ModLang.MSG_BLOOD_MORTAL, "Your meridians are sealed: the blood finds nothing to take hold of. Open them first.");
		t.add(ModLang.MSG_BLOOD_OTHER, "Your blood is the %s's: it rejects another race's.");
		t.add(ModLang.MSG_BLOOD_HELD, "Your blood is already awake to %s: this adds nothing.");
		t.add(ModLang.MSG_BLOOD_WARN, "This blood would make you a %s for good, body and look, with no way back. Drink it while sneaking if you mean to.");
		t.add(ModLang.MSG_BLOOD_CORE, "The %s's blood takes hold: a demon core forms in you. Your Demon Path begins at Qi Refining Early; your blood's inheritance is %s.");
		t.add(ModLang.MSG_BLOOD_AWAKENS, "Your blood awakens to %s: your Demon Path may break through into it.");
		t.add(ModLang.MSG_BLOOD_PURITY, "The %s's blood in you grows purer: %s%%.");
		t.add(ModLang.MSG_BLOOD_TURNED, "The blood overwhelms you: you are a %s now, wholly.");
		t.add(ModLang.JOURNAL_HALF_BLOOD, "A Half-Blood");
		t.add(ModLang.JOURNAL_HALF_BLOOD_TEXT, "You drank the %s's blood and a demon core formed in you. You are still human to look at, and to every sect and clan, but you walk the Demon Path now, and your blood gives you its race's gifts at half their strength and its inheritance's spells, %s. The more you drink, the purer it grows: at the whole, or drinking blood far above your other paths, you will become one of them, with no way back.");
		t.add(ModLang.JOURNAL_TURNED, "Turned by Blood");
		t.add(ModLang.JOURNAL_TURNED_TEXT, "The blood you drank has made you a %s, wholly: your body and your look, its gifts at their full strength. Your Demon Path is as it was. There is no going back.");
		t.add(ModLang.HALF_BLOOD_LINE, "%s half-blood · blood purity %s%%");
		t.add(ModLang.DEMON_HELD_BY_BLOOD, "Your blood holds your Demon Path at %s: drink %s blood of %s or higher to break through.");
		t.add(ModLang.CMD_BLOODLINE_HALF, "%s is human, a %s half-blood (blood purity %s%%, awake to %s)");
		t.add(ModItems.SECT_PASS, "Sect Guest Pass");
		t.add(ModLang.PASS_NAME, "Guest Pass of %s");
		t.add(ModLang.PASS_BLANK, "A blank token: its sect's people sell them made out");
		t.add(ModLang.PASS_USE, "Carry it and its sect's barrier lets you through, as it does its own");
		t.add(ModLang.PASS_LEFT, "Good for %s more minutes");
		t.add(ModLang.PASS_LAPSED, "Lapsed");
		t.add(ModLang.PASS_BOUND, "Bound to %s: no one else can pass with it");
		t.add(ModLang.MSG_PASS_REVOKED, "You turned on the sect: its guest pass crumbles in your hand.");
		t.add(ModItems.JIANMU_BOUGH_VASE, "Jianmu Bough Vase");
		t.add(ModItems.HEAVEN_FURNACE_CENSER, "Heaven Furnace Censer");
		t.add(ModItems.NINE_PROVINCES_DING, "Nine Provinces Ding");
		t.add(ModItems.IMMORTAL_FORGING_BELL, "Immortal-Forging Bell");
		t.add(ModItems.SEALED_OCEAN_GOURD, "Sealed Ocean Gourd");
		t.add(ModLang.SACRED_VESSEL_USE, "A Sacred Vessel of %s: held in either hand, your spells of it strike %s%% harder");
		t.add(ModLang.SACRED_VESSEL_LORE, "A relic of the ancients, found still on its altar in the Great Void");
		t.add(ModLang.officialName("ox_head"), "Ox-Head");
		t.add(ModLang.officialName("horse_face"), "Horse-Face");
		t.add(ModLang.officialName("meng_po"), "Meng Po");
		t.add(ModLang.TALK_SOUP, "She ladles a bowl of thin, bitter soup and presses it into your hands. As it goes down, the past grows "
				+ "faint: when the Wheel returns you to life, your body will be washed clean of every pill's poison.");
		t.add(ModLang.TALK_SOUP_LIVING, "\"Not for you, child. You still have a life to remember. Go home.\"");
		t.add(ModLang.MSG_SOUP, "Meng Po's soup: your next life begins free of medicinal toxicity.");
		t.add(ModLang.MSG_SOUP_AGAIN, "You have drunk the soup already. One bowl is enough to forget.");
		t.add(ModLang.MSG_SOUP_LIVING, "Meng Po's soup is for the dead.");
		t.add(ModLang.TALK_TEACH_LEARN, "Listen, then. This is the %s... Sit with it, and it will come to you.");
		t.add(ModLang.TALK_TEACH_PROGRESS, "You know the %s already. Here: this is how I breathe it. (%s minutes' progress.)");
		t.add(ModLang.MSG_TERRACE, "From the Terrace you see, far off, %s.");
		t.add(ModLang.TERRACE_PLACE, "the place you died: %s, near %s, %s, %s");
		t.add(ModLang.JOURNAL_TERRACE, "The Terrace for Gazing Homeward");
		t.add(ModLang.JOURNAL_TERRACE_TEXT, "I looked homeward, and saw %s.");
		t.add(ModLang.JOURNAL_METHOD, "A ghost method");
		t.add(ModLang.JOURNAL_METHOD_TEXT, "I learned the %s. %s");
		t.add(ModLang.DIMENSION_OVERWORLD, "the Overworld");
		t.add(ModLang.DIMENSION_NETHER, "the Nether");
		t.add(ModLang.DIMENSION_END, "the End");
		t.add(ModLang.DIMENSION_UPPER_REALM, "the Jade Firmament");
		t.add(ModLang.DIMENSION_GREAT_VOID, "the Great Void Realm");
		t.add(ModLang.DIMENSION_VOID_BREACH, "a Void Breach");
		t.add(ModLang.DIMENSION_IMMORTAL_REALM, "the Immortal Realm");
		t.add(ModLang.DIMENSION_IMMORTAL_PASSAGE, "the Immortal Gate");
		t.add(ModLang.DIMENSION_NETHERWORLD, "the Netherworld");
		// The Astral Realm
		t.add(ModLang.DIMENSION_ASTRAL_VOID, "the Astral Realm");
		t.add(ModLang.DIMENSION_ASTRAL_SURFACES, "a world of the Astral Realm");
		t.add(ModLang.MSG_ASTRAL_TOO_WEAK, "Above you the sky thins to nothing. Only a Nascent Soul could bear the void beyond.");
		t.add(ModLang.MSG_ASTRAL_ENTER, "You rise past the last of the sky into the Astral Realm. The stars lie open before you.");
		t.add(ModLang.MSG_ASTRAL_LAND, "You descend through the sky of %s, a %s world. Its qi: x%s.");
		t.add(ModLang.MSG_ASTRAL_HOME, "You come down through the Overworld's sky.");
		t.add(ModLang.MSG_ASTRAL_LEAVE, "You rise out of the sky of %s into the void.");
		t.add(ModLang.MSG_ASTRAL_HOLD, "The land of %s comes into view...");
		t.add(ModLang.MSG_ASTRAL_STAR_HEAT, "The fire of %s sears you: turn back!");
		t.add(com.example.defyingtheheavens.ModEntities.VOID_VESTIGE, "Vestige of the Void");
		t.add(ModLang.VESTIGE_NAME, "Vestige of the Void (%s)");
		t.add(ModLang.ASTRAL_QI_LINE, "Astral Realm: qi x%s");
		t.add(ModLang.STATS_QI_GATHER_PLACE, "Qi gather x%s here");
		t.add(ModLang.MSG_VESTIGE_NEAR, "Something vast stirs in the dark...");
		for (AstralPlanets.Kind kind : AstralPlanets.Kind.values()) {
			t.add(ModLang.astralKind(kind), switch (kind) {
				case VERDANT -> "verdant";
				case TEMPERATE -> "temperate";
				case OCEAN -> "ocean";
				case DESERT -> "desert";
				case MESA -> "mesa";
				case FROZEN -> "frozen";
				case JUNGLE -> "jungle";
				case MUSHROOM -> "mushroom";
				case VOLCANIC -> "volcanic";
				case ASHEN -> "ashen";
				case CRYSTAL -> "crystal";
				case SWAMP -> "swamp";
				case BARREN -> "barren";
				case BLOSSOM -> "blossom";
				case SAVANNA -> "savanna";
				case TAIGA -> "taiga";
			});
		}
		t.add("biome.defying-the-heavens.astral_void", "Astral Void");
		t.add("biome.defying-the-heavens.astral_volcanic", "Volcanic Wastes");
		t.add("biome.defying-the-heavens.astral_ashen", "Ash Plains");
		t.add("biome.defying-the-heavens.astral_crystal", "Crystal Fields");
		t.add("biome.defying-the-heavens.astral_barren", "Barren Regolith");
		// The Netherworld's herbs, materials and items
		t.add(ModItems.OTHER_SHORE_FLOWER, "Other-Shore Flower");
		t.add(ModBlocks.OTHER_SHORE_FLOWER, "Other-Shore Flower");
		t.add(ModItems.OTHER_SHORE_FLOWER_SEEDS, "Other-Shore Flower Bulbs");
		t.add(ModItems.NETHERWORLD_LOTUS, "Netherworld Lotus");
		t.add(ModBlocks.NETHERWORLD_LOTUS, "Netherworld Lotus");
		t.add(ModItems.NETHERWORLD_LOTUS_SEEDS, "Netherworld Lotus Seeds");
		t.add(ModItems.YIN_JADE, "Yin Jade");
		t.add(ModBlocks.YIN_JADE_ORE, "Yin Jade Ore");
		t.add(ModBlocks.YIN_JADE_BLOCK, "Block of Yin Jade");
		t.add(ModItems.PEACH_WOOD_SWORD, "Peach Wood Sword");
		t.add(ModLang.TOOLTIP_PEACH_WOOD_SWORD, "Against the dead every blow lands in full, and twice as hard");
		t.add(ModItems.YANG_PRESERVATION_PILL, "Yang Preservation Pill");
		t.add(ModItems.YIN_GATHERING_PILL, "Yin Gathering Pill");
		t.add(ModEffects.YANG_PRESERVATION, "Yang Preservation");
		t.add(ModEffects.YIN_GATHERING, "Yin Gathering");
		t.add(ModLang.MSG_PILL_NO_METHOD, "You have no ghost path for its yin to feed: learn a ghost foundation first.");
		t.add(ModLang.MSG_PILL_YANG, "Yang warmth spreads through you: the Netherworld's yin is held still for %s minutes");
		t.add(ModLang.MSG_PILL_YIN, "+%s%% ghost path cultivation for %s minutes");
		t.add(ModLang.PILL_EFFECT_YANG, "Holds the Netherworld's yin still for %s minutes");
		t.add(ModLang.PILL_EFFECT_YIN, "+%s%% ghost path cultivation for %s minutes");
		t.add(ModItems.LONGEVITY_PILL, "Longevity Pill");
		t.add(ModLang.PILL_EFFECT_LONGEVITY, "+%s years of lifespan");
		t.add(ModLang.PILL_EFFECT_LONGEVITY_LESS, "%s%% of that for you: %s taken already this life");
		t.add(ModLang.MSG_PILL_LONGEVITY, "Your lifespan grows by %s years (%s in all)");
		t.add(ModLang.MSG_PILL_LONGEVITY_ENDLESS, "An immortal's years have no end: the pill would add nothing to them.");
		t.add(ModItems.REBIRTH_PILL, "Rebirth Pill");
		t.add(ModLang.TOOLTIP_REBIRTH_PILL, "Ends this life's cultivation whole, as a spent life does, and begins it anew: a mortal again, in a body born anew");
		t.add(ModLang.TOOLTIP_REBIRTH_PILL_WARN, "Eaten only while sneaking");
		t.add(ModLang.MSG_REBIRTH_CONFIRM, "It would end everything you have cultivated: eat it while sneaking to begin your life anew.");
		t.add(ModLang.MSG_REBIRTH, "The pill dissolves all you were. You begin your life anew.");
		t.add(ModLang.MSG_REBIRTH_ETERNAL, "Your Eternal Reincarnation Body is born again as itself, whatever comes: the pill has no hold on it.");
		t.add(ModItems.GHOST_QUELLING_TALISMAN, "Ghost-Quelling Talisman");
		t.add(ModLang.TOOLTIP_GHOST_QUELLING_TALISMAN, "Throw it at the dead: it strikes them and binds them where they stand for %s seconds");
		t.add(ModEntities.GHOST_QUELLING_TALISMAN, "Ghost-Quelling Talisman");
		t.add(ModEffects.QUELLED, "Quelled");
		t.add(ModLang.MSG_QUELLED, "A talisman seals your brow: you are bound where you stand!");
		t.add(ModLang.MSG_QUELLED_OTHER, "The talisman binds %s where they stand");
		t.add(ModLang.MSG_QUELLED_HELD, "The talisman holds you where you stand.");
		t.add(ModLang.MSG_SPACE_LOCKED, "The earth's veins lock space here: it won't open for you.");
		for (NpcTitle title : NpcTitle.values()) {
			t.add(ModLang.titleKey(title.getId()), switch (title) {
				case SECT_MASTER -> "Sect Master";
				case GRAND_ELDER -> "Grand Elder";
				case ELDER -> "Elder";
				case INNER_DISCIPLE -> "Inner Disciple";
				case OUTER_DISCIPLE -> "Outer Disciple";
				case ROGUE -> "Rogue Cultivator";
				case WANDERER -> "Wandering Mortal";
				case WANDERING_GHOST -> "Wandering Ghost";
				case GHOST_ELDER -> "Ghost Elder";
				case GATE_WARDEN -> "Gate Warden";
				case BRIDGE_KEEPER -> "Keeper of the Naihe Bridge";
				case GHOST_KING -> "Ghost King";
				case GHOST_RETAINER -> "Ghost Retainer";
				case RELIC_KEEPER -> "Relic Keeper";
				case IMMORTAL_OFFICER -> "Immortal Officer";
				case IMMORTAL_MERCHANT -> "Immortal Merchant";
				case HEAVENLY_KING -> "Heavenly King";
				case HEAVENLY_VENERABLE -> "Heavenly Venerable";
				case IMMORTAL_GENERAL -> "Immortal General";
				case COURT_QUARTERMASTER -> "Court Quartermaster";
			});
		}
		t.add(ModLang.titleKey("clan_head"), "Clan Head"); // a demon clan's master (NpcTitle#getDisplayName(boolean))
		t.add(ModLang.titleKey("patriarch"), "Patriarch"); // a Holy Land's master (NpcTitle#getDisplayName(boolean, boolean))
		t.add(ModLang.titleKey("demon_sovereign"), "Demon Sovereign"); // a Demon Palace's master (the same)
		t.add(ModLang.titleKey("sect_lord"), "Sect Lord"); // an immortal sect's master (NpcTitle#getDisplayName(boolean, boolean, boolean))
		t.add(ModLang.NPC_TITLE_BRACKETS, "[%s]");
		t.add(ModLang.NPC_GATE_GUARD, "Gate Guard");
		t.add(ModLang.NPC_OF_SECT, "%s of the %s");
		t.add(ModLang.NPC_GREET_RIGHTEOUS, "%s nods to you: \"May your path be clear and your heart steady.\"");
		t.add(ModLang.NPC_GREET_NEUTRAL, "%s regards you calmly and goes back to their own affairs.");
		t.add(ModLang.NPC_GREET_DEVILISH, "%s smiles thinly: \"Walk carefully, little one.\"");
		t.add(ModLang.NPC_GREET_HOSTILE, "%s glares at you with open killing intent.");
		t.add(ModLang.NPC_GREET_SECLUDED, "%s sits in deep seclusion and does not stir.");
		// Talking to cultivators (TalkSystem): the wheel, the dialogue box, and what they say.
		t.add("cultivation.defying-the-heavens.talk.and", "%s and %s");
		t.add("cultivation.defying-the-heavens.talk.back", "Esc  Back to the wheel");
		t.add("cultivation.defying-the-heavens.talk.busy", "%s is in no state to talk.");
		t.add("cultivation.defying-the-heavens.talk.direction.east", "east");
		t.add("cultivation.defying-the-heavens.talk.direction.north", "north");
		t.add("cultivation.defying-the-heavens.talk.direction.north_east", "north-east");
		t.add("cultivation.defying-the-heavens.talk.direction.north_west", "north-west");
		t.add("cultivation.defying-the-heavens.talk.direction.south", "south");
		t.add("cultivation.defying-the-heavens.talk.direction.south_east", "south-east");
		t.add("cultivation.defying-the-heavens.talk.direction.south_west", "south-west");
		t.add("cultivation.defying-the-heavens.talk.direction.west", "west");
		t.add("cultivation.defying-the-heavens.talk.gift_empty", "Hold what you mean to give in your hand.");
		t.add("cultivation.defying-the-heavens.talk.intent.ask", "Ask");
		t.add("cultivation.defying-the-heavens.talk.intent.ask.hint", "Open a conversation: who they are, their sect and its people, the sects nearby.");
		t.add("cultivation.defying-the-heavens.talk.intent.gift", "Gift");
		t.add("cultivation.defying-the-heavens.talk.intent.gift.hint", "Give what you hold. Old herbs, fine pills and Cultivation Fruit please the weak most; the strong are harder to impress.");
		t.add("cultivation.defying-the-heavens.talk.intent.greet", "Greet");
		t.add("cultivation.defying-the-heavens.talk.intent.greet.hint", "Bow, or cup your fist, as befits the two of you. Good manners are remembered, once a day.");
		t.add("cultivation.defying-the-heavens.talk.intent.provoke", "Provoke");
		t.add("cultivation.defying-the-heavens.talk.intent.provoke.hint", "Insult them. They will think less of you, and they may decide to teach you a lesson.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.commoner", "Me, Immortal? I know very little, but ask.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.commoner.mortal", "Ask away, friend.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.devilish.above", "Questions cost nothing. Answers might.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.devilish.below", "Senior wishes to know? Ask, ask.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.devilish.far_above", "You want something from me? How bold. Ask, before I lose interest.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.devilish.far_below", "This one will hold nothing back from Senior!");
		t.add("cultivation.defying-the-heavens.talk.line.ask.devilish.mortal", "A curious mortal. Ask; it amuses me.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.devilish.peer", "Ask. I'll decide what's true.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.neutral.above", "Ask.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.neutral.below", "Ask, Senior.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.neutral.far_above", "Be brief.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.neutral.far_below", "Senior need only ask.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.neutral.mortal", "What does a mortal want to know?");
		t.add("cultivation.defying-the-heavens.talk.line.ask.neutral.peer", "What would you know?");
		t.add("cultivation.defying-the-heavens.talk.line.ask.righteous.above", "Questions? Ask, then. I have a little time.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.righteous.below", "Senior may ask anything.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.righteous.far_above", "Speak quickly, junior. This one has little time.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.righteous.far_below", "Whatever Senior wishes to know, this one will answer.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.righteous.mortal", "Ask, then. Briefly.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.righteous.peer", "Ask, fellow Daoist. I'll answer what I can.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.enough.commoner", "You've given me plenty today, Immortal. Keep it.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.enough.devilish", "Again? Keep your trinkets.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.enough.neutral", "You've given me enough of those today.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.enough.righteous", "You are too generous. Keep it, friend.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.good.commoner", "This is too much, Immortal. Thank you!");
		t.add("cultivation.defying-the-heavens.talk.line.gift.good.devilish", "Now this is a proper gift.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.good.neutral", "Useful. My thanks.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.good.righteous", "This is generous. I won't forget it.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.great.commoner", "I... I don't know what to say. Thank you, Immortal!");
		t.add("cultivation.defying-the-heavens.talk.line.gift.great.devilish", "Ha! You do understand how the world works.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.great.neutral", "This is worth a great deal. I'll remember it.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.great.righteous", "Such a treasure... this one is in your debt.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.refuse.commoner", "I couldn't take that, Immortal.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.refuse.devilish", "Are you mocking me?");
		t.add("cultivation.defying-the-heavens.talk.line.gift.refuse.neutral", "What would I do with that?");
		t.add("cultivation.defying-the-heavens.talk.line.gift.refuse.righteous", "This one cannot accept that.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.small.commoner", "For me? Thank you, Immortal.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.small.devilish", "Is that all?");
		t.add("cultivation.defying-the-heavens.talk.line.gift.small.neutral", "Hm. Thank you.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.small.righteous", "A kind thought. Thank you.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.commoner", "Greetings, Immortal. Safe travels.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.commoner.mortal", "Good day to you, traveller.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.devilish.above", "Little one, you have manners. They might even keep you alive.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.devilish.below", "Senior... this one meant no offence. None at all.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.devilish.far_above", "An ant that bows. How sweet. Crawl along.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.devilish.far_below", "S-senior! This worthless one greets Senior!");
		t.add("cultivation.defying-the-heavens.talk.line.greet.devilish.mortal", "A mortal, walking right up to me. Brave, or stupid?");
		t.add("cultivation.defying-the-heavens.talk.line.greet.devilish.peer", "Fellow Daoist. Tea? It isn't poisoned. Probably.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.neutral.above", "Junior. What is it?");
		t.add("cultivation.defying-the-heavens.talk.line.greet.neutral.below", "Senior. This one is listening.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.neutral.far_above", "Hm.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.neutral.far_below", "Senior's presence is... considerable. How can this one help?");
		t.add("cultivation.defying-the-heavens.talk.line.greet.neutral.mortal", "A mortal. Keep out from underfoot and no one will trouble you.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.neutral.peer", "Fellow Daoist. Tea's hot, if you want it.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.righteous.above", "Well met, little friend. Your foundation looks steady.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.righteous.below", "Senior honours this one by stopping. Please, be at ease.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.righteous.far_above", "Junior. Keep to the path and mind your manners on this mountain.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.righteous.far_below", "This junior greets Senior! What does Senior require?");
		t.add("cultivation.defying-the-heavens.talk.line.greet.righteous.mortal", "A mortal this far up? Go carefully, and keep to the path.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.righteous.peer", "Fellow Daoist. Sit and take some tea with me; may your path be clear and your heart steady.");
		t.add("cultivation.defying-the-heavens.talk.line.nearby", "The %1$s, on the %2$s path, lies %3$s of here, %4$s blocks or so.");
		t.add("cultivation.defying-the-heavens.talk.line.nearby.enemy", "The %1$s, %2$s-path dogs, are %3$s of here, %4$s blocks or so. Keep clear of them.");
		t.add("cultivation.defying-the-heavens.talk.line.nearby.none", "No sect that I know of, not within a few days on foot.");
		t.add("cultivation.defying-the-heavens.talk.line.people", "Guarding the treasury and the core: %1$s. Sect Master %2$s keeps the main hall.");
		t.add("cultivation.defying-the-heavens.talk.line.people.alone", "There is no one else to answer for this sect.");
		t.add("cultivation.defying-the-heavens.talk.line.people.master_self", "Guarding the treasury and the core: %1$s. The main hall is mine.");
		t.add("cultivation.defying-the-heavens.talk.line.people.no_elders", "Sect Master %1$s keeps the main hall. We have no elders now.");
		t.add("cultivation.defying-the-heavens.talk.line.people.no_master", "Guarding the treasury and the core: %1$s. We have no master now.");
		t.add("cultivation.defying-the-heavens.talk.line.provoke.attack.commoner", "That's enough! I won't be spoken to like that!");
		t.add("cultivation.defying-the-heavens.talk.line.provoke.attack.devilish", "I was hoping you'd say that.");
		t.add("cultivation.defying-the-heavens.talk.line.provoke.attack.neutral", "You'll regret that.");
		t.add("cultivation.defying-the-heavens.talk.line.provoke.attack.righteous", "Then let your blade speak for you!");
		t.add("cultivation.defying-the-heavens.talk.line.provoke.scorn.commoner", "Please, Immortal, I don't want any trouble.");
		t.add("cultivation.defying-the-heavens.talk.line.provoke.scorn.devilish", "Barking at me? Cute.");
		t.add("cultivation.defying-the-heavens.talk.line.provoke.scorn.neutral", "I've no time for fools.");
		t.add("cultivation.defying-the-heavens.talk.line.provoke.scorn.righteous", "Your words dirty only yourself.");
		t.add("cultivation.defying-the-heavens.talk.line.sect", "The %1$s: %2$s of us, on the %3$s path, led by Sect Master %4$s at %5$s.");
		t.add("cultivation.defying-the-heavens.talk.line.sect.no_master", "The %1$s: %2$s of us, on the %3$s path.");
		t.add("cultivation.defying-the-heavens.talk.line.sect.self_master", "The %1$s: %2$s of us, on the %3$s path. I lead it.");
		t.add("cultivation.defying-the-heavens.talk.line.self.commoner", "%1$s. I'm only a mortal, Immortal. I have no cultivation to speak of.");
		t.add("cultivation.defying-the-heavens.talk.line.self.rogue.devilish", "%1$s, at %2$s. I answer to no sect, and to no one.");
		t.add("cultivation.defying-the-heavens.talk.line.self.rogue.neutral", "%1$s. %2$s. I go where the qi is.");
		t.add("cultivation.defying-the-heavens.talk.line.self.rogue.righteous", "I am %1$s, a wandering cultivator at %2$s. No sect, only the road.");
		t.add("cultivation.defying-the-heavens.talk.line.self.sect.devilish", "%1$s, %2$s of the %3$s, at %4$s. Remember the name. It may be the last one you learn.");
		t.add("cultivation.defying-the-heavens.talk.line.self.sect.neutral", "%1$s, %2$s of the %3$s. %4$s. That's all there is to say.");
		t.add("cultivation.defying-the-heavens.talk.line.self.sect.righteous", "I am %1$s, %2$s of the %3$s, at %4$s. I walk the righteous path.");
		t.add("cultivation.defying-the-heavens.talk.list", ", ");
		t.add("cultivation.defying-the-heavens.talk.mortal", "Mortal");
		t.add("cultivation.defying-the-heavens.talk.occupied", "%s is speaking with someone else.");
		t.add("cultivation.defying-the-heavens.talk.path.devilish", "devilish");
		t.add("cultivation.defying-the-heavens.talk.path.neutral", "neutral");
		t.add("cultivation.defying-the-heavens.talk.path.righteous", "righteous");
		t.add("cultivation.defying-the-heavens.talk.pick", "Click, or press 1-%s");
		t.add("cultivation.defying-the-heavens.talk.possessed", "%s doesn't seem to hear you.");
		t.add(ModLang.TALK_UNSEEN, "%s looks straight through you, as if nobody were there.");
		t.add(ModLang.TALK_UNSEEN_KING, "%s looks straight through you: the living don't see the dead. In your human form (Form Shift) they would.");
		// Talk phases 2-4 (TalkSystem, SparSystem, Rumours, CultivatorTrades) and spirit stones (SpiritStoneItem)
		t.add("cultivation.defying-the-heavens.talk.interrupted", "The blow breaks off your talk with %s.");
		t.add("cultivation.defying-the-heavens.talk.intent.spar", "Spar");
		t.add("cultivation.defying-the-heavens.talk.intent.spar.hint", "A bout of several exchanges, its blows pulled to half strength: it ends when either of you has lost half their health. Set terms: friendly, a wager on what you hold, or at their full strength.");
		t.add("cultivation.defying-the-heavens.talk.intent.trade", "Trade");
		t.add("cultivation.defying-the-heavens.talk.intent.trade.hint", "Barter for spirit stones. They sell by their station, buy herbs, pills and fruit from you, and the better they like you, the better the deal.");
		t.add("cultivation.defying-the-heavens.talk.intent.demand", "Demand");
		t.add("cultivation.defying-the-heavens.talk.intent.demand.hint", "Take tribute while your realm presses on them. They'll hate you for it, and your path darkens.");
		t.add("cultivation.defying-the-heavens.talk.intent.release", "Release");
		t.add("cultivation.defying-the-heavens.talk.intent.release.hint", "Lift your pressure off them, and let them stand.");
		t.add("cultivation.defying-the-heavens.talk.intent.spare", "Spare");
		t.add("cultivation.defying-the-heavens.talk.intent.spare.hint", "Let them live. They owe you a life, and your path brightens.");
		t.add("cultivation.defying-the-heavens.talk.intent.leave", "Leave");
		t.add("cultivation.defying-the-heavens.talk.intent.leave.hint", "Walk away.");
		t.add("cultivation.defying-the-heavens.talk.topic.rumours", "Rumours");
		t.add("cultivation.defying-the-heavens.talk.topic.guidance", "Guidance");
		t.add("cultivation.defying-the-heavens.talk.topic.soup", "Drink the soup");
		t.add("cultivation.defying-the-heavens.talk.rumour.stele", "A stele of the %1$s stands %2$s of here, in the %3$s, some %4$s blocks off. Sit by it a while and it will teach you.");
		t.add("cultivation.defying-the-heavens.talk.needs", "(needs %s)");
		t.add(ModLang.TRADE_LOCKED, "Locked: sold to those it regards as %s (you: %s)");
		t.add("cultivation.defying-the-heavens.talk.at_realm", "%s (%s)");
		t.add("cultivation.defying-the-heavens.talk.line.self.years", "I have cultivated some %s years.");
		t.add("cultivation.defying-the-heavens.talk.line.self.progress", "I'm about %s%% of the way through this stage.");
		t.add("cultivation.defying-the-heavens.talk.line.sect.enemy", "Our enemies, the %s, lie %s of here.");
		t.add("cultivation.defying-the-heavens.talk.line.people.gates", "Our gates are kept by %s.");
		t.add("cultivation.defying-the-heavens.talk.rumour.old", "%1$s grows %2$s of here, in the %3$s, some %4$s blocks off: %5$s years old, they say.");
		t.add("cultivation.defying-the-heavens.talk.rumour.cave", "A Heavenly Being, %1$s, sits in seclusion %2$s of here, in the %3$s, some %4$s blocks off. Disturb them at your peril.");
		t.add("cultivation.defying-the-heavens.talk.rumour.core", "The %1$s fell, but their Formation Core still stands unclaimed, %2$s of here in the %3$s, some %4$s blocks off.");
		t.add("cultivation.defying-the-heavens.talk.rumour.killing", "%1$s was slain %2$s of here, in the %3$s, some %4$s blocks off, %5$s. They say %6$s did it.");
		t.add("cultivation.defying-the-heavens.talk.rumour.today", "today");
		t.add("cultivation.defying-the-heavens.talk.rumour.yesterday", "yesterday");
		t.add("cultivation.defying-the-heavens.talk.rumour.days_ago", "%s days ago");
		t.add("cultivation.defying-the-heavens.talk.rumour.about", "about %s");
		t.add("cultivation.defying-the-heavens.talk.rumour.between", "%s to %s");
		t.add("cultivation.defying-the-heavens.talk.rumour.wilds", "wilds");
		t.add("cultivation.defying-the-heavens.talk.rumour.subject.fruit", "A Cultivation Fruit");
		t.add("cultivation.defying-the-heavens.talk.rumour.subject.peach_tree", "A Spirit Peach Tree");
		t.add("cultivation.defying-the-heavens.talk.line.rumour.none.righteous", "I've heard nothing worth repeating, I'm afraid.");
		t.add("cultivation.defying-the-heavens.talk.line.rumour.none.neutral", "Nothing new.");
		t.add("cultivation.defying-the-heavens.talk.line.rumour.none.devilish", "Gossip? Go bore someone else.");
		t.add("cultivation.defying-the-heavens.talk.line.rumour.none.commoner", "Nothing, I fear. Quiet times.");
		t.add("cultivation.defying-the-heavens.talk.journal_noted", "Noted in your journal.");
		t.add("cultivation.defying-the-heavens.talk.line.guidance.righteous", "Breathe out slower than you breathe in, and let the qi settle at the dantian before you push. Sit with that.");
		t.add("cultivation.defying-the-heavens.talk.line.guidance.neutral", "Your circulation stumbles at the third meridian. Fix it.");
		t.add("cultivation.defying-the-heavens.talk.line.guidance.devilish", "You force your qi like a drunk forcing a door. Coax it. There: free advice.");
		t.add("cultivation.defying-the-heavens.talk.line.guidance.again.righteous", "That is enough for one day. Let it settle.");
		t.add("cultivation.defying-the-heavens.talk.line.guidance.again.neutral", "Enough for today.");
		t.add("cultivation.defying-the-heavens.talk.line.guidance.again.devilish", "Greedy. Come back tomorrow.");
		t.add("cultivation.defying-the-heavens.talk.insight", "%s's guidance: +%s%% cultivation from meditation, for %s minutes.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.terms.righteous", "A friendly bout? Gladly. Name your terms, and we keep to them.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.terms.neutral", "A spar. Terms?");
		t.add("cultivation.defying-the-heavens.talk.line.spar.terms.devilish", "You want to trade blows with me? Name your terms, and try not to weep.");
		t.add("cultivation.defying-the-heavens.talk.spar_terms.friendly", "A friendly spar");
		t.add("cultivation.defying-the-heavens.talk.spar_terms.wager", "For a wager: what I hold");
		t.add("cultivation.defying-the-heavens.talk.spar_terms.full", "Don't hold back");
		t.add("cultivation.defying-the-heavens.talk.spar_terms.wager_full", "A wager, and don't hold back");
		t.add("cultivation.defying-the-heavens.spar.busy", "Not now; I already have a bout to settle.");
		t.add("cultivation.defying-the-heavens.spar.hurt", "Rest first. Neither of us should spar like this.");
		t.add("cultivation.defying-the-heavens.spar.no_stake", "A wager needs a stake. Hold what you'd put up.");
		t.add("cultivation.defying-the-heavens.spar.wagered", "No more wagers today; once is enough.");
		t.add("cultivation.defying-the-heavens.spar.hold_stake", "(hold a herb, pill or stones)");
		t.add("cultivation.defying-the-heavens.spar.beneath", "A wager, against you? I'd only be handing it over. A friendly bout, if you like.");
		t.add("cultivation.defying-the-heavens.spar.friendly_only", "(friendly only: beneath your realm)");
		t.add("cultivation.defying-the-heavens.spar.stakes", "Stakes: your %s against their %s. The winner takes both.");
		t.add("cultivation.defying-the-heavens.spar.count", "%s %s");
		t.add("cultivation.defying-the-heavens.spar.begin", "%s takes a stance. Three breaths, then begin!");
		t.add(ModLang.SPAR_FOLLOW, "%s leads the way to the sparring ground. Follow: the bout begins there.");
		t.add(ModLang.SPAR_LEADING, "Follow %s to the sparring ground.");
		t.add(ModLang.SPAR_WAITING, "%s waits for you at the sparring ground.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.called_off.righteous", "You did not come. Another day, then.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.called_off.neutral", "Not coming? Then there's no bout.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.called_off.devilish", "Wasting my time. Begone.");
		t.add("cultivation.defying-the-heavens.spar.countdown", "%s...");
		t.add("cultivation.defying-the-heavens.spar.fight", "Begin!");
		t.add("cultivation.defying-the-heavens.talk.line.spar.won.righteous", "Well fought. I am bested, and gladly.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.won.neutral", "You win.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.won.devilish", "Tch. Lucky. Next time I won't be so kind.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.lost.righteous", "A good bout. Mind your footwork, and you'll have me next time.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.lost.neutral", "Again, when you're stronger.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.lost.devilish", "Pathetic. Come back when you can stand.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.forfeit.righteous", "You leave mid-bout? That is no way to spar.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.forfeit.neutral", "Walked off. Hm.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.forfeit.devilish", "Running already? I'll remember that.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.draw.righteous", "Neither yields. An honourable draw.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.draw.neutral", "Even. Enough for today.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.draw.devilish", "We'll finish this another day.");
		t.add("cultivation.defying-the-heavens.talk.line.tribute.righteous", "Take it, then. The heavens see what you do.");
		t.add("cultivation.defying-the-heavens.talk.line.tribute.neutral", "Take it. Go.");
		t.add("cultivation.defying-the-heavens.talk.line.tribute.devilish", "Hah... take it. I won't forget your face.");
		t.add("cultivation.defying-the-heavens.talk.line.tribute.commoner", "Please, take it, take it all...");
		t.add("cultivation.defying-the-heavens.talk.line.tribute.again.righteous", "I have nothing more to give you today.");
		t.add("cultivation.defying-the-heavens.talk.line.tribute.again.neutral", "Nothing left.");
		t.add("cultivation.defying-the-heavens.talk.line.tribute.again.devilish", "You've bled me once today already.");
		t.add("cultivation.defying-the-heavens.talk.line.tribute.again.commoner", "There's nothing more, I swear it!");
		t.add("cultivation.defying-the-heavens.talk.line.release.righteous", "Senior is merciful. This one thanks you.");
		t.add("cultivation.defying-the-heavens.talk.line.release.neutral", "...Thank you.");
		t.add("cultivation.defying-the-heavens.talk.line.release.devilish", "Hah. Generous of you.");
		t.add("cultivation.defying-the-heavens.talk.line.release.commoner", "Thank you, immortal, thank you!");
		t.add("cultivation.defying-the-heavens.talk.line.spared.righteous", "This one owes you a life. I will not forget it.");
		t.add("cultivation.defying-the-heavens.talk.line.spared.neutral", "I owe you. I won't forget.");
		t.add("cultivation.defying-the-heavens.talk.line.spared.devilish", "...You let me live? Fool. Or perhaps not.");
		t.add("cultivation.defying-the-heavens.talk.line.spared.commoner", "Bless you, immortal! Bless you!");
		t.add("cultivation.defying-the-heavens.talk.begs", "%s throws themself to the ground and begs for their life. (Right-click them to answer.)");
		t.add("cultivation.defying-the-heavens.talk.aid", "%s comes to your aid!");
		t.add("cultivation.defying-the-heavens.talk.gift_back", "%s gives you %s.");
		t.add("cultivation.defying-the-heavens.message.stone.mortal", "Your meridians are closed; the stone's qi has nowhere to go.");
		t.add("cultivation.defying-the-heavens.message.stone.qi_full", "Your qi is already full.");
		t.add("cultivation.defying-the-heavens.message.stone.absorbed", "You draw in the stone's qi: +%s qi.");
		t.add("cultivation.defying-the-heavens.stone.worth", "Worth %s Low-grade Spirit Stones");
		t.add("cultivation.defying-the-heavens.stone.absorb", "Hold use to absorb: +%s qi");
		t.add("cultivation.defying-the-heavens.stone.meditate", "Held while meditating: +%s%% cultivation");
		t.add("cultivation.defying-the-heavens.stone.qi_left", "Qi left in this stone: %s%%");
		t.add(ModLang.STONE_FORTUNE, "A settlement's fortune, shared with %s: absorbed by them, it fills their Axis of Fortune");
		t.add("cultivation.defying-the-heavens.tab.journal", "Journal");
		t.add("cultivation.defying-the-heavens.journal.empty", "Nothing yet. Cultivators who think well enough of you (Courteous) will tell you rumours if you ask; what they tell you is noted here.");
		t.add("cultivation.defying-the-heavens.journal.entry", "Day %s, from %s");
		t.add("item.defying-the-heavens.low_spirit_stone", "Low-grade Spirit Stone");
		t.add("item.defying-the-heavens.mid_spirit_stone", "Mid-grade Spirit Stone");
		t.add("item.defying-the-heavens.high_spirit_stone", "High-grade Spirit Stone");
		t.add("item.defying-the-heavens.supreme_spirit_stone", "Supreme-grade Spirit Stone");
		t.add("item.defying-the-heavens.immortal_spirit_stone", "Immortal-grade Spirit Stone");
		t.add("item.defying-the-heavens.immortal_jade", "Immortal Jade");
		t.add(ModLang.STONE_JADE, "The Immortal Realm's coin: nine Immortal-grade Spirit Stones");
		t.add("block.defying-the-heavens.spirit_stone_ore", "Spirit Stone Vein");
		t.add("block.defying-the-heavens.deepslate_spirit_stone_ore", "Deepslate Spirit Stone Vein");
		t.add("block.defying-the-heavens.rich_spirit_stone_ore", "Rich Spirit Stone Vein");
		t.add("effect.defying-the-heavens.insight", "Insight");
		t.add("cultivation.defying-the-heavens.talk.regard", "Regard");
		t.add("cultivation.defying-the-heavens.talk.regard.courteous", "Courteous");
		t.add("cultivation.defying-the-heavens.talk.regard.hostile", "Hostile");
		t.add("cultivation.defying-the-heavens.talk.regard.indifferent", "Indifferent");
		t.add("cultivation.defying-the-heavens.talk.regard.polite", "Polite");
		t.add("cultivation.defying-the-heavens.talk.regard.resentful", "Resentful");
		t.add("cultivation.defying-the-heavens.talk.regard.sworn", "Sworn friend");
		t.add("cultivation.defying-the-heavens.talk.regard.trusted", "Trusted");
		t.add("cultivation.defying-the-heavens.talk.regard.warm", "Warm");
		t.add("cultivation.defying-the-heavens.talk.regard_delta", "  Regard %s");
		t.add("cultivation.defying-the-heavens.talk.says", "%s: %s");
		t.add("cultivation.defying-the-heavens.talk.titled", "%s %s");
		t.add("cultivation.defying-the-heavens.talk.topic.nearby", "Any other sects near here?");
		t.add("cultivation.defying-the-heavens.talk.topic.people", "Who leads it?");
		t.add("cultivation.defying-the-heavens.talk.topic.sect", "Tell me about your sect.");
		t.add("cultivation.defying-the-heavens.talk.topic.self", "Who are you?");
		t.add("cultivation.defying-the-heavens.talk.topic.method", "Will you teach me your method?");
		t.add(ModLang.MSG_SECLUSION_REPULSE, "A concealment barrier pushes you back. Disturbing a secluded cultivator would be unwise.");

		// Cultivator clothing
		for (ClothingStyle style : ClothingStyle.values()) {
			t.add(ModLang.clothingStyleKey(style.getId()), switch (style) {
				case RIGHTEOUS -> "Righteous white";
				case NEUTRAL -> "Neutral blue";
				case DEVILISH -> "Devilish red and black";
			});
		}
		for (ClothingTier tier : ClothingTier.values()) {
			String grade = switch (tier) {
				case MORTAL -> "Mortal-Grade";
				case SPIRIT -> "Spirit-Grade";
				case EARTH -> "Earth-Grade";
				case HEAVEN -> "Heaven-Grade";
				case IMMORTAL -> "Immortal-Grade";
			};
			for (ArmorItem.Type type : ArmorItem.Type.values()) {
				String piece = switch (type) {
					case HELMET -> "Cultivator's Hair Crown";
					case CHESTPLATE -> "Cultivator's Robe";
					case LEGGINGS -> "Cultivator's Trousers";
					case BOOTS -> "Cloud-Stepping Boots";
				};
				t.add(ModItems.clothing(tier, type), grade + " " + piece);
			}
		}
		t.add(ModLang.CLOTHING_QI, "+%s%% qi gathering");
		t.add(ModLang.CLOTHING_MEDITATION, "+%s%% cultivation while meditating");
		t.add(ModLang.CLOTHING_UPGRADE_HINT, "Dye it, or raise its grade, at a crafting table");
		t.add(ModLang.BOOST_GARMENTS, "Cultivator's Garments");

		// Formations
		t.add(ModBlocks.QI_VEIN, "Qi Vein");
		t.add(ModBlocks.DEEPSLATE_QI_VEIN, "Deepslate Qi Vein");
		t.add(ModBlocks.FORMATION_CORE, "Formation Core");
		t.add(ModBlocks.SECT_BARRIER, "Formation Barrier");
		t.add(ModBlocks.SEAL, "Seal of Qi Ink");
		t.add(ModItems.INSCRIPTION_BRUSH, "Inscription Brush");
		t.add(ModLang.CORE_TOOLTIP, "Raises a protective barrier, fed by Qi Veins joined to it with seals");
		t.add(ModLang.CORE_TITLE, "Formation Core");
		t.add(ModLang.CORE_OWNER, "Keeper: %s");
		t.add(ModLang.CORE_RADIUS, "Barrier radius: %s");
		t.add(ModLang.CORE_SWITCH_ON, "Raise the Barrier");
		t.add(ModLang.CORE_SWITCH_OFF, "Lower the Barrier");
		t.add(ModLang.CORE_BATTERY, "Stored qi: %s / %s");
		t.add(ModLang.CORE_VEINS, "Qi Veins joined: %s (+%s qi/s)");
		t.add(ModLang.CORE_UPKEEP, "Barrier upkeep: %s qi/s");
		t.add(ModLang.CORE_SHORTFALL, "The veins fall short: the store runs dry in %s s");
		t.add(ModLang.CORE_STRENGTH, "Only %s or stronger can break through");
		t.add(ModLang.CORE_STRENGTH_MORTAL, "Set by a mortal: anyone can break through");
		t.add(ModLang.CORE_RAISED, "The barrier stands.");
		t.add(ModLang.CORE_LOWERED, "The barrier is down.");
		t.add(ModLang.CORE_SHATTERED, "The barrier was shattered. It can rise again in %s s.");
		t.add(ModLang.CORE_HINT, "Paint seals from the core to Qi Veins with an Inscription Brush (within 64 blocks). Only Qi Sense shows them.");
		t.add(ModLang.MSG_CORE_SECT, "The formation of the %s refuses your touch.");
		t.add(ModLang.MSG_CORE_NOT_YOURS, "This formation answers only to %s.");
		t.add(ModLang.MSG_BARRIER_TOO_STRONG, "The formation is too strong for your cultivation to break.");
		t.add(ModLang.MSG_BARRIER_GROUND, "A formation's shell runs through this ground. You are not strong enough to dig through it.");
		t.add(ModLang.MSG_BARRIER_EXPELLED, "The formation will not suffer one so weak within it. You are cast out.");
		t.add(ModLang.MSG_SEAL_UNSEEN, "The qi ink sinks into the surface, unseen by mortal eyes. Only Qi Sense reveals it.");
		t.add(ModLang.MSG_TREASURY_SEALED, "A seal of %s holds it shut. Only one a realm above its elders could break it.");
		t.add(ModLang.MSG_TREASURY_SEAL_BROKEN, "The elders' seal shatters under your hand. The treasury lies open.");
		// Players in sects (SectMembership)
		t.add(ModLang.MSG_SECT_JOINED, "You are now a disciple of the %s: %s.");
		t.add(ModLang.MSG_SECT_LEFT, "You have left the %s.");
		t.add(ModLang.MSG_SECT_EXPELLED, "You have been cast out of the %s: %s");
		t.add(ModLang.MSG_SECT_EXPELLED_TREASURY, "you laid hands on the elders' treasury again.");
		t.add(ModLang.MSG_SECT_EXPELLED_VIOLENCE, "you struck one of your own sect.");
		t.add(ModLang.MSG_SECT_EXPELLED_BANISHED, "by the word of one of its seniors.");
		t.add(ModLang.MSG_SECT_BODY_LEFT, "%s leaves the %s.");
		t.add(ModLang.MSG_SECT_BANISHED, "%s is cast out of the %s.");
		t.add(ModLang.MSG_SECT_TREASURY_WARNING, "The treasury is the elders' alone. Lay hands on it again and you will be cast out.");
		t.add(ModLang.MSG_SECT_TITLE, "By your strength you are now %s of the %s.");
		t.add(ModLang.STATS_SECT, "%s - %s");
		t.add(ModLang.TAB_SECT, "Sect");
		t.add(ModLang.SECT_TAB_LOADING, "...");
		t.add(ModLang.SECT_TAB_NONE, "You belong to no sect.");
		t.add(ModLang.SECT_TAB_HOW, "To join one, talk to any of its members and ask to join: its disciples send you to an elder, who sets you an entrance trial (a spar, holding back to your realm; win it or stand your ground to the end). A body of High grade or better, or an elder Warm to you, needs no trial. Its disciples cultivate a quarter faster on its grounds, pass its barrier, and are helped by its members.");
		t.add(ModLang.SECT_TAB_NEAREST, "Nearest: %s (%s), %s blocks to the %s.");
		t.add(ModLang.SECT_TAB_NEAREST_NONE, "No sect is known of within 4,000 blocks.");
		t.add(ModLang.SECT_TAB_BARRED, "It won't hear you for %s more day(s).");
		t.add(ModLang.SECT_TAB_TITLE, "Your title: %s");
		t.add(ModLang.SECT_TAB_STANDING, "Its regard for you: %s (%s)");
		t.add(ModLang.SECT_TAB_GROUNDS_HERE, "Grounds: you stand on them (cultivating %s%% faster)");
		t.add(ModLang.SECT_TAB_GROUNDS_AWAY, "Grounds: %s blocks to the %s");
		t.add(ModLang.SECT_TAB_GROUNDS_ELSEWHERE, "Grounds: in another world");
		t.add(ModLang.SECT_TAB_BARRIER_UP, "Barrier: raised (it lets you through)");
		t.add(ModLang.SECT_TAB_BARRIER_DOWN, "Barrier: down");
		t.add(ModLang.SECT_TAB_TREASURY_OPEN, "Treasury: yours to open (Elder and above)");
		t.add(ModLang.SECT_TAB_TREASURY_CLOSED, "Treasury: elders only (a first try: a warning)");
		t.add(ModLang.SECT_TAB_TREASURY_LAST, "Treasury: warned once; another try casts you out");
		t.add(ModLang.SECT_TAB_MEMBERS, "Members: %s cultivator(s), %s player(s)");
		t.add(ModLang.SECT_TAB_DAYS, "A disciple for %s day(s). To leave, ask an elder.");
		t.add(ModLang.SECT_TAB_LEAVE, "To leave, ask an elder.");
		t.add(ModLang.SECT_TAB_PLAYER, "%s (player)");
		t.add(ModLang.SECT_TAB_OFFLINE, "away");
		t.add(ModLang.SECT_TAB_BODY, "As the body you are in:");
		t.add(ModLang.SECT_TAB_LEAVE_BUTTON, "[Leave the sect]");
		t.add(ModLang.SECT_TAB_BANISH_BUTTON, "[Banish]");
		t.add(ModLang.SECT_TAB_SURE, "[Sure?]");
		for (StatPoints.Stat stat : StatPoints.Stat.values()) {
			String name = switch (stat) {
				case STRENGTH -> "Strength";
				case CONSTITUTION -> "Constitution";
				case AGILITY -> "Agility";
				case ENDURANCE -> "Endurance";
				case SPIRIT -> "Spirit";
				case INSIGHT -> "Insight";
			};
			String effect = switch (stat) {
				case STRENGTH -> "+%s%% melee harm";
				case CONSTITUTION -> "+%s%% max health";
				case AGILITY -> "+%s%% speed, attack speed";
				case ENDURANCE -> "+%s%% armour, toughness";
				case SPIRIT -> "+%s%% qi pools";
				case INSIGHT -> "+%s%% cultivation, half in qi";
			};
			t.add(ModLang.STAT_POINT_PREFIX + stat.getId(), name);
			t.add(ModLang.STAT_POINT_PREFIX + stat.getId() + ".effect", effect);
		}
		t.add(ModLang.MSG_LIFESPAN_WARN, "Your body grows old: %s years of your lifespan remain. Break through to a new realm, or win years another way, before they run out: the heavens come to collect a spent life.");
		t.add(ModLang.MSG_LIFESPAN_END, "Your spent life is over. Everything you cultivated went with it: you begin again as a mortal, in a body born anew.");
		t.add(ModLang.MSG_SOUL_TIME_WARN, "Your soul thins among the dead: %s years of your lifespan remain before the Netherworld comes to collect it.");
		t.add(ModLang.MSG_SOUL_TIME_END, "Your lifespan is spent among the dead. Your soul scatters into the Soul River and is born again, a mortal remembering nothing of its cultivation.");
		t.add(ModLang.STATS_LIFESPAN, "Age: %s of %s years");
		t.add(ModLang.STATS_LIFESPAN_ENDLESS, "Age: %s, an immortal's years without end");
		t.add(ModLang.CMD_LIFESPAN, "%s: %s of %s years (%s left)");
		t.add(ModLang.CMD_LIFESPAN_ENDLESS, "%s: %s years lived, an immortal's without end");
		t.add(ModLang.LIFE_TRIB_HUD, "Lifespan Tribulation: %s%%");
		t.add(ModLang.LIFE_TRIB_HUD_DEAD, "The Netherworld's Lifespan Tribulation: %s%%");
		t.add(ModLang.LIFE_TRIB_HUD_WAITING, "Lifespan Tribulation, waiting beyond the gap: %s%%");
		t.add(ModLang.MSG_LIFE_TRIB_START, "Your lifespan is spent, and the heavens come to collect it: a tribulation that grows until nothing can bear it. Break through into a new realm, or take years from the grateful, before it takes you.");
		t.add(ModLang.MSG_LIFE_TRIB_START_DEAD, "Your lifespan is spent, and the Netherworld comes to collect your soul: a yin tribulation that grows until nothing can bear it. Break through into a new realm before it scatters you into the Soul River.");
		t.add(ModLang.MSG_LIFE_TRIB_BROADCAST, "The heavens come for %s: their lifespan is spent.");
		t.add(ModLang.MSG_LIFE_TRIB_PAUSED, "The heavens lose sight of you in the Spatial Gap: your lifespan tribulation waits, as strong as it was, until you come out.");
		t.add(ModLang.MSG_LIFE_TRIB_RESUMED, "The heavens find you again: your lifespan tribulation goes on, as strong as it was.");
		t.add(ModLang.MSG_LIFE_TRIB_RENEWED, "Your lifespan is renewed: the heavens withdraw.");
		t.add(ModLang.MSG_LIFE_TRIB_RENEWED_BROADCAST, "%s has won new years: the heavens withdraw.");
		t.add(ModLang.MSG_LIFE_TRIB_NO_PILL, "No pill buys a spent life back from the heavens.");
		t.add(ModLang.MSG_POSSESS_HEAVENS, "The heavens come for %s's spent life: you are driven out of the body.");
		t.add(ModLang.MSG_SOUL_BODY_TAKEN, "The heavens strike your body down where it sits: your soul goes with it.");
		// What years are spent on besides (the user's asks, 2026-10-21): the meditation's pace, space travel, Burn Lifespan, Life
		// Force's years, a mortal's old age.
		t.add(ModLang.BTN_PACE, "×%s");
		t.add(ModLang.TIP_PACE_1, "Meditation pace ×1: your own pace. Click for ×2 (twice as fast, each bit of it for twice the years).");
		t.add(ModLang.TIP_PACE_2, "Meditation pace ×2: you cultivate twice as fast, but each bit of it costs twice the years (four times the years a minute). Click for ×3.");
		t.add(ModLang.TIP_PACE_3, "Meditation pace ×3: you cultivate three times as fast, but each bit of it costs four times the years (twelve times the years a minute). Click for ×1.");
		t.add(ModLang.MSG_PACE_1, "Meditation pace ×1: your own pace.");
		t.add(ModLang.MSG_PACE_2, "Meditation pace ×2: twice as fast, each bit for twice the years.");
		t.add(ModLang.MSG_PACE_3, "Meditation pace ×3: three times as fast, each bit for four times the years.");
		t.add(ModLang.MSG_MEDITATION_SLOWED, "Your years run short: your meditation slows to its own pace (×1). You may set it faster again.");
		t.add(ModLang.SECLUSION_LINE, "Years to this realm's peak: %s (of %s left)");
		t.add(ModLang.HUD_SPACE_YEARS, "Space: %s years spent · %s left · %s to fly home");
		t.add(ModLang.HUD_SPACE_HOME, "Home, the Overworld: %s blocks");
		t.add(ModLang.HUD_SPACE_HOME_ABOVE, "Home, the Overworld: %s blocks, above you");
		t.add(ModLang.HUD_SPACE_HOME_BELOW, "Home, the Overworld: %s blocks, below you");
		t.add(ModLang.HUD_PLANET_WAY_OUT, "To leave: fly up past Y %s, then follow the way home");
		t.add(ModLang.MSG_SPACE_NO_WAY_HOME, "You no longer have the years to fly straight home: it would take %s, and you have %s left.");
		t.add(ModLang.MSG_OLD_AGE, "Your body grows old: from now on it weakens, a little more every year (a cultivator's never does).");
		t.add(ModLang.MSG_BURN_ON, "Your lifespan burns: %s years a second (%s%% of it).");
		t.add(ModLang.MSG_BURN_ON_ENDLESS, "Your lifespan burns, and an immortal's years have no end: it costs you nothing.");
		t.add(ModLang.MSG_BURN_OFF, "Your lifespan stops burning.");
		t.add(ModLang.MSG_BURN_OUT, "Too little of your life is left to burn: the flame goes out.");
		t.add(ModLang.MSG_BURN_TOO_FEW, "Too little of your life is left to burn.");
		t.add(ModLang.MSG_ESSENCE_ON, "Your immortal essence flows: your blows and spells strike %s%% harder, for %s seconds of it.");
		t.add(ModLang.MSG_ESSENCE_OFF, "Your immortal essence settles.");
		t.add(ModLang.MSG_ESSENCE_SPENT, "Your immortal essence is spent. Only the Immortal Realm (or a small world) fills it again.");
		t.add(ModLang.MSG_ESSENCE_TOO_LITTLE, "Too little immortal essence is left in you. Only the Immortal Realm (or a small world) fills it again.");
		t.add(ModLang.MSG_DOMAIN_ON, "You unfurl the domain of your %s, %s blocks round you: %s seconds of your essence.");
		t.add(ModLang.MSG_DOMAIN_OFF, "The domain of your %s folds away.");
		t.add(ModLang.MSG_DOMAIN_SPENT, "Your immortal essence is spent: the domain of your %s folds away. Only the Immortal Realm (or a small world) fills it again.");
		t.add(ModLang.MSG_DOMAIN_TOO_LITTLE, "Too little immortal essence is left in you to unfurl a domain. Only the Immortal Realm (or a small world) fills it again.");
		t.add(ModLang.MSG_DOMAIN_LOST, "The domain of your %s can't hold here, and folds away.");
		t.add(ModLang.MSG_DOMAIN_SPACE_LOCKED, "A domain of the Law of Space locks space here: it won't open for you.");
		t.add(ModLang.MSG_DOMAIN_UNMADE, "The Unmaking tears your domain of %s apart. It folds away.");
		// The Chaos Frontier (ChaosFrontier, FirstFlame, phase 3c): crossing in and out, its storm, the First Flame's spring, the Primordial
		// Chaos's Unmaking, its stones and fragments.
		t.add(ModLang.MSG_CHAOS_ENTER, "You cross into the Chaos Frontier. Here chaos from before heaven and earth still churns: unsheltered, its storm unmakes you, and your body will not mend of itself. A Law's domain shelters you, or the First Flame's light.");
		t.add(ModLang.MSG_CHAOS_LEAVE, "You leave the Chaos Frontier behind.");
		t.add(ModLang.MSG_CHAOS_EXPOSED, "The chaos storm tears at you!");
		t.add(ModLang.MSG_CHAOS_SHELTERED, "Sheltered from the chaos storm.");
		t.add(ModLang.MSG_FIRST_FLAME, "A white-gold flame burns here, as it burned before heaven and earth were parted: the First Flame. In its light the storm can't reach you, and the Laws come the clearer.");
		t.add(ModLang.MSG_CHAOS_UNMAKING, "The Primordial Chaos draws in on itself... get clear!");
		t.add(ModLang.MSG_DAO_FRAGMENT, "The Dao Fragment dissolves into you: insight into the Law of %s.");
		t.add("item.defying-the-heavens.dao_fragment", "Dao Fragment");
		t.add(ModLang.ITEM_DAO_FRAGMENT_OF, "Dao Fragment of %s");
		t.add(ModLang.TOOLTIP_DAO_FRAGMENT, "Use it to take in its Law: %s%% of the next level's insight. Greater Laws only from Golden Immortal.");
		t.add("item.defying-the-heavens.chaos_origin_stone", "Chaos Origin Stone");
		t.add(ModLang.TOOLTIP_CHAOS_ORIGIN_STONE, "Chaos from before heaven and earth, set hard. A small world's seed is made of it.");
		t.add(ModBlocks.CHAOS_ORIGIN_ORE, "Chaos Origin Ore");
		t.add(ModBlocks.FIRST_FLAME, "First Flame");
		// The small worlds (SmallWorlds, phase 3d): the seed, its refining, the door, coming in and going out, the land grown, the guest list.
		t.add("item.defying-the-heavens.world_seed", "World Seed");
		t.add(ModLang.TOOLTIP_WORLD_SEED, "Chaos Origin Stones and Immortal Jade made one. A True Immortal (or one past it) who holds it in use for %s s refines it into a small world of their own.");
		t.add(ModBlocks.SMALL_WORLD_GATE, "Small World Gate");
		t.add("entity.defying-the-heavens.small_world_door", "Small World Door");
		t.add(ModLang.biomeKey("small_world"), "Small World");
		t.add(ModLang.MSG_SEED_NOT_IMMORTAL, "Only a True Immortal, or one past it, can refine a world out of the seed.");
		t.add(ModLang.MSG_SEED_HAVE_ONE, "You have a small world already: one is all an immortal holds.");
		t.add(ModLang.MSG_SEED_REFINING, "You draw chaos into the seed... hold it %s s.");
		t.add(ModLang.MSG_SEED_REFINED, "The seed takes root in the void and opens into a world of your own. Its land reaches %s blocks out, and grows with every level of the Laws you hold. Open its door with the Small World ability.");
		t.add(ModLang.MSG_DOOR_OPEN, "The door to your world stands open for %s s.");
		t.add(ModLang.MSG_DOOR_CLOSED, "The door to your world closes.");
		t.add(ModLang.MSG_DOOR_NOT_HERE, "Your world's door won't open here.");
		t.add(ModLang.MSG_DOOR_NOT_IMMORTAL, "Fallen below True Immortal, you can't open your world's door.");
		t.add(ModLang.MSG_DOOR_TURNED_AWAY, "The door to %s's world isn't open to you.");
		t.add(ModLang.MSG_WORLD_OWN, "Your own world. Its qi is the Immortal Realm's: x%s.");
		t.add(ModLang.MSG_WORLD_GUEST, "%s's world, a guest's welcome. Its gate is the way out.");
		t.add(ModLang.MSG_WORLD_LEFT, "You step out of the small world.");
		t.add(ModLang.MSG_WORLD_GROWN, "Your world grows with your Laws: its land reaches %s blocks out.");
		t.add(ModLang.MSG_WORLD_CAUGHT, "Beyond the island is nothing: the world sets you back on its land.");
		t.add(ModLang.MSG_WORLD_SENT_OUT, "This world isn't open to you any more: you are sent out of it.");
		t.add(ModLang.MSG_WORLD_BORNE, "Your small world comes with you, and the %s inside it. With everyone you bring, the spatial pressure on you is %s times what it would be.");
		t.add(ModLang.MSG_WORLD_CARRIED_UP, "%s has carried this world through to the other side: its gate now leads out beside them there.");
		t.add(ModLang.MSG_WORLD_BROUGHT, "You brought %s through in your small world: its gate now leads out here.");
		t.add(ModLang.MSG_GUEST_INVITED, "%s has put you on the guest list of their small world: you may walk through its door.");
		t.add(ModLang.CMD_WORLD_NONE, "You have no small world: refine a World Seed (five Chaos Origin Stones and four Immortal Jade) as a True Immortal.");
		t.add(ModLang.CMD_WORLD_INFO, "Your small world: its land reaches %s blocks out (%s laid so far), %s guests.");
		t.add(ModLang.CMD_WORLD_GUESTS, "Your world's guests: %s.");
		t.add(ModLang.CMD_WORLD_NO_GUESTS, "Your world has no guests yet: /smallworld guest add <player>.");
		t.add(ModLang.CMD_GUEST_ADDED, "%s is on your world's guest list.");
		t.add(ModLang.CMD_GUEST_ALREADY, "%s is on your world's guest list already.");
		t.add(ModLang.CMD_GUEST_SELF, "Your world is your own: no need to be your own guest.");
		t.add(ModLang.CMD_GUEST_REMOVED, "%s is off your world's guest list.");
		t.add(ModLang.CMD_GUEST_UNKNOWN, "No guest of your world is named %s.");
		// The titles players take (PlayerTitles, 2026-10-27)
		t.add(ModLang.TITLE_LABEL, "Title");
		t.add(ModLang.TITLE_HINT, "Name yourself (Enter to set)");
		t.add(ModLang.TITLE_TIP, "From Void Shattering on you may take a title: up to 50 characters. Type it and press Enter; empty the field and press Enter to clear it. It stands over your name, above your sect's title, and cultivators call you by it.");
		t.add(ModLang.MSG_TITLE_SET, "You now go by the title %s.");
		t.add(ModLang.MSG_TITLE_ANNOUNCED, "%s has taken the title %s");
		t.add(ModLang.MSG_TITLE_CLEARED, "You go by no title now.");
		t.add(ModLang.MSG_TITLE_LOCKED, "Only from Void Shattering on may you take a title.");
		// What cultivators call a player (NpcAddress, 2026-10-27): by how far above them the player stands, or below
		t.add(ModLang.address("ancestor"), "Ancestor");
		t.add(ModLang.address("lord_immortal"), "Lord Immortal");
		t.add(ModLang.address("revered"), "Revered Senior");
		t.add(ModLang.address("esteemed"), "Esteemed Senior");
		t.add(ModLang.address("senior"), "Senior");
		t.add(ModLang.address("peer"), "Fellow Daoist");
		t.add(ModLang.address("friend"), "friend");
		t.add(ModLang.address("immortal"), "Immortal %s");
		t.add(ModLang.address("above.righteous"), "young friend");
		t.add(ModLang.address("above.neutral"), "junior");
		t.add(ModLang.address("above.devilish"), "brat");
		t.add(ModLang.address("above.ghost"), "young soul");
		t.add(ModLang.address("far_above.righteous"), "little friend");
		t.add(ModLang.address("far_above.neutral"), "junior");
		t.add(ModLang.address("far_above.devilish"), "ant");
		t.add(ModLang.address("far_above.ghost"), "little soul");
		t.add(ModLang.address("mortal.righteous"), "young one");
		t.add(ModLang.address("mortal.neutral"), "mortal");
		t.add(ModLang.address("mortal.devilish"), "mortal worm");
		t.add(ModLang.address("mortal.ghost"), "warm one");
		t.add(ModLang.address("titled_junior.righteous"), "young %s");
		t.add(ModLang.address("titled_junior.neutral"), "%s");
		t.add(ModLang.address("titled_junior.devilish"), "little %s");
		t.add(ModLang.address("titled_junior.ghost"), "young %s");
		// The Laws (Laws, phase 3)
		t.add(ModLang.lawKey("metal"), "Law of Metal");
		t.add(ModLang.lawKey("wood"), "Law of Wood");
		t.add(ModLang.lawKey("water"), "Law of Water");
		t.add(ModLang.lawKey("fire"), "Law of Fire");
		t.add(ModLang.lawKey("earth"), "Law of Earth");
		t.add(ModLang.lawKey("space"), "Law of Space");
		t.add(ModLang.lawKey("time"), "Law of Time");
		t.add(ModLang.lawKey("life_and_death"), "Law of Life and Death");
		for (String element : new String[]{"metal", "wood", "water", "fire", "earth"}) {
			String name = Character.toUpperCase(element.charAt(0)) + element.substring(1);
			t.add(ModLang.lawKey(element) + ".gains", "Each level: your " + name + " spells strike 10% harder and reach 5% farther.");
			t.add(ModLang.lawKey(element) + ".now", "Now: your " + name + " spells +%s%% harm, +%s%% reach.");
		}
		t.add(ModLang.lawKey("space") + ".gains", "Each level: Void Shattering's spells strike 10% harder and reach 5% farther, as do Nine Nether Step and Soul Dash; the Spatial Tear is ready 3% sooner.");
		t.add(ModLang.lawKey("space") + ".now", "Now: Void Shattering's spells and the steps +%s%% harm, +%s%% reach; the Spatial Tear %s%% sooner.");
		t.add(ModLang.lawKey("time") + ".gains", "Each level: every spell's cooldown 3% shorter.");
		t.add(ModLang.lawKey("time") + ".now", "Now: every spell's cooldown %s%% shorter.");
		t.add(ModLang.lawKey("life_and_death") + ".gains", "Each level: the ghost path's spells strike 10% harder, and all healing you take is 10% greater.");
		t.add(ModLang.lawKey("life_and_death") + ".now", "Now: the ghost path's spells +%s%% harm; your healing +%s%%.");
		t.add(ModLang.LAW_GATE, "Breaking into %s wants %s levels of the Laws comprehended (you hold %s). Meditate on a Law in the Immortal Realm: the Methods tab's Laws page.");
		t.add(ModLang.MSG_LAW_LEVEL, "You comprehend the %s to its level %s of %s.");
		t.add(ModLang.MSG_LAW_WAY_OPEN, "The Laws you hold open the way into %s.");
		t.add(ModLang.MSG_LAW_GLIMPSE, "In the blow that fells it, a glimpse of the %s.");
		t.add(ModLang.MSG_LAW_FOCUSED, "Your meditation in the Immortal Realm turns to the %s.");
		t.add(ModLang.MSG_LAW_UNFOCUSED, "No Law in your meditation's focus.");
		t.add(ModLang.MSG_LAW_GREATER_SHUT, "The %s opens to comprehension only at Golden Immortal.");
		t.add(ModLang.MSG_LAW_COMPLETE, "You comprehend the %s whole already.");
		t.add(ModLang.MSG_LAW_NOT_IMMORTAL, "The %s is comprehended only by a True Immortal and those past it.");
		t.add(ModLang.METHODS_PAGE_LAWS, "Laws");
		t.add(ModLang.LAWS_TITLE, "The Laws");
		t.add(ModLang.LAWS_SEALED, "The Laws are comprehended only by a True Immortal and those past it: past True Immortal, no method carries a cultivator on, but the Laws do.");
		t.add(ModLang.LAWS_TOTAL, "Laws comprehended: %s levels");
		t.add(ModLang.LAWS_NEXT, "The way into %s wants %s");
		t.add(ModLang.LAWS_NEXT_OPEN, "The way into %s is open (it wants %s)");
		t.add(ModLang.LAWS_FOCUS, "Meditating here comprehends the %s");
		t.add(ModLang.LAWS_FOCUS_AWAY, "The %s in focus: only meditation in the Immortal Realm (or a small world) comprehends it");
		t.add(ModLang.LAWS_NO_FOCUS, "No Law in focus: choose one below for your meditation in the Immortal Realm (or a small world)");
		t.add(ModLang.LAWS_CHOICES, "%s levels yours to choose, for the Five Decays weathered: +1 on a Law");
		t.add(ModLang.LAWS_CHOICE_ONE, "A level yours to choose, for the Five Decays weathered: +1 on a Law");
		t.add(ModLang.LAWS_LEVEL, "Level %s / %s");
		t.add(ModLang.LAWS_INSIGHT, "Insight toward level %s: %s%%");
		t.add(ModLang.LAWS_COMPLETE, "Comprehended whole");
		t.add(ModLang.LAWS_GREATER_SHUT, "Opens to comprehension at Golden Immortal");
		t.add(ModLang.LAWS_FOCUSED, "In focus");
		t.add(ModLang.BTN_LAW_FOCUS, "Focus");
		t.add(ModLang.BTN_LAW_CHOOSE, "+1");
		// The Five Decays (FiveDecays, phase 3)
		t.add(ModLang.MSG_DECAY_WARNING, "A shadow falls on your immortality: the Five Decays of Heaven and Man come for you tomorrow.");
		t.add(ModLang.MSG_DECAY_SIGN[0], "The first of the Five Decays: your robes stain, and your body's armour wanes.");
		t.add(ModLang.MSG_DECAY_SIGN[1], "The second: the flowers in your hair wither. Nothing mends you now.");
		t.add(ModLang.MSG_DECAY_SIGN[2], "The third: your qi falters, gathering at half, and your immortal essence drains away.");
		t.add(ModLang.MSG_DECAY_SIGN[3], "The fourth: your body sweats, and your blows and spells weaken.");
		t.add(ModLang.MSG_DECAY_SIGN[4], "The fifth: your mind can find no rest. In a minute the heavens judge you.");
		t.add(ModLang.MSG_DECAY_JUDGEMENT, "The heavens' judgement falls: %s strikes in thirty seconds, and nothing mends you. Weather it!");
		t.add(ModLang.MSG_DECAY_JUDGEMENT_BROADCAST, "%s faces the judgement of the Five Decays.");
		t.add(ModLang.MSG_DECAY_STRIKES, "Judgement of the Five Decays: %s strikes to come");
		t.add(ModLang.MSG_DECAY_WAITS, "Where the heavens can't see you, the Five Decays wait.");
		t.add(ModLang.MSG_DECAY_WEATHERED, "You have weathered the Five Decays. A level of the Laws is yours to choose: the Methods tab's Laws page.");
		t.add(ModLang.MSG_DECAY_WEATHERED_BROADCAST, "%s has weathered the Five Decays of Heaven and Man.");
		t.add(ModLang.MSG_DECAY_FAILED, "The judgement of the Five Decays strikes you down, and you live on, fallen to %s.");
		t.add(ModLang.MSG_DECAY_FAILED_BROADCAST, "%s fell to the judgement of the Five Decays.");
		t.add(ModLang.MSG_DECAY_RESTLESS, "Under the Five Decays your mind can find no rest: you cannot meditate.");
		t.add(ModLang.LAWS_DECAY_IN, "The Five Decays come for you in %s days");
		t.add(ModLang.LAWS_DECAY_SOON, "The Five Decays come for you within a day");
		t.add(ModLang.LAWS_DECAY_SIGN, "The Five Decays are upon you: sign %s of 5");
		t.add(ModLang.LAWS_DECAY_JUDGEMENT, "The judgement of the Five Decays is upon you");
		t.add(ModLang.MSG_LIFE_YEARS_DRAWN, "You drew %s years of life out of %s (%s of them are yours).");
		t.add(ModLang.MSG_LIFE_YEARS_LOST, "%s drew %s years of your life away.");
		t.add(ModLang.MSG_LIFE_YEARS_GIVEN, "You gave it %s years of your life: %s years of growth.");
		t.add(ModLang.MSG_LIFE_YEARS_TOO_FEW, "Your years are too few to give any away.");
		t.add(ModLang.AXIS_PREFIX + "longevity", "Axis of Longevity");
		t.add(ModLang.AXIS_PREFIX + "fortune", "Axis of Fortune");
		t.add(ModLang.AXIS_PREFIX + "merit", "Axis of Merit");
		t.add(ModLang.AXIS_PREFIX + "enlightenment", "Axis of Enlightenment");
		t.add(ModLang.AXIS_UNIT_PREFIX + "longevity", "years");
		t.add(ModLang.AXIS_UNIT_PREFIX + "fortune", "stones' worth");
		t.add(ModLang.AXIS_UNIT_PREFIX + "merit", "hearts");
		t.add(ModLang.AXIS_UNIT_PREFIX + "enlightenment", "stages");
		t.add(ModLang.AXIS_HOW_PREFIX + "longevity", "Heal the sick in the settlements of the Astral Realm's worlds and take years of life from them, "
				+ "%s in all. Each year taken goes onto your own lifespan too.");
		t.add(ModLang.AXIS_HOW_PREFIX + "fortune", "Bring the settlements of the Astral Realm's worlds what they lack, and absorb the spirit stones "
				+ "they share with you in turn: %s Low-grade Spirit Stones' worth in all.");
		t.add(ModLang.AXIS_HOW_PREFIX + "merit", "Rid the settlements of the Astral Realm's worlds of what preys on them: each of it slain by your "
				+ "own hand counts its hearts, %s in all.");
		t.add(ModLang.AXIS_HOW_PREFIX + "enlightenment", "Guide the lesser cultivators in the settlements of the Astral Realm's worlds through "
				+ "their walls, beside them or from within their bodies: %s stages in all.");
		t.add(ModLang.AXIS_GATE, "Your %s stands at %s / %s %s: complete it before you break through.");
		t.add(ModLang.AXIS_BAR, "%s: %s / %s %s");
		t.add(ModLang.AXIS_BAR_SHORT, "%s / %s %s");
		t.add(ModLang.MSG_AXIS_PROGRESS, "+%s toward your %s (%s / %s %s)");
		t.add(ModLang.MSG_AXIS_COMPLETE, "Your %s stands complete.");
		t.add("cultivation.defying-the-heavens.talk.topic.wish", "Their wish");
		t.add(ModLang.WISH_NOT_FOUR_AXIS, "(Only one of Four Axis could grant it.)");
		t.add(ModLang.WISH_AXES_DONE, "(Your four axes stand complete: there is no more virtue for you in it.)");
		t.add(ModLang.WISH_AXIS_DONE, "(Your axis stands complete: break through before you seek the next.)");
		t.add(ModLang.WISH_OTHER_AXIS, "(It would serve the %s, not the axis you stand at.)");
		t.add(ModLang.WISH_NONE, "We want for nothing now, thanks to you. Perhaps some other day.");
		t.add(ModLang.WISH_SAY + "longevity.1", "My mother lies sick, and fades more each day. No herb of ours can touch it.");
		t.add(ModLang.WISH_SAY + "longevity.2", "My brother took a fever three nights ago and has not woken since.");
		t.add(ModLang.WISH_SAY + "longevity.3", "Our elder is dying. Without her we would not know how to live on this world.");
		t.add(ModLang.WISH_SAY + "fortune.1", "Our stores are bare and the season turns against us. If only we had %s %s.");
		t.add(ModLang.WISH_SAY + "fortune.2", "We came here with too little. %s %s would see us through.");
		t.add(ModLang.WISH_SAY + "fortune.3", "The traders stopped coming. We need %s %s, and no one here can spare them.");
		t.add(ModLang.WISH_SAY + "merit.beast", "A great beast comes out of the wilds at night and tramples everything in its way. We cannot stand against it.");
		t.add(ModLang.WISH_SAY + "merit.rider", "Raiders ride a great beast down on us and take whatever they like. We cannot stand against them.");
		t.add(ModLang.WISH_SAY + "merit.sorcerer", "A sorcerer haunts the land past our fields. Two of ours went out and did not come back.");
		t.add(ModLang.WISH_SAY + "merit.illusionist", "Something that wears many faces haunts the woods. Those who follow it come back blind, or not at all.");
		t.add(ModLang.WISH_SAY + "merit.raiders", "Raiders come every few nights and take what we have grown. Next time they will take us.");
		t.add(ModLang.WISH_SAY + "merit.coven", "Witches have made their home nearby. Our children sicken, and our well tastes of their brews.");
		t.add(ModLang.WISH_SAY + "merit.undead", "The dead walk out of the old barrows, and they no longer rest even by day.");
		t.add(ModLang.WISH_SAY + "merit.horsemen", "Riders of bone come out of the storms. Whoever they pass falls down dead.");
		t.add(ModLang.WISH_SAY + "merit.brood", "A brood of great spiders nests in the hills. They take whatever strays from the village.");
		t.add(ModLang.WISH_SAY + "merit.pack", "A wolf pack has learned how easy our flocks are. Soon it will learn how easy we are.");
		t.add(ModLang.WISH_SAY + "merit.devilish", "A cultivator of the devilish path has made us a hunting ground. Each moon one of us is taken for a cauldron.");
		t.add(ModLang.WISH_SAY + "enlightenment.1", "I have sat at the same wall for years. I can feel the way through and cannot take it.");
		t.add(ModLang.WISH_SAY + "enlightenment.2", "My cultivation is stuck. If someone far above me would sit with me only once...");
		t.add(ModLang.WISH_SAY + "enlightenment.3", "There is no master on this world. I have gone as far as I can alone.");
		t.add(ModLang.WISH_HEALED, "They breathe easy, colour in their face again. They owe you their life: %s years they could give you, if you would take them.");
		t.add(ModLang.WISH_THREAT_OUT, "It is out there now, and it has your scent. Kill it and we are free of it.");
		t.add(ModLang.WISH_GUIDE_SIT, "Sit with me in meditation, close by. (%s seconds more)");
		t.add(ModLang.WISH_TAKE_NONE, "Take nothing.");
		t.add(ModLang.WISH_TAKE_FEW, "Take a few years (%s).");
		t.add(ModLang.WISH_TAKE_SHARE, "Take a share (%s years).");
		t.add(ModLang.WISH_TAKE_ALL, "Take all they can spare (%s years).");
		t.add(ModLang.WISH_GRANT_HEAL, "Heal them with your qi.");
		t.add(ModLang.WISH_GRANT_GIVE, "Give %s %s.");
		t.add(ModLang.WISH_GRANT_THREAT, "I'll deal with it.");
		t.add(ModLang.WISH_GRANT_GUIDE, "I'll guide you.");
		t.add(ModLang.WISH_HEAL_QI, "(Healing them takes %s%% of your qi, and you haven't that much now.)");
		t.add(ModLang.WISH_TOOK_NONE, "You ask for nothing? We will never forget you.");
		t.add(ModLang.WISH_TOOK, "It is fair, for a life. %s years are yours.");
		t.add(ModLang.WISH_GIVE_LACK, "(You haven't %s %s on you.)");
		t.add(ModLang.WISH_GIVE_OWED, "You already carry enough of others' fortune to complete your axis. Take it into yourself first; "
				+ "we would not have you carry what you don't need.");
		t.add(ModLang.WISH_GIVEN, "You've saved us. Take these, it's little enough: %s Mid-grade Spirit Stones, a share of all we have. "
				+ "(Absorb them to fill your Axis of Fortune.)");
		t.add(ModLang.WISH_THREAT_SLAIN, "The threat to %s's people is slain.");
		t.add(ModLang.WISH_GUIDED, "Guided by you, %s breaks through to %s.");
		t.add(ModLang.WISH_NOT_FOUR_AXIS_DEAD, "(Only one of Four Axis could grant it: of the dead, one whose ghost path is.)");
		t.add(ModLang.WISH_THREAT_OUT_MANY, "They are out there now, %s of them, and they have your scent. Kill them all and we are free of them.");
		t.add(ModLang.WISH_THREAT_LOST, "What preyed on %s's people is gone, but not by your hand: their wish still stands.");
		t.add(ModLang.WISH_GUIDED_WITHIN, "From within, you carry %s through its wall to %s.");
		t.add(ModLang.MSG_SETTLEMENT_NEAR, "A settlement of the lower realms' people: %s");
		t.add(ModLang.OBSESSION_PREFIX + "longevity", "Longevity");
		t.add(ModLang.OBSESSION_PREFIX + "fortune", "Fortune");
		t.add(ModLang.OBSESSION_PREFIX + "merit", "Merit");
		t.add(ModLang.OBSESSION_PREFIX + "enlightenment", "Enlightenment");
		t.add(ModLang.OBSESSION_BOON_PREFIX + "longevity", "Lifespan x1.2");
		t.add(ModLang.OBSESSION_BOON_PREFIX + "fortune", "You pay x0.8 in trade");
		t.add(ModLang.OBSESSION_BOON_PREFIX + "merit", "+10 regard with everyone");
		t.add(ModLang.OBSESSION_BOON_PREFIX + "enlightenment", "Cultivation x1.2");
		t.add(ModLang.OBSESSION_TITLE, "Name Your Obsession");
		t.add(ModLang.OBSESSION_INTRO, "Before the tribulation, name the one thing your heart will not let go of. Your road will bend toward it, and it can never be named again.");
		t.add(ModLang.OBSESSION_AXIS, "Its axis fills twice as fast");
		t.add(ModLang.OBSESSION_CHOOSE, "Choose one, then face the tribulation.");
		t.add(ModLang.OBSESSION_CHOSEN, "Your obsession: %s");
		t.add(ModLang.OBSESSION_CONFIRM, "Face the Tribulation");
		t.add(ModLang.MSG_OBSESSION_NAMED, "You name your obsession: %s (%s). Your road bends toward it, for good.");
		t.add(ModLang.STATS_OBSESSION, "Obsession: %s");
		t.add(ModLang.STATS_OBSESSION_BOON, "Obsession: %s (%s)");
		t.add(ModLang.CMD_OBSESSION, "%s's obsession: %s (%s)");
		t.add(ModLang.CMD_OBSESSION_NONE, "%s has named no obsession yet");
		// Bloodlines: the races, their colourings and lore; the picker; what one is told; the command.
		t.add(ModLang.bloodlineKey("human"), "Human");
		t.add(ModLang.bloodlineKey("tiger"), "Tiger");
		t.add(ModLang.bloodlineKey("lizard"), "Lizard");
		t.add(ModLang.bloodlineKey("fox"), "Fox");
		t.add(ModLang.bloodlineKey("dragon"), "Dragon");
		t.add(ModLang.bloodlineVariantKey("tiger", "golden"), "Golden Tiger");
		t.add(ModLang.bloodlineVariantKey("tiger", "white"), "White Tiger");
		t.add(ModLang.bloodlineVariantKey("tiger", "black"), "Black Tiger");
		t.add(ModLang.bloodlineVariantKey("lizard", "jade"), "Jade Lizard");
		t.add(ModLang.bloodlineVariantKey("lizard", "black"), "Black Lizard");
		t.add(ModLang.bloodlineVariantKey("lizard", "crimson"), "Crimson Lizard");
		t.add(ModLang.bloodlineVariantKey("fox", "red"), "Red Fox");
		t.add(ModLang.bloodlineVariantKey("fox", "snow"), "Snow Fox");
		t.add(ModLang.bloodlineVariantKey("fox", "silver"), "Silver Fox");
		t.add(ModLang.bloodlineVariantKey("dragon", "azure"), "Azure Dragon");
		t.add(ModLang.bloodlineVariantKey("dragon", "gold"), "Golden Dragon");
		t.add(ModLang.bloodlineVariantKey("dragon", "crimson"), "Crimson Dragon");
		t.add(ModLang.bloodlineLoreKey("human"), "Born of men, with your own face. The heavens' path is yours by birth; a beast's blood may yet be awakened in you.");
		t.add(ModLang.bloodlineLoreKey("tiger"), "Kin of the White Tiger of the west. Striped and fierce, born with a demon core, its blows heavy and its eyes keen in the dark.");
		t.add(ModLang.bloodlineLoreKey("lizard"), "Of the great lizards that bask on sun-hot stones and hunt the marshes. Cold-blooded and patient, born with a demon core, quick as a whip.");
		t.add(ModLang.bloodlineLoreKey("fox"), "A fox spirit, born with a demon core and one tail. Another grows with every realm, until nine tails fan out behind it.");
		t.add(ModLang.bloodlineLoreKey("dragon"), "Of the true dragons, rarest of the demon races. Born with a demon core, horned and whiskered, slow to grow and mighty when it has.");
		t.add(ModLang.BLOODLINE_TITLE, "Choose Your Bloodline");
		t.add(ModLang.BLOODLINE_INTRO, "Whose blood runs in you? A demon beast wears its race's whole form; a human keeps their own.");
		t.add(ModLang.BLOODLINE_COLOURING, "%s");
		t.add(ModLang.BLOODLINE_OWN_SKIN, "Your own skin");
		t.add(ModLang.BLOODLINE_CORE, "Born with a demon core");
		t.add(ModLang.BLOODLINE_CONFIRM, "Begin");
		t.add(ModLang.BLOODLINE_FINAL, "Chosen once, for good.");
		t.add(ModLang.MSG_BLOODLINE_BEAST, "You are of the %s's blood: a demon of the %s race, born with a demon core.");
		t.add(ModLang.MSG_BLOODLINE_HUMAN, "You are human, born of men. A beast's blood may yet be awakened in you.");
		t.add(ModLang.JOURNAL_BLOODLINE, "Your Bloodline");
		t.add(ModLang.CMD_BLOODLINE, "%s is of the %s race: %s");
		t.add(ModLang.CMD_BLOODLINE_HUMAN, "%s is human");
		t.add(ModLang.CMD_BLOODLINE_NONE, "%s has chosen no bloodline yet");
		t.add(ModLang.CMD_BLOODLINE_ASKED, "%s is asked to choose a bloodline again");
		// The Demon Path.
		t.add(ModLang.MSG_DEMON_CORE, "A demon core burns in you, as in every %s born: your Demon Path begins at Qi Refining Early. Your blood's inheritance is %s.");
		t.add(ModLang.JOURNAL_DEMON_PATH, "The Demon Path");
		t.add(ModLang.JOURNAL_DEMON_PATH_TEXT, "Born of the %s race with a demon core, you walk the Demon Path beside the heavens' own: the same realms, its own bar. No method, pill or ritual carries it; your blood does, and as it climbs it teaches you your inheritance's spells, %s. Its body is the mightier and its life the longer, but the heavens strike it the harder. Switch the Cultivation tab's Path to Demon to cultivate it.");
		t.add(ModLang.MSG_DEMON_STAGE_UP, "Demon Path: %s");
		t.add(ModLang.MSG_DEMON_INHERITS, "Your blood remembers: you have inherited %s.");
		t.add(ModLang.MSG_DEMON_BOTTLENECK, "Your Demon Path is at its bottleneck: break through in the cultivation menu to face the heavens' tribulation.");
		t.add(ModLang.MSG_DEMON_TRIB_START, "The heavens see a demon rise to %s, and hate it! Survive %s strikes!");
		t.add(ModLang.MSG_DEMON_TRIB_START_SINGLE, "The heavens see a demon rise to %s, and hate it! Survive the strike!");
		t.add(ModLang.MSG_DEMON_BREAKTHROUGH, "The heavens' hatred survived: your Demon Path stands at %s!");
		t.add(ModLang.MSG_DEMON_TRIB_FALL, "Backlash! Your Demon Path falls to %s.");
		t.add(ModLang.MSG_DEMON_TRIB_FLED, "You fled the heavens' tribulation. Backlash! Your Demon Path falls to %s.");
		t.add(ModLang.MSG_PATH_DEMON, "You draw on your demon core: meditating cultivates your Demon Path.");
		t.add(ModLang.BTN_PATH_DEMON, "Path: Demon");
		t.add(ModLang.METHODS_PAGE_DEMON, "Demon Path");
		t.add(ModLang.DEMON_COLUMN_PACE, "%s blood: +%s/s");
		t.add(ModLang.DEMON_COLUMN_PACE_SHORT, "+%s/s");
		t.add(ModLang.DEMON_BOTTLENECK, "Your Demon Path's bottleneck: break through to %s (the heavens' tribulation, the harder).");
		t.add(ModLang.DEMON_BOTTLENECK_SWITCH, "Your Demon Path's bottleneck: draw on your demon core (Path: Demon) to break through to %s.");
		t.add(ModLang.DEMON_PINNACLE, "Your Demon Path stands at its very pinnacle.");
		t.add(ModLang.STATS_DEMON_BODY, "Demon Path: %s");
		// The races' gifts.
		t.add(ModLang.MSG_SHED_SKIN, "You shed your skin and rise again, whole enough to fight on. (Again in %s minutes.)");
		t.add(ModLang.MSG_GIFT_COOLDOWN, "Not yet: %s s.");
		t.add(ModLang.MSG_GIFT_NO_QI, "Not enough qi (%s).");
		t.add(ModLang.MSG_ROARED, "You roar! %s cowed.");
		t.add(ModLang.MSG_ILLUSION, "You fade into an illusion for %s s.");
		t.add(ModLang.MSG_ILLUSION_BROKEN, "Your strike breaks the illusion.");
		t.add(ModLang.MSG_SETTLEMENT_SMOKE, "Smoke rises from a settlement some %s blocks to the %s.");
		t.add(ModLang.MSG_SETTLER_ARRIVES, "A newcomer has come to %s.");
		t.add(ModLang.MSG_STAT_POINTS_RETURNED, "Your cultivation has fallen: the %s stat points its lost stages gave are gone with them, the last you put in. "
				+ "The rest stay where you put them; climb back and they are yours to spend again.");
		t.add(ModLang.BTN_STAT_POINTS, "Stat Points");
		t.add(ModLang.BTN_STAT_LIST, "Stats");
		t.add(ModLang.STAT_POINTS_UNSPENT, "Points to spend: %s");
		t.add(ModLang.STAT_POINTS_EARNED, "%s earned: %s a stage, %s a realm");
		t.add(ModLang.STAT_POINTS_HINT, "+ spends a point, shift-click five");
		t.add(ModLang.STAT_POINTS_MORTAL, "Points come with your first breakthrough.");
		t.add("cultivation.defying-the-heavens.talk.topic.join", "May I join your sect?");
		t.add("cultivation.defying-the-heavens.talk.topic.trial", "I am ready for the entrance trial.");
		t.add("cultivation.defying-the-heavens.talk.topic.leave_sect", "I wish to leave the sect.");
		t.add(ModLang.talkLine("join.disciple"), "I am only a disciple. Speak to one of our elders: they decide who joins us. "
				+ "Go in, our barrier will let you pass.");
		t.add(ModLang.talkLine("join.ghost"), "The dead have no place in a sect of the living.");
		t.add(ModLang.talkLine("join.possessed"), "There is another soul behind those eyes. Come to us in your own body, if you would join us.");
		t.add(ModLang.talkLine("join.mortal"), "You have not even begun to cultivate. Open your meridians first, then come to us.");
		t.add(ModLang.talkLine("join.other_sect"), "You are already a disciple of the %s. A cultivator serves one sect.");
		t.add(ModLang.talkLine("join.barred"), "We will not take you back yet. Come again in %s days, if you still wish it.");
		t.add(ModLang.talkLine("join.hated"), "Our sect has not forgotten what you have done to us. Leave.");
		t.add(ModLang.talkLine("join.too_strong"), "One of your strength would be no disciple of ours. We cannot take you in.");
		t.add(ModLang.talkLine("join.tried"), "You have been tested today already. Train, and come back tomorrow.");
		t.add(ModLang.talkLine("join.trial"), "Then show me what you have learned: an entrance trial, against me. I will hold back to your realm. "
				+ "Win, or stand your ground until the time runs out, and you are one of us. Say when you are ready.");
		t.add(ModLang.talkLine("join.gifted"), "A body like yours needs no testing. Welcome to the %s: from today you are %s.");
		t.add(ModLang.talkLine("join.friend"), "You have long been a friend to us. Welcome to the %s: from today you are %s.");
		t.add(ModLang.talkLine("join.kin"), "Our own blood runs in you: there is nothing to test. Welcome to the %s: from today you are %s.");
		t.add(ModLang.talkLine("join.human"), "You smell of men. A clan of our blood takes no one without a beast's blood in them. Go.");
		t.add(ModLang.talkLine("join.passed"), "Well fought. From today you are one of us. Our gates are open to you.");
		t.add(ModLang.talkLine("join.failed"), "Not yet. Train, and come back tomorrow.");
		t.add(ModLang.talkLine("leave.disciple"), "Only an elder can release you from the sect.");
		t.add(ModLang.talkLine("leave.done"), "So be it. You are no longer one of us. Go well.");
		// The Celestial Court (CelestialCourt): a Heavenly Venerable's topic and answers, its people's own words of themselves, its merit.
		t.add("cultivation.defying-the-heavens.talk.topic.court", "I would serve the Celestial Court.");
		t.add(ModLang.talkLine("court.join"), "%s writes your name into the Court's rolls. \"From today you are an Officer of the Celestial Court. "
				+ "Keep watch on the Southern Heavenly Gate's plaza while the Gate stands open, and hunt down the devilish immortals of this realm. "
				+ "The Court pays its officers in merit; its Quartermaster, on the palace terrace, takes it.\"");
		t.add(ModLang.talkLine("court.member"), "\"You hold the rank of %s in the Celestial Court, with %s merit to your name. Earn %s more "
				+ "and you will be made a %s.\"");
		t.add(ModLang.talkLine("court.member.top"), "\"You hold the rank of %s in the Celestial Court, with %s merit to your name. The Court "
				+ "has no higher rank to give you.\"");
		t.add(ModLang.talkLine("court.too_weak"), "\"The Court takes no one who could not climb the Gate.\"");
		t.add(ModLang.talkLine("self.court"), "I am %1$s, %2$s of the Celestial Court: a %3$s.");
		t.add(ModLang.talkLine("self.court.venerable"), "I am %1$s, the Heavenly Venerable of %2$s of the Celestial Court: a %3$s.");
		t.add(ModLang.TALK_NEEDS_COURT, "(needs the rank of Court %s)");
		t.add(ModLang.TRADE_LOCKED_RANK, "Locked: for a Court %s or higher (you: %s)");
		t.add(ModLang.TRADE_NOT_OF_COURT, "not of the Court");
		t.add(ModLang.courtRank("officer"), "Officer");
		t.add(ModLang.courtRank("captain"), "Captain");
		t.add(ModLang.courtRank("general"), "General");
		t.add(ModLang.MSG_COURT_MERIT_WATCH, "+1 merit: a minute's watch at the Southern Heavenly Gate (%s in all)");
		t.add(ModLang.MSG_COURT_MERIT_SLAIN, "+%2$s merit: %1$s, a devilish immortal, slain (%3$s in all)");
		t.add(ModLang.MSG_COURT_RANK_UP, "The Celestial Court raises you: you are one of its %ss now.");
		t.add("item.defying-the-heavens.court_merit_token", "Court Merit Token");
		t.add(ModLang.TOKEN_EARNED, "The Celestial Court's pay for its officers: a minute's watch at the open Southern Heavenly Gate, a devilish immortal slain, a primordial beast fought");
		t.add(ModLang.TOKEN_SPENT, "Spent with the Court's Quartermaster");
		t.add(ModLang.SECT_TAB_COURT, "Celestial Court: %s, %s merit");
		t.add(ModLang.SECT_TAB_COURT_NEXT, " (%s at %s)");
		// The primordial beasts (PrimordialBeast, PrimordialBeasts): their coming, the Kun become the Peng, the Phoenix's rebirth, one slain, the
		// Court's merit for one, and what a Heavenly Venerable says of them.
		t.add(ModLang.primordialComes("kun_peng"), "Far out over the sea of clouds something vast moves beneath them: the Kun has come.");
		t.add(ModLang.primordialComes("true_phoenix"), "Fire crowns a far peak: the True Phoenix has come to its roost.");
		t.add(ModLang.primordialComes("true_dragon"), "Thunder rolls across the sky: the True Dragon rides its storm here.");
		t.add(ModLang.primordialComes("primordial_chaos"), "The chaos thickens and takes a shape with no face: the Primordial Chaos stirs.");
		t.add(ModLang.MSG_PRIMORDIAL_BREACH, "The Kun breaks from the sea of clouds and spreads its wings: it is the Peng!");
		t.add(ModLang.MSG_PRIMORDIAL_EMBER, "The True Phoenix burns down to an ember... but its fire is not out.");
		t.add(ModLang.MSG_PRIMORDIAL_REBORN, "The True Phoenix rises again from its own fire!");
		t.add(ModLang.MSG_PRIMORDIAL_SLAIN, "The %s has fallen. Another of its kind will come in five days.");
		t.add(ModLang.MSG_COURT_MERIT_PRIMORDIAL, "+%2$s merit: the %1$s slain (%3$s in all)");
		t.add("cultivation.defying-the-heavens.talk.topic.beasts", "Where are the primordial beasts?");
		t.add(ModLang.talkLine("primordial"), "\"Four beasts older than the Court keep this realm, and none of them bows to it.\"");
		t.add(ModLang.primordialWhere("kun_peng"), "The Kun swims the sea of clouds %s of the Southern Heavenly Gate, some %s blocks out (about %s, %s). Wound it deep and it is the Peng.");
		t.add(ModLang.primordialGone("kun_peng"), "The Kun-Peng was slain; another comes to its waters in %s days.");
		t.add(ModLang.primordialWhere("true_phoenix"), "The True Phoenix roosts on a peak %s of the Gate, some %s blocks out (about %s, %s). Kill it once and it burns, and rises again.");
		t.add(ModLang.primordialGone("true_phoenix"), "The True Phoenix was slain; another comes to its peak in %s days.");
		t.add(ModLang.primordialWhere("true_dragon"), "The True Dragon rides its storm high in the sky %s of the Gate, some %s blocks out (about %s, %s).");
		t.add(ModLang.primordialGone("true_dragon"), "The True Dragon was slain; another rides its storm in %s days.");
		t.add(ModLang.primordialWhere("primordial_chaos"), "Out past the realm's edge, in the Chaos Frontier %s of the Gate, some %s blocks out (about %s, %s), what is left of Hundun, the Primordial Chaos, still churns. It unmakes what it catches.");
		t.add(ModLang.primordialGone("primordial_chaos"), "The Primordial Chaos was scattered; the chaos gathers into it again in %s days.");
		t.add(ModLang.MSG_PALACE_SEALED, "The Ghost King's seal of %s holds it shut. Only one a realm above the king could break it.");
		t.add(ModLang.MSG_PALACE_SEAL_BROKEN, "The Ghost King's seal shatters under your hand. The treasury lies open.");
		t.add(ModLang.BRUSH_TOOLTIP, "Paints seals of qi ink on floors and walls, to join Qi Veins to a Formation Core");
		t.add(ModLang.BRUSH_TOOLTIP_ERASE, "Sneak to wipe a seal away. Only Qi Sense shows the ink.");
		t.add(ModLang.MSG_CORE_CLAIMED, "The formation of the fallen %s answers to you now.");
		t.add(ModLang.CORE_TRUSTED_BUTTON, "Trusted...");
		t.add(ModLang.CORE_TRUSTED_TITLE, "Trusted Cultivators");
		t.add(ModLang.CORE_TRUSTED_HINT, "They pass through the barrier as you do.");
		t.add(ModLang.CORE_TRUSTED_COUNT, "%s of %s");
		t.add(ModLang.CORE_TRUSTED_NONE, "No one yet: only you may pass.");
		t.add(ModLang.CORE_TRUST_ADD, "Trust");
		t.add(ModLang.CORE_TRUST_REMOVE, "Stop trusting %s");
		t.add(ModLang.CORE_BACK, "Back");
		t.add(ModLang.MSG_TRUST_ADDED, "%s may now pass through the barrier.");
		t.add(ModLang.MSG_TRUST_REMOVED, "%s may no longer pass through the barrier.");
		t.add(ModLang.MSG_TRUST_UNKNOWN, "No cultivator called %s has been seen in this world.");
		t.add(ModLang.MSG_TRUST_FULL, "A formation can know at most %s cultivators.");
		t.add(ModLang.MSG_TRUST_SELF, "The formation already answers to you.");

		// Soul Projection
		t.add(ModLang.MSG_SOUL_GAINED, "Your nascent soul has formed: Soul Projection is yours.");
		t.add(ModLang.MSG_SOUL_GATHERING, "Your nascent soul gathers itself. Hold still (%s s)...");
		t.add(ModLang.MSG_SOUL_CANCELLED, "Your soul settles back into your body.");
		t.add(ModLang.MSG_SOUL_MOVED, "You moved: your soul settles back into your body.");
		t.add(ModLang.MSG_SOUL_NOT_HERE, "Your soul can't leave your body here.");
		t.add(ModLang.MSG_SOUL_GROUND, "You need steady, dry ground to send out your soul.");
		t.add(ModLang.MSG_SOUL_LOW_QI, "Not enough qi: your soul needs %s%% of your pool to leave.");
		t.add(ModLang.MSG_SOUL_LOW_GHOST_QI, "Not enough ghost qi: your soul needs %s%% of your ghost qi to leave.");
		t.add(ModLang.MSG_SOUL_NO_GHOST_QI, "Your ghost qi is spent: your soul snaps back into your body.");
		t.add(ModLang.MSG_POSSESS_GHOST_QI, "Not enough ghost qi: taking hold of a body costs %s%% of your ghost qi.");
		t.add(ModLang.MSG_SOUL_OUT, "Your nascent soul leaves your body.");
		t.add(ModLang.MSG_SOUL_RECALLING, "Your soul returns along its tether (%s s)...");
		t.add(ModLang.MSG_SOUL_BACK, "Your soul is back in your body.");
		t.add(ModLang.MSG_SOUL_NO_QI, "Your qi is spent: your soul snaps back into your body.");
		t.add(ModLang.MSG_SOUL_BUSY, "Not while your soul is out of your body.");
		t.add(ModLang.MSG_SOUL_LOST, "Your nascent soul is lost. You fall to %s.");
		t.add(ModLang.MSG_SOUL_DESTROYED, "Your nascent soul was destroyed.");
		t.add(ModLang.MSG_SOUL_SEVERED, "Your soul's tether was cut.");
		t.add(ModLang.MSG_SOUL_BODY_SLAIN, "Your body was slain while your soul was away.");
		t.add(ModLang.MSG_SOUL_BODY_GONE, "Your body is gone: your soul has nothing to return to.");
		t.add(ModLang.MSG_SOUL_FLED, "You fled a fight with your soul out of your body: its tether snapped.");
		t.add(ModLang.MSG_SOUL_BODY_HURT, "Your body is under attack! (%s / %s)");
		t.add(ModLang.MSG_SOUL_TETHER_STRUCK, "%s strikes at your tether!");
		t.add(ModLang.MSG_SOUL_SENSED, "You sense a nascent soul nearby.");
		t.add(ModLang.MSG_SOUL_BARRIER, "A barrier of qi turns your soul back.");
		t.add(ModLang.MSG_POSSESS_START, "You take hold of %s. Sneak to let go.");
		t.add(ModLang.MSG_POSSESS_CANNOT, "Your soul can't take hold of that.");
		t.add(ModLang.MSG_POSSESS_PVP, "You can't seize another player's body here.");
		t.add(ModLang.MSG_POSSESS_TOO_STRONG, "Too strong: you can only possess beings at least a realm below your own.");
		t.add(ModLang.MSG_POSSESS_QI, "Not enough qi: taking hold of a body costs %s%% of your pool.");
		t.add(ModLang.MSG_POSSESS_EJECTED, "The body you held is dead: your soul is thrown out, exposed.");
		t.add(ModLang.MSG_POSSESS_TETHER, "Your tether pulls your soul out of the body.");
		t.add(ModLang.MSG_POSSESS_DEAD, "The dead have no body to take.");
		t.add(ModLang.MSG_POSSESS_INHABIT, "You are in %s's body: everything they carry, their strength and their cultivation are yours to use. "
				+ "Soul Projection (on your ability wheel) leaves it.");
		t.add(ModLang.MSG_POSSESS_TAKEN, "%s's nascent soul has taken your body. You can only watch through its eyes until it lets go.");
		t.add(ModLang.MSG_POSSESS_FREED, "Your body is your own again.");
		t.add(ModLang.MSG_POSSESS_LET_GO, "You leave the body.");
		t.add(ModLang.MSG_POSSESS_BODY_DIED, "The body you were in has died: your soul is thrown out.");
		t.add(ModLang.MSG_POSSESS_OWNER_LEFT, "The body's owner is gone, and their body with them: your soul is thrown out.");
		t.add(ModLang.MSG_POSSESS_LOST, "The body slips from your hold: your soul is thrown out.");
		t.add(ModLang.MSG_POSSESS_OWNER_AWAY, "%s has left the world; their body is still yours.");
		t.add(ModLang.MSG_POSSESS_OWNER_BACK, "%s is back, watching through their body's eyes.");
		t.add(ModLang.MSG_POSSESS_STILL_HELD, "%s's nascent soul still holds your body. You can only watch through its eyes until it lets go.");
		t.add(ModLang.MSG_POSSESS_DIED_AWAY, "Your body died while another's soul held it.");
		t.add(ModLang.MSG_POSSESS_GUIDANCE, "Your soul stands above this body: meditating in it, you cultivate it %s times as fast as it could alone.");
		t.add(ModLang.MSG_POSSESS_MERIDIANS, "Your soul's might forces open %s's meridians: the body can cultivate now.");
		t.add(ModLang.MSG_POSSESS_MERIDIANS_TAKEN, "The soul in your body has forced your meridians open: you can cultivate now.");
		t.add(ModLang.ABILITY_SOUL_WOUNDED, "Your soul is wounded: %s of %s hearts. It mends while it rests in a body (yours, or one it possesses).");
		t.add(ModLang.HUD_SOUL_GATHERING, "Leaving your body... %s s");
		t.add(ModLang.HUD_SOUL_RECALLING, "Returning to your body... %s s");
		t.add(ModLang.HUD_SOUL_TETHER, "Tether %s / %s");
		t.add(ModLang.HUD_SOUL_BODY, "Body %s / %s");
		t.add(ModLang.HUD_SOUL_POSSESSING, "In %s (sneak to let go)");
		t.add(ModLang.HUD_SOUL_INSIDE, "In %s's body (Soul Projection leaves it)");
		t.add(ModLang.HUD_SOUL_OWN_QI_LABEL, "Own Qi");
		t.add(ModLang.HUD_ESSENCE_LABEL, "Essence");
		t.add(ModLang.HUD_SOUL_OWN_GHOST_QI_LABEL, "Own Ghost Qi");
		t.add(ModLang.HUD_SOUL_TETHER_AWAY, "Tether across the realms");
		t.add(ModLang.HUD_SOUL_TETHER_ENDLESS, "Body %s blocks off (the tether has no end)");

		// The Ritual to the Heavens (HeavenRitual, HeavenAltar)
		t.add(ModBlocks.HEAVEN_ALTAR, "Heaven Altar");
		t.add(ModLang.ritualOffering("unworthy"), "Unworthy");
		t.add(ModLang.ritualOffering("proper"), "Proper");
		t.add(ModLang.ritualOffering("generous"), "Generous");
		t.add(ModLang.ritualOffering("lavish"), "Lavish");
		t.add(ModLang.ritualOffering("heavenly"), "Heavenly");
		t.add(ModLang.ritualGrandeur("humble"), "Humble");
		t.add(ModLang.ritualGrandeur("modest"), "Modest");
		t.add(ModLang.ritualGrandeur("grand"), "Grand");
		t.add(ModLang.ritualGrandeur("magnificent"), "Magnificent");
		t.add(ModLang.ALTAR_TOOLTIP, "Where the Ritual to the Heavens is performed: the heavens' permission to cultivate past Qi Refining Mid");
		t.add(ModLang.ALTAR_TOOLTIP_BEGIN, "Right-click: lay offerings. Sneak + right-click, empty-handed: begin");
		t.add(ModLang.ALTAR_TRAY, "Offerings to the Heavens");
		t.add(ModLang.ALTAR_CHANCE, "Chance");
		t.add(ModLang.ALTAR_OFFERING, "Offering");
		t.add(ModLang.ALTAR_WORTH, "Worth %s");
		t.add(ModLang.ALTAR_WORTH_NEXT, "Worth %1$s · %2$s at %3$s");
		t.add(ModLang.ALTAR_ALTAR, "Altar");
		t.add(ModLang.ALTAR_HEIGHT, "Height");
		t.add(ModLang.ALTAR_HEIGHT_Y, "Y %s");
		t.add(ModLang.ALTAR_REWARD, "Reward");
		t.add(ModLang.ALTAR_REWARD_LEAVE, "Permission to cultivate");
		t.add(ModLang.ALTAR_REWARD_FAVOUR, "+ Heaven's Favour %1$s (+%2$s%%)");
		t.add(ModLang.ALTAR_REWARD_HAVE, "Favour %s already yours");
		t.add(ModLang.altarPart("platform"), "Platform");
		t.add(ModLang.altarPart("terraces"), "Levels");
		t.add(ModLang.altarPart("noble"), "Noble stone");
		t.add(ModLang.altarPart("pillars"), "Pillars");
		t.add(ModLang.altarPart("lights"), "Lights");
		t.add(ModLang.altarPart("treasures"), "Treasures");
		t.add(ModLang.altarState("ready"), "Sneak + right-click the altar, empty-handed, to begin.");
		t.add(ModLang.altarState("granted"), "The heavens have already permitted you to cultivate.");
		t.add(ModLang.altarState("not_yet"), "The ritual waits for Qi Refining Mid.");
		t.add(ModLang.altarState("dawn"), "The heavens will not hear you again until dawn.");
		t.add(ModLang.altarState("busy"), "Not now: finish what you are doing first.");
		t.add(ModLang.altarState("no_sky"), "The heavens can't see this altar: it needs open sky.");
		t.add(ModLang.ALTAR_NO_SKY, "The heavens cannot see this altar: it needs open sky above it.");
		t.add(ModLang.FAVOUR_LINE, "Heaven's Favour: +%s%%");
		t.add(ModLang.RITUAL_AHEAD, "Before Qi Refining Late: the Ritual to the Heavens.");
		t.add(ModLang.RITUAL_GATE, "The heavens must grant you leave to go on.");
		t.add(ModLang.RITUAL_HOW, "At a Heaven Altar under open sky, lay offerings, then sneak, right-click it empty-handed and kneel there, still, for 15 seconds.");
		t.add(ModLang.MSG_RITUAL_GATE, "Qi Refining Mid is full. Perform the Ritual to the Heavens at a Heaven Altar to go further.");
		t.add(ModLang.MSG_RITUAL_BEGIN, "The ritual begins (%1$s, %2$s chance). Kneel still.");
		t.add(ModLang.MSG_RITUAL_NOT_NOW, "The heavens have nothing to grant you now.");
		t.add(ModLang.MSG_RITUAL_DAWN, "The heavens will not hear you again until dawn.");
		t.add(ModLang.MSG_RITUAL_ALTAR_BUSY, "Someone is already praying at this altar.");
		t.add(ModLang.MSG_RITUAL_BUSY, "Not now: finish what you are doing first.");
		t.add(ModLang.MSG_RITUAL_MOVED, "You moved: the ritual is broken, the offering lost.");
		t.add(ModLang.MSG_RITUAL_BROKEN, "The ritual is broken, the offering lost.");
		t.add(ModLang.MSG_RITUAL_SUCCESS, "The heavens permit you to cultivate past Qi Refining Mid.");
		t.add(ModLang.MSG_RITUAL_FAVOUR, "Heaven's Favour: +%s%% cultivation");
		t.add(ModLang.MSG_RITUAL_FAIL, "The heavens are silent. Ask again after dawn.");
		t.add(ModLang.RITUAL_TITLE_SUCCESS, "The Heavens Grant You Permission");
		t.add(ModLang.RITUAL_TITLE_FAIL, "The Heavens Are Silent");
		t.add(ModLang.HUD_RITUAL, "Kneeling before the heavens... %s:%s");

		// What cultivators say of their own accord (NpcLife, NpcSpeech): every line and its variants, kept in NpcLines.
		NpcLines.english().forEach((key, text) -> t.add(key, text));

		// The mod's advancements (ModAdvancements): each one's title and description.
		ModAdvancements.english().forEach((key, text) -> t.add(key, text));
	}

	/** A physique's name and description (Physique). */
	private static void physique(TranslationBuilder t, String id, String name, String description) {
		t.add(ModLang.physiqueKey(id), name);
		t.add(ModLang.physiqueKey(id) + ".description", description);
	}

	/** A realm's English name (its lang entry, and the texts that name it). */
	static String realmName(Realm realm) {
		return switch (realm) {
			case QI_REFINING -> "Qi Refining";
			case FOUNDATION_BUILDING -> "Foundation Building";
			case CORE_FORMATION -> "Core Formation";
			case NASCENT_SOUL -> "Nascent Soul";
			case HEAVENLY_BEING -> "Heavenly Being";
			case FOUR_AXIS -> "Four Axis";
			case DAO_INTEGRATION -> "Dao Integration";
			case VOID_SHATTERING -> "Void Shattering";
			case SACRED_VESSEL -> "Sacred Vessel";
			case ENTERING_NIRVANA -> "Entering Nirvana";
			case TRUE_IMMORTAL -> "True Immortal";
			case EARTH_IMMORTAL -> "Earth Immortal";
			case HEAVEN_IMMORTAL -> "Heaven Immortal";
			case GOLDEN_IMMORTAL -> "Golden Immortal";
			case TAIYI_GOLDEN_IMMORTAL -> "Taiyi Golden Immortal";
			case PROFOUND_SOVEREIGN -> "Profound Sovereign";
			case HEAVENLY_LORD -> "Heavenly Lord";
		};
	}
}
