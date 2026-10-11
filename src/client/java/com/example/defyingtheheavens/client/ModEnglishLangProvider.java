package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.Alignment;
import com.example.defyingtheheavens.ClothingStyle;
import com.example.defyingtheheavens.ClothingTier;
import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.NpcTitle;
import net.minecraft.world.item.ArmorItem;
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
		t.add(ModEntities.HEART_DEMON, "Heart Demon");
		t.add(ModBlocks.SOUL_CRYSTAL, "Soul Crystal");

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
		t.add(ModLang.KEY_CIRCULATE, "Circulate Qi (qi surges)");
		t.add(ModLang.KEY_ABILITY_WHEEL, "Ability Wheel (hold)");

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
		t.add(ModLang.BTN_ARRANGE_WHEEL, "Arrange Wheel");
		t.add(ModLang.BTN_ABILITY_LIST, "Ability List");
		t.add(ModLang.WHEEL_TITLE, "Ability Wheel");
		t.add(ModLang.WHEEL_HINT, "Click an ability to switch it on or off");
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
			});
			t.add(key + ".description", switch (ability) {
				case QI_SUSTENANCE -> "Qi keeps you fed. Hunger stays full.";
				case QI_FLIGHT -> "Double-tap jump to fly, at a cost of %s qi/s. Switching it on switches Sword Flight off.";
				case QI_SENSE -> "See the plane of Qi: the five elements drifting through a grey world. No qi cost.";
				case CONSCIOUSNESS_DOMAIN -> "Sense all life within %s blocks, even through walls. Cultivators show their realm. No qi cost.";
				case REALM_SUPPRESS -> "Your realm crushes weaker beings within %s blocks, harder the weaker they are. Costs qi per target.";
				case INNER_REALM -> "After 10 s of meditation your soul turns inward (+50% cultivation). Walk freely; stop meditating to return. Harm to your body pulls you back.";
				case SWORD_FLIGHT -> "Hold a sword and double-tap jump to ride it, at a cost of %s qi/s. Cheaper than Qi Flight but slower (75%% of its speed, at every realm); the sword takes no wear. Switching it on switches Qi Flight off.";
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
		t.add(ModLang.MSG_SWORD_FLIGHT_GAINED, "Your foundation can send qi through a blade. Hold a sword and double-tap jump to ride it.");
		t.add(ModLang.MSG_FLIGHT_EXCLUSIVE, "Only one flight at a time: Qi Flight is on and Sword Flight is off. Switch between them on the ability wheel or the Abilities tab.");
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
		t.add(ModLang.MSG_TRIB_FLED, "You fled the tribulation, but the heavens remember. Backlash! You fall to %s.");
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
				case DEMONIC -> "Demonic";
			});
		}
		t.add(ModLang.ALIGNMENT_VALUE, "%s (%s)");
		t.add(ModLang.ALIGNMENT_LINE, "Path: %s");
		t.add(ModLang.MSG_ALIGNMENT_UP, "Slaying the wicked strengthens your heart: alignment +%s (%s)");
		t.add(ModLang.MSG_ALIGNMENT_DOWN, "Innocent blood stains your hands: alignment -%s (%s)");

		// NPC cultivators and sects
		t.add(ModEntities.CULTIVATOR, "Cultivator");
		t.add(ModItems.CULTIVATOR_SPAWN_EGG, "Cultivator Spawn Egg");
		for (NpcTitle title : NpcTitle.values()) {
			t.add(ModLang.titleKey(title.getId()), switch (title) {
				case SECT_MASTER -> "Sect Master";
				case GRAND_ELDER -> "Grand Elder";
				case ELDER -> "Elder";
				case INNER_DISCIPLE -> "Inner Disciple";
				case OUTER_DISCIPLE -> "Outer Disciple";
				case ROGUE -> "Rogue Cultivator";
				case WANDERER -> "Wandering Mortal";
			});
		}
		t.add(ModLang.NPC_TITLE_BRACKETS, "[%s]");
		t.add(ModLang.NPC_GATE_GUARD, "Gate Guard");
		t.add(ModLang.NPC_OF_SECT, "%s of the %s");
		t.add(ModLang.NPC_GREET_RIGHTEOUS, "%s nods to you: \"May your path be clear and your heart steady.\"");
		t.add(ModLang.NPC_GREET_NEUTRAL, "%s regards you calmly and goes back to their own affairs.");
		t.add(ModLang.NPC_GREET_DEMONIC, "%s smiles thinly: \"Walk carefully, little one.\"");
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
		t.add("cultivation.defying-the-heavens.talk.line.ask.demonic.above", "Questions cost nothing. Answers might.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.demonic.below", "Senior wishes to know? Ask, ask.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.demonic.far_above", "You want something from me? How bold. Ask, before I lose interest.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.demonic.far_below", "This one will hold nothing back from Senior!");
		t.add("cultivation.defying-the-heavens.talk.line.ask.demonic.mortal", "A curious mortal. Ask; it amuses me.");
		t.add("cultivation.defying-the-heavens.talk.line.ask.demonic.peer", "Ask. I'll decide what's true.");
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
		t.add("cultivation.defying-the-heavens.talk.line.gift.good.commoner", "This is too much, Immortal. Thank you!");
		t.add("cultivation.defying-the-heavens.talk.line.gift.good.demonic", "Now this is a proper gift.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.good.neutral", "Useful. My thanks.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.good.righteous", "This is generous. I won't forget it.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.great.commoner", "I... I don't know what to say. Thank you, Immortal!");
		t.add("cultivation.defying-the-heavens.talk.line.gift.great.demonic", "Ha! You do understand how the world works.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.great.neutral", "This is worth a great deal. I'll remember it.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.great.righteous", "Such a treasure... this one is in your debt.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.refuse.commoner", "I couldn't take that, Immortal.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.refuse.demonic", "Are you mocking me?");
		t.add("cultivation.defying-the-heavens.talk.line.gift.refuse.neutral", "What would I do with that?");
		t.add("cultivation.defying-the-heavens.talk.line.gift.refuse.righteous", "This one cannot accept that.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.small.commoner", "For me? Thank you, Immortal.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.small.demonic", "Is that all?");
		t.add("cultivation.defying-the-heavens.talk.line.gift.small.neutral", "Hm. Thank you.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.small.righteous", "A kind thought. Thank you.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.commoner", "Greetings, Immortal. Safe travels.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.commoner.mortal", "Good day to you, traveller.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.demonic.above", "Little one, you have manners. They might even keep you alive.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.demonic.below", "Senior... this one meant no offence. None at all.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.demonic.far_above", "An ant that bows. How sweet. Crawl along.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.demonic.far_below", "S-senior! This worthless one greets Senior!");
		t.add("cultivation.defying-the-heavens.talk.line.greet.demonic.mortal", "A mortal, walking right up to me. Brave, or stupid?");
		t.add("cultivation.defying-the-heavens.talk.line.greet.demonic.peer", "Fellow Daoist. Shall we pretend we're friends?");
		t.add("cultivation.defying-the-heavens.talk.line.greet.neutral.above", "Junior. What is it?");
		t.add("cultivation.defying-the-heavens.talk.line.greet.neutral.below", "Senior. This one is listening.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.neutral.far_above", "Hm.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.neutral.far_below", "Senior's presence is... considerable. How can this one help?");
		t.add("cultivation.defying-the-heavens.talk.line.greet.neutral.mortal", "A mortal. Keep out from underfoot and no one will trouble you.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.neutral.peer", "Fellow Daoist.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.righteous.above", "Well met, little friend. Your foundation looks steady.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.righteous.below", "Senior honours this one by stopping. Please, be at ease.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.righteous.far_above", "Junior. Keep to the path and mind your manners on this mountain.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.righteous.far_below", "This junior greets Senior! What does Senior require?");
		t.add("cultivation.defying-the-heavens.talk.line.greet.righteous.mortal", "A mortal this far up? Go carefully, and keep to the path.");
		t.add("cultivation.defying-the-heavens.talk.line.greet.righteous.peer", "Fellow Daoist. May your path be clear and your heart steady.");
		t.add("cultivation.defying-the-heavens.talk.line.nearby", "The %1$s, on the %2$s path, lies %3$s of here, %4$s blocks or so.");
		t.add("cultivation.defying-the-heavens.talk.line.nearby.enemy", "The %1$s, %2$s-path dogs, are %3$s of here, %4$s blocks or so. Keep clear of them.");
		t.add("cultivation.defying-the-heavens.talk.line.nearby.none", "No sect that I know of, not within a few days on foot.");
		t.add("cultivation.defying-the-heavens.talk.line.people", "Guarding the treasury and the core: %1$s. Sect Master %2$s keeps the main hall.");
		t.add("cultivation.defying-the-heavens.talk.line.people.alone", "There is no one else to answer for this sect.");
		t.add("cultivation.defying-the-heavens.talk.line.people.master_self", "Guarding the treasury and the core: %1$s. The main hall is mine.");
		t.add("cultivation.defying-the-heavens.talk.line.people.no_elders", "Sect Master %1$s keeps the main hall. We have no elders now.");
		t.add("cultivation.defying-the-heavens.talk.line.people.no_master", "Guarding the treasury and the core: %1$s. We have no master now.");
		t.add("cultivation.defying-the-heavens.talk.line.provoke.attack.commoner", "That's enough! I won't be spoken to like that!");
		t.add("cultivation.defying-the-heavens.talk.line.provoke.attack.demonic", "I was hoping you'd say that.");
		t.add("cultivation.defying-the-heavens.talk.line.provoke.attack.neutral", "You'll regret that.");
		t.add("cultivation.defying-the-heavens.talk.line.provoke.attack.righteous", "Then let your blade speak for you!");
		t.add("cultivation.defying-the-heavens.talk.line.provoke.scorn.commoner", "Please, Immortal, I don't want any trouble.");
		t.add("cultivation.defying-the-heavens.talk.line.provoke.scorn.demonic", "Barking at me? Cute.");
		t.add("cultivation.defying-the-heavens.talk.line.provoke.scorn.neutral", "I've no time for fools.");
		t.add("cultivation.defying-the-heavens.talk.line.provoke.scorn.righteous", "Your words dirty only yourself.");
		t.add("cultivation.defying-the-heavens.talk.line.sect", "The %1$s: %2$s of us, on the %3$s path, led by Sect Master %4$s at %5$s.");
		t.add("cultivation.defying-the-heavens.talk.line.sect.no_master", "The %1$s: %2$s of us, on the %3$s path.");
		t.add("cultivation.defying-the-heavens.talk.line.sect.self_master", "The %1$s: %2$s of us, on the %3$s path. I lead it.");
		t.add("cultivation.defying-the-heavens.talk.line.self.commoner", "%1$s. I'm only a mortal, Immortal. I have no cultivation to speak of.");
		t.add("cultivation.defying-the-heavens.talk.line.self.rogue.demonic", "%1$s, at %2$s. I answer to no sect, and to no one.");
		t.add("cultivation.defying-the-heavens.talk.line.self.rogue.neutral", "%1$s. %2$s. I go where the qi is.");
		t.add("cultivation.defying-the-heavens.talk.line.self.rogue.righteous", "I am %1$s, a wandering cultivator at %2$s. No sect, only the road.");
		t.add("cultivation.defying-the-heavens.talk.line.self.sect.demonic", "%1$s, %2$s of the %3$s, at %4$s. Remember the name. It may be the last one you learn.");
		t.add("cultivation.defying-the-heavens.talk.line.self.sect.neutral", "%1$s, %2$s of the %3$s. %4$s. That's all there is to say.");
		t.add("cultivation.defying-the-heavens.talk.line.self.sect.righteous", "I am %1$s, %2$s of the %3$s, at %4$s. I walk the righteous path.");
		t.add("cultivation.defying-the-heavens.talk.list", ", ");
		t.add("cultivation.defying-the-heavens.talk.mortal", "Mortal");
		t.add("cultivation.defying-the-heavens.talk.occupied", "%s is speaking with someone else.");
		t.add("cultivation.defying-the-heavens.talk.path.demonic", "demonic");
		t.add("cultivation.defying-the-heavens.talk.path.neutral", "neutral");
		t.add("cultivation.defying-the-heavens.talk.path.righteous", "righteous");
		t.add("cultivation.defying-the-heavens.talk.pick", "Click, or press 1-%s");
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
		t.add(ModLang.MSG_SECLUSION_REPULSE, "A concealment barrier pushes you back. Disturbing a secluded cultivator would be unwise.");

		// Cultivator clothing
		for (ClothingStyle style : ClothingStyle.values()) {
			t.add(ModLang.clothingStyleKey(style.getId()), switch (style) {
				case RIGHTEOUS -> "Righteous white";
				case NEUTRAL -> "Neutral blue";
				case DEMONIC -> "Demonic red and black";
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
	}
}
