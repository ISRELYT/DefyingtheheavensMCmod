package com.example.defyingtheheavens;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Dao Codex, the mod's guide in game (the user's ask, 2026-10-27: "a little encyclopaedia or mod guide that players always have
 * access to in game ... Preferably, it doesn't spoil too much (maybe info unlocks as you go?)"; their picks: the J key and a button on
 * the cultivation menu, a sealed page shown as ??? with a vague hint, a toast and a gold dot for what is new, a player's guide: what things
 * are and how to do them, few exact numbers). Written in plain English (the user's ask, 2026-10-27, after "half again as fast" read as
 * half speed): needs, not "wants"; numbers as numbers.
 * <p>
 * Its chapters and their pages, each page opened by any one of the mod's advancements (ModAdvancements) or open from the first day. Kept
 * on the common side, though only the client reads it (CodexScreen, CodexState, CodexToast) and the datagen writes its words
 * (ModEnglishLangProvider), so a game test can hold its advancements to those the server loads (CodexGameTests).
 * <p>
 * A page's text: paragraphs one to a line; a line beginning "- " is a point of a list, one beginning "# " a small heading; {@code {name}}
 * is a key (one of {@link #KEYS}), shown as the key it is bound to now.
 */
public final class Codex {
	/** What a sealed page says of what opens it: vague on purpose (the user's pick). */
	public enum Hint {
		CULTIVATE("cultivate", "Cultivate further to open this page."),
		STUDY("study", "Learn more of the cultivation arts to open this page."),
		FIND("find", "Find a certain treasure to open this page."),
		SEEK("seek", "Meet more of the world's cultivators to open this page."),
		SECT("sect", "Join a sect to open this page."),
		VENTURE("venture", "Travel farther to open this page."),
		DEAD("dead", "Only the dead know of this."),
		BLOOD("blood", "Only those with a beast's bloodline know of this."),
		HUNT("hunt", "Hunt the great beasts to open this page."),
		IMMORTAL("immortal", "Only immortals know of this.");

		private final String id;
		private final String english;

		Hint(String id, String english) {
			this.id = id;
			this.english = english;
		}

		public String id() { return id; }

		public String english() { return english; }
	}

	/** A chapter: its pages in the order they are listed, and the hint it gives while every one of them is sealed. */
	public record Chapter(String id, ItemLike icon, Hint hint, String title, List<Entry> entries) {
		public String titleKey() {
			return ModLang.codexChapter(id);
		}
	}

	/** A page: what opens it (advancement ids under the mod's namespace, any one of them; empty: open from the first day). */
	public record Entry(String id, String chapter, ItemLike icon, List<String> unlocks, Hint hint, String title, String text) {
		public boolean open() {
			return unlocks.isEmpty();
		}

		public String titleKey() {
			return ModLang.codexEntryTitle(id);
		}

		public String textKey() {
			return ModLang.codexEntryText(id);
		}
	}

	/** The keys a page may name ({@code {menu}}): the mod's own and a few of vanilla's, each shown as it is bound now. */
	public static final Set<String> KEYS = Set.of("menu", "meditate", "circulate", "abilities", "quick", "spells", "cast", "ring", "codex",
			"jump", "sneak", "sprint", "forward", "use");
	public static final Pattern KEY = Pattern.compile("\\{([a-z]+)}");

	private static final List<Chapter> CHAPTERS = new ArrayList<>();
	private static final List<Entry> ENTRIES = new ArrayList<>();

	/** Every chapter, in the order the Codex shows them. */
	public static List<Chapter> chapters() {
		return Collections.unmodifiableList(CHAPTERS);
	}

	/** Every page, chapter by chapter. */
	public static List<Entry> entries() {
		return Collections.unmodifiableList(ENTRIES);
	}

	public static Entry entry(String id) {
		for (Entry entry : ENTRIES) if (entry.id().equals(id)) return entry;
		return null;
	}

	public static Chapter chapterOf(Entry entry) {
		for (Chapter chapter : CHAPTERS) if (chapter.id().equals(entry.chapter())) return chapter;
		return CHAPTERS.get(0);
	}

	/** Every chapter's title and every page's title and text, by key (for ModEnglishLangProvider). */
	public static Map<String, String> english() {
		Map<String, String> english = new LinkedHashMap<>();
		for (Chapter chapter : CHAPTERS) english.put(chapter.titleKey(), chapter.title());
		for (Entry entry : ENTRIES) {
			english.put(entry.titleKey(), entry.title());
			english.put(entry.textKey(), entry.text());
		}
		return english;
	}

	/** The keys a page's text names that aren't {@link #KEYS} (none, if it is written right). */
	public static Set<String> unknownKeys(String text) {
		Set<String> unknown = new HashSet<>();
		Matcher matcher = KEY.matcher(text);
		while (matcher.find()) if (!KEYS.contains(matcher.group(1))) unknown.add(matcher.group(1));
		return unknown;
	}

	// --- How the pages are written ---

	private static List<Entry> writing;
	private static String writingId;
	private static Hint writingHint;

	private static void chapter(String id, ItemLike icon, Hint hint, String title) {
		writing = new ArrayList<>();
		writingId = id;
		writingHint = hint;
		CHAPTERS.add(new Chapter(id, icon, hint, title, Collections.unmodifiableList(writing)));
	}

	/** A page of the chapter being written, with the chapter's hint; {@code unlocks} "a|b" (any one), or null for open. */
	private static void page(String id, ItemLike icon, String unlocks, String title, String text) {
		page(id, icon, unlocks, writingHint, title, text);
	}

	private static void page(String id, ItemLike icon, String unlocks, Hint hint, String title, String text) {
		for (Entry entry : ENTRIES) if (entry.id().equals(id)) throw new IllegalStateException("Two Codex pages are " + id);
		Entry entry = new Entry(id, writingId, icon, unlocks == null ? List.of() : List.of(unlocks.split("\\|")), hint, title, text.strip());
		writing.add(entry);
		ENTRIES.add(entry);
	}

	// The advancements that open pages (ModAdvancements' ids).
	private static final String QR = "cultivation/root", FB = "cultivation/foundation_building", CF = "cultivation/core_formation",
			NS = "cultivation/nascent_soul", HB = "cultivation/heavenly_being", FA = "cultivation/four_axis", DI = "cultivation/dao_integration",
			VS = "cultivation/void_shattering", SV = "cultivation/sacred_vessel", EN = "cultivation/entering_nirvana", TI = "cultivation/true_immortal";

	static {
		// ===================================================== First Steps =====================================================
		chapter("start", ModBlocks.MEDITATION_MAT, Hint.CULTIVATE, "First Steps");
		page("codex", Items.BOOK, null, "The Dao Codex", """
				This is your guide to cultivation. Press {codex} to open it at any time, or click the book beside the cultivation menu.
				The Codex grows with you. Most pages are sealed at first and show only ???, with a hint of how to open them. Reach a new \
				realm, learn a new art or set foot in a new world, and the pages about it open. A toast tells you when that happens, and a \
				gold dot marks every page you haven't read yet.
				Choose a chapter on the left, then a page from its list.""");
		page("mortal", Items.IRON_PICKAXE, null, "A Mortal Body", """
				Everyone begins as a mortal, with no qi, no cultivation and no abilities. Meditation does nothing yet.
				First you must temper your body through four stages: Mortal Low, Mid, High and Peak. Fighting, mining, sprinting and eating \
				all temper it, and so does eating spirit herbs raw. The cultivation menu ({menu}) shows your progress, and each stage makes \
				you a little stronger.
				At Mortal Peak your body is ready to have its meridians opened. (Demons are born with a demon core and start at Qi Refining \
				instead.)""");
		page("meridians", ModItems.MARROW_CLEANSING_ELIXIR, null, "Opening the Meridians", """
				At Mortal Peak, drink a Marrow Cleansing Elixir. It opens your meridians, and you begin cultivating at Qi Refining Early, \
				the first realm. The elixir only works at Mortal Peak.
				Brew it in an Alchemy Cauldron from Ginseng, a Honey Bottle and two Bone Meal.
				Ginseng grows wild on the ground: right-click a plant to dig it up.""");
		page("cauldron", ModBlocks.ALCHEMY_CAULDRON, null, "The Alchemy Cauldron", """
				Pills are brewed, not crafted. Make an Alchemy Cauldron from a cauldron, two copper ingots and a gold ingot (the mod's \
				crafting recipes have their own tab in the recipe book).
				- Fill it with a water bucket.
				- Light a fire under it: fire, soul fire, a lit campfire, lava or a magma block.
				- Right-click the cauldron with each ingredient, one at a time. An ingredient that fits no recipe is refused, so nothing is \
				wasted.
				Once the ingredients make a recipe, the water turns jade and brewing begins; when it finishes, the pill pops out. Right-click \
				with an empty hand to see what's inside, or sneak and right-click to take the ingredients back out. Each pill's tooltip lists \
				its ingredients.""");
		page("meditation", ModBlocks.MEDITATION_MAT, null, "Meditation", """
				Once you can cultivate, meditation is how you grow. Press {meditate} (or the Meditate button on the Cultivation tab) and keep \
				still. Press it again, or move, to stop.
				Meditation draws the world's qi into your cultivation. Some places are much richer in qi than others, and meditation mats, \
				spirit pedestals, a spirit stone held in your hand and cultivator clothing all speed it up.
				Now and then your qi surges: you hear a chime, and a small game appears around the crosshair. Play it with {circulate} to \
				earn Qi Harmony, which speeds up your meditation for a while. If you ignore it, nothing is lost, but fail it badly and you \
				suffer a qi deviation.
				If you meditate long enough, your soul turns inward to your own Inner Realm.""");
		page("menu", ModItems.JADE, null, "The Cultivation Menu", """
				Press {menu} to open the cultivation menu. Its tabs:
				- Cultivation: your realm and stage, the cultivation bar, and the Meditate and Breakthrough buttons.
				- Stats: what your realm gives your body, and your stat points.
				- Abilities: the abilities you have awakened, each with its own switch.
				- Methods: the methods you know, and the one you practise.
				- Spells: the spells your methods have taught you.
				- Sect: your sect, once you belong to one.
				- Journal: what you have heard from cultivators, and the events of your own story.""");
		page("keys", Items.TRIPWIRE_HOOK, null, "Keys", """
				The mod's keys, all of which can be changed in Controls:
				- {menu}: the cultivation menu
				- {meditate}: start or stop meditating
				- {circulate}: circulate qi (for the qi surge games)
				- {abilities} (hold): the ability wheel
				- {quick}: use your selected ability
				- {spells} (hold): the spell wheel
				- {cast}: cast your readied spell
				- {ring}: open your Storage Ring
				- {codex}: the Dao Codex""");
		page("physique", ModItems.SPIRIT_GINSENG, null, "Your Physique", """
				Every cultivator is born with a physique. Yours was rolled when you first joined, announced in chat, and is shown on the \
				Stats tab.
				Its grade (Low, Mid, High, Supreme or Immortal) is your talent: the higher it is, the faster you cultivate and the fewer \
				years meditation costs you. Many physiques also have special gifts, and every physique has spiritual roots, which decide \
				which elements' methods you can learn.
				The higher the grade, the rarer it is.""");

		// ================================================ The Way of Cultivation ================================================
		chapter("way", ModItems.CULTIVATION_PILL, Hint.CULTIVATE, "The Way of Cultivation");
		page("stages", Items.EXPERIENCE_BOTTLE, QR, "Realms and Stages", """
				Cultivation climbs through realms, and each realm has four stages: Early, Mid, Late and Grand Perfection. Meditation fills \
				the cultivation bar on the Cultivation tab.
				Below Nascent Soul, a full bar moves you up to the next stage automatically. Grand Perfection is a realm's peak: there the \
				bar stays full until you break through into the next realm. From Nascent Soul on, every new stage needs a breakthrough too.
				Each realm makes your body far stronger than the last, and many bring new abilities.
				Your qi is shown at the top left of the screen. Abilities, flight and spells use it up, and it refills as you gather qi.""");
		page("unrefined", ModItems.CULTIVATION_FRUIT, QR, "Unrefined Qi", """
				Cultivation Pills and Cultivation Fruit give cultivation, but not all at once. Their qi waits beside your cultivation bar, \
				unrefined, and meditating turns it into cultivation.
				Each pill or fruit you take makes the next one weaker for a while. This medicinal toxicity fades over time; the cultivation \
				menu and the tooltips show how much you have.
				While your bar is full at a realm's peak, nothing is refined, but nothing is lost either.""");
		page("breakthroughs", ModItems.FOUNDATION_PILL, QR, "Breakthroughs", """
				At a realm's Grand Perfection, once the bar is full, press Breakthrough on the Cultivation tab. Breaking into a new realm \
				needs:
				- a heaven method of that realm or a higher one (see Methods and Spells);
				- for Foundation Building, a Foundation Pill eaten at the peak; for Core Formation, a Core Pill;
				- surviving a heavenly tribulation.
				The Cultivation tab tells you what is still missing. Realms after Core Formation need no pill, but some have other \
				requirements of their own.""");
		page("ritual", ModBlocks.HEAVEN_ALTAR, QR, "Ritual to the Heavens", """
				At Qi Refining Mid your bar fills up and stops: to go further, you need the heavens' permission to cultivate.
				- Craft a Heaven Altar (smooth stone, jade, a Low-grade spirit stone and copper), place it under the open sky, and build \
				around it: a grander altar earns more favour.
				- Right-click it and place your offerings in its ring. The dial shows your chance of success; richer offerings raise it.
				- Sneak and right-click it with an empty hand to begin, then kneel still until the heavens answer.
				If they stay silent, your offering is still used up, and you can try again from the next dawn.""");
		page("tribulations", Items.LIGHTNING_ROD, QR, "Tribulations", """
				Breaking into a new realm calls down a heavenly tribulation: a storm gathers over you and strikes you with lightning again \
				and again. From Nascent Soul on, every new stage brings one too.
				Survive every strike and the breakthrough succeeds. If the lightning kills you, you die and also drop back a stage.
				The higher the realm, the more strikes there are and the harder they hit. The first one is easy; after that, wear good \
				armour or cultivator clothing with Protection enchantments, and later bring potions as well. Keep your health high.""");
		page("lifespan", Items.CLOCK, null, "Lifespan", """
				Everyone ages: one year for every day of play, starting from age sixteen. A mortal lives a hundred years, and each realm you \
				reach adds more years, far more with every realm. The Stats tab shows your age and your lifespan.
				Time is life: meditating at a faster pace costs years, and so do some arts. Longevity Pills give some years back.
				When your years run out, the heavens send a storm to take your life. It only ends if you gain new years by breaking through \
				into a new realm. If you die during it, you lose everything you have cultivated.""");
		page("stat_points", Items.GOLDEN_APPLE, QR, "Stat Points", """
				Each stage you reach gives you stat points, and each new realm gives more. Spend them on the Stats tab's Stat Points page, \
				across six stats: Strength, Constitution, Agility, Endurance, Spirit and Insight.
				Stat points belong to you, not to one path: every path you walk shares them. If you fall back, the points you spent last are \
				taken back first.""");
		page("obsession", Items.HEART_OF_THE_SEA, NS, "Name Your Obsession", """
				Before the tribulation from Nascent Soul into Heavenly Being, the heavens ask what your heart cannot let go of: Longevity, \
				Fortune, Merit or Enlightenment.
				Each gives its own boon, and at Four Axis it makes the axis of the same name faster to fill. You can only choose once, so \
				read them all before you face the tribulation.""");
		page("four_axes", Items.COMPASS, FA, "The Four Axes", """
				Four Axis isn't climbed on qi alone. Each of its stages has an axis of virtue, and you can't break out of that stage until \
				its axis is complete:
				- Early, the Axis of Longevity: heal the sick and fading, and accept the years they can spare.
				- Mid, the Axis of Fortune: bring people the goods they lack, and share in their fortune.
				- Late, the Axis of Merit: slay whatever preys on the people.
				- Grand Perfection, the Axis of Enlightenment: help lesser cultivators break through their bottlenecks.
				These wishes come from the settlers on the Astral Realm's worlds: ask a settler about their wish.""");

		// ====================================================== The Realms ======================================================
		chapter("realms", ModItems.FOUNDATION_PILL, Hint.CULTIVATE, "The Realms");
		page("qi_refining", ModItems.QI_GATHERING_PILL, QR, "Qi Refining", """
				The first realm: your meridians are open, and qi flows through them.
				- Meditate, and learn a heaven method (see Methods and Spells).
				- At Qi Refining Mid, perform the Ritual to the Heavens.
				- You already have your consciousness domain, Realm Suppress, Burn Lifespan and the Inner Realm (see Abilities).
				To break through at Grand Perfection, you need a Foundation Building method, a Foundation Pill (brewed from Ginseng, \
				Lingzhi, Spirit Ginseng and Jade), and to survive a weak tribulation.""");
		page("foundation_building", ModItems.FOUNDATION_PILL, FB, "Foundation Building", """
				Your foundation is laid. Your qi now sustains you, so you no longer get hungry. Hold a sword and you can ride it through the \
				air (Sword Flight), and you can now refine a Storage Ring.
				To break through, you need a Core Formation method, a Core Pill (Spirit Ginseng, Spirit Lotus, a Cultivation Fruit and Blaze \
				Powder), and to survive a tribulation that calls for armour.""");
		page("core_formation", ModItems.CORE_PILL, CF, "Core Formation", """
				Your qi condenses into a golden core. You can fly on your qi (Qi Flight, with Meteor Flight as its fast mode), see the qi of \
				the world (Qi Sense), and you float while you meditate. Your core is also a last resort (Core Detonation).
				To break through, you need a Nascent Soul method and to survive a harder tribulation. From here on, no pill is needed.""");
		page("nascent_soul", ModItems.SPIRIT_LOTUS, NS, "Nascent Soul", """
				A soul is born inside your core. It can leave your body (Soul Projection) and take over another body (Possession), and from \
				this realm you can learn techniques (see Methods and Spells). Fly high enough and you can rise beyond the sky itself.
				From Nascent Soul on, every stage needs a breakthrough with its own tribulation. Before you leave this realm, the heavens \
				will ask you to name your obsession.""");
		page("heavenly_being", ModItems.SPIRIT_DEW, HB, "Heavenly Being", """
				You rise above the mortal world, and can tear space itself open (Spatial Tear).
				This is as high as anyone can climb in the lower realms: you can only break through to Four Axis above them, in the Jade \
				Firmament (see Worlds Beyond).""");
		page("four_axis", ModItems.JADE, FA, "Four Axis", """
				You stand upon the four axes of heaven and earth. Each stage of this realm has an axis of virtue to complete (see The Four \
				Axes, in The Way of Cultivation), filled by granting the wishes of the lower realms' people.
				Cultivation needs far more qi than ever before, so cultivate in the Jade Firmament, where qi is rich. In the lower realms \
				your strength is capped at Heavenly Being's.""");
		page("dao_integration", ModItems.JADE_SLIP, DI, "Dao Integration", """
				You become one with the Dao, and your might doubles again. Its methods are rare: only a few steles in the Jade Firmament's \
				wondrous lands, and the few cultivators of this realm, can teach them.
				In the lower realms your strength is still capped at Heavenly Being's.""");
		page("void_shattering", Items.ENDER_EYE, VS, "Void Shattering", """
				You shatter the void: the lower realms no longer hold you back, and you keep your full strength everywhere.
				- Tear space high in the Jade Firmament's sky to open a Void Breach into the Great Void (see Worlds Beyond).
				- Take a title of your own on the Cultivation tab; it is announced to the whole world.
				Cultivating this realm needs the Great Void's dense qi: anywhere else it is many times slower.""");
		page("sacred_vessel", ModItems.NINE_PROVINCES_DING, SV, "Sacred Vessel", """
				You temper yourself into a vessel for the heavens' power. The Patriarchs of the Holy Lands and the Sovereigns of the Demon \
				Palaces are of this realm, and few others in the world.
				Its methods are among the rarest of all: look for them in the Great Void and in the Jade Firmament's most wondrous lands.""");
		page("entering_nirvana", Items.TOTEM_OF_UNDYING, EN, "Entering Nirvana", """
				Burn, and be reborn from the ashes. Cultivators of this realm gather on the Shore of Nirvana in the Great Void, waiting for \
				the Immortal Gate to open.
				The Gate's passage crushes all but the strongest: attempt it at Grand Perfection, well armoured and with healing at hand.""");
		page("true_immortal", ModItems.IMMORTAL_JADE, TI, "True Immortal", """
				Immortality: your years have no end. From now on you can only break through in the Immortal Realm, and no method can take \
				you higher: the Laws do (see Laws and Immortality).
				Your immortal essence only fills in the Immortal Realm. And every ten days, the Five Decays come for you.""");
		page("beyond", ModItems.OCHRE_HUANGJING, TI, "Past True Immortal", """
				The ladder continues: Earth Immortal, Heaven Immortal, Golden Immortal, Taiyi Golden Immortal, Profound Sovereign and, at \
				the summit of the Dao, Heavenly Lord.
				Each realm is twice as mighty as the one before, and each needs more total levels of the Laws. No Heavenly Lord walks any \
				world.""");

		// =================================================== Methods and Spells ===================================================
		chapter("methods", ModItems.JADE_SLIP, Hint.STUDY, "Methods and Spells");
		page("heaven_methods", ModItems.JADE_SLIP, QR, "Heaven Methods", """
				Heaven methods are the arts of cultivation: one for each element in every realm. A method:
				- speeds up your cultivation (without one, you cultivate at three quarters of the speed);
				- gives a passive bonus of its element, which grows as you do;
				- teaches spells as its layers settle in;
				- opens the way into new realms: a breakthrough needs a method of the realm ahead, or a higher one.
				You only understand a method up to your current cultivation. When you reach a new stage, its next layers settle in as you \
				meditate. Choose which method you practise on the Methods tab.""");
		page("learning", ModBlocks.DAO_STELE, QR, "Learning a Method", """
				- Jade Slips: right-click one to read it. It teaches its method at once and isn't used up. Cultivators carry them, sect \
				libraries keep them, and elders and rogues sell them to people they like.
				- Dao Steles: carved stones in the wilds, rarer the higher their method. Meditate beside one for a while to learn its \
				method.
				- Teachers: a sect's elders and master, and rogues, may teach you their method once their regard for you is Warm.
				Methods of different elements add together; for methods of the same element, only the highest counts.""");
		page("elements", Items.BLAZE_POWDER, QR, "The Five Elements", """
				Wood, water, fire, earth and metal. Every heaven method belongs to one element: it draws in that element's qi, and its \
				spells are of that element too.
				Your physique's spiritual roots decide which elements you can learn: a method whose element your roots reject won't take.""");
		page("spells", Items.FIRE_CHARGE, "arts/root", "Spells", """
				Your methods teach spells as their layers settle in; you'll find them on the Spells tab.
				Ready a spell there, or hold {spells} to open the spell wheel and pick one, then press {cast} to cast it. Spells cost qi, \
				and each has a cooldown before it can be cast again. Arrange the spell wheel on the Spells tab.
				The higher your realm, the farther and wider your spells reach. Whether spells permanently change the land is a world \
				setting, which an operator can switch at the bottom of the Spells tab.""");
		page("spells_both", Items.DRAGON_BREATH, "arts/heaven_demon|sects/bloodline", Hint.BLOOD, "Spells of Both Paths", """
				From Foundation Building on, each realm has one spell that belongs to both the heaven path and the Demon Path. You can use \
				it once both of those paths have reached its realm.
				Only the living can cast these spells, and they use your qi.""");
		page("techniques", ModItems.TECHNIQUE_MANUAL, NS, Hint.CULTIVATE, "Techniques", """
				Techniques are arts learned from a Technique Manual, rather than gained with a realm. Sects keep these manuals in their \
				masters' halls, and more rarely in their libraries.
				Read a manual at its realm and the technique is yours for good. The manual isn't used up, so you can pass it on; if you're \
				below its realm, it tells you which realm you need.
				The techniques so far are Clone and Universe in a Sleeve (see Abilities).""");

		// ======================================================= Abilities =======================================================
		chapter("abilities", Items.FEATHER, Hint.CULTIVATE, "Abilities");
		page("wheel", Items.RECOVERY_COMPASS, QR, "The Ability Wheel", """
				Your abilities are listed on the Abilities tab, each with its own switch, and placed on the ability wheel. Hold \
				{abilities}, point toward a slot and click to select it; click it again, right-click, or press its number to switch it on \
				or off (or to use it, for one-time abilities).
				{quick} uses your selected ability without opening the wheel. You can rearrange the wheel on the Abilities tab.
				New abilities awaken as you climb the realms.""");
		page("domain", Items.SPYGLASS, QR, "Consciousness Domain", """
				Your consciousness spreads around you as a domain, growing wider with every stage. Inside it you see every living thing \
				outlined, even through walls, along with each cultivator's realm.
				Realm Suppress (off until you switch it on) weighs down everything weaker than you inside your domain, at a cost in qi.""");
		page("inner_realm", Items.ENDER_PEARL, QR, "The Inner Realm", """
				Meditate for a while and your soul turns inward: you open your eyes on an island of your own in a void, while your body \
				stays sitting where you left it. Meditation there is 1.5 times as effective.
				Walk the island as you like, and press {meditate} to return to your body. If anything hits your body, you are pulled back \
				at once.
				You can switch it off on the Abilities tab if you'd rather stay where you sit.""");
		page("burn_lifespan", Items.TORCH, QR, "Burn Lifespan", """
				Set your own years alight for strength. While they burn, your blows and spells hit 1.5 times as hard and you move faster, \
				but your lifespan burns away quickly.
				It switches itself off before it would leave you with too few years.""");
		page("sword_flight", Items.IRON_SWORD, FB, "Sword Flight", """
				Hold any sword in your main hand and double-tap {jump}: the sword lies flat under your feet and carries you through the \
				air. It costs little qi, but it is slow, and no realm makes it faster.
				Only one of Sword Flight and Qi Flight can be switched on at a time.""");
		page("qi_flight", Items.FEATHER, CF, "Qi Flight", """
				Double-tap {jump} to fly on your qi, as freely as in creative mode. Flying uses up qi, and a young core's pool runs dry \
				quickly, so keep an eye on it and land before it empties. In the Jade Firmament, and from Heavenly Being on, it never runs \
				out.
				Meteor Flight is its fast mode.""");
		page("meteor_flight", Items.FIRE_CHARGE, CF, "Meteor Flight", """
				Qi Flight's fast mode, off until you switch it on. With it on, hold {forward} to fly where you look, and hold {sprint} to \
				build up to your realm's top speed.
				From Nascent Soul, you blaze across the sky and can smash through whatever stands in your way, if the world's settings \
				allow it.""");
		page("qi_sense", Items.GLOW_INK_SAC, CF, "Qi Sense", """
				You can see the plane of qi. While Qi Sense is on, the world turns grey and its qi appears as motes in the colours of their \
				elements, crowded where qi is thick. Spirit treasures keep their colours.
				Switch it off on the Abilities tab or the wheel to see the world normally again.""");
		page("core_detonation", Items.TNT, CF, "Core Detonation", """
				A last resort. Use it once and your core trembles as a warning; use it again within a few seconds and your core breaks, \
				swells and bursts. The blast grows with your realm and tears through everything nearby.
				You die, and your golden core is lost: you fall back to Foundation Building.""");
		page("soul_projection", Items.SOUL_LANTERN, NS, "Soul Projection", """
				Your soul can leave your body. Switch Soul Projection on while standing on steady, dry ground and keep still for a few \
				seconds, and your soul rises out. Your body stays sitting where you left it, and any harm done to it is done to you.
				Your soul flies freely on a tether to your body, unseen except by those who can perceive souls. Switch the ability again to \
				call it home.""");
		page("possession", Items.ARMOR_STAND, NS, "Possession", """
				A projected soul can take over a body at least one realm below yours: right-click it. In a cultivator's body you play as \
				yourself, with everything it carries, and others see its face and name.
				Let go, or call your soul home, to leave the body. If the body dies, your soul is thrown out unharmed.""");
		page("clone", ModItems.BLANK_MANUAL, NS, "Clone", """
				A technique for Nascent Soul and above, learned from its manual. Use it to split off a second body at the cost of half your \
				qi; use it again to move into the clone while your main body sits in meditation.
				The clone is you, at your own realm, but with half your strength.""");
		page("spatial_tear", Items.OBSIDIAN, HB, "The Spatial Tear", """
				Tear space open: a rift appears in front of you and stays open for a while, and anyone can walk through it. Its rim shows \
				where it leads: violet into the Spatial Gap, green out to the Overworld, gold out to the Jade Firmament.
				Each tear costs part of your qi.""");
		page("sleeve", Items.ENDER_EYE, HB, "Universe in a Sleeve", """
				A technique for Heavenly Beings and above, learned from its manual. Look at another player close by and use it to offer \
				them a place in your sleeve. Up to four can be inside at once. Anyone who accepts vanishes into it: safe, untouchable, and \
				seeing what you see.
				They go wherever you go, up through the Spatial Gap, the Void Breach and the Immortal Gate, whatever their realm, and step \
				out beside you when you arrive. You pay for it: the spatial pressure on you doubles for each person you carry. Even a True \
				Immortal, who feels nothing at the Immortal Gate on its own, takes a quarter of its body over the climb, doubled the same \
				way.
				Sneak and use it to let everyone out, anywhere but partway through a climb. If you die on the way, they're thrown out \
				where you fell.
				Anyone inside your small world travels with you the same way, at the same cost.""");

		// ================================================= Treasures and Alchemy =================================================
		chapter("treasures", ModItems.LOW_SPIRIT_STONE, Hint.FIND, "Treasures and Alchemy");
		page("spirit_stones", ModItems.LOW_SPIRIT_STONE, null, "Spirit Stones", """
				Spirit stones are heaven and earth's qi condensed into stone: the cultivators' money, and a store of qi. There are five \
				grades (Low, Mid, High, Supreme and Immortal), each worth nine of the grade below. Nine stones craft into one of the next \
				grade, and one breaks back down into nine.
				- Hold right-click to absorb a stone's qi into your pool.
				- Meditate with one in your hand to cultivate faster while its qi lasts.
				They are mined from spirit stone veins in the Overworld's stone (the richer veins lie deepest), and cultivators carry them.""");
		page("fruit", ModItems.CULTIVATION_FRUIT, null, "Cultivation Fruit", """
				Cultivation Fruit is rare. It hangs under the leaves of wild trees and ages as the days pass: the older it is, the brighter \
				its aura and the more qi it holds. Right-click it to pick it. A picked fruit keeps its age but grows no older.
				Eating it gives cultivation, refined as you meditate. Now and then one leaves a Peach Pit, which grows into a Spirit Peach \
				Tree of your own.""");
		page("herbs", ModItems.GINSENG, null, "Spirit Herbs", """
				Spirit herbs grow wild and age like fruit does: Ginseng and the rarer Spirit Ginseng, Lingzhi in dark forests, Huangjing, \
				the Spirit Lotus on still water, and Spirit Dew Grass, which gathers beads of dew. Right-click a herb to dig it up. It keeps \
				its age, and the older the herbs, the better the pills brewed from them.
				A fully grown plant gives seeds when you dig it up. Purple Lingzhi and Ochre Huangjing are rare kinds that count as twice \
				their age.""");
		page("pills", ModItems.QI_GATHERING_PILL, QR, Hint.CULTIVATE, "Pills", """
				Brewed in the Alchemy Cauldron:
				- Qi Gathering Pill: you gather more qi for a while.
				- Cultivation Pill: gives cultivation, refined as you meditate.
				- Foundation Pill and Core Pill: needed to break into Foundation Building and Core Formation.
				- Longevity Pill: adds years of life; each one adds fewer than the last.
				A pill comes out at Low, Mid, High, Supreme or Immortal grade: the older its herbs, the higher the grade and the stronger \
				the pill.""");
		page("mats", ModBlocks.SPIRIT_PEDESTAL, QR, Hint.CULTIVATE, "Mats and Pedestals", """
				A Meditation Mat, or the finer Red Silk Meditation Mat, speeds up your meditation: right-click it to sit down and meditate.
				A Spirit Pedestal holds one Cultivation Fruit or herb, floating above it. Place pedestals close around where you meditate \
				and their qi feeds you: the older what they hold, the more it helps. Only the oldest few count.""");
		page("clothing", Items.LEATHER_CHESTPLATE, null, "Cultivator Clothing", """
				Robes, guan, trousers and boots worn instead of armour, in five grades: Mortal, Spirit, Earth, Heaven and Immortal. Each \
				grade protects as well as the matching tier of armour, and also speeds up your qi gathering and meditation.
				Mortal clothing is crafted from wool and string, and each piece can be upgraded one grade at a time in the crafting grid, \
				keeping its enchantments. Cultivators wear it too, and sometimes drop it.""");
		page("storage_ring", ModItems.STORAGE_RING, FB, Hint.CULTIVATE, "Storage Rings", """
				A Storage Ring folds a pocket of space into a ring. Craft one from four gold ingots, four jade and an ender pearl.
				Right-click to open it, sneak and right-click to wear it, and press {ring} to open the one you're wearing. Whatever it \
				holds stays inside, wherever the ring goes.
				Refine it (the button at the top right of its screen) to raise its grade and its capacity. Each grade needs spirit stones, a \
				few rarer items and a higher realm, starting at Foundation Building; the highest grade has no limit.""");
		page("jade", ModBlocks.JADE_ORE, null, "Jade and Spirit Wood", """
				Jade Ore is found in mountains, about as rare as diamonds; mine it with an iron pickaxe. Jade goes into the Foundation Pill, \
				spirit pedestals, Storage Rings and more.
				Blue Spirit Trees, spruces with blue qi flowing up their trunks, grow among the spruces of the taiga.""");
		page("sacred_vessels", ModItems.JIANMU_BOUGH_VASE, "arts/sacred_vessel|worlds/great_void", Hint.VENTURE, "Sacred Vessels", """
				Five treasures from the old tales, one for each element: the Jianmu Bough Vase, the Heaven Furnace Censer, the Nine \
				Provinces Ding, the Immortal-Forging Bell and the Sealed Ocean Gourd.
				Each lies on a relic altar in the Great Void, and a guardian rises to protect it from anyone who comes near. Held in your \
				hand, a vessel makes spells of its element hit harder.""");
		page("chaos_treasures", ModItems.DAO_FRAGMENT, "arts/dao_fragment|arts/origin_stone|worlds/chaos_frontier", Hint.IMMORTAL,
				"Chaos Treasures", """
				Two treasures come from the Chaos Frontier:
				- A Dao Fragment is a shard of one Law. An immortal can use it to gain half a level of insight into that Law.
				- A Chaos Origin Stone is hardened chaos, the material of a small world's seed. Chaos Fiends drop them, and Chaos Origin Ore \
				in the Frontier's land holds them.""");

		// ================================================= Sects and Cultivators =================================================
		chapter("sects", ModItems.SECT_PASS, Hint.SEEK, "Sects and Cultivators");
		page("cultivators", Items.PLAYER_HEAD, null, "Cultivators", """
				The world is full of cultivators: wandering rogues, hermits in seclusion, and sect disciples. Each has a realm (your \
				consciousness domain shows it), a temperament and an alignment.
				They live their own lives: they cultivate, travel, spar, trade and break through, and some of them die. Talk to them, and \
				most will have something to tell you.""");
		page("talking", Items.WRITABLE_BOOK, null, "Talking", """
				Right-click a cultivator to talk. The talk wheel offers Greet, Ask, Spar, Trade, Gift and Provoke.
				- Ask: about them, their sect, rumours, and what they could teach or sell you.
				- Trade: buy and sell for spirit stones.
				- Gift: give them something they value to raise their regard for you.
				Mind your manners with cultivators far above your realm.""");
		page("regard", Items.GOLD_NUGGET, "sects/root", "Regard", """
				Every cultivator has a regard for you, ranging from Hostile through Polite, Courteous, Warm and Trusted up to Sworn. \
				Greetings, gifts and spars raise it; provoking or harming them lowers it.
				The warmer their regard, the better their prices, and the more they will sell you and teach you.""");
		page("alignment", Items.SHIELD, null, "Alignment", """
				Alignment runs from Devilish through Neutral to Righteous. Neutral cultivators keep the peace unless provoked, while \
				righteous and devilish ones attack each other on sight, except in the Immortal Realm. Sects treat you according to your \
				alignment.
				Cultivators below Entering Nirvana run from someone of the other path a realm or more above them. From Entering Nirvana \
				up, they stand their ground.
				Killing a cultivator who didn't provoke you makes your alignment more devilish; killing devilish cultivators makes it more \
				righteous.""");
		page("sparring", Items.WOODEN_SWORD, "sects/root", "Sparring", """
				Ask a cultivator to spar: a friendly bout with pulled blows that ends when one side drops to half health, not a fight to the \
				death. You can bet spirit stones on it.
				Winning spars earns respect, and some sects' entrance trials are spars.""");
		page("sects", ModBlocks.FORMATION_CORE, null, "Sects", """
				Sects are villages of cultivators built around a main hall, protected by the barrier of their Formation Core. A gate stands \
				on the road in, watched by gate guards, and the members follow a daily routine of lectures, practice, sleep and night watch.
				A sect's library keeps jade slips, and its sealed treasury holds its riches. Members are ranked by strength, from Outer \
				Disciples up to the Sect Master. Demon clans live the same way, each made up of a single race.""");
		page("barriers", Items.GLASS, null, "Protective Formations", """
				A Formation Core raises a barrier around itself that only its sect (or its owner) can pass, and no monsters spawn inside it. \
				A cultivator at least as strong as the formation can slowly break through. Break the core, or let it run out of qi, and the \
				whole barrier falls.
				Craft a Formation Core of your own to raise a barrier for yourself and the people you trust.""");
		page("joining", ModItems.SECT_PASS, "sects/root", "Joining a Sect", """
				Ask any sect member to join. A disciple will send you to an elder, who judges you: most set an entrance trial, a spar that \
				you must either win or last to the end. A gifted physique, or an elder whose regard for you is Warm, may let you in without \
				one.
				As a disciple you can pass the barrier, and you cultivate faster on the sect's grounds. Follow the treasury's rules: a thief \
				is warned once, then expelled.""");
		page("rising", Items.LECTERN, "sects/joined", Hint.SECT, "Rising in a Sect", """
				Your title in a sect depends on your strength compared with its members: Outer Disciple, Inner Disciple, Elder or Grand \
				Elder. It changes as you, or they, break through. Elders may open the treasury.
				The Sect tab shows your sect, your title, the sect's regard for you, and its members.""");
		page("address", Items.GOLD_INGOT, "sects/root", "Forms of Address", """
				Cultivators address you according to your realm and theirs: those below you are polite or humble, and those above you talk \
				down to you. If you stand four or more realms above a cultivator, they kneel before you.
				Within your sect, members below you address you by your title.""");
		page("titles", Items.NAME_TAG, VS, Hint.CULTIVATE, "A Title of Your Own", """
				From Void Shattering on, you can take a title of your own: type it into the field on the Cultivation tab and press Enter. It \
				is announced to the whole world and shown above your name.""");
		page("court", ModItems.COURT_MERIT_TOKEN, "worlds/immortal_realm", Hint.VENTURE, "The Celestial Court", """
				The Immortal Realm's court: the Heavenly King on the throne, five Heavenly Venerables in their halls, and the Immortal \
				Generals at its gate.
				Ask a Venerable to join its service. You earn merit by standing watch at the Southern Heavenly Gate, slaying devilish \
				immortals and fighting the primordial beasts. Merit raises your rank from Officer to Captain and General, and the \
				Quartermaster trades goods for it.""");

		// ================================================= Beasts and Bloodlines =================================================
		chapter("beasts", ModItems.DEMON_CORE, Hint.BLOOD, "Beasts and Bloodlines");
		page("demon_beasts", Items.BONE, null, "Demon Beasts", """
				Beasts with cultivation of their own roam the wilds, from Qi Refining up to Core Formation. They are as strong as \
				cultivators of the same rank, and the stronger they are, the bigger they grow. Your consciousness domain shows their realm.
				Each kind lives in its own lands and has its own way of fighting.""");
		page("demon_cores", ModItems.DEMON_CORE, "arts/demon_core", Hint.FIND, "Demon Cores", """
				A slain demon beast may drop its demon core; the higher its rank, the more likely. A core records the beast's realm, stage \
				and element, and its look shows its realm.
				Beasts related to a demon race may also drop a phial of that race's blood.""");
		page("bloodlines", ModItems.DEMON_BLOOD, null, "Bloodlines", """
				When you first join, you choose your bloodline, once and for good: Human, or one of the demon races (Tiger, Lizard, Fox or \
				Dragon), each in three colourings.
				A demon is born with a demon core, so its Demon Path and its heaven path have both already begun. Each race has its own \
				gifts.""");
		page("demon_path", Items.CRIMSON_FUNGUS, "sects/bloodline", "The Demon Path", """
				A third path beside the heaven path and the ghost path, with the same realms and stages and its own cultivation bar. You can \
				walk it alongside the heaven path.
				At a realm's Grand Perfection, the Demon Path can only break through once you have drunk your race's blood of the next \
				realm, just as the heaven path needs a method.""");
		page("demon_blood", ModItems.DEMON_BLOOD, "sects/bloodline", "Demon Blood", """
				Tiger, Lizard, Fox and Dragon Blood, each of a certain realm, drunk like a potion. Demons of that race drop it, and so do \
				wild beasts related to it.
				- A demon can only drink its own race's blood, which opens the way for its Demon Path to break through.
				- A human who drinks it forms a demon core and becomes a half-blood: human in looks, with half the race's gifts. Each drink \
				makes the blood purer. A drink that would turn you completely into that race is refused with a warning, unless you drink it \
				while sneaking. There is no way back.""");
		page("clans", Items.RED_NETHER_BRICKS, "sects/bloodline|sects/root", Hint.SEEK, "Demon Clans", """
				A demon clan is a sect of a single demon race, led by its Clan Head. It works like a sect: it has a barrier, a treasury and \
				titles by strength. Its members are demons of that race, cultivating the Demon Path.
				A clan only takes in demons of its own race.""");
		page("great_beasts", ModItems.DEMON_BLOOD, "arts/elder_beast|worlds/battlefield", Hint.HUNT, "The Elder Beasts", """
				On the Great Void's Ancient Battlefields, each pool of blood holds a phial of a demon race's blood. Come near, and that \
				race's elder rises out of the pool to guard it: the Elder White Tiger, the Nine-Tailed Fox, the Mountain-Bearing Lizard King \
				or the Azure Dragon.
				They stay by their pools, and leave alone anyone a realm or more above them.""");
		page("primordials", Items.DRAGON_HEAD, "arts/primordial_beast|worlds/immortal_realm", Hint.HUNT, "Primordial Beasts", """
				The Immortal Realm's world bosses, one of each at a time: the Kun-Peng, the True Phoenix and the True Dragon, each stronger \
				than a True Immortal at its peak. A fourth, the Primordial Chaos, lurks far out in the Chaos Frontier.
				The Heavenly Venerables know where to find them, and the Court rewards those who fight them.""");

		// ===================================================== Worlds Beyond =====================================================
		chapter("worlds", Items.ENDER_EYE, Hint.VENTURE, "Worlds Beyond");
		page("spatial_gap", Items.OBSIDIAN, HB + "|worlds/spatial_gap", "The Spatial Gap", """
				Between the Overworld and the Jade Firmament lies the Spatial Gap, a sea of torn space. A rift at the very centre of the \
				Overworld leads into it, and so does a Spatial Tear.
				Anyone who can fly flies freely there; anyone who can't falls back out. Every block you cross in the gap counts as many \
				blocks in the world. Its pressure and storms wear you down the whole time, so bring healing.
				Rise through its upper layers to come out in the Jade Firmament; sink through its floor and you fall into the Overworld's \
				sky.""");
		page("jade_firmament", Items.EMERALD, HB + "|worlds/jade_firmament", "The Jade Firmament", """
				The Upper Realm: islands of jade-veined land under a jade sky, far richer in qi than the lower realms. Its sects are \
				stronger, its herbs and spirit stones richer, and its steles teach higher methods.
				Four Axis and Dao Integration are cultivated here.""");
		page("astral", Items.AMETHYST_CLUSTER, NS + "|worlds/astral", "The Astral Realm", """
				Beyond the Overworld's sky lies space. The Overworld is one planet among thousands, circling hundreds of stars, and every \
				other planet has land of its own to explore.
				From Nascent Soul, fly high enough above the Overworld or the Jade Firmament and you rise out into space. Settlers live on \
				its worlds, and they have wishes of their own.""");
		page("great_void", Items.CRYING_OBSIDIAN, VS + "|worlds/great_void", "The Great Void", """
				Above the Jade Firmament: floating continents under a black-violet void full of stars, with a hundred times the lower \
				realms' qi.
				- Reach it through a Void Breach, torn high in the Jade Firmament's sky from Void Shattering on. A Dao Integration \
				cultivator can only pass through a breach held open by someone stronger.
				- The climb takes a while, through shattered space where no flight works, and the void presses on you all the way.
				- To go back down, fall off a continent's edge or tear space.
				Drifting rifts of starlight carry whoever touches them far across the void.""");
		page("holy_lands", Items.QUARTZ_PILLAR, "worlds/great_void|worlds/holy_land|worlds/demon_palace", "Holy Lands", """
				The Great Void's sects. A Holy Land is a sect of white jade and gold, led by a Sacred Vessel Patriarch; a Demon Palace is the \
				demons' version, of a single race, led by its Demon Sovereign.
				Both are rare, and both take in disciples like any sect or clan.""");
		page("battlefields", Items.SKELETON_SKULL, "worlds/great_void|worlds/battlefield", "Ancient Battlefields", """
				Lands scarred by the ancient war between Heaven and the demons: craters, fissures and toppled spires, bones and broken \
				walls, and water that runs red.
				Their ghostly qi is denser than anywhere else, even the Netherworld, which makes them the best place for the ghost path. \
				Elder beasts guard pools of blood there.""");
		page("shore", Items.CAMPFIRE, EN + "|worlds/shore_of_nirvana", "The Shore of Nirvana", """
				At the edge of the Great Void's sea, a camp of Entering Nirvana hermits and old monsters waits for the Immortal Gate. They \
				trade, and are rarely hostile.
				Attack one of them and the whole camp fights you.""");
		page("gate", Items.END_PORTAL_FRAME, EN + "|worlds/shore_of_nirvana", "The Immortal Gate", """
				The Gate hangs in the Great Void's sky beyond the Shore of Nirvana. It opens every few days for a few minutes, and everyone \
				is told a minute before.
				Crossing it means climbing through its passage under crushing pressure: True Immortals cross unharmed, Entering Nirvana \
				cultivators at Grand Perfection survive with care, and anyone weaker is crushed or turned back.""");
		page("immortal_realm", Items.BEACON, "worlds/immortal_realm", "The Immortal Realm", """
				An endless sea of clouds with jade peaks rising out of it, under a golden sky, with a thousand times the lower realms' qi. \
				Nobody below Entering Nirvana can enter.
				Its cities and markets, its island sects and the Celestial Court are here. The only way back down is through the Gate while \
				it is open. Far out, the land breaks up into chaos.
				The immortals here think the quarrel between the righteous and the devilish is beneath them. Nobody attacks you on sight \
				or runs from you because of your path, and they'll talk and trade with you whatever it is. Attack one and it fights back.""");
		page("chaos_frontier", ModItems.CHAOS_ORIGIN_STONE, "worlds/chaos_frontier", "The Chaos Frontier", """
				The Immortal Realm's edge, where chaos from before heaven and earth still churns: floating shards that, farther out, merge \
				into one wasteland.
				- Its storm wears you down while you're unsheltered. A lit Law domain shelters you, and so does the light of the First \
				Flame, whose spring lies far to the south.
				- Chaos Fiends appear out of the air, stronger the deeper you go.
				- Meditating in the First Flame's light comprehends a Law much faster.""");
		page("small_worlds", ModItems.WORLD_SEED, TI + "|worlds/small_world", Hint.IMMORTAL, "Small Worlds", """
				An immortal's own world: a floating island under the stars. Craft a World Seed from five Chaos Origin Stones and four \
				Immortal Jade, then, as a True Immortal, hold it in use to refine it.
				After that, you can open its door wherever you stand, for yourself and the guests you allow.
				Your world travels with you. Anyone inside it when you go through the Spatial Gap, the Void Breach or the Immortal Gate \
				comes through with you, whatever their realm, and its gate then leads out beside you. As with Universe in a Sleeve, the \
				spatial pressure on you doubles for each of them. If you die on the way, they stay safe inside, and its gate still leads \
				back to where they came in.""");

		// ================================================== The Yellow Springs ==================================================
		chapter("springs", ModItems.OTHER_SHORE_FLOWER, Hint.DEAD, "The Yellow Springs");
		page("death", Items.SKELETON_SKULL, "springs/root", "Death", """
				Death is not the end. When you die, you wake at the Ghost Gate in the Netherworld as a ghost: cyan light in your own \
				likeness. You fly freely (double-tap {jump}) and can't touch anything in the living world.
				Your items drop where you died. You keep everything you cultivated, but while you are dead you can only cultivate the ghost \
				path (the heaven path and the Demon Path wait until you live again), and the dead cultivate it 1.5 times as fast as the \
				living.
				If you are struck down as a ghost, you scatter and re-form at the Gate.""");
		page("returning", ModItems.REBIRTH_PILL, "springs/ghost", "Coming Back", """
				- The Wheel of Reincarnation, across the bridge from the Gate: use its hub to return to life at your spawn point. Nothing a \
				ghost carries comes back with it.
				- Meng Po's soup, by the bridge: drink it before the Wheel, and your medicinal toxicity is cleared when you live again.
				- The Terrace for Gazing Homeward: from its top, a ghost can see where it died.""");
		page("netherworld", Items.SOUL_SOIL, "springs/root", "The Netherworld", """
				The realm of the dead: golden riverlands, fields of red spider lilies, wastes of ash and bone, and the dark Nine Nether \
				Peaks, where ghostly qi is densest.
				The Ghost Gate is its hub. Ghost cultivators wander it, Hungry Ghosts gather in the Bone Wastes, and Ghost Kings rule from \
				their palaces.""");
		page("soul_river", ModItems.NETHERWORLD_LOTUS, "springs/root", "The Soul River", """
				The living reach the Netherworld through the Gate of the Yellow Springs, a rift at the very centre of the Nether: walk into \
				it, and the Soul River carries you up to the Ghost Gate.
				To go back down, use the rift on the Gate's plaza.""");
		page("yin", Items.BLUE_ICE, "springs/root", "Yin Corruption", """
				A living body in the Netherworld fills up with its yin, shown by the Yin Corruption bar under your qi. Frost creeps in from \
				the edges of the screen as it rises, with warnings along the way.
				When it is full, your body gives way and you become a ghost where you stand. Outside the Netherworld it slowly drains away.""");
		page("ghostly_qi", ModItems.YIN_JADE, "springs/root", "Ghostly Qi", """
				The second kind of qi. It belongs to places, not to you: ghost methods are cultivated on it, and your ghostly qi pool fills \
				from it. Under Qi Sense it appears as cyan yin motes.
				It is thickest on the Great Void's Ancient Battlefields, thick in the Netherworld (thicker on the Nine Nether Peaks), thin \
				in soul sand valleys and faint in the Overworld.
				Your ghostly qi pool grows with your ghost path's realm, as your qi pool grows with the heaven path's, and the ghost methods \
				you know add to it. The higher your ghost path, the faster it refills, but the place still matters most.""");
		page("ghost_path", ModItems.GHOST_JADE_SLIP, "springs/root", "The Ghost Path", """
				The path of the ghost methods, walked by both the dead and the living, with its own cultivation bar and its own \
				tribulations. It begins with a ghost method, learned from a Ghost Jade Slip, a Method Stele, or a ghost cultivator willing \
				to teach it.
				The first method, the Netherworld Breathing Art, is carved on the stele on the Ghost Gate's plaza.
				The living choose which qi they draw in with the Path button on the Cultivation tab.""");
		page("ghost_kings", Items.GOLDEN_HELMET, "springs/ghost_path", "Ghost Kings", """
				A dead cultivator whose ghost path has reached Core Formation becomes a Ghost King: crowned, mighty, and a lord of the \
				Netherworld. The heaven path never makes a king, however high it goes.
				The Netherworld's own kings rule palaces and courts, and keep treasuries under their seal.""");
		page("two_paths", ModItems.YIN_GATHERING_PILL, "springs/ghost_path", "The Two Paths", """
				Neither path builds on the other, but someone who walks both is stronger than someone who walks only one. Your body follows \
				the stronger of the two, and the other adds its share.
				The living get the heaven path's full strength and part of the ghost path's; for the dead, it is the other way round.""");
		page("exorcist", ModItems.PEACH_WOOD_SWORD, "springs/root", "The Exorcist's Tools", """
				Tools against the dead:
				- A Peach Wood Sword hits the dead with full force, and twice as hard.
				- An Inscription Brush paints talismans. The Ghost-Quelling Talisman, thrown, strikes the dead and binds them; the Yang Fire \
				Talisman, burned, drives the Netherworld's yin out of your body.
				- The Yang Preservation Pill stops yin corruption from rising; the Yin Gathering Pill, brewed from an Other-Shore Flower and \
				a Netherworld Lotus, speeds up the ghost path.
				Yin Jade, mined in the Netherworld, goes into the Ghost-Quelling Talisman, and the dead value it highly.""");

		// ================================================= Laws and Immortality =================================================
		chapter("laws", Items.NETHER_STAR, Hint.IMMORTAL, "Laws and Immortality");
		page("laws", Items.WRITTEN_BOOK, TI, "The Laws", """
				Metal, Wood, Water, Fire and Earth, and the greater Laws of Space, Time, and Life and Death, each with nine levels. Past \
				True Immortal, every realm needs a certain total of Law levels.
				- Meditate in the Immortal Realm to comprehend the Law you are focusing on.
				- Slay strong foes with an element's spells for glimpses of that element's Law.
				- Survive the Five Decays to gain a level of your choice.
				Each Law strengthens its own arts. The greater Laws open at Golden Immortal. You'll find them on the Methods tab's Laws \
				page.""");
		page("law_domains", Items.END_CRYSTAL, "cultivation/law", "Law Domains", """
				Once you comprehend a Law's first level, you awaken its domain: the Law's own ground spread out around you. Only one domain \
				can be lit at a time, and it burns immortal essence. Foes inside it suffer the Law's effects, and your spells of that Law \
				hit them harder.""");
		page("decays", Items.WITHER_ROSE, TI, "The Five Decays", """
				Every ten days the heavens judge an immortal, with a warning the day before. Five signs come a minute apart: your robes \
				stain (your armour weakens), your flowers wither (nothing heals you), your qi falters (qi gathering is halved), your body \
				sweats (your blows and spells weaken) and your mind can't rest (no meditation). Then comes the judgement: a storm of \
				lightning.
				Survive it and choose a level of the Laws. If it strikes you down, you live on, but one stage lower.""");
		page("essence", Items.GOLD_BLOCK, TI, "Immortal Essence", """
				A True Immortal's golden essence pool. It only fills in the Immortal Realm and drains away anywhere else. Switch on the \
				Immortal Essence ability and your blows and spells hit harder while it lasts. Law domains use it up too.""");
	}

	private Codex() {}
}
