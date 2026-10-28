package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What NPC cultivators say of their own accord (see NpcLife; spoken through NpcSpeech): remarks to a player passing by, the
 * weather and the time of day, the morning lecture, talk among themselves, practice bouts, greeting their seniors, the gate
 * watch, grief, alarms and breakthroughs. Each line has a few variants, one picked at random, and most are written once for
 * each path's voice: "righteous" formal and earnest, "neutral" terse, "devilish" mocking; and "commoner" for mortals (a mortal
 * with no line of its own speaks as the neutral do).
 * <p>
 * The English lives here rather than in the lang provider alone so the server knows how many variants each line has, and how
 * long each takes to read (NpcSpeech). ModEnglishLangProvider adds every line under {@code npc.say.<key>.<n>}, and every talk
 * between two ({@link #script}) under {@code npc.say.<key>.<n>.<line>}.
 * <p>
 * In a talk between two, {@code %1$s} is the name of the one being spoken to and {@code %2$s} the talk's subject (an elder's
 * name, a visitor's, a rumour).
 */
public final class NpcLines {
	private static final Map<String, List<String>> LINES = new LinkedHashMap<>();
	private static final Map<String, List<List<String>>> SCRIPTS = new LinkedHashMap<>();
	private static final Map<String, String> ENGLISH = new LinkedHashMap<>();

	/** The voice {@code npc} speaks in: its path's, or a commoner's; the Netherworld's dead speak as the dead. */
	public static String voice(CultivatorNpc npc) {
		if (npc.isGhost()) return "ghost";
		return npc.isMortal() ? "commoner" : npc.getFaction().getId();
	}

	/** A random variant of the line {@code key} (taken as it is, with no voice added), or null if there is no such line. */
	public static Component line(String key, RandomSource random, Object... args) {
		List<String> variants = LINES.get(key);
		if (variants == null) return null;
		return Component.translatable(ModLang.npcSay(key + "." + (1 + random.nextInt(variants.size()))), args);
	}

	/** {@code situation} in {@code npc}'s voice, or null if it has nothing to say to it. */
	public static Component line(String situation, CultivatorNpc npc, Object... args) {
		String key = situation + "." + voice(npc);
		if (!LINES.containsKey(key) && npc.isMortal()) key = situation + ".neutral";
		return line(key, npc.getRandom(), args);
	}

	/** Whether there is a line {@code situation} in {@code npc}'s voice. */
	public static boolean has(String situation, CultivatorNpc npc) {
		return LINES.containsKey(situation + "." + voice(npc)) || npc.isMortal() && LINES.containsKey(situation + ".neutral");
	}

	/**
	 * A random talk between two on {@code topic}, in {@code voice}: the translation keys of its lines in order, the one who starts
	 * it speaking first and they take turns; or null if there is none.
	 */
	public static List<String> script(String topic, String voice, RandomSource random) {
		String key = topic + "." + voice;
		List<List<String>> scripts = SCRIPTS.get(key);
		if (scripts == null && voice.equals("commoner")) scripts = SCRIPTS.get(key = topic + ".neutral");
		if (scripts == null) return null;
		int n = random.nextInt(scripts.size());
		List<String> keys = new ArrayList<>();
		for (int i = 0; i < scripts.get(n).size(); i++) keys.add(ModLang.npcSay(key + "." + (n + 1) + "." + (i + 1)));
		return keys;
	}

	/** How long the English of {@code translationKey} is, in characters; -1 if it isn't one of these lines. */
	public static int length(String translationKey) {
		String text = ENGLISH.get(translationKey);
		return text == null ? -1 : text.length();
	}

	/** Every line's translation key and its English (for ModEnglishLangProvider). */
	public static Map<String, String> english() {
		return Collections.unmodifiableMap(ENGLISH);
	}

	private static void lines(String key, String... variants) {
		LINES.computeIfAbsent(key, k -> new ArrayList<>()).addAll(Arrays.asList(variants));
		List<String> all = LINES.get(key);
		for (int i = all.size() - variants.length; i < all.size(); i++) ENGLISH.put(ModLang.npcSay(key + "." + (i + 1)), all.get(i));
	}

	private static void talk(String key, String... lines) {
		List<List<String>> scripts = SCRIPTS.computeIfAbsent(key, k -> new ArrayList<>());
		scripts.add(Arrays.asList(lines));
		int n = scripts.size();
		for (int i = 0; i < lines.length; i++) ENGLISH.put(ModLang.npcSay(key + "." + n + "." + (i + 1)), lines[i]);
	}

	static {
		// --- Waiting on the Shore of Nirvana for the Immortal Gate (NirvanaCamp), to a player passing by ---
		lines("shore.wait.righteous", "The Immortal Gate shows itself in its own time. We wait.", "A thousand years of cultivation, and still I wait upon this shore.",
				"When the Gate opens, only the swift pass. Rest while you can.", "Look north, over the void sea. That is where it opens.");
		lines("shore.wait.neutral", "Waiting.", "The Gate opens when it opens.", "Mind the sea. Nothing comes back from it.", "Another one come to wait?");
		lines("shore.wait.devilish", "Another hopeful? The Gate takes few.", "Keep clear of my place on this shore.", "When it opens, I'll be first. Remember that.",
				"Stand any closer and you can wait for the next life instead.");
		// --- A player passing by (%s: the player's name, or what they carry) ---
		lines("pass.liked.righteous", "Fellow Daoist %s! Well met again.", "Ah, %s. May your cultivation flourish.", "%s! You honour us with a visit.");
		lines("pass.liked.neutral", "Ah, %s. Road treating you well?", "Back again, %s?", "%s. Good to see you.");
		lines("pass.liked.devilish", "Well, if it isn't %s. Still alive, I see.", "%s! My favourite guest. Don't make me regret it.", "%s. You're almost tolerable.");
		lines("pass.liked.commoner", "Good day, %s!", "%s! Come round for tea sometime.", "Ah, %s is back!");
		lines("pass.disliked.righteous", "Hmph. You again.", "Mind your conduct here.", "I have not forgotten what you did.");
		lines("pass.disliked.neutral", "You. Keep walking.", "Don't stop here.", "Hm. Trouble's back.");
		lines("pass.disliked.devilish", "Look who crawled back.", "Come closer. I dare you.", "I remember your face. Fondly. Like a debt.");
		lines("pass.disliked.commoner", "It's that one... don't look.", "Please, Immortal, we want no trouble.", "Hide the rice, it's that one again.");
		// Disliked, but standing well above it (%1$s: what it calls them, NpcAddress; %2$s: their title or name).
		lines("pass.disliked_senior.righteous", "(stiffly) Greetings, %1$s.", "This one has not forgotten, %1$s. But greets you all the same.");
		lines("pass.disliked_senior.neutral", "(curtly) %1$s.", "...%1$s.");
		lines("pass.disliked_senior.devilish", "(through gritted teeth) %1$s. What an honour.", "%1$s. This one remembers you. Fondly.");
		lines("pass.disliked_senior.ghost", "The dead remember, %1$s. Even you.", "...%1$s.");
		lines("pass.disliked_senior.commoner", "(muttering) It's that one again...", "Please, %1$s, we want no trouble.");
		lines("pass.awe.righteous", "Such a presence... this junior greets Senior!", "Senior! This junior pays respects.", "The air itself grows heavy... a true expert.");
		lines("pass.awe.neutral", "That pressure... a senior.", "Best not to stare.", "Senior.");
		lines("pass.awe.devilish", "S-senior... this one is only passing through!", "What a monstrous aura... don't look at it.", "Ah! Senior! Lovely weather!");
		lines("pass.awe.commoner", "An Immortal! Bow, bow!", "Heavens... I can hardly breathe near them.", "Immortal, please, spare this humble one!");
		lines("pass.junior.righteous", "Work hard, little one. The path is long.", "A junior. Keep your heart steady.", "Mind your footing on this mountain, junior.");
		lines("pass.junior.neutral", "Out of the way, junior.", "Hm. Small fry.", "Don't waste my time.");
		lines("pass.junior.devilish", "What a tiny little aura. Adorable.", "An ant, walking upright. Cute.", "Run along before I get hungry.");
		lines("pass.mortal.righteous", "A mortal? Keep to the path; these mountains are dangerous.", "Mortal friend, mind the beasts after dark.", "Few mortals come this far. Be careful.");
		lines("pass.mortal.neutral", "A mortal. Stay out from underfoot.", "Mortal. Don't touch anything.", "Hm. Lost?");
		lines("pass.mortal.devilish", "A mortal. How brave. How stupid.", "Fresh meat, walking about on its own.", "Mortal. I could use a new servant.");
		lines("pass.peer.righteous", "Fellow Daoist.", "Peace on your path.", "A fine day for cultivation.");
		lines("pass.peer.neutral", "Traveller.", "Hm. Passing through?", "Watch your step.");
		lines("pass.peer.devilish", "Keep your hands where I can see them.", "Looking for trouble? You found the right place.", "Hm? Something you want?");
		lines("pass.peer.commoner", "Good day to you!", "Fine weather, isn't it?", "Safe travels, friend.");
		lines("pass.outsider.righteous", "A guest on our mountain? Mind the sect's rules.", "Visitors should present themselves at the main hall.", "An outsider... welcome, but don't wander.");
		lines("pass.outsider.neutral", "Outsider. Don't wander.", "Who let you in?", "Keep to the paths.");
		lines("pass.outsider.devilish", "An outsider. Did you lose your way, or your mind?", "Wander a little further. I'd like to watch.", "Visitors here tend to stay. Forever.");
		lines("pass.stone.righteous", "Is that a %s? Keep it hidden, friend.", "The qi in that %s... remarkable.");
		lines("pass.stone.neutral", "That %s will get you killed. Put it away.", "Nice %s. Don't wave it around.");
		lines("pass.stone.devilish", "My, what a pretty %s. Shame if someone took it.", "That %s would look better in my hand.");
		lines("pass.stone.commoner", "Heavens, it shines like the moon!", "Is that... an Immortal's jewel?");
		lines("pass.fruit.righteous", "I can feel the years in that fruit from here.", "A Cultivation Fruit? Guard it well.");
		lines("pass.fruit.neutral", "Fruit like that draws thieves.", "Eat it or hide it. Don't carry it about.");
		lines("pass.fruit.devilish", "Mm, that fruit smells delicious. So do you.", "Give me that fruit and I'll forget your face.");
		lines("pass.fruit.commoner", "What a strange fruit... it glows!", "Oh my, that fruit smells like heaven.");

		// --- A player passing by, by the rung they stand on before it (NpcAddress, NpcLife#remarkOn): %1$s is what it calls them (their
		// title, from Void Shattering on; Senior, Revered Senior, little friend...), %2$s their title or their name ---
		// One realm above it: it bows.
		lines("pass.senior.righteous", "Greetings, %1$s.", "This junior greets %1$s.", "May your path be smooth, %1$s.", "After you, %1$s. Please.");
		lines("pass.senior.neutral", "%1$s.", "After you, %1$s.", "Good day, %1$s.", "Mm. Greetings, %1$s.");
		lines("pass.senior.devilish", "Oh, %1$s. Was I in your way?", "Lovely day not to be fighting, %1$s.", "After you, %1$s. Always after you.",
				"Mm, %1$s. You look... healthy.");
		lines("pass.senior.ghost", "Your soul burns brighter than mine, %1$s.", "Pass, %1$s. The road is yours.", "The dead greet you, %1$s.");
		lines("pass.senior.commoner", "Good day, %1$s!", "Welcome, welcome, %1$s!", "Make way, it's %1$s!", "Blessings on you, %1$s.");
		// Two or three realms above it: a deep bow.
		lines("pass.esteemed.righteous", "This junior pays respects to %1$s!", "%1$s honours our humble mountain.", "Such depth... greetings, %1$s!",
				"To watch %1$s walk is a lesson in itself.");
		lines("pass.esteemed.neutral", "%1$s. This one greets you.", "That aura... my respects, %1$s.", "Make way. %1$s is passing.", "An honour, %1$s.");
		lines("pass.esteemed.devilish", "%1$s! This one was just leaving. Just leaving!", "Such pressure... my respects, %1$s!",
				"This one has never wronged anyone like %1$s. Never!", "Ah, %1$s! Do forgive my... everything.");
		lines("pass.esteemed.ghost", "Your light hurts to look at, %1$s.", "Even the dead lower their eyes, %1$s.", "This lost soul greets %1$s.");
		lines("pass.esteemed.commoner", "It's %1$s! Bow, everyone, bow!", "Please, %1$s, take whatever you need!", "My grandchildren won't believe I saw %1$s!",
				"Bless our village with your passing, %1$s!");
		// Four or five realms above it: it kneels as they pass.
		lines("pass.revered.righteous", "This humble junior kowtows to %1$s!", "Forgive this one for standing in your path, %1$s!",
				"To meet %1$s in this life... my fortune runs deep.", "The heavens seem to bow with you, %1$s.");
		lines("pass.revered.neutral", "This one kneels, %1$s.", "Head down. It's %1$s.", "This one meant no disrespect, %1$s.",
				"I can't stand under that pressure, %1$s...");
		lines("pass.revered.devilish", "Spare this worthless one, %1$s!", "This dog of a junior kneels to %1$s!", "I'll be good, %1$s. So very good.",
				"Not me, %1$s! I'm nobody! Nobody at all!");
		lines("pass.revered.ghost", "Even the dead kneel to %1$s.", "Your soul weighs like a mountain, %1$s.", "Pass us by, %1$s. Please, pass us by.");
		lines("pass.revered.commoner", "We kowtow, %1$s, we kowtow!", "Heavens preserve us... it's %1$s!", "Our village has done nothing wrong, %1$s!",
				"Like a god walking the earth... %1$s!");
		// Six realms or more above it: its forehead to the ground.
		lines("pass.ancestor.righteous", "This lowly one is unworthy to stand in %1$s's shadow!", "The legends walk among us... %1$s!",
				"To breathe the same air as %1$s... this junior is blessed!", "My whole sect would kneel with me, %1$s!");
		lines("pass.ancestor.neutral", "...%1$s.", "Heaven above. %1$s.", "Can't... breathe... %1$s...", "Eyes down. That is %1$s.");
		lines("pass.ancestor.devilish", "This insect begs to live, %1$s!", "I-I worship you, %1$s! I always have!", "Mercy, %1$s! I'll be your dog for ten thousand years!",
				"Don't even breathe. %1$s might hear.");
		lines("pass.ancestor.ghost", "Even the Wheel would stop for you, %1$s.", "The dead tell tales of ones like %1$s.", "Do not scatter us, %1$s. Please.");
		lines("pass.ancestor.commoner", "We kowtow, %1$s! We kowtow!", "Have the heavens come down? %1$s!", "Bless my children, %1$s, bless my fields!",
				"Lie flat, all of you! It's %1$s!");
		// It stands one realm above them: a word to a junior.
		lines("pass.above.righteous", "Keep at it, %1$s.", "Steady breathing, %1$s. You're close.", "A good foundation, %1$s. Don't rush it.",
				"Mind the beasts beyond the walls, %1$s.");
		lines("pass.above.neutral", "Hm. Keep training, %1$s.", "Busy. Walk on, %1$s.", "Not now, %1$s.", "Watch where you're going, %1$s.");
		lines("pass.above.devilish", "Out of my way, %1$s.", "Careful, %1$s. I bite.", "Are you lost, %1$s?", "Run along, %1$s. Grown-ups are talking.");
		lines("pass.above.ghost", "Walk softly, %1$s. The dead are resting.", "Hold on to your name, %1$s.", "You're new here, aren't you, %1$s?");
		// Two realms or more above them.
		lines("pass.far_above.righteous", "Work hard, %1$s. The road is long.", "Don't stray from the path, %1$s.", "Ah, to be at your stage again, %1$s.",
				"Patience, %1$s. Every mountain is climbed a step at a time.");
		lines("pass.far_above.neutral", "Out of the way, %1$s.", "Don't waste my time, %1$s.", "Small fry.", "Run along, %1$s.");
		lines("pass.far_above.devilish", "What a tiny aura, %1$s. Adorable.", "Shoo, %1$s.", "One breath and you'd be gone, %1$s. Remember that.",
				"I could swat you without looking, %1$s.");
		lines("pass.far_above.ghost", "So small, %1$s. So very small.", "The cold gets easier, %1$s. Then it gets worse.", "Little things scatter easily, %1$s. Be careful.");
		// Peers and mortals, more of them (the lines above take no name; these call them).
		lines("pass.peer.righteous", "Well met, %1$s.", "May the Dao guide you, %1$s.");
		lines("pass.peer.neutral", "Hm. %1$s.", "Mind the road, %1$s.");
		lines("pass.peer.devilish", "Eyes front, %1$s.", "Don't get any ideas, %1$s.");
		lines("pass.peer.ghost", "Wander well, %1$s.", "Another lantern on the road. Hello, %1$s.");
		lines("pass.peer.commoner", "Hello there, %1$s!", "Mind the cart, %1$s!");
		lines("pass.mortal.righteous", "Go carefully, %1$s. The road isn't safe.");
		lines("pass.mortal.neutral", "Mind yourself, %1$s.");
		lines("pass.mortal.devilish", "Run home, %1$s, while you still can.");
		lines("pass.mortal.ghost", "Breathe while you can, %1$s.");
		// A friend who stands above it.
		lines("pass.liked_senior.righteous", "%1$s! You honour this junior again.", "Welcome back, %1$s! Shall I brew tea?", "This one hoped you'd pass this way, %1$s!");
		lines("pass.liked_senior.neutral", "Good to see you again, %1$s.", "Back again, %1$s? Welcome.", "The road treats you well, I hope, %1$s.");
		lines("pass.liked_senior.devilish", "%1$s! My favourite senior.", "Ah, %1$s! This one kept your seat warm.", "Back so soon, %1$s? This one is delighted. Truly.");
		lines("pass.liked_senior.ghost", "You came back to us, %1$s.", "Sit with us a while, %1$s.", "We remember you, %1$s.");
		lines("pass.liked_senior.commoner", "%1$s is back! Bless you, bless you!", "Welcome back, %1$s! Stay for supper!", "The children will be so glad, %1$s!");
		// One of its own sect, come home.
		lines("pass.fellow.righteous", "Welcome home, %1$s.", "The sect is brighter with you back, %1$s.", "Back from the road, %1$s? Rest well.");
		lines("pass.fellow.neutral", "Back, %1$s?", "Good to have you home, %1$s.", "Welcome back.");
		lines("pass.fellow.devilish", "Back again, %1$s? The sect missed you. I didn't.", "Don't make trouble for the sect, %1$s.", "Home, %1$s? Bring anything back for me?");
		lines("pass.fellow.ghost", "You've come back to us, %1$s.", "Home, %1$s. Such as it is.", "The halls are quiet without you, %1$s.");
		lines("pass.fellow.commoner", "Welcome home, %1$s!", "Good to see you home safe, %1$s!");
		// One of its own sect's disciples passing, of the sect's missions (SectMissions#remark): their deed of the day (%3$s: "slew the
		// Bloodfang Tiger"), the gate watch they have taken and not begun, the day's postings they haven't looked at.
		lines("pass.mission.done.righteous", "%1$s! They say you %3$s. The whole sect is proud of you.", "Word is you %3$s, %1$s. Well done.",
				"%1$s, you %3$s? The elders spoke of it this morning.");
		lines("pass.mission.done.neutral", "Heard you %3$s, %1$s.", "You %3$s? Not bad, %1$s.", "%1$s. The one who %3$s.");
		lines("pass.mission.done.devilish", "So you %3$s, %1$s. Don't let it go to your head.", "You %3$s? Lucky, %1$s. Very lucky.",
				"%1$s %3$s... and lived. Interesting.");
		lines("pass.mission.watch.righteous", "%1$s, the gate watch is yours today. The gate guards are waiting.", "Is the gate watch not yours, %1$s?");
		lines("pass.mission.watch.neutral", "The gate watch is yours, %1$s. Don't be late.", "%1$s. The gate. Your watch.");
		lines("pass.mission.watch.devilish", "The gate watch is yours, %1$s. Try not to run.", "Off to the gate, %1$s. I'd hate for you to miss the fun.");
		lines("pass.mission.new.righteous", "%1$s, the elders have posted new missions on the board.", "New missions on the board today, %1$s. Have you looked?");
		lines("pass.mission.new.neutral", "New postings on the board, %1$s.", "The board's got fresh work, %1$s.");
		lines("pass.mission.new.devilish", "Fresh missions on the board, %1$s. Some of them might even kill you.", "The board's new today, %1$s. Go on, earn your keep.");

		// --- What it sees on one passing (NpcLife#remarkOn), from one who needn't bow low to say it; %1$s and %2$s as above ---
		// Flying past (Qi Flight, a flying sword, wings).
		lines("pass.flying.righteous", "Riding the wind, %1$s? A fine sight.", "Mind the sect's banners up there, %1$s!", "Ah, to soar like that one day...");
		lines("pass.flying.neutral", "Flying. Must be nice.", "Don't land on the roofs, %1$s.", "Hm. Show-off.");
		lines("pass.flying.devilish", "Flaunting it, %1$s? Come down here.", "Fly a little lower, %1$s. I'd like a closer look.", "One day I'll knock you out of the sky.");
		lines("pass.flying.ghost", "You float like one of us, %1$s.", "The living, flying? How strange.", "From up there, %1$s, the lanterns must look like stars.");
		lines("pass.flying.commoner", "Look! An immortal, flying!", "Up there! Riding the wind!", "Mama, look, a flying immortal!");
		// Badly hurt.
		lines("pass.hurt.righteous", "You're hurt, %1$s! Sit, rest a moment.", "Those wounds need tending, %1$s. Go to the healers.", "Who did this to you, %1$s?");
		lines("pass.hurt.neutral", "You're bleeding, %1$s.", "Bad fight, %1$s?", "Patch yourself up before you fall over.");
		lines("pass.hurt.devilish", "Bleeding, %1$s? I can smell it from here.", "Wounded... how very interesting.", "Limping, %1$s? Do be careful. Accidents happen.");
		lines("pass.hurt.ghost", "Your life is leaking out, %1$s. We can see it.", "Careful, %1$s. A little more and you'll be one of us.", "So much red... I'd forgotten the colour.");
		lines("pass.hurt.commoner", "Heavens, you're hurt! Sit, sit!", "Someone fetch water, they're bleeding!", "Are you all right? You look awful!");
		// Meditating where it walks.
		lines("pass.meditating.righteous", "Steady breath, %1$s. Good.", "Cultivating in the open? Mind your surroundings, %1$s.", "Shh. Don't disturb them.");
		lines("pass.meditating.neutral", "Hm. Meditating out here.", "Pick a safer spot next time, %1$s.", "Don't mind me.");
		lines("pass.meditating.devilish", "Eyes closed, out in the open? How trusting, %1$s.", "I could take everything you own right now, %1$s. Lucky I'm in a good mood.",
				"Sleep tight, %1$s.");
		lines("pass.meditating.ghost", "Still as the dead, %1$s. You're learning.", "Breathe in the yin, %1$s. Slowly.", "Shh. Let them sink.");
		lines("pass.meditating.commoner", "Shh! The immortal is meditating!", "Don't bother them, children.", "Should I bring them some tea?");
		// A beast's head on a person (a demon of the Demon Path), to one of human blood...
		lines("pass.demon.righteous", "A demon? Keep your claws sheathed on this mountain, %1$s.", "Demon blood... I'll be watching you, %1$s.",
				"Even a demon may walk the righteous path. May you, %1$s.");
		lines("pass.demon.neutral", "A demon. Behave, %1$s.", "Hm. Don't shed on anything.", "Demons. Everywhere, these days.");
		lines("pass.demon.devilish", "A demon! Now that's good company, %1$s.", "Mm, that bloodline... I could use a drop or two.", "Beast blood. Strong. I like it.");
		lines("pass.demon.ghost", "A beast's soul in a person's shape... how strange.", "Your blood howls, %1$s. Even we can hear it.", "Even death can't tell what you are, %1$s.");
		lines("pass.demon.commoner", "A d-demon! Inside, children, quick!", "Is that... a beast, walking like a person?", "Don't stare at it! Don't make it angry!");
		// ...and to its own kind (a demon of a clan).
		lines("pass.kin.righteous", "Kin! Well met, %1$s.", "The blood knows its own. Welcome, %1$s.", "May your bloodline thicken, %1$s.");
		lines("pass.kin.neutral", "Kin.", "Blood calls to blood, %1$s.", "Hm. One of us.");
		lines("pass.kin.devilish", "Kin! Hunt with me sometime, %1$s.", "Your blood's thin, %1$s. Mine is thicker.", "One of us. Don't trust the humans, %1$s.");
		// In the Five Decays (FiveDecays).
		lines("pass.decays.righteous", "Your robes are soiled and your flowers wilt, %1$s... the Five Decays.", "Even an immortal withers. Endure it, %1$s.",
				"Heaven's last test is the cruellest, %1$s.");
		lines("pass.decays.neutral", "The Decays are on you, %1$s.", "You smell of withered flowers, %1$s.", "Five Decays. Hm. Hold on.");
		lines("pass.decays.devilish", "Withering, %1$s? How the mighty fade.", "The Decays! I'll come for your treasures when you're dust.", "Rotting, %1$s? Don't come too close.");
		lines("pass.decays.ghost", "You fade, %1$s. As we all faded.", "Soon you'll walk with us, %1$s.", "The Decays... I remember them.");
		// Walking in a lit Law domain (LawDomains).
		lines("pass.domain.righteous", "The world bends around you, %1$s... a Law made manifest.", "Such a domain! Keep it in check here, %1$s.", "Your Law weighs on the very air, %1$s.");
		lines("pass.domain.neutral", "Put the domain away, %1$s.", "Your Law is pressing on everything, %1$s.", "Hm. A domain. Mind it.");
		lines("pass.domain.devilish", "Showing off your Law, %1$s? Charming.", "Turn that domain on me and see what happens.", "A Law domain, out for a stroll. Bold.");
		lines("pass.domain.ghost", "Your Law reaches even into death, %1$s.", "The Springs ripple around you, %1$s.", "Such a weight of Law... it hurts.");
		// An outsider on its sect's grounds after dark.
		lines("pass.night.righteous", "A guest on our grounds at this hour? State your purpose, %1$s.", "The guest hall is by the main gate, %1$s. You'll want to be there by now.",
				"It's late, %1$s. Our disciples are sleeping.");
		lines("pass.night.neutral", "Late to be wandering, %1$s.", "Night watch is out. Don't startle them.", "Off our grounds before dawn, %1$s.");
		lines("pass.night.devilish", "Creeping about after dark, %1$s? So am I.", "Thieves come at night. Are you a thief, %1$s?", "The night watch here kills first, %1$s.");
		lines("pass.night.commoner", "Out so late? The beasts come down at night!", "Who's there? Oh... a traveller.", "Mind the dark, the wolves are out.");

		// --- A tribulation nearby (NpcLife#tribulations) ---
		lines("tribulation.watch.righteous", "A heavenly tribulation! May they endure it!", "Those clouds... someone is breaking through!", "Heaven tests another. Watch, and learn.",
				"Such thunder... this is no small breakthrough.");
		lines("tribulation.watch.neutral", "Tribulation clouds. Stay back.", "Someone's breaking through. Keep your distance.", "Don't get caught under that.", "Big one.");
		lines("tribulation.watch.devilish", "A tribulation! Let's see if they survive it.", "If the heavens kill them, their treasures are mine.", "Strike harder, heavens! Harder!",
				"Ha! Defy them, then!");
		lines("tribulation.watch.ghost", "Heaven's lightning... it found me once, too.", "Someone else is about to join us.", "The thunder... I remember the thunder.");
		lines("tribulation.watch.commoner", "The sky! The sky is angry!", "An immortal is fighting the heavens!", "Get inside, it's a tribulation!", "Heavens have mercy on them...");

		// --- At the gate ---
		lines("gate.welcome.righteous", "Welcome back, %s. Enter in peace.", "%s! The gates are open to you.");
		lines("gate.welcome.neutral", "%s. Go on through.", "You're known here, %s. Pass.");
		lines("gate.welcome.devilish", "%s. Go in. Behave.", "Ah, %s. In you go, before I change my mind.");
		lines("gate.halt.righteous", "Halt, traveller. State your business with our sect.", "Visitors are welcome. Thieves are not.");
		lines("gate.halt.neutral", "State your business.", "Gate's watched. Don't try anything.");
		lines("gate.halt.devilish", "Turn back, or don't. I'm bored either way.", "One step closer and you're my afternoon's entertainment.");
		// To one who stands above it (%1$s: what it calls them, NpcAddress): known here...
		lines("gate.welcome_senior.righteous", "%1$s! Our gates are honoured. Please, enter.", "Welcome back, %1$s. The sect is at your service.", "%1$s returns! Open the way!");
		lines("gate.welcome_senior.neutral", "%1$s. Go on through.", "Welcome back, %1$s.", "The gate is open to you, %1$s.");
		lines("gate.welcome_senior.devilish", "%1$s! In, in, please. No need to knock.", "Welcome back, %1$s. This one never doubted you'd return.",
				"The gate is yours, %1$s. As is everything.");
		lines("gate.welcome_senior.ghost", "The gate opens for you, %1$s.", "Enter, %1$s. The dead remember their friends.");
		// ...or not.
		lines("gate.halt_senior.righteous", "%1$s... may this junior ask what brings you to our sect?", "Forgive the question, %1$s, but whom do you seek here?",
				"This junior must ask your business, %1$s. It is the sect's rule.");
		lines("gate.halt_senior.neutral", "%1$s. Your business here?", "Pardon, %1$s. I must ask why you've come.", "Who do you seek, %1$s?");
		lines("gate.halt_senior.devilish", "%1$s! What an... honour. Whatever brings you here?", "This one is only the gatekeeper, %1$s! Please, don't take it out on me!",
				"W-what does %1$s want with our humble sect?");
		lines("gate.halt_senior.ghost", "Why does one such as you come to the gate of the dead, %1$s?", "Your business, %1$s? Speak, and pass.");
		lines("watch.begin.righteous", "I'll keep the gate. Rest well.", "My watch begins. Heaven keep us.");
		lines("watch.begin.neutral", "My watch.", "I've got the gate.");
		lines("watch.begin.devilish", "My watch. Pray nothing interesting happens. Or do.", "Go and sleep. I'll guard your precious gate.");
		lines("watch.end.righteous", "The watch passes on. It's been quiet.", "All is well. The gate is yours.");
		lines("watch.end.neutral", "Quiet. Your turn.", "Done. Gate's yours.");
		lines("watch.end.devilish", "Nothing died. Disappointing. Your turn.", "Your gate now. Try not to lose it.");

		// --- Weather and the time of day ---
		lines("rain.righteous", "Rain. The herbs will be glad of it.", "Heaven waters the earth; a good sign.", "Let it rain. The qi is fresh after rain.");
		lines("rain.neutral", "Rain again.", "Wet. Lovely.", "There goes the training ground.");
		lines("rain.devilish", "Rain. Washes the blood away, at least.", "Ugh. Wet robes.", "Rain and gloom. My favourite.");
		lines("rain.commoner", "Rain! Get the washing in!", "The fields needed this.", "My knees said it would rain.");
		lines("rain.patrol.righteous", "A disciple's duty doesn't wait for fair weather.", "Rain or not, the perimeter must be walked.");
		lines("rain.patrol.neutral", "Patrol in the rain. Great.", "Every time I'm on rounds.");
		lines("rain.patrol.devilish", "Who decided we patrol in this muck?", "If anything attacks now, I'm letting it.");
		lines("thunder.righteous", "Thunder... is someone facing their tribulation?", "Heaven's wrath stirs. Be humble today.");
		lines("thunder.neutral", "Thunder. Someone's tribulation, maybe.", "Loud up there.");
		lines("thunder.devilish", "Ha! Some fool is being struck down by Heaven.", "Listen to it roar. Heaven is jealous again.");
		lines("thunder.commoner", "Thunder! Heaven protect us!", "Close the shutters!");
		lines("dusk.righteous", "The day is done. Rest well.", "Another day of cultivation behind us.");
		lines("dusk.neutral", "Lights out soon.", "Day's done.");
		lines("dusk.devilish", "Night. Finally, some peace.", "The best things happen after dark.");
		lines("dusk.commoner", "Time to head home.", "The lamps are lit. Off to bed.");
		lines("dawn.righteous", "Dawn. The morning qi is the purest.", "Up, up! The bell has rung.");
		lines("dawn.neutral", "Morning.", "Bell's rung. Up.");
		lines("dawn.devilish", "Morning already? Who rang that cursed bell?", "Another day, another fool to beat.");
		lines("dawn.commoner", "Morning! Back to work.", "Ah, the sun's up.");
		lines("sleep", "Zzz...", "Mm... five more breaths...", "...the third meridian... no... the fourth...", "Zzz... I'm awake, Elder... zzz...");

		// --- The morning lecture ---
		lines("lecture.open.righteous", "Settle your breath. Today we speak of the Dao.", "Sit straight, disciples. Let us begin.");
		lines("lecture.open.neutral", "Sit. Listen. We begin.", "Quiet. The lesson is starting.");
		lines("lecture.open.devilish", "Sit down and be quiet. Today you might even learn something.", "Eyes on me. The first to doze spars with me.");
		lines("lecture.righteous",
				"Qi follows intent, and intent follows the heart. Cultivate the heart first.",
				"A tree grows by the inch. Do not envy the bamboo its speed.",
				"When you breathe in, take in Heaven. When you breathe out, return what is not yours.",
				"A bottleneck is not a wall. It is a door you have not yet learned to open.",
				"Virtue steadies the foundation. Without it, every realm you climb is a cliff's edge.",
				"Heaven tests those it values. Welcome your tribulations.");
		lines("lecture.neutral",
				"Circulate your qi through all twelve meridians before you sleep. Every night.",
				"Talent is a head start. Persistence decides the race.",
				"Don't force a breakthrough. Fill the vessel first.",
				"Know your realm, and your enemy's. Most deaths are simple arithmetic.",
				"Pills help. Pills also poison. Learn which is which.",
				"Meditate where the qi is dense: on mats, near old herbs, up high. Use your head.");
		lines("lecture.devilish",
				"The Dao favours the strong. Be strong, or be someone's stepping stone.",
				"Mercy is a debt your enemy collects later. With interest.",
				"Steal a technique once and it's theft. Master it and it's yours.",
				"Heaven is not your friend. It's a rival with better weather.",
				"Your heart demons feed on hesitation. Starve them. Decide, and act.",
				"Every senior you bow to is a senior you will one day surpass.");
		lines("lecture.answer.righteous", "Yes, Elder!", "This disciple understands.", "We will remember, Elder.");
		lines("lecture.answer.neutral", "Yes, Elder.", "Understood.", "Noted, Elder.");
		lines("lecture.answer.devilish", "Yes, Elder.", "Hah. Of course, Elder.", "As you say, Elder.");
		lines("lecture.question.righteous", "Elder, how does one know when the foundation is truly firm?", "Elder, what if one's heart wavers at the bottleneck?");
		lines("lecture.question.neutral", "Elder, how long should a session last?", "Elder, does the mat really make a difference?");
		lines("lecture.question.devilish", "Elder, is it true the strong can eat the qi of the weak?", "Elder, what if my senior is in my way?");
		lines("lecture.close.righteous", "That is all for today. Go, and cultivate diligently.", "Reflect on today's words. Dismissed.");
		lines("lecture.close.neutral", "That's all. Back to work.", "Dismissed.");
		lines("lecture.close.devilish", "Enough. Get out of my sight and get stronger.", "Dismissed. Try not to die before tomorrow.");

		// --- Talk among themselves (%1$s: the one spoken to; %2$s: the subject) ---
		talk("chat.progress.righteous", "%1$s, how goes your cultivation?", "Slowly. The bottleneck holds firm.", "Patience. It will give way.");
		talk("chat.progress.righteous", "My qi ran a full circuit without stumbling today.", "Congratulations, %1$s! Your diligence is paying off.");
		talk("chat.progress.righteous", "%1$s, do you ever doubt the path?", "Every day. Then I sit down and cultivate anyway.");
		talk("chat.progress.neutral", "Broken through yet?", "No. You?", "No.");
		talk("chat.progress.neutral", "%1$s, lend me your mat? Mine's worn thin.", "Get your own.", "Worth a try.");
		talk("chat.progress.neutral", "How far along are you?", "Far enough. Not far enough.");
		talk("chat.progress.devilish", "%1$s, I'll reach the next stage before you.", "Only if I'm dead first.", "That can be arranged.");
		talk("chat.progress.devilish", "I hear your circulation stumbled again.", "Who told you? I'll cut out their tongue.");
		talk("chat.progress.devilish", "Your aura's grown, %1$s.", "Jealous?", "Hungry.");
		talk("chat.duty.righteous", "Have you swept the plaza, %1$s?", "At dawn, as always.");
		talk("chat.duty.righteous", "The patrol found wolf tracks by the east path.", "I'll tell the Elders. Stay alert tonight, %1$s.");
		talk("chat.duty.neutral", "Gate duty again tomorrow.", "Better than latrine duty.", "Barely.");
		talk("chat.duty.neutral", "Who's on night watch?", "Not me, thank Heaven.");
		talk("chat.duty.devilish", "If I walk that patrol ring one more time, I'll burn it down.", "Do it. I'll watch.");
		talk("chat.duty.devilish", "The juniors are slacking again.", "Then we'll teach them. Painfully.");
		talk("chat.elder.righteous", "Elder %2$s seemed troubled this morning.", "The Elders carry burdens we cannot see.");
		talk("chat.elder.righteous", "They say %2$s once fought a demon for three days.", "And won without spilling a drop of innocent blood, I heard.");
		talk("chat.elder.neutral", "%2$s is in a mood today.", "Then stay out of sight.");
		talk("chat.elder.neutral", "Has anyone ever seen %2$s eat?", "They live on qi and disapproval.");
		talk("chat.elder.devilish", "One day I'll sit in %2$s's seat.", "Say that louder. I'd love to watch.");
		talk("chat.elder.devilish", "%2$s looked at me today.", "And you're still alive? Lucky.");
		talk("chat.visitor.righteous", "Did you see %2$s at the gates?", "A capable one. Courteous, too.");
		talk("chat.visitor.righteous", "%2$s came by again.", "Then we should treat our guest well, %1$s.");
		talk("chat.visitor.neutral", "%2$s was here again.", "Watch your pockets.");
		talk("chat.visitor.neutral", "What does %2$s want with us?", "Don't know. Don't care.");
		talk("chat.visitor.devilish", "That %2$s is back.", "Strong?", "Strong enough to be interesting.");
		talk("chat.visitor.devilish", "I'd like to see what %2$s is made of.", "Bones, mostly. Like everyone.");
		talk("chat.gossip.righteous", "Have you heard, %1$s? %2$s", "Truly? Heaven provides for those who seek.");
		talk("chat.gossip.righteous", "They say %2$s", "We should tell the Elders.");
		talk("chat.gossip.neutral", "Heard something. %2$s", "Worth a look.");
		talk("chat.gossip.neutral", "Word is: %2$s", "Or it's a trap.");
		talk("chat.gossip.devilish", "Listen, %1$s. %2$s", "Then we get there first.");
		talk("chat.gossip.devilish", "A little bird told me: %2$s", "And did you eat the bird?");
		talk("chat.gossip.commoner", "Did you hear? %2$s", "Immortal business. Best stay away.");
		// The sect's missions (SectMissions#chatSubject): %2$s a deed one of its disciples did today, or a mission one has taken on.
		talk("chat.mission.righteous", "Have you heard, %1$s? %2$s.", "Then let us not fall behind. The board rewards the diligent.");
		talk("chat.mission.righteous", "%2$s, they say.", "May Heaven guard them out there.");
		talk("chat.mission.neutral", "%2$s.", "Good. Less work for us.");
		talk("chat.mission.neutral", "Heard %2$s.", "Contribution doesn't earn itself, %1$s.");
		talk("chat.mission.devilish", "%2$s.", "Showing off for the elders. How tiresome.");
		talk("chat.mission.devilish", "Did you hear, %1$s? %2$s.", "Then they'll be the first to die out there. Pity.");
		talk("chat.life.righteous", "The plum trees will flower soon.", "I look forward to it. A calm heart sees beauty everywhere.");
		talk("chat.life.righteous", "%1$s, the kitchen made steamed buns today.", "Then I know where I'm going after meditation.");
		talk("chat.life.neutral", "Kitchen's out of rice again.", "Of course it is.");
		talk("chat.life.neutral", "Saw a fox in the garden.", "Leave it. It eats the rats.");
		talk("chat.life.devilish", "Someone stole my good robe.", "Was it red?", "Yes.", "Then I look excellent in it.");
		talk("chat.life.devilish", "The cook put in too much chilli again.", "Good. Builds character.");
		talk("chat.whisper.righteous", "(whispering) %1$s, are you awake?", "(whispering) Hush. Cultivate.");
		talk("chat.whisper.righteous", "(whispering) My legs have gone numb.", "(whispering) That is the Dao entering you.");
		talk("chat.whisper.neutral", "(whispering) How long have we been sitting?", "(whispering) Too long. Shh.");
		talk("chat.whisper.neutral", "(whispering) You're snoring.", "(whispering) I'm meditating loudly.");
		talk("chat.whisper.devilish", "(whispering) I'm going to pass you, %1$s.", "(whispering) In your dreams.");
		talk("chat.whisper.devilish", "(whispering) If the Elder looks away, we leave.", "(whispering) The Elder never looks away.");
		talk("chat.elders.righteous", "The young ones grow quickly this year, %1$s.", "If they keep their hearts steady, the sect is in good hands.");
		talk("chat.elders.righteous", "Our barrier holds well.", "Thanks to the veins beneath us, and our care.");
		talk("chat.elders.neutral", "Quiet day.", "Let's keep it that way.");
		talk("chat.elders.neutral", "The treasury's light this season.", "Then we spend less.");
		talk("chat.elders.devilish", "Which disciple will you discipline today, %1$s?", "The one who yawned in my lecture.");
		talk("chat.elders.devilish", "If the righteous come, let them come.", "I've been saving a technique just for them.");
		talk("chat.road.righteous", "Where does your road lead, fellow Daoist?", "Wherever the qi is thickest.");
		talk("chat.road.righteous", "Have you passed any sects nearby?", "One, a few days back. They keep good manners.");
		talk("chat.road.neutral", "Road's been quiet.", "Too quiet.");
		talk("chat.road.neutral", "Seen any old herbs?", "If I had, would I tell you?");
		talk("chat.road.devilish", "Nice robe. Where'd you get it?", "From the last one who asked.");
		talk("chat.road.devilish", "Travelling alone?", "Not for long, if you keep following me.");
		talk("chat.road.commoner", "Off to market?", "If the bandits allow it.");
		talk("chat.road.commoner", "Did you see the Immortals flying overhead?", "Like geese! Every morning!");
		// More of the same (2026-10-27: the user's ask for more of what NPCs say).
		talk("chat.progress.righteous", "%1$s, the dawn qi was thick today. Did you feel it?", "Like spring water. I sat until my legs went numb.");
		talk("chat.progress.righteous", "I read the third chapter of the manual again.", "And?", "I understood one more line than yesterday.", "Then today was a good day, %1$s.");
		talk("chat.progress.neutral", "My meridians ache.", "Means it's working.", "Means something.");
		talk("chat.progress.neutral", "Took a pill last night.", "And?", "Saw colours till dawn. Don't buy from the east market.");
		talk("chat.progress.devilish", "I found a faster way to cultivate, %1$s.", "Whose blood is it?", "Don't ask questions you don't want answered.");
		talk("chat.progress.devilish", "When I break through, you'll call me Senior.", "When you break through, I'll call you lucky.");
		talk("chat.duty.righteous", "The herb garden needs weeding, %1$s.", "Then we weed it. Even this is cultivation.");
		talk("chat.duty.righteous", "The juniors asked me to teach them the sword form.", "Teach them patiently, %1$s. We were juniors once.");
		talk("chat.duty.neutral", "Treasury needs counting again.", "Who lost count this time?", "You did.");
		talk("chat.duty.neutral", "The north wall needs mending.", "Tomorrow.", "You said that yesterday.");
		talk("chat.duty.devilish", "I was told to guard the herb garden.", "From what?", "From me, I think.");
		talk("chat.duty.devilish", "Who left the gate open last night?", "Someone who wanted a fight, I hope.");
		talk("chat.life.righteous", "Listen. The cranes are back on the peak.", "A good omen, %1$s.");
		talk("chat.life.righteous", "I wrote to my family at last.", "They'll be glad. Mortal years pass quickly; don't wait so long next time.");
		talk("chat.life.neutral", "Tea's gone cold.", "Heat it with your qi.", "That's wasteful.", "So is complaining.");
		talk("chat.life.neutral", "My robe's torn again.", "Sew it.", "With what? My qi?");
		talk("chat.life.devilish", "I had the strangest dream, %1$s.", "Was I in it?", "You were dying.", "Was I winning?");
		talk("chat.life.devilish", "The new disciple cried at breakfast.", "Did you make them?", "I made the breakfast.");
		talk("chat.visitor.righteous", "Did you feel %2$s's aura when they passed?", "Like standing at the foot of a mountain.", "We have far to go, %1$s.");
		talk("chat.visitor.neutral", "%2$s came through today.", "Strong?", "Stronger than us.", "Then we stay polite.");
		talk("chat.visitor.devilish", "%2$s walked past me like I was dirt.", "You are dirt.", "Not for much longer.");
		talk("chat.road.righteous", "The roads are dangerous these days, fellow Daoist.", "Then we walk them together for a while.");
		talk("chat.road.neutral", "Where are you headed?", "Away from here.", "Same.");
		talk("chat.road.devilish", "You look like you have spirit stones.", "You look like you want to lose a hand.", "...Lovely weather.");
		talk("chat.road.commoner", "The harvest's good this year.", "Thank the heavens. And the immortals, for chasing off the beasts.");
		talk("chat.road.commoner", "My son wants to become an immortal.", "Mine too.", "They'll grow out of it.", "Or they'll fly.");
		talk("chat.road.ghost", "I met someone on the bridge who remembered their whole life.", "Lucky.", "They were weeping.", "...Lucky, still.");

		// --- Practice (%s: the other's name; for those watching, %1$s and %2$s: the two sparring) ---
		lines("spar.begin.righteous", "%s, a friendly bout?", "Shall we test our swords, %s?");
		lines("spar.begin.neutral", "%s. Practice?", "A bout, %s? Go easy.");
		lines("spar.begin.devilish", "%s! Let's see if you've improved. I doubt it.", "Draw, %s. Try not to cry.");
		lines("spar.win.righteous", "Well fought, %s. Your stance is improving.", "A good bout, %s. Thank you.");
		lines("spar.win.neutral", "Mine. Again tomorrow, %s.", "Your guard drops on the left, %s.");
		lines("spar.win.devilish", "Too slow, %s. As always.", "Pathetic, %s. Again tomorrow.");
		lines("spar.lose.righteous", "I concede, %s. Your sword is swift.", "You have bested me, %s. I will reflect on it.");
		lines("spar.lose.neutral", "Fine. You win, %s.", "Lucky, %s.");
		lines("spar.lose.devilish", "Next time I won't hold back, %s.", "Enjoy it, %s. It won't happen twice.");
		lines("spar.watch.righteous", "Look, %1$s and %2$s are sparring!", "%1$s's footwork is excellent.");
		lines("spar.watch.neutral", "%1$s against %2$s. Five stones on %1$s.", "Hm. %2$s will lose.");
		lines("spar.watch.devilish", "Someone's going to bleed. Finally.", "Hit harder, %1$s! Hit %2$s harder!");

		// --- Juniors and seniors (%s: the senior's name) ---
		lines("bow.elder.righteous", "Greetings, Elder %s.", "This disciple greets Elder %s.");
		lines("bow.elder.neutral", "Elder.", "Elder %s.");
		lines("bow.elder.devilish", "Elder %s. Your humble disciple.", "Elder. Always a pleasure.");
		lines("bow.master.righteous", "Greetings, Sect Master!", "Sect Master %s! This disciple pays respects.");
		lines("bow.master.neutral", "Sect Master.", "Sect Master %s.");
		lines("bow.master.devilish", "Sect Master! Long may you reign.", "Sect Master. Your servant.");
		lines("nod.righteous", "Carry on, child.", "Diligence, disciple.");
		lines("nod.neutral", "Mm. Carry on.", "Back to work.");
		lines("nod.devilish", "Don't grovel. It's unbecoming.", "Yes, yes. Move along.");

		// --- Grief, alarm and breakthroughs (%s: a name) ---
		lines("grief.righteous", "%s! No...!", "%s has fallen! Avenge them!", "%s... I swear you will be avenged.");
		lines("grief.neutral", "%s is down!", "They killed %s!", "%s... damn it.");
		lines("grief.devilish", "Who dares kill %s? I'll flay them!", "%s is dead! Blood for blood!", "%s, you fool. Dying before me.");
		lines("mourn.righteous", "Rest well, %s. May your next life be gentler.", "We will carry on your cultivation, %s.", "I burned incense for you, %s.");
		lines("mourn.neutral", "You were a good one, %s.", "Should've been me, %s.", "Rest, %s.");
		lines("mourn.devilish", "You owed me three spirit stones, %s. I forgive you.", "Weak, %s. But you were ours.", "I'll find who did this, %s.");
		lines("alarm.attack.righteous", "We're under attack! To arms, disciples!", "Protect the sect!");
		lines("alarm.attack.neutral", "Attack! Everyone up!", "Intruder!");
		lines("alarm.attack.devilish", "Finally, a fight! Kill them!", "Someone's come here to die!");
		lines("alarm.barrier.righteous", "The barrier is under assault!", "Someone is breaking the barrier!");
		lines("alarm.barrier.neutral", "The barrier's being hit!", "Something's at the barrier!");
		lines("alarm.barrier.devilish", "Someone's knocking on our barrier. Let's answer.", "The barrier! Kill whoever's out there!");
		lines("alarm.thief.righteous", "Thief! In the treasury!", "Protect the treasury!");
		lines("alarm.thief.neutral", "Thief!", "The treasury!");
		lines("alarm.thief.devilish", "A thief! I'll take their hands!", "Someone's in our treasury. Slowly, now. Make it hurt.");
		lines("alarm.trespass.righteous", "Who goes there? Stop!", "Hold! You trespass on sect grounds!");
		lines("alarm.trespass.neutral", "Hey! Hands off!", "Stop right there!");
		lines("alarm.trespass.devilish", "Touch one more thing and I'll break your fingers.", "A trespasser! Oh, good.");
		lines("breakthrough.righteous", "I... I've broken through! The heavens are kind!", "The bottleneck gives way! My thanks to my teachers!");
		lines("breakthrough.neutral", "Finally. Broke through.", "Next stage. Good.");
		lines("breakthrough.devilish", "Ha! Stronger! Let them all tremble!", "A breakthrough! Who's laughing now?");
		lines("congratulate.righteous", "Congratulations, %s! Your diligence is rewarded.", "%s has broken through! Wonderful!");
		lines("congratulate.neutral", "Nice, %s.", "Well done, %s.");
		lines("congratulate.devilish", "Hmph. Congratulations, %s. Don't let it go to your head.", "%s broke through? ...I'm happy for you. Really.");
		// The Ritual to the Heavens (HeavenRitual): kneeling at the altar, and the heavens' answer.
		lines("ritual.begin.righteous", "Heavens above, hear this humble disciple.", "I offer what I have. Grant me the way forward.");
		lines("ritual.begin.neutral", "Heavens. I ask your permission to cultivate on.", "Here's my offering. Let me through.");
		lines("ritual.begin.devilish", "Heavens! I'll ask once. Politely.", "Take the offering and open the way. I'm not asking twice.");
		lines("ritual.granted.righteous", "The heavens have answered! My thanks, my thanks!", "The way is open. I won't waste it.");
		lines("ritual.granted.neutral", "They said yes.", "Permission granted. Good.");
		lines("ritual.granted.devilish", "Ha! Even the heavens know better than to refuse me.", "Granted. As it should be.");
		lines("ritual.denied.righteous", "Silence... I am not ready yet. Tomorrow, then.", "The heavens turn away. I must be more sincere.");
		lines("ritual.denied.neutral", "Nothing. Tomorrow.", "Wasted offering.");
		lines("ritual.denied.devilish", "Ignored?! Tomorrow they'll listen.", "Stingy heavens. Fine. Again at dawn.");

		// --- In talk with a player (TalkSystem#lineFor), by the rung they stand on before it (NpcAddress): its answer to a greeting, a
		// friend's greeting, its answer when asked; %1$s what it calls them, %2$s their title or their name ---
		lines("talk.greet.far_above.righteous", "Well met, %1$s. Mind your manners on this mountain.", "Diligence, %1$s. One day we may speak as equals.");
		lines("talk.greet.far_above.neutral", "What is it, %1$s?", "Speak, %1$s. Briefly.");
		lines("talk.greet.far_above.devilish", "Bowing to me, %1$s? Smart. Very smart.", "Oh, it talks. What is it, %1$s?");
		lines("talk.greet.far_above.ghost", "A little light, come to greet the dark. Hello, %1$s.", "So small a soul, and so polite. Greetings, %1$s.");
		lines("talk.greet.above.righteous", "Well met, %1$s. Your foundation looks steady.", "Greetings, %1$s. Keep your heart as steady as your breath.");
		lines("talk.greet.above.neutral", "Mm. Greetings, %1$s.", "Need something, %1$s?");
		lines("talk.greet.above.devilish", "Manners, from you, %1$s? They might even keep you alive.", "Greetings, %1$s. Don't look so nervous. Yet.");
		lines("talk.greet.above.ghost", "Greetings, %1$s. Have you found your feet down here?", "Ah, %1$s. Walk with me a while, if you like.");
		lines("talk.greet.peer.righteous", "Well met, %1$s! Sit and take some tea with me.", "May your path be clear and your heart steady, %1$s.");
		lines("talk.greet.peer.neutral", "%1$s. Tea's hot, if you want it.", "Greetings, %1$s. Road treating you well?");
		lines("talk.greet.peer.devilish", "%1$s. Tea? It isn't poisoned. Probably.", "Well, well. %1$s. What brings you to me?");
		lines("talk.greet.peer.ghost", "Another soul on the road. Greetings, %1$s.", "Sit, %1$s. We have all the time there is.");
		lines("talk.greet.peer.commoner", "Good day to you, %1$s!", "Hello, %1$s! Fine weather, isn't it?");
		lines("talk.greet.senior.righteous", "%1$s honours this one by stopping. Please, be at ease.", "This junior greets %1$s. How may I serve?");
		lines("talk.greet.senior.neutral", "%1$s. This one is listening.", "Greetings, %1$s. What do you need?");
		lines("talk.greet.senior.devilish", "%1$s! This one meant no offence. None at all.", "Ah, %1$s. What a pleasant surprise. Truly.");
		lines("talk.greet.senior.ghost", "Your warmth is welcome here, %1$s.", "This soul greets %1$s.");
		lines("talk.greet.senior.commoner", "Greetings, %1$s! What can this humble one do for you?", "%1$s! Welcome to our village!");
		lines("talk.greet.esteemed.righteous", "This junior greets %1$s! To be noticed by you is my fortune.", "%1$s! Please, tell this junior how I may be of use.");
		lines("talk.greet.esteemed.neutral", "%1$s. This one is honoured. What do you require?", "Your presence is... considerable, %1$s. How can this one help?");
		lines("talk.greet.esteemed.devilish", "%1$s! This worthless one greets you!", "Such a presence... whatever %1$s wants, this one will see it done!");
		lines("talk.greet.esteemed.ghost", "Your light is almost too bright for us, %1$s.", "This poor soul bows to %1$s.");
		lines("talk.greet.esteemed.commoner", "%1$s! Please, please, come in! Sit!", "This humble one greets %1$s! What do you need?");
		lines("talk.greet.revered.righteous", "This humble junior kowtows to %1$s! What would you have of me?", "This one is not worthy to speak with you, %1$s. But ask, and I will answer.");
		lines("talk.greet.revered.neutral", "%1$s. This one kneels. Command me.", "Speak, %1$s, and it will be done.");
		lines("talk.greet.revered.devilish", "Mercy, %1$s! This one will do anything!", "This lowly dog kneels to %1$s! Command me, command me!");
		lines("talk.greet.revered.ghost", "Even the dead kneel to %1$s.", "Command this soul, %1$s. It is yours.");
		lines("talk.greet.revered.commoner", "%1$s! We kowtow! Ask anything of us, anything!", "This humble one kneels before %1$s! What is your will?");
		lines("talk.greet.ancestor.righteous", "%1$s! My ancestors are honoured beyond words. Command me!", "To be greeted by %1$s... this junior will remember it until death!");
		lines("talk.greet.ancestor.neutral", "...%1$s. This one dares not look up.", "%1$s. Anything. Ask anything.");
		lines("talk.greet.ancestor.devilish", "This insect kowtows to %1$s! A thousand, ten thousand times!", "%1$s! This worm lives only to serve!");
		lines("talk.greet.ancestor.ghost", "The Wheel itself would kneel to you, %1$s.", "This soul is dust before %1$s.");
		lines("talk.greet.ancestor.commoner", "Is it... is it really %1$s? We kowtow! We kowtow!", "Heavens above! %1$s, in our humble village!");
		lines("talk.greet.mortal.righteous", "A mortal this far up? Go carefully, %1$s, and keep to the path.", "Greetings, %1$s. These mountains are no place for the unwary.");
		lines("talk.greet.mortal.neutral", "Keep out from underfoot, %1$s, and no one will trouble you.", "What is it, %1$s?");
		lines("talk.greet.mortal.devilish", "Walking right up to me, %1$s? Brave, or stupid?", "How quaint, %1$s. Greeting me as if we were equals.");
		lines("talk.greet.mortal.ghost", "Still warm, %1$s? What are you doing among the dead?", "Greetings, %1$s. Your breath fogs in the cold here.");
		lines("talk.greet.liked.far_above.righteous", "%2$s! You've grown since last time. Good.", "Welcome back, %1$s. Sit, sit. Tell me how your cultivation goes.");
		lines("talk.greet.liked.far_above.neutral", "%2$s. Good to see you.", "Back again, %2$s? Sit down.");
		lines("talk.greet.liked.far_above.devilish", "%2$s! Still alive, I see. Good.", "Ah, %2$s. You're almost tolerable, you know.");
		lines("talk.greet.liked.far_above.ghost", "%2$s... you came back. Few ever do.", "Ah, %2$s. Sit with me a while.");
		lines("talk.greet.liked.above.righteous", "%2$s! Well met again. Your foundation grows steadier.", "Ah, %2$s. Come, take tea with me.");
		lines("talk.greet.liked.above.neutral", "%2$s. Road treating you well?", "Back again, %2$s? Good.");
		lines("talk.greet.liked.above.devilish", "%2$s! Don't make me regret liking you.", "Well, if it isn't %2$s. Still breathing.");
		lines("talk.greet.liked.above.ghost", "%2$s. You remembered us.", "Welcome back, %2$s. The lanterns missed you.");
		lines("talk.greet.liked.peer.righteous", "%2$s! Well met again. Sit, the tea is fresh.", "Ah, %2$s, my friend. May your cultivation flourish.");
		lines("talk.greet.liked.peer.neutral", "%2$s. Good to see you.", "%2$s. Sit. Talk.");
		lines("talk.greet.liked.peer.devilish", "%2$s! My favourite guest. Don't make me regret it.", "%2$s. You're almost tolerable.");
		lines("talk.greet.liked.peer.ghost", "%2$s. Time is all we have here; spend some with me.", "Ah, %2$s. You still remember me?");
		lines("talk.greet.liked.peer.commoner", "%2$s! Come in, come in!", "Good to see you, %2$s! How's the family?");
		lines("talk.greet.liked.senior.righteous", "%1$s! This junior hoped you would come by.", "Welcome back, %1$s! Please, sit. I'll bring tea.");
		lines("talk.greet.liked.senior.neutral", "%1$s. Good to see you again.", "Back, %1$s? Welcome.");
		lines("talk.greet.liked.senior.devilish", "%1$s! This one kept your favourite seat warm.", "Ah, %1$s! This one missed you. Truly.");
		lines("talk.greet.liked.senior.ghost", "You came back to us, %1$s.", "We remember you, %1$s. Sit.");
		lines("talk.greet.liked.senior.commoner", "%1$s is back! Bless you!", "Welcome back, %1$s! Stay for supper!");
		lines("talk.greet.liked.esteemed.righteous", "%1$s! You honour this junior once again.", "This junior is always glad to see %1$s! Please, rest here.");
		lines("talk.greet.liked.esteemed.neutral", "%1$s. An honour, as always.", "Welcome back, %1$s. This one is at your service.");
		lines("talk.greet.liked.esteemed.devilish", "%1$s! The best thing to happen to this one all year!", "Ah, %1$s, back again! This one was just thinking of you. Fondly!");
		lines("talk.greet.liked.esteemed.ghost", "Even here, you bring warmth, %1$s.", "The dead are glad of your return, %1$s.");
		lines("talk.greet.liked.esteemed.commoner", "%1$s! The whole village hoped you'd come back!", "Bless you, %1$s! Come, sit by the fire!");
		lines("talk.greet.liked.revered.righteous", "%1$s! To see you again, this junior's fortune is boundless.", "Every visit is a blessing to this junior, %1$s.");
		lines("talk.greet.liked.revered.neutral", "%1$s. This one is glad to kneel to you again.", "Welcome back, %1$s.");
		lines("talk.greet.liked.revered.devilish", "%1$s! This loyal dog greets you again!", "Ah, %1$s! This one has been ever so good since you last came!");
		lines("talk.greet.liked.revered.ghost", "The dead kneel gladly to you, %1$s.", "You came back, %1$s. We kneel.");
		lines("talk.greet.liked.revered.commoner", "%1$s has come back to us! We kowtow!", "Our dear %1$s! We kneel, we kneel!");
		lines("talk.greet.liked.ancestor.righteous", "%1$s! My ancestors smile on me this day!", "To be greeted again by %1$s... this one's heart is full.");
		lines("talk.greet.liked.ancestor.neutral", "%1$s. Again. This one is... honoured.", "Command me, %1$s, as before.");
		lines("talk.greet.liked.ancestor.devilish", "%1$s remembers this worm! This worm is blessed!", "Ah, %1$s! This one tells everyone about you!");
		lines("talk.greet.liked.ancestor.ghost", "The Wheel itself would turn back for you, %1$s.", "This soul has waited for your return, %1$s.");
		lines("talk.greet.liked.ancestor.commoner", "%1$s, again! The heavens love our village!", "We kowtow, %1$s, we kowtow! Welcome back!");
		lines("talk.greet.liked.mortal.righteous", "Ah, %2$s! Still keeping to the path, I hope.", "%2$s, my mortal friend. Sit, have some tea.");
		lines("talk.greet.liked.mortal.neutral", "%2$s. You again. Good.", "Back, %2$s? Stay out of trouble.");
		lines("talk.greet.liked.mortal.devilish", "%2$s! My favourite mortal. Don't die yet.", "Still alive, %2$s? Clever little thing.");
		lines("talk.greet.liked.mortal.ghost", "%2$s. You're still warm. I'm glad.", "Ah, %2$s. Sit by the lantern, where it's warm.");
		lines("talk.ask.far_above.righteous", "Speak quickly, %1$s. This one has little time.", "Questions, %1$s? Ask, then.");
		lines("talk.ask.far_above.neutral", "Be brief, %1$s.", "Ask. Quickly.");
		lines("talk.ask.far_above.devilish", "You want something from me, %1$s? How bold. Ask, before I lose interest.", "Ask, %1$s. If I like the question, you might live.");
		lines("talk.ask.far_above.ghost", "Ask, %1$s. The dead know many things.", "What would a little soul want to know?");
		lines("talk.ask.above.righteous", "Questions, %1$s? Ask. I have a little time.", "Ask, %1$s. I'll answer what I can.");
		lines("talk.ask.above.neutral", "Ask, %1$s.", "What is it you want to know, %1$s?");
		lines("talk.ask.above.devilish", "Questions cost nothing, %1$s. Answers might.", "Ask away, %1$s. I'll decide what's true.");
		lines("talk.ask.above.ghost", "Ask, %1$s. I have nothing but time.", "The dead hear things. Ask, %1$s.");
		lines("talk.ask.peer.righteous", "Ask, %1$s. I'll answer what I can.", "What would you know, %1$s?");
		lines("talk.ask.peer.neutral", "What would you know, %1$s?", "Go on, %1$s. Ask.");
		lines("talk.ask.peer.devilish", "Ask, %1$s. I'll decide what's true.", "Information, %1$s? Everything has a price.");
		lines("talk.ask.peer.ghost", "Ask, %1$s. The Springs whisper to us.", "What would you know of the dead, %1$s?");
		lines("talk.ask.peer.commoner", "Ask away, %1$s.", "What's on your mind, %1$s?");
		lines("talk.ask.senior.righteous", "%1$s may ask anything.", "Whatever %1$s wishes to know, this junior will answer.");
		lines("talk.ask.senior.neutral", "Ask, %1$s.", "This one is listening, %1$s.");
		lines("talk.ask.senior.devilish", "%1$s wishes to know? Ask, ask!", "This one knows lots of things, %1$s! Ask!");
		lines("talk.ask.senior.ghost", "Ask, %1$s. The dead have no secrets left.", "This soul will answer %1$s.");
		lines("talk.ask.senior.commoner", "Me, %1$s? I know very little, but ask.", "Ask, %1$s! This humble one will try to help.");
		lines("talk.ask.esteemed.righteous", "This junior will answer whatever %1$s asks, to the best of my poor knowledge.", "Please, %1$s, ask anything at all.");
		lines("talk.ask.esteemed.neutral", "%1$s need only ask.", "Ask, %1$s. This one will hold nothing back.");
		lines("talk.ask.esteemed.devilish", "This one will hold nothing back from %1$s!", "Ask, %1$s, ask! This one knows every secret around here!");
		lines("talk.ask.esteemed.ghost", "The dead will tell %1$s all they know.", "Ask, %1$s. This soul will not lie to you.");
		lines("talk.ask.esteemed.commoner", "Ask anything, %1$s! Anything at all!", "This humble one will tell %1$s whatever I know!");
		lines("talk.ask.revered.righteous", "Command this junior, %1$s. Whatever you wish to know, I will tell you.", "Ask, %1$s, and this junior will answer with all I know.");
		lines("talk.ask.revered.neutral", "Ask, %1$s. This one will answer, on my life.", "Every word true, %1$s. Ask.");
		lines("talk.ask.revered.devilish", "Anything, %1$s! This dog will tell you anything!", "Ask, %1$s, and spare this one!");
		lines("talk.ask.revered.ghost", "Ask, %1$s. This soul has no secrets from you.", "The dead will answer %1$s, on their knees.");
		lines("talk.ask.revered.commoner", "Ask, %1$s! We'll answer, we will!", "Whatever %1$s wishes to know!");
		lines("talk.ask.ancestor.righteous", "That %1$s would ask this lowly one anything... ask, and I will answer with my life!",
				"This junior is unworthy, but will answer %1$s as best I can.");
		lines("talk.ask.ancestor.neutral", "...Ask, %1$s. Anything.", "This one will answer %1$s. Every word true.");
		lines("talk.ask.ancestor.devilish", "Ask, %1$s! This worm knows nothing, but this worm will tell you everything!", "%1$s deigns to ask this insect? Ask, ask!");
		lines("talk.ask.ancestor.ghost", "Ask, %1$s. Even the Wheel would answer you.", "This soul will answer %1$s with all it remembers.");
		lines("talk.ask.ancestor.commoner", "Ask us anything, %1$s! Anything at all!", "Answer %1$s? Oh, heavens. Yes, ask!");
		lines("talk.ask.mortal.righteous", "Ask, then, %1$s. Briefly.", "Questions, %1$s? Ask. Then go home.");
		lines("talk.ask.mortal.neutral", "What do you want to know, %1$s?", "Ask, %1$s. Quickly.");
		lines("talk.ask.mortal.devilish", "Ask, %1$s. It amuses me.", "Questions, %1$s? How brave.");
		lines("talk.ask.mortal.ghost", "What would the warm want with the dead? Ask, %1$s.", "Ask, %1$s, before the cold gets in.");

		// --- The Netherworld's dead (GhostCultivatorNpc): melancholy, and a little strange ---
		lines("pass.liked.ghost", "%s... you came back. Few ever do.", "Ah, %s. You still remember me?", "%s. Sit a while. Time is all we have here.");
		lines("pass.disliked.ghost", "You again. The dead have long memories.", "Go on, %s. Walk past, as the living always do.",
				"I counted your steps last time, %s. I'm counting again.");
		lines("pass.awe.ghost", "Such a soul... even death bows to it.", "Senior. Even here, your light is heavy.", "Do not look at it. Do not look.");
		lines("pass.junior.ghost", "So small. So newly dead.", "Little one, the cold gets easier. Then it gets worse.", "Do you know your name yet? Hold on to it.");
		lines("pass.mortal.ghost", "A mortal soul... how did you come so far?", "You smell of rice and rain. I miss rain.", "Stay close to the lanterns, little mortal.");
		lines("pass.peer.ghost", "Another wanderer of the Yellow Springs.", "The lanterns burn blue tonight. They always do.",
				"Have you seen my body? I left it somewhere.", "Do you hear it? The Wheel, turning, far away.");
		lines("pass.stone.ghost", "A %s... it still holds the warmth of the sun.", "What use is a %s to the dead? ...May I hold it?");
		lines("pass.fruit.ghost", "That fruit is alive. I can hear it breathing.", "Put it away. Hunger never dies down here.");
		lines("pass.living.ghost", "You're warm. You're so warm.", "Your heart is still beating. Go home before it stops.",
				"The yin is in you already. I can see it on your breath.", "The living should not linger by the Springs.");
		lines("spar.begin.ghost", "%s. Shall we see what death left us?", "Fight me, %s. I want to remember how.");
		lines("spar.win.ghost", "You flicker, %s. Hold yourself together.", "Again tomorrow, %s. We have nothing but tomorrows.");
		lines("spar.lose.ghost", "I yield, %s. I came apart a little.", "Well struck, %s. It almost hurt.");
		lines("spar.watch.ghost", "%1$s and %2$s are fighting. How lively.", "Look at them, %1$s and %2$s. Like the old days.");
		lines("grief.ghost", "%s has scattered!", "%s... gone to cyan sparks.", "Who scattered %s? Who?");
		lines("mourn.ghost", "%s will gather again. Most do.", "Come back together, %s.", "The Wheel will find you, %s.");
		lines("alarm.attack.ghost", "Something hunts us! Gather your yin!", "They come for the dead!");
		lines("breakthrough.ghost", "I... grew stronger. Even dead, I grew stronger!", "The ghostly qi answers me!");
		lines("congratulate.ghost", "%s has grown. Death is not the end of the road.", "Well done, %s. Climb on.");
		talk("chat.road.ghost", "Do you remember the sun, %1$s?", "Warm. Yellow. That's all I have left of it.");
		talk("chat.road.ghost", "How long have you been dead, %1$s?", "I stopped counting at the hundredth Wheel.", "Lucky. I never started.");
		talk("chat.road.ghost", "I dreamed I was alive again.", "Ghosts don't dream, %1$s.", "Then what was it?");
		talk("chat.road.ghost", "Have you drunk Meng Po's soup?", "Not yet. I'm not ready to forget.", "Neither am I.");
		talk("chat.road.ghost", "The Hungry Ghosts are out in the Bone Wastes again.", "Then we stay on the road.");
		talk("chat.gossip.ghost", "Listen, %1$s. %2$s", "Even the dead hear things, it seems.");
		talk("chat.gossip.ghost", "They say %2$s", "I'll go and look. What else is there to do?");
		// The Gate's keepers (GhostCultivatorNpc): to the dead as they come, to the living, and Meng Po's call.
		lines("warden.greet", "Another one. Welcome to the Netherworld, %s.", "%s. Your thread was cut. Walk on to the Wheel when you're ready.",
				"Name: %s. Dead. Pass.");
		lines("warden.warn", "You, %s! You still breathe. The yin is filling you. Go back.", "Living one. Your warmth is going out. Leave.",
				"%s. The living do not belong at this gate.");
		lines("meng_po.call", "Come, %s. A bowl of soup for the road.", "Drink, child. Forgetting is a kindness.",
				"%s... the soup is warm. The past is heavy.");
		// A Ghost King on its throne (GhostCultivatorNpc#speakToComers): to the dead, and to the living.
		lines("king.greet", "Kneel, %s. You stand in the court of a king of the dead.", "Another soul at my gates. Come, %s, and be counted.",
				"The dead bow to a king, %s. So will you.");
		lines("king.warn", "A living thing in my palace? Leave, %s, before my court makes you one of us.",
				"The halls of the dead are not for the warm, %s. Go.", "Breath, in my court? Bold, %s. Foolish.");
		// How each of the dead died (GhostCultivatorNpc#deathKey), told when asked about themselves.
		lines("death.tribulation", "The ninth bolt of my tribulation. I saw it coming, and I still didn't move.");
		lines("death.deviation", "My qi turned on me halfway through a breakthrough. My meridians burned from the inside.");
		lines("death.sect_war", "A sect war. Ours lost. I held the gate until there was nothing left of me to hold it.");
		lines("death.devilish_palm", "A devilish cultivator's palm. One touch. My heart simply stopped.");
		lines("death.old_age", "Old age, with my cultivation unfinished. Two hundred years, and the bottleneck never gave way.");
		lines("death.poison", "A pill I brewed myself. I got the fire wrong.");
		lines("death.betrayal", "My own sworn brother, for a herb a thousand years old. I hope it was worth it to him.");
		lines("death.beast", "A spirit beast in the deep mountains. I thought I could take its core.");
		lines("death.duel", "A duel over a slight I can't even remember now.");
		lines("death.breakthrough", "I forced a breakthrough I wasn't ready for. The heavens weren't patient with me.");
		lines("death.starvation", "Starved, in seclusion. I sealed the cave too well, and forgot to come out.");
		lines("death.plague", "A plague in a mortal village I was protecting. Even cultivators can sicken.");
		lines("death.drowning", "I fell from my sword over the sea. Qi Refining. I should have learned to swim.");
		lines("death.assassin", "A knife in the dark, during meditation. I never saw a face.");
		lines("death.ascension", "The Spatial Gap, on my way to ascend. Its pressure crushed me two breaths from the other side.");
		lines("death.ox_head", "Died? I keep this gate. I have kept it longer than your world has had a name.");
		lines("death.horse_face", "My death is older than your ancestors' ancestors. Ask me something else.");
		lines("death.meng_po", "I forgot, child. I drank my own soup long ago.");
	}

	private NpcLines() {}
}
