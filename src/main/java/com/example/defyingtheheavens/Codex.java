package com.example.defyingtheheavens;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
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
 * is a key (one of {@link #KEYS}), shown as the key it is bound to now. A line beginning "@" is a picture (the user's pick, 2026-10-28:
 * diagrams and rows of items), one of {@link #PICTURES}:
 * <ul>
 *   <li>"@items jade minecraft:bone_meal*2 + ... > ...": a row of items (the mod's own by their name alone, "*n" for how many), with
 *   "+" and an arrow (">") drawn between where they stand;</li>
 *   <li>"@brew foundation_pill": the cauldron's recipe for it, as AlchemyRecipes has it;</li>
 *   <li>"@craft low_storage_formation": a crafting recipe, by its id, as the game has it;</li>
 *   <li>"@diagram worlds": one of {@link #DIAGRAMS}, drawn by the client (CodexDiagrams);</li>
 *   <li>"@beast iron_hide_boar": a Bestiary page's creature, turning in its frame, with what it is and what it drops.</li>
 * </ul>
 * The Bestiary's pages (the user's pick, 2026-10-28) open when their creature is first seen up close (CodexState), not by advancements.
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
		IMMORTAL("immortal", "Only immortals know of this."),
		SIGHT("sight", "Come upon this creature to record it.");

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

	/**
	 * A page: what opens it (advancement ids under the mod's namespace, any one of them; empty: open from the first day), or for the
	 * Bestiary's, the creature that opens it once seen ({@code sight}, an entity type's id: CodexState notes what its player has come upon).
	 */
	public record Entry(String id, String chapter, ItemLike icon, List<String> unlocks, Hint hint, String title, String text, String sight) {
		public boolean open() {
			return unlocks.isEmpty() && sight == null;
		}

		public String titleKey() {
			return ModLang.codexEntryTitle(id);
		}

		public String textKey() {
			return ModLang.codexEntryText(id);
		}
	}

	/** The keys a page may name ({@code {menu}}): the mod's own and a few of vanilla's, each shown as it is bound now. */
	public static final Set<String> KEYS = Set.of("menu", "meditate", "circulate", "abilities", "quick", "spells", "cast", "codex",
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

	/** Every creature the Bestiary has a page for (Entry#sight: entity type ids). */
	public static Set<String> sights() {
		Set<String> sights = new HashSet<>();
		for (Entry entry : ENTRIES) if (entry.sight() != null) sights.add(entry.sight());
		return sights;
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

	// --- Pictures ---

	/**
	 * The pictures a page may hold ("@items ..."), and the diagrams (client CodexDiagrams: the map of the worlds and the rest). "@beast id"
	 * is a Bestiary page's creature (the mod's entity type): its likeness turning, what it is, and what it drops.
	 */
	public static final Set<String> PICTURES = Set.of("items", "brew", "craft", "diagram", "beast");
	public static final Set<String> DIAGRAMS = Set.of("worlds", "realms", "elements");

	/** The picture a line of a page is ("items", for "@items jade"), or null if it is words. */
	public static String pictureKind(String line) {
		if (!line.startsWith("@")) return null;
		int space = line.indexOf(' ');
		return space < 0 ? line.substring(1) : line.substring(1, space);
	}

	/** What a picture's line names after its kind, one word each. */
	public static List<String> pictureParts(String line) {
		int space = line.indexOf(' ');
		return space < 0 ? List.of() : List.of(line.substring(space + 1).strip().split("\\s+"));
	}

	/** A name a picture gives ("jade", "minecraft:bone_meal"), as an id: the mod's own unless it names another; null if it can't be one. */
	public static ResourceLocation pictureId(String name) {
		return ResourceLocation.tryParse(name.contains(":") ? name : DefyingTheHeavens.MOD_ID + ":" + name);
	}

	/** The item a picture's word names ("jade*4": four Jade), or null if there is no such item. */
	public static Item pictureItem(String word) {
		int star = word.indexOf('*');
		ResourceLocation id = pictureId(star < 0 ? word : word.substring(0, star));
		return id != null && BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : null;
	}

	/** How many of its item a picture's word shows: 1, or the number after "*" (0 if that isn't one). */
	public static int pictureCount(String word) {
		int star = word.indexOf('*');
		if (star < 0) return 1;
		try {
			return Integer.parseInt(word.substring(star + 1));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/** What is wrong with a page's pictures (nothing, if they are written right); a craft's recipe is the server's to know (CodexGameTests). */
	public static List<String> pictureProblems(String text) {
		List<String> problems = new ArrayList<>();
		for (String line : text.split("\n")) {
			String kind = pictureKind(line);
			if (kind == null) continue;
			List<String> parts = pictureParts(line);
			switch (kind) {
				case "diagram" -> {
					if (parts.size() != 1 || !DIAGRAMS.contains(parts.get(0))) problems.add("no such diagram: " + line);
				}
				case "craft" -> {
					if (parts.size() != 1 || pictureId(parts.get(0)) == null) problems.add("one recipe id: " + line);
				}
				case "beast" -> {
					ResourceLocation id = parts.size() == 1 ? pictureId(parts.get(0)) : null;
					if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) problems.add("no such creature: " + line);
				}
				case "brew" -> {
					Item item = parts.size() == 1 ? pictureItem(parts.get(0)) : null;
					if (item == null || AlchemyRecipes.forOutput(item) == null) problems.add("nothing brews it: " + line);
				}
				case "items" -> {
					if (parts.isEmpty()) problems.add("no items: " + line);
					for (String part : parts) {
						if (part.equals("+") || part.equals(">")) continue;
						int count = pictureCount(part);
						if (pictureItem(part) == null || count < 1 || count > 64) problems.add("no such item: " + part);
					}
				}
				default -> problems.add("no such picture: " + line);
			}
		}
		return problems;
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
		add(new Entry(id, writingId, icon, unlocks == null ? List.of() : List.of(unlocks.split("\\|")), hint, title, text.strip(), null));
	}

	/** A Bestiary page: opened once its creature ({@code entity}, the mod's own) is seen, its picture first ("@beast"), then its words. */
	private static void creature(String entity, ItemLike icon, String title, String text) {
		add(new Entry("beast_" + entity, writingId, icon, List.of(), Hint.SIGHT, title, "@beast " + entity + "\n" + text.strip(),
				DefyingTheHeavens.MOD_ID + ":" + entity));
	}

	private static void add(Entry entry) {
		for (Entry other : ENTRIES) if (other.id().equals(entry.id())) throw new IllegalStateException("Two Codex pages are " + entry.id());
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
				This is your guide to cultivation. Press {codex} to open it at any time, or click Codex at the foot of the cultivation menu's \
				tabs.
				The Codex grows with you. Most pages are sealed at first and show only ???, with a hint of how to open them. Reach a new \
				realm, learn a new art or set foot in a new world, and the pages about it open. A toast tells you when that happens, and a \
				gold square marks every page you haven't read yet.
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
				@brew marrow_cleansing_elixir
				Ginseng grows wild on the ground: right-click a plant to dig it up.""");
		page("cauldron", ModBlocks.ALCHEMY_CAULDRON, null, "The Alchemy Cauldron", """
				Pills are brewed, not crafted. Make an Alchemy Cauldron from a cauldron, two copper ingots and a gold ingot (the mod's \
				crafting recipes have their own tab in the recipe book).
				@craft alchemy_cauldron
				- Fill it with a water bucket.
				- Light a fire under it: fire, soul fire, a lit campfire, lava or a magma block.
				@items minecraft:flint_and_steel minecraft:campfire minecraft:soul_campfire minecraft:lava_bucket minecraft:magma_block
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
				- {codex}: the Dao Codex""");
		page("physique", ModItems.SPIRIT_GINSENG, null, "Your Physique", """
				Every cultivator is born with a physique. Yours was rolled when you first joined, announced in chat, and is shown on the \
				Stats tab.
				Its grade (Low, Mid, High, Supreme or Immortal) is your talent: the higher it is, the faster you cultivate and the fewer \
				years meditation costs you. Many physiques also have special gifts, and every physique has spiritual roots, which decide \
				which elements' methods you can learn.
				The higher the grade, the rarer it is.""");
		page("bodies", Items.SKELETON_SKULL, null, "The Bodies of the Dead", """
				When a player or a cultivator dies, their body stays where they fell, and everything they carried stays with it instead of \
				scattering. Break the body to take it back: it spills everything it holds.
				A body is fresh for two minutes, then rots, and after five minutes only bones are left. Bones lie there until someone breaks \
				them, so what you carried waits for you, even while your soul is among the dead.
				- Right-click a body to see whose it was.
				- Right-click a fresh cultivator's body with a glass bottle to draw its blood, once: Cultivator's Blood of their realm. \
				Drawing it darkens your path.
				- The dead give off ghostly qi around them, more for a higher realm and less once they are bones, so a ghost cultivator can \
				cultivate beside them.
				Experience still drops as it always has.""");

		// ================================================ The Way of Cultivation ================================================
		chapter("way", ModItems.CULTIVATION_PILL, Hint.CULTIVATE, "The Way of Cultivation");
		page("stages", Items.EXPERIENCE_BOTTLE, QR, "Realms and Stages", """
				Cultivation climbs through realms, and each realm has four stages: Early, Mid, Late and Grand Perfection. Meditation fills \
				the cultivation bar on the Cultivation tab.
				@diagram realms
				The ladder names each realm once you reach it, and shows where its qi is rich enough to cultivate it. Your own realm is \
				marked in gold.
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
				@craft heaven_altar
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
				Lingzhi, Spirit Ginseng and Jade), and to survive a weak tribulation.
				@brew foundation_pill""");
		page("foundation_building", ModItems.FOUNDATION_PILL, FB, "Foundation Building", """
				Your foundation is laid. Your qi now sustains you, so you no longer get hungry. Hold a sword and you can ride it through the \
				air (Sword Flight).
				To break through, you need a Core Formation method, a Core Pill (Spirit Ginseng, Spirit Lotus, a Cultivation Fruit and Blaze \
				Powder), and to survive a tribulation that calls for armour.
				@brew core_pill""");
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
				Your physique's spiritual roots decide which elements you can learn: a method whose element your roots reject won't take.
				# The cycle of the five
				Each element overcomes one other: fire melts metal, metal fells wood, wood breaks earth, earth dams water and water quenches \
				fire.
				@diagram elements
				Shielding spells ward you with their element. A ward holds best against the element it overcomes (a water ward stops \
				fire), while the element that overcomes it strikes harder through it. Point at an element to see which it overcomes.
				Each also gives rise to another, in a cycle of its own: wood feeds fire, fire leaves earth in its ash, earth bears metal, \
				metal gathers water, and water feeds wood.""");
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
				The techniques so far are Clone and Universe in a Sleeve, and the secret realms' Sword Intent and Blood Battle (see \
				Abilities).""");

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
				stays sitting where you left it. Meditation there is a quarter faster, and costs a fifth fewer years.
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
				A projected soul can take over a body at least one realm below the highest of your paths: right-click it. In a \
				cultivator's body you play as yourself, with everything it carries, and others see its face and name.
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
		page("sword_intent", ModItems.SLEEPING_SWORD, CF, "Sword Intent", """
				A technique for Core Formation and above, the Sunken Sword Tomb's Great Inheritance. Switch it on and your intent rides \
				your blade: every blow you land with a sword is followed a moment later by a second cut of lingering intent, half as \
				strong as the blow.
				It costs a little qi every second while it's on, and goes out when you run short. The tomb's own lingering intent never \
				cuts one whose Sword Intent is lit.""");
		page("blood_battle", ModItems.WAR_GENERALS_BANNER, NS, "Blood Battle", """
				A technique for Nascent Soul and above, the Ancient Battlefield's Great Inheritance. Switch it on and battle feeds you: \
				every blow you land heals you a tenth of it.
				With more than one foe set on you close by, each one past the first takes a tenth off the harm their blows do you, up to \
				half.
				It costs a little qi every second while it's on, and goes out when you run short.""");

		// ================================================= Treasures and Alchemy =================================================
		chapter("treasures", ModItems.LOW_SPIRIT_STONE, Hint.FIND, "Treasures and Alchemy");
		page("spirit_stones", ModItems.LOW_SPIRIT_STONE, null, "Spirit Stones", """
				Spirit stones are heaven and earth's qi condensed into stone: the cultivators' money, and a store of qi. There are five \
				grades (Low, Mid, High, Supreme and Immortal), each worth nine of the grade below. Nine stones craft into one of the next \
				grade, and one breaks back down into nine.
				@items low_spirit_stone > mid_spirit_stone > high_spirit_stone > supreme_spirit_stone > immortal_spirit_stone
				@craft mid_spirit_stone_from_low
				- Hold right-click to absorb a stone's qi into your pool.
				- Meditate with one in your hand to cultivate faster while its qi lasts.
				They are mined from spirit stone veins in the Overworld's stone (the richer veins lie deepest), and cultivators carry them.""");
		page("fruit", ModItems.CULTIVATION_FRUIT, null, "Cultivation Fruit", """
				Cultivation Fruit is rare. It hangs under the leaves of wild trees and ages as the days pass: the older it is, the brighter \
				its aura and the more qi it holds. Right-click it to pick it. A picked fruit keeps its age but grows no older.
				Eating it gives cultivation, refined as you meditate. Now and then one leaves a Peach Pit, which grows into a Spirit Peach \
				Tree of your own.""");
		page("marrow_washing", ModItems.MARROW_WASHING_FRUIT, "worlds/secret_realm", "Marrow Washing", """
				The Marrow-Washing Fruit, half of the Cave Abode of a Heavenly Being's Great Inheritance, grew once in its master's garden. \
				Eat it and it washes your marrow: your physique rises a grade, Low to Mid, Mid to High and so on, and your talent and pace \
				with it. Its gifts and its spiritual roots stay as they were.
				It works once in a life, and does nothing for a physique of the Immortal grade, a mortal, one of the dead, or a soul in \
				another's body.""");
		page("herbs", ModItems.GINSENG, null, "Spirit Herbs", """
				Spirit herbs grow wild and age like fruit does: Ginseng and the rarer Spirit Ginseng, Lingzhi in dark forests, Huangjing, \
				the Spirit Lotus on still water, and Spirit Dew Grass, which gathers beads of dew. Right-click a herb to dig it up. It keeps \
				its age, and the older the herbs, the better the pills brewed from them.
				@items ginseng spirit_ginseng lingzhi purple_lingzhi huangjing ochre_huangjing spirit_lotus spirit_dew
				A fully grown plant gives seeds when you dig it up. Purple Lingzhi and Ochre Huangjing are rare kinds that count as twice \
				their age.""");
		page("pills", ModItems.QI_GATHERING_PILL, QR, Hint.CULTIVATE, "Pills", """
				Brewed in the Alchemy Cauldron:
				- Qi Gathering Pill: you gather more qi for a while.
				@brew qi_gathering_pill
				- Cultivation Pill: gives cultivation, refined as you meditate.
				@brew cultivation_pill
				- Foundation Pill and Core Pill: needed to break into Foundation Building and Core Formation.
				@brew foundation_pill
				@brew core_pill
				- Longevity Pill: adds years of life; each one adds fewer than the last.
				@brew longevity_pill
				A pill comes out at Low, Mid, High, Supreme or Immortal grade: the older its herbs, the higher the grade and the stronger \
				the pill. A practised alchemist does better still (see Craft Mastery).""");
		page("mastery", Items.ENCHANTED_BOOK, null, "Craft Mastery", """
				Alchemy, forging and formations are crafts, and each grows with practice through five ranks: Apprentice, Adept, Master, \
				Grandmaster and Saint. The Stats tab's Crafts page shows how far you have come in each.
				- Alchemy: every brew teaches you, and harder pills teach more. A pill harder than your rank can fail, ruining its herbs, \
				though you still learn half as much from it. The Marrow Cleansing Elixir never fails.
				- Each rank past Apprentice gives a chance of a pill one grade better than its herbs would give, never better than \
				herbs that age could give at all. Grandmasters and Saints sometimes draw two pills from one brew.
				- Forging: every artifact forged teaches you, and higher grades teach more. A grade harder than your rank can fail, and \
				each rank brings a chance of finer work (see Forging Artifacts).
				- Formations grow by crafting and refining them, and by keeping a Formation Core's barrier or concealment up.
				Your mastery is your own: it stays with you through death and rebirth.""");
		page("forge", ModBlocks.ARTIFACT_FORGE, null, "The Artifact Forge", """
				Artifacts are forged, not crafted. Make an Artifact Forge from five copper ingots, a blast furnace, two iron ingots and a \
				Low-Grade spirit stone.
				@craft artifact_forge
				- Light a fire under it, as under the Alchemy Cauldron. Lava, soul fire or a magma block burns hotter: the forge works 1.5 \
				times as fast over them.
				@items minecraft:campfire minecraft:soul_campfire minecraft:lava_bucket minecraft:magma_block
				- Right-click the forge with each material, one at a time. A material that fits nothing it could forge is refused, so \
				nothing is wasted.
				- Once everything for an artifact is in, right-click with an empty hand to begin. (Some artifacts start the same way, so \
				the forge waits for you.)
				- Stay within 5 blocks while it works: it draws qi from you every second, more for higher grades. If you walk away or run \
				out of qi, it stops until you are back.
				Right-click with an empty hand to see what is inside or how far along it is. Sneak and right-click to take the materials \
				back out; a working in progress stops, and the qi it used is lost.""");
		page("forging", ModItems.artifact(ArtifactKind.FLYING_SWORD, ArtifactGrade.SPIRIT), null, "Forging Artifacts", """
				Every artifact is forged at one of five grades: Mortal, Spirit, Earth, Heaven or Immortal. A grade takes its own metal, two \
				spirit stones of its own grade and, from Spirit up, a demon core:
				- Mortal: iron ingots and Low-Grade stones.
				- Spirit: gold ingots, Mid-Grade stones and a demon core of Qi Refining or above.
				- Earth: diamonds, High-Grade stones and a Core Formation core or better.
				- Heaven: netherite ingots, Supreme stones and a Nascent Soul core or better.
				- Immortal: Immortal Jade, Immortal stones and a Dao Integration core or better.
				Each kind of artifact is made the same way at every grade. At Mortal grade:
				@items minecraft:iron_ingot*2 + minecraft:stick + low_spirit_stone*2 > mortal_flying_sword
				@items minecraft:iron_ingot*3 + minecraft:leather + low_spirit_stone*2 > mortal_saber
				@items minecraft:iron_ingot + minecraft:stick*2 + low_spirit_stone*2 > mortal_spear
				@items minecraft:iron_ingot*2 + minecraft:stick*2 + low_spirit_stone*2 > mortal_glaive
				@items minecraft:white_wool*2 + minecraft:stick + low_spirit_stone*2 > mortal_element_banner
				@items minecraft:pumpkin + minecraft:glass_bottle + low_spirit_stone*2 > mortal_qi_gourd
				@items jade*2 + minecraft:string + low_spirit_stone*2 > mortal_protective_jade
				- A banner takes its element from its demon core. A Mortal banner has no core, so it takes a token instead: blaze powder \
				(fire), a prismarine shard (water), a sapling (wood), clay (earth) or an iron nugget (metal).
				- From Earth grade up, a gourd and a jade also take one of the grade's metal.
				- Higher grades take longer and draw more qi.
				Your forging rank (see Craft Mastery) decides how it turns out. A grade harder than your rank can fail: the materials burn \
				away, though a demon core comes back. Each rank past Apprentice adds 10 percent to the chance of Fine work, and each rank \
				from Master on adds 5 percent to the chance of Perfect work. A Fine artifact is 10 percent stronger in everything, a \
				Perfect one 25 percent.""");
		page("artifacts", ModItems.artifact(ArtifactKind.GOURD, ArtifactGrade.EARTH), null, "Artifacts", """
				What each artifact does. The higher its grade, the stronger it is.
				@items mortal_flying_sword > spirit_flying_sword > earth_flying_sword > heaven_flying_sword > immortal_flying_sword
				- Flying sword: made to be ridden. Sword Flight on it costs less qi: 10 percent less at Mortal grade, up to 50 percent \
				at Immortal.
				- Saber: heavy and slow, the hardest-hitting blade.
				- Spear: each blow also strikes up to 1 to 3 foes standing behind your target.
				- Glaive: each blow also strikes everyone close around you.
				- From Spirit grade up, every blade also makes all your blows stronger: 5 percent more at Spirit, up to 25 percent at \
				Immortal (a saber a little more).
				- Element banner: held in either hand, your spells of its element hit harder: 5 percent at Mortal, up to 20 percent at \
				Immortal. It doesn't add to a Sacred Vessel's power: the stronger of the two counts.
				- Qi gourd: stores spare qi for later. Sneak and right-click to pour your qi in, right-click to drink it back. Higher \
				grades hold far more.
				- Protective jade: worn in a ring slot (right-click it to put it on). Part of the harm you take (10 percent at Mortal, up to 30 percent at Immortal) \
				comes out of your qi instead of your health. It never softens a heavenly tribulation.
				Heaven and Immortal artifacts don't burn, and Immortal blades never wear out. Sect elders, sect masters, wandering rogues \
				and the Immortal Realm's merchants sell artifacts, and sect treasuries keep some.""");
		page("elemental_weapons", ModItems.artifact(ArtifactKind.FLAME_BLADE, ArtifactGrade.SPIRIT), null, "Weapons of the Elements", """
				Some weapons are not made of metal but of an element itself, given a weapon's shape by a demon core of that element. They \
				are forged at the Artifact Forge like any other artifact, from Spirit grade up.
				@items spirit_flame_blade spirit_tide_spear spirit_vine_whip spirit_mountain_hammer spirit_golden_light_sword
				Each takes its element's own matter, two spirit stones of its grade and a demon core of its own element: Qi Refining or \
				above for Spirit grade, Core Formation for Earth, Nascent Soul for Heaven and Dao Integration for Immortal.
				@items minecraft:blaze_rod*2 + minecraft:magma_cream + mid_spirit_stone*2 + demon_core > spirit_flame_blade
				@items minecraft:prismarine_crystals*3 + mid_spirit_stone*2 + demon_core > spirit_tide_spear
				@items minecraft:vine*2 + blue_spirit_log + mid_spirit_stone*2 + demon_core > spirit_vine_whip
				@items minecraft:obsidian*2 + jade_stone + mid_spirit_stone*2 + demon_core > spirit_mountain_hammer
				@items minecraft:gold_ingot*2 + minecraft:glowstone_dust + mid_spirit_stone*2 + demon_core > spirit_golden_light_sword
				Every blow adds part of your strike as its element's qi, which passes armour, and does what its element does:
				- Flame Blade: sets what it strikes burning.
				- Tide Spear: slows what it strikes and puts out its fire.
				- Vine Whip: roots what it strikes in place for a moment. The same foe can't be rooted again straight away.
				- Mountain Hammer: the heaviest and slowest. It staggers what it strikes, throwing it back and dazing it.
				- Golden Light Sword: the largest share of qi of any, cutting straight through armour.
				Right-click with one in your main hand to use its art. An art costs 8 percent of your qi, and it takes a few seconds before \
				you can use it again:
				- Flame Arc: a sweep of fire in front of you.
				- Surging Tide: ride a wave forward, striking and slowing everything in the way.
				- Binding Lash: lash a foe at a distance and root it.
				- Mountain Quake: slam the ground and stagger everything around you.
				- Golden Crescent: a crescent of golden qi that cuts through everything in a line.
				They are mended with their own element's matter (a blaze rod, prismarine crystals, a vine, obsidian, a gold ingot), and \
				the Flame Blade never burns.""");
		page("mats", ModBlocks.SPIRIT_PEDESTAL, QR, Hint.CULTIVATE, "Mats and Pedestals", """
				A Meditation Mat, or the finer Red Silk Meditation Mat, speeds up your meditation: right-click it to sit down and meditate.
				@craft meditation_mat
				A Spirit Pedestal holds one Cultivation Fruit or herb, floating above it. Place pedestals close around where you meditate \
				and their qi feeds you: the older what they hold, the more it helps. Only the oldest few count.
				@craft spirit_pedestal""");
		page("clothing", Items.LEATHER_CHESTPLATE, null, "Cultivator Clothing", """
				Robes, guan, trousers and boots worn instead of armour, in five grades: Mortal, Spirit, Earth, Heaven and Immortal. Each \
				grade protects as well as the matching tier of armour, and also speeds up your qi gathering and meditation.
				@items mortal_cultivator_robe > spirit_cultivator_robe > earth_cultivator_robe > heaven_cultivator_robe > immortal_cultivator_robe
				Mortal clothing is crafted from wool and string, and each piece can be upgraded one grade at a time in the crafting grid, \
				keeping its enchantments. Cultivators wear it too, and sometimes drop it.
				@craft mortal_cultivator_robe""");
		page("storage_formations", ModBlocks.storageFormation(StorageFormations.Tier.LOW), FB, Hint.CULTIVATE, "Storage Formations", """
				A storage formation folds a space into a sealed block. Craft a Low-Grade one from a chest, four Low-Grade spirit stones and \
				four jade, and place it. Only you, and those you trust, can open it.
				@craft low_storage_formation
				Refine it (the Refine button beside its search line) to raise its grade: each grade holds more, and its seal is \
				stronger. The Immortal grade holds without end.
				@items low_storage_formation > mid_storage_formation > high_storage_formation > supreme_storage_formation > immortal_storage_formation
				- Its seal: no pickaxe, blast or spell breaks it. A stranger as strong as its seal (Foundation Building for Low, up to \
				True Immortal for Immortal) can force it open by staying beside it for a while, and you are warned when they start. A \
				broken seal stays open until you open the formation again.
				- Refining needs the next grade's spirit stones and more, a realm as high as the new seal, and from Supreme on, some \
				mastery of formations.
				- It stays where you set it down: nothing is carried off inside one. Once it is empty, sneak and right-click it with an \
				empty hand to take it up and set it down elsewhere.
				- Use its Trust button to name the players who may open it as you do.
				Sect treasuries keep their riches in storage formations too. An old Storage Ring you carry unfolds into a storage \
				formation of its size, with everything it held.""");
		page("jade", ModBlocks.JADE_ORE, null, "Jade and Spirit Wood", """
				Jade Ore is found in mountains, about as rare as diamonds; mine it with an iron pickaxe. Jade goes into the Foundation Pill, \
				spirit pedestals, storage formations and more.
				Blue Spirit Trees, spruces with blue qi flowing up their trunks, grow among the spruces of the taiga.""");
		page("sacred_vessels", ModItems.JIANMU_BOUGH_VASE, "arts/sacred_vessel|worlds/great_void", Hint.VENTURE, "Sacred Vessels", """
				Five treasures from the old tales, one for each element: the Jianmu Bough Vase, the Heaven Furnace Censer, the Nine \
				Provinces Ding, the Immortal-Forging Bell and the Sealed Ocean Gourd.
				@items jianmu_bough_vase heaven_furnace_censer nine_provinces_ding immortal_forging_bell sealed_ocean_gourd
				Each lies on a relic altar in the Great Void, and a guardian rises to protect it from anyone who comes near. Held in your \
				hand, a vessel makes spells of its element hit harder.""");
		page("chaos_treasures", ModItems.DAO_FRAGMENT, "arts/dao_fragment|arts/origin_stone|worlds/chaos_frontier", Hint.IMMORTAL,
				"Chaos Treasures", """
				Two treasures come from the Chaos Frontier:
				@items dao_fragment chaos_origin_stone
				- A Dao Fragment is a shard of one Law. An immortal can use it to gain half a level of insight into that Law.
				- A Chaos Origin Stone is hardened chaos, the material of a small world's seed. Chaos Fiends drop them, and Chaos Origin Ore \
				in the Frontier's land holds them.""");

		// ======================================================= Legends =======================================================
		chapter("legends", ModItems.legend(Legend.GANJIANG), Hint.SEEK, "Legends");
		page("legends", ModItems.legend(Legend.MOYE), "arts/legends", "Legendary Weapons", """
				Some weapons were never made at any forge you know. The legends say each was made once, long ago, and there is only one of \
				each in all the worlds. The world's cultivators talk of them: ask them for rumours.
				@items ganjiang moye tribulation_spear xuanyuan_sword sun_shooting_bow sea_fixing_needle
				@items pangu_axe azure_dragon_glaive white_tiger_saber vermilion_bird_fan black_tortoise_mace yellow_dragon_seal
				- One of a kind: there is only ever one true one. A copy, from a command or the creative menu, is a hollow echo with no \
				power at all.
				- Too heavy for the weak: below the lowest realm that can wield it, it slips from your hand.
				- Bonding: its powers answer only its owner. Meditate with it in your main hand for two minutes to bond with it. Whoever \
				takes it after you, from your body or anywhere else, must bond with it anew.
				- Awakening: it grows with you. Its powers wake in three stages, at its lowest realm, two realms above that and four \
				realms above that, and each stage hits harder than the last.
				- It comes back: one that falls out of the world or is left lying until it vanishes returns to its resting place a day \
				later, its trial or its keeper with it.""");
		page("ganjiang_moye", ModItems.legend(Legend.GANJIANG), "arts/legend_ganjiang_moye", "Ganjiang and Moye", """
				The paired swords, yang and yin, wielded from Core Formation.
				@items ganjiang moye
				- Ganjiang sleeps in a sword tomb on a mountain of the Overworld. Come near it and the tomb's old blades rise against you, \
				in two waves. Beat them both, then right-click the stone to draw the sword.
				- Moye lies in a yin pool among the Other Shore's flowers in the Netherworld. Come near and a woman's ghost rises from the \
				pool with the sword in her hand. Beat her and she drops it.
				- Ganjiang's blows add fire qi and set your foes burning. Moye's add yin, which passes armour, and chill them, and her \
				blows reach the dead in full.
				- Hold one in each hand, both bonded to you, and every blow strikes twice: yang fire and yin cold.
				- Right-click: Twin Dragons. A spirit of each sword you hold flies out and fights for you for a while. It costs 8 \
				percent of your qi.""");
		page("tribulation_spear", ModItems.legend(Legend.TRIBULATION_SPEAR), "arts/legend_tribulation_spear", Hint.IMMORTAL,
				"Heaven's Tribulation Spear", """
				A bolt of heavenly tribulation that never fell, wielded from True Immortal. Lei Zhen the Tribulation Breaker tamed it long \
				ago, and it drove him mad.
				@items tribulation_spear
				- Lei Zhen roams the Immortal Realm under his own storm. He is a Golden Immortal, and he attacks anyone who comes into his \
				consciousness. Slay him and he drops the spear.
				- To tame it, meditate with it in your main hand for two minutes, then survive the three bolts it calls down on you.
				- Each blow's lightning leaps on into the foes around its target.
				- Right-click: Heaven's Brand. A tribulation bolt strikes the foe you look at, for 10 percent of your qi. A player it \
				strikes is branded: every heavenly tribulation they face from then on brings more bolts, for good, up to twice as many.
				- Hold it in your hand through your own heavenly tribulation and it drinks every third bolt. None of those reach you, and \
				each one it drinks makes it a little stronger, for good.""");
		page("xuanyuan_sword", ModItems.legend(Legend.XUANYUAN), "arts/legend_xuanyuan", "The Xuanyuan Sword", """
				The Yellow Emperor's holy sword, the bane of evil, wielded from Nascent Soul by the righteous alone.
				@items xuanyuan_sword
				- It rests in a stone on a jade shrine at the highest peak near the heart of the Jade Firmament. Only a righteous \
				cultivator can draw it.
				- Drawing it wakes the shade of Chiyou, the war god the Yellow Emperor slew. The ox-headed shade hunts whoever drew the \
				sword. Strike it down, or get far enough away, and it is gone.
				- It will not stay with the devilish. If one of them holds it, it flies back to its stone. Its powers answer its owner \
				only while they are righteous.
				- Its blows add golden qi, twice as much against the devilish, demons and the dead, and they reach the dead in full.
				- Right-click: Yellow Emperor's Judgement. The sword falls from the sky, vast, where you look, twice as hard on the evil. \
				It costs 10 percent of your qi.""");
		page("sun_shooting_bow", ModItems.legend(Legend.SUN_BOW), "arts/legend_sun_bow", Hint.VENTURE, "The Sun-Shooting Bow", """
				The bow that shot down the suns, wielded from Heavenly Being.
				@items sun_shooting_bow
				- A dead sun fell on the barren or ashen world of the Astral Realm nearest the Overworld, on its east-facing side. The \
				bow lies in the cinder at the heart of its crater.
				- The Golden Crow keeps the crater, and rises when you come near. While it lives, the bow cannot be drawn.
				- It needs no arrows. Draw it and let go, and it looses an arrow of sun qi that burns what it strikes.
				- Sneak and let go at full draw: Falling Sun. A sun falls where you aim and bursts into sunfire. It costs 10 percent of \
				your qi.""");
		page("sea_fixing_needle", ModItems.legend(Legend.SEA_NEEDLE), "arts/legend_sea_needle", Hint.IMMORTAL, "The Sea-Fixing Needle", """
				The gold-banded iron pillar that once held the East Sea still, wielded from Entering Nirvana.
				@items sea_fixing_needle
				- It stands as a pillar over the Immortal Realm's open sea of clouds. Lay your hand on it at Entering Nirvana or above \
				and it shrinks into your hand.
				- Pulling it shakes the sea: everyone near is thrown back, and Ao Guang, Dragon King of the East Sea, rises to take it \
				back. He is a True Immortal.
				- You keep it behind your ear: it is never dropped when you die.
				- Its blows add earth qi and throw your foe back.
				- Right-click: As You Will. It stretches out where you look, striking everyone along it. Sneak and right-click: it grows \
				to a mountain and comes down where you look. Each costs 10 percent of your qi.""");
		page("pangu_axe", ModItems.legend(Legend.PANGU_AXE), "arts/legend_pangu_axe", Hint.IMMORTAL, "Pangu's Axe", """
				The axe that split heaven from earth, wielded from Earth Immortal, the realm past True Immortal.
				@items pangu_axe
				- It lies in the heart of the Primordial Chaos, out in the Immortal Realm's Chaos Frontier. Slay the Chaos and the axe \
				falls out of it. Lost, it is in the heart of the next Chaos.
				- Its blows add metal qi, and they pass every ward: no aura, Golden Bell, ward or protective jade turns them.
				- No barrier holds against it. Its wielder can break any barrier, and every blow on one shatters it.
				- Foes' Law domains have no hold on its wielder, and a blow on a domain's owner folds their domain.
				- Right-click: Split Heaven and Earth. A cleave runs along the land where you look and a tear in space opens down its \
				length. Everyone near it is struck and thrown aside, and every barrier and Law domain on it breaks. A player's also \
				splits the land. It costs 10 percent of your qi.""");
		page("venerables_weapons", ModItems.legend(Legend.AZURE_GLAIVE), "arts/legend_venerables", Hint.IMMORTAL, "The Venerables' Weapons", """
				The Four Symbols and the Yellow Dragon: the Celestial Court's five Heavenly Venerables each hold the strongest weapon of their \
				element, wielded from True Immortal.
				@items azure_dragon_glaive white_tiger_saber vermilion_bird_fan black_tortoise_mace yellow_dragon_seal
				- Azure Dragon Glaive (wood): its roots slow. Art: Azure Dragon.
				- White Tiger Saber (metal): the keenest edge. Art: White Tiger's Pounce.
				- Vermilion Bird Fan (fire): sets foes burning. Art: Vermilion Bird's Descent.
				- Black Tortoise Mace (water): chills and throws. Art: Black Tortoise Bastion.
				- Yellow Dragon Seal (earth): staggers, and brings fliers down. Art: Seal of Ten Thousand Mountains.
				- Each art is its symbol's own spell, cast through the weapon at your own realm, for 10 percent of your qi.
				- Dao Duel: a General of the Court can challenge a Venerable for its weapon. Talk to it, choose Spar, then the Dao Duel, \
				holding 32 Court Merit Tokens as your stake. It holds back to your realm. Win, and it hands you its weapon and keeps \
				its post; lose, and it keeps the tokens. One Dao Duel a day with each Venerable.
				- A Venerable's weapon that is lost returns to its Venerable's hand.""");

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
				Craft a Formation Core of your own to raise a barrier for yourself and the people you trust.
				@craft formation_core""");
		page("concealment", ModBlocks.FORMATION_CORE, null, "Concealment", """
				A Formation Core you own can also raise a concealment: its second tab. Inside its sphere, everyone else sees the land as it \
				was before any player changed it. Your buildings, your digging, the barrier itself, and the players inside are all hidden, \
				along with their sounds and particles.
				- You, the players you trust, and anyone stronger than you see through it. Walking inside reveals nothing.
				- It hides, but it doesn't block: outsiders walk straight through what they can't see, and over the ground they see. They \
				can't mine or use the hidden blocks. Raise the barrier as well to keep them out: with both up, your home is unseen and \
				no one can enter.
				- It has its own radius and switch, and costs three quarters of a barrier's upkeep, from the same Qi Veins and store. Both \
				can run at once if your veins carry them. When the qi runs short, the concealment falls before the barrier does.
				- Mastery of formations lowers both upkeeps, and keeping them up teaches it.
				The world remembers what stood in every spot players have changed, so a concealment raised long after you built still \
				hides everything.""");
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
		page("missions", ModBlocks.MISSION_BOARD, "sects/joined", Hint.SECT, "The Mission Board", """
				Every sect keeps a mission board on its plaza, by its Formation Core. Each day it posts five missions for each of its \
				disciples, fitted to their realm: Easy, Fitting or Hard. You may hold three at once.
				@items mission_board mission_slip sealed_letter
				- Hunt a beast that troubles the sect's lands, or purge a cultivator who preys on its people. Hold the mission's slip to be \
				shown the way: the quarry comes out, glowing, when you draw near, and fights only you.
				- Gather what the sect's stores want, or brew pills and forge artifacts for it, and hand them in at the board.
				- Carry a sealed letter to another sect, and hand it to any of its people.
				- Keep watch at the gate for three minutes while waves come at it.
				A mission seen through pays Contribution and raises the sect's regard for you; one left to lapse lowers it. Spend \
				Contribution at the board's exchange on pills, herbs, manuals, artifacts and more. The better wares are kept for Inner \
				Disciples and Elders. If you leave the sect, your Contribution stays with it.""");
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
		page("primeval_blood_essence", ModItems.PRIMEVAL_BLOOD_ESSENCE, "worlds/secret_realm", "Primeval Blood Essence", """
				The blood of the primeval dragon whose bones lie in the Primeval Beast Graveyard, the blood every beast's blood came from: \
				that realm's Great Inheritance.
				- A demon who drinks it wakes its blood a realm further, as far as Entering Nirvana.
				- A half-blood's own race's blood grows purer, as if it had drunk that race's blood a realm above its other paths.
				- A human who drinks it wakes a dragon's blood in them and becomes a Dragon half-blood. You are warned first, and must \
				drink it sneaking: there is no way back.
				The dead and mortals can't drink it.""");
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

		// ======================================================= Bestiary =======================================================
		// The user's pick, 2026-10-28: a page for every creature, sealed until it is seen up close (CodexState), its picture first.
		chapter("bestiary", Items.SPYGLASS, Hint.SIGHT, "Bestiary");
		creature("iron_hide_boar", ModItems.beastEgg(BeastKind.IRON_HIDE_BOAR), "Iron-Hide Boar", """
				A boar whose hide is as hard as iron plate. When anything comes near, it lowers its head and charges in a straight line, \
				throwing back whatever it hits. It roams forests, taiga, plains and meadows.""");
		creature("shadow_wolf", ModItems.beastEgg(BeastKind.SHADOW_WOLF), "Shadow Wolf", """
				Dark wolves that hunt in packs of three to five and fight as one: hurt one and the whole pack turns on you. At night \
				they hunt harder still. They run through taiga, dark forests and snowy lands.""");
		creature("jade_viper", ModItems.beastEgg(BeastKind.JADE_VIPER), "Jade Viper", """
				A green viper whose bite poisons, and the stronger the viper, the worse its venom. It hides in jungles, swamps and \
				flower forests. Kin to the lizard race, it sometimes drops a phial of Lizard Blood.""");
		creature("rock_shell_turtle", ModItems.beastEgg(BeastKind.ROCK_SHELL_TURTLE), "Rock-Shell Turtle", """
				A peaceful turtle with a shell of stone. It only fights back, and when hurt it draws into its shell, where little can \
				reach it. It lives on beaches, on riverbanks and in swamps.""");
		creature("wind_hawk", ModItems.beastEgg(BeastKind.WIND_HAWK), "Wind Hawk", """
				A hawk that circles high above mountains, hills and badlands, dives on its prey and climbs away again before you can \
				strike back.""");
		creature("fire_monkey", ModItems.beastEgg(BeastKind.FIRE_MONKEY), "Fire Monkey", """
				Monkeys that come in troops of two or three and hurl burning stones from a distance. They live in jungles, savannas, \
				badlands and deserts.""");
		creature("bloodfang_tiger", ModItems.beastEgg(BeastKind.BLOODFANG_TIGER), "Bloodfang Tiger", """
				A red-striped tiger that leaps onto its prey from a distance. It hunts in jungles, savannas and dark forests. Kin to \
				the tiger race, it sometimes drops a phial of Tiger Blood.""");
		creature("frost_ape", ModItems.beastEgg(BeastKind.FROST_APE), "Frost Ape", """
				A great white ape of the snowfields. It slams the ground with a burst of frost that slows everything around it. It \
				lives on snowy plains, slopes and peaks.""");
		creature("earthquake_bear", ModItems.beastEgg(BeastKind.EARTHQUAKE_BEAR), "Earthquake Bear", """
				A huge bear whose pounding makes the ground heave around it. It is found in taiga, on windswept hills and on stony \
				peaks.""");
		creature("iron_jaw_crocodile", ModItems.beastEgg(BeastKind.IRON_JAW_CROCODILE), "Iron-Jaw Crocodile", """
				Its bite holds on, and in the water it drags you under. It lurks in swamps, rivers and jungle waters. Kin to the \
				lizard race, it sometimes drops a phial of Lizard Blood.""");
		creature("lightning_antelope", ModItems.beastEgg(BeastKind.LIGHTNING_ANTELOPE), "Lightning Antelope", """
				A swift antelope that grazes in small herds on savannas, plains and meadows. It only fights back, charging with a \
				spark that slows whatever it hits.""");
		creature("ember_fox", ModItems.beastEgg(BeastKind.EMBER_FOX), "Ember Fox", """
				A fiery fox that breathes flame in a cone before it. It lives in deserts, badlands and savannas. Kin to the fox race, \
				it sometimes drops a phial of Fox Blood.""");
		creature("golden_winged_roc", ModItems.beastEgg(BeastKind.GOLDEN_WINGED_ROC), "Golden-Winged Roc", """
				A great golden bird of the high peaks. It circles, then dives, and the gust from its wings throws back everything \
				near it.""");
		creature("jade_scaled_python", ModItems.beastEgg(BeastKind.JADE_SCALED_PYTHON), "Jade-Scaled Python", """
				A vast serpent that coils around its prey, holding and crushing it. It hides in jungles and swamps. Kin to the lizard \
				race, it sometimes drops a phial of Lizard Blood.""");
		creature("horned_thunder_rhino", ModItems.beastEgg(BeastKind.HORNED_THUNDER_RHINO), "Horned Thunder Rhino", """
				A grey rhino whose charge stuns whatever it hits. It roams savannas, badlands and plains.""");
		creature("flood_dragon", ModItems.beastEgg(BeastKind.FLOOD_DRAGON), "Flood Dragon", """
				A dragon of the seas and rivers that never leaves the water. It strikes from afar with jets of water that knock you \
				back. Kin to the dragon race, it sometimes drops a phial of Dragon Blood.""");
		creature("elder_white_tiger", ModItems.beastEgg(BeastKind.ELDER_WHITE_TIGER), "Elder White Tiger", """
				The tiger race's elder. It rises from a pool of Tiger Blood on the Great Void's Ancient Battlefields to guard it, and \
				its pounce reaches far. It leaves alone anyone a realm or more above it.""");
		creature("nine_tailed_fox", ModItems.beastEgg(BeastKind.NINE_TAILED_FOX), "Nine-Tailed Fox", """
				The fox race's elder, guarding a pool of Fox Blood on the Great Void's Ancient Battlefields. Its fire breath reaches far. \
				It leaves alone anyone a realm or more above it.""");
		creature("mountain_lizard_king", ModItems.beastEgg(BeastKind.MOUNTAIN_LIZARD_KING), "Mountain-Bearing Lizard King", """
				The lizard race's elder, guarding a pool of Lizard Blood on the Great Void's Ancient Battlefields. When it pounds the \
				ground, the land heaves far around it. It leaves alone anyone a realm or more above it.""");
		creature("azure_dragon", ModItems.beastEgg(BeastKind.AZURE_DRAGON), "Azure Dragon", """
				The dragon race's elder, guarding a pool of Dragon Blood on the Great Void's Ancient Battlefields. It flies, circles \
				and dives with a great gust. It leaves alone anyone a realm or more above it.""");
		creature("kun_peng", ModItems.beastEgg(BeastKind.KUN_PENG), "Kun-Peng", """
				A fish as vast as an island, swimming in the Immortal Realm's sea of clouds. Bring it to half its strength and it \
				breaks from the clouds as the Peng, a bird whose wings cover the sky. It drops a True Immortal jade slip of its \
				element.""");
		creature("true_phoenix", ModItems.beastEgg(BeastKind.TRUE_PHOENIX), "True Phoenix", """
				A bird of fire that circles the Immortal Realm. Strike it down once and it falls as an ember and is reborn from its \
				ashes: you must slay it twice. It drops True Immortal Fox Blood.""");
		creature("true_dragon", ModItems.beastEgg(BeastKind.TRUE_DRAGON), "True Dragon", """
				The longest of the primordial beasts. It brings a storm with it and calls lightning down on its foes. It drops True \
				Immortal Dragon Blood.""");
		creature("primordial_chaos", ModItems.beastEgg(BeastKind.PRIMORDIAL_CHAOS), "Primordial Chaos", """
				What remains of Hundun, faceless chaos on six legs and four wings, in its lair far out in the Chaos Frontier. It is a \
				Golden Immortal, stronger than the other primordial beasts, and its Unmaking undoes what it touches. It drops Dao \
				Fragments, Chaos Origin Stones and Immortal Jade.""");
		creature("chaos_fiend", ModItems.beastEgg(BeastKind.CHAOS_FIEND), "Chaos Fiend", """
				Wraiths of smoke and stone that rise out of the Chaos Frontier's air, stronger the deeper you go. They circle their \
				prey loosing bolts of chaos, then dive to claw. They leave alone anyone a realm or more above them, and they burn away \
				in the First Flame's light.""");
		creature("golden_crow", ModItems.beastEgg(BeastKind.GOLDEN_CROW), "Golden Crow", """
				The sun's own crow, black and gold, with three legs. One keeps the crater of a dead sun on a barren world of the \
				Astral Realm, where the Sun-Shooting Bow lies in the cinder. It rises when someone comes near, and it is a Heavenly \
				Being at Grand Perfection. While it lives, the bow cannot be drawn.""");
		creature("hungry_ghost", Items.SOUL_LANTERN, "Hungry Ghost", """
				The dead who starved in life and hunger still, gaunt and cyan. They gather in packs of two to four, most often in the \
				Netherworld's Bone Wastes. Their blows strike the dead hard, and drive the Netherworld's yin into the living. A ghost \
				who disperses one takes some of its ghostly qi.""");
		creature("heart_demon", Items.ECHO_SHARD, "Heart Demon", """
				Your own shadow, risen in your Inner Realm during a qi surge from Nascent Soul on. It fights only you, matched to \
				your own strength. Slay it for a great surge of cultivation; if it wins, your soul is thrown back to your body.""");
		creature("void_vestige", Items.PHANTOM_MEMBRANE, "Vestige of the Void", """
				A black thing of the deep void, shaped like a dragonfish, larger with every stage of its strength. It hunts whoever \
				flies through open space, and the climbers of the Void Breach, but it turns away from a planet's air. It drops \
				spirit stones.""");

		// ===================================================== Worlds Beyond =====================================================
		chapter("worlds", Items.ENDER_EYE, Hint.VENTURE, "Worlds Beyond");
		page("map", Items.MAP, null, "Map of the Worlds", """
				@diagram worlds
				Every world, and which worlds lead to which. Point at a world to see the way in. A world you haven't been to yet shows \
				??? for everything else; once you have been there, it also shows its name, its qi and the realm you should reach before \
				you go.
				Qi is counted against the lower realms' qi, and ghostly qi against a soul sand valley's. A gold ring marks the world \
				you're in.""");
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
				@craft world_seed
				After that, you can open its door wherever you stand, for yourself and the guests you allow.
				Your world travels with you. Anyone inside it when you go through the Spatial Gap, the Void Breach or the Immortal Gate \
				comes through with you, whatever their realm, and its gate then leads out beside you. As with Universe in a Sleeve, the \
				spatial pressure on you doubles for each of them. If you die on the way, they stay safe inside, and its gate still leads \
				back to where they came in.""");

		// ================================================== The Secret Realms ===================================================
		chapter("secret_realms", ModItems.DEPARTURE_JADE, Hint.VENTURE, "Secret Realms");
		page("secret_realms", ModItems.DEPARTURE_JADE, QR + "|worlds/secret_realm", "Secret Realms", """
				Sealed worlds the ancients left behind, each under a Seal in the world: a ring of old pillars round a stone gate. Every few \
				years one stirs. A pillar of light rises from its Seal, and everyone strong enough to enter is told which way it lies and \
				roughly how far, but never exactly where.
				- Half a day later its gate fills with light and stays open for the first quarter of its time. Walk into it to go in.
				- Each realm has a lowest realm it lets in. There is no upper limit: anyone at or above it may enter, by their strongest \
				path. Only the living may enter (but the Yellow Springs Hollow, a realm of the dead), in their own bodies, carrying no one \
				in a sleeve or a world.
				- Inside, its qi is three times as dense as its world's, and that world's law still holds you.
				- The sky presses down. You can fly no higher than a few blocks above the ground, unless you stand two whole realms above \
				the realm it is meant for. Over the void between a floating realm's lands, the sky's hold counts from the land's level.
				- Space is sealed: no Spatial Tear, no small world's door, no sleeve. You leave early only by holding up a Departure Jade, \
				which a blow interrupts.
				- Two minutes before it closes, the land quakes. When it closes, everyone still inside is thrown out beside its Seal with \
				what they carry.
				- Whoever dies inside stays inside: their body, and everything they carried, lies there until someone finds it.
				Sects send their disciples, and rogues and devilish cultivators come too. They take what they find, force vaults and race \
				you through the realm's trials. Your own sect's disciples stand with you; the devilish go for anyone badly hurt.
				As a realm quakes, the strongest of the devilish inside may slip out early to lie in wait at its Seal. Whoever comes out \
				carrying more than they took in is their prey.""");
		page("verdant_herb_valley", ModItems.SPIRIT_GINSENG, "worlds/secret_realm", "The Verdant Herb Valley", """
				A misty valley of terraced herb gardens round a lake, from Qi Refining Late, meant for Foundation Building. It opens every \
				three years (three in-game days) for a quarter of an hour.
				- The terraces: beds of old herbs, a Vine Keeper lying beside each. Take a herb from a bed and its keeper wakes.
				- The ruins about the lake: the old alchemists' Pill Hall, Library, Furnace Court, Quarters, Stele Court and Medicine \
				Garden. Bronze Puppets guard them, and killing formations hide among them, plain under Qi Sense; break a formation's eye \
				to stop it. Their vaults are sealed formations: force one if you have its seal's realm, and the puppets near it wake.
				- Herbs nobody picks age a century for every year the valley sleeps.
				- Once each opening, the valley's heart blooms: a ten-thousand-year Spirit Ginseng flowers on the island for a minute, and \
				its Elder Keeper wakes.
				- The island: the Pill Sage's hall and his two trials. Defeat the Gatekeeper before its door; it fights you as an equal, \
				at your own strength. Then brew a Foundation Pill of Mid-Grade or better in his cauldron inside.
				@brew foundation_pill
				The first to pass both in this world takes his Great Inheritance: the Pill Sage's Cauldron, with its own fire, quicker \
				brews, fewer failures and pills a grade better, and his Notes, a whole rank of alchemy. After that, the first to pass each \
				opening takes a lesser legacy.
				Look for Departure Jade, Ancient Essence (one more stat point) and the Spring of the Ancients (faster cultivation for a \
				day) in its vaults.""");
		page("sunken_sword_tomb", ModItems.SLEEPING_SWORD, "worlds/secret_realm", "The Sunken Sword Tomb", """
				A gorge sunk deep into grey stone, from Foundation Building Late, meant for Core Formation. It opens every four years \
				(four in-game days) for twenty minutes.
				- You arrive on ledges at the heads of four branch canyons; stairs lead down them to the gorge's floor, strewn with the \
				swords of a sect that fell here. Its ruins stand about the floor, each with a vault, and Sword Wraiths keep them.
				- Lingering sword intent hangs in patches on the floor, glinting faintly, plainly under Qi Sense. Walk or sneak through it \
				and it leaves you be; run, fly or fight in it and it cuts you.
				- Some of the old swords in the floor still glint. Right-click one to draw it and take what it kept: a blade, a sword \
				spell's manual or spirit stones. The wraiths about it wake.
				- Some time into each opening the swords wake: for a minute, blades rise from the gorge and fall like rain about everyone \
				inside. A glint marks where each will land.
				- The spire at its heart: the Sword Path winds up its face to the Sleeping Sword's shrine on the summit. Walk it on foot \
				from its foot to the top. Its intent bears down harder the higher you climb, drags fliers down onto its steps and cuts \
				now and then; step off it and you must begin again.
				- Then the sword's spirit meets you before the shrine as an equal, at your own strength. Defeat it, then right-click the \
				sword in the shrine's stone.
				@items sleeping_sword
				The first in this world to pass both takes its Great Inheritance: the Sleeping Sword, a master's jian that wakes as its \
				bearer grows (Spirit grade, then Earth from Nascent Soul, Heaven from Four Axis and Immortal from Void Shattering), and \
				the manual of Sword Intent. After that, the first to pass each opening takes a lesser legacy.""");
		page("ancient_battlefield_realm", ModItems.WAR_GENERALS_BANNER, "worlds/secret_realm", "The Ancient Battlefield", """
				A red plain an old war was fought over, from Core Formation Late, meant for Nascent Soul. It opens every five years for \
				twenty minutes.
				- Trenches and earthworks ring the General's hill at its heart. War banners stand all over the plain, and the bones of \
				its dead lie everywhere, each with what it died with. Its yin is thick: its ghostly qi is three times the Netherworld's.
				- Bone Soldiers kneel where they fell, and rise for whoever comes close. A general's tent stands on each diagonal \
				between the trenches, its war chest sealed as a vault.
				- Some time into each opening the dead rise: for a minute and a half, old soldiers claw up out of the earth around \
				everyone inside.
				- The trial: hold the General's hill. Step into the ring of banners on its top and the war drum sounds; five waves of \
				Battle Spirits come up the hill at you, the last led by the General's Spirit. Leave the ring for more than a moment, or \
				fall, and you have lost it.
				- Held, the General's tomb in the hill's far side lets you through its barrier: go to his bier.
				@items war_generals_banner
				The first in this world to hold it takes his Great Inheritance: the War General's Banner and the manual of Blood Battle. \
				Held in either hand, the banner strengthens your side within sixteen blocks: you, your sect's fellows and those it calls \
				strike harder and are harder to harm. Raised, it calls four soldiers of the old army to fight beside you for forty \
				seconds, for a fifth of your qi, then rests five minutes. After that, the first to hold the hill each opening takes a \
				lesser legacy.""");
		page("primeval_beast_graveyard", ModItems.PRIMEVAL_BLOOD_ESSENCE, "worlds/secret_realm", "The Primeval Beast Graveyard", """
				A jungle basin walled by cliffs, from Core Formation Late, meant for Nascent Soul. It opens every six years for twenty \
				minutes.
				- The bones of a primeval dragon lie in a great curve across its heart: its skull open-jawed, its ribs arching over the \
				jungle floor, its tail tapering away.
				- Wild demon beasts gather here and grow past their kind on its bones, as strong as the realm it is meant for, each \
				keeping to its own ground. Three great ones keep about its ribs.
				- Old herbs grow in the dragon's shadow, older for every year it slept. Two beast tamers' lodges lie fallen in across the \
				basin, each with a vault.
				- Some time into each opening the bones roar: for a minute every beast in it goes wild, swifter and stronger, and rushes \
				the nearest visitor.
				- The trial: before the skull, the dragon's echo rises for whoever faces it, as an equal, at their own strength. Best it, \
				then step into the hollow of the skull, where its blood essence gathers.
				@items primeval_blood_essence
				The first in this world to do so takes the Primeval Blood Essence. After that, the first to best the echo each opening \
				takes a lesser legacy: dragon blood, a great beast's core and spirit stones.""");
		page("cave_abode", ModItems.MARROW_WASHING_FRUIT, "worlds/secret_realm", "The Cave Abode of a Heavenly Being", """
				A lone mountain in a basin of pines round a still lake, from Nascent Soul Late, meant for Heavenly Being. It opens every \
				seven years for twenty-five minutes.
				- Its abode is hollowed out of the mountain: the gate at its foot across the lake's causeway, a long hall with a library on \
				one side and a hall of spirit springs on the other, then the lecture hall. At its heart a garden lies open to the sky down \
				a shaft cut through the peak. Bronze Puppets guard its halls.
				- Bathe in the spirit springs and they mend you. The library's vault keeps manuals and jade slips; the springs', the \
				Spring of the Ancients.
				- Some time into each opening the master's voice lectures in the lecture hall for two minutes: meditate there and you \
				gather five times as much. Sit through enough of it and an Insight stays with you after.
				- The trials lie off the garden. First the hall of the five elements: light its five pillars in the order each element \
				gives rise to the next (see The Five Elements), beginning where you will. Touch one out of turn and they strike you back, \
				and all five go dark.
				- Then walk the maze to its heart. Its walls hold against every hand and spell.
				- Then the master's puppet before his chamber meets you as an equal, at your own strength. Best it, and his chamber's \
				barrier lets you through: go to his altar.
				@items marrow_washing_fruit
				The first in this world to pass takes his Great Inheritance: his own method, to Four Axis, and the Marrow-Washing Fruit. \
				After that, the first to pass each opening takes a lesser legacy.""");
		page("yellow_springs_hollow", ModItems.OTHER_SHORE_FLOWER, "worlds/secret_realm", "The Yellow Springs Hollow", """
				A drowned city of the dead in the Netherworld, from Nascent Soul Late, meant for Heavenly Being. It opens every five years \
				for twenty minutes, and it lets in the dead as well as the living. Inside it the dead lay hands on things as the living do.
				- Its islands stand out of murky golden water over the city's sunken streets, joined by bridges of black stone. Drowned \
				Shades keep them, and two of its houses keep vaults.
				- Other Shore Flowers grow on its islands, older for every year it slept.
				- At its heart the Ghost King's tomb stands on an island, ringed by the Yellow Springs themselves: deep water that drags \
				down and withers whoever swims it.
				- Some time into each opening comes the lantern night: for two minutes lanterns of the dead rise from the water, every \
				Other Shore Flower blooms three centuries older, the drowned rise, and the Ferryman carries everyone who asks.
				- The trial: the Ferryman's judgement at his landing. The righteous he takes across for nothing; the neutral for his toll, \
				three Yin Jade or two Mid spirit stones; the devilish must best him, as an equal, at their own strength. Right-click him \
				to be judged, and again to cross. Ring the bell on the far jetty to be taken back.
				- The tomb's door lets in those he has judged: go to the Ghost King's coffin.
				The first in this world to reach it takes his Great Inheritance: his own foundation method, the Six Paths Reincarnation \
				Canon, with Yin Jade from his tomb. After that, the first each opening takes a lesser legacy.""");
		page("jade_lake_mirage", ModItems.JADE_LAKE_MIRROR, "worlds/secret_realm", "The Jade Lake Mirage", """
				A still lake of pale jade under the Jade Firmament, from Four Axis Late, meant for Dao Integration. It opens every eight \
				years for twenty-five minutes.
				- White isles stand in the lake, joined by bridges of jade. Not every bridge is real: about a third have a span that is \
				only a mirage, and whoever walks onto it falls through into the lake. Mirror Shades keep the isles and the bridges.
				- Spirit lotuses grow by the isles, and mirage lotuses among them; two vaults stand on the isles, and two mirage caskets \
				in their likeness. A mirage dissolves into mist at a touch. Under Qi Sense a mirage shimmers, plain for what it is.
				- Some time into each opening the mirror wakes: for a minute, each visitor's reflection rises from the lake beside them, \
				at half their strength.
				- The trial: the Mirror Pavilion on the isle at its heart. Touch its great mirror and your reflection steps out of it, as \
				strong as you, in your armour, casting your own arts. Only your blows touch it. Best it, and the mirror is a door to you: \
				walk through it to the altar behind.
				@items jade_lake_mirror
				The first in this world to reach the altar takes the Jade Lake Mirror: raised, it turns the next three blows or spells \
				that strike you back on whoever struck them, then rests a minute. After that, the first each opening takes a lesser \
				legacy.""");
		page("fallen_holy_land", Items.CHISELED_QUARTZ_BLOCK, "worlds/secret_realm", "The Ruins of a Fallen Holy Land", """
				The shards of a holy land broken in some war of the ancients, floating in the Great Void, from Sacred Vessel Late, meant \
				for Entering Nirvana. It opens every ten years for half an hour.
				- Its shards hang over nothing: fall from one and you are caught and set back where you came in. Void currents run \
				between them, two to each pair, there and back. Step into one and it carries you to its far end.
				- The holy land's remnant guards keep the shards, and two of its broken halls keep vaults.
				- Some time into each opening comes the void tide: for a minute the currents surge, and the void tears anyone near a \
				shard's edge off it.
				- The trial: the ancestral hall on the greatest shard stands under a formation, its walls unbreakable and its door \
				barred. Four formation eyes on the shards about it hold it up, each kept by a guard. Bring the guard down, then break its \
				eye by hand. When all four are broken the formation falls, and the hall stands open to everyone for the rest of the \
				opening.
				The first in this world to reach its ancestral altar takes the holy land's ancestral sword, a flying sword of Heaven grade \
				and Perfect quality. After that, the first each opening takes a lesser legacy.""");
		page("fallen_immortal_tomb", ModItems.DAO_FRAGMENT, "worlds/secret_realm", "The Tomb of a Fallen Immortal", """
				A fallen immortal's palace on a floating base over a sea of golden cloud in the Immortal Realm, from Entering Nirvana \
				Late, meant for True Immortal. It opens every twelve years for half an hour.
				- Round its court stand eight halls, one for each Law, each door sealed by its Law: the seal lets through only one who \
				holds that Law. Inside each, a vault of that Law's Dao Fragments and immortal spirit stones.
				- Decayed celestial guards keep the court and the halls. The palace's walls can't be broken: go in by its doors.
				- Some time into each opening the Five Decays that took him spread through it: for a minute everyone's blows and armour \
				weaken.
				- The trial: his sanctum beyond the court, up its steps. Its door opens to one who holds three Laws, or to one who bests \
				his decayed spirit before it, as an equal, at their own strength.
				@items dao_fragment
				The first in this world to reach his coffin takes his Great Inheritance: his glaive, of Immortal grade and Perfect \
				quality, and Dao Fragments of three Laws. After that, the first each opening takes a lesser legacy.""");

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
				you know add to it. It refills on its own wherever you are, as your qi pool does, the faster the higher your ghost path, and \
				faster still where ghostly qi is thick: three times as fast in the Netherworld, about twenty times on the Ancient \
				Battlefields.""");
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
				@items peach_wood_sword inscription_brush ghost_quelling_talisman yang_fire_talisman yang_preservation_pill yin_gathering_pill
				- A Peach Wood Sword hits the dead with full force, and twice as hard.
				- An Inscription Brush paints talismans. The Ghost-Quelling Talisman, thrown, strikes the dead and binds them; the Yang Fire \
				Talisman, burned, drives the Netherworld's yin out of your body.
				- The Yang Preservation Pill stops yin corruption from rising; the Yin Gathering Pill, brewed from an Other-Shore Flower and \
				a Netherworld Lotus, speeds up the ghost path.
				@brew yin_gathering_pill
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
