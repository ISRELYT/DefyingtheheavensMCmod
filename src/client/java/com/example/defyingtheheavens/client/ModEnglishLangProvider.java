package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.ModEntities;
import com.example.defyingtheheavens.ModEffects;
import com.example.defyingtheheavens.ModItems;
import com.example.defyingtheheavens.ModLang;
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
		t.add("item.defying-the-heavens.cultivation_fruit.gain", "Eat for up to %s cultivation (stage limits apply)");
		t.add("item.defying-the-heavens.cultivation_fruit.picked", "Harvested: no longer ages and cannot be replanted");
		t.add("item.defying-the-heavens.cultivation_fruit.full", "Your inventory is full. Make room to harvest this fruit.");
		t.add(ModBlocks.MEDITATION_MAT, "Meditation Mat");
		t.add("gui.defying-the-heavens.baubles", "Baubles");
		t.add("config.jade.plugin_defying-the-heavens.fruit_age", "Fruit and Herb Age"); // Jade's toggle for the fruit-age tooltip
		t.add(ModBlocks.RED_MEDITATION_MAT, "Red Silk Meditation Mat");
		t.add(ModBlocks.SPIRIT_PEDESTAL, "Spirit Pedestal");
		t.add(ModLang.PEDESTAL_TOOLTIP, "Holds a Cultivation Fruit or a herb to aid meditation nearby");
		t.add(ModItems.GINSENG, "Ginseng");
		t.add(ModBlocks.GINSENG, "Ginseng");
		t.add(ModItems.SPIRIT_GINSENG, "Spirit Ginseng");
		t.add(ModBlocks.SPIRIT_GINSENG, "Spirit Ginseng");
		t.add(ModItems.PEACH_PIT, "Peach Pit");
		t.add(ModBlocks.SPIRIT_PEACH_SAPLING, "Spirit Peach Sapling");
		t.add(ModBlocks.SPIRIT_PEACH_LEAVES, "Spirit Peach Leaves");
		t.add(ModBlocks.SPIRIT_PEACH_HEART, "Spirit Peach Heart");
		t.add(ModLang.PEACH_PIT_TOOLTIP, "Plant it in soil to grow a Spirit Peach Tree");
		t.add(ModLang.PEACH_NEED_CORE, "Only a cultivator with a Golden Core (Core Formation) can pour qi into the tree.");
		t.add(ModLang.PEACH_NOT_ENOUGH, "Not enough qi: the tree needs %s for its next year.");
		t.add(ModLang.PEACH_MAX, "This tree has reached 10,000 years. It can grow no older.");
		t.add(ModLang.PEACH_FED, "You pour %s qi into the tree. It grows %s years (now %s years old).");
		t.add(ModLang.GINSENG_INGREDIENT, "An ingredient for pills and elixirs: brew it in an Alchemy Cauldron");
		t.add(ModLang.GINSENG_HARVESTED, "Harvested: no longer ages and cannot be replanted");
		t.add(ModLang.GINSENG_FULL, "Your inventory is full. Make room to harvest this herb.");
		t.add(ModItems.GINSENG_SEEDS, "Ginseng Seeds");
		t.add(ModItems.SPIRIT_GINSENG_SEEDS, "Spirit Ginseng Seeds");
		t.add(ModLang.GINSENG_SEEDS_TOOLTIP, "Plant in soil. Full grown after %s years, when digging it up also gives seeds");
		t.add(ModLang.GINSENG_NEED_CORE, "Only a cultivator with a Golden Core (Core Formation) can pour qi into a spirit herb.");
		t.add(ModLang.GINSENG_NOT_ENOUGH, "Not enough qi: the herb needs %s for its next year.");
		t.add(ModLang.GINSENG_MAX, "This herb has reached 10,000 years. It can grow no older.");
		t.add(ModLang.GINSENG_FED, "You pour %s qi into the herb. It grows %s years (now %s years old).");
		t.add(ModLang.FRUIT_PEDESTAL_TOOLTIP, "On a Spirit Pedestal: +%s%% cultivation speed while meditating nearby");
		t.add(ModItems.RING_OF_POWER, "Ring of Power");
		t.add(ModLang.RING_OF_POWER_TOOLTIP, "When worn: +%s cultivation per second while meditating");
		t.add(ModItems.RING_OF_TRANSCENDENCE, "Ring of Transcendence");
		t.add(ModLang.RING_OF_TRANSCENDENCE_TOOLTIP, "When worn: Heavenly Tribulation damage x%s");

		// Entities
		t.add(ModEntities.TRIBULATION_LIGHTNING, "Tribulation Lightning");

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
		t.add(ModLang.MSG_GAP_ASCEND, "Reality tears open and drags you into the Spatial Gap! Endure the pressure for %s seconds to ascend.");
		t.add(ModLang.MSG_GAP_DESCEND, "You fall through the floor of the Upper Realm into the Spatial Gap! Endure the pressure for %s seconds to descend.");
		t.add(ModLang.MSG_GAP_RESTART, "The Spatial Gap still holds you. The %s-second trial begins again.");
		t.add(ModLang.MSG_GAP_TIMER, "Spatial Pressure: %ss");
		t.add(ModLang.MSG_ARRIVE_UPPER, "You emerge into the Upper Realm. Its dense qi makes meditation and Qi gathering here %sx as fruitful.");
		t.add(ModLang.MSG_ARRIVE_LOWER, "You are cast down into the lower realm.");
		t.add(ModLang.MSG_SUPPRESSED, "The lower realm cannot sustain your cultivation. You are suppressed to %s.");
		t.add(ModLang.MSG_RESTORED, "The suppression lifts. Your true cultivation returns: %s.");
		t.add(ModLang.MSG_REALM_LOCKED, "Only the Upper Realm can sustain a breakthrough beyond %s.");
		t.add(ModLang.MSG_PORTAL_BLOCKED, "The laws of this realm reject the portal.");
		t.add(ModLang.SUPPRESSED, "Realm Suppression! Stats capped at:");
		t.add(ModLang.SUPPRESSED_TO, "%s");
		t.add(ModLang.BREAKTHROUGH_SEALED, "Only the Upper Realm can sustain this breakthrough.");

		// Keybinds
		t.add(ModLang.KEY_CATEGORY, "Defying The Heavens");
		t.add(ModLang.KEY_MENU, "Open Cultivation Menu");
		t.add(ModLang.KEY_MEDITATE, "Meditate (Lotus Position)");

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
		t.add(ModLang.ABILITIES_HINT, "Abilities you awaken through cultivation will appear here.");
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
			});
			t.add(key + ".description", switch (ability) {
				case QI_SUSTENANCE -> "Qi sustains your body: hunger and saturation always stay full.";
				case QI_FLIGHT -> "Double-tap jump to fly, at a cost of %s qi/s.";
				case QI_SENSE -> "Peer into the plane of Qi: the world loses its colour and the qi of the five elements drifts "
						+ "through it, drawn into anyone meditating. Costs no qi.";
				case CONSCIOUSNESS_DOMAIN -> "Your consciousness spreads %s blocks around you. Every living thing inside shows "
						+ "through walls, cultivators with their realm, and you feel any other domain that touches yours. Costs no qi.";
				case REALM_SUPPRESS -> "The weight of your realm presses down on every weaker being within %s blocks: mortals "
						+ "lose half their strength, lower cultivators more the further below you they are. Costs qi for each one.";
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
		t.add(ModLang.MSG_START, "You sit in the lotus position and begin to gather qi.");
		t.add(ModLang.MSG_STOP, "You open your eyes, ending your meditation.");
		t.add(ModLang.MSG_INTERRUPTED, "Your meditation was interrupted!");
		t.add(ModLang.MSG_BOOST, "Cultivation speed +%s%% (%s)");
		t.add(ModLang.MSG_BOOST_NONE, "Your surroundings no longer aid your cultivation.");
		t.add(ModLang.BOOST_PEDESTAL, "1 Spirit Pedestal");
		t.add(ModLang.BOOST_PEDESTALS, "%s Spirit Pedestals");
		t.add(ModLang.BOOST_HEIGHT, "High Altitude");
		t.add(ModLang.BOOST_TRANQUIL, "Tranquil Surroundings");
		t.add(ModLang.MSG_CANNOT, "You need steady, dry ground to meditate.");
		t.add(ModLang.MSG_STAGE_UP, "Your cultivation advances: %s - %s!");
		t.add(ModLang.MSG_BREAKTHROUGH, "You survived the Heavenly Tribulation and ascended to %s!");
		t.add(ModLang.MSG_NOT_READY, "You are not ready to break through.");
		t.add(ModLang.MSG_BOTTLENECK, "You have reached a bottleneck. Open the cultivation menu and break through!");
		t.add(ModLang.MSG_QI_FLIGHT_GAINED, "Your golden core circulates enough Qi through your body to make it tangible. "
				+ "You can now use it to carry your physical form. Double-tap jump to fly.");
		t.add(ModLang.MSG_QI_FLIGHT_EXHAUSTED, "Your qi runs dry and you fall! You can fly again at %s%% qi.");
		t.add(ModLang.MSG_QI_SENSE_GAINED, "You can now peer into the plane of Qi.");
		t.add(ModLang.MSG_SUPPRESS_EXHAUSTED, "Your qi runs dry and your realm's pressure lifts. It returns at %s%% qi.");
		t.add(ModLang.CONSCIOUSNESS_ALERT_TITLE, "A foreign consciousness touches yours");
		t.add(ModLang.CONSCIOUSNESS_ALERT_DETAIL, "%s - %s");
		t.add(ModLang.CONSCIOUSNESS_UNKNOWN, "Unknown realm");
		t.add(ModLang.PRESSURE, "A higher realm weighs on you!");
		t.add(ModLang.PRESSURE_DETAIL, "Strength and qi -%s%%");
		t.add(ModLang.STATS_PRESSED, "is pressed down to");
		t.add(ModLang.HUD_PRESSURE, "Suppressed -%s%%");
		t.add(ModLang.MSG_TRIB_START, "The heavens take notice of your ascent to %s! Survive %s strikes of heavenly lightning!");
		t.add(ModLang.MSG_TRIB_START_SINGLE, "The heavens take notice of your ascent to %s! Survive a strike of heavenly lightning!");
		t.add(ModLang.MSG_TRIB_BUSY, "You cannot do that while a tribulation is in progress.");
		t.add(ModLang.MSG_TRIB_FAILED, "%s has failed their breakthrough and suffered an existential backlash!");
		t.add(ModLang.MSG_TRIB_FALL, "The backlash shatters your foundation. Your cultivation falls to %s.");
		t.add(ModLang.MSG_TRIB_ABANDONED, "You fall out of the realm and the tribulation clouds lose you. The breakthrough is abandoned.");
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
		t.add(ModLang.MORTAL_DETAIL, "Your meridians are sealed: you can't gather qi, and cultivation can't take hold in you.");
		t.add(ModLang.MORTAL_RECIPE, "Drink a %s to open them. Brew it in an Alchemy Cauldron over a fire: %s.");
		t.add(ModLang.MORTAL_STATS, "A mortal's body has no realm to strengthen it.");
		t.add(ModLang.NEED_PILL, "Bottleneck reached! You need a %s to break into %s.");
		t.add(ModLang.PILL_PREPARED, "A pill's power waits to carry you into %s.");
		t.add(ModLang.QI_BOOST_LINE, "Qi Gathering Pill: +%s%% qi gathering");
		t.add(ModLang.PILL_RESISTANCE_LINE, "Pill resistance: %s%%");
		t.add(ModLang.MSG_MORTAL_MEDITATE, "Your meridians are sealed. Drink a Marrow Cleansing Elixir before you can gather qi.");
		t.add(ModLang.MSG_NEED_PILL, "You need a %s to break into %s. Eat it at this bottleneck, then break through.");
		t.add(ModLang.MSG_AWAKENED, "Black impurities seep from your pores as your meridians open. You have begun to cultivate: %s!");
		t.add(ModLang.MSG_ALREADY_AWAKENED, "Your meridians are already open.");
		t.add(ModLang.MSG_PILL_PREPARED, "The %s settles in your dantian: the way into %s is open (+%s%% head start). Break through when ready.");
		t.add(ModLang.MSG_PILL_WRONG_TIME, "This pill is only of use at %s.");
		t.add(ModLang.MSG_PILL_ALREADY, "You have already taken a pill for %s.");
		t.add(ModLang.MSG_PILL_MORTAL, "A mortal body can't absorb this pill. Drink a Marrow Cleansing Elixir first.");
		t.add(ModLang.MSG_PILL_BOTTLENECK, "Your cultivation can't hold any more right now. The pill would be wasted.");
		t.add(ModLang.MSG_PILL_CULTIVATION, "+%s cultivation (pill effectiveness %s%%)");
		t.add(ModLang.MSG_PILL_QI, "+%s%% qi gathering for %s minutes");
		t.add(ModLang.PILL_AGE, "Ingredients: %s years old");
		t.add(ModLang.PILL_AGE_SHORT, " (%s years)");
		t.add(ModLang.PILL_RECIPE, "Brewed from: %s");
		t.add(ModLang.PILL_EFFECT_AWAKEN, "Opens a mortal's meridians: begin cultivating at Qi Refining");
		t.add(ModLang.PILL_EFFECT_BREAKTHROUGH, "Opens the way into %s (the tribulation remains)");
		t.add(ModLang.PILL_EFFECT_START, "Start %s%% of the way into the new stage");
		t.add(ModLang.PILL_EFFECT_QI, "+%s%% qi gathering for %s minutes");
		t.add(ModLang.PILL_EFFECT_CULTIVATION, "Grants %s%% of your current stage's cultivation");
		t.add(ModLang.PILL_EFFECT_RESISTANCE, "Pill resistance: works at %s%% right now");
		t.add(ModLang.PILL_WHEN, "Take it at %s");
		t.add(ModLang.CAULDRON_TOOLTIP, "Fill with water, light a fire beneath, then add ingredients");
		t.add(ModLang.CAULDRON_NEEDS_WATER, "Fill the cauldron with a water bucket first.");
		t.add(ModLang.CAULDRON_EMPTY, "The water is ready. Add ingredients one at a time.");
		t.add(ModLang.CAULDRON_CONTENTS, "In the cauldron: %s (sneak with an empty hand to tip them out)");
		t.add(ModLang.CAULDRON_ADDED, "%s added. In the cauldron: %s");
		t.add(ModLang.CAULDRON_REJECTED, "%s doesn't fit any recipe with what's already in the cauldron.");
		t.add(ModLang.CAULDRON_FULL, "The cauldron can't hold any more ingredients.");
		t.add(ModLang.CAULDRON_EMPTIED, "You tip the ingredients back out.");
		t.add(ModLang.CAULDRON_BREWING, "Brewing %s... %s%%");
		t.add(ModLang.CAULDRON_NEEDS_HEAT, "%s is ready to brew. Light a fire beneath the cauldron.");
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
		t.add(ModLang.LOTUS_SEEDS_TOOLTIP, "Place on still water. Full grown after %s years, when picking it also gives seeds");
		t.add(ModLang.DEW_TOOLTIP, "Drink to restore %s%% of your qi");
		t.add(ModLang.MSG_DEW_MORTAL, "A mortal body can't hold the dew's qi.");
		t.add(ModLang.MSG_DEW_FULL, "Your qi is already full.");
		t.add(ModLang.MSG_DEW_DRUNK, "+%s qi");
		t.add(ModLang.PILL_SUBSTITUTE, "Stands in for %s in any recipe, counting as twice its age");
	}
}
