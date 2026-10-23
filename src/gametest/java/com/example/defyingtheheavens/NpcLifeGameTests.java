package com.example.defyingtheheavens;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * NPCs' everyday life (NpcLife, NpcSpeech, NpcLines): the parts of the day, sleep and the night watch, where the lecture is
 * held, a talk taking turns, the fallen mourned, remarks to a passer-by, how long a line stays up, and every line being in
 * the lang file. Each sect made here is put out of the way (extinct) when its test is done.
 */
public class NpcLifeGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void theDayHasItsParts(GameTestHelper helper) {
		helper.assertTrue(NpcLife.Phase.of(0) == NpcLife.Phase.DAWN && NpcLife.Phase.of(999) == NpcLife.Phase.DAWN, "Sunrise is dawn");
		helper.assertTrue(NpcLife.Phase.of(1000) == NpcLife.Phase.LECTURE && NpcLife.Phase.of(3999) == NpcLife.Phase.LECTURE, "Then the lecture");
		helper.assertTrue(NpcLife.Phase.of(4000) == NpcLife.Phase.DAY && NpcLife.Phase.of(10999) == NpcLife.Phase.DAY, "Then the day");
		helper.assertTrue(NpcLife.Phase.of(11000) == NpcLife.Phase.EVENING, "Then the evening");
		helper.assertTrue(NpcLife.Phase.of(13000) == NpcLife.Phase.NIGHT && NpcLife.Phase.of(23999) == NpcLife.Phase.NIGHT, "Then night");
		helper.assertTrue(NpcLife.Phase.of(24000) == NpcLife.Phase.DAWN && NpcLife.Phase.of(-1) == NpcLife.Phase.NIGHT, "Days wrap round");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyLineIsInTheLangFile(GameTestHelper helper) {
		JsonObject lang;
		try (InputStream in = NpcLifeGameTests.class.getResourceAsStream("/assets/defying-the-heavens/lang/en_us.json")) {
			helper.assertTrue(in != null, "The English lang file is on the classpath");
			lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
		int count = 0;
		for (Map.Entry<String, String> entry : NpcLines.english().entrySet()) {
			helper.assertTrue(lang.has(entry.getKey()), "en_us.json has " + entry.getKey());
			helper.assertTrue(lang.get(entry.getKey()).getAsString().equals(entry.getValue()), "en_us.json says what NpcLines says for " + entry.getKey());
			count++;
		}
		helper.assertTrue(count > 300, "There's a good deal to say (" + count + " lines)");
		// What the server picks are keys the lang file has, a mortal falling back to the neutral voice where it has none.
		CultivatorNpc righteous = npc(helper, new BlockPos(1, 2, 1), Realm.QI_REFINING, 120);
		CultivatorNpc commoner = npc(helper, new BlockPos(3, 2, 1), null, 0);
		for (int i = 0; i < 20; i++) {
			helper.assertTrue(lang.has(key(NpcLines.line("pass.peer", righteous))), "A passing remark is in the file");
			helper.assertTrue(lang.has(key(NpcLines.line("lecture", commoner))), "A commoner without a lecture of its own speaks as the neutral do");
			for (String line : NpcLines.script("chat.progress", "devilish", righteous.getRandom())) helper.assertTrue(lang.has(line), "A talk's lines are in the file");
		}
		helper.assertTrue(NpcLines.line("no.such.line", righteous) == null, "Nothing to say to what isn't there");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void disciplesSleepAtNightAndRiseAtDawn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = sect(helper);
		try {
			CultivatorNpc outer = member(helper, sect, new BlockPos(1, 2, 3), Realm.QI_REFINING, NpcTitle.OUTER_DISCIPLE, 0);
			CultivatorNpc inner = member(helper, sect, new BlockPos(3, 2, 3), Realm.CORE_FORMATION, NpcTitle.INNER_DISCIPLE, 1);
			CultivatorNpc elder = member(helper, sect, new BlockPos(5, 2, 3), Realm.NASCENT_SOUL, NpcTitle.ELDER, 2);
			long now = level.getGameTime();
			NpcLife.live(level, sect, List.of(outer, inner, elder), NpcLife.Phase.NIGHT, now);
			NpcLife.Activity sleep = outer.activity();
			helper.assertTrue(sleep != null && sleep.kind == NpcLife.Kind.SLEEP, "A Qi Refining disciple goes to bed at night");
			helper.assertTrue(sleep.spot.equals(sect.dwelling(0)), "...at home");
			NpcLife.Activity rest = inner.activity();
			helper.assertTrue(rest != null && rest.kind == NpcLife.Kind.REST && rest.seated, "From Core Formation, it meditates through the night instead");
			helper.assertTrue(elder.activity() == null, "An Elder keeps its post");
			NpcLife.live(level, sect, List.of(outer, inner, elder), NpcLife.Phase.DAWN, now);
			helper.assertTrue(outer.activity() == sleep && sleep.until <= now + 600, "At dawn it gets up within half a minute");
			helper.succeed();
		} finally {
			close(sect);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theGatesHaveADayAndANightWatch(GameTestHelper helper) {
		Sect sect = new Sect(UUID.randomUUID());
		sect.posts(Sect.Post.GATE_GUARD).add(new BlockPos(0, 0, 0));
		sect.posts(Sect.Post.GATE_GUARD).add(new BlockPos(1, 0, 0));
		List<Sect.Member> outer = new ArrayList<>();
		for (int rank = 3; rank >= 0; rank--) {
			Sect.Member m = new Sect.Member(UUID.randomUUID(), rank, 0);
			m.title = NpcTitle.OUTER_DISCIPLE;
			sect.members.put(m.id, m);
			outer.add(m); // strongest first
		}
		helper.assertTrue(sect.guardPost(outer.get(0), false) != null && sect.guardPost(outer.get(1), false) != null, "The strongest two keep the day");
		helper.assertTrue(sect.guardPost(outer.get(0), true) == null && sect.guardPost(outer.get(1), true) == null, "...and rest at night");
		helper.assertTrue(new BlockPos(0, 0, 0).equals(sect.guardPost(outer.get(2), true)) && new BlockPos(1, 0, 0).equals(sect.guardPost(outer.get(3), true)),
				"The next two keep the night");
		CultivatorNpc weak = npc(helper, new BlockPos(1, 2, 1), Realm.QI_REFINING, 0);
		helper.assertTrue(NpcLife.routineFor(sect, outer.get(0), weak, NpcLife.Phase.NIGHT) == NpcLife.Kind.SLEEP, "Off watch at night: to bed");
		helper.assertTrue(NpcLife.routineFor(sect, outer.get(2), weak, NpcLife.Phase.NIGHT) == null, "On the night watch: at the gate");
		helper.assertTrue(NpcLife.routineFor(sect, outer.get(2), weak, NpcLife.Phase.LECTURE) == NpcLife.Kind.SLEEP, "The night watch sleeps in");
		helper.assertTrue(NpcLife.routineFor(sect, outer.get(0), weak, NpcLife.Phase.LECTURE) == null, "The day watch is up");
		sect.members.remove(outer.get(3).id);
		helper.assertTrue(sect.guardPost(outer.get(0), true) != null && sect.guardPost(outer.get(2), true) == null,
				"Too few for two watches: the day's guards keep the gates all night");
		helper.succeed();
	}

	/**
	 * The hall is built out past the test's own one-block structure, and the seat search reaches past it again (rows of five, to the
	 * hall's end). Sharing a batch, the neighbours' floors, players and builds came into that ground over the 20 ticks it once waited,
	 * now and then taking seats away or the whole hall ("no room", "2 seats"; 2026-10-19). So: a batch of its own, and the ground
	 * the search reaches cleared first.
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = "npc_life_lecture_hall", timeoutTicks = 60)
	public void theLectureIsHeldInTheMainHall(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = -1; x <= 10; x++) for (int z = -3; z <= 10; z++) for (int y = 1; y <= 7; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
		// A hall in miniature: a stone floor, a roof over its far end; the master's seat at the back, the plaza (core) ahead.
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) {
			helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			if (x >= 3) helper.setBlock(new BlockPos(x, 4, z), Blocks.STONE);
		}
		Sect sect = new Sect(UUID.randomUUID());
		sect.core = helper.absolutePos(new BlockPos(30, 2, 3));
		sect.posts(Sect.Post.MASTER_SEAT).add(helper.absolutePos(new BlockPos(0, 4, 3)));
		// "Roofed" goes by the sky's light (NpcLife#roofed), which takes some ticks to settle under a new roof.
		helper.runAfterDelay(20, () -> {
			NpcLife.LectureLayout layout = NpcLife.lectureLayout(level, sect);
			helper.assertTrue(layout != null, "There's room for a lecture");
			helper.assertTrue(layout.lectern().equals(helper.absolutePos(new BlockPos(4, 2, 3))), "The teacher stands four blocks before the seat, on the floor");
			helper.assertTrue(layout.seats().contains(helper.absolutePos(new BlockPos(6, 2, 3))), "The front row is two blocks further, facing it");
			helper.assertTrue(layout.seats().size() >= 3, "A row has several seats (" + layout.seats().size() + ")");
			for (BlockPos seat : layout.seats()) helper.assertTrue(seat.getX() == helper.absolutePos(new BlockPos(6, 2, 3)).getX(), "Only the roofed row");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
	public void aTalkTakesTurns(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CultivatorNpc a = npc(helper, new BlockPos(2, 2, 2), Realm.QI_REFINING, 0);
		CultivatorNpc b = npc(helper, new BlockPos(3, 2, 2), Realm.QI_REFINING, 0);
		long start = level.getGameTime();
		NpcLife.Conversation talk = NpcLife.startTalk(level, null, null, a, b, true, start);
		helper.assertTrue(talk != null, "Two rogues fall to talking");
		helper.assertTrue(a.activity().kind == NpcLife.Kind.CHAT && b.getUUID().equals(a.activity().partner)
				&& a.getUUID().equals(b.activity().partner), "Each talks with the other");
		NpcLife.advanceTalksForTest();
		helper.assertTrue(a.lastSpoke() == start && b.lastSpoke() < start, "The one who began speaks first");
		helper.succeedWhen(() -> {
			helper.assertTrue(b.lastSpoke() > start, "Then the other answers");
			helper.assertTrue(a.activity() == null && b.activity() == null, "And when it's said, they go their ways");
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theFallenAreMournedWhereTheyFell(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE); // ground to stand on
		Sect sect = sect(helper);
		try {
			CultivatorNpc victim = member(helper, sect, new BlockPos(2, 2, 2), Realm.QI_REFINING, NpcTitle.OUTER_DISCIPLE, 0);
			CultivatorNpc friend = member(helper, sect, new BlockPos(5, 2, 5), Realm.QI_REFINING, NpcTitle.OUTER_DISCIPLE, 1);
			NpcLife.onDeath(level, victim);
			long now = level.getGameTime();
			for (int i = 0; i < 300 && friend.activity() == null; i++) NpcLife.mournForTest(level, sect, List.of(friend), NpcLife.Phase.DAY, now);
			NpcLife.Activity mourn = friend.activity();
			helper.assertTrue(mourn != null && mourn.kind == NpcLife.Kind.MOURN, "A fellow disciple goes to mourn");
			helper.assertTrue(mourn.spot.closerThan(victim.blockPosition(), 4), "...where it fell");
			helper.assertTrue(victim.getPersonalName().equals(mourn.mourned), "...for the one who fell");
			helper.succeed();
		} finally {
			close(sect);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void passersByAreRemarkedOn(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer strong = cultivator(helper, made, "strong", Realm.NASCENT_SOUL, Stage.EARLY);
			CultivatorNpc junior = npc(helper, new BlockPos(3, 2, 2), Realm.QI_REFINING, 0);
			helper.assertTrue(key(NpcLines.line("pass.awe", junior)) != null, "There are lines of awe");
			helper.assertTrue(key(NpcLife.remarkOn(junior, strong, false)).startsWith(ModLang.npcSay("pass.awe.")), "Two realms beneath you, it is in awe");
			junior.changeRegard(strong, Regard.WARM);
			helper.assertTrue(key(NpcLife.remarkOn(junior, strong, false)).startsWith(ModLang.npcSay("pass.liked.")), "One who likes you greets you");
			junior.changeRegard(strong, -Regard.WARM - 40);
			helper.assertTrue(key(NpcLife.remarkOn(junior, strong, false)).startsWith(ModLang.npcSay("pass.disliked.")), "One who doesn't, says so");
			CultivatorNpc guard = npc(helper, new BlockPos(1, 2, 3), Realm.QI_REFINING, 0);
			guard.setGateGuard(true);
			helper.assertTrue(key(NpcLife.remarkOn(guard, strong, false)).startsWith(ModLang.npcSay("gate.halt.")), "A gate guard halts a stranger");
			guard.changeRegard(strong, Regard.COURTEOUS);
			helper.assertTrue(key(NpcLife.remarkOn(guard, strong, false)).startsWith(ModLang.npcSay("gate.welcome.")), "...and welcomes a friend");
			strong.getInventory().setItem(strong.getInventory().selected, new net.minecraft.world.item.ItemStack(ModItems.IMMORTAL_SPIRIT_STONE));
			CultivatorNpc greedy = npc(helper, new BlockPos(4, 2, 4), Realm.NASCENT_SOUL, -120);
			helper.assertTrue(key(NpcLife.remarkOn(greedy, strong, false)).startsWith(ModLang.npcSay("pass.stone.devilish.")), "An Immortal stone in hand gets noticed");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aGateGuardRaisedAboveTheGatesLeavesThem(GameTestHelper helper) {
		// Only outer disciples keep the gates: one raised to a higher title (its seniors fallen while it fought, say) is a gate
		// guard no longer, so its name shows the new title, and not only in its colour.
		CultivatorNpc guard = npc(helper, new BlockPos(2, 2, 2), Realm.QI_REFINING, 0);
		guard.setTitle(NpcTitle.OUTER_DISCIPLE);
		guard.setGateGuard(true);
		guard.setTitle(NpcTitle.OUTER_DISCIPLE);
		helper.assertTrue(guard.isGateGuard(), "Still an outer disciple, it keeps the gate");
		guard.setTitle(NpcTitle.SECT_MASTER);
		helper.assertTrue(!guard.isGateGuard() && guard.getTitle() == NpcTitle.SECT_MASTER, "Made Sect Master, it is no gate guard");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void speechTakesItsTime(GameTestHelper helper) {
		CultivatorNpc npc = npc(helper, new BlockPos(2, 2, 2), Realm.QI_REFINING, 0);
		helper.assertTrue(NpcSpeech.mayRemark(npc), "Quiet at first");
		Component short1 = Component.translatable(ModLang.npcSay("pass.peer.neutral.1"));
		Component lecture = Component.translatable(ModLang.npcSay("lecture.neutral.6"));
		int shortTicks = NpcSpeech.ticksFor(short1), longTicks = NpcSpeech.ticksFor(lecture);
		helper.assertTrue(shortTicks >= 50 && longTicks <= 240 && longTicks > shortTicks, "A longer line stays up longer (" + shortTicks + ", " + longTicks + ")");
		helper.assertTrue(NpcSpeech.say(npc, NpcLines.line("pass.peer", npc)), "It speaks");
		helper.assertTrue(!NpcSpeech.mayRemark(npc), "...and won't remark on anything else straight away");
		helper.assertTrue(!NpcSpeech.say(npc, null), "Nothing to say is not said");
		helper.succeed();
	}

	// --- Helpers ---

	private static String key(Component line) {
		return line != null && line.getContents() instanceof TranslatableContents t ? t.getKey() : null;
	}

	/** A righteous sect around the test, registered so its members find it, with three homes. */
	private static Sect sect(GameTestHelper helper) {
		Sect sect = new Sect(UUID.randomUUID());
		sect.core = helper.absolutePos(new BlockPos(4, 2, 4));
		sect.barrierRadius = 48;
		sect.alignment = 100;
		for (int i = 0; i < 3; i++) sect.posts(Sect.Post.DWELLING).add(helper.absolutePos(new BlockPos(1 + 2 * i, 2, 1)));
		SectData.get(helper.getLevel().getServer()).put(sect);
		return sect;
	}

	private static CultivatorNpc member(GameTestHelper helper, Sect sect, BlockPos pos, Realm realm, NpcTitle title, int home) {
		CultivatorNpc npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, pos);
		npc.setRank(PlayerCultivation.rank(realm, Stage.EARLY));
		Sect.Member m = new Sect.Member(npc.getUUID(), npc.getRank(), home);
		m.title = title;
		sect.members.put(m.id, m);
		npc.joinSect(sect, home);
		npc.setTitle(title);
		return npc;
	}

	/** Puts the test's sect out of the way: nothing more happens to it or its day. */
	private static void close(Sect sect) {
		sect.extinct = true;
		NpcLife.forget(sect.id);
	}

	/** A cultivator NPC of {@code realm} (Early; null for a commoner) and {@code alignment}, with no will of its own. */
	private static CultivatorNpc npc(GameTestHelper helper, BlockPos pos, Realm realm, int alignment) {
		CultivatorNpc npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, pos);
		npc.setRank(realm == null ? CultivatorNpc.MORTAL : PlayerCultivation.rank(realm, Stage.EARLY));
		npc.setAlignment(alignment);
		npc.setHealth(npc.getMaxHealth());
		return npc;
	}

	private static ServerPlayer cultivator(GameTestHelper helper, List<ServerPlayer> made, String name, Realm realm, Stage stage) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isSpectator() {
				return false;
			}

			@Override
			public boolean isCreative() {
				return false;
			}
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		made.add(p);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(realm, stage, 0);
		CultivationStats.apply(p, c);
		return p;
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) p.server.getPlayerList().remove(p);
	}
}
