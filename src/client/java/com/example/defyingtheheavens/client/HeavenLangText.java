package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

/**
 * The heaven methods' English (HeavenMethod, the heaven spells, the five passives): their names, lore, what their layers bring,
 * and what each spell does. Generated from the design's spec sheet; kept apart from ModEnglishLangProvider for its length.
 */
final class HeavenLangText {
	static void add(FabricLanguageProvider.TranslationBuilder t) {
		// Green Sprout Breathing Manual
		t.add(ModLang.heavenMethodKey("green_sprout_breathing_manual"), "Green Sprout Breathing Manual");
		t.add(ModLang.heavenMethodKey("green_sprout_breathing_manual") + ".description", "Village hermits copied this primer from the margins of an old almanac. It teaches you to breathe the way a seed breathes when it splits its husk, drawing green qi up from the soil through the soles of the feet; humble and patient, it is the root every Wood art grows from.");
		t.add(ModLang.heavenMethodKey("green_sprout_breathing_manual") + ".gains", "Qi Refining, nine layers (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 3: Grasping Roots. Layer 7: Bramble Mantle. Every layer: Evergreen Vitality.");
		t.add(ModLang.spellKey("grasping_roots"), "Grasping Roots");
		t.add(ModLang.spellKey("grasping_roots") + ".description", "Roots burst from the earth under the first being you look at within 16 blocks. It takes 5 harm. If it is in the air (up to 12 blocks above the ground) it is hauled down, and then it is bound where it stands for 2.5 s, unable to walk or jump. Great bosses are bound for 1 s.");
		t.add(ModLang.spellKey("bramble_mantle"), "Bramble Mantle");
		t.add(ModLang.spellKey("bramble_mantle") + ".description", "For 12 s you wear a mantle of brambles. Melee blows against you deal 15% less harm, and each one tears its striker for 2 harm plus half the harm it dealt (at most 8) and snags it with Slowness for 1 s.");
		// Coiling Vine Scripture
		t.add(ModLang.heavenMethodKey("coiling_vine_scripture"), "Coiling Vine Scripture");
		t.add(ModLang.heavenMethodKey("coiling_vine_scripture") + ".description", "A recluse left it in a ruined mountain shrine after watching wild vines pull the shrine's walls down over forty springs. It teaches qi to climb, to coil and never to let go.");
		t.add(ModLang.heavenMethodKey("coiling_vine_scripture") + ".gains", "Qi Refining to Foundation Building, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 12: Wisteria Ascent. Layer 16: Twin Serpent Vines. Every layer: Evergreen Vitality.");
		t.add(ModLang.spellKey("wisteria_ascent"), "Wisteria Ascent");
		t.add(ModLang.spellKey("wisteria_ascent") + ".description", "Casts a flowering wisteria vine onto the block you look at, up to 28 blocks away, and hauls you to it, up walls, cliffs and ceilings alike. There you cling for up to 4 s (sneak to let go), and you take no fall harm for 3 s after letting go.");
		t.add(ModLang.spellKey("twin_serpent_vines"), "Twin Serpent Vines");
		t.add(ModLang.spellKey("twin_serpent_vines") + ".description", "Two serpent-vines whip the first being you look at within 12 blocks and the nearest other being within 10 blocks of it, for 8 harm each, and bind the two together for 8 s. Whenever they stray more than 4 blocks apart, the vines haul them back together, tearing each for 4 harm (at most once a second). With no other being near, the first is bound to the ground where it stands.");
		// Ancient Cypress Art
		t.add(ModLang.heavenMethodKey("ancient_cypress_art"), "Ancient Cypress Art");
		t.add(ModLang.heavenMethodKey("ancient_cypress_art") + ".description", "Carved into the bark of a cypress that outlived three dynasties, this art teaches the cultivator to grow ring upon ring and endure every storm, and to scatter thorns among its roots for whatever comes to fell it.");
		t.add(ModLang.heavenMethodKey("ancient_cypress_art") + ".gains", "Qi Refining to Core Formation, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 21: Bursting Thorn Pods. Layer 25: Heartwood of Nine Rings. Every layer: Evergreen Vitality.");
		t.add(ModLang.spellKey("bursting_thorn_pods"), "Bursting Thorn Pods");
		t.add(ModLang.spellKey("bursting_thorn_pods") + ".description", "Scatters five thorn pods on the ground around the spot you look at, within 20 blocks. They lie hidden for up to 20 s, and when a foe comes within 1.5 blocks of one it bursts: that foe takes 18 harm, everything else within 2.5 blocks takes 8, and all of them are slowed (Slowness III) for 2 s. At most ten of your pods lie at once.");
		t.add(ModLang.spellKey("heartwood_of_nine_rings"), "Heartwood of Nine Rings");
		t.add(ModLang.spellKey("heartwood_of_nine_rings") + ".description", "You root yourself where you stand for up to 9 s and cannot walk, be moved or be knocked back. Each second grows a ring (up to 9); each ring cuts the harm you take by 5% and heals you 0.5 health a second. When you uproot (cast again, or when the 9 s end), your next melee blow within 8 s strikes for 3 more harm per ring and hurls its target back.");
		// Primeval Forest Canon
		t.add(ModLang.heavenMethodKey("primeval_forest_canon"), "Primeval Forest Canon");
		t.add(ModLang.heavenMethodKey("primeval_forest_canon") + ".description", "The canon of a forest sect whose elders sat beneath the trees so long that none could say where their meridians ended and the roots began. Its cultivator becomes a whole wood, drowsy with blossom and bristling with thorns.");
		t.add(ModLang.heavenMethodKey("primeval_forest_canon") + ".gains", "Qi Refining to Nascent Soul, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 30: Pollen of Long Dreams. Layer 34: Myriad Thorn Thicket. Every layer: Evergreen Vitality.");
		t.add(ModLang.spellKey("pollen_of_long_dreams"), "Pollen of Long Dreams");
		t.add(ModLang.spellKey("pollen_of_long_dreams") + ".description", "Releases a drifting cloud of golden pollen, 4.5 blocks in radius, at the spot you look at within 24 blocks. It lingers for 4 s, and every foe who breathes it falls asleep for 6 s, unable to move, attack or notice you. Harm wakes a sleeper, and the blow that wakes it strikes for half again as much. A woken foe cannot be put back to sleep for 10 s. Great bosses are only slowed for 3 s.");
		t.add(ModLang.spellKey("myriad_thorn_thicket"), "Myriad Thorn Thicket");
		t.add(ModLang.spellKey("myriad_thorn_thicket") + ".description", "Raises a thicket of thorns 10 blocks around the spot you look at (within 24 blocks) for 10 s. Every being inside except you and your pets is slowed, and each one that moves is torn for 3 harm per block it travels (at most 16 a second). Whatever stands still is spared.");
		// Eternal Spring Sutra
		t.add(ModLang.heavenMethodKey("eternal_spring_sutra"), "Eternal Spring Sutra");
		t.add(ModLang.heavenMethodKey("eternal_spring_sutra") + ".description", "Recited in the peach gardens of the immortals, where spring never yields to autumn. Here life renews itself without pause and mends the wounds of all it shelters, and what it cannot grow it takes, as the strangler fig takes its host.");
		t.add(ModLang.heavenMethodKey("eternal_spring_sutra") + ".gains", "Qi Refining to Heavenly Being, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 39: Strangler Fig Seed. Layer 43: Rite of Returning Spring. Every layer: Evergreen Vitality.");
		t.add(ModLang.spellKey("strangler_fig_seed"), "Strangler Fig Seed");
		t.add(ModLang.spellKey("strangler_fig_seed") + ".description", "Shoots a strangler-fig seed into the first being you look at within 32 blocks: 65 harm, and for 10 s the fig feeds on it. The host is weakened (Weakness I), and every bit of healing it receives heals you instead, wherever you are within 64 blocks.");
		t.add(ModLang.spellKey("rite_of_returning_spring"), "Rite of Returning Spring");
		t.add(ModLang.spellKey("rite_of_returning_spring") + ".description", "Blesses you and every non-hostile being within 24 blocks for 20 s. Each of them at once regains 30 health, and of every harm a blessed being takes, 60% grows back as healing over the next 3 s.");
		// Heaven-Bearing World Tree Classic
		t.add(ModLang.heavenMethodKey("heaven_bearing_world_tree_classic"), "Heaven-Bearing World Tree Classic");
		t.add(ModLang.heavenMethodKey("heaven_bearing_world_tree_classic") + ".description", "The root-script of the tree that stood where the four axes meet before heaven and earth were parted, bearing the sky upon its branches. Whoever completes it carries a world within their trunk, and all beneath its shade are kept.");
		t.add(ModLang.heavenMethodKey("heaven_bearing_world_tree_classic") + ".gains", "Qi Refining to Four Axis, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 48: Heaven-Shading Canopy. Layer 52: The World Tree Rises. Every layer: Evergreen Vitality.");
		t.add(ModLang.spellKey("heaven_shading_canopy"), "Heaven-Shading Canopy");
		t.add(ModLang.spellKey("heaven_shading_canopy") + ".description", "For 8 s the World Tree's canopy spreads 32 blocks around you. Every arrow, fireball or other missile within it that was not loosed by you or your allies is caught in its leaves: arrows and tridents drop harmless to the ground, and everything else is snuffed out. Each missile caught gives you back 20 qi.");
		t.add(ModLang.spellKey("world_tree_rises"), "The World Tree Rises");
		t.add(ModLang.spellKey("world_tree_rises") + ".description", "Plants the World Tree at the spot you look at (within 32 blocks). For 30 s it towers 48 blocks high with a canopy 28 blocks around. Allies beneath it regain 12 health each second. Foes beneath it cannot heal, and every 3 s its roots erupt under up to 8 of them (nearest the trunk first) for 60 harm each, tossing them into the air.");
		// Morning Dew Gathering Art
		t.add(ModLang.heavenMethodKey("morning_dew_gathering_art"), "Morning Dew Gathering Art");
		t.add(ModLang.heavenMethodKey("morning_dew_gathering_art") + ".description", "A humble breathing art of river hermits. You sit among wet grass before sunrise and draw in the cold qi that settles with the dew, until your meridians run as clear as a mountain brook.");
		t.add(ModLang.heavenMethodKey("morning_dew_gathering_art") + ".gains", "Qi Refining, nine layers (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 3: Hoarfrost Needle. Layer 7: Morning Mist Shroud. Every layer: Ceaseless Flow.");
		t.add(ModLang.spellKey("hoarfrost_needle"), "Hoarfrost Needle");
		t.add(ModLang.spellKey("hoarfrost_needle") + ".description", "Flicks a needle of hoarfrost up to 24 blocks. The first being struck takes 6 harm and gains a Frost stack (Slowness I per stack, lasting 10 s). At 3 stacks it freezes solid for 2 s and cannot move or jump.");
		t.add(ModLang.spellKey("morning_mist_shroud"), "Morning Mist Shroud");
		t.add(ModLang.spellKey("morning_mist_shroud") + ".description", "Breathes out a bank of morning mist 5 blocks around you for 6 s. Creatures lose track of anyone inside the mist who is more than 3 blocks from them. Foe players inside are blinded. While you stay in the mist you are invisible and swift (Speed I). Striking or casting ends your invisibility but not the mist.");
		// Clear Fount Cleansing Manual
		t.add(ModLang.heavenMethodKey("clear_fount_cleansing_manual"), "Clear Fount Cleansing Manual");
		t.add(ModLang.heavenMethodKey("clear_fount_cleansing_manual") + ".description", "Written beside a fount that never froze and never failed. It teaches qi to rise from deep in the body as spring water rises from the rock, washing every impurity away as it climbs.");
		t.add(ModLang.heavenMethodKey("clear_fount_cleansing_manual") + ".gains", "Qi Refining to Foundation Building, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 12: Returning Current. Layer 16: Renewing Mountain Rain. Every layer: Ceaseless Flow.");
		t.add(ModLang.spellKey("returning_current"), "Returning Current");
		t.add(ModLang.spellKey("returning_current") + ".description", "Leaves a spring where you stand. After 5 s, or sooner if you cast it again, the current carries you back to it, gives back half the health you have lost since, and puts out any fire on you. You take no fall harm for 2 s after.");
		t.add(ModLang.spellKey("renewing_mountain_rain"), "Renewing Mountain Rain");
		t.add(ModLang.spellKey("renewing_mountain_rain") + ".description", "Calls a soft mountain rain 6 blocks around you for 10 s. You and your allies in it regain 1 health each second and are put out if burning, and while you stand in it, your spells' cooldowns (and your allies') run down twice as fast.");
		// Nine-Bend River Record
		t.add(ModLang.heavenMethodKey("nine_bend_river_record"), "Nine-Bend River Record");
		t.add(ModLang.heavenMethodKey("nine_bend_river_record") + ".description", "A record of the great river that turns nine times on its way to the sea, never contending with the mountains yet wearing them all away. From it the cultivator learns to bend, to coil and to carry.");
		t.add(ModLang.heavenMethodKey("nine_bend_river_record") + ".gains", "Qi Refining to Core Formation, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 21: Coiling Stream Lash. Layer 25: Nine-Bend Whirlpool. Every layer: Ceaseless Flow.");
		t.add(ModLang.spellKey("coiling_stream_lash"), "Coiling Stream Lash");
		t.add(ModLang.spellKey("coiling_stream_lash") + ".description", "Lashes a whip of water out up to 16 blocks. It coils round the first being it touches, swings it over your head and dashes it down 6 blocks behind you for 22 harm and Slowness III for 2 s.");
		t.add(ModLang.spellKey("nine_bend_whirlpool"), "Nine-Bend Whirlpool");
		t.add(ModLang.spellKey("nine_bend_whirlpool") + ".description", "Opens a whirlpool 5 blocks in radius at the spot you look at (up to 24 blocks) for 5 s. Beings caught in it are swept round and round, held from escaping, and take 2 harm each second. When it collapses they are flung out for 6 more harm.");
		// Moon-Mirror Lake Sutra
		t.add(ModLang.heavenMethodKey("moon_mirror_lake_sutra"), "Moon-Mirror Lake Sutra");
		t.add(ModLang.heavenMethodKey("moon_mirror_lake_sutra") + ".description", "Composed by a recluse who sat for a hundred years beside a lake so still that it held the moon twice. The sutra teaches the heart to become still water, reflecting all things and holding none.");
		t.add(ModLang.heavenMethodKey("moon_mirror_lake_sutra") + ".gains", "Qi Refining to Nascent Soul, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 30: Moon in the Water. Layer 34: Still Lake Mirror. Every layer: Ceaseless Flow.");
		t.add(ModLang.spellKey("moon_in_the_water"), "Moon in the Water");
		t.add(ModLang.spellKey("moon_in_the_water") + ".description", "Like the moon and its reflection, you and the first being you look at (up to 32 blocks) change places in an instant. The one you swap with is left reeling (Slowness II for 2 s; a player is also dizzied for 3 s). You take no fall harm for 2 s.");
		t.add(ModLang.spellKey("still_lake_mirror"), "Still Lake Mirror");
		t.add(ModLang.spellKey("still_lake_mirror") + ".description", "For 8 s you become a still lake that mirrors every blow. All harm you take is halved, and the mirror remembers who dealt it. When the 8 s end, every being that harmed you (if within 32 blocks) is struck by its own reflection for twice the harm the mirror spared you from it, up to 50 each.");
		// Heavenly River Scripture
		t.add(ModLang.heavenMethodKey("heavenly_river_scripture"), "Heavenly River Scripture");
		t.add(ModLang.heavenMethodKey("heavenly_river_scripture") + ".description", "Said to be copied from the starry river that divides the sky. It teaches qi to fall from the heavens with the whole weight of the celestial current behind it, and to drown whatever it closes over.");
		t.add(ModLang.heavenMethodKey("heavenly_river_scripture") + ".gains", "Qi Refining to Heavenly Being, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 39: Abyssal Water Prison. Layer 43: Overturned Heavenly River. Every layer: Ceaseless Flow.");
		t.add(ModLang.spellKey("abyssal_water_prison"), "Abyssal Water Prison");
		t.add(ModLang.spellKey("abyssal_water_prison") + ".description", "A globe of abyssal water closes over the first being you look at within 32 blocks: 50 harm, and for 5 s it drowns in the globe, its breath gone, losing 8 health each second, and is slowed (Slowness II). A being that breathes water or needs no air takes only the first harm and the slowness.");
		t.add(ModLang.spellKey("overturned_heavenly_river"), "Overturned Heavenly River");
		t.add(ModLang.spellKey("overturned_heavenly_river") + ".description", "Tips the Heavenly River onto the earth: a wall of water 15 blocks wide and 5 high rolls 32 blocks ahead of you. Everything it meets takes 8 harm and is carried along inside it. Where it ends, or breaks against a cliff, it crashes down: 32 harm, a toss upward and Slowness II for 4 s.");
		// Boundless Four Seas Canon
		t.add(ModLang.heavenMethodKey("boundless_four_seas_canon"), "Boundless Four Seas Canon");
		t.add(ModLang.heavenMethodKey("boundless_four_seas_canon") + ".description", "The canon of the waters that ring the world at its four axes, from which every river comes and to which every river returns. At its end lies the abyss of the north, where even time stands frozen.");
		t.add(ModLang.heavenMethodKey("boundless_four_seas_canon") + ".gains", "Qi Refining to Four Axis, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 48: Ten Thousand Reflections. Layer 52: Eternal Winter Descends. Every layer: Ceaseless Flow.");
		t.add(ModLang.spellKey("ten_thousand_reflections"), "Ten Thousand Reflections");
		t.add(ModLang.spellKey("ten_thousand_reflections") + ".description", "Stills the ground for 20 blocks around you into a mirror lake in which every foe sees enemies in its own reflection. For 6 s each foe creature attacks the nearest other foe instead of you or any player, and blows they land on one another deal 50% more harm. Foe players are only dizzied (Nausea, 4 s).");
		t.add(ModLang.spellKey("eternal_winter_descends"), "Eternal Winter Descends");
		t.add(ModLang.spellKey("eternal_winter_descends") + ".description", "The winter of the northern abyss falls for 24 blocks around you. After 1 s, everything within it is frozen still for 6 s (players and great beasts for 3 s), and arrows and other missiles hang in the air. Harm dealt to the frozen is held in the ice. When the ice shatters, each takes 60 harm plus one and a half times the harm held (up to 140 more; a player at most half its health).");
		// Ember Kindling Art
		t.add(ModLang.heavenMethodKey("ember_kindling_art"), "Ember Kindling Art");
		t.add(ModLang.heavenMethodKey("ember_kindling_art") + ".description", "A hearth-keeper's method handed down in mountain villages. Breath is banked in the dantian like coals under ash at night and fanned again at dawn, so that even the smallest ember of qi never goes out.");
		t.add(ModLang.heavenMethodKey("ember_kindling_art") + ".gains", "Qi Refining, nine layers (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 3: Sleeping Ember. Layer 7: Flash of Struck Flint. Every layer: Fury of the Fanned Flame.");
		t.add(ModLang.spellKey("sleeping_ember"), "Sleeping Ember");
		t.add(ModLang.spellKey("sleeping_ember") + ".description", "Flicks a sleeping ember into the first being within 16 blocks. After 3 s it wakes and bursts: that being takes 7 harm and burns for 3 s, and everything else within 2.5 blocks takes 3 harm and burns for 2 s. Strike the marked being in melee and the ember bursts at once.");
		t.add(ModLang.spellKey("struck_flint_flash"), "Flash of Struck Flint");
		t.add(ModLang.spellKey("struck_flint_flash") + ".description", "Strikes a flash of light from your palms. Every foe within 12 blocks that is looking at you is blinded and slowed for 4 s, and creatures among them lose sight of you for 3 s. Foes looking away see nothing.");
		// Furnace Heart Manual
		t.add(ModLang.heavenMethodKey("furnace_heart_manual"), "Furnace Heart Manual");
		t.add(ModLang.heavenMethodKey("furnace_heart_manual") + ".description", "The foundation is laid as the floor of a furnace beneath the heart. Breath works the bellows, each layer tempers the furnace walls, and whatever is thrown into its fire is refined, not lost.");
		t.add(ModLang.heavenMethodKey("furnace_heart_manual") + ".gains", "Qi Refining to Foundation Building, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 12: Fire Horse Gallop. Layer 16: Sealed Furnace Refinement. Every layer: Fury of the Fanned Flame.");
		t.add(ModLang.spellKey("fire_horse_gallop"), "Fire Horse Gallop");
		t.add(ModLang.spellKey("fire_horse_gallop") + ".description", "For 6 s you run like a fire horse, with Speed III and Haste II, and your hoofprints burn for 3 s. Any other being that treads on a print takes 2 harm and catches fire for 3 s (at most once a second and 3 times in all). Plunging into water ends the gallop.");
		t.add(ModLang.spellKey("sealed_furnace_refinement"), "Sealed Furnace Refinement");
		t.add(ModLang.spellKey("sealed_furnace_refinement") + ".description", "Seals your body like a furnace for 12 s. Harm you take is cut by 35%, harm from fire, lava and burning by 100%, and every point of harm held back is refined into 3 qi (up to 150 qi in all).");
		// Crimson Crucible Classic
		t.add(ModLang.heavenMethodKey("crimson_crucible_classic"), "Crimson Crucible Classic");
		t.add(ModLang.heavenMethodKey("crimson_crucible_classic") + ".description", "Written by an alchemist who held that a golden core is only ore until it is smelted. The body becomes a crucible, and the core is poured from fire refined nine times over, until it blooms like a lotus in the flame.");
		t.add(ModLang.heavenMethodKey("crimson_crucible_classic") + ".gains", "Qi Refining to Core Formation, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 21: Prairie-Burning Spark. Layer 25: Crimson Lotus Blooms. Every layer: Fury of the Fanned Flame.");
		t.add(ModLang.spellKey("prairie_burning_spark"), "Prairie-Burning Spark");
		t.add(ModLang.spellKey("prairie_burning_spark") + ".description", "Throws a spark at the first being within 24 blocks. It burns with crimson fire for 8 s, taking 2.5 harm a second on top of ordinary burning. Each second the fire leaps from every burning being to up to 2 others within 4 blocks, who burn for 5 s. Up to 12 beings can catch it; water puts it out.");
		t.add(ModLang.spellKey("crimson_lotus_blooms"), "Crimson Lotus Blooms");
		t.add(ModLang.spellKey("crimson_lotus_blooms") + ".description", "Plants a bud of crimson fire at the spot you look at, within 24 blocks. After 1.5 s it opens: everything within 2 blocks of its heart takes 20 harm, and twelve petals of fire fly out along the ground for 12 blocks, each striking the first being it meets for 14 harm (a being once) and setting it burning for 4 s.");
		// Vermilion Bird Sutra
		t.add(ModLang.heavenMethodKey("vermilion_bird_sutra"), "Vermilion Bird Sutra");
		t.add(ModLang.heavenMethodKey("vermilion_bird_sutra") + ".description", "The Nascent Soul is hatched in the fire of the south and takes the shape of the Vermilion Bird, guardian of the summer quarter of the sky. Its talons sear what they touch, and its descent sets the plains alight.");
		t.add(ModLang.heavenMethodKey("vermilion_bird_sutra") + ".gains", "Qi Refining to Nascent Soul, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 30: Ash-Searing Talon. Layer 34: Vermilion Bird Plunge. Every layer: Fury of the Fanned Flame.");
		t.add(ModLang.spellKey("ash_searing_talon"), "Ash-Searing Talon");
		t.add(ModLang.spellKey("ash_searing_talon") + ".description", "Rakes the first being within 28 blocks with a burning talon. It takes 36 harm and burns for 6 s, and ash sears the wound shut: for 10 s it cannot regain health by any means.");
		t.add(ModLang.spellKey("vermilion_bird_plunge"), "Vermilion Bird Plunge");
		t.add(ModLang.spellKey("vermilion_bird_plunge") + ".description", "Spreads vermilion wings and soars about 20 blocks into the sky, then glides down for up to 6 s, steering where you look; sneak to stoop at once. Where you land, everything within 6 blocks takes 22 harm, more the further you fell (up to 30), is hurled away and burns for 5 s. You take no fall harm.");
		// Golden Crow Great Sun Scripture
		t.add(ModLang.heavenMethodKey("golden_crow_great_sun_scripture"), "Golden Crow Great Sun Scripture");
		t.add(ModLang.heavenMethodKey("golden_crow_great_sun_scripture") + ".description", "Said to be copied from the marks the three-legged golden crow left on the great mulberry of the eastern sea before it carried the sun across the sky. Its practitioners learn to bear a sun within them, as bright as noon.");
		t.add(ModLang.heavenMethodKey("golden_crow_great_sun_scripture") + ".gains", "Qi Refining to Heavenly Being, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 39: Light of First Dawn. Layer 43: Noonday Golden Crow. Every layer: Fury of the Fanned Flame.");
		t.add(ModLang.spellKey("light_of_first_dawn"), "Light of First Dawn");
		t.add(ModLang.spellKey("light_of_first_dawn") + ".description", "Calls up the light of first dawn over 20 blocks around you. You and your allies there are cleansed of every harmful effect and of burning, and healed 6 for each effect burned away. For 6 s afterwards, no new harmful effect can take hold of them.");
		t.add(ModLang.spellKey("noonday_golden_crow"), "Noonday Golden Crow");
		t.add(ModLang.spellKey("noonday_golden_crow") + ".description", "Hangs a golden-crow sun 7 blocks above you for 12 s. Each second, every other being within 24 blocks that the sun can see (cover hides you from it) takes 4 harm, burns for 3 s and glows; the undead take 6 instead.");
		// Heaven-Burning Phoenix Nirvana Canon
		t.add(ModLang.heavenMethodKey("heaven_burning_phoenix_nirvana_canon"), "Heaven-Burning Phoenix Nirvana Canon");
		t.add(ModLang.heavenMethodKey("heaven_burning_phoenix_nirvana_canon") + ".description", "The highest fire under the four axes of heaven, the fire of the phoenix that burns itself to ash and rises again. Its masters set storms of flame loose across the sky and, once in a long while, refuse death itself.");
		t.add(ModLang.heavenMethodKey("heaven_burning_phoenix_nirvana_canon") + ".gains", "Qi Refining to Four Axis, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 48: Three Heaven-Scorching Firestorms. Layer 52: Undying Phoenix. Every layer: Fury of the Fanned Flame.");
		t.add(ModLang.spellKey("heaven_scorching_firestorms"), "Three Heaven-Scorching Firestorms");
		t.add(ModLang.spellKey("heaven_scorching_firestorms") + ".description", "Looses three firestorms that roam for 8 s, each hunting a different foe within 32 blocks. Every being caught in a storm takes 7 harm twice a second, burns for 4 s and is lifted and spun; a foe hunted the whole time takes up to 112.");
		t.add(ModLang.spellKey("undying_phoenix"), "Undying Phoenix");
		t.add(ModLang.spellKey("undying_phoenix") + ".description", "Wreathes you in phoenix fire for 60 s. If you would die in that time, you burn to ash instead and rise 2 s later with half your health, cleansed of every harmful effect, with Strength II, Regeneration III and Fire Resistance for 15 s, as an explosion of phoenix fire deals 80 harm to everything within 16 blocks, burns it for 8 s and hurls it away. If the wreath goes unused, the spell is ready again 90 s after casting.");
		// Rammed Earth Manual
		t.add(ModLang.heavenMethodKey("rammed_earth_manual"), "Rammed Earth Manual");
		t.add(ModLang.heavenMethodKey("rammed_earth_manual") + ".description", "A village builder's manual, its pages stiff with dried clay. It teaches the body to take in the soil's qi one layer at a time, packed hard, the way earth is rammed between boards to raise a wall.");
		t.add(ModLang.heavenMethodKey("rammed_earth_manual") + ".gains", "Qi Refining, nine layers (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 3: Rolling Millstone. Layer 7: Rammed Earth Rampart. Every layer: Unshaken Mountain Body.");
		t.add(ModLang.spellKey("rolling_millstone"), "Rolling Millstone");
		t.add(ModLang.spellKey("rolling_millstone") + ".description", "Sets a millstone of packed earth rolling along the ground the way you face, for up to 16 blocks. It climbs one-block steps and rolls down slopes. Every being it runs over takes 4 harm and is shoved about 2 blocks on ahead of it.");
		t.add(ModLang.spellKey("rammed_earth_rampart"), "Rammed Earth Rampart");
		t.add(ModLang.spellKey("rammed_earth_rampart") + ".description", "Raises a rampart of rammed earth 5 blocks wide and 3 high, 3 blocks in front of you, row by row. It stands for 8 seconds and stops beings and arrows alike, then crumbles away. Anything standing where it rises is shoved out of the way first.");
		// Thousand-Layer Stone Record
		t.add(ModLang.heavenMethodKey("thousand_layer_stone_record"), "Thousand-Layer Stone Record");
		t.add(ModLang.heavenMethodKey("thousand_layer_stone_record") + ".description", "Copied from a cliff face by a hermit who read the strata like the pages of a book. It records stone laid on stone over ten thousand years, each layer pressing the one beneath into something harder.");
		t.add(ModLang.heavenMethodKey("thousand_layer_stone_record") + ".gains", "Qi Refining to Foundation Building, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 12: Stone Scale Armour. Layer 16: Swallowing Sands. Every layer: Unshaken Mountain Body.");
		t.add(ModLang.spellKey("stone_scale_armour"), "Stone Scale Armour");
		t.add(ModLang.spellKey("stone_scale_armour") + ".description", "Clads you in three scales of layered stone for 20 seconds. Each blow that strikes you breaks one scale: a blow of 10 harm or less is stopped entirely, and a heavier one is lessened by 10. Casting again restores all three.");
		t.add(ModLang.spellKey("swallowing_sands"), "Swallowing Sands");
		t.add(ModLang.spellKey("swallowing_sands") + ".description", "Turns the ground to quicksand in a circle 4.5 blocks around the point you look at (up to 24 blocks) for 6 seconds. Beings in it cannot jump, and they sink deeper for every second they stay. They are slowed from Slowness I up to V and take half a point of harm per depth each second, up to 10 harm in all. Climbing out ends the sinking.");
		// Five Sacred Peaks Classic
		t.add(ModLang.heavenMethodKey("five_sacred_peaks_classic"), "Five Sacred Peaks Classic");
		t.add(ModLang.heavenMethodKey("five_sacred_peaks_classic") + ".description", "The five sacred mountains stand at the four quarters and the centre of the land. This classic teaches the cultivator to gather their weight into a single core that nothing under heaven can shift.");
		t.add(ModLang.heavenMethodKey("five_sacred_peaks_classic") + ".gains", "Qi Refining to Core Formation, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 21: Earth-Splitting Fissure. Layer 25: Mount Tai Presses Down. Every layer: Unshaken Mountain Body.");
		t.add(ModLang.spellKey("earth_splitting_fissure"), "Earth-Splitting Fissure");
		t.add(ModLang.spellKey("earth_splitting_fissure") + ".description", "Splits the earth in a line 20 blocks long the way you face. After 1 s the fissure erupts along its length: everything standing on it takes 22 harm and is thrown into the air. A wall two blocks high stops it.");
		t.add(ModLang.spellKey("mount_tai_presses_down"), "Mount Tai Presses Down");
		t.add(ModLang.spellKey("mount_tai_presses_down") + ".description", "Lays the weight of Mount Tai on the first being you look at, up to 32 blocks away, for 6 seconds. It cannot jump, rise or fly, is slowed (Slowness III), and is hurled to the ground. When it meets the ground it is crushed for 18 harm, plus 1 for each block it fell (up to 30 in all).");
		// Yellow Dragon Earth-Vein Canon
		t.add(ModLang.heavenMethodKey("yellow_dragon_earth_vein_canon"), "Yellow Dragon Earth-Vein Canon");
		t.add(ModLang.heavenMethodKey("yellow_dragon_earth_vein_canon") + ".description", "Beneath every mountain run the earth's veins, and in them sleeps the Yellow Dragon of the Centre. This canon teaches the nascent soul to swim those veins beside it, and to wake it.");
		t.add(ModLang.heavenMethodKey("yellow_dragon_earth_vein_canon") + ".gains", "Qi Refining to Nascent Soul, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 30: Earth Dragon Turns Over. Layer 34: Earth-Vein Guardian. Every layer: Unshaken Mountain Body.");
		t.add(ModLang.spellKey("earth_dragon_turns_over"), "Earth Dragon Turns Over");
		t.add(ModLang.spellKey("earth_dragon_turns_over") + ".description", "Wakes an earth dragon that dives into the ground and hunts the being you look at (or, if you look at none, the nearest foe within 24 blocks) for up to 8 seconds, burrowing at 8 blocks a second. When it comes up beneath its quarry it strikes for 40 harm and leaves it reeling: slowed (Slowness II) and dazed (Nausea) for 3 s. A quarry high above the ground is waited for until it comes down.");
		t.add(ModLang.spellKey("earth_vein_guardian"), "Earth-Vein Guardian");
		t.add(ModLang.spellKey("earth_vein_guardian") + ".description", "Raises the guardian of the earth vein, a great stone head, at the spot you look at within 24 blocks, for 12 s. Every 2 s it bites the nearest foe within 10 blocks of it for 24 harm and drags it 2 blocks toward itself.");
		// Sovereign Earth Myriad-Bearing Sutra
		t.add(ModLang.heavenMethodKey("sovereign_earth_myriad_bearing_sutra"), "Sovereign Earth Myriad-Bearing Sutra");
		t.add(ModLang.heavenMethodKey("sovereign_earth_myriad_bearing_sutra") + ".description", "Sovereign Earth carries every mountain, river and living thing and never once complains of the weight. Those who recite this sutra learn to bear what others cannot, and to become the ground beneath their companions' feet.");
		t.add(ModLang.heavenMethodKey("sovereign_earth_myriad_bearing_sutra") + ".gains", "Qi Refining to Heavenly Being, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 39: Bedrock Bears the Blows. Layer 43: Shoulder the Myriad Burdens. Every layer: Unshaken Mountain Body.");
		t.add(ModLang.spellKey("bedrock_bears_the_blows"), "Bedrock Bears the Blows");
		t.add(ModLang.spellKey("bedrock_bears_the_blows") + ".description", "For 8 seconds no harm lands on you at once; the bedrock takes it first. Four tenths of every hurt sink into the ground and are gone. The other six tenths reach you slowly, a quarter of it each second over the next 4 seconds.");
		t.add(ModLang.spellKey("shoulder_the_myriad_burdens"), "Shoulder the Myriad Burdens");
		t.add(ModLang.spellKey("shoulder_the_myriad_burdens") + ".description", "For 15 seconds you carry the burdens of every ally within 16 blocks. Every hurt that befalls one of them lands at half its weight; the other half comes to you, and your stone body lets only half of that through. The bond breaks if you fall to a quarter of your health.");
		// Four Pillars Upholding Heaven Scripture
		t.add(ModLang.heavenMethodKey("four_pillars_upholding_heaven_scripture"), "Four Pillars Upholding Heaven Scripture");
		t.add(ModLang.heavenMethodKey("four_pillars_upholding_heaven_scripture") + ".description", "When Mount Buzhou broke and the sky tilted, four pillars were raised at the ends of the world to hold heaven up again. This scripture is found only above the clouds, and is said to be cut into the stone of those pillars themselves.");
		t.add(ModLang.heavenMethodKey("four_pillars_upholding_heaven_scripture") + ".gains", "Qi Refining to Four Axis, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 48: Riding Ten Thousand Dunes. Layer 52: Fall of Buzhou. Every layer: Unshaken Mountain Body.");
		t.add(ModLang.spellKey("riding_ten_thousand_dunes"), "Riding Ten Thousand Dunes");
		t.add(ModLang.spellKey("riding_ten_thousand_dunes") + ".description", "A river of yellow sand carries you and every ally within 5 blocks along the ground the way you look, at 25 blocks a second for up to 5 seconds, over slopes and water alike. Sneak to set down. Foes in its path are trampled for 50 harm and half-buried (Slowness IV for 2 s). No one it carries takes fall harm.");
		t.add(ModLang.spellKey("fall_of_buzhou"), "Fall of Buzhou");
		t.add(ModLang.spellKey("fall_of_buzhou") + ".description", "Calls down Mount Buzhou, the broken pillar of heaven, on the point you look at, up to 64 blocks away. Its shadow spreads 14 blocks around that point for 3 seconds as it falls. Then every being in the shadow but you is struck for a fifth of its own greatest health (at least 50, at most 130) and left half-buried, slowed (Slowness IV) and sapped (Mining Fatigue III) for 5 s. Beings 14 to 22 blocks out are thrown outward by the quake.");
		// Whetstone Edge Manual
		t.add(ModLang.heavenMethodKey("whetstone_edge_manual"), "Whetstone Edge Manual");
		t.add(ModLang.heavenMethodKey("whetstone_edge_manual") + ".description", "A smith's apprentice's manual, its pages black with forge soot: grind your qi as a whetstone grinds iron and hang it about the body like an iron bell, until one thread of it grows keen enough to fly out and come home.");
		t.add(ModLang.heavenMethodKey("whetstone_edge_manual") + ".gains", "Qi Refining, nine layers (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 3: Swallow-Return Thread. Layer 7: Iron Bell Cover. Every layer: Whetted Edge.");
		t.add(ModLang.spellKey("swallow_return_thread"), "Swallow-Return Thread");
		t.add(ModLang.spellKey("swallow_return_thread") + ".description", "Flicks a thread of sword qi up to 14 blocks along your gaze. It pierces every being in its path for 4 harm, then turns like a swallow and flies back to you, piercing again for 4 on the way home (up to 8 to a being struck both ways).");
		t.add(ModLang.spellKey("iron_bell_cover"), "Iron Bell Cover");
		t.add(ModLang.spellKey("iron_bell_cover") + ".description", "Hangs a bell of iron qi over you for 12 s: every blow struck by a being or its projectile harms you 2 less. A blow of 2 or less is stopped outright, and the bell rings.");
		// Hundred-Forged Lodestone Art
		t.add(ModLang.heavenMethodKey("hundred_forged_lodestone_art"), "Hundred-Forged Lodestone Art");
		t.add(ModLang.heavenMethodKey("hundred_forged_lodestone_art") + ".description", "The foundation is folded a hundred times like sword steel and laid about a lodestone heart. Iron leans toward its practitioner, and the weapons of others grow heavy in their hands.");
		t.add(ModLang.heavenMethodKey("hundred_forged_lodestone_art") + ".gains", "Qi Refining to Foundation Building, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 12: Lodestone Brand. Layer 16: Hundred Weapons Kneel. Every layer: Whetted Edge.");
		t.add(ModLang.spellKey("lodestone_brand"), "Lodestone Brand");
		t.add(ModLang.spellKey("lodestone_brand") + ".description", "Brands the first being you look at within 24 blocks with lodestone qi: it takes 10 harm, and for 10 s every arrow, trident or other missile flying within 8 blocks of it bends toward it and strikes it.");
		t.add(ModLang.spellKey("hundred_weapons_kneel"), "Hundred Weapons Kneel");
		t.add(ModLang.spellKey("hundred_weapons_kneel") + ".description", "Your lodestone heart wrenches at every foe within 10 blocks (other players too): each takes 8 harm and for 5 s strikes only half as hard. Monsters and beasts have their held weapons torn away (gear they spawned with shatters; anything they picked up falls to the ground) and cannot pick anything up for 8 s; a player's held items seize up for 5 s instead (no drawing bows, raising shields or throwing).");
		// Golden Thunder Core Sutra
		t.add(ModLang.heavenMethodKey("golden_thunder_core_sutra"), "Golden Thunder Core Sutra");
		t.add(ModLang.heavenMethodKey("golden_thunder_core_sutra") + ".description", "Left in a cave beneath the Thunder Peaks by a sword cultivator who let heaven's lightning hammer his core like an anvil until it rang gold. Its practitioners point and armour splits; they open their hand and the thunder leaps.");
		t.add(ModLang.heavenMethodKey("golden_thunder_core_sutra") + ".gains", "Qi Refining to Core Formation, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 21: Armour-Splitting Finger. Layer 25: Leaping Golden Lightning. Every layer: Whetted Edge.");
		t.add(ModLang.spellKey("armour_splitting_finger"), "Armour-Splitting Finger");
		t.add(ModLang.spellKey("armour_splitting_finger") + ".description", "Points a finger of golden qi at the first being within 24 blocks: it takes 20 harm and its armour cracks for 10 s. Each crack stage strips 20% of its armour and makes it take 5% more harm; the finger opens one stage and every later blow it takes from anyone widens the crack by another, up to 5 stages (no armour left, +25% harm).");
		t.add(ModLang.spellKey("leaping_golden_lightning"), "Leaping Golden Lightning");
		t.add(ModLang.spellKey("leaping_golden_lightning") + ".description", "Golden lightning leaps from your hand to the first being within 28 blocks for 26 harm, then from each struck being to the nearest one not yet struck within 7 blocks, up to 5 more leaps (18, 16, 15, 13, 12 harm). Metal carries water: from a being standing in water the lightning leaps 14 blocks.");
		// Nine Bells Sword-Soul Classic
		t.add(ModLang.heavenMethodKey("nine_bells_sword_soul_classic"), "Nine Bells Sword-Soul Classic");
		t.add(ModLang.heavenMethodKey("nine_bells_sword_soul_classic") + ".description", "The nascent soul is cast like a great bell with nine swords hung inside it. When it tolls, every blade answers, and all who hear it share one another's wounds.");
		t.add(ModLang.heavenMethodKey("nine_bells_sword_soul_classic") + ".gains", "Qi Refining to Nascent Soul, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 30: Nine Circling Blades. Layer 34: Thousand-Echo Toll. Every layer: Whetted Edge.");
		t.add(ModLang.spellKey("nine_circling_blades"), "Nine Circling Blades");
		t.add(ModLang.spellKey("nine_circling_blades") + ".description", "Nine small flying blades circle you for 40 s. Whenever a foe comes within 5 blocks, the nearest blade darts out and strikes it for 26 harm with a slight knockback, then is spent. Recasting restores all nine.");
		t.add(ModLang.spellKey("thousand_echo_toll"), "Thousand-Echo Toll");
		t.add(ModLang.spellKey("thousand_echo_toll") + ".description", "Your soul tolls like a great bell: every foe within 16 blocks (other players too) takes 22 harm and resonates for 10 s. Whenever a resonating being loses health, 40% of what it lost echoes as harm to every other resonating being within 24 blocks of it.");
		// Heaven-Severing Sword Scripture
		t.add(ModLang.heavenMethodKey("heaven_severing_sword_scripture"), "Heaven-Severing Sword Scripture");
		t.add(ModLang.heavenMethodKey("heaven_severing_sword_scripture") + ".description", "Recovered from the deepest grave of the Ancient Sword Graveyard, a scripture of the edge that cuts even karma, severing whatever heaven has granted. At its height the practitioner calls down the swords of every master buried there.");
		t.add(ModLang.heavenMethodKey("heaven_severing_sword_scripture") + ".gains", "Qi Refining to Heavenly Being, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 39: Fate-Severing Edge. Layer 43: Ten Thousand Swords Descend. Every layer: Whetted Edge.");
		t.add(ModLang.spellKey("fate_severing_edge"), "Fate-Severing Edge");
		t.add(ModLang.spellKey("fate_severing_edge") + ".description", "A hair-thin edge of sword light cuts the first being within 32 blocks: every blessing on it (all its beneficial effects and its absorption) is severed, then it takes 60 harm, and for 10 s no new blessing can take hold of it.");
		t.add(ModLang.spellKey("ten_thousand_swords_descend"), "Ten Thousand Swords Descend");
		t.add(ModLang.spellKey("ten_thousand_swords_descend") + ".description", "Gathers a sky of swords above the ground you look at (up to 40 blocks). After 1 s they rain over a 10-block radius for 6 s, twelve each second: every being beneath is struck by one sword each second for 8 harm (up to 48), and swords that strike open ground still cut anyone within 1.5 blocks. A roof shelters what lies under it.");
		// Four Swords Immortal-Slaying Canon
		t.add(ModLang.heavenMethodKey("four_swords_immortal_slaying_canon"), "Four Swords Immortal-Slaying Canon");
		t.add(ModLang.heavenMethodKey("four_swords_immortal_slaying_canon") + ".description", "The Metal at the four axes of the world, forged into four swords that stand at the gates of creation. Whoever enters their array is never let out, and whoever wears this Canon's golden body is broken by no single blow.");
		t.add(ModLang.heavenMethodKey("four_swords_immortal_slaying_canon") + ".gains", "Qi Refining to Four Axis, nine layers a realm (of each realm's nine, layer 1 Early, 3 Mid, 6 Late, 9 Grand Perfection; each opens when your cultivation reaches it). Layer 48: Indestructible Golden Body. Layer 52: Four Gates Immortal-Slaying Array. Every layer: Whetted Edge.");
		t.add(ModLang.spellKey("indestructible_golden_body"), "Indestructible Golden Body");
		t.add(ModLang.spellKey("indestructible_golden_body") + ".description", "For 10 s your body is gold that will not break: no single blow can take more than a tenth of your greatest health, and nothing can knock you back.");
		t.add(ModLang.spellKey("four_gates_immortal_slaying_array"), "Four Gates Immortal-Slaying Array");
		t.add(ModLang.spellKey("four_gates_immortal_slaying_array") + ".description", "Four immortal-slaying swords fall at the four gates of a 28-block square round the ground you look at (up to 48 blocks) and stand for 15 s. Every foe inside, and any that walks in, is sealed: it cannot leave (each try cuts it for 60 harm and casts it back inside), and every 3 s the swords loose their qi, striking each sealed being for 16 harm. Other players are sealed too, but never your allies.");
	}

	private HeavenLangText() {}
}
