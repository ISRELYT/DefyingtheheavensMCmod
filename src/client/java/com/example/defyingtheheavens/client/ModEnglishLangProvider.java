package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.Alignment;
import com.example.defyingtheheavens.AstralPlanets;
import com.example.defyingtheheavens.ClothingStyle;
import com.example.defyingtheheavens.ClothingTier;
import com.example.defyingtheheavens.GhostMethod;
import com.example.defyingtheheavens.Spell;
import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.NpcTitle;
import net.minecraft.world.item.ArmorItem;
import com.example.defyingtheheavens.ModEntities;
import com.example.defyingtheheavens.ModEffects;
import com.example.defyingtheheavens.ModItems;
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
		t.add(ModLang.GAMERULE_AFTER_DEATH, "The dead go to the Netherworld");
		t.add(ModLang.GAMERULE_AFTER_DEATH + ".description", "Players who die wake at the Ghost Gate as ghosts, and return to life at the Wheel of Reincarnation.");
		t.add(ModLang.MSG_GHOST_WAKE, "You are dead. The Wheel of Reincarnation, across the bridge, will return you to life.");
		t.add(ModLang.MSG_YIN_OVERTAKEN, "The yin overtakes you: you walk on among the dead.");
		t.add(ModLang.MSG_BELONGINGS_LEFT, "What you carried stays behind at the Wheel.");
		t.add(ModLang.MSG_GHOST_KING, "You are a Ghost King: you can touch the world again, and cross to the living.");
		t.add(ModLang.MSG_GHOST_KING_LOST, "You are no longer a Ghost King.");
		t.add(ModLang.JOURNAL_GHOST_KING, "A Ghost King");
		t.add(ModLang.JOURNAL_GHOST_KING_TEXT, "Dead, I rose to Core Formation and became a Ghost King. My hands are my own again, and the "
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
		t.add(ModLang.DEATH_SOUL_SEVERED, "%1$s's nascent soul was severed from their body");
		t.add(ModLang.DEATH_SOUL_SEVERED + ".player", "%1$s's nascent soul was severed from their body by %2$s");
		t.add(ModLang.DEATH_SOUL_SEIZED, "%1$s's body was seized by %2$s's nascent soul");
		t.add(ModLang.DEATH_SOUL_SEIZED + ".item", "%1$s's body was seized by %2$s's nascent soul");
		t.add(ModLang.DEATH_SOUL_SEIZED + ".player", "%1$s's body was seized by %2$s's nascent soul");

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

		// The creative tab
		t.add(ModLang.CREATIVE_TAB, "Defying The Heavens");

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
		t.add(ModLang.METHOD_LINE, "%s, layer %s of %s: +%s/s");
		t.add(ModLang.METHOD_LINE_NAME, "%s: +%s/s");
		t.add(ModLang.METHOD_LINE_SHORT, "Layer %s of %s: +%s/s");
		t.add(ModLang.METHOD_LINE_NONE, "No heaven method: +%s/s");
		t.add(ModLang.SETTLING_LINE, "Layer %s settling: %s%%");
		t.add(ModLang.UPPER_REALM_LINE, "Upper Realm: qi x%s");
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
			});
			t.add(key + ".description", switch (ability) {
				case QI_SUSTENANCE -> "Qi keeps you fed. Hunger stays full.";
				case QI_FLIGHT -> "Double-tap jump to fly, at a cost of %s qi/s. Switching it on switches Sword Flight off.";
				case QI_SENSE -> "See the plane of Qi: the five elements drifting through a grey world. No qi cost.";
				case CONSCIOUSNESS_DOMAIN -> "Sense all life within %s blocks, even through walls. Cultivators show their realm. No qi cost.";
				case REALM_SUPPRESS -> "Your realm crushes weaker beings within %s blocks, harder the weaker they are. Costs qi per target.";
				case INNER_REALM -> "After 10 s of meditation your soul turns inward (+50% cultivation). Walk freely; stop meditating to return. Harm to your body pulls you back.";
				case SWORD_FLIGHT -> "Hold a sword and double-tap jump to ride it, at a cost of %s qi/s. Cheaper than Qi Flight but slower (75%% of its speed, at every realm); the sword takes no wear. Switching it on switches Qi Flight off.";
				case SOUL_PROJECTION -> "Send your nascent soul out of your body (5 s to leave, 5 s to return) while your body sits on. It flies through walls on %s hearts, within %s blocks of your body, for %s-%s qi/s (more the farther it goes). Right-click a being three realms below you to possess it. If its tether is cut or your body slain, you die and fall to Core Formation.";
				case GHOST_EYE -> "See the dead and the Method Steles through walls within 32 blocks. Costs %s ghost qi/s.";
				case NETHERWORLD_VEIL -> "Wrap your body in the Netherworld's own yin: it seeps into you at half the rate. Costs %s ghost qi/s.";
				case YIN_WIND_STEPS -> "Walk on the yin wind: %s%% swifter, and falls do you no harm. Costs %s ghost qi/s.";
				case GHOST_FLIGHT -> "The dead fly at a drift (double-tap jump); with this, as swiftly as on qi, at a cost of %s ghost qi/s.";
				case FORM_SHIFT -> "Your true form is the form of your higher path: now your %s. Switched on, you take your %s instead. "
						+ "In a Ghost King's form you are seen as the dead are: outside the Netherworld, only by those who perceive souls. "
						+ "Only while you live; no cost.";
				case CORE_DETONATION -> "The last resort. Use it twice within 5 s and your golden core breaks: 3 s later it bursts, everything within %s blocks "
						+ "harmed by its distance and how far its realm stands below yours (a cultivator of your own realm in good armour may live). "
						+ "You die, and the core is lost: you return at Foundation Building Grand Perfection.";
				case SPATIAL_TEAR -> "Tear space open before you: a rift that stands 20 s, that you and anyone with you can walk through, into the Spatial Gap "
						+ "or out of it again into the realm you came from. There you fly freely, "
						+ "each block crossing %s in the world, under spatial pressure (worse in its stormy layers: rise through the upper ones to cross "
						+ "into the Upper Realm, or sink slowly through the lower ones to fall into the mortal world). "
						+ "Costs %s%% of your qi to tear in, %s%% to tear out.";
			});
		}
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
				case SIX_PATHS_CANON -> "The last foundation, the law of the Wheel itself: the six paths of rebirth turn within you. It carries "
						+ "your ghost path from Qi Refining all the way through Four Axis.";
			});
			t.add(ModLang.methodKey(method.getId()) + ".gains", switch (method) {
				case NETHERWORLD_BREATHING -> "Qi Refining: layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection. Layer 3: Ghost Eye. Layer 5: Ghostly "
						+ "Wail. Layer 6: Netherworld Veil. Every layer: +20 ghost qi.";
				case GHOST_FIRE_SCRIPTURE -> "Your ghost path opens its layers: 1-3 at Qi Refining, 4-6 at Foundation Building, 7-9 at Core Formation. "
						+ "Layer 1: Ghost Fire. Layer 4: Ghost Fire Bolt. Layer 7: the fire leaps to a second foe. Every layer: a ghost's blows +10%.";
				case WANDERING_SOUL_STEPS -> "Your ghost path opens its layers: 1-3 at Qi Refining, 4-6 at Foundation Building, 7-9 at Core Formation. "
						+ "Layer 1: Yin Wind Steps. Layer 2: Soul Dash. Layer 4: Ghost Flight (the dead). Every layer: a ghost +3% speed.";
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
				case YIN_YANG_PALM -> "Yin-Yang Palm";
				case TAIJI_SHIELD -> "Taiji Shield";
				case LIFE_DEATH_SEAL -> "Life and Death Seal";
				case HEAVEN_HELL_PILLAR -> "Heaven and Hell Pillar";
				case PRIMORDIAL_CHAOS -> "Primordial Chaos";
				default -> throw new IllegalStateException(spell.getId()); // a heaven spell: skipped above
			});
			t.add(ModLang.spellKey(spell.getId()) + ".description", switch (spell) {
				case GHOST_FIRE -> "Wreathe your hands in cold fire: your next 3 blows within 20 seconds set what they strike burning (3 ghost qi each). "
						+ "It burns the dead three times as hard.";
				case GHOST_FIRE_BOLT -> "Throw a bolt of ghost fire up to 24 blocks: it strikes the first being in its path and sets it burning.";
				case SOUL_DASH -> "Rush forward on the yin wind, about eight blocks. Falls do you no harm for a moment after.";
				case GHOSTLY_WAIL -> "A cry of the dead: everything within 8 blocks is driven back, slowed and weakened, and the dead are torn at.";
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
				case HELL_CHAINS -> "Chains drag every foe within 12 blocks to you and hold them fast for 3 seconds.";
				case SAMSARA_WHEEL -> "The six paths turn: your health is made whole, your harmful effects are gone, and half your yin "
						+ "corruption with them.";
				case SIX_PATHS_ANNIHILATION -> "Six pillars of cold fire rise round you: everything within 12 blocks takes 20 hearts of ghostly "
						+ "harm and is hurled away.";
				case YIN_YANG_PALM -> "A spell of both paths. A palm of light and shadow strikes the first being within 16 blocks: 5 hearts of "
						+ "harm, living or dead, and it is thrown back. A quarter of the harm mends you.";
				case TAIJI_SHIELD -> "A spell of both paths. Light and dark turn round you for 10 seconds: arrows and fireballs shot at you are "
						+ "turned back on whoever shot them, and you have Resistance I.";
				case LIFE_DEATH_SEAL -> "A spell of both paths. Seal the being you look at, up to 32 blocks: for 8 seconds it loses 2 hearts a "
						+ "second, living or dead, and half of it comes to you. It is slowed while sealed.";
				case HEAVEN_HELL_PILLAR -> "A spell of both paths. Where you look, up to 32 blocks, heaven's light falls and hell's fire rises: "
						+ "everything within 5 blocks takes 15 hearts of harm, is thrown up and set burning.";
				case PRIMORDIAL_CHAOS -> "A spell of both paths, made one again. Everything within 16 blocks is dragged in and takes 25 hearts of "
						+ "harm; a tenth of all of it mends you, and you have Strength II for 10 seconds.";
				default -> throw new IllegalStateException(spell.getId()); // a heaven spell: skipped above
			});
		}
		t.add(ModLang.SPELLS_COST_BOTH, "%s qi and %s ghost qi");
		t.add(ModLang.SPELLS_QI_COST, "%s qi");
		t.add(ModLang.SPELLS_LOCKED_BOTH, "Both paths at %s");
		t.add(ModLang.MSG_SPELL_NO_BOTH, "Not enough qi for %s: a spell of both paths draws on your qi and your ghost qi.");
		t.add(ModLang.MSG_SPELL_BOTH_PATHS, "New spell: %2$s (both paths at %1$s).");
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
		t.add(ModLang.METHOD_BAR_PERFECTED, "%s: perfected");
		t.add(ModLang.METHOD_BAR_SEEK, "Perfected: seek the %s");
		t.add(ModLang.FORM_PREFIX + "human", "human form");
		t.add(ModLang.FORM_PREFIX + "ghost_king", "Ghost King form");
		t.add(ModLang.MSG_FORMS_OPEN, "Your ghost path reached Core Formation: Form Shift (Abilities) takes a Ghost King's form.");
		t.add(ModLang.MSG_FORMS_OPEN_DEAD, "Your ghost path reached Core Formation: a Ghost King's form is yours.");
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
				+ "(Form Shift); in the Ghost King's, outside the Netherworld only those who perceive souls see me. In death I am simply "
				+ "one of the dead.");
		t.add(ModLang.KEY_SPELL_WHEEL, "Spell Wheel (hold)");
		t.add(ModLang.KEY_CAST_SPELL, "Cast Readied Spell");
		t.add(ModLang.MSG_METHOD_LEARNED, "Learned the %s.");
		t.add(ModLang.MSG_METHOD_LEARNED_AT, "Learned the %s, at layer %s.");
		t.add(ModLang.MSG_METHOD_LAYER, "%s: layer %s reached.");
		t.add(ModLang.MSG_METHOD_REALM_CAPPED, "The %s waits at layer %s until your ghost path reaches %s.");
		t.add(ModLang.MSG_METHOD_FLOOR, "The %s carries your ghost path to %s.");
		t.add(ModLang.MSG_METHOD_PRACTISED, "Your meditation now cultivates the %s.");
		t.add(ModLang.MSG_GHOST_NO_METHOD, "The dead cultivate only the ghost methods. Learn one at a Method Stele first.");
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
		t.add(ModLang.METHODS_PROGRESS, "%s%% to the next layer");
		t.add(ModLang.METHODS_RATE, "Meditating here, the %s's next layer comes in about %s min.");
		t.add(ModLang.METHODS_NO_QI, "There is no ghostly qi here to cultivate with. Meditate in the Nether, or ten times as fast in the Netherworld.");
		t.add(ModLang.BTN_SPELL_LIST, "Spell List");
		t.add(ModLang.METHODS_REALM_CAPPED, "Held at layer %s until your ghost path reaches %s: practise your foundation method to raise it.");
		t.add(ModLang.METHODS_FLOOR_NEXT, "Layer %s of the %s carries your ghost path to %s.");
		t.add(ModLang.METHODS_SEEK_NEXT, "The %s is perfected. Only a foundation that goes further, the %s or one beyond it, carries your "
				+ "ghost path on: seek it out.");
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
		physique(t, "mortal_body", "Mortal Body", "An ordinary body. Nothing helps it and nothing holds it back.");
		physique(t, "five_element_spiritual_roots", "Five Elements Spiritual Roots",
				"Roots of all five elements: any element's methods may be learned, at the ordinary pace.");
		physique(t, "cursed_meridian_body", "Cursed Meridian Body", "Meridians that leak: qi gathers 0.8 times as fast into a pool 0.8 times as deep.");
		physique(t, "shattered_meridians", "Shattered Meridians",
				"Meridians that will never hold qi: no cultivation, no heaven methods, no spells. The body can be tempered on past a mortal's Peak instead, "
						+ "eight stages of body refining, the last as strong as a Core Formation cultivator's. Meditation tempers it too.");
		physique(t, "three_element_spiritual_roots", "Three Elements Spiritual Roots",
				"Roots of three elements only: methods of those three alone, cultivated 1.5 times as fast.");
		physique(t, "yin_soul_body", "Yin Soul Body", "A soul at home in yin: yin corruption never takes hold of it.");
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
				"No medicinal toxicity ever builds up, and the qi of pills and spirit fruit is refined the moment it is taken.");
		physique(t, "fallen_true_immortal", "Fallen True Immortal",
				"An immortal reborn in a mortal body: any element, five times the pace on both paths, and a method's new layers need no settling.");
		physique(t, "ghost_devil_body", "Ghost Devil Body",
				"Half the dead's already: the ghost path at twice the pace, and a Ghost King's form of its own anywhere, even among the living, "
						+ "as strong as its ghost methods allow. Find a ghost method of a deeper realm and the ghost path leaps to it.");
		physique(t, "heavenly_thunder_body", "Heavenly Thunder Body",
				"The rarest of all. A tribulation's lightning feeds it instead of harming it: every bolt taken becomes cultivation and qi.");
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
		t.add(ModLang.STATS_PHYSIQUE, "Physique: %s (%s)");
		t.add(ModLang.STATS_ROOTS, "Roots: %s");
		t.add(ModLang.STATS_BODY_REFINING, "Body refining: %s");
		t.add(ModLang.CMD_PHYSIQUE_GET, "%s was born with a %s");
		t.add(ModLang.CMD_PHYSIQUE_SET, "%s now has a %s");
		t.add(ModLang.CMD_PHYSIQUE_UNKNOWN, "No physique called %s");
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
		t.add(ModLang.DEATH_CORE_DETONATION, "%1$s detonated their golden core");
		t.add(ModLang.DEATH_CORE_DETONATION + ".player", "%1$s detonated their golden core against %2$s");
		// The Spatial Tear and travel through the gap
		t.add(ModLang.MSG_GAP_TRAVEL, "The Spatial Gap: each block here is %s in the world. Tear space again to step out.");
		t.add(ModLang.MSG_GAP_TRAVEL_SHALLOWS, "The shallows - the world beneath: %s, %s - %s blocks to the crossing");
		t.add(ModLang.MSG_GAP_TRAVEL_UPPER, "The upper layers - the world beneath: %s, %s - %s blocks to the crossing");
		t.add(ModLang.MSG_GAP_TRAVEL_LOWER, "The lower layers - the world beneath: %s, %s - %s blocks to the floor");
		t.add(ModLang.MSG_GAP_SETTLING, "Space settles about your way out...");
		t.add(ModLang.MSG_TEAR_COOLING, "Space hasn't settled from your last tear (%s s).");
		t.add(ModLang.MSG_TEAR_OPEN, "Space tears open before you. Step through it, and any with you, before it closes.");
		t.add(ModLang.MSG_TEAR_NOT_HERE, "Space here won't tear: only the mortal world and the Upper Realm open onto the gap.");
		t.add(ModLang.MSG_TEAR_NO_QI, "Tearing space open takes %s qi.");
		t.add(ModLang.MSG_TEAR_OUT, "You tear your way out of the gap at %s, %s.");
		t.add(ModLang.MSG_TEAR_OUT_UPPER, "You tear your way out into the Upper Realm at %s, %s.");
		t.add(ModLang.MSG_DAO_STELE_TEACHES, "This stele holds the %s (%s, to %s). Meditate beside it to learn it.");
		t.add(ModLang.MSG_DAO_STELE_KNOWN, "This stele holds the %s (%s, to %s), which you know. Meditating beside a stele of its element while you practise it is faster.");
		t.add(ModLang.MSG_DAO_STELE_READING, "You read the %s carved in the stele...");
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
				+ "seldom carved outside the Upper Realm.");
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
		// The Netherworld's dead: ghost cultivators, the Gate's keepers, Hungry Ghosts
		t.add(ModEntities.GHOST_CULTIVATOR, "Ghost Cultivator");
		t.add(ModItems.GHOST_CULTIVATOR_SPAWN_EGG, "Ghost Cultivator Spawn Egg");
		t.add(ModEntities.HUNGRY_GHOST, "Hungry Ghost");
		t.add(ModItems.HUNGRY_GHOST_SPAWN_EGG, "Hungry Ghost Spawn Egg");
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
		t.add(ModLang.DIMENSION_UPPER_REALM, "the Upper Realm");
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
		t.add(ModLang.MSG_PILL_NO_METHOD, "You know no ghost method for its yin to feed.");
		t.add(ModLang.MSG_PILL_YANG, "Yang warmth spreads through you: the Netherworld's yin is held still for %s minutes");
		t.add(ModLang.MSG_PILL_YIN, "+%s%% ghost method cultivation for %s minutes");
		t.add(ModLang.PILL_EFFECT_YANG, "Holds the Netherworld's yin still for %s minutes");
		t.add(ModLang.PILL_EFFECT_YIN, "+%s%% ghost method cultivation for %s minutes");
		t.add(ModItems.GHOST_QUELLING_TALISMAN, "Ghost-Quelling Talisman");
		t.add(ModLang.TOOLTIP_GHOST_QUELLING_TALISMAN, "Throw it at the dead: it strikes them and binds them where they stand for %s seconds");
		t.add(ModEntities.GHOST_QUELLING_TALISMAN, "Ghost-Quelling Talisman");
		t.add(ModEffects.QUELLED, "Quelled");
		t.add(ModLang.MSG_QUELLED, "A talisman seals your brow: you are bound where you stand!");
		t.add(ModLang.MSG_QUELLED_OTHER, "The talisman binds %s where they stand");
		t.add(ModLang.MSG_QUELLED_HELD, "The talisman holds you where you stand.");
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
		t.add("cultivation.defying-the-heavens.talk.line.gift.enough.commoner", "You've given me plenty today, Immortal. Keep it.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.enough.demonic", "Again? Keep your trinkets.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.enough.neutral", "You've given me enough of those today.");
		t.add("cultivation.defying-the-heavens.talk.line.gift.enough.righteous", "You are too generous. Keep it, friend.");
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
		t.add("cultivation.defying-the-heavens.talk.line.greet.demonic.peer", "Fellow Daoist. Tea? It isn't poisoned. Probably.");
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
		t.add("cultivation.defying-the-heavens.talk.possessed", "%s doesn't seem to hear you.");
		t.add(ModLang.TALK_UNSEEN, "%s looks straight through you, as if nobody were there.");
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
		t.add("cultivation.defying-the-heavens.talk.line.rumour.none.demonic", "Gossip? Go bore someone else.");
		t.add("cultivation.defying-the-heavens.talk.line.rumour.none.commoner", "Nothing, I fear. Quiet times.");
		t.add("cultivation.defying-the-heavens.talk.journal_noted", "Noted in your journal.");
		t.add("cultivation.defying-the-heavens.talk.line.guidance.righteous", "Breathe out slower than you breathe in, and let the qi settle at the dantian before you push. Sit with that.");
		t.add("cultivation.defying-the-heavens.talk.line.guidance.neutral", "Your circulation stumbles at the third meridian. Fix it.");
		t.add("cultivation.defying-the-heavens.talk.line.guidance.demonic", "You force your qi like a drunk forcing a door. Coax it. There: free advice.");
		t.add("cultivation.defying-the-heavens.talk.line.guidance.again.righteous", "That is enough for one day. Let it settle.");
		t.add("cultivation.defying-the-heavens.talk.line.guidance.again.neutral", "Enough for today.");
		t.add("cultivation.defying-the-heavens.talk.line.guidance.again.demonic", "Greedy. Come back tomorrow.");
		t.add("cultivation.defying-the-heavens.talk.insight", "%s's guidance: +%s%% cultivation from meditation, for %s minutes.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.terms.righteous", "A friendly bout? Gladly. Name your terms, and we keep to them.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.terms.neutral", "A spar. Terms?");
		t.add("cultivation.defying-the-heavens.talk.line.spar.terms.demonic", "You want to trade blows with me? Name your terms, and try not to weep.");
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
		t.add("cultivation.defying-the-heavens.spar.countdown", "%s...");
		t.add("cultivation.defying-the-heavens.spar.fight", "Begin!");
		t.add("cultivation.defying-the-heavens.talk.line.spar.won.righteous", "Well fought. I am bested, and gladly.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.won.neutral", "You win.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.won.demonic", "Tch. Lucky. Next time I won't be so kind.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.lost.righteous", "A good bout. Mind your footwork, and you'll have me next time.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.lost.neutral", "Again, when you're stronger.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.lost.demonic", "Pathetic. Come back when you can stand.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.forfeit.righteous", "You leave mid-bout? That is no way to spar.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.forfeit.neutral", "Walked off. Hm.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.forfeit.demonic", "Running already? I'll remember that.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.draw.righteous", "Neither yields. An honourable draw.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.draw.neutral", "Even. Enough for today.");
		t.add("cultivation.defying-the-heavens.talk.line.spar.draw.demonic", "We'll finish this another day.");
		t.add("cultivation.defying-the-heavens.talk.line.tribute.righteous", "Take it, then. The heavens see what you do.");
		t.add("cultivation.defying-the-heavens.talk.line.tribute.neutral", "Take it. Go.");
		t.add("cultivation.defying-the-heavens.talk.line.tribute.demonic", "Hah... take it. I won't forget your face.");
		t.add("cultivation.defying-the-heavens.talk.line.tribute.commoner", "Please, take it, take it all...");
		t.add("cultivation.defying-the-heavens.talk.line.tribute.again.righteous", "I have nothing more to give you today.");
		t.add("cultivation.defying-the-heavens.talk.line.tribute.again.neutral", "Nothing left.");
		t.add("cultivation.defying-the-heavens.talk.line.tribute.again.demonic", "You've bled me once today already.");
		t.add("cultivation.defying-the-heavens.talk.line.tribute.again.commoner", "There's nothing more, I swear it!");
		t.add("cultivation.defying-the-heavens.talk.line.release.righteous", "Senior is merciful. This one thanks you.");
		t.add("cultivation.defying-the-heavens.talk.line.release.neutral", "...Thank you.");
		t.add("cultivation.defying-the-heavens.talk.line.release.demonic", "Hah. Generous of you.");
		t.add("cultivation.defying-the-heavens.talk.line.release.commoner", "Thank you, immortal, thank you!");
		t.add("cultivation.defying-the-heavens.talk.line.spared.righteous", "This one owes you a life. I will not forget it.");
		t.add("cultivation.defying-the-heavens.talk.line.spared.neutral", "I owe you. I won't forget.");
		t.add("cultivation.defying-the-heavens.talk.line.spared.demonic", "...You let me live? Fool. Or perhaps not.");
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
		t.add("cultivation.defying-the-heavens.tab.journal", "Journal");
		t.add("cultivation.defying-the-heavens.journal.empty", "Nothing yet. Cultivators who think well enough of you (Courteous) will tell you rumours if you ask; what they tell you is noted here.");
		t.add("cultivation.defying-the-heavens.journal.entry", "Day %s, from %s");
		t.add("item.defying-the-heavens.low_spirit_stone", "Low-grade Spirit Stone");
		t.add("item.defying-the-heavens.mid_spirit_stone", "Mid-grade Spirit Stone");
		t.add("item.defying-the-heavens.high_spirit_stone", "High-grade Spirit Stone");
		t.add("item.defying-the-heavens.supreme_spirit_stone", "Supreme-grade Spirit Stone");
		t.add("item.defying-the-heavens.immortal_spirit_stone", "Immortal-grade Spirit Stone");
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
		t.add(ModLang.MSG_TREASURY_SEALED, "A seal of %s holds it shut. Only one a realm above its elders could break it.");
		t.add(ModLang.MSG_TREASURY_SEAL_BROKEN, "The elders' seal shatters under your hand. The treasury lies open.");
		// Players in sects (SectMembership)
		t.add(ModLang.MSG_SECT_JOINED, "You are now a disciple of the %s: %s.");
		t.add(ModLang.MSG_SECT_LEFT, "You have left the %s.");
		t.add(ModLang.MSG_SECT_EXPELLED, "You have been cast out of the %s: %s");
		t.add(ModLang.MSG_SECT_EXPELLED_TREASURY, "you laid hands on the elders' treasury again.");
		t.add(ModLang.MSG_SECT_EXPELLED_VIOLENCE, "you struck one of your own sect.");
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
				case SPIRIT -> "+%s%% qi pool";
				case INSIGHT -> "+%s%% cultivation, half in qi";
			};
			t.add(ModLang.STAT_POINT_PREFIX + stat.getId(), name);
			t.add(ModLang.STAT_POINT_PREFIX + stat.getId() + ".effect", effect);
		}
		t.add(ModLang.MSG_STAT_POINTS_RETURNED, "Your cultivation has fallen below what your stat points were spent from: every point is given back to spend again (Stats tab, Stat Points).");
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
		t.add(ModLang.talkLine("join.passed"), "Well fought. From today you are one of us. Our gates are open to you.");
		t.add(ModLang.talkLine("join.failed"), "Not yet. Train, and come back tomorrow.");
		t.add(ModLang.talkLine("leave.disciple"), "Only an elder can release you from the sect.");
		t.add(ModLang.talkLine("leave.done"), "So be it. You are no longer one of us. Go well.");
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
		t.add(ModLang.MSG_POSSESS_TOO_STRONG, "Too strong: you can only possess beings three realms below your own.");
		t.add(ModLang.MSG_POSSESS_QI, "Not enough qi: taking hold of a body costs %s%% of your pool.");
		t.add(ModLang.MSG_POSSESS_EJECTED, "The body you held is dead: your soul is thrown out, exposed.");
		t.add(ModLang.MSG_POSSESS_TETHER, "Your tether pulls your soul out of the body.");
		t.add(ModLang.HUD_SOUL_GATHERING, "Leaving your body... %s s");
		t.add(ModLang.HUD_SOUL_RECALLING, "Returning to your body... %s s");
		t.add(ModLang.HUD_SOUL_TETHER, "Tether %s / %s");
		t.add(ModLang.HUD_SOUL_BODY, "Body %s / %s");
		t.add(ModLang.HUD_SOUL_POSSESSING, "In %s (sneak to let go)");

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
	}

	/** A physique's name and description (Physique). */
	private static void physique(TranslationBuilder t, String id, String name, String description) {
		t.add(ModLang.physiqueKey(id), name);
		t.add(ModLang.physiqueKey(id) + ".description", description);
	}
}
