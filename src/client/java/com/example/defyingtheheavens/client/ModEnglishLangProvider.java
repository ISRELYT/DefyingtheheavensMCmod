package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.ModEntities;
import com.example.defyingtheheavens.ModItems;
import com.example.defyingtheheavens.ModLang;
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
		t.add(ModItems.RING_OF_POWER, "Ring of Power");
		t.add(ModLang.RING_OF_POWER_TOOLTIP, "When worn: +%s cultivation per second while meditating");
		t.add(ModItems.RING_OF_TRANSCENDENCE, "Ring of Transcendence");
		t.add(ModLang.RING_OF_TRANSCENDENCE_TOOLTIP, "When worn: Heavenly Tribulation damage x%s");

		// Entities
		t.add(ModEntities.TRIBULATION_LIGHTNING, "Tribulation Lightning");

		// Blocks
		t.add(ModBlocks.SPATIAL_RIFT, "Spatial Rift");
		t.add(ModBlocks.JADE_STONE, "Jade Stone");
		t.add(ModBlocks.WHITE_BLOSSOM_LEAVES, "White Blossom Leaves");

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
		t.add(ModLang.HUD_QI_LABEL, "Qi");		t.add(ModLang.BOTTLENECK, "Bottleneck reached! Break through to advance.");
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
			});
			t.add(key + ".description", switch (ability) {
				case QI_SUSTENANCE -> "Qi sustains your body: hunger and saturation always stay full.";
				case QI_FLIGHT -> "Double-tap jump to fly, at a cost of %s qi/s.";
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
		t.add(ModLang.MSG_CANNOT, "You need steady, dry ground to meditate.");
		t.add(ModLang.MSG_STAGE_UP, "Your cultivation advances: %s - %s!");
		t.add(ModLang.MSG_BREAKTHROUGH, "You survived the Heavenly Tribulation and ascended to %s!");
		t.add(ModLang.MSG_NOT_READY, "You are not ready to break through.");
		t.add(ModLang.MSG_BOTTLENECK, "You have reached a bottleneck. Open the cultivation menu and break through!");
		t.add(ModLang.MSG_QI_FLIGHT_GAINED, "Your golden core circulates enough Qi through your body to make it tangible. "
				+ "You can now use it to carry your physical form. Double-tap jump to fly.");
		t.add(ModLang.MSG_QI_FLIGHT_EXHAUSTED, "Your qi runs dry and you fall! You can fly again at %s%% qi.");
		t.add(ModLang.MSG_QI_FLIGHT_RESTORED, "Your qi can bear you aloft again.");
		t.add(ModLang.MSG_TRIB_START, "The heavens take notice of your ascent to %s! Survive %s strikes of heavenly lightning!");
		t.add(ModLang.MSG_TRIB_START_SINGLE, "The heavens take notice of your ascent to %s! Survive a strike of heavenly lightning!");
		t.add(ModLang.MSG_TRIB_BUSY, "You cannot do that while a tribulation is in progress.");
		t.add(ModLang.MSG_TRIB_FAILED, "%s has failed their breakthrough and suffered an existential backlash!");
		t.add(ModLang.MSG_TRIB_FALL, "The backlash shatters your foundation. Your cultivation falls to %s.");
		t.add(ModLang.MSG_TRIB_ABANDONED, "You fall out of the realm and the tribulation clouds lose you. The breakthrough is abandoned.");
		t.add(ModLang.TRIB_HUD, "Heavenly Tribulation (%s) - strikes left: %s");
	}
}
