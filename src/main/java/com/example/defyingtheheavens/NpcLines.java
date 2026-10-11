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
 * each path's voice: "righteous" formal and earnest, "neutral" terse, "demonic" mocking; and "commoner" for mortals (a mortal
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

	/** The voice {@code npc} speaks in: its path's, or a commoner's. */
	public static String voice(CultivatorNpc npc) {
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
		// --- A player passing by (%s: the player's name, or what they carry) ---
		lines("pass.liked.righteous", "Fellow Daoist %s! Well met again.", "Ah, %s. May your cultivation flourish.", "%s! You honour us with a visit.");
		lines("pass.liked.neutral", "Ah, %s. Road treating you well?", "Back again, %s?", "%s. Good to see you.");
		lines("pass.liked.demonic", "Well, if it isn't %s. Still alive, I see.", "%s! My favourite guest. Don't make me regret it.", "%s. You're almost tolerable.");
		lines("pass.liked.commoner", "Good day, Immortal %s!", "Immortal %s! Bless you, bless you.", "Ah, Immortal %s is back!");
		lines("pass.disliked.righteous", "Hmph. You again.", "Mind your conduct here.", "I have not forgotten what you did.");
		lines("pass.disliked.neutral", "You. Keep walking.", "Don't stop here.", "Hm. Trouble's back.");
		lines("pass.disliked.demonic", "Look who crawled back.", "Come closer. I dare you.", "I remember your face. Fondly. Like a debt.");
		lines("pass.disliked.commoner", "It's that one... don't look.", "Please, Immortal, we want no trouble.", "Hide the rice, it's that one again.");
		lines("pass.awe.righteous", "Such a presence... this junior greets Senior!", "Senior! This junior pays respects.", "The air itself grows heavy... a true expert.");
		lines("pass.awe.neutral", "That pressure... a senior.", "Best not to stare.", "Senior.");
		lines("pass.awe.demonic", "S-senior... this one is only passing through!", "What a monstrous aura... don't look at it.", "Ah! Senior! Lovely weather!");
		lines("pass.awe.commoner", "An Immortal! Bow, bow!", "Heavens... I can hardly breathe near them.", "Immortal, please, spare this humble one!");
		lines("pass.junior.righteous", "Work hard, little one. The path is long.", "A junior. Keep your heart steady.", "Mind your footing on this mountain, junior.");
		lines("pass.junior.neutral", "Out of the way, junior.", "Hm. Small fry.", "Don't waste my time.");
		lines("pass.junior.demonic", "What a tiny little aura. Adorable.", "An ant, walking upright. Cute.", "Run along before I get hungry.");
		lines("pass.mortal.righteous", "A mortal? Keep to the path; these mountains are dangerous.", "Mortal friend, mind the beasts after dark.", "Few mortals come this far. Be careful.");
		lines("pass.mortal.neutral", "A mortal. Stay out from underfoot.", "Mortal. Don't touch anything.", "Hm. Lost?");
		lines("pass.mortal.demonic", "A mortal. How brave. How stupid.", "Fresh meat, walking about on its own.", "Mortal. I could use a new servant.");
		lines("pass.peer.righteous", "Fellow Daoist.", "Peace on your path.", "A fine day for cultivation.");
		lines("pass.peer.neutral", "Traveller.", "Hm. Passing through?", "Watch your step.");
		lines("pass.peer.demonic", "Keep your hands where I can see them.", "Looking for trouble? You found the right place.", "Hm? Something you want?");
		lines("pass.peer.commoner", "Good day to you!", "Fine weather, isn't it?", "Safe travels, friend.");
		lines("pass.outsider.righteous", "A guest on our mountain? Mind the sect's rules.", "Visitors should present themselves at the main hall.", "An outsider... welcome, but don't wander.");
		lines("pass.outsider.neutral", "Outsider. Don't wander.", "Who let you in?", "Keep to the paths.");
		lines("pass.outsider.demonic", "An outsider. Did you lose your way, or your mind?", "Wander a little further. I'd like to watch.", "Visitors here tend to stay. Forever.");
		lines("pass.stone.righteous", "Is that a %s? Keep it hidden, friend.", "The qi in that %s... remarkable.");
		lines("pass.stone.neutral", "That %s will get you killed. Put it away.", "Nice %s. Don't wave it around.");
		lines("pass.stone.demonic", "My, what a pretty %s. Shame if someone took it.", "That %s would look better in my hand.");
		lines("pass.stone.commoner", "Heavens, it shines like the moon!", "Is that... an Immortal's jewel?");
		lines("pass.fruit.righteous", "I can feel the years in that fruit from here.", "A Cultivation Fruit? Guard it well.");
		lines("pass.fruit.neutral", "Fruit like that draws thieves.", "Eat it or hide it. Don't carry it about.");
		lines("pass.fruit.demonic", "Mm, that fruit smells delicious. So do you.", "Give me that fruit and I'll forget your face.");
		lines("pass.fruit.commoner", "What a strange fruit... it glows!", "Oh my, that fruit smells like heaven.");

		// --- At the gate ---
		lines("gate.welcome.righteous", "Welcome back, %s. Enter in peace.", "%s! The gates are open to you.");
		lines("gate.welcome.neutral", "%s. Go on through.", "You're known here, %s. Pass.");
		lines("gate.welcome.demonic", "%s. Go in. Behave.", "Ah, %s. In you go, before I change my mind.");
		lines("gate.halt.righteous", "Halt, traveller. State your business with our sect.", "Visitors are welcome. Thieves are not.");
		lines("gate.halt.neutral", "State your business.", "Gate's watched. Don't try anything.");
		lines("gate.halt.demonic", "Turn back, or don't. I'm bored either way.", "One step closer and you're my afternoon's entertainment.");
		lines("watch.begin.righteous", "I'll keep the gate. Rest well.", "My watch begins. Heaven keep us.");
		lines("watch.begin.neutral", "My watch.", "I've got the gate.");
		lines("watch.begin.demonic", "My watch. Pray nothing interesting happens. Or do.", "Go and sleep. I'll guard your precious gate.");
		lines("watch.end.righteous", "The watch passes on. It's been quiet.", "All is well. The gate is yours.");
		lines("watch.end.neutral", "Quiet. Your turn.", "Done. Gate's yours.");
		lines("watch.end.demonic", "Nothing died. Disappointing. Your turn.", "Your gate now. Try not to lose it.");

		// --- Weather and the time of day ---
		lines("rain.righteous", "Rain. The herbs will be glad of it.", "Heaven waters the earth; a good sign.", "Let it rain. The qi is fresh after rain.");
		lines("rain.neutral", "Rain again.", "Wet. Lovely.", "There goes the training ground.");
		lines("rain.demonic", "Rain. Washes the blood away, at least.", "Ugh. Wet robes.", "Rain and gloom. My favourite.");
		lines("rain.commoner", "Rain! Get the washing in!", "The fields needed this.", "My knees said it would rain.");
		lines("rain.patrol.righteous", "A disciple's duty doesn't wait for fair weather.", "Rain or not, the perimeter must be walked.");
		lines("rain.patrol.neutral", "Patrol in the rain. Great.", "Every time I'm on rounds.");
		lines("rain.patrol.demonic", "Who decided we patrol in this muck?", "If anything attacks now, I'm letting it.");
		lines("thunder.righteous", "Thunder... is someone facing their tribulation?", "Heaven's wrath stirs. Be humble today.");
		lines("thunder.neutral", "Thunder. Someone's tribulation, maybe.", "Loud up there.");
		lines("thunder.demonic", "Ha! Some fool is being struck down by Heaven.", "Listen to it roar. Heaven is jealous again.");
		lines("thunder.commoner", "Thunder! Heaven protect us!", "Close the shutters!");
		lines("dusk.righteous", "The day is done. Rest well.", "Another day of cultivation behind us.");
		lines("dusk.neutral", "Lights out soon.", "Day's done.");
		lines("dusk.demonic", "Night. Finally, some peace.", "The best things happen after dark.");
		lines("dusk.commoner", "Time to head home.", "The lamps are lit. Off to bed.");
		lines("dawn.righteous", "Dawn. The morning qi is the purest.", "Up, up! The bell has rung.");
		lines("dawn.neutral", "Morning.", "Bell's rung. Up.");
		lines("dawn.demonic", "Morning already? Who rang that cursed bell?", "Another day, another fool to beat.");
		lines("dawn.commoner", "Morning! Back to work.", "Ah, the sun's up.");
		lines("sleep", "Zzz...", "Mm... five more breaths...", "...the third meridian... no... the fourth...", "Zzz... I'm awake, Elder... zzz...");

		// --- The morning lecture ---
		lines("lecture.open.righteous", "Settle your breath. Today we speak of the Dao.", "Sit straight, disciples. Let us begin.");
		lines("lecture.open.neutral", "Sit. Listen. We begin.", "Quiet. The lesson is starting.");
		lines("lecture.open.demonic", "Sit down and be quiet. Today you might even learn something.", "Eyes on me. The first to doze spars with me.");
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
		lines("lecture.demonic",
				"The Dao favours the strong. Be strong, or be someone's stepping stone.",
				"Mercy is a debt your enemy collects later. With interest.",
				"Steal a technique once and it's theft. Master it and it's yours.",
				"Heaven is not your friend. It's a rival with better weather.",
				"Your heart demons feed on hesitation. Starve them. Decide, and act.",
				"Every senior you bow to is a senior you will one day surpass.");
		lines("lecture.answer.righteous", "Yes, Elder!", "This disciple understands.", "We will remember, Elder.");
		lines("lecture.answer.neutral", "Yes, Elder.", "Understood.", "Noted, Elder.");
		lines("lecture.answer.demonic", "Yes, Elder.", "Hah. Of course, Elder.", "As you say, Elder.");
		lines("lecture.question.righteous", "Elder, how does one know when the foundation is truly firm?", "Elder, what if one's heart wavers at the bottleneck?");
		lines("lecture.question.neutral", "Elder, how long should a session last?", "Elder, does the mat really make a difference?");
		lines("lecture.question.demonic", "Elder, is it true the strong can eat the qi of the weak?", "Elder, what if my senior is in my way?");
		lines("lecture.close.righteous", "That is all for today. Go, and cultivate diligently.", "Reflect on today's words. Dismissed.");
		lines("lecture.close.neutral", "That's all. Back to work.", "Dismissed.");
		lines("lecture.close.demonic", "Enough. Get out of my sight and get stronger.", "Dismissed. Try not to die before tomorrow.");

		// --- Talk among themselves (%1$s: the one spoken to; %2$s: the subject) ---
		talk("chat.progress.righteous", "%1$s, how goes your cultivation?", "Slowly. The bottleneck holds firm.", "Patience. It will give way.");
		talk("chat.progress.righteous", "My qi ran a full circuit without stumbling today.", "Congratulations, %1$s! Your diligence is paying off.");
		talk("chat.progress.righteous", "%1$s, do you ever doubt the path?", "Every day. Then I sit down and cultivate anyway.");
		talk("chat.progress.neutral", "Broken through yet?", "No. You?", "No.");
		talk("chat.progress.neutral", "%1$s, lend me your mat? Mine's worn thin.", "Get your own.", "Worth a try.");
		talk("chat.progress.neutral", "How far along are you?", "Far enough. Not far enough.");
		talk("chat.progress.demonic", "%1$s, I'll reach the next stage before you.", "Only if I'm dead first.", "That can be arranged.");
		talk("chat.progress.demonic", "I hear your circulation stumbled again.", "Who told you? I'll cut out their tongue.");
		talk("chat.progress.demonic", "Your aura's grown, %1$s.", "Jealous?", "Hungry.");
		talk("chat.duty.righteous", "Have you swept the plaza, %1$s?", "At dawn, as always.");
		talk("chat.duty.righteous", "The patrol found wolf tracks by the east path.", "I'll tell the Elders. Stay alert tonight, %1$s.");
		talk("chat.duty.neutral", "Gate duty again tomorrow.", "Better than latrine duty.", "Barely.");
		talk("chat.duty.neutral", "Who's on night watch?", "Not me, thank Heaven.");
		talk("chat.duty.demonic", "If I walk that patrol ring one more time, I'll burn it down.", "Do it. I'll watch.");
		talk("chat.duty.demonic", "The juniors are slacking again.", "Then we'll teach them. Painfully.");
		talk("chat.elder.righteous", "Elder %2$s seemed troubled this morning.", "The Elders carry burdens we cannot see.");
		talk("chat.elder.righteous", "They say %2$s once fought a demon for three days.", "And won without spilling a drop of innocent blood, I heard.");
		talk("chat.elder.neutral", "%2$s is in a mood today.", "Then stay out of sight.");
		talk("chat.elder.neutral", "Has anyone ever seen %2$s eat?", "They live on qi and disapproval.");
		talk("chat.elder.demonic", "One day I'll sit in %2$s's seat.", "Say that louder. I'd love to watch.");
		talk("chat.elder.demonic", "%2$s looked at me today.", "And you're still alive? Lucky.");
		talk("chat.visitor.righteous", "Did you see %2$s at the gates?", "A capable one. Courteous, too.");
		talk("chat.visitor.righteous", "%2$s came by again.", "Then we should treat our guest well, %1$s.");
		talk("chat.visitor.neutral", "%2$s was here again.", "Watch your pockets.");
		talk("chat.visitor.neutral", "What does %2$s want with us?", "Don't know. Don't care.");
		talk("chat.visitor.demonic", "That %2$s is back.", "Strong?", "Strong enough to be interesting.");
		talk("chat.visitor.demonic", "I'd like to see what %2$s is made of.", "Bones, mostly. Like everyone.");
		talk("chat.gossip.righteous", "Have you heard, %1$s? %2$s", "Truly? Heaven provides for those who seek.");
		talk("chat.gossip.righteous", "They say %2$s", "We should tell the Elders.");
		talk("chat.gossip.neutral", "Heard something. %2$s", "Worth a look.");
		talk("chat.gossip.neutral", "Word is: %2$s", "Or it's a trap.");
		talk("chat.gossip.demonic", "Listen, %1$s. %2$s", "Then we get there first.");
		talk("chat.gossip.demonic", "A little bird told me: %2$s", "And did you eat the bird?");
		talk("chat.gossip.commoner", "Did you hear? %2$s", "Immortal business. Best stay away.");
		talk("chat.life.righteous", "The plum trees will flower soon.", "I look forward to it. A calm heart sees beauty everywhere.");
		talk("chat.life.righteous", "%1$s, the kitchen made steamed buns today.", "Then I know where I'm going after meditation.");
		talk("chat.life.neutral", "Kitchen's out of rice again.", "Of course it is.");
		talk("chat.life.neutral", "Saw a fox in the garden.", "Leave it. It eats the rats.");
		talk("chat.life.demonic", "Someone stole my good robe.", "Was it red?", "Yes.", "Then I look excellent in it.");
		talk("chat.life.demonic", "The cook put in too much chilli again.", "Good. Builds character.");
		talk("chat.whisper.righteous", "(whispering) %1$s, are you awake?", "(whispering) Hush. Cultivate.");
		talk("chat.whisper.righteous", "(whispering) My legs have gone numb.", "(whispering) That is the Dao entering you.");
		talk("chat.whisper.neutral", "(whispering) How long have we been sitting?", "(whispering) Too long. Shh.");
		talk("chat.whisper.neutral", "(whispering) You're snoring.", "(whispering) I'm meditating loudly.");
		talk("chat.whisper.demonic", "(whispering) I'm going to pass you, %1$s.", "(whispering) In your dreams.");
		talk("chat.whisper.demonic", "(whispering) If the Elder looks away, we leave.", "(whispering) The Elder never looks away.");
		talk("chat.elders.righteous", "The young ones grow quickly this year, %1$s.", "If they keep their hearts steady, the sect is in good hands.");
		talk("chat.elders.righteous", "Our barrier holds well.", "Thanks to the veins beneath us, and our care.");
		talk("chat.elders.neutral", "Quiet day.", "Let's keep it that way.");
		talk("chat.elders.neutral", "The treasury's light this season.", "Then we spend less.");
		talk("chat.elders.demonic", "Which disciple will you discipline today, %1$s?", "The one who yawned in my lecture.");
		talk("chat.elders.demonic", "If the righteous come, let them come.", "I've been saving a technique just for them.");
		talk("chat.road.righteous", "Where does your road lead, fellow Daoist?", "Wherever the qi is thickest.");
		talk("chat.road.righteous", "Have you passed any sects nearby?", "One, a few days back. They keep good manners.");
		talk("chat.road.neutral", "Road's been quiet.", "Too quiet.");
		talk("chat.road.neutral", "Seen any old herbs?", "If I had, would I tell you?");
		talk("chat.road.demonic", "Nice robe. Where'd you get it?", "From the last one who asked.");
		talk("chat.road.demonic", "Travelling alone?", "Not for long, if you keep following me.");
		talk("chat.road.commoner", "Off to market?", "If the bandits allow it.");
		talk("chat.road.commoner", "Did you see the Immortals flying overhead?", "Like geese! Every morning!");

		// --- Practice (%s: the other's name; for those watching, %1$s and %2$s: the two sparring) ---
		lines("spar.begin.righteous", "%s, a friendly bout?", "Shall we test our swords, %s?");
		lines("spar.begin.neutral", "%s. Practice?", "A bout, %s? Go easy.");
		lines("spar.begin.demonic", "%s! Let's see if you've improved. I doubt it.", "Draw, %s. Try not to cry.");
		lines("spar.win.righteous", "Well fought, %s. Your stance is improving.", "A good bout, %s. Thank you.");
		lines("spar.win.neutral", "Mine. Again tomorrow, %s.", "Your guard drops on the left, %s.");
		lines("spar.win.demonic", "Too slow, %s. As always.", "Pathetic, %s. Again tomorrow.");
		lines("spar.lose.righteous", "I concede, %s. Your sword is swift.", "You have bested me, %s. I will reflect on it.");
		lines("spar.lose.neutral", "Fine. You win, %s.", "Lucky, %s.");
		lines("spar.lose.demonic", "Next time I won't hold back, %s.", "Enjoy it, %s. It won't happen twice.");
		lines("spar.watch.righteous", "Look, %1$s and %2$s are sparring!", "%1$s's footwork is excellent.");
		lines("spar.watch.neutral", "%1$s against %2$s. Five stones on %1$s.", "Hm. %2$s will lose.");
		lines("spar.watch.demonic", "Someone's going to bleed. Finally.", "Hit harder, %1$s! Hit %2$s harder!");

		// --- Juniors and seniors (%s: the senior's name) ---
		lines("bow.elder.righteous", "Greetings, Elder %s.", "This disciple greets Elder %s.");
		lines("bow.elder.neutral", "Elder.", "Elder %s.");
		lines("bow.elder.demonic", "Elder %s. Your humble disciple.", "Elder. Always a pleasure.");
		lines("bow.master.righteous", "Greetings, Sect Master!", "Sect Master %s! This disciple pays respects.");
		lines("bow.master.neutral", "Sect Master.", "Sect Master %s.");
		lines("bow.master.demonic", "Sect Master! Long may you reign.", "Sect Master. Your servant.");
		lines("nod.righteous", "Carry on, child.", "Diligence, disciple.");
		lines("nod.neutral", "Mm. Carry on.", "Back to work.");
		lines("nod.demonic", "Don't grovel. It's unbecoming.", "Yes, yes. Move along.");

		// --- Grief, alarm and breakthroughs (%s: a name) ---
		lines("grief.righteous", "%s! No...!", "%s has fallen! Avenge them!", "%s... I swear you will be avenged.");
		lines("grief.neutral", "%s is down!", "They killed %s!", "%s... damn it.");
		lines("grief.demonic", "Who dares kill %s? I'll flay them!", "%s is dead! Blood for blood!", "%s, you fool. Dying before me.");
		lines("mourn.righteous", "Rest well, %s. May your next life be gentler.", "We will carry on your cultivation, %s.", "I burned incense for you, %s.");
		lines("mourn.neutral", "You were a good one, %s.", "Should've been me, %s.", "Rest, %s.");
		lines("mourn.demonic", "You owed me three spirit stones, %s. I forgive you.", "Weak, %s. But you were ours.", "I'll find who did this, %s.");
		lines("alarm.attack.righteous", "We're under attack! To arms, disciples!", "Protect the sect!");
		lines("alarm.attack.neutral", "Attack! Everyone up!", "Intruder!");
		lines("alarm.attack.demonic", "Finally, a fight! Kill them!", "Someone's come here to die!");
		lines("alarm.barrier.righteous", "The barrier is under assault!", "Someone is breaking the barrier!");
		lines("alarm.barrier.neutral", "The barrier's being hit!", "Something's at the barrier!");
		lines("alarm.barrier.demonic", "Someone's knocking on our barrier. Let's answer.", "The barrier! Kill whoever's out there!");
		lines("alarm.thief.righteous", "Thief! In the treasury!", "Protect the treasury!");
		lines("alarm.thief.neutral", "Thief!", "The treasury!");
		lines("alarm.thief.demonic", "A thief! I'll take their hands!", "Someone's in our treasury. Slowly, now. Make it hurt.");
		lines("alarm.trespass.righteous", "Who goes there? Stop!", "Hold! You trespass on sect grounds!");
		lines("alarm.trespass.neutral", "Hey! Hands off!", "Stop right there!");
		lines("alarm.trespass.demonic", "Touch one more thing and I'll break your fingers.", "A trespasser! Oh, good.");
		lines("breakthrough.righteous", "I... I've broken through! The heavens are kind!", "The bottleneck gives way! My thanks to my teachers!");
		lines("breakthrough.neutral", "Finally. Broke through.", "Next stage. Good.");
		lines("breakthrough.demonic", "Ha! Stronger! Let them all tremble!", "A breakthrough! Who's laughing now?");
		lines("congratulate.righteous", "Congratulations, %s! Your diligence is rewarded.", "%s has broken through! Wonderful!");
		lines("congratulate.neutral", "Nice, %s.", "Well done, %s.");
		lines("congratulate.demonic", "Hmph. Congratulations, %s. Don't let it go to your head.", "%s broke through? ...I'm happy for you. Really.");
	}

	private NpcLines() {}
}
