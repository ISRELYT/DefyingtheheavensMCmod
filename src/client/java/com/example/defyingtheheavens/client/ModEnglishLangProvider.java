package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.ModEntities;
import com.example.defyingtheheavens.ModEffects;
import com.example.defyingtheheavens.ModItems;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.MortalStage;
import com.example.defyingtheheavens.PillGrade;
import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.Stage;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

public class ModEnglishLangProvider extends FabricLanguageProvider {
	public ModEnglishLangProvider(FabricDataOutput output) {
		super(output, "en_us");
	}

	@Override
	public void generateTranslations(TranslationBuilder t) {
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

		// Blocks
		t.add(ModBlocks.SPATIAL_RIFT, "Spatial Rift");
		t.add(ModBlocks.JADE_STONE, "Jade Stone");
		t.add(ModBlocks.JADE_ORE, "Jade Ore");
		t.add(ModBlocks.DEEPSLATE_JADE_ORE, "Deepslate Jade Ore");
		t.add(ModItems.JADE, "Jade");
		t.add(ModBlocks.WHITE_BLOSSOM_LEAVES, "White Blossom Leaves");
		t.add(ModBlocks.BLUE_SPIRIT_LOG, "Blue Spirit Log");
		t.add(ModBlocks.BLUE_SPIRIT_WOOD, "Blue Spirit Wood");
		t.add(ModBlocks.BLUE_SPIRIT_PLANKS, "Blue Spirit Planks");

		// Upper Realm biomes and the Spatial Gap
		t.add(ModLang.biomeKey("dense_qi_peaks"), "Dense Qi Peaks");
		t.add(ModLang.biomeKey("spirit_forest"), "Spirit Forest");
		t.add(ModLang.biomeKey("peach_blossom_sanctuary"), "Peach Blossom Sanctuary");
		t.add(ModLang.biomeKey("thunder_peaks"), "Thunder Peaks");
		t.add(ModLang.biomeKey("ancient_sword_graveyard"), "Ancient Sword Graveyard");
		t.add(ModLang.biomeKey("spatial_gap"), "Spatial Gap");
		t.add(ModLang.DEATH_SPATIAL_PRESSURE, "%1$s was crushed by spatial pressure");
		t.add(ModLang.DEATH_SPATIAL_PRESSURE + ".player", "%1$s was crushed by spatial pressure while fighting %2$s");

		// Spatial Gap, Realm Suppression and portals
		t.add(ModLang.MSG_GAP_ASCEND, "Torn into the Spatial Gap! Endure %s seconds to ascend.");
		t.add(ModLang.MSG_GAP_DESCEND, "You fall into the Spatial Gap! Endure %s seconds to descend.");
		t.add(ModLang.MSG_GAP_RESTART, "Still in the Spatial Gap. The %s-second trial restarts.");
		t.add(ModLang.MSG_GAP_TIMER, "Spatial Pressure: %ss");
		t.add(ModLang.MSG_ARRIVE_UPPER, "The Upper Realm. Its dense qi makes cultivation %sx faster.");
		t.add(ModLang.MSG_ARRIVE_LOWER, "You are cast down into the lower realm.");
		t.add(ModLang.MSG_SUPPRESSED, "The lower realm suppresses you to %s.");
		t.add(ModLang.MSG_RESTORED, "Suppression lifted: %s.");
		t.add(ModLang.MSG_REALM_LOCKED, "Past %s, only the Upper Realm can hold you.");
		t.add(ModLang.MSG_PORTAL_BLOCKED, "The laws of this realm reject the portal.");
		t.add(ModLang.SUPPRESSED, "Realm Suppression! Stats capped at:");
		t.add(ModLang.SUPPRESSED_TO, "%s");
		t.add(ModLang.BREAKTHROUGH_SEALED, "Only the Upper Realm can hold this breakthrough.");

		// Keybinds
		t.add(ModLang.KEY_CATEGORY, "Defying The Heavens");
		t.add(ModLang.KEY_MENU, "Open Cultivation Menu");
		t.add(ModLang.KEY_MEDITATE, "Meditate (Lotus Position)");
		t.add(ModLang.KEY_CIRCULATE, "Circulate Qi (while meditating)");

		// Realms and stages
		for (Realm realm : Realm.values()) {
			t.add(realm.getTranslationKey(), switch (realm) {
				case QI_REFINING -> "Qi Refining";
				case FOUNDATION_BUILDING -> "Foundation Building";
				case CORE_FORMATION -> "Core Formation";
				case NASCENT_SOUL -> "Nascent Soul";
				case HEAVENLY_BEING -> "Heavenly Being";
				case FOUR_AXIS -> "Four Axis";
			});
		}
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
		t.add(ModLang.CULTIVATION_RATE, "Cultivation gain: %s/s");
		t.add(ModLang.CULTIVATION_RATE_UPPER, "Cultivation gain: %s/s (x%s Upper Realm)");
		t.add(ModLang.HUD_QI, "%s / %s");
		t.add(ModLang.HUD_QI_LABEL, "Qi");
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
		t.add(ModLang.STATS_QI_GATHER_UPPER, "Qi gather x%s in the Upper Realm");
		t.add(ModLang.ABILITIES_TITLE, "Abilities");
		t.add(ModLang.ABILITIES_EMPTY, "No abilities awakened yet.");
		t.add(ModLang.ABILITIES_HINT, "Abilities you awaken will appear here.");
		t.add(ModLang.ABILITY_ON, "On");
		t.add(ModLang.ABILITY_OFF, "Off");
		for (Ability ability : Ability.values()) {
			String key = ModLang.abilityKey(ability.getId());
			t.add(key, switch (ability) {
				case QI_SUSTENANCE -> "Qi Sustenance";
				case QI_FLIGHT -> "Qi Flight";
				case QI_SENSE -> "Qi Sense";
				case CONSCIOUSNESS_DOMAIN -> "Consciousness Domain";
				case REALM_SUPPRESS -> "Realm Suppress";
				case INNER_REALM -> "Inner Realm";
			});
			t.add(key + ".description", switch (ability) {
				case QI_SUSTENANCE -> "Qi keeps you fed. Hunger stays full.";
				case QI_FLIGHT -> "Double-tap jump to fly, at a cost of %s qi/s.";
				case QI_SENSE -> "See the plane of Qi: the five elements drifting through a grey world. No qi cost.";
				case CONSCIOUSNESS_DOMAIN -> "Sense all life within %s blocks, even through walls. Cultivators show their realm. No qi cost.";
				case REALM_SUPPRESS -> "Your realm crushes weaker beings within %s blocks, harder the weaker they are. Costs qi per target.";
				case INNER_REALM -> "After 10 s of meditation your soul turns inward (+50% cultivation). Get up to return. Harm to your body pulls you back.";
			});
		}
		t.add(ModLang.METHODS_TITLE, "Cultivation Methods");
		t.add(ModLang.METHODS_EMPTY, "No cultivation methods learned yet.");
		t.add(ModLang.METHODS_HINT, "Methods you learn will appear here.");
		t.add(ModLang.SPELLS_TITLE, "Spells");
		t.add(ModLang.SPELLS_EMPTY, "No spells learned yet.");
		t.add(ModLang.SPELLS_HINT, "Spells you learn will appear here.");
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
		t.add(ModLang.NEED_PILL, "Bottleneck! You need a %s to enter %s.");
		t.add(ModLang.PILL_PREPARED, "A pill's power waits to carry you into %s.");
		t.add(ModLang.QI_BOOST_LINE, "Qi Gathering Pill: +%s%% qi gathering");
		t.add(ModLang.PILL_RESISTANCE_LINE, "Medicinal toxicity: %s%%");
		t.add(ModLang.UNREFINED_LINE, "Unrefined qi: %s (meditate to refine)");
		t.add(ModLang.MSG_INNER_ENTER, "You close your eyes and turn inward.");
		t.add(ModLang.MSG_INNER_PULLED, "Your body is disturbed! Your soul snaps back.");
		t.add(ModLang.MSG_INNER_TRIBULATION, "Return to your body first.");
		t.add(ModLang.MSG_DEVIATION, "Qi deviation! Your circulation went astray.");
		t.add(ModLang.CIRCULATION_HINT, "[%s] Circulate qi");
		t.add(ModLang.CIRCULATION_STREAK, "Circulation x%s");
		t.add(ModLang.MSG_MORTAL_MEDITATE, "Your meridians are sealed. You can't gather qi yet.");
		t.add(ModLang.MSG_NEED_PILL, "Need a %s to enter %s. Eat it here, then break through.");
		t.add(ModLang.MSG_AWAKENED, "Your meridians open. You begin cultivating: %s!");
		t.add(ModLang.MSG_ALREADY_AWAKENED, "Your meridians are already open.");
		t.add(ModLang.MSG_PILL_PREPARED, "%s absorbed. The way into %s is open (+%s%% head start).");
		t.add(ModLang.MSG_PILL_WRONG_TIME, "This pill is only of use at %s.");
		t.add(ModLang.MSG_PILL_ALREADY, "You have already taken a pill for %s.");
		t.add(ModLang.MSG_PILL_MORTAL, "A mortal body can't absorb this.");
		t.add(ModLang.MSG_PILL_BOTTLENECK, "Bottleneck: the pill would be wasted.");
		t.add(ModLang.MSG_PILL_CULTIVATION, "+%s unrefined qi (%s%% effective). Meditate to refine it.");
		t.add(ModLang.MSG_PILL_QI, "+%s%% qi gathering for %s minutes");
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
	}
}
